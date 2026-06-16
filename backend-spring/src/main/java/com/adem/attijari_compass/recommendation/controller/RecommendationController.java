package com.adem.attijari_compass.recommendation.controller;

import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.recommendation.dto.RecommendationExplanationRequest;
import com.adem.attijari_compass.recommendation.dto.RecommendationExplanationResponse;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.service.RecommendationExplanationService;
import com.adem.attijari_compass.recommendation.service.RecommendationService;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.admin.AppSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationExplanationService recommendationExplanationService;
    private final AppSettingService appSettingService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<RecommendationResponseDto> getMyRecommendations(Authentication authentication) {
        ensureRecommendationsAvailable();
        String userEmail = authentication.getName();
        return ResponseEntity.ok(recommendationService.generateRecommendationsForUser(userEmail));
    }

    @PostMapping("/my/explanation")
    public ResponseEntity<RecommendationExplanationResponse> explainMyRecommendation(
            Authentication authentication,
            @Valid @RequestBody RecommendationExplanationRequest request
    ) {
        ensureRecommendationsAvailable();
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(recommendationExplanationService.generateRecommendationExplanation(user.getId(), request));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecommendationResponseDto> getRecommendationsForUser(@PathVariable Long userId) {
        // Protected endpoint: only administrators should access recommendations for another user.
        return ResponseEntity.ok(recommendationService.generateRecommendationsForUser(userId));
    }

    @PostMapping("/user/{userId}/explanation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecommendationExplanationResponse> explainRecommendationForUser(
            @PathVariable Long userId,
            @Valid @RequestBody RecommendationExplanationRequest request
    ) {
        return ResponseEntity.ok(recommendationExplanationService.generateRecommendationExplanation(userId, request));
    }

    private void ensureRecommendationsAvailable() {
        if (appSettingService.isMaintenanceMode()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L application est temporairement en maintenance.");
        }

        if (!appSettingService.isRecommendationsEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le module de recommandations est temporairement desactive par l administrateur.");
        }
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AuthenticationRequiredException("Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AuthenticationRequiredException("Authenticated user could not be resolved"));
    }
}
