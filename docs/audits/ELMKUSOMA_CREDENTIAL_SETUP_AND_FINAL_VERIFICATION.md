# ELMKUSOMA — Credential Setup & Final Verification

**Purpose:** the exact, practical checklist for supplying real credentials and then proving the recording pipeline works end to end.
**Date:** 2026-10-11
**Repository:** `C:\Users\user\Desktop\elmkusoma`
**Status of this document:** hand-off instructions. It contains **no credentials**.

> **Do not paste credentials into chat, a commit, a log, a screenshot, a report, or the
> frontend.** Every value below goes into `backend/.env` (local) or your orchestrator's
> secret store (deployed). All verification steps here use *presence* checks that never
> print a value.

---

## 0. Where values go

| Environment | Location |
|---|---|
| Local development | `backend/.env` (git-ignored — confirmed `git check-ignore` matches it, and `git ls-files` does not list it) |
| Template to copy from | `backend/.env.example` (**tracked** — placeholder values only, never real ones) |
| Docker Compose | `environment:` blocks in `docker-compose.yml` |
| Deployed | real environment variables / secret manager of your platform |

**Precedence:** real environment variable → existing system property → `backend/.env` →
`application.yml` default. The file only fills gaps; it never overwrites you.

**Which process reads what** (this matters — it is the most common setup mistake):

| Module | Reads `backend/.env` itself? | How it actually gets config |
|---|---|---|
| `elmkusoma-core` | **Yes** (`./.env`, then `../.env` only from a directory containing `.env.example`) | self + ambient env |
| `elmkusoma-media` | No | ambient env injected by `start-backend.ps1` / `start-dev.ps1`, or compose `environment:` |
| `elmkusoma-workers` | No | ambient env / compose |
| `elmkusoma-realtime` | No | ambient env / compose |

Launch `elmkusoma-media`, `-workers` or `-realtime` from a bare shell and a value written
only in `backend/.env` **will not reach it.**

**Restart:** every setting is read once at startup. Restart the consuming service.

---

## 1. Always required

| Variable | Consumed by | Format | Obtain from | Secret | Restart |
|---|---|---|---|---|---|
| `DB_PASSWORD` | core, media, workers | your PostgreSQL password | your DBA / `setup-db.ps1` | **yes** | yes |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` | core, media, workers | plain | your DBA | no | yes |
| `JWT_SECRET` | core, media, realtime (**not** workers) | Base64, ≥ 32 random bytes | `openssl rand -base64 32` | **yes** | yes |

`JWT_SECRET` has **no default**: startup fails loudly without it, which is intentional.
`elmkusoma-realtime` Base64-decodes it at boot and requires a ≥256-bit key — a short value
aborts that service immediately. Changing it logs every user out.

---

## 2. Platform Admin bootstrap (one-time, fresh database only)

```dotenv
ELM_BOOTSTRAP_ADMIN=true
ADMIN_BOOTSTRAP_PASSWORD=<a strong password you generate>
```

- Consumed by `elmkusoma-core` (`AdminUserInitializer`).
- **Safe by construction** (asserted by `AdminUserInitializerTest`): disabled by default;
  if `admin@elmkusoma.go.tz` already exists the account is **never** modified — password,
  role and status are untouched; if enabled with no password it refuses to start.
- Procedure: set both → boot once → confirm the account exists → remove
  `ADMIN_BOOTSTRAP_PASSWORD` and set `ELM_BOOTSTRAP_ADMIN=false` → restart.
- **Never** leave `ADMIN_BOOTSTRAP_PASSWORD` lying around; it is used exactly once.

---

## 3. MFA encryption key

```bash
openssl rand -base64 32      # run locally; do not paste the output anywhere but .env
```

```dotenv
MFA_ENCRYPTION_KEY=<that value>
```

- Consumed by `elmkusoma-core` (`MfaEncryptionService`).
- **Algorithm as implemented:** `AES/GCM/NoPadding`, fresh 12-byte IV per encryption,
  envelope `base64(iv).base64(ciphertext)`.
- **Validation:** must be Base64 decoding to **exactly 32 bytes**. Non-Base64 or the wrong
  length is rejected with an explicit message. There is no fallback key.
- Absent ⇒ MFA enrollment and every MFA login fail loudly at runtime; the service still boots.
- **Rotation warning:** changing this key makes every already-enrolled authenticator secret
  undecryptable — all users would have to re-enroll. Do not rotate casually.

---

## 4. Frontend URL (account-recovery links)

```dotenv
FRONTEND_URL=https://your-public-frontend-host
```

- Consumed by `elmkusoma-core` (`AuthServiceImpl`, `PlatformAdminService`) via
  `app.frontend-url`. **This wiring was broken and has been fixed** — previously the name in
  `.env.example` was read by nothing and links silently became `http://localhost:3000`.
