# R7 — Permission / Role Consolidation: Design Document (DESIGN ONLY)

**Status:** DESIGN COMPLETE — NOT IMPLEMENTED (per remediation plan: R7 is
research/design only; implementation requires explicit approval).
**Date:** 2026-10-04
**Scope:** Backend authorization model (`@PreAuthorize`, role strings, JWT role
claim) and its frontend mirrors.

## 1. Current state (research findings)

### 1.1 Method-level annotations (primary mechanism)
- `@PreAuthorize` appears **100+ times** across controllers, almost always as
  inline role-set strings, e.g.
  - `AcademicController.java:29` — `hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')`
  - `AttendanceController.java:73` — `hasAnyRole('ADMIN','INSTITUTION_ADMIN','TEACHER','STUDENT','PARENT')`
  - `TeacherController.java:32` — `hasRole('TEACHER')`
  - `CertificateController.java:112` — `permitAll` (public certificate verify)
- The same role tuples are re-typed on nearly every method
  (`ADMIN, INSTITUTION_ADMIN, TEACHER` is repeated dozens of times).

### 1.2 Role vocabulary
- Spring roles come from the JWT role claim via the security filter chain;
  observed role tokens: `ADMIN`, `INSTITUTION_ADMIN`, `TEACHER`, `STUDENT`,
  `PARENT`, `OTHER_LEARNER`.
- Oversight roles exist as separate strings in other layers, e.g.
  `NATIONAL_ADMIN`, `REGIONAL_ADMIN`, `DISTRICT_ADMIN`
  (`LiveClassWebSocketHandler.isAuthorityRole`, line 1008-1012) — **not** the
  same vocabulary as the `@PreAuthorize` tokens above.
- `users.role` is the single source that feeds both the JWT claim and
  `@PreAuthorize` (kept consistent by `InstitutionPeopleService.java:103`).

### 1.3 Non-annotation authorization
- **Institution scope:** `OwnershipGuard` + repository filters (institution_id
  scoping) — orthogonal to roles.
- **WebSocket:** token validated at handshake (`JwtHandshakeInterceptor`);
  join gates re-check institution membership + observer authority
  (`LiveClassWebSocketHandler.handleJoin`, lines 144-214; regression test
  `LiveClassWebSocketHandlerJoinGateTest`).
- **Frontend:** role strings drive routing/nav (`lib/auth.tsx`, workspace
  landing logic, `dashboard/learner/layout.tsx:86`) — a *display* mirror of the
  backend model, duplicated per surface.

### 1.4 Problems observed
1. **Duplication:** role tuples re-declared 100+ times; a policy change (e.g.
   granting `OTHER_LEARNER` read access) requires touching many files.
2. **Vocabulary drift:** two role vocabularies (Spring `ADMIN…` vs oversight
   `NATIONAL_ADMIN…`) with ad-hoc bridging per call site.
3. **No central audit point:** cannot answer "which endpoints allow role X?"
   without grepping annotations.
4. **Drift risk between layers:** frontend nav gates and backend method gates
   are maintained independently.

## 2. Proposed design (for future implementation)

### Phase A — Central role constants (low risk, mechanical)
- Introduce `tz.elmkusoma.security.Roles` with `public static final String`
  tokens for every role, referenced from annotations
  (`@PreAuthorize("hasAnyRole(TeachingRoles.ADMIN_TEACHER)")` requires
  compile-time constants — define static final String tuples where Java
  allows, else code-style constants + a checklist).
- Add a unit test asserting every literal role token used in `@PreAuthorize`
  strings exists in `Roles` (parse annotations at test time).

### Phase B — Named permission groupings
- Replace raw tuples with a single domain vocabulary, e.g.
  `INSTRUCTOR = {TEACHER, INSTITUTION_ADMIN, ADMIN}`,
  `STAFF = {INSTITUTION_ADMIN, ADMIN}`,
  `AUTHORITY = {ADMIN, INSTITUTION_ADMIN}` + oversight roles.
- Deliverable: one `Permissions` class + migration of annotations grouped by
  controller, each group covered by a role-matrix test
  (`@ParameterizedTest` over roles × endpoint → 403/200).

### Phase C — Unified oversight roles
- Decide whether `NATIONAL_ADMIN/REGIONAL_ADMIN/DISTRICT_ADMIN` become first-class
  Spring roles (mapped from the role claim / institution type) or remain a
  scope dimension (institution_type + membership). Recommendation: keep them as
  a **scope dimension** (`OwnershipGuard` jurisdiction checks already model
  this) and expose a single helper `@AuthorityScope` rather than mixing them
  into `hasRole` strings.

### Phase D — Frontend mirror contract
- Export a generated role→capability map (from the backend constants) consumed
  by nav/landing gates so display rules cannot drift from server rules.
- Server remains authoritative (frontend gates are UX only).

## 3. Explicit non-goals
- No change to verified hardening: IDOR guards, ownership checks, institution
  scoping, throttling, OTP, JWT validation, payment reconciliation stay as-is.
- No mass annotation rewrite in this remediation cycle (R7 is design-only).
- No deletion of `@PreAuthorize` coverage — only centralization.

## 4. Risks & verification plan (for the future implementation phase)
- Each phase lands with: `mvn test` green, role-matrix tests added per touched
  controller, and a diff review confirming **no endpoint loosens** (assert the
  allowed-role set per endpoint before/after is identical).
- Freeze rule: characterization tests are written BEFORE moving any annotation.
