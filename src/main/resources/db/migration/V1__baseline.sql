CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    firstname       VARCHAR(255) NOT NULL,
    lastname        VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(255),
    email_verified  BOOLEAN NOT NULL
);

CREATE TABLE categories (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(500),
    created_at       TIMESTAMP NOT NULL,
    system_category  BOOLEAN NOT NULL,
    user_id          BIGINT NOT NULL REFERENCES users (id)
);

CREATE TABLE products (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    description          VARCHAR(1000),
    sku                  VARCHAR(255) NOT NULL UNIQUE,
    price                NUMERIC NOT NULL,
    stock_quantity       INTEGER NOT NULL,
    low_stock_threshold  INTEGER,
    created_at           TIMESTAMP NOT NULL,
    user_id              BIGINT NOT NULL REFERENCES users (id),
    category_id          BIGINT REFERENCES categories (id),
    active               BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE suppliers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    email       VARCHAR(255),
    phone       VARCHAR(255),
    address     VARCHAR(255),
    notes       VARCHAR(1000),
    active      BOOLEAN NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    user_id     BIGINT NOT NULL REFERENCES users (id)
);

CREATE TABLE cash_register_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    opened_at           TIMESTAMP NOT NULL,
    closed_at           TIMESTAMP,
    opened_by           VARCHAR(255) NOT NULL,
    closed_by           VARCHAR(255),
    status              VARCHAR(255) NOT NULL,
    reopen_count        INTEGER NOT NULL,
    closing_amount      NUMERIC,
    cash_count          INTEGER NOT NULL,
    cash_amount         NUMERIC NOT NULL,
    credit_card_count   INTEGER NOT NULL,
    credit_card_amount  NUMERIC NOT NULL,
    debit_card_count    INTEGER NOT NULL,
    debit_card_amount   NUMERIC NOT NULL,
    transfer_count      INTEGER NOT NULL,
    transfer_amount     NUMERIC NOT NULL,
    other_count         INTEGER NOT NULL,
    other_amount        NUMERIC NOT NULL,
    user_id             BIGINT NOT NULL REFERENCES users (id)
);

CREATE TABLE sales (
    id                        BIGSERIAL PRIMARY KEY,
    payment_method            VARCHAR(255) NOT NULL,
    total_amount              NUMERIC NOT NULL,
    created_at                TIMESTAMP NOT NULL,
    user_id                   BIGINT NOT NULL REFERENCES users (id),
    cash_register_session_id  BIGINT NOT NULL REFERENCES cash_register_sessions (id)
);

CREATE TABLE sale_items (
    id          BIGSERIAL PRIMARY KEY,
    sale_id     BIGINT NOT NULL REFERENCES sales (id),
    product_id  BIGINT REFERENCES products (id),
    quantity    INTEGER NOT NULL,
    type        VARCHAR(20) NOT NULL,
    custom_name VARCHAR(180),
    unit_label  VARCHAR(30),
    unit_price  NUMERIC NOT NULL,
    subtotal    NUMERIC NOT NULL
);

CREATE TABLE stock_movements (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products (id),
    type            VARCHAR(255) NOT NULL,
    quantity        INTEGER NOT NULL,
    previous_stock  INTEGER NOT NULL,
    current_stock   INTEGER NOT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users (id)
);

CREATE TABLE supplier_orders (
    id               BIGSERIAL PRIMARY KEY,
    supplier_id      BIGINT NOT NULL REFERENCES suppliers (id),
    user_id          BIGINT NOT NULL REFERENCES users (id),
    status           VARCHAR(255) NOT NULL,
    message_preview  VARCHAR(2000),
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE TABLE supplier_order_items (
    id                BIGSERIAL PRIMARY KEY,
    supplier_order_id BIGINT NOT NULL REFERENCES supplier_orders (id),
    product_id        BIGINT NOT NULL REFERENCES products (id),
    quantity          INTEGER NOT NULL
);

CREATE TABLE system_settings (
    id                           BIGSERIAL PRIMARY KEY,
    global_low_stock_threshold   INTEGER NOT NULL,
    admin_access_password_hash   VARCHAR(255),
    updated_at                   TIMESTAMP NOT NULL,
    business_timezone            VARCHAR(255) NOT NULL,
    cash_register_auto_close_time TIME NOT NULL,
    whatsapp_enabled             BOOLEAN NOT NULL,
    user_id                      BIGINT NOT NULL REFERENCES users (id)
);

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    action       VARCHAR(60) NOT NULL,
    entity_type  VARCHAR(60) NOT NULL,
    entity_id    BIGINT,
    description  VARCHAR(500) NOT NULL,
    user_id      BIGINT,
    user_email   VARCHAR(150),
    user_name    VARCHAR(180),
    entity_name  VARCHAR(180),
    created_at   TIMESTAMP NOT NULL
);

CREATE TABLE email_verification_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users (id),
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE cash_register_reopen_logs (
    id                        BIGSERIAL PRIMARY KEY,
    cash_register_session_id  BIGINT NOT NULL REFERENCES cash_register_sessions (id),
    reopened_by               VARCHAR(255) NOT NULL,
    reopened_at               TIMESTAMP NOT NULL
);
