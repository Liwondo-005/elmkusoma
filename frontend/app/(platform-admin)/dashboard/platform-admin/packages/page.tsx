"use client"

import { useEffect, useState, useCallback } from "react"
import { Package, RefreshCw, AlertCircle, Search, Users, CheckCircle2, XCircle } from "lucide-react"
import { platformAdminApi, type ServiceSummary, type PageResponse } from "@/lib/platform-admin-api"

export default function PlatformPackagesPage() {
  const [page, setPage] = useState<PageResponse<ServiceSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [pageIndex, setPageIndex] = useState(0)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      setPage(await platformAdminApi.listServices(pageIndex, 24))
    } catch (e: any) {
      setError(e.message || "Failed to load packages"); setPage(null)
    } finally { setLoading(false) }
  }, [pageIndex])

  useEffect(() => { load() }, [load])

  const rows = (page?.content ?? []).filter((s) =>
    !search || `${s.name} ${s.code} ${s.category}`.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-teal-600 text-white"><Package className="size-4" /></span> Packages</h1>
            <p className="mt-1 text-sm text-muted-foreground">Service packages backed by the platform services catalogue — pricing, seat quotas, and activation state (spec § packages / §40).</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
        </div>
        <div className="mt-4 relative">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search packages..." className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <div className="animate-pulse grid gap-4 sm:grid-cols-2 lg:grid-cols-3"><div className="h-32 rounded-xl bg-muted" /><div className="h-32 rounded-xl bg-muted" /><div className="h-32 rounded-xl bg-muted" /></div> : rows.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <Package className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">No packages found</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">Packages are real entries from the platform services catalogue. Empty result means no services are defined — nothing is fabricated.</p>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {rows.map((s) => (
              <div key={s.id} className="rounded-xl border border-border bg-card p-4 hover:bg-muted/30">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-semibold text-foreground">{s.name}</p>
                    <p className="text-xs text-muted-foreground">{s.code} · {s.category}</p>
                  </div>
                  {s.isActive
                    ? <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700"><CheckCircle2 className="size-3" />Active</span>
                    : <span className="inline-flex items-center gap-1 rounded-full border bg-slate-50 px-2 py-0.5 text-xs font-semibold text-slate-600"><XCircle className="size-3" />Inactive</span>}
                </div>
                <p className="mt-2 line-clamp-2 text-xs text-muted-foreground">{s.description}</p>
                <div className="mt-3 flex items-center justify-between text-xs">
                  <span className="font-bold text-foreground">{s.monthlyPrice != null ? `${s.monthlyPrice.toLocaleString()} ${s.currency}/mo` : "No price set"}</span>
                  {s.maxSeats != null && <span className="inline-flex items-center gap-1 text-muted-foreground"><Users className="size-3" />{s.maxSeats} seats</span>}
                </div>
                {s.requiresVerification && <p className="mt-2 text-xs text-amber-600">Requires verification</p>}
              </div>
            ))}
          </div>
        )}
        {page && page.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <button disabled={page.first} onClick={() => setPageIndex(i => i - 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">Previous</button>
            <span className="text-xs text-muted-foreground">Page {page.page + 1} of {page.totalPages} · {page.totalElements} packages</span>
            <button disabled={page.last} onClick={() => setPageIndex(i => i + 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">Next</button>
          </div>
        )}
      </div>
    </div>
  )
}
