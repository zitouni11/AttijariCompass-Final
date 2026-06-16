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
public class ReportSummaryResponse {

    private String month;
    private String monthLabel;
    private Double income;
    private Double expenses;
    private Double netBalance;
    private Double savingsRate;
    private Integer trackedTransactions;
    private Integer alertCount;
    private List<ReportCategoryResponse> categories;
    private ReportCashBreakdownResponse cashBreakdown;
}
