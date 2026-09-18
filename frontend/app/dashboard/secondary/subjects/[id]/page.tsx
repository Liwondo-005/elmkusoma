"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, ChevronRight, FileText, Video, CheckCircle, Clock } from "lucide-react"
import Link from "next/link"

interface Lesson {
  id: string
  title: string
  description?: string
  sortOrder: number
  isPublished: boolean
  createdAt: string
}

interface LessonProgress {
  lessonId: string
  completionPercentage: number
}

export default function SubjectWorkspacePage({ params }: { params: { id: string } }) {
  const { user } = useRequireAuth()
  const [subject, setSubject] = useState<SubjectSummary | null>(null)
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [progress, setProgress] = useState<LessonProgress[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getSubject(params.id),
      secondaryApi.getLessonsBySubject(params.id, user.classGroupId),
      secondaryApi.getLessonProgress(user.id).catch(() => []),
    ])
      .then(([s, l, p]) => {
        setSubject(s)
        setLessons(l || [])
        setProgress(p || [])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user, params.id])

  function getLessonProgress(lessonId: string): number {
    const p = progress.find(x => x.lessonId === lessonId)
    return p?.completionPercentage || 0
  }

  if (loading) return <LoadingState />

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{subject?.name || "Subject"}</h1>
          <p className="text-sm text-gray-500">
            {lessons.length} topic{lessons.length !== 1 ? "s" : ""}
            {subject?.averageScore !== undefined && ` · Average ${subject.averageScore}%`}
          </p>
        </div>
      </div>

      {/* Subject Overview */}
      {subject && (
        <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-blue-500 p-5 text-white">
          <h2 className="text-lg font-bold">{subject.name}</h2>
          {subject.description && (
            <p className="mt-1 text-sm text-white/70">{subject.description}</p>
          )}
          <div className="mt-3 flex gap-4 text-sm">
            {subject.totalLessons !== undefined && (
              <span className="text-white/70">{subject.totalLessons} topics</span>
            )}
            {subject.completedLessons !== undefined && (
              <span className="text-white/70">{subject.completedLessons} done</span>
            )}
            {subject.upcomingAssessments !== undefined && subject.upcomingAssessments > 0 && (
              <span className="text-white/70">{subject.upcomingAssessments} upcoming</span>
            )}
          </div>
        </div>
      )}

      {/* Topics List */}
      {lessons.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <BookOpen className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No topics yet</h3>
          <p className="mt-1 text-sm text-gray-500">Your teacher will add topics soon.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {lessons.map((lesson, idx) => {
            const pct = getLessonProgress(lesson.id)
            const isComplete = pct >= 100
            const isStarted = pct > 0

            return (
              <Link
                key={lesson.id}
                href={`/dashboard/secondary/subjects/${params.id}/topics/${lesson.id}`}
                className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
              >
                <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl text-sm font-bold ${
                  isComplete ? "bg-green-100 text-green-600" :
                  isStarted ? "bg-amber-100 text-amber-600" :
                  "bg-gray-100 text-gray-500"
                }`}>
                  {isComplete ? <CheckCircle className="size-5" /> : idx + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lesson.title}</p>
                  {lesson.description && (
                    <p className="mt-0.5 text-xs text-gray-400 line-clamp-1">{lesson.description}</p>
                  )}
                  {pct > 0 && (
                    <div className="mt-2 flex items-center gap-2">
                      <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-gray-100">
                        <div
                          className={`h-full rounded-full ${isComplete ? "bg-green-500" : "bg-indigo-500"}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className="text-[10px] font-semibold text-gray-400">{Math.round(pct)}%</span>
                    </div>
                  )}
                </div>
                <ChevronRight className="size-5 shrink-0 text-gray-300" />
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
