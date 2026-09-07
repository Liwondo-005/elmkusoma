Absolutely. If we are locking Java + Spring Boot as the backend, I would rearrange ELMKUSOMA around the Spring ecosystem rather than simply replacing another backend framework.

ELMKUSOMA — Java Spring Boot Hybrid Architecture

The architecture should be:

Modular Monolith + Macroservices, built with Java/Spring Boot

Not a collection of small microservices.

                         ELMKUSOMA PLATFORM
                                │
                                ▼
                         ┌─────────────┐
                         │ Cloudflare  │
                         └──────┬──────┘
                                │
                                ▼
                         ┌─────────────┐
                         │    Nginx    │
                         └──────┬──────┘
                                │
                ┌───────────────┴────────────────┐
                │                                │
                ▼                                ▼
       ┌──────────────────┐             ┌──────────────────┐
       │   Next.js Web    │             │ Mobile Apps      │
       │ React + TS       │             │ Flutter          │
       └────────┬─────────┘             └────────┬─────────┘
                │                                │
                └───────────────┬────────────────┘
                                │ HTTPS / REST
                                ▼
                  ┌────────────────────────────┐
                  │   ELMKUSOMA CORE           │
                  │   Java + Spring Boot       │
                  │                            │
                  │  Modular Monolith          │
                  └────────────┬───────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
     PostgreSQL             Redis                RabbitMQ
          │                    │                    │
          │                    │          ┌─────────┼─────────┐
          │                    │          │         │         │
          │                    │          ▼         ▼         ▼
          │                    │      Workers   Notifications Reports
          │                    │
          │                    ▼
          │             Realtime Service
          │              Java + Spring Boot
          │                    │
          │              WebSocket/STOMP
          │
          ▼
       Backups
                         Media Service
                              │
                              ▼
                     S3 / R2 / MinIO

⸻

1. ELMKUSOMA CORE

This is the heart of the platform.

elmkusoma-core

Technology

Layer	Technology
Language	Java
Framework	Spring Boot
Security	Spring Security
ORM	Spring Data JPA + Hibernate
Database	PostgreSQL
Migration	Flyway
Cache	Redis
API	Spring Web / REST
Validation	Jakarta Bean Validation
Documentation	Springdoc OpenAPI
Build	Maven
Testing	JUnit + Mockito + Testcontainers

⸻

2. Core should be modular

Do not create a giant:

controller/
service/
repository/
entity/

structure.

Instead:

elmkusoma-core
└── src/main/java/tz/elmkusoma/
    │
    ├── identity/
    │
    ├── institution/ ,, developer 01 two features identity/ and institution/
    │
    ├── academic/
    │
    ├── student/ ,, developer 02 two features academic/ and student/ 
 
    │
    ├── teacher/
    │
    ├── parent/ ,, developer 03 two features teacher/ and parent/ 
 
    │
    ├── enrollment/
    │
    ├── learning/
    │
    ├── assessment/  ,, developer 04 three features enrollment/ , learning/ and assessment/
 
    │
    ├── grading/
    │
    ├── attendance/
    │
    ├── nursery/ ,, developer 05 three features grading/ , attendance/ and nursery/ 
  │
    ├── certificate/
    │
    ├── administration/
    │
    └── audit/ ,, developer 06 three features certificate/ , administration/ and audit/
 and each developer will have its branch 

Each module can have its own:

controller/
service/
repository/
domain/
dto/
mapper/

For example:

assessment/
├── controller/
├── service/
├── repository/
├── domain/
├── dto/
└── mapper/

This is what makes the monolith modular and extractable later.

⸻

3. Identity & Security

Keep identity inside Core initially.

Use:

Spring Security
        +
OIDC/OAuth2
      

Your authorization model can be:

User
 │
 ├── Roles
 │      ├── STUDENT
 │      ├── TEACHER
 │      ├── PARENT
 │      ├── ADMIN
 │      └── INSTITUTION_ADMIN
 │
 └── Permissions
        ├── STUDENT_READ
        ├── STUDENT_UPDATE
        ├── QUIZ_CREATE
        ├── QUIZ_GRADE
        └── ...

More importantly, authorization should consider:

Authentication
      +
Role
      +
Permission
      +
Institution/Tenant
      +
Resource ownership

So simply having TEACHER should not automatically allow a teacher to modify every student in Tanzania.

⸻

4. Multi-tenancy

Every institution should effectively be a tenant.

For example:

