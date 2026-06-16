package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatResponse;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class DirectAssistantResponseService {

    public StorytellingChatResponse respond(AssistantIntent intent,
                                            ConversationLanguage language,
                                            AssistantFinancialContext financialContext) {
        return switch (intent) {
            case GREETING -> build(greeting(language), AssistantEmotion.FRIENDLY, AssistantAction.NONE, intent);
            case IDENTITY -> build(identity(language), AssistantEmotion.FRIENDLY, AssistantAction.NONE, intent);
            case SALARY_INFO -> build(salary(language, financialContext), AssistantEmotion.INFORMATIVE, AssistantAction.NONE, intent);
            case ACCOUNT_BALANCE -> build(accountBalance(language, financialContext), AssistantEmotion.CALM, AssistantAction.NONE, intent);
            case SAVINGS_BALANCE -> build(savingsBalance(language, financialContext), AssistantEmotion.CALM, AssistantAction.NONE, intent);
            case UNKNOWN -> build(clarification(language), AssistantEmotion.CALM, AssistantAction.NONE, intent);
            default -> build(clarification(language), AssistantEmotion.CALM, AssistantAction.NONE, AssistantIntent.UNKNOWN);
        };
    }

    private StorytellingChatResponse build(String reply, AssistantEmotion emotion, AssistantAction action, AssistantIntent intent) {
        return StorytellingChatResponse.builder()
                .reply(reply)
                .emotion(emotion)
                .action(action)
                .intent(intent)
                .build();
    }

    private String greeting(ConversationLanguage language) {
        return switch (language) {
            case ENGLISH -> "Hello. I can answer simple account questions or help you with budget, savings, and financial projects.";
            case ARABIC -> "مرحبا. يمكنني الإجابة عن الأسئلة البسيطة حول الحساب أو مساعدتك في الميزانية والادخار والمشاريع المالية.";
            case FRENCH -> "Bonjour. Je peux répondre aux questions simples sur votre situation et vous aider sur le budget, l'épargne ou un projet financier.";
        };
    }

    private String identity(ConversationLanguage language) {
        return switch (language) {
            case ENGLISH -> "I am your virtual banking assistant. I answer simple financial questions directly and use deeper coaching only when you need guidance.";
            case ARABIC -> "أنا مساعدك البنكي الافتراضي. أجيب مباشرة عن الأسئلة المالية البسيطة وأستخدم أسلوب الإرشاد فقط عندما تحتاج إلى مرافقة أعمق.";
            case FRENCH -> "Je suis votre assistant bancaire virtuel. Je réponds directement aux questions simples et je passe en mode accompagnement seulement quand vous avez besoin de guidance.";
        };
    }

    private String salary(ConversationLanguage language, AssistantFinancialContext context) {
        BigDecimal salary = context.getSalary() != null ? context.getSalary() : context.getIncome();
        if (salary == null) {
            return switch (language) {
                case ENGLISH -> "I do not have a verified salary in the current data. If you share it in the financial context, I can use it directly.";
                case ARABIC -> "لا أملك حاليا راتبا مؤكدا في البيانات المتاحة. إذا أرسلته داخل السياق المالي يمكنني استخدامه مباشرة.";
                case FRENCH -> "Je ne dispose pas d'un salaire vérifié dans les données actuelles. Si vous l'envoyez dans le contexte financier, je pourrai l'utiliser directement.";
            };
        }
        return switch (language) {
            case ENGLISH -> "Your verified salary in the current context is " + amount(salary, context) + ".";
            case ARABIC -> "راتبك المؤكد في السياق الحالي هو " + amount(salary, context) + ".";
            case FRENCH -> "Votre salaire vérifié dans le contexte actuel est de " + amount(salary, context) + ".";
        };
    }

    private String accountBalance(ConversationLanguage language, AssistantFinancialContext context) {
        if (!context.isBalanceAvailable() || context.getBalance() == null) {
            return switch (language) {
                case ENGLISH -> "I do not have a verified account balance in the current data.";
                case ARABIC -> "لا أملك رصيدا مؤكدا للحساب في البيانات الحالية.";
                case FRENCH -> "Je ne dispose pas d'un solde de compte vérifié dans les données actuelles.";
            };
        }
        return switch (language) {
            case ENGLISH -> "Your verified account balance is " + amount(context.getBalance(), context) + ".";
            case ARABIC -> "رصيد حسابك المؤكد هو " + amount(context.getBalance(), context) + ".";
            case FRENCH -> "Votre solde de compte vérifié est de " + amount(context.getBalance(), context) + ".";
        };
    }

    private String savingsBalance(ConversationLanguage language, AssistantFinancialContext context) {
        if (!context.isSavingsBalanceAvailable() || context.getSavingsBalance() == null) {
            return switch (language) {
                case ENGLISH -> "I do not have a verified savings balance in the current data.";
                case ARABIC -> "لا أملك رصيد ادخار مؤكدا في البيانات الحالية.";
                case FRENCH -> "Je ne dispose pas d'un solde d'épargne vérifié dans les données actuelles.";
            };
        }
        return switch (language) {
            case ENGLISH -> "Your verified savings balance is " + amount(context.getSavingsBalance(), context) + ".";
            case ARABIC -> "رصيد الادخار المؤكد لديك هو " + amount(context.getSavingsBalance(), context) + ".";
            case FRENCH -> "Votre solde d'épargne vérifié est de " + amount(context.getSavingsBalance(), context) + ".";
        };
    }

    private String clarification(ConversationLanguage language) {
        return switch (language) {
            case ENGLISH -> "I did not identify the request clearly. You can ask about your salary, account balance, savings balance, expenses, budget, savings goal, or project financing.";
            case ARABIC -> "لم أحدد الطلب بشكل واضح. يمكنك سؤالي عن الراتب أو رصيد الحساب أو رصيد الادخار أو المصاريف أو الميزانية أو هدف الادخار أو تمويل مشروع.";
            case FRENCH -> "Je n'ai pas identifié la demande clairement. Vous pouvez me demander votre salaire, votre solde, votre épargne, une analyse des dépenses, de l'aide budget, un objectif d'épargne ou le financement d'un projet.";
        };
    }

    private String amount(BigDecimal value, AssistantFinancialContext context) {
        String currency = StringUtils.hasText(context.getCurrency()) ? context.getCurrency() : "DT";
        return value.toPlainString() + " " + currency;
    }
}
