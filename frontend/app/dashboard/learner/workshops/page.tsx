"use client"

import { useState, useEffect, useCallback } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  AlertCircle,
  FlaskConical,
  Plus,
  Calendar,
  Clock,
  MapPin,
  Users,
  CheckCircle,
  ExternalLink,
  Trash2,
} from "lucide-react"

type WorkshopSession = {
  id: string
  institutionId: string
  studentId: string
  title: string
  description?: string
  workshopType: string
  courseId?: string
  scheduledAt?: string
  durationMinutes?: number
  location?: string
  status: string
  maxParticipants?: number
  currentParticipants?: number
  materialsUrl?: string
  instructorId?: string
}

const WORKSHOP_TYPES = ["Workshop", "Lab Session", "Practical Demo", "Hands-On", "Field Lab", "Studio Session"] as const

const STATUS_OPTIONS = ["All", "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"] as const

const TYPE_COLORS: Record<string, string> = {
  Workshop: "bg-primary/10 text-primary",
  "Lab Session": "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  "Practical Demo": "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
  "Hands-On": "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  "Field Lab": "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
  "Studio Session": "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
}

const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  IN_PROGRESS: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  COMPLETED: "bg-muted text-muted-foreground",
  CANCELLED: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
}

function TypeBadge({ type }: { type: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${TYPE_COLORS[type] || "bg-muted text-muted-foreground"}`}>
      {type}
    </span>
  )
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[status] || "bg-muted text-muted-foreground"}`}>
      {status === "IN_PROGRESS" && <span className="mr-1 size-1.5 rounded-full bg-emerald-500 animate-pulse" />}
      {status.replace(/_/g, " ")}
    </span>
  )
}

const emptyForm = {
  title: "",
  workshopType: "Workshop",
  description: "",
  scheduledAt: "",
  durationMinutes: 60,
  location: "",
  materialsUrl: "",
}

