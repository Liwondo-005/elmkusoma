# ELMKUSOMA Payments & Transactions — Verified Progress Record

**Purpose.** Execution interrupted by context limits. This file is the resume point for
`Payment.md` implementation. It records what is *actually verified in this repository*, with
real test evidence — not what is planned.

**Baseline:** `main` @ `23d69e87` (synced with `origin/main`) — the original full-suite baseline,
kept for history.
**Current:** `main` @ `43da0d8d` (synced with `origin/main`) plus the uncommitted §61
notification and §45 read-oracle work described in §5 items 9–10.
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
  `scheduledAt.toLocalDate() == LocalDate.now()` against a seed of `now().minusHours(2`,
  so it fails in the two hours after midnight and nowhere else. It now compares the
  stored schedule against the seeded one, which is deterministic and actually tests
  preservation.

### Re-verified after the Decision 1 / Decision 5 / IDOR work

Again unsharded on the whole working tree, so no package can be missed:

```
Command : mvn -o test                          (JAVA_HOME=jdk-21.0.12.1, loadenv-min.ps1)
Exit    : 0   BUILD SUCCESS   Total time: 09:42 min
Result  : Tests run: 2456, Failures: 0, Errors: 0, Skipped: 5
Classes : 196
```

**Coverage verified, not assumed:** 196 `*Test.java` files under `src/test` and 196 surefire
reports — none missing, none extra.

**The +39 is fully accounted for**, with no other test having moved:

| Change | Tests |
|---|---|
| `PaymentSpecSecurityCoverageTest` (new) | +17 |
| `PaymentIdorHardeningTest` (new) | +11 |
| `EntitlementExpirySweepIntegrationTest` (new) | +5 |
| `BundleGrantServiceTest` 10 → 13 (Decision 1) | +3 |
| `CoreSchedulerExpirySweepTest` 4 → 7 (entitlement sweep wiring) | +3 |
| **total** | **+39 → 2456** |

The 5 skips are unchanged and all belong to the integration package (`EventD03ComplianceTest` 1,
`LiveKitIntegrationTest` 1, `LiveKitRealServerTest` 3). No `[ERROR]` line appears anywhere in the
run.

Frontend on the same tree: `npx tsc --noEmit` exit 0, `node scripts/check-i18n-keys.mjs` exit 0
(`OK - every statically-referenced key exists in en and sw`, 540 source files / 967 bindings —
one more binding than before, and **no message file was edited to get there**; the retry buttons
reuse `common.retry`), `npm run build` exit 0.

### Re-verified after the §61 / §45 / correlation work

The §61 notification emitter, the §45 read-oracle fixes and the §58/§71 correlation filter each
touched production code, so the whole suite was rerun unsharded on the whole working tree:

```
Command : mvn -o test                          (JAVA_HOME=jdk-21.0.12.1, loadenv-min.ps1)
Exit    : 0   BUILD SUCCESS   Total time: 20:04 min
Result  : Tests run: 2478, Failures: 0, Errors: 0, Skipped: 5
Classes : 198
```

**Coverage verified, not assumed:** 198 `*Test.java` files under `src/test` and 198 surefire
reports — none missing, none extra.

**The +22 over the 2456 baseline is fully accounted for**, with no other test having moved:

| Change | Tests |
|---|---|
| `PaymentEventNotificationServiceTest` (new — §61) | +12 |
| `RequestCorrelationFilterTest` (new — §58/§71 correlation) | +7 |
| `PaymentSpecSecurityCoverageTest` 17 → 19 (§45 unknown-id oracle; correlation wiring) | +2 |
| `PaymentIdorHardeningTest` 11 → 12 (§61: an inbox row grants no access) | +1 |
| **total** | **+22 → 2478** |

The same three test classes needed a mock added because `RefundService` and
`ParentPaymentService` gained a fourth collaborator: `RefundServiceTest` and
`ParentPaymentServiceTest` both `@InjectMocks`, and `@InjectMocks` silently leaves a
constructor argument `null` when no matching `@Mock` exists — which surfaced as an NPE on
`paymentInitiated`/`refundStatusChanged` rather than as a compile error. The mocks are in
`RefundServiceTest` and `ParentPaymentServiceTest` now. The 5 skips are unchanged and all belong
to the integration package. No `[ERROR]` line appears anywhere in the run.

Frontend on the same tree: `npx tsc --noEmit` exit 0, `node scripts/check-i18n-keys.mjs` exit 0
(540 files / 967 bindings), `npm run build` exit 0. `git diff --check` reports no whitespace
errors (only line-ending warnings). Safety greps clean: no `@Disabled`/`@Ignore`/`skip`/`fixme`
in the suites, no `assertTrue(true)`, `STAKABA_ALLOW_LIVE` still `false`, no
`repair`/`baseline`/`dropAll` issued anywhere.


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
| Failure recovery | all three payment surfaces offer an in-place retry: `parent/payments` already had one, `learner/payments` and `platform-admin/payments` now do too (`common.retry`, no message file touched) |

No fabricated totals were found; the page loads through the real API. `authority-sidebar`
has **no** finance/earnings entry. Each payment surface previously rendered a bare error box
whose only recovery was a full page reload, which also threw away the operator's filter and page
position; all three now retry in place.

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

**Delivered end to end:** `app/dashboard/earnings/page.tsx` fetches `GET /v1/earnings/me` and
renders gross, refunded, commission and provider share; it is linked from the learner, provider
and institution sidebars with its own `en`/`sw` message keys. It reports no commission rate and
no net figure rather than inventing either (see A-05).

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

Two were answered; one remains.

- ~~**Whether a bundle may overlap an entitlement the learner already holds.**~~ **Answered —
  Decision 1** (`docs/PAYMENT-OWNER-DECISIONS.md` §1, rules 1–6, proposed default accepted).
  A repurchase that lands while the period is still running stays the unchanged no-op it always
  was; a repurchase after the period ended now opens a fresh period starting at that moment and
  closes the old one out to `EXPIRED`. Implemented in `BundleGrantService.grantOne`, scheduled
  bookkeeping in `CoreScheduler.expireElapsedEntitlements`, proven by `BundleGrantServiceTest` 13/13
  and `EntitlementExpirySweepIntegrationTest` 5/5.
- ~~**Bundle item `serviceType` must be one the platform actually sells**~~ — this was decision
  request §5, and it is now **answered and implemented**: server side by
  `BundleAdminService.requirePricedServices` (already DONE at B-10), catalogue endpoint and
  type-name-only picker added this session, see B-10.
- **Bundle refund allocation.** A bundle costs one price but contains several services.
  A partial refund has no defined split, and inventing one would move money that policy
  has not allocated. Blocked until a rule is approved (decision request §3 = A-07).

---

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
suspends, and lists this institution's bundles.

Editing needed one thing the admin list could not supply. `update` replaces the bundle's whole
item set, so an editor that opened without the items would delete every service in the bundle
simply by saving, and the public `GET /v1/bundles/{id}` cannot help because it refuses drafts on
purpose. So there is now `GET /v1/institution/bundles/{bundleId}`, institution-scoped through the
same `ownedBundle` check as every other admin call, returning the draft and its items in authored
order. The editor loads that before opening and will not open at all if the load fails, rather
than opening an editor it cannot fill in correctly.

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
6. **`BundleGrantService.grantOne` refused a repurchase after expiry — money taken, nothing
   granted.** The coverage check asked the *status column*
   (`existsBy…StatusAndIsDeletedFalse(…, "ACTIVE")`), and nothing ever flips that column when a
   period runs out, so an entitlement that expired months ago still read `ACTIVE` and still
   counted as held. The quote side, which reads `expiresAt`, had already decided the learner held
   nothing and charged the parent for it — then the grant side decided they already held it and
   wrote nothing. Fixed under Decision 1 (owner-approved rules 1–6, `docs/PAYMENT-OWNER-DECISIONS.md`
   §1): both sides now ask the same question of the same timestamp, an elapsed row is closed out to
   `EXPIRED` before the new period is opened (so the old one stays auditable instead of being
   overwritten), the new period starts now and is never backdated, a period that has not elapsed is
   neither re-created nor extended, a row that records no end still blocks, and no payment below
   COMPLETED ever reaches this method.
7. **Receipt ownership compared a user id against a column that also holds a parent row id.**
   `payments.parent_id` receives the payer's `users.id` from the dashboard path but the `Parent`
   row id from `EventBookingService` — so a parent who had just bought an event ticket was refused
   their *own* receipt (403) and, worse, that receipt never appeared in their receipt list at all,
   because the list filtered on the user id alone. `requireOwnsOrAdmin` and
   `PaymentReceiptRepository.findByParentIdIn…` now accept either id space; `mine()` unions the
   two. An empty `parentId` stays admin-only.
