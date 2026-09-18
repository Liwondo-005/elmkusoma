# ELMKUSOMA Database Schema

## Migrations
- V01–V20: Core tables (identity, institution, academic, student, teacher, parent, enrollment, learning, assessment, grading, attendance, nursery, certificate, administration, audit)
- V21–V46: Feature additions (teacher features, learner workspace, events, live classes, media assets)
- Media service: V001 (media_files)

## Key Tables
| Table | Description |
|-------|-------------|
| institutions | Multi-tenant root |
| users | Authentication (shared) |
| institution_memberships | User ↔ Institution mapping |
| students | Formal education learners |
| teachers | Teaching staff |
| parents | Parent/guardian profiles |
| academic_years | Academic year definitions |
| terms | Term/semester definitions |
| grades | Grade/class definitions |
| subjects | Subject definitions |
| class_groups | Class groupings |
| courses | Course offerings |
| lessons | Lesson content |
| assessments | Quizzes and exams |
| attendance_records | Student attendance |
| certificates | Issued certificates |
| audit_logs | System audit trail |
| media_assets | Live class recordings |
| media_files | Object storage metadata |

## Multi-Tenancy
All tenant-owned tables include `institution_id` column.
