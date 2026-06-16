package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatResponse;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectAssistantResponseServiceTest {

    private final DirectAssistantResponseService service = new DirectAssistantResponseService();

    @Test
    void shouldAnswerSalaryDirectly() {
        StorytellingChatResponse response = service.respond(
                AssistantIntent.SALARY_INFO,
                ConversationLanguage.FRENCH,
                AssistantFinancialContext.builder()
                        .salary(BigDecimal.valueOf(4200))
                        .currency("DT")
                        .build()
        );

        assertEquals(AssistantIntent.SALARY_INFO, response.getIntent());
        assertTrue(response.getReply().contains("4200"));
    }

    @Test
    void shouldClarifyUnknownRequests() {
        StorytellingChatResponse response = service.respond(
                AssistantIntent.UNKNOWN,
                ConversationLanguage.FRENCH,
                AssistantFinancialContext.builder().build()
        );

        assertEquals(AssistantIntent.UNKNOWN, response.getIntent());
        assertTrue(response.getReply().contains("Je n'ai pas identifié"));
    }
}
