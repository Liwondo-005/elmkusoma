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
| HEAD | `f43187af` — **unchanged across both passes** |
| Staged | nothing |
| Tracked modifications + new files | 25 (listed in §2) — **left uncommitted** |
| Stash | empty |
| Commits / pushes / merges / rebases | **none performed** |
| Destructive commands (`reset --hard`, force-push, clean) | **none performed** |

**No Flyway migration was added, renamed, rewritten or deleted in either pass.** The migration
directory holds 155 files with a highest version of **V160**; the gaps in the numbering
(V128–V130, V140–V141) predate this work and were left exactly as found. The next available
version is therefore **V161**, and Flyway validation is untouched.

---

## 2. Files changed, and why

This document covers **two passes**. Pass 1 was the environment/credential remediation; pass 2
(§10) closed the recording **playback authorization** gap. Both are listed so the tree can be
reviewed against a single account of itself.

### Pass 1 — modified (4)

| File | Why |
|---|---|
| `backend/elmkusoma-core/.../ElmkusomaCoreApplication.java` | Loader made predictable, testable and value-free in its diagnostics (§3) |
| `backend/elmkusoma-core/.../resources/application.yml` | **`FRONTEND_URL` wiring defect** + `PAYMENT_WEBHOOK_SECRET` wiring (§4) |
| `backend/elmkusoma-core/.../resources/application-prod.yml` | Production overrides were inert due to YAML nesting (§5) |
| `backend/.env.example` | Credential-ready template for all categories (§6) |

### Pass 1 — added, production code (2)

| File | Why |
|---|---|
| `.../config/FrontendUrlValidator.java` | Startup validation of the recovery-link base URL |
| `.../liveclass/config/LiveKitEgressConfigValidator.java` | Redacted startup diagnostic for half-configured recording |

### Pass 1 — added, tests (5 classes, 44 tests)

`DotEnvLoaderTest` (14), `FrontendUrlValidatorTest` (12), `FrontendUrlBindingTest` (5),
`PaymentWebhookSecretWiringTest` (2), `LiveKitEgressConfigValidatorTest` (11).

### Pass 2 — modified (12)

| File | Why |
|---|---|
| `.../config/security/JwtAuthenticationFilter.java` | Cookie fallback for playback paths only |
| `.../config/security/JwtRequestAttributeFilter.java` | Same, so request attributes resolve for `<video src>` |
| `.../liveclass/controller/LiveSessionController.java` | Recording-download URL now points at the playback endpoint; new byte-serving endpoint reusing the same authorisation |
| `.../event/controller/ReplayController.java` | New byte-serving endpoint; replay `videoUrl` is now a playback URL |
| `.../event/controller/LearnerReplayController.java` | Entitlement delegated to the shared service; `videoUrl` is now a playback URL |
| `.../course/service/LiveClassServiceImpl.java` | `LiveClassResponse.recordingUrl` is now a playback URL |
| `.../learner/controller/LearnerController.java` | Same, for the learner live-class detail and summary |
| `.../event/domain/Replay.java` | `recordingUrl` is write-only in JSON (server state) |
| `.../course/domain/LiveClass.java` | `recordingUrl` is write-only in JSON (server state) |
| `.../resources/application.yml` | Two recording-playback timeouts, documented |
| `backend/.env.example` | Playback authorisation contract documented |
| `.../security/LiveClassGateHttpSecurityTest.java`, `.../integration/EventD03ComplianceTest.java` | Two assertions pinned the old contract (raw storage URL returned); retargeted to the playback URL |

### Pass 2 — added, production code (6)

| File | Why |
|---|---|
| `.../liveclass/service/RecordingStorageLocation.java` | The trust boundary: persisted location → the one URL this process may fetch |
| `.../liveclass/service/RecordingPlaybackService.java` | Range-capable, redirect-free, timeout-bounded streaming proxy |
| `.../liveclass/service/RecordingPlaybackUrls.java` | Builds the application-owned playback URLs clients receive |
| `.../liveclass/service/LiveClassRecordingAccessService.java` | The class-ownership rule, shared by both recording endpoints |
| `.../event/service/ReplayEntitlementService.java` | The single definition of who may open a replay |
| `.../config/security/RequestTokens.java` | Header-first token extraction with a GET/HEAD-only, playback-path-only cookie fallback |

