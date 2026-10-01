const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

function getToken(): string | null {
  if (typeof document === "undefined") return null
  const match = document.cookie.match(/elmkusoma_access_token=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

function getInstitutionId(): string | null {
  if (typeof document === "undefined") return null
  const match = document.cookie.match(/elmkusoma_institution_id=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

async function regionalFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken()
  const institutionId = getInstitutionId()
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init?.headers as Record<string, string> || {}),
  }
  if (token) headers["Authorization"] = `Bearer ${token}`
  if (institutionId) headers["X-Institution-Id"] = institutionId

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (!res.ok) {
    let detail = ""
    try {
      const body = await res.json()
      detail = body?.message || body?.error || ""
    } catch {
      // non-JSON error body
    }
    throw new Error(detail ? `API ${res.status}: ${detail}` : `API ${res.status}: ${res.statusText}`)
  }
  const json = await res.json()
  return json.data ?? json
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

export interface JurisdictionSummary { type: string; name: string; code: string }

export interface QuickAction {
  id: string
  label: string
  description: string
  icon: string
  route: string
  category: string
}

export interface AttentionItem {
  severity: string
  title: string
  description: string
  category: string
  actionUrl: string
}

export interface OversightAlert {
  id: string
  type: string
  message: string
  severity: string
  institutionName: string
  institutionId: string
  timestamp: string
}

export interface DistrictSummary {
  id: string
  name: string
  code: string
  isActive: boolean
  institutionCount: number
  schoolCount: number
  teacherCount: number
  learnerCount: number
  attendanceRate: number | null
  averagePerformance: number | null
}

export interface RegionalDashboard {
  jurisdictionSummary: JurisdictionSummary
  totalInstitutions: number
  totalSchools: number
  totalDistricts: number
  totalTeachers: number
  totalLearners: number
  totalClasses: number
  totalLessons: number
  totalCourses: number
  activeLiveClasses: number
  attendanceRate: number | null
  averagePerformance: number | null
  curriculumProgress: number | null
  alertsCount: number
  pendingVerifications: number
  dataQualityIssues: number
  topDistricts: DistrictSummary[]
  recentAlerts: OversightAlert[]
  quickActions: QuickAction[]
  lastUpdated: string
}

export interface RegionalPulse {
  liveNow: number
  scheduledToday: number
  completedToday: number
  totalThisWeek: number
  teachers: number
  learners: number
  totalLessons: number
  publishedLessons: number
  pendingVerifications: number
  alertsCount: number
  lastUpdated: string
}

export interface RegionInfo {
  id: string
  name: string
  code: string
  isActive: boolean
  institutionCount: number | null
  teacherCount: number | null
  studentCount: number | null
}

export interface RegionDetail {
  id: string
  name: string
  code: string
  isActive: boolean
  districtCount: number
  institutionCount: number
  schoolCount: number
  teacherCount: number
  learnerCount: number
  attendanceRate: number | null
  averagePerformance: number | null
  curriculumProgress: number | null
  districts: DistrictSummary[]
}

export interface DistrictInfo {
  id: string
  name: string
  code: string
  regionId: string
  regionName: string
  isActive: boolean
  institutionCount: number | null
  teacherCount: number | null
  studentCount: number | null
}

export interface DistrictDetail {
  id: string
  name: string
  code: string
  isActive: boolean
  regionId: string
  regionName: string
  regionCode: string
  institutionCount: number
  schoolCount: number
  teacherCount: number
  learnerCount: number
  attendanceRate: number | null
  averagePerformance: number | null
  curriculumProgress: number | null
  institutions: OversightInstitution[]
}

export interface OversightInstitution {
  id: string
  name: string
  code: string
  type: string
  districtName: string
  regionName: string
  teacherCount: number | null
  studentCount: number | null
  isActive: boolean
}

export interface RegionalInstitution {
  id: string
  name: string
  code: string
  type: string
  districtName: string
  regionName: string
  teacherCount: number | null
  studentCount: number | null
  isActive: boolean
}

export interface InstitutionGovernance {
  id: string
  name: string
  code: string
  type: string
  isActive: boolean
  status: string | null
  districtId: string | null
  districtName: string | null
  regionId: string | null
  regionName: string | null
  teacherCount: number
  learnerCount: number
  classCount: number
  lessonCount: number
  liveClassCount: number
  attendanceRate: number | null
  averagePerformance: number | null
  curriculumProgress: number | null
  verificationStatus: string
  dataQualityIssues: string[]
  contactEmail: string | null
  contactPhone: string | null
  address: string | null
  city: string | null
}

export interface PersonSummary {
  id: string
  fullName: string
  email: string
  institutionId: string | null
  institutionName: string | null
  role: string
  isActive: boolean
  createdAt: string
}

export interface CourseSummary {
  id: string
  title: string
  level: string | null
  category: string | null
  subject: string | null
  institutionId: string | null
  institutionName: string | null
  isPublished: boolean
  createdAt: string
}

export interface LiveClassSummary {
  id: string
  title: string
  status: string
  scheduledAt: string | null
  durationMinutes: number | null
  maxParticipants: number | null
  subjectId: string | null
  subjectName: string | null
  teacherId: string | null
  teacherName: string | null
  institutionId: string | null
  institutionName: string | null
}

export interface ObserverJoin {
  websocketUrl: string
  token: string
  liveClassId: string
  title: string
  institutionName: string | null
  subjectName: string | null
  teacherName: string | null
}

export interface ResourceSummary {
  id: string
  title: string
  type: string | null
  mimeType: string | null
  fileSize: number | null
  visibility: string | null
  institutionId: string | null
  institutionName: string | null
  createdAt: string
}

export interface VideoTutorialSummary {
  id: string
  title: string
  status: string
  durationSeconds: number | null
  recordingUrl: string | null
  institutionId: string | null
  institutionName: string | null
  createdAt: string
}

export interface SchoolPerformance {
  institutionId: string
  institutionName: string
  institutionCode: string
  averageScore: number | null
  passRate: number | null
  studentCount: number | null
  assessmentCount: number | null
}

export interface SubjectPerformance {
  subjectName: string
  subjectCode: string
  averageScore: number | null
  passRate: number | null
  assessmentCount: number | null
}

export interface PerformanceResponse {
  overallAverage: number | null
  passRate: number | null
  totalAssessments: number | null
  totalReportCards: number | null
  schoolPerformance: SchoolPerformance[]
  subjectPerformance: SubjectPerformance[]
}

export interface DailyTrend { date: string; present: number | null; absent: number | null; late: number | null; excused: number | null }
export interface SchoolAttendance { institutionId: string; institutionName: string; institutionCode: string; studentCount: number | null; attendanceRate: number | null; absentCount: number | null; lateCount: number | null }
export interface LowAttendanceStudent { studentId: string; studentName: string; institutionName: string; attendanceRate: number | null; daysAbsent: number | null }

export interface AttendanceResponse {
  overallRate: number | null
  totalStudents: number | null
  schoolsAtRisk: number | null
  dailyTrend: DailyTrend[]
  schoolAttendance: SchoolAttendance[]
  lowAttendanceStudents: LowAttendanceStudent[]
}

export interface SchoolAssessment { institutionId: string; institutionName: string; institutionCode: string; assessmentCount: number | null; completedCount: number | null; averageScore: number | null; passRate: number | null }
export interface RecentAssessment { id: string; title: string; institutionName: string; subjectName: string; scheduledDate: string; status: string; participantCount: number | null }

export interface AssessmentsResponse {
  totalAssessments: number | null
  completedAssessments: number | null
  averageScore: number | null
  passRate: number | null
  pendingGrading: number | null
  schoolAssessments: SchoolAssessment[]
  recentAssessments: RecentAssessment[]
}

export interface SubjectProgress { subjectName: string; subjectCode: string; totalLessons: number | null; completedLessons: number | null; progressRate: number | null }
export interface SchoolCurriculumProgress { institutionId: string; institutionName: string; institutionCode: string; totalLessons: number | null; completedLessons: number | null; progressRate: number | null; status: string }

export interface CurriculumResponse {
  overallProgress: number | null
  totalLessons: number | null
  completedLessons: number | null
  schoolsOnTrack: number | null
  schoolsBehind: number | null
  subjectProgress: SubjectProgress[]
  schoolProgress: SchoolCurriculumProgress[]
}

export interface ReportSummary {
  id: string
  title: string
  description: string
  type: string
  icon: string
  color: string
  available: boolean
}

export interface OversightAlertItem {
  id: string
  type: string
  title: string
  message: string
  severity: string
  institutionName: string
  institutionId: string
  indicator: string
  value: string
  threshold: string
  timestamp: string
  status: string
  acknowledgedBy: string | null
  acknowledgedAt: string | null
  resolvedBy: string | null
  resolvedAt: string | null
}

export interface AlertsResponse {
  alerts: OversightAlertItem[]
  summary: { total: number | null; high: number | null; medium: number | null; low: number | null; new_: number | null; acknowledged: number | null; resolved: number | null }
}

export interface VerificationSummary {
  id: string
  entityType: string
  entityId: string
  entityName: string
  verificationType: string
  status: string
  submittedBy: string
  submittedAt: string
  reviewedAt: string | null
}

export interface VerificationDetail {
  id: string
  entityType: string
  entityId: string
  entityName: string
  verificationType: string
  status: string
  submittedBy: string
  submittedAt: string
  documents: string[]
  reviewerNotes: string | null
  reviewedAt: string | null
  reviewedBy: string | null
}

export interface VerificationReviewPayload { status: string; reviewerNotes?: string }

export interface DataQualityIssue {
  id: string
  type: string
  severity: string
  title: string
  description: string
  entityType: string
  entityId: string | null
  entityName: string
  suggestedAction: string
  detectedAt: string
}

export interface DataQualityResponse {
  totalIssues: number
  criticalIssues: number
  warningIssues: number
  infoIssues: number
  issues: DataQualityIssue[]
}

export interface ComplianceCheck {
  id: string
  name: string
  category: string
  passed: boolean
  description: string
  remediation: string
  lastChecked: string
}

export interface ComplianceResponse {
  isCompliant: boolean
  totalChecks: number
  passedChecks: number
  failedChecks: number
  checks: ComplianceCheck[]
  note: string
}

export interface AuditLogSummary {
  id: string
  entityType: string
  entityId: string | null
  entityName: string | null
  action: string
  actorName: string | null
  actorRole: string | null
  institutionName: string | null
  timestamp: string
}

export interface AnnouncementSummary {
  id: string
  title: string
  summary: string
  priority: string
  audienceType: string
  recipientCount: number
  sentBy: string
  sentAt: string
  expiresAt: string | null
}

export interface AnnouncementDetail {
  id: string
  title: string
  content: string
  priority: string
  audienceType: string
  targetDistrictIds: string[]
  targetInstitutionIds: string[]
  recipientCount: number
  sentBy: string
  sentAt: string
  expiresAt: string | null
}

export interface CreateAnnouncementPayload {
  title: string
  content: string
  priority?: string
  audienceType: string
  targetDistrictIds?: string[]
  targetInstitutionIds?: string[]
  expiresAt?: string
}

export interface NotificationSummary {
  id: string
  title: string
  message: string
  notificationType: string
  targetType: string
  targetId: string | null
  isRead: boolean
  createdAt: string
}

export interface SearchResult {
  type: string
  id: string
  title: string
  subtitle: string | null
  route: string
  icon: string | null
}

function qs(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== "") search.set(k, String(v))
  }
  const s = search.toString()
  return s ? `?${s}` : ""
}

