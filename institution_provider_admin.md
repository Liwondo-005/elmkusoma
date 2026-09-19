ELMKUSOMA

INSTITUTION / PROVIDER ADMIN

MASTER REFERENCE & ARCHITECTURE SPECIFICATION

---

DOCUMENT CONTROL

Field| Details
Product| ELMKUSOMA
Document| Institution / Provider Admin
Document Type| Master Product, Architecture & Implementation Reference
Scope| Organization-Level Administration
Status| Production Architecture Reference
Primary Principle| One Shared Organization Administration Foundation → Multiple Context-Aware Experiences

---

1. EXECUTIVE SUMMARY

The ELMKUSOMA Institution / Provider Admin is the organization-level control plane of the ELMKUSOMA ecosystem.

It enables authorized organizations to manage their own:

- Organization
- People
- Learners
- Teachers
- Lecturers
- Instructors
- Trainers
- Programs
- Courses
- Learning Content
- Resources
- Live Learning
- Events
- Media
- Certificates
- Payments
- Entitlements
- Communication
- Reports
- Analytics
- Organization Operations

The system must use one shared administration foundation while adapting the experience according to:

Organization Type
+
Enabled Services
+
Admin Role
+
Permissions
+
Organization Scope
+
Resource Ownership

The Institution / Provider Admin is NOT the same as:

- Platform Admin
- Education Authority
- Teacher
- Lecturer
- Instructor
- Learner
- Parent

Each role has a separate responsibility boundary.

---

2. CORE PRODUCT PRINCIPLE

ELMKUSOMA must NOT create a completely separate administration system for every organization type.

The architecture should follow:

Organization Type
        +
Enabled Services
        +
Admin Role
        +
Permissions
        +
Organization Scope
        +
Resource Ownership
        ↓
Adaptive Organization Workspace

The foundation remains shared.

The experience adapts.

---

3. ELMKUSOMA ADMIN ECOSYSTEM

ELMKUSOMA
│
├── PLATFORM ADMIN
│      Platform-wide Control Plane
│
├── INSTITUTION / PROVIDER ADMIN
│      Organization-level Control Plane
│
├── EDUCATION AUTHORITY
│      Education Oversight & Intelligence
│
├── TEACHER / LECTURER / INSTRUCTOR
│      Teaching & Learning Delivery
│
├── LEARNER
│      Learning Experience
│
└── PARENT
       Authorized Learner Support / Visibility

These systems must remain logically separated.

---

4. PLATFORM ADMIN

Platform Admin operates at the ELMKUSOMA platform level.

Potential responsibilities include:

- Platform governance
- Organization approval
- Organization activation
- Organization governance
- Platform configuration
- Global services
- Global security
- Global integrations
- Platform analytics
- Platform audit
- Platform lifecycle
- Platform support
- Platform-wide policies

Platform Admin is above organization-level administration.

---

5. INSTITUTION / PROVIDER ADMIN

Institution / Provider Admin operates within a specific organization or explicitly authorized organizational scope.

Potential responsibilities include:

- Organization profile
- Organization structure
- Organization users
- Organization memberships
- Courses
- Programs
- Learning content
- Resources
- Live classes
- Events
- Media
- Certificates
- Payments where enabled
- Entitlements where enabled
- Notifications
- Reports
- Analytics
- Organization settings

Organization Admin must never automatically receive platform-wide privileges.

---

6. EDUCATION AUTHORITY

Education Authority is a separate oversight layer.

Organization Admin
        ↓
Manages OWN organization

Education Authority
        ↓
Oversees authorized education jurisdiction

An Education Authority may monitor:

- Schools
- Institutions
- Learners
- Teachers
- Attendance
- Assessments
- Curriculum
- Live Learning
- Performance
- Reports
- Alerts
- Interventions

However, Education Authority and Organization Admin must remain separate authorization domains.

A government organization administrator does not automatically become an Education Authority.

---

7. ORGANIZATION TYPES

The shared organization foundation should support different organizational contexts.

Potential contexts include:

7.1 Formal Education

- School
- College
- TVET
- University

7.2 Professional / Skills

- Training Provider
- Professional Body
- Skills Provider

7.3 Organization-Based Learning

- Company
- Bank
- NGO
- Government Organization

7.4 Content / Learning Provider

- Education Content Provider
- Learning Publisher

7.5 Event / Learning Provider

- Seminar Provider
- Workshop Provider
- Webinar Provider
- Event Organizer

These are conceptual organization contexts.

The actual implementation must inspect the repository first and reuse existing organization models.

---

8. ADAPTIVE ORGANIZATION ADMINISTRATION

The organization workspace should be determined by:

Organization Type
        +
Enabled Services
        +
Admin Role
        +
Permissions
        +
Organization Scope

Example:

SCHOOL
+
COURSES
+
LIVE
+
ATTENDANCE
+
ASSESSMENTS
        ↓
SCHOOL ADMIN WORKSPACE

Example:

UNIVERSITY
+
COURSES
+
LIVE
+
RESEARCH
+
ASSESSMENTS
        ↓
UNIVERSITY ADMIN WORKSPACE

Example:

EVENT PROVIDER
+
EVENTS
+
LIVE
+
CERTIFICATES
        ↓
EVENT PROVIDER WORKSPACE

The underlying platform remains shared.

---

9. SHARED ORGANIZATION ADMIN FOUNDATION

The shared foundation should support, where applicable:

- Organization context
- Organization profile
- Organization membership
- Roles
- Permissions
- Scope
- People management
- Learning management
- Content management
- Live management
- Event management
- Media
- Resources
- Certificates
- Payments
- Entitlements
- Communication
- Analytics
- Reports
- Audit
- Settings

Existing repository capabilities must always be reused before introducing new systems.

---

10. ROLE ARCHITECTURE

Actual roles must first be inspected from the repository.

Potential role families include:

- ORGANIZATION_ADMIN
- ACADEMIC_ADMIN
- USER_ADMIN
- CONTENT_ADMIN
- LIVE_EVENT_ADMIN
- FINANCE_ADMIN
- REPORTING_ADMIN
- DEPARTMENT_ADMIN
- FACULTY_ADMIN
- PROGRAM_ADMIN

These are reference role families.

They are NOT instructions to create duplicate roles.

If equivalent roles already exist, reuse them.

---

11. ROLE IS NOT ORGANIZATION TYPE

The system must not rely on simplistic authorization such as:

UNIVERSITY_ADMIN = EVERYTHING

Instead:

USER
+
ORGANIZATION MEMBERSHIP
+
ROLE
+
PERMISSION
+
SCOPE
+
ENABLED SERVICES
+
RESOURCE OWNERSHIP

This provides a scalable authorization model.

---

12. FUNDAMENTAL ACCESS MODEL

The fundamental access rule is:

ACCESS
=
AUTHENTICATED USER
+
ROLE
+
PERMISSION
+
ORGANIZATION SCOPE
+
RESOURCE OWNERSHIP
+
ENABLED SERVICE

