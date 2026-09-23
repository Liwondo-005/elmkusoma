# ELMKUSOMA — System Overview + Core Platform + Backend Documentation

**Owner:** Developer 01
**File:** `docs/README-01-SYSTEM-BACKEND.md`
**Scope:** Whole-system overview, ecosystem, backend, database, Flyway, auth, APIs, live backend, events, payments, notifications, security, environment, testing, run requirements, known issues.
**Frontend/user journeys/live UX:** owned by Developer 02 (`docs/README-02-USER-FRONTEND-LIVE.md`) — covered here only where backend evidence requires it.
**Method:** Every capability below is classified as IMPLEMENTED / PARTIAL / CONFIGURED / PLANNED / BLOCKED / NOT VERIFIED based on repository evidence. Anything not verifiable is marked NOT VERIFIED IN REPOSITORY. No secrets are included — only variable names.
**Repository state at time of writing:** branch `main`, Spring Boot `3.4.1`, Java 17 (core) / 21 (other modules), PostgreSQL, 69 Flyway migrations (V01–V69), ~170 JPA entities, ~67 REST controllers.

Related detail docs (existing, not owned here): `docs/api/api-reference.md`, `docs/architecture/overview.md`, `docs/database/schema.md`, `docs/security/jwt-setup.md`.

---

## 1. What is ELMKUSOMA?

ELMKUSOMA is an education management and learning platform. The root `README.md` describes it as "an education management platform for schools, colleges, and universities across Africa."

**Status: IMPLEMENTED** (as a working codebase — backend API + web frontend + realtime/media/worker services).

From repository evidence, the platform is:

- **Type:** A multi-tenant, role-based education platform with a Spring Boot backend, a Next.js web frontend, and supporting realtime/media/background services.
- **Purpose:** To connect learners, educators, institutions, families, and education authorities in one ecosystem covering enrollment, learning content, assessment, progress, live classes, events, media/replays, certificates, payments, notifications, administration, and oversight.
- **Institution scoping:** Almost every domain record carries an `institutionId` (via the shared `BaseEntity`), and users are attached to institutions through `institution_memberships`. The platform is therefore designed as one shared system partitioned by institution, not as isolated per-school deployments.
- **Learning model:** Structured around courses → modules → lessons, class groups, subjects, academic years/terms, assessments, attendance, grading/report cards, and evidence (certificates, transcripts, portfolios).
- **Live learning:** First-class support through LiveKit/WebRTC rooms, WebSocket signaling, attendance tracking, in-class interaction (chat, polls, quizzes, breakout rooms, hand-raise), recording, and replay distribution.
- **Technology role:** Technology is the delivery and record-keeping backbone — JWT-secured REST APIs, PostgreSQL as the system of record (Flyway-migrated), Redis/RabbitMQ for realtime and background work, MinIO-compatible object storage for media, LiveKit for realtime audio/video.

---

## 2. ELMKUSOMA Ecosystem

**Status: IMPLEMENTED** (actors and their backend domains all exist as code).

```
                        ELMKUSOMA
                            |
       +--------------------+--------------------+
       |                    |                    |
    LEARNERS             EDUCATORS          INSTITUTIONS / PROVIDERS
 (Student, Other_      (Teacher, Lecturer,  (Institution, Institution
  Learner, NFE           Instructor)          Membership, NFE Provider)
  Learner, Parent-
  linked children)
       |                    |                    |
       +--------------------+--------------------+
                            |
                      LEARNING CORE
                  (Course → Module → Lesson,
                   Enrollment, Progress,
                   Assessment, Grading,
                   Attendance, Resources)
                            |
        +-------------------+-------------------+
        |                   |                   |
   CONTENT / MEDIA     LIVE LEARNING      EVIDENCE
   (Resource, Lesson,  (LiveClass,         (Certificate,
    MediaAsset,         LiveKit room,       Transcript,
    EventMaterial)      WebSocket, Chat,    Portfolio,
                        Polls, Quizzes)     ReportCard)
                            |
        +-------------------+-------------------+
        |                                       |
PLATFORM OPERATIONS                   EDUCATION OVERSIGHT
(Platform Admin, Institution Admin,    (National / Regional /
 Custom Roles, Commerce,               District Admins,
 Moderation, Support, Audit)            Regions, Districts,
                                        Observe live classes)
```

How the actors interact (from backend evidence):

- **Learners** enroll in courses (`learner_enrollments` / `enrollments`), consume lessons, submit assignments/assessments, accumulate progress (`lesson_progress`), join live classes as participants, register for events, save bookmarks, set learning goals, and earn certificates.
- **Teachers** create courses/modules/lessons, publish assessments and assignments, mark attendance, grade submissions, run live classes (token grants distinguish teacher vs learner capabilities), upload media, and create announcements.
- **Parents** link to children (`parent_student_links`), view attendance/results/activity, manage payments and entitlements, open support tickets, and control notification preferences.
- **Institutions / Providers** own courses, users (via memberships), live classes, events, media, and certificates. NFE (non-formal) providers manage their own programs, sessions, materials, learners, attendance, assessments, and certificates in parallel tables.
- **Platform Administration** manages users, institutions (status + lifecycle), services, incidents, configuration, delegations, verifications, entitlements, commerce (sponsor seats), support tickets, content moderation reports, security events, and audit logs.
- **Education Oversight** (national/regional/district) reads dashboards, regions/districts/schools, performance, attendance, curriculum, assessments, live classes (including observer join), reports, and alerts. Oversight is read-oriented; no oversight write paths to learning data were found.

---

## 3. User and Role Ecosystem

### 3.1 Roles (verbatim from `shared/domain/User.java`, `Role` enum) — IMPLEMENTED

