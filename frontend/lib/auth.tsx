"use client"

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react"
import { useRouter, usePathname } from "next/navigation"
import { authApi, setTokens, clearTokens, type UserInfo } from "@/lib/api"

export interface AuthUser {
  id: string
  name: string
  email: string
  role: string
  firstName?: string
  lastName?: string
  institutionId?: string
  classGroupId?: string
}

interface AuthContextValue {
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<{ error?: string }>
  register: (data: {
    firstName: string
    middleName?: string
    lastName: string
    email: string
    password: string
    phone?: string
    role: string
  }) => Promise<{ error?: string }>
  logout: () => Promise<void>
  token: string | null
}

const AuthContext = createContext<AuthContextValue | null>(null)

const CURRENT_USER_KEY = "elmkusoma_current_user"
const TOKEN_KEY = "elmkusoma_access_token"

function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem(CURRENT_USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function getStoredToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem(TOKEN_KEY)
}

function setCurrentUser(user: AuthUser | null) {
  if (user) {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user))
    document.cookie = `elmkusoma_current_user=${encodeURIComponent(JSON.stringify(user))}; path=/; max-age=604800; SameSite=Lax`
  } else {
    localStorage.removeItem(CURRENT_USER_KEY)
    document.cookie = "elmkusoma_current_user=; path=/; max-age=0"
  }
}

function setAuthCookie(token: string | null) {
  if (token) {
    document.cookie = `elmkusoma_access_token=${token}; path=/; max-age=86400; SameSite=Lax`
  } else {
    document.cookie = "elmkusoma_access_token=; path=/; max-age=0"
  }
}

function mapUserInfo(info: UserInfo): AuthUser {
  return {
    id: info.id,
    name: info.fullName || [info.firstName, info.lastName].filter(Boolean).join(" "),
    email: info.email,
    role: info.role,
    firstName: info.firstName,
    lastName: info.lastName,
    institutionId: info.institutionId,
    classGroupId: info.classGroupId || undefined,
  }
}

function mapRoleToFrontend(backendRole: string): string {
  const roleMap: Record<string, string> = {
    STUDENT: "Student",
    TEACHER: "Teacher",
    PARENT: "Parent",
    ADMIN: "Admin",
    INSTITUTION_ADMIN: "Institution Admin",
  }
  return roleMap[backendRole] || backendRole
}

function mapRoleToBackend(frontendRole: string): string {
  const roleMap: Record<string, string> = {
    Student: "STUDENT",
    Teacher: "TEACHER",
    Lecturer: "TEACHER",
    Facilitator: "TEACHER",
    Parent: "PARENT",
    Admin: "ADMIN",
    "Institution Admin": "INSTITUTION_ADMIN",
  }
  return roleMap[frontendRole] || "STUDENT"
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = getStoredToken()
    const stored = getStoredUser()
    if (token && stored) {
      setUser(stored)
    }
    setLoading(false)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    try {
      const response = await authApi.login({ email, password })
      const authUser = mapUserInfo(response.user)
      authUser.role = mapRoleToFrontend(response.user.role)
      setTokens(response.accessToken, response.refreshToken)
      setAuthCookie(response.accessToken)
      setCurrentUser(authUser)
      setUser(authUser)
      return {}
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Login failed"
      return { error: message }
    }
  }, [])

  const register = useCallback(async (data: {
    firstName: string
    middleName?: string
    lastName: string
    email: string
    password: string
    phone?: string
    role: string
  }) => {
    try {
      const backendRole = mapRoleToBackend(data.role)
      const response = await authApi.register({
        firstName: data.firstName,
        middleName: data.middleName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
        phone: data.phone,
        role: backendRole,
      })
      const authUser = mapUserInfo(response.user)
      authUser.role = mapRoleToFrontend(response.user.role)
      setTokens(response.accessToken, response.refreshToken)
      setAuthCookie(response.accessToken)
      setCurrentUser(authUser)
      setUser(authUser)
      return {}
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Registration failed"
      return { error: message }
    }
  }, [])

  const logout = useCallback(async () => {
    clearTokens()
    setAuthCookie(null)
    setCurrentUser(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, token: getStoredToken() }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return ctx
}

export function useRequireAuth() {
  const { user, loading } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (!loading && !user) {
      router.push(`/login?redirect=${encodeURIComponent(pathname)}`)
    }
  }, [user, loading, router, pathname])

  return { user, loading }
}
