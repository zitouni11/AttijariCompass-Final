package com.adem.attijari_compass.dto.storytelling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorytellingChatRequest {

    private String message;

    private String userObjective;

    private List<ConversationMessageDto> conversationHistory;

    private FinancialContextDto financialContext;
}
