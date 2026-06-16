ALTER TABLE public."transaction"
    DROP CONSTRAINT IF EXISTS transaction_category_check;

ALTER TABLE public."transaction"
    ADD CONSTRAINT transaction_category_check
    CHECK (
        category IS NULL OR category IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'HOTEL', 'IMPORT_EXPORT', 'LIVRAISON', 'NETTOYAGE',
            'OPERATEURS_TELEPHONIQUES', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

ALTER TABLE public.test_card_transaction
    DROP CONSTRAINT IF EXISTS test_card_transaction_category_suggestion_check;

ALTER TABLE public.test_card_transaction
    ADD CONSTRAINT test_card_transaction_category_suggestion_check
    CHECK (
        category_suggestion IS NULL OR category_suggestion IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'HOTEL', 'IMPORT_EXPORT', 'LIVRAISON', 'NETTOYAGE',
            'OPERATEURS_TELEPHONIQUES', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

ALTER TABLE public.budget_target
    DROP CONSTRAINT IF EXISTS budget_target_category_check;

ALTER TABLE public.budget_target
    ADD CONSTRAINT budget_target_category_check
    CHECK (
        category IS NULL OR category IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'HOTEL', 'IMPORT_EXPORT', 'LIVRAISON', 'NETTOYAGE',
            'OPERATEURS_TELEPHONIQUES', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

ALTER TABLE public.transaction_cash_breakdown
    DROP CONSTRAINT IF EXISTS transaction_cash_breakdown_category_check;

ALTER TABLE public.transaction_cash_breakdown
    ADD CONSTRAINT transaction_cash_breakdown_category_check
    CHECK (
        category IS NULL OR category IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'HOTEL', 'IMPORT_EXPORT', 'LIVRAISON', 'NETTOYAGE',
            'OPERATEURS_TELEPHONIQUES', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

ALTER TABLE public.transaction_category_feedback
    DROP CONSTRAINT IF EXISTS transaction_category_feedback_predicted_category_check;

ALTER TABLE public.transaction_category_feedback
    ADD CONSTRAINT transaction_category_feedback_predicted_category_check
    CHECK (
        predicted_category IS NULL OR predicted_category IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'HOTEL', 'IMPORT_EXPORT', 'LIVRAISON', 'NETTOYAGE',
            'OPERATEURS_TELEPHONIQUES', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

ALTER TABLE public.transaction_category_feedback
    DROP CONSTRAINT IF EXISTS transaction_category_feedback_corrected_category_check;

ALTER TABLE public.transaction_category_feedback
    ADD CONSTRAINT transaction_category_feedback_corrected_category_check
    CHECK (
        corrected_category IS NULL OR corrected_category IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'HOTEL', 'IMPORT_EXPORT', 'LIVRAISON', 'NETTOYAGE',
            'OPERATEURS_TELEPHONIQUES', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

UPDATE public."transaction" AS tx
SET category = CASE
    WHEN mapped.norm = '' THEN 'AUTRES'
    WHEN mapped.norm IN ('ALIMENTATION', 'ALIMENTAIRE', 'FOOD', 'FOODS', 'GROCERY', 'GROCERIES', 'COURSE', 'COURSES')
        THEN CASE
            WHEN mapped.corpus ~ '(carrefour|monoprix|supermarche|supermarket|mg)' THEN 'SUPERMARCHE'
            ELSE 'ALIMENTATION'
        END
    WHEN mapped.norm IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA')
        THEN CASE
            WHEN mapped.corpus ~ '(glovo|talabat|delivery|livraison|uber ?eats|ubereats)' THEN 'LIVRAISON'
            ELSE 'CAFES'
        END
    WHEN mapped.norm IN ('TRANSPORT', 'TRANSPORTATION', 'MOBILITY', 'MOBILITE')
        THEN CASE
            WHEN mapped.corpus ~ '(fuel|essence|carburant|station|total|shell|oil)' THEN 'STATION_SERVICES'
            WHEN mapped.corpus ~ '(garage|repair|reparation|vidange|pneu|atelier|car wash)' THEN 'SERVICE_AUTO'
            ELSE 'TRANSPORT'
        END
    WHEN mapped.norm IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
    WHEN mapped.norm IN ('SANTE', 'HEALTH', 'MEDICAL') THEN 'SANTE'
    WHEN mapped.norm IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT')
        THEN CASE
            WHEN mapped.corpus ~ '(hotel|airbnb|booking|resort|hostel)' THEN 'HOTEL'
            WHEN mapped.corpus ~ '(voyage|travel|flight|airline|vacance)' THEN 'VOYAGE'
            ELSE 'DIVERTISSEMENT'
        END
    WHEN mapped.norm IN ('SHOPPING', 'SHOP', 'PURCHASE', 'PURCHASES')
        THEN CASE
            WHEN mapped.corpus ~ '(beauty|beaute|spa|salon|barber|cosmetic)' THEN 'BEAUTE'
            WHEN mapped.corpus ~ '(tech|software|netflix|spotify|laptop|pc|mobile|phone)' THEN 'TECHNOLOGIE'
            ELSE 'SHOPPING'
        END
    WHEN mapped.norm = 'EDUCATION' THEN 'EDUCATION'
    WHEN mapped.norm IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY')
        THEN CASE
            WHEN mapped.corpus ~ '(orange|ooredoo|telecom|telephone|phone|internet|mobile|fibre)' THEN 'OPERATEURS_TELEPHONIQUES'
            ELSE 'STEG_SONEDE'
        END
    WHEN mapped.norm IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
    WHEN mapped.norm IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
    WHEN mapped.norm = 'DISTRIBUTION' THEN 'DISTRIBUTION'
    WHEN mapped.norm = 'IMPORT_EXPORT' THEN 'IMPORT_EXPORT'
    WHEN mapped.norm = 'NETTOYAGE' THEN 'NETTOYAGE'
    WHEN mapped.norm = 'OPERATEURS_TELEPHONIQUES' THEN 'OPERATEURS_TELEPHONIQUES'
    WHEN mapped.norm = 'SERVICE_AUTO' THEN 'SERVICE_AUTO'
    WHEN mapped.norm = 'STATION_SERVICES' THEN 'STATION_SERVICES'
    WHEN mapped.norm = 'STEG_SONEDE' THEN 'STEG_SONEDE'
    WHEN mapped.norm = 'SUPERMARCHE' THEN 'SUPERMARCHE'
    WHEN mapped.norm = 'TECHNOLOGIE' THEN 'TECHNOLOGIE'
    WHEN mapped.norm = 'BANQUE' THEN 'BANQUE'
    WHEN mapped.norm = 'BEAUTE' THEN 'BEAUTE'
    WHEN mapped.norm = 'CAFES' THEN 'CAFES'
    WHEN mapped.norm = 'DIVERTISSEMENT' THEN 'DIVERTISSEMENT'
    WHEN mapped.norm = 'HOTEL' THEN 'HOTEL'
    WHEN mapped.norm = 'LIVRAISON' THEN 'LIVRAISON'
    WHEN mapped.norm = 'SANTE' THEN 'SANTE'
    WHEN mapped.norm = 'SHOPPING' THEN 'SHOPPING'
    WHEN mapped.norm = 'TRANSPORT' THEN 'TRANSPORT'
    WHEN mapped.norm = 'VOYAGE' THEN 'VOYAGE'
    WHEN mapped.norm IN ('AUTRE', 'OTHER', 'OTHERS', 'SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS') THEN 'AUTRES'
    ELSE 'AUTRES'
END
FROM (
    SELECT
        id,
        REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') AS norm,
        LOWER(COALESCE(merchant_name, '') || ' ' || COALESCE(description, '')) AS corpus
    FROM public."transaction"
) AS mapped
WHERE tx.id = mapped.id;

UPDATE public.budget_target AS bt
SET category = CASE
        WHEN mapped.norm IN ('ALIMENTATION', 'ALIMENTAIRE', 'FOOD', 'FOODS', 'GROCERY', 'GROCERIES', 'COURSE', 'COURSES') THEN 'ALIMENTATION'
        WHEN mapped.norm IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA') THEN 'CAFES'
        WHEN mapped.norm IN ('TRANSPORT', 'TRANSPORTATION', 'MOBILITY', 'MOBILITE') THEN 'TRANSPORT'
        WHEN mapped.norm IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
        WHEN mapped.norm IN ('SANTE', 'HEALTH', 'MEDICAL') THEN 'SANTE'
        WHEN mapped.norm IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
        WHEN mapped.norm IN ('SHOPPING', 'SHOP', 'PURCHASE', 'PURCHASES') THEN 'SHOPPING'
        WHEN mapped.norm = 'EDUCATION' THEN 'EDUCATION'
        WHEN mapped.norm IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY') THEN 'STEG_SONEDE'
        WHEN mapped.norm IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
        WHEN mapped.norm IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
        WHEN mapped.norm = 'DISTRIBUTION' THEN 'DISTRIBUTION'
        WHEN mapped.norm = 'IMPORT_EXPORT' THEN 'IMPORT_EXPORT'
        WHEN mapped.norm = 'NETTOYAGE' THEN 'NETTOYAGE'
        WHEN mapped.norm = 'OPERATEURS_TELEPHONIQUES' THEN 'OPERATEURS_TELEPHONIQUES'
        WHEN mapped.norm = 'SERVICE_AUTO' THEN 'SERVICE_AUTO'
        WHEN mapped.norm = 'STATION_SERVICES' THEN 'STATION_SERVICES'
        WHEN mapped.norm = 'STEG_SONEDE' THEN 'STEG_SONEDE'
        WHEN mapped.norm = 'SUPERMARCHE' THEN 'SUPERMARCHE'
        WHEN mapped.norm = 'TECHNOLOGIE' THEN 'TECHNOLOGIE'
        WHEN mapped.norm = 'BANQUE' THEN 'BANQUE'
        WHEN mapped.norm = 'BEAUTE' THEN 'BEAUTE'
        WHEN mapped.norm = 'CAFES' THEN 'CAFES'
        WHEN mapped.norm = 'DIVERTISSEMENT' THEN 'DIVERTISSEMENT'
        WHEN mapped.norm = 'HOTEL' THEN 'HOTEL'
        WHEN mapped.norm = 'LIVRAISON' THEN 'LIVRAISON'
        WHEN mapped.norm = 'SANTE' THEN 'SANTE'
        WHEN mapped.norm = 'SHOPPING' THEN 'SHOPPING'
        WHEN mapped.norm = 'TRANSPORT' THEN 'TRANSPORT'
        WHEN mapped.norm = 'VOYAGE' THEN 'VOYAGE'
        ELSE 'AUTRES'
    END,
    category_label = CASE
        WHEN mapped.norm IN ('ALIMENTATION', 'ALIMENTAIRE', 'FOOD', 'FOODS', 'GROCERY', 'GROCERIES', 'COURSE', 'COURSES') THEN 'Alimentation'
        WHEN mapped.norm IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA') THEN 'Cafes'
        WHEN mapped.norm IN ('TRANSPORT', 'TRANSPORTATION', 'MOBILITY', 'MOBILITE') THEN 'Transport'
        WHEN mapped.norm IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'Hotel'
        WHEN mapped.norm IN ('SANTE', 'HEALTH', 'MEDICAL') THEN 'Sante'
        WHEN mapped.norm IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'Divertissement'
        WHEN mapped.norm IN ('SHOPPING', 'SHOP', 'PURCHASE', 'PURCHASES') THEN 'Shopping'
        WHEN mapped.norm = 'EDUCATION' THEN 'Education'
        WHEN mapped.norm IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY') THEN 'Steg/Sonede'
        WHEN mapped.norm IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'Technologie'
        WHEN mapped.norm IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'Banque'
        WHEN mapped.norm = 'DISTRIBUTION' THEN 'Distribution'
        WHEN mapped.norm = 'IMPORT_EXPORT' THEN 'Import/export'
        WHEN mapped.norm = 'NETTOYAGE' THEN 'Nettoyage'
        WHEN mapped.norm = 'OPERATEURS_TELEPHONIQUES' THEN 'Operateurs telephoniques'
        WHEN mapped.norm = 'SERVICE_AUTO' THEN 'Service auto'
        WHEN mapped.norm = 'STATION_SERVICES' THEN 'Station-services'
        WHEN mapped.norm = 'STEG_SONEDE' THEN 'Steg/Sonede'
        WHEN mapped.norm = 'SUPERMARCHE' THEN 'Supermarche'
        WHEN mapped.norm = 'TECHNOLOGIE' THEN 'Technologie'
        WHEN mapped.norm = 'BANQUE' THEN 'Banque'
        WHEN mapped.norm = 'BEAUTE' THEN 'Beaute'
        WHEN mapped.norm = 'CAFES' THEN 'Cafes'
        WHEN mapped.norm = 'DIVERTISSEMENT' THEN 'Divertissement'
        WHEN mapped.norm = 'HOTEL' THEN 'Hotel'
        WHEN mapped.norm = 'LIVRAISON' THEN 'Livraison'
        WHEN mapped.norm = 'SHOPPING' THEN 'Shopping'
        WHEN mapped.norm = 'TRANSPORT' THEN 'Transport'
        WHEN mapped.norm = 'VOYAGE' THEN 'Voyage'
        ELSE 'Autres'
    END
FROM (
    SELECT
        id,
        REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') AS norm
    FROM public.budget_target
) AS mapped
WHERE bt.id = mapped.id;

UPDATE public.transaction_cash_breakdown
SET category = CASE
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('ALIMENTATION', 'ALIMENTAIRE', 'FOOD', 'FOODS', 'GROCERY', 'GROCERIES', 'COURSE', 'COURSES') THEN 'ALIMENTATION'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA') THEN 'CAFES'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('TRANSPORT', 'TRANSPORTATION', 'MOBILITY', 'MOBILITE') THEN 'TRANSPORT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SANTE', 'HEALTH', 'MEDICAL') THEN 'SANTE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SHOPPING', 'SHOP', 'PURCHASE', 'PURCHASES') THEN 'SHOPPING'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') = 'EDUCATION' THEN 'EDUCATION'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY') THEN 'STEG_SONEDE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS', 'AUTRE', 'OTHER', 'OTHERS') THEN 'AUTRES'
    ELSE COALESCE(category, 'AUTRES')
END;

UPDATE public.transaction_category_feedback
SET predicted_category = CASE
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA') THEN 'CAFES'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY') THEN 'STEG_SONEDE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS', 'AUTRE', 'OTHER', 'OTHERS') THEN 'AUTRES'
        ELSE COALESCE(predicted_category, 'AUTRES')
    END,
    corrected_category = CASE
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA') THEN 'CAFES'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY') THEN 'STEG_SONEDE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS', 'AUTRE', 'OTHER', 'OTHERS') THEN 'AUTRES'
        ELSE COALESCE(corrected_category, 'AUTRES')
    END;

UPDATE public.test_card_transaction
SET category_suggestion = CASE
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA') THEN 'CAFES'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY') THEN 'STEG_SONEDE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS', 'AUTRE', 'OTHER', 'OTHERS') THEN 'AUTRES'
    ELSE COALESCE(category_suggestion, 'AUTRES')
