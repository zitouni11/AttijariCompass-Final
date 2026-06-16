package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.budget.BudgetTargetCreateRequest;
import com.adem.attijari_compass.dto.budget.BudgetTargetResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetStatusUpdateRequest;
import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetTargetSource;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.repository.BudgetTargetRepository;
import com.adem.attijari_compass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetTargetServiceTest {

    @Mock
    private BudgetTargetRepository budgetTargetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetTargetMonitoringService budgetTargetMonitoringService;

    @InjectMocks
    private BudgetTargetService budgetTargetService;

    @Test
    void shouldCreateBudgetTargetAndDeactivateExistingActiveBudget() {
        User user = buildUser();
        BudgetTarget existingTarget = BudgetTarget.builder()
                .id(11L)
                .user(user)
                .category(TransactionCategory.SHOPPING)
                .categoryLabel("Shopping")
                .source(BudgetTargetSource.RECOMMENDATION_AI)
                .selectedLevel(BudgetTargetLevel.PRUDENT)
                .status(BudgetTargetStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        BudgetTargetCreateRequest request = new BudgetTargetCreateRequest();
        request.setCategory(TransactionCategory.SHOPPING);
        request.setCategoryLabel("Shopping");
        request.setSelectedLevel(BudgetTargetLevel.EQUILIBRE);
        request.setSuggestedMonthlyAmount(new BigDecimal("400.00"));
        request.setSource(BudgetTargetSource.RECOMMENDATION_AI);
        request.setRecommendationId("expense-HIGH-1-shopping");
        request.setRecommendationTitle("Limiter le poids du shopping dans votre budget");
        request.setSummary("Cadre budgetaire prepare depuis une recommandation IA");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(budgetTargetRepository.findAllByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
                user.getId(),
                TransactionCategory.SHOPPING,
                BudgetTargetStatus.ACTIVE
        )).thenReturn(List.of(existingTarget));
        when(budgetTargetRepository.saveAndFlush(any(BudgetTarget.class))).thenAnswer(invocation -> {
            BudgetTarget savedTarget = invocation.getArgument(0);
            savedTarget.setId(21L);
            savedTarget.setCreatedAt(LocalDateTime.of(2026, 4, 10, 9, 30));
            savedTarget.setUpdatedAt(LocalDateTime.of(2026, 4, 10, 9, 30));
            return savedTarget;
        });
        when(budgetTargetMonitoringService.buildSnapshot(eq(user.getId()), any(BudgetTarget.class))).thenReturn(
                new BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot(
                        new BigDecimal("400.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("280.00"),
                        new BigDecimal("30.00"),
                        BudgetMonitoringStatus.SOUS_CONTROLE,
                        "Sous controle"
                )
        );

        BudgetTargetResponse response = budgetTargetService.createBudgetTarget(request, user.getEmail());

        assertEquals(BudgetTargetStatus.INACTIVE, existingTarget.getStatus());
        assertEquals(21L, response.getId());
        assertEquals(TransactionCategory.SHOPPING, response.getCategory());
        assertEquals("Shopping", response.getCategoryLabel());
        assertEquals(BudgetTargetLevel.EQUILIBRE, response.getSelectedLevel());
        assertEquals("Equilibre", response.getSelectedLevelLabel());
        assertEquals(0, response.getTargetAmount().compareTo(new BigDecimal("400.00")));
        assertEquals(0, response.getSuggestedMonthlyAmount().compareTo(new BigDecimal("400.00")));
        assertEquals(0, response.getSpentThisMonth().compareTo(new BigDecimal("120.00")));
        assertEquals(0, response.getRemainingAmount().compareTo(new BigDecimal("280.00")));
        assertEquals(0, response.getUsagePercent().compareTo(new BigDecimal("30.00")));
        assertEquals(BudgetMonitoringStatus.SOUS_CONTROLE, response.getMonitoringStatus());
        assertEquals(BudgetTargetSource.RECOMMENDATION_AI, response.getSource());
        assertTrue(response.isAiGenerated());
        assertEquals(BudgetTargetStatus.ACTIVE, response.getStatus());

        verify(budgetTargetRepository).saveAll(List.of(existingTarget));
        verify(budgetTargetRepository).flush();
        verify(budgetTargetRepository).saveAndFlush(any(BudgetTarget.class));
    }

    @Test
    void shouldReturnOnlyActiveBudgetTargetsForCurrentUser() {
        User user = buildUser();
        BudgetTarget activeTarget = BudgetTarget.builder()
                .id(31L)
                .user(user)
                .category(TransactionCategory.TRANSPORT)
                .categoryLabel("Transport")
                .source(BudgetTargetSource.MANUAL)
                .selectedLevel(BudgetTargetLevel.PRUDENT)
                .suggestedMonthlyAmount(new BigDecimal("250.00"))
                .summary("Cadre manuel")
                .status(BudgetTargetStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 4, 9, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 9, 8, 15))
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(budgetTargetRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(),
                BudgetTargetStatus.ACTIVE
        )).thenReturn(List.of(activeTarget));
        when(budgetTargetMonitoringService.buildSnapshots(user.getId(), List.of(activeTarget))).thenReturn(
                java.util.Map.of(
                        activeTarget.getId(),
                        new BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot(
                                new BigDecimal("250.00"),
                                new BigDecimal("190.00"),
                                new BigDecimal("60.00"),
                                new BigDecimal("76.00"),
                                BudgetMonitoringStatus.A_SURVEILLER,
                                "A surveiller"
                        )
                )
        );

        List<BudgetTargetResponse> responses = budgetTargetService.getActiveBudgetTargetsForCurrentUser(user.getEmail());

        assertEquals(1, responses.size());
        assertEquals(TransactionCategory.TRANSPORT, responses.getFirst().getCategory());
        assertEquals("Transport", responses.getFirst().getCategoryLabel());
        assertEquals(0, responses.getFirst().getTargetAmount().compareTo(new BigDecimal("250.00")));
        assertEquals(0, responses.getFirst().getSpentThisMonth().compareTo(new BigDecimal("190.00")));
        assertEquals(0, responses.getFirst().getRemainingAmount().compareTo(new BigDecimal("60.00")));
        assertEquals(0, responses.getFirst().getUsagePercent().compareTo(new BigDecimal("76.00")));
        assertEquals(BudgetMonitoringStatus.A_SURVEILLER, responses.getFirst().getMonitoringStatus());
        assertEquals("Prudent", responses.getFirst().getSelectedLevelLabel());
        assertEquals("Saisie manuelle", responses.getFirst().getSourceLabel());
        assertFalse(responses.getFirst().isAiGenerated());
    }

    @Test
    void shouldActivateBudgetTargetAndDeactivateOtherActiveTargetInSameCategory() {
        User user = buildUser();
        BudgetTarget targetToActivate = BudgetTarget.builder()
                .id(41L)
                .user(user)
                .category(TransactionCategory.STEG_SONEDE)
                .categoryLabel("Factures")
                .source(BudgetTargetSource.RECOMMENDATION_AI)
                .selectedLevel(BudgetTargetLevel.RENFORCE)
                .status(BudgetTargetStatus.INACTIVE)
                .createdAt(LocalDateTime.of(2026, 4, 6, 7, 30))
                .updatedAt(LocalDateTime.of(2026, 4, 6, 7, 30))
                .build();
        BudgetTarget otherActiveTarget = BudgetTarget.builder()
                .id(42L)
                .user(user)
                .category(TransactionCategory.STEG_SONEDE)
                .categoryLabel("Factures")
                .source(BudgetTargetSource.RECOMMENDATION_AI)
                .selectedLevel(BudgetTargetLevel.EQUILIBRE)
                .status(BudgetTargetStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 4, 8, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 8, 12, 0))
                .build();
        BudgetTargetStatusUpdateRequest request = new BudgetTargetStatusUpdateRequest();
        request.setStatus(BudgetTargetStatus.ACTIVE);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(budgetTargetRepository.findByIdAndUserId(targetToActivate.getId(), user.getId()))
                .thenReturn(Optional.of(targetToActivate));
        when(budgetTargetRepository.findAllByUserIdAndCategoryAndStatusAndIdNotOrderByCreatedAtDesc(
                user.getId(),
                TransactionCategory.STEG_SONEDE,
                BudgetTargetStatus.ACTIVE,
                targetToActivate.getId()
        )).thenReturn(List.of(otherActiveTarget));
        when(budgetTargetRepository.saveAndFlush(targetToActivate)).thenAnswer(invocation -> {
            BudgetTarget savedTarget = invocation.getArgument(0);
            savedTarget.setUpdatedAt(LocalDateTime.of(2026, 4, 10, 10, 45));
            return savedTarget;
        });
        when(budgetTargetMonitoringService.buildSnapshot(eq(user.getId()), eq(targetToActivate))).thenReturn(
                new BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot(
                        null,
                        new BigDecimal("75.00"),
                        null,
                        null,
                        null,
                        null
                )
        );

        BudgetTargetResponse response = budgetTargetService.updateBudgetTargetStatus(
                targetToActivate.getId(),
                request,
                user.getEmail()
        );

        assertEquals(BudgetTargetStatus.INACTIVE, otherActiveTarget.getStatus());
        assertEquals(BudgetTargetStatus.ACTIVE, response.getStatus());
        assertEquals(0, response.getSpentThisMonth().compareTo(new BigDecimal("75.00")));
        assertNull(response.getMonitoringStatus());
        assertEquals("Renforce", response.getSelectedLevelLabel());

        verify(budgetTargetRepository).saveAll(List.of(otherActiveTarget));
        verify(budgetTargetRepository).flush();
        verify(budgetTargetRepository).saveAndFlush(targetToActivate);
    }

    private User buildUser() {
        return User.builder()
                .id(7L)
                .email("budget@test.com")
                .password("secret")
                .role(Role.USER)
                .createdAt(LocalDateTime.of(2026, 1, 5, 9, 0))
                .build();
    }
}
