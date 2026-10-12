# ELMKUSOMA — Environment & Recording Storage Remediation

**Date:** 2026-10-11
**Repository:** `C:\Users\user\Desktop\elmkusoma`
**Follows:** `ELMKUSOMA_ENVIRONMENT_AND_RECORDING_STORAGE_AUDIT.md` (2026-10-11, findings preserved unchanged — see its new §10 addendum)
**Hand-off checklist:** `ELMKUSOMA_CREDENTIAL_SETUP_AND_FINAL_VERIFICATION.md`

> **Secret handling:** no credential, key, token, password or signed URL appears anywhere in
> this document. Credential state is reported as ABSENT / EMPTY / PRESENT only.

---

## 1. Git state

| | |
|---|---|
| Branch | `main` |
| Starting HEAD | `5c8daca0` (clean tree, ahead 0 / behind 0 vs `origin/main`) |
| Ending HEAD | `5c8daca0` — **unchanged** |
| Staged | nothing |
| Stash | empty |
| Commits / pushes / merges / rebases | **none performed** |
| Destructive commands (`reset --hard`, force-push, clean) | **none performed** |

The task brief described an *uncommitted* `V158 → V159` migration rename as a starting
assumption. That was stale: it is already committed as `5c8daca0`. It was treated as protected
and left exactly as found. **No Flyway migration was renamed, rewritten, added or deleted** in
this pass; versions 1–159 remain taken and the next available version is **V160**.

---

## 2. Files changed, and why

### Modified (4)

| File | Why |
|---|---|
| `backend/elmkusoma-core/.../ElmkusomaCoreApplication.java` | Loader made predictable, testable and value-free in its diagnostics (§3) |
| `backend/elmkusoma-core/.../resources/application.yml` | **`FRONTEND_URL` wiring defect** + `PAYMENT_WEBHOOK_SECRET` wiring (§4) |
| `backend/elmkusoma-core/.../resources/application-prod.yml` | Production overrides were inert due to YAML nesting (§5) |
| `backend/.env.example` | Credential-ready template for all categories (§6) |

### Added — production code (2)

| File | Why |
|---|---|
| `.../config/FrontendUrlValidator.java` | Startup validation of the recovery-link base URL |
| `.../liveclass/config/LiveKitEgressConfigValidator.java` | Redacted startup diagnostic for half-configured recording |

### Added — tests (5 classes, 44 tests)

`DotEnvLoaderTest` (14), `FrontendUrlValidatorTest` (12), `FrontendUrlBindingTest` (5),
`PaymentWebhookSecretWiringTest` (2), `LiveKitEgressConfigValidatorTest` (11).

### Added — reports (2) + amended (1)

`ELMKUSOMA_CREDENTIAL_SETUP_AND_FINAL_VERIFICATION.md`, this file, and a dated §10 addendum
appended to the original audit (its findings were **not** edited or removed).

### Not touched

`LiveRecordingPublisher`, `LiveKitService`, `LiveKitWebhookController`, `LiveClassServiceImpl`,
`LiveClassRepository`, `LiveRecordingMediaPublisher`, every migration, and all security
configuration. Webhook signature verification and authorization rules are byte-identical to
the starting tree.

---

## 3. Environment loading — as implemented and as tested

**Mechanism (unchanged in shape, hardened in detail):** `ElmkusomaCoreApplication.main()`
calls `loadDotenv()` *before* `SpringApplication.run`. The file parser writes **system
properties**, never environment variables.

**Precedence (highest first):**

1. real environment variable
2. already-set system property
3. value from `backend/.env`
4. default in `application.yml`

The guard `env == null && prop == null` means the file only fills gaps.

**Changes made:**

- **File discovery is now bounded.** `./.env` is always accepted. `../.env` is accepted **only
  when that parent directory contains `.env.example`** — the marker for "this is the backend
  root". Previously an arbitrary parent-directory `.env` could be loaded if the working
  directory happened to sit one level below it. No directory further up is ever searched.
- **Diagnostics print names, never values:** file path, count, and the key names applied.
- **Parsing/precedence extracted** into injectable, package-visible methods so tests can drive
  them with sentinel values in an isolated temp directory.

**Module ownership (verified, not assumed):**

