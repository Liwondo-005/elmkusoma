"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type CourseDetail, type CourseModuleSummary, type CourseLesson, type Enrollment, type CourseSummary, type CourseLessonFlat } from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import { BookOpen, ArrowLeft, ChevronDown, ChevronRight, Loader2, AlertCircle, CheckCircle, Clock, Bookmark, BookmarkCheck, Play, FileText, ExternalLink } from "lucide-react"
import { getLastAccessedLesson } from "@/lib/learner-api"

export default function CourseDetailPage() {
  const { user, loading: authLoading } = useAuth()
  const params = useParams()
  const router = useRouter()
  const courseId = params.id as string

  const [courseData, setCourseData] = useState<CourseDetail | null>(null)
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null)
  const [expandedModules, setExpandedModules] = useState<Set<string>>(new Set())
  const [moduleLessons, setModuleLessons] = useState<Record<string, CourseLesson[]>>({})
  const [courseLessons, setCourseLessons] = useState<CourseLessonFlat[]>([])
  const [completedLessons, setCompletedLessons] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [enrolling, setEnrolling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [relatedCourses, setRelatedCourses] = useState<CourseSummary[]>([])
  const [bookmarked, setBookmarked] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)
  const [courseProgress, setCourseProgress] = useState<{ total: number; completed: number; pct: number }>({ total: 0, completed: 0, pct: 0 })

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadCourse()
  }, [user, courseId])

  async function loadCourse() {
    try {
      setLoading(true)
      setError(null)
      const [courseData, enrollmentsData, courseLessonsData] = await Promise.all([
        learnerApi.getCourse(courseId),
        learnerApi.getEnrollments().catch(() => []),
        learnerApi.getCourseLessons(courseId).catch(() => []),
      ])
      setCourseData(courseData)
      const existingEnrollment = enrollmentsData.find((e) => e.courseId === courseId)
      setEnrollment(existingEnrollment || null)
      setCourseLessons(courseLessonsData)

      // Compute progress from enrollment
      if (existingEnrollment) {
        const total = courseLessonsData.length
        const pct = existingEnrollment.progressPercentage || 0
        const completed = Math.round((pct / 100) * total)
        setCourseProgress({ total, completed, pct })
      } else {
        setCourseProgress({ total: courseLessonsData.length, completed: 0, pct: 0 })
      }

      learnerApi.getRelatedCourses(courseId).then(setRelatedCourses).catch(() => {})
      learnerApi.checkBookmark("course", courseId).then((isBookmarked) => {
        setBookmarked(isBookmarked)
      }).catch(() => {})
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

  async function handleBookmark() {
    try {
      setBookmarkLoading(true)
      if (bookmarked) {
        const bookmarks = await learnerApi.getBookmarks()
        const existing = bookmarks.find((b) => b.targetType === "course" && b.targetId === courseId)
        if (existing) {
          await learnerApi.removeBookmark(existing.id)
        }
        setBookmarked(false)
      } else {
        await learnerApi.addBookmark("course", courseId)
        setBookmarked(true)
      }
    } catch {
      // Silent fail
    } finally {
      setBookmarkLoading(false)
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

  function getContentTypeIcon(contentType: string | null | undefined) {
    switch (contentType?.toUpperCase()) {
      case "VIDEO": return <Play className="size-3" />
      case "DOCUMENT": return <FileText className="size-3" />
      case "LINK": return <ExternalLink className="size-3" />
      default: return <BookOpen className="size-3" />
    }
  }

  // Find the first incomplete lesson (or the last lesson)
  function getNextLesson(): CourseLessonFlat | null {
    if (courseLessons.length === 0) return null
    const incomplete = courseLessons.find((l) => !completedLessons.has(l.id))
    return incomplete || courseLessons[courseLessons.length - 1]
  }

  if (authLoading || loading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  if (!courseData) {
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

  const { course, modules } = courseData
  const nextLesson = getNextLesson()

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <Link href="/dashboard/learner/courses" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
          <ArrowLeft className="size-4" /> Back to courses
        </Link>
        <button
          onClick={handleBookmark}
          disabled={bookmarkLoading}
          className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted"
        >
          {bookmarked ? (
            <BookmarkCheck className="size-3.5 text-primary" />
          ) : (
            <Bookmark className="size-3.5" />
          )}
          {bookmarked ? "Saved" : "Save"}
        </button>
      </div>

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
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <h1 className="text-xl font-bold text-foreground">{course.title}</h1>
            {course.description && (
              <p className="mt-2 text-sm text-muted-foreground">{course.description}</p>
            )}
          </div>
          <button
            onClick={handleBookmark}
            disabled={bookmarkLoading}
            className="shrink-0 rounded-lg border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
            title={bookmarked ? "Remove bookmark" : "Bookmark this course"}
          >
            {bookmarked ? <BookmarkCheck className="size-5 text-primary" /> : <Bookmark className="size-5" />}
          </button>
        </div>
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
          {modules && (
            <span className="text-xs text-muted-foreground">
              {modules.length} modules &middot; {courseLessons.length} lessons
            </span>
          )}
        </div>
        <div className="mt-6">
          {enrollment ? (
            <div className="space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Your progress</span>
                <span className="font-semibold text-teal">{courseProgress.pct.toFixed(0)}%</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${courseProgress.pct}%` }} />
              </div>
              <p className="text-xs text-muted-foreground">
                {courseProgress.completed} of {courseProgress.total} lessons completed
              </p>
              {nextLesson ? (
                <Link
                  href={`/dashboard/learner/courses/${courseId}/lessons/${nextLesson.id}`}
                  className="inline-flex items-center gap-2 rounded-lg bg-teal px-4 py-2 text-sm font-medium text-white hover:bg-teal/90"
                >
                  <Play className="size-4" />
                  {courseProgress.pct > 0 ? "Continue Learning" : "Start Learning"}
                </Link>
              ) : (
                <Link
                  href={(() => {
                    const last = getLastAccessedLesson()
                    if (last && last.courseId === courseId) {
                      return `/dashboard/learner/courses/${courseId}/lessons/${last.lessonId}`
                    }
                    if (modules && modules.length > 0) {
                      const firstModule = modules.sort((a, b) => a.sortOrder - b.sortOrder)[0]
                      return `/dashboard/learner/courses/${courseId}/lessons?moduleId=${firstModule.id}`
                    }
                    return `/dashboard/learner/my-learning`
                  })()}
                  className="inline-flex items-center gap-2 rounded-lg bg-teal px-4 py-2 text-sm font-medium text-white hover:bg-teal/90"
                >
                  Continue Learning
                </Link>
              )}
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

      {modules && modules.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground">Course Content</h2>
          <div className="mt-4 space-y-3">
            {modules.sort((a, b) => a.sortOrder - b.sortOrder).map((module) => (
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
                      <Link
                        key={lesson.id}
                        href={enrollment ? `/dashboard/learner/courses/${courseId}/lessons/${lesson.id}` : "#"}
                        className={`flex items-center gap-3 px-4 py-3 border-b border-border last:border-0 transition-colors ${
                          enrollment ? "hover:bg-muted/50 cursor-pointer" : "cursor-default"
                        }`}
                      >
                        <div className="flex size-6 items-center justify-center rounded bg-muted text-muted-foreground">
                          {getContentTypeIcon(lesson.contentType)}
                        </div>
                        <p className="flex-1 text-sm text-foreground">{lesson.title}</p>
                        {lesson.durationMinutes && (
                          <span className="text-[10px] text-muted-foreground flex items-center gap-1">
                            <Clock className="size-3" />
                            {lesson.durationMinutes}m
                          </span>
                        )}
                        {enrollment && (
                          <span className="text-[10px] text-muted-foreground capitalize">{lesson.contentType?.toLowerCase()}</span>
                        )}
                      </Link>
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

      {relatedCourses.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground">Related Courses</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {relatedCourses.map((rc) => (
              <Link
                key={rc.id}
                href={`/dashboard/learner/courses/${rc.id}`}
                className="rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
              >
                {rc.thumbnailUrl ? (
                  <div className="mb-3 h-24 overflow-hidden rounded-lg bg-muted">
                    <img src={rc.thumbnailUrl} alt={rc.title} className="h-full w-full object-cover" />
                  </div>
                ) : (
                  <div className="mb-3 flex h-24 items-center justify-center rounded-lg bg-primary/10">
                    <BookOpen className="size-6 text-primary/40" />
                  </div>
                )}
                <h3 className="text-sm font-semibold text-foreground truncate">{rc.title}</h3>
                <div className="mt-1 flex items-center gap-2">
                  {rc.level && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                      {rc.level}
                    </span>
                  )}
                  {rc.category && (
                    <span className="text-[10px] text-muted-foreground">{rc.category}</span>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
