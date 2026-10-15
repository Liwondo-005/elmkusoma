# Payment Backend — Developer 01 → Developer 02 API Contract

Status: complete for the Developer 01 scope in `Payment.md`.
Live gateway: **not enabled**. No production credentials, no real money collection, no payouts.

---

## 1. What changed, and why the UI must adapt

Developer 01 closed every verified backend gap in the payment domain. Four defects that made
the checkout unsafe are fixed, and the pricing, commission, refund, receipt and reconciliation
layers that were entirely missing now exist.

Three consequences the UI must reflect:

1. **The client no longer sets a price.** `initiatePayment` resolves the amount from the
   authoritative pricing policy and stores that. Sending a different amount is rejected with
   HTTP 409. The checkout screen should display the price the server returns and must not post
   one it invented.
2. **A discount is resolved server-side.** The UI never sends a discount amount. If a discount
   applies, the initiate response reports it and the net is what gets charged.
3. **"Paid" is not a synonym for "entitled", and "refunded" is not a request.** Entitlement is
   granted only on a COMPLETED payment, and a refund only moves money at COMPLETED status.
   Intermediate refund states must be shown as such, never collapsed into "refunded".

---

## 2. Pricing policy model

Every chargeable amount is read from `pricing_policies`, a versioned table. Payment policies are
seeded **all DRAFT — none ACTIVE**. Nothing is priced until a Platform Admin activates a policy,
which is an audited decision.

A resolved price carries the terms it was sold under:

| Field | Meaning |
|---|---|
| `amount` | The authoritative charge in TZS |
| `policyKey` | e.g. `SERVICE_DEFAULT_PRICE`, `EDUCATION_LEVEL_BAND` |
| `policyVersion` | The version active at initiation, snapshotted onto the payment |
| `accessDays` | Access length this purchase grants, snapshotted |

Snapshotted terms are never recomputed. A later price change cannot rewrite history.

Education bands (`PRIMARY`, `SECONDARY`, `HIGHER`) are governed: a band price must fall inside
the ACTIVE band for the learner's level, or the charge is refused with 409.

---

## 3. Endpoints

### Parent — checkout and history

#### `POST /v1/parents/payments/initiate`

Role: `PARENT`.

```json
{
  "studentId": "uuid",
  "serviceType": "ONLINE_COURSE",
  "serviceId": null,
  "amount": 2000,
  "description": "Optional",
  "idempotencyKey": "optional-client-token"
}
```

`amount` is checked against the resolved policy price, not trusted. A mismatch is 409.
`idempotencyKey` is optional; a retry with the same key replays the existing payment instead of
creating a second one, so a checkout that can be retried should always send one.

Response:

```json
{
  "paymentId": "uuid",
  "status": "PENDING",
  "amount": 2000,
  "policyKey": "SERVICE_DEFAULT_PRICE",
  "policyVersion": 1
}
```

#### `GET /v1/parents/payments`
#### `GET /v1/parents/children/{studentId}/payments`
#### `POST /v1/parents/payments/{paymentId}/cancel`
#### `GET /v1/parents/children/{studentId}/entitlements`

Unchanged in shape. Entitlement validity now also respects `startsAt` and `expiresAt`, and a
refund revokes the entitlement that payment granted.

### Parent — refunds

#### `POST /v1/parents/payments/{paymentId}/refund-requests`

Role: `PARENT` (ownership checked — you may only request against a payment you made).

```json
{ "amount": 10000, "reason": "Duplicate charge", "evidenceUrl": null }
```

Opens a request. This is **not** a refund. Only a COMPLETED payment can be requested against,
the amount may not exceed the refundable balance, and only one refund may be open per payment.

#### `GET /v1/parents/payments/{paymentId}/refund-requests`

Your refund requests and their states.

Refund states, in order:

```
REQUESTED -> UNDER_REVIEW -> APPROVED -> PROCESSING -> COMPLETED
                     \-> REJECTED
         \-> CANCELLED (from any non-terminal state)
```

**Only COMPLETED means money was returned.** Every earlier state means "not yet". The UI must
not render REQUESTED, UNDER_REVIEW, APPROVED or PROCESSING as a completed refund.

### Parent — receipts

#### `GET /v1/parents/payments/{paymentId}/receipt`
#### `GET /v1/parents/receipts/{receiptNumber}`
#### `GET /v1/parents/receipts`

A receipt exists only for a COMPLETED payment. Knowing a receipt number is not authorisation —
a parent may only read receipts for payments they made.

A refund does not rewrite the original receipt. It is annotated with a credit note
(`creditNoteNumber`, `creditNoteReason`, `refundedAmount`) so the historical record of what was
charged survives intact beside the reversal.