8. **Two payer-scoped endpoints trusted whatever id they were handed.** `POST …/notifications/{id}/read`
   used an unscoped `findById`, so any signed-in parent could flip another family's notification;
   `POST /v1/bundles/quote` had no ownership check whatsoever, so any signed-in parent (or learner)
   could price a bundle against *any* learner id and read back that child's entitlements.
   Both now resolve the caller first, allow only their own learner record or a learner they are
   linked to, and answer `404` rather than `403` for a foreign id — the same convention
   `ParentSelfController` already used, so a caller cannot learn whether an id exists. A related
   trap: `ParentAuthorizationService.isAuthorizedChild` throws when the caller has no parent
   profile, which would have made the learner-self branch fail closed for every learner; the
   ownership check uses `isLinkedParentOf`, which returns `false` instead.

   Tests: `PaymentIdorHardeningTest` 11/11 (notification own/foreign idempotent, quote for
   own child / linked-to-another-family / unlinked / own learner record / foreign learner /
   anonymous), `PaymentSpecSecurityCoverageTest` 17/17 (the §63 cases that were enforced but never
   exercised). Removing any of the ownership checks makes them fail.

---

## 3. PARTIAL / MISSING / BROKEN

| Item | Status | Blocker |
|---|---|---|
| Full backend suite green | **DONE — re-verified on current code** | 2478 run / 0 failures / 0 errors / 5 skipped, 198/198 test classes — see §1a |
| Frontend Payments & Transactions nav/dashboards | **DONE** | Verified wired end-to-end — see §1b; §2a rows UI-01–UI-04 record the residual gaps |
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
actually does today, not what a section title implies. "Evidence" is a code symbol or a test
class/method that was actually read.

**Two passes.** The first nine groups below (`F-`, `G-`, `S-`, `B-`, `E-`, `P-`, `R-`, `A-`,
`D-`) were built in the earlier rounds and cover spec §14–§27, §33, §46–§57 and §59–§63. The
completion pass then audited every section the first pass had never reached and added ten more
groups: `UI-` (§28–§31), `RO-` (§32–§42), `AP-`/`AV-` (§43–§45), `RC-` (§56), `AU-` (§58),
`NT-` (§61–§62), `TC-` (§64–§69), `OP-` (§70–§71) and `AC-` (§83–§86). Before this pass the
matrix reached about fifteen of the ninety sections; it now reaches every section that states a
product requirement.

**Status vocabulary.** `DONE` — implemented and evidenced by a cited symbol or test. `PARTIAL` —
the substantial behaviour exists and is evidenced, with a specific named remainder recorded in
the row. `MISSING` — the code for it does not exist. `BLOCKED` — correct implementation is
impossible until an owner decision or an external approval lands. `N/A` — the spec itself scopes
it out. `NEEDS VERIFICATION` — nobody has confirmed either way. `BROKEN` — exists but behaves
wrongly. A row is never `DONE` on the strength of a section title.

**Sections that are process, not product.** §72–§81 are the phase gates this record is itself
executed under; §87 is the format of the final report; §88–§90 are the working rules. They are
evaluated in the report rather than given rows, because a row saying "followed the process" can
only ever be self-certified. §82 is discharged by this table existing at all.

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
| S-09 | Expiry scheduler | `CoreScheduler.expireElapsedSubscriptions` + `CoreScheduler.expireElapsedEntitlements` (Decision 1 rule 6 — bookkeeping only, never the thing that grants or denies) | `CoreSchedulerExpirySweepTest` **7/7** (delegates, passes current time, survives a failing sweep, + 3 entitlement-sweep wiring tests) + `CoreSchedulerExpirySweepIntegrationTest` 4/4 (real rows flip ACTIVE→EXPIRED, live rows untouched, boundary at `period_end`, second run no-op) + `EntitlementExpirySweepIntegrationTest` 5/5 (sweeps only elapsed entitlements, live rows untouched, boundary, repeat run no-op, audit rows written with the system actor); all proven to fail when the delegation is removed | DONE |

### Bundles and entitlements (spec §22, BR-03, §D)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| B-01 | Catalog + detail, published only | `BundleCatalogController` | `BundleCatalogControllerTest` 4/4 | DONE |
| B-02 | Draft/publish/suspend, no delete | `BundleAdminService` | `BundleAdminServiceTest` 13/13 | DONE |
| B-03 | Create/edit UI | `app/dashboard/admin/bundles/page.tsx` | `next build` exit 0 | DONE |
| B-04 | Editing must not silently drop items | `GET /v1/institution/bundles/{id}` | `editingADraftLoadsTheItemsItActuallyContains` | DONE |
| B-05 | Duplicate service prevention (BE + UI) | `requireDistinctItems` | `theSameServiceCannotBeListedTwice` | DONE |
| B-06 | No price column / no price field in UI | `V180` schema + admin UI | migration test | DONE |
| B-07 | Entitlement only after confirmed payment | `BundleGrantService.grantOne` — only ever reached for a COMPLETED payment, and since Decision 1 the coverage predicate is the timestamp (`isActive && isUnexpired`), not the stored status string | `BundleGrantServiceTest` **13/13** incl. `aPaymentThatIsNotCompleteGrantsNothing`, `aRepeatedCallbackGrantsNothingTwice`, `twoConcurrentPurchasesCannotGrantTheSameServiceTwice`, and the three expiry/repurchase cases cited at A-06 | DONE |
| B-08 | Overlap/top-up policy | `BundleTopUpService` | `BundleTopUpServiceTest` 7/7 | DONE |
| B-09 | Concurrent purchase / duplicate grant protection | transaction-scoped advisory lock in `BundleGrantService.grantOne`, taken before the coverage check | `BundleGrantServiceTest` `twoConcurrentPurchasesCannotGrantTheSameServiceTwice` (fails with 2 grants when the lock is removed) | DONE |
| B-10 | Bundle item must point at a real service | `BundleAdminService.requirePricedServices` — every item's `serviceType` must resolve to an ACTIVE pricing policy, checked on create, update **and** publish; that same policy is what `BundleTopUpService` resolves the item's price from | `BundleAdminServiceTest` 13/13 incl. `anItemNamingAServiceThePlatformDoesNotSellIsRefused` (nothing persisted, message names the offending type), `aDraftCannotBePublishedOnceItsServiceIsNoLongerSold`, `anEmptyServiceNameIsRefusedRatherThanTreatedAsAValidType` | DONE — server-side enforcement proven, and the frontend picker from decision request §5 is now implemented: `GET /v1/institution/bundles/catalogue/service-types` returns the distinct `serviceType` names of ACTIVE pricing policies (names only — no price, amount, commission or rate leaves the platform), the admin page renders them as a `<select>` and falls back to a text input when the catalogue is empty so an institution cannot be forced to type blindly; `PaymentSpecSecurityCoverageTest` proves an INSTITUTION_ADMIN may read it while PARENT may not, and an item naming a type the platform does not sell is still refused at the server |

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
| P-06 | Receipts + transaction search | `ReceiptController` with `requireOwnsOrAdmin`, which accepts **either** id space (a dashboard payment stores the parent's `users.id`, an event ticket stores the `Parent` row id — see defect 7 below) | `TransactionSearchServiceTest` 23/23; `PaymentSpecSecurityCoverageTest` receipt cases (owner sees both shapes, stranger 403, ADMIN 200, stranger's list empty) | DONE |

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
| A-06 | Expired-entitlement grace period | no grace period, now **owner-approved** (Decision 1 rule 3, `docs/PAYMENT-OWNER-DECISIONS.md` §1): access ends exactly at `expiresAt` regardless of a stale `ACTIVE`, a repurchase past expiry opens a period starting at the moment of repurchase and never backdated, and the elapsed row is closed out to `EXPIRED` rather than overwritten | `BundleTopUpServiceTest.anExpiredEntitlementIsNotCoverage` (quote side refuses an elapsed grant as coverage, 7/7); `BundleGrantServiceTest` 13/13 incl. `aRepurchaseAfterThePeriodEndedOpensANewPeriodAndClosesTheOldOne`, `aRepurchaseWhileThePeriodStillRunsIsNeitherExtendedNorDuplicated`, `anEntitlementThatRecordsNoEndStillBlocksARepurchase`, `aPaymentThatIsNotCompleteGrantsNothing`; `EntitlementExpirySweepIntegrationTest` 5/5 | DONE — approved and implemented |
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

The cases with a dedicated test in this repository:

| Requirement | Implementation | Evidence | Status |
|---|---|---|---|
| Cross-user transaction read | `SecurityConfig` denies `/v1/payments/**` | `CrossOrgAttackTest.paymentIdManipulation_CrossInstitution_Returns403` | DONE |
| Receipt ownership | `ReceiptController.requireOwnsOrAdmin` → 403 | `PaymentSpecSecurityCoverageTest` (6 cases) | DONE |
| Cross-family child access | `requireChildAccess` → 404 | `ParentAuthorizationServiceTest` | DONE |
| Cross-family subscription access | `cancelOwnSubscription` → 404 | `SubscriptionCancellationPolicyTest` | DONE |
| Provider→other provider earnings | query-level isolation | `BeneficiaryEarningsTest` | DONE |
| Unauthorized refund | `@PreAuthorize` + service re-check | `BundleRefundPolicyTest` | DONE |
| Refund approval authorisation | `hasRole('ADMIN')` on `/v1/platform-admin/refunds/**` | `PaymentSpecSecurityCoverageTest` (3 cases) | DONE |
| Callback replay | idempotent verification | `ParentPaymentServiceTest` | DONE |
| Forged success values | only verified gateway evidence grants | `EventBookingServiceTest` | DONE |
| Platform-admin escalation | `/v1/platform-admin/**` fenced to ADMIN | `PrivilegeEscalationTest.studentCannotAccessPlatformAdminEndpoints` | DONE |
| Pricing governance | `hasRole('ADMIN')` on `/v1/platform-admin/pricing` | `PaymentSpecSecurityCoverageTest` (3 cases) | DONE |
| Reconciliation access | `hasRole('ADMIN')` on `/v1/platform-admin/financial` | `PaymentSpecSecurityCoverageTest` (3 cases) | DONE |
| Notification ownership on write | `findByIdAndUserIdAndIsDeletedFalse` → 404 | `PaymentIdorHardeningTest` (4 cases) | DONE |
| Bundle quote ownership | parent link or own learner record → 404 | `PaymentIdorHardeningTest` (5 cases) | DONE |

