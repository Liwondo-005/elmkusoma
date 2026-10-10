# API contract — public contact, site settings, legal content

Owner: **[Developer 01]** (backend). Consumers: **[Developer 02]** (frontend).
Published before frontend integration begins.

All responses use the existing `ApiResponse<T>` envelope (`data`, `success`, `error`).
All paths follow existing conventions (no `/api` prefix). Auth via `Authorization: Bearer`.
Tenant header `X-Institution-Id` is required on every platform-admin route and is
validated against the caller's membership by `OrganizationContextResolver`.

---

## 1. Public — anonymous

### `GET /v1/public/site-settings`

Returns the approved public site configuration as a **flat map of already-approved keys**.

- 200 `ApiResponse<Map<String, String>>`
- Only rows with `is_public = true` **and** `is_sensitive = false` **and** an allow-listed key.
- Values that were never configured are **absent from the map**, not present-and-empty, so the
  client can distinguish "not set" without special-casing `""`.
- Never returns: SMTP credentials, notification recipients, internal ops settings, or any
  `is_sensitive` row. Enforced server-side by an explicit key allow-list, not by trusting the
  `is_public` flag alone.

Keys:

| Key | Purpose |
|---|---|
| `public.contact.email` | Official public contact address |
| `public.contact.supportEmail` | Address the support team answers on |
| `public.contact.phone` | Public phone, international format |
| `public.contact.whatsapp` | Support WhatsApp number, digits only (no `+`, no spaces) |
| `public.contact.address` | Office address |
| `public.contact.workingHours` | Support hours, free text |
| `public.contact.responseTime` | Expected response time, free text |
| `public.social.facebook` | Full URL |
| `public.social.instagram` | Full URL |
| `public.social.youtube` | Full URL |
| `public.social.linkedin` | Full URL |
| `public.social.tiktok` | Full URL |
| `public.social.x` | Full URL |

### `POST /v1/public/contact`

Accepts a public enquiry. **No authentication.**

Request:

| Field | Type | Rules |
|---|---|---|
| `name` | string | required, 2–120 chars |
| `email` | string | required, valid email, ≤ 254 chars |
| `category` | string | required, one of `GENERAL`, `SUPPORT`, `PARTNERSHIP`, `FEEDBACK`, `COMPLAINT` |
| `subject` | string | required, 4–160 chars |
| `message` | string | required, 20–4000 chars |
| `website` | string | honeypot — must be empty; a non-empty value is accepted and silently discarded |
| `formStartedAt` | epoch millis | honeypot — if present and the elapsed time is under 3 seconds, the submission is discarded |

201 `ApiResponse<ContactReceipt>`:

```json
{ "reference": "CNT-2026-000123",
  "status": "NEW",
  "receivedAt": "2026-10-10T09:15:00",
  "notificationStatus": "NOT_CONFIGURED" }
```

- `notificationStatus` is one of `PENDING`, `SENT`, `FAILED`, `NOT_CONFIGURED`.
  It reports what actually happened to the notification attempt. **A stored message is never
  reported as `SENT`** unless a delivery attempt was made.
- 400 invalid input, 429 rate limited (10 per hour per IP), 200-with-generic-ack for honeypot hits
  (so a bot cannot learn the filter fired).
- The message row is committed **before** any notification attempt.

### `GET /v1/public/legal/{type}`

`type` ∈ `TERMS`, `PRIVACY`, `COOKIE`, `SUPPORT_POLICY`.

200 `ApiResponse<LegalDocumentView>`:

```json
{ "type": "TERMS", "version": 3, "title": "Terms & Conditions",
  "content": "…plain text…",
  "effectiveDate": "2026-09-01", "publishedAt": "2026-09-01T10:00:00" }
```

- Returns the **published** version only. Drafts and unpublished revisions are never exposed here.
- 404 when no published version exists, so the frontend can fall back honestly.

### `GET /v1/my/contact-messages` (authenticated, any role)

The caller's own enquiries, matched on their account email. Ownership is enforced in the backend;
a caller can only ever see rows addressed to their own address.

200 `ApiResponse<List<ContactMessageSummary>>` — reference, subject, category, status,
receivedAt, lastUpdateAt, replyCount. **Never includes internal notes.**

---

## 2. Platform Admin — `ADMIN` only

Existing `GET /v1/platform-admin/config?category=` and
`PUT /v1/platform-admin/config/{key}` are **reused unchanged** for the public settings screens.
They already support `category`, `is_public` and `is_sensitive`, and already mask sensitive
values. No parallel settings store is created.

### Contact inbox

| Method | Path | Notes |
|---|---|---|
| GET | `/v1/platform-admin/contact-messages` | `page`, `size`, `status`, `category`, `q` (subject/email/reference substring) |
| GET | `/v1/platform-admin/contact-messages/{id}` | Single message |
| PUT | `/v1/platform-admin/contact-messages/{id}/status?status=` | `NEW`, `IN_PROGRESS`, `AWAITING_RESPONSE`, `RESOLVED`, `CLOSED` |
| PUT | `/v1/platform-admin/contact-messages/{id}/assign` | body `{"assigneeId": "<uuid>"}` |
| GET | `/v1/platform-admin/contact-messages/{id}/replies` | `isInternal` included **only** for `ADMIN` |
| POST | `/v1/platform-admin/contact-messages/{id}/replies` | body `{"message": "...", "internal": false}` |
| POST | `/v1/platform-admin/contact-messages/{id}/notify` | Re-attempts notification **on the same message**. Never creates a second ticket. |

### Legal content management

| Method | Path | Notes |
|---|---|---|
| GET | `/v1/platform-admin/legal-documents?type=` | Working copy + current published version |
| POST | `/v1/platform-admin/legal-documents` | `{type, title, content, effectiveDate}` → creates draft v1 |
| PUT | `/v1/platform-admin/legal-documents/{id}` | Edits the working copy only. Does **not** affect the published version. |
| POST | `/v1/platform-admin/legal-documents/{id}/publish` | Archives the current published version, then publishes the working copy as the next version. |
| GET | `/v1/platform-admin/legal-documents/{id}/versions` | Immutable published history, newest first |
| POST | `/v1/platform-admin/legal-documents/{id}/revert` | body `{"version": n}` — copies old version n's content into the working copy as a **new** draft. History is never rewritten. |
| DELETE | `/v1/platform-admin/legal-documents/{id}` | Removes the working copy and its unpublished state. Published history is retained. |

## 3. Content format

Legal content is **plain text**, not HTML. Paragraphs are separated by blank lines.
No endpoint accepts or returns HTML, and the frontend renders it as React text nodes.
This is deliberate: no HTML sanitiser dependency is available to the backend build, and
refusing HTML outright makes stored XSS structurally impossible instead of relying on a
filter to catch it.

## 4. What this contract does not cover

- No endpoint returns notification recipients, SMTP configuration, or any secret. Those are
  `is_public = false` / `is_sensitive = true` and stay behind `/v1/platform-admin/config`.
- No automated WhatsApp messaging. Click-to-chat links only.