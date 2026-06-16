package com.adem.attijari_compass.recommendation.dto;

import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
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
public class FinancialScoreBreakdownDto {

    private Integer rawScore;
    private Integer finalScore;
    private Integer baseScore;
    private Integer penaltyPoints;
    private Integer rawBonusPoints;
    private Integer bonusPoints;
    private Boolean bonusCapApplied;
    private Integer appliedScoreCap;
    private String label;
    private Boolean criticalMonthlySituation;
    private CurrentMonthSeverity currentMonthSeverity;

    @Builder.Default
    private List<FinancialScoreFactorDto> penalties = new ArrayList<>();

    @Builder.Default
    private List<FinancialScoreFactorDto> bonuses = new ArrayList<>();
}
