ELMKUSOMA — DEVELOPER 04

GENERAL / OTHER LEARNER

RESOURCE LIBRARY • KNOWLEDGE GATEWAY • UNIFIED SEARCH • DISCOVERY • SAVED CONTENT • RELATED LEARNING • CERTIFICATES • VERIFICATION

---

0. AUTHORITATIVE MASTER REFERENCES

Use the following as the authoritative references for this implementation:

1. ELMKUSOMA — GENERAL / OTHER LEARNER — MULTI-DEVELOPER PRODUCTION IMPLEMENTATION PACKAGE
2. MASTER COGNITIVE ENGINEERING & SOLUTION PROMPT
3. OPENCODE — EXECUTE + VERIFY + EXACT COMPLETION
4. Existing ELMKUSOMA repository, architecture, database, APIs, security model, UI/UX, components and infrastructure.

You are DEVELOPER 04.

Your implementation must fit into the existing ELMKUSOMA platform.

This is NOT a rebuild.

This is NOT a greenfield project.

This is NOT permission to create parallel systems.

---

1. PRIMARY MISSION

Upgrade and complete ELMKUSOMA's:

- Resource Library
- Knowledge Gateway
- Unified Search
- Search & Discovery
- Related Learning
- Saved / Bookmarked Content
- Resource Preview / Open / Download
- Content Relationships
- Certificate Experience
- Certificate Verification
- Provider/Admin Integration
- D01/D02/D03 integrations

into one coherent, secure, production-ready learning discovery experience.

The learner should be able to move naturally through:

DISCOVER
 ↓
SEARCH
 ↓
FILTER
 ↓
UNDERSTAND
 ↓
PREVIEW
 ↓
OPEN
 ↓
LEARN
 ↓
SAVE
 ↓
CONNECT
 ↓
CONTINUE
 ↓
COMPLETE
 ↓
EVIDENCE
 ↓
CERTIFICATE
 ↓
VERIFY
 ↓
DISCOVER MORE

The result must feel like one ELMKUSOMA ecosystem.

---

2. PRODUCT PRINCIPLE

D04 is NOT merely:

- a PDF library
- a search box
- a bookmark page
- a certificate page

D04 is the Knowledge & Discovery Layer of ELMKUSOMA.

It should connect authorized learning knowledge across:

COURSES
 ↕
PROGRAMS
 ↕
MODULES
 ↕
LESSONS
 ↕
VIDEOS
 ↕
AUDIO
 ↕
LIVE SESSIONS
 ↕
EVENTS
 ↕
REPLAYS
 ↕
RESOURCES
 ↕
PRACTICE
 ↕
PROJECTS
 ↕
RESEARCH MATERIAL
 ↕
CERTIFICATES

Only expose relationships supported by real system data.

NEVER fabricate relationships.

---

3. CORE USER EXPERIENCE

A learner should not need to understand ELMKUSOMA's technical architecture.

The experience must answer:

- What can I learn?
- What is related to what I am learning?
- Can I access it?
- Why is it related?
- Can I preview it?
- Can I save it?
- Can I return later?
- Can I continue learning?
- Can I download it?
- Is this certificate legitimate?
- Can another person verify the certificate?

If a learner needs to understand backend architecture to use a feature, the UX is not good enough.

---

4. HARD ARCHITECTURAL RULE

INSPECT FIRST.

Before creating, modifying or deleting anything, inspect the existing implementation.

Inspect:

- frontend
- backend
- entities
- DTOs
- controllers
- services
- repositories
- APIs
- database
- migrations
- storage
- authorization
- resource models
- media models
- search architecture
- indexing
- bookmarks
- saved content
- certificates
- verification
- QR
- notifications
- providers
- institutions
- events
- LiveSession
- recordings
- course relationships
- enrollment
- learning progress
- existing routes
- navigation
- UI components

Do not assume something is missing because it is not obvious from one file.

Search the repository.

Trace references.

Trace API calls.

Trace database relationships.

Trace frontend routes.

Trace authorization.

---

5. AUDIT CLASSIFICATION

Every relevant capability must be classified as:

EXISTS
PARTIAL
BROKEN
MISSING
DUPLICATED
INCONSISTENT
NEEDS IMPROVEMENT

For each item document:

Component
Current implementation
Source files
Database source
API source
Owner
Dependencies
Current behavior
Expected behavior
Gap
Risk
Required modification

Do not claim MISSING without repository evidence.

---

6. EVIDENCE CATEGORIES

For all reasoning use:

REQUIREMENT
FACT
VERIFIED
ASSUMPTION
INFERENCE

Do not present assumptions as facts.

Do not invent:

- tables
- APIs
- routes
- entities
- fields
- providers
- certificate rules
- relationships
- search behavior
- resource types
- analytics
- permissions

---

7. DOMAIN OWNERSHIP

D04 owns:

RESOURCE LIBRARY
KNOWLEDGE GATEWAY
UNIFIED SEARCH/DISCOVERY
SAVED DISCOVERY
RELATED CONTENT
CERTIFICATES EXPERIENCE
CERTIFICATE VERIFICATION EXPERIENCE

D04 integrates with but does NOT own:

D01 → Learner Dashboard / Workspace

D02 → Courses / Learning / Enrollment / Progress / Goals

D03 → Events / Live / Streaming / Recordings / Media

Existing infrastructure →
Auth / Users / Institutions / Providers / Notifications / Storage

---

8. D04 MUST NOT CREATE DUPLICATE SYSTEMS

Do NOT create another:

- authentication system
- user system
- institution system
- provider system
- course system
- enrollment system
- learning progress system
- LiveKit system
- WebRTC system
- live session system
- event system
- recording system
- media system
- notification system
- calendar system
- storage system
- bookmark system
- certificate system
- certificate verification system
- search engine

if an existing implementation already provides that capability.

Reuse and extend.

---

9. UNIVERSAL CONTENT IDENTITY

Audit how ELMKUSOMA currently represents:

