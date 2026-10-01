-- next_run_at must be nullable: deleted and failed scheduled reports clear it
-- (and a PAUSED report has no deadline until reactivated). The NOT NULL from
-- V116 turned every DELETE into a 409 constraint violation.
ALTER TABLE scheduled_reports ALTER COLUMN next_run_at DROP NOT NULL;
