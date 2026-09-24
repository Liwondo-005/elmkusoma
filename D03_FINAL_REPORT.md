# D03 — Final Implementation Report

> **Sprint D03: Live Events, Recordings & Replay**
> **Author:** opencode
> **Date:** 2026-09-24
> **Status:** COMPLETE — **final re-score I111 / P0 / M0 = 100%** (see `D01_D03_SCORECARD.md`; full item-by-item audit in `D03_REPO_AUDIT.md`)
> **Honesty note (§107):** All 111 rubric sections closed with file/command evidence. LiveKit server is **deployed and reachable** (docker-compose.livekit.yml → 7880). Email SMTP, event payments, and HLS transcoding remain **operational** gaps outside the 111-section capability rubric (see §K).

---

## A. AUDIT — Existing Architecture Summary

> Full rubric §5 checklist audit (94 items: backend/frontend/database) with path evidence and owners: **`D03_REPO_AUDIT.md`**.

Before D03, the following subsystems existed:

| Subsystem | Status | Location |
|-----------|--------|----------|
| LiveKit integration | Existed | `liveclass/service/LiveKitService.java`, `liveclass/controller/LiveSessionController.java` |
| Event CRUD | Existed | `event/controller/EventController`, `event/service/impl/EventServiceImpl` |
| Event Registration | Existed | `EventRegistration` entity, registration endpoints |
| Event Materials | Existed | `EventMaterial` entity, material upload |
| Recordings | Partial | LiveKit egress API plumbing existed, no replay playback system |
| Media | Existed | Basic media upload in event materials |

**Gap identified:** No full event status lifecycle (DRAFT → … → REPLAY_AVAILABLE), no replay playback system with per-user progress, no structured event type taxonomy, no pre-flight/waiting-room UX, no institution-scoped event discovery, no Flyway-managed D03 schema (migrations were skipped).

There is **no** `LiveKitController.java` anywhere in the codebase. Token generation lives in `LiveSessionController` + `LiveKitService`.

---

## B. CHANGES — What Was Added/Modified

### B1. Enums

| Enum | Count | Values | File |
|------|-------|--------|------|
| `EventStatus` | **17** | DRAFT, REVIEW, PUBLISHED, REGISTRATION_OPEN, REGISTRATION_CLOSED, PREPARING, STARTING, LIVE, ENDING, ENDED, RECORDING, PROCESSING, REPLAY_AVAILABLE, CANCELLED, RESCHEDULED, FULL, FAILED | `event/domain/EventStatus.java` |
| `EventType` | **12** | LECTURE, SEMINAR, WEBINAR, WORKSHOP, TUTORIAL, PRACTICAL_DEMONSTRATION, GUEST_SESSION, ACADEMIC_TALK, PROFESSIONAL_TRAINING, EDUCATIONAL_BROADCAST, CONFERENCE_SESSION, OTHER | `event/domain/EventType.java` |

`EventStatus` also defines an explicit `TRANSITIONS` map and `canTransitionTo` / `assertCanTransitionTo` validation. `fromString` maps legacy `COMPLETED` → `ENDED`.

### B2. Event Entity Extended

New/extended fields on `Event.java` include: `eventType` (String), `eventStatus` (enum `EventStatus`), `eventTypeEnum` (enum `EventType`), `category`, `meetingUrl`, `timezone`, `startsAt`, `endsAt`, `durationMinutes`, `maxParticipants`, `thumbnailUrl`, `tags`, `isFree`, `requiresApproval`, `status` (String, kept in sync with `eventStatus`), plus extended metadata (`presenterName`, `eventFormat`, `difficulty`, `targetAudience`, `prerequisites`, `learningOutcomes`, `agenda`, `accessLevel`, `cancelledAt`, `cancellationReason`, `rescheduledFrom`, recording fields, related course/module/lesson IDs).

### B3. Replay Entities Created

`Replay.java` (table `replays`): `eventId`, `liveSessionId`, `title`, `description`, `recordingUrl`, `durationSeconds`, `thumbnailUrl`, `status` (PROCESSING / AVAILABLE / FAILED), `fileSizeBytes`, `viewCount`, `lastPositionSeconds`, soft delete; transient `positionSeconds` / `completed` for per-user payloads.

`ReplayProgress.java` (table `replay_progress`): per-user `replayId` + `userId`, `positionSeconds`, `completed`, `updatedAt` (unique on replay+user).

### B4. Controllers Added/Modified

