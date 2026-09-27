"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { HeartPulse, RefreshCw, AlertCircle, Database, Server, Radio, HardDrive, Cpu, CheckCircle2, XCircle, AlertTriangle, HelpCircle, Archive, Activity } from "lucide-react"
import { platformAdminApi, type PlatformHealth, type BackupStatus } from "@/lib/platform-admin-api"

type HealthStatus = "Operational" | "Degraded" | "Failing" | "Unknown" | "Stale"

function statusFromString(s: string | null | undefined): HealthStatus {
  if (!s) return "Unknown"
  const l = s.toLowerCase()
  if (l.includes("up") || l.includes("operational") || l.includes("ok") || l.includes("healthy")) return "Operational"
  if (l.includes("degrad")) return "Degraded"
  if (l.includes("stale")) return "Stale"
  if (l.includes("down") || l.includes("fail") || l.includes("error")) return "Failing"
  return "Unknown"
}

function StatusBadge({ status }: { status: HealthStatus }) {
  const map: Record<HealthStatus, string> = {
    Operational: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
    Degraded: "bg-amber-500/10 text-amber-700 border-amber-200",
    Stale: "bg-amber-500/10 text-amber-700 border-amber-200",
    Failing: "bg-red-500/10 text-red-700 border-red-200",
    Unknown: "bg-slate-500/10 text-slate-600 border-slate-200",
  }
  const Icon = status === "Operational" ? CheckCircle2 : status === "Failing" ? XCircle : (status === "Degraded" || status === "Stale") ? AlertTriangle : HelpCircle
  return <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold ${map[status]}`}><Icon className="size-3.5" />{status}</span>
}

function HealthRow({ icon: Icon, label, status, detail }: { icon: any; label: string; status: HealthStatus; detail?: string | null }) {
  const dot: Record<HealthStatus, string> = { Operational: "bg-emerald-500", Degraded: "bg-amber-500", Stale: "bg-amber-500", Failing: "bg-red-500", Unknown: "bg-slate-300" }
  return (
    <div className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
      <span className="flex items-center gap-3 text-sm font-medium"><span className={`size-2 rounded-full ${dot[status]}`} /><Icon className="size-4 text-muted-foreground" />{label}{detail && <span className="text-xs text-muted-foreground">· {detail}</span>}</span>
      <StatusBadge status={status} />
    </div>
  )
}

export default function PlatformHealthPage() {
  const t = useTranslations("platformAdmin");
  const [health, setHealth] = useState<PlatformHealth | null>(null)
  const [backup, setBackup] = useState<BackupStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const [h, b] = await Promise.all([
        platformAdminApi.getHealth(),
        platformAdminApi.getBackupStatus().catch(() => null),
      ])
      setHealth(h)
      setBackup(b)
    } catch (e: any) {
      setError(e.message || t("health.failedToLoadHealth"))
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-emerald-500 text-white"><HeartPulse className="size-4" /></span> {t("health.platformOperationsHealth")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("health.realProbesOnlyNever")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("health.refresh")}</button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("health.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h2 className="text-sm font-bold text-foreground">{t("health.coreIntegrationHealth")}</h2>
        {loading ? (
          <div className="mt-4 space-y-2 animate-pulse"><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /></div>
        ) : health ? (
          <div className="mt-4 space-y-2">
            <HealthRow icon={Database} label="Database" status={statusFromString(health.databaseStatus)} detail={health.databaseStatus} />
            <HealthRow icon={Server} label="API" status={statusFromString(health.apiStatus)} detail={health.apiStatus} />
            <HealthRow icon={Radio} label="LiveKit / Realtime" status={statusFromString(health.livekitStatus)} detail={health.livekitStatus} />
            <HealthRow icon={HardDrive} label="Storage / Media" status={statusFromString(health.storageStatus)} detail={health.storageStatus} />
            <HealthRow icon={Cpu} label="Background Jobs" status={statusFromString(health.backgroundJobsStatus)} detail={health.heartbeatAt ? `heartbeat ${health.heartbeatAt}` : health.backgroundJobsStatus} />
            <HealthRow icon={Activity} label="Payments" status={statusFromString(health.paymentsStatus)} detail={health.paymentsStatus} />
            <HealthRow icon={HeartPulse} label="Notifications" status={statusFromString(health.notificationsStatus)} detail={health.notificationsStatus} />
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div className="rounded-xl border border-border bg-muted/20 p-3 text-center"><p className="text-lg font-bold tabular-nums">{health.totalUsers.toLocaleString()}</p><p className="text-xs text-muted-foreground">{t("health.totalUsers")}</p></div>
              <div className="rounded-xl border border-border bg-muted/20 p-3 text-center"><p className="text-lg font-bold tabular-nums">{health.totalInstitutions.toLocaleString()}</p><p className="text-xs text-muted-foreground">{t("health.totalInstitutions")}</p></div>
            </div>
          </div>
        ) : <p className="mt-4 text-sm text-muted-foreground">{t("health.dataUnavailableHealthEndpoint")}</p>}
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Archive className="size-4" /> {t("health.backupStatusRealArtifacts")}</h2>
        {loading ? (
          <div className="mt-3 h-20 animate-pulse rounded-xl bg-muted" />
        ) : !backup ? (
          <p className="mt-3 text-sm text-muted-foreground">{t("health.dataUnavailableBackupStatus")}</p>
        ) : backup.status === "NEVER_RUN" || !backup.status ? (
          <div className="mt-3 rounded-xl border border-dashed border-amber-300 bg-amber-50 p-4 text-sm text-amber-800">
            {t("health.backupHasNeverRun")}<code className="rounded bg-white/60 px-1">{backup.statusFilePath}</code>{t("health.run")}<code className="rounded bg-white/60 px-1">infrastructure/scripts/backup-db.sh</code> {t("health.toProduceAReal")}</div>
        ) : (
          <div className="mt-3 grid gap-3 sm:grid-cols-3 text-xs">
            <div className="rounded-xl border border-border bg-muted/20 p-3">
              <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("health.status")}</p>
              <p className={`mt-1 text-lg font-bold ${backup.status === "SUCCESS" ? "text-emerald-600" : "text-red-600"}`}>{backup.status}</p>
<p className="mt-0.5 text-muted-foreground">{backup.integrityOk == null ? t("health.integrityUnknown") : backup.integrityOk ? t("health.integrityOk") : t("health.integrityFailed")}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/20 p-3">
              <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("health.lastRun")}</p>
              <p className="mt-1 font-medium text-foreground">{backup.lastRunAt ? new Date(backup.lastRunAt).toLocaleString() : "—"}</p>
              <p className="mt-0.5 text-muted-foreground">{backup.lastFile ?? "—"}{backup.lastFileSizeBytes ? ` · ${(backup.lastFileSizeBytes / 1024 / 1024).toFixed(1)} MB` : ""}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/20 p-3">
              <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("health.dumpsOnDisk")}</p>
              <p className="mt-1 text-lg font-bold text-foreground">{backup.backupCount ?? "—"}</p>
              <p className="mt-0.5 text-muted-foreground">{t("health.newest")}{backup.newestBackupAt ? new Date(backup.newestBackupAt).toLocaleDateString() : "—"}</p>
            </div>
            <div className="sm:col-span-3 rounded-xl border border-border bg-muted/20 p-3">
              <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("health.restoreProcedure")}</p>
              <pre className="mt-1 whitespace-pre-wrap text-[11px] leading-relaxed text-muted-foreground">{backup.restoreProcedure}</pre>
              <p className="mt-2 text-muted-foreground">
                {t("health.scriptsBackup")}{backup.scriptPresent ? "✓" : "✗"} {t("health.restore")}{backup.restoreScriptPresent ? "✓" : "✗"}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
