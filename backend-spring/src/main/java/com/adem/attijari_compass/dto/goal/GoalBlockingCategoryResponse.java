package com.adem.attijari_compass.dto.goal;

import com.adem.attijari_compass.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalBlockingCategoryResponse {
    private TransactionCategory category;
    private String categoryLabel;
    private Double averageMonthlyAmount;
    private Double estimatedReducibleAmount;
    private Double reductionRate;
    private String severity;
    private String severityLabel;
    private String displayLabel;
    private String reason;
}