- Format: absolute `http(s)` origin, **no trailing slash**, no query/fragment.
- Validated at startup by `FrontendUrlValidator`. Under the `prod` profile a loopback host
  is **rejected** — production can no longer email localhost links.
- The routes it builds all exist: `frontend/app/reset-password/page.tsx`,
  `frontend/app/verify-email/page.tsx`, `frontend/app/login/page.tsx`. No middleware
  intercepts them.

---

## 5. General media storage (NOT the recording store)

Consumed by **`elmkusoma-media` only**, and that module does not read `backend/.env` itself.

```dotenv
MEDIA_STORAGE=minio          # or "local"
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=<key>
MINIO_SECRET_KEY=<secret>
MINIO_BUCKET=elmkusoma-media
```

- `MEDIA_STORAGE=local` uses `MEDIA_LOCAL_DIR` (defaults to `${java.io.tmpdir}/elmkusoma-media`).
- The media `application.yml` ships well-known development MinIO credentials as defaults.
  Fine locally; never expose the media service with them outside a local environment.
- **`MEDIA_STORAGE` does not configure LiveKit recording storage.** That is §7, exclusively.

---

## 6. SMTP

**Both** `elmkusoma-core` (OTP codes, direct SMTP) and `elmkusoma-workers` (mail sent from
the `elmkusoma.email.queue` RabbitMQ queue) need these. Setting one does not configure the other.

```dotenv
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<account>
SPRING_MAIL_PASSWORD=<APP PASSWORD, not the account password>
```

- Gmail/Google require an app password and STARTTLS on 587.
- Missing credentials **never fail a request** — delivery is best-effort, so OTP/recovery
  mail is silently undeliverable until this is real. Failures appear as send-time errors
  and queue retries, not startup failures.

---

## 7. LiveKit Egress + Backblaze B2 (recording)

### 7.1 The seven supported names

| # | Variable | Bound property | Current value in `backend/.env` |
|---|---|---|---|
| 1 | `LIVEKIT_EGRESS_ENABLED` | `livekit.egress.enabled` | `false` |
| 2 | `LIVEKIT_EGRESS_BUCKET` | `livekit.egress.output-bucket` | `elmkusoma-live-recordings` |
| 3 | `LIVEKIT_EGRESS_PATH` | `livekit.egress.output-path` | `recordings/` |
| 4 | `LIVEKIT_EGRESS_S3_ACCESS_KEY` | `livekit.egress.access-key` | **present but EMPTY** |
| 5 | `LIVEKIT_EGRESS_S3_SECRET` | `livekit.egress.secret` | **present but EMPTY** |
| 6 | `LIVEKIT_EGRESS_S3_REGION` | `livekit.egress.region` | `us-east-005` |
| 7 | `LIVEKIT_EGRESS_S3_ENDPOINT` | `livekit.egress.endpoint` | `https://s3.us-east-005.backblazeb2.com` |

Plus the LiveKit server credentials the same module needs:
`LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`.

### 7.2 Create the bucket

