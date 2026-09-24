"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type Enrollment, type CourseSummary, type CourseModuleSummary, type CourseLesson } from "@/lib/learner-api"
import { LoadingState, EmptyState } from "@/components/learner/shared"
import { BarChart3, BookOpen, ArrowRight, CheckCircle, Clock, TrendingUp, AlertCircle } from "lucide-react"

interface CourseProgressDetail {
  enrollment: Enrollment
  modules: CourseModuleSummary[]
  totalLessons: number
  completedLessons: number
}

export default function ProgressPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [progressDetails, setProgressDetails] = useState<CourseProgressDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const enrollmentsData = await learnerApi.getEnrollments().catch(() => [])
      setEnrollments(enrollmentsData)

      const details: CourseProgressDetail[] = []
      for (const enrollment of enrollmentsData) {
        try {
          const courseDetail = await learnerApi.getCourse(enrollment.courseId)
          let totalLessons = 0
          for (const mod of courseDetail.modules || []) {
            const lessons = await learnerApi.getModuleLessons(mod.id)
            totalLessons += lessons.length
          }
          const completedLessons = Math.round((enrollment.progressPercentage / 100) * totalLessons)
          details.push({
            enrollment,
            modules: courseDetail.modules || [],
            totalLessons,
            completedLessons,
          })
        } catch {
          details.push({
            enrollment,
            modules: [],
            totalLessons: 0,
            completedLessons: 0,
          })
        }
      }
      setProgressDetails(details)
    } catch {
      setError(t("prog.loadError"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  const totalCourses = enrollments.length
  const completedCourses = enrollments.filter((e) => e.completedAt).length
  const inProgressCourses = enrollments.filter((e) => !e.completedAt).length
  const avgProgress = totalCourses > 0 ? Math.round(enrollments.reduce((sum, e) => sum + e.progressPercentage, 0) / totalCourses) : 0

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("prog.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("prog.subtitle")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("prog.totalCourses")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{totalCourses}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("prog.inProgress")}</p>
          <p className="mt-1 text-2xl font-extrabold text-blue-500">{inProgressCourses}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("prog.completedLabel")}</p>
          <p className="mt-1 text-2xl font-extrabold text-green-600">{completedCourses}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("prog.avgProgress")}</p>
          <p className="mt-1 text-2xl font-extrabold text-teal">{avgProgress}%</p>
        </div>
      </div>

      {loading ? (
        <LoadingState />
      ) : enrollments.length === 0 ? (
        <EmptyState
          icon={<BarChart3 className="size-8" />}
          title={t("prog.emptyTitle")}
          description={t("prog.emptyDesc")}
          action={
            <Link
              href="/dashboard/learner/courses"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              {t("exploreCourses")} <ArrowRight className="size-4" />
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {progressDetails.map((detail) => (
            <div key={detail.enrollment.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-semibold text-foreground truncate">{detail.enrollment.courseTitle}</h3>
                    {detail.enrollment.completedAt ? (
                      <span className="inline-flex items-center gap-1 shrink-0 rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                        <CheckCircle className="size-3" /> {t("prog.completedBadge")}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 shrink-0 rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-500">
                        <Clock className="size-3" /> {t("prog.inProgressBadge")}
                      </span>
                    )}
                  </div>
                  {detail.enrollment.courseDescription && (
                    <p className="mt-1 text-xs text-muted-foreground line-clamp-1">{detail.enrollment.courseDescription}</p>
                  )}
                </div>
                <Link
                  href={`/dashboard/learner/courses/${detail.enrollment.courseId}`}
                  className="shrink-0 text-xs font-medium text-primary hover:underline"
                >
                  {t("prog.viewCourse")}
                </Link>
              </div>

              <div className="mt-4">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">
                    {t("prog.lessonsLine", { done: detail.completedLessons, total: detail.totalLessons })}
                  </span>
                  <span className="font-semibold text-teal">{detail.enrollment.progressPercentage}%</span>
                </div>
                <div className="mt-1.5 h-2.5 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-teal transition-all"
                    style={{ width: `${detail.enrollment.progressPercentage}%` }}
                  />
                </div>
              </div>

              {detail.modules.length > 0 && (
                <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                  {detail.modules.sort((a, b) => a.sortOrder - b.sortOrder).map((mod) => (
                    <div key={mod.id} className="rounded-lg border border-border bg-muted/30 px-3 py-2">
                      <p className="text-[11px] font-medium text-foreground truncate">{mod.title}</p>
                      <p className="text-[10px] text-muted-foreground">{t("prog.modLessons", { count: mod.lessonCount })}</p>
                    </div>
                  ))}
                </div>
              )}

              <div className="mt-3 flex items-center gap-4 text-[10px] text-muted-foreground">
                <span>{t("prog.enrolledOn", { date: new Date(detail.enrollment.enrolledAt).toLocaleDateString() })}</span>
                {detail.enrollment.completedAt && (
                  <span>{t("prog.completedOn", { date: new Date(detail.enrollment.completedAt).toLocaleDateString() })}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
