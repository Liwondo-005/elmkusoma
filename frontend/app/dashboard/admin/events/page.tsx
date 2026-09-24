"use client"

import { useEffect, useState, useCallback } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import {
  Calendar, Plus, Search, Filter, Loader2, ChevronLeft, ChevronRight,
  Edit, Trash2, Eye, Play, Square, Video, MoreHorizontal, CheckSquare,
  XCircle, AlertCircle
} from "lucide-react"
import { adminApi, getInstitutionId } from "@/lib/api"
import { cn } from "@/lib/utils"

type EventStatus = "DRAFT" | "PUBLISHED" | "LIVE" | "ENDED" | "CANCELLED"

interface AdminEvent {
  id: string
  title: string
  description?: string
  eventType: string
  category?: string
  status: EventStatus
  startsAt: string
  endsAt?: string
  durationMinutes?: number
  maxParticipants?: number
  registeredCount?: number
  availableSpots?: number
  presenterName?: string
  recordingUrl?: string
  meetingUrl?: string
  institutionId?: string
  createdAt: string
}

const statusConfig: Record<EventStatus, { label: string; className: string }> = {
  DRAFT: { label: "Draft", className: "bg-gray-100 text-gray-700 border border-gray-200" },
  PUBLISHED: { label: "Published", className: "bg-blue-100 text-blue-700 border border-blue-200" },
  LIVE: { label: "Live", className: "bg-green-100 text-green-700 border border-green-200 animate-pulse" },
  ENDED: { label: "Ended", className: "bg-gray-100 text-gray-500 border border-gray-200" },
  CANCELLED: { label: "Cancelled", className: "bg-red-100 text-red-700 border border-red-200" },
}

const ITEMS_PER_PAGE = 10

