const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

export interface ApiError {
  success: false
  error?: string
  message?: string
  data?: Record<string, string>
  timestamp?: string
}

export interface ApiSuccess<T> {
  success: true
  data: T
  message?: string
  timestamp?: string
}

export type ApiResponse<T> = ApiSuccess<T> | ApiError

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_access_token")
}

function getInstitutionId(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_institution_id")
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("elmkusoma_access_token", accessToken)
  localStorage.setItem("elmkusoma_refresh_token", refreshToken)
}

export function clearTokens() {
  localStorage.removeItem("elmkusoma_access_token")
  localStorage.removeItem("elmkusoma_refresh_token")
  localStorage.removeItem("elmkusoma_current_user")
  localStorage.removeItem("elmkusoma_institution_id")
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_refresh_token")
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const institutionId = getInstitutionId()
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) || {}),
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`
  }

  if (institutionId) {
    headers["X-Institution-Id"] = institutionId
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const body = await res.json()

  if (!res.ok || body.success === false) {
    const errorMsg = body.error || body.message || `Request failed (${res.status})`
    throw new ApiRequestError(errorMsg, res.status, body)
  }

  return body.data !== undefined ? body.data : body as T
}

export class ApiRequestError extends Error {
  status: number
  body: unknown

  constructor(message: string, status: number, body?: unknown) {
    super(message)
    this.name = "ApiRequestError"
    this.status = status
    this.body = body
  }
}

export interface UserInfo {
  id: string
  email: string
  firstName: string
  lastName: string
  fullName?: string
  role: string
  institutionId: string
  classGroupId: string | null
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface RegisterPayload {
  firstName: string
  middleName?: string
  lastName: string
  email: string
  password: string
  phone?: string
  role: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const authApi = {
  register: (data: RegisterPayload) =>
    request<AuthResponse>("/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  login: (data: LoginPayload) =>
    request<AuthResponse>("/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  me: () => request<UserInfo>("/v1/auth/me"),
}

// Enrollment API
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

export const enrollmentApi = {
  list: (page = 0, size = 20) =>
    request<PageResponse<Enrollment>>(`/v1/enrollments?page=${page}&size=${size}`),
  byStudent: (studentId: string) =>
    request<Enrollment[]>(`/v1/enrollments/student/${studentId}`),
  byClass: (classGroupId: string) =>
    request<Enrollment[]>(`/v1/enrollments/class/${classGroupId}`),
  enroll: (data: { studentId: string; classGroupId: string; academicYearId: string }) =>
    request<Enrollment>("/v1/enrollments", { method: "POST", body: JSON.stringify(data) }),
  updateStatus: (id: string, status: string) =>
    request<Enrollment>(`/v1/enrollments/${id}/status?status=${status}`, { method: "PUT" }),
  transfer: (id: string, data: { toClassGroupId: string; reason?: string }) =>
    request<{ id: string }>(`/v1/enrollments/${id}/transfer`, { method: "POST", body: JSON.stringify(data) }),
}

// Learning API
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

export const learningApi = {
  getLessons: (subjectId: string, classGroupId: string) =>
    request<Lesson[]>(`/v1/learning/lessons/subject/${subjectId}/class/${classGroupId}`),
  getLessonsByClass: (classGroupId: string) =>
    request<Lesson[]>(`/v1/learning/lessons/class/${classGroupId}`),
  createLesson: (data: Partial<Lesson>) =>
    request<Lesson>("/v1/learning/lessons", { method: "POST", body: JSON.stringify(data) }),
  updateProgress: (lessonId: string, completionPercentage: number) =>
    request<LessonProgress>("/v1/learning/progress", { method: "POST", body: JSON.stringify({ lessonId, completionPercentage }) }),
  getStudentProgress: (studentId: string) =>
    request<LessonProgress[]>(`/v1/learning/progress/student/${studentId}`),
  getAssignments: (classGroupId: string) =>
    request<Assignment[]>(`/v1/learning/assignments/class/${classGroupId}`),
  createAssignment: (data: Partial<Assignment>) =>
    request<Assignment>("/v1/learning/assignments", { method: "POST", body: JSON.stringify(data) }),
  submitAssignment: (assignmentId: string) =>
    request<AssignmentSubmission>(`/v1/learning/assignments/${assignmentId}/submit`, { method: "POST" }),
  getSubmissions: (assignmentId: string) =>
    request<AssignmentSubmission[]>(`/v1/learning/assignments/${assignmentId}/submissions`),
}

// Assessment API
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

export const assessmentApi = {
  getByClass: (classGroupId: string) =>
    request<Assessment[]>(`/v1/assessments/class/${classGroupId}`),
  getBySubject: (subjectId: string) =>
    request<Assessment[]>(`/v1/assessments/subject/${subjectId}`),
  create: (data: Partial<Assessment>) =>
    request<Assessment>("/v1/assessments", { method: "POST", body: JSON.stringify(data) }),
  getQuestions: (assessmentId: string) =>
    request<Question[]>(`/v1/assessments/${assessmentId}/questions`),
  addQuestion: (assessmentId: string, data: Partial<Question>) =>
    request<Question>(`/v1/assessments/${assessmentId}/questions`, { method: "POST", body: JSON.stringify(data) }),
  startAttempt: (assessmentId: string) =>
    request<Attempt>(`/v1/assessments/${assessmentId}/start`, { method: "POST" }),
  submitAttempt: (attemptId: string, answers: Array<{ questionId: string; selectedOptionId?: string; textAnswer?: string }>) =>
    request<Attempt>(`/v1/assessments/attempts/${attemptId}/submit`, { method: "POST", body: JSON.stringify({ answers }) }),
  getResults: (assessmentId: string) =>
    request<AssessmentResult[]>(`/v1/assessments/${assessmentId}/results`),
  getResult: (assessmentId: string, studentId: string) =>
    request<AssessmentResult>(`/v1/assessments/${assessmentId}/results/student/${studentId}`),
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