Course
Program
Module
Lesson
Video
Audio
Live Session
Event
Replay
Resource
Book
PDF
Document
Practice
Project
Research Material
Certificate

The discovery layer should be able to distinguish content types without creating duplicate domain entities.

Conceptually:

CONTENT
 ├── type
 ├── id
 ├── title
 ├── description
 ├── source
 ├── provider
 ├── context
 ├── accessStatus
 ├── availability
 ├── relationships
 └── actions

Use the existing API/domain structure where possible.

---

10. RESOURCE LIBRARY

Where supported, learners should discover:

- books
- PDFs
- notes
- guides
- handouts
- presentations
- manuals
- documents
- educational materials
- other legitimate learning resources already supported by the system

Resource flow:

BROWSE
 ↓
SEARCH
 ↓
FILTER
 ↓
DETAILS
 ↓
PREVIEW
 ↓
OPEN
 ↓
DOWNLOAD IF AUTHORIZED
 ↓
SAVE
 ↓
RELATED LEARNING

Use real resources only.

No fake production resources.

---

11. RESOURCE DETAILS / WORKSPACE

Where supported, resource details should expose:

Title
Description
Resource Type
Provider
Author / Creator
Language
Category
Topic
Learning Context
Level
Course
Module
Lesson
Publication Date
Updated Date
Availability
Access Type

Only show metadata actually supported by the system.

Do not invent metadata.

Actions must be contextual:

[Preview]
[Open]
[Download]
[Save]

depending on actual authorization and availability.

---

12. RESOURCE ACCESS STATUS

Where architecture supports equivalent states, correctly represent:

AVAILABLE
ENROLLMENT_REQUIRED
REGISTRATION_REQUIRED
RESTRICTED
NOT_YET_AVAILABLE
EXPIRED
ARCHIVED
REMOVED
UNAUTHORIZED

Never show an action that the backend will reject unless the UI is intentionally communicating restricted access.

---

13. RESOURCE PREVIEW SECURITY

Preview is an access-controlled operation.

Test:

Authorized learner
→ preview
✓

Unauthorized learner
→ preview
✗

Also test:

Unauthorized learner
→ direct preview URL
✗

Unauthorized learner
→ direct file URL
✗

Unauthorized learner
→ modified resourceId
✗

---

14. DOWNLOAD SECURITY

Download flow must be:

CLICK DOWNLOAD
 ↓
AUTHENTICATION
 ↓
AUTHORIZATION
 ↓
RESOURCE OWNERSHIP / SCOPE
 ↓
ELIGIBILITY
 ↓
RESOURCE STATUS
 ↓
CONTROLLED FILE ACCESS
 ↓
DOWNLOAD

Never rely on frontend filtering.

Where existing architecture uses signed URLs, controlled storage endpoints or equivalent mechanisms, reuse them.

Never expose storage credentials.

---

15. FILE STORAGE AUDIT

Inspect:

- storage provider
- storage paths
- buckets/directories
- object ownership
- signed URLs
- URL expiry
- private/public resources
- access controls
- file metadata
- deleted files
- orphan files
- preview assets
- download endpoints

Verify that storage itself cannot bypass application authorization.

---

16. RESOURCE LIFECYCLE

Where supported, handle:

DRAFT
 ↓
PUBLISHED
 ↓
AVAILABLE
 ↓
UPDATED
 ↓
ARCHIVED
 ↓
REMOVED

Search, saved content and discovery must respect resource lifecycle.

A removed resource must not remain falsely discoverable as available.

---

17. RESOURCE VERSIONING

Inspect whether resources support versions.

If supported:

VERSION 1
 ↓
VERSION 2
 ↓
VERSION 3

Saved references and relationships must remain valid.

Do not introduce resource versioning merely because it sounds useful.

Only implement if required by existing architecture or an evidence-backed requirement.

---

18. ORPHAN RESOURCE DETECTION

Detect/report cases such as:

Resource exists
BUT
Parent course deleted
Provider removed
File missing
Relationship broken

Do not silently display broken content.

---

19. BROKEN RELATIONSHIP DETECTION

Examples:

Course
 ↓
Resource
 ↓
resource no longer exists

or:

Event
 ↓
Replay
 ↓
Recording no longer exists

The UI must not produce dead links.

---

20. UNIFIED SEARCH

Reuse the existing ELMKUSOMA search architecture.

Do NOT create a second search engine.

Where supported, search across:

Courses
Programs
Modules
Lessons
Videos
Audio
Live Sessions
Events
Seminars
Workshops
Recordings
Replays
Resources
Books
PDFs
Notes
Guides
Manuals
Learning Materials
Projects
Research Materials
Certificates

Only index/search content that is actually supported by existing architecture.

---

21. GLOBAL SEARCH VS CONTEXT SEARCH

Audit existing search scopes.

Where supported, provide logical views such as:

GLOBAL SEARCH
→ ELMKUSOMA ecosystem

COURSE SEARCH
→ current course

RESOURCE SEARCH
→ resources

MY LEARNING SEARCH
→ learner-authorized learning

MY SAVED SEARCH
→ saved content

These should remain views/scopes over shared search infrastructure, NOT independent search engines.

---

22. SEARCH RESULT STRUCTURE

Search results should expose enough information to understand the result:

Title
Content Type
Provider
Learning Context
Description
Availability
Access Status
Relationship / relevance
Available Actions

Example actions:

Course → View Course

Resource → Preview / Open / Save

Live/Event → View / Register

Replay → Watch

Certificate → View

Restricted content → Access Restricted

Actions must reflect real backend state.

---

23. SEARCH FILTERS

Where supported, filters may include:

Content Type
Category
Provider
Learning Context
Language
Date
Availability
Free/Paid
Level
Course
Program

Do not invent filters for fields that do not exist.

Audit the existing schema first.

---

24. SEARCH STATES

Implement complete:

IDLE
LOADING
RESULTS
NO RESULTS
FILTERED NO RESULTS
ERROR
UNAUTHORIZED
PARTIAL / DEGRADED

No blank screens.

No fake results.

No fake counts.

---

