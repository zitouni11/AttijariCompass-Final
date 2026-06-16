package com.adem.attijari_compass.simulations.controller;

import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditCompareRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsCompareRequest;
import com.adem.attijari_compass.simulations.dto.response.CreditCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.CreditCompareResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsCompareResponse;
import com.adem.attijari_compass.simulations.service.CreditSimulationService;
import com.adem.attijari_compass.simulations.service.SavingsSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulations", description = "Projection endpoints for savings and credit scenarios")
public class SimulationCalculatorController {

    private final SavingsSimulationService savingsSimulationService;
    private final CreditSimulationService creditSimulationService;

    @PostMapping("/savings/calculate")
    @Operation(summary = "Calculate a savings projection")
    public ResponseEntity<SavingsCalculateResponse> calculateSavings(@Valid @RequestBody SavingsCalculateRequest request) {
        return ResponseEntity.ok(savingsSimulationService.calculate(request));
    }

    @PostMapping("/savings/compare")
    @Operation(summary = "Compare up to three savings scenarios")
    public ResponseEntity<SavingsCompareResponse> compareSavings(@Valid @RequestBody SavingsCompareRequest request) {
        return ResponseEntity.ok(savingsSimulationService.compare(request));
    }

    @PostMapping("/credit/calculate")
    @Operation(summary = "Calculate a credit projection")
    public ResponseEntity<CreditCalculateResponse> calculateCredit(@Valid @RequestBody CreditCalculateRequest request) {
        return ResponseEntity.ok(creditSimulationService.calculate(request));
    }

    @PostMapping("/credit/compare")
    @Operation(summary = "Compare up to three credit scenarios")
    public ResponseEntity<CreditCompareResponse> compareCredit(@Valid @RequestBody CreditCompareRequest request) {
        return ResponseEntity.ok(creditSimulationService.compare(request));
    }
}
