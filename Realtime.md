ELMKUSOMA — ULTIMATE LIVE STREAMING, REAL-TIME INTERACTION & MEDIA PLATFORM ENHANCEMENT MASTER PROMPT

EXTEND + MODIFY + HARDEN EXISTING LIVE SYSTEM

NEVER REBUILD • NEVER DUPLICATE • NEVER BREAK EXISTING FUNCTIONALITY

REAL-TIME INTERACTION • PHONE • BROWSER • PROFESSIONAL CAMERA • OBS • ENCODER • STUDIO • LIVEKIT • INGRESS • EGRESS • RECORDING • MEDIA • REPLAY

---

001. MISSION

You are working inside the existing production ELMKUSOMA repository.

Your mission is NOT to create a new Live Streaming application.

Your mission is to:

«INSPECT → UNDERSTAND → AUDIT → PROTECT → MODIFY → EXTEND → INTEGRATE → HARDEN → TEST → VERIFY»

the existing ELMKUSOMA Live ecosystem.

The final result must be a significantly more capable, reliable, real-time and production-ready:

«ELMKUSOMA LIVE — LEARNING LIVE & MEDIA PLATFORM»

without destroying, replacing, duplicating or unnecessarily restructuring the functionality that already exists.

---

002. ABSOLUTE RULE — EXISTING SYSTEM FIRST

This is the most important instruction in this entire task.

DO NOT REBUILD ELMKUSOMA LIVE.

Do not create a replacement Live system.

Do not create a second Live architecture.

Do not create duplicate:

- LiveClass
- LiveSession
- LiveRoom
- LiveKit integration
- WebRTC integration
- Chat
- Q&A
- Poll
- Quiz
- Raise Hand
- Attendance
- Recording
- Media
- Resource
- Lesson
- Enrollment
- Authentication
- Authorization
- Notification
- Search
- Analytics
- Admin
- Institution/School scope
- learner system
- teacher system

If an equivalent already exists, EXTEND IT.

Before creating any class, entity, service, API, component, route, event, table or subsystem, search the repository and prove that an equivalent does not already exist.

---

003. GOLDEN RULE

The existing ELMKUSOMA implementation is the foundation.

We are doing:

«MODIFICATION + EXTENSION + INTEGRATION + HARDENING»

NOT:

«REBUILD + REPLACE + DUPLICATE»

Every new capability must be attached to the safest existing extension point.

---

004. REQUIRED FIRST ACTION — INSPECT EVERYTHING

Before modifying code, inspect the existing repository.

Inspect at minimum:

Backend

- LiveClass
- LiveSession
- LiveRoom
- LiveKit configuration
- LiveKit token generation
- participant management
- attendance
- chat
- Q&A
- polls
- quizzes
- raise hand
- breakout rooms
- screen sharing
- recording
- media
- resource library
- lessons
- courses
- enrollment
- authentication
- authorization
- permissions
- institution_id
- jurisdiction/scope
- notifications
- WebSocket
- STOMP
- SSE
- Redis
- Pub/Sub
- event publishers
- event consumers
- scheduled jobs
- async processing
- media processing
- storage
- existing APIs
- DTOs
- services
- repositories
- Flyway migrations
- configuration
- exception handling
- tests

Frontend

Inspect:

- Live routes
- Teacher Live UI
- Learner Live UI
- Live Control Room
- participant components
- LiveKit hooks
- room state
- participant state
- chat
- Q&A
- poll
- quiz
- breakout room UI
- screen sharing
- resource panel
- attendance indicators
- notifications
- WebSocket/SSE/event hooks
- API clients
- React state management
- caching
- query invalidation
- reconnect handling
- loading/error states
- mobile responsiveness
- existing design system

Infrastructure

Inspect:

- LiveKit server
- Redis
- LiveKit Ingress
- LiveKit Egress
- environment variables
- storage
- media configuration
- Docker/config files
- local development configuration
- production configuration where available

---

005. MANDATORY EXISTING-SYSTEM CLASSIFICATION

Before implementation, classify every relevant area as:

- VERIFIED EXISTING
- PARTIAL
- BROKEN
- MISSING
- DUPLICATED
- INCONSISTENT
- BLOCKED
- UNSUPPORTED
- NOT APPLICABLE

Do not call something MISSING merely because you did not find it immediately.

Search thoroughly first.

For every important conclusion provide evidence:

- file
- class/component
- method
- API
- route
- migration
- configuration
- test
- runtime evidence

Use:

«REQUIREMENT / FACT / VERIFIED / ASSUMPTION / INFERENCE»

where appropriate.

---

006. KNOWN EXISTING ELMKUSOMA LIVE CONTEXT

Do not assume the project is empty.

The existing ecosystem already includes Live-related functionality around:

- LiveKit/WebRTC
- teacher Live creation/start flow
- learner joining
- Live session lifecycle
- attendance
- chat
- Q&A
- polls
- quizzes
- raise hand
- screen sharing
- recording/media integration
- existing Live UI
- existing authentication/authorization
- existing institution scope

There have also been known/pre-existing limitations such as:

