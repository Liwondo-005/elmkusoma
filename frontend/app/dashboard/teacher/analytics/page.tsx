"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { Users, FileText, BookOpen, BarChart3, ClipboardCheck, School, Clock, TrendingUp, Loader2, AlertCircle } from "lucide-react"

async function teacherFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const institutionId = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001" : "00000000-0000-0000-0000-000000000001"
  const res = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

interface AnalyticsData {
  totalStudents: number
  totalAssignments: number
  totalLessons: number
  avgAttendancePercentage: number
  pendingGrading: number
  totalClasses: number
  upcomingDeadlines: Array<{
    id: string
    title: string
    dueDate: string
    className?: string
  }>
  recentSubmissions: Array<{
    id: string
    studentName: string
    assignmentTitle: string
    submittedAt: string
    obtainedMarks?: number
  }>
}

export default function TeacherAnalyticsPage() {
  const { user } = useAuth()
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherFetch<AnalyticsData>("/v1/teachers/me/analytics")
      setAnalytics(data)
    } catch {
      setError("Failed to load analytics data")
    } finally {
      setLoading(false)
    }
  }

  function formatDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  function formatShortDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
    })
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Analytics</h1>
          <p className="mt-1 text-sm text-muted-foreground">View your teaching analytics and insights.</p>
        </div>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      </div>
    )
  }

  const stats = [
    {
      label: "Total Students",
      value: analytics?.totalStudents ?? 0,
      icon: Users,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
    },
    {
      label: "Total Assignments",
      value: analytics?.totalAssignments ?? 0,
      icon: FileText,
      color: "text-purple-500",
      bg: "bg-purple-500/10",
    },
    {
      label: "Total Lessons",
      value: analytics?.totalLessons ?? 0,
      icon: BookOpen,
      color: "text-teal-500",
      bg: "bg-teal-500/10",
    },
    {
      label: "Avg Attendance",
      value: `${analytics?.avgAttendancePercentage ?? 0}%`,
      icon: BarChart3,
      color: "text-green-500",
      bg: "bg-green-500/10",
    },
    {
      label: "Pending Grading",
      value: analytics?.pendingGrading ?? 0,
      icon: ClipboardCheck,
      color: "text-orange-500",
      bg: "bg-orange-500/10",
    },
    {
      label: "Classes",
      value: analytics?.totalClasses ?? 0,
      icon: School,
      color: "text-indigo-500",
      bg: "bg-indigo-500/10",
    },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Analytics</h1>
        <p className="mt-1 text-sm text-muted-foreground">View your teaching analytics and insights.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((stat) => (
          <div key={stat.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className={`mb-3 inline-flex size-9 items-center justify-center rounded-xl ${stat.bg}`}>
              <stat.icon className={`size-5 ${stat.color}`} />
            </div>
            <p className="text-2xl font-extrabold text-foreground">{stat.value}</p>
            <p className="text-sm font-medium text-foreground">{stat.label}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Upcoming Deadlines</h2>
            <Clock className="size-4 text-muted-foreground" />
          </div>
          <div className="mt-4 space-y-3">
            {!analytics?.upcomingDeadlines?.length ? (
              <p className="py-4 text-center text-sm text-muted-foreground">No upcoming deadlines</p>
            ) : (
              analytics.upcomingDeadlines.slice(0, 5).map((item) => (
                <div key={item.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-orange/10">
                    <Clock className="size-4 text-orange" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>{formatShortDate(item.dueDate)}</span>
                      {item.className && (
                        <>
                          <span className="text-border">·</span>
                          <span>{item.className}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Submissions</h2>
            <TrendingUp className="size-4 text-muted-foreground" />
          </div>
          <div className="mt-4 space-y-3">
            {!analytics?.recentSubmissions?.length ? (
              <p className="py-4 text-center text-sm text-muted-foreground">No recent submissions</p>
            ) : (
              analytics.recentSubmissions.slice(0, 5).map((sub) => (
                <div key={sub.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <Users className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{sub.studentName}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span className="truncate">{sub.assignmentTitle}</span>
                      <span className="text-border">·</span>
                      <span>{formatDate(sub.submittedAt)}</span>
                    </div>
                  </div>
                  {sub.obtainedMarks !== undefined && sub.obtainedMarks !== null && (
                    <span className="shrink-0 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-400">
                      {sub.obtainedMarks}
                    </span>
                  )}
                </div>
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
