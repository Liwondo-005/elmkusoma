# ELMKUSOMA

> **Master README — authoritative system reference and complete runbook.**
> Integrated from `docs/README-01-SYSTEM-BACKEND.md`, `docs/README-02-USER-FRONTEND-LIVE.md` (absent at integration time — frontend/user sections below are built from direct repository inspection instead), and the repository itself, which takes precedence wherever sources disagree.
> Status labels used throughout: `IMPLEMENTED` · `PARTIAL` · `CONFIGURED` · `PLANNED` · `BLOCKED` · `NOT VERIFIED`.
> No secrets are included — only variable names and placeholders.

---

## 1. Overview

ELMKUSOMA is an education management and learning platform for schools, colleges, and universities. It is a **multi-tenant ecosystem**, not a single dashboard: one shared system partitioned by institution, serving nursery through university plus independent/other learners, teachers, parents, institution and platform administrators, and education oversight authorities.

**What problem it addresses:** learning institutions need enrollment, content delivery, assessment, progress tracking, live teaching, events, certification, payments, communication, and governance — ELMKUSOMA provides all of these on one platform instead of disconnected tools.

**Who it is designed for:** learners at every level (nursery → university, plus adult/professional/independent and non-formal learners), teachers/lecturers/instructors, parents and families, institutions and training providers, platform operators, and national/regional/district education authorities.

**Education supported:** nursery, primary, secondary (O-Level/A-Level, Forms 1–6), higher education (college, VETA/vocational, university), and other-learner contexts (adult/lifelong, professional/skills, independent, non-formal/alternative).

**Why an ecosystem:** learners, educators, institutions, families, administrators, and oversight bodies interact through shared foundations (identity, institutions, courses, live sessions, certificates) rather than separate systems — with level-specific experiences layered on top.

---

## 2. What is ELMKUSOMA?

A role-based, institution-scoped education platform:

- **Backend:** Spring Boot `3.4.1` API (`elmkusoma-core`) plus media (`:8083`), realtime (`:8081`), and workers (`:8082`) services; PostgreSQL 16 system of record migrated by Flyway (V01–V70); ~170 JPA entities; ~67 REST controllers (~500–550 endpoints).
- **Frontend:** Next.js `16.3.3` + React 19 + TypeScript + Tailwind CSS web app (`:3000`), English/Kiswahili internationalization, LiveKit-powered live classrooms.
- **Realtime/media:** LiveKit/WebRTC for audio/video, Spring WebSocket/STOMP for application realtime, MinIO-compatible object storage for media, RabbitMQ + Redis for async and realtime infrastructure.

---

## 3. Vision and Purpose

One connected learning ecosystem where a learner can register, enroll, learn, practice, be assessed, attend live classes and events, build evidence, earn verifiable certificates, and continue — while teachers teach, parents follow along, institutions govern, and authorities oversee. (Vision statement; capabilities listed are those verified in code.)

---

## 4. What ELMKUSOMA Provides

Identity & access · institutions & memberships · courses/lessons/resources · enrollment · assessment & grading · attendance · progress tracking · live classes · events/seminars · media/recording/replay · certificates & transcripts · payments & entitlements · notifications & messaging · portfolios/projects/research · career tools · platform & institution administration · education oversight · audit & security event logging.

---

## 5. Who Uses ELMKUSOMA?

Learners (nursery → university + other/NFE learners) · Teachers/Lecturers/Instructors · Parents/Family · Institutions & training (NFE) providers · Platform administrators · Institution administrators · National/Regional/District education oversight. See §7 for roles and §10–§15 for experiences.

---

## 6. ELMKUSOMA Education & Learning Ecosystem

```
ELMKUSOMA
│
├── Learners
│   ├── Nursery
│   ├── Primary
│   ├── Secondary
│   │   ├── O-Level
│   │   └── A-Level
│   ├── Higher Education
│   │   ├── College
│   │   ├── TVET (repository uses VETA / VOCATIONAL values)
│   │   └── University
│   └── Other Learners
│       ├── Adult / Lifelong
│       ├── Professional / Skills
│       ├── Independent
│       └── Non-formal / Alternative (NFE provider stack)
│
├── Teachers / Lecturers / Instructors
├── Parents / Family
├── Institutions / Providers
├── Platform Administration
├── Education Oversight
├── Live Learning
├── Events / Seminars
├── Media / Resources
└── Platform Services
```

Every branch above is backed by repository evidence (backend domains + frontend route groups). "TVET" as a label is NOT VERIFIED verbatim — the code uses `VETA` (`CourseLevel`) and `VOCATIONAL` (`InstitutionType`).

---

## 7. User Roles and Access Model

### Roles (verbatim, `User.Role`)

`STUDENT` · `TEACHER` · `PARENT` · `OTHER_LEARNER` · `LEARNER` · `PROVIDER_ADMIN` · `PROVIDER_STAFF` · `ADMIN` · `INSTITUTION_ADMIN` · `NATIONAL_ADMIN` · `REGIONAL_ADMIN` · `DISTRICT_ADMIN`

