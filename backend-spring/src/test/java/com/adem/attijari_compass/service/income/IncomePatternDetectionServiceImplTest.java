package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomePatternDetectionResult;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomePatternDetectionServiceImplTest {

    private final IncomePatternDetectionServiceImpl service = new IncomePatternDetectionServiceImpl();

    @Test
    void shouldDetectSalaryFromExplicitKeywordsWithoutHistory() {
        IncomeTransactionSnapshot currentTransaction = snapshot(
                "VIR SALAIRE ATTIJARI",
                "",
                2800,
                LocalDate.of(2026, 4, 9)
        );

        IncomePatternDetectionResult result = service.detectPattern(currentTransaction, List.of());

        assertEquals(IncomeTypes.SALAIRE, result.getDetectedType());
        assertEquals("RULE_SALARY_KEYWORD", result.getReason());
        assertTrue(result.getConfidence() >= 0.95d);
    }

    @Test
    void shouldDetectSalaryFromPaieKeywordVariants() {
        IncomeTransactionSnapshot currentTransaction = snapshot(
                "Vir paie mensuelle",
                "",
                3100,
                LocalDate.of(2026, 4, 9)
        );

        IncomePatternDetectionResult result = service.detectPattern(currentTransaction, List.of());

        assertEquals(IncomeTypes.SALAIRE, result.getDetectedType());
        assertEquals("RULE_SALARY_KEYWORD", result.getReason());
        assertTrue(result.getConfidence() >= 0.95d);
    }

    @Test
    void shouldDetectTransferFromExplicitKeywordsWithoutHistory() {
        IncomeTransactionSnapshot currentTransaction = snapshot(
                "Virement recu Ahmed",
                "transfer recu de proche",
                600,
                LocalDate.of(2026, 4, 9)
        );

        IncomePatternDetectionResult result = service.detectPattern(currentTransaction, List.of());

        assertEquals(IncomeTypes.TRANSFER, result.getDetectedType());
        assertEquals("RULE_TRANSFER_KEYWORD", result.getReason());
        assertTrue(result.getConfidence() >= 0.85d);
    }

    @Test
    void shouldDetectCashDepositFromExplicitKeywordsWithoutHistory() {
        IncomeTransactionSnapshot currentTransaction = snapshot(
                "Depot espece agence centre",
                "versement espece guichet",
                900,
                LocalDate.of(2026, 4, 9)
        );

        IncomePatternDetectionResult result = service.detectPattern(currentTransaction, List.of());

        assertEquals(IncomeTypes.CASH_DEPOSIT, result.getDetectedType());
        assertEquals("RULE_CASH_DEPOSIT_KEYWORD", result.getReason());
        assertTrue(result.getConfidence() >= 0.80d);
    }

    @Test
    void shouldDetectFreelanceFromExplicitKeywordsWithoutHistory() {
        IncomeTransactionSnapshot currentTransaction = new IncomeTransactionSnapshot(
                "Recu paiement freelance design",
                "mission client design",
                BigDecimal.valueOf(1250),
                LocalDate.of(2026, 4, 9)
        );

        IncomePatternDetectionResult result = service.detectPattern(currentTransaction, List.of());

        assertEquals(IncomeTypes.FREELANCE, result.getDetectedType());
        assertEquals("RULE_FREELANCE_KEYWORD", result.getReason());
        assertTrue(result.getConfidence() >= 0.75d);
    }

    private IncomeTransactionSnapshot snapshot(String merchantName,
                                               String description,
                                               double amount,
                                               LocalDate transactionDate) {
        return new IncomeTransactionSnapshot(
                merchantName,
                description,
                BigDecimal.valueOf(amount),
                transactionDate
        );
    }
}
