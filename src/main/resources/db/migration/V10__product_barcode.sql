ALTER TABLE products RENAME COLUMN sku TO barcode;

ALTER TABLE products
    ADD COLUMN barcode_format VARCHAR(32) NOT NULL DEFAULT 'CODE_128';

ALTER TABLE products RENAME CONSTRAINT uk_products_organization_sku TO uk_products_organization_barcode;
