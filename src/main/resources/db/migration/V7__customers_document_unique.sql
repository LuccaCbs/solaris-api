CREATE UNIQUE INDEX IF NOT EXISTS uk_customers_organization_document
    ON customers (organization_id, document_type, document_number)
    WHERE organization_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customers_user_document
    ON customers (user_id, document_type, document_number)
    WHERE organization_id IS NULL;