> **Correction.** Earlier revisions of this table cited `ResourceHttpSecurityMatrixTest` for the
> `/v1/payments/**` and `/v1/platform-admin/**` rows. That class drives `/v1/resources` and
> `/v1/learner/resources` only, so it never covered either path; the evidence above replaces it
> with the tests that actually do.

Two of the §63 cases — receipt ownership and bundle quote ownership — were found enforced but
unexercised this session, and writing the test surfaced a real defect in each area rather than
confirming them clean: `ReceiptController` compared a caller's *user* id against a column that
also carries the parent *record* id for event tickets (so a parent was refused their own receipt,
and their receipt was missing from the list entirely), and `BundleQuoteController` priced any
learner id an authenticated caller named, disclosing that learner's entitlements. Both are fixed
and pinned above.

The remaining §63 cases are covered through the generic wiring tests
(`ResourceHttpSecurityMatrixTest` for resource/jurisdiction/404-oracle behaviour,
`CrossOrgAttackTest` for tenant isolation, `PrivilegeEscalationTest` for role fences) rather than
one test per row; they are not re-listed here to avoid citing the same evidence twice.

### Unified frontend and UX (spec §28–§31)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| UI-01 | One shared payment module on the existing design system; no per-role duplicate payment application | Shared `frontend/lib/payment-status.ts` (`presentPaymentStatus`, `statusToneClasses`, `isCancellable`), `lib/money.ts`, `lib/csv.ts` shared by every role's page | `frontend/lib/payment-status.ts`; learner/parent/platform-admin/finance pages; no `components/payments` directory exists | PARTIAL |
| UI-02 | Capabilities mapped onto real existing routes with no duplicate page where an equivalent already exists | Learner, parent and platform-admin payments + finance routes exist; transaction-details, payment-attempts, receipts and orders routes do not; sidebar carries both Finance and Payments | no `payments/[id]` route; `platform-admin-sidebar.tsx:53-54` Finance + Payments | PARTIAL |
| UI-03 | Area exposes overview, history, details, orders, attempts, receipts, subscriptions, bundles, refunds, earnings, reconciliation, notifications, reports and admin controls | Present: history, purchases, entitlements, bundles, refund dialog, earnings, reconciliation, CSV export, admin controls. Missing: transaction details, attempts and receipts pages; subscription page is a placeholder | `dashboard/subscription/page.tsx` ("unavailable"); `finance/page.tsx` tabs payments/transactions/entitlements/exceptions/catalogue | PARTIAL |
| UI-04 | Transaction shows reference, order reference, service, payer, beneficiary, amount, currency, method, status and timestamps | `FinancialTransaction` carries providerReference, commission fields, paidAt, receiptNumber; `Payment` has no paymentMethod or orderReference column | `lib/platform-admin-api.ts` `FinancialTransaction`; `parent/domain/Payment.java`; `V178`/`V179__payment_*_attribution.sql` | PARTIAL |
| UI-05 | Transaction exposes a **status timeline**, not only the current status | Status pill only; no `status_history` or `payment_attempts` table in any migration and no timeline UI | grep `status_history\|payment_attempts` over `db/migration` → none; status pill in `learner/payments/page.tsx` | MISSING |
| UI-06 | Gateway reference is **masked** where displayed | Full `providerReference` rendered in the parent receipt table; no masking helper anywhere | `parent/payments/page.tsx:1034-1038`; `payment-surfaces.spec.ts` asserts `PSP-REF-9911` visible | MISSING |
| UI-07 | Receipt, subscription/entitlement, event booking and refund history reachable from the transaction | `receiptNumber` shown inline; backend receipt endpoints exist; no frontend receipt fetch, download or print surface | `ReceiptController.java:48,64,76`; `parent-api.ts:319` (field only); `learner/payments/page.tsx:200` | PARTIAL |
| UI-08 | Available actions and their eligibility shown | `isCancellable` limits cancel to PENDING/PROCESSING; refund only on COMPLETED; ineligible actions are hidden rather than shown with a reason | `lib/payment-status.ts` `isCancellable`; e2e "a failed cancel leaves the row on screen" | PARTIAL |
| UI-09 | Payer, student, parent, attendee, provider, organizer and beneficiary never conflated | Payer/recipient split plus per-child grants in the parent view; purchaser/beneficiary attribution migrated | e2e "shows the payer/recipient split and per-child access grants"; `V178`/`V179` | PARTIAL |
| UI-10 | Loading state | `role="status"` aria-live loader (parent), shared `LoadingState` (learner) | `parent/payments/page.tsx` loader; `components/learner/shared/LoadingState` | DONE |
| UI-11 | Empty state | Honest empty states, and an explicit explanation when a filter excludes everything | e2e "shows an honest empty state, not a zero-filled dashboard"; parent `noMatchTitle`/`noMatchBody`; `payment-surfaces.spec.ts:360` | DONE |
| UI-12 | Error state | `role="alert"` errors; a secondary endpoint failure does not blank the primary view | e2e "surfaces a load failure with a retry that refetches"; "an entitlement endpoint failure does not blank the payment history" | DONE |
| UI-13 | Retry state | Retry buttons refetch on the payment surfaces | `payment-surfaces.spec.ts:194`; `learner/payments/page.tsx:103`, `parent/payments/page.tsx:278`, `earnings/page.tsx:96` | DONE |
| UI-14 | Pending / verified / failed / cancelled / expired / refund states rendered distinctly and never as paid | `PRESENTATION` map (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED — only COMPLETED settled); text + icon, unknown status rendered verbatim | `lib/payment-status.ts`; e2e "renders FAILED and REFUNDED distinctly", "shows an unrecognised status verbatim" | DONE |
| UI-15 | Disabled actions carry an explanation | Refund dialog disables with `aria-describedby` help text; cancel confirmation explains cancel ≠ refund; plain rows simply hide ineligible actions with no reason | `partial-refund-dialog.tsx`; `subscriptions-section.tsx` `nothingToCancel` | PARTIAL |
| UI-16 | Pagination and filtering states | Finance pager; search-scope disclosure; backend counts used rather than page sums | `finance/page.tsx` pager + "rows on this page only"; e2e "reports the backend's own record counts, not a page sum" | DONE |
| UI-17 | No status by colour alone, no fabricated metrics, no success shown for a failure | No page-local totals; CSV export carries no total row; commission and earnings reported "not calculable, never zero" | e2e:405,635 (no total/sum row); e2e:531; earnings page omits profit rather than showing 0 | DONE |
| UI-18 | Accessible labels, keyboard navigation and clear status text | `role="tablist"`/`role="tab"`/`aria-selected`, `htmlFor` labels, `sr-only` captions, `aria-live` regions | `finance/page.tsx` tabs and labels; `frontend/e2e/accessibility.spec.ts` | DONE |
| UI-19 | Small mobile screens supported; responsive tables or equivalent mobile layouts | `overflow-x-auto` wrappers on learner, platform-admin and finance tables; parent uses a responsive card grid instead of a table | `overflow-x-auto` ×1/×1/×4; `parent/payments/page.tsx:294` `sm:grid-cols-2`; `responsive.spec.ts` | DONE |
| UI-20 | Accidental duplicate submission prevented | `initiatePayment` takes an idempotency key, enforced by a partial unique index and gateway replay; **mobile-money send button has no busy guard** | `parent-api.ts:628`; `V170__financial_integrity_*.sql:36-38`; `PaymentGatewayServiceTest.initiate_isIdempotentWhenReferenceAlreadyStored` | PARTIAL |
| UI-21 | Existing localization conventions followed | Payment pages use `useTranslations` against `en`/`sw` messages; a key checker runs in verification | `frontend/scripts/check-i18n-keys.mjs`; `frontend/messages/en.json`, `sw.json` | DONE |

