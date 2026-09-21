"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment, type CourseModuleSummary, type CourseLesson } from "@/lib/learner-api"
import { LoadingState, EmptyState } from "@/components/learner/shared"
import { ClipboardCheck, BookOpen, ArrowRight, AlertCircle, Clock, FileText, CheckCircle } from "lucide-react"

interface AssessmentItem {
  courseId: string
  courseTitle: string
  moduleId: string
  moduleTitle: string
  lesson: CourseLesson
}

export default function AssessmentsPage() {
  const { user, loading: authLoading } = useAuth()
  const [assessments, setAssessments] = useState<AssessmentItem[]>([])
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<"all" | "quiz" | "assignment">("all")

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const enrollmentsData = await learnerApi.getEnrollments().catch(() => [])
      setEnrollments(enrollmentsData)

      const allAssessments: AssessmentItem[] = []
      for (const enrollment of enrollmentsData) {
        try {
          const courseDetail = await learnerApi.getCourse(enrollment.courseId)
          for (const mod of courseDetail.modules || []) {
            try {
              const lessons = await learnerApi.getModuleLessons(mod.id)
              for (const lesson of lessons) {
                if (lesson.contentType === "QUIZ" || lesson.contentType === "ASSIGNMENT") {
                  allAssessments.push({
                    courseId: enrollment.courseId,
                    courseTitle: enrollment.courseTitle,
                    moduleId: mod.id,
                    moduleTitle: mod.title,
                    lesson,
                  })
                }
              }
            } catch {
              // Skip failed module
            }
          }
        } catch {
          // Skip failed course
        }
      }
      setAssessments(allAssessments)
    } catch {
      setError("Failed to load assessments")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  const filtered = assessments.filter((a) => {
    if (filter === "all") return true
    if (filter === "quiz") return a.lesson.contentType === "QUIZ"
    if (filter === "assignment") return a.lesson.contentType === "ASSIGNMENT"
    return true
  })

  const quizCount = assessments.filter((a) => a.lesson.contentType === "QUIZ").length
  const assignmentCount = assessments.filter((a) => a.lesson.contentType === "ASSIGNMENT").length

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Assessments</h1>
        <p className="mt-1 text-sm text-muted-foreground">Quizzes and assignments from your enrolled courses.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Total Assessments</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{assessments.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Quizzes</p>
          <p className="mt-1 text-2xl font-extrabold text-primary">{quizCount}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Assignments</p>
          <p className="mt-1 text-2xl font-extrabold text-orange">{assignmentCount}</p>
        </div>
      </div>

      <div className="flex gap-2">
        {(["all", "quiz", "assignment"] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`h-9 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === f ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            {f === "all" ? "All" : f === "quiz" ? "Quizzes" : "Assignments"}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingState />
      ) : assessments.length === 0 ? (
        <EmptyState
          icon={<ClipboardCheck className="size-8" />}
          title="No assessments yet"
          description="Assessments will appear here when your courses include quizzes or assignments."
          action={
            <Link
              href="/dashboard/learner/courses"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Explore Courses <ArrowRight className="size-4" />
            </Link>
          }
        />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<ClipboardCheck className="size-8" />}
          title={`No ${filter === "quiz" ? "quizzes" : "assignments"} found`}
          description="Try a different filter."
        />
      ) : (
        <div className="space-y-3">
          {filtered.map((item) => (
            <Link
              key={item.lesson.id}
              href={`/dashboard/learner/courses/${item.courseId}/lessons/${item.lesson.id}`}
              className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
            >
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                {item.lesson.contentType === "QUIZ" ? (
                  <FileText className="size-5 text-primary" />
                ) : (
                  <ClipboardCheck className="size-5 text-orange" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-foreground truncate">{item.lesson.title}</p>
                <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                  <span>{item.courseTitle}</span>
                  <span>-</span>
                  <span>{item.moduleTitle}</span>
                </div>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                  item.lesson.contentType === "QUIZ"
                    ? "bg-primary/10 text-primary"
                    : "bg-orange/10 text-orange"
                }`}>
                  {item.lesson.contentType}
                </span>
                {item.lesson.durationMinutes != null && (
                  <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                    <Clock className="size-3" /> {item.lesson.durationMinutes}m
                  </span>
                )}
                <ArrowRight className="size-4 text-muted-foreground" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
