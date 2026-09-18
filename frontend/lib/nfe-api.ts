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
  return data.data ?? data
}

export const nfeApi = {
  listProviders: () => nfeRequest<any[]>("/v1/nfe/providers"),
  getProvider: (id: string) => nfeRequest<any>(`/v1/nfe/providers/${id}`),
  createProvider: (data: any) => nfeRequest<any>("/v1/nfe/providers", { method: "POST", body: JSON.stringify(data) }),
  updateProvider: (id: string, data: any) => nfeRequest<any>(`/v1/nfe/providers/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteProvider: (id: string) => nfeRequest<void>(`/v1/nfe/providers/${id}`, { method: "DELETE" }),

  listPrograms: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/programs${providerId ? `?providerId=${providerId}` : ""}`),
  getProgram: (id: string) => nfeRequest<any>(`/v1/nfe/programs/${id}`),
  createProgram: (data: any) => nfeRequest<any>("/v1/nfe/programs", { method: "POST", body: JSON.stringify(data) }),
  updateProgram: (id: string, data: any) => nfeRequest<any>(`/v1/nfe/programs/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteProgram: (id: string) => nfeRequest<void>(`/v1/nfe/programs/${id}`, { method: "DELETE" }),

  listLearners: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/learners${providerId ? `?providerId=${providerId}` : ""}`),
  getLearner: (id: string) => nfeRequest<any>(`/v1/nfe/learners/${id}`),
  createLearner: (data: any) => nfeRequest<any>("/v1/nfe/learners", { method: "POST", body: JSON.stringify(data) }),

  listSessions: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/sessions${providerId ? `?providerId=${providerId}` : ""}`),
  createSession: (data: any) => nfeRequest<any>("/v1/nfe/sessions", { method: "POST", body: JSON.stringify(data) }),

  listMaterials: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/materials${providerId ? `?providerId=${providerId}` : ""}`),
  createMaterial: (data: any) => nfeRequest<any>("/v1/nfe/materials", { method: "POST", body: JSON.stringify(data) }),

  listAssessments: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/assessments${providerId ? `?providerId=${providerId}` : ""}`),
  createAssessment: (data: any) => nfeRequest<any>("/v1/nfe/assessments", { method: "POST", body: JSON.stringify(data) }),

  listAttendance: (sessionId?: string) => nfeRequest<any[]>(`/v1/nfe/attendance${sessionId ? `?sessionId=${sessionId}` : ""}`),
  markAttendance: (data: any) => nfeRequest<any>("/v1/nfe/attendance", { method: "POST", body: JSON.stringify(data) }),

  listCertificates: (providerId?: string) => nfeRequest<any[]>(`/v1/nfe/certificates${providerId ? `?providerId=${providerId}` : ""}`),
  generateCertificate: (data: any) => nfeRequest<any>("/v1/nfe/certificates", { method: "POST", body: JSON.stringify(data) }),
  verifyCertificate: (code: string) => nfeRequest<any>(`/v1/nfe/certificates/verify/${code}`),
}
