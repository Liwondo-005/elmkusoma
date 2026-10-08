"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Award, BookOpen, CheckCircle, ClipboardList, FolderOpen, Loader2, Sparkles, Star, Target, TrendingUp, Users } from "lucide-react"
import {
  parentApi,
  type AchievementItem,
  type AttendanceData,
  type EntitlementItem,
  type LearningProgressData,
  type ParentAchievements,
  type ParentIntelligence,
  type ResultData,
  type ReportCardItem,
  type SubjectPerformanceItem,
} from "@/lib/parent-api"
import { useSelectedChild } from "@/hooks/use-selected-child"

export default function ParentPortfolioPage() {
  const tp = useTranslations("parentPortfolio")
  const ts = useTranslations("status")
  const [attendance, setAttendance] = useState<AttendanceData | null>(null)
  const [results, setResults] = useState<ResultData | null>(null)
  const [subjects, setSubjects] = useState<SubjectPerformanceItem[]>([])
  const [learning, setLearning] = useState<LearningProgressData | null>(null)
  const [achievements, setAchievements] = useState<ParentAchievements | null>(null)
  const [entitlements, setEntitlements] = useState<EntitlementItem[]>([])
  const [intelligence, setIntelligence] = useState<ParentIntelligence | null>(null)
  const [dataLoading, setDataLoading] = useState(false)
  const { children, selectedChildId, setSelectedChildId, loading, error } = useSelectedChild()

  useEffect(() => {
    if (!selectedChildId) return
    setDataLoading(true)
    setAttendance(null)
    setResults(null)
    setSubjects([])
    setLearning(null)
    setAchievements(null)
    setEntitlements([])
    setIntelligence(null)
    Promise.all([
      parentApi.getChildAttendance(selectedChildId).catch(() => null),
      parentApi.getChildResults(selectedChildId).catch(() => null),
      parentApi.getChildSubjectPerformance(selectedChildId).catch(() => null),
      parentApi.getChildLearningProgress(selectedChildId).catch(() => null),
      parentApi.getChildAchievements(selectedChildId).catch(() => null),
      parentApi.getChildEntitlements(selectedChildId).catch(() => null),
      parentApi.getChildIntelligence(selectedChildId).catch(() => null),
    ]).then(([att, res, sub, lp, ach, ent, intel]) => {
      setAttendance(att)
      setResults(res)
      setSubjects(sub?.subjects || [])
      setLearning(lp)
      setAchievements(ach)
      setEntitlements(Array.isArray(ent) ? ent : [])
      setIntelligence(intel)
      setDataLoading(false)
    })
  }, [selectedChildId])

  if (loading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl py-16 text-center">
        <p className="text-sm text-muted-foreground">{error}</p>
        <p className="mt-2 text-xs text-muted-foreground">{tp("loadError")}</p>
      </div>
    )
  }

  const child = children.find((c) => c.studentId === selectedChildId)

  if (!child) {
    return (
      <div className="rounded-2xl border border-dashed border-border py-16 text-center">
        <Users className="mx-auto mb-3 size-10 text-muted-foreground" />
        <p className="text-sm font-medium text-foreground">{tp("noChildTitle")}</p>
        <p className="mt-1 text-xs text-muted-foreground">{tp("noChildDesc")}</p>
      </div>
    )
  }

  const latestReport = pickLatestReport(results?.reportCards || [])
  const attendanceRate = attendance ? attendance.attendancePercentage : child.attendancePercentage
  const overallProgress = learning ? learning.overallProgress : child.learningProgress
  const strongSubjects = [...subjects]
    .filter((s) => s.averageMark !== null)
    .sort((a, b) => (b.averageMark || 0) - (a.averageMark || 0))
    .slice(0, 3)
  const courses = learning?.courses || []
  const achievementList = achievements?.achievements || []
  const highlights = intelligence?.weeklyBrief?.highlights || []
  const hasData =
    !!latestReport ||
    strongSubjects.length > 0 ||
    courses.length > 0 ||
    achievementList.length > 0 ||
    highlights.length > 0 ||
    entitlements.length > 0 ||
    (!!attendance && attendance.totalDays > 0)

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tp("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {tp("subtitle", { name: child.studentName, className: child.className })}
        </p>
      </div>

      {children.length > 1 && (
        <div className="space-y-2">
          <div className="flex gap-2 overflow-x-auto pb-1">
            {children.map((option) => (
              <button key={option.studentId} onClick={() => setSelectedChildId(option.studentId)}
                className={`shrink-0 rounded-xl border px-4 py-2 text-sm font-medium transition-colors ${selectedChildId === option.studentId ? "border-primary bg-primary text-primary-foreground" : "border-border bg-card text-foreground hover:bg-muted"}`}>
                {option.studentName}
              </button>
            ))}
          </div>
          <p className="text-xs text-muted-foreground">{tp("selectionHint")}</p>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Tile label={tp("attendanceLabel")} value={attendanceRate != null ? `${Math.round(attendanceRate)}%` : "—"} hint={attendance ? tp("attendanceHint", { count: attendance.presentDays }) : tp("noData")} icon={ClipboardList} />
        <Tile label={tp("gradeLabel")} value={latestReport?.overallGrade || child.latestGrade || "—"} hint={latestReport?.averageMark != null ? tp("averageHint", { value: Math.round(latestReport.averageMark) }) : tp("noData")} icon={Award} />
        <Tile label={tp("progressLabel")} value={overallProgress != null ? `${Math.round(overallProgress)}%` : "—"} hint={learning ? tp("progressHint", { done: learning.completedCourses, total: learning.totalCourses }) : tp("noData")} icon={TrendingUp} />
        <Tile label={tp("achievementsLabel")} value={achievements?.totalAchievements ?? achievementList.length} hint={tp("achievementsHint")} icon={Star} />
      </div>

      {dataLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : !hasData ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <FolderOpen className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{tp("emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{tp("emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-6">
          {strongSubjects.length > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("strengthsTitle")}</h2>
              <div className="mt-4 space-y-3">
                {strongSubjects.map((subject) => (
                  <div key={subject.subjectId} className="flex items-center gap-4 rounded-xl border border-border bg-muted/30 p-4">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-foreground">{subject.subjectName}</p>
                      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                        <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, subject.averageMark || 0)}%` }} />
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-bold text-foreground">{subject.averageMark != null ? `${Math.round(subject.averageMark)}%` : "—"}</p>
                      <p className="text-[10px] text-muted-foreground">{tp("assessmentsDone", { done: subject.completedAssessments, total: subject.totalAssessments })}</p>
                    </div>
                    {subject.grade && (
                      <span className="shrink-0 rounded bg-primary/10 px-2 py-1 text-xs font-bold text-primary">{subject.grade}</span>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {courses.length > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("coursesTitle")}</h2>
              <div className="mt-4 space-y-3">
                {courses.map((course) => (
                  <div key={course.courseId} className="flex items-center gap-4 rounded-xl border border-border bg-muted/30 p-4">
                    <BookOpen className="size-4 shrink-0 text-muted-foreground" />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-foreground">{course.courseName}</p>
                      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                        <div className="h-full rounded-full bg-primary" style={{ width: `${Math.round(course.progressPercentage || 0)}%` }} />
                      </div>
                      <p className="mt-1 text-[10px] text-muted-foreground">
                        {tp("lessonsCount", { done: course.completedLessons, total: course.totalLessons })}
                      </p>
                    </div>
                    <span className="text-xs font-bold text-foreground">{Math.round(course.progressPercentage || 0)}%</span>
                  </div>
                ))}
              </div>
            </section>
          )}

          {latestReport && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("reportTitle")}</h2>
              <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <ReportTile label={tp("termLabel")} value={`${latestReport.term} · ${latestReport.academicYear}`} />
                <ReportTile label={tp("gradeLabel")} value={latestReport.overallGrade || "—"} />
                <ReportTile label={tp("averageLabel")} value={latestReport.averageMark != null ? latestReport.averageMark.toFixed(1) : "—"} />
                <ReportTile label={tp("rankLabel")} value={latestReport.classRank != null && latestReport.totalStudentsInClass != null ? `${latestReport.classRank}/${latestReport.totalStudentsInClass}` : "—"} />
              </div>
              {latestReport.remarks && (
                <p className="mt-3 rounded-lg bg-muted/30 p-3 text-xs text-muted-foreground">
                  <span className="font-semibold text-foreground">{tp("remarksLabel")} </span>{latestReport.remarks}
                </p>
              )}
            </section>
          )}

          {achievementList.length > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("achievementsTitle")}</h2>
              <div className="mt-4 space-y-2">
                {achievementList.slice(0, 8).map((achievement) => (
                  <AchievementRow key={achievement.id} achievement={achievement} label={tp("earnedOn", { date: formatDate(achievement.achievedAt) })} />
                ))}
              </div>
            </section>
          )}

          {attendance && attendance.totalDays > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("attendanceTitle")}</h2>
              <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-5">
                <ReportTile label={tp("totalDaysLabel")} value={attendance.totalDays} />
                <ReportTile label={ts("present")} value={attendance.presentDays} />
                <ReportTile label={ts("absent")} value={attendance.absentDays} />
                <ReportTile label={ts("late")} value={attendance.lateDays} />
                <ReportTile label={tp("rateLabel")} value={`${Math.round(attendance.attendancePercentage)}%`} />
              </div>
            </section>
          )}

          {highlights.length > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("highlightsTitle")}</h2>
              <div className="mt-3 space-y-1">
                {highlights.map((highlight, index) => (
                  <div key={index} className="flex items-start gap-2 text-sm text-foreground">
                    <CheckCircle className="mt-0.5 size-4 shrink-0 text-green-600" />
                    {highlight}
                  </div>
                ))}
              </div>
            </section>
          )}

          {entitlements.length > 0 && (
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{tp("servicesTitle")}</h2>
              <div className="mt-4 flex flex-wrap gap-2">
                {entitlements.map((entitlement) => (
                  <span key={entitlement.id} className="inline-flex items-center gap-2 rounded-lg border border-border bg-muted/30 px-3 py-1.5 text-xs font-medium text-foreground">
                    <Target className="size-3 text-muted-foreground" />
                    {entitlement.serviceType.replace(/_/g, " ").toLowerCase()}
                  </span>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      <p className="text-xs text-muted-foreground">{tp("sourceNote")}</p>
    </div>
  )
}

function Tile({ label, value, hint, icon: Icon }: { label: string; value: string | number; hint: string; icon: typeof Sparkles }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-2">
        <Icon className="size-4 text-muted-foreground" />
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
      </div>
      <p className="mt-1 text-2xl font-extrabold text-foreground">{value}</p>
      <p className="text-xs text-muted-foreground">{hint}</p>
    </div>
  )
}

function ReportTile({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg bg-muted/50 p-3 text-center">
      <p className="text-lg font-bold text-foreground">{value}</p>
      <p className="text-[10px] text-muted-foreground">{label}</p>
    </div>
  )
}

function AchievementRow({ achievement, label }: { achievement: AchievementItem; label: string }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border p-3">
      <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
        <Sparkles className="size-4" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{achievement.title}</p>
        {achievement.description && <p className="text-xs text-muted-foreground">{achievement.description}</p>}
      </div>
      <span className="shrink-0 text-[10px] text-muted-foreground">{label}</span>
    </div>
  )
}

function pickLatestReport(reportCards: ReportCardItem[]): ReportCardItem | null {
  if (reportCards.length === 0) return null
  const withDates = reportCards.filter((rc) => !!rc.publishedAt)
  if (withDates.length === 0) return reportCards[0]
  return withDates.reduce((latest, current) => (new Date(current.publishedAt as string).getTime() > new Date(latest.publishedAt as string).getTime() ? current : latest))
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })
}