"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, type DashboardSummary, type ContinueLearningItem, type RecentActivity } from "@/lib/api"
import { BookOpen, FileText, Loader2, PenTool, BarChart3, ArrowRight, Award, Users, TrendingUp, Clock, CheckCircle } from "lucide-react"
import { getDashboardConfig, getLevelLabel, type LearningLevel } from "@/lib/learner-config"
import { LearnerHeader, ContinueLearningCard, LearningItemCard, AssignmentCard, ProgressCard, EmptyState, LoadingState } from "@/components/learner/shared"

interface DashboardData {
  totalLessons: number
  pendingAssignments: number
  assessments: number
  certificates: number
  recentLessons: { title: string; subject: string; progress: number; id?: string }[]
  pendingWork: { title: string; subject?: string; dueDate?: string; status: string }[]
  recentActivity: { label: string; detail: string; time: string }[]
}

export default function DashboardPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [continueItems, setContinueItems] = useState<ContinueLearningItem[]>([])
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [loading, setLoading] = useState(true)
  const firstName = user?.name?.split(" ")[0] || "Student"
  const level = user?.learningLevel as LearningLevel | null
  const config = getDashboardConfig(level)
  const [data, setData] = useState<DashboardData>({
    totalLessons: 0,
    pendingAssignments: 0,
    assessments: 0,
    certificates: 0,
    recentLessons: [],
    pendingWork: [],
    recentActivity: [],
  })

  useEffect(() => {
    if (!authLoading && user?.role === "Parent") {
      router.replace("/dashboard/parent")
    } else if (!authLoading && user?.role === "Teacher") {
      router.replace("/dashboard/teacher")
    } else if (!authLoading && (user?.role === "Admin" || user?.role === "Institution Admin")) {
      router.replace("/dashboard/admin")
    } else if (!authLoading && user?.role === "Other Learner") {
      router.replace("/dashboard/learner")
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

        if ((student as any)?.classGroupId) {
          try {
            const classGroup = await academicApi.getClassGroup((student as any).classGroupId)
            if (classGroup?.gradeId) {
              const grade = await academicApi.getGrade(classGroup.gradeId)
              if (grade?.educationLevel) {
                localStorage.setItem("elmkusoma_education_level", grade.educationLevel)
              }
            }
          } catch { /* education level stays default */ }
        }

        const classId = user?.classGroupId || (student as any)?.classGroupId || ""

        const [assignments, assessments, certificates, lessons] = await Promise.all([
          classId ? learningApi.getAssignments(classId).catch(() => []) : Promise.resolve([]),
          classId ? assessmentApi.getByClass(classId).catch(() => []) : Promise.resolve([]),
          certificateApi.list().catch(() => []),
          classId ? learningApi.getLessonsByClass(classId).catch(() => []) : Promise.resolve([]),
        ])

        const lessonList = (Array.isArray(lessons) ? lessons : []).map((l: any) => ({
          title: l.title || "Untitled Lesson",
          subject: l.subjectName || "",
          progress: l.completionPercentage || 0,
          id: l.id,
        }))

        const assignmentList = (Array.isArray(assignments) ? assignments : []).slice(0, 5).map((a: any) => ({
          title: a.title || "Assignment",
          subject: a.subjectName || "",
          dueDate: a.dueDate,
          status: a.status || "PENDING",
        }))

        setData({
          totalLessons: lessonList.length,
          pendingAssignments: assignmentList.filter((a: any) => a.status === "PENDING" || a.status === "ACTIVE").length,
          assessments: Array.isArray(assessments) ? assessments.length : 0,
          certificates: Array.isArray(certificates) ? certificates.length : 0,
          recentLessons: lessonList.slice(0, 3),
          pendingWork: assignmentList,
          recentActivity: lessonList.slice(0, 3).map((l: any) => ({
            label: "Lesson available",
            detail: l.title,
            time: "",
          })),
        })
      } catch { /* loads with zero data */ }
    } catch {
      // Dashboard data unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading || user?.role === "Parent" || user?.role === "Teacher" || user?.role === "Admin" || user?.role === "Institution Admin") {
    return <LoadingState />
  }

  const levelLabel = getLevelLabel(level)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} level={levelLabel} subtitle={config.subtitle} />

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <ProgressCard label="Enrolled Courses" value={summary?.activeEnrollments ?? 0} />
        <ProgressCard label="Lessons Completed" value={summary?.completedLessons ?? 0} />
        <ProgressCard label="Attendance Rate" value={summary?.monthAttendanceRate ?? 0} />
        <ProgressCard label="Overall Average" value={summary?.overallAverage ?? 0} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Continue Learning */}
        <div className="lg:col-span-2">
          {continueItems.length > 0 ? (
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
                      <div className="h-full rounded-full bg-primary" style={{ width: `${item.completionPercentage}%` }} />
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : data.recentLessons.length > 0 ? (
            <ContinueLearningCard
              title={data.recentLessons[0].title}
              subject={data.recentLessons[0].subject}
              progress={data.recentLessons[0].progress}
              onResume={() => data.recentLessons[0].id && router.push("/dashboard/lessons")}
            />
          ) : (
            <ContinueLearningCard title={undefined} onResume={() => router.push("/dashboard/lessons")} />
          )}
        </div>

        {/* Quick Links */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Quick Links</h2>
          <div className="mt-4 space-y-2">
            {config.navItems.slice(1, 5).map((item) => (
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
        </div>
      </div>

      {/* Today's Learning */}
      {data.recentLessons.length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-foreground">Today&apos;s Learning</h2>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {data.recentLessons.map((lesson, i) => (
              <LearningItemCard
                key={i}
                title={lesson.title}
                subject={lesson.subject}
                icon={<BookOpen className="size-4" />}
              />
            ))}
          </div>
        </section>
      )}

      {/* Recent Activity */}
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

      {/* Empty state if no data */}
      {data.totalLessons === 0 && continueItems.length === 0 && activities.length === 0 && (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title={config.emptyStateTitle}
          description={config.emptyStateDescription}
          action={
            <Link href="/dashboard/lessons" className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              Browse Lessons <ArrowRight className="size-4" />
            </Link>
          }
        />
      )}
    </div>
  )
}
