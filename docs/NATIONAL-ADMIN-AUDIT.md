# National Education Governance & Oversight Command Center — §3 Implementation Audit

**Spec:** `docs/Nationaladmin.md`
**Audit mode:** read-only inspection of `backend/elmkusoma-core` and `frontend/` on branch `main` (HEAD `c363b80`)
**Evidence rules:** every row marked `VERIFIED` was confirmed by reading the cited file, querying the live database (`elmukusoma`, 201 tables, Flyway V111), or executing a live HTTP request against the running backend (port 8080, authenticated as `national@elmkusoma.go.tz`). `ASSUMPTION` and `INFERENCE` rows are labelled as such.

Legend: **VERIFIED EXISTING** · **PARTIAL** · **BROKEN** · **MISSING** · **DUPLICATED** · **INCONSISTENT** · **UNSUPPORTED** · **NOT APPLICABLE**

---

## 1. Governance backend (§5, §6)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1.1 | `/v1/oversight/**` governance endpoints | **VERIFIED EXISTING** | `oversight/controller/OversightController.java` — `@RequestMapping("/v1/oversight")`, class `@PreAuthorize("hasAnyRole('NATIONAL_ADMIN','REGIONAL_ADMIN','DISTRICT_ADMIN')")`, GET endpoints: `/dashboard`, `/regions`, `/regions/{id}/districts`, `/districts/{id}/institutions`, `/institutions/{id}`, `/schools`, `/performance`, `/attendance`, `/curriculum`, `/assessments`, `/live-classes`, `/alerts`, `/reports` |
| 1.2 | Jurisdiction derived server-side from authenticated user (never trusted from client) | **PARTIAL** | `OversightService` resolves region/district from `userId` request attribute → `User.getRegionId()/getDistrictId()` (`JwtRequestAttributeFilter.java:64`, `OrganizationContextResolver.java:169`). **However** `/regions/{id}/districts`, `/districts/{id}/institutions`, `/schools`, `/performance` … accept caller-supplied IDs with **no verification that the caller owns that region/district** → a REGIONAL_ADMIN can read another region's districts/institutions (cross-jurisdiction read). Institution detail is protected (`verifyInstitutionJurisdiction`), region/district listing is not. |
| 1.3 | Optional scope drill-down (national → region → district) validated server-side | **MISSING / INCONSISTENT** | `OversightService.getDashboard(UUID, UUID)` ignores request query params entirely; jurisdiction comes only from the caller's own user row. Frontend regional/district landings still send `?regionId=` / `?districtId=` (`app/dashboard/regional/page.tsx:45`, `app/dashboard/district/page.tsx:45`) which the backend **silently discards** → client believes it filters; server does not. |
| 1.4 | Real data (no fake/hardcoded) | **BROKEN (reports only)** | Dashboard/schools/performance/attendance/curriculum/assessments/live-classes/alerts all query real repositories (`OversightService` 882 lines: `attendanceSummaryRepository`, `reportCardRepository`, `liveClassRepository`, …). **But `getReports()` (lines 694–721) returns a hardcoded static list** of 8 `ReportSummary` records with `available(true)` and no data source. |
| 1.5 | Wards (§34) | **UNSUPPORTED** | No `ward` table (201-table inventory), no `Ward` entity/repository anywhere in `backend`. `regions` (25 rows) and `districts` (9 rows) exist. Ward-level drill-down cannot be implemented without an additive migration + seeding — out of scope for a truthful "reuse" implementation; documented as not implemented. |
| 1.6 | Data quality (§22) | **MISSING** | No `data_quality*` table, no service/controller reference. Can be **derived** from existing tables (see implementation plan §22 below). |
| 1.7 | Compliance / regulation coverage (§22) | **PARTIAL** | `audit/controller/AuditController` has `/v1/audit/compliance`, `/distribution`, `/logs` but class `@PreAuthorize("hasAnyRole('ADMIN','INSTITUTION_ADMIN')")` → **no national-admin access** (`AuditController.java:26`). |
| 1.8 | Verification governance (§22) | **PARTIAL** | `administration/controller/VerificationReviewController` (`/v1/verifications/**`, no class-level role lock; `PlatformAdminService.reviewProviderVerification` enforces platform-admin-or-delegation server-side). Table `verification_records` exists but is **empty (0 rows)**. No read-only "pending verifications" view scoped for NATIONAL_ADMIN. |
| 1.9 | Export/CSV reuse (§26) | **VERIFIED EXISTING (elsewhere)** | CSV/export patterns exist in `PlatformAdminController`, `AdministrationController`, `LiveSessionController` — none wired to oversight. |
| 1.10 | Scheduler reuse (§27) | **VERIFIED EXISTING (elsewhere)** | `config/CoreScheduler.java` — 6 `@Scheduled` methods. No scheduled-report feature or tables exist. |

