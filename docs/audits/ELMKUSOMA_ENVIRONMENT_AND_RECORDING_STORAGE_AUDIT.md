# ELMKUSOMA — Environment Configuration & Live Recording Storage Audit

**Scope:** Read-only audit of all environment settings for platform-admin bootstrap, MFA encryption, frontend URL, media/MinIO storage, SMTP, and LiveKit Egress recording storage (Backblaze B2).
**Date:** 2026-10-11
**Repository:** `C:\Users\user\Desktop\elmkusoma`
**Auditor role:** Spring Boot infrastructure / LiveKit Egress / S3-compatible storage / application security
**Changes made to application code or configuration:** none. This audit produced only this document.

> **Secret handling:** No credential, key, token, password or signature value is reproduced in this report. Credential fields are reported by presence/absence only. The only concrete values shown are non-secret identifiers (bucket name, region, endpoint host, object prefix, boolean flags).

---

## 1. Audit scope and method

In scope: `backend/elmkusoma-core`, `backend/elmkusoma-media`, `backend/elmkusoma-realtime`, `backend/elmkusoma-workers`, `frontend`, root Docker Compose files, and `backend/.env` / `backend/.env.example`.

Excluded from inspection: `node_modules`, `target`, `.next`, `.git`, generated build output.

Method: static inspection of configuration sources, property-binding classes and consumers, cross-checked against the environment files actually present, plus runtime evidence taken from the already-running backend process log. No configuration was written, no key generated, no recording started.

---

## 2. Project paths and actual configuration loading

| Component | Path | Role |
|---|---|---|
| Backend core | `backend/elmkusoma-core` | REST API, LiveKit integration, Egress, replay/media publishing |
| Media service | `backend/elmkusoma-media` | MinIO/S3 media storage, recording library |
| Workers | `backend/elmkusoma-workers` | async jobs |
| Realtime | `backend/elmkusoma-realtime` | WebSocket |
| Frontend | `frontend` | Next.js 16 |
| Shared env file | `backend/.env` (git-ignored), `backend/.env.example` | template only |

### 2.1 How environment variables actually reach the backend

This is the single most important mechanical fact in this audit, and it is **not** the java-dotenv library.

`backend/elmkusoma-core/src/main/java/tz/elmkusoma/ElmkusomaCoreApplication.java`

- Lines 22–25: `loadDotenv()` runs **before** `SpringApplication.run(...)`.
- Lines 32–63: a hand-rolled parser. For each non-comment `KEY=VALUE` line it sets a **system property**, not an environment variable:
  - line 54 — `if (System.getenv(key) == null && System.getProperty(key) == null)`
  - line 55 — `System.setProperty(key, value)`
- Lines 65–73: searches only `./.env` then `../.env` relative to the working directory.
- Spring's `${VAR:default}` placeholders resolve from system properties, so `backend/.env` does feed the `${...}` expressions in `application.yml`.

**Precedence: real environment variable or system property wins; `.env` only fills gaps.** Values are never overwritten by the file.

### 2.2 Critical limitation — only the core module loads `.env`

| Module | Own dotenv loader |
|---|---|
| `elmkusoma-core` | **YES** |
| `elmkusoma-media` | **NO** |
| `elmkusoma-workers` | **NO** |
| `elmkusoma-realtime` | **NO** |

Consequence: **`MEDIA_STORAGE`, `MINIO_*` and `MEDIA_LOCAL_DIR` are consumed by `elmkusoma-media`, which does not read `.env`.** They only take effect if they exist as genuine ambient environment variables. They currently reach the media service only because `backend/start-backend.ps1` and the root `start-dev.ps1` parse `backend/.env` into the PowerShell process environment before launching each module. Launching the media jar directly from a bare shell would silently fall back to defaults.

### 2.3 Runtime evidence — dotenv behaviour observed

From the live backend log (`.devlogs/backend.log`):

```
[dotenv] Loading C:\Users\user\Desktop\elmkusoma\backend\elmkusoma-core\..\.env
[dotenv] Loaded 0 properties from .env
```