- SHARED_MEDIA backend gap
- LiveKit Egress not enabled in some environment configuration
- Redis not available in some local environment
- previous backend process running old code

These must be VERIFIED against the current repository/runtime.

Do not automatically treat them as new bugs introduced by this task.

---

007. PRODUCT DIRECTION

ELMKUSOMA LIVE IS NOT:

- Zoom clone
- Google Meet clone
- Microsoft Teams clone
- generic webinar software
- generic OBS clone
- generic YouTube Live clone

ELMKUSOMA LIVE IS:

«A Learning Live & Media Platform»

Core lifecycle:

Teach
↓
Schedule
↓
Prepare
↓
Go Live
↓
Interact
↓
Broadcast
↓
Record
↓
Store
↓
Replay
↓
Learn
↓
Analyze

Live is connected to:

- Courses
- Subjects
- Lessons
- Resources
- Learners
- Teachers
- Enrollment
- Attendance
- Assessment
- Progress
- Notifications
- Media
- Resource Library
- Analytics

---

008. SOURCE PRINCIPLE

The teacher must be able to:

«Broadcast from wherever they are, using the equipment they have.»

Do NOT make a laptop camera mandatory.

The unified source model should support, where technically available:

MOBILE
BROWSER
USB_CAMERA
PROFESSIONAL_CAMERA
CAPTURE_CARD
OBS
ENCODER
STUDIO
OTHER_COMPATIBLE_SOURCE

These are NOT separate Live systems.

They are different inputs into the same ELMKUSOMA Live ecosystem.

---

009. UNIFIED LIVE SOURCE ARCHITECTURE

Target concept:

PHONE
   │
BROWSER
   │
USB CAMERA
   │
PROFESSIONAL CAMERA
   │
CAPTURE CARD
   │
OBS
   │
ENCODER
   │
STUDIO
   │
OTHER SOURCE
   │
   ▼
UNIFIED LIVE SOURCE PIPELINE
   │
   ▼
LIVEKIT / EXISTING LIVE INFRASTRUCTURE
   │
   ▼
ELMKUSOMA LIVE
   │
   ▼
LEARNERS

Do not implement every source as a separate product.

Create/reuse a unified abstraction only where the current architecture requires it.

---

010. PHONE / MOBILE LIVE

Teacher must be able to use a phone where the existing architecture supports it.

Support the existing mobile/web path without creating a duplicate teacher system.

Mobile requirements:

- camera
- microphone
- front/back camera where technically supported
- camera switching
- mute/unmute
- network change handling
- reconnect
- connection status
- battery/network awareness where appropriate
- responsive Live Control experience

Learners must also receive the same Live session without needing a separate Live system.

---

011. BROWSER / LAPTOP SOURCE

Browser camera and microphone remain supported.

But:

«Laptop camera is OPTIONAL, not mandatory.»

A teacher should be able to use:

- laptop camera
- external USB camera
- screen
- presentation
- external microphone
- capture card

where supported by the existing browser/device capabilities.

---

012. PROFESSIONAL CAMERA

Support professional camera workflows through existing compatible infrastructure.

Expected conceptual path:

PROFESSIONAL CAMERA
↓
CAPTURE CARD / ENCODER
↓
OBS / ENCODER
↓
LIVEKIT INGRESS
↓
ELMKUSOMA LIVE

Do not fake professional-camera support.

The implementation must reflect actual device/input capabilities.

---

013. OBS INTEGRATION

OBS is a production source/tool, not a replacement for ELMKUSOMA Live.

Expected path where applicable:

OBS
↓
RTMP / RTMPS / WHIP / SRT
↓
LIVEKIT INGRESS
↓
LIVEKIT ROOM
↓
ELMKUSOMA LIVE

Support actual configuration.

Do not create fake “Connect OBS” UI that does not correspond to a working ingest path.

If LiveKit Ingress is not currently configured:

1. identify what is missing;
2. document it;
3. implement integration safely if supported by the repository/environment;
4. provide exact configuration requirements;
5. do not claim VERIFIED until actually tested.

---

014. ENCODER SUPPORT

Where technically appropriate, support compatible encoders.

Examples:

- hardware encoder
- software encoder
- external encoder
- studio encoder

Do not create arbitrary protocols just to satisfy UI requirements.

Use protocols actually supported by the infrastructure.

---

015. STUDIO SUPPORT

ELMKUSOMA Studio is not a separate platform.

Studio is a production source/workflow that feeds ELMKUSOMA Live.

Target:

CAMERAS
SCREEN
PRESENTATION
AUDIO
GRAPHICS
OTHER INPUTS
      ↓
STUDIO / PRODUCTION
      ↓
ENCODER / OBS
      ↓
INGRESS
      ↓
ELMKUSOMA LIVE

---

016. TEACHER LIVE CONTROL ROOM

Improve the existing Teacher Live experience rather than replacing it.

It should become a real learning broadcast control room.

Where applicable, provide:

Broadcast controls

- source selection
- camera
- microphone
- screen share
- presentation
- external source
- OBS/encoder source
- professional camera source
- preview
- connection status
- quality status

Learning controls

- chat
- Q&A
- poll
- quiz
- raise hand
- resources
- lesson context
- participant list
- attendance
- breakout rooms

