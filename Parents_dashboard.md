
ELMKUSOMA

PARENT DASHBOARD & FAMILY LEARNING EXPERIENCE

Product & Implementation Specification

Document Type: Parent Experience & Implementation Specification
Platform: ELMKUSOMA Education Platform
Audience: Product Team, Backend Developers, Frontend Developers, UI/UX Team, QA
Status: Implementation Blueprint
Scope: Parent/Guardian Experience
Priority: Core Platform Capability

---

1. DOCUMENT PURPOSE

This document defines the complete Parent Dashboard and Family Learning Experience for ELMKUSOMA.

The Parent Dashboard must not be implemented as a simple page showing marks, attendance and payments.

It should function as a:

«Family Learning & Engagement Center»

The parent should be able to understand the child's learning journey, monitor progress, receive meaningful alerts, communicate with authorized teachers, support learning at home, access relevant educational services, manage payments where applicable, and interact with the wider ELMKUSOMA ecosystem.

The implementation must use the existing ELMKUSOMA architecture.

DO NOT rebuild ELMKUSOMA from scratch.

DO NOT introduce a new technology stack.

DO NOT create duplicate APIs, models, dashboards or business logic where equivalent functionality already exists.

The existing backend remains the authoritative source of educational data.

---

2. CORE PRODUCT VISION

The Parent Experience should answer five questions immediately:

1. How is my child doing?

2. What is my child learning?

3. What requires my attention?

4. How can I support my child?

5. What can I do next?

The experience should therefore follow:

                    PARENT
                       |
                       v
              +------------------+
              |   FAMILY HOME    |
              +---------+--------+
                        |
        +---------------+---------------+
        |               |               |
        v               v               v
    CHILDREN        LEARNING        SCHOOL LIFE
        |               |               |
        v               v               v
   Progress        Courses        Services
   Attendance      Lessons       Calendar
   Performance     Live Classes  Communication
        |               |               |
        +---------------+---------------+
                        |
                        v
              LEARNING INTELLIGENCE
                        |
             +----------+----------+
             |          |          |
             v          v          v
          Doing      Attention    Next
           Well        Needed     Action
             |          |          |
             +----------+----------+
                        |
                        v
                PARENT ACTION
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
       Support       Contact       Learn
       Child         Teacher      Together

---

3. DESIGN PRINCIPLES

The Parent Dashboard must follow these principles.

3.1 Real Data Only

Never display fabricated:

- statistics
- progress
- attendance
- grades
- payments
- achievements
- activity
- recommendations

If data is unavailable, show an appropriate empty state.

Example:

Attendance data is not available yet.

Not:

Attendance: 94%

when no real attendance exists.

---

3.2 Parent-Specific Experience

The Parent Dashboard must not be a copy of the Learner Dashboard.

Learner

What do I need to learn and do?

Teacher

What do I need to teach and manage?

Parent

How is my child doing, what needs attention,
and how can I support the child?

---

3.3 Context-Aware

The dashboard must adapt to:

- number of children
- child's school
- child's class
- child's courses
- available services
- role permissions
- active enrollments
- institution configuration
- available data

Do not display modules that are not applicable.

---

3.4 Action-Oriented

Every important insight should have an action.

Example:

Mathematics performance has declined.

[View Details]

or:

2 assignments are overdue.

[View Assignments]

or:

Teacher feedback is available.

[View Feedback]

---

4. INFORMATION ARCHITECTURE

The Parent Dashboard should follow this structure:

PARENT DASHBOARD
|
+-- Home
|
+-- My Children
|   |
|   +-- Child Overview
|   +-- Learning Progress
|   +-- Academic Performance
|   +-- Attendance
|   +-- Assignments
|   +-- Assessments
|   +-- Live Learning
|   +-- Achievements
|   +-- Reports
|
+-- Learning
|   |
|   +-- Courses
|   +-- Lessons
|   +-- Learning Activity
|   +-- Learning Library
|   +-- Learn Together
|   +-- Goals
|
+-- Live Learning
|
+-- Calendar
|
+-- Teachers & Communication
|
+-- Notifications
|
+-- Family Services
|   |
|   +-- School Services
|   +-- Transport
|   +-- Library
|   +-- Clubs & Activities
|   +-- Events
|
+-- Payments
|
+-- Reports
|
+-- Support
|
+-- Settings

