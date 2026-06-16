package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.categorization.CategorizationPredictRequest;
import com.adem.attijari_compass.dto.categorization.CategorizationPredictResponse;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.service.SmartCategorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorization")
@RequiredArgsConstructor
public class CategorizationController {

    private final SmartCategorizationService smartCategorizationService;
    private final UserRepository userRepository;

    @PostMapping("/predict")
    public ResponseEntity<CategorizationPredictResponse> predict(
            @Valid @RequestBody CategorizationPredictRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = userDetails == null
                ? null
                : userRepository.findByEmail(userDetails.getUsername())
                .map(user -> user.getId())
                .orElse(null);

        CategorizationResult result = smartCategorizationService.categorize(
                request.merchantName(),
                request.description(),
                userId
        );

        return ResponseEntity.ok(new CategorizationPredictResponse(
                result.getCategory().name(),
                result.getConfidence(),
                result.getSource(),
                result.getReason(),
                result.getNormalizedText()
        ));
    }
}
