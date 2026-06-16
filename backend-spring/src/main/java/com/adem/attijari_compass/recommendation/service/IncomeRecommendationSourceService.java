package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.RecommendationDto;

import java.util.List;

public interface IncomeRecommendationSourceService {

    List<RecommendationDto> generateRecommendationsForUser(Long userId);

    List<RecommendationDto> generateRecommendationsForUser(String email);
}
