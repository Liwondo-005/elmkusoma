-- V25: Fix jsonb column type mismatches between entities and migrations
-- Some columns were created as TEXT but entities declare them as jsonb
-- Some columns were missing entirely from migrations
-- Uses DO blocks to be idempotent (safe to run even if columns already correct)

-- 1. Fix system_settings.setting_value: TEXT -> JSONB
DO $$
BEGIN
    ALTER TABLE system_settings ALTER COLUMN setting_value SET DEFAULT '{}'::jsonb;
    UPDATE system_settings SET setting_value = '{}' WHERE setting_value IS NULL OR setting_value = '';
    ALTER TABLE system_settings ALTER COLUMN setting_value SET NOT NULL;
    ALTER TABLE system_settings ALTER COLUMN setting_value TYPE jsonb USING setting_value::jsonb;
EXCEPTION
    WHEN undefined_column OR duplicate_column THEN NULL;
END $$;

-- 2. Fix data_import_jobs.error_log: TEXT -> JSONB (nullable)
DO $$
BEGIN
    ALTER TABLE data_import_jobs ALTER COLUMN error_log TYPE jsonb USING COALESCE(error_log, 'null')::jsonb;
EXCEPTION
    WHEN undefined_column THEN NULL;
END $$;

-- 3. Add dashboard_snapshots.snapshot_data (missing from V17)
ALTER TABLE dashboard_snapshots ADD COLUMN IF NOT EXISTS snapshot_data jsonb DEFAULT '{}'::jsonb;

-- 4. Add certificates.skills (missing from V16)
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS skills jsonb DEFAULT '[]'::jsonb;

-- 5. Add certificates.metadata (missing from V16)
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS metadata jsonb DEFAULT '{}'::jsonb;

-- 6. Add transcripts.metadata (missing from V16)
ALTER TABLE transcripts ADD COLUMN IF NOT EXISTS metadata jsonb DEFAULT '{}'::jsonb;
