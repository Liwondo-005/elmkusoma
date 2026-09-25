-- V81: Align schema with Hibernate entities (generated from ddl-auto=update pre/post diff)
-- New tables: 28 | New columns on existing tables: 220 | Type/null/default changes: 338

-- ===== SECTION A: new tables =====
CREATE TABLE IF NOT EXISTS announcements (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    author_id uuid NOT NULL,
    class_group_id uuid,
    content text NOT NULL,
    priority character varying(20) NOT NULL,
    subject_id uuid,
    title character varying(300) NOT NULL
);
CREATE TABLE IF NOT EXISTS curriculum_topics (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    description text,
    education_level character varying(50) NOT NULL,
    sort_order integer,
    subject_id uuid NOT NULL,
    topic_name character varying(255) NOT NULL,
    total_lessons integer
);
CREATE TABLE IF NOT EXISTS discovery_entries (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    discovery_type character varying(30) NOT NULL,
    evidence text,
    is_resolved boolean NOT NULL,
    question text NOT NULL,
    result text,
    student_id uuid NOT NULL,
    subject_name character varying(100),
    title character varying(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS elmkusoma_labs (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    expected_result text,
    hypothesis text,
    is_attempted boolean NOT NULL,
    lab_title character varying(255) NOT NULL,
    lab_type character varying(30) NOT NULL,
    materials_list text,
    score integer,
    steps text,
    student_id uuid NOT NULL,
    student_notes text
);
CREATE TABLE IF NOT EXISTS learner_goals (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    completed_at timestamp(6) without time zone,
    description text,
    goal_type character varying(30),
    progress_percentage integer,
    status character varying(20),
    target_date date,
    title character varying(200) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT learner_goals_goal_type_check CHECK (((goal_type)::text = ANY ((ARRAY['PERSONAL'::character varying, 'ACADEMIC'::character varying, 'CAREER'::character varying, 'SKILL'::character varying, 'CERTIFICATION'::character varying, 'PROJECT'::character varying])::text[]))),
    CONSTRAINT learner_goals_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'COMPLETED'::character varying, 'PAUSED'::character varying, 'CANCELLED'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS learning_evidence (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    description text,
    evidence_type character varying(30) NOT NULL,
    evidence_url character varying(500),
    points integer NOT NULL,
    student_id uuid NOT NULL,
    subject_name character varying(100),
    title character varying(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS learning_passports (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    current_country character varying(100),
    last_activity timestamp(6) without time zone,
    stamps_earned integer NOT NULL,
    student_id uuid NOT NULL,
    total_stamps integer NOT NULL
);
CREATE TABLE IF NOT EXISTS learning_profiles (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    goals text,
    interests text,
    learning_style character varying(30) NOT NULL,
    level integer NOT NULL,
    strengths text,
    student_id uuid NOT NULL,
    total_points integer NOT NULL
);
CREATE TABLE IF NOT EXISTS live_class_activities (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    activity_type character varying(30) NOT NULL,
    correct_answer character varying(500),
    created_by_id uuid NOT NULL,
    is_published boolean NOT NULL,
    live_class_id uuid NOT NULL,
    options text,
    order_index integer NOT NULL,
    title character varying(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS live_class_responses (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    activity_type character varying(30) NOT NULL,
    live_class_id uuid NOT NULL,
    responded_at timestamp(6) without time zone NOT NULL,
    score integer,
    selected_answer character varying(500),
    student_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS mistake_lab_entries (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    correct_answer text,
    explanation text,
    is_reviewed boolean NOT NULL,
    question text NOT NULL,
    student_id uuid NOT NULL,
    subject_name character varying(100),
    wrong_answer text
);
CREATE TABLE IF NOT EXISTS nfe_assessments (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    assessment_type character varying(255) NOT NULL,
    description text,
    ends_at timestamp(6) without time zone,
    is_published boolean NOT NULL,
    pass_marks integer,
    program_id uuid,
    provider_id uuid NOT NULL,
    starts_at timestamp(6) without time zone,
    time_limit_minutes integer,
    title character varying(255) NOT NULL,
    total_marks integer,
    CONSTRAINT nfe_assessments_assessment_type_check CHECK (((assessment_type)::text = ANY ((ARRAY['QUIZ'::character varying, 'EXAM'::character varying, 'SURVEY'::character varying, 'FEEDBACK'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS nfe_attendance (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    check_in_time timestamp(6) without time zone,
    check_out_time timestamp(6) without time zone,
    learner_id uuid NOT NULL,
    marked_by uuid,
    provider_id uuid NOT NULL,
    remarks character varying(255),
    session_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT nfe_attendance_status_check CHECK (((status)::text = ANY ((ARRAY['PRESENT'::character varying, 'ABSENT'::character varying, 'LATE'::character varying, 'EXCUSED'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS nfe_certificates (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    certificate_type character varying(255) NOT NULL,
    expiry_date date,
    issued_at timestamp(6) without time zone,
    issued_by uuid,
    learner_id uuid NOT NULL,
    program_id uuid,
    provider_id uuid NOT NULL,
    serial_number character varying(255),
    status character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    verification_code character varying(255),
    CONSTRAINT nfe_certificates_certificate_type_check CHECK (((certificate_type)::text = ANY ((ARRAY['COMPLETION'::character varying, 'PARTICIPATION'::character varying, 'ACHIEVEMENT'::character varying])::text[]))),
    CONSTRAINT nfe_certificates_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ISSUED'::character varying, 'REVOKED'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS nfe_education_providers (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    address character varying(255),
    city character varying(255),
    contact_person_email character varying(255),
    contact_person_name character varying(255),
    contact_person_phone character varying(255),
    country character varying(255),
    description text,
    email character varying(255),
    is_active boolean NOT NULL,
    is_verified boolean NOT NULL,
    logo_url character varying(255),
    name character varying(255) NOT NULL,
    phone character varying(255),
    provider_type character varying(255) NOT NULL,
    website character varying(255),
    CONSTRAINT nfe_education_providers_provider_type_check CHECK (((provider_type)::text = ANY ((ARRAY['ORGANIZATION'::character varying, 'COMPANY'::character varying, 'GOVERNMENT'::character varying, 'RELIGIOUS'::character varying, 'TRAINING'::character varying, 'INDIVIDUAL'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS nfe_learners (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    occupation character varying(255),
    organization character varying(255),
    participant_number character varying(255),
    provider_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    user_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS nfe_materials (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    content_url character varying(255),
    description text,
    is_free boolean NOT NULL,
    material_type character varying(255) NOT NULL,
    program_id uuid,
    provider_id uuid NOT NULL,
    sort_order integer,
    title character varying(255) NOT NULL,
    CONSTRAINT nfe_materials_material_type_check CHECK (((material_type)::text = ANY ((ARRAY['DOCUMENT'::character varying, 'VIDEO'::character varying, 'AUDIO'::character varying, 'LINK'::character varying, 'FILE'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS nfe_programs (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    category character varying(255),
    description text,
    end_date timestamp(6) without time zone,
    is_published boolean NOT NULL,
    max_participants integer,
    program_type character varying(255) NOT NULL,
    provider_id uuid NOT NULL,
    start_date timestamp(6) without time zone,
    target_audience character varying(255),
    title character varying(255) NOT NULL,
    CONSTRAINT nfe_programs_program_type_check CHECK (((program_type)::text = ANY ((ARRAY['PROGRAM'::character varying, 'COURSE'::character varying, 'SEMINAR'::character varying, 'WORKSHOP'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS nfe_sessions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    description text,
    duration_minutes integer,
    max_participants integer,
    meeting_url character varying(255),
    program_id uuid,
    provider_id uuid NOT NULL,
    scheduled_at timestamp(6) without time zone NOT NULL,
    session_type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    CONSTRAINT nfe_sessions_session_type_check CHECK (((session_type)::text = ANY ((ARRAY['LIVE'::character varying, 'SEMINAR'::character varying, 'WORKSHOP'::character varying, 'WEBINAR'::character varying])::text[]))),
    CONSTRAINT nfe_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'LIVE'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);
CREATE TABLE IF NOT EXISTS parent_messages (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    body text NOT NULL,
    is_read boolean NOT NULL,
    message_type character varying(20) NOT NULL,
    recipient_id uuid NOT NULL,
    sender_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    subject character varying(300) NOT NULL
);
CREATE TABLE IF NOT EXISTS primary_learning_collaborations (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    activity text,
    collaboration_type character varying(30) NOT NULL,
    is_completed boolean NOT NULL,
    partner_name character varying(255),
    student_id uuid NOT NULL,
    subject_name character varying(100)
);
CREATE TABLE IF NOT EXISTS quest_challenges (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    completed_at timestamp(6) without time zone,
    description text,
    difficulty character varying(10) NOT NULL,
    is_completed boolean NOT NULL,
    quest_type character varying(30) NOT NULL,
    score integer,
    student_id uuid NOT NULL,
    subject_name character varying(100),
    title character varying(255) NOT NULL,
    total_points integer NOT NULL
);
CREATE TABLE IF NOT EXISTS reading_adventures (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    content text NOT NULL,
    cover_color character varying(20),
    is_favorite boolean NOT NULL,
    read_time_minutes integer NOT NULL,
    reading_level character varying(30) NOT NULL,
    student_id uuid NOT NULL,
    subject_name character varying(100),
    times_read integer NOT NULL,
    title character varying(255) NOT NULL,
    word_count integer NOT NULL
);
CREATE TABLE IF NOT EXISTS real_world_missions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    description text,
    evidence text,
    instructions text,
    is_completed boolean NOT NULL,
    location character varying(255),
    mission_title character varying(255) NOT NULL,
    mission_type character varying(30) NOT NULL,
    points integer,
    student_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS speaking_activities (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    activity_type character varying(30) NOT NULL,
    audio_url character varying(500),
    description text,
    duration_seconds integer,
    image_url character varying(500),
    is_completed boolean NOT NULL,
    student_id uuid NOT NULL,
    subject_name character varying(100),
    title character varying(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS student_badges (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    awarded_at timestamp(6) without time zone NOT NULL,
    badge_name character varying(100) NOT NULL,
    badge_type character varying(50) NOT NULL,
    description text,
    icon_url character varying(255),
    points integer,
    student_id uuid NOT NULL
);
CREATE TABLE IF NOT EXISTS student_streaks (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    current_streak integer NOT NULL,
    last_activity_date timestamp(6) without time zone,
    longest_streak integer NOT NULL,
    student_id uuid NOT NULL,
    total_points integer NOT NULL
);
CREATE TABLE IF NOT EXISTS verification_codes (
    id uuid NOT NULL,
    attempts integer NOT NULL,
    code character varying(5) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used boolean NOT NULL
);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'announcements'::regclass AND conname = 'announcements_pkey') THEN
    ALTER TABLE ONLY announcements ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'curriculum_topics'::regclass AND conname = 'curriculum_topics_pkey') THEN
    ALTER TABLE ONLY curriculum_topics ADD CONSTRAINT curriculum_topics_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'discovery_entries'::regclass AND conname = 'discovery_entries_pkey') THEN
    ALTER TABLE ONLY discovery_entries ADD CONSTRAINT discovery_entries_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'elmkusoma_labs'::regclass AND conname = 'elmkusoma_labs_pkey') THEN
    ALTER TABLE ONLY elmkusoma_labs ADD CONSTRAINT elmkusoma_labs_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learner_goals'::regclass AND conname = 'learner_goals_pkey') THEN
    ALTER TABLE ONLY learner_goals ADD CONSTRAINT learner_goals_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_evidence'::regclass AND conname = 'learning_evidence_pkey') THEN
    ALTER TABLE ONLY learning_evidence ADD CONSTRAINT learning_evidence_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_passports'::regclass AND conname = 'learning_passports_pkey') THEN
    ALTER TABLE ONLY learning_passports ADD CONSTRAINT learning_passports_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_profiles'::regclass AND conname = 'learning_profiles_pkey') THEN
    ALTER TABLE ONLY learning_profiles ADD CONSTRAINT learning_profiles_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_activities'::regclass AND conname = 'live_class_activities_pkey') THEN
    ALTER TABLE ONLY live_class_activities ADD CONSTRAINT live_class_activities_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_responses'::regclass AND conname = 'live_class_responses_pkey') THEN
    ALTER TABLE ONLY live_class_responses ADD CONSTRAINT live_class_responses_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'mistake_lab_entries'::regclass AND conname = 'mistake_lab_entries_pkey') THEN
    ALTER TABLE ONLY mistake_lab_entries ADD CONSTRAINT mistake_lab_entries_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_assessments'::regclass AND conname = 'nfe_assessments_pkey') THEN
    ALTER TABLE ONLY nfe_assessments ADD CONSTRAINT nfe_assessments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_attendance'::regclass AND conname = 'nfe_attendance_pkey') THEN
    ALTER TABLE ONLY nfe_attendance ADD CONSTRAINT nfe_attendance_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_certificates'::regclass AND conname = 'nfe_certificates_pkey') THEN
    ALTER TABLE ONLY nfe_certificates ADD CONSTRAINT nfe_certificates_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_education_providers'::regclass AND conname = 'nfe_education_providers_pkey') THEN
    ALTER TABLE ONLY nfe_education_providers ADD CONSTRAINT nfe_education_providers_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_learners'::regclass AND conname = 'nfe_learners_pkey') THEN
    ALTER TABLE ONLY nfe_learners ADD CONSTRAINT nfe_learners_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_materials'::regclass AND conname = 'nfe_materials_pkey') THEN
    ALTER TABLE ONLY nfe_materials ADD CONSTRAINT nfe_materials_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_programs'::regclass AND conname = 'nfe_programs_pkey') THEN
    ALTER TABLE ONLY nfe_programs ADD CONSTRAINT nfe_programs_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nfe_sessions'::regclass AND conname = 'nfe_sessions_pkey') THEN
    ALTER TABLE ONLY nfe_sessions ADD CONSTRAINT nfe_sessions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parent_messages'::regclass AND conname = 'parent_messages_pkey') THEN
    ALTER TABLE ONLY parent_messages ADD CONSTRAINT parent_messages_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'primary_learning_collaborations'::regclass AND conname = 'primary_learning_collaborations_pkey') THEN
    ALTER TABLE ONLY primary_learning_collaborations ADD CONSTRAINT primary_learning_collaborations_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'quest_challenges'::regclass AND conname = 'quest_challenges_pkey') THEN
    ALTER TABLE ONLY quest_challenges ADD CONSTRAINT quest_challenges_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'reading_adventures'::regclass AND conname = 'reading_adventures_pkey') THEN
    ALTER TABLE ONLY reading_adventures ADD CONSTRAINT reading_adventures_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'real_world_missions'::regclass AND conname = 'real_world_missions_pkey') THEN
    ALTER TABLE ONLY real_world_missions ADD CONSTRAINT real_world_missions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'speaking_activities'::regclass AND conname = 'speaking_activities_pkey') THEN
    ALTER TABLE ONLY speaking_activities ADD CONSTRAINT speaking_activities_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'student_badges'::regclass AND conname = 'student_badges_pkey') THEN
    ALTER TABLE ONLY student_badges ADD CONSTRAINT student_badges_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'student_streaks'::regclass AND conname = 'student_streaks_pkey') THEN
    ALTER TABLE ONLY student_streaks ADD CONSTRAINT student_streaks_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_profiles'::regclass AND conname = 'uk31adyb2icx3270ufx4brx2thv') THEN
    ALTER TABLE ONLY learning_profiles ADD CONSTRAINT uk31adyb2icx3270ufx4brx2thv UNIQUE (student_id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'verification_codes'::regclass AND conname = 'verification_codes_pkey') THEN
    ALTER TABLE ONLY verification_codes ADD CONSTRAINT verification_codes_pkey PRIMARY KEY (id);
  END IF;
END
$$;

-- ===== SECTION B: type/nullable/default changes =====
ALTER TABLE academic_years ALTER COLUMN education_level TYPE character varying(255) USING education_level::character varying(255);
ALTER TABLE academic_years ALTER COLUMN year_label TYPE character varying(255) USING year_label::character varying(255);
ALTER TABLE achievements ALTER COLUMN achievement_type TYPE character varying(255) USING achievement_type::character varying(255);
ALTER TABLE achievements ALTER COLUMN color TYPE character varying(255) USING color::character varying(255);
ALTER TABLE achievements ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE achievements ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE achievements ALTER COLUMN icon TYPE character varying(255) USING icon::character varying(255);
ALTER TABLE achievements ALTER COLUMN related_entity_type TYPE character varying(255) USING related_entity_type::character varying(255);
ALTER TABLE achievements ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE achievements ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE activity_feeds ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE activity_feeds ALTER COLUMN entity_type TYPE character varying(255) USING entity_type::character varying(255);
ALTER TABLE admin_delegations ALTER COLUMN revocation_reason TYPE character varying(255) USING revocation_reason::character varying(255);
ALTER TABLE admin_delegations ALTER COLUMN scope TYPE character varying(255) USING scope::character varying(255);
ALTER TABLE admin_delegations ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE assessments ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE assessments ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE assignment_submissions ALTER COLUMN file_url TYPE character varying(255) USING file_url::character varying(255);
ALTER TABLE assignments ALTER COLUMN attachments TYPE character varying(255) USING attachments::character varying(255);
ALTER TABLE assignments ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE assignments ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE attendance_records ALTER COLUMN remarks TYPE character varying(255) USING remarks::character varying(255);
ALTER TABLE attendance_records ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE attendance_summaries ALTER COLUMN attendance_percentage TYPE numeric(38,2) USING attendance_percentage::numeric(38,2);
ALTER TABLE audit_logs ALTER COLUMN action TYPE character varying(255) USING action::character varying(255);
ALTER TABLE audit_logs ALTER COLUMN entity_type TYPE character varying(255) USING entity_type::character varying(255);
ALTER TABLE audit_logs ALTER COLUMN ip_address TYPE character varying(255) USING ip_address::character varying(255);
ALTER TABLE audit_logs ALTER COLUMN user_agent TYPE character varying(255) USING user_agent::character varying(255);
ALTER TABLE certificate_templates ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE certificate_templates ALTER COLUMN logo_url TYPE character varying(255) USING logo_url::character varying(255);
ALTER TABLE certificate_templates ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE certificates ALTER COLUMN certificate_number TYPE character varying(255) USING certificate_number::character varying(255);
ALTER TABLE certificates ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE certificates ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE certificates ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE certificates ALTER COLUMN verification_code TYPE character varying(255) USING verification_code::character varying(255);
ALTER TABLE class_groups ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE class_groups ALTER COLUMN section TYPE character varying(255) USING section::character varying(255);
ALTER TABLE competencies ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE competencies ALTER COLUMN competency_type TYPE character varying(255) USING competency_type::character varying(255);
ALTER TABLE competencies ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE competency_records ALTER COLUMN evidence TYPE character varying(255) USING evidence::character varying(255);
ALTER TABLE competency_records ALTER COLUMN notes TYPE character varying(255) USING notes::character varying(255);
ALTER TABLE competency_records ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE custom_roles ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE custom_roles ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE data_import_jobs ALTER COLUMN file_name TYPE character varying(255) USING file_name::character varying(255);
ALTER TABLE data_import_jobs ALTER COLUMN import_type TYPE character varying(255) USING import_type::character varying(255);
ALTER TABLE data_import_jobs ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE deep_learning_contents ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE deep_learning_contents ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE departments ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE districts ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE districts ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE districts ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE enrollments ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE entitlements ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE entitlements ALTER COLUMN service_type TYPE character varying(255) USING service_type::character varying(255);
ALTER TABLE entitlements ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE entitlements ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE fieldwork_placements ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE fieldwork_placements ALTER COLUMN supervisor_phone TYPE character varying(255) USING supervisor_phone::character varying(255);
ALTER TABLE grade_boundaries ALTER COLUMN gpa_points TYPE numeric(38,2) USING gpa_points::numeric(38,2);
ALTER TABLE grade_boundaries ALTER COLUMN max_percentage TYPE numeric(38,2) USING max_percentage::numeric(38,2);
ALTER TABLE grade_boundaries ALTER COLUMN min_percentage TYPE numeric(38,2) USING min_percentage::numeric(38,2);
ALTER TABLE grades ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE grades ALTER COLUMN education_level TYPE character varying(255) USING education_level::character varying(255);
ALTER TABLE grades ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE grading_rubrics ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE grading_scales ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE grading_scales ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE institution_activity ALTER COLUMN activity_type TYPE character varying(255) USING activity_type::character varying(255);
ALTER TABLE institution_activity ALTER COLUMN actor_name TYPE character varying(255) USING actor_name::character varying(255);
ALTER TABLE institution_activity ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE institution_activity ALTER COLUMN metadata_json TYPE character varying(255) USING metadata_json::character varying(255);
ALTER TABLE institution_activity ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN action TYPE character varying(255) USING action::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN actor_email TYPE character varying(255) USING actor_email::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN actor_role TYPE character varying(255) USING actor_role::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN details TYPE character varying(255) USING details::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN ip_address TYPE character varying(255) USING ip_address::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN target_id TYPE character varying(255) USING target_id::character varying(255);
ALTER TABLE institution_audit_log ALTER COLUMN target_type TYPE character varying(255) USING target_type::character varying(255);
ALTER TABLE institution_invitations ALTER COLUMN email TYPE character varying(255) USING email::character varying(255);
ALTER TABLE institution_invitations ALTER COLUMN role TYPE character varying(255) USING role::character varying(255);
ALTER TABLE institution_invitations ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE institution_invitations ALTER COLUMN token TYPE character varying(255) USING token::character varying(255);
ALTER TABLE institution_memberships ALTER COLUMN role TYPE character varying(255) USING role::character varying(255);
ALTER TABLE institutions ALTER COLUMN address TYPE character varying(255) USING address::character varying(255);
ALTER TABLE institutions ALTER COLUMN approved_by TYPE character varying(255) USING approved_by::character varying(255);
ALTER TABLE institutions ALTER COLUMN banner_url TYPE character varying(255) USING banner_url::character varying(255);
ALTER TABLE institutions ALTER COLUMN city TYPE character varying(255) USING city::character varying(255);
ALTER TABLE institutions ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE institutions ALTER COLUMN country TYPE character varying(255) USING country::character varying(255);
ALTER TABLE institutions ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE institutions ALTER COLUMN enabled_services TYPE character varying(255) USING enabled_services::character varying(255);
ALTER TABLE institutions ALTER COLUMN logo_url TYPE character varying(255) USING logo_url::character varying(255);
ALTER TABLE institutions ALTER COLUMN metadata_json TYPE character varying(255) USING metadata_json::character varying(255);
ALTER TABLE institutions ALTER COLUMN motto TYPE character varying(255) USING motto::character varying(255);
ALTER TABLE institutions ALTER COLUMN phone TYPE character varying(255) USING phone::character varying(255);
ALTER TABLE institutions ALTER COLUMN region TYPE character varying(255) USING region::character varying(255);
ALTER TABLE institutions ALTER COLUMN type TYPE character varying(255) USING type::character varying(255);
ALTER TABLE learning_collaborations ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE learning_collaborations ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE learning_goals ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE learning_goals ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE learning_goals ALTER COLUMN goal_type TYPE character varying(255) USING goal_type::character varying(255);
ALTER TABLE learning_goals ALTER COLUMN related_entity_type TYPE character varying(255) USING related_entity_type::character varying(255);
ALTER TABLE learning_goals ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE learning_goals ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE learning_modules ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE learning_modules ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE lesson_progress ALTER COLUMN completion_percentage TYPE double precision USING completion_percentage::double precision;
ALTER TABLE lessons ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE lessons ALTER COLUMN file_attachments TYPE character varying(255) USING file_attachments::character varying(255);
ALTER TABLE lessons ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE lessons ALTER COLUMN video_url TYPE character varying(255) USING video_url::character varying(255);
ALTER TABLE live_class_chat_messages ALTER COLUMN message_type TYPE character varying(255) USING message_type::character varying(255);
ALTER TABLE live_class_issues ALTER COLUMN description TYPE character varying(2000) USING description::character varying(2000);
ALTER TABLE live_class_issues ALTER COLUMN issue_type TYPE character varying(255) USING issue_type::character varying(255);
ALTER TABLE live_class_issues ALTER COLUMN severity TYPE character varying(255) USING severity::character varying(255);
ALTER TABLE live_class_issues ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE live_class_polls ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE live_class_quiz_questions ALTER COLUMN correct_answer TYPE character varying(255) USING correct_answer::character varying(255);
ALTER TABLE live_class_quiz_responses ALTER COLUMN answer_text TYPE character varying(255) USING answer_text::character varying(255);
ALTER TABLE live_class_quizzes ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE live_class_session_events ALTER COLUMN event_data TYPE character varying(255) USING event_data::character varying(255);
ALTER TABLE live_class_session_events ALTER COLUMN event_type TYPE character varying(255) USING event_type::character varying(255);
ALTER TABLE media_assets ALTER COLUMN description TYPE character varying(2000) USING description::character varying(2000);
ALTER TABLE media_assets ALTER COLUMN media_type TYPE character varying(255) USING media_type::character varying(255);
ALTER TABLE media_assets ALTER COLUMN mime_type TYPE character varying(255) USING mime_type::character varying(255);
ALTER TABLE media_assets ALTER COLUMN source_type TYPE character varying(255) USING source_type::character varying(255);
ALTER TABLE media_assets ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE media_assets ALTER COLUMN visibility TYPE character varying(255) USING visibility::character varying(255);
ALTER TABLE nursery_activities ALTER COLUMN activity_type TYPE character varying(255) USING activity_type::character varying(255);
ALTER TABLE nursery_activities ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE nursery_activities ALTER COLUMN instructions TYPE character varying(255) USING instructions::character varying(255);
ALTER TABLE nursery_activities ALTER COLUMN materials_needed TYPE character varying(255) USING materials_needed::character varying(255);
ALTER TABLE nursery_daily_quests ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE nursery_daily_quests ALTER COLUMN quest_description TYPE character varying(255) USING quest_description::character varying(255);
ALTER TABLE nursery_daily_quests ALTER COLUMN quest_title TYPE character varying(255) USING quest_title::character varying(255);
ALTER TABLE nursery_daily_quests ALTER COLUMN quest_type TYPE character varying(255) USING quest_type::character varying(255);
ALTER TABLE nursery_daily_quests ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE nursery_daily_quests ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE nursery_feelings_checkin ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE nursery_feelings_checkin ALTER COLUMN emoji TYPE character varying(255) USING emoji::character varying(255);
ALTER TABLE nursery_feelings_checkin ALTER COLUMN feeling TYPE character varying(255) USING feeling::character varying(255);
ALTER TABLE nursery_feelings_checkin ALTER COLUMN note TYPE character varying(255) USING note::character varying(255);
ALTER TABLE nursery_feelings_checkin ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE nursery_milestones ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN evidence_image_url TYPE character varying(255) USING evidence_image_url::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN evidence_notes TYPE character varying(255) USING evidence_notes::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN mission_description TYPE character varying(255) USING mission_description::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN mission_title TYPE character varying(255) USING mission_title::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN mission_type TYPE character varying(255) USING mission_type::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE nursery_missions ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN activity_description TYPE character varying(255) USING activity_description::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN activity_title TYPE character varying(255) USING activity_title::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN activity_type TYPE character varying(255) USING activity_type::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN completion_status TYPE character varying(255) USING completion_status::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN notes TYPE character varying(255) USING notes::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN parent_name TYPE character varying(255) USING parent_name::character varying(255);
ALTER TABLE nursery_parent_learning ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE nursery_report_cards ALTER COLUMN cognitive_development TYPE character varying(255) USING cognitive_development::character varying(255);
ALTER TABLE nursery_report_cards ALTER COLUMN emotional_development TYPE character varying(255) USING emotional_development::character varying(255);
ALTER TABLE nursery_report_cards ALTER COLUMN physical_development TYPE character varying(255) USING physical_development::character varying(255);
ALTER TABLE nursery_report_cards ALTER COLUMN teacher_comments TYPE character varying(255) USING teacher_comments::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN audio_url TYPE character varying(255) USING audio_url::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN illustration_url TYPE character varying(255) USING illustration_url::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN reading_level TYPE character varying(255) USING reading_level::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN story_type TYPE character varying(255) USING story_type::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE nursery_stories ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN image_url TYPE character varying(255) USING image_url::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN region TYPE character varying(255) USING region::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN topic_description TYPE character varying(255) USING topic_description::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN topic_title TYPE character varying(255) USING topic_title::character varying(255);
ALTER TABLE nursery_tanzania_discovery ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE options ALTER COLUMN option_text TYPE character varying(255) USING option_text::character varying(255);
ALTER TABLE parent_student_links ALTER COLUMN relationship_type TYPE character varying(255) USING relationship_type::character varying(255);
ALTER TABLE parents ALTER COLUMN emergency_contact TYPE character varying(255) USING emergency_contact::character varying(255);
ALTER TABLE parents ALTER COLUMN relationship_type TYPE character varying(255) USING relationship_type::character varying(255);
ALTER TABLE payments ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE payments ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE payments ALTER COLUMN provider TYPE character varying(255) USING provider::character varying(255);
ALTER TABLE payments ALTER COLUMN provider_reference TYPE character varying(255) USING provider_reference::character varying(255);
ALTER TABLE payments ALTER COLUMN service_type TYPE character varying(255) USING service_type::character varying(255);
ALTER TABLE payments ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE payments ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE platform_config ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE platform_config ALTER COLUMN config_type TYPE character varying(255) USING config_type::character varying(255);
ALTER TABLE platform_config ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN affected_entity_type TYPE character varying(255) USING affected_entity_type::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN affected_service TYPE character varying(255) USING affected_service::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN resolution_notes TYPE character varying(255) USING resolution_notes::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN root_cause TYPE character varying(255) USING root_cause::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN severity TYPE character varying(255) USING severity::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE platform_incidents ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE platform_notifications ALTER COLUMN notification_type TYPE character varying(255) USING notification_type::character varying(255);
ALTER TABLE platform_notifications ALTER COLUMN priority TYPE character varying(255) USING priority::character varying(255);
ALTER TABLE platform_notifications ALTER COLUMN target_audience TYPE character varying(255) USING target_audience::character varying(255);
ALTER TABLE platform_notifications ALTER COLUMN target_role TYPE character varying(255) USING target_role::character varying(255);
ALTER TABLE platform_notifications ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE platform_services ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE platform_services ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE platform_services ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE portfolio_items ALTER COLUMN file_url TYPE character varying(255) USING file_url::character varying(255);
ALTER TABLE portfolio_items ALTER COLUMN item_type TYPE character varying(255) USING item_type::character varying(255);
ALTER TABLE portfolios ALTER COLUMN visibility TYPE character varying(255) USING visibility::character varying(255);
ALTER TABLE practical_demonstrations ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE professional_development_goals ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE professional_development_goals ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE programmes ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE programmes ALTER COLUMN education_level TYPE character varying(255) USING education_level::character varying(255);
ALTER TABLE programmes ALTER COLUMN programme_type TYPE character varying(255) USING programme_type::character varying(255);
ALTER TABLE project_submissions ALTER COLUMN file_url TYPE character varying(255) USING file_url::character varying(255);
ALTER TABLE project_submissions ALTER COLUMN grade TYPE character varying(255) USING grade::character varying(255);
ALTER TABLE project_submissions ALTER COLUMN submission_type TYPE character varying(255) USING submission_type::character varying(255);
ALTER TABLE provider_memberships ALTER COLUMN role TYPE character varying(255) USING role::character varying(255);
ALTER TABLE provider_service_entitlements ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE questions ALTER COLUMN question_type TYPE character varying(255) USING question_type::character varying(255);
ALTER TABLE regions ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE regions ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE regions ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE replays ALTER COLUMN caption_url TYPE character varying(255) USING caption_url::character varying(255);
ALTER TABLE replays ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE replays ALTER COLUMN recording_url TYPE character varying(255) USING recording_url::character varying(255);
ALTER TABLE replays ALTER COLUMN thumbnail_url TYPE character varying(255) USING thumbnail_url::character varying(255);
ALTER TABLE replays ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE report_cards ALTER COLUMN overall_grade TYPE character varying(255) USING overall_grade::character varying(255);
ALTER TABLE report_cards ALTER COLUMN remarks TYPE character varying(255) USING remarks::character varying(255);
ALTER TABLE report_cards ALTER COLUMN total_marks TYPE numeric(38,2) USING total_marks::numeric(38,2);
ALTER TABLE research_milestones ALTER COLUMN completed_date TYPE date USING completed_date::date;
ALTER TABLE research_milestones ALTER COLUMN title TYPE character varying(300) USING title::character varying(300);
ALTER TABLE research_projects ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE research_projects ALTER COLUMN title TYPE character varying(300) USING title::character varying(300);
ALTER TABLE research_resources ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE research_resources ALTER COLUMN resource_type TYPE character varying(50) USING resource_type::character varying(50);
ALTER TABLE research_resources ALTER COLUMN title TYPE character varying(300) USING title::character varying(300);
ALTER TABLE resources ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE resources ALTER COLUMN file_url TYPE character varying(255) USING file_url::character varying(255);
ALTER TABLE resources ALTER COLUMN title TYPE character varying(255) USING title::character varying(255);
ALTER TABLE role_permissions ALTER COLUMN permission TYPE character varying(255) USING permission::character varying(255);
ALTER TABLE rubric_criteria ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE rubric_criteria ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE secondary_concept_bank ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE secondary_concept_bank ALTER COLUMN concept_name TYPE character varying(255) USING concept_name::character varying(255);
ALTER TABLE secondary_concept_bank ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE secondary_concept_bank ALTER COLUMN difficulty_level TYPE character varying(255) USING difficulty_level::character varying(255);
ALTER TABLE secondary_concept_bank ALTER COLUMN related_concepts TYPE character varying(255) USING related_concepts::character varying(255);
ALTER TABLE secondary_concept_bank ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE secondary_error_bank ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE secondary_error_bank ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE secondary_error_bank ALTER COLUMN error_title TYPE character varying(255) USING error_title::character varying(255);
ALTER TABLE secondary_error_bank ALTER COLUMN frequency TYPE character varying(255) USING frequency::character varying(255);
ALTER TABLE secondary_error_bank ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE secondary_problem_bank ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE secondary_problem_bank ALTER COLUMN difficulty_level TYPE character varying(255) USING difficulty_level::character varying(255);
ALTER TABLE secondary_problem_bank ALTER COLUMN problem_title TYPE character varying(255) USING problem_title::character varying(255);
ALTER TABLE secondary_problem_bank ALTER COLUMN problem_type TYPE character varying(255) USING problem_type::character varying(255);
ALTER TABLE secondary_problem_bank ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE secondary_study_planner ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE secondary_study_planner ALTER COLUMN notes TYPE character varying(255) USING notes::character varying(255);
ALTER TABLE secondary_study_planner ALTER COLUMN priority TYPE character varying(255) USING priority::character varying(255);
ALTER TABLE secondary_study_planner ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE secondary_study_planner ALTER COLUMN topic_name TYPE character varying(255) USING topic_name::character varying(255);
ALTER TABLE secondary_study_planner ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE security_events ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE security_events ALTER COLUMN event_type TYPE character varying(255) USING event_type::character varying(255);
ALTER TABLE security_events ALTER COLUMN ip_address TYPE character varying(255) USING ip_address::character varying(255);
ALTER TABLE security_events ALTER COLUMN severity TYPE character varying(255) USING severity::character varying(255);
ALTER TABLE security_events ALTER COLUMN user_agent TYPE character varying(255) USING user_agent::character varying(255);
ALTER TABLE student_portfolio_items ALTER COLUMN thumbnail_url TYPE character varying(255) USING thumbnail_url::character varying(255);
ALTER TABLE student_projects ALTER COLUMN objective TYPE character varying(255) USING objective::character varying(255);
ALTER TABLE student_projects ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE students ALTER COLUMN address TYPE character varying(255) USING address::character varying(255);
ALTER TABLE students ALTER COLUMN admission_number TYPE character varying(255) USING admission_number::character varying(255);
ALTER TABLE students ALTER COLUMN blood_group TYPE character varying(255) USING blood_group::character varying(255);
ALTER TABLE students ALTER COLUMN city TYPE character varying(255) USING city::character varying(255);
ALTER TABLE students ALTER COLUMN gender TYPE character varying(255) USING gender::character varying(255);
ALTER TABLE students ALTER COLUMN guardian_name TYPE character varying(255) USING guardian_name::character varying(255);
ALTER TABLE students ALTER COLUMN guardian_phone TYPE character varying(255) USING guardian_phone::character varying(255);
ALTER TABLE students ALTER COLUMN guardian_relationship TYPE character varying(255) USING guardian_relationship::character varying(255);
ALTER TABLE students ALTER COLUMN national_id TYPE character varying(255) USING national_id::character varying(255);
ALTER TABLE students ALTER COLUMN region TYPE character varying(255) USING region::character varying(255);
ALTER TABLE students ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE study_tasks ALTER COLUMN priority TYPE character varying(255) USING priority::character varying(255);
ALTER TABLE study_tasks ALTER COLUMN task_type TYPE character varying(255) USING task_type::character varying(255);
ALTER TABLE subject_grades ALTER COLUMN grade TYPE character varying(255) USING grade::character varying(255);
ALTER TABLE subject_grades ALTER COLUMN marks_obtained TYPE numeric(38,2) USING marks_obtained::numeric(38,2);
ALTER TABLE subjects ALTER COLUMN code TYPE character varying(255) USING code::character varying(255);
ALTER TABLE subjects ALTER COLUMN education_level TYPE character varying(255) USING education_level::character varying(255);
ALTER TABLE subjects ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE support_tickets ALTER COLUMN category TYPE character varying(255) USING category::character varying(255);
ALTER TABLE support_tickets ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE support_tickets ALTER COLUMN priority TYPE character varying(255) USING priority::character varying(255);
ALTER TABLE support_tickets ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE support_tickets ALTER COLUMN subject TYPE character varying(255) USING subject::character varying(255);
ALTER TABLE support_tickets ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);
ALTER TABLE system_settings ALTER COLUMN description TYPE character varying(255) USING description::character varying(255);
ALTER TABLE system_settings ALTER COLUMN setting_key TYPE character varying(255) USING setting_key::character varying(255);
ALTER TABLE system_settings ALTER COLUMN setting_type TYPE character varying(255) USING setting_type::character varying(255);
ALTER TABLE teacher_assignments ALTER COLUMN academic_year TYPE character varying(255) USING academic_year::character varying(255);
ALTER TABLE teacher_qualifications ALTER COLUMN certificate_url TYPE character varying(255) USING certificate_url::character varying(255);
ALTER TABLE teachers ALTER COLUMN employee_number TYPE character varying(255) USING employee_number::character varying(255);
ALTER TABLE teachers ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE terms ALTER COLUMN name TYPE character varying(255) USING name::character varying(255);
ALTER TABLE theses ALTER COLUMN final_grade TYPE character varying(20) USING final_grade::character varying(20);
ALTER TABLE theses ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE theses ALTER COLUMN title TYPE character varying(300) USING title::character varying(300);
ALTER TABLE transcript_entries ALTER COLUMN grade TYPE character varying(255) USING grade::character varying(255);
ALTER TABLE transcript_entries ALTER COLUMN subject_name TYPE character varying(255) USING subject_name::character varying(255);
ALTER TABLE transcripts ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE transfer_records ALTER COLUMN reason TYPE character varying(255) USING reason::character varying(255);
ALTER TABLE users ALTER COLUMN first_name TYPE character varying(255) USING first_name::character varying(255);
ALTER TABLE users ALTER COLUMN form TYPE character varying(255) USING form::character varying(255);
ALTER TABLE users ALTER COLUMN last_name TYPE character varying(255) USING last_name::character varying(255);
ALTER TABLE users ALTER COLUMN learning_level TYPE character varying(255) USING learning_level::character varying(255);
ALTER TABLE users ALTER COLUMN middle_name TYPE character varying(255) USING middle_name::character varying(255);
ALTER TABLE users ALTER COLUMN phone TYPE character varying(255) USING phone::character varying(255);
ALTER TABLE users ALTER COLUMN profile_image_url TYPE character varying(255) USING profile_image_url::character varying(255);
ALTER TABLE users ALTER COLUMN role TYPE character varying(255) USING role::character varying(255);
ALTER TABLE users ALTER COLUMN secondary_stage TYPE character varying(255) USING secondary_stage::character varying(255);
ALTER TABLE verification_records ALTER COLUMN entity_type TYPE character varying(255) USING entity_type::character varying(255);
ALTER TABLE verification_records ALTER COLUMN notes TYPE character varying(255) USING notes::character varying(255);
ALTER TABLE verification_records ALTER COLUMN status TYPE character varying(255) USING status::character varying(255);
ALTER TABLE verification_records ALTER COLUMN verification_type TYPE character varying(255) USING verification_type::character varying(255);
ALTER TABLE workshop_sessions ALTER COLUMN created_by TYPE character varying(255) USING created_by::character varying(255);
ALTER TABLE workshop_sessions ALTER COLUMN updated_by TYPE character varying(255) USING updated_by::character varying(255);

-- ===== SECTION C: new columns on existing tables =====
ALTER TABLE IF EXISTS activity_feeds ADD COLUMN IF NOT EXISTS action character varying(255);
DO $$ BEGIN
  ALTER TABLE activity_feeds ALTER COLUMN action SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS activity_feeds ADD COLUMN IF NOT EXISTS actor_name character varying(255);
ALTER TABLE IF EXISTS activity_feeds ADD COLUMN IF NOT EXISTS entity_name character varying(255);
ALTER TABLE IF EXISTS activity_feeds ADD COLUMN IF NOT EXISTS visibility character varying(255);
DO $$ BEGIN
  ALTER TABLE activity_feeds ALTER COLUMN visibility SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS answers ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS answers ADD COLUMN IF NOT EXISTS feedback text;
ALTER TABLE IF EXISTS answers ADD COLUMN IF NOT EXISTS graded_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS answers ADD COLUMN IF NOT EXISTS graded_by uuid;
ALTER TABLE IF EXISTS answers ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS answers ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS assessment_results ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS assessment_results ADD COLUMN IF NOT EXISTS graded_by uuid;
ALTER TABLE IF EXISTS assessment_results ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS assessments ADD COLUMN IF NOT EXISTS participant_count bigint;
ALTER TABLE IF EXISTS assessments ADD COLUMN IF NOT EXISTS scheduled_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS assessments ADD COLUMN IF NOT EXISTS status character varying(30);
ALTER TABLE IF EXISTS assignment_submissions ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS assignment_submissions ADD COLUMN IF NOT EXISTS graded_by uuid;
ALTER TABLE IF EXISTS assignment_submissions ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS attempts ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS attempts ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS attendance_records ADD COLUMN IF NOT EXISTS attendance_date date;
DO $$ BEGIN
  ALTER TABLE attendance_records ALTER COLUMN attendance_date SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS attendance_records ADD COLUMN IF NOT EXISTS check_in_time timestamp(6) without time zone;
ALTER TABLE IF EXISTS attendance_records ADD COLUMN IF NOT EXISTS check_out_time timestamp(6) without time zone;
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS days_absent integer;
DO $$ BEGIN
  ALTER TABLE attendance_summaries ALTER COLUMN days_absent SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS days_excused integer;
DO $$ BEGIN
  ALTER TABLE attendance_summaries ALTER COLUMN days_excused SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS days_late integer;
DO $$ BEGIN
  ALTER TABLE attendance_summaries ALTER COLUMN days_late SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS days_present integer;
DO $$ BEGIN
  ALTER TABLE attendance_summaries ALTER COLUMN days_present SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS total_school_days integer;
DO $$ BEGIN
  ALTER TABLE attendance_summaries ALTER COLUMN total_school_days SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS attendance_summaries ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS duration_ms bigint;
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS entity_name character varying(255);
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS request_method character varying(255);
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS request_url character varying(255);
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS response_status integer;
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS session_id character varying(255);
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS user_email character varying(255);
ALTER TABLE IF EXISTS audit_logs ADD COLUMN IF NOT EXISTS user_role character varying(255);
ALTER TABLE IF EXISTS bulk_attendance_sessions ADD COLUMN IF NOT EXISTS attendance_date date;
DO $$ BEGIN
  ALTER TABLE bulk_attendance_sessions ALTER COLUMN attendance_date SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS bulk_attendance_sessions ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS bulk_attendance_sessions ADD COLUMN IF NOT EXISTS marked_count integer;
DO $$ BEGIN
  ALTER TABLE bulk_attendance_sessions ALTER COLUMN marked_count SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS bulk_attendance_sessions ADD COLUMN IF NOT EXISTS status character varying(255);
DO $$ BEGIN
  ALTER TABLE bulk_attendance_sessions ALTER COLUMN status SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS bulk_attendance_sessions ADD COLUMN IF NOT EXISTS total_students integer;
DO $$ BEGIN
  ALTER TABLE bulk_attendance_sessions ALTER COLUMN total_students SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS bulk_attendance_sessions ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS css_content text;
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS html_content text;
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS is_active boolean;
DO $$ BEGIN
  ALTER TABLE certificate_templates ALTER COLUMN is_active SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS signature_line_1 character varying(255);
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS signature_line_2 character varying(255);
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS signature_line_3 character varying(255);
ALTER TABLE IF EXISTS certificate_templates ADD COLUMN IF NOT EXISTS template_type character varying(255);
DO $$ BEGIN
  ALTER TABLE certificate_templates ALTER COLUMN template_type SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS certificate_type character varying(255);
DO $$ BEGIN
  ALTER TABLE certificates ALTER COLUMN certificate_type SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS completion_date date;
DO $$ BEGIN
  ALTER TABLE certificates ALTER COLUMN completion_date SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS course_or_programme character varying(255);
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS grade character varying(255);
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS instructor_name character varying(255);
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS issue_date timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE certificates ALTER COLUMN issue_date SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS issued_by uuid;
DO $$ BEGIN
  ALTER TABLE certificates ALTER COLUMN issued_by SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS qr_code_url character varying(255);
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS revoked_reason character varying(255);
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS serial_number character varying(255);
DO $$ BEGIN
  ALTER TABLE certificates ALTER COLUMN serial_number SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS student_id_number character varying(255);
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS student_name character varying(255);
DO $$ BEGIN
  ALTER TABLE certificates ALTER COLUMN student_name SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS certificates ADD COLUMN IF NOT EXISTS verification_url character varying(255);
ALTER TABLE IF EXISTS custom_roles ADD COLUMN IF NOT EXISTS display_name character varying(255);
DO $$ BEGIN
  ALTER TABLE custom_roles ALTER COLUMN display_name SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS custom_roles ADD COLUMN IF NOT EXISTS is_system_role boolean;
DO $$ BEGIN
  ALTER TABLE custom_roles ALTER COLUMN is_system_role SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS dashboard_snapshots ADD COLUMN IF NOT EXISTS expires_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS dashboard_snapshots ADD COLUMN IF NOT EXISTS generated_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE dashboard_snapshots ALTER COLUMN generated_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS dashboard_snapshots ADD COLUMN IF NOT EXISTS snapshot_type character varying(255);
DO $$ BEGIN
  ALTER TABLE dashboard_snapshots ALTER COLUMN snapshot_type SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS data_import_jobs ADD COLUMN IF NOT EXISTS file_url character varying(255);
ALTER TABLE IF EXISTS data_import_jobs ADD COLUMN IF NOT EXISTS imported_by uuid;
DO $$ BEGIN
  ALTER TABLE data_import_jobs ALTER COLUMN imported_by SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS deep_learning_contents ADD COLUMN IF NOT EXISTS is_deleted boolean;
DO $$ BEGIN
  ALTER TABLE deep_learning_contents ALTER COLUMN is_deleted SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS districts ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS enrollments ADD COLUMN IF NOT EXISTS withdraw_reason character varying(255);
ALTER TABLE IF EXISTS grade_boundaries ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS grade_boundaries ADD COLUMN IF NOT EXISTS grade_label character varying(255);
DO $$ BEGIN
  ALTER TABLE grade_boundaries ALTER COLUMN grade_label SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS grade_boundaries ADD COLUMN IF NOT EXISTS grade_name character varying(255);
ALTER TABLE IF EXISTS grade_boundaries ADD COLUMN IF NOT EXISTS sort_order integer;
DO $$ BEGIN
  ALTER TABLE grade_boundaries ALTER COLUMN sort_order SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS grade_boundaries ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS grading_rubrics ADD COLUMN IF NOT EXISTS is_active boolean;
DO $$ BEGIN
  ALTER TABLE grading_rubrics ALTER COLUMN is_active SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS grading_rubrics ADD COLUMN IF NOT EXISTS name character varying(255);
DO $$ BEGIN
  ALTER TABLE grading_rubrics ALTER COLUMN name SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS grading_rubrics ADD COLUMN IF NOT EXISTS total_points numeric(38,2);
DO $$ BEGIN
  ALTER TABLE grading_rubrics ALTER COLUMN total_points SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS grading_scales ADD COLUMN IF NOT EXISTS is_active boolean;
DO $$ BEGIN
  ALTER TABLE grading_scales ALTER COLUMN is_active SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS grading_scales ADD COLUMN IF NOT EXISTS max_value numeric(38,2);
ALTER TABLE IF EXISTS grading_scales ADD COLUMN IF NOT EXISTS min_value numeric(38,2);
ALTER TABLE IF EXISTS grading_scales ADD COLUMN IF NOT EXISTS scale_type character varying(255);
DO $$ BEGIN
  ALTER TABLE grading_scales ALTER COLUMN scale_type SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS learner_enrollments ADD COLUMN IF NOT EXISTS last_accessed_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS learning_collaborations ADD COLUMN IF NOT EXISTS is_deleted boolean;
DO $$ BEGIN
  ALTER TABLE learning_collaborations ALTER COLUMN is_deleted SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS learning_goals ADD COLUMN IF NOT EXISTS assigned_by uuid;
ALTER TABLE IF EXISTS learning_goals ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS learning_modules ADD COLUMN IF NOT EXISTS is_deleted boolean;
DO $$ BEGIN
  ALTER TABLE learning_modules ALTER COLUMN is_deleted SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS lesson_progress ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS lesson_progress ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_attendance_detail ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_attendance_detail ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_attendance_detail ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_attendance_detail ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_breakout_assignments ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE live_class_breakout_assignments ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_breakout_assignments ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_breakout_assignments ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_breakout_assignments ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_breakout_assignments ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_breakout_rooms ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_breakout_rooms ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_breakout_rooms ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_hand_raise_queue ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE live_class_hand_raise_queue ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_hand_raise_queue ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_hand_raise_queue ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_hand_raise_queue ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_hand_raise_queue ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_poll_votes ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE live_class_poll_votes ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_poll_votes ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_poll_votes ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_poll_votes ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_poll_votes ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_polls ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_polls ADD COLUMN IF NOT EXISTS teacher_id uuid;
DO $$ BEGIN
  ALTER TABLE live_class_polls ALTER COLUMN teacher_id SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_polls ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_polls ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_quiz_questions ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE live_class_quiz_questions ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_quiz_questions ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_quiz_questions ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_quiz_questions ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_quiz_questions ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_quiz_responses ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE live_class_quiz_responses ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_quiz_responses ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_quiz_responses ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_quiz_responses ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_quiz_responses ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_quizzes ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_quizzes ADD COLUMN IF NOT EXISTS teacher_id uuid;
DO $$ BEGIN
  ALTER TABLE live_class_quizzes ALTER COLUMN teacher_id SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_quizzes ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS live_class_shared_media ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE live_class_shared_media ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS live_class_shared_media ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS live_class_shared_media ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS live_class_shared_media ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS live_class_shared_media ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS activity_date date;
DO $$ BEGIN
  ALTER TABLE nursery_activities ALTER COLUMN activity_date SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS activity_name character varying(255);
DO $$ BEGIN
  ALTER TABLE nursery_activities ALTER COLUMN activity_name SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS age_group character varying(255);
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS class_group_id uuid;
DO $$ BEGIN
  ALTER TABLE nursery_activities ALTER COLUMN class_group_id SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS conducted_by uuid;
DO $$ BEGIN
  ALTER TABLE nursery_activities ALTER COLUMN conducted_by SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS learning_objectives character varying(255);
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS max_participants integer;
ALTER TABLE IF EXISTS nursery_activities ADD COLUMN IF NOT EXISTS status character varying(255);
DO $$ BEGIN
  ALTER TABLE nursery_activities ALTER COLUMN status SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_activity_participations ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS nursery_activity_participations ADD COLUMN IF NOT EXISTS engagement_score integer;
ALTER TABLE IF EXISTS nursery_activity_participations ADD COLUMN IF NOT EXISTS notes character varying(255);
ALTER TABLE IF EXISTS nursery_activity_participations ADD COLUMN IF NOT EXISTS participated_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE nursery_activity_participations ALTER COLUMN participated_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_activity_participations ADD COLUMN IF NOT EXISTS participation_level character varying(255);
ALTER TABLE IF EXISTS nursery_activity_participations ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS nursery_milestones ADD COLUMN IF NOT EXISTS category character varying(255);
DO $$ BEGIN
  ALTER TABLE nursery_milestones ALTER COLUMN category SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_milestones ADD COLUMN IF NOT EXISTS evidence_notes character varying(255);
ALTER TABLE IF EXISTS nursery_milestones ADD COLUMN IF NOT EXISTS expected_age_months integer;
ALTER TABLE IF EXISTS nursery_milestones ADD COLUMN IF NOT EXISTS milestone_name character varying(255);
DO $$ BEGIN
  ALTER TABLE nursery_milestones ALTER COLUMN milestone_name SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_milestones ADD COLUMN IF NOT EXISTS observed_by uuid;
ALTER TABLE IF EXISTS nursery_milestones ADD COLUMN IF NOT EXISTS status character varying(255);
DO $$ BEGIN
  ALTER TABLE nursery_milestones ALTER COLUMN status SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS areas_for_improvement character varying(255);
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS areas_of_strength character varying(255);
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS general_remarks character varying(255);
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS language_development character varying(255);
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS prepared_by uuid;
DO $$ BEGIN
  ALTER TABLE nursery_report_cards ALTER COLUMN prepared_by SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS published_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS recommendations_for_parents character varying(255);
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS social_development character varying(255);
ALTER TABLE IF EXISTS nursery_report_cards ADD COLUMN IF NOT EXISTS status character varying(255);
DO $$ BEGIN
  ALTER TABLE nursery_report_cards ALTER COLUMN status SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS options ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS options ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS options ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS professional_development_goals ADD COLUMN IF NOT EXISTS is_deleted boolean;
DO $$ BEGIN
  ALTER TABLE professional_development_goals ALTER COLUMN is_deleted SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS regions ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS average_mark numeric(38,2);
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS class_rank integer;
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS gpa numeric(38,2);
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS grading_scale_id uuid;
DO $$ BEGIN
  ALTER TABLE report_cards ALTER COLUMN grading_scale_id SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS published_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS status character varying(255);
DO $$ BEGIN
  ALTER TABLE report_cards ALTER COLUMN status SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS report_cards ADD COLUMN IF NOT EXISTS total_students_in_class integer;
ALTER TABLE IF EXISTS resources ADD COLUMN IF NOT EXISTS resource_type character varying(255);
DO $$ BEGIN
  ALTER TABLE resources ALTER COLUMN resource_type SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS revoked_tokens ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE revoked_tokens ALTER COLUMN created_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS revoked_tokens ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS revoked_tokens ADD COLUMN IF NOT EXISTS institution_id uuid;
ALTER TABLE IF EXISTS revoked_tokens ADD COLUMN IF NOT EXISTS is_deleted boolean;
DO $$ BEGIN
  ALTER TABLE revoked_tokens ALTER COLUMN is_deleted SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS revoked_tokens ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS revoked_tokens ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS rubric_criteria ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS rubric_criteria ADD COLUMN IF NOT EXISTS max_points numeric(38,2);
DO $$ BEGIN
  ALTER TABLE rubric_criteria ALTER COLUMN max_points SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS rubric_criteria ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS security_events ADD COLUMN IF NOT EXISTS location character varying(255);
ALTER TABLE IF EXISTS security_events ADD COLUMN IF NOT EXISTS user_email character varying(255);
ALTER TABLE IF EXISTS subject_grades ADD COLUMN IF NOT EXISTS assessed_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS subject_grades ADD COLUMN IF NOT EXISTS assessed_by uuid;
ALTER TABLE IF EXISTS subject_grades ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS subject_grades ADD COLUMN IF NOT EXISTS grade_points numeric(38,2);
ALTER TABLE IF EXISTS subject_grades ADD COLUMN IF NOT EXISTS teacher_remarks character varying(255);
ALTER TABLE IF EXISTS subject_grades ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS transcript_entries ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS transcript_entries ADD COLUMN IF NOT EXISTS remarks character varying(255);
ALTER TABLE IF EXISTS transcript_entries ADD COLUMN IF NOT EXISTS score numeric(5,2);
ALTER TABLE IF EXISTS transcript_entries ADD COLUMN IF NOT EXISTS subject_code character varying(255);
ALTER TABLE IF EXISTS transcript_entries ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS transcript_entries ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS academic_year character varying(255);
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS average_score numeric(5,2);
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS class_rank integer;
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS generated_at timestamp(6) without time zone;
DO $$ BEGIN
  ALTER TABLE transcripts ALTER COLUMN generated_at SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS issued_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS issued_by uuid;
DO $$ BEGIN
  ALTER TABLE transcripts ALTER COLUMN issued_by SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS remarks character varying(255);
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS serial_number character varying(255);
DO $$ BEGIN
  ALTER TABLE transcripts ALTER COLUMN serial_number SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS term character varying(255);
ALTER TABLE IF EXISTS transcripts ADD COLUMN IF NOT EXISTS total_subjects integer;
ALTER TABLE IF EXISTS transfer_records ADD COLUMN IF NOT EXISTS transferred_by uuid;
DO $$ BEGIN
  ALTER TABLE transfer_records ALTER COLUMN transferred_by SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;
ALTER TABLE IF EXISTS user_role_assignments ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE IF EXISTS user_role_assignments ADD COLUMN IF NOT EXISTS expires_at timestamp(6) without time zone;
ALTER TABLE IF EXISTS user_role_assignments ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE IF EXISTS workshop_sessions ADD COLUMN IF NOT EXISTS is_deleted boolean;
DO $$ BEGIN
  ALTER TABLE workshop_sessions ALTER COLUMN is_deleted SET NOT NULL;
EXCEPTION WHEN not_null_violation OR check_violation THEN NULL; END $$;

-- ===== SECTION D: sequences/indexes/constraints on existing tables =====
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'academic_years'::regclass AND conname = 'academic_years_pkey') THEN
    ALTER TABLE ONLY academic_years ADD CONSTRAINT academic_years_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_academic_years_institution ON academic_years USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_academic_years_level ON academic_years USING btree (education_level) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'academic_years'::regclass AND conname = 'fk_academic_years_institution') THEN
    ALTER TABLE ONLY academic_years ADD CONSTRAINT fk_academic_years_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'achievements'::regclass AND conname = 'achievements_pkey') THEN
    ALTER TABLE ONLY achievements ADD CONSTRAINT achievements_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_achievements_institution_id ON achievements USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_achievements_student_id ON achievements USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_achievements_type ON achievements USING btree (achievement_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'activity_feeds'::regclass AND conname = 'activity_feeds_pkey') THEN
    ALTER TABLE ONLY activity_feeds ADD CONSTRAINT activity_feeds_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_activity_feed_institution ON activity_feeds USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_activity_feed_user ON activity_feeds USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'activity_feeds'::regclass AND conname = 'fk_activity_feed_institution') THEN
    ALTER TABLE ONLY activity_feeds ADD CONSTRAINT fk_activity_feed_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'admin_delegations'::regclass AND conname = 'admin_delegations_pkey') THEN
    ALTER TABLE ONLY admin_delegations ADD CONSTRAINT admin_delegations_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_admin_delegations_delegate ON admin_delegations USING btree (delegate_id);
CREATE INDEX IF NOT EXISTS idx_admin_delegations_status ON admin_delegations USING btree (status);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'answers'::regclass AND conname = 'answers_pkey') THEN
    ALTER TABLE ONLY answers ADD CONSTRAINT answers_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'answers'::regclass AND conname = 'fk_answers_attempt') THEN
    ALTER TABLE ONLY answers ADD CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'answers'::regclass AND conname = 'fk_answers_institution') THEN
    ALTER TABLE ONLY answers ADD CONSTRAINT fk_answers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'answers'::regclass AND conname = 'fk_answers_question') THEN
    ALTER TABLE ONLY answers ADD CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assessment_results'::regclass AND conname = 'assessment_results_pkey') THEN
    ALTER TABLE ONLY assessment_results ADD CONSTRAINT assessment_results_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assessment_results'::regclass AND conname = 'fk_results_assessment') THEN
    ALTER TABLE ONLY assessment_results ADD CONSTRAINT fk_results_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assessment_results'::regclass AND conname = 'fk_results_institution') THEN
    ALTER TABLE ONLY assessment_results ADD CONSTRAINT fk_results_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assessment_results'::regclass AND conname = 'fk_results_student') THEN
    ALTER TABLE ONLY assessment_results ADD CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assessments'::regclass AND conname = 'assessments_pkey') THEN
    ALTER TABLE ONLY assessments ADD CONSTRAINT assessments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_assessments_class_group ON assessments USING btree (class_group_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assessments'::regclass AND conname = 'fk_assessments_institution') THEN
    ALTER TABLE ONLY assessments ADD CONSTRAINT fk_assessments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assignment_submissions'::regclass AND conname = 'assignment_submissions_pkey') THEN
    ALTER TABLE ONLY assignment_submissions ADD CONSTRAINT assignment_submissions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_assignment ON assignment_submissions USING btree (assignment_id);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_student ON assignment_submissions USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment ON assignment_submissions USING btree (assignment_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assignment_submissions'::regclass AND conname = 'fk_submissions_assignment') THEN
    ALTER TABLE ONLY assignment_submissions ADD CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assignment_submissions'::regclass AND conname = 'fk_submissions_institution') THEN
    ALTER TABLE ONLY assignment_submissions ADD CONSTRAINT fk_submissions_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assignment_submissions'::regclass AND conname = 'fk_submissions_student') THEN
    ALTER TABLE ONLY assignment_submissions ADD CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assignments'::regclass AND conname = 'assignments_pkey') THEN
    ALTER TABLE ONLY assignments ADD CONSTRAINT assignments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_assignments_cg ON assignments USING btree (class_group_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'assignments'::regclass AND conname = 'fk_assignments_institution') THEN
    ALTER TABLE ONLY assignments ADD CONSTRAINT fk_assignments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attempts'::regclass AND conname = 'attempts_pkey') THEN
    ALTER TABLE ONLY attempts ADD CONSTRAINT attempts_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_attempts_assessment ON attempts USING btree (assessment_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_attempts_student ON attempts USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attempts'::regclass AND conname = 'fk_attempts_assessment') THEN
    ALTER TABLE ONLY attempts ADD CONSTRAINT fk_attempts_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attempts'::regclass AND conname = 'fk_attempts_institution') THEN
    ALTER TABLE ONLY attempts ADD CONSTRAINT fk_attempts_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attempts'::regclass AND conname = 'fk_attempts_student') THEN
    ALTER TABLE ONLY attempts ADD CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_records'::regclass AND conname = 'attendance_records_pkey') THEN
    ALTER TABLE ONLY attendance_records ADD CONSTRAINT attendance_records_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_attendance_class_date ON attendance_records USING btree (class_group_id, record_date) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance_records USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_records'::regclass AND conname = 'fk_attendance_class_group') THEN
    ALTER TABLE ONLY attendance_records ADD CONSTRAINT fk_attendance_class_group FOREIGN KEY (class_group_id) REFERENCES classes(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_records'::regclass AND conname = 'fk_attendance_institution') THEN
    ALTER TABLE ONLY attendance_records ADD CONSTRAINT fk_attendance_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_records'::regclass AND conname = 'fk_attendance_student') THEN
    ALTER TABLE ONLY attendance_records ADD CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_summaries'::regclass AND conname = 'attendance_summaries_pkey') THEN
    ALTER TABLE ONLY attendance_summaries ADD CONSTRAINT attendance_summaries_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_summary_student ON attendance_summaries USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_summaries'::regclass AND conname = 'fk_summary_institution') THEN
    ALTER TABLE ONLY attendance_summaries ADD CONSTRAINT fk_summary_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'attendance_summaries'::regclass AND conname = 'fk_summary_student') THEN
    ALTER TABLE ONLY attendance_summaries ADD CONSTRAINT fk_summary_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'audit_logs'::regclass AND conname = 'audit_logs_pkey') THEN
    ALTER TABLE ONLY audit_logs ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_audit_logs_archived_at ON audit_logs USING btree (archived_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs USING btree (created_at) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs USING btree (entity_type, entity_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_audit_logs_institution ON audit_logs USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'audit_logs'::regclass AND conname = 'fk_audit_logs_institution') THEN
    ALTER TABLE ONLY audit_logs ADD CONSTRAINT fk_audit_logs_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'bulk_attendance_sessions'::regclass AND conname = 'bulk_attendance_sessions_pkey') THEN
    ALTER TABLE ONLY bulk_attendance_sessions ADD CONSTRAINT bulk_attendance_sessions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'bulk_attendance_sessions'::regclass AND conname = 'fk_bulk_session_class') THEN
    ALTER TABLE ONLY bulk_attendance_sessions ADD CONSTRAINT fk_bulk_session_class FOREIGN KEY (class_group_id) REFERENCES classes(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'bulk_attendance_sessions'::regclass AND conname = 'fk_bulk_session_institution') THEN
    ALTER TABLE ONLY bulk_attendance_sessions ADD CONSTRAINT fk_bulk_session_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificate_templates'::regclass AND conname = 'certificate_templates_pkey') THEN
    ALTER TABLE ONLY certificate_templates ADD CONSTRAINT certificate_templates_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_cert_templates_institution ON certificate_templates USING btree (institution_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificate_templates'::regclass AND conname = 'fk_cert_templates_institution') THEN
    ALTER TABLE ONLY certificate_templates ADD CONSTRAINT fk_cert_templates_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'certificates_certificate_number_key') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT certificates_certificate_number_key UNIQUE (certificate_number);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'certificates_pkey') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT certificates_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'certificates_verification_code_key') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT certificates_verification_code_key UNIQUE (verification_code);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'ukfhimy9jsw510b0ga7b2wo2nw2') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT ukfhimy9jsw510b0ga7b2wo2nw2 UNIQUE (serial_number);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_certificates_number ON certificates USING btree (certificate_number) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_certificates_student ON certificates USING btree (student_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_certificates_verification ON certificates USING btree (verification_code) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'fk_certificates_institution') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT fk_certificates_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'fk_certificates_student') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT fk_certificates_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'certificates'::regclass AND conname = 'fk_certificates_template') THEN
    ALTER TABLE ONLY certificates ADD CONSTRAINT fk_certificates_template FOREIGN KEY (template_id) REFERENCES certificate_templates(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'class_groups'::regclass AND conname = 'class_groups_pkey') THEN
    ALTER TABLE ONLY class_groups ADD CONSTRAINT class_groups_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_class_groups_grade ON class_groups USING btree (grade_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_class_groups_institution ON class_groups USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_class_groups_term ON class_groups USING btree (term_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'class_groups'::regclass AND conname = 'fk_class_groups_academic_year') THEN
    ALTER TABLE ONLY class_groups ADD CONSTRAINT fk_class_groups_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'class_groups'::regclass AND conname = 'fk_class_groups_grade') THEN
    ALTER TABLE ONLY class_groups ADD CONSTRAINT fk_class_groups_grade FOREIGN KEY (grade_id) REFERENCES grades(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'class_groups'::regclass AND conname = 'fk_class_groups_institution') THEN
    ALTER TABLE ONLY class_groups ADD CONSTRAINT fk_class_groups_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'class_groups'::regclass AND conname = 'fk_class_groups_term') THEN
    ALTER TABLE ONLY class_groups ADD CONSTRAINT fk_class_groups_term FOREIGN KEY (term_id) REFERENCES terms(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'competencies'::regclass AND conname = 'competencies_pkey') THEN
    ALTER TABLE ONLY competencies ADD CONSTRAINT competencies_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_competencies_institution ON competencies USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_competencies_programme ON competencies USING btree (programme_id);
CREATE INDEX IF NOT EXISTS idx_competencies_subject ON competencies USING btree (subject_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'competency_records'::regclass AND conname = 'competency_records_pkey') THEN
    ALTER TABLE ONLY competency_records ADD CONSTRAINT competency_records_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'competency_records'::regclass AND conname = 'uq_competency_record_student_competency') THEN
    ALTER TABLE ONLY competency_records ADD CONSTRAINT uq_competency_record_student_competency UNIQUE (student_id, competency_id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_competency_records_competency ON competency_records USING btree (competency_id);
CREATE INDEX IF NOT EXISTS idx_competency_records_student ON competency_records USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'custom_roles'::regclass AND conname = 'custom_roles_pkey') THEN
    ALTER TABLE ONLY custom_roles ADD CONSTRAINT custom_roles_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'custom_roles'::regclass AND conname = 'uq_custom_role_code') THEN
    ALTER TABLE ONLY custom_roles ADD CONSTRAINT uq_custom_role_code UNIQUE (institution_id, code);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'custom_roles'::regclass AND conname = 'fk_custom_roles_institution') THEN
    ALTER TABLE ONLY custom_roles ADD CONSTRAINT fk_custom_roles_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'dashboard_snapshots'::regclass AND conname = 'dashboard_snapshots_pkey') THEN
    ALTER TABLE ONLY dashboard_snapshots ADD CONSTRAINT dashboard_snapshots_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'dashboard_snapshots'::regclass AND conname = 'uq_snapshot_date') THEN
    ALTER TABLE ONLY dashboard_snapshots ADD CONSTRAINT uq_snapshot_date UNIQUE (institution_id, snapshot_date);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'dashboard_snapshots'::regclass AND conname = 'fk_snapshots_institution') THEN
    ALTER TABLE ONLY dashboard_snapshots ADD CONSTRAINT fk_snapshots_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'data_import_jobs'::regclass AND conname = 'data_import_jobs_pkey') THEN
    ALTER TABLE ONLY data_import_jobs ADD CONSTRAINT data_import_jobs_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'data_import_jobs'::regclass AND conname = 'fk_import_jobs_institution') THEN
    ALTER TABLE ONLY data_import_jobs ADD CONSTRAINT fk_import_jobs_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'deep_learning_contents'::regclass AND conname = 'deep_learning_contents_pkey') THEN
    ALTER TABLE ONLY deep_learning_contents ADD CONSTRAINT deep_learning_contents_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_dlc_institution ON deep_learning_contents USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_dlc_student ON deep_learning_contents USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'departments'::regclass AND conname = 'departments_pkey') THEN
    ALTER TABLE ONLY departments ADD CONSTRAINT departments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_departments_active ON departments USING btree (is_active) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_departments_institution ON departments USING btree (institution_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'districts'::regclass AND conname = 'districts_pkey') THEN
    ALTER TABLE ONLY districts ADD CONSTRAINT districts_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'districts'::regclass AND conname = 'uk_district_code') THEN
    ALTER TABLE ONLY districts ADD CONSTRAINT uk_district_code UNIQUE (code);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_districts_region ON districts USING btree (region_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'districts'::regclass AND conname = 'fk_district_region') THEN
    ALTER TABLE ONLY districts ADD CONSTRAINT fk_district_region FOREIGN KEY (region_id) REFERENCES regions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'enrollments'::regclass AND conname = 'enrollments_pkey') THEN
    ALTER TABLE ONLY enrollments ADD CONSTRAINT enrollments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_enrollments_class_group ON enrollments USING btree (class_group_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'enrollments'::regclass AND conname = 'fk_enrollments_class_group') THEN
    ALTER TABLE ONLY enrollments ADD CONSTRAINT fk_enrollments_class_group FOREIGN KEY (class_group_id) REFERENCES classes(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'enrollments'::regclass AND conname = 'fk_enrollments_institution') THEN
    ALTER TABLE ONLY enrollments ADD CONSTRAINT fk_enrollments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'enrollments'::regclass AND conname = 'fk_enrollments_student') THEN
    ALTER TABLE ONLY enrollments ADD CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'entitlements'::regclass AND conname = 'entitlements_pkey') THEN
    ALTER TABLE ONLY entitlements ADD CONSTRAINT entitlements_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_entitlements_institution_id ON entitlements USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_service ON entitlements USING btree (service_type, service_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_status ON entitlements USING btree (status);
CREATE INDEX IF NOT EXISTS idx_entitlements_student_id ON entitlements USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_user_id ON entitlements USING btree (user_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'fieldwork_placements'::regclass AND conname = 'fieldwork_placements_pkey') THEN
    ALTER TABLE ONLY fieldwork_placements ADD CONSTRAINT fieldwork_placements_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_fieldwork_placements_institution ON fieldwork_placements USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_fieldwork_placements_programme ON fieldwork_placements USING btree (programme_id);
CREATE INDEX IF NOT EXISTS idx_fieldwork_placements_status ON fieldwork_placements USING btree (status);
CREATE INDEX IF NOT EXISTS idx_fieldwork_placements_student ON fieldwork_placements USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_fieldwork_placements_supervisor ON fieldwork_placements USING btree (institution_supervisor_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grade_boundaries'::regclass AND conname = 'grade_boundaries_pkey') THEN
    ALTER TABLE ONLY grade_boundaries ADD CONSTRAINT grade_boundaries_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_boundaries_scale ON grade_boundaries USING btree (grading_scale_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grade_boundaries'::regclass AND conname = 'fk_boundaries_institution') THEN
    ALTER TABLE ONLY grade_boundaries ADD CONSTRAINT fk_boundaries_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grade_boundaries'::regclass AND conname = 'fk_boundaries_scale') THEN
    ALTER TABLE ONLY grade_boundaries ADD CONSTRAINT fk_boundaries_scale FOREIGN KEY (grading_scale_id) REFERENCES grading_scales(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grades'::regclass AND conname = 'grades_pkey') THEN
    ALTER TABLE ONLY grades ADD CONSTRAINT grades_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_grades_institution ON grades USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_grades_level ON grades USING btree (education_level) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grades'::regclass AND conname = 'fk_grades_institution') THEN
    ALTER TABLE ONLY grades ADD CONSTRAINT fk_grades_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grading_rubrics'::regclass AND conname = 'grading_rubrics_pkey') THEN
    ALTER TABLE ONLY grading_rubrics ADD CONSTRAINT grading_rubrics_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grading_rubrics'::regclass AND conname = 'fk_rubrics_institution') THEN
    ALTER TABLE ONLY grading_rubrics ADD CONSTRAINT fk_rubrics_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grading_scales'::regclass AND conname = 'grading_scales_pkey') THEN
    ALTER TABLE ONLY grading_scales ADD CONSTRAINT grading_scales_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_grading_scales_institution ON grading_scales USING btree (institution_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'grading_scales'::regclass AND conname = 'fk_grading_scales_institution') THEN
    ALTER TABLE ONLY grading_scales ADD CONSTRAINT fk_grading_scales_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'institution_activity'::regclass AND conname = 'institution_activity_pkey') THEN
    ALTER TABLE ONLY institution_activity ADD CONSTRAINT institution_activity_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_ia_created ON institution_activity USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_ia_institution ON institution_activity USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_ia_type ON institution_activity USING btree (activity_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'institution_audit_log'::regclass AND conname = 'institution_audit_log_pkey') THEN
    ALTER TABLE ONLY institution_audit_log ADD CONSTRAINT institution_audit_log_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_ial_action ON institution_audit_log USING btree (action);
CREATE INDEX IF NOT EXISTS idx_ial_actor ON institution_audit_log USING btree (actor_id);
CREATE INDEX IF NOT EXISTS idx_ial_created ON institution_audit_log USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_ial_institution ON institution_audit_log USING btree (institution_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'institution_invitations'::regclass AND conname = 'institution_invitations_pkey') THEN
    ALTER TABLE ONLY institution_invitations ADD CONSTRAINT institution_invitations_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_inv_email ON institution_invitations USING btree (email);
CREATE INDEX IF NOT EXISTS idx_inv_institution ON institution_invitations USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_inv_token ON institution_invitations USING btree (token);
CREATE SEQUENCE IF NOT EXISTS institution_memberships_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE institution_memberships_id_seq OWNED BY institution_memberships.id;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'institution_memberships'::regclass AND conname = 'institution_memberships_pkey') THEN
    ALTER TABLE ONLY institution_memberships ADD CONSTRAINT institution_memberships_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_institution_memberships_institution ON institution_memberships USING btree (institution_id) WHERE (is_active = true);
CREATE UNIQUE INDEX IF NOT EXISTS idx_institution_memberships_unique ON institution_memberships USING btree (user_id, institution_id) WHERE (is_active = true);
CREATE INDEX IF NOT EXISTS idx_institution_memberships_user ON institution_memberships USING btree (user_id) WHERE (is_active = true);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'institutions'::regclass AND conname = 'institutions_code_key') THEN
    ALTER TABLE ONLY institutions ADD CONSTRAINT institutions_code_key UNIQUE (code);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'institutions'::regclass AND conname = 'institutions_pkey') THEN
    ALTER TABLE ONLY institutions ADD CONSTRAINT institutions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_institutions_code ON institutions USING btree (code) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_institutions_district ON institutions USING btree (district_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_institutions_region ON institutions USING btree (region_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_institutions_status ON institutions USING btree (status);
CREATE INDEX IF NOT EXISTS idx_institutions_type ON institutions USING btree (type) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learner_enrollments'::regclass AND conname = 'learner_enrollments_pkey') THEN
    ALTER TABLE ONLY learner_enrollments ADD CONSTRAINT learner_enrollments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learner_enrollments'::regclass AND conname = 'uq_learner_course') THEN
    ALTER TABLE ONLY learner_enrollments ADD CONSTRAINT uq_learner_course UNIQUE (user_id, course_id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_collaborations'::regclass AND conname = 'learning_collaborations_pkey') THEN
    ALTER TABLE ONLY learning_collaborations ADD CONSTRAINT learning_collaborations_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_lc_institution ON learning_collaborations USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_lc_student ON learning_collaborations USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_goals'::regclass AND conname = 'learning_goals_pkey') THEN
    ALTER TABLE ONLY learning_goals ADD CONSTRAINT learning_goals_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_learning_goals_institution_id ON learning_goals USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_learning_goals_status ON learning_goals USING btree (status);
CREATE INDEX IF NOT EXISTS idx_learning_goals_student_id ON learning_goals USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'learning_modules'::regclass AND conname = 'learning_modules_pkey') THEN
    ALTER TABLE ONLY learning_modules ADD CONSTRAINT learning_modules_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_lm_institution ON learning_modules USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_lm_student ON learning_modules USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'lesson_progress'::regclass AND conname = 'lesson_progress_pkey') THEN
    ALTER TABLE ONLY lesson_progress ADD CONSTRAINT lesson_progress_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_progress_lesson ON lesson_progress USING btree (lesson_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_progress_student ON lesson_progress USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'lesson_progress'::regclass AND conname = 'fk_progress_institution') THEN
    ALTER TABLE ONLY lesson_progress ADD CONSTRAINT fk_progress_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'lesson_progress'::regclass AND conname = 'fk_progress_lesson') THEN
    ALTER TABLE ONLY lesson_progress ADD CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'lesson_progress'::regclass AND conname = 'fk_progress_student') THEN
    ALTER TABLE ONLY lesson_progress ADD CONSTRAINT fk_progress_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'lessons'::regclass AND conname = 'lessons_pkey') THEN
    ALTER TABLE ONLY lessons ADD CONSTRAINT lessons_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_lessons_class_group ON lessons USING btree (class_group_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_lessons_subject ON lessons USING btree (subject_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'lessons'::regclass AND conname = 'fk_lessons_institution') THEN
    ALTER TABLE ONLY lessons ADD CONSTRAINT fk_lessons_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_attendance_detail'::regclass AND conname = 'live_class_attendance_detail_pkey') THEN
    ALTER TABLE ONLY live_class_attendance_detail ADD CONSTRAINT live_class_attendance_detail_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_breakout_assignments'::regclass AND conname = 'live_class_breakout_assignments_pkey') THEN
    ALTER TABLE ONLY live_class_breakout_assignments ADD CONSTRAINT live_class_breakout_assignments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_breakout_rooms'::regclass AND conname = 'live_class_breakout_rooms_pkey') THEN
    ALTER TABLE ONLY live_class_breakout_rooms ADD CONSTRAINT live_class_breakout_rooms_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_chat_messages'::regclass AND conname = 'live_class_chat_messages_pkey') THEN
    ALTER TABLE ONLY live_class_chat_messages ADD CONSTRAINT live_class_chat_messages_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_lc_chat_class ON live_class_chat_messages USING btree (live_class_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_lc_chat_sent ON live_class_chat_messages USING btree (live_class_id, sent_at) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_lc_chat_user ON live_class_chat_messages USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_hand_raise_queue'::regclass AND conname = 'live_class_hand_raise_queue_pkey') THEN
    ALTER TABLE ONLY live_class_hand_raise_queue ADD CONSTRAINT live_class_hand_raise_queue_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_issues'::regclass AND conname = 'live_class_issues_pkey') THEN
    ALTER TABLE ONLY live_class_issues ADD CONSTRAINT live_class_issues_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_lc_issue_class ON live_class_issues USING btree (live_class_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_lc_issue_status ON live_class_issues USING btree (status) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_poll_votes'::regclass AND conname = 'live_class_poll_votes_pkey') THEN
    ALTER TABLE ONLY live_class_poll_votes ADD CONSTRAINT live_class_poll_votes_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_polls'::regclass AND conname = 'live_class_polls_pkey') THEN
    ALTER TABLE ONLY live_class_polls ADD CONSTRAINT live_class_polls_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_quiz_questions'::regclass AND conname = 'live_class_quiz_questions_pkey') THEN
    ALTER TABLE ONLY live_class_quiz_questions ADD CONSTRAINT live_class_quiz_questions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_quiz_responses'::regclass AND conname = 'live_class_quiz_responses_pkey') THEN
    ALTER TABLE ONLY live_class_quiz_responses ADD CONSTRAINT live_class_quiz_responses_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_quizzes'::regclass AND conname = 'live_class_quizzes_pkey') THEN
    ALTER TABLE ONLY live_class_quizzes ADD CONSTRAINT live_class_quizzes_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_session_events'::regclass AND conname = 'live_class_session_events_pkey') THEN
    ALTER TABLE ONLY live_class_session_events ADD CONSTRAINT live_class_session_events_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_lc_event_class ON live_class_session_events USING btree (live_class_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_lc_event_type ON live_class_session_events USING btree (live_class_id, event_type) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_lc_event_user ON live_class_session_events USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'live_class_shared_media'::regclass AND conname = 'live_class_shared_media_pkey') THEN
    ALTER TABLE ONLY live_class_shared_media ADD CONSTRAINT live_class_shared_media_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'media_assets'::regclass AND conname = 'media_assets_pkey') THEN
    ALTER TABLE ONLY media_assets ADD CONSTRAINT media_assets_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_media_institution ON media_assets USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_media_source ON media_assets USING btree (source_type, source_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_media_status ON media_assets USING btree (status) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_media_teacher ON media_assets USING btree (teacher_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_media_type ON media_assets USING btree (media_type) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_activities'::regclass AND conname = 'nursery_activities_pkey') THEN
    ALTER TABLE ONLY nursery_activities ADD CONSTRAINT nursery_activities_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_nursery_activities_institution ON nursery_activities USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_nursery_activities_type ON nursery_activities USING btree (activity_type) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_activities'::regclass AND conname = 'fk_nursery_activities_institution') THEN
    ALTER TABLE ONLY nursery_activities ADD CONSTRAINT fk_nursery_activities_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_activity_participations'::regclass AND conname = 'nursery_activity_participations_pkey') THEN
    ALTER TABLE ONLY nursery_activity_participations ADD CONSTRAINT nursery_activity_participations_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_nap_activity ON nursery_activity_participations USING btree (activity_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_nap_student ON nursery_activity_participations USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_activity_participations'::regclass AND conname = 'fk_nap_activity') THEN
    ALTER TABLE ONLY nursery_activity_participations ADD CONSTRAINT fk_nap_activity FOREIGN KEY (activity_id) REFERENCES nursery_activities(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_activity_participations'::regclass AND conname = 'fk_nap_institution') THEN
    ALTER TABLE ONLY nursery_activity_participations ADD CONSTRAINT fk_nap_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_activity_participations'::regclass AND conname = 'fk_nap_student') THEN
    ALTER TABLE ONLY nursery_activity_participations ADD CONSTRAINT fk_nap_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_daily_quests'::regclass AND conname = 'nursery_daily_quests_pkey') THEN
    ALTER TABLE ONLY nursery_daily_quests ADD CONSTRAINT nursery_daily_quests_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_feelings_checkin'::regclass AND conname = 'nursery_feelings_checkin_pkey') THEN
    ALTER TABLE ONLY nursery_feelings_checkin ADD CONSTRAINT nursery_feelings_checkin_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_milestones'::regclass AND conname = 'nursery_milestones_pkey') THEN
    ALTER TABLE ONLY nursery_milestones ADD CONSTRAINT nursery_milestones_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_milestones_student ON nursery_milestones USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_milestones'::regclass AND conname = 'fk_milestones_institution') THEN
    ALTER TABLE ONLY nursery_milestones ADD CONSTRAINT fk_milestones_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_milestones'::regclass AND conname = 'fk_milestones_student') THEN
    ALTER TABLE ONLY nursery_milestones ADD CONSTRAINT fk_milestones_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_missions'::regclass AND conname = 'nursery_missions_pkey') THEN
    ALTER TABLE ONLY nursery_missions ADD CONSTRAINT nursery_missions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_parent_learning'::regclass AND conname = 'nursery_parent_learning_pkey') THEN
    ALTER TABLE ONLY nursery_parent_learning ADD CONSTRAINT nursery_parent_learning_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_report_cards'::regclass AND conname = 'nursery_report_cards_pkey') THEN
    ALTER TABLE ONLY nursery_report_cards ADD CONSTRAINT nursery_report_cards_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_nrc_student ON nursery_report_cards USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_report_cards'::regclass AND conname = 'fk_nrc_institution') THEN
    ALTER TABLE ONLY nursery_report_cards ADD CONSTRAINT fk_nrc_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_report_cards'::regclass AND conname = 'fk_nrc_student') THEN
    ALTER TABLE ONLY nursery_report_cards ADD CONSTRAINT fk_nrc_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_stories'::regclass AND conname = 'nursery_stories_pkey') THEN
    ALTER TABLE ONLY nursery_stories ADD CONSTRAINT nursery_stories_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'nursery_tanzania_discovery'::regclass AND conname = 'nursery_tanzania_discovery_pkey') THEN
    ALTER TABLE ONLY nursery_tanzania_discovery ADD CONSTRAINT nursery_tanzania_discovery_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'options'::regclass AND conname = 'options_pkey') THEN
    ALTER TABLE ONLY options ADD CONSTRAINT options_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'options'::regclass AND conname = 'fk_options_institution') THEN
    ALTER TABLE ONLY options ADD CONSTRAINT fk_options_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'options'::regclass AND conname = 'fk_options_question') THEN
    ALTER TABLE ONLY options ADD CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parent_student_links'::regclass AND conname = 'parent_student_links_pkey') THEN
    ALTER TABLE ONLY parent_student_links ADD CONSTRAINT parent_student_links_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parent_student_links'::regclass AND conname = 'uq_parent_student') THEN
    ALTER TABLE ONLY parent_student_links ADD CONSTRAINT uq_parent_student UNIQUE (parent_id, student_id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_parent_student_links_parent ON parent_student_links USING btree (parent_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_parent_student_links_student ON parent_student_links USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parent_student_links'::regclass AND conname = 'fk_links_institution') THEN
    ALTER TABLE ONLY parent_student_links ADD CONSTRAINT fk_links_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parent_student_links'::regclass AND conname = 'fk_links_parent') THEN
    ALTER TABLE ONLY parent_student_links ADD CONSTRAINT fk_links_parent FOREIGN KEY (parent_id) REFERENCES parents(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parents'::regclass AND conname = 'parents_pkey') THEN
    ALTER TABLE ONLY parents ADD CONSTRAINT parents_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parents'::regclass AND conname = 'uq_parents_user') THEN
    ALTER TABLE ONLY parents ADD CONSTRAINT uq_parents_user UNIQUE (user_id, institution_id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_parents_institution ON parents USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_parents_user ON parents USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parents'::regclass AND conname = 'fk_parents_institution') THEN
    ALTER TABLE ONLY parents ADD CONSTRAINT fk_parents_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'parents'::regclass AND conname = 'fk_parents_user') THEN
    ALTER TABLE ONLY parents ADD CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'payments'::regclass AND conname = 'payments_pkey') THEN
    ALTER TABLE ONLY payments ADD CONSTRAINT payments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_payments_institution_id ON payments USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_payments_parent_id ON payments USING btree (parent_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments USING btree (status);
CREATE INDEX IF NOT EXISTS idx_payments_student_id ON payments USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'platform_config'::regclass AND conname = 'platform_config_config_key_key') THEN
    ALTER TABLE ONLY platform_config ADD CONSTRAINT platform_config_config_key_key UNIQUE (config_key);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'platform_config'::regclass AND conname = 'platform_config_pkey') THEN
    ALTER TABLE ONLY platform_config ADD CONSTRAINT platform_config_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_platform_config_category ON platform_config USING btree (category);
CREATE INDEX IF NOT EXISTS idx_platform_config_institution ON platform_config USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_platform_config_key ON platform_config USING btree (config_key);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'platform_incidents'::regclass AND conname = 'platform_incidents_pkey') THEN
    ALTER TABLE ONLY platform_incidents ADD CONSTRAINT platform_incidents_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_incidents_category ON platform_incidents USING btree (category);
CREATE INDEX IF NOT EXISTS idx_incidents_detected ON platform_incidents USING btree (detected_at);
CREATE INDEX IF NOT EXISTS idx_incidents_severity ON platform_incidents USING btree (severity);
CREATE INDEX IF NOT EXISTS idx_incidents_status ON platform_incidents USING btree (status);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'platform_notifications'::regclass AND conname = 'platform_notifications_pkey') THEN
    ALTER TABLE ONLY platform_notifications ADD CONSTRAINT platform_notifications_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_platform_notifications_audience ON platform_notifications USING btree (target_audience);
CREATE INDEX IF NOT EXISTS idx_platform_notifications_sent ON platform_notifications USING btree (sent_at);
CREATE INDEX IF NOT EXISTS idx_platform_notifications_type ON platform_notifications USING btree (notification_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'platform_services'::regclass AND conname = 'platform_services_code_key') THEN
    ALTER TABLE ONLY platform_services ADD CONSTRAINT platform_services_code_key UNIQUE (code);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'platform_services'::regclass AND conname = 'platform_services_pkey') THEN
    ALTER TABLE ONLY platform_services ADD CONSTRAINT platform_services_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_platform_services_active ON platform_services USING btree (is_active);