The actual navigation must remain role-aware and should only expose modules available to the parent.

---

5. PARENT HOME

The Home page is the most important screen.

It should provide a concise picture of the family learning situation.

5.1 Header

Example:

Good afternoon, Mama Amani

Here's how your children are doing today.

The header should support:

- parent name
- profile
- notifications
- language
- settings

---

6. CHILD SELECTOR

Parents may have more than one child.

The system must support:

My Children

+-----------------------+
| Amani John            |
| Grade 8               |
| ABC School            |
+-----------------------+

+-----------------------+
| Neema John            |
| Grade 5               |
| XYZ School            |
+-----------------------+

The parent can switch between children.

The selected child becomes the context for:

- progress
- attendance
- assessments
- courses
- assignments
- live classes
- reports
- teacher communication
- achievements
- learning activity

---

7. MULTIPLE CHILDREN SUPPORT

The system must support:

Parent
 |
 +-- Child A
 |
 +-- Child B
 |
 +-- Child C

Each child must have an independent data context.

The backend must enforce authorization.

A parent must never access another learner by manually changing:

- learner ID
- URL
- API request
- query parameter

---

8. CHILD OVERVIEW

The selected child should have an overview card.

Example:

AMANI JOHN
Grade 8 • ABC School

Learning Progress       Attendance
78%                     94%

Assignments             Assessments
3 Pending               82%

Live Classes            Courses
2 Upcoming              6 Active

Every metric must come from real data.

---

9. WEEKLY LEARNING SUMMARY

The dashboard should provide a weekly summary.

Example:

THIS WEEK

5 lessons completed
2 assignments submitted
3 live classes attended
1 assessment completed

This should provide a quick understanding of the child's learning activity.

---

10. LEARNING PROGRESS

Parents should see what the learner is studying.

Example:

LEARNING PROGRESS

Mathematics
82% complete
Current topic: Algebraic Expressions

English
64% complete
Current topic: Reading Comprehension

Science
91% complete
Current topic: Cells

Each course may expose:

- course progress
- current lesson
- completed lessons
- pending lessons
- last activity
- next lesson
- course status

---

11. SUBJECT PERFORMANCE

Performance should be presented by subject/course.

Example:

SUBJECT PERFORMANCE

Mathematics       78%
Science           86%
English           74%
History           81%

The parent should be able to open detailed performance.

---

12. PERFORMANCE TRENDS

The system should not only show the latest grade.

It should show trends.

Example:

MATHEMATICS PERFORMANCE

90 |
85 |                 *
80 |           *
75 |      *
70 |  *
   +-------------------------
      Test1 Test2 Test3 Test4

Possible interpretation:

Improving
Stable
Declining
No sufficient data

These classifications must be based on actual available data.

---

13. ASSESSMENTS

Parents should be able to view:

- upcoming assessments
- completed assessments
- scores
- grades
- assessment dates
- subject
- feedback where authorized
- performance trends

Example:

Recent Assessments

Mathematics
Score: 78%
Date: 15 Sept

Science
Score: 86%
Date: 12 Sept

---

14. ATTENDANCE

Parent attendance view should include:

- present
- absent
- late
- excused
- attendance percentage where meaningful
- date
- class/session
- attendance trend

Example:

ATTENDANCE

Present       22
Absent         1
Late           2
Excused        1

The system must not automatically treat attendance as academic performance.

---

15. ENGAGEMENT

Engagement may include:

- lessons completed
- assignments submitted
- live classes attended
- recent learning activity
- course activity
- learning sessions

Do not present engagement as equivalent to academic achievement.

---

16. ASSIGNMENTS

Parents should be able to see:

ASSIGNMENTS

Due Soon
- Science Project
- Mathematics Practice

