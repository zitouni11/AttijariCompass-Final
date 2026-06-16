package com.adem.attijari_compass.model.storytelling;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AssistantAction {
    NONE("none"),
    SHOW_BUDGET("show_budget"),
    SHOW_SAVINGS_PLAN("show_savings_plan"),
    ASK_GOAL("ask_goal");

    private final String value;

    AssistantAction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
