# Regional Administration Command Center — Forensic Audit & Traceability Report

**Date:** 2026-10-01
**Baseline commit:** `f97d6e82` (main) — all Regional Administration work is additive on top of it, uncommitted at audit time
**Auditor method:** static code inspection + runtime API verification + browser E2E + full automated test gates
**Source requirement:** *ELMKUSOMA — Regional Administration & Regional Education Governance Command Center* master prompt (§1–92)

**Evidence labels used below:**
`REQUIREMENT` = mandated behaviour · `FACT` = observable in code/DB · `VERIFIED` = re-checked at runtime with recorded output · `ASSUMPTION` = stated belief not directly observable · `INFERENCE` = conclusion drawn from FACT/VERIFIED items.

> **LIMITATION (ASSUMPTION):** the source file `~/Downloads/PROMPT.txt` disappeared from disk during this session (only the LibreOffice lock file `~/Downloads/.~lock.PROMPT.txt#` remains; Trash contains no copy). Section numbers cited here are reconstructed from the implementation record of this work stream. Requirement *content* is stated from that record; the numeric § references are `ASSUMPTION`-grade.

---

## 1. Gates — all green at audit time

| Gate | Command | Result | Label |
|---|---|---|---|
| Backend unit suite | `cd backend/elmkusoma-core && mvn -q test` | **585 tests / 0 failures / 0 errors / 0 skipped** (42 surefire reports) | VERIFIED |
| Jurisdiction hardening tests | `RegionalAdminServiceJurisdictionTest` | **15 / 15 pass** | VERIFIED |
| Frontend types | `cd frontend && npx tsc --noEmit` | **exit 0** | VERIFIED |
| Frontend production build | `cd frontend && npx next build` | **exit 0** (all routes compiled, proxy middleware emitted) | VERIFIED |
| Browser E2E (new spec) | `npx playwright test regional-admin.spec.ts --workers=1` | **7 / 7 pass** (1.4 min) | VERIFIED |
| Runtime API suite | curl matrix, section 3 below | all expectations met | VERIFIED |

---

## 2. Environment & fixtures (FACT / ASSUMPTION)

- Stack: Spring Boot 3.4.1 (`:8080`), Next.js 16.3.3 dev (`:3001`), PostgreSQL.
- **Port 3000 is occupied by an unrelated local project** (`kayani-app/frontend`, started by a different session). Our dev server was therefore started with `npx next dev -p 3001`, and `frontend/playwright.config.ts` was made `PLAYWRIGHT_BASE_URL`-aware so E2E can target any port. (INFERENCE: this pre-existing occupancy is why earlier `learning-content.spec.ts` runs failed — a server on 3000 that is not this app answers every request.)
- Test accounts (password `password` for all; hashes reset to the admin hash — documented environment fixture, not product behaviour):
  `regional.dar@test.com` (REGIONAL_ADMIN, Dar es Salaam), `regional.aru@test.com` (Arusha), `district.ila@test.com` (Ilala district), `district.arc@test.com`, `admin@elmkusoma.go.tz` (ADMIN), `teacherb@test.com` (TEACHER), `shamsa@gmail.com` (STUDENT, used only for the proxy-bounce test).
- **Seeding gap repaired (FACT):** every regional institution had **0 active memberships**, so announcement recipient resolution could only ever return 409. One existing teacher (`teacherb@test.com`) was attached to *Test Primary School Ilala* exactly the way `AuthServiceImpl.ensureMembership` does at registration (same SQL run, `UPDATE users.institution_id` + `INSERT institution_memberships`). Label: environment fixture, recorded here for full disclosure.
- Flyway state: `V110` row in `flyway_schema_history` renumbered to `V111` (source renumbered it in commit `4067900`), stale `target/classes` duplicates cleared with `mvn clean`. `V112__seed_geographic_jurisdiction_links.sql` applied. All pre-existing breakage, repaired — not introduced by this work.

---

## 3. Runtime verification matrix (VERIFIED)

Executed against `http://localhost:8080`; full transcript: `/tmp/opencode/verify-evidence.txt`.

### A. Authentication & role gate
| Request | Result | Expected |
|---|---|---|
| `GET /v1/regional-admin/dashboard` unauthenticated | **403** | 401/403 |
| Same endpoint with **ADMIN** token | **403** | 403 (regional API is REGIONAL_ADMIN/DISTRICT_ADMIN only) |
| Login `regional.dar@test.com` | **200**, JWT issued | 200 |

