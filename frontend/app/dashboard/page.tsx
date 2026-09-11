"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, type DashboardSummary, type ContinueLearningItem, type RecentActivity } from "@/lib/api"
import { BookOpen, FileText, Loader2, PenTool, BarChart3, ArrowRight, Award, Users, TrendingUp, Clock, CheckCircle, Video } from "lucide-react"
import { cn } from "@/lib/utils"

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
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [continueItems, setContinueItems] = useState<ContinueLearningItem[]>([])
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [loading, setLoading] = useState(true)
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
    if (!authLoading && user?.role === "Parent") {
      router.replace("/dashboard/parent")
    } else if (!authLoading && user?.role === "Teacher") {
      router.replace("/dashboard/teacher")
    } else if (!authLoading && (user?.role === "Admin" || user?.role === "Institution Admin")) {
      router.replace("/dashboard/admin")
    }
  }, [user, authLoading, router])

  useEffect(() => {
    if (!user || user.role === "Parent" || user.role === "Teacher" || user.role === "Admin" || user.role === "Institution Admin") return
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

      try {
        const { studentApi, learningApi, assessmentApi, certificateApi, academicApi } = await import("@/lib/api")
        const institutionId = localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001"

        const students = await studentApi.getStudents(institutionId).catch(() => [])
        const student = students.find((s: any) => s.userId === user?.id || s.email === user?.email)

        if (student?.classGroupId) {
          try {
            const classGroup = await academicApi.getClassGroup(student.classGroupId)
            if (classGroup?.gradeId) {
              const grade = await academicApi.getGrade(classGroup.gradeId)
              if (grade?.educationLevel) {
                localStorage.setItem("elmkusoma_education_level", grade.educationLevel)
              }
            }
          } catch { /* education level stays default */ }
        }

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
    } catch {
      // Dashboard data unavailable
    } finally {
      setLoading(false)
      setLoadingData(false)
    }
  }

  if (authLoading || loading || user?.role === "Parent" || user?.role === "Teacher" || user?.role === "Admin" || user?.role === "Institution Admin") {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const statCards = [
    { label: "Enrolled Courses", value: String(summary?.activeEnrollments ?? stats.enrolledCourses), icon: BookOpen, color: "bg-blue-500/10 text-blue-600", note: "Active" },
    { label: "Lessons Completed", value: String(summary?.completedLessons ?? stats.completedLessons), icon: CheckCircle, color: "bg-green-500/10 text-green-600", note: "Total" },
    { label: "Attendance Rate", value: `${summary?.monthAttendanceRate ?? 0}%`, icon: Users, color: "bg-purple-500/10 text-purple-600", note: "This month" },
    { label: "Overall Average", value: `${summary?.overallAverage ?? 0}%`, icon: TrendingUp, color: "bg-orange-500/10 text-orange-600", note: "Overall" },
  ]

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Welcome back, {firstName}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here&apos;s what&apos;s happening with your learning today.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between">
              <div className={`flex size-10 items-center justify-center rounded-xl ${s.color}`}>
                <s.icon className="size-5" />
              </div>
            </div>
            <p className="mt-3 text-2xl font-extrabold text-foreground">{loadingData ? "—" : s.value}</p>
            <p className="mt-1 text-sm text-muted-foreground">{s.label}</p>
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
          ) : recentLessons.length === 0 && continueItems.length === 0 ? (
            <div className="py-8 text-center">
              <BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No lessons yet</p>
              <p className="mt-1 text-xs text-muted-foreground">Lessons from your classes will appear here.</p>
            </div>
          ) : recentLessons.length > 0 ? (
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
          ) : (
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
              { label: "Live Classes", href: "/dashboard/live-classes", icon: Video },
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

      {continueItems.length === 0 && activities.length === 0 && recentLessons.length === 0 && (
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
