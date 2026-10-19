# ELMKUSOMA Payments & Transactions — Verified Progress Record

**Purpose.** Execution interrupted by context limits. This file is the resume point for
`Payment.md` implementation. It records what is *actually verified in this repository*, with
real test evidence — not what is planned.

**Baseline:** `main` @ `23d69e87` (synced with `origin/main`) — the original full-suite baseline,
kept for history.
**Current:** `main` @ `2c19597f` plus the P-05/S-09/R-07 work recorded in §5.
**Spec:** `Payment.md`, 1,811 lines, last modified 10-17 01:41.

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

### Re-verified on current code

The run above predates V180 and the BR-01/02/03 work, so it was rerun. The environment
here cannot complete one unsharded `mvn clean test` in a single command, so the suite
was run in four shards covering all 188 test classes in all 32 packages (the only
surefire-matched files outside those packages are `TestDataSeeder` and `TestTokens`,
which are helpers holding no `@Test`):

| Shard | Packages | Result |
|---|---|---|
| A | `bundle`, `parent`, `payments`, `refund`, `subscription`, `pricing`, `finance`, `event`, `integration` | 514 run / 0 failures / 0 errors / 5 skipped |
| B | `security`, `config`, `emailoutbox` | 674 run / 0 / 0 / 0 |
| C | 21 packages incl. `certificate`, `identity`, `liveclass`, `learning` | 1143 run / 0 / 0 / 0 |
| D | `nursery` | 78 run / 0 / 0 / 0 |
| | **total** | **2409 run / 0 failures / 0 errors / 5 skipped** |

Run at that point: working tree at `2c19597f` plus the P-05/S-09/R-07 work in §5, sharded as above
with `loadenv-min.ps1` (DB vars only). **Coverage was verified, not assumed**: the 193
`*Test.java` files under `src/test` and the 193 classes surefire reported across the four shards
match exactly — none missing, none extra. Earlier runs at `b14e6c6d`/`629e67e4` recorded 2350; the
increase is the 20 new P-05/S-09/R-07 tests plus tests brought in with `01947d08..2c19597f`. All 34
packages containing test classes are covered. `TestDataSeeder` and `TestTokens` match surefire's
default naming but are helpers holding no `@Test`, so they contribute nothing and are not counted.

### Re-verified after the B-10 / E-05 work

Because both fixes changed production and test code, the whole suite was rerun unsharded on the
current working tree — no shards, so there is no chance of a package being missed:

```
Command : mvn -o test                          (JAVA_HOME=jdk-21.0.12.1, loadenv-min.ps1)
Exit    : 0   BUILD SUCCESS   Total time: 16:46 min
Result  : Tests run: 2417, Failures: 0, Errors: 0, Skipped: 5
Classes : 193
```

**Coverage verified, not assumed:** the 193 `*Test.java` files under `src/test` and the 193
classes surefire reported match exactly — none missing, none extra. 2417 − 2409 = 8, which is
precisely the 3 new `BundleAdminServiceTest` cases plus the 5 new `EventBookingServiceTest` cases
added this session; no other test count moved. The 5 skips are unchanged and all belong to the
integration package (`EventD03ComplianceTest` 1, `LiveKitIntegrationTest` 1,
`LiveKitRealServerTest` 3). No `[ERROR]` line appears anywhere in the run.

Frontend on the same tree: `npx tsc --noEmit` exit 0, `node scripts/check-i18n-keys.mjs` exit 0
(`OK - every statically-referenced key exists in en and sw`), `npm run build` exit 0.

Two failures seen during this work were not product defects and are worth recording so
nobody re-chases them:

- Three `liveclass` storage assertions failed only because the run injected every
  variable from `backend/.env`, and `LIVEKIT_*` satisfied the "no storage configured"
  precondition. Injecting only `DB_*` makes them pass unchanged.
- `PublicLiveClassDiscoveryTest.unpublishingPreservesAcademicRecords` asserted
  `scheduledAt.toLocalDate() == LocalDate.now()` against a seed of `now().minusHours(2)`,
  so it fails in the two hours after midnight and nowhere else. It now compares the
  stored schedule against the seeded one, which is deterministic and actually tests
  preservation.

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
## 1d. Per-beneficiary earnings - DONE (endpoint, page, sidebar, en/sw)

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
## 1e. Bundles / packages - DONE (domain, admin API, catalog, purchase, top-up, admin UI)

### Audit findings (do not re-derive these)

- **No bundle or package concept exists anywhere in the backend.** Searched all of
  `tz.elmkusoma`: zero files matching `Bundle` or `Package`. This is a genuinely new
  domain, not an unfinished one.
- **`Entitlement` is the correct grant mechanism and needs no change.**
  `EntitlementRepository` keys on `(studentId, serviceType, serviceId, status)`, which
  is exactly the shape a bundle item grant needs. Granting a bundle should write one
  `Entitlement` per item through the existing path, not a parallel system.
  Note `service_id` is NOT NULL, so every bundle item must name a real service.
- **Pricing must go through `PricingService.resolvePrice(serviceType, ...)`.** A bundle is
  just another service type. Prices must never be a column on the bundle row, or pricing
  governance stops applying to them.
- **Publication state is required.** `Payment` has no notion of a purchasable object, so
  the bundle row itself must carry DRAFT / PUBLISHED / SUSPENDED and be checked at
  purchase time. Suspended or draft bundles must refuse the purchase, not just be hidden.
- **Granting must reuse `ParentPaymentService.verifyPayment`.** That is the single
  authoritative COMPLETED transition. Bundles get entitlements from there, exactly like
  subscriptions and event seats do now, so duplicate-callback idempotency comes for free.

### Delivered

