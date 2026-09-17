
ELMKUSOMA LIVE CLASSES & LIVE STREAMING

Developer 02 — Post-Implementation Enhancement, Schedule Live Class & Production Completion Task

---

0. IMPORTANT — READ BEFORE STARTING

You have already worked on the existing ELMKUSOMA Live Classes / Live Streaming implementation.

This is a POST-IMPLEMENTATION ENHANCEMENT AND COMPLETION TASK.

DO NOT rebuild ELMKUSOMA Live from scratch.

Do NOT replace the existing architecture simply because another implementation may appear easier.

Do NOT create a second Live system.

Do NOT create duplicate entities, APIs, services, pages, attendance systems, recording systems, media systems, notification systems, or realtime systems.

Your first responsibility is to:

«INSPECT → UNDERSTAND → MAP → REUSE → FIX → COMPLETE → EXTEND → INTEGRATE → VERIFY»

The objective is to take the existing ELMKUSOMA Live implementation and evolve it into a production-oriented:

ELMKUSOMA LEARNING LIVE & MEDIA PLATFORM

The existing Live Classes implementation must remain the foundation.

---

1. PRIMARY OBJECTIVE

Perform a complete technical and functional inspection of the current Live Classes / Live Streaming implementation.

Determine:

- What is already implemented?
- What is partially implemented?
- What is incorrectly implemented?
- What is broken?
- What is duplicated?
- What is missing?
- What is only UI/mock functionality?
- What is actually connected to backend APIs?
- What is actually connected to PostgreSQL?
- What is actually connected to realtime infrastructure?
- What is actually connected to LiveKit?
- What is production-ready?
- What is not production-ready?
- What can be improved?
- What should be reused?
- What should be consolidated?
- What genuinely needs to be added?

Then implement the required improvements.

---

2. FINAL PRODUCT DIRECTION

The final ELMKUSOMA Live lifecycle should support:

PLAN
  ↓
SCHEDULE
  ↓
PREPARE
  ↓
GO LIVE
  ↓
TEACH / INTERACT
  ↓
END
  ↓
RECORD
  ↓
PROCESS
  ↓
STORE
  ↓
REPLAY
  ↓
LEARN
  ↓
ANALYZE

The larger product experience is:

Teach
  ↓
Schedule
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

ELMKUSOMA Live should be capable of supporting:

- Interactive online classes
- School classes
- University lectures
- Seminars
- Workshops
- Webinars
- Professional training
- Educational events
- Studio-produced educational content
- Live broadcasts
- Recorded learning content
- Large-scale educational streaming

However:

«Do not implement UI for capabilities that the underlying architecture cannot actually support.»

If something requires external infrastructure, implement the correct integration boundary and clearly report the remaining dependency.

---

3. CORE PRODUCT PRINCIPLE

The most important distinction is:

«ELMKUSOMA Live is not simply a video meeting feature.»

A Live Session must be an actual learning event inside ELMKUSOMA.

For example:

Institution
    ↓
Course
    ↓
Class
    ↓
Subject
    ↓
Lesson
    ↓
Live Session
    ↓
Attendance
    ↓
Recording
    ↓
Media Library
    ↓
Replay
    ↓
Learning Activity
    ↓
Assessment / Progress

The Live Session should preserve learning context throughout its lifecycle.

---

4. EXISTING SYSTEM INSPECTION — MANDATORY

Before modifying anything, inspect the repository.

Do not make assumptions based on filenames or UI appearance.

---

4.1 BACKEND INSPECTION

Inspect all relevant:

- Live session entities
- Live class entities
- Repositories
- Services
- Controllers
- DTOs
- Mappers
- Enums
- Validation
- Authentication
- Authorization
- Institution relationships
- Teacher relationships
- Class relationships
- Course relationships
- Subject relationships
- Lesson relationships
- Assignment relationships
- Learning activities
- Enrollment
- Attendance
- Notifications
- Media
- Recordings
- Storage
- WebSocket
- Redis
- LiveKit integration
- Streaming infrastructure
- Error handling
- Logging
- Security
- Tests
- Configuration
- Flyway migrations

Also inspect whether the existing implementation already contains scheduling-related logic.

---

5. FRONTEND INSPECTION

Inspect:

- Teacher Live pages
- Teacher Live dashboard
- Teacher Studio
- Schedule Live Class UI
- Live session details
- Preparation/lobby UI
- Live classroom
- Learner Live pages
- Upcoming Live Classes
- Participant components
- Camera controls
- Microphone controls
- Screen sharing
- Chat
- Raise hand
- Participant management
- Attendance UI
- Recording UI
- Replay UI
- Media Library
- Notifications
- Error states
- Loading states
- Empty states
- Reconnection handling
- Responsive behavior
- API integration
- Authentication handling
- Authorization handling

Do not assume a frontend button is functional.

Trace:

UI
 ↓
API
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database

Where realtime is involved:

UI
 ↓
LiveKit / Realtime
 ↓
Backend authorization
 ↓
Session

---

6. INFRASTRUCTURE INSPECTION

Inspect:

- LiveKit
- WebRTC
- Room management
- Token generation
- Token expiration
- Ingress
- Egress
- Recording
- Redis
- PostgreSQL
- Object storage
- Media processing
- Environment variables
- Secrets
- CORS
- Network configuration
- Production configuration

Determine exactly what is available and what is not.

---

7. DO NOT CREATE DUPLICATE SYSTEMS

This is a critical architectural rule.

Do NOT create another:

- LiveSession system
- LiveClass system
- Schedule system
- Attendance system
- Course system
- Class system
- Lesson system
- Enrollment system
- Recording system
- Media system
- Authentication system
- Authorization system
- Notification system
- Realtime system
- Support system

If an existing system already performs the function:

«Reuse and improve it.»

If incomplete:

«Complete it.»

If broken:

«Fix it.»

If duplicated:

«Consolidate it.»

If a new model is genuinely required:

