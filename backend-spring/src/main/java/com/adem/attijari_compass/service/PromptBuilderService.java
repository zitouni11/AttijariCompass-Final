package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.storytelling.ConversationMessageDto;
import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.model.storytelling.ConversationLanguage;
import com.adem.attijari_compass.model.storytelling.PromptPayload;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class PromptBuilderService {

    public PromptPayload buildPrompt(StorytellingChatRequest request,
                                     AssistantFinancialContext financialContext,
                                     ConversationLanguage language) {
        return PromptPayload.builder()
                .systemPrompt(buildSystemPrompt(language))
                .userPrompt(buildUserPrompt(request, financialContext, language))
                .build();
    }

    public String buildSystemPrompt(ConversationLanguage language) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("You are a virtual banking advisor for a Personal Financial Management application.");
        prompt.add("Respond strictly in the user's latest language: " + language.getCode() + ".");
        prompt.add("Supported languages are French, English and Arabic.");
        prompt.add("Tone: human, professional, warm, reassuring and natural.");
        prompt.add("Mission: explain finances clearly, help users progress toward goals, and stay grounded in available data.");
        prompt.add("Never invent a balance, transaction, account event or banking amount.");
        prompt.add("If requested financial data is missing, say that clearly and invite the user to share or sync the data.");
        prompt.add("Use storytelling only when the user talks about savings, goals, projects or financial coaching.");
        prompt.add("For greetings, identity questions and direct requests, answer simply and naturally.");
        prompt.add("Keep answers short to medium, with useful next steps when relevant.");
        prompt.add("Return only valid JSON with exactly these keys: reply, emotion, action, intent.");
        prompt.add("Allowed emotion values: friendly, calm, encouraging, informative.");
        prompt.add("Allowed action values: none, show_budget, show_savings_plan, ask_goal.");
        prompt.add("Allowed intent values: greeting, identity, salary_info, account_balance, transaction_count, savings_balance, budget_help, expense_analysis, savings_goal, project_financing, unknown.");
        prompt.add("Do not wrap JSON in markdown.");
        return prompt.toString();
    }

    public String buildUserPrompt(StorytellingChatRequest request,
                                  AssistantFinancialContext financialContext,
                                  ConversationLanguage language) {
        StringJoiner prompt = new StringJoiner("\n");
        prompt.add("Language: " + language.getCode());

        if (StringUtils.hasText(request.getUserObjective())) {
            prompt.add("User objective: " + request.getUserObjective().trim());
        }

        prompt.add("Financial context:");
        prompt.add(buildFinancialContextSection(financialContext));
        prompt.add("Conversation history:");
        prompt.add(buildHistorySection(request));
        prompt.add("Latest user message:");
        prompt.add(safeText(request.getMessage()));
        return prompt.toString();
    }

    public String buildFinancialContextSection(AssistantFinancialContext financialContext) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("- Real banking data available: " + financialContext.isRealDataAvailable());
        joiner.add("- Currency: " + defaultValue(financialContext.getCurrency(), "DT"));
        joiner.add("- Salary: " + format(financialContext.getSalary()));
        joiner.add("- Income: " + format(financialContext.getIncome()));
        joiner.add("- Expenses: " + format(financialContext.getExpenses()));
        joiner.add("- Budget: " + format(financialContext.getBudget()));
        joiner.add("- Balance available: " + financialContext.isBalanceAvailable());
        joiner.add("- Balance: " + format(financialContext.getBalance()));
        joiner.add("- Savings balance available: " + financialContext.isSavingsBalanceAvailable());
        joiner.add("- Savings balance: " + format(financialContext.getSavingsBalance()));
        joiner.add("- Monthly summary: " + defaultValue(financialContext.getMonthlySummary(), "No verified monthly summary available"));
        joiner.add("- Recent transactions: " + buildRecentTransactions(financialContext));
        joiner.add("- Spending by category: " + buildCategories(financialContext.getSpendingByCategory()));
        joiner.add("- Additional data: " + buildAdditionalData(financialContext.getAdditionalData()));
        return joiner.toString();
    }

    public String buildHistorySection(StorytellingChatRequest request) {
        if (CollectionUtils.isEmpty(request.getConversationHistory())) {
            return "- No recent conversation history";
        }

        StringJoiner joiner = new StringJoiner("\n");
        for (ConversationMessageDto message : request.getConversationHistory()) {
            joiner.add("- " + safeText(message.getRole()) + ": " + safeText(message.getText()));
        }
        return joiner.toString();
    }

    private String buildRecentTransactions(AssistantFinancialContext financialContext) {
        if (CollectionUtils.isEmpty(financialContext.getRecentTransactions())) {
            return "No verified recent transaction summary available";
        }
        return String.join(" | ", financialContext.getRecentTransactions());
    }

    private String buildCategories(Map<String, BigDecimal> spendingByCategory) {
        if (spendingByCategory == null || spendingByCategory.isEmpty()) {
            return "No verified category breakdown available";
        }

        StringJoiner joiner = new StringJoiner(", ");
        spendingByCategory.forEach((category, amount) -> joiner.add(category + "=" + amount));
        return joiner.toString();
    }

    private String buildAdditionalData(Map<String, Object> additionalData) {
        return additionalData == null || additionalData.isEmpty() ? "None" : additionalData.toString();
    }

    private String format(BigDecimal amount) {
        return amount == null ? "Unavailable" : amount.toPlainString();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
