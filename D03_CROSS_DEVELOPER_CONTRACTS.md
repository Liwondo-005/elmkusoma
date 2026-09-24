# D03 — Cross-Developer Contracts

> **Sprint D03: Live Events, Recordings & Replay**
> **Author:** opencode
> **Date:** 2026-09-22

---

## Contract 1: D03 → D01 — Event Data for Dashboard Widgets

| Field | Value |
|-------|-------|
| **Identifier** | `D03-EVT-DASH-001` |
| **Consumer** | D01 (Dashboard) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/institution/{institutionId}?status=PUBLISHED` |
| **Input** | Path: `institutionId` (UUID), Query: `status=PUBLISHED`, `limit=5` |
| **Output** | `ApiResponse<List<EventResponse>>` — list of upcoming published events with title, startsAt, eventType, thumbnailUrl, registeredCount |
| **Ownership** | D03 owns the Event entity and all query logic |
| **Source of Truth** | `backend/elmkusoma-core/src/main/java/tz/elmkusoma/event/controller/EventController.java` → `GET /v1/events/institution/{institutionId}`; entity `event/domain/Event.java`; query `event/repository/EventRepository.java` (runtime OpenAPI via `config/OpenApiConfig.java`) |
| **Authorization** | JWT token with valid institutionId; `@PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER','STUDENT','OTHER_LEARNER')")` |
| **Error Behavior** | 401 if unauthenticated; 403 if cross-institution; empty list if no events |
| **Compatibility** | Backward-compatible; new fields (eventType, category, tags) are nullable and ignored by D01 |

---

## Contract 2: D03 → D01 — Live Event Data for Dashboard

| Field | Value |
|-------|-------|
| **Identifier** | `D03-LIVE-DASH-002` |
| **Consumer** | D01 (Dashboard) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/institution/{institutionId}?status=LIVE` |
| **Input** | Path: `institutionId` (UUID), Query: `status=LIVE` (matches the **`EventStatus.LIVE`** state — NOT `eventType`; the `EventType` enum has **no** `LIVE` value) |
| **Output** | `ApiResponse<List<EventResponse>>` — events whose `status`/`eventStatus == "LIVE"` with `startsAt`, `meetingUrl`, `eventStatus` |
| **Ownership** | D03 owns event status transitions (`EventStatus.canTransitionTo`; webhook `room_started` → LIVE) |
| **Source of Truth** | `event/domain/EventStatus.java` (LIVE state + transition map); `EventController.getEventsByInstitution` (`status` query param verified: `EventController.java` L48–65 → `EventServiceImpl.getEvents` → `findByInstitutionIdAndStatus…`); entity `event/domain/Event.java` |
| **Authorization** | Same as Contract 1 |
| **Error Behavior** | Same as Contract 1; unknown/unsupported status string returns empty list (no error) |
| **Compatibility** | Server-side filter `?status=LIVE` is **supported today** (VERIFIED in code). Fallback: if D01 needs a broader set (e.g. upcoming + live), call without `status` or `?status=PUBLISHED` and **client-filter** on `eventStatus == "LIVE"` / `status == "LIVE"` — both fields are present on `EventResponse`. No backend change needed either way |

---

## Contract 3: D03 → D01 — Recording Data for Dashboard

| Field | Value |
|-------|-------|
| **Identifier** | `D03-REC-DASH-003` |
| **Consumer** | D01 (Dashboard) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/replays/event/{eventId}` |
| **Input** | Path: `eventId` (UUID) |
| **Output** | `ApiResponse<List<Replay>>` — replays with status=AVAILABLE, durationSeconds, thumbnailUrl |
| **Ownership** | D03 owns Replay entity |
| **Source of Truth** | `event/controller/ReplayController.java` → `GET /v1/replays/event/{eventId}`; entity `event/domain/Replay.java`; `event/repository/ReplayRepository.java` |
| **Authorization** | JWT token with valid institutionId |
| **Error Behavior** | 200 with empty list if no replays; 404 if event not found |
| **Compatibility** | New entity; no backward-compatibility concerns |

