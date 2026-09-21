"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Project } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { FolderKanban, CheckCircle2, Clock, Calendar, AlertCircle } from "lucide-react"

function ProjectStatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    IDEATION: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
    PLANNING: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    IN_PROGRESS: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    REVIEW: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
    ARCHIVED: "bg-muted text-muted-foreground",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status] || "bg-muted"}`}>
      {status.replace(/_/g, " ")}
    </span>
  )
}

export default function ProjectsPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadProjects()
  }, [user])

  async function loadProjects() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentProjects(studentId)
      setProjects(res.data || [])
    } catch {
      setError(tc("error"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error && projects.length === 0) {
    return (
      <div role="main" className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("subtitle.projects")} />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadProjects() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const inProgress = projects.filter(p => p.status === "IN_PROGRESS" || p.status === "PLANNING").length
  const completed = projects.filter(p => p.status === "COMPLETED").length

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("subtitle.projects")} />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <FolderKanban className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.total")}</p>
              <p className="text-2xl font-extrabold text-foreground">{projects.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <Clock className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.inProgress")}</p>
              <p className="text-2xl font-extrabold text-foreground">{inProgress}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.completed")}</p>
              <p className="text-2xl font-extrabold text-foreground">{completed}</p>
            </div>
          </div>
        </div>
      </div>

      {projects.length === 0 ? (
        <EmptyState
          icon={<FolderKanban className="size-8" />}
          title={t("empty.noProjects")}
          description={t("empty.noProjects")}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p) => (
            <div key={p.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-foreground line-clamp-2">{p.title}</h3>
                <ProjectStatusBadge status={p.status} />
              </div>
              {p.description && <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{p.description}</p>}
              <div className="mt-4 flex items-center gap-4 text-xs text-muted-foreground">
                {p.startDate && (
                  <span className="flex items-center gap-1">
                    <Calendar className="size-3" />
                    {new Date(p.startDate).toLocaleDateString()}
                  </span>
                )}
                {p.dueDate && <span>{t("deadline")} {new Date(p.dueDate).toLocaleDateString()}</span>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