## 2. Command Center landing & navigation (§8)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 2.1 | Single command center for national/regional/district levels | **DUPLICATED / INCONSISTENT** | Three parallel thin landings: `app/dashboard/national/page.tsx` (143 lines), `regional/page.tsx` (146), `district/page.tsx` (146) — each fetches `/v1/oversight/dashboard` — **plus** the full `app/oversight/` suite (9 pages, ~2500 lines) with the same data. `lib/workspace.ts` sends NATIONAL_ADMIN → `/dashboard/national`, so post-login the user lands on the **thin** page, not the full suite. |
| 2.2 | KPI cards: regions, districts, schools, institutions, teachers, learners, courses, subjects | **PARTIAL** | `OversightDashboardResponse` returns `totalRegions, totalDistricts, totalInstitutions, totalTeachers, totalStudents, totalUsers, totalLessons, totalClasses` (matches frontend interface). **Missing:** `totalCourses`, `totalSubjects` — `CourseRepository.countByIsDeletedFalse()` exists (`CourseRepository.java:73`) and `SubjectRepository` exists, so counts are cheap to add. |
| 2.3 | Learning pulse charts (§11) | **PARTIAL** | Attendance rate, average performance, curriculum progress returned by dashboard; `topRegions` table rendered. No time-series/trend data (no snapshots for authority role). |
| 2.4 | Attention center (§12) | **PARTIAL** | `/v1/oversight/alerts` computes **real** low-attendance (<75%) and low-performance (<40%) alerts from repositories (`OversightService:723+`). Missing governance items: pending verification, content reports, data-quality issues, unread counts. |
| 2.5 | Scope selector (§8) — click region → district → institution | **UNSUPPORTED** (until §1.3 fixed) | Drill-down endpoints exist (`/regions/{id}/districts`, `/districts/{id}/institutions`) but no validated scope parameter flow, and no scope selector UI in `app/oversight/page.tsx`. |
| 2.6 | Sidebar §7 groups (Education / People / Learning / Performance / Governance / Reports / Communication), collapsible, badges, scope indicator, quick actions | **MISSING** | `components/dashboard/authority-sidebar.tsx` — flat 9-item list (`authorityNav`, lines 31–40), no groups, no collapse, no badges, no scope indicator, no quick actions. |
| 2.7 | Sidebar has no dead routes | **BROKEN** | `authority-sidebar.tsx:99` links `/oversight/profile`, `:111` links `/oversight/settings` — **neither route exists** (`frontend/app/oversight/` contains only layout + the 9 content pages). |
| 2.8 | Notification bell works for authority roles | **BROKEN (verified live)** | `GET /v1/notifications/unread-count` with national-admin JWT → **HTTP 403**. Root cause: `learner/controller/NotificationController` `@PreAuthorize("hasAnyRole('STUDENT','OTHER_LEARNER','TEACHER','ADMIN','INSTITUTION_ADMIN','PARENT')")` excludes all three authority roles. Topbar polls this endpoint every 30s for every logged-in user (`dashboard-topbar.tsx`). |
| 2.9 | Notifications page for authority roles | **MISSING** | `dashboard-topbar.tsx:187` `notificationsPath()` returns `null` for National/Regional/District Admin → bell click navigates nowhere. |