The frontend is not the security boundary.

The backend must enforce authorization.

---

13. ORGANIZATION SCOPE

Scope may exist at different organizational levels.

Examples:

Organization Admin
        ↓
Organization Scope

University Admin
        ↓
University Scope

Faculty Admin
        ↓
Faculty Scope

Department Admin
        ↓
Department Scope

Program Admin
        ↓
Program Scope

School Admin
        ↓
School Scope

A scoped administrator must not access resources outside the assigned scope.

---

14. ORGANIZATION ISOLATION

Organization A must not access Organization B.

ORGANIZATION A ADMIN
        ↓
ORGANIZATION A
        ↓
OWN RESOURCES
        ↓
ALLOW

But:

ORGANIZATION A ADMIN
        ↓
ORGANIZATION B
        ↓
ORGANIZATION B RESOURCES
        ↓
DENY

Isolation must apply to:

- Users
- Learners
- Teachers
- Lecturers
- Instructors
- Trainers
- Staff
- Courses
- Programs
- Lessons
- Activities
- Assessments
- Events
- Live Sessions
- Recordings
- Media
- Resources
- Certificates
- Payments
- Entitlements
- Reports
- Analytics
- Files
- Notifications
- Audit data

---

15. IDOR / BOLA PROTECTION

The backend must prevent unauthorized access through manipulated IDs.

Important identifiers include:

organizationId
institutionId
providerId
schoolId
departmentId
facultyId
programId
courseId
eventId
liveClassId
liveSessionId
userId
learnerId
resourceId
certificateId
paymentId

Expected:

AUTHORIZED RESOURCE
        ↓
ALLOW

UNAUTHORIZED RESOURCE
        ↓
DENY

Changing an identifier must never expand authorization.

---

16. ORGANIZATION ONBOARDING

Reference lifecycle:

Organization Application
        ↓
Platform Review
        ↓
Approval
        ↓
Organization Activation
        ↓
Service Configuration
        ↓
Admin Account Activation
        ↓
Secure Credential Setup
        ↓
Organization Admin Login
        ↓
Organization Administration

Organization Admin cannot bypass Platform governance.

---

17. ADMIN CREDENTIAL MANAGEMENT

Platform Admin may create, invite, or activate organization administrators through the existing account system.

Preferred flow:

PLATFORM ADMIN
        ↓
CREATE / INVITE / ACTIVATE ADMIN
        ↓
SECURE CREDENTIAL SETUP
        ↓
AUTHENTICATION
        ↓
SECURITY CONTROLS
        ↓
ORGANIZATION SCOPE RESOLUTION
        ↓
ORGANIZATION WORKSPACE

Never share Platform Admin credentials.

Never expose platform infrastructure secrets to organization administrators.

Where supported, use:

- Secure invitations
- Password setup
- MFA
- Account activation
- Account expiration
- Revocation
- Audit logging

---

18. MULTI-ORGANIZATION MEMBERSHIP

First inspect whether the repository supports users belonging to multiple organizations.

If supported:

USER
│
├── ORGANIZATION A
│      ├── ROLE
│      ├── PERMISSIONS
│      └── SCOPE
│
├── ORGANIZATION B
│      ├── ROLE
│      ├── PERMISSIONS
│      └── SCOPE
│
└── ORGANIZATION C
       ├── ROLE
       ├── PERMISSIONS
       └── SCOPE

Organization switching must be secure.

The active organization context must be explicit.

Data from one organization must never leak into another.

If multi-organization membership does not currently exist, do not redesign the architecture unnecessarily.

---

19. PREMIUM DASHBOARD

The Institution / Provider Admin dashboard must be a professional organizational command center.

It should be:

- Premium
- Modern
- Attractive
- Card-based
- Action-oriented
- Easy to scan
- Responsive
- Accessible
- Clean
- Professional
- Trustworthy

It must NOT look like:

- Generic CRUD admin
- Boring table-only software
- Student dashboard
- Marketing landing page
- Fake analytics dashboard
- Excessive glassmorphism
- Excessive gradients
- Excessive animation
- Unnecessary visual clutter

---

20. DASHBOARD PURPOSE

The dashboard should immediately answer:

WHAT IS HAPPENING?

WHAT CHANGED?

WHAT NEEDS ATTENTION?

WHAT SHOULD I DO NEXT?

The dashboard is not merely a collection of statistics.

---

21. DASHBOARD CARD ARCHITECTURE

Potential card categories include:

KPI CARDS

Examples:

- Active Learners
- Educators
- Courses
- Programs
- Active Enrollments
- Live Sessions
- Upcoming Events
- Certificates

OPERATIONAL CARDS

Examples:

- Live Now
- Upcoming Classes
- Upcoming Events
- Recent Courses
- Pending Reviews
- Recent Activity

ATTENTION CARDS

Examples:

- Pending approvals
- Unpublished content
- Attendance issues
- Payment issues
- Certificate issues
- Access issues
- Data-quality issues

QUICK ACTION CARDS

Examples:

- Create Course
- Schedule Live Class
- Create Event
- Invite Staff
- Add Learners
- Upload Resource
- Publish Content
- Review Certificates

Only display actions authorized for the current user.

---

22. DASHBOARD REAL-DATA RULE

Production dashboards must use actual backend data.

Never fabricate:

- Learners
- Users
- Courses
- Revenue
- Attendance
- Activity
- Live sessions
- Events
- Certificates
- Reports
- Charts
- Trends
- Testimonials
- Statistics

If data is unavailable:

Data unavailable

or a meaningful empty state must be displayed.

---

23. DASHBOARD VISUAL HIERARCHY

Recommended hierarchy:

HEADER
    ↓
CONTEXT / ORGANIZATION
    ↓
KEY METRICS
    ↓
ATTENTION REQUIRED
    ↓
LIVE / UPCOMING
    ↓
OPERATIONAL ACTIVITY
    ↓
ANALYTICS / INSIGHTS
    ↓
QUICK ACTIONS

The exact layout must adapt to the organization and role.

---

24. DASHBOARD INTERACTION STATES

Cards and controls should support:

- Default
- Hover
- Focus
- Pressed
- Selected
- Loading
- Success
- Warning
- Error
- Disabled

Motion should be subtle and purposeful.

Avoid unnecessary animation.

---

25. SCHOOL ADMIN CONTEXT

A school-oriented workspace may prioritize:

- Learners
- Teachers
- Classes
- Streams
- Subjects
- Attendance
- Assessments
- Courses
- Live Classes
- Resources
- Parents where supported
- Reports

Do not force university-specific concepts into the school experience.

---

26. COLLEGE / TVET ADMIN CONTEXT

A College / TVET workspace may prioritize:

