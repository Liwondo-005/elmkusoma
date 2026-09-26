"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { Building2, Search, Loader2, AlertCircle, RefreshCw, Filter, Briefcase, HeartHandshake, MapPin, Activity } from "lucide-react"
import { platformAdminApi, type InstitutionSummary, type PageResponse } from "@/lib/platform-admin-api"

const ORG_TYPES = ["all", "COMPANY", "NGO", "GOVERNMENT", "COOPERATIVE", "OTHER"]

function Skeleton() {
  return <div className="grid gap-4 md:grid-cols-2"><div className="h-28 rounded-2xl bg-muted animate-pulse" /><div className="h-28 rounded-2xl bg-muted animate-pulse" /><div className="h-28 rounded-2xl bg-muted animate-pulse" /><div className="h-28 rounded-2xl bg-muted animate-pulse" /></div>
}

export default function PlatformOrganizationsPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [institutions, setInstitutions] = useState<PageResponse<InstitutionSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [typeFilter, setTypeFilter] = useState("all")
  const [page, setPage] = useState(0)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listInstitutions(page, 20, search || undefined)
      setInstitutions(res)
    } catch (e: any) {
      setError(e.message || t("organizations.failedToLoadOrganizations"))
    } finally { setLoading(false) }
  }, [page, search])

  useEffect(() => { load() }, [load])

  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setPage(0); setSearch(searchInput) }

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
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-slate-700 text-white"><Briefcase className="size-4" /></span> {t("organizations.organizations")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("organizations.combinedInstitutionsProvidersOverview")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("organizations.refresh")}</button>
        </div>

        {stats && (
          <div className="mt-4 grid gap-3 sm:grid-cols-4">
            <div className="rounded-xl border border-border bg-muted/20 p-3 text-center"><p className="text-lg font-bold tabular-nums">{stats.total.toLocaleString()}</p><p className="text-xs text-muted-foreground">{t("organizations.totalThisPageScope")}</p></div>
            <div className="rounded-xl border border-blue-200 bg-blue-50 p-3 text-center"><p className="text-lg font-bold text-blue-700 tabular-nums">{stats.companies}</p><p className="text-xs text-muted-foreground">{t("organizations.companies")}</p></div>
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-center"><p className="text-lg font-bold text-emerald-700 tabular-nums">{stats.ngos}</p><p className="text-xs text-muted-foreground">{t("organizations.ngos")}</p></div>
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-center"><p className="text-lg font-bold text-emerald-700 tabular-nums">{stats.active}</p><p className="text-xs text-muted-foreground">{ts("active")}</p></div>
          </div>
        )}

        <form onSubmit={handleSearch} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={searchInput} onChange={(e) => setSearchInput(e.target.value)} placeholder={t("organizations.searchOrganizationsByName")} className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-muted-foreground" />
            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
              {ORG_TYPES.map(ot => <option key={ot} value={ot}>{ot === "all" ? t("organizations.allTypes") : ot}</option>)}
            </select>
          </div>
        </form>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("organizations.retry")}</button>
        </div>
      )}

      {loading ? <Skeleton /> : filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <Building2 className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("organizations.noOrganizationsFound")}</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">{search ? t("organizations.tryADifferentSearch") : t("organizations.noOrganizationsAvailableOrganizations")} {t("organizations.dataUnavailablePerSpec")}</p>
          </div>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {filtered.map((org) => (
            <div key={org.id} className="rounded-2xl border border-border bg-card p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10"><Building2 className="size-5 text-primary" /></div>
                  <div className="min-w-0">
                    <h3 className="font-semibold text-foreground truncate">{org.name}</h3>
                    <p className="text-xs text-muted-foreground">{org.code} · {org.type}</p>
                  </div>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${org.isActive ? "bg-emerald-500/10 text-emerald-700" : "bg-red-500/10 text-red-700"}`}>{org.isActive ? ts("active") : ts("inactive")}</span>
              </div>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
                {org.city && <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-1"><MapPin className="size-3" />{org.city}{org.region ? `, ${org.region}` : ""}</span>}
                <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-1"><Activity className="size-3" />{new Date(org.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {institutions && institutions.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50">{t("organizations.prev")}</button>
          <span className="text-sm text-muted-foreground">{t("organizations.pageOf", { p0: page + 1, p1: institutions.totalPages })}</span>
          <button onClick={() => setPage(p => Math.min(institutions.totalPages - 1, p + 1))} disabled={page >= institutions.totalPages - 1} className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50">{tc("next")}</button>
        </div>
      )}
    </div>
  )
}