«First prove why the existing model cannot support the requirement.»

---

8. MAJOR NEW REQUIREMENT — SCHEDULE LIVE CLASS

The existing Live implementation must be enhanced with a complete:

SCHEDULE LIVE CLASS

workflow.

This is not merely a form that stores:

title
date
time

It must create a properly authorized ELMKUSOMA learning event.

The intended lifecycle is:

Teacher Dashboard
      ↓
Schedule Live Class
      ↓
Learning Context
      ↓
Date & Time
      ↓
Participants & Access
      ↓
Live Experience
      ↓
Learning Content
      ↓
Recording & Notifications
      ↓
Review & Readiness
      ↓
Schedule
      ↓
SCHEDULED
      ↓
Preparation
      ↓
Preflight
      ↓
STARTING
      ↓
LIVE

---

9. SCHEDULE LIVE CLASS — PRODUCT PRINCIPLE

Scheduling and starting are different operations.

Schedule

Creates and persists the learning event.

Schedule Live Class
        ↓
LiveSession = SCHEDULED

Prepare

Allows the teacher to prepare the session.

SCHEDULED
   ↓
Preparation
   ↓
Camera
Microphone
Speaker
Network
Materials
Session settings

Start

Actually starts the realtime classroom.

Start Live
    ↓
Authorization
    ↓
LiveKit room/session activation
    ↓
Short-lived token
    ↓
Teacher joins
    ↓
LIVE

Never treat:

«Schedule = Start»

---

10. SCHEDULE LIVE CLASS — STEP 1: LEARNING CONTEXT

The first section should establish where the Live Class belongs.

Possible fields:

- Course / Program
- Class
- Subject
- Lesson / Topic
- Live Class Title
- Description

Expected conceptual relationship:

Course
   ↓
Class
   ↓
Subject
   ↓
Lesson
   ↓
Live Session

However:

«Use the actual relationships already implemented in ELMKUSOMA.»

Do not invent relationships merely to make the UI appear complete.

---

11. CONTEXT-AWARE FORM

The scheduling interface should be intelligent.

If the teacher starts scheduling from:

Course → Mathematics → Form 2A

the form should automatically use the available context where the backend supports it.

The teacher should not repeatedly enter information that ELMKUSOMA already knows.

For example:

Course: Mathematics
Class: Form 2A
Subject: Mathematics

should be resolved from authoritative backend data where possible.

The frontend must not invent relationships.

Backend remains authoritative.

---

12. SCHEDULE LIVE CLASS — STEP 2: DATE & TIME

Allow the teacher to specify:

- Date
- Start time
- End time
- Duration
- Timezone

Use the project's established timezone strategy.

For Tanzania, the expected local timezone is:

Africa/Dar_es_Salaam
UTC+03:00

But follow the existing application timezone conventions if already defined.

---

12.1 TIME VALIDATION

Prevent invalid schedules.

Validate:

- Date cannot be unintentionally in the past
- Start time must be before end time
- End time must be after start time
- Duration must be valid
- Session cannot exceed supported limits
- Invalid timestamps must be rejected
- Backend must revalidate all frontend validations

Never trust frontend validation alone.

---

13. SCHEDULE CONFLICT DETECTION

Add a professional scheduling safeguard.

Before creating the session, detect conflicts where supported.

Possible conflicts:

Teacher
   ↓
Existing Scheduled Live Class
   ↓
Overlapping Time

Also consider relevant:

- Class schedule conflicts
- Teacher's existing teaching commitments
- Existing Live Sessions
- Institution scheduling rules

Example:

⚠ Schedule conflict

You already have another live class scheduled
during this time.

Existing:
Mathematics — Form 2A
10:00 AM – 11:00 AM

Do not use fake conflict detection.

The backend must perform the authoritative conflict check.

---

14. RACE-SAFE SCHEDULING

Two requests may arrive almost simultaneously.

Example:

Request A → Schedule 10:00
Request B → Schedule 10:00

The system must prevent accidental duplicate scheduling where business rules prohibit it.

Use appropriate:

- Database constraints
- Transaction boundaries
- Unique constraints where justified
- Idempotency
- Backend validation

Do not rely only on disabling the frontend button.

---

15. SCHEDULE LIVE CLASS — STEP 3: PARTICIPANTS & ACCESS

Participants should normally come from existing ELMKUSOMA authorization systems.

Possible source:

Class Enrollment
      ↓
Eligible Learners
      ↓
Live Session Access

Display useful information such as:

Eligible learners: 42

But the number must come from real backend data.

Never hard-code participant counts.

---

16. PARTICIPANT SELECTION

Where existing authorization supports it, allow:

- Entire authorized class
- Selected authorized learners
- Other explicitly authorized participants

Do not allow arbitrary users to be added simply because their email exists.

Backend must verify:

- Institution
- Teacher authorization
- Class
- Course
- Enrollment
- Session access policy

---

17. LIVE JOIN POLICY

Define and enforce access rules.

Possible policy:

Authorized enrolled learners
        ↓
May view scheduled session
        ↓
May join within allowed time
        ↓
Backend authorization
        ↓
Short-lived LiveKit token
        ↓
Join

Support where appropriate:

- Early join
- Waiting/lobby period
- On-time join
- Late join
- Rejoin after temporary disconnection

Do not expose a room ID as an access credential.

---

18. SCHEDULE LIVE CLASS — STEP 4: LIVE EXPERIENCE

Allow the teacher to configure supported classroom capabilities.

Possible options:

- Camera
- Microphone
- Screen sharing
- Chat
- Raise hand
- Participant audio
- Participant video
- Teacher participant controls
- Recording

Use progressive disclosure.

Do not make the scheduling form unnecessarily complicated.

Advanced options should be collapsed where appropriate.

---

19. DEFAULT LIVE CLASSROOM SETTINGS

Where supported, allow configuration such as:

Teacher camera: ON/OFF
Teacher microphone: ON/OFF

Learner microphone:
Allowed / Restricted

