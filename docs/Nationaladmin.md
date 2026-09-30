ELMKUSOMA — MASTER OPENCODE

NATIONAL ADMINISTRATION & NATIONAL EDUCATION GOVERNANCE COMMAND CENTER

FULL PRODUCTION IMPLEMENTATION + INTEGRATION + REAL DATA + USER COMMUNICATION + SECURITY + AUDIT + E2E VERIFICATION

---

0. MISSION

You are implementing the National Admin / National Education Governance Command Center inside the existing ELMKUSOMA production repository.

This is an existing production system.

DO NOT rebuild ELMKUSOMA.

DO NOT create a parallel education platform.

DO NOT replace existing authentication, authorization, learner, teacher, institution, course, learning content, Live, notification, search, analytics, reporting, audit, communication, media, payment, or security systems.

Your responsibility is to:

«INSPECT → AUDIT → REUSE → EXTEND → INTEGRATE → IMPLEMENT → TEST → SECURE → VERIFY»

The final result must be a fully functional National Administration ecosystem, not a static dashboard.

Every visible number, table, chart, badge, notification, action, search result, report, status, user interaction and navigation item must be backed by real system data or a clearly defined existing source.

NO MOCK DATA.

NO FAKE STATISTICS.

NO DEMO RECORDS.

NO DEAD BUTTONS.

NO UI-ONLY IMPLEMENTATION.

---

1. CORE OBJECTIVE

Build the:

NATIONAL EDUCATION GOVERNANCE & OVERSIGHT COMMAND CENTER

Its purpose is to allow authorized National Administrators to monitor, understand, govern and coordinate the national education ecosystem across:

National
   ↓
Region
   ↓
District
   ↓
Ward
   ↓
School / Institution
   ↓
Class
   ↓
Teacher
   ↓
Learner

The National Admin must be able to move through this hierarchy using real data while respecting authorization, jurisdiction, institution ownership and existing security rules.

The dashboard must connect to the wider ELMKUSOMA ecosystem:

National Admin
      │
      ├── Regions
      ├── Districts
      ├── Wards
      ├── Schools
      ├── Institutions
      ├── Teachers
      ├── Learners
      │
      ├── Courses
      ├── Subjects
      ├── Lessons
      ├── Resources
      ├── Video Tutorials
      ├── Live Learning
      ├── Assignments
      ├── Quizzes
      ├── Attendance
      ├── Progress
      ├── Assessments
      │
      ├── Verification
      ├── Governance
      ├── Data Quality
      ├── Audit
      ├── Communication
      └── Reports

---

2. NON-NEGOTIABLE EXISTING-SYSTEM RULE

Before changing anything:

INSPECT THE REPOSITORY.

Do not assume the architecture.

Search and document existing:

- Authentication
- Users
- Roles
- Permissions
- Authorities
- Jurisdiction
- Organization/institution scope
- Regions
- Districts
- Wards
- Schools
- Institutions
- Teachers
- Learners
- Classes
- Courses
- Subjects
- Lessons
- Resources
- Media
- Videos
- Live classes
- LiveKit
- Attendance
- Assignments
- Quizzes
- Assessments
- Progress
- Enrollment
- Notifications
- Messaging
- Announcements
- Search
- Analytics
- Reporting
- Audit
- Verification
- Data quality
- Security
- File storage
- Object storage
- Realtime events
- WebSocket/STOMP/SSE
- Existing admin dashboards
- Existing sidebar/navigation
- Existing UI components
- Existing design system
- Existing API conventions
- Existing DTOs
- Existing services
- Existing repositories
- Existing database schema
- Existing Flyway migrations
- Existing tests

Reuse existing systems wherever possible.

---

3. FIRST TASK — NATIONAL ADMIN AUDIT

Before implementation create a detailed audit.

Classify every requirement:

- VERIFIED EXISTING
- PARTIAL
- BROKEN
- MISSING
- DUPLICATED
- INCONSISTENT
- UNSUPPORTED
- NOT APPLICABLE

Every finding must include evidence.

Use:

