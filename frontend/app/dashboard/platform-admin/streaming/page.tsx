"use client"

import { useEffect, useState, useCallback } from "react"
import { Radio, RefreshCw, AlertCircle, Search, Users, Clock, Activity, HeartPulse, Eye, ExternalLink, Loader2 } from "lucide-react"
import { platformAdminApi, type LiveClassSummary, type PlatformHealth, type PageResponse } from "@/lib/platform-admin-api"

function Stat({ icon: Icon, label, value, sub }: { icon: any; label: string; value: string | number | null; sub: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
      <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary"><Icon className="size-5" /></div>
      <p className="mt-4 text-xs font-bold tracking-widest uppercase text-muted-foreground">{label}</p>
      {value != null ? <p className="mt-1 text-2xl font-bold tabular-nums">{typeof value === "number" ? value.toLocaleString() : value}</p>
        : <p className="mt-1 text-sm font-semibold text-muted-foreground">Data unavailable</p>}
      <p className="mt-1 text-xs text-muted-foreground">{sub}</p>
    </div>
  )
}

export default function PlatformStreamingPage() {
  const [live, setLive] = useState<PageResponse<LiveClassSummary> | null>(null)
  const [all, setAll] = useState<PageResponse<LiveClassSummary> | null>(null)
  const [health, setHealth] = useState<PlatformHealth | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [observing, setObserving] = useState<string | null>(null)
  const [observerUrl, setObserverUrl] = useState<string | null>(null)
  const [observerError, setObserverError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    const [l, a, h] = await Promise.allSettled([
      platformAdminApi.listLiveClasses(0, 50, "LIVE"),
      platformAdminApi.listLiveClasses(0, 50),
      platformAdminApi.getHealth(),
    ])
    if (l.status === "fulfilled") setLive(l.value); else setError("Live class telemetry unavailable")
    if (a.status === "fulfilled") setAll(a.value)
    if (h.status === "fulfilled") setHealth(h.value)
    setLoading(false)
  }, [])

  useEffect(() => { load() }, [load])

  const joinAsObserver = async (liveClassId: string) => {
    setObserving(liveClassId); setObserverError(null); setObserverUrl(null)
    try {
      const res = await platformAdminApi.getObserverJoinUrl(liveClassId)
      setObserverUrl(res.websocketUrl)
    } catch (e: any) {
      setObserverError(e.message || "Unable to join as observer")
    } finally { setObserving(null) }
  }

  const rows = (all?.content ?? []).filter((c) =>
    (!search || c.title.toLowerCase().includes(search.toLowerCase()))
  )
  const liveCount = live?.totalElements ?? null
  const apiUp = health?.apiStatus != null && /up|ok|healthy/i.test(health.apiStatus)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-violet-600 text-white"><Radio className="size-4" /></span> Streaming</h1>
            <p className="mt-1 text-sm text-muted-foreground">Live streaming overview from real live-class sessions and API health. Per-room LiveKit telemetry (egress, participants) shows <span className="font-semibold">Data unavailable</span> until aggregated — no fake metrics.</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat icon={Activity} label="Live now" value={liveCount} sub="Sessions with status LIVE" />
        <Stat icon={Radio} label="Total sessions" value={all?.totalElements ?? null} sub="All live class sessions" />
        <Stat icon={HeartPulse} label="API health" value={health ? (apiUp ? "Operational" : health.apiStatus) : null} sub="From platform health endpoint" />
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      {observerError && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{observerError}</span>
          <button onClick={() => setObserverError(null)} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Dismiss</button>
        </div>
      )}
      {observerUrl && (
        <div className="rounded-2xl border border-violet-200 bg-violet-50 px-4 py-3 text-sm text-violet-800 flex items-center justify-between gap-3">
          <span className="flex min-w-0 items-center gap-2"><Eye className="size-4 shrink-0" />Observer WebSocket: <code className="truncate font-mono text-xs">{observerUrl}</code></span>
          <a href={observerUrl} target="_blank" rel="noopener noreferrer" className="inline-flex shrink-0 items-center gap-1 rounded-lg bg-white border border-violet-200 px-3 py-1 text-xs font-semibold hover:bg-violet-100">
            Open <ExternalLink className="size-3" />
          </a>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-bold text-foreground">Sessions</h2>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Filter by title..." className="rounded-xl border border-border bg-background pl-9 pr-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
        </div>
        <div className="mt-3 space-y-2">
          {loading ? <div className="animate-pulse space-y-2"><div className="h-14 rounded-xl bg-muted" /><div className="h-14 rounded-xl bg-muted" /></div>
            : rows.length === 0 ? (
              <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-10 text-center">
                <Radio className="size-8 text-muted-foreground/50" />
                <p className="mt-2 text-sm font-semibold">No live class sessions</p>
                <p className="mt-1 text-xs text-muted-foreground">Sessions created by teachers appear here. Nothing is fabricated.</p>
              </div>
            ) : rows.map((c) => (
              <div key={c.id} className="flex items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/30">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${c.status === "LIVE" ? "border-red-200 bg-red-50 text-red-700" : c.status === "COMPLETED" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "bg-muted"}`}>{c.status}</span>
                    <p className="truncate font-medium text-foreground">{c.title}</p>
                  </div>
                  <p className="mt-1 flex flex-wrap gap-3 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1"><Clock className="size-3" />{new Date(c.scheduledAt).toLocaleString()}</span>
                    <span>· {c.durationMinutes} min</span>
                    <span className="inline-flex items-center gap-1"><Users className="size-3" />{c.currentParticipants}/{c.maxParticipants}</span>
                  </p>
                </div>
                {(c.status === "LIVE" || c.status === "IN_PROGRESS") && (
                  <button
                    onClick={() => joinAsObserver(c.id)}
                    disabled={observing === c.id}
                    className="inline-flex shrink-0 items-center gap-1.5 rounded-xl border border-violet-200 bg-violet-50 px-3 py-1.5 text-xs font-semibold text-violet-700 hover:bg-violet-100 disabled:opacity-50"
                    title="Join as read-only observer"
                  >
                    {observing === c.id ? <Loader2 className="size-3.5 animate-spin" /> : <Eye className="size-3.5" />}
                    Observe
                  </button>
                )}
              </div>
            ))}
        </div>
        {all && all.totalPages > 1 && (
          <p className="mt-3 text-center text-xs text-muted-foreground">Showing first {all.size} of {all.totalElements} sessions</p>
        )}
      </div>
    </div>
  )
}
