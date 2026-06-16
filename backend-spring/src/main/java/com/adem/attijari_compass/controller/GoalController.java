package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.dto.goal.GoalAiRefreshResponse;
import com.adem.attijari_compass.dto.goal.GoalPredictionResponse;
import com.adem.attijari_compass.dto.goal.GoalRecommendationResponse;
import com.adem.attijari_compass.dto.goal.GoalRequest;
import com.adem.attijari_compass.dto.goal.GoalResponse;
import com.adem.attijari_compass.dto.goal.GoalScenarioResponse;
import com.adem.attijari_compass.dto.goal.GoalStorytellingResponse;
import com.adem.attijari_compass.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@Slf4j
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @Valid @RequestBody GoalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.createGoal(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getAllGoals(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getAllGoals(userDetails.getUsername()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GoalResponse>> getGoalsByUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoalsByUser(userId, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalAiRefreshResponse> getGoalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoalAiRefresh(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalAiRefreshResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Goal update endpoint called: goalId={}, userEmail={}, targetAmount={}, currentAmount={}, targetDate={}",
                id,
                userDetails.getUsername(),
                request.getTargetAmount(),
                request.getCurrentAmount(),
                request.getTargetDate());
        return ResponseEntity.ok(goalService.updateGoalAndRefreshAi(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<GoalResponse> addProgress(
            @PathVariable Long id,
            @RequestParam Double amount,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.addProgress(id, amount, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        goalService.deleteGoal(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<GoalAnalysisResponse> getGoalAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoalAnalysis(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/prediction")
    public ResponseEntity<GoalPredictionResponse> getGoalPrediction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoalPrediction(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/simulations")
    public ResponseEntity<List<GoalScenarioResponse>> getGoalSimulations(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoalSimulations(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<GoalRecommendationResponse>> getGoalRecommendations(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoalRecommendations(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/storytelling")
    public ResponseEntity<GoalStorytellingResponse> generateGoalStorytelling(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.generateGoalStorytelling(id, userDetails.getUsername()));
    }
}