REQUIREMENT
FACT
VERIFIED
ASSUMPTION
INFERENCE

Do not claim something is complete merely because:

- a sidebar item exists
- a page renders
- a button exists
- an API endpoint exists
- a database table exists

A feature is only VERIFIED when the complete flow works:

UI
→ Route
→ API
→ Authentication
→ Authorization
→ Service
→ Database / Existing Service
→ Response
→ UI State
→ Error Handling
→ Audit where applicable

---

4. NATIONAL ADMIN BOUNDARY

National Admin is NOT Platform Admin.

Platform Admin

Platform Admin governs:

- platform operations
- infrastructure
- platform configuration
- platform-wide security
- platform services
- system governance
- platform-wide identity
- technical operations

National Admin

National Admin governs and monitors:

- national education structure
- regions
- districts
- wards
- schools
- institutions
- teachers
- learners
- learning activity
- education performance
- attendance
- assessments
- learning content governance
- verification
- data quality
- national communication
- national reports
- national education analytics

Institution / Provider Admin

Institution Admin operates within the institution/provider scope.

DO NOT give National Admin unrestricted platform infrastructure powers merely because the role is called “National Admin”.

Authorization must remain explicit.

---

5. NATIONAL ADMIN ACCESS MODEL

Use the existing ELMKUSOMA authorization architecture.

The backend remains authoritative.

Conceptually:

ACCESS =
ROLE
+
PERMISSION
+
JURISDICTION
+
RESOURCE OWNERSHIP
+
ORGANIZATION / INSTITUTION SCOPE
+
RESOURCE STATE
+
SERVICE CAPABILITY

National Admin must only access resources within the authorized national jurisdiction.

Never trust:

- frontend filters
- hidden buttons
- route parameters
- query parameters
- IDs supplied by the browser

The backend must validate scope.

---

6. DASHBOARD UX

Build a premium ELMKUSOMA light education/governance interface.

Use existing ELMKUSOMA design system wherever available.

Preferred identity:

Primary Blue: #2563EB
Teal:         #0D9488
Orange:       #F59E0B
White:        #FFFFFF
Soft:         #F8FAFC
Text:         #1E293B

Avoid:

- dark generic admin templates
- excessive gradients
- excessive shadows
- oversized rounded cards
- fake AI effects
- meaningless animations
- random icon styles
- fake charts
- fake statistics

The interface must feel:

- professional
- institutional
- modern
- trustworthy
- educational
- data-driven
- accessible
- responsive

---

7. NATIONAL ADMIN SIDEBAR

Implement a complete, usable sidebar.

Header

ELMKUSOMA
National Administration

COMMAND CENTER

Overview
National Pulse

EDUCATION

Regions
Districts
Wards
Schools
Institutions

PEOPLE

Learners
Teachers
Education Staff

LEARNING

Courses
Subjects
Lessons
Resources
Video Tutorials
Live Learning

PERFORMANCE

Learner Progress
Attendance
Assessments
Engagement

GOVERNANCE

Verification
Data Quality
Compliance
Audit

REPORTS

National Reports
Analytics
Scheduled Reports

COMMUNICATION

Announcements
Notifications

Sidebar capabilities

Implement:

- collapsible groups
- active route state
- accessible icons
- real pending-count badges
- responsive behavior
- desktop collapse
- mobile drawer
- keyboard navigation
- tooltips for collapsed state
- current scope indicator
- user profile footer
- Quick Actions
- Notifications
- Help & Support

Do not create duplicate navigation systems if the repository already has an equivalent.

---

8. GLOBAL NATIONAL CONTEXT

The dashboard must support contextual scope.

Example:

Scope
[ National ▾ ]

Region
[ All Regions ▾ ]

District
[ All Districts ▾ ]

Ward
[ All Wards ▾ ]

Institution
[ All Institutions ▾ ]

Education Level
[ All ▾ ]

Period
[ This Month ▾ ]

When National Admin selects:

National

the dashboard shows national data.

When selecting:

Dar es Salaam

the dashboard becomes Dar es Salaam scoped.

When selecting:

Kinondoni