Overdue
- English Essay

Completed
- History Assignment

Each assignment may include:

- title
- subject/course
- due date
- status
- submission status
- grade where available
- teacher feedback where authorized

---

17. UPCOMING ACTIVITIES

A dedicated upcoming section should include:

- assignments
- assessments
- live classes
- school events
- parent meetings
- deadlines

Example:

UPCOMING

Today
18:00 — Mathematics Live Class

Tomorrow
Science Assignment Due

Friday
Mathematics Assessment

Saturday
Parent–Teacher Meeting

---

18. LIVE LEARNING

Live Classes are a core ELMKUSOMA capability.

Parent experience should include:

LIVE LEARNING

Upcoming
Mathematics
Today • 18:00
Teacher: Mr. John

[View Class]

After a class:

COMPLETED

Mathematics
15 Sept • 18:00

Attendance: Present
Recording: Available
Materials: Available

The parent should see only information they are authorized to access.

---

19. LIVE CLASS ACCESS STATUS

Where payment/subscription is required, the parent may see:

Access Available

or:

Access Required

[View Package]

or:

Payment Pending

This must connect to the authoritative entitlement/payment system.

---

20. LEARNING ACTIVITY

The parent should have an activity timeline.

Example:

RECENT LEARNING ACTIVITY

Today
✓ Completed Mathematics Lesson

Today
✓ Submitted Science Assignment

Yesterday
✓ Attended Physics Live Class

12 Sept
✓ Completed English Quiz

---

21. "NEEDS YOUR ATTENTION"

This is one of the most important Parent Intelligence features.

Example:

NEEDS YOUR ATTENTION

⚠ Mathematics performance has declined
   [View Details]

⚠ 2 assignments are overdue
   [View Assignments]

ℹ Teacher feedback is available
   [View Feedback]

The system must avoid unsupported conclusions.

Do not say:

Your child is failing.

unless the underlying system has a formally defined and appropriate status.

Prefer:

Mathematics assessment performance has declined
across the latest available assessments.

---

22. "DOING WELL"

The dashboard should also surface positive activity.

Example:

DOING WELL

✓ Science course progress increased

✓ 4 consecutive learning sessions completed

✓ Mathematics assignment submitted

✓ Course milestone completed

The dashboard should provide a balanced view rather than only problems.

---

23. PARENT ACTIONS

Insights should result in actions.

Possible actions:

View Details
Review Assignment
Contact Teacher
Open Course
View Report
Join/Manage Service
View Payment
View Recording

---

24. PARENT SUPPORT FOR LEARNING

Parents should receive guidance where appropriate.

Example:

HOW YOU CAN HELP

Mathematics

Current focus:
Algebra

You can help by:

• Asking your child to explain today's concept
• Reviewing the assigned practice
• Encouraging completion before Friday

This guidance should not replace the teacher.

---

25. LEARN TOGETHER

ELMKUSOMA should provide a family learning experience.

Example:

LEARN TOGETHER

Recommended for Amani

📘 Algebra Basics
🎥 Introduction to Cells
📝 Grade 8 Revision
🎯 Exam Preparation

Parents may access appropriate learning resources intended for family support.

---

26. FAMILY LEARNING LIBRARY

Library content may include:

- notes
- videos
- revision materials
- practice questions
- study guides
- educational resources
- parent guidance resources

Content access must respect:

- permissions
- course access
- institution settings
- subscription/entitlement
- content visibility

---

27. LEARNING GOALS

Parents should be able to view learner goals.

Example:

AMANI'S LEARNING GOALS

✓ Complete Mathematics Unit 3

✓ Submit Science Assignment

○ Improve Mathematics Assessment

○ Complete Revision Module

Where the platform supports it, parents may be able to encourage/support goals without directly modifying learner academic records.

---

28. ACHIEVEMENTS

Possible achievements:

- course completion
- certificate
- learning milestone
- assessment improvement
- participation milestone
- consistent learning activity

Example:

ACHIEVEMENTS

🏆 Mathematics Course Completed

