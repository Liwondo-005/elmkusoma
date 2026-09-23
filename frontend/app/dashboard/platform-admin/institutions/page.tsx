"use client"

import { useEffect, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { Building2, Loader2, Search, MapPin } from "lucide-react"
import { platformAdminApi, type InstitutionSummary, type PageResponse } from "@/lib/platform-admin-api"

const PAGE_SIZE = 20

const LIFECYCLE_OPTIONS: Record<string, string[]> = {
  ACTIVE: ["SUSPENDED", "DEACTIVATED", "ARCHIVED"],
  SUSPENDED: ["ACTIVE", "DEACTIVATED", "ARCHIVED"],
  DEACTIVATED: ["ACTIVE", "ARCHIVED", "SUSPENDED"],
  ARCHIVED: ["ACTIVE", "SUSPENDED"],
}

export default function PlatformInstitutionsPage() {
  const router = useRouter()
  const [data, setData] = useState<PageResponse<InstitutionSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [lifecycleBusy, setLifecycleBusy] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listInstitutions(page, PAGE_SIZE, search || undefined)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load institutions")
    } finally {
      setLoading(false)
    }
  }, [page, search])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  const handleToggleStatus = async (inst: InstitutionSummary) => {
    try {
      await platformAdminApi.updateInstitutionStatus(inst.id, !inst.isActive)
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          content: prev.content.map((i) =>
            i.id === inst.id ? { ...i, isActive: !i.isActive } : i
          ),
        }
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update institution status")
    }
  }

  const handleLifecycleChange = async (inst: InstitutionSummary, status: string) => {
    setLifecycleBusy(inst.id)
    setError(null)
    try {
      const updated = await platformAdminApi.updateInstitutionLifecycle(inst.id, status)
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          content: prev.content.map((i) => (i.id === inst.id ? { ...i, ...updated } : i)),
        }
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update lifecycle status")
    } finally {
      setLifecycleBusy(null)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Institutions</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage all education institutions on the platform.</p>
      </div>

      <form onSubmit={handleSearch} className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search institutions by name, code, or city..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
        />
      </form>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Building2 className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No institutions found</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {search ? "Try a different search term." : "No institutions available."}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {data.content.map((inst) => (
            <div
              key={inst.id}
              className="rounded-2xl border border-border bg-card p-5 shadow-xs cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => router.push(`/dashboard/platform-admin/institutions/${inst.id}`)}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
                    <Building2 className="size-5 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">{inst.name}</h3>
                    <p className="text-xs text-muted-foreground">{inst.code}</p>
                  </div>
                </div>
                <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${inst.isActive ? "bg-green-500/10 text-green-600" : "bg-red-500/10 text-red-600"}`}>
                  {inst.status ?? (inst.isActive ? "Active" : "Inactive")}
                </span>
              </div>

              <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
                <span className="inline-block rounded-full bg-primary/10 px-2.5 py-0.5 font-medium text-primary">{inst.type}</span>
                {inst.city && (
                  <span className="inline-flex items-center gap-1"><MapPin className="size-3" />{inst.city}{inst.region ? `, ${inst.region}` : ""}</span>
                )}
              </div>

              <div className="mt-4 flex justify-end items-center gap-2">
                <select
                  value=""
                  disabled={lifecycleBusy === inst.id}
                  onClick={(e) => e.stopPropagation()}
                  onChange={(e) => { if (e.target.value) handleLifecycleChange(inst, e.target.value) }}
                  className="rounded-lg border border-border bg-background px-2 py-1.5 text-xs font-medium outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                >
                  <option value="">Change lifecycle…</option>
                  {(LIFECYCLE_OPTIONS[inst.status ?? "ACTIVE"] ?? []).map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <button
                  onClick={(e) => { e.stopPropagation(); handleToggleStatus(inst) }}
                  className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                    inst.isActive
                      ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                      : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
                  }`}
                >
                  {inst.isActive ? "Deactivate" : "Activate"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {data && data.totalElements > PAGE_SIZE && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Prev
          </button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
