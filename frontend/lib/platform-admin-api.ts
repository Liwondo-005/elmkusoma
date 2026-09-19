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

  getAuditLogs: (page = 0, size = 20) =>
    platformFetch<AuditLogEntry[]>(`/v1/platform-admin/audit/logs?page=${page}&size=${size}`),

  search: (q: string, type?: string) => {
    const params = new URLSearchParams({ q, limit: "20" })
    if (type) params.set("type", type)
    return platformFetch<GlobalSearchResult[]>(`/v1/platform-admin/search?${params}`)
  },
}
