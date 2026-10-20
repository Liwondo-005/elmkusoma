# ELMKUSOMA Payments — Owner Decision Request

One consolidated request. Every item below is something `Payment.md` explicitly forbids inventing,
or a commercial rule the code cannot infer. Each lists the exact choice, the proposed default, and
what it costs if the default is accepted or refused.

Nothing here has been implemented. No rate, window, grace period or payer rule was assumed.

**Prepared:** 2026-10-20 · **Repo:** `main` @ `9391b424` · **Spec:** `Payment.md`

---

## 1. Entitlement expiry and repurchase after expiry

**Status:** **ACCEPTED — rules 1–6 as proposed, no grace period. IMPLEMENTED.**
Evidence: `PAYMENT-PROGRESS.md` §1a (re-verification), §2a (A-06, B-07, S-09), defects 6–8 in §2;
`BundleGrantServiceTest` 13/13, `EntitlementExpirySweepIntegrationTest` 5/5,
`CoreSchedulerExpirySweepTest` 7/7.

**The defect.** Nothing sets an entitlement's `status` to `EXPIRED`. An elapsed entitlement stays
`ACTIVE` forever, and the two sides of the system read that differently:

- `BundleTopUpService.currentlyValid` treats elapsed time as **not coverage** → the parent is
  charged for the repurchase.
- `BundleGrantService.grantOne` treats `status = "ACTIVE"` as **already held** → the grant is
  declined.

A legitimate repurchase after expiry is therefore charged in full and grants nothing.

**Decision required:** the rule that closes the gap. Proposed default (matches the six rules
already circulated and *not yet approved*):

| # | Rule | Default |
|---|---|---|
| 1 | Access ends when `expiresAt` passes, regardless of a stale persisted `ACTIVE` | Yes |
| 2 | After a verified repurchase past expiry, create a new period and keep the old row as `EXPIRED` | Yes |
| 3 | No automatic grace period | Yes (A-06 already used this reading) |
| 4 | Repurchase while still valid follows existing top-up/overlap/pricing rules | Unchanged |
| 5 | Failed, pending, rejected or unverified payments never grant | Unchanged |
| 6 | The scheduler updates stored status; access checks and purchase eligibility independently enforce `expiresAt` | Yes |

**Consequence if accepted:** bundle entitlement and top-up requirements move to DONE and the
charge-without-grant defect is fixed with regression tests covering scheduler delay, expired
access, repurchase after expiry, valid-entitlement top-up, payment failure, duplicate callback and
idempotency.

**Consequence if refused:** the defect stays open and documented; affected requirements stay
BLOCKED. **No code will implement this without approval** — it changes when money is taken.

**Proposed default: rules 1–6 as written, with no grace period.**

---

## 2. Commission policy — A-05

**Status:** BLOCKED. `Payment.md` §59 line 1171: *"Do not assume any commission percentage."*
§23 lists commission percentages among the decisions that must not be invented.

**What already exists and is DONE (no decision needed):**
- `CommissionPolicy` (rate or fixed amount, `DRAFT`/`ACTIVE`/`RETIRED`, versioned) — seeded
  `V170` as `DRAFT` with a **NULL rate**, so nothing has ever been charged.
- `CommissionService.allocate` computes at confirmation time from the ACTIVE policy and stamps
  `commissionAmount`, `providerShare`, `commissionStatus` onto the payment.
- Statuses `NO_ACTIVE_POLICY`, `RATE_NOT_APPROVED`, `RATE_EXCEEDS_AMOUNT` are recorded — an
  unresolved allocation is stored as *unresolved*, never as zero.
- `EarningsService` and `FinancialReportingService` report gross, refunds, commission, beneficiary
  allocation and net separately; where any transaction lacks an allocation they return
  **"unavailable"** rather than a figure.
- No HTTP endpoint can create or activate a policy (GET only), so a rate can only enter by an
  authorised write.
- **No hardcoded rate exists anywhere in the codebase.**

**Decision required:** the rate and its shape.