### Role capabilities (spec §32–§42)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| RO-01 | Student sees own transactions, orders, attempts, receipts, subscriptions, entitlements, paid content, bookings, retries, eligible refund requests and notifications | `MyPurchasesController` (`requireOwnStudent`) + learner payments page + `/v1/my/purchases/access`; no attempts UI, no receipts page, no student refund-request flow | `MyPurchasesController.java`; e2e `returnsOnlyTheCallersOwnPayments` | PARTIAL |
| RO-02 | A student cannot reach another student's financial records by manipulating an identifier | Own-purchases controller takes no caller-chosen identifier; IDOR tests across learner and parent paths | `MyPurchasesControllerTest.endpoint_takesNoIdentifierThatCouldBeSwappedForSomebodyElses`; `PaymentIdorHardeningTest` | DONE |
| RO-03 | Student can retry an eligible payment attempt | Parent checkout offers failed → try again (new attempt); the learner page's retry button re-fetches the list only | `parent/payments/page.tsx:724-733` (`retryHint`/`tryAgain`); `learner/payments/page.tsx:103` | PARTIAL |
| RO-04 | Parent access resolved from authoritative parent–child relationships, never from numeric ID equality | `ParentAuthorizationService.requireChildAccess` + link-integrity tests + child switch reloads server-side | `ParentAuthorizationServiceTest.requireChildAccess_throwsForbidden_whenNotLinked`; `ParentStudentLinkIntegrityTest`; e2e "switching child reloads that child's data" | DONE |
| RO-05 | Teacher/trainer sees sales, gross revenue, refunds, fees, net earnings, pending/held/available earnings, settled amounts where verified, service-level reports — without unnecessary payer personal data | `GET /v1/earnings/me` + `MyEarningsPage` (gross, refunded, commission, providerShare); no service-level reports; no payouts | `BeneficiaryEarningsController.java` `@PreAuthorize` incl. TEACHER; `dashboard/earnings/page.tsx` | PARTIAL |
| RO-06 | Organizer sees event transactions, ticket sales, bookings, seat capacity/reservations/utilisation, payment status, refunds, gross revenue, deductions, net earnings, reports | Seats and capacity come from authoritative booking data; no ticket-revenue or net-earnings view for organizers | `EventServiceImpl` summary `maxCapacity`/`currentRegistrations`; `EventController` `GET /{id}/registrations`; event summary page has no revenue | PARTIAL |
| RO-07 | Provider sees provider-owned service sales, orders, transactions, refunds, commissions, net earnings, bundle performance, refund cases, reports and staff activity | No provider-scoped orders/transactions controller or UI; provider earnings reachable only through the shared `/v1/earnings/me`; provider nav has no Payments entry | no `earning`/`transaction`/`order` match in provider `*Controller.java`; `dashboard-sidebar.tsx` `providerNavSections` | MISSING |
| RO-08 | Provider Admin restricted to the assigned Provider and explicitly delegated permissions; no cross-provider, global control, gateway secret or platform-wide financial access | Provider-scope security tests + `provider_staff_scope` migration | `ProviderIsolationSecurityTest.providerAdmin_readsForeignEvent_404`, `..._pagedEventList_scopedToOwnEvents`; `V165__provider_staff_scope.sql` | DONE |
| RO-09 | Institution/College Admin gets institution-scoped transactions, student/cohort summaries, receipts, pending payments, refunds, and earnings only as an approved beneficiary | Backend `GET /v1/institution/earnings/summary` exists; no institution payments/transactions page and no institution-scoped transaction list endpoint | `InstitutionEarningsController.java:34-43`; no frontend consumer of `/v1/institution/earnings` | PARTIAL |
| RO-10 | NGO/CBO, religious and corporate owners reuse the existing Provider/service-owner model; no redundant financial roles or modules | No NGO/CBO value in the role enum; provider/service-owner model reused | `User.java:87` `Role` enum (13 values); `README-01` §3.1 role table | DONE |
| RO-11 | Ordinary registered users get authorized access to own purchases, transaction history, receipts, subscriptions and entitlements | `/dashboard/learner/payments` exists; the learner sidebar section carries no Payments entry | `dashboard-sidebar.tsx:320-372` `learnerNavSections` has no payments item; `:295` university section does | PARTIAL |
| RO-12 | Platform Admin sees transactions, attempts, gateway health, callback exceptions, pricing policies, subscriptions, entitlements, refunds, earnings, allocations, reconciliation, receipts, reports, audit logs and support ops | ADMIN-only `FinancialReportingController` + finance/payments/health pages; missing: payment-attempts UI, refund-request review queue, receipts page, financial summary consumer | `FinancialReportingController.java` `@PreAuthorize hasRole('ADMIN')`; `platform-admin-api.requestPartialRefund` (:1007, POST only); health page `HealthRow` payments | PARTIAL |
| RO-13 | No permanent Super Admin above Platform Admin, and no separate Finance Staff or Payment Support Staff roles | `User.Role` contains none of SUPER_ADMIN / FINANCE_ADMIN / PAYMENT_SUPPORT; delegation authorities exclude finance | `User.java:87`; `PlatformAdminService.DELEGATION_AUTHORITIES` (no finance authority) | DONE |
| RO-14 | Every actual role's navigation, accessible records, permitted actions and restrictions documented | Role→workspace→access-boundary table (13 roles) + per-role route inventory + payment map for parent/learner/platform-admin; learner row unverified; teacher/provider/organizer payment actions undocumented | `README-01` §3.1 (L86-99); `README-02` route tables (L400-448); §1b above | PARTIAL |
| RO-15 | Multi-role users combine only the permissions legitimately granted by those roles and their scopes | `getCombinedPermissions` unions membership permissions, applied to effective memberships in org context | `PermissionService.java:83-89`; `OrganizationContextResolver.java:153` | DONE |

### API inventory and required capabilities (spec §43–§44)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| AP-01 | Endpoint inventory recording route, method, controller, DTOs, service, authn/authz, ownership, validation, transaction boundary, errors, idempotency, audit, tests and frontend consumers | `docs/PAYMENT-API-CONTRACT-DEVELOPER-01.md` | 344-line doc but stale paths (`v1/parents/payments/initiate` vs real `/v1/my/payments/initiate`, `ParentSelfController:308`) and no authz/DTO/transaction/audit/frontend columns; `api-reference.md` lists module prefixes only | PARTIAL |
| AP-02 | Listing authorized transactions | `ParentSelfController`, `FinancialReportingController` | `GET /v1/my/payments` (:259); `GET /v1/platform-admin/financial/transactions` (:69, class `@PreAuthorize ADMIN`) | DONE |
| AP-03 | Retrieving a single transaction | `ParentSelfController`, `ReceiptController` | No `GET`-by-id transaction; single read only via `POST /v1/my/payments/{id}/status` (:369, gateway re-query) or `GET .../receipt` (:48) | PARTIAL |
| AP-04 | Listing orders | `ParentSelfController`, `PlatformAdminController` | No `Order` entity — the `Payment` row is the order record: `GET /v1/my/payments` (:259), `GET /v1/platform-admin/payments` (:313) | DONE |
| AP-05 | Creating an eligible purchase | `ParentSelfController` | `POST /v1/my/payments/initiate` (:308) → `ParentPaymentService.initiatePayment`; child access enforced (:145); events via `EventBookingService.book` (:123) | DONE |
| AP-06 | Initiating a payment attempt | `ParentSelfController` | `POST /v1/my/payments/{paymentId}/mobile-money` (:347) → `PaymentGatewayService.initiateCollectionForOwnedPayment` (:86) | DONE |
| AP-07 | Checking payment status | `ParentSelfController` | `POST /v1/my/payments/{paymentId}/status` (:369) → `PaymentGatewayService.verifyPaymentWithGateway` (:187); the server re-queries the gateway | DONE |
| AP-08 | Retrying an eligible attempt | `MobileMoneyInitiateRequest`, `PaymentGatewayService` | Route documented "initiating or retrying"; only PENDING retriable; an existing providerReference is replayed (:111-120); `PaymentGatewayServiceTest.initiate_isIdempotentWhenReferenceAlreadyStored` | DONE |
| AP-09 | Viewing receipts | `ReceiptController` | `GET /v1/parents/payments/{id}/receipt` (:48), `GET /v1/parents/receipts/{number}` (:64), `GET /v1/parents/receipts` (:76), each `requireOwnsOrAdmin` | DONE |
| AP-10 | Viewing subscriptions | `ParentSelfController` | `GET /v1/my/subscriptions` (:281-287) → `SubscriptionService.listForPayer(userId)` | DONE |
| AP-11 | Viewing entitlements | `ParentSelfController`, `MyPurchasesController` | `GET /v1/my/children/{studentId}/entitlements` (:518, `requireChildAccess` → 404); `GET /v1/my/purchases/access` (:103); admin `GET /v1/platform-admin/entitlements` | DONE |
| AP-12 | Requesting a refund | `RefundController` | `POST /v1/parents/payments/{paymentId}/refund-requests` (:47) → `RefundService.requestRefund` (:80), amount ≤ refundable | DONE |
| AP-13 | Reviewing refunds with proper permissions | `RefundController` | `POST /v1/platform-admin/refunds/{id}/approve\|reject\|complete` (:106,:114,:127) `@PreAuthorize hasRole('ADMIN')`; `PaymentSpecSecurityCoverageTest.aPlatformAdminPassesTheApprovalGate` | DONE |
| AP-14 | Viewing authorized earnings | `BeneficiaryEarningsController`, `InstitutionEarningsController` | `GET /v1/earnings/me` (:45) → `EarningsService.summaryForBeneficiary(caller)`; `GET /v1/institution/earnings/summary` (:43) | DONE |
| AP-15 | Viewing reconciliation exceptions | `FinancialReportingController` | `GET /v1/platform-admin/financial/reconciliation/exceptions` (:161) → `ReconciliationService.openExceptions`; `openExceptions_listsOnlyOpenOnes` | DONE |
| AP-16 | Updating approved pricing policy | `PricingGovernanceController` | Lifecycle only — activate (:71) / retire (:78) via `PricingPolicyAdminService:109,124`; **no endpoint edits policy values** | PARTIAL |
| AP-17 | Producing authorized reports and exports | `FinancialReportingController`, `PlatformAdminController` | `GET .../financial/transactions/export` (:93, `exportCsv:209`, `EXPORT_LIMIT 10_000`); `GET /v1/platform-admin/export` (:826); both class-level `@PreAuthorize ADMIN` | DONE |

