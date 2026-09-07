const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api"

export interface ApiError {
  success: false
  error?: string
  message?: string
  data?: Record<string, string>
  timestamp?: string
}

export interface ApiSuccess<T> {
  success: true
  data: T
  message?: string
  timestamp?: string
}

export type ApiResponse<T> = ApiSuccess<T> | ApiError

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_access_token")
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("elmkusoma_access_token", accessToken)
  localStorage.setItem("elmkusoma_refresh_token", refreshToken)
}

export function clearTokens() {
  localStorage.removeItem("elmkusoma_access_token")
  localStorage.removeItem("elmkusoma_refresh_token")
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_refresh_token")
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) || {}),
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const body = await res.json()

  if (!res.ok || body.success === false) {
    const errorMsg = body.error || body.message || `Request failed (${res.status})`
    throw new ApiRequestError(errorMsg, res.status, body)
  }

  return body.data !== undefined ? body.data : body as T
}

export class ApiRequestError extends Error {
  status: number
  body: unknown

  constructor(message: string, status: number, body?: unknown) {
    super(message)
    this.name = "ApiRequestError"
    this.status = status
    this.body = body
  }
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface UserInfo {
  id: string
  email: string
  firstName: string
  lastName: string
  role: string
  emailVerified: boolean
}

export interface RegisterPayload {
  firstName: string
  middleName?: string
  lastName: string
  email: string
  password: string
  phone?: string
  role: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface ForgotPasswordPayload {
  email: string
}

export interface ResetPasswordPayload {
  token: string
  newPassword: string
}

export interface VerifyEmailPayload {
  token: string
}

export const authApi = {
  register: (data: RegisterPayload) =>
    request<AuthResponse>("/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  login: (data: LoginPayload) =>
    request<AuthResponse>("/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  refresh: (refreshToken: string) =>
    request<AuthResponse>("/v1/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),

  forgotPassword: (data: ForgotPasswordPayload) =>
    request<void>("/v1/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  resetPassword: (data: ResetPasswordPayload) =>
    request<void>("/v1/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  verifyEmail: (data: VerifyEmailPayload) =>
    request<void>("/v1/auth/verify-email", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  logout: (refreshToken: string) =>
    request<void>("/v1/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),
}

export interface InstitutionResponse {
  id: string
  name: string
  description: string | null
  type: string
  status: string
  logoUrl: string | null
  website: string | null
  email: string | null
  phone: string | null
  address: string | null
  city: string | null
  country: string | null
  createdAt: string
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

export const institutionApi = {
  list: (page = 0, size = 20) =>
    request<PageResponse<InstitutionResponse>>(`/v1/institutions?page=${page}&size=${size}`),

  get: (id: string) =>
    request<InstitutionResponse>(`/v1/institutions/${id}`),

  create: (data: { name: string; type: string; description?: string; logoUrl?: string; website?: string; email?: string; phone?: string; address?: string; city?: string; country?: string }) =>
    request<InstitutionResponse>("/v1/institutions", {
      method: "POST",
      body: JSON.stringify(data),
    }),
}
