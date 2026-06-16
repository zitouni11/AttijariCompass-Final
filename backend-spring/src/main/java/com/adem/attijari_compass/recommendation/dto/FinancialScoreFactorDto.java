package com.adem.attijari_compass.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialScoreFactorDto {

    private String code;
    private String label;
    private Integer points;
    private String explanation;
}
