-- =============================================================================
-- Reset test user (e.g. CAJERO) to retry onboarding / new organization
-- Database: shared Neon (solaris-api + solaris-billing-api)
--
-- Neon SQL Editor:
--   1. Replace 'REPLACE_WITH_CAJERO_EMAIL' in all occurrences below
--   2. Run PREVIEW
--   3. Run MODE A (or MODE B) — change ROLLBACK to COMMIT when satisfied
-- =============================================================================

-- ---------------------------------------------------------------------------
-- PREVIEW
-- ---------------------------------------------------------------------------
SELECT id, email, firstname, lastname, role, email_verified
FROM users
WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL');

SELECT om.id AS membership_id,
       om.role,
       om.status,
       o.id AS organization_id,
       COALESCE(o.display_name, o.razon_social) AS organization_name
FROM organization_members om
JOIN users u ON u.id = om.user_id
JOIN organizations o ON o.id = om.organization_id
WHERE lower(u.email) = lower('REPLACE_WITH_CAJERO_EMAIL');

SELECT id, email, role, status, expires_at
FROM organization_invites
WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL');

-- ---------------------------------------------------------------------------
-- MODE A — MEMBERSHIP_ONLY (recommended)
-- Keeps user + password + email_verified.
-- Login → /onboarding/setup → new org as OWNER.
-- Does not touch org id=1 (Solaris Store) or other members.
-- ---------------------------------------------------------------------------
BEGIN;

DELETE FROM billing_portal_payment_intents
WHERE session_id IN (
    SELECT bps.id
    FROM billing_portal_sessions bps
    JOIN users u ON u.id = bps.user_id
    WHERE lower(u.email) = lower('REPLACE_WITH_CAJERO_EMAIL')
);

DELETE FROM billing_portal_sessions
WHERE user_id IN (
    SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL')
);

DELETE FROM billing_portal_email_challenges
WHERE lower(email_normalized) = lower('REPLACE_WITH_CAJERO_EMAIL');

DELETE FROM organization_invites
WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL');

DELETE FROM promo_code_redemptions
WHERE redeemed_by_user_id IN (
    SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL')
);

DELETE FROM organization_members
WHERE user_id IN (
    SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL')
);

SELECT u.id,
       u.email,
       u.email_verified,
       (SELECT count(*) FROM organization_members om WHERE om.user_id = u.id) AS memberships
FROM users u
WHERE lower(u.email) = lower('REPLACE_WITH_CAJERO_EMAIL');

ROLLBACK;
-- COMMIT;

-- ---------------------------------------------------------------------------
-- MODE B — FULL_USER (email free for new registration)
-- Uncomment block below if MODE A is not enough.
-- ---------------------------------------------------------------------------
/*
BEGIN;

DELETE FROM billing_portal_payment_intents
WHERE session_id IN (
    SELECT bps.id FROM billing_portal_sessions bps
    JOIN users u ON u.id = bps.user_id
    WHERE lower(u.email) = lower('REPLACE_WITH_CAJERO_EMAIL')
);

DELETE FROM billing_portal_sessions
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'));

DELETE FROM billing_portal_email_challenges
WHERE lower(email_normalized) = lower('REPLACE_WITH_CAJERO_EMAIL');

DELETE FROM organization_invites
WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL');

DELETE FROM promo_code_redemptions
WHERE redeemed_by_user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'))
   OR revoked_by_user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'));

DELETE FROM organization_members
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'));

UPDATE promo_codes
SET created_by_user_id = NULL, revoked_by_user_id = NULL
WHERE created_by_user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'))
   OR revoked_by_user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'));

DELETE FROM email_verification_tokens
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'));

DELETE FROM password_reset_tokens
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'));

DELETE FROM categories
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL'))
  AND id NOT IN (SELECT DISTINCT category_id FROM products WHERE category_id IS NOT NULL);

DELETE FROM users
WHERE lower(email) = lower('REPLACE_WITH_CAJERO_EMAIL');

ROLLBACK;
-- COMMIT;
*/