### Pass 2 — added, tests (4 classes, 58 tests)

`RecordingStorageLocationTest` (23), `RecordingPlaybackAuthorizationTest` (16),
`RecordingPlaybackServiceTest` (9), `RequestTokensTest` (15) — §10.5.

### Reports

`ELMKUSOMA_CREDENTIAL_SETUP_AND_FINAL_VERIFICATION.md`, this file, and a dated §10 addendum
appended to the original audit (its findings were **not** edited or removed).

### Deliberately not touched

Webhook signature verification, `LiveKitWebhookController`, `LiveRecordingPublisher`,
`LiveClassRepository`, `LiveRecordingMediaPublisher`, every migration, and all Spring Security
configuration apart from the two filters' token *extraction* helper. No authorization rule was
weakened, and no `@PreAuthorize` was changed. The media library (`MediaLibraryController`,
`media_assets`) was left alone by design — see §13.7.

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
| `mvn -o -q -DskipTests compile` (pass 1) | `backend/elmkusoma-core` | exit **0** |
| `mvn -o test "-Dtest=DotEnvLoaderTest,FrontendUrlValidatorTest,FrontendUrlBindingTest,LiveKitEgressConfigValidatorTest" "-DfailIfNoSpecifiedTests=false"` | `backend/elmkusoma-core` | exit **0**, **42 tests, 0 failures, 0 errors** |
| `mvn -o test` (pass 1, full suite) | `backend/elmkusoma-core` | exit **0**, **`Tests run: 1421, Failures: 0, Errors: 0, Skipped: 5` — `BUILD SUCCESS`** |
| `mvn -f backend\elmkusoma-core\pom.xml -q compile -DskipTests` (pass 2) | repo root | exit **0** |
| `mvn -f backend\elmkusoma-core\pom.xml test "-Dtest=RecordingStorageLocationTest,RecordingPlaybackServiceTest" "-DfailIfNoSpecifiedTests=false"` | repo root | exit **0**, **32 tests, 0 failures, 0 errors** |
| `mvn -f backend\elmkusoma-core\pom.xml test "-Dtest=RecordingPlaybackAuthorizationTest" "-DfailIfNoSpecifiedTests=false"` | repo root | exit **0**, **16 tests, 0 failures, 0 errors** |
| `mvn -f backend\elmkusoma-core\pom.xml test` (pass 2, full suite) | repo root | exit **0**, **`Tests run: 1481, Failures: 0, Errors: 0, Skipped: 5` — `BUILD SUCCESS`** |
| `mvn -f backend\elmkusoma-core\pom.xml test "-Dtest=LiveClassRecordingMediaLibraryIT,MediaLibraryRecordingAuthorizationIT,ReplayChapterAuthorizationIT,MediaLibraryIT" "-DfailIfNoSpecifiedTests=false"` | repo root | **`ReplayChapterAuthorizationIT` 9/0/0 ✅**; **`LiveClassRecordingMediaLibraryIT` 6 tests, 2 failures, 1 error ❌ — pre-existing, see §13.9** |
| `git worktree add <tmp> HEAD --detach` + same IT run, in the clean baseline | repo root | **identical 6/2/1** — proves §13.9 predates this work |
| `npx tsc --noEmit` | `frontend` | exit **0** (no output) |

Baseline before pass 1: **1377 tests, 0 failures, 0 errors, 5 skipped**.
After pass 1: **1421** (+44). After pass 2: **1481** (+58 new test methods in four new classes,
plus 2 new assertions added to two existing security tests).
**The skipped count is unchanged at 5** — nothing was skipped, weakened or deleted to obtain
either build.

Two existing assertions were *retargeted* rather than removed: `LiveClassGateHttpSecurityTest`
and `EventD03ComplianceTest` both asserted that `recording/download` returns the raw
`https://cdn…` storage URL. That was precisely the contract this pass removes, so they now
assert the playback URL, and each gained a companion assertion that the storage host never
appears in the response at all.

### The 5 skipped tests (pre-existing, credential-gated)

