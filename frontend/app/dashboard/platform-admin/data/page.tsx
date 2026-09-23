"use client"

import { useEffect, useState, useCallback } from "react"
import { Database, Shield, Eye, Lock, Download, RefreshCw, FileCheck, Building2, HardDrive, AlertCircle, Clock, PlayCircle } from "lucide-react"
import { platformAdminApi, type PlatformConfigItem, type RetentionStatus, type DataQualityCheck } from "@/lib/platform-admin-api"

export default function PlatformDataGovernancePage() {
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState<string | null>(null)
  const [exportSuccess, setExportSuccess] = useState(false)
  const [config, setConfig] = useState<PlatformConfigItem[] | null>(null)
  const [configError, setConfigError] = useState<string | null>(null)
  const [configLoading, setConfigLoading] = useState(true)
  const [retention, setRetention] = useState<RetentionStatus | null>(null)
  const [quality, setQuality] = useState<DataQualityCheck[] | null>(null)
  const [sweeping, setSweeping] = useState(false)
  const [govError, setGovError] = useState<string | null>(null)

  const loadGov = useCallback(async () => {
    setConfigLoading(true); setGovError(null)
    try {
      const [r, q, c] = await Promise.all([
        platformAdminApi.getRetentionStatus(),
        platformAdminApi.getDataQuality(),
        platformAdminApi.listConfig("DATA"),
      ])
      setRetention(r); setQuality(q); setConfig(c)
    } catch (e: any) {
      setGovError(e.message || "Data unavailable")
      setRetention(null); setQuality(null)
    } finally { setConfigLoading(false) }
  }, [])

  useEffect(() => { loadGov() }, [loadGov])

  const runSweep = async () => {
    setSweeping(true); setGovError(null)
    try {
      const r = await platformAdminApi.runRetentionSweep()
      setRetention(r)
    } catch (e: any) {
      setGovError(e.message || "Sweep failed")
    } finally { setSweeping(false) }
  }

  const handleExport = async (type: "users" | "institutions" | "audit") => {
    setExporting(true); setExportError(null); setExportSuccess(false)
    try {
      await platformAdminApi.exportPlatformData(type)
      setExportSuccess(true)
    } catch (e: any) {
      setExportError(`${e.message} — Data unavailable per spec §13.`)
    } finally { setExporting(false) }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-indigo-600 text-white"><Database className="size-4" /></span> Data Governance</h1>
        <p className="mt-1 text-sm text-muted-foreground">Ownership, retention sweep, and live data-quality checks — every number from a real query.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Building2 className="size-4 text-primary" /> WHO OWNS</h2>
          <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
            <li><span className="font-semibold text-foreground">Institution</span> owns student, enrollment, academic, and media data.</li>
            <li><span className="font-semibold text-foreground">Platform</span> owns cross-institution registry, catalog, and audit trails.</li>
            <li><span className="font-semibold text-foreground">User</span> owns PII and consent-scoped data.</li>
          </ul>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Shield className="size-4 text-emerald-600" /> WHO MANAGES</h2>
          <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
            <li><span className="font-semibold text-foreground">Institution Admin</span> manages their institution&rsquo;s records and retention.</li>
            <li><span className="font-semibold text-foreground">Platform Admin</span> manages platform-wide policies, retention defaults, and compliance.</li>
            <li><span className="font-semibold text-foreground">System</span> enforces TTL, archival, and purge jobs.</li>
          </ul>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Eye className="size-4 text-violet-600" /> WHO VIEWS</h2>
          <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
            <li><span className="font-semibold text-foreground">Scoped roles</span> — data visible only within institution + role.</li>
            <li><span className="font-semibold text-foreground">Platform Admin</span> has permission-aware, audited cross-institution read.</li>
            <li><span className="font-semibold text-foreground">No implicit global read</span> — every cross-cut is logged.</li>
          </ul>
        </div>
      </div>

      {govError && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{govError}</span>
          <button onClick={loadGov} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Lock className="size-4" /> Retention &amp; Compliance</h2>
              <div className="flex items-center gap-2">
                <button onClick={loadGov} className="text-muted-foreground hover:text-foreground" aria-label="Refresh"><RefreshCw className="size-3.5" /></button>
                <button onClick={runSweep} disabled={sweeping}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-semibold hover:bg-muted disabled:opacity-50">
                  {sweeping ? <RefreshCw className="size-3.5 animate-spin" /> : <PlayCircle className="size-3.5" />} Run sweep
                </button>
              </div>
            </div>
            {configLoading ? (
              <div className="mt-3 animate-pulse space-y-2"><div className="h-8 rounded-lg bg-muted" /><div className="h-8 rounded-lg bg-muted" /></div>
            ) : (
              <>
                <div className="mt-3 grid gap-3 sm:grid-cols-3">
                  <div className="rounded-xl border border-border bg-muted/20 p-3">
                    <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Archived audit logs</p>
                    <p className="mt-1 text-lg font-bold text-foreground">{retention?.archivedAuditCount ?? "—"}</p>
                    <p className="text-xs text-muted-foreground">of {retention?.totalAuditCount ?? "—"} total</p>
                  </div>
                  <div className="rounded-xl border border-border bg-muted/20 p-3">
                    <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Purged soft-deleted reports</p>
                    <p className="mt-1 text-lg font-bold text-foreground">{retention?.purgedSoftDeletedReports ?? "—"}</p>
                  </div>
                  <div className="rounded-xl border border-border bg-muted/20 p-3">
                    <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Last sweep</p>
                    <p className="mt-1 text-xs font-medium text-foreground break-all">{retention?.lastSweep ?? "Never run"}</p>
                  </div>
                </div>
                {config && config.length > 0 && (
                  <div className="mt-3 grid gap-3 sm:grid-cols-2">
                    {config.map((c) => (
                      <div key={c.id} className="rounded-xl border border-border bg-muted/20 p-3">
                        <p className="flex items-center gap-1 text-xs font-bold uppercase tracking-widest text-foreground"><Clock className="size-3" /> {c.configKey}</p>
                        <p className="mt-1 text-lg font-bold text-foreground">{c.configValue}{c.configKey.includes("days") ? " days" : ""}</p>
                        <p className="mt-0.5 text-xs text-muted-foreground">{c.description}</p>
                      </div>
                    ))}
                  </div>
                )}
                {!config || config.length === 0 ? (
                  <p className="mt-3 text-xs text-muted-foreground">No DATA retention config found.</p>
                ) : null}
              </>
            )}
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><FileCheck className="size-4" /> Data Quality Checks</h2>
            {configLoading ? (
              <div className="mt-3 h-24 animate-pulse rounded-xl bg-muted" />
            ) : !quality ? (
              <p className="mt-3 text-sm text-muted-foreground">Data unavailable — quality endpoint unreachable.</p>
            ) : quality.length === 0 ? (
              <p className="mt-3 text-sm text-muted-foreground">No checks returned.</p>
            ) : (
              <div className="mt-3 space-y-2">
                {quality.map((q) => (
                  <div key={q.name} className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-2.5">
                    <div>
                      <p className="text-sm font-medium text-foreground">{q.name}</p>
                      <p className="text-xs text-muted-foreground">{q.detail}</p>
                    </div>
                    <div className="flex items-center gap-2 text-xs">
                      <span className="font-bold tabular-nums text-foreground">{q.count ?? "—"}</span>
                      <span className={`rounded-full border px-2 py-0.5 font-semibold ${q.status === "OK" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : q.status === "WARN" ? "border-amber-200 bg-amber-50 text-amber-700" : "border-slate-200 bg-slate-50 text-slate-600"}`}>
                        {q.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Download className="size-4" /> Export (CSV, audit-logged)</h2>
            <p className="mt-1 text-xs text-muted-foreground">Every export writes an audit row.</p>
            <div className="mt-3 space-y-2">
              {(["users", "institutions", "audit"] as const).map((t) => (
                <button key={t} onClick={() => handleExport(t)} disabled={exporting}
                  className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-semibold hover:bg-muted disabled:opacity-50">
                  {exporting ? <RefreshCw className="size-4 animate-spin" /> : <Download className="size-4" />} Export {t}
                </button>
              ))}
            </div>
            {exportError && <p className="mt-2 flex items-center gap-1 text-xs text-red-600"><AlertCircle className="size-3.5" />{exportError}</p>}
            {exportSuccess && <p className="mt-2 text-xs text-emerald-600">Export started — check downloads.</p>}
          </div>

          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
            <p className="flex items-center gap-2 text-sm font-bold text-amber-800"><HardDrive className="size-4" /> Storage</p>
            <p className="mt-1 text-xs leading-relaxed text-amber-800/80">Per-institution usage surfaces from verified metering when available — no estimates.</p>
          </div>
        </div>
      </div>
    </div>
  )
}