25. SEARCH PAGINATION / PERFORMANCE

Inspect and optimize:

- pagination
- sorting
- filtering
- database indexing
- query efficiency
- N+1 queries
- expensive joins
- duplicate queries
- large result sets
- concurrent searches
- search timeout
- caching where appropriate

Do NOT introduce Elasticsearch/OpenSearch/another search infrastructure automatically.

Only do so if the existing architecture and evidence require it.

---

26. SEARCH RELEVANCE

Audit existing ranking.

Where supported, relevance may consider:

Text relevance
Content type
Context
Relationships
Availability
Recency

Do not introduce arbitrary or opaque ranking.

Do not call something “AI-powered” unless the repository contains such functionality.

---

27. SEARCH SUGGESTIONS

Where supported:

net...
 ↓
network
network security
network architecture
network administration

Suggestions must be based on actual searchable data.

---

28. QUERY TOLERANCE

Where supported, handle reasonable spelling mistakes.

Example:

cyber secrity
 ↓
Did you mean:
cyber security?

Do not fabricate correction suggestions.

---

29. SEARCH HISTORY

Audit whether search history already exists.

If it exists, integrate it.

Where appropriate:

Recent Searches
Clear Search History

Respect privacy and account boundaries.

Do not create a second search-history system.

---

30. SEARCH INDEX LIFECYCLE

If indexing exists:

CREATE
 ↓
PUBLISH
 ↓
INDEX
 ↓
SEARCHABLE

Updates:

UPDATE
 ↓
RE-INDEX

Removal:

UNPUBLISH / DELETE
 ↓
REMOVE FROM SEARCH/DISCOVERY

Verify stale records cannot expose inaccessible content.

---

31. AUTHORIZATION-AWARE SEARCH

Search MUST respect:

USER
 ↓
ROLE
 ↓
INSTITUTION
 ↓
PROVIDER
 ↓
ENROLLMENT
 ↓
RESOURCE ACCESS
 ↓
SEARCH RESULTS

The search engine/query layer must not become a security bypass.

Test:

Private resource
→ must not appear to unauthorized learner

Restricted event
→ must not expose restricted details

Private certificate
→ must not appear publicly

---

32. KNOWLEDGE DISCOVERY

D04 should connect learning knowledge.

Conceptual model:

CURRENT LEARNING
 ↓
RELATED KNOWLEDGE
 ↓
RELATED CONTENT
 ↓
NEXT LEARNING ACTION

Examples:

Course
 ↕
Lesson
 ↕
Resource
 ↕
Video
 ↕
Live
 ↕
Replay
 ↕
Practice
 ↕
Project
 ↕
Certificate

Only expose relationships backed by real data.

---

33. CONTENT RELATIONSHIP GRAPH

Audit existing relationships.

Where supported, identify:

belongs-to
related-to
prerequisite-of
follows
replay-of
resource-for
supports
produced-by
part-of
evidence-for

Do not create duplicate relationship systems if existing relations already exist.

---

34. RELATED CONTENT

Where real relationships exist, expose:

Related Courses
Related Lessons
Related Videos
Related Resources
Related Events
Related Live Sessions
Related Replays
Related Practice
Related Projects
Related Research

Each related item must explain the relationship where useful.

Examples:

Related to your current course

Connected to this lesson

From your enrolled program

Related to this event

Replay of this live session

Do not use generic “Recommended for you” unless there is actual recommendation logic supporting it.

---

35. “EXPLORE MORE”

Every major content page should avoid dead ends.

Examples:

RESOURCE
 ↓
Related Course
 ↓
Continue Learning

REPLAY
 ↓
Related Resource
 ↓
Practice

COURSE
 ↓
Related Resources
 ↓
Live
 ↓
Replay

CERTIFICATE
 ↓
View
 ↓
Download
 ↓
Verify
 ↓
Related Learning

---

36. SAVED / BOOKMARKS

Reuse the existing bookmark/saved infrastructure.

Do NOT create another.

Where supported:

SAVE
REMOVE
MY SAVED
OPEN
SEARCH/FILTER
UNAVAILABLE HANDLING
DELETED CONTENT HANDLING

---

37. SAVED CONTENT

Where supported, saved content may contain:

Courses
Videos
Resources
Events
Replays
Research Materials
Other supported content

Do not introduce unsupported categories.

---

38. SAVED ITEM STATES

If content becomes unavailable:

Saved
 ↓
Content unavailable

Show:

This content is no longer available.

Provide useful recovery where possible:

[Back to Saved]
[Find Related Content]

Do not leave broken links.

---

39. CERTIFICATES — INSPECT FIRST

Inspect the existing certificate implementation.

Audit:

- entities
- issuance
- eligibility
- completion evidence
- provider ownership
- course relationship
- event relationship
- certificate files
- verification
- QR
- public verification
- revocation
- expiration
- notifications
- permissions
- APIs
- frontend
- database
- migrations

Do NOT create a second formal learner certificate system.

---

40. CERTIFICATE EXPERIENCE

Where legitimately supported:

MY CERTIFICATES
 ↓
CERTIFICATE DETAILS
 ↓
VIEW
 ↓
DOWNLOAD
 ↓
VERIFY

Certificate details may include:

Certificate ID
Recipient
Provider / Issuer
Course / Program / Event
Achievement
Issue Date
Status
Verification

Only expose fields actually supported and authorized.

---

41. CERTIFICATE ELIGIBILITY

Certificate eligibility MUST be evidence-based.

Conceptually:

LEARNING / PARTICIPATION
 ↓
REAL EVIDENCE
 ↓
ELIGIBILITY RULES
 ↓
ELIGIBLE
 ↓
ISSUE
 ↓
CERTIFICATE

Possible evidence:

Course completion
Assessment completion
Attendance
Participation
Event completion
Provider approval

Use authoritative evidence from D02/D03/provider systems.

Do not duplicate their evidence engines.

---

42. CERTIFICATE BYPASS PREVENTION

The following must NOT be possible:

User clicks certificate endpoint
 ↓
