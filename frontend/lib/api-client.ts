import type { ApiResponse } from "./types/api"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
const INSTITUTION_ID = process.env.NEXT_PUBLIC_INSTITUTION_ID || ""

interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>
}

function buildUrl(path: string, params?: Record<string, string | number | boolean | undefined>): string {
  const url = new URL(`${API_BASE}${path}`)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value))
      }
    })
  }
  return url.toString()
}

function getHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }

  if (typeof window !== "undefined") {
    const token = localStorage.getItem("elmkusoma_access_token")
    if (token) {
      headers["Authorization"] = `Bearer ${token}`
    }
    const institutionId = localStorage.getItem("elmkusoma_institution_id")
    if (institutionId) {
      headers["X-Institution-Id"] = institutionId
    }
  }

  return headers
}

export class ApiError extends Error {
  status: number
  error?: string
  data?: Record<string, string>

  constructor(status: number, message: string, error?: string, data?: Record<string, string>) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.error = error
    this.data = data
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const { params, ...fetchOptions } = options
  const url = buildUrl(path, params)

  const response = await fetch(url, {
    ...fetchOptions,
    headers: {
      ...getHeaders(),
      ...fetchOptions.headers,
    },
  })

  const body = await response.json() as ApiResponse<T>

  if (!response.ok || !body.success) {
    throw new ApiError(
      response.status,
      body.error || body.message || `Request failed with status ${response.status}`,
      body.error,
      body.data as Record<string, string> | undefined,
    )
  }

  return body
}

export const api = {
  get: <T>(path: string, params?: Record<string, string | number | boolean>) =>
    request<T>(path, { method: "GET", params }),

  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "POST", body: data ? JSON.stringify(data) : undefined }),

  put: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "PUT", body: data ? JSON.stringify(data) : undefined }),

  delete: <T>(path: string) =>
    request<T>(path, { method: "DELETE" }),
}
