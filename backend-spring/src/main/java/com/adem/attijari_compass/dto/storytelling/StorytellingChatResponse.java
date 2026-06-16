package com.adem.attijari_compass.dto.storytelling;

import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorytellingChatResponse {
    private String reply;
    private AssistantEmotion emotion;
    private AssistantAction action;
    private AssistantIntent intent;
}