| Module | reads `backend/.env` itself |
|---|---|
| `elmkusoma-core` | **yes** |
| `elmkusoma-media` / `-workers` / `-realtime` | **no** |

The other three receive ambient environment variables injected by `start-backend.ps1`,
`start-dev.ps1`, or the compose `environment:` blocks. This is now documented in the template
header rather than left implicit.

**Decision — the loader was *not* duplicated.** The supported launch model already delivers
config to every module; a second hand-rolled parser in three modules would have created a
second precedence implementation to keep in sync. Documenting and testing the single mechanism
is the smaller correct change.

---

## 4. `FRONTEND_URL` — the defect, fixed and tested

**Defect (audit finding C, confirmed against the current tree):** `.env.example:123`
advertised `FRONTEND_URL`, but no code or YAML consumed that name. Consumers read
`app.frontend-url` (`AuthServiceImpl:69`, `PlatformAdminService:131`). Setting the documented
variable changed nothing and every recovery email carried `http://localhost:3000`.

**Fix:**

```yaml
app:
  frontend-url: ${FRONTEND_URL:${APP_FRONTEND_URL:http://localhost:3000}}
```

Both names now work; the documented one takes precedence; the local default is unchanged.

**Also added — `FrontendUrlValidator`** (fails startup with a clear message):

| Rule | Scope |
|---|---|
| must be non-blank | always |
| must be absolute `http(s)` | always |
| must not end with `/` (links are built `<base>/reset-password`, so `/` would double) | always |
| must have a host, and no query/fragment | always |
| must **not** be `localhost` / `127.0.0.1` / `0.0.0.0` / `::1` / `*.localhost` | **`prod` only** |

Local development keeps `http://localhost:3000`; production can no longer silently emit it.

**Routes verified:** `frontend/app/reset-password/page.tsx`,
`frontend/app/verify-email/page.tsx`, `frontend/app/login/page.tsx` all exist, take `token`,
and are not intercepted by `frontend/proxy.ts` (which only matches `/dashboard/*` and
`/oversight/*`). No new route was invented.

**Same defect class, also fixed:** `payment.webhook.secret` was only reachable through relaxed
environment binding, so a value written solely in `backend/.env` was silently ignored,
leaving `PaymentWebhookController` permanently fail-closed with no indication why. An explicit
`payment.webhook.secret: ${PAYMENT_WEBHOOK_SECRET:}` mapping was added. **Empty still rejects
every webhook — verification was not weakened.**

---

## 5. Production overrides were inert

`application-prod.yml` had `flyway:`, `data:` and `rabbitmq:` indented at column 2 following a
top-level `elmkusoma:` key, so Spring bound them as `elmkusoma.flyway.*`, `elmkusoma.data.*`,
`elmkusoma.rabbitmq.*` — names nothing reads. Consequences of the defect:

- prod kept the **dev** Redis/RabbitMQ hosts (`localhost`);
- `spring.flyway.placeholders.demoSeedDeactivate: "true"` never reached Flyway, so **V142
  never deactivated the seeded demo accounts** whose password hashes are published in the
  repository.

**Fix:** nesting corrected so the blocks sit under `spring:`. **No value was altered** — every
default is byte-identical to before.

**Risk assessment recorded before making the change:**

- The `prod` profile is **not activated anywhere in the repository** (no
  `SPRING_PROFILES_ACTIVE=prod`, no compose entry, no launcher flag), so nothing currently runs
  these values.
- `V142` is already applied on existing databases; Flyway does not re-run it. The placeholder
  therefore only affects a **fresh** production database, where deactivating demo logins is the
  documented intent.
- First activation of `prod` should be reviewed deliberately rather than inheriting dev
  defaults. Flagged in the hand-off checklist §12.4.

---

## 6. Configuration contract — names and real consumers

Full variable-by-variable detail (purpose, required/optional, consuming module, format,
secret flag, restart, status) is in `backend/.env.example`. Summary of the audit's P0–P3:

| Finding | Status |
|---|---|
| P0-1/2 B2 `LIVEKIT_EGRESS_S3_ACCESS_KEY` / `_SECRET` empty | **unchanged — developer must supply** (§9) |
| P0-3 `LIVEKIT_EGRESS_ENABLED=false` | **unchanged — deliberately left off** |
| P1-4 `MFA_ENCRYPTION_KEY` absent | documented + generation instructions; **value still developer-supplied** |
| P1-5 `FRONTEND_URL` not wired | **FIXED** + validated + tested |
| P1-6 `.env` loaded by core only | **documented**; loader bounded and tested |
| P2-7 SMTP unset | documented (both consumers, app-password requirement) |
| P2-8 MinIO dev defaults | documented as local-only |
| P2-9 no production guard against localhost links | **FIXED** (`FrontendUrlValidator`, prod profile) |
| P3-10 template drift | **FIXED** — template rewritten around real consumers |

New findings raised in this pass:

- **`application-prod.yml` nesting** (§5) — fixed.
- **`docker-compose.yml` carries committed development credentials** (shared `JWT_SECRET`,
  DB password, MinIO root credentials, RabbitMQ `guest`) for the local stack. *Reported, not
  changed*: replacing them would invalidate existing local tokens and is a deployment decision,
  not a code fix. **Must be replaced from a secret store before any shared/production use.**
- **Unsupported `MEDIA_STORAGE` values fail opaquely.** Supported modes are exactly `minio`
  and `local`. Any other value yields no `StorageService` bean and the media service fails to
  start with a generic `NoSuchBeanDefinitionException`. *Left untouched*: a validator in the
  media module would have to run before bean wiring fails, which is not reliably achievable
  without restructuring bean creation — out of proportion for a message-only improvement.
  Exact next action: validate `media.storage` in a `BeanFactoryPostProcessor` if the clearer
  diagnostic is wanted.

---

## 7. Recording lifecycle — verified as still intact

Reinspected, not modified. All of the following remain as previously delivered:

- completion via **both** the session-end path and the `egress_ended` webhook;
- publication serialised by `LiveClassRepository.findByIdForUpdate` (`PESSIMISTIC_WRITE`), live
  class row always locked first;
- transaction propagation **REQUIRED, not REQUIRES_NEW** (REQUIRES_NEW re-acquires the held row
  and self-deadlocks);
- notification/audit side effects in isolated `REQUIRES_NEW` transactions;
- `toPlayableRecordingUrl` passes `http(s)` through, rebuilds anything else **from the
  configured** endpoint+bucket (never from the webhook payload — anti-SSRF), and returns `null`
  rather than fabricating a URL;
- `hasStorage()` gate at job start, so no doomed egress job is created;
- private bucket; playback access control unchanged.

**Recording configuration validation added** (`LiveKitEgressConfigValidator`):

- runs **only** when `livekit.egress.enabled=true`; silent otherwise, so dev/test boot with no
  B2 credentials at all;
- names which of `LIVEKIT_EGRESS_BUCKET`, `_S3_ACCESS_KEY`, `_S3_SECRET`,
  `_S3_REGION`/`_S3_ENDPOINT`, `LIVEKIT_API_KEY`/`_SECRET` are missing, and flags a
  non-absolute endpoint;
- **logs, does not abort** — recording storage is optional, and failing startup would take
  down unrelated features. The request-time refusal (503) remains the enforcement boundary;
- makes **no network call** to B2;
- `LiveKitEgressConfigValidatorTest.diagnostics_neverEchoCredentialValues` pins that no
  credential value can appear in the message.

**Deliberately not changed:** the 22.5 s resolve poll inside session completion's transaction.
Moving it would require re-acquiring the live-class row the caller already holds → self-deadlock.

---

## 8. Tests and evidence

### Commands and actual results

| Command | Working dir | Result |
|---|---|---|
| `mvn -o -q -DskipTests compile` | `backend/elmkusoma-core` | exit **0** |
| `mvn -o test "-Dtest=DotEnvLoaderTest,FrontendUrlValidatorTest,FrontendUrlBindingTest,LiveKitEgressConfigValidatorTest" "-DfailIfNoSpecifiedTests=false"` | `backend/elmkusoma-core` | exit **0**, **42 tests, 0 failures, 0 errors** |
| `mvn -o test` (final, full suite) | `backend/elmkusoma-core` | exit **0**, **`Tests run: 1421, Failures: 0, Errors: 0, Skipped: 5` — `BUILD SUCCESS`** |
| `npx tsc --noEmit` | `frontend` | exit **0** (no output) |

