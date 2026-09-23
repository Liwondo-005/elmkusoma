"use client"

import { useEffect, useState } from "react"
import { BarChart3, Users, Building2, Radio, AlertTriangle, ShieldAlert, Package, Award, GraduationCap, Activity, Loader2, AlertCircle, RefreshCw, TrendingUp, Camera, History } from "lucide-react"
import { platformAdminApi, type EnhancedDashboard, type AttentionItem, type AnalyticsSnapshot } from "@/lib/platform-admin-api"

function SkeletonCard() {
  return <div className="rounded-2xl border border-border bg-card p-5 animate-pulse"><div className="h-10 w-10 rounded-xl bg-muted" /><div className="mt-4 h-6 w-20 rounded bg-muted" /><div className="mt-2 h-3 w-24 rounded bg-muted" /></div>
}

function Kpi({ icon: Icon, label, value, sub, available }: { icon: any; label: string; value: number | null; sub: string; available: boolean }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
      <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary"><Icon className="size-5" /></div>
      <p className="mt-4 text-xs font-bold tracking-widest uppercase text-muted-foreground">{label}</p>
      {available ? <p className="mt-1 text-2xl font-bold tabular-nums">{(value ?? 0).toLocaleString()}</p> : <p className="mt-1 text-sm font-semibold text-muted-foreground">Data unavailable</p>}
      <p className="mt-1 text-xs text-muted-foreground">{sub}</p>
    </div>
  )
}

