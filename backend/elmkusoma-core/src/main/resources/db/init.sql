--
-- PostgreSQL database dump
--


-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: resource_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.resource_type AS ENUM (
    'PDF',
    'DOCUMENT',
    'PRESENTATION',
    'SPREADSHEET',
    'IMAGE',
    'VIDEO',
    'AUDIO',
    'EXTERNAL_LINK',
    'LIVE_RECORDING',
    'ARCHIVE',
    'OTHER'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: academic_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.academic_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    programme_id uuid,
    academic_year character varying(20),
    semester character varying(20),
    total_credit_hours integer,
    earned_credit_hours integer,
    semester_gpa double precision,
    cumulative_gpa double precision,
    total_courses integer,
    completed_courses integer,
    failed_courses integer,
    academic_standing character varying(30),
    class_rank integer,
    total_students_in_class integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: academic_years; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.academic_years (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    education_level character varying(255) NOT NULL,
    year_label character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    is_current boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: achievements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.achievements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    student_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    achievement_type character varying(255) NOT NULL,
    icon character varying(255),
    color character varying(255),
    related_entity_type character varying(255),
    related_entity_id uuid,
    achieved_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false
);


--
-- Name: activity_feeds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activity_feeds (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid,
    activity_type character varying(50) NOT NULL,
    title character varying(300) NOT NULL,
    description character varying(255),
    entity_type character varying(255),
    entity_id uuid,
    metadata jsonb,
    is_read boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    action character varying(255) NOT NULL,
    actor_name character varying(255),
    entity_name character varying(255),
    visibility character varying(255) NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: admin_delegations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_delegations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    delegator_id uuid NOT NULL,
    delegate_id uuid NOT NULL,
    permissions jsonb NOT NULL,
    scope character varying(255) DEFAULT 'PLATFORM'::character varying NOT NULL,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    starts_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone,
    revoked_at timestamp without time zone,
    revoked_by uuid,
    revocation_reason character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    authority character varying(60) DEFAULT 'GENERAL_ADMIN'::character varying NOT NULL,
    reason text,
    notes text,
    resource_ids jsonb,
    approved_by uuid,
    approved_at timestamp without time zone,
    rejected_at timestamp without time zone,
    rejection_reason text
);


--
-- Name: announcements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.announcements (
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
    title character varying(300) NOT NULL,
    audience_type character varying(32),
    audience_region_id uuid,
    audience_district_id uuid,
    status character varying(16) DEFAULT 'PUBLISHED'::character varying,
    scheduled_at timestamp without time zone,
    published_at timestamp without time zone
);


--
-- Name: answers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    attempt_id uuid NOT NULL,
    question_id uuid NOT NULL,
    selected_option_id uuid,
    text_answer text,
    is_correct boolean,
    marks_obtained integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    feedback text,
    graded_at timestamp(6) without time zone,
    graded_by uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: assessment_answers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_answers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    attempt_id uuid NOT NULL,
    question_id uuid NOT NULL,
    selected_option_id uuid,
    text_answer text,
    is_correct boolean,
    points_earned numeric(5,2),
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: assessment_attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_attempts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    assessment_id uuid NOT NULL,
    student_id uuid NOT NULL,
    started_at timestamp without time zone DEFAULT now(),
    completed_at timestamp without time zone,
    score numeric(5,2),
    status character varying(20) DEFAULT 'IN_PROGRESS'::character varying,
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: assessment_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_results (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    assessment_id uuid NOT NULL,
    student_id uuid NOT NULL,
    attempt_id uuid NOT NULL,
    total_score integer DEFAULT 0 NOT NULL,
    is_passed boolean DEFAULT false NOT NULL,
    graded_at timestamp without time zone,
    feedback text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    graded_by uuid,
    updated_by character varying(255)
);


--
-- Name: assessments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    time_limit_minutes integer,
    total_marks integer DEFAULT 100 NOT NULL,
    pass_marks integer DEFAULT 50 NOT NULL,
    is_published boolean DEFAULT false NOT NULL,
    starts_at timestamp without time zone,
    ends_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    participant_count bigint,
    scheduled_at timestamp(6) without time zone,
    status character varying(30),
    lesson_id uuid,
    max_attempts integer
);


--
-- Name: assignment_submissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assignment_submissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    assignment_id uuid NOT NULL,
    student_id uuid NOT NULL,
    file_url character varying(255),
    submitted_at timestamp without time zone DEFAULT now() NOT NULL,
    grade integer,
    feedback text,
    graded_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    graded_by uuid,
    updated_by character varying(255),
    submission_text text,
    status character varying(32) DEFAULT 'SUBMITTED'::character varying,
    is_draft boolean DEFAULT false,
    is_late boolean DEFAULT false
);


--
-- Name: assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    due_date timestamp without time zone,
    total_marks integer DEFAULT 100 NOT NULL,
    attachments character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    assignment_type character varying(50),
    instructions text,
    status character varying(20) DEFAULT 'PUBLISHED'::character varying,
    lesson_id uuid,
    open_date timestamp without time zone,
    close_date timestamp without time zone,
    allow_late_submission boolean DEFAULT false
);


--
-- Name: attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attempts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    assessment_id uuid NOT NULL,
    student_id uuid NOT NULL,
    started_at timestamp without time zone DEFAULT now() NOT NULL,
    submitted_at timestamp without time zone,
    is_completed boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: attendance_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    record_date date NOT NULL,
    status character varying(255) DEFAULT 'PRESENT'::character varying NOT NULL,
    remarks character varying(255),
    marked_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    attendance_date date NOT NULL,
    check_in_time timestamp(6) without time zone,
    check_out_time timestamp(6) without time zone
);


--
-- Name: attendance_summaries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance_summaries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    term_id uuid NOT NULL,
    total_days integer DEFAULT 0 NOT NULL,
    present_days integer DEFAULT 0 NOT NULL,
    absent_days integer DEFAULT 0 NOT NULL,
    late_days integer DEFAULT 0 NOT NULL,
    excused_days integer DEFAULT 0 NOT NULL,
    attendance_percentage numeric(38,2),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    days_absent integer NOT NULL,
    days_excused integer NOT NULL,
    days_late integer NOT NULL,
    days_present integer NOT NULL,
    total_school_days integer NOT NULL,
    updated_by character varying(255)
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid,
    action character varying(255) NOT NULL,
    entity_type character varying(255) NOT NULL,
    entity_id uuid,
    old_values jsonb,
    new_values jsonb,
    ip_address character varying(255),
    user_agent character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    archived_at timestamp without time zone,
    duration_ms bigint,
    entity_name character varying(255),
    request_method character varying(255),
    request_url character varying(255),
    response_status integer,
    session_id character varying(255),
    user_email character varying(255),
    user_role character varying(255),
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: bookmarks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bookmarks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    target_type character varying(50) NOT NULL,
    target_id uuid NOT NULL,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: bulk_attendance_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bulk_attendance_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    record_date date NOT NULL,
    total_marked integer DEFAULT 0 NOT NULL,
    marked_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    attendance_date date NOT NULL,
    created_by character varying(255),
    marked_count integer NOT NULL,
    status character varying(255) NOT NULL,
    total_students integer NOT NULL,
    updated_by character varying(255)
);


--
-- Name: career_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.career_profiles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    career_objective text,
    target_industry character varying(100),
    target_role character varying(150),
    skills text,
    certifications text,
    experience_summary text,
    cv_file_url character varying(500),
    linkedin_url character varying(500),
    portfolio_url character varying(500),
    is_public boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: certificate_signatories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.certificate_signatories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    full_name character varying(200) NOT NULL,
    position_title character varying(200),
    organization character varying(200),
    signature_image text,
    certificate_types character varying(200),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    valid_from date,
    valid_until date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_cert_signatories_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: certificate_template_signatories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.certificate_template_signatories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    template_id uuid NOT NULL,
    signatory_id uuid NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: certificate_template_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.certificate_template_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    template_id uuid NOT NULL,
    version integer NOT NULL,
    name character varying(200) NOT NULL,
    template_type character varying(30) NOT NULL,
    description text,
    html_content text,
    css_content text,
    logo_url character varying(500),
    archived_at timestamp without time zone DEFAULT now() NOT NULL,
    archived_by character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: certificate_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.certificate_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    template_html text,
    logo_url character varying(255),
    signature_url character varying(500),
    is_default boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    css_content text,
    html_content text,
    is_active boolean NOT NULL,
    signature_line_1 character varying(255),
    signature_line_2 character varying(255),
    signature_line_3 character varying(255),
    template_type character varying(255) NOT NULL,
    version integer DEFAULT 1 NOT NULL
);


--
-- Name: certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.certificates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    template_id uuid,
    certificate_number character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    issued_date date DEFAULT CURRENT_DATE NOT NULL,
    expiry_date date,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    verification_code character varying(255) NOT NULL,
    pdf_url character varying(500),
    revoked_at timestamp without time zone,
    revocation_reason text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    skills jsonb DEFAULT '[]'::jsonb,
    metadata jsonb DEFAULT '{}'::jsonb,
    certificate_type character varying(255) NOT NULL,
    completion_date date NOT NULL,
    course_or_programme character varying(255),
    grade character varying(255),
    instructor_name character varying(255),
    issue_date timestamp(6) without time zone NOT NULL,
    issued_by uuid NOT NULL,
    qr_code_url character varying(255),
    revoked_reason character varying(255),
    serial_number character varying(255) NOT NULL,
    student_id_number character varying(255),
    student_name character varying(255) NOT NULL,
    verification_url character varying(255)
);


--
-- Name: class_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    grade_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    term_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    section character varying(255),
    capacity integer,
    class_teacher_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: class_subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_subjects (
    class_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    teacher_id uuid,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: class_timetable; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_timetable (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    class_group_id uuid NOT NULL,
    subject_id uuid,
    teacher_id uuid,
    day_of_week character varying(10) NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    room character varying(100),
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: classes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.classes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    level character varying(50) NOT NULL,
    section character varying(20),
    capacity integer,
    class_teacher_id uuid,
    academic_year character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: competencies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.competencies (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255),
    description character varying(255),
    competency_type character varying(255) NOT NULL,
    programme_id uuid,
    subject_id uuid,
    sort_order integer DEFAULT 0,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: competency_assessments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.competency_assessments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    competency_id uuid NOT NULL,
    assessment_id uuid NOT NULL,
    weight integer DEFAULT 100 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: competency_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.competency_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    competency_id uuid NOT NULL,
    status character varying(255) DEFAULT 'NOT_STARTED'::character varying NOT NULL,
    evidence character varying(255),
    assessed_by uuid,
    assessment_date date,
    last_practice_date date,
    notes character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: content_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_reports (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    entity_type character varying(50) NOT NULL,
    entity_id uuid NOT NULL,
    entity_title character varying(500),
    reporter_id uuid,
    reason character varying(100) NOT NULL,
    description text,
    status character varying(30) DEFAULT 'OPEN'::character varying NOT NULL,
    resolution_notes text,
    resolved_by uuid,
    resolved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: course_lessons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.course_lessons (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    module_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    content_type character varying(50) NOT NULL,
    content_url character varying(500),
    duration_minutes integer,
    sort_order integer DEFAULT 0 NOT NULL,
    is_free boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: course_modules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.course_modules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    course_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: courses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.courses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid,
    title character varying(300) NOT NULL,
    description text,
    thumbnail_url character varying(500),
    level character varying(50) NOT NULL,
    category character varying(100),
    is_published boolean DEFAULT false NOT NULL,
    is_featured boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: curriculum_topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.curriculum_topics (
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
-- Name: custom_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custom_roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(255),
    is_system boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    display_name character varying(255) NOT NULL,
    is_system_role boolean NOT NULL
);


--
-- Name: dashboard_snapshots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_snapshots (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    snapshot_date date DEFAULT CURRENT_DATE NOT NULL,
    total_students integer DEFAULT 0,
    total_teachers integer DEFAULT 0,
    total_classes integer DEFAULT 0,
    total_courses integer DEFAULT 0,
    active_enrollments integer DEFAULT 0,
    attendance_rate numeric(5,2),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    snapshot_data jsonb DEFAULT '{}'::jsonb,
    expires_at timestamp(6) without time zone,
    generated_at timestamp(6) without time zone NOT NULL,
    snapshot_type character varying(255) NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: data_import_jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_import_jobs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    import_type character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    total_rows integer DEFAULT 0,
    processed_rows integer DEFAULT 0,
    successful_rows integer DEFAULT 0,
    failed_rows integer DEFAULT 0,
    error_log jsonb,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    file_url character varying(255),
    imported_by uuid NOT NULL
);


--
-- Name: deep_learning_contents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.deep_learning_contents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    course_id uuid,
    module_id uuid,
    title character varying(300) NOT NULL,
    content_type character varying(30) DEFAULT 'ARTICLE'::character varying NOT NULL,
    content_text text,
    file_url character varying(500),
    difficulty_level character varying(20) DEFAULT 'INTERMEDIATE'::character varying,
    tags character varying(500),
    is_completed boolean DEFAULT false,
    time_spent_minutes integer DEFAULT 0,
    notes text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean NOT NULL
);


--
-- Name: departments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255),
    description text,
    programme_ids jsonb,
    head_of_department_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: discovery_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discovery_entries (
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
-- Name: districts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.districts (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    region_id uuid NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    institution_id uuid
);


--
-- Name: elmkusoma_labs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.elmkusoma_labs (
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
-- Name: email_verification_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verification_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    institution_id uuid,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: enrollment_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrollment_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    enrollment_id uuid NOT NULL,
    student_id uuid NOT NULL,
    from_class_group_id uuid,
    to_class_group_id uuid,
    from_academic_year_id uuid,
    to_academic_year_id uuid,
    change_type character varying(50) NOT NULL,
    reason text,
    changed_by uuid,
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrollments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    enrolled_at timestamp without time zone DEFAULT now() NOT NULL,
    withdrawn_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    withdraw_reason character varying(255)
);


--
-- Name: entitlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.entitlements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    user_id uuid NOT NULL,
    student_id uuid NOT NULL,
    service_type character varying(255) NOT NULL,
    service_id uuid NOT NULL,
    payment_id uuid,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    starts_at timestamp without time zone NOT NULL,
    expires_at timestamp without time zone,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false
);


--
-- Name: event_materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_materials (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    material_type character varying(50) NOT NULL,
    file_url character varying(500) NOT NULL,
    file_size bigint,
    duration_minutes integer,
    sort_order integer DEFAULT 0,
    is_public boolean DEFAULT true NOT NULL,
    uploaded_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid
);


--
-- Name: event_registrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_registrations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    status character varying(20) DEFAULT 'REGISTERED'::character varying NOT NULL,
    registered_at timestamp without time zone DEFAULT now() NOT NULL,
    cancelled_at timestamp without time zone,
    cancellation_reason character varying(500),
    attended boolean DEFAULT false,
    attended_at timestamp without time zone,
    certificate_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid
);


--
-- Name: events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    organizer_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    event_type character varying(50) NOT NULL,
    category character varying(100),
    location character varying(500),
    meeting_url character varying(500),
    starts_at timestamp without time zone NOT NULL,
    ends_at timestamp without time zone,
    duration_minutes integer DEFAULT 60,
    max_participants integer,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    thumbnail_url character varying(500),
    tags character varying(500),
    is_free boolean DEFAULT true NOT NULL,
    requires_approval boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    event_status character varying(30),
    event_type_enum character varying(50),
    timezone character varying(50),
    max_capacity integer,
    current_registrations integer DEFAULT 0,
    related_course_id character varying(36),
    related_module_id character varying(36),
    related_lesson_id character varying(36),
    recording_url character varying(500),
    recording_status character varying(20),
    provider_id character varying(36),
    presenter_name character varying(200),
    event_format character varying(30),
    difficulty character varying(30),
    target_audience character varying(100),
    prerequisites text,
    learning_outcomes text,
    agenda text,
    cancelled_at timestamp without time zone,
    cancellation_reason character varying(500),
    rescheduled_from uuid,
    access_level character varying(30)
);


--
-- Name: fieldwork_placements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fieldwork_placements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    programme_id uuid,
    organisation_name character varying(255) NOT NULL,
    placement_title character varying(255) NOT NULL,
    supervisor_name character varying(255),
    supervisor_email character varying(255),
    supervisor_phone character varying(255),
    institution_supervisor_id uuid,
    start_date date,
    end_date date,
    status character varying(255) DEFAULT 'PLANNING'::character varying NOT NULL,
    total_hours_required integer,
    total_hours_completed integer DEFAULT 0,
    objectives text,
    remarks text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: flyway_media_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_media_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: flyway_workers_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_workers_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: general_learner_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.general_learner_profiles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    institution_id uuid,
    interests text,
    bio character varying(500),
    avatar_url character varying(500),
    learning_goal character varying(200),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: grade_boundaries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grade_boundaries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    grading_scale_id uuid NOT NULL,
    grade_letter character varying(10) NOT NULL,
    min_percentage numeric(38,2) NOT NULL,
    max_percentage numeric(38,2) NOT NULL,
    description character varying(200),
    gpa_points numeric(38,2),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    grade_label character varying(255) NOT NULL,
    grade_name character varying(255),
    sort_order integer NOT NULL,
    updated_by character varying(255)
);


--
-- Name: grades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grades (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    education_level character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: grading_rubrics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grading_rubrics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid,
    title character varying(200) DEFAULT ''::character varying NOT NULL,
    description character varying(255),
    total_criteria integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    is_active boolean NOT NULL,
    name character varying(255) NOT NULL,
    total_points numeric(38,2) NOT NULL
);


--
-- Name: grading_scales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grading_scales (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    is_default boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    is_active boolean NOT NULL,
    max_value numeric(38,2),
    min_value numeric(38,2),
    scale_type character varying(255) NOT NULL
);


