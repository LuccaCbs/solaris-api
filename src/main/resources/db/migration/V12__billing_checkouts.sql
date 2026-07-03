CREATE TABLE billing_checkouts (
    id                      BIGSERIAL PRIMARY KEY,
    organization_id         BIGINT NOT NULL REFERENCES organizations (id),
    checkout_type           VARCHAR(50) NOT NULL DEFAULT 'STORE_ADDON',
    quantity                INT NOT NULL DEFAULT 1,
    unit_amount             NUMERIC(12, 2) NOT NULL,
    total_amount            NUMERIC(12, 2) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'ARS',
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    provider                VARCHAR(50) NOT NULL DEFAULT 'MERCADOPAGO',
    external_preference_id  VARCHAR(255),
    external_payment_id     VARCHAR(255),
    fulfilled_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_checkouts_organization_id ON billing_checkouts (organization_id);

CREATE UNIQUE INDEX idx_billing_checkouts_external_preference_id
    ON billing_checkouts (external_preference_id)
    WHERE external_preference_id IS NOT NULL;

CREATE UNIQUE INDEX idx_billing_checkouts_external_payment_id
    ON billing_checkouts (external_payment_id)
    WHERE external_payment_id IS NOT NULL;