### API validation (spec §45)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| AV-01 | Prices resolved server-side; caller-supplied amount never trusted | `ParentPaymentService.initiatePayment`, `PricingService` | `initiatePayment` uses `PricingService.ResolvedPrice`; `requireAmountMatches:164`; `initiatePayment_usesTheAuthoritativePolicyPrice_notTheCallerAmount` | DONE |
| AV-02 | Amount and currency validated | `InitiatePaymentRequest`, `PaymentGatewayService` | `@DecimalMin 0.01` / `@DecimalMax 10000000`; currency from policy only; non-TZS refused (:122); `initiate_rejectsNonTzsPayment` | DONE |
| AV-03 | Order/payment ownership validated server-side | `PaymentGatewayService`, `ParentSelfController` | `requireOwnership:86-90`; `requireChildAccess:145` → 404; `verify_crossParentAccess_isRefusedBeforeAnyGatewayCall` | DONE |
| AV-04 | Beneficiary eligibility validated | `EventBookingService` | beneficiary set server-side only (:189-190 `organizerId`); **no approved-beneficiary eligibility check exists** on earnings/beneficiary paths | PARTIAL |
| AV-05 | Event capacity validated | `EventBookingService` | `confirmSeat:236-244` sums held seats against `maxParticipants` under a locked read; `aGroupBookingLargerThanCapacityIsRefusedAtConfirmation` | DONE |
| AV-06 | Subscription eligibility validated | `SubscriptionService` | `activateFromVerifiedPayment:54-65` requires COMPLETED; `refusesToOpenASubscriptionFromAnUnverifiedPayment` | DONE |
| AV-07 | Idempotency keys validated where supported | `InitiatePaymentRequest`, `ParentPaymentService` | `@Size(max=128)` key; replay via `findByIdempotencyKeyAndIsDeletedFalse`; unique index `V170__financial_integrity...:36-38`; `initiatePayment_replaysOnTheSameIdempotencyKey...` | DONE |
| AV-08 | Unexpected state transitions rejected | `RefundService`, `PaymentGatewayService` | `transition`/`OPEN_STATES` guards (:59-96, :136); `lifecycle_cannotSkipReviewStraightToProcessing`; `initiateCollection` refuses non-PENDING (:108-110) | DONE |
| AV-09 | Permissions enforced for exports and downloads | `FinancialReportingController`, `PlatformAdminController` | class-level `@PreAuthorize hasRole('ADMIN')` covers both export routes (:93, :826); no anonymous download route | DONE |
| AV-10 | Mass assignment of financial state prevented | `InitiatePaymentRequest`, `RefundController` | request DTOs carry no `status`/`refundedAmount`; `Payment` built server-side (`ParentPaymentService:162-183`); `approve` accepts only note + revokeEntitlement; webhook payload compared, never bound | DONE |
| AV-11 | Never accepts arbitrary `success=true` / `paid=true` as proof of payment | `PaymentWebhookController`, `StakabaWebhookController` | `X-Webhook-Secret` + amount/currency reconciliation (:48-105); Stakaba endpoint unauthenticated (:63-85) **but** re-verifies via gateway `GET /api/v1/transactions/{ref}`; `webhook_forgedSuccess_apiSaysFailed_neverFulfils` | PARTIAL |
| AV-12 | Consistent error responses across payment endpoints | `GlobalExceptionHandler`, `ApiResponse` | single `ApiResponse.error` shape for all payment endpoints; `SecurityExceptionHandlingTest` | DONE |
| AV-13 | Private record existence not leaked — foreign id and unknown id indistinguishable on read paths | `ReceiptController`, `RefundController` (**fixed this round**), `NotificationService` | `anUnknownPaymentIdAndAForeignOneAreIndistinguishable` (receipts, both 404/`Payment not found`); refund-request read now 404 (`RefundController.get`); learner `markAsRead` and event read paths still answer 403-vs-success | PARTIAL |

### Receipts and invoices (spec §56)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| RC-01 | Unique receipt reference | `ReceiptService` | `receiptNumber` derived from the payment id (:131); `@Column(unique=true)`; unique index `V170:167`; `receiptsDifferPerPayment` | DONE |
| RC-02 | Correct order/transaction association, payer, service, amount, currency, status and dates | `ReceiptService` | `issueFor:54-93` copies `paymentId`/`parentId`/`studentId`/`serviceType`/`amount`/`currency`/`paidAt` from the `Payment` row; refuses non-COMPLETED | DONE |
| RC-03 | Authorized view, **download and print** of a receipt | `ReceiptController` | three authorized JSON `GET`s only — **no download, PDF or print endpoint**; `frontend/lib/parent-api.ts` has no consumer for `/v1/parents/receipts` | PARTIAL |
| RC-04 | Receipt verification where supported | `ReceiptController` | `GET /v1/parents/receipts/{receiptNumber}` (:64), still `requireOwnsOrAdmin`; no public verification route (receipts are private by design) | DONE |
| RC-05 | Refund references recorded against the original receipt | `RefundService`, `ReceiptService` | `completeRefund:282` → `receiptService.recordCreditNote(...)`; `creditNote_keepsTheOriginalReceiptIntact` | DONE |
| RC-06 | Reissue/correction history preserved | `ReceiptService`, `ReconciliationService` | credit note preserves the original; `raiseReceiptFailure:221-252` records `resolvedBy`/`resolvedAt`/`resolutionNote` for human reissue; **no reissue/version endpoint or table** | PARTIAL |
| RC-07 | A final paid receipt is never issued for an unverified transaction | `ReceiptService` | `issueFor` refuses any payment that is not COMPLETED; `issueFor_refusesToDocumentMoneyThatWasNeverReceived` | DONE |
| RC-08 | Receipts never exposed through unrestricted or guessable download URLs | `ReceiptController` | every route `requireOwnsOrAdmin`; a guessed number yields 404 (this round made foreign and unknown identical); no signed/token receipt route exists | DONE |

### Financial audit (spec §58)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| AU-01 | Audit events record actor, action, target and timestamp | `AuditLog` | `user_id`/`user_email`/`user_role`, `action` enum, `entity_type`/`entity_id`/`entity_name`, `created_at`; written by `PaymentGatewayService.audit:456-469` | DONE |
| AU-02 | Audit events record a **result** | `AuditLog`, `PlatformIntegrationService` | `audit_logs` has no result column — the outcome lives in `oldValues`/`newValues` status (`ParentPaymentService:257-264`) or in `webhook_events.processing_result` via `recordWebhook:216-235`; the `responseStatus` column is never set | PARTIAL |
| AU-03 | Audit events record **correlation identifiers** | `RequestCorrelationFilter` carries the id for every request; **`audit_logs` still has no correlation column** | the log half is done — `X-Correlation-Id` validated and put in the MDC for the whole chain, echoed to the caller, printed by `logging.pattern.level`, wiring asserted by `everyRequestCarriesACorrelationIdThatIsWiredAheadOfTheSecurityChain`; the row half is not: `audit_logs` has no correlation column (a nullable `V184` would close it), and `PlatformAdminService:828-829` records request method/url only | PARTIAL |
| AU-04 | Logs protected from tampering and free of unnecessary personal information | `V18__create_audit_tables.sql`, `PlatformIntegrationService` | `audit_logs` is soft-delete like any table — no trigger, no append-only enforcement, no `REVOKE` on UPDATE/DELETE in any migration; the only redaction is `PlatformIntegrationService.redact:343`; no logback masking and no secret-logging test | PARTIAL |
| AU-05 | History traceable; explicit adjustments instead of silent edits to historical transactions | `RefundService`, `ReconciliationService` | `refunded_amount` accumulated under a row lock (:277); `V171` `chk_refunded_not_overpaid`; `ReconciliationService.resolve` sets resolution fields only; no `.setAmount(` on a settled row in `src/main` | DONE |

