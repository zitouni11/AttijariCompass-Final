package com.adem.attijari_compass.model.storytelling;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AssistantEmotion {
    FRIENDLY("friendly"),
    CALM("calm"),
    ENCOURAGING("encouraging"),
    INFORMATIVE("informative");

    private final String value;

    AssistantEmotion(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