- Learners
- Lecturers
- Instructors
- Trainers
- Departments
- Programs
- Courses
- Modules
- Attendance
- Assessments
- Skills
- Competencies
- Practical learning
- Workshops
- Laboratories
- Fieldwork
- Live learning
- Resources
- Certificates
- Reports

TVET learning emphasis:

SKILLS
    ↓
PRACTICAL
    ↓
COMPETENCY
    ↓
ASSESSMENT
    ↓
EVIDENCE

---

27. UNIVERSITY ADMIN CONTEXT

A University workspace may prioritize:

- Students
- Lecturers
- Faculties
- Schools
- Departments
- Programs
- Courses
- Modules
- Academic periods
- Enrollment
- Assessments
- Academic progress
- Research
- Projects
- Live lectures
- Resources
- Certificates
- Reports

Faculty and department administrators must remain scope-limited.

---

28. PROFESSIONAL / SKILLS PROVIDER CONTEXT

A professional provider may manage:

- Programs
- Courses
- Trainers
- Participants
- Events
- Workshops
- Live sessions
- Resources
- Assessments
- Certificates
- Payments
- Analytics

Do not force:

- GPA
- University semesters
- Faculties
- University transcripts

unless genuinely required.

---

29. COMPANY LEARNING CONTEXT

A company may manage:

- Employees
- Departments
- Training programs
- Courses
- Learning paths
- Skills
- Workshops
- Live training
- Attendance
- Assessments
- Certificates
- Reports

Employee information must remain properly scoped and protected.

---

30. BANK / NGO / GOVERNMENT ORGANIZATION

Where supported, such organizations may provide:

- Public education
- Financial education
- Professional training
- Workshops
- Seminars
- Live sessions
- Courses
- Resources
- Certificates
- Sponsored access
- Events

A government organization does not automatically become an Education Authority.

---

31. CONTENT PROVIDER CONTEXT

Content providers may manage:

- Courses
- Lessons
- Media
- Resources
- Contributors
- Publishing
- Content review
- Live/media
- Analytics

Do not force formal academic structures into content-provider workflows.

---

32. EVENT PROVIDER CONTEXT

Event providers may manage:

- Events
- Seminars
- Workshops
- Webinars
- Speakers
- Participants
- Registration
- Live sessions
- Attendance
- Recordings
- Resources
- Certificates

Reference lifecycle:

CREATE EVENT
        ↓
DRAFT
        ↓
REVIEW
        ↓
PUBLISH
        ↓
REGISTRATION
        ↓
PARTICIPATION
        ↓
ATTENDANCE
        ↓
LIVE / RECORDING
        ↓
CERTIFICATE WHERE ELIGIBLE
        ↓
REPORT

---

33. ENABLED SERVICES

Organization capabilities may be configurable.

Example:

ORGANIZATION
│
├── COURSES             ✓
├── LIVE LEARNING       ✓
├── EVENTS              ✓
├── MEDIA               ✓
├── RESOURCES           ✓
├── CERTIFICATES        ✓
├── PAYMENTS            ✓
└── SPONSORED ACCESS    ✗

Disabled services must not appear as active capabilities.

Backend must enforce service availability.

Frontend hiding alone is insufficient.

---

34. PEOPLE MANAGEMENT

Where supported:

ORGANIZATION
│
├── LEARNERS
├── TEACHERS
├── LECTURERS
├── INSTRUCTORS
├── TRAINERS
├── STAFF
└── DELEGATED ADMINISTRATORS

Authorized capabilities may include:

- View
- Add
- Invite
- Activate
- Deactivate
- Assign role
- Assign department
- Assign program
- Manage membership
- View permitted activity

Every action must enforce organization scope.

---

35. ORGANIZATION STRUCTURE

Depending on organization type, the system may support:

Organization
│
├── Campuses
├── Faculties
├── Schools
├── Departments
├── Programs
├── Classes
├── Streams
└── Teams / Units

Only implement structures that are supported and necessary.

Avoid unnecessary database complexity.

---

36. LEARNING MANAGEMENT

Where applicable:

PROGRAM
    ↓
COURSE
    ↓
MODULE
    ↓
LESSON
    ↓
ACTIVITY
    ↓
ASSESSMENT
    ↓
PROGRESS
    ↓
EVIDENCE

Reuse existing ELMKUSOMA learning architecture.

Do not create a second course engine.

---

37. CONTENT MANAGEMENT

Content may include:

- Lessons
- Notes
- Documents
- PDFs
- Images
- Videos
- Audio
- Interactive activities
- Quizzes
- Assessments
- Learning resources

Where supported, content should have a lifecycle:

DRAFT
    ↓
REVIEW
    ↓
APPROVED
    ↓
PUBLISHED
    ↓
UPDATED
    ↓
ARCHIVED

Publishing permissions must be explicit.

---

38. LIVE LEARNING

Live Learning is a first-class ELMKUSOMA capability.

Reuse existing:

- LiveSession architecture
- LiveKit
- WebRTC
- WebSocket / STOMP
- Attendance
- Recordings
- Replay
- Notifications
- Authorization

Do not create a separate live infrastructure for each organization type.

---

39. LIVE ADMINISTRATION

Where authorized, Organization Admin may:

- View live sessions
- Create sessions
- Schedule sessions
- Configure sessions
- Manage eligible participants
- Monitor attendance
- Review recording status
- Access replay metadata
- Review live activity
- Connect live sessions to courses/events

Organization Admin does not automatically receive classroom-control privileges.

Permissions must determine actual capabilities.

---

40. LIVE SESSION LIFECYCLE

Reference lifecycle:

SCHEDULED
    ↓
PREPARE
    ↓
PREFLIGHT
    ↓
STARTING
    ↓
LIVE
    ↓
ENDED
    ↓
PROCESSING
    ↓
REPLAY

Scheduling must not automatically start the LiveKit session.

Short-lived access tokens should be used where applicable.

---

41. EVENTS

Where enabled, organization administrators may manage:

- Create
- Draft
- Review
- Publish
- Schedule
- Registration
- Participants
- Attendance
- Live integration
- Recordings
- Resources
- Certificates
- Reports

Reuse shared event infrastructure.

---

42. MEDIA

Where supported:

- Video
- Audio
- Recordings
- Learning media
- Replay

Reuse shared media architecture.

Avoid creating duplicate storage systems.

---

43. RESOURCE LIBRARY

Resources may include:

- Documents
- Notes
- PDFs
- Images
- Audio
- Video
- Course resources
- Event resources
- Learning materials

Resources must respect:

- Organization ownership
- Permissions
- Publication state
- Scope
- File security

---

44. CERTIFICATES

Certificates must be evidence-based.

Reference lifecycle:

COMPLETION
    ↓
ELIGIBILITY
    ↓
VALIDATION
    ↓
CERTIFICATE ISSUED
    ↓
VERIFICATION

Do not permit arbitrary certificate generation.

Reuse the existing certificate engine.

---

45. PAYMENTS AND ENTITLEMENTS

Where enabled:

SERVICE
    ↓
PACKAGE / PRICE
    ↓
PAYMENT
    ↓
VERIFICATION
    ↓
ENTITLEMENT
    ↓
ACCESS

Potential use cases:

- Course payments
- Event registration
- Live access
- Subscriptions
- Training packages
- Sponsored access

Reuse centralized ELMKUSOMA payment and entitlement architecture.

---

46. SPONSORED ACCESS

Where supported:

SPONSOR
    ↓
PROGRAM / PACKAGE
    ↓
ELIGIBLE LEARNERS
    ↓
ENTITLEMENT
    ↓
ACCESS

Sponsorship decisions must be traceable and auditable.

---

47. COMMUNICATION

Use shared communication infrastructure for:

- Announcements
- Notifications
- Event reminders
- Course announcements
- Staff communication
- Learner communication
- System alerts

Do not create parallel notification systems.

---

48. ANALYTICS

Organization analytics should answer:

- Who is participating?
- What is being used?
- What is progressing?
- What is incomplete?
- What needs attention?
- Which services are active?
- What learning activity is occurring?
- What event activity is occurring?
- What live activity is occurring?

Use real data.

Do not invent trends.

---

49. REPORTING

Where supported, reports may include:

- Learner activity
- Course participation
- Completion
- Attendance
- Assessment activity
- Live participation
- Event participation
- Certificates
- Payments
- Service usage

All reports must respect organization scope.

---

50. ATTENTION ENGINE

The dashboard should highlight evidence-based issues.

Example:

ATTENTION REQUIRED
────────────────────────────────
7 pending content reviews
3 upcoming live sessions require preparation
2 certificate issues
1 payment verification pending

Every attention item should contain, where applicable:

- Issue
- Resource
- Date / period
- Status
- Action

Never create fake alerts.

---

51. ACTIVITY FEED

Where supported, activity may include:

- New learner
- New staff
- Course published
- Event created
- Live session scheduled
- Certificate issued
- Payment received
- Configuration changed

Only show actual system activity.

---

52. ROLE DELEGATION

Organization Admin may delegate selected responsibilities where supported.

Example:

ORGANIZATION ADMIN
│
├── ACADEMIC ADMIN
├── FINANCE ADMIN
├── CONTENT ADMIN
├── LIVE / EVENT ADMIN
└── REPORTING ADMIN

Delegated accounts require:

- Role
- Permission
- Scope
- Status
- Audit trail

Never share the primary administrator password.

---

53. SEPARATION OF DUTIES

Do not automatically grant every administrator every permission.

Sensitive functions may include:

- Payments
- Refunds
- Certificate issuance
- Role assignment
- Account deactivation
- Content publishing
- Data export
- Permission changes

Use least privilege.

Use separation of duties where supported.

---

54. PLATFORM ADMIN BOUNDARY

Organization Admin must not control:

- Global platform configuration
- All organizations
- Platform-wide roles
- Platform infrastructure secrets
- Global payment configuration
- Global LiveKit secrets
- Platform-wide security settings
- Platform-wide authority assignments
- Other organizations

These belong to Platform Admin or appropriate platform governance roles.

---

55. TEACHER / LECTURER BOUNDARY

Do not duplicate teaching workspaces.

Organization Admin:

Organization Structure
+
Membership
+
Services
+
Administration

Teacher / Lecturer / Instructor:

Teaching
+
Learning Delivery
+
Assessment
+
Learner Support

Reuse existing teaching architecture.

---

56. LEARNER BOUNDARY

Do not redesign learner workspaces as part of Institution / Provider Admin.

Organization Admin may manage authorized learner records and organizational membership.

Learners remain responsible for:

- Learning
- Practice
- Assessments
- Progress
- Live participation
- Resources
- Learning evidence

---

57. PARENT BOUNDARY

Do not redesign the Parent workspace.

Only expose organization-level parent operations where already supported and authorized.

---

58. RECOMMENDED NAVIGATION

A shared adaptive navigation may include:

Dashboard
Organization
People
Learning
Courses / Programs
Live
Events
Media
Resources
Certificates
Payments
Reports
Analytics
Notifications
Settings

Modules should appear only when:

Organization Type
+
Enabled Service
+
Role
+
Permission

allow them.

---

59. SCHOOL NAVIGATION EXAMPLE

Dashboard
Organization
Students
Teachers
Classes
Subjects
Courses
Attendance
Assessments
Live
Events
Resources
Certificates
Reports
Settings

---

60. UNIVERSITY NAVIGATION EXAMPLE

Dashboard
University
Students
Lecturers
Faculties
Departments
Programs
Courses
Assessments
Research
Live
Events
Resources
Certificates
Reports
Settings

---

61. TVET NAVIGATION EXAMPLE

Dashboard
Institution
Learners
Trainers
Programs
Modules
Skills
Competencies
Practical
Assessment
Live
Resources
Certificates
Reports
Settings

---

62. PROFESSIONAL PROVIDER NAVIGATION EXAMPLE

Dashboard
Organization
Participants
Programs
Courses
Training
Live
Events
Resources
Certificates
Payments
Analytics
Settings

---

63. EVENT PROVIDER NAVIGATION EXAMPLE

Dashboard
Organization
Events
Seminars
Workshops
Webinars
Speakers
Participants
Live
Attendance
Recordings
Certificates
Reports
Settings

These are contextual examples.

They must not result in duplicate applications.

---

64. FRONTEND ROUTING

Inspect existing frontend routing before changing it.

Use the repository's established routing architecture.

Organization Admin routes must resolve through:

Authenticated Identity
        ↓
Role
        ↓
Organization
        ↓
Permissions
        ↓
Scope
        ↓
Enabled Services
        ↓
Authorized Resource

Route hiding is not a security mechanism.

---

65. BACKEND API ARCHITECTURE

Inspect existing APIs before creating new endpoints.

Reuse existing APIs whenever possible.

New APIs must:

- Authenticate
- Authorize
- Validate input
- Enforce organization scope
- Enforce ownership
- Respect enabled services
- Support pagination where necessary
- Return consistent errors
- Avoid unauthorized data leakage
- Preserve backward compatibility

---

66. DATABASE ARCHITECTURE

Inspect the current database before creating migrations.

Potential reusable entities include:

- Organization
- Institution
- Provider
- User
- Role
- Permission
- Membership
- Enrollment
- Course
- Program
- Module
- Lesson
- LiveSession
- Event
- Media
- Resource
- Certificate
- Payment
- Entitlement
- Notification
- Audit

Only introduce new entities when a genuine domain gap exists.

---

67. DATA OWNERSHIP

Every organization-owned resource must have a defensible relationship to its organization.

Examples:

Organization
    ↓
Course

Organization
    ↓
Event

Organization
    ↓
Live Session

Organization
    ↓
Resource

Organization
    ↓
Certificate

Backend authorization must verify actual ownership relationships.