CREATE INDEX IF NOT EXISTS idx_platform_services_category ON platform_services USING btree (category);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'portfolio_items'::regclass AND conname = 'portfolio_items_pkey') THEN
    ALTER TABLE ONLY portfolio_items ADD CONSTRAINT portfolio_items_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_portfolio_items_competency ON portfolio_items USING btree (competency_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_items_item_type ON portfolio_items USING btree (item_type);
CREATE INDEX IF NOT EXISTS idx_portfolio_items_portfolio ON portfolio_items USING btree (portfolio_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_items_project ON portfolio_items USING btree (project_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'portfolios'::regclass AND conname = 'portfolios_pkey') THEN
    ALTER TABLE ONLY portfolios ADD CONSTRAINT portfolios_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_portfolios_institution ON portfolios USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_portfolios_student ON portfolios USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_portfolios_visibility ON portfolios USING btree (visibility);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'practical_demonstrations'::regclass AND conname = 'practical_demonstrations_pkey') THEN
    ALTER TABLE ONLY practical_demonstrations ADD CONSTRAINT practical_demonstrations_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_practical_demonstrations_competency ON practical_demonstrations USING btree (competency_id);
CREATE INDEX IF NOT EXISTS idx_practical_demonstrations_institution ON practical_demonstrations USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_practical_demonstrations_project ON practical_demonstrations USING btree (project_id);
CREATE INDEX IF NOT EXISTS idx_practical_demonstrations_reviewer ON practical_demonstrations USING btree (reviewer_id);
CREATE INDEX IF NOT EXISTS idx_practical_demonstrations_status ON practical_demonstrations USING btree (status);
CREATE INDEX IF NOT EXISTS idx_practical_demonstrations_student ON practical_demonstrations USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'professional_development_goals'::regclass AND conname = 'professional_development_goals_pkey') THEN
    ALTER TABLE ONLY professional_development_goals ADD CONSTRAINT professional_development_goals_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_pdg_institution ON professional_development_goals USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_pdg_student ON professional_development_goals USING btree (student_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'programmes'::regclass AND conname = 'programmes_pkey') THEN
    ALTER TABLE ONLY programmes ADD CONSTRAINT programmes_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_programmes_active ON programmes USING btree (is_active) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_programmes_education_level ON programmes USING btree (education_level);