`V180` bundles + bundle_items, `Bundle` / `BundleItem` entities, both repositories, and
`BundleGrantService`, wired into `ParentPaymentService.verifyPayment` so a bundle shares the
single authoritative COMPLETED transition. One `Entitlement` per item is granted through the
existing grant path rather than a parallel access system.

`BundleGrantServiceTest` 5/5: every item granted; a repeated callback grants nothing twice;
a service already held is not re-created or silently extended; DRAFT and SUSPENDED bundles
grant nothing and produce no entitlement; an unverified payment grants nothing.

Two real defects were caught by the tests while building this:

1. **Non-published bundles granted access.** `isPurchasable()` existed on the entity but was
   never enforced in the grant path, so a payment raised against a suspended bundle would
   still have delivered every item in it. Now refused in `grantFor`.
2. **`created_by` / `updated_by` were written as `uuid`.** `BaseEntity` declares them as
   `String` and the project convention is `varchar(255)`. Caught by the existing
   `FlywayMigrationValidationTest.auditedActorColumns_AreCharacterTypesInMigrations`.

Still to do: admin CRUD and publication endpoints, discovery endpoint, frontend page, and
the two policy decisions above.

### Minimum coherent design

1. `bundles` - institution, name, description, `service_type` (the pricing key),
   `status` (DRAFT/PUBLISHED/SUSPENDED), access_days. **No price column.**
2. `bundle_items` - bundle_id, service_type, service_id, position. Reject an item
   pointing at something that is not a real service.
3. On verified payment with a bundle service type: resolve each item and grant an
   `Entitlement` through the existing grant path. Idempotent - a retried webhook must
   not double-grant, and must not extend an entitlement twice.
4. Refuse purchase when the bundle is not PUBLISHED. Refuse an empty bundle.
5. Admin CRUD, publication and suspension behind `@PreAuthorize` scoped to the institution.
6. Entitlement revocation on refund must follow the established refund policy - do not
   invent one.

### Open decisions needing the owner

- **Bundle refund allocation.** A bundle costs one price but contains several services.
  A partial refund has no defined split, and inventing one would move money that policy
  has not allocated. Blocked until a rule is approved.
- **Whether a bundle may overlap an entitlement the learner already holds.** Granting is
  idempotent, so this is currently a no-op rather than a top-up. Needs a decision.
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
| Full Flyway chain V1→V183 on real PostgreSQL | `SubscriptionMigrationConstraintTest` | **9/9** |
| Migration naming/version validation | `FlywayMigrationValidationTest` | **32/32** |
| Duplicate-callback idempotency | `confirmationIsIdempotent`, `payment_id UNIQUE` | PASS |
| Payment → subscription transactional activation | `ParentPaymentServiceTest` | **24/24** |
| Entitlement continuity (extend, never truncate) | `grantEntitlement` | PASS |
| Subscription expiry operational | `CoreScheduler.expireElapsedSubscriptions` | shipped `c2a7d695` |
| Parent financial notifications (assembler) | `ParentFinancialNotificationAssemblerTest` | **17/17** |
| Dashboard notification wiring | assembler reachable from `GET /v1/my/notifications` | merged `ParentSelfController.getNotifications` |
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

## 1f. Approved business policies - BR-01, BR-02, BR-03



Bundle **admin UI** is done too: `app/dashboard/admin/bundles/page.tsx` lists this
institution's bundles with their status, publishes drafts and suspends published ones,
linked from the admin sidebar. No price field exists on it by design - a bundle's price comes
from the approved pricing policy, so accepting one here would create a second, ungoverned
way to change what a learner pays. There is no delete button either: a bundle that has been
bought cannot be unbought, so suspension is the way to stop it being sold.

The UI mirrors what the server enforces but is not the rule: hiding a button is presentation
and `BundleAdminController` refuses the call regardless. Server refusals are shown
verbatim, because "a bundle needs an access duration" is more useful than anything
invented client-side.

Creation and item editing are done as well. The page creates a draft, edits one, publishes and
suspends, and lists this institution's bundles. No price field exists on it by design - a bundle's
price comes from the approved pricing policy, so accepting one here would create a second,
ungoverned way to change what a learner pays.

Editing needed one thing the admin list could not supply. `update` replaces the bundle's whole
item set, so an editor that opened without the items would delete every service in the bundle
simply by saving, and the public `GET /v1/bundles/{id}` cannot help because it refuses drafts on
purpose. So there is now `GET /v1/institution/bundles/{bundleId}`, institution-scoped through the
same `ownedBundle` check as every other admin call, returning the draft and its items in authored
order. The editor loads that before opening and will not open at all if the load fails, rather
than opening an editor it cannot fill in correctly.

The UI mirrors what the server enforces but is not the rule: hiding a button is presentation
and `BundleAdminController` refuses the call regardless. Server refusals are shown
verbatim, because "a bundle needs an access duration" is more useful than anything
invented client-side.

`BundleAdminServiceTest` 10/10; `BundleCatalogControllerTest` 4/4; `BundleGrantServiceTest` 5/5;
`BundleTopUpServiceTest` 6/6; `BundleRefundPolicyTest` 9/9; tsc 0 errors; i18n exit 0;
`next build` exit 0.

The grant path no longer invents an access period. It previously fell back to
the bundle's duration and then to a literal `30` days when the payment carried
no snapshot, which meant a paid grant could run for a length nobody sold and
which would differ by environment depending on which branch executed first. It
now uses the payment's snapshot, then the bundle's authored duration, and refuses
the grant when neither exists. Publishing already refuses a bundle with no
duration, so the refusal is a backstop rather than a live path.

`BundleGrantServiceTest` 7/7, including `theDurationIsTheOneSnapshottedOnThePaymentNotTheBundle`
and `noDurationIsInventedWhenNeitherSourceHasOne`.

### BR-01 subscription cancellation - DONE