Baseline before this pass: **1377 tests, 0 failures, 0 errors, 5 skipped**.
Final: **1421** = baseline **+44** new tests. **Skipped count unchanged at 5** — nothing was
skipped, weakened or deleted to obtain the build.

### The 5 skipped tests (pre-existing, credential-gated)

| Class | Skipped |
|---|---|
| `tz.elmkusoma.integration.EventD03ComplianceTest` | 1 |
| `tz.elmkusoma.integration.LiveKitIntegrationTest` | 1 |
| `tz.elmkusoma.integration.LiveKitRealServerTest` | 3 |

All are integration tests that self-disable without genuinely configured LiveKit/B2
credentials. They are the reason no runtime recording evidence exists (§10).

### New tests

| Class | Tests | What it proves |
|---|---|---|
| `DotEnvLoaderTest` | 14 | `./.env` preferred; `../.env` accepted only with the `.env.example` marker; **a stray parent-directory `.env` is refused**; env var and existing property both win over the file; comments/malformed/`=`-in-value parsing; result carries names only |
| `FrontendUrlBindingTest` | 5 | loads the **real** `application.yml` and resolves `app.frontend-url` — `FRONTEND_URL` and `APP_FRONTEND_URL` both reach it, documented name wins, default preserved |
| `FrontendUrlValidatorTest` | 12 | every rejection rule incl. prod-only loopback rejection |
| `PaymentWebhookSecretWiringTest` | 2 | `PAYMENT_WEBHOOK_SECRET` reaches the property; default stays empty (fail-closed preserved) |
| `LiveKitEgressConfigValidatorTest` | 11 | precise missing-setting names; **credential values never echoed**; incomplete config never aborts startup |

---

## 9. Configuration security checks performed

| Check | Result |
|---|---|
| `.env` ignored by Git | ✅ `git check-ignore -v backend/.env` → `.gitignore:9` |
| `.env` tracked? | ✅ not tracked (`git ls-files --error-unmatch` fails) |
| `.env`-like files tracked | only `backend/.env.example` (template, placeholder values) |
| AWS key / B2 key / private key / `sk-`/`ghp_`/`xox*` patterns in tracked files | **none** |
| `NEXT_PUBLIC_*` carrying secret-looking literals | **none**; frontend has no `.env*` file at all |
| Frontend holds LiveKit / B2 / JWT / MFA material | **no** — LiveKit tokens are minted server-side, presigned uploads are server-mediated, the TOTP secret arrives at runtime |
| `System.out/err` printing config in main code | only the `.env` loader: path, count, **key names** — never values |
| logging that echoes secret/password/token in main code | **none** |
| Webhook signature verification | unchanged; both webhooks fail closed and log no secret |
| Bucket privacy / playback access control | unchanged — no authorization rule was relaxed |
| Startup validation added | fails on invalid frontend URL; egress problems **reported** not fatal |

---

## 10. Verification status — honest separation of evidence

| Claim | Evidence type | Status |
|---|---|---|
| `.env` loader location + precedence rules | **automated tests** (14) | **VERIFIED** |
| `FRONTEND_URL` reaches `app.frontend-url` | **automated tests** (5) | **VERIFIED** |
| Frontend URL validation incl. prod loopback rejection | **automated tests** (12) | **VERIFIED** |
| Egress redaction; no startup abort | **automated tests** (11) | **VERIFIED** |
| Payment webhook secret wiring; fail-closed default | **automated tests** (2) | **VERIFIED** |
| Full backend suite green; no test weakened | **automated** | **VERIFIED** (1421/0/0/5) |
| Frontend type-check clean | **automated** | **VERIFIED** (`tsc --noEmit`, exit 0) |
| Config variable → consumer mapping | **code inspection** | **VERIFIED** (§6, `application*.yml`) |
| Recovery routes exist and match built links | **code inspection** | **VERIFIED** |
| Recording lifecycle/idempotency intact | **code inspection + pre-existing tests** | **VERIFIED** |
| No secret committed; `.env` ignored | **scans** (§9) | **VERIFIED** |
| **B2 credentials valid** | — | ❌ **UNVERIFIED — no credential supplied** |
| **Bucket reachable / path-style accepted** | — | ❌ **UNVERIFIED — no network call made** |
| **A recording actually uploads to B2** | — | ❌ **UNVERIFIED — never executed** |
| **`file_results[].location` scheme** | — | ❌ **UNVERIFIED — never observed** |
| **Replay published from a real `egress_ended`** | — | ❌ **UNVERIFIED at runtime** (logic covered by unit/concurrency tests only) |
| **Playback of a real recording, authorized vs not** | — | ❌ **UNVERIFIED — depends on the above** |