--
-- Name: institution_activity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_activity (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    actor_id uuid,
    actor_name character varying(255) NOT NULL,
    activity_type character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    metadata_json character varying(255),
    is_read boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: institution_audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    actor_id uuid NOT NULL,
    actor_email character varying(255) NOT NULL,
    actor_role character varying(255) NOT NULL,
    action character varying(255) NOT NULL,
    target_type character varying(255),
    target_id character varying(255),
    details character varying(255),
    ip_address character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: institution_invitations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    email character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    invited_by uuid NOT NULL,
    token character varying(255) NOT NULL,
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    accepted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: institution_memberships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_memberships (
    id bigint NOT NULL,
    user_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    department_id uuid,
    campus_id uuid,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: institution_memberships_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_memberships_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_memberships_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_memberships_id_seq OWNED BY public.institution_memberships.id;


--
-- Name: institution_services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institution_services (
    id bigint NOT NULL,
    institution_id uuid NOT NULL,
    feature_key character varying(60) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    configuration jsonb,
    enabled_at timestamp without time zone,
    enabled_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: institution_services_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institution_services_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institution_services_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.institution_services_id_seq OWNED BY public.institution_services.id;


--
-- Name: institutions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institutions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    description character varying(255),
    address character varying(255),
    city character varying(255),
    region character varying(255),
    country character varying(255) DEFAULT 'Tanzania'::character varying NOT NULL,
    phone character varying(255),
    email character varying(255),
    website character varying(255),
    logo_url character varying(255),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    region_id uuid,
    district_id uuid,
    banner_url character varying(255),
    motto character varying(255),
    founded_year integer,
    total_capacity integer,
    enabled_services character varying(255),
    metadata_json character varying(255),
    approved_at timestamp without time zone,
    approved_by character varying(255),
    status character varying(30) DEFAULT 'ACTIVE'::character varying,
    institution_id uuid,
    ward_id uuid
);


--
-- Name: integration_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.integration_status (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    integration_key character varying(50) NOT NULL,
    display_name character varying(100) NOT NULL,
    category character varying(50),
    connection_status character varying(20) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    last_success_at timestamp without time zone,
    failure_count integer DEFAULT 0 NOT NULL,
    webhook_status character varying(20),
    retry_status character varying(20),
    config_status character varying(20) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    diagnostics text,
    probe_detail text,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: learner_enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learner_enrollments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    course_id uuid NOT NULL,
    institution_id uuid,
    enrolled_at timestamp without time zone DEFAULT now() NOT NULL,
    completed_at timestamp without time zone,
    progress_percentage double precision DEFAULT 0.0,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    last_accessed_at timestamp(6) without time zone
);


--
-- Name: learner_goals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learner_goals (
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
    CONSTRAINT learner_goals_goal_type_check CHECK (((goal_type)::text = ANY (ARRAY[('PERSONAL'::character varying)::text, ('ACADEMIC'::character varying)::text, ('CAREER'::character varying)::text, ('SKILL'::character varying)::text, ('CERTIFICATION'::character varying)::text, ('PROJECT'::character varying)::text]))),
    CONSTRAINT learner_goals_status_check CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('COMPLETED'::character varying)::text, ('PAUSED'::character varying)::text, ('CANCELLED'::character varying)::text])))
);


--
-- Name: learner_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learner_notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    notification_type character varying(50) NOT NULL,
    target_type character varying(50),
    target_id uuid,
    is_read boolean DEFAULT false NOT NULL,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: learning_collaborations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_collaborations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    peer_student_id uuid,
    collaboration_type character varying(30) DEFAULT 'STUDY_GROUP'::character varying NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    course_id uuid,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean NOT NULL
);


--
-- Name: learning_evidence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_evidence (
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
-- Name: learning_goals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_goals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    student_id uuid NOT NULL,
    created_by character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    goal_type character varying(255),
    target_date date,
    completed_at timestamp without time zone,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    progress_percentage numeric(5,2) DEFAULT 0,
    related_entity_type character varying(255),
    related_entity_id uuid,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    assigned_by uuid,
    updated_by character varying(255)
);


--
-- Name: learning_modules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_modules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    course_id uuid NOT NULL,
    module_title character varying(300) NOT NULL,
    module_code character varying(50),
    description text,
    credit_hours integer,
    instructor_id uuid,
    semester character varying(20),
    academic_year character varying(20),
    status character varying(20) DEFAULT 'NOT_STARTED'::character varying NOT NULL,
    progress_percent integer DEFAULT 0,
    grade character varying(5),
    total_lessons integer,
    completed_lessons integer DEFAULT 0,
    total_assignments integer,
    completed_assignments integer DEFAULT 0,
    total_assessments integer,
    completed_assessments integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean NOT NULL
);