| Class | Skipped |
|---|---|
| `tz.elmkusoma.integration.EventD03ComplianceTest` | 1 |
| `tz.elmkusoma.integration.LiveKitIntegrationTest` | 1 |
| `tz.elmkusoma.integration.LiveKitRealServerTest` | 3 |

All are integration tests that self-disable without genuinely configured LiveKit/B2
credentials. They are the reason no runtime recording evidence exists (§11).

### New tests — pass 1

| Class | Tests | What it proves |
|---|---|---|
| `DotEnvLoaderTest` | 14 | `./.env` preferred; `../.env` accepted only with the `.env.example` marker; **a stray parent-directory `.env` is refused**; env var and existing property both win over the file; comments/malformed/`=`-in-value parsing; result carries names only |
| `FrontendUrlBindingTest` | 5 | loads the **real** `application.yml` and resolves `app.frontend-url` — `FRONTEND_URL` and `APP_FRONTEND_URL` both reach it, documented name wins, default preserved |
| `FrontendUrlValidatorTest` | 12 | every rejection rule incl. prod-only loopback rejection |
| `PaymentWebhookSecretWiringTest` | 2 | `PAYMENT_WEBHOOK_SECRET` reaches the property; default stays empty (fail-closed preserved) |
| `LiveKitEgressConfigValidatorTest` | 11 | precise missing-setting names; **credential values never echoed**; incomplete config never aborts startup |

### New tests — pass 2 (playback authorization)

| Class | Tests | What it proves |
|---|---|---|
| `RecordingStorageLocationTest` | 23 | every rejection rule of the trust boundary: foreign host, cloud-metadata address, non-default port, scheme downgrade, embedded credentials, fragment, foreign bucket (`s3://` and http), `..` traversal, leading slash, non-http schemes, control characters, overlong input, unparseable endpoint; and that nothing is fabricated when no endpoint is configured |
| `RecordingPlaybackAuthorizationTest` | 16 | end-to-end through MockMvc against a **real local HTTP server**: entitled student streams real bytes; Range/206 seeking; unauthenticated 403 with an empty body; non-participant 403; a copied URL grants nothing; cross-class and cross-institution denied; cookie authenticates playback and is refused elsewhere; a stored location pointing at another storage host is never fetched (hit counter); redirect neither followed nor replayed; JSON never contains host/bucket/key |
| `RecordingPlaybackServiceTest` | 9 | the byte path against a real server: full fetch, `bytes=a-b`, open-ended and suffix ranges, malformed and multi-range dropped, upstream error → empty result, redirect → empty result, untrusted location never resolved |
| `RequestTokensTest` | 10 | the cookie fallback's refusal surface: header always wins; accepted only on the two playback prefixes, only for GET/HEAD; refused on every other path, every other verb, and when no credentials exist |

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

## 10. Recording playback authorization (second pass)

### 10.1 What was actually wrong

Not "the bucket is public" — the private bucket meant playback usually failed closed. The
defect was structural, and it had two halves:

1. **No authorization on the byte path.** Every recording endpoint checked entitlement only
   when producing *metadata*. The browser then fetched the file itself, from object storage,
   with nothing but the URL. Once a storage URL had been seen it could be copied, shared and
   replayed forever, and no part of the application was consulted again.
2. **If LiveKit ever returns a signed `location`,** `toPlayableRecordingUrl` passes http(s)
   through unchanged, so the application would persist a bearer capability in
   `live_classes.recording_url`, `replay.recording_url` and `media_assets.file_url` and then
   distribute it to anyone who could read a JSON response.

Which half bit first depended on an observation nobody had made: whether LiveKit's
`file_results[].location` is a plain private URL or a signed one. **That observation is still
missing** (no B2 credentials, recording disabled). Rather than guess again, both halves were
closed in a way that is correct under either outcome.

### 10.2 Why the obvious alternatives were rejected

| Option | Why not |
|---|---|
| Make the bucket public | Explicitly forbidden; every object becomes world-readable. |
| Treat URL secrecy as protection | That is what was already there. `@PreAuthorize` on a *listing* endpoint is not protection for a *byte* endpoint. |
| Short-lived signed URLs | Sound, and explicitly permitted — but a `<video>` issues Range requests for the whole lesson, so the signature either expires mid-watch or has to live for hours. It also leaves a bearer URL in the client for that window. |
| Cookie-authenticated playback, globally | Would change the authentication transport of every state-changing endpoint. Unnecessary blast radius. |

