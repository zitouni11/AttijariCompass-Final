package com.adem.attijari_compass.service;

import com.adem.attijari_compass.config.IncomeMlProperties;
import com.adem.attijari_compass.dto.income.IncomePredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class IncomeMlClient {

    private static final String UNKNOWN = "unknown";
    private static final double UNKNOWN_CONFIDENCE = 0.0d;

    private final WebClient webClient;
    private final IncomeMlProperties properties;

    public IncomeMlClient(IncomeMlProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public IncomePredictionResponse predictIncomeType(String merchantName, String description) {
        if (!properties.isEnabled()) {
            log.warn("Income ML service is disabled. Using fallback response.");
            return fallbackResponse();
        }

        try {
            String safeMerchantName = merchantName == null ? "" : merchantName.trim();
            String safeDescription = description == null ? "" : description.trim();
            if (safeMerchantName.isBlank() && safeDescription.isBlank()) {
                log.warn("Income ML skipped because merchantName and description are both empty.");
                return fallbackResponse();
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantName", safeMerchantName);
            payload.put("description", safeDescription);
            payload.put("threshold", properties.getThreshold());

            log.info("Calling income ML service at {}/predict-income-type", properties.getBaseUrl());
            log.info("Payload sent to FastAPI = {}", payload);

            IncomePredictionResponse response = webClient.post()
                    .uri("/predict-income-type")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(IncomePredictionResponse.class)
                    .block();

            log.info("Income ML response body = {}", response);

            if (response == null || response.getPredictedType() == null || response.getPredictedType().isBlank()) {
                log.warn("Income ML returned an empty response body. Using fallback response.");
                return fallbackResponse();
            }

            return response;

        } catch (Exception ex) {
            log.error("Income ML request failed", ex);
            return fallbackResponse();
        }
    }

    private IncomePredictionResponse fallbackResponse() {
        return new IncomePredictionResponse(UNKNOWN, UNKNOWN_CONFIDENCE);
    }
}