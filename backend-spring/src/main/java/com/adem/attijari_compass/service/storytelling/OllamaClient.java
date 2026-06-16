package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.ollama.OllamaGenerateRequest;
import com.adem.attijari_compass.dto.storytelling.ollama.OllamaGenerateResponse;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.LlmClientRequest;
import com.adem.attijari_compass.model.storytelling.LlmClientResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
@Primary
@Slf4j
public class OllamaClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StubLlmClient stubLlmClient;
    private final String model;

    public OllamaClient(ObjectMapper objectMapper,
                        StubLlmClient stubLlmClient,
                        @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
                        @Value("${ollama.model:mistral}") String model) {
        this.objectMapper = objectMapper;
        this.stubLlmClient = stubLlmClient;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000); // 60 sec
        factory.setReadTimeout(3000);    // 🔥 CRITIQUE

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public LlmClientResponse generateResponse(LlmClientRequest request) {
        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(OllamaGenerateRequest.builder()
                            .model(model)
                            .stream(false)
                            .format("json")
                            .system(request.getPromptPayload().getSystemPrompt())
                            .prompt(request.getPromptPayload().getUserPrompt())
                            .build())
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
                throw new IllegalStateException("Empty response from Ollama");
            }

            return parseStructuredResponse(response.getResponse());
        } catch (Exception ex) {
            log.warn("Ollama request failed, falling back to StubLlmClient: {}", ex.getMessage());
            return stubLlmClient.generateResponse(request);
        }
    }

    public String generateRawText(String message) {
        OllamaGenerateResponse response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(OllamaGenerateRequest.builder()
                        .model(model)
                        .stream(false)
                        .prompt(message)
                        .build())
                .retrieve()
                .body(OllamaGenerateResponse.class);

        return response != null ? response.getResponse() : "";
    }

    public String generateStructuredJson(String systemPrompt, String prompt) {
        try {
            log.info("Calling Ollama structured JSON endpoint for storytelling");
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(OllamaGenerateRequest.builder()
                            .model(model)
                            .stream(false)
                            .format("json")
                            .system(systemPrompt)
                            .prompt(prompt)
                            .build())
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
                throw new IllegalStateException("Empty response from Ollama");
            }

            log.debug("Ollama structured JSON raw response: {}", response.getResponse());
            return response.getResponse();
        } catch (Exception ex) {
            log.warn("Ollama structured JSON request failed: {}", ex.getMessage());
            throw ex;
        }
    }

    private LlmClientResponse parseStructuredResponse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(extractJson(rawResponse));

        return LlmClientResponse.builder()
                .reply(textValue(root, "reply"))
                .emotion(parseEmotion(textValue(root, "emotion")))
                .action(parseAction(textValue(root, "action")))
                .intent(parseIntent(textValue(root, "intent")))
                .build();
    }

    private String extractJson(String rawResponse) {
        int start = rawResponse.indexOf('{');
        int end = rawResponse.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return rawResponse.substring(start, end + 1);
        }
        return rawResponse;
    }

    private String textValue(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node != null && !node.isNull() ? node.asText() : "";
    }

    private AssistantEmotion parseEmotion(String value) {
        return switch (normalize(value)) {
            case "friendly" -> AssistantEmotion.FRIENDLY;
            case "encouraging" -> AssistantEmotion.ENCOURAGING;
            case "informative" -> AssistantEmotion.INFORMATIVE;
            default -> AssistantEmotion.CALM;
        };
    }

    private AssistantAction parseAction(String value) {
        return switch (normalize(value)) {
            case "show_budget" -> AssistantAction.SHOW_BUDGET;
            case "show_savings_plan" -> AssistantAction.SHOW_SAVINGS_PLAN;
            case "ask_goal" -> AssistantAction.ASK_GOAL;
            default -> AssistantAction.NONE;
        };
    }

    private AssistantIntent parseIntent(String value) {
        return switch (normalize(value)) {
            case "greeting" -> AssistantIntent.GREETING;
            case "identity" -> AssistantIntent.IDENTITY;
            case "salary_info" -> AssistantIntent.SALARY_INFO;
            case "account_balance" -> AssistantIntent.ACCOUNT_BALANCE;
            case "transaction_count" -> AssistantIntent.TRANSACTION_COUNT;
            case "savings_balance" -> AssistantIntent.SAVINGS_BALANCE;
            case "budget_help" -> AssistantIntent.BUDGET_HELP;
            case "expense_analysis" -> AssistantIntent.EXPENSE_ANALYSIS;
            case "project_financing" -> AssistantIntent.PROJECT_FINANCING;
            case "savings_goal" -> AssistantIntent.SAVINGS_GOAL;
            default -> AssistantIntent.UNKNOWN;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
