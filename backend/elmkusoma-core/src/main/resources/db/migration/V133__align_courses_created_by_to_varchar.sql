-- V133: Align courses.created_by with the JPA mapping (uuid -> varchar).
--
-- BaseEntity.createdBy is a String and AuditListener @PrePersist fills it with
-- authentication.getName() (an email/username), exactly like updated_by. Every
-- other audited table stores created_by as varchar; courses was the only table
-- still declared uuid with a FK to users(id), so every authenticated course
-- insert aborted with
--   ERROR: column "created_by" is of type uuid but expression is of type
--          character varying
-- which the global handler surfaced as a bare HTTP 500
-- ("An unexpected error occurred") when saving a course from
-- /dashboard/admin/courses. Reading was fine, so the defect only appeared on
-- write (create and any update that rewrites the row).
--
-- The FK cannot survive the type change: the column holds usernames/emails, not
-- user ids (CourseMapper maps it to CourseResponse.createdByName and the rest of
-- the codebase resolves it with userRepository.findByEmailAndIsDeletedFalse).
-- Same rationale as the other varchar created_by columns, so it is dropped
-- together with the ALTER. Existing rows are preserved (uuid -> text cast).

ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS courses_created_by_fkey;

ALTER TABLE courses
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::text;