### Notifications (spec §61–§62)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| NT-01 | Existing notification infrastructure reused; no parallel system invented | `NotificationService` + `LearnerNotification`; `PaymentEventNotificationService` delegates to it | `PaymentEventNotificationServiceTest.aPaymentInitiatedHeadlineGoesToThePayerAndNobodyElse` verifies the `notifyUser` call; the assembler persists nothing | DONE |
| NT-02 | **Payment initiated** event supported | `ParentPaymentService.initiatePayment` → `PaymentEventNotificationService.paymentInitiated` | `PaymentEventNotificationServiceTest.aPaymentInitiatedHeadlineGoesToThePayerAndNobodyElse` | DONE |
| NT-03 | **Payment verified** event supported | `ParentFinancialNotificationAssembler` projects COMPLETED payments into `GET /v1/my/notifications` | `ParentFinancialNotificationAssemblerTest.aCompletedPaymentProjectsPaymentAndReceiptItems`; `ParentFinancialNotificationEndpointTest.aCompletedPaymentForAChildReachesThatParentsInbox` | DONE |
| NT-04 | **Payment failed** event supported | `ParentFinancialNotificationAssembler.addPaymentItem` renders non-completed statuses HIGH; `PaymentWebhookController:116` sets FAILED | `ParentFinancialNotificationAssemblerTest.aFailedPaymentIsHighPriority` | DONE |
| NT-05 | **Payment cancelled or expired** event supported | generic status render `ParentFinancialNotificationAssembler:103-119`; `CANCELLED` set at `ParentPaymentService:544` | CANCELLED covered by the code path only — **no test asserts a cancelled/expired payment alert**, and no EXPIRED payment state exists | PARTIAL |
| NT-06 | **Receipt available** event supported | `ParentFinancialNotificationAssembler.addReceiptItem` ("Receipt Available", stable id `receipt-<paymentId>`) | `ParentFinancialNotificationAssemblerTest.aCompletedPaymentProjectsPaymentAndReceiptItems`; endpoint test asserts the `receipt-` id | DONE |
| NT-07 | **Subscription activated or expiring** event supported | `PaymentEventNotificationService.subscriptionActivated`/`subscriptionExpired`, called from `SubscriptionService:103,293` | `PaymentEventNotificationServiceTest.anOpeningPeriodIsReportedAsItsOwnEvent`, `aClosingPeriodIsReportedToo`; **no advance "expiring soon" warning exists** | PARTIAL |
| NT-08 | **Booking confirmed** event supported | "Registration confirmed" at `EventServiceImpl:722,746`; the paid-seat confirm path (`EventBookingService.confirmSeat`, reached from `ParentPaymentService:409`) emits none | `EventLifecycleE2ETest.registration_ConfirmationNotificationCreated`; no notification call in `EventBookingService` | PARTIAL |
| NT-09 | **Refund status changed** event supported | `RefundService:119,164,192,219,288,310,372` → `PaymentEventNotificationService.refundStatusChanged` | `PaymentEventNotificationServiceTest.eachIntermediateRefundStateIsReportedWithItsOwnHeadline`, `aCompletedPartialRefundIsReportedHereBecauseTheProjectionStaysSilent` | DONE |
| NT-10 | Duplicate notifications made safe | `PaymentEventNotificationService.notifyOnce` exists-check on (user, target, type, headline); assembler derives on read with ids stable per payment | `theSameHeadlineIsNeverToldTwice`, `aReplayedWebhookStillReachesTheGuardRatherThanFiringTwice`, `theSamePaymentAlwaysProjectsTheSameItemId` | DONE |
| NT-11 | Existing preference model respected | `NotificationService.isNotificationAllowed` (feeAlerts extended to REFUND and SUBSCRIPTION this round); assembler `financialAlertsEnabled` | `turningFeeAlertsOffRemovesEveryFinancialItem`, `aTurnedOffFeePreferenceSuppressesEveryFinancialItem`, `anAbsentPreferenceStillShowsMoneyRatherThanHidingIt` | DONE |
| NT-12 | Notifications never independently grant access | `PaymentEventNotificationService` and `NotificationService.notifyUser` write only a `LearnerNotification` row (`NotificationService:45-53`) | `PaymentIdorHardeningTest.aMoneyNotificationOnItsOwnGrantsNoAccess` drives the real emitter against the real database and then proves the family's access did not move: the inbox row exists, the held entitlement keeps its status and expiry, no entitlement was created for the unpaid service, and the quote still reports the same coverage | DONE |
| NT-13 | `ParentFinancialNotificationAssembler` actually integrated into `ParentDashboardServiceImpl` and surfaced by the API | `ParentDashboardServiceImpl:616` (`getChildNotifications`), `:835` (`getFinancialNotifications`), merged at `ParentSelfController:920` `GET /v1/my/notifications` | `ParentFinancialNotificationEndpointTest` (`@SpringBootTest` + MockMvc); **`getAllChildrenNotifications` (`:749-820`) never calls the assembler** | PARTIAL |
| NT-14 | Verified links, feeAlerts, preserved assignments/attendance/live classes/announcements, stable deduplicated ids, no financial writes on reads, integration test for the actual API response | assembler + `ParentDashboardServiceImpl.getChildNotifications`; ids derived from the payment id | `ParentFinancialNotificationEndpointTest` (links, feeAlerts, cross-family); `theSamePaymentAlwaysProjectsTheSameItemId`; `itemsAreNeverMarkedReadByTheProjection`; no test asserts assignment/attendance/live-class/announcement preservation | PARTIAL |

### Test coverage (spec §64–§69)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| TC-01 | Baseline commands exist and are documented: backend build/unit/integration, Flyway validation, frontend type check, lint where configured, frontend build, e2e, security, migration | `frontend/package.json` (`build`, `test:e2e`, `check:i18n`); `FlywayMigrationValidationTest`; security and migration suites present | §1a records `mvn -o test`, `npx tsc --noEmit`, `npm run build` and the i18n check with exit codes; **no eslint config exists → lint is N/A** | DONE |
| TC-02 | Lifecycle tests: verified success, pending, failure, cancellation, timeout | `PaymentGatewayService`, `ParentPaymentService`, `PaymentWebhookController` | `initiate_storesReference_andStaysPending`, `initiate_timeoutFlagsAmbiguous_andRethrowsWithoutCompleting`, `verify_gatewaySaysFailed_marksPaymentFailed`, `verifyPayment_marksCompleteAndGrantsEntitlement`, `parent_cancelOwnPayment_200_cancelled` | DONE |
| TC-03 | Lifecycle tests: duplicate callbacks, replayed callbacks, unknown references | `PaymentWebhookController` duplicate/replay guards; `PaymentGatewayService.handleStakabaWebhook` | `samePaymentRetry_isIdempotentSuccess`, `providerReferenceReplayAcrossPayments_is409`, `webhook_unknownReference_isAcknowledgedAndIgnored`, `unknownReference_isAcknowledgedWithoutFulfilment` | DONE |
| TC-04 | Lifecycle tests: amount mismatch, currency mismatch, **incorrect merchant** | callback amount/currency guards (`PaymentWebhookController:86-96`); provider guard `PaymentGatewayService:338` | `amountMismatch_is422AndPendingPreserved`, `currencyMismatch_is422WithNoCompletion`, `verify_amountMismatch_refusesFulfilment`; **no test for incorrect merchant/provider** | PARTIAL |
| TC-05 | Lifecycle tests: **out-of-order callbacks** | guard exists — FAILED after COMPLETED is ignored (`PaymentWebhookController:115`) | only duplicate-after-completion is tested (`webhook_duplicateDelivery_afterCompletion_isHarmless`); **no payment test exercises late/out-of-order ordering** | PARTIAL |
| TC-06 | Lifecycle tests: retry and duplicate fulfillment | idempotent verify/fulfil pipeline (`ParentPaymentService.verifyPayment`) | `webhook_verificationInfrastructureFailure_propagatesForRetry`, `verificationInfrastructureFailure_is500SoProviderRetries`, `webhook_duplicateDelivery_afterCompletion_isHarmless`, `verify_alreadyCompleted_isIdempotent_noSecondGatewayCall` | DONE |
| TC-07 | Lifecycle tests: receipt consistency and notification failure | `ReceiptService`, `ReconciliationService.raiseReceiptFailure`, notification catch-and-log | `issueFor_issuesAReceiptForACompletedPayment`, `verifyPayment_stillCompletesWhenReceiptIssuanceFails`, `raiseReceiptFailure_recordsItButLeavesThePaymentCompleted`, `anInboxFailureNeverBecomesAFailureInTheCaller` | DONE |
| TC-08 | Lifecycle tests: reconciliation, concurrent attempts, partial-processing recovery | `ReconciliationService`; concurrency locks in booking/refund/grant | `ReconciliationServiceTest` (11 methods), `concurrentConfirmations_neverOversellTheLastSeat`, `concurrentLegacyRefunds_exactlyOneSucceedsAndTheAmountIsNeverDoubled`, `aRejectedGrantLeavesNoEntitlementAndDoesNotLeaveALockHeld` | DONE |
| TC-09 | Subscription tests: first purchase, renewal before/after expiry, boundaries, persisted duration, concurrency, duplicate callback, expiry scheduler, idempotency, recovery, DB constraints, entitlement correctness | `SubscriptionService`, `CoreScheduler` sweeps, `SubscriptionMigrationConstraintTest` | `activatesFromAVerifiedPayment_usingTheApprovedAccessDuration`, `renewingWhileAPeriodIsLive_chainsTheNewPeriodAfterIt`, `renewalAfterALapseStartsNowRatherThanBackfillingTheGap`, `aPeriodThatEndsNowIsExpiredButOneSecondAwayIsNot`, `concurrentRenewalsAreSerialisedByTheDatabase`, `hasEntitlement_returnsFalse_afterExpiry_evenThoughStatusIsStillActive` | DONE |
| TC-10 | Event tests: organizer-paid group booking, individual payment, identity, verified seat, invalid payment rejection, duplicate callback, capacity concurrency, cancellation/refund effects, scope | `EventBookingService`, `EventSeatCapacityConcurrencyTest`; organizer-paid flow not implemented (E-05) | `aSeatIsConfirmedOnlyAfterThePaymentIsCompleted`, `aGroupBookingConsumesEverySeatItBuys`, `aSeatIsNotConfirmedWhenThePaymentFailedOrIsStillPending`, `concurrentConfirmations_neverOversellTheLastSeat`, `registrations_CrossUser_ManipulatedUserId_Returns403` | PARTIAL |
| TC-11 | Frontend tests: role navigation, permission-dependent actions, listing, filtering/pagination, details, receipts, subscription access, refund states | `frontend/e2e/payment-surfaces.spec.ts`, `p0-workspace.spec.ts`, `provider-workspace.spec.ts`, `oversight.spec.ts` | listing/filters/pagination and FAILED/REFUNDED rendering covered; role guards covered; **no e2e for transaction details, receipts or subscription-access gating** (no such surfaces exist to test) | PARTIAL |
| TC-12 | Frontend tests: loading/empty, errors and retries, mobile, accessibility, no fabricated values, no false success, no unauthorized exposure | `payment-surfaces.spec.ts`, `responsive.spec.ts`, `accessibility.spec.ts` | "shows an honest empty state", "surfaces a load failure with a retry that refetches", "never calls pending paid", "no invented total", "switching child … does not leak the other" | DONE |
| TC-13 | Regression rules: no test disabled for a green build, no mocked test passed off as a gateway test, no competing builds, per-command results recorded | §1a, §6, §7 above | grep: 0 `@Disabled`/`@Ignore` in backend, 0 `skip`/`fixme`/`.only` in e2e; §7 separates mock from sandbox rungs (G-06 BLOCKED); §6 mandates one Maven build at a time | DONE |

