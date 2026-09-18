# ELMKUSOMA API Reference

## Base URL
`/api/v1`

## Authentication
All authenticated endpoints require:
```
Authorization: Bearer {jwt_token}
X-Institution-Id: {institution_uuid}
```

## Core Modules
- `/auth/*` — Authentication (login, register, refresh, forgot-password)
- `/institutions/*` — Institution management
- `/academic/*` — Academic structure (levels, years, terms, subjects, classes)
- `/students/*` — Student management
- `/teachers/*` — Teacher management
- `/parents/*` — Parent management
- `/enrollments/*` — Enrollment management
- `/learning/*` — Lessons, assignments, resources
- `/assessments/*` — Quizzes, exams, attempts
- `/grading/*` — Scales, report cards, GPA
- `/attendance/*` — Attendance tracking
- `/nursery/*` — Nursery activities and milestones
- `/certificates/*` — Certificate generation and verification
- `/admin/*` — System administration
- `/audit/*` — Audit logs
- `/media/*` — Media file management

## Macroservices
- Realtime: WebSocket at `ws://localhost:8081/ws`
- Workers: Internal RabbitMQ consumers (no external API)
- Media: `/api/v1/media/*` via port 8083

## NFE Extension (Future)
- `/nfe/providers/*` — Education Provider CRUD
- `/nfe/programs/*` — Program management
- `/nfe/learners/*` — Learner management
- `/nfe/sessions/*` — Live sessions
- `/nfe/materials/*` — Learning materials
- `/nfe/assessments/*` — NFE assessments
- `/nfe/attendance/*` — NFE attendance
- `/nfe/certificates/*` — NFE certificates
