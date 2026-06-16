package com.adem.attijari_compass.service;

import com.adem.attijari_compass.config.CategorizationMlProperties;
import com.adem.attijari_compass.dto.categorization.MlPredictionResponse;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.model.categorization.CategorizationSources;
import com.adem.attijari_compass.util.TransactionTextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlCategorizationService {

    private final RestTemplate restTemplate;
    private final CategorizationMlProperties properties;

    public Optional<CategorizationResult> categorize(String merchantName, String description) {
        if (!properties.isEnabled()) {
            log.warn("ML categorization is disabled");
            return Optional.empty();
        }

        String normalizedText = TransactionTextNormalizer.normalize(merchantName, description);
        String url = properties.getBaseUrl() + "/predict";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("merchantName", merchantName);
            requestBody.put("description", description);

            ResponseEntity<MlPredictionResponse> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(requestBody, headers),
                    MlPredictionResponse.class
            );

            MlPredictionResponse body = response.getBody();
            if (body == null || body.category() == null || body.category().isBlank()) {
                log.warn("ML categorization returned an empty payload");
                return Optional.empty();
            }

            TransactionCategory category = TransactionCategory.fromValue(body.category());

            CategorizationResult result = CategorizationResult.builder()
                    .category(category)
                    .confidence(body.confidence())
                    .source(CategorizationSources.ML_MODEL)
                    .reason("ml_model")
                    .normalizedText(body.normalizedText() == null || body.normalizedText().isBlank()
                            ? normalizedText
                            : body.normalizedText())
                    .build();

            return Optional.of(result);
        } catch (IllegalArgumentException ex) {
            log.warn("ML categorization returned an unknown category: {}", ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.warn("ML microservice unavailable at {}: {}", properties.getBaseUrl(), ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected ML categorization error: {}", ex.getMessage(), ex);
        }

        return Optional.empty();
    }
}
