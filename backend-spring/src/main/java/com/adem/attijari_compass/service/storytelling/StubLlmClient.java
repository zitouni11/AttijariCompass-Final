package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.model.storytelling.AssistantAction;
import com.adem.attijari_compass.model.storytelling.AssistantEmotion;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import com.adem.attijari_compass.model.storytelling.LlmClientRequest;
import com.adem.attijari_compass.model.storytelling.LlmClientResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class StubLlmClient implements LlmClient {

    @Override
    public LlmClientResponse generateResponse(LlmClientRequest request) {
        String message = normalize(request.getRequest().getMessage());

        if (isProjectFinancing(message, request.getRequest())) {
            return build(projectFinancing(request.getLanguage(), request.getFinancialContext()),
                    AssistantEmotion.ENCOURAGING, AssistantAction.ASK_GOAL, AssistantIntent.PROJECT_FINANCING);
        }

        if (isExpenseAnalysis(message)) {
            return build(expenseAnalysis(request.getLanguage(), request.getFinancialContext()),
                    AssistantEmotion.INFORMATIVE, AssistantAction.SHOW_BUDGET, AssistantIntent.EXPENSE_ANALYSIS);
        }

        if (isBudgetHelp(message)) {
            return build(budgetGuidance(request.getLanguage(), request.getFinancialContext()),
                    AssistantEmotion.INFORMATIVE, AssistantAction.SHOW_BUDGET, AssistantIntent.BUDGET_HELP);
        }

        return build(savingsGuidance(request.getLanguage(), request.getRequest(), request.getFinancialContext()),
                AssistantEmotion.ENCOURAGING, AssistantAction.SHOW_SAVINGS_PLAN, AssistantIntent.SAVINGS_GOAL);
    }

    private LlmClientResponse build(String reply, AssistantEmotion emotion, AssistantAction action, AssistantIntent intent) {
        return LlmClientResponse.builder()
                .reply(reply)
                .emotion(emotion)
                .action(action)
                .intent(intent)
                .build();
    }

    private String savingsGuidance(ConversationLanguage language, StorytellingChatRequest request,
                                   AssistantFinancialContext context) {
        String objective = StringUtils.hasText(request.getUserObjective()) ? request.getUserObjective().trim() : defaultSavingsObjective(language);
        BigDecimal monthlyCapacity = estimateMonthlyCapacity(context);

        return switch (language) {
            case ENGLISH -> "Let us turn your savings goal into a realistic rhythm. "
                    + "Your objective is " + objective + ". "
                    + savingsCapacitySentence(language, context, monthlyCapacity)
                    + "If you want, I can help split that into a practical monthly plan.";
            case ARABIC -> "لنحوّل هدف الادخار إلى خطة واقعية. "
                    + "هدفك هو " + objective + ". "
                    + savingsCapacitySentence(language, context, monthlyCapacity)
                    + "إذا أردت، يمكنني تحويل ذلك إلى خطة شهرية عملية.";
            case FRENCH -> "Transformons votre objectif d'épargne en rythme réaliste. "
                    + "Votre objectif est " + objective + ". "
                    + savingsCapacitySentence(language, context, monthlyCapacity)
                    + "Si vous voulez, je peux le convertir en plan mensuel concret.";
        };
    }

    private String budgetGuidance(ConversationLanguage language, AssistantFinancialContext context) {
        String income = context.getIncome() != null ? context.getIncome().toPlainString() : null;
        String expenses = context.getExpenses() != null ? context.getExpenses().toPlainString() : null;

        return switch (language) {
            case ENGLISH -> income != null && expenses != null
                    ? "Your verified income is " + amount(context.getIncome(), context) + " and verified expenses are "
                    + amount(context.getExpenses(), context) + ". We can use that to rebalance essentials, savings, and flexible spending."
                    : "Share your main income and expenses and I will help you structure a clearer budget.";
            case ARABIC -> income != null && expenses != null
                    ? "الدخل المؤكد هو " + amount(context.getIncome(), context) + " والمصاريف المؤكدة هي "
                    + amount(context.getExpenses(), context) + ". يمكننا استخدام ذلك لإعادة تنظيم الضروريات والادخار والمصاريف المرنة."
                    : "شارك الدخل والمصاريف الأساسية وسأساعدك على بناء ميزانية أوضح.";
            case FRENCH -> income != null && expenses != null
                    ? "Votre revenu vérifié est de " + amount(context.getIncome(), context) + " et vos dépenses vérifiées sont de "
                    + amount(context.getExpenses(), context) + ". On peut s'appuyer dessus pour rééquilibrer l'essentiel, l'épargne et le flexible."
                    : "Partagez vos revenus et dépenses principales et je vous aiderai à structurer un budget plus clair.";
        };
    }

    private String expenseAnalysis(ConversationLanguage language, AssistantFinancialContext context) {
        String categorySummary = context.getSpendingByCategory() != null && !context.getSpendingByCategory().isEmpty()
                ? context.getSpendingByCategory().entrySet().stream()
                .limit(3)
                .map(entry -> entry.getKey() + "=" + entry.getValue().toPlainString())
                .reduce((left, right) -> left + ", " + right)
                .orElse("")
                : null;

        return switch (language) {
            case ENGLISH -> categorySummary != null
                    ? "Your verified spending is concentrated around " + categorySummary + ". We can now identify what is essential, reducible, and avoidable."
                    : "I can analyze expenses if you share recent transactions or category totals.";
            case ARABIC -> categorySummary != null
                    ? "المصاريف المؤكدة تتركز حول " + categorySummary + ". يمكننا الآن تحديد ما هو أساسي وما يمكن تخفيفه."
                    : "يمكنني تحليل المصاريف إذا شاركت المعاملات الأخيرة أو إجماليات الفئات.";
            case FRENCH -> categorySummary != null
                    ? "Vos dépenses vérifiées se concentrent surtout sur " + categorySummary + ". On peut maintenant distinguer l'essentiel, le réductible et l'évitable."
                    : "Je peux analyser vos dépenses si vous partagez les transactions récentes ou les totaux par catégorie.";
        };
    }

    private String projectFinancing(ConversationLanguage language, AssistantFinancialContext context) {
        BigDecimal monthlyCapacity = estimateMonthlyCapacity(context);

        return switch (language) {
            case ENGLISH -> "For project financing, the first step is to estimate a safe monthly capacity. "
                    + savingsCapacitySentence(language, context, monthlyCapacity)
                    + "Then we can decide whether savings, staged financing, or a credit simulation fits best.";
            case ARABIC -> "لتمويل مشروع، الخطوة الأولى هي تقدير قدرة شهرية آمنة. "
                    + savingsCapacitySentence(language, context, monthlyCapacity)
                    + "بعدها يمكننا تحديد ما إذا كان الادخار أو التمويل المرحلي أو المحاكاة الائتمانية هو الأنسب.";
            case FRENCH -> "Pour financer un projet, la première étape est d'estimer une capacité mensuelle saine. "
                    + savingsCapacitySentence(language, context, monthlyCapacity)
                    + "Ensuite, on pourra voir si l'épargne, un financement progressif ou une simulation de crédit est le plus adapté.";
        };
    }

    private String savingsCapacitySentence(ConversationLanguage language, AssistantFinancialContext context, BigDecimal monthlyCapacity) {
        if (monthlyCapacity == null) {
            return switch (language) {
                case ENGLISH -> "I still need verified income and expenses to estimate that precisely. ";
                case ARABIC -> "ما زلت أحتاج إلى دخل ومصاريف مؤكدة لتقدير ذلك بدقة. ";
                case FRENCH -> "J'ai encore besoin de revenus et dépenses vérifiés pour l'estimer précisément. ";
            };
        }

        return switch (language) {
            case ENGLISH -> "A reasonable starting point looks close to " + amount(monthlyCapacity, context) + " per month. ";
            case ARABIC -> "نقطة بداية معقولة تبدو قريبة من " + amount(monthlyCapacity, context) + " شهريا. ";
            case FRENCH -> "Un point de départ raisonnable semble proche de " + amount(monthlyCapacity, context) + " par mois. ";
        };
    }

    private BigDecimal estimateMonthlyCapacity(AssistantFinancialContext context) {
        if (context.getIncome() == null || context.getExpenses() == null) {
            return null;
        }

        BigDecimal remaining = context.getIncome().subtract(context.getExpenses());
        if (remaining.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return remaining.multiply(BigDecimal.valueOf(0.5));
    }

    private String defaultSavingsObjective(ConversationLanguage language) {
        return switch (language) {
            case ENGLISH -> "a savings target that matters to you";
            case ARABIC -> "هدف ادخار مهم بالنسبة لك";
            case FRENCH -> "un objectif d'épargne important pour vous";
        };
    }

    private String amount(BigDecimal value, AssistantFinancialContext context) {
        String currency = StringUtils.hasText(context.getCurrency()) ? context.getCurrency() : "DT";
        return value.toPlainString() + " " + currency;
    }

    private boolean isBudgetHelp(String message) {
        return containsAny(message, "budget", "budgeting", "planifier mes depenses", "planifier mes dépenses", "ميزانية");
    }

    private boolean isExpenseAnalysis(String message) {
        return containsAny(message, "depenses", "dépenses", "expenses", "spending", "مصاريف");
    }

    private boolean isProjectFinancing(String message, StorytellingChatRequest request) {
        String objective = normalize(request.getUserObjective());
        return containsAny(message, "project", "projet", "financer", "loan", "تمويل", "مشروع")
                || objective.contains("project")
                || objective.contains("projet")
                || objective.contains("مشروع");
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
