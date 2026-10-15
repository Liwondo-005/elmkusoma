# Stakaba Mobile-Money Integration — Sandbox Testing Guide

**Status: sandbox only.** No live credentials, no live transactions, no production
enablement. Nothing in this document was executed against the real Stakaba API —
automated coverage is mocked. See "What has NOT been verified" at the end.

---

## 0. Quick start — first sandbox test (Windows, PowerShell)

> **Read §0.1 first.** Two prerequisites will stop you before you reach a payment
> prompt. Both are known and neither is a gateway fault.

### 0.0 What is already true

| Item | State |
|---|---|
| `.env` location | `backend\.env` (git-ignored, never committed) |
| Migrations V169–V172 | **applied** to dev DB `elmkusoma` (0 failures) |
| Pricing policies | seeded **DRAFT**, none ACTIVE — you must activate one |
| Gateway at startup | disabled until you set the variables below |

### 0.1 Two blockers to clear first

1. **No ACTIVE pricing policy.** V169 seeds 17 policies as DRAFT with no rate
   invented. Until one is ACTIVE, checkout returns HTTP 409 *"Pricing is not
   configured…"*. Requires a **Platform Admin** (§4).
2. **`/v1/my/children` resolves nothing.** The seeded `parent_student_links`
   rows store *user* ids in `student_id`, but that column is a *student* id, so
   the join matches 0 of 6 `students` rows and the checkout cannot offer a
   child. Pre-existing seed defect, unrelated to Stakaba. Needs a **DBA** to
   repair the seed data — see §8.

### 0.2 Create your `.env`

```powershell
cd C:\Users\dell\Desktop\elmkusoma1\elmkusoma\backend
Copy-Item .env.example .env          # keep the existing DB_*/JWT_* values
notepad .env                          # then append the block below
```

Append (keeping your own values for `DB_PASSWORD` and `JWT_SECRET`):

```dotenv
ELMKUSOMA_PAYMENT_PROVIDER=stakaba
STAKABA_ENABLED=true
STAKABA_BASE_URL=https://api.stakaba.com
STAKABA_API_KEY=sk_test_PASTE_YOUR_SANDBOX_KEY
STAKABA_ALLOW_LIVE=false
STAKABA_CONNECT_TIMEOUT_MS=5000
STAKABA_READ_TIMEOUT_MS=15000
STAKABA_SEND_BEARER_FALLBACK=true
```

Notes:

- Quotes are optional — `STAKABA_API_KEY="sk_test_..."` and
  `STAKABA_API_KEY=sk_test_...` behave identically. One layer of matching
  surrounding quotes is stripped on load.
- `STAKABA_ALLOW_LIVE=false` is the safety interlock. With a live
  (`sk_live_`) key and this left `false`, **the application refuses to start**.
  Do not set it `true` for sandbox work.
- Get the key from app.stakaba.com → Settings → API Keys → sandbox
  (`sk_test_…`). Never commit it, and never paste it into a chat or issue.

### 0.3 Confirm configuration without exposing the key

```powershell
cd C:\Users\dell\Desktop\elmkusoma1\elmkusoma\backend
.\start-backend.ps1 -VerifyConfig
```

Expected — note it prints **state, never values**:

```
  .env in use          : ...\elmkusoma\backend\.env
  DB_PASSWORD          : set
  provider             : stakaba
  STAKABA_ENABLED      : true
  STAKABA_BASE_URL     : https://api.stakaba.com
  STAKABA_API_KEY      : set (sandbox key sk_test_*)
  STAKABA_ALLOW_LIVE   : false

  Configuration parsed. Start the app with .\start-backend.ps1
```

If `STAKABA_API_KEY` shows `set (does not match sk_test_*/sk_live_*)` or
`not set`, the app will refuse to start with an actionable message. Nothing
here reveals the key.

### 0.4 Start the backend, then confirm it recognised the key

