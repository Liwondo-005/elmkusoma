
# ELMKUSOMA — Live Classes & Live Streaming

Team direction & overview for the Live Classes and Live Streaming module of ELMKUSOMA.

> **Note:** This revision supersedes the earlier draft. Key change: ELMKUSOMA Live is now the **sole official live platform** — no normal external-meeting alternative (see §9) — replacing the earlier "external meeting fallback" idea.

## 1. Vision

The goal is **not** to build a basic video-meeting feature similar to Google Meet.

The goal is to build ELMKUSOMA as a **Learning Live & Media Platform**, supporting:

- Online classes / interactive live classrooms
- University lectures, seminars, workshops, webinars
- Professional training, educational events
- Studio-produced educational content
- Live broadcasts, recorded learning content
- Large-scale educational streaming

Experience journey:

```
Teach → Schedule → Go Live → Interact → Broadcast → Record → Store → Replay → Learn → Analyze
```

---

## 2. ELMKUSOMA Live Classroom

The primary live-learning experience.

**Teacher capabilities**
- Schedule live classes; start/end sessions
- Toggle camera / microphone
- Share screen
- View participants; manage learners
- Control learner audio/video permissions
- Live chat; handle raise-hand interactions
- Track live attendance and measurable participation
- Monitor connection/network quality
- Attach lesson materials
- Link sessions to: classes, courses, subjects, lessons, assignments/learning activities
- Send notifications and reminders
- Record sessions when enabled; provide authorized replay access

**Learner capabilities**
- View upcoming live classes; join authorized sessions
- Control camera/microphone per permissions
- Use chat; raise hand
- View shared screens and materials
- Leave and rejoin sessions
- Recover from temporary network interruptions
- Access recordings after the session when authorized

---

## 3. Technology Direction

**WebRTC + LiveKit** — do not build the full realtime video stack from scratch without a specific technical reason.

**LiveKit — realtime media layer**
- Audio, video, screen sharing
- Participants, realtime rooms, media routing
- Ingress / egress
- Recording workflows

**Spring Boot (existing backend) — business & control layer**
- Authentication & authorization
- Teacher ownership; class/enrollment verification
- Live-session management & lifecycle
- Attendance, permissions, notifications
- Recording metadata, analytics
- Database integration
- **Live-service state** and **incident/status management**

> ⚠️ Spring Boot must **not** become the video/media server.

**Next.js — frontend**
- Teacher Live Studio
- Teacher Live Classroom
- Learner Live Classroom
- Live-session controls / participant experience
- Media Library
- Recording/replay experience

---

## 4. Professional Camera & Studio Support

Not limited to a laptop webcam — the architecture must be **source-agnostic**:

- Smartphones, laptop webcams, USB/DSLR/mirrorless/professional cameras
- HDMI / SDI cameras
- External microphones, audio interfaces, audio mixers
- OBS, hardware encoders, professional studio equipment

```
Camera → Capture Card → OBS → RTMP/WHIP/SRT → ELMKUSOMA
Camera → Professional Studio/Encoder → ELMKUSOMA
```

---

## 5. ELMKUSOMA Broadcast / Studio Mode

A professional studio could contain: Camera 1/2/3, Presentation, Screen, Teacher, Guest, External audio.

Producer-switchable scenes: Camera, Presentation, Camera + Slides, Interview, Q&A, Closing, Branded educational broadcast.

**Two levels of experience**

| Level | Tools |
|---|---|
| Standard users | Phone, laptop, webcam, browser |
| Professional users | OBS, capture cards, mixers, switchers, professional cameras, hardware encoders, studio workflows |

---

## 6. Recording & Media Architecture

Not just "record meeting → download MP4" — a proper learning-media workflow:

```
Live Session → Recording → Processing → Storage → Media Library → Replay
```

Recordings maintain educational context, belonging to: course, lesson, class, subject, live session, teacher, learning activity, institution.

- Video files are **not** stored in PostgreSQL — PostgreSQL holds metadata and relationships only.
- Media uses object storage, media processing, and video delivery infrastructure.

