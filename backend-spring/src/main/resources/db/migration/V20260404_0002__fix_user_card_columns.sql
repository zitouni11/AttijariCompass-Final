ALTER TABLE user_card
    ADD COLUMN IF NOT EXISTS card_catalog_id BIGINT,
    ADD COLUMN IF NOT EXISTS card_holder_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS last4 VARCHAR(4),
    ADD COLUMN IF NOT EXISTS expiry_month INTEGER,
    ADD COLUMN IF NOT EXISTS expiry_year INTEGER,
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(100),
    ADD COLUMN IF NOT EXISTS card_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS linked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS primary_card BOOLEAN,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE user_card
    ALTER COLUMN primary_card SET DEFAULT FALSE;

UPDATE user_card
SET primary_card = FALSE
WHERE primary_card IS NULL;

ALTER TABLE user_card
    ALTER COLUMN primary_card SET NOT NULL;

UPDATE user_card
SET card_holder_name = holder_name
WHERE card_holder_name IS NULL
  AND holder_name IS NOT NULL;

UPDATE user_card
SET last4 = RIGHT(REGEXP_REPLACE(masked_card_number, '[^0-9]', '', 'g'), 4)
WHERE last4 IS NULL
  AND masked_card_number IS NOT NULL;

UPDATE user_card uc
SET expiry_month = COALESCE(uc.expiry_month, t.expiry_month),
    expiry_year = COALESCE(uc.expiry_year, t.expiry_year)
FROM test_card_catalog t
WHERE uc.linked_test_card_id = t.id
  AND (uc.expiry_month IS NULL OR uc.expiry_year IS NULL);

UPDATE user_card
SET card_status = status
WHERE card_status IS NULL
  AND status IS NOT NULL;

UPDATE user_card
SET linked_at = COALESCE(connected_at, CURRENT_TIMESTAMP)
WHERE linked_at IS NULL;

UPDATE user_card
SET source_type = CASE
    WHEN linked_test_card_id IS NOT NULL THEN 'SANDBOX'
    ELSE 'MANUAL'
END
WHERE source_type IS NULL;

UPDATE user_card
SET created_at = COALESCE(connected_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL;

UPDATE user_card
SET updated_at = COALESCE(last_sync_at, created_at, connected_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE user_card
    ALTER COLUMN linked_test_card_id DROP NOT NULL;
