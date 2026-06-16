package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;

import java.util.List;

public interface RecommendationEngineService {

    List<RecommendationDto> generateRecommendations(FinancialInsightDto insight);
}
