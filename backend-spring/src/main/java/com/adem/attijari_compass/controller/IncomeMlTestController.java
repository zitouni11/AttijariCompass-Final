package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.income.IncomePredictionResponse;
import com.adem.attijari_compass.service.IncomeMlClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/income-ml")
@RequiredArgsConstructor
public class IncomeMlTestController {

    private final IncomeMlClient incomeMlClient;

    @GetMapping("/test")
    public IncomePredictionResponse testPrediction(
            @RequestParam String merchantName,
            @RequestParam(defaultValue = "") String description
    ) {
        return incomeMlClient.predictIncomeType(merchantName, description);
    }
}