Learner camera:
Allowed / Restricted

Chat:
Enabled / Disabled

Raise hand:
Enabled / Disabled

Screen sharing:
Teacher only / Allowed participants

Every setting must correspond to real backend/realtime behavior.

Do not create settings that do nothing.

---

20. SCHEDULE LIVE CLASS — STEP 5: LEARNING CONTENT

The Live Class must remain connected to learning.

Where supported, allow the teacher to attach existing:

- Lesson
- Notes
- PDF
- Presentation
- Learning material
- Assignment
- Assessment
- Learning activity

Example:

Course
 ↓
Lesson
 ↓
Live Class
 ↓
Assignment

Do not create another assignment system.

Reuse the existing ELMKUSOMA learning infrastructure.

---

21. POST-LIVE LEARNING ACTIONS

Where supported, allow the teacher to define what happens after the session.

Examples:

Live ends
   ↓
Recording becomes available
   ↓
Learner replay
   ↓
Assignment
   ↓
Assessment
   ↓
Learning progress

Potential actions:

- Make recording available
- Link assignment
- Link assessment
- Mark lesson activity
- Add replay to learning content
- Notify learners

Only implement what the existing architecture can genuinely support.

---

22. SCHEDULE LIVE CLASS — STEP 6: RECORDING

Allow the teacher to define recording behavior where the infrastructure supports it.

Possible settings:

- Record session
- Replay availability
- Download permission
- Recording visibility
- Authorized audience

Example:

Record session
        ↓
Process recording
        ↓
Store media
        ↓
Media Library
        ↓
Replay authorization

Do not claim that recording is available if LiveKit/egress/storage is not configured.

---

23. SCHEDULE LIVE CLASS — STEP 7: NOTIFICATIONS

Use the existing ELMKUSOMA notification system.

Possible notifications:

Immediately after scheduling

Live class scheduled

Before class

Live class starting soon

At start

Live class has started

If delayed

Live class delayed

If service unavailable

Live service temporarily unavailable

After class

Live class ended

Recording

Recording available

Do not create a new notification infrastructure.

---

24. SCHEDULE LIVE CLASS — STEP 8: REVIEW & READINESS

Before final scheduling, provide a review stage.

Example:

REVIEW LIVE CLASS

Title:
Algebra — Introduction

Course:
Mathematics

Class:
Form 2A

Subject:
Mathematics

Lesson:
Linear Equations

Date:
18 September 2026

Time:
10:00 – 11:00

Participants:
42 authorized learners

Recording:
Enabled

Notifications:
Enabled

Access:
Enrolled learners

Then show:

Schedule Live Class

---

25. PRE-SCHEDULE VALIDATION

Before creating the session, verify:

- Teacher authorization
- Institution authorization
- Course authorization
- Class authorization
- Subject authorization
- Lesson relationship
- Participant eligibility
- Date
- Start time
- End time
- Timezone
- Conflict detection
- Recording configuration
- Notification configuration
- Access policy

If validation fails:

«Do not create the session.»

Return a useful error.

---

26. DOUBLE-SUBMISSION PROTECTION

If the teacher taps:

Schedule Live Class

multiple times because of:

- slow network
- UI lag
- accidental double tap
- browser retry

the system must not accidentally create duplicate sessions.

Implement appropriate:

- Idempotency
- Request protection
- Transaction handling
- UI submission state

Do not rely only on:

button.disabled = true

---

27. DRAFT / RESUME SUPPORT

If the existing architecture supports drafts, allow:

Draft
  ↓
Save
  ↓
Resume later
  ↓
Schedule

If draft persistence does not exist:

- Do not create a large unnecessary draft architecture merely for UI convenience.
- Use the simplest architecture consistent with the existing system.
- Do not create fake draft records.

---

28. AFTER SUCCESSFUL SCHEDULING

After scheduling:

LiveSession
status = SCHEDULED

Show a session details page.

Provide actions such as:

- View Live Class
- Edit
- Reschedule
- Cancel
- View Learners
- Add Materials
- View Schedule
- Enter Preparation
- Send Announcement where supported

---

29. EDIT LIVE CLASS

A scheduled Live Class should be editable where business rules permit.

Possible editable fields:

- Title
- Description
- Materials
- Participants
- Date
- Time
- Recording policy
- Notifications
- Access settings

Do not allow unsafe changes once the session is already LIVE.

Backend must enforce state-dependent edit rules.

---

30. RESCHEDULE LIVE CLASS

Support rescheduling where appropriate.

Flow:

SCHEDULED
   ↓
Reschedule
   ↓
Validate new time
   ↓
Check conflicts
   ↓
Update session
   ↓
Notify affected users
   ↓
Remain SCHEDULED

Do not simply modify timestamps without considering notifications and conflicts.

---

31. CANCEL LIVE CLASS

Where supported:

SCHEDULED
   ↓
Cancel
   ↓
Confirm
   ↓
Update status
   ↓
Notify participants

Do not delete the historical session record merely to hide it.

Preserve audit/history where the architecture supports it.

---

32. PREPARATION / PRE-CLASS LOBBY

Introduce a distinction between:

Scheduled

and:

Ready to Start

Teacher should be able to enter a preparation experience before starting the live session.

Preparation may include:

- Camera test
- Microphone test
- Speaker test
- Browser permission check
- Network check
- LiveKit connectivity check
- Selected camera
- Selected microphone
- Selected speaker
- Materials check
- Recording configuration
- Participant preview
- Session information

The teacher should be able to prepare without accidentally making the session LIVE.

---

33. REAL PRE-FLIGHT CHECK

The preparation screen should show actual measurable status.

Example:

Camera             ✓ Ready
Microphone         ✓ Ready
Speaker            ✓ Ready
Network             ✓ Good
Live service        ✓ Available
Recording           ✓ Configured
Materials           ✓ Ready

But:

«Never fake these values.»

If a value cannot be measured:

Not checked

