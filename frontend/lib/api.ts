const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api"

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
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) || {}),
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`
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

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface UserInfo {
  id: string
  email: string
  firstName: string
  lastName: string
  role: string
  emailVerified: boolean
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

export interface ForgotPasswordPayload {
  email: string
}

export interface ResetPasswordPayload {
  token: string
  newPassword: string
}

export interface VerifyEmailPayload {
  token: string
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

  refresh: (refreshToken: string) =>
    request<AuthResponse>("/v1/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),

  forgotPassword: (data: ForgotPasswordPayload) =>
    request<void>("/v1/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  resetPassword: (data: ResetPasswordPayload) =>
    request<void>("/v1/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  verifyEmail: (data: VerifyEmailPayload) =>
    request<void>("/v1/auth/verify-email", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  logout: (refreshToken: string) =>
    request<void>("/v1/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),
}

export interface InstitutionResponse {
  id: string
  name: string
  description: string | null
  type: string
  status: string
  logoUrl: string | null
  website: string | null
  email: string | null
  phone: string | null
  address: string | null
  city: string | null
  country: string | null
  createdAt: string
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

export const institutionApi = {
  list: (page = 0, size = 20) =>
    request<PageResponse<InstitutionResponse>>(`/v1/institutions?page=${page}&size=${size}`),

  get: (id: string) =>
    request<InstitutionResponse>(`/v1/institutions/${id}`),

  create: (data: { name: string; type: string; description?: string; logoUrl?: string; website?: string; email?: string; phone?: string; address?: string; city?: string; country?: string }) =>
    request<InstitutionResponse>("/v1/institutions", {
      method: "POST",
      body: JSON.stringify(data),
    }),
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
  additionalStats: Record<string, unknown>
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
