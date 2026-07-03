ALTER TABLE organizations
    ADD COLUMN display_name VARCHAR(255);

UPDATE organizations
SET display_name = razon_social
WHERE display_name IS NULL;
