package com.adem.attijari_compass.util;

public final class CardMaskingUtil {

    private CardMaskingUtil() {
    }

    public static String normalizeCardNumber(String cardNumber) {
        return cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
    }

    public static String maskCardNumber(String cardNumber) {
        String normalized = normalizeCardNumber(cardNumber);
        if (normalized.length() < 4) {
            return "****";
        }

        String last4 = normalized.substring(normalized.length() - 4);
        return "**** **** **** " + last4;
    }

    public static String extractLast4(String cardNumber) {
        String normalized = normalizeCardNumber(cardNumber);
        if (normalized.length() < 4) {
            return normalized;
        }

        return normalized.substring(normalized.length() - 4);
    }
}