🌟 10 Learning Sessions Completed

📜 Certificate Earned

Achievements must always be generated from real system events.

---

29. LEARNING MOMENTS

Where the institution enables it and privacy/consent requirements are satisfied:

LEARNING MOMENTS

📷 Science Practical
🎥 Class Presentation
📄 Project Submission
🏆 Academic Achievement

This feature must have strict privacy controls.

---

30. FAMILY EDUCATION CALENDAR

The parent should have an education calendar.

Example:

SEPTEMBER

18
Mathematics Live Class

19
Science Assignment Due

21
Mathematics Assessment

24
Parent–Teacher Meeting

28
Term Report Available

Calendar events may include:

- classes
- live classes
- assessments
- assignments
- meetings
- school events
- deadlines
- reports

---

31. PARENT–TEACHER COMMUNICATION

Communication must be contextual.

Example:

MATHEMATICS
Teacher: Mr. John

Teacher Feedback:

"Amani understands the basic concept
but needs more practice."

[Reply]

Parents should only communicate with authorized teachers/staff connected to their child.

---

32. TEACHER DIRECTORY

The parent may see relevant teachers:

MY CHILD'S TEACHERS

Mathematics
Mr. John

Science
Ms. Anna

English
Mr. Peter

Available actions:

View Profile
Message Teacher
View Class

Only authorized information should be displayed.

---

33. NOTIFICATIONS

Notification categories:

🔴 Action Required
🟡 Attention
🟢 Positive
🔵 Information

Examples:

Action Required
Science assignment is overdue.

Attention
Attendance pattern changed.

Positive
Amani completed a course.

Information
School announcement available.

Avoid notification overload.

---

34. SMART NOTIFICATIONS

Notifications should be generated from meaningful events.

Examples:

- assignment due
- assignment overdue
- assessment result available
- attendance event
- live class reminder
- teacher message
- report available
- payment status
- school announcement
- course milestone

---

35. WEEKLY FAMILY BRIEF

ELMKUSOMA should support a weekly family summary.

Example:

AMANI'S WEEK

Learning
↑ Improving

Attendance
✓ Good

Assignments
4 / 5 completed

Assessments
82%

HIGHLIGHTS
Science assignment completed.

FOCUS NEXT WEEK
Mathematics revision.

UPCOMING
Mathematics assessment.

This may eventually be delivered through:

- dashboard
- in-app notification
- email
- SMS
- other supported channels

Channel availability depends on platform configuration and integrations.

---

36. REPORTS

Parents should be able to access reports such as:

- academic reports
- progress reports
- attendance reports
- assessment reports
- learning activity reports
- certificates
- institution reports where authorized

Actions:

View
Download
Print

Where supported.

---

37. SCHOOL SERVICES

The Parent Dashboard should expose services activated by the relevant institution.

Possible services:

School Services

+-- Transport
+-- Library
+-- Clubs
+-- Activities
+-- Events
+-- Boarding/Dormitory
+-- School Announcements
+-- Parent Meetings

Do not show services that are not configured for the institution.

---

38. TRANSPORT SERVICE

If enabled:

Parent may view:

- transport status
- route
- pickup information
- drop-off information
- relevant alerts

Any real-time location feature must only be implemented with appropriate privacy, authorization and infrastructure.

---

39. LIBRARY SERVICE

If enabled:

Parent may see relevant child activity such as:

- borrowed resources
- due dates
- available digital resources
- library notifications

---

40. CLUBS & ACTIVITIES

If enabled:

- clubs
- extracurricular activities
- participation
- upcoming events
- activity schedules

---

41. SCHOOL EVENTS

Parent may see:

- school calendar
- meetings
- academic events
- competitions
- workshops
- important announcements

---

42. PAYMENTS

The Family Payment Center should support where applicable:

PAYMENTS

Outstanding
TSh 120,000

Paid This Term
TSh 450,000

[Pay Now]

Payment history:

✓ 01 Sept — TSh 150,000
✓ 01 Aug — TSh 200,000
✓ 01 Jul — TSh 100,000

