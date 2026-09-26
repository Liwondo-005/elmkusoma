"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState, useCallback } from "react"
import { BookOpen, Search, AlertCircle, RefreshCw, Filter, CheckSquare, Square, Loader2, Send } from "lucide-react"
import { platformAdminApi, type PageResponse } from "@/lib/platform-admin-api"

interface CourseRow {
  id: string
  institutionId: string | null
  title: string
  level: string
  category: string | null
  isPublished: boolean
  isFeatured: boolean
  createdAt: string | null
}

function Skeleton() {
  return <div className="animate-pulse space-y-3"><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /><div className="h-12 rounded-xl bg-muted" /></div>
}

export default function PlatformCoursesPage() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [page, setPage] = useState<PageResponse<CourseRow> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [filter, setFilter] = useState<"all" | "published" | "draft">("all")
  const [pageIndex, setPageIndex] = useState(0)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [bulkAction, setBulkAction] = useState<"PUBLISH" | "UNPUBLISH" | "ARCHIVE" | "RESTORE">("PUBLISH")
  const [bulkBusy, setBulkBusy] = useState(false)
  const [bulkResult, setBulkResult] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const res = await platformAdminApi.listPlatformCourses(pageIndex, 20, search || undefined)
      setPage(res)
      setSelected(new Set())
    } catch (e: any) {
      setError(e.message || t("courses.failedToLoadCourses"))
    } finally { setLoading(false) }
  }, [pageIndex, search])

  useEffect(() => { const t = setTimeout(load, 300); return () => clearTimeout(t) }, [load])

  const courses = page?.content ?? []
  const filtered = courses.filter((c) => {
    if (filter === "published" && !c.isPublished) return false
    if (filter === "draft" && c.isPublished) return false
    return true
  })

  const toggle = (id: string) => {
    setSelected(prev => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id); else n.add(id)
      return n
    })
  }

  const selectAll = () => {
    setSelected(prev => prev.size === filtered.length && filtered.length > 0 ? new Set() : new Set(filtered.map(c => c.id)))
  }

  const runBulk = async () => {
    if (selected.size === 0) return
    setBulkBusy(true); setBulkResult(null); setError(null)
    try {
      const res = await platformAdminApi.bulkContentAction("COURSE", bulkAction, [...selected])
      setBulkResult(t("courses.affectedCourseS", { p0: res.affected, p1: res.failures?.length ? `, ${res.failures.length} failed` : "" }))
      await load()
    } catch (e: any) {
      setError(e.message || t("courses.bulkActionFailed"))
    } finally { setBulkBusy(false) }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-violet-500 text-white"><BookOpen className="size-4" /></span> {t("courses.platformCourses")}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{t("courses.governanceOverCoursesAcross")}</p>
          </div>
          <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-muted"><RefreshCw className="size-4" /> {t("courses.refresh")}</button>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={t("courses.searchCoursesByTitle")} className="w-full rounded-xl border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring" />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-muted-foreground" />
            <select value={filter} onChange={(e) => setFilter(e.target.value as any)} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring">
              <option value="all">{tc("all")}</option>
              <option value="published">{t("courses.published")}</option>
              <option value="draft">{t("courses.draft")}</option>
            </select>
          </div>
          <div className="flex items-center gap-2">
            <select value={bulkAction} onChange={(e) => setBulkAction(e.target.value as any)} className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none" aria-label={t("courses.bulkAction")}>
              <option value="PUBLISH">PUBLISH</option>
              <option value="UNPUBLISH">UNPUBLISH</option>
              <option value="ARCHIVE">ARCHIVE</option>
              <option value="RESTORE">RESTORE</option>
            </select>
            <button onClick={runBulk} disabled={bulkBusy || selected.size === 0} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {bulkBusy ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />} {t("courses.apply")}{selected.size})
            </button>
          </div>
        </div>
        {bulkResult && <p className="mt-3 text-xs text-emerald-700">{bulkResult}</p>}
      </div>

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between">
          <span className="flex items-center gap-2"><AlertCircle className="size-4" />{error}</span>
          <button onClick={load} className="rounded-lg bg-white border px-3 py-1 text-xs font-semibold">{t("courses.retry")}</button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        {loading ? <Skeleton /> : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
            <BookOpen className="size-10 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("courses.noCoursesFound")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{search || filter !== "all" ? t("courses.tryADifferentSearch") : t("courses.noCoursesExistYet")}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-4 py-3 text-left">
                    <button onClick={selectAll} className="inline-flex items-center" aria-label={t("courses.selectAllCourses")} title="Select all">
                      {selected.size === filtered.length && filtered.length > 0 ? <CheckSquare className="size-4 text-primary" /> : <Square className="size-4 text-muted-foreground" />}
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("courses.title")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("courses.category")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("courses.level")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("courses.status")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("courses.created")}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((c) => (
                  <tr key={c.id} className="border-b border-border last:border-0 hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <button onClick={() => toggle(c.id)} className="inline-flex items-center" aria-label={`Select ${c.title}`} title="Select">
                        {selected.has(c.id) ? <CheckSquare className="size-4 text-primary" /> : <Square className="size-4 text-muted-foreground" />}
                      </button>
                    </td>
                    <td className="px-4 py-3"><p className="font-medium text-foreground line-clamp-1">{c.title}</p></td>
                    <td className="px-4 py-3 text-muted-foreground">{c.category ?? "—"}</td>
                    <td className="px-4 py-3"><span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">{c.level}</span></td>
                    <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${c.isPublished ? "bg-emerald-500/10 text-emerald-700" : "bg-amber-500/10 text-amber-700"}`}>{c.isPublished ? t("courses.published2") : t("courses.draft2")}</span></td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
              <span>{t("courses.pageOf", { p0: pageIndex + 1, p1: Math.max(page?.totalPages ?? 1, 1) })}</span>
              <div className="flex gap-2">
                <button disabled={pageIndex === 0} onClick={() => setPageIndex(pageIndex - 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{t("courses.prev")}</button>
                <button disabled={(page?.totalPages ?? 1) <= pageIndex + 1} onClick={() => setPageIndex(pageIndex + 1)} className="rounded-lg border border-border px-3 py-1.5 font-medium disabled:opacity-50">{tc("next")}</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
