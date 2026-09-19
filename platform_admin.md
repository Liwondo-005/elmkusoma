ELMKUSOMA — PLATFORM ADMIN

Master Reference, Architecture, Governance & Implementation Roadmap

Document: "platform_admin.md"
Product: ELMKUSOMA
Component: Platform Administration & Governance
Status: Master Reference — Ready for Repository Audit & Phased Implementation
Authority: Platform Admin Product/Architecture Reference
Implementation Principle: Audit → Understand → Design → Implement → Integrate → Verify

---

1. DOCUMENT PURPOSE

This document defines the complete conceptual, functional, architectural, security, operational, commercial, governance, and implementation requirements for the ELMKUSOMA Platform Admin.

The Platform Admin is not merely an administrative dashboard.

It is the:

«ELMKUSOMA Platform Control, Governance, Operations & Intelligence Center.»

The Platform Admin provides platform-wide governance over the ELMKUSOMA ecosystem while maintaining strict separation between:

- Platform Administration
- Institution Administration
- Provider/Organization Administration
- Teaching roles
- Education Authority roles
- Learner roles

This document is intended to serve as:

1. Product reference
2. Architecture reference
3. Functional specification
4. Permission/governance reference
5. Developer ownership reference
6. Implementation roadmap
7. Security reference
8. Integration reference
9. Acceptance criteria reference
10. OpenCode implementation reference
11. Future maintenance reference
12. Requirement-level completion audit reference

---

2. CORE PRODUCT VISION

ELMKUSOMA is not limited to schools and traditional students.

The platform is designed as a broader learning ecosystem capable of supporting:

- Nursery learning
- Primary education
- Secondary education
- College
- TVET
- University
- Adult learning
- Lifelong learning
- Professional learning
- Skills development
- Independent learning
- Non-formal learning
- Religious education
- Seminars
- Workshops
- Webinars
- Professional events
- Educational events
- Live classes
- Live broadcasts
- Recorded learning
- Digital resources
- Content providers
- Organizations
- Companies
- Banks
- NGOs
- Government institutions
- Professional bodies
- Training organizations
- Individual educators
- Content creators
- Consultants
- Event organizers
- Skills providers
- Other legitimate learning/service providers

Therefore, Platform Admin must govern a multi-organization, multi-provider, multi-service learning ecosystem.

---

3. PRIMARY PLATFORM PRINCIPLE

One ELMKUSOMA Platform — Multiple Contexts

ELMKUSOMA must not become a collection of disconnected systems.

The architecture must prefer:

«One shared platform foundation with context-aware experiences.»

Examples:

ELMKUSOMA
│
├── Students
│   ├── Nursery
│   ├── Primary
│   ├── Secondary
│   └── Higher Education
│       ├── College / TVET
│       └── University
│
├── Other Learners
│   ├── Adult / Lifelong
│   ├── Professional
│   ├── Independent
│   ├── Skills
│   └── Non-formal
│
├── Institutions
│   ├── Schools
│   ├── Colleges
│   ├── TVET
│   └── Universities
│
└── Providers / Organizations
    ├── Banks
    ├── Companies
    ├── NGOs
    ├── Government Institutions
    ├── Professional Bodies
    ├── Training Organizations
    ├── Educators
    ├── Content Creators
    ├── Consultants
    ├── Event Organizers
    └── Skills Providers

Platform Admin governs this ecosystem from the top.

---

4. PLATFORM ADMIN ROLE

Platform Admin is the highest operational and governance role within ELMKUSOMA.

However:

«Highest authority does not mean unrestricted raw database access or an un-auditable superuser.»

Platform Admin should have platform-wide functional authority while critical operations remain protected by:

- Authentication
- Authorization
- Permission checks
- Scope checks
- Re-authentication where appropriate
- Confirmation
- Approval workflows where required
- Audit logging
- Separation of duties where necessary
- Break-glass controls
- Recovery protections

The goal is:

«Maximum legitimate platform control with minimum uncontrolled risk.»

---

5. PLATFORM ADMIN ECOSYSTEM MAP

                         ELMKUSOMA PLATFORM
                                  │
                           PLATFORM ADMIN
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ↓                       ↓                       ↓
     GOVERNANCE               ECOSYSTEM              OPERATIONS
          │                   MANAGEMENT                  │
          ↓                       ↓                       ↓
 Users / Roles             Institutions              Security
 Providers                 Organizations             Payments
 Services                  Educators                 Integrations
 Policies                  Learners                  Incidents
 Billing                   Content                   Health
 Data                      Events                    Monitoring

---

6. COMPLETE ECOSYSTEM RELATIONSHIP

                         PLATFORM ADMIN
                               │
                               ↓
                       ORGANIZATIONS
                    ┌──────────┴──────────┐
                    ↓                     ↓
              INSTITUTIONS            PROVIDERS
                    │                     │
                    ↓                     ↓
              Institution Admin     Provider Admin
                    │                     │
                    └──────────┬──────────┘
                               ↓
                            SERVICES
                               │
          ┌────────────┬───────┼───────┬────────────┐
          ↓            ↓       ↓       ↓            ↓
       COURSES       LIVE    EVENTS   MEDIA      RESOURCES
          │            │       │       │            │
          └────────────┴───────┼───────┴────────────┘
                               ↓
                            USERS
                               │
                 ┌─────────────┼─────────────┐
                 ↓             ↓             ↓
              Students    Other Learners   Professionals
                 │             │             │
                 └─────────────┼─────────────┘
                               ↓
                      ACCESS / LEARNING
                               ↓
               Progress / Evidence / Attendance
                               ↓
                         Certificates

---

7. ROLE ECOSYSTEM

ELMKUSOMA must clearly separate these roles:

Role| Primary Responsibility
Platform Admin| Platform-wide governance and control
Delegated/Assistant Platform Admin| Assigned platform responsibilities
Provider Admin| Own organization and services
Provider Staff| Assigned provider operations
Institution/School Admin| Own institution
Teacher/Lecturer/Instructor| Teaching and learning delivery
Education Authority| Authorized jurisdictional oversight
Student| Formal education learning
Other Learner| Flexible/non-formal/professional learning
Parent| Authorized learner/family visibility
System/Technical Operations Roles| Controlled technical operations where applicable

Existing role names must be inspected in the repository before introducing new roles.

Never create duplicate roles if equivalent roles already exist.

---

8. PLATFORM ADMIN HIERARCHY

PLATFORM ADMIN
│
├── Delegated / Assistant Platform Admin
│      ├── Selected permissions
│      ├── Selected scope
│      ├── Optional expiry
│      └── Full audit
│
├── Institution / School Admin
│      └── Own institution
│
└── Provider Admin
       └── Own organization
            ├── Provider Staff
            ├── Educators
            ├── Event Managers
            └── Service Managers

---

9. CREDENTIAL GOVERNANCE

Platform Admin may onboard and issue access to:

- School administrators
- Institution administrators
- Provider administrators
- Organization administrators
- Delegated administrators
- Authorized operational users

Credentials must be individually assigned.

Never:

- Share Platform Admin credentials
- Give Platform Admin password to a provider
- Give infrastructure secrets to providers
- Use one shared administrator account for multiple people

Instead:

Platform Admin
      ↓
Invite / Create Account
      ↓
Assign Role
      ↓
Assign Permissions
      ↓
Assign Scope
      ↓
Activate
      ↓
Audit

Credential lifecycle:

INVITED
   ↓
PENDING ACTIVATION
   ↓
ACTIVE
   ↓
SUSPENDED
   ↓