or an appropriate unknown state must be shown.

---

34. START LIVE

The Start button should initiate the actual realtime session.

Expected flow:

Teacher
   ↓
Start Live
   ↓
Backend authorization
   ↓
Validate session state
   ↓
Activate Live session
   ↓
LiveKit room
   ↓
Short-lived token
   ↓
Teacher joins
   ↓
STARTING
   ↓
LIVE

Do not mark the session LIVE merely because the teacher opened the page.

---

35. LIVE CLASSROOM

The ELMKUSOMA Live Classroom must provide a real interactive learning experience.

---

TEACHER

Verify and implement where missing:

- Start session
- End session
- Camera
- Microphone
- Screen sharing
- Participant list
- Participant management
- Learner audio/video permissions
- Chat
- Raise hand
- Attendance
- Participation tracking where measurable
- Network quality
- Materials
- Class context
- Course context
- Subject context
- Lesson context
- Assignment/learning activity context
- Notifications
- Recording
- Replay
- Session status

---

LEARNER

Verify and implement where missing:

- Upcoming Live Classes
- Authorized joining
- Camera controls
- Microphone controls
- Permission handling
- Chat
- Raise hand
- Screen viewing
- Materials
- Leave
- Rejoin
- Reconnection
- Connection quality
- Authorized recording access

---

36. LIVEKIT + WEBRTC ARCHITECTURE

Use:

WebRTC
+
LiveKit

for realtime media.

Do not build custom video infrastructure unless there is a documented technical requirement.

---

37. SPRING BOOT RESPONSIBILITY

Spring Boot remains the:

BUSINESS + CONTROL LAYER

Spring Boot handles:

- Authentication
- Authorization
- Session ownership
- Institution boundaries
- Enrollment verification
- Class verification
- Course authorization
- Live session lifecycle
- Attendance
- Permissions
- Notifications
- Recording metadata
- Media metadata
- Analytics
- Session status
- Incident/status management
- Audit events

Spring Boot must NOT become the video/media server.

---

38. LIVEKIT RESPONSIBILITY

LiveKit should handle the realtime media layer.

Verify:

- Room creation
- Room lifecycle
- Participants
- Audio
- Video
- Screen sharing
- Reconnection
- Participant events
- Token handling
- Token expiration
- Recording/egress where configured
- Ingress where configured

---

39. SECURITY FLOW

The expected flow is:

LOGIN
  ↓
ROLE VERIFICATION
  ↓
INSTITUTION VERIFICATION
  ↓
CLASS / COURSE / ENROLLMENT VERIFICATION
  ↓
LIVE SESSION AUTHORIZATION
  ↓
SHORT-LIVED LIVEKIT TOKEN
  ↓
JOIN

A user must not be able to join simply because they know:

- Room ID
- Session ID
- URL
- Another user's token

Verify:

- Teacher ownership
- Learner authorization
- Institution boundaries
- Enrollment
- Session state
- Access policy
- Token expiration
- Session expiration
- Recording permissions
- Download permissions
- Stream-key security
- Audit logging

Never expose:

LIVEKIT_API_SECRET

or other server-side secrets to the frontend.

---

40. PROFESSIONAL CAMERA & STUDIO SUPPORT

The architecture must remain source-agnostic.

It should not be tied to laptop webcams.

Prepare/support integration boundaries for:

- Smartphone
- Laptop
- USB webcam
- DSLR
- Mirrorless
- Professional camera
- HDMI
- SDI
- External microphone
- Audio interface
- Audio mixer
- OBS
- Capture card
- Hardware encoder

Conceptual workflow:

Camera
   ↓
Capture Card
   ↓
OBS / Studio Encoder
   ↓
RTMP / WHIP / SRT
   ↓
ELMKUSOMA Streaming Layer
   ↓
ELMKUSOMA Live

Do not tie the architecture to one camera manufacturer.

---

41. ELMKUSOMA STUDIO / BROADCAST MODE

Where infrastructure supports it, prepare for professional educational production.

Possible sources:

Camera 1
Camera 2
Camera 3
Presentation
Screen
Teacher
Guest
External Audio

Possible scenes:

Camera
Presentation
Camera + Slides
Interview
Q&A
Closing
Educational Broadcast

Professional users should be able to work with:

- OBS
- Capture cards
- Mixers
- Switchers
- Professional cameras
- Hardware encoders

Ordinary teachers should still be able to go live from:

- Phone
- Laptop
- Browser
- Webcam

Do not force ordinary teachers into studio complexity.

---

42. RECORDING & MEDIA PIPELINE

Recording must not simply be:

Record Meeting
      ↓
Download MP4

Target:

Live Session
      ↓
Recording
      ↓
Processing
      ↓
Storage
      ↓
Media Metadata
      ↓
ELMKUSOMA Media Library
      ↓
Replay

PostgreSQL stores metadata.

Actual media must use appropriate object/media storage.

Do NOT store video binaries directly in PostgreSQL.

---

43. EDUCATIONAL RECORDING CONTEXT

A recording should preserve:

- Live Session
- Course
- Class
- Subject
- Lesson
- Teacher
- Institution
- Learning activity

Where supported, prepare for:

- Master recording
- Composite recording
- Teacher video
- Screen share
- Audio
- Multiple tracks
- Thumbnails
- Captions
- Playback metadata
- Access control
- Download permissions

Reuse existing media models if compatible.

Do not blindly create the conceptual model below:

MediaAsset
├── title
├── description
├── thumbnail
├── duration
├── playbackInformation
├── mediaType
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
├── status
└── createdAt

Use the repository's actual architecture.

---

44. MEDIA LIBRARY

Inspect the existing Media Library.

If it exists:

«Improve and integrate it.»

If missing:

«Implement only the necessary architecture.»

The Media Library should support educational media such as:

- Live recordings
- Recorded lessons
- Course videos
- Lecture recordings
- Webinars
- Workshops
- Educational events
- Studio content
- Tutorials
- Replays

