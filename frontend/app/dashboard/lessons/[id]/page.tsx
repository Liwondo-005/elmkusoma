"use client"

import { useEffect, useState, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { learningApi, type Lesson, type LessonProgress } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { ArrowLeft, CheckCircle, Clock, BookOpen, Play } from "lucide-react"

export default function LessonDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useRequireAuth()
  const [lesson, setLesson] = useState<Lesson | null>(null)
  const [progress, setProgress] = useState<LessonProgress | null>(null)
  const [loading, setLoading] = useState(true)
  const [completing, setCompleting] = useState(false)

  useEffect(() => {
    if (!user || !params.id) return
    loadData()
  }, [user, params.id])

  async function loadData() {
    try {
      setLoading(true)
      const allLessons = await learningApi.getLessonsByClass(user!.id)
      const found = allLessons.find((l) => l.id === params.id)
      if (found) {
        setLesson(found)
        const progressData = await learningApi.getStudentProgress(user!.id).catch(() => [])
        const p = progressData.find((pr) => pr.lessonId === params.id)
        if (p) setProgress(p)
      }
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  const markComplete = useCallback(async () => {
    if (!lesson || !user) return
    setCompleting(true)
    try {
      const result = await learningApi.updateProgress(lesson.id, 100)
      setProgress(result)
    } catch {
      // ignore
    } finally {
      setCompleting(false)
    }
  }, [lesson, user])

  const updateProgressPct = useCallback(async (pct: number) => {
    if (!lesson || !user) return
    try {
      const result = await learningApi.updateProgress(lesson.id, pct)
      setProgress(result)
    } catch {
      // ignore
    }
  }, [lesson, user])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!lesson) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={() => router.back()}>
          <ArrowLeft className="mr-2 size-4" /> Back
        </Button>
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Lesson Not Found</h3>
        </div>
      </div>
    )
  }

  const pct = progress?.completionPercentage || 0

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => router.back()} className="w-fit">
        <ArrowLeft className="mr-2 size-4" /> Back to Lessons
      </Button>

      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{lesson.title}</h1>
            {lesson.description && (
              <p className="mt-2 text-sm text-muted-foreground">{lesson.description}</p>
            )}
          </div>
          <div className="flex items-center gap-2">
            {pct >= 100 ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                <CheckCircle className="size-3" /> Completed
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
                <Clock className="size-3" /> {Math.round(pct)}% Complete
              </span>
            )}
          </div>
        </div>

        {lesson.videoUrl && (
          <div className="mt-6 aspect-video overflow-hidden rounded-xl bg-muted">
            <iframe
              src={lesson.videoUrl}
              className="h-full w-full"
              allowFullScreen
              title={lesson.title}
            />
          </div>
        )}

        {lesson.contentText && (
          <div className="mt-6 prose prose-sm max-w-none text-foreground">
            <div className="whitespace-pre-wrap rounded-xl bg-muted/50 p-6 text-sm leading-relaxed">
              {lesson.contentText}
            </div>
          </div>
        )}

        {lesson.fileAttachments && (
          <div className="mt-6 rounded-xl border border-border bg-muted/30 p-4">
            <h3 className="text-sm font-semibold text-foreground">Attachments</h3>
            <p className="mt-1 text-xs text-muted-foreground">{lesson.fileAttachments}</p>
          </div>
        )}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground">Progress</h2>
        <div className="mt-3">
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>Completion</span>
            <span>{Math.round(pct)}%</span>
          </div>
          <div className="mt-2 h-2 w-full rounded-full bg-muted overflow-hidden">
            <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${pct}%` }} />
          </div>
        </div>
        <div className="mt-4 flex gap-2">
          {pct < 25 && (
            <Button variant="outline" size="sm" onClick={() => updateProgressPct(25)}>
              Mark 25%
            </Button>
          )}
          {pct < 50 && (
            <Button variant="outline" size="sm" onClick={() => updateProgressPct(50)}>
              Mark 50%
            </Button>
          )}
          {pct < 75 && (
            <Button variant="outline" size="sm" onClick={() => updateProgressPct(75)}>
              Mark 75%
            </Button>
          )}
          {pct < 100 && (
            <Button size="sm" onClick={markComplete} disabled={completing} className="gap-1.5">
              {completing ? (
                <div className="size-3.5 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <CheckCircle className="size-3.5" />
              )}
              Mark Complete
            </Button>
          )}
        </div>
      </div>

      <div className="flex justify-between">
        <Button variant="outline" onClick={() => router.back()}>
          <ArrowLeft className="mr-2 size-4" /> Previous
        </Button>
        <Button onClick={() => router.push("/dashboard/lessons")}>
          Next <Play className="ml-2 size-4" />
        </Button>
      </div>
    </div>
  )
}
