"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { FieldworkPlacement } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Briefcase, CheckCircle2, Clock, MapPin, Hourglass, AlertCircle, Plus } from "lucide-react"

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

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

const emptyEntry = { entryDate: today(), activities: "", hoursWorked: "" }

export default function FieldworkPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [placements, setPlacements] = useState<FieldworkPlacement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [openPlacementId, setOpenPlacementId] = useState<string | null>(null)
  const [entry, setEntry] = useState(emptyEntry)
  const [savingId, setSavingId] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadFieldwork()
  }, [user])

  async function loadFieldwork(silent = false) {
    try {
      if (!silent) setLoading(true)
      setError(null)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentFieldwork(studentId)
      setPlacements(res.data || [])
    } catch {
      setError(tc("error.generic"))
    } finally {
      if (!silent) setLoading(false)
    }
  }

  async function handleAddLogbook(placementId: string) {
    if (!entry.entryDate || !entry.activities.trim()) return
    try {
      setSavingId(placementId)
      setError(null)
      await collegeApi.addLogbookEntry(placementId, {
        placementId,
        entryDate: entry.entryDate,
        activities: entry.activities.trim(),
        hoursWorked: entry.hoursWorked ? Number(entry.hoursWorked) : undefined,
      })
      setEntry({ ...emptyEntry, entryDate: today() })
      setOpenPlacementId(null)
      await loadFieldwork(true)
    } catch {
      setError(tc("error.create"))
    } finally {
      setSavingId(null)
    }
  }

  if (authLoading || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error && placements.length === 0) {
    return (
      <div role="main" className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("subtitle.fieldwork")} />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadFieldwork() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const active = placements.filter(p => p.status === "ACTIVE").length
  const completed = placements.filter(p => p.status === "COMPLETED").length
  const totalHours = placements.reduce((sum, p) => sum + (p.totalHoursCompleted || 0), 0)

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => setError(null)} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}
      <LearnerHeader firstName={firstName} subtitle={t("subtitle.fieldwork")} />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <Briefcase className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.total")}</p>
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
              <p className="text-xs font-medium text-muted-foreground">{t("filters.active")}</p>
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
              <p className="text-xs font-medium text-muted-foreground">{t("stats.completed")}</p>
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
              <p className="text-xs font-medium text-muted-foreground">{t("creditHours")}</p>
              <p className="text-2xl font-extrabold text-foreground">{totalHours}</p>
            </div>
          </div>
        </div>
      </div>

      {placements.length === 0 ? (
        <EmptyState
          icon={<Briefcase className="size-8" />}
          title={t("empty.noFieldwork")}
          description={t("empty.noFieldworkDesc")}
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
                      <p className="text-xs text-muted-foreground">{t("supervisor")}: {p.supervisorName}</p>
                    )}
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted-foreground">
                      {p.startDate && new Date(p.startDate).toLocaleDateString()} —{" "}
                      {p.endDate ? new Date(p.endDate).toLocaleDateString() : t("filters.active")}
                    </p>
                  </div>
                </div>
                {p.totalHoursRequired ? (
                  <div className="mt-4">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>{p.totalHoursCompleted || 0} / {p.totalHoursRequired} {t("creditHours").toLowerCase()}</span>
                      <span>{pct}%</span>
                    </div>
                    <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
                      <div
                        className="h-full rounded-full bg-primary transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                ) : null}
                <div className="mt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setEntry({ ...emptyEntry, entryDate: today() })
                      setOpenPlacementId(openPlacementId === p.id ? null : p.id)
                    }}
                    aria-expanded={openPlacementId === p.id}
                    aria-label={t("learnerActions.addLogbook")}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-primary/20 bg-primary/5 px-3 py-1.5 text-xs font-medium text-primary transition hover:bg-primary/10"
                  >
                    <Plus className="size-3" />
                    {t("learnerActions.addLogbook")}
                  </button>
                </div>
                {openPlacementId === p.id && (
                  <form
                    onSubmit={(e) => { e.preventDefault(); handleAddLogbook(p.id) }}
                    className="mt-3 space-y-3 rounded-xl border border-border bg-muted/30 p-4"
                  >
                    <div className="grid gap-3 sm:grid-cols-3">
                      <div className="space-y-1.5">
                        <label className="text-xs font-medium text-muted-foreground">{t("learnerActions.entryDate")} *</label>
                        <input
                          type="date"
                          required
                          value={entry.entryDate}
                          onChange={(e) => setEntry((s) => ({ ...s, entryDate: e.target.value }))}
                          aria-label={t("learnerActions.entryDate")}
                          className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                        />
                      </div>
                      <div className="space-y-1.5">
                        <label className="text-xs font-medium text-muted-foreground">{t("learnerActions.hours")}</label>
                        <input
                          type="number"
                          min={0}
                          step={0.5}
                          value={entry.hoursWorked}
                          onChange={(e) => setEntry((s) => ({ ...s, hoursWorked: e.target.value }))}
                          aria-label={t("learnerActions.hours")}
                          className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                        />
                      </div>
                    </div>
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-muted-foreground">{t("learnerActions.activities")} *</label>
                      <textarea
                        rows={2}
                        required
                        value={entry.activities}
                        onChange={(e) => setEntry((s) => ({ ...s, activities: e.target.value }))}
                        aria-label={t("learnerActions.activities")}
                        className="w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
                      />
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="submit"
                        disabled={savingId === p.id || !entry.activities.trim()}
                        aria-label={tc("submit")}
                        className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs transition hover:bg-primary/90 disabled:opacity-50"
                      >
                        {savingId === p.id ? tc("loading") : tc("submit")}
                      </button>
                      <button
                        type="button"
                        onClick={() => setOpenPlacementId(null)}
                        aria-label={tc("cancel")}
                        className="rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition hover:bg-muted"
                      >
                        {tc("cancel")}
                      </button>
                    </div>
                  </form>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
