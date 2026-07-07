ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(255) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE users
    ADD COLUMN google_sub VARCHAR(255);

ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;

CREATE UNIQUE INDEX uk_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;
