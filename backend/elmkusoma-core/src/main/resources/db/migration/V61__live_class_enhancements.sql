ALTER TABLE live_classes
  ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'Africa/Dar_es_Salaam',
  ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS recurrence_pattern VARCHAR(50),
  ADD COLUMN IF NOT EXISTS recurrence_end_date DATE,
  ADD COLUMN IF NOT EXISTS parent_recurring_id UUID,
  ADD COLUMN IF NOT EXISTS lobby_enabled BOOLEAN DEFAULT FALSE;

ALTER TABLE live_class_participants
  ADD COLUMN IF NOT EXISTS hand_raised_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS hand_raise_order INTEGER;

CREATE TABLE IF NOT EXISTS live_class_breakout_rooms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  live_class_id UUID NOT NULL,
  name VARCHAR(200) NOT NULL,
  max_participants INTEGER DEFAULT 10,
  status VARCHAR(20) DEFAULT 'WAITING',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_breakout_assignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  breakout_room_id UUID NOT NULL,
  user_id UUID NOT NULL,
  assigned_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_quizzes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  live_class_id UUID NOT NULL,
  created_by UUID NOT NULL,
  title VARCHAR(300) NOT NULL,
  status VARCHAR(20) DEFAULT 'DRAFT',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_quiz_questions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quiz_id UUID NOT NULL,
  question_text TEXT NOT NULL,
  question_type VARCHAR(20) DEFAULT 'MULTIPLE_CHOICE',
  options JSONB,
  correct_answer TEXT,
  display_order INTEGER DEFAULT 0,
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_quiz_responses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quiz_id UUID NOT NULL,
  question_id UUID NOT NULL,
  user_id UUID NOT NULL,
  answer_text TEXT,
  is_correct BOOLEAN,
  responded_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_polls (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  live_class_id UUID NOT NULL,
  created_by UUID NOT NULL,
  question TEXT NOT NULL,
  options JSONB NOT NULL,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT NOW(),
  closed_at TIMESTAMP,
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_poll_votes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  poll_id UUID NOT NULL,
  user_id UUID NOT NULL,
  option_index INTEGER NOT NULL,
  voted_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_shared_media (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  live_class_id UUID NOT NULL,
  shared_by UUID NOT NULL,
  media_type VARCHAR(50) NOT NULL,
  title VARCHAR(300),
  url VARCHAR(1000) NOT NULL,
  duration_seconds INTEGER,
  shared_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_attendance_detail (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  live_class_id UUID NOT NULL,
  user_id UUID NOT NULL,
  joined_at TIMESTAMP,
  left_at TIMESTAMP,
  total_seconds INTEGER DEFAULT 0,
  percentage DECIMAL(5,2) DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS live_class_hand_raise_queue (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  live_class_id UUID NOT NULL,
  user_id UUID NOT NULL,
  raised_at TIMESTAMP DEFAULT NOW(),
  lowered_at TIMESTAMP,
  position INTEGER NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  is_deleted BOOLEAN DEFAULT FALSE
);
