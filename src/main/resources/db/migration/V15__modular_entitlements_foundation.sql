-- =============================================================================
-- V15: Modular entitlements foundation
-- Plans, module catalog, promo codes, and grandfather existing orgs to BUSINESS.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Subscription plan catalog
-- -----------------------------------------------------------------------------
CREATE TABLE subscription_plans (
    code                VARCHAR(50) PRIMARY KEY,
    display_name        VARCHAR(120) NOT NULL,
    description         TEXT,
    is_public           BOOLEAN NOT NULL DEFAULT TRUE,
    max_stores          INT NOT NULL DEFAULT 1,
    max_users           INT,
    sort_order          INT NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO subscription_plans (code, display_name, description, is_public, max_stores, max_users, sort_order)
VALUES
    (
        'POS',
        'POS',
        'Freemium plan with core point-of-sale features.',
        TRUE,
        1,
        NULL,
        10
    ),
    (
        'BUSINESS',
        'Business',
        'Full cloud + e-invoicing bundle for growing businesses.',
        TRUE,
        1,
        NULL,
        20
    ),
    (
        'SCALE',
        'Scale',
        'Multi-store operations with advanced modules.',
        TRUE,
        1,
        NULL,
        30
    );

-- Internal / operator-only plan (never shown in public billing UI)
INSERT INTO subscription_plans (code, display_name, description, is_public, max_stores, max_users, sort_order)
VALUES
    (
        'INTERNAL',
        'Internal',
        'Operator-granted unlimited access for testers and partners.',
        FALSE,
        99,
        NULL,
        99
    );

-- -----------------------------------------------------------------------------
-- 2. Modules included per plan
-- -----------------------------------------------------------------------------
CREATE TABLE plan_module_grants (
    plan_code       VARCHAR(50) NOT NULL REFERENCES subscription_plans (code),
    module_code     VARCHAR(50) NOT NULL,
    PRIMARY KEY (plan_code, module_code)
);

-- POS freemium: core sales + basic inventory only
INSERT INTO plan_module_grants (plan_code, module_code) VALUES
    ('POS', 'CORE'),
    ('POS', 'INVENTORY');

-- Business: full product minus multi-store premium extras
INSERT INTO plan_module_grants (plan_code, module_code) VALUES
    ('BUSINESS', 'CORE'),
    ('BUSINESS', 'INVENTORY'),
    ('BUSINESS', 'CUSTOMERS'),
    ('BUSINESS', 'FISCAL'),
    ('BUSINESS', 'TEAM'),
    ('BUSINESS', 'AUDIT'),
    ('BUSINESS', 'ANALYTICS');

-- Scale: everything including multi-store
INSERT INTO plan_module_grants (plan_code, module_code) VALUES
    ('SCALE', 'CORE'),
    ('SCALE', 'INVENTORY'),
    ('SCALE', 'CUSTOMERS'),
    ('SCALE', 'FISCAL'),
    ('SCALE', 'TEAM'),
    ('SCALE', 'MULTI_STORE'),
    ('SCALE', 'AUDIT'),
    ('SCALE', 'ANALYTICS');

-- Internal: all modules for testers / operator grants
INSERT INTO plan_module_grants (plan_code, module_code) VALUES
    ('INTERNAL', 'CORE'),
    ('INTERNAL', 'INVENTORY'),
    ('INTERNAL', 'CUSTOMERS'),
    ('INTERNAL', 'FISCAL'),
    ('INTERNAL', 'TEAM'),
    ('INTERNAL', 'MULTI_STORE'),
    ('INTERNAL', 'AUDIT'),
    ('INTERNAL', 'ANALYTICS');

-- -----------------------------------------------------------------------------
-- 3. Purchasable / grantable module add-ons (catalog for future billing)
-- -----------------------------------------------------------------------------
CREATE TABLE module_catalog (
    code                VARCHAR(50) PRIMARY KEY,
    display_name        VARCHAR(120) NOT NULL,
    description         TEXT,
    is_public           BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order          INT NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO module_catalog (code, display_name, description, sort_order) VALUES
    ('CORE', 'Core POS', 'Sales, cash register and dashboard.', 10),
    ('INVENTORY', 'Inventory', 'Products, categories, stock and suppliers.', 20),
    ('CUSTOMERS', 'Customers', 'Customer directory and CRM basics.', 30),
    ('FISCAL', 'E-Invoicing', 'AFIP electronic invoicing via TusFacturas.', 40),
    ('TEAM', 'Team', 'Invites, roles and organization members.', 50),
    ('MULTI_STORE', 'Multi-store', 'More than one branch per organization.', 60),
    ('AUDIT', 'Audit', 'Audit log and compliance trail.', 70),
    ('ANALYTICS', 'Analytics', 'Advanced dashboard metrics.', 80);

-- Optional add-on modules attached to an organization (MP billing, manual, promo)
CREATE TABLE organization_module_addons (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     BIGINT NOT NULL REFERENCES organizations (id),
    module_code         VARCHAR(50) NOT NULL REFERENCES module_catalog (code),
    source_type         VARCHAR(50) NOT NULL,
    source_reference    VARCHAR(255),
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    valid_from          TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at          TIMESTAMP,
    UNIQUE (organization_id, module_code, source_type, source_reference)
);

CREATE INDEX idx_org_module_addons_org_status
    ON organization_module_addons (organization_id, status);

-- -----------------------------------------------------------------------------
-- 4. Promo / gift codes (platform operator only — not a public org module)
-- -----------------------------------------------------------------------------
CREATE TABLE promo_codes (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(64) NOT NULL,
    code_normalized     VARCHAR(64) NOT NULL,
    promo_type          VARCHAR(50) NOT NULL,
    grant_plan_code     VARCHAR(50) REFERENCES subscription_plans (code),
    grant_module_code   VARCHAR(50) REFERENCES module_catalog (code),
    duration_days       INT,
    max_redemptions     INT,
    redemption_count    INT NOT NULL DEFAULT 0,
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    valid_from          TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMP,
    internal_note       TEXT,
    created_by_user_id  BIGINT REFERENCES users (id),
    revoked_at          TIMESTAMP,
    revoked_by_user_id  BIGINT REFERENCES users (id),
    revoke_reason       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_promo_codes_code_normalized UNIQUE (code_normalized)
);

CREATE INDEX idx_promo_codes_status ON promo_codes (status);

CREATE TABLE promo_code_redemptions (
    id                      BIGSERIAL PRIMARY KEY,
    promo_code_id           BIGINT NOT NULL REFERENCES promo_codes (id),
    organization_id         BIGINT NOT NULL REFERENCES organizations (id),
    redeemed_by_user_id     BIGINT NOT NULL REFERENCES users (id),
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    granted_plan_code       VARCHAR(50) REFERENCES subscription_plans (code),
    granted_module_code     VARCHAR(50) REFERENCES module_catalog (code),
    access_valid_from       TIMESTAMP NOT NULL DEFAULT NOW(),
    access_valid_until      TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at              TIMESTAMP,
    revoked_by_user_id      BIGINT REFERENCES users (id),
    revoke_reason           TEXT
);

CREATE INDEX idx_promo_redemptions_org_status
    ON promo_code_redemptions (organization_id, status);

CREATE INDEX idx_promo_redemptions_code_status
    ON promo_code_redemptions (promo_code_id, status);

-- -----------------------------------------------------------------------------
-- 5. Platform operators (developer / system admins — never org-level)
-- -----------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN platform_operator BOOLEAN NOT NULL DEFAULT FALSE;

-- -----------------------------------------------------------------------------
-- 6. Migrate existing subscriptions: STARTER -> BUSINESS (grandfather testers)
-- -----------------------------------------------------------------------------
UPDATE organization_subscriptions
SET plan_code = 'BUSINESS',
    status = 'ACTIVE',
    updated_at = NOW()
WHERE plan_code = 'STARTER';

-- New signups will default to POS in application code (SubscriptionService).
