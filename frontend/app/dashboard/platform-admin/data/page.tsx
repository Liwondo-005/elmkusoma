"use client"

import { useEffect, useState } from "react"
import { Database, Shield, Eye, Lock, Download, AlertTriangle, RefreshCw, FileCheck, Users, Building2, HardDrive, AlertCircle, Clock } from "lucide-react"
import { platformAdminApi, type PlatformConfigItem } from "@/lib/platform-admin-api"

export default function PlatformDataGovernancePage() {
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState<string | null>(null)
  const [exportSuccess, setExportSuccess] = useState(false)
  const [config, setConfig] = useState<PlatformConfigItem[] | null>(null)
  const [configError, setConfigError] = useState<string | null>(null)
  const [configLoading, setConfigLoading] = useState(true)

  useEffect(() => {
    let alive = true
    ;(async () => {
      setConfigLoading(true); setConfigError(null)
      try {
        const items = await platformAdminApi.listConfig("DATA")
        if (alive) setConfig(items)
      } catch (e: any) {
        if (alive) { setConfig(null); setConfigError(e.message || "Data unavailable") }
      } finally { if (alive) setConfigLoading(false) }
    })()
    return () => { alive = false }
  }, [])

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
        <p className="mt-1 text-sm text-muted-foreground">Ownership, access, retention, and quality — who owns, manages, and views data across the platform. Per platform_admin.md data governance.</p>
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

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Lock className="size-4" /> Retention &amp; Compliance</h2>
              <button onClick={() => platformAdminApi.listConfig("DATA").then(setConfig).catch((e) => setConfigError(e.message))} className="text-muted-foreground hover:text-foreground"><RefreshCw className="size-3.5" /></button>
            </div>
            {configLoading ? (
              <div className="mt-3 animate-pulse space-y-2"><div className="h-8 rounded-lg bg-muted" /><div className="h-8 rounded-lg bg-muted" /></div>
            ) : configError ? (
              <p className="mt-3 flex items-center gap-1 text-xs text-red-600"><AlertCircle className="size-3.5" />{configError} — retention config unavailable.</p>
            ) : !config || config.length === 0 ? (
              <p className="mt-3 text-xs text-muted-foreground">No DATA retention config found. Expected keys from V69 migration.</p>
            ) : (
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
            <div className="mt-3 grid gap-3 sm:grid-cols-2 text-xs">
              <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="font-bold uppercase tracking-widest">Access</p><p className="mt-1 text-muted-foreground">X-Institution-Id + JWT role gate every read. Cross-institution queries go through platform-admin API only.</p></div>
              <div className="rounded-xl border border-border bg-muted/20 p-3"><p className="font-bold uppercase tracking-widest">Audit</p><p className="mt-1 text-muted-foreground">All data access and exports are audit-logged with actor, purpose, and scope.</p></div>
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><FileCheck className="size-4" /> Data Quality Issues</h2>
            <div className="mt-3 rounded-xl border border-dashed border-border bg-muted/30 p-8 text-center">
              <AlertTriangle className="mx-auto size-8 text-amber-500" />
              <p className="mt-2 text-sm font-semibold">No data quality issues reported</p>
              <p className="mt-1 text-xs text-muted-foreground max-w-md mx-auto">Quality signals (duplicate users, orphan enrollments, stale media, missing consent) will surface here when the data-quality endpoint is available. No fabricated issues — Data unavailable when not wired.</p>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Download className="size-4" /> Export (CSV, audit-logged)</h2>
            <p className="mt-1 text-xs text-muted-foreground">Platform-wide export via <code className="rounded bg-muted px-1">GET /v1/platform-admin/export?type=…</code>. Every export writes an audit row.</p>
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
            <p className="mt-1 text-xs leading-relaxed text-amber-800/80">Storage and retention quotas are pending platform policy. When available, per-institution usage and growth will be shown from verified metering — no estimates.</p>
          </div>
        </div>
      </div>
    </div>
  )
}