Where supported:

- Thumbnails
- Captions
- Search
- Filtering
- Access control
- Learning context
- Tags
- Playback metadata

The Media Library is the bridge between:

LIVE TEACHING
      ↓
RECORDED KNOWLEDGE
      ↓
REUSABLE LEARNING CONTENT

---

45. VIDEO QUALITY & ADAPTABILITY

Verify actual support for:

- Device capability
- Network bandwidth
- Connection quality
- Adaptive media
- Quality degradation
- Recovery
- Reconnection

A strong connection should receive appropriate quality.

A weak connection should degrade gracefully rather than unnecessarily losing the classroom.

Do not create fake:

98% connection
720p
Excellent network

unless these values are actually measurable.

---

46. LIVE ATTENDANCE

Attendance must be based on actual realtime/session events.

Expected:

Learner joins
      ↓
Participant detected
      ↓
Join event recorded
      ↓
Attendance updated
      ↓
Learner leaves
      ↓
Leave event recorded
      ↓
Duration calculated

Support where possible:

- Join
- Leave
- Rejoin
- Session duration
- Participation duration
- Teacher presence
- Learner presence

Opening the Live Class page alone must NOT mark attendance.

---

47. LIVE ANALYTICS

Where measurable, support:

- Participant count
- Join events
- Leave events
- Attendance
- Participation duration
- Session duration
- Connection quality
- Engagement indicators
- Recording status

Never fabricate analytics.

---

48. LIVE SESSION STATES

Inspect the existing session lifecycle.

Recommended conceptual lifecycle:

DRAFT
  ↓
SCHEDULED
  ↓
STARTING
  ↓
LIVE
  ↓
ENDING
  ↓
ENDED

Where technically supported:

LIVE
 ↓
SERVICE_DEGRADED
 ↓
SERVICE_UNAVAILABLE
 ↓
RECOVERING
 ↓
LIVE

Do not create a second state system.

Use the existing enum/model where possible.

Every state transition must be controlled by backend business rules.

---

49. STATE RULES

Examples:

SCHEDULED
→ can edit
→ can reschedule
→ can cancel
→ can prepare

STARTING
→ preparation mostly locked
→ joining begins

LIVE
→ realtime classroom active
→ unsafe scheduling edits blocked

ENDING
→ session closing
→ recording finalization may begin

ENDED
→ replay/recording processing
→ historical record retained

Do not allow impossible transitions.

---

50. NO EXTERNAL MEETING ALTERNATIVE

Do not add selectable external teaching platforms such as:

- Google Meet
- Zoom
- Microsoft Teams

ELMKUSOMA Live is the official ELMKUSOMA classroom.

The purpose is to keep:

Attendance
Learning
Recordings
Analytics
Courses
Lessons
Notifications
Security

inside ELMKUSOMA.

---

51. INCIDENT HANDLING

If the Live infrastructure fails, show accurate status.

Example:

ELMKUSOMA Live is temporarily unavailable.

We are experiencing a temporary technical issue
with the live service.

Your scheduled class remains active while the
service is being restored.

Learner:

Live class temporarily unavailable.

The ELMKUSOMA Live service is currently
experiencing a technical issue.

Please remain available. You will be notified
when the classroom is available.

Never fake:

- Live status
- Participant count
- Connection quality
- Recording status
- Service health

---

52. LIVE ISSUE REPORTING

Reuse an existing support/incident architecture if available.

Possible issue categories:

- Cannot join
- Camera problem
- Microphone problem
- Audio problem
- Poor video quality
- Connection problem
- Live service unavailable
- Recording problem
- Other

Do not create a second unrelated support system.

---

53. ADMIN / OPERATIONS MONITORING

Where the architecture supports it, administrators/operators should be able to see real operational information such as:

- Live service health
- Active sessions
- Active rooms
- Participant count
- Connection quality
- Recording status
- Failed sessions
- Service errors
- Realtime infrastructure status

Only expose information that can actually be measured.

---

54. LEARNING INTEGRATION

A Live Session must integrate with:

- Institution
- Teacher
- Class
- Course
- Subject
- Lesson
- Assignment
- Learning Activity
- Enrollment
- Attendance
- Notification
- Media Library
- Progress
- Assessment where supported

Example:

Course
   ↓
Lesson
   ↓
Live Session
   ↓
Attendance
   ↓
Recording
   ↓
Media Library
   ↓
Replay
   ↓
Learning Activity
   ↓
Progress

---

55. TEACHER DASHBOARD INTEGRATION

The scheduling feature must connect naturally to the existing Teacher Dashboard.

Expected flow:

Teacher Dashboard
      ↓
Schedule Live Class
      ↓
Schedule Form
      ↓
Create Live Session
      ↓
SCHEDULED
      ↓
Live Class Details

The scheduled session should appear in relevant existing areas such as:

- Today's Live Classes
- Upcoming Live Classes
- Schedule
- Live Classes
- Dashboard Command Center

Do not create another dashboard.

---

56. LEARNER EXPERIENCE

Authorized learners should see scheduled sessions in the appropriate existing learner experience.

Example:

Upcoming Live Class

Mathematics
Linear Equations

Today
10:00 AM – 11:00 AM

Teacher:
[Existing teacher data]

Status:
Upcoming

Join access must depend on backend authorization.

The learner should not receive a usable LiveKit token simply by loading the page.

---

57. NOTIFICATION INTEGRATION

Use the existing notification infrastructure.

Do not create:

LiveNotificationService

if the platform already has a general notification system capable of handling this.

Integrate Live events into the established architecture.

---

58. RECONNECTION

Verify:

- Automatic reconnection
- Participant state recovery
- Media recovery
- UI feedback
- Retry behavior
- Session continuity

Temporary network interruption should not unnecessarily terminate the user's entire learning session.

---

59. RESPONSIVE DESIGN

Verify:

- Desktop
- Laptop
- Tablet
- Mobile

Teacher controls must remain usable.

