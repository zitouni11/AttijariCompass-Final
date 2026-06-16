package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.ConversationMessageDto;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Locale;

@Component
public class ConversationLanguageResolver {

    public ConversationLanguage resolve(StorytellingChatRequest request) {
        String candidate = request.getMessage();

        if ((candidate == null || candidate.isBlank()) && !CollectionUtils.isEmpty(request.getConversationHistory())) {
            List<ConversationMessageDto> history = request.getConversationHistory();
            ConversationMessageDto lastMessage = history.get(history.size() - 1);
            if (lastMessage != null && "user".equalsIgnoreCase(lastMessage.getRole())) {
                candidate = lastMessage.getText();
            }
        }

        if (containsArabic(candidate)) {
            return ConversationLanguage.ARABIC;
        }

        String normalized = candidate == null ? "" : candidate.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "hello", "hi", "budget", "savings", "balance", "who are you")) {
            return ConversationLanguage.ENGLISH;
        }

        return ConversationLanguage.FRENCH;
    }

    private boolean containsArabic(String value) {
        if (value == null) {
            return false;
        }
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x0600 && codePoint <= 0x06FF);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
