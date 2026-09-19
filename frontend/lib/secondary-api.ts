const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function secondaryFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  let institutionId = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_institution_id") : null
  if (!institutionId && typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("elmkusoma_current_user")
      if (raw) {
        const user = JSON.parse(raw)
        if (user?.institutionId) institutionId = user.institutionId
      }
    } catch {}
  }
  if (!institutionId) institutionId = "00000000-0000-0000-0000-000000000001"
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

export interface SecondaryDashboardSummary {
  studentId: string
  admissionNumber: string
  status: string
  totalEnrollments: number
  activeEnrollments: number
  totalReportCards: number
  monthAttendanceTotal: number
  monthAttendancePresent: number
  monthAttendanceRate: number
  totalLessonsStarted: number
  completedLessons: number
  overallAverage: number
}

export interface ContinueLearningItem {
  lessonId: string
  title?: string
  subjectName?: string
  completionPercentage: number
  startedAt?: string
}

export interface RecentActivity {
  type: string
  lessonId?: string
  title?: string
  subjectName?: string
  completedAt?: string
  date?: string
  status?: string
}

export interface SubjectSummary {
  id: string
  name: string
  code: string
  description?: string
  educationLevel: string
  totalLessons?: number
  completedLessons?: number
  averageScore?: number
  upcomingAssessments?: number
  hasTeacherFeedback?: boolean
  topicStatus?: string
}

export interface UpcomingAssessment {
  id: string
  title: string
  subjectName?: string
  scheduledAt?: string
  startsAt?: string
  endsAt?: string
  totalMarks: number
  timeLimitMinutes?: number
  status: string
}

export interface AssignmentSummary {
  id: string
  title: string
  subjectName?: string
  dueDate?: string
  status: string
  totalMarks: number
  grade?: number
}

export interface LiveClassSummary {
  id: string
  title: string
  subjectName?: string
  scheduledAt: string
  durationMinutes: number
  status: string
  meetingUrl?: string
}

export interface AttendanceSummary {
  totalDays: number
  present: number
  absent: number
  late: number
  excused: number
  attendanceRate: number
}

export interface ProgressBySubject {
  subjectId: string
  subjectName: string
  completedTopics: number
  totalTopics: number
  averageScore: number
  status: string
}

export interface TeacherFeedback {
  id: string
  teacherName: string
  subjectName?: string
  message: string
  createdAt: string
}

export interface NextStep {
  type: "lesson" | "assessment" | "assignment" | "revision" | "live_class" | "none"
  title: string
  subtitle: string
  subjectName?: string
  actionLabel: string
  actionHref: string
  icon?: string
}

export interface TodayItem {
  id: string
  time?: string
  title: string
  subjectName?: string
  type: "lesson" | "assessment" | "assignment" | "live_class" | "revision"
  status: string
  href: string
}

export interface FocusItem {
  subjectName: string
  topicName?: string
  reason: string
  type: "practice" | "review" | "improve" | "assessment"
  subjectId?: string
}

export interface AcademicPulse {
  topicsCompleted: number
  assessmentsUpcoming: number
  practiceUnfinished: number
  feedbackAvailable: number
  lessonsCompleted: number
  attendanceRate: number
}

