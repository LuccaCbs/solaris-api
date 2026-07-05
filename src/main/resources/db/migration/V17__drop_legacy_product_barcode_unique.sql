-- Remove legacy single-column unique constraints on product barcode/sku if they still exist.
-- Barcodes are unique per organization via uk_products_organization_barcode.

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_sku_key;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_barcode_key;
