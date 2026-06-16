CREATE TABLE IF NOT EXISTS card_catalog (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    brand VARCHAR(50) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    max_payment_limit NUMERIC(19, 2) NOT NULL,
    max_withdrawal_limit NUMERIC(19, 2) NOT NULL,
    allows_online_payment BOOLEAN NOT NULL DEFAULT FALSE,
    allows_international_payment BOOLEAN NOT NULL DEFAULT FALSE,
    allows_installments BOOLEAN NOT NULL DEFAULT FALSE,
    installment_months_max INTEGER,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_card_catalog_code UNIQUE (code),
    CONSTRAINT chk_card_catalog_installments
        CHECK (
            (allows_installments = FALSE AND (installment_months_max IS NULL OR installment_months_max = 0))
            OR (allows_installments = TRUE AND installment_months_max IS NOT NULL AND installment_months_max > 0)
        )
);

CREATE INDEX IF NOT EXISTS idx_card_catalog_active ON card_catalog (active);

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

ALTER TABLE user_card
    ALTER COLUMN linked_test_card_id DROP NOT NULL;

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
SET updated_at = COALESCE(last_sync_at, connected_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_card_user_id ON user_card (user_id);
CREATE INDEX IF NOT EXISTS idx_user_card_card_catalog_id ON user_card (card_catalog_id);
CREATE INDEX IF NOT EXISTS idx_user_card_linked_at ON user_card (linked_at);

ALTER TABLE user_card
    DROP CONSTRAINT IF EXISTS fk_user_card_card_catalog;

ALTER TABLE user_card
    ADD CONSTRAINT fk_user_card_card_catalog
        FOREIGN KEY (card_catalog_id) REFERENCES card_catalog (id);

CREATE TABLE IF NOT EXISTS card_transaction (
    id BIGSERIAL PRIMARY KEY,
    user_card_id BIGINT NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    value_date TIMESTAMP,
    amount NUMERIC(19, 2) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    merchant_name VARCHAR(150),
    description VARCHAR(500),
    reference VARCHAR(100),
    category VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    channel VARCHAR(30),
    city VARCHAR(120),
    country VARCHAR(120),
    currency VARCHAR(3) NOT NULL,
    installment BOOLEAN NOT NULL DEFAULT FALSE,
    installment_index INTEGER,
    installment_total INTEGER,
    external_reference VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_card_transaction_user_card_external_reference UNIQUE (user_card_id, external_reference),
    CONSTRAINT chk_card_transaction_installments
        CHECK (
            (installment = FALSE AND installment_index IS NULL AND installment_total IS NULL)
            OR (
                installment = TRUE
                AND installment_index IS NOT NULL
                AND installment_total IS NOT NULL
                AND installment_index > 0
                AND installment_total > 0
                AND installment_index <= installment_total
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_card_transaction_user_card_id ON card_transaction (user_card_id);
CREATE INDEX IF NOT EXISTS idx_card_transaction_transaction_date ON card_transaction (transaction_date);
CREATE INDEX IF NOT EXISTS idx_card_transaction_user_card_date ON card_transaction (user_card_id, transaction_date);

ALTER TABLE card_transaction
    DROP CONSTRAINT IF EXISTS fk_card_transaction_user_card;

ALTER TABLE card_transaction
    ADD CONSTRAINT fk_card_transaction_user_card
        FOREIGN KEY (user_card_id) REFERENCES user_card (id);

CREATE TABLE IF NOT EXISTS external_card_mapping (
    id BIGSERIAL PRIMARY KEY,
    user_card_id BIGINT NOT NULL,
    external_card_id VARCHAR(120) NOT NULL,
    external_customer_id VARCHAR(120),
    source_system VARCHAR(80) NOT NULL,
    sync_status VARCHAR(30) NOT NULL,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_external_card_mapping_source_card UNIQUE (source_system, external_card_id),
    CONSTRAINT uk_external_card_mapping_user_card_source UNIQUE (user_card_id, source_system)
);

CREATE INDEX IF NOT EXISTS idx_external_card_mapping_user_card_id ON external_card_mapping (user_card_id);
CREATE INDEX IF NOT EXISTS idx_external_card_mapping_source_system ON external_card_mapping (source_system);

ALTER TABLE external_card_mapping
    DROP CONSTRAINT IF EXISTS fk_external_card_mapping_user_card;

ALTER TABLE external_card_mapping
    ADD CONSTRAINT fk_external_card_mapping_user_card
        FOREIGN KEY (user_card_id) REFERENCES user_card (id);
