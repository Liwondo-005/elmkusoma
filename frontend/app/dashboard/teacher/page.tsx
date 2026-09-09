"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { teacherService } from "@/lib/services/teacher"
import { learningApi, assessmentApi, type Assignment, type Assessment } from "@/lib/api"
import { BookOpen, Users, FileText, PenTool, Video, Clock, ArrowRight, TrendingUp, GraduationCap, Calendar } from "lucide-react"

interface TeacherStats {
  totalStudents: number
  totalCourses: number
  pendingSubmissions: number
  upcomingClasses: number
}

export default function TeacherDashboardPage() {
  const { user } = useAuth()
  const [stats, setStats] = useState<TeacherStats>({ totalStudents: 0, totalCourses: 0, pendingSubmissions: 0, upcomingClasses: 0 })
  const [recentAssignments, setRecentAssignments] = useState<Assignment[]>([])
  const [recentAssessments, setRecentAssessments] = useState<Assessment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [assignmentsData, assessmentsData] = await Promise.allSettled([
        learningApi.getAssignments(user?.classGroupId || ""),
        assessmentApi.getByClass(user?.classGroupId || ""),
      ])
      setRecentAssignments(assignmentsData.status === "fulfilled" ? assignmentsData.value.slice(0, 5) : [])
      setRecentAssessments(assessmentsData.status === "fulfilled" ? assessmentsData.value.slice(0, 5) : [])
      setStats({
        totalStudents: 0,
        totalCourses: 0,
        pendingSubmissions: assignmentsData.status === "fulfilled" ? assignmentsData.value.length : 0,
        upcomingClasses: assessmentsData.status === "fulfilled" ? assessmentsData.value.length : 0,
      })
    } catch {
      // empty state
    } finally {
      setLoading(false)
    }
  }

  const firstName = user?.name?.split(" ")[0] || "Teacher"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Teacher Dashboard
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Welcome back, {firstName}. Here&apos;s an overview of your teaching activity.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={Users} label="Total Students" value={String(stats.totalStudents)} note="Enrolled" />
        <StatCard icon={BookOpen} label="My Courses" value={String(stats.totalCourses)} note="Active" />
        <StatCard icon={FileText} label="Pending Submissions" value={String(stats.pendingSubmissions)} note="To grade" />
        <StatCard icon={Calendar} label="Upcoming Classes" value={String(stats.upcomingClasses)} note="Scheduled" />
      </div>

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
            ) : recentAssignments.length === 0 ? (
              <p className="py-4 text-center text-sm text-muted-foreground">No assignments yet</p>
            ) : (
              recentAssignments.map((a) => (
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
            ) : recentAssessments.length === 0 ? (
              <p className="py-4 text-center text-sm text-muted-foreground">No assessments yet</p>
            ) : (
              recentAssessments.map((a) => (
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
          <QuickAction href="/dashboard/teacher/assignments" icon={FileText} label="Create Assignment" />
          <QuickAction href="/dashboard/teacher/assessments" icon={PenTool} label="Create Assessment" />
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