Session controls

- start
- pause where supported
- resume
- end
- recording status
- participant count
- network health

Do not remove existing working controls.

---

017. LEARNER EXPERIENCE

Learner should experience a:

«Learning Live session»

not a generic meeting.

The learner should be able to:

- watch Live
- chat
- ask questions
- answer polls
- participate in quizzes
- raise hand
- access shared resources
- see lesson context
- participate in breakout rooms where authorized
- reconnect
- continue after network changes
- receive real-time session changes

---

018. CRITICAL ISSUE — REAL-TIME INTERACTION

This is a P0 requirement.

Current problem:

«Teacher starts a poll/quiz/breakout-room action, but learner only sees the change after manually refreshing.»

This is NOT acceptable for a real Live learning platform.

The solution must NOT be:

setInterval(fetch every 3 seconds)

unless there is a specific fallback reason.

We need proper event-driven real-time synchronization.

---

019. REAL-TIME TARGET ARCHITECTURE

Target:

TEACHER
   ↓
ACTION
   ↓
SPRING BOOT
   ↓
AUTHORITATIVE STATE
   ↓
PERSIST
   ↓
REAL-TIME EVENT
   ↓
CONNECTED LEARNERS
   ↓
FRONTEND EVENT HANDLER
   ↓
UI UPDATE

No manual refresh.

---

020. INSPECT EXISTING REAL-TIME TECHNOLOGY FIRST

Before introducing anything new, inspect whether the project already uses:

- WebSocket
- STOMP
- SSE
- LiveKit Data Messages
- Redis Pub/Sub
- application events
- event publishers
- event consumers
- realtime notification services
- existing room events
- frontend subscriptions

Reuse the most appropriate existing mechanism.

Do NOT blindly install another WebSocket library.

Do NOT create a second realtime architecture if one already exists.

---

021. REAL-TIME STATE + EVENT MODEL

Do not rely on events alone.

Backend remains the authoritative source of truth.

Use:

«EVENT = FAST PROPAGATION
STATE = AUTHORITATIVE TRUTH»

Example:

Poll
status = ACTIVE
sessionId = ...
question = ...
startedAt = ...

Then emit:

POLL_STARTED

Connected learners update immediately.

If a learner reconnects later, they fetch/synchronize the authoritative current state.

---

022. REAL-TIME EVENT CATEGORIES

Design/reuse an event model capable of supporting events such as:

POLL_STARTED
POLL_UPDATED
POLL_CLOSED

QUIZ_STARTED
QUIZ_QUESTION_CHANGED
QUIZ_PAUSED
QUIZ_RESUMED
QUIZ_ENDED

BREAKOUT_CREATED
BREAKOUT_UPDATED
BREAKOUT_ASSIGNED
BREAKOUT_STARTED
BREAKOUT_ENDED

RAISE_HAND
HAND_LOWERED

CHAT_MESSAGE
Q_AND_A_CREATED
Q_AND_A_UPDATED

SCREEN_SHARE_STARTED
SCREEN_SHARE_STOPPED

RESOURCE_SHARED
LESSON_UPDATED

LIVE_STARTED
LIVE_ENDED

PARTICIPANT_JOINED
PARTICIPANT_LEFT

RECORDING_STARTED
RECORDING_STOPPED

Only implement events required by the existing functionality and task.

Do not create unnecessary abstractions.

---

023. POLL REAL-TIME REQUIREMENTS

Teacher starts Poll.

All connected authorized learners must see it without refresh.

Teacher closes Poll.

Learners must see closed state without refresh.

Teacher updates Poll where supported.

Learners must receive update immediately.

Persist authoritative state.

Handle reconnect.

---

024. QUIZ REAL-TIME REQUIREMENTS

Teacher starts Quiz.

Learners receive it immediately.

Teacher changes question.

Learners receive the new question without refresh.

Teacher pauses/resumes/ends.

Learners receive state changes immediately.

If quiz progress/state already exists, preserve it.

Do not create duplicate quiz systems.

---

025. BREAKOUT ROOMS — CRITICAL

Breakout Rooms must be genuinely real-time.

Teacher creates rooms.

Learners receive room state without refresh.

Teacher assigns learner.

Learner receives assignment immediately.

Teacher moves learner.

Learner receives updated assignment immediately.

Teacher starts breakout.

Learners receive transition/state.

Teacher ends breakout.

Learners return/synchronize correctly.

Handle:

- reconnect
- room state synchronization
- assignment state
- participant state
- authorization
- room lifecycle

Do not implement fake breakout rooms where the UI changes but the underlying room/session state does not.

---

026. REAL-TIME RECONNECTION

Example:

LIVE
 ↓
NETWORK LOST
 ↓
DISCONNECTED
 ↓
RECONNECT
 ↓
SYNC CURRENT SESSION STATE
 ↓
CONTINUE

After reconnect, learner must not remain stuck with stale:

- poll
- quiz
- breakout
- chat
- Q&A
- participant
- session
- resource
- recording state

Use authoritative backend/session state to resynchronize.

---

027. EVENT ORDERING & CONSISTENCY

Handle rapid events safely.

