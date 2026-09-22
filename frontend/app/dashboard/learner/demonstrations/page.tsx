"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { PracticalDemonstration } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Trophy, Send, CheckCircle2, Clock, AlertCircle, Star } from "lucide-react"

function DemoStatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    DRAFT: "bg-muted text-muted-foreground",
    SUBMITTED: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    UNDER_REVIEW: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    APPROVED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
    NEEDS_REVISION: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
    REJECTED: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status] || "bg-muted"}`}>
      {status.replace(/_/g, " ")}
    </span>
  )
}

export default function DemonstrationsPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [demos, setDemos] = useState<PracticalDemonstration[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadDemos()
  }, [user])

  async function loadDemos() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentDemonstrations(studentId)
      setDemos((res.data as PracticalDemonstration[] | undefined) || [])
    } catch { setError(tc("error.load")) } finally { setLoading(false) }
  }

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error && demos.length === 0) {
    return (
      <div role="main" className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("demonstrations.subtitle")} />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadDemos() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const drafts = demos.filter(d => d.status === "DRAFT").length
  const submitted = demos.filter(d => d.status === "SUBMITTED" || d.status === "UNDER_REVIEW").length
  const approved = demos.filter(d => d.status === "APPROVED").length

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("demonstrations.subtitle")} />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10"><Trophy className="size-5 text-primary" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("demonstrations.totalSubmissions")}</p><p className="text-2xl font-extrabold text-foreground">{demos.length}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-muted"><AlertCircle className="size-5 text-muted-foreground" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("demonstrations.drafts")}</p><p className="text-2xl font-extrabold text-foreground">{drafts}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30"><Send className="size-5 text-blue-600 dark:text-blue-400" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("demonstrations.submitted")}</p><p className="text-2xl font-extrabold text-foreground">{submitted}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30"><CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">{t("demonstrations.approved")}</p><p className="text-2xl font-extrabold text-foreground">{approved}</p></div>
          </div>
        </div>
      </div>

      {demos.length === 0 ? (
        <EmptyState title={t("demonstrations.noDemonstrations")} description={t("demonstrations.noDemonstrationsDesc")} />
      ) : (
        <div className="space-y-4">
          {demos.map((d) => (
            <div key={d.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-foreground">{d.title}</h3>
                    <DemoStatusBadge status={d.status} />
                  </div>
                  {d.description && <p className="line-clamp-2 text-sm text-muted-foreground">{d.description}</p>}
                </div>
                <div className="flex items-center gap-4 text-right">
                  {d.score != null && (
                    <div className="flex items-center gap-1 text-amber-600">
                      <Star className="size-4" />
                      <span className="font-bold">{d.score}</span>
                    </div>
                  )}
                  <div className="text-xs text-muted-foreground">
                    {d.submittedAt && <p>Submitted {new Date(d.submittedAt).toLocaleDateString()}</p>}
                    {d.reviewedAt && <p>Reviewed {new Date(d.reviewedAt).toLocaleDateString()}</p>}
                  </div>
                </div>
              </div>
              {d.reviewNotes && (
                <div className="mt-3 rounded-xl bg-muted/50 px-4 py-2 text-sm text-muted-foreground">
                  <span className="font-medium">{t("demonstrations.reviewerNotes")}</span> {d.reviewNotes}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
