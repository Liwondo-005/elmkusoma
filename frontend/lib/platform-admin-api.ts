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

async function platformFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken()
  const institutionId = getInstitutionId()
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init?.headers as Record<string, string> || {}),
  }
  if (token) headers["Authorization"] = `Bearer ${token}`
  if (institutionId) headers["X-Institution-Id"] = institutionId

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (!res.ok) throw new Error(`API ${res.status}: ${res.statusText}`)
  const json = await res.json()
  return json.data ?? json
}

export interface PlatformDashboard {
  totalUsers: number
  totalStudents: number
  totalTeachers: number
  totalParents: number
  totalInstitutions: number
  totalLiveClasses: number
  activeLiveClasses: number
  totalPayments: number
  totalCertificates: number
  unresolvedSecurityEvents: number
}

export interface AttentionItem {
  severity: string
  title: string
  description: string
  category: string
  actionUrl: string
}

export interface ActivityFeed {
  id: string
  institutionId: string
  userId: string
  actorName: string
  action: string
  description: string
  entityType: string
  entityId: string
  entityName: string
  createdAt: string
}

export interface PlatformHealth {
  databaseStatus: string
  apiStatus: string
  totalUsers: number
  activeUsers: number
  totalInstitutions: number
  activeInstitutions: number
  livekitStatus?: string
  storageStatus?: string
  backgroundJobsStatus?: string
  notificationsStatus?: string
  paymentsStatus?: string
  realtimeStatus?: string
  mediaStatus?: string
  heartbeatAt?: string | null
}

export interface UserSummary {
  id: string
  email: string
  firstName: string
  lastName: string
  role: string
  isActive: boolean
  institutionId: string | null
  createdAt: string
}

export interface InstitutionSummary {
  id: string
  name: string
  code: string
  type: string
  city: string
  region: string
  isActive: boolean
  status?: string
  createdAt: string
}

export interface InstitutionDetail extends InstitutionSummary {
  address: string
  country: string
  phone: string
  email: string
  totalUsers: number
  totalStudents: number
  totalTeachers: number
}

export interface InstitutionFormPayload {
  name: string
  type: string
  description?: string
  logoUrl?: string
  website?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  country?: string
}

export const INSTITUTION_TYPES = [
  "NATIONAL",
  "NURSERY",
  "PRIMARY",
  "SECONDARY",
  "SCHOOL",
  "COLLEGE",
  "VOCATIONAL",
  "UNIVERSITY",
  "TRAINING_PROVIDER",
  "PROFESSIONAL_BODY",
  "COMPANY",
  "NGO",
  "GOVERNMENT",
  "CONTENT_PROVIDER",
  "EVENT_PROVIDER",
  "COMMUNITY_SCHOOL",
  "ADULT_EDUCATION",
] as const

export interface OrgMember {
  userId: string
  fullName: string | null
  email: string | null
  phone: string | null
  userRole: string | null
  membershipRole: string | null
  isActive: boolean | null
  userActive: boolean | null
  joinedAt: string | null
}

export const ORG_MEMBER_ROLES = [
  "OWNER",
  "INSTITUTION_ADMIN",
  "ADMIN",
  "NATIONAL_ADMIN",
  "TEACHER",
  "INSTRUCTOR",
  "STUDENT",
  "PARENT",
  "OTHER_LEARNER",
] as const

export interface LiveClassSummary {
  id: string
  title: string
  status: string
  scheduledAt: string
  durationMinutes: number
  maxParticipants: number
  currentParticipants: number
  createdAt: string
}

export interface PaymentSummary {
  id: string
  parentId: string
  studentId: string
  amount: number
  currency: string
  status: string
  serviceType: string
  createdAt: string
}

export interface CertificateSummary {
  id: string
  studentId: string
  serialNumber: string
  certificateNumber: string | null
  title: string
  studentName: string | null
  certificateType: string | null
  courseOrProgramme: string | null
  institutionId: string | null
  institutionName: string | null
  issueDate: string
  expiryDate: string | null
  status: string
  createdAt: string | null
}

export interface CertificateOverview {
  total: number
  issued: number
  revoked: number
  draft: number
  byType: Record<string, number>
  verificationActivity30Days: number
  generatedAt: string
}