---

43. PAYMENT FEATURES

Where supported:

- payment initiation
- payment verification
- payment status
- pending transactions
- failed transactions
- receipts
- transaction history
- package/subscription access
- live-class payment
- service payments
- entitlement activation

Payment logic must remain authoritative in the backend.

---

44. PAYMENT SECURITY

Never trust the frontend to determine:

Payment successful

or:

Access granted

Payment confirmation and service entitlement must be verified server-side.

---

45. SUPPORT CENTER

Parent should have:

NEED HELP?

What do you need help with?

○ Learning
○ Payment
○ Live Class
○ Technical Problem
○ Teacher Communication
○ Account
○ Other

Requests should be routed to the appropriate support/admin workflow.

---

46. FAMILY ACCOUNT

Parent settings may include:

- profile
- contact information
- language
- notification preferences
- password/security
- linked children
- communication preferences
- payment preferences where supported

---

47. LANGUAGE

ELMKUSOMA should support:

English
Kiswahili

Parent-facing labels, messages and guidance should support localization.

Translations must not alter the meaning of academic data.

---

48. MOBILE EXPERIENCE

The Parent Dashboard must be mobile-first.

Recommended mobile navigation:

+--------------------------------+
| ELMKUSOMA        🔔   👤       |
+--------------------------------+

Good afternoon, Mama Amani

Amani John
Grade 8 • ABC School

Learning       Attendance
78%            94%

-------------------------------

NEEDS YOUR ATTENTION

⚠ Mathematics performance
⚠ 2 overdue assignments

-------------------------------

UPCOMING

Mathematics Live Class
Today • 18:00

-------------------------------

THIS WEEK

5 lessons
2 assignments
3 live classes

Bottom navigation:

Home | Child | Learning | Messages | More

---

49. DESKTOP EXPERIENCE

Desktop should provide:

+-----------------------------------------------------+
| ELMKUSOMA                          Notifications User|
+-------------+---------------------------------------+
| Home        | Parent Overview                       |
| Children    |                                       |
| Learning    | Selected Child                        |
| Live        |                                       |
| Calendar    | Progress | Attendance | Performance   |
| Teachers    |                                       |
| Reports     | Needs Attention                       |
| Services    |                                       |
| Payments    | Upcoming                              |
| Support     |                                       |
+-------------+---------------------------------------+

---

50. ACCESSIBILITY

Parent experience must support:

- keyboard navigation
- readable typography
- sufficient contrast
- semantic labels
- accessible buttons
- screen-reader compatibility
- clear focus states
- responsive layouts
- meaningful error messages
- accessible charts
- non-color-only status indicators

---

51. RESPONSIVE DESIGN

The system must support:

Mobile
Tablet
Desktop
Large Desktop

Do not simply shrink the desktop interface.

The information hierarchy must adapt to the screen.

---

52. ROLE & PERMISSION MODEL

Parent access must be server-authorized.

Parent may only access:

Parent
  ↓
Authorized Child
  ↓
Authorized Institution
  ↓
Authorized Learning Data

Do not rely only on frontend route protection.

---

53. SECURITY REQUIREMENTS

The Parent Dashboard must enforce:

- authentication
- authorization
- child relationship validation
- institution relationship validation
- API-level access control
- secure payment data handling
- secure messaging
- audit logging where required
- privacy controls
- secure file/report access

---

54. DATA SOURCES

The dashboard should consume authoritative backend data from existing ELMKUSOMA modules.

Possible sources include:

Users
Parents
Learners
Institutions
Enrollments
Courses
Lessons
Assessments
Submissions
Grades
Attendance
Live Classes
Learning Activity
Notifications
Messages
Reports
Certificates
Payments
Entitlements
School Services

Do not create parallel data stores unless architecturally necessary.

---

55. DATA FLOW

DATABASE
   |
   v
DOMAIN / JPA
   |
   v
SERVICE LAYER
   |
   v
API / CONTROLLER
   |
   v
FRONTEND API CLIENT
   |
   v
PARENT DASHBOARD

