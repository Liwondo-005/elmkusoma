CREATE TABLE IF NOT EXISTS worker_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_id BIGINT,
    institution_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_worker_notifications_user_id ON worker_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_worker_notifications_institution_id ON worker_notifications(institution_id);
CREATE INDEX IF NOT EXISTS idx_worker_notifications_is_read ON worker_notifications(is_read);
