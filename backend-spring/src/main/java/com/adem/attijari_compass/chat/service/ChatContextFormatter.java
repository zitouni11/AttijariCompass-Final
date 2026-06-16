package com.adem.attijari_compass.chat.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ChatContextFormatter {

    public String buildSection(String title, List<String> rawLines) {
        String lines = rawLines.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(System.lineSeparator()));

        if (!StringUtils.hasText(lines)) {
            lines = "- Aucune donnee exploitable pour cette section.";
        }

        return "[" + title + "]" + System.lineSeparator() + lines;
    }

    public String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    public String compact(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public String normalizeKeywordText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
