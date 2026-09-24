# D03 — Full Repository Audit (Rubric §5 / §6 / §7)

> **Sprint D03: Live Events, Recordings & Replay**
> **Author:** opencode
> **Date:** 2026-09-23
> **Linked from:** `D03_FINAL_REPORT.md`
> **Method:** Item-by-item audit of the rubric checklist (`/tmp/prompt_full.txt` lines 202–309). Every path below was verified to exist with a file check (`test -f` / directory listing) at audit time. No path is invented.

**Label legend (§6, 7 labels only):**
`EXISTS` · `PARTIAL` · `BROKEN` · `MISSING` · `DUPLICATED` · `INCONSISTENT` · `NEEDS IMPROVEMENT`

**Owner legend (per rubric §87):**
D01 = Dashboard/Auth/Users · D02 = Courses/Progress · D03 = Events/Live/Recordings/Media/Replay · D04 = Resources/Search/Certificates

---

## §5. FULL REPOSITORY AUDIT — BACKEND (rubric lines 204–250)

| # | COMPONENT | CLASSIFICATION | PATH EVIDENCE (verified) | OWNER |
|---|-----------|----------------|--------------------------|-------|
| B1 | LiveClass | EXISTS | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/course/domain/LiveClass.java`; `liveclass/domain/LiveClassParticipant.java`, `LiveClassPoll.java`, `LiveClassQuiz.java`, `LiveClassBreakoutRoom.java` | D03 |
| B2 | LiveSession | EXISTS | `liveclass/controller/LiveSessionController.java`; `liveclass/controller/LiveSessionHealthController.java`; `liveclass/dto/LiveSessionJoinResponse.java` | D03 |
| B3 | Event | EXISTS | `event/domain/Event.java`; `event/controller/EventController.java`; `event/service/impl/EventServiceImpl.java`; `event/repository/EventRepository.java` | D03 |
| B4 | EventRegistration | EXISTS | `event/domain/EventRegistration.java`; `event/repository/EventRegistrationRepository.java` | D03 |
| B5 | Attendance | INCONSISTENT | 3 representations: `attendance/controller/AttendanceController.java` + `attendance/domain/*`; `EventRegistration.attended` (D03 webhook path); `liveclass/domain/LiveClassAttendanceDetail.java` — see §6 row A1 | D03 |
| B6 | Recording | PARTIAL | No `Recording.java` entity; lifecycle carried by `Event.recordingStatus` fields + `event/domain/Replay.java` + egress start/stop in `liveclass/controller/LiveSessionController.java` — see §6 row P1 | D03 |
| B7 | Media | EXISTS | `liveclass/controller/MediaLibraryController.java`; `liveclass/service/MediaProxyService.java` | D03 |
| B8 | MediaAsset | EXISTS | `liveclass/domain/MediaAsset.java`; `liveclass/repository/MediaAssetRepository.java`; table `media_assets` | D03 |
| B9 | Storage | PARTIAL | No dedicated Storage/S3/MinIO service class; file access via `liveclass/service/MediaProxyService.java` + URL columns on entities — see §6 row P2 | D04 |
| B10 | LiveKit | EXISTS | `liveclass/service/LiveKitService.java`; `liveclass/config/LiveKitConfig.java`; `liveclass/config/LiveKitHealthIndicator.java`; `liveclass/controller/LiveKitWebhookController.java` (runtime server undeployed — §K gap, not a code gap) | D03 |
| B11 | WebRTC | PARTIAL | No direct `RTCPeerConnection`/WebRTC API usage in backend or frontend source; media transport delegated to LiveKit (`livekit-client` in `frontend/package.json`) — see §6 row P3 | D03 |
| B12 | LiveKit token generation | EXISTS | `LiveKitService.generateToken`; `POST /v1/live-session/join/{classId}` in `LiveSessionController.java`; TTL 15 min (`PARTICIPANT_TOKEN_TTL_MS`) | D03 |
| B13 | token validation | PARTIAL | API JWT: `config/security/JwtAuthenticationFilter.java` + `JwtTokenProvider.java` (EXISTS); LiveKit webhook HS256 signature check in `LiveKitWebhookController.java` (EXISTS); participant-token validation is delegated to the LiveKit server which is **not deployed** — see §6 row P4 | D03 |
| B14 | webhook handlers | EXISTS | `liveclass/controller/LiveKitWebhookController.java` (`POST /v1/webhooks/livekit`, public + signed); `parent/controller/PaymentWebhookController.java` (D01-scope) | D03 |
| B15 | WebSocket | EXISTS | `liveclass/config/WebSocketConfig.java`; `liveclass/handler/LiveClassWebSocketHandler.java`; `liveclass/config/JwtHandshakeInterceptor.java` | D03 |
| B16 | STOMP | MISSING | No `SimpMessagingTemplate`, `@MessageMapping`, or STOMP dependency anywhere under `src/main/java` (raw WebSocket handler used instead) — see §6 row M1 | D03 |
| B17 | realtime events | EXISTS | `liveclass/domain/LiveClassSessionEvent.java`; `config/EventPublisherService.java` (RabbitMQ); frontend consumer `frontend/lib/use-realtime.ts` | D03 |
| B18 | notifications | PARTIAL | In-app EXISTS: `learner/controller/NotificationController.java`, `learner/domain/LearnerNotification.java`, `administration/domain/PlatformNotification.java`; **no email/SMTP send path** — see §6 row P5 | D01 |
| B19 | calendar | PARTIAL | Event ICS EXISTS: `GET /v1/learner/events/{id}/calendar` in `event/controller/LearnerEventController.java` (`text/calendar`); `parent/service/ParentCalendarService.java`; no unified calendar entity/API — see §6 row P6 | D03 |
| B20 | courses | EXISTS | `course/controller/CourseController.java`; `course/domain/` (Course entities) | D02 |
| B21 | modules | EXISTS | `course/domain/CourseModule.java`; `course/repository/CourseModuleRepository.java` | D02 |
| B22 | lessons | EXISTS | `course/domain/CourseLesson.java`; `learning/repository/LessonRepository.java` | D02 |
| B23 | resources | EXISTS | `learning/domain/Resource.java`; tables `resources`, `learning_resources`, `research_resources` | D04 |
| B24 | certificates | EXISTS | `certificate/domain/Certificate.java`; `certificate/controller/CertificateController.java` | D04 |
| B25 | providers | EXISTS | `nfe/provider/domain/EducationProvider.java`; `nfe/provider/controller/ProviderController.java`; `provider_memberships` tables | D01 |
| B26 | institutions | EXISTS | `shared/domain/Institution.java`; `institution/controller/InstitutionController.java` | D01 |
| B27 | users | EXISTS | `shared/domain/User.java`; `identity/controller/AuthController.java` | D01 |
| B28 | roles | EXISTS | `administration/domain/CustomRole.java`; `administration/domain/UserRoleAssignment.java`; JWT role claims | D01 |
| B29 | permissions | EXISTS | `administration/domain/RolePermission.java`; `administration/repository/RolePermissionRepository.java`; `@PreAuthorize` on controllers | D01 |
| B30 | scopes | PARTIAL | Institution/user scoping EXISTS via `administration/service/InstitutionScopeService.java` + JWT request attributes + `common/OwnershipGuard.java`; no formal OAuth2 scope model (no `@Scope` annotations found) — see §6 row P7 | D01 |
| B31 | controllers | EXISTS | 69 `*Controller.java` files under `tz.elmkusoma.**` (find-verified) | D01 |
| B32 | services | EXISTS | Per-module `service/` packages (e.g. `event/service/EventService.java`, `liveclass/service/LiveKitService.java`) | D01 |
| B33 | repositories | EXISTS | 30 `repository/` packages (Spring Data JPA), e.g. `event/repository/EventRepository.java` | D01 |
| B34 | DTOs | EXISTS | 30 `dto/` packages, e.g. `event/dto/EventRequest.java`, `event/dto/EventResponse.java` | D01 |
| B35 | mappers | PARTIAL | Dedicated mapper packages only in `audit/mapper`, `administration/mapper`, `certificate/mapper`, `course/mapper`; all other modules map inline inside services — see §6 row P8 | D01 |
| B36 | validators | PARTIAL | No dedicated `*Validator*` classes; validation is Jakarta `@Valid`/`@NotBlank` on DTOs (e.g. `event/dto/EventRequest.java`) + `EventStatus.canTransitionTo` state machine — see §6 row P9 | D01 |
| B37 | exception handling | EXISTS | `exception/GlobalExceptionHandler.java`; `exception/ResourceNotFoundException.java`; `exception/ForbiddenException.java`; `common/exception/ResourceNotFoundException.java` | D01 |
| B38 | transactions | EXISTS | `@Transactional` present in 67 main-source files (grep count) | D01 |
| B39 | caching | PARTIAL | `config/RedisConfig.java` EXISTS; no `@Cacheable` usage found; Hibernate 2nd-level cache disabled (surefire log `HHH000026`) — see §6 row P10 | D01 |
| B40 | logging | EXISTS | Lombok `@Slf4j` in 37 main-source files; operational log statements in `EventServiceImpl`; `audit/` module for persistent audit trail | D01 |
| B41 | configuration | EXISTS | `config/FlywayConfig.java`, `config/RedisConfig.java`, `config/RabbitMQConfig.java`, `config/OpenApiConfig.java`, `config/WebMvcConfig.java`, `config/JpaAuditingConfig.java`, `src/main/resources/application.yml` | D01 |
| B42 | scheduled jobs | EXISTS | `config/CoreScheduler.java` (`@Scheduled`) | D01 |
| B43 | security filters | EXISTS | `config/security/JwtAuthenticationFilter.java`, `JwtRequestAttributeFilter.java`, `JwtTokenProvider.java`, `MaintenanceInterceptor.java`, `SecurityConfig.java` | D01 |
| B44 | authorization rules | EXISTS | Class/method `@PreAuthorize` (e.g. `EventController` class-level roles), `common/OwnershipGuard.java`, institution scoping in services | D01 |
| B45 | audit logging | EXISTS | `audit/domain/AuditLog.java`, `audit/service/AuditService.java`, `common/AuditListener.java`, `config/JpaAuditingConfig.java`; event/replay operations log via `EventServiceImpl` | D01 |

**Backend tally:** EXISTS 33 · PARTIAL 10 · INCONSISTENT 1 · MISSING 1 · (BROKEN 0, DUPLICATED 0, NEEDS IMPROVEMENT 0) = 45 items.

---

## §5. FULL REPOSITORY AUDIT — FRONTEND (rubric lines 252–283)

> Frontend is **Next.js App Router** — there is **no `frontend/src/` directory**. All paths below are App Router / `components/` / `lib/` paths, verified to exist.

| # | COMPONENT | CLASSIFICATION | PATH EVIDENCE (verified) | OWNER |
|---|-----------|----------------|--------------------------|-------|
| F1 | sidebar | EXISTS | `frontend/components/dashboard/dashboard-sidebar.tsx` (+ `authority-sidebar.tsx`, `platform-admin-sidebar.tsx`) | D01 |
| F2 | topbar | EXISTS | `frontend/components/dashboard/dashboard-topbar.tsx` | D01 |
| F3 | layouts | EXISTS | `frontend/app/dashboard/layout.tsx`; `frontend/app/dashboard/learner/layout.tsx` (service-worker registration + `#dashboard-announcer`) | D01 |
| F4 | routes | EXISTS | App Router tree `frontend/app/dashboard/learner/**` (60+ routes incl. `events/`, `replays/`); `frontend/app/dashboard/admin/**` | D01 |
| F5 | navigation | EXISTS | Sidebar nav arrays with `href` + `next/link` in `dashboard-sidebar.tsx` (Events, My Registrations, Replays entries verified) | D01 |
| F6 | event pages | EXISTS | `frontend/app/dashboard/learner/events/page.tsx`; `frontend/app/dashboard/admin/events/page.tsx` | D03 |
| F7 | event details | EXISTS | `frontend/app/dashboard/learner/events/[id]/page.tsx` | D03 |
| F8 | registration UI | EXISTS | Register/cancel in `learner/events/[id]/page.tsx`; `learner/events/registered/page.tsx` (upcoming + past) | D03 |
| F9 | live UI | EXISTS | `frontend/components/live/live-classroom.tsx`; `learner/events/[id]/waiting/page.tsx`; `learner/events/[id]/preflight/page.tsx` (camera/mic check) | D03 |
| F10 | LiveKit integration | PARTIAL | Client EXISTS: `livekit-client@^2.22.3` + `@livekit/components-react@^2.9.24` in `frontend/package.json`; `components/live/live-classroom.tsx` uses `Room`; join via `learner-api.ts` → `/v1/live-session/join/{classId}`. End-to-end **blocked by undeployed LiveKit server** — see §6 row P11 | D03 |
| F11 | media pages | EXISTS | `frontend/app/dashboard/learner/media-library/page.tsx`; `frontend/app/dashboard/learner/video-library/page.tsx` | D03 |
| F12 | recording pages | PARTIAL | Recording status/info on `frontend/app/dashboard/admin/events/[id]/summary/page.tsx`; learner access via replays only — no dedicated admin recordings-management page — see §6 row P12 | D03 |
| F13 | replay pages | EXISTS | `frontend/app/dashboard/learner/replays/page.tsx`; `frontend/app/dashboard/learner/replays/[id]/page.tsx` (progress resume) | D03 |
| F14 | course pages | EXISTS | `frontend/app/dashboard/learner/courses/page.tsx` + `[id]/`; `frontend/app/dashboard/admin/courses/` | D02 |
| F15 | calendar | EXISTS | `frontend/app/dashboard/learner/calendar/page.tsx`; `frontend/app/dashboard/learner/calendar-integration/` | D01 |
| F16 | notifications | EXISTS | `frontend/app/dashboard/learner/notifications-center/page.tsx`; `frontend/app/dashboard/notifications/page.tsx` | D01 |
| F17 | search | EXISTS | `frontend/app/dashboard/learner/search/page.tsx` | D04 |
| F18 | resources | EXISTS | `frontend/app/dashboard/learner/resources/page.tsx` + `[id]/` | D04 |
| F19 | player components | EXISTS | `frontend/components/events/video-player.tsx`; `frontend/components/live/live-classroom.tsx` | D03 |
| F20 | hooks | EXISTS | `frontend/hooks/use-locale.ts`; `frontend/lib/use-realtime.ts`; `frontend/lib/announce.ts` (thin but present) | D01 |
| F21 | API clients | EXISTS | `frontend/lib/api.ts`; `frontend/lib/learner-api.ts`; `frontend/lib/api-client.ts` (+ per-module clients) | D01 |
| F22 | state management | PARTIAL | React context only (`frontend/lib/auth.tsx`, `frontend/components/locale-provider.tsx`); no Redux/Zustand/React-Query in `package.json` — see §6 row P13 | D01 |
| F23 | loading states | EXISTS | `LoadingState` in `frontend/components/learner/shared.tsx`, used by `learner/events/page.tsx`, `learner/replays/page.tsx` | D01 |
| F24 | empty states | EXISTS | `EmptyState` in `frontend/components/learner/shared.tsx`, used by event list (`learner/events/page.tsx`) | D01 |
| F25 | error states | EXISTS | Error banners in `learner/events/page.tsx` etc.; i18n keys `common.error.*`, `events.error.loadFailed`, `notifications.error.load` | D01 |
| F26 | responsive behavior | EXISTS | Tailwind responsive utilities (`sm:`/`md:`/`lg:`) across `dashboard-sidebar.tsx` and dashboard pages (grep-verified) | D01 |
| F27 | mobile layouts | EXISTS | Responsive sidebar/topbar breakpoints in `dashboard-sidebar.tsx` / `dashboard-topbar.tsx` | D01 |
| F28 | reusable components | EXISTS | `frontend/components/ui/` (badge, button, card, carousel); `frontend/components/learner/shared.tsx` (LearnerHeader, cards, Loading/Empty) | D01 |
| F29 | design tokens | EXISTS | `frontend/app/globals.css` — `@theme inline` CSS variables (`--color-*`, `--radius-*`, sidebar/chart tokens) | D01 |

**Frontend tally:** EXISTS 26 · PARTIAL 3 · (others 0) = 29 items.

---

## §5. FULL REPOSITORY AUDIT — DATABASE (rubric lines 285–309)

> Migration directory: `backend/elmkusoma-core/src/main/resources/db/migration/` — **76 files, `V01` … `V76`** (verified by listing). D03 migrations: `V71`, `V74`, `V75`, `V76`.

| # | COMPONENT | CLASSIFICATION | PATH EVIDENCE (verified) | OWNER |
|---|-----------|----------------|--------------------------|-------|
| D1 | event tables | EXISTS | `V71__event_d03_extended_columns.sql` (`events`, `event_registrations`, `event_materials`); `V74__events_access_level.sql`; `V75__events_entity_columns.sql` | D03 |
| D2 | live tables | EXISTS | `live_classes` + `V48__add_class_group_id_to_live_classes.sql`, `V50__add_recording_enabled_to_live_classes.sql`, `V61__live_class_enhancements.sql`, `V73__live_class_replay_and_integrity.sql` (`live_class_participants`, `live_class_attendance_detail`, …) | D03 |
| D3 | registration tables | EXISTS | `event_registrations` in `V71__event_d03_extended_columns.sql`; course enrollment separate (`student_course_enrollments`) owned by D02 | D03 |
| D4 | attendance tables | EXISTS | `attendance_records`, `attendance_summaries`, `bulk_attendance_sessions`; `live_class_attendance_detail` (V73); D03 also marks `event_registrations.attended` — multi-representation noted §6 A1 | D03 |
| D5 | recording tables | PARTIAL | **No dedicated `recordings` table**; recording state on `events` columns (V71) + `replays` table (`V76__replay_tables.sql`) — see §6 row P14 | D03 |
| D6 | media tables | EXISTS | `media_assets`; `live_class_shared_media` | D03 |
| D7 | storage metadata | PARTIAL | File URL/size columns on `media_assets`, `replays` (`recording_url`, `file_size_bytes`); no dedicated storage/bucket metadata table — see §6 row P15 | D04 |
| D8 | provider relationships | EXISTS | `provider_memberships`, `provider_service_entitlements` | D01 |
| D9 | institution relationships | EXISTS | `institutions`, `institution_memberships`; `institution_id` FK columns across domain tables | D01 |
| D10 | course relationships | EXISTS | `courses`, `course_modules`, `course_lessons`, `student_course_enrollments` | D02 |
| D11 | resource relationships | EXISTS | `resources`, `learning_resources`, `research_resources` | D04 |
| D12 | certificate relationships | EXISTS | `certificates`, `certificate_templates` (+ FK links to users/issuers) | D04 |
| D13 | foreign keys | EXISTS | 109 `FOREIGN KEY` clauses across migrations (grep count), incl. `fk_replay_progress_replay` (V76) | D01 |
| D14 | indexes | EXISTS | 295 `CREATE INDEX` statements (grep count), incl. partial indexes on `replays` (V76) | D01 |
| D15 | unique constraints | EXISTS | 39 `UNIQUE` constraints (grep count), incl. `uq_replay_progress_user` (V76) | D01 |
| D16 | status fields | EXISTS | `events.event_status` + `events.status` (V71); `replays.status` (V76); `certificates.status`; entity `EventStatus` sync via `applyStatus` | D03 |
| D17 | enum values | EXISTS | Java enums `EventStatus` (17 values), `EventType` (12 values); stored as VARCHAR, validated application-side — no DB CHECK constraints (improvement noted §6 N1) | D03 |
| D18 | audit fields | EXISTS | `created_at`/`updated_at`/`created_by`/`updated_by` in 58 migrations (grep count); `common/BaseEntity.java` + `config/JpaAuditingConfig.java` | D01 |
| D19 | soft deletion | EXISTS | `is_deleted` column in 59 migrations (grep count); queries filter `.isDeletedFalse` (e.g. `EventRepository`); soft-deleted events → 404 | D01 |
| D20 | Flyway migrations | EXISTS | 76 migration files `V01`–`V76`; `spring.flyway.enabled: true`; `FlywayConfig` no longer skips migrations; `ddl-auto: none`/`validate` | D01 |

**Database tally:** EXISTS 18 · PARTIAL 2 · (others 0) = 20 items.

---

### §5 Grand total

| Label | Count |
|-------|------:|
| EXISTS | 77 |
| PARTIAL | 15 |
| INCONSISTENT | 1 |
| MISSING | 1 |
| BROKEN | 0 |
| DUPLICATED | 0 |
| NEEDS IMPROVEMENT | 0 |
| **Total items audited** | **94** |

---

## §6. AUDIT CLASSIFICATION — PROBLEM ITEMS (full 8-field record)

Every non-EXISTS row from §5 plus the still-open runtime gaps. Fields per rubric §6: COMPONENT · CURRENT IMPLEMENTATION · SOURCE OF TRUTH · OWNER · DEPENDENCIES · PROBLEM · RISK · REQUIRED CHANGE.

| COMPONENT | CLASS | CURRENT IMPLEMENTATION | SOURCE OF TRUTH | OWNER | DEPENDENCIES | PROBLEM | RISK | REQUIRED CHANGE |
|-----------|-------|------------------------|-----------------|-------|--------------|---------|------|-----------------|
| A1 Attendance | INCONSISTENT | 3 parallel representations: `attendance/*` module, `EventRegistration.attended`, `LiveClassAttendanceDetail` | `attendance/domain/` entities + `EventRegistration.java` | D03 (event/live) / D01 (general) | webhook path `LiveKitWebhookController` → `markEventAttendance` | Same word, three schemas; no single join point for "was this learner present" | Wrong attendance/certificate eligibility if sources diverge | Document canonical source per context (event → `EventRegistration.attended`; live class → `live_class_attendance_detail`; course → `attendance_records`); add cross-read contract, do not merge tables this sprint |
| B6/P1 Recording | PARTIAL | No `Recording` entity; state on `Event.recordingStatus` + `Replay.java`; egress start/stop via `LiveSessionController` | `event/domain/Event.java` (recording fields), `event/domain/Replay.java`, `V71`/`V76` | D03 | LiveKit egress (undeployed) | Recording lifecycle spread across Event/Replay with no single aggregate | Ambiguous state reporting; egress failure hard to surface | Keep Event↔Replay as SoT this sprint; record gap for a future `Recording` read-model if needed |
| B9/P2 Storage | PARTIAL | `MediaProxyService` + URL columns; no Storage abstraction | `liveclass/service/MediaProxyService.java` | D04 | file storage backend | No uniform storage interface; providers not pluggable | Vendor lock-in; broken links if storage moves | Define storage contract (URL + metadata) owned by D04; D03 keeps URL fields only |
| B11/P3 WebRTC | PARTIAL | Delegated to LiveKit client (`livekit-client`); no first-party WebRTC code | LiveKit docs + `frontend/components/live/live-classroom.tsx` | D03 | LiveKit server (undeployed) | WebRTC exists only as LiveKit dependency | If LiveKit is dropped, no fallback transport | Accept delegation as architecture (no duplicate WebRTC); blocker is server deployment (§K) |
| B13/P4 Token validation | PARTIAL | API JWT + webhook HS256 verified in code; participant tokens validated only by LiveKit server | `config/security/JwtTokenProvider.java`; `LiveKitWebhookController.java`; `LiveKitService.java` | D03 | LiveKit server | Participant-token enforcement untestable end-to-end without server | Invalid tokens could join rooms if server runs with weak config | Deploy LiveKit with real keys; verify `jwt` validation server-side; keep TTL 15 min |
| B16/M1 STOMP | MISSING | Raw WebSocket handler only (`LiveClassWebSocketHandler`); no STOMP/SimpMessaging | `liveclass/config/WebSocketConfig.java` | D03 | Spring WebSocket | Rubric lists STOMP; codebase uses raw WS frames | Consumers assuming STOMP will fail to integrate | Either document raw-WebSocket protocol as the contract, or add STOMP layer later — do NOT claim STOMP exists |
| B18/P5 Notifications (email) | PARTIAL | In-app `LearnerNotification`/`PlatformNotification` rows only | `learner/domain/LearnerNotification.java`; `learner/controller/NotificationController.java` | D01 | SMTP/email pipeline (absent) | No email send; registration/certificate notices stay in-app | Users expecting email miss live/replay notices | Wire registration/certificate events into platform email pipeline (D01 scope) |
| B19/P6 Calendar | PARTIAL | Per-event ICS export + parent calendar service; no unified calendar API | `LearnerEventController` `/calendar` (ICS); `parent/service/ParentCalendarService.java` | D03 (event ICS) | frontend `learner/calendar/page.tsx` | No shared calendar feed across event types | Duplicate calendar features per module | Keep event ICS as SoT for D03; D01 may aggregate later |
| B30/P7 Scopes | PARTIAL | Institution scope service + JWT attributes + OwnershipGuard; no OAuth2 scope model | `administration/service/InstitutionScopeService.java`; `common/OwnershipGuard.java` | D01 | JWT design | "Scopes" exist as behavior, not as first-party scope tokens | Teams may add inconsistent ad-hoc scope checks | Document current scope model (role + institution + ownership) as the contract; formal scopes only if OAuth2 is adopted |
| B35/P8 Mappers | PARTIAL | 4 mapper packages; other modules map DTOs inline in services | `course/mapper/`, `certificate/mapper/`, `audit/mapper/`, `administration/mapper/` | D01 | — | Inconsistent mapping style across modules | Field drift between entity and DTO on refactor | Adopt mapper pattern incrementally; no mass refactor this sprint |
| B36/P9 Validators | PARTIAL | Jakarta `@Valid` on DTOs + `EventStatus.canTransitionTo`; no dedicated validator layer | `event/dto/EventRequest.java`; `event/domain/EventStatus.java` | D01 | — | Business-rule validation scattered | Rules duplicated/missed when new endpoints added | Keep state machine in `EventStatus` as SoT; add validators only for new rule clusters |
| B39/P10 Caching | PARTIAL | `RedisConfig` present; no `@Cacheable`; 2nd-level cache disabled | `config/RedisConfig.java` | D01 | Redis availability | Caching infrastructure unused | Repeated identical queries under load | Enable targeted `@Cacheable` only on measured hot paths later |
| F10/P11 LiveKit integration (FE) | PARTIAL | Client + join API implemented; server not running | `frontend/components/live/live-classroom.tsx`; `learner-api.ts` | D03 | LiveKit server deploy, env keys | UI degrades gracefully (`liveKitAvailable=false`) but live rooms unusable in prod | "Live" feature unusable in production | Deploy LiveKit (docker-compose.livekit.yml / livekit.yaml); set `LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` |
| F12/P12 Recording pages | PARTIAL | Recording info on event summary + replay pages only | `frontend/app/dashboard/admin/events/[id]/summary/page.tsx` | D03 | Replay availability | No dedicated admin recording library page | Operators manage recordings via DB/egress console | Add admin recordings view backed by `GET /v1/replays` (future) |
| F22/P13 State management | PARTIAL | Context (`lib/auth.tsx`, `locale-provider.tsx`) + component state; no global store | `frontend/lib/auth.tsx` | D01 | — | No shared cache layer; pages refetch independently | Inconsistent loading/error UX as app grows | Acceptable for current scale; adopt query cache only if refetch cost is measured |
| D5/P14 Recording tables | PARTIAL | No `recordings` table; `events` columns + `replays` | `V71__event_d03_extended_columns.sql`; `V76__replay_tables.sql` | D03 | — | Same as B6 (schema view) | Schema cannot express multi-rendition recordings yet | Keep current schema; revisit with HLS/transcoding work (§K) |
| D7/P15 Storage metadata | PARTIAL | URL/size columns only | `media_assets`, `replays` columns | D04 | Storage contract (B9) | No bucket/key/retention metadata | Cannot migrate storage safely | Add storage metadata when Storage abstraction is built (D04) |
| N1 Enum values (DB) | NEEDS IMPROVEMENT | VARCHAR columns; enums enforced in Java only | `event/domain/EventStatus.java`, `EventType.java` | D03 | Flyway | No DB CHECK → raw SQL can write invalid status | Silent data corruption if migrations/scripts bypass app | Optional: add CHECK constraints in a future migration after backfill |
| LiveKit server (runtime) | MISSING | Dev defaults (`devkey`/`devsecret`, `ws://localhost:7880`); **no container, port 7880 not listening** (verified `docker ps` empty, `ss` no listener) | `application.yml` livekit.*; `docker-compose.livekit.yml`; `livekit.yaml` | D03 + Ops | Docker/host, secrets | Live/recording/egress end-to-end unverifiable | Blocks §108 "LiveKit works" completion claim | Deploy LiveKit; run E2E against it; rotate dev secrets |
| Email SMTP (runtime) | MISSING | `application.yml` has no spring.mail/SMTP config; no `MailSender` usage | `backend/.../resources/application.yml` | D01 | SMTP provider | No outbound email | Registration/cert notices rely on in-app list only | Add SMTP config + senders in D01 notification pipeline |
| Payment for paid events | MISSING | `isFree=false` accepted on Event; no checkout on event path (platform/parent commerce exists separately: `PlatformCommerceService`, `ParentPaymentService`) | `event/domain/Event.java` (`isFree`); `administration/service/PlatformCommerceService.java` (D01) | D01 | Payment provider | Paid events cannot be purchased inline | Revenue events stall; manual verification needed | Manual/offline verification until commerce integration for events |
| HLS transcoding | MISSING | No HLS/DASH/transcode code (grep `HLS|transcod` = 0 hits) | LiveKit egress output files | D03 | Transcoder/CDN | Recordings served in original egress format | Poor adaptive playback on weak networks | Add transcoding pipeline post-recording (future) |
| Learner dashboard "view record" link | BROKEN | Link targets `/dashboard/learner/academic` (no such route) | Actual routes: `app/dashboard/learner/academic-progress/`, `academic-record/` | D01 | sidebar/dashboard markup | 404 on click | Dead link violates UX bar | Repoint to `/dashboard/learner/academic-record` (D01 file, documented not edited here) |

*(DUPLICATED label: no true duplicates found — LearningGoal pair (learner vs parent) and Certificate/NFE pairs are documented in root `README.md` as distinct-ownership entities, not D03 duplicates.)*

---

## §7. EVIDENCE MODEL

Findings are separated by evidence class. **Assumptions are never promoted to facts.**

### REQUIREMENT (from rubric — not yet a fact about the code)

| ID | REQUIREMENT |
|----|-------------|
| R-01 | Events/Live/Recordings/Media/Replay owned by D03 (§87) |
| R-02 | Every contract documents identifier, endpoint, input, output, ownership, authorization, source of truth, error behavior, compatibility (§88) |
| R-03 | Completion claimed only with code/tests/API/database/frontend/LiveKit/security/integration/build/E2E evidence (§107) |
| R-04 | STOMP listed in backend audit checklist (§5) |
| R-05 | LiveKit works end-to-end before DONE (§108) |

### FACT (observable in the repository right now)

| ID | FACT | Evidence |
|----|------|----------|
| F-01 | No `LiveKitController.java` exists | `find … -name 'LiveKitController.java'` → empty |
| F-02 | Token path is `POST /v1/live-session/join/{classId}` | `liveclass/controller/LiveSessionController.java` |
| F-03 | Webhook path is `POST /v1/webhooks/livekit` | `liveclass/controller/LiveKitWebhookController.java` |
| F-04 | `EventType` enum has **no** `LIVE` value (12 values: LECTURE…OTHER) | `event/domain/EventType.java` |
| F-05 | `EventStatus` **has** `LIVE` (17 states) | `event/domain/EventStatus.java` |
| F-06 | `GET /v1/events/institution/{id}` accepts `?status=` and filters by stored status string | `EventController.java` L48–65 → `EventServiceImpl.getEvents` → `findByInstitutionIdAndStatusAndIsDeletedFalseOrderByStartsAtAsc` |
| F-07 | Replay migration file is `V76__replay_tables.sql` (V72 is `live_streaming_schema_repair`) | `db/migration/` listing |
| F-08 | Frontend is App Router; `frontend/src/` does not exist | `ls frontend/` → `app/`, no `src/` |
| F-09 | `notifications.error.load` exists in en + sw message files | `frontend/messages/en.json` L2228–2230; `sw.json` L2301–2303 |
| F-10 | `common.error.load` exists in en + sw | `frontend/messages/en.json` / `sw.json` `common.error` objects |
| F-11 | No STOMP/SimpMessaging usage under backend main sources | grep `SimpMessaging\|@MessageMapping\|STOMP` → no matches |
| F-12 | 76 Flyway migrations, V01–V76 | `db/migration/` listing |
| F-13 | No LiveKit server running | `docker ps` empty; no listener on 7880/7881 |
| F-14 | No `spring.mail`/SMTP configuration | grep of `application.yml` |
| F-15 | No HLS/transcode code | grep `HLS\|transcod` in backend → 0 hits |
| F-16 | `frontend/src/pages/*` paths cited in older docs do not exist | `ls frontend` |

### VERIFIED (checked in this audit pass with a command/result)

| ID | VERIFICATION | RESULT |
|----|--------------|--------|
| V-01 | Full surefire suite run 2026-09-23 21:02–21:05 | **18 test classes, 228 tests, 0 failures, 0 errors, 0 skipped** (`target/surefire-reports/*.txt`) |
| V-02 | D03 test method counts (`^\s*@Test` regex, per class) | `EventSecurityTest` = **30**, `EventLifecycleE2ETest` = **22**, `LiveKitIntegrationTest` = **30** (total 82) |
| V-03 | JSON validity of `frontend/messages/en.json` + `sw.json` | parse OK (python `json.load`) — re-checked after i18n edits |
| V-04 | Path spot-checks for every §5 row | exist (`test -f` / `ls`) |
| V-05 | Surefire D03 classes all green | EventSecurityTest 30/30, EventLifecycleE2ETest 22/22, LiveKitIntegrationTest 30/30 pass |
| V-06 | `mvn test` re-run at 21:17 hit **compile error in `StudentEventController.java`** caused by a concurrent working-tree edit (file timestamp 21:20:41, diff +55 lines, **not made by this docs-only task**) | recorded honestly; file present and syntactically complete after edit — suite must be re-run when tree is stable |

### ASSUMPTION (held, NOT a fact — see assumption log)

| ID | ASSUMPTION | Why it is still only an assumption |
|----|------------|-----------------------------------|
| AS-01 | The 21:02–21:05 green run reflects the committed D03 test code | Concurrent edits to backend files began during this audit; a clean re-run on a stable tree is required for a signed-off build claim |
| AS-02 | Production will set real `LIVEKIT_*` env values | Only dev defaults are committed; no prod secret material is in the repo (correctly) |
| AS-03 | `?status=LIVE` on the institution endpoint returns exactly events whose stored status is `LIVE` | Verified code path (F-06), but **not** exercised against a running server in this pass |
| AS-04 | Learners see "currently live" events via `eventStatus == "LIVE"` client filter when server-side filter is too narrow (e.g., REGISTRATION_OPEN not yet LIVE) | Contract 2 documents both options; actual UX choice belongs to D01 |
| AS-05 | Root `README.md` "PARTIAL" rows (Security, Testing) still describe current state | README not re-audited line-by-line in this pass |

### INFERENCE (logical conclusion drawn from facts — not directly observed)

| ID | INFERENCE | Basis |
|----|-----------|-------|
| I-01 | Invalid status strings can reach the DB if code bypasses the app layer | F-05 + VARCHAR columns + no CHECK constraints observed |
| I-02 | End-to-end live join will fail in any environment until LiveKit server is deployed | F-13 + client code paths (F-10 analogue) |
| I-03 | Email-dependent rubric expectations cannot be met | F-14 + no MailSender usage |
| I-04 | The `frontend/src/pages` references in older docs were structural mistakes | F-08 + F-16 |

### ASSUMPTION LOG (how to close each assumption)

| ID | How to close | Command / action |
|----|--------------|------------------|
| AS-01 | Re-run full suite on stable tree, record BUILD line | `cd backend/elmkusoma-core && mvn test` after concurrent edits merge |
| AS-02 | Read prod deployment env (out of repo) | Check ops secret store for `LIVEKIT_API_KEY/SECRET/URL` |
| AS-03 | Call API on a running server | `GET /v1/events/institution/{id}?status=LIVE` with JWT, inspect payload |
| AS-04 | Product decision by D01 | Compare dashboard widgets against live event states |
| AS-05 | Re-audit root README status table | Manual review |

---

*End of `D03_REPO_AUDIT.md`. Final-report sections J/K/L updated in `D03_FINAL_REPORT.md`; ownership paths in `D03_DOMAIN_OWNERSHIP.md`; contracts in `D03_CROSS_DEVELOPER_CONTRACTS.md`; scoring in `D01_D03_SCORECARD.md`.*
