# Stakaba Mobile-Money Integration — Sandbox Testing Guide

**Status: sandbox only.** No live credentials, no live transactions, no production
enablement. Nothing in this document was executed against the real Stakaba API —
automated coverage is mocked. See "What has NOT been verified" at the end.

---

## 1. What was implemented

Stakaba is wired into the **existing** ELMKUSOMA payment domain. No parallel
payment, subscription or receipt system was created.

| Layer | Change |
|---|---|
| Adapter | `payments/gateway/StakabaGatewayClientImpl` — documented REST calls only |
| Orchestration | `parent/service/PaymentGatewayService` — all outbound calls, verification, fulfilment rules |
| Webhook | `parent/controller/StakabaWebhookController` — `POST /v1/webhooks/stakaba` |
| Config | `application.yml` → `payment.stakaba.*`, validated at startup |
| Storage | `V172__stakaba_gateway_integrity.sql` — partial unique index on the gateway reference |
| UI | Parent payments page — network/phone checkout, pending state, status polling |

A subscription tier (BASIC/PLUS/PREMIUM) and a student access pass are the
**same priced purchase** end to end in this codebase, so both are paid through
the one checkout. No separate subscription purchase system was introduced.

### Verified Stakaba contracts (from official docs only)

| Fact | Source |
|---|---|
| Base URL `https://api.stakaba.com` (same sandbox/live; key selects mode) | `/get-started/environments` |
| Auth header `x-api-key`; sandbox `sk_test_...`, live `sk_live_...` | `/get-started/authentication` |
| `POST /api/v1/payments/collection` → **201** `{"internalReference":"..."}` | `/api-reference/payments/collection-...post` |
| Body: `grossAmount` (whole integer TZS), `currency` (TZS), `mobileNumber` (`255XXXXXXXXX`), `network`, `metadata` | `/payments/mobile-money-collections` |
| Networks, case-sensitive: `Mpesa`, `AirtelMoney`, `TigoPesa`, `HaloPesa` | same |
| `GET /api/v1/transactions/{internalReference}` for status | same + `/api-reference/transactions/get-transaction-...get` |
| Events `transaction.success` / `transaction.failed`; **no signature documented** | `/payments/webhooks` |
| "Never fulfil on the webhook alone — always verify via the API" | `/payments/webhooks` |
| Errors `{"error","message"}`; 401/403 auth, 400/422 validation, 429 rate, 502 provider | `/reference/error-handling` |
| No idempotency key is accepted; every attempt mints a new `internalReference` | `/reference/error-handling` |

**Known documentation conflict (documented, not guessed around):** the
auto-generated OpenAPI page for *Get Transaction* lists an `Authorization:
Bearer` header, while the Authentication page, the Environments page and the
Mobile Money Collections guide all specify `x-api-key` for payment endpoints.
This implementation sends **`x-api-key`**, matching three of four sources and
the documented "All payment endpoints use an `x-api-key` header" rule. If a
sandbox call returns 401, this is the first thing to re-check with Stakaba
support.

---

## 2. Required environment variables

Set these **in `backend/.env` only** (git-ignored). Never in source, a commit,
a frontend variable, a screenshot or chat.

```properties
ELMKUSOMA_PAYMENT_PROVIDER=stakaba
STAKABA_ENABLED=true
STAKABA_BASE_URL=https://api.stakaba.com
STAKABA_API_KEY=sk_test_...        # your sandbox key from app.stakaba.com > Settings > API Keys
```

Optional (defaults shown):

```properties
STAKABA_CONNECT_TIMEOUT_MS=5000
STAKABA_READ_TIMEOUT_MS=15000
STAKABA_ALLOW_LIVE=false          # must stay false
```

Get the key at <https://app.stakaba.com> → *Settings → API Keys* → copy the
**sandbox** key. To set it without it ever appearing in a shared artifact,
edit `backend/.env` in your editor (or run `nano backend/.env`) and paste it
there.