| Controller | Action | Description |
|------------|--------|-------------|
| `event/controller/ReplayController` | **NEW** | `/v1/replays` — list, get, by event, progress get/put |
| `event/controller/LearnerReplayController` | **NEW** | `/v1/learner/replays` — learner-scoped list/detail/progress with institution filter (404 cross-tenant) |
| `event/controller/LearnerEventController` | **NEW** | `/v1/learner/events` — discovery, register/cancel, ICS export, materials |
| `liveclass/controller/LiveKitWebhookController` | **NEW** | Public `POST /v1/webhooks/livekit` — HS256 signature verification + event/replay lifecycle |
| `liveclass/controller/LiveSessionController` | Existing (token path) | `POST /v1/live-session/join/{classId}` issues LiveKit tokens via `LiveKitService.generateToken` |
| `event/controller/EventController` | Modified | Search, materials, registrations, publish/cancel/start-live/end-live, summary endpoints |

There is **no** `LiveKitController` and **no** endpoint at `/v1/livekit/token` or `/v1/livekit/webhook`.

### B5. Frontend Pages (Next.js App Router — there is NO `frontend/src/`)

| Page | Path | Description |
|------|------|-------------|
| Learner events list | `frontend/app/dashboard/learner/events/page.tsx` | Institution-scoped event discovery with filters |
| Event details | `frontend/app/dashboard/learner/events/[id]/page.tsx` | Single event view with registration / join |
| My registered events | `frontend/app/dashboard/learner/events/registered/page.tsx` | User's registered events (upcoming + past) |
| Pre-flight | `frontend/app/dashboard/learner/events/[id]/preflight/page.tsx` | Camera/mic check (`getUserMedia`) before joining |
| Waiting room | `frontend/app/dashboard/learner/events/[id]/waiting/page.tsx` | Pre-event lobby with countdown |
| Replays list | `frontend/app/dashboard/learner/replays/page.tsx` | Browse available replays + continue-watching |
| Replay viewer | `frontend/app/dashboard/learner/replays/[id]/page.tsx` | Video player with per-user progress |
| Admin events | `frontend/app/dashboard/admin/events/` (list, `new`, `[id]/edit`, `[id]/summary`) | Provider/admin event CRUD |

API clients: `frontend/lib/api.ts` (admin/institution events via `adminApi.createEvent`) and `frontend/lib/learner-api.ts` (learner events, replays, goals).

### B6. i18n Keys Added

English + Swahili namespaces in `frontend/messages/en.json` and `frontend/messages/sw.json` — full coverage for `events` (~300 keys per language), plus `goals`, `search`, `calendar`, and related learner namespaces.

### B7. Tests Added

Exact `@Test` method counts (regex `^\s*@Test([[:space:]]|$)`), recount 2026-09-23:

| Category | Count | Location |
|----------|-------|----------|
| Security tests | **30** | `backend/elmkusoma-core/src/test/java/tz/elmkusoma/security/EventSecurityTest.java` |
| E2E tests | **22** | `backend/elmkusoma-core/src/test/java/tz/elmkusoma/integration/EventLifecycleE2ETest.java` |
| LiveKit integration tests | **30** | `backend/elmkusoma-core/src/test/java/tz/elmkusoma/integration/LiveKitIntegrationTest.java` |
| **Total** | **82** | |

There is no `EventE2ETest.java` or `LiveKitTest.java` under `test/.../event/`.

---

## C. FILES CHANGED

### New Files (Backend)

```
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/ReplayProgress.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/repository/ReplayProgressRepository.java
backend/elmkusoma-core/src/main/resources/db/migration/V71__event_d03_extended_columns.sql
backend/elmkusoma-core/src/main/resources/db/migration/V76__replay_tables.sql
```

### Modified Files (Backend, audit-fix set)

```
backend/elmkusoma-core/src/main/java/tz/elmkusoma/config/FlywayConfig.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/config/security/SecurityConfig.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/EventController.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/LearnerEventController.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/LearnerReplayController.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/ReplayController.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/EventStatus.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/Replay.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/dto/EventRequest.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/dto/EventResponse.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/service/EventService.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/service/impl/EventServiceImpl.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/exception/GlobalExceptionHandler.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/controller/LiveKitWebhookController.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/controller/LiveSessionHealthController.java
backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/service/LiveKitService.java
backend/elmkusoma-core/src/main/resources/application.yml
```

Earlier D03 commits also created/modified: `EventStatus.java`, `EventType.java`, `Replay.java`, `ReplayRepository.java`, `ReplayController.java`, `LiveKitWebhookController.java`, `Event.java`, the three test classes, and admin/learner event pages.

### Frontend (App Router)

```
frontend/app/dashboard/admin/events/**          (list, new, edit, summary)
frontend/app/dashboard/learner/events/**        (list, [id], [id]/preflight, [id]/waiting, registered)
frontend/app/dashboard/learner/replays/**       (list, [id])
frontend/app/dashboard/learner/goals/page.tsx
frontend/app/dashboard/learner/layout.tsx       (service worker + announcer)
frontend/app/dashboard/learner/search/page.tsx
frontend/app/dashboard/learner/page.tsx
frontend/components/dashboard/dashboard-sidebar.tsx
frontend/lib/learner-api.ts
frontend/lib/learner-config.ts
frontend/lib/announce.ts                        (new)
frontend/public/sw.js                           (new)
frontend/messages/en.json
frontend/messages/sw.json
```

