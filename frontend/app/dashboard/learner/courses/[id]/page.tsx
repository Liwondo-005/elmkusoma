"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type CourseDetail, type CourseModuleSummary, type CourseLesson, type Enrollment } from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import { BookOpen, ArrowLeft, ChevronDown, ChevronRight, Loader2, AlertCircle, CheckCircle } from "lucide-react"

export default function CourseDetailPage() {
  const { user, loading: authLoading } = useAuth()
  const params = useParams()
  const router = useRouter()
  const courseId = params.id as string

  const [course, setCourse] = useState<CourseDetail | null>(null)
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null)
  const [expandedModules, setExpandedModules] = useState<Set<string>>(new Set())
  const [moduleLessons, setModuleLessons] = useState<Record<string, CourseLesson[]>>({})
  const [loading, setLoading] = useState(true)
  const [enrolling, setEnrolling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadCourse()
  }, [user, courseId])

  async function loadCourse() {
    try {
      setLoading(true)
      setError(null)
      const [courseData, enrollmentsData] = await Promise.all([
        learnerApi.getCourse(courseId),
        learnerApi.getEnrollments().catch(() => []),
      ])
      setCourse(courseData)
      const existingEnrollment = enrollmentsData.find((e) => e.courseId === courseId)
      setEnrollment(existingEnrollment || null)
    } catch {
      setError("Failed to load course details")
    } finally {
      setLoading(false)
    }
  }

  async function handleEnroll() {
    try {
      setEnrolling(true)
      setError(null)
      const result = await learnerApi.enroll(courseId)
      setEnrollment(result)
      setSuccess("Successfully enrolled in this course!")
    } catch (err: any) {
      setError(err.message || "Failed to enroll in course")
    } finally {
      setEnrolling(false)
    }
  }

  async function toggleModule(moduleId: string) {
    const newExpanded = new Set(expandedModules)
    if (newExpanded.has(moduleId)) {
      newExpanded.delete(moduleId)
    } else {
      newExpanded.add(moduleId)
      if (!moduleLessons[moduleId]) {
        try {
          const lessons = await learnerApi.getModuleLessons(moduleId)
          setModuleLessons((prev) => ({ ...prev, [moduleId]: lessons }))
        } catch {
          // Failed to load lessons
        }
      }
    }
    setExpandedModules(newExpanded)
  }

  if (authLoading || loading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  if (!course) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            Course not found
          </div>
        </div>
        <Link href="/dashboard/learner/courses" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
          <ArrowLeft className="size-4" /> Back to courses
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <Link href="/dashboard/learner/courses" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to courses
      </Link>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-4">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <CheckCircle className="size-4" />
            {success}
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        {course.thumbnailUrl ? (
          <div className="mb-4 h-48 overflow-hidden rounded-xl bg-muted">
            <img src={course.thumbnailUrl} alt={course.title} className="h-full w-full object-cover" />
          </div>
        ) : (
          <div className="mb-4 flex h-48 items-center justify-center rounded-xl bg-primary/10">
            <BookOpen className="size-12 text-primary/40" />
          </div>
        )}
        <h1 className="text-xl font-bold text-foreground">{course.title}</h1>
        {course.description && (
          <p className="mt-2 text-sm text-muted-foreground">{course.description}</p>
        )}
        <div className="mt-4 flex items-center gap-3 flex-wrap">
          {course.level && (
            <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              {course.level}
            </span>
          )}
          {course.category && (
            <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              {course.category}
            </span>
          )}
          {course.modules && (
            <span className="text-xs text-muted-foreground">
              {course.modules.length} modules
            </span>
          )}
        </div>
        <div className="mt-6">
          {enrollment ? (
            <div className="space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Your progress</span>
                <span className="font-semibold text-teal">{enrollment.progressPercentage}%</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${enrollment.progressPercentage}%` }} />
              </div>
              <Link
                href={`/dashboard/learner/my-learning`}
                className="inline-flex items-center gap-2 rounded-lg bg-teal px-4 py-2 text-sm font-medium text-white hover:bg-teal/90"
              >
                Continue Learning
              </Link>
            </div>
          ) : (
            <button
              onClick={handleEnroll}
              disabled={enrolling}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {enrolling ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Enrolling...
                </>
              ) : (
                "Enroll in Course"
              )}
            </button>
          )}
        </div>
      </div>

      {course.modules && course.modules.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground">Course Modules</h2>
          <div className="mt-4 space-y-3">
            {course.modules.sort((a, b) => a.sortOrder - b.sortOrder).map((module) => (
              <div key={module.id} className="rounded-xl border border-border overflow-hidden">
                <button
                  onClick={() => toggleModule(module.id)}
                  className="flex w-full items-center gap-3 p-4 text-left hover:bg-muted/30 transition-colors"
                >
                  <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <span className="text-xs font-bold">{module.sortOrder}</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground">{module.title}</p>
                    {module.description && (
                      <p className="text-xs text-muted-foreground truncate">{module.description}</p>
                    )}
                  </div>
                  <span className="text-xs text-muted-foreground">{module.lessonCount} lessons</span>
                  {expandedModules.has(module.id) ? (
                    <ChevronDown className="size-4 text-muted-foreground" />
                  ) : (
                    <ChevronRight className="size-4 text-muted-foreground" />
                  )}
                </button>
                {expandedModules.has(module.id) && moduleLessons[module.id] && (
                  <div className="border-t border-border bg-muted/20">
                    {moduleLessons[module.id].sort((a, b) => a.sortOrder - b.sortOrder).map((lesson) => (
                      <div key={lesson.id} className="flex items-center gap-3 px-4 py-3 border-b border-border last:border-0">
                        <div className="flex size-6 items-center justify-center rounded bg-muted text-xs text-muted-foreground">
                          {lesson.sortOrder}
                        </div>
                        <p className="text-sm text-foreground">{lesson.title}</p>
                      </div>
                    ))}
                    {moduleLessons[module.id].length === 0 && (
                      <p className="px-4 py-3 text-sm text-muted-foreground">No lessons in this module yet.</p>
                    )}
                  </div>
                )}
                {expandedModules.has(module.id) && !moduleLessons[module.id] && (
                  <div className="border-t border-border bg-muted/20 px-4 py-3">
                    <Loader2 className="size-4 animate-spin text-muted-foreground" />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
