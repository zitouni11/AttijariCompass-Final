package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.simulation.CreditSimulationRequest;
import com.adem.attijari_compass.dto.simulation.CreditSimulationResponse;
import com.adem.attijari_compass.dto.simulation.SavingsSimulationRequest;
import com.adem.attijari_compass.dto.simulation.SavingsSimulationResponse;
import com.adem.attijari_compass.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/savings")
    public ResponseEntity<SavingsSimulationResponse> simulateSavings(
            @Valid @RequestBody SavingsSimulationRequest request) {
        return ResponseEntity.ok(simulationService.simulateSavings(request));
    }

    @PostMapping("/credit")
    public ResponseEntity<CreditSimulationResponse> simulateCredit(
            @Valid @RequestBody CreditSimulationRequest request) {
        return ResponseEntity.ok(simulationService.simulateCredit(request));
    }
}