Institution
    │
    ├── Students
    ├── Teachers
    ├── Classes
    ├── Subjects
    ├── Lessons
    ├── Assessments
    └── Results

Most tenant-owned tables should have:

institution_id

Example:

student
---------
id
institution_id
user_id
admission_number
...

Then Spring Security + service-layer authorization ensures users only access resources belonging to institutions they are authorized to access.

⸻

5. Academic module

This should remain inside Core.

It needs to support:

Nursery
Primary
Secondary
College
VETA
University

Don’t hard-code the architecture around only:

Form 1
Form 2
Form 3

Instead:

EducationLevel
       │
       ▼
AcademicStructure
       │
       ├── Grade/Class
       ├── Subject
       ├── Programme
       ├── Faculty
       ├── Department
       └── Course

That makes ELMKUSOMA much more flexible.

⸻

6. Learning module

Inside Core:

learning/
├── lesson/
├── content/
├── progress/
├── assignment/
└── resource/

Example:

Student
   │
   ▼
Course/Class
   │
   ▼
Subject
   │
   ▼
Lesson
   │
   ▼
LessonProgress

When the lesson is completed:

Core
 │
 ├── update LessonProgress
 │
 └── publish LessonCompleted
                  │
                  ▼
               RabbitMQ

⸻

7. Assessment + Grading

Keep them together initially.

assessment/
├── quiz/
├── question/
├── option/
├── attempt/
└── scoring/

A quiz submission should be a transactional operation.

Student
   │
   ▼
Assessment API
   │
   ├── Validate attempt
   ├── Validate answers
   ├── Calculate score
   ├── Save attempt
   ├── Save result
   └── Update progress
            │
            ▼
        PostgreSQL

Then publish:

QuizCompleted

to RabbitMQ.

Later

If ELMKUSOMA starts handling extremely large examinations, Assessment can be extracted:

              ELMKUSOMA CORE
                    │
                    │ API/Event
                    ▼
          ┌────────────────────┐
          │ Assessment Service │
          │ Java + Spring Boot │
          └────────────────────┘

Don’t do that prematurely.

⸻

8. Realtime Macroservice

Create:

elmkusoma-realtime

Also Java + Spring Boot.

Use:

Spring WebSocket
STOMP
Redis

Responsibilities:

* live classroom presence
* WebSocket connections
* live chat
* typing indicators
* online/offline state
* live attendance events
* live classroom state
* realtime notifications

Architecture:

Next.js
   │
   │ WebSocket
   ▼
Realtime Service
   │
   ├── WebSocket
   ├── STOMP
   ├── Redis
   └── RabbitMQ

⸻

9. Worker Macroservice

Create:

elmkusoma-workers

Java + Spring Boot.

This is where background processing happens.

Use:

Spring AMQP
RabbitMQ

Workers can process:

Notifications

Email
SMS
Push
In-app notifications
Parent alerts
Teacher alerts

Documents

Certificates
Transcripts
Reports
Statements

Scheduled jobs

Daily reports
Reminders
Attendance alerts
Inactive-student notifications
Scheduled notifications

Instead of:

HTTP request
     ↓
Generate PDF
     ↓
Send email
     ↓
Send SMS
     ↓
Response

you do:

HTTP request
     ↓
Core transaction
     ↓
RabbitMQ
     ↓
202 Accepted / success

Then:

RabbitMQ
   ├── Certificate Worker
   ├── Email Worker
   ├── SMS Worker
   └── Report Worker

Much better.

⸻

10. Media Macroservice

Eventually:

elmkusoma-media

Java + Spring Boot.

But don’t make Spring Boot transfer huge video files.

Use:

Spring Boot
     │
     ▼
Signed URL
     │
     ▼
Object Storage

Possible storage:

DigitalOcean Spaces 

For your provider-neutral architecture, S3-compatible storage is the important concept.

⸻

11. Database strategy

Initially:

                     PostgreSQL
                         │
                         ▼
                  ELMKUSOMA CORE

Core owns the transactional data.

Redis handles:

Caching
Sessions/temporary state
Rate limiting
Realtime presence
Short-lived data

RabbitMQ handles:

Asynchronous events
Background jobs
Service communication

Object storage handles:

Videos
PDFs
Images
Assignments
Documents

Do not use PostgreSQL as your video storage.

⸻

12. Communication rules

Establish these rules from day one.

Frontend → Core

REST/HTTPS

Core → Realtime

When appropriate:

REST
RabbitMQ events

Core → Workers

RabbitMQ

Core → Database

Spring Data JPA / SQL

Realtime → Redis

