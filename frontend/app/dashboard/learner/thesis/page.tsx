"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Thesis } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { FileText, CheckCircle2, Clock, Calendar, User, AlertCircle } from "lucide-react"

function ThesisStatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    NOT_STARTED: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
    PROPOSAL: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    IN_PROGRESS: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    REVIEW: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    REVISION: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
    DEFENSE_SCHEDULED: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
    DEFENSE_COMPLETE: "bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400",
    COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status] || "bg-muted text-muted-foreground"}`}>
      {status.replace(/_/g, " ")}
    </span>
  )
}

export default function ThesisPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [theses, setTheses] = useState<Thesis[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadTheses()
  }, [user])

  async function loadTheses() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentTheses(studentId)
      setTheses(res.data || [])
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Student"
  const active = theses.filter(t => t.status !== "COMPLETED").length
  const completed = theses.filter(t => t.status === "COMPLETED").length

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadTheses() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      <LearnerHeader firstName={firstName} subtitle={t("thesis.subtitle")} />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <FileText className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{tc("total")}</p>
              <p className="text-2xl font-extrabold text-foreground">{theses.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <Clock className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("thesis.inProgress")}</p>
              <p className="text-2xl font-extrabold text-foreground">{active}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("thesis.completed")}</p>
              <p className="text-2xl font-extrabold text-foreground">{completed}</p>
            </div>
          </div>
        </div>
      </div>

      {theses.length === 0 ? (
        <EmptyState
          icon={<FileText className="size-8" />}
          title={t("thesis.noThesis")}
          description={t("thesis.noThesisDesc")}
        />
      ) : (
        <div className="space-y-4">
          {theses.map((thesis) => (
            <div key={thesis.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-foreground">{thesis.title}</h3>
                <ThesisStatusBadge status={thesis.status} />
              </div>
              {thesis.abstractText && (
                <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{thesis.abstractText}</p>
              )}
              <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                {thesis.supervisorId && (
                  <span className="flex items-center gap-1.5">
                    <User className="size-3" />
                    {t("thesis.supervisorAssigned")}
                  </span>
                )}
                {thesis.submissionDate && (
                  <span className="flex items-center gap-1.5">
                    <Calendar className="size-3" />
                    Submitted {new Date(thesis.submissionDate).toLocaleDateString()}
                  </span>
                )}
                {thesis.defenseDate && (
                  <span className="flex items-center gap-1.5">
                    <Calendar className="size-3" />
                    Defense {new Date(thesis.defenseDate).toLocaleDateString()}
                  </span>
                )}
                {thesis.finalGrade && (
                  <span className="font-medium text-foreground">{t("thesis.grade")}: {thesis.finalGrade}</span>
                )}
                {thesis.wordCount && (
                  <span>{thesis.wordCount.toLocaleString()} words</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