---

## Contract 4: D03 → D02 — Course↔Event Relationship

| Field | Value |
|-------|-------|
| **Identifier** | `D03-EVT-COURSE-004` |
| **Consumer** | D02 (Courses) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/{eventId}` |
| **Input** | Path: `eventId` (UUID) |
| **Output** | `EventResponse` with `eventType`, `category`, `tags` (used by D02 to link events to courses) |
| **Ownership** | D03 owns Event entity; D02 reads `category` field to map to course |
| **Source of Truth** | `event/controller/EventController.java` → `GET /v1/events/{eventId}`; entity `event/domain/Event.java` (`eventType`, `category`, `tags` fields) |
| **Authorization** | JWT token; `@PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER','STUDENT','OTHER_LEARNER')")` |
| **Error Behavior** | 404 if event not found or deleted |
| **Compatibility** | D02 uses existing fields; no D03 changes needed for this contract |

---

## Contract 5: D03 → D02 — Event Attendance for Learning History

| Field | Value |
|-------|-------|
| **Identifier** | `D03-ATT-COURSE-005` |
| **Consumer** | D02 (Courses/Progress) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/registrations/user/{userId}` |
| **Input** | Path: `userId` (UUID), Header: `Authorization: Bearer <JWT>` |
| **Output** | `ApiResponse<List<EventRegistrationResponse>>` — registrations with `eventId`, `eventTitle`, `status`, `attended`, `registeredAt` |
| **Ownership** | D03 owns EventRegistration entity |
| **Source of Truth** | `event/controller/EventController.java` → `GET /v1/events/registrations/user/{userId}`; entity `event/domain/EventRegistration.java`; `event/repository/EventRegistrationRepository.java` |
| **Authorization** | JWT token; user can only query own registrations or ADMIN/INSTITUTION_ADMIN can query any |
| **Error Behavior** | 401 if unauthenticated; 403 if cross-user without admin role; empty list if no registrations |
| **Compatibility** | New endpoint; D02 calls it to enrich learning history with event attendance |

---

## Contract 6: D03 → D04 — Event Materials for Resources

| Field | Value |
|-------|-------|
| **Identifier** | `D03-MAT-RES-006` |
| **Consumer** | D04 (Resources) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/{eventId}/materials` |
| **Input** | Path: `eventId` (UUID) |
| **Output** | `ApiResponse<List<EventMaterialResponse>>` — materials with `title`, `materialType`, `fileUrl`, `fileSize`, `durationMinutes` |
| **Ownership** | D03 owns EventMaterial entity |
| **Source of Truth** | `event/controller/EventController.java` → `GET /v1/events/{eventId}/materials`; entity `event/domain/EventMaterial.java` |
| **Authorization** | JWT token; `@PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER','STUDENT','OTHER_LEARNER')")` |
| **Error Behavior** | 200 with empty list if no materials; 404 if event not found |
| **Compatibility** | D04 aggregates across D03 materials + own document uploads |

---

## Contract 7: D03 → D04 — Event Data for Search Indexing

| Field | Value |
|-------|-------|
| **Identifier** | `D03-EVT-SEARCH-007` |
| **Consumer** | D04 (Search) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/institution/{institutionId}?status=PUBLISHED` |
| **Input** | Path: `institutionId` (UUID), Query: `status=PUBLISHED` |
| **Output** | `ApiResponse<List<EventResponse>>` — full event data for search index |
| **Ownership** | D03 owns event data; D04 indexes it |
| **Source of Truth** | Same endpoint as Contract 1 — `EventController.getEventsByInstitution` (`GET /v1/events/institution/{id}`); entity `event/domain/Event.java` |
| **Authorization** | Internal service-to-service (JWT with SERVICE role) |
| **Error Behavior** | 200 with empty list if no events |
| **Compatibility** | D04 periodically syncs; new fields are additive |

---

## Contract 8: D03 → D04 — Recording Data for Media Library

| Field | Value |
|-------|-------|
| **Identifier** | `D03-REC-MEDIA-008` |
| **Consumer** | D04 (Media Library) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/replays` |
| **Input** | None (returns all AVAILABLE replays) |
| **Output** | `ApiResponse<List<Replay>>` — replays with `eventId`, `recordingUrl`, `durationSeconds`, `thumbnailUrl`, `status` |
| **Ownership** | D03 owns Replay entity |
| **Source of Truth** | `event/controller/ReplayController.java` → `GET /v1/replays`; entity `event/domain/Replay.java`; `event/repository/ReplayRepository.java` |
| **Authorization** | Internal service-to-service (JWT with SERVICE role) |
| **Error Behavior** | 200 with empty list if no replays |
| **Compatibility** | D04 displays replays in media gallery; new entity, no backward concerns |

