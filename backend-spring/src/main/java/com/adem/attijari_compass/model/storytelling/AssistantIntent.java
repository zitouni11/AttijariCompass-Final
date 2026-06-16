package com.adem.attijari_compass.model.storytelling;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AssistantIntent {
    GREETING("greeting"),
    IDENTITY("identity"),
    SALARY_INFO("salary_info"),
    ACCOUNT_BALANCE("account_balance"),
    TRANSACTION_COUNT("transaction_count"),
    SAVINGS_BALANCE("savings_balance"),
    BUDGET_HELP("budget_help"),
    EXPENSE_ANALYSIS("expense_analysis"),
    SAVINGS_GOAL("savings_goal"),
    PROJECT_FINANCING("project_financing"),
    UNKNOWN("unknown");

    private final String value;

    AssistantIntent(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
