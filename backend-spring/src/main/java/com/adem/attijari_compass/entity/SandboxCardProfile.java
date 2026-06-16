package com.adem.attijari_compass.entity;

import com.adem.attijari_compass.exception.InvalidProfileValueException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public enum SandboxCardProfile {
    STUDENT,
    SALARIED,
    FAMILY,
    PREMIUM;

    @JsonCreator
    public static SandboxCardProfile fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidProfileValueException();
        }

        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(profile -> matchesProfile(normalized, profile))
                .findFirst()
                .orElseThrow(InvalidProfileValueException::new);
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    private static boolean matchesProfile(String normalized, SandboxCardProfile profile) {
        String enumName = profile.name();
        return normalized.equals(enumName)
                || normalized.startsWith(enumName + "_");
    }

    private static String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