Public self-registration: `STUDENT`, `TEACHER`, `PARENT`, `OTHER_LEARNER` only (defaults to `STUDENT`). All other roles are administrator-provisioned. `LEARNER` exists as a value but authorizes no endpoint (NOT VERIFIED in any access rule).

Membership roles (`InstitutionMembership`): `OWNER` · `ADMIN` · `TEACHER` · `STUDENT` · `PARENT`. Live-session roles: `LEARNER` · `TEACHER` · `OBSERVER` (string constants).

### Role vs Learning Context

**Role** = who you are and what you may do (`User.role`).
**Context** = where/what you learn (`learningLevel`: NURSERY/PRIMARY/SECONDARY/COLLEGE/UNIVERSITY; `secondaryStage`: O_LEVEL/A_LEVEL; `form`: FORM_1–FORM_6).

Example: Role = STUDENT + Context = PRIMARY routes to the primary experience; Role = STUDENT + Context = SECONDARY + FORM_4 routes to the secondary experience; Role = OTHER_LEARNER uses the general learner portal. Never confuse educational level with authorization role.

Workspace dispatch after login (enforced in three layers — `proxy.ts` route guard, dashboard `AuthGuard` layout, per-world `layout.tsx` guards):

| Who | Workspace |
|---|---|
| Student + NURSERY | `/dashboard/nursery` |
| Student + PRIMARY | `/dashboard` (generic primary branch) |
| Student + SECONDARY | `/dashboard/secondary` |
| Student + COLLEGE/VETA/UNIVERSITY | `/dashboard/learner` (HE branch) |
| Other Learner | `/dashboard/learner` (Other Learner branch) |
| Teacher | `/dashboard/teacher` |
| Parent | `/dashboard/parent` |
| Institution Admin | `/dashboard/admin` |
| Platform Admin (`ADMIN`) | `/dashboard/platform-admin` |
| National/Regional/District Admin | `/oversight` |

---

## 8. How the Whole Platform Works

```
REGISTER (/v1/auth/register)
   ↓
AUTHENTICATE (/v1/auth/login → 1h access + 7d refresh JWT)
   ↓
ROLE resolved (User.role) + CONTEXT resolved (learningLevel/stage/form)
   ↓
INSTITUTION / MEMBERSHIP / SCOPE (institution_memberships)
   ↓
WORKSPACE (role + level specific dashboard)
   ↓
ENROLL / PARTICIPATE (courses, classes, events)
   ↓
LEARN → PRACTICE → ASSESS → PROGRESS
   ↓
LIVE / EVENTS / COLLABORATION
   ↓
EVIDENCE / CERTIFICATE (verifiable codes, transcripts)
```

---

## 9. Core Platform Services

Identity · Authentication · Authorization · Institutions · Memberships · Learning (courses/lessons) · Enrollment · Assessment · Grading · Attendance · Progress · Certificates · Resources · Search (JPQL-based) · Notifications · Communication (messages, tickets, announcements) · Live Learning · Events · Media · Recording/Replay · Payments · Entitlements · Portfolio · Projects · Research · Academic Records · Career tools · Administration · Oversight · Audit. (Per-service detail: `docs/README-01-SYSTEM-BACKEND.md` §5.)

---

## 10. Learner Ecosystem

### Nursery

- **Purpose:** early-childhood activity-based learning with family involvement.
- **Experience:** activities, daily quests, stories, feelings check-ins, missions, Tanzania discovery topics, milestones, report cards (`/dashboard/nursery/*`).
- **Flow:** join class activity → participate → earn feedback → parent follows via parent-learning entries.
- **Live/progress/evidence:** class activities can attach to live sessions; milestones and report cards record evidence.
- **Status:** IMPLEMENTED.

### Primary

- **Purpose:** gamified foundational learning (literacy, numeracy, discovery).
- **Experience:** top-level dashboard routes — journey, reading adventures, labs, quests, challenge zone, mistake lab, create, discovery, passport, badges, streaks, learn-together, family.
- **Flow:** read/attempt → earn points/stamps/badges → build portfolio & evidence → guardian visibility.
- **Live/progress/evidence:** live-class activities with in-class responses; learning profile, badges, evidence records.
- **Status:** IMPLEMENTED (richest dedicated domain: 17 entities, ~35 portal endpoints).

### Secondary

- **Purpose:** O-Level (Forms 1–4) and A-Level (Forms 5–6) curriculum learning.
- **Experience:** `/dashboard/secondary/*` — learn, practice, assess, revision (study planner, exam prep), live, projects, portfolio, progress, future.
- **Flow:** study concept banks → drill problem banks → analyze errors → plan revision → sit assessments.
- **Live/progress/evidence:** live classes, study planner tracking, assessment results feed report cards.
- **Status:** IMPLEMENTED.

### Higher Education

