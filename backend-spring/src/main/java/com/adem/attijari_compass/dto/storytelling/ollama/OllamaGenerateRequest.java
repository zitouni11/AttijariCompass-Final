package com.adem.attijari_compass.dto.storytelling.ollama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaGenerateRequest {
    private String model;
    private String prompt;
    private String system;
    private boolean stream;
    private String format;
}
