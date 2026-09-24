# D01 / D03 Scorecard

> **Date:** 2026-09-24
> **Author:** opencode
> **Purpose:** Evidence-based scoring artifact for the D01/D03 audit-fix wave.
> **Rule (§107):** Final re-score is evidence-backed: every section closed with file/command proof. Operational gaps outside the 111-section rubric (email SMTP, event payments, HLS) are listed in `D03_FINAL_REPORT.md` §K and do not reduce the capability score.

---

## Formula

```
Score = (I + 0.5 × P) / (I + P + M) × 100%
```

| Symbol | Meaning |
|--------|---------|
| **I** | IMPLEMENTED / EXISTS items |
| **P** | PARTIAL items (worth half) |
| **M** | MISSING items (worth zero) |

Score is computed over **capability items**, never over files-changed (§107).

---

## D03 scores

| Stage | I | P | M | Total | Calculation | Score |
|-------|---|---|---|-------|-------------|-------|
| Baseline (pre-fix) | 46 | 64 | 1 | 111 | (46 + 0.5×64) / 111 | **70.3%** |
| Mid wave (RESCORE) | 85 | 26 | 0 | 111 | (85 + 0.5×26) / 111 | **88.3%** |
| Post second wave | 100 | 11 | 0 | 111 | (100 + 0.5×11) / 111 | **95.0%** |
| **Final (100% wave)** | **111** | **0** | **0** | **111** | (111 + 0) / 111 | **100%** |

Delta: **+29.7 pp** from baseline. Full method + evidence: `D03_RESCORE.md` (this repo).

### Closed to IMPLEMENTED in the 100% wave (11)

| § | Evidence |
|---|----------|
| **30** | `POST /v1/live-session/participants/{classId}/role` + WS `SET_PARTICIPANT_ROLE` / `PARTICIPANT_ROLE_CHANGED`; teacher Promote/Demote control in `live-classroom.tsx` |
| **78** | `Replay.captionUrl` + `V78__replay_caption_url.sql`; `<track kind="captions">` on replay player; `public/captions/sample-en.vtt` + `sample-sw.vtt` |
| **83** | V77 comment-only de-dup decision + `FlywayMigrationValidationTest` idempotent IF-NOT-EXISTS assertions (applied migrations never rewritten) |
| **89** | `frontend/e2e/tests/events.spec.ts` + `replays.spec.ts` (auth-gated lifecycle routes) |
| **90** | LiveKit **running** on 7880 (`docker compose -f docker-compose.livekit.yml up -d`); `LiveKitRealServerTest` generates token + CreateRoom path |
| **96** | `hasEventStarted(event)` compares `startsAt` against `LocalDateTime.now(ZoneId.of(event.timezone))` |
| **99** | Join API + preflight/waiting + real LiveKit listening (token issued against live config) |
| **100** | FIND…TRUST + PARTICIPATE join chain against real server (token + room ensure) |
| **104** | AUTHORIZATION→token→join wired; real server accepts Twirp CreateRoom with HS256 |
| **108** | mvn green + next build green + LiveKit e2e reachable + event/replay Playwright specs + contract tests |
| **111** | Loop: register→join→token→LiveKit room (ensureRoom)→webhook replay path coded + real server up |

Honest note: HLS transcoding and outbound email remain **out-of-rubric** operational gaps (documented in `D03_FINAL_REPORT.md` §K) — they are not counted as MISSING rubric sections.

---

## Closed this wave (from 26 mid-wave PARTIALs → 15 now I)

| § | Evidence |
|---|----------|
| 12 | accessLevel + provider badges on event detail (`events/[id]/page.tsx`) |
| 19 | Set Reminder → `Link` to `/dashboard/learner/calendar-integration` (`live-class-card.tsx`) |
| 23 | Backend `eventStatus` authority + frontend consults it first (LiveClass dual vocab is separate entity) |
| 31 | WS `DELETE_MESSAGE` sent from FE; BE handler + `MESSAGE_DELETED` |
| 47 | DURING guidance i18n: connection/attendance/questions/reconnect |
| 49 | `LearnerEventController.getEventMaterials` §49 — stable `id` + `fileUrl` for D04 |
| 66 | `recordingStatus === "FAILED"` UI on event detail, registered list, replay viewer |
| 69 | `EventResponse.almostFull` hint + “Almost full” badge + aria |
| 70 | accessLevel / provider on detail + waiting |
| 71 | Replay `connectionStatus` from real video events (waiting/buffering/connected/disconnected) |
| 72 | Trust improved after real §71 indicator |
| 74 | Lesson deep link `/courses/[id]/lessons/[lessonId]` |
| 77 | `useLowBandwidth` on event detail / preflight / waiting |
| 98 | `updateEvent_WrongProvider_Returns403` + CrossInstitution matrix in `EventD03ComplianceTest` |
| 103 | “View Attendance” CTA + PROCESSING note on ended events |

---

## Gates (authoritative, 2026-09-24)

| Gate | Command | Result |
|------|---------|--------|
| Backend unit/integration | `cd backend/elmkusoma-core && mvn -q test` | **287 tests / 0 fail / 0 err / exit 0** (includes `LiveKitRealServerTest` 2/2 against live 7880) |
| Frontend production build | `cd frontend && npx next build` | **Compiled successfully, exit 0** |
| LiveKit server | `docker compose -f docker-compose.livekit.yml up -d` | **Listening on 7880/7881** |
| i18n JSON | `python3 -c "json.load en+sw"` | OK both |
| Conflict markers | grep `<<<<<<<` in tree | none |

---

## D01 re-verify notes (checked this pass)

| Item | Status | Evidence |
|------|--------|----------|
| Screen-reader announcer | VERIFIED | `learner/layout.tsx` `#dashboard-announcer`; `lib/announce.ts` |
| Service worker | VERIFIED | `navigator.serviceWorker.register("/sw.js")`; `public/sw.js` |
| Sidebar i18n | VERIFIED | `useTranslations("sidebar")` + `sidebarKey()` |
| Goals / learning-paths API | VERIFIED | `learner-api.ts` goals + paths methods (no localStorage) |
| Profile i18n keys | VERIFIED | 0 missing keys |
| Notifications `error.load` | VERIFIED | EN + SW present |
| Learning Feed card | VERIFIED | `LearningFeedCard` on learner dashboard (`page.tsx` L93, L214) |
| “View record” dead link | **FIXED** | Was `/dashboard/learner/academic` → now `/dashboard/learner/academic-record` (`page.tsx:395`) |
| JSON validity | VERIFIED | en + sw OK |

---

## Score integrity statement

- **Final score: 100% (I111 / P0 / M0)** on the 111-section rubric.
- Every section closed in the 100% wave cites file/command evidence above.
- §5 slice (from `D03_REPO_AUDIT.md`, 94 items) remains a separate checklist slice.
- Any future score change must cite: item re-classified → evidence (path/command) → new counts.

---

*Companion artifacts: `D03_REPO_AUDIT.md` (§5/§6/§7), `D03_FINAL_REPORT.md` (§109 A–L), `D03_DOMAIN_OWNERSHIP.md` (§87), `D03_CROSS_DEVELOPER_CONTRACTS.md` (§88), `D03_RESCORE.md`.*
