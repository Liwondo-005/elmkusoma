"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { Building2, Loader2, Search, MapPin, Plus, Pencil, Trash2, CheckCircle2 } from "lucide-react"
import { platformAdminApi, type InstitutionSummary, type PageResponse } from "@/lib/platform-admin-api"
import { InstitutionFormModal } from "@/components/platform-admin/institution-form-modal"

const PAGE_SIZE = 20

const LIFECYCLE_OPTIONS: Record<string, string[]> = {
  ACTIVE: ["SUSPENDED", "DEACTIVATED", "ARCHIVED"],
  SUSPENDED: ["ACTIVE", "DEACTIVATED", "ARCHIVED"],
  DEACTIVATED: ["ACTIVE", "ARCHIVED", "SUSPENDED"],
  ARCHIVED: ["ACTIVE", "SUSPENDED"],
}

export default function PlatformInstitutionsPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const router = useRouter()
  const [data, setData] = useState<PageResponse<InstitutionSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [lifecycleBusy, setLifecycleBusy] = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<InstitutionSummary | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listInstitutions(page, PAGE_SIZE, search || undefined)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("institutions.failedToLoadInstitutions"))
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
      setError(err instanceof Error ? err.message : t("institutions.failedToUpdateInstitution"))
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
      setError(err instanceof Error ? err.message : t("institutions.failedToUpdateLifecycle"))
    } finally {
      setLifecycleBusy(null)
    }
  }

  const handleSaved = async (message: string) => {
    setSuccess(message)
    await loadData()
    setTimeout(() => setSuccess(null), 5000)
  }

  const handleDelete = async (inst: InstitutionSummary) => {
    if (!window.confirm(`Delete "${inst.name}"? This will remove it from the platform.`)) return
    setDeleting(inst.id)
    setError(null)
    try {
      await platformAdminApi.deleteInstitution(inst.id)
      setSuccess(`Institution "${inst.name}" deleted`)
      await loadData()
      setTimeout(() => setSuccess(null), 5000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete institution")
    } finally {
      setDeleting(null)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
<div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("institutions.institutions")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("institutions.manageAllEducationInstitutions")}</p>
        </div>
        <button
          onClick={() => { setEditing(null); setModalOpen(true) }}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" />
          {t("institutions.addInstitution")}
        </button>
      </div>
      </div>

      <form onSubmit={handleSearch} className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder={t("institutions.searchInstitutionsByName")}
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
        />
      </form>

      {success && (
        <div className="flex items-center gap-2 rounded-2xl border border-green-500/20 bg-green-500/5 px-6 py-4 text-sm text-green-600">
          <CheckCircle2 className="size-4 shrink-0" /> {success}
        </div>
      )}

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
          <p className="mt-4 text-sm font-medium text-foreground">{t("institutions.noInstitutionsFound")}</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {search ? t("institutions.tryADifferentSearch") : t("institutions.noInstitutionsAvailable")}
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
                  {inst.status ?? (inst.isActive ? ts("active") : ts("inactive"))}
                </span>
              </div>

              <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
                <span className="inline-block rounded-full bg-primary/10 px-2.5 py-0.5 font-medium text-primary">{inst.type}</span>
                {inst.city && (
                  <span className="inline-flex items-center gap-1"><MapPin className="size-3" />{inst.city}{inst.region ? `, ${inst.region}` : ""}</span>
                )}
              </div>

              <div className="mt-4 flex flex-wrap justify-end items-center gap-2">
                <select
                  value=""
                  disabled={lifecycleBusy === inst.id}
                  onClick={(e) => e.stopPropagation()}
                  onChange={(e) => { if (e.target.value) handleLifecycleChange(inst, e.target.value) }}
                  className="rounded-lg border border-border bg-background px-2 py-1.5 text-xs font-medium outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                >
                  <option value="">{t("institutions.changeLifecycle")}</option>
                  {(LIFECYCLE_OPTIONS[inst.status ?? "ACTIVE"] ?? []).map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <button
                  onClick={(e) => { e.stopPropagation(); setEditing(inst); setModalOpen(true) }}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                >
                  <Pencil className="size-3" /> Edit
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); handleDelete(inst) }}
                  disabled={deleting === inst.id}
                  className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50 dark:border-red-800"
                >
                  {deleting === inst.id ? <Loader2 className="size-3 animate-spin" /> : <Trash2 className="size-3" />} Delete
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); handleToggleStatus(inst) }}
                  className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                    inst.isActive
                      ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                      : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
                  }`}
                >
                  {inst.isActive ? t("institutions.deactivate") : t("institutions.activate")}
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
            {t("institutions.prev")}</button>
          <span className="text-sm text-muted-foreground">
            {t("institutions.pageOf", { p0: page + 1, p1: data.totalPages })}</span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            {tc("next")}</button>
        </div>
      )}

      <InstitutionFormModal
        open={modalOpen}
        institution={editing}
        onClose={() => setModalOpen(false)}
        onSaved={handleSaved}
      />
    </div>
  )
}