`Loaded 0` is **correct and expected here**, not a failure: the launcher had already injected every `.env` key into the process environment, so the loader's guard at line 54 suppressed every assignment. This confirms the precedence rule in §2.1 and confirms that `backend/.env` content *is* reaching the running process.

---

## 3. Phase 2 — Today's settings

| Variable | Purpose | Expected source | Presence in `backend/.env` | Consumed by code | Status | Evidence | Next action |
|---|---|---|---|---|---|---|---|
| `ELM_BOOTSTRAP_ADMIN` | Enable first-boot Platform Admin provisioning | `app.security.admin-bootstrap.enabled` | **ABSENT** | Yes | **COMMENTED OUT / TEMPLATE ONLY** (defaults `false`) | `application.yml:139-140`; consumer `AdminUserInitializer.java:28` | Set to `true` only for genuine first boot; leave unset thereafter |
| `ADMIN_BOOTSTRAP_PASSWORD` | One-time password used at admin creation | env/system property | **ABSENT** | Yes | **COMMENTED OUT / TEMPLATE ONLY** | `AdminUserInitializer.java:31,43-46` | Supply only alongside `ELM_BOOTSTRAP_ADMIN=true` |
| `MFA_ENCRYPTION_KEY` | Base64 32-byte AES-256-GCM key for authenticator secrets | env/system property | **ABSENT** | Yes | **MISSING** | `MfaEncryptionService.java:28,31-46` | Generate a 32-byte random Base64 key and set it |
| `FRONTEND_URL` | Public base URL for recovery/reset links | **none — not wired** | **ABSENT** | **No** | **CODE SUPPORT MISSING** (wiring) / **MISSING** (value) | Consumers read `app.frontend-url` at `AuthServiceImpl.java:69` and `PlatformAdminService.java:130`; no `.yml` maps `FRONTEND_URL` to it | Use `APP_FRONTEND_URL` (Spring relaxed binding for `app.frontend-url`), or add a mapping line to `application.yml` |
| `MEDIA_STORAGE` | Storage mode for general media | `elmkusoma-media` | **ABSENT** | Yes (media module) | **MISSING** — falls back to `minio` | `elmkusoma-media/…/application.yml:73` | Set in the **media service** environment |
| `MEDIA_LOCAL_DIR` | Local filesystem store path | `elmkusoma-media` | **ABSENT** | Yes (media module) | **MISSING** — falls back to `${java.io.tmpdir}/elmkusoma-media` | `elmkusoma-media/…/application.yml:75` | Set only if `MEDIA_STORAGE=local` |
| `MINIO_ENDPOINT` | MinIO/S3 endpoint | `elmkusoma-media` | **ABSENT** | Yes (media module) | **MISSING** — defaults `http://localhost:9000` | `elmkusoma-media/…/application.yml:65` | Provide if using MinIO |
| `MINIO_ACCESS_KEY` | MinIO/S3 access key | `elmkusoma-media` | **ABSENT** | Yes (media module) | **MISSING** — insecure default (redacted) | `elmkusoma-media/…/application.yml:66` | Provide if using MinIO |
| `MINIO_SECRET_KEY` | MinIO/S3 secret key | `elmkusoma-media` | **ABSENT** | Yes (media module) | **MISSING** — insecure default (redacted) | `elmkusoma-media/…/application.yml:67` | Provide if using MinIO |
| `MINIO_BUCKET` | MinIO/S3 bucket | `elmkusoma-media` | **ABSENT** | Yes (media module) | **MISSING** — defaults `elmkusoma-media` | `elmkusoma-media/…/application.yml:68` | Provide if using MinIO |
| `SPRING_MAIL_HOST` | SMTP host | `spring.mail.host` | **ABSENT** | Yes | **MISSING** — defaults `localhost` | `application.yml:71` | Set real SMTP host for production |
| `SPRING_MAIL_PORT` | SMTP port | `spring.mail.port` | **ABSENT** | Yes | **MISSING** — defaults `1025` (MailHog/Mailpit sink) | `application.yml:72` | Set real SMTP port |
| `SPRING_MAIL_USERNAME` | SMTP username | `spring.mail.username` | **ABSENT** | Yes | **MISSING** — defaults empty | `application.yml:73` | Set if the provider requires auth |
| `SPRING_MAIL_PASSWORD` | SMTP password | `spring.mail.password` | **ABSENT** | Yes | **MISSING** — defaults empty | `application.yml:74` | Set if the provider requires auth |

