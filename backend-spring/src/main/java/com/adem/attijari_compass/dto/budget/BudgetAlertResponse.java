package com.adem.attijari_compass.dto.budget;

import com.adem.attijari_compass.entity.BudgetAlertSeverity;
import com.adem.attijari_compass.entity.BudgetAlertType;
import com.adem.attijari_compass.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlertResponse {

    private BudgetAlertType alertType;
    private String alertTypeLabel;
    private BudgetAlertSeverity severity;
    private String severityLabel;
    private Long budgetTargetId;
    private TransactionCategory category;
    private String categoryLabel;
    private String title;
    private String message;
    private BigDecimal usagePercent;
    private BigDecimal targetAmount;
    private BigDecimal spentThisMonth;
    private BigDecimal remainingAmount;
    private Integer priorityRank;
    private LocalDateTime generatedAt;
}