CREATE INDEX IF NOT EXISTS idx_programmes_institution ON programmes USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_programmes_type ON programmes USING btree (programme_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'project_submissions'::regclass AND conname = 'project_submissions_pkey') THEN
    ALTER TABLE ONLY project_submissions ADD CONSTRAINT project_submissions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_project_submissions_milestone ON project_submissions USING btree (milestone_id);
CREATE INDEX IF NOT EXISTS idx_project_submissions_project ON project_submissions USING btree (project_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'provider_memberships'::regclass AND conname = 'provider_memberships_pkey') THEN
    ALTER TABLE ONLY provider_memberships ADD CONSTRAINT provider_memberships_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_provider_memberships_provider ON provider_memberships USING btree (provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_memberships_user ON provider_memberships USING btree (user_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'provider_service_entitlements'::regclass AND conname = 'provider_service_entitlements_pkey') THEN
    ALTER TABLE ONLY provider_service_entitlements ADD CONSTRAINT provider_service_entitlements_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_pse_provider ON provider_service_entitlements USING btree (provider_id);
CREATE INDEX IF NOT EXISTS idx_pse_service ON provider_service_entitlements USING btree (service_id);
CREATE INDEX IF NOT EXISTS idx_pse_status ON provider_service_entitlements USING btree (status);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'questions'::regclass AND conname = 'questions_pkey') THEN
    ALTER TABLE ONLY questions ADD CONSTRAINT questions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_questions_assessment ON questions USING btree (assessment_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'questions'::regclass AND conname = 'fk_questions_assessment') THEN
    ALTER TABLE ONLY questions ADD CONSTRAINT fk_questions_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'questions'::regclass AND conname = 'fk_questions_institution') THEN
    ALTER TABLE ONLY questions ADD CONSTRAINT fk_questions_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'regions'::regclass AND conname = 'regions_pkey') THEN
    ALTER TABLE ONLY regions ADD CONSTRAINT regions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'regions'::regclass AND conname = 'uk_region_code') THEN
    ALTER TABLE ONLY regions ADD CONSTRAINT uk_region_code UNIQUE (code);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'regions'::regclass AND conname = 'uk_region_name') THEN
    ALTER TABLE ONLY regions ADD CONSTRAINT uk_region_name UNIQUE (name);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'replays'::regclass AND conname = 'replays_pkey') THEN
    ALTER TABLE ONLY replays ADD CONSTRAINT replays_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_replays_event ON replays USING btree (event_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_replays_institution ON replays USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_replays_live_session ON replays USING btree (live_session_id);
CREATE INDEX IF NOT EXISTS idx_replays_status ON replays USING btree (status) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'report_cards'::regclass AND conname = 'report_cards_pkey') THEN
    ALTER TABLE ONLY report_cards ADD CONSTRAINT report_cards_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_report_cards_student ON report_cards USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'report_cards'::regclass AND conname = 'fk_report_cards_institution') THEN
    ALTER TABLE ONLY report_cards ADD CONSTRAINT fk_report_cards_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'report_cards'::regclass AND conname = 'fk_report_cards_student') THEN
    ALTER TABLE ONLY report_cards ADD CONSTRAINT fk_report_cards_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'research_milestones'::regclass AND conname = 'research_milestones_pkey') THEN
    ALTER TABLE ONLY research_milestones ADD CONSTRAINT research_milestones_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_research_milestones_due_date ON research_milestones USING btree (due_date);
