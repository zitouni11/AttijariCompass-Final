ALTER TABLE app_user ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
UPDATE app_user SET active = TRUE WHERE active IS NULL;
ALTER TABLE app_user ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE app_user ALTER COLUMN active SET NOT NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    category VARCHAR(40) NOT NULL,
    message VARCHAR(3000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    admin_reply VARCHAR(3000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS general_notifications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(3000) NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT,
    actor_email VARCHAR(255),
    actor_role VARCHAR(60),
    action VARCHAR(80) NOT NULL,
    module VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module ON audit_logs(module);

CREATE TABLE IF NOT EXISTS app_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(120) NOT NULL UNIQUE,
    setting_value VARCHAR(3000) NOT NULL,
    type VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

ALTER TABLE app_settings ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
UPDATE app_settings SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

INSERT INTO app_settings(setting_key, setting_value, type, description, updated_at, updated_by)
VALUES
    ('maintenanceMode', 'false', 'BOOLEAN', 'Mode maintenance global', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('chatbotEnabled', 'true', 'BOOLEAN', 'Activation visuelle du chatbot', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('importsEnabled', 'true', 'BOOLEAN', 'Activation des imports utilisateurs', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('recommendationsEnabled', 'true', 'BOOLEAN', 'Activation des recommandations', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('maxImportFileSizeMb', '10', 'NUMBER', 'Taille maximale des fichiers importes en Mo', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('welcomeMessage', 'Bienvenue sur Attijari Compass', 'STRING', 'Message d accueil global', CURRENT_TIMESTAMP, 'SYSTEM')
ON CONFLICT (setting_key) DO NOTHING;
