-- Q&A workflow (mission 023 "marks answered"): additive columns on chat messages.
-- NULL answered_at = question still open; set when a teacher marks it answered.
ALTER TABLE live_class_chat_messages ADD COLUMN IF NOT EXISTS answered_at TIMESTAMP;
ALTER TABLE live_class_chat_messages ADD COLUMN IF NOT EXISTS answered_by UUID;

COMMENT ON COLUMN live_class_chat_messages.answered_at IS 'Set when a teacher marks a Q&A question as answered; NULL = open.';
COMMENT ON COLUMN live_class_chat_messages.answered_by IS 'User id of the teacher/admin who marked the question answered.';