export const regionalAdminApi = {
  getDashboard: () => regionalFetch<RegionalDashboard>("/v1/regional-admin/dashboard"),
  getAttention: () => regionalFetch<AttentionItem[]>("/v1/regional-admin/attention"),
  getPulse: () => regionalFetch<RegionalPulse>("/v1/regional-admin/pulse"),
  getQuickActions: () => regionalFetch<QuickAction[]>("/v1/regional-admin/quick-actions"),

  getRegions: () => regionalFetch<RegionInfo[]>("/v1/regional-admin/regions"),
  getRegionDetail: (regionId: string) => regionalFetch<RegionDetail>(`/v1/regional-admin/regions/${regionId}`),
  getDistricts: (regionId: string) => regionalFetch<DistrictInfo[]>(`/v1/regional-admin/regions/${regionId}/districts`),
  getDistrict: (districtId: string) => regionalFetch<DistrictDetail>(`/v1/regional-admin/districts/${districtId}`),

  listInstitutions: (params: { page?: number; size?: number; search?: string; districtId?: string; type?: string } = {}) =>
    regionalFetch<PageResponse<RegionalInstitution>>(`/v1/regional-admin/institutions${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, districtId: params.districtId, type: params.type })}`),
  getInstitutionGovernance: (institutionId: string) =>
    regionalFetch<InstitutionGovernance>(`/v1/regional-admin/institutions/${institutionId}`),

  listLearners: (params: { page?: number; size?: number; search?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<PersonSummary>>(`/v1/regional-admin/learners${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, institutionId: params.institutionId, districtId: params.districtId })}`),
  listTeachers: (params: { page?: number; size?: number; search?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<PersonSummary>>(`/v1/regional-admin/teachers${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, institutionId: params.institutionId, districtId: params.districtId })}`),
  listEducationStaff: (params: { page?: number; size?: number; search?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<PersonSummary>>(`/v1/regional-admin/education-staff${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, institutionId: params.institutionId, districtId: params.districtId })}`),

  listCourses: (params: { page?: number; size?: number; search?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<CourseSummary>>(`/v1/regional-admin/courses${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, institutionId: params.institutionId, districtId: params.districtId })}`),
  listLiveClasses: (params: { page?: number; size?: number; status?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<LiveClassSummary>>(`/v1/regional-admin/live-classes${qs({ page: params.page ?? 0, size: params.size ?? 20, status: params.status, institutionId: params.institutionId, districtId: params.districtId })}`),
  observeLiveClass: (liveClassId: string) =>
    regionalFetch<ObserverJoin>(`/v1/regional-admin/live-classes/${liveClassId}/observe`),
  listResources: (params: { page?: number; size?: number; search?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<ResourceSummary>>(`/v1/regional-admin/resources${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, institutionId: params.institutionId, districtId: params.districtId })}`),
  listVideoTutorials: (params: { page?: number; size?: number; search?: string; institutionId?: string; districtId?: string } = {}) =>
    regionalFetch<PageResponse<VideoTutorialSummary>>(`/v1/regional-admin/video-tutorials${qs({ page: params.page ?? 0, size: params.size ?? 20, search: params.search, institutionId: params.institutionId, districtId: params.districtId })}`),

  getPerformance: () => regionalFetch<PerformanceResponse>("/v1/regional-admin/performance"),
  getAttendance: () => regionalFetch<AttendanceResponse>("/v1/regional-admin/attendance"),
  getAssessments: () => regionalFetch<AssessmentsResponse>("/v1/regional-admin/assessments"),
  getCurriculum: () => regionalFetch<CurriculumResponse>("/v1/regional-admin/curriculum"),
  getReports: () => regionalFetch<ReportSummary[]>("/v1/regional-admin/reports"),
  getAlerts: () => regionalFetch<AlertsResponse>("/v1/regional-admin/alerts"),

  listVerifications: (params: { page?: number; size?: number; status?: string } = {}) =>
    regionalFetch<PageResponse<VerificationSummary>>(`/v1/regional-admin/verifications${qs({ page: params.page ?? 0, size: params.size ?? 20, status: params.status })}`),
  getVerification: (verificationId: string) =>
    regionalFetch<VerificationDetail>(`/v1/regional-admin/verifications/${verificationId}`),
  reviewVerification: (verificationId: string, payload: VerificationReviewPayload) =>
    regionalFetch<VerificationDetail>(`/v1/regional-admin/verifications/${verificationId}/review`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  getDataQuality: () => regionalFetch<DataQualityResponse>("/v1/regional-admin/data-quality"),
  getCompliance: () => regionalFetch<ComplianceResponse>("/v1/regional-admin/compliance"),
  getAuditLogs: (params: { page?: number; size?: number; entityType?: string } = {}) =>
    regionalFetch<PageResponse<AuditLogSummary>>(`/v1/regional-admin/audit${qs({ page: params.page ?? 0, size: params.size ?? 20, entityType: params.entityType })}`),

  listAnnouncements: (params: { page?: number; size?: number } = {}) =>
    regionalFetch<PageResponse<AnnouncementSummary>>(`/v1/regional-admin/announcements${qs({ page: params.page ?? 0, size: params.size ?? 20 })}`),
  createAnnouncement: (payload: CreateAnnouncementPayload) =>
    regionalFetch<AnnouncementDetail>("/v1/regional-admin/announcements", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  listNotifications: (params: { page?: number; size?: number } = {}) =>
    regionalFetch<PageResponse<NotificationSummary>>(`/v1/regional-admin/notifications${qs({ page: params.page ?? 0, size: params.size ?? 20 })}`),
  getUnreadNotificationCount: () =>
    regionalFetch<{ count: number }>("/v1/regional-admin/notifications/unread-count"),
  markNotificationRead: (notificationId: string) =>
    regionalFetch<void>(`/v1/regional-admin/notifications/${notificationId}/read`, { method: "PUT" }),

  search: (q: string, limit = 20) =>
    regionalFetch<SearchResult[]>(`/v1/regional-admin/search${qs({ q, limit })}`),
}
