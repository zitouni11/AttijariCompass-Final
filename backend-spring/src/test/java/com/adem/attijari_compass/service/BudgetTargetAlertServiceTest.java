package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.budget.BudgetAlertResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetResponse;
import com.adem.attijari_compass.entity.BudgetAlertSeverity;
import com.adem.attijari_compass.entity.BudgetAlertType;
import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetTargetSource;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.TransactionCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetTargetAlertServiceTest {

    @Mock
    private BudgetTargetService budgetTargetService;

    @InjectMocks
    private BudgetTargetAlertService budgetTargetAlertService;

    @Test
    void shouldGeneratePriorityExceededAndQuasiAlertsInSeverityOrder() {
        BudgetTargetResponse shopping = buildBudget(
                1L,
                TransactionCategory.SHOPPING,
                "Shopping",
                "620.00",
                "725.00",
                "-105.00",
                "116.94",
                BudgetMonitoringStatus.DEPASSE
        );
        BudgetTargetResponse loisir = buildBudget(
                2L,
                TransactionCategory.DIVERTISSEMENT,
                "Loisirs",
                "300.00",
                "267.00",
                "33.00",
                "89.00",
                BudgetMonitoringStatus.A_SURVEILLER
        );

        when(budgetTargetService.getActiveBudgetTargetsForCurrentUser("alerts@test.com"))
                .thenReturn(List.of(shopping, loisir));

        List<BudgetAlertResponse> alerts = budgetTargetAlertService.getAlertsForCurrentUser("alerts@test.com");

        assertEquals(3, alerts.size());
        assertEquals(BudgetAlertType.BUDGET_CRITIQUE_PRIORITAIRE, alerts.getFirst().getAlertType());
        assertEquals(BudgetAlertSeverity.CRITICAL, alerts.getFirst().getSeverity());
        assertEquals(shopping.getId(), alerts.getFirst().getBudgetTargetId());

        assertEquals(BudgetAlertType.BUDGET_DEPASSE, alerts.get(1).getAlertType());
        assertEquals(BudgetAlertSeverity.CRITICAL, alerts.get(1).getSeverity());
        assertEquals(shopping.getId(), alerts.get(1).getBudgetTargetId());
        assertTrue(alerts.get(1).getMessage().contains("Depassement estime"));

        assertEquals(BudgetAlertType.BUDGET_QUASI_ATTEINT, alerts.get(2).getAlertType());
        assertEquals(BudgetAlertSeverity.WARNING, alerts.get(2).getSeverity());
        assertEquals(loisir.getId(), alerts.get(2).getBudgetTargetId());
    }

    @Test
    void shouldGenerateGlobalMasteryAlertWhenAllBudgetsAreHealthy() {
        BudgetTargetResponse transport = buildBudget(
                3L,
                TransactionCategory.TRANSPORT,
                "Transport",
                "250.00",
                "90.00",
                "160.00",
                "36.00",
                BudgetMonitoringStatus.SOUS_CONTROLE
        );
        BudgetTargetResponse factures = buildBudget(
                4L,
                TransactionCategory.STEG_SONEDE,
                "Factures",
                "400.00",
                "140.00",
                "260.00",
                "35.00",
                BudgetMonitoringStatus.SOUS_CONTROLE
        );

        when(budgetTargetService.getActiveBudgetTargetsForCurrentUser("healthy@test.com"))
                .thenReturn(List.of(transport, factures));

        List<BudgetAlertResponse> alerts = budgetTargetAlertService.getAlertsForCurrentUser("healthy@test.com");

        assertEquals(1, alerts.size());
        assertEquals(BudgetAlertType.BUDGET_MAITRISE_GLOBALE, alerts.getFirst().getAlertType());
        assertEquals(BudgetAlertSeverity.INFO, alerts.getFirst().getSeverity());
        assertNotNull(alerts.getFirst().getGeneratedAt());
    }

    @Test
    void shouldGenerateLowRemainingAlertWithoutDuplicatingQuasiThreshold() {
        BudgetTargetResponse budget = buildBudget(
                5L,
                TransactionCategory.ALIMENTATION,
                "Alimentation",
                "200.00",
                "172.00",
                "28.00",
                "72.00",
                BudgetMonitoringStatus.A_SURVEILLER
        );

        when(budgetTargetService.getActiveBudgetTargetsForCurrentUser("low@test.com"))
                .thenReturn(List.of(budget));

        List<BudgetAlertResponse> alerts = budgetTargetAlertService.getAlertsForCurrentUser("low@test.com");

        assertEquals(1, alerts.size());
        assertEquals(BudgetAlertType.BUDGET_RESTE_FAIBLE, alerts.getFirst().getAlertType());
        assertEquals(BudgetAlertSeverity.WARNING, alerts.getFirst().getSeverity());
        assertEquals(0, alerts.getFirst().getRemainingAmount().compareTo(new BigDecimal("28.00")));
    }

    private BudgetTargetResponse buildBudget(Long id,
                                             TransactionCategory category,
                                             String categoryLabel,
                                             String targetAmount,
                                             String spentThisMonth,
                                             String remainingAmount,
                                             String usagePercent,
                                             BudgetMonitoringStatus monitoringStatus) {
        return BudgetTargetResponse.builder()
                .id(id)
                .category(category)
                .categoryLabel(categoryLabel)
                .targetAmount(new BigDecimal(targetAmount))
                .selectedLevel(BudgetTargetLevel.EQUILIBRE)
                .selectedLevelLabel("Equilibre")
                .suggestedMonthlyAmount(new BigDecimal(targetAmount))
                .spentThisMonth(new BigDecimal(spentThisMonth))
                .remainingAmount(new BigDecimal(remainingAmount))
                .usagePercent(new BigDecimal(usagePercent))
                .monitoringStatus(monitoringStatus)
                .monitoringStatusLabel(monitoringStatus != null ? monitoringStatus.getLabel() : null)
                .source(BudgetTargetSource.RECOMMENDATION_AI)
                .sourceLabel("Recommendation IA")
                .status(BudgetTargetStatus.ACTIVE)
                .statusLabel("Actif")
                .aiGenerated(true)
                .createdAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .build();
    }
}