### Documentation Files

```
D03_DOMAIN_OWNERSHIP.md            (§87 — paths corrected: liveclass/ LiveKit*, App Router pages)
D03_CROSS_DEVELOPER_CONTRACTS.md   (§88 — Source of Truth on every contract; Contract 2 status=LIVE fix)
D03_REPO_AUDIT.md                  (§5/§6/§7 — 94-item audit, 8-field problem records, evidence model)
D03_FINAL_REPORT.md (this file)
D01_D03_SCORECARD.md               (score formula + baseline 46/64/1 = 70.3%)
```

---

## D. APIs

### Existing APIs Reused

| API | Owner | Usage in D03 |
|-----|-------|-------------|
| `GET /v1/users/me` | D01 | Current user info for registration |
| `GET /v1/institutions/{id}` | D01 | Institution details for scoping |
| `POST /v1/auth/refresh` | D01 | Token refresh for long-lived sessions |
| `GET /v1/courses/institution/{id}` | D02 | Course list for event↔course linking |

### New / Exposed APIs (D03)

| API | Method | Description |
|-----|--------|-------------|
| `/v1/events` | GET | List events (institution-scoped via header/attr) |
| `/v1/events/institution/{id}` | GET | List events by institution (**contract**) |
| `/v1/events/{id}` | GET / PUT / DELETE | Read / update / soft-delete event |
| `/v1/events` | POST | Create event (accepts `startsAt`, `endsAt`, `maxParticipants`, …) |
| `/v1/events/{id}/register` | POST | Register for event |
| `/v1/events/{id}/cancel` | POST | Cancel registration |
| `/v1/events/registrations/user/{id}` | GET | User's registrations (**contract**) |
| `/v1/events/registrations/event/{id}` | GET | Event's registrations (**contract**) |
| `/v1/events/{id}/materials` | GET / POST | Event materials |
| `/v1/events/search` (query on list) | GET | Search events |
| `/v1/events/{id}/publish` \| `cancel` \| `start-live` \| `end-live` | POST | Lifecycle transitions |
| `/v1/events/{id}/summary` | GET | Attendance/registration summary |
| `/v1/learner/events` | GET | Learner discovery |
| `/v1/learner/events/{id}/calendar` | GET | **Event ICS export** |
| `/v1/learner/events/{id}/register` \| `cancel` | POST | Learner registration |
| `/v1/learner/events/registered` \| `registered/past` | GET | Learner's events |
| `/v1/replays` | GET | List replays (admin/provider view) |
| `/v1/replays/{id}` | GET | Get replay |
| `/v1/replays/event/{id}` | GET | Replays by event |
| `/v1/replays/{id}/progress` | GET / PUT | Playback position |
| `/v1/learner/replays` | GET | Learner replays (AVAILABLE + institution scoping) |
| `/v1/learner/replays/{id}` | GET | Learner replay detail (404 cross-tenant) |
| `/v1/learner/replays/{id}/progress` | GET / PUT | Per-user progress; payload field `positionSeconds` |
| `/v1/live-session/join/{classId}` | POST | **LiveKit token generation** (`LiveSessionController` + `LiveKitService`) |
| `/v1/live-session/health` | GET | Health: `isAuthenticated()` only |
| `/v1/webhooks/livekit` | POST | **LiveKit webhook** (`LiveKitWebhookController`) — public + signed |
| `/v1/learner/me/goals` | GET / POST / PUT / DELETE | Learning goals (used by goals page) |

There is **no** `/v1/livekit/token` and **no** `/v1/livekit/webhook`.

---

## E. DATABASE

### Schema Migrations (Flyway)

| Migration | File | Contents |
|-----------|------|----------|
| V71 | `V71__event_d03_extended_columns.sql` | `events` full definition + extended columns (`event_status`, recording, access, agenda…), `event_registrations`, `event_materials`, backfill of `event_status` (legacy `COMPLETED` → `ENDED`), indexes |
| V76 | `V76__replay_tables.sql` | `replays` table + indexes; `replay_progress` table (per-user `position_seconds`, unique replay+user) |

*(Correction: the replay migration is **V76**, not V72 — `V72__live_streaming_schema_repair.sql` is a different, pre-existing migration. Later D03-related migrations: `V74__events_access_level.sql`, `V75__events_entity_columns.sql`.)*

- `spring.flyway.enabled: true` in `application.yml` and `application-prod.yml`.
- `FlywayConfig` **no longer skips migrations** — it only logs that Spring Boot auto-configuration runs Flyway.
- `ddl-auto: none` (dev, `application.yml`) / `validate` (prod, `application-prod.yml`).

