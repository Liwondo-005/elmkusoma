"use client"

import { Fragment, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Competency, CompetencyRecord, CompetencySummary } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Target, CheckCircle2, Clock, AlertTriangle, Search, Filter, AlertCircle } from "lucide-react"

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    NOT_STARTED: "bg-muted text-muted-foreground",
    LEARNING: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    PRACTICING: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    ASSESSED: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    COMPETENT: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
    NEEDS_PRACTICE: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status] || "bg-muted"}`}>
      {status.replace(/_/g, " ")}
    </span>
  )
}

export default function CompetenciesPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [competencies, setCompetencies] = useState<Competency[]>([])
  const [records, setRecords] = useState<CompetencyRecord[]>([])
  const [summary, setSummary] = useState<CompetencySummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("ALL")
  const [updatingId, setUpdatingId] = useState<string | null>(null)
  const [savingId, setSavingId] = useState<string | null>(null)
  const [form, setForm] = useState<{ status: "LEARNING" | "PRACTICING"; evidence: string }>({
    status: "LEARNING",
    evidence: "",
  })

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const [allCompetencies, studentRecords, summaryData] = await Promise.all([
        collegeApi.listCompetencies(),
        collegeApi.getStudentCompetencies(studentId).catch((err) => { setError(err?.message || tc("error.generic")); return { success: true, data: [] } as const }),
        collegeApi.getCompetencySummary(studentId).catch((err) => { setError(err?.message || tc("error.generic")); return { success: true, data: null } as const }),
      ])
      setCompetencies((allCompetencies.data as Competency[] | undefined) || [])
      setRecords((studentRecords.data as CompetencyRecord[] | undefined) || [])
      setSummary((summaryData.data as CompetencySummary | undefined) || null)
    } catch {
      setError(tc("error.generic"))
    } finally {
      setLoading(false)
    }
  }

  const recordMap = new Map(records.map(r => [r.competencyId, r]))

  async function handleUpdateRecord(competencyId: string) {
    if (!user) return
    try {
      setSavingId(competencyId)
      setError(null)
      const res = await collegeApi.updateStudentCompetency(user.id || "", competencyId, {
        status: form.status,
        evidence: form.evidence.trim() || undefined,
      })
      const updated = res.data as CompetencyRecord | undefined
      setRecords((prev) => {
        const existing = prev.find((r) => r.competencyId === competencyId)
        const merged = updated || (existing ? { ...existing, status: form.status, evidence: form.evidence.trim() } : undefined)
        if (!merged) return prev
        return [...prev.filter((r) => r.competencyId !== competencyId), merged]
      })
      setUpdatingId(null)
    } catch {
      setError(tc("error.update"))
    } finally {
      setSavingId(null)
    }
  }

  const filtered = competencies.filter(c => {
    const matchSearch = c.name.toLowerCase().includes(search.toLowerCase()) || c.code?.toLowerCase().includes(search.toLowerCase())
    const record = recordMap.get(c.id)
    const status = record?.status || "NOT_STARTED"
    const matchStatus = statusFilter === "ALL" || status === statusFilter
    return matchSearch && matchStatus
  })

  if (authLoading || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error && competencies.length === 0) {
    return (
      <div role="main" className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("subtitle.competencies")} />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadData() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => setError(null)} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}
      <LearnerHeader firstName={firstName} subtitle={t("subtitle.competencies")} />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <Target className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.total")}</p>
              <p className="text-2xl font-extrabold text-foreground">{summary?.total ?? competencies.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("competency")}</p>
              <p className="text-2xl font-extrabold text-foreground">{summary?.competent ?? 0}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <Clock className="size-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("stats.inProgress")}</p>
              <p className="text-2xl font-extrabold text-foreground">{(summary?.learning ?? 0) + (summary?.practicing ?? 0)}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/30">
              <AlertTriangle className="size-5 text-red-600 dark:text-red-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("filters.pending")}</p>
              <p className="text-2xl font-extrabold text-foreground">{summary?.needsPractice ?? 0}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={tc("search") + "..."}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label={tc("search")}
            className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            aria-label={tc("filter")}
            className="appearance-none rounded-xl border border-border bg-card py-2.5 pl-10 pr-8 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option value="ALL">{tc("filter")}</option>
            <option value="NOT_STARTED">{t("filters.pending")}</option>
            <option value="LEARNING">{t("competencyLevel")}</option>
            <option value="PRACTICING">{t("practical")}</option>
            <option value="COMPETENT">{t("competency")}</option>
            <option value="NEEDS_PRACTICE">{t("filters.pending")}</option>
            <option value="COMPLETED">{t("stats.completed")}</option>
          </select>
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Target className="size-8" />}
          title={t("empty.noCompetencies")}
          description={t("empty.noCompetenciesDesc")}
        />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("competency")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("department")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("status")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("academicYear")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{tc("edit")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filtered.map((c) => {
                  const record = recordMap.get(c.id)
                  const editable = !record || record.status === "NOT_STARTED" || record.status === "LEARNING" || record.status === "PRACTICING"
                  return (
                    <Fragment key={c.id}>
                      <tr className="hover:bg-muted/30">
                        <td className="px-4 py-3">
                          <p className="font-medium text-foreground">{c.name}</p>
                          {c.code && <p className="text-xs text-muted-foreground">{c.code}</p>}
                        </td>
                        <td className="px-4 py-3 text-muted-foreground">{c.competencyType}</td>
                        <td className="px-4 py-3"><StatusBadge status={record?.status || "NOT_STARTED"} /></td>
                        <td className="px-4 py-3 text-muted-foreground">{record?.updatedAt ? new Date(record.updatedAt).toLocaleDateString() : "—"}</td>
                        <td className="px-4 py-3 text-right">
                          {editable ? (
                            <button
                              type="button"
                              onClick={() => {
                                setForm({
                                  status: record?.status === "PRACTICING" ? "PRACTICING" : "LEARNING",
                                  evidence: record?.evidence || "",
                                })
                                setUpdatingId(updatingId === c.id ? null : c.id)
                              }}
                              aria-expanded={updatingId === c.id}
                              aria-label={t("learnerActions.updateStatus")}
                              className="inline-flex items-center gap-1.5 rounded-lg border border-primary/20 bg-primary/5 px-3 py-1.5 text-xs font-medium text-primary transition hover:bg-primary/10"
                            >
                              {updatingId === c.id ? tc("cancel") : t("learnerActions.updateStatus")}
                            </button>
                          ) : (
                            <span className="text-xs text-muted-foreground">—</span>
                          )}
                        </td>
                      </tr>
                      {updatingId === c.id && (
                        <tr>
                          <td colSpan={5} className="bg-muted/30 px-4 py-3">
                            <form
                              onSubmit={(e) => { e.preventDefault(); handleUpdateRecord(c.id) }}
                              className="flex flex-col gap-3 sm:flex-row sm:items-end"
                            >
                              <div className="space-y-1.5">
                                <label className="text-xs font-medium text-muted-foreground">{t("status")}</label>
                                <select
                                  value={form.status}
                                  onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as "LEARNING" | "PRACTICING" }))}
                                  aria-label={t("status")}
                                  className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                                >
                                  <option value="LEARNING">{t("learnerActions.statusLearning")}</option>
                                  <option value="PRACTICING">{t("learnerActions.statusPracticing")}</option>
                                </select>
                              </div>
                              <div className="flex-1 space-y-1.5">
                                <label className="text-xs font-medium text-muted-foreground">{t("learnerActions.evidenceOptional")}</label>
                                <input
                                  type="text"
                                  value={form.evidence}
                                  onChange={(e) => setForm((f) => ({ ...f, evidence: e.target.value }))}
                                  aria-label={t("learnerActions.evidenceOptional")}
                                  className="h-9 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                                />
                              </div>
                              <div className="flex items-center gap-2">
                                <button
                                  type="submit"
                                  disabled={savingId === c.id}
                                  aria-label={tc("save")}
                                  className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs transition hover:bg-primary/90 disabled:opacity-50"
                                >
                                  {savingId === c.id ? tc("loading") : tc("save")}
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setUpdatingId(null)}
                                  aria-label={tc("cancel")}
                                  className="rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition hover:bg-muted"
                                >
                                  {tc("cancel")}
                                </button>
                              </div>
                            </form>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