For intelligence:

Education Data
      |
      v
Aggregation
      |
      v
Rules / Analytics
      |
      v
Insight
      |
      v
Parent Action

---

56. INTELLIGENCE SAFETY

The system must distinguish:

FACT

from:

INTERPRETATION

Example:

FACT:

Latest Mathematics score: 68%
Previous score: 78%

Possible system insight:

Performance decreased between the two latest assessments.

Avoid unsupported statements such as:

The learner is lazy.

or:

The learner does not understand Mathematics.

---

57. EMPTY STATES

Every module must handle missing data.

Examples:

No assessments yet.

No attendance records available.

No upcoming live classes.

No teacher messages.

No payment history.

No achievements yet.

No reports available.

Empty states should explain what happens next where appropriate.

---

58. ERROR STATES

Examples:

Unable to load attendance.

[Try Again]

Payment:

Payment status could not be verified.

[Check Again]

Live class:

Live class information is temporarily unavailable.

Never show misleading values.

---

59. PERFORMANCE REQUIREMENTS

Parent dashboard should avoid loading every module synchronously if unnecessary.

Prefer:

Dashboard shell
      |
      +-- Overview
      +-- Alerts
      +-- Progress
      +-- Upcoming
      +-- Activity

with efficient API requests.

Use pagination for:

- activity
- notifications
- messages
- payments
- reports
- assignments
- assessments

---

60. API DESIGN

Reuse existing APIs where possible.

If an API does not exist, introduce a domain-appropriate endpoint.

Avoid creating:

/api/parent/dashboard1
/api/parent/dashboard2
/api/parent/dashboard-new

Instead use coherent resources and/or a well-designed parent dashboard aggregation endpoint where justified.

---

61. NO DUPLICATE BUSINESS LOGIC

The Parent Dashboard must not independently calculate:

- grades
- enrollment state
- payment state
- entitlement
- attendance status

when the backend already owns those rules.

The frontend displays authoritative backend results.

---

62. PARENT EXPERIENCE MODULE MAP

                         PARENT
                           |
                           v
                  +----------------+
                  | FAMILY HOME    |
                  +-------+--------+
                          |
       +------------------+------------------+
       |                  |                  |
       v                  v                  v
   CHILDREN           LEARNING           SERVICES
       |                  |                  |
       |          +-------+-------+          |
       |          |       |       |          |
       v          v       v       v          v
   Overview    Courses  Live   Assessments  School
   Progress    Lessons  Class   Results     Services
   Attendance  Activity        Trends      Payments
       |          |       |       |          |
       +----------+-------+-------+----------+
                          |
                          v
                  EDUCATION INSIGHTS
                          |
             +------------+------------+
             |            |            |
             v            v            v
          Positive     Attention    Upcoming
             |            |            |
             +------------+------------+
                          |
                          v
                    PARENT ACTION

---

63. PARENT EXPERIENCE PRIORITY LEVELS

P0 — Core

These are required:

Parent Home
Child Selector
Child Overview
Learning Progress
Academic Performance
Assessments
Attendance
Assignments
Upcoming Activities
Live Learning
Teacher Communication
Notifications
Reports
Authorization
Mobile Experience

P1 — High Value

Needs Attention
Doing Well
Weekly Family Brief
Learn Together
Learning Library
Goals
Achievements
Payments
School Services
Support Center
Family Calendar

P2 — Extended Experience

Learning Moments
Advanced recommendations
Additional family services
Advanced communication channels
Expanded analytics

P2 must not block the core Parent Dashboard.

---

64. DEFINITION OF DONE

The Parent Dashboard is not complete simply because the UI exists.

It is complete when:

✓ Parent can securely log in

✓ Parent sees only authorized children

✓ Child switching works

✓ Dashboard uses real backend data

✓ Learning progress works

✓ Attendance works

✓ Assessments work

✓ Assignments work

✓ Performance trends work

✓ Live learning works

✓ Teacher communication works

✓ Notifications work

✓ Reports work

✓ Payment status is authoritative

