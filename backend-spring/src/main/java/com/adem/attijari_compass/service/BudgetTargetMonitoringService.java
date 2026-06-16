package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.repository.CategoryExpenseTotalProjection;
import com.adem.attijari_compass.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BudgetTargetMonitoringService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal SURVEILLANCE_THRESHOLD = new BigDecimal("70");
    private static final int MONEY_SCALE = 2;
    private static final int PERCENTAGE_SCALE = 2;

    private final TransactionRepository transactionRepository;

    public Map<Long, BudgetTargetMonitoringSnapshot> buildSnapshots(Long userId, List<BudgetTarget> budgetTargets) {
        if (budgetTargets == null || budgetTargets.isEmpty()) {
            return Map.of();
        }

        Set<TransactionCategory> categories = budgetTargets.stream()
                .map(BudgetTarget::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TransactionCategory.class)));

        Map<TransactionCategory, BigDecimal> monthlySpendByCategory = loadMonthlySpendByCategory(userId, categories);
        Map<Long, BudgetTargetMonitoringSnapshot> snapshots = new HashMap<>();

        for (BudgetTarget budgetTarget : budgetTargets) {
            BigDecimal spentThisMonth = monthlySpendByCategory.getOrDefault(budgetTarget.getCategory(), ZERO);
            snapshots.put(budgetTarget.getId(), createSnapshot(budgetTarget, spentThisMonth));
        }

        return snapshots;
    }

    public BudgetTargetMonitoringSnapshot buildSnapshot(Long userId, BudgetTarget budgetTarget) {
        if (budgetTarget == null) {
            return emptySnapshot();
        }

        Map<Long, BudgetTargetMonitoringSnapshot> snapshots = buildSnapshots(userId, List.of(budgetTarget));
        return snapshots.getOrDefault(budgetTarget.getId(), emptySnapshot());
    }

    private Map<TransactionCategory, BigDecimal> loadMonthlySpendByCategory(Long userId, Set<TransactionCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        List<CategoryExpenseTotalProjection> totals = transactionRepository
                .sumAbsoluteAmountByUserIdAndTypeAndDateBetweenAndCategoryIn(
                        userId,
                        TransactionType.DEPENSE,
                        startOfMonth,
                        today,
                        List.copyOf(categories)
                );

        Map<TransactionCategory, BigDecimal> spendByCategory = new HashMap<>();
        for (CategoryExpenseTotalProjection total : totals) {
            if (total.getCategory() == null) {
                continue;
            }
            spendByCategory.put(total.getCategory(), toMoney(total.getTotalAmount()));
        }

        log.debug("Budget monitoring totals loaded: userId={}, categories={}, totals={}",
                userId,
                categories.size(),
                spendByCategory.size());

        return spendByCategory;
    }

    private BudgetTargetMonitoringSnapshot createSnapshot(BudgetTarget budgetTarget, BigDecimal spentThisMonth) {
        BigDecimal normalizedSpent = normalizeMoney(spentThisMonth);
        BigDecimal targetAmount = normalizeMoney(budgetTarget.getSuggestedMonthlyAmount());

        if (targetAmount == null) {
            return new BudgetTargetMonitoringSnapshot(
                    null,
                    normalizedSpent,
                    null,
                    null,
                    null,
                    null
            );
        }

        BigDecimal remainingAmount = normalizeMoney(targetAmount.subtract(normalizedSpent));
        BigDecimal usagePercent;
        BudgetMonitoringStatus monitoringStatus;

        if (targetAmount.signum() == 0) {
            usagePercent = normalizedSpent.signum() > 0 ? HUNDRED.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP) : ZERO;
            monitoringStatus = normalizedSpent.signum() > 0
                    ? BudgetMonitoringStatus.DEPASSE
                    : BudgetMonitoringStatus.SOUS_CONTROLE;
        } else {
            usagePercent = normalizedSpent
                    .multiply(HUNDRED)
                    .divide(targetAmount, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
            monitoringStatus = resolveMonitoringStatus(usagePercent);
        }

        return new BudgetTargetMonitoringSnapshot(
                targetAmount,
                normalizedSpent,
                remainingAmount,
                usagePercent,
                monitoringStatus,
                monitoringStatus != null ? monitoringStatus.getLabel() : null
        );
    }

    private BudgetMonitoringStatus resolveMonitoringStatus(BigDecimal usagePercent) {
        if (usagePercent == null) {
            return null;
        }
        if (usagePercent.compareTo(SURVEILLANCE_THRESHOLD) < 0) {
            return BudgetMonitoringStatus.SOUS_CONTROLE;
        }
        if (usagePercent.compareTo(HUNDRED) <= 0) {
            return BudgetMonitoringStatus.A_SURVEILLER;
        }
        return BudgetMonitoringStatus.DEPASSE;
    }

    private BudgetTargetMonitoringSnapshot emptySnapshot() {
        return new BudgetTargetMonitoringSnapshot(null, ZERO, null, null, null, null);
    }

    private BigDecimal toMoney(Double value) {
        if (value == null) {
            return ZERO;
        }
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record BudgetTargetMonitoringSnapshot(
            BigDecimal targetAmount,
            BigDecimal spentThisMonth,
            BigDecimal remainingAmount,
            BigDecimal usagePercent,
            BudgetMonitoringStatus monitoringStatus,
            String monitoringStatusLabel
    ) {
    }
}
