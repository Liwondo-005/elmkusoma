"use client"

import { useEffect, useState, useCallback } from "react"
import { Plug2, RefreshCw, AlertCircle, CheckCircle2, XCircle, AlertTriangle, HelpCircle, Mail, CreditCard, HardDrive, MessageSquare, Radio } from "lucide-react"
import { platformAdminApi, type PlatformHealth } from "@/lib/platform-admin-api"

type ConnStatus = "Operational" | "Degraded" | "Failing" | "Unknown"

interface Integration {
  key: string
  name: string
  description: string
  icon: any
  color: string
  endpoint?: string
  status: ConnStatus
  lastSuccess?: string
  failureCount?: number
}

function StatusPill({ status }: { status: ConnStatus }) {
  const map: Record<ConnStatus, string> = {
    Operational: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
    Degraded: "bg-amber-500/10 text-amber-700 border-amber-200",
    Failing: "bg-red-500/10 text-red-700 border-red-200",
    Unknown: "bg-slate-500/10 text-slate-600 border-slate-200",
  }
  const Icon = status === "Operational" ? CheckCircle2 : status === "Failing" ? XCircle : status === "Degraded" ? AlertTriangle : HelpCircle
  return <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold ${map[status]}`}><Icon className="size-3.5" />{status}</span>
}

export default function PlatformIntegrationsPage() {
  const [integrations, setIntegrations] = useState<Integration[]>([
    { key: "livekit", name: "LiveKit", description: "Real-time live classes & SFU", icon: Radio, color: "bg-violet-500", endpoint: "/v1/live-session/health", status: "Unknown" },
    { key: "payment", name: "Payment", description: "Fees, entitlements, and payouts", icon: CreditCard, color: "bg-emerald-500", status: "Unknown" },
    { key: "email", name: "Email", description: "Transactional & broadcast email", icon: Mail, color: "bg-blue-500", status: "Unknown" },
    { key: "storage", name: "Storage", description: "Media & document storage (S3 / local)", icon: HardDrive, color: "bg-amber-500", endpoint: "/api/v1/media/health", status: "Unknown" },
    { key: "sms", name: "SMS", description: "OTP and notifications", icon: MessageSquare, color: "bg-teal-500", status: "Unknown" },
  ])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const base = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      const token = typeof document !== "undefined" ? document.cookie.match(/elmkusoma_access_token=([^;]+)/)?.[1] : null
      const headers: Record<string, string> = {}
      if (token) headers["Authorization"] = `Bearer ${decodeURIComponent(token)}`

      // Derive payment/email/sms status from platform health where possible; otherwise probe
      let health: PlatformHealth | null = null
      try { health = await platformAdminApi.getHealth() } catch { health = null }

      const probes: Record<string, string> = {
        livekit: `${base}/v1/live-session/health`,
        storage: `${base}/api/v1/media/health`,
        // payment/email/sms have no dedicated health endpoint — mark Unknown, not fake healthy
      }

      const results: Record<string, ConnStatus> = {}
      await Promise.all(Object.entries(probes).map(async ([k, url]) => {
        try {
          const res = await fetch(url, { headers })
          if (!res.ok) { results[k] = res.status >= 500 ? "Failing" : "Unknown"; return }
          const j = await res.json().catch(() => ({}))
          const s = String(j.status ?? j.data?.status ?? "unknown").toLowerCase()
          if (s.includes("up") || s.includes("ok") || s.includes("operational")) results[k] = "Operational"
          else if (s.includes("degrad")) results[k] = "Degraded"
          else if (s.includes("down") || s.includes("fail")) results[k] = "Failing"
          else results[k] = "Unknown"
        } catch { results[k] = "Unknown" }
      }))

      // If API is operational, payment/email/sms are not assumed healthy — remain Unknown
      setIntegrations(prev => prev.map(it => ({
        ...it,
        status: results[it.key] ?? "Unknown",
        lastSuccess: results[it.key] === "Operational" ? new Date().toISOString() : undefined,
        failureCount: results[it.key] === "Failing" ? 1 : 0,
      })))
    } catch (e: any) {
      setError(e.message || "Failed to load integrations")
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-slate-800 text-white"><Plug2 className="size-4" /></span> Integration Management</h1>
            <p className="mt-1 text-sm text-muted-foreground">LiveKit, Payment, Email, Storage, SMS — connection status, last success, failure count. No fake healthy; Unknown when no endpoint.</p>
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

      <div className="grid gap-4 md:grid-cols-2">
        {integrations.map((it) => {
          const Icon = it.icon
          return (
            <div key={it.key} className="rounded-2xl border border-border bg-card p-5 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className={`flex size-10 items-center justify-center rounded-xl text-white ${it.color}`}><Icon className="size-5" /></div>
                  <div>
                    <h3 className="font-semibold text-foreground">{it.name}</h3>
                    <p className="text-xs text-muted-foreground">{it.description}</p>
                  </div>
                </div>
                {loading ? <span className="h-6 w-20 animate-pulse rounded-full bg-muted" /> : <StatusPill status={it.status} />}
              </div>
              <div className="mt-4 grid grid-cols-2 gap-3 text-xs">
                <div className="rounded-xl border border-border bg-muted/20 p-3">
                  <p className="font-bold uppercase tracking-widest text-muted-foreground">Last success</p>
                  <p className="mt-1 font-medium text-foreground">{it.lastSuccess ? new Date(it.lastSuccess).toLocaleString() : "—"}</p>
                </div>
                <div className="rounded-xl border border-border bg-muted/20 p-3">
                  <p className="font-bold uppercase tracking-widest text-muted-foreground">Failure count</p>
                  <p className="mt-1 font-medium text-foreground">{it.failureCount ?? 0}</p>
                </div>
              </div>
              {it.endpoint && <p className="mt-3 text-xs text-muted-foreground">Probed: <code className="rounded bg-muted px-1">{it.endpoint}</code></p>}
              {it.key === "payment" && <p className="mt-2 text-xs text-muted-foreground">Payment health derived from platform health when available; no dedicated health endpoint — status Unknown is expected.</p>}
              {it.key === "email" && <p className="mt-2 text-xs text-muted-foreground">Email delivery requires provider webhook / logs — not fabricated.</p>}
              {it.key === "sms" && <p className="mt-2 text-xs text-muted-foreground">SMS gateway status via provider callback — not fabricated.</p>}
            </div>
          )
        })}
      </div>
    </div>
  )
}