Certificate generated without eligibility

or:

Modified userId
 ↓
Another learner's certificate

or:

Modified courseId/eventId
 ↓
Unauthorized certificate

---

43. CERTIFICATE OWNERSHIP

Test:

Learner A
→ Learner A certificate
✓

Learner A
→ Learner B certificate
✗

Learner A
→ modified certificateId
✗

Unauthenticated user
→ private certificate endpoint
✗

---

44. PUBLIC CERTIFICATE VERIFICATION

Separate:

PRIVATE LEARNER CERTIFICATE VIEW

from:

PUBLIC CERTIFICATE VERIFICATION

Public verification must reveal only information intentionally permitted by the certificate verification policy.

Never expose private learner account information.

---

45. QR VERIFICATION

If QR verification exists, inspect and harden it.

Test:

Valid certificate
→ VERIFIED

Revoked certificate
→ REVOKED

Unknown certificate
→ NOT FOUND / INVALID

Modified certificate identifier
→ INVALID

Private certificate data
→ MUST NOT LEAK

QR must resolve to authoritative verification.

---

46. CERTIFICATE STATUS

Where supported:

ISSUED
ACTIVE
REVOKED
EXPIRED
INVALID

Do not invent statuses.

Verification must always reflect authoritative current status.

---

47. CERTIFICATE REVOCATION

Where existing architecture supports revocation:

ACTIVE
 ↓
REVOKED

Public verification must immediately or appropriately reflect the authoritative state.

Do not maintain a disconnected verification copy.

---

48. CERTIFICATE DOCUMENT INTEGRITY

If certificates are downloadable documents:

ISSUE
 ↓
GENERATE
 ↓
STORE
 ↓
CONTROL ACCESS
 ↓
DOWNLOAD
 ↓
VERIFY

A modified local copy must not become an authoritative certificate.

---

49. CERTIFICATE IMMUTABILITY

Audit protection of:

- certificate ID
- recipient
- issuer
- achievement
- issue date
- evidence
- verification status

Critical issued-certificate information must not be arbitrarily modified by learners.

---

50. PROVIDER / ADMIN OWNERSHIP

Maintain real relationships:

PROVIDER
 ↓
COURSE / EVENT / RESOURCE
 ↓
LEARNING / PARTICIPATION
 ↓
REAL EVIDENCE
 ↓
ELIGIBILITY
 ↓
CERTIFICATE
 ↓
LEARNER
 ↓
VERIFICATION

Provider/admin publication and ownership rules remain authoritative.

No fake provider data.

---

51. PROVIDER RESOURCE WORKFLOW

Where existing architecture supports:

PROVIDER
 ↓
CREATE RESOURCE
 ↓
DRAFT
 ↓
REVIEW
 ↓
PUBLISH
 ↓
DISCOVERABLE

Reuse existing workflow.

Do not create a second provider content management system.

---

52. MODERATION / REPORTING

If existing moderation/reporting infrastructure exists, integrate it for:

- broken resource
- incorrect metadata
- inaccessible file
- inappropriate content
- incorrect certificate information

Do not create another moderation system.

---

53. D01 INTEGRATION

D01 owns the dashboard.

D04 must expose stable, reusable contracts for:

Resource Cards
Search Results
Discovery Results
Saved Content
Certificates
Related Content

Possible conceptual contract:

DISCOVERY ITEM

id
type
title
description
provider
context
accessStatus
availability
relationship
source
actions
metadata

Reuse existing contracts where possible.

Do NOT force D01 to restructure its dashboard.

---

54. D02 INTEGRATION

Where applicable:

Course ↔ Resource
Module ↔ Resource
Lesson ↔ Resource
Course ↔ Certificate
Completion ↔ Certificate Eligibility
Learning ↔ Related Content

D04 consumes authoritative course/progress/completion information.

D04 does NOT create a second course or progress system.

---

55. D03 INTEGRATION

Where applicable:

Event ↔ Recording
Live Replay ↔ Resource
Media ↔ Resource
Event ↔ Certificate
Recording ↔ Course
Replay ↔ Learning

D04 discovers/exposes these relationships.

D04 does NOT create:

- LiveKit
- WebRTC
- LiveSession
- recording engine
- media processing engine

---

56. NOTIFICATION INTEGRATION

Reuse the existing notification infrastructure.

Relevant events may include:

Certificate Issued
Course Update
Event Registration
Event Reminder
Recording Available
Resource Update
Provider Announcement

Only implement notifications that existing architecture and ownership support.

Do NOT create another notification system.

---

57. CONTENT DISCOVERY CONTINUITY

The learner should be able to move:

SEARCH
 ↓
RESULT
 ↓
DETAILS
 ↓
OPEN
 ↓
RELATED
 ↓
SAVE
 ↓
CONTINUE LEARNING

No dead ends.

---

58. KNOWLEDGE TRAIL

If existing activity infrastructure supports it, preserve continuity such as:

Recent Searches
Recently Opened
Recently Saved
Recently Watched
Recently Viewed

Do not create a new tracking architecture unnecessarily.

---

59. CONTINUE ACROSS CONTENT

Where authoritative progress systems exist:

Continue Reading
Continue Watching
Continue Learning

D04 consumes existing progress state.

D04 must NOT create another learning progress engine.

---

60. CROSS-CONTEXT SUPPORT

D04 must support the shared General / Other Learner foundation.

Potential contexts include:

Adult
Professional
Independent
Skills
Non-formal
Religious Education
Other legitimate learning contexts

Also integrate correctly with:

College / TVET
University

Do not create separate resource/search/certificate systems per context.

---

61. LEARNING-LEVEL AWARENESS

Where actual learner/content data supports it:

Beginner
Intermediate
Advanced
Professional

Do not infer or fabricate learner level.

---

62. LANGUAGE-AWARE DISCOVERY

Audit support for:

English
Kiswahili

and existing multilingual content metadata.

Search and discovery should respect existing localization/content architecture.

Do not create a parallel translation system.

---

63. ACCESSIBILITY