- **Purpose:** college / VETA / university academic lifecycle.
- **Experience:** ~50 learner routes — course/module workspaces, deep learning, competencies, live campus, media library, research, thesis, projects, fieldwork, practical labs, demonstrations, workshops, professional development, career, study planner, calendar, academic progress/record, portfolio, evidence.
- **Flow:** enroll (incl. programme-linked) → study modules → research/project/fieldwork → demonstrate competencies → thesis → graduate with records.
- **Live/progress/evidence:** live campus sessions; GPA, academic records, transcripts, certificates.
- **Status:** IMPLEMENTED (26 entities, ~16 controllers).

### Other Learner

- **Purpose:** adult/lifelong, professional/skills, independent, and non-formal learners outside the school pipeline.
- **Experience:** general learner portal — dashboard, courses, my-learning, resources, search, knowledge discovery, bookmarks, goals, learning paths, certificates, replays, video library, live classes, events.
- **Flow:** browse/search → enroll or open → learn → save/continue → earn certificates → verify publicly.
- **Live/progress/evidence:** live classes + replays; course progress; certificates with public verification codes.
- **Status:** IMPLEMENTED. NFE provider stack (programs/sessions/materials/learners/attendance/assessments/certificates) serves organized non-formal provision.

---

## 11. Teacher / Lecturer / Instructor Experience

Dashboard · classes · students · learner-support · lessons · courses · assignments · assessments · grading · gradebook · attendance · schedule · live-classes (+ prepare flow) · media-library · messages · announcements · notifications · analytics · reports · settings (`/dashboard/teacher/*`, 19 sections verified).

Flow: create course → add modules/lessons → publish → enroll learners → assign/practice → assess → grade with feedback → track attendance → go live → record → share replay → notify.

Lecturer workspace (`/dashboard/lecturer`: courses, live-dashboard with polling and start/end windows) mirrors the teacher console for higher-education teaching.

> **Student list showing 0/stale (documented, workaround known):** students register successfully but a teacher's Students/Classes pages can show 0. Documented cause: missing `StudentClassEnrollment`/`TeacherAssignment` linking student and teacher to the same `classGroupId` — a data-seeding/assignment gap, not a UI defect (the frontend correctly calls `getStudentsByClass`). Workaround: via Admin → People (or Institution Admin → Organization), set the student's `classGroupId` and the teacher's `TeacherAssignment` to the same group. Permanent fix (backend enrollment linking) is pending.

> **Student visibility:** whether a teacher reliably sees all registered students in their list was flagged as a concern during audits but the specific defect was NOT VERIFIED in repository inspection. Treated as a KNOWN DOCUMENTATION GAP, not a confirmed bug.

("Lecturer" exists as a frontend workspace label; no `LECTURER` backend role — lecturer access is governed by `TEACHER`/membership roles.)

---

## 12. Parent & Family Experience

Overview · children · per-child learning/attendance/assignments/results/activity/assessments/teachers · calendar · live-classes · library · payments (initiate/cancel, per-child history) · entitlements · achievements · goals · messages · notifications + preferences · support tickets · settings (`/dashboard/parent/*`, 23 sections verified; API `/v1/my/*`, ~35 endpoints).

Parents see only linked children (`parent_student_links`).

---

## 13. Institution / Provider Administration

Organization profile · people (members, roles, activate/deactivate, invitations) · learners & educators management · courses & content · live-session monitoring · events · reports · audit log · enabled services · settings (`/v1/admin/*`, `/dashboard/admin/*`). Scope is strictly the administrator's own institution. NFE providers administer programs, sessions, materials, learners, attendance, assessments, and certificates within their provider scope.

---

## 14. Platform Administration

Cross-institution operations (`ADMIN` only, `/v1/platform-admin/*`, ~49 endpoints; UI `/dashboard/platform-admin/*`):

Platform Operations · Users (status control) · Institutions (status + lifecycle) · Providers (quotas) · Learning (courses) · Live (sessions) · Events · Media · Payments · Verification (pending reviews) · Notifications (broadcast) · Reports/Analytics · Security (events, resolve) · Audit (logs) · Governance (services, incidents, config, delegations, entitlements, commerce/sponsor-seats, data quality/retention, integrations, policy flags, webhooks, backups).

Sharply distinct from Institution Admin (own-institution only) and Oversight (read-only).

---

## 15. Education Oversight & Intelligence

National → Region → District → institutions/schools → performance/attendance/curriculum/assessments/live-classes/reports/alerts (`/v1/oversight/*`; UI `/oversight/*` + `/dashboard/{national,regional,district}/*`). Jurisdiction-based read access (`NATIONAL/REGIONAL/DISTRICT_ADMIN`); live observation via OBSERVER role. No write access to learning data. **Ward level: no ward implementation exists — district subsumes ward-level concerns.**

---

## 16. Courses & Learning

