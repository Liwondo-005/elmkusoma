"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { StudyTask } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { FlaskConical, CheckCircle2, Clock, Target, AlertCircle } from "lucide-react"

function StatusBadge({ status }: { status: boolean }) {
  const t = useTranslations("highered")
  return status ? (
    <span className="inline-flex items-center rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400">
      {t("stats.completed")}
    </span>
  ) : (
    <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
      {t("filters.pending")}
    </span>
  )
}

export default function PracticalLabPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [tasks, setTasks] = useState<StudyTask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadTasks()
  }, [user])

  async function loadTasks() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentTasks(studentId)
      const allTasks = (res.data as StudyTask[] | undefined) || []
      const practicalTasks = allTasks.filter(
        (t) => t.taskType === "PROJECT" || t.taskType === "ASSIGNMENT" || t.taskType === "RESEARCH"
      )
      setTasks(practicalTasks)
    } catch {
      setError(tc("error"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error && tasks.length === 0) {
    return (
      <div role="main" className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("subtitle.practicalLab")} />
        <div className="rounded-2xl border border-border bg-card p-4 text-sm text-red-600 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadTasks() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const completed = tasks.filter((t) => t.isCompleted).length
  const pending = tasks.length - completed

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("subtitle.practicalLab")} />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <FlaskConical className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.total")}</p>
              <p className="text-2xl font-extrabold text-foreground">{tasks.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <Clock className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("filters.pending")}</p>
              <p className="text-2xl font-extrabold text-foreground">{pending}</p>
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

      {tasks.length === 0 ? (
        <EmptyState
          icon={<FlaskConical className="size-8" />}
          title={t("empty.noModules")}
          description={t("empty.noModules")}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {tasks.map((task) => (
            <div key={task.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-foreground line-clamp-2">{task.title}</h3>
                <StatusBadge status={task.isCompleted} />
              </div>
              {task.description && <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{task.description}</p>}
              <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
                <Target className="size-3" />
                <span className="capitalize">{task.taskType?.replace(/_/g, " ").toLowerCase()}</span>
                {task.scheduledDate && (
                  <span className="ml-auto">{new Date(task.scheduledDate).toLocaleDateString()}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