Learner controls must remain clear.

The scheduling workflow must also be usable on mobile.

Do not create a desktop-only scheduling form.

---

60. SCHEDULING UX PRINCIPLES

The Schedule Live Class interface should be:

- Context-aware
- Progressive
- Clear
- Fast
- Accessible
- Responsive
- Professional
- Low-friction

Do not present every advanced setting at once.

Recommended structure:

1. Learning Context
2. Date & Time
3. Participants & Access
4. Live Experience
5. Learning Content
6. Recording & Notifications
7. Review & Readiness

This does not necessarily mean seven separate pages.

Use the existing ELMKUSOMA UI patterns.

Do not introduce a new UI framework without a strong reason.

---

61. ACCESSIBILITY

Verify:

- Keyboard navigation
- Labels
- Focus states
- Screen-reader compatibility
- Error messaging
- Color-independent status indicators
- Touch target sizes
- Mobile usability
- Accessible forms

The scheduling flow must not depend only on color or icons.

---

62. DATABASE

Inspect the existing database before modifying it.

Do not create duplicate tables for:

- Live sessions
- Attendance
- Recordings
- Media
- Courses
- Classes
- Lessons
- Enrollments

If schema changes are genuinely required:

- Use Flyway
- Follow existing migration conventions
- Create deterministic migrations
- Preserve existing data
- Test clean migration
- Test migration against existing database
- Add appropriate indexes/constraints where justified

Never manually modify production schema.

---

63. SCHEDULING DATABASE SAFETY

Where appropriate, enforce database-level protection for:

- Invalid state transitions
- Duplicate session creation
- Conflict conditions
- Referential integrity
- Institution boundaries
- Required relationships

Do not push all integrity responsibility into the frontend.

---

64. API DESIGN

Inspect existing Live APIs first.

Reuse existing endpoints where appropriate.

Do not automatically create:

/api/v2/live-sessions

or another parallel API simply because a new feature is being added.

If an endpoint must be added:

- Follow existing API conventions
- Reuse existing DTOs where possible
- Reuse existing services
- Reuse existing authorization
- Keep naming consistent
- Avoid duplicate business logic

---

65. CONFIGURATION & SECRETS

Use existing environment configuration conventions.

Possible configuration:

LIVEKIT_URL
LIVEKIT_API_KEY
LIVEKIT_API_SECRET

Never commit:

- API secrets
- LiveKit secret
- Stream keys
- Production credentials

Frontend must never receive server-side secrets.

---

66. PERFORMANCE

Optimize:

- API calls
- Scheduling queries
- Conflict checks
- Database queries
- Realtime subscriptions
- Participant updates
- Frontend rendering
- Media state updates
- Recording polling
- Notifications

Avoid:

- Duplicate requests
- Duplicate websocket connections
- Duplicate LiveKit connections
- Memory leaks
- Excessive polling
- Unnecessary re-renders

---

67. ERROR HANDLING

Every important scheduling operation must handle:

- Validation failure
- Unauthorized teacher
- Invalid course
- Invalid class
- Invalid lesson
- Invalid enrollment
- Schedule conflict
- Duplicate request
- Database failure
- Notification failure
- LiveKit failure
- Recording configuration failure
- Network failure

Errors should be:

- Accurate
- Actionable
- User-friendly
- Logged appropriately

Do not expose sensitive backend information.

---

68. AUDIT TRAIL

Where an existing audit architecture exists, use it.

Important actions may include:

Live session created
Live session edited
Live session rescheduled
Live session cancelled
Live session started
Live session ended
Recording created
Recording published
Access changed

Do not create a duplicate audit system.

---

69. NO FAKE DATA

This requirement applies everywhere.

Do not hard-code:

- Participant counts
- Attendance
- Connection quality
- Recording status
- Live status
- Analytics
- Session statistics
- Learner counts
- Teacher metrics

If data does not exist:

Use a meaningful empty state.

Remember:

«NO DATA ≠ FEATURE DOES NOT EXIST»

---

70. EMPTY / LOADING / ERROR STATES

Every relevant Live interface must handle:

Loading

Loading live session...

No scheduled sessions

No live classes scheduled yet.

No participants

No participants have joined yet.

Recording processing

Recording is being processed.

Recording unavailable

Recording is not available yet.

Service unavailable

ELMKUSOMA Live is temporarily unavailable.

Do not hide functionality simply because data is currently empty.

---

71. TESTING REQUIREMENT

Do not finish by saying:

«"Implemented successfully."»

Actually test the implementation.

---

BACKEND

Verify:

- Build succeeds
- Application starts
- Database connection works
- Flyway works
- Live APIs work
- Schedule API works
- Authorization works
- Conflict detection works
- Duplicate submission protection works
- Token generation works
- Existing APIs remain functional

---

FRONTEND

Verify:

- Application builds
- Schedule page renders
- Teacher can access authorized scheduling
- Unauthorized users are blocked
- Context loading works
- Date/time validation works
- Conflict errors work
- Schedule submission works
- Session details work
- Preparation works
- Live classroom works
- Learner access works
- Errors work
- Loading states work

---

72. REAL LIVE FLOW VERIFICATION

Verify as much of the actual flow as the environment permits.

Teacher Login
      ↓
Teacher Dashboard
      ↓
Schedule Live Class
      ↓
Select Course/Class/Subject/Lesson
      ↓
Select Date/Time
      ↓
Validate Participants
      ↓
Check Conflicts
      ↓
Configure Live Experience
      ↓
Configure Recording
      ↓
Configure Notifications
      ↓
Review
      ↓
Schedule
      ↓
SCHEDULED
      ↓
Preparation
      ↓
Preflight
      ↓
Start Live
      ↓
LiveKit Authorization
      ↓
Short-lived Token
      ↓
Teacher Joins
      ↓
Learner Joins
      ↓
Realtime Media
      ↓
Participant Events
      ↓
Attendance
      ↓
Interaction
      ↓
