package com.adem.attijari_compass.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TransactionTextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\s\\u0600-\\u06FF]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TransactionTextNormalizer() {
    }

    public static String normalize(String merchantName, String description) {
        return normalize(joinText(merchantName, description));
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC);
        normalized = PUNCTUATION.matcher(normalized).replaceAll(" ");
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }

    private static String joinText(String merchantName, String description) {
        String merchant = merchantName == null ? "" : merchantName;
        String details = description == null ? "" : description;
        return (merchant + " " + details).trim();
    }
}
