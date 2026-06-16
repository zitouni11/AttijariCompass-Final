package com.adem.attijari_compass.model.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmClientRequest {
    private StorytellingChatRequest request;
    private AssistantFinancialContext financialContext;
    private ConversationLanguage language;
    private PromptPayload promptPayload;
}