### B. Command centre surface (DAR regional token) — every endpoint
`dashboard 200` (jurisdiction `region / Dar es Salaam / DAR`, 3 districts, 3 institutions, 3 schools, 0 pending verifications, 1 data-quality issue, 8 quick actions) · `regions 200` · `attention 200` · `pulse 200` · `quick-actions 200` · `verifications 200` · `data-quality 200` · `compliance 200` · `audit 200` · `announcements 200` · `notifications 200` · `notifications/unread-count 200` · `performance 200` · `attendance 200` · `assessments 200` · `curriculum 200` · `reports 200` · `alerts 200` · `learners 200` · `teachers 200` · `education-staff 200` · `courses 200` · `resources 200` · `video-tutorials 200` · `live-classes 200` · `institutions 200` (real rows: *Dar es Salaam Model School*, *Test Primary School Ilala* → Ilala, *Test Secondary School Kinondoni* → Kinondoni) · `institutions/{id} 200` · `districts/{id} 200` (Ilala → *Test Primary School Ilala*) · `search?q=test 200`.

> `[…404] unread-count` in section B of the transcript is a **harness bug** (the loop built `/v1/regional-admin/unread-count`, a path that does not exist → correct 404). The real endpoint `/v1/regional-admin/notifications/unread-count` returned `200 {"count":0}` in section E.

### C. Cross-jurisdiction denials (the security core)
| Caller | Request | Result |
|---|---|---|
| Arusha regional | open Dar district `2201` | **403** |
| Arusha regional | filter institutions by Dar district | **403** |
| Ilala district admin | open sibling district Kinondoni | **403** |
| Ilala district admin | open region detail | **403** |
| Ilala district admin | list regions | **200 `[]`** (district admins have no region scope) |
| Arusha regional | `GET /v1/oversight/regions/{Dar}/institutions` | **403** |
| Dar regional | same endpoint, own region | **200** |
| Ilala district admin | `GET /v1/oversight/districts/{Kinondoni}/institutions` | **403** |
| Arusha regional | `search?q=school` | only *Test Primary School Arusha* (no Dar leak) |
| Ilala district admin | `search?q=test` | only Ilala school + its own teacher (no sibling leak) |

### D. Announcements (audience validation + scope enforcement)
| Case | Result |
|---|---|
| Arusha targets Dar district | **403** `You are not authorized to view this district` |
| Dar, `audienceType=ALL` | **200**, `recipientCount=1`, priority HIGH |
| Dar, `audienceType=DISTRICTS` + Ilala | **200**, `recipientCount=1`, targets `[2201]` |
| `audienceType=COUNTRY` | **400** `Unsupported audience type: COUNTRY (use ALL, DISTRICTS or INSTITUTIONS)` |
| Ilala district admin, own district `ALL` | **200**, `recipientCount=1` (district-scope auto-restriction) |

### E. Notification delivery (end-to-end)
Recipient `teacherb@test.com`: `GET /v1/notifications/unread-count` → **`{"count":9}`**; `GET /v1/notifications` → three `REGIONAL_ANNOUNCEMENT` items (*Dar regional notice*, *Ilala update*, *Ilala district note*), unread. Sender-scoped endpoint for the regional admin → `{"count":0}` (senders are not recipients).

### F. Quick-action link targets (frontend-dead-link check)
`communication` · `governance/verification` · `governance/data-quality` · `districts` · `institutions` · `governance/audit` · `/oversight/reports` · `institutions` — **every route exists in the frontend** (verified against the 19 emitted `page.tsx` routes).

---

## 4. Browser E2E (VERIFIED)

`frontend/e2e/regional-admin.spec.ts` — **7/7 passed**:

1. Regional admin login lands on *Regional Education Command Center*, single sidebar, jurisdiction chip **Dar es Salaam**, no learner/platform-admin links leaked.
2. Districts page lists real district **Ilala**, deep-links into a district detail.
3. Institutions page shows *Test Primary School Ilala*; search `Kinondoni` finds *Test Secondary School Kinondoni*; **Arusha school never appears**.
4. Governance → Data Quality surfaces the real finding *Dar es Salaam Model School*.
5. `/dashboard/regional-admin/search?q=Ilala` returns only in-scope records.
6. District admin deep-links into `/dashboard/regional-admin` and sees *Ilala* (its default landing remains the pre-existing `/dashboard/district` workspace — see §6).
7. Non-regional role gets a **30x redirect** from the edge proxy on `/dashboard/regional-admin` (`page.request`, `maxRedirects: 0`).

