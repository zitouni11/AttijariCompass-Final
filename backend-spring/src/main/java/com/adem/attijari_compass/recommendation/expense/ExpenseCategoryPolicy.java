package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.TransactionCategory;

public final class ExpenseCategoryPolicy {

    private ExpenseCategoryPolicy() {
    }

    public static ExpenseCategoryProfile resolveProfile(TransactionCategory category) {
        if (category == null) {
            return ExpenseCategoryProfile.AMBIGUOUS;
        }

        switch (category) {
            case CAFES:
            case RESTAURANT:
            case DIVERTISSEMENT:
            case HOTEL:
            case VOYAGE:
            case BEAUTE:
            case SHOPPING:
                return ExpenseCategoryProfile.DISCRETIONARY;
            case ALIMENTATION:
            case SUPERMARCHE:
            case DISTRIBUTION:
            case TRANSPORT:
            case STATION_SERVICES:
            case SERVICE_AUTO:
            case LIVRAISON:
            case FACTURES:
            case SANTE:
            case EDUCATION:
                return ExpenseCategoryProfile.ESSENTIAL_VARIABLE;
            case LOGEMENT:
            case OPERATEURS_TELEPHONIQUES:
            case STEG_SONEDE:
            case BANQUE:
                return ExpenseCategoryProfile.FIXED_STRUCTURAL;
            case SALAIRE:
            case EPARGNE:
                return ExpenseCategoryProfile.EXCLUDED;
            case IMPORT_EXPORT:
            case TECHNOLOGIE:
            case NETTOYAGE:
            case AUTRES:
            default:
                return ExpenseCategoryProfile.AMBIGUOUS;
        }
    }

    public static boolean isExpenseEligible(TransactionCategory category) {
        return category != null && resolveProfile(category) != ExpenseCategoryProfile.EXCLUDED;
    }

    public static boolean isFixedStructural(TransactionCategory category) {
        return resolveProfile(category) == ExpenseCategoryProfile.FIXED_STRUCTURAL;
    }
}