| Choice | Options | Recommendation |
|---|---|---|
| Rate form | Percentage of net · Fixed amount per transaction · Per-service-type rates · Not yet | **Percentage of net**, single platform-wide rate to start |
| Basis | Gross · Net after discount · Net after gateway fee | **Net after discount** (already what `allocate` receives) |
| Rounding | Half-up · Half-even · Truncate | **Half-up to 2 dp** (already implemented, `MONEY_SCALE = 2`) |
| Effective when | At confirmation · At settlement | **At confirmation**, stamped on the payment (already implemented) |
| Policy change after sale | Recompute history · Keep the snapshot | **Keep the snapshot** (already implemented — refunds reverse from the recorded figure, never from today's rate) |
| Activation authority | Platform Admin role · Explicit migration + sign-off | **Platform Admin write path**, once approved |

**Example (illustrative only, not an approved rate):** net TZS 25,000 at 10% → commission
TZS 2,500, provider share TZS 22,500; a later refund of TZS 10,000 reverses TZS 1,000 / TZS 9,000.

**Consequence if refused:** earnings reporting continues to state "no approved rate" honestly.
**Settlement and payout stay BLOCKED** regardless — `Payment.md` §60 line 1199 also requires
gateway support, approvals and safeguards that do not yet exist.

**Proposed default: percentage of net, half-up to 2 dp, evaluated and snapshotted at
confirmation, snapshot never rewritten by a later policy change.**

---

## 3. Component-level refund valuation — A-07

**Status:** BLOCKED for *component* valuation only. `Payment.md` §57: *"Do not invent eligibility
or processing windows."* §23 lists refund windows and proration rules as unresolved.

**What already exists and is DONE (no decision needed):** the **whole-payment** refund basis is
implemented and enforced at four independent levels —

1. `RefundService.requestRefund` bounds the request against `payment.refundableAmount()`.
2. `completeRefund` re-derives the balance **under a pessimistic row lock** before applying.
3. `BundleRefundCalculator.breakdown` refuses before any write.
4. DB constraints `chk_refunded_not_overpaid` / `chk_refunded_not_negative` (V171) plus a partial
   unique index allowing only one open refund per payment.

Basis: **collected − already refunded**. No window, no proration, no client-supplied ceiling.

**Decision required:** whether a bundle's refundable amount may be split by component.

`BundleRefundCalculator` states the current position plainly: *"Deliberately not a component
valuation. Pricing what an individual item inside a bundle is 'worth' is a commercial decision
nobody has approved."*

| Choice | Options | Recommendation |
|---|---|---|
| Bundle refund basis | Whole-payment only · Pro-rata by listed price · Equal share per item · Access-time weighted | **Whole-payment only** until a split rule is approved |
| Refund window | None · Fixed days from purchase · Case-by-case | **None** (no invented window), case-by-case approval as today |

**Consequence if refused:** partial refunds inside a bundle can only be requested as a proportion
of the whole payment, which is what the code already does.

**Proposed default: whole-payment basis, no window, no component split.**

---

## 4. Organizer-paid group booking — E-05

**Status:** PARTIAL. `Payment.md` §53 requires both *"Organizer-paid group bookings"* and
*"Individually paid participant bookings"*, and §55 says organizer-paid groups *"follow the
approved group rules"* — which do not exist.

**What already exists and is DONE:** multi-seat booking end to end — `seats` column with
`CHECK (seats > 0)`, capacity measured as `SUM(seats)`, price `= ticketPrice × seats` computed
server-side, seat held only on a `COMPLETED` payment, idempotent single confirmation, pessimistic
event-row lock preventing oversell, amount/currency mismatch rejected at the webhook and gateway
boundary, cancellation releasing seats, and no `bookingType`/`payerType` anywhere.

**Decision required:** what an organizer-paid booking *is*.

| Choice | Options | Recommendation |
|---|---|---|
| Who is the payer of record | Organizer's own account · A dedicated booking account | **Organizer**, recorded as `purchaserType = ORGANISER` on the payment |
| How attendees are attached | One registration per named attendee on one payment · One group registration | **One registration per named attendee**, all linked to the organizer's payment |
| When is it confirmed | Same rule as individual: only on verified success | **Same rule** — no separate confirmation path |
| Can the organizer hold seats before paying | No · Yes, time-limited hold | **No** — matches the existing "no seat until COMPLETED" rule |
| Refund/cancellation of a group | Release all seats · Keep seats | **Release all seats** (consistent with individual cancellation) |

**Consequence if refused:** E-05 stays PARTIAL and the organizer-paid flow is not claimed as
complete. The individually-paid multi-seat flow remains fully DONE and tested.

**Proposed default: organizer is the payer, one registration per named attendee on one payment,
confirmation only on verified success, no pre-payment seat hold, cancellation releases all seats.**

---

## 5. Who may read the service catalogue — B-10 frontend

**Status:** **ACCEPTED — service type names only, read by `INSTITUTION_ADMIN` (and platform ADMIN).
IMPLEMENTED.** `GET /v1/institution/bundles/catalogue/service-types` returns distinct `serviceType`
names of ACTIVE pricing policies and nothing else; the bundle editor renders them as a picker and
falls back to text input when the catalogue is empty. Evidence: `PAYMENT-PROGRESS.md` §2a (B-10),
`PaymentSpecSecurityCoverageTest` (INSTITUTION_ADMIN 200, PARENT 403), `npx tsc --noEmit` 0,
`npm run build` 0.

The bundle editor now validates server-side that every item names a service the platform actually
sells (an ACTIVE pricing policy exists for its `serviceType`). The editor UI still offers free-text
inputs; a picker needs an endpoint an **institution admin** can call. `GET /v1/platform-admin/pricing`
exists but requires `hasRole('ADMIN')` (platform admin), which the bundle editor is not.

**Decision required:** who may read the pricing/service catalogue.

**Proposed default:** expose a **read-only** catalogue of *service type names only* (no amounts,
no margins) to `INSTITUTION_ADMIN`, so the picker cannot leak platform pricing policy while still
being consistent with the backend.

---

## 6. Activate a pricing policy — blocks the first sandbox checkout

**Status:** operational prerequisite for G-06, not a code gap.

`V169` seeds 17 pricing policies as `DRAFT`. `PricingService.resolvePrice` only reads `ACTIVE`
rows, so **no payment can be priced** until at least one policy is activated — `docs/STAKABA-SANDBOX-TESTING.md`
lists this as blocker 1 for the sandbox run.

Activation is already an authorised operation (`POST /v1/platform-admin/pricing/policies/{id}/activate`).

**Decision required:** authorise activating a **sandbox-only** ACTIVE policy so a real sandbox
transaction can be executed for G-06.

**Consequence if refused:** G-06 cannot progress past configuration verification.

**Proposed default: activate the seeded default-price policies for the duration of sandbox
testing, then retire them.** This changes no production rate because `STAKABA_ALLOW_LIVE=false`
and no live key is accepted.

---

## 7. V175 reconciliation — D-06

See `docs/V175-RECOVERY-PROCEDURE.md`. Requires **database-owner** authorisation only; it is a
read-only analysis today and nothing has been written to shared history.

---

## Summary

| # | Decision | Blocks | Proposed default | Status |
|---|---|---|---|---|
| 1 | Expiry / repurchase rule | Entitlement requirements + a live charge-without-grant defect | 6 rules, no grace period | **ACCEPTED + IMPLEMENTED** |
| 2 | Commission rate | A-05 settlement/payout | Percentage of net, snapshot at confirmation | open |
| 3 | Component refund valuation | A-07 component split | Whole-payment basis, no window | open |
| 4 | Organizer-paid booking rules | E-05 organizer-paid flow | Organizer pays, per-attendee registration | open |
| 5 | Catalogue read audience | B-10 frontend picker | Type names only, to institution admin | **ACCEPTED + IMPLEMENTED** |
| 6 | Sandbox pricing activation | G-06 first checkout | Activate seeded policies for sandbox only | open |
| 7 | V175 history reconciliation | D-06 | Owner executes `docs/V175-RECOVERY-PROCEDURE.md` | open |

Decisions 1–4 change when or how much money is taken. They require explicit approval in writing
before any code implements them. **Decision 1 has that approval and the code is in**; the other
three in that group still do not, and no code implements them.
