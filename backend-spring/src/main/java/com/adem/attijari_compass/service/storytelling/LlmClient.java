package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.model.storytelling.LlmClientRequest;
import com.adem.attijari_compass.model.storytelling.LlmClientResponse;

public interface LlmClient {

    LlmClientResponse generateResponse(LlmClientRequest request);
}
