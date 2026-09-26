"use client"

import { useAuth } from "@/lib/auth"
import { announce } from "@/lib/announce"
import { useTranslations } from "next-intl"
import { Loader2 } from "lucide-react"
import { useEffect, useRef } from "react"

export default function LearnerLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  const t = useTranslations("common")
  const announcedUserIdRef = useRef<string | null | undefined>(undefined)
  const loadedAnnouncedRef = useRef(false)

  function tl(key: string, defaultValue: string): string {
    return t.has(key) ? t(key) : defaultValue
  }

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

  useEffect(() => {
    if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return
    navigator.serviceWorker.register("/sw.js").catch(() => {})
  }, [])

  useEffect(() => {
    function handleAnnounce(event: Event) {
      const detail = (event as CustomEvent<unknown>).detail
      if (typeof detail === "string" && detail) announce(detail)
    }
    window.addEventListener("elmkusoma:announce", handleAnnounce)
    return () => window.removeEventListener("elmkusoma:announce", handleAnnounce)
  }, [])

  useEffect(() => {
    if (loading) return
    const userId = user?.id ?? null
    if (announcedUserIdRef.current === userId) return
    announcedUserIdRef.current = userId

    if (user) {
      const messages: string[] = []
      if (!loadedAnnouncedRef.current) {
        loadedAnnouncedRef.current = true
        messages.push(tl("learnerDashboardLoaded", "Learner dashboard loaded"))
      }
      messages.push(tl("accessGranted", "Access granted"))
      announce(messages.join(". "))
    } else {
      announce(tl("accessDenied", "Access denied"))
    }
    // tl closes over t; re-run when t/user/loading change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, user, t])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20" role="status" aria-label={t("loading")} aria-live="polite">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
        <span className="sr-only">{t("loading")}</span>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-muted-foreground">{t("accessDenied")}</p>
      </div>
    )
  }

  const isOtherLearner = user.role === "Other Learner"
  const level = (user.learningLevel || "").toUpperCase()
  const isCollegeStudent = user.role === "Student" && (level === "COLLEGE" || level === "UNIVERSITY" || level === "VETA")

  if (!isOtherLearner && !isCollegeStudent) {
    return (
      <div className="flex items-center justify-center py-20" role="alert" aria-live="assertive">
        <p className="text-muted-foreground">{t("accessDenied")}</p>
      </div>
    )
  }

  return (
    <>
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:m-4 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground"
      >
        {t("skipToMainContent")}
      </a>
      <main id="main-content" role="main" aria-label={t("learnerDashboard")}>
        <div aria-live="polite" aria-atomic="true" className="sr-only" id="dashboard-announcer" />
        {children}
      </main>
    </>
  )
}
