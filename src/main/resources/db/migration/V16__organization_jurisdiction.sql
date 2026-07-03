-- =============================================================================
-- V16: Organization jurisdiction for multi-country billing and fiscal routing
-- =============================================================================

ALTER TABLE organizations
    ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'AR',
    ADD COLUMN billing_jurisdiction VARCHAR(10) NOT NULL DEFAULT 'AR',
    ADD COLUMN fiscal_jurisdiction VARCHAR(30) NOT NULL DEFAULT 'AR_AFIP',
    ADD COLUMN default_currency VARCHAR(3) NOT NULL DEFAULT 'ARS';

CREATE INDEX idx_organizations_country_code ON organizations (country_code);
