"use client"

import { useEffect, useState, useCallback } from "react"
import { Search, Loader2, AlertCircle, RefreshCw, Users, Building2, Filter } from "lucide-react"
import { platformAdminApi, type GlobalSearchResult } from "@/lib/platform-admin-api"

function Skeleton() {
  return <div className="animate-pulse space-y-2"><div className="h-14 rounded-xl bg-muted" /><div className="h-14 rounded-xl bg-muted" /><div className="h-14 rounded-xl bg-muted" /></div>
}

export default function PlatformGlobalSearchPage() {
  const [q, setQ] = useState("")
  const [type, setType] = useState<string>("")
  const [results, setResults] = useState<GlobalSearchResult[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hasSearched, setHasSearched] = useState(false)

  const doSearch = useCallback(async (query: string, t: string) => {
    if (!query.trim()) { setResults([]); setHasSearched(false); return }
    setLoading(true); setError(null); setHasSearched(true)
    try {
      const res = await platformAdminApi.search(query.trim(), t || undefined)
      setResults(Array.isArray(res) ? res : [])
    } catch (e: any) {
      setError(e.message || "Search failed")
      setResults([])
    } finally { setLoading(false) }
  }, [])

  const onSubmit = (e: React.FormEvent) => { e.preventDefault(); doSearch(q, type) }

  useEffect(() => {
    const id = setTimeout(() => { if (q.trim().length >= 2) doSearch(q, type) }, 400)
    return () => clearTimeout(id)
  }, [q, type, doSearch])

  const grouped = results.reduce<Record<string, GlobalSearchResult[]>>((acc, r) => {
    const k = (r.type ?? "OTHER").toUpperCase()
    acc[k] = acc[k] ?? []
    acc[k].push(r)
    return acc
  }, {})

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-blue-600 text-white"><Search className="size-4" /></span> Global Search</h1>
        <p className="mt-1 text-sm text-muted-foreground">Permission-aware search via <code className="rounded bg-muted px-1">platformAdminApi.search(q)</code> — results grouped by type USER / INSTITUTION. Scoped to caller’s platform-admin privileges.</p>
        <form onSubmit={onSubmit} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search users, institutions..." className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-muted-foreground" />
            <select value={type} onChange={(e) => setType(e.target.value)} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
              <option value="">All types</option>
              <option value="USER">USER</option>
              <option value="INSTITUTION">INSTITUTION</option>
            </select>
            <button type="submit" className="rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90">Search</button>
          </div>
        </form>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={() => doSearch(q, type)} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <Skeleton /> : !hasSearched ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <Search className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">Search the platform</p>
            <p className="mt-1 text-xs text-muted-foreground">Enter at least 2 characters to search. Results are permission-aware.</p>
          </div>
        ) : results.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <Search className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">No results for “{q}”</p>
            <p className="mt-1 text-xs text-muted-foreground">Try a different query or change type filter. Verified search — no synthetic results.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {Object.entries(grouped).map(([t, items]) => (
              <div key={t}>
                <h2 className="flex items-center gap-2 text-xs font-bold tracking-widest uppercase text-muted-foreground">
                  {t === "USER" ? <Users className="size-3.5" /> : t === "INSTITUTION" ? <Building2 className="size-3.5" /> : <Search className="size-3.5" />} {t} · {items.length}
                </h2>
                <div className="mt-2 space-y-2">
                  {items.map((r) => (
                    <div key={`${r.type}-${r.id}`} className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/30">
                      <div className="min-w-0">
                        <p className="font-medium text-foreground truncate">{r.title}</p>
                        <p className="text-xs text-muted-foreground truncate">{r.subtitle ?? r.id}</p>
                      </div>
                      <span className="ml-3 shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs font-medium">{r.type}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