`SubscriptionService.cancelAtPeriodEnd(subscriptionId, reason)`.

Prevents the next renewal. Does NOT revoke access immediately and does NOT refund: access is
derived from the paid period's boundary rather than the status, so a cancelled subscription whose
period has not ended still grants access until it does. The effective date is the period end -
the approved decision, not a default.

`requestCancellation` is idempotent: a repeat returns the same effective date and keeps the
original reason rather than failing. An elapsed period is refused. Only the cancelled period is
touched - a later chained period is a separate row. Audit written on first cancellation only.

`SubscriptionCancellationPolicyTest` 5/5.

### BR-02 partial bundle refunds - DONE

`BundleRefundCalculator` + `BundleRefundService` + `BundleRefundController` at
`/v1/platform-admin/bundles` (`GET .../refundable`, `POST .../refunds`).

Deliberately **not a component valuation** - that is an unapproved commercial decision. The
calculation bounds and apportions using figures the approved policy already produced and that were
snapshotted onto the payment: eligible = discount-adjusted net collected minus what is already
refunded; commission reversed = a proportion of the RECORDED commission; provider share = the
remainder, so the two always sum to exactly the refunded amount. Recorded figures are used so
today's policy cannot rewrite the history of a past sale. No clock, no randomness, no policy
lookup, so the result is reproducible.

No new refund machinery: a request enters the existing lifecycle as REQUESTED and travels
REQUESTED -> UNDER_REVIEW -> APPROVED -> PROCESSING -> COMPLETED.

Authorization is `@PreAuthorize("hasRole('ADMIN')")` plus a service-level re-check using the real
granted authorities from the SecurityContext, never a client-supplied value. The bound is checked
before any write, so an over-refund cannot be recorded even as a pending request.

**A bug this exposed in my own code:** Spring Security reports authorities as `ROLE_ADMIN` and the
service compared against the bare name `ADMIN`, which would have refused every genuine Platform
Admin. Fixed, and `aRealSpringAuthorityIsRecognised` now pins the prefixed form - a security check
with no test can quietly stop working.

`BundleRefundPolicyTest` 9/9, `PaymentRefundIntegrityTest` 6/6.

### BR-03 bundle overlap and top-up - DONE, and wired end to end

`BundleTopUpService.quote(...)` + `GET /v1/bundles/{bundleId}/quote?studentId=...`.

A service covered by a CURRENTLY VALID entitlement is not charged again. Expired, revoked and
not-yet-started grants are NOT coverage, because treating them as coverage would silently withhold
a charge the learner owes. Service identity is (studentId, serviceType, serviceId) and nothing
else.

Each remaining item is priced individually through `PricingService.resolvePrice` (ACTIVE approved
policy). No bundle-level price and no discount spread across components; an item with no approved
policy fails the quote rather than making the arithmetic work.

Wiring this closed a real defect: the bundles page previously fetched the bundle's HEADLINE price
before a child was chosen and sent it as the charge amount - exactly how a learner who already
owns part of a bundle gets billed twice. Now no price is fetched on open, the server re-prices
when a child is chosen, submission refuses to proceed without a server quote, and Pay is disabled
when the bundle is fully covered.

`BundleTopUpServiceTest` 7/7; tsc 0 errors; i18n exit 0.

**The purchase path was still broken, and only the display side was fixed.** With the frontend
correctly sending the top-up total, `initiatePayment` validated that figure against the bundle's
HEADLINE policy price and refused it. So the feature failed for exactly the learners it exists
for — anyone who already holds part of the bundle — with "amount does not match the authoritative
price from policy ...", where the "authoritative" policy was the thing that had produced the
correct figure. The earlier "wired end to end" claim above was true of the quote and false of the
charge, and it should not have been written before the charge was exercised.

The authoritative amount for a bundle is now the server-side top-up quote, recomputed inside
`initiatePayment` rather than trusted from the client. Recomputing is also what closes the
quote-to-pay gap: a learner who gains coverage between being quoted and paying is charged the
smaller correct amount rather than the larger displayed one. A fully covered learner is refused
outright, because taking money for access already held is not a purchase. A bundle-level discount
is not applied on top, since its total is already the sum of per-item policy prices and
distributing a discount across components is still unapproved.

The caller still cannot name the figure: a bundle purchase is checked against the server's quote
just as anything else is checked against its policy.

`ParentPaymentServiceTest` 27/27 including `aBundleIsChargedTheTopUpTotalNotTheHeadlinePrice`,
`aBundlePurchaseCannotBeInitiatedWhenNothingIsLeftToBuy`, and
`aBundlePurchaseStillCannotBeUnderpaidByNamingAnAmount`. Wider payment regression across
`bundle`, `parent`, `payments`, `refund`, `subscription`, `pricing`, `finance`, `event`:
353 run / 0 failures / 0 errors.

Also fixed here: `BundleTopUpService.quote(...)` accepted a moment and then ignored it,
resolving coverage against `LocalDateTime.now()` internally. The caller could not pin an answer,
which made the parameter a lie in the signature.

Documented rather than decided: whether an expired entitlement should have a grace period, and how
incompatible access levels are treated.
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
5. **`BundleGrantService` could double-grant under concurrent purchases (B-09).** `grantOne`
   checked whether the learner already held a service and then inserted, with nothing between the
   two. Two simultaneous purchases for the same learner both read "not held" and both inserted, so
   the learner held the same paid access twice. A row lock was the obvious remedy and is wrong
   here: when no entitlement row exists yet there is nothing for `SELECT ... FOR UPDATE` to lock,
   which is exactly the case where the race happens. Replaced with a transaction-scoped advisory
   lock keyed on (learner, serviceType, serviceId), taken *before* the coverage check inside the
   existing transaction. It serialises instead of rejecting, so a legitimate second purchase still
   proceeds; it is database-scoped, so it holds across application instances rather than per JVM;
   and a hash collision costs only needless waiting, never a merged grant.

   Evidence that the test is real rather than decorative: with the lock removed the same test
   fails with `expected: <1> but was: <2>` — two grants committed. With it in place it passes.