## 3. Search (§28)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.1 | Global search works for national admin | **MISSING (blocked by authz)** | `GlobalSearchDropdown` calls `/v1/platform-admin/search`; `PlatformAdminController` is class-level `@PreAuthorize("hasRole('ADMIN')")` and URL-locked in `SecurityConfig.java:160` (`/v1/platform-admin/**` → `hasRole("ADMIN")`). The reusable service `PlatformAdminService.globalSearch(query, type, limit)` (line 835) queries real repositories (users, institutions, live classes, certificates) — **available for delegation**. |
| 3.2 | Search respects jurisdiction | **MISSING** | Existing `globalSearch` is platform-scoped with no region/district filter. |

## 4. Command palette (§30)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 4.1 | Ctrl+K palette for national admin | **MISSING** | `dashboard-topbar.tsx:154–156`: `paletteMode = isAdmin ? "platform" : isOrgAdmin ? "organization" : null`; keyboard handler gated on `canUsePalette`. Authority roles get `null` → shortcut and icon are disabled. Commands are hardcoded per mode (§020 pattern), so an "authority" mode is additive, not a duplicate. |

## 5. Communications (§23)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 5.1 | National announcements with audience targeting (nationwide / region / district / institution / role) | **MISSING** | Only `GET /v1/teachers/me/announcements` (TEACHER) and `GET /v1/learners/.../announcements` (read). No create endpoint for authority roles. `announcements` table columns: `author_id, class_group_id, institution_id, subject_id, title, content, priority` — **no audience/status/scheduled columns** (table also currently has 0 rows). |
| 5.2 | Announcement delivery reuses existing notification system | **VERIFIED EXISTING** | `learner/service/NotificationService.notifyUser(userId, title, message, type, targetType, targetId)` persists to `learner_notifications` + publishes event → **reuse target, do not build a second system**. `learner_notifications` (0 rows) has `user_id, title, message, notification_type, target_type, target_id, is_read`. |
| 5.3 | Platform-wide notification broadcast (admin) | **VERIFIED EXISTING (ADMIN-only)** | `PlatformAdminController` `GET/POST /v1/platform-admin/notifications` → `platform_notifications` table. Platform Admin ≠ National Admin (§4) → **must not be widened**; national broadcast goes through announcements. |

## 6. Reports (§26, §27)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 6.1 | Report catalogue from real source | **BROKEN / DUPLICATED** | Two independent hardcoded copies of the same 8 reports: server `OversightService.getReports()` (694–721) **and** client `app/oversight/reports/page.tsx:19–92` (`reportTypes` const). The page never calls `/v1/oversight/reports`. |
| 6.2 | Generate / download works | **BROKEN (dead button)** | `app/oversight/reports/page.tsx:159` — `<button>` "Generate" with **no `onClick`**, inside a `cursor-pointer` div with no handler. No export endpoint exists under `/v1/oversight`. Direct violation of §29. |
| 6.3 | Scheduled reports (§27) | **MISSING** | No scheduling tables; `CoreScheduler` exists but hosts no report jobs. Spec requires honest reporting over partial delivery → implemented as *not built* unless time allows. |

## 7. Existing services to REUSE (do not rebuild)