1. Create bucket `elmkusoma-live-recordings` in Backblaze B2, in region `us-east-005`.
2. **Leave it PRIVATE.** Do not make it public — see §9.

### 7.3 Create a bucket-scoped application key (minimum privilege)

In the B2 console, use **Application Keys** (not the master application key) and create a
key **restricted to the single bucket** `elmkusoma-live-recordings`, ideally also restricted
to the `recordings/` prefix if your account supports prefix restriction. Grant only what the
upload needs: **write** to that bucket/prefix (add **list** if your LiveKit version reports
the key back through listing).

Why minimum privilege: this credential is handed to the **LiveKit egress process**, which is
a different process from your backend. It should not be able to touch any other bucket,
delete unrelated data, or administer the account.

You receive:
- the **key ID** → `LIVEKIT_EGRESS_S3_ACCESS_KEY`
- the **application key** (secret) → `LIVEKIT_EGRESS_S3_SECRET`

Both are **secrets**. Never commit them.

### 7.4 Enable in two steps (deliberate)

1. Fill in **only** the two credential lines. Leave `LIVEKIT_EGRESS_ENABLED=false`.
2. Restart, and run §11.1 — confirm no "not usable" diagnostic.
3. Set `LIVEKIT_EGRESS_ENABLED=true`, restart, re-run §11.1.

This ordering exists so the switch is only flipped once storage is proven reachable.

---

## 8. LiveKit recording configuration validation

When `LIVEKIT_EGRESS_ENABLED=true` the backend logs, at startup:

- **a redacted `ERROR`** naming exactly which of `LIVEKIT_EGRESS_BUCKET`,
  `LIVEKIT_EGRESS_S3_ACCESS_KEY`, `LIVEKIT_EGRESS_S3_SECRET`,
  `LIVEKIT_EGRESS_S3_REGION`/`_ENDPOINT`, `LIVEKIT_API_KEY`/`_SECRET` are missing, and
  reporting a non-absolute `LIVEKIT_EGRESS_S3_ENDPOINT`. **Variable names only — never a value.**
- Recording start requests are then refused (503), so no doomed egress job is ever created.

**It deliberately does not abort startup.** Recording storage is optional; taking the whole
platform down for one optional subsystem would break unrelated features.

When `LIVEKIT_EGRESS_ENABLED=false` — the current state — none of this runs and the app
starts normally with no B2 credentials at all.

---

## 9. Verify presence without displaying values

```powershell
# Names of everything currently active in your local file (prints no values):
Get-Content backend\.env | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } |
  ForEach-Object { ($_ -split '=',2)[0] }
```

```powershell
# Is a specific secret present and non-empty? Prints only PRESENT / EMPTY / ABSENT.
$lines = Get-Content backend\.env
foreach ($k in 'LIVEKIT_EGRESS_S3_ACCESS_KEY','LIVEKIT_EGRESS_S3_SECRET','MFA_ENCRYPTION_KEY','FRONTEND_URL','SPRING_MAIL_PASSWORD') {
  $hit = $lines | Where-Object { $_ -match ('^\s*' + [regex]::Escape($k) + '\s*=') } | Select-Object -First 1
  if (-not $hit) { "  $k : ABSENT"; continue }
  $v = ($hit -replace '^[^=]*=','').Trim()
  "  $k : " + $(if ($v -eq '') { 'EMPTY' } else { 'PRESENT' })
}
```

```powershell
# Is the file actually ignored? (should print backend/.env and exit 0)
git check-ignore -v backend/.env
```

Startup diagnostics are also safe: the `.env` loader prints the file path, the count, and
the **key names** it applied — never values.

---

## 10. First recording — end-to-end verification

> **Authorization required.** Run this only in an environment you are permitted to record.
> It creates a real egress job and a real object in your bucket.

**Step 1 — start.**
`POST /v1/live-session/classes/{classId}/recording/start`
- ✅ no `503 Recording storage is not configured on the server.`
- ✅ response carries an egress id; `live_classes.recording_url` becomes `egress:<id>`

