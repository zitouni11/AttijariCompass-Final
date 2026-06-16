package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;

public interface RecommendationService {

    RecommendationResponseDto generateRecommendationsForUser(Long userId);

    RecommendationResponseDto generateRecommendationsForUser(String email);
}
