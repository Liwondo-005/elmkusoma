# Live Broadcast Sources — Architecture, Boundaries & Operations

> Part of the ELMKUSOMA live streaming enhancement. This document describes how a
> teacher broadcasts from different sources, what is actually implemented (with the
> exact configuration it requires), and the hard boundaries of the system.

## 1. Source model

Every live class carries an optional **broadcast source** — where the teacher's
video feed comes from. It is a single additive column:

| Column | Values | Default |
|---|---|---|
| `live_classes.broadcast_source` (V90) | `BROWSER`, `MOBILE`, `USB_CAMERA`, `PROFESSIONAL_CAMERA`, `OBS`, `ENCODER`, `STUDIO`, `OTHER` | `BROWSER` |

- Null/blank (rows that pre-date V90) reads as `BROWSER` — identical to previous behaviour.
- Unknown values are rejected with `400` and the supported list (never silently coerced).
- The source is metadata + routing intent. **The media plane is unchanged**: everything
  ends up in the same LiveKit room that LiveKit already served before this feature.

## 2. Source paths (what actually happens)

| Source | How it reaches the room | Backend dependency |
|---|---|---|
| `BROWSER` (laptop camera) | Browser `getUserMedia` → existing LiveKit WebRTC publish | none beyond LiveKit server |
| `MOBILE` (phone camera) | Same WebRTC publish from the phone's browser (responsive control room + classroom) | none beyond LiveKit server |
| `USB_CAMERA` | Browser `getUserMedia({ deviceId })` (device pickers enumerate USB webcams) → LiveKit publish; switch mid-session republishes the track | none beyond LiveKit server |
| `PROFESSIONAL_CAMERA` | Camera via capture card / NDI-to-browser / HDMI USB device — same `deviceId` selection path as USB | none beyond LiveKit server |
| `OBS`, `ENCODER`, `STUDIO` | OBS/encoder pushes **WHIP/RTMP/SRT** to a LiveKit **Ingress** input, which publishes into the same room as a participant | **requires the `livekit-ingress` service** (see §4) |

The laptop camera is never required: any source above can be selected in the Live
Control Room, and preview/mic checks only touch the devices actually chosen.

## 3. API surface (additive, all new)

### Ingest (external sources → LiveKit ingress)

All three endpoints sit behind the same authorization model as the recording endpoints:
role `TEACHER | INSTITUTION_ADMIN | ADMIN` **plus** institution match **plus** ownership
(the class must belong to the caller's teacher profile, admins fall back to their role).

| Method & path | Behaviour |
|---|---|
| `GET  /v1/live-session/classes/{classId}/ingress` | `{ configured, ingresses[], message }` — live state from LiveKit `ListIngress` |
| `POST /v1/live-session/classes/{classId}/ingress` | body `{ "protocol": "WHIP" \| "RTMP" \| "SRT" }` → `{ inputUrl, streamKey, … }` (create) |
| `DELETE /v1/live-session/classes/{classId}/ingress` | removes all ingress inputs for the class → `{ removed, configured }` |

**Honest states, never fake endpoints:**

- `livekit.ingress.enabled=false` (the default, and the value in test/dev config):
  `GET` → `200 { configured:false, ingresses:[], message:"…not configured…" }`,
  `POST` → `503` with the same explanation, `DELETE` → `200 { removed:0, configured:false }`.
- Enabled but LiveKit unreachable → `POST` returns a real failure (no invented URL or key).
- The stream key is returned **only** to the authorized teacher/admin, over the
  authenticated API (same pattern as the Twitch/YouTube destination keys). It is never
  present in the frontend build environment.

State is deliberately **not persisted**: LiveKit `ListIngress` is the source of truth.

### WebSocket (existing live-class socket, new message types)

| Direction | Type | Notes |
|---|---|---|
| teacher → server | `SOURCE_UPDATE` `{source, label}` | teacher-gated; announced + audited |
| server → room | `SOURCE_CHANGED` `{source, label, changedBy}` | drives the live "Source" badge |
| teacher → server | `QA_MARK_ANSWERED` `{messageId}` | toggles answered/open on a Q&A message |
| server → room | `QA_ANSWERED` `{messageId, answered, answeredBy}` | live Answered badge for everyone |

Both actions are persisted as `live_class_session_events` rows (`SOURCE_CHANGED`,
`QA_ANSWERED`) for observability.

### Chat/Q&A persistence (V91, additive)

`live_class_chat_messages.answered_at` / `answered_by` (NULL = open question).
`CHAT_MESSAGE` frames now carry the persisted `id` (older clients ignore the field),
and `CHAT_HISTORY` carries `answeredAt`/`answeredBy`.

## 4. Deployment requirement for external ingest (RTMP/WHIP/SRT)

The repo's `docker-compose.livekit.yml` runs **only** `livekit-server`, and
`livekit.yaml` has no `ingress:` section. External ingest therefore reports
`configured:false` until an operator deploys the ingress side:

1. Add the ingress container (image `livekit/ingress`) beside `livekit-server`,
   configured with the same API key/secret (`devkey` / … in dev).
2. Add an `ingress:` block to `livekit.yaml` (input endpoints, e.g. WHIP on 7880,
   RTMP/SRT on their standard ports) and restart.
3. Set `livekit.ingress.enabled=true` in the backend environment.

Only then does `POST …/ingress` return real URLs/keys. Until then the UI shows an
honest "not configured" panel with the same guidance — no placeholder endpoints.

## 5. Explicit boundaries (honest list)

| Boundary | Status |
|---|---|
| Native mobile app (iOS/Android) | **NOT IMPLEMENTED** — mobile = phone browser (responsive UI). No store app, no push-to-broadcast from an OS camera app. |
| RTMP/WHIP/SRT ingest | **IMPLEMENTED against LiveKit's API, BLOCKED on deployment** — requires the `livekit-ingress` service (§4); guarded by `livekit.ingress.enabled`. |
| SRT ingest | Passed through to LiveKit ingress as `SRT_INPUT` (transcode on). No independent SRT stack; actual support depends on the deployed ingress version. |
| Studio | Not a separate media stack — `STUDIO` is a source label using the same ingest path as OBS/encoder (§2). |
| Re-encoding / transcoding quality policy | Delegated to LiveKit ingress defaults (`transcode:true`); no custom ladder configured. |
| Recording / replay of ingested streams | Unchanged — the existing egress configuration records whatever participant publishes to the room, including an ingress participant. Egress storage rules (`livekit.egress.hasStorage()`) are untouched. |
| Viewer-side source selection | Learners always receive the room's mixed output; they cannot choose a source. |
| Multi-bitrate ABR variants, CDN egress (HLS/DASH out) | **NOT IMPLEMENTED** (no egress output targets beyond what already existed). |

## 6. Compatibility

- **Schema**: V90 + V91 are additive (`ADD COLUMN IF NOT EXISTS`), no rewrites/drops.
- **API**: no existing endpoint changed shape; new fields (`broadcastSource`,
  `answeredAt`, chat `id`) are optional additions.
- **Behaviour defaults**: no source field ⇒ `BROWSER`; ingress flag off ⇒ previous
  behaviour exactly (there was no ingest before).
- **Security**: role fences unchanged except that `DELETE /v1/teachers/me/**` now
  matches its TEACHER-only controllers (live-class cancel / announcement delete had
  been over-blocked by the broader admin fence).
