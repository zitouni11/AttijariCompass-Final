package com.adem.attijari_compass.entity;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public enum TransactionCategory {
    ALIMENTATION("Alimentation", "shopping_cart"),
    AUTRES("Autres", "account_balance_wallet"),
    BANQUE("Banque", "account_balance"),
    BEAUTE("Beaute", "content_cut"),
    CAFES("Cafes", "local_cafe"),
    DISTRIBUTION("Distribution", "storefront"),
    DIVERTISSEMENT("Divertissement", "stadia_controller"),
    EPARGNE("Epargne", "savings"),
    FACTURES("Factures", "description"),
    HOTEL("Hotel", "hotel"),
    IMPORT_EXPORT("Import/export", "swap_horiz"),
    LIVRAISON("Livraison", "delivery_dining"),
    LOGEMENT("Logement", "home"),
    NETTOYAGE("Nettoyage", "cleaning_services"),
    OPERATEURS_TELEPHONIQUES("Operateurs telephoniques", "perm_phone_msg"),
    RESTAURANT("Restaurant", "restaurant"),
    SALAIRE("Salaire", "payments"),
    SANTE("Sante", "health_and_safety"),
    SERVICE_AUTO("Service auto", "car_repair"),
    SHOPPING("Shopping", "shopping_bag"),
    STATION_SERVICES("Station-services", "local_gas_station"),
    STEG_SONEDE("Steg/Sonede", "receipt_long"),
    SUPERMARCHE("Supermarche", "local_grocery_store"),
    TECHNOLOGIE("Technologie", "devices"),
    TRANSPORT("Transport", "commute"),
    VOYAGE("Voyage", "flight"),
    EDUCATION("Education", "school");

    private static final Map<String, TransactionCategory> LOOKUP = buildLookup();

    private final String label;
    private final String iconName;

    TransactionCategory(String label, String iconName) {
        this.label = label;
        this.iconName = iconName;
    }

    public String label() {
        return label;
    }

    public String lowerLabel() {
        return label.toLowerCase(Locale.ROOT);
    }

    public String iconName() {
        return iconName;
    }

    public static TransactionCategory fallback() {
        return AUTRES;
    }

    public static TransactionCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return fallback();
        }

        TransactionCategory match = LOOKUP.get(normalizeKey(value));
        return match != null ? match : fallback();
    }

    private static Map<String, TransactionCategory> buildLookup() {
        Map<String, TransactionCategory> lookup = new LinkedHashMap<>();

        for (TransactionCategory category : values()) {
            register(lookup, category, category.name());
            register(lookup, category, category.label());
        }

        register(lookup, AUTRES, "AUTRE", "OTHER", "OTHERS");
        register(lookup, SALAIRE, "SALARY", "PAYROLL", "WAGE", "BONUS", "PRIME");
        register(lookup, EPARGNE, "SAVING", "SAVINGS", "INVESTMENT", "INVESTMENTS", "INVESTISSEMENT", "COMPTE_EPARGNE");
        register(lookup, ALIMENTATION, "FOOD", "FOODS", "GROCERY", "GROCERIES", "ALIMENTAIRE", "COURSE", "COURSES");
        register(lookup, RESTAURANT, "RESTAURATION", "DINING", "RESTAURANTS");
        register(lookup, CAFES, "CAFE", "COFFEE", "CAFETERIA");
        register(lookup, TRANSPORT, "TRANSPORTATION", "MOBILITY", "MOBILITE");
        register(lookup, FACTURES, "FACTURE", "BILL", "BILLS", "INVOICE", "INVOICES", "UTILITIES", "UTILITY");
        register(lookup, TECHNOLOGIE, "ABONNEMENT", "ABONNEMENTS", "SUBSCRIPTION", "SUBSCRIPTIONS", "TECH");
        register(lookup, BANQUE, "TRANSFERT", "TRANSFER", "TRANSFERS", "VIREMENT", "FRAIS_BANCAIRES", "BANK_FEES", "BANK_FEE");
        register(lookup, DIVERTISSEMENT, "LOISIRS", "LEISURE", "ENTERTAINMENT");
        register(lookup, LOGEMENT, "HOUSING", "LODGING", "RENT", "LOYER", "APARTMENT", "RESIDENCE", "HOUSE", "HOME");
        register(lookup, HOTEL, "HOTELS", "AIRBNB", "BOOKING");
        register(lookup, OPERATEURS_TELEPHONIQUES, "TELECOM", "TELEPHONE", "PHONE_OPERATORS", "PHONE_OPERATOR");
        register(lookup, STATION_SERVICES, "FUEL", "GAS_STATION", "SERVICE_STATION", "STATION_SERVICE");
        register(lookup, SERVICE_AUTO, "AUTO", "CAR_SERVICE", "CAR_REPAIR");
        register(lookup, BEAUTE, "BEAUTY", "COSMETIC", "COSMETICS", "SALON", "BARBER", "SPA");
        register(lookup, LIVRAISON, "DELIVERY", "DELIVERIES");
        register(lookup, VOYAGE, "TRAVEL", "TRAVELS", "VACANCE", "VACANCES");
        register(lookup, SUPERMARCHE, "SUPERMARKET", "SUPERMARKETS");
        register(lookup, SANTE, "HEALTH", "MEDICAL");

        return Map.copyOf(lookup);
    }

    private static void register(Map<String, TransactionCategory> lookup, TransactionCategory category, String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            lookup.put(normalizeKey(value), category);
        }
    }

    private static String normalizeKey(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('&', ' ')
                .replace('/', ' ')
                .replace('-', ' ')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_");

        if (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
