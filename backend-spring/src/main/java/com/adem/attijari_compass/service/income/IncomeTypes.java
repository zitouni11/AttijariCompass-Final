package com.adem.attijari_compass.service.income;

import java.util.Locale;

public final class IncomeTypes {

    public static final String SALAIRE = "salaire";
    public static final String LOYER = "loyer";
    public static final String FREELANCE = "freelance";
    public static final String TRANSFER = "transfer";
    public static final String CASH_DEPOSIT = "cash_deposit";
    public static final String UNKNOWN = "unknown";

    private IncomeTypes() {
    }

    public static String normalize(String rawType) {
        String sanitized = sanitize(rawType);
        if (sanitized.isEmpty()) {
            return UNKNOWN;
        }

        return switch (sanitized) {
            case "salaire", "salary", "payroll" -> SALAIRE;
            case "loyer", "rent" -> LOYER;
            case "freelance", "free lance", "freelance income", "freelance design",
                    "dev freelance", "consulting", "consultant" -> FREELANCE;
            case "transfer", "bank transfer", "received transfer", "money transfer",
                    "transfer recu", "transfert", "virement", "virement recu", "vir" -> TRANSFER;
            case "cash deposit", "cash deposits", "cash deposit recu", "depot espece",
                    "depot especes", "versement espece", "versement especes" -> CASH_DEPOSIT;
            case "unknown", "inconnu" -> UNKNOWN;
            default -> UNKNOWN;
        };
    }

    private static String sanitize(String rawType) {
        if (rawType == null) {
            return "";
        }

        return rawType.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
