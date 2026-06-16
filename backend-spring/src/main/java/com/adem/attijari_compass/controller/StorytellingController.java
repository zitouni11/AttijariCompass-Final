package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.storytelling.MonthlyStoryResponse;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatResponse;
import com.adem.attijari_compass.service.StorytellingAssistantService;
import com.adem.attijari_compass.service.StorytellingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storytelling")
@RequiredArgsConstructor
@Slf4j
public class StorytellingController {

    private final StorytellingService storytellingService;
    private final StorytellingAssistantService storytellingAssistantService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStoryResponse> getMonthlyStory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(storytellingService.getMonthlyStory(userDetails.getUsername()));
    }

    @PostMapping("/chat")
    public ResponseEntity<StorytellingChatResponse> chat(
            @RequestBody StorytellingChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("Storytelling chat request received: userEmail={}, message='{}', userObjective='{}', historySize={}, hasFinancialContext={}",
                userEmail,
                request != null ? request.getMessage() : null,
                request != null ? request.getUserObjective() : null,
                request != null && request.getConversationHistory() != null ? request.getConversationHistory().size() : 0,
                request != null && request.getFinancialContext() != null);
        log.debug("Storytelling chat payload: {}", request);
        return ResponseEntity.ok(storytellingAssistantService.chat(request, userEmail));
    }

    @PostMapping("/chat/diagnostic")
    public ResponseEntity<Map<String, Object>> chatDiagnostic(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("Storytelling diagnostic payload received: userEmail={}, keys={}", userEmail, payload != null ? payload.keySet() : List.of());
        log.debug("Storytelling diagnostic raw payload: {}", payload);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("received", true);
        response.put("userEmail", userEmail);
        response.put("payload", payload);
        return ResponseEntity.ok(response);
    }
}

