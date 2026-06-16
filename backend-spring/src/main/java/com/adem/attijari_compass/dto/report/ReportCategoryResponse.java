package com.adem.attijari_compass.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCategoryResponse {

    private String category;
    private String categoryLabel;
    private String icon;
    private Double budget;
    private Double spent;
    private Double usagePercent;
    private Double remainingAmount;
    private String advice;
    private String status;
}
