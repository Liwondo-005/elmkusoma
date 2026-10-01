-- BaseEntity audit columns carry the authenticated principal's email as text;
-- V116/V117 typed created_by/updated_by as UUID (copied from the regions and
-- districts seeds, which are never written through Hibernate), so every INSERT
-- of a ward/scheduled-report row failed with a type mismatch. Match the rest
-- of the platform's Hibernate-managed tables.
ALTER TABLE wards ALTER COLUMN created_by TYPE TEXT USING created_by::text;
ALTER TABLE wards ALTER COLUMN updated_by TYPE TEXT USING updated_by::text;
ALTER TABLE scheduled_reports ALTER COLUMN created_by TYPE TEXT USING created_by::text;
ALTER TABLE scheduled_reports ALTER COLUMN updated_by TYPE TEXT USING updated_by::text;
ALTER TABLE scheduled_report_runs ALTER COLUMN created_by TYPE TEXT USING created_by::text;
ALTER TABLE scheduled_report_runs ALTER COLUMN updated_by TYPE TEXT USING updated_by::text;