Example:

Teacher:
Start Poll
↓
Update Poll
↓
Close Poll

Learners must not end up showing:

ACTIVE

after receiving:

CLOSED

Use appropriate event IDs/version/timestamps/sequence mechanisms if required by the current architecture.

Do not over-engineer unless evidence requires it.

---

028. LIVEKIT

Preserve the existing LiveKit architecture.

LiveKit remains the media/realtime media foundation where already integrated.

Spring Boot remains the authoritative control plane for:

- authentication
- authorization
- enrollment
- session lifecycle
- permissions
- attendance
- notifications
- metadata
- application state

Do not move authoritative business logic into the frontend.

---

029. LIVEKIT SECURITY

Never expose LiveKit API secrets in the frontend.

Use short-lived tokens.

Preserve existing authorization.

Validate:

- user
- role
- permission
- institution
- jurisdiction
- resource ownership
- session membership

before issuing/accessing Live capabilities.

---

030. ATTENDANCE

Attendance must represent actual Live participation.

Do not count page-open as attendance.

Where already implemented, preserve and harden:

JOIN
↓
PARTICIPATE
↓
LEAVE
↓
DURATION

Handle reconnects correctly.

Do not double-count reconnects as new attendance unless existing business rules explicitly require it.

---

031. NETWORK RESILIENCE

Design for real-world network conditions.

Especially:

- mobile data
- Wi-Fi
- unstable connections
- low bandwidth
- network switching
- temporary packet loss
- reconnect
- high latency

Target:

GOOD
↓
HIGHER QUALITY

MEDIUM
↓
ADAPTIVE QUALITY

WEAK
↓
LOWER QUALITY

DISCONNECTED
↓
RECONNECT

RECONNECTED
↓
STATE RESYNC

Do not fake adaptive quality if the underlying technology does not support it.

---

032. TEACHER CONNECTION HEALTH

Where metrics are actually available, show useful status such as:

- connection state
- latency
- packet loss
- video quality
- audio quality
- participant count
- recording state

Never invent metrics.

If a metric is unavailable, do not display fake numbers.

---

033. LEARNER CONNECTION EXPERIENCE

Learner should receive clear states:

- Connecting
- Live
- Reconnecting
- Connected
- Degraded
- Disconnected
- Recovered

Avoid confusing blank video or silent failure.

---

034. RECORDING / EGRESS

Do not create a second recording system.

Inspect existing recording implementation.

If LiveKit Egress is already integrated, extend it.

If Egress is missing/disabled:

- identify exact blocker;
- configure only where appropriate;
- document environment requirements;
- test it;
- do not claim completion until verified.

Target:

LIVEKIT ROOM
↓
EGRESS
↓
RECORDING
↓
STORAGE
↓
MEDIA ASSET
↓
RESOURCE LIBRARY
↓
LESSON
↓
REPLAY

---

035. RECORDING FAILURE ISOLATION

Recording failure must not automatically crash an active Live session.

Example:

LIVE
+
RECORDING FAILS
↓
LIVE CONTINUES
↓
SYSTEM REPORTS RECORDING FAILURE

Likewise:

STORAGE FAILURE
↓
LIVE SHOULD NOT AUTOMATICALLY DIE

where architecture permits.

---

036. MEDIA ASSET INTEGRATION

Do not save large media files directly inside PostgreSQL as blobs unless existing architecture explicitly requires and supports it.

Use existing media/storage architecture.

Recording should become a reusable media asset where appropriate.

---

037. RESOURCE LIBRARY INTEGRATION

A completed recording should be capable of becoming:

«Resource Library asset»

without creating a duplicate media repository.

Support:

Live Recording
↓
Media Asset
↓
Resource Library
↓
Lesson
↓
Course
↓
Learner Replay

Preserve existing Resource Library implementation.

---

038. LESSON INTEGRATION

Teacher should be able to connect Live learning with lessons.

Concept:

Course
↓
Lesson
↓
Live Class
↓
Recording
↓
Replay Resource

Do not create duplicate Lesson/Resource models.

---

039. MULTI-CAMERA / PRODUCTION

Where infrastructure supports it, enable professional broadcast workflows:

- multiple cameras
- screen
- presentation
- document
- whiteboard
- media
- audio
- source switching

This should be implemented through existing Live/Media architecture.

Do not turn ELMKUSOMA into a generic OBS clone.

---

040. AUDIO QUALITY

Inspect existing audio implementation and improve where necessary.

Consider:

- microphone selection
- mute/unmute
- gain
- noise suppression
- echo handling
- audio/video sync
- external audio devices

Only implement what is technically supported by the existing stack.

---

041. MOBILE UX

Ensure Live experiences work properly on mobile.

Teacher mobile:

- source selection
- camera
- microphone
- start/end
- chat
- Q&A
- poll
- quiz
- participants
- connection state

Learner mobile:

- video
- interaction
- poll
- quiz
- chat
- Q&A
- raise hand
- breakout
- resources
- reconnect

Do not create duplicate mobile Live logic if responsive existing logic can be extended.

---

042. PLATFORM ADMIN / INSTITUTION ADMIN

Respect existing scope.

