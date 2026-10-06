-- V126: Persist the academic year declared when creating a workshop session.
-- Additive and nullable: legacy workshop rows keep NULL and render no
-- academic-year chip; new writes may provide e.g. 2025/2026 or 2026.

ALTER TABLE workshop_sessions
    ADD COLUMN IF NOT EXISTS academic_year VARCHAR(32);
