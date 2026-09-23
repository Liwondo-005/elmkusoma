"use client"

import { useEffect, useState, useCallback } from "react"
import { ShieldCheck, AlertCircle, RefreshCw, Search, Clock, Flag, CheckCircle2, XCircle, Eye, Plus } from "lucide-react"
import { platformAdminApi, type PageResponse, type ContentReport } from "@/lib/platform-admin-api"

const STATUSES = ["", "OPEN", "REVIEWING", "RESOLVED", "DISMISSED"]
const ENTITY_TYPES = ["COURSE", "MEDIA", "RESOURCE", "EVENT", "USER", "COMMENT"]
const REASONS = ["INAPPROPRIATE", "SPAM", "COPYRIGHT", "SAFETY", "OTHER"]

function StatusBadge({ s }: { s: string }) {
  const map: Record<string, string> = {
    OPEN: "bg-blue-500/10 text-blue-700 border-blue-200",
    REVIEWING: "bg-amber-500/10 text-amber-700 border-amber-200",
    RESOLVED: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
    DISMISSED: "bg-slate-500/10 text-slate-600 border-slate-200",
  }
  return <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${map[s] ?? "bg-muted"}`}>{s}</span>
}

export default function PlatformModerationPage() {
  const [page, setPage] = useState<PageResponse<ContentReport> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState("")
  const [pageIndex, setPageIndex] = useState(0)
  const [search, setSearch] = useState("")
  const [updating, setUpdating] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ entityType: "COURSE", entityId: "", entityTitle: "", reason: "INAPPROPRIATE", description: "" })
  const [formError, setFormError] = useState<string | null>(null)
  const [formBusy, setFormBusy] = useState(false)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      setPage(await platformAdminApi.listContentReports(pageIndex, 20, status || undefined))
    } catch (e: any) {
      setError(e.message || "Failed to load reports"); setPage(null)
    } finally { setLoading(false) }
  }, [pageIndex, status])

  useEffect(() => { load() }, [load])

  const act = async (id: string, action: string, notes?: string) => {
    setUpdating(id)
    try { await platformAdminApi.actOnContentReport(id, action, notes); await load() }
    catch (e: any) { setError(e.message || "Action failed") }
    finally { setUpdating(null) }
  }

  const submitReport = async () => {
    setFormBusy(true); setFormError(null)
    try {
      if (!form.entityId.trim()) { setFormError("Entity ID is required — no fabricated references."); return }
      await platformAdminApi.createContentReport({ ...form, entityId: form.entityId.trim(), entityTitle: form.entityTitle || undefined })
      setShowForm(false)
      setForm({ entityType: "COURSE", entityId: "", entityTitle: "", reason: "INAPPROPRIATE", description: "" })
      await load()
    } catch (e: any) { setFormError(e.message || "Failed to create report") }
    finally { setFormBusy(false) }
  }

  const rows = (page?.content ?? []).filter((r) =>
    !search || `${r.entityTitle ?? ""} ${r.reason} ${r.description ?? ""}`.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-orange-500 text-white"><ShieldCheck className="size-4" /></span> Moderation</h1>
            <p className="mt-1 text-sm text-muted-foreground">Content report queue — reported courses, media, resources, events, and users. Actions: <span className="font-mono text-xs">OPEN → REVIEWING → RESOLVED | DISMISSED</span>.</p>
          </div>
          <div className="flex gap-2">
            <button onClick={() => setShowForm(v => !v)} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><Plus className="size-4" /> New report</button>
            <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
          </div>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search reports..." className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <select value={status} onChange={(e) => { setStatus(e.target.value); setPageIndex(0) }} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
            {STATUSES.map(s => <option key={s} value={s}>{s || "All statuses"}</option>)}
          </select>
        </div>
        {showForm && (
          <div className="mt-4 rounded-xl border border-border bg-muted/20 p-4 space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="text-xs font-semibold text-foreground">Entity type
                <select value={form.entityType} onChange={(e) => setForm(f => ({ ...f, entityType: e.target.value }))} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm">
                  {ENTITY_TYPES.map(t => <option key={t}>{t}</option>)}
                </select>
              </label>
              <label className="text-xs font-semibold text-foreground">Reason
                <select value={form.reason} onChange={(e) => setForm(f => ({ ...f, reason: e.target.value }))} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm">
                  {REASONS.map(t => <option key={t}>{t}</option>)}
                </select>
              </label>
              <label className="text-xs font-semibold text-foreground">Entity ID (UUID)
                <input value={form.entityId} onChange={(e) => setForm(f => ({ ...f, entityId: e.target.value }))} placeholder="e.g. 3f2a…" className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm font-mono" />
              </label>
              <label className="text-xs font-semibold text-foreground">Entity title (optional)
                <input value={form.entityTitle} onChange={(e) => setForm(f => ({ ...f, entityTitle: e.target.value }))} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" />
              </label>
            </div>
            <label className="block text-xs font-semibold text-foreground">Description
              <textarea value={form.description} onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))} rows={2} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" />
            </label>
            {formError && <p className="flex items-center gap-1 text-xs text-red-600"><AlertCircle className="size-3.5" />{formError}</p>}
            <button onClick={submitReport} disabled={formBusy} className="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {formBusy ? "Submitting…" : "Submit report"}
            </button>
          </div>
        )}
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
            <ShieldCheck className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">Moderation queue is empty</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">No content reports match this filter. Reported courses, media, and user content will surface here — nothing is fabricated.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {rows.map((r) => (
              <div key={r.id} className="flex items-start justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/30">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge s={r.status} />
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-semibold">{r.entityType}</span>
                    <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-xs"><Flag className="size-3" />{r.reason}</span>
                  </div>
                  <p className="mt-1 font-medium text-foreground truncate">{r.entityTitle || r.entityId}</p>
                  {r.description && <p className="text-xs text-muted-foreground line-clamp-2">{r.description}</p>}
                  <p className="mt-1 flex flex-wrap gap-2 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1"><Clock className="size-3" />{new Date(r.createdAt).toLocaleString()}</span>
                    {r.resolvedAt && <span>· Resolved {new Date(r.resolvedAt).toLocaleString()}</span>}
                    {r.resolutionNotes && <span>· {r.resolutionNotes}</span>}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  {r.status === "OPEN" && (
                    <button disabled={updating === r.id} onClick={() => act(r.id, "REVIEWING")} className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50"><Eye className="size-3" /> Review</button>
                  )}
                  {r.status === "OPEN" || r.status === "REVIEWING" ? (
                    <>
                      <button disabled={updating === r.id} onClick={() => act(r.id, "RESOLVED", "Actioned by platform admin")} className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 px-2.5 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-50 disabled:opacity-50"><CheckCircle2 className="size-3" /> Resolve</button>
                      <button disabled={updating === r.id} onClick={() => act(r.id, "DISMISSED", "No violation found")} className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-semibold hover:bg-muted disabled:opacity-50"><XCircle className="size-3" /> Dismiss</button>
                    </>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        )}
        {page && page.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <button disabled={page.first} onClick={() => setPageIndex(i => i - 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">Previous</button>
            <span className="text-xs text-muted-foreground">Page {page.page + 1} of {page.totalPages} · {page.totalElements} reports</span>
            <button disabled={page.last} onClick={() => setPageIndex(i => i + 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">Next</button>
          </div>
        )}
      </div>
    </div>
  )
}
