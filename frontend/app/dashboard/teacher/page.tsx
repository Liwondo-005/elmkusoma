"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Users, BookOpen, FileText, ClipboardList, BarChart3, Clock, ChevronRight, Loader2, AlertTriangle, CheckCircle } from "lucide-react"
import { useAuth } from "@/lib/auth"

export default function TeacherDashboardPage() {
  const { user } = useAuth()
  const firstName = user?.name?.split(" ")[0] || "Teacher"
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState({
    totalStudents: 0,
    totalClasses: 0,
    totalSubjects: 0,
    pendingGrading: 0,
    todayAttendance: 0,
    attendanceRate: 0,
  })

  useEffect(() => {
    async function load() {
      try {
        const { teacherApi } = await import("@/lib/teacher-api")
        const institutionId = localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001"
        const profileRes = await teacherApi.listTeachers(0, 50)
        const teacher = profileRes.content?.find((t) => t.email === user?.email)
        if (teacher) {
          const [assignments, qualifications] = await Promise.all([
            teacherApi.getAssignments(teacher.id).catch(() => []),
            teacherApi.getQualifications(teacher.id).catch(() => []),
          ])
          const classIds = [...new Set(assignments.map((a) => a.classGroupId))]
          const uniqueSubjects = [...new Set(assignments.map((a) => a.subjectName).filter(Boolean))]

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
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const statCards = [
    { label: "My Students", value: stats.totalStudents, icon: Users, color: "text-blue-500" },
    { label: "My Classes", value: stats.totalClasses, icon: BookOpen, color: "text-teal" },
    { label: "Subjects", value: stats.totalSubjects, icon: FileText, color: "text-purple-500" },
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

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((card) => (
          <div key={card.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <card.icon className={`mb-2 size-5 ${card.color}`} />
            <p className="text-2xl font-extrabold text-foreground">{card.value}</p>
            <p className="text-sm font-medium text-foreground">{card.label}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <Link
              href="/dashboard/teacher/classes"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Users className="size-4 shrink-0 text-muted-foreground" />
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
              href="/dashboard/teacher/grading"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BarChart3 className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">Grading</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Schedule</h2>
          <div className="mt-4 flex flex-col items-center py-6 text-center">
            <Clock className="mb-3 size-8 text-muted-foreground" />
            <p className="text-sm font-medium text-foreground">No classes scheduled</p>
            <p className="mt-1 text-xs text-muted-foreground">Your schedule will appear here.</p>
          </div>
        </section>
      </div>

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">Recent Activity</h2>
        </div>
        <div className="mt-4 flex flex-col items-center py-6 text-center">
          <CheckCircle className="mb-3 size-8 text-teal" />
          <p className="text-sm font-medium text-foreground">All caught up!</p>
          <p className="mt-1 text-xs text-muted-foreground">No pending items.</p>
        </div>
      </section>
    </div>
  )
}
