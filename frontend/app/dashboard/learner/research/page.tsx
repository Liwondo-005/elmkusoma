"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { ResearchProject } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { FlaskConical, CheckCircle2, Clock, Calendar, User, AlertCircle } from "lucide-react"

function ResearchStatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    IDEA: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
    QUESTION: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    LITERATURE: "bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400",
    PROPOSAL: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    METHODOLOGY: "bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-400",
    DATA_COLLECTION: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    ANALYSIS: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
    WRITING: "bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400",
    REVIEW: "bg-pink-100 text-pink-700 dark:bg-pink-900/30 dark:text-pink-400",
    REVISION: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
    DEFENSE: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status] || "bg-muted text-muted-foreground"}`}>
      {status.replace(/_/g, " ")}
    </span>
  )
}

export default function ResearchPage() {
  const { user, loading: authLoading } = useAuth()
  const [projects, setProjects] = useState<ResearchProject[]>([])
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
      const res = await collegeApi.getStudentResearch(studentId)
      setProjects(res.data || [])
    } catch {
      setError("Failed to load research projects")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />

  if (error && projects.length === 0) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle="Manage your research projects, literature, and thesis." />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadProjects() }} className="ml-auto text-xs underline">Retry</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const active = projects.filter(p => p.status !== "COMPLETED").length
  const completed = projects.filter(p => p.status === "COMPLETED").length

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Manage your research projects, literature, and thesis." />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <FlaskConical className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Total Projects</p>
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
              <p className="text-xs font-medium text-muted-foreground">Active</p>
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
              <p className="text-xs font-medium text-muted-foreground">Completed</p>
              <p className="text-2xl font-extrabold text-foreground">{completed}</p>
            </div>
          </div>
        </div>
      </div>

      {projects.length === 0 ? (
        <EmptyState
          icon={<FlaskConical className="size-8" />}
          title="No research projects yet"
          description="Your supervisor will assign research projects or you can propose your own."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p) => (
            <div key={p.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-foreground line-clamp-2">{p.title}</h3>
                <ResearchStatusBadge status={p.status} />
              </div>
              {p.researchQuestion && (
                <p className="mt-2 line-clamp-2 text-sm text-muted-foreground italic">{p.researchQuestion}</p>
              )}
              {p.abstractText && !p.researchQuestion && (
                <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{p.abstractText}</p>
              )}
              <div className="mt-4 space-y-1.5 text-xs text-muted-foreground">
                {p.supervisorId && (
                  <span className="flex items-center gap-1.5">
                    <User className="size-3" />
                    Supervisor assigned
                  </span>
                )}
                {p.startDate && (
                  <span className="flex items-center gap-1.5">
                    <Calendar className="size-3" />
                    Started {new Date(p.startDate).toLocaleDateString()}
                  </span>
                )}
                {p.dueDate && <span className="ml-4.5">Due {new Date(p.dueDate).toLocaleDateString()}</span>}
              </div>
              {p.keywords && (
                <div className="mt-3 flex flex-wrap gap-1">
                  {p.keywords.split(",").map((kw, i) => (
                    <span key={i} className="inline-block rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {kw.trim()}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
