"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { CalendarDays, Search, AlertCircle, RefreshCw, Filter, Clock, Users, Archive, Loader2 } from "lucide-react"
import { platformAdminApi, type PageResponse } from "@/lib/platform-admin-api"

interface PlatformEvent {
  id: string
  title: string
  status?: string
  eventType?: string
  startsAt?: string
  institutionId?: string
  createdAt?: string
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
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [pageIndex, setPageIndex] = useState(0)
  const [acting, setActing] = useState<string | null>(null)

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

  const archiveEvent = async (id: string) => {
    setActing(id); setError(null)
    try {
      await platformAdminApi.bulkContentAction("EVENT", "ARCHIVE", [id])
      await load()
    } catch (e: any) {
      setError(e.message || t("events.failedToArchiveEvent"))
    } finally { setActing(null) }
  }

  const filtered = (page?.content ?? []).filter((ev) => {
    if (statusFilter !== "all" && (ev.status ?? "").toLowerCase() !== statusFilter) return false
    return true
  })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
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

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("events.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <Skeleton /> : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <CalendarDays className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("events.noPlatformEventsYet")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{search || statusFilter !== "all" ? t("events.tryADifferentSearch") : t("events.noEventsExistYet")}</p>
          </div>
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {filtered.map((ev) => (
              <div key={ev.id} className="rounded-xl border border-border bg-card p-4 hover:shadow-sm transition-shadow">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold text-foreground line-clamp-1">{ev.title}</h3>
                  <div className="flex shrink-0 items-center gap-1.5">
                    {ev.status && <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium capitalize">{ev.status.toLowerCase()}</span>}
                    <button
                      onClick={() => archiveEvent(ev.id)}
                      disabled={acting === ev.id}
                      className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2 py-0.5 text-[11px] font-semibold hover:bg-muted disabled:opacity-50"
                      title="Archive event"
                    >
                      {acting === ev.id ? <Loader2 className="size-3 animate-spin" /> : <Archive className="size-3" />} {t("events.archive")}</button>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
                  {ev.startsAt && <span className="inline-flex items-center gap-1"><Clock className="size-3" />{new Date(ev.startsAt).toLocaleDateString()}</span>}
                  {ev.eventType && <span className="inline-flex items-center gap-1"><Users className="size-3" />{ev.eventType}</span>}
                  {ev.institutionId && <span className="text-[10px] font-mono">{ev.institutionId.slice(0, 8)}…</span>}
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
          <span>{t("events.pageOf", { p0: pageIndex + 1, p1: Math.max(page?.totalPages ?? 1, 1) })}</span>
          <div className="flex gap-2">
            <button disabled={pageIndex === 0} onClick={() => setPageIndex(pageIndex - 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{t("events.prev")}</button>
            <button disabled={(page?.totalPages ?? 1) <= pageIndex + 1} onClick={() => setPageIndex(pageIndex + 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{tc("next")}</button>
          </div>
        </div>
      </div>
    </div>
  )
}
