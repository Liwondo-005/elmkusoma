"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { GitBranch, RefreshCw, AlertCircle, Search, Building2, ChevronDown } from "lucide-react"
import { platformAdminApi, type InstitutionSummary, type PageResponse } from "@/lib/platform-admin-api"

const LIFECYCLE_OPTIONS = ["ACTIVE", "SUSPENDED", "DEACTIVATED", "ARCHIVED"] as const
const TRANSITIONS: Record<string, string[]> = {
  ACTIVE: ["SUSPENDED", "DEACTIVATED"],
  SUSPENDED: ["ACTIVE", "DEACTIVATED"],
  DEACTIVATED: ["ACTIVE", "ARCHIVED"],
  ARCHIVED: [],
}

function StatusBadge({ s }: { s: string }) {
  const map: Record<string, string> = {
    ACTIVE: "bg-emerald-500/10 text-emerald-700 border-emerald-200",
    SUSPENDED: "bg-amber-500/10 text-amber-700 border-amber-200",
    DEACTIVATED: "bg-red-500/10 text-red-700 border-red-200",
    ARCHIVED: "bg-slate-500/10 text-slate-600 border-slate-200",
  }
  const label = s || "ACTIVE"
  return <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${map[label] ?? "bg-muted"}`}>{label}</span>
}

export default function PlatformLifecyclePage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [page, setPage] = useState<PageResponse<InstitutionSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [pageIndex, setPageIndex] = useState(0)
  const [updating, setUpdating] = useState<string | null>(null)
  const [openMenu, setOpenMenu] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      setPage(await platformAdminApi.listInstitutions(pageIndex, 20, search || undefined))
    } catch (e: any) {
      setError(e.message || t("lifecycle.failedToLoadLifecycle")); setPage(null)
    } finally { setLoading(false) }
  }, [pageIndex, search])

  useEffect(() => { load() }, [load])

  const transition = async (inst: InstitutionSummary, target: string) => {
    setUpdating(inst.id); setOpenMenu(null)
    try {
      await platformAdminApi.updateInstitutionLifecycle(inst.id, target)
      await load()
    } catch (e: any) {
      setError(e.message || t("lifecycle.lifecycleTransitionFailed"))
    } finally { setUpdating(null) }
  }

  const rows = page?.content ?? []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-slate-700 text-white"><GitBranch className="size-4" /></span> {t("lifecycle.platformLifecycle")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("lifecycle.institutionLifecycleGovernance")}<span className="font-mono text-xs">{t("lifecycle.activeSuspendedDeactivatedArchived")}</span>{t("lifecycle.offboardingNeverSilentlyDestroys")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("lifecycle.refresh")}</button>
        </div>
        <div className="mt-4 relative">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input value={search} onChange={(e) => { setSearch(e.target.value); setPageIndex(0) }} placeholder={t("lifecycle.searchInstitutions")} className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("lifecycle.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <div className="animate-pulse space-y-3"><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /></div> : rows.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <GitBranch className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("lifecycle.noInstitutionsFound")}</p>
            <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">{t("lifecycle.institutionLifecycleTransitionsAppear")}</p>
          </div>
        ) : (
          <div className="space-y-2">
            {rows.map((inst) => {
              const current = (inst.status || (inst.isActive ? "ACTIVE" : "SUSPENDED")).toUpperCase()
              const allowed = TRANSITIONS[current] ?? []
              return (
                <div key={inst.id} className="flex items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/30">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2"><StatusBadge s={current} /><span className="rounded-full bg-muted px-2 py-0.5 text-xs">{inst.type}</span></div>
                    <p className="mt-1 flex items-center gap-1.5 font-medium text-foreground truncate"><Building2 className="size-3.5 text-muted-foreground" />{inst.name}</p>
                    <p className="text-xs text-muted-foreground">{inst.code} · {inst.city}</p>
                  </div>
                  <div className="relative">
                    <button disabled={updating === inst.id || allowed.length === 0}
                      onClick={() => setOpenMenu(openMenu === inst.id ? null : inst.id)}
                      className="inline-flex items-center gap-1 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-semibold hover:bg-muted disabled:opacity-40">
                      {updating === inst.id ? t("lifecycle.updating") : t("lifecycle.transition")} <ChevronDown className="size-3" />
                    </button>
                    {openMenu === inst.id && allowed.length > 0 && (
                      <div className="absolute right-0 z-10 mt-1 w-44 rounded-xl border border-border bg-card p-1 shadow-lg">
                        {allowed.map((t) => (
                          <button key={t} onClick={() => transition(inst, t)}
                            className="w-full rounded-lg px-3 py-2 text-left text-xs font-semibold hover:bg-muted">
                            → {t}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
        {page && page.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <button disabled={page.first} onClick={() => setPageIndex(i => i - 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">{tc("previous")}</button>
            <span className="text-xs text-muted-foreground">{t("lifecycle.pageOfInstitutions", { p0: page.page + 1, p1: page.totalPages, p2: page.totalElements })}</span>
            <button disabled={page.last} onClick={() => setPageIndex(i => i + 1)} className="rounded-lg border px-3 py-1.5 disabled:opacity-40">{tc("next")}</button>
          </div>
        )}
      </div>
    </div>
  )
}