### 3.1 Validation and security notes

**A — Admin bootstrap (positive findings).** `AdminUserInitializer.run()` is well hardened:
- Opt-in and create-only. Lines 39–42: disabled by default, logs that the existing account is left untouched.
- Lines 48–51: if the admin account already exists it **never** resets password, role or status — a restart cannot silently restore a default privileged credential.
- Lines 43–46: if enabled without a password it **throws and refuses to start** rather than defaulting.

*Risk if misconfigured:* leaving `ELM_BOOTSTRAP_ADMIN=true` in a long-lived environment is unnecessary but not harmful, because creation is idempotent and non-destructive.

**B — MFA encryption (positive findings).** `MfaEncryptionService` validates defensively:
- line 32–35: missing key → `IllegalStateException`, refusing to handle authenticator secrets.
- lines 37–41: non-Base64 → explicit error.
- lines 42–44: decoded length ≠ 32 bytes → explicit AES-256 error.
- Uses AES/GCM/NoPadding with a fresh 12-byte random IV per encryption (lines 50–54), envelope `base64(iv).base64(ct)`.
- No hardcoded fallback key.

*Impact of absence:* MFA authenticator enrolment/decryption fails at runtime with a clear error rather than silently using a weak key.

**C — Frontend URL (real defect).** `FRONTEND_URL` appears in `.env.example` but **no code or YAML consumes that name.** Two consumers read `app.frontend-url`, which defaults to `http://localhost:3000`. Setting `FRONTEND_URL` therefore has **no effect**; recovery emails would contain `localhost` links. Spring's relaxed binding maps `APP_FRONTEND_URL` (not `FRONTEND_URL`) to `app.frontend-url`.

**D — Media storage.** All MinIO keys default to the well-known public MinIO development credentials (values redacted here; literals are in `elmkusoma-media/src/main/resources/application.yml:66-67`) and to `http://localhost:9000`. Acceptable for local development, unsafe if the media service is ever exposed with these defaults in a non-local profile. No production-profile override was found for the media module.

**E — SMTP.** Delivery is documented as best-effort: an unreachable host never fails `/v1/auth/send-code` (`application.yml:66-69`). Defaults target a local MailHog/Mailpit sink on 1025. Missing credentials mean OTP mail is silently undeliverable in production unless a relay accepts unauthenticated local delivery.

---

## 4. Phase 3 — LiveKit Egress + Backblaze B2

### 4.1 The seven canonical variable names actually supported

Binding class: `backend/elmkusoma-core/src/main/java/tz/elmkusoma/liveclass/config/LiveKitConfig.java`, `@ConfigurationProperties(prefix = "livekit", ignoreInvalidFields = true)`, nested `EgressConfig` at lines 42–64.

Declared in `backend/elmkusoma-core/src/main/resources/application.yml:123-132`:

| # | Environment variable | Bound property | Default |
|---|---|---|---|
| 1 | `LIVEKIT_EGRESS_ENABLED` | `livekit.egress.enabled` | `false` |
| 2 | `LIVEKIT_EGRESS_BUCKET` | `livekit.egress.output-bucket` | `""` |
| 3 | `LIVEKIT_EGRESS_PATH` | `livekit.egress.output-path` | `recordings/` |
| 4 | `LIVEKIT_EGRESS_S3_ACCESS_KEY` | `livekit.egress.access-key` | `""` |
| 5 | `LIVEKIT_EGRESS_S3_SECRET` | `livekit.egress.secret` | `""` |
| 6 | `LIVEKIT_EGRESS_S3_REGION` | `livekit.egress.region` | `""` |
| 7 | `LIVEKIT_EGRESS_S3_ENDPOINT` | `livekit.egress.endpoint` | `""` |

These are the exact names. Note they are **not** the generic `MINIO_*` names and **not** `LIVEKIT_EGRESS_S3_PATH`.

### 4.2 Current state versus intended configuration

