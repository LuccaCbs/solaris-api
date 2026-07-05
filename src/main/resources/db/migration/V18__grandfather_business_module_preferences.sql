-- Grandfather optional Business-tier modules for organizations already on paid plans.
INSERT INTO organization_module_addons (
    organization_id,
    module_code,
    source_type,
    source_reference,
    status,
    valid_from,
    created_at
)
SELECT
    os.organization_id,
    module_code,
    'SYSTEM',
    'grandfather-business-modules',
    'ACTIVE',
    NOW(),
    NOW()
FROM organization_subscriptions os
CROSS JOIN (
    VALUES
        ('TEAM'),
        ('FISCAL'),
        ('AUDIT'),
        ('CUSTOMERS'),
        ('ANALYTICS')
) AS optional_modules(module_code)
WHERE os.status IN ('ACTIVE', 'TRIALING')
  AND os.plan_code IN ('BUSINESS', 'SCALE', 'INTERNAL')
  AND NOT EXISTS (
      SELECT 1
      FROM organization_module_addons existing
      WHERE existing.organization_id = os.organization_id
        AND existing.module_code = optional_modules.module_code
        AND existing.status = 'ACTIVE'
  );