---

68. API SECURITY

Never trust frontend-provided values for authorization such as:

- organizationId
- institutionId
- providerId
- schoolId
- role
- permission
- scope

Authorization must be resolved server-side.

---

69. AUDIT LOGGING

Important organization operations should be auditable.

Examples:

- Login
- Role assignment
- Staff invitation
- Learner activation
- Learner deactivation
- Content publication
- Course modification
- Event modification
- Live session modification
- Certificate actions
- Payment actions
- Data export
- Permission changes
- Organization settings changes

Audit logs should support:

- Who
- What
- When
- Organization
- Resource
- Action
- Result
- Relevant metadata

---

70. FILE SECURITY

Organization-owned files must respect:

- Organization ownership
- User authorization
- Resource permissions
- Publication state
- Scope
- Secure access
- Secure downloads

Use signed URLs where appropriate.

Never expose private files through predictable public paths.

---

71. PERFORMANCE

Audit the system for:

- Duplicate API calls
- N+1 queries
- Oversized payloads
- Duplicate data fetching
- Excessive rendering
- Large bundles
- Expensive dashboard aggregation
- Unnecessary polling

Use where appropriate:

- Pagination
- Caching
- Server-side aggregation
- Lazy loading
- Skeleton loading
- Incremental loading

---

72. RESPONSIVE DESIGN

Support:

- Desktop
- Laptop
- Tablet
- Mobile

Mobile must be intentionally designed.

Do not simply scale desktop down.

Dashboard cards should reorganize intelligently.

Tables must have appropriate mobile behavior.

Navigation must remain usable.

---

73. ACCESSIBILITY

Audit:

- Keyboard navigation
- Visible focus
- Semantic HTML
- Labels
- Contrast
- Screen reader compatibility
- Form validation
- Error messaging
- Status communication

Do not rely on color alone.

---

74. UX STATES

Every major feature should support meaningful:

- Loading
- Skeleton
- Empty
- Success
- Warning
- Error
- Permission denied
- Unavailable
- Retry

states.

---

75. SEARCH

Where shared search infrastructure exists, organization administrators may search authorized:

- Learners
- Staff
- Courses
- Programs
- Events
- Live sessions
- Resources
- Certificates

Search must be organization-scoped.

---

76. NOTIFICATIONS

Reuse the shared notification system.

Potential notifications include:

- System notices
- Staff actions
- Learner activity
- Event reminders
- Payment notifications
- Content workflow
- Security alerts
- Live session reminders

Only expose information appropriate to the role and scope.

---

77. NO DUPLICATE SYSTEMS

Do not create:

- Duplicate user systems
- Duplicate organization systems
- Duplicate role systems
- Duplicate permission systems
- Duplicate course systems
- Duplicate learning systems
- Duplicate live systems
- Duplicate event systems
- Duplicate media systems
- Duplicate payment systems
- Duplicate entitlement systems
- Duplicate certificate systems
- Duplicate notification systems
- Duplicate analytics systems

Reuse shared ELMKUSOMA infrastructure.

---

78. INTEGRATION MAP

                         ELMKUSOMA
                              │
          ┌───────────────────┼───────────────────┐
          ↓                   ↓                   ↓
   PLATFORM ADMIN     EDUCATION AUTHORITY   SHARED SERVICES
          │             Oversight Layer
          ↓
 ORGANIZATION GOVERNANCE
          ↓
 INSTITUTION / PROVIDER ADMIN
          │
 ┌────────┼──────────┬──────────┬──────────┐
 ↓        ↓          ↓          ↓          ↓
People  Learning    Live      Events    Commerce
          │
          ↓
Teacher / Lecturer / Instructor
          │
          ↓
       Learner
          │
          ↓
Parent where authorized

Shared services:

Authentication
Authorization
Organizations
Learning
Live
Media
Events
Payments
Entitlements
Certificates
Notifications
Search
Analytics
Audit

---

79. END-TO-END ORGANIZATION JOURNEY

PLATFORM ADMIN
        ↓
APPROVES ORGANIZATION
        ↓
ACTIVATES ORGANIZATION
        ↓
CONFIGURES SERVICES
        ↓
ACTIVATES ADMIN
        ↓
ADMIN LOGS IN
        ↓
ORGANIZATION CONTEXT RESOLVED
        ↓
ADMIN DASHBOARD
        ↓
CONFIGURES ORGANIZATION
        ↓
ADDS / INVITES PEOPLE
        ↓
CREATES COURSES / EVENTS / LIVE / RESOURCES
        ↓
PUBLISHES
        ↓
LEARNERS DISCOVER
        ↓
ENROLL / REGISTER / PAY / SPONSORED ACCESS
        ↓
LEARN / PARTICIPATE
        ↓
PROGRESS / ATTENDANCE / EVIDENCE
        ↓
CERTIFICATE WHERE ELIGIBLE
        ↓
ORGANIZATION REPORTING
        ↓
IMPROVEMENT

---

80. SCHOOL END-TO-END

School approved
        ↓
School Admin activated
        ↓
School Admin Login
        ↓
School Dashboard
        ↓
School Structure
        ↓
Teachers / Learners
        ↓
Classes / Subjects
        ↓
Courses / Attendance / Assessments
        ↓
Live Learning
        ↓
Reports

Security requirement:

School A Admin
        ↓
School A
        ✓

School A Admin
        ↓
School B
        ✗

---

81. UNIVERSITY END-TO-END

University approved
        ↓
University Admin activated
        ↓
University Workspace
        ↓
Faculties / Departments
        ↓
Programs
        ↓
Courses
        ↓
Lecturers / Students
        ↓
Assessments
        ↓
Research
        ↓
Live Lectures
        ↓
Reports

Faculty and department administrators must remain within their assigned scope.

---

82. TVET END-TO-END

TVET approved
        ↓
TVET Admin
        ↓
Programs
        ↓
Modules
        ↓
Skills
        ↓
Competencies
        ↓
Practical Learning
        ↓
Assessment
        ↓
Evidence
        ↓
Live / Workshops
        ↓
Certificates

---

83. PROFESSIONAL PROVIDER END-TO-END

Provider approved
        ↓
Provider Admin
        ↓
Provider Dashboard
        ↓
Create Program / Course
        ↓
Create Event / Live
        ↓
Learner Registration
        ↓
Payment / Entitlement where enabled
        ↓
Participation
        ↓
Attendance
        ↓
Progress
        ↓
Certificate where eligible
        ↓
Analytics

---

84. EVENT PROVIDER END-TO-END

Create Event
        ↓
Draft
        ↓
Publish
        ↓
Registration
        ↓
Participants
        ↓
Schedule Live
        ↓
Live Participation
        ↓
Attendance
        ↓
Recording
        ↓
Replay
        ↓
Certificate where eligible
        ↓
Report

---

85. AUTHORIZATION TEST MATRIX

