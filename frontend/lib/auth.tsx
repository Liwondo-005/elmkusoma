"use client"

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react"
import { useRouter, usePathname } from "next/navigation"
import { authApi, type AuthUser } from "@/lib/api"

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
    role?: string
  }) => Promise<{ error?: string }>
  logout: () => void
  token: string | null
}

const AuthContext = createContext<AuthContextValue | null>(null)

const TOKEN_KEY = "elmkusoma_access_token"
const USER_KEY = "elmkusoma_current_user"
const INSTITUTION_KEY = "elmkusoma_institution_id"

function getStoredToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem(TOKEN_KEY)
}

function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function setStoredAuth(token: string, user: AuthUser) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  localStorage.setItem(INSTITUTION_KEY, user.institutionId)
  document.cookie = `elmkusoma_current_user=${encodeURIComponent(JSON.stringify(user))}; path=/; max-age=604800; SameSite=Lax`
}

function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(INSTITUTION_KEY)
  document.cookie = "elmkusoma_current_user=; path=/; max-age=0"
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
      const res = await authApi.login({ email, password })
      setStoredAuth(res.accessToken, res.user)
      setUser(res.user)
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
    role?: string
  }) => {
    try {
      const res = await authApi.register({
        ...data,
        role: data.role || "STUDENT",
      })
      setStoredAuth(res.accessToken, res.user)
      setUser(res.user)
      return {}
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Registration failed"
      return { error: message }
    }
  }, [])

  const logout = useCallback(() => {
    clearStoredAuth()
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
