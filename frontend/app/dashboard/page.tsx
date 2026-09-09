"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth"

export default function DashboardPage() {
  const { user, loading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (loading) return
    if (!user) return

    const role = user.role
    if (role === "Teacher" || role === "Instructor") {
      router.replace("/dashboard/teacher")
    } else if (role === "Admin" || role === "Institution Admin") {
      router.replace("/dashboard/admin")
    }
  }, [user, loading, router])

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return null
}
