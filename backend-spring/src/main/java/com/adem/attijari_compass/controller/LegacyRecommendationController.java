package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.recommendation.RecommendationResponse;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.service.RecommendationService;
import com.adem.attijari_compass.service.admin.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Deprecated
public class LegacyRecommendationController {

    private final RecommendationService recommendationService;
    private final AppSettingService appSettingService;

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (appSettingService.isMaintenanceMode()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L application est temporairement en maintenance.");
        }
        if (!appSettingService.isRecommendationsEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le module de recommandations est temporairement desactive par l administrateur.");
        }
        List<RecommendationResponse> legacyResponse = recommendationService.generateRecommendationsForUser(userDetails.getUsername())
                .getRecommendations()
                .stream()
                .map(this::mapToLegacyResponse)
                .toList();
        return ResponseEntity.ok(legacyResponse);
    }

    private RecommendationResponse mapToLegacyResponse(RecommendationDto recommendation) {
        return RecommendationResponse.builder()
                .categorie(recommendation.getCategory())
                .message(recommendation.getMessage())
                .suggestion(recommendation.getSuggestedAction())
                .gainEstimeEnMois(recommendation.getEstimatedMonthlyGain())
                .priorite(recommendation.getPriority() != null ? recommendation.getPriority().name() : null)
                .sourceType(recommendation.getSourceType())
                .build();
    }
}

