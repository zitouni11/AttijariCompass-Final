package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeClassificationResult;
import com.adem.attijari_compass.dto.income.IncomePatternDetectionResult;
import com.adem.attijari_compass.dto.income.IncomePredictionResponse;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;
import com.adem.attijari_compass.service.IncomeMlClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class IncomeClassificationOrchestratorServiceImpl implements IncomeClassificationOrchestratorService {

    private static final String ML_SOURCE = "ML";
    private static final String PATTERN_SOURCE = "PATTERN";
    private static final String FALLBACK_SOURCE = "FALLBACK";
    private static final String ML_CONFIDENT = "ML_CONFIDENT";
    private static final String INSUFFICIENT_SIGNAL = "INSUFFICIENT_SIGNAL";
    private static final double ML_ACCEPTANCE_THRESHOLD = 0.60d;
    private static final double PATTERN_ACCEPTANCE_THRESHOLD = 0.65d;

    private final IncomeMlClient incomeMlClient;
    private final IncomePatternDetectionService incomePatternDetectionService;

    public IncomeClassificationOrchestratorServiceImpl(IncomeMlClient incomeMlClient,
                                                       IncomePatternDetectionService incomePatternDetectionService) {
        this.incomeMlClient = incomeMlClient;
        this.incomePatternDetectionService = incomePatternDetectionService;
    }

    @Override
    public IncomeClassificationResult classifyIncome(IncomeTransactionSnapshot currentTransaction,
                                                     List<IncomeTransactionSnapshot> historicalCredits) {
        if (currentTransaction == null) {
            return fallbackResult(IncomeTypes.UNKNOWN, 0.0d, null);
        }

        IncomePredictionResponse mlResponse = incomeMlClient.predictIncomeType(
                currentTransaction.getMerchantName(),
                currentTransaction.getDescription()
        );

        String mlPredictedType = normalizeType(mlResponse != null ? mlResponse.getPredictedType() : null);
        double mlConfidence = mlResponse != null ? mlResponse.getConfidence() : 0.0d;
        boolean mlAccepted = isAcceptedType(mlPredictedType, mlConfidence, ML_ACCEPTANCE_THRESHOLD);

        IncomePatternDetectionResult patternResult = incomePatternDetectionService.detectPattern(
                currentTransaction,
                historicalCredits
        );

        String patternType = patternResult != null ? normalizeType(patternResult.getDetectedType()) : IncomeTypes.UNKNOWN;
        double patternConfidence = patternResult != null ? patternResult.getConfidence() : 0.0d;
        boolean patternAccepted = patternResult != null
                && isAcceptedType(patternType, patternConfidence, PATTERN_ACCEPTANCE_THRESHOLD);

        if (shouldPreferPatternOverMl(mlAccepted, mlConfidence, patternAccepted, patternConfidence)) {
            log.debug("Income classification prioritized pattern detection over ML with type '{}' and confidence {}",
                    patternType, patternConfidence);
            return buildPatternAcceptedResult(mlPredictedType, mlConfidence, patternResult, patternType);
        }

        if (mlAccepted) {
            log.debug("Income classification accepted from ML with type '{}' and confidence {}",
                    mlPredictedType, mlConfidence);
            return buildMlAcceptedResult(mlPredictedType, mlConfidence);
        }

        if (patternAccepted) {
            log.debug("Income classification accepted from pattern detection with type '{}' and confidence {}",
                    patternType, patternConfidence);
            return buildPatternAcceptedResult(mlPredictedType, mlConfidence, patternResult, patternType);
        }

        return fallbackResult(mlPredictedType, mlConfidence, patternResult);
    }

    private IncomeClassificationResult buildMlAcceptedResult(String mlPredictedType, double mlConfidence) {
        return new IncomeClassificationResult(
                mlPredictedType,
                mlConfidence,
                ML_SOURCE,
                ML_CONFIDENT,
                "Type detecte par le moteur ML avec un niveau de confiance eleve.",
                mlPredictedType,
                mlConfidence,
                false,
                null,
                0.0d
        );
    }

    private IncomeClassificationResult buildPatternAcceptedResult(String mlPredictedType,
                                                                  double mlConfidence,
                                                                  IncomePatternDetectionResult patternResult,
                                                                  String patternType) {
        return new IncomeClassificationResult(
                patternType,
                patternResult.getConfidence(),
                PATTERN_SOURCE,
                patternResult.getReason(),
                patternResult.getExplanation(),
                mlPredictedType,
                mlConfidence,
                true,
                patternType,
                patternResult.getConfidence()
        );
    }

    private IncomeClassificationResult fallbackResult(String mlPredictedType,
                                                      double mlConfidence,
                                                      IncomePatternDetectionResult patternResult) {
        double patternConfidence = patternResult != null ? patternResult.getConfidence() : 0.0d;
        String patternType = patternResult != null ? normalizeType(patternResult.getDetectedType()) : IncomeTypes.UNKNOWN;
        boolean patternDetected = patternResult != null && patternResult.isMonthlyRecurring();

        return new IncomeClassificationResult(
                IncomeTypes.UNKNOWN,
                Math.max(mlConfidence, patternConfidence),
                FALLBACK_SOURCE,
                INSUFFICIENT_SIGNAL,
                buildFallbackExplanation(patternResult),
                normalizeType(mlPredictedType),
                mlConfidence,
                patternDetected,
                patternType,
                patternConfidence
        );
    }

    private String buildFallbackExplanation(IncomePatternDetectionResult patternResult) {
        if (patternResult == null || patternResult.getExplanation() == null || patternResult.getExplanation().isBlank()) {
            return "Aucun signal suffisamment fiable n'a permis d'identifier un type de revenu.";
        }

        return "Aucun signal suffisamment fiable n'a permis d'identifier un type de revenu. "
                + patternResult.getExplanation();
    }

    private boolean isAcceptedType(String type, double confidence, double threshold) {
        return type != null
                && !type.isBlank()
                && !IncomeTypes.UNKNOWN.equalsIgnoreCase(type)
                && confidence >= threshold;
    }

    private boolean shouldPreferPatternOverMl(boolean mlAccepted,
                                              double mlConfidence,
                                              boolean patternAccepted,
                                              double patternConfidence) {
        return mlAccepted
                && patternAccepted
                && patternConfidence >= mlConfidence;
    }

    private String normalizeType(String type) {
        return IncomeTypes.normalize(type);
    }
}
