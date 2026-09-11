"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { BookOpen, FileText, Loader2, PenTool, BarChart3, ArrowRight } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

interface DashboardStats {
  enrolledCourses: number
  liveClasses: number
  totalLessons: number
  certificates: number
  pendingAssignments: number
  assessments: number
  completedLessons: number
}

export default function DashboardPage() {
  const { user, loading } = useAuth()
  const router = useRouter()
  const firstName = user?.name?.split(" ")[0] || "Student"
  const [stats, setStats] = useState<DashboardStats>({
    enrolledCourses: 0,
    liveClasses: 0,
    totalLessons: 0,
    certificates: 0,
    pendingAssignments: 0,
    assessments: 0,
    completedLessons: 0,
  })
  const [recentLessons, setRecentLessons] = useState<{ title: string; subject: string; progress: number }[]>([])
  const [loadingData, setLoadingData] = useState(true)

  useEffect(() => {
    if (!loading) {
      if (user?.role === "Parent") router.replace("/dashboard/parent")
      else if (user?.role === "Teacher") router.replace("/dashboard/teacher")
      else if (user?.role === "Admin" || user?.role === "Institution Admin") router.replace("/dashboard/admin")
    }
  }, [user, loading, router])

  useEffect(() => {
    if (loading || !user || user.role !== "Student") return
    async function load() {
      try {
        const { studentApi, learningApi, assessmentApi, certificateApi } = await import("@/lib/api")
        const institutionId = localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001"

        const students = await studentApi.getStudents(institutionId).catch(() => [])
        const student = students.find((s: any) => s.userId === user?.id || s.email === user?.email)

        const classId = user?.classGroupId || student?.classGroupId || ""

        const [assignments, assessments, certificates, lessons] = await Promise.all([
          classId ? learningApi.getAssignments(classId).catch(() => []) : Promise.resolve([]),
          classId ? assessmentApi.getByClass(classId).catch(() => []) : Promise.resolve([]),
          certificateApi.list().catch(() => []),
          classId ? learningApi.getLessonsByClass(classId).catch(() => []) : Promise.resolve([]),
        ])

        setStats({
          enrolledCourses: classId ? 1 : 0,
          liveClasses: 0,
          totalLessons: lessons.length,
          certificates: Array.isArray(certificates) ? certificates.length : 0,
          pendingAssignments: Array.isArray(assignments) ? assignments.filter((a: any) => a.status === "ACTIVE" || a.status === "PENDING").length : 0,
          assessments: Array.isArray(assessments) ? assessments.length : 0,
          completedLessons: 0,
        })

        setRecentLessons(
          (Array.isArray(lessons) ? lessons.slice(0, 3) : []).map((l: any) => ({
            title: l.title || "Untitled Lesson",
            subject: l.subjectName || "",
            progress: l.completionPercentage || 0,
          }))
        )
      } catch { /* dashboard loads with zero stats */ }
      finally { setLoadingData(false) }
    }
    load()
  }, [user, loading])

  if (user?.role === "Parent" || user?.role === "Teacher" || user?.role === "Admin" || user?.role === "Institution Admin") {
    return null
  }

  const statCards = [
    { label: "My Lessons", value: String(stats.totalLessons), note: "Available", icon: BookOpen },
    { label: "Assignments", value: String(stats.pendingAssignments), note: "Pending", icon: FileText },
    { label: "Assessments", value: String(stats.assessments), note: "Available", icon: PenTool },
    { label: "Certificates", value: String(stats.certificates), note: "Earned", icon: Award },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Welcome back, {firstName}!</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Keep up the great work and continue your learning journey.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <s.icon className="mb-2 size-5 text-muted-foreground" />
            <p className="text-2xl font-extrabold text-foreground">{loadingData ? "—" : s.value}</p>
            <p className="text-sm font-medium text-foreground">{s.label}</p>
            <p className="text-xs text-muted-foreground">{s.note}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Continue Learning</h2>
            <Link href="/dashboard/lessons" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
            </Link>
          </div>
          {loadingData ? (
            <div className="flex justify-center py-8"><Loader2 className="size-5 animate-spin text-muted-foreground" /></div>
          ) : recentLessons.length === 0 ? (
            <div className="py-8 text-center">
              <BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No lessons yet</p>
              <p className="mt-1 text-xs text-muted-foreground">Lessons from your classes will appear here.</p>
            </div>
          ) : (
            <div className="mt-4 space-y-4">
              {recentLessons.map((lesson) => (
                <div key={lesson.title} className="flex items-center gap-4">
                  <div className="size-14 shrink-0 rounded-xl bg-primary/10 flex items-center justify-center">
                    <BookOpen className="size-6 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-3">
                      <p className="truncate text-sm font-medium text-foreground">{lesson.title}</p>
                      <span className="text-xs font-semibold text-teal">{lesson.progress}%</span>
                    </div>
                    {lesson.subject && <p className="text-xs text-muted-foreground">{lesson.subject}</p>}
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-teal" style={{ width: `${lesson.progress}%` }} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Quick Links</h2>
          <div className="mt-4 space-y-2">
            {[
              { label: "Lessons", href: "/dashboard/lessons", icon: BookOpen },
              { label: "Assignments", href: "/dashboard/assignments", icon: FileText },
              { label: "Assessments", href: "/dashboard/assessments", icon: PenTool },
              { label: "Certificates", href: "/dashboard/certificates", icon: Award },
              { label: "Progress", href: "/dashboard/progress", icon: BarChart3 },
            ].map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
              >
                <item.icon className="size-4 shrink-0 text-muted-foreground" />
                <span className="flex-1">{item.label}</span>
                <ArrowRight className="size-3 shrink-0 text-muted-foreground" />
              </Link>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
