"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { type TeacherDashboard, type Assignment, type Assessment, learningApi, assessmentApi, teacherApi as apiTeacher } from "@/lib/api"
import { teacherApi } from "@/lib/teacher-api"
import { BookOpen, Users, FileText, PenTool, Video, Clock, ArrowRight, TrendingUp, GraduationCap, Calendar, AlertCircle, ClipboardCheck, BarChart3, ChevronRight, Loader2, AlertTriangle, CheckCircle, ClipboardList } from "lucide-react"

export default function TeacherDashboardPage() {
  const { user } = useAuth()
  const firstName = user?.name?.split(" ")[0] || "Teacher"
  const [loading, setLoading] = useState(true)
  const [dashboard, setDashboard] = useState<TeacherDashboard | null>(null)
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

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)

      const [dashboardData, assignmentsData, assessmentsData] = await Promise.allSettled([
        apiTeacher.getDashboard(),
        loadAssignments(),
        loadAssessments(),
      ])

      if (dashboardData.status === "fulfilled") {
        setDashboard(dashboardData.value)
      }

      setAssignments(assignmentsData.status === "fulfilled" ? assignmentsData.value : [])
      setAssessments(assessmentsData.status === "fulfilled" ? assessmentsData.value : [])

      try {
        const institutionId = localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001"
        const profileRes = await teacherApi.listTeachers(0, 50)
        const teacher = profileRes.content?.find((t) => t.email === user?.email)
        if (teacher) {
          const [teacherAssignments, qualifications] = await Promise.all([
            teacherApi.getAssignments(teacher.id).catch(() => []),
            teacherApi.getQualifications(teacher.id).catch(() => []),
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
            pendingGrading: 0,
            todayAttendance: 0,
            attendanceRate: 0,
          })
        }
      } catch { /* dashboard loads with zero stats */ }
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

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const statCards = [
    { label: "My Students", value: stats.totalStudents || dashboard?.totalStudents || 0, icon: Users, color: "text-blue-500" },
    { label: "My Classes", value: stats.totalClasses || dashboard?.totalClasses || 0, icon: BookOpen, color: "text-teal" },
    { label: "Assignments", value: dashboard?.totalAssignments || 0, icon: FileText, color: "text-purple-500", note: `${dashboard?.pendingSubmissions ?? 0} pending` },
    { label: "Pending Grading", value: stats.pendingGrading, icon: AlertTriangle, color: "text-orange" },
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

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((card) => (
          <div key={card.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <card.icon className={`mb-2 size-5 ${card.color}`} />
            <p className="text-2xl font-extrabold text-foreground">{card.value}</p>
            <p className="text-sm font-medium text-foreground">{card.label}</p>
            {card.note && <p className="text-xs text-muted-foreground">{card.note}</p>}
          </div>
        ))}
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

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <Link
              href="/dashboard/teacher/courses"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BookOpen className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">My Classes</span>
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
    </div>
  )
}