**Step 2 — let it run ~1 minute, then stop / end the class.**

**Step 3 — confirm the object exists in the private bucket.**
List `recordings/<classId>/` in the B2 console (or `b2 ls`):
- ✅ exactly one object, under `recordings/<classId>/<timestamp>`

**Step 4 — confirm the webhook finished publication.**
- ✅ `egress_ended` / `recording_completed` handled → `live_classes.recording_url` is now a URL
- ✅ exactly **one** row in `replays` for that session
- ✅ exactly **one** row in `media_assets` with `source_type='LIVE_CLASS'` and `source_id=<class id>`

Prove idempotency by re-delivering the webhook (or ending the class twice) and re-counting —
migration **V159** (`uq_replays_live_session`) plus the media-asset unique index are what
make a duplicate impossible.

```sql
SELECT count(*) FROM replays        WHERE live_session_id = '<class id>';
SELECT count(*) FROM media_assets   WHERE source_type='LIVE_CLASS' AND source_id='<class id>';
```

**Step 5 — record the actual `location` value.** This is the observation the whole design
turns on and **has never been captured** (no real recording has ever run). Note whether
`file_results[].location` is `s3://…`, a bare key, or a full `https://…` URL.

**Step 6 — playback.**
- ✅ an **entitled** user (participant / owning teacher) can open the replay
- ⛔ a user **without** access gets 403/404 from the *listing* endpoint and never learns the URL

Expected outcomes for the byte fetch itself (both are acceptable; neither is yet observed):

| `location` shape | Private bucket result |
|---|---|
| plain `https://<endpoint>/<bucket>/…` | browser gets **403** → playback fails, nothing leaks |
| signed URL | plays until expiry |

**Do not make the bucket public to get a green result.** See §12 for the open item.

**Step 7 — confirm the bucket is still private** and that no unauthenticated request can
fetch the object.

---

## 11. Quick configuration checks

### 11.1 After changing Egress settings

```
grep -A3 "LiveKitEgressConfigValidator" .devlogs/backend.log
```
- ✅ `storage destination configured (values not displayed)` → good
- ❌ `LIVEKIT_EGRESS_ENABLED=true, but live recording is not usable: …` → fill the named vars

### 11.2 After changing any other value

Restart, then confirm the service came up and the `.env` loader line shows the expected
**key names**:
```
[dotenv] Loaded N properties from .env: KEY1, KEY2, ...
```

---

## 12. Remaining risks — read before go-live

1. **B2 upload has never been performed.** No credential has ever been supplied and no
   recording has ever completed. Bucket reachability, key permissions, path-style addressing
   and the actual `location` scheme are all **UNVERIFIED**. Unit tests passing does not
   establish any of them.
2. **Per-request entitlement on the byte fetch is not demonstrated.** Replay/recording URLs
   are consumed by the frontend directly (`<a href>`, `<video src>`, `window.open`);
   `MediaProxyService` serves *media-library* resources, not live-class recordings. Listing
   endpoints are `@PreAuthorize`-gated, so an unauthorized user never learns the URL — but
   that is URL-secrecy, not a per-request entitlement check. Decide the playback model after
   §10 Step 5 shows what `location` actually is.
3. **`docker-compose.yml` contains committed credentials** (a shared `JWT_SECRET`, the
   database password, MinIO root credentials, RabbitMQ `guest`) for the local stack. They are
   well-known development values, not production secrets, but they are in a tracked file.
   Replace them from a secret store before any shared or production deployment.
4. **The `prod` profile is not activated anywhere in the repository.** Its overrides (Redis/
   RabbitMQ hosts, Flyway seed deactivation) were previously inert due to a YAML indentation
   defect that has now been corrected — so the first person to enable `prod` should review
   those values deliberately rather than inheriting dev defaults.

---

*No credential is contained in this document.*
