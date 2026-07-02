CREATE TABLE organization_invites (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     BIGINT NOT NULL REFERENCES organizations (id),
    email               VARCHAR(255) NOT NULL,
    role                VARCHAR(255) NOT NULL,
    store_id            BIGINT REFERENCES stores (id),
    token               VARCHAR(255) NOT NULL UNIQUE,
    status              VARCHAR(255) NOT NULL,
    invited_by_user_id  BIGINT NOT NULL REFERENCES users (id),
    expires_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    accepted_at         TIMESTAMP
);

CREATE INDEX idx_organization_invites_org_status ON organization_invites (organization_id, status);
CREATE INDEX idx_organization_invites_email ON organization_invites (email);