export default function WorkshopsPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [sessions, setSessions] = useState<WorkshopSession[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [typeFilter, setTypeFilter] = useState<string>("All")
  const [statusFilter, setStatusFilter] = useState<string>("All")
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const loadSessions = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      setError(null)
      const studentId = user.id || ""
      const res = await collegeApi.getLearnerWorkshops(studentId)
      setSessions(res.data || [])
    } catch {
      setError(tc("error"))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (!user) return
    loadSessions()
  }, [user, loadSessions])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!user) return
    if (!form.title.trim()) return
    try {
      setSubmitting(true)
      setError(null)
      const dto = {
        title: form.title.trim(),
        workshopType: form.workshopType,
        description: form.description.trim() || undefined,
        scheduledAt: form.scheduledAt || undefined,
        durationMinutes: form.durationMinutes || undefined,
        location: form.location.trim() || undefined,
        materialsUrl: form.materialsUrl.trim() || undefined,
        studentId: user.id,
        institutionId: user.institutionId || "",
        status: "SCHEDULED",
      }
      await collegeApi.createWorkshop(dto)
      setForm(emptyForm)
      setShowForm(false)
      await loadSessions()
    } catch {
      setError(tc("error"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(tc("confirm"))) return
    try {
      setDeletingId(id)
      setError(null)
      await collegeApi.deleteWorkshop(id)
      setSessions((prev) => prev.filter((s) => s.id !== id))
    } catch {
      setError(tc("error"))
    } finally {
      setDeletingId(null)
    }
  }

  function formatScheduledAt(dateStr: string) {
    try {
      const d = new Date(dateStr)
      return d.toLocaleDateString("en-US", {
        weekday: "short",
        year: "numeric",
        month: "short",
        day: "numeric",
      }) + " at " + d.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
      })
    } catch {
      return dateStr
    }
  }

  const filtered = sessions.filter((s) => {
    if (typeFilter !== "All" && s.workshopType !== typeFilter) return false
    if (statusFilter !== "All" && s.status !== statusFilter) return false
    return true
  })

  const totalSessions = sessions.length
  const scheduledCount = sessions.filter((s) => s.status === "SCHEDULED").length
  const inProgressCount = sessions.filter((s) => s.status === "IN_PROGRESS").length
  const completedCount = sessions.filter((s) => s.status === "COMPLETED").length

  if (authLoading || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-start justify-between">
        <LearnerHeader
          firstName={firstName}
          subtitle={t("subtitle.workshops")}
        />
        <button
          onClick={() => setShowForm(!showForm)}
          aria-label={t("workshop")}
          aria-pressed={showForm}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs transition hover:bg-primary/90"
        >
          <Plus className="size-4" />
          {t("workshop")}
        </button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
          <button onClick={() => setError(null)} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h3 className="font-semibold text-foreground">{t("workshop")}</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground">{t("courses")} *</label>
              <input
                type="text"
                required
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                placeholder="e.g. Chemistry Lab: Titration"
                aria-label={t("courses")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground">{t("department")}</label>
              <select
                value={form.workshopType}
                onChange={(e) => setForm((f) => ({ ...f, workshopType: e.target.value }))}
                aria-label={t("department")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                {WORKSHOP_TYPES.map((wt) => (
                  <option key={wt} value={wt}>{wt}</option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground">{t("academicYear")}</label>
              <input
                type="datetime-local"
                value={form.scheduledAt}
                onChange={(e) => setForm((f) => ({ ...f, scheduledAt: e.target.value }))}
                aria-label={t("academicYear")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground">{t("creditHours")}</label>
              <input
                type="number"
                min={0}
                value={form.durationMinutes}
                onChange={(e) => setForm((f) => ({ ...f, durationMinutes: Number(e.target.value) }))}
                aria-label={t("creditHours")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground">{t("location")}</label>
              <input
                type="text"
                value={form.location}
                onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))}
                placeholder="e.g. Lab Room 204"
                aria-label={t("location")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground">{t("download")}</label>
              <input
                type="url"
                value={form.materialsUrl}
                onChange={(e) => setForm((f) => ({ ...f, materialsUrl: e.target.value }))}
                placeholder="https://..."
                aria-label={t("download")}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-muted-foreground">{t("description")}</label>
            <textarea
              rows={3}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              placeholder="Describe the workshop session..."
              aria-label={t("description")}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
            />
          </div>
          <div className="flex items-center gap-3">
            <button
              type="submit"
              disabled={submitting || !form.title.trim()}
              aria-label={tc("submit")}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs transition hover:bg-primary/90 disabled:opacity-50"
            >
              {submitting ? tc("loading") : tc("submit")}
            </button>
            <button
              type="button"
              onClick={() => { setShowForm(false); setForm(emptyForm) }}
              aria-label={tc("cancel")}
              className="rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition hover:bg-muted"
            >
              {tc("cancel")}
            </button>
          </div>
        </form>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <FlaskConical className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.total")}</p>
              <p className="text-2xl font-extrabold text-foreground">{totalSessions}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <Calendar className="size-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("filters.upcoming")}</p>
              <p className="text-2xl font-extrabold text-foreground">{scheduledCount}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <Clock className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.inProgress")}</p>
              <p className="text-2xl font-extrabold text-foreground">{inProgressCount}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-muted">
              <CheckCircle className="size-5 text-muted-foreground" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.completed")}</p>
              <p className="text-2xl font-extrabold text-foreground">{completedCount}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-muted-foreground">{t("department")}:</span>
        <button
          onClick={() => setTypeFilter("All")}
          aria-pressed={typeFilter === "All"}
          aria-label={tc("filter")}
          className={`rounded-full px-3 py-1 text-xs font-medium transition ${typeFilter === "All" ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}
        >
          {tc("filter")}
        </button>
        {WORKSHOP_TYPES.map((wt) => (
          <button
            key={wt}
            onClick={() => setTypeFilter(wt)}
            aria-pressed={typeFilter === wt}
            aria-label={wt}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${typeFilter === wt ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}
          >
            {wt}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-muted-foreground">{t("status")}:</span>
        {STATUS_OPTIONS.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            aria-pressed={statusFilter === s}
            aria-label={s === "All" ? tc("filter") : s.replace(/_/g, " ")}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${statusFilter === s ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}
          >
            {s === "All" ? tc("filter") : s.replace(/_/g, " ")}
          </button>
        ))}
      </div>

      {sessions.length === 0 ? (
        <EmptyState
          icon={<FlaskConical className="size-8" />}
          title={t("empty.noModules")}
          description={t("empty.noModules")}
          action={
            <button
              onClick={() => setShowForm(true)}
              aria-label={t("workshop")}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="size-4" />
              {t("workshop")}
            </button>
          }
        />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<FlaskConical className="size-8" />}
          title={tc("noResults")}
          description={tc("noResults")}
        />
      ) : (
        <div className="space-y-4">
          {filtered.map((session) => (
            <div key={session.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="space-y-1 min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold text-foreground">{session.title}</h3>
                    <TypeBadge type={session.workshopType} />
                    <StatusBadge status={session.status} />
                  </div>
                  {session.description && (
                    <p className="line-clamp-2 text-sm text-muted-foreground">{session.description}</p>
                  )}
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {session.materialsUrl && (
                    <a
                      href={session.materialsUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      aria-label={t("download")}
                      className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground transition hover:bg-muted"
                    >
                      <ExternalLink className="size-3" />
                      {t("download")}
                    </a>
                  )}
                  <button
                    onClick={() => handleDelete(session.id)}
                    disabled={deletingId === session.id}
                    aria-label={tc("delete")}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-1.5 text-xs font-medium text-destructive transition hover:bg-destructive/10 disabled:opacity-50"
                  >
                    <Trash2 className="size-3" />
                    {deletingId === session.id ? tc("loading") : tc("delete")}
                  </button>
                </div>
              </div>
              <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                {session.scheduledAt && (
                  <span className="inline-flex items-center gap-1.5">
                    <Calendar className="size-3.5" />
                    {formatScheduledAt(session.scheduledAt)}
                  </span>
                )}
                {session.durationMinutes != null && session.durationMinutes > 0 && (
                  <span className="inline-flex items-center gap-1.5">
                    <Clock className="size-3.5" />
                    {session.durationMinutes} min
                  </span>
                )}
                {session.location && (
                  <span className="inline-flex items-center gap-1.5">
                    <MapPin className="size-3.5" />
                    {session.location}
                  </span>
                )}
                {(session.maxParticipants != null || session.currentParticipants != null) && (
                  <span className="inline-flex items-center gap-1.5">
                    <Users className="size-3.5" />
                    {session.currentParticipants ?? 0}
                    {session.maxParticipants != null && ` / ${session.maxParticipants}`} participants
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
