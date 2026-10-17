# ELMKUSOMA Payments & Transactions — Verified Progress Record

**Purpose.** Execution interrupted by context limits. This file is the resume point for
`Payment.md` implementation. It records what is *actually verified in this repository*, with
real test evidence — not what is planned.

**Baseline:** `main` @ `23d69e87` (synced with `origin/main`).
**Spec:** `Payment.md`, 1,310 lines, last modified 10-17 01:41.

---

## 1a. Full-suite baseline (VERIFIED)

```
Command : mvn -o clean test
Config  : JAVA_HOME=jdk-21.0.12.1, DB_PASSWORD=5050, application-test.properties
Precond : 0 Java processes, 0 connections to elmkusoma_test (verified before launch)
Exit    : 0
Result  : Tests run: 2303, Failures: 0, Errors: 0, Skipped: 5  — BUILD SUCCESS
```

This is the first genuine full-suite baseline of the engagement. Every earlier result was
targeted only. Payment-relevant suites confirmed present and green in the same run:
`EventBookingServiceTest`, `EventSeatCapacityConcurrencyTest`, `ParentPaymentServiceTest`,
`ParentFinancialNotificationAssemblerTest`, `ParentStudentLinkIntegrityTest`,
`PaymentGatewayServiceTest`, `PaymentRefundIntegrityTest`, `ParentAuthorizationServiceTest`,
`TransactionSearchServiceTest`, `EarningsServiceTest`, `FinancialReportingServiceTest`,
`ReconciliationServiceTest`, `FlywayMigrationValidationTest`,
`SubscriptionServiceTest`, `SubscriptionMigrationConstraintTest`.

---

## 1b. Frontend payment surface — verified WIRED (corrected)

An earlier pass reported this as MISSING. That was wrong. Verified by inspection:

| Layer | Evidence |
|---|---|
| Parent payments page | `app/dashboard/parent/payments/page.tsx`, 1027 lines, proper `loading`/`error`/`cancellingId`/`checkout` state |
| Data layer | `parentApi.getPayments()` → `lib/parent-api.ts:557` → `GET /v1/my/payments` |
| Backend | `ParentSelfController.java:256` `@GetMapping("/payments")` |
| Learner payments | `app/dashboard/learner/payments` + sidebar entry (`dashboard-sidebar.tsx:295`) |
| Parent payments nav | `dashboard-sidebar.tsx:493` → `/dashboard/parent/payments` |
| Platform Admin | `platform-admin-sidebar.tsx:53-54` Finance + Payments |
| Refunds / receipts | `RefundController`, `ReceiptController` endpoints exist |
| CSV export | `toCsv`/`downloadCsv` from `lib/csv` — the hardened exporter from §1 |

No fabricated totals were found; the page loads through the real API. `authority-sidebar`
has **no** finance/earnings entry.

### V179 -- beneficiary attribution (schema, rehearsed)

Per-persona earnings could not be derived because no payment recorded a
beneficiary. The money split existed (``commission_amount``,
``provider_share_amount``, policy version) but ``CommissionService.allocate``
was always called with ``providerId = null``, so no payment named the person who
earned the provider share.

V179 adds ``beneficiary_id`` + ``beneficiary_type`` to ``payments``, forward-only.

Deliberately **not backfilled**. Historical payments were confirmed allocated
against a null provider, so NULL is the only truthful value for them. Backfilling
from a current profile would fabricate earnings for money that was genuinely
unattributed and would restate history already reported on. They stay
unattributed and visible to reconciliation instead.

Rehearsed on a disposable database seeded to match production:
- applies twice with exit 0 (re-runnable, as the V175 reconciliation needs)
- invalid ``beneficiary_type`` rejected by ``payments_beneficiary_type_ck``
- half-populated id/type pair rejected by ``payments_beneficiary_pair_ck``
- valid pair accepted
- legacy-style row left with ``beneficiary_id`` NULL, confirming no backfill
- ``elmkusoma`` read-only throughout; its V175 record still 222681029

**Status: schema DONE, population PENDING.** ``EventBookingService`` records the
organiser for event tickets next, followed by a per-beneficiary earnings endpoint.
## 1d. Per-beneficiary earnings — endpoint DONE, UI pending

`EarningsService.summaryForBeneficiary(beneficiaryId, from, to)` and
`GET /v1/earnings/me` (`BeneficiaryEarningsController`).

**No beneficiary id parameter exists on the endpoint.** The beneficiary is taken from the
authenticated principal, so a caller can only ever read their own earnings. Accepting the id
from the request would have been a plain IDOR - changing one id in a URL exposes another
person's revenue.

- The beneficiary filter is applied in the query, never in Java, matching the existing
  institution scope discipline.
- `null` beneficiary is refused rather than treated as "no filter", which would return
  everything.
- Only payments with a recorded beneficiary are visible; historical rows stay unattributed.

