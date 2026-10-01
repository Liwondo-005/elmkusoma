"use client"

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { BookOpen, School } from "lucide-react"
import {
  regionalAdminApi,
  type AssessmentsResponse,
  type AttendanceResponse,
  type CurriculumResponse,
  type PerformanceResponse,
  type RegionalPulse,
} from "@/lib/regional-admin-api"
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  ProgressBar,
  StatCard,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

const SCOPE_LABEL = "Scope: your jurisdiction · Period: All available records"

type Section<T> = { data: T | null; error: string | null }

function settle<T>(result: PromiseSettledResult<T>, label: string): Section<T> {
  if (result.status === "fulfilled") return { data: result.value, error: null }
  const raw = result.reason instanceof Error ? result.reason.message : ""
  return { data: null, error: raw ? `${label} (${raw})` : label }
}

function ProgressStat({
  label, value, hint,
}: {
  label: string
  value: number | null | undefined
  hint?: string
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
        {label}
      </p>
      <div className="mt-2">
        <ProgressBar value={value} />
      </div>
      {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
    </div>
  )
}

function PanelShell({
  title, error, onRetry, children,
}: {
  title: string
  error: string | null
  onRetry: () => void
  children: ReactNode
}) {
  return (
    <Panel
      title={title}
      actions={<span className="text-xs text-muted-foreground">{SCOPE_LABEL}</span>}
    >
      {error ? <ErrorState message={error} onRetry={onRetry} /> : children}
    </Panel>
  )
}

function score(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : value.toFixed(1)
}

function pct(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : `${value.toFixed(1)}%`
}

export default function RegionalPulsePage() {
  const [pulse, setPulse] = useState<Section<RegionalPulse>>({ data: null, error: null })
  const [attendance, setAttendance] = useState<Section<AttendanceResponse>>({
    data: null,
    error: null,
  })
  const [curriculum, setCurriculum] = useState<Section<CurriculumResponse>>({
    data: null,
    error: null,
  })
  const [assessments, setAssessments] = useState<Section<AssessmentsResponse>>({
    data: null,
    error: null,
  })
  const [performance, setPerformance] = useState<Section<PerformanceResponse>>({
    data: null,
    error: null,
  })
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    const [p, a, c, s, f] = await Promise.allSettled([
      regionalAdminApi.getPulse(),
      regionalAdminApi.getAttendance(),
      regionalAdminApi.getCurriculum(),
      regionalAdminApi.getAssessments(),
      regionalAdminApi.getPerformance(),
    ])
    setPulse(settle(p, "Unable to load the pulse metrics."))
    setAttendance(settle(a, "Unable to load attendance for your jurisdiction."))
    setCurriculum(settle(c, "Unable to load curriculum progress for your jurisdiction."))
    setAssessments(settle(s, "Unable to load assessments for your jurisdiction."))
    setPerformance(settle(f, "Unable to load performance for your jurisdiction."))
    setLoading(false)
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const allFailed =
    !loading &&
    pulse.error !== null &&
    attendance.error !== null &&
    curriculum.error !== null &&
    assessments.error !== null &&
    performance.error !== null

  const schoolsSorted = useMemo(() => {
    const rows = [...(performance.data?.schoolPerformance ?? [])]
    rows.sort(
      (a, b) =>
        (b.averageScore ?? Number.NEGATIVE_INFINITY) -
        (a.averageScore ?? Number.NEGATIVE_INFINITY),
    )
    return rows
  }, [performance.data])

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Regional Pulse"
        description="Live activity across your jurisdiction with metric, value and scope — no synthetic trends."
      />

      {allFailed && (
        <ErrorState
          message="Unable to load the regional pulse. Check your connection and try again."
          onRetry={load}
        />
      )}

      {loading && <LoadingState label="Loading regional pulse…" />}

      {!loading && !allFailed && (
        <>
          <section aria-label="Pulse metrics" className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-xs font-bold uppercase tracking-widest text-muted-foreground">
                Pulse metrics
              </h2>
              {pulse.data && (
                <p className="text-xs text-muted-foreground">
                  {SCOPE_LABEL} · Last updated: {formatDateTime(pulse.data.lastUpdated)}
                </p>
              )}
            </div>

            {pulse.error ? (
              <ErrorState message={pulse.error} onRetry={load} />
            ) : pulse.data ? (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <StatCard
                  label="Live now"
                  value={pulse.data.liveNow}
                  hint="Live classes in progress"
                />
                <StatCard label="Scheduled today" value={pulse.data.scheduledToday} />
                <StatCard label="Completed today" value={pulse.data.completedToday} />
                <StatCard label="This week total" value={pulse.data.totalThisWeek} />
                <StatCard label="Teachers" value={pulse.data.teachers} />
                <StatCard label="Learners" value={pulse.data.learners} />
                <StatCard
                  label="Published lessons"
                  value={pulse.data.publishedLessons}
                  hint={`of ${pulse.data.totalLessons.toLocaleString()} total lessons`}
                />
                <StatCard
                  label="Pending verifications"
                  value={pulse.data.pendingVerifications}
                  tone={pulse.data.pendingVerifications > 0 ? "warning" : "default"}
                />
                <StatCard
                  label="Alerts"
                  value={pulse.data.alertsCount}
                  tone={pulse.data.alertsCount > 0 ? "danger" : "default"}
                />
              </div>
            ) : (
              <EmptyState title="Pulse metrics are unavailable for your jurisdiction." />
            )}
          </section>

          <div className="grid gap-6 xl:grid-cols-2">
            <PanelShell title="Attendance" error={attendance.error} onRetry={load}>
              {attendance.data ? (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-3">
                    <ProgressStat
                      label="Overall attendance rate"
                      value={attendance.data.overallRate}
                    />
                    <StatCard
                      label="Students"
                      value={attendance.data.totalStudents ?? "—"}
                    />
                    <StatCard
                      label="Schools at risk"
                      value={attendance.data.schoolsAtRisk ?? "—"}
                      tone={(attendance.data.schoolsAtRisk ?? 0) > 0 ? "danger" : "default"}
                    />
                  </div>

                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                      Attendance by institution
                    </p>
                    {attendance.data.schoolAttendance.length === 0 ? (
                      <p className="mt-2 text-sm text-muted-foreground">
                        No institution attendance records were returned.
                      </p>
                    ) : (
                      <ul className="mt-2 space-y-2">
                        {attendance.data.schoolAttendance.map((school) => (
                          <li
                            key={school.institutionId}
                            className="grid gap-1.5 sm:grid-cols-[minmax(0,1fr)_minmax(0,1.5fr)] sm:items-center sm:gap-4"
                          >
                            <span className="flex min-w-0 items-center gap-2 text-sm text-foreground">
                              <School className="size-3.5 shrink-0 text-muted-foreground" />
                              <span className="truncate">{school.institutionName}</span>
                            </span>
                            <ProgressBar value={school.attendanceRate} />
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              ) : (
                <EmptyState title="No attendance records were returned for your jurisdiction." />
              )}
            </PanelShell>

            <PanelShell title="Curriculum" error={curriculum.error} onRetry={load}>
              {curriculum.data ? (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-3">
                    <ProgressStat
                      label="Overall progress"
                      value={curriculum.data.overallProgress}
                    />
                    <StatCard
                      label="Lessons completed"
                      value={curriculum.data.completedLessons ?? "—"}
                      hint={`of ${curriculum.data.totalLessons ?? 0} lessons`}
                    />
                    <StatCard
                      label="Schools on track"
                      value={curriculum.data.schoolsOnTrack ?? "—"}
                      hint={`${curriculum.data.schoolsBehind ?? 0} behind`}
                      tone={(curriculum.data.schoolsBehind ?? 0) > 0 ? "warning" : "success"}
                    />
                  </div>

                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                      Progress by subject
                    </p>
                    {curriculum.data.subjectProgress.length === 0 ? (
                      <p className="mt-2 text-sm text-muted-foreground">
                        No subject progress records were returned.
                      </p>
                    ) : (
                      <ul className="mt-2 space-y-2">
                        {curriculum.data.subjectProgress.map((subject) => (
                          <li
                            key={`${subject.subjectCode}-${subject.subjectName}`}
                            className="grid gap-1.5 sm:grid-cols-[minmax(0,1fr)_minmax(0,1.5fr)] sm:items-center sm:gap-4"
                          >
                            <span className="flex min-w-0 items-center gap-2 text-sm text-foreground">
                              <BookOpen className="size-3.5 shrink-0 text-muted-foreground" />
                              <span className="truncate">{subject.subjectName}</span>
                              <span className="text-xs text-muted-foreground">
                                {subject.completedLessons ?? 0}/{subject.totalLessons ?? 0}
                              </span>
                            </span>
                            <ProgressBar value={subject.progressRate} />
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              ) : (
                <EmptyState title="No curriculum records were returned for your jurisdiction." />
              )}
            </PanelShell>
          </div>

          <div className="grid gap-6 xl:grid-cols-2">
            <PanelShell title="Assessments" error={assessments.error} onRetry={load}>
              {assessments.data ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <StatCard
                    label="Total assessments"
                    value={assessments.data.totalAssessments ?? "—"}
                  />
                  <StatCard
                    label="Completed"
                    value={assessments.data.completedAssessments ?? "—"}
                  />
                  <StatCard
                    label="Average score"
                    value={score(assessments.data.averageScore)}
                    hint="Across completed assessments"
                  />
                  <StatCard label="Pass rate" value={pct(assessments.data.passRate)} />
                  <StatCard
                    label="Pending grading"
                    value={assessments.data.pendingGrading ?? "—"}
                    tone={(assessments.data.pendingGrading ?? 0) > 0 ? "warning" : "default"}
                  />
                </div>
              ) : (
                <EmptyState title="No assessment records were returned for your jurisdiction." />
              )}
            </PanelShell>

            <PanelShell title="Performance" error={performance.error} onRetry={load}>
              {performance.data ? (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <StatCard
                      label="Overall average"
                      value={score(performance.data.overallAverage)}
                    />
                    <StatCard label="Pass rate" value={pct(performance.data.passRate)} />
                    <StatCard
                      label="Total assessments"
                      value={performance.data.totalAssessments ?? "—"}
                    />
                    <StatCard
                      label="Report cards"
                      value={performance.data.totalReportCards ?? "—"}
                    />
                  </div>

                  {schoolsSorted.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                      No school performance records were returned.
                    </p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-sm">
                        <thead>
                          <tr className="border-b border-border text-[11px] uppercase tracking-widest text-muted-foreground">
                            <th className="px-3 py-2 font-bold">Institution</th>
                            <th className="px-3 py-2 font-bold">Average</th>
                            <th className="px-3 py-2 font-bold">Pass rate</th>
                            <th className="px-3 py-2 font-bold">Students</th>
                            <th className="px-3 py-2 font-bold">Assessments</th>
                          </tr>
                        </thead>
                        <tbody>
                          {schoolsSorted.map((school) => (
                            <tr
                              key={school.institutionId}
                              className="border-b border-border/60 transition-colors hover:bg-muted/40"
                            >
                              <td className="px-3 py-2.5">
                                <p className="font-medium text-foreground">
                                  {school.institutionName}
                                </p>
                                <p className="text-xs text-muted-foreground">
                                  {school.institutionCode}
                                </p>
                              </td>
                              <td className="px-3 py-2.5 tabular-nums">
                                {score(school.averageScore)}
                              </td>
                              <td className="px-3 py-2.5 tabular-nums">
                                {pct(school.passRate)}
                              </td>
                              <td className="px-3 py-2.5 tabular-nums">
                                {school.studentCount ?? "—"}
                              </td>
                              <td className="px-3 py-2.5 tabular-nums">
                                {school.assessmentCount ?? "—"}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              ) : (
                <EmptyState title="No performance records were returned for your jurisdiction." />
              )}
            </PanelShell>
          </div>
        </>
      )}
    </div>
  )
}