| # | Variable | Intended | Actual in `backend/.env` | Match |
|---|---|---|---|---|
| 1 | `LIVEKIT_EGRESS_ENABLED` | enabled | `false` | **NO** |
| 2 | `LIVEKIT_EGRESS_BUCKET` | `elmkusoma-live-recordings` | `elmkusoma-live-recordings` | yes |
| 3 | `LIVEKIT_EGRESS_PATH` | `recordings/` | `recordings/` | yes |
| 4 | `LIVEKIT_EGRESS_S3_ACCESS_KEY` | B2 application key ID | **present but EMPTY** | **NO** |
| 5 | `LIVEKIT_EGRESS_S3_SECRET` | B2 secret application key | **present but EMPTY** | **NO** |
| 6 | `LIVEKIT_EGRESS_S3_REGION` | `us-east-005` | `us-east-005` | yes |
| 7 | `LIVEKIT_EGRESS_S3_ENDPOINT` | `https://s3.us-east-005.backblazeb2.com` | `https://s3.us-east-005.backblazeb2.com` | yes |

**5 of 7 non-secret settings match intent exactly. The two credentials are empty and the enable flag is `false`.**

### 4.3 Trace: source → binding → Egress/S3 upload code

1. `.env` → system property (loader) or ambient env → `application.yml:123-132` placeholder → `LiveKitConfig.EgressConfig`.
2. Enablement gate: `LiveKitConfig.isEgressEnabled()` (lines 75–77) requires **both** `isConfigured()` (non-empty server api-key **and** api-secret, lines 66–69) **and** `egress.enabled`.
3. Storage gate: `EgressConfig.hasStorage()` (lines 56–63) requires `outputBucket` **and** `accessKey` **and** `secret` non-empty **and** at least one of `region` / `endpoint`.
4. Enforcement point: `LiveKitService.startRecording(...)`, lines 213–229 — returns `null` and logs *"Recording storage not configured"* when `hasStorage()` is false, rather than starting an egress job that would fail later at upload.
5. Egress request construction: lines 234–255.
   - line 234 — `outputPath = outputPath + classId + "/%c"` → with the configured prefix this yields `recordings/<classId>/<timestamp>` (LiveKit substitutes `%c` as a timestamp). **Matches the intended object pattern.**
   - lines 237–240 — `accessKey`, `secret`, `bucket`, `region` sent.
   - lines 241–243 — `endpoint` sent **and `forcePathStyle=true` set**, but only when `endpoint` is non-empty. **Matches the intended B2 path-style requirement.** Because the endpoint *is* configured, path-style will apply.
   - lines 248–255 — `EncodedFileOutput` with the storage `output` oneof.
   - line 258 — Twirp `StartRoomCompositeEgress`.
6. Lifecycle: `LiveClassServiceImpl.java:366` starts recording; `:547` stops it; `:548` calls `resolveRecordingUrl`; `:590` persists `recordingUrl`.
7. Webhook: `LiveKitWebhookController.java:121` handles `egress_ended` / `recording_completed`; line 686 finalises the class recording. HMAC signature verification at lines 148–151, returning **401** on failure (lines 71–76).
8. Playback URL: `LiveKitService.toPlayableRecordingUrl(...)`, lines 588–616 — accepts `http(s)` as-is; otherwise **rebuilds the URL from the configured endpoint and bucket**, never from the webhook payload. This is a deliberate anti-SSRF control. Returns `null` rather than fabricating a URL (lines 599–607).

### 4.4 LiveKit recording storage vs general MinIO/media storage — distinct systems

These are **two unrelated storage paths** and must not be conflated.

| | General media storage | LiveKit Egress recording storage |
|---|---|---|
| Owner module | `elmkusoma-media` | `elmkusoma-core` |
| Config keys | `MEDIA_STORAGE`, `MINIO_*`, `MEDIA_LOCAL_DIR` | `LIVEKIT_EGRESS_*` (7 keys, §4.1) |
| Default backing store | MinIO `http://localhost:9000` | none — disabled |
| Purpose | Uploaded/general media assets, recording **library entries** | Byte stream of the live class recording, written **by LiveKit itself** |
| Writes objects? | Yes, application-side | **No** — LiveKit writes directly using the credentials sent in the egress request |
| Who uploads | `elmkusoma-media` | LiveKit server |

