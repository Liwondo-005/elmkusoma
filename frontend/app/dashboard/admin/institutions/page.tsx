"use client"

import { useEffect, useState } from "react"
import { Building2, Plus, Pencil, Trash2, Loader2, X, Search, MapPin, Globe, Phone, Mail } from "lucide-react"
import { institutionApi, type Institution, type CreateInstitutionRequest } from "@/lib/api"

const TYPES = ["PRIMARY", "SECONDARY", "COLLEGE", "UNIVERSITY", "VETA", "OTHER"]

export default function InstitutionsPage() {
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState<Institution | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [page, setPage] = useState(0)
  const [saving, setSaving] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)

  const [form, setForm] = useState<CreateInstitutionRequest>({
    name: "",
    description: "",
    type: "PRIMARY",
    email: "",
    phone: "",
    address: "",
    city: "",
    country: "Tanzania",
    website: "",
  })

  const loadData = async () => {
    try {
      setLoading(true)
      const res = await institutionApi.list(page, 20)
      setInstitutions(res.content)
      setTotal(res.totalElements)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load institutions")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [page])

  const resetForm = () => {
    setForm({ name: "", description: "", type: "PRIMARY", email: "", phone: "", address: "", city: "", country: "Tanzania", website: "" })
    setEditing(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editing) {
        await institutionApi.update(editing.id, form)
      } else {
        await institutionApi.create(form)
      }
      setShowForm(false)
      resetForm()
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save institution")
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (inst: Institution) => {
    setForm({
      name: inst.name,
      description: inst.description || "",
      type: inst.type,
      email: inst.email || "",
      phone: inst.phone || "",
      address: inst.address || "",
      city: inst.city || "",
      country: inst.country || "",
      website: inst.website || "",
    })
    setEditing(inst)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await institutionApi.delete(id)
      setDeleteConfirm(null)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete institution")
    }
  }

  const filtered = institutions.filter((i) =>
    i.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (i.city || "").toLowerCase().includes(searchQuery.toLowerCase()) ||
    (i.type || "").toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Institutions</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage schools and education institutions.</p>
        </div>
        <button
          onClick={() => { resetForm(); setShowForm(true) }}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" />
          Add Institution
        </button>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search institutions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <span className="text-sm text-muted-foreground">{total} institutions</span>
      </div>

      {error && (
        <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-xl border border-border bg-card p-12 text-center">
          <Building2 className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No institutions found</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            {searchQuery ? "Try a different search term." : "Add your first institution to get started."}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {filtered.map((inst) => (
            <div key={inst.id} className="rounded-xl border border-border bg-card p-5 space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
                    <Building2 className="size-5 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">{inst.name}</h3>
                    <span className="inline-block rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{inst.type}</span>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button onClick={() => handleEdit(inst)} className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground">
                    <Pencil className="size-4" />
                  </button>
                  {deleteConfirm === inst.id ? (
                    <div className="flex items-center gap-1">
                      <button onClick={() => handleDelete(inst.id)} className="rounded-lg bg-destructive px-2 py-1 text-xs text-destructive-foreground">Delete</button>
                      <button onClick={() => setDeleteConfirm(null)} className="rounded-lg bg-muted px-2 py-1 text-xs">Cancel</button>
                    </div>
                  ) : (
                    <button onClick={() => setDeleteConfirm(inst.id)} className="rounded-lg p-2 text-muted-foreground hover:bg-destructive/10 hover:text-destructive">
                      <Trash2 className="size-4" />
                    </button>
                  )}
                </div>
              </div>

              {inst.description && (
                <p className="text-sm text-muted-foreground line-clamp-2">{inst.description}</p>
              )}

              <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                {inst.city && (
                  <span className="inline-flex items-center gap-1"><MapPin className="size-3" />{inst.city}{inst.country ? `, ${inst.country}` : ""}</span>
                )}
                {inst.email && (
                  <span className="inline-flex items-center gap-1"><Mail className="size-3" />{inst.email}</span>
                )}
                {inst.phone && (
                  <span className="inline-flex items-center gap-1"><Phone className="size-3" />{inst.phone}</span>
                )}
                {inst.website && (
                  <span className="inline-flex items-center gap-1"><Globe className="size-3" />{inst.website}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {total > 20 && (
        <div className="flex items-center justify-center gap-2">
          <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50">Prev</button>
          <span className="text-sm text-muted-foreground">Page {page + 1} of {Math.ceil(total / 20)}</span>
          <button onClick={() => setPage((p) => p + 1)} disabled={(page + 1) * 20 >= total} className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50">Next</button>
        </div>
      )}

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold">{editing ? "Edit Institution" : "Add Institution"}</h2>
              <button onClick={() => { setShowForm(false); resetForm() }} className="rounded-lg p-1 hover:bg-muted"><X className="size-5" /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium">Name *</label>
                <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">Type *</label>
                <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring">
                  {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">Description</label>
                <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-sm font-medium">Email</label>
                  <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Phone</label>
                  <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">Address</label>
                <input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-sm font-medium">City</label>
                  <input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Country</label>
                  <input value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">Website</label>
                <input value={form.website} onChange={(e) => setForm({ ...form, website: e.target.value })} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => { setShowForm(false); resetForm() }} className="rounded-lg border border-border px-4 py-2 text-sm hover:bg-muted">Cancel</button>
                <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
                  {saving && <Loader2 className="size-4 animate-spin" />}
                  {editing ? "Save Changes" : "Create"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
