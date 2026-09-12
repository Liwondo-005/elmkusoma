"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type DashboardData, type CourseSummary } from "@/lib/learner-api"
import { LearnerHeader, ContinueLearningCard, EmptyState, LoadingState } from "@/components/learner/shared"
import { BookOpen, Library, Video, Award, ArrowRight, Clock, Loader2, AlertCircle } from "lucide-react"

export default function LearnerDashboardPage() {
  const { user, loading: authLoading } = useAuth()
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadDashboard()
  }, [user])

  async function loadDashboard() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getDashboard()
      setDashboard(data)
    } catch {
      setError("Failed to load dashboard data")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Welcome to your learning dashboard." />

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Enrolled Courses</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{dashboard?.enrolledCourses ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Completed Courses</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{dashboard?.completedCourses ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Overall Progress</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{dashboard?.overallProgress ?? 0}%</p>
          <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
            <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${dashboard?.overallProgress ?? 0}%` }} />
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          {dashboard?.continueLearning && dashboard.continueLearning.length > 0 ? (
            <div className="space-y-3">
              <h2 className="text-base font-semibold text-foreground">Continue Learning</h2>
              {dashboard.continueLearning.map((item) => (
                <Link
                  key={item.courseId}
                  href={`/dashboard/learner/courses/${item.courseId}`}
                  className="flex items-center gap-4 rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
                >
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <Clock className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{item.courseName}</p>
                    <p className="text-xs text-muted-foreground">
                      {item.progressPercentage}% complete
                    </p>
                  </div>
                  <div className="text-right">
                    <div className="h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-primary" style={{ width: `${item.progressPercentage}%` }} />
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <ContinueLearningCard title={undefined} onResume={() => {}} />
          )}
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Quick Links</h2>
          <div className="mt-4 space-y-2">
            <Link
              href="/dashboard/learner/courses"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BookOpen className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Explore Courses</span>
              <ArrowRight className="size-3 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/learner/resources"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Library className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Resources</span>
              <ArrowRight className="size-3 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/learner/live-classes"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Video className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Live Classes</span>
              <ArrowRight className="size-3 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/learner/certificates"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Award className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Certificates</span>
              <ArrowRight className="size-3 shrink-0 text-muted-foreground" />
            </Link>
          </div>
        </div>
      </div>

      {dashboard?.recommended && dashboard.recommended.length > 0 && (
        <section>
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recommended Courses</h2>
            <Link href="/dashboard/learner/courses" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {dashboard.recommended.slice(0, 3).map((course) => (
              <Link
                key={course.id}
                href={`/dashboard/learner/courses/${course.id}`}
                className="rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
              >
                {course.thumbnailUrl ? (
                  <div className="mb-3 h-32 overflow-hidden rounded-lg bg-muted">
                    <img src={course.thumbnailUrl} alt={course.title} className="h-full w-full object-cover" />
                  </div>
                ) : (
                  <div className="mb-3 flex h-32 items-center justify-center rounded-lg bg-primary/10">
                    <BookOpen className="size-8 text-primary/40" />
                  </div>
                )}
                <h3 className="text-sm font-semibold text-foreground truncate">{course.title}</h3>
                <div className="mt-2 flex items-center gap-2">
                  {course.level && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                      {course.level}
                    </span>
                  )}
                  {course.category && (
                    <span className="text-[10px] text-muted-foreground">{course.category}</span>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {dashboard?.recentEnrollments && dashboard.recentEnrollments.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Enrollments</h2>
            <Link href="/dashboard/learner/my-learning" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {dashboard.recentEnrollments.slice(0, 5).map((enrollment) => (
              <div key={enrollment.id} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                  <BookOpen className="size-4 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{enrollment.courseName}</p>
                  <p className="text-xs text-muted-foreground">{enrollment.progressPercentage}% complete</p>
                </div>
                <Link
                  href={`/dashboard/learner/courses/${enrollment.courseId}`}
                  className="shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                >
                  Continue
                </Link>
              </div>
            ))}
          </div>
        </section>
      )}

      {(!dashboard?.enrolledCourses || dashboard.enrolledCourses === 0) &&
        (!dashboard?.recommended || dashboard.recommended.length === 0) && (
          <EmptyState
            icon={<BookOpen className="size-8" />}
            title="Your learning journey starts here"
            description="Explore courses and begin your learning adventure."
            action={
              <Link
                href="/dashboard/learner/courses"
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Explore Courses <ArrowRight className="size-4" />
              </Link>
            }
          />
        )}
    </div>
  )
}