Platform Admin should have platform-level visibility/control according to existing authorization.

Institution/School Admin should only operate within authorized institution scope.

Do not bypass:

- institution_id
- jurisdiction
- role
- permission
- resource ownership

---

043. AUTHORIZATION

Maintain:

ACCESS =
ROLE
+
PERMISSION
+
JURISDICTION
+
RESOURCE OWNERSHIP

Backend is authoritative.

Never rely on frontend hiding buttons as the security boundary.

---

044. EXISTING UI / DESIGN

Do not replace the current ELMKUSOMA design language.

Preserve the established premium light EdTech identity:

- Primary Blue #2563EB
- Teal #0D9488
- Orange #F59E0B
- White #FFFFFF
- Soft #F8FAFC
- Text #1E293B

Avoid:

- dark generic admin templates
- excessive gradients
- excessive shadows
- excessive rounded cards
- fake AI effects
- random iconography
- unnecessary animations
- clutter

Improve the Live experience while remaining visually consistent with ELMKUSOMA.

---

045. REAL DATA ONLY

Never create:

- fake participants
- fake connection metrics
- fake recordings
- fake polls
- fake quiz results
- fake attendance
- fake analytics
- fake rooms
- fake platform health

If backend data does not exist, implement the real source or clearly report the blocker.

---

046. DATABASE / FLYWAY

Before changing the database:

1. inspect current schema;
2. inspect migrations;
3. inspect entities;
4. inspect relationships;
5. determine whether schema change is actually necessary.

Rules:

- Flyway additive migrations only;
- no destructive migration;
- no duplicate tables;
- no duplicate columns;
- no accidental migration version collision;
- preserve existing data;
- do not rename/drop production structures unnecessarily.

Do not use database changes to solve a frontend synchronization problem if no database change is required.

---

047. API COMPATIBILITY

Preserve working API contracts unless there is a demonstrated reason to change them.

If an API must change:

- identify consumers;
- preserve backward compatibility where practical;
- update all affected clients;
- test old functionality;
- document the change.

---

048. ROUTES

Do not unnecessarily change existing routes.

Before adding a route:

- search existing routes;
- determine whether an equivalent exists;
- extend existing route where appropriate.

Perform a route/link audit after implementation.

---

049. ERROR ISOLATION

The Live platform must degrade gracefully.

Examples:

OBS FAILS
→ browser/mobile Live should remain usable where possible

CAMERA FAILS
→ teacher can switch source where supported

RECORDING FAILS
→ Live continues

CHAT FAILS
→ video continues

POLL FAILS
→ Live continues

REDIS FAILS
→ system enters appropriate degraded mode

STORAGE FAILS
→ Live should not unnecessarily terminate

ONE LEARNER DISCONNECTS
→ other learners remain connected

Do not let one optional subsystem crash the entire Live session.

---

050. REDIS

Inspect current Redis dependency.

If Redis is required for:

- realtime events
- presence
- LiveKit Ingress
- LiveKit Egress
- pub/sub

ensure the implementation handles unavailable Redis appropriately.

Do not falsely claim production readiness if a required infrastructure component is disabled.

---

051. LIVEKIT INGRESS

If using self-hosted LiveKit Ingress:

verify actual configuration and dependencies.

Inspect:

- URL
- Redis
- API key
- API secret
- ingress service
- supported protocols
- stream key generation
- lifecycle
- participant publication

Do not create a fake ingest endpoint.

---

052. LIVEKIT EGRESS

If using self-hosted Egress:

verify:

- Redis
- Egress service
- API credentials
- storage
- output configuration
- recording lifecycle
- failure handling

Do not claim recording is production-ready if Egress is disabled.

---

053. STORAGE

Inspect existing storage implementation.

Determine:

- provider
- bucket
- paths
- access control
- signed URLs
- retention
- metadata
- processing status

Do not create a second storage abstraction.

---

054. OBS / INGRESS / EGRESS ARE INFRASTRUCTURE, NOT DUPLICATE PRODUCTS

The product remains:

«ELMKUSOMA Live.»

OBS, Ingress, Egress, Encoder and Studio are supporting components.

Do not expose unnecessary infrastructure complexity to learners.

---

055. REAL-TIME UX REQUIREMENT

This is mandatory:

«No learner should be required to manually refresh the page to receive a Live-session state change initiated by a teacher or authorized moderator.»

This applies to existing interactive features such as:

- Poll
- Quiz
- Breakout Room
- Q&A
- Chat
- Raise Hand
- shared resources
- session state
- participant state
- screen sharing state

where the feature already exists.

---

056. REAL-TIME ACCEPTANCE TESTS

TEST 01 — Poll

Teacher starts Poll.

Expected:

Teacher starts Poll
↓
Backend persists state
↓
Realtime event
↓
Student sees Poll

No refresh.

PASS only if verified.

---

TEST 02 — Poll Close

Teacher closes Poll.

Student sees closed state immediately.

No refresh.

---

TEST 03 — Quiz Start

Teacher starts Quiz.

Student receives Quiz immediately.

---

TEST 04 — Quiz Question Change

Teacher changes question.

Student sees new question immediately.

No refresh.

---

