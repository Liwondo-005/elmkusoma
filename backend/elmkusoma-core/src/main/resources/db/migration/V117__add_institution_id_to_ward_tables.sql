-- BaseEntity maps an institution_id column onto every entity; the ward tables
-- created in V116 were missing it, which broke every SELECT (column does not
-- exist). Mirror the existing pattern from V97 (video_tutorial_progress).
ALTER TABLE wards ADD COLUMN institution_id UUID;
ALTER TABLE scheduled_reports ADD COLUMN institution_id UUID;
ALTER TABLE scheduled_report_runs ADD COLUMN institution_id UUID;
