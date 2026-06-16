package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.dashboard.DashboardResponse;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"", "/summary"})
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationRequiredException("Authentication is required to access dashboard data");
        }
        return ResponseEntity.ok(dashboardService.getDashboard(userDetails.getUsername(), month));
    }
}

