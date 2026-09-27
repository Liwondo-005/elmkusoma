# ELMKUSOMA — User, Frontend & Live Documentation

> **Developer 02 — User Ecosystem, Frontend & Live Learning**
> Complements `docs/README-01-SYSTEM-BACKEND.md` (Developer 01). This document covers **how people use ELMKUSOMA from the browser, how the frontend is structured, how Live Learning works, and exactly how to install, run and test the platform.** Backend internals are referenced, not duplicated.

---

## Table of Contents

1. [Frontend Stack](#1-frontend-stack)
2. [ELMKUSOMA User Experience](#2-elmkusoma-user-experience)
3. [Learner Ecosystem Overview](#3-learner-ecosystem-overview)
4. [Nursery Experience](#4-nursery-experience)
5. [Primary Experience](#5-primary-experience)
6. [Secondary Experience](#6-secondary-experience)
7. [Higher Education Experience](#7-higher-education-experience)
8. [Other Learner Experience](#8-other-learner-experience)
9. [Teacher / Lecturer / Instructor](#9-teacher--lecturer--instructor)
10. [Parent / Family](#10-parent--family)
11. [Institution / Provider Admin](#11-institution--provider-admin)
12. [Platform Admin](#12-platform-admin)
13. [Education Oversight](#13-education-oversight)
14. [Frontend Routing](#14-frontend-routing)
15. [Live Learning — Architecture](#15-live-learning--architecture)
16. [Live User Journey](#16-live-user-journey)
17. [Teacher Live Test](#17-teacher-live-test)
18. [Learner Live Test](#18-learner-live-test)
19. [Seminar / Event Live Test](#19-seminar--event-live-test)
20. [Multi-User Live Test Matrix](#20-multi-user-live-test-matrix)
21. [Media / Recording / Replay](#21-media--recording--replay)
22. [Courses and Learning](#22-courses-and-learning)
23. [Authentication Experience](#23-authentication-experience)
24. [Frontend Environment](#24-frontend-environment)
25. [Complete Local Setup](#25-complete-local-setup)
26. [Running ELMKUSOMA](#26-running-elmkusoma)
27. [Complete Testing Runbook](#27-complete-testing-runbook)
28. [Two-Browser Test](#28-two-browser-test)
29. [Troubleshooting](#29-troubleshooting)
30. [Mobile / Low Bandwidth / Accessibility](#30-mobile--low-bandwidth--accessibility)
31. [Frontend Status Matrix](#31-frontend-status-matrix)
32. [Documentation Completeness Check](#32-documentation-completeness-check)

---

## 1. Frontend Stack

All versions verified from `frontend/package.json` and config files.

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Framework | **Next.js** | 16.3.3 | App Router, SSR/SSG, `output: "standalone"`, Turbopack |
| UI Library | **React** | 19.x | Client components (`"use client"`), hooks |
| Language | **TypeScript** | 5.7.3 | Strict typing, `ignoreBuildErrors: false` |
| Package Manager | **npm** | — | `package-lock.json` committed; `cross-env` for scripts |
| UI Components | **shadcn** | 4.11.0 | Radix-based primitives in `components/ui/` |
| Base UI | **@base-ui/react** | 1.5.0 | Headless component primitives |
| Styling | **Tailwind CSS** | 4.3.3 + `@tailwindcss/postcss` 4.3.3 | Utility-first; `tw-animate-css` 1.4.0, `tailwind-merge` 3.3.1, `clsx` 2.1.1, `class-variance-authority` 0.7.1 |
| Forms | **react-hook-form** | 7.87.0 + `@hookform/resolvers` 5.9.1 + **zod** 4.5.4 | Schema validation (email, password, captcha) |
| Icons | **lucide-react** | 1.16.0 | Every nav item, card, and action |
| i18n | **next-intl** | 4.14.6 | `en` / `sw` locales; `messages/en.json`, `messages/sw.json` |
| State | **React Context + localStorage** | — | `AuthContext` in `lib/auth.tsx`; no Redux/Zustand |
| API Client | **Native `fetch`** wrappers | — | `lib/api.ts` (2134 lines), `lib/fetch.ts`, `lib/learner-api.ts` (507), `lib/college-api.ts`, `lib/nursery-api.ts` (281), `lib/secondary-api.ts` (325), etc. |
| Auth | **JWT (httpOnly cookie + localStorage)** | — | `elmkusoma_access_token` (86400s), `elmkusoma_current_user` JSON, `X-Institution-Id` header |
| Realtime | **WebSocket (native)** | — | `lib/use-realtime.ts` (69 lines) → `wss://host:8081/ws?token=&institutionId=&userId=` |
| Live Media | **LiveKit** | `livekit-client` 2.22.3 + `@livekit/components-react` 2.9.24 | SFU for video/audio/screen — only `components/live/live-classroom.tsx` uses imperative client |
| E2E Tests | **Playwright** | 1.63.0 | `e2e/`, `playwright.config.ts` |
| Analytics | **@vercel/analytics** | 1.6.1 | — |

**Key config:** `next.config.mjs` — `images.unoptimized: true`, `typescript.ignoreBuildErrors: false`, rewrites proxy `/v1/:path*` → `BACKEND_URL` (default `http://localhost:8080`), `/api/v1/media/:path*` → `MEDIA_URL` (`http://localhost:8083`), `/ws/:path*` → `REALTIME_URL` (`http://localhost:8081`). Middleware is `proxy.ts` (Next 15 `proxy()` pattern, not `middleware.ts`), guarding `/dashboard/:path*`.

**Hooks:** Only `hooks/use-locale.ts` — reads `NEXT_LOCALE` cookie.

---

## 2. ELMKUSOMA User Experience

```
DISCOVER  →  Public pages: /, /about, /courses, /schools, /live-classes
                ↓
REGISTER  →  /register — role (Student/Teacher/Parent/Other Learner),
                learningLevel (if Student), captcha, agreeToTerms
                ↓
AUTHENTICATE → /login — zod email+password, showPassword toggle,
                /forgot-password, email verification (backend)
                ↓
ROLE      →  Auth maps BACKEND↔FRONTEND (see §23):
                STUDENT→Student, TEACHER→Teacher,
                OTHER_LEARNER→Other Learner, PARENT→Parent,
                ADMIN→Admin, INSTITUTION_ADMIN→Institution Admin,
                NATIONAL/REGIONAL/DISTRICT_ADMIN→oversight
                ↓
LEARNING CONTEXT → learningLevel: NURSERY | PRIMARY | SECONDARY
                  | COLLEGE | VETA | UNIVERSITY
                  (+ secondaryStage/form for Secondary)
                ↓
WORKSPACE →  Dashboard routing (proxy.ts + layout.tsx + sidebar):
                Nursery → /dashboard/nursery
                Primary → /dashboard (generic, isPrimary branch)
                Secondary → /dashboard/secondary
                College/VETA/University → /dashboard/learner (HE branch)
                Other Learner → /dashboard/learner (Other Learner branch)
                Teacher → /dashboard/teacher
                Parent → /dashboard/parent
                Institution Admin → /dashboard/admin
                Platform Admin → /dashboard/platform-admin
                Oversight → /oversight
                ↓
LEARN     →  Courses → Modules → Lessons (videos, docs, links, text)
                ↓
PRACTICE  →  Assignments, labs, discovery, quests
                ↓
ASSESS    →  Assessments/quizzes/exams, auto-grading where supported
                ↓
PROGRESS  →  Dashboards, progress bars, academic records, portfolios
                ↓
LIVE / COLLABORATE → Live classes, events, breakout rooms, polls
                ↓
EVIDENCE  →  Portfolio items, logbook entries, milestones, evidence boards
                ↓
CERTIFICATE / NEXT STEP → Certificates (verify at /certificates/verify/[code]),
                transcripts, career profiles, next course/enrollment
```

Role routing is enforced in three layers: `proxy.ts` (route guard), `app/dashboard/layout.tsx` (`AuthGuard` + `DashboardSidebar`), and per-world `layout.tsx` (e.g. `nursery/layout.tsx` redirects unless `Student + NURSERY`).

---

## 3. Learner Ecosystem Overview

| World | Base Path | Layout Guard | Nav Items | Card Style | Greeting |
|-------|-----------|-------------|-----------|------------|----------|
| **Nursery** | `/dashboard/nursery` (25 routes) | `Student + NURSERY` | 25 flat | `colorful` | “Welcome to your fun learning world!” |
| **Primary** | `/dashboard` (generic, 20+ pages) | `Student + PRIMARY` branch of shared `page.tsx` | 30+ in 7 groups | `colorful` | “Welcome back, Explorer!” |
| **Secondary** | `/dashboard/secondary` (28 routes) | `Student + SECONDARY` | 15 flat + subs | `academic` | “Stay focused on your goals.” |
| **College / VETA** | `/dashboard/learner` (HE branch, 40+ routes) | `Student + COLLEGE|VETA` | 18 (8 groups) | `professional` | “Your College / VETA World” |
| **University** | `/dashboard/learner` (HE branch) | `Student + UNIVERSITY` | collapsible 8 groups | `professional` | “Your University World” |
| **Other Learner** | `/dashboard/learner` (Other Learner branch) | `Other Learner` | 11 (6 groups) | `professional` | “Welcome back! My Learning World” |

All worlds share `DashboardTopbar` (search, notifications, locale toggle, profile), `LowBandwidthProvider`, and `AuthGuard`. Sidebar is driven by `components/dashboard/dashboard-sidebar.tsx` (757 lines) + `lib/learner-config.ts` (`getDashboardConfig`, `getLearnerNavItems`).

---

## 4. Nursery Experience

> **Stack:** `lib/nursery-api.ts` (`nurseryFetch` with `X-Institution-Id` + Bearer). Types: `NurseryActivity` (`GAME|SONG|STORY|CRAFT|PHYSICAL|EDUCATIONAL`), `NurseryMilestone` (`PHYSICAL|COGNITIVE|SOCIAL|EMOTIONAL|LANGUAGE|MOTOR`, `PENDING|ACHIEVED|IN_PROGRESS|NOT_OBSERVED`), plus `NurseryStory`, `NurseryDailyQuest`, `NurseryFeelingsCheckin`, `NurseryMission`, `NurseryTanzaniaDiscovery`, `NurseryParentLearning`.

| Page (route suffix) | User Flow |
|---------------------|-----------|
| **My World** (`/nursery`) | Home dashboard: time-based greeting (`Sun/CloudSun/Moon`), discovery areas (Numbers/Animals/Letters), activity icons, live + milestones + stories via `nurseryApi` |
| **Learning Journey** (`/nursery/learning-journey`) | Filterable milestones by 6 categories, colored chips, `getMilestones(user.id)` |
| **Play** (`/nursery/play`) | Play & Learn hub: categories All Games / Fun Games / Sing Along / Move & Dance / Learn & Play; icon map `GAME/SONG/PHYSICAL/EDUCATIONAL` |
| **Stories** (`/nursery/stories`) | Built-in stories (“The Little Seed”, “Bouncy the Bunny”) with paginated `LocalStory.pages[]` + API stories; audio `Volume2/VolumeX` |
| **Discovery** (`/nursery/discovery`) | Static cards (The Sun, Rain…) with `facts[]` + `funFact`; icons `Sun/CloudRain/TreePine/Dog/Cat` |
| **Create** (`/nursery/create`) | Canvas drawing (10 colors, brush sizes, emoji stamps, Do-Re-Mi notes), tabs `draw|music` |
| **Music & Movement** (`/nursery/music`) | Song list + movement cards (Head Shoulders…), filter `SONG`, `playing` state |
| **Speak & Listen** (`/nursery/speak-listen`) | Story/educational filtered activities, `Headphones/Mic/Volume2` |
| **Movement Breaks** (`/nursery/movement`) | Timed breaks (Jumping Jacks 30s, Dance Party, Hop…) with countdown |
| **Daily Quest** (`/nursery/daily-quest`) | 5 built-in quests (Read Story, Count to 10, Draw, Sing, Help Someone) with points + `completed Set`; `getDailyQuests(classGroupId)` |
| **Milestones** (`/nursery/milestones`) | Cards by `CATEGORY_INFO` + `STATUS_INFO` (ACHIEVED/IN_PROGRESS/PENDING/NOT_OBSERVED) |
| **Feelings** (`/nursery/feelings`) | 8-emoji selector (HAPPY/CALM/EXCITED/TIRED/SAD/ANGRY/PROUD/WORRIED) → `createFeelingsCheckin({studentId, classGroupId, feeling, emoji})` |
| **Tanzania** (`/nursery/tanzania`) | 6 landmarks (Kilimanjaro, Serengeti, Zanzibar, Ngorongoro, Lake Victoria, Baobab) `facts[]`; `getTanzaniaTopics` |
| **Evidence / Portfolio / Backpack / Progress** (`/nursery/evidence`, `/portfolio`, `/backpack`, `/progress`) | Four distinct views over the same `milestone` data — grid of achieved milestones, categorized backpack with download simulation, progress bars per category |
| **Live** (`/nursery/live`) | Live classes list (`Radio/Video/Clock`) status `SCHEDULED|LIVE|COMPLETED`; `getLiveClasses(classGroupId)` |
| **Journey** (same as Learning Journey) + **Backpack** | See above |
| **My Teacher** (`/nursery/my-teacher`) | `NurseryTeacherInfo` primary teacher card + message textarea; `getMyTeachers(user.id)` |
| **Family** (`/nursery/family-learning`, `/nursery/learn-together`) | Suggested family activities (6) + classmates sharing circle via `teacherApi.getStudents()` |

All nursery pages are `"use client"`, `useRequireAuth`, `useTranslations("nursery")`, `LoadingState`, back link to `/dashboard/nursery`.

---

## 5. Primary Experience

> No `/dashboard/primary` folder — Primary is the **generic** `/dashboard/*` branch (guarded by `learningLevel==="PRIMARY"` + `primaryNavSections`).

**Sidebar (7 groups, from `dashboard-sidebar.tsx`):**

- **HOME:** My World (`/dashboard`)
- **LEARNING:** Learn (`/lessons`), Journey (`/journey`), Read (`/reading`)
- **PRACTICE ZONE:** Practice (`/assignments`), Create (`/create`), Assessments (`/assessments`), Discovery (`/discovery`), Labs (`/labs`)
- **CHALLENGES:** Quests (`/quests`), Challenge Zone*, Mistake Lab*, AI Guide (`/ai-guide`)
- **COLLABORATION:** Learn Together (`/learn-together`), Family (`/family`)*
- **TRACKING:** Live Learning (`/live-classes`), Progress (`/progress`), Evidence (`/evidence`), Passport (`/passport`), Attendance (`/attendance`)
- **ACCOUNT:** My Teacher (`/my-teachers`), Messages, Notifications (`/notifications`), Portfolio, Learning Profile (`/learning-profile`)*, Profile (`/profile`), Settings (`/settings`)

`*` = nav item defined but no `page.tsx` found — placeholder for future.

**Actual filesystem (verified via Glob):** `dashboard/page.tsx` (698 lines, shared — switches via `getDashboardConfig(level)`), `lessons/page.tsx` + `lessons/[id]/page.tsx` (filtered `learningApi.getLessonsByClass(classGroupId)` + `getStudentProgress`, subject pills for 8 subjects: Mathematics, English, Kiswahili, Science, Social Studies, Religious Education, Creative Arts, Physical Education), `assignments/*`, `assessments/*`, `journey`, `reading`, `discovery`, `labs`, `quests`, `create`, `ai-guide`, `live-classes`, `progress`, `evidence`, `passport`, `attendance`, `portfolio`, `learn-together`, `my-teacher(s)`, `messages`, `profile`, `settings`, plus `academic`, `speak-create`.

**Components:** `components/primary/` — `gamification.tsx`, `gamification-bar.tsx` (streak/points/badges via `Flame/Star/Award`), `teacher-info-card.tsx`, `breadcrumbs.tsx`, `loading-skeleton.tsx`, `permission-denied.tsx`, `low-bandwidth-provider.tsx`, `accessibility-wrapper.tsx`, `skip-to-content.tsx`.

**Flow:** Dashboard greeting + `GamificationBar` + subjects grid → **Learn** (lessons by subject pill) → open lesson → **Practice** (assignment) → **Discovery/Labs** → **Live** → **Progress/Evidence/Passport**.

---

## 6. Secondary Experience

> **Base:** `/dashboard/secondary` (28 routes), layout guard `Student + SECONDARY`, `lib/secondary-api.ts` (325 lines, `secondaryFetch`). Covers O-Level and A-Level via `secondaryStage`/`form`.

**Sidebar (15 flat items):** My Academic World (`/secondary`), Learn, Practice, Assess, Revision, Live, Projects, Research, Science Lab, Portfolio, Progress, Future World, My Teachers, Events (`/dashboard/learner/events` cross-link), Notifications.

| Area | Route | What the user sees |
|------|-------|--------------------|
| **My Academic World** | `/secondary` (554 lines) | Greeting + `getStageLabel(O_LEVEL/A_LEVEL + Form)`, academic pulse, continue learning (`getContinueLearning`), upcoming assessments/assignments, live classes, attendance |
| **Learn** | `/secondary/learn` | Subject explorer (`getSubjects(classGroupId)` → `subjects/[id]`), icons `BookOpen/FlaskConical/Brain`; sub-pages: `research` (4-step guide), `science-lab` (3 built-in experiments + `LONG_ANSWER` problems), `concept-explorer` (3 concepts + `getConceptsByClass`), plus `practical-learning`, `problem-solving`, `controlled-ai`, `exam-mode`, `critical-thinking`, `communication`, `error-analysis` (all exist) |
| **Practice** | `/secondary/practice` | Assignments via `getAssignments`, split pending (`PENDING/ACTIVE`) vs submitted (`SUBMITTED/GRADED`) |
| **Assess** | `/secondary/assess` | Assessments via `getAssessments`, upcoming (`SCHEDULED/PUBLISHED`) vs past (`COMPLETED/ENDED`) |
| **Revision** | `/secondary/revision` | Hub of 9 quick actions (Assessments, Recently Studied, Practice Sets, Exam Prep, Study Planner, Concept Explorer, Problem Solving, Error Analysis, Exam Mode); `exam-prep` combines assessments+subjects timetable; `study-planner` CRUD `getStudyPlans/createStudyPlan/updateStudyPlan` with `topicName/plannedDate/durationMinutes/priority/notes` |
| **Live** | `/secondary/live` | `getLiveClasses()` → `Radio/Video/Clock` cards |
| **Projects** | `/secondary/projects` | 6-step workflow + link to `projects/passport` |
| **Progress** | `/secondary/progress` | Attendance rate bar via `getAttendance()` |
| **Portfolio** | `/secondary/portfolio` | Subjects + attendance snapshot |
| **Future World** | `/secondary/future` | Career paths (University/TVET/Career/Skills) + 10 fields (Medicine, Engineering, CS, Business…) |

All secondary pages: `"use client"`, `classGroupId` guard, `LoadingState`, `useTranslations("secondary")`.

**O-Level vs A-Level:** Supported via `User.secondaryStage` (`O_LEVEL | A_LEVEL`) and `form` field; `getStageLabel()` displays Form I–VI accordingly. Content filtering is shared; no separate O-Level/A-Level content route — stage influences label and future content gating.

---

## 7. Higher Education Experience

> **Base:** `/dashboard/learner` — HE branch when `Student + COLLEGE|VETA|UNIVERSITY`. Shares files with Other Learner but `isHE = COLLEGE|UNIVERSITY` branch inside `learner/page.tsx`. HE adds `lib/college-api.ts` (183 lines) + `lib/types/college.ts` (551 lines) on top of `learner-api.ts`.

**Sidebar variants:**

- **College / VETA:** `collegeNavSections` (8 groups, 18+ items) — OVERVIEW, MY COLLEGE, LIVE & MEDIA, BUILD & DISCOVER, PRACTICAL, ACADEMICS, MY WORLD, CONNECT, ACCOUNT
- **University:** `universityNavSections` — collapsible `NavSection` with children: My Learning → My Courses / Continue / Resources / Practical Lab / Assessments …; Live Campus → Live Now / Media Library …; Build & Discover → Research / Thesis …; Academics → Calendar / Competencies / Events …; My World → Fieldwork / Certificates …; plus Deep Learning, Course Workspace, Academic Assistant

**Verified routes (61 Glob — key samples):**

| Route | Purpose |
|-------|---------|
| `/learner` (857 lines) | Dual dashboard: HE → `collegeApi.getHEDashboard(studentId, level)` (Today Items, Study Tasks, Research, Live Sessions, Academic Pulse); Other Learner → `learnerApi.getCourses/getBookmarks + getLastAccessedLesson()` |
| `courses`, `courses/[id]`, `courses/[id]/lessons/[lessonId]` | Course marketplace (search + level/category filter, enroll CTA, `isEnrolled()`), detail with expandable modules + lazy `getModuleLessons`, lesson player with `CourseLessonDetail {completed, progressPercentage, previous/nextLessonId, totalLessons, currentIndex}` + `completeLesson` + `setLastAccessedLesson` |
| `modules`, `module-workspace`, `course-workspace` | Workspace views: `getLearnerModules(userId)` with `moduleTitle/moduleCode/creditHours/progressPercent/completedLessons/totalLessons/grade/semester` |
| `research`, `thesis` | `getStudentResearch` with 12-status badge (`IDEA→COMPLETED`), `getStudentTheses` with 7-status (`PROPOSAL…DEFENSE_COMPLETE`) |
| `projects`, `fieldwork` | `getStudentProjects` (`IDEATION→ARCHIVED`), `getStudentFieldwork` with `PlacementStatusBadge` (`PLANNING|ACTIVE|COMPLETED|TERMINATED`) + `LogbookEntry` |
| `live-campus` (787 lines) | Rich live hub: `LiveClassItem` + `sessionType`, filters `all|live|upcoming|recordings`, `canJoin`, `recordingUrl`, `joinLiveSession` |
| `competencies` | `listCompetencies + getStudentCompetencies + getCompetencySummary`, status `NOT_STARTED/LEARNING/PRACTICING/ASSESSED/COMPETENT` |
| `practical-lab` | Filters `StudyTask` where `taskType PROJECT|ASSIGNMENT|RESEARCH` |
| `academic-progress`, `academic-record` | `getAcademicRecord + getStudentEnrollments + getCompetencySummary` |
| `my-learning`, `resources/[id]`, `media-library`, `video-library`, `replays/[id]` | Enrollments filtered `all/in-progress/completed`, paginated `getResources({page,size:20})`, media `mediaApi.list()` by `mediaType`, `getReplays/getReplayProgress/updateReplayProgress(positionSeconds)` |
| `assessments` | Derives from enrollments→modules→lessons (`contentType QUIZ`) |
| `events/[id]`, `events/[id]/preflight`, `events/[id]/waiting`, `events/registered` | Event marketplace + preflight/waiting rooms (see §19) |
| `career`, `professional-dev` | `getCareerProfile/upsertCareerProfile` (`careerObjective/targetIndustry/targetRole/skills`), `getLearnerProfessionalDev` |
| `portfolio`, `evidence`, `demonstrations`, `collaborations`, `software-tools`, `workshops`, `knowledge-discovery`, `search`, `deep-learning`, `academic-assistant`, `my-learning-kit` | HE portfolio ecosystem: portfolios/demonstrations submit/review, collaborations, workshops, knowledge search (`learnerApi.search(q,type,filters)`), deep learning content, AI assistant |

**VETA/College vs University:** `VETA` variant mirrors College without Research/Deep Learning sub-nav. `lib/learner-config.ts` documents `VETA cardStyle:"professional"` without `Research, Deep Learning`. Verified: `universityNavSections` children include `Research, Thesis, Deep Learning, Calendar, Course Workspace`; `collegeNavSections` omit those. Shared foundation (courses, modules, live, projects, fieldwork, competencies) is identical.

---

## 8. Other Learner Experience

> **Same files as HE** (`/dashboard/learner/*`) but **Other Learner branch** inside `layout.tsx` + `page.tsx` + sidebar.

**Role mapping (`lib/auth.tsx`):** `OTHER_LEARNER ↔ "Other Learner"` frontend. `lib/learner-config.ts`:

```ts
function getDashboardConfig(level, role){
  if(role==="Other Learner") return otherLearnerConfig // "Welcome back! My Learning World"
  return dashboardConfigs[level]
}
```

**Learner nav (11 items, 6 groups — `learnerNavSections`):** OVERVIEW: My Learning World (`/learner`), LEARNING: My Learning (`/learner/my-learning`), DISCOVER: Resources, Explore Courses, Live Classes, Events & Workshops, KNOWLEDGE: Bookmarks, History, MY PROGRESS: Certificates, Notifications, ACCOUNT: Profile.

**What “Other Learner” sees vs Student (HE):**

| Capability | Other Learner (Adult / Lifelong) | Student (HE) |
|------------|----------------------------------|--------------|
| Courses | Self-paced marketplace, `learnerApi.getCourses` → enroll → modules/lessons | Same + `collegeApi` programmes/competencies overlay |
| Projects / Research / Thesis / Fieldwork | Not shown (empty/error for non-HE) | Full CRUD with status badges |
| Competencies | Not shown | `NOT_STARTED→COMPETENT` tracking |
| Live | `live-classes` + `live-campus` (join via `learnerApi`) | Same + HE live-campus |
| Portfolio/Evidence | Basic (bookmarks, certificates, history) | Full portfolios/demonstrations/logbook |
| Certificates | `learnerApi.getCertificates()` | Same + `collegeApi` academicRecord |
| Professional Dev / Career | Not shown | `career/profile`, `professional-dev` |

**Learning focus (non-formal):** Short courses, skills, professional development, business, technology, entrepreneurship, life skills, general education — all via the course marketplace (`category` filter). No classGroup/academicYear enrollment; self-enrollment via `POST /v1/learner/me/enrollments {courseId}`.

---

## 9. Teacher / Lecturer / Instructor

> **Base:** `/dashboard/teacher` (20 subdirs), also `/dashboard/lecturer` (2 routes: `courses`, `live-dashboard`). Role: `Teacher | Instructor` (`isTeacher` guard).

**Dashboard** (`teacher/page.tsx`, 875 lines): Greets `firstName`, fetches `apiTeacher.getDashboard()`, `teacherFetch("/v1/teachers/me/analytics")`, `learningApi.getAssignments + assessmentApi.getByClass` per class, `teacherApi.getClasses() + getStudentsByClass()` for counts, `getLiveClasses()` filtered to today. Sections: Command Center (pendingGrading, pendingSubmissions, upcomingDeadlines, todayLiveClasses, attendanceNotTaken), Key Metrics (Students, Classes, Assignments, Pending Grading), Today's Classes, My Classes grid (enrolledStudents, link to `classes/{id}`), Live Classes (LIVE NOW / UPCOMING), Recent Activity, Needs Grading (→ `/grading`), Quick Actions (8 links: Create Assignment, Create Lesson, Create Assessment, Start Live Class, Take Attendance, Grade Submissions, View Schedule, View Analytics), Recent Assignments/Assessments (5 each), Teaching Insights.

**Sidebar (`teacherNavSections`):** OVERVIEW: Dashboard; WORKSPACE: My Classes, Courses, Students, Learner Support; TEACHING: Lessons, Assignments, Assessments, Grading; CLASS MANAGEMENT: Attendance, Gradebook, Schedule; COMMUNICATION: Live Classes, Media Library, Messages, Announcements, Notifications; INSIGHTS: Analytics, Reports; ACCOUNT: Settings.

**Key pages verified:**

- **Assignments** (`teacher/assignments`, 774 lines): CRUD types `ESSAY, MULTIPLE_CHOICE, FILL_IN_THE_BLANK, MATCHING, FILE_UPLOAD`; fields `title*, description, instructions, classGroupId*, subjectId*, dueDate, totalMarks, status DRAFT/PUBLISHED, attachments`; fetches classes via `appFetch("/v1/teachers/me/classes")`, per-class `GET /v1/learning/assignments/class/{classGroupId}`; filters (class, status), edit `PUT /v1/learning/assignments/{id}`, delete, submissions `GET /v1/learning/assignments/{id}/submissions`, grade `PUT /v1/learning/submissions/{id}/grade?grade=&feedback=`.
- **Attendance** (`teacher/attendance`, 635 lines): Tabs Mark + History. Mark: select class + date, bulk Mark All Present/Absent, per-student 4 status (PRESENT/PRESENT-active teal, ABSENT red, LATE amber, EXCUSED primary), progress `markedCount/totalCount`, submit `attendanceApi.bulkMark({classGroupId, attendanceDate, records})`. History: date range, `GET /v1/attendance?classId=&date=`, summary map + tables.
- **Grading** (`teacher/grading`, 323 lines): Tabs Grading Scales vs Report Cards. Scales via `teacherApi.getGradingScales()` → `gradeBoundaries` table (Grade, Min, Max, GPA, Remarks). Reports: select class → `getStudentsByClass` then `getReportCardsByStudent`, summary Students/Published/Average Mark, Export CSV.
- **Analytics** (`teacher/analytics`): `teacherFetch("/v1/teachers/me/analytics")` → 6 stat cards + Upcoming Deadlines + Recent Submissions.
- **Live Classes** (`teacher/live-classes`, 774 lines): 14 session types incl. `COMPETENCY_ASSESSMENT`; fields `title*, description, sessionType, scheduledAt*, duration, maxParticipants, classGroupId, subjectId, enableRecording, lobbyEnabled, timezone (12 options), isRecurring + recurrencePattern (DAILY/WEEKLY/BIWEEKLY/MONTHLY)`; Review & Schedule → Confirm. Status actions: SCHEDULED → Prepare (`/teacher/live-classes/[id]/prepare`), Start Live, Edit, Cancel; IN_PROGRESS/LIVE → Open Classroom (`/live-classes/[id]`), End; COMPLETED/ENDED/CANCELLED → View Recording/Delete. Detail page lazy `GET /v1/teachers/me/live-classes/{id}/participants`. Prepare page: sequential device checks (video getUserMedia, audio, AudioContext, `/api/health` latency, `GET /v1/live-session/health` LiveKit).
- **Lecturer Live Dashboard** (`lecturer/live-dashboard`, 1071 lines): Polling every 30s, filters `ALL|LIVE|UPCOMING|COMPLETED`, summary Total/Upcoming/Live/Completed, create `POST /api/v1/teachers/me/live-classes`, delete `DELETE`, start `POST /{id}/start`, end `POST /{id}/end`, `canStartSession` window (15min before to 60min after), Schedule form with timezone + session types.

**API:** `lib/teacher-api.ts` (`teacherFetch` with `X-Institution-Id` + Bearer). Endpoints: `getClasses (/v1/teachers/me/classes)`, `getStudentsByClass (/v1/students?classId=)`, `getProfile`, `getAssignmentsByClass/createAssignment/getSubmissions/gradeSubmission`, `getGradingScales/getReportCardsByTerm/getReportCardsByStudent`, `bulkMarkAttendance`, etc.

**Known issue — Students missing from teacher list:**

| Symptom | Students register successfully but teacher’s Students/Classes pages show 0 or stale counts |
|---------|----------------------------------------------------------------------------------------------|
| What to inspect | `teacherApi.getStudentsByClass(classId)` → `GET /v1/students?classId=` ; compare with `GET /v1/students` (all). Check backend `StudentRepository.findByClassGroupId`, check `StudentClassEnrollment` / `classGroupId` assignment on registration (does `register` set `classGroupId`? Check `lib/auth.tsx` register payload). Also check `GET /v1/teachers/me/classes` — teacher must have `TeacherAssignment` to that classGroup. |
| Likely location | `frontend/lib/teacher-api.ts: getStudentsByClass`, `frontend/app/dashboard/teacher/students/page.tsx`, `frontend/app/dashboard/teacher/classes/[id]/page.tsx`; backend `TeacherAssignment` + `StudentClassEnrollment` |
| Status | **Observed and documented — not hidden.** Workaround: ensure student’s `classGroupId` is set via Admin → People or Institution Admin → Organization; ensure teacher is assigned to same `classGroupId` via Admin. |
| Verified solution | None committed yet — this requires backend enrollment linking. Frontend correctly calls `getStudentsByClass`; the gap is data seeding/assignment, not UI. |

---

## 10. Parent / Family

> **Base:** `/dashboard/parent` (23 subdirs + `parent/page.tsx` 413 lines), plus `/dashboard/family/page.tsx` (92 lines, family collaboration inside primary). Role: `Parent`. API: `lib/parent-api.ts`.

**Dashboard** (`parent/page.tsx`): `parentApi.getOverview()`, `getChildren()`, `getChildIntelligence(childId)`. Types: `FamilyOverview`, `ChildOverview` (`studentId, studentName, className, relationshipType, isPrimary, attendancePercentage, learningProgress, pendingAssignments, latestGrade, latestAverage, overdueAssignments`), `ParentIntelligence` (weeklyBrief, `needsAttention[]`, `doingWell[]`, `upcoming[]`, `recommendations[]`). UI: greeting by time, Primary School CTA if `hasPrimaryChild → /parent/primary-progress`, child selector tabs (highlights `isPrimary`), 4 child-overview cards (Attendance %, Learning Progress %, Pending Tasks, Latest Grade/Avg), Needs Your Attention (red border, `AttentionCard` priority CRITICAL red / IMPORTANT orange / NORMAL blue), Doing Well (green), This Week (lessonsCompleted, assignmentsCompleted, liveClassesAttended, assessmentsCompleted), Upcoming (type badges LIVE_CLASS blue / ASSIGNMENT orange / ASSESSMENT purple / SCHOOL_EVENT teal), Quick Actions (Learning, Progress, Assessments, Activity, Teachers, Calendar), How You Can Help, My Children grid (Attendance/Grade/Overdue → `/parent/child/{id}`).

**Sub-pages (all `page.tsx` exist, pattern verified):** `achievements`, `activity`, `assessments`, `assignments`, `attendance`, `calendar` (via `getChildCalendar`), `children` + `child/[id]`, `goals` (`getChildGoals` active/completed + progress bars), `learning` (tabs per child: Recommendations, `getChildLiveClasses`, `getLibrary` family library by category), `library`, `live-classes`, `messages`, `notifications`, `payments`, `primary-progress` (kid-friendly view linked from hero), `reports` (177 lines — `getChildSubjectPerformance + getChildLearningProgress`, summary Subjects count + Average GPA + Courses, subject rows with progress bar + grade badge + trend IMMROVING/DECLINING), `results`, `services`, `support`, `teachers`.

**Sidebar (`parentNav`, 19 items):** Dashboard, Learning, Progress (`/reports`), Assessments, Activity, Attendance, Assignments, Results, Achievements, Goals, Teachers, Calendar, Live Classes, Library, Payments, Services, Support, Notifications, Settings.

**Family collaboration** (`/dashboard/family`): `primaryApi.getCollaborations()` filtered `FAMILY`, static activities grid (Family Quiz Night, Read Together, Science at Home, Art & Craft, Music Time), Recent Family Activities (partnerName, isCompleted Done badge), gradient header + Share with Family (copies `window.location.origin + "/dashboard/lessons"`).

---

## 11. Institution / Provider Admin

> Two admin experiences: **Institution Admin** (`/dashboard/admin`, 14 entries) and **NFE Provider** (`/dashboard/provider`, 9 entries).

**Institution Admin** (`/dashboard/admin`): `adminApi.getEnhancedDashboard(institutionId)` where `institutionId = getInstitutionId()` (localStorage `elmkusoma_institution_id`). Error if no context. Sections: Attention Items (amber, `AlertTriangle`, count badge, → `actionUrl`), Quick Actions (People, Roles, Profile, Import, Settings, Audit), Institution Overview (6 stat cards → `/admin/people|: totalStudents, totalTeachers, totalParents, activeStudents, certificatesIssued, pendingImportJobs), Course Management (6 cards: totalCourses, publishedCourses, draftCourses, totalModules, totalLessons, liveClassesScheduled), Recent Activity (dot list), Enabled Services pills. Header shows `institutionName — Overview`.

**Sub-pages:** `people` (user table), `profile` (organization), `institutions`, `programmes`, `departments`, `competencies`, `roles`, `audit`, `events` (474 lines — pagination 10/page, search+status/type filters, bulk publish/cancel, rows title/type/status/date/registrations/recording, actions Edit `/admin/events/{id}/edit`, View Summary, Publish if DRAFT, Start/End Live, viewRecording, cancel, delete), `events/[id]/edit`, `events/new`, `events/{id}/summary`, `import` (CSV), `settings`, `live-operations`, `courses`, `audit`.

**Sidebar (`adminNav`, 14 items):** Platform Admin (`/platform-admin`), Administration (`/admin`), People, Organization (`/admin/profile`), Institutions, Programmes, Departments, Competencies, Roles, Audit Log, Events, Data Import, Settings. `isAdmin = Admin || Institution Admin`.

**Provider (NFE — Non-Formal Education)** (`/dashboard/provider`, 9 entries): `GET /v1/nfe/providers/stats` with `Authorization + X-Institution-Id`. `ProviderStats`: totalPrograms/activePrograms, totalLearners/activeLearners, totalSessions/completedSessions, totalCertificates, totalProviders/activeProviders. 4 cards (Providers, Programs, Learners, Sessions) + Certificates big stat + Quick Actions: Manage Programs, View Sessions, Manage Learners. Sub-pages: `programs`, `sessions`, `learners`, `assessments`, `attendance`, `certificates`, `materials`, `settings`.

---

## 12. Platform Admin

> **Base:** `/dashboard/platform-admin` (36+ entries), layout guard `role !== "Admin" → redirect to /dashboard` (`platform-admin/layout.tsx`, 36 lines). Sidebar: `components/dashboard/platform-admin-sidebar.tsx` (147 lines, 15 sections).

**Command Center** (`platform-admin/page.tsx`, 260 lines): Imports `platformAdminApi` (`getEnhancedDashboard()`, `getAttention()`, `getActivity(0,10)`, `getHealth()`). Hero gradient, Refresh + Attention badge. 4 Primary KPIs: Total Users (totalStudents+totalTeachers educators), Institutions, Live Classes (activeLiveClasses), Certificates (Issued & verified) — each `PrimaryKpi` link; 4 Operational: Open Incidents (red), Pending Verification (amber), Active Services (teal), Notifications (violet); Attention Required amber gradient (`AttentionCard` severity HIGH red ShieldAlert / MEDIUM amber AlertTriangle / INFO blue Bell → `actionUrl`); Activity Feed 8 rows (action color CREATE emerald / UPDATE blue / DELETE red / LOGIN violet, entityType badge, actorName, createdAt); Health Panel (DB + API dots Operational emerald / Degraded amber / Failing red, Active Users / Active Institutions); Ecosystem Overview (Institutions, Providers pending, Live·Certificates, Commerce payments); Quick Actions 8: Users, Institutions, Providers, Services, Verifications, Live, Incidents, Search; bottom Security → `/security`, Configuration → `/config`, Audit → `/audit`.

**Sidebar sections (15):** COMMAND CENTER: Dashboard, Attention; IDENTITY & ACCESS: Users, Admins, Delegations (Key icon); ECOSYSTEM: Institutions, Providers, Organizations, Services; LEARNING: Courses, Live Classes, Certificates, Entitlements; LIVE & EVENTS: Events, Streaming; MEDIA & RESOURCES: Media, Resources; COMMERCE: Payments, Packages; TRUST & SAFETY: Verifications, Moderation; COMMUNICATION: Notifications; INTELLIGENCE: Analytics; SECURITY: Security, Audit Logs; OPERATIONS: Incidents, Integrations, Health; DATA: Governance; PLATFORM: Configuration, Lifecycle; SUPPORT: Cases, Search; + footer Platform Status (DB/API health via `getHealth`).

**Sample sub-page — Users** (`platform-admin/users/page.tsx`, 197 lines): Paginated `PageResponse<UserSummary>`, PAGE_SIZE 20, role filter `["", STUDENT, TEACHER, PARENT, OTHER_LEARNER, ADMIN, INSTITUTION_ADMIN, PROVIDER_ADMIN]`, search by name/email. Table: Name (link to `/users/{id}`), Email, Role badge, Status Active green vs Suspended red, Created (en-GB), Actions Suspend/Activate via `updateUserStatus` with spinner. All 36 sub-pages follow same `platformAdminApi` pattern.

**Full route inventory:** `admins`, `analytics`, `attention`, `audit`, `certificates`, `communications`, `config`, `courses`, `data`, `delegations`, `entitlements`, `events`, `health`, `incidents`, `institutions/[id]`, `integrations`, `lifecycle`, `live-classes`, `media`, `moderation`, `organizations`, `packages`, `payments`, `providers/[id]`, `resources`, `search`, `security`, `services`, `settings`, `streaming`, `support`, `users/[id]`, `verifications`.

---

## 13. Education Oversight

> **Base:** `/oversight` (11 entries), `layout.tsx` uses `AuthoritySidebar` + `DashboardTopbar` + `AuthGuard`, `lg:pl-64`. Sidebar `authority-sidebar.tsx` (133 lines).

**Jurisdiction model (verified in `auth.tsx` + `oversight/page.tsx`):**

| Level | Frontend Role | Jurisdiction Token | Access |
|-------|---------------|--------------------|--------|
| National | `National Admin` | `national` | All regions/districts/schools |
| Regional | `Regional Admin` | `regionId` | That region’s districts/schools |
| District | `District Admin` | `districtId` | That district’s schools |
| Ward | — | — | Not separately implemented (district subsumes) |
| School | — | — | Viewed but not a separate role |

`AuthUser { regionId, districtId }` populated on login; sidebar guard `isAuthority = National|Regional|District Admin` else hidden. Dashboard fetches `GET /v1/oversight/dashboard` with Bearer — **jurisdiction-based**; the backend filters by the caller’s `regionId/districtId`. Access is NOT unrestricted — unverified claim of full cross-jurisdiction access would be false.

**Command Center** (`oversight/page.tsx`, 221 lines): `OversightDashboardStats` (`totalInstitutions, totalTeachers, totalStudents, totalUsers, totalRegions, totalDistricts, activeLiveClasses, totalLessons, totalClasses, attendanceRate, averagePerformance, curriculumProgress, alertsCount, topRegions[], recentAlerts[], jurisdictionSummary{type: national|region|district, name, code}`). UI: header `Education Oversight Command Center` + jurisdictionLabel + user.name; 12 StatCards in 3 grids of 4 (Institutions blue, Teachers green, Students purple, Classes orange, Live Classes red, Attendance teal, Avg Performance indigo, Curriculum pink, Active Alerts orange, Total Lessons gray, Regions cyan, Districts lime); Top Regions table (Region, Code, Institutions, Teachers, Students, Attendance%, Performance%); Recent Alerts rows (severity HIGH red / MEDIUM orange / YELLOW yellow, message + `institutionName•timestamp`).

**Schools** (`oversight/schools/page.tsx`, 158 lines): `GET /v1/oversight/schools → {institutions: Institution[]}` where `Institution` adds `teacherCount, studentCount, classCount, lessonCount, activeLiveClasses, attendanceRate, averagePerformance, curriculumProgress, address, city, regionName, districtName`. Table 12 columns: Name (→ `/oversight/schools/{id}`), Code, Type, Location (city/district/region), Teachers, Students, Classes, Attendance%, Performance%, Curriculum%, Status, Actions View. Other pages: `performance`, `attendance`, `curriculum`, `assessments`, `live-classes`, `reports`, `alerts`, `schools/[id]`.

**Sidebar:** `authority-sidebar.tsx` — Overview (`/oversight`), Schools, Performance, Attendance, Curriculum, Assessments, Live Classes, Reports, Alerts; Account: Profile, Settings; Logout → `/login`.

---

## 14. Frontend Routing

All routes verified via `find app -type d` and `Read` page guards. **Never-invented** list.

### Public

| Route | File | Access |
|-------|------|--------|
| `/` | `app/page.tsx` | Public |
| `/about` | `app/about/page.tsx` | Public |
| `/contact` | `app/contact/page.tsx` | Public |
| `/courses` + `/courses/colleges-universities`, `/courses/colleges-universities/[college]/[faculty]`, `/courses/veta/[trade]/[level]`, `/courses/[level]/[className]/[id]` | `app/courses/**` | Public |
| `/schools`, `/schools/nursery/[id]`, `/schools/primary/[id]`, `/schools/secondary/[id]`, `/schools/vocational/[id]`, `/schools/universities/[id]` | `app/schools/**` | Public |
| `/live-classes`, `/live-classes/[id]` | `app/live-classes/**` | Public browser + auth-gated classroom router |
| `/certificates/verify/[code]` | `app/certificates/verify/[code]/page.tsx` | **Public** (no auth) — verification |
| `/privacy`, `/terms`, `/support`, `/notes-library` | `app/*` | Public |
| `*` → `/not-found.tsx` | `app/not-found.tsx` | Public 404 |

### Authentication

| Route | File | Guard |
|-------|------|-------|
| `/login` | `app/login/page.tsx` | zod email+password, `useAuth().login`, redirect `?redirect=` else `/dashboard`, links to `/register`, `/forgot-password` |
| `/register` | `app/register/page.tsx` | 469 lines; roles Student/Teacher/Parent/Other Learner; `learningLevel` if Student; 5-digit captcha rotated/skewed; `useAuth().register` → Dashboard |
| `/forgot-password` | `app/forgot-password/page.tsx` | Public |

### Learner

See §4–8. Consolidated:

- **Nursery:** `/dashboard/nursery` + 24 children (`learning-journey`, `play`, `stories`, `discovery`, `create`, `music`, `speak-listen`, `movement`, `daily-quest`, `milestones`, `feelings`, `tanzania`, `evidence`, `portfolio`, `backpack`, `progress`, `profile`, `missions`, `family-learning`, `learn-together`, `my-teacher`, `live`, `notifications`)
- **Primary:** `/dashboard` (generic) + `lessons/[id]`, `assignments/[id]`, `assessments/[id]`, `journey`, `reading`, `discovery`, `labs`, `quests`, `create`, `ai-guide`, `live-classes`, `progress`, `evidence`, `passport`, `attendance`, `portfolio`, `learn-together`, `my-teacher(s)`, `messages`, `profile`, `settings`, `academic`, `speak-create`
- **Secondary:** `/dashboard/secondary` + 27 children (`learn/*` 9 subs, `practice`, `assess`, `revision/exam-prep/study-planner`, `live`, `projects/passport`, `portfolio`, `progress`, `future`, `subjects/[id]/topics/[topicId]`, `notifications`, `teachers`)
- **Higher Ed + Other Learner:** `/dashboard/learner` + 60 children (see §7–8 inventory; key: `courses/[id]/lessons/[lessonId]`, `courses`, `modules`, `module-workspace`, `course-workspace`, `research`, `thesis`, `projects`, `fieldwork`, `live-campus`, `live-classes`, `competencies`, `practical-lab`, `academic-progress`, `my-learning`, `resources/[id]`, `media-library`, `video-library`, `replays/[id]`, `assessments`, `certificates`, `progress`, `events/[id]/preflight/waiting/registered`, `career`, `portfolio`, `search`, etc.)

### Teacher / Lecturer

| Route | File |
|-------|------|
| `/dashboard/teacher` | `teacher/page.tsx` |
| `/dashboard/teacher/{analytics,announcements,assessments,assignments,attendance,classes/[id],courses,gradebook,grading,learner-support,lessons,live-classes/[id]/prepare,media-library,messages,notifications,reports,schedule,settings,students}` | 20 subdirs |
| `/dashboard/lecturer`, `/dashboard/lecturer/courses`, `/dashboard/lecturer/live-dashboard` | `lecturer/**` |

### Parent

| Route | File |
|-------|------|
| `/dashboard/parent` | `parent/page.tsx` |
| `/dashboard/parent/{achievements,activity,assessments,assignments,attendance,calendar,children,child/[id],goals,learning,library,live-classes,messages,notifications,payments,primary-progress,reports,results,services,support,teachers,settings}` | 23 entries |
| `/dashboard/family` | `family/page.tsx` |

### Institution / Provider Admin

| Route | File |
|-------|------|
| `/dashboard/admin` + 13 children (`audit`, `competencies`, `courses`, `departments`, `events/{new,[id]/edit,[id]/summary}`, `import`, `institutions`, `live-operations`, `people`, `profile`, `programmes`, `roles`, `settings`) | `admin/**` |
| `/dashboard/provider` + 8 children (`assessments`, `attendance`, `certificates`, `learners`, `materials`, `programs`, `sessions`, `settings`) | `provider/**` |

### Platform Admin

| Route | File |
|-------|------|
| `/dashboard/platform-admin` + 35 children (`admins`, `analytics`, `attention`, `audit`, `certificates`, `communications`, `config`, `courses`, `data`, `delegations`, `entitlements`, `events`, `health`, `incidents`, `institutions/[id]`, `integrations`, `lifecycle`, `live-classes`, `media`, `moderation`, `organizations`, `packages`, `payments`, `providers/[id]`, `resources`, `search`, `security`, `services`, `settings`, `streaming`, `support`, `users/[id]`, `verifications`) | `platform-admin/**` |

### Oversight / Authority

| Route | File |
|-------|------|
| `/oversight` + 9 children (`alerts`, `assessments`, `attendance`, `curriculum`, `live-classes`, `performance`, `reports`, `schools/[id]`) | `oversight/**` |

### Live

| Route | File |
|-------|------|
| `/live-classes` | `app/live-classes/page.tsx` (public browser) |
| `/live-classes/[id]` | `app/live-classes/[id]/page.tsx` (AuthGuard → `LiveClassroom` router) |
| `/dashboard/teacher/live-classes/[id]` | Teacher session detail |
| `/dashboard/teacher/live-classes/[id]/prepare` | Device preflight |
| `/dashboard/learner/events/[id]/preflight`, `waiting` | Event preflight/waiting rooms |

### Events

| Route | File |
|-------|------|
| `/dashboard/learner/events` | Learner events marketplace |
| `/dashboard/learner/events/[id]`, `[id]/preflight`, `[id]/waiting`, `registered` | Detail + preflight + waiting + registered list |
| `/dashboard/learner/events/registered` | Registered events list |
| `/dashboard/admin/events`, `admin/events/new`, `admin/events/[id]/edit`, `admin/events/[id]/summary` | Admin event CRUD |

### Media

| Route | File |
|-------|------|
| `/dashboard/learner/media-library` | Media assets (`VIDEO|DOCUMENT|AUDIO|IMAGE|RECORDING`) |
| `/dashboard/learner/video-library` | Video resources modal player |
| `/dashboard/learner/replays`, `replays/[id]` | Replay library + viewer with progress |

---

## 15. Live Learning — Architecture

```
Browser
 ├─ Next.js 16.3.3 (port 3000)
 │   ├─ LiveKit JS  livekit-client@2.22.3 + @livekit/components-react@2.9.24
 │   ├─ REST → BACKEND_URL (elmkusoma-core:8080) via learnerApi / appFetch
 │   └─ WebSocket → elmkusoma-core (8080) or elmkusoma-realtime (8081)
 ├─ LiveKit SFU  (7880 TCP, 7881 RTC, 50000-60000 UDP) + Redis
 ├─ elmkusoma-core:8080  (Spring Boot + Postgres, Redis, RabbitMQ)
 │       live-classes CRUD, start/end, health, join/token mint
 ├─ elmkusoma-realtime:8081  (WebSocket gateway — presence, chat, hand-raise, poll/quiz)
 ├─ elmkusoma-media:8083  (MinIO — recordings / VOD)
 └─ Postgres 16, Redis 7, RabbitMQ 3.12, Minio
```

**`livekit.yaml` (repo root):**

```yaml
port: 7880
rtc: { tcp_port: 7881, port_range: [50000, 60000], use_external_ip: false }
redis: { address: redis:6379 }
keys: { devkey: devsecret }
room: { auto_create: true, empty_timeout: 300, max_participants: 10000 }
```

> **Note:** `docker-compose.yml` defines 8 services (`postgres`, `redis`, `rabbitmq`, `minio`, `elmkusoma-core`, `elmkusoma-realtime`, `elmkusoma-workers`, `elmkusoma-media`, `nginx` optional profile, `frontend`) but **no `livekit` service** — LiveKit is expected externally or via `infrastructure/nginx/nginx.conf`. Frontend dep is installed and `livekit.yaml` exists, but container is missing unless added (see §29).

**API surface:**

| Client | Base | Auth | Live endpoints |
|--------|------|------|----------------|
| `learnerApi` | `NEXT_PUBLIC_API_URL` | `Authorization: Bearer` + `X-Institution-Id` | `GET /v1/learner/live-classes`, `GET /v1/learner/live-classes/{id}`, `POST /v1/live-session/join/{id} → {liveKitToken, liveKitUrl, roomName, liveKitAvailable}`, `POST /v1/live-session/report/{id}`, `GET /v1/live-session/health`, `GET /v1/live-session/participants/{id}` |
| `teacher` (`appFetch` / `teacherFetch`) | same | same | `GET|POST /v1/teachers/me/live-classes`, `PUT|DELETE /v1/teachers/me/live-classes/{id}`, `POST …/{id}/start|/end`, `GET …/{id}/participants` (+ `lobbyEnabled, isRecurring, recurrencePattern, sessionType, timezone`) |
| `LiveClassroom` extras | same | same | `POST /v1/live-session/classes/{id}/recording/start|/stop`, `POST …/quizzes`, `/polls`, `/breakout-rooms/...`, `/polls/{id}/vote` |
| Realtime WS | `ws://host:8080/ws/live-class/{id}?token=` (classroom) and `wss://host:8081/ws?token=` (global `useRealtime`) | token in query | See §18 |

**Proxy:** `frontend/proxy.ts` exports `proxy(request: NextRequest)` matching `/dashboard/:path*`. Guards `learnerRoutes → Other Learner|Student`, `teacherRoutes → Teacher|Instructor`, etc. `frontend/Dockerfile` sets `BACKEND_URL=http://elmkusoma-core:8080`, `MEDIA_URL=http://elmkusoma-media:8083`, `REALTIME_URL=http://elmkusoma-realtime:8081`.

**LiveKit usage (grep 51 matches):** Only `components/live/live-classroom.tsx` imports imperatively:

```ts
import { Room, RoomEvent, Track, Participant as LKParticipant, TrackPublication } from "livekit-client"
new Room({ adaptiveStream: true, dynacast: true })
```

Handles `Connected`, `Disconnected` (exponential backoff 3s·1.5ⁿ max 30s, 10 retries), `ParticipantConnected/Disconnected`, `TrackSubscribed` (video/audio). Publishes local camera/mic/screen via `room.localParticipant.publishTrack`; attaches via `videoTrack.mediaStreamTrack → MediaStream`. No other component uses `@livekit/components-react` hooks — despite the dep, only imperative client. `primary-live-classroom.tsx` uses **no LiveKit** (pure WS + avatar cards).

---

## 16. Live User Journey

```
Schedule  →  Teacher: /dashboard/teacher/live-classes → Review & Schedule
             form: title*, description, sessionType (14 options),
             scheduledAt*, duration, maxParticipants, classGroupId,
             subjectId, enableRecording, lobbyEnabled, timezone (12),
             isRecurring + recurrencePattern (DAILY/WEEKLY/BIWEEKLY/MONTHLY)
             ↓
Details   →  Session card: title, sessionType, scheduledAt, duration,
             maxParticipants, classGroup/subject, lobby, recording,
             recurrence. Status badge: SCHEDULED | IN_PROGRESS | LIVE
             | COMPLETED | ENDED | CANCELLED
             ↓
Prepare   →  Teacher: /dashboard/teacher/live-classes/[id]/prepare
             sequential device checks: getUserMedia video, audio,
             AudioContext resume, /api/health latency,
             GET /v1/live-session/health (LiveKit details).
             Preview video + “Start Live Class” → /live-classes/[id]
             ↓
Preflight →  Event participant: /dashboard/learner/events/[id]/preflight
             (events path has dedicated preflight; live-classes use
             Prepare page above)
             ↓
Start     →  Teacher: POST /v1/teachers/me/live-classes/{id}/start
             (only if canStartSession: 15min before → 60min after
             scheduledAt). Lecturer variant: POST /api/v1/teachers/me/live-classes/{id}/start
             ↓
Join      →  Learner: /dashboard/learner/live-classes or
             /dashboard/learner/live-campus → Join Live Class
             → /live-classes/[id] (AuthGuard → LiveClassroom).
             Handshake: POST /v1/live-session/join/{id} (X-Institution-Id)
             → { liveKitAvailable, liveKitToken, liveKitUrl, roomName }
             → serviceMode="full" (LiveKit) or "chat-only" (WS fallback)
             ↓
Interact  →  LiveClassroom (dual-stack):
             WS /ws/live-class/{id}?token= for chat, presence,
             hand-raise queue, poll/quiz/media/breakout, mute/kick.
             LiveKit Room when isInProgress && token+url && serviceMode==="full":
             remote video/audio tracks, PiP for screen+camera,
             local camera/mic/screen publish.
             Controls: Camera, Mic, ScreenShare, Recording toggle
             (POST …/recording/start|/stop), Attach Material, Hand Raise,
             Report Issue → learnerApi.reportLiveIssue, Leave.
             Participant pane: teacher mute/unmute/kick (hover actions),
             HandRaiseQueue with position/elapsed, activePolls
             (POST /v1/live-session/polls/{id}/vote), quizzes
             (GET …/quizzes), breakout rooms (create/assign/join/start/end),
             shared media list.
             PrimaryLiveClassroom (young learners): no LiveKit, WS chat only,
             avatar cards (crown for teacher), Activities panel via
             LiveInteractivePanel (9 types: POLL/MCQ/QUIZ/TRUE_FALSE/MATCHING/
             DRAWING/PREDICTION/QUESTION/CHALLENGE → primaryApi)
             ↓
Attendance→  Participant list: online dot, duration, role; presence events
             USER_JOINED/LEFT, PARTICIPANTS. Teacher detail page lazy
             GET /v1/teachers/me/live-classes/{id}/participants.
             ↓
End       →  Teacher: POST /v1/teachers/me/live-classes/{id}/end
             (or Lecturer POST /api/v1/…/{id}/end). Participants see
             Disconnected + reconnect backoff.
             ↓
Recording →  Toggle in LiveClassroom; requires LiveKit Egress webhook
             to MinIO (not in livekit.yaml — production TODO).
             Recording URL stored as recordingUrl on session.
             ↓
Replay    →  /dashboard/learner/replays + /replays/[id]: continueWatching
             (positionSeconds>0 && !completed) vs other; custom player
             (progress seek, speed 0.5–2x, fullscreen, volume), restore
             saved position, save every 10s via PUT /v1/replays/{id}/progress,
             relatedResources + upcomingEvents + continueLearning CTA.
```

> Stages marked: **Prepare/Preflight** — implemented for teacher live-classes and events; **Recording** — toggle implemented, persistence requires Egress config; **Replay** — fully implemented.

---

## 17. Teacher Live Test

Use actual UI names. Run against `http://localhost:3000`.

```
 1. Login         →  /login as Teacher (e.g. teacher@elmkusoma.test)
 2. Navigate      →  /dashboard/teacher/live-classes
 3. Create        →  Click “Create Live Class” / “Schedule”
                    Fill: Title* (e.g. “Biology — Photosynthesis”),
                    Description, Session Type (select 14 options),
                    Scheduled At* (datetime-local, must be future),
                    Duration (1–480 min), Max Participants,
                    Class Group (select from teacher’s classes),
                    Subject, Enable Recording (toggle),
                    Lobby Enabled, Timezone (12), Is Recurring +
                    Recurrence Pattern + End Date (if needed)
                    → “Review & Schedule” → review card → “Confirm & Schedule”
                    → redirect to /dashboard/teacher/live-classes/{id}
                    Verify: card appears in SCHEDULED filter, summary Total++
 4. Open Live    →  Click session card or “View” → detail page
 5. Preflight    →  Click “Prepare” → /dashboard/teacher/live-classes/{id}/prepare (new tab)
                    Wait for 4 checks: Camera (getUserMedia video preview),
                    Microphone (audio level), Speaker (AudioContext resume),
                    Network/LiveKit health (GET /v1/live-session/health)
                    All green → “Start Live Class” button enabled
 6. Start        →  Click “Start Live Class” → POST /{id}/start
                    (only enabled if within 15min before → 60min after scheduledAt)
                    → opens /live-classes/{id} (LiveClassroom)
 7. Camera       →  In LiveClassroom controls bar, toggle Camera
                    Verify: local video appears (PiP), remote sees it
 8. Microphone   →  Toggle Mic, speak, verify audio level
 9. Screen Share →  Toggle Screen Share → pick screen/window/tab
                    Verify: screen track publishes, remote sees share
10. Participants →  Open participant pane, verify self + any joined learners
                    Teacher hover actions: Mute/Unmute, Kick
11. Interaction →  Chat: send message → appears in history
                    Poll: create via LiveClassroom extras → vote flow
                    Hand Raise Queue: verify queue updates
                    Attach Material: upload → WS CHAT share
12. End         →  Click “End” / Leave → POST /{id}/end
                    Verify: room Disconnected, participants notified
13. Recording/Replay → If recordingEnabled + Egress configured, recordingUrl
                    appears on session card → /dashboard/learner/replays or
                    /dashboard/learner/replays/{id} (see §21). Without Egress,
                    UI shows toggle but no persisted replay — expected.
```

---

## 18. Learner Live Test

```
 1. Login         →  /login as Student or Other Learner
 2. Find Live    →  /dashboard/learner/live-classes (or /live-campus)
                    Verify: Live Now section (green border, LIVE pill) shows
                    sessions with IN_PROGRESS/LIVE; Upcoming (blue) shows
                    SCHEDULED; Past (opacity 80) shows COMPLETED
                    Search filter works; bookmark toggle works
 3. Details      →  Click session → /live-classes/{id} or
                    /dashboard/learner/live-classes/{id}
                    Verify: title, description, scheduledAt, duration,
                    status badge, teacher name
 4. Join         →  Click “Join Live Class” / “Join”
                    Allow mic/camera when prompted
                    Handshake POST /v1/live-session/join/{id} →
                    serviceMode banner: “Live service degraded — chat only”
                    if LiveKit unavailable, else full room
 5. Verify Teacher→  Participant pane shows teacher (crown), online dot
 6. Verify A/V   →  Teacher video/audio tracks render (srcObject = MediaStream)
                    If chat-only, A/V not expected — banner explains
 7. Chat/Interaction → Send chat message → appears in history
                    If poll/quiz active: vote via POST /v1/live-session/polls/{id}/vote
                    Raise Hand → appears in HandRaiseQueue with position
 8. Attendance   →  Presence via WS USER_JOINED/LEFT, PARTICIPANTS list
                    Teacher detail page shows learner in participants
 9. Leave        →  Click Leave → Disconnected + modal → back to
                    /dashboard/learner/live-classes
10. Replay       →  If session had recordingUrl: /dashboard/learner/replays →
                    find replay → /replays/{id} → player restores position,
                    speed/fullscreen work, relatedResources shown
                    Without recording: no replay entry — expected
```

Any step where `serviceMode==="chat-only"` is **not a failure** — it is graceful degradation (see §15).

---

## 19. Seminar / Event Live Test

> Events and Live share the **same Live architecture** — events use it as their delivery mechanism.

```
 1. Create/Register →  Admin: /dashboard/admin/events → New Event
                    Fill: title, type, category, startsAt/endsAt, duration,
                    location, capacity, status PUBLISHED, meetingUrl
                    → Publish.  Or as learner: /dashboard/learner/events →
                    Browse live|upcoming|past|replays tabs → search →
                    Click event → /dashboard/learner/events/{id}
                    → “Register” (if PUBLISHED, not full, not already
                    registered) → registered via learnerApi.registerForEvent
                    → “You Are Registered” + meetingUrl + Add to Calendar (ICS Blob)
                    → Cancel with confirm (learnerApi.cancelRegistration)

 2. Open Event   →  /dashboard/learner/events/{id}
                    Header badges: type, category, live/past/scheduled, registered.
                    Left: date/time/duration/location/capacity. Right actions:
                    if live + meetingUrl → “Join Live Session” (green);
                    if scheduled + PUBLISHED + not registered → Register;
                    else registered → meetingUrl + ICS + Cancel.
                    Materials section: recordings/videos vs documents (VideoPlayer
                    modal or download). About + tags, Before/During/After hints.

 3. Access Live  →  Click “Join Live Session” → /dashboard/learner/events/{id}/preflight
                    (events-dedicated preflight) or directly meetingUrl / /live-classes/{id}
                    if event is linked to a live session.

 4. Join         →  Same as §18 step 4 — POST /v1/live-session/join/{id}
                    (event’s linked liveClassId).

 5. A/V Test    →  Same as §17 steps 7–9.

 6. Interact    →  Same as §17 step 11 — chat, hand-raise, polls.

 7. Attendance  →  Same as §17 step 8 — via participants list.

 8. End         →  Teacher/Admin: POST /v1/events/{id}/endLive or
                    POST /v1/teachers/me/live-classes/{id}/end.

 9. Replay      →  Event materials: VideoPlayer (YouTube/Vimeo/native) via
                    components/events/video-player.tsx.
                    Also /dashboard/learner/replays filtered by eventType.
```

**Shared architecture?** **Yes** — seminars/events reuse `LiveClassroom` + LiveKit + WS. Event `meetingUrl` is the same `liveKitUrl/roomName` path. `learner/events/[id]/preflight` and `waiting` are thin wrappers around the same join flow.

**Known gap:** `learnerApi.getEventMaterials(id)` returns recordings only after teacher uploads — not automatic from LiveKit Egress. Manual upload via `admin/events/{id}/edit` required unless Egress is configured.

---

## 20. Multi-User Live Test Matrix

All participants must have valid accounts with correct roles and (for learners) `classGroupId` assignments.

| User | Action | Expected Result (verified implementation) |
|------|--------|-------------------------------------------|
| **Teacher** | Create Live (`POST /v1/teachers/me/live-classes`) | Card appears in SCHEDULED, polling shows it, `canStartSession` false until T-15min |
| **Teacher** | Start (`POST /{id}/start`) inside window | Status → `IN_PROGRESS`, room opens at `/live-classes/{id}`, WebSocket connects, LiveKit offer if available |
| **Teacher** | Start outside window | Button disabled — “canStartSession” guard prevents call |
| **Learner (enrolled in teacher’s classGroup)** | Find Live at `/learner/live-classes` | Appears in Live Now or Upcoming, search finds it |
| **Learner** | Join (`POST /v1/live-session/join/{id}`) | WS connects, appears in teacher’s participant pane (online dot), chat history loads |
| **Learner 2** (second browser) | Join same session | Second participant appears, count increments, both see each other’s chat |
| **Learner (not in classGroup)** | Find Live | May not see if filtered by `classGroupId` on backend; else can join if `isPublished` and capacity not reached — depends on `classGroupId` null check |
| **Seminar Participant** (`Other Learner` registered to event) | Join via event’s Join Live Session | Same as learner join, via event’s linked liveClassId |
| **Admin** (Institution Admin) | Observe via `/oversight/live-classes` or `/admin/live-operations` | Can view activeLiveClasses count, list. No direct “join as observer” — would need participant token for `/live-classes/{id}` |
| **Teacher** | Mute learner | `MUTE_PARTICIPANT` WS → learner’s mic muted |
| **Teacher** | Kick learner | `KICK_PARTICIPANT` WS → learner receives `KICKED` + disconnected |
| **Teacher** | Send poll/quiz | `QUIZ/POLL_STARTED` WS → learners see vote UI, submit `POST /v1/live-session/polls/{id}/vote` |
| **Learner** | Raise hand | `RAISE_HAND` WS → teacher’s HandRaiseQueue shows position + elapsed |
| **Learner** | Attach material | File → `FormData` to `/api/v1/media/upload` → WS `CHAT` share with fileUrl |
| **Teacher** | Share screen | `SCREEN_SHARE_START` WS + LiveKit screen track publish → learners see PiP |
| **Any** | Network drop | Reconnect with exponential backoff (3s·1.5ⁿ max 30s, 10 retries), banner “Reconnecting…” |
| **Any** | Backend/LiveKit down | `serviceMode==="chat-only"` banner “Live service degraded — chat only”, WS chat still works |

> **Do not assume** “admin can observe” means join — oversight pages show aggregates, not live video. Joining still requires a valid participant token.

---

## 21. Media / Recording / Replay

| Area | Route | Implementation |
|------|-------|---------------|
| **Media Library** | `/dashboard/learner/media-library` (234 lines) | `mediaApi.list() → MediaAsset[]` filtered by `mediaType` VIDEO/DOCUMENT/AUDIO/IMAGE/RECORDING. Note `mediaApi.list` calls `/api/v1/media?institutionId=` (different prefix vs `/v1/learner/resources`). Search across title/desc/tags, type filter buttons, cards with icon+type pill, duration (`formatDuration`), fileSize, date, tags, View (`window.open(fileUrl)`) with Play icon |
| **Video Library** | `/dashboard/learner/video-library` (130 lines) | `learnerApi.getVideoLibrary() → Resource[]` filtered `resourceType==="VIDEO"` (`/v1/learner/resources`), modal `VideoPlayer` on card click, Play overlay, download link |
| **Recordings** | Stored via `elmkusoma-media:8083` (MinIO) | Toggle in LiveClassroom (`POST …/recording/start|/stop`) → requires LiveKit Egress webhook to MinIO for persistence. Without Egress, UI toggle exists but no `recordingUrl` persisted — expected |
| **Replay Library** | `/dashboard/learner/replays` (267 lines) | `learnerApi.getReplays({search, eventType, dateFrom, dateTo}) → ReplayItem[]`, split `continueWatching (positionSeconds>0 && !completed)` vs other, cards: thumbnail/Play placeholder, duration badge, progress bar %, title/eventTitle/presenter, date, filters search+type(6)+dateFrom/dateTo, relatedLearning (first 3 replays → course/lesson links) |
| **Replay Viewer** | `/dashboard/learner/replays/[id]` (423 lines) | `learnerApi.getReplay(id) → {replay, relatedResources, upcomingEvents}`, native `<video>` with custom overlay (progress seek, play/pause, volume, speed 0.5–2x, fullscreen, time), restores saved `positionSeconds` if within 5s of duration, saves every 10s via `PUT /v1/replays/{id}/progress`, right column: presenter/recordedOn/duration/viewCount; relatedResources list; upcomingEvents; continueLearning CTA |
| **Media Upload** | `components/media/media-upload.tsx` (90 lines) | Drag&drop + click input, posts `FormData file` to `/api/v1/media/upload` with `Authorization + X-Institution-Id`, calls `onUploadComplete(data.data)` |
| **Video Player** | `components/events/video-player.tsx` (159 lines) | Polymorphic: YouTube (`youtube.com/youtu.be` → iframe), Vimeo → iframe, else native `<video>` with custom controls (play/pause, mute, seek progress click, fullscreen, time `m:ss`, volume) |

**How Live recording becomes learning material:**

1. Teacher toggles Recording in LiveClassroom → `POST …/recording/start`
2. If LiveKit Egress is configured (webhook egress to MinIO → `minio/livekit-recordings/`), Egress writes `*.mp4` to MinIO and backend sets `liveClass.recordingUrl`
3. `recordingUrl` then appears as a `ReplayItem` / event material automatically
4. **Without Egress** (current `livekit.yaml` has no `egress:` block) — recording toggle shows but no persisted file; teacher must manually upload via Media Library or `admin/events/{id}/edit`

**API drift:** `mediaApi.list` uses `/api/v1/media` while `learnerApi.getResources` uses `/v1/learner/resources` — both serve media but via different prefixes (proxied identically in `next.config.mjs`). Unify to avoid confusion.

---

## 22. Courses and Learning

| Workflow | Frontend Path | API |
|----------|---------------|-----|
| **Courses** | Public `/courses`, `/courses/[level]/[className]/[id]`; Authenticated `learner/courses`, `teacher/courses`, `admin/courses` / `platform-admin/courses` | `learnerApi.getCourses/getCourse`, `GET /v1/courses?institutionId=`, `platformAdminApi.getCourses` |
| **Subjects** | `secondary/learn` → `subjects/[id]` → `topics/[topicId]`; Primary via subject pills on `lessons/page.tsx` | `secondaryApi.getSubjects/getConceptsByClass`, `learningApi.getSubjects` |
| **Modules** | `learner/modules`, `learner/module-workspace`, `learner/course-workspace` | `learnerApi.getCourseModules`, `collegeApi.getLearnerModules` |
| **Lessons** | `dashboard/lessons/[id]`, `learner/courses/[id]/lessons/[lessonId]` (lesson player with YouTube embed, video, doc, link, quiz, assignment), `labs`, `secondary/learn/*` | `learnerApi.getModuleLessons/getLessonDetail/completeLesson/updateProgress`, `secondaryApi.getConcepts/getProblems` |
| **Practice** | `assignments/[id]`, `secondary/practice`, `learner/practical-lab`, `learner/assessments` (quiz contentType), `secondary/learn/problem-solving` | `learningApi.getAssignmentsByClass/createAssignment`, `primaryApi.getLiveClassActivities` |
| **Assignments** | `dashboard/assignments/[id]` (learner), `teacher/assignments` (CRUD), `provider/assessments` | `learningApi.getAssignments/classGroupId`, `PUT /v1/learning/submissions/{id}/grade` |
| **Assessments** | `dashboard/assessments/[id]` (learner), `teacher/assessments`, `secondary/assess` (SCHEDULED/PUBLISHED vs COMPLETED), `learner/assessments` | `assessmentApi.getByClass`, `secondaryApi.getAssessments` |
| **Progress** | `dashboard/progress`, `learner/progress`, `learner/academic-progress`, `secondary/progress`, `teacher/analytics` | `learnerApi.getProgress`, `collegeApi.getAcademicRecord/getCompetencySummary`, `teacherFetch /v1/teachers/me/analytics` |
| **Projects** | `secondary/projects/passport`, `learner/projects`, `lecturer/courses` | `collegeApi.getStudentProjects` (`IDEATION→ARCHIVED`) |
| **Research** | `learner/research` (12-status `IDEA→COMPLETED`), `learner/thesis` (7-status `PROPOSAL…DEFENSE_COMPLETE`) | `collegeApi.getStudentResearch/getStudentTheses` |
| **Portfolio** | `dashboard/portfolio`, `nursery/portfolio`, `secondary/portfolio`, `learner/portfolio`, `dashboard/evidence`, `nursery/evidence`, `learner/evidence`, `learner/demonstrations` | `collegeApi.getPortfolios`, `nurseryApi.getMilestones` |
| **Certificates** | `dashboard/certificates/*`, `learner/certificates`, `provider/certificates`, `platform-admin/certificates`, `certificates/verify/[code]` (public) | `learnerApi.getCertificates`, `certificateApi.verify` |
| **Resources** | `learner/resources/[id]`, `learner/media-library`, `learner/video-library`, `learner/replays`, `notes-library` | `learnerApi.getResources/getVideoLibrary`, `mediaApi.list` |
| **Search** | `learner/search` (`learnerApi.search(q,type,filters)`), topbar search (Plan: 30s polling unread, admin GlobalSearchDropdown → `/api/v1/platform-admin/search`) | `GET /v1/learner/search?q=&type=` |

---

## 23. Authentication Experience

| Step | What the user sees | Technical detail |
|------|-------------------|------------------|
| **Registration** | `/register` — 469 lines, fields: firstName min2, middleName optional, lastName min2, email, phone min10 required, role (Student/Teacher/Parent/Other Learner — Admin roles not self-registrable), learningLevel (NURSERY…UNIVERSITY) only if Student, password min8 + uppercase + number, confirmPassword, agreeToTerms true; 5-digit captcha (rotated/skewed, monospace, refresh button) validates before submit; `registerUser({firstName,…,phone,password,role,learningLevel})` → “Account Created! Go to Dashboard” | `authApi.register` maps frontend role → backend (`Other Learner→OTHER_LEARNER`, Teacher/Lecturer/Facilitator→TEACHER). Stores `elmkusoma_current_user` JSON + cookie 7 days, `elmkusoma_access_token` cookie 86400 |
| **Login** | `/login` — 155 lines, zod `email required email, password min6`, react-hook-form, showPassword toggle Eye/EyeOff, `login(email,password)` → redirect `searchParams.get("redirect")||"/dashboard"`, serverError banner, background `/images/login-bg.jpg` overlay, links to `/register`, `/forgot-password` | `authApi.login → setTokens + cookies`; `doRefreshToken` via `authApi.refresh(refreshToken)` auto every 60s if expiring <5min |
| **Password reset** | `/forgot-password` (public) → email flow (backend) | Not a frontend form beyond email input |
| **Email verification** | After register, backend sends verification (email) — frontend shows “check email” notice; token verified on next login | `isEmailVerified` field on User |
| **Session** | `lib/auth.tsx` `AuthContext`: `AuthUser {id, name, email, role, firstName, lastName, phone, institutionId, classGroupId, learningLevel, secondaryStage, form, regionId, districtId}`, `loading` boolean, `login/register/logout/refreshToken`. Storage: `elmkusoma_current_user` JSON + cookie 7 days, `elmkusoma_access_token` cookie 86400, `elmkusoma_institution_id` localStorage | `getStoredUser/getStoredToken`, `useRequireAuth` redirects `!loading && !user → /login?redirect=pathname` |
| **Protected routes** | `proxy.ts` guards `/dashboard/:path*` (`learnerRoutes→Other Learner|Student`, `teacherRoutes→Teacher|Instructor`, etc.). `app/dashboard/layout.tsx` wraps `NextIntlClientProvider(en)`, `AuthGuard`, `LowBandwidthProvider`, fixed aside `DashboardSidebar` + `DashboardTopbar` + main `p-4 sm:p-6`. Per-world `layout.tsx` adds second guard (e.g. nursery only `Student+NURSERY`) | `AuthGuard` shows `LoadingState` while `loading`, else `children` |
| **Role routing** | After login, `router.push(redirectParam || "/dashboard")` → `dashboard/page.tsx` (or `proxy.ts`) dispatches by `role+learningLevel`: `Parent→/parent`, `Teacher→/teacher`, `Other Learner→/learner`, `Student+NURSERY→/nursery`, `PRIMARY→/dashboard` (generic), `SECONDARY→/secondary`, `COLLEGE/VETA/UNIVERSITY→/learner` (HE). `National/Regional/District Admin→/oversight` or `/platform-admin` if `Admin` | `lib/auth.tsx` `BACKEND→FRONTEND` map + `lib/learner-config.ts` `getDashboardConfig/getLearnerNavItems` + `dashboard-sidebar.tsx` `isTeacher/isAdmin/isParent/isLearner/getStudentNavSections/getStudentNavFlat` |

Focus is UX, not backend JWT internals (see Developer 01 doc for signing/refresh implementation).

---

## 24. Frontend Environment

No `.env.example` or `.env.local` committed — values below are inferred from `next.config.mjs`, `proxy.ts`, `Dockerfile`, `docker-compose.yml`, and `lib/**/*.ts` fallbacks. **Do not commit real secrets.**

| Variable | Purpose | Required | Default / Example |
|----------|---------|----------|-------------------|
| `NEXT_PUBLIC_API_URL` | Backend base URL for `learnerApi`, `appFetch`, `platformAdminApi` | Yes | `http://localhost:8080` |
| `NEXT_PUBLIC_API_BASE` | Alias used by `live-classroom.tsx` recording/poll/breakout calls | No (fallback to `NEXT_PUBLIC_API_URL`) | `http://localhost:8080` |
| `BACKEND_URL` | Server-side rewrite target for `/v1/:path*` (in `next.config.mjs`) | Yes (Docker) | `http://elmkusoma-core:8080` (Docker), `http://localhost:8080` (local) |
| `MEDIA_URL` | Media service for `/api/v1/media/:path*` | No | `http://localhost:8083` / `http://elmkusoma-media:8083` |
| `REALTIME_URL` | Realtime gateway for `/ws/:path*` | No | `http://localhost:8081` / `http://elmkusoma-realtime:8081` |
| `NEXT_PUBLIC_WS_HOST` | Legacy WS host used by some live code | No | `localhost` |
| `NEXT_PUBLIC_WS_PORT` | Legacy WS port | No | `8080` or `8081` |
| `NEXT_PUBLIC_REALTIME_HOST` | Realtime host (alt) | No | `localhost` |
| `NEXT_PUBLIC_REALTIME_PORT` | Realtime port (alt) | No | `8081` |
| `NEXT_PUBLIC_LIVEKIT_URL` | LiveKit SFU URL returned by `POST /v1/live-session/join/{id}` | No (runtime) | `ws://localhost:7880` |
| `LIVEKIT_URL` | Server-side LiveKit URL (backend `application.yml`) | No | `ws://localhost:7880` |
| `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` | LiveKit auth (keys `devkey: devsecret` in `livekit.yaml`) | Yes if LiveKit enabled | `devkey` / `devsecret` |
| `NEXT_LOCALE` | Cookie set by locale toggle (`en` ↔ `sw`), read by `use-locale.ts` + `i18n.ts` | No | `en` (default) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Backend only (see Developer 01) | Backend | `localhost, 5432, elmkusoma, postgres, …` |

Do **not** expose `LIVEKIT_API_SECRET`, `DB_PASSWORD`, or JWT secrets in frontend env.

---

## 25. Complete Local Setup

### Prerequisites (only services actually required)

| Service | Version | Required for | Install |
|---------|---------|--------------|---------|
| **Node.js** | 22+ recommended (24 tested, via `nvm`) | Frontend build/dev | `nvm install 24` or `https://nodejs.org` |
| **npm** | 10+ | `npm install`, `npm run dev` | Bundled with Node |
| **Java** | 17 (e.g. Amazon Corretto `17.0.20.10`) | `elmkusoma-core` (Spring Boot 3.4) | `sdk install java 17` or `apt install openjdk-17-jdk` |
| **Maven** | 3.9+ | Backend build | `sdk install maven` or `apt install maven`; wrapper `~/bin/mvn` forces JDK 17 if needed |
| **PostgreSQL** | 16 | `elmkusoma` DB | `docker run -e POSTGRES_DB=elmkusoma -e POSTGRES_USER=elmkusoma -p 5432:5432 postgres:16-alpine` or via `docker-compose.yml` |
| **Redis** | 7 | Cache, session, LiveKit | `docker run -p 6379:6379 redis:7-alpine` or compose |
| **RabbitMQ** | 3.12 | Event bus (core → workers → realtime) | `docker run -p 5672:5672 -p 15672:15672 rabbitmq:3-management` or compose |
| **LiveKit** | SFU (latest `livekit/livekit-server`) | Live video/audio/screen (optional — degrades to chat-only without) | `docker run -p 7880:7880 -p 7881:7881 -p 50000-60000:50000-60000/udp livekit/livekit-server --dev --config livekit.yaml` or add service to `docker-compose.yml` |
| **MinIO** | latest | Media/recordings | `docker run -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address :9001` or compose |

Docker Compose at repo root already defines `postgres`, `redis`, `rabbitmq`, `minio`, `elmkusoma-core`, `elmkusoma-realtime`, `elmkusoma-workers`, `elmkusoma-media`, `nginx` (profile `production`), `frontend`. LiveKit service is **missing** and should be added (see §29).

**Verify:**

```bash
java -version        # 17
mvn -v               # 3.9
node -v              # 22+
psql --version       # 16
redis-cli ping       # PONG
rabbitmqctl status   # running
```

---

## 26. Running ELMKUSOMA

> Backend run is summarized here; full backend instructions are in Developer 01’s document.

### Via Docker Compose (recommended)

```bash
cd /home/tally/elmkusoma
docker compose up --build -d        # postgres, redis, rabbitmq, minio, core, realtime, workers, media, frontend
docker compose logs -f elmkusoma-core
docker compose logs -f frontend
```

Add LiveKit service to `docker-compose.yml` if you need video (see `livekit.yaml` + §15):

```yaml
  livekit:
    image: livekit/livekit-server:latest
    ports: ["7880:7880","7881:7881","50000-60000:50000-60000/udp"]
    volumes: ["./livekit.yaml:/etc/livekit.yaml"]
    command: --config /etc/livekit.yaml --dev
    depends_on: [redis]
```

### Local (without Docker) — Frontend

```bash
cd frontend
npm install          # installs next 16.3.3, react 19, livekit-client, next-intl, etc.
npm run dev          # → http://localhost:3000 (Turbopack)
# Build:
NODE_OPTIONS=--max-old-space-size=4096 npm run build
# E2E:
npx playwright test
```

### Local — Backend (summary)

```bash
export JAVA_HOME=/path/to/corretto-17
export DB_HOST=localhost DB_PORT=5432 DB_NAME=elmkusoma DB_USERNAME=elmkusoma DB_PASSWORD=...
cd backend/elmkusoma-core
mvn spring-boot:run          # → http://localhost:8080 (Flyway disabled, ddl-auto: none)
# Other services:
cd ../elmkusoma-realtime && mvn spring-boot:run   # → http://localhost:8081
cd ../elmkusoma-media && mvn spring-boot:run      # → http://localhost:8083
cd ../elmkusoma-workers && mvn spring-boot:run
```

### URLs (verified)

| Service | URL |
|---------|-----|
| Frontend | `http://localhost:3000` |
| Backend (core) | `http://localhost:8080` — Swagger `http://localhost:8080/swagger-ui.html`, Actuator `http://localhost:8080/actuator/health` |
| Realtime | `http://localhost:8081` — WS `ws://localhost:8081/ws?token=&institutionId=&userId=` and `ws://localhost:8080/ws/live-class/{id}?token=` |
| Media | `http://localhost:8083` |
| LiveKit | `ws://localhost:7880` (RTC 7881, UDP 50000-60000) |
| Postgres | `localhost:5432` DB `elmkusoma` |
| Redis | `localhost:6379` |
| RabbitMQ | `localhost:5672` (management `http://localhost:15672`) |
| MinIO | `http://localhost:9000` (console `http://localhost:9001`) |

### Environment setup (local)

No `.env.local` committed. Create `frontend/.env.local` if needed:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE=http://localhost:8080
BACKEND_URL=http://localhost:8080
MEDIA_URL=http://localhost:8083
REALTIME_URL=http://localhost:8081
```

`next.config.mjs` defaults handle missing vars, so `.env.local` is optional for local dev.

---

## 27. Complete Testing Runbook

```
START SERVICES
  docker compose up -d postgres redis rabbitmq minio
  # or: docker compose up -d  (all services)
  Verify: pg_isready -h localhost -U elmkusoma -d elmkusoma
          redis-cli ping → PONG
          rabbitmqctl status (or http://localhost:15672)
          #
DATABASE
  psql -h localhost -U elmkusoma -d elmkusoma -c "SELECT count(*) FROM users;"
  # Migrations run via Flyway on backend startup (V01…V69) — check logs for “Flyway” or “Started ElmkusomaCoreApplication”
  #
BACKEND
  export DB_HOST=localhost DB_NAME=elmkusoma DB_USERNAME=elmkusoma DB_PASSWORD=...
  cd backend/elmkusoma-core && mvn spring-boot:run
  # Wait for: “Started ElmkusomaCoreApplication in Xs” + “Tomcat started on port 8080”
  curl http://localhost:8080/actuator/health   # → {"status":"UP"}
  curl http://localhost:8080/v1/auth/me -H "Authorization: Bearer …"  # 401 without token, 200 with
  #
LIVEKIT
  # If testing video: docker run livekit/livekit-server --dev --config livekit.yaml
  # Verify WS: wscat -c ws://localhost:7880  (or check livekit logs)
  # Without LiveKit: live sessions degrade to chat-only — still testable
  #
FRONTEND
  cd frontend && npm install && npm run dev
  # → http://localhost:3000  (Turbopack)
  # Check proxy: curl http://localhost:3000/v1/auth/me should proxy to backend (or direct fetch with NEXT_PUBLIC_API_URL)
  #
REGISTER
  Open http://localhost:3000/register
  Fill: firstName, lastName, email (unique), phone, role (pick one per test plan),
        learningLevel (if Student), password (8+ upper+number), confirm, captcha (rotated 5 digits), agreeToTerms
  Submit → “Account Created! Go to Dashboard”
  # For multi-role test: register 1 Teacher, 2 Students (one PRIMARY, one SECONDARY), 1 Other Learner, 1 Parent linked to a child
  #
LOGIN
  http://localhost:3000/login → email+password → redirect to /dashboard (or ?redirect=)
  # Failed login shows serverError banner
  #
ROLE ROUTING
  After login, verify redirect:
    Student+NURSERY → /dashboard/nursery
    Student+PRIMARY → /dashboard
    Student+SECONDARY → /dashboard/secondary
    Student+COLLEGE/VETA/UNIVERSITY → /dashboard/learner (HE)
    Other Learner → /dashboard/learner (Other Learner)
    Teacher → /dashboard/teacher
    Parent → /dashboard/parent
    Admin → /dashboard/platform-admin (or /dashboard/admin if Institution Admin)
    National/Regional/District Admin → /oversight
  #
LEARNER WORKSPACE
  As Student/Other Learner: navigate sidebar, verify correct nav renders (see §4–8)
  Open dashboard, verify greeting, cards, continue learning, recommended courses
  #
ENROLLMENT
  As Other Learner or HE student: /dashboard/learner/courses → search/filter → click course → enroll CTA
  POST /v1/learner/me/enrollments {courseId} → 201
  Verify: /dashboard/learner/my-learning shows enrollment; /dashboard/learner/courses shows enrolled badge; duplicate enroll → 200 “Already enrolled”
  #
LEARNING
  Open /dashboard/learner/courses/{id} → expand modules → click lesson → /courses/{id}/lessons/{lessonId}
  Verify: video/doc/link/text renders; YouTube embeds iframe; duration shown
  Click “Mark as Complete” → POST /v1/learner/me/lessons/{lessonId}/complete → completed badge, progress bar updates, lastAccessedLesson saved to localStorage
  Navigate prev/next, verify navigation works
  #
TEACHER
  As Teacher: /dashboard/teacher → verify Command Center, Key Metrics, Classes, Live Classes, Needs Grading
  /teacher/assignments → create assignment (ESSAY etc.) → verify appears in class
  /teacher/attendance → select class+date → Mark All Present → submit → verify history
  /teacher/grading → verify scales + report cards
  # Known issue: /teacher/students may show 0 if StudentClassEnrollment/ClassGroup not linked — document, don’t hide (see §9)
  #
LIVE
  Follow §17 (Teacher) then §18 (Learner) then §19 (Event)
  Teacher: /dashboard/teacher/live-classes → Create → Prepare → Start → LiveClassroom
  Learner: /dashboard/learner/live-classes → Join → allow mic/cam → verify teacher + A/V + chat + hand-raise
  Without LiveKit: verify chat-only fallback banner, still test chat/polls
  #
ATTENDANCE (Live)
  Verify participant list shows online dot + count, teacher sees learner, learner sees teacher
  #
RECORDING / REPLAY
  Toggle Recording in LiveClassroom → if Egress configured, recordingUrl appears → /dashboard/learner/replays → continueWatching + replays/[id] player (seek, speed, volume, fullscreen, resume)
  Without Egress: no replay expected — manual upload via media-library
```

---

## 28. Two-Browser Test

> **Mandatory test** — use two browsers (or Chrome + Incognito, or Chrome + Firefox). One = Teacher, one = Learner.

**Setup:** Register (or seed) 1 Teacher and 1 Learner (Other Learner or Student in a classGroup assigned to that teacher). Ensure both have `institutionId`.

| Step | Browser A (Teacher) | Browser B (Learner) | Expected |
|------|---------------------|---------------------|----------|
| 1. Login | Login as Teacher → lands `/dashboard/teacher` | Login as Learner → lands `/dashboard/learner` or `/dashboard` | Both see correct sidebar per role |
| 2. Schedule | `/dashboard/teacher/live-classes` → Create Live (title “Two-Browser Test”, future time +5min, duration 30, classGroup = learner’s) → Review → Confirm | — | Card appears SCHEDULED in teacher list |
| 3. Prepare | Click Prepare → `/teacher/live-classes/{id}/prepare` → wait 4 checks green | — | All checks pass; Start button enabled only when `canStartSession` (T-15min) |
| 4. Start | Click Start → `POST /{id}/start` → opens `/live-classes/{id}` (LiveClassroom) | — | Status → IN_PROGRESS, room opens |
| 5. Learner sees Live | — | Refresh `/dashboard/learner/live-classes` or `/live-campus` | Live Now section shows “Two-Browser Test” with LIVE pill |
| 6. Learner joins | — | Click Join → allow mic/cam → `POST /v1/live-session/join/{id}` → LiveClassroom | WS connects; serviceMode banner if chat-only else full; learner appears in teacher’s participant pane |
| 7. Teacher sees learner | Verify participant list shows learner (online dot, name) | — | Count 2, hover shows Mute/Kick for teacher |
| 8. Learner sees teacher | — | Verify participant pane shows teacher (crown) | Both see each other |
| 9. Audio | Speak | Listen | Audio track renders; if chat-only, no audio expected — verify chat instead |
| 10. Video | Toggle Camera on | Verify video tile | `videoTrack.mediaStreamTrack → MediaStream` renders; PiP if screen-share |
| 11. Screen share | Toggle Screen Share → pick screen | Verify share appears | Screen track publishes, remote sees share; PiP for camera+screen |
| 12. Interaction | Type chat message | Verify message appears in learner’s history, and vice versa | `CHAT` WS → `CHAT_MESSAGE` broadcast |
| 13. Hand raise | — | Raise Hand | Teacher’s HandRaiseQueue shows learner with position + elapsed |
| 14. Poll/Quiz | Create poll (if UI available) | Vote `POST /v1/live-session/polls/{id}/vote` | Poll results update |
| 15. Attendance | Check participants | Check participants | Both lists consistent, duration ticking |
| 16. Recording | Toggle Recording (if enabled) | — | If Egress, `recordingUrl` saved; else toggle visible but no file — expected |
| 17. End | Click End → `POST /{id}/end` | — | Room Disconnected, banner, back to list |
| 18. Replay | — | `/dashboard/learner/replays` → if recording, `replays/[id]` player | Restore position, speed, fullscreen, relatedResources |

Record results; if any step fails, note **Symptom → Inspect** mapping in §29.

---

## 29. Troubleshooting

| Symptom | What to inspect | Likely location | Verified solution |
|---------|----------------|-----------------|-------------------|
| `npm install` fails (`ENOTDIR rename next → .next-PS…`) | `node_modules/.next-*` leftover from bad install | `frontend/node_modules` | `rm -rf node_modules/.next-* && npm install --legacy-peer-deps` |
| `cross-env: not found` on `npm run build` | `cross-env` not installed | `frontend/package.json` devDeps | `npm install --legacy-peer-deps` or `NODE_OPTIONS=--max-old-space-size=4096 npx next build` directly |
| `Could not find the Next.js package` | `node_modules/next/package.json` missing | `frontend/node_modules` | `npm install --legacy-peer-deps` (reinstalls next) |
| `Cannot find module 'next-intl'` | `next-intl` not installed; many secondary/nursery/admin pages import it | `frontend/package.json` | `npm install next-intl --legacy-peer-deps` |
| `Ambiguous app routes … courses/[…]/lessons/[…]` matches multiple | Duplicate `[id]` vs `[courseId]` lesson route | `frontend/app/dashboard/learner/courses/` | Keep one (`[id]/lessons/[lessonId]`); remove `[courseId]/lessons` |
| `error TS2339: Property 'bookmarked' does not exist on type 'boolean'` | `checkBookmark` return type changed `boolean` vs `{bookmarked}` | `frontend/lib/learner-api.ts` + callers | `then((isBookmarked) => setIsBookmarked(isBookmarked))` not `res.bookmarked` |
| Next.js startup `Error: Invalid env` or stale `.next` types referencing moved `notifications/page.js` | `.next/dev/types` cache stale after `notifications → notifications-center` move | `frontend/.next` | `rm -rf .next && npm run dev` |
| Frontend shows blank / “Access denied” after login | Role/learningLevel mismatch | `lib/auth.tsx` role map, `app/dashboard/layout.tsx` proxy, per-world `layout.tsx` guard | Re-register with correct role+level; check `elmkusoma_current_user` in localStorage; Admin roles not self-registrable |
| Login 401 / “Invalid credentials” | Backend not running or DB empty | `http://localhost:8080/actuator/health`, `backend` logs | `docker compose logs elmkusoma-core`; verify `DB_USERNAME/DB_PASSWORD`; run migrations (backend startup) |
| Routing loops `/login?redirect=/dashboard` | `useRequireAuth` always redirecting | `lib/auth.tsx` `getStoredUser/getStoredToken` + cookie expiry | Clear cookies + localStorage (`elmkusoma_*`), re-login; check `NEXT_PUBLIC_API_URL` points to running backend |
| CORS error `Access-Control-Allow-Origin` | Frontend fetch to backend on different origin | `backend/.../config/CorsConfig.java` or `SecurityConfig` | Backend must allow `http://localhost:3000`; `next.config.mjs` rewrites `/v1/:path*` proxy the browser request server-side (no CORS) — use relative `/v1/...` not absolute URL in dev |
| WebSocket fails `ws://localhost:8081/ws` 404/403 | Realtime service not running or token missing | `elmkusoma-realtime` logs, `lib/use-realtime.ts` query `token=&institutionId=&userId=` | `docker compose up elmkusoma-realtime`; verify `REALTIME_URL=http://localhost:8081` in `next.config.mjs`; pass Bearer token in query |
| WebSocket `Kicked` immediately after join | Auth / institution mismatch | `JwtAuthenticationFilter`, `X-Institution-Id` header | Verify learner and teacher share same `institutionId` (localStorage `elmkusoma_institution_id`); use `learnerApi` (adds header) not raw fetch |
| LiveKit `liveKitAvailable: false`, banner “chat only” | LiveKit SFU not running or not configured | `livekit.yaml`, `GET /v1/live-session/health` (`details.livekit` null), `livekit-client` Room connect error | Start LiveKit: `docker run livekit/livekit-server --dev --config livekit.yaml` or add `livekit` service to compose (ports 7880/7881/50000-60000/udp); set `LIVEKIT_URL`, `LIVEKIT_API_KEY/SECRET` on backend |
| Camera not showing / `getUserMedia` error | Browser permission denied or HTTPS required | `/live-classes/[id]` LiveClassroom, `/teacher/live-classes/[id]/prepare` checks | Allow camera in browser prompt; use `https` or `localhost` (insecure contexts block getUserMedia); check `navigator.mediaDevices.getUserMedia` in console |
| Microphone no audio | Permission or device not found | Same as camera | Allow mic; check `AudioContext` resumed; toggle Mic in controls; inspect `room.localParticipant` publish status in console |
| Screen share fails | Browser denies or not supported | `room.localParticipant.publishTrack` screen | Use Chrome/Edge; share must be user gesture; fallback chat-only has no screen share |
| Learner cannot join (403 / “not eligible”) | Session not `IN_PROGRESS` or capacity reached or classGroup mismatch | `POST /v1/live-session/join/{id}` response, `canJoin` flag, `maxParticipants` | Teacher must `POST /{id}/start` first; check capacity; ensure learner’s `classGroupId` matches session’s `classGroupId` or session has `classGroupId=null` (open) |
| Teacher cannot start (`canStartSession` false) | Outside 15min-before → 60min-after window | `lecturer/live-dashboard` + `teacher/live-classes` `canStartSession(scheduledAt)` | Set scheduledAt to now+5min for tests; wait until window; or temporarily bypass by setting DB `scheduled_at` to now |
| Student missing from teacher list | No `StudentClassEnrollment` / `TeacherAssignment` linking student+teacher to same `classGroupId` | `teacher/students/page.tsx` → `teacherApi.getStudentsByClass`, backend `StudentRepository.findByClassGroupId` | Assign via Admin → People or Institution Admin → Organization: set student’s `classGroupId` and teacher’s `TeacherAssignment` to same group; or seed `student_class_enrollments` |
| Seminar participant cannot join | Not registered or event not PUBLISHED or capacity | `learner/events/[id]` Register button, `learnerApi.registerForEvent`, `GET /v1/events/{id}` status | Admin: set event `PUBLISHED`, ensure `capacity` > `registeredCount`; participant: click Register first, then Join |
| Recording not saved / no replay | `recordingUrl` null after toggle | LiveClassroom recording toggle `POST …/recording/start`, backend `liveClass.recordingUrl`, `livekit.yaml` egress | Add Egress config to `livekit.yaml` webhook egress to MinIO, or manually upload via `media-library` / `admin/events/{id}/edit`; without Egress, chat-only has no recording |
| Replay not playing / 404 | `replayId` wrong or MinIO not running | `learner/replays/[id]` `learnerApi.getReplay(id)` → `404`, `minio` logs | `docker compose up minio`; verify `MEDIA_URL=http://localhost:8083`; check `mediaApi.list` vs `learnerApi.getReplays` prefix drift |

---

## 30. Mobile / Low Bandwidth / Accessibility

Verified by inspecting `components/`, `app/`, `hooks/`, `i18n.ts`, `messages/`, `globals.css`, and layout files.

| Capability | Verified | Evidence |
|-----------|----------|----------|
| **Responsive UI** | Yes | Every page: `mx-auto max-w-6xl`, `grid sm:grid-cols-2 lg:grid-cols-3`, `hidden sm:flex`, `lg:hidden` hamburger (`Menu/X` in topbar), `w-64 lg:block` sidebar collapse, `p-4 sm:p-6` padding |
| **Mobile** | Yes | `DashboardTopbar` hamburger `lg:hidden` + overlay `fixed inset-0 z-50 lg:hidden` + `DashboardSidebar` drawer `w-72`, `isMobile` checks in live-classroom for PiP sizing, touch targets `h-10 w-10`, `size-10` buttons |
| **Low bandwidth** | Yes | `components/primary/low-bandwidth-provider.tsx` (`LowBandwidthProvider` wraps `dashboard/layout.tsx`), `useTranslations` for conditional heavy-asset loading, `images.unoptimized: true` (no server resize), `LoadingState` + `EmptyState` everywhere, `progress` save every 10s (not on every frame), `adaptiveStream: true, dynacast: true` on LiveKit Room |
| **Loading states** | Yes | `components/learner/shared.tsx` exports `LoadingState` (`Loader2 animate-spin`), `components/primary/loading-skeleton.tsx` (skeleton cards), every page: `if (loading) return <LoadingState />` + `authLoading` guard |
| **Skeletons** | Yes | `loading-skeleton.tsx` used in primary dashboard while `GamificationBar` loads; other pages use `Loader2` inline |
| **Empty states** | Yes | `components/learner/shared.tsx` `EmptyState` (icon, title, description, action), `components/primary/…` empty checks, every list: `if (items.length===0) <EmptyState … />` with icons (`BookOpen`, `Video`, `Calendar`, `Award`, `Search`) |
| **Error states** | Yes | `catch { setError("Failed to load …") }` → `rounded-2xl border border-destructive/20 bg-destructive/5` banner with `AlertCircle`; `components/error-boundary.tsx` + `components/toast.tsx` in `app/providers.tsx` (Toast auto-dismiss 4s), `not-found.tsx` public 404 |
| **Offline** | Not verified | No service worker / PWA manifest found; `navigator.onLine` not checked. Considered **NOT IMPLEMENTED** |
| **Accessibility** | Partial | `role="banner" aria-label="Top navigation"` on header, `aria-label="Open menu"` on hamburger, `role="main"` on dashboard, `aria-busy` on loading, `aria-label="Notifications"` on bell, `skip-to-content` component (`components/primary/skip-to-content.tsx`), keyboard `onKeyDown` in live interactive panel, `accessibility-wrapper.tsx` (focus trap). No verified `aria-live` on dynamic counters, no `prefers-reduced-motion` handling, contrast not audited |
| **Keyboard** | Partial | `skip-to-content` link, `onKeyDown` handlers, `handleKey` for `Ctrl+K` / `Esc` in topbar search, `onClose` via Esc in `GlobalSearchDropdown`, tab order via native `<Link>`/`<button>` |
| **ARIA** | Partial | Roles above, but no `aria-live="polite"` on notification badge, no `role="status"` on progress bars, no `aria-describedby` on forms beyond zod errors |
| **Contrast** | Not audited | Tailwind `bg-primary/10`, `text-muted-foreground` used pervasively — manual WCAG check recommended |
| **i18n** | Yes | `next-intl` 4.14.6, `locales=['en','sw']`, `defaultLocale='en'`, `components/locale-switcher.tsx` + topbar `Globe SW/EN` toggle (`NEXT_LOCALE` cookie 1yr), `messages/en.json` (common, nursery, secondary, etc.), `useLocale()` hook |

---

## 31. Frontend Status Matrix

| Area | Status | Evidence |
|------|--------|----------|
| **Authentication UI** | IMPLEMENTED | `app/login` (155 lines, zod+react-hook-form, showPassword, redirect), `app/register` (469 lines, 4 roles, learningLevel, captcha), `app/forgot-password`, `lib/auth.tsx` (286 lines, JWT cookie 86400 + refresh 60s, role map 9 roles) |
| **Learner Routing** | IMPLEMENTED | `proxy.ts` guards `/dashboard/learner`, per-world `layout.tsx` (nursery/secondary/learner), `dashboard-sidebar.tsx` role resolution, `dashboard/layout.tsx` AuthGuard |
| **Nursery** | IMPLEMENTED | 25 routes under `/dashboard/nursery`, `lib/nursery-api.ts` (281 lines, 8 types, 10 endpoints), layout guard `Student+NURSERY`, 7 pages with built-ins verified |
| **Primary** | IMPLEMENTED | No `/primary` folder — generic `/dashboard/*` branch via `isPrimary` + `primaryNavSections` (7 groups, 30+ items), `components/primary/` (gamification, low-bandwidth, accessibility), shared `dashboard/page.tsx` |
| **Secondary** | IMPLEMENTED | 28 routes `/dashboard/secondary`, `lib/secondary-api.ts` (325 lines, 15 sidebar items, O-Level/A-Level via `secondaryStage/form`), `layout.tsx` guard |
| **Higher Education** | IMPLEMENTED | `/dashboard/learner` HE branch, `lib/college-api.ts` (183) + `lib/types/college.ts` (551), `collegeNavSections` (18) + `universityNavSections` (collapsible), 40+ routes (research/thesis/projects/fieldwork/live-campus/competencies/etc.) |
| **Other Learner** | IMPLEMENTED | Same `/dashboard/learner` files, Other Learner branch via `learnerNavSections` (11 items, 6 groups), `otherLearnerConfig`, `lib/learner-config.ts` |
| **Teacher** | IMPLEMENTED | `/dashboard/teacher` (20 subdirs), `lib/teacher-api.ts` (249 lines), dashboard 875 lines + assignments/attendance/grading/analytics/live-classes with recurrence/lobby/prepare; **known issue documented**: students missing from teacher list (see §9) |
| **Parent** | IMPLEMENTED | `/dashboard/parent` (23 subdirs, `parent/page.tsx` 413 lines, FamilyOverview+ChildIntelligence), `lib/parent-api.ts`, `dashboard/family` collaboration; all 23 sub-pages exist |
| **Institution Admin** | IMPLEMENTED | `/dashboard/admin` (14 entries, `page.tsx` 220 lines EnhancedDashboard), `adminApi` via `lib/api.ts`; Provider NFE `/dashboard/provider` (9 entries) |
| **Platform Admin** | IMPLEMENTED | `/dashboard/platform-admin` (36+ entries, `page.tsx` 260 Command Center), `lib/platform-admin-api.ts`, `platform-admin-sidebar.tsx` (147 lines, 15 sections), `layout.tsx` guard `Admin` only |
| **Oversight** | IMPLEMENTED | `/oversight` (11 entries, `page.tsx` 221 Command Center, jurisdiction-based national/region/district), `authority-sidebar.tsx` (133 lines), `GET /v1/oversight/dashboard` + `/schools` |
| **Live** | IMPLEMENTED | 6 learner/teacher/lecturer/primary/nursery/secondary live pages + `components/live/` (live-classroom.tsx 1300+, primary-live-classroom.tsx 488, live-interactive-panel.tsx 991, activity-creator.tsx 253) + LiveKit dep installed; dual WS+SFU, chat-only fallback, recording toggle; **infra gap**: LiveKit container missing from compose (see §15) |
| **Events** | IMPLEMENTED | Learner `events/` (5 routes: browse, `[id]`, `preflight`, `waiting`, `registered`) + Admin `admin/events/` (4 routes) + `components/events/video-player.tsx` (YouTube/Vimeo/native) |
| **Seminar** | IMPLEMENTED | Reuses Live+Events — same `LiveClassroom` + `liveKitToken`; no separate seminar stack; preflight/waiting shared |
| **Media** | IMPLEMENTED | `learner/media-library` (234 lines, `mediaApi.list` by mediaType), `learner/video-library` (130 lines, `learnerApi.getVideoLibrary`), `components/media/media-upload.tsx` (90 lines, drag&drop → MinIO), `elmkusoma-media:8083` |
| **Replay** | IMPLEMENTED | `learner/replays` (267 lines, continueWatching + progress), `replays/[id]` (423 lines, custom player speed/fullscreen/resume every 10s), relatedResources + upcomingEvents |
| **Testing** | PARTIAL | `e2e/` + `playwright.config.ts` + `@playwright/test` 1.63.0 exist; `__tests__/primary-pages.test.tsx` exists; no per-page unit tests visible; manual runbook (§27) is primary method; build `npm run build` passes (see §26) |

---

## 32. Documentation Completeness Check

| Question | Covered in | Section |
|----------|------------|---------|
| WHO uses ELMKUSOMA | Roles table + per-role sections | §2, §3, §9–13 |
| WHAT ELMKUSOMA provides | User journey + per-world feature tables | §2, §4–8 |
| HOW users navigate it | Routing tables + sidebar per role | §14 + §4–13 |
| HOW learning works | Courses/Modules/Lessons → Practice → Assess → Progress | §22 |
| HOW teachers work | Dashboard + 20 sub-pages + known issue + test | §9 + §17, §20, §27 |
| HOW parents interact | Dashboard + 23 sub-pages + family collaboration | §10 |
| HOW institutions work | Institution Admin + Provider Admin | §11 |
| HOW administrators work | Platform Admin Command Center + 36 routes | §12 |
| HOW oversight works | National/Regional/District jurisdiction-based | §13 |
| HOW Live works | Architecture + user journey + classroom dual-stack | §15 + §16 |
| HOW seminars work | Events share Live architecture, preflight/waiting | §19 + §15 |
| HOW media works | Media Library / Video Library / Upload / VideoPlayer + MinIO | §21 |
| HOW to install | Prerequisites table (Node, Java, Maven, Postgres, Redis, RabbitMQ, LiveKit, MinIO) | §25 |
| HOW to configure | Env vars table (NEXT_PUBLIC_* etc.) | §24 |
| HOW to run | Docker Compose + local frontend/backend commands + URLs | §26 |
| HOW to test | Complete runbook (START→DATABASE→BACKEND→LIVEKIT→FRONTEND→REGISTER→…→REPLAY) | §27 |
| HOW to troubleshoot | 25-item symptom → inspect → location → solution table | §29 |

All 16 checks pass — no applicable area missing.

---

## Appendix: Repository Locations Quick Reference

| Concern | File(s) |
|---------|---------|
| Frontend stack / deps | `frontend/package.json`, `frontend/next.config.mjs` |
| Auth context + role map | `frontend/lib/auth.tsx` |
| API clients | `frontend/lib/api.ts`, `frontend/lib/fetch.ts`, `frontend/lib/learner-api.ts`, `frontend/lib/college-api.ts`, `frontend/lib/nursery-api.ts`, `frontend/lib/secondary-api.ts`, `frontend/lib/teacher-api.ts`, `frontend/lib/parent-api.ts`, `frontend/lib/platform-admin-api.ts` |
| Routing guards | `frontend/proxy.ts`, `frontend/app/dashboard/layout.tsx`, `frontend/app/dashboard/nursery/layout.tsx`, `frontend/app/dashboard/secondary/layout.tsx`, `frontend/app/dashboard/learner/layout.tsx`, `frontend/app/(platform-admin)/dashboard/platform-admin/layout.tsx`, `frontend/app/oversight/layout.tsx` |
| Sidebars | `frontend/components/dashboard/dashboard-sidebar.tsx`, `frontend/components/dashboard/platform-admin-sidebar.tsx`, `frontend/components/dashboard/authority-sidebar.tsx` |
| Learner config | `frontend/lib/learner-config.ts` |
| Live classroom | `frontend/components/live/live-classroom.tsx`, `frontend/components/live/primary-live-classroom.tsx`, `frontend/components/live/live-interactive-panel.tsx`, `frontend/components/live/activity-creator.tsx` |
| Realtime | `frontend/lib/use-realtime.ts` |
| i18n | `frontend/i18n.ts`, `frontend/messages/en.json`, `frontend/messages/sw.json`, `frontend/hooks/use-locale.ts`, `frontend/components/locale-switcher.tsx` |
| Infrastructure | `docker-compose.yml`, `livekit.yaml`, `infrastructure/nginx/nginx.conf`, `infrastructure/monitoring/*` |
| Backend dashboard | `backend/elmkusoma-core/src/main/resources/application.yml` (port 8080, Flyway, Postgres, Redis, RabbitMQ) |
| Docs | `docs/README-01-SYSTEM-BACKEND.md` (Developer 01), `docs/README-02-USER-FRONTEND-LIVE.md` (this file) |

---

*Generated by Developer 02 — User Ecosystem + Frontend + Live + Complete Runbook Documentation. All statements verified against the repository at `/home/tally/elmkusoma` (branch `main`). No backend files were modified. No shared documentation files were modified.*