REACTIVATED / REVOKED
   ↓
ARCHIVED

---

10. DELEGATED / ASSISTANT PLATFORM ADMIN

The Platform Admin may have an assistant.

The assistant must NOT automatically inherit unrestricted Platform Admin power.

Delegation should support:

- Selected permissions
- Selected modules
- Selected operational scope
- Start date
- Expiry date
- Activation status
- Revocation
- Audit history

Example:

PLATFORM ADMIN
      ↓
Assistant Admin
      ↓
Permissions:
✓ Provider onboarding
✓ Support
✓ Content moderation

✗ Payment configuration
✗ Security policy
✗ Root platform configuration
✗ Destructive database operations

Where necessary, emergency/break-glass access may be supported with:

- Explicit justification
- Time limitation
- Strong authentication
- Full audit
- Automatic expiry
- Review

---

11. FUNDAMENTAL AUTHORIZATION MODEL

Authorization should conceptually follow:

AUTHENTICATION
      ↓
IDENTITY
      ↓
ROLE
      ↓
PERMISSION
      ↓
SCOPE
      ↓
RESOURCE
      ↓
ACTION
      ↓
AUDIT

Frontend visibility is never the security boundary.

Backend authorization must enforce access.

---

12. PLATFORM ADMIN MASTER SIDEBAR

The Platform Admin workspace should conceptually contain:

PLATFORM ADMIN
│
├── Command Center
│
├── Identity & Access
│
├── Organizations
│
├── Institutions
│
├── Providers
│
├── Services
│
├── Learning
│
├── Live & Events
│
├── Media & Resources
│
├── Commerce
│
├── Trust & Safety
│
├── Communications
│
├── Intelligence
│
├── Security
│
├── Operations
│
├── Data Governance
│
├── Audit & Compliance
│
├── Platform Configuration
│
├── Global Search
│
├── Support & Cases
│
└── Platform Lifecycle

Exact navigation must be reconciled with the existing frontend architecture before implementation.

Do not blindly create duplicate navigation structures.

---

13. MODULE 01 — PLATFORM COMMAND CENTER

Purpose

Answer:

«“What is happening across ELMKUSOMA right now, and what requires attention?”»

The dashboard must not be merely a collection of charts.

It should provide:

Platform Overview

Where actual data exists:

- Users
- Learners
- Teachers
- Lecturers
- Educators
- Institutions
- Providers
- Organizations
- Courses
- Learning content
- Live sessions
- Events
- Resources
- Media
- Certificates
- Transactions
- Active services
- Issues
- Security events
- Operational events

Attention Center

Examples:

- Provider awaiting verification
- Institution awaiting approval
- Payment integration failure
- Failed webhook
- Live session problem
- Recording processing problem
- Suspicious privileged login
- Expiring provider package
- Service quota approaching limit
- Unresolved report
- Security incident
- Failed background job

No fabricated metrics.

If real data is unavailable:

«Data unavailable»

not:

«0»

unless zero is actually known from the database.

---

14. MODULE 02 — IDENTITY & ACCESS GOVERNANCE

Platform Admin should manage:

- Users
- Roles
- Permissions
- Admins
- Delegated admins
- Access status
- Sessions
- Account activation
- Account suspension
- Account revocation
- Role assignment
- Permission assignment
- Security events
- Access history

Capabilities should include:

- Search
- Filter
- View
- Activate
- Suspend
- Revoke
- Assign appropriate roles
- Review associations
- Review security status
- Review audit history

Sensitive operations require appropriate controls.

---

15. MODULE 03 — ADMIN MANAGEMENT

Platform Admin must manage administrator accounts.

Supported concepts:

- Platform Admin
- Delegated Admin
- Institution Admin
- Provider Admin
- Provider Staff
- Other authorized administrative roles

For every administrative account, the system should be able to determine:

WHO
WHAT ROLE
WHAT PERMISSIONS
WHAT SCOPE
WHAT STATUS
WHEN CREATED
WHO CREATED
WHEN MODIFIED
WHEN EXPIRES
WHAT ACTIONS PERFORMED

---

16. MODULE 04 — INSTITUTION MANAGEMENT

Platform Admin may onboard and govern:

- Schools
- Colleges
- TVET institutions
- Universities
- Other legitimate educational institutions

Lifecycle:

APPLICATION
   ↓
VERIFICATION
   ↓
APPROVAL
   ↓
INSTITUTION CREATED
   ↓
ADMIN CREDENTIALS
   ↓
SERVICES ENABLED
   ↓
OPERATION
   ↓
MONITORING
   ↓
SUSPENSION / ARCHIVAL / EXIT

Platform Admin should be able to review:

- Institution profile
- Institution status
- Institution administrators
- Associated educators
- Learners
- Courses
- Classes
- Live sessions
- Events
- Resources
- Content
- Usage
- Reports
- Verification history
- Trust/policy history
- Billing where applicable

---

17. MODULE 05 — PROVIDER / ORGANIZATION ECOSYSTEM

This is a core ELMKUSOMA capability.

ELMKUSOMA must support organizations beyond traditional education institutions.

Potential providers include:

- Banks
- Companies
- NGOs
- Government institutions
- Professional bodies
- Training organizations
- Individual educators
- Content creators
- Authors
- Publishers
- Consultants
- Event organizers
- Skills providers
- Educational organizations
- Other legitimate service providers

The repository must be inspected first to determine whether existing organization/provider models can support these concepts.

Do not create unnecessary duplicate entities.

---

18. PROVIDER ONBOARDING

Provider lifecycle:

APPLICATION
      ↓
IDENTITY / ORGANIZATION DATA
      ↓
VERIFICATION
      ↓
APPROVAL
      ↓
PROVIDER ACCOUNT
      ↓
PROVIDER ADMIN CREDENTIALS
      ↓
SERVICE ELIGIBILITY
      ↓
SERVICE ACTIVATION
      ↓
PACKAGE / BILLING
      ↓
OPERATION
      ↓
MONITORING
      ↓
RENEW / MODIFY / SUSPEND / EXIT

Platform Admin controls onboarding.

Provider manages its own approved services.

---

19. PROVIDER WORKSPACE

After approval, a provider should operate from its own workspace.

Conceptual provider workspace:

PROVIDER WORKSPACE
│
├── Dashboard
├── Organization Profile
├── Team
├── Services
├── Courses
├── Seminars
├── Events
├── Live Sessions
├── Resources
├── Videos / Media
├── Participants
├── Attendance
├── Certificates
├── Communication
├── Analytics
├── Payments
├── Entitlements
└── Settings

Provider Admin must NOT receive Platform Admin credentials.

---

20. PROVIDER SERVICE CATALOGUE

Platform Admin should control which services are available to providers.

Examples:

Provider
│
├── Live Streaming
├── Courses
├── Seminars
├── Webinars
├── Events
├── Media
├── Resources
├── Certificates
└── Other approved services

The Service Catalogue should conceptually support:

- Service definition
- Service status
- Provider eligibility
- Activation
- Deactivation
- Usage limits
- Package association
- Access rules
- Commercial model
- Service dependencies

A provider should only access services explicitly enabled for it.

---

21. SERVICE ACTIVATION MODEL

PLATFORM ADMIN
      ↓
Provider Verification
      ↓
Service Eligibility
      ↓
Service Selection
      ↓
Package / Commercial Terms
      ↓
Activation
      ↓
Provider Uses Service
      ↓
Platform Monitors Usage

---

22. SERVICE USAGE & QUOTAS

Where supported, Platform Admin should monitor:

