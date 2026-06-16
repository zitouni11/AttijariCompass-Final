package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;

import java.util.List;

public interface RecommendationNarrativeService {

    RecommendationSummaryDto buildSummary(
            FinancialInsightDto insight,
            FinancialScoreBreakdownDto scoreBreakdown,
            List<RecommendationDto> recommendations
    );
}
