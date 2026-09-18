function getStoredUser(): { institutionId?: string } | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem("elmkusoma_current_user")
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export async function appFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const user = getStoredUser()
  const institutionId = user?.institutionId || "00000000-0000-0000-0000-000000000001"
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
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}