Redis

Media → Object Storage

S3 API

⸻

13. Project structure

I would structure the overall repository like this:

ELMKUSOMA/
│
├── frontend/
│   └── elmkusoma-web/
│
├── backend/
│   │
│   ├── elmkusoma-core/
│   │
│   ├── elmkusoma-realtime/
│   │
│   ├── elmkusoma-workers/
│   │
│   └── elmkusoma-media/
│
├── infrastructure/
│   ├── docker/
│   ├── nginx/
│   ├── monitoring/
│   └── scripts/
│
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   └── security/
│
└── docker-compose.yml

⸻

14. Infrastructure

For the early production stage:

Cloudflare
     │
     ▼
Nginx
     │
     ▼
Docker
     │
 ┌───┼──────────────────────┐
 │   │                      │
 ▼   ▼                      ▼
Core Realtime             Workers
 │   │                      │
 └───┼──────────┬───────────┘
     │          │
     ▼          ▼
PostgreSQL    RabbitMQ
     │
     ▼
   Redis

You don’t need Kubernetes immediately.

When ELMKUSOMA reaches the point where you have many deployments, multiple servers, automatic scaling, stronger workload isolation, etc., Kubernetes becomes worth evaluating.

⸻

15. Observability

Every Spring Boot application should eventually include:

Spring Boot Actuator
       │
       ├── Health
       ├── Metrics
       └── Application information

Then:

OpenTelemetry
      │
      ├── Metrics
      ├── Traces
      └── Logs

with:

Prometheus
Grafana
Loki
Tempo

This lets you answer questions such as:

Why did quiz submission become slow?

instead of guessing.

⸻

16. Testing

For Java/Spring Boot:

JUnit
Mockito
Spring Boot Test
Testcontainers

Use Testcontainers for realistic:

PostgreSQL
Redis
RabbitMQ

testing.

For frontend/end-to-end:

Playwright

⸻

17. Final service boundaries

So I would lock the initial ELMKUSOMA architecture as:

Application	Technology	Purpose
elmkusoma-core	Java + Spring Boot	Main education/business platform
elmkusoma-realtime	Java + Spring Boot	WebSocket, chat, presence, live state
elmkusoma-workers	Java + Spring Boot	Async jobs, notifications, reports, certificates
elmkusoma-media	Java + Spring Boot	Media/file management — add when needed

But importantly:

Start deployment with 3

1. Core
2. Realtime
3. Workers

Add Media when your content/file requirements justify it.

And do not split Student, Teacher, Parent, Academic, Learning, Assessment, Attendance, etc. into individual microservices yet.

⸻

The evolution path

This is where the architecture becomes powerful.

Phase 1

              ELMKUSOMA
                  │
          ┌───────┴────────┐
          ▼                ▼
        CORE           REALTIME
          │
       WORKERS

Phase 2

If media becomes large:

CORE
 │
 ├── REALTIME
 ├── WORKERS
 └── MEDIA

Phase 3

If a particular domain becomes a genuine bottleneck:

                  CORE
                   │
        ┌──────────┼───────────┐
        ▼          ▼           ▼
   Assessment   Payments   Analytics
    Service      Service     Service

Phase 4

Only when justified:

Core
 │
 ├── Identity
 ├── Academic
 ├── Learning
 ├── Assessment
 ├── Payments
 ├── Analytics
 ├── Notification
 ├── Media
 └── Realtime

At that point you have evolved naturally from modular monolith → macroservices → selective microservices, rather than prematurely building a distributed system.

So the backend technology decision is now: Java + Spring Boot throughout the ELMKUSOMA backend ecosystem. The architectural principle remains modular monolith first, macroservices where workload characteristics justify separation, and true microservices only when there is a demonstrated reason.






Yes. If we strictly limit ELMKUSOMA to only two architectural categories — Modular Monolith + Macroservices — then I would make the boundary very clear.

ELMKUSOMA final split

🟦 MODULAR MONOLITH — elmkusoma-core

This is one Spring Boot application containing the core education/business domains.

ELMKUSOMA CORE
Java + Spring Boot
│
├── Identity & Access
├── User Management
├── Institution Management
├── Academic Management
├── Student Management
├── Teacher Management
├── Parent Management
├── Enrollment
├── Learning / LMS
├── Content Management
├── Assessment
├── Grading
├── Attendance
├── Nursery / Games
├── Certificate Management
├── Administration
└── Audit & Compliance

