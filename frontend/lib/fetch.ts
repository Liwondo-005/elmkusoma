function getStoredUser(): { institutionId?: string } | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem("elmkusoma_current_user")
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * The thrown Error carries the HTTP status so callers can distinguish cases that share a
 * message shape - most importantly a 404 ("this article does not exist", a real state to render)
 * from a 500 ("something broke", an error to report). Before this was added the status was lost
 * whenever the body had an `error` field, because `body.error` replaced it entirely.
 */
export interface ApiError extends Error {
  status: number
}

export function isApiError(error: unknown, status: number): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    (error as { status?: unknown }).status === status
  )
}

export async function appFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const user = getStoredUser()
  const institutionId = user?.institutionId || "a0000000-0000-0000-0000-000000000001"
  const res = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw Object.assign(
      new Error(body.error || body.message || `Request failed: ${res.status}`),
      { status: res.status },
    )
  }
  const json = await res.json()
  return json.data ?? json
}