### Fail-safe behaviour (all unit-tested)

- Gateway **disabled** by default — the legacy manual-verification flow is
  byte-for-byte unchanged.
- Enabled without a key, with a non-HTTPS base URL, with a malformed key, or
  with a **`sk_live_` key while `STAKABA_ALLOW_LIVE=false`**, the app refuses
  to start and names the variable to set. It never silently runs live.
- The key is read from a header only; it is never logged, never placed in an
  exception message, and never returned to the browser.

---

## 3. Manual sandbox checklist

Prerequisites: an ACTIVE pricing policy (see §4), a parent account with at
least one child, and the variables above.

### Documented sandbox behaviour you can rely on

Per `/get-started/sandbox-and-live-mode`, with a **sandbox key** no real money
moves, and:

| Payer number | Simulated result |
|---|---|
| any valid Tanzanian number | simulated **success** |
| `255000000001` | simulated **failure** |

`255000000001` is accepted by ELMKUSOMA **only** when `STAKABA_API_KEY` is a
sandbox key; on a live key it is rejected like any other invalid number. Use it
to test the failure path end to end (checklist step 11).

- [ ] **1. Config points at sandbox.** `STAKABA_API_KEY` starts with `sk_test_`
      (never `sk_live_`); `STAKABA_BASE_URL=https://api.stakaba.com`.
- [ ] **2. App starts clean.** No `StakabaConfigurationValidator` error. The
      log line reads `keyMode=SANDBOX` and never prints the key.
- [ ] **3. Disabled is safe.** Set `STAKABA_ENABLED=false`, restart, confirm the
      startup line says the gateway is disabled and the payments page still
      loads with no gateway calls.
- [ ] **4. Initiate from the UI.** Parent → *Payments* → *New payment* → pick a
      child, a service, a network and a phone number → *Send payment prompt*.
- [ ] **5. Stakaba receives the right values.** The payment row shows
      `provider=stakaba` and an `internalReference`; the request carried the
      policy price as a **whole integer TZS**, currency `TZS`, the **exact-case**
      network, and the 12-digit MSISDN.
- [ ] **6. UI shows PENDING — never "paid".** While the prompt awaits approval
      the page polls `/v1/my/payments/{id}/status` and shows "Waiting for
      approval on the phone".
- [ ] **7. Approve the USSD prompt** with the sandbox test PIN. On success the
      status flips to COMPLETED **only after** the backend re-reads the
      transaction from the API; access/receipt update exactly once.
- [ ] **8. Webhook authenticity.** In the Stakaba dashboard trigger a sandbox
      test transaction and confirm a POST arrives at
      `https://<your-host>/v1/webhooks/stakaba` and a row lands in
      `webhook_events`. Stakaba documents **no signature**, so authenticity is
      established by the mandatory server-side API re-check, not by a header.
- [ ] **9. Status updates exactly once.** Replay the same event (or press
      *Poll* repeatedly): the audit log gains no second `Payment Verification`
      row and the entitlement expiry is not extended twice.
- [ ] **10. Access changes only on confirmation.** Before approval the student
      still lacks access; after a verified SUCCESS it gains it.
- [ ] **11. Failure grants nothing.** Repeat step 4 with payer
      `255000000001` → the simulated failure marks the payment FAILED, no
      entitlement, no receipt, and the UI offers a retry.
- [ ] **12. Forged webhook changes nothing.** `curl -X POST .../v1/webhooks/stakaba`
      with a made-up reference → `200 accepted=false`, no state change.
- [ ] **13. Logs are clean.** `grep -i "sk_" backend.log` finds no key; no
      response body or PIN is logged.
- [ ] **14. Stale payments surface on their own.** Leave one payment PENDING
      and unapproved; within 24h the hourly reconciliation sweep raises a
      `PENDING_UNRESOLVED` reconciliation exception (it only records — it never
      moves money or grants access).
