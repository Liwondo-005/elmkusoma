"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Project, SubmissionType } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { FolderKanban, CheckCircle2, Clock, Calendar, AlertCircle, Plus } from "lucide-react"

const SUBMISSION_TYPES: SubmissionType[] = ["DOCUMENT", "IMAGE", "VIDEO", "CODE", "PRESENTATION", "OTHER"]

const emptySubmission = { title: "", description: "", fileUrl: "", submissionType: "DOCUMENT" as SubmissionType }

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
  const [openProjectId, setOpenProjectId] = useState<string | null>(null)
  const [submission, setSubmission] = useState(emptySubmission)
  const [submittingId, setSubmittingId] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadProjects()
  }, [user])

  async function loadProjects(silent = false) {
    try {
      if (!silent) setLoading(true)
      setError(null)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentProjects(studentId)
      setProjects(res.data || [])
    } catch {
      setError(tc("error.generic"))
    } finally {
      if (!silent) setLoading(false)
    }
  }

  async function handleAddSubmission(projectId: string) {
    if (!submission.title.trim()) return
    try {
      setSubmittingId(projectId)
      setError(null)
      await collegeApi.addSubmission(projectId, {
        projectId,
        submissionType: submission.submissionType,
        title: submission.title.trim(),
        description: submission.description.trim() || undefined,
        fileUrl: submission.fileUrl.trim() || undefined,
      })
      setSubmission(emptySubmission)
      setOpenProjectId(null)
      await loadProjects(true)
    } catch {
      setError(tc("error.create"))
    } finally {
      setSubmittingId(null)
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
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => setError(null)} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}
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
          description={t("empty.noProjectsDesc")}
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
              <div className="mt-4">
                <button
                  type="button"
                  onClick={() => {
                    setSubmission(emptySubmission)
                    setOpenProjectId(openProjectId === p.id ? null : p.id)
                  }}
                  aria-expanded={openProjectId === p.id}
                  aria-label={t("learnerActions.addSubmission")}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-primary/20 bg-primary/5 px-3 py-1.5 text-xs font-medium text-primary transition hover:bg-primary/10"
                >
                  <Plus className="size-3" />
                  {t("learnerActions.addSubmission")}
                </button>
              </div>
              {openProjectId === p.id && (
                <form
                  onSubmit={(e) => { e.preventDefault(); handleAddSubmission(p.id) }}
                  className="mt-3 space-y-3 rounded-xl border border-border bg-muted/30 p-4"
                >
                  <div className="grid gap-3 sm:grid-cols-2">
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-muted-foreground">{t("title")} *</label>
                      <input
                        type="text"
                        required
                        value={submission.title}
                        onChange={(e) => setSubmission((s) => ({ ...s, title: e.target.value }))}
                        aria-label={t("title")}
                        className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                      />
                    </div>
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-muted-foreground">{t("learnerActions.submissionType")}</label>
                      <select
                        value={submission.submissionType}
                        onChange={(e) => setSubmission((s) => ({ ...s, submissionType: e.target.value as SubmissionType }))}
                        aria-label={t("learnerActions.submissionType")}
                        className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                      >
                        {SUBMISSION_TYPES.map((st) => (
                          <option key={st} value={st}>{st.replace(/_/g, " ")}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-xs font-medium text-muted-foreground">{t("learnerActions.fileUrl")}</label>
                    <input
                      type="url"
                      value={submission.fileUrl}
                      onChange={(e) => setSubmission((s) => ({ ...s, fileUrl: e.target.value }))}
                      placeholder="https://..."
                      aria-label={t("learnerActions.fileUrl")}
                      className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-xs font-medium text-muted-foreground">{t("description")}</label>
                    <textarea
                      rows={2}
                      value={submission.description}
                      onChange={(e) => setSubmission((s) => ({ ...s, description: e.target.value }))}
                      aria-label={t("description")}
                      className="w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="submit"
                      disabled={submittingId === p.id || !submission.title.trim()}
                      aria-label={tc("submit")}
                      className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs transition hover:bg-primary/90 disabled:opacity-50"
                    >
                      {submittingId === p.id ? tc("loading") : tc("submit")}
                    </button>
                    <button
                      type="button"
                      onClick={() => { setOpenProjectId(null); setSubmission(emptySubmission) }}
                      aria-label={tc("cancel")}
                      className="rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition hover:bg-muted"
                    >
                      {tc("cancel")}
                    </button>
                  </div>
                </form>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