Ensure:

- keyboard navigation
- visible focus
- semantic headings
- screen-reader labels
- accessible filters
- accessible search
- accessible dialogs
- accessible status messages
- readable typography
- sufficient contrast
- accessible buttons
- clear error messages
- mobile usability

Use the existing ELMKUSOMA design system.

---

64. MOBILE-FIRST EXPERIENCE

The experience must work naturally on:

Mobile
Tablet
Desktop

Mobile should not merely be a compressed desktop page.

Prioritize:

Search
Filters
Results
Open
Save
Continue
Certificate
Verify

Keep interaction simple.

---

65. LOW-BANDWIDTH EXPERIENCE

Where applicable:

- lazy loading
- lightweight result cards
- optimized previews
- controlled file loading
- pagination
- minimal unnecessary requests
- retry handling
- useful offline/error states where existing architecture supports them

Do not sacrifice security for performance.

---

66. UI/UX DESIGN PRESERVATION

DO NOT redesign the existing ELMKUSOMA platform unnecessarily.

Preserve existing:

- sidebar
- topbar
- layout
- typography
- buttons
- cards
- forms
- modals
- spacing
- shadows
- borders
- icons
- responsive behavior
- design tokens
- interaction patterns

Use existing ELMKUSOMA palette:

Primary Blue  #2563EB
Secondary Teal #0D9488
Accent Orange #F59E0B
White #FFFFFF
Light #F8FAFC
Dark Text #1E293B

The UI should feel:

Premium
Clean
Spacious
Modern
Academic
Professional
Calm
Trustworthy
Accessible

Do not add excessive animations.

Do not create a new design system.

---

67. NAVIGATION / ROUTE AUDIT

Inspect all relevant:

- sidebar links
- header links
- resource routes
- search routes
- saved routes
- certificate routes
- verification routes
- detail pages
- deep links
- mobile navigation

Every route must:

EXIST
LOAD
AUTHORIZE
NAVIGATE
RETURN CORRECTLY
WORK ON MOBILE

No orphan pages.

No dead links.

No duplicated routes representing the same feature.

---

68. DEEP LINKING

Test direct access to:

Resource
Search result
Saved item
Certificate
Certificate verification
Related content
Replay
Event

Direct navigation must still trigger backend authorization.

---

69. SECURITY THREAT MODEL

Explicitly test:

IDOR
BOLA
Privilege Escalation
Unauthorized Download
Unauthorized Preview
Unauthorized Certificate Access
Certificate Enumeration
Provider Boundary Bypass
Institution Boundary Bypass
Modified resourceId
Modified certificateId
Modified providerId
Modified learnerId
Private Search Exposure
Private Storage Exposure
Public Verification Data Leakage

Frontend hiding is NOT security.

Backend must remain authoritative.

---

70. MULTI-TENANCY

Preserve existing:

institution_id
provider ownership
organization scope
learner scope
role
permission
resource ownership

Do not allow cross-institution leakage.

Test:

Institution A learner
→ Institution B private resource
✗

---

71. API SAFETY

Audit every affected endpoint for:

- authentication
- authorization
- validation
- ownership
- institution/provider scope
- pagination
- filtering
- error handling
- object-level access
- consistent response structure

Do not create unnecessary APIs.

Modify existing APIs where appropriate.

---

72. TRANSACTION SAFETY

Where applicable, protect:

- certificate issuance
- certificate status changes
- saved-item changes
- resource publication
- indexing state
- relationship changes

against:

- duplicate requests
- concurrency
- partial writes
- inconsistent state

Use existing transaction architecture.

---

73. IDEMPOTENCY

Where relevant, ensure repeated operations do not create duplicates.

Examples:

SAVE
SAVE again
→ one saved record

CERTIFICATE ISSUE
request repeated
→ no duplicate authoritative certificate

SEARCH INDEX EVENT
duplicate event
→ no duplicate indexing side effects

---

74. DATABASE SAFETY

Before changing schema:

1. Inspect existing tables.
2. Inspect entities.
3. Inspect repositories.
4. Inspect relationships.
5. Inspect migrations.
6. Search for duplicate concepts.
7. Check existing data.
8. Check constraints/indexes.
9. Check foreign keys.
10. Check production compatibility.

Never destroy existing data.

Never rename/drop existing structures casually.

---

75. FLYWAY SAFETY

If migration is genuinely required:

- use additive migration
- follow existing naming convention
- avoid duplicate migration versions
- preserve historical migrations
- validate SQL
- check foreign keys
- check indexes
- check rollback implications
- test against current schema

Do not create migrations for functionality already supported by the database.

---

76. DATA INTEGRITY

Verify:

Resource
↔ Provider
↔ Institution
↔ Course
↔ Lesson
↔ Event
↔ Replay

and:

Certificate
↔ Learner
↔ Provider
↔ Course/Event
↔ Evidence
↔ Verification

relationships remain consistent.

---

77. ERROR HANDLING

Every major operation needs truthful errors:

NOT_FOUND
UNAUTHORIZED
FORBIDDEN
UNAVAILABLE
EXPIRED
CONFLICT
VALIDATION_ERROR
SERVER_ERROR

Use the existing ELMKUSOMA API/error conventions.

Do not invent incompatible error formats.

---

78. LOADING / EMPTY / ERROR STATES

Every page and component must handle:

Loading
Empty
Error
Unauthorized
Unavailable
Deleted
Archived
Expired

No broken blank pages.

No fake placeholder content in production.

---

79. SEARCH → RESULT → ACTION E2E

Verify:

SEARCH
 ↓
RESULT
 ↓
DETAILS
 ↓
OPEN / ENROLL / SAVE / REGISTER

depending on content type.

---

80. RESOURCE E2E

Verify:

RESOURCE
 ↓
DETAILS
 ↓
PREVIEW
 ↓
OPEN
 ↓
DOWNLOAD IF AUTHORIZED
 ↓
SAVE
 ↓
MY SAVED
 ↓
REOPEN
 ↓
RELATED CONTENT

---

