package com.adem.attijari_compass.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCashBreakdownResponse {

    private Double totalCashExpenses;
    private Double shareOfExpenses;
    private Integer transactionCount;
    private Integer completedBreakdowns;
    private Integer pendingBreakdowns;
    private Double averageTransactionAmount;
    private List<ReportCashCategoryResponse> categories;
}
