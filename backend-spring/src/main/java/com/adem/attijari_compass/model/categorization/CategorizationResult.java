package com.adem.attijari_compass.model.categorization;

import com.adem.attijari_compass.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationResult {

    private TransactionCategory category;
    private double confidence;
    private String source;
    private String reason;
    private String normalizedText;
}