- [ ] **15. No real money moved.** The key used was `sk_test_...`; the
      Stakaba dashboard shows the transaction under the **Test workspace**.

Quick negative checks:

```bash
# wrong amount must be refused, never fulfilled (expect 422 + audit entry)
curl -X POST http://localhost:8080/v1/webhooks/stakaba -H 'Content-Type: application/json' \
  -d '{"event":"transaction.success","internalReference":"M_REAL","grossAmount":1}'

# unknown reference is acknowledged, not fulfilled
curl -X POST http://localhost:8080/v1/webhooks/stakaba -H 'Content-Type: application/json' \
  -d '{"event":"transaction.success","internalReference":"M_DOES_NOT_EXIST"}'
```

---

## 4. Sandbox prerequisite: an ACTIVE pricing policy

Prices are never taken from the client. `V169` seeds the access passes and
tiers as **DRAFT**, and `PricingService` refuses to charge anything without an
ACTIVE policy (HTTP 409, surfaced in the UI as "Pricing is not configured…").

Before step 4 of the checklist: *Platform Admin → Pricing → activate* the
policy for the service type you want to buy (e.g. `MONTHLY_ACCESS`).

---

## 5. Behaviour worth knowing

- **A queued prompt is not a payment.** `201 Created` only means Stakaba
  queued the USSD push; the record stays `PENDING`.
- **The non-idempotent call is never blindly retried.** On an ambiguous
  timeout the payment is flagged `initiationAmbiguous` and the caller is told
  to check their phone first. Only the read-only status lookup is retried
  (3 attempts, 200/400 ms backoff) — permitted by the documented 500/502 rule.
- **Concurrency.** Verification takes `PESSIMISTIC_WRITE` on the payment row,
  so simultaneous webhooks and status polls serialise; the COMPLETED
  short-circuit is race-free. `V172` additionally makes one gateway reference
  bind to at most one live payment.
- **Every outcome is audited**: `Payment Initiation`, `Payment Failure` and the
  pre-existing `Payment Verification` (which also allocates commission, grants
  the entitlement and issues the receipt — unchanged).
- **Stale payments are swept hourly.** `ReconciliationService.scan()` was
  implemented and tested but never called; it is now scheduled hourly
  (`CoreScheduler.hourlyReconciliationScan`). It is a read-only pass that only
  *raises* reconciliation exceptions — it never moves money, never grants or
  revokes access and never changes a payment status.
- **`/v1/webhooks/payments` is untouched.** The legacy shared-secret webhook and
  `POST /v1/parents/payments/{id}/verify` still behave exactly as before.

---

## 6. Outstanding production blockers

1. **No webhook signature exists in the public docs.** Fulfilment is protected
   by mandatory server-side verification, which is the documented approach, but
   ask Stakaba about an IP allow-list, mTLS or a signed payload. Until then the
   endpoint should be firewalled to Stakaba's egress IPs.
2. **Transaction-status response schema is `map<string, any>`** in the OpenAPI
   spec — untyped, and *List Transactions* is untyped too, so it is not a
   one-off. Parsing is deliberately tolerant (enveloped `data`/`transaction`
   unwrapped, `amount` accepted as an alias, numeric strings parsed) and a
   claimed success with **no amount is refused rather than assumed**. Confirm the
   real shape against a sandbox response.
3. **Bearer vs `x-api-key`** conflict on the status endpoint (§1).
4. **Business/KYC approval, settlement destination and timing, fees and
   transaction limits** — all unconfirmed; `sk_live_` remains interlocked.
5. **Reconciliation is now scheduled hourly** (see §5) — it was previously
   manual. Confirm the chosen cadence and alerting for raised exceptions before
   go-live.
6. **`/dashboard/subscription` is a static page** whose hard-coded "TZS 25,000"
   disagrees with the seeded PREMIUM price (TZS 7,000), and its upgrade button
   still does nothing. It was left alone deliberately: wiring it belongs with
   the broader subscription-workflow decision, not this gateway change.
