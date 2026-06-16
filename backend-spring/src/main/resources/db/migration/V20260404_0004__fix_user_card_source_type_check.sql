ALTER TABLE user_card
    DROP CONSTRAINT IF EXISTS user_card_source_type_check;

ALTER TABLE user_card
    ADD CONSTRAINT user_card_source_type_check
        CHECK (
            source_type IS NULL
            OR source_type IN ('SANDBOX', 'DEMO_POOL', 'ATTIJARI_CORE', 'ATTIJARI_EXTERNAL', 'MANUAL')
        );