- Seats
- Participants
- Live hours
- Storage
- Events
- Courses
- Certificates
- Media usage
- Resource usage
- Package limits
- Usage consumption
- Expiry
- Overage

The actual measurable dimensions must be derived from available infrastructure and repository capabilities.

Never display invented usage.

---

23. PROVIDER SERVICE OWNERSHIP

Core boundary:

PLATFORM ADMIN
      ↓
Governance / Eligibility / Platform Control
      ↓
PROVIDER
      ↓
Own Services
      ↓
Own Participants
      ↓
Own Operations

Provider may manage its own:

- Courses
- Events
- Seminars
- Live sessions
- Resources
- Media
- Participants
- Attendance
- Provider communication
- Provider analytics
- Certificates where authorized

Platform Admin governs the ecosystem but should not unnecessarily perform provider operational tasks.

---

24. MODULE 06 — LEARNING & CONTENT GOVERNANCE

Platform Admin may oversee:

- Courses
- Modules
- Lessons
- Learning content
- Assessments
- Resources
- Media
- Educational materials
- Provider content
- Institution content

Capabilities may include:

- Review
- Publish governance
- Suspend
- Archive
- Moderate
- Search
- Inspect ownership
- View usage
- Review reports

Teaching responsibilities remain with teachers/lecturers/instructors.

---

25. CONTENT OWNERSHIP

Every important content object should have identifiable ownership or responsible organization/provider.

Admin should be able to determine:

WHO CREATED IT?
WHO OWNS IT?
WHO PUBLISHED IT?
WHO CAN MODIFY IT?
WHO CAN ACCESS IT?
WHAT ORGANIZATION DOES IT BELONG TO?
WHAT POLICY APPLIES?

---

26. MODULE 07 — LIVE & STREAMING OPERATIONS

Live Learning is a first-class ELMKUSOMA service.

It is not a separate product per education level.

Shared architecture should be reused.

Core loop:

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

Platform Admin should monitor:

- Live Now
- Upcoming
- Completed
- Provider
- Institution
- Course/class
- Event
- Teacher/instructor
- Topic
- Participants
- Attendance
- Started At
- Status
- Recording
- Processing
- Errors

---

27. LIVE ADMIN BOUNDARY

Platform Admin may have operational visibility where supported.

Examples:

- Session status
- Participant count
- Attendance
- Recording state
- Provider
- Institution
- Technical issues

Observer functionality, where implemented, must be:

«OBSERVER MODE / READ ONLY»

Observer must not:

- Grade students
- Change attendance
- Submit student work
- Modify content
- Control class participants
- Impersonate users
- Alter provider data
- Change live session ownership

Infrastructure secrets such as LiveKit secret keys must never be exposed through the normal admin UI.

---

28. LIVE OBSERVER SECURITY

Where live observation exists:

AUTHENTICATE
      ↓
VERIFY PLATFORM ADMIN AUTHORITY
      ↓
RESOLVE LIVE SESSION
      ↓
RESOLVE OWNER
      ↓
VERIFY AUTHORIZED ACCESS
      ↓
VERIFY SESSION ACTIVE
      ↓
CREATE OBSERVER SESSION
      ↓
READ-ONLY OBSERVATION
      ↓
AUDIT

---

29. MODULE 08 — EVENTS / SEMINARS / WEBINARS

ELMKUSOMA must support organizations such as banks and companies using the platform for:

- Seminars
- Workshops
- Webinars
- Professional events
- Training events
- Educational events
- Conferences
- Public learning events

Example:

BANK
 ↓
ELMKUSOMA
 ↓
Create Seminar
 ↓
Configure Access
 ↓
Public / Paid / Sponsored / Invite Only
 ↓
Users Register
 ↓
Payment / Entitlement
 ↓
Join
 ↓
Attendance
 ↓
Recording / Replay
 ↓
Evidence / Certificate where applicable

Platform Admin governs service availability and platform-level compliance.

---

30. MODULE 09 — MEDIA

Platform-wide media governance may include:

- Video
- Audio
- Recordings
- Educational media
- Provider media
- Event recordings

Admin should be able to monitor:

- Ownership
- Status
- Processing
- Storage
- Usage
- Reports
- Availability
- Policy status

No unnecessary duplication of media infrastructure.

---

31. MODULE 10 — RESOURCE LIBRARY

Resources may include:

- Documents
- Notes
- PDFs
- Guides
- Reference materials
- Learning resources
- Provider materials
- Institution materials

Platform Admin should govern:

- Ownership
- Visibility
- Publication
- Moderation
- Access
- Storage
- Reports
- Archival

---

32. MODULE 11 — CERTIFICATES & CREDENTIALS

Platform Admin should monitor:

- Certificates issued
- Certificate status
- Provider
- Program/course/event
- Recipient
- Verification
- Revocation
- Suspicious cases
- Certificate history

Certificates must only be issued through legitimate completion/authorization workflows.

Admin should not bypass legitimate issuance rules merely because of elevated privileges.

---

33. MODULE 12 — COMMERCE / BILLING / PAYMENTS

This is a major platform capability.

ELMKUSOMA should conceptually support:

- B2C
- B2B
- B2B2C
- Institution-sponsored access
- Provider-sponsored access
- Learner-paid access
- Hybrid access
- Subscription/package models
- Pay-as-you-go models

---

34. CENTRAL BILLING ARCHITECTURE

Payment logic must not be duplicated separately for:

- Live
- Courses
- Events
- Resources
- Media
- Certificates

Instead:

BILLING
   ↓
PAYMENT
   ↓
TRANSACTION
   ↓
VERIFICATION
   ↓
ENTITLEMENT
   ↓
ACCESS

This should be a central platform capability.

---

35. PAYMENT MODEL A — LEARNER PAYS

Provider / Institution
        ↓
Creates Paid Service
        ↓
Learner Requests Access
        ↓
Payment
        ↓
Transaction Verification
        ↓
Entitlement
        ↓
Access Granted

---

36. PAYMENT MODEL B — PROVIDER SPONSORS USERS

Example:

A bank purchases a package of 500 participant seats.

BANK
 ↓
Purchases Package
 ↓
500 Sponsored Seats
 ↓
Selects / Invites Users
 ↓
Entitlements
 ↓
Users Join FREE

The learner does not pay individually for the sponsored service.

---

37. PAYMENT MODEL C — HYBRID

Provider sponsors 500 seats
            +
Public users can pay individually
            ↓
        SAME SERVICE

The system must distinguish:

- Sponsored users
- Self-paying users
- Public users
- Invite-only users
- Package users

without creating separate service implementations.

---

38. ACCESS MODELS

Services should conceptually support:

- Public Free
- Public Paid
- Sponsored
- Private / Invite Only
- Institution Only
- Package Access
- Hybrid

Actual implementation should reuse existing access/entitlement models where possible.

---

39. ENTITLEMENT ENGINE

Central entitlement architecture:

PAYMENT
   │
   ├── Learner payment
   ├── Provider package
   ├── Institution sponsorship
   └── Other authorized grant
          ↓
      ENTITLEMENT
          ↓
   ┌──────┼────────┐
   ↓      ↓        ↓
 USER   GROUP   ORGANIZATION
          ↓
       SERVICE
          ↓
 Course / Live / Event / Resource

Entitlement must determine:

- Who can access
- What they can access
- Why they can access it
- When access starts
- When access expires
- Whether access is sponsored
- Whether package limits apply
- Whether access is revoked

---

40. PROVIDER COMMERCIAL MODEL

Provider may:

1. Pay ELMKUSOMA for service usage.
2. Choose learner-paid access.
3. Sponsor users.
4. Purchase participant packages.
5. Use hybrid access.
6. Subscribe to platform services.
7. Use approved pay-as-you-go services.

Possible package concepts:

Provider Package
├── Participants
├── Events
├── Live
├── Media
├── Storage
├── Courses
├── Certificates
├── Duration
└── Other approved usage limits

Exact commercial rules must be based on implemented business requirements and real payment capabilities.

---

41. MODULE 13 — VERIFICATION / TRUST / MODERATION

Platform Admin should govern verification of:

- Institutions
- Providers
- Organizations
- Educators
- Content
- Courses
- Events
- Resources
- Users where applicable

Lifecycle:

SUBMITTED
   ↓
UNDER REVIEW
   ↓
APPROVED
   ↓
ACTIVE

or:

SUBMITTED
   ↓
REJECTED

or:

ACTIVE
   ↓
SUSPENDED
   ↓
REVIEW
   ↓
REINSTATED / ARCHIVED

All significant decisions should be traceable.

---

42. TRUST HISTORY

For organizations/providers, Platform Admin should be able to inspect:

- Verification status
- Verification history
- Policy reports
- Suspensions
- Warnings
- Content reports
- Payment issues
- Security incidents
- Credential status

Avoid unsupported artificial “trust scores.”

Evidence should be shown instead.

---

43. MODULE 14 — USERS / LEARNERS / EDUCATORS

Platform Admin should have platform-wide visibility, subject to privacy and authorization rules, into:

Learners

- Student ecosystem
- Other Learners
- Learning activity
- Enrollments
- Access
- Status
- Relevant progress
- Issues

Educators

- Teachers
- Lecturers
- Instructors
- Content creators where appropriate
- Provider associations
- Institution associations
- Courses
- Live sessions
- Events
- Content
- Verification status

Do not duplicate learner or teacher systems already implemented elsewhere.

---

44. MODULE 15 — COMMUNICATIONS

Platform Admin may manage platform-wide communications such as:

- Notifications
- Announcements
- Operational alerts
- Provider communications
- Institution communications
- Security notices
- System notices

Communication must respect:

- Role
- Scope
- Consent
- Notification preferences
- Privacy
- Delivery status

---

45. MODULE 16 — PLATFORM ANALYTICS & INTELLIGENCE

Platform Admin intelligence may cover:

People

- Users
- Learners
- Educators
- Organizations

Learning

- Enrollments
- Participation
- Completion
- Progress
- Learning activity

Content

- Courses
- Resources
- Media
- Usage

Live

- Sessions
- Participants
- Attendance
- Replays

Business

- Payments
- Transactions
- Packages
- Entitlements
- Provider usage

Operations

- Availability
- Failures
- Integrations
- Processing

All analytics must be based on real available data.

---

46. ATTENTION ENGINE

The system should surface actionable situations.

Example:

REAL PLATFORM DATA
       ↓
INDICATOR
       ↓
ATTENTION CONDITION
       ↓
ALERT
       ↓
EVIDENCE
       ↓
REVIEW
       ↓
ACTION
       ↓
FOLLOW-UP
       ↓
RESOLVED

Example alerts:

- Provider verification pending
- Payment failure
- Webhook failure
- Security anomaly
- Live recording processing failure
- Package nearing expiry
- Service quota reached
- Suspicious privileged action

AI, if used, must be controlled and evidence-based.

No fabricated explanations.

---

47. MODULE 17 — SECURITY CENTER

Platform Admin security center should cover available real security events such as:

- Authentication events
- Failed logins
- Suspicious logins
- Privileged actions
- Role changes
- Permission changes
- Session activity
- Account lockouts
- Security alerts
- Critical configuration changes

Security actions must be auditable.

---

48. ADMIN SELF-PROTECTION

Platform Admin must be protected from catastrophic accidental actions.

Examples:

- Cannot accidentally remove the last valid administrative/recovery path.
- Critical operations require confirmation.
- Sensitive changes may require re-authentication.
- Destructive actions may require approval.
- Emergency access must be time-limited.
- Audit logs cannot be silently deleted by ordinary administrative actions.

---

49. SEPARATION OF DUTIES

For high-risk operations, the system may require:

INITIATOR
    ↓
APPROVAL
    ↓
EXECUTION
    ↓
AUDIT

Potential examples:

- Destructive data operations
- Critical billing operations
- Major provider suspension
- Security configuration changes
- Platform-wide policy changes
- High-risk access changes

Exact approval rules must be based on business requirements and actual implementation capabilities.

---

50. MODULE 18 — AUDIT & ACCOUNTABILITY

Every critical action should capture, where technically supported:

ACTOR
ACTION
RESOURCE
RESOURCE ID
BEFORE
AFTER
TIMESTAMP
CONTEXT
RESULT
REASON
IP / SESSION CONTEXT WHERE APPROPRIATE

Examples:

- Created provider
- Approved institution
- Issued admin credentials
- Changed role
- Changed permissions
- Activated service
- Suspended provider
- Modified package
- Changed entitlement
- Viewed sensitive information
- Observed live session
- Generated report
- Changed platform configuration

---

51. AUDIT LOG PROTECTION

Platform Admin must not have a normal “erase my history” capability.

Audit records must have controlled retention and administrative governance.

Where deletion is legally/operationally required, it must follow defined retention policies and remain appropriately traceable.

---

52. MODULE 19 — INCIDENT MANAGEMENT

Platform Admin should be able to manage platform incidents.

Possible categories:

- Security
- Authentication
- Payment
- Live streaming
- Database
- Media
- Notification
- API
- Performance
- Availability
- Integration
- Background jobs

Lifecycle:

DETECTED
   ↓
INVESTIGATING
   ↓
CONTAINED
   ↓
RESOLVED
   ↓
REVIEWED

Each incident should have:

- Description
- Evidence
- Severity
- Status
- Owner
- Timeline
- Actions
- Resolution
- Follow-up

---

53. MODULE 20 — PLATFORM OPERATIONS

Where observability exists, Platform Admin should see:

- Backend health
- Database status
- API status
- Realtime/WebSocket status
- LiveKit integration status
- Media processing
- Storage
- Notifications
- Payment integrations
- Background jobs
- External service status
- Errors
- Failures

Do not display a fake “System Healthy” indicator.

If health cannot be verified:

«Health status unavailable.»

---

54. MODULE 21 — INTEGRATION MANAGEMENT

Potential integrations:

- Payment providers
- Mobile money
- Banks
- LiveKit
- Email
- SMS
- Storage
- Authentication
- Notification services
- Other approved external services

Platform Admin may inspect:

- Connection status
- Last successful operation
- Failure count where available
- Webhook status
- Retry status
- Diagnostics
- Configuration status

Secrets must never be displayed.

---

55. WEBHOOK OPERATIONS

Where webhook infrastructure exists, monitor:

- Incoming events
- Verification status
- Processing result
- Failed events
- Retry status
- Last success
- Error details where safe

Payment and Live integrations should have observable webhook health where supported.

---

56. MODULE 22 — DATA GOVERNANCE

Platform Admin governance should cover:

- Data ownership
- Data access
- Privacy
- Retention
- Archival
- Deletion
- Export
- Correction
- Sensitive-data access
- Access history
- Data quality

The system should clearly distinguish:

WHO OWNS DATA
WHO MANAGES DATA
WHO CAN VIEW DATA
WHO CAN MODIFY DATA
WHO CAN EXPORT DATA
WHO CAN DELETE DATA