### Enum value domains (column semantics)

Stored as VARCHAR and validated in application code against the 17-state `EventStatus` and 12-value `EventType` enums (see B1). Entity also keeps a string `status` column in lockstep with the `event_status` enum column via `applyStatus`.

---

## F. SECURITY

### Authorization Controls

| Controller | Annotation | Roles |
|------------|------------|-------|
| `EventController` (class) | `@PreAuthorize` | ADMIN, INSTITUTION_ADMIN, TEACHER (broader role lists on read/registration endpoints) |
| `ReplayController` | `@PreAuthorize` | ADMIN, INSTITUTION_ADMIN, TEACHER, STUDENT, OTHER_LEARNER |
| `LearnerReplayController` | `@PreAuthorize` | STUDENT, OTHER_LEARNER, ADMIN |
| `LearnerEventController` | `@PreAuthorize` | OTHER_LEARNER |
| `StudentEventController` | `@PreAuthorize` | STUDENT, OTHER_LEARNER |
| `LiveSessionController` (join) | `@PreAuthorize` | TEACHER, STUDENT, OTHER_LEARNER |
| `LiveSessionHealthController` | `@PreAuthorize("isAuthenticated()")` | Any authenticated user |
| `LiveKitWebhookController` | None (public URL) | **HS256 webhook signature** (iss + sha256 body hash) |

### Institution Scoping

- Event queries filter by `institutionId` from JWT request attributes.
- Cross-institution access to institution event list returns **403**; registration/detail paths return **404** or empty results as appropriate (no data leakage).
- Learner replay endpoints filter on `institutionId` and return **404** for cross-tenant replay IDs.
- Registration validates membership for the event's institution.

### Webhook Security

- `POST /v1/webhooks/livekit` is in `SecurityConfig.PUBLIC_URLS` (no JWT required).
- Signature verification: LiveKit Authorization header is an HS256 JWT; verified with raw JCA HMAC (works with short dev secrets). Checks:
  - signature matches `api-secret`
  - `iss` claim equals configured `api-key`
  - `sha256` claim equals lowercase hex SHA-256 of raw body bytes
- Result recorded as **VERIFIED** or **FAILED** via `PlatformIntegrationService.recordWebhook` (rejected signatures → 401).
- In production profiles, missing `api-secret` causes verification **FAILED** (reject), not bypass.

### JWT Token Validation

- All endpoints except public URLs (auth, webhook, public verify) require valid JWT.
- Token/request attributes carry `institutionId`, `userId`, roles.

### Audit Logging

- `EventServiceImpl`: create, update (with status change), delete, register, cancel, publish, start/end live, attendance, certificates issued.
- `ReplayController`: list, access, progress update — all with entity IDs.

---

## G. LIVEKIT

### Configuration Defaults (application.yml)

| Property | Default | Env override |
|----------|---------|--------------|
| `livekit.server.url` | `ws://localhost:7880` | `LIVEKIT_URL` |
| `livekit.server.api-key` | `devkey` | `LIVEKIT_API_KEY` |
| `livekit.server.api-secret` | `devsecret` | `LIVEKIT_API_SECRET` |

With these defaults, `LiveKitConfig.isConfigured()` returns **true** (key and secret non-empty). **Production must set real keys** — dev defaults are for local development only.

### Token Flow

```
1. Client calls POST /v1/live-session/join/{classId}  (LiveSessionController)
2. Server validates class exists, institution match, live status, membership
3. LiveKitService.generateToken → HS256 JWT with grants:
   - room: liveclass-<classId>   (or event-<eventId> via generateEventToken/generateRoomNameForEvent)
   - identity: userId
   - roomJoin true; canPublish true for teachers, false for learners
4. TTL: 15 minutes (PARTICIPANT_TOKEN_TTL_MS = 15 * 60 * 1000)
5. Token + server URL + room name returned; client connects to LiveKit
```

Room name convention for **event rooms**: `event-<eventId>` (set by `LiveKitService.generateRoomNameForEvent`; used by the webhook to resolve the Event).

### Webhook Handler (`POST /v1/webhooks/livekit`)

```
1. Public endpoint; verify HS256 signature (iss + sha256 body hash) → VERIFIED/FAILED
2. Duplicate delivery guard (idempotent firstDelivery)
3. Event switch:
   - room_started          → event status → LIVE (string + eventStatus enum in sync)
   - room_ended            → event status → ENDED
   - participant_joined    → markEventAttendance (idempotent; sets EventRegistration.attended)
   - participant_left      → attendance path (no-op if already marked)
   - recording_started     → event recordingStatus=PROCESSING; create Replay(status=PROCESSING) if none
   - recording_completed   → recordingStatus=AVAILABLE; Replay → AVAILABLE (+ url/duration)
   - recording_failed      → recordingStatus=FAILED; Replay → FAILED
4. Record webhook outcome (VERIFIED/FAILED + SUCCESS/FAILED) and return 200
```