End Session
      ↓
Recording
      ↓
Processing
      ↓
Media Library
      ↓
Replay

If an external infrastructure dependency prevents testing:

«Clearly report the limitation.»

Never claim that it was tested when it was not.

---

73. TEST SCHEDULING EDGE CASES

Test at minimum:

- Valid schedule
- Past date
- Invalid start/end
- Zero/negative duration
- Teacher conflict
- Class conflict where applicable
- Unauthorized teacher
- Unauthorized learner
- Invalid course
- Invalid class
- Invalid lesson
- Duplicate submission
- Network interruption
- Reschedule
- Cancel
- Notification failure
- Recording configuration unavailable
- Existing database migration
- Clean database migration

---

74. SECURITY TESTING

Verify:

User A
  ↓
Cannot access User B's Live Session

Learner not enrolled
  ↓
Cannot obtain join authorization

Teacher from Institution A
  ↓
Cannot manage Institution B's session

Expired token
  ↓
Cannot continue authorization

Random room ID
  ↓
Cannot grant access

---

75. PERFORMANCE / REALTIME TESTING

Verify that:

- One participant does not create excessive API traffic
- Participant events are handled efficiently
- Realtime connections are cleaned up
- Rejoining does not create duplicate connections
- Leaving a session cleans resources
- Scheduling does not create duplicate requests
- Recording status does not cause uncontrolled polling

---

76. DO NOT BREAK EXISTING ELMKUSOMA

Before completing the task, verify that existing modules still work.

Especially:

- Authentication
- Users
- Students
- Teachers
- Parents
- Institutions
- Courses
- Classes
- Subjects
- Lessons
- Enrollments
- Attendance
- Assignments
- Assessments
- Grading
- Notifications
- Media
- Certificates
- Dashboards

Do not introduce unrelated regressions.

---

77. IMPLEMENTATION STRATEGY

Work in this exact order.

STEP 1 — INSPECT

Inspect the existing repository and implementation.

STEP 2 — MAP

Map:

Existing Live Architecture
Existing APIs
Existing Database
Existing Frontend
Existing Realtime
Existing Media
Existing Notifications
Existing Authorization

STEP 3 — GAP ANALYSIS

Classify:

IMPLEMENTED
PARTIALLY IMPLEMENTED
BROKEN
MISSING
DUPLICATED
NEEDS IMPROVEMENT

STEP 4 — REUSE

Reuse existing:

- Entities
- Services
- APIs
- Repositories
- DTOs
- Components
- Hooks
- Utilities
- Notifications
- Authorization
- Media
- Attendance

STEP 5 — FIX

Fix broken implementation.

STEP 6 — COMPLETE

Complete partial implementation.

STEP 7 — SCHEDULE

Implement the complete:

Schedule
→ Validate
→ Conflict Check
→ Create
→ Notify
→ Prepare

workflow.

STEP 8 — INTEGRATE

Connect scheduling with:

- Teacher Dashboard
- Live Classes
- Schedule
- Learner Workspace
- Enrollment
- Attendance
- Notifications
- Recording
- Media Library

STEP 9 — CONSOLIDATE

Remove/consolidate duplicate implementations.

STEP 10 — DATABASE

Implement only genuinely required Flyway changes.

STEP 11 — TEST

Run backend and frontend tests/builds.

STEP 12 — REAL FLOW

Test the actual live workflow as far as infrastructure allows.

STEP 13 — FIX

Fix failures directly related to this task.

STEP 14 — FINAL AUDIT

Perform a final architecture, security, UX, database and realtime audit.

---

78. DO NOT WASTE TIME

Do not:

- Rebuild existing functionality
- Repeatedly inspect the same files
- Refactor unrelated modules
- Introduce unnecessary dependencies
- Create duplicate APIs
- Create duplicate entities
- Create duplicate pages
- Create duplicate services
- Create theoretical features without implementation value
- Change architecture without justification

Priority:

Correctness
    ↓
Security
    ↓
Compatibility
    ↓
Learning Integration
    ↓
Reliability
    ↓
Simplicity
    ↓
Performance
    ↓
Speed

---

79. WHAT MAKES THIS DIFFERENT

The objective is not simply:

Schedule Meeting

It is:

Schedule Learning Event

The teacher should be scheduling something that already understands:

Who is teaching?
       ↓
What course?
       ↓
Which class?
       ↓
Which subject?
       ↓
Which lesson?
       ↓
Which learners?
       ↓
When?
       ↓
How will they join?
       ↓
What will be taught?
       ↓
Will it be recorded?
       ↓
Where will the recording go?
       ↓
What happens after the class?

This makes ELMKUSOMA Live fundamentally part of the learning platform rather than a separate video feature.

---

80. TARGET EXPERIENCE

The final experience should feel like:

TEACHER

Dashboard
   ↓
Schedule Live Class
   ↓
Context-aware scheduling
   ↓
Conflict & eligibility check
   ↓
Review
   ↓
SCHEDULED
   ↓
Prepare
   ↓
Preflight
   ↓
START LIVE
   ↓
LIVE CLASSROOM
   ↓
END
   ↓
Recording Processing
   ↓
Media Library
   ↓
Replay

Learner:

Learner Workspace
   ↓
Upcoming Live Class
   ↓
Authorized Join
   ↓
Live Classroom
   ↓
Participate
   ↓
Attendance
   ↓
Leave/Rejoin
   ↓
Replay
   ↓
Learning Activity

---

81. FINAL PRODUCT ARCHITECTURE

The target conceptual architecture is:

                    ELMKUSOMA
                         │
        ┌────────────────┼────────────────┐
        │                │                │
     Teacher           Learner          Admin
        │                │                │
        └────────────────┼────────────────┘
                         │
                  ELMKUSOMA LIVE
                         │
       ┌─────────────────┼─────────────────┐
       │                 │                 │
   Scheduling        Classroom          Media
       │                 │                 │
       │             LiveKit/WebRTC      Recording
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
                  Spring Boot
                 Business Layer
                         │
       ┌─────────────────┼─────────────────┐
       │                 │                 │
   PostgreSQL          Redis          Notifications
       │
       └───────────────┬───────────────────┘
                       │
                 Object Storage
                       │
                  Media Library

