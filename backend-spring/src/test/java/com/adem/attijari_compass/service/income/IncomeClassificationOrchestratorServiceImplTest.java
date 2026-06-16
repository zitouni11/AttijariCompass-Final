package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.config.IncomeMlProperties;
import com.adem.attijari_compass.dto.income.IncomeClassificationResult;
import com.adem.attijari_compass.dto.income.IncomePatternDetectionResult;
import com.adem.attijari_compass.dto.income.IncomePredictionResponse;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;
import com.adem.attijari_compass.service.IncomeMlClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomeClassificationOrchestratorServiceImplTest {

    @Test
    void shouldPreferPatternWhenPatternConfidenceBeatsAcceptedMlSalary() {
        IncomeClassificationOrchestratorServiceImpl orchestrator = orchestrator(
                new IncomePredictionResponse(IncomeTypes.SALAIRE, 0.68d),
                patternResult(IncomeTypes.SALAIRE, 0.95d, "RULE_SALARY_KEYWORD", true)
        );

        IncomeClassificationResult result = orchestrator.classifyIncome(
                snapshot("VIR SALAIRE ATTIJARI", "", 2800),
                List.of()
        );

        assertEquals(IncomeTypes.SALAIRE, result.getFinalType());
        assertEquals("PATTERN", result.getSource());
        assertEquals("RULE_SALARY_KEYWORD", result.getReason());
        assertEquals(0.95d, result.getFinalConfidence(), 0.0001d);
        assertEquals(0.68d, result.getMlConfidence(), 0.0001d);
        assertEquals(0.95d, result.getPatternConfidence(), 0.0001d);
        assertTrue(result.isPatternDetected());
    }

    @Test
    void shouldKeepMlWhenMlIsStrongerThanAcceptedPattern() {
        IncomeClassificationOrchestratorServiceImpl orchestrator = orchestrator(
                new IncomePredictionResponse(IncomeTypes.FREELANCE, 0.91d),
                patternResult(IncomeTypes.FREELANCE, 0.70d, "PATTERN_RECURRING_VARIABLE_FREELANCE", true)
        );

        IncomeClassificationResult result = orchestrator.classifyIncome(
                snapshot("Client studio", "paiement mission", 1500),
                List.of()
        );

        assertEquals(IncomeTypes.FREELANCE, result.getFinalType());
        assertEquals("ML", result.getSource());
        assertEquals("ML_CONFIDENT", result.getReason());
        assertEquals(0.91d, result.getFinalConfidence(), 0.0001d);
        assertEquals(0.0d, result.getPatternConfidence(), 0.0001d);
    }

    @Test
    void shouldPreferPatternTransferWhenMlIsAcceptedButWeaker() {
        IncomeClassificationOrchestratorServiceImpl orchestrator = orchestrator(
                new IncomePredictionResponse(IncomeTypes.TRANSFER, 0.62d),
                patternResult(IncomeTypes.TRANSFER, 0.85d, "RULE_TRANSFER_KEYWORD", false)
        );

        IncomeClassificationResult result = orchestrator.classifyIncome(
                snapshot("Virement recu Ahmed", "transfer recu de proche", 600),
                List.of()
        );

        assertEquals(IncomeTypes.TRANSFER, result.getFinalType());
        assertEquals("PATTERN", result.getSource());
        assertEquals("RULE_TRANSFER_KEYWORD", result.getReason());
        assertEquals(0.85d, result.getFinalConfidence(), 0.0001d);
    }

    @Test
    void shouldPreferPatternCashDepositWhenMlIsAcceptedButWeaker() {
        IncomeClassificationOrchestratorServiceImpl orchestrator = orchestrator(
                new IncomePredictionResponse(IncomeTypes.CASH_DEPOSIT, 0.61d),
                patternResult(IncomeTypes.CASH_DEPOSIT, 0.80d, "RULE_CASH_DEPOSIT_KEYWORD", false)
        );

        IncomeClassificationResult result = orchestrator.classifyIncome(
                snapshot("Depot espece agence centre", "versement espece guichet", 900),
                List.of()
        );

        assertEquals(IncomeTypes.CASH_DEPOSIT, result.getFinalType());
        assertEquals("PATTERN", result.getSource());
        assertEquals("RULE_CASH_DEPOSIT_KEYWORD", result.getReason());
        assertEquals(0.80d, result.getFinalConfidence(), 0.0001d);
    }

    @Test
    void shouldPreferPatternFreelanceWhenMlIsAcceptedButWeaker() {
        IncomeClassificationOrchestratorServiceImpl orchestrator = orchestrator(
                new IncomePredictionResponse(IncomeTypes.FREELANCE, 0.60d),
                patternResult(IncomeTypes.FREELANCE, 0.75d, "RULE_FREELANCE_KEYWORD", false)
        );

        IncomeClassificationResult result = orchestrator.classifyIncome(
                snapshot("Recu paiement freelance design", "mission client design", 1250),
                List.of()
        );

        assertEquals(IncomeTypes.FREELANCE, result.getFinalType());
        assertEquals("PATTERN", result.getSource());
        assertEquals("RULE_FREELANCE_KEYWORD", result.getReason());
        assertEquals(0.75d, result.getFinalConfidence(), 0.0001d);
    }

    @Test
    void shouldFallbackWhenMlAndPatternAreInsufficient() {
        IncomeClassificationOrchestratorServiceImpl orchestrator = orchestrator(
                new IncomePredictionResponse(IncomeTypes.UNKNOWN, 0.20d),
                patternResult(IncomeTypes.UNKNOWN, 0.40d, "PATTERN_NO_CLEAR_SIGNAL", false)
        );

        IncomeClassificationResult result = orchestrator.classifyIncome(
                snapshot("Credit divers", "", 500),
                List.of()
        );

        assertEquals(IncomeTypes.UNKNOWN, result.getFinalType());
        assertEquals("FALLBACK", result.getSource());
        assertEquals("INSUFFICIENT_SIGNAL", result.getReason());
        assertEquals(0.40d, result.getFinalConfidence(), 0.0001d);
    }

    private IncomeClassificationOrchestratorServiceImpl orchestrator(IncomePredictionResponse mlResponse,
                                                                     IncomePatternDetectionResult patternResult) {
        return new IncomeClassificationOrchestratorServiceImpl(
                new StubIncomeMlClient(mlResponse),
                new StubIncomePatternDetectionService(patternResult)
        );
    }

    private IncomePatternDetectionResult patternResult(String type,
                                                       double confidence,
                                                       String reason,
                                                       boolean monthlyRecurring) {
        return new IncomePatternDetectionResult(
                type,
                confidence,
                reason,
                "Pattern explanation for " + type,
                3,
                monthlyRecurring,
                0.90d,
                0.90d,
                monthlyRecurring ? "MONTHLY" : "NONE"
        );
    }

    private IncomeTransactionSnapshot snapshot(String merchantName, String description, double amount) {
        return new IncomeTransactionSnapshot(
                merchantName,
                description,
                BigDecimal.valueOf(amount),
                LocalDate.of(2026, 4, 9)
        );
    }

    private static final class StubIncomeMlClient extends IncomeMlClient {

        private final IncomePredictionResponse response;

        private StubIncomeMlClient(IncomePredictionResponse response) {
            super(defaultProperties());
            this.response = response;
        }

        @Override
        public IncomePredictionResponse predictIncomeType(String merchantName, String description) {
            return response;
        }

        private static IncomeMlProperties defaultProperties() {
            IncomeMlProperties properties = new IncomeMlProperties();
            properties.setEnabled(false);
            properties.setBaseUrl("http://127.0.0.1:8000");
            properties.setThreshold(0.6d);
            return properties;
        }
    }

    private static final class StubIncomePatternDetectionService implements IncomePatternDetectionService {

        private final IncomePatternDetectionResult result;

        private StubIncomePatternDetectionService(IncomePatternDetectionResult result) {
            this.result = result;
        }

        @Override
        public IncomePatternDetectionResult detectPattern(IncomeTransactionSnapshot currentTransaction,
                                                          List<IncomeTransactionSnapshot> historicalCredits) {
            return result;
        }
    }
}
