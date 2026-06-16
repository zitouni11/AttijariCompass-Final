package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatResponse;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import com.adem.attijari_compass.model.storytelling.LlmClientResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class ResponseGuardService {

    public StorytellingChatResponse guard(LlmClientResponse llmResponse,
                                          StorytellingChatRequest request,
                                          AssistantFinancialContext financialContext,
                                          ConversationLanguage language) {
        if (asksForBalance(request.getMessage()) && !financialContext.isBalanceAvailable()) {
            return missingBalance(language);
        }

        if (asksForTransactions(request.getMessage()) && CollectionUtils.isEmpty(financialContext.getRecentTransactions())) {
            return missingTransactions(language);
        }

        if (!StringUtils.hasText(llmResponse.getReply())) {
            return safeFallback(language);
        }

        String guardedReply = guardVerifiedFinancialClaims(llmResponse.getReply().trim(), financialContext, language);

        return StorytellingChatResponse.builder()
                .reply(guardedReply)
                .emotion(llmResponse.getEmotion())
                .action(llmResponse.getAction())
                .intent(llmResponse.getIntent())
                .build();
    }

    private StorytellingChatResponse missingBalance(ConversationLanguage language) {
        return StorytellingChatResponse.builder()
                .reply(switch (language) {
                    case ENGLISH -> "I do not have a verified account balance in the current data. If you share it or sync the account, I can help you interpret it.";
                    case ARABIC -> "لا أملك حاليا رصيدا بنكيا مؤكدا في البيانات المتاحة. إذا قمت بمشاركته أو مزامنة الحساب يمكنني مساعدتك في تفسيره.";
                    case FRENCH -> "Je ne dispose pas d'un solde bancaire vérifié dans les données actuelles. Si vous le partagez ou synchronisez le compte, je pourrai vous aider à l'interpréter.";
                })
                .emotion(AssistantEmotion.CALM)
                .action(AssistantAction.NONE)
                .intent(AssistantIntent.ACCOUNT_BALANCE)
                .build();
    }

    private StorytellingChatResponse missingTransactions(ConversationLanguage language) {
        return StorytellingChatResponse.builder()
                .reply(switch (language) {
                    case ENGLISH -> "I do not have verified recent transactions right now. If you sync or share them, I can help analyze the pattern.";
                    case ARABIC -> "لا أملك حاليا معاملات حديثة مؤكدة. إذا قمت بمزامنتها أو مشاركتها يمكنني مساعدتك في تحليلها.";
                    case FRENCH -> "Je ne dispose pas de transactions récentes vérifiées pour le moment. Si vous les synchronisez ou les partagez, je pourrai vous aider à les analyser.";
                })
                .emotion(AssistantEmotion.CALM)
                .action(AssistantAction.NONE)
                .intent(AssistantIntent.EXPENSE_ANALYSIS)
                .build();
    }

    private StorytellingChatResponse safeFallback(ConversationLanguage language) {
        return StorytellingChatResponse.builder()
                .reply(switch (language) {
                    case ENGLISH -> "I need a bit more context. You can ask about your salary, balance, savings, expenses, budget, or a project you want to finance.";
                    case ARABIC -> "أحتاج إلى توضيح بسيط. يمكنك سؤالي عن الراتب أو الرصيد أو الادخار أو المصاريف أو الميزانية أو مشروع تريد تمويله.";
                    case FRENCH -> "J'ai besoin d'un peu plus de contexte. Vous pouvez me parler de votre salaire, de votre solde, de votre épargne, de vos dépenses, de votre budget ou d'un projet à financer.";
                })
                .emotion(AssistantEmotion.CALM)
                .action(AssistantAction.NONE)
                .intent(AssistantIntent.UNKNOWN)
                .build();
    }

    private boolean asksForBalance(String message) {
        String normalized = normalize(message);
        return normalized.contains("solde") || normalized.contains("balance") || normalized.contains("رصيد");
    }

    private boolean asksForTransactions(String message) {
        String normalized = normalize(message);
        return normalized.contains("transaction")
                || normalized.contains("transactions")
                || normalized.contains("operations")
                || normalized.contains("movement")
                || normalized.contains("عمليات");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String guardVerifiedFinancialClaims(String reply,
                                               AssistantFinancialContext financialContext,
                                               ConversationLanguage language) {
        String guardedReply = reply;

        if (financialContext.getSalary() != null && mentionsSalary(reply)
                && !reply.contains(financialContext.getSalary().toPlainString())) {
            guardedReply = appendSentence(guardedReply, verifiedSalarySentence(financialContext, language));
        }

        if (financialContext.getBalance() != null && mentionsBalance(reply)
                && !reply.contains(financialContext.getBalance().toPlainString())) {
            guardedReply = appendSentence(guardedReply, verifiedBalanceSentence(financialContext, language));
        }

        if (financialContext.getSavingsBalance() != null && mentionsSavingsBalance(reply)
                && !reply.contains(financialContext.getSavingsBalance().toPlainString())) {
            guardedReply = appendSentence(guardedReply, verifiedSavingsSentence(financialContext, language));
        }

        return guardedReply;
    }

    private boolean mentionsSalary(String reply) {
        String normalized = normalize(reply);
        return normalized.contains("salary") || normalized.contains("salaire") || normalized.contains("راتب");
    }

    private boolean mentionsBalance(String reply) {
        String normalized = normalize(reply);
        return normalized.contains("balance") || normalized.contains("solde") || normalized.contains("رصيد");
    }

    private boolean mentionsSavingsBalance(String reply) {
        String normalized = normalize(reply);
        return normalized.contains("savings") || normalized.contains("épargne") || normalized.contains("epargne") || normalized.contains("ادخار");
    }

    private String verifiedSalarySentence(AssistantFinancialContext context, ConversationLanguage language) {
        String currency = currency(context);
        return switch (language) {
            case ENGLISH -> "Verified salary in context: " + context.getSalary().toPlainString() + " " + currency + ".";
            case ARABIC -> "الراتب المؤكد في السياق هو " + context.getSalary().toPlainString() + " " + currency + ".";
            case FRENCH -> "Le salaire vérifié dans le contexte est de " + context.getSalary().toPlainString() + " " + currency + ".";
        };
    }

    private String verifiedBalanceSentence(AssistantFinancialContext context, ConversationLanguage language) {
        String currency = currency(context);
        return switch (language) {
            case ENGLISH -> "Verified balance in context: " + context.getBalance().toPlainString() + " " + currency + ".";
            case ARABIC -> "الرصيد المؤكد في السياق هو " + context.getBalance().toPlainString() + " " + currency + ".";
            case FRENCH -> "Le solde vérifié dans le contexte est de " + context.getBalance().toPlainString() + " " + currency + ".";
        };
    }

    private String verifiedSavingsSentence(AssistantFinancialContext context, ConversationLanguage language) {
        String currency = currency(context);
        return switch (language) {
            case ENGLISH -> "Verified savings balance in context: " + context.getSavingsBalance().toPlainString() + " " + currency + ".";
            case ARABIC -> "رصيد الادخار المؤكد في السياق هو " + context.getSavingsBalance().toPlainString() + " " + currency + ".";
            case FRENCH -> "Le solde d'épargne vérifié dans le contexte est de " + context.getSavingsBalance().toPlainString() + " " + currency + ".";
        };
    }

    private String appendSentence(String reply, String sentence) {
        if (reply.endsWith(".")) {
            return reply + " " + sentence;
        }
        return reply + ". " + sentence;
    }

    private String currency(AssistantFinancialContext context) {
        return StringUtils.hasText(context.getCurrency()) ? context.getCurrency() : "DT";
    }
}