CREATE INDEX IF NOT EXISTS idx_research_milestones_project ON research_milestones USING btree (research_project_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'research_projects'::regclass AND conname = 'research_projects_pkey') THEN
    ALTER TABLE ONLY research_projects ADD CONSTRAINT research_projects_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_research_projects_institution ON research_projects USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_research_projects_programme ON research_projects USING btree (programme_id);
CREATE INDEX IF NOT EXISTS idx_research_projects_status ON research_projects USING btree (status);
CREATE INDEX IF NOT EXISTS idx_research_projects_student ON research_projects USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_research_projects_subject ON research_projects USING btree (subject_id);
CREATE INDEX IF NOT EXISTS idx_research_projects_supervisor ON research_projects USING btree (supervisor_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'research_resources'::regclass AND conname = 'research_resources_pkey') THEN
    ALTER TABLE ONLY research_resources ADD CONSTRAINT research_resources_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_research_resources_project ON research_resources USING btree (research_project_id);
CREATE INDEX IF NOT EXISTS idx_research_resources_type ON research_resources USING btree (resource_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'resources'::regclass AND conname = 'resources_pkey') THEN
    ALTER TABLE ONLY resources ADD CONSTRAINT resources_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'resources'::regclass AND conname = 'fk_resources_institution') THEN
    ALTER TABLE ONLY resources ADD CONSTRAINT fk_resources_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'revoked_tokens'::regclass AND conname = 'revoked_tokens_pkey') THEN
    ALTER TABLE ONLY revoked_tokens ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires ON revoked_tokens USING btree (expires_at);
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_hash ON revoked_tokens USING btree (token_hash);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'role_permissions'::regclass AND conname = 'role_permissions_pkey') THEN
    ALTER TABLE ONLY role_permissions ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions USING btree (role_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'rubric_criteria'::regclass AND conname = 'rubric_criteria_pkey') THEN
    ALTER TABLE ONLY rubric_criteria ADD CONSTRAINT rubric_criteria_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'rubric_criteria'::regclass AND conname = 'fk_rubric_criteria_institution') THEN
    ALTER TABLE ONLY rubric_criteria ADD CONSTRAINT fk_rubric_criteria_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'rubric_criteria'::regclass AND conname = 'fk_rubric_criteria_rubric') THEN
    ALTER TABLE ONLY rubric_criteria ADD CONSTRAINT fk_rubric_criteria_rubric FOREIGN KEY (rubric_id) REFERENCES grading_rubrics(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'secondary_concept_bank'::regclass AND conname = 'secondary_concept_bank_pkey') THEN
    ALTER TABLE ONLY secondary_concept_bank ADD CONSTRAINT secondary_concept_bank_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'secondary_error_bank'::regclass AND conname = 'secondary_error_bank_pkey') THEN
    ALTER TABLE ONLY secondary_error_bank ADD CONSTRAINT secondary_error_bank_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'secondary_problem_bank'::regclass AND conname = 'secondary_problem_bank_pkey') THEN
    ALTER TABLE ONLY secondary_problem_bank ADD CONSTRAINT secondary_problem_bank_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'secondary_study_planner'::regclass AND conname = 'secondary_study_planner_pkey') THEN
    ALTER TABLE ONLY secondary_study_planner ADD CONSTRAINT secondary_study_planner_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'security_events'::regclass AND conname = 'security_events_pkey') THEN
    ALTER TABLE ONLY security_events ADD CONSTRAINT security_events_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_security_events_institution ON security_events USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_security_events_severity ON security_events USING btree (severity) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_security_events_type ON security_events USING btree (event_type) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'security_events'::regclass AND conname = 'fk_security_events_institution') THEN
    ALTER TABLE ONLY security_events ADD CONSTRAINT fk_security_events_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'student_portfolio_items'::regclass AND conname = 'student_portfolio_items_pkey') THEN
    ALTER TABLE ONLY student_portfolio_items ADD CONSTRAINT student_portfolio_items_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_student_portfolio_items_institution ON student_portfolio_items USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_student_portfolio_items_student ON student_portfolio_items USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_student_portfolio_items_type ON student_portfolio_items USING btree (portfolio_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'student_projects'::regclass AND conname = 'student_projects_pkey') THEN
    ALTER TABLE ONLY student_projects ADD CONSTRAINT student_projects_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_student_projects_institution ON student_projects USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_student_projects_instructor ON student_projects USING btree (instructor_id);
