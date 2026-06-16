package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.income.IncomeClassificationResult;
import com.adem.attijari_compass.dto.income.IncomeClassificationTestRequest;
import com.adem.attijari_compass.service.income.IncomeClassificationOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/income-classification")
@RequiredArgsConstructor
public class IncomeClassificationTestController {

    private final IncomeClassificationOrchestratorService incomeClassificationOrchestratorService;

    @PostMapping("/test")
    public ResponseEntity<IncomeClassificationResult> testClassification(
            @RequestBody IncomeClassificationTestRequest request
    ) {
        IncomeClassificationResult result = incomeClassificationOrchestratorService.classifyIncome(
                request.getCurrentTransaction(),
                request.getHistoricalCredits()
        );

        return ResponseEntity.ok(result);
    }
}
