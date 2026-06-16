package com.adem.attijari_compass.dto.budget;

import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import com.adem.attijari_compass.entity.BudgetTargetSource;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
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
public class BudgetTargetResponse {

    private Long id;
    private TransactionCategory category;
    private String categoryLabel;
    private BigDecimal targetAmount;
    private BudgetTargetLevel selectedLevel;
    private String selectedLevelLabel;
    private String selectedLevelSummary;
    private BigDecimal suggestedMonthlyAmount;
    private BigDecimal spentThisMonth;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercent;
    private BudgetMonitoringStatus monitoringStatus;
    private String monitoringStatusLabel;
    private BudgetTargetSource source;
    private String sourceLabel;
    private String recommendationId;
    private String recommendationTitle;
    private String summary;
    private BudgetTargetStatus status;
    private String statusLabel;
    private boolean aiGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
