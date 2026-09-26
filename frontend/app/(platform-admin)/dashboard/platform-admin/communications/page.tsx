"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Bell, Send, Loader2, Inbox, RefreshCw, AlertCircle } from "lucide-react"
import { platformAdminApi, type NotificationSummary, type PageResponse, type CommunicationDelivery } from "@/lib/platform-admin-api"

export default function CommunicationsPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [data, setData] = useState<PageResponse<NotificationSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [showSend, setShowSend] = useState(false)
  const [form, setForm] = useState({ title: "", message: "", notificationType: "ANNOUNCEMENT", priority: "NORMAL", targetAudience: "ALL" })
  const [saving, setSaving] = useState(false)
  const [delivery, setDelivery] = useState<CommunicationDelivery | null>(null)
  const [deliveryError, setDeliveryError] = useState<string | null>(null)
  const [pageIndex, setPageIndex] = useState(0)

  const load = useCallback(async (p = 0) => {
    setLoading(true)
    try {
      setData(await platformAdminApi.listNotifications(p, 20))
    } catch (e: any) {
      setDeliveryError(e.message || t("communications.failedToLoadNotifications"))
      setData(null)
    } finally { setLoading(false) }
  }, [])

  const loadDelivery = useCallback(async () => {
    setDeliveryError(null)
    try {
      setDelivery(await platformAdminApi.getCommunicationDelivery())
    } catch (e: any) {
      setDelivery(null)
      setDeliveryError(e.message || t("communications.deliveryStatsUnavailable"))
    }
  }, [])

  useEffect(() => { load(pageIndex) }, [load, pageIndex])
  useEffect(() => { loadDelivery() }, [loadDelivery])

  async function handleSend() {
    if (!form.title || !form.message) return
    setSaving(true)
    try {
      await platformAdminApi.sendNotification(form)
      setForm({ title: "", message: "", notificationType: "ANNOUNCEMENT", priority: "NORMAL", targetAudience: "ALL" })
      setShowSend(false)
      await Promise.all([load(pageIndex), loadDelivery()])
    } finally { setSaving(false) }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("communications.communications")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("communications.platformWideNotificationsAnd")}</p>
        </div>
        <button onClick={() => setShowSend(!showSend)} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Send className="size-4" /> {t("communications.sendNotification")}</button>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-base font-semibold text-foreground"><Inbox className="size-4" /> {t("communications.deliveryStats")}</h2>
          <button onClick={loadDelivery} className="text-muted-foreground hover:text-foreground" aria-label={t("communications.refreshDelivery")}><RefreshCw className="size-3.5" /></button>
        </div>
        {deliveryError && <p className="mt-2 flex items-center gap-1 text-xs text-red-600"><AlertCircle className="size-3.5" />{deliveryError}</p>}
        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { label: t("communications.platformNotifications"), value: delivery?.platformNotifications },
            { label: t("communications.learnerNotifications"), value: delivery?.learnerNotifications },
            { label: t("communications.learnerRead"), value: delivery?.learnerRead },
            { label: t("communications.learnerUnread"), value: delivery?.learnerUnread },
          ].map(s => (
            <div key={s.label} className="rounded-xl border border-border bg-background p-4">
              <p className="text-xs text-muted-foreground">{s.label}</p>
              {s.value === null || s.value === undefined
                ? <p className="mt-1 text-sm font-semibold text-muted-foreground">{t("communications.dataUnavailable")}</p>
                : <p className="mt-1 text-xl font-bold tabular-nums">{s.value.toLocaleString()}</p>}
            </div>
          ))}
        </div>
        <p className="mt-3 text-xs text-muted-foreground">
          {t("communications.readRate")}{delivery?.learnerReadRate != null ? `${delivery.learnerReadRate}%` : "—"} · {delivery?.deliveryNote ?? "Data unavailable"}
        </p>
      </div>

      {showSend && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">{t("communications.sendPlatformNotification")}</h2>
          <input placeholder={t("communications.title")} value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} className="w-full rounded-xl border border-border bg-background px-4 py-2 text-sm" />
          <textarea placeholder={t("communications.message")} value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} rows={3} className="w-full rounded-xl border border-border bg-background px-4 py-2 text-sm" />
          <div className="grid grid-cols-3 gap-4">
            <select value={form.notificationType} onChange={e => setForm({ ...form, notificationType: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm">
              {["ANNOUNCEMENT", "SYSTEM", "SECURITY", "MAINTENANCE", "FEATURE"].map(t => <option key={t}>{t}</option>)}
            </select>
            <select value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm">
              {["LOW", "NORMAL", "HIGH", "URGENT"].map(p => <option key={p}>{p}</option>)}
            </select>
            <select value={form.targetAudience} onChange={e => setForm({ ...form, targetAudience: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm">
              {["ALL", "ADMINS", "INSTITUTIONS", "PROVIDERS", "TEACHERS", "STUDENTS", "PARENTS"].map(a => <option key={a}>{a}</option>)}
            </select>
          </div>
          <div className="flex gap-2">
            <button onClick={handleSend} disabled={saving || !form.title || !form.message} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50">
              {saving ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />} {t("communications.send")}</button>
            <button onClick={() => setShowSend(false)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground">{tc("cancel")}</button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Bell className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">{t("communications.noNotificationsSentYet")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data.content.map(n => (
            <div key={n.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">{n.title}</p>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{n.notificationType}</span>
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${n.priority === "URGENT" || n.priority === "HIGH" ? "bg-red-50 text-red-700" : "bg-blue-50 text-blue-700"}`}>{n.priority}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{n.message}</p>
                  <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
                    <span>{t("communications.toAudience", { audience: n.targetAudience || t("communications.allAudiences") })}</span>
                    <span>{t("communications.sentByUser", { user: n.sentBy || t("communications.systemUser") })}</span>
                    <span>{new Date(n.sentAt).toLocaleString("en-GB")}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <button disabled={pageIndex === 0} onClick={() => setPageIndex(i => i - 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">{tc("previous")}</button>
          <span className="text-xs text-muted-foreground">{t("communications.pageOf", { p0: pageIndex + 1, p1: data.totalPages })}</span>
          <button disabled={data.last} onClick={() => setPageIndex(i => i + 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">{tc("next")}</button>
        </div>
      )}
    </div>
  )
}
