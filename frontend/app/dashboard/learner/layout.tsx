"use client"

import { useAuth } from "@/lib/auth"
import { Loader2 } from "lucide-react"

export default function LearnerLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20" role="status" aria-label="Loading">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
        <span className="sr-only">Loading...</span>
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
      <div className="flex items-center justify-center py-20" role="alert">
        <p className="text-muted-foreground">Access denied</p>
      </div>
    )
  }

  return (
    <>
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:m-4 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground"
      >
        Skip to main content
      </a>
      <main id="main-content" role="main">
        {children}
      </main>
    </>
  )
}
