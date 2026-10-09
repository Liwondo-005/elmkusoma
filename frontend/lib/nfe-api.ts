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
 * institution, so resolve that institution's NFE provider once and reuse it.
 *
 * Audit B-01: nothing used to create the nfe_education_providers row, so this
 * always resolved to null, every create failed with "No education provider exists
 * for this institution yet" and three lists were permanently empty. The backend
 * now exposes GET /v1/nfe/providers/me which provisions the provider from the
 * institution the platform already approved, so the workspace self-provisions.
 */
let providerIdCache: string | null | undefined
async function resolveProviderId(): Promise<string | null> {
  if (providerIdCache !== undefined) return providerIdCache
  try {
    const provider = await nfeRequest<{ id?: string } | null>("/v1/nfe/providers/me")
    providerIdCache = provider?.id ?? null
  } catch {
    // Fall back to the list endpoint for institutions provisioned before /me existed.
    try {
      const providers = await nfeRequest<unknown[]>("/v1/nfe/providers")
      providerIdCache =
        Array.isArray(providers) && providers.length > 0 && (providers[0] as { id?: string }).id
          ? ((providers[0] as { id: string }).id ?? null)
          : null
    } catch {
      providerIdCache = null
    }
  }
  return providerIdCache
}

/** Drops the memoised provider so a new provider switch takes effect immediately. */
export function resetNfeProviderCache() {
  providerIdCache = undefined
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

  /**
   * Audit B-32/B-33: these helpers appended `?providerId=`, which no backend list endpoint
   * declares â€” Spring ignores it, so callers received the whole institution-wide list while the
   * code read as if provider scoping were applied. They now call the routes that actually scope
   * (`/provider/{providerId}`) and resolve the active provider through the (now self-provisioning)
   * /providers/me endpoint instead of pinning `providers[0]`.
   */
  listPrograms: async (): Promise<any[]> => {
    const providerId = await resolveProviderId()
    if (!providerId) return []
    return nfeRequest<any[]>(`/v1/nfe/programs/provider/${providerId}`)
  },
  getProgram: (id: string) => nfeRequest<any>(`/v1/nfe/programs/${id}`),
  createProgram: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/programs", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },
  updateProgram: async (id: string, data: any) => {
    const payload = data?.providerId ? data : { ...data, providerId: await requireProviderId() }
    return nfeRequest<any>(`/v1/nfe/programs/${id}`, { method: "PUT", body: JSON.stringify(payload) })
  },
  deleteProgram: (id: string) => nfeRequest<void>(`/v1/nfe/programs/${id}`, { method: "DELETE" }),

  listLearners: async (): Promise<any[]> => {
    const pid = await resolveProviderId()
    return pid ? nfeRequest<any[]>(`/v1/nfe/learners/provider/${pid}`) : []
  },
  getLearner: (id: string) => nfeRequest<any>(`/v1/nfe/learners/${id}`),
  createLearner: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/learners", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },
  updateLearner: async (id: string, data: any) => {
    const payload = data?.providerId ? data : { ...data, providerId: await requireProviderId() }
    return nfeRequest<any>(`/v1/nfe/learners/${id}`, { method: "PUT", body: JSON.stringify(payload) })
  },
  deleteLearner: (id: string) => nfeRequest<void>(`/v1/nfe/learners/${id}`, { method: "DELETE" }),

  listSessions: async (): Promise<any[]> => {
    const pid = await resolveProviderId()
    return pid ? nfeRequest<any[]>(`/v1/nfe/sessions/provider/${pid}`) : []
  },
  createSession: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/sessions", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },
  updateSession: async (id: string, data: any) => {
    const payload = data?.providerId ? data : { ...data, providerId: await requireProviderId() }
    return nfeRequest<any>(`/v1/nfe/sessions/${id}`, { method: "PUT", body: JSON.stringify(payload) })
  },
  deleteSession: (id: string) => nfeRequest<void>(`/v1/nfe/sessions/${id}`, { method: "DELETE" }),

  listMaterials: async (): Promise<any[]> => {
    const pid = await resolveProviderId()
    return pid ? nfeRequest<any[]>(`/v1/nfe/materials/provider/${pid}`) : []
  },
  createMaterial: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>("/v1/nfe/materials", { method: "POST", body: JSON.stringify({ ...data, providerId }) })
  },
  updateMaterial: async (id: string, data: any) => {
    const payload = data?.providerId ? data : { ...data, providerId: await requireProviderId() }
    return nfeRequest<any>(`/v1/nfe/materials/${id}`, { method: "PUT", body: JSON.stringify(payload) })
  },
  deleteMaterial: (id: string) => nfeRequest<void>(`/v1/nfe/materials/${id}`, { method: "DELETE" }),

  listAssessments: async () => {
    const providerId = await resolveProviderId()
    if (!providerId) return []
    return nfeRequest<any[]>(`/v1/nfe/assessments/providers/${providerId}`)
  },
  createAssessment: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>(`/v1/nfe/assessments/providers/${providerId}`, { method: "POST", body: JSON.stringify(data) })
  },
  updateAssessment: (id: string, data: any) =>
    nfeRequest<any>(`/v1/nfe/assessments/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteAssessment: (id: string) => nfeRequest<void>(`/v1/nfe/assessments/${id}`, { method: "DELETE" }),

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
  updateAttendance: (id: string, data: any) =>
    nfeRequest<any>(`/v1/nfe/attendance/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteAttendance: (id: string) => nfeRequest<void>(`/v1/nfe/attendance/${id}`, { method: "DELETE" }),

  listCertificates: async () => {
    const providerId = await resolveProviderId()
    if (!providerId) return []
    return nfeRequest<any[]>(`/v1/nfe/certificates/providers/${providerId}`)
  },
  generateCertificate: async (data: any) => {
    const providerId = await requireProviderId()
    return nfeRequest<any>(`/v1/nfe/certificates/providers/${providerId}`, { method: "POST", body: JSON.stringify(data) })
  },
  updateCertificate: (id: string, data: any) =>
    nfeRequest<any>(`/v1/nfe/certificates/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteCertificate: (id: string) => nfeRequest<void>(`/v1/nfe/certificates/${id}`, { method: "DELETE" }),
  verifyCertificate: (code: string) => nfeRequest<any>(`/v1/nfe/certificates/verify/${code}`),
}