it becomes Kinondoni scoped.

The same dashboard architecture should support:

National
→ Region
→ District
→ Ward
→ Institution / School
→ Class

Do not create separate duplicated dashboards for every level unless the existing architecture requires separate bounded contexts.

---

9. COMMAND CENTER

Create the main National Admin landing page.

Header:

National Education Overview
Monitor education participation, learning activity,
institutions, teachers and learners across the national ecosystem.

Show:

- current scope
- selected period
- last updated timestamp
- refresh behavior where appropriate

Do not display fake “last updated” values.

---

10. NATIONAL KPI OVERVIEW

Create real KPI cards for applicable metrics:

Regions
Districts
Schools
Institutions
Teachers
Learners
Courses
Subjects

Each card must support:

- real value
- label
- contextual trend if historical data exists
- clickable navigation where appropriate
- loading state
- empty state
- error state

Never invent percentage changes.

If comparison data does not exist:

DO NOT display a fabricated trend.

---

11. NATIONAL LEARNING PULSE

Create a central analytics section.

Use real existing analytics/activity data.

Potential dimensions:

Learner Activity
Teacher Activity
Course Activity
Lesson Activity
Assessment Activity
Attendance
Live Learning
Video Learning
Resource Usage

Use tabs or a switcher rather than creating unnecessary charts.

Charts must:

- represent actual backend data
- have meaningful axes
- show time range
- handle empty data
- handle loading
- handle errors
- be accessible
- avoid misleading visualizations

---

12. ATTENTION CENTER

Create:

ATTENTION REQUIRED

Potential real items:

- pending institution verification
- data quality issues
- content review
- governance actions
- compliance items
- unresolved administrative issues

Every alert must come from a real source.

Clicking an alert must lead to the relevant existing page/action.

Do not generate random warnings.

---

13. NATIONAL EDUCATION MAP

If existing mapping/GIS infrastructure is available:

Implement national map visualization.

Show real:

- regions
- schools
- institutions
- learners
- teachers
- activity

Support drill-down:

National
→ Region
→ District
→ Ward
→ School

If GIS infrastructure does not exist:

DO NOT create an unnecessary complex GIS platform.

Implement the best supported geographical visualization using existing data/infrastructure, or clearly report the unsupported part.

---

14. REGIONAL PERFORMANCE

Implement a real regional table.

Columns may include:

Region
Institutions
Schools
Teachers
Learners
Learning Activity
Attendance
Assessment Activity

Do not automatically rank regions as “best” or “worst”.

Present factual measurements.

Allow:

- search
- filtering
- pagination
- sorting where meaningful
- drill-down
- export where authorized

---

15. PEOPLE OVERVIEW

Learners

Show real:

- total learners
- active learners
- enrollment
- learning activity
- progress
- attendance
- assessment participation

Teachers

Show real:

- total teachers
- active teachers
- teaching activity
- courses/content
- live teaching
- assessments

Institutions

Show:

- total
- active
- verification state
- provider type
- geographical distribution

---

16. LEARNING ECOSYSTEM

Integrate existing:

Courses
Subjects
Modules
Lessons
Resources
Video Tutorials
Assignments
Quizzes
Live Learning
Recordings

Do not create duplicate content systems.

The National Admin should be able to inspect national learning activity and governance state according to permissions.

---

17. LEARNING CONTENT GOVERNANCE

Integrate with the existing Learning Content ecosystem.

Support visibility into:

- lessons
- resources
- videos
- live recordings
- publication state
- processing state
- flagged content
- archived content
- ownership
- institution
- course
- subject

Reuse existing content ownership and lifecycle.

Do not create another Resource Library.

---

18. LIVE LEARNING

Integrate with existing ELMKUSOMA Live.

Show real:

Currently Live
Scheduled Today
Completed Sessions
Recordings
Replay Activity
Attendance
Participation

Do NOT rebuild:

- LiveKit
- attendance
- recording
- media storage
- Live class architecture

Reuse existing services.

---

19. LEARNER PERFORMANCE

Create a national performance view using existing learner/progress systems.