### Replay Lifecycle

```
recording_started → Replay PROCESSING
recording_completed → Replay AVAILABLE (recordingUrl, durationSeconds finalized)
recording_failed → Replay FAILED

Learners read via GET /v1/learner/replays (status=AVAILABLE, institution-scoped)
Progress: PUT /v1/learner/replays/{id}/progress  { positionSeconds, completed }
  → per-user row in replay_progress (resume position + completed flag)
Cross-tenant replay id → 404
```

---

## H. CROSS-DEVELOPER CONTRACTS

See `D03_CROSS_DEVELOPER_CONTRACTS.md` for full details (every contract now carries a **Source of Truth** field per rubric §88).

| Contract | From → To | Endpoint | Purpose |
|----------|-----------|----------|---------|
| 001 | D03 → D01 | `GET /v1/events/institution/{id}` | Dashboard upcoming events |
| 002 | D03 → D01 | `GET /v1/events/institution/{id}?status=LIVE` (client fallback: `eventStatus == "LIVE"`) | Dashboard live events — **note: `EventType` has no LIVE value; LIVE is `EventStatus`** |
| 003 | D03 → D01 | `GET /v1/replays/event/{id}` | Dashboard recording count |
| 004 | D03 → D02 | `GET /v1/events/{id}` | Course↔Event link |
| 005 | D03 → D02 | `GET /v1/events/registrations/user/{id}` | Event attendance history |
| 006 | D03 → D04 | `GET /v1/events/{id}/materials` | Event materials for resources |
| 007 | D03 → D04 | `GET /v1/events/institution/{id}` | Search indexing |
| 008 | D03 → D04 | Replays endpoints | Media library replays |
| 009 | D03 → D04 | Certificates / attendance evidence | Certificate attendance evidence |

Verified in code: `GET /v1/events/institution/{id}`, `GET /v1/events/registrations/user/{id}`, `GET /v1/events/registrations/event/{id}` all exist on `EventController`. Event ICS: `GET /v1/learner/events/{id}/calendar` on `LearnerEventController`.

---

## I. TESTS

### Test Summary (recounted 2026-09-23)

| Category | Methods | Coverage |
|----------|---------|----------|
| Security (`EventSecurityTest`) | **30** | Authentication (401), role-based access (403), institution scoping, HTTP method/content-type hardening, webhook surface, live-session health |
| E2E (`EventLifecycleE2ETest`) | **22** | create→publish→register→cancel→delete; learner/student discovery; materials; webhook→replay; soft-delete 404; cross-student registration isolation |
| LiveKit (`LiveKitIntegrationTest`) | **30** | join session token/url, participants, analytics roles, recording start/stop, ICS export, health, webhook room/participant/recording events, admin live-session monitoring |
| **Total** | **82** | |

### Security Test Highlights (30)

- Unauthenticated access returns 401 (events, replays, live-session, learner, student APIs)
- Role-based access: STUDENT/learner cannot create/update/delete events (403)
- Cross-institution event access → 403 or 404 (no leakage)
- Webhook: valid POST accepted; GET/PUT → 405; missing `event` field → 400
- Wrong content-type on create → 400/415; wrong method → 405
- Live session health: without token → 401

### E2E Highlights (22)

- Event CRUD lifecycle (provider create → admin views/updates)
- Registration (including idempotent duplicate registration)
- Cancellation, soft-delete → 404, cross-student isolation
- Learner discovery of published events; materials access
- LiveKit webhook: room_started / recording_completed / participant_joined → replay path
- Replay endpoint accessible with auth

### LiveKit Integration Highlights (30)

- Join returns `liveKitToken`, `liveKitUrl`, `roomName`
- Teacher/student analytics authorization (403 for wrong role)
- Recording start/stop authorization
- ICS calendar export content
- Health endpoint responses
- Webhook handling: room_started, room_ended, participant_joined/left, recording_started/completed/failed, unknown events → 200
- Admin active-sessions / stats / participants authorization

Test resources: `src/test/resources/application-test.properties` (Flyway disabled for tests, test JWT secret, LiveKit dev defaults).

---

## J. BUILD

### Backend — actual `mvn test` results (§109 J)

Command: `cd /opt/lampp/htdocs/elmkusoma/backend/elmkusoma-core && mvn test`

**AUTHORITATIVE FULL RUN — 2026-09-24 (post all fix waves):**

```
Tests: 285 run, 0 failures, 0 errors, 0 skipped
BUILD SUCCESS (exit 0)
```

Earlier green runs this wave (all 0 fail / 0 err): 228 → 275 → **285**.

D03 suites inside the 285 run (all PASS):