---

57. DATA QUALITY CENTER

Where supported, surface:

- Incomplete records
- Duplicate organizations
- Duplicate users
- Invalid relationships
- Orphan records
- Failed synchronization
- Broken references
- Invalid configuration
- Data inconsistencies

Never silently “repair” critical data without controlled workflows.

---

58. MODULE 23 — BACKUP & DISASTER RECOVERY

Platform Admin/Operations should have visibility into:

- Last successful backup
- Backup failures
- Backup status
- Recovery readiness
- Restoration procedures
- Integrity checks where supported
- Recovery events

Actual backup infrastructure must be inspected before implementing UI.

Do not create a fake backup dashboard.

---

59. MODULE 24 — GLOBAL SEARCH

Platform Admin should have a permission-aware global search.

Potential entities:

User
Institution
School
Provider
Organization
Admin
Teacher
Learner
Course
Content
Live Session
Event
Seminar
Resource
Media
Certificate
Payment
Transaction
Entitlement
Incident
Audit Event

Search must respect authorization and data sensitivity.

---

60. MODULE 25 — SUPPORT & CASE MANAGEMENT

Platform Admin may manage operational cases such as:

- Account access
- Payment issues
- Live issues
- Course access
- Certificate issues
- Provider issues
- Institution issues
- Content reports
- Technical problems

Case lifecycle:

OPEN
 ↓
ASSIGNED
 ↓
INVESTIGATING
 ↓
ACTION REQUIRED
 ↓
RESOLVED
 ↓
CLOSED

---

61. MODULE 26 — PLATFORM CONFIGURATION

Platform Admin may govern platform-level settings such as:

- Service availability
- Provider eligibility
- Access policies
- Content policies
- Registration policies
- Notification policies
- Platform rules
- Feature flags
- Maintenance settings
- Other supported global configuration

Configuration changes must be audited.

---

62. MODULE 27 — POLICY & GOVERNANCE ENGINE

Where appropriate, platform rules should be represented centrally rather than scattered across unrelated frontend components.

Potential policy areas:

- Who can register
- Who can create providers
- Who can publish
- Who can create Live sessions
- Who can issue certificates
- Who can charge users
- Who can sponsor users
- Who can invite participants
- Who can access paid services
- Who requires verification
- Who requires approval

Do not over-engineer a generic policy engine if the repository does not require one.

---

63. MODULE 28 — PLATFORM LIFECYCLE

Platform Admin should conceptually support:

ONBOARD
   ↓
CONFIGURE
   ↓
OPERATE
   ↓
MONITOR
   ↓
MAINTAIN
   ↓
UPGRADE
   ↓
DEPRECATE
   ↓
RETIRE

For providers/services:

ACTIVE
 ↓
SUSPENDED
 ↓
REVIEW
 ↓
REACTIVATED / ARCHIVED

For platform features:

PLANNED
 ↓
DEVELOPMENT
 ↓
TESTING
 ↓
ROLLOUT
 ↓
ACTIVE
 ↓
DEPRECATED
 ↓
RETIRED

---

64. PROVIDER OFFBOARDING

Provider exit must be designed intentionally.

Possible workflow:

OFFBOARDING REQUEST
       ↓
REVIEW SERVICES
       ↓
SETTLE BILLING
       ↓
REVIEW ACTIVE USERS
       ↓
REVIEW ENTITLEMENTS
       ↓
REVIEW CONTENT / MEDIA
       ↓
REVOKE CREDENTIALS
       ↓
DISABLE SERVICES
       ↓
ARCHIVE REQUIRED RECORDS
       ↓
FINAL AUDIT
       ↓
CLOSED

Learner access and certificates must be handled according to applicable policy.

---

65. PLATFORM-WIDE ECOSYSTEM MAP

Platform Admin should be able to understand relationships such as:

Organization
   ↓
Provider
   ↓
Provider Admin
   ↓
Services
   ↓
Courses / Live / Events / Resources
   ↓
Participants
   ↓
Payments / Sponsorship
   ↓
Entitlements
   ↓
Learning Evidence

This relationship visibility is important for troubleshooting, governance, support and analytics.

---

66. LEARNER ECOSYSTEM INTEGRATION

Platform Admin must recognize the full learner ecosystem:

LEARNER ECOSYSTEM
│
├── Student
│   ├── Nursery
│   ├── Primary
│   ├── Secondary
│   └── Higher Education
│       ├── College / TVET
│       └── University
│
└── Other Learner
    ├── Adult / Lifelong
    ├── Professional
    ├── Independent
    ├── Skills
    └── Non-formal

Platform Admin governs the platform infrastructure around these learners.

It must not redesign or break their existing learner experiences merely to implement Platform Admin.

---

67. EDUCATION AUTHORITY BOUNDARY

Education Authority is different from Platform Admin.

PLATFORM ADMIN
      ↓
ELMKUSOMA PLATFORM-WIDE GOVERNANCE

while:

EDUCATION AUTHORITY
      ↓
AUTHORIZED JURISDICTION
      ↓
Education Oversight

Education Authority may monitor:

- Region
- District
- Ward
- School
- Class
- Teacher
- Student
- Education indicators

according to jurisdiction.

Platform Admin governs the platform itself.

Do not merge these responsibilities.

---

68. PROVIDER VS INSTITUTION

A school/institution and a provider are related but should not automatically be treated as identical.

Institution

Examples:

- School
- College
- TVET
- University

Provider

Examples:

- Bank
- Company
- NGO
- Government institution
- Professional body
- Training organization
- Educator
- Content creator
- Event organizer
- Skills provider

Some institutions may also act as providers of services.

The repository/domain model must be inspected before deciding whether one existing organization model can represent both.

---

69. CORE COMMERCIAL RELATIONSHIP

                   ELMKUSOMA
                       │
              ┌────────┴────────┐
              ↓                 ↓
        INSTITUTION          PROVIDER
              │                 │
              └────────┬────────┘
                       ↓
                    SERVICE
                       ↓
                 ACCESS MODEL
                       ↓
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
    LEARNER PAY    SPONSORED        HYBRID
        │              │              │
        └──────────────┼──────────────┘
                       ↓
                  ENTITLEMENT
                       ↓
                     USER

---

70. CENTRAL ACCESS PRINCIPLE

The system should not implement:

LivePaymentSystem
CoursePaymentSystem
EventPaymentSystem
ResourcePaymentSystem

as separate unrelated systems.

Instead:

CENTRAL BILLING
      ↓
TRANSACTION
      ↓
ENTITLEMENT
      ↓
SERVICE ACCESS

This improves consistency, security and scalability.

---

71. ARCHITECTURAL NON-NEGOTIABLES

1. Inspect the repository before coding.
2. Repository is the source of truth.
3. Existing working functionality must be preserved.
4. Reuse existing architecture wherever appropriate.
5. Do not create duplicate entities unnecessarily.
6. Do not create duplicate APIs unnecessarily.
7. Do not create duplicate payment systems.
8. Do not create duplicate Live systems.
9. Do not create duplicate user/role systems.
10. Do not create fake production data.
11. Do not create fake analytics.
12. Do not create fake health indicators.
13. Do not expose infrastructure secrets.
14. Backend authorization is mandatory.
15. Frontend is not a security boundary.
16. Critical administrative actions must be auditable.
17. Provider credentials must remain separate from Platform Admin credentials.
18. Providers must not receive platform infrastructure secrets.
19. Payment must lead to verified transaction state before entitlement.
20. Entitlement must control access.
21. Existing learner systems must remain intact.
22. Existing teacher systems must remain intact.
23. Existing parent systems must remain intact.
24. Existing Education Authority systems must remain intact.
25. Shared Live architecture must be reused.
26. Changes to shared files must be minimized.
27. Database changes require migration safety.
28. API changes require backward-compatibility analysis.
29. Destructive operations require safeguards.
30. Completion must be evidence-based.

