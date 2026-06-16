package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.service.storytelling.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class OllamaTestController {

    private final OllamaClient ollamaClient;

    @GetMapping("/ollama")
    public ResponseEntity<Map<String, Object>> testOllama(
            @RequestParam(defaultValue = "Say in one sentence that the financial assistant is connected.") String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "ollama");
        response.put("message", message);
        response.put("reply", ollamaClient.generateRawText(message));
        return ResponseEntity.ok(response);
    }
}