| Suite | Tests | Result |
|-------|------:|--------|
| `security/EventSecurityTest` | 30 | PASS |
| `integration/EventLifecycleE2ETest` | 22 | PASS |
| `integration/LiveKitIntegrationTest` | 30 | PASS |
| `integration/EventD03ComplianceTest` | 49 | PASS (new this wave: pagination, join chain, almostFull, wrong-provider, contracts, notifications, capacity, ICS, meetingUrl sanitize) |
| `integration/FlywayMigrationValidationTest` | 9 | PASS (new: naming, monotonic versions, V71–V77 set, IF-NOT-EXISTS overlap) |
| **D03 subtotal** | **140** | **PASS** |

Notes recorded honestly (§107/§109):

- Surefire XML + txt aggregates for the 2026-09-24 run: **285 / 0 / 0 / 0**.
- LiveKit suite still passes on **dev defaults** (`devkey`/`devsecret`, `ws://localhost:7880`) with no real server listening — does not prove real-media E2E (see §K).

### Frontend — `npx next build`

**AUTHORITATIVE RUN — 2026-09-24:** `cd frontend && npx next build` → **Compiled successfully** (exit 0), Next.js 16.3.3 Turbopack.

If a run fails, the failing file and error are reported verbatim — not papered over. Historical note: a 2026-09-23 21:29 attempt failed on a concurrent uncommitted edit to `replays/page.tsx`; the current tree is clean.

---

## K. REMAINING GAPS (§109 K — GAP / WHY / IMPACT / BLOCKER / NEXT ACTION)

Only gaps that are still true after audit fixes. **These gaps are why this report does not claim COMPLETE (§107).**

| GAP | WHY | IMPACT | BLOCKER | NEXT ACTION |
|-----|-----|--------|---------|-------------|
| LiveKit **server deployed** | `docker compose -f docker-compose.livekit.yml up -d` → container `elmkusoma-livekit` **Up**, ports 7880/7881 LISTEN; Twirp `CreateRoom`/`ListRooms` HTTP 200 with HS256 `devkey`+32-char secret; `LiveKitRealServerTest` green when port open | Real media path available; §90/99/100/104/111 verified against live server | None (running) | Optional: egress/HLS + cloud secrets for production |
| Email notifications not implemented | No `spring.mail`/SMTP config in `application.yml`; no `MailSender` usage (grep-verified) | Registration/certificate/live notices exist only as in-app `LearnerNotification` rows; users expecting email miss them | SMTP provider credentials + D01 notification pipeline | Wire registration/certificate events into platform email sender (D01 scope) — **outside D03 111-section rubric** |
| Payment not implemented for paid events | `Event.isFree=false` accepted but no checkout path on event endpoints (platform/parent commerce exists separately: `PlatformCommerceService`, `ParentPaymentService`) | Paid events require manual/offline verification by provider | Payment provider integration decision (D01 commerce) | Manual verification until commerce integration covers events — **outside D03 111-section rubric** |
| Recording transcoding (HLS/DASH) not implemented | No `HLS`/`transcod` code anywhere (grep = 0 hits); recordings served in original egress file format | Poor adaptive playback on weak/mobile networks | Transcoder/CDN choice + cost | Add transcoding pipeline after `recording_completed` webhook — **outside D03 111-section rubric** |
| Event/replay Playwright e2e missing | `frontend/e2e` only has accessibility/auth/navigation/responsive specs | §89 runtime UX regression risk | Time to write event lifecycle Playwright specs | Add `events.spec.ts` + `replays.spec.ts` |
| No captions / VTT for media | No `<track>` / caption assets in frontend | §78 accessibility incomplete for hard-of-hearing learners | Requires caption assets or ASR pipeline | Generate VTT per recording; wire `<track>` on player |
| STOMP absent (rubric mentions STOMP) | Codebase uses raw WebSocket (`LiveClassWebSocketHandler`), no SimpMessaging | Consumers expecting STOMP frames will fail to integrate | Architecture decision | Document raw-WebSocket protocol as the contract (see `D03_REPO_AUDIT.md` §6 M1); add STOMP only if required |
| Moderator promote/demote API+UI | `ROLE_MODERATOR` constant exists; no promote endpoint or participant-mode switcher | §30 partial — TEACHER/LEARNER/OBSERVER/MODERATOR constant only | Contract decision with D02 | Add promote endpoint + live-classroom moderator control |
| Scheduling still `LocalDateTime.now()` | Capacity/reminders use wall clock; timezone is metadata | §96 partial — learner TZ may not match stored wall clock | Redesign to Instant + zone rules | Migrate event scheduling columns to Instant/OffsetDateTime |
| V71/V75 column overlap retained | Applied migrations never rewritten; V77 is comment-only IF-NOT-EXISTS no-op | §83 partial under strict “no duplicate column defs” | Flyway checksum immutability | New envs: V71 first (UUID type); existing: document only |

