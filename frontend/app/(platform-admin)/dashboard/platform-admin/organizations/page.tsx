"use client"

import { useEffect, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { Building2, Search, Loader2, AlertCircle, RefreshCw, Filter, Briefcase, MapPin, Activity, Plus, Pencil, Trash2, ShieldCheck, CheckCircle2 } from "lucide-react"
import { platformAdminApi, INSTITUTION_TYPES, type InstitutionSummary, type PageResponse } from "@/lib/platform-admin-api"
import { InstitutionFormModal } from "@/components/platform-admin/institution-form-modal"
import { OrgMembersModal } from "@/components/platform-admin/org-members-modal"

const ORG_TYPES: string[] = ["all", ...INSTITUTION_TYPES]

const LIFECYCLE_OPTIONS: Record<string, string[]> = {
  ACTIVE: ["SUSPENDED", "DEACTIVATED", "ARCHIVED"],
  SUSPENDED: ["ACTIVE", "DEACTIVATED", "ARCHIVED"],
  DEACTIVATED: ["ACTIVE", "ARCHIVED", "SUSPENDED"],
  ARCHIVED: ["ACTIVE", "SUSPENDED"],
}

function Skeleton() {
  return <div className="grid gap-4 md:grid-cols-2"><div className="h-28 rounded-2xl bg-muted animate-pulse" /><div className="h-28 rounded-2xl bg-muted animate-pulse" /><div className="h-28 rounded-2xl bg-muted animate-pulse" /><div className="h-28 rounded-2xl bg-muted animate-pulse" /></div>
}

export default function PlatformOrganizationsPage() {
  const router = useRouter()
  const [institutions, setInstitutions] = useState<PageResponse<InstitutionSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [typeFilter, setTypeFilter] = useState("all")
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<InstitutionSummary | null>(null)
  const [rolesFor, setRolesFor] = useState<InstitutionSummary | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [lifecycleBusy, setLifecycleBusy] = useState<string | null>(null)

  const flash = (msg: string) => {
    setSuccess(msg)
    setTimeout(() => setSuccess(null), 5000)
  }

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listInstitutions(page, 20, search || undefined)
      setInstitutions(res)
    } catch (e: any) {
      setError(e.message || "Failed to load organizations")
    } finally { setLoading(false) }
  }, [page, search])

  useEffect(() => { load() }, [load])

  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setPage(0); setSearch(searchInput) }

  const handleSaved = async (message: string) => {
    flash(message)
    await load()
  }

  const handleToggleStatus = async (org: InstitutionSummary) => {
    setError(null)
    try {
      await platformAdminApi.updateInstitutionStatus(org.id, !org.isActive)
      setInstitutions((prev) => prev ? {
        ...prev,
        content: prev.content.map((i) => i.id === org.id ? { ...i, isActive: !i.isActive } : i),
      } : prev)
      flash(`Organization ${org.isActive ? "deactivated" : "activated"}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update status")
    }
  }

  const handleLifecycleChange = async (org: InstitutionSummary, status: string) => {
    setLifecycleBusy(org.id)
    setError(null)
    try {
      const updated = await platformAdminApi.updateInstitutionLifecycle(org.id, status)
      setInstitutions((prev) => prev ? {
        ...prev,
        content: prev.content.map((i) => i.id === org.id ? { ...i, ...updated } : i),
      } : prev)
      flash(`Lifecycle changed to ${status}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update lifecycle status")
    } finally {
      setLifecycleBusy(null)
    }
  }

  const handleDelete = async (org: InstitutionSummary) => {
    if (!window.confirm(`Delete "${org.name}"? This will remove it from the platform.`)) return
    setDeleting(org.id)
    setError(null)
    try {
      await platformAdminApi.deleteInstitution(org.id)
      flash(`Organization "${org.name}" deleted`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete organization")
    } finally {
      setDeleting(null)
    }
  }

  const filtered = (institutions?.content ?? []).filter((org) => {
    if (typeFilter === "all") return true
    return (org.type ?? "").toUpperCase() === typeFilter
  })

  const stats = institutions ? {
    total: institutions.totalElements,
    companies: (institutions.content ?? []).filter(i => i.type === "COMPANY").length,
    ngos: (institutions.content ?? []).filter(i => i.type === "NGO").length,
    active: (institutions.content ?? []).filter(i => i.isActive).length,
  } : null

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-slate-700 text-white"><Briefcase className="size-4" /></span> Organizations</h1>
            <p className="mt-1 text-sm text-muted-foreground">Combined institutions + providers overview — companies, NGOs, government, and cooperatives. Filter by type.</p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> Refresh</button>
            <button
              onClick={() => { setEditing(null); setModalOpen(true) }}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="size-4" /> Add Organization
            </button>
          </div>
        </div>

        {stats && (
          <div className="mt-4 grid gap-3 sm:grid-cols-4">
            <div className="rounded-xl border border-border bg-muted/20 p-3 text-center"><p className="text-lg font-bold tabular-nums">{stats.total.toLocaleString()}</p><p className="text-xs text-muted-foreground">Total (this page scope)</p></div>
            <div className="rounded-xl border border-blue-200 bg-blue-50 p-3 text-center"><p className="text-lg font-bold text-blue-700 tabular-nums">{stats.companies}</p><p className="text-xs text-muted-foreground">Companies</p></div>
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-center"><p className="text-lg font-bold text-emerald-700 tabular-nums">{stats.ngos}</p><p className="text-xs text-muted-foreground">NGOs</p></div>
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-center"><p className="text-lg font-bold text-emerald-700 tabular-nums">{stats.active}</p><p className="text-xs text-muted-foreground">Active</p></div>
          </div>
        )}

        <form onSubmit={handleSearch} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={searchInput} onChange={(e) => setSearchInput(e.target.value)} placeholder="Search organizations by name, code, city..." className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-muted-foreground" />
            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
              {ORG_TYPES.map(t => <option key={t} value={t}>{t === "all" ? "All types" : t.replace(/_/g, " ")}</option>)}
            </select>
          </div>
        </form>
      </div>

      {success && (
        <div className="flex items-center gap-2 rounded-2xl border border-green-500/20 bg-green-500/5 px-6 py-4 text-sm text-green-600">
          <CheckCircle2 className="size-4 shrink-0" /> {success}
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">Retry</button>
        </div>
      )}

      {loading ? <Skeleton /> : filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <Building2 className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">No organizations found</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">{search ? "Try a different search term." : "No organizations available. Use Add Organization to register one."} Data unavailable per spec §13 when empty.</p>
          </div>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {filtered.map((org) => (
            <div
              key={org.id}
              className="rounded-2xl border border-border bg-card p-5 shadow-sm hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => router.push(`/dashboard/platform-admin/institutions/${org.id}`)}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10"><Building2 className="size-5 text-primary" /></div>
                  <div className="min-w-0">
                    <h3 className="font-semibold text-foreground truncate">{org.name}</h3>
                    <p className="text-xs text-muted-foreground">{org.code} · {org.type}</p>
                  </div>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${org.isActive ? "bg-emerald-500/10 text-emerald-700" : "bg-red-500/10 text-red-700"}`}>{org.isActive ? "Active" : "Inactive"}</span>
              </div>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
                {org.city && <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-1"><MapPin className="size-3" />{org.city}{org.region ? `, ${org.region}` : ""}</span>}
                <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-1"><Activity className="size-3" />{new Date(org.createdAt).toLocaleDateString()}</span>
              </div>

              <div className="mt-4 flex flex-wrap justify-end items-center gap-2">
                <select
                  value=""
                  disabled={lifecycleBusy === org.id}
                  onClick={(e) => e.stopPropagation()}
                  onChange={(e) => { if (e.target.value) handleLifecycleChange(org, e.target.value) }}
                  className="rounded-lg border border-border bg-background px-2 py-1.5 text-xs font-medium outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                >
                  <option value="">Change lifecycle…</option>
                  {(LIFECYCLE_OPTIONS[org.status ?? "ACTIVE"] ?? []).map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <button
                  onClick={(e) => { e.stopPropagation(); setEditing(org); setModalOpen(true) }}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                >
                  <Pencil className="size-3" /> Edit
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); setRolesFor(org) }}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                >
                  <ShieldCheck className="size-3" /> Roles
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); handleDelete(org) }}
                  disabled={deleting === org.id}
                  className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50 dark:border-red-800"
                >
                  {deleting === org.id ? <Loader2 className="size-3 animate-spin" /> : <Trash2 className="size-3" />} Delete
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); handleToggleStatus(org) }}
                  className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                    org.isActive
                      ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                      : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
                  }`}
                >
                  {org.isActive ? "Deactivate" : "Activate"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {institutions && institutions.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50">Prev</button>
          <span className="text-sm text-muted-foreground">Page {page + 1} of {institutions.totalPages}</span>
          <button onClick={() => setPage(p => Math.min(institutions.totalPages - 1, p + 1))} disabled={page >= institutions.totalPages - 1} className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50">Next</button>
        </div>
      )}

      <InstitutionFormModal
        open={modalOpen}
        institution={editing}
        onClose={() => setModalOpen(false)}
        onSaved={handleSaved}
      />

      <OrgMembersModal
        open={rolesFor !== null}
        institution={rolesFor}
        onClose={() => setRolesFor(null)}
      />
    </div>
  )
}
