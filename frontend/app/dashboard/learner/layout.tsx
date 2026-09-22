"use client"

import { useAuth } from "@/lib/auth"
import { Loader2 } from "lucide-react"
import { useEffect } from "react"

export default function LearnerLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        const active = document.activeElement as HTMLElement
        if (active && active.blur) active.blur()
      }
    }
    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20" role="status" aria-label="Loading" aria-live="polite">
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
  const isCollegeStudent = user.role === "Student" && (level === "COLLEGE" || level === "UNIVERSITY" || level === "VETA")

  if (!isOtherLearner && !isCollegeStudent) {
    return (
      <div className="flex items-center justify-center py-20" role="alert" aria-live="assertive">
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
      <main id="main-content" role="main" aria-label="Learner Dashboard">
        <div aria-live="polite" aria-atomic="true" className="sr-only" id="dashboard-announcer" />
        {children}
      </main>
    </>
  )
}
