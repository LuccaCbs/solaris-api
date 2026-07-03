CREATE TABLE organization_subscriptions (
    id                          BIGSERIAL PRIMARY KEY,
    organization_id             BIGINT NOT NULL UNIQUE REFERENCES organizations (id),
    plan_code                   VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    status                      VARCHAR(50) NOT NULL DEFAULT 'TRIALING',
    max_stores                  INT NOT NULL DEFAULT 1,
    extra_stores_purchased      INT NOT NULL DEFAULT 0,
    billing_provider            VARCHAR(50) NOT NULL DEFAULT 'NONE',
    external_customer_id        VARCHAR(255),
    external_subscription_id    VARCHAR(255),
    trial_ends_at               TIMESTAMP,
    current_period_start        TIMESTAMP,
    current_period_end          TIMESTAMP,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO organization_subscriptions (
    organization_id,
    plan_code,
    status,
    max_stores,
    extra_stores_purchased,
    billing_provider,
    created_at,
    updated_at
)
SELECT
    id,
    'STARTER',
    'ACTIVE',
    1,
    0,
    'NONE',
    NOW(),
    NOW()
FROM organizations;
