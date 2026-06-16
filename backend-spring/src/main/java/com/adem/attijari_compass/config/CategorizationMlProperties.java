package com.adem.attijari_compass.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@ConfigurationProperties(prefix = "app.categorization.ml")
public class CategorizationMlProperties {

    private boolean enabled = true;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double threshold = 0.65d;

    @NotBlank
    private String baseUrl = "http://localhost:8001";
}
