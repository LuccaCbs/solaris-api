-- =============================================================================
-- Create / refresh freemium promo code SOLARISTEST1
-- Type: GRANT_PLAN → activates POS (Freemium) at $0 via billing portal
-- Max redemptions: 10
-- Database: shared Neon
-- =============================================================================

-- Preview existing code
SELECT id, code, promo_type, grant_plan_code, max_redemptions, redemption_count, status
FROM promo_codes
WHERE code_normalized = 'SOLARISTEST1';

BEGIN;

INSERT INTO promo_codes (
    code,
    code_normalized,
    promo_type,
    grant_plan_code,
    grant_module_code,
    duration_days,
    max_redemptions,
    redemption_count,
    status,
    valid_from,
    valid_until,
    internal_note,
    created_at,
    updated_at
)
VALUES (
    'SOLARISTEST1',
    'SOLARISTEST1',
    'GRANT_PLAN',
    'POS',
    NULL,
    NULL,
    10,
    0,
    'ACTIVE',
    NOW(),
    NULL,
    'Freemium test code — 10 redemptions (SOLARISTEST1)',
    NOW(),
    NOW()
)
ON CONFLICT (code_normalized) DO UPDATE SET
    promo_type        = EXCLUDED.promo_type,
    grant_plan_code   = EXCLUDED.grant_plan_code,
    grant_module_code = EXCLUDED.grant_module_code,
    duration_days     = EXCLUDED.duration_days,
    max_redemptions   = EXCLUDED.max_redemptions,
    status            = 'ACTIVE',
    valid_from        = LEAST(promo_codes.valid_from, NOW()),
    valid_until       = NULL,
    revoked_at        = NULL,
    revoked_by_user_id = NULL,
    revoke_reason     = NULL,
    internal_note     = EXCLUDED.internal_note,
    updated_at        = NOW();

SELECT id,
       code,
       promo_type,
       grant_plan_code,
       max_redemptions,
       redemption_count,
       status,
       internal_note
FROM promo_codes
WHERE code_normalized = 'SOLARISTEST1';

-- COMMIT;
ROLLBACK;

-- =============================================================================
-- Optional: reset redemption counter (keeps code, clears usage history)
-- Uncomment if you need a clean slate for the same organizations
-- =============================================================================
/*
BEGIN;
UPDATE promo_codes SET redemption_count = 0, status = 'ACTIVE', updated_at = NOW()
WHERE code_normalized = 'SOLARISTEST1';

DELETE FROM promo_code_redemptions pcr
USING promo_codes pc
WHERE pcr.promo_code_id = pc.id AND pc.code_normalized = 'SOLARISTEST1';

-- COMMIT;
ROLLBACK;
*/
