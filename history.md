# ELMKUSOMA — Project History

This file tracks the development history, decisions, and milestone progress of the ELMKUSOMA education platform.

---

## Architecture Overview

- **Type:** Modular Monolith + Macroservices
- **Backend:** Java 21 + Spring Boot 3.4
- **Frontend:** Next.js + TypeScript
- **Database:** PostgreSQL (Flyway migrations)
- **Cache:** Redis
- **Message Broker:** RabbitMQ
- **Build:** Maven

---

## Branch Strategy

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

**Workflow:** `developer-XX` -> `develop` -> `main`

---

## Developer Assignments

| Developer | Modules | Branch |
|-----------|---------|--------|
| Dev 01 | identity/ + institution/ | feature/developer-01-identity-institution |
| Dev 02 | academic/ + student/ | feature/developer-02-academic-student |
| Dev 03 | teacher/ + parent/ | feature/developer-03-teacher-parent |
| Dev 04 | enrollment/ + learning/ + assessment/ | feature/developer-04-enrollment-learning-assessment |
| Dev 05 | grading/ + attendance/ + nursery/ | feature/developer-05-grading-attendance-nursery |
| Dev 06 | certificate/ + administration/ + audit/ | feature/developer-06-certificate-administration-audit |

---

## Timeline

### 2026-09-07 — Project Initialization

**What was done:**
- Cloned repository from GitHub
- Resolved merge conflict in `frontend/components/live-class-card.tsx`
- Reviewed and confirmed ELMKUSOMA PRD (Java + Spring Boot modular monolith architecture)
- Designed full module structure and developer assignments
- Created all Git branches (main, develop, feature/core-base-setup, 6 developer branches)
- Created `history.md` for project tracking

**Branches created:**
- `develop` — integration branch
- `feature/core-base-setup` — Spring Boot skeleton, shared infra, common entities
- `feature/developer-01-identity-institution` — identity + institution modules
- `feature/developer-02-academic-student` — academic + student modules
- `feature/developer-03-teacher-parent` — teacher + parent modules
- `feature/developer-04-enrollment-learning-assessment` — enrollment + learning + assessment
- `feature/developer-05-grading-attendance-nursery` — grading + attendance + nursery
- `feature/developer-06-certificate-administration-audit` — certificate + administration + audit

**Architecture decisions:**
- Multi-tenancy via `institution_id` on all tenant-owned tables
- JWT authentication (access + refresh tokens)
- BCrypt password hashing
- Flyway for database migrations
- MapStruct for entity-DTO mapping
- Lombok for boilerplate reduction
- Springdoc OpenAPI for API documentation

---

## Core Module Structure (elmkusoma-core)

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
    │   ├── BaseEntity.java
    │   ├── AuditListener.java
    │   ├── PageResponse.java
    │   └── ApiResponse.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── ResourceNotFoundException.java
    │   └── ForbiddenException.java
    ├── shared/
    │   ├── domain/
    │   │   ├── User.java
    │   │   └── Institution.java
    │   └── repository/
    │       ├── UserRepository.java
    │       └── InstitutionRepository.java
    ├── identity/
    ├── institution/
    ├── academic/
    ├── student/
    ├── teacher/
    ├── parent/
    ├── enrollment/
    ├── learning/
    ├── assessment/
    ├── grading/
    ├── attendance/
    ├── nursery/
    ├── certificate/
    ├── administration/
    └── audit/
```

Each module follows internal structure:
```
module/
├── controller/
├── service/
│   └── impl/
├── repository/
├── domain/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
└── validation/
```

---

## Pending Actions

- [ ] Complete core-base-setup (fill missing shared entities, exceptions, migrations)
- [ ] Push feature/core-base-setup branch
- [ ] Merge into develop
- [ ] Merge develop into main
- [ ] Developer 01 begins identity/ + institution/
- [ ] Developer 02 begins academic/ + student/
- [ ] Developer 03 begins teacher/ + parent/
- [ ] Developer 04 begins enrollment/ + learning/ + assessment/
- [ ] Developer 05 begins grading/ + attendance/ + nursery/
- [ ] Developer 06 begins certificate/ + administration/ + audit/
