const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

export interface AiHealth {
  configured: boolean
  provider: string | null
  model: string | null
  reason: string | null
}

export class AiApiError extends Error {
  status: number
  code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = "AiApiError"
    this.status = status
    this.code = code
  }
}

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_access_token")
}

function getInstitutionId(): string | null {
  if (typeof window === "undefined") return null
  const stored = localStorage.getItem("elmkusoma_institution_id")
  if (stored) return stored
  try {
    const raw = localStorage.getItem("elmkusoma_current_user")
    if (raw) {
      const user = JSON.parse(raw)
      if (user?.institutionId) return user.institutionId
    }
  } catch {}
  return null
}

async function aiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
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

  let body: Record<string, unknown>
  try {
    body = await res.json()
  } catch {
    throw new AiApiError(`Server returned non-JSON response (${res.status})`, res.status)
  }

  if (!res.ok || body.success === false) {
    const message = String(body.message || body.error || `Request failed (${res.status})`)
    const code = typeof body.error === "string" ? body.error : undefined
    throw new AiApiError(message, res.status, code)
  }

  return (body.data !== undefined ? body.data : body) as T
}

export function isAiUnavailable(error: unknown): boolean {
  return error instanceof AiApiError && error.status === 503
}

export const aiApi = {
  getAiHealth: (): Promise<AiHealth> => aiRequest<AiHealth>("/v1/ai/health"),

  askAi: async (question: string): Promise<{ answer: string }> =>
    aiRequest<{ answer: string }>("/v1/ai/assistant/ask", {
      method: "POST",
      body: JSON.stringify({ question }),
    }),
}