Scenario| Expected
Organization Admin → Own Organization| ALLOW
Organization Admin → Other Organization| DENY
School Admin → Own School| ALLOW
School Admin → Other School| DENY
Department Admin → Own Department| ALLOW
Department Admin → Other Department| DENY
Finance Admin → Payment Area| ALLOW if permitted
Finance Admin → Academic Area| DENY unless permitted
Content Admin → Content| ALLOW
Content Admin → Platform Security| DENY
Event Admin → Events| ALLOW
Event Admin → Platform Configuration| DENY
Organization Admin → Platform Admin| DENY
Organization Admin → Education Authority| DENY

Actual tests must use the real roles from the repository.

---

86. SECURITY TESTING

Test for:

- IDOR
- BOLA
- Privilege escalation
- Role manipulation
- Organization ID manipulation
- School ID manipulation
- Department ID manipulation
- Faculty ID manipulation
- Course ID manipulation
- Event ID manipulation
- Live Session ID manipulation
- User ID manipulation
- Direct URL access
- Unauthorized API access
- Hidden-route access
- Unauthorized export
- Unauthorized file access
- Unauthorized certificate access
- Unauthorized live session access
- Unauthorized payment access
- Unauthorized learner data access

Every vulnerability must be:

FIXED
OR
EXPLICITLY REPORTED AS A BLOCKER

---

87. BACKWARD COMPATIBILITY

Existing working systems must remain functional.

Especially:

- Nursery
- Primary
- Secondary
- College
- TVET
- University
- Other Learner
- Teacher
- Lecturer
- Instructor
- Parent
- Live
- Authentication
- Payments
- Certificates
- Notifications

Do not restructure unrelated systems without evidence.

---

88. IMPLEMENTATION PHASES

PHASE 1 — REPOSITORY AUDIT

Inspect:

- Repository structure
- Git status
- Branch
- Existing commits
- Authentication
- Roles
- Permissions
- Organizations
- Current admin implementation
- APIs
- Services
- Database
- Frontend
- Existing tests

PHASE 2 — ARCHITECTURE AUDIT

Identify:

- Existing organization model
- Existing membership model
- Existing authorization model
- Existing scope model
- Existing service capability model
- Existing admin routes
- Existing dashboard components
- Existing shared services

PHASE 3 — SECURITY

Verify:

- Authentication
- Role authorization
- Permission authorization
- Organization isolation
- Scope enforcement
- Resource ownership
- IDOR/BOLA protection

PHASE 4 — DASHBOARD

Implement or improve:

- Premium card layout
- Real KPIs
- Attention cards
- Quick actions
- Operational cards
- Activity
- Empty states
- Loading states
- Error states
- Responsive behavior

PHASE 5 — ORGANIZATION MANAGEMENT

Implement or complete:

- Organization profile
- People
- Membership
- Roles
- Permissions
- Organization settings

PHASE 6 — LEARNING

Implement or complete:

- Programs
- Courses
- Modules
- Lessons
- Content
- Resources
- Assessments where applicable

PHASE 7 — LIVE / EVENTS / MEDIA

Implement or complete:

- Live
- Scheduling
- Attendance
- Events
- Recordings
- Media
- Replay

PHASE 8 — COMMERCE

Implement or complete where supported:

- Payments
- Entitlements
- Packages
- Sponsored access

PHASE 9 — CERTIFICATES

Implement or complete:

- Eligibility
- Validation
- Issuance
- Verification

PHASE 10 — REPORTING

Implement or complete:

- Analytics
- Reports
- Activity
- Attention engine

PHASE 11 — END-TO-END

Verify real organization journeys.

PHASE 12 — FINAL AUDIT

Perform a requirement-by-requirement completion audit.

---

89. INSPECT → UNDERSTAND → AUDIT → IMPLEMENT

Never begin implementation blindly.

Required workflow:

INSPECT
    ↓
UNDERSTAND
    ↓
AUDIT
    ↓
CLASSIFY
    ↓
DESIGN MINIMUM NECESSARY CHANGE
    ↓
IMPLEMENT
    ↓
INTEGRATE
    ↓
TEST
    ↓
FIX
    ↓
VERIFY

---

90. PRESERVE EXISTING WORKING SYSTEMS

If a capability already exists:

AUDIT
    ↓
IDENTIFY GAP
    ↓
FIX
    ↓
VERIFY

Do not delete and rebuild working systems without evidence.

---

91. GIT / TEAM SAFETY

Before modifying shared files:

- Inspect git status
- Inspect current branch
- Inspect recent commits
- Identify uncommitted work
- Identify parallel developer work
- Identify modified shared files
- Avoid overwriting unrelated work
- Minimize shared-file changes
- Document conflict risks

Never reset, discard, or overwrite another developer's work without explicit authorization.

---

92. TESTING REQUIREMENTS

Backend

Run applicable:

- Compilation
- Unit tests
- Integration tests
- Security tests
- Repository tests

Frontend

Run applicable:

- Lint
- Type checking
- Build
- Route tests
- Component tests

Database

Verify:

- Migrations
- Schema
- Constraints
- Data integrity

Integration

Verify:

- Authentication
- Organization resolution
- Authorization
- API → UI
- Live
- Payments
- Certificates
- Notifications

---

93. UI-ONLY COMPLETION IS INVALID

A feature is NOT complete merely because:

- A menu exists
- A card exists
- A button exists
- A page renders
- Mock data appears
- A route exists

A real feature requires:

UI
 ↓
API
 ↓
AUTHENTICATION
 ↓
AUTHORIZATION
 ↓
BUSINESS LOGIC
 ↓
DATABASE
 ↓
RESPONSE
 ↓
UI

to function correctly.

---

94. DATA INTEGRITY

Critical operations must preserve:

- Referential integrity
- Organization ownership
- Scope
- Auditability
- Status transitions
- Payment state
- Entitlement state
- Certificate eligibility
- Attendance integrity
- Learning progress integrity

Do not introduce shortcuts that bypass domain rules.

---

95. ERROR HANDLING

Errors must be:

- Consistent
- Safe
- Useful
- Non-sensitive
- Actionable where appropriate

Do not expose:

- Stack traces
- Database internals
- Secrets
- Tokens
- Internal infrastructure details

to end users.

---

96. OBSERVABILITY

Where supported, important organization operations should produce useful operational telemetry.

Potential signals include:

- Authentication events
- Authorization failures
- API failures
- Live session failures
- Payment failures
- Certificate failures
- Notification failures
- File access failures

Operational monitoring must not expose sensitive information unnecessarily.

---

97. DATA PRIVACY

Organization administrators should only see data necessary for their role.

Apply:

LEAST PRIVILEGE

to:

- Learner data
- Staff data
- Academic records
- Payments
- Certificates
- Communication
- Files
- Reports

Sensitive data should not be exposed merely because an administrator can access the dashboard.

---

98. EXPORTS

Where exports are supported:

