package com.adem.attijari_compass.service.storytelling;

import com.adem.attijari_compass.dto.storytelling.StorytellingChatRequest;
import com.adem.attijari_compass.model.storytelling.AssistantIntent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class MessageIntentDetector {

    public AssistantIntent detect(StorytellingChatRequest request) {
        String message = normalize(request != null ? request.getMessage() : null);
        String objective = normalize(request != null ? request.getUserObjective() : null);

        if (message.isBlank() && objective.isBlank()) {
            return AssistantIntent.UNKNOWN;
        }
        if (isGreeting(message)) {
            return AssistantIntent.GREETING;
        }
        if (isIdentityQuestion(message)) {
            return AssistantIntent.IDENTITY;
        }
        if (isSalaryQuestion(message)) {
            return AssistantIntent.SALARY_INFO;
        }
        if (isTransactionCountQuestion(message)) {
            return AssistantIntent.TRANSACTION_COUNT;
        }
        if (isSavingsBalanceQuestion(message)) {
            return AssistantIntent.SAVINGS_BALANCE;
        }
        if (isAccountBalanceQuestion(message)) {
            return AssistantIntent.ACCOUNT_BALANCE;
        }
        if (isExpenseAnalysis(message)) {
            return AssistantIntent.EXPENSE_ANALYSIS;
        }
        if (isBudgetHelp(message)) {
            return AssistantIntent.BUDGET_HELP;
        }
        if (isProjectFinancing(message, objective)) {
            return AssistantIntent.PROJECT_FINANCING;
        }
        if (isSavingsGoal(message, objective)) {
            return AssistantIntent.SAVINGS_GOAL;
        }
        return AssistantIntent.UNKNOWN;
    }

    public boolean shouldUseLlm(AssistantIntent intent) {
        return intent == AssistantIntent.BUDGET_HELP
                || intent == AssistantIntent.EXPENSE_ANALYSIS
                || intent == AssistantIntent.SAVINGS_GOAL
                || intent == AssistantIntent.PROJECT_FINANCING
                || intent == AssistantIntent.UNKNOWN;
    }

    public boolean isImmediateIntent(AssistantIntent intent) {
        return intent == AssistantIntent.GREETING
                || intent == AssistantIntent.IDENTITY;
    }

    public boolean isTransactionCountQuestion(String message) {
        String normalized = normalize(message);
        return containsAny(normalized,
                "combien de transaction existe dans mon compte",
                "combien de transactions existe dans mon compte",
                "combien de transactions j'ai",
                "combien de transactions jai",
                "combien de transactions ai-je",
                "nombre de transactions",
                "nombre total de transactions",
                "transaction count",
                "how many transactions do i have",
                "how many transactions are in my account",
                "count my transactions",
                "كم عدد المعاملات",
                "عدد المعاملات",
                "كم عملية عندي");
    }

    public boolean isAccountBalanceQuestion(String message) {
        String normalized = normalize(message);
        return containsAny(normalized,
                "mon solde",
                "solde de mon compte",
                "solde compte",
                "solde du compte",
                "combien d'argent dans mon compte",
                "combien argent dans mon compte",
                "combien j'ai dans mon compte",
                "combien jai dans mon compte",
                "combien ai-je dans mon compte",
                "argent dans mon compte",
                "account balance",
                "what is my account balance",
                "how much do i have in my account",
                "how much money in my account",
                "money in my account",
                "checking balance",
                "bank balance",
                "رصيد الحساب",
                "الرصيد",
                "كم في حسابي",
                "كم عندي في حسابي",
                "كم عندي في الحساب");
    }

    private boolean isGreeting(String message) {
        return containsAny(message, "bonjour", "salut", "bonsoir", "hello", "hi", "hey", "مرحبا", "السلام");
    }

    private boolean isIdentityQuestion(String message) {
        return containsAny(message,
                "comment tu t'appelles",
                "comment tu t appelles",
                "ton nom",
                "qui es-tu",
                "qui es tu",
                "who are you",
                "what is your name",
                "what's your name",
                "اسمك",
                "من انت",
                "من أنت");
    }

    private boolean isSalaryQuestion(String message) {
        return containsAny(message, "mon salaire", "salaire", "salary", "my salary", "revenu mensuel", "راتبي", "الراتب");
    }

    private boolean isSavingsBalanceQuestion(String message) {
        return containsAny(message,
                "solde epargne",
                "solde épargne",
                "epargne disponible",
                "épargne disponible",
                "savings balance",
                "saving balance",
                "رصيد الادخار",
                "رصيد التوفير",
                "الادخار");
    }

    private boolean isBudgetHelp(String message) {
        return containsAny(message,
                "budget",
                "planifier mes depenses",
                "planifier mes dépenses",
                "organiser mes finances",
                "budget help",
                "budgeting",
                "ميزانية",
                "إدارة الميزانية");
    }

    private boolean isExpenseAnalysis(String message) {
        return containsAny(message,
                "mes depenses",
                "mes dépenses",
                "analyser mes depenses",
                "analyse expenses",
                "where do i spend",
                "spending analysis",
                "مصاريفي",
                "تحليل المصاريف");
    }

    private boolean isSavingsGoal(String message, String objective) {
        return StringUtils.hasText(objective)
                || containsAny(message,
                "epargne",
                "épargne",
                "economiser",
                "économiser",
                "save more",
                "savings goal",
                "emergency fund",
                "ادخار",
                "هدف الادخار");
    }

    private boolean isProjectFinancing(String message, String objective) {
        return containsAny(message,
                "projet",
                "financer",
                "acheter une voiture",
                "acheter maison",
                "project financing",
                "finance a project",
                "loan for project",
                "مشروع",
                "تمويل",
                "شراء سيارة",
                "شراء منزل")
                || objective.contains("projet")
                || objective.contains("project")
                || objective.contains("مشروع");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
