package com.adem.attijari_compass.dto.admin.decision;

import java.util.List;

public record TransactionSourceDecisionDto(
        long totalTransactions,
        List<TransactionSourceMetricDto> sources,
        String dominantSource,
        double digitalisationRate,
        double digitalisationTarget,
        double digitalisationGap,
        String priorityLevel,
        long digitalTransactions,
        TransactionSourceDiagnosticDto diagnostic,
        TransactionSourceAnalysisDto analyse,
        TransactionSourceReasoningDto reasoning,
        TransactionSourceImpactDto impact,
        TransactionSourceStrategicDecisionDto strategicDecision,
        List<TransactionSourceActionPlanItemDto> actionPlan,
        String executiveConclusion,
        List<String> recommendedActions,
        String message
) {
}
