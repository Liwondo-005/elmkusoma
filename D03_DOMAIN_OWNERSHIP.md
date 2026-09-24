# D03 — Domain Ownership Matrix

> **Sprint D03: Live Events, Recordings & Replay**
> **Author:** opencode
> **Date:** 2026-09-22

---

## Domain Ownership Table

| Domain | Owner | Description |
|--------|-------|-------------|
| Dashboard | D01 | Home page, notifications, analytics widgets (rubric §87) |
| **Events** | **D03** | Event discovery, details, types, registration, status, access, preparation, participation (§87) |
| **Live** | **D03** | Live session integration, LiveKit, tokens, live room experience, realtime session state (§87) |
| **Attendance** | **D03** | Event/live attendance state, evidence, synchronization (§8 D03 owns; general attendance module shared) |
| **Recordings** | **D03** | Recording lifecycle, processing status, availability, access (§87) |
| **Media** | **D03** | Event/live recordings integration, playback, media relationships (§87) |
| **Replay** | **D03** | Replay availability, continue watching, learning return path (§87) |
| Courses | D02 | Course CRUD, modules, enrollment (§87) |
| Learning Progress | D02 | Learning progress, completion tracking (§87) |
| Resources | D04 | Document library, file uploads, categorization (§87) |
| Global Search | D04 | Full-text search, indexing (§87) |
| Certificates | D04 | Certificate generation, verification (§87) |

If repository evidence shows a different existing owner, it is documented in `D03_REPO_AUDIT.md` §5 before modification.

---

## D03 Domain Components — File Paths

| Component | Path |
|-----------|------|
| Event entity | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/Event.java` |
| Event status enum | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/EventStatus.java` |
| Event type enum | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/EventType.java` |
| Event repository | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/repository/EventRepository.java` |
| Event service | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/service/EventService.java` |
| Event service impl | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/service/impl/EventServiceImpl.java` |
| Event controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/EventController.java` |
| Event DTOs | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/dto/` |
| Event registration entity | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/EventRegistration.java` |
| Event material entity | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/EventMaterial.java` |
| Replay entity | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/Replay.java` |
| Replay progress entity | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/domain/ReplayProgress.java` |
| Replay repository | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/repository/ReplayRepository.java` |
| Replay controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/ReplayController.java` |
| Learner replay controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/LearnerReplayController.java` |
| Learner event controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/LearnerEventController.java` |
| Live session controller (token generation) | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/controller/LiveSessionController.java` |
| Live session health controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/controller/LiveSessionHealthController.java` |
| LiveKit service | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/service/LiveKitService.java` |
| LiveKit config | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/config/LiveKitConfig.java` |
| LiveKit webhook controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/controller/LiveKitWebhookController.java` |
| WebSocket config / handler | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/config/WebSocketConfig.java`, `liveclass/handler/LiveClassWebSocketHandler.java` |
| Frontend learner event pages | `frontend/app/dashboard/learner/events/` (list, `[id]`, `[id]/preflight`, `[id]/waiting`, `registered`) |
| Frontend learner replay pages | `frontend/app/dashboard/learner/replays/` (list, `[id]`) |
| Frontend admin event pages | `frontend/app/dashboard/admin/events/` (list, `new`, `[id]/edit`, `[id]/summary`) |
| Frontend live components | `frontend/components/live/` (`live-classroom.tsx` uses `livekit-client`) |
| D03 Flyway migrations | `backend/elmkusoma-core/src/main/resources/db/migration/V71__event_d03_extended_columns.sql`, `V74__events_access_level.sql`, `V75__events_entity_columns.sql`, `V76__replay_tables.sql` |

> **Corrections (2026-09-23):** There is **no** `LiveKitController.java` anywhere in the repository — token generation lives in `LiveSessionController` + `LiveKitService`, webhook lives in `LiveKitWebhookController` (both under `liveclass/`). There is **no** `frontend/src/` directory — the frontend is Next.js App Router (`frontend/app/...`). The Domain|Owner table above matches rubric §87.

---

## D03 DOES NOT Own

| Component | Owner | Reason |
|-----------|-------|--------|
| User management | D01 | User entity, auth, JWT tokens |
| Course structure | D02 | Course, Module, Lesson entities |
| Learning progress | D02 | Progress tracking, completion logic |
| Document uploads | D04 | General file storage, categorization |
| Search indexing | D04 | Full-text search engine |
| Certificate generation | D04 | Certificate templates, PDF generation |
| Dashboard widgets | D01 | Home page aggregation |
| Notifications | D01 | Push/email notification system |
| Payment processing | D01 | Billing, subscriptions |

---

## Cross-Domain Dependencies

```
D03 (Events/Live) ──→ D01 (Dashboard): event counts, upcoming events
D03 (Events/Live) ──→ D02 (Courses): course↔event relationships
D03 (Events/Live) ──→ D04 (Resources): event materials, recordings in media library
D03 (Events/Live) ──→ D04 (Search): event data for search index
D03 (Events/Live) ──→ D04 (Certificates): attendance evidence for certificate eligibility
```