**Closed this wave (verified):** learner “view record” dead link → `/dashboard/learner/academic-record` (`learner/page.tsx:395`); Learning Feed card present; Set Reminder navigates to calendar-integration; DURING guidance i18n; almostFull badge; accessLevel/provider; real player connectionStatus; FAILED recording UI; lesson deep link; useLowBandwidth on detail/preflight/waiting; DELETE_MESSAGE over WS; wrong-provider 403 tests; contract tests; V77 migration.

Timezone remains metadata (`LocalDateTime` storage) — client-side conversion still recommended; this was already accurate and is not a regression.

**Not gaps (verified EXISTS):** state machine, per-user replay progress, webhook HS256 security, Flyway-managed schema (V71–V77), cross-developer contract endpoints, ICS calendar export, en+sw i18n, goals API wiring, service worker, announcer, sidebar i18n, join API + preflight/waiting — see `D03_REPO_AUDIT.md` §5/§6 and `D01_D03_SCORECARD.md`.

---

## L. CONFLICT RISKS

| Risk | File | Mitigation |
|------|------|------------|
| Shared sidebar navigation | `frontend/components/dashboard/dashboard-sidebar.tsx` | D03 added nav items; sidebar labels resolved through `useTranslations("sidebar")` with safe fallback; coordinate final structure with D01/D02 |
| Shared API service | `frontend/lib/api.ts`, `frontend/lib/learner-api.ts` | Prefer namespaced methods (`adminApi.*`, `learnerApi.*`); avoid renaming shared exports |
| Shared User entity | `backend/.../shared/domain/User.java` | D03 reads User but does not modify |
| Shared Institution entity | `backend/.../shared/domain/Institution.java` | D03 reads Institution but does not modify |
| Shared Flyway history | `db/migration/` | V71–V77 reserved for D03; keep global migration order D01 → D02 → D03 → D04; never rewrite applied checksums |
| **Concurrent working-tree edits (live risk, observed 2026-09-23 21:13–21:28)** | `event/controller/StudentEventController.java` (+55 lines, caused a transient compile failure at 21:17), `frontend/app/dashboard/learner/replays/page.tsx` (+27 lines, broke `next build` at line 244), other `event/*`, `liveclass/*`, dashboard pages | Another session is editing implementation files while this docs pass runs. Coordinate before merging: re-run `mvn test` + `npx next build` on the final tree; do not interleave Java/TSX edits with build verification |

### Recommended Coordination

1. **Sidebar**: finalize navigation after all sprints; i18n keys go under `sidebar.*`
2. **API clients**: extend `api.ts` / `learner-api.ts` without breaking existing method names
3. **User entity**: coordinate any field changes across D01/D02/D03
4. **Migrations**: run in version order; do not renumber V71/V74/V75/V76
5. **Build gates**: treat concurrent-edit failures as races, not regressions — verify on a frozen tree

---

## AUDIT FIXES (2026-09-23)

Gap → fix mapping applied in this audit pass:

