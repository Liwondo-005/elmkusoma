"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { FieldworkPlacement } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Briefcase, CheckCircle2, Clock, MapPin, Hourglass, AlertCircle } from "lucide-react"

function PlacementStatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    PLANNING: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    ACTIVE: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
    COMPLETED: "bg-muted text-muted-foreground",
    TERMINATED: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status] || "bg-muted"}`}>
      {status}
    </span>
  )
}

export default function FieldworkPage() {
  const { user, loading: authLoading } = useAuth()
  const [placements, setPlacements] = useState<FieldworkPlacement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadFieldwork()
  }, [user])

  async function loadFieldwork() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentFieldwork(studentId)
      setPlacements(res.data || [])
    } catch {
      setError("Failed to load fieldwork")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />

  if (error && placements.length === 0) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle="Track your fieldwork placements, logbook entries, and practical experience." />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadFieldwork() }} className="ml-auto text-xs underline">Retry</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const active = placements.filter(p => p.status === "ACTIVE").length
  const completed = placements.filter(p => p.status === "COMPLETED").length
  const totalHours = placements.reduce((sum, p) => sum + (p.totalHoursCompleted || 0), 0)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Track your fieldwork placements, logbook entries, and practical experience." />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <Briefcase className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Placements</p>
              <p className="text-2xl font-extrabold text-foreground">{placements.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Active</p>
              <p className="text-2xl font-extrabold text-foreground">{active}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <Clock className="size-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Completed</p>
              <p className="text-2xl font-extrabold text-foreground">{completed}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <Hourglass className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Hours Logged</p>
              <p className="text-2xl font-extrabold text-foreground">{totalHours}</p>
            </div>
          </div>
        </div>
      </div>

      {placements.length === 0 ? (
        <EmptyState
          icon={<Briefcase className="size-8" />}
          title="No fieldwork placements yet"
          description="Your placement will appear here once assigned."
        />
      ) : (
        <div className="space-y-4">
          {placements.map((p) => {
            const pct = p.totalHoursRequired
              ? Math.min(100, Math.round(((p.totalHoursCompleted || 0) / p.totalHoursRequired) * 100))
              : 0
            return (
              <div key={p.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-foreground">{p.placementTitle}</h3>
                      <PlacementStatusBadge status={p.status} />
                    </div>
                    <p className="flex items-center gap-1 text-sm text-muted-foreground">
                      <MapPin className="size-3.5" />
                      {p.organisationName}
                    </p>
                    {p.supervisorName && (
                      <p className="text-xs text-muted-foreground">Supervisor: {p.supervisorName}</p>
                    )}
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted-foreground">
                      {p.startDate && new Date(p.startDate).toLocaleDateString()} —{" "}
                      {p.endDate ? new Date(p.endDate).toLocaleDateString() : "Ongoing"}
                    </p>
                  </div>
                </div>
                {p.totalHoursRequired ? (
                  <div className="mt-4">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>{p.totalHoursCompleted || 0} / {p.totalHoursRequired} hours</span>
                      <span>{pct}%</span>
                    </div>
                    <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
                      <div
                        className="h-full rounded-full bg-primary transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                ) : null}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
