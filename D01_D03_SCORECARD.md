# D01 / D03 Scorecard

> **Date:** 2026-09-24
> **Author:** opencode
> **Purpose:** Evidence-based scoring artifact for the D01/D03 audit-fix wave.
> **Rule (§107):** No 100% claim without a full re-audit. Counts below are the **post-fix re-score** of all prior PARTIAL/MISSING sections against the current tree. Remaining PARTIAL items are listed with gaps.

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
| **Final (this wave)** | **100** | **11** | **0** | **111** | (100 + 0.5×11) / 111 | **95.0%** |

Delta: **+24.7 pp** from baseline. Full method + evidence: `D03_RESCORE.md` (this repo).

---

## Still PARTIAL (11) — why not 100%

| § | Remaining gap | Blocker |
|---|---------------|---------|
| **30** | Participation modes: `ROLE_MODERATOR` constant added; **no promote/demote API or UI** for presenter/moderator | Needs promote endpoint (D02/D03 contract) |
| **78** | No `<track>` / captions / transcripts for event or replay media | No VTT/caption assets exist in repo |
| **83** | V71/V75 still declare overlapping columns; V77 documents intentional IF-NOT-EXISTS no-op but does **not eliminate** duplication | Applied migrations never rewritten (Flyway checksum) |
| **89** | Contract MockMvc tests + Flyway tests added; **event/replay Playwright e2e** still missing (only auth/nav/a11y/responsive specs exist) | Needs Playwright specs for events/replays |
| **90** | Join issues short-lived token in MockMvc full chain; **not verified on a real LiveKit server** | LiveKit server undeployed |
| **96** | Scheduling/capacity still `LocalDateTime.now()`; timezone still metadata | Wall-clock redesign out of wave scope |
| **99** | Participate/Reconnect/Leave wired in API+UI; **unverified against real LiveKit** | LiveKit server undeployed |
| **100** | FIND/UNDERSTAND/ACCESS/CONTINUE/LEARN/TRUST strong; **real PARTICIPATE experience** not demonstrated | LiveKit server undeployed |
| **104** | AUTHORIZATION→token→join API wired + external fallback; **full chain not on real system** | LiveKit server undeployed |
| **108** | Tests pass + build pass + no-fake-data evidenced; still open: real LiveKit e2e, event mobile/desktop Playwright, D01/D02/D04 runtime contracts | Infra + e2e |
| **111** | Loop coded end-to-end in UI/API; **not demonstrated on a real LiveKit deployment** | LiveKit server undeployed |

Six of eleven remaining PARTIALs (90, 99, 100, 104, 108, 111) are **LiveKit-server-deployment** blockers, not code gaps.

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
| Backend unit/integration | `cd backend/elmkusoma-core && mvn -q test` | **285 tests / 0 fail / 0 err / exit 0** |
| Frontend production build | `cd frontend && npx next build` | **Compiled successfully, exit 0** |
| i18n JSON | `python3 -c "json.load en+sw"` | OK both |
| Conflict markers | grep `<<<<<<<` in tree | none |

D03 test suites inside the 285 run (all PASS):

| Suite | Tests |
|-------|------:|
| `security/EventSecurityTest` | 30 |
| `integration/EventLifecycleE2ETest` | 22 |
| `integration/LiveKitIntegrationTest` | 30 |
| `integration/EventD03ComplianceTest` | 49 (new this wave) |
| `integration/FlywayMigrationValidationTest` | 9 (new this wave) |
| **D03 subtotal** | **140** |

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

- **Do not claim 100%.** Final score is **95.0% (I100 / P11 / M0)** on the 111-section rubric.
- The 11 PARTIALs above are listed with file/command evidence so they cannot hide inside P.
- §5 slice (from `D03_REPO_AUDIT.md`, 94 items) remains a separate checklist slice and does not replace the 111-section score.
- Any future score change must cite: item re-classified → evidence (path/command) → new counts.

---

*Companion artifacts: `D03_REPO_AUDIT.md` (§5/§6/§7), `D03_FINAL_REPORT.md` (§109 A–L), `D03_DOMAIN_OWNERSHIP.md` (§87), `D03_CROSS_DEVELOPER_CONTRACTS.md` (§88).*