---

72. UX/UI PRINCIPLES

Platform Admin should feel like:

«A professional platform operations and governance center.»

Not:

- A generic school dashboard
- A student dashboard
- A collection of random charts
- A flashy admin template
- A fake SaaS dashboard

The UI should prioritize:

- Clarity
- Hierarchy
- Information density without clutter
- Searchability
- Actionability
- Accessibility
- Responsive design
- Consistent navigation
- Meaningful empty states
- Meaningful error states
- Loading states
- Clear destructive-action warnings
- Clear permissions
- Clear status
- Evidence-based alerts

Avoid unnecessary:

- Excessive gradients
- Excessive animations
- Excessive glassmorphism
- Decorative charts
- Fake AI cards
- Fake statistics
- Dashboard clutter

---

73. RESPONSIVE & ACCESSIBILITY REQUIREMENTS

The Platform Admin workspace should support:

- Desktop
- Tablet
- Mobile where operationally appropriate

Accessibility should include:

- Keyboard navigation
- Semantic structure
- Accessible controls
- Clear contrast
- Screen-reader considerations
- Clear focus states
- Non-color-only status indicators
- Appropriate confirmation flows

---

74. PERFORMANCE REQUIREMENTS

Implementation must avoid:

- Duplicate API requests
- N+1 queries
- Unnecessary frontend rerenders
- Oversized bundles
- Unnecessary polling
- Unbounded data tables
- Loading entire datasets when pagination is appropriate

Use:

- Pagination
- Filtering
- Server-side search where appropriate
- Caching where justified
- Efficient queries
- Appropriate indexes
- Lazy loading where useful

---

75. API REQUIREMENTS

Platform Admin APIs should have:

- Authentication
- Server-side authorization
- Validation
- Scope checks
- Pagination
- Filtering
- Sorting where appropriate
- Consistent error responses
- Safe handling of sensitive information
- Backward compatibility
- Audit integration for critical operations

Never rely only on:

if (user.role === "ADMIN")

when resource scope or permission-level authorization is required.

---

76. DATABASE REQUIREMENTS

Before modifying the database:

1. Inspect existing entities.
2. Inspect existing relationships.
3. Inspect existing migrations.
4. Inspect existing repositories.
5. Inspect existing services.
6. Identify reusable models.
7. Identify duplicate concepts.
8. Only then determine required schema changes.

Potential concepts may include:

- Provider
- Organization
- Service
- Service entitlement
- Provider package
- Sponsored group
- Delegation
- Audit event
- Incident
- Observer session
- Verification record

But these must not automatically become new tables.

Only introduce schema changes where existing architecture cannot satisfy the requirement.

---

77. SECURITY TESTING

Minimum security verification should include:

Role Isolation

Provider Admin → Platform Admin functions
Expected: DENIED

Institution Admin → Another Institution
Expected: DENIED

Provider Admin → Another Provider
Expected: DENIED

ID Manipulation

Test unauthorized manipulation of:

- userId
- organizationId
- providerId
- institutionId
- serviceId
- courseId
- liveClassId
- eventId
- transactionId
- entitlementId
- certificateId

Changing IDs must not bypass authorization.

---

78. PRIVILEGED ACTION TESTING

Verify:

- Role assignment
- Permission changes
- Provider approval
- Institution approval
- Service activation
- Provider suspension
- Entitlement changes
- Package changes
- Sensitive data access
- Critical configuration
- Delegated admin creation
- Delegated admin revocation

All must respect authorization and audit requirements.

---

79. BUSINESS RULE VERIFICATION

Verify:

Learner Pays
→ Transaction Verified
→ Entitlement
→ Access

Provider Pays
→ Package Active
→ Sponsored User
→ Entitlement
→ Free Access

Hybrid
→ Sponsored Users
+
Self-Paying Users
→ Same Service

Verify expiry:

ENTITLEMENT ACTIVE
      ↓
EXPIRY
      ↓
ACCESS REVOKED

unless another valid entitlement still grants access.

---

80. LIVE E2E VERIFICATION

Where supported:

Provider
 ↓
Create Session
 ↓
Schedule
 ↓
Prepare
 ↓
Start
 ↓
Backend Validation
 ↓
Short-lived Token
 ↓
LiveKit
 ↓
Learner
 ↓
Attendance
 ↓
End
 ↓
Recording
 ↓
Replay

Platform Admin should be able to observe operational state where authorized.

---

81. PROVIDER E2E ACCEPTANCE

A valid provider journey should be:

Provider Application
      ↓
Verification
      ↓
Approval
      ↓
Provider Credentials
      ↓
Service Activation
      ↓
Package / Billing
      ↓
Provider Workspace
      ↓
Create Service
      ↓
Publish
      ↓
User Access
      ↓
Payment / Sponsorship
      ↓
Entitlement
      ↓
Participation
      ↓
Evidence / Attendance
      ↓
Reporting

This should work end-to-end using real repository functionality.

---

82. ADMIN END-TO-END ACCEPTANCE

Platform Admin should be able to:

1. Authenticate.
2. Reach Platform Admin workspace.
3. View real platform overview.
4. Search platform entities.
5. Manage authorized administrators.
6. Onboard institution/provider where supported.
7. Issue appropriate credentials.
8. Assign permissions/scope.
9. Enable provider services.
10. Monitor service usage where supported.
11. Monitor learning/live/events/content.
12. Monitor billing/payments.
13. Manage verification/moderation.
14. Review security events.
15. Review audit history.
16. Handle incidents/cases where supported.
17. Review platform operations.
18. Manage supported platform configuration.
19. Preserve existing user/learner functionality.

---

83. IMPLEMENTATION ROADMAP

PHASE 0 — REPOSITORY AUDIT

Before implementation:

- Inspect repository
- Identify current Platform Admin role
- Identify authentication architecture
- Identify authorization architecture
- Identify existing dashboards
- Identify organization models
- Identify institution models
- Identify provider models
- Identify user models
- Identify service models
- Identify course models
- Identify live models
- Identify event models
- Identify media/resource models
- Identify payment models
- Identify certificate models
- Identify audit models
- Identify notification models
- Identify security models
- Identify migrations
- Identify existing admin APIs
- Identify frontend routes
- Identify reusable components

Deliverable:

«Platform Admin Capability Gap Matrix.»

---

84. PHASE 1 — IDENTITY & ACCESS FOUNDATION

Implement/fix:

- Platform Admin authentication
- Platform Admin authorization
- Role model
- Permission model
- Admin accounts
- Delegation
- Scope
- Credential lifecycle
- Critical-action protection
- Audit foundation

Do not continue to complex modules if authorization foundation is broken.

---

85. PHASE 2 — PLATFORM ADMIN SHELL

Implement:

- Admin layout
- Navigation
- Command Center
- Global search foundation
- Notifications/alerts
- Profile
- Settings
- Permission-aware navigation

No fake dashboard metrics.

---

86. PHASE 3 — INSTITUTIONS & PROVIDERS

Implement:

- Institution management
- Provider management
- Organization profiles
- Verification
- Admin credential issuance
- Provider team foundation
- Provider status
- Onboarding lifecycle

---

87. PHASE 4 — SERVICE CATALOGUE