| Role | Purpose (from usage in controllers) | Major workspace | Access boundary |
|---|---|---|---|
| `STUDENT` | Enrolled school learner | Student dashboard, classes, assessments | Own records; class/institution scope |
| `TEACHER` | Educator | Teacher workspace, live-class console, grading | Own classes; institution scope |
| `PARENT` | Family member | Parent dashboard (`/v1/my/*`) | Linked children only |
| `OTHER_LEARNER` | General/independent learner | Learner portal (`/v1/learner/*`) | Own records; institution scope |
| `LEARNER` | Declared role; **no endpoint authorizes it** (NOT VERIFIED in any `@PreAuthorize`) | NOT VERIFIED | NOT VERIFIED |
| `PROVIDER_ADMIN` | NFE provider administrator | NFE provider APIs | Own provider scope |
| `PROVIDER_STAFF` | NFE provider staff | NFE provider APIs | Own provider scope |
| `ADMIN` | Platform administrator | Platform admin (`/v1/platform-admin/*`) | Cross-institution |
| `INSTITUTION_ADMIN` | Institution administrator | Institution admin (`/v1/admin/*`) | Own institution |
| `NATIONAL_ADMIN` | Education oversight (national) | Oversight dashboards | National read scope |
| `REGIONAL_ADMIN` | Education oversight (regional) | Oversight dashboards | Regional read scope |
| `DISTRICT_ADMIN` | Education oversight (district) | Oversight dashboards | District read scope |

Public self-registration is restricted to `STUDENT, TEACHER, PARENT, OTHER_LEARNER` (enforced in `AuthServiceImpl`; defaults to `STUDENT`). All other roles must be provisioned by an administrator.

Membership-level roles (`InstitutionMembership.Role`, separate enum): `OWNER, ADMIN, TEACHER, STUDENT, PARENT`. Live-class participant roles (string constants): `LEARNER, TEACHER, OBSERVER`.

### 3.2 ROLE vs LEARNING CONTEXT — IMPLEMENTED distinction

The repository separates **who you are** (`User.role`) from **where/what you learn** (`User.learningLevel`, `secondaryStage`, `form`):

- `LearningLevel`: `NURSERY, PRIMARY, SECONDARY, COLLEGE, UNIVERSITY`
- `SecondaryStage`: `O_LEVEL, A_LEVEL`
- `Form`: `FORM_1 … FORM_6`

A `STUDENT` with `learningLevel=SECONDARY` + `form=FORM_4` is routed differently from a `STUDENT` with `learningLevel=PRIMARY`, while an `OTHER_LEARNER` uses the general learner portal regardless of level. Role gates *permissions*; learning context gates *experience and curriculum*.

---

## 4. Education Ecosystem

