# Teacher Formal Verification — Design & Decision Record

Status: Implemented (Phase 2). Zero migrations; reuses `verification_records`.

## Context

Teachers could publish learning offerings, host live classes, publish events/lessons/videos
and expose resources publicly without any formal approval step. Only institutions and
providers had a verification workflow. Independent (school-less) teachers additionally hit
a broken qualifications flow because `/v1/teachers/{id}/qualifications` requires the
institution request attribute.

## Decisions

### 1. Storage: reuse `verification_records`, no new tables

Teacher verification is a row with `entity_type = 'TEACHER'` and `entity_id = <teacher user id>`.
No migrations are introduced. Suspension and reactivation are also rows in the same table
(`SUSPENDED` / new `APPROVED`), so the full history is the audit trail, plus an explicit
`AuditLog` entry for admin suspend/reactivate.

### 2. Derived state (`TeacherVerificationStateService`)

`effectiveStatus` for a teacher user:

1. A `SUSPENDED` or `APPROVED` record **newer than any other such record** wins over any
   newer `PENDING` row — a pending re-submission cannot self-lift a suspension or change
   an approval (suspension is not self-liftable; only Platform Admin reactivate lifts it).
2. Reactivate writes a fresh `APPROVED` record, which therefore wins by recency.
3. Otherwise the newest record's status applies (`PENDING`, `CHANGES_REQUIRED`, `REJECTED`).
4. No records at all → `PROVISIONAL`.

`formallyVerified = true` only when the effective state is `APPROVED`.

### 3. What provisional/pending/changes/rejected/suspended teachers may do

Allowed: manage own profile, record qualifications, create and edit **draft** content.
Blocked (backend, immediately, on **new** actions only):

- publishing learning offerings or changing them to `PUBLISHED`
- starting a live session for a class whose publication state is `APPROVED`/`PUBLISHED`
- creating/updating events as `PUBLISHED`, event publish, event start-live
- lesson create/update as published, lesson publish
- video tutorial publish
- resource visibility `PUBLIC`

Forward-only blocking: already-published legacy content stays visible, in-progress
sessions finish, nothing is retroactively unpublished, nothing is auto-approved.

### 4. Enforcement layering (actor identity lives in different layers)

- **Service layer**: `LearningOfferingService.create/update` (PUBLISHED) and
  `LiveClassServiceImpl.startSession` (publication `APPROVED|PUBLISHED`, teacher resolved
  from the teacher row).
- **Controller layer**: `EventController`, `LearningController`, `VideoTutorialController`,
  `ResourceController` — guarded before institution-scoped request attributes are consumed.
- The shared guard `requireTeacherPublishOrDeliver` is **role-aware**: non-TEACHER roles
  (institution staff, platform admin, providers) are exempt; a missing or inactive user
  fails closed with 403. Violations raise `ForbiddenException` → HTTP 403.

### 5. Platform Admin review, suspend, reactivate

- Review reuses `PUT /v1/platform-admin/verifications/{id}/review`; `TEACHER` was added to
  the entity allowlist and the review switch, with an in-app notification to the teacher
  on every decision.
- `POST /v1/platform-admin/teachers/{userId}/suspend` and `.../reactivate` take
  `{ reason }`, append the corresponding verification record, write an audit entry and
  notify the teacher in-app. Suspension is verification-level: the teacher keeps login and
  profile; it is independent of `users.isActive`.

### 6. Document matrix (preliminary automated check, per BAND_GOVERNED service type)

| Service types | Required documents |
| --- | --- |
| `LIVE_CLASS`, `LIVE_SESSION`, `LIVE_EVENT` | qualification, certificate |
| `VOCATIONAL_MODULE`, `ACADEMIC_MODULE` | qualification, competency |
| `COURSE`, `RESOURCE`, `REPLAY` | qualification |

The matrix is exposed on `GET /v1/teachers/me/verification`
(`requiredDocumentsByServiceType`) and validated on submit. The Platform Admin makes the
final decision regardless of the automated check.

### 7. Self-service endpoints

- `GET /v1/teachers/me/verification` — effective state, matrix, full record history.
- `POST /v1/teachers/me/verification/submit` — `{ serviceTypes[], documents[], notes }`.
- `GET/POST /v1/teachers/me/qualifications` — keyed to the caller's **own** teacher row,
  fixing independent teachers (no institution header required).

### 8. Notifications & expiry

Decisions are delivered as in-app `notifyUser` notifications only (no email). Verifications
do not expire.

### 9. Test scaffolding, not product behaviour

The test seeders (`TestDataSeeder`, `TestDataSeederConfig`) fabricate one `APPROVED`
verification record for the seeded teacher user so existing suites keep their
pre-verification expectations. **Product code never auto-approves or grandfathers.**

## Frontend

- Teacher: `/dashboard/teacher/verification` — status card (with suspension/provisional
  explanations and reviewer note), application form (service types → required documents
  derived from the server matrix), qualifications add/list, review history. Sidebar entry
  under ACCOUNT. API: `teacherApi.getVerification/submitVerification/
  getMyQualifications/addMyQualification`.
- Platform Admin: verifications page gains per-row **Suspend teacher** for `TEACHER`
  submissions plus a manual **Suspend or reactivate a teacher** tool (user UUID + reason).
  API: `platformAdminApi.suspendTeacherVerification/reactivateTeacherVerification`.
- i18n: full en + sw key sets for both surfaces.

## Testing

- `TeacherVerificationStateServiceTest` (17 unit tests): derivation rules, suspension
  precedence, reactivate-wins, doc matrix.
- `TeacherVerificationGateSecurityTest` (7 end-to-end acceptance scenarios): provisional
  teacher may manage profile/qualifications/drafts but every publish/delivery path returns
  403; approved teacher passes all gates; suspended teacher is blocked backend-side while
  login/profile remain intact.
- Full backend suite: **2413 tests, 0 failures**.
- `tsc --noEmit` clean; `check:i18n` reports only the 19 pre-existing
  `platformAdmin.payments.refunds.*` keys (inherited defect, out of scope).

## Non-goals

Verification expiry, email notifications, retroactive unpublishing, per-institution
teacher verification, migration of legacy published content.
