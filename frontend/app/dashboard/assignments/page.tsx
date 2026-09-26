"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learningApi, type Assignment } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { FileText, Clock, CheckCircle, Send, ArrowRight, AlertTriangle, BookOpen, Star } from "lucide-react"

export default function AssignmentsPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await learningApi.getAssignments(user!.classGroupId || "")
      setAssignments(data)
    } catch {
      setAssignments([])
    } finally {
      setLoading(false)
    }
  }

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  const pendingAssignments = assignments.filter((a) => !isOverdue(a.dueDate))
  const overdueAssignments = assignments.filter((a) => isOverdue(a.dueDate))

  if (isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        {/* Header */}
        <div className="rounded-2xl border border-border bg-gradient-to-br from-amber-50 via-card to-orange-50 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-500/10">
              <BookOpen className="size-6 text-amber-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("assignments.practiceTitle")}</h1>
              <p className="text-sm text-muted-foreground">{t("assignments.practiceSubtitle")}</p>
            </div>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : assignments.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border py-12 text-center">
            <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-muted">
              <BookOpen className="size-8 text-muted-foreground" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-foreground">{t("assignments.emptyPracticeTitle")}</h3>
            <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
              {t("assignments.emptyPracticeDesc")}
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Overdue Assignments */}
            {overdueAssignments.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                  <AlertTriangle className="size-5 text-orange-500" />
                  {t("assignments.needsAttention")}
                </h2>
                <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {overdueAssignments.map((a) => (
                    <div key={a.id} className="rounded-2xl border-2 border-orange-200 bg-orange-50/50 p-5">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-orange-100 px-2.5 py-1 text-xs font-semibold text-orange-700">
                          <Clock className="size-3" /> {ts("overdue")}
                        </span>
                      </div>
                      <h3 className="mt-3 text-base font-semibold text-foreground">{a.title}</h3>
                      {a.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{a.description}</p>
                      )}
                      <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
                        <span>{t("assignments.totalMarks", { count: a.totalMarks })}</span>
                        {a.dueDate && (
                          <span className="text-orange-600 font-medium">
                            {t("assignments.dueDate", { date: new Date(a.dueDate).toLocaleDateString() })}
                          </span>
                        )}
                      </div>
                      <Link
                        href={`/dashboard/assignments/${a.id}`}
                        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-orange-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-orange-700 transition-colors"
                      >
                        <Send className="size-4" /> {t("assignments.submitNow")}
                      </Link>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {/* Pending Assignments */}
            {pendingAssignments.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-foreground">{t("assignments.practiceWorkTitle")}</h2>
                <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {pendingAssignments.map((a) => (
                    <div key={a.id} className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md hover:border-primary/30">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
                          <Star className="size-3" /> {t("assignments.practiceBadge")}
                        </span>
                      </div>
                      <h3 className="mt-3 text-base font-semibold text-foreground">{a.title}</h3>
                      {a.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{a.description}</p>
                      )}
                      <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                        <span>{t("assignments.marksCount", { count: a.totalMarks })}</span>
                        {a.dueDate && (
                          <span className="flex items-center gap-1">
                            <Clock className="size-3" />
                            {t("assignments.dueDate", { date: new Date(a.dueDate).toLocaleDateString() })}
                          </span>
                        )}
                      </div>
                      <Link
                        href={`/dashboard/assignments/${a.id}`}
                        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-colors"
                      >
                        <Send className="size-4" /> {t("assignments.submit")}
                      </Link>
                    </div>
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </div>
    )
  }

  /* Non-Primary: Original assignments page */
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("assignments.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("assignments.subtitle")}</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : assignments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <FileText className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("assignments.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("assignments.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {assignments.map((a) => {
            const overdue = isOverdue(a.dueDate)
            return (
              <div
                key={a.id}
                className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm"
              >
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                  <FileText className="size-5 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                  {a.description && (
                    <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                  )}
                  <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                    <span>{t("assignments.totalMarks", { count: a.totalMarks })}</span>
                    {a.dueDate && (
                      <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                        <Clock className="size-3" />
                        {t("assignments.dueDate", { date: new Date(a.dueDate).toLocaleDateString() })}
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {overdue ? (
                    <span className="rounded-full bg-red-100 px-2 py-0.5 text-[10px] font-semibold text-red-700">{ts("overdue")}</span>
                  ) : (
                    <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700">{ts("active")}</span>
                  )}
                  <Link
                    href={`/dashboard/assignments/${a.id}`}
                    className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    <Send className="size-3" /> {t("assignments.view")}
                  </Link>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