81. CERTIFICATE E2E

Verify:

COURSE / EVENT
 ↓
REAL COMPLETION / PARTICIPATION
 ↓
REAL EVIDENCE
 ↓
ELIGIBILITY
 ↓
CERTIFICATE
 ↓
MY CERTIFICATES
 ↓
DETAILS
 ↓
DOWNLOAD
 ↓
PUBLIC VERIFICATION
 ↓
AUTHORITATIVE STATUS

---

82. SECURITY E2E

Verify:

Learner A
→ Learner B resource
✗

Learner A
→ Learner B certificate
✗

Learner A
→ private provider resource
✗

Modified resourceId
→ authorization bypass
✗

Modified certificateId
→ authorization bypass
✗

Direct private file URL
→ unauthorized access
✗

---

83. DISCOVERY E2E

Verify:

COURSE
 ↓
LESSON
 ↓
RELATED RESOURCE
 ↓
SAVE
 ↓
RELATED LIVE
 ↓
REPLAY
 ↓
RELATED PRACTICE
 ↓
CONTINUE LEARNING

Only where actual relationships exist.

---

84. D01/D02/D03 CROSS-SYSTEM E2E

Verify:

D01 Dashboard
 ↓
Search / Resource / Certificate
 ↓
D04
 ↓
Course
 ↓
D02
 ↓
Live / Event
 ↓
D03
 ↓
Replay
 ↓
D04 Discovery
 ↓
Resource
 ↓
Certificate

No duplicated data ownership.

---

85. SEARCH INDEX E2E

If indexing exists:

CREATE
 ↓
PUBLISH
 ↓
INDEX
 ↓
SEARCH
 ↓
OPEN

Then:

UPDATE
 ↓
REINDEX
 ↓
SEARCH UPDATED CONTENT

Then:

UNPUBLISH / DELETE
 ↓
REMOVE FROM SEARCH
 ↓
VERIFY NOT EXPOSED

---

86. FAILURE TESTING

Test:

Missing resource
Missing file
Deleted resource
Archived resource
Unavailable provider
Expired content
Broken relationship
Failed search
Search timeout
Unauthorized download
Expired signed URL
Certificate revoked
Certificate not found
Duplicate save
Duplicate certificate issue request
Delayed indexing
Stale search result

The system must fail safely and truthfully.

---

87. OBSERVABILITY

Where existing infrastructure supports it, verify:

- errors
- failed downloads
- failed indexing
- certificate issuance failures
- certificate verification failures
- authorization failures
- search failures
- broken resource references

Do not build unnecessary analytics infrastructure.

---

88. ANALYTICS RULE

If analytics already exist, integrate with them.

Potential legitimate signals:

Searches
Result clicks
Resource opens
Downloads
Saves
Certificate views
Verification requests
Discovery navigation

Never fabricate:

- users
- downloads
- views
- search counts
- certificates
- engagement metrics

---

89. CONTENT QUALITY

Audit resource metadata quality.

Where supported, detect:

- missing title
- missing description
- broken file
- invalid type
- missing provider
- missing relationship
- inaccessible content

Do not silently invent values.

---

90. DUPLICATE CONTENT

Audit potential duplicate resources.

Example:

Networking Guide.pdf
Networking_Guide.pdf
Networking Guide Final.pdf

If duplicate detection is supported:

POTENTIAL DUPLICATE
 ↓
FLAG
 ↓
ADMIN/PROVIDER REVIEW
 ↓
RESOLVE

Never automatically delete authoritative content without an appropriate existing workflow.

---

91. SEARCH PRIVACY

Search history and personal discovery activity must respect:

- authentication
- account boundaries
- privacy
- institution boundaries
- existing retention rules

One learner must never see another learner's private search history.

---

92. CERTIFICATE PRIVACY

Private certificate details must remain private.

Public verification must expose only intentionally public information.

Test for accidental leakage through:

- API
- search
- URL
- metadata
- QR
- logs
- frontend state
- cached responses

where applicable.

---

93. PERFORMANCE ACCEPTANCE

The implementation must avoid:

- unnecessary API calls
- N+1 queries
- giant payloads
- unbounded search results
- duplicate requests
- blocking UI
- excessive file loading

Use pagination and lazy loading where appropriate.

---

94. UI EXPERIENCE ACCEPTANCE

The final experience must have:

CLARITY
CONFIDENCE
CONTINUITY
RELEVANCE
SPEED
FEEDBACK
TRUST
ACCESSIBILITY

A learner should understand:

What is this?
Why am I seeing it?
Can I access it?
What can I do?
What should I do next?

---

95. NO DEAD-END PRINCIPLE

Major pages must provide a meaningful next action.

SEARCH → OPEN
RESOURCE → RELATED / SAVE
REPLAY → RELATED / CONTINUE
COURSE → RESOURCES / LEARNING
CERTIFICATE → VERIFY
SAVED → OPEN / CONTINUE

Only provide actions supported by actual state.

---

96. NO FAKE DATA

ABSOLUTE RULE:

Never create production fake:

- resources
- books
- PDFs
- courses
- providers
- certificates
- search results
- related content
- analytics
- views
- downloads
- completion
- eligibility
- verification status

If test data is required:

- use existing test fixtures
- use existing seed mechanisms
- clearly isolate test data
- never leak test data into production

---

97. NO FAKE COMPLETION

Never mark:

completed
eligible
attended
certified
verified

without authoritative evidence.

---

98. DESIGN QUALITY

Do not optimize for:

- number of cards
- number of pages
- number of APIs
- number of database tables
- number of animations
- amount of code

Optimize for:

REAL VALUE
LOW FRICTION
RELIABILITY
SECURITY
CONTINUITY
DISCOVERABILITY
ACCESSIBILITY
MOBILE QUALITY
TRUST
LEARNING CONNECTION

---

99. IMPLEMENTATION ORDER

Execute in this order:

PHASE 0 — MASTER REFERENCE REVIEW

Read:

- master package
- D01 scope
- D02 scope
- D03 scope
- existing architecture
- relevant developer contracts

