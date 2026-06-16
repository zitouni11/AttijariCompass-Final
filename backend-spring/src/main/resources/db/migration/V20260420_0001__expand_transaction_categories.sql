ALTER TABLE public."transaction"
    DROP CONSTRAINT IF EXISTS transaction_category_check;

ALTER TABLE public."transaction"
    ADD CONSTRAINT transaction_category_check
    CHECK (
        category IS NULL OR category IN (
            'ALIMENTATION', 'AUTRES', 'BANQUE', 'BEAUTE', 'CAFES', 'DISTRIBUTION',
            'DIVERTISSEMENT', 'EPARGNE', 'FACTURES', 'HOTEL', 'IMPORT_EXPORT',
            'LIVRAISON', 'LOGEMENT', 'NETTOYAGE', 'OPERATEURS_TELEPHONIQUES',
            'RESTAURANT', 'SALAIRE', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
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
            'DIVERTISSEMENT', 'EPARGNE', 'FACTURES', 'HOTEL', 'IMPORT_EXPORT',
            'LIVRAISON', 'LOGEMENT', 'NETTOYAGE', 'OPERATEURS_TELEPHONIQUES',
            'RESTAURANT', 'SALAIRE', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
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
            'DIVERTISSEMENT', 'EPARGNE', 'FACTURES', 'HOTEL', 'IMPORT_EXPORT',
            'LIVRAISON', 'LOGEMENT', 'NETTOYAGE', 'OPERATEURS_TELEPHONIQUES',
            'RESTAURANT', 'SALAIRE', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
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
            'DIVERTISSEMENT', 'EPARGNE', 'FACTURES', 'HOTEL', 'IMPORT_EXPORT',
            'LIVRAISON', 'LOGEMENT', 'NETTOYAGE', 'OPERATEURS_TELEPHONIQUES',
            'RESTAURANT', 'SALAIRE', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
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
            'DIVERTISSEMENT', 'EPARGNE', 'FACTURES', 'HOTEL', 'IMPORT_EXPORT',
            'LIVRAISON', 'LOGEMENT', 'NETTOYAGE', 'OPERATEURS_TELEPHONIQUES',
            'RESTAURANT', 'SALAIRE', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
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
            'DIVERTISSEMENT', 'EPARGNE', 'FACTURES', 'HOTEL', 'IMPORT_EXPORT',
            'LIVRAISON', 'LOGEMENT', 'NETTOYAGE', 'OPERATEURS_TELEPHONIQUES',
            'RESTAURANT', 'SALAIRE', 'SANTE', 'SERVICE_AUTO', 'SHOPPING',
            'STATION_SERVICES', 'STEG_SONEDE', 'SUPERMARCHE', 'TECHNOLOGIE',
            'TRANSPORT', 'VOYAGE', 'EDUCATION'
        )
    ) NOT VALID;

UPDATE public."transaction"
SET category = CASE
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS') THEN 'RESTAURANT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'INVOICE', 'INVOICES', 'UTILITIES', 'UTILITY') THEN 'FACTURES'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'RENT', 'LOYER', 'APARTMENT', 'RESIDENCE', 'HOUSE', 'HOME') THEN 'LOGEMENT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'WAGE', 'BONUS', 'PRIME') THEN 'SALAIRE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('EPARGNE', 'SAVING', 'SAVINGS', 'INVESTMENT', 'INVESTMENTS', 'INVESTISSEMENT', 'COMPTE_EPARGNE') THEN 'EPARGNE'
    ELSE category
END;

UPDATE public.test_card_transaction
SET category_suggestion = CASE
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS') THEN 'RESTAURANT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'INVOICE', 'INVOICES', 'UTILITIES', 'UTILITY') THEN 'FACTURES'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'RENT', 'LOYER', 'APARTMENT', 'RESIDENCE', 'HOUSE', 'HOME') THEN 'LOGEMENT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'WAGE', 'BONUS', 'PRIME') THEN 'SALAIRE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category_suggestion, '')), '[^A-Z0-9]+', '_', 'g') IN ('EPARGNE', 'SAVING', 'SAVINGS', 'INVESTMENT', 'INVESTMENTS', 'INVESTISSEMENT', 'COMPTE_EPARGNE') THEN 'EPARGNE'
    ELSE category_suggestion
END;

UPDATE public.budget_target
SET category = CASE
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS') THEN 'RESTAURANT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'INVOICE', 'INVOICES', 'UTILITIES', 'UTILITY') THEN 'FACTURES'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'RENT', 'LOYER', 'APARTMENT', 'RESIDENCE', 'HOUSE', 'HOME') THEN 'LOGEMENT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'WAGE', 'BONUS', 'PRIME') THEN 'SALAIRE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('EPARGNE', 'SAVING', 'SAVINGS', 'INVESTMENT', 'INVESTMENTS', 'INVESTISSEMENT', 'COMPTE_EPARGNE') THEN 'EPARGNE'
    ELSE category
END;

UPDATE public.transaction_cash_breakdown
SET category = CASE
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS') THEN 'RESTAURANT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'INVOICE', 'INVOICES', 'UTILITIES', 'UTILITY') THEN 'FACTURES'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'RENT', 'LOYER', 'APARTMENT', 'RESIDENCE', 'HOUSE', 'HOME') THEN 'LOGEMENT'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'WAGE', 'BONUS', 'PRIME') THEN 'SALAIRE'
    WHEN REGEXP_REPLACE(UPPER(COALESCE(category, '')), '[^A-Z0-9]+', '_', 'g') IN ('EPARGNE', 'SAVING', 'SAVINGS', 'INVESTMENT', 'INVESTMENTS', 'INVESTISSEMENT', 'COMPTE_EPARGNE') THEN 'EPARGNE'
    ELSE category
END;

UPDATE public.transaction_category_feedback
SET predicted_category = CASE
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS') THEN 'RESTAURANT'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'INVOICE', 'INVOICES', 'UTILITIES', 'UTILITY') THEN 'FACTURES'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'RENT', 'LOYER', 'APARTMENT', 'RESIDENCE', 'HOUSE', 'HOME') THEN 'LOGEMENT'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'WAGE', 'BONUS', 'PRIME') THEN 'SALAIRE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(predicted_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('EPARGNE', 'SAVING', 'SAVINGS', 'INVESTMENT', 'INVESTMENTS', 'INVESTISSEMENT', 'COMPTE_EPARGNE') THEN 'EPARGNE'
        ELSE predicted_category
    END,
    corrected_category = CASE
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('RESTAURANT', 'RESTAURATION', 'DINING', 'RESTAURANTS') THEN 'RESTAURANT'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('FACTURES', 'FACTURE', 'BILL', 'BILLS', 'INVOICE', 'INVOICES', 'UTILITIES', 'UTILITY') THEN 'FACTURES'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('LOGEMENT', 'HOUSING', 'LODGING', 'RENT', 'LOYER', 'APARTMENT', 'RESIDENCE', 'HOUSE', 'HOME') THEN 'LOGEMENT'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('SALAIRE', 'SALARY', 'PAYROLL', 'WAGE', 'BONUS', 'PRIME') THEN 'SALAIRE'
        WHEN REGEXP_REPLACE(UPPER(COALESCE(corrected_category, '')), '[^A-Z0-9]+', '_', 'g') IN ('EPARGNE', 'SAVING', 'SAVINGS', 'INVESTMENT', 'INVESTMENTS', 'INVESTISSEMENT', 'COMPTE_EPARGNE') THEN 'EPARGNE'
        ELSE corrected_category
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
