"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { learningApi, type Lesson, type LessonProgress } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { BookOpen, Clock, CheckCircle, Play, ArrowRight } from "lucide-react"

export default function LessonsPage() {
  const { user } = useRequireAuth()
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [progress, setProgress] = useState<LessonProgress[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [lessonsData, progressData] = await Promise.all([
        learningApi.getLessonsByClass(user!.classGroupId || ""),
        learningApi.getStudentProgress(user!.id).catch(() => []),
      ])
      setLessons(lessonsData)
      setProgress(progressData)
    } catch {
      setLessons([])
    } finally {
      setLoading(false)
    }
  }

  function getLessonProgress(lessonId: string): number {
    const p = progress.find((pr) => pr.lessonId === lessonId)
    return p?.completionPercentage || 0
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Lessons</h1>
        <p className="text-sm text-muted-foreground">Browse and complete lessons to track your learning progress.</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : lessons.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Lessons Available</h3>
          <p className="mt-2 text-sm text-muted-foreground">Lessons will appear here once your teacher publishes them.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {lessons.map((lesson) => {
            const pct = getLessonProgress(lesson.id)
            return (
              <Link
                key={lesson.id}
                href={`/dashboard/lessons/${lesson.id}`}
                className="group rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start justify-between">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    {pct >= 100 ? (
                      <CheckCircle className="size-5 text-primary" />
                    ) : pct > 0 ? (
                      <Clock className="size-5 text-primary" />
                    ) : (
                      <Play className="size-5 text-primary" />
                    )}
                  </div>
                  <ArrowRight className="size-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground line-clamp-1">{lesson.title}</h3>
                {lesson.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{lesson.description}</p>
                )}
                <div className="mt-3">
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span>{pct >= 100 ? "Completed" : pct > 0 ? "In Progress" : "Not Started"}</span>
                    <span>{Math.round(pct)}%</span>
                  </div>
                  <div className="mt-1.5 h-1.5 w-full rounded-full bg-muted overflow-hidden">
                    <div
                      className="h-full rounded-full bg-primary transition-all"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