CREATE INDEX IF NOT EXISTS idx_student_projects_status ON student_projects USING btree (status);
CREATE INDEX IF NOT EXISTS idx_student_projects_student ON student_projects USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_student_projects_subject ON student_projects USING btree (subject_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'students'::regclass AND conname = 'students_admission_number_key') THEN
    ALTER TABLE ONLY students ADD CONSTRAINT students_admission_number_key UNIQUE (admission_number);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'students'::regclass AND conname = 'students_pkey') THEN
    ALTER TABLE ONLY students ADD CONSTRAINT students_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_students_admission ON students USING btree (admission_number) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_students_institution ON students USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_students_status ON students USING btree (status) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_students_user ON students USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'students'::regclass AND conname = 'fk_students_institution') THEN
    ALTER TABLE ONLY students ADD CONSTRAINT fk_students_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'students'::regclass AND conname = 'fk_students_user') THEN
    ALTER TABLE ONLY students ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'students'::regclass AND conname = 'students_institution_id_fkey') THEN
    ALTER TABLE ONLY students ADD CONSTRAINT students_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'students'::regclass AND conname = 'students_user_id_fkey') THEN
    ALTER TABLE ONLY students ADD CONSTRAINT students_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'study_tasks'::regclass AND conname = 'study_tasks_pkey') THEN
    ALTER TABLE ONLY study_tasks ADD CONSTRAINT study_tasks_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_study_tasks_institution ON study_tasks USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_study_tasks_priority ON study_tasks USING btree (priority);
