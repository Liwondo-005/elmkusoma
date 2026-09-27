"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { LifeBuoy, RefreshCw, AlertCircle, Clock, UserCheck, Search, ChevronRight } from "lucide-react"
import { platformAdminApi, type PageResponse, type SupportTicket } from "@/lib/platform-admin-api"

const LIFECYCLE = ["OPEN", "ASSIGNED", "INVESTIGATING", "ACTION_REQUIRED", "RESOLVED", "CLOSED"] as const
type TicketStatus = (typeof LIFECYCLE)[number]

const NEXT: Record<string, string[]> = {
  OPEN: ["ASSIGNED", "INVESTIGATING", "ACTION_REQUIRED", "RESOLVED"],
  ASSIGNED: ["INVESTIGATING", "ACTION_REQUIRED", "RESOLVED", "OPEN"],
  INVESTIGATING: ["ACTION_REQUIRED", "RESOLVED"],
  ACTION_REQUIRED: ["INVESTIGATING", "RESOLVED"],
  RESOLVED: ["CLOSED", "INVESTIGATING", "ACTION_REQUIRED"],
  CLOSED: ["OPEN"],
}

function StatusBadge({ s }: { s: string }) {
  const map: Record<string, string> = {
    OPEN: "bg-blue-500/10 text-blue-700 border-blue-200",
    ASSIGNED: "bg-violet-500/10 text-violet-700 border-violet-200",
    INVESTIGATING: "bg-amber-500/10 text-amber-700 border-amber-200",
    ACTION_REQUIRED: "bg-orange-500/10 text-orange-700 border-orange-200",
    RESOLVED: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
    CLOSED: "bg-slate-500/10 text-slate-600 border-slate-200",
  }
  return <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${map[s] ?? "bg-muted text-muted-foreground"}`}>{s}</span>
}

export default function PlatformSupportPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [page, setPage] = useState<PageResponse<SupportTicket> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<string>("")
  const [search, setSearch] = useState("")
  const [pageIndex, setPageIndex] = useState(0)
  const [updating, setUpdating] = useState<string | null>(null)
  const [adminId, setAdminId] = useState("")
  const [assigning, setAssigning] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listSupportTickets(pageIndex, 20, filter === "all" ? undefined : filter || undefined)
      setPage(res)
    } catch (e: any) {
      setError(e.message || t("support.failedToLoadSupport"))
      setPage(null)
    } finally { setLoading(false) }
  }, [pageIndex, filter])

  useEffect(() => { load() }, [load])

  const advance = async (ticket: SupportTicket, target: string) => {
    setUpdating(ticket.id)
    try {
      await platformAdminApi.updateSupportTicketStatus(ticket.id, target)
      await load()
    } catch (e: any) {
      setError(e.message || t("support.failedToUpdateTicket"))
    } finally { setUpdating(null) }
  }

  const assign = async (ticket: SupportTicket) => {
    if (!adminId.trim()) { setError(t("support.enterAnAdminUser")); return }
    setAssigning(ticket.id); setError(null)
    try {
      await platformAdminApi.assignSupportTicket(ticket.id, adminId.trim())
      await load()
    } catch (e: any) {
      setError(e.message || t("support.failedToAssignTicket"))
    } finally { setAssigning(null) }
  }

  const rows = (page?.content ?? []).filter((t) =>
    !search || `${t.title} ${t.description ?? ""}`.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-teal-600 text-white"><LifeBuoy className="size-4" /></span> {t("support.supportCases")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("support.ticketLifecycle")}<span className="font-mono text-xs">{t("support.openAssignedInvestigatingAction")}</span>{t("support.platformWideSupportTickets")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("support.refresh")}</button>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={t("support.searchTickets")} className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <select value={filter} onChange={(e) => { setFilter(e.target.value); setPageIndex(0) }} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
            <option value="">{t("support.allStatuses")}</option>
            {LIFECYCLE.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <input
            value={adminId}
            onChange={(e) => setAdminId(e.target.value)}
            placeholder={t("support.assigneeAdminUserId")}
            className="rounded-xl border border-border bg-background px-3 py-2.5 text-xs font-mono outline-none focus:ring-2 focus:ring-ring sm:w-56"
          />
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("support.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <div className="animate-pulse space-y-3"><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /></div> : rows.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <LifeBuoy className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("support.noSupportTicketsFound")}</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">{t("support.ticketsCreatedByParents")}</p>
          </div>
        ) : (
          <div className="space-y-2">
            {rows.map((row) => (
              <div key={row.id} className="flex items-start justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/30">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2"><StatusBadge s={row.status} />{row.priority && <span className="rounded-full bg-muted px-2 py-0.5 text-xs">{row.priority}</span>}{row.category && <span className="rounded-full bg-muted px-2 py-0.5 text-xs">{row.category}</span>}</div>
                  <p className="mt-1 font-medium text-foreground truncate">{row.title}</p>
                  {row.description && <p className="text-xs text-muted-foreground line-clamp-2">{row.description}</p>}
                  <p className="mt-1 flex flex-wrap gap-2 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1"><Clock className="size-3" />{new Date(row.createdAt).toLocaleDateString()}</span>
                    {row.assignedTo && <span className="inline-flex items-center gap-1"><UserCheck className="size-3" />{row.assignedTo}</span>}
                    {row.resolvedAt && <span>{t("support.resolved", { p0: new Date(row.resolvedAt).toLocaleDateString() })}</span>}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <button disabled={assigning === row.id || !adminId.trim()} onClick={() => assign(row)}
                    className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50">
                    <UserCheck className="size-3" /> {t("support.assign")}</button>
                  {(NEXT[row.status] ?? []).map((s) => (
                    <button key={s} disabled={updating === row.id} onClick={() => advance(row, s)}
                      className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50">
                      {s} <ChevronRight className="size-3" />
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
        {page && page.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <button disabled={page.first} onClick={() => setPageIndex(i => i - 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">{tc("previous")}</button>
            <span className="text-xs text-muted-foreground">{t("support.pageOfTickets", { p0: page.page + 1, p1: page.totalPages, p2: page.totalElements })}</span>
            <button disabled={page.last} onClick={() => setPageIndex(i => i + 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">{tc("next")}</button>
          </div>
        )}
      </div>
    </div>
  )
}
