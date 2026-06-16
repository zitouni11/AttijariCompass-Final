package com.adem.attijari_compass.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCashCategoryResponse {

    private String category;
    private String categoryLabel;
    private Double amount;
    private Double share;
    private Integer transactionCount;
}
