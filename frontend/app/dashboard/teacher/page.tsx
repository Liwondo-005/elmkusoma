"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import {
  type TeacherDashboard,
  type Assignment,
  type Assessment,
  learningApi,
  assessmentApi,
  teacherApi as apiTeacher,
} from "@/lib/api"
import { teacherApi, teacherFetch, type TeacherAnalytics } from "@/lib/teacher-api"
import {
  BookOpen,
  Users,
  FileText,
  PenTool,
  Video,
  ArrowRight,
  GraduationCap,
  AlertCircle,
  BarChart3,
  ChevronRight,
  AlertTriangle,
  ClipboardList,
  Calendar,
  CheckCircle2,
  Clock,
  TrendingUp,
  Zap,
  Target,
  Plus,
} from "lucide-react"

function SkeletonPulse({ className }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-muted ${className}`} />
}

function DashboardSkeleton() {
  return (
    <div className="mx-auto max-w-6xl space-y-6" aria-busy="true" aria-label="Loading dashboard">
      <div>
        <SkeletonPulse className="h-8 w-64 mb-2" />
        <SkeletonPulse className="h-4 w-48" />
      </div>

      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <SkeletonPulse className="h-5 w-5 mb-3" />
            <SkeletonPulse className="h-7 w-16 mb-1" />
            <SkeletonPulse className="h-4 w-24" />
          </div>
        ))}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <SkeletonPulse className="h-5 w-5 mb-2" />
            <SkeletonPulse className="h-8 w-12 mb-1" />
            <SkeletonPulse className="h-4 w-20" />
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <SkeletonPulse className="h-5 w-40 mb-4" />
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center gap-3 rounded-xl border border-border p-4">
              <SkeletonPulse className="h-9 w-9 rounded-lg" />
              <div className="flex-1">
                <SkeletonPulse className="h-4 w-24 mb-1" />
                <SkeletonPulse className="h-3 w-16" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function TeacherDashboardPage() {
  const { user } = useAuth()
  const firstName = user?.name?.split(" ")[0] || "Teacher"
  const [loading, setLoading] = useState(true)
  const [dashboard, setDashboard] = useState<TeacherDashboard | null>(null)
  const [analytics, setAnalytics] = useState<TeacherAnalytics | null>(null)
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [error, setError] = useState<string | null>(null)
  const [stats, setStats] = useState({
    totalStudents: 0,
    totalClasses: 0,
    totalSubjects: 0,
    pendingGrading: 0,
    todayAttendance: 0,
    attendanceRate: 0,
  })
  const [todayClasses, setTodayClasses] = useState<
    { className: string; subjectName: string; classGroupId?: string }[]
  >([])
  const [todayLiveClasses, setTodayLiveClasses] = useState<
    { title: string; scheduledAt: string; status: string }[]
  >([])
  const [recentActivity, setRecentActivity] = useState<
    { type: string; title: string; description: string; timestamp: string }[]
  >([])

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)

      const [dashboardData, analyticsData] = await Promise.allSettled([
        apiTeacher.getDashboard(),
        teacherFetch<TeacherAnalytics>("/v1/teachers/me/analytics"),
      ])

      if (dashboardData.status === "fulfilled") {
        setDashboard(dashboardData.value)
      }
      if (analyticsData.status === "fulfilled") {
        setAnalytics(analyticsData.value)
      }

      const [assignmentsData, assessmentsData] = await Promise.allSettled([
        loadAssignments(dashboardData.status === "fulfilled" ? dashboardData.value : null),
        loadAssessments(dashboardData.status === "fulfilled" ? dashboardData.value : null),
      ])

      setAssignments(assignmentsData.status === "fulfilled" ? assignmentsData.value : [])
      setAssessments(assessmentsData.status === "fulfilled" ? assessmentsData.value : [])

      try {
        const profileRes = await teacherApi.listTeachers(0, 50)
        const teacher = profileRes.content?.find((t) => t.email === user?.email)
        if (teacher) {
          const [teacherAssignments] = await Promise.all([
            teacherApi.getAssignments(teacher.id).catch(() => []),
          ])
          const classIds = [...new Set(teacherAssignments.map((a) => a.classGroupId))]
          const uniqueSubjects = [
            ...new Set(teacherAssignments.map((a) => a.subjectName).filter(Boolean)),
          ]

          let totalStudents = 0
          for (const cid of classIds.slice(0, 5)) {
            try {
              const students = await teacherApi.getStudentsByClass(cid)
              totalStudents += students.length
            } catch {
              /* skip */
            }
          }

          setStats({
            totalStudents,
            totalClasses: classIds.length,
            totalSubjects: uniqueSubjects.length,
            pendingGrading:
              dashboardData.status === "fulfilled"
                ? dashboardData.value?.pendingGrading ?? 0
                : 0,
            todayAttendance: 0,
            attendanceRate:
              analyticsData.status === "fulfilled"
                ? analyticsData.value?.averageAttendance ?? 0
                : 0,
          })

          if (
            dashboardData.status === "fulfilled" &&
            dashboardData.value?.classes
          ) {
            setTodayClasses(
              dashboardData.value.classes.map((c) => ({
                className: c.className,
                subjectName: c.subjectName,
                classGroupId: c.classGroupId,
              }))
            )
          }
          if (
            dashboardData.status === "fulfilled" &&
            dashboardData.value?.recentActivity
          ) {
            setRecentActivity(dashboardData.value.recentActivity)
          }

          try {
            const liveClasses = await teacherFetch<
              { title: string; scheduledAt: string; status: string }[]
            >("/v1/teachers/me/live-classes").catch(() => [])
            const today = new Date().toISOString().split("T")[0]
            setTodayLiveClasses(
              liveClasses.filter((lc) => lc.scheduledAt?.startsWith(today))
            )
          } catch {
            /* skip */
          }
        }
      } catch {
        /* dashboard loads with zero stats */
      }
    } catch {
      setError("Failed to load dashboard data")
    } finally {
      setLoading(false)
    }
  }

  async function loadAssignments(dash: TeacherDashboard | null): Promise<Assignment[]> {
    if (!dash?.classes?.length) return []
    const all: Assignment[] = []
    for (const cls of dash.classes.slice(0, 5)) {
      try {
        const data = await learningApi.getAssignments(cls.classGroupId)
        all.push(...data)
      } catch {
        /* skip failed class */
      }
    }
    return all.slice(0, 10)
  }

  async function loadAssessments(dash: TeacherDashboard | null): Promise<Assessment[]> {
    if (!dash?.classes?.length) return []
    const all: Assessment[] = []
    for (const cls of dash.classes.slice(0, 5)) {
      try {
        const data = await assessmentApi.getByClass(cls.classGroupId)
        all.push(...data)
      } catch {
        /* skip failed class */
      }
    }
    return all.slice(0, 10)
  }

  if (loading) {
    return <DashboardSkeleton />
  }

  const pendingGradingCount = stats.pendingGrading
  const pendingSubmissionsCount = dashboard?.pendingSubmissions ?? 0
  const upcomingDeadlines = analytics?.upcomingDeadlines ?? []
  const recentSubmissions = analytics?.recentSubmissions ?? []
  const ungradedSubmissions = recentSubmissions.filter((s) => s.graded === false)

  const commandItems = [
    pendingGradingCount > 0 && {
      label: `${pendingGradingCount} Assignment${pendingGradingCount !== 1 ? "s" : ""} to Grade`,
      icon: AlertTriangle,
      color: "text-orange",
      bg: "bg-orange/10",
      href: "/dashboard/teacher/grading",
    },
    pendingSubmissionsCount > 0 && {
      label: `${pendingSubmissionsCount} Pending Submission${pendingSubmissionsCount !== 1 ? "s" : ""}`,
      icon: Clock,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
      href: "/dashboard/teacher/assignments",
    },
    upcomingDeadlines.length > 0 && {
      label: `${upcomingDeadlines.length} Upcoming Deadline${upcomingDeadlines.length !== 1 ? "s" : ""}`,
      icon: Calendar,
      color: "text-purple-500",
      bg: "bg-purple-500/10",
      href: "/dashboard/teacher/assignments",
    },
    todayLiveClasses.length > 0 && {
      label: `${todayLiveClasses.length} Live Class${todayLiveClasses.length !== 1 ? "es" : ""} Today`,
      icon: Video,
      color: "text-green-600",
      bg: "bg-green-500/10",
      href: "/dashboard/teacher/live-classes",
    },
    stats.todayAttendance === 0 &&
      stats.totalStudents > 0 && {
        label: "Attendance Not Taken",
        icon: ClipboardList,
        color: "text-amber-600",
        bg: "bg-amber-500/10",
        href: "/dashboard/teacher/attendance",
      },
  ].filter(Boolean) as Array<{
    label: string
    icon: typeof AlertTriangle
    color: string
    bg: string
    href: string
  }>

  const statCards = [
    {
      label: "Students",
      value: stats.totalStudents || dashboard?.totalStudents || 0,
      icon: Users,
      color: "text-blue-500",
      detail:
        stats.totalClasses > 0
          ? `Across ${stats.totalClasses} class${stats.totalClasses !== 1 ? "es" : ""}`
          : undefined,
    },
    {
      label: "Classes",
      value: stats.totalClasses || dashboard?.totalClasses || 0,
      icon: BookOpen,
      color: "text-teal",
      detail:
        stats.totalSubjects > 0
          ? `${stats.totalSubjects} subject${stats.totalSubjects !== 1 ? "s" : ""}`
          : undefined,
    },
    {
      label: "Assignments",
      value: dashboard?.totalAssignments || 0,
      icon: FileText,
      color: "text-purple-500",
      note: `${pendingSubmissionsCount} pending`,
    },
    {
      label: "Pending Grading",
      value: stats.pendingGrading,
      icon: AlertTriangle,
      color: "text-orange",
      note: pendingGradingCount > 0 ? "Action needed" : "All caught up",
    },
  ]

  const formatDate = (ts?: string) => {
    if (!ts) return ""
    const d = new Date(ts)
    const now = new Date()
    const diffMs = now.getTime() - d.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    if (diffMins < 1) return "Just now"
    if (diffMins < 60) return `${diffMins}m ago`
    const diffHours = Math.floor(diffMins / 60)
    if (diffHours < 24) return `${diffHours}h ago`
    return d.toLocaleDateString()
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Welcome back, {firstName}!
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here is your teaching overview for today.
        </p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
            <button
              onClick={loadData}
              className="ml-auto text-xs font-medium underline hover:no-underline"
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {/* ── Command Center ── */}
      <section className="rounded-2xl border border-primary/20 bg-primary/5 p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Zap className="size-4 text-primary" />
          <h2 className="text-base font-semibold text-foreground">Command Center</h2>
          {commandItems.length > 0 && (
            <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
              {commandItems.length} action{commandItems.length !== 1 ? "s" : ""}
            </span>
          )}
        </div>
        {commandItems.length > 0 ? (
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {commandItems.map((item, i) => (
              <Link
                key={i}
                href={item.href}
                className="flex items-center gap-3 rounded-xl border border-border bg-background p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted/50"
              >
                <div className={`flex size-8 items-center justify-center rounded-lg ${item.bg}`}>
                  <item.icon className={`size-4 ${item.color}`} />
                </div>
                <span className="flex-1">{item.label}</span>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
              </Link>
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-border bg-background p-6 text-center">
            <CheckCircle2 className="mx-auto mb-2 size-8 text-green-500" />
            <p className="text-sm font-medium text-foreground">You&apos;re all caught up.</p>
            <p className="mt-1 text-xs text-muted-foreground">
              No urgent teaching actions right now.
            </p>
            <div className="mt-4 flex items-center justify-center gap-3">
              <Link
                href="/dashboard/teacher/schedule"
                className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
              >
                <Calendar className="size-3" /> View Schedule
              </Link>
              <Link
                href="/dashboard/teacher/classes"
                className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
              >
                <BookOpen className="size-3" /> View Classes
              </Link>
            </div>
          </div>
        )}
      </section>

      {/* ── Key Metrics ── */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((card) => (
          <div key={card.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <card.icon className={`mb-2 size-5 ${card.color}`} />
            <p className="text-2xl font-extrabold text-foreground">{card.value}</p>
            <p className="text-sm font-medium text-foreground">{card.label}</p>
            {card.note && <p className="text-xs text-muted-foreground">{card.note}</p>}
            {card.detail && <p className="text-xs text-muted-foreground">{card.detail}</p>}
          </div>
        ))}
      </div>

      {/* ── Today's Classes ── */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">Today&apos;s Classes</h2>
          <Link
            href="/dashboard/teacher/schedule"
            className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
          >
            Full Schedule <ArrowRight className="size-3" />
          </Link>
        </div>
        {todayClasses.length > 0 ? (
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {todayClasses.slice(0, 6).map((cls, i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl border border-border p-4">
                <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
                  <GraduationCap className="size-4 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{cls.className}</p>
                  <p className="text-xs text-muted-foreground">{cls.subjectName}</p>
                </div>
                {cls.classGroupId ? (
                  <Link
                    href={`/dashboard/teacher/classes/${cls.classGroupId}`}
                    className="rounded-lg bg-primary/10 px-2 py-1 text-xs font-medium text-primary hover:bg-primary/20"
                  >
                    Open
                  </Link>
                ) : (
                  <Link
                    href="/dashboard/teacher/attendance"
                    className="rounded-lg bg-primary/10 px-2 py-1 text-xs font-medium text-primary hover:bg-primary/20"
                  >
                    Take Attendance
                  </Link>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="mt-4 rounded-xl border border-border bg-background p-6 text-center">
            <Calendar className="mx-auto mb-2 size-8 text-muted-foreground/50" />
            <p className="text-sm font-medium text-foreground">
              No classes scheduled for today.
            </p>
            <Link
              href="/dashboard/teacher/schedule"
              className="mt-3 inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              <Calendar className="size-3" /> View Schedule
            </Link>
          </div>
        )}
      </section>

      {/* ── My Classes ── */}
      {dashboard?.classes && dashboard.classes.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">My Classes</h2>
            <Link
              href="/dashboard/teacher/classes"
              className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
            >
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {dashboard.classes.map((cls) => (
              <Link
                key={cls.classGroupId}
                href={`/dashboard/teacher/classes/${cls.classGroupId}`}
                className="group rounded-xl border border-border p-4 transition-colors hover:bg-muted/30"
              >
                <div className="flex items-center gap-3">
                  <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
                    <GraduationCap className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">
                      {cls.className}
                    </p>
                    <p className="text-xs text-muted-foreground">{cls.subjectName}</p>
                  </div>
                  <ArrowRight className="size-4 shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                </div>
                <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <Users className="size-3" /> {cls.enrolledStudents} students
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ── Live Learning ── */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">Live Classes</h2>
          <Link
            href="/dashboard/teacher/live-classes"
            className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
          >
            Manage <ArrowRight className="size-3" />
          </Link>
        </div>
        {todayLiveClasses.length > 0 ? (
          <div className="mt-4 space-y-3">
            {todayLiveClasses.map((lc, i) => (
              <div key={i} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-green-500/10">
                  <Video className="size-4 text-green-600" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{lc.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {new Date(lc.scheduledAt).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                </div>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${
                    lc.status === "IN_PROGRESS"
                      ? "bg-green-100 text-green-700"
                      : "bg-blue-100 text-blue-700"
                  }`}
                >
                  {lc.status === "IN_PROGRESS" ? "LIVE NOW" : "UPCOMING"}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <div className="mt-4 rounded-xl border border-border bg-background p-6 text-center">
            <Video className="mx-auto mb-2 size-8 text-muted-foreground/50" />
            <p className="text-sm font-medium text-foreground">
              No live classes scheduled for today.
            </p>
            <Link
              href="/dashboard/teacher/live-classes"
              className="mt-3 inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              <Plus className="size-3" /> Schedule Live Class
            </Link>
          </div>
        )}
      </section>

      {/* ── Recent Activity ── */}
      {recentActivity.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Recent Activity</h2>
          <div className="mt-4 space-y-3">
            {recentActivity.slice(0, 5).map((activity, i) => (
              <div key={i} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                  {activity.type === "assignment" ? (
                    <FileText className="size-4 text-primary" />
                  ) : activity.type === "assessment" ? (
                    <PenTool className="size-4 text-primary" />
                  ) : (
                    <BookOpen className="size-4 text-primary" />
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{activity.title}</p>
                  <p className="text-xs text-muted-foreground">{activity.description}</p>
                </div>
                <span className="shrink-0 text-[10px] text-muted-foreground">
                  {formatDate(activity.timestamp)}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ── Needs Grading ── */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">Needs Grading</h2>
          {ungradedSubmissions.length > 0 && (
            <Link
              href="/dashboard/teacher/grading"
              className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
            >
              Grade Now <ArrowRight className="size-3" />
            </Link>
          )}
        </div>
        {ungradedSubmissions.length > 0 ? (
          <div className="mt-4 space-y-3">
            {ungradedSubmissions.slice(0, 5).map((s, i) => (
              <div key={i} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-orange/10">
                  <AlertTriangle className="size-4 text-orange" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{s.studentName}</p>
                  <p className="text-xs text-muted-foreground">{s.assignmentTitle}</p>
                </div>
                <span className="text-xs text-muted-foreground">
                  {s.submittedAt ? formatDate(s.submittedAt) : ""}
                </span>
                <Link
                  href="/dashboard/teacher/grading"
                  className="shrink-0 rounded-lg bg-orange/10 px-2 py-1 text-xs font-medium text-orange hover:bg-orange/20"
                >
                  Grade
                </Link>
              </div>
            ))}
          </div>
        ) : (
          <div className="mt-4 rounded-xl border border-border bg-background p-6 text-center">
            <CheckCircle2 className="mx-auto mb-2 size-8 text-green-500" />
            <p className="text-sm font-medium text-foreground">You&apos;re all caught up.</p>
            <p className="mt-1 text-xs text-muted-foreground">
              No submissions require grading right now.
            </p>
            <Link
              href="/dashboard/teacher/gradebook"
              className="mt-3 inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BarChart3 className="size-3" /> Open Gradebook
            </Link>
          </div>
        )}
      </section>

      {/* ── Quick Actions + Recent Work ── */}
      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <Link
              href="/dashboard/teacher/assignments"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <div className="flex size-8 items-center justify-center rounded-lg bg-purple-500/10">
                <Plus className="size-4 text-purple-500" />
              </div>
              <span className="flex-1">Create Assignment</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/lessons"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                <Plus className="size-4 text-primary" />
              </div>
              <span className="flex-1">Create Lesson</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/assessments"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10">
                <Plus className="size-4 text-teal" />
              </div>
              <span className="flex-1">Create Assessment</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/live-classes"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <div className="flex size-8 items-center justify-center rounded-lg bg-green-500/10">
                <Video className="size-4 text-green-600" />
              </div>
              <span className="flex-1">Start Live Class</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/attendance"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <ClipboardList className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Take Attendance</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/grading"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BarChart3 className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Grade Submissions</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/schedule"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Calendar className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">View Schedule</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/analytics"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <TrendingUp className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">View Analytics</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Assignments</h2>
            <Link
              href="/dashboard/teacher/assignments"
              className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
            >
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {assignments.length === 0 ? (
              <div className="py-4 text-center">
                <FileText className="mx-auto mb-2 size-6 text-muted-foreground/50" />
                <p className="text-sm text-muted-foreground">No assignments yet</p>
                <Link
                  href="/dashboard/teacher/assignments"
                  className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                >
                  <Plus className="size-3" /> Create Assignment
                </Link>
              </div>
            ) : (
              assignments.slice(0, 5).map((a) => (
                <div key={a.id} className="flex items-center gap-3 rounded-lg border border-border p-3">
                  <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                    <FileText className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{a.title}</p>
                    <p className="text-xs text-muted-foreground">
                      Due: {a.dueDate ? new Date(a.dueDate).toLocaleDateString() : "No due date"}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </div>

      {/* ── Recent Assessments ── */}
      {assessments.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Assessments</h2>
            <Link
              href="/dashboard/teacher/assessments"
              className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
            >
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {assessments.slice(0, 5).map((a) => (
              <div key={a.id} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-teal/10">
                  <PenTool className="size-4 text-teal" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{a.title}</p>
                  <p className="text-xs text-muted-foreground">{a.totalMarks} marks</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ── Teaching Insights ── */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Target className="size-4 text-primary" />
          <h2 className="text-base font-semibold text-foreground">Teaching Insights</h2>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl border border-border p-4">
            <div className="flex items-center gap-2 mb-2">
              <TrendingUp className="size-4 text-teal" />
              <span className="text-sm font-medium text-foreground">Attendance Rate</span>
            </div>
            <p className="text-2xl font-extrabold text-foreground">
              {stats.attendanceRate > 0 ? `${Math.round(stats.attendanceRate)}%` : "—"}
            </p>
            <p className="text-xs text-muted-foreground">Average across your classes</p>
          </div>
          <div className="rounded-xl border border-border p-4">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle2 className="size-4 text-green-600" />
              <span className="text-sm font-medium text-foreground">Assignment Completion</span>
            </div>
            <p className="text-2xl font-extrabold text-foreground">
              {dashboard?.totalAssignments
                ? `${Math.round(
                    ((dashboard.totalAssignments - pendingSubmissionsCount) /
                      dashboard.totalAssignments) *
                      100
                  )}%`
                : "—"}
            </p>
            <p className="text-xs text-muted-foreground">Submissions received</p>
          </div>
          <div className="rounded-xl border border-border p-4">
            <div className="flex items-center gap-2 mb-2">
              <BarChart3 className="size-4 text-purple-500" />
              <span className="text-sm font-medium text-foreground">Grading Progress</span>
            </div>
            <p className="text-2xl font-extrabold text-foreground">
              {pendingGradingCount === 0 ? "Done" : `${pendingGradingCount} left`}
            </p>
            <p className="text-xs text-muted-foreground">
              {pendingGradingCount === 0
                ? "All submissions graded"
                : "Pending submissions"}
            </p>
          </div>
        </div>
      </section>
    </div>
  )
}
