"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherDashboard, type Assignment, type Assessment, learningApi, assessmentApi } from "@/lib/api"
import { BookOpen, Users, FileText, PenTool, Video, Clock, ArrowRight, TrendingUp, GraduationCap, Calendar, AlertCircle, ClipboardCheck, BarChart3 } from "lucide-react"

export default function TeacherDashboardPage() {
  const { user } = useAuth()
  const [dashboard, setDashboard] = useState<TeacherDashboard | null>(null)
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
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

      const [dashboardData, assignmentsData, assessmentsData] = await Promise.allSettled([
        teacherApi.getDashboard(),
        loadAssignments(),
        loadAssessments(),
      ])

      if (dashboardData.status === "fulfilled") {
        setDashboard(dashboardData.value)
      }

      setAssignments(assignmentsData.status === "fulfilled" ? assignmentsData.value : [])
      setAssessments(assessmentsData.status === "fulfilled" ? assessmentsData.value : [])
    } catch {
      setError("Failed to load dashboard data")
    } finally {
      setLoading(false)
    }
  }

  async function loadAssignments(): Promise<Assignment[]> {
    if (!dashboard?.classes?.length) return []
    const all: Assignment[] = []
    for (const cls of dashboard.classes.slice(0, 5)) {
      try {
        const data = await learningApi.getAssignments(cls.classGroupId)
        all.push(...data)
      } catch {
        // skip failed class
      }
    }
    return all.slice(0, 10)
  }

  async function loadAssessments(): Promise<Assessment[]> {
    if (!dashboard?.classes?.length) return []
    const all: Assessment[] = []
    for (const cls of dashboard.classes.slice(0, 5)) {
      try {
        const data = await assessmentApi.getByClass(cls.classGroupId)
        all.push(...data)
      } catch {
        // skip failed class
      }
    }
    return all.slice(0, 10)
  }

  const firstName = user?.name?.split(" ")[0] || "Teacher"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Teacher Dashboard
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Welcome back, {firstName}. Here&apos;s your teaching overview.
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

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={Users}
          label="My Students"
          value={loading ? "..." : String(dashboard?.totalStudents ?? 0)}
          note="Across all classes"
        />
        <StatCard
          icon={BookOpen}
          label="My Classes"
          value={loading ? "..." : String(dashboard?.totalClasses ?? 0)}
          note="Class groups"
        />
        <StatCard
          icon={FileText}
          label="Assignments"
          value={loading ? "..." : String(dashboard?.totalAssignments ?? 0)}
          note={`${dashboard?.pendingSubmissions ?? 0} pending`}
        />
        <StatCard
          icon={PenTool}
          label="Assessments"
          value={loading ? "..." : String(dashboard?.totalAssessments ?? 0)}
          note="Total created"
        />
      </div>

      {dashboard?.classes && dashboard.classes.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">My Classes</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {dashboard.classes.map((cls) => (
              <div key={cls.classGroupId} className="rounded-xl border border-border p-4 hover:bg-muted/30 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
                    <GraduationCap className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{cls.className}</p>
                    <p className="text-xs text-muted-foreground">{cls.subjectName}</p>
                  </div>
                </div>
                <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <Users className="size-3" /> {cls.enrolledStudents} students
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Assignments</h2>
            <Link href="/dashboard/teacher/assignments" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {loading ? (
              <div className="flex justify-center py-8">
                <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
              </div>
            ) : assignments.length === 0 ? (
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

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Recent Assessments</h2>
            <Link href="/dashboard/teacher/assessments" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {loading ? (
              <div className="flex justify-center py-8">
                <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
              </div>
            ) : assessments.length === 0 ? (
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
      </div>

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <QuickAction href="/dashboard/teacher/courses" icon={BookOpen} label="Manage Courses" />
          <QuickAction href="/dashboard/teacher/students" icon={Users} label="View Students" />
          <QuickAction href="/dashboard/teacher/assignments" icon={FileText} label="Assignments" />
          <QuickAction href="/dashboard/teacher/assessments" icon={PenTool} label="Assessments" />
          <QuickAction href="/dashboard/teacher/attendance" icon={ClipboardCheck} label="Take Attendance" />
          <QuickAction href="/dashboard/teacher/gradebook" icon={BarChart3} label="Gradebook" />
          <QuickAction href="/dashboard/teacher/live-classes" icon={Video} label="Live Classes" />
          <QuickAction href="/dashboard/teacher/schedule" icon={Calendar} label="My Schedule" />
        </div>
      </section>
    </div>
  )
}

function StatCard({ icon: Icon, label, value, note }: { icon: typeof BookOpen; label: string; value: string; note: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <Icon className="mb-2 size-5 text-muted-foreground" />
      <p className="text-2xl font-extrabold text-foreground">{value}</p>
      <p className="text-sm font-medium text-foreground">{label}</p>
      <p className="text-xs text-muted-foreground">{note}</p>
    </div>
  )
}

function QuickAction({ href, icon: Icon, label }: { href: string; icon: typeof BookOpen; label: string }) {
  return (
    <Link
      href={href}
      className="flex items-center gap-3 rounded-xl border border-border bg-background p-4 transition-all hover:border-primary/30 hover:shadow-sm"
    >
      <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
        <Icon className="size-4 text-primary" />
      </div>
      <span className="text-sm font-medium text-foreground">{label}</span>
      <ArrowRight className="ml-auto size-4 text-muted-foreground" />
    </Link>
  )
}
