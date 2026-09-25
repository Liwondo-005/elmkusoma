-- V87: Create 28 tables whose entities were mapped in JPA but never created by
-- any migration (pre-existing drift after the DB recreate; discovered when
-- GET /v1/primary/me/discovery failed with relation "discovery_entries" does
-- not exist). DDL captured from Hibernate schema-update output via pg_dump so
-- it matches the entity mappings exactly (incl. BaseEntity columns).
-- NOTE: guard with IF NOT EXISTS so re-apply on an already-repaired DB is a no-op.
-- Constraint statements are DO-guarded below (PG has no IF NOT EXISTS for constraints),
-- so re-apply stays a no-op even when the table already has its PK/UNIQUE.
CREATE TABLE IF NOT EXISTS public.announcements (
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


--
-- Name: curriculum_topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.curriculum_topics (
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


--
-- Name: discovery_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.discovery_entries (
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


--
-- Name: elmkusoma_labs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.elmkusoma_labs (
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


--
-- Name: learner_goals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.learner_goals (
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


--
-- Name: learning_evidence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.learning_evidence (
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


--
-- Name: learning_passports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.learning_passports (
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


--
-- Name: learning_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.learning_profiles (
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


--
-- Name: live_class_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.live_class_activities (
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


--
-- Name: live_class_responses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.live_class_responses (
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


--
-- Name: mistake_lab_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.mistake_lab_entries (
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


--
-- Name: nfe_assessments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_assessments (
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


--
-- Name: nfe_attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_attendance (
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


--
-- Name: nfe_certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_certificates (
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


--
-- Name: nfe_education_providers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_education_providers (
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


--
-- Name: nfe_learners; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_learners (
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


--
-- Name: nfe_materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_materials (
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


--
-- Name: nfe_programs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_programs (
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


--
-- Name: nfe_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.nfe_sessions (
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


--
-- Name: parent_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.parent_messages (
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


--
-- Name: primary_learning_collaborations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.primary_learning_collaborations (
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


--
-- Name: quest_challenges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.quest_challenges (
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


--
-- Name: reading_adventures; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.reading_adventures (
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


--
-- Name: real_world_missions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.real_world_missions (
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


--
-- Name: speaking_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.speaking_activities (
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


--
-- Name: student_badges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.student_badges (
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


--
-- Name: student_streaks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.student_streaks (
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


--
-- Name: verification_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.verification_codes (
    id uuid NOT NULL,
    attempts integer NOT NULL,
    code character varying(5) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used boolean NOT NULL
);


--
-- Name: announcements announcements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.announcements'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.announcements
        ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: curriculum_topics curriculum_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.curriculum_topics'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.curriculum_topics
        ADD CONSTRAINT curriculum_topics_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: discovery_entries discovery_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.discovery_entries'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.discovery_entries
        ADD CONSTRAINT discovery_entries_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: elmkusoma_labs elmkusoma_labs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.elmkusoma_labs'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.elmkusoma_labs
        ADD CONSTRAINT elmkusoma_labs_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: learner_goals learner_goals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.learner_goals'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.learner_goals
        ADD CONSTRAINT learner_goals_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: learning_evidence learning_evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.learning_evidence'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.learning_evidence
        ADD CONSTRAINT learning_evidence_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: learning_passports learning_passports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.learning_passports'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.learning_passports
        ADD CONSTRAINT learning_passports_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: learning_profiles learning_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.learning_profiles'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.learning_profiles
        ADD CONSTRAINT learning_profiles_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: live_class_activities live_class_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.live_class_activities'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.live_class_activities
        ADD CONSTRAINT live_class_activities_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: live_class_responses live_class_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.live_class_responses'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.live_class_responses
        ADD CONSTRAINT live_class_responses_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: mistake_lab_entries mistake_lab_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.mistake_lab_entries'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.mistake_lab_entries
        ADD CONSTRAINT mistake_lab_entries_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_assessments nfe_assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_assessments'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_assessments
        ADD CONSTRAINT nfe_assessments_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_attendance nfe_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_attendance'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_attendance
        ADD CONSTRAINT nfe_attendance_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_certificates nfe_certificates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_certificates'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_certificates
        ADD CONSTRAINT nfe_certificates_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_education_providers nfe_education_providers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_education_providers'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_education_providers
        ADD CONSTRAINT nfe_education_providers_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_learners nfe_learners_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_learners'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_learners
        ADD CONSTRAINT nfe_learners_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_materials nfe_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_materials'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_materials
        ADD CONSTRAINT nfe_materials_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_programs nfe_programs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_programs'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_programs
        ADD CONSTRAINT nfe_programs_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: nfe_sessions nfe_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.nfe_sessions'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.nfe_sessions
        ADD CONSTRAINT nfe_sessions_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: parent_messages parent_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.parent_messages'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.parent_messages
        ADD CONSTRAINT parent_messages_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: primary_learning_collaborations primary_learning_collaborations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.primary_learning_collaborations'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.primary_learning_collaborations
        ADD CONSTRAINT primary_learning_collaborations_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: quest_challenges quest_challenges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.quest_challenges'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.quest_challenges
        ADD CONSTRAINT quest_challenges_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: reading_adventures reading_adventures_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.reading_adventures'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.reading_adventures
        ADD CONSTRAINT reading_adventures_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: real_world_missions real_world_missions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.real_world_missions'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.real_world_missions
        ADD CONSTRAINT real_world_missions_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: speaking_activities speaking_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.speaking_activities'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.speaking_activities
        ADD CONSTRAINT speaking_activities_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: student_badges student_badges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.student_badges'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.student_badges
        ADD CONSTRAINT student_badges_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: student_streaks student_streaks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.student_streaks'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.student_streaks
        ADD CONSTRAINT student_streaks_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- Name: learning_profiles uk31adyb2icx3270ufx4brx2thv; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.learning_profiles'::regclass AND contype = 'u' AND conname = 'uk31adyb2icx3270ufx4brx2thv') THEN
    ALTER TABLE ONLY public.learning_profiles
        ADD CONSTRAINT uk31adyb2icx3270ufx4brx2thv UNIQUE (student_id);
  END IF;
END $$;


--
-- Name: verification_codes verification_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.verification_codes'::regclass AND contype = 'p') THEN
    ALTER TABLE ONLY public.verification_codes
        ADD CONSTRAINT verification_codes_pkey PRIMARY KEY (id);
  END IF;
END $$;


--
-- PostgreSQL database dump complete
--