Possible metrics:

- course progress
- lesson completion
- assessment participation
- assignment completion
- attendance
- learning engagement

Allow filters:

Education Level
Region
District
Institution
School
Class
Subject
Course
Period

Never expose unauthorized learner-level information.

---

20. ATTENDANCE

Integrate existing attendance systems.

Support factual views such as:

- attendance activity
- school attendance
- class attendance
- Live class attendance

National Admin should be able to drill down only according to permission.

---

21. ASSESSMENTS

Integrate existing:

- quizzes
- assignments
- assessments
- grading/progress
- completion

Show:

- participation
- completion
- pending activity
- result summaries where authorized

Do not create duplicate assessment systems.

---

22. GOVERNANCE

Implement:

Verification
Data Quality
Compliance
Audit

Each must connect to existing systems.

Verification

Show real:

- pending
- approved
- rejected
- requiring action

Data Quality

Identify real:

- missing required information
- incomplete records
- inconsistent relationships
- invalid data
- duplicates where detectable by existing rules

Do not modify data automatically without explicit workflow.

Audit

Use existing audit system.

Do not create another audit table unless absolutely necessary.

---

23. COMMUNICATION WITH ALL RELEVANT USERS

This is a critical requirement.

National Admin must be able to communicate through the existing ELMKUSOMA communication/notification infrastructure.

Support authorized targeting such as:

All National Users
Regions
Districts
Institutions
Schools
Teachers
Learners
Education Staff
Specific authorized groups

Potential communication types:

- national announcement
- education notice
- administrative notification
- targeted notification
- scheduled announcement
- urgent notice where existing infrastructure supports it

The system must preserve:

Sender
Recipient / Audience
Scope
Message
Created At
Scheduled At
Delivered At
Read State
Status

Reuse the existing notification system.

Do not create a second notification platform.

---

24. USER-TO-USER / ECOSYSTEM COMMUNICATION

National Admin integration must respect existing communication boundaries.

Do not automatically grant National Admin access to private conversations.

Where existing systems support:

- announcements
- notifications
- official communication
- institutional communication

integrate them.

Private messaging remains subject to existing authorization.

---

25. REAL-TIME UPDATES

If the existing system already has:

- WebSocket
- STOMP
- SSE
- event bus
- Redis pub/sub
- notifications

reuse it.

Possible real-time events:

INSTITUTION_VERIFICATION_UPDATED
ANNOUNCEMENT_PUBLISHED
CONTENT_REVIEW_UPDATED
LIVE_SESSION_STARTED
LIVE_SESSION_ENDED
DATA_QUALITY_ALERT
GOVERNANCE_ACTION_UPDATED
NOTIFICATION_RECEIVED

Do not create a second realtime architecture.

If realtime is not required for a particular metric, use appropriate refresh/fetch behavior instead.

---

26. REPORTING

Implement National Reports using the existing reporting/export infrastructure.

Possible reports:

National Education Report
Regional Report
District Report
Institution Report
Learner Report
Teacher Report
Learning Activity Report
Attendance Report
Assessment Report
Live Learning Report
Content Governance Report
Data Quality Report

Support:

- view
- generate
- filter
- export
- date range
- scope

Only expose reports permitted by authorization.

---

27. SCHEDULED REPORTS

If the existing reporting infrastructure supports scheduling:

Allow National Admin to configure authorized reports such as:

- daily
- weekly
- monthly

If no scheduler exists:

Do not build a duplicate scheduler without first assessing the existing automation infrastructure.

---

28. SEARCH

Reuse the existing global/search architecture.

National Admin should be able to search authorized:

- institutions
- schools
- teachers
- learners
- courses
- subjects
- lessons
- resources
- reports

Search must respect scope and authorization.

No second global search engine.

---

29. QUICK ACTIONS

Provide contextual actions:

Create Announcement
Review Verification
Open Data Quality
Generate Report
View Regional Data
Open Audit

Every action must work.

No placeholder buttons.

---

30. COMMAND PALETTE

If the existing UI architecture supports it, implement:

Ctrl + K

