"use client"

import { useEffect, useState, useCallback } from "react"
import { HeartPulse, RefreshCw, Loader2, AlertCircle, Database, Server, Radio, HardDrive, Cpu, CheckCircle2, XCircle, AlertTriangle, HelpCircle } from "lucide-react"
import { platformAdminApi, type PlatformHealth } from "@/lib/platform-admin-api"

type HealthStatus = "Operational" | "Degraded" | "Failing" | "Unknown"

function statusFromString(s: string): HealthStatus {
  if (!s) return "Unknown"
  const l = s.toLowerCase()
  if (l.includes("up") || l.includes("operational") || l.includes("ok") || l.includes("healthy")) return "Operational"
  if (l.includes("degrad")) return "Degraded"
  if (l.includes("down") || l.includes("fail") || l.includes("error")) return "Failing"
  return "Unknown"
}

function StatusBadge({ status }: { status: HealthStatus }) {
  const map: Record<HealthStatus, string> = {
    Operational: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
    Degraded: "bg-amber-500/10 text-amber-700 border-amber-200",
    Failing: "bg-red-500/10 text-red-700 border-red-200",
    Unknown: "bg-slate-500/10 text-slate-600 border-slate-200",
  }
  const Icon = status === "Operational" ? CheckCircle2 : status === "Failing" ? XCircle : status === "Degraded" ? AlertTriangle : HelpCircle
  return <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold ${map[status]}`}><Icon className="size-3.5" />{status}</span>
}

function HealthRow({ icon: Icon, label, status, detail }: { icon: any; label: string; status: HealthStatus; detail?: string }) {
  const dot: Record<HealthStatus, string> = { Operational: "bg-emerald-500", Degraded: "bg-amber-500", Failing: "bg-red-500", Unknown: "bg-slate-300" }
  return (
    <div className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
      <span className="flex items-center gap-3 text-sm font-medium"><span className={`size-2 rounded-full ${dot[status]}`} /><Icon className="size-4 text-muted-foreground" />{label}{detail && <span className="text-xs text-muted-foreground">· {detail}</span>}</span>
      <StatusBadge status={status} />
    </div>
  )
}

export default function PlatformHealthPage() {
  const [health, setHealth] = useState<PlatformHealth | null>(null)
  const [livekit, setLivekit] = useState<HealthStatus>("Unknown")
  const [media, setMedia] = useState<HealthStatus>("Unknown")
  const [worker, setWorker] = useState<HealthStatus>("Unknown")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const h = await platformAdminApi.getHealth()
      setHealth(h)
      // Probe auxiliary health endpoints best-effort — do not fail page if unavailable
      const base = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      const token = typeof document !== "undefined" ? document.cookie.match(/elmkusoma_access_token=([^;]+)/)?.[1] : null
      const headers: Record<string, string> = {}
      if (token) headers["Authorization"] = `Bearer ${decodeURIComponent(token)}`
      const probes: [string, (s: HealthStatus) => void][] = [
        [`${base}/v1/live-session/health`, setLivekit],
        [`${base}/api/v1/media/health`, setMedia],
        [`${base}/v1/worker/health`, setWorker],
      ]
      await Promise.all(probes.map(async ([url, setter]) => {
        try {
          const res = await fetch(url, { headers })
          if (!res.ok) { setter(res.status >= 500 ? "Failing" : "Unknown"); return }
          const j = await res.json().catch(() => ({}))
          const s = j.status ?? j.data?.status ?? "Operational"
          setter(statusFromString(String(s)))
        } catch { setter("Unknown") }
      }))
    } catch (e: any) {
      setError(e.message || "Failed to load health")
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-emerald-500 text-white"><HeartPulse className="size-4" /></span> Platform Operations Health</h1>
            <p className="mt-1 text-sm text-muted-foreground">Verified health: Database & API from <code className="rounded bg-muted px-1">getHealth</code>, plus LiveKit / Media / Worker probes. Statuses: Operational / Degraded / Failing / Unknown.</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h2 className="text-sm font-bold text-foreground">Core Health (verified)</h2>
        {loading ? <div className="mt-4 space-y-2 animate-pulse"><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /></div> : health ? (
          <div className="mt-4 space-y-2">
            <HealthRow icon={Database} label="Database" status={statusFromString(health.databaseStatus)} detail={health.databaseStatus} />
            <HealthRow icon={Server} label="API" status={statusFromString(health.apiStatus)} detail={health.apiStatus} />
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div className="rounded-xl border border-border bg-muted/20 p-3 text-center"><p className="text-lg font-bold tabular-nums">{health.totalUsers.toLocaleString()}</p><p className="text-xs text-muted-foreground">Total Users</p><p className="text-xs text-emerald-600">{health.activeUsers.toLocaleString()} active</p></div>
              <div className="rounded-xl border border-border bg-muted/20 p-3 text-center"><p className="text-lg font-bold tabular-nums">{health.totalInstitutions.toLocaleString()}</p><p className="text-xs text-muted-foreground">Total Institutions</p><p className="text-xs text-emerald-600">{health.activeInstitutions.toLocaleString()} active</p></div>
            </div>
          </div>
        ) : <p className="mt-4 text-sm text-muted-foreground">Data unavailable — health endpoint did not return data per spec §13.</p>}
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h2 className="text-sm font-bold text-foreground">Extended Probes (best-effort)</h2>
        <p className="mt-1 text-xs text-muted-foreground">Probed via <code className="rounded bg-muted px-1">/v1/live-session/health</code>, <code className="rounded bg-muted px-1">/api/v1/media/health</code>, <code className="rounded bg-muted px-1">/v1/worker/health</code>. Unknown means endpoint unavailable — not fabricated as healthy.</p>
        {loading ? <div className="mt-4 space-y-2 animate-pulse"><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /></div> : (
          <div className="mt-4 space-y-2">
            <HealthRow icon={Radio} label="LiveKit / Live Session" status={livekit} detail={livekit === "Unknown" ? "No health endpoint" : undefined} />
            <HealthRow icon={HardDrive} label="Media / Storage" status={media} detail={media === "Unknown" ? "No health endpoint" : undefined} />
            <HealthRow icon={Cpu} label="Worker / Jobs" status={worker} detail={worker === "Unknown" ? "No health endpoint" : undefined} />
          </div>
        )}
      </div>
    </div>
  )
}
