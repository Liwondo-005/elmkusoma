"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Plug2, RefreshCw, AlertCircle, CheckCircle2, XCircle, AlertTriangle, HelpCircle, Mail, CreditCard, HardDrive, MessageSquare, Radio, Bug, Webhook } from "lucide-react"
import { platformAdminApi, type IntegrationStatus, type WebhookEvent, type PageResponse } from "@/lib/platform-admin-api"

type ConnStatus = "Operational" | "Degraded" | "Failing" | "Unknown"

const META: Record<string, { name: string; description: string; icon: any; color: string }> = {
  livekit: { name: "LiveKit", description: "Real-time live classes & SFU", icon: Radio, color: "bg-violet-500" },
  payment: { name: "Payment", description: "Fees, entitlements, and payouts", icon: CreditCard, color: "bg-emerald-500" },
  email: { name: "Email", description: "Transactional & broadcast email", icon: Mail, color: "bg-blue-500" },
  sms: { name: "SMS", description: "OTP and notifications", icon: MessageSquare, color: "bg-teal-500" },
  storage: { name: "Storage", description: "Media & document storage", icon: HardDrive, color: "bg-amber-500" },
  authentication: { name: "Authentication", description: "Local identity store", icon: Bug, color: "bg-rose-500" },
  notification: { name: "Notification Services", description: "Platform + learner notifications", icon: Webhook, color: "bg-sky-500" },
}

function toConn(s: string | null | undefined): ConnStatus {
  if (!s) return "Unknown"
  const l = s.toLowerCase()
  if (l.includes("operational")) return "Operational"
  if (l.includes("degrad")) return "Degraded"
  if (l.includes("fail")) return "Failing"
  return "Unknown"
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
  const t = useTranslations("platformAdmin");
  const [integrations, setIntegrations] = useState<IntegrationStatus[]>([])
  const [webhooks, setWebhooks] = useState<WebhookEvent[]>([])
  const [failedOnly, setFailedOnly] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [probing, setProbing] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const [list, hooks] = await Promise.all([
        platformAdminApi.listIntegrations(),
        platformAdminApi.listWebhookEvents(undefined, failedOnly, 0, 10),
      ])
      setIntegrations(list)
      setWebhooks((hooks as PageResponse<WebhookEvent>).content ?? [])
    } catch (e: any) {
      setError(e.message || t("integrations.failedToLoadIntegrations"))
    } finally { setLoading(false) }
  }, [failedOnly])

  useEffect(() => { load() }, [load])

  const probe = async (key: string) => {
    setProbing(key)
    try {
      const updated = await platformAdminApi.probeIntegration(key)
      setIntegrations(prev => prev.map(i => (i.key === key ? updated : i)))
    } catch (e: any) {
      setError(e.message || t("integrations.probeFailedFor", { p0: key }))
    } finally { setProbing(null) }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-slate-800 text-white"><Plug2 className="size-4" /></span> {t("integrations.integrationManagement")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("integrations.registryBackedConnectionStatus")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("integrations.refresh")}</button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("integrations.retry")}</button>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {loading && !integrations.length && Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-44 animate-pulse rounded-2xl bg-muted" />
        ))}
        {integrations.map((it) => {
          const meta = META[it.key] ?? { name: it.name, description: it.category ?? "", icon: Plug2, color: "bg-slate-500" }
          const Icon = meta.icon
          const status = toConn(it.connectionStatus)
          return (
            <div key={it.key} className="rounded-2xl border border-border bg-card p-5 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className={`flex size-10 items-center justify-center rounded-xl text-white ${meta.color}`}><Icon className="size-5" /></div>
                  <div>
                    <h3 className="font-semibold text-foreground">{meta.name}</h3>
                    <p className="text-xs text-muted-foreground">{meta.description}</p>
                  </div>
                </div>
                <StatusPill status={status} />
              </div>
              <div className="mt-4 grid grid-cols-3 gap-3 text-xs">
                <div className="rounded-xl border border-border bg-muted/20 p-3">
                  <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("integrations.config")}</p>
                  <p className="mt-1 font-medium text-foreground">{it.configStatus}</p>
                </div>
                <div className="rounded-xl border border-border bg-muted/20 p-3">
                  <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("integrations.lastSuccess")}</p>
                  <p className="mt-1 font-medium text-foreground">{it.lastSuccessAt ? new Date(it.lastSuccessAt).toLocaleString() : "—"}</p>
                </div>
                <div className="rounded-xl border border-border bg-muted/20 p-3">
                  <p className="font-bold uppercase tracking-widest text-muted-foreground">{t("integrations.failures")}</p>
                  <p className="mt-1 font-medium text-foreground">{it.failureCount ?? 0}</p>
                </div>
              </div>
              {it.webhookStatus && (
                <p className="mt-3 text-xs text-muted-foreground">{t("integrations.webhook")}<span className="font-semibold">{it.webhookStatus}</span> {t("integrations.retry2")}{it.retryStatus ?? "—"}</p>
              )}
              {it.probeDetail && <p className="mt-1 text-xs text-muted-foreground">{it.probeDetail}</p>}
              <button
                onClick={() => probe(it.key)}
                disabled={probing === it.key}
                className="mt-3 inline-flex items-center gap-2 rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-semibold hover:bg-muted disabled:opacity-50"
              >
                {probing === it.key ? <RefreshCw className="size-3.5 animate-spin" /> : <Bug className="size-3.5" />} {t("integrations.runProbe")}</button>
            </div>
          )
        })}
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-sm font-bold text-foreground"><Webhook className="size-4" /> {t("integrations.webhookEvents")}</h2>
          <label className="flex items-center gap-2 text-xs text-muted-foreground">
            <input type="checkbox" checked={failedOnly} onChange={e => setFailedOnly(e.target.checked)} className="size-3.5" />
            {t("integrations.failedOnly")}</label>
        </div>
        {loading ? (
          <div className="mt-3 h-24 animate-pulse rounded-xl bg-muted" />
        ) : webhooks.length === 0 ? (
          <p className="mt-3 text-sm text-muted-foreground">{t("integrations.noWebhookEventsRecorded")}</p>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border text-muted-foreground">
                  <th className="py-2 pr-4 font-semibold">{t("integrations.source")}</th>
                  <th className="py-2 pr-4 font-semibold">{t("integrations.event")}</th>
                  <th className="py-2 pr-4 font-semibold">{t("integrations.verification")}</th>
                  <th className="py-2 pr-4 font-semibold">{t("integrations.result")}</th>
                  <th className="py-2 pr-4 font-semibold">{t("integrations.received")}</th>
                </tr>
              </thead>
              <tbody>
                {webhooks.map(w => (
                  <tr key={w.id} className="border-b border-border/50">
                    <td className="py-2 pr-4 font-medium">{w.source}</td>
                    <td className="py-2 pr-4">{w.eventType ?? "—"}</td>
                    <td className="py-2 pr-4">{w.verificationStatus}</td>
                    <td className="py-2 pr-4">
                      <span className={`rounded-full border px-2 py-0.5 font-semibold ${w.processingResult === "SUCCESS" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-red-200 bg-red-50 text-red-700"}`}>
                        {w.processingResult}
                      </span>
                    </td>
                    <td className="py-2 pr-4 text-muted-foreground">{new Date(w.receivedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