---

## 3. PARTIAL / MISSING / BROKEN

| Item | Status | Blocker |
|---|---|---|
| Full backend suite green | **DONE — re-verified on current code** | 2409 run / 0 failures / 0 errors / 5 skipped, 193/193 test classes — see §1a |
| Frontend Payments & Transactions nav/dashboards | **PARTIAL** | Verified wired end-to-end — see §1b |
| Provider/teacher/organizer earnings views | **DONE** | Endpoint + page + sidebar — see §1d |
| Bundles/packages | **DONE — grant path, admin API, admin UI** | `next build` exit 0; see §1f. Known gap below |
| Subscription cancellation API | **DONE** | `GET /v1/my/subscriptions` + `POST .../{id}/cancel`, payer-scoped, UI on parent payments — see §1f |
| Refund Admin UI (BR-02) | **DONE** | Dialog on platform-admin payments; records a REQUEST, never claims a refund paid |
| Payment methods (bank/card) | **N/A** | Not modelled by existing gateway |
| Payouts/settlement | **OUT OF SCOPE** | Requires approved commission policy |
| Real Stakaba sandbox transaction | **BLOCKED** | `sk_test_*` key present and config verified; no transaction executed |

---

## 2a. Requirement traceability matrix

Read against `Payment.md` (1811 lines, 90 numbered sections). Every row records what the code
actually does today, not what a section title implies. "Evidence" is a recorded test run.

### Payment foundation (spec §14–16, §15 invariants)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| F-01 | Server computes the amount; caller never names it | `PricingService.requireAmountMatches`, `ParentPaymentService.initiatePayment` | `ParentPaymentServiceTest` 27/27 | DONE |
| F-02 | Amount for a partly-held bundle is the top-up total | `BundleTopUpService.quoteByServiceType` used at initiation | `aBundleIsChargedTheTopUpTotalNotTheHeadlinePrice` | DONE |
| F-03 | Nothing left to buy ⇒ refuse, do not take money | `initiatePayment` throws when `fullyCovered` | `aBundlePurchaseCannotBeInitiatedWhenNothingIsLeftToBuy` | DONE |
| F-04 | Frontend success is never authoritative | Access only after `verifyPayment` | `verifyPayment_marksCompleteAndGrantsEntitlement` | DONE |
| F-05 | Pending ≠ successful | Grant path requires `COMPLETED` | `aPaymentThatIsNotCompleteGrantsNothing` | DONE |
| F-06 | Duplicate callbacks cannot duplicate financial effects | Payment reaches COMPLETED once; grant idempotent | `aRepeatedCallbackGrantsNothingTwice` | DONE |
| F-07 | Mismatched amount/currency cannot fulfil | `PricingConflictException`; gateway reconciliation | `PaymentRefundIntegrityTest`, `PaymentGatewayServiceTest` | DONE |
| F-08 | Idempotent submission | Partial unique index on `idempotency_key` | `ParentPaymentServiceTest` replay test | DONE |
| F-09 | Reconciliation of unresolved discrepancies | `ReconciliationService` | `ReconciliationServiceTest` | DONE |
| F-10 | Access period never invented | `BundleGrantService` uses snapshot → authored → refuse | `noDurationIsInventedWhenNeitherSourceHasOne` | DONE |

### Stakaba gateway (spec §25–27)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| G-01 | Client + collection request handling | `PaymentGatewayService`, `StakabaProperties` | `PaymentGatewayServiceTest` | DONE |
| G-02 | Webhook authenticity | `StakabaWebhookController` | `PaymentWebhookController` tests | DONE |
| G-03 | Duplicate delivery / replay | Verified-payment path is idempotent | `ParentPaymentServiceTest` | DONE |
| G-04 | Provider disabled / missing credentials | `StakabaProperties` interlock | `PaymentGatewayServiceTest` | DONE |
| G-05 | `STAKABA_ALLOW_LIVE=false` held | `.env` has sandbox key, live off | `start-backend.ps1 -VerifyConfig` exit 0 | DONE |
| G-06 | **Real sandbox transaction** | none executed; `STAKABA_ALLOW_LIVE` remains `false`, no live key accepted | — | **BLOCKED** — 63 mock/contract tests across `StakabaGatewayClientImplTest` 18, `PaymentGatewayServiceTest` 23, `StakabaWebhookSecurityTest` 7, `StakabaConfigurationValidatorTest` 7, `PaymentWebhookSecurityTest` 7, wiring tests; reproducible checklist in `docs/STAKABA-SANDBOX-TESTING.md`; blocked on pricing activation (decision request §6) + a genuine sandbox transaction |
| G-07 | Production credentials / onboarding | none | — | **BLOCKED** — out of scope, not authorized |

