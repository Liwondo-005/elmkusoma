"use client"

import { useEffect, useState } from "react"
import { Bell, Send, Loader2 } from "lucide-react"
import { platformAdminApi, type NotificationSummary, type PageResponse } from "@/lib/platform-admin-api"

export default function CommunicationsPage() {
  const [data, setData] = useState<PageResponse<NotificationSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [showSend, setShowSend] = useState(false)
  const [form, setForm] = useState({ title: "", message: "", notificationType: "ANNOUNCEMENT", priority: "NORMAL", targetAudience: "ALL" })
  const [saving, setSaving] = useState(false)

  const load = (p = 0) => {
    setLoading(true)
    platformAdminApi.listNotifications(p, 20).then(setData).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleSend() {
    if (!form.title || !form.message) return
    setSaving(true)
    try {
      await platformAdminApi.sendNotification(form)
      setForm({ title: "", message: "", notificationType: "ANNOUNCEMENT", priority: "NORMAL", targetAudience: "ALL" })
      setShowSend(false)
      load()
    } finally { setSaving(false) }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Communications</h1>
          <p className="mt-1 text-sm text-muted-foreground">Platform-wide notifications and announcements</p>
        </div>
        <button onClick={() => setShowSend(!showSend)} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Send className="size-4" /> Send Notification
        </button>
      </div>

      {showSend && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">Send Platform Notification</h2>
          <input placeholder="Title" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} className="w-full rounded-xl border border-border bg-background px-4 py-2 text-sm" />
          <textarea placeholder="Message" value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} rows={3} className="w-full rounded-xl border border-border bg-background px-4 py-2 text-sm" />
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
              {saving ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />} Send
            </button>
            <button onClick={() => setShowSend(false)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground">Cancel</button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Bell className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">No notifications sent yet</p>
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
                    <span>To: {n.targetAudience || "All"}</span>
                    <span>Sent by: {n.sentBy || "System"}</span>
                    <span>{new Date(n.sentAt).toLocaleString("en-GB")}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