### 10.3 What was built

Clients are now given an **application-owned, same-origin playback URL** instead of a storage
URL, and that endpoint authorises **every request**:

```
GET /v1/replays/{replayId}/playback
GET /v1/live-session/classes/{classId}/recording/playback
```

Both re-check, on every request including every Range request a player makes while seeking:
authentication → institution scope (unknown id and foreign id both answer 404) → the role and
class-ownership rule →, for learners, session participation. The role/ownership rule was moved
into `LiveClassRecordingAccessService` and the participation rule into
`ReplayEntitlementService` so the new endpoints and the pre-existing ones share **one**
implementation each; two copies of an entitlement rule is exactly how one drifts while the
other still looks correct.

Because `recordingUrl`/`videoUrl` in API responses now carry the playback URL, **no frontend
change was required** — the ~15 existing consumers (`<video src>`, `<a target=_blank>`,
`window.open`) keep working, and the two `startsWith("http")` gates
(`frontend/components/live/live-video-player.tsx:193`,
`LiveSessionController` recording-status check) still pass because the playback URL is
absolute.

### 10.4 The trust boundary

`RecordingStorageLocation` is the line between "a string we persisted from an external
service" and "a URL this process will make an outbound request to". It fails closed on:
a host that is not the configured egress endpoint (SSRF to `169.254.169.254`, internal
services, another tenant); a port or scheme that differs; an embedded `user:password@`; a
fragment; a path outside `/<configuredBucket>/`; an `s3://` URI naming another bucket; `..`
segments; control characters and whitespace in a key; and an endpoint that is not a parseable
absolute http(s) URL. An http(s) query string *is* forwarded, because S3-compatible providers
put signature material there and dropping it would make a valid recording unfetchable — but it
is only ever sent back to the same allow-listed host and is never returned to a client, logged,
or persisted.

`RecordingPlaybackService` then proxies with redirects **disabled** (a 3xx from storage is
treated as a failure, never replayed — otherwise an authenticated endpoint becomes an open
redirect), explicit connect/read timeouts, no buffering (the upstream stream is piped straight
through, so an hour-long lesson costs fixed memory), `Accept-Encoding: identity` so Range
offsets stay meaningful, and `Cache-Control: private, no-store`. Nothing is logged that could
contain an object key, a signed query, or an upstream message.

### 10.5 Authentication for `<video src>`, without widening authentication

A browser attaches no `Authorization` header to a media element. `RequestTokens` therefore
lets the two JWT filters fall back to the access-token cookie the frontend **already** writes
(`elmkusoma_access_token`, `path=/`, `SameSite=Lax`) — but only when **all** of the following
hold: the request is **GET or HEAD**; the path starts with `/v1/replays/` or
`/v1/live-session/classes/`; and the path ends with `/playback`. It is the same token,
validated by the same `JwtTokenProvider`, with the same signature, expiry and security-version
checks. No other endpoint accepts it, so nothing about the CSRF posture of the rest of the API
changes; `RequestTokensTest` pins that refusal explicitly.

### 10.6 Evidence

| Claim | Evidence type | Status |
|---|---|---|
| Persisted location → safe fetch URL (SSRF, bucket, traversal, scheme, credentials, fragments) | **automated** (23) | **VERIFIED** |
| Entitled student streams real bytes; Range/206 seeking works | **automated** (real local HTTP server, not a mock) | **VERIFIED** |
| Unauthenticated → 403 with an empty body; non-participant → 403 | **automated** | **VERIFIED** |
| A copied playback URL grants nothing without the caller's own session | **automated** | **VERIFIED** |
| Cross-class and cross-institution ids denied; unknown and foreign are indistinguishable | **automated** | **VERIFIED** |
| Cookie authenticates playback, and is refused on every non-playback path and every non-read verb | **automated** | **VERIFIED** |
| A stored location pointing at another storage host is never fetched (hit counter stays at zero) | **automated** | **VERIFIED** |
| Redirects are neither followed nor replayed to the client | **automated** | **VERIFIED** |
| Replay/live-class JSON never contains the storage host, bucket or object key | **automated** | **VERIFIED** |
| Full backend suite green; no test weakened (two retargeted off the old, insecure contract) | **automated** | **VERIFIED** — 1481/0/0/5, §11 |
| Frontend type-check clean (no frontend change was needed) | **automated** | **VERIFIED** (`tsc --noEmit`, exit 0) |
| **A real B2-hosted recording plays end to end** | — | ❌ **UNVERIFIED — no credentials, recording disabled** |
| **LiveKit's real `location` scheme is one `RecordingStorageLocation` accepts** | — | ❌ **UNVERIFIED — never observed** |

