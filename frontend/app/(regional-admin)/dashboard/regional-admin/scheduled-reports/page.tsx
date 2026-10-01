"use client"

import { useCallback, useEffect, useState } from "react"
import { CalendarClock, History, Play, Plus, Trash2 } from "lucide-react"
import {
  regionalAdminApi,
  type CreateScheduledReportPayload,
  type ScheduledReport,
  type ScheduledReportRun,
} from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

const REPORT_TYPES = [
  { value: "PERFORMANCE", label: "Performance" },
  { value: "ATTENDANCE", label: "Attendance" },
  { value: "LEARNERS", label: "Learners" },
  { value: "DATA_QUALITY", label: "Data quality" },
  { value: "GOVERNANCE", label: "Governance" },
]

const FREQUENCIES = [
  { value: "DAILY", label: "Daily" },
  { value: "WEEKLY", label: "Weekly" },
  { value: "MONTHLY", label: "Monthly" },
]

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : "Something went wrong."
}

function statusTone(status: string): "success" | "warning" | "danger" {
  if (status === "ACTIVE") return "success"
  if (status === "PAUSED") return "warning"
  return "danger"
}

function RunHistory({ reportId }: { reportId: string }) {
  const [runs, setRuns] = useState<ScheduledReportRun[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setRuns(null)
    setError(null)
    regionalAdminApi
      .listScheduledReportRuns(reportId)
      .then((r) => { if (active) setRuns(r) })
      .catch((e) => { if (active) setError(errMsg(e)) })
    return () => { active = false }
  }, [reportId])

  if (error) return <ErrorState message={error} />
  if (runs === null) return <LoadingState label="Loading run history…" />
  if (runs.length === 0) {
    return (
      <p className="text-xs text-muted-foreground">
        No runs yet — this report has not been generated.
      </p>
    )
  }
  return (
    <div className="space-y-2">
      {runs.map((run) => (
        <div
          key={run.id}
          className="flex flex-wrap items-start justify-between gap-3 rounded-xl border border-border bg-background/60 p-3"
        >
          <div className="min-w-0 space-y-1">
            <div className="flex items-center gap-2">
              <Chip tone={run.status === "SUCCESS" ? "success" : "danger"}>{run.status}</Chip>
              <span className="text-xs text-muted-foreground">{formatDateTime(run.runAt)}</span>
            </div>
            {run.summary ? (
              <pre className="max-w-full overflow-x-auto whitespace-pre-wrap break-all font-mono text-[11px] leading-relaxed text-muted-foreground">
                {run.summary}
              </pre>
            ) : null}
            {run.error ? (
              <p className="text-xs font-medium text-destructive">{run.error}</p>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  )
}

export default function ScheduledReportsPage() {
  const [reports, setReports] = useState<ScheduledReport[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState<string | null>(null)
  const [openRuns, setOpenRuns] = useState<string | null>(null)
  const [refresh, setRefresh] = useState(0)

  const [form, setForm] = useState<CreateScheduledReportPayload>({
    title: "",
    reportType: "PERFORMANCE",
    frequency: "MONTHLY",
    recipients: "",
  })
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await regionalAdminApi.listScheduledReports()
      setReports(list)
    } catch (e) {
      setReports([])
      setError(errMsg(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load, refresh])

  const retry = () => setRefresh((k) => k + 1)

  async function createReport(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) {
      setCreateError("A title is required.")
      return
    }
    setCreating(true)
    setCreateError(null)
    try {
      await regionalAdminApi.createScheduledReport({
        ...form,
        title: form.title.trim(),
        recipients: form.recipients?.trim() || undefined,
      })
      setForm({ title: "", reportType: "PERFORMANCE", frequency: "MONTHLY", recipients: "" })
      setRefresh((k) => k + 1)
    } catch (e2) {
      setCreateError(errMsg(e2))
    } finally {
      setCreating(false)
    }
  }

  async function runNow(report: ScheduledReport) {
    setActionError(null)
    try {
      await regionalAdminApi.runScheduledReport(report.id)
      setOpenRuns(report.id)
      setRefresh((k) => k + 1)
    } catch (e) {
      setActionError(errMsg(e))
    }
  }

  async function toggleStatus(report: ScheduledReport) {
    setActionError(null)
    try {
      await regionalAdminApi.updateScheduledReport(report.id, {
        status: report.status === "ACTIVE" ? "PAUSED" : "ACTIVE",
      })
      setRefresh((k) => k + 1)
    } catch (e) {
      setActionError(errMsg(e))
    }
  }

  async function remove(report: ScheduledReport) {
    setActionError(null)
    try {
      await regionalAdminApi.deleteScheduledReport(report.id)
      setRefresh((k) => k + 1)
    } catch (e) {
      setActionError(errMsg(e))
    }
  }

  return (
    <div className="space-y-6 pb-8" data-testid="scheduled-reports-page">
      <PageHeader
        title="Scheduled Reports"
        description="Reports generated automatically inside your jurisdiction from real oversight figures — never synthetic data."
      />

      <form
        onSubmit={createReport}
        className="rounded-2xl border border-border bg-card p-5 shadow-sm"
      >
        <h2 className="mb-4 flex items-center gap-2 text-sm font-semibold text-foreground">
          <Plus className="size-4" /> New scheduled report
        </h2>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <label className="space-y-1.5">
            <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Title</span>
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="e.g. Monthly attendance digest"
              className="w-full rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            />
          </label>
          <label className="space-y-1.5">
            <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Report type</span>
            <select
              value={form.reportType}
              onChange={(e) => setForm({ ...form, reportType: e.target.value })}
              className="w-full rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              {REPORT_TYPES.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>
          <label className="space-y-1.5">
            <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Frequency</span>
            <select
              value={form.frequency}
              onChange={(e) => setForm({ ...form, frequency: e.target.value })}
              className="w-full rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              {FREQUENCIES.map((f) => (
                <option key={f.value} value={f.value}>{f.label}</option>
              ))}
            </select>
          </label>
          <label className="space-y-1.5">
            <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Recipients (optional)</span>
            <input
              value={form.recipients ?? ""}
              onChange={(e) => setForm({ ...form, recipients: e.target.value })}
              placeholder="comma-separated emails"
              className="w-full rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            />
          </label>
        </div>
        {createError && <p className="mt-3 text-xs font-medium text-destructive">{createError}</p>}
        <div className="mt-4 flex justify-end">
          <button
            type="submit"
            disabled={creating}
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
          >
            <Plus className="size-4" />
            {creating ? "Creating…" : "Create report"}
          </button>
        </div>
      </form>

      {actionError && <ErrorState message={actionError} onRetry={() => setActionError(null)} />}

      {loading ? (
        <LoadingState label="Loading scheduled reports…" />
      ) : error ? (
        <ErrorState message={error} onRetry={retry} />
      ) : !reports || reports.length === 0 ? (
        <EmptyState
          icon={<CalendarClock className="size-10" />}
          title="No scheduled reports yet"
          hint="Create your first report above — it will be generated on schedule inside your jurisdiction."
        />
      ) : (
        <div className="space-y-4">
          {reports.map((report) => (
            <div key={report.id} className="rounded-2xl border border-border bg-card p-5 shadow-sm">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="truncate font-semibold text-foreground">{report.title}</h3>
                    <Chip tone={statusTone(report.status)}>{report.status}</Chip>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {REPORT_TYPES.find((t) => t.value === report.reportType)?.label ?? report.reportType}
                    {" · "}
                    {FREQUENCIES.find((f) => f.value === report.frequency)?.label ?? report.frequency}
                    {" · "}
                    {report.jurisdiction ?? "Jurisdiction"}
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <button
                    onClick={() => runNow(report)}
                    className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
                  >
                    <Play className="size-3.5" /> Run now
                  </button>
                  <button
                    onClick={() => toggleStatus(report)}
                    className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
                  >
                    {report.status === "ACTIVE" ? "Pause" : "Activate"}
                  </button>
                  <button
                    onClick={() => remove(report)}
                    aria-label={`Delete ${report.title}`}
                    className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-destructive hover:bg-destructive/10"
                  >
                    <Trash2 className="size-3.5" />
                  </button>
                </div>
              </div>

              <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-3 sm:grid-cols-4">
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Next run</dt>
                  <dd className="mt-0.5 text-sm font-semibold text-foreground">{formatDateTime(report.nextRunAt)}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Last run</dt>
                  <dd className="mt-0.5 text-sm font-semibold text-foreground">{formatDateTime(report.lastRunAt)}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Runs</dt>
                  <dd className="mt-0.5 text-sm font-semibold tabular-nums text-foreground">{report.runCount ?? 0}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Recipients</dt>
                  <dd className="mt-0.5 truncate text-sm font-semibold text-foreground">{report.recipients || "—"}</dd>
                </div>
              </dl>

              <div className="mt-4">
                <button
                  onClick={() => setOpenRuns(openRuns === report.id ? null : report.id)}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
                >
                  <History className="size-3.5" />
                  {openRuns === report.id ? "Hide run history" : "Show run history"}
                </button>
                {openRuns === report.id && (
                  <div className="mt-3">
                    <RunHistory reportId={report.id} />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