--
-- Name: learning_offerings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_offerings (
    id uuid NOT NULL,
    institution_id uuid,
    owner_user_id uuid NOT NULL,
    teacher_id uuid,
    course_id uuid,
    subject_id uuid,
    title character varying(300) NOT NULL,
    description text,
    thumbnail_url character varying(500),
    education_level character varying(50),
    visibility character varying(20) DEFAULT 'INSTITUTION'::character varying NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: learning_passports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_passports (
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

CREATE TABLE public.learning_profiles (
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
-- Name: learning_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    resource_type character varying(50),
    file_url character varying(500),
    subject_id uuid,
    class_group_id uuid,
    uploaded_by uuid,
    download_count integer DEFAULT 0,
    is_public boolean DEFAULT false,
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: lesson_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lesson_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    lesson_id uuid NOT NULL,
    student_id uuid NOT NULL,
    completion_percentage double precision DEFAULT 0 NOT NULL,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: lessons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lessons (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    content_text text,
    video_url character varying(255),
    file_attachments character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    is_published boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    content_type character varying(50),
    file_url character varying(500),
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL
);


--
-- Name: live_class_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_activities (
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
-- Name: live_class_attendance_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_attendance_detail (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    live_class_id uuid NOT NULL,
    user_id uuid NOT NULL,
    joined_at timestamp without time zone,
    left_at timestamp without time zone,
    total_seconds integer DEFAULT 0,
    percentage numeric(5,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_breakout_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_breakout_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    breakout_room_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_breakout_rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_breakout_rooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    live_class_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    max_participants integer DEFAULT 10,
    status character varying(20) DEFAULT 'WAITING'::character varying,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    institution_id uuid,
    updated_by character varying(255)
);


--
-- Name: live_class_chat_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    live_class_id uuid NOT NULL,
    user_id uuid NOT NULL,
    user_name character varying(255),
    message character varying(2000) NOT NULL,
    message_type character varying(255) DEFAULT 'CHAT'::character varying,
    sent_at timestamp without time zone DEFAULT now() NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    answered_at timestamp without time zone,
    answered_by uuid
);


--
-- Name: COLUMN live_class_chat_messages.answered_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.live_class_chat_messages.answered_at IS 'Set when a teacher marks a Q&A question as answered; NULL = open.';


--
-- Name: COLUMN live_class_chat_messages.answered_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.live_class_chat_messages.answered_by IS 'User id of the teacher/admin who marked the question answered.';


--
-- Name: live_class_hand_raise_queue; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_hand_raise_queue (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    live_class_id uuid NOT NULL,
    user_id uuid NOT NULL,
    raised_at timestamp without time zone DEFAULT now(),
    lowered_at timestamp without time zone,
    "position" integer NOT NULL,
    is_active boolean DEFAULT true,
    is_deleted boolean DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_issues; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_issues (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    live_class_id uuid NOT NULL,
    user_id uuid NOT NULL,
    issue_type character varying(255) NOT NULL,
    description character varying(2000),
    severity character varying(255) DEFAULT 'MEDIUM'::character varying,
    status character varying(255) DEFAULT 'OPEN'::character varying,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: live_class_participants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_participants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    live_class_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(20) DEFAULT 'LEARNER'::character varying NOT NULL,
    joined_at timestamp without time zone,
    left_at timestamp without time zone,
    duration_seconds bigint,
    connection_id character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    hand_raised_at timestamp without time zone,
    hand_raise_order integer
);


--
-- Name: live_class_poll_votes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_poll_votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    user_id uuid NOT NULL,
    option_index integer NOT NULL,
    voted_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_polls; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_polls (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    live_class_id uuid NOT NULL,
    created_by character varying(255) NOT NULL,
    question text NOT NULL,
    options jsonb NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT now(),
    closed_at timestamp without time zone,
    is_deleted boolean DEFAULT false,
    institution_id uuid,
    teacher_id uuid NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_quiz_questions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_quiz_questions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quiz_id uuid NOT NULL,
    question_text text NOT NULL,
    question_type character varying(20) DEFAULT 'MULTIPLE_CHOICE'::character varying,
    options jsonb,
    correct_answer character varying(255),
    display_order integer DEFAULT 0,
    is_deleted boolean DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_quiz_responses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_quiz_responses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quiz_id uuid NOT NULL,
    question_id uuid NOT NULL,
    user_id uuid NOT NULL,
    answer_text character varying(255),
    is_correct boolean,
    responded_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_class_quizzes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_quizzes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    live_class_id uuid NOT NULL,
    created_by character varying(255) NOT NULL,
    title character varying(300) NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    institution_id uuid,
    teacher_id uuid NOT NULL,
    updated_by character varying(255)
);


--
-- Name: live_class_responses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_responses (
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
-- Name: live_class_session_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_session_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    live_class_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_type character varying(255) NOT NULL,
    event_data character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: live_class_shared_media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_class_shared_media (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    live_class_id uuid NOT NULL,
    shared_by uuid NOT NULL,
    media_type character varying(50) NOT NULL,
    title character varying(300),
    url character varying(1000) NOT NULL,
    duration_seconds integer,
    shared_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: live_classes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.live_classes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid,
    teacher_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    scheduled_at timestamp without time zone NOT NULL,
    duration_minutes integer DEFAULT 60 NOT NULL,
    status character varying(20) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    meeting_url character varying(500),
    max_participants integer,
    recording_url character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    recording_duration_seconds integer,
    current_participants integer DEFAULT 0,
    class_group_id uuid,
    recording_enabled boolean DEFAULT false,
    session_type character varying(30) DEFAULT 'LECTURE'::character varying,
    timezone character varying(50) DEFAULT 'Africa/Dar_es_Salaam'::character varying,
    is_recurring boolean DEFAULT false,
    recurrence_pattern character varying(50),
    recurrence_end_date date,
    parent_recurring_id uuid,
    lobby_enabled boolean DEFAULT false,
    broadcast_source character varying(30) DEFAULT 'BROWSER'::character varying,
    lesson_id uuid
);


--
-- Name: COLUMN live_classes.class_group_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.live_classes.class_group_id IS 'Optional link to a class group for the live session';


--
-- Name: COLUMN live_classes.broadcast_source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.live_classes.broadcast_source IS 'Declared broadcast source: BROWSER, MOBILE, USB_CAMERA, PROFESSIONAL_CAMERA, OBS, ENCODER, STUDIO, OTHER';


--
-- Name: logbook_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.logbook_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    placement_id uuid NOT NULL,
    entry_date date NOT NULL,
    activities text NOT NULL,
    hours_worked double precision,
    skills_used text,
    challenges text,
    learning_outcomes text,
    supervisor_comments text,
    is_approved boolean DEFAULT false NOT NULL,
    approved_by uuid,
    approved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: media_assets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.media_assets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    title character varying(300) NOT NULL,
    description character varying(2000),
    media_type character varying(255) NOT NULL,
    file_url character varying(1000),
    thumbnail_url character varying(500),
    duration_seconds bigint,
    file_size_bytes bigint,
    mime_type character varying(255),
    status character varying(255) DEFAULT 'READY'::character varying,
    visibility character varying(255) DEFAULT 'INSTITUTION'::character varying,
    source_type character varying(255),
    source_id uuid,
    teacher_id uuid,
    course_id uuid,
    subject_id uuid,
    tags character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: media_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.media_files (
    id bigint NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid NOT NULL,
    file_name character varying(255) NOT NULL,
    original_name character varying(255) NOT NULL,
    content_type character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    object_key character varying(500) NOT NULL,
    bucket character varying(255) NOT NULL,
    url character varying(1000),
    thumbnail_url character varying(1000),
    metadata text,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: media_files_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.media_files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: media_files_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.media_files_id_seq OWNED BY public.media_files.id;


--
-- Name: mfa_factors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mfa_factors (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    factor_type character varying(20) DEFAULT 'TOTP'::character varying NOT NULL,
    secret_ciphertext character varying(512) NOT NULL,
    label character varying(255) DEFAULT 'Authenticator app'::character varying NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    verified_at timestamp without time zone,
    last_used_at timestamp without time zone
);


--
-- Name: mistake_lab_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mistake_lab_entries (
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

CREATE TABLE public.nfe_assessments (
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
    CONSTRAINT nfe_assessments_assessment_type_check CHECK (((assessment_type)::text = ANY (ARRAY[('QUIZ'::character varying)::text, ('EXAM'::character varying)::text, ('SURVEY'::character varying)::text, ('FEEDBACK'::character varying)::text])))
);


--
-- Name: nfe_attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nfe_attendance (
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
    CONSTRAINT nfe_attendance_status_check CHECK (((status)::text = ANY (ARRAY[('PRESENT'::character varying)::text, ('ABSENT'::character varying)::text, ('LATE'::character varying)::text, ('EXCUSED'::character varying)::text])))
);


--
-- Name: nfe_certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nfe_certificates (
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
    CONSTRAINT nfe_certificates_certificate_type_check CHECK (((certificate_type)::text = ANY (ARRAY[('COMPLETION'::character varying)::text, ('PARTICIPATION'::character varying)::text, ('ACHIEVEMENT'::character varying)::text]))),
    CONSTRAINT nfe_certificates_status_check CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('ISSUED'::character varying)::text, ('REVOKED'::character varying)::text])))
);


--
-- Name: nfe_education_providers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nfe_education_providers (
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
    CONSTRAINT nfe_education_providers_provider_type_check CHECK (((provider_type)::text = ANY (ARRAY[('ORGANIZATION'::character varying)::text, ('COMPANY'::character varying)::text, ('GOVERNMENT'::character varying)::text, ('RELIGIOUS'::character varying)::text, ('TRAINING'::character varying)::text, ('INDIVIDUAL'::character varying)::text])))
);


--
-- Name: nfe_learners; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nfe_learners (
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

CREATE TABLE public.nfe_materials (
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
    CONSTRAINT nfe_materials_material_type_check CHECK (((material_type)::text = ANY (ARRAY[('DOCUMENT'::character varying)::text, ('VIDEO'::character varying)::text, ('AUDIO'::character varying)::text, ('LINK'::character varying)::text, ('FILE'::character varying)::text])))
);


--
-- Name: nfe_programs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nfe_programs (
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
    CONSTRAINT nfe_programs_program_type_check CHECK (((program_type)::text = ANY (ARRAY[('PROGRAM'::character varying)::text, ('COURSE'::character varying)::text, ('SEMINAR'::character varying)::text, ('WORKSHOP'::character varying)::text])))
);


--
-- Name: nfe_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nfe_sessions (
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
    CONSTRAINT nfe_sessions_session_type_check CHECK (((session_type)::text = ANY (ARRAY[('LIVE'::character varying)::text, ('SEMINAR'::character varying)::text, ('WORKSHOP'::character varying)::text, ('WEBINAR'::character varying)::text]))),
    CONSTRAINT nfe_sessions_status_check CHECK (((status)::text = ANY (ARRAY[('SCHEDULED'::character varying)::text, ('LIVE'::character varying)::text, ('COMPLETED'::character varying)::text, ('CANCELLED'::character varying)::text])))
);


--
-- Name: notification_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    template_type character varying(50) NOT NULL,
    subject_template text,
    body_template text,
    is_active boolean DEFAULT true,
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: nursery_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_activities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description character varying(255),
    activity_type character varying(255) NOT NULL,
    education_level character varying(50) NOT NULL,
    subject character varying(100),
    difficulty character varying(20) DEFAULT 'EASY'::character varying,
    duration_minutes integer,
    materials_needed character varying(255),
    instructions character varying(255),
    image_url character varying(500),
    is_published boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    activity_date date NOT NULL,
    activity_name character varying(255) NOT NULL,
    age_group character varying(255),
    class_group_id uuid NOT NULL,
    conducted_by uuid NOT NULL,
    learning_objectives character varying(255),
    max_participants integer,
    status character varying(255) NOT NULL
);


--
-- Name: nursery_activity_participations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_activity_participations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    activity_id uuid NOT NULL,
    student_id uuid NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    score integer,
    feedback text,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    engagement_score integer,
    notes character varying(255),
    participated_at timestamp(6) without time zone NOT NULL,
    participation_level character varying(255),
    updated_by character varying(255)
);


--
-- Name: nursery_daily_quests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_daily_quests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    student_id uuid,
    quest_title character varying(255) NOT NULL,
    quest_description character varying(255),
    quest_type character varying(255) NOT NULL,
    reward_points integer DEFAULT 0,
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    due_date date,
    completed_date date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: nursery_feelings_checkin; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_feelings_checkin (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    feeling character varying(255) NOT NULL,
    emoji character varying(255),
    note character varying(255),
    checkin_date date DEFAULT CURRENT_DATE NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: nursery_milestones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_milestones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description character varying(255),
    milestone_type character varying(50) NOT NULL,
    target_age_months integer,
    achieved_date date,
    is_achieved boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    category character varying(255) NOT NULL,
    evidence_notes character varying(255),
    expected_age_months integer,
    milestone_name character varying(255) NOT NULL,
    observed_by uuid,
    status character varying(255) NOT NULL
);


--
-- Name: nursery_missions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_missions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    student_id uuid,
    mission_title character varying(255) NOT NULL,
    mission_description character varying(255),
    mission_type character varying(255) NOT NULL,
    reward_points integer DEFAULT 0,
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    due_date date,
    completed_date date,
    evidence_notes character varying(255),
    evidence_image_url character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: nursery_parent_learning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_parent_learning (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    activity_title character varying(255) NOT NULL,
    activity_description character varying(255),
    activity_type character varying(255) NOT NULL,
    parent_name character varying(255),
    completion_status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    completed_date date,
    notes character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: nursery_report_cards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_report_cards (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    term_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    overall_performance character varying(50),
    social_skills character varying(50),
    emotional_development character varying(255),
    physical_development character varying(255),
    cognitive_development character varying(255),
    creative_expression character varying(50),
    teacher_comments character varying(255),
    generated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    areas_for_improvement character varying(255),
    areas_of_strength character varying(255),
    general_remarks character varying(255),
    language_development character varying(255),
    prepared_by uuid NOT NULL,
    published_at timestamp(6) without time zone,
    recommendations_for_parents character varying(255),
    social_development character varying(255),
    status character varying(255) NOT NULL
);


--
-- Name: nursery_stories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_stories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    story_type character varying(255) NOT NULL,
    illustration_url character varying(255),
    audio_url character varying(255),
    duration_minutes integer,
    reading_level character varying(255),
    is_published boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: nursery_tanzania_discovery; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nursery_tanzania_discovery (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    class_group_id uuid,
    topic_title character varying(255) NOT NULL,
    topic_description character varying(255),
    category character varying(255) NOT NULL,
    region character varying(255),
    fun_facts text,
    image_url character varying(255),
    is_published boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: options; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.options (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    question_id uuid NOT NULL,
    option_text character varying(255) NOT NULL,
    is_correct boolean DEFAULT false NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: parent_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parent_messages (
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
-- Name: parent_notification_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parent_notification_preferences (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    parent_id uuid NOT NULL,
    attendance_alerts boolean DEFAULT true NOT NULL,
    grade_alerts boolean DEFAULT true NOT NULL,
    fee_alerts boolean DEFAULT true NOT NULL,
    general_announcements boolean DEFAULT true NOT NULL,
    sms_enabled boolean DEFAULT false NOT NULL,
    email_enabled boolean DEFAULT true NOT NULL,
    push_enabled boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: parent_student_links; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parent_student_links (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    parent_id uuid NOT NULL,
    student_id uuid NOT NULL,
    relationship_type character varying(255) DEFAULT 'GUARDIAN'::character varying NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: parents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid NOT NULL,
    occupation character varying(255),
    relationship_type character varying(255) DEFAULT 'GUARDIAN'::character varying NOT NULL,
    emergency_contact character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    institution_id uuid,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    parent_id uuid NOT NULL,
    student_id uuid NOT NULL,
    amount numeric(12,2) NOT NULL,
    currency character varying(10) DEFAULT 'TZS'::character varying,
    description character varying(255),
    service_type character varying(255) NOT NULL,
    service_id uuid,
    provider character varying(255),
    provider_reference character varying(255),
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    paid_at timestamp without time zone,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false
);


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(500),
    module character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid
);


--
-- Name: platform_audit_trail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.platform_audit_trail (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    actor_id uuid,
    actor_email character varying(255),
    actor_role character varying(50),
    action character varying(50) NOT NULL,
    resource_type character varying(100) NOT NULL,
    resource_id uuid,
    resource_name character varying(255),
    old_values jsonb,
    new_values jsonb,
    ip_address character varying(45),
    user_agent text,
    request_method character varying(10),
    request_url text,
    response_status integer,
    duration_ms bigint,
    context jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: platform_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.platform_config (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    config_key character varying(255) NOT NULL,
    config_value text,
    config_type character varying(255) DEFAULT 'STRING'::character varying NOT NULL,
    description character varying(255),
    category character varying(255),
    is_sensitive boolean DEFAULT false NOT NULL,
    is_public boolean DEFAULT false NOT NULL,
    last_modified_by character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid
);


--
-- Name: platform_features; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.platform_features (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    feature_key character varying(60) NOT NULL,
    name character varying(120) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    description text,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: platform_incidents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.platform_incidents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    title character varying(255) NOT NULL,
    description character varying(255),
    category character varying(255) NOT NULL,
    severity character varying(255) DEFAULT 'MEDIUM'::character varying NOT NULL,
    status character varying(255) DEFAULT 'DETECTED'::character varying NOT NULL,
    affected_service character varying(255),
    affected_entity_type character varying(255),
    affected_entity_id uuid,
    detected_at timestamp without time zone DEFAULT now() NOT NULL,
    acknowledged_at timestamp without time zone,
    contained_at timestamp without time zone,
    resolved_at timestamp without time zone,
    reviewed_at timestamp without time zone,
    assigned_to character varying(255),
    resolution_notes character varying(255),
    root_cause character varying(255),
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: platform_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.platform_notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    notification_type character varying(255) NOT NULL,
    priority character varying(255) DEFAULT 'NORMAL'::character varying NOT NULL,
    target_audience character varying(255),
    target_role character varying(255),
    target_institution_id uuid,
    sent_by character varying(255),
    sent_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone,
    read_count integer DEFAULT 0,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: platform_services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.platform_services (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    name character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    description character varying(255),
    category character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    requires_verification boolean DEFAULT false NOT NULL,
    max_seats integer,
    monthly_price numeric(12,2),
    currency character varying(10) DEFAULT 'TZS'::character varying,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: portfolio_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.portfolio_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    item_type character varying(255) NOT NULL,
    file_url character varying(255),
    competency_id uuid,
    project_id uuid,
    date_obtained date,
    sort_order integer DEFAULT 0,
    is_visible boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: portfolios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.portfolios (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    visibility character varying(255) DEFAULT 'PRIVATE'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: practical_demonstrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.practical_demonstrations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    competency_id uuid,
    project_id uuid,
    title character varying(255) NOT NULL,
    description text,
    media_urls text,
    status character varying(255) DEFAULT 'DRAFT'::character varying NOT NULL,
    reviewer_id uuid,
    review_notes text,
    score integer,
    reviewed_at timestamp without time zone,
    submitted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: primary_learning_collaborations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.primary_learning_collaborations (
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
-- Name: professional_development_goals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professional_development_goals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    goal_type character varying(30) DEFAULT 'SKILL_DEVELOPMENT'::character varying NOT NULL,
    target_date date,
    completed_date date,
    status character varying(20) DEFAULT 'NOT_STARTED'::character varying NOT NULL,
    progress_percent integer DEFAULT 0,
    evidence_url character varying(500),
    notes text,
    category character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean NOT NULL
);


--
-- Name: programmes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.programmes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255),
    description text,
    programme_type character varying(255) NOT NULL,
    education_level character varying(255),
    duration_months integer,
    credit_hours integer,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: project_milestones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_milestones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    project_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    due_date date,
    is_completed boolean DEFAULT false NOT NULL,
    completed_date timestamp without time zone,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: project_submissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_submissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    project_id uuid NOT NULL,
    milestone_id uuid,
    submission_type character varying(255) NOT NULL,
    title character varying(255),
    file_url character varying(255),
    description text,
    feedback text,
    grade character varying(255),
    submitted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: provider_memberships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_memberships (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    user_id uuid NOT NULL,
    provider_id uuid NOT NULL,
    role character varying(255) DEFAULT 'STAFF'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: provider_service_entitlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.provider_service_entitlements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    provider_id uuid NOT NULL,
    service_id uuid NOT NULL,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    seats_used integer DEFAULT 0,
    max_seats integer,
    starts_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: quest_challenges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quest_challenges (
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
-- Name: questions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.questions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    assessment_id uuid NOT NULL,
    question_type character varying(255) NOT NULL,
    question_text text NOT NULL,
    marks integer DEFAULT 1 NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: reading_adventures; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reading_adventures (
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

CREATE TABLE public.real_world_missions (
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
-- Name: recovery_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recovery_codes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    code_hash character varying(64) NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    used_at timestamp without time zone
);


--
-- Name: regions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.regions (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    institution_id uuid
);


--
-- Name: replay_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.replay_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    replay_id uuid NOT NULL,
    user_id uuid NOT NULL,
    position_seconds integer DEFAULT 0 NOT NULL,
    completed boolean DEFAULT false NOT NULL,
    updated_at timestamp without time zone,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: replays; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.replays (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    event_id uuid,
    live_session_id uuid,
    title character varying(255),
    description character varying(255),
    recording_url character varying(255),
    duration_seconds integer,
    thumbnail_url character varying(255),
    status character varying(20) DEFAULT 'PROCESSING'::character varying NOT NULL,
    file_size_bytes bigint,
    view_count integer DEFAULT 0 NOT NULL,
    last_position_seconds integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    caption_url character varying(255)
);


--
-- Name: report_cards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.report_cards (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    term_id uuid NOT NULL,
    total_marks numeric(38,2),
    average_marks numeric(5,2),
    overall_grade character varying(255),
    rank_in_class integer,
    remarks character varying(255),
    generated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    average_mark numeric(38,2),
    class_rank integer,
    gpa numeric(38,2),
    grading_scale_id uuid NOT NULL,
    published_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    total_students_in_class integer
);


--
-- Name: research_milestones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.research_milestones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    research_project_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    due_date date,
    is_completed boolean DEFAULT false NOT NULL,
    completed_date date,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: research_projects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.research_projects (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    research_question character varying(500),
    objectives text,
    supervisor_id uuid,
    status character varying(255) DEFAULT 'IDEA'::character varying NOT NULL,
    programme_id uuid,
    subject_id uuid,
    methodology text,
    start_date date,
    due_date date,
    completed_date date,
    abstract_text text,
    keywords character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: research_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.research_resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    research_project_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description character varying(255),
    resource_type character varying(50) NOT NULL,
    file_url character varying(500),
    citation text,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: resource_analytics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_analytics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    resource_id uuid NOT NULL,
    date date NOT NULL,
    view_count integer DEFAULT 0,
    download_count integer DEFAULT 0,
    unique_viewers integer DEFAULT 0,
    unique_downloaders integer DEFAULT 0,
    total_watch_time_seconds bigint DEFAULT 0,
    avg_watch_time_seconds numeric(10,2) DEFAULT 0.00,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid
);


--
-- Name: resource_annotations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_annotations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    resource_id uuid NOT NULL,
    student_id uuid NOT NULL,
    content text NOT NULL,
    position_data jsonb,
    is_private boolean DEFAULT true,
    parent_annotation_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    institution_id uuid
);


--
-- Name: resource_taggings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_taggings (
    resource_id uuid NOT NULL,
    tag_id uuid NOT NULL,
    tagged_by uuid,
    tagged_at timestamp without time zone DEFAULT now() NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    institution_id uuid
);


--
-- Name: resource_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_tags (
    name character varying(100) NOT NULL,
    description text,
    color character varying(20),
    is_system boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    updated_at timestamp without time zone,
    institution_id uuid
);


--
-- Name: resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    file_size bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    resource_type character varying(255) NOT NULL,
    lesson_id uuid,
    module_id uuid,
    course_id uuid,
    uploaded_by uuid,
    mime_type character varying(100),
    storage_url character varying(1000),
    storage_object_key character varying(500),
    storage_bucket character varying(100),
    external_url character varying(1000),
    thumbnail_url character varying(500),
    duration_seconds integer,
    page_count integer,
    width integer,
    height integer,
    visibility character varying(20) DEFAULT 'DRAFT'::character varying,
    sort_order integer,
    is_downloadable boolean,
    is_previewable boolean,
    processing_status character varying(30),
    processing_error text,
    metadata jsonb,
    tags character varying(500),
    media_id bigint,
    teacher_assignment_id uuid,
    CONSTRAINT chk_resource_external CHECK ((((resource_type)::text <> ALL ((ARRAY['EXTERNAL_LINK'::character varying, 'LINK'::character varying])::text[])) OR (external_url IS NOT NULL))),
    CONSTRAINT chk_resource_file CHECK ((((resource_type)::text = ANY ((ARRAY['EXTERNAL_LINK'::character varying, 'LINK'::character varying])::text[])) OR (storage_url IS NOT NULL) OR ((processing_status)::text = ANY ((ARRAY['PROCESSING'::character varying, 'FAILED'::character varying])::text[])))),
    CONSTRAINT resources_resource_type_check CHECK (((resource_type)::text = ANY ((ARRAY['PDF'::character varying, 'DOCUMENT'::character varying, 'PRESENTATION'::character varying, 'SPREADSHEET'::character varying, 'IMAGE'::character varying, 'VIDEO'::character varying, 'AUDIO'::character varying, 'EXTERNAL_LINK'::character varying, 'LINK'::character varying, 'LIVE_RECORDING'::character varying, 'ARCHIVE'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: revoked_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.revoked_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token_hash character varying(64) NOT NULL,
    revoked_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    institution_id uuid,
    is_deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: role_permission_mappings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permission_mappings (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    role_id uuid NOT NULL,
    permission character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    institution_id uuid,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: role_permissions_admin; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions_admin (
    role_id uuid NOT NULL,
    permission_code character varying(100) NOT NULL,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(500),
    is_system boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: rubric_criteria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rubric_criteria (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    rubric_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    max_score integer DEFAULT 10 NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    max_points numeric(38,2) NOT NULL,
    updated_by character varying(255)
);


--
-- Name: scheduled_report_runs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduled_report_runs (
    id uuid NOT NULL,
    scheduled_report_id uuid NOT NULL,
    run_at timestamp without time zone NOT NULL,
    status character varying(20) NOT NULL,
    summary text,
    error text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by text,
    updated_by text,
    institution_id uuid,
    CONSTRAINT chk_scheduled_report_run_status CHECK (((status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: scheduled_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduled_reports (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    region_id uuid,
    district_id uuid,
    report_type character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    frequency character varying(20) NOT NULL,
    recipients character varying(2000),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    next_run_at timestamp without time zone,
    last_run_at timestamp without time zone,
    run_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by text,
    updated_by text,
    institution_id uuid,
    CONSTRAINT chk_scheduled_report_frequency CHECK (((frequency)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying])::text[]))),
    CONSTRAINT chk_scheduled_report_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PAUSED'::character varying])::text[]))),
    CONSTRAINT chk_scheduled_report_type CHECK (((report_type)::text = ANY ((ARRAY['PERFORMANCE'::character varying, 'ATTENDANCE'::character varying, 'LEARNERS'::character varying, 'DATA_QUALITY'::character varying, 'GOVERNANCE'::character varying])::text[])))
);


--
-- Name: secondary_concept_bank; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.secondary_concept_bank (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    concept_name character varying(255) NOT NULL,
    concept_description text NOT NULL,
    examples text,
    related_concepts character varying(255),
    difficulty_level character varying(255) DEFAULT 'BASIC'::character varying NOT NULL,
    category character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: secondary_error_bank; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.secondary_error_bank (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    error_title character varying(255) NOT NULL,
    error_description text NOT NULL,
    incorrect_example text,
    correct_example text,
    explanation text NOT NULL,
    category character varying(255),
    frequency character varying(255) DEFAULT 'COMMON'::character varying,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: secondary_problem_bank; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.secondary_problem_bank (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    problem_title character varying(255) NOT NULL,
    problem_description text NOT NULL,
    problem_type character varying(255) NOT NULL,
    options text,
    correct_answer text NOT NULL,
    solution text,
    difficulty_level character varying(255) DEFAULT 'BASIC'::character varying NOT NULL,
    marks integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: secondary_study_planner; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.secondary_study_planner (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    subject_id uuid,
    topic_name character varying(255) NOT NULL,
    planned_date date NOT NULL,
    duration_minutes integer DEFAULT 30,
    status character varying(255) DEFAULT 'PLANNED'::character varying NOT NULL,
    priority character varying(255) DEFAULT 'MEDIUM'::character varying NOT NULL,
    notes character varying(255),
    completed_date date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: security_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.security_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid,
    event_type character varying(255) NOT NULL,
    severity character varying(255) DEFAULT 'INFO'::character varying NOT NULL,
    description character varying(255) NOT NULL,
    ip_address character varying(255),
    user_agent character varying(255),
    metadata jsonb,
    resolved boolean DEFAULT false NOT NULL,
    resolved_at timestamp without time zone,
    resolved_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    location character varying(255),
    user_email character varying(255),
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: speaking_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.speaking_activities (
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

CREATE TABLE public.student_badges (
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
-- Name: student_class_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_class_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    term_id uuid NOT NULL,
    assigned_date timestamp without time zone DEFAULT now() NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: student_class_enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_class_enrollments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    student_id uuid NOT NULL,
    class_id uuid NOT NULL,
    academic_year character varying(20) NOT NULL,
    enrolled_at timestamp without time zone DEFAULT now() NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: student_course_enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_course_enrollments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    course_id uuid NOT NULL,
    programme_id uuid,
    semester character varying(20),
    academic_year character varying(20),
    credit_hours integer,
    status character varying(20) DEFAULT 'ENROLLED'::character varying NOT NULL,
    enrolled_date date,
    completed_date date,
    grade character varying(5),
    grade_points double precision,
    instructor_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: student_portfolio_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_portfolio_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    student_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    file_url text,
    thumbnail_url character varying(255),
    portfolio_type character varying(50) NOT NULL,
    subject_name character varying(100),
    display_order integer DEFAULT 0,
    is_featured boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    content text
);


--
-- Name: student_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    student_id uuid NOT NULL,
    lesson_id uuid,
    course_id uuid,
    progress_percent numeric(5,2) DEFAULT 0,
    completed boolean DEFAULT false,
    completed_at timestamp without time zone,
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: student_projects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_projects (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    subject_id uuid,
    title character varying(255) NOT NULL,
    objective character varying(255),
    description text,
    status character varying(255) DEFAULT 'IDEATION'::character varying NOT NULL,
    instructor_id uuid,
    start_date date,
    due_date date,
    completed_date date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: student_saved_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_saved_resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    student_id uuid NOT NULL,
    resource_id uuid NOT NULL,
    saved_at timestamp without time zone DEFAULT now() NOT NULL,
    notes text,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    institution_id uuid,
    updated_at timestamp without time zone
);


--
-- Name: student_streaks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_streaks (
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
-- Name: students; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.students (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid NOT NULL,
    admission_number character varying(255) NOT NULL,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    date_of_birth date,
    gender character varying(255),
    address character varying(255),
    city character varying(255),
    region character varying(255),
    national_id character varying(255),
    blood_group character varying(255),
    medical_notes text,
    guardian_name character varying(255),
    guardian_phone character varying(255),
    guardian_email character varying(255),
    guardian_relationship character varying(255),
    enrollment_date date DEFAULT CURRENT_DATE NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: study_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.study_tasks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    task_type character varying(255) DEFAULT 'STUDY'::character varying NOT NULL,
    priority character varying(255) DEFAULT 'MEDIUM'::character varying NOT NULL,
    subject_id uuid,
    scheduled_date date,
    scheduled_time time without time zone,
    duration_minutes integer,
    is_completed boolean DEFAULT false NOT NULL,
    completed_date timestamp without time zone,
    notes text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: subject_grades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subject_grades (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    report_card_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    marks_obtained numeric(38,2),
    grade character varying(255),
    comments text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    assessed_at timestamp(6) without time zone,
    assessed_by uuid,
    created_by character varying(255),
    grade_points numeric(38,2),
    teacher_remarks character varying(255),
    updated_by character varying(255)
);


--
-- Name: subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subjects (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    education_level character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(255),
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    category character varying(100)
);


--
-- Name: support_ticket_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.support_ticket_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    ticket_id uuid NOT NULL,
    sender_id uuid NOT NULL,
    message text NOT NULL,
    is_internal boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    institution_id uuid,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: support_tickets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.support_tickets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    user_id uuid NOT NULL,
    subject character varying(255) NOT NULL,
    description text NOT NULL,
    category character varying(255) NOT NULL,
    priority character varying(255) DEFAULT 'NORMAL'::character varying NOT NULL,
    status character varying(255) DEFAULT 'OPEN'::character varying NOT NULL,
    assigned_to uuid,
    resolved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false
);


--
-- Name: system_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.system_settings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    setting_key character varying(255) NOT NULL,
    setting_value jsonb DEFAULT '{}'::jsonb NOT NULL,
    setting_type character varying(255) DEFAULT 'STRING'::character varying NOT NULL,
    description character varying(255),
    is_public boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: teacher_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teacher_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    teacher_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    academic_year character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    start_date date,
    end_date date
);


--
-- Name: teacher_class_subject_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teacher_class_subject_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    teacher_id uuid NOT NULL,
    class_group_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    academic_year_id uuid,
    institution_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_deleted boolean DEFAULT false,
    created_by character varying(255),
    updated_by character varying(255)
);


--
-- Name: teacher_qualifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teacher_qualifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    teacher_id uuid NOT NULL,
    qualification_name character varying(255) NOT NULL,
    institution_name character varying(255) NOT NULL,
    field_of_study character varying(255),
    year_obtained integer,
    certificate_url character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: teachers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teachers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid NOT NULL,
    employee_number character varying(255),
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    specialization character varying(255),
    hire_date date,
    bio text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: terms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    term_number integer NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: theses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.theses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    research_project_id uuid,
    supervisor_id uuid,
    status character varying(255) DEFAULT 'NOT_STARTED'::character varying NOT NULL,
    programme_id uuid,
    submission_date date,
    defense_date date,
    final_grade character varying(20),
    abstract_text text,
    word_count integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: transcript_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transcript_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    transcript_id uuid NOT NULL,
    subject_name character varying(255) NOT NULL,
    grade character varying(255),
    marks numeric(5,2),
    credit_hours integer,
    comments text,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    remarks character varying(255),
    score numeric(5,2),
    subject_code character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: transcripts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transcripts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    transcript_number character varying(100) NOT NULL,
    academic_year_id uuid NOT NULL,
    issued_date date DEFAULT CURRENT_DATE NOT NULL,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL,
    pdf_url character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb,
    academic_year character varying(255),
    average_score numeric(5,2),
    class_rank integer,
    generated_at timestamp(6) without time zone NOT NULL,
    issued_at timestamp(6) without time zone,
    issued_by uuid NOT NULL,
    remarks character varying(255),
    serial_number character varying(255) NOT NULL,
    term character varying(255),
    total_subjects integer
);


--
-- Name: transfer_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transfer_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    enrollment_id uuid NOT NULL,
    from_class_group_id uuid NOT NULL,
    to_class_group_id uuid NOT NULL,
    reason character varying(255),
    transferred_at timestamp without time zone DEFAULT now() NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    transferred_by uuid NOT NULL
);


--
-- Name: user_role_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_role_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp without time zone DEFAULT now() NOT NULL,
    assigned_by uuid,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by character varying(255),
    expires_at timestamp(6) without time zone,
    updated_by character varying(255)
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    middle_name character varying(255),
    last_name character varying(255) NOT NULL,
    phone character varying(255),
    role character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_email_verified boolean DEFAULT false NOT NULL,
    profile_image_url character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    learning_level character varying(255),
    region_id uuid,
    district_id uuid,
    secondary_stage character varying(255),
    form character varying(255),
    security_version bigint DEFAULT 1 NOT NULL
);


--
-- Name: verification_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.verification_codes (
    id uuid NOT NULL,
    attempts integer NOT NULL,
    code character varying(64) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used boolean NOT NULL,
    institution_id uuid,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: verification_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.verification_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    entity_type character varying(255) NOT NULL,
    entity_id uuid NOT NULL,
    verification_type character varying(255) NOT NULL,
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    submitted_by uuid,
    reviewed_by uuid,
    submitted_at timestamp without time zone DEFAULT now() NOT NULL,
    reviewed_at timestamp without time zone,
    expires_at timestamp without time zone,
    notes character varying(255),
    documents jsonb,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: video_tutorial_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.video_tutorial_progress (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    video_tutorial_id uuid NOT NULL,
    student_id uuid NOT NULL,
    position_seconds integer DEFAULT 0,
    completed boolean DEFAULT false,
    completion_percentage numeric(5,2) DEFAULT 0.00,
    last_watched_at timestamp without time zone,
    completed_at timestamp without time zone,
    watch_count integer DEFAULT 0,
    total_watch_time_seconds integer DEFAULT 0,
    last_position_seconds integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL,
    institution_id uuid
);


--
-- Name: video_tutorials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.video_tutorials (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    lesson_id uuid,
    module_id uuid,
    course_id uuid,
    created_by character varying(255) NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    duration_seconds integer,
    recording_url character varying(1000),
    recording_object_key character varying(500),
    recording_bucket character varying(100),
    thumbnail_url character varying(500),
    thumbnail_object_key character varying(500),
    caption_url character varying(1000),
    caption_object_key character varying(500),
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    processing_error text,
    processing_started_at timestamp without time zone,
    processing_completed_at timestamp without time zone,
    visibility character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    sort_order integer DEFAULT 0,
    is_downloadable boolean DEFAULT true,
    is_previewable boolean DEFAULT true,
    tags character varying(500),
    metadata jsonb,
    updated_by character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: wards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wards (
    id uuid NOT NULL,
    district_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_by text,
    updated_by text,
    institution_id uuid
);


--
-- Name: webhook_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.webhook_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    source character varying(30) NOT NULL,
    event_type character varying(100),
    verification_status character varying(20) NOT NULL,
    processing_result character varying(20) NOT NULL,
    error_details text,
    retry_count integer DEFAULT 0 NOT NULL,
    received_at timestamp without time zone DEFAULT now() NOT NULL,
    processed_at timestamp without time zone,
    institution_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean DEFAULT false NOT NULL
);


--
-- Name: worker_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    notification_type character varying(255) NOT NULL,
    target_type character varying(255) NOT NULL,
    target_id bigint,
    institution_id bigint NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: worker_notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.worker_notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: worker_notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.worker_notifications_id_seq OWNED BY public.worker_notifications.id;


--
-- Name: workshop_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workshop_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    title character varying(300) NOT NULL,
    description text,
    workshop_type character varying(30) DEFAULT 'WORKSHOP'::character varying NOT NULL,
    course_id uuid,
    scheduled_at timestamp without time zone,
    duration_minutes integer,
    location character varying(200),
    status character varying(20) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    max_participants integer,
    current_participants integer DEFAULT 0,
    materials_url character varying(500),
    instructor_id uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    is_deleted boolean NOT NULL,
    academic_year character varying(32)
);


--
-- Name: institution_memberships id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_memberships ALTER COLUMN id SET DEFAULT nextval('public.institution_memberships_id_seq'::regclass);


--
-- Name: institution_services id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_services ALTER COLUMN id SET DEFAULT nextval('public.institution_services_id_seq'::regclass);


--
-- Name: media_files id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media_files ALTER COLUMN id SET DEFAULT nextval('public.media_files_id_seq'::regclass);


--
-- Name: worker_notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_notifications ALTER COLUMN id SET DEFAULT nextval('public.worker_notifications_id_seq'::regclass);


--
-- Name: academic_records academic_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_records
    ADD CONSTRAINT academic_records_pkey PRIMARY KEY (id);


--
-- Name: academic_years academic_years_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_years
    ADD CONSTRAINT academic_years_pkey PRIMARY KEY (id);


--
-- Name: achievements achievements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.achievements
    ADD CONSTRAINT achievements_pkey PRIMARY KEY (id);


--
-- Name: activity_feeds activity_feeds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_feeds
    ADD CONSTRAINT activity_feeds_pkey PRIMARY KEY (id);


--
-- Name: admin_delegations admin_delegations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_delegations
    ADD CONSTRAINT admin_delegations_pkey PRIMARY KEY (id);


--
-- Name: announcements announcements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.announcements
    ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);


--
-- Name: answers answers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT answers_pkey PRIMARY KEY (id);


--
-- Name: assessment_answers assessment_answers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_answers
    ADD CONSTRAINT assessment_answers_pkey PRIMARY KEY (id);


--
-- Name: assessment_attempts assessment_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_attempts
    ADD CONSTRAINT assessment_attempts_pkey PRIMARY KEY (id);


--
-- Name: assessment_results assessment_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_results
    ADD CONSTRAINT assessment_results_pkey PRIMARY KEY (id);


--
-- Name: assessments assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessments
    ADD CONSTRAINT assessments_pkey PRIMARY KEY (id);


--
-- Name: assignment_submissions assignment_submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment_submissions
    ADD CONSTRAINT assignment_submissions_pkey PRIMARY KEY (id);


--
-- Name: assignments assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT assignments_pkey PRIMARY KEY (id);


--
-- Name: attempts attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attempts
    ADD CONSTRAINT attempts_pkey PRIMARY KEY (id);


--
-- Name: attendance_records attendance_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_records
    ADD CONSTRAINT attendance_records_pkey PRIMARY KEY (id);


--
-- Name: attendance_summaries attendance_summaries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_summaries
    ADD CONSTRAINT attendance_summaries_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: bookmarks bookmarks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookmarks
    ADD CONSTRAINT bookmarks_pkey PRIMARY KEY (id);


--
-- Name: bulk_attendance_sessions bulk_attendance_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bulk_attendance_sessions
    ADD CONSTRAINT bulk_attendance_sessions_pkey PRIMARY KEY (id);


--
-- Name: career_profiles career_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.career_profiles
    ADD CONSTRAINT career_profiles_pkey PRIMARY KEY (id);


--
-- Name: certificate_signatories certificate_signatories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_signatories
    ADD CONSTRAINT certificate_signatories_pkey PRIMARY KEY (id);


--
-- Name: certificate_template_signatories certificate_template_signatories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_signatories
    ADD CONSTRAINT certificate_template_signatories_pkey PRIMARY KEY (id);


--
-- Name: certificate_template_versions certificate_template_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_versions
    ADD CONSTRAINT certificate_template_versions_pkey PRIMARY KEY (id);


--
-- Name: certificate_templates certificate_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_templates
    ADD CONSTRAINT certificate_templates_pkey PRIMARY KEY (id);


--
-- Name: certificates certificates_certificate_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT certificates_certificate_number_key UNIQUE (certificate_number);


--
-- Name: certificates certificates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT certificates_pkey PRIMARY KEY (id);


--
-- Name: certificates certificates_verification_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT certificates_verification_code_key UNIQUE (verification_code);


--
-- Name: class_groups class_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_groups
    ADD CONSTRAINT class_groups_pkey PRIMARY KEY (id);


--
-- Name: class_subjects class_subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT class_subjects_pkey PRIMARY KEY (class_id, subject_id);


--
-- Name: class_timetable class_timetable_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_timetable
    ADD CONSTRAINT class_timetable_pkey PRIMARY KEY (id);


--
-- Name: classes classes_institution_id_code_academic_year_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_institution_id_code_academic_year_key UNIQUE (institution_id, code, academic_year);


--
-- Name: classes classes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_pkey PRIMARY KEY (id);


--
-- Name: competencies competencies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competencies
    ADD CONSTRAINT competencies_pkey PRIMARY KEY (id);


--
-- Name: competency_assessments competency_assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competency_assessments
    ADD CONSTRAINT competency_assessments_pkey PRIMARY KEY (id);


--
-- Name: competency_records competency_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competency_records
    ADD CONSTRAINT competency_records_pkey PRIMARY KEY (id);


--
-- Name: content_reports content_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_reports
    ADD CONSTRAINT content_reports_pkey PRIMARY KEY (id);


--
-- Name: course_lessons course_lessons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_lessons
    ADD CONSTRAINT course_lessons_pkey PRIMARY KEY (id);


--
-- Name: course_modules course_modules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_modules
    ADD CONSTRAINT course_modules_pkey PRIMARY KEY (id);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: curriculum_topics curriculum_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.curriculum_topics
    ADD CONSTRAINT curriculum_topics_pkey PRIMARY KEY (id);


--
-- Name: custom_roles custom_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_roles
    ADD CONSTRAINT custom_roles_pkey PRIMARY KEY (id);


--
-- Name: dashboard_snapshots dashboard_snapshots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_snapshots
    ADD CONSTRAINT dashboard_snapshots_pkey PRIMARY KEY (id);


--
-- Name: data_import_jobs data_import_jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_import_jobs
    ADD CONSTRAINT data_import_jobs_pkey PRIMARY KEY (id);


--
-- Name: deep_learning_contents deep_learning_contents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deep_learning_contents
    ADD CONSTRAINT deep_learning_contents_pkey PRIMARY KEY (id);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: discovery_entries discovery_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discovery_entries
    ADD CONSTRAINT discovery_entries_pkey PRIMARY KEY (id);


--
-- Name: districts districts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_pkey PRIMARY KEY (id);


--
-- Name: elmkusoma_labs elmkusoma_labs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.elmkusoma_labs
    ADD CONSTRAINT elmkusoma_labs_pkey PRIMARY KEY (id);


--
-- Name: email_verification_tokens email_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id);


--
-- Name: email_verification_tokens email_verification_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_token_key UNIQUE (token);


--
-- Name: enrollment_history enrollment_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollment_history
    ADD CONSTRAINT enrollment_history_pkey PRIMARY KEY (id);


--
-- Name: enrollments enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT enrollments_pkey PRIMARY KEY (id);


--
-- Name: entitlements entitlements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entitlements
    ADD CONSTRAINT entitlements_pkey PRIMARY KEY (id);


--
-- Name: event_materials event_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_materials
    ADD CONSTRAINT event_materials_pkey PRIMARY KEY (id);


--
-- Name: event_registrations event_registrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT event_registrations_pkey PRIMARY KEY (id);


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: fieldwork_placements fieldwork_placements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fieldwork_placements
    ADD CONSTRAINT fieldwork_placements_pkey PRIMARY KEY (id);


--
-- Name: flyway_media_history flyway_media_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_media_history
    ADD CONSTRAINT flyway_media_history_pk PRIMARY KEY (installed_rank);


--
-- Name: flyway_workers_history flyway_workers_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_workers_history
    ADD CONSTRAINT flyway_workers_history_pk PRIMARY KEY (installed_rank);


--
-- Name: general_learner_profiles general_learner_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.general_learner_profiles
    ADD CONSTRAINT general_learner_profiles_pkey PRIMARY KEY (id);


--
-- Name: general_learner_profiles general_learner_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.general_learner_profiles
    ADD CONSTRAINT general_learner_profiles_user_id_key UNIQUE (user_id);


--
-- Name: grade_boundaries grade_boundaries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grade_boundaries
    ADD CONSTRAINT grade_boundaries_pkey PRIMARY KEY (id);


--
-- Name: grades grades_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grades
    ADD CONSTRAINT grades_pkey PRIMARY KEY (id);


--
-- Name: grading_rubrics grading_rubrics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grading_rubrics
    ADD CONSTRAINT grading_rubrics_pkey PRIMARY KEY (id);


--
-- Name: grading_scales grading_scales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grading_scales
    ADD CONSTRAINT grading_scales_pkey PRIMARY KEY (id);


--
-- Name: institution_activity institution_activity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_activity
    ADD CONSTRAINT institution_activity_pkey PRIMARY KEY (id);


--
-- Name: institution_audit_log institution_audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_audit_log
    ADD CONSTRAINT institution_audit_log_pkey PRIMARY KEY (id);


--
-- Name: institution_invitations institution_invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_invitations
    ADD CONSTRAINT institution_invitations_pkey PRIMARY KEY (id);


--
-- Name: institution_memberships institution_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_memberships
    ADD CONSTRAINT institution_memberships_pkey PRIMARY KEY (id);


--
-- Name: institution_services institution_services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_services
    ADD CONSTRAINT institution_services_pkey PRIMARY KEY (id);


--
-- Name: institutions institutions_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_code_key UNIQUE (code);


--
-- Name: institutions institutions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_pkey PRIMARY KEY (id);


--
-- Name: integration_status integration_status_integration_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.integration_status
    ADD CONSTRAINT integration_status_integration_key_key UNIQUE (integration_key);


--
-- Name: integration_status integration_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.integration_status
    ADD CONSTRAINT integration_status_pkey PRIMARY KEY (id);


--
-- Name: learner_enrollments learner_enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learner_enrollments
    ADD CONSTRAINT learner_enrollments_pkey PRIMARY KEY (id);


--
-- Name: learner_goals learner_goals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learner_goals
    ADD CONSTRAINT learner_goals_pkey PRIMARY KEY (id);


--
-- Name: learner_notifications learner_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learner_notifications
    ADD CONSTRAINT learner_notifications_pkey PRIMARY KEY (id);


--
-- Name: learning_collaborations learning_collaborations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_collaborations
    ADD CONSTRAINT learning_collaborations_pkey PRIMARY KEY (id);


--
-- Name: learning_evidence learning_evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_evidence
    ADD CONSTRAINT learning_evidence_pkey PRIMARY KEY (id);


--
-- Name: learning_goals learning_goals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_goals
    ADD CONSTRAINT learning_goals_pkey PRIMARY KEY (id);


--
-- Name: learning_modules learning_modules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_modules
    ADD CONSTRAINT learning_modules_pkey PRIMARY KEY (id);


--
-- Name: learning_offerings learning_offerings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_offerings
    ADD CONSTRAINT learning_offerings_pkey PRIMARY KEY (id);


--
-- Name: learning_passports learning_passports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_passports
    ADD CONSTRAINT learning_passports_pkey PRIMARY KEY (id);


--
-- Name: learning_profiles learning_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_profiles
    ADD CONSTRAINT learning_profiles_pkey PRIMARY KEY (id);


--
-- Name: learning_resources learning_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_resources
    ADD CONSTRAINT learning_resources_pkey PRIMARY KEY (id);


--
-- Name: lesson_progress lesson_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson_progress
    ADD CONSTRAINT lesson_progress_pkey PRIMARY KEY (id);


--
-- Name: lessons lessons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT lessons_pkey PRIMARY KEY (id);


--
-- Name: live_class_activities live_class_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_activities
    ADD CONSTRAINT live_class_activities_pkey PRIMARY KEY (id);


--
-- Name: live_class_attendance_detail live_class_attendance_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_attendance_detail
    ADD CONSTRAINT live_class_attendance_detail_pkey PRIMARY KEY (id);


--
-- Name: live_class_breakout_assignments live_class_breakout_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_breakout_assignments
    ADD CONSTRAINT live_class_breakout_assignments_pkey PRIMARY KEY (id);


--
-- Name: live_class_breakout_rooms live_class_breakout_rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_breakout_rooms
    ADD CONSTRAINT live_class_breakout_rooms_pkey PRIMARY KEY (id);


--
-- Name: live_class_chat_messages live_class_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_chat_messages
    ADD CONSTRAINT live_class_chat_messages_pkey PRIMARY KEY (id);


--
-- Name: live_class_hand_raise_queue live_class_hand_raise_queue_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_hand_raise_queue
    ADD CONSTRAINT live_class_hand_raise_queue_pkey PRIMARY KEY (id);


--
-- Name: live_class_issues live_class_issues_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_issues
    ADD CONSTRAINT live_class_issues_pkey PRIMARY KEY (id);


--
-- Name: live_class_participants live_class_participants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_participants
    ADD CONSTRAINT live_class_participants_pkey PRIMARY KEY (id);


--
-- Name: live_class_poll_votes live_class_poll_votes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_poll_votes
    ADD CONSTRAINT live_class_poll_votes_pkey PRIMARY KEY (id);


--
-- Name: live_class_polls live_class_polls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_polls
    ADD CONSTRAINT live_class_polls_pkey PRIMARY KEY (id);


--
-- Name: live_class_quiz_questions live_class_quiz_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_quiz_questions
    ADD CONSTRAINT live_class_quiz_questions_pkey PRIMARY KEY (id);


--
-- Name: live_class_quiz_responses live_class_quiz_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_quiz_responses
    ADD CONSTRAINT live_class_quiz_responses_pkey PRIMARY KEY (id);


--
-- Name: live_class_quizzes live_class_quizzes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_quizzes
    ADD CONSTRAINT live_class_quizzes_pkey PRIMARY KEY (id);


--
-- Name: live_class_responses live_class_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_responses
    ADD CONSTRAINT live_class_responses_pkey PRIMARY KEY (id);


--
-- Name: live_class_session_events live_class_session_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_session_events
    ADD CONSTRAINT live_class_session_events_pkey PRIMARY KEY (id);


--
-- Name: live_class_shared_media live_class_shared_media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_shared_media
    ADD CONSTRAINT live_class_shared_media_pkey PRIMARY KEY (id);


--
-- Name: live_classes live_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_classes
    ADD CONSTRAINT live_classes_pkey PRIMARY KEY (id);


--
-- Name: logbook_entries logbook_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logbook_entries
    ADD CONSTRAINT logbook_entries_pkey PRIMARY KEY (id);


--
-- Name: media_assets media_assets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media_assets
    ADD CONSTRAINT media_assets_pkey PRIMARY KEY (id);


--
-- Name: media_files media_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media_files
    ADD CONSTRAINT media_files_pkey PRIMARY KEY (id);


--
-- Name: mfa_factors mfa_factors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mfa_factors
    ADD CONSTRAINT mfa_factors_pkey PRIMARY KEY (id);


--
-- Name: mistake_lab_entries mistake_lab_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mistake_lab_entries
    ADD CONSTRAINT mistake_lab_entries_pkey PRIMARY KEY (id);


--
-- Name: nfe_assessments nfe_assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_assessments
    ADD CONSTRAINT nfe_assessments_pkey PRIMARY KEY (id);


--
-- Name: nfe_attendance nfe_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_attendance
    ADD CONSTRAINT nfe_attendance_pkey PRIMARY KEY (id);


--
-- Name: nfe_certificates nfe_certificates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_certificates
    ADD CONSTRAINT nfe_certificates_pkey PRIMARY KEY (id);


--
-- Name: nfe_education_providers nfe_education_providers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_education_providers
    ADD CONSTRAINT nfe_education_providers_pkey PRIMARY KEY (id);


--
-- Name: nfe_learners nfe_learners_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_learners
    ADD CONSTRAINT nfe_learners_pkey PRIMARY KEY (id);


--
-- Name: nfe_materials nfe_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_materials
    ADD CONSTRAINT nfe_materials_pkey PRIMARY KEY (id);


--
-- Name: nfe_programs nfe_programs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_programs
    ADD CONSTRAINT nfe_programs_pkey PRIMARY KEY (id);


--
-- Name: nfe_sessions nfe_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nfe_sessions
    ADD CONSTRAINT nfe_sessions_pkey PRIMARY KEY (id);


--
-- Name: notification_templates notification_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);


--
-- Name: nursery_activities nursery_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_activities
    ADD CONSTRAINT nursery_activities_pkey PRIMARY KEY (id);


--
-- Name: nursery_activity_participations nursery_activity_participations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_activity_participations
    ADD CONSTRAINT nursery_activity_participations_pkey PRIMARY KEY (id);


--
-- Name: nursery_daily_quests nursery_daily_quests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_daily_quests
    ADD CONSTRAINT nursery_daily_quests_pkey PRIMARY KEY (id);


--
-- Name: nursery_feelings_checkin nursery_feelings_checkin_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_feelings_checkin
    ADD CONSTRAINT nursery_feelings_checkin_pkey PRIMARY KEY (id);


--
-- Name: nursery_milestones nursery_milestones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_milestones
    ADD CONSTRAINT nursery_milestones_pkey PRIMARY KEY (id);


--
-- Name: nursery_missions nursery_missions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_missions
    ADD CONSTRAINT nursery_missions_pkey PRIMARY KEY (id);


--
-- Name: nursery_parent_learning nursery_parent_learning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_parent_learning
    ADD CONSTRAINT nursery_parent_learning_pkey PRIMARY KEY (id);


--
-- Name: nursery_report_cards nursery_report_cards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_report_cards
    ADD CONSTRAINT nursery_report_cards_pkey PRIMARY KEY (id);


--
-- Name: nursery_stories nursery_stories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_stories
    ADD CONSTRAINT nursery_stories_pkey PRIMARY KEY (id);


--
-- Name: nursery_tanzania_discovery nursery_tanzania_discovery_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_tanzania_discovery
    ADD CONSTRAINT nursery_tanzania_discovery_pkey PRIMARY KEY (id);


--
-- Name: options options_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT options_pkey PRIMARY KEY (id);


--
-- Name: parent_messages parent_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_messages
    ADD CONSTRAINT parent_messages_pkey PRIMARY KEY (id);


--
-- Name: parent_notification_preferences parent_notification_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_notification_preferences
    ADD CONSTRAINT parent_notification_preferences_pkey PRIMARY KEY (id);


--
-- Name: parent_student_links parent_student_links_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_student_links
    ADD CONSTRAINT parent_student_links_pkey PRIMARY KEY (id);


--
-- Name: parents parents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT parents_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_code_key UNIQUE (code);


--
-- Name: permissions permissions_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_name_key UNIQUE (name);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: platform_audit_trail platform_audit_trail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_audit_trail
    ADD CONSTRAINT platform_audit_trail_pkey PRIMARY KEY (id);


--
-- Name: platform_config platform_config_config_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_config
    ADD CONSTRAINT platform_config_config_key_key UNIQUE (config_key);


--
-- Name: platform_config platform_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_config
    ADD CONSTRAINT platform_config_pkey PRIMARY KEY (id);


--
-- Name: platform_features platform_features_feature_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_features
    ADD CONSTRAINT platform_features_feature_key_key UNIQUE (feature_key);


--
-- Name: platform_features platform_features_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_features
    ADD CONSTRAINT platform_features_pkey PRIMARY KEY (id);


--
-- Name: platform_incidents platform_incidents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_incidents
    ADD CONSTRAINT platform_incidents_pkey PRIMARY KEY (id);


--
-- Name: platform_notifications platform_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_notifications
    ADD CONSTRAINT platform_notifications_pkey PRIMARY KEY (id);


--
-- Name: platform_services platform_services_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_services
    ADD CONSTRAINT platform_services_code_key UNIQUE (code);


--
-- Name: platform_services platform_services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.platform_services
    ADD CONSTRAINT platform_services_pkey PRIMARY KEY (id);


--
-- Name: portfolio_items portfolio_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.portfolio_items
    ADD CONSTRAINT portfolio_items_pkey PRIMARY KEY (id);


--
-- Name: portfolios portfolios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.portfolios
    ADD CONSTRAINT portfolios_pkey PRIMARY KEY (id);


--
-- Name: practical_demonstrations practical_demonstrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.practical_demonstrations
    ADD CONSTRAINT practical_demonstrations_pkey PRIMARY KEY (id);


--
-- Name: primary_learning_collaborations primary_learning_collaborations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.primary_learning_collaborations
    ADD CONSTRAINT primary_learning_collaborations_pkey PRIMARY KEY (id);


--
-- Name: professional_development_goals professional_development_goals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_development_goals
    ADD CONSTRAINT professional_development_goals_pkey PRIMARY KEY (id);


--
-- Name: programmes programmes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.programmes
    ADD CONSTRAINT programmes_pkey PRIMARY KEY (id);


--
-- Name: project_milestones project_milestones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_milestones
    ADD CONSTRAINT project_milestones_pkey PRIMARY KEY (id);


--
-- Name: project_submissions project_submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_submissions
    ADD CONSTRAINT project_submissions_pkey PRIMARY KEY (id);


--
-- Name: provider_memberships provider_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_memberships
    ADD CONSTRAINT provider_memberships_pkey PRIMARY KEY (id);


--
-- Name: provider_service_entitlements provider_service_entitlements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.provider_service_entitlements
    ADD CONSTRAINT provider_service_entitlements_pkey PRIMARY KEY (id);


--
-- Name: quest_challenges quest_challenges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_challenges
    ADD CONSTRAINT quest_challenges_pkey PRIMARY KEY (id);


--
-- Name: questions questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_pkey PRIMARY KEY (id);


--
-- Name: reading_adventures reading_adventures_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reading_adventures
    ADD CONSTRAINT reading_adventures_pkey PRIMARY KEY (id);


--
-- Name: real_world_missions real_world_missions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.real_world_missions
    ADD CONSTRAINT real_world_missions_pkey PRIMARY KEY (id);


--
-- Name: recovery_codes recovery_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recovery_codes
    ADD CONSTRAINT recovery_codes_pkey PRIMARY KEY (id);


--
-- Name: regions regions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.regions
    ADD CONSTRAINT regions_pkey PRIMARY KEY (id);


--
-- Name: replay_progress replay_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.replay_progress
    ADD CONSTRAINT replay_progress_pkey PRIMARY KEY (id);


--
-- Name: replays replays_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.replays
    ADD CONSTRAINT replays_pkey PRIMARY KEY (id);


--
-- Name: report_cards report_cards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT report_cards_pkey PRIMARY KEY (id);


--
-- Name: research_milestones research_milestones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.research_milestones
    ADD CONSTRAINT research_milestones_pkey PRIMARY KEY (id);


--
-- Name: research_projects research_projects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.research_projects
    ADD CONSTRAINT research_projects_pkey PRIMARY KEY (id);


--
-- Name: research_resources research_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.research_resources
    ADD CONSTRAINT research_resources_pkey PRIMARY KEY (id);


--
-- Name: resource_analytics resource_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_analytics
    ADD CONSTRAINT resource_analytics_pkey PRIMARY KEY (id);


--
-- Name: resource_annotations resource_annotations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_annotations
    ADD CONSTRAINT resource_annotations_pkey PRIMARY KEY (id);


--
-- Name: resource_taggings resource_taggings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_taggings
    ADD CONSTRAINT resource_taggings_pkey PRIMARY KEY (id);


--
-- Name: resource_tags resource_tags_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_tags
    ADD CONSTRAINT resource_tags_name_key UNIQUE (name);


--
-- Name: resource_tags resource_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_tags
    ADD CONSTRAINT resource_tags_pkey PRIMARY KEY (id);


--
-- Name: resources resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_pkey PRIMARY KEY (id);


--
-- Name: revoked_tokens revoked_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (id);


--
-- Name: role_permission_mappings role_permission_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_mappings
    ADD CONSTRAINT role_permission_mappings_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: role_permissions_admin role_permissions_admin_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions_admin
    ADD CONSTRAINT role_permissions_admin_pkey PRIMARY KEY (role_id, permission_code);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);


--
-- Name: roles roles_institution_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_institution_id_code_key UNIQUE (institution_id, code);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: rubric_criteria rubric_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rubric_criteria
    ADD CONSTRAINT rubric_criteria_pkey PRIMARY KEY (id);


--
-- Name: scheduled_report_runs scheduled_report_runs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_report_runs
    ADD CONSTRAINT scheduled_report_runs_pkey PRIMARY KEY (id);


--
-- Name: scheduled_reports scheduled_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_reports
    ADD CONSTRAINT scheduled_reports_pkey PRIMARY KEY (id);


--
-- Name: secondary_concept_bank secondary_concept_bank_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secondary_concept_bank
    ADD CONSTRAINT secondary_concept_bank_pkey PRIMARY KEY (id);


--
-- Name: secondary_error_bank secondary_error_bank_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secondary_error_bank
    ADD CONSTRAINT secondary_error_bank_pkey PRIMARY KEY (id);


--
-- Name: secondary_problem_bank secondary_problem_bank_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secondary_problem_bank
    ADD CONSTRAINT secondary_problem_bank_pkey PRIMARY KEY (id);


--
-- Name: secondary_study_planner secondary_study_planner_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.secondary_study_planner
    ADD CONSTRAINT secondary_study_planner_pkey PRIMARY KEY (id);


--
-- Name: security_events security_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT security_events_pkey PRIMARY KEY (id);


--
-- Name: speaking_activities speaking_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.speaking_activities
    ADD CONSTRAINT speaking_activities_pkey PRIMARY KEY (id);


--
-- Name: student_badges student_badges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_badges
    ADD CONSTRAINT student_badges_pkey PRIMARY KEY (id);


--
-- Name: student_class_assignments student_class_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_assignments
    ADD CONSTRAINT student_class_assignments_pkey PRIMARY KEY (id);


--
-- Name: student_class_enrollments student_class_enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_enrollments
    ADD CONSTRAINT student_class_enrollments_pkey PRIMARY KEY (id);


--
-- Name: student_class_enrollments student_class_enrollments_student_id_class_id_academic_year_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_enrollments
    ADD CONSTRAINT student_class_enrollments_student_id_class_id_academic_year_key UNIQUE (student_id, class_id, academic_year);


--
-- Name: student_course_enrollments student_course_enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_course_enrollments
    ADD CONSTRAINT student_course_enrollments_pkey PRIMARY KEY (id);


--
-- Name: student_portfolio_items student_portfolio_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_portfolio_items
    ADD CONSTRAINT student_portfolio_items_pkey PRIMARY KEY (id);


--
-- Name: student_progress student_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_progress
    ADD CONSTRAINT student_progress_pkey PRIMARY KEY (id);


--
-- Name: student_projects student_projects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_projects
    ADD CONSTRAINT student_projects_pkey PRIMARY KEY (id);


--
-- Name: student_saved_resources student_saved_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_saved_resources
    ADD CONSTRAINT student_saved_resources_pkey PRIMARY KEY (id);


--
-- Name: student_streaks student_streaks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_streaks
    ADD CONSTRAINT student_streaks_pkey PRIMARY KEY (id);


--
-- Name: students students_admission_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_admission_number_key UNIQUE (admission_number);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: study_tasks study_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.study_tasks
    ADD CONSTRAINT study_tasks_pkey PRIMARY KEY (id);


--
-- Name: subject_grades subject_grades_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subject_grades
    ADD CONSTRAINT subject_grades_pkey PRIMARY KEY (id);


--
-- Name: subjects subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT subjects_pkey PRIMARY KEY (id);


--
-- Name: support_ticket_messages support_ticket_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_ticket_messages
    ADD CONSTRAINT support_ticket_messages_pkey PRIMARY KEY (id);


--
-- Name: support_tickets support_tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (id);


--
-- Name: system_settings system_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);


--
-- Name: teacher_assignments teacher_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_assignments
    ADD CONSTRAINT teacher_assignments_pkey PRIMARY KEY (id);


--
-- Name: teacher_class_subject_assignments teacher_class_subject_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_class_subject_assignments
    ADD CONSTRAINT teacher_class_subject_assignments_pkey PRIMARY KEY (id);


--
-- Name: teacher_qualifications teacher_qualifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_qualifications
    ADD CONSTRAINT teacher_qualifications_pkey PRIMARY KEY (id);


--
-- Name: teachers teachers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT teachers_pkey PRIMARY KEY (id);


--
-- Name: terms terms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms
    ADD CONSTRAINT terms_pkey PRIMARY KEY (id);


--
-- Name: theses theses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.theses
    ADD CONSTRAINT theses_pkey PRIMARY KEY (id);


--
-- Name: transcript_entries transcript_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcript_entries
    ADD CONSTRAINT transcript_entries_pkey PRIMARY KEY (id);


--
-- Name: transcripts transcripts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcripts
    ADD CONSTRAINT transcripts_pkey PRIMARY KEY (id);


--
-- Name: transcripts transcripts_transcript_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcripts
    ADD CONSTRAINT transcripts_transcript_number_key UNIQUE (transcript_number);


--
-- Name: transfer_records transfer_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfer_records
    ADD CONSTRAINT transfer_records_pkey PRIMARY KEY (id);


--
-- Name: learning_profiles uk31adyb2icx3270ufx4brx2thv; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_profiles
    ADD CONSTRAINT uk31adyb2icx3270ufx4brx2thv UNIQUE (student_id);


--
-- Name: transcripts uk4vnqqnsw1ggt455r5yhofxgj7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcripts
    ADD CONSTRAINT uk4vnqqnsw1ggt455r5yhofxgj7 UNIQUE (serial_number);


--
-- Name: districts uk_district_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT uk_district_code UNIQUE (code);


--
-- Name: regions uk_region_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.regions
    ADD CONSTRAINT uk_region_code UNIQUE (code);


--
-- Name: regions uk_region_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.regions
    ADD CONSTRAINT uk_region_name UNIQUE (name);


--
-- Name: resource_analytics uk_resource_analytics_daily; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_analytics
    ADD CONSTRAINT uk_resource_analytics_daily UNIQUE (resource_id, date);


--
-- Name: student_saved_resources uk_student_resource; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_saved_resources
    ADD CONSTRAINT uk_student_resource UNIQUE (student_id, resource_id);


--
-- Name: video_tutorial_progress uk_video_progress_student; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorial_progress
    ADD CONSTRAINT uk_video_progress_student UNIQUE (video_tutorial_id, student_id);


--
-- Name: wards uk_ward_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wards
    ADD CONSTRAINT uk_ward_code UNIQUE (code);


--
-- Name: certificates ukfhimy9jsw510b0ga7b2wo2nw2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT ukfhimy9jsw510b0ga7b2wo2nw2 UNIQUE (serial_number);


--
-- Name: certificate_template_signatories uq_cert_tpl_sign; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_signatories
    ADD CONSTRAINT uq_cert_tpl_sign UNIQUE (template_id, signatory_id);


--
-- Name: certificate_template_versions uq_cert_tpl_ver; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_versions
    ADD CONSTRAINT uq_cert_tpl_ver UNIQUE (template_id, version);


--
-- Name: competency_assessments uq_competency_assessment_competency_assessment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competency_assessments
    ADD CONSTRAINT uq_competency_assessment_competency_assessment UNIQUE (competency_id, assessment_id);


--
-- Name: competency_records uq_competency_record_student_competency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.competency_records
    ADD CONSTRAINT uq_competency_record_student_competency UNIQUE (student_id, competency_id);


--
-- Name: custom_roles uq_custom_role_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_roles
    ADD CONSTRAINT uq_custom_role_code UNIQUE (institution_id, code);


--
-- Name: event_registrations uq_event_registration; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT uq_event_registration UNIQUE (event_id, user_id);


--
-- Name: institution_services uq_institution_service; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_services
    ADD CONSTRAINT uq_institution_service UNIQUE (institution_id, feature_key);


--
-- Name: learner_enrollments uq_learner_course; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learner_enrollments
    ADD CONSTRAINT uq_learner_course UNIQUE (user_id, course_id);


--
-- Name: live_class_participants uq_live_class_participant; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_participants
    ADD CONSTRAINT uq_live_class_participant UNIQUE (live_class_id, user_id);


--
-- Name: parent_notification_preferences uq_parent_notif_prefs; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_notification_preferences
    ADD CONSTRAINT uq_parent_notif_prefs UNIQUE (parent_id);


--
-- Name: parent_student_links uq_parent_student; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_student_links
    ADD CONSTRAINT uq_parent_student UNIQUE (parent_id, student_id);


--
-- Name: parents uq_parents_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT uq_parents_user UNIQUE (user_id, institution_id);


--
-- Name: replay_progress uq_replay_progress_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.replay_progress
    ADD CONSTRAINT uq_replay_progress_user UNIQUE (replay_id, user_id);


--
-- Name: system_settings uq_setting_key_institution; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT uq_setting_key_institution UNIQUE (institution_id, setting_key);


--
-- Name: dashboard_snapshots uq_snapshot_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_snapshots
    ADD CONSTRAINT uq_snapshot_date UNIQUE (institution_id, snapshot_date);


--
-- Name: teacher_assignments uq_teacher_assignment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_assignments
    ADD CONSTRAINT uq_teacher_assignment UNIQUE (teacher_id, class_group_id, subject_id, academic_year);


--
-- Name: teachers uq_teachers_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT uq_teachers_user UNIQUE (user_id, institution_id);


--
-- Name: bookmarks uq_user_bookmark; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookmarks
    ADD CONSTRAINT uq_user_bookmark UNIQUE (user_id, target_type, target_id);


--
-- Name: user_role_assignments user_role_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_assignments
    ADD CONSTRAINT user_role_assignments_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: verification_codes verification_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.verification_codes
    ADD CONSTRAINT verification_codes_pkey PRIMARY KEY (id);


--
-- Name: verification_records verification_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.verification_records
    ADD CONSTRAINT verification_records_pkey PRIMARY KEY (id);


--
-- Name: video_tutorial_progress video_tutorial_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorial_progress
    ADD CONSTRAINT video_tutorial_progress_pkey PRIMARY KEY (id);


--
-- Name: video_tutorials video_tutorials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorials
    ADD CONSTRAINT video_tutorials_pkey PRIMARY KEY (id);


--
-- Name: wards wards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wards
    ADD CONSTRAINT wards_pkey PRIMARY KEY (id);


--
-- Name: webhook_events webhook_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_events
    ADD CONSTRAINT webhook_events_pkey PRIMARY KEY (id);


--
-- Name: worker_notifications worker_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_notifications
    ADD CONSTRAINT worker_notifications_pkey PRIMARY KEY (id);


--
-- Name: workshop_sessions workshop_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workshop_sessions
    ADD CONSTRAINT workshop_sessions_pkey PRIMARY KEY (id);


--
-- Name: flyway_media_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_media_history_s_idx ON public.flyway_media_history USING btree (success);


--
-- Name: flyway_workers_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_workers_history_s_idx ON public.flyway_workers_history USING btree (success);


--
-- Name: idx_academic_years_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_years_institution ON public.academic_years USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_academic_years_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_years_level ON public.academic_years USING btree (education_level) WHERE (is_deleted = false);


--
-- Name: idx_achievements_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_achievements_institution_id ON public.achievements USING btree (institution_id);


--
-- Name: idx_achievements_student_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_achievements_student_id ON public.achievements USING btree (student_id);


--
-- Name: idx_achievements_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_achievements_type ON public.achievements USING btree (achievement_type);


--
-- Name: idx_activity_feed_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_activity_feed_institution ON public.activity_feeds USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_activity_feed_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_activity_feed_user ON public.activity_feeds USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_admin_delegations_authority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_delegations_authority ON public.admin_delegations USING btree (authority);


--
-- Name: idx_admin_delegations_delegate; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_delegations_delegate ON public.admin_delegations USING btree (delegate_id);


--
-- Name: idx_admin_delegations_delegator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_delegations_delegator ON public.admin_delegations USING btree (delegator_id);


--
-- Name: idx_admin_delegations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_delegations_status ON public.admin_delegations USING btree (status);


--
-- Name: idx_analytics_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_created_by ON public.resource_analytics USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_analytics_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_date ON public.resource_analytics USING btree (date);


--
-- Name: idx_analytics_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_institution ON public.resource_analytics USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_analytics_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_analytics_resource ON public.resource_analytics USING btree (resource_id);


--
-- Name: idx_annotations_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_annotations_created_by ON public.resource_annotations USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_annotations_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_annotations_institution ON public.resource_annotations USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_annotations_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_annotations_parent ON public.resource_annotations USING btree (parent_annotation_id) WHERE (is_deleted = false);


--
-- Name: idx_annotations_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_annotations_resource ON public.resource_annotations USING btree (resource_id) WHERE (is_deleted = false);


--
-- Name: idx_annotations_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_annotations_student ON public.resource_annotations USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_announcements_audience_district; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcements_audience_district ON public.announcements USING btree (audience_district_id) WHERE (is_deleted = false);


--
-- Name: idx_announcements_audience_region; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcements_audience_region ON public.announcements USING btree (audience_region_id) WHERE (is_deleted = false);


--
-- Name: idx_announcements_audience_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_announcements_audience_type ON public.announcements USING btree (audience_type) WHERE (is_deleted = false);


--
-- Name: idx_ar_programme; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ar_programme ON public.academic_records USING btree (programme_id);


--
-- Name: idx_ar_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ar_student ON public.academic_records USING btree (student_id);


--
-- Name: idx_ar_student_semester; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ar_student_semester ON public.academic_records USING btree (student_id, semester, academic_year);


--
-- Name: idx_assessment_answers_attempt; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assessment_answers_attempt ON public.assessment_answers USING btree (attempt_id);


--
-- Name: idx_assessment_attempts_assessment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assessment_attempts_assessment ON public.assessment_attempts USING btree (assessment_id);


--
-- Name: idx_assessment_attempts_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assessment_attempts_student ON public.assessment_attempts USING btree (student_id);


--
-- Name: idx_assessments_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assessments_class_group ON public.assessments USING btree (class_group_id) WHERE (is_deleted = false);


--
-- Name: idx_assessments_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assessments_lesson_id ON public.assessments USING btree (lesson_id);


--
-- Name: idx_assignment_submissions_assignment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_submissions_assignment ON public.assignment_submissions USING btree (assignment_id);


--
-- Name: idx_assignment_submissions_assignment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_submissions_assignment_status ON public.assignment_submissions USING btree (assignment_id, status);


--
-- Name: idx_assignment_submissions_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_submissions_student ON public.assignment_submissions USING btree (student_id);


--
-- Name: idx_assignments_cg; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignments_cg ON public.assignments USING btree (class_group_id) WHERE (is_deleted = false);


--
-- Name: idx_assignments_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignments_class_group ON public.teacher_assignments USING btree (class_group_id) WHERE (is_deleted = false);


--
-- Name: idx_assignments_class_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignments_class_status ON public.assignments USING btree (class_group_id, status);


--
-- Name: idx_assignments_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignments_lesson_id ON public.assignments USING btree (lesson_id);


--
-- Name: idx_assignments_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignments_subject ON public.teacher_assignments USING btree (subject_id) WHERE (is_deleted = false);


--
-- Name: idx_assignments_teacher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignments_teacher ON public.teacher_assignments USING btree (teacher_id) WHERE (is_deleted = false);


--
-- Name: idx_attempts_assessment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attempts_assessment ON public.attempts USING btree (assessment_id) WHERE (is_deleted = false);


--
-- Name: idx_attempts_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attempts_student ON public.attempts USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_attendance_class_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_class_date ON public.attendance_records USING btree (class_group_id, record_date) WHERE (is_deleted = false);


--
-- Name: idx_attendance_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_student ON public.attendance_records USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_audit_logs_archived_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_archived_at ON public.audit_logs USING btree (archived_at);


--
-- Name: idx_audit_logs_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_created ON public.audit_logs USING btree (created_at) WHERE (is_deleted = false);


--
-- Name: idx_audit_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at);


--
-- Name: idx_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_type, entity_id) WHERE (is_deleted = false);


--
-- Name: idx_audit_logs_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_institution ON public.audit_logs USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_audit_logs_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_user ON public.audit_logs USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_boundaries_scale; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_boundaries_scale ON public.grade_boundaries USING btree (grading_scale_id) WHERE (is_deleted = false);


--
-- Name: idx_cert_signatories_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cert_signatories_institution ON public.certificate_signatories USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_cert_signatories_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cert_signatories_status ON public.certificate_signatories USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_cert_templates_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cert_templates_institution ON public.certificate_templates USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_cert_tpl_sign_template; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cert_tpl_sign_template ON public.certificate_template_signatories USING btree (template_id) WHERE (is_deleted = false);


--
-- Name: idx_cert_tpl_versions_template; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cert_tpl_versions_template ON public.certificate_template_versions USING btree (template_id) WHERE (is_deleted = false);


--
-- Name: idx_certificates_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_certificates_number ON public.certificates USING btree (certificate_number) WHERE (is_deleted = false);


--
-- Name: idx_certificates_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_certificates_student ON public.certificates USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_certificates_verification; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_certificates_verification ON public.certificates USING btree (verification_code) WHERE (is_deleted = false);


--
-- Name: idx_class_groups_grade; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_groups_grade ON public.class_groups USING btree (grade_id) WHERE (is_deleted = false);


--
-- Name: idx_class_groups_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_groups_institution ON public.class_groups USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_class_groups_term; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_groups_term ON public.class_groups USING btree (term_id) WHERE (is_deleted = false);


--
-- Name: idx_class_timetable_class; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_timetable_class ON public.class_timetable USING btree (class_group_id);


--
-- Name: idx_classes_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_classes_institution ON public.classes USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_competencies_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competencies_institution ON public.competencies USING btree (institution_id);


--
-- Name: idx_competencies_programme; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competencies_programme ON public.competencies USING btree (programme_id);


--
-- Name: idx_competencies_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competencies_subject ON public.competencies USING btree (subject_id);


--
-- Name: idx_competency_assessments_assessment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competency_assessments_assessment ON public.competency_assessments USING btree (assessment_id);


--
-- Name: idx_competency_assessments_competency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competency_assessments_competency ON public.competency_assessments USING btree (competency_id);


--
-- Name: idx_competency_records_competency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competency_records_competency ON public.competency_records USING btree (competency_id);


--
-- Name: idx_competency_records_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_competency_records_student ON public.competency_records USING btree (student_id);


--
-- Name: idx_content_reports_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_reports_entity ON public.content_reports USING btree (entity_type, entity_id);


--
-- Name: idx_content_reports_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_reports_status ON public.content_reports USING btree (status);


--
-- Name: idx_course_lessons_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_lessons_institution ON public.course_lessons USING btree (institution_id);


--
-- Name: idx_course_modules_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_modules_institution ON public.course_modules USING btree (institution_id);


--
-- Name: idx_courses_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courses_institution ON public.courses USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_courses_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courses_subject ON public.courses USING btree (subject_id) WHERE (is_deleted = false);


--
-- Name: idx_cp_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cp_student ON public.career_profiles USING btree (student_id);


--
-- Name: idx_departments_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_departments_active ON public.departments USING btree (is_active) WHERE (is_deleted = false);


--
-- Name: idx_departments_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_departments_institution ON public.departments USING btree (institution_id);


--
-- Name: idx_districts_region; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_districts_region ON public.districts USING btree (region_id) WHERE (is_deleted = false);


--
-- Name: idx_dlc_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dlc_institution ON public.deep_learning_contents USING btree (institution_id);


--
-- Name: idx_dlc_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dlc_student ON public.deep_learning_contents USING btree (student_id);


--
-- Name: idx_email_verification_tokens_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_verification_tokens_token ON public.email_verification_tokens USING btree (token) WHERE (used = false);


--
-- Name: idx_email_verification_tokens_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_email_verification_tokens_user ON public.email_verification_tokens USING btree (user_id);


--
-- Name: idx_enrollment_history_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrollment_history_student ON public.enrollment_history USING btree (student_id);


--
-- Name: idx_enrollments_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrollments_class_group ON public.enrollments USING btree (class_group_id) WHERE (is_deleted = false);


--
-- Name: idx_enrollments_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrollments_student ON public.enrollments USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_entitlements_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entitlements_institution_id ON public.entitlements USING btree (institution_id);


--
-- Name: idx_entitlements_service; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entitlements_service ON public.entitlements USING btree (service_type, service_id);


--
-- Name: idx_entitlements_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entitlements_status ON public.entitlements USING btree (status);


--
-- Name: idx_entitlements_student_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entitlements_student_id ON public.entitlements USING btree (student_id);


--
-- Name: idx_entitlements_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entitlements_user_id ON public.entitlements USING btree (user_id);


--
-- Name: idx_event_materials_event; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_materials_event ON public.event_materials USING btree (event_id) WHERE (is_deleted = false);


--
-- Name: idx_event_reg_event; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_reg_event ON public.event_registrations USING btree (event_id) WHERE (is_deleted = false);


--
-- Name: idx_event_reg_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_reg_user ON public.event_registrations USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_events_access_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_events_access_level ON public.events USING btree (access_level) WHERE (is_deleted = false);


--
-- Name: idx_events_event_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_events_event_status ON public.events USING btree (event_status) WHERE (is_deleted = false);


--
-- Name: idx_events_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_events_institution ON public.events USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_events_starts_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_events_starts_at ON public.events USING btree (starts_at) WHERE (is_deleted = false);


--
-- Name: idx_events_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_events_status ON public.events USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_events_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_events_type ON public.events USING btree (event_type) WHERE (is_deleted = false);


--
-- Name: idx_fieldwork_placements_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fieldwork_placements_institution ON public.fieldwork_placements USING btree (institution_id);


--
-- Name: idx_fieldwork_placements_programme; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fieldwork_placements_programme ON public.fieldwork_placements USING btree (programme_id);


--
-- Name: idx_fieldwork_placements_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fieldwork_placements_status ON public.fieldwork_placements USING btree (status);


--
-- Name: idx_fieldwork_placements_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fieldwork_placements_student ON public.fieldwork_placements USING btree (student_id);


--
-- Name: idx_fieldwork_placements_supervisor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fieldwork_placements_supervisor ON public.fieldwork_placements USING btree (institution_supervisor_id);


--
-- Name: idx_grades_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grades_institution ON public.grades USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_grades_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grades_level ON public.grades USING btree (education_level) WHERE (is_deleted = false);


--
-- Name: idx_grading_scales_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grading_scales_institution ON public.grading_scales USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_ia_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ia_created ON public.institution_activity USING btree (created_at);


--
-- Name: idx_ia_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ia_institution ON public.institution_activity USING btree (institution_id);


--
-- Name: idx_ia_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ia_type ON public.institution_activity USING btree (activity_type);


--
-- Name: idx_ial_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ial_action ON public.institution_audit_log USING btree (action);


--
-- Name: idx_ial_actor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ial_actor ON public.institution_audit_log USING btree (actor_id);


--
-- Name: idx_ial_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ial_created ON public.institution_audit_log USING btree (created_at);


--
-- Name: idx_ial_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ial_institution ON public.institution_audit_log USING btree (institution_id);


--
-- Name: idx_incidents_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_category ON public.platform_incidents USING btree (category);


--
-- Name: idx_incidents_detected; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_detected ON public.platform_incidents USING btree (detected_at);


--
-- Name: idx_incidents_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_severity ON public.platform_incidents USING btree (severity);


--
-- Name: idx_incidents_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_status ON public.platform_incidents USING btree (status);


--
-- Name: idx_institution_memberships_campus; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_memberships_campus ON public.institution_memberships USING btree (campus_id) WHERE (is_deleted = false);


--
-- Name: idx_institution_memberships_department; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_memberships_department ON public.institution_memberships USING btree (department_id) WHERE (is_deleted = false);


--
-- Name: idx_institution_memberships_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_memberships_institution ON public.institution_memberships USING btree (institution_id) WHERE (is_active = true);


--
-- Name: idx_institution_memberships_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_institution_memberships_unique ON public.institution_memberships USING btree (user_id, institution_id) WHERE (is_active = true);


--
-- Name: idx_institution_memberships_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_memberships_user ON public.institution_memberships USING btree (user_id) WHERE (is_active = true);


--
-- Name: idx_institution_services_feature; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_services_feature ON public.institution_services USING btree (feature_key) WHERE (is_deleted = false);


--
-- Name: idx_institution_services_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institution_services_institution ON public.institution_services USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_institutions_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institutions_code ON public.institutions USING btree (code) WHERE (is_deleted = false);


--
-- Name: idx_institutions_district; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institutions_district ON public.institutions USING btree (district_id) WHERE (is_deleted = false);


--
-- Name: idx_institutions_region; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institutions_region ON public.institutions USING btree (region_id) WHERE (is_deleted = false);


--
-- Name: idx_institutions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institutions_status ON public.institutions USING btree (status);


--
-- Name: idx_institutions_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institutions_type ON public.institutions USING btree (type) WHERE (is_deleted = false);


--
-- Name: idx_institutions_ward; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institutions_ward ON public.institutions USING btree (ward_id) WHERE (is_deleted = false);


--
-- Name: idx_inv_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inv_email ON public.institution_invitations USING btree (email);


--
-- Name: idx_inv_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inv_institution ON public.institution_invitations USING btree (institution_id);


--
-- Name: idx_inv_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inv_token ON public.institution_invitations USING btree (token);


--
-- Name: idx_lc_chat_class; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_chat_class ON public.live_class_chat_messages USING btree (live_class_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_chat_sent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_chat_sent ON public.live_class_chat_messages USING btree (live_class_id, sent_at) WHERE (is_deleted = false);


--
-- Name: idx_lc_chat_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_chat_user ON public.live_class_chat_messages USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_event_class; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_event_class ON public.live_class_session_events USING btree (live_class_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_event_type ON public.live_class_session_events USING btree (live_class_id, event_type) WHERE (is_deleted = false);


--
-- Name: idx_lc_event_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_event_user ON public.live_class_session_events USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_institution ON public.learning_collaborations USING btree (institution_id);


--
-- Name: idx_lc_issue_class; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_issue_class ON public.live_class_issues USING btree (live_class_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_issue_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_issue_status ON public.live_class_issues USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_lc_participant_class; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_participant_class ON public.live_class_participants USING btree (live_class_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_participant_connection; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_participant_connection ON public.live_class_participants USING btree (connection_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_participant_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_participant_user ON public.live_class_participants USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_lc_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lc_student ON public.learning_collaborations USING btree (student_id);


--
-- Name: idx_learning_goals_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_goals_institution_id ON public.learning_goals USING btree (institution_id);


--
-- Name: idx_learning_goals_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_goals_status ON public.learning_goals USING btree (status);


--
-- Name: idx_learning_goals_student_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_goals_student_id ON public.learning_goals USING btree (student_id);


--
-- Name: idx_learning_offerings_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_offerings_course ON public.learning_offerings USING btree (course_id);


--
-- Name: idx_learning_offerings_discover; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_offerings_discover ON public.learning_offerings USING btree (status, visibility, education_level);


--
-- Name: idx_learning_offerings_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_offerings_institution ON public.learning_offerings USING btree (institution_id);


--
-- Name: idx_learning_offerings_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_offerings_owner ON public.learning_offerings USING btree (owner_user_id);


--
-- Name: idx_learning_offerings_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_offerings_subject ON public.learning_offerings USING btree (subject_id);


--
-- Name: idx_learning_resources_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_learning_resources_subject ON public.learning_resources USING btree (subject_id);


--
-- Name: idx_lessons_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lessons_class_group ON public.lessons USING btree (class_group_id) WHERE (is_deleted = false);


--
-- Name: idx_lessons_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lessons_status ON public.lessons USING btree (status);


--
-- Name: idx_lessons_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lessons_subject ON public.lessons USING btree (subject_id) WHERE (is_deleted = false);


--
-- Name: idx_live_classes_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_live_classes_class_group ON public.live_classes USING btree (class_group_id);


--
-- Name: idx_live_classes_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_live_classes_institution ON public.live_classes USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_live_classes_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_live_classes_lesson_id ON public.live_classes USING btree (lesson_id);


--
-- Name: idx_live_classes_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_live_classes_status ON public.live_classes USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_live_classes_teacher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_live_classes_teacher ON public.live_classes USING btree (teacher_id) WHERE (is_deleted = false);


--
-- Name: idx_lm_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lm_institution ON public.learning_modules USING btree (institution_id);


--
-- Name: idx_lm_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lm_student ON public.learning_modules USING btree (student_id);


--
-- Name: idx_logbook_entries_approved; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_logbook_entries_approved ON public.logbook_entries USING btree (is_approved);


--
-- Name: idx_logbook_entries_entry_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_logbook_entries_entry_date ON public.logbook_entries USING btree (entry_date);


--
-- Name: idx_logbook_entries_placement; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_logbook_entries_placement ON public.logbook_entries USING btree (placement_id);


--
-- Name: idx_media_files_content_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_content_type ON public.media_files USING btree (content_type);


--
-- Name: idx_media_files_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_created_at ON public.media_files USING btree (created_at DESC);


--
-- Name: idx_media_files_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_institution_id ON public.media_files USING btree (institution_id);


--
-- Name: idx_media_files_is_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_is_deleted ON public.media_files USING btree (is_deleted);


--
-- Name: idx_media_files_object_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_object_key ON public.media_files USING btree (object_key);


--
-- Name: idx_media_files_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_user_id ON public.media_files USING btree (user_id);


--
-- Name: idx_media_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_institution ON public.media_assets USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_media_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_source ON public.media_assets USING btree (source_type, source_id) WHERE (is_deleted = false);


--
-- Name: idx_media_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_status ON public.media_assets USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_media_teacher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_teacher ON public.media_assets USING btree (teacher_id) WHERE (is_deleted = false);


--
-- Name: idx_media_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_type ON public.media_assets USING btree (media_type) WHERE (is_deleted = false);


--
-- Name: idx_mfa_factors_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mfa_factors_user_id ON public.mfa_factors USING btree (user_id);


--
-- Name: idx_milestones_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_milestones_student ON public.nursery_milestones USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_nap_activity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nap_activity ON public.nursery_activity_participations USING btree (activity_id) WHERE (is_deleted = false);


--
-- Name: idx_nap_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nap_student ON public.nursery_activity_participations USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_nrc_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nrc_student ON public.nursery_report_cards USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_nursery_activities_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nursery_activities_institution ON public.nursery_activities USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_nursery_activities_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nursery_activities_type ON public.nursery_activities USING btree (activity_type) WHERE (is_deleted = false);


--
-- Name: idx_parent_student_links_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parent_student_links_parent ON public.parent_student_links USING btree (parent_id) WHERE (is_deleted = false);


--
-- Name: idx_parent_student_links_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parent_student_links_student ON public.parent_student_links USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_parents_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parents_institution ON public.parents USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_parents_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parents_user ON public.parents USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_password_reset_tokens_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_token ON public.password_reset_tokens USING btree (token) WHERE (used = false);


--
-- Name: idx_password_reset_tokens_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_user ON public.password_reset_tokens USING btree (user_id);


--
-- Name: idx_payments_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_institution_id ON public.payments USING btree (institution_id);


--
-- Name: idx_payments_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_parent_id ON public.payments USING btree (parent_id);


--
-- Name: idx_payments_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_status ON public.payments USING btree (status);


--
-- Name: idx_payments_student_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_student_id ON public.payments USING btree (student_id);


--
-- Name: idx_pdg_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pdg_institution ON public.professional_development_goals USING btree (institution_id);


--
-- Name: idx_pdg_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pdg_student ON public.professional_development_goals USING btree (student_id);


--
-- Name: idx_platform_audit_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_audit_action ON public.platform_audit_trail USING btree (action);


--
-- Name: idx_platform_audit_actor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_audit_actor ON public.platform_audit_trail USING btree (actor_id);


--
-- Name: idx_platform_audit_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_audit_created ON public.platform_audit_trail USING btree (created_at);


--
-- Name: idx_platform_audit_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_audit_resource ON public.platform_audit_trail USING btree (resource_type, resource_id);


--
-- Name: idx_platform_config_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_config_category ON public.platform_config USING btree (category);


--
-- Name: idx_platform_config_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_config_institution ON public.platform_config USING btree (institution_id);


--
-- Name: idx_platform_config_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_config_institution_id ON public.platform_config USING btree (institution_id);


--
-- Name: idx_platform_config_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_config_key ON public.platform_config USING btree (config_key);


--
-- Name: idx_platform_notifications_audience; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_notifications_audience ON public.platform_notifications USING btree (target_audience);


--
-- Name: idx_platform_notifications_sent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_notifications_sent ON public.platform_notifications USING btree (sent_at);


--
-- Name: idx_platform_notifications_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_notifications_type ON public.platform_notifications USING btree (notification_type);


--
-- Name: idx_platform_services_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_services_active ON public.platform_services USING btree (is_active);


--
-- Name: idx_platform_services_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_platform_services_category ON public.platform_services USING btree (category);


--
-- Name: idx_portfolio_items_competency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolio_items_competency ON public.portfolio_items USING btree (competency_id);


--
-- Name: idx_portfolio_items_item_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolio_items_item_type ON public.portfolio_items USING btree (item_type);


--
-- Name: idx_portfolio_items_portfolio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolio_items_portfolio ON public.portfolio_items USING btree (portfolio_id);


--
-- Name: idx_portfolio_items_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolio_items_project ON public.portfolio_items USING btree (project_id);


--
-- Name: idx_portfolios_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolios_institution ON public.portfolios USING btree (institution_id);


--
-- Name: idx_portfolios_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolios_student ON public.portfolios USING btree (student_id);


--
-- Name: idx_portfolios_visibility; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portfolios_visibility ON public.portfolios USING btree (visibility);


--
-- Name: idx_practical_demonstrations_competency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_practical_demonstrations_competency ON public.practical_demonstrations USING btree (competency_id);


--
-- Name: idx_practical_demonstrations_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_practical_demonstrations_institution ON public.practical_demonstrations USING btree (institution_id);


--
-- Name: idx_practical_demonstrations_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_practical_demonstrations_project ON public.practical_demonstrations USING btree (project_id);


--
-- Name: idx_practical_demonstrations_reviewer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_practical_demonstrations_reviewer ON public.practical_demonstrations USING btree (reviewer_id);


--
-- Name: idx_practical_demonstrations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_practical_demonstrations_status ON public.practical_demonstrations USING btree (status);


--
-- Name: idx_practical_demonstrations_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_practical_demonstrations_student ON public.practical_demonstrations USING btree (student_id);


--
-- Name: idx_programmes_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_programmes_active ON public.programmes USING btree (is_active) WHERE (is_deleted = false);


--
-- Name: idx_programmes_education_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_programmes_education_level ON public.programmes USING btree (education_level);


--
-- Name: idx_programmes_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_programmes_institution ON public.programmes USING btree (institution_id);


--
-- Name: idx_programmes_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_programmes_type ON public.programmes USING btree (programme_type);


--
-- Name: idx_progress_lesson; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_progress_lesson ON public.lesson_progress USING btree (lesson_id) WHERE (is_deleted = false);


--
-- Name: idx_progress_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_progress_student ON public.lesson_progress USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_project_milestones_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_milestones_project ON public.project_milestones USING btree (project_id);


--
-- Name: idx_project_submissions_milestone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_submissions_milestone ON public.project_submissions USING btree (milestone_id);


--
-- Name: idx_project_submissions_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_submissions_project ON public.project_submissions USING btree (project_id);


--
-- Name: idx_provider_memberships_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_provider_memberships_provider ON public.provider_memberships USING btree (provider_id);


--
-- Name: idx_provider_memberships_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_provider_memberships_user ON public.provider_memberships USING btree (user_id);


--
-- Name: idx_pse_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pse_provider ON public.provider_service_entitlements USING btree (provider_id);


--
-- Name: idx_pse_service; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pse_service ON public.provider_service_entitlements USING btree (service_id);


--
-- Name: idx_pse_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pse_status ON public.provider_service_entitlements USING btree (status);


--
-- Name: idx_qualifications_teacher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qualifications_teacher ON public.teacher_qualifications USING btree (teacher_id) WHERE (is_deleted = false);


--
-- Name: idx_questions_assessment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_questions_assessment ON public.questions USING btree (assessment_id) WHERE (is_deleted = false);


--
-- Name: idx_recovery_codes_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recovery_codes_user_id ON public.recovery_codes USING btree (user_id);


--
-- Name: idx_replay_progress_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_replay_progress_user ON public.replay_progress USING btree (user_id);


--
-- Name: idx_replays_event; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_replays_event ON public.replays USING btree (event_id) WHERE (is_deleted = false);


--
-- Name: idx_replays_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_replays_institution ON public.replays USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_replays_live_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_replays_live_session ON public.replays USING btree (live_session_id);


--
-- Name: idx_replays_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_replays_status ON public.replays USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_report_cards_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_report_cards_class_group ON public.report_cards USING btree (class_group_id);


--
-- Name: idx_report_cards_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_report_cards_student ON public.report_cards USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_report_cards_student_term; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_report_cards_student_term ON public.report_cards USING btree (student_id, term_id);


--
-- Name: idx_research_milestones_due_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_milestones_due_date ON public.research_milestones USING btree (due_date);


--
-- Name: idx_research_milestones_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_milestones_project ON public.research_milestones USING btree (research_project_id);


--
-- Name: idx_research_projects_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_projects_institution ON public.research_projects USING btree (institution_id);


--
-- Name: idx_research_projects_programme; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_projects_programme ON public.research_projects USING btree (programme_id);


--
-- Name: idx_research_projects_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_projects_status ON public.research_projects USING btree (status);


--
-- Name: idx_research_projects_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_projects_student ON public.research_projects USING btree (student_id);


--
-- Name: idx_research_projects_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_projects_subject ON public.research_projects USING btree (subject_id);


--
-- Name: idx_research_projects_supervisor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_projects_supervisor ON public.research_projects USING btree (supervisor_id);


--
-- Name: idx_research_resources_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_resources_project ON public.research_resources USING btree (research_project_id);


--
-- Name: idx_research_resources_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_research_resources_type ON public.research_resources USING btree (resource_type);


--
-- Name: idx_resource_taggings_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resource_taggings_created_by ON public.resource_taggings USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_resource_taggings_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resource_taggings_institution ON public.resource_taggings USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_resource_taggings_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resource_taggings_resource ON public.resource_taggings USING btree (resource_id);


--
-- Name: idx_resource_taggings_tag; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resource_taggings_tag ON public.resource_taggings USING btree (tag_id);


--
-- Name: idx_resource_tags_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resource_tags_created_by ON public.resource_tags USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_resource_tags_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resource_tags_institution ON public.resource_tags USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_resources_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_course ON public.resources USING btree (course_id) WHERE (is_deleted = false);


--
-- Name: idx_resources_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_institution ON public.resources USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_resources_lesson; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_lesson ON public.resources USING btree (lesson_id) WHERE (is_deleted = false);


--
-- Name: idx_resources_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_lesson_id ON public.resources USING btree (lesson_id);


--
-- Name: idx_resources_media_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_media_id ON public.resources USING btree (media_id);


--
-- Name: idx_resources_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_module ON public.resources USING btree (module_id) WHERE (is_deleted = false);


--
-- Name: idx_resources_teacher_assignment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_teacher_assignment ON public.resources USING btree (teacher_assignment_id) WHERE (is_deleted = false);


--
-- Name: idx_resources_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_type ON public.resources USING btree (resource_type) WHERE (is_deleted = false);


--
-- Name: idx_resources_type_visibility; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_type_visibility ON public.resources USING btree (resource_type, visibility) WHERE (is_deleted = false);


--
-- Name: idx_resources_uploaded_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_uploaded_by ON public.resources USING btree (uploaded_by) WHERE (is_deleted = false);


--
-- Name: idx_resources_visibility; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resources_visibility ON public.resources USING btree (visibility) WHERE (is_deleted = false);


--
-- Name: idx_revoked_tokens_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_revoked_tokens_expires ON public.revoked_tokens USING btree (expires_at);


--
-- Name: idx_revoked_tokens_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_revoked_tokens_hash ON public.revoked_tokens USING btree (token_hash);


--
-- Name: idx_role_permissions_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_permissions_role ON public.role_permissions USING btree (role_id);


--
-- Name: idx_roles_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_roles_institution ON public.roles USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_saved_resources_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_saved_resources_created_by ON public.student_saved_resources USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_saved_resources_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_saved_resources_institution ON public.student_saved_resources USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_saved_resources_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_saved_resources_resource ON public.student_saved_resources USING btree (resource_id);


--
-- Name: idx_saved_resources_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_saved_resources_student ON public.student_saved_resources USING btree (student_id);


--
-- Name: idx_sca_class_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sca_class_group ON public.student_class_assignments USING btree (class_group_id) WHERE (is_deleted = false);


--
-- Name: idx_sca_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sca_student ON public.student_class_assignments USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_sca_term; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sca_term ON public.student_class_assignments USING btree (term_id) WHERE (is_deleted = false);


--
-- Name: idx_sce_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sce_course ON public.student_course_enrollments USING btree (course_id);


--
-- Name: idx_sce_programme; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sce_programme ON public.student_course_enrollments USING btree (programme_id);


--
-- Name: idx_sce_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sce_status ON public.student_course_enrollments USING btree (status);


--
-- Name: idx_sce_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sce_student ON public.student_course_enrollments USING btree (student_id);


--
-- Name: idx_sce_student_semester; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sce_student_semester ON public.student_course_enrollments USING btree (student_id, semester, academic_year);


--
-- Name: idx_scheduled_report_runs_report; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_report_runs_report ON public.scheduled_report_runs USING btree (scheduled_report_id, run_at DESC);


--
-- Name: idx_scheduled_reports_due; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_reports_due ON public.scheduled_reports USING btree (status, next_run_at) WHERE (is_deleted = false);


--
-- Name: idx_scheduled_reports_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scheduled_reports_user ON public.scheduled_reports USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_security_events_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_institution ON public.security_events USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_security_events_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_severity ON public.security_events USING btree (severity) WHERE (is_deleted = false);


--
-- Name: idx_security_events_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_type ON public.security_events USING btree (event_type) WHERE (is_deleted = false);


--
-- Name: idx_settings_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settings_institution ON public.system_settings USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_student_portfolio_items_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_portfolio_items_institution ON public.student_portfolio_items USING btree (institution_id);


--
-- Name: idx_student_portfolio_items_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_portfolio_items_student ON public.student_portfolio_items USING btree (student_id);


--
-- Name: idx_student_portfolio_items_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_portfolio_items_type ON public.student_portfolio_items USING btree (portfolio_type);


--
-- Name: idx_student_progress_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_progress_student ON public.student_progress USING btree (student_id);


--
-- Name: idx_student_projects_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_projects_institution ON public.student_projects USING btree (institution_id);


--
-- Name: idx_student_projects_instructor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_projects_instructor ON public.student_projects USING btree (instructor_id);


--
-- Name: idx_student_projects_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_projects_status ON public.student_projects USING btree (status);


--
-- Name: idx_student_projects_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_projects_student ON public.student_projects USING btree (student_id);


--
-- Name: idx_student_projects_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_projects_subject ON public.student_projects USING btree (subject_id);


--
-- Name: idx_students_admission; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_students_admission ON public.students USING btree (admission_number) WHERE (is_deleted = false);


--
-- Name: idx_students_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_students_institution ON public.students USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_students_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_students_status ON public.students USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_students_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_students_user ON public.students USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_study_tasks_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_institution ON public.study_tasks USING btree (institution_id);


--
-- Name: idx_study_tasks_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_priority ON public.study_tasks USING btree (priority);


--
-- Name: idx_study_tasks_scheduled_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_scheduled_date ON public.study_tasks USING btree (scheduled_date);


--
-- Name: idx_study_tasks_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_student ON public.study_tasks USING btree (student_id);


--
-- Name: idx_study_tasks_student_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_student_date ON public.study_tasks USING btree (student_id, scheduled_date);


--
-- Name: idx_study_tasks_subject; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_subject ON public.study_tasks USING btree (subject_id);


--
-- Name: idx_study_tasks_task_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_study_tasks_task_type ON public.study_tasks USING btree (task_type);


--
-- Name: idx_subject_grades_report; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subject_grades_report ON public.subject_grades USING btree (report_card_id) WHERE (is_deleted = false);


--
-- Name: idx_subjects_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subjects_institution ON public.subjects USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_subjects_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subjects_level ON public.subjects USING btree (education_level) WHERE (is_deleted = false);


--
-- Name: idx_submissions_assignment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_submissions_assignment ON public.assignment_submissions USING btree (assignment_id) WHERE (is_deleted = false);


--
-- Name: idx_summary_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_summary_student ON public.attendance_summaries USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_support_ticket_messages_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_support_ticket_messages_institution ON public.support_ticket_messages USING btree (institution_id);


--
-- Name: idx_support_ticket_messages_ticket_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_support_ticket_messages_ticket_id ON public.support_ticket_messages USING btree (ticket_id);


--
-- Name: idx_support_tickets_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_support_tickets_institution_id ON public.support_tickets USING btree (institution_id);


--
-- Name: idx_support_tickets_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_support_tickets_status ON public.support_tickets USING btree (status);


--
-- Name: idx_support_tickets_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_support_tickets_user_id ON public.support_tickets USING btree (user_id);


--
-- Name: idx_system_settings_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_system_settings_key ON public.system_settings USING btree (setting_key, institution_id);


--
-- Name: idx_tcsa_class; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tcsa_class ON public.teacher_class_subject_assignments USING btree (class_group_id);


--
-- Name: idx_tcsa_teacher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tcsa_teacher ON public.teacher_class_subject_assignments USING btree (teacher_id);


--
-- Name: idx_teacher_assignments_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_teacher_assignments_status ON public.teacher_assignments USING btree (teacher_id, status) WHERE (is_deleted = false);


--
-- Name: idx_teacher_qualifications_teacher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_teacher_qualifications_teacher ON public.teacher_qualifications USING btree (teacher_id);


--
-- Name: idx_teachers_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_teachers_institution ON public.teachers USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_teachers_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_teachers_status ON public.teachers USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_teachers_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_teachers_user ON public.teachers USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_terms_academic_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_terms_academic_year ON public.terms USING btree (academic_year_id) WHERE (is_deleted = false);


--
-- Name: idx_theses_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_theses_institution ON public.theses USING btree (institution_id);


--
-- Name: idx_theses_programme; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_theses_programme ON public.theses USING btree (programme_id);


--
-- Name: idx_theses_research_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_theses_research_project ON public.theses USING btree (research_project_id);


--
-- Name: idx_theses_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_theses_status ON public.theses USING btree (status);


--
-- Name: idx_theses_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_theses_student ON public.theses USING btree (student_id);


--
-- Name: idx_theses_supervisor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_theses_supervisor ON public.theses USING btree (supervisor_id);


--
-- Name: idx_transcript_entries_transcript; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transcript_entries_transcript ON public.transcript_entries USING btree (transcript_id) WHERE (is_deleted = false);


--
-- Name: idx_transcripts_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transcripts_student ON public.transcripts USING btree (student_id) WHERE (is_deleted = false);


--
-- Name: idx_ura_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ura_user ON public.user_role_assignments USING btree (user_id) WHERE (is_deleted = false);


--
-- Name: idx_users_district; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_district ON public.users USING btree (district_id) WHERE (is_deleted = false);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email) WHERE (is_deleted = false);


--
-- Name: idx_users_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_institution ON public.users USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_users_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_institution_id ON public.users USING btree (institution_id);


--
-- Name: idx_users_region; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_region ON public.users USING btree (region_id) WHERE (is_deleted = false);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role) WHERE (is_deleted = false);


--
-- Name: idx_verification_records_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_verification_records_entity ON public.verification_records USING btree (entity_type, entity_id);


--
-- Name: idx_verification_records_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_verification_records_status ON public.verification_records USING btree (status);


--
-- Name: idx_video_progress_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_progress_created_by ON public.video_tutorial_progress USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_video_progress_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_progress_institution ON public.video_tutorial_progress USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_video_progress_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_progress_student ON public.video_tutorial_progress USING btree (student_id);


--
-- Name: idx_video_progress_tutorial; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_progress_tutorial ON public.video_tutorial_progress USING btree (video_tutorial_id);


--
-- Name: idx_video_tutorials_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_course ON public.video_tutorials USING btree (course_id) WHERE (is_deleted = false);


--
-- Name: idx_video_tutorials_created_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_created_by ON public.video_tutorials USING btree (created_by) WHERE (is_deleted = false);


--
-- Name: idx_video_tutorials_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_institution ON public.video_tutorials USING btree (institution_id) WHERE (is_deleted = false);


--
-- Name: idx_video_tutorials_lesson; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_lesson ON public.video_tutorials USING btree (lesson_id) WHERE (is_deleted = false);


--
-- Name: idx_video_tutorials_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_lesson_id ON public.video_tutorials USING btree (lesson_id);


--
-- Name: idx_video_tutorials_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_module ON public.video_tutorials USING btree (module_id) WHERE (is_deleted = false);


--
-- Name: idx_video_tutorials_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_status ON public.video_tutorials USING btree (status) WHERE (is_deleted = false);


--
-- Name: idx_video_tutorials_visibility; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_video_tutorials_visibility ON public.video_tutorials USING btree (visibility) WHERE (is_deleted = false);


--
-- Name: idx_wards_district; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wards_district ON public.wards USING btree (district_id) WHERE (is_deleted = false);


--
-- Name: idx_webhook_events_result; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_webhook_events_result ON public.webhook_events USING btree (processing_result);


--
-- Name: idx_webhook_events_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_webhook_events_source ON public.webhook_events USING btree (source, received_at DESC);


--
-- Name: idx_worker_notifications_institution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_notifications_institution_id ON public.worker_notifications USING btree (institution_id);


--
-- Name: idx_worker_notifications_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_notifications_is_read ON public.worker_notifications USING btree (is_read);


--
-- Name: idx_worker_notifications_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_worker_notifications_user_id ON public.worker_notifications USING btree (user_id);


--
-- Name: idx_ws_institution; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ws_institution ON public.workshop_sessions USING btree (institution_id);


--
-- Name: idx_ws_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ws_student ON public.workshop_sessions USING btree (student_id);


--
-- Name: assessment_answers assessment_answers_attempt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_answers
    ADD CONSTRAINT assessment_answers_attempt_id_fkey FOREIGN KEY (attempt_id) REFERENCES public.assessment_attempts(id);


--
-- Name: class_subjects class_subjects_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT class_subjects_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: class_subjects class_subjects_teacher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT class_subjects_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: class_timetable class_timetable_teacher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_timetable
    ADD CONSTRAINT class_timetable_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: classes classes_class_teacher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_class_teacher_id_fkey FOREIGN KEY (class_teacher_id) REFERENCES public.teachers(id);


--
-- Name: classes classes_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: course_lessons course_lessons_module_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_lessons
    ADD CONSTRAINT course_lessons_module_id_fkey FOREIGN KEY (module_id) REFERENCES public.course_modules(id);


--
-- Name: course_modules course_modules_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_modules
    ADD CONSTRAINT course_modules_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: courses courses_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: academic_years fk_academic_years_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_years
    ADD CONSTRAINT fk_academic_years_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: activity_feeds fk_activity_feed_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_feeds
    ADD CONSTRAINT fk_activity_feed_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: answers fk_answers_attempt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) REFERENCES public.attempts(id);


--
-- Name: answers fk_answers_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT fk_answers_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: answers fk_answers_question; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES public.questions(id);


--
-- Name: assessments fk_assessments_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessments
    ADD CONSTRAINT fk_assessments_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: assignments fk_assignments_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT fk_assignments_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: teacher_assignments fk_assignments_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_assignments
    ADD CONSTRAINT fk_assignments_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: teacher_assignments fk_assignments_teacher; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_assignments
    ADD CONSTRAINT fk_assignments_teacher FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: attempts fk_attempts_assessment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attempts
    ADD CONSTRAINT fk_attempts_assessment FOREIGN KEY (assessment_id) REFERENCES public.assessments(id);


--
-- Name: attempts fk_attempts_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attempts
    ADD CONSTRAINT fk_attempts_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: attempts fk_attempts_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attempts
    ADD CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: attendance_records fk_attendance_class_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_records
    ADD CONSTRAINT fk_attendance_class_group FOREIGN KEY (class_group_id) REFERENCES public.classes(id);


--
-- Name: attendance_records fk_attendance_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_records
    ADD CONSTRAINT fk_attendance_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: attendance_records fk_attendance_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_records
    ADD CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: audit_logs fk_audit_logs_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fk_audit_logs_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: grade_boundaries fk_boundaries_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grade_boundaries
    ADD CONSTRAINT fk_boundaries_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: grade_boundaries fk_boundaries_scale; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grade_boundaries
    ADD CONSTRAINT fk_boundaries_scale FOREIGN KEY (grading_scale_id) REFERENCES public.grading_scales(id);


--
-- Name: bulk_attendance_sessions fk_bulk_session_class; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bulk_attendance_sessions
    ADD CONSTRAINT fk_bulk_session_class FOREIGN KEY (class_group_id) REFERENCES public.classes(id);


--
-- Name: bulk_attendance_sessions fk_bulk_session_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bulk_attendance_sessions
    ADD CONSTRAINT fk_bulk_session_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: certificate_signatories fk_cert_signatories_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_signatories
    ADD CONSTRAINT fk_cert_signatories_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: certificate_templates fk_cert_templates_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_templates
    ADD CONSTRAINT fk_cert_templates_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: certificate_template_signatories fk_cert_tpl_sign_signatory; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_signatories
    ADD CONSTRAINT fk_cert_tpl_sign_signatory FOREIGN KEY (signatory_id) REFERENCES public.certificate_signatories(id);


--
-- Name: certificate_template_signatories fk_cert_tpl_sign_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_signatories
    ADD CONSTRAINT fk_cert_tpl_sign_template FOREIGN KEY (template_id) REFERENCES public.certificate_templates(id);


--
-- Name: certificate_template_versions fk_cert_tpl_ver_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificate_template_versions
    ADD CONSTRAINT fk_cert_tpl_ver_template FOREIGN KEY (template_id) REFERENCES public.certificate_templates(id);


--
-- Name: certificates fk_certificates_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT fk_certificates_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: certificates fk_certificates_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT fk_certificates_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: certificates fk_certificates_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.certificates
    ADD CONSTRAINT fk_certificates_template FOREIGN KEY (template_id) REFERENCES public.certificate_templates(id);


--
-- Name: class_groups fk_class_groups_academic_year; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_groups
    ADD CONSTRAINT fk_class_groups_academic_year FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: class_groups fk_class_groups_grade; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_groups
    ADD CONSTRAINT fk_class_groups_grade FOREIGN KEY (grade_id) REFERENCES public.grades(id);


--
-- Name: class_groups fk_class_groups_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_groups
    ADD CONSTRAINT fk_class_groups_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: class_groups fk_class_groups_term; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_groups
    ADD CONSTRAINT fk_class_groups_term FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: custom_roles fk_custom_roles_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_roles
    ADD CONSTRAINT fk_custom_roles_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: districts fk_district_region; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT fk_district_region FOREIGN KEY (region_id) REFERENCES public.regions(id);


--
-- Name: enrollments fk_enrollments_class_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk_enrollments_class_group FOREIGN KEY (class_group_id) REFERENCES public.classes(id);


--
-- Name: enrollments fk_enrollments_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk_enrollments_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: enrollments fk_enrollments_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: event_materials fk_event_mat_event; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_materials
    ADD CONSTRAINT fk_event_mat_event FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- Name: event_registrations fk_event_reg_event; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT fk_event_reg_event FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- Name: events fk_events_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fk_events_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: grades fk_grades_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grades
    ADD CONSTRAINT fk_grades_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: grading_scales fk_grading_scales_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grading_scales
    ADD CONSTRAINT fk_grading_scales_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: data_import_jobs fk_import_jobs_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_import_jobs
    ADD CONSTRAINT fk_import_jobs_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: institution_services fk_institution_service_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_services
    ADD CONSTRAINT fk_institution_service_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: live_class_participants fk_lc_participant_class; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_participants
    ADD CONSTRAINT fk_lc_participant_class FOREIGN KEY (live_class_id) REFERENCES public.live_classes(id);


--
-- Name: live_class_participants fk_lc_participant_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_participants
    ADD CONSTRAINT fk_lc_participant_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: lessons fk_lessons_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT fk_lessons_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: parent_student_links fk_links_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_student_links
    ADD CONSTRAINT fk_links_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: parent_student_links fk_links_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_student_links
    ADD CONSTRAINT fk_links_parent FOREIGN KEY (parent_id) REFERENCES public.parents(id);


--
-- Name: live_class_participants fk_live_class_participants_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_class_participants
    ADD CONSTRAINT fk_live_class_participants_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: nursery_milestones fk_milestones_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_milestones
    ADD CONSTRAINT fk_milestones_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: nursery_milestones fk_milestones_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_milestones
    ADD CONSTRAINT fk_milestones_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: nursery_activity_participations fk_nap_activity; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_activity_participations
    ADD CONSTRAINT fk_nap_activity FOREIGN KEY (activity_id) REFERENCES public.nursery_activities(id);


--
-- Name: nursery_activity_participations fk_nap_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_activity_participations
    ADD CONSTRAINT fk_nap_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: nursery_activity_participations fk_nap_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_activity_participations
    ADD CONSTRAINT fk_nap_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: parent_notification_preferences fk_notif_prefs_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_notification_preferences
    ADD CONSTRAINT fk_notif_prefs_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: parent_notification_preferences fk_notif_prefs_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parent_notification_preferences
    ADD CONSTRAINT fk_notif_prefs_parent FOREIGN KEY (parent_id) REFERENCES public.parents(id);


--
-- Name: nursery_report_cards fk_nrc_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_report_cards
    ADD CONSTRAINT fk_nrc_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: nursery_report_cards fk_nrc_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_report_cards
    ADD CONSTRAINT fk_nrc_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: nursery_activities fk_nursery_activities_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nursery_activities
    ADD CONSTRAINT fk_nursery_activities_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: options fk_options_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT fk_options_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: options fk_options_question; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES public.questions(id);


--
-- Name: parents fk_parents_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT fk_parents_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: parents fk_parents_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: lesson_progress fk_progress_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson_progress
    ADD CONSTRAINT fk_progress_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: lesson_progress fk_progress_lesson; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson_progress
    ADD CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);


--
-- Name: lesson_progress fk_progress_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson_progress
    ADD CONSTRAINT fk_progress_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: teacher_qualifications fk_qualifications_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_qualifications
    ADD CONSTRAINT fk_qualifications_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: teacher_qualifications fk_qualifications_teacher; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_qualifications
    ADD CONSTRAINT fk_qualifications_teacher FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: questions fk_questions_assessment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT fk_questions_assessment FOREIGN KEY (assessment_id) REFERENCES public.assessments(id);


--
-- Name: questions fk_questions_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT fk_questions_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: replay_progress fk_replay_progress_replay; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.replay_progress
    ADD CONSTRAINT fk_replay_progress_replay FOREIGN KEY (replay_id) REFERENCES public.replays(id);


--
-- Name: report_cards fk_report_cards_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT fk_report_cards_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: report_cards fk_report_cards_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT fk_report_cards_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: resources fk_resources_course; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT fk_resources_course FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE SET NULL;


--
-- Name: resources fk_resources_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT fk_resources_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: resources fk_resources_module; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT fk_resources_module FOREIGN KEY (module_id) REFERENCES public.course_modules(id) ON DELETE SET NULL;


--
-- Name: resources fk_resources_teacher_assignment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT fk_resources_teacher_assignment FOREIGN KEY (teacher_assignment_id) REFERENCES public.teacher_assignments(id);


--
-- Name: assessment_results fk_results_assessment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_results
    ADD CONSTRAINT fk_results_assessment FOREIGN KEY (assessment_id) REFERENCES public.assessments(id);


--
-- Name: assessment_results fk_results_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_results
    ADD CONSTRAINT fk_results_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: assessment_results fk_results_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_results
    ADD CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: rubric_criteria fk_rubric_criteria_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rubric_criteria
    ADD CONSTRAINT fk_rubric_criteria_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: rubric_criteria fk_rubric_criteria_rubric; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rubric_criteria
    ADD CONSTRAINT fk_rubric_criteria_rubric FOREIGN KEY (rubric_id) REFERENCES public.grading_rubrics(id);


--
-- Name: grading_rubrics fk_rubrics_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grading_rubrics
    ADD CONSTRAINT fk_rubrics_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: student_class_assignments fk_sca_academic_year; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_assignments
    ADD CONSTRAINT fk_sca_academic_year FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: student_class_assignments fk_sca_class_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_assignments
    ADD CONSTRAINT fk_sca_class_group FOREIGN KEY (class_group_id) REFERENCES public.class_groups(id);


--
-- Name: student_class_assignments fk_sca_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_assignments
    ADD CONSTRAINT fk_sca_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: student_class_assignments fk_sca_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_assignments
    ADD CONSTRAINT fk_sca_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: student_class_assignments fk_sca_term; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_assignments
    ADD CONSTRAINT fk_sca_term FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: scheduled_report_runs fk_scheduled_report_run; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_report_runs
    ADD CONSTRAINT fk_scheduled_report_run FOREIGN KEY (scheduled_report_id) REFERENCES public.scheduled_reports(id) ON DELETE CASCADE;


--
-- Name: scheduled_reports fk_scheduled_report_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_reports
    ADD CONSTRAINT fk_scheduled_report_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: security_events fk_security_events_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT fk_security_events_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: system_settings fk_settings_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT fk_settings_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: dashboard_snapshots fk_snapshots_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_snapshots
    ADD CONSTRAINT fk_snapshots_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: students fk_students_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk_students_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: students fk_students_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: subject_grades fk_subject_grades_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subject_grades
    ADD CONSTRAINT fk_subject_grades_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: subject_grades fk_subject_grades_report; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subject_grades
    ADD CONSTRAINT fk_subject_grades_report FOREIGN KEY (report_card_id) REFERENCES public.report_cards(id);


--
-- Name: assignment_submissions fk_submissions_assignment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment_submissions
    ADD CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES public.assignments(id);


--
-- Name: assignment_submissions fk_submissions_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment_submissions
    ADD CONSTRAINT fk_submissions_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: assignment_submissions fk_submissions_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment_submissions
    ADD CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: attendance_summaries fk_summary_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_summaries
    ADD CONSTRAINT fk_summary_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: attendance_summaries fk_summary_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_summaries
    ADD CONSTRAINT fk_summary_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: teachers fk_teachers_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT fk_teachers_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: teachers fk_teachers_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: terms fk_terms_academic_year; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms
    ADD CONSTRAINT fk_terms_academic_year FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: terms fk_terms_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms
    ADD CONSTRAINT fk_terms_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: transcript_entries fk_transcript_entries_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcript_entries
    ADD CONSTRAINT fk_transcript_entries_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: transcript_entries fk_transcript_entries_transcript; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcript_entries
    ADD CONSTRAINT fk_transcript_entries_transcript FOREIGN KEY (transcript_id) REFERENCES public.transcripts(id);


--
-- Name: transcripts fk_transcripts_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcripts
    ADD CONSTRAINT fk_transcripts_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: transcripts fk_transcripts_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transcripts
    ADD CONSTRAINT fk_transcripts_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: transfer_records fk_transfers_enrollment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfer_records
    ADD CONSTRAINT fk_transfers_enrollment FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: transfer_records fk_transfers_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfer_records
    ADD CONSTRAINT fk_transfers_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: user_role_assignments fk_ura_institution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_assignments
    ADD CONSTRAINT fk_ura_institution FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: user_role_assignments fk_ura_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_assignments
    ADD CONSTRAINT fk_ura_role FOREIGN KEY (role_id) REFERENCES public.custom_roles(id);


--
-- Name: user_role_assignments fk_ura_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_assignments
    ADD CONSTRAINT fk_ura_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: wards fk_ward_district; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wards
    ADD CONSTRAINT fk_ward_district FOREIGN KEY (district_id) REFERENCES public.districts(id);


--
-- Name: live_classes live_classes_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_classes
    ADD CONSTRAINT live_classes_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: live_classes live_classes_teacher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.live_classes
    ADD CONSTRAINT live_classes_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: resource_analytics resource_analytics_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_analytics
    ADD CONSTRAINT resource_analytics_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE CASCADE;


--
-- Name: resource_annotations resource_annotations_parent_annotation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_annotations
    ADD CONSTRAINT resource_annotations_parent_annotation_id_fkey FOREIGN KEY (parent_annotation_id) REFERENCES public.resource_annotations(id) ON DELETE CASCADE;


--
-- Name: resource_annotations resource_annotations_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_annotations
    ADD CONSTRAINT resource_annotations_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE CASCADE;


--
-- Name: resource_taggings resource_taggings_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_taggings
    ADD CONSTRAINT resource_taggings_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE CASCADE;


--
-- Name: resource_taggings resource_taggings_tag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_taggings
    ADD CONSTRAINT resource_taggings_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.resource_tags(id) ON DELETE CASCADE;


--
-- Name: role_permission_mappings role_permission_mappings_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_mappings
    ADD CONSTRAINT role_permission_mappings_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- Name: role_permission_mappings role_permission_mappings_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permission_mappings
    ADD CONSTRAINT role_permission_mappings_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: roles roles_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: student_class_enrollments student_class_enrollments_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_enrollments
    ADD CONSTRAINT student_class_enrollments_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: student_class_enrollments student_class_enrollments_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_class_enrollments
    ADD CONSTRAINT student_class_enrollments_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: student_saved_resources student_saved_resources_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_saved_resources
    ADD CONSTRAINT student_saved_resources_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.resources(id) ON DELETE CASCADE;


--
-- Name: students students_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: students students_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: subjects subjects_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT subjects_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: teacher_class_subject_assignments teacher_class_subject_assignments_teacher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_class_subject_assignments
    ADD CONSTRAINT teacher_class_subject_assignments_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: users users_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: video_tutorial_progress video_tutorial_progress_video_tutorial_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorial_progress
    ADD CONSTRAINT video_tutorial_progress_video_tutorial_id_fkey FOREIGN KEY (video_tutorial_id) REFERENCES public.video_tutorials(id) ON DELETE CASCADE;


--
-- Name: video_tutorials video_tutorials_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorials
    ADD CONSTRAINT video_tutorials_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE SET NULL;


--
-- Name: video_tutorials video_tutorials_institution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorials
    ADD CONSTRAINT video_tutorials_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: video_tutorials video_tutorials_module_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.video_tutorials
    ADD CONSTRAINT video_tutorials_module_id_fkey FOREIGN KEY (module_id) REFERENCES public.course_modules(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--