---

PHASE 1 — REPOSITORY RECONNAISSANCE

Map:

Frontend
Backend
Database
Storage
Search
Resources
Certificates
Navigation
Security

---

PHASE 2 — RESOURCE AUDIT

Audit:

Resource entities
APIs
Services
Repositories
Storage
Preview
Download
Provider ownership

---

PHASE 3 — SEARCH AUDIT

Map:

Search API
Search service
Queries
Indexes
Filters
Pagination
Ranking
Routes
Frontend

---

PHASE 4 — DISCOVERY AUDIT

Map:

Relationships
Related content
Course/resource
Event/resource
Replay/resource
Certificate/content

---

PHASE 5 — SAVED CONTENT AUDIT

Identify existing:

Bookmark
Saved
Favorites
Watchlist
Personal collections

Reuse only the authoritative existing mechanism.

---

PHASE 6 — CERTIFICATE AUDIT

Map:

Eligibility
Evidence
Issuance
Ownership
Storage
Download
Verification
QR
Revocation
Notifications

---

PHASE 7 — DATABASE / MIGRATION AUDIT

Map:

Tables
Entities
Foreign Keys
Indexes
Constraints
Migrations
Duplicates
Data integrity

---

PHASE 8 — SECURITY AUDIT

Perform:

Authentication
Authorization
Ownership
Institution scope
Provider scope
IDOR/BOLA
Direct URL
Storage access
Certificate access
Verification privacy

---

PHASE 9 — CROSS-DEVELOPER CONTRACT AUDIT

Verify:

D01
D02
D03

and identify:

Existing Contract
Missing Contract
Broken Contract
Duplicate Contract
Required Change

---

PHASE 10 — GAP MATRIX

Produce:

Requirement
Current State
Gap
Owner
Dependency
Risk
Implementation
Verification

---

PHASE 11 — IMPLEMENT BACKEND

Implement necessary:

- APIs
- authorization
- services
- repositories
- relationships
- search integration
- resource access
- certificate integration
- verification
- provider integration

Reuse existing infrastructure.

---

PHASE 12 — IMPLEMENT FRONTEND

Implement/fix:

- Resource Library
- Resource Details
- Search
- Filters
- Discovery
- Saved
- Certificates
- Verification
- Related Content
- Loading states
- Empty states
- Error states
- Mobile behavior
- Accessibility

Preserve existing ELMKUSOMA UI/UX.

---

PHASE 13 — INTEGRATE D01/D02/D03

Connect:

Dashboard
Courses
Learning
Events
Live
Recordings
Resources
Certificates

using stable contracts.

---

PHASE 14 — SECURITY HARDENING

Re-test all authorization boundaries.

---

PHASE 15 — PERFORMANCE / ACCESSIBILITY / MOBILE

Verify:

Desktop
Tablet
Mobile
Low bandwidth
Keyboard
Screen reader
Large result sets

---

PHASE 16 — E2E TESTING

Execute all critical flows.

---

PHASE 17 — BUILD / REGRESSION

Run the appropriate existing:

Backend tests
Frontend tests
Integration tests
E2E tests
Build
Lint
Type checks
Migration validation

Do not invent commands; inspect package/build configuration first.

---

PHASE 18 — FINAL VERIFICATION

Verify every requirement against evidence.

---

100. REQUIRED CROSS-DEVELOPER CONTRACT MATRIX

Produce and verify:

Contract| Owner| Consumer| Status
Dashboard discovery data| D01| D04| 
Course-resource relationship| D02| D04| 
Completion evidence| D02| D04| 
Event-resource relationship| D03| D04| 
Replay-resource relationship| D03| D04| 
Certificate eligibility| Authoritative source| D04| 
Search| Existing shared architecture| D04| 
Storage| Existing storage owner| D04| 
Notifications| Existing notification owner| D04| 

Do not create a new owner merely because integration is inconvenient.

---

101. REQUIRED SECURITY TEST MATRIX

Test| Expected
Learner A → Learner B resource| DENIED
Learner A → Learner B certificate| DENIED
Modified resourceId| DENIED
Modified certificateId| DENIED
Modified providerId| DENIED
Private file direct URL| DENIED
Private preview direct URL| DENIED
Restricted search result| NOT EXPOSED
Unauthorized download| DENIED
Certificate without evidence| DENIED
Modified verification ID| INVALID
Private certificate through public verification| NO PRIVATE DATA

---

102. REQUIRED PRODUCT ACCEPTANCE

The implementation is successful only if a learner can realistically:

SEARCH FOR KNOWLEDGE
 ↓
FIND REAL CONTENT
 ↓
UNDERSTAND THE CONTENT
 ↓
CHECK ACCESS
 ↓
OPEN / PREVIEW
 ↓
SAVE
 ↓
DISCOVER RELATED LEARNING
 ↓
CONTINUE LEARNING
 ↓
COMPLETE REAL LEARNING
 ↓
RECEIVE ELIGIBLE CERTIFICATE
 ↓
VIEW / DOWNLOAD
 ↓
VERIFY

without:

- broken links
- fake content
- duplicate systems
- security bypass
- confusing navigation
- unnecessary friction.

---

103. REQUIRED USER EXPERIENCE STANDARD

The learner should feel:

«“Nikipata kitu ninachohitaji hapa, naweza kukielewa, kukifikia, kukihifadhi, kukirudia na kuendelea na learning journey yangu bila kupotea.”»

The product should encourage useful return through value and continuity, NOT manipulative engagement patterns.

---

104. KNOWLEDGE GATEWAY NORTH STAR

The D04 experience should achieve:

WHATEVER THE LEARNER IS LEARNING
             ↓
FIND THE RIGHT AUTHORIZED KNOWLEDGE
             ↓
CONNECT IT TO CURRENT LEARNING
             ↓
SAVE WHAT MATTERS
             ↓
RETURN LATER
             ↓
CONTINUE LEARNING
             ↓
BUILD REAL EVIDENCE
             ↓