END;

ALTER TABLE public."transaction"
    VALIDATE CONSTRAINT transaction_category_check;

ALTER TABLE public.test_card_transaction
    VALIDATE CONSTRAINT test_card_transaction_category_suggestion_check;

ALTER TABLE public.budget_target
    VALIDATE CONSTRAINT budget_target_category_check;

ALTER TABLE public.transaction_cash_breakdown
    VALIDATE CONSTRAINT transaction_cash_breakdown_category_check;

ALTER TABLE public.transaction_category_feedback
    VALIDATE CONSTRAINT transaction_category_feedback_predicted_category_check;

ALTER TABLE public.transaction_category_feedback
    VALIDATE CONSTRAINT transaction_category_feedback_corrected_category_check;

UPDATE public.card_transaction AS ct
SET category = CASE
    WHEN mapped.norm = '' THEN 'AUTRES'
    WHEN mapped.norm IN ('ALIMENTATION', 'ALIMENTAIRE', 'FOOD', 'FOODS', 'GROCERY', 'GROCERIES', 'COURSE', 'COURSES')
        THEN CASE
            WHEN mapped.corpus ~ '(carrefour|monoprix|supermarche|supermarket|mg)' THEN 'SUPERMARCHE'
            ELSE 'ALIMENTATION'
        END
    WHEN mapped.norm IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA')
        THEN CASE
            WHEN mapped.corpus ~ '(glovo|talabat|delivery|livraison|uber ?eats|ubereats)' THEN 'LIVRAISON'
            ELSE 'CAFES'
        END
    WHEN mapped.norm IN ('TRANSPORT', 'TRANSPORTATION', 'MOBILITY', 'MOBILITE')
        THEN CASE
            WHEN mapped.corpus ~ '(fuel|essence|carburant|station|total|shell|oil)' THEN 'STATION_SERVICES'
            WHEN mapped.corpus ~ '(garage|repair|reparation|vidange|pneu|atelier|car wash)' THEN 'SERVICE_AUTO'
            ELSE 'TRANSPORT'
        END
    WHEN mapped.norm IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
    WHEN mapped.norm IN ('SANTE', 'HEALTH', 'MEDICAL') THEN 'SANTE'
    WHEN mapped.norm IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
    WHEN mapped.norm IN ('SHOPPING', 'SHOP', 'PURCHASE', 'PURCHASES')
        THEN CASE
            WHEN mapped.corpus ~ '(beauty|beaute|spa|salon|barber|cosmetic)' THEN 'BEAUTE'
            WHEN mapped.corpus ~ '(tech|software|netflix|spotify|laptop|pc|mobile|phone)' THEN 'TECHNOLOGIE'
            ELSE 'SHOPPING'
        END
    WHEN mapped.norm = 'EDUCATION' THEN 'EDUCATION'
    WHEN mapped.norm IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY')
        THEN CASE
            WHEN mapped.corpus ~ '(orange|ooredoo|telecom|telephone|phone|internet|mobile|fibre)' THEN 'OPERATEURS_TELEPHONIQUES'
            ELSE 'STEG_SONEDE'
        END
    WHEN mapped.norm IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
    WHEN mapped.norm IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
    WHEN mapped.norm IN ('SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS', 'AUTRE', 'OTHER', 'OTHERS') THEN 'AUTRES'
    ELSE ct.category