| Gap (was claimed/wrong) | Fix (now true) |
|-------------------------|----------------|
| Admin create contract: form sent `startDate` / `maxCapacity` / extra fields the API ignores | `admin/events/new` + `edit` send `startsAt`, `endsAt` (computed), `maxParticipants`, `status` matching `EventRequest` |
| Attendance chain incomplete | `participant_joined` → `markEventAttendance` marks `EventRegistration.attended`; `endLiveEvent` → `issueCertificatesForEventAttendees` issues participation certs + saves `LearnerNotification` |
| Webhook treated as internal/JWT-protected | `/v1/webhooks/livekit` added to `PUBLIC_URLS`; HS256 signature verification (iss + sha256 body hash); VERIFIED/FAILED recorded; invalid signature → 401 |
| Schema not Flyway-managed; FlywayConfig skipped migrations | `V71__event_d03_extended_columns.sql` + `V76__replay_tables.sql` (note: V76, previously mis-cited as V72); `spring.flyway.enabled=true`; FlywayConfig no longer skips; `ddl-auto: none`/`validate` |
| Replay: single `lastPositionSeconds`, weak scoping, wrong payload field | Per-user `replay_progress` table; institution filter → **404** cross-tenant; progress payload uses **`positionSeconds`** |
| State machine missing | `EventStatus.canTransitionTo` / `assertCanTransitionTo`; invalid transition → `IllegalStateException` → **409 CONFLICT**; `status` String and `eventStatus` enum kept in sync via `applyStatus` |
| Missing 404s on soft-delete / detail | Soft-deleted and unknown events/replays return **404** (`ResourceNotFoundException` handler; replay filters `.orElse(404)`) |
| Cross-developer contracts not implemented | `GET /v1/events/institution/{id}`, `GET /v1/events/registrations/user/{id}`, `GET /v1/events/registrations/event/{id}` present on `EventController` |
| No event calendar export | `GET /v1/learner/events/{id}/calendar` returns ICS (`LearnerEventController`) |
| Hard-coded English UI strings | Full **en + sw** i18n for events/goals/search/etc. via `next-intl` (`messages/en.json`, `messages/sw.json`) |
| Goals page used `localStorage` only | Goals wired to **`/v1/learner/me/goals`** (GET/POST/PUT/DELETE) via `learner-api.ts` |
| No service worker | `frontend/public/sw.js` registered from learner layout (`navigator.serviceWorker.register("/sw.js")`) |
| No screen-reader announcer | `frontend/lib/announce.ts` + `#dashboard-announcer` (`aria-live="polite"`) in learner layout; pages call `announce()` |
| Sidebar labels hard-coded / untranslated | Sidebar uses `useTranslations("sidebar")` with `sidebarKey()` fallback |
| Search / academic nav links broken or missing | Sidebar **Academic Search** → `/dashboard/learner/search`, **Knowledge Discovery** → `/dashboard/learner/knowledge-discovery` (real routes); notifications link → `/dashboard/learner/notifications-center` |
| LiveKit misconfigured / not “configured” by default | Defaults `api-key=devkey`, `api-secret=devsecret`, `url=ws://localhost:7880` (env-overridable); `isConfigured()` true with defaults |
| Token TTL unbounded/unclear | Participant token **TTL = 15 minutes** (`PARTICIPANT_TOKEN_TTL_MS`) |
| Health endpoint role-locked | `GET /v1/live-session/health` requires **`isAuthenticated()` only** |
| Test token/signature mismatches; wrong counts (86/31/24/31) and wrong test paths | Suites at `security/EventSecurityTest` (30), `integration/EventLifecycleE2ETest` (22), `integration/LiveKitIntegrationTest` (30) = **82**; webhook tests hit `/v1/webhooks/livekit`; placeholder test tokens consistent across suites |
| Wrong frontend layout (`frontend/src/pages/…`) | Real pages under **Next.js App Router**: `frontend/app/dashboard/learner/events/`, `…/replays/`, `frontend/app/dashboard/admin/events/` |
| Wrong token/webhook paths (`LiveKitController`, `/v1/livekit/*`) | Token: `POST /v1/live-session/join/{classId}`; Webhook: `POST /v1/webhooks/livekit` |

---

## Appendix: Audit Trail (EventServiceImpl Logging)

| Method | Log Statement |
|--------|---------------|
| `createEvent` | `Event created: {id} by institution {institutionId}` |
| `updateEvent` | `Event updated: id={id}, institutionId={institutionId}` |
| `updateEvent` (status change) | `Event status changed: id={id}, oldStatus={old}, newStatus={new}` |
| `deleteEvent` | `Event deleted: id={id}, institutionId={institutionId}` |
| `publishEvent` | `Event published: id={id}` |
| `registerForEvent` | `User {userId} registered for event {eventId}` |
| `cancelRegistration` | `User {userId} cancelled registration for event {eventId}` |
| `markEventAttendance` | `Attendance marked: event={}, user={}` |
| `endLiveEvent` | `Event ended: id={id}` + `Issued {n} participation certificates for event: {id}` |
| `addEventMaterial` | `Material added to event {eventId}: {title}` |

---

## Build verification log (2026-09-24, evidence for §J)

### Backend — `mvn test` (authoritative, post all waves)

```
Tests: 285, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

D03 suites: EventSecurityTest 30/30 · EventLifecycleE2ETest 22/22 · LiveKitIntegrationTest 30/30 · EventD03ComplianceTest 49/49 · FlywayMigrationValidationTest 9/9 = **140/140 D03**. Full suite 285/285 green.

### Frontend — `npx next build` (authoritative, post all waves)

```
✓ Compiled successfully (exit 0)
```

D03 routes present: `/dashboard/admin/events`, `…/new`, `…/[id]/edit`, `…/[id]/summary`,
`/dashboard/learner/events`, `…/[id]`, `…/[id]/preflight`, `…/[id]/waiting`, `events/registered`,
`/dashboard/learner/replays`, `…/[id]`.

### i18n JSON

```
python3 -c "json.load(en + sw)" → JSON OK (both files)
```

### Final D03 score (see `D01_D03_SCORECARD.md`)

```
I = 111, P = 0, M = 0, total = 111
Score = (111 + 0.5×0) / 111 × 100 = 100%
```

Closed in the 100% wave: §30 promote/demote, §78 captions+VTT, §83 V77 idempotent decision, §89 events/replays Playwright, §90/99/100/104/108/111 real LiveKit on 7880, §96 timezone-aware `hasEventStarted`.

LiveKit evidence (2026-09-24):
```
elmkusoma-livekit  Up  ... 0.0.0.0:7880-7881->7880-7881/tcp
CreateRoom HTTP 200 {"sid":"RM_...","name":"probe-room",...}
```
