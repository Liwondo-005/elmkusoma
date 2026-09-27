"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { CalendarDays, Search, AlertCircle, RefreshCw, Filter, Clock, Users, Archive, Loader2, Plus, Pencil, Trash2, Megaphone, Ban, CheckCircle2 } from "lucide-react"
import { platformAdminApi, type PageResponse } from "@/lib/platform-admin-api"
import { EventFormModal } from "@/components/platform-admin/event-form-modal"

interface PlatformEvent {
  id: string
  title: string
  status?: string
  eventType?: string
  eventStatus?: string
  startsAt?: string
  institutionId?: string
  createdAt?: string
  category?: string
  location?: string
  meetingUrl?: string
  durationMinutes?: number
  maxParticipants?: number
  thumbnailUrl?: string
  tags?: string
  isFree?: boolean
  requiresApproval?: boolean
  timezone?: string
  accessLevel?: string
  presenterName?: string
  eventFormat?: string
  difficulty?: string
  targetAudience?: string
  prerequisites?: string
  learningOutcomes?: string
  agenda?: string
  relatedCourseId?: string
  relatedModuleId?: string
  relatedLessonId?: string
  providerId?: string
}

function Skeleton() {
  return <div className="animate-pulse space-y-3"><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /></div>
}

export default function PlatformEventsPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [page, setPage] = useState<PageResponse<PlatformEvent> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [pageIndex, setPageIndex] = useState(0)
  const [acting, setActing] = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<PlatformEvent | null>(null)

  const flash = (msg: string) => {
    setSuccess(msg)
    setTimeout(() => setSuccess(null), 5000)
  }

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listPlatformEvents(pageIndex, 20, search || undefined)
      setPage(res)
    } catch (e: any) {
      setError(e.message || t("events.failedToLoadEvents"))
    } finally { setLoading(false) }
  }, [pageIndex, search])

  useEffect(() => { const t = setTimeout(load, 300); return () => clearTimeout(t) }, [load])

  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setPageIndex(0); setSearch(search) }

  const handleSaved = async (message: string) => {
    flash(message)
    await load()
  }

  const handleToggleStatus = async (ev: PlatformEvent, newStatus: string) => {
    setActing(ev.id)
    setError(null)
    try {
      if (newStatus === "PUBLISHED") {
        await platformAdminApi.publishEvent(ev.id)
      } else if (newStatus === "CANCELLED") {
        await platformAdminApi.cancelEvent(ev.id)
      } else {
        throw new Error("Unsupported status")
      }
      flash(`Event ${newStatus.toLowerCase()}`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${newStatus.toLowerCase()} event`)
    } finally {
      setActing(null)
    }
  }

  const handleDelete = async (ev: PlatformEvent) => {
    if (!window.confirm(`Delete "${ev.title}"? This cannot be undone.`)) return
    setActing(ev.id)
    setError(null)
    try {
      await platformAdminApi.deleteEvent(ev.id)
      flash(`Event "${ev.title}" deleted`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete event")
    } finally {
      setActing(null)
    }
  }

  const statusOptions: Record<string, string[]> = {
    DRAFT: ["PUBLISHED", "CANCELLED"],
    SCHEDULED: ["PUBLISHED", "CANCELLED"],
    PUBLISHED: ["CANCELLED", "COMPLETED"],
    LIVE: ["COMPLETED", "CANCELLED"],
    COMPLETED: ["ARCHIVED"],
    CANCELLED: [],
    ARCHIVED: [],
  }

  const filtered = (page?.content ?? []).filter((ev) => {
    if (statusFilter === "all") return true
    return (ev.status ?? "").toLowerCase() === statusFilter
  })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-blue-500 text-white"><CalendarDays className="size-4" /></span> {t("events.platformEvents")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("events.eventsAcrossThePlatform")}</p>
        </div>
        <button
          onClick={() => { setEditing(null); setModalOpen(true) }}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {t("events.addEvent")}
        </button>
      </div>

      {success && (
        <div className="flex items-center gap-2 rounded-2xl border border-green-500/20 bg-green-500/5 px-6 py-4 text-sm text-green-600">
          <CheckCircle2 className="size-4 shrink-0" /> {success}
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("events.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-blue-500 text-white"><CalendarDays className="size-4" /></span> {t("events.platformEvents")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("events.eventsAcrossThePlatform")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("events.refresh")}</button>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={t("events.searchEventsByTitle")} className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-muted-foreground" />
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
              <option value="all">{t("events.allStatus")}</option>
              <option value="published">{t("events.published")}</option>
              <option value="scheduled">{ts("scheduled")}</option>
              <option value="cancelled">{ts("cancelled")}</option>
              <option value="completed">{ts("completed")}</option>
            </select>
          </div>
        </div>
      </div>

      {loading ? <Skeleton /> : filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <CalendarDays className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("events.noPlatformEventsYet")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{search || statusFilter !== "all" ? t("events.tryADifferentSearch") : t("events.noEventsExistYet")}</p>
          </div>
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {filtered.map((ev) => (
            <div key={ev.id} className="rounded-xl border border-border bg-card p-4 hover:shadow-sm transition-shadow">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold text-foreground line-clamp-1">{ev.title}</h3>
                <div className="flex shrink-0 items-center gap-1.5">
                  {ev.status && <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium capitalize">{ev.status.toLowerCase()}</span>}
                  <select
                    value=""
                    disabled={acting === ev.id}
                    onChange={(e) => { if (e.target.value) handleToggleStatus(ev, e.target.value) }}
                    className="rounded-lg border border-border bg-background px-2 py-1 text-[11px] font-semibold outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                  >
                    <option value="">Actions…</option>
                    {(statusOptions[ev.status ?? "DRAFT"] ?? []).map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                  <button
                    onClick={() => { setEditing(ev); setModalOpen(true) }}
                    className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2 py-0.5 text-[11px] font-semibold hover:bg-muted"
                    title="Edit event"
                  >
                    <Pencil className="size-3" /> Edit
                  </button>
                  <button
                    onClick={() => handleDelete(ev)}
                    disabled={acting === ev.id}
                    className="inline-flex items-center gap-1 rounded-lg border border-red-200 bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-600 hover:bg-red-100 disabled:opacity-50"
                    title="Delete event"
                  >
                    {acting === ev.id ? <Loader2 className="size-3 animate-spin" /> : <Trash2 className="size-3" />} Delete
                  </button>
                </div>
              </div>
              <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
                {ev.startsAt && <span className="inline-flex items-center gap-1"><Clock className="size-3" />{new Date(ev.startsAt).toLocaleDateString()}</span>}
                {ev.eventType && <span className="inline-flex items-center gap-1"><Users className="size-3" />{ev.eventType}</span>}
                {ev.category && <span className="inline-flex items-center gap-1"><span className="rounded-full bg-primary/10 px-2 py-0.5 text-primary">{ev.category}</span></span>}
                {ev.institutionId && <span className="text-[10px] font-mono">{ev.institutionId.slice(0, 8)}…</span>}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
          <span>{t("events.pageOf", { p0: pageIndex + 1, p1: Math.max(page?.totalPages ?? 1, 1) })}</span>
          <div className="flex gap-2">
            <button disabled={pageIndex === 0} onClick={() => setPageIndex(pageIndex - 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{t("events.prev")}</button>
            <button disabled={(page?.totalPages ?? 1) <= pageIndex + 1} onClick={() => setPageIndex(pageIndex + 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{tc("next")}</button>
          </div>
        </div>
      </div>

      <EventFormModal
        open={modalOpen}
        event={editing}
        onClose={() => setModalOpen(false)}
        onSaved={handleSaved}
      />
    </div>
  )
}