- Require appropriate permission
- Enforce organization scope
- Log the export
- Avoid exposing unauthorized records
- Use safe file generation
- Protect generated files

---

99. ORGANIZATION OFFBOARDING

Where supported, organization lifecycle should account for:

ACTIVE
   ↓
SUSPENDED
   ↓
DEACTIVATED
   ↓
ARCHIVED

Actual lifecycle must follow repository/domain rules.

Offboarding must not silently destroy data.

Data retention and deletion must follow the platform's established policies.

---

100. ORGANIZATION SETTINGS

Potential settings include:

- Organization profile
- Branding
- Services
- Notifications
- Academic configuration
- Learning configuration
- Live settings
- Event settings
- Certificate settings
- Payment settings
- Membership settings

Only expose settings that belong to organization scope.

---

101. DASHBOARD QUALITY GATE

Before completion, verify:

- Dashboard is attractive
- Dashboard is professional
- Dashboard is premium
- Cards are meaningful
- Visual hierarchy is obvious
- Actions are discoverable
- Cards are not excessive
- Information density is controlled
- Mobile layout works
- Accessibility works
- Loading states work
- Empty states work
- Error states work
- Real data is used
- No fake metrics exist
- No unnecessary animation exists
- No excessive gradients exist
- No excessive glassmorphism exists
- No dashboard clutter exists

---

102. PRODUCT QUALITY GATE

Verify:

Product

Does the system genuinely support organization administration?

Architecture

Is there one shared administration foundation?

Adaptation

Does the workspace adapt by organization type?

Services

Do enabled services determine available capabilities?

Authorization

Do role and permissions control access?

Scope

Is organization and resource scope enforced server-side?

Security

Is cross-organization access prevented?

Integration

Does the system integrate with existing ELMKUSOMA services?

Data

Are dashboard metrics and reports based on real data?

Production

Do build, test, database and integration checks pass?

---

103. DEFINITION OF DONE

A requirement can be classified as:

VERIFIED COMPLETE

Actual repository, code, database, API and/or test evidence proves the requirement works.

PARTIAL

Meaningful implementation exists but a critical part remains incomplete.

MISSING

The requirement is not implemented.

BROKEN

The requirement exists but does not work correctly.

NOT APPLICABLE

Repository/domain evidence demonstrates that the requirement does not apply.

---

104. EXACT COMPLETION AUDIT

After implementation, perform a requirement-by-requirement audit.

Use:

COMPLETION %
=
VERIFIED COMPLETE APPLICABLE REQUIREMENTS
÷
TOTAL APPLICABLE REQUIREMENTS
×
100

The percentage must be based on actual evidence.

Never:

- Estimate
- Guess
- Inflate
- Count visual presence as implementation
- Claim 100% without evidence

If completion is below 98%, report the actual percentage.

If it is safe and practical to continue fixing remaining gaps, do so before final reporting.

---

105. EVIDENCE LOG

For important requirements record evidence such as:

- File
- Class
- Service
- Controller
- Route
- API
- Database table
- Migration
- Test
- Authorization rule
- Integration behavior

Critical claims must be evidence-backed.

---

106. ADVERSARIAL PRE-MORTEM

Before declaring completion, test the system against major failure modes.

FAILURE 1 — CROSS-ORGANIZATION DATA LEAKAGE

Potential cause:

Incorrect organization filtering.

Mitigation:

- Server-side scope enforcement
- Ownership checks
- IDOR/BOLA tests
- Cross-organization security tests

FAILURE 2 — FAKE DASHBOARD DATA

Potential cause:

Hard-coded statistics or mock data.

Mitigation:

Every metric must map to a real backend source.

FAILURE 3 — ORGANIZATION ADMIN RECEIVES PLATFORM PRIVILEGES

Potential cause:

Overly broad roles.

Mitigation:

Separate platform and organization authorization.

FAILURE 4 — DUPLICATE SYSTEMS

Potential cause:

Creating separate course, live, payment or certificate engines.

Mitigation:

Reuse shared ELMKUSOMA services.

FAILURE 5 — EXISTING LEARNER / TEACHER SYSTEMS BREAK

Potential cause:

Uncontrolled restructuring.

Mitigation:

Regression and integration tests.

FAILURE 6 — DISABLED SERVICES APPEAR ACTIVE

Potential cause:

Frontend-only service configuration.

Mitigation:

Backend + frontend capability enforcement.

FAILURE 7 — DELEGATED ADMIN HAS TOO MUCH ACCESS

Potential cause:

Broad role inheritance.

Mitigation:

Least privilege + explicit permissions + scope.

FAILURE 8 — UI EXISTS WITHOUT BACKEND IMPLEMENTATION

Potential cause:

Treating visual completion as feature completion.

Mitigation:

Verify UI → API → Auth → Logic → DB → Response.

FAILURE 9 — SHARED FILE CONFLICTS

Potential cause:

Multiple developers modifying common architecture simultaneously.

Mitigation:

Git inspection + ownership boundaries + minimal shared-file changes.

---

107. TEAM DEVELOPMENT PRINCIPLE

When multiple developers work simultaneously:

SHARED FOUNDATION
        ↓
CLEAR OWNERSHIP
        ↓
MINIMAL SHARED-FILE MODIFICATION
        ↓
STABLE CONTRACTS
        ↓
INTEGRATION
        ↓
REGRESSION TESTING

No developer should silently replace another developer's domain implementation.

---

108. API CONTRACT PRINCIPLE

Shared API contracts must be:

- Explicit
- Stable
- Backward compatible
- Authenticated
- Authorized
- Organization-scoped
- Documented

Avoid breaking existing consumers.

---

109. FRONTEND CONTRACT PRINCIPLE

Shared UI components should be reusable.

Prefer:

Shared Design System
        ↓
Shared Organization Shell
        ↓
Context-Aware Modules

Avoid:

School Admin App
University Admin App
TVET Admin App
Provider Admin App
Company Admin App
Event Admin App

as completely independent applications.

---

110. DATABASE PRINCIPLE

Prefer:

SHARED DOMAIN MODEL
        +
ORGANIZATION CONTEXT
        +
TYPE / CAPABILITY CONFIGURATION
        +
ROLE / PERMISSION
        +
SCOPE

over multiple duplicated schemas.

Do not redesign the database unless required by a genuine domain gap.

---

111. LIVE INTEGRATION PRINCIPLE

The Live system remains shared.

Organization
      ↓
Course / Event / Program
      ↓
LiveSession
      ↓
LiveKit / WebRTC
      ↓
Attendance
      ↓
Recording
      ↓
Replay
      ↓
Learning Evidence

No organization type should create its own separate LiveKit architecture.

---

112. COMMERCE INTEGRATION PRINCIPLE

Payments and entitlements remain shared.

Organization
      ↓
Service
      ↓
Package / Price
      ↓
Payment
      ↓
Verification
      ↓
Entitlement
      ↓