export interface Signatory {
  id: string
  institutionId: string | null
  institutionName: string | null
  fullName: string
  positionTitle: string | null
  organization: string | null
  signatureImage: string | null
  certificateTypes: string[] | null
  status: string
  validFrom: string | null
  validUntil: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface SignatoryInput {
  fullName: string
  positionTitle?: string | null
  organization?: string | null
  signatureImage?: string | null
  certificateTypes?: string[] | null
  status?: string
  validFrom?: string | null
  validUntil?: string | null
  institutionId?: string | null
}

export interface PlatformTemplate {
  id: string
  institutionId: string
  institutionName: string | null
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
  version: number
  usageCount: number
  createdAt: string | null
  updatedAt: string | null
  signatories: Signatory[]
}

export interface TemplateInput {
  name: string
  description?: string | null
  templateType: string
  htmlContent?: string | null
  cssContent?: string | null
  logoUrl?: string | null
  signatureLine1?: string | null
  signatureLine2?: string | null
  signatureLine3?: string | null
  institutionId?: string
}

export interface TemplateVersion {
  id: string
  templateId: string
  version: number
  name: string
  templateType: string
  description: string | null
  htmlContent: string | null
  cssContent: string | null
  logoUrl: string | null
  archivedAt: string | null
  archivedBy: string | null
}

export interface CertificateDetail {
  certificate: import("./api").CertificateResponse
  institutionName: string | null
  templateId: string | null
  templateName: string | null
  templateVersion: number | null
  templateIsActive: boolean | null
  signatories: Signatory[]
}

export interface SecurityEventItem {
  id: string
  userId: string
  eventType: string
  severity: string
  description: string
  ipAddress: string
  resolved: boolean
  createdAt: string
}

export interface AuditLogEntry {
  id: string
  userId: string
  performedBy: string
  userRole: string
  action: string
  entityType: string
  entityId: string
  entityName: string
  oldValues: Record<string, unknown>
  newValues: Record<string, unknown>
  ipAddress: string
  createdAt: string
}

export interface GlobalSearchResult {
  type: string
  id: string
  title: string
  subtitle: string
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

export interface ServiceSummary {
  id: string; name: string; code: string; description: string; category: string
  isActive: boolean; requiresVerification: boolean; maxSeats: number | null
  monthlyPrice: number | null; currency: string; createdAt: string
}

export interface IncidentSummary {
  id: string; title: string; description: string; category: string
  severity: string; status: string; affectedService: string | null
  assignedTo: string | null; detectedAt: string; resolvedAt: string | null; createdAt: string
}

export interface PlatformConfigItem {
  id: string; configKey: string; configValue: string; configType: string
  description: string; category: string; isSensitive: boolean; isPublic: boolean
  lastModifiedBy: string | null; updatedAt: string | null
}

export interface NotificationSummary {
  id: string; title: string; message: string; notificationType: string
  priority: string; targetAudience: string | null; targetRole: string | null
  sentBy: string | null; sentAt: string; readCount: number
}

export interface DelegationSummary {
  id: string; delegatorId: string; delegateId: string; permissions: string
  scope: string; status: string; startsAt: string; expiresAt: string | null; createdAt: string
}

export interface VerificationSummary {
  id: string; entityType: string; entityId: string; verificationType: string
  status: string; submittedBy: string | null; reviewedBy: string | null
  submittedAt: string; reviewedAt: string | null; createdAt: string
}

export interface EntitlementSummary {
  id: string; userId: string; studentId: string; serviceType: string
  serviceId: string; status: string; startsAt: string; expiresAt: string | null; createdAt: string
}

export interface EnhancedDashboard extends PlatformDashboard {
  openIncidents: number; pendingVerifications: number; activeServices: number
  totalNotifications: number; activeDelegations: number
}

export interface ProviderQuota {
  id: string; providerId: string; serviceId: string
  serviceName: string | null; serviceCode: string | null
  status: string; seatsUsed: number | null; maxSeats: number | null
  expiresAt: string | null; createdAt: string
}

export interface SupportTicket {
  id: string; userId: string; title: string; description: string
  category: string; priority: string; status: string
  assignedTo: string | null; resolvedAt: string | null; createdAt: string
}

export interface ContentReport {
  id: string; entityType: string; entityId: string; entityTitle: string | null
  reporterId: string | null; reason: string; description: string | null
  status: string; resolutionNotes: string | null; resolvedBy: string | null
  resolvedAt: string | null; createdAt: string
}

export interface IntegrationStatus {
  key: string; name: string; category: string | null
  connectionStatus: string; lastSuccessAt: string | null; failureCount: number
  webhookStatus: string | null; retryStatus: string | null; configStatus: string
  diagnostics: string | null; probeDetail: string | null
}

export interface WebhookEvent {
  id: string; source: string; eventType: string | null
  verificationStatus: string; processingResult: string
  errorDetails: string | null; retryCount: number
  receivedAt: string; processedAt: string | null
}

export interface BackupStatus {
  status: string | null; lastRunAt: string | null; lastFile: string | null
  lastFileSizeBytes: number | null; integrityOk: boolean | null
  backupCount: number | null; newestBackupAt: string | null; oldestBackupAt: string | null
  backupDirectory: string; statusFilePath: string
  scriptPresent: boolean; restoreScriptPresent: boolean
  restoreProcedure: string; recoveryEvents: string[] | null
}

export interface PolicyFlag {
  key: string; value: string; description: string | null; category: string | null
}

export interface RetentionStatus {
  config: Record<string, string>
  lastSweep: string | null
  archivedAuditCount: number | null
  totalAuditCount: number | null
  purgedSoftDeletedReports: number | null
}

export interface DataQualityCheck {
  name: string; status: string; count: number | null; detail: string
}

export interface AdminAccount {
  userId: string; email: string; fullName: string; role: string
  assignedRoleName: string | null; permissions: string[]; scope: string | null
  isActive: boolean | null; createdAt: string | null; createdBy: string | null
  lastModifiedAt: string | null; expiresAt: string | null; recentActionCount: number | null
}

export interface OffboardingStep {
  step: string; status: string; detail: string
}

export interface OffboardingChecklist {
  institutionId: string; institutionName: string; lifecycleStatus: string
  steps: OffboardingStep[]
}

export interface FeatureStatus {
  key: string; name: string; status: string; description: string | null; updatedAt: string | null
}

export interface CommunicationDelivery {
  platformNotifications: number | null
  learnerNotifications: number | null
  learnerRead: number | null
  learnerUnread: number | null
  learnerReadRate: number | null
  platformReadCountSum: number | null
  deliveryNote: string | null
}

export interface AnalyticsSnapshot {
  id: string; snapshotType: string; generatedAt: string | null; data: Record<string, unknown> | null
}

export const platformAdminApi = {
  getDashboard: () => platformFetch<PlatformDashboard>("/v1/platform-admin/dashboard"),
  getAttention: () => platformFetch<AttentionItem[]>("/v1/platform-admin/attention"),
  getActivity: (page = 0, size = 20) => platformFetch<ActivityFeed[]>(`/v1/platform-admin/activity?page=${page}&size=${size}`),
  getHealth: () => platformFetch<PlatformHealth>("/v1/platform-admin/health"),

  listUsers: (page = 0, size = 20, role?: string, search?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (role) params.set("role", role)
    if (search) params.set("search", search)
    return platformFetch<PageResponse<UserSummary>>(`/v1/platform-admin/users?${params}`)
  },
  getUser: (userId: string) => platformFetch<UserSummary>(`/v1/platform-admin/users/${userId}`),
  updateUserStatus: (userId: string, active: boolean) =>
    platformFetch<UserSummary>(`/v1/platform-admin/users/${userId}/status?active=${active}`, { method: "PUT" }),

  listInstitutions: (page = 0, size = 20, search?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (search) params.set("search", search)
    return platformFetch<PageResponse<InstitutionSummary>>(`/v1/platform-admin/institutions?${params}`)
  },
  getInstitution: (id: string) => platformFetch<InstitutionDetail>(`/v1/platform-admin/institutions/${id}`),
  updateInstitutionStatus: (id: string, active: boolean) =>
    platformFetch<InstitutionSummary>(`/v1/platform-admin/institutions/${id}/status?active=${active}`, { method: "PUT" }),
  updateInstitutionLifecycle: (id: string, status: string) =>
    platformFetch<InstitutionSummary>(`/v1/platform-admin/institutions/${id}/lifecycle`, { method: "PUT", body: JSON.stringify({ status }) }),
  createInstitution: (payload: InstitutionFormPayload) =>
    platformFetch<InstitutionDetail>(`/v1/platform-admin/institutions`, { method: "POST", body: JSON.stringify(payload) }),
  updateInstitution: (id: string, payload: InstitutionFormPayload) =>
    platformFetch<InstitutionDetail>(`/v1/platform-admin/institutions/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteInstitution: (id: string) =>
    platformFetch<null>(`/v1/platform-admin/institutions/${id}`, { method: "DELETE" }),
  listOrgMembers: (institutionId: string) =>
    platformFetch<OrgMember[]>(`/v1/platform-admin/institutions/${institutionId}/members`),
  updateOrgMemberRole: (institutionId: string, userId: string, role: string) =>
    platformFetch<OrgMember>(
      `/v1/platform-admin/institutions/${institutionId}/members/${userId}/role?role=${encodeURIComponent(role)}`,
      { method: "PUT" }
    ),

  getProviderQuotas: (providerId: string) =>
    platformFetch<ProviderQuota[]>(`/v1/platform-admin/providers/${providerId}/quotas`),
  updateEntitlement: (entitlementId: string, data: { maxSeats?: number; status?: string }) =>
    platformFetch<ProviderQuota>(`/v1/platform-admin/entitlements/${entitlementId}`, { method: "PUT", body: JSON.stringify(data) }),
  grantSponsorSeats: (data: { providerId: string; serviceId: string; userId: string; studentId: string; seats?: number; expiresAt?: string; note?: string }) =>
    platformFetch<{ seatsGranted: number; seatsUsed: number; maxSeats: number | null; entitlementIds: string[] }>(
      "/v1/platform-admin/commerce/sponsor-seats", { method: "POST", body: JSON.stringify(data) }),
  revokeSponsoredEntitlement: (entitlementId: string) =>
    platformFetch<string>(`/v1/platform-admin/commerce/entitlements/${entitlementId}/revoke`, { method: "PUT" }),

  listLiveClasses: (page = 0, size = 20, status?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) params.set("status", status)
    return platformFetch<PageResponse<LiveClassSummary>>(`/v1/platform-admin/live-classes?${params}`)
  },

  listPayments: (page = 0, size = 20, status?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) params.set("status", status)
    return platformFetch<PageResponse<PaymentSummary>>(`/v1/platform-admin/payments?${params}`)
  },

  listCertificates: (page = 0, size = 20, filters?: {
    search?: string; status?: string; type?: string; institutionId?: string; from?: string; to?: string
  }) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters?.search) params.set("search", filters.search)
    if (filters?.status) params.set("status", filters.status)
    if (filters?.type) params.set("type", filters.type)
    if (filters?.institutionId) params.set("institutionId", filters.institutionId)
    if (filters?.from) params.set("from", filters.from)
    if (filters?.to) params.set("to", filters.to)
    return platformFetch<PageResponse<CertificateSummary>>(`/v1/platform-admin/certificates?${params}`)
  },

  getCertificateOverview: () =>
    platformFetch<CertificateOverview>("/v1/platform-admin/certificates/overview"),

  getCertificateDetail: (certificateId: string) =>
    platformFetch<CertificateDetail>(`/v1/platform-admin/certificates/${certificateId}`),

  listCertificateTemplates: (page = 0, size = 50, filters?: { search?: string; type?: string; institutionId?: string }) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters?.search) params.set("search", filters.search)
    if (filters?.type) params.set("type", filters.type)
    if (filters?.institutionId) params.set("institutionId", filters.institutionId)
    return platformFetch<PageResponse<PlatformTemplate>>(`/v1/platform-admin/certificate-templates?${params}`)
  },

  createCertificateTemplate: (body: TemplateInput) =>
    platformFetch<PlatformTemplate>("/v1/platform-admin/certificate-templates", {
      method: "POST", body: JSON.stringify(body) }),

  updateCertificateTemplate: (templateId: string, body: TemplateInput) =>
    platformFetch<PlatformTemplate>(`/v1/platform-admin/certificate-templates/${templateId}`, {
      method: "PUT", body: JSON.stringify(body) }),

  setCertificateTemplateStatus: (templateId: string, isActive: boolean) =>
    platformFetch<PlatformTemplate>(`/v1/platform-admin/certificate-templates/${templateId}/status`, {
      method: "PUT", body: JSON.stringify({ isActive }) }),

  getTemplateSignatories: (templateId: string) =>
    platformFetch<Signatory[]>(`/v1/platform-admin/certificate-templates/${templateId}/signatories`),

  replaceTemplateSignatories: (templateId: string, signatoryIds: string[]) =>
    platformFetch<Signatory[]>(`/v1/platform-admin/certificate-templates/${templateId}/signatories`, {
      method: "PUT", body: JSON.stringify({ signatoryIds }) }),

  getTemplateVersions: (templateId: string) =>
    platformFetch<TemplateVersion[]>(`/v1/platform-admin/certificate-templates/${templateId}/versions`),

  listCertificateSignatories: (page = 0, size = 50, filters?: { search?: string; status?: string; institutionId?: string }) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters?.search) params.set("search", filters.search)
    if (filters?.status) params.set("status", filters.status)
    if (filters?.institutionId) params.set("institutionId", filters.institutionId)
    return platformFetch<PageResponse<Signatory>>(`/v1/platform-admin/certificate-signatories?${params}`)
  },

  createCertificateSignatory: (body: SignatoryInput) =>
    platformFetch<Signatory>("/v1/platform-admin/certificate-signatories", {
      method: "POST", body: JSON.stringify(body) }),

  updateCertificateSignatory: (signatoryId: string, body: SignatoryInput) =>
    platformFetch<Signatory>(`/v1/platform-admin/certificate-signatories/${signatoryId}`, {
      method: "PUT", body: JSON.stringify(body) }),

  deleteCertificateSignatory: (signatoryId: string) =>
    platformFetch<{ id: string; deleted: boolean; linksRemoved: number }>(
      `/v1/platform-admin/certificate-signatories/${signatoryId}`, { method: "DELETE" }),

  getSecurityEvents: () => platformFetch<SecurityEventItem[]>("/v1/platform-admin/security/events"),
  resolveSecurityEvent: (eventId: string) =>
    platformFetch<SecurityEventItem>(`/v1/platform-admin/security/events/${eventId}/resolve`, { method: "POST" }),

  getAuditLogs: (page = 0, size = 20, action?: string, entityType?: string, entityId?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (action) params.set("action", action)
    if (entityType) params.set("entityType", entityType)
    if (entityId) params.set("entityId", entityId)
    return platformFetch<AuditLogEntry[]>(`/v1/platform-admin/audit/logs?${params}`)
  },

  search: (q: string, type?: string) => {
    const params = new URLSearchParams({ q, limit: "20" })
    if (type) params.set("type", type)
    return platformFetch<GlobalSearchResult[]>(`/v1/platform-admin/search?${params}`)
  },

  getEnhancedDashboard: () => platformFetch<EnhancedDashboard>("/v1/platform-admin/dashboard/enhanced"),

  listServices: (page = 0, size = 50, category?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (category) params.set("category", category)
    return platformFetch<PageResponse<ServiceSummary>>(`/v1/platform-admin/services?${params}`)
  },
  createService: (data: { name: string; code: string; description?: string; category: string }) =>
    platformFetch<ServiceSummary>("/v1/platform-admin/services", { method: "POST", body: JSON.stringify(data) }),
  updateService: (id: string, data: Record<string, unknown>) =>
    platformFetch<ServiceSummary>(`/v1/platform-admin/services/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  listIncidents: (page = 0, size = 20, status?: string, severity?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) params.set("status", status)
    if (severity) params.set("severity", severity)
    return platformFetch<PageResponse<IncidentSummary>>(`/v1/platform-admin/incidents?${params}`)
  },
  createIncident: (data: { title: string; description?: string; category: string; severity?: string }) =>
    platformFetch<IncidentSummary>("/v1/platform-admin/incidents", { method: "POST", body: JSON.stringify(data) }),
  updateIncidentStatus: (id: string, status: string, notes?: string) => {
    const params = new URLSearchParams({ status })
    if (notes) params.set("notes", notes)
    return platformFetch<IncidentSummary>(`/v1/platform-admin/incidents/${id}/status?${params}`, { method: "PUT" })
  },

  listConfig: (category?: string) => {
    const params = category ? `?category=${category}` : ""
    return platformFetch<PlatformConfigItem[]>(`/v1/platform-admin/config${params}`)
  },
  updateConfig: (key: string, value: string) =>
    platformFetch<PlatformConfigItem>(`/v1/platform-admin/config/${key}`, { method: "PUT", body: JSON.stringify({ value }) }),

  listNotifications: (page = 0, size = 20) =>
    platformFetch<PageResponse<NotificationSummary>>(`/v1/platform-admin/notifications?page=${page}&size=${size}`),
  sendNotification: (data: { title: string; message: string; notificationType: string; priority?: string; targetAudience?: string }) =>
    platformFetch<NotificationSummary>("/v1/platform-admin/notifications", { method: "POST", body: JSON.stringify(data) }),

  listDelegations: () => platformFetch<DelegationSummary[]>("/v1/platform-admin/delegations"),
  createDelegation: (data: { delegatorId: string; delegateId: string; permissions: string; scope?: string }) =>
    platformFetch<DelegationSummary>("/v1/platform-admin/delegations", { method: "POST", body: JSON.stringify(data) }),
  revokeDelegation: (id: string, reason?: string) => {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : ""
    return platformFetch<string>(`/v1/platform-admin/delegations/${id}/revoke${params}`, { method: "PUT" })
  },

  listPendingVerifications: () => platformFetch<VerificationSummary[]>("/v1/platform-admin/verifications/pending"),
  reviewVerification: (id: string, status: string, notes?: string) => {
    const params = new URLSearchParams({ status })
    if (notes) params.set("notes", notes)
    return platformFetch<VerificationSummary>(`/v1/platform-admin/verifications/${id}/review?${params}`, { method: "PUT" })
  },

  listEntitlements: (page = 0, size = 20, status?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) params.set("status", status)
    return platformFetch<PageResponse<EntitlementSummary>>(`/v1/platform-admin/entitlements?${params}`)
  },