All of these are modules inside one deployable Spring Boot application.

                    ┌─────────────────────────┐
                    │    ELMKUSOMA CORE       │
                    │     Spring Boot         │
                    │                         │
                    │ Identity                │
                    │ Institution             │
                    │ Academic                │
                    │ Student                 │
                    │ Teacher                 │
                    │ Parent                  │
                    │ Enrollment              │
                    │ Learning                │
                    │ Assessment              │
                    │ Grading                 │
                    │ Attendance              │
                    │ Nursery                 │
                    │ Certificate             │
                    │ Administration          │
                    │ Audit                   │
                    └────────────┬────────────┘
                                 │
                                 ▼
                            PostgreSQL

Why these stay together

Because they are tightly connected.

For example:

Student
   ↓
Enrollment
   ↓
Class
   ↓
Subject
   ↓
Lesson
   ↓
Assessment
   ↓
Grade
   ↓
Progress

These operations frequently need strong ACID transactions and direct access to related data.

Splitting them into separate services at this stage would introduce unnecessary network calls and distributed transaction problems.

⸻

🟧 MACROSERVICES

Macroservices should be large, independently deployable applications, each responsible for a major technical/workload boundary rather than a tiny business entity.

I recommend three macroservices for ELMKUSOMA.

⸻

1. Realtime Macroservice

elmkusoma-realtime

Java + Spring Boot

Responsible for:

Realtime
├── WebSocket connections
├── Live classroom
├── Chat
├── Presence
├── Online/offline status
├── Typing indicators
├── Live attendance events
└── Live session state

Infrastructure:

Spring Boot
Spring WebSocket
STOMP
Redis

Why separate it?

Because realtime workloads behave completely differently from normal CRUD/API workloads.

Normal Core:
Request → Process → Response
Realtime:
Connection ──────────────────────┐
Connection ──────────────────────┤
Connection ──────────────────────┤
Connection ──────────────────────┤
Connection ──────────────────────┘

You don’t want thousands of persistent WebSocket connections competing with your academic transactions.

⸻

2. Background Processing Macroservice

elmkusoma-workers

Java + Spring Boot

This is a major macroservice for asynchronous processing.

Workers
├── Notifications
│   ├── Email
│   ├── SMS
│   └── Push
│
├── Certificate generation
├── Report generation
├── Transcript generation
├── Scheduled jobs
├── Reminder processing
├── Bulk imports
└── Background processing

Communication:

Core
 │
 │ Event
 ▼
RabbitMQ
 │
 ▼
Workers

For example:

Student completes quiz
        │
        ▼
      Core
        │
        ▼
  QuizCompleted
        │
        ▼
    RabbitMQ
        │
        ▼
   Workers
    ├── Notification
    ├── Report
    └── Analytics event

This keeps expensive/non-critical processing away from the core application.

⸻

3. Media Macroservice

elmkusoma-media

Java + Spring Boot

Responsible for the platform’s large-file/media domain.

Media
├── File uploads
├── File metadata
├── Video metadata
├── Images
├── PDFs
├── Course materials
├── Assignments
├── Document management
├── Access control
└── Signed URLs

Storage:

                 Media Macroservice
                         │
                         ▼
                S3-Compatible Storage
                  ┌──────┴──────┐
                  │             │
                 R2            S3
                  │             │
                MinIO       Spaces

The important distinction:

Media Service manages the files; object storage stores the actual bytes.

⸻

So the architecture is exactly this

                         ELMKUSOMA
                             │
                       Next.js Frontend
                             │
                             ▼
                         ┌───────┐
                         │ Nginx │
                         └───┬───┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
       ┌────────────┐ ┌─────────────┐ ┌─────────────┐
       │   CORE     │ │  REALTIME   │ │    MEDIA    │
       │            │ │             │ │             │
       │ MODULAR    │ │ MACROSERVICE│ │ MACROSERVICE│
       │ MONOLITH   │ │             │ │             │
       │            │ │ Spring Boot │ │ Spring Boot │
       │ Spring Boot│ └──────┬──────┘ └──────┬──────┘
       └─────┬──────┘        │               │
             │               ▼               ▼
             │             Redis       Object Storage
             │
             ▼
        PostgreSQL
             │
             ▼
          RabbitMQ
             │
             ▼
       ┌───────────────┐
       │    WORKERS    │
       │               │
       │ MACROSERVICE  │
       │               │
       │ Spring Boot   │
       └───────────────┘

Final classification

