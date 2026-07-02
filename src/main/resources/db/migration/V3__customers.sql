CREATE TABLE customers (
    id                  BIGSERIAL PRIMARY KEY,
    document_type       VARCHAR(255) NOT NULL,
    document_number     VARCHAR(20) NOT NULL,
    razon_social        VARCHAR(255) NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(255),
    address             VARCHAR(255),
    condicion_iva       VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    user_id             BIGINT NOT NULL REFERENCES users (id),
    organization_id     BIGINT REFERENCES organizations (id),
    created_by_user_id  BIGINT REFERENCES users (id)
);

CREATE INDEX idx_customers_organization_id ON customers (organization_id);
CREATE INDEX idx_customers_user_id ON customers (user_id);