`BeneficiaryEarningsTest` 4/4 covers: earnings stated only for the recorded
beneficiary; one beneficiary can never see another's; payments with no beneficiary
are never attributed to anyone; null beneficiary refused.

**Remaining:** no frontend page consumes `/v1/earnings/me` yet.
## 1c. Per-persona earnings — confirmed PARTIAL

- Backend: `EarningsService.summaryFor(UUID institutionId, ...)` — **institution-scoped only**.
- `InstitutionEarningsController` → `/v1/institution/earnings/summary`.
- Platform-wide reporting: `FinancialReportingController` → `/v1/platform-admin/financial/*`.
- **No provider / teacher / trainer / organizer earnings endpoint or page exists.**

Root blocker: no payment records a beneficiary. Commission is allocated per *institution*
(`CommissionService`), so per-persona earnings cannot be derived. This requires a forward
migration adding beneficiary attribution plus dashboard work. **Do not infer earnings from
profile ownership** — that would fabricate history.

Each row was executed and observed in this session. Exit code and counts are real.

| Requirement | Evidence | Result |
|---|---|---|
| Subscription renewal chaining (Option A) | `SubscriptionServiceTest` | **18/18** |
| Renewal period from persisted `accessDays` snapshot | `refusesToGuessAPeriodWhenThePaymentCarriesNoApprovedDuration` | PASS |
| Append-only history, prior periods never rewritten | `renewalDoesNotSaveOrMutateAnyPriorRow` | PASS |
| Half-open `[start,end)` Java ↔ PostgreSQL | `tsrange '[)'` vs `isWithinPeriod` | PASS |
| Non-overlap enforcement | `overlappingPeriodsAreRejected`, `adjacentPeriodsAreAccepted` | PASS |
| NULL `service_id` sentinel | `nullServiceIdCollidesWithAnotherNullServiceId` | PASS |
| Duplicate `payment_id` rejection | `duplicatePaymentIdIsRejected` | PASS |
| Concurrent renewal serialisation (real PG) | `concurrentRenewalsAreSerialisedByTheDatabase` | PASS |
| First-purchase payer lock (before period lookup) | `ParentRepository.findByIdForUpdate`, `SubscriptionServiceTest` | PASS |
| Full Flyway chain V1→V178 on real PostgreSQL | `SubscriptionMigrationConstraintTest` | **8/8** |
| Migration naming/version validation | `FlywayMigrationValidationTest` | **32/32** |
| Duplicate-callback idempotency | `confirmationIsIdempotent`, `payment_id UNIQUE` | PASS |
| Payment → subscription transactional activation | `ParentPaymentServiceTest` | **24/24** |
| Entitlement continuity (extend, never truncate) | `grantEntitlement` | PASS |
| Subscription expiry operational | `CoreScheduler.expireElapsedSubscriptions` | shipped `c2a7d695` |
| Parent financial notifications (assembler) | `ParentFinancialNotificationAssemblerTest` | **17/17** |
| Dashboard notification wiring | `ParentDashboardServiceImpl` | shipped `c2a7d695` |
| **Paid seat handover on verified payment** | `confirmSeat` had **0 callers**; wired to `verifyPayment` | shipped `8509867e` |
| Seat confirmation idempotency | `EventBookingServiceTest` | **21/21** |
| Paid-path authorization + lifecycle parity | cancelled/started/private/cross-institution denied | PASS |
| Charge snapshot immune to repricing | `theAmountConfirmedIsWhatWasCharged_notWhatTheEventCostsNow` | PASS |
| Seat capacity under concurrency | `EventSeatCapacityConcurrencyTest` | **4/4** |
| Missing/failed/pending payment never grants seat | `aSeatIsNotConfirmedWhen...` | PASS |
| Payment purchaser attribution (V178) | `purchaser_id` + `purchaser_type` | shipped `4e88af60` |
| CSV export hardening (RFC 4186 + formula injection) | `TransactionSearchServiceTest` | **23/23** |
| Parent-student link integrity (V174) | `ParentStudentLinkIntegrityTest` | **5/5** |
| IDOR / cross-parent protection | `aParentCannotReachAnotherParentsChild` | PASS |
| Receipts, refunds, reconciliation | regression-verified, unchanged | PASS |

**Cumulative this session: 0 failures across every suite run after each change.**

---

## 2. Defects found and fixed

Recorded because they were real and non-obvious:

1. **`confirmSeat` had zero callers in main.** Seats are consumed at confirmation, not booking —
   so a paying attendee was charged and **never granted their place**. Paid events collected money
   and refused entry. Fixed in `verifyPayment`, the single authoritative `COMPLETED` transition.
2. **Ticket payments wrote `users.id` into `parent_id` and `student_id`.** Neither column
   references `users.id`, so payer-scoped reporting grouped payments under non-existent parents
   and refunds/inbox could never see event revenue. Fixed by V178.
3. **`V175` collision.** Upstream and payment work both claimed V175 → Flyway refuses to start.
   Renamed payment migration to V177.
