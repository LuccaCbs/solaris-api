ALTER TABLE organizations
    ADD COLUMN fiscal_provider VARCHAR(50) NOT NULL DEFAULT 'MOCK',
    ADD COLUMN fiscal_api_key VARCHAR(512),
    ADD COLUMN fiscal_punto_venta INTEGER;

CREATE TABLE fiscal_documents (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     BIGINT NOT NULL REFERENCES organizations (id),
    store_id            BIGINT REFERENCES stores (id),
    sale_id             BIGINT UNIQUE REFERENCES sales (id),
    customer_id         BIGINT REFERENCES customers (id),
    tipo_comprobante    VARCHAR(50) NOT NULL,
    punto_venta         INTEGER NOT NULL,
    numero_comprobante  BIGINT NOT NULL,
    cae                 VARCHAR(14),
    cae_vencimiento     DATE,
    importe_neto        NUMERIC NOT NULL,
    importe_iva         NUMERIC NOT NULL,
    importe_total       NUMERIC NOT NULL,
    status              VARCHAR(50) NOT NULL,
    afip_raw_json       TEXT,
    pdf_url             VARCHAR(1024),
    created_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_fiscal_documents_organization_id ON fiscal_documents (organization_id);

CREATE UNIQUE INDEX uk_fiscal_documents_org_pv_tipo_numero
    ON fiscal_documents (organization_id, punto_venta, tipo_comprobante, numero_comprobante);