TEST 05 — Breakout Creation

Teacher creates breakout rooms.

Students see available/assigned room state immediately.

---

TEST 06 — Breakout Assignment

Teacher assigns Student A to Room B.

Student A receives assignment immediately.

---

TEST 07 — Breakout Move

Teacher moves Student A.

Student receives updated room assignment.

---

TEST 08 — Reconnect

Student loses network.

Student reconnects.

Current session state is synchronized.

---

TEST 09 — Late Join

Teacher started Poll before Student joined.

Student joins.

Student receives current authoritative Poll state.

---

TEST 10 — Rapid Events

Teacher performs multiple state changes rapidly.

Learner state remains consistent.

---

057. END-TO-END LIVE ACCEPTANCE TEST

Verify:

Teacher
↓
Create Live
↓
Attach Course
↓
Attach Lesson
↓
Select Source
↓
Prepare Control Room
↓
Go Live
↓
Learner Joins
↓
Video Works
↓
Audio Works
↓
Attendance Works
↓
Chat Works
↓
Q&A Works
↓
Poll Works in REAL TIME
↓
Quiz Works in REAL TIME
↓
Raise Hand Works
↓
Breakout Works in REAL TIME
↓
Screen Share Works
↓
Resources Work
↓
Recording Starts
↓
Teacher Ends Live
↓
Recording Processes
↓
Media Asset Created
↓
Resource Library
↓
Lesson
↓
Learner Replay
↓
Analytics

---

058. BROADCAST ACCEPTANCE TEST

Where environment supports it:

Professional Camera
↓
Capture Card
↓
OBS / Encoder
↓
LiveKit Ingress
↓
LiveKit
↓
ELMKUSOMA Live
↓
Learners

Verify actual video/audio arrival.

Do not accept a UI-only implementation.

---

059. RECORDING ACCEPTANCE TEST

LiveKit Room
↓
Egress
↓
Recording
↓
Storage
↓
Media Asset
↓
Resource Library
↓
Lesson
↓
Replay

Verify each stage.

---

060. NO BLIND IMPLEMENTATION

Do not implement based solely on tutorials.

Tutorials are references for understanding:

- OBS
- production workflows
- cameras
- capture cards
- encoders
- streaming protocols
- WebRTC
- latency
- networking
- recording
- Egress
- Ingress
- failure scenarios

The actual implementation must conform to:

1. Existing ELMKUSOMA architecture
2. Existing code
3. Existing database
4. Existing APIs
5. Existing authorization
6. Existing LiveKit integration
7. Actual supported infrastructure

---

061. DO NOT CHASE FEATURES THAT ARE NOT COMPATIBLE

If a tutorial shows a feature that conflicts with ELMKUSOMA architecture:

DO NOT copy it blindly.

Instead:

Tutorial Concept
↓
ELMKUSOMA Architecture
↓
Compatibility Analysis
↓
Adapt Concept
↓
Implement Safely

---

062. PRIORITY ORDER

Implement in this priority:

P0 — Protect Existing

1. Existing Live functionality
2. Existing teacher flow
3. Existing learner flow
4. Existing LiveKit
5. Existing attendance
6. Existing interaction
7. Existing authorization

P0 — REAL-TIME

8. Poll realtime
9. Quiz realtime
10. Breakout realtime
11. Q&A/chat realtime where necessary
12. reconnect/resync
13. event ordering/state consistency

P1 — Broadcast Sources

14. Mobile
15. Browser
16. External USB camera
17. Professional camera
18. Capture card
19. OBS
20. Encoder
21. Studio

P1 — Media

22. Ingress
23. Egress
24. Recording
25. Storage
26. Media Asset
27. Resource Library
28. Lesson
29. Replay

P1 — Resilience

30. network adaptation
31. reconnect
32. degraded states
33. failure isolation
34. monitoring

P2 — Production Hardening

35. analytics
36. observability
37. admin visibility
38. mobile refinement
39. performance optimization
40. security hardening

---

063. PERFORMANCE

Do not solve every realtime problem with repeated API fetching.

Avoid:

- unnecessary polling
- excessive database queries
- duplicate subscriptions
- memory leaks
- duplicate event listeners
- repeated LiveKit connections
- unnecessary rerenders
- large media through PostgreSQL
- blocking backend operations

Use event-driven updates where appropriate.

---

064. FRONTEND REAL-TIME IMPLEMENTATION

Inspect current React/Next.js architecture.

Ensure:

- subscriptions are established once;
- listeners are cleaned up;
- events update the correct state;
- duplicate events do not duplicate UI;
- stale state is replaced correctly;
- reconnect triggers resynchronization;
- components do not subscribe repeatedly on rerender;
- unmounted components do not continue processing events.

---

065. BACKEND REAL-TIME IMPLEMENTATION

Ensure:

- events originate from authoritative backend actions;
- authorization occurs before publishing sensitive state;
- event payloads contain only required information;
- event delivery is scoped to the correct Live session;
- tenant/institution boundaries are respected;
- room/session ownership is respected;
- state remains persistent and recoverable.

---

066. SECURITY OF EVENTS

Never broadcast sensitive data to every connected user.

Every event must respect:

ROLE
+
PERMISSION
+
JURISDICTION
+
RESOURCE OWNERSHIP
+
SESSION MEMBERSHIP

A learner should only receive events they are authorized to receive.

---

067. UI STATES

Every Live feature should have appropriate:

- loading
- success
- error
- reconnecting
- disconnected
- degraded
- empty
- unauthorized
- unavailable

states.

Do not leave users staring at a frozen interface.

---

068. MOBILE NETWORK TESTING

Where possible test:

Wi-Fi
↓
Mobile Data
↓
Reconnect

and:

Mobile Data
↓
Wi-Fi
↓
Reconnect

Verify:

- media recovery
- realtime event recovery
- state synchronization
- attendance continuity
- breakout state
- poll/quiz state

---

069. BROWSER TESTING

Test at least relevant supported browsers.

Verify:

- camera permissions
- microphone permissions
- screen share
- reconnection
- realtime events
- mobile/responsive behavior

Do not claim browser compatibility without testing.

---

070. REGRESSION TESTING

Before declaring complete:

Run tests against existing functionality.

At minimum:

- Teacher creates Live
- Teacher starts Live
- Learner joins
- Learner sees video
- Learner sees audio
- Chat
- Q&A
- Poll
- Quiz
- Raise Hand
- Screen Share
- Attendance
- End Live
- Recording
- Existing admin functionality

Then test new enhancements.

---

071. MANUAL UI VERIFICATION

Protocol/API tests are NOT sufficient.

Where possible perform actual browser testing.

Check:

- browser console
- network errors
- WebSocket/SSE connection
- LiveKit connection
- event delivery
- UI state
- actual media
- permissions
- reconnect
- mobile layout

If browser testing is blocked, report it explicitly.

Do not claim VERIFIED.

---

072. NO FALSE COMPLETION

Never report:

«100% complete»

just because code compiles.

Completion must be evidence-based.

Differentiate:

CODE IMPLEMENTED
BUILD PASSED
UNIT TEST PASSED
INTEGRATION TEST PASSED
MANUAL UI VERIFIED
END-TO-END VERIFIED
ENVIRONMENT BLOCKED

---

073. FINAL AUDIT

At the end provide a table:

Requirement| Existing Before| Modification| Final State| Verification| Evidence
Existing Live| | | | | 
Teacher Control Room| | | | | 
Learner Live| | | | | 
Poll realtime| | | | | 
Quiz realtime| | | | | 
Breakout realtime| | | | | 
Chat/Q&A| | | | | 
Raise Hand| | | | | 
Attendance| | | | | 
Mobile source| | | | | 
Browser source| | | | | 
External camera| | | | | 
Professional camera| | | | | 
OBS| | | | | 
Encoder| | | | | 
Studio| | | | | 
Ingress| | | | | 
Egress| | | | | 
Recording| | | | | 
Storage| | | | | 
Media Asset| | | | | 
Resource Library| | | | | 
Lesson| | | | | 
Replay| | | | | 
Reconnection| | | | | 
Network resilience| | | | | 
Monitoring| | | | | 
Security| | | | | 
Mobile UX| | | | | 
Regression| | | | | 

---

074. COMPLETION PERCENTAGE

Calculate completion based on actual verified work.

Do NOT inflate percentages.

Report separately:

Existing functionality preserved: XX%
New functionality implemented: XX%
Real-time functionality verified: XX%
Broadcast functionality verified: XX%
Media/recording pipeline verified: XX%
Production hardening verified: XX%
Overall evidence-based completion: XX%

Explain blockers separately.

---

075. ROOT-CAUSE REPORT

If something cannot be completed, identify the real root cause.

Examples:

Egress unavailable
ROOT CAUSE:
LiveKit Egress service is disabled in environment.

Redis realtime unavailable
ROOT CAUSE:
Redis service is not running/configured.

OBS integration blocked
ROOT CAUSE:
Ingress service is not configured.

Browser verification blocked
ROOT CAUSE:
Browser environment unavailable.

Do not hide infrastructure blockers behind generic messages.

---

076. FILE CHANGE REPORT

Report:

- files modified
- files created
- files deleted, if any
- migrations added
- APIs changed
- APIs added
- frontend routes changed
- backend routes changed
- configuration changed
- environment variables required
- tests added/modified

Explain WHY each important change was necessary.

---

077. DO NOT MODIFY UNRELATED AREAS

This task is specifically about:

«ELMKUSOMA Live Streaming + Real-Time Interaction + Media Ecosystem.»

Do not make unrelated changes to:

- unrelated dashboards
- unrelated learner modules
- unrelated payment systems
- unrelated authentication
- unrelated courses
- unrelated certificates
- unrelated admin systems

unless a verified dependency requires it.

---

078. GIT SAFETY

Before major changes:

- inspect git status;
- understand current branch;
- do not overwrite unrelated developer work;
- do not reset user changes;
- do not force destructive git operations;
- preserve existing work.

If there are uncommitted changes, inspect them before touching related files.

---

079. ENVIRONMENT SAFETY

Do not assume:

- Redis is running
- Ingress is running
- Egress is running
- LiveKit is running
- storage is configured
- backend is using latest code

Verify.

If backend is running an old process, report it and provide the exact safe restart requirement.

---

080. IMPLEMENTATION STRATEGY

Use this exact sequence:

1. INSPECT
2. MAP EXISTING LIVE
3. CLASSIFY EXISTING FEATURES
4. IDENTIFY REAL-TIME ARCHITECTURE
5. IDENTIFY CURRENT BLOCKERS
6. PROTECT EXISTING FUNCTIONALITY
7. FIX REAL-TIME STATE PROPAGATION
8. FIX RECONNECT/RESYNC
9. HARDEN BREAKOUT ROOMS
10. HARDEN POLL
11. HARDEN QUIZ
12. EXTEND SOURCE ARCHITECTURE
13. MOBILE
14. EXTERNAL CAMERA
15. PROFESSIONAL CAMERA
16. OBS
17. ENCODER
18. STUDIO
19. INGRESS
20. EGRESS
21. RECORDING
22. STORAGE
23. MEDIA ASSET
24. RESOURCE LIBRARY
25. LESSON
26. REPLAY
27. NETWORK RESILIENCE
28. MONITORING
29. SECURITY
30. REGRESSION TEST
31. MANUAL UI TEST
32. END-TO-END TEST
33. FINAL AUDIT
34. COMPLETION REPORT

Do not blindly execute all 34 steps if repository evidence shows an area already works.

Use the existing implementation wherever possible.

---

081. GOLDEN ARCHITECTURAL PRINCIPLE

The final system should conceptually look like:

                         ELMKUSOMA LIVE
                               │
                    LEARNING LIVE PLATFORM
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
     TEACHER                LEARNER               ADMIN
        │                      │                      │
 Live Control Room       Learning Experience    Governance
        │                      │
        └──────────────┬───────┘
                       │
                  LIVE SESSION
                       │
       ┌───────────────┼────────────────┐
       │               │                │
     MEDIA         INTERACTION       STATE
       │               │                │
 Camera             Chat             Session
 Mobile             Q&A              Poll
 Browser            Poll             Quiz
 Pro Camera         Quiz             Breakout
 OBS                Raise Hand        Attendance
 Encoder            Resources        Participants
 Studio             etc.             etc.
       │               │                │
       └───────────────┼────────────────┘
                       │
                    RECORD
                       │
                    EGRESS
                       │
                    STORAGE
                       │
                  MEDIA ASSET
                       │
              RESOURCE LIBRARY
                       │
                    LESSON
                       │
                    REPLAY
                       │
                   ANALYTICS

---

082. FINAL NON-NEGOTIABLE RULE

The final implementation must satisfy all of these:

EXISTING SYSTEM

«KEEP IT»

WORKING FEATURES

«DO NOT BREAK THEM»

EXISTING ARCHITECTURE

«EXTEND IT»

DUPLICATE SYSTEMS

«DO NOT CREATE THEM»

REAL-TIME

«NO MANUAL REFRESH»

STATE

«BACKEND REMAINS AUTHORITATIVE»

EVENTS

«REAL-TIME PROPAGATION»

RECONNECT

«RESYNC CURRENT STATE»

BROADCAST

«PHONE + BROWSER + EXTERNAL CAMERA + PROFESSIONAL CAMERA + OBS + ENCODER + STUDIO»

MEDIA

«INGRESS + EGRESS + RECORDING + STORAGE + RESOURCE LIBRARY + LESSON + REPLAY»

SECURITY

«ROLE + PERMISSION + JURISDICTION + RESOURCE OWNERSHIP»

DATA

«REAL DATA ONLY»

DATABASE

«ADDITIVE FLYWAY ONLY»

UI

«PRESERVE ELMKUSOMA DESIGN»

TESTING

«OLD + NEW + END-TO-END»

REPORTING

«EVIDENCE-BASED COMPLETION %»

---

083. EXECUTION COMMAND

Now execute this task directly against the existing ELMKUSOMA repository.

Do NOT start by writing a generic plan.

Start by inspecting the actual repository and existing Live implementation.

Then:

1. produce the evidence-based audit;
2. identify exact existing extension points;
3. identify the root cause of the current Poll/Quiz/Breakout refresh problem;
4. implement the safest real-time solution using the existing architecture where possible;
5. preserve existing functionality;
6. extend the Live source/media ecosystem;
7. test old and new functionality;
8. perform manual/browser verification where possible;
9. perform end-to-end verification;
10. provide the final evidence-based completion report.

FINAL RULE:

«DO NOT REBUILD WHAT ALREADY EXISTS.

DO NOT REPLACE WORKING ELMKUSOMA LIVE.

DO NOT CREATE DUPLICATE SYSTEMS.

MODIFY WHAT EXISTS, EXTEND WHAT IS PARTIAL, FIX WHAT IS BROKEN, ADD ONLY WHAT IS MISSING, AND VERIFY EVERYTHING.»

The objective is not merely to add more features.

The objective is to make the existing ELMKUSOMA Live ecosystem substantially more real-time, reliable, flexible, broadcast-capable, learning-focused, resilient and production-ready — without breaking what already works.