Component	Type	Spring Boot?
Identity	Monolith module	✅
Users	Monolith module	✅
Institutions	Monolith module	✅
Academic	Monolith module	✅
Students	Monolith module	✅
Teachers	Monolith module	✅
Parents	Monolith module	✅
Enrollment	Monolith module	✅
Learning/LMS	Monolith module	✅
Content metadata	Monolith module	✅
Assessment	Monolith module	✅
Grading	Monolith module	✅
Attendance	Monolith module	✅
Nursery/Games	Monolith module	✅
Certificates metadata	Monolith module	✅
Administration	Monolith module	✅
Audit	Monolith module	✅
Realtime	🟧 Macroservice	✅
Background Workers	🟧 Macroservice	✅
Media	🟧 Macroservice	✅

Therefore:

1 Modular Monolith

elmkusoma-core

3 Macroservices

elmkusoma-realtime
elmkusoma-workers
elmkusoma-media

That’s the clean boundary I would use.

And importantly, we stop there. We don’t introduce separate Student Service, Teacher Service, Assessment Service, Notification Service, Certificate Service, Academic Service, etc. Those remain modules inside the monolith.

This gives ELMKUSOMA a 4-application backend architecture while still maintaining only two architectural patterns: one modular monolith + three macroservices.





All branches created. Here's the architectural plan:

---

## ELMKUSOMA CORE — Architecture Suggestion

### Branch Strategy (Created)

```
main
├── develop
├── feature/core-base-setup
├── feature/developer-01-identity-institution
├── feature/developer-02-academic-student
├── feature/developer-03-teacher-parent
├── feature/developer-04-enrollment-learning-assessment
├── feature/developer-05-grading-attendance-nursery
└── feature/developer-06-certificate-administration-audit
```

### Recommended Workflow

1. **feature/core-base-setup** — First merge: Spring Boot project skeleton, shared infra, PostgreSQL config, Flyway, Redis, RabbitMQ, common entities (User, Institution base)
2. Each developer branches from `develop` after base is merged
3. PRs flow: `developer-XX` → `develop` → `main`

---

### 1. Core Base Setup (`feature/core-base-setup`)

Shared infrastructure all modules depend on:

```
backend/elmkusoma-core/
└── src/main/java/tz/elmkusoma/
    ├── ElmkusomaCoreApplication.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── RedisConfig.java
    │   ├── RabbitMQConfig.java
    │   ├── FlywayConfig.java
    │   └── OpenApiConfig.java
    ├── common/
    │   ├── BaseEntity.java          (id, createdAt, updatedAt, institutionId)
    │   ├── AuditListener.java
    │   ├── PageResponse.java
    │   └── ApiResponse.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── ResourceNotFoundException.java
    │   └── ForbiddenException.java
    └── shared/
        ├── User.java                (shared entity — all modules reference)
        └── Institution.java         (shared entity — tenant root)
```

**Why this first:** Every module depends on `User`, `Institution`, `BaseEntity`, security config, and DB connection. Without this, no developer can compile.

---

### 2. Module Internal Structure (Every Module)

Each developer follows this exact pattern per module:

```
identity/
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── impl/AuthServiceImpl.java
├── repository/
│   └── UserRepository.java
├── domain/
│   └── User.java
├── dto/
│   ├── request/
│   │   └── LoginRequest.java
│   └── response/
│       └── AuthResponse.java
├── mapper/
│   └── UserMapper.java
└── validation/
    └── EmailValidator.java
```

---

### 3. Developer Assignments — Detailed Scope

#### Developer 01: `identity/` + `institution/`