**`MEDIA_STORAGE` does NOT configure recording storage.** Setting `MEDIA_STORAGE` or the `MINIO_*` keys changes only the general media library. Recording upload is governed solely by §4.1.

### 4.5 Why recording cannot start right now

Two independent gates both fail:

1. `LIVEKIT_EGRESS_ENABLED=false` → `isEgressEnabled()` is false → recording is refused.
2. `LIVEKIT_EGRESS_S3_ACCESS_KEY` and `..._S3_SECRET` are empty → `hasStorage()` is false → `startRecording()` returns `null` at `LiveKitService.java:224-229` and logs *"Recording storage not configured"*.

Even if the flag were flipped with empty credentials, LiveKit Cloud would reject the start or the upload would fail with an S3 access error — the code deliberately prevents creating such a doomed job.

**Runtime status: recording is confirmed INACTIVE.** No egress log line appears in the live backend log, and no recording was attempted during this audit (read-only).

---

## 5. Verification status

| Area | Evidence type | Result |
|---|---|---|
| Egress code path | Static | Complete and correct |
| B2 non-secret config | `.env` inspection | 5/7 correct |
| B2 credentials | `.env` inspection | **Absent/empty** |
| Egress enablement | `.env` + `isEgressEnabled()` logic | **Disabled** |
| Object path pattern | Static (`LiveKitService.java:234`) | `recordings/<classId>/%c` — correct |
| Path-style access | Static (`LiveKitService.java:243`) | `forcePathStyle=true` — correct |
| Webhook signature verification | Static (`LiveKitWebhookController.java:148-151`) | Present, 401 on failure |
| Playback URL anti-SSRF | Static (`LiveKitService.java:588-616`) | Correct |
| `.env` → running process | Runtime log (`Loaded 0 properties`) | Confirmed reaching process |
| **Recording end-to-end** | **None** | **UNVERIFIED — never executed** |
| **B2 credential validity** | **None** | **UNVERIFIED** |
| **Object actually uploaded to B2** | **None** | **UNVERIFIED** |

> Static inspection cannot prove B2 credentials are valid or that any object reached the bucket. No recording was started and no upload was attempted, per the read-only constraint.

---

## 6. Runtime verification gaps and safe next tests

Once the missing values are supplied, the following are safe, non-destructive checks:

1. **Storage-gate check (no side effects).** Start the backend and confirm `startRecording()` is reachable — i.e. no *"Recording storage not configured"* warning appears when a class starts.
2. **Egress start/stop.** Use the existing integration test `LiveKitIntegrationTest.livekit_EgressStartAndStop_AgainstRealServer()` — it starts and immediately stops a real egress against the configured server. Note it is deliberately guarded to require genuinely configured credentials.
3. **Path-style reachability (read-only).** From a machine with network access, issue a `HEAD` on the bucket endpoint using the B2 key, confirming `403`/`401`-free DNS+TLS and that path-style addressing is accepted. This validates endpoint/region/credentials **without writing an object**.
4. **End-to-end upload.** Only after 1–3 pass: run one short live class with recording enabled, then confirm `egress_ended` arrives, `recordingUrl` is persisted, and the object exists in the bucket at `recordings/<classId>/<timestamp>`. This is the only step that proves the upload works.

No step above requires modifying application code.

---

## 7. Prioritized findings

**P0 — blocks live recording entirely**
1. `LIVEKIT_EGRESS_S3_ACCESS_KEY` — present but empty.
2. `LIVEKIT_EGRESS_S3_SECRET` — present but empty.
3. `LIVEKIT_EGRESS_ENABLED` — `false`.

**P1 — security / correctness**
4. `MFA_ENCRYPTION_KEY` absent — MFA authenticator secrets cannot be encrypted or decrypted; the service fails closed with a clear error.
5. `FRONTEND_URL` is not wired to anything. Account-recovery links resolve to `http://localhost:3000`. Use `APP_FRONTEND_URL` or add an explicit mapping.
6. `.env` is loaded by **only** `elmkusoma-core`; `elmkusoma-media`, `-workers`, `-realtime` depend on ambient environment variables. A media/minio setting added to `.env` alone may appear configured but never reach the media service.