```powershell
cd C:\Users\dell\Desktop\elmkusoma1\elmkusoma\backend
.\start-backend.ps1 -CoreOnly
```

In the backend log you need exactly one line:

```
StakabaConfigurationValidator : Stakaba payment gateway enabled:
    baseUrl=https://api.stakaba.com keyMode=SANDBOX (value never logged)
```

`keyMode=SANDBOX` is the confirmation. If the log instead says
*"Stakaba payment gateway disabled"*, your variables did not load — re-check
the file name (`backend\.env`) and that `STAKABA_ENABLED=true`.

> **Migrations apply on startup.** `spring.flyway.enabled=true` with no
> target, so starting the backend migrates the configured database
> automatically. On this machine V169–V172 have already been applied to
> `elmkusoma`; starting again is a no-op. Point `DB_NAME` elsewhere first if
> you need to control this.

### 0.5 Start the frontend

```powershell
cd C:\Users\dell\Desktop\elmkusoma1\elmkusoma\frontend
npm run dev          # http://localhost:3000
```

### 0.6 Activate a pricing policy (Platform Admin)

Prices are never taken from the client, and no policy is ACTIVE out of the
box. Sign in as a Platform Admin, then:

- **UI:** Platform Admin → Pricing → activate the policy for the service you
  want to buy (e.g. `MONTHLY_ACCESS`).
- **Or API:** `POST /v1/platform-admin/pricing/policies/{policyId}/activate`
  with an optional `{"note":"..."}`, `ADMIN` role only, audited.

It refuses rather than adjusts — already ACTIVE, no version, no target, or a
service price without a default amount are all rejected.

### 0.7 Run a checkout

1. Sign in as a **Parent** who has at least one child. *(Blocked today — see
   §0.1 item 2 — until the seed data is repaired.)*
2. Parent → **Payments** → **New payment**.
3. Choose child, service, network (M-Pesa / Airtel Money / Mixx by Yas /
   HaloPesa) and a Tanzanian phone number.
4. **Send payment prompt**.

### 0.8 What you should observe

| Moment | Expected |
|---|---|
| Prompt queued | `201`; payment row `provider=stakaba` with an `internalReference`; status stays **PENDING** |
| Awaiting approval | UI shows *"Waiting for approval on the phone"* and polls — never "paid" |
| Approve the USSD prompt | Backend re-reads the transaction from Stakaba, then status → **COMPLETED**, entitlement granted once, receipt issued |
| Simulated failure | Payer `255000000001` → **FAILED**, no entitlement, no receipt, retry offered |
| Duplicate callback | Second webhook is a no-op — no second entitlement, expiry not extended |
| Ambiguous timeout | Flagged `initiationAmbiguous`; you are told to check the phone first, never blind-retried |

Full 15-step verification checklist: **§3**.

### 0.9 Stop safely and collect redacted logs

```powershell
# stop the backend (close the minimised windows, or:)
Get-Process -Name java | Where-Object { $_.StartTime -gt (Get-Date).AddHours(-2) } |
    Stop-Process
```

If something fails, collect diagnostics **without** the secret:

```powershell
Select-String -Path .\backend.log -Pattern 'Stakaba|dotenv|keyMode|reconcil' |
    Select-Object -Last 40
```

That prints `keyMode=SANDBOX`, never the key. Before sharing any log, confirm:

```powershell
Select-String -Path .\backend.log -Pattern 'sk_test_|sk_live_'   # must return nothing
```

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

**Known documentation conflicts (recorded, not guessed around).** Re-checked
against the machine-readable spec (`https://docs.stakaba.com/openapi.json`) on
2026-10-15:

1. **Auth on the status endpoint.** The OpenAPI spec declares
   `security: [HTTPBearer]` for `GET /api/v1/transactions/{transaction_id}` and
   exposes **no** `x-api-key` parameter for it — yet the Authentication,
   Environments and Mobile Money Collections guides all specify `x-api-key` for
   payment endpoints, and the *same spec* uses `x-api-key` (and no bearer) for
   `POST /api/v1/payments/collection`. Because both are documented, the status
   lookup sends **both** `x-api-key` and `Authorization: Bearer <api-key>`
   (disable with `STAKABA_SEND_BEARER_FALLBACK=false`). The collection endpoint
   sends only `x-api-key`, matching its spec entry.
2. **`transaction_id` is declared `format: uuid`** in the spec, while every
   documented example value and the returned `internalReference`
   (`M_X7Y8Z9W0`) is **not** a UUID. This integration passes the
   `internalReference` through verbatim as the path segment, matching the
   documented examples and the webhook payload. If a sandbox lookup 422s on a
   UUID-format complaint, this is the suspect.
3. Confirmed by the spec as implemented: `grossAmount` is an **integer** with
   `exclusiveMinimum: 0`, `currency` defaults to `TZS`, `network` is the enum
   `Mpesa | TigoPesa | AirtelMoney | HaloPesa`, collection success is **201**
   with `object<string,string>`, and `metadata` is a free-form object.

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

---

## 7. Verified local state (re-checked 2026-10-15)

Recorded so the next run does not have to rediscover it.

| Item | Observed |
|---|---|
| Sandbox credentials | **Absent** from `backend\.env`, process env and machine/user env |
| `.env` ignored | `git check-ignore -v backend/.env` → `.gitignore:9`; never tracked, never committed |
| Dev DB `elmkusoma` | **V172 applied** (V169–V172 at 11:24:55, `success=t`, 0 failures) — applied by an application start, which migrates automatically |
| `pricing_policies` | 17 rows, **all DRAFT**, zero ACTIVE; amounts 200–7,000 TZS |
| `commission_policies` | 14 rows, **all DRAFT**, `commission_rate` NULL on **every** row, `fee_bearer` NULL — no rate invented |
| `discount_rules` | 0 rows — none activated |
| `payments` | 0 rows — no payment history touched |
| Migrations on a fresh DB | V1→V172 replay cleanly (verified on a throwaway DB, since dropped) |
| Startup with sandbox config | `keyMode=SANDBOX` logged, Tomcat on 8080, `Started ElmkusomaCoreApplication` — **on a throwaway DB, sentinel key only** |
| `.env` loader | `[dotenv] Loading …\backend\.env`; loaded 0 properties when the shell already supplied them — precedence correct |
| `parent_student_links` | 2 rows, **0** match `students.id`, **2** match `users.id` — seed defect, blocks checkout (§8) |

**Activation workflow** (do not run without authorization): Platform Admin →
`POST /v1/platform-admin/pricing/policies/{policyId}/activate` with an optional
`{"note": "..."}`. `ADMIN` role only, audited with the acting user, and it
refuses rather than adjusts (already ACTIVE / no version / no target).

---

## 8. Seed defect blocking the first checkout

`parent_student_links.student_id` is a **student** id (the entity and every
query treat it as one), but the seeded rows store **user** ids. The result:

```
parent_student_links                     2 rows
  JOIN students ON students.id = ...      0 matches
  JOIN users    ON users.id    = ...      2 matches
```

`GET /v1/my/children` therefore returns an empty list, and the checkout cannot
offer a child, so the first sandbox payment cannot be started from the UI.

This is **pre-existing and unrelated to the Stakaba integration** — it was
present before any gateway code existed. Fixing it is a data decision, not a
gateway change: either re-point the links at the matching `students.id`, or
confirm that `student_id` was *meant* to hold user ids and correct the queries
instead. Both alter behaviour other features depend on
(`CertificateController` joins the same table), so this needs a DBA/owner
decision and must not be "fixed" as a side effect of payment work.

Until it is resolved, a sandbox collection can still be exercised
programmatically by creating the parent, student and link rows correctly — but
not through the parent checkout page.