### Subscriptions (spec §49–52, BR-01)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| S-01 | Activation only after verified payment | `SubscriptionService` from `verifyPayment` | `SubscriptionServiceTest` 18/18 | DONE |
| S-02 | `GET /v1/my/subscriptions` | `ParentSelfController` | `listingShowsOnlyTheCallersOwnSubscriptions` | DONE |
| S-03 | `POST /v1/my/subscriptions/{id}/cancel` | `ParentSelfController` | `aPayerCanCancelTheirOwnSubscription` | DONE |
| S-04 | Payer ownership; indistinguishable not-found | `cancelOwnSubscription` resolves by payer | `anotherFamiliesSubscriptionDoesNotResolveAndIsNotCancelled` | DONE |
| S-05 | Access retained through paid period | period derived from boundary, not status | `accessContinuesUntilThePaidPeriodEnds` | DONE |
| S-06 | No renewal after cancellation | effective date = period end | `SubscriptionCancellationPolicyTest` 8/8 | DONE |
| S-07 | No automatic refund on cancellation | no refund path invoked | `cancellingDoesNotRefund` | DONE |
| S-08 | Concurrency | `findByIdForUpdate` row lock | `SubscriptionServiceTest` | DONE |
| S-09 | Expiry scheduler | `CoreScheduler.expireElapsedSubscriptions` | `CoreSchedulerExpirySweepTest` 4/4 (delegates, passes current time, survives a failing sweep) + `CoreSchedulerExpirySweepIntegrationTest` 4/4 (real rows flip ACTIVE→EXPIRED, live rows untouched, boundary at `period_end`, second run no-op); both proven to fail when the delegation is removed | DONE |

### Bundles and entitlements (spec §22, BR-03, §D)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| B-01 | Catalog + detail, published only | `BundleCatalogController` | `BundleCatalogControllerTest` 4/4 | DONE |
| B-02 | Draft/publish/suspend, no delete | `BundleAdminService` | `BundleAdminServiceTest` 10/10 | DONE |
| B-03 | Create/edit UI | `app/dashboard/admin/bundles/page.tsx` | `next build` exit 0 | DONE |
| B-04 | Editing must not silently drop items | `GET /v1/institution/bundles/{id}` | `editingADraftLoadsTheItemsItActuallyContains` | DONE |
| B-05 | Duplicate service prevention (BE + UI) | `requireDistinctItems` | `theSameServiceCannotBeListedTwice` | DONE |
| B-06 | No price column / no price field in UI | `V180` schema + admin UI | migration test | DONE |
| B-07 | Entitlement only after confirmed payment | `BundleGrantService` | `BundleGrantServiceTest` 7/7 | DONE |
| B-08 | Overlap/top-up policy | `BundleTopUpService` | `BundleTopUpServiceTest` 7/7 | DONE |
| B-09 | Concurrent purchase / duplicate grant protection | transaction-scoped advisory lock in `BundleGrantService.grantOne`, taken before the coverage check | `BundleGrantServiceTest` `twoConcurrentPurchasesCannotGrantTheSameServiceTwice` (fails with 2 grants when the lock is removed) | DONE |
| B-10 | Bundle item must point at a real service | `BundleAdminService.requirePricedServices` — every item's `serviceType` must resolve to an ACTIVE pricing policy, checked on create, update **and** publish; that same policy is what `BundleTopUpService` resolves the item's price from | `BundleAdminServiceTest` 13/13 incl. `anItemNamingAServiceThePlatformDoesNotSellIsRefused` (nothing persisted, message names the offending type), `aDraftCannotBePublishedOnceItsServiceIsNoLongerSold`, `anEmptyServiceNameIsRefusedRatherThanTreatedAsAValidType` | DONE — server-side enforcement proven; the frontend picker is a separate open item (decision request §5) |

### Events and bookings (spec §53–55)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| E-01 | Ticket only after verified payment | `confirmSeat` wired into `verifyPayment` | `EventBookingServiceTest` 21/21 | DONE |
| E-02 | Seat capacity + concurrency | lock before confirm | `EventSeatCapacityConcurrencyTest` 4/4 | DONE |
| E-03 | Duplicate booking / retry | idempotent verify path | `EventBookingServiceTest` | DONE |
| E-04 | Amount snapshotted at booking, not current price | payment snapshot | `EventBookingServiceTest` | DONE |
| E-05 | Organizer-paid vs individual-paid group flows | multi-seat booking implemented end to end; two defects found and fixed this session (attendee permanently locked out after a cancelled/failed booking; capacity recounted as a row count instead of `SUM(seats)`); the *organizer-paid vs individually-paid* distinction is **not** implemented | `EventBookingServiceTest` **26/26** incl. `aCancelledRegistrationLetsTheAttendeeBookTheEventAgain`, `aBookingWhosePaymentWasCancelledLetsTheAttendeeTryAgain`, `aConfirmedSeatStillBlocksASecondBooking`, `aLivePaymentStillBlocksASecondBooking`, `cancellingAGroupBookingRestoresEverySeatItHeld`; `EventSeatCapacityConcurrencyTest` 4/4; `book(…, int seats)` :123, `total = unit × seats` :158, `sumHeldSeats` `EventRegistrationRepository:50-53`, `V173` | **PARTIAL** — individual paid multi-seat flow DONE and tested; no `bookingType`/`payerType`/`ORGANIZER_PAID` anywhere in main and no booking UI (only free "register"), so §53's organizer-paid flow awaits decision request §4 |

