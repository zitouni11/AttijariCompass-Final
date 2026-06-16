CREATE TABLE IF NOT EXISTS budget_target (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    category_label VARCHAR(100) NOT NULL,
    source VARCHAR(30) NOT NULL,
    recommendation_id VARCHAR(150),
    recommendation_title VARCHAR(255),
    suggested_monthly_amount NUMERIC(19, 2),
    selected_level VARCHAR(20) NOT NULL,
    summary VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_target_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT chk_budget_target_amount
        CHECK (suggested_monthly_amount IS NULL OR suggested_monthly_amount >= 0),
    CONSTRAINT chk_budget_target_source
        CHECK (source IN ('RECOMMENDATION_AI', 'MANUAL')),
    CONSTRAINT chk_budget_target_level
        CHECK (selected_level IN ('PRUDENT', 'EQUILIBRE', 'RENFORCE')),
    CONSTRAINT chk_budget_target_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_budget_target_user_status_created
    ON budget_target (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_budget_target_user_category_status
    ON budget_target (user_id, category, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_budget_target_active_user_category
    ON budget_target (user_id, category)
    WHERE status = 'ACTIVE';
