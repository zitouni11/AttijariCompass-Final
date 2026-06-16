package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageIntentDetectorTest {

    private final MessageIntentDetector detector = new MessageIntentDetector();

    @Test
    void shouldDetectIdentityQuestion() {
        AssistantIntent intent = detector.detect(StorytellingChatRequest.builder()
                .message("comment tu t'appelles")
                .build());

        assertEquals(AssistantIntent.IDENTITY, intent);
        assertFalse(detector.shouldUseLlm(intent));
    }

    @Test
    void shouldDetectSalaryQuestion() {
        AssistantIntent intent = detector.detect(StorytellingChatRequest.builder()
                .message("combien mon salaire")
                .build());

        assertEquals(AssistantIntent.SALARY_INFO, intent);
        assertFalse(detector.shouldUseLlm(intent));
    }

    @Test
    void shouldDetectBudgetHelpAsCoachingIntent() {
        AssistantIntent intent = detector.detect(StorytellingChatRequest.builder()
                .message("aide-moi avec mon budget")
                .build());

        assertEquals(AssistantIntent.BUDGET_HELP, intent);
        assertTrue(detector.shouldUseLlm(intent));
    }

    @Test
    void shouldDetectAccountBalanceQuestion() {
        AssistantIntent intent = detector.detect(StorytellingChatRequest.builder()
                .message("combien j'ai dans mon compte")
                .build());

        assertEquals(AssistantIntent.ACCOUNT_BALANCE, intent);
        assertFalse(detector.shouldUseLlm(intent));
    }

    @Test
    void shouldDetectTransactionCountQuestion() {
        AssistantIntent intent = detector.detect(StorytellingChatRequest.builder()
                .message("combien de transactions j'ai")
                .build());

        assertEquals(AssistantIntent.TRANSACTION_COUNT, intent);
        assertFalse(detector.shouldUseLlm(intent));
    }

    @Test
    void shouldReturnUnknownForUnclearRequest() {
        AssistantIntent intent = detector.detect(StorytellingChatRequest.builder()
                .message("code de chambre")
                .build());

        assertEquals(AssistantIntent.UNKNOWN, intent);
        assertTrue(detector.shouldUseLlm(intent));
    }
}