  listPlatformCourses: (page = 0, size = 20, search?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (search) params.set("search", search)
    return platformFetch<PageResponse<any>>(`/v1/platform-admin/courses?${params}`)
  },
  listPlatformEvents: (page = 0, size = 20, search?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (search) params.set("search", search)
    return platformFetch<PageResponse<any>>(`/v1/platform-admin/events?${params}`)
  },
  listPlatformMedia: (page = 0, size = 20) => platformFetch<PageResponse<any>>(`/v1/platform-admin/media?page=${page}&size=${size}`),
  listPlatformResources: (page = 0, size = 20) => platformFetch<PageResponse<any>>(`/v1/platform-admin/resources?page=${page}&size=${size}`),

  listSupportTickets: (page = 0, size = 20, status?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) params.set("status", status)
    return platformFetch<PageResponse<SupportTicket>>(`/v1/platform-admin/support/tickets?${params}`)
  },
  updateSupportTicketStatus: (id: string, status: string) =>
    platformFetch<SupportTicket>(`/v1/platform-admin/support/tickets/${id}/status?status=${status}`, { method: "PUT" }),

  listContentReports: (page = 0, size = 20, status?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) params.set("status", status)
    return platformFetch<PageResponse<ContentReport>>(`/v1/platform-admin/moderation/reports?${params}`)
  },
  createContentReport: (data: { entityType: string; entityId: string; entityTitle?: string; reporterId?: string; reason: string; description?: string }) =>
    platformFetch<ContentReport>("/v1/platform-admin/moderation/reports", { method: "POST", body: JSON.stringify(data) }),
  actOnContentReport: (id: string, action: string, notes?: string) =>
    platformFetch<ContentReport>(`/v1/platform-admin/moderation/reports/${id}/action`, { method: "PUT", body: JSON.stringify({ action, notes }) }),

  exportPlatformData: async (type: "users" | "institutions" | "audit" = "users"): Promise<void> => {
    const token = getToken()
    const headers: Record<string, string> = {}
    if (token) headers["Authorization"] = `Bearer ${token}`
    const res = await fetch(`${API_BASE}/v1/platform-admin/export?type=${type}`, { headers })
    if (!res.ok) throw new Error(`API ${res.status}: ${res.statusText}`)
    const blob = await res.blob()
    const a = document.createElement("a")
    a.href = URL.createObjectURL(blob)
    a.download = `platform_${type}_export.csv`
    a.click()
    URL.revokeObjectURL(a.href)
  },

  // ── BATCH 13 ──

  listIntegrations: () => platformFetch<IntegrationStatus[]>("/v1/platform-admin/integrations"),
  probeIntegration: (key: string) =>
    platformFetch<IntegrationStatus>(`/v1/platform-admin/integrations/${key}/probe`, { method: "POST" }),
  listWebhookEvents: (source?: string, failedOnly = false, page = 0, size = 20) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (source) params.set("source", source)
    if (failedOnly) params.set("failedOnly", "true")
    return platformFetch<PageResponse<WebhookEvent>>(`/v1/platform-admin/webhook-events?${params}`)
  },
  getBackupStatus: () => platformFetch<BackupStatus>("/v1/platform-admin/backup/status"),

  listPolicies: () => platformFetch<PolicyFlag[]>("/v1/platform-admin/policies"),
  updatePolicy: (key: string, value: boolean) =>
    platformFetch<PolicyFlag>(`/v1/platform-admin/policies/${key}`, {
      method: "PUT", body: JSON.stringify({ value: String(value) }),
    }),

  getRetentionStatus: () => platformFetch<RetentionStatus>("/v1/platform-admin/data-governance/retention"),
  runRetentionSweep: () =>
    platformFetch<RetentionStatus>("/v1/platform-admin/data-governance/retention/run", { method: "POST" }),
  getDataQuality: () => platformFetch<DataQualityCheck[]>("/v1/platform-admin/data-governance/quality"),

  assignSupportTicket: (ticketId: string, assigneeId: string | null) =>
    platformFetch<SupportTicket>(`/v1/platform-admin/support/tickets/${ticketId}/assign`, {
      method: "PUT", body: JSON.stringify({ assigneeId }),
    }),

  listAdmins: () => platformFetch<AdminAccount[]>("/v1/platform-admin/admins"),
  getRolePermissions: (roleId: string) => platformFetch<string[]>(`/v1/platform-admin/roles/${roleId}/permissions`),
  updateRolePermissions: (roleId: string, permissions: string[]) =>
    platformFetch<string[]>(`/v1/platform-admin/roles/${roleId}/permissions`, {
      method: "PUT", body: JSON.stringify({ permissions }),
    }),

  getOffboardingChecklist: (institutionId: string) =>
    platformFetch<OffboardingChecklist>(`/v1/platform-admin/institutions/${institutionId}/offboarding`),

  bulkContentAction: (type: "COURSE" | "EVENT" | "RESOURCE" | "MEDIA", action: "PUBLISH" | "UNPUBLISH" | "ARCHIVE" | "RESTORE", ids: string[]) =>
    platformFetch<{ affected: number; requested: number; failures: string[] }>(
      "/v1/platform-admin/content/bulk-action", { method: "POST", body: JSON.stringify({ type, action, ids }) }),

  getObserverJoinUrl: (liveClassId: string) =>
    platformFetch<{ websocketUrl: string; token: string; liveClassId: string; title: string; institutionName: string; subjectName: string; teacherName: string }>(
      `/v1/oversight/live-classes/${liveClassId}/observe`),

  listFeatures: () => platformFetch<FeatureStatus[]>("/v1/platform-admin/features"),
  updateFeatureStatus: (key: string, status: string) =>
    platformFetch<FeatureStatus>(`/v1/platform-admin/features/${key}/status`, {
      method: "PUT", body: JSON.stringify({ status }),
    }),

  getCommunicationDelivery: () => platformFetch<CommunicationDelivery>("/v1/platform-admin/communications/delivery"),

  listAnalyticsSnapshots: (limit = 20) =>
    platformFetch<AnalyticsSnapshot[]>(`/v1/platform-admin/analytics/snapshots?limit=${limit}`),
  createAnalyticsSnapshot: () =>
    platformFetch<AnalyticsSnapshot>("/v1/platform-admin/analytics/snapshots", { method: "POST" }),

  revokeCertificatePlatform: (certificateId: string, reason?: string) =>
    platformFetch<{ id: string; status: string; serialNumber: string; reason: string | null }>(
      `/v1/platform-admin/certificates/${certificateId}/revoke`,
      { method: "POST", body: JSON.stringify({ reason }) }),
}
