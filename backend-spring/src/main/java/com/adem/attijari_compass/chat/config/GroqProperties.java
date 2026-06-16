package com.adem.attijari_compass.chat.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "groq.api")
public class GroqProperties {

    private String key = "";

    @NotBlank
    private String url = "https://api.groq.com/openai/v1/chat/completions";

    @NotBlank
    private String model = "openai/gpt-oss-20b";

    @NotBlank
    private String advancedModel = "openai/gpt-oss-120b";

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private double temperature = 0.3d;

    @Min(1)
    private int maxTokens = 900;

    @Min(1)
    private int timeoutSeconds = 45;
}
