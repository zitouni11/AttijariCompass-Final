package com.adem.attijari_compass.model.storytelling;

public enum ConversationLanguage {
    FRENCH("fr"),
    ENGLISH("en"),
    ARABIC("ar");

    private final String code;

    ConversationLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
