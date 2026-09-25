"use client"

import { useEffect, useState } from "react"
import { Package, Plus, Search, Loader2, CheckCircle, XCircle } from "lucide-react"
import { platformAdminApi, type ServiceSummary, type PageResponse } from "@/lib/platform-admin-api"

const CATEGORIES = ["LEARNING", "LIVE", "EVENTS", "MEDIA", "RESOURCES", "CERTIFICATES", "COMMERCE", "ANALYTICS", "COMMUNICATION", "SERVICES"]

export default function ServicesPage() {
  const [data, setData] = useState<PageResponse<ServiceSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [category, setCategory] = useState<string>("")
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ name: "", code: "", description: "", category: "LEARNING" })
  const [saving, setSaving] = useState(false)

  const load = (p = 0) => {
    setLoading(true)
    platformAdminApi.listServices(p, 50, category || undefined).then(setData).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [category])

  async function handleCreate() {
    if (!form.name || !form.code) return
    setSaving(true)
    try {
      await platformAdminApi.createService(form)
      setForm({ name: "", code: "", description: "", category: "LEARNING" })
      setShowCreate(false)
      load()
    } finally { setSaving(false) }
  }

  async function toggleService(svc: ServiceSummary) {
    await platformAdminApi.updateService(svc.id, { isActive: !svc.isActive })
    load()
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Service Catalogue</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage platform services available to providers</p>
        </div>
        <button onClick={() => setShowCreate(!showCreate)} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> Add Service
        </button>
      </div>

      <div className="flex flex-wrap gap-2">
        <button onClick={() => setCategory("")} className={`rounded-lg px-3 py-1.5 text-xs font-medium ${!category ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}>All</button>
        {CATEGORIES.map(c => (
          <button key={c} onClick={() => setCategory(c)} className={`rounded-lg px-3 py-1.5 text-xs font-medium ${category === c ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}>{c}</button>
        ))}
      </div>

      {showCreate && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">Create Service</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <input placeholder="Service Name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm" />
            <input placeholder="Service Code (e.g. COURSES)" value={form.code} onChange={e => setForm({ ...form, code: e.target.value.toUpperCase() })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm uppercase" />
            <input placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm" />
            <select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} className="rounded-xl border border-border bg-background px-4 py-2 text-sm">
              {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="flex gap-2">
            <button onClick={handleCreate} disabled={saving || !form.name || !form.code} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />} Create
            </button>
            <button onClick={() => setShowCreate(false)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground hover:bg-muted/50">Cancel</button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Package className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">No services configured yet</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map(svc => (
            <div key={svc.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-semibold text-foreground">{svc.name}</p>
                  <p className="text-xs text-muted-foreground font-mono">{svc.code}</p>
                </div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${svc.isActive ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                  {svc.isActive ? "ACTIVE" : "INACTIVE"}
                </span>
              </div>
              {svc.description && <p className="text-xs text-muted-foreground line-clamp-2">{svc.description}</p>}
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span className="rounded-md bg-muted px-2 py-0.5">{svc.category}</span>
                {svc.monthlyPrice && <span>{svc.currency} {svc.monthlyPrice.toLocaleString()}/mo</span>}
              </div>
              <button onClick={() => toggleService(svc)} className={`w-full rounded-xl border px-3 py-1.5 text-xs font-medium ${svc.isActive ? "border-red-200 text-red-600 hover:bg-red-50" : "border-green-200 text-green-600 hover:bg-green-50"}`}>
                {svc.isActive ? "Deactivate" : "Activate"}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
