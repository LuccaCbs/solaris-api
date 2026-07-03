ALTER TABLE cash_register_sessions
    ADD COLUMN store_id BIGINT REFERENCES stores (id);

UPDATE cash_register_sessions crs
SET store_id = om.store_id
FROM organization_members om
WHERE crs.organization_id IS NOT NULL
  AND crs.store_id IS NULL
  AND om.user_id = COALESCE(crs.created_by_user_id, crs.user_id)
  AND om.organization_id = crs.organization_id
  AND om.status = 'ACTIVE'
  AND om.store_id IS NOT NULL;

UPDATE cash_register_sessions crs
SET store_id = (
    SELECT s.id
    FROM stores s
    WHERE s.organization_id = crs.organization_id
      AND s.active = true
    ORDER BY s.id
    LIMIT 1
)
WHERE crs.organization_id IS NOT NULL
  AND crs.store_id IS NULL;

CREATE INDEX idx_cash_register_sessions_org_store
    ON cash_register_sessions (organization_id, store_id);

CREATE INDEX idx_cash_register_sessions_store_status
    ON cash_register_sessions (store_id, status)
    WHERE store_id IS NOT NULL;
