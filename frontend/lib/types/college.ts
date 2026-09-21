export type ProgrammeType = "DIPLOMA" | "CERTIFICATE" | "DEGREE" | "HIGHER_DIPLOMA" | "PROFESSIONAL"
export type EducationLevelType = "COLLEGE" | "VETA" | "UNIVERSITY"

export interface Programme {
  id: string
  institutionId: string
  name: string
  code: string
  description?: string
  programmeType: ProgrammeType
  educationLevel: EducationLevelType
  durationMonths?: number
  creditHours?: number
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface Department {
  id: string
  institutionId: string
  name: string
  code: string
  description?: string
  programmeIds?: string[]
  headOfDepartmentId?: string
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export type CompetencyStatus = "NOT_STARTED" | "LEARNING" | "PRACTICING" | "ASSESSED" | "COMPETENT" | "NEEDS_PRACTICE" | "COMPLETED"
export type CompetencyType = "SKILL" | "KNOWLEDGE" | "PRACTICAL" | "PROFESSIONAL"

export interface Competency {
  id: string
  institutionId: string
  name: string
  code?: string
  description?: string
  competencyType: CompetencyType
  programmeId?: string
  subjectId?: string
  sortOrder?: number
  isActive: boolean
  createdAt: string
}

export interface CompetencyRecord {
  id: string
  institutionId: string
  studentId: string
  competencyId: string
  competencyName?: string
  competencyType?: CompetencyType
  status: CompetencyStatus
  evidence?: string
  assessedBy?: string
  assessmentDate?: string
  lastPracticeDate?: string
  notes?: string
  createdAt: string
  updatedAt?: string
}

export interface CompetencySummary {
  total: number
  notStarted: number
  learning: number
  practicing: number
  assessed: number
  competent: number
  needsPractice: number
  completed: number
}

export type ProjectStatus = "IDEATION" | "PLANNING" | "IN_PROGRESS" | "REVIEW" | "COMPLETED" | "ARCHIVED"
export type SubmissionType = "DOCUMENT" | "IMAGE" | "VIDEO" | "CODE" | "PRESENTATION" | "FILE" | "OTHER"

export interface Project {
  id: string
  institutionId: string
  studentId: string
  subjectId?: string
  title: string
  objective?: string
  description?: string
  status: ProjectStatus
  instructorId?: string
  startDate?: string
  dueDate?: string
  completedDate?: string
  createdAt: string
  updatedAt?: string
}

export interface ProjectMilestone {
  id: string
  projectId: string
  title: string
  description?: string
  dueDate?: string
  isCompleted: boolean
  completedDate?: string
  sortOrder?: number
}

export interface ProjectSubmission {
  id: string
  projectId: string
  milestoneId?: string
  submissionType: SubmissionType
  title?: string
  fileUrl?: string
  description?: string
  feedback?: string
  grade?: string
  submittedAt?: string
}

export type PlacementStatus = "PLANNING" | "ACTIVE" | "COMPLETED" | "TERMINATED"

export interface FieldworkPlacement {
  id: string
  institutionId: string
  studentId: string
  programmeId?: string
  organisationName: string
  placementTitle: string
  supervisorName?: string
  supervisorEmail?: string
  supervisorPhone?: string
  institutionSupervisorId?: string
  startDate?: string
  endDate?: string
  status: PlacementStatus
  totalHoursRequired?: number
  totalHoursCompleted?: number
  objectives?: string
  remarks?: string
  createdAt: string
  updatedAt?: string
}

export interface LogbookEntry {
  id: string
  placementId: string
  entryDate: string
  activities: string
  hoursWorked?: number
  skillsUsed?: string
  challenges?: string
  learningOutcomes?: string
  supervisorComments?: string
  isApproved: boolean
  approvedBy?: string
  approvedAt?: string
  createdAt: string
}

// Portfolio types
export type PortfolioItemType = "DOCUMENT" | "CERTIFICATE" | "PROJECT_EVIDENCE" | "MEDIA" | "ACHIEVEMENT" | "WORK_SAMPLE" | "REFERENCE" | "OTHER"
export type Visibility = "PRIVATE" | "INSTITUTION" | "PUBLIC"

export interface Portfolio {
  id: string
  institutionId: string
  studentId: string
  title: string
  visibility: Visibility
  isActive: boolean
  items?: PortfolioItem[]
  createdAt: string
  updatedAt?: string
}

export interface PortfolioItem {
  id: string
  portfolioId: string
  title: string
  description?: string
  itemType: PortfolioItemType
  fileUrl?: string
  competencyId?: string
  projectId?: string
  dateObtained?: string
  sortOrder?: number
  isVisible: boolean
  createdAt: string
}

// Practical Demonstration types
export type DemonstrationStatus = "DRAFT" | "SUBMITTED" | "UNDER_REVIEW" | "APPROVED" | "NEEDS_REVISION" | "REJECTED"

export interface PracticalDemonstration {
  id: string
  institutionId: string
  studentId: string
  competencyId?: string
  projectId?: string
  title: string
  description?: string
  mediaUrls?: string
  status: DemonstrationStatus
  reviewerId?: string
  reviewNotes?: string
  score?: number
  reviewedAt?: string
  submittedAt?: string
  createdAt: string
  updatedAt?: string
}

// Research types
export type ResearchStatus = "IDEA" | "QUESTION" | "LITERATURE" | "PROPOSAL" | "METHODOLOGY" | "DATA_COLLECTION" | "ANALYSIS" | "WRITING" | "REVIEW" | "REVISION" | "DEFENSE" | "COMPLETED"
export type ThesisStatus = "NOT_STARTED" | "PROPOSAL" | "IN_PROGRESS" | "REVIEW" | "REVISION" | "DEFENSE_SCHEDULED" | "DEFENSE_COMPLETE" | "COMPLETED"
export type ResourceType = "ARTICLE" | "BOOK" | "THESIS" | "WEBSITE" | "DATASET" | "CODE" | "OTHER"

export interface ResearchProject {
  id: string
  institutionId: string
  studentId: string
  title: string
  researchQuestion?: string
  objectives?: string
  supervisorId?: string
  status: ResearchStatus
  programmeId?: string
  subjectId?: string
  methodology?: string
  startDate?: string
  dueDate?: string
  completedDate?: string
  abstractText?: string
  keywords?: string
  createdAt: string
  updatedAt?: string
}

export interface ResearchMilestone {
  id: string
  researchProjectId: string
  title: string
  description?: string
  dueDate?: string
  isCompleted: boolean
  completedDate?: string
  sortOrder?: number
}

export interface ResearchResource {
  id: string
  researchProjectId: string
  title: string
  description?: string
  resourceType: ResourceType
  fileUrl?: string
  citation?: string
  sortOrder?: number
}

export interface Thesis {
  id: string
  institutionId: string
  studentId: string
  title: string
  researchProjectId?: string
  supervisorId?: string
  status: ThesisStatus
  programmeId?: string
  submissionDate?: string
  defenseDate?: string
  finalGrade?: string
  abstractText?: string
  wordCount?: number
  createdAt: string
}

// Study Planner types
export type StudyTaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT"
export type StudyTaskType = "STUDY" | "REVISION" | "ASSIGNMENT" | "PROJECT" | "RESEARCH" | "EXAM_PREP" | "REPLAY" | "OTHER"

export interface StudyTask {
  id: string
  institutionId: string
  studentId: string
  title: string
  description?: string
  taskType: StudyTaskType
  priority: StudyTaskPriority
  subjectId?: string
  scheduledDate?: string
  scheduledTime?: string
  durationMinutes?: number
  isCompleted: boolean
  completedDate?: string
  notes?: string
  createdAt: string
}

// Higher Education Dashboard types
export type EnrollmentStatusType = "ENROLLED" | "IN_PROGRESS" | "COMPLETED" | "DROPPED" | "WITHDRAWN" | "FAILED"

export interface StudentCourseEnrollment {
  id: string
  studentId: string
  courseId: string
  programmeId?: string
  semester?: string
  academicYear?: string
  creditHours?: number
  status: EnrollmentStatusType
  enrolledDate?: string
  completedDate?: string
  grade?: string
  gradePoints?: number
  instructorId?: string
  institutionId: string
}

export interface AcademicRecord {
  id: string
  studentId: string
  programmeId?: string
  academicYear?: string
  semester?: string
  totalCreditHours?: number
  earnedCreditHours?: number
  semesterGpa?: number
  cumulativeGpa?: number
  totalCourses?: number
  completedCourses?: number
  failedCourses?: number
  academicStanding?: string
  classRank?: number
  totalStudentsInClass?: number
  institutionId: string
}

export interface CareerProfile {
  id: string
  studentId: string
  careerObjective?: string
  targetIndustry?: string
  targetRole?: string
  skills?: string
  certifications?: string
  experienceSummary?: string
  cvFileUrl?: string
  linkedinUrl?: string
  portfolioUrl?: string
  isPublic: boolean
  institutionId: string
}

export interface WhatsNext {
  title: string
  description: string
  type: string
  url?: string
  deadline?: string
}

export interface TodayItem {
  title: string
  type: string
  time?: string
  status: string
}

export interface TodayView {
  totalTasks: number
  completedTasks: number
  pendingTasks: number
  items: TodayItem[]
}

export interface ContinueLearning {
  lastCourse?: string
  lastModule?: string
  progressPercent: number
  courseId?: string
}

export interface LiveSessionSummary {
  id: string
  title: string
  sessionType: string
  startTime: string
  status: string
}

export interface LiveCampus {
  liveNow: number
  upcomingToday: number
  sessions: LiveSessionSummary[]
}

export interface CourseSummary {
  id: string
  title: string
  progressPercent: number
  totalModules: number
  completedModules: number
}

export interface AcademicLoad {
  totalCreditHours: number
  enrolledCourses: number
  completedCreditHours: number
  currentSemesterCourses: number
  currentSemesterGpa?: number
  cumulativeGpa?: number
}

export interface MyProgress {
  cumulativeGpa?: number
  semesterGpa?: number
  totalCourses: number
  completedCourses: number
  competenciesCompleted: number
  competenciesTotal: number
  projectsCompleted: number
  projectsTotal: number
  academicStanding?: string
}

export interface MyEvidence {
  portfolioItems: number
  demonstrations: number
  projectSubmissions: number
  logbookEntries: number
  competenciesRecorded: number
}

export interface CareerWorld {
  careerObjective?: string
  targetIndustry?: string
  targetRole?: string
  skillsCount: number
  hasProfile: boolean
}

export interface DayItem {
  title: string
  type: string
  time?: string
  date?: string
  status: string
}

export interface DayWeekView {
  todayItems: DayItem[]
  weekItems: DayItem[]
}

export interface HigherEducationDashboard {
  academicContext: string
  programmeName?: string
  departmentName?: string
  academicYear?: string
  semester?: string
  whatsNext?: WhatsNext
  today?: TodayView
  continueLearning?: ContinueLearning
  liveCampus?: LiveCampus
  myCourses: CourseSummary[]
  academicLoad?: AcademicLoad
  projects: CourseSummary[]
  research: ResearchProject[]
  myProgress?: MyProgress
  myEvidence?: MyEvidence
  careerWorld?: CareerWorld
  dayWeekView?: DayWeekView
  studyPlannerTasks: StudyTask[]
}

export interface DeepContent {
  id: string
  studentId: string
  courseId?: string
  moduleId?: string
  title: string
  contentType: string
  contentText?: string
  fileUrl?: string
  difficultyLevel?: string
  tags?: string
  isCompleted?: boolean
  timeSpentMinutes?: number
  notes?: string
}

export interface DeepContentDto {
  studentId: string
  courseId?: string
  moduleId?: string
  title: string
  contentType: string
  contentText?: string
  fileUrl?: string
  difficultyLevel?: string
  tags?: string
  isCompleted?: boolean
  timeSpentMinutes?: number
  notes?: string
}

export interface LearnerNotification {
  id: string
  userId: string
  title: string
  message: string
  notificationType?: string
  targetType?: string
  targetId?: string
  isRead?: boolean
  createdAt?: string
}

export interface StudentEvent {
  id: string
  title: string
  description?: string
  eventType?: string
  category?: string
  startDateTime?: string
  endDateTime?: string
  location?: string
  institutionId?: string
  organizerId?: string
  maxParticipants?: number
  currentParticipants?: number
  isRegistered?: boolean
}

export interface EventRegistration {
  id: string
  eventId: string
  userId: string
  status?: string
  registeredAt?: string
}

export interface GpaResult {
  semesterGpa?: number | null
  cumulativeGpa?: number | null
  totalCreditHours?: number
  earnedCreditHours?: number
  academicStanding?: string
}
