"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { BookOpen, FileText, PenTool, BarChart3, ArrowRight, Loader2, Award } from "lucide-react"
import { useAuth } from "@/lib/auth"
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
  const { user, loading } = useAuth()
  const router = useRouter()
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
      finally { setLoadingData(false) }
    }
    load()
  }, [user, loading])

  if (user?.role === "Parent" || user?.role === "Teacher" || user?.role === "Admin" || user?.role === "Institution Admin") {
    return null
  }

  if (loading || loadingData) return <LoadingState />

  const levelLabel = getLevelLabel(level)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} level={levelLabel} subtitle={config.subtitle} />

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <ProgressCard label="Lessons" value={data.totalLessons} />
        <ProgressCard label="Pending Assignments" value={data.pendingAssignments} />
        <ProgressCard label="Assessments" value={data.assessments} />
        <ProgressCard label="Certificates" value={data.certificates} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Continue Learning */}
        <div className="lg:col-span-2">
          {data.recentLessons.length > 0 ? (
            <ContinueLearningCard
              title={data.recentLessons[0].title}
              subject={data.recentLessons[0].subject}
              progress={data.recentLessons[0].progress}
              onResume={() => data.recentLessons[0].id && router.push(`/dashboard/lessons`)}
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

      {/* Pending Work */}
      {data.pendingWork.length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-foreground">Pending Work</h2>
          <div className="mt-3 space-y-2">
            {data.pendingWork.map((work, i) => (
              <AssignmentCard
                key={i}
                title={work.title}
                subject={work.subject}
                dueDate={work.dueDate}
                status={work.status}
              />
            ))}
          </div>
        </section>
      )}

      {/* Empty state if no data */}
      {data.totalLessons === 0 && data.pendingAssignments === 0 && (
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