CREATE INDEX IF NOT EXISTS idx_study_tasks_scheduled_date ON study_tasks USING btree (scheduled_date);
CREATE INDEX IF NOT EXISTS idx_study_tasks_student ON study_tasks USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_study_tasks_student_date ON study_tasks USING btree (student_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_study_tasks_subject ON study_tasks USING btree (subject_id);
CREATE INDEX IF NOT EXISTS idx_study_tasks_task_type ON study_tasks USING btree (task_type);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'subject_grades'::regclass AND conname = 'subject_grades_pkey') THEN
    ALTER TABLE ONLY subject_grades ADD CONSTRAINT subject_grades_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_subject_grades_report ON subject_grades USING btree (report_card_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'subject_grades'::regclass AND conname = 'fk_subject_grades_institution') THEN
    ALTER TABLE ONLY subject_grades ADD CONSTRAINT fk_subject_grades_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'subject_grades'::regclass AND conname = 'fk_subject_grades_report') THEN
    ALTER TABLE ONLY subject_grades ADD CONSTRAINT fk_subject_grades_report FOREIGN KEY (report_card_id) REFERENCES report_cards(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'subjects'::regclass AND conname = 'subjects_pkey') THEN
    ALTER TABLE ONLY subjects ADD CONSTRAINT subjects_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_subjects_institution ON subjects USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_subjects_level ON subjects USING btree (education_level) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'subjects'::regclass AND conname = 'subjects_institution_id_fkey') THEN
    ALTER TABLE ONLY subjects ADD CONSTRAINT subjects_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'support_tickets'::regclass AND conname = 'support_tickets_pkey') THEN
    ALTER TABLE ONLY support_tickets ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_support_tickets_institution_id ON support_tickets USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status ON support_tickets USING btree (status);
