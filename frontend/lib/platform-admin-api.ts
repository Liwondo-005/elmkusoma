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
  title: string
  issueDate: string
  status: string
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

  listCertificates: (page = 0, size = 20) =>
    platformFetch<PageResponse<CertificateSummary>>(`/v1/platform-admin/certificates?page=${page}&size=${size}`),

  getSecurityEvents: () => platformFetch<SecurityEventItem[]>("/v1/platform-admin/security/events"),
  resolveSecurityEvent: (eventId: string) =>
    platformFetch<SecurityEventItem>(`/v1/platform-admin/security/events/${eventId}/resolve`, { method: "POST" }),

  getAuditLogs: (page = 0, size = 20, action?: string, entityType?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (action) params.set("action", action)
    if (entityType) params.set("entityType", entityType)
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
}
