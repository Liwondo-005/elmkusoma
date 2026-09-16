"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { type TeacherDashboard, type Assignment, type Assessment, learningApi, assessmentApi, teacherApi as apiTeacher } from "@/lib/api"
import { teacherApi, teacherFetch, type TeacherAnalytics } from "@/lib/teacher-api"
import { BookOpen, Users, FileText, PenTool, Video, ArrowRight, GraduationCap, AlertCircle, BarChart3, ChevronRight, Loader2, AlertTriangle, ClipboardList, Calendar, CheckCircle2, Clock, TrendingUp, Zap, Target } from "lucide-react"

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
  const [todayClasses, setTodayClasses] = useState<{ className: string; subjectName: string }[]>([])
  const [todayLiveClasses, setTodayLiveClasses] = useState<{ title: string; scheduledAt: string; status: string }[]>([])
  const [recentActivity, setRecentActivity] = useState<{ type: string; title: string; description: string; timestamp: string }[]>([])

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
          const uniqueSubjects = [...new Set(teacherAssignments.map((a) => a.subjectName).filter(Boolean))]

          let totalStudents = 0
          for (const cid of classIds.slice(0, 5)) {
            try {
              const students = await teacherApi.getStudentsByClass(cid)
              totalStudents += students.length
            } catch { /* skip */ }
          }

          setStats({
            totalStudents,
            totalClasses: classIds.length,
            totalSubjects: uniqueSubjects.length,
            pendingGrading: dashboardData.status === "fulfilled" ? (dashboardData.value?.pendingGrading ?? 0) : 0,
            todayAttendance: 0,
            attendanceRate: analyticsData.status === "fulfilled" ? (analyticsData.value?.averageAttendance ?? 0) : 0,
          })

          if (dashboardData.status === "fulfilled" && dashboardData.value?.classes) {
            setTodayClasses(dashboardData.value.classes.map((c) => ({ className: c.className, subjectName: c.subjectName })))
          }
          if (dashboardData.status === "fulfilled" && dashboardData.value?.recentActivity) {
            setRecentActivity(dashboardData.value.recentActivity)
          }

          try {
            const liveClasses = await teacherFetch<{ title: string; scheduledAt: string; status: string }[]>("/v1/teachers/me/live-classes").catch(() => [])
            const today = new Date().toISOString().split("T")[0]
            setTodayLiveClasses(liveClasses.filter((lc) => lc.scheduledAt?.startsWith(today)))
          } catch { /* skip */ }
        }
      } catch { /* dashboard loads with zero stats */ }
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
        // skip failed class
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
        // skip failed class
      }
    }
    return all.slice(0, 10)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
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
      href: "/dashboard/teacher/assignments",
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
    stats.todayAttendance === 0 && stats.totalStudents > 0 && {
      label: "Attendance Not Taken",
      icon: ClipboardList,
      color: "text-amber-600",
      bg: "bg-amber-500/10",
      href: "/dashboard/teacher/attendance",
    },
  ].filter(Boolean)

  const statCards = [
    { label: "My Students", value: stats.totalStudents || dashboard?.totalStudents || 0, icon: Users, color: "text-blue-500", detail: stats.totalClasses > 0 ? `Across ${stats.totalClasses} class${stats.totalClasses !== 1 ? "es" : ""}` : undefined },
    { label: "My Classes", value: stats.totalClasses || dashboard?.totalClasses || 0, icon: BookOpen, color: "text-teal", detail: stats.totalSubjects > 0 ? `${stats.totalSubjects} subject${stats.totalSubjects !== 1 ? "s" : ""}` : undefined },
    { label: "Assignments", value: dashboard?.totalAssignments || 0, icon: FileText, color: "text-purple-500", note: `${pendingSubmissionsCount} pending` },
    { label: "Pending Grading", value: stats.pendingGrading, icon: AlertTriangle, color: "text-orange", note: pendingGradingCount > 0 ? "Action needed" : "All caught up" },
  ]

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
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {commandItems.length > 0 && (
        <section className="rounded-2xl border border-primary/20 bg-primary/5 p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-4">
            <Zap className="size-4 text-primary" />
            <h2 className="text-base font-semibold text-foreground">Command Center</h2>
            <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
              {commandItems.length} action{commandItems.length !== 1 ? "s" : ""} today
            </span>
          </div>
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
        </section>
      )}

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

      {todayClasses.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Today&apos;s Schedule</h2>
            <Link href="/dashboard/teacher/schedule" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Full Schedule <ArrowRight className="size-3" />
            </Link>
          </div>
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
                <Link href="/dashboard/teacher/attendance" className="rounded-lg bg-primary/10 px-2 py-1 text-xs font-medium text-primary hover:bg-primary/20">
                  Take Attendance
                </Link>
              </div>
            ))}
          </div>
        </section>
      )}

      {dashboard?.classes && dashboard.classes.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">My Classes</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {dashboard.classes.map((cls) => (
              <Link key={cls.classGroupId} href={`/dashboard/teacher/classes/${cls.classGroupId}`} className="group rounded-xl border border-border p-4 transition-colors hover:bg-muted/30">
                <div className="flex items-center gap-3">
                  <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
                    <GraduationCap className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{cls.className}</p>
                    <p className="text-xs text-muted-foreground">{cls.subjectName}</p>
                  </div>
                  <ArrowRight className="size-4 shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                </div>
                <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><Users className="size-3" /> {cls.enrolledStudents} students</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {todayLiveClasses.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Today&apos;s Live Classes</h2>
          <div className="mt-4 space-y-3">
            {todayLiveClasses.map((lc, i) => (
              <div key={i} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-green-500/10"><Video className="size-4 text-green-600" /></div>
                <div className="min-w-0 flex-1"><p className="text-sm font-medium text-foreground">{lc.title}</p><p className="text-xs text-muted-foreground">{new Date(lc.scheduledAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</p></div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${lc.status === "IN_PROGRESS" ? "bg-green-100 text-green-700" : "bg-blue-100 text-blue-700"}`}>{lc.status}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      {recentActivity.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Recent Activity</h2>
          <div className="mt-4 space-y-3">
            {recentActivity.slice(0, 5).map((activity, i) => (
              <div key={i} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                  {activity.type === "assignment" ? <FileText className="size-4 text-primary" /> : <BookOpen className="size-4 text-primary" />}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{activity.title}</p>
                  <p className="text-xs text-muted-foreground">{activity.description}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <Link
              href="/dashboard/teacher/classes"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BookOpen className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">My Classes</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/lessons"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BookOpen className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Create Lesson</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/attendance"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <ClipboardList className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Mark Attendance</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/assignments"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <FileText className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Assignments</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/gradebook"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BarChart3 className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Grading</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/live-classes"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Video className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Live Classes</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/schedule"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Calendar className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">My Schedule</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/teacher/assessments"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <PenTool className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Create Assessment</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Assignments</h2>
            <Link href="/dashboard/teacher/assignments" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {assignments.length === 0 ? (
              <p className="py-4 text-center text-sm text-muted-foreground">No assignments yet</p>
            ) : (
              assignments.slice(0, 5).map((a) => (
                <div key={a.id} className="flex items-center gap-3 rounded-lg border border-border p-3">
                  <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                    <FileText className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{a.title}</p>
                    <p className="text-xs text-muted-foreground">Due: {a.dueDate ? new Date(a.dueDate).toLocaleDateString() : "No due date"}</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </div>

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">Recent Assessments</h2>
          <Link href="/dashboard/teacher/assessments" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
            View All <ArrowRight className="size-3" />
          </Link>
        </div>
        <div className="mt-4 space-y-3">
          {assessments.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No assessments yet</p>
          ) : (
            assessments.slice(0, 5).map((a) => (
              <div key={a.id} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10">
                  <PenTool className="size-4 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{a.title}</p>
                  <p className="text-xs text-muted-foreground">{a.totalMarks} marks</p>
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      {ungradedSubmissions.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Needs Grading</h2>
            <Link href="/dashboard/teacher/assignments" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Grade Now <ArrowRight className="size-3" />
            </Link>
          </div>
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
                  {s.submittedAt ? new Date(s.submittedAt).toLocaleDateString() : ""}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

      {stats.attendanceRate > 0 && (
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
              <p className="text-2xl font-extrabold text-foreground">{Math.round(stats.attendanceRate)}%</p>
              <p className="text-xs text-muted-foreground">Average across your classes</p>
            </div>
            <div className="rounded-xl border border-border p-4">
              <div className="flex items-center gap-2 mb-2">
                <CheckCircle2 className="size-4 text-green-600" />
                <span className="text-sm font-medium text-foreground">Assignment Completion</span>
              </div>
              <p className="text-2xl font-extrabold text-foreground">
                {dashboard?.totalAssignments ? Math.round(((dashboard.totalAssignments - pendingSubmissionsCount) / dashboard.totalAssignments) * 100) : 0}%
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
              <p className="text-xs text-muted-foreground">Pending submissions</p>
            </div>
          </div>
        </section>
      )}
    </div>
  )
}