Search/navigation actions:

Go to Regions
Go to Districts
Find Institution
Find Learner
Find Teacher
Open Reports
Open Verification
Open Data Quality
Create Announcement

All results must respect authorization.

---

31. FAVORITES / RECENT ITEMS

If appropriate within the existing frontend architecture:

Support:

- pinned pages
- recent views

Do not create unnecessary backend persistence if local UI preferences are sufficient.

If persisted, use an existing preference/settings mechanism.

---

32. NOTIFICATIONS

Integrate with the existing notification center.

Sidebar may display a real unread count.

Notification center must support:

- unread/read
- timestamp
- source
- navigation target
- appropriate category

No fake notifications.

---

33. PROFILE / SECURITY

National Admin profile area should integrate with existing:

- account
- profile
- security
- session management
- password/security controls

Do not create another authentication system.

---

34. DATABASE

Inspect existing schema before adding anything.

Search for existing entities/tables:

- users
- roles
- permissions
- regions
- districts
- wards
- schools
- institutions
- classes
- teachers
- learners
- courses
- subjects
- lessons
- resources
- media
- videos
- recordings
- attendance
- assessments
- assignments
- quizzes
- progress
- notifications
- announcements
- reports
- analytics
- audit
- verification
- data quality

Reuse existing structures.

If new persistence is genuinely required:

- use additive Flyway migration
- never rewrite old migrations
- never duplicate existing tables
- add constraints/indexes intentionally
- preserve institution_id and scope relationships
- document why the table is required

---

35. BACKEND API

Inspect existing API conventions.

Implement only missing endpoints.

Potential capabilities:

National overview
Regional summaries
District summaries
Institution summaries
Learner summaries
Teacher summaries
Learning summaries
Performance summaries
Governance summaries
Attention items
Reports
Announcements
Notifications

Every endpoint must enforce:

- authentication
- role
- permission
- jurisdiction
- institution scope
- resource ownership
- state
- appropriate data visibility

---

36. PERFORMANCE

Audit for:

- N+1 queries
- huge payloads
- duplicate API requests
- unnecessary polling
- expensive dashboard queries
- missing indexes
- unbounded lists
- unnecessary frontend rerenders

Use:

- pagination
- aggregation queries
- projection DTOs
- caching where justified
- lazy loading
- debounced search
- server-side filtering

Do not over-engineer.

---

37. DATA PRIVACY

National-level access does NOT mean unrestricted personal-data exposure.

Only display learner/teacher information necessary for the National Admin's authorized duties.

Avoid unnecessary:

- sensitive personal information
- private communications
- credentials
- secrets
- private records

Audit sensitive administrative actions where the existing audit architecture supports it.

---

38. SECURITY TESTS

Explicitly test:

Authentication

- unauthenticated user
- expired session
- invalid token

Authorization

- Teacher accessing National Admin
- Learner accessing National Admin
- Institution Admin accessing National Admin
- unauthorized admin permission
- wrong jurisdiction

IDOR

Manipulate:

- region ID
- district ID
- institution ID
- school ID
- learner ID
- teacher ID
- report ID

Verify backend rejects unauthorized access.

Cross-institution

Verify National Admin functionality does not accidentally leak unrelated protected information through APIs that bypass intended scope.

Communication

Verify unauthorized users cannot:

- send national announcements
- target protected audiences
- read unauthorized administrative communications

---

39. FRONTEND ROUTE AUDIT

Verify:

Sidebar
→ Route
→ Page
→ API
→ Backend authorization
→ Real data

Audit every navigation item.

No dead routes.

No placeholder pages.

No buttons that only show a toast.

---

40. RESPONSIVE DESIGN

Desktop:

- full sidebar
- multi-column dashboard

Tablet:

- collapsed/sidebar adaptive
- two-column sections

Mobile:

- drawer navigation
- stacked metrics
- responsive charts
- horizontally scrollable data tables only where necessary
- accessible filters

Do not destroy existing mobile behavior.

---

41. ACCESSIBILITY

Implement/verify:

