"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, type DashboardSummary } from "@/lib/api"
import { BarChart3 } from "lucide-react"

export default function DashboardProgressPage() {
  const { user } = useRequireAuth()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await dashboardApi.getSummary().catch(() => null)
      setSummary(data)
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

  const stats = [
    { label: "Lessons Started", value: summary?.totalLessonsStarted ?? 0 },
    { label: "Lessons Completed", value: summary?.completedLessons ?? 0 },
    { label: "Attendance Rate", value: `${summary?.monthAttendanceRate ?? 0}%` },
    { label: "Overall Average", value: `${summary?.overallAverage ?? 0}%` },
  ]

  const completionRate = summary && summary.totalLessonsStarted > 0
    ? Math.round((summary.completedLessons / summary.totalLessonsStarted) * 100)
    : 0

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Progress</h1>
        <p className="mt-1 text-sm text-muted-foreground">Track your learning journey across all courses.</p>
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
        <h2 className="text-lg font-semibold text-foreground mb-4">Overall Completion</h2>
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
          {summary?.completedLessons ?? 0} of {summary?.totalLessonsStarted ?? 0} lessons completed
        </p>
      </div>

      <div className="rounded-2xl border border-border bg-card p-12 text-center">
        <BarChart3 className="mx-auto size-12 text-muted-foreground/50" />
        <h3 className="mt-4 text-lg font-semibold text-foreground">Detailed Progress</h3>
        <p className="mt-2 text-sm text-muted-foreground">
          Course-by-course progress tracking coming soon. Complete lessons to see your progress here.
        </p>
      </div>
    </div>
  )
}
