package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.budget.BudgetTargetCreateRequest;
import com.adem.attijari_compass.dto.budget.BudgetTargetResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetStatusUpdateRequest;
import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetTargetSource;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.BudgetTargetConflictException;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.BudgetTargetRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BudgetTargetService {

    private final BudgetTargetRepository budgetTargetRepository;
    private final UserRepository userRepository;
    private final BudgetTargetMonitoringService budgetTargetMonitoringService;

    public BudgetTargetResponse createBudgetTarget(BudgetTargetCreateRequest request, String email) {
        User user = getUserByEmail(email);

        log.info("Budget target creation requested: userId={}, category={}, level={}, source={}, amount={}",
                user.getId(),
                request.getCategory(),
                request.getSelectedLevel(),
                request.getSource(),
                request.getSuggestedMonthlyAmount());

        deactivateActiveTargets(user.getId(), request.getCategory(), null, "replaced by a newer budget target");

        BudgetTarget budgetTarget = BudgetTarget.builder()
                .user(user)
                .category(request.getCategory())
                .categoryLabel(resolveCategoryLabel(request.getCategory(), request.getCategoryLabel()))
                .source(request.getSource())
                .recommendationId(normalizeOptional(request.getRecommendationId()))
                .recommendationTitle(normalizeOptional(request.getRecommendationTitle()))
                .suggestedMonthlyAmount(request.getSuggestedMonthlyAmount())
                .selectedLevel(request.getSelectedLevel())
                .summary(normalizeOptional(request.getSummary()))
                .status(BudgetTargetStatus.ACTIVE)
                .build();

        BudgetTarget savedBudgetTarget = saveBudgetTarget(budgetTarget, user.getId(), request.getCategory());

        log.info("Budget target created: budgetTargetId={}, userId={}, category={}, status={}",
                savedBudgetTarget.getId(),
                user.getId(),
                savedBudgetTarget.getCategory(),
                savedBudgetTarget.getStatus());

        return mapToResponse(savedBudgetTarget, budgetTargetMonitoringService.buildSnapshot(user.getId(), savedBudgetTarget));
    }

    @Transactional(readOnly = true)
    public List<BudgetTargetResponse> getActiveBudgetTargetsForCurrentUser(String email) {
        User user = getUserByEmail(email);
        List<BudgetTarget> activeBudgetTargets = budgetTargetRepository
                .findAllByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), BudgetTargetStatus.ACTIVE);

        var monitoringSnapshots = budgetTargetMonitoringService.buildSnapshots(user.getId(), activeBudgetTargets);
        List<BudgetTargetResponse> responses = activeBudgetTargets
                .stream()
                .map(budgetTarget -> mapToResponse(budgetTarget, monitoringSnapshots.get(budgetTarget.getId())))
                .toList();

        log.info("Budget targets fetched: userId={}, activeCount={}", user.getId(), responses.size());
        return responses;
    }

    public BudgetTargetResponse updateBudgetTargetStatus(Long id, BudgetTargetStatusUpdateRequest request, String email) {
        User user = getUserByEmail(email);
        BudgetTarget budgetTarget = getBudgetTargetForUser(id, user.getId());

        if (request.getStatus() == BudgetTargetStatus.ACTIVE) {
            deactivateActiveTargets(user.getId(), budgetTarget.getCategory(), budgetTarget.getId(), "replaced by budget target activation");
        }

        budgetTarget.setStatus(request.getStatus());
        BudgetTarget savedBudgetTarget = saveBudgetTarget(budgetTarget, user.getId(), budgetTarget.getCategory());

        log.info("Budget target status updated: budgetTargetId={}, userId={}, newStatus={}",
                savedBudgetTarget.getId(),
                user.getId(),
                savedBudgetTarget.getStatus());

        return mapToResponse(savedBudgetTarget, budgetTargetMonitoringService.buildSnapshot(user.getId(), savedBudgetTarget));
    }

    private void deactivateActiveTargets(Long userId,
                                         TransactionCategory category,
                                         Long excludedBudgetTargetId,
                                         String reason) {
        List<BudgetTarget> activeTargets = excludedBudgetTargetId == null
                ? budgetTargetRepository.findAllByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
                        userId,
                        category,
                        BudgetTargetStatus.ACTIVE
                )
                : budgetTargetRepository.findAllByUserIdAndCategoryAndStatusAndIdNotOrderByCreatedAtDesc(
                        userId,
                        category,
                        BudgetTargetStatus.ACTIVE,
                        excludedBudgetTargetId
                );

        if (activeTargets.isEmpty()) {
            return;
        }

        activeTargets.forEach(target -> target.setStatus(BudgetTargetStatus.INACTIVE));
        budgetTargetRepository.saveAll(activeTargets);
        budgetTargetRepository.flush();

        log.info("Active budget targets deactivated: userId={}, category={}, count={}, reason={}",
                userId,
                category,
                activeTargets.size(),
                reason);
    }

    private BudgetTarget saveBudgetTarget(BudgetTarget budgetTarget, Long userId, TransactionCategory category) {
        try {
            return budgetTargetRepository.saveAndFlush(budgetTarget);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Budget target write conflict: userId={}, category={}, budgetTargetId={}",
                    userId,
                    category,
                    budgetTarget.getId(),
                    ex);
            throw new BudgetTargetConflictException(
                    "An active budget target already exists for category " + category + ". Please retry."
            );
        }
    }

    private BudgetTarget getBudgetTargetForUser(Long id, Long userId) {
        return budgetTargetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget target not found with id: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private BudgetTargetResponse mapToResponse(BudgetTarget budgetTarget,
                                               BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot monitoringSnapshot) {
        BudgetTargetLevel selectedLevel = budgetTarget.getSelectedLevel();
        BudgetTargetSource source = budgetTarget.getSource();
        BudgetTargetStatus status = budgetTarget.getStatus();

        return BudgetTargetResponse.builder()
                .id(budgetTarget.getId())
                .category(budgetTarget.getCategory())
                .categoryLabel(resolveCategoryLabel(budgetTarget.getCategory(), budgetTarget.getCategoryLabel()))
                .targetAmount(monitoringSnapshot != null ? monitoringSnapshot.targetAmount() : budgetTarget.getSuggestedMonthlyAmount())
                .selectedLevel(selectedLevel)
                .selectedLevelLabel(selectedLevel != null ? selectedLevel.getLabel() : null)
                .selectedLevelSummary(selectedLevel != null ? selectedLevel.getSummary() : null)
                .suggestedMonthlyAmount(budgetTarget.getSuggestedMonthlyAmount())
                .spentThisMonth(monitoringSnapshot != null ? monitoringSnapshot.spentThisMonth() : null)
                .remainingAmount(monitoringSnapshot != null ? monitoringSnapshot.remainingAmount() : null)
                .usagePercent(monitoringSnapshot != null ? monitoringSnapshot.usagePercent() : null)
                .monitoringStatus(monitoringSnapshot != null ? monitoringSnapshot.monitoringStatus() : null)
                .monitoringStatusLabel(monitoringSnapshot != null ? monitoringSnapshot.monitoringStatusLabel() : null)
                .source(source)
                .sourceLabel(source != null ? source.getLabel() : null)
                .recommendationId(budgetTarget.getRecommendationId())
                .recommendationTitle(budgetTarget.getRecommendationTitle())
                .summary(budgetTarget.getSummary())
                .status(status)
                .statusLabel(status != null ? status.getLabel() : null)
                .aiGenerated(source == BudgetTargetSource.RECOMMENDATION_AI)
                .createdAt(budgetTarget.getCreatedAt())
                .updatedAt(budgetTarget.getUpdatedAt())
                .build();
    }

    private String resolveCategoryLabel(TransactionCategory category, String requestedLabel) {
        if (StringUtils.hasText(requestedLabel)) {
            return requestedLabel.trim();
        }
        return humanizeCategory(category);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String humanizeCategory(TransactionCategory category) {
        if (category == null) {
            return null;
        }

        return Arrays.stream(category.name().toLowerCase(Locale.ROOT).split("_"))
                .map(this::capitalize)
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