export const secondaryApi = {
  getDashboardSummary: () =>
    secondaryFetch<SecondaryDashboardSummary>("/v1/student/dashboard/summary"),

  getContinueLearning: () =>
    secondaryFetch<ContinueLearningItem[]>("/v1/student/dashboard/continue-learning"),

  getRecentActivity: () =>
    secondaryFetch<RecentActivity[]>("/v1/student/dashboard/recent-activity"),

  getResults: () =>
    secondaryFetch<any[]>("/v1/student/dashboard/results"),

  getAttendance: () =>
    secondaryFetch<AttendanceSummary>("/v1/student/dashboard/attendance"),

  getLiveClasses: () =>
    secondaryFetch<LiveClassSummary[]>("/v1/student/dashboard/live-classes"),

  getSubjects: (classGroupId: string) =>
    secondaryFetch<SubjectSummary[]>(`/v1/academic/subjects?classGroupId=${classGroupId}`),

  getSubject: (id: string) =>
    secondaryFetch<SubjectSummary>(`/v1/academic/subjects/${id}`),

  getLessons: (classId: string) =>
    secondaryFetch<any[]>(`/v1/learning/lessons/class/${classId}`),

  getLesson: (id: string) =>
    secondaryFetch<any>(`/v1/learning/lessons/${id}`),

  getAssignments: (classId: string) =>
    secondaryFetch<AssignmentSummary[]>(`/v1/learning/assignments/class/${classId}`),

  getAssessments: (classId: string) =>
    secondaryFetch<UpcomingAssessment[]>(`/v1/assessments/class/${classId}`),

  getClassGroup: (id: string) =>
    secondaryFetch<any>(`/v1/academic/class-groups/${id}`),

  getGrade: (id: string) =>
    secondaryFetch<any>(`/v1/academic/grades/${id}`),

  getStudentProfile: (studentId: string) =>
    secondaryFetch<any>(`/v1/students/${studentId}`),

  getLessonsBySubject: (subjectId: string, classGroupId: string) =>
    secondaryFetch<any[]>(`/v1/learning/lessons/subject/${subjectId}/class/${classGroupId}`),

  getLessonProgress: (studentId: string) =>
    secondaryFetch<any[]>(`/v1/learning/progress/student/${studentId}`),

  updateLessonProgress: (lessonId: string, completionPercentage: number) =>
    secondaryFetch<any>("/v1/learning/progress", {
      method: "POST",
      body: JSON.stringify({ lessonId, completionPercentage }),
    }),

  // Concept Bank
  getConcepts: (subjectId: string) =>
    secondaryFetch<SecondaryConcept[]>(`/v1/secondary/extended/concepts?subjectId=${subjectId}`),
  getConceptsByClass: (classGroupId: string) =>
    secondaryFetch<SecondaryConcept[]>(`/v1/secondary/extended/concepts/class/${classGroupId}`),
  createConcept: (data: Partial<SecondaryConcept>) =>
    secondaryFetch<SecondaryConcept>("/v1/secondary/extended/concepts", { method: "POST", body: JSON.stringify(data) }),

  // Problem Bank
  getProblems: (subjectId: string) =>
    secondaryFetch<SecondaryProblem[]>(`/v1/secondary/extended/problems?subjectId=${subjectId}`),
  getProblemsByClass: (classGroupId: string) =>
    secondaryFetch<SecondaryProblem[]>(`/v1/secondary/extended/problems/class/${classGroupId}`),
  createProblem: (data: Partial<SecondaryProblem>) =>
    secondaryFetch<SecondaryProblem>("/v1/secondary/extended/problems", { method: "POST", body: JSON.stringify(data) }),

  // Error Bank
  getErrors: (subjectId: string) =>
    secondaryFetch<SecondaryError[]>(`/v1/secondary/extended/errors?subjectId=${subjectId}`),
  getErrorsByClass: (classGroupId: string) =>
    secondaryFetch<SecondaryError[]>(`/v1/secondary/extended/errors/class/${classGroupId}`),
  createError: (data: Partial<SecondaryError>) =>
    secondaryFetch<SecondaryError>("/v1/secondary/extended/errors", { method: "POST", body: JSON.stringify(data) }),

  // Study Planner
  getStudyPlans: (studentId: string) =>
    secondaryFetch<SecondaryStudyPlan[]>(`/v1/secondary/extended/study-planner/student/${studentId}`),
  createStudyPlan: (data: Partial<SecondaryStudyPlan>) =>
    secondaryFetch<SecondaryStudyPlan>("/v1/secondary/extended/study-planner", { method: "POST", body: JSON.stringify(data) }),
  updateStudyPlan: (id: string, data: Partial<SecondaryStudyPlan>) =>
    secondaryFetch<SecondaryStudyPlan>(`/v1/secondary/extended/study-planner/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  // Teacher Feedback (reuse existing lesson progress/subject data)
  getTeacherFeedback: (subjectId: string) =>
    secondaryFetch<any[]>(`/v1/secondary/extended/concepts?subjectId=${subjectId}`),
}

export interface SecondaryConcept {
  id: string
  subjectId: string
  classGroupId: string
  conceptName: string
  conceptDescription: string
  examples: string | null
  relatedConcepts: string | null
  difficultyLevel: "BASIC" | "INTERMEDIATE" | "ADVANCED"
  category: string | null
  createdAt: string
}

export interface SecondaryProblem {
  id: string
  subjectId: string
  classGroupId: string
  problemTitle: string
  problemDescription: string
  problemType: "MCQ" | "SHORT_ANSWER" | "LONG_ANSWER" | "NUMERICAL" | "TRUE_FALSE"
  options: string | null
  correctAnswer: string
  solution: string | null
  difficultyLevel: "BASIC" | "INTERMEDIATE" | "ADVANCED"
  marks: number
  createdAt: string
}

export interface SecondaryError {
  id: string
  subjectId: string
  classGroupId: string
  errorTitle: string
  errorDescription: string
  incorrectExample: string | null
  correctExample: string | null
  explanation: string
  category: string | null
  frequency: "COMMON" | "OCCASIONAL" | "RARE"
  createdAt: string
}

export interface SecondaryStudyPlan {
  id: string
  studentId: string
  classGroupId: string
  subjectId: string | null
  topicName: string
  plannedDate: string
  durationMinutes: number
  status: "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "SKIPPED"
  priority: "HIGH" | "MEDIUM" | "LOW"
  notes: string | null
  completedDate: string | null
  createdAt: string
}