- keyboard navigation
- visible focus
- semantic buttons
- labels
- ARIA where needed
- sufficient contrast
- screen-reader meaningful navigation
- accessible charts/data alternatives
- mobile touch targets

---

42. LOADING / EMPTY / ERROR STATES

Every dashboard section must support:

Loading
Loaded
Empty
Error
Retry
Unauthorized
Unavailable

Do not display zero merely because data failed to load.

Do not display fake fallback statistics.

---

43. REAL DATA RULE

The following are forbidden:

mockData
fakeStats
dummyUsers
sampleLearners
hardcodedCounts
fakeCharts
staticNotificationCounts
placeholderReports
demoRecords

If real data is unavailable:

show:

No data available

or an appropriate state.

---

44. INTEGRATION WITH ALL EXISTING USERS

Verify communication and data relationships across:

National Admin
Platform Admin
Institution Admin
School Admin where supported
Teacher
Learner
Parent where applicable
Education Authority roles

The National Admin must not replace these users.

Each role remains responsible for its own scope.

Examples:

Teacher creates lesson
        ↓
Lesson becomes available according to publication/access rules
        ↓
Learner learns
        ↓
Progress recorded
        ↓
National analytics may aggregate authorized metrics

Another example:

Institution registered
        ↓
Verification
        ↓
Institution becomes active
        ↓
Teachers/Learners operate
        ↓
National Admin sees authorized national statistics

Another:

Teacher starts Live Class
        ↓
Learners join
        ↓
Attendance/participation recorded
        ↓
Recording generated
        ↓
Recording becomes media/resource
        ↓
Learning Content can attach it
        ↓
National Admin sees aggregated Live activity

---

45. NO DUPLICATION

Do not create duplicate:

- learner service
- teacher service
- institution service
- course service
- lesson service
- resource library
- media service
- video service
- Live service
- attendance service
- progress service
- notification service
- search service
- analytics service
- audit service
- authorization service
- reporting service

If an existing service is imperfect:

EXTEND OR FIX IT where appropriate.

Do not create a parallel version simply because it is easier.

---

46. TESTING MATRIX

Create tests for:

Dashboard

- national overview
- scoped overview
- KPI accuracy
- empty state
- error state

Geography

- region
- district
- ward
- school
- institution
- drill-down

People

- learners
- teachers
- institutions

Learning

- courses
- subjects
- lessons
- resources
- videos
- Live

Performance

- progress
- attendance
- assessments
- engagement

Governance

- verification
- data quality
- compliance
- audit

Communication

- announcements
- notifications
- audience targeting
- read/unread

Reports

- generation
- filtering
- authorization
- export

Security

- role boundaries
- jurisdiction
- institution boundary
- IDOR
- unauthorized endpoints

---

47. END-TO-END SCENARIOS

Scenario A — National Overview

1. Login as National Admin
2. Open National Admin
3. Dashboard loads
4. KPI values come from real APIs
5. Regional table loads
6. Learning analytics loads
7. Attention center loads
8. No fake data

Scenario B — Drill Down

National
→ Region
→ District
→ Institution
→ School

Verify each transition.

Scenario C — Communication

1. National Admin creates announcement
2. Selects authorized audience
3. Backend validates permission
4. Announcement persists
5. Notification is generated through existing system
6. Target user receives it
7. Read state works
8. Audit is recorded where supported

Scenario D — Learning Integration

1. Teacher creates lesson
2. Learner accesses lesson
3. Learner progress is recorded
4. National analytics reflects aggregated activity

Scenario E — Live Integration

1. Teacher schedules Live class
2. Learners join
3. Attendance records
4. Live session ends
5. Recording processes
6. Recording becomes available
7. National Live analytics reflects session

Scenario F — Security

1. Non-National user attempts National Admin route
2. Backend rejects request
3. Direct API request also rejects
4. Manipulated IDs cannot cross scope

---

48. GIT SAFETY

Before implementation:

git status
git branch
git diff

Do not:

- reset
- clean unrelated changes
- overwrite existing work
- remove unrelated features

After implementation:

git status
git diff
git diff --stat