### Platform Admin — pricing governance

All require role `ADMIN`, under `/v1/platform-admin/pricing`.

| Method | Path | Purpose |
|---|---|---|
| GET | `` | List policies |
| GET | `?status=DRAFT` | List by status |
| POST | `/{policyId}/activate` | Activate a policy (audited; names the approver) |
| POST | `/{policyId}/retire` | Retire an ACTIVE policy (audited) |
| GET | `/history?serviceType=...` | Version history for one service or level |

Activation refuses rather than adjusts when a policy is malformed or would create a second
ACTIVE price for the same service or band. Clamping a price into range would change what the
provider asked for without anybody deciding to.

### Platform Admin — refunds

All require role `ADMIN`, under `/v1/platform-admin/refunds`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/{refundId}/review` | Move REQUESTED → UNDER_REVIEW |
| POST | `/{refundId}/approve` | Approve; body `{ "note", "revokeEntitlement" }` |
| POST | `/{refundId}/reject` | Reject; body `{ "note" }` |
| POST | `/{refundId}/processing` | Record dispatch; body `{ "refundReference" }` |
| POST | `/{refundId}/complete` | **The only transition that moves money** |
| POST | `/{refundId}/cancel` | Cancel a non-terminal refund |

Approval is a decision, not a completion — no money has moved at APPROVED. With no live
gateway, reaching COMPLETED is an authorized back-office action confirming that processing
actually happened, not something a callback can do.

`revokeEntitlement` is chosen at approval and applied at completion. A partial refund may
legitimately retain access while a full one revokes it.

No approval threshold or time limit is invented; every approval names the human who made it.

#### Single-step admin refund — `POST /v1/parents/payments/{paymentId}/refund`

Role: `ADMIN`, `INSTITUTION_ADMIN`. Body: `{ "reason": "..." }`. Returns the payment.

This is a **full refund of a COMPLETED payment in one call** — no request, no review. It is a
back-office shortcut, not a second way of accounting: it now runs the same governed lifecycle
(request → approve with entitlement revoked → complete), so it records `refundedAmount`, writes
the credit note and revokes the entitlement exactly as the multi-step path does.

What the UI must expect from it:

- It refuses a payment that is not COMPLETED, and refuses one with no refundable balance left.
- It always revokes the entitlement — there is no retain option here.
- After it returns, the payment shows `status: REFUNDED`, `refundedAmount` equal to the full
  amount, and the receipt carries a credit note.

**There is one writer of a payment's refund history: the governed lifecycle.** Whichever path
an admin takes, `refundedAmount` accumulates there and the refundable balance is re-checked
under a row lock at completion, so the same money cannot be returned twice. The database
enforces the same rule as a backstop (`refunded_amount <= amount`).

### Platform Admin — financial command centre

All require role `ADMIN`, under `/v1/platform-admin/financial`.

#### `GET /summary?from=...&to=...` or `?lastNDays=30`

Every figure is a `Metric` with an `availability` field:

```json
{
  "confirmedCollections": { "name": "...", "value": 50000, "currency": "TZS",
                            "availability": "AVAILABLE", "unavailableReason": null },
  "profit": { "name": "profit", "value": null, "currency": null,
              "availability": "UNAVAILABLE", "unavailableReason": "Profit requires ..." }
}
```

**Profit is permanently UNAVAILABLE.** It requires processing fees, taxes, refunds, expenses and
settlement timing, none of which has an approved methodology. Platform revenue minus provider
share is an operational estimate, not profit, and is deliberately not published as a figure.

`platformRevenue` and `providerShares` are UNAVAILABLE whenever any confirmed transaction in the
period lacks a resolved commission allocation. Summing only the resolved ones would understate
the total while looking precise, which is worse than saying it cannot be computed yet.

**The UI must render `UNAVAILABLE` as unavailable, never as zero, and must show
`unavailableReason`.** A zero here would assert a fact the business has not approved.

#### Reconciliation

| Method | Path | Purpose |
|---|---|---|
| POST | `/reconciliation/scan` | Detect inconsistencies; read-and-record only |
| GET | `/reconciliation/exceptions` | Open exceptions |
| POST | `/reconciliation/exceptions/{id}/assign` | Take ownership |
| POST | `/reconciliation/exceptions/{id}/resolve` | Close with a note |

Exception types: `PENDING_UNRESOLVED`, `AMOUNT_MISMATCH`, `DUPLICATE_EXTERNAL_REF`,
`MISSING_ENTITLEMENT`, `REFUND_MISMATCH`, `FEE_MISMATCH`, `INCONSISTENT_STATUS`,
`MISSING_INTERNAL_RECORD`.

Scanning never edits a payment and never rewrites an amount. A discrepancy that has been
silently "corrected" leaves no trace that it ever existed, which is exactly how a real mismatch
disappears. Detection is idempotent — one open exception per (type, reference).

### Platform Admin — webhooks

`POST /v1/webhooks/payments` is for the gateway only, not for the UI. It is HMAC-authenticated
and the signature is verified before any lookup. Unauthenticated calls are rejected.

---

## 4. Error contract

| Status | When |
|---|---|
| 400 | Malformed body, missing required field, non-positive amount |
| 401 | Missing or invalid authentication |
| 403 | Authenticated but not permitted (a parent reading another family's data, non-admin reaching a governance endpoint) |
| 404 | Resource does not exist, or is not visible to this caller |
| 409 | Pricing conflict: amount mismatch, band breach, discount/rate out of range, refund state conflict, duplicate open refund |
| 500 | Unexpected server error |

Pricing and governance conflicts surface as **409**, not 500 — they are business-rule
rejections with a human-readable message, and the checkout should show that message.

---

## 5. Entitlement semantics

### Access length granted by one verified payment

Resolved from the payment's snapshotted `accessDays` where present, otherwise from the service
type:

| Service type | Access |
|---|---|
| `DAILY_ACCESS` | 24 hours (rolling, from verification — decided in Payment.md §5) |
| `WEEKLY_ACCESS` | 7 days |
| `MONTHLY_ACCESS`, `BASIC_ACCESS`, `PLUS_ACCESS`, `PREMIUM_ACCESS` | 30 days |
| Any other service | 30 days |

### Renewal

A renewal extends from the current expiry. It never restarts the clock from now, so buying a
second month does not discard a remaining fortnight.

### Validity

An entitlement counts as valid only when its status is ACTIVE, `startsAt` is not in the future,
and `expiresAt` is null or in the future.

### Refund

A completed refund with `revokeEntitlement` set revokes the entitlement **that payment granted**
— identified by payment id, not merely by matching the service — and ends it immediately.

---

## 6. Financial rules the UI must not paper over

- **Money is `BigDecimal` end to end.** No floating point anywhere in a displayed figure.
- **Currency is TZS.** Amounts are minor-unit integer shillings.
- **A commission rate is never invented.** If no ACTIVE rate-bearing policy existed when a
  payment was confirmed, its allocation is recorded as unresolved (`commissionStatus` =
  `NO_ACTIVE_POLICY` / `RATE_NOT_APPROVED`), with `commissionAmount` null. Null means "not yet
  decided", which is **not** the same as zero, and the two must never be rendered alike.
- **Who bears the gateway fee is undecided** (`feeBearer` null) until a policy records it.
- **Receipt failure never rolls back a confirmed payment.** The payment stays COMPLETED, the loss
  is recorded as a reconciliation exception, and the receipt is reissued by a human.
- **A refund can never exceed what was charged.** `refundedAmount` accumulates across partial
  refunds, the remaining balance is re-checked under a row lock at completion, and the database
  refuses `refunded_amount > amount` outright. A payment refunded once has nothing left to give,
  whichever path the second attempt takes.

---

## 7. Open items that are deliberately NOT done

These need a business decision, not code. Do not implement defaults for them.

- The commission rate itself, for any service.
- Who bears the gateway processing fee.
- Any refund approval threshold or time limit.
- Any discount rule. The table exists; no rule is activated.
- An accounting methodology for profit.

Note: the DAILY access model is **not** open. Payment.md §5 resolves it as a rolling 24 hours
from verified entitlement activation, and the backend implements it that way.

---

## 8. Server types

- `PricingPolicy` — `pricing_policies`
- `CommissionPolicy` — `commission_policies`
- `DiscountRule` — `discount_rules`
- `RefundRequest` — `refund_requests`
- `PaymentReceipt` — `payment_receipts`
- `ReconciliationException` — `reconciliation_exceptions`
- `Payment` — `payments`, extended with policy snapshot, discount/net, idempotency, commission
  snapshot, `refundedAmount`, `receiptNumber`
- `Entitlement` — `entitlements`

Migrations: `V169__pricing_policies.sql`, `V170__financial_integrity_refunds_receipts_commission.sql`,
`V171__refund_integrity.sql`.
All are additive; no table is dropped, no column renamed, no history rewritten. V171 adds the
`refunded_amount <= amount` and `refunded_amount >= 0` checks that make over-refunding
impossible at the storage layer.

**Migrations are proposed only.** None of V169–V171 has been applied to any environment, per
the approval gate in Payment.md §19. They must be reviewed and approved before running.
