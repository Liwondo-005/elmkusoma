const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

interface ApiOptions {
  method?: string
  body?: unknown
  headers?: Record<string, string>
}

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_access_token")
}

function getInstitutionId(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_institution_id")
}

export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const token = getToken()
  const institutionId = getInstitutionId()

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...options.headers,
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`
  }

  if (institutionId) {
    headers["X-Institution-Id"] = institutionId
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method: options.method || "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  const json = await res.json()

  if (!res.ok || !json.success) {
    throw new Error(json.error || json.message || "Request failed")
  }

  return json.data as T
}

export interface AuthUser {
  id: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  role: string
  institutionId: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export interface Enrollment {
  id: string
  studentId: string
  classGroupId: string
  academicYearId: string
  status: "PENDING" | "ENROLLED" | "WITHDRAWN" | "COMPLETED"
  enrolledAt: string
  withdrawnAt?: string
  completedAt?: string
  createdAt: string
}

export interface Lesson {
  id: string
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  contentText?: string
  videoUrl?: string
  fileAttachments?: string
  sortOrder: number
  isPublished: boolean
  createdAt: string
}

export interface LessonProgress {
  id: string
  lessonId: string
  studentId: string
  completionPercentage: number
  startedAt?: string
  completedAt?: string
  createdAt: string
}

export interface Assignment {
  id: string
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  dueDate?: string
  totalMarks: number
  attachments?: string
  createdAt: string
}

export interface AssignmentSubmission {
  id: string
  assignmentId: string
  studentId: string
  fileUrl?: string
  submittedAt: string
  grade?: number
  feedback?: string
  gradedAt?: string
  createdAt: string
}

export interface Assessment {
  id: string
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  timeLimitMinutes?: number
  totalMarks: number
  passMarks: number
  isPublished: boolean
  startsAt?: string
  endsAt?: string
  createdAt: string
}

export interface Question {
  id: string
  assessmentId: string
  questionType: "MCQ" | "TRUE_FALSE" | "SHORT_ANSWER" | "ESSAY"
  questionText: string
  marks: number
  sortOrder: number
  options: Option[]
}

export interface Option {
  id: string
  questionId: string
  optionText: string
  isCorrect: boolean
  sortOrder: number
}

export interface Attempt {
  id: string
  assessmentId: string
  studentId: string
  startedAt: string
  submittedAt?: string
  isCompleted: boolean
  answers?: Answer[]
  result?: AssessmentResult
}

export interface Answer {
  id: string
  attemptId: string
  questionId: string
  selectedOptionId?: string
  textAnswer?: string
  isCorrect?: boolean
  marksObtained?: number
}

export interface AssessmentResult {
  id: string
  assessmentId: string
  studentId: string
  attemptId: string
  totalScore: number
  isPassed: boolean
  gradedAt?: string
  feedback?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// Auth API
export const authApi = {
  register: (data: { firstName: string; middleName?: string; lastName: string; email: string; password: string; phone?: string; role: string }) =>
    api<AuthResponse>("/v1/auth/register", { method: "POST", body: data }),
  login: (data: { email: string; password: string }) =>
    api<AuthResponse>("/v1/auth/login", { method: "POST", body: data }),
  me: () => api<AuthUser>("/v1/auth/me"),
}

// Enrollment API
export const enrollmentApi = {
  list: (page = 0, size = 20) =>
    api<PageResponse<Enrollment>>(`/v1/enrollments?page=${page}&size=${size}`),
  byStudent: (studentId: string) =>
    api<Enrollment[]>(`/v1/enrollments/student/${studentId}`),
  byClass: (classGroupId: string) =>
    api<Enrollment[]>(`/v1/enrollments/class/${classGroupId}`),
  enroll: (data: { studentId: string; classGroupId: string; academicYearId: string }) =>
    api<Enrollment>("/v1/enrollments", { method: "POST", body: data }),
  updateStatus: (id: string, status: string) =>
    api<Enrollment>(`/v1/enrollments/${id}/status?status=${status}`, { method: "PUT" }),
  transfer: (id: string, data: { toClassGroupId: string; reason?: string }) =>
    api<{ id: string }>(`/v1/enrollments/${id}/transfer`, { method: "POST", body: data }),
}

// Learning API
export const learningApi = {
  getLessons: (subjectId: string, classGroupId: string) =>
    api<Lesson[]>(`/v1/learning/lessons/subject/${subjectId}/class/${classGroupId}`),
  getLessonsByClass: (classGroupId: string) =>
    api<Lesson[]>(`/v1/learning/lessons/class/${classGroupId}`),
  createLesson: (data: Partial<Lesson>) =>
    api<Lesson>("/v1/learning/lessons", { method: "POST", body: data }),
  updateProgress: (lessonId: string, completionPercentage: number) =>
    api<LessonProgress>("/v1/learning/progress", { method: "POST", body: { lessonId, completionPercentage } }),
  getStudentProgress: (studentId: string) =>
    api<LessonProgress[]>(`/v1/learning/progress/student/${studentId}`),
  getAssignments: (classGroupId: string) =>
    api<Assignment[]>(`/v1/learning/assignments/class/${classGroupId}`),
  createAssignment: (data: Partial<Assignment>) =>
    api<Assignment>("/v1/learning/assignments", { method: "POST", body: data }),
  submitAssignment: (assignmentId: string) =>
    api<AssignmentSubmission>(`/v1/learning/assignments/${assignmentId}/submit`, { method: "POST" }),
  getSubmissions: (assignmentId: string) =>
    api<AssignmentSubmission[]>(`/v1/learning/assignments/${assignmentId}/submissions`),
}

// Assessment API
export const assessmentApi = {
  getByClass: (classGroupId: string) =>
    api<Assessment[]>(`/v1/assessments/class/${classGroupId}`),
  getBySubject: (subjectId: string) =>
    api<Assessment[]>(`/v1/assessments/subject/${subjectId}`),
  create: (data: Partial<Assessment>) =>
    api<Assessment>("/v1/assessments", { method: "POST", body: data }),
  getQuestions: (assessmentId: string) =>
    api<Question[]>(`/v1/assessments/${assessmentId}/questions`),
  addQuestion: (assessmentId: string, data: Partial<Question>) =>
    api<Question>(`/v1/assessments/${assessmentId}/questions`, { method: "POST", body: data }),
  startAttempt: (assessmentId: string) =>
    api<Attempt>(`/v1/assessments/${assessmentId}/start`, { method: "POST" }),
  submitAttempt: (attemptId: string, answers: Array<{ questionId: string; selectedOptionId?: string; textAnswer?: string }>) =>
    api<Attempt>(`/v1/assessments/attempts/${attemptId}/submit`, { method: "POST", body: { answers } }),
  getResults: (assessmentId: string) =>
    api<AssessmentResult[]>(`/v1/assessments/${assessmentId}/results`),
  getResult: (assessmentId: string, studentId: string) =>
    api<AssessmentResult>(`/v1/assessments/${assessmentId}/results/student/${studentId}`),
}