CREATE INDEX IF NOT EXISTS idx_support_tickets_user_id ON support_tickets USING btree (user_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'system_settings'::regclass AND conname = 'system_settings_pkey') THEN
    ALTER TABLE ONLY system_settings ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'system_settings'::regclass AND conname = 'uq_setting_key_institution') THEN
    ALTER TABLE ONLY system_settings ADD CONSTRAINT uq_setting_key_institution UNIQUE (institution_id, setting_key);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_settings_institution ON system_settings USING btree (institution_id) WHERE (is_deleted = false);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_settings_key ON system_settings USING btree (setting_key, institution_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'system_settings'::regclass AND conname = 'fk_settings_institution') THEN
    ALTER TABLE ONLY system_settings ADD CONSTRAINT fk_settings_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_assignments'::regclass AND conname = 'teacher_assignments_pkey') THEN
    ALTER TABLE ONLY teacher_assignments ADD CONSTRAINT teacher_assignments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_assignments'::regclass AND conname = 'uq_teacher_assignment') THEN
    ALTER TABLE ONLY teacher_assignments ADD CONSTRAINT uq_teacher_assignment UNIQUE (teacher_id, class_group_id, subject_id, academic_year);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_assignments_class_group ON teacher_assignments USING btree (class_group_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_assignments_subject ON teacher_assignments USING btree (subject_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_assignments_teacher ON teacher_assignments USING btree (teacher_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_assignments'::regclass AND conname = 'fk_assignments_institution') THEN
    ALTER TABLE ONLY teacher_assignments ADD CONSTRAINT fk_assignments_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_assignments'::regclass AND conname = 'fk_assignments_teacher') THEN
    ALTER TABLE ONLY teacher_assignments ADD CONSTRAINT fk_assignments_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_qualifications'::regclass AND conname = 'teacher_qualifications_pkey') THEN
    ALTER TABLE ONLY teacher_qualifications ADD CONSTRAINT teacher_qualifications_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_qualifications_teacher ON teacher_qualifications USING btree (teacher_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_teacher_qualifications_teacher ON teacher_qualifications USING btree (teacher_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_qualifications'::regclass AND conname = 'fk_qualifications_institution') THEN
    ALTER TABLE ONLY teacher_qualifications ADD CONSTRAINT fk_qualifications_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teacher_qualifications'::regclass AND conname = 'fk_qualifications_teacher') THEN
    ALTER TABLE ONLY teacher_qualifications ADD CONSTRAINT fk_qualifications_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teachers'::regclass AND conname = 'teachers_pkey') THEN
    ALTER TABLE ONLY teachers ADD CONSTRAINT teachers_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teachers'::regclass AND conname = 'uq_teachers_user') THEN
    ALTER TABLE ONLY teachers ADD CONSTRAINT uq_teachers_user UNIQUE (user_id, institution_id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_teachers_institution ON teachers USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_teachers_status ON teachers USING btree (status) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_teachers_user ON teachers USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teachers'::regclass AND conname = 'fk_teachers_institution') THEN
    ALTER TABLE ONLY teachers ADD CONSTRAINT fk_teachers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'teachers'::regclass AND conname = 'fk_teachers_user') THEN
    ALTER TABLE ONLY teachers ADD CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'terms'::regclass AND conname = 'terms_pkey') THEN
    ALTER TABLE ONLY terms ADD CONSTRAINT terms_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_terms_academic_year ON terms USING btree (academic_year_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'terms'::regclass AND conname = 'fk_terms_academic_year') THEN
    ALTER TABLE ONLY terms ADD CONSTRAINT fk_terms_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'terms'::regclass AND conname = 'fk_terms_institution') THEN
    ALTER TABLE ONLY terms ADD CONSTRAINT fk_terms_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'theses'::regclass AND conname = 'theses_pkey') THEN
    ALTER TABLE ONLY theses ADD CONSTRAINT theses_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_theses_institution ON theses USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_theses_programme ON theses USING btree (programme_id);
CREATE INDEX IF NOT EXISTS idx_theses_research_project ON theses USING btree (research_project_id);
CREATE INDEX IF NOT EXISTS idx_theses_status ON theses USING btree (status);
CREATE INDEX IF NOT EXISTS idx_theses_student ON theses USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_theses_supervisor ON theses USING btree (supervisor_id);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcript_entries'::regclass AND conname = 'transcript_entries_pkey') THEN
    ALTER TABLE ONLY transcript_entries ADD CONSTRAINT transcript_entries_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_transcript_entries_transcript ON transcript_entries USING btree (transcript_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcript_entries'::regclass AND conname = 'fk_transcript_entries_institution') THEN
    ALTER TABLE ONLY transcript_entries ADD CONSTRAINT fk_transcript_entries_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcript_entries'::regclass AND conname = 'fk_transcript_entries_transcript') THEN
    ALTER TABLE ONLY transcript_entries ADD CONSTRAINT fk_transcript_entries_transcript FOREIGN KEY (transcript_id) REFERENCES transcripts(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcripts'::regclass AND conname = 'transcripts_pkey') THEN
    ALTER TABLE ONLY transcripts ADD CONSTRAINT transcripts_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcripts'::regclass AND conname = 'transcripts_transcript_number_key') THEN
    ALTER TABLE ONLY transcripts ADD CONSTRAINT transcripts_transcript_number_key UNIQUE (transcript_number);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcripts'::regclass AND conname = 'uk4vnqqnsw1ggt455r5yhofxgj7') THEN
    ALTER TABLE ONLY transcripts ADD CONSTRAINT uk4vnqqnsw1ggt455r5yhofxgj7 UNIQUE (serial_number);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_transcripts_student ON transcripts USING btree (student_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcripts'::regclass AND conname = 'fk_transcripts_institution') THEN
    ALTER TABLE ONLY transcripts ADD CONSTRAINT fk_transcripts_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transcripts'::regclass AND conname = 'fk_transcripts_student') THEN
    ALTER TABLE ONLY transcripts ADD CONSTRAINT fk_transcripts_student FOREIGN KEY (student_id) REFERENCES students(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transfer_records'::regclass AND conname = 'transfer_records_pkey') THEN
    ALTER TABLE ONLY transfer_records ADD CONSTRAINT transfer_records_pkey PRIMARY KEY (id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transfer_records'::regclass AND conname = 'fk_transfers_enrollment') THEN
    ALTER TABLE ONLY transfer_records ADD CONSTRAINT fk_transfers_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'transfer_records'::regclass AND conname = 'fk_transfers_institution') THEN
    ALTER TABLE ONLY transfer_records ADD CONSTRAINT fk_transfers_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_role_assignments'::regclass AND conname = 'user_role_assignments_pkey') THEN
    ALTER TABLE ONLY user_role_assignments ADD CONSTRAINT user_role_assignments_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_ura_user ON user_role_assignments USING btree (user_id) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_role_assignments'::regclass AND conname = 'fk_ura_institution') THEN
    ALTER TABLE ONLY user_role_assignments ADD CONSTRAINT fk_ura_institution FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_role_assignments'::regclass AND conname = 'fk_ura_role') THEN
    ALTER TABLE ONLY user_role_assignments ADD CONSTRAINT fk_ura_role FOREIGN KEY (role_id) REFERENCES custom_roles(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_role_assignments'::regclass AND conname = 'fk_ura_user') THEN
    ALTER TABLE ONLY user_role_assignments ADD CONSTRAINT fk_ura_user FOREIGN KEY (user_id) REFERENCES users(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'users'::regclass AND conname = 'users_email_key') THEN
    ALTER TABLE ONLY users ADD CONSTRAINT users_email_key UNIQUE (email);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'users'::regclass AND conname = 'users_pkey') THEN
    ALTER TABLE ONLY users ADD CONSTRAINT users_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_users_district ON users USING btree (district_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_users_email ON users USING btree (email) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_users_institution ON users USING btree (institution_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_users_region ON users USING btree (region_id) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_users_role ON users USING btree (role) WHERE (is_deleted = false);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'users'::regclass AND conname = 'users_institution_id_fkey') THEN
    ALTER TABLE ONLY users ADD CONSTRAINT users_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institutions(id);
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'verification_records'::regclass AND conname = 'verification_records_pkey') THEN
    ALTER TABLE ONLY verification_records ADD CONSTRAINT verification_records_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_verification_records_entity ON verification_records USING btree (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_verification_records_status ON verification_records USING btree (status);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'workshop_sessions'::regclass AND conname = 'workshop_sessions_pkey') THEN
    ALTER TABLE ONLY workshop_sessions ADD CONSTRAINT workshop_sessions_pkey PRIMARY KEY (id);
  END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_ws_institution ON workshop_sessions USING btree (institution_id);
CREATE INDEX IF NOT EXISTS idx_ws_student ON workshop_sessions USING btree (student_id);
