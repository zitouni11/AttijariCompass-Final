package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetTargetSource;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.repository.CategoryExpenseTotalProjection;
import com.adem.attijari_compass.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetTargetMonitoringServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetTargetMonitoringService budgetTargetMonitoringService;

    @Test
    void shouldBuildMonitoringSnapshotsForActiveBudgets() {
        BudgetTarget shoppingBudget = buildBudgetTarget(1L, TransactionCategory.SHOPPING, "620.00");
        BudgetTarget transportBudget = buildBudgetTarget(2L, TransactionCategory.TRANSPORT, "200.00");

        when(transactionRepository.sumAbsoluteAmountByUserIdAndTypeAndDateBetweenAndCategoryIn(
                eq(7L),
                eq(TransactionType.DEPENSE),
                any(),
                any(),
                any()
        )).thenReturn(List.of(
                projection(TransactionCategory.SHOPPING, 480.0),
                projection(TransactionCategory.TRANSPORT, 240.0)
        ));

        Map<Long, BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot> snapshots =
                budgetTargetMonitoringService.buildSnapshots(7L, List.of(shoppingBudget, transportBudget));

        BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot shoppingSnapshot = snapshots.get(1L);
        BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot transportSnapshot = snapshots.get(2L);

        assertEquals(0, shoppingSnapshot.targetAmount().compareTo(new BigDecimal("620.00")));
        assertEquals(0, shoppingSnapshot.spentThisMonth().compareTo(new BigDecimal("480.00")));
        assertEquals(0, shoppingSnapshot.remainingAmount().compareTo(new BigDecimal("140.00")));
        assertEquals(0, shoppingSnapshot.usagePercent().compareTo(new BigDecimal("77.42")));
        assertEquals(BudgetMonitoringStatus.A_SURVEILLER, shoppingSnapshot.monitoringStatus());

        assertEquals(0, transportSnapshot.targetAmount().compareTo(new BigDecimal("200.00")));
        assertEquals(0, transportSnapshot.spentThisMonth().compareTo(new BigDecimal("240.00")));
        assertEquals(0, transportSnapshot.remainingAmount().compareTo(new BigDecimal("-40.00")));
        assertEquals(0, transportSnapshot.usagePercent().compareTo(new BigDecimal("120.00")));
        assertEquals(BudgetMonitoringStatus.DEPASSE, transportSnapshot.monitoringStatus());
    }

    @Test
    void shouldHandleBudgetWithoutTargetAmount() {
        BudgetTarget budgetTarget = BudgetTarget.builder()
                .id(3L)
                .category(TransactionCategory.DIVERTISSEMENT)
                .categoryLabel("Loisirs")
                .source(BudgetTargetSource.MANUAL)
                .selectedLevel(BudgetTargetLevel.PRUDENT)
                .status(BudgetTargetStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .build();

        when(transactionRepository.sumAbsoluteAmountByUserIdAndTypeAndDateBetweenAndCategoryIn(
                eq(7L),
                eq(TransactionType.DEPENSE),
                any(),
                any(),
                any()
        )).thenReturn(List.of(
                projection(TransactionCategory.DIVERTISSEMENT, 85.0)
        ));

        BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot snapshot =
                budgetTargetMonitoringService.buildSnapshot(7L, budgetTarget);

        assertNull(snapshot.targetAmount());
        assertEquals(0, snapshot.spentThisMonth().compareTo(new BigDecimal("85.00")));
        assertNull(snapshot.remainingAmount());
        assertNull(snapshot.usagePercent());
        assertNull(snapshot.monitoringStatus());
    }

    @Test
    void shouldTreatZeroBudgetAsExceededWhenThereIsSpending() {
        BudgetTarget budgetTarget = buildBudgetTarget(4L, TransactionCategory.STEG_SONEDE, "0.00");

        when(transactionRepository.sumAbsoluteAmountByUserIdAndTypeAndDateBetweenAndCategoryIn(
                eq(7L),
                eq(TransactionType.DEPENSE),
                any(),
                any(),
                any()
        )).thenReturn(List.of(
                projection(TransactionCategory.STEG_SONEDE, 15.0)
        ));

        BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot snapshot =
                budgetTargetMonitoringService.buildSnapshot(7L, budgetTarget);

        assertEquals(0, snapshot.targetAmount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.spentThisMonth().compareTo(new BigDecimal("15.00")));
        assertEquals(0, snapshot.remainingAmount().compareTo(new BigDecimal("-15.00")));
        assertEquals(0, snapshot.usagePercent().compareTo(new BigDecimal("100.00")));
        assertEquals(BudgetMonitoringStatus.DEPASSE, snapshot.monitoringStatus());
    }

    private BudgetTarget buildBudgetTarget(Long id, TransactionCategory category, String targetAmount) {
        return BudgetTarget.builder()
                .id(id)
                .category(category)
                .categoryLabel(category.name())
                .suggestedMonthlyAmount(new BigDecimal(targetAmount))
                .source(BudgetTargetSource.RECOMMENDATION_AI)
                .selectedLevel(BudgetTargetLevel.EQUILIBRE)
                .status(BudgetTargetStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .build();
    }

    private CategoryExpenseTotalProjection projection(TransactionCategory category, Double totalAmount) {
        return new CategoryExpenseTotalProjection() {
            @Override
            public TransactionCategory getCategory() {
                return category;
            }

            @Override
            public Double getTotalAmount() {
                return totalAmount;
            }
        };
    }
}