The last two are the whole of what remains, and neither is closable from a keyboard without
credentials. The tests above establish the authorisation and proxying behaviour against real
HTTP, using an in-process server; they establish **nothing** about Backblaze B2 reachability,
key permissions or path-style addressing.

---

## 11. Verification status — honest separation of evidence

| Claim | Evidence type | Status |
|---|---|---|
| `.env` loader location + precedence rules | **automated tests** (14) | **VERIFIED** |
| `FRONTEND_URL` reaches `app.frontend-url` | **automated tests** (5) | **VERIFIED** |
| Frontend URL validation incl. prod loopback rejection | **automated tests** (12) | **VERIFIED** |
| Egress redaction; no startup abort | **automated tests** (11) | **VERIFIED** |
| Payment webhook secret wiring; fail-closed default | **automated tests** (2) | **VERIFIED** |
| Full backend suite green; no test weakened | **automated** | **VERIFIED** (1481/0/0/5) |
| Frontend type-check clean | **automated** | **VERIFIED** (`tsc --noEmit`, exit 0) |
| Config variable → consumer mapping | **code inspection** | **VERIFIED** (§6, `application*.yml`) |
| Recovery routes exist and match built links | **code inspection** | **VERIFIED** |
| Recording lifecycle/idempotency intact | **code inspection + pre-existing tests** | **VERIFIED** |
| No secret committed; `.env` ignored | **scans** (§9) | **VERIFIED** |
| Playback byte path authorised per request (§10.6) | **automated** (60 new tests) | **VERIFIED** |
| Storage location never disclosed to a client | **automated** | **VERIFIED** |
| Cookie fallback scoped to playback, GET/HEAD only | **automated** (10) | **VERIFIED** |
| **B2 credentials valid** | — | ❌ **UNVERIFIED — no credential supplied** |
| **Bucket reachable / path-style accepted** | — | ❌ **UNVERIFIED — no network call made** |
| **A recording actually uploads to B2** | — | ❌ **UNVERIFIED — never executed** |
| **`file_results[].location` scheme** | — | ❌ **UNVERIFIED — never observed** |
| **Replay published from a real `egress_ended`** | — | ❌ **UNVERIFIED at runtime** (logic covered by unit/concurrency tests only) |
| **A real B2-hosted recording plays through the new endpoint** | — | ❌ **UNVERIFIED — depends on the two above** |

**A real B2 upload was NOT attempted.** No credentials exist, recording is disabled, and no
authorisation to perform a recording was given. Nothing in this report should be read as B2
working.

---

## 12. Remaining manual tasks (developer)

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

## 13. Unresolved risks and why they could not be closed

1. **No B2 integration evidence.** Requires credentials that do not exist and an authorised
   test recording. Cannot be closed by code or tests.
