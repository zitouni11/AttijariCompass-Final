package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.report.ReportSummaryResponse;
import com.adem.attijari_compass.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryResponse> getSummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reportService.getSummary(userDetails.getUsername(), month));
    }
}
