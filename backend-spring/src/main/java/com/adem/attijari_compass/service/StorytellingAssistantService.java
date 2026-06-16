package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatResponse;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import com.adem.attijari_compass.model.storytelling.LlmClientRequest;
import com.adem.attijari_compass.model.storytelling.LlmClientResponse;
import com.adem.attijari_compass.model.storytelling.PromptPayload;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.service.storytelling.ConversationLanguageResolver;
import com.adem.attijari_compass.service.storytelling.DirectAssistantResponseService;
import com.adem.attijari_compass.service.storytelling.LlmClient;
import com.adem.attijari_compass.service.storytelling.MessageIntentDetector;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorytellingAssistantService {

    private final PromptBuilderService promptBuilderService;
    private final FinancialContextService financialContextService;
    private final ResponseGuardService responseGuardService;
    private final ConversationLanguageResolver conversationLanguageResolver;
    private final MessageIntentDetector messageIntentDetector;
    private final DirectAssistantResponseService directAssistantResponseService;
    private final TransactionRepository transactionRepository;
    private final LlmClient llmClient;

    public StorytellingChatResponse chat(StorytellingChatRequest request, String userEmail) {
        ConversationLanguage language = conversationLanguageResolver.resolve(request);
        AssistantIntent intent = messageIntentDetector.detect(request);

        if (messageIntentDetector.isImmediateIntent(intent)) {
            StorytellingChatResponse response = directAssistantResponseService.respond(intent, language, null);
            logFinalResponse(response);
            return response;
        }

        if (intent == AssistantIntent.ACCOUNT_BALANCE) {
            AssistantFinancialContext financialContext = financialContextService.buildAccountBalanceContext(userEmail, request.getFinancialContext());
            StorytellingChatResponse response = directAssistantResponseService.respond(intent, language, financialContext);
            logFinalResponse(response);
            return response;
        }

        if (intent == AssistantIntent.TRANSACTION_COUNT) {
            StorytellingChatResponse response = buildTransactionCountResponse(language, userEmail);
            logFinalResponse(response);
            return response;
        }

        AssistantFinancialContext financialContext = financialContextService.buildFinancialContext(userEmail, request.getFinancialContext());

        if (!messageIntentDetector.shouldUseLlm(intent)) {
            StorytellingChatResponse response = directAssistantResponseService.respond(intent, language, financialContext);
            logFinalResponse(response);
            return response;
        }

        PromptPayload promptPayload = promptBuilderService.buildPrompt(request, financialContext, language);

        LlmClientResponse llmResponse = llmClient.generateResponse(LlmClientRequest.builder()
                .request(request)
                .financialContext(financialContext)
                .language(language)
                .promptPayload(promptPayload)
                .build());

        StorytellingChatResponse response = responseGuardService.guard(llmResponse, request, financialContext, language);
        logFinalResponse(response);
        return response;
    }

    private void logFinalResponse(StorytellingChatResponse response) {
        log.info("Storytelling response generated: intent={}, action={}, emotion={}, reply={}",
                response != null ? response.getIntent() : null,
                response != null ? response.getAction() : null,
                response != null ? response.getEmotion() : null,
                response != null ? response.getReply() : null);
    }

    private StorytellingChatResponse buildTransactionCountResponse(ConversationLanguage language, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return StorytellingChatResponse.builder()
                    .reply(switch (language) {
                        case ENGLISH -> "I cannot count your transactions because no authenticated user was found.";
                        case ARABIC -> "لا يمكنني حساب عدد المعاملات لأن المستخدم غير معروف في هذه الجلسة.";
                        case FRENCH -> "Je ne peux pas compter vos transactions car aucun utilisateur authentifié n'a été trouvé.";
                    })
                    .emotion(AssistantEmotion.CALM)
                    .action(AssistantAction.NONE)
                    .intent(AssistantIntent.TRANSACTION_COUNT)
                    .build();
        }

        long transactionCount = transactionRepository.countByUserEmail(userEmail);

        return StorytellingChatResponse.builder()
                .reply(switch (language) {
                    case ENGLISH -> "The total number of transactions in your account is " + transactionCount + ".";
                    case ARABIC -> "العدد الإجمالي للمعاملات في حسابك هو " + transactionCount + ".";
                    case FRENCH -> "Le nombre total de transactions dans votre compte est de " + transactionCount + ".";
                })
                .emotion(AssistantEmotion.INFORMATIVE)
                .action(AssistantAction.NONE)
                .intent(AssistantIntent.TRANSACTION_COUNT)
                .build();
    }
}