Architecture must be ready for: master & composite recordings, separate teacher video / screen share / audio tracks (multi-track where supported), replay, thumbnails, video metadata, **captions**, access control, download permissions.

---

## 7. High-Quality Video

Support professional-quality video without forcing every learner to receive maximum quality. Delivery adapts to device, network, bandwidth, connection quality, and streaming conditions — strong connections get higher quality, weak connections get a lower-quality stream instead of session failure. Production stays high quality; learner delivery stays adaptive and reliable.

---

## 8. Large-Scale Live Streaming

| Mode | Shape |
|---|---|
| Interactive Live Classroom | Teacher ↔ Learners |
| Large-Scale Broadcast | Teacher/Studio → Large Audience |

Must expand to: webinars, public lectures, university events, large seminars, conferences, educational broadcasts, public learning events.

Future dedicated streaming/CDN infrastructure (e.g. Amazon IVS or equivalent) can be introduced when required — **today's system must not block this future capability**, even though it need not be implemented immediately.

---

## 9. No Normal External Meeting Alternative

**Policy change from earlier drafts:** ELMKUSOMA Live remains the **primary and official** live platform. Teachers are **not** given normal options such as Google Meet, Zoom, or Microsoft Teams as alternative platforms for their classes — the goal is not to encourage users to leave ELMKUSOMA.

> If a teacher schedules a live class on ELMKUSOMA, ELMKUSOMA Live **is** the classroom.

This keeps the experience: consistent, centralized, measurable, secure, and integrated with attendance, learning, recordings, analytics, courses, and lessons.

---

## 10. Live Service Reliability & Incident Handling

ELMKUSOMA Live must still be designed for reliability, with health monitoring of the live infrastructure.

**Possible live-session states**
- `SCHEDULED`
- `STARTING`
- `LIVE`
- `ENDING`
- `ENDED`
- `SERVICE_DEGRADED`
- `SERVICE_UNAVAILABLE`
- `RECOVERING`

**Teacher-facing message (example)**
> ELMKUSOMA Live is temporarily unavailable — "We are experiencing a temporary technical issue with the live service. Your scheduled class remains active. We are working to restore the service."

**Learner-facing message (example)**
> Live class temporarily unavailable — "The ELMKUSOMA Live service is currently experiencing a technical issue. Please remain available. You will be notified when the classroom is available."

Status must update automatically on recovery. Never fake availability or pretend a session is live when infrastructure is actually unavailable.

---

## 11. Live Issue Reporting

Reportable issues: cannot join, camera problem, microphone problem, poor video quality, connection problem, live service unavailable, recording problem, audio problem, other — feeding a **Live Incident / Support Workflow**.

Production monitoring (admins/operators) should cover: live service health, active rooms, participants, connection quality, recording status, failed sessions, service errors, realtime infrastructure status.

---

## 12. Attendance & Participation

```
Learner joins → participant detected → attendance recorded → join/leave events tracked → duration calculated where appropriate
```

Measurable analytics: participant count, join/leave events, attendance, participation duration, session duration, connection quality, engagement indicators (where technically measurable).

> ⚠️ Never create fake analytics. Only record what the system can actually measure.

---

## 13. Security & Authorization

```
Login → Role → Class/Enrollment Verification → Session Authorization → Short-Lived Live Token → Join
```

Backend remains the sole authority — knowing a room ID must never be sufficient to join.

Must account for: teacher ownership, learner authorization, institution boundaries, session permissions, recording access control, download permissions, token expiration, session expiration, stream-key protection, audit logs, abuse protection.

---

## 14. High-Level Architecture