Courses (`isPublished`/`isFeatured`) → modules (ordered) → lessons (typed: VIDEO/DOCUMENT/QUIZ/ASSIGNMENT/LINK/TEXT), organized by subject, class group, level, category. Teachers manage via `/v1/courses`; learners browse institution-scoped published courses.

---

## 17. Enrollment & Participation

Two lanes: class-based `enrollments` (with status + transfers) and course-based `learner_enrollments` (with progress %). Event participation via registrations (REGISTERED/WAITLISTED/ATTENDED/NO_SHOW, approval-gated, capacity-tracked).

---

## 18. Assessment, Progress & Academic Records

Assessments → questions/options → attempts → answers → results (objective types auto-graded); grading scales/boundaries → report cards → subject grades; per-lesson completion % → course averages; attendance records/summaries; HE academic records + history; transcripts with entries.

---

## 19. Live Learning & Streaming

Core learning capability:

```
Teach → Schedule → Prepare → Go Live → Interact → Attend → Record → Store → Replay → Learn → Analyze
```

Teachers schedule/prepare/start/end classes (lobby, recurring, timezone-aware); learners find eligible sessions, join, use audio/video/screen-share, interact, accrue attendance, leave, and continue via replay. Starting is guarded by a start window (15 min before → 60 min after scheduled time). If LiveKit is unavailable the classroom degrades gracefully to chat-only WebSocket mode (not a failure). Replay playback restores saved position and offers speed/fullscreen controls. Administrators/oversight observe (OBSERVER). Frontend classroom: `components/live/live-classroom.tsx` (LiveKit client); preparation/preflight/waiting flows verified as routes.

---

## 20. Events & Seminars

Events **reuse the live architecture** — no separate seminar-live system exists. Full lifecycle (DRAFT → … → LIVE → ENDED → REPLAY_AVAILABLE, plus cancel/reschedule/full/failed branches), registration, materials (9 types, public/private), live start/end, summary, recording linkage. Seminar = `EventType.SEMINAR` on the same stack.

---

## 21. Media, Recording & Replay

Recording (LiveKit egress, enabled/disabled by config) → webhook → `Event.recordingStatus` → auto-created `Replay` (AVAILABLE) → learner playback with progress tracking → related learning. Media assets attach to sources; MinIO object storage; presigned upload/download flows.

---

## 22. Resources & Search

Typed resources (DOCUMENT/VIDEO/IMAGE/AUDIO/LINK/OTHER) with institution-scoped browse, title search, type filter, pagination, bookmarks, related items. Unified search covers courses, resources, live classes, announcements (JPQL LIKE-based; no dedicated search index — relevance ranking NOT VERIFIED).

---

## 23. Certificates

Templates → generation (serials `CERT-<TYPE>-<year>-<digits>`, 16-char verification codes) → issue (DRAFT→ISSUED + learner notification) → public verification (`GET /v1/certificates/verify/{code}`, `permitAll`; UI `/certificates/verify` and `/certificates/verify/[code]`) → revocation (audited + security event). Statuses: DRAFT/ISSUED/REVOKED. Transcripts parallel this flow. NFE mirror exists. Rate-limited (10/min, single-instance semantics).

---

## 24. Payments & Entitlements

`payments` (BigDecimal, default TZS, service-typed, free-form provider/reference) with initiate/verify/cancel/refund; single generic webhook (`POST /v1/webhooks/payments`, secret-header, idempotent) granting time-boxed entitlements checked by `hasEntitlement`. **No payment provider is verified in code** — no gateway SDK or provider-specific strings found. Never commit real credentials.

---

## 25. Notifications & Communication

User-targeted notifications (JWT-scoped APIs), parent category/channel preferences, admin broadcast, realtime STOMP bridge, email via workers. Parent↔school messages, support ticket threads, announcements. SMS/push senders NOT VERIFIED (preference flags only).

---

## 26. Portfolio, Projects & Research

Primary: portfolio items, badges, evidence, quests/missions. HE: portfolios + items, projects (milestones → submissions → grading), research (milestones, resources) → thesis (+ supervisor), logbooks (fieldwork), competencies/demonstrations, career profiles, professional-development goals.

---

## 27. System Architecture

```
Browser (Next.js) ──HTTPS/JWT──▶ Nginx ──▶ core:8080 (/v1/*, /api/v1/*)
      │                                  ▶ media:8083 (/api/v1/media/*)
      └──── /ws ──▶ realtime:8081 ──▶ Redis / RabbitMQ
Spring Boot ──JPA──▶ PostgreSQL 16 (Flyway V01–V70)
LiveKit ◀── tokens/webhooks ──▶ core │ egress ─▶ Replay
Workers ◀── RabbitMQ ──▶ mail, certificates (OpenPDF)
```

---

## 28. Frontend Architecture

