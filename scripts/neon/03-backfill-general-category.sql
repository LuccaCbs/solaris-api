-- Backfill org-scoped "General" category for active organizations missing it.
-- Safe to run multiple times.

INSERT INTO categories (
    name,
    description,
    created_at,
    system_category,
    user_id,
    organization_id,
    created_by_user_id
)
SELECT
    'General',
    'Default category',
    NOW(),
    TRUE,
    owner_member.user_id,
    owner_member.organization_id,
    owner_member.user_id
FROM (
    SELECT DISTINCT ON (om.organization_id)
        om.organization_id,
        om.user_id
    FROM organization_members om
    WHERE om.status = 'ACTIVE'
    ORDER BY om.organization_id,
             CASE WHEN om.role = 'OWNER' THEN 0 ELSE 1 END,
             om.id
) AS owner_member
JOIN organization_subscriptions os
    ON os.organization_id = owner_member.organization_id
WHERE os.status IN ('ACTIVE', 'TRIALING')
  AND NOT EXISTS (
      SELECT 1
      FROM categories c
      WHERE c.organization_id = owner_member.organization_id
        AND LOWER(c.name) = 'general'
  );