```
                         ELMKUSOMA
                             │
                    ┌────────▼────────┐
                    │     Next.js     │
                    │                 │
                    │ Teacher Studio  │
                    │ Live Classroom  │
                    │ Learner Room    │
                    │ Media Library   │
                    │ Video Player    │
                    └────────┬────────┘
                             │
                     Spring Boot Core
                             │
        ┌────────────────────┼─────────────────────┐
        │                    │                     │
   PostgreSQL              Redis               Security
        │
        ├── Live Sessions
        ├── Attendance
        ├── Recordings Metadata
        ├── Courses
        ├── Lessons
        ├── Classes
        ├── Media
        ├── Permissions
        └── Analytics
        │
        ▼
      LiveKit
        │
        ├── WebRTC
        ├── Realtime Media
        ├── Rooms
        ├── Ingress
        └── Egress
        │
        ├───────────────┐
        ▼               ▼
 Interactive       Professional
 Classroom         Studio Input
                       │
                 OBS / Hardware
                       │
              RTMP / WHIP / SRT
                       │
                       ▼
               Streaming Layer
                       │
                       ▼
              Large-scale Broadcast
                       │
                       ▼
                Media Processing
                       │
                 Object Storage
                       │
                       ▼
              ELMKUSOMA Media Library
```

---

## 15. Media Library

The ELMKUSOMA Media Library becomes a central learning-media system, eventually supporting: live recordings, recorded lessons, course videos, lecture recordings, webinars, workshops, educational events, studio-produced content, tutorials, replays, thumbnails, captions, search, filtering, access control.

**Example `MediaAsset` shape**

```
MediaAsset
├── title
├── description
├── thumbnail
├── duration
├── playback information
├── media type
├── course
├── lesson
├── class
├── subject
├── teacher
├── institution
├── liveSession
├── visibility
├── accessPolicy
├── captions
├── tags
├── createdAt
└── status
```

---

## 16. Implementation Principle

This is **not** a request to blindly create another live system.

Before implementation, the assigned developer must inspect the existing ELMKUSOMA implementation:
- Existing live classes & live-class database tables
- Existing APIs
- Existing WebSocket/realtime functionality
- Existing authentication & authorization
- Existing attendance
- Existing notifications
- Existing recording & media functionality
- Existing course / class / lesson relationships
- Existing frontend routes & components
- Existing infrastructure

| Situation | Action |
|---|---|
| Exists and works | Reuse and improve |
| Exists but incomplete | Complete it |
| Broken | Fix it |
| Missing | Implement it |
| Duplicated | Consolidate — reuse the existing system |

**Do not create duplicate**: Live Class, Attendance, Media, Course, Class, Authentication, or Notification systems.

---

## 17. Development Priorities

**Phase 1 — Core Live Classroom**
- Live session management; LiveKit integration
- Teacher classroom; Learner classroom
- Authorization; participant management
- Camera/microphone; screen sharing
- Chat; raise hand
- Attendance; reconnection handling

**Phase 2 — Learning Integration**
- Course / class / subject / lesson / assignment integration
- Materials; notifications; session history

**Phase 3 — Recording & Media**
- Recording; processing; media metadata; storage
- Replay; Media Library; access control
- Thumbnails; captions where supported

**Phase 4 — Professional Studio**
- OBS integration; ingress
- Professional camera workflows; capture cards
- External audio; studio production workflows
- Hardware encoder compatibility

**Phase 5 — Scale & Broadcast**
- Large-scale streaming architecture; CDN/streaming infrastructure
- Broadcast mode; public events; webinars
- Large educational broadcasts; advanced media analytics

---

## 18. Final Product Direction

> The goal is **not** "Build an ELMKUSOMA version of Google Meet."
> The goal **is** "Build ELMKUSOMA as a Learning Live & Media Platform."

Full journey:

```
Teach → Schedule → Go Live → Interact → Broadcast → Record → Store → Replay → Learn → Analyze
```

Across all environments:

```
Phone → Laptop → Webcam → Professional Camera → OBS → Studio → Broadcast Workflow
```

Long-term platform scope: online teaching, interactive classrooms, university lectures, seminars, workshops, webinars, professional training, educational broadcasts, studio-produced educational content, recorded courses, live events, large-scale learning broadcasts.

The architecture must integrate with the existing ELMKUSOMA ecosystem without breaking: Student, Teacher, Parent, Admin, Institution, Learning, Assessment, Grading, Attendance, Certificate, Course, Class, Notification, and other existing workflows.

**ELMKUSOMA Live should become a core learning infrastructure of the platform — not simply another video-meeting feature.**