**P2 — production hygiene**
7. SMTP entirely unset — defaults target a local MailHog sink on 1025; OTP/recovery mail is effectively undeliverable.
8. MinIO development defaults apply when unset (credential literals redacted; see §3.1-D); no production override found for the media module.
9. No production-profile validation that rejects MinIO dev defaults or a `localhost` frontend URL.

**P3 — documentation drift**
10. `backend/.env.example` advertises `FRONTEND_URL`, which no code reads, and comments out `MEDIA_LOCAL_DIR` with a blank value.

---

## 8. Summary

### Confirmed configured
- Five of seven LiveKit Egress settings are exactly as intended: bucket, region, endpoint, object prefix, and (statically) path-style access and object-path construction.
- Egress, webhook signature verification, recording finalisation, playback-URL derivation and playback access control are all implemented and wired end to end in code.
- Admin bootstrap is implemented safely: opt-in, create-only, never resets an existing account, fails closed when enabled without a password.
- MFA encryption is implemented safely: Base64 + 32-byte AES-256 validation, AES-GCM, per-encryption random IV, no fallback key.
- `backend/.env` content is demonstrably reaching the running backend process.

### Missing or commented out
- `MFA_ENCRYPTION_KEY`
- `ELM_BOOTSTRAP_ADMIN`, `ADMIN_BOOTSTRAP_PASSWORD`
- `MEDIA_STORAGE`, `MEDIA_LOCAL_DIR`, `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`
- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- `FRONTEND_URL`
- `LIVEKIT_EGRESS_S3_ACCESS_KEY`, `LIVEKIT_EGRESS_S3_SECRET` — present as keys but **empty**

### Present but not wired
- `FRONTEND_URL` — present in `.env.example`, read by nothing.
- `MEDIA_STORAGE` / `MINIO_*` / `MEDIA_LOCAL_DIR` — wired in `elmkusoma-media`, but that module does not load `.env`; the values only arrive via ambient environment variables injected by a start script.

### Code/configuration issues
- `LIVEKIT_EGRESS_ENABLED=false` while four of five non-secret Egress values are already correct — recording is switched off despite being otherwise configured.
- No production-profile guard rejecting MinIO development defaults or a localhost frontend URL.

### Requires runtime testing
- Whether the B2 credentials are valid and can write to `elmkusoma-live-recordings`.
- Whether a real class recording is produced, uploaded, and finalised through `egress_ended`.
- Whether playback URLs resolve from B2 and are served under the correct access control.

---

## 9. Exact configuration items required

To enable live recording to Backblaze B2, set in `backend/.env` (values for 1–2 are placeholders to be filled with real B2 application-key credentials):

```
LIVEKIT_EGRESS_ENABLED=true
LIVEKIT_EGRESS_S3_ACCESS_KEY=<B2 application key ID>
LIVEKIT_EGRESS_S3_SECRET=<B2 secret application key>
```

The remaining five settings already match intent and need no change.

To enable MFA, set a newly generated Base64-encoded 32-byte key:

```
MFA_ENCRYPTION_KEY=<base64 of 32 random bytes>
```

To fix recovery links, either:

```
APP_FRONTEND_URL=https://<public-frontend-host>
```

or add to `application.yml` under `app:`:

```yaml
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
```

To enable transactional mail:

```
SPRING_MAIL_HOST=<smtp host>
SPRING_MAIL_PORT=<smtp port>
SPRING_MAIL_USERNAME=<smtp username>
SPRING_MAIL_PASSWORD=<smtp password>
```

For MinIO-backed media, set these **in the `elmkusoma-media` service environment** (not only in `backend/.env`, which that module does not read):

```
MEDIA_STORAGE=minio
MINIO_ENDPOINT=<endpoint>
MINIO_ACCESS_KEY=<key>
MINIO_SECRET_KEY=<secret>
MINIO_BUCKET=<bucket>
```

---

*End of audit. No application code or configuration was modified.*