---

82. FINAL LEARNING MEDIA LOOP

The completed platform should support:

PLAN
 ↓
SCHEDULE
 ↓
PREPARE
 ↓
TEACH
 ↓
INTERACT
 ↓
ATTENDANCE
 ↓
RECORD
 ↓
PROCESS
 ↓
STORE
 ↓
REPLAY
 ↓
LEARN
 ↓
ASSESS
 ↓
ANALYZE
 ↓
IMPROVE

This is the core product direction.

---

83. FINAL ACCEPTANCE CRITERIA

The implementation is considered complete only when the following are verified as applicable to the existing architecture:

Architecture

- Existing Live architecture reused
- No duplicate Live system
- No duplicate scheduling system
- No duplicate attendance system
- No duplicate recording system
- No duplicate media system
- No duplicate notification system

Scheduling

- Schedule Live Class exists
- Learning context is connected
- Date/time validation works
- Timezone handling is correct
- Participant eligibility works
- Conflict detection works where applicable
- Duplicate submission is protected
- Review stage exists
- Session becomes SCHEDULED
- Edit works where allowed
- Reschedule works where allowed
- Cancel works where allowed

Preparation

- Teacher can prepare before going live
- Camera check exists
- Microphone check exists
- Speaker check exists
- Network/service checks are real where measurable
- Teacher does not accidentally start the session while preparing

Live Classroom

- Teacher can start
- Teacher can end
- Camera works
- Microphone works
- Screen sharing works
- Participant management works
- Chat works where supported
- Raise hand works where supported
- Attendance uses real events
- Reconnection is handled

Security

- Authentication enforced
- Authorization enforced
- Institution boundaries enforced
- Enrollment/class access verified
- Short-lived LiveKit tokens used
- Secrets protected
- Recording access controlled
- Download permissions controlled

Recording

- Recording workflow integrated where configured
- Metadata stored correctly
- Actual media stored outside PostgreSQL
- Media Library integration exists
- Replay authorization works where configured

Professional Media

- Browser/phone/laptop workflow supported
- Professional camera architecture is not blocked
- OBS workflow is architecturally supported
- External encoder integration boundary is clear
- Studio mode does not interfere with ordinary teacher workflow

Reliability

- Live status is accurate
- Service degradation is handled
- Reconnection works
- Errors are meaningful
- No fake metrics
- No fake service health
- No fake participant counts

Learning Integration

- Course
- Class
- Subject
- Lesson
- Enrollment
- Attendance
- Learning activity
- Assignment/assessment where supported
- Notifications
- Media Library
- Progress where supported

remain connected to Live where applicable.

Quality

- Responsive
- Accessible
- Performant
- Secure
- Maintainable
- Production-oriented

Verification

- Backend build verified
- Backend startup verified
- Database verified
- Flyway verified
- Frontend build verified
- APIs verified
- Scheduling verified
- Teacher flow verified
- Learner flow verified
- LiveKit verified where environment permits
- Attendance verified
- Recording verified where infrastructure permits
- Media Library verified
- Reconnection verified where environment permits

---

84. FINAL REPORT — REQUIRED FORMAT

After implementation, report exactly these sections.

Existing

What was already implemented.

Changed

What was modified.

Added

What was genuinely missing and implemented.

Fixed

What was broken and how it was fixed.

Reused

Which existing:

- APIs
- entities
- services
- repositories
- components
- modules
- notification systems
- authorization systems
- media systems

were reused.

Removed / Consolidated

Any duplicate functionality that was consolidated.

Schedule Live Class

Specifically report:

- Context selection
- Date/time
- Conflict detection
- Participants
- Access control
- Live settings
- Materials
- Recording
- Notifications
- Review
- Preparation
- Reschedule
- Cancel
- Idempotency

Verification

Report actual results for:

- Backend build
- Backend startup
- Database
- Flyway
- Live APIs
- Scheduling APIs
- Authentication
- Authorization
- Conflict detection
- LiveKit
- Frontend build
- Teacher flow
- Learner flow
- Attendance
- Recording
- Media Library
- Reconnection
- Notifications

Remaining Limitations

Clearly identify:

- External services unavailable
- LiveKit limitations
- Recording infrastructure limitations
- Object storage limitations
- Features that could not be tested
- Anything requiring production infrastructure

Do not hide limitations.

Do not claim success without verification.

---

85. FINAL PRINCIPLE

The objective is NOT to create another video-meeting application.

The objective is to evolve the existing implementation into:

ELMKUSOMA LEARNING LIVE & MEDIA PLATFORM

with:

Phone
   ↓
Laptop
   ↓
Webcam
   ↓
Professional Camera
   ↓
OBS
   ↓
Studio
   ↓
Broadcast

and:

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
Attend
   ↓
Record
   ↓
Store
   ↓
Replay
   ↓
Learn
   ↓
Assess
   ↓
Analyze
   ↓
Improve

while remaining deeply integrated with the existing ELMKUSOMA platform:

Student
Teacher
Parent
Institution
Admin
Course
Class
Subject
Lesson
Learning
Assessment
Grading
Attendance
Notification
Certificate
Media
Authentication
Authorization
Enrollment

NON-NEGOTIABLE RULE

Do not break existing functionality.

Do not create duplicate systems.

Do not fake functionality.

Do not fake data.

Do not fake analytics.

Do not claim infrastructure works when it has not been verified.

Do not rebuild what already exists.

Inspect first.

Understand the existing architecture.

Reuse where possible.

Fix what is broken.

Complete what is incomplete.

Add what is genuinely missing.

Integrate the new Schedule Live Class capability into the existing lifecycle.

Verify everything before reporting completion.

END OF TASK
