package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.budget.BudgetTargetCreateRequest;
import com.adem.attijari_compass.dto.budget.BudgetAlertResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetResponse;
import com.adem.attijari_compass.dto.budget.BudgetTargetStatusUpdateRequest;
import com.adem.attijari_compass.service.BudgetTargetAlertService;
import com.adem.attijari_compass.service.BudgetTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budget-targets")
@RequiredArgsConstructor
public class BudgetTargetController {

    private final BudgetTargetService budgetTargetService;
    private final BudgetTargetAlertService budgetTargetAlertService;

    @PostMapping
    public ResponseEntity<BudgetTargetResponse> createBudgetTarget(
            @Valid @RequestBody BudgetTargetCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetTargetService.createBudgetTarget(request, userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BudgetTargetResponse>> getMyActiveBudgetTargets(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(budgetTargetService.getActiveBudgetTargetsForCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/my/alerts")
    public ResponseEntity<List<BudgetAlertResponse>> getMyBudgetAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(budgetTargetAlertService.getAlertsForCurrentUser(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BudgetTargetResponse> updateBudgetTargetStatus(
            @PathVariable Long id,
            @Valid @RequestBody BudgetTargetStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(budgetTargetService.updateBudgetTargetStatus(id, request, userDetails.getUsername()));
    }
}
