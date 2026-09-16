-- V42: Create live_class_participants table for real-time session tracking
-- Uses IF NOT EXISTS to handle cases where Hibernate ddl-auto already created the table

CREATE TABLE IF NOT EXISTS live_class_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID,
    live_class_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'LEARNER',
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    duration_seconds BIGINT,
    connection_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_lc_participant_class FOREIGN KEY (live_class_id) REFERENCES live_classes(id),
    CONSTRAINT fk_lc_participant_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_live_class_participant UNIQUE (live_class_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_lc_participant_class ON live_class_participants(live_class_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_lc_participant_user ON live_class_participants(user_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_lc_participant_connection ON live_class_participants(connection_id) WHERE is_deleted = false;