✓ Empty states exist

✓ Error states exist

✓ Mobile experience works

✓ Desktop experience works

✓ Accessibility is considered

✓ No mock data remains

✓ No duplicate business logic exists

✓ APIs are properly authorized

✓ Backend remains source of truth

✓ End-to-end workflows are tested

---

65. THREE-DEVELOPER IMPLEMENTATION PLAN

The implementation should be divided into three major workstreams.

The goal is to avoid three developers modifying the same files unnecessarily.

---

DEVELOPER 01 — PARENT EXPERIENCE & FRONTEND

Primary Responsibility

Build the Parent Dashboard user experience and frontend integration.

Work Package

Parent Home
Child Selector
Child Overview
Learning Progress UI
Performance UI
Attendance UI
Assignments UI
Assessments UI
Upcoming Activities
Live Learning UI
Achievements UI
Calendar UI
Reports UI
Notifications UI
Parent Navigation
Responsive Design
Accessibility

Developer 01 must implement

1. Parent dashboard shell

2. Parent navigation

3. Child switching interface

4. Overview cards

5. Progress components

6. Assessment components

7. Attendance components

8. Assignment components

9. Performance/trend visualizations

10. Upcoming activities

11. Live learning interface

12. Reports interface

13. Achievement interface

14. Mobile layout

15. Desktop layout

16. Loading states

17. Empty states

18. Error states

19. Accessibility

20. Frontend API integration

Developer 01 must NOT

- create fake backend data
- bypass authorization
- implement payment verification
- duplicate business rules
- create independent academic calculations

---

DEVELOPER 02 — PARENT BACKEND, DATA & SECURITY

Primary Responsibility

Ensure the backend can securely provide all Parent Dashboard data.

Work Package

Parent APIs
Parent-child relationship
Authorization
Dashboard aggregation
Learning progress
Attendance
Assessments
Assignments
Performance
Live class access
Notifications
Teacher communication
Reports
Parent-specific backend services

Developer 02 must implement/verify

1. Parent authorization

2. Parent-child relationship validation

3. Parent dashboard APIs

4. Child switching API support

5. Learning progress aggregation

6. Attendance retrieval

7. Assessment retrieval

8. Assignment retrieval

9. Performance aggregation

10. Upcoming activity aggregation

11. Live class access

12. Teacher relationship validation

13. Parent messaging authorization

14. Notification retrieval

15. Report authorization

16. Audit/security controls where required

Developer 02 must NOT

- create frontend UI
- duplicate existing domain logic
- bypass existing security architecture
- create fake records

---

DEVELOPER 03 — SERVICES, PAYMENTS, INTELLIGENCE & INTEGRATION

Primary Responsibility

Implement advanced Parent Experience services and connect them to the wider ELMKUSOMA ecosystem.

Work Package

Parent Intelligence
Needs Attention
Doing Well
Weekly Family Brief
Learn Together
Learning Library
Payments
Entitlements
School Services
Support
Advanced Notifications
Cross-module Integration

Developer 03 must implement/verify

1. Needs Attention logic

2. Positive learning signals

3. Weekly family summary

4. Learning support recommendations

5. Family learning resources

6. Payment status integration

7. Payment entitlement integration

8. Live-class access entitlement

9. School services integration

10. Family calendar integration

11. Advanced notification events

12. Parent support workflows

13. Cross-module integration testing

Developer 03 must NOT

- create independent payment truth
- bypass payment verification
- expose unauthorized learner information
- create unsupported AI conclusions
- create fake intelligence

---

66. TEAM OWNERSHIP MODEL

                    PARENT EXPERIENCE
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     DEVELOPER 01     DEVELOPER 02     DEVELOPER 03
          |                |                |
       FRONTEND         BACKEND          SERVICES
          |                |                |
          v                v                v
       UX/UI           DATA/API        INTELLIGENCE
       Mobile          Security        Payments
       Desktop         Relations       Integration

---

67. SHARED CONTRACT BETWEEN DEVELOPERS

All three developers must agree on:

Parent Role
Child Relationship
API Contracts
Data Models
Permissions
Error Format
Pagination
Loading States
Status Values
Naming Conventions

Before implementation, Developer 02 should document required API contracts for Developer 01.

Developer 03 should consume existing domain APIs where possible instead of duplicating them.

---

68. IMPLEMENTATION ORDER

Recommended order:

PHASE 1
Parent Authorization
        ↓
Parent-Child Relationship
        ↓
Parent Dashboard API
        ↓
Parent Home UI

PHASE 2
Learning
Attendance
Assessments
Assignments
Performance
Upcoming Activities

PHASE 3
Live Learning
Teacher Communication
Notifications
Reports
Calendar

PHASE 4
Parent Intelligence
Needs Attention
Doing Well
Weekly Family Brief
Learning Support

PHASE 5
Payments
Entitlements
School Services
Support
Family Learning

PHASE 6
Integration Testing
Security Testing
Responsive Testing
Accessibility Testing
Performance Testing
Production Hardening

---

69. CROSS-DEVELOPER DEPENDENCIES

Developer 02
Backend/API
      |
      v
Developer 01
Frontend
      |
      v
Integrated Parent Experience
      ^
      |
Developer 03
Services + Intelligence

Developer 01 should not wait for every advanced service before building the core UI.

Use clearly defined API contracts.

---

70. TESTING REQUIREMENTS

Functional

Test:

Parent login
Child switching
Dashboard loading
Progress
Attendance
Assessments
Assignments
Live classes
Messages
Notifications
Reports
Payments
Services

Authorization

Test:

Parent A cannot access Child B.

Parent cannot access unrelated teacher.

Parent cannot access unrelated institution.

Parent cannot modify learner academic records.

Parent cannot fake payment success.

Parent cannot access restricted reports.

Responsive

Test:

Mobile
Tablet
Desktop

Accessibility

Test:

Keyboard
Screen reader
Contrast
Focus
Labels
Error messages

---

71. FINAL PRODUCT EXPERIENCE

The final Parent experience should feel like:

                    ELMKUSOMA FAMILY
                           |
                           v
                  "How is my child?"
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
       LEARNING        WELLBEING*        SCHOOL
          |                |                |
          v                v                v
      Progress         Attendance        Events
      Courses          Engagement        Teachers
      Assessments      Activity          Services
      Live Classes
          |
          v
                    INSIGHTS
          |
     +----+----+
     |         |
     v         v
 Doing Well  Attention
     |         |
     +----+----+
          |
          v
       ACTION
          |
   +------+------+------+
   |      |      |      |
   v      v      v      v
Support Teacher Learn  Review
Child   Contact Together Report

*Any wellbeing-related information must be limited to data the platform is actually designed and authorized to collect; do not infer sensitive personal or health information.

---

72. FINAL SUCCESS CRITERIA

The Parent Dashboard should make a parent feel:

"I understand my child's learning."

"I know what is going well."

"I know what needs attention."

"I know what is coming next."

"I can communicate with the right people."

"I can support learning at home."

"I can access relevant ELMKUSOMA services."

"I can manage relevant payments."

"I can trust the information because it comes
from the real education system."

The objective is not to create the largest dashboard.

The objective is to create a clear, trustworthy, useful and engaging family education experience.

---

73. FINAL IMPLEMENTATION RULE

Before adding any new Parent Dashboard feature, the team must ask:

1. Does it serve a real parent need?

2. Does the backend have authoritative data for it?

3. Is the parent authorized to see it?

4. Does it connect to an existing ELMKUSOMA capability?

5. Does it improve the child's learning/support experience?

6. Can it be implemented without duplicating existing logic?

7. Does it work on mobile?

8. Does it handle empty/error states?

9. Does it preserve privacy and security?

10. Can the feature be measured/tested?

If the answer is no, do not add the feature merely to make the dashboard look bigger.

---

END OF DOCUMENT

ELMKUSOMA — Parent Dashboard & Family Learning Experience

Core principle:

«See → Understand → Support → Engage → Act»
