"use client"

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { BookOpen, School } from "lucide-react"
import { useTranslations } from "next-intl"
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
  scopeLabel,
}: {
  title: string
  error: string | null
  onRetry: () => void
  children: ReactNode
  scopeLabel: string
}) {
  return (
    <Panel
      title={title}
      actions={<span className="text-xs text-muted-foreground">{scopeLabel}</span>}
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
  const t = useTranslations("regionalAdmin")
  const scopeLabel = t("pulse.scopeLabel")
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
    setPulse(settle(p, t("pulse.pulseLoadError")))
    setAttendance(settle(a, t("pulse.attendanceLoadError")))
    setCurriculum(settle(c, t("pulse.curriculumLoadError")))
    setAssessments(settle(s, t("pulse.assessmentsLoadError")))
    setPerformance(settle(f, t("pulse.performanceLoadError")))
    setLoading(false)
  }, [t])

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
        title={t("pulse.title")}
        description={t("pulse.description")}
      />

      {allFailed && (
        <ErrorState
          message={t("pulse.allFailedError")}
          onRetry={load}
        />
      )}

      {loading && <LoadingState label={t("pulse.loadingLabel")} />}

      {!loading && !allFailed && (
        <>
          <section aria-label={t("pulse.metricsAriaLabel")} className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-xs font-bold uppercase tracking-widest text-muted-foreground">
                {t("pulse.metricsHeading")}
              </h2>
              {pulse.data && (
                <p className="text-xs text-muted-foreground">
                  {scopeLabel} · {t("pulse.lastUpdatedLabel")}: {formatDateTime(pulse.data.lastUpdated)}
                </p>
              )}
            </div>

            {pulse.error ? (
              <ErrorState message={pulse.error} onRetry={load} />
            ) : pulse.data ? (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <StatCard
                  label={t("pulse.statLiveNow")}
                  value={pulse.data.liveNow}
                  hint={t("pulse.statLiveNowHint")}
                />
                <StatCard label={t("pulse.statScheduledToday")} value={pulse.data.scheduledToday} />
                <StatCard label={t("pulse.statCompletedToday")} value={pulse.data.completedToday} />
                <StatCard label={t("pulse.statWeekTotal")} value={pulse.data.totalThisWeek} />
                <StatCard label={t("pulse.statTeachers")} value={pulse.data.teachers} />
                <StatCard label={t("pulse.statLearners")} value={pulse.data.learners} />
                <StatCard
                  label={t("pulse.statPublishedLessons")}
                  value={pulse.data.publishedLessons}
                  hint={t("pulse.statPublishedLessonsHint", { count: pulse.data.totalLessons.toLocaleString() })}
                />
                <StatCard
                  label={t("pulse.statPendingVerifications")}
                  value={pulse.data.pendingVerifications}
                  tone={pulse.data.pendingVerifications > 0 ? "warning" : "default"}
                />
                <StatCard
                  label={t("pulse.statAlerts")}
                  value={pulse.data.alertsCount}
                  tone={pulse.data.alertsCount > 0 ? "danger" : "default"}
                />
              </div>
            ) : (
              <EmptyState title={t("pulse.metricsUnavailable")} />
            )}
          </section>

          <div className="grid gap-6 xl:grid-cols-2">
            <PanelShell title={t("pulse.attendanceTitle")} error={attendance.error} onRetry={load} scopeLabel={scopeLabel}>
              {attendance.data ? (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-3">
                    <ProgressStat
                      label={t("pulse.overallAttendanceLabel")}
                      value={attendance.data.overallRate}
                    />
                    <StatCard
                      label={t("pulse.studentsLabel")}
                      value={attendance.data.totalStudents ?? "—"}
                    />
                    <StatCard
                      label={t("pulse.schoolsAtRiskLabel")}
                      value={attendance.data.schoolsAtRisk ?? "—"}
                      tone={(attendance.data.schoolsAtRisk ?? 0) > 0 ? "danger" : "default"}
                    />
                  </div>

                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                      {t("pulse.attendanceByInstitution")}
                    </p>
                    {attendance.data.schoolAttendance.length === 0 ? (
                      <p className="mt-2 text-sm text-muted-foreground">
                        {t("pulse.noAttendanceRecords")}
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
                <EmptyState title={t("pulse.noAttendanceForJurisdiction")} />
              )}
            </PanelShell>

            <PanelShell title={t("pulse.curriculumTitle")} error={curriculum.error} onRetry={load} scopeLabel={scopeLabel}>
              {curriculum.data ? (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-3">
                    <ProgressStat
                      label={t("pulse.overallProgressLabel")}
                      value={curriculum.data.overallProgress}
                    />
                    <StatCard
                      label={t("pulse.lessonsCompletedLabel")}
                      value={curriculum.data.completedLessons ?? "—"}
                      hint={t("pulse.lessonsCompletedHint", { count: curriculum.data.totalLessons ?? 0 })}
                    />
                    <StatCard
                      label={t("pulse.schoolsOnTrackLabel")}
                      value={curriculum.data.schoolsOnTrack ?? "—"}
                      hint={t("pulse.schoolsBehindHint", { count: curriculum.data.schoolsBehind ?? 0 })}
                      tone={(curriculum.data.schoolsBehind ?? 0) > 0 ? "warning" : "success"}
                    />
                  </div>

                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                      {t("pulse.progressBySubject")}
                    </p>
                    {curriculum.data.subjectProgress.length === 0 ? (
                      <p className="mt-2 text-sm text-muted-foreground">
                        {t("pulse.noSubjectProgress")}
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
                <EmptyState title={t("pulse.noCurriculumForJurisdiction")} />
              )}
            </PanelShell>
          </div>

          <div className="grid gap-6 xl:grid-cols-2">
            <PanelShell title={t("pulse.assessmentsTitle")} error={assessments.error} onRetry={load} scopeLabel={scopeLabel}>
              {assessments.data ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <StatCard
                    label={t("pulse.totalAssessmentsLabel")}
                    value={assessments.data.totalAssessments ?? "—"}
                  />
                  <StatCard
                    label={t("pulse.completedLabel")}
                    value={assessments.data.completedAssessments ?? "—"}
                  />
                  <StatCard
                    label={t("pulse.averageScoreLabel")}
                    value={score(assessments.data.averageScore)}
                    hint={t("pulse.averageScoreHint")}
                  />
                  <StatCard label={t("pulse.passRateLabel")} value={pct(assessments.data.passRate)} />
                  <StatCard
                    label={t("pulse.pendingGradingLabel")}
                    value={assessments.data.pendingGrading ?? "—"}
                    tone={(assessments.data.pendingGrading ?? 0) > 0 ? "warning" : "default"}
                  />
                </div>
              ) : (
                <EmptyState title={t("pulse.noAssessmentsForJurisdiction")} />
              )}
            </PanelShell>

            <PanelShell title={t("pulse.performanceTitle")} error={performance.error} onRetry={load} scopeLabel={scopeLabel}>
              {performance.data ? (
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <StatCard
                      label={t("pulse.overallAverageLabel")}
                      value={score(performance.data.overallAverage)}
                    />
                    <StatCard label={t("pulse.passRateLabel")} value={pct(performance.data.passRate)} />
                    <StatCard
                      label={t("pulse.totalAssessmentsLabel")}
                      value={performance.data.totalAssessments ?? "—"}
                    />
                    <StatCard
                      label={t("pulse.reportCardsLabel")}
                      value={performance.data.totalReportCards ?? "—"}
                    />
                  </div>

                  {schoolsSorted.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                      {t("pulse.noSchoolPerformance")}
                    </p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-sm">
                        <thead>
                          <tr className="border-b border-border text-[11px] uppercase tracking-widest text-muted-foreground">
                            <th className="px-3 py-2 font-bold">{t("pulse.colInstitution")}</th>
                            <th className="px-3 py-2 font-bold">{t("pulse.colAverage")}</th>
                            <th className="px-3 py-2 font-bold">{t("pulse.colPassRate")}</th>
                            <th className="px-3 py-2 font-bold">{t("pulse.colStudents")}</th>
                            <th className="px-3 py-2 font-bold">{t("pulse.colAssessments")}</th>
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
                <EmptyState title={t("pulse.noPerformanceForJurisdiction")} />
              )}
            </PanelShell>
          </div>
        </>
      )}
    </div>
  )
}