### Reconciliation and operational visibility (spec §70–§71)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| OP-01 | Compare authoritative gateway records with local transactions where the gateway supports it | `PaymentGatewayService.verify` + `StakabaGatewayClientImpl.getTransaction` per payment; **no batch gateway comparison** | `getTransaction_parsesStatusAmountAndCurrency`, `verify_alreadyCompleted_isIdempotent_noSecondGatewayCall`; `CoreScheduler.hourlyReconciliationScan:264` runs a local-only `ReconciliationService.scan()` | PARTIAL |
| OP-02 | Detect gateway success with missing local fulfillment | `ReconciliationService.scanMissingEntitlements` (COMPLETED payment with no entitlement) | `ReconciliationServiceTest.scan_flagsACompletedPaymentThatGrantedNoEntitlement`; **detection is local-only, not driven by gateway records** | PARTIAL |
| OP-03 | Detect local success with uncertain gateway outcome | — | `ReconciliationException.TYPE_INCONSISTENT_STATUS` declared (:35) but **never raised by any scan** | MISSING |
| OP-04 | Detect unknown gateway references | `PaymentGatewayService:330` `log.warn` + `recordWebhook("…Unknown internalReference…")` | `webhook_unknownReference_isAcknowledgedAndIgnored`, `unknownReference_isAcknowledgedWithoutFulfilment`, `verify_unknownGatewayReference_keepsPendingForLater`; **no reconciliation exception type for it** | PARTIAL |
| OP-05 | Detect amount mismatches | `ReconciliationService.scanAmountInconsistencies` + callback amount guard | `amountMismatch_is422AndPendingPreserved`, `verify_amountMismatch_refusesFulfilment`; the scan itself is untested | DONE |
| OP-06 | Detect **currency** mismatches | callback-time guard only (`PaymentGatewayService:293`, `PaymentWebhookController:92`) | `currencyMismatch_is422WithNoCompletion`; **`ReconciliationService.scan()` has no currency check** | PARTIAL |
| OP-07 | Detect stale pending attempts | `ReconciliationService.scanStalePending` (24 h), wired to `CoreScheduler.hourlyReconciliationScan:264` | `scan_flagsAPaymentStuckForOverTwentyFourHours`, `scan_leavesAFreshPendingPaymentAlone` | DONE |
| OP-08 | Detect duplicate gateway events | `ReconciliationService.scanDuplicateProviderReferences` + callback replay guard | `scan_flagsADuplicateProviderReference`, `providerReferenceReplayAcrossPayments_is409`, `webhook_duplicateDelivery_afterCompletion_isHarmless` | DONE |
| OP-09 | Detect refund inconsistencies | `ReconciliationService.scanRefundInconsistencies` (over-refund, or REFUNDED with zero refunded) | implementation at `ReconciliationService:146-166`; **no test exercises it** | PARTIAL |
| OP-10 | Detect **earnings allocation** discrepancies | — | `ReconciliationException.TYPE_FEE_MISMATCH` declared only; **no scan compares `commissionAmount`/`providerShareAmount`** | MISSING |
| OP-11 | Never silently modify balances to force reconciliation; classify exceptions and recover only through authorized, auditable procedures | `ReconciliationService` (read-and-record only) + `resolve(id,resolvedBy,note)`/`assign`; `FinancialReportingController /v1/platform-admin/financial/reconciliation/*` `@PreAuthorize hasRole('ADMIN')` | `resolve_closesAnExceptionWithANote_andRecordsWho`, `assign_movesAnExceptionToInProgressUnderAnOwner`, `aPlatformAdminCanReadReconciliationExceptions`, `anInstitutionAdminCannotReadReconciliationExceptions` | DONE |
| OP-12 | Operational visibility across the 12 listed failure classes | logs in `PaymentGatewayService:150,330`, `PaymentWebhookController` `recordWebhook` (:56-125), `ParentPaymentService` fulfillment errors, `ReconciliationService.raise`, `CoreScheduler:80,273`; ops endpoint `GET /v1/platform-admin/webhook-events` (`PlatformAdminController:855`); `infrastructure/monitoring/prometheus.yml` + grafana/otel configs | classes recorded via `PlatformIntegrationService.recordWebhook` (`WebhookEvent`) and reconciliation exceptions; monitoring config files exist | DONE |
| OP-13 | **Correlation IDs** used in operational logs | `RequestCorrelationFilter` + `logging.pattern.level` | every request gets an `X-Correlation-Id` (inbound id reused only when it matches `[A-Za-z0-9._-]{1,64}`, otherwise replaced, so a hostile header is never echoed or logged), held in the MDC for the whole chain and cleared in `finally`; printed on every log line as `%5p [%X{correlationId:-}]`; registered ahead of the security chain by `SecurityConfig.requestCorrelationFilterRegistration` (`HIGHEST_PRECEDENCE + 1`); `RequestCorrelationFilterTest` 7/7 + `PaymentSpecSecurityCoverageTest.everyRequestCarriesACorrelationIdThatIsWiredAheadOfTheSecurityChain` | DONE |
| OP-14 | No secrets, auth tokens or unnecessary personal data logged | — | no secret or token values found in log statements (`PaymentWebhookController:55-60` names only; `StakabaGatewayClientImpl:247` names the env var); **emails are logged** (`AuthServiceImpl:341`, `JwtAuthenticationFilter:66`); no test asserts redaction | PARTIAL |

### Acceptance (spec §83–§86)

