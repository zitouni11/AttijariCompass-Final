CREATE TABLE IF NOT EXISTS account_restore_requests (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    verification_code_hash VARCHAR(255),
    verification_code_expires_at TIMESTAMP NULL,
    verification_attempts INTEGER DEFAULT 0,
    email_verified BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    approved_at TIMESTAMP NULL,
    approved_by BIGINT NULL,
    rejected_at TIMESTAMP NULL,
    rejected_by BIGINT NULL,
    rejection_reason TEXT NULL
);

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS verification_code_hash VARCHAR(255);

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP NULL;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS verification_attempts INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'PENDING_EMAIL_VERIFICATION';

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP NULL;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP NULL;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS approved_by BIGINT NULL;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP NULL;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS rejected_by BIGINT NULL;

ALTER TABLE IF EXISTS account_restore_requests
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT NULL;

CREATE INDEX IF NOT EXISTS idx_account_restore_requests_email
    ON account_restore_requests (email);

CREATE INDEX IF NOT EXISTS idx_account_restore_requests_status
    ON account_restore_requests (status);