EARN VERIFIABLE ACHIEVEMENT
             ↓
DISCOVER WHAT COMES NEXT

---

105. DEFINITION OF DONE

D04 is COMPLETE only when evidence confirms:

Architecture

- existing architecture inspected
- existing systems reused
- no unnecessary duplicate systems
- ownership boundaries respected

Resources

- library works
- details work
- preview works where supported
- open works
- download is authorized
- save works
- unavailable content handled

Search

- unified search works
- filters work where supported
- pagination works
- authorization is enforced
- indexing lifecycle is correct
- no stale/private results

Discovery

- related content works from real relationships
- no fabricated recommendations
- content relationships remain valid
- no dead ends

Saved

- save works
- remove works
- saved list works
- unavailable/deleted content handled

Certificates

- real evidence drives eligibility
- certificate ownership enforced
- view works
- download works
- verification works
- QR works where supported
- public verification does not leak private data
- revocation/status is authoritative

Integration

- D01 integrated
- D02 integrated
- D03 integrated
- notifications integrated where appropriate
- provider/admin ownership preserved

Security

- IDOR/BOLA tested
- direct URLs tested
- storage tested
- authorization tested
- certificate security tested
- multi-tenancy tested

UX

- desktop works
- mobile works
- responsive
- accessible
- loading states
- empty states
- error states
- no broken routes

Quality

- tests pass
- build passes
- no critical regression
- database remains consistent
- migrations remain valid
- implementation evidence documented

---

106. COMPLETION RULE

Use:

AUDIT COMPLETE
≠
IMPLEMENTATION COMPLETE

IMPLEMENTED
≠
VERIFIED

VERIFIED
≠
PRODUCTION READY

Production completion requires:

IMPLEMENTED
+
INTEGRATED
+
AUTHORIZED
+
TESTED
+
BUILDS
+
NO CRITICAL REGRESSION
+
EVIDENCE PROVIDED

Never inflate completion percentage.

Never claim 100% while critical requirements remain unresolved.

---

107. BLOCKER RULE

If a dependency is blocked:

DO NOT silently stop.

Instead:

1. Implement everything that can safely be implemented.
2. Document the exact dependency.
3. Identify the owning developer/system.
4. Provide exact file/API/database evidence.
5. Explain what remains blocked.
6. Verify that the rest of the implementation is still correct.

Do NOT replace a blocked real dependency with fake data.

---

108. FINAL REPORT — MANDATORY

At completion provide:

A. Executive Summary

What was implemented and verified.

B. Audit Findings

EXISTS
PARTIAL
BROKEN
MISSING
DUPLICATED
INCONSISTENT
NEEDS IMPROVEMENT

C. Files Changed

Exact:

path
change
reason

D. Shared Files Changed

Clearly identify files shared with D01/D02/D03.

E. Backend

List:

- controllers
- services
- repositories
- DTOs
- authorization
- integrations

F. Frontend

List:

- routes
- pages
- components
- hooks
- API clients
- navigation

G. Database

Report:

- tables inspected
- tables changed
- migrations added
- indexes/constraints
- data preservation

H. Search

Report:

- architecture reused
- endpoints
- filters
- indexing
- ranking
- pagination
- security

I. Resources

Report:

- library
- details
- preview
- download
- save
- storage
- lifecycle

J. Discovery

Report:

- relationships
- related content
- cross-content navigation

K. Certificates

Report:

- eligibility
- evidence
- issuance
- ownership
- download
- verification
- QR
- status/revocation

L. Provider/Admin

Report ownership and publication integration.

M. D01/D02/D03

Report every contract and dependency.

N. Security Tests

Report pass/fail evidence.

O. Testing

Report:

- unit
- integration
- E2E
- frontend
- backend
- security
- regression

P. Build

Provide actual build/test results.

Q. Remaining Gaps

Only evidence-backed gaps.

R. Blockers

Exact blocker evidence.

S. Merge / Conflict Risks

Identify shared files and likely merge conflicts.

T. Evidence-Based Completion

Provide:

Implemented: XX%
Verified: XX%
Production readiness: XX%
Critical unresolved: X

Never inflate these numbers.

---

109. FINAL EXECUTION DIRECTIVE

Execute the complete task using:

INSPECT
 ↓
EVIDENCE
 ↓
AUDIT
 ↓
MAP
 ↓
MODEL
 ↓
IDENTIFY GAPS
 ↓
PLAN
 ↓
IMPLEMENT
 ↓
INTEGRATE
 ↓
SECURE
 ↓
TEST
 ↓
VERIFY
 ↓
POLISH
 ↓
REGRESSION TEST
 ↓
REPORT

Do not stop at:

- analysis
- audit
- planning
- recommendations
- TODO lists

The task is implementation.

If something is missing, implement it where safely possible.

If something already exists, reuse it.

If something is broken, fix it.

If something is duplicated, consolidate carefully.

If something is unnecessary, do not add it.

If something is blocked, document exact evidence and continue with everything else that can safely be completed.

---

110. FINAL PRINCIPLE

Build D04 as the ELMKUSOMA Knowledge Gateway, not as a collection of isolated pages.

The final experience must make this journey feel natural:

I HAVE A QUESTION
       ↓
I SEARCH
       ↓
I FIND REAL KNOWLEDGE
       ↓
I UNDERSTAND WHY IT MATTERS
       ↓
I ACCESS IT SAFELY
       ↓
I SAVE IT
       ↓
I DISCOVER RELATED LEARNING
       ↓
I LEARN
       ↓
I PRACTICE
       ↓
I COMPLETE
       ↓
I BUILD EVIDENCE
       ↓
I RECEIVE A REAL CERTIFICATE
       ↓
OTHERS CAN VERIFY IT
       ↓
I DISCOVER WHAT TO LEARN NEXT

This is the standard.

Do not optimize for feature count.

Optimize for:

DISCOVERABILITY + TRUST + SECURITY + CONTINUITY + LEARNING VALUE + EASE OF USE + REAL EVIDENCE.

END OF D04 MASTER PROMPT
