"use client"

import { useEffect, useState } from "react"
import { AlertTriangle, Plus, Loader2, Clock, CheckCircle2 } from "lucide-react"
import { platformAdminApi, type IncidentSummary, type PageResponse } from "@/lib/platform-admin-api"

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: "bg-red-50 text-red-700 border-red-200",
  HIGH: "bg-orange-50 text-orange-700 border-orange-200",
  MEDIUM: "bg-yellow-50 text-yellow-700 border-yellow-200",
  LOW: "bg-blue-50 text-blue-700 border-blue-200",
  INFO: "bg-gray-50 text-gray-700 border-gray-200",
}

const STATUS_ICONS: Record<string, typeof Clock> = {
  DETECTED: AlertTriangle,
  INVESTIGATING: Clock,
  CONTAINED: Clock,
  RESOLVED: CheckCircle2,
  REVIEWED: CheckCircle2,
}

export default function IncidentsPage() {
  const [data, setData] = useState<PageResponse<IncidentSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState("")
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ title: "", description: "", category: "PLATFORM", severity: "MEDIUM" })
  const [saving, setSaving] = useState(false)

  const load = (p = 0) => {
    setLoading(true)
    platformAdminApi.listIncidents(p, 20, filter || undefined).then(setData).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [filter])

  async function handleCreate() {
    if (!form.title) return
    setSaving(true)
    try {
      await platformAdminApi.createIncident(form)
      setForm({ title: "", description: "", category: "PLATFORM", severity: "MEDIUM" })
      setShowCreate(false)
      load()
    } finally { setSaving(false) }
  }

  async function resolveIncident(id: string) {
    await platformAdminApi.updateIncidentStatus(id, "RESOLVED")
    load()
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Incident Management</h1>
          <p className="mt-1 text-sm text-muted-foreground">Track and resolve platform incidents</p>
        </div>
        <button onClick={() => setShowCreate(!showCreate)} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> Report Incident
        </button>
      </div>

      <div className="flex gap-2">
        {["", "DETECTED", "INVESTIGATING", "RESOLVED"].map(s => (
          <button key={s} onClick={() => setFilter(s)} className={`rounded-lg px-3 py-1.5 text-xs font-medium ${filter === s ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}>
            {s || "All"}
          </button>
        ))}
      </div>

      {showCreate && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">Report Incident</h2>
          <input placeholder="Title" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} className="w-full rounded-xl border border-border bg-background px-4 py-2 text-sm" />
          <textarea placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} className="w-full rounded-xl border border-border bg-background px-4 py-2 text-sm" />
          <div className="grid grid-cols-2 gap-4">
            <select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm">
              {["PLATFORM", "SECURITY", "PAYMENT", "LIVE", "DATABASE", "MEDIA", "NOTIFICATION", "API", "PERFORMANCE", "INTEGRATION"].map(c => <option key={c}>{c}</option>)}
            </select>
            <select value={form.severity} onChange={e => setForm({ ...form, severity: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm">
              {["LOW", "MEDIUM", "HIGH", "CRITICAL"].map(s => <option key={s}>{s}</option>)}
            </select>
          </div>
          <div className="flex gap-2">
            <button onClick={handleCreate} disabled={saving || !form.title} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50">
              {saving ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />} Create
            </button>
            <button onClick={() => setShowCreate(false)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground">Cancel</button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <CheckCircle2 className="mx-auto size-10 text-green-500/50" />
          <p className="mt-3 text-sm text-muted-foreground">No incidents reported</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data.content.map(inc => {
            const StatusIcon = STATUS_ICONS[inc.status] || Clock
            return (
              <div key={inc.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3 min-w-0">
                    <StatusIcon className="size-5 shrink-0 mt-0.5 text-muted-foreground" />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-semibold text-foreground">{inc.title}</p>
                        <span className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold ${SEVERITY_COLORS[inc.severity] || ""}`}>{inc.severity}</span>
                        <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{inc.status}</span>
                      </div>
                      {inc.description && <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{inc.description}</p>}
                      <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
                        <span>{inc.category}</span>
                        {inc.affectedService && <span>Affected: {inc.affectedService}</span>}
                        <span>{new Date(inc.detectedAt).toLocaleString("en-GB")}</span>
                      </div>
                    </div>
                  </div>
                  {inc.status !== "RESOLVED" && inc.status !== "REVIEWED" && (
                    <button onClick={() => resolveIncident(inc.id)} className="shrink-0 rounded-xl border border-green-200 px-3 py-1.5 text-xs font-medium text-green-600 hover:bg-green-50">
                      Resolve
                    </button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
