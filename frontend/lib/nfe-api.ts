"use client"

async function nfeRequest<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("elmkusoma_access_token")
  const institutionId = localStorage.getItem("elmkusoma_institution_id") || ""
  const res = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(institutionId ? { "X-Institution-Id": institutionId } : {}),
      ...options.headers,
    },
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Request failed" }))
    throw new Error(error.message || `HTTP ${res.status}`)
  }
  const data = await res.json()
  const payload = data.data ?? data
  // Backend list endpoints are paged (PageResponse { content, totalElements, ... });
  // the workspace pages consume plain arrays, so unwrap the content array here
  // instead of every call site.
  if (
    payload &&
    typeof payload === "object" &&
    !Array.isArray(payload) &&
    Array.isArray((payload as { content?: unknown }).content) &&
    typeof (payload as { totalElements?: unknown }).totalElements === "number"
  ) {
    return (payload as { content: T }).content
  }
  return payload as T
}

/**
 * Assessments, attendance and certificates are provider-scoped on the backend
 * (/providers/{providerId} path segments). The workspace belongs to a single
 * institution, so resolve the institution's first NFE provider once and reuse
 * it; institutions without a provider row get empty lists (and creates fail
 * with a clear message) rather than 404s.
 */
let providerIdCache: string | null | undefined
async function resolveProviderId(): Promise<string | null> {
  if (providerIdCache !== undefined) return providerIdCache
  try {
    const providers = await nfeRequest<unknown[]>("/v1/nfe/providers")
    providerIdCache =
      Array.isArray(providers) && providers.length > 0 && (providers[0] as { id?: string }).id
        ? ((providers[0] as { id: string }).id ?? null)
        : null
  } catch {
    providerIdCache = null
  }
  return providerIdCache
}
async function requireProviderId(): Promise<string> {
  const id = await resolveProviderId()
  if (!id) {
    throw new Error("No education provider exists for this institution yet")
  }
  return id
}

export const nfeApi = {
  listProviders: () => nfeRequest<any[]>("/v1/nfe/providers"),
  getProvider: (id: string) => nfeRequest<any>(`/v1/nfe/providers/${id}`),
  createProvider: (data: any) => nfeRequest<any>("/v1/nfe/providers", { method: "POST", body: JSON.stringify(data) }),
  updateProvider: (id: string, data: any) => nfeRequest<any>(`/v1/nfe/providers/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteProvider: (id: string) => nfeRequest<void>(`/v1/nfe/providers/${id}`, { method: "DELETE" }),

  listPrograms: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/programs${providerId ? `?providerId=${providerId}` : ""}`),
  getProgram: (id: string) => nfeRequest<any>(`/v1/nfe/programs/${id}`),
  createProgram: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/programs", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },
  updateProgram: (id: string, data: any) => nfeRequest<any>(`/v1/nfe/programs/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteProgram: (id: string) => nfeRequest<void>(`/v1/nfe/programs/${id}`, { method: "DELETE" }),

  listLearners: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/learners${providerId ? `?providerId=${providerId}` : ""}`),
  getLearner: (id: string) => nfeRequest<any>(`/v1/nfe/learners/${id}`),
  createLearner: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/learners", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },

  listSessions: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/sessions${providerId ? `?providerId=${providerId}` : ""}`),
  createSession: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/sessions", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },

  listMaterials: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/materials${providerId ? `?providerId=${providerId}` : ""}`),
  createMaterial: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/materials", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },

  listAssessments: async () => {
    const providerId = await resolveProviderId()
    if (!providerId) return []
    return nfeRequest<any[]>(`/v1/nfe/assessments/providers/${providerId}`)
  },
  createAssessment: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>(`/v1/nfe/assessments/providers/${providerId}`, { method: "POST", body: JSON.stringify(data) })
  },

  listAttendance: async (sessionId?: string) => {
    if (sessionId) return nfeRequest<any[]>(`/v1/nfe/attendance/sessions/${sessionId}`)
    const providerId = await resolveProviderId()
    if (!providerId) return []
    return nfeRequest<any[]>(`/v1/nfe/attendance/providers/${providerId}`)
  },
  markAttendance: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>(`/v1/nfe/attendance/providers/${providerId}`, { method: "POST", body: JSON.stringify(data) })
  },

  listCertificates: async () => {
    const providerId = await resolveProviderId()
    if (!providerId) return []
    return nfeRequest<any[]>(`/v1/nfe/certificates/providers/${providerId}`)
  },
  generateCertificate: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>(`/v1/nfe/certificates/providers/${providerId}`, { method: "POST", body: JSON.stringify(data) })
  },
  verifyCertificate: (code: string) => nfeRequest<any>(`/v1/nfe/certificates/verify/${code}`),
}
