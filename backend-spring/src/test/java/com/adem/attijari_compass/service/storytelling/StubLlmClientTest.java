package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import com.adem.attijari_compass.model.storytelling.LlmClientRequest;
import com.adem.attijari_compass.model.storytelling.LlmClientResponse;
import com.adem.attijari_compass.model.storytelling.PromptPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubLlmClientTest {

    private final StubLlmClient stubLlmClient = new StubLlmClient();

    @Test
    void shouldAnswerInFrenchForSavingsGoal() {
        LlmClientResponse response = stubLlmClient.generateResponse(baseRequest(
                ConversationLanguage.FRENCH,
                StorytellingChatRequest.builder()
                        .message("Je veux commencer une epargne")
                        .userObjective("un fonds d'urgence")
                        .build(),
                AssistantFinancialContext.builder()
                        .income(BigDecimal.valueOf(3000))
                        .expenses(BigDecimal.valueOf(2200))
                        .currency("DT")
                        .build()
        ));

        assertEquals(AssistantEmotion.ENCOURAGING, response.getEmotion());
        assertEquals(AssistantAction.SHOW_SAVINGS_PLAN, response.getAction());
        assertEquals(AssistantIntent.SAVINGS_GOAL, response.getIntent());
        assertTrue(response.getReply().contains("fonds d'urgence"));
    }

    @Test
    void shouldAnswerInEnglishForProjectFinancing() {
        LlmClientResponse response = stubLlmClient.generateResponse(baseRequest(
                ConversationLanguage.ENGLISH,
                StorytellingChatRequest.builder()
                        .message("I need help financing a project")
                        .build(),
                AssistantFinancialContext.builder()
                        .income(BigDecimal.valueOf(5000))
                        .expenses(BigDecimal.valueOf(3200))
                        .currency("DT")
                        .build()
        ));

        assertEquals(AssistantIntent.PROJECT_FINANCING, response.getIntent());
        assertTrue(response.getReply().contains("project"));
    }

    @Test
    void shouldAnswerInArabicForBudgetHelp() {
        LlmClientResponse response = stubLlmClient.generateResponse(baseRequest(
                ConversationLanguage.ARABIC,
                StorytellingChatRequest.builder()
                        .message("مساعدة في الميزانية")
                        .build(),
                AssistantFinancialContext.builder()
                        .income(BigDecimal.valueOf(4000))
                        .expenses(BigDecimal.valueOf(2500))
                        .currency("DT")
                        .build()
        ));

        assertEquals(AssistantIntent.BUDGET_HELP, response.getIntent());
        assertTrue(response.getReply().length() > 10);
    }

    private LlmClientRequest baseRequest(ConversationLanguage language,
                                         StorytellingChatRequest request,
                                         AssistantFinancialContext context) {
        return LlmClientRequest.builder()
                .language(language)
                .request(request)
                .financialContext(context)
                .promptPayload(PromptPayload.builder()
                        .systemPrompt("system")
                        .userPrompt("user")
                        .build())
                .build();
    }
}