Clearly identify:

- files created
- files modified
- migrations added
- tests added
- unrelated pre-existing changes

---

49. REQUIRED VALIDATION

Backend:

mvn clean package -DskipTests
mvn test

Frontend:

npm install
npx tsc --noEmit
npm run build

Use the project's actual commands if they differ.

Also perform:

- API tests
- authorization tests
- integration tests
- frontend route tests where available
- E2E tests where available
- manual browser verification
- responsive verification

---

50. FINAL VERIFICATION REPORT

At completion produce:

A. Executive Summary

What was implemented.

B. Requirement Matrix

Requirement | Before | After | Evidence

C. Completion

Calculate from actual verified requirements.

Do NOT say:

«100% complete»

unless every applicable requirement is actually verified.

D. Dashboard

- overview
- KPIs
- learning pulse
- attention center
- map
- regional performance
- people
- learning
- Live
- governance
- reports
- communication

E. Sidebar

- navigation
- collapsible groups
- badges
- responsive
- active state
- quick actions
- notifications
- profile

F. Backend

- APIs
- services
- authorization
- queries
- integrations

G. Database

- tables reused
- migrations added
- indexes
- relationships

H. Communication

- announcements
- notifications
- audiences
- delivery/read state

I. Security

- role tests
- jurisdiction tests
- IDOR tests
- cross-scope tests

J. Testing

Provide:

Backend:
Tests:
Passed:
Failed:
Skipped:

Frontend:
TypeScript:
Build:

K. E2E

Report actual verified flows.

L. Remaining Blockers

Clearly distinguish:

VERIFIED
PARTIAL
BLOCKED
NOT IMPLEMENTED
UNSUPPORTED

Do not hide blockers.

---

51. FINAL IMPLEMENTATION PRINCIPLE

The final National Admin experience must follow:

SEE
↓
UNDERSTAND
↓
EXPLORE
↓
DRILL DOWN
↓
IDENTIFY ATTENTION
↓
COMMUNICATE / ACT
↓
REPORT
↓
AUDIT

The final architecture must remain:

Existing ELMKUSOMA
        +
National Administration
        +
Existing Shared Services
        +
Real Data
        +
Existing Authorization
        +
Existing Communication
        +
Existing Learning
        +
Existing Live
        +
Existing Analytics
        +
Existing Reporting

NOT:

Existing ELMKUSOMA
        +
Duplicate National Platform

---

52. DEFINITION OF DONE

This task is DONE only when:

- National Admin has a complete dashboard
- Sidebar is fully functional
- Navigation works
- Dashboard uses real data
- APIs are real
- Database integration is real
- Authorization is enforced server-side
- National → Region → District → Ward → School drill-down works where supported
- Learner/Teacher/Institution data integrates correctly
- Learning Content integrates
- Live integrates
- Progress integrates
- Attendance integrates
- Assessment integrates
- Governance integrates
- Notifications integrate
- Announcements integrate
- Reports integrate
- Search integrates
- Realtime behavior reuses existing infrastructure where applicable
- Mobile works
- Accessibility is addressed
- Loading/empty/error states work
- No fake data exists
- No dead buttons exist
- No duplicate core systems were created
- Existing ELMKUSOMA functionality remains intact
- Security tests pass
- E2E scenarios pass
- Git changes are reviewed
- Final evidence-based audit is produced

DO NOT STOP AT UI.

DO NOT STOP AT API.

DO NOT STOP AT DATABASE.

DO NOT STOP AT “PAGE LOADS”.

TRACE THE COMPLETE SYSTEM:

USER
→ SIDEBAR
→ ROUTE
→ UI
→ API
→ AUTHORIZATION
→ SERVICE
→ DATABASE / EXISTING DOMAIN SERVICE
→ REAL DATA
→ RESPONSE
→ UI
→ USER ACTION
→ COMMUNICATION / EVENT
→ AUDIT

The final result must be a production-grade National Education Governance & Oversight Command Center fully integrated into ELMKUSOMA, while preserving every existing working system and existing user experience outside the scope of this implementation.
