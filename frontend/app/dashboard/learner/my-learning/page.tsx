"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment, getLastAccessedLesson } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { BookOpen, ArrowRight, Loader2, AlertCircle, CheckCircle, Clock, Search } from "lucide-react"

export default function MyLearningPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<"all" | "in-progress" | "completed">("all")
  const [search, setSearch] = useState("")

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadEnrollments()
  }, [user])

  async function loadEnrollments() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getEnrollments()
      setEnrollments(data)
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  const filteredEnrollments = enrollments.filter((e) => {
    const matchesSearch = search === "" || (e.courseTitle || "").toLowerCase().includes(search.toLowerCase())
    const matchesFilter = filter === "all" ||
      (filter === "in-progress" && !e.completedAt) ||
      (filter === "completed" && e.completedAt)
    return matchesSearch && matchesFilter
  })

  const inProgress = enrollments.filter((e) => !e.completedAt)
  const completed = enrollments.filter((e) => e.completedAt)

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>
  }

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("myLearning.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("myLearning.subtitle")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
            <button onClick={() => { setError(null); loadEnrollments() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("myLearning.totalEnrolled")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{enrollments.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("myLearning.inProgress")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{inProgress.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">{t("myLearning.completed")}</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{completed.length}</p>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("myLearning.searchPlaceholder")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label={t("myLearning.searchPlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setFilter("all")}
            aria-label={tc("all")}
            aria-pressed={filter === "all"}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "all" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            {tc("all")}
          </button>
          <button
            onClick={() => setFilter("in-progress")}
            aria-label={t("myLearning.inProgress")}
            aria-pressed={filter === "in-progress"}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "in-progress" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            {t("myLearning.inProgress")}
          </button>
          <button
            onClick={() => setFilter("completed")}
            aria-label={t("myLearning.completed")}
            aria-pressed={filter === "completed"}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "completed" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            {t("myLearning.completed")}
          </button>
        </div>
      </div>

      {loading ? (
        <LoadingState />
      ) : filteredEnrollments.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title={search || filter !== "all" ? t("myLearning.noMatching") : t("myLearning.noEnrollments")}
          description={search || filter !== "all" ? t("myLearning.tryFilters") : t("myLearning.startExploring")}
          action={
            !search && filter === "all" ? (
              <Link
                href="/dashboard/learner/courses"
                aria-label={t("myLearning.exploreCourses")}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                {t("myLearning.exploreCourses")} <ArrowRight className="size-4" />
              </Link>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3">
          {filteredEnrollments.map((enrollment) => (
            <div key={enrollment.id} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs">
              {enrollment.courseThumbnailUrl ? (
                <div className="h-16 w-16 shrink-0 overflow-hidden rounded-xl bg-muted">
                  <img src={enrollment.courseThumbnailUrl} alt={enrollment.courseTitle} className="h-full w-full object-cover" />
                </div>
              ) : (
                <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                  <BookOpen className="size-6 text-primary/40" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-foreground truncate">{enrollment.courseTitle}</p>
                <div className="mt-1 flex items-center gap-2">
                  {enrollment.completedAt ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                      <CheckCircle className="size-3" /> {t("myLearning.completed")}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-500">
                      <Clock className="size-3" /> {t("myLearning.inProgress")}
                    </span>
                  )}
                  <span className="text-[10px] text-muted-foreground">
                    Enrolled {new Date(enrollment.enrolledAt).toLocaleDateString()}
                  </span>
                </div>
                <div className="mt-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">{tc("progress")}</span>
                    <span className="font-semibold text-teal">{enrollment.progressPercentage}%</span>
                  </div>
                  <div
                    role="progressbar"
                    aria-valuenow={enrollment.progressPercentage}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    className="mt-1 h-1.5 w-full max-w-xs overflow-hidden rounded-full bg-muted"
                  >
                    <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${enrollment.progressPercentage}%` }} />
                  </div>
                </div>
              </div>
              <Link
                href={(() => {
                  if (enrollment.completedAt) return `/dashboard/learner/courses/${enrollment.courseId}`
                  const last = getLastAccessedLesson()
                  if (last && last.courseId === enrollment.courseId) {
                    return `/dashboard/learner/courses/${enrollment.courseId}/lessons/${last.lessonId}`
                  }
                  return `/dashboard/learner/courses/${enrollment.courseId}`
                })()}
                aria-label={enrollment.completedAt ? tc("review") : tc("continue")}
                className="shrink-0 rounded-lg bg-primary px-4 py-2 text-xs font-medium text-primary-foreground hover:bg-primary/90"
              >
                {enrollment.completedAt ? tc("review") : tc("continue")}
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
