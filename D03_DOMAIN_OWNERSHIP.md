# D03 — Domain Ownership Matrix

> **Sprint D03: Live Events, Recordings & Replay**
> **Author:** opencode
> **Date:** 2026-09-22

---

## Domain Ownership Table

| Domain | Owner | Description |
|--------|-------|-------------|
| Dashboard | D01 | Home page, notifications, analytics widgets |
| **Events** | **D03** | Event CRUD, registration, materials |
| **Live** | **D03** | LiveKit integration, token generation, webhooks |
| **Attendance** | **D03** | Event registration, check-in, attendance tracking |
| **Recordings** | **D03** | LiveKit recording lifecycle, storage |
| **Media** | **D03** | Media library (event recordings, uploads) |
| **Replay** | **D03** | On-demand replay playback, progress tracking |
| Courses | D02 | Course CRUD, modules, enrollment |
| Progress | D02 | Learning progress, completion tracking |
| Resources | D04 | Document library, file uploads, categorization |
| Search | D04 | Full-text search, indexing |
| Certificates | D04 | Certificate generation, verification |

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
| Replay repository | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/repository/ReplayRepository.java` |
| Replay controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/ReplayController.java` |
| LiveKit controller | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/LiveKitController.java` |
| LiveKit webhook | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/LiveKitWebhookController.java` |
| Frontend event pages | `frontend/src/pages/events/` |
| Frontend replay pages | `frontend/src/pages/replays/` |
| Frontend provider pages | `frontend/src/pages/provider/` |

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
