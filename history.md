# ELMKUSOMA — Project History

## Overview
ELMKUSOMA is a modern African EdTech platform for live classes, courses, recorded lessons, and a digital library.

---

## Branch Strategy

```
main
├── develop
│   ├── feature/core-base-setup
│   ├── feature/developer-01-identity-institution
│   ├── feature/developer-02-academic-student
│   ├── feature/developer-03-teacher-parent
│   ├── feature/developer-04-enrollment-learning-assessment
│   ├── feature/developer-05-grading-attendance-nursery
│   └── feature/developer-06-certificate-administration-audit
```

### Workflow
1. `feature/core-base-setup` — merged first (shared infrastructure)
2. Each developer branches from `develop` after base is merged
3. PRs flow: `developer-XX` → `develop` → `main`

---

## Developer Assignments

| Developer | Branch | Modules |
|---|---|---|
| Developer 01 | `feature/developer-01-identity-institution` | `identity/`, `institution/` |
| Developer 02 | `feature/developer-02-academic-student` | `academic/`, `student/` |
| Developer 03 | `feature/developer-03-teacher-parent` | `teacher/`, `parent/` |
| Developer 04 | `feature/developer-04-enrollment-learning-assessment` | `enrollment/`, `learning/`, `assessment/` |
| Developer 05 | `feature/developer-05-grading-attendance-nursery` | `grading/`, `attendance/`, `nursery/` |
| Developer 06 | `feature/developer-06-certificate-administration-audit` | `certificate/`, `administration/`, `audit/` |

---

## Changelog

### 2026-09-07

- **[INIT]** Repository cloned from `Liwondo-005/elmkusoma`
- **[FRONTEND]** Next.js 16.3.3 frontend with Tailwind CSS, shadcn/ui
- **[FRONTEND]** Register page: role selection (Student, Teacher, Lecturer, Facilitator, Parent, Other)
- **[FRONTEND]** Background images added to login, register, forgot-password pages
- **[FRONTEND]** Fixed hydration error in site-footer.tsx (nested `<a>` tags)
- **[BACKEND]** Spring Boot project scaffolded: `backend/elmkusoma-core/`
- **[BACKEND]** Shared foundation: BaseEntity, User, Institution, SecurityConfig, RedisConfig, RabbitMQConfig, Flyway migrations
- **[BRANCH]** Branch strategy created: `main`, `develop`, `feature/core-base-setup`, 6 developer branches
- **[DOCS]** Architecture PRD documented (`elmkusoma_prd.md`)

---

## Architecture

- **Frontend**: Next.js 16.3.3 + React 19 + TypeScript + Tailwind CSS
- **Backend**: Java + Spring Boot (Modular Monolith)
- **Database**: PostgreSQL (Flyway migrations)
- **Cache**: Redis
- **Queue**: RabbitMQ
- **Realtime**: WebSocket/STOMP (planned)
- **Media**: S3-compatible storage (planned)

---

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16.3.3, React 19, TypeScript, Tailwind CSS |
| Backend | Java 17+, Spring Boot 3.x |
| Security | Spring Security, OAuth2/OIDC |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Cache | Redis |
| Queue | RabbitMQ |
| API Docs | Springdoc OpenAPI |
| Build | Maven |
| Testing | JUnit 5, Mockito, Testcontainers |