---

## Contract 9: D03 → D04 — Attendance Evidence for Certificates

| Field | Value |
|-------|-------|
| **Identifier** | `D03-ATT-CERT-009` |
| **Consumer** | D04 (Certificates) |
| **Provider** | D03 (Events) |
| **Endpoint** | `GET /v1/events/registrations/event/{eventId}` |
| **Input** | Path: `eventId` (UUID), Header: `Authorization: Bearer <JWT>` |
| **Output** | `ApiResponse<List<EventRegistrationResponse>>` — registrations with `userId`, `attended`, `status=REGISTERED` |
| **Ownership** | D03 owns attendance data |
| **Source of Truth** | `event/controller/EventController.java` → `GET /v1/events/registrations/event/{eventId}`; entity `event/domain/EventRegistration.java` field `attended` (set by webhook `participant_joined` → `EventServiceImpl.markEventAttendance`) |
| **Authorization** | JWT token; `@PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")` |
| **Error Behavior** | 200 with empty list if no registrations; 403 if STUDENT |
| **Compatibility** | D04 checks `attended=true` to determine certificate eligibility |

---

## Contract Summary Matrix

| ID | From → To | Endpoint | Purpose | Source of Truth |
|----|-----------|----------|---------|-----------------|
| 001 | D03 → D01 | `GET /v1/events/institution/{id}` | Dashboard upcoming events | `EventController` + `Event.java` / `EventRepository` |
| 002 | D03 → D01 | `GET /v1/events/institution/{id}?status=LIVE` (or client-filter `eventStatus == "LIVE"`) | Dashboard live events | `EventStatus.java` (LIVE) + `EventController` `status` param |
| 003 | D03 → D01 | `GET /v1/replays/event/{id}` | Dashboard recording count | `ReplayController` + `Replay.java` / `ReplayRepository` |
| 004 | D03 → D02 | `GET /v1/events/{id}` | Course↔Event link | `EventController.getEvent` + `Event.java` |
| 005 | D03 → D02 | `GET /v1/events/registrations/user/{id}` | Event attendance history | `EventController` registrations + `EventRegistration.java` |
| 006 | D03 → D04 | `GET /v1/events/{id}/materials` | Event materials for resources | `EventController` materials + `EventMaterial.java` |
| 007 | D03 → D04 | `GET /v1/events/institution/{id}` | Search indexing | Same as 001 (`EventController` + `Event.java`) |
| 008 | D03 → D04 | `GET /v1/replays` | Media library replays | `ReplayController` + `Replay.java` / `ReplayRepository` |
| 009 | D03 → D04 | `GET /v1/events/registrations/event/{id}` | Certificate attendance evidence | `EventController` registrations + `EventRegistration.attended` |

**Contract 2 correction (2026-09-23):** earlier drafts filtered `eventType=LIVE`, but `EventType` has no `LIVE` value (12 values: LECTURE … OTHER). LIVE is an **`EventStatus`** (17 states). Server-side `?status=LIVE` on the institution endpoint is supported (verified: `EventController` L48–65 → `findByInstitutionIdAndStatus…`); client fallback is `eventStatus == "LIVE"` on `EventResponse`.
