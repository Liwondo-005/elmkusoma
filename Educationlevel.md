ELMKUSOMA — EDUCATION LEVEL DEVELOPMENT ALLOCATION

Developer 01–04 OpenCode Implementation Guide

Purpose

This document defines the exact education-level ownership for four developers working on the existing ELMKUSOMA platform.

The goal is to make the work easy to understand when this file is attached to OpenCode together with the corresponding implementation prompt.

---

1. MASTER EDUCATION ARCHITECTURE

ELMKUSOMA education-level routing is fixed as follows:

REGISTER
   ↓
Select Role
   ↓
STUDENT
   ↓
Select Education Level
   │
   ├── Nursery
   │      ↓
   │   Nursery Dashboard
   │
   ├── Primary
   │      ↓
   │   Primary Dashboard
   │
   ├── Secondary
   │      ↓
   │   Secondary Dashboard
   │      │
   │      ├── O-Level Context
   │      └── A-Level Context
   │
   ├── College / TVET
   │      ↓
   │   Higher Education Dashboard
   │
   └── University
          ↓
       Higher Education Dashboard

IMPORTANT ARCHITECTURE RULES

- College/TVET and University are NOT two separate top-level systems.
- They share ONE Higher Education foundation and dashboard.
- O-Level and A-Level are NOT separate registration branches.
- Secondary is one foundation with O-Level/A-Level academic context.
- Education level selected during registration is the primary routing context.
- Academic context determines the learner's detailed experience.
- Developers must preserve this architecture.

---

2. DEVELOPER OWNERSHIP

Developer| Ownership| Product
Developer 01| Nursery| ELMKUSOMA Early Learning World
Developer 02| Primary| ELMKUSOMA Primary — My Learning World
Developer 03| Secondary| ELMKUSOMA Secondary — O-Level + A-Level
Developer 04| Higher Education| College/TVET + University

Each developer owns the complete education experience for their assigned level, including where applicable:

- frontend experience
- backend/domain integration
- data requirements
- APIs
- authorization requirements
- learning workflows
- progress/evidence
- Live Learning integration
- notifications
- accessibility
- responsive/mobile experience
- low-bandwidth behavior
- testing
- documentation

DO NOT

- Create duplicate global systems.
- Create a second dashboard for an already existing shared experience.
- Create fake APIs.
- Create fake statistics.
- Create fake learner progress.
- Create fake attendance.
- Replace existing architecture.
- Rebuild the entire platform.

---

3. SHARED RULES FOR ALL DEVELOPERS

Every developer must:

1. Work inside the existing ELMKUSOMA repository.
2. Inspect the current implementation before changing anything.
3. Preserve existing working architecture.
4. Reuse existing authentication.
5. Reuse existing authorization.
6. Reuse existing users and profiles.
7. Reuse existing institutions where applicable.
8. Reuse existing enrollments.
9. Reuse existing learning relationships.
10. Reuse existing Live Learning infrastructure.
11. Never create fake production data.
12. Never create duplicate dashboards.
13. Never create duplicate APIs.
14. Never create duplicate domain entities.
15. Never assume an API exists — verify it.
16. Never silently change database semantics.
17. Keep frontend/backend contracts consistent.
18. Keep the system responsive.
19. Support mobile-first usage.
20. Consider low-bandwidth environments.
21. Follow accessibility requirements.
22. Enforce authorization on the backend.
23. Add database migrations only when genuinely required.
24. Never modify already-applied Flyway migrations.
25. Test before declaring work complete.
26. Document new APIs, entities, migrations, permissions and integrations.
27. Avoid unrelated refactoring.

---

4. SHARED LIVE LEARNING ARCHITECTURE

Live Learning is a first-class ELMKUSOMA capability.

It must NOT become a separate system for each education level.

All developers must reuse the shared Live Learning architecture.

Backend — Spring Boot

Responsible for:

- authentication
- authorization
- LiveSession lifecycle
- enrollment/eligibility
- attendance
- notifications
- recording metadata
- learning relationships
- LiveKit token generation

Media — LiveKit/WebRTC

Responsible for:

- audio
- video
- screen sharing
- media transport
- room participation

Application Realtime — Spring WebSocket/STOMP

Responsible for:

- notifications
- status updates
- attendance updates
- dashboard updates
- application-level realtime events

Expected Live Lifecycle

Schedule
   ↓
Details
   ↓
Prepare
   ↓
Preflight
   ↓
Start Live
   ↓
Backend Validation
   ↓
Short-lived LiveKit Token
   ↓
LiveKit / WebRTC
   ↓
STARTING
   ↓
LIVE
   ↓
END
   ↓
Recording / Processing
   ↓
Replay

IMPORTANT

Scheduling a session must NOT:

