"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type TeacherInfo } from "@/lib/api"
import { GraduationCap, Mail, MessageSquare, BookOpen, Users } from "lucide-react"

const SUBJECT_COLORS: Record<string, string> = {
  Mathematics: "bg-blue-500",
  English: "bg-purple-500",
  Science: "bg-green-500",
  Kiswahili: "bg-orange-500",
  SocialStudies: "bg-cyan-500",
}

export default function MyTeachersPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const tc = useTranslations("common")
  const [teachers, setTeachers] = useState<TeacherInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    if (!user) return
    loadTeachers()
  }, [user])

  async function loadTeachers() {
    try {
      setLoading(true)
      setError("")
      const data = await primaryApi.getTeachers()
      setTeachers(data)
    } catch {
      setError(t("teachers.loadError"))
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const subjects = [...new Set(teachers.map((t) => t.subjectName).filter(Boolean))]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-purple-50 via-card to-blue-50 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-purple-500/10">
            <GraduationCap className="size-6 text-purple-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("teachers.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("teachers.subtitle")}</p>
          </div>
        </div>
      </div>

      {error && (
        <div className="rounded-xl bg-destructive/10 p-4 text-center text-sm text-destructive">
          {error}
          <button onClick={loadTeachers} className="ml-2 underline">{tc("retry")}</button>
        </div>
      )}

      {teachers.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-muted">
            <GraduationCap className="size-8 text-muted-foreground" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("teachers.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
            {t("teachers.emptyDesc")}
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="flex items-center gap-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <Users className="size-4" />
              {t("teachers.countLine", { count: teachers.length })}
            </span>
            <span className="flex items-center gap-1.5">
              <BookOpen className="size-4" />
              {t("teachers.subjectsLine", { count: subjects.length })}
            </span>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {teachers.map((teacher) => {
              const initials = `${teacher.firstName?.charAt(0) || ""}${teacher.lastName?.charAt(0) || ""}`.toUpperCase()
              const colorClass = SUBJECT_COLORS[teacher.subjectName] || SUBJECT_COLORS.default

              return (
                <div
                  key={teacher.id}
                  className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md"
                >
                  <div className="flex items-start gap-4">
                    <div className={`flex size-14 shrink-0 items-center justify-center rounded-2xl ${colorClass} text-lg font-bold text-white`}>
                      {initials || "?"}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="text-base font-semibold text-foreground">
                        {teacher.firstName} {teacher.lastName}
                      </h3>
                      {teacher.subjectName && (
                        <p className="mt-0.5 text-sm font-medium text-primary">
                          {teacher.subjectName}
                        </p>
                      )}
                      {teacher.specialization && (
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {teacher.specialization}
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="mt-4 flex items-center gap-2">
                    {teacher.email && (
                      <a
                        href={`mailto:${teacher.email}`}
                        className="flex items-center gap-1.5 rounded-lg bg-muted px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted/80"
                      >
                        <Mail className="size-3.5" />
                        {t("teachers.emailButton")}
                      </a>
                    )}
                    <Link
                      href="/dashboard/messages"
                      className="flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary/20"
                    >
                      <MessageSquare className="size-3.5" />
                      {t("teachers.askButton")}
                    </Link>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