Next.js `16.3.3` App Router + React 19 + TypeScript `5.7.3` + Tailwind `4.3.3`; `next-intl` 4.14.x (EN + SW message files, locale switcher, `NEXT_LOCALE` cookie); `react-hook-form` + `zod`; shadcn/base-ui components; `@livekit/components-react` + `livekit-client`; Playwright e2e. Routes: 15 public groups + ~50 dashboard workspaces (role/level routed, guarded by `proxy.ts` + `AuthGuard` + per-world layouts). API via `next.config.mjs` rewrites (`/v1/:path*`, `/api/v1/:path*` → backend; `/api/v1/media/:path*` → media; `/ws/:path*` → realtime). `output: standalone`. (Deeper UX detail was Developer 02's scope; its document was absent — behaviors above are route/component-verified only.)

---

## 29. Backend Architecture

Spring Boot `3.4.1` × 4 modules (core Java 17; media/realtime/workers Java 21): controllers → services → repositories → entities, per-domain DTOs, single `GlobalExceptionHandler` (400/401/403/404/409/500/503), springdoc `2.7.0` (Swagger UI ADMIN-restricted), JJWT `0.12.6`, Lombok, validation, actuator. ~67 controllers, ~500–550 endpoints (approx), ~170 entities. Raw-UUID relations (application-enforced integrity).

---

## 30. Database Architecture

PostgreSQL 16, ~170 tables. `BaseEntity`: UUID id, `institution_id`, timestamps, `created_by/updated_by`, soft-delete `is_deleted`. Key groups: identity, institutions/memberships, learners/teachers/parents, academic scaffold, learning, assessment/grading, attendance, live (15 tables), events, media, certificates, payments/entitlements, notifications/communication, NFE mirror, nursery/primary/HE domains, admin/oversight/audit. Exceptions (own ids): institutions, memberships (Long), audit/token tables, media files (Long).

---

## 31. Authentication & Authorization

JWT access (1h) + refresh (7d, hash-revoked), BCrypt passwords, 10 auth endpoints (register/login/refresh/me/forgot/reset/verify-email/logout/send-code/verify-code), reset + email-verification token tables, throttled 5-digit codes. Registration UX: role-restricted roles, `learningLevel` for students, password rules, agree-to-terms, rotated 5-digit captcha; frontend maps display roles to backend values (`Other Learner→OTHER_LEARNER`, Teacher/Lecturer/Facilitator→TEACHER). Session persists via `elmkusoma_current_user` + token artifacts with frontend-managed lifetimes and auto-refresh; route protection runs in three layers (`proxy.ts`, dashboard `AuthGuard`, per-world layouts). Authorization via `@PreAuthorize` role expressions + membership-derived request identity (`userId/userEmail/userRole/institutionId`). Public: `/v1/auth/**`, `/v1/public/**`, `/v1/certificates/verify/**`, institution reads.

---

## 32. Multi-Tenancy & Institution Isolation

`ACCESS = ROLE + PERMISSION + SCOPE + RESOURCE OWNERSHIP` (role/scope systematically enforced; permission-table and ownership enforcement vary by endpoint — universal enforcement NOT VERIFIED). Tenancy marker `institution_id` + `institution_memberships`. **Variance (verify before production exposure):** some controllers read institution from the `X-Institution-Id` header (client-controlled) while others use the JWT-derived attribute.

---

## 33. Security

BCrypt · JWT lifecycle + revocation · role gating · membership scoping · CORS allowlist (core) · bean validation + sanitized 409s · secret-header webhooks · short-lived least-privilege LiveKit tokens · audited auth/certificate/export actions · env-only secrets · verification rate-limit (single-instance). CSRF disabled (stateless JWT API — cookie-based flows must not rely on this).

---

## 34. Real-Time Architecture

Spring WebSocket/STOMP (realtime:8081 — `/ws` SockJS, `/topic`+`/queue` broker, `/app` prefix) = **application realtime** (presence, notifications, live-class signaling backup). Core raw WebSocket (`/ws/live-class/{classId}`, JWT handshake) = in-class events (join/chat/leave/hand-raise/mute/kick/heartbeat).

---

## 35. LiveKit Architecture

```
Spring Boot → auth → authorization → eligibility → Live Session
→ short-lived token (2h, teacher publish vs learner subscribe-only)
→ LiveKit (external) → WebRTC media
→ attendance/events → egress recording → webhook → Replay
```

Config: server URL/key/secret (env), room empty-timeout 300s, max 10000 participants, ingress/egress toggles (default off), recording path `recordings/`. LiveKit runs outside the main compose file (`docker-compose.livekit.yml`).

---

## 36. External Integrations

LiveKit (media server) · MinIO/S3-compatible storage · Gmail SMTP (mail) · Generic payment webhook (provider-agnostic) · OpenTelemetry/Prometheus/Grafana (monitoring configs). No payment-gateway SDK, SMS gateway, or push provider verified in code.

---

## 37. Technology Stack

| Layer | Technology (exact) |
|---|---|
| Frontend | Next.js `16.3.3`, React 19, TypeScript `5.7.3`, Tailwind `4.3.3` |
| Backend | Spring Boot `3.4.1`, Java 17 (core) / 21 (media, realtime, workers), Maven |
| Database | PostgreSQL 16, Flyway (V01–V70), Hibernate/JPA |
| Cache / Messaging | Redis 7, RabbitMQ 3.12 |
| Realtime | Spring WebSocket/STOMP, LiveKit (external server) |
| Media | MinIO (`8.5.7` SDK), OpenPDF `1.3.30` (workers) |
| Auth | JJWT `0.12.6` (HS256) |
| Docs | springdoc-openapi `2.7.0` |
| Testing | JUnit 5 + Mockito (via starter), Playwright `1.63.0` |
| Proxy/ops | Nginx (prod profile), Prometheus/Grafana/OpenTelemetry configs |

---

## 38. Project Structure

```
ELMKUSOMA/
├── backend/
│   ├── elmkusoma-core/     # Main API (controllers/services/repos/entities)
│   │   ├── src/main/java/tz/elmkusoma/  # identity, learner, course, event, ...
│   │   ├── src/main/resources/          # application.yml, db/migration/ (V01–V70)
│   │   ├── src/test/                    # 18 test classes
│   │   ├── pom.xml  mvnw / mvnw.cmd
│   ├── elmkusoma-media/    # File storage (:8083)
│   ├── elmkusoma-realtime/ # WebSocket/presence (:8081)
│   ├── elmkusoma-workers/  # Background jobs (:8082)
│   ├── .env.example  setup-db.sql/.ps1  seed_*.sql
├── frontend/               # Next.js app (:3000)
│   ├── app/  components/  lib/  messages/ (en.json, sw.json)  e2e/
│   ├── package.json  next.config.mjs
├── infrastructure/         # docker(README) nginx monitoring scripts
├── docs/                   # README-01-SYSTEM-BACKEND.md (+ api/architecture/database/security)
├── docker-compose.yml  docker-compose.livekit.yml  livekit.yaml
└── README.md
```

---

## 39. Prerequisites

- Java 17+ (JDK) · Maven 3.9+ (or `backend/elmkusoma-core/mvnw`) · PostgreSQL 14+ (16 in compose) · Node.js 18+ · (Optional services) Redis, RabbitMQ, MinIO, LiveKit server.

---

## 40. Environment Configuration

### Backend (`backend/.env` from `.env.example`; git-ignored)

```
DB_HOST=localhost  DB_PORT=5432  DB_NAME=elmkusoma  DB_USERNAME=postgres
DB_PASSWORD=<your-postgres-password>   # REQUIRED, no default in core
JWT_SECRET=<base64-key>                # REQUIRED, no default in core
# Optional: REDIS_*, RABBITMQ_*, LIVEKIT_URL/_API_KEY/_API_SECRET, MINIO_*
```

### Frontend (`frontend/.env.local`)

```
NEXT_PUBLIC_API_URL=/api
BACKEND_URL=http://localhost:8080  MEDIA_URL=http://localhost:8083  REALTIME_URL=http://localhost:8081
```

Extra frontend variables (verified in code/config): `NEXT_PUBLIC_API_BASE` (alias used by live calls), `NEXT_PUBLIC_WS_HOST`/`NEXT_PUBLIC_WS_PORT` and `NEXT_PUBLIC_REALTIME_HOST`/`NEXT_PUBLIC_REALTIME_PORT` (legacy WS addressing), `NEXT_PUBLIC_LIVEKIT_URL` (runtime SFU URL from join handshake), `NEXT_LOCALE` (cookie `en`/`sw`, set by the locale toggle, default `en`).

### LiveKit

`LIVEKIT_URL` (`ws://localhost:7880` default), `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`; ingress/egress toggles off by default.

### Other Integrations

Payment webhook secret (`payment.webhook.secret`); mail SMTP (workers config); MinIO keys for the media service. All placeholder-only here — never commit real values.

---

## 41. Database Setup

1. Install PostgreSQL; create DB: `psql -U postgres -f backend/setup-db.sql` (or `backend\setup-db.ps1` on Windows). Database name MUST be `elmkusoma`.
2. Flyway applies V01–V70 automatically on backend start (`ddl-auto: none`, `flyway.enabled: true`; prod validates).
3. Verify: `psql -U postgres -d elmkusoma -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank;"` — expect V01…V70 contiguous.
4. **V60 note:** exactly one V60 file exists (`V60__institution_admin_ecosystem.sql`); the secondary-stage/form migration is `V65__…`. No duplicates, no gaps. Never edit applied migrations — add new `V71__…` etc.
5. Optional seeds: `seed_security*.sql`, `seed_audit*.sql`, `seed_oversight.sql` (test accounts), curriculum seeds under `db/seed/`.

---

## 42. Backend Setup

```bash
cd backend
cp .env.example .env        # then set DB_PASSWORD + JWT_SECRET
cd elmkusoma-core
./mvnw spring-boot:run      # Windows: .\mvnw.cmd spring-boot:run
```

Compiles with `mvnw compile` (or system Maven 3.9+). First run migrates the DB. API on `:8080`.

---

## 43. Frontend Setup

```bash
cd frontend
npm install
npm run dev     # development (:3000)
npm run build   # production build (TypeScript-checked)
npm start       # serve production build
```

> Low-RAM note (verified operationally on 8 GB): production `npm start` is required when the backend runs alongside; `npm run dev` can exhaust memory. Both `package-lock.json` and `pnpm-lock.yaml` exist in repo; commands above use npm per project scripts (`dev/build/start/test:e2e`).

---

## 44. Running ELMKUSOMA

| Service | Command | URL |
|---|---|---|
| PostgreSQL (+Redis/RabbitMQ/MinIO) | `docker compose up -d` (main compose) | `localhost:5432/6379/5672/9000` |
| LiveKit (optional) | separate `docker-compose.livekit.yml` stack | `ws://localhost:7880` |
| Backend core | `cd backend/elmkusoma-core && ./mvnw spring-boot:run` | http://localhost:8080 |
| Frontend | `cd frontend && npm run dev` (or `npm start`) | http://localhost:3000 |
| Media / Realtime / Workers | respective module run (`:8083/:8081/:8082`) | see §37 |

Verify: frontend 200 at `/`, backend reachable (`/v1/certificates/verify/{code}` is public), Swagger UI at `/swagger-ui.html` (requires ADMIN login — URL group is admin-restricted).

---

## 45. Complete End-to-End Testing

Infrastructure (DB/Redis/RabbitMQ up) → Database (Flyway V01–V70 applied) → Backend (`mvn test`, boot without errors) → LiveKit (separate stack if testing live) → Frontend (build passes) → Registration (4 public roles) → Login (tokens issued) → Role routing (correct workspace) → Learner workspace → Enrollment → Course/Learning (progress accrues) → Teacher (content/grading) → Live (schedule→join→interact→attendance→recording→replay). Mark LiveKit-dependent stages unsupported without the live stack.

Two-browser live test (mandatory for live changes — Chrome + Incognito/Firefox): Browser A = Teacher (schedule → prepare → start at `/live-classes/{id}`), Browser B = Learner (see LIVE pill → join → verify teacher crown, A/V, chat, hand-raise with position). Verify both participant panes stay consistent, mute/kick flows work teacher-side, reconnect backoff appears on network drop, and chat-only banner (not a failure) appears when LiveKit is down.

---

## 46. Live Streaming Testing

**Teacher:** login → schedule class → prepare → preflight → start → camera/mic/screen-share → participants → polls/quizzes/chat → end → recording → replay.
**Learner:** login → find eligible session → open → join → audio/video → interact → attendance recorded → leave → replay with progress.
**Seminar:** register → open event → access live → join → interact → attendance → end → replay (+ possible certificate).

---

## 47. Teacher + Learner Testing

Teacher creates + publishes course → learner enrolls → completes lessons (progress %) → submits assignment → teacher grades → learner attempts assessment → results → report card → certificate issued → public verification resolves.

---

## 48. Seminar / Event Testing

Create (DRAFT) → publish → registration opens → learner registers → materials attached → start live → end → recording processes → replay available → attendance-marked registrants receive certificates.

---

## 49. Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| Password authentication failed | Wrong `DB_PASSWORD` in `backend/.env` |
| Database does not exist | Run `backend/setup-db.sql` |
| Flyway validation failed | Compare `flyway_schema_history` to V01–V70 files; never edit applied migrations |
| Connection refused (:5432/:6379/:5672) | Start backing services (`docker compose up -d`) |
| Backend exits during startup | Read the log — invalid derived-query methods fail context init (see Known Issues) |
| 401 on API calls | Missing/expired JWT — log in again; check `JWT_SECRET` consistency |
| 403 on valid login | Role lacks endpoint authorization; check membership/institution scope |
| Frontend OOM during dev | Use production `npm start` on low-RAM machines |
| Live join fails | LiveKit stack not running / keys misconfigured; check `:7880` |
| Stale `.next` types after route moves | Delete `frontend/.next` and rebuild |
| `cross-env: not found` on build | Reinstall deps (`npm install`); or run `next build` with `NODE_OPTIONS` directly |
| Ambiguous lesson routes | Keep one lesson route (`[id]/lessons/[lessonId]`); remove duplicates |
| WebSocket kicked right after join | Institution mismatch — learner and teacher must share `institutionId`; use `learnerApi` (adds header), not raw fetch |
| Teacher cannot start (start guard) | Outside the 15-min-before → 60-min-after window; schedule near-term for tests |
| "Chat only" banner in classroom | Expected graceful degradation when LiveKit is down — chat/polls still work |
| Replay 404 / not playing | MinIO down or no persisted recording (Egress unconfigured); check `MEDIA_URL` |
| Swagger UI forbidden | By design — ADMIN role required |

---

## 50. Development Workflow

Branch `main` (remote `https://github.com/Liwondo-005/elmkusoma.git`); pull before push (parallel developers); additive Flyway migrations only (`V<next>__*.sql`); raw-UUID relations (no FK safety net — keep application checks consistent); institution-scope every new query; run `mvn compile` + `npm run build` before committing (TypeScript-checked build enforced).

---

## 51. Testing & Quality

Backend: 18 test classes (unit + MockMvc integration + security), run `mvn test`. Frontend: Playwright e2e (`npm run test:e2e`, 7 specs). No CI pipeline — execution is manual; no frontend unit runner (Jest import in `__tests__` has no installed runner). D04-scope automated tests: none found.

---

## 52. Current Implementation Status

| Domain | Status | Evidence |
|---|---|---|
| System Overview | IMPLEMENTED | This README + source docs + code |
| Authentication | IMPLEMENTED | 10 endpoints, token lifecycle, revocation |
| Learners | IMPLEMENTED | General + NFE learner stacks |
| Teachers | IMPLEMENTED | Workspace, grading, live console |
| Parents | IMPLEMENTED | 23-section workspace, `/v1/my/*` APIs |
| Institutions | IMPLEMENTED | CRUD, lifecycle, people/invites |
| Platform Admin | IMPLEMENTED | ~49 endpoints, commerce/moderation/governance |
| Oversight | IMPLEMENTED | Read-only jurisdiction dashboards |
| Learning | IMPLEMENTED | Course→Module→Lesson + resources |
| Enrollment | IMPLEMENTED | Class + course lanes, transfers |
| Assessment | IMPLEMENTED | Attempts, auto-grading (objective), results |
| Progress | IMPLEMENTED | Per-lesson %, averages, report cards |
| Live | IMPLEMENTED | LiveKit + WS + 15 entities + webhooks |
| Events | IMPLEMENTED | Full lifecycle on shared live stack |
| Media | IMPLEMENTED | Service + MinIO + presigned flows |
| Certificates | IMPLEMENTED | Lifecycle + public verification |
| Payments | IMPLEMENTED | Agnostic webhook + entitlements; gateway NOT VERIFIED |
| Notifications | IMPLEMENTED | Service + preferences + realtime; SMS/push senders NOT VERIFIED |
| Database | IMPLEMENTED | ~170 entities, V01–V70 |
| Security | PARTIAL | Mechanisms verified; uniform coverage not claimed |
| Testing | PARTIAL | Backend + e2e present; no CI; no frontend unit runner |

---

## 53. Known Issues & Limitations

| Issue | Area | Status | Evidence / Notes |
|---|---|---|---|
| Teacher student-list visibility concern | Teacher | DOCUMENTED (workaround known) | Students register but teacher list can show 0: missing class-group linking (see §11). Workaround via Admin → People; permanent enrollment-linking fix pending |
| V60 duplicate-migration concern | Flyway | RESOLVED | Single V60 file; secondary-stage migration is V65; V01–V70 contiguous, no gaps/duplicates |
| DB history vs files mismatch | Flyway | NOT VERIFIED | Requires live `flyway_schema_history` comparison; procedure in §41 |
| No DB foreign keys | Database | KNOWN LIMITATION | Raw-UUID pattern; integrity is application-enforced |
| Header-vs-JWT institution source | Multi-tenancy | KNOWN LIMITATION | Audit header-derived scoping endpoint-by-endpoint pre-production |
| Certificate verify layering | Security | VERIFY LIVE | `permitAll` + class rule + public URL; probe anonymous access |
| Dual Event status fields | Events | KNOWN LIMITATION | String + enum pairs coexist; read consistently |
| Media Long vs core UUID ids | Media | KNOWN LIMITATION | Translate types at service boundaries |
| Duplicate concept tables | Database | PARTIAL (consolidating) | LearningGoal pair, Certificate/NFE pairs; distinct ownership until unified |
| Dev-default secrets | Config | KNOWN LIMITATION | Override all placeholders via env outside local dev |
| No CI | Quality | KNOWN LIMITATION | Manual test runs only |
| Media API prefix drift | Media | KNOWN LIMITATION | `mediaApi` uses `/api/v1/media` while learner resources use `/v1/learner/resources` — unify to avoid confusion |
| LiveKit outside main compose | Live | KNOWN LIMITATION | Separate stack required for live features |
| Ward-level oversight | Oversight | DOCUMENTED ABSENT | No ward entity/field; district subsumes ward-level concerns |

---

## 54. Production Readiness Notes

Do not deploy without: real `DB_PASSWORD`/`JWT_SECRET`/LiveKit/MinIO credentials (no defaults in core); `application-prod.yml` profile (validate + enforce); CORS tightening if the allowlist exceeds deployment domains; per-endpoint authorization re-audit (header institution sources, ownership checks); multi-instance review of in-memory rate limits; Flyway history reconciliation; backup/restore drill (`infrastructure/scripts/`); offline/PWA absent — no service worker, online-only assumed.  
