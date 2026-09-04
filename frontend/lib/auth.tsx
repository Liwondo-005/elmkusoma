"use client"

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react"
import { useRouter, usePathname } from "next/navigation"

export interface AuthUser {
  id: string
  name: string
  email: string
  role: "student" | "teacher" | "admin"
}

interface AuthContextValue {
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<{ error?: string }>
  register: (data: { name: string; email: string; password: string; educationLevel: string }) => Promise<{ error?: string }>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

const MOCK_USERS_KEY = "elmkusoma_users"
const CURRENT_USER_KEY = "elmkusoma_current_user"

function getStoredUsers(): Array<AuthUser & { password: string }> {
  if (typeof window === "undefined") return []
  try {
    return JSON.parse(localStorage.getItem(MOCK_USERS_KEY) || "[]")
  } catch {
    return []
  }
}

function setStoredUsers(users: Array<AuthUser & { password: string }>) {
  localStorage.setItem(MOCK_USERS_KEY, JSON.stringify(users))
}

function getCurrentUser(): AuthUser | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem(CURRENT_USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function setCurrentUser(user: AuthUser | null) {
  if (user) {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(CURRENT_USER_KEY)
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const stored = getCurrentUser()
    setUser(stored)
    setLoading(false)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const users = getStoredUsers()
    const found = users.find((u) => u.email === email && u.password === password)
    if (!found) {
      return { error: "Invalid email or password" }
    }
    const { password: _, ...authUser } = found
    setCurrentUser(authUser)
    setUser(authUser)
    return {}
  }, [])

  const register = useCallback(async (data: { name: string; email: string; password: string; educationLevel: string }) => {
    const users = getStoredUsers()
    if (users.some((u) => u.email === data.email)) {
      return { error: "An account with this email already exists" }
    }
    const newUser: AuthUser & { password: string } = {
      id: crypto.randomUUID(),
      name: data.name,
      email: data.email,
      role: "student",
      password: data.password,
    }
    users.push(newUser)
    setStoredUsers(users)
    const { password: _, ...authUser } = newUser
    setCurrentUser(authUser)
    setUser(authUser)
    return {}
  }, [])

  const logout = useCallback(() => {
    setCurrentUser(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
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
