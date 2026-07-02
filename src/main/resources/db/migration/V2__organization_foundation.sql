CREATE TABLE organizations (
    id             BIGSERIAL PRIMARY KEY,
    cuit           VARCHAR(13),
    razon_social   VARCHAR(255) NOT NULL,
    condicion_iva  VARCHAR(255) NOT NULL,
    timezone       VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP NOT NULL
);

CREATE TABLE stores (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL REFERENCES organizations (id),
    name              VARCHAR(255) NOT NULL,
    address           VARCHAR(255),
    afip_punto_venta  INTEGER,
    active            BOOLEAN NOT NULL
);

CREATE TABLE organization_members (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users (id),
    organization_id  BIGINT NOT NULL REFERENCES organizations (id),
    role             VARCHAR(255) NOT NULL,
    store_id         BIGINT REFERENCES stores (id),
    status           VARCHAR(255) NOT NULL,
    CONSTRAINT uk_organization_members_user_org UNIQUE (user_id, organization_id)
);

ALTER TABLE products
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE categories
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE suppliers
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE sales
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE stock_movements
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE supplier_orders
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE system_settings
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id);

ALTER TABLE cash_register_sessions
    ADD COLUMN organization_id BIGINT REFERENCES organizations (id),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id);

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_sku_key;
ALTER TABLE products ADD CONSTRAINT uk_products_organization_sku UNIQUE (organization_id, sku);
