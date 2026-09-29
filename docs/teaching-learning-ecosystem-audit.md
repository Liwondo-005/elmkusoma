# ELMKUSOMA — TEACHING & LEARNING ECOSYSTEM: REQUIRED AUDIT (Section 71)

Base commit: `9683979` (main, in sync with origin/main). Date: 2026-09-29.
Evidence classes: VERIFIED = read in code/DB/tests this session; INFERENCE = derived from code reading; ASSUMPTION = flagged as such.

---

## A. EXISTING ARCHITECTURE MAP

Legend: Status = EXISTS / PARTIAL / BROKEN / MISSING / DUPLICATED / INCONSISTENT

### A1. Course / Subject / ClassGroup / Enrollment
| Entity → Table | Repo | Service | Controller / API | Frontend Route | Migrations | Status |
|---|---|---|---|---|---|---|
| `Course`→courses, `CourseModule`→course_modules, `CourseLesson`→course_lessons | course/repository/* | CourseService, CourseMapper | `/v1/courses` (CourseController:19) | /dashboard/teacher/courses? (courses pages exist), learner course pages | V08, V79, V81 | EXISTS |
| `Subject`→subjects, `ClassGroup`→class_groups, `Grade`(level)→grades, `Term`, `AcademicYear` | academic/repository/* | AcademicService | `/v1/academic` (AcademicController:27) | teacher class selectors | V07, V19 | EXISTS |
| `Enrollment`→enrollments, `StudentClassAssignment`→student_class_assignments, `LearnerEnrollment`→learner_enrollments, `StudentCourseEnrollment` | enrollment/learner/student repos | EnrollmentService+impl | `/v1/enrollments` (EnrollmentController:24) | admin/enroll pages | V10, V20, V23, V56 | DUPLICATED (4 parallel enrollment models; orphan `classes`/`class_subjects` tables from V06 have no entity; `enrollments.class_group_id` FK→`classes(id)` not `class_groups(id)` — V10:19) |

### A2. Lesson
| Entity | Repo | Service | API | Frontend | Migrations | Status |
|---|---|---|---|---|---|---|
| `Lesson`→lessons (learning/domain/Lesson.java:11-49; status DRAFT\|READY\|PUBLISHED\|ARCHIVED) | LessonRepository | LearningServiceImpl (:36; assertLessonOwnership :278) | `/v1/learning/lessons*` (LearningController:37-136: create/update/status/delete/publish/unpublish/archive/list) | `/dashboard/teacher/lessons` (page.tsx, real CRUD); learner lesson content via learner course routes | V11, V28, V79, V81, V106, V107 | EXISTS (content+publish+ownership tests) / **MISSING: learning objectives/outcomes (0 grep hits), lesson_id links from assignments/assessments/live_classes** |
| `CourseLesson`→course_lessons | CourseModuleRepository paths | CourseService | `/v1/courses/{id}/modules/{id}/lessons` | learner course lessons | V08 | DUPLICATED (2nd parallel lesson model; resources/video_tutorials FK to `course_lessons`, `lessons` is the teacher-sidebar model) |

### A3. Resource
| Entity | Repo | Service | API | Frontend | Migrations | Status |
|---|---|---|---|---|---|---|
| `Resource`→resources (Resource.java:18-175; ResourceType 11 values incl. LIVE_RECORDING, ResourceVisibility 7 values, is_downloadable/is_previewable, storage_url, lesson_id) + ResourceAnalytics/Tag/Tagging/Annotation, StudentSavedResource, VideoTutorial(+Progress) | learning/repository/* (9 repos) | ResourceService(:39), ResourceAnalyticsService, ResourceAnnotationService, VideoTutorialService | `/v1/resources` CRUD+tags+save+annotations+analytics (ResourceController:28-260); `/v1/video-tutorials` incl. `POST /attach-recording` (:35) | `/dashboard/teacher/resources` (real CRUD, **no file upload — URL fields only**); `/dashboard/learner/resources[/{id}]` (real, bookmarks+annotations) | V11, V90, V93, V94, V95-V103, V106 | EXISTS (visibility security tests) / **MISSING: versioning (no ResourceVersion), file upload wiring, institution-unscoped `findByIsDeletedFalse` leak path (ParentLibraryService:25), presigned-upload placeholder TODO (ResourceService:305), N+1 in getSavedResources (:291)** |

### A4. Assignment + Submission
| Entity | Repo | Service | API | Frontend | Migrations | Status |
|---|---|---|---|---|---|---|
| `Assignment`→assignments (subject_id, class_group_id, title, description, due_date, total_marks, attachments String, assignment_type String, instructions, status) | AssignmentRepository, AssignmentSubmissionRepository | LearningServiceImpl create/update/delete/list/submit/grade (:330-428) | `/v1/learning/assignments*` (LearningController:201-265) | `/dashboard/teacher/assignments` (783 ln, real, grading UI inline); `/dashboard/assignments[/{id}]` learner (list + detail) | V11, V22, V29(no-op), V81 | **BROKEN: `POST .../submit` accepts NO body (LearningController:241-244) → submission file_url always null; learner payload built but never sent (frontend lib/api.ts:304)**; **PARTIAL: no status/draft/resubmission (rejects 2nd submit :395-397), no tasks, no enum, no lesson link, no open/late rules, no tests**; **SECURITY: update/delete/grade/list have NO institution/ownership check (:350-428)** |
| `AssignmentSubmission`→assignment_submissions (assignment_id, student_id, file_url, submitted_at, grade, feedback, graded_at, graded_by) | ↑ | ↑ | `POST /{id}/submit`, `GET /{id}/submissions`, `PUT /submissions/{id}/grade` | learner detail page; teacher grading inline | V11, V29, V81 | PARTIAL: **no submission_text/status/draft columns** (V29 shape never applied after V11), no late flag, no multi-file, `SubmissionRequest` DTO dead |

### A5. Assessment + Question + Attempt + Answer
| Entity | Repo | Service | API | Frontend | Migrations | Status |
|---|---|---|---|---|---|---|
| `Assessment`→assessments (starts_at, ends_at, time_limit_minutes, pass_marks, total_marks, is_published, status raw String never set) | AssessmentRepository | AssessmentServiceImpl(:24) | `/v1/assessments` (AssessmentController:22: create, list, addQuestion, getQuestions, start, submit, results, gradeEssay, submissions) | `/dashboard/teacher/assessments` (create+add-question only, no edit/delete); `/dashboard/assessments[/{id}]` learner (attempt UI, countdown, results) | V12, V81 | **PARTIAL: availability window + time_limit + max-attempts NEVER enforced (startAttempt:118-142); status never set on create; no sections; no type enum** |
| `Question`→questions (MCQ\|TRUE_FALSE\|SHORT_ANSWER\|ESSAY), `Option`→options | QuestionRepository, OptionRepository | addQuestion/getQuestions (:66-116) | `POST/GET /v1/assessments/{id}/questions` | teacher question editor; learner attempt | V12 | **BROKEN/SECURITY: `GET /{id}/questions` returns `is_correct` to STUDENT (OptionResponse:19, no sanitization)**; **MISSING: QuestionBank, versioning/snapshot, question edit/delete, types beyond 4** |
| `Attempt`→attempts (2 states only), `Answer`→answers, `AssessmentResult`→assessment_results | Attempt/Answer/AssessmentResult repos | startAttempt(:118), submitAttempt(:144-213), gradeEssay(:232) | `POST /{id}/start`, `POST /attempts/{id}/submit`, `PUT /answers/{id}/grade` | attempt page: **no autosave/resume — answers in useState only (page.tsx:297)** | V12, V30/V31 orphans | **PARTIAL: auto-grade MCQ/TF all-or-nothing (:177-191); NO autosave endpoint (0 grep), no resume, no timer expiry/auto-submit server-side, gradeEssay no ownership check, results/student/{id} no self-check for STUDENT** |

### A6. Grading + Rubric + Gradebook
| Entity | Repo | Service | API | Frontend | Migrations | Status |
|---|---|---|---|---|---|---|
| `GradingScale`→grading_scales, `GradeBoundary`→grade_boundaries | grading repos | GradingScaleServiceImpl, GradeBoundaryServiceImpl (calculateGradeForPercentage :96 **never called**) | `/v1/grading/scales*`, `/boundaries*` | `/dashboard/teacher/grading` (read-only + CSV) | V13 | EXISTS (unused calc) |
| `ReportCard`→report_cards, `SubjectGrade`→subject_grades | ↑ | ReportCardServiceImpl.generate **computes nothing** (:44-56); calculateClassRanks (:109) no caller | `POST /report-cards/generate`, `GET ...`, `PATCH /{id}/status` | grading page report tab | V13 | **BROKEN: generate persists empty shells (total_marks/average/gpa never set); NO service writes SubjectGrade; no grade-release notification/lock** |
| `GradingRubric`→grading_rubrics, `RubricCriteria`→rubric_criteria | repos exist | **NONE — dead code** | **NONE** | none | V13, V32 (dup) | **MISSING surface (entities exist, zero endpoints)** |
| Gradebook | — | — | — | `/dashboard/teacher/gradebook` (real data assembled client-side, read-only matrix, N+1) | none | **MISSING backend (grep Gradebook=0); no table; aggregation endpoint needed** |

### A7. Progress / Outcomes / Competencies / Academic records
| Item | Evidence | Status |
|---|---|---|
| `LessonProgress`→lesson_progress (completion_percentage, started/completed_at); `POST/GET /v1/learning/progress*`; learner `POST /v1/learner/me/lessons/{id}/complete`, `GET /me/progress/{courseId}`; replay+video progress | V11:26, LearningServiceImpl updateProgress, LearnerController:915 | EXISTS (completion driven by learner actions; no configurable rules per §9) |
| LearningOutcome entity/table | grep 0 | **MISSING** |
| Competency/CompetencyRecord/CompetencyAssessment (V52) linked to assessments (linkAssessmentToCompetency :197) | highered package only | EXISTS (HE-scope; not joined to grades/lessons) |
| AcademicRecord→academic_records (HE, V56) + GpaCalculationService | highered controllers | EXISTS (HE-scope; school side uses report_cards) |

### A8. Notification + Communication + Audit
| Item | Evidence | Status |
|---|---|---|
| `LearnerNotification`→learner_notifications; NotificationService.notifyUser(:37)/notifyInstitutionStudentsExcluding(:71) → RabbitMQ → realtime broadcast; NotificationController `/v1/notifications` (all roles); learner `/v1/learner/me/notifications*` | V23 | EXISTS engine |
| Teaching types written today: LESSON_PUBLISHED, COURSE_UPDATE, LIVE_CLASS_*, EVENT_*, CERTIFICATE, ENROLLMENT, COURSE_COMPLETION | LearningServiceImpl:121-131 etc. | **MISSING: ASSIGNMENT_*, SUBMISSION_*, ASSESSMENT_*, GRADE_* types (no notifyUser calls in learning/assessment/grading)** |
| `Announcement`→announcements + TeacherAnnouncementController `/v1/teachers/me/announcements` + LearnerController GET `/v1/learner/announcements` | V81/V88 | PARTIAL (AnnouncementServiceImpl has no controller; scope-unscoped findAll) |
| Comments/Discussion/Forum | grep 0 | MISSING (live-class chat exists) |
| AuditService.recordAuditLog(:38)/recordActivity(:91) + AuditController `/v1/audit` (ADMIN\|INST_ADMIN); AuditLog action enum = generic CRUD/LOGIN/... | V18 | EXISTS infra, **NOT WIRED: 0 calls in learning/assessment/grading/enrollment/academic/course; no academic action vocabulary** |

### A9. LiveClass ↔ Lesson
| Item | Evidence | Status |
|---|---|---|
| `LiveClass`→live_classes (subject_id, class_group_id, teacher_id, recording_url, broadcast_source V108, full interaction domain V61: quizzes/polls/breakouts/attendance/hands) | V08, V40-V78, V108/V109 | EXISTS (tested) |
| **No `lesson_id` on live_classes** (V48 added class_group_id only) | LiveClass.java:18-88 | **MISSING link** |
| Recording→lesson: `POST /v1/video-tutorials/attach-recording` → video_tutorials (lesson_id → course_lessons) | VideoTutorialController:35 | PARTIAL (indirect; Resource.LIVE_RECORDING never used) |
| Lesson↔live UI join control | frontend audit: none in any lesson page | **MISSING** |

### A10. Frontend routes (teacher TEACHING + learner)
| Route | Page | Data | Status |
|---|---|---|---|
| /dashboard/teacher/lessons | 321 ln real CRUD + publish | real | EXISTS |
| /dashboard/teacher/resources | 570 ln real CRUD | real; **no upload** | PARTIAL |
| /dashboard/teacher/assignments | 783 ln CRUD+submissions+grade | real | EXISTS (decorative paperclip L486; N+1) |
| /dashboard/teacher/assessments | 532 ln create+questions+results | real | PARTIAL (no edit/delete) |
| /dashboard/teacher/grading | 326 ln scales+report cards | real | EXISTS read-only (N+1) |
| /dashboard/teacher/gradebook | 379 ln matrix | real client-assembled | PARTIAL (no writes; heaviest N+1) |
| /dashboard/assignments[/{id}] learner | list/detail/submit | **payload dropped (lib/api.ts:304)** | BROKEN |
| /dashboard/assessments[/{id}] learner | list/attempt/results + countdown | real | PARTIAL (no autosave/resume) |
| /dashboard/learner/resources[/{id}] | list/detail/bookmarks/annotations | real | EXISTS (i18n hardcodes L243-271) |
| /dashboard/learner/assessments | derived list → **lesson page "coming soon" placeholder (lessons/[lessonId]/page.tsx:311-327)** | real | BROKEN dead-end |
| /dashboard/learner/progress, my-learning, academic-record, replays | real | real | EXISTS |
| /dashboard/results | real grades | **orphan — 0 incoming links** | INCONSISTENT |
| Sidebar: 5 Teaching routes all resolve; **assignments absent from learner/college/university navs**; topbar bell route hard-coded learner path | dashboard-sidebar.tsx:364-418, topbar:248 | — | PARTIAL |

---

## B. GAP MATRIX (Requirement → Existing → Status → Missing → Modification)

| # | Requirement | Existing | Status | Missing / Modification required |
|---|---|---|---|---|
| B1 | Resource core fields/types/visibility | Resource entity full | EXISTS | — (extend: file upload wiring; versioning deferred w/ disclosure) |
| B2 | Resource lifecycle states | ResourceVisibility + processing_status | PARTIAL | Explicit DRAFT→PUBLISHED handled via visibility DRAFT; no action needed beyond docs |
| B3 | Resource reusable multi-association | lesson_id/module_id/course_id columns | EXISTS | — |
| B4 | Resource access control | visibility matrix + tests | PARTIAL | institution-unscoped repo method used by ParentLibraryService → scope it |
| B5 | Resource learner analytics | ResourceAnalytics endpoints | EXISTS | — |
| B6 | Lesson lifecycle (create→publish→archive) | full endpoints + ownership tests | EXISTS | — |
| B7 | Lesson objectives/outcomes | none | MISSING | Out of scope this wave → REPORT (no fake) |
| B8 | Lesson completion rules | LessonProgress percentage | PARTIAL | keep learner-driven completion; disclose |
| B9 | Lesson ↔ resources | resources.lesson_id (relaxed FK V106) | EXISTS | — |
| B10 | Lesson ↔ assignments | none (subject/class only) | MISSING | ADD `lesson_id` to assignments (+entity, migration, API, UI filters) |
| B11 | Lesson ↔ assessments | none | MISSING | ADD `lesson_id` to assessments (same pattern) |
| B12 | Lesson ↔ live class | none | MISSING | ADD `lesson_id` to live_classes + expose in teacher live-class create/update + lesson-page join entry |
| B13 | Assignment create with fields/types | title/desc/due/total_marks/type(String) | PARTIAL | tasks/structured fields → REPORT; enum not required (existing String reused per rule "reuse existing") |
| B14 | Assignment ↔ resources attach | attachments free-text String, decorative paperclip | PARTIAL | wire paperclip to mediaApi.upload → store URL(s) in attachments; backend accepts body already |
| B15 | Assignment submission types + **persistence** | submit endpoint, no body | **BROKEN** | **accept `SubmissionRequest` body (content+fileUrl), persist submission_text; migration adds columns** |
| B16 | Learner flow states (draft/submitted/graded/returned) | single implicit state | MISSING | ADD status + is_draft columns; draft save endpoint; resubmission while ungraded; status transitions |
| B17 | Assignment rules (late, resubmit, max attempts) | due_date only | PARTIAL | add allow_late_submission + close_date (additive); disclose rest |
| B18 | Group assignments | none | MISSING | REPORT (no existing model) |
| B19 | Assignment feedback | feedback on grade endpoint | EXISTS | — (release policy → REPORT) |
| B20 | Assessment types/sections/randomization | none (Assessment has raw status) | MISSING | REPORT sections/randomization; set status on create |
| B21 | Question types | 4 types | PARTIAL | REPORT extra types (no duplicate engine) |
| B22 | **Question answer-key protection** | leak to STUDENT | **BROKEN** | sanitize DTO for STUDENT role |
| B23 | Question bank/versioning | none | MISSING | REPORT (question snapshot at attempt → out; disclose corruption risk) |
| B24 | Assessment rules: availability/time/max attempts | columns exist, unenforced | **BROKEN** | **enforce starts_at/ends_at/time limit server-side + max_attempts column** |
| B25 | Learner attempt flow + autosave + resume | start/submit only | PARTIAL | **ADD `PUT /attempts/{id}/answers` (upsert) + `GET /{id}/my-attempt` (resume) + frontend autosave timer + resume on load** |
| B26 | Auto-grading objective Qs | MCQ/TF all-or-nothing | EXISTS | partial marks → REPORT |
| B27 | Manual grading (essay) | gradeEssay | PARTIAL | add ownership/institution check |
| B28 | Grade release states | report_cards.status DRAFT/PUBLISHED | EXISTS | release notification → add (GRADE_RELEASED) |
| B29 | Grade override audit | none | MISSING | wire AuditService on grade write (old→new in details) |
| B30 | **Gradebook aggregation** | client-side N+1 matrix | MISSING backend | **ADD `GET /v1/grading/gradebook/class/{id}` aggregate (real data, ≤3 queries) + switch frontend page** |
| B31 | Rubric engine | entities+repos dead | PARTIAL | **wire CRUD endpoints + use in grading UI display** |
| B32 | Report card computation | generate() empty | **BROKEN** | **compute average from graded submissions+results, grade via existing GradeBoundaryService.calculateGradeForPercentage (first caller)** |
| B33 | Grades → learner "My Grades" | /dashboard/results orphan | INCONSISTENT | **link Results into learner sidebar**; submission grade visible on assignment detail |
| B34 | Grade → progress | separate | PARTIAL | gradebook response includes lesson/course progress via existing endpoints; full linkage REPORT |
| B35 | Communication reuse w/ context IDs | announcements + notifications target_id | EXISTS | use targetType/targetId for new events |
| B36 | **Teaching notifications** (lesson/assignment/assessment/grade/live) | LESSON_PUBLISHED + LIVE only | PARTIAL | **add ASSIGNMENT_PUBLISHED, ASSIGNMENT_SUBMITTED, ASSIGNMENT_GRADED, ASSESSMENT_PUBLISHED, ASSESSMENT_SUBMITTED, GRADE_RELEASED via existing NotificationService** |
| B37 | Learner dashboard discoverability | resources/progress/live real; assignments/assessments reachable but sidebar-missing | PARTIAL | add Assignments (and Grades) entries to learner navs; fix lesson placeholder dead-end → link to attempt/submit pages |
| B38 | Teacher dashboard actionable overview | teacherApi.getDashboard exists | EXISTS | REPORT as-is (numbers clickable → verify links only) |
| B39 | Search/discovery | client-side filters exist | PARTIAL | REPORT (reuse existing search arch only where present) |
| B40 | **Backend authoritative authz** | @PreAuthorize everywhere; ownership on lessons/resources | **PARTIAL/BROKEN** | **add institution+ownership checks: assignment update/delete/list/grade; assessment start/questions/results/gradeEssay; self-check results/student/{id}; scope resource unscoped repo** |
| B41 | Enrollment checks for learner reads | none (grep enrollment=0 in learning) | MISSING | add class-membership check for STUDENT on class listings (student_class_assignments OR enrollments fallback; log+deny mismatch) — MUST NOT break existing flows → gate on resolvable student record |
| B42 | Multi-tenancy institution_id | present on all teaching tables | EXISTS | enforce in service where missing (B40) |
| B43 | Additive migrations only | V01-V109 | EXISTS | V110+ additive only |
| B44 | **Audit logging for academic actions** | AuditService unused in teaching | MISSING | **wire recordAuditLog on: lesson publish, assignment create/update/delete, submission, assessment create/question add/attempt start+submit, grade write/release, report card publish** |
| B45 | No fake data | no mocks in teaching pages | EXISTS | gradebook computes real rows only |
| B46 | Route/link audit | all sidebar links resolve; /dashboard/results orphan; lesson placeholder dead-end | PARTIAL | fix 2 dead-ends; wire bell role-aware |
| B47 | Low-bandwidth/pagination | lists unpaged (per class bounded) | PARTIAL | gradebook aggregate reduces requests; disclose |
| B48 | States (loading/empty/error) | all pages handled | EXISTS | — |
| B49 | Tests: assignments/assessments/grading 0 tests | security tests exist for lessons/resources | MISSING | add service tests: submit persistence, scope checks, answer-key sanitization, availability/max-attempts, autosave, gradebook aggregation |

---

## C. DUPLICATION AUDIT

1. **Two lesson models**: `lessons` (teacher sidebar + progress) vs `course_lessons` (course builder; resource/video FKs target it). → KEEP BOTH (rule: don't remove); connect via UI, document.
2. **Four enrollment models**: `enrollments`, `student_class_assignments`, `learner_enrollments`, `student_course_enrollments` + orphans `classes`/`class_subjects`/`student_class_enrollments` (V06, no entity). → KEEP; use `student_class_assignments` (+`enrollments` fallback) for learner membership checks.
3. **Two `teacherApi` objects** (lib/api.ts:1782 vs lib/teacher-api.ts:207) + duplicated assignment CRUD in both + page-local appFetch. → NOT consolidating wholesale (risk); new code uses one convention (`appFetch`/`teacherFetch` as page already does) — consolidation REPORTED.
4. **Duplicate migration shapes**: assignment_submissions (V11+V29), grading_rubrics (V13+V32), attempts vs assessment_attempts (V30 orphan), answers vs assessment_answers (V31 orphan), V81+V88 double-create. → No action (idempotent); REPORT only.
5. **Two audit systems**: audit_logs (AuditService) vs institution_audit_log (InstitutionAuditService). → use AuditService per §60; don't create third.
6. **Notification write paths**: NotificationService vs direct repository.save in 6 call sites. → new teaching events go through NotificationService (single engine).
7. **Progress systems**: lesson_progress vs replay_progress vs video_tutorial_progress vs HE competency records. → all distinct scopes; keep.

---

## D. DEPENDENCY MAP (current vs required)

```
CURRENT (solid lines = real link, ✗ = missing):
Institution ─┬─ ClassGroup ─ Subject ──┬─ Lesson(lessons) ──► lesson_progress
             │                         ├─ Assignment ──► AssignmentSubmission(grade, feedback) ──✗──► ReportCard/SubjectGrade
             │                         ├─ Assessment ──► Question ──► Option
             │                         │      └─ Attempt ──► Answer ──► AssessmentResult ──✗──► ReportCard
             │                         └─ LiveClass ─✗lesson─► Lesson   └─✗─► SubjectGrade (never written)
             ├─ GradingScale ── GradeBoundary (calc unused)   GradingRubric ── RubricCriteria (DEAD)
             ├─ LessonProgress ─✗─► course/certificate completion
             ├─ LearnerNotification (engine ✓; ASSIGNMENT/GRADE types ✗)
             ├─ Announcement (teacher ✓, learner read ✓; scope ✗)
             ├─ AuditService (infra ✓; teaching call sites ✗)
             └─ Competency ── AcademicRecord (HE-only, not joined to school grades)

REQUIRED links to add (this implementation):
  Lesson ──(lesson_id)── Assignment / Assessment / LiveClass
  AssignmentSubmission(body persist, status/draft) ──► Gradebook aggregate ──► GradeBoundary calc ──► ReportCard compute
  AssessmentResult + AssignmentSubmission ──► Gradebook
  Attempt ── autosave Answer upsert ── resume
  NotificationService ◄── assignment/assessment/grade events (targetType/targetId context)
  AuditService ◄── publish/submit/grade/release actions
```

---

## E. RISK REGISTER

| Risk | Change touching it | Mitigation |
|---|---|---|
| Authentication | none (no filter/JWT change) | — |
| Authorization | new ownership/institution checks in LearningServiceImpl/AssessmentServiceImpl | follow `assertLessonOwnership` pattern + `LessonOwnershipSecurityTest` style tests; keep role semantics; never deny owner/admin |
| Enrollment | new student class-membership check | fallback across student_class_assignments→enrollments; only enforce when student record resolvable; test with seeded learners |
| Courses/Enrollment flows | none (course controller untouched) | — |
| Lessons | adding lesson_id columns (additive), notification wiring in create paths | notifications wrapped so failure can't fail the write path? (match existing LESSON_PUBLISHED pattern which is inline — keep same behavior) |
| **Live Classes** | add lesson_id field to entity | additive column, nullable; no behavior change; LiveKit tests must stay green |
| Notifications | new types appended | engine already stores free strings; no schema change |
| Progress | none | — |
| Academic Records | ReportCard.generate starts computing | only when endpoint called; additive; keep DRAFT default |
| **Existing Learner Experience** | submit endpoint body acceptance (was body-less), autosave endpoints, sidebar additions, placeholder replacement | body is additive (old callers send none → content null OK); placeholder replaced by real links; no routes removed |
| **Existing Teacher Experience** | new checks on update/delete/grade | owner/creator and admins still pass; tests added |
| **510 existing tests** | any service signature change | keep signatures backward-compatible (overload where needed); run full `mvn clean test` |
| Flyway numbering | new V110+ | verified next free number after V109; additive only |
| Prod schema validation (`ddl-auto: validate` in prod) | entity ↔ migration drift | every new column added in migration in same commit |

---

## AUDIT VERDICT (feeds Section 72/73)

- **EXISTS & working**: lessons CRUD/publish, resources (visibility-tested), notifications engine, audit infra, scales/boundaries, progress, live classes, all 5 teaching routes with real data.
- **BROKEN (fix this wave)**: assignment submit payload loss, question answer-key leak, assessment availability/max-attempt non-enforcement, report card empty computation, learner lesson quiz/assignment dead-end.
- **MISSING (build this wave, minimal & connected)**: gradebook aggregation endpoint, attempt autosave/resume, submission status/draft, lesson_id links (assignment/assessment/live), teaching notification types, audit call sites, rubric endpoints, scope/ownership checks.
- **REPORTED OUT (disclose, don't fake)**: learning outcomes entities, question bank+versioning, group assignments, assessment sections/randomization, moderation/multi-grader, configurable completion rules, partial-credit re-grade, comment/discussion system, full API-client consolidation.