- start a LiveKit room
- mark the session LIVE
- issue long-lived media credentials

Attendance must represent real participation, not simply opening a page.

---

5. DEVELOPER 01 — NURSERY

Product

ELMKUSOMA Early Learning World

Product Principle

Nursery should feel like a:

«Safe, guided, interactive learning world.»

It should NOT feel like a school administration portal.

Core Learning Loop

Explore
   ↓
Play
   ↓
Think
   ↓
Create
   ↓
Move
   ↓
Talk
   ↓
Practice
   ↓
Reflect
   ↓
Grow

Main Experience

Developer 01 owns:

- My World
- Learning Journey
- Play & Learn
- Story World
- Create Studio
- Music & Movement
- Discovery Lab
- Real-World Missions
- Speak & Listen
- My Portfolio
- My Teacher
- Learn Together
- Daily Quest
- Milestones
- Learning Backpack
- Tanzania Discovery
- Feelings Check-in
- Movement Breaks
- Evidence of Learning
- Parent-supported learning

Nursery Live Learning

Integrate the shared Live Learning architecture for:

- story sessions
- songs
- music
- movement sessions
- interactive storytelling
- discovery/science activities
- guided activities
- teacher interaction
- parent-supported live learning
- recordings/replays where supported

Nursery MUST NOT introduce

- GPA
- university timetable
- semester management
- university course registration
- complex academic grading
- research workflows
- advanced university academic controls

UX Direction

Nursery should be:

- child-safe
- simple
- visual
- guided
- accessible
- low cognitive load
- parent/teacher aware
- responsive
- engaging without becoming distracting

---

6. DEVELOPER 02 — PRIMARY

Product

ELMKUSOMA PRIMARY — MY LEARNING WORLD

Core Learning Loop

Learn
   ↓
Explore
   ↓
Practice
   ↓
Create
   ↓
Solve
   ↓
Collaborate
   ↓
Apply
   ↓
Reflect
   ↓
Grow

Main Experience

Developer 02 owns:

- My World
- Learning Map/Journey
- Learning Profile
- Discovery Mode / I Wonder
- ELMKUSOMA Labs
- Reading Adventures
- Speak & Create
- Problem Solving Quests
- Create & Build
- Real World Missions
- Mistake Lab
- Challenge Zone
- Learn Together
- Family Learning
- My Learning Evidence
- Learning Backpack
- Learning Passport
- controlled AI learning support

Navigation

Home
Learn
Practice
Read
Create
Live
Progress
My Teacher
Notifications

Primary Live Learning

Live Learning Hub should support:

- Live Now
- Upcoming
- Today's Live
- Recently Completed
- Replays
- Teacher
- Subject
- Topic
- Duration
- Join Eligibility
- Materials
- Attendance
- Recording/Replay Availability

Interactive Live Learning

Where supported:

- polls
- MCQ
- quick quiz
- matching
- questions
- challenges
- drawing
- prediction
- true/false
- raise hand
- moderated chat

Product Direction

Primary must bridge Nursery and Secondary.

It must NOT become:

- Nursery 2.0
- assignments-only LMS
- generic school portal

---

7. DEVELOPER 03 — SECONDARY

Product

ELMKUSOMA SECONDARY — ACADEMIC & FUTURE WORLD

One Secondary Foundation

Secondary is ONE product foundation with two academic contexts:

SECONDARY
   │
   ├── O-LEVEL
   │    Form I–IV
   │
   └── A-LEVEL
        Form V–VI

O-LEVEL

Focus:

- strong foundations
- broad subjects
- conceptual understanding
- practice
- discipline
- assessment
- examination preparation
- strengths and interests
- next-stage preparation

A-LEVEL

Focus:

- specialization
- subject combinations
- deeper concepts
- analysis
- research-oriented learning
- independent study
- advanced problem solving
- examination preparation
- university/TVET/career transition

---

8. SECONDARY REGISTRATION RULE

The registration flow remains:

Student
   ↓
Secondary
   ↓
Secondary Dashboard
   ↓
Academic Context
   ├── O-Level
   └── A-Level

DO NOT create:

Student
   ↓
O-Level

and:

Student
   ↓
A-Level

as separate top-level education registration branches.

O-Level/A-Level is an academic context inside Secondary.

---

9. SECONDARY CORE LEARNING MODEL

General Secondary

Understand
   ↓
Practice
   ↓
Analyze
   ↓
Apply
   ↓
Assess
   ↓
Improve

A-Level Deeper Learning

Understand
   ↓
Analyze
   ↓
Connect
   ↓
Investigate
   ↓
Solve
   ↓
Argue
   ↓
Create
   ↓
Master

---

10. SECONDARY DASHBOARD

Dashboard identity:

MY ACADEMIC WORLD

It should include:

- greeting/context
- Your Next Step
- Today
- Continue Where You Left Off
- Your Focus
- Academic Pulse
- My Subjects
- Quick Learn
- Teacher Feedback
- Learning Journey
- Live Learning
- My Learning Evidence
- Future World

Navigation

My Academic World
Learn
Practice
Assess
Revision
Live
Projects
Progress
Future World
My Teachers
Notifications

---

11. SECONDARY SUBJECT WORKSPACE

Each subject experience should support where applicable:

- Overview
- Topics
- Lessons
- Practice
- Assignments
- Assessments
- Resources
- Live Classes
- Replays
- Progress
- Teacher Feedback

---

12. SECONDARY LEARNING SYSTEMS

Developer 03 should integrate/implement where supported:

- Concept Explorer
- Problem-Solving Engine
- Error Analysis
- Revision Center
- Exam Preparation
- Exam Mode
- Study Planner
- Projects
- Research
- Practical Learning
- Science Lab
- Critical Thinking
- Communication
- Portfolio
- Learning Passport
- evidence-based progress
- controlled AI

Signature

UNDERSTAND. THINK. APPLY.

O-Level:

«Build the foundation.»

A-Level:

«Build depth.»

---

13. DEVELOPER 04 — HIGHER EDUCATION

Ownership

Developer 04 owns ONE Higher Education foundation covering:

- College
- TVET
- University

These must share:

- Higher Education Dashboard
- common foundation
- authentication
- authorization
- academic context framework
- Live Campus
- shared learning infrastructure
- shared progress/evidence foundation
- shared notification architecture
- shared realtime architecture

---

14. HIGHER EDUCATION ROUTING

Student
   ↓
Select Education Level
   │
   ├── College / TVET
   │       ↓
   │   Higher Education Dashboard
   │
   └── University
           ↓
       Higher Education Dashboard

Academic context then determines the appropriate experience.

Do NOT create two completely independent higher-education platforms.

---

15. HIGHER EDUCATION PRODUCT

ELMKUSOMA HIGHER EDUCATION

Academic • Professional • Live Learning World

Signature

LEARN. APPLY. RESEARCH. BUILD.

Core Journey

Learn
   ↓
Practice
   ↓
Collaborate
   ↓
Apply
   ↓
Research
   ↓
Assess
   ↓
Build Evidence
   ↓
Connect
   ↓
Prepare for Profession

---

16. HIGHER EDUCATION DASHBOARD

Dashboard identity:

MY ACADEMIC & PROFESSIONAL WORLD

Main sections:

- academic context
- What’s Next?
- Today
- Continue Learning
- Live Campus
- My Courses / My Modules
- Academic Load
- Projects
- Research
- My Progress
- My Evidence
- Career & Professional World
- My Day / My Week
- Study Planner

---

17. HIGHER EDUCATION — WHAT'S NEXT?

"What’s Next?" must use real backend evidence.

Possible sources:

- unfinished learning
- upcoming assessment
- assignment deadline
- lecturer recommendation
- current learning
- scheduled Live Class
- revision requirement
- learner goals

Never fabricate:

- AI recommendations
- progress
- academic statistics
- attendance
- deadlines
- GPA
- performance

---

18. COLLEGE / TVET EXPERIENCE

Priority Model

Skills
   ↓
Practical
   ↓
Modules
   ↓
Assessment
   ↓
Workshop / Lab
   ↓
Live
   ↓
Fieldwork
   ↓
Portfolio

Main Features

- Practical World
- Competency Engine
- Module Workspace
- workshops/labs
- practical tasks
- competency evidence
- projects
- fieldwork
- internship
- professional portfolio
- "Show Me What You Can Do"

---

19. UNIVERSITY EXPERIENCE

Priority Model

Courses
   ↓
Academic Work
   ↓
Live Lectures
   ↓
Research
   ↓
Projects
   ↓
Assessments
   ↓
Academic Progress
   ↓
Professional Development

Main Features

- Course Workspace
- Deep Learning
- Research World
- Research Workspace
- thesis/dissertation where applicable
- academic projects
- collaboration
- academic record
- GPA only when authoritative data exists
- academic calendar
- internship/fieldwork
- professional portfolio
- career development
- professional development

---

20. HIGHER EDUCATION LIVE CAMPUS

Live Campus is a first-class Higher Education capability.

It should support where applicable:

- live lectures
- tutorials
- practical demonstrations
- laboratories
- seminars
- webinars
- guest speakers
- research presentations
- project defenses
- conferences
- professional training
- career events
- institution events
- public broadcasts where supported

Learning Loop

LIVE
  ↓
REPLAY
  ↓
RESOURCES
  ↓
PRACTICE
  ↓
ASSESSMENT
  ↓
PROGRESS

---

21. LIVE CLASSROOM

Where supported:

- camera
- microphone
- screen sharing
- participants
- chat
- raise hand
- questions
- polls
- quizzes
- whiteboard
- moderation
- recording
- learning materials
- attendance

Use the existing:

- Spring Boot
- LiveKit
- WebRTC
- Spring WebSocket/STOMP

architecture.

Do NOT create another Live system.

---

22. OPEN CODE MASTER IMPLEMENTATION INSTRUCTION

Attach this file together with the developer-specific implementation prompt.

Every developer must follow this instruction:

«You are working on the existing ELMKUSOMA production repository.

First inspect the repository, architecture, existing routes, APIs, entities, migrations, services, components, authentication, authorization, enrollment model, and existing Live Learning implementation.

Do NOT rebuild ELMKUSOMA.

Do NOT replace the current architecture.

Do NOT introduce a new technology stack.

Do NOT create duplicate systems.

Do NOT create fake data.

Do NOT assume an endpoint exists — verify it.

Do NOT change unrelated modules.

Implement only the education-level scope assigned to you in this document and the attached developer-specific prompt.

Before coding:

1. Inspect the existing implementation.
2. Identify reusable components and APIs.
3. Identify the existing database/domain model.
4. Identify existing routes and dashboard architecture.
5. Identify the existing Live Learning architecture.
6. Identify dependencies on shared platform services.
7. Produce a short implementation plan.

Then implement the required functionality.

For every feature:

- connect it to real backend data;
- enforce authorization server-side;
- preserve existing contracts unless a justified change is required;
- reuse existing shared services;
- keep the UI responsive;
- support mobile and low-bandwidth usage;
- maintain accessibility;
- handle loading, empty, error and permission states;
- avoid fake metrics;
- avoid duplicate APIs/components/models.

For database changes:

- inspect existing migrations first;
- create a proper Flyway migration only when required;
- never modify old migrations that may already be applied;
- keep data integrity and relationships correct.

For Live Learning:

- reuse the existing LiveSession/LiveKit/WebRTC/WebSocket architecture;
- never create a second media system;
- never issue long-lived LiveKit credentials;
- ensure authorization and enrollment/eligibility are checked by the backend;
- record actual participation for attendance.

Testing:

- run relevant backend tests;
- run relevant frontend tests;
- run lint/type checks where available;
- run production build checks where available;
- test authorization;
- test responsive behavior;
- test real API integration;
- test empty/error/loading states;
- test the affected user journeys.

At the end report:

1. What you inspected.
2. What you changed.
3. Files/modules changed.
4. APIs/entities/migrations changed.
5. Shared systems reused.
6. Tests executed and results.
7. Any blockers or decisions requiring another developer.
8. Any cross-developer dependency.

Do not claim a feature is complete if it is only a UI mock.»

---

23. DEVELOPER HANDOFF RULE

The four developers can work independently within their assigned education level.

Developer 01
    ↓
Nursery

Developer 02
    ↓
Primary

Developer 03
    ↓
Secondary
(O-Level + A-Level)

Developer 04
    ↓
Higher Education
(College/TVET + University)

Shared capabilities must remain shared.

If a developer discovers that a required shared capability is missing:

DO NOT
↓
Create private duplicate

DO
↓
Document the dependency
↓
Coordinate shared implementation
↓
Reuse the shared capability

---

24. FINAL DEFINITION OF DONE

An education level is considered implemented only when:

- routing works correctly
- dashboard loads real data
- authorization is enforced
- core learning journeys work
- education context is correctly recognized
- required backend support exists
- no fake production data remains
- loading states work
- empty states work
- error states work
- permission states work
- mobile/responsive experience works
- accessibility is considered
- Live Learning integrates with the shared architecture
- attendance uses real participation
- notifications use real backend events where applicable
- relevant tests pass
- no duplicate architecture was introduced
- developer documents cross-team dependencies

---

25. FINAL PLATFORM PRINCIPLE

The objective is NOT to create four isolated applications.

The objective is to build:

                 ONE ELMKUSOMA PLATFORM
                         │
          ┌──────────────┼──────────────┐
          │              │              │
       Shared         Shared         Shared
       Auth           Data           Live
          │              │              │
          └──────────────┼──────────────┘
                         │
       ┌─────────────────┼─────────────────┐
       │                 │                 │
     Nursery           Primary          Secondary
       │                 │                 │
       │                 │            O-Level/A-Level
       │                 │
       └─────────────────┼─────────────────┐
                         │                 │
                   Higher Education        │
                    College/TVET           │
                    University             │
                         │
                         └─────────────────

ONE PLATFORM. SHARED FOUNDATIONS. CONTEXT-AWARE EDUCATION EXPERIENCES.

The four developers own different education experiences, but they are building one ELMKUSOMA ecosystem.