| Req | Requirement | Implementation | Evidence | Status |
|---|---|---|---|---|
| AC-01 | Functional: purchase creation, payment initiation, gateway verification, transaction history and details | `ParentPaymentService.initiatePayment`/`verifyPayment`, `PaymentGatewayService`, `TransactionSearchService` | `initiatePayment_usesTheAuthoritativePolicyPrice_notTheCallerAmount`, `verifyPayment_marksCompleteAndGrantsEntitlement`, `verify_matchingSuccess_completesExactlyOnceViaExistingPipeline`, `search_returnsStoredPaymentsWithoutInventingAny` | DONE |
| AC-02 | Functional: receipts, subscription activation, entitlement fulfillment, renewal and expiry | `ReceiptService`, `SubscriptionService`, `BundleGrantService`, `CoreScheduler` sweeps | `issueFor_issuesAReceiptForACompletedPayment`, `activatesFromAVerifiedPayment_usingTheApprovedAccessDuration`, `everyItemInTheBundleIsGranted`, `theScheduledSweepMarksRealElapsedRowsExpiredAndLeavesLiveOnesAlone` | DONE |
| AC-03 | Functional: individual purchases, bundles, event bookings, group payments, seat integrity | `MyPurchasesController`, `BundleGrantService`/`BundleTopUpService`, `EventBookingService`, `EventSeatCapacityConcurrencyTest` | `returnsOnlyTheCallersOwnPayments`, `twoConcurrentPurchasesCannotGrantTheSameServiceTwice`, `aGroupBookingConsumesEverySeatItBuys`, `concurrentRegistrations_neverExceedTheSeatCap` | DONE |
| AC-04 | Functional: parent/guardian scope, provider/teacher/organizer financial views, pricing governance | `ParentAuthorizationService`, `EarningsService`, `PricingPolicyAdminService` | `ParentAuthorizationServiceTest`, `earningsAreStatedOnlyForTheRecordedBeneficiary`, `oneBeneficiaryCanNeverSeeAnotherBeneficiarysEarnings`, `activate_movesADraftToActiveAndAuditsWhoApprovedIt` | DONE |
| AC-05 | Functional: refund workflow, reconciliation, notifications | `RefundService`, `BundleRefundService`, `ReconciliationService`, notification services | `RefundServiceTest` (`completeRefund_accumulatesTheRefundedAmountAndReachesREFUNDED`), `ReconciliationServiceTest`, `ParentFinancialNotificationEndpointTest`, `PaymentEventNotificationServiceTest` | DONE |
| AC-06 | Security: backend authorization, resource ownership, provider/institution scope, parent–child relationships, IDOR protection | `PaymentIdorHardeningTest`, `PaymentSpecSecurityCoverageTest`, `ParentAuthorizationServiceTest`, `ProviderIsolationSecurityTest`, `InstitutionScopeSecurityTest` | `aForeignIdAndAnIdThatNeverExistedAreIndistinguishable`, `anotherParentCannotReadIt`, `aParentCannotQuoteForAChildBelongingToAnotherFamily`, `endpoint_isForbiddenToANonAdmin` | DONE |
| AC-07 | Security: callback authentication, amount/currency validation, idempotency, safe receipt downloads, export authorization | `PaymentWebhookController`, `StakabaWebhookController`, `ReceiptController`, `FinancialReportingController` | `missingSecret_is401WithNoStateChange`, `amountMismatch_is422AndPendingPreserved`, `samePaymentRetry_isIdempotentSuccess`, `aParentReadsTheReceiptForAPaymentTheyMade`, `endpoint_isForbiddenToANonAdmin` (CSV export) | DONE |
| AC-08 | Security: refund and payout authorization, audit logging, secret protection | `RefundService` (+`AuditLogRepository`), `BundleRefundService.requirePlatformAdmin`, `StakabaProperties` webhook secret wiring | `institutionAdmin_refundsForeignCompletedPayment_403_statusUnchanged`, `RefundServiceTest` verifies `auditLogRepository.save`, `paymentWebhookSecret_envVarReachesThePropertyControllersRead`; **payouts not implemented (A-05, out of scope)** | PARTIAL |
| AC-09 | Quality: backend tests, frontend type check, frontend build, integration, security and migration tests | `mvn -o test`; `npx tsc --noEmit`; `npm run build`; `integration/`, `security/`, `FlywayMigrationValidationTest`, `SubscriptionMigrationConstraintTest` | recorded in §1a with exit codes and class counts | DONE |
| AC-10 | Quality: responsive UI, accessibility, failure recovery, documentation consistency | `frontend/e2e/responsive.spec.ts`, `accessibility.spec.ts`; in-place retry on the payment surfaces | `mobile viewport hides sidebar`, `login page has proper heading structure`, "surfaces a load failure with a retry that refetches"; **documentation is maintained by hand with no consistency check** | PARTIAL |
| AC-11 | Production readiness reported separately from code completion | §7 nine-rung verification ladder + final verdict below | the ladder records rungs 1–9 individually, "Highest rung actually reached: 2", "It is not ready for money" — mock evidence is never presented as sandbox evidence | DONE |
| AC-12 | Remaining production-readiness blockers listed rather than claimed | §3 PARTIAL/MISSING/BLOCKED table, §7 rungs 4–9, `docs/PAYMENT-OWNER-DECISIONS.md` summary | G-06, G-07, A-05, A-07, D-06 plus 5 open decisions are explicit; **prod config, secret management, monitoring, operational recovery and security verification are not itemised as blockers** | PARTIAL |

**Counts: DONE 143 · PARTIAL 46 · NEEDS VERIFICATION 0 · BLOCKED 5 · MISSING 5. BROKEN 0.**
Recomputed from the rows themselves, not asserted. The matrix now holds **199 ID rows** across
19 groups: the original 67 (`F`,`G`,`S`,`B`,`E`,`P`,`R`,`A`,`D`) contribute 61 DONE, 1 PARTIAL
(E-05) and 5 BLOCKED (G-06, G-07, A-05, A-07, D-06); the completion pass added **132 rows**
(`UI` 21, `RO` 15, `AP` 17, `AV` 13, `RC` 8, `AU` 5, `NT` 14, `TC` 13, `OP` 14, `AC` 12)
contributing 82 DONE, 45 PARTIAL and 5 MISSING. Adding the single non-ID N/A row (bank/card
payment methods, §3) gives **200 entries**. The 14 §63 security rows above are held as
cross-referenced evidence for the one section they come from and are deliberately not counted
again.

**Against the 98 % target: not met, and the arithmetic is the point.**
- Strict — DONE ÷ ID rows: **143 / 199 = 71.9 %**.
- Generous — DONE + PARTIAL ÷ ID rows: **189 / 199 = 95.0 %** (counting N/A as satisfied:
  190 / 200 = 95.0 %).

The earlier record stood at 61/67 = 91.0 % because the first pass only enumerated the sections it
had already built against. Expanding the denominator to every requirement-bearing section drops
the figure by ~19 points; that drop is a measurement correction, not a regression. Three rows then
closed inside the same round — OP-13 and NT-12 to DONE, AU-03 from MISSING to PARTIAL — which is
why these figures are 71.9/95.0 rather than the 70.9/94.0 the expansion first produced.

`MISSING` is 5 rather than 0 because this pass reports gaps it did not close: UI-05 (status
timeline), UI-06 (masked gateway reference), RO-07 (provider payment surface), OP-03
(local-success/gateway-uncertain) and OP-10 (earnings allocation discrepancy). `BROKEN` remains 0
because the one genuinely broken path found in the earlier pass — BR-03 rejecting the correct
top-up amount at initiation — was fixed, and because this round's two fixes (receipt and
refund-request read oracles) closed rather than introduced defects.


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
   V175. **Two are now answered and implemented — Decision 1 (rules 1–6, no grace period) and
   Decision 5 (type-names-only catalogue picker), recorded at §1e/§2a (A-06, B-07, B-10) and
   defects 6–8 in §2.** Five remain: commission rate (§2 → A-05), component refund valuation
    (§3 → A-07), organizer-paid booking rules (§4 → E-05), sandbox pricing activation (§6 → G-06),
    V175 (→ D-06). Items 1, 4, 6, 7 above all still resolve behind those.
9. **§61 notification coverage — DONE.** `PaymentEventNotificationService` emits the four events the
   derived parent inbox cannot project (payment initiated, subscription activated, subscription
   expired, refund status changed) through the existing `NotificationService`, so preferences and
   dedupe still apply. Hooked at `ParentPaymentService.initiatePayment` and
   `SubscriptionService.create`/`expireElapsed`, and at all seven `RefundService` transitions.
   Booking confirmed was already emitted at `EventServiceImpl:722,746`; payment
   verified/failed/cancelled and receipt-available stay derived on read, which is what keeps a
   replayed callback from telling a parent twice. A refund completion is skipped only when the
   payment is *fully* refunded, so a completed **partial** refund is still reported. Evidence:
   `PaymentEventNotificationServiceTest` 12/12.
10. **§45 read-path existence oracles — DONE.** `ReceiptController` and `RefundController.get`
    now answer 404 with the same wording for a foreign id and an id that never existed, instead of
    403 for foreign, which let a caller probe which ids are real. Mutation paths (refund approve/
    reject, payment cancel) deliberately stay 403. Evidence:
    `PaymentSpecSecurityCoverageTest.anUnknownPaymentIdAndAForeignOneAreIndistinguishable`.


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
verified completion — and, since Decision 1, an entitlement that *stays* granted only while its
period runs, with repurchase after expiry opening a new period instead of silently charging for
nothing; refunds bounded and split-persisted, per-beneficiary earnings isolated, and a bundle
catalogue whose items must resolve to a real, priced service **and may now be chosen from a
catalogue of type names rather than typed blind**. Receipts, notification writes and bundle quotes
are scoped to their owner, with a foreign id answering 404 rather than 403 so nothing leaks about
whether it exists. `MISSING 0 · BROKEN 0`.

It is **not** ready for money. One PARTIAL (E-05 organizer-paid) and five BLOCKED rows (G-06,
G-07, A-05, A-07, D-06) resolve behind the five still-open decisions in
`docs/PAYMENT-OWNER-DECISIONS.md`, and rungs 4-9 of §27 cannot begin until those decisions and
the sandbox authorisation exist. Claiming anything beyond rung 2 today would breach
`Payment.md:590`.
