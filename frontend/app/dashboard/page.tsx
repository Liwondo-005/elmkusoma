"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, type DashboardSummary, type ContinueLearningItem, type RecentActivity } from "@/lib/api"
import { BookOpen, Clock, CheckCircle, TrendingUp, Users, Award, ArrowRight, BarChart3, Video, FileText } from "lucide-react"

export default function DashboardPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [continueItems, setContinueItems] = useState<ContinueLearningItem[]>([])
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadDashboard()
  }, [user])

  async function loadDashboard() {
    try {
      setLoading(true)
      const [summaryData, continueData, activityData] = await Promise.all([
        dashboardApi.getSummary().catch(() => null),
        dashboardApi.getContinueLearning().catch(() => []),
        dashboardApi.getRecentActivity().catch(() => []),
      ])
      setSummary(summaryData)
      setContinueItems(continueData)
      setActivities(activityData)
    } catch {
      // Dashboard data unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const stats = [
    { label: "Enrolled Courses", value: summary?.activeEnrollments ?? 0, icon: BookOpen, color: "bg-blue-500/10 text-blue-600" },
    { label: "Lessons Completed", value: summary?.completedLessons ?? 0, icon: CheckCircle, color: "bg-green-500/10 text-green-600" },
    { label: "Attendance Rate", value: `${summary?.monthAttendanceRate ?? 0}%`, icon: Users, color: "bg-purple-500/10 text-purple-600" },
    { label: "Overall Average", value: `${summary?.overallAverage ?? 0}%`, icon: TrendingUp, color: "bg-orange-500/10 text-orange-600" },
  ]

  const quickActions = [
    { label: "My Courses", href: "/dashboard/courses", icon: BookOpen, color: "bg-teal text-white" },
    { label: "Results", href: "/dashboard/results", icon: Award, color: "bg-blue-600 text-white" },
    { label: "Attendance", href: "/dashboard/attendance", icon: BarChart3, color: "bg-purple-600 text-white" },
    { label: "Live Classes", href: "/dashboard/live-classes", icon: Video, color: "bg-red-500 text-white" },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Welcome back, {user?.name?.split(" ")[0] || "Student"}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here&apos;s what&apos;s happening with your learning today.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between">
              <div className={`flex size-10 items-center justify-center rounded-xl ${s.color}`}>
                <s.icon className="size-5" />
              </div>
            </div>
            <p className="mt-3 text-2xl font-extrabold text-foreground">{s.value}</p>
            <p className="mt-1 text-sm text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {quickActions.map((action) => (
          <Link
            key={action.href}
            href={action.href}
            className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
          >
            <div className={`flex size-10 items-center justify-center rounded-xl ${action.color}`}>
              <action.icon className="size-5" />
            </div>
            <span className="text-sm font-semibold text-foreground">{action.label}</span>
            <ArrowRight className="ml-auto size-4 text-muted-foreground" />
          </Link>
        ))}
      </div>

      {continueItems.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-foreground">Continue Learning</h2>
            <Link href="/dashboard/lessons" className="text-sm text-primary hover:underline">
              View all
            </Link>
          </div>
          <div className="space-y-3">
            {continueItems.map((item) => (
              <Link
                key={item.lessonId}
                href={`/dashboard/lessons/${item.lessonId}`}
                className="flex items-center gap-4 rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                  <Clock className="size-5 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">Lesson {item.lessonId.slice(0, 8)}</p>
                  <p className="text-xs text-muted-foreground">
                    {item.startedAt ? new Date(item.startedAt).toLocaleDateString() : "Recently started"}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-primary">{Math.round(item.completionPercentage)}%</p>
                  <div className="mt-1 h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary"
                      style={{ width: `${item.completionPercentage}%` }}
                    />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {activities.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Recent Activity</h2>
          <div className="space-y-3">
            {activities.map((activity, i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className={`flex size-8 items-center justify-center rounded-lg ${
                  activity.type === "lesson_completed" ? "bg-green-500/10" : "bg-blue-500/10"
                }`}>
                  {activity.type === "lesson_completed" ? (
                    <CheckCircle className="size-4 text-green-600" />
                  ) : (
                    <Clock className="size-4 text-blue-600" />
                  )}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-foreground">
                    {activity.type === "lesson_completed" ? "Completed a lesson" : "Attendance recorded"}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {activity.completedAt
                      ? new Date(activity.completedAt).toLocaleDateString()
                      : activity.date
                        ? new Date(activity.date).toLocaleDateString()
                        : ""}
                  </p>
                </div>
                {activity.status && (
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                    activity.status === "PRESENT" ? "bg-green-100 text-green-700" :
                    activity.status === "ABSENT" ? "bg-red-100 text-red-700" :
                    "bg-yellow-100 text-yellow-700"
                  }`}>
                    {activity.status}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {continueItems.length === 0 && activities.length === 0 && (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Getting Started</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Your learning journey begins here. Enroll in courses and start learning.
          </p>
          <Link
            href="/dashboard/courses"
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
          >
            Browse Courses <ArrowRight className="size-4" />
          </Link>
        </div>
      )}
    </div>
  )
}