**A real B2 upload was NOT attempted.** No credentials exist, recording is disabled, and no
authorisation to perform a recording was given. Nothing in this report should be read as B2
working.

---

## 11. Remaining manual tasks (developer)

Exact steps, formats and presence-only checks:
`ELMKUSOMA_CREDENTIAL_SETUP_AND_FINAL_VERIFICATION.md`.

1. `LIVEKIT_EGRESS_S3_ACCESS_KEY` ← B2 bucket-scoped application key ID
2. `LIVEKIT_EGRESS_S3_SECRET` ← that key's secret
3. `LIVEKIT_EGRESS_ENABLED=true` — only *after* §11.1 of that document is clean
4. `MFA_ENCRYPTION_KEY` ← `openssl rand -base64 32` (required for MFA)
5. `FRONTEND_URL` ← public frontend origin, no trailing slash (required for production)
6. `SPRING_MAIL_*` ← provider + **app password**, set for **both** core and workers
7. `MEDIA_STORAGE` / `MINIO_*` ← only for the media service's environment
8. `ELM_BOOTSTRAP_ADMIN` + `ADMIN_BOOTSTRAP_PASSWORD` ← one-time, fresh database only
9. `PAYMENT_WEBHOOK_SECRET` ← only if payments are used (empty = fail-closed)
10. Replace the committed credentials in `docker-compose.yml` from a secret store before any
    shared or production deployment

Then run the first-recording procedure (§10 of the checklist) and **record the actual
`location` value** — that observation decides the playback model.

---

## 12. Unresolved risks and why they could not be closed

1. **No B2 integration evidence.** Requires credentials that do not exist and an authorised
   test recording. Cannot be closed by code or tests.
2. **Playback entitlement on the byte fetch is not demonstrated.** Replay/recording URLs are
   consumed by the frontend **directly** (`<a href>`, `<video src>`, `window.open` — e.g.
   `frontend/app/dashboard/learner/replays/[id]/page.tsx:283`,
   `frontend/app/dashboard/live-classes/page.tsx:293`). `MediaProxyService` serves
   *media-library* resources, not live-class recordings. Listing endpoints **are**
   `@PreAuthorize`-gated, so an unauthorized user never learns a URL — but that is URL secrecy,
   not a per-request entitlement check. Which outcome applies depends on whether LiveKit
   returns a plain private-bucket URL (browser → 403, fail-closed) or a signed URL (plays until
   expiry). **Not resolvable without the observation above.** Deliberately *not* "fixed" by
   adding a proxy: building one against an unobserved location scheme would be guesswork, and
   making the bucket public is explicitly forbidden.
   *Note: the original template text claimed playback already goes through
   authorization-checked media endpoints. Code inspection does not support that for live-class
   recordings, so the text was corrected rather than repeated.*
3. **Committed credentials in `docker-compose.yml`.** Changing them invalidates local tokens
   and is a deployment decision; reported instead of silently altered.
4. **`prod` profile never activated.** Its corrected overrides have therefore never executed.
   The first activation should be reviewed deliberately.
5. **Unsupported `MEDIA_STORAGE` diagnostic is opaque.** Reported with an exact next action;
   fixing it safely requires bean-creation restructuring disproportionate to a message.
6. **Two `media_assets` writers remain** (`LiveRecordingMediaPublisher` for session
   completion, `LiveRecordingPublisher` for the webhook). Both are idempotent and backstopped
   by V150; metadata verified equivalent. Consolidation deferred — it is a refactor, not an
   environment fix.

---

## 13. Bottom line

Every safe, evidence-supported change that could be made **without credentials** has been made
and is covered by 44 new tests on a green 1421-test suite.

What remains is **manual credential entry plus one authorised test recording** — not code.
Until that recording runs, B2 upload, the `location` scheme, and playback behaviour are
**UNVERIFIED**, and this feature must not be described as complete.

*No credential appears in this document.*