Implement:

- Service definitions
- Service status
- Provider eligibility
- Service activation
- Service deactivation
- Usage limits where supported
- Package association
- Service visibility

---

88. PHASE 5 — USERS / EDUCATORS / LEARNERS

Implement or integrate:

- Platform-wide user management
- Educator management
- Learner visibility
- Institution associations
- Provider associations
- Account status
- Relevant activity
- Access management

Reuse existing learner/teacher systems.

---

89. PHASE 6 — LEARNING / CONTENT / RESOURCES / CERTIFICATES

Implement/govern:

- Courses
- Content
- Assessments
- Resources
- Media
- Certificates
- Publication/moderation
- Ownership
- Usage

---

90. PHASE 7 — LIVE / EVENTS / MEDIA

Implement/integrate:

- Live monitoring
- Live sessions
- Events
- Seminars
- Webinars
- Recordings
- Attendance
- Media operations
- Observer mode where supported

Reuse shared LiveKit/WebSocket architecture.

---

91. PHASE 8 — BILLING / PAYMENTS / ENTITLEMENTS

Implement/integrate:

- Provider packages
- Pricing/configuration where required
- Transactions
- Payment verification
- Sponsored seats
- Learner-paid access
- Hybrid access
- Entitlements
- Expiry
- Revocation
- Billing visibility

---

92. PHASE 9 — TRUST / MODERATION / VERIFICATION

Implement:

- Provider verification
- Institution verification
- Educator verification
- Content moderation
- Reports
- Suspensions
- Warnings
- Trust history
- Review workflows

---

93. PHASE 10 — SECURITY / AUDIT / DELEGATION

Implement/finalize:

- Security center
- Audit
- Privileged actions
- Delegated admin
- Break-glass controls
- Separation of duties where required
- Re-authentication for sensitive actions
- Session visibility
- Security alerts

---

94. PHASE 11 — ANALYTICS / INTELLIGENCE

Implement:

- Platform analytics
- Learning analytics
- Provider analytics
- Business analytics
- Live analytics
- Operational analytics
- Attention engine
- Evidence-based intelligence

No fabricated AI.

---

95. PHASE 12 — OPERATIONS / INTEGRATIONS / INCIDENTS

Implement/integrate:

- System health
- Integrations
- Webhooks
- Background jobs
- Incident management
- Support cases
- Operational diagnostics
- Failure monitoring

Only show metrics supported by real observability.

---

96. PHASE 13 — DATA GOVERNANCE / BACKUP / DR

Implement/integrate:

- Data governance
- Data quality
- Retention
- Archival
- Export
- Privacy
- Backup visibility
- Recovery readiness
- Restoration procedures where supported

---

97. PHASE 14 — E2E INTEGRATION

Validate:

PLATFORM ADMIN
      ↓
PROVIDER
      ↓
SERVICE
      ↓
BILLING / ENTITLEMENT
      ↓
LEARNER
      ↓
ACCESS
      ↓
LEARNING
      ↓
EVIDENCE
      ↓
ANALYTICS
      ↓
PLATFORM ADMIN

Also validate:

INSTITUTION
      ↓
ADMIN
      ↓
TEACHER
      ↓
LEARNER
      ↓
LEARNING
      ↓
PLATFORM ADMIN VISIBILITY

---

98. PHASE 15 — PRODUCTION READINESS

Before declaring complete:

- Security audit
- Authorization audit
- IDOR/BOLA testing
- API testing
- Frontend testing
- Database testing
- Integration testing
- E2E testing
- Performance review
- Accessibility review
- Migration review
- Regression testing
- Error-state testing
- Empty-state testing
- Observability verification
- Documentation review

---

99. DEPENDENCY MAP

IDENTITY
   ↓
AUTHORIZATION
   ↓
ORGANIZATION / INSTITUTION / PROVIDER
   ↓
SERVICE CATALOGUE
   ↓
SERVICE ACTIVATION
   ↓
COURSES / LIVE / EVENTS / MEDIA / RESOURCES
   ↓
PAYMENTS
   ↓
ENTITLEMENTS
   ↓
LEARNER ACCESS
   ↓
PROGRESS / ATTENDANCE / EVIDENCE
   ↓
CERTIFICATES
   ↓
ANALYTICS

Cross-cutting:

SECURITY
AUDIT
NOTIFICATIONS
SEARCH
DATA GOVERNANCE
OBSERVABILITY

These must remain reusable platform capabilities.

---

100. DEVELOPER OWNERSHIP PRINCIPLE

Implementation should be divided by capability where necessary, but ownership boundaries must be explicit.

No two developers should independently redesign the same:

- Admin shell
- Role model
- Permission model
- Organization model
- Payment model
- Entitlement model
- Live architecture
- Notification architecture
- Audit architecture

Shared contracts must be agreed before implementation.

---

101. SHARED FILE PROTECTION

Before modifying a shared file:

1. Identify owner.
2. Identify dependencies.
3. Check current branch state.
4. Check whether another developer is modifying it.
5. Make the smallest necessary change.
6. Document the change.
7. Run relevant tests.

Avoid unnecessary changes to shared:

- Auth
- API client
- DB models
- Global components
- Routing
- Design system
- Security middleware

---

102. IMPLEMENTATION STATUS SYSTEM

Every capability should use:

[ ] NOT STARTED
[~] IN PROGRESS
[✓] VERIFIED COMPLETE
[!] BLOCKED
[⚠] PARTIAL
[✗] BROKEN

A feature is not "[✓] VERIFIED COMPLETE" merely because:

- UI exists
- route exists
- button exists
- API exists

It requires end-to-end evidence.

---

103. CAPABILITY VERIFICATION MODEL

For each capability evaluate:

Requirement
   ↓
Frontend
   ↓
API
   ↓
Authentication
   ↓
Authorization
   ↓
Business Logic
   ↓
Database
   ↓
Response
   ↓
Frontend Rendering
   ↓
Tests

If any critical layer is missing, the feature is not fully implemented.

---

104. REQUIREMENT COMPLETION MATRIX

Each requirement should be classified as:

- VERIFIED COMPLETE
- PARTIAL
- MISSING
- BROKEN
- NOT APPLICABLE

Completion:

COMPLETION %
=
VERIFIED COMPLETE applicable requirements
÷
TOTAL applicable requirements
× 100

The percentage must be based on actual:

- Repository evidence
- Source code
- Database/migrations
- API behavior
- Authentication/authorization
- Tests
- Integration evidence

Never estimate completion from visual appearance.

---

105. TARGET

The implementation target is:

«98%+ verified requirement coverage before production-readiness declaration.»

However:

98% is a target, not permission to inflate the percentage.

If actual evidence shows:

94%

report:

94%

and continue fixing applicable gaps where safe and practical.

---

106. FINAL QUALITY GATE

The Platform Admin should only be considered production-ready when:

PRODUCT
  ✓

ARCHITECTURE
  ✓

FRONTEND
  ✓

BACKEND
  ✓

DATABASE
  ✓

AUTHORIZATION
  ✓

SECURITY
  ✓

PAYMENTS / ENTITLEMENTS
  ✓

LIVE / EVENTS
  ✓

PROVIDERS
  ✓

INSTITUTIONS
  ✓

AUDIT
  ✓

OPERATIONS
  ✓

INTEGRATIONS
  ✓

TESTING
  ✓

E2E
  ✓

REGRESSION
  ✓

with no unresolved critical blocker.

---

107. MASTER WORKFLOW

Every implementation cycle must follow:

INSPECT
   ↓
UNDERSTAND
   ↓
