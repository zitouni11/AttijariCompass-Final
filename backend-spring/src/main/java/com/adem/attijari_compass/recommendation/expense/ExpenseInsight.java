package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseInsight {

    private String insightType;
    private TransactionCategory category;
    private ExpenseCategoryProfile profile;

    private String title;
    private String message;
    private String suggestedAction;

    private RecommendationPriority priority;
    private Double severityScore;
    private Double confidenceScore;
    private Double estimatedMonthlyGain;
    private Double targetedTransactionsTotal;

    private String explanation;

    @Builder.Default
    private List<String> basedOn = new ArrayList<>();
}