export default function AdminEventsPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const router = useRouter()

  const [events, setEvents] = useState<AdminEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("ALL")
  const [typeFilter, setTypeFilter] = useState<string>("ALL")
  const [page, setPage] = useState(0)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const loadEvents = useCallback(async () => {
    const institutionId = getInstitutionId()
    if (!institutionId) {
      setError("No institution context found")
      setLoading(false)
      return
    }
    try {
      setLoading(true)
      setError(null)
      const data = await adminApi.getEvents(institutionId)
      setEvents(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("error.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadEvents()
  }, [loadEvents])

  async function handleAction(action: string, eventId: string) {
    setActionLoading(eventId)
    try {
      switch (action) {
        case "publish":
          await adminApi.publishEvent(eventId)
          break
        case "cancel":
          await adminApi.cancelEvent(eventId)
          break
        case "delete":
          if (!window.confirm(t("confirm.deleteMessage"))) return
          await adminApi.deleteEvent(eventId)
          break
        case "startLive":
          await adminApi.startLiveEvent(eventId)
          break
        case "endLive":
          await adminApi.endLiveEvent(eventId)
          break
      }
      loadEvents()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Action failed")
    } finally {
      setActionLoading(null)
    }
  }

  async function handleBulkAction(action: "publish" | "cancel") {
    if (selectedIds.size === 0) return
    setActionLoading("bulk")
    try {
      for (const id of selectedIds) {
        if (action === "publish") await adminApi.publishEvent(id)
        else await adminApi.cancelEvent(id)
      }
      setSelectedIds(new Set())
      loadEvents()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bulk action failed")
    } finally {
      setActionLoading(null)
    }
  }

  function toggleSelect(id: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function toggleSelectAll() {
    const filtered = getFilteredEvents()
    const paged = filtered.slice(page * ITEMS_PER_PAGE, (page + 1) * ITEMS_PER_PAGE)
    if (selectedIds.size === paged.length) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(new Set(paged.map((e) => e.id)))
    }
  }

  function getFilteredEvents() {
    return events.filter((e) => {
      const matchesSearch = !search || e.title.toLowerCase().includes(search.toLowerCase())
      const matchesStatus = statusFilter === "ALL" || e.status === statusFilter
      const matchesType = typeFilter === "ALL" || e.eventType === typeFilter
      return matchesSearch && matchesStatus && matchesType
    })
  }

  const filtered = getFilteredEvents()
  const totalPages = Math.ceil(filtered.length / ITEMS_PER_PAGE)
  const paged = filtered.slice(page * ITEMS_PER_PAGE, (page + 1) * ITEMS_PER_PAGE)

  const eventTypes = [...new Set(events.map((e) => e.eventType))]

  return (
    <div className="mx-auto max-w-7xl space-y-6" role="main" aria-label={t("admin.title")}>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("admin.title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("admin.subtitle")}</p>
        </div>
        <Link
          href="/dashboard/admin/events/new"
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90"
          aria-label={t("admin.createEvent")}
        >
          <Plus className="size-4" />
          {t("admin.createEvent")}
        </Link>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 flex items-center gap-3">
          <AlertCircle className="size-5 text-destructive shrink-0" />
          <p className="text-sm font-medium text-destructive flex-1">{error}</p>
          <button onClick={loadEvents} className="text-sm font-medium text-destructive hover:underline" aria-label={tc("retry")}>
            {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && events.length === 0 && (
        <div className="rounded-2xl border border-border bg-card px-6 py-16 text-center">
          <Calendar className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("admin.empty.noEvents")}</h3>
          <p className="mt-1 text-sm text-muted-foreground">{t("admin.empty.noEventsDesc")}</p>
          <Link
            href="/dashboard/admin/events/new"
            className="mt-6 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90"
            aria-label={t("admin.createEvent")}
          >
            <Plus className="size-4" />
            {t("admin.createEvent")}
          </Link>
        </div>
      )}

      {!loading && !error && events.length > 0 && (
        <>
          {/* Filters */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder={t("admin.searchPlaceholder")}
                value={search}
                onChange={(e) => { setSearch(e.target.value); setPage(0) }}
                className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                aria-label={t("admin.searchPlaceholder")}
              />
            </div>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}
              className="rounded-xl border border-border bg-card px-3 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.filterByStatus")}
            >
              <option value="ALL">{t("admin.allStatuses")}</option>
              {Object.entries(statusConfig).map(([key, { label }]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
            <select
              value={typeFilter}
              onChange={(e) => { setTypeFilter(e.target.value); setPage(0) }}
              className="rounded-xl border border-border bg-card px-3 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.filterByType")}
            >
              <option value="ALL">{t("admin.allTypes")}</option>
              {eventTypes.map((type) => (
                <option key={type} value={type}>{t(`admin.types.${type}` as any)}</option>
              ))}
            </select>
          </div>

          {/* Bulk actions */}
          {selectedIds.size > 0 && (
            <div className="flex items-center gap-3 rounded-xl border border-primary/20 bg-primary/5 px-4 py-3">
              <span className="text-sm font-medium text-foreground">{selectedIds.size} selected</span>
              <button
                onClick={() => handleBulkAction("publish")}
                disabled={!!actionLoading}
                className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
                aria-label={t("admin.bulkActions.publishSelected")}
              >
                <Play className="size-3" />
                {t("admin.bulkActions.publishSelected")}
              </button>
              <button
                onClick={() => handleBulkAction("cancel")}
                disabled={!!actionLoading}
                className="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                aria-label={t("admin.bulkActions.cancelSelected")}
              >
                <XCircle className="size-3" />
                {t("admin.bulkActions.cancelSelected")}
              </button>
              <button
                onClick={() => setSelectedIds(new Set())}
                className="text-xs font-medium text-muted-foreground hover:text-foreground"
                aria-label={tc("cancel")}
              >
                {tc("cancel")}
              </button>
            </div>
          )}

          {/* Table */}
          <div className="rounded-2xl border border-border bg-card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-border bg-muted/50">
                    <th className="px-4 py-3 text-left">
                      <button onClick={toggleSelectAll} className="text-muted-foreground hover:text-foreground" aria-label="Select all">
                        <CheckSquare className={cn("size-4", selectedIds.size === paged.length && paged.length > 0 ? "text-primary" : "")} />
                      </button>
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.title")}</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.type")}</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.status")}</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.date")}</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.registrations")}</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.recording")}</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-muted-foreground uppercase">{t("admin.columns.actions")}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {paged.map((event) => (
                    <tr key={event.id} className="hover:bg-muted/30 transition-colors">
                      <td className="px-4 py-3">
                        <button onClick={() => toggleSelect(event.id)} aria-label={`Select ${event.title}`}>
                          <CheckSquare className={cn("size-4", selectedIds.has(event.id) ? "text-primary" : "text-muted-foreground/50")} />
                        </button>
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-sm font-medium text-foreground">{event.title}</p>
                        {event.presenterName && (
                          <p className="text-xs text-muted-foreground">{event.presenterName}</p>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
                          {t(`admin.types.${event.eventType}` as any)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={cn("inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium", statusConfig[event.status]?.className)}>
                          {event.status === "LIVE" && <span className="mr-1 size-1.5 rounded-full bg-green-600 animate-pulse" />}
                          {t(`admin.status.${event.status}` as any)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-sm text-foreground">{new Date(event.startsAt).toLocaleDateString()}</p>
                        <p className="text-xs text-muted-foreground">{new Date(event.startsAt).toLocaleTimeString()}</p>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-sm text-foreground">
                          {event.registeredCount ?? 0}{event.maxParticipants ? `/${event.maxParticipants}` : ""}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        {event.recordingUrl ? (
                          <span className="inline-flex items-center gap-1 text-xs text-emerald-600">
                            <Video className="size-3" />
                            Available
                          </span>
                        ) : (
                          <span className="text-xs text-muted-foreground">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Link
                            href={`/dashboard/admin/events/${event.id}/edit`}
                            className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
                            aria-label={t("admin.actions.edit")}
                          >
                            <Edit className="size-3.5" />
                          </Link>
                          <Link
                            href={`/dashboard/admin/events/${event.id}/summary`}
                            className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
                            aria-label={t("admin.actions.viewSummary")}
                          >
                            <Eye className="size-3.5" />
                          </Link>
                          {event.status === "DRAFT" && (
                            <button
                              onClick={() => handleAction("publish", event.id)}
                              disabled={actionLoading === event.id}
                              className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-emerald-600 hover:bg-emerald-50 disabled:opacity-50"
                              aria-label={t("admin.actions.publish")}
                            >
                              <Play className="size-3.5" />
                            </button>
                          )}
                          {event.status === "PUBLISHED" && (
                            <button
                              onClick={() => handleAction("startLive", event.id)}
                              disabled={actionLoading === event.id}
                              className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-green-600 hover:bg-green-50 disabled:opacity-50"
                              aria-label={t("admin.actions.startLive")}
                            >
                              <Play className="size-3.5" />
                            </button>
                          )}
                          {event.status === "LIVE" && (
                            <button
                              onClick={() => handleAction("endLive", event.id)}
                              disabled={actionLoading === event.id}
                              className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-orange-600 hover:bg-orange-50 disabled:opacity-50"
                              aria-label={t("admin.actions.endLive")}
                            >
                              <Square className="size-3.5" />
                            </button>
                          )}
                          {event.recordingUrl && (
                            <a
                              href={event.recordingUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-50"
                              aria-label={t("admin.actions.viewRecording")}
                            >
                              <Video className="size-3.5" />
                            </a>
                          )}
                          {event.status !== "LIVE" && (
                            <button
                              onClick={() => handleAction("cancel", event.id)}
                              disabled={actionLoading === event.id}
                              className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
                              aria-label={t("admin.actions.cancel")}
                            >
                              <XCircle className="size-3.5" />
                            </button>
                          )}
                          <button
                            onClick={() => handleAction("delete", event.id)}
                            disabled={actionLoading === event.id}
                            className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
                            aria-label={t("admin.actions.delete")}
                          >
                            <Trash2 className="size-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {paged.length === 0 && (
                    <tr>
                      <td colSpan={8} className="px-4 py-10 text-center text-sm text-muted-foreground">
                        {t("admin.empty.noEvents")}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                {t("admin.pagination.showing")} {page * ITEMS_PER_PAGE + 1}–{Math.min((page + 1) * ITEMS_PER_PAGE, filtered.length)} {t("admin.pagination.of")} {filtered.length} {t("admin.pagination.events")}
              </p>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted disabled:opacity-50"
                  aria-label={t("admin.pagination.previous")}
                >
                  <ChevronLeft className="size-4" />
                  {t("admin.pagination.previous")}
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted disabled:opacity-50"
                  aria-label={t("admin.pagination.next")}
                >
                  {t("admin.pagination.next")}
                  <ChevronRight className="size-4" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