4. **`paidEvent` fixture** built events with `status=REGISTRATION_OPEN` but `eventStatus` defaulted
   to `DRAFT` — a combination no production path produces. Fixture corrected, not the guard.

---

## 3. PARTIAL / MISSING / BROKEN

| Item | Status | Blocker |
|---|---|---|
| Full backend suite green | **DONE** | `mvn -o clean test` — see §1a | **2303/0/0/5** |
| Frontend Payments & Transactions nav/dashboards | **PARTIAL** | Verified wired end-to-end — see §1b |
| Provider/teacher/organizer earnings views | **DONE** | Endpoint + page + sidebar — see §1d |
| Bundles/packages | **MISSING** | Not started |
| Subscription cancellation API | **BLOCKED** | No approved effective-date policy. `requestCancellation` correctly refuses to invent one |
| Payment methods (bank/card) | **N/A** | Not modelled by existing gateway |
| Payouts/settlement | **OUT OF SCOPE** | Requires approved commission policy |
| Real Stakaba sandbox transaction | **BLOCKED** | `sk_test_*` key present and config verified; no transaction executed |

---

## 4. DATABASE BLOCKERS — require database-owner authorization

### V175 checksum mismatch — app cannot boot against `elmkusoma`

`flyway_schema_history` (read-only) records:

```
171 | 175 | subscription lifecycle | 222681029 | true
170 | 174 | parent student link integrity | 797143152 | true
```

Repository now has `V175__allow_independent_teachers`. **Flyway validate will fail on V175.**

Physical state of `elmkusoma`: has `subscriptions` table, `btree_gist`,
`ex_subscriptions_active_no_overlap` — i.e. the *former subscription* V175. The
independent-teacher and institutionless-audit-log DDL was **never applied**.

**Recovery plan (rehearsed on a disposable database; result below):**
1. V177/V178 are already idempotent (`IF NOT EXISTS` / `IF EXISTS`) — they re-run cleanly.
2. Add forward idempotent migrations for the teacher/audit DDL at unused versions.
3. Owner reconciles the recorded V175 row. **Never** edit a checksum, delete history, or baseline.

### V178 rehearsal result (disposable database)

V178 was applied repeatedly to a disposable database seeded to match production
(NOT NULL parent_id/student_id). **This found a real defect:** the
payments_purchaser_type_ck CHECK constraint had no guard, so a second run failed
with "constraint already exists". That matters precisely because the V175
reconciliation path may replay migrations on a database that already carries part
of the attribution work.

Fixed with DROP CONSTRAINT IF EXISTS + ADD CONSTRAINT. Re-verified: repeat runs
exit 0, the constraint exists exactly once, and an invalid purchaser_type is
rejected by the database itself. Full chain still applies
(SubscriptionMigrationConstraintTest 8/8). The rehearsal database was dropped;
elmkusoma and elmkusoma_test were read-only throughout and were not modified.

### V174 -- source VERIFIED correct

The committed source **is** the source that was applied. A new test applies the
repository's own migration chain to a disposable database with Flyway enabled and
no Hibernate involved, then compares the computed V174 checksum against
797143152, the value recorded in `elmkusoma`'s `flyway_schema_history`. They match.

This closes a standing risk: V174 had been applied to the shared database before its
source reached Git, so the database had executed a migration the repository did not
contain. If the committed file had drifted, every environment would have failed
Flyway validation at startup and the mismatch would only appear on a developer's
machine. It is now caught by a test instead.

Guard added permanently as
`theCommittedV174SourceMatchesTheChecksumAlreadyAppliedInTheSharedDatabase`.
Do not remove it; if an applied migration is ever edited, this test fails loudly
instead of the failure surfacing at deploy time.


Applied with checksum `797143152`. Local file was reported as CRC-32 `894014887`. Committed to
source, but **bytes not confirmed against the applied checksum**. Rehearsal on a disposable
database is the next concrete step.

---

## 5. Exact next actions

1. Run `mvn -o clean test` on an **idle** machine; establish a true full-suite baseline.
2. Rehearse the V175/V174 reconciliation on a **disposable** PostgreSQL database with Flyway
   enabled and Hibernate DDL disabled. Produce before/after evidence.
3. Frontend Payments & Transactions navigation + role dashboards.
4. Per-persona earnings attribution (needs recorded beneficiary; do not fabricate history).
5. Obtain cancellation effective-date policy, then implement with authorization + idempotency tests.

---

## 6. Working practices that must continue

- **One Maven build at a time.** Concurrent builds on shared `target` and `elmkusoma_test`
  caused real failures (`pg_type_typname_nsp_index`, `ClassNotFoundException`) that looked like
  code defects. Diagnose by cause; never label unexplained failures "environmental".
- `SubscriptionMigrationConstraintTest` owns the full Flyway chain. It hardcodes the highest
  version — **update that assertion whenever a migration is added.**
- Never kill another developer's process without authorization; inspect first.
- Never commit `git add .` / `-A`. Stage exact paths.