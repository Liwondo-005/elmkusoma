"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useRequireAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"

export default function NurseryLayout({ children }: { children: React.ReactNode }) {
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()

  useEffect(() => {
    if (!authLoading && user) {
      const level = (user.learningLevel || "").toUpperCase()
      const isStudent = user.role === "Student"
      if (!isStudent) {
        router.replace("/dashboard")
      } else if (level !== "NURSERY") {
        router.replace("/dashboard")
      }
    }
  }, [user, authLoading, router])

  if (authLoading) return <LoadingState />
  if (!user) return null

  const level = (user.learningLevel || "").toUpperCase()
  if (level !== "NURSERY" || user.role !== "Student") return null

  return (
    <div className="nursery-world">
      {children}
    </div>
  )
}