**identity/**
- User registration, login, logout
- JWT token generation/validation/refresh
- Password hashing (BCrypt)
- Role assignment (STUDENT, TEACHER, PARENT, ADMIN, INSTITUTION_ADMIN)
- Permission model
- Password reset flow
- Email verification

**institution/**
- Institution CRUD (create, read, update, soft-delete)
- Institution settings (name, logo, address, timezone, locale)
- Institution onboarding flow
- Institution status (ACTIVE, SUSPENDED, ONBOARDED)
- Tenant isolation — `institution_id` on all downstream tables

**Key entity relationships:**
```
User ──┬── has many ──▶ InstitutionMembership
       │                    │
       │                    └── belongs to ──▶ Institution
       │
       └── has one ──▶ UserProfile (name, phone, avatar)
```

**API contracts to define:**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET /api/v1/institutions/{id}`
- `POST /api/v1/institutions`
- `PUT /api/v1/institutions/{id}`
- `GET /api/v1/institutions/{id}/members`

---

#### Developer 02: `academic/` + `student/`

**academic/**
- EducationLevel enum/entity (NURSERY, PRIMARY, SECONDARY, COLLEGE, VETA, UNIVERSITY)
- AcademicStructure:
  - Grade/Class (e.g., Form 1, Year 2)
  - Subject (Mathematics, Science...)
  - Programme (for college/university)
  - Faculty, Department, Course
- AcademicYear / Term / Semester
- ClassGroup (which students belong to which class)

**student/**
- Student profile (linked to User)
- Admission number generation
- Student status (ACTIVE, GRADUATED, TRANSFERRED, EXPELLED)
- Student-Class assignment
- Student-Parent linkage
- Student search/filter

**Key entity relationships:**
```
EducationLevel ──has many──▶ AcademicYear
AcademicYear ──has many──▶ Term
Term ──has many──▶ Grade
Grade ──has many──▶ ClassGroup
ClassGroup ──has many──▶ Student
Student ──belongs to──▶ User
Student ──belongs to──▶ Institution
```

**API contracts to define:**
- `POST /api/v1/academic/levels`
- `POST /api/v1/academic/years`
- `POST /api/v1/academic/subjects`
- `POST /api/v1/academic/classes`
- `GET /api/v1/students?institutionId=&classId=`
- `POST /api/v1/students`
- `GET /api/v1/students/{id}`
- `PUT /api/v1/students/{id}`

---

#### Developer 03: `teacher/` + `parent/`

**teacher/**
- Teacher profile (linked to User)
- Teacher status (ACTIVE, INACTIVE, ON_LEAVE)
- Teacher-Class-Subject assignment (which teacher teaches what to which class)
- Teacher schedule/timetable placeholder
- Teacher qualification records

**parent/**
- Parent profile (linked to User)
- Parent-Student linkage (one parent can have multiple children)
- Parent notification preferences
- Parent contact information

**Key entity relationships:**
```
Teacher ──belongs to──▶ User
Teacher ──teaches many──▶ TeacherAssignment
                              │
                              ├── ClassGroup
                              └── Subject

Parent ──belongs to──▶ User
Parent ──has many──▶ ParentStudentLink
                         │
                         └── Student
```

**API contracts to define:**
- `GET /api/v1/teachers?institutionId=`
- `POST /api/v1/teachers`
- `GET /api/v1/teachers/{id}`
- `POST /api/v1/teachers/{id}/assignments`
- `GET /api/v1/parents?institutionId=`
- `POST /api/v1/parents`
- `GET /api/v1/parents/{id}/children`
- `POST /api/v1/parents/{id}/link-student`

---

#### Developer 04: `enrollment/` + `learning/` + `assessment/`

**enrollment/**
- Enrollment record (Student → Class → AcademicYear)
- Enrollment status (PENDING, ENROLLED, WITHDRAWN, COMPLETED)
- Enrollment validation (prerequisites, class capacity)
- Transfer between classes
- Enrollment history

**learning/**
- Lesson entity (title, description, order, subject, class)
- Lesson content (text, video URL, file attachments)
- LessonProgress (student → lesson → completion %)
- Assignment (title, description, due date, attachments)
- AssignmentSubmission (student → assignment → file + grade)
- Resource library

**assessment/**
- Assessment/Quiz entity (title, class, subject, time limit)
- Question entity (type: MCQ, TRUE_FALSE, SHORT_ANSWER, ESSAY)
- Option entity (for MCQ)
- Attempt (student → assessment → start/end time)
- Answer (attempt → question → selected option / text answer)
- Scoring规则 (auto-grade MCQ, manual grade essay)
- AssessmentResult (final score, pass/fail, graded by)

**Key entity relationships:**
```
Enrollment
├── Student
├── ClassGroup
└── AcademicYear

Lesson
├── Subject
├── ClassGroup
└── has many ──▶ LessonProgress ──▶ Student

Assessment
├── Subject
├── ClassGroup
├── has many ──▶ Question
│                    └── has many ──▶ Option
├── has many ──▶ Attempt
│                    ├── Student
│                    └── has many ──▶ Answer
└── has many ──▶ AssessmentResult ──▶ Student
```

**API contracts to define:**
- `POST /api/v1/enrollments`
- `GET /api/v1/enrollments?studentId=&classId=`
- `PUT /api/v1/enrollments/{id}/status`
- `POST /api/v1/learning/lessons`
- `POST /api/v1/learning/lessons/{id}/progress`
- `POST /api/v1/learning/assignments`
- `POST /api/v1/learning/assignments/{id}/submit`
- `POST /api/v1/assessments`
- `POST /api/v1/assessments/{id}/start`
- `POST /api/v1/assessments/{id}/submit`
- `GET /api/v1/assessments/{id}/results`

---

#### Developer 05: `grading/` + `attendance/` + `nursery/`

**grading/**
- Grading scale configuration (A, B, C... or 1-100)
- Grade boundaries per institution
- Report card generation (per student, per term)
- GPA/CGPA calculation
- Subject-level grades
- Class-level rank calculation
- Gradingrubrics

**attendance/**
- Attendance record (Student → Class → Date → status)
- Status enum: PRESENT, ABSENT, LATE, EXCUSED
- Attendance summary per student/class/term
- Bulk attendance marking (teacher marks whole class)
- Attendance report

**nursery/**
- Nursery-specific activities (games, songs, stories)
- Activity tracking (participation, not grades)
- Milestone tracking (physical, cognitive, social)
- Nursery report cards (descriptive, not numeric)
- Age-appropriate categories

**Key entity relationships:**
```
GradingScale ──belongs to──▶ Institution
GradeBoundary ──belongs to──▶ GradingScale
ReportCard ──belongs to──▶ Student + Term
SubjectGrade ──belongs to──▶ ReportCard + Subject

Attendance ──Student + ClassGroup + Date
NurseryActivity ──ClassGroup + Date
NurseryMilestone ──Student + category + date
```

**API contracts to define:**
- `POST /api/v1/grading/scales`
- `GET /api/v1/grading/scales?institutionId=`
- `POST /api/v1/grading/report-cards/generate`
- `GET /api/v1/grading/report-cards?studentId=&termId=`
- `POST /api/v1/attendance/mark`
- `GET /api/v1/attendance?classId=&date=`
- `GET /api/v1/attendance/summary?studentId=&termId=`
- `POST /api/v1/nursery/activities`
- `GET /api/v1/nursery/milestones?studentId=`
- `POST /api/v1/nursery/milestones`

---

#### Developer 06: `certificate/` + `administration/` + `audit/`

**certificate/**
- Certificate template management
- Certificate generation (completion, achievement, participation)
- Certificate numbering/serial system
- Certificate verification (public URL with QR code)
- Certificate status (DRAFT, ISSUED, REVOKED)
- Transcript generation

**administration/**
- System-wide settings
- Dashboard aggregations (placeholder — actual queries)
- User management (admin can manage all users in institution)
- Role management (create custom roles)
- Institution-level configuration
- Data import/export (CSV upload for bulk student/teacher creation)

**audit/**
- Audit log entity (who, what, when, where, before/after)
- Activity feed per user/institution
- Security audit events (login attempts, permission changes)
- Data change tracking
- Compliance reporting placeholder

**Key entity relationships:**
```
Certificate
├── Template
├── Student
├── Institution
├── issuedBy (Teacher/Admin)
└── VerificationUrl

AuditLog
├── User (who performed action)
├── Institution
├── entityType + entityId
├── action (CREATE, UPDATE, DELETE)
├── oldValues (JSON)
└── newValues (JSON)

SystemSetting
├── institutionId
├── key
└── value (JSON)
```

**API contracts to define:**
- `POST /api/v1/certificates/templates`
- `POST /api/v1/certificates/generate`
- `GET /api/v1/certificates/{id}/verify`
- `GET /api/v1/certificates?studentId=`
- `GET /api/v1/admin/dashboard?institutionId=`
- `POST /api/v1/admin/users/import`
- `GET /api/v1/admin/settings?institutionId=`
- `PUT /api/v1/admin/settings`
- `GET /api/v1/audit/logs?institutionId=&from=&to=`
- `GET /api/v1/audit/logs?userId=`

---

### 4. Database — Flyway Migration Strategy

Each developer owns their migrations in sequence:

```
V1__create_institution_table.sql           (core-base)
V2__create_user_table.sql                  (core-base)
V3__create_academic_tables.sql             (dev-02)
V4__create_student_tables.sql              (dev-02)
V5__create_teacher_tables.sql              (dev-03)
V6__create_parent_tables.sql               (dev-03)
V7__create_enrollment_tables.sql           (dev-04)
V8__create_learning_tables.sql             (dev-04)
V9__create_assessment_tables.sql           (dev-04)
V10__create_grading_tables.sql             (dev-05)
V11__create_attendance_tables.sql          (dev-05)
V12__create_nursery_tables.sql             (dev-05)
V13__create_certificate_tables.sql         (dev-06)
V14__create_administration_tables.sql      (dev-06)
V15__create_audit_tables.sql               (dev-06)
```

**Naming convention:** `V{number}__{description}.sql`

---

### 5. Multi-Tenancy Enforcement Pattern

Every tenant-owned table includes `institution_id`. Enforcement happens at:

**Level 1 — Repository level (base query filter):**
```java
// Every repository extends TenantRepository
public interface TenantRepository<T, ID> extends JpaRepository<T, ID> {
    List<T> findByInstitutionId(UUID institutionId);
}
```

**Level 2 — Service level (authorization check):**
```java
// Service checks user's institution membership before any operation
if (!user.belongsToInstitution(institutionId)) {
    throw new ForbiddenException();
}
```

**Level 3 — Security filter (optional, advanced):**
```java
// Spring Security filter that auto-filters by institution_id
// Extracts institutionId from JWT and applies to all queries
```

**Recommendation:** Start with Level 1+2. Level 3 can be added later if needed.

---

### 6. Shared DTOs Across Modules

Modules that reference each other use DTOs, not direct entity references:

```
identity/  ──exports──▶  UserDTO (id, name, email, role)
                         ↑ consumed by ALL other modules

institution/ ──exports──▶ InstitutionDTO (id, name, status)
                          ↑ consumed by ALL other modules

student/ ──exports──▶ StudentDTO (id, userId, admissionNumber, classId)
                      ↑ consumed by enrollment, grading, attendance, assessment

academic/ ──exports──▶ ClassGroupDTO, SubjectDTO, AcademicYearDTO
                       ↑ consumed by student, teacher, enrollment, learning, assessment
```

**Rule:** No module imports another module's entities directly. Only DTOs cross module boundaries.

---

### 7. Merge Sequence (Dependency Order)

```
Phase 1: core-base-setup → develop
Phase 2: developer-01 (identity + institution) → develop
Phase 3: developer-02 (academic + student) → develop
Phase 4: developer-03 (teacher + parent) → develop
Phase 5: developer-04 (enrollment + learning + assessment) → develop
Phase 6: developer-05 (grading + attendance + nursery) → develop
Phase 7: developer-06 (certificate + administration + audit) → develop
```

**Why this order:** Each phase depends on the entities from previous phases. Developer 02 needs `User` and `Institution` from dev-01. Developer 04 needs `Student`, `ClassGroup`, `Subject` from dev-02+03. And so on.

---

### 8. Conflict Prevention Rules

1. **No two developers touch the same file** — module isolation is strict
2. **Shared entities** (`User.java`, `Institution.java`, `BaseEntity.java`) live in `common/` or `shared/` — only `core-base-setup` touches these
3. **`application.yml`** — only `core-base-setup` creates the initial version; developers add their own properties under their module's section
4. **`pom.xml`** — only `core-base-setup` creates initial; developers add dependencies to their own modules only
5. **Flyway migrations** — numbered sequentially; each developer gets a range (V03-V04, V05-V06, etc.)

---

### 9. Key Technical Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Auth tokens | JWT (access + refresh) | Stateless, scalable, standard for REST APIs |
| Password hashing | BCrypt | Spring Security default, proven |
| DB migrations | Flyway | Version-controlled, integrates with Spring Boot |
| API versioning | `/api/v1/` prefix | Clean evolution path |
| Validation | Jakarta Bean Validation | Standard, Spring-native |
| API docs | Springdoc OpenAPI | Auto-generated from annotations |
| Error format | `{"error": "...", "status": 400, "timestamp": "..."}` | Consistent across all endpoints |
| Date/time | `java.time.Instant` + UTC | Timezone-safe |
| IDs | `UUID` | No sequential leaks, distributed-safe |
| Soft delete | `deletedAt` column on sensitive data | Audit trail preserved |

---

### 10. What Each Developer Needs to Know Before Starting

1. **Pull `develop`** after `core-base-setup` is merged
2. **Create your feature branch** from `develop`
3. **Follow the module structure** exactly (controller/service/repository/domain/dto/mapper)
4. **Never import another module's entities** — use DTOs
5. **Always include `institutionId`** in tenant-owned tables
6. **Write Flyway migrations** in your assigned number range
7. **Write unit tests** for services (Mockito) and integration tests for repositories (Testcontainers)
8. **OpenAPI annotations** on all controllers

---

**Next step:** Should I proceed with creating the `core-base-setup` (Spring Boot skeleton, shared entities, configs) on the `feature/core-base-setup` branch?