**Status: IMPLEMENTED** (backend domains + frontend route groups exist for each level; depth varies — frontend detail is Developer 02's scope).

| Context | Backend evidence | Notes |
|---|---|---|
| Nursery | `nursery/domain` (10 entities: activities, milestones, quests, stories, feelings check-ins, missions, Tanzania discovery, parent learning, report cards); `NurseryController`, `NurseryExtendedController` | Dedicated domain. Status: IMPLEMENTED |
| Primary | `primary/domain` (17 entities: curriculum topics, reading adventures, labs, quests, mistake lab, passport, badges, streaks, missions, speaking, collaborations); `PrimaryPortalController` (~35 endpoints) | Richest dedicated domain. Status: IMPLEMENTED |
| Secondary O-Level / A-Level | `SecondaryStage` + `Form` on `User`; `learning/domain` secondary banks (concepts, problems, errors, study planner); `SecondaryExtendedController` | O/A-Level + Forms 1–6 modeled. Status: IMPLEMENTED |
| Higher Education — College / TVET / University | `highereducation/domain` (26 entities: programmes, departments, theses, research, projects, fieldwork, competencies, portfolios, study tasks, workshops, career); ~16 controllers under `/api/v1/education/*` + `/v1/college/learner/*` | Largest domain package. Status: IMPLEMENTED |
| Other Learner (Adult / Lifelong / Professional / Skills / Independent / Non-formal) | `OTHER_LEARNER` role + `learner/` package (profile, enrollments, bookmarks, goals, notifications, certificates, search, discovery); `GeneralLearnerProfile` | Served by shared foundations, not a separate system. Status: IMPLEMENTED |
| Non-formal / Alternative provision | `nfe/` packages (provider, program, session, material, learner, attendance, assessment, certificate) with parallel tables | Separate-but-mirrored NFE stack. Status: IMPLEMENTED |

Shared foundations used across all levels (not duplicated): `User`, `Institution`, `InstitutionMembership`, `Course`, `Resource`, `Bookmark`, `Certificate`, `Event/Replay`, `Notification`, `Audit`. Level-specific tables exist only where pedagogy differs (nursery activities, primary gamification, HE research/thesis).

---

## 5. Core Platform Services

| Service | Status | Main backend components | Key relationships |
|---|---|---|---|
| Authentication | IMPLEMENTED | `AuthController` (10 endpoints), `AuthServiceImpl`, `JwtTokenProvider`, token entities | `users`, `revoked_tokens`, `password_reset_tokens`, `email_verification_tokens`, `verification_codes` |
| Authorization | IMPLEMENTED (endpoint-level); PARTIAL (object-level — see §10/§28) | `SecurityConfig` ×4 modules, `@PreAuthorize`, `JwtRequestAttributeFilter` | role + `institution_memberships` + ownership checks in services |
| User Management | IMPLEMENTED | `User`, `StudentController`, `TeacherController`, `InstitutionPeopleController`, invites | `users` ↔ `students`/`teachers`/`parents` via `userId` |
| Institutions | IMPLEMENTED | `InstitutionController`, `Institution` (15 types), lifecycle status | `institutions` ↔ memberships ↔ all `institutionId` records |
| Memberships | IMPLEMENTED | `InstitutionMembership`, people/invite flows | `institution_memberships` (own sequence id) |
| Learning (courses/lessons) | IMPLEMENTED | `CourseController`, `LearningController`, `Course/Module/Lesson` | courses → modules → lessons; `subjectId`, `classGroupId` |
| Enrollment | IMPLEMENTED | `EnrollmentController`, `Enrollment`, `LearnerEnrollment`, transfers | student/user ↔ class/course |
| Assessment | IMPLEMENTED | `AssessmentController`, Assessment→Question→Option→Attempt→Answer→Result | auto-grading for MCQ/TRUE_FALSE (verified via service code) |
| Grading | IMPLEMENTED | `GradingController`, scales/boundaries/report cards/`subject_grades` | report card → subject grades |
| Attendance | IMPLEMENTED | `AttendanceController`, bulk sessions, summaries; live attendance detail | class/student/date; live join/leave seconds |
| Progress | IMPLEMENTED | `LessonProgress`, `LearnerEnrollment.progressPercentage`, averages | lesson ↔ student completion % |
| Certificates | IMPLEMENTED | `CertificateService` (template/generate/issue/revoke/verify), `CertificateController`, NFE mirror | template → certificate → public verification code |
| Resources | IMPLEMENTED | `Resource` (6 types), search/filter, related | subject/class/institution scope |
| Search | IMPLEMENTED (JPQL LIKE-based; no dedicated index) | `LearnerController /search`, per-domain search queries | courses, resources, live, announcements |
| Notifications | IMPLEMENTED | `NotificationService`, `LearnerNotification`, parent preferences, realtime broadcaster | user-targeted; parent preference gates |
| Communication | IMPLEMENTED | Parent messages, support tickets + messages, announcements | sender/recipient, ticket threads |
| Live Learning | IMPLEMENTED | LiveKit service, session/interaction controllers, 15 live entities | class → participants → attendance → recording |
| Events | IMPLEMENTED | `EventService` (publish/register/materials/live/summary), registrations | event → registrations/materials → replay → certificate |
| Media | IMPLEMENTED | `elmkusoma-media` service, MinIO storage, `MediaAsset`/`MediaFile` | source-typed attachments, presigned upload |
| Recording/Replay | IMPLEMENTED | Egress config, webhook auto-creates `Replay`, replay controllers | event/live session → replay → progress |
| Payments | IMPLEMENTED (provider-agnostic) | `Payment`, `ParentPaymentService`, generic webhook (secret-header) | payment → entitlement → access |
| Entitlements | IMPLEMENTED | `Entitlement`, `hasEntitlement` checks, commerce sponsor-seats | user/student ↔ service access window |
| Portfolio | IMPLEMENTED | Primary `student_portfolio_items`; HE `portfolios` + items | student-owned collections |
| Projects | IMPLEMENTED | HE projects/milestones/submissions/grading; primary missions | student → milestones → submissions |
| Research | IMPLEMENTED | HE research projects/milestones/resources, thesis + supervisor | student → research → thesis |
| Academic Records | IMPLEMENTED | HE academic records/history, transcripts + entries | student → record history |
| Career/Professional | IMPLEMENTED | HE career profiles, professional development goals, demonstrations, competencies | student → profile/goals/competency records |
| Administration | IMPLEMENTED | Platform + institution admin controllers/services, commerce, moderation | cross-cutting governance |
| Oversight | IMPLEMENTED (read-oriented) | `OversightController`, regions/districts, observer join | region → district → institution |
| Audit | IMPLEMENTED | `AuditLog`, `SecurityEvent`, `ActivityFeed`, audit controller | every significant action recorded |

---

## 6. How the Whole System Works

**Status: IMPLEMENTED** (each step maps to verified endpoints/entities; adapted where flows branch by role).

```
REGISTER (/v1/auth/register — STUDENT/TEACHER/PARENT/OTHER_LEARNER)
   ↓
AUTHENTICATE (/v1/auth/login — BCrypt check, isActive enforced)
   ↓  access token (1h) + refresh token (7d), claims: userId/role/institutionId
SELECT / RESOLVE ROLE (User.role; admin-provisioned roles beyond the public 4)
   ↓
RESOLVE LEARNING CONTEXT (learningLevel, secondaryStage, form)
   ↓
INSTITUTION / MEMBERSHIP / SCOPE (institution_memberships → request attributes)
   ↓
ACCESS WORKSPACE (role-specific dashboard: student/teacher/parent/learner/admin/oversight)
   ↓
ENROLL / PARTICIPATE (course enrollments, class assignments, event registrations)
   ↓
LEARN (lessons, resources, media, assignments)
   ↓
PRACTICE (problem banks, labs, quests, study planner)
   ↓
ASSESS (assessments/attempts, auto-graded where objective)
   ↓
PROGRESS (lesson completion %, averages, report cards, attendance)
   ↓
LIVE / EVENTS / COLLABORATION (LiveKit rooms, events/seminars, group work)
   ↓
EVIDENCE / CERTIFICATE (certificates with public verification codes, transcripts)
```

---

## 7. System Architecture

**Status: IMPLEMENTED.**

```
Browser / Client (Next.js 16, React 19)
        ↓  HTTPS + JWT (Bearer)          /ws (SockJS/STOMP via nginx → realtime:8081)
Nginx (production profile only: 80/443)
        ↓  /v1/* /api/v1/* → core:8080 | /api/v1/media/* → media:8083
Spring Boot services
        ↓  JPA repositories
PostgreSQL 16 (system of record, Flyway-migrated)
```

Additional systems (all confirmed in config/code):

| System | Status | Evidence |
|---|---|---|
| Redis 7 | CONFIGURED | `spring-data-redis`, `localhost:6379`; compose `redis:7-alpine` |
| RabbitMQ 3.12 | CONFIGURED | `spring-amqp`, guest/guest; workers consume queues; management `:15672` |
| WebSocket/STOMP | IMPLEMENTED | Realtime module (`/ws`, `/topic`, `/queue`, `/app`); core raw WS `/ws/live-class/{classId}` |
| LiveKit | CONFIGURED (server external) | `livekit-server:latest` in `docker-compose.livekit.yml` only (not main compose); token/webhook code present |
| Media/MinIO | CONFIGURED | `minio/minio:latest` (`:9000`/`:9001`); media service `minio 8.5.7` SDK |
| External payments | NOT VERIFIED (provider-agnostic webhook) | No gateway SDK strings found; provider is a free-form field |
| Email | CONFIGURED | `spring-boot-starter-mail`, workers mail+Thymeleaf templates, Gmail SMTP defaults |
| SMS/Push | PLANNED (flags only) | Preference booleans exist; no SMS/push sender code verified |
| Monitoring | CONFIGURED | Prometheus, Grafana dashboard, OpenTelemetry collector configs in `infrastructure/monitoring` |

Service ports (verified): core `8080`, realtime `8081`, workers `8082`, media `8083`, frontend `3000`, postgres `5432`, redis `6379`, rabbitmq `5672`, minio `9000/9001`, livekit `7880/7881`.

---

## 8. Backend Architecture

**Status: IMPLEMENTED.**

- **Framework:** Spring Boot `3.4.1` (parent POM) in all four modules.
- **Language:** Java 17 (`elmkusoma-core`; compiler plugin `3.11.0`, release 17). Media/realtime/workers declare Java 21.
- **Build:** Maven (`mvnw` wrapper referenced by root README; system Maven used in practice).
- **Modules:** `elmkusoma-core` (monolith API, `0.1.0-SNAPSHOT`), `elmkusoma-media` (file storage), `elmkusoma-realtime` (WebSocket/presence/notifications), `elmkusoma-workers` (background jobs: mail, certificates via OpenPDF `1.3.30`, retries).
- **Layering:** controllers → services → repositories → entities; DTOs per domain (`certificate/dto` 8 classes, `event/dto` 6, `learner/dto` 12); MapStruct-style mappers NOT VERIFIED (manual mapping observed).
- **Scale:** ~67 REST controllers, ~500–550 endpoints (approx), ~170 JPA entities.
- **Configuration:** `application.yml` (+ `application-prod.yml`), `@ConfigurationProperties` (e.g. LiveKit), `@Value` env bindings.
- **Exception handling:** Single `GlobalExceptionHandler` (`@RestControllerAdvice`, 11 handlers → 400/401/403/404/409/500/503 + `ResponseStatusException` passthrough).
- **API docs:** springdoc-openapi `2.7.0` (Swagger UI restricted to ADMIN in core).
- **Cross-cutting:** Lombok `1.18.36`, validation starter, actuator health, auditing entity listener + custom `AuditListener`.
- **Scheduled/background:** Workers module consumes RabbitMQ queues (mail, certificates); in-app `@Scheduled` jobs NOT VERIFIED.

---

## 9. Database Architecture

**Status: IMPLEMENTED.**

- **Engine:** PostgreSQL 16 (`postgres:16-alpine` in compose; `postgresql` runtime driver).
- **Size:** ~170 tables (one per `@Entity`).
- **Identity:** `BaseEntity` (`@MappedSuperclass`) gives most tables: UUID primary key, `institution_id`, `created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted` (soft delete). Exceptions with own ids: `Institution`, `InstitutionMembership` (Long), audit tables, identity token tables, media `media_files` (Long, separate service).
- **Institution ownership:** `institution_id` column is the tenancy marker across learning, events, certificates, media, admin, and audit tables.
- **Users:** `users` (email unique, password hash, role enum, active/verified flags, learning context columns, region/district UUIDs).
- **Memberships:** `institution_memberships` links user ↔ institution with a membership role.
- **Learners:** `students` (+ class assignments), `general_learner_profiles`, `learner_enrollments`, `nfe_learners`.
- **Teachers:** `teachers` (+ assignments, qualifications), each with a read-only JPA link back to `users`.
- **Enrollment:** `enrollments` (class-based, with transfer records) and `learner_enrollments` (course-based, with progress %).
- **Learning:** `courses` → `course_modules` → `course_lessons`; `lessons`, `assignments` (+ submissions), `resources`.
- **Assessments:** `assessments` → `questions` → `options`; `attempts` → `answers`; `assessment_results`; grading scales/boundaries/report cards/`subject_grades`.
- **Live:** `live_classes`, `live_class_participants` (unique class+user), attendance detail, chat, polls/votes, quizzes/questions/responses, breakout rooms/assignments, hand-raise queue, session events, shared media, issues.
- **Events:** `events`, `event_registrations` (unique event+user), `event_materials`, `replays`.
- **Media:** `media_assets` (core) + `media_files` (media service).
- **Administration:** settings, custom roles, delegations, invitations, imports, incidents, services, entitlements, moderation reports, support tickets.
- **Oversight:** `regions` → `districts` (region-linked); institutions carry region/district UUIDs.
- **Relationship pattern:** Raw UUID columns dominate (~769 FK-style UUID fields vs 6 read-only `@ManyToOne` joins in parent/teacher domains). Consequence: referential integrity is enforced by application logic, not database foreign keys.

---

## 10. Flyway

**Status: IMPLEMENTED (core); media service has its own V001.**

- **Location:** `backend/elmkusoma-core/src/main/resources/db/migration/`
- **Naming:** `V<version>__<snake_case_description>.sql` (V01–V09 zero-padded; V10+ unpadded).
- **Range/count:** V01–V69, 69 files, contiguous — **no gaps, no duplicate versions** (verified by enumeration).
- **V60 question (resolved):** The repository contains exactly ONE V60 file: `V60__institution_admin_ecosystem.sql` (extends `institutions`, adds `institution_audit_log`). There is NO `V60__add_secondary_stage_and_form_to_users.sql` — that description lives at `V65__add_secondary_stage_and_form_to_users.sql` (`secondary_stage`, `form` on `users`). No action required; do not edit either file.
- **Latest migrations:** `V68__institution_lifecycle_status.sql`, `V69__content_reports_and_data_retention.sql`.
- **Runtime config:** default profile — `ddl-auto: none`, `flyway.enabled: true`, `out-of-order: true`, `baseline-on-migrate: true`. Production — `ddl-auto: validate`, `flyway.enabled: true`, `validate-on-migrate: true`, `baseline-version: 6`.
- **Current migration status:** NOT VERIFIED IN REPOSITORY (requires a live database's `flyway_schema_history`; if the repo's V01–V64 range is referenced against a database whose history differs, compare `flyway_schema_history` versions to the V01–V69 file list exactly).
- **Seeds (not migrations):** `backend/setup-db.sql` (create DB), `seed_security*.sql`, `seed_audit*.sql`, `seed_oversight.sql` (test accounts), curriculum/reading/lab seeds under `db/seed/`, consolidated `db/init.sql` (V1–V40, initial setup only).

---

## 11. Authentication

**Status: IMPLEMENTED.**

| Capability | Implementation |
|---|---|
| Registration | `POST /v1/auth/register` — public roles only (STUDENT/TEACHER/PARENT/OTHER_LEARNER, default STUDENT); password BCrypt-encoded (verified via encoder usage in service) |
| Login | `POST /v1/auth/login` — `AuthenticationManager`, `isActive` enforced, returns access + refresh tokens |
| Password hashing | BCrypt (`passwordHash` column; encoder bean in security config) |
| JWT | JJWT `0.12.6`, HS256, Base64-decoded `jwt.secret`; access 1h (`3600000` ms), refresh 7d (`604800000` ms); claims: subject=email + optional `userId`, `role`, `institutionId` |
| Refresh token | `POST /v1/auth/refresh` — stateless JWT + SHA-256 hash revocation check; rotates both tokens |
| Logout | `POST /v1/auth/logout` — persists SHA-256 hash in `revoked_tokens` (7-day expiry) |
| Password reset | `forgot-password` (24h `PasswordResetToken`, generic response) → `reset-password` (consumes token) |
| Email verification | `verify-email` (`EmailVerificationToken`) + 5-digit `VerificationCode` flow (`send-code`/`verify-code`: 10-min expiry, 60s resend throttle, 5 attempt cap) |
| Current user | `GET /v1/auth/me` from `SecurityContext` |

---

## 12. Authorization and Multi-Tenancy

**Status: IMPLEMENTED at endpoint level; PARTIAL at object level (see §28).**

- **Roles:** 12 `User.Role` values enforced via `@PreAuthorize` (`hasRole`/`hasAnyRole`, authorities built as `ROLE_<name>`). Five roles (`LEARNER`, `NATIONAL/REGIONAL/DISTRICT_ADMIN`) appear in almost no learner/teacher `@PreAuthorize` strings — oversight roles are authorized on oversight endpoints instead.
- **Permissions:** `custom_roles` + `role_permissions` + `user_role_assignments` tables exist (PLATFORM ADMIN scope); service-level enforcement NOT VERIFIED endpoint-by-endpoint.
- **Institution scope:** `JwtRequestAttributeFilter` sets `userId`, `userEmail`, `userRole`, `institutionId` (first active membership) as request attributes; controllers scope queries with them.
- **Membership:** `institution_memberships` with `OWNER/ADMIN/TEACHER/STUDENT/PARENT` membership roles; people/invite lifecycle managed by institution admins.
- **Resource ownership:** Services check ownership where implemented (bookmarks scoped to JWT user; certificate student-ownership checks added for learner roles). Universal enforcement is NOT VERIFIED — each controller must be audited individually.
- **Endpoint authorization:** Class-level `@PreAuthorize` on most controllers; NFE/HE controllers rely on method-level annotations; webhooks (`/v1/webhooks/*`) use secret-header or no auth by design.
- **Access model (aspirational form, verified in parts):** `ACCESS = ROLE + PERMISSION + SCOPE + RESOURCE OWNERSHIP`. ROLE and SCOPE are systematically enforced; PERMISSION-table and OWNERSHIP enforcement vary by endpoint — do not claim universal enforcement.

---

## 13. API Architecture

**Status: IMPLEMENTED (~67 controllers, ~500–550 endpoints approx).** Conventions: `/v1/*` (majority) and `/api/v1/*` (student-facing + higher-education groups). Response envelope: `ApiResponse` (success/error). Page convention where paginated: `page` (default 0) + `size` (default 20) → Spring `Page`.

| Group | Base path | Auth | Key endpoints |
|---|---|---|---|
| Authentication | `/v1/auth` | public (register/login/refresh/reset/verify) + authenticated (`/me`, `/logout`) | register, login, refresh, me, forgot/reset-password, verify-email, logout, send/verify-code |
| Users (students/teachers) | `/v1/students`, `/v1/teachers`, `/v1/teachers/me/*` | TEACHER/INSTITUTION_ADMIN/ADMIN; self-profile for TEACHER | CRUD, assign-class, profile, classes, students, dashboard, live-classes, announcements, analytics |
| Institutions | `/v1/institutions` | INSTITUTION_ADMIN/ADMIN | CRUD, activate/deactivate |
| Memberships | `/v1/admin/people` | ADMIN/INSTITUTION_ADMIN | list/get, role change, activate/deactivate, invites |
| Learners (general) | `/v1/learner` (38 endpoints) | OTHER_LEARNER/TEACHER/STUDENT/INSTITUTION_ADMIN/ADMIN | profile, dashboard, courses, enrollments, progress, resources, live-classes, announcements, bookmarks, notifications, certificates, search, related, learning-paths, goals |
| Parents | `/v1/parents` (admin-side), `/v1/my` (self, ~35 endpoints) | PARENT for self | overview, children detail/attendance/results/activity, payments initiate/cancel, achievements, goals, library, entitlements, tickets, messages, notifications, preferences |
| Courses | `/v1/courses` | TEACHER/INSTITUTION_ADMIN/ADMIN | CRUD, toggle-publish, stats, modules/lessons CRUD |
| Enrollment | `/v1/enrollments` | TEACHER/INSTITUTION_ADMIN/ADMIN | create/list, by student/class, status, transfer |
| Learning | `/v1/learning`, `/v1/secondary/extended` | TEACHER/INSTITUTION_ADMIN/ADMIN (+STUDENT read on extended) | lessons, progress, assignments/submit/grade, concept/problem/error banks, study planner |
| Assessment | `/v1/assessments` | TEACHER/INSTITUTION_ADMIN/ADMIN | create, by class/subject, questions, start/submit, results, grading |
| Progress | via learner (`/me/progress*`), learning (`/progress/*`), student dashboard | role-scoped | course progress, averages, continue-learning |
| Live | `/v1/live-session` (×3 controllers), `/v1/teachers/me/live-classes`, `/api/v1/student/live-classes`, `/v1/admin/live-sessions` | mixed (TEACHER run, STUDENT/OTHER_LEARNER join, ADMIN observe) | join/leave, participants, analytics, recording start/stop/download, quizzes, polls, breakout rooms, shared media, attendance |
| Events | `/v1/events`, `/v1/learner/events`, `/api/v1/student/events` | ADMIN/INSTITUTION_ADMIN/TEACHER manage; learners register | CRUD, publish/cancel, start/end-live, registrations, materials, summary |
| Media | `/v1/media` (core), media service `/api/v1/media/*` | mixed; teacher upload | list/search/get, upload, presigned-upload, download-url |
| Certificates | `/v1/certificates` (+ NFE mirror) | TEACHER/INSTITUTION_ADMIN/ADMIN manage; `GET /verify/{code}` public (`permitAll`) | templates, generate/issue/revoke, get/list, transcripts, public verify |
| Payments | `/v1/parents` (verify/refund), `/v1/my` (initiate), `/v1/webhooks/payments` (secret-header) | PARENT self + admin | initiate/verify/cancel/refund, entitlement checks |
| Notifications | `/api/v1/notifications`, `/v1/learner/me/notifications` | JWT-user-scoped | list, unread-count, mark read/all-read |
| Administration | `/v1/admin`, `/v1/platform-admin` (~49 endpoints), `/v1/admin/org`, `/v1/admin/people` | INSTITUTION_ADMIN / ADMIN respectively | dashboards, users, institutions + lifecycle, services, incidents, config, delegations, verifications, entitlements, commerce, support, moderation, search, export |
| Oversight | `/v1/oversight` | NATIONAL/REGIONAL/DISTRICT_ADMIN (method-level) | dashboard, regions/districts/schools, performance, attendance, curriculum, assessments, live-classes + observe, reports, alerts |

No endpoint paths were invented — all paths above were read from controller mappings.

---

## 14. Live Backend Architecture

**Status: IMPLEMENTED.**

```
User (join request)
 ↓  eligibility (enrollment/registration checks in service)
Live Session (live_classes row: SCHEDULED → LIVE → ENDED)
 ↓  authorization (TEACHER manages; STUDENT/OTHER_LEARNER joins; OBSERVER observes)
Short-lived LiveKit token (2h HS256 JWT; teacher grants include roomCreate/roomAdmin/publish; learner publish=false)
 ↓
LiveKit / WebRTC (external server) + WebSocket signaling (/ws/live-class/{classId}: join/chat/leave/hand-raise/mute/kick/heartbeat)
 ↓  Attendance / Events (participants table, join/leave timestamps, attendance detail seconds)
Recording (egress start/stop; webhook recording_completed)
 ↓
Replay (auto-created AVAILABLE row → learner progress tracking)
```

- Token service: `LiveKitService.generateToken` (room `liveclass-<classId>`); server auth header for egress.
- Webhooks: `POST /v1/webhooks/livekit` handles room/participant/recording events; recording completion flips `Event.recordingStatus` and auto-creates `Replay`.
- Interaction: chat messages, polls/votes, quizzes/questions/responses, breakout rooms/assignments, hand-raise queue, shared media, issues — all persisted.
- Health: `LiveKitHealthIndicator`, session health endpoint, admin live-session stats.
- Realtime module (port 8081) adds STOMP presence/notification broadcast alongside core's raw WebSocket.

---

## 15. Events and Seminars

**Status: IMPLEMENTED.** Events **reuse the same live architecture** — there is no separate seminar-live system.

- `events` table carries both scheduling fields and live linkage (`meetingUrl`, `recordingUrl/Status`, `relatedCourse/Module/LessonId`, `rescheduledFrom`, 17-state `EventStatus`, 12-value `EventType`).
- Lifecycle: DRAFT → REVIEW → PUBLISHED → REGISTRATION_OPEN → … → LIVE → ENDED → REPLAY_AVAILABLE (plus CANCELLED/RESCHEDULED/FULL/FAILED branches).
- Registration: unique event+user rows, WAITLISTED/ATTENDED/NO_SHOW states, approval-gated events, capacity tracking.
- Attendance at events can yield `certificateId` on the registration (event → certificate automation verified in service code).
- Materials support 9 types (RECORDING/VIDEO/DOCUMENT/PRESENTATION/PDF/IMAGE/AUDIO/LINK/OTHER) with public/private flags.
- Learner, student, and general-learner controllers expose parallel registration views over the same tables.

---

## 16. Learning and Academic Core

**Status: IMPLEMENTED** (see entity inventory in §9 and endpoints in §13).

Courses (with `isPublished`/`isFeatured` flags) → modules (ordered) → lessons (typed content: VIDEO/DOCUMENT/QUIZ/ASSIGNMENT/LINK/TEXT). Subjects, class groups, grades, academic years, and terms provide the academic scaffolding. Enrollment exists in two lanes (class-based `enrollments` with transfers; course-based `learner_enrollments` with progress %). Progress is tracked per lesson per student. Assessment runs attempts with auto-grading for objective types; grading scales/boundaries feed report cards and subject grades. Academic records and transcripts persist achievement; certificates (and NFE certificates) are the public, verifiable evidence layer. Projects, research, portfolios, competencies, fieldwork, theses, study tasks, and career profiles extend the core for higher education; primary/nursery have their own engagement domains.

---

## 17. Platform Administration

**Status: IMPLEMENTED.** Boundaries:

- **Platform Administration** (`ADMIN` only, `/v1/platform-admin`): cross-institution users, institutions (status + lifecycle), services catalog, incidents, config, notifications broadcast, delegations, pending verifications, entitlements, commerce (sponsor seats, revocation), support tickets, content moderation reports, security events, audit logs, global search/export, health/attention/activity feeds.
- **Institution/Provider Administration** (`ADMIN`/`INSTITUTION_ADMIN`, `/v1/admin*`): dashboard, settings, custom roles, user import jobs, org profile, people/invites, enabled services.
- **Education Oversight** (`NATIONAL/REGIONAL/DISTRICT_ADMIN`, `/v1/oversight`): read-only dashboards and reports; live-class observation via OBSERVER role. No write access to learning data.
- **Audit/Governance:** `audit_logs` (who did what to which entity, with before/after values, IP, timing), `security_events` (typed, severity-graded, resolvable), `activity_feeds` (visibility-scoped), plus institution-scoped audit/activity tables.
- **Security ops:** security-event resolution flows exist in both platform-admin and audit controllers.

---

## 18. Payments and Entitlements

**Status: IMPLEMENTED (provider-agnostic).**

- `payments`: parent/student-linked, `BigDecimal` amount (default currency TZS), service-typed (`serviceType`/`serviceId`), free-form `provider`/`providerReference`, lifecycle PENDING → COMPLETED/FAILED (plus cancel/refund paths).
- Webhook: single `POST /v1/webhooks/payments`, secret-header authenticated (`payment.webhook.secret`), idempotent success handling (grants entitlement), failure marking. No `@PreAuthorize` by design.
- Entitlements: time-boxed service access grants; `hasEntitlement` checks gate access; commerce flows (sponsor seats, revocation) managed by platform admin.
- Supported providers: NOT VERIFIED IN REPOSITORY — no gateway SDK or provider-specific strings (Selcom, M-Pesa, Stripe, Flutterwave, DPO, etc.) were found in code. Do not claim any.

---

## 19. Notifications and Communication

**Status: IMPLEMENTED.**

- `learner_notifications`: user-targeted rows (type/target linkage, read flags), JWT-scoped list/unread-count/read APIs.
- `NotificationService`: direct notify, institution-student broadcast (STUDENT only), read-state management, and parent preference gating (attendance/grade/fee/announcement categories).
- Preferences: per-parent channel toggles (SMS/email/push); platform-level notification records for admin broadcast.
- Realtime: STOMP broadcaster in realtime module + event-publisher bridge from core.
- Email: workers module (Thymeleaf templates, Gmail SMTP defaults). SMS/push senders: NOT VERIFIED (preference flags only).
- Communication: parent↔school messages, support ticket threads, announcements, platform notifications.

---

## 20. Security

**Status: IMPLEMENTED (mechanisms); PARTIAL (uniform coverage — see §28).**

- Password hashing: BCrypt; account-active enforcement at login.
- Authentication: JWT access (1h) + refresh (7d) with hash-revocation list; password-reset and email-verification token tables; 5-digit code flow with throttling.
- Authorization: method security with role expressions; request-attribute identity (`userId/userEmail/userRole/institutionId`).
- Token security: Base64 HMAC secret (required env), 2h LiveKit tokens with least-privilege grants, webhook secret-header auth.
- CORS: explicit origin allowlist in core (localhost dev ports + production domains); open patterns only in auxiliary modules' dev-facing configs.
- Validation: Bean Validation on DTOs + global 400 mapping; FK/unique violations mapped to 409 with sanitized messages.
- Institution isolation: `institutionId` scoping on queries; membership-derived identity. Known variance: some controllers accept institution from the `X-Institution-Id` header while others use the JWT-derived attribute (see §28).
- Audit: auth-relevant events (login success/failure, password changes, certificate issue/revoke, exports) recorded as security events/audit logs.
- Secrets: all secrets via environment; no real secret values in code (dev placeholders only — see §21/§28).
- Upload security: media via presigned flows and type-tracked assets; object-level auth rules per endpoint (verify per endpoint).
- Rate limiting: in-memory 10/min guard on public certificate verification (single-instance semantics — NOT VERIFIED under multi-instance deployment).

---

## 21. Environment Configuration

**Status: CONFIGURED (compose + examples); production values NOT VERIFIED.**

| Variable | Purpose | Required | Sensitive |
|---|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` | Postgres connection | Yes (defaults: localhost/5432/elmkusoma/postgres) | No |
| `DB_PASSWORD` | Postgres password | Yes (no default in core) | YES |
| `JWT_SECRET` | JWT HMAC Base64 key | Yes (no default in core) | YES |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis | No (localhost:6379) | Password YES |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ | No (guest/guest) | Password YES |
| `LIVEKIT_URL` / `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` | LiveKit server | For live features | Key/secret YES |
| `LIVEKIT_INGRESS_ENABLED` / `LIVEKIT_WHIP_ENDPOINT` / `LIVEKIT_RTMP_ENDPOINT` / `LIVEKIT_SRT_ENDPOINT` | Ingress | No (disabled) | No |
| `LIVEKIT_EGRESS_ENABLED` / `LIVEKIT_EGRESS_BUCKET` / `LIVEKIT_EGRESS_PATH` | Recording egress | No (disabled) | No |
| `MEDIA_SERVICE_URL` / `REALTIME_SERVICE_URL` / `WORKERS_SERVICE_URL` | Inter-service URLs | No (localhost defaults) | No |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_BUCKET` | Object storage | For media service | Keys YES |
| `SERVER_PORT` (media) | Media port | No (8083) | No |
| `JWT_EXPIRATION` (media) / `app.jwt.secret` (workers) | Auxiliary JWT | Module-local | YES |
| `RABBITMQ_*` prod variants | Prod broker | In prod compose | Password YES |

Hardcoded non-secret behavior (verified): token lifetimes (1h/7d), LiveKit room defaults (empty-timeout 300s, max 10000 participants), egress path `recordings/`, CORS origins, pagination defaults (page 0, size 20).

---

## 22. Testing

**Status: PARTIAL (backend unit/integration present; no CI; frontend unit runner absent).**

Backend (`backend/elmkusoma-core/src/test`, 18 files + test properties):

| Test | Type | Covers |
|---|---|---|
| `AcademicServiceTest` | Unit | Academic service |
| `PlatformAdminServiceTest` | Unit | Platform-admin dashboard |
| `CertificateServiceTest` | Unit | Certificate generate/issue/revoke/verify |
| `LiveClassServiceTest` | Unit | Live-class lifecycle |
| `GpaCalculationServiceTest` | Unit | GPA calculation |
| `HigherEducationDashboardServiceTest` | Unit | HE dashboard |
| `AuthServiceTest` | Unit | Auth flows |
| `AuthServiceImplRegistrationTest` | Unit | Registration role rules |
| `EventLifecycleE2ETest` | Integration (MockMvc) | Event lifecycle incl. webhooks |
| `LiveKitIntegrationTest` | Integration (MockMvc) | Webhooks + session health roles |
| `NotificationControllerTest` | Controller | Notification endpoints |
| `LearnerServiceTest` (nfe) | Unit | NFE learner service |
| `ProviderServiceTest` (nfe) | Unit | NFE provider service |
| `ParentAuthorizationServiceTest` | Unit | Parent-child authorization |
| `ParentPaymentServiceTest` | Unit | Payment/entitlement logic |
| `EventSecurityTest` | Security (MockMvc) | 401/403 on event + webhook paths |
| `StudentServiceTest` | Unit | Student service |
| `TeacherServiceTest` | Unit | Teacher service |

Frontend: Playwright e2e only (`test:e2e` script; 7 spec files: navigation, auth, dashboard redirect, responsive, accessibility). No unit-test runner configured (`__tests__/primary-pages.test.tsx` imports `@jest/globals` but Jest is not installed — NOT VERIFIED runnable). CI configuration: absent (no `.github/workflows`, no Jenkins/GitLab config found).

---

## 23. Backend Run Requirements

**Status: VERIFIED from compose + configuration (local defaults).**

| Requirement | Value | Required? |
|---|---|---|
| Java | 17+ (core runs on 17; media/realtime/workers declare 21) | Yes |
| Maven | 3.9+ (or `mvnw` wrapper) | Yes |
| PostgreSQL | 14+ (compose pins 16-alpine), database `elmkusoma` | Yes |
| Redis | 7 (compose `redis:7-alpine`, `:6379`) | Yes (configured; graceful degradation NOT VERIFIED) |
| RabbitMQ | 3.12 (compose `-management-alpine`, `:5672`) | Yes for workers/async paths |
| MinIO (S3-compatible) | `:9000`/`:9001`, bucket `elmkusoma-media` | For media service |
| LiveKit server | `:7880` (separate compose file) | For live classes |
| JWT secret | `JWT_SECRET` env (Base64) | Yes — core has no default |
| DB password | `DB_PASSWORD` env | Yes — core has no default |
| Node.js 18+ | Frontend only (Developer 02 scope) | For frontend |

---

## 24. Known System Issues

Only repository-verified items. Causes are not guessed.

1. **V60 migration naming question — RESOLVED, no action.** Only one V60 file exists (`V60__institution_admin_ecosystem.sql`). The secondary-stage/form migration is `V65__add_secondary_stage_and_form_to_users.sql`. Migration range V01–V69 is contiguous with no duplicates and no gaps. Do not edit either file.
2. **No database-level referential integrity.** The dominant pattern is raw UUID columns (~769 FK-style fields vs 6 read-only JPA joins). Orphaned rows are possible if application logic misses a case; there is no FK-constraint safety net.
3. **Inconsistent institution-source pattern.** Some controllers resolve the institution from the `X-Institution-Id` request header while others use the JWT-derived `institutionId` request attribute. Header-derived scoping is spoofable by any authenticated caller and must be audited endpoint-by-endpoint before production exposure.
4. **Certificate verify authorization layering.** `GET /verify/{code}` carries `@PreAuthorize("permitAll")` under a class-level teacher/admin rule and is also listed in `PUBLIC_URLS`. Public reachability is intended, but the effective behavior depends on filter-chain/method-security precedence — verify with a live 401/anonymous probe before relying on it.
5. **Dual status/type fields on `Event`.** `status` (String) vs `eventStatus` (enum), `eventType` (String) vs `eventTypeEnum` (enum) coexist; inconsistent use can produce conflicting state reads.
6. **ID-type mismatch across services.** Core uses UUID keys; the media service's `media_files` uses Long keys (`institutionId`/`userId` as Long). Cross-service joins must translate types.
7. **Duplicate concept tables.** Two `LearningGoal` entities (`learner_goals` learner-owned vs `learning_goals` parent-assigned); `Certificate` vs `NfeCertificate`; primary vs higher-ed portfolio/collaboration tables. Recent history shows active consolidation; treat each pair's ownership as distinct until migrations unify them.
8. **Dev-default secrets in auxiliary modules.** Workers/media/realtime configs contain hardcoded placeholder secrets and guest credentials. They must be overridden via environment in any non-local deployment.
9. **No CI pipeline.** Test execution is manual (`mvn test`, `playwright test`); regressions are not gated automatically.
10. **LiveKit not in the main compose file.** Live features require the separate `docker-compose.livekit.yml` stack; a default `docker compose up` does not provide a LiveKit server.

---

## 25. Document Status Matrix

| Area | Status | Evidence |
|---|---|---|
| System Overview | IMPLEMENTED | Root README + module/code inventory |
| Identity | IMPLEMENTED | `User`, `AuthController` (10 endpoints), token entities |
| Authorization | PARTIAL | Endpoint `@PreAuthorize` verified; object-level varies |
| Institutions | IMPLEMENTED | `Institution`, memberships, people/invite flows |
| Learners | IMPLEMENTED | `learner/` package, 38-endpoint controller, goals/bookmarks |
| Teachers | IMPLEMENTED | `teacher/` package, workspace + live console controllers |
| Learning | IMPLEMENTED | Course→Module→Lesson, resources, assignments |
| Enrollment | IMPLEMENTED | Class + course lanes, transfers |
| Assessment | IMPLEMENTED | Full attempt/result pipeline, objective auto-grading |
| Progress | IMPLEMENTED | Per-lesson %, averages, report cards |
| Live Backend | IMPLEMENTED | LiveKit service, 15 entities, webhooks, WS signaling |
| Events | IMPLEMENTED | Full lifecycle reusing live architecture |
| Media | IMPLEMENTED | Media service + MinIO + presigned flows |
| Certificates | IMPLEMENTED | Lifecycle + public verification + revocation |
| Payments | IMPLEMENTED (agnostic) | Payment entity, webhook, entitlements; no gateway SDK |
| Notifications | IMPLEMENTED | Service, preferences, realtime bridge; SMS/push senders NOT VERIFIED |
| Platform Admin | IMPLEMENTED | ~49-endpoint controller + commerce/moderation |
| Institution Admin | IMPLEMENTED | Dashboard, settings, roles, people, org profile |
| Oversight | IMPLEMENTED | Read-oriented controller + region/district model |
| Database | IMPLEMENTED | ~170 entities, BaseEntity, raw-UUID pattern documented |
| Flyway | IMPLEMENTED | V01–V69 contiguous; live-history comparison NOT VERIFIED |
| Security | PARTIAL | Mechanisms verified; uniform coverage not claimed |
| Testing | PARTIAL | 18 backend tests + e2e; no CI; frontend unit runner absent |

---

*End of Developer 01 document. Frontend, user journeys, and live UX detail: see Developer 02 (`docs/README-02-USER-FRONTEND-LIVE.md`).*