AUDIT
   ↓
MAP EXISTING ARCHITECTURE
   ↓
IDENTIFY GAPS
   ↓
DESIGN MINIMUM NECESSARY CHANGE
   ↓
IMPLEMENT
   ↓
INTEGRATE
   ↓
TEST
   ↓
SECURITY VERIFY
   ↓
E2E VERIFY
   ↓
AUDIT AGAIN
   ↓
REPORT

---

108. MASTER COGNITIVE ENGINEERING PRINCIPLES

All implementation work must follow the ELMKUSOMA Master Cognitive Engineering principles:

Evidence Categories

Distinguish:

- User Requirements
- Established Facts
- Externally Verified Facts
- Operational Assumptions
- Inferences

Do not convert assumptions into facts.

First Principles

Before adding architecture, ask:

1. What problem exists?
2. What already solves part of it?
3. What is the minimum missing capability?
4. Can the existing model be extended?
5. Will a new entity/API/system create duplication?
6. What are the security implications?
7. What is the end-to-end acceptance condition?

Materiality Gate

Do not stop for irrelevant ambiguity.

Stop and ask targeted questions only when unresolved ambiguity materially changes:

- Architecture
- Core logic
- Database model
- Business rules
- Security
- API contracts
- Acceptance criteria

---

109. ADVERSARIAL PRE-MORTEM

Before major implementation, consider at minimum:

Failure 1 — Duplicate systems

Example:

A second payment system is created only for provider services.

Mitigation:

Use central Billing → Transaction → Entitlement → Access.

Failure 2 — Provider receives excessive authority

Example:

Provider can access Platform Admin functions.

Mitigation:

Strict role + permission + organization scope.

Failure 3 — Fake operational data

Example:

Admin dashboard displays fabricated “99.9% uptime.”

Mitigation:

Only display observable real data.

Failure 4 — Frontend-only security

Example:

Hidden admin menu prevents access visually but API remains accessible.

Mitigation:

Server-side authorization and direct API security testing.

Failure 5 — Shared architecture breaks

Example:

Platform Admin changes Live/Enrollment/Auth models and breaks learner experiences.

Mitigation:

Inspect dependencies and perform regression testing.

Failure 6 — Credential sharing

Example:

Assistant or provider receives Platform Admin password.

Mitigation:

Individual accounts + delegated permissions + audit.

Failure 7 — Entitlement inconsistency

Example:

Payment succeeds but access is not correctly granted/revoked.

Mitigation:

Central entitlement lifecycle and E2E payment/access tests.

Failure 8 — Uncontrolled destructive administration

Example:

Admin deletes critical data accidentally.

Mitigation:

Confirmation, re-authentication, approval, soft-delete/archival where appropriate, audit and recovery.

---

110. DEFINITION OF DONE

A Platform Admin capability is DONE only when:

- Requirement exists in this document.
- Existing architecture was inspected.
- Existing implementation was reused where possible.
- Backend is implemented.
- Frontend is implemented.
- Database is correct where needed.
- Authentication works.
- Authorization works.
- Business rules work.
- API works.
- UI consumes real data.
- Error states work.
- Empty states work.
- Security has been tested.
- Integration has been tested.
- Regression has been tested.
- Audit exists where required.
- Documentation/status is updated.

---

111. WHAT MUST NEVER HAPPEN

Never:

- Build a fake admin dashboard.
- Populate production UI with mock statistics.
- Create duplicate user systems.
- Create duplicate organization systems without justification.
- Create separate payment logic for every service.
- Create separate Live systems for every education level.
- Give providers Platform Admin credentials.
- Give providers LiveKit/payment infrastructure secrets.
- Trust frontend role checks as security.
- Allow ID manipulation to bypass authorization.
- Delete critical records without safeguards.
- Claim completion without evidence.
- Claim 98%+ simply because the UI looks complete.
- Break existing learner, teacher, parent or authority experiences.
- Replace working architecture without material justification.

---

112. FUTURE EXPANSION

The architecture should leave room for future legitimate expansion such as:

- More provider categories
- More education contexts
- International organizations
- Corporate learning
- Professional certification
- Public learning broadcasts
- Large-scale webinars
- Enterprise packages
- Institutional subscriptions
- Advanced provider analytics
- Advanced learning intelligence
- Additional payment providers
- Additional communication channels
- Advanced data governance
- Advanced observability

Expansion must follow the same principle:

«Extend the shared platform foundation rather than creating disconnected products.»

---

113. FINAL PLATFORM ADMIN MAP

                         ELMKUSOMA
                             │
                     PLATFORM ADMIN
                             │
 ┌──────────────┬────────────┼─────────────┬───────────────┐
 ↓              ↓            ↓             ↓               ↓
IDENTITY    ORGANIZATIONS  SERVICES     COMMERCE       GOVERNANCE
 ↓              ↓            ↓             ↓               ↓
Users       Institutions   Courses      Billing         Security
Roles       Providers      Live         Payments        Audit
Admins      Organizations  Events       Packages        Policies
Delegation  Verification   Media        Entitlements    Compliance
Permissions Teams          Resources    Sponsorship     Moderation
                             ↓
                         LEARNERS
                             ↓
                 Learn / Join / Consume
                             ↓
              Progress / Attendance / Evidence
                             ↓
                        Certificates
                             ↓
                        INTELLIGENCE
                             ↓
                      PLATFORM ADMIN

---

114. MASTER IMPLEMENTATION PRINCIPLE

The Platform Admin implementation must follow:

«Do not build what already exists.
Do not replace what already works.
Do not duplicate what can be shared.
Do not expose what should be protected.
Do not display what cannot be verified.
Do not mark complete what has not been tested.»

---

115. REFERENCE STATUS

This document is the conceptual master reference for Platform Admin implementation.

Before coding:

READ THIS DOCUMENT
       ↓
INSPECT REPOSITORY
       ↓
MAP EXISTING IMPLEMENTATION
       ↓
IDENTIFY GAPS
       ↓
UPDATE IMPLEMENTATION PLAN
       ↓
IMPLEMENT PHASE
       ↓
VERIFY
       ↓
UPDATE THIS DOCUMENT

The document should remain a living implementation reference and should be updated when verified architectural decisions materially change.

---

116. NEXT REQUIRED ARTIFACTS

After this document is reviewed and accepted, create:

Artifact 1

Platform Admin Repository Audit Prompt

Purpose:

«Inspect the current ELMKUSOMA repository and compare actual implementation against this document.»

Output:

- Existing capabilities
- Missing capabilities
- Partial capabilities
- Broken capabilities
- Duplicate systems
- Security gaps
- Database gaps
- API gaps
- Frontend gaps
- Integration gaps
- Dependency map
- Recommended implementation order

Artifact 2

Platform Admin Implementation Prompt

Purpose:

«Implement the approved roadmap phase-by-phase while treating this document as the authoritative reference.»

Artifact 3

Platform Admin Completion & Verification Report

Purpose:

«Calculate actual requirement-level completion and verify the complete system.»

---

117. FINAL PRINCIPLE

ELMKUSOMA Platform Admin should ultimately become:

«The control plane of the ELMKUSOMA ecosystem — governing identity, institutions, providers, services, learning, live experiences, commerce, access, trust, security, operations, data and platform intelligence from one secure, auditable and scalable foundation.»

It should enable ELMKUSOMA to grow from an education platform into a broader:

«Learning, Education, Professional Development, Content, Events and Digital Learning Services Ecosystem»

without losing architectural discipline, security, ownership boundaries, or trust.
