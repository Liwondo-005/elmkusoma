"use client"

import { useEffect, useState, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import {
  learnerApi,
  type CourseDetail,
  type CourseModuleSummary,
  type CourseLesson,
  type Enrollment,
  setLastAccessedLesson,
} from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import {
  ArrowLeft,
  CheckCircle,
  Loader2,
  AlertCircle,
  BookOpen,
  Clock,
  ChevronLeft,
  ChevronRight,
  Video,
  File,
  FileText,
  LinkIcon,
  Type,
  ExternalLink,
  Bookmark,
  BookmarkCheck,
} from "lucide-react"

export default function LessonViewerPage() {
  const { user, loading: authLoading } = useAuth()
  const params = useParams()
  const router = useRouter()
  const courseId = params.id as string
  const lessonId = params.lessonId as string

  const [courseData, setCourseData] = useState<CourseDetail | null>(null)
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null)
  const [allLessons, setAllLessons] = useState<CourseLesson[]>([])
  const [currentLesson, setCurrentLesson] = useState<CourseLesson | null>(null)
  const [currentModule, setCurrentModule] = useState<CourseModuleSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [completing, setCompleting] = useState(false)
  const [completed, setCompleted] = useState(false)
  const [bookmarked, setBookmarked] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)

  const loadLesson = useCallback(async () => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    try {
      setLoading(true)
      setError(null)

      const [courseResp, enrollments] = await Promise.all([
        learnerApi.getCourse(courseId),
        learnerApi.getEnrollments().catch(() => []),
      ])

      setCourseData(courseResp)
      const existing = enrollments.find((e) => e.courseId === courseId)
      setEnrollment(existing || null)

      const sortedModules = [...(courseResp.modules || [])].sort((a, b) => a.sortOrder - b.sortOrder)

      const lessonsForAllModules: CourseLesson[] = []
      let foundLesson: CourseLesson | null = null
      let foundModule: CourseModuleSummary | null = null

      for (const mod of sortedModules) {
        const lessons = await learnerApi.getModuleLessons(mod.id)
        const sorted = [...lessons].sort((a, b) => a.sortOrder - b.sortOrder)
        lessonsForAllModules.push(...sorted)

        const match = sorted.find((l) => l.id === lessonId)
        if (match) {
          foundLesson = match
          foundModule = mod
        }
      }

      setAllLessons(lessonsForAllModules)
      setCurrentLesson(foundLesson)
      setCurrentModule(foundModule)

      if (!foundLesson) {
        setError("Lesson not found")
      }
    } catch {
      setError("Failed to load lesson")
    } finally {
      setLoading(false)
    }
  }, [user, courseId, lessonId])

  useEffect(() => {
    loadLesson()
  }, [loadLesson])

  useEffect(() => {
    if (!lessonId) return
    learnerApi.checkBookmark("course_lesson", lessonId).then((isBookmarked) => {
      setBookmarked(isBookmarked)
    }).catch(() => {})
  }, [lessonId])

  async function handleBookmark() {
    try {
      setBookmarkLoading(true)
      if (bookmarked) {
        const bookmarks = await learnerApi.getBookmarks()
        const existing = bookmarks.find((b) => b.targetId === lessonId && b.targetType === "course_lesson")
        if (existing) await learnerApi.removeBookmark(existing.id)
        setBookmarked(false)
      } else {
        await learnerApi.addBookmark("course_lesson", lessonId)
        setBookmarked(true)
      }
    } catch {} finally { setBookmarkLoading(false) }
  }

  useEffect(() => {
    if (currentLesson && currentModule && courseData) {
      setLastAccessedLesson({
        courseId,
        courseTitle: courseData.course.title,
        moduleId: currentModule.id,
        moduleTitle: currentModule.title,
        lessonId: currentLesson.id,
        lessonTitle: currentLesson.title,
        lastAccessedAt: new Date().toISOString(),
      })
    }
  }, [currentLesson, currentModule, courseData, courseId])

  const currentIndex = allLessons.findIndex((l) => l.id === lessonId)
  const prevLesson = currentIndex > 0 ? allLessons[currentIndex - 1] : null
  const nextLesson = currentIndex < allLessons.length - 1 ? allLessons[currentIndex + 1] : null

  async function handleMarkComplete() {
    if (!currentLesson || completing || completed) return
    try {
      setCompleting(true)
      await learnerApi.completeLesson(currentLesson.id)
      setCompleted(true)
    } catch {
      await learnerApi.updateProgress(currentLesson.id, 100).catch(() => {})
      setCompleted(true)
    } finally {
      setCompleting(false)
    }
  }

  function navigateToLesson(id: string) {
    router.push(`/dashboard/learner/courses/${courseId}/lessons/${id}`)
  }

  if (authLoading || loading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  if (error || !currentLesson) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <Link
          href={`/dashboard/learner/courses/${courseId}`}
          className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline"
        >
          <ArrowLeft className="size-4" /> Back to course
        </Link>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error || "Lesson not found"}
          </div>
        </div>
      </div>
    )
  }

  const contentTypeIcon: Record<string, typeof Video> = {
    VIDEO: Video,
    DOCUMENT: File,
    LINK: LinkIcon,
    TEXT: Type,
    QUIZ: FileText,
    ASSIGNMENT: FileText,
  }

  const ContentIcon = contentTypeIcon[currentLesson.contentType] || FileText

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between gap-4">
        <Link
          href={`/dashboard/learner/courses/${courseId}`}
          className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline"
        >
          <ArrowLeft className="size-4" /> {courseData?.course.title || "Course"}
        </Link>
        <div className="flex items-center gap-2">
          <button
            onClick={handleBookmark}
            disabled={bookmarkLoading}
            className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted"
          >
            {bookmarked ? <BookmarkCheck className="size-3.5 text-primary" /> : <Bookmark className="size-3.5" />}
            {bookmarked ? "Saved" : "Save"}
          </button>
          {currentModule && (
            <span className="text-xs text-muted-foreground">
              Module: {currentModule.title}
            </span>
          )}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <ContentIcon className="size-5 text-primary" />
          </div>
          <div className="flex-1">
            <h1 className="text-lg font-bold text-foreground">{currentLesson.title}</h1>
            <div className="flex items-center gap-3 mt-1">
              <span className="text-xs text-muted-foreground">
                Lesson {currentIndex + 1} of {allLessons.length}
              </span>
              {currentLesson.durationMinutes != null && (
                <span className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Clock className="size-3" /> {currentLesson.durationMinutes} min
                </span>
              )}
              <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                {currentLesson.contentType}
              </span>
            </div>
          </div>
        </div>

        {currentLesson.contentType === "VIDEO" && currentLesson.contentUrl && (() => {
          const isYouTube = currentLesson.contentUrl.includes("youtube.com") || currentLesson.contentUrl.includes("youtu.be")
          if (isYouTube) {
            let embedUrl = currentLesson.contentUrl
            const match = currentLesson.contentUrl.match(/(?:v=|youtu\.be\/)([^&?#]+)/)
            if (match) embedUrl = `https://www.youtube.com/embed/${match[1]}`
            return (
              <div className="mb-4 aspect-video overflow-hidden rounded-xl bg-black">
                <iframe src={embedUrl} className="h-full w-full" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen title={currentLesson.title} />
              </div>
            )
          }
          return (
            <div className="mb-4 overflow-hidden rounded-xl bg-black">
              <video src={currentLesson.contentUrl} controls className="w-full" style={{ maxHeight: 480 }}>
                Your browser does not support video playback.
              </video>
            </div>
          )
        })()}

        {currentLesson.contentType === "TEXT" && currentLesson.contentUrl && (
          <div className="mb-4 rounded-xl border border-border bg-muted/30 p-6">
            <div className="prose prose-sm max-w-none text-foreground whitespace-pre-wrap">
              {currentLesson.contentUrl}
            </div>
          </div>
        )}

        {currentLesson.contentType === "DOCUMENT" && currentLesson.contentUrl && (
          <div className="mb-4">
            <a
              href={currentLesson.contentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-muted/30 px-4 py-3 text-sm font-medium text-foreground hover:bg-muted/50 transition-colors"
            >
              <FileText className="size-4 text-primary" />
              Open Document
              <ExternalLink className="size-3 text-muted-foreground" />
            </a>
          </div>
        )}

        {currentLesson.contentType === "LINK" && currentLesson.contentUrl && (
          <div className="mb-4">
            <a
              href={currentLesson.contentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-muted/30 px-4 py-3 text-sm font-medium text-foreground hover:bg-muted/50 transition-colors"
            >
              <ExternalLink className="size-4 text-primary" />
              Open External Resource
            </a>
          </div>
        )}

        {currentLesson.contentType === "QUIZ" && (
          <div className="mb-4 rounded-xl border border-border bg-muted/30 p-6 text-center">
            <FileText className="mx-auto size-8 text-primary/40 mb-2" />
            <p className="text-sm text-muted-foreground">
              This lesson contains a quiz. Assessment features coming soon.
            </p>
          </div>
        )}

        {currentLesson.contentType === "ASSIGNMENT" && (
          <div className="mb-4 rounded-xl border border-border bg-muted/30 p-6 text-center">
            <FileText className="mx-auto size-8 text-primary/40 mb-2" />
            <p className="text-sm text-muted-foreground">
              This lesson contains an assignment. Submission features coming soon.
            </p>
          </div>
        )}

        {!currentLesson.contentUrl && currentLesson.contentType !== "QUIZ" && currentLesson.contentType !== "ASSIGNMENT" && (
          <div className="mb-4 rounded-xl border border-dashed border-border bg-muted/20 p-8 text-center">
            <BookOpen className="mx-auto size-8 text-muted-foreground/40 mb-2" />
            <p className="text-sm text-muted-foreground">
              Content is being prepared. Check back later.
            </p>
          </div>
        )}

        <div className="flex items-center gap-3 mt-6 pt-4 border-t border-border">
          <button
            onClick={handleMarkComplete}
            disabled={completing || completed}
            className={`inline-flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
              completed
                ? "bg-green-500/10 text-green-600 border border-green-500/20"
                : "bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            }`}
          >
            {completed ? (
              <>
                <CheckCircle className="size-4" />
                Completed
              </>
            ) : completing ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <CheckCircle className="size-4" />
                Mark as Complete
              </>
            )}
          </button>
          {enrollment && (
            <div className="flex-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">Course progress</span>
                <span className="font-semibold text-teal">{enrollment.progressPercentage}%</span>
              </div>
              <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-teal transition-all"
                  style={{ width: `${enrollment.progressPercentage}%` }}
                />
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between gap-4">
        {prevLesson ? (
          <button
            onClick={() => navigateToLesson(prevLesson.id)}
            className="inline-flex items-center gap-2 rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted/50 transition-colors"
          >
            <ChevronLeft className="size-4" />
            {prevLesson.title}
          </button>
        ) : (
          <div />
        )}
        {nextLesson ? (
          <button
            onClick={() => navigateToLesson(nextLesson.id)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            {nextLesson.title}
            <ChevronRight className="size-4" />
          </button>
        ) : (
          <Link
            href={`/dashboard/learner/courses/${courseId}`}
            className="inline-flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-green-700 transition-colors"
          >
            <CheckCircle className="size-4" />
            Back to Course
          </Link>
        )}
      </div>

      {allLessons.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-sm font-semibold text-foreground mb-3">All Lessons</h2>
          <div className="space-y-1">
            {allLessons.map((lesson, idx) => (
              <button
                key={lesson.id}
                onClick={() => navigateToLesson(lesson.id)}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm transition-colors ${
                  lesson.id === lessonId
                    ? "bg-primary/10 text-primary font-medium"
                    : "hover:bg-muted/50 text-foreground"
                }`}
              >
                <div
                  className={`flex size-6 shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                    lesson.id === lessonId
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted text-muted-foreground"
                  }`}
                >
                  {idx + 1}
                </div>
                <span className="truncate">{lesson.title}</span>
                {lesson.durationMinutes != null && (
                  <span className="ml-auto shrink-0 text-[10px] text-muted-foreground">
                    {lesson.durationMinutes}m
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
