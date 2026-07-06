CREATE TABLE customer_documents (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    document_type   VARCHAR(255) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    organization_id BIGINT REFERENCES organizations (id),
    user_id         BIGINT NOT NULL REFERENCES users (id),
    created_at      TIMESTAMP NOT NULL
);

INSERT INTO customer_documents (
    customer_id,
    document_type,
    document_number,
    is_primary,
    organization_id,
    user_id,
    created_at
)
SELECT
    id,
    document_type,
    document_number,
    TRUE,
    organization_id,
    user_id,
    created_at
FROM customers;

CREATE INDEX idx_customer_documents_customer_id ON customer_documents (customer_id);
CREATE INDEX idx_customer_documents_document_number ON customer_documents (document_number);

CREATE UNIQUE INDEX uk_customer_documents_customer_type_number
    ON customer_documents (customer_id, document_type, document_number);

CREATE UNIQUE INDEX uk_customer_documents_organization_document
    ON customer_documents (organization_id, document_type, document_number)
    WHERE organization_id IS NOT NULL;

CREATE UNIQUE INDEX uk_customer_documents_user_document
    ON customer_documents (user_id, document_type, document_number)
    WHERE organization_id IS NULL;

DROP INDEX IF EXISTS uk_customers_organization_document;
DROP INDEX IF EXISTS uk_customers_user_document;
