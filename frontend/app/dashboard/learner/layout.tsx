"use client"

import { useAuth } from "@/lib/auth"
import { Loader2 } from "lucide-react"

export default function LearnerLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!user) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-muted-foreground">Access denied</p>
      </div>
    )
  }

  const isOtherLearner = user.role === "Other Learner"
  const level = (user.learningLevel || "").toUpperCase()
  const isCollegeStudent = user.role === "Student" && (level === "COLLEGE" || level === "UNIVERSITY")

  if (!isOtherLearner && !isCollegeStudent) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-muted-foreground">Access denied</p>
      </div>
    )
  }

  return <>{children}</>
}