END
FROM (
    SELECT
        id,
        REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') AS norm,
        LOWER(COALESCE(merchant_name, '') || ' ' || COALESCE(description, '')) AS corpus
    FROM public.card_transaction
) AS mapped
WHERE ct.id = mapped.id;

UPDATE public.card_pool_transaction AS cp
SET category = CASE
    WHEN mapped.norm = '' THEN 'AUTRES'
    WHEN mapped.norm IN ('ALIMENTATION', 'ALIMENTAIRE', 'FOOD', 'FOODS', 'GROCERY', 'GROCERIES', 'COURSE', 'COURSES')
        THEN CASE
            WHEN mapped.corpus ~ '(carrefour|monoprix|supermarche|supermarket|mg)' THEN 'SUPERMARCHE'
            ELSE 'ALIMENTATION'
        END
    WHEN mapped.norm IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS', 'CAFE', 'COFFEE', 'CAFETERIA')
        THEN CASE
            WHEN mapped.corpus ~ '(glovo|talabat|delivery|livraison|uber ?eats|ubereats)' THEN 'LIVRAISON'
            ELSE 'CAFES'
        END
    WHEN mapped.norm IN ('TRANSPORT', 'TRANSPORTATION', 'MOBILITY', 'MOBILITE')
        THEN CASE
            WHEN mapped.corpus ~ '(fuel|essence|carburant|station|total|shell|oil)' THEN 'STATION_SERVICES'
            WHEN mapped.corpus ~ '(garage|repair|reparation|vidange|pneu|atelier|car wash)' THEN 'SERVICE_AUTO'
            ELSE 'TRANSPORT'
        END
    WHEN mapped.norm IN ('LOGEMENT', 'HOUSING', 'LODGING', 'AIRBNB', 'BOOKING', 'HOTEL', 'HOTELS') THEN 'HOTEL'
    WHEN mapped.norm IN ('SANTE', 'HEALTH', 'MEDICAL') THEN 'SANTE'
    WHEN mapped.norm IN ('LOISIRS', 'LEISURE', 'ENTERTAINMENT') THEN 'DIVERTISSEMENT'
    WHEN mapped.norm IN ('SHOPPING', 'SHOP', 'PURCHASE', 'PURCHASES')
        THEN CASE
            WHEN mapped.corpus ~ '(beauty|beaute|spa|salon|barber|cosmetic)' THEN 'BEAUTE'
            WHEN mapped.corpus ~ '(tech|software|netflix|spotify|laptop|pc|mobile|phone)' THEN 'TECHNOLOGIE'
            ELSE 'SHOPPING'
        END
    WHEN mapped.norm = 'EDUCATION' THEN 'EDUCATION'
    WHEN mapped.norm IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'UTILITIES', 'UTILITY')
        THEN CASE
            WHEN mapped.corpus ~ '(orange|ooredoo|telecom|telephone|phone|internet|mobile|fibre)' THEN 'OPERATEURS_TELEPHONIQUES'
            ELSE 'STEG_SONEDE'
        END
    WHEN mapped.norm IN ('ABONNEMENT', 'ABONNEMENTS', 'SUBSCRIPTION', 'SUBSCRIPTIONS', 'TECH') THEN 'TECHNOLOGIE'
    WHEN mapped.norm IN ('TRANSFERT', 'TRANSFER', 'TRANSFERS', 'VIREMENT', 'FRAIS_BANCAIRES', 'BANK_FEES', 'BANK_FEE') THEN 'BANQUE'
    WHEN mapped.norm IN ('SALAIRE', 'SALARY', 'PAYROLL', 'EPARGNE', 'SAVING', 'SAVINGS', 'AUTRE', 'OTHER', 'OTHERS') THEN 'AUTRES'
    ELSE cp.category
END
FROM (
    SELECT
        id,
        REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') AS norm,
        LOWER(COALESCE(merchant_name, '') || ' ' || COALESCE(description, '')) AS corpus
    FROM public.card_pool_transaction
) AS mapped
WHERE cp.id = mapped.id;
