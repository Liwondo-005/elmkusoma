-- V112: Learning Offerings (additive, production-safe)
-- New minimal aggregate for teacher-owned learning opportunities that learners can discover.
-- References existing content optionally (course_id) and never alters existing tables.
-- institution_id is intentionally NULLABLE so independent (non-institution) teachers can publish.

CREATE TABLE IF NOT EXISTS learning_offerings (
    id              UUID PRIMARY KEY,
    institution_id  UUID,
    owner_user_id   UUID NOT NULL,
    teacher_id      UUID,
    course_id       UUID,
    subject_id      UUID,
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    thumbnail_url   VARCHAR(500),
    education_level VARCHAR(50),
    visibility      VARCHAR(20) NOT NULL DEFAULT 'INSTITUTION',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_learning_offerings_owner
    ON learning_offerings (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_learning_offerings_institution
    ON learning_offerings (institution_id);
CREATE INDEX IF NOT EXISTS idx_learning_offerings_discover
    ON learning_offerings (status, visibility, education_level);
CREATE INDEX IF NOT EXISTS idx_learning_offerings_subject
    ON learning_offerings (subject_id);
CREATE INDEX IF NOT EXISTS idx_learning_offerings_course
    ON learning_offerings (course_id);

-- rollback: DROP TABLE IF EXISTS learning_offerings;