export default function PlatformAnalyticsPage() {
  const [dash, setDash] = useState<EnhancedDashboard | null>(null)
  const [attention, setAttention] = useState<AttentionItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [snapshots, setSnapshots] = useState<AnalyticsSnapshot[]>([])
  const [snapError, setSnapError] = useState<string | null>(null)
  const [snapBusy, setSnapBusy] = useState(false)

  async function load() {
    setLoading(true); setError(null)
    try {
      const [d, a] = await Promise.allSettled([platformAdminApi.getEnhancedDashboard(), platformAdminApi.getAttention()])
      if (d.status === "fulfilled") setDash(d.value)
      else setError("Unable to load analytics — Data unavailable")
      if (a.status === "fulfilled") setAttention(a.value)
    } catch (e: any) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])
  useEffect(() => { loadSnapshots() }, [])

  async function loadSnapshots() {
    setSnapError(null)
    try { setSnapshots(await platformAdminApi.listAnalyticsSnapshots(20)) }
    catch (e: any) { setSnapshots([]); setSnapError(e.message || "Snapshots unavailable") }
  }

  async function createSnapshot() {
    setSnapBusy(true); setSnapError(null)
    try {
      await platformAdminApi.createAnalyticsSnapshot()
      await loadSnapshots()
    } catch (e: any) { setSnapError(e.message || "Failed to create snapshot") }
    finally { setSnapBusy(false) }
  }

  const hasDash = !!dash

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-indigo-500 text-white"><BarChart3 className="size-4" /></span> Platform Analytics & Intelligence</h1>
            <p className="mt-1 text-sm text-muted-foreground">Real KPI cards from <code className="rounded bg-muted px-1">getEnhancedDashboard</code>. No fake charts — only verified counts. “Data unavailable” when source missing per spec §13.</p>
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

      <div>
        <h2 className="mb-3 text-xs font-bold tracking-widest uppercase text-muted-foreground">Key Performance Indicators</h2>
        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[1, 2, 3, 4].map(i => <SkeletonCard key={i} />)}</div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Kpi icon={Users} label="Total Users" value={dash?.totalUsers ?? null} sub={`${dash?.totalStudents ?? "—"} students · ${dash?.totalTeachers ?? "—"} educators`} available={hasDash} />
            <Kpi icon={Building2} label="Institutions" value={dash?.totalInstitutions ?? null} sub="Active institutions" available={hasDash} />
            <Kpi icon={Radio} label="Live Classes" value={dash?.totalLiveClasses ?? null} sub={`${dash?.activeLiveClasses ?? "—"} active now`} available={hasDash} />
            <Kpi icon={Award} label="Certificates" value={dash?.totalCertificates ?? null} sub="Issued & verified" available={hasDash} />
          </div>
        )}
      </div>

      {loading ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[1, 2, 3, 4].map(i => <SkeletonCard key={i} />)}</div> : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Kpi icon={AlertTriangle} label="Open Incidents" value={dash?.openIncidents ?? null} sub="Requires resolution" available={hasDash} />
          <Kpi icon={ShieldAlert} label="Pending Verifications" value={dash?.pendingVerifications ?? null} sub="Awaiting review" available={hasDash} />
          <Kpi icon={Package} label="Active Services" value={dash?.activeServices ?? null} sub="Enabled catalogue" available={hasDash} />
          <Kpi icon={GraduationCap} label="Payments" value={dash?.totalPayments ?? null} sub="Total payments" available={hasDash} />
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><History className="size-4 text-primary" /> Analytics Snapshots</h2>
            <p className="mt-1 text-xs text-muted-foreground">Persisted daily snapshots of verified KPI counts (created by scheduler or on demand).</p>
          </div>
          <button onClick={createSnapshot} disabled={snapBusy}
            className="inline-flex items-center gap-2 rounded-xl border border-border bg-background px-3 py-2 text-xs font-semibold hover:bg-muted disabled:opacity-50">
            {snapBusy ? <Loader2 className="size-3.5 animate-spin" /> : <Camera className="size-3.5" />} Create snapshot
          </button>
        </div>
        {snapError && <p className="mt-2 flex items-center gap-1 text-xs text-red-600"><AlertCircle className="size-3.5" />{snapError}</p>}
        <div className="mt-4">
          {snapshots.length === 0 && !snapError ? (
            <div className="rounded-xl border border-dashed border-border bg-muted/30 p-6 text-center">
              <p className="text-sm font-medium">No snapshots yet</p>
              <p className="mt-1 text-xs text-muted-foreground">Create one now or wait for the daily job.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {snapshots.map(s => (
                <div key={s.id} className="flex items-start justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-foreground">{s.snapshotType}</p>
                    <p className="text-xs text-muted-foreground">{s.generatedAt ? new Date(s.generatedAt).toLocaleString() : "Data unavailable"}</p>
                    {s.data && (
                      <p className="mt-1 flex flex-wrap gap-2 text-xs text-muted-foreground">
                        {Object.entries(s.data).slice(0, 8).map(([k, v]) => (
                          <span key={k} className="rounded-full bg-muted px-2 py-0.5">{k}: {String(v)}</span>
                        ))}
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><TrendingUp className="size-4 text-primary" /> Intelligence — Attention Items</h2>
          <p className="mt-1 text-xs text-muted-foreground">Signals requiring action. No fabricated trends; only attention items from verified source.</p>
          <div className="mt-4">
            {loading ? <div className="h-24 animate-pulse rounded-xl bg-muted" /> : attention.length === 0 ? (
              <div className="rounded-xl border border-dashed border-border bg-muted/30 p-8 text-center">
                <Activity className="mx-auto size-8 text-muted-foreground" />
                <p className="mt-2 text-sm font-medium">No attention signals</p>
                <p className="mt-1 text-xs text-muted-foreground">When incidents, verifications, or security events exist they will surface here. Data unavailable otherwise.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {attention.slice(0, 8).map((it, i) => (
                  <div key={i} className="flex gap-3 rounded-xl border border-border bg-card px-4 py-3">
                    <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-bold text-white h-fit ${it.severity === "HIGH" ? "bg-red-500" : it.severity === "MEDIUM" ? "bg-amber-500" : "bg-blue-500"}`}>{it.severity}</span>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold">{it.title}</p>
                      <p className="text-xs text-muted-foreground line-clamp-2">{it.description}</p>
                      <p className="mt-1 text-xs text-muted-foreground">{it.category}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="text-sm font-bold text-foreground">Analytics Notes</h2>
          <ul className="mt-3 space-y-2 text-xs leading-relaxed text-muted-foreground list-disc pl-4">
            <li>All KPI values are taken directly from <code className="rounded bg-muted px-1">/v1/platform-admin/dashboard/enhanced</code>.</li>
            <li>No synthetic charts are rendered — charts require verified time-series endpoints.</li>
            <li>When endpoint returns no data, cards show <span className="font-semibold">“Data unavailable”</span> per spec §13.</li>
            <li>Extend with cohort or retention analytics when backend exposes them.</li>
          </ul>
          <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
            Charts are intentionally omitted. Fabricated metrics are disallowed — wire a real analytics service before visualizing trends.
          </div>
        </div>
      </div>
    </div>
  )
}
