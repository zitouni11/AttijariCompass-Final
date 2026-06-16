package com.adem.attijari_compass.model.storytelling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptPayload {
    private String systemPrompt;
    private String userPrompt;
}
