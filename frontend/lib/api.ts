const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

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

export function getInstitutionId(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_institution_id")
}

export function setInstitutionId(id: string) {
  localStorage.setItem("elmkusoma_institution_id", id)
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split(".")[1]
    const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"))
    return JSON.parse(json)
  } catch {
    return null
  }
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
  learningLevel?: string | null
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
  learningLevel?: string
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

  forgotPassword: (data: { email: string }) =>
    request<void>("/v1/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  refresh: (refreshToken: string) =>
    request<AuthResponse>("/v1/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),
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

// ── Academic API ─────────────────────────────────────────────────

export interface AcademicYear {
  id: string
  institutionId: string
  educationLevel: string
  yearLabel: string
  startDate: string
  endDate: string
  isCurrent: boolean
  isActive: boolean
  createdAt: string
}

export interface Term {
  id: string
  institutionId: string
  academicYearId: string
  name: string
  termNumber: number
  startDate: string
  endDate: string
  isActive: boolean
  createdAt: string
}

export interface Grade {
  id: string
  institutionId: string
  educationLevel: string
  name: string
  code?: string
  sortOrder: number
  isActive: boolean
  createdAt: string
}

export interface Subject {
  id: string
  institutionId: string
  educationLevel: string
  name: string
  code?: string
  description?: string
  category?: string
  isActive: boolean
  createdAt: string
}

export interface ClassGroup {
  id: string
  institutionId: string
  gradeId: string
  academicYearId: string
  termId: string
  name: string
  section?: string
  capacity?: number
  classTeacherId?: string
  isActive: boolean
  createdAt: string
}

export const academicApi = {
  // Academic Years
  getAcademicYears: (institutionId: string, educationLevel?: string) => {
    const params = new URLSearchParams({ institutionId })
    if (educationLevel) params.set("educationLevel", educationLevel)
    return request<AcademicYear[]>(`/v1/academic/years?${params}`)
  },
  getAcademicYear: (id: string) =>
    request<AcademicYear>(`/v1/academic/years/${id}`),
  createAcademicYear: (data: {
    institutionId: string
    educationLevel: string
    yearLabel: string
    startDate: string
    endDate: string
    isCurrent?: boolean
  }) =>
    request<AcademicYear>("/v1/academic/years", { method: "POST", body: JSON.stringify(data) }),

  // Terms
  getTerms: (academicYearId: string) =>
    request<Term[]>(`/v1/academic/years/${academicYearId}/terms`),
  createTerm: (academicYearId: string, data: {
    institutionId: string
    name: string
    termNumber: number
    startDate: string
    endDate: string
  }) =>
    request<Term>(`/v1/academic/years/${academicYearId}/terms`, { method: "POST", body: JSON.stringify(data) }),

  // Grades
  getGrades: (institutionId: string, educationLevel?: string) => {
    const params = new URLSearchParams({ institutionId })
    if (educationLevel) params.set("educationLevel", educationLevel)
    return request<Grade[]>(`/v1/academic/grades?${params}`)
  },
  getGrade: (id: string) =>
    request<Grade>(`/v1/academic/grades/${id}`),
  createGrade: (data: {
    institutionId: string
    educationLevel: string
    name: string
    code?: string
    sortOrder?: number
  }) =>
    request<Grade>("/v1/academic/grades", { method: "POST", body: JSON.stringify(data) }),

  // Subjects
  getSubjects: (institutionId: string, educationLevel?: string) => {
    const params = new URLSearchParams({ institutionId })
    if (educationLevel) params.set("educationLevel", educationLevel)
    return request<Subject[]>(`/v1/academic/subjects?${params}`)
  },
  getSubject: (id: string) =>
    request<Subject>(`/v1/academic/subjects/${id}`),
  createSubject: (data: {
    institutionId: string
    educationLevel: string
    name: string
    code?: string
    description?: string
  }) =>
    request<Subject>("/v1/academic/subjects", { method: "POST", body: JSON.stringify(data) }),

  // Class Groups
  getClassGroups: (institutionId: string, gradeId?: string, termId?: string) => {
    const params = new URLSearchParams({ institutionId })
    if (gradeId) params.set("gradeId", gradeId)
    if (termId) params.set("termId", termId)
    return request<ClassGroup[]>(`/v1/academic/classes?${params}`)
  },
  getClassGroup: (id: string) =>
    request<ClassGroup>(`/v1/academic/classes/${id}`),
  createClassGroup: (data: {
    institutionId: string
    gradeId: string
    academicYearId: string
    termId: string
    name: string
    section?: string
    capacity?: number
  }) =>
    request<ClassGroup>("/v1/academic/classes", { method: "POST", body: JSON.stringify(data) }),
}

// ── Student API ──────────────────────────────────────────────────

export interface Student {
  id: string
  userId: string
  institutionId: string
  admissionNumber: string
  status: string
  firstName: string
  middleName?: string
  lastName: string
  email: string
  phone?: string
  dateOfBirth?: string
  gender?: string
  address?: string
  city?: string
  region?: string
  guardianName?: string
  guardianPhone?: string
  guardianRelationship?: string
  enrollmentDate: string
  createdAt: string
}

export interface StudentClassAssignment {
  id: string
  institutionId: string
  studentId: string
  classGroupId: string
  academicYearId: string
  termId: string
  assignedDate: string
  isActive: boolean
  createdAt: string
}

export const studentApi = {
  getStudents: (institutionId: string, classId?: string, query?: string) => {
    const params = new URLSearchParams({ institutionId })
    if (classId) params.set("classId", classId)
    if (query) params.set("query", query)
    return request<Student[]>(`/v1/students?${params}`)
  },
  getStudent: (id: string) =>
    request<Student>(`/v1/students/${id}`),
  getStudentByAdmission: (admissionNumber: string) =>
    request<Student>(`/v1/students/admission/${admissionNumber}`),
  createStudent: (data: {
    institutionId: string
    userId: string
    firstName: string
    middleName?: string
    lastName: string
    email: string
    phone?: string
    dateOfBirth?: string
    gender?: string
    address?: string
    city?: string
    region?: string
    guardianName?: string
    guardianPhone?: string
    guardianRelationship?: string
  }) =>
    request<Student>("/v1/students", { method: "POST", body: JSON.stringify(data) }),
  updateStudent: (id: string, data: Partial<{
    firstName: string
    middleName: string
    lastName: string
    phone: string
    dateOfBirth: string
    gender: string
    address: string
    city: string
    region: string
    guardianName: string
    guardianPhone: string
    guardianRelationship: string
    status: string
  }>) =>
    request<Student>(`/v1/students/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  assignClass: (id: string, data: {
    classGroupId: string
    academicYearId: string
    termId: string
  }) =>
    request<StudentClassAssignment>(`/v1/students/${id}/assign-class`, { method: "POST", body: JSON.stringify(data) }),
  countStudents: (institutionId: string) =>
    request<number>(`/v1/students/stats/count?institutionId=${institutionId}`),
  countActiveStudents: (institutionId: string) =>
    request<number>(`/v1/students/stats/active?institutionId=${institutionId}`),
}

// ---------------------------------------------------------------------------
// Certificate API
// ---------------------------------------------------------------------------

export interface TemplateResponse {
  id: string
  institutionId: string
  name: string
  description: string | null
  templateType: string
  htmlContent: string | null
  cssContent: string | null
  logoUrl: string | null
  signatureLine1: string | null
  signatureLine2: string | null
  signatureLine3: string | null
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface CertificateResponse {
  id: string
  institutionId: string
  templateId: string
  studentId: string
  issuedBy: string | null
  serialNumber: string
  certificateType: string
  title: string
  courseTitle: string | null
  description: string | null
  studentName: string
  studentIdNumber: string | null
  courseOrProgramme: string | null
  instructorName: string | null
  grade: string | null
  skills: string[]
  completionDate: string
  issueDate: string | null
  expiryDate: string | null
  status: string
  verificationCode: string
  verificationUrl: string | null
  qrCodeUrl: string | null
  revokedReason: string | null
  revokedAt: string | null
  metadata: Record<string, unknown> | null
  createdAt: string
  updatedAt: string
}

export interface CertificateVerificationResponse {
  valid: boolean
  id: string | null
  serialNumber: string | null
  studentName: string | null
  certificateType: string | null
  title: string | null
  courseTitle: string | null
  instructorName: string | null
  grade: string | null
  skills: string[]
  completionDate: string | null
  institutionName: string | null
  issuedBy: string | null
  issueDate: string | null
  status: string | null
  message: string | null
}

export interface TranscriptEntryResponse {
  id: string
  subjectName: string
  subjectCode: string | null
  score: number | null
  grade: string | null
  remarks: string | null
}

export interface TranscriptResponse {
  id: string
  institutionId: string
  studentId: string
  issuedBy: string | null
  serialNumber: string
  academicYear: string | null
  term: string | null
  status: string
  totalSubjects: number | null
  averageScore: number | null
  classRank: number | null
  remarks: string | null
  generatedAt: string
  issuedAt: string | null
  entries: TranscriptEntryResponse[]
  createdAt: string
}

export interface CreateTemplateRequest {
  name: string
  description?: string
  templateType: string
  htmlContent?: string
  cssContent?: string
  logoUrl?: string
  signatureLine1?: string
  signatureLine2?: string
  signatureLine3?: string
}

export interface GenerateCertificateRequest {
  templateId: string
  studentId: string
  certificateType: string
  title: string
  description?: string
  studentName: string
  studentIdNumber?: string
  courseOrProgramme?: string
  instructorName?: string
  grade?: string
  skills?: string[]
  completionDate: string
  expiryDate?: string
}

export interface GenerateTranscriptRequest {
  studentId: string
  academicYear?: string
  term?: string
  entries?: { subjectName: string; subjectCode?: string; score?: number; grade?: string; remarks?: string }[]
  remarks?: string
}

export const certificateApi = {
  listTemplates: () =>
    request<TemplateResponse[]>("/v1/certificates/templates"),

  getTemplate: (templateId: string) =>
    request<TemplateResponse>(`/v1/certificates/templates/${templateId}`),

  createTemplate: (data: CreateTemplateRequest) =>
    request<TemplateResponse>("/v1/certificates/templates", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  generate: (data: GenerateCertificateRequest) =>
    request<CertificateResponse>("/v1/certificates/generate", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  issue: (certificateId: string) =>
    request<CertificateResponse>(`/v1/certificates/${certificateId}/issue`, {
      method: "POST",
    }),

  revoke: (certificateId: string, reason: string) =>
    request<CertificateResponse>(`/v1/certificates/${certificateId}/revoke`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  verify: (verificationCode: string) =>
    request<CertificateVerificationResponse>(`/v1/certificates/verify/${verificationCode}`),

  get: (certificateId: string) =>
    request<CertificateResponse>(`/v1/certificates/${certificateId}`),

  list: (studentId?: string) => {
    const params = studentId ? `?studentId=${studentId}` : ""
    return request<CertificateResponse[]>(`/v1/certificates${params}`)
  },

  generateTranscript: (data: GenerateTranscriptRequest) =>
    request<TranscriptResponse>("/v1/certificates/transcripts/generate", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  issueTranscript: (transcriptId: string) =>
    request<TranscriptResponse>(`/v1/certificates/transcripts/${transcriptId}/issue`, {
      method: "POST",
    }),

  listTranscripts: (studentId: string) =>
    request<TranscriptResponse[]>(`/v1/certificates/transcripts?studentId=${studentId}`),
}

// ---------------------------------------------------------------------------
// Administration API
// ---------------------------------------------------------------------------

export interface DashboardResponse {
  institutionId: string
  totalStudents: number
  totalTeachers: number
  totalParents: number
  activeStudents: number
  certificatesIssued: number
  pendingImportJobs: number
  totalCourses: number
  publishedCourses: number
  draftCourses: number
  totalModules: number
  totalLessons: number
  liveClassesScheduled: number
  additionalStats: Record<string, unknown>
}

export interface Course {
  id: string
  institutionId: string
  subjectId: string | null
  subjectName: string | null
  title: string
  description: string | null
  thumbnailUrl: string | null
  level: string
  category: string | null
  isPublished: boolean
  isFeatured: boolean
  createdByName: string | null
  moduleCount: number
  lessonCount: number
  createdAt: string
  updatedAt: string
}

export interface CourseModule {
  id: string
  courseId: string
  title: string
  description: string | null
  sortOrder: number
  lessonCount: number
  createdAt: string
}

export interface CourseLesson {
  id: string
  moduleId: string
  title: string
  contentType: string
  contentUrl: string | null
  durationMinutes: number | null
  sortOrder: number
  isFree: boolean
  createdAt: string
}

export interface CourseStats {
  totalCourses: number
  publishedCourses: number
  draftCourses: number
  featuredCourses: number
  totalModules: number
  totalLessons: number
  liveClassesScheduled: number
  liveClassesCompleted: number
  coursesByLevel: Record<string, number>
}

export interface SettingResponse {
  id: string
  institutionId: string
  settingKey: string
  settingValue: Record<string, unknown>
  settingType: string | null
  description: string | null
  isPublic: boolean
  createdAt: string
  updatedAt: string
}

export interface RoleResponse {
  id: string
  institutionId: string
  name: string
  displayName: string
  description: string | null
  isSystemRole: boolean
  isActive: boolean
  permissions: string[]
  createdAt: string
}

export interface ImportJobResponse {
  id: string
  institutionId: string
  importedBy: string
  importType: string
  fileName: string
  status: string
  totalRows: number | null
  processedRows: number | null
  successfulRows: number | null
  failedRows: number | null
  errorLog: Record<string, unknown> | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

export interface CreateRoleRequest {
  name: string
  displayName: string
  description?: string
  permissions?: string[]
}

export interface SettingRequest {
  settingKey: string
  settingValue: Record<string, unknown>
  settingType?: string
  description?: string
  isPublic?: boolean
}

export const adminApi = {
  getDashboard: (institutionId: string) =>
    request<DashboardResponse>(`/v1/admin/dashboard?institutionId=${institutionId}`),

  listSettings: (institutionId: string) =>
    request<SettingResponse[]>(`/v1/admin/settings?institutionId=${institutionId}`),

  getSetting: (institutionId: string, key: string) =>
    request<SettingResponse>(`/v1/admin/settings/${key}?institutionId=${institutionId}`),

  updateSetting: (institutionId: string, data: SettingRequest) =>
    request<SettingResponse>(`/v1/admin/settings?institutionId=${institutionId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  createRole: (institutionId: string, data: CreateRoleRequest) =>
    request<RoleResponse>(`/v1/admin/roles?institutionId=${institutionId}`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  listRoles: (institutionId: string) =>
    request<RoleResponse[]>(`/v1/admin/roles?institutionId=${institutionId}`),

  getRole: (institutionId: string, roleId: string) =>
    request<RoleResponse>(`/v1/admin/roles/${roleId}?institutionId=${institutionId}`),

  deleteRole: (institutionId: string, roleId: string) =>
    request<void>(`/v1/admin/roles/${roleId}?institutionId=${institutionId}`, {
      method: "DELETE",
    }),

  triggerImport: (institutionId: string, importType: string, fileName: string) =>
    request<ImportJobResponse>(`/v1/admin/users/import?institutionId=${institutionId}&importType=${importType}&fileName=${fileName}`, {
      method: "POST",
    }),

  listImportJobs: (institutionId: string) =>
    request<ImportJobResponse[]>(`/v1/admin/users/import?institutionId=${institutionId}`),

  getImportJob: (institutionId: string, jobId: string) =>
    request<ImportJobResponse>(`/v1/admin/users/import/${jobId}?institutionId=${institutionId}`),
}

// ---------------------------------------------------------------------------
// Course API
// ---------------------------------------------------------------------------

export const courseApi = {
  listCourses: (institutionId: string) =>
    request<Course[]>(`/v1/courses`),

  getCourse: (courseId: string) =>
    request<Course>(`/v1/courses/${courseId}`),

  createCourse: (data: { title: string; description?: string; subjectId?: string; level?: string; category?: string; thumbnailUrl?: string; isPublished?: boolean; isFeatured?: boolean }) =>
    request<Course>(`/v1/courses`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  updateCourse: (courseId: string, data: { title?: string; description?: string; subjectId?: string; level?: string; category?: string; thumbnailUrl?: string; isPublished?: boolean; isFeatured?: boolean }) =>
    request<Course>(`/v1/courses/${courseId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  deleteCourse: (courseId: string) =>
    request<void>(`/v1/courses/${courseId}`, {
      method: "DELETE",
    }),

  togglePublish: (courseId: string) =>
    request<Course>(`/v1/courses/${courseId}/toggle-publish`, {
      method: "POST",
    }),

  getStats: () =>
    request<CourseStats>(`/v1/courses/stats`),

  listModules: (courseId: string) =>
    request<CourseModule[]>(`/v1/courses/${courseId}/modules`),

  createModule: (courseId: string, data: { title: string; description?: string; sortOrder?: number }) =>
    request<CourseModule>(`/v1/courses/${courseId}/modules`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  deleteModule: (moduleId: string) =>
    request<void>(`/v1/courses/modules/${moduleId}`, {
      method: "DELETE",
    }),

  listLessons: (moduleId: string) =>
    request<CourseLesson[]>(`/v1/courses/modules/${moduleId}/lessons`),

  createLesson: (moduleId: string, data: { title: string; contentType: string; contentUrl?: string; durationMinutes?: number; sortOrder?: number; isFree?: boolean }) =>
    request<CourseLesson>(`/v1/courses/modules/${moduleId}/lessons`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  deleteLesson: (lessonId: string) =>
    request<void>(`/v1/courses/lessons/${lessonId}`, {
      method: "DELETE",
    }),
}

// ---------------------------------------------------------------------------
// Audit API
// ---------------------------------------------------------------------------

export interface AuditLogResponse {
  id: string
  institutionId: string
  userId: string
  userEmail: string | null
  userRole: string | null
  entityType: string
  entityId: string
  entityName: string | null
  action: string
  oldValues: Record<string, unknown> | null
  newValues: Record<string, unknown> | null
  ipAddress: string | null
  userAgent: string | null
  requestMethod: string | null
  requestUrl: string | null
  responseStatus: number | null
  durationMs: number | null
  createdAt: string
}

export interface ActivityFeedResponse {
  id: string
  institutionId: string
  userId: string
  actorName: string
  action: string
  description: string
  entityType: string
  entityId: string | null
  entityName: string | null
  metadata: Record<string, unknown> | null
  visibility: string
  createdAt: string
}

export interface SecurityEventResponse {
  id: string
  institutionId: string
  userId: string
  userEmail: string | null
  eventType: string
  description: string
  ipAddress: string | null
  userAgent: string | null
  location: string | null
  severity: string
  metadata: Record<string, unknown> | null
  resolved: boolean
  resolvedAt: string | null
  resolvedBy: string | null
  createdAt: string
}

export interface ComplianceReportResponse {
  totalAuditLogs: number
  totalSecurityEvents: number
  unresolvedSecurityEvents: number
  criticalEvents: number
  failedLoginAttempts: number
  topEventTypes: { eventType: string; count: number }[]
  severityBreakdown: { severity: string; count: number }[]
}

export const auditApi = {
  listLogs: (institutionId: string, from?: string, to?: string, page = 0, size = 20) => {
    const params = new URLSearchParams({ institutionId, page: String(page), size: String(size) })
    if (from) params.set("from", from)
    if (to) params.set("to", to)
    return request<AuditLogResponse[]>(`/v1/audit/logs?${params}`)
  },

  listLogsByUser: (userId: string, page = 0, size = 20) =>
    request<AuditLogResponse[]>(`/v1/audit/logs/user/${userId}?page=${page}&size=${size}`),

  listLogsByEntity: (institutionId: string, entityType: string, entityId: string) =>
    request<AuditLogResponse[]>(`/v1/audit/logs/entity/${entityType}/${entityId}?institutionId=${institutionId}`),

  listActivity: (institutionId: string, page = 0, size = 20) =>
    request<ActivityFeedResponse[]>(`/v1/audit/activity?institutionId=${institutionId}&page=${page}&size=${size}`),

  listActivityByUser: (userId: string, page = 0, size = 20) =>
    request<ActivityFeedResponse[]>(`/v1/audit/activity/user/${userId}?page=${page}&size=${size}`),

  listSecurityEvents: (institutionId: string, page = 0, size = 20) =>
    request<SecurityEventResponse[]>(`/v1/audit/security?institutionId=${institutionId}&page=${page}&size=${size}`),

  listUnresolvedSecurityEvents: (institutionId: string) =>
    request<SecurityEventResponse[]>(`/v1/audit/security/unresolved?institutionId=${institutionId}`),

  resolveSecurityEvent: (eventId: string) =>
    request<SecurityEventResponse>(`/v1/audit/security/${eventId}/resolve`, {
      method: "POST",
    }),

  getComplianceReport: (institutionId: string) =>
    request<ComplianceReportResponse>(`/v1/audit/compliance?institutionId=${institutionId}`),
}

export interface Institution {
  id: string
  name: string
  description: string | null
  type: string
  status: string | null
  logoUrl: string | null
  website: string | null
  email: string | null
  phone: string | null
  address: string | null
  city: string | null
  country: string | null
  createdAt: string
}

export interface CreateInstitutionRequest {
  name: string
  description?: string
  type: string
  logoUrl?: string
  website?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  country?: string
}

export interface UpdateInstitutionRequest {
  name?: string
  description?: string
  logoUrl?: string
  website?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  country?: string
}

export const institutionApi = {
  get: (id: string) =>
    request<Institution>(`/v1/institutions/${id}`),

  list: (page = 0, size = 20) =>
    request<{ content: Institution[]; totalElements: number; totalPages: number }>(
      `/v1/institutions?page=${page}&size=${size}`
    ),

  create: (data: CreateInstitutionRequest) =>
    request<Institution>("/v1/institutions", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (id: string, data: UpdateInstitutionRequest) =>
    request<Institution>(`/v1/institutions/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  delete: (id: string) =>
    request<void>(`/v1/institutions/${id}`, { method: "DELETE" }),

  activate: (id: string) =>
    request<Institution>(`/v1/institutions/${id}/activate`, { method: "PUT" }),

  deactivate: (id: string) =>
    request<Institution>(`/v1/institutions/${id}/deactivate`, { method: "PUT" }),
}

export interface TeacherProfile {
  id: string
  userId: string
  fullName: string
  email: string
  phone: string
  employeeNumber: string
  status: string
  specialization: string
  hireDate: string
  bio: string
  createdAt: string
}

export interface TeacherClassGroup {
  classGroupId: string
  className: string
  classSection: string
  subjectId: string
  subjectName: string
  academicYear: string
  enrolledStudents: number
  totalAssignments: number
  totalLessons: number
}

export interface TeacherStudent {
  studentId: string
  fullName: string
  email: string
  admissionNumber: string
  className: string
  subjectName: string
  gender: string
  status: string
  classGroupId: string
}

export interface TeacherDashboard {
  totalStudents: number
  totalClasses: number
  totalAssignments: number
  totalAssessments: number
  pendingSubmissions: number
  pendingGrading: number
  classes: Array<{
    classGroupId: string
    className: string
    subjectName: string
    enrolledStudents: number
  }>
  recentActivity: Array<{
    type: string
    title: string
    description: string
    timestamp: string
  }>
}

export interface LiveClass {
  id: string
  subjectId: string
  teacherId: string
  title: string
  description: string
  scheduledAt: string
  durationMinutes: number
  status: string
  meetingUrl: string
  maxParticipants: number
  recordingUrl: string
  createdAt: string
}

export const teacherApi = {
  getProfile: () =>
    request<TeacherProfile>("/v1/teachers/me/profile"),

  getClasses: () =>
    request<TeacherClassGroup[]>("/v1/teachers/me/classes"),

  getStudents: () =>
    request<TeacherStudent[]>("/v1/teachers/me/students"),

  getDashboard: () =>
    request<TeacherDashboard>("/v1/teachers/me/dashboard"),
}

export interface CreateLessonRequest {
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  contentText?: string
  videoUrl?: string
  fileAttachments?: string
  sortOrder?: number
  isPublished?: boolean
}

export const lessonApi = {
  create: (data: CreateLessonRequest) =>
    request<Lesson>("/v1/learning/lessons", {
      method: "POST",
      body: JSON.stringify(data),
    }),
}

export interface CreateAssessmentRequest {
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  timeLimitMinutes?: number
  totalMarks: number
  passMarks: number
  isPublished?: boolean
  startsAt?: string
  endsAt?: string
}

export const assessmentCreateApi = {
  create: (data: CreateAssessmentRequest) =>
    request<Assessment>("/v1/assessments", {
      method: "POST",
      body: JSON.stringify(data),
    }),
}

export interface CreateAssignmentRequest {
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  dueDate?: string
  totalMarks: number
  attachments?: string
}

export const assignmentCreateApi = {
  create: (data: CreateAssignmentRequest) =>
    request<Assignment>("/v1/learning/assignments", {
      method: "POST",
      body: JSON.stringify(data),
    }),
}

export const liveClassApi = {
  getByTeacher: (teacherId: string) =>
    request<LiveClass[]>(`/v1/teachers/${teacherId}/live-classes`),
}

export const attendanceApi = {
  getByClassAndDate: (classGroupId: string, date: string) =>
    request<unknown[]>(`/v1/attendance?classGroupId=${classGroupId}&date=${date}`),

  mark: (data: { studentId: string; classGroupId: string; attendanceDate: string; status: string; remarks?: string }) =>
    request<unknown>("/v1/attendance/mark", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  bulkMark: (data: { classGroupId: string; attendanceDate: string; records: Array<{ studentId: string; status: string; remarks?: string }> }) =>
    request<unknown>("/v1/attendance/bulk", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  getSummary: (studentId: string) =>
    request<unknown>(`/v1/attendance/summary/student/${studentId}`),
}

export const gradingApi = {
  getScales: () =>
    request<unknown[]>("/v1/grading/scales"),

  getReportCard: (id: string) =>
    request<unknown>(`/v1/grading/report-cards/${id}`),

  getStudentReportCards: (studentId: string) =>
    request<unknown[]>(`/v1/grading/report-cards/student/${studentId}`),
}

// ── Student Dashboard API ────────────────────────────────────────────────────

export interface DashboardSummary {
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
  completionPercentage: number
  startedAt: string
  completedAt: string | null
}

export interface RecentActivity {
  type: string
  lessonId?: string
  completedAt?: string
  date?: string
  status?: string
}

export interface SubjectGradeResult {
  subjectId: string
  marksObtained: number
  grade: string
  gradePoints: number
  teacherRemarks: string
}

export interface StudentResult {
  id: string
  academicYearId: string
  termId: string
  totalMarks: number
  averageMark: number
  classRank: number
  remarks: string
  overallGrade: string
  isPublished: boolean
  subjectGrades: SubjectGradeResult[]
}

export interface AttendanceRecordItem {
  id: string
  date: string
  status: string
  checkInTime: string | null
  checkOutTime: string | null
  remarks: string | null
}

export interface AttendanceSummary {
  totalDays: number
  present: number
  absent: number
  late: number
  excused: number
  attendanceRate: number
  records: AttendanceRecordItem[]
}

export const dashboardApi = {
  getSummary: () =>
    request<DashboardSummary>("/v1/student/dashboard/summary"),

  getContinueLearning: () =>
    request<ContinueLearningItem[]>("/v1/student/dashboard/continue-learning"),

  getRecentActivity: () =>
    request<RecentActivity[]>("/v1/student/dashboard/recent-activity"),

  getResults: () =>
    request<StudentResult[]>("/v1/student/dashboard/results"),

  getAttendance: () =>
    request<AttendanceSummary>("/v1/student/dashboard/attendance"),

  getLiveClasses: () =>
    request<unknown[]>("/v1/student/dashboard/live-classes"),
}
