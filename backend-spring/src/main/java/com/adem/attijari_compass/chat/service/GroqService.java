package com.adem.attijari_compass.chat.service;

import com.adem.attijari_compass.chat.config.GroqProperties;
import com.adem.attijari_compass.chat.dto.GroqRequestDto;
import com.adem.attijari_compass.chat.dto.GroqResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final GroqProperties groqProperties;

    private final WebClient groqWebClient;

    public String ask(String model, String systemPrompt, String userPrompt) {
        return askWithMetadata(model, systemPrompt, userPrompt).content();
    }

    public GroqAnswer askWithMetadata(String model, String systemPrompt, String userPrompt) {
        ensureApiKeyPresent();

        String requestedModel = StringUtils.hasText(model) ? model : groqProperties.getModel();
        try {
            return callModel(requestedModel, systemPrompt, userPrompt);
        } catch (GroqCallException ex) {
            if (shouldFallback(requestedModel, ex)) {
                log.warn("Groq default model fallback triggered: requestedModel={}, reason={}", requestedModel, ex.getMessage());
                return callModel(groqProperties.getAdvancedModel(), systemPrompt, userPrompt);
            }
            throw ex;
        }
    }

    private GroqAnswer callModel(String model, String systemPrompt, String userPrompt) {
        long startedAt = System.currentTimeMillis();

        GroqRequestDto request = GroqRequestDto.builder()
                .model(model)
                .messages(List.of(
                        GroqRequestDto.MessageDto.builder().role("system").content(systemPrompt).build(),
                        GroqRequestDto.MessageDto.builder().role("user").content(userPrompt).build()
                ))
                .temperature(groqProperties.getTemperature())
                .maxTokens(groqProperties.getMaxTokens())
                .build();

        try {
            String rawResponse = groqWebClient.post()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqProperties.getKey().trim())
        .bodyValue(request)
        .exchangeToMono(clientResponse -> handleResponse(clientResponse.statusCode(), clientResponse.bodyToMono(String.class)))
        .timeout(Duration.ofSeconds(groqProperties.getTimeoutSeconds()))
        .block();

            String content = extractContentSafe(rawResponse);            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("Groq call completed: model={}, durationMs={}", model, durationMs);
            return new GroqAnswer(content, model);
        } catch (GroqCallException ex) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.warn("Groq call failed: model={}, durationMs={}, message={}", model, durationMs, ex.getMessage());
            throw ex;
        } catch (WebClientRequestException ex) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.error("Groq transport error: model={}, durationMs={}, message={}", model, durationMs, ex.getMessage(), ex);
            throw new GroqCallException("Le service IA n'est pas joignable pour le moment.", true, 503);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.error("Groq unexpected failure: model={}, durationMs={}, message={}", model, durationMs, ex.getMessage(), ex);
            throw new GroqCallException("Le service IA est momentanement indisponible.", true, 500);
        }
    }

    private Mono<String> handleResponse(HttpStatusCode statusCode, Mono<String> bodyMono) {
    if (statusCode.is2xxSuccessful()) {
        return bodyMono;
    }

    return bodyMono.defaultIfEmpty("")
            .flatMap(body -> Mono.error(toException(statusCode.value(), body)));
}

    private GroqCallException toException(int statusCode, String responseBody) {
        String message = switch (statusCode) {
            case 401 -> "La cle API Groq est invalide ou absente.";
            case 403 -> "L'appel Groq a ete refuse.";
            case 429 -> "La limite d'appels du service IA a ete atteinte.";
            default -> "Groq a retourne une erreur HTTP " + statusCode + ".";
        };

        boolean recoverable = statusCode == 404 || statusCode == 408 || statusCode == 409
                || statusCode == 429 || statusCode >= 500;
        if (StringUtils.hasText(responseBody)) {
            message = message + " " + responseBody;
        }
        return new GroqCallException(message, recoverable, statusCode);
    }

    private String extractContent(GroqResponseDto response) {
        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().get(0) == null
                || response.getChoices().get(0).getMessage() == null
                || !StringUtils.hasText(response.getChoices().get(0).getMessage().getContent())) {
            throw new GroqCallException("La reponse Groq est vide.", true, 502);
        }

        return response.getChoices().get(0).getMessage().getContent().trim();
    }



    private String extractContentSafe(String rawJson) {
    try {
        com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawJson);

        if (root.has("choices")) {
            com.fasterxml.jackson.databind.JsonNode choices = root.get("choices");

            if (choices.isArray() && choices.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode message =
                        choices.get(0).get("message");

                if (message != null && message.has("content")) {
                    return message.get("content").asText().trim();
                }
            }
        }

        return "Réponse IA non disponible.";
    } catch (Exception e) {
        return "Erreur parsing réponse IA.";
    }
}

    private boolean shouldFallback(String requestedModel, GroqCallException exception) {
        return exception.isRecoverable()
                && StringUtils.hasText(groqProperties.getAdvancedModel())
                && !groqProperties.getAdvancedModel().equalsIgnoreCase(requestedModel);
    }

    private void ensureApiKeyPresent() {
        if (!StringUtils.hasText(groqProperties.getKey())) {
            log.error("Groq configuration error: GROQ_API_KEY is missing");
            throw new GroqCallException("Le service IA est temporairement indisponible.", false, 503);
        }
    }

    public record GroqAnswer(String content, String usedModel) {
    }

    public static class GroqCallException extends RuntimeException {
        private final boolean recoverable;
        private final int statusCode;

        public GroqCallException(String message, boolean recoverable, int statusCode) {
            super(message);
            this.recoverable = recoverable;
            this.statusCode = statusCode;
        }

        public boolean isRecoverable() {
            return recoverable;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    private static final class JsonSupport {
        private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules();

        private static <T> T fromJson(String body, Class<T> type) {
            try {
                return OBJECT_MAPPER.readValue(body, type);
            } catch (Exception ex) {
                throw new GroqCallException("Impossible de lire la reponse Groq.", true, 502);
            }
        }
    }
}
