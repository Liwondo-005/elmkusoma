"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { dashboardApi, type DashboardSummary, learningApi, type LessonProgress } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { BarChart3, CheckCircle, Clock, TrendingUp, Award, BookOpen, Star, Trophy, Target } from "lucide-react"

export default function DashboardProgressPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [lessonProgress, setLessonProgress] = useState<LessonProgress[]>([])
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [summaryData, progressData] = await Promise.all([
        dashboardApi.getSummary().catch(() => null),
        learningApi.getStudentProgress(user!.id).catch(() => []),
      ])
      setSummary(summaryData)
      setLessonProgress(progressData)
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const completionRate = summary && summary.totalLessonsStarted > 0
    ? Math.round((summary.completedLessons / summary.totalLessonsStarted) * 100)
    : 0

  const completedLessons = lessonProgress.filter((p) => p.completionPercentage >= 100).length
  const inProgressLessons = lessonProgress.filter((p) => p.completionPercentage > 0 && p.completionPercentage < 100).length
  const notStartedLessons = lessonProgress.filter((p) => p.completionPercentage === 0).length

  if (isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        {/* Header */}
        <div className="rounded-2xl border border-border bg-gradient-to-br from-teal/5 via-card to-primary/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-teal/10">
              <TrendingUp className="size-6 text-teal" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("growth.title")}</h1>
              <p className="text-sm text-muted-foreground">{t("growth.subtitle")}
            </div>
          </div>
        </div>

        {/* Growth Stats */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
                <CheckCircle className="size-5 text-green-600" />
              </div>
              <div>
                <p className="text-2xl font-extrabold text-foreground">{completedLessons}</p>
                <p className="text-xs text-muted-foreground">{ts("completed")}</p>
              </div>
            </div>
          </div>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
                <Clock className="size-5 text-amber-600" />
              </div>
              <div>
                <p className="text-2xl font-extrabold text-foreground">{inProgressLessons}</p>
                <p className="text-xs text-muted-foreground">{ts("inProgress")}</p>
              </div>
            </div>
          </div>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
                <BookOpen className="size-5 text-blue-600" />
              </div>
              <div>
                <p className="text-2xl font-extrabold text-foreground">{summary?.activeEnrollments ?? 0}</p>
                <p className="text-xs text-muted-foreground">{t("growth.subjects")}
              </div>
            </div>
          </div>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                <Star className="size-5 text-primary" />
              </div>
              <div>
                <p className="text-2xl font-extrabold text-foreground">{summary?.overallAverage ?? 0}%</p>
                <p className="text-xs text-muted-foreground">{t("growth.average")}
              </div>
            </div>
          </div>
        </div>

        {/* Overall Progress Bar */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("growth.overall")}
          <div className="flex items-center gap-4">
            <div className="h-5 flex-1 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-gradient-to-r from-teal to-primary transition-all"
                style={{ width: `${completionRate}%` }}
              />
            </div>
            <span className="text-2xl font-bold text-primary">{completionRate}%</span>
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            {t("growth.lessonsSummary", { done: summary?.completedLessons ?? 0, total: summary?.totalLessonsStarted ?? 0 })}
          </p>
        </div>

        {/* Learning Milestones */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("growth.milestones")}
          <div className="space-y-3">
            {completedLessons >= 1 && (
              <div className="flex items-center gap-3 rounded-xl bg-green-50 p-3">
                <div className="flex size-8 items-center justify-center rounded-full bg-green-100">
                  <CheckCircle className="size-4 text-green-600" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">{t("growth.firstDone")}
                  <p className="text-xs text-muted-foreground">{t("growth.firstDoneDesc")}
                </div>
              </div>
            )}
            {completedLessons >= 5 && (
              <div className="flex items-center gap-3 rounded-xl bg-blue-50 p-3">
                <div className="flex size-8 items-center justify-center rounded-full bg-blue-100">
                  <Star className="size-4 text-blue-600" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">{t("growth.fastLearner")}
                  <p className="text-xs text-muted-foreground">{t("growth.fastLearnerDesc")}
                </div>
              </div>
            )}
            {completedLessons >= 10 && (
              <div className="flex items-center gap-3 rounded-xl bg-purple-50 p-3">
                <div className="flex size-8 items-center justify-center rounded-full bg-purple-100">
                  <Trophy className="size-4 text-purple-600" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">{t("growth.champion")}
                  <p className="text-xs text-muted-foreground">{t("growth.championDesc")}
                </div>
              </div>
            )}
            {completionRate >= 50 && (
              <div className="flex items-center gap-3 rounded-xl bg-amber-50 p-3">
                <div className="flex size-8 items-center justify-center rounded-full bg-amber-100">
                  <Target className="size-4 text-amber-600" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">{t("growth.halfway")}
                  <p className="text-xs text-muted-foreground">{t("growth.halfwayDesc")}
                </div>
              </div>
            )}
            {completedLessons === 0 && inProgressLessons === 0 && (
              <div className="flex flex-col items-center py-8 text-center">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-muted">
                  <Star className="size-6 text-muted-foreground" />
                </div>
                <p className="mt-3 text-sm font-medium text-foreground">{t("growth.emptyTitle")}
                <p className="text-xs text-muted-foreground">{t("growth.emptyDesc")}
              </div>
            )}
          </div>
        </div>

        {/* Attendance */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("growth.attendance")}
          <div className="flex items-center gap-4">
            <div className="text-center">
              <p className="text-3xl font-extrabold text-primary">{summary?.monthAttendanceRate ?? 0}%</p>
              <p className="text-xs text-muted-foreground mt-1">{t("growth.thisMonth")}
            </div>
            <div className="flex-1">
              <div className="h-4 overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-teal to-green-500 transition-all"
                  style={{ width: `${summary?.monthAttendanceRate ?? 0}%` }}
                />
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                {(summary?.monthAttendanceRate ?? 0) >= 90
                  ? t("growth.attExcellent")
                  : (summary?.monthAttendanceRate ?? 0) >= 75
                    ? t("growth.attGood")
                    : t("growth.attImprove")}
              </p>
            </div>
          </div>
        </div>
      </div>
    )
  }

  /* Non-Primary: Original progress page */
  const stats = [
    { label: t("growth.statStarted"), value: summary?.totalLessonsStarted ?? 0 },
    { label: t("growth.statCompleted"), value: summary?.completedLessons ?? 0 },
    { label: t("growth.statAttendance"), value: `${summary?.monthAttendanceRate ?? 0}%` },
    { label: t("growth.statAverage"), value: `${summary?.overallAverage ?? 0}%` },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("growth.myProgress")}
        <p className="mt-1 text-sm text-muted-foreground">{t("growth.myProgressDesc")}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <p className="text-2xl font-extrabold text-foreground">{s.value}</p>
            <p className="mt-1 text-sm text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("growth.overallCompletion")}
        <div className="flex items-center gap-4">
          <div className="h-4 flex-1 overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-teal transition-all"
              style={{ width: `${completionRate}%` }}
            />
          </div>
          <span className="text-2xl font-bold text-teal">{completionRate}%</span>
        </div>
        <p className="mt-2 text-sm text-muted-foreground">
          {t("growth.lessonsSummary", { done: summary?.completedLessons ?? 0, total: summary?.totalLessonsStarted ?? 0 })}
        </p>
      </div>

      <div className="rounded-2xl border border-border bg-card p-12 text-center">
        <BarChart3 className="mx-auto size-12 text-muted-foreground/50" />
        <h3 className="mt-4 text-lg font-semibold text-foreground">{t("growth.detailed")}
        <p className="mt-2 text-sm text-muted-foreground">
          {t("growth.detailedDesc")}
        </p>
      </div>
    </div>
  )
}