| System | Location | Reuse decision |
|---|---|---|
| Oversight aggregations | `oversight/service/OversightService` + 10 repositories | Extend (scope params, KPI fields, attention/data-quality) |
| Jurisdiction resolution | `JwtRequestAttributeFilter` / `OrganizationContextResolver` → `userId` attribute | Reuse as the only scope authority |
| Search | `PlatformAdminService.globalSearch` (repo-backed) | Delegate from a jurisdiction-filtered `/v1/oversight/search` — **not** a second engine |
| Notifications | `NotificationService.notifyUser` + `learner_notifications` | Reuse for announcement fan-out; fix role gate |
| Audit | `AuditController` + `audit_logs` (institution_id, user_role, action, entity…) | Add NATIONAL_ADMIN read via jurisdiction-scoped oversight endpoint (platform `/v1/audit` stays ADMIN/INSTITUTION_ADMIN) |
| Verification | `VerificationRecordRepository`, `VerificationReviewController` | Read-only pending list for attention center; review actions stay platform-admin/delegated (§4) |
| Content reports | `ContentReportRepository` (`content_reports`) | Feed into attention center |
| Export | existing CSV patterns in `PlatformAdminController` etc. | Same pattern for oversight report export |
| Scheduler | `config/CoreScheduler.java` | Only if scheduled reports are built |
| E2E | `frontend/e2e/*.spec.ts` (auth, navigation, accessibility, responsive) | Add governance scenario |
| Backend tests | 43 tests incl. `InstitutionScopeSecurityTest`, `NotificationControllerTest` | Extend for scope security |

## 8. NOT APPLICABLE / out of scope for this build

| # | Item | Reason |
|---|---|---|
| 8.1 | GIS maps, satellite imagery (§13) | No map/GIS infrastructure exists in repo; claiming a map would require a heavy new dependency — documented as not implemented rather than faked. |
| 8.2 | Biometric / hardware integration | No hardware integration exists; not fabricable. |
| 8.3 | Ward level (§34) | No ward schema (see 1.5). |
| 8.4 | Data import (§22) | `DataImportJobRepository` exists (ADMIN platform feature); National Admin bulk import not required for first delivery. |

## 9. Verified runtime facts

- `GET /v1/oversight/dashboard` (national JWT) → **200**, real data: `totalInstitutions=4, totalTeachers=3, totalStudents=2, totalUsers=12, totalRegions=25, totalDistricts=9, jurisdictionSummary={type:national,name:Tanzania,code:TZA}`, `topRegions` populated.
- `GET /v1/notifications/unread-count` (national JWT) → **403**.
- `GET /actuator/health` → **403** (not public; not required).
- DB row counts: `regions=25, districts=9, institutions=4, users=19, announcements=0, learner_notifications=0, verification_records=0, audit_logs=0`.
- `@EnableMethodSecurity` is active (`SecurityConfig.java:34`) → `@PreAuthorize` annotations are enforced.

## 10. Implementation plan derived from the audit

1. **Fix broken (backend):** widen `NotificationController` role gate to include the three authority roles; add server-side scope validation to every oversight endpoint taking region/district/institution IDs (closes cross-jurisdiction read, enables §8 drill-down).
2. **Extend dashboard:** optional validated `regionId`/`districtId` scope params + `totalCourses`, `totalSubjects`, `pendingVerifications`, `dataQualityIssues` KPI fields.
3. **New oversight endpoints (reusing existing services):** `/attention` (alerts + pending verifications + content reports + data-quality), `/data-quality` (derived, read-only checks), `/search` (delegates `globalSearch`, filtered by jurisdiction), `/audit` (jurisdiction-scoped `audit_logs`), `/reports/{id}/export` (real CSV from the same aggregations).
4. **Announcements:** additive Flyway migration (`announcement_audience_type/region_id/district_id`, `status`, `scheduled_at`, `published_at`) + `/v1/oversight/announcements` GET/POST (NATIONAL_ADMIN write) fanning out via `NotificationService`.
5. **Frontend:** §7 grouped sidebar with badges/scope indicator/quick actions (fix dead profile/settings links to existing `/dashboard/profile`, `/dashboard/settings`); Command Center on `/oversight` with scope selector; `/dashboard/{national,regional,district}` become redirects to the single command center; reports page fetches API + working CSV download; `/oversight/notifications` page; Ctrl+K authority mode using `/v1/oversight/search`; i18n (en+sw) for all new strings.
6. **Tests/validation:** `OversightScopeSecurityTest` (MockMvc: national 200, cross-region 403, teacher 403, anonymous 401), `NotificationControllerTest` national-role case, `mvn clean package -DskipTests`, `mvn test`, `npx tsc --noEmit`, `npm run build`, Playwright governance smoke.