2. **Playback of a *real* B2-hosted recording is still not demonstrated** (was: "playback
   entitlement on the byte fetch is not demonstrated" — that part is now closed, §10). The
   authorisation, proxying, Range handling and non-disclosure are all covered by automated
   tests against real HTTP. What no test can establish is that LiveKit's actual
   `file_results[].location` is a form `RecordingStorageLocation` accepts, and that the
   configured B2 endpoint and bucket are reachable with the supplied key. If the real location
   is something unrecognised, playback returns 503 (`resolveFetchUrl` empty) rather than
   leaking or fetching an untrusted host — **fails closed, but fails**. Closing this needs the
   first authorised recording and its recorded `location` value.
3. **The media library still returns `media_assets.file_url` verbatim**
   (`MediaLibraryController` `MediaAssetResponse.fileUrl` and `/{id}/download-url`).
   `LiveRecordingPublisher` writes the recording's location into that column, so a
   media-library viewer with entitlement receives the raw storage URL. Entitlement *is*
   enforced on that endpoint (its own suite passes), and a private bucket means the URL is a
   dead link rather than a leak — but if LiveKit returns a signed URL it is a bearer capability
   handed out in JSON. Not changed here: media assets also carry arbitrary teacher-supplied
   URLs (public CDN links and the like), so rewriting `file_url` to a playback URL would break
   legitimate uses and would be a redesign of a separate, already-tested subsystem. Recommended
   follow-up: have the media library expose its own authorized byte endpoint for
   `source_type = LIVE_CLASS` assets.
4. **`Event.recordingUrl` is a separate, older path** (`EventServiceImpl` → `LiveClassResponse`
   is *not* involved; event recordings are stored on the event row). It was out of scope for a
   live-class recording fix and was left byte-identical. Same residual shape as item 3.
5. **Committed credentials in `docker-compose.yml`.** Changing them invalidates local tokens
   and is a deployment decision; reported instead of silently altered.
6. **`prod` profile never activated.** Its corrected overrides have therefore never executed.
   The first activation should be reviewed deliberately.
7. **Unsupported `MEDIA_STORAGE` diagnostic is opaque.** Reported with an exact next action;
   fixing it safely requires bean-creation restructuring disproportionate to a message.
8. **Two `media_assets` writers remain** (`LiveRecordingMediaPublisher` for session
   completion, `LiveRecordingPublisher` for the webhook). Both are idempotent and backstopped
   by V150; metadata verified equivalent. Consolidation deferred — it is a refactor, not an
   environment fix.
9. **A pre-existing test failure exists outside the default suite, found during this work.**
   `LiveClassRecordingMediaLibraryIT` fails — `Tests run: 6, Failures: 2, Errors: 1` — on the
   **pristine baseline** (verified by running it in a clean `git worktree` at `f43187af` with
   zero changes), so it is **not** caused by either pass of this work. Cause:
   `LiveRecordingPublisher.publishUnderLock` refuses to publish when `recording_url` is
   null/blank (deliberate — *"a stray or replayed webhook must not conjure a Replay for it,
   because a Replay is learner-facing"*), while the IT fixture calls `setRecordingUrl(null)`
   before posting its `egress_ended` webhook, so publication is correctly refused and no asset
   appears. The fixture and the guard disagree; the guard is the security-correct side.
   `*IT.java` is not in surefire's default includes, which is why the green 1481-test suite
   has never surfaced it. **Not fixed here** — repairing it means deciding whether the fixture
   should seed an `egress:` marker (likely) or whether the guard is too strict (changing a
   deliberate anti-fabrication control), and neither is an environment or playback change.
   To reproduce explicitly:
   `mvn -f backend\elmkusoma-core\pom.xml test "-Dtest=LiveClassRecordingMediaLibraryIT" "-DfailIfNoSpecifiedTests=false"`

---

## 14. Bottom line

Every safe, evidence-supported change that could be made **without credentials** has been made
across two passes, and is covered by 104 new tests (1377 → 1481) on a green suite with 5
skipped, all pre-existing and credential-gated.

Pass 1 made the environment configuration real: `FRONTEND_URL` reaches `app.frontend-url`,
production overrides are no longer inert, `.env` loading is bounded and prints names only, and
half-configured recording is diagnosed at startup without taking the platform down.

Pass 2 closed the recording **playback authorization** gap. Clients are now given an
application-owned, same-origin playback URL and the byte fetch itself is authorised on every
request, so a copied, shared or screenshotted URL grants nothing. The bucket stays private, no
storage URL, bucket name or object key reaches a client, and the cookie fallback is confined to
GET/HEAD on two playback paths so nothing about the rest of the API's authentication changed.

What remains is **manual credential entry plus one authorised test recording** — not code.
Until that recording runs, B2 upload, the real `location` scheme, and playback of a *genuinely*
B2-hosted recording are **UNVERIFIED**, and this feature must not be described as complete.

*No credential appears in this document.*
