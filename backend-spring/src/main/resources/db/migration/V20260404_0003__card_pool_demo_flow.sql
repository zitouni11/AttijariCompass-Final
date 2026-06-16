CREATE TABLE IF NOT EXISTS card_pool (
    id BIGSERIAL PRIMARY KEY,
    card_catalog_id BIGINT NOT NULL,
    card_holder_name VARCHAR(150) NOT NULL,
    card_number VARCHAR(25) NOT NULL,
    expiry_month INTEGER NOT NULL,
    expiry_year INTEGER NOT NULL,
    cvv VARCHAR(4),
    masked_card_number VARCHAR(25) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    card_code VARCHAR(50) NOT NULL,
    assigned BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_card_pool_card_code ON card_pool (card_code);
CREATE INDEX IF NOT EXISTS idx_card_pool_card_catalog_id ON card_pool (card_catalog_id);
CREATE INDEX IF NOT EXISTS idx_card_pool_assigned ON card_pool (assigned);
CREATE INDEX IF NOT EXISTS idx_card_pool_assigned_user_id ON card_pool (assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_card_pool_last4 ON card_pool (last4);

ALTER TABLE card_pool
    DROP CONSTRAINT IF EXISTS fk_card_pool_card_catalog;

ALTER TABLE card_pool
    ADD CONSTRAINT fk_card_pool_card_catalog
        FOREIGN KEY (card_catalog_id) REFERENCES card_catalog (id);

ALTER TABLE card_pool
    DROP CONSTRAINT IF EXISTS fk_card_pool_assigned_user;

ALTER TABLE card_pool
    ADD CONSTRAINT fk_card_pool_assigned_user
        FOREIGN KEY (assigned_user_id) REFERENCES app_user (id);

CREATE TABLE IF NOT EXISTS card_pool_transaction (
    id BIGSERIAL PRIMARY KEY,
    card_pool_id BIGINT NOT NULL,
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
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    installment BOOLEAN NOT NULL DEFAULT FALSE,
    installment_index INTEGER,
    installment_total INTEGER,
    external_reference VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_card_pool_transaction_card_pool_id ON card_pool_transaction (card_pool_id);
CREATE INDEX IF NOT EXISTS idx_card_pool_transaction_transaction_date ON card_pool_transaction (transaction_date);
CREATE INDEX IF NOT EXISTS idx_card_pool_transaction_pool_date ON card_pool_transaction (card_pool_id, transaction_date);
CREATE UNIQUE INDEX IF NOT EXISTS uk_card_pool_transaction_pool_external_reference
    ON card_pool_transaction (card_pool_id, external_reference);

ALTER TABLE card_pool_transaction
    DROP CONSTRAINT IF EXISTS chk_card_pool_transaction_currency_tnd;

ALTER TABLE card_pool_transaction
    ADD CONSTRAINT chk_card_pool_transaction_currency_tnd
        CHECK (currency = 'TND');

ALTER TABLE card_pool_transaction
    DROP CONSTRAINT IF EXISTS chk_card_pool_transaction_installments;

ALTER TABLE card_pool_transaction
    ADD CONSTRAINT chk_card_pool_transaction_installments
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
        );

ALTER TABLE card_pool_transaction
    DROP CONSTRAINT IF EXISTS fk_card_pool_transaction_card_pool;

ALTER TABLE card_pool_transaction
    ADD CONSTRAINT fk_card_pool_transaction_card_pool
        FOREIGN KEY (card_pool_id) REFERENCES card_pool (id);

ALTER TABLE user_card
    ADD COLUMN IF NOT EXISTS card_pool_id BIGINT,
    ADD COLUMN IF NOT EXISTS card_number VARCHAR(25),
    ADD COLUMN IF NOT EXISTS card_code VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_user_card_card_pool_id ON user_card (card_pool_id);
CREATE INDEX IF NOT EXISTS idx_user_card_card_code ON user_card (card_code);

ALTER TABLE user_card
    DROP CONSTRAINT IF EXISTS fk_user_card_card_pool;

ALTER TABLE user_card
    ADD CONSTRAINT fk_user_card_card_pool
        FOREIGN KEY (card_pool_id) REFERENCES card_pool (id);

UPDATE user_card uc
SET card_number = cp.card_number,
    card_code = cp.card_code,
    source_type = 'DEMO_POOL'
FROM card_pool cp
WHERE uc.card_pool_id = cp.id
  AND (uc.card_number IS NULL OR uc.card_code IS NULL OR uc.source_type IS DISTINCT FROM 'DEMO_POOL');

ALTER TABLE card_transaction
    ALTER COLUMN currency SET DEFAULT 'TND';

UPDATE card_transaction
SET currency = 'TND'
WHERE currency IS NULL
   OR currency <> 'TND';

ALTER TABLE card_transaction
    DROP CONSTRAINT IF EXISTS chk_card_transaction_currency_tnd;

ALTER TABLE card_transaction
    ADD CONSTRAINT chk_card_transaction_currency_tnd
        CHECK (currency = 'TND');