Access

Organization-specific configuration may adapt the experience.

The underlying payment architecture should remain centralized where possible.

---

113. CERTIFICATE INTEGRATION PRINCIPLE

Certificates should use the shared certificate engine.

Learning / Event
      ↓
Participation / Completion
      ↓
Eligibility
      ↓
Validation
      ↓
Certificate
      ↓
Verification

Organization Admin should not bypass certificate eligibility rules.

---

114. LEARNING INTEGRATION PRINCIPLE

Organization administration connects to the shared learner ecosystem.

ORGANIZATION
      ↓
PROGRAM / COURSE / EVENT
      ↓
LEARNER
      ↓
LEARNING
      ↓
PROGRESS
      ↓
EVIDENCE
      ↓
CERTIFICATE

Do not create separate learner identities for each organization.

---

115. AUTHENTICATION PRINCIPLE

Identity should remain centralized.

USER IDENTITY
      ↓
AUTHENTICATION
      ↓
ROLE
      ↓
ORGANIZATION MEMBERSHIP
      ↓
PERMISSIONS
      ↓
SCOPE
      ↓
AUTHORIZED WORKSPACE

Do not create separate login systems for every organization type unless explicitly required by the architecture.

---

116. FINAL ARCHITECTURE

                           ELMKUSOMA
                               │
             ┌─────────────────┼──────────────────┐
             ↓                 ↓                  ↓
      PLATFORM ADMIN    EDUCATION AUTHORITY   SHARED SERVICES
      Control Plane      Oversight Plane
             │
             ↓
      ORGANIZATION GOVERNANCE
             │
             ↓
      ORGANIZATION / PROVIDER
             │
       ┌─────┴───────────────┐
       ↓                     ↓
Organization Type      Enabled Services
       │                     │
       └──────────┬──────────┘
                  ↓
              ADMIN ROLE
                  +
             PERMISSIONS
                  +
                SCOPE
                  ↓
      INSTITUTION / PROVIDER ADMIN
             CONTROL PLANE
                  │
        ┌─────────┼─────────┐
        ↓         ↓         ↓
      PEOPLE   LEARNING   OPERATIONS
                  │
        ┌─────────┼───────────────┐
        ↓         ↓               ↓
      COURSES    LIVE            EVENTS
        ↓         ↓               ↓
    CONTENT   ATTENDANCE        MEDIA
        ↓         ↓               ↓
   RESOURCES  RECORDINGS     CERTIFICATES
        │         │               │
        └─────────┼───────────────┘
                  ↓
           PAYMENTS / ACCESS
                  ↓
              LEARNERS
                  ↓
       PROGRESS / EVIDENCE
                  ↓
          ANALYTICS / REPORTS

---

117. MASTER PRODUCT JOURNEY

PLATFORM GOVERNANCE
        ↓
ORGANIZATION
        ↓
ORGANIZATION ADMIN
        ↓
PEOPLE
        ↓
LEARNING / CONTENT
        ↓
LIVE / EVENTS
        ↓
MEDIA / RESOURCES
        ↓
PAYMENTS / ENTITLEMENTS
        ↓
PARTICIPATION
        ↓
ATTENDANCE / PROGRESS / EVIDENCE
        ↓
CERTIFICATES
        ↓
ANALYTICS / REPORTING
        ↓
IMPROVEMENT

---

118. NON-NEGOTIABLES

1. Inspect before coding.
2. Repository is the source of truth.
3. Reuse before creating.
4. One shared Institution / Provider Administration foundation.
5. Organization type adapts the experience.
6. Enabled services adapt the experience.
7. Roles control responsibilities.
8. Permissions control capabilities.
9. Scope controls organizational boundaries.
10. Backend authorization is mandatory.
11. Cross-organization access is prohibited.
12. No fake production data.
13. No duplicate user systems.
14. No duplicate organization systems.
15. No duplicate learning systems.
16. No duplicate live systems.
17. No duplicate payment systems.
18. No duplicate certificate systems.
19. No duplicate notification systems.
20. Platform Admin remains separate.
21. Education Authority remains separate.
22. Teacher/Lecturer/Instructor remains separate.
23. Learner remains separate.
24. Parent remains separate.
25. Live Learning remains first-class.
26. UI presence does not equal implementation.
27. Critical requirements require evidence.
28. Existing working systems must be preserved.
29. Completion percentages must be evidence-based.
30. Security testing is mandatory.
31. Team/Git safety is mandatory.
32. Sensitive administrative actions must be auditable.
33. Least privilege is preferred.
34. Delegated administrators must have explicit permissions.
35. Organization administrators must never receive platform-wide authority accidentally.
36. Disabled services must not become accessible through API manipulation.
37. Organization scope must be enforced server-side.
38. Production dashboards must use real data.
39. Mobile and accessibility requirements must be treated as first-class.
40. The final system must be integrated, not a collection of disconnected modules.

---

119. FINAL PRINCIPLE

The ELMKUSOMA Institution / Provider Admin layer should ultimately become:

«THE SECURE OPERATIONAL CONTROL CENTER OF AN ORGANIZATION INSIDE ELMKUSOMA.»

It should enable each organization to manage its own:

PEOPLE
LEARNING
COURSES
PROGRAMS
CONTENT
LIVE
EVENTS
MEDIA
RESOURCES
CERTIFICATES
PAYMENTS
ACCESS
REPORTS
OPERATIONS

only where those capabilities are genuinely:

SUPPORTED
+
ENABLED
+
AUTHORIZED
+
SECURE
+
INTEGRATED

The architecture must remain:

SHARED
    ↓
ADAPTIVE
    ↓
SCOPED
    ↓
SECURE
    ↓
AUDITABLE
    ↓
INTEGRATED
    ↓
SCALABLE
    ↓
PRODUCTION-READY

---

120. REFERENCE USAGE

This document should be treated as the master reference for Institution / Provider Admin architecture.

Before implementation:

READ REFERENCE
      ↓
INSPECT REPOSITORY
      ↓
COMPARE CURRENT STATE
      ↓
IDENTIFY GAPS
      ↓
REUSE EXISTING ARCHITECTURE
      ↓
IMPLEMENT ONLY REQUIRED CHANGES
      ↓
TEST
      ↓
SECURITY AUDIT
      ↓
INTEGRATION TEST
      ↓
EXACT COMPLETION AUDIT

The reference does not authorize developers to invent unsupported entities, APIs, roles, database structures, metrics, integrations, or workflows.

The repository, actual business rules, existing architecture, and verified requirements remain the implementation source of truth.

---

END OF DOCUMENT

ELMKUSOMA — INSTITUTION / PROVIDER ADMIN

ONE SHARED FOUNDATION.
MULTIPLE CONTEXT-AWARE ORGANIZATIONS.
SECURE SCOPE.
REAL DATA.
NO DUPLICATION.
FULL INTEGRATION.
PRODUCTION READINESS.
