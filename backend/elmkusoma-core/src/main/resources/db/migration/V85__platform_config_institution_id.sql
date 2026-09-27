-- V84: platform_config is mapped by PlatformConfigEntry extends BaseEntity,
-- which selects institution_id. V63 created platform_config without that
-- column, so every platform_config query (registration policy, scheduled
-- integrity jobs) fails with SQLState 42703.
ALTER TABLE platform_config ADD COLUMN IF NOT EXISTS institution_id UUID;

CREATE INDEX IF NOT EXISTS idx_platform_config_institution_id ON platform_config(institution_id);
