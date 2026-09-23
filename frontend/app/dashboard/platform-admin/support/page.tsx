"use client"

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
      setError(e.message || "Failed to load support tickets")
      setPage(null)
    } finally { setLoading(false) }
  }, [pageIndex, filter])

  useEffect(() => { load() }, [load])

  const advance = async (t: SupportTicket, target: string) => {
    setUpdating(t.id)
    try {
      await platformAdminApi.updateSupportTicketStatus(t.id, target)
      await load()
    } catch (e: any) {
      setError(e.message || "Failed to update ticket")
    } finally { setUpdating(null) }
  }

  const assign = async (t: SupportTicket) => {
    if (!adminId.trim()) { setError("Enter an admin user ID to assign"); return }
    setAssigning(t.id); setError(null)
    try {
      await platformAdminApi.assignSupportTicket(t.id, adminId.trim())
      await load()
    } catch (e: any) {
      setError(e.message || "Failed to assign ticket")
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
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-teal-600 text-white"><LifeBuoy className="size-4" /></span> Support & Cases</h1>
            <p className="mt-1 text-sm text-muted-foreground">Ticket lifecycle: <span className="font-mono text-xs">OPEN → ASSIGNED → INVESTIGATING → ACTION_REQUIRED → RESOLVED → CLOSED</span>. Platform-wide support tickets with status transitions.</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search tickets..." className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <select value={filter} onChange={(e) => { setFilter(e.target.value); setPageIndex(0) }} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
            <option value="">All statuses</option>
            {LIFECYCLE.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <input
            value={adminId}
            onChange={(e) => setAdminId(e.target.value)}
            placeholder="Assignee admin user ID"
            className="rounded-xl border border-border bg-background px-3 py-2.5 text-xs font-mono outline-none focus:ring-2 focus:ring-ring sm:w-56"
          />
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <div className="animate-pulse space-y-3"><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /></div> : rows.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <LifeBuoy className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">No support tickets found</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">Tickets created by parents and users via the support service appear here. Empty queue means no open cases.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {rows.map((t) => (
              <div key={t.id} className="flex items-start justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/30">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2"><StatusBadge s={t.status} />{t.priority && <span className="rounded-full bg-muted px-2 py-0.5 text-xs">{t.priority}</span>}{t.category && <span className="rounded-full bg-muted px-2 py-0.5 text-xs">{t.category}</span>}</div>
                  <p className="mt-1 font-medium text-foreground truncate">{t.title}</p>
                  {t.description && <p className="text-xs text-muted-foreground line-clamp-2">{t.description}</p>}
                  <p className="mt-1 flex flex-wrap gap-2 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1"><Clock className="size-3" />{new Date(t.createdAt).toLocaleDateString()}</span>
                    {t.assignedTo && <span className="inline-flex items-center gap-1"><UserCheck className="size-3" />{t.assignedTo}</span>}
                    {t.resolvedAt && <span>Resolved {new Date(t.resolvedAt).toLocaleDateString()}</span>}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <button disabled={assigning === t.id || !adminId.trim()} onClick={() => assign(t)}
                    className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50">
                    <UserCheck className="size-3" /> Assign
                  </button>
                  {(NEXT[t.status] ?? []).map((s) => (
                    <button key={s} disabled={updating === t.id} onClick={() => advance(t, s)}
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
            <button disabled={page.first} onClick={() => setPageIndex(i => i - 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">Previous</button>
            <span className="text-xs text-muted-foreground">Page {page.page + 1} of {page.totalPages} · {page.totalElements} tickets</span>
            <button disabled={page.last} onClick={() => setPageIndex(i => i + 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">Next</button>
          </div>
        )}
      </div>
    </div>
  )
}