---

## 5. Traceability matrix

| # | Requirement (from work record) | Status | Implementation evidence | Runtime evidence |
|---|---|---|---|---|
| 1 | Regional/District-only workspace, edge-enforced | IMPLEMENTED | `frontend/proxy.ts:12,42,69` (`isRegionalAdminRoute`, both roles) · `RegionalAdminController.java:30` `@PreAuthorize` | §A (403 admin/unauth), E2E #1,#7 |
| 2 | Server-authoritative jurisdiction scope | IMPLEMENTED | `RegionalAdminService.java:105` `record Scope`, `:113 resolveScope`, `:148 assertRegionInScope`, `:1282 assertVerificationInScope`; district admins resolve to district-only scope | §C table, 15/15 unit tests |
| 3 | Command-centre dashboard (KPIs, attention, pulse, quick actions) | IMPLEMENTED | `GET /v1/regional-admin/dashboard|attention|pulse|quick-actions`; overview page `…/regional-admin/page.tsx` | §B, §F |
| 4 | District administration (list + detail drill-down) | IMPLEMENTED | `…/districts/page.tsx`, `…/districts/[id]/page.tsx` | §B district detail, E2E #2 |
| 5 | Institution governance view (verification, data quality, geo links) | IMPLEMENTED | `…/institutions/[id]/page.tsx`, `GET /v1/regional-admin/institutions/{id}` | §B, E2E #3 |
| 6 | People administration (learners, teachers, education staff) | IMPLEMENTED | `…/learners|teachers|education-staff/page.tsx` + scoped endpoints | §B (200 + scoped lists) |
| 7 | Learning inventory (courses, resources, videos) | IMPLEMENTED | `…/learning/{courses,resources,videos}/page.tsx` | §B (200) |
| 8 | Analytics reuse — no duplicate engines | IMPLEMENTED | sidebar → existing `/oversight/{performance,attendance,assessments,curriculum,reports}`; backend delegates to `OversightService` | §B analytics 200; oversight scope denials §C |
| 9 | Governance: verification queue | IMPLEMENTED | `…/governance/verification/page.tsx`, `GET …/verifications` | §B (200, `total=0` — honest empty queue) |
| 10 | Data quality = derived real findings | IMPLEMENTED | `GET …/data-quality` computes from real geo/verification state | §B: 1 critical — *Institution has no district link → Dar es Salaam Model School* |
| 11 | Compliance = derived checks, never faked | IMPLEMENTED | `GET …/compliance` + explanatory `note` | §B: 2 pass / 3 fail, note present |
| 12 | Audit trail scoped to jurisdiction | IMPLEMENTED | `…/governance/audit/page.tsx`, `GET …/audit` | §B 200 (`totalElements=0` — no audit rows in scope yet) |
| 13 | Announcements with validated audiences | IMPLEMENTED | `RegionalAdminService` audience switch: `ALL/DISTRICTS/INSTITUTIONS`, 400 on unknown, 5 000 cap | §D (403/200/400 matrix) |
| 14 | Notification fan-out to real members + unread counter | IMPLEMENTED | recipient resolution from active memberships; `GET …/notifications/unread-count` (regional roles excluded from generic counter) | §E (`count=9`, typed `REGIONAL_ANNOUNCEMENT`) |
| 15 | Navigation IA (§15 layout) | IMPLEMENTED | `frontend/components/dashboard/regional-admin-sidebar.tsx` — Districts / Institutions / People / Learning / Governance / Communication groups | E2E #1 (single sidebar), §F link-target check |
| 16 | Command palette entries for regional role | IMPLEMENTED | `frontend/components/ui/command-palette.tsx:31,195,377` | type-checked + build green |
| 17 | Workspace resolution & role routing | IMPLEMENTED | `frontend/lib/workspace.ts:26` → `/dashboard/district`; regional entries added earlier for `Regional Admin` | E2E #6 |
| 18 | Platform-admin institution geography fields | IMPLEMENTED | `InstitutionSummaryResponse`/`InstitutionDetailResponse` + `PlatformAdminService` expose `regionId/districtId`; `institution-form-modal.tsx` region/district selects | build/type gates |
| 19 | Geographic seed data | IMPLEMENTED | `V112__seed_geographic_jurisdiction_links.sql` (applied) | §B district→institution linkage |
| 20 | i18n for new chrome | IMPLEMENTED | `nav` (18), `commandPalette.nav` (9), `platformAdmin.institutionForm` (6) keys in `en.json` + `sw.json` | insert-only diff verified earlier |
| 21 | Regional search (scoped) | IMPLEMENTED | `…/regional-admin/search/page.tsx` (was an empty directory = dead link; page now written) + `GET /v1/regional-admin/search` | §C leak tests, E2E #5 |
| 22 | Jurisdiction-leak fix in shared oversight API | IMPLEMENTED | `OversightController.verifyRegionScope/verifyDistrictScope` | §C oversight 403/200 matrix |
| 23 | **Ward dimension** | **GAP (documented)** | no Ward domain exists in schema; not invented (per prompt §23 constraint) | n/a |
| 24 | **Scheduled/published reports** | **GAP (documented)** | no scheduler infrastructure; reports reuse `/oversight/reports` (per prompt §45 constraint) | n/a |
| 25 | **Fake compliance engine** | **NOT DONE (by design)** | compliance is derived from verification/audit/security/data facts and labelled as such | §B |