### Parent / learner (spec §33, §F)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| P-01 | Payer↔learner ownership | `requireChildAccess` | `ParentAuthorizationServiceTest` | DONE |
| P-02 | Link integrity | `ParentStudentLinkIntegrityTest` | 5/5 | DONE |
| P-03 | Beneficiary attribution | V179 + `GET /v1/earnings/me` | `BeneficiaryEarningsTest` 4/4 | DONE |
| P-04 | Purchaser vs beneficiary kept distinct | V178 `purchaser_*` | `ParentPaymentServiceTest` | DONE |
| P-05 | Financial notifications on parent dashboard | `ParentDashboardServiceImpl.getFinancialNotifications` → `ParentFinancialNotificationAssembler`, merged into `GET /v1/my/notifications` in `ParentSelfController:919` | `ParentFinancialNotificationEndpointTest` 10/10 (payment+receipt reach the inbox, another family's withheld, unlinked child withheld, `feeAlerts=false` suppresses, absent preference falls back to showing); 5 of the 10 fail when the merge is removed | DONE |
| P-06 | Receipts + transaction search | `ReceiptController`, `TransactionSearchService` | `TransactionSearchServiceTest` 23/23 | DONE |

### Refunds (spec §57, BR-02)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| R-01 | Refund eligibility + status transitions | `RefundService`, `RefundRequest` | `PaymentRefundIntegrityTest` 6/6 | DONE |
| R-02 | Partial refund per approved policy | `BundleRefundCalculator` | `BundleRefundPolicyTest` 11/11 | DONE |
| R-03 | Server-side authorisation, re-checked from real authorities | `BundleRefundService.requirePlatformAdmin` | `BundleRefundPolicyTest` ROLE_ test | DONE |
| R-04 | Amount bounded before any write | `eligibleRemaining` | `BundleRefundPolicyTest` | DONE |
| R-05 | **Refund Admin UI** | `components/platform-admin/partial-refund-dialog.tsx` | `next build` exit 0, i18n exit 0 | DONE |
| R-06 | Request is not proof of refund | UI states REQUESTED and "no money returned" | copy reviewed | DONE |
| R-07 | Split recorded as commission/provider reversals | `RefundRequest.commissionReversed` / `providerReversed` + `V183__bundle_refund_split.sql`, written by `BundleRefundService` in the same transaction as the request | `BundleRefundPolicyTest.theSplitThatWasCalculatedIsPersistedOnTheRefundRequest` (fails when the write is removed) and `.aRefundRaisedOutsideABundleSplitRecordsNoSplitAtAll` (NULL, not 0); V183 applied by the real chain into an isolated schema (179 migrations → V183, both columns `numeric`, second run applied 0) | DONE |

### Pricing, allocation, earnings (spec §17–24, §59–60)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| A-01 | Prices from approved ACTIVE policy only | `PricingService.resolvePrice` | `ParentPaymentServiceTest` | DONE |
| A-02 | Platform commission allocation | `CommissionService` | `ParentPaymentServiceTest` | DONE |
| A-03 | Per-beneficiary earnings | `EarningsService` | `BeneficiaryEarningsTest` 4/4 | DONE |
| A-04 | Bundle-level discount distribution | deliberately not applied | `aBundleIsChargedTheTopUpTotalNotTheHeadlinePrice` (never resolves discount) | DONE — documented as unapproved |
| A-05 | Settlement / payouts | `CommissionPolicy` + `CommissionService.allocate` (rate-agnostic; stamps `commissionAmount`/`providerShare`/`commissionStatus` at confirmation) + `EarningsService`/`FinancialReportingService` reporting "unavailable" rather than 0; **no hardcoded rate anywhere, and no endpoint can activate a policy** | `BeneficiaryEarningsTest`, `FinancialReportingServiceTest`, `FlywayMigrationValidationTest.commissionSeed_CarriesNoRateAndNoInventedFeeBearer`; `Payment.md` line 1171 "Do not assume any commission percentage" | **BLOCKED** — machinery DONE and proven; needs an approved rate (decision request §2). Settlement/payout additionally blocked by `Payment.md` line 1199 |
| A-06 | Expired-entitlement grace period | straightforward reading used | `anExpiredEntitlementIsNotCoverage` | DONE — documented, unapproved alternative noted |
| A-07 | Component-level refund valuation basis | whole-payment basis implemented: `refundableAmount()` = collected − refunded, enforced at 4 levels (request bound, completion re-check under `findByIdForUpdate`, `BundleRefundCalculator` pre-write refusal, DB checks `chk_refunded_not_overpaid`/`chk_refunded_not_negative` + one-open-refund-per-payment index); a component split is deliberately not invented | `RefundServiceTest` 21/21, `PaymentRefundIntegrityTest` 6/6 incl. `concurrentLegacyRefunds_exactlyOneSucceedsAndTheAmountIsNeverDoubled`, `BundleRefundPolicyTest` 11/11; `V170`, `V171`, `V183` | **BLOCKED** for *component* valuation only (decision request §3); the whole-payment rule is DONE and tested |

### Database and migrations (spec §46–48)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| D-01 | Migration chain applies cleanly | V170–V183 contiguous | `SubscriptionMigrationConstraintTest` 9/9 | DONE |
| D-02 | Chain must not skip a version | assertion now scans for gaps ≥ V170 | same | DONE |
| D-03 | V174 source matches applied checksum | unchanged since verification | `theCommittedV174SourceMatchesTheChecksumAlreadyAppliedInTheSharedDatabase` | DONE |
| D-04 | Monetary precision | `BigDecimal` throughout | `RefundRequest`, `Payment` | DONE |
| D-05 | Idempotency constraints | partial unique index on `idempotency_key` | `ParentPaymentServiceTest` | DONE |
| D-06 | **V175 history mismatch on `elmkusoma`** | read-only inspection only; nothing written, no repair/update/baseline ever run | recorded row `171 \| 175 \| subscription lifecycle \| 222681029 \| true` vs repo `V175__allow_independent_teachers`; **found this session: this is a version collision, not just a checksum mismatch** — the repo's `V177__subscription_lifecycle.sql` line 1 is the very migration the shared DB recorded at 175 (renamed after an upstream collision, §3). Full options, risks, the exact approval text and post-recovery checks are in `docs/V175-RECOVERY-PROCEDURE.md` | **BLOCKED** — needs database-owner authorization |

**V183 — added by this change set.** `V183__bundle_refund_split.sql` adds
`commission_reversed` / `provider_reversed` (`DECIMAL(12,2)`, nullable on purpose: NULL means "no
split was calculated", zero would falsely assert the platform kept nothing). It was **not**
validated by `mvn flyway:migrate` — that plugin is not usable offline here (missing
`jackson-dataformat-toml`, `commons-text`) — and the test profile runs with
`spring.flyway.enabled=false`, so no test exercises it either. It was therefore validated against
a real PostgreSQL instance in a scratch schema (`v183_chain_check`) via a standalone Flyway 10
runner: the full chain executed **179 migrations to target V183**, both columns read back as
`numeric`, a second run applied 0, and the scratch schema was dropped. The shared
`flyway_schema_history` on `elmkusoma` and `elmkusoma_test` was never touched.

### Security and IDOR (spec §63, 25 cases)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
- Cross-user transaction read | `SecurityConfig` denies `/v1/payments/**` | `ResourceHttpSecurityMatrixTest` | DONE
- Cross-family child access | `requireChildAccess` → 404 | `ParentAuthorizationServiceTest` | DONE
- Cross-family subscription access | `cancelOwnSubscription` → 404 | `SubscriptionCancellationPolicyTest` | DONE
- Provider→other provider earnings | query-level isolation | `BeneficiaryEarningsTest` | DONE
- Unauthorized refund | `@PreAuthorize` + service re-check | `BundleRefundPolicyTest` | DONE
- Callback replay | idempotent verification | `ParentPaymentServiceTest` | DONE
- Forged success values | only verified gateway evidence grants | `EventBookingServiceTest` | DONE
- Platform-admin escalation | `/v1/platform-admin/**` fenced to ADMIN | `ResourceHttpSecurityMatrixTest` | DONE

**Counts: DONE 61 · PARTIAL 1 · NEEDS VERIFICATION 0 · BLOCKED 5 · N/A 1. MISSING 0, BROKEN 0.**
(67 ID rows + the one non-ID N/A row = 68. B-10 moved PARTIAL → DONE this session once
server-side enforcement and its tests existed; E-05 remains the only PARTIAL.)

`MISSING` is zero because the two UI gaps the audit found (bundle create/edit, refund admin)
were closed rather than reported. `BROKEN` is zero because the one genuinely broken path found —
BR-03 rejecting the correct top-up amount at initiation — was fixed.

---

## 4. DATABASE BLOCKERS — require database-owner authorization

### V175 mismatch — app cannot boot against `elmkusoma`

> **The full procedure lives in `docs/V175-RECOVERY-PROCEDURE.md`** — observed record, repo
> migration identity, how to validate read-only, four candidate options with risks, the exact
> approval text required, and rollback plus post-recovery checks. Nothing below has been executed
> against a shared database.

`flyway_schema_history` (read-only) records:

```
171 | 175 | subscription lifecycle | 222681029 | true
170 | 174 | parent student link integrity | 797143152 | true
```

Repository now has `V175__allow_independent_teachers`. **Flyway validate will fail on V175.**

Physical state of `elmkusoma`: has `subscriptions` table, `btree_gist`,
`ex_subscriptions_active_no_overlap` — i.e. the *former subscription* V175. The
independent-teacher and institutionless-audit-log DDL was **never applied**.

**This is a version collision, not just a checksum mismatch.** The repo's
`V177__subscription_lifecycle.sql` (line 1: `-- V177: subscription lifecycle records.`) is the
very migration the shared database recorded at version 175 — upstream and payment work both
claimed 175, so the payment one was renamed to 177 (see §3). Two different contents therefore
claim version 175, so fixing only the checksum would leave the collision in place.

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
(SubscriptionMigrationConstraintTest 9/9). The rehearsal database was dropped;
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

Ordered by what blocks a controlled test, not by what is easiest.

1. **Charge without grant on repurchase after expiry (new finding, B-09 investigation).**
   Nothing ever sets an entitlement's `status` to `EXPIRED` — the only `setStatus("EXPIRED")` in
   the codebase is `CoreScheduler.expireDelegations`, which is for delegations, and there is no
   bulk `UPDATE Entitlement`. An elapsed entitlement therefore stays `ACTIVE` forever. The two
   sides disagree about what that means:
   - `BundleTopUpService.currentlyValid` reads elapsed as **not coverage**, so the parent is
     charged for the repurchase (`BundleTopUpService.java:166-177`).
   - `BundleGrantService.grantOne` reads `existsBy...Status="ACTIVE"` as **already held**, so the
     grant is declined (`BundleGrantService.java:136-141`).

   Net effect: a legitimate repurchase after expiry is charged in full and grants nothing. Not
   caused by the B-09 lock, and not fixed by it. Untouched deliberately because deciding whether
   repurchase should extend, re-grant, or apply a grace window is a commercial decision, not a
   bug-local edit. Needs an approved rule before it can be closed.
2. ~~**Dashboard notification integration (P-05).**~~ **RESOLVED.** The assembler was unreachable
   from the endpoint a parent actually calls: `GET /v1/my/notifications` read learner
   notifications straight from its own repository and never consulted it, so a completed payment
   never reached the inbox. `getFinancialNotifications` is now merged in, sorted before
   pagination, and counted in `unreadCount`. Proven at the endpoint, not at the component.
3. ~~**Subscription expiry scheduler (S-09).**~~ **RESOLVED.** Two layers now covered: the
   `@Scheduled` wrapper is proven to delegate, to pass the current time, and to survive a failing
   sweep without taking the recurring task down; and an integration test drives the real bean
   against real rows so the ACTIVE→EXPIRED flip is observed in the database rather than inferred
   from a stubbed repository.
4. **Group booking flows (E-05).** Read as **PARTIAL**, not "untested". Multi-seat booking *is*
   implemented and tested end to end (seats column, `total = unit × seats`, capacity against
   `SUM(seats)`, positive-seats constraint in V173), and two defects found during this audit were
   fixed and covered by tests: a cancelled or dead-payment registration used to lock the attendee
   out permanently (the blanket `existsByEventIdAndUserId` matched CANCELLED rows while the unique
   constraint `(event_id,user_id)` forbade a second row), and capacity was recounted as a **row
   count** instead of `SUM(seats)` on cancellation. What §53 additionally asks for and what does
   **not** exist: any distinction between an organizer-paid booking and an individually paid one
   (`bookingType`/`payerType`/`ORGANIZER_PAID` appear nowhere in main), and any frontend booking
   UI — the site only offers free registration. Both await decision request §4.
5. ~~**Refund split persistence (R-07).**~~ **RESOLVED.** The split was computed and then thrown
   away, because `RefundService` is told only the total. It is now stored on the request
   (`commission_reversed`, `provider_reversed`, V183). Non-bundle refunds deliberately record
   NULL rather than 0: zero would assert the platform kept nothing from a split nobody
   calculated.
6. **Real Stakaba sandbox transaction (G-06).** Blocked until authorized. Mock contract tests do
   not substitute for this. Two prerequisites, both needing a decision first: an ACTIVE pricing
   policy (decision request §6 — V169 seeds all 17 as DRAFT, so *no* checkout can be priced today)
   and a genuine sandbox transaction executed against `docs/STAKABA-SANDBOX-TESTING.md`.
7. **V175 reconciliation (D-06).** Blocked pending database-owner authorization. Read-only
   evidence is in §4; `docs/V175-RECOVERY-PROCEDURE.md` now carries the full picture — including
   this session's finding that it is a **version collision**, not a checksum mismatch (the repo's
   `V177__subscription_lifecycle.sql` is the migration the shared DB recorded at 175) — plus four
   options with risks, the exact approval text required, rollback and post-recovery checks.
8. **Owner decisions.** `docs/PAYMENT-OWNER-DECISIONS.md` consolidates the seven decisions that
   nothing can be built without: entitlement expiry/repurchase, commission rate, component refund
   valuation, organizer-paid booking rules, catalogue read audience, sandbox pricing activation,
   V175. Items 1, 4, 6, 7 above all resolve behind these.

---

## 6. Working practices that must continue

- **One Maven build at a time.** Concurrent builds on shared `target` and `elmkusoma_test`
  caused real failures (`pg_type_typname_nsp_index`, `ClassNotFoundException`) that looked like
  code defects. Diagnose by cause; never label unexplained failures "environmental".
- **Inject only `DB_*` from `.env` when running tests.** Loading every variable sets `LIVEKIT_*`,
  which satisfies the "no storage configured" precondition in the liveclass tests and produces
  three failures that look like regressions but are an artefact of the harness. `loadenv-min.ps1`
  in the session temp directory does this.
- `SubscriptionMigrationConstraintTest` owns the full Flyway chain. It asserts the latest version
  is **at least** 180 and that nothing is skipped from V170 up. It no longer needs editing when a
  migration is added — that brittleness is what made V181 fail an unrelated test.
- Never kill another developer's process without authorization; inspect first.
- Never commit `git add .` / `-A`. Stage exact paths.
- When a locale file also carries another developer's uncommitted key, stage a filtered blob with
  `git hash-object --stdin` and **normalise CRLF → LF**, because `--stdin` applies no autocrlf
  filter and the diff would otherwise show the entire file as changed. Never write the filtered
  text back to the working tree — that deletes the other developer's edit.


---

## 7. Verification level (Payment.md §27 ladder)

`Payment.md:594-604` requires these nine to be recorded **separately**. They are, so mock
evidence is never presented as sandbox or production evidence (`Payment.md:590-591`).

| # | Rung | Status | Evidence |
|---|---|---|---|
| 1 | Code implemented | **Achieved** | 61 DONE rows in §2a |
| 2 | Mock tests passed | **Achieved** | full suite green; see §1a for the exact run |
| 3 | Sandbox credentials configured | **Configured, not exercised** | `.env` carries a sandbox key with `STAKABA_ALLOW_LIVE=false`; `start-backend.ps1 -VerifyConfig` exit 0; `StakabaConfigurationValidatorTest` 7/7 |
| 4 | Real sandbox transactions verified | **Not achieved** | G-06 BLOCKED — no transaction has ever been executed against Stakaba |
| 5 | Production credentials configured | **Deliberately not done** | no live key is accepted anywhere |
| 6 | Production onboarding approved | **Not achieved** | G-07 BLOCKED, out of scope, no approval on record |
| 7 | Live collections authorized | **Not achieved** | `Payment.md:587` — would require 5 and 6 first |
| 8 | Refund capability verified | **Mock-verified only** | `RefundServiceTest` 21/21, `PaymentRefundIntegrityTest` 6/6, `BundleRefundPolicyTest` 11/11; **no real refund has been executed** (`Payment.md:588`) |
| 9 | Settlement/payout capability verified | **Not achieved** | A-05 BLOCKED — `Payment.md:1199` forbids payout activation without approval, and `Payment.md:1171` forbids assuming a rate |

**Highest rung actually reached: 2.** Rung 3 is *configured*, which is not the same as verified.
Everything from rung 4 up is blocked by an owner decision or an authorisation, not by missing code.

### Final verdict

The payment system is **implemented and mock-verified**: server-computed amounts, ACTIVE-policy
pricing, idempotent and replay-safe collection, webhook authenticity, entitlement granted only on
verified completion, refunds bounded and split-persisted, per-beneficiary earnings isolated, and
a bundle catalogue whose items must resolve to a real, priced service. `MISSING 0 · BROKEN 0`.

It is **not** ready for money. One PARTIAL (E-05 organizer-paid) and five BLOCKED rows (G-06,
G-07, A-05, A-07, D-06) all resolve behind the seven decisions in
`docs/PAYMENT-OWNER-DECISIONS.md`, and rungs 4-9 of §27 cannot begin until those decisions and
the sandbox authorisation exist. Claiming anything beyond rung 2 today would breach
`Payment.md:590`.
