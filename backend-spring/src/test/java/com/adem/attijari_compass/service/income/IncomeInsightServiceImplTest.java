package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeClassifiedTransaction;
import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomeInsightServiceImplTest {

    private final IncomeInsightServiceImpl service = new IncomeInsightServiceImpl();

    @Test
    void shouldDetectStableSalaryDominance() {
        IncomeInsightResponse response = service.analyze(List.of(
                income("salary", 0.94d, 3200, LocalDate.of(2026, 1, 28)),
                income("salaire", 0.93d, 3200, LocalDate.of(2026, 2, 28)),
                income("payroll", 0.95d, 3200, LocalDate.of(2026, 3, 28)),
                income("salaire", 0.96d, 3205, LocalDate.of(2026, 4, 28))
        ));

        assertEquals(IncomeTypes.SALAIRE, response.getPrimaryIncomeType());
        assertEquals("STABLE", response.getIncomeStability());
        assertEquals("MONTHLY", response.getIncomeRegularity());
        assertEquals(4, response.getSalaryLikeCount());
        assertEquals(0, response.getOtherIncomeCount());
        assertEquals(1.0d, response.getDominantIncomeShare(), 0.0001d);
        assertFalse(response.getHasSecondaryIncome());
        assertTrue(response.getIncomeConfidenceScore() >= 90);
        assertTrue(response.getInsightSummary().contains("salaire mensuel"));
    }

    @Test
    void shouldDetectVariableIncomeWhenFreelanceAndTransferDominate() {
        IncomeInsightResponse response = service.analyze(List.of(
                income("freelance", 0.80d, 1200, LocalDate.of(2026, 1, 7)),
                income("freelance", 0.78d, 1900, LocalDate.of(2026, 2, 19)),
                income("freelance", 0.82d, 1450, LocalDate.of(2026, 4, 3)),
                income("transfer", 0.85d, 600, LocalDate.of(2026, 1, 15)),
                income("virement", 0.83d, 500, LocalDate.of(2026, 3, 9))
        ));

        assertEquals(IncomeTypes.FREELANCE, response.getPrimaryIncomeType());
        assertEquals("VARIABLE", response.getIncomeStability());
        assertEquals("IRREGULAR", response.getIncomeRegularity());
        assertEquals(3, response.getFreelanceLikeCount());
        assertEquals(2, response.getTransferLikeCount());
        assertTrue(response.getHasSecondaryIncome());
        assertTrue(response.getDominantIncomeShare() >= 0.60d);
        assertTrue(response.getInsightSummary().contains("variables"));
    }

    @Test
    void shouldReturnUndefinedWhenDataIsInsufficient() {
        IncomeInsightResponse response = service.analyze(List.of(
                income("unknown", 0.40d, 300, LocalDate.of(2026, 4, 8)),
                income("unknown", 0.35d, 250, LocalDate.of(2026, 4, 9))
        ));

        assertEquals(IncomeTypes.UNKNOWN, response.getPrimaryIncomeType());
        assertEquals("UNDEFINED", response.getIncomeStability());
        assertEquals("UNKNOWN", response.getIncomeRegularity());
        assertEquals(2, response.getOtherIncomeCount());
        assertFalse(response.getHasSecondaryIncome());
        assertEquals(0, response.getIncomeConfidenceScore());
        assertTrue(response.getInsightSummary().contains("insuffisantes"));
    }

    private IncomeClassifiedTransaction income(String type, double confidence, double amount, LocalDate date) {
        return new IncomeClassifiedTransaction(
                type,
                confidence,
                BigDecimal.valueOf(amount),
                date,
                "PATTERN"
        );
    }
}
