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
| **Endpoint** | `GET /v1/events/institution/{institutionId}?status=PUBLISHED` (filtered client-side for `eventType=LIVE`) |
| **Input** | Same as Contract 1 |
| **Output** | Subset of events where `eventType == "LIVE"` with `startsAt`, `meetingUrl`, `status` |
| **Ownership** | D03 owns event status transitions |
| **Authorization** | Same as Contract 1 |
| **Error Behavior** | Same as Contract 1 |
| **Compatibility** | D01 filters client-side; no backend changes needed |

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
| **Authorization** | JWT token; `@PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER')")` |
| **Error Behavior** | 200 with empty list if no registrations; 403 if STUDENT |
| **Compatibility** | D04 checks `attended=true` to determine certificate eligibility |

---

## Contract Summary Matrix

| ID | From → To | Endpoint | Purpose |
|----|-----------|----------|---------|
| 001 | D03 → D01 | `GET /v1/events/institution/{id}` | Dashboard upcoming events |
| 002 | D03 → D01 | Same as 001 (filtered) | Dashboard live events |
| 003 | D03 → D01 | `GET /v1/replays/event/{id}` | Dashboard recording count |
| 004 | D03 → D02 | `GET /v1/events/{id}` | Course↔Event link |
| 005 | D03 → D02 | `GET /v1/events/registrations/user/{id}` | Event attendance history |
| 006 | D03 → D04 | `GET /v1/events/{id}/materials` | Event materials for resources |
| 007 | D03 → D04 | `GET /v1/events/institution/{id}` | Search indexing |
| 008 | D03 → D04 | `GET /v1/replays` | Media library replays |
| 009 | D03 → D04 | `GET /v1/events/registrations/event/{id}` | Certificate attendance evidence |
