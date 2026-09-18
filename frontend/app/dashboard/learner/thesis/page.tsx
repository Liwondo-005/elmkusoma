"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Thesis } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { FileText, CheckCircle2, Clock, Calendar, User } from "lucide-react"

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
  const { user, loading: authLoading } = useAuth()
  const [theses, setTheses] = useState<Thesis[]>([])
  const [loading, setLoading] = useState(true)

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
      // silent
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Student"
  const active = theses.filter(t => t.status !== "COMPLETED").length
  const completed = theses.filter(t => t.status === "COMPLETED").length

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Manage your thesis and dissertation." />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <FileText className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Total</p>
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
              <p className="text-xs font-medium text-muted-foreground">In Progress</p>
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

      {theses.length === 0 ? (
        <EmptyState
          icon={<FileText className="size-8" />}
          title="No thesis yet"
          description="Your thesis or dissertation will appear here once assigned by your supervisor."
        />
      ) : (
        <div className="space-y-4">
          {theses.map((t) => (
            <div key={t.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-foreground">{t.title}</h3>
                <ThesisStatusBadge status={t.status} />
              </div>
              {t.abstractText && (
                <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{t.abstractText}</p>
              )}
              <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                {t.supervisorId && (
                  <span className="flex items-center gap-1.5">
                    <User className="size-3" />
                    Supervisor assigned
                  </span>
                )}
                {t.submissionDate && (
                  <span className="flex items-center gap-1.5">
                    <Calendar className="size-3" />
                    Submitted {new Date(t.submissionDate).toLocaleDateString()}
                  </span>
                )}
                {t.defenseDate && (
                  <span className="flex items-center gap-1.5">
                    <Calendar className="size-3" />
                    Defense {new Date(t.defenseDate).toLocaleDateString()}
                  </span>
                )}
                {t.finalGrade && (
                  <span className="font-medium text-foreground">Grade: {t.finalGrade}</span>
                )}
                {t.wordCount && (
                  <span>{t.wordCount.toLocaleString()} words</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