---

## 6. Defects found during verification and their disposition

| Finding | Origin | Disposition | Evidence |
|---|---|---|---|
| `GET /v1/regional-admin/attendance` → **500** `relation "attendance_summary" does not exist` (also broke `GET /v1/oversight/attendance` for every role) | **Pre-existing** — `InstitutionRepository` native query used singular table name; real table is `attendance_summaries` | **Fixed** (`InstitutionRepository.java:49`) | before: 500; after: 200 (§B) |
| Quick actions / attention cards pointed at non-existent routes (`/governance?tab=…`, `/verifications`, `/regional-admin/reports`) | Introduced in this work stream | **Fixed** to real routes; `/reports` → `/oversight/reports` | §F |
| Sidebar “Search” linked to an **empty** `search/` directory (404) | Introduced in this work stream | **Implemented** `search/page.tsx` (scoped, debounced) | E2E #5 |
| CORS allow-list hardcoded to `localhost:3000` | Pre-existing | **Changed** to origin patterns `http://localhost:*` / `http://127.0.0.1:*` (+prod domains), credentials preserved | preflight from `:3001` → 200 + `Access-Control-Allow-Origin` |
| E2E config pinned to `localhost:3000` | Pre-existing | **Made configurable** via `PLAYWRIGHT_BASE_URL` (defaults unchanged) | `playwright.config.ts` |
| Flyway “more than one migration with version 105” + `V110/V111` numbering drift | Pre-existing | **Repaired** (`mvn clean`, history row renumber) | clean boot, V110–V112 applied |
| District Admin lands on `/dashboard/district`, not the regional workspace | **Pre-existing product decision** (`workspace.ts:26`, “District Education Oversight”) | **Kept** — regional workspace remains deep-linkable for that role (proxy allows both) | E2E #6 |
| Zero memberships in regional institutions → announcements always 409 | Pre-existing seeding gap | Documented + one membership fixture added (§2) | §D/§E now exercise happy path |

---

## 7. Real data surfaced (no mock data anywhere)

- Dashboard: Dar es Salaam → 3 districts / 3 institutions / 3 schools / 1 data-quality issue (FACT, from DB).
- Data quality: **HIGH — “Institution has no district link” → Dar es Salaam Model School** (VERIFIED).
- Compliance: **not compliant — 2 passed, 3 failed** (geographic linkage, verification coverage, audit trail), with the explicit note that checks are derived (VERIFIED).
- Verification queue `0`, audit `0`, alerts `0` — reported as empty rather than fabricated (VERIFIED).

## 8. Assumptions & inferences

- `ASSUMPTION`: §-number citations are reconstructed (source prompt file missing, §1 limitation).
- `INFERENCE`: pre-existing E2E failures reported earlier were largely caused by the foreign app holding port 3000; with an explicit base URL the same harness is green (7/7).
- `ASSUMPTION`: `teacherb@test.com` membership is acceptable test-environment data; it mirrors production code paths and is disclosed in §2.
- `INFERENCE`: because oversight analytics endpoints now enforce region/district scope for regional callers, previously shared analytics pages cannot leak cross-jurisdiction data to this role (proved for institutions; other pages share the same scope object).

## 9. Verdict

**PASS with documented gaps.** All gates green (585 unit tests, tsc, production build, 7/7 E2E), the full runtime matrix meets or exceeds expectation, and every denial path (403/400) was exercised against live traffic. Known product gaps (Wards, scheduled reports) remain intentionally unimplemented; six defects found during verification — four pre-existing, two from this work stream — were repaired and re-verified.
