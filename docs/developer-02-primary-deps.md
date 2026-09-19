# Developer 02 — Primary: Cross-Team Dependencies

## Shared Systems Reused

| System | Owner | How Primary Uses It |
|--------|-------|---------------------|
| Authentication (JWT) | Shared | All API calls require JWT via `@RequestAttribute("userId")` |
| Authorization (Roles) | Shared | `@PreAuthorize("hasRole('STUDENT')")` on student endpoints |
| User entity | Shared | `User.learningLevel = PRIMARY` for routing |
| Institution multi-tenancy | Shared | `institutionId` on all Primary entities via BaseEntity |
| LiveClass WebSocket | Shared | PrimaryLiveClassroom connects to `/ws/live-class/{classId}` |
| LiveKit | Shared | Live video/audio via existing LiveKit token generation |
| Student entity | Shared | Links student to class groups via StudentClassAssignment |
| TeacherAssignment | Shared | Links teachers to classes/subjects for My Teacher feature |
| Lesson/Assignment | Shared | Learning data via existing learningApi endpoints |
| Assessment | Shared | Quiz flow via existing assessmentApi endpoints |
| Attendance | Shared | Attendance records via existing dashboardApi endpoints |
| Enrollment | Shared | Subject enrollment via existing enrollmentApi |
| Notification (LearnerNotification) | Shared | Student notifications via existing entity |

## New Backend APIs Created (Primary Module)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/primary/me/teachers` | GET | Student's teachers |
| `/v1/primary/me/portfolio` | GET/POST/DELETE | Portfolio CRUD |
| `/v1/primary/me/badges` | GET | Achievement badges |
| `/v1/primary/me/streak` | GET | Learning streak |
| `/v1/primary/curriculum/subject/{id}/topics` | GET | Curriculum topics |
| `/v1/primary/me/notifications` | GET | Student notifications |
| `/v1/primary/me/learning-profile` | GET/PUT | Learning preferences |
| `/v1/primary/me/discovery` | GET/POST | Discovery entries |
| `/v1/primary/me/reading-adventures` | GET | Reading stories |
| `/v1/primary/me/reading-adventures/{id}/complete` | POST | Mark reading done |
| `/v1/primary/me/reading-adventures/{id}/favorite` | POST | Toggle favorite |
| `/v1/primary/live-class/{id}/activities` | GET/POST | Live activities |
| `/v1/primary/live-class/activities/{id}/respond` | POST | Respond to activity |
| `/v1/primary/me/evidence` | GET/POST | Learning evidence |
| `/v1/primary/me/passport` | GET | Learning passport |
| `/v1/primary/me/quests` | GET | Quest challenges |
| `/v1/primary/me/quests/{id}/complete` | POST | Complete quest |
| `/v1/primary/me/mistake-lab` | GET/POST | Mistake lab entries |
| `/v1/primary/me/collaborations` | GET | Collaborations |
| `/v1/primary/me/labs` | GET | ELMKUSOMA Labs |
| `/v1/primary/me/labs/{id}/attempt` | POST | Attempt lab |
| `/v1/primary/me/speaking` | GET/POST | Speaking activities |
| `/v1/primary/me/speaking/{id}/complete` | POST | Complete speaking |
| `/v1/primary/me/missions` | GET | Real world missions |
| `/v1/primary/me/missions/{id}/complete` | POST | Complete mission |

## New Entities Created

| Entity | Table | Purpose |
|--------|-------|---------|
| PortfolioItem | portfolio_items | Student creations |
| StudentBadge | student_badges | Achievement badges |
| CurriculumTopic | curriculum_topics | Primary curriculum |
| StudentStreak | student_streaks | Learning streaks |
| LearningProfile | learning_profiles | Student preferences |
| DiscoveryEntry | discovery_entries | I Wonder entries |
| ReadingAdventure | reading_adventures | Reading stories |
| LiveClassActivity | live_class_activities | Interactive activities |
| LiveClassResponse | live_class_responses | Activity responses |
| LearningEvidence | learning_evidence | Progress evidence |
| LearningPassport | learning_passports | Passport progress |
| QuestChallenge | quest_challenges | Challenges/quests |
| LearningCollaboration | learning_collaborations | Peer/family learning |
| MistakeLabEntry | mistake_lab_entries | Error review |
| ELmkusomaLab | elmkusoma_labs | Virtual experiments |
| SpeakingActivity | speaking_activities | Voice/creation |
| RealWorldMission | real_world_missions | Applied learning |

## Cross-Developer Dependencies

| Dependency | Needed From | Status |
|------------|-------------|--------|
| LiveClass entity fields | Dev 03 (Shared) | Uses existing LiveClass.title, subject, teacher |
| LiveKit token | Dev 03 (Shared) | Uses existing LiveKitService |
| WebSocket handler | Dev 03 (Shared) | Uses existing LiveClassWebSocketHandler |
| Teacher entities | Dev 03 (Shared) | Uses existing TeacherAssignment |
| Student entities | Dev 03 (Shared) | Uses existing Student, StudentClassAssignment |
| Subject/ClassGroup | Dev 03 (Shared) | Uses existing Subject, ClassGroup |

## Blockers

| Blocker | Impact | Mitigation |
|---------|--------|------------|
| No AI backend service | AI Learning Guide shows "Coming Soon" | Page exists, ready to connect when AI infra is available |
| LiveKit not deployed | Live video/audio won't work | WebSocket chat works, video requires deployment |
| No file upload service | Portfolio text only | Media module (elmkusoma-media) exists but not integrated |
