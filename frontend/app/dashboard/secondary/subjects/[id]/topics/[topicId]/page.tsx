"use client"

import { useEffect, useState, useCallback } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, CheckCircle, ChevronLeft, ChevronRight, Clock, Save } from "lucide-react"
import Link from "next/link"

interface LessonDetail {
  id: string
  subjectId: string
  classGroupId: string
  title: string
  description?: string
  contentText?: string
  videoUrl?: string
  fileAttachments?: string
  sortOrder: number
  isPublished: boolean
}

interface AllLesson {
  id: string
  title: string
  sortOrder: number
}

export default function TopicWorkspacePage({ params }: { params: { id: string; topicId: string } }) {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [lesson, setLesson] = useState<LessonDetail | null>(null)
  const [allLessons, setAllLessons] = useState<AllLesson[]>([])
  const [progress, setProgress] = useState(0)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getLesson(params.topicId),
      secondaryApi.getLessonsBySubject(params.id, user.classGroupId),
      secondaryApi.getLessonProgress(user.id).catch(() => []),
    ])
      .then(([l, lessons, prog]) => {
        setLesson(l)
        setAllLessons(lessons || [])
        const p = prog?.find((x: any) => x.lessonId === params.topicId)
        setProgress(p?.completionPercentage || 0)
      })
      .catch(() => setError(t("errorLoading")))
      .finally(() => setLoading(false))
  }, [user, params.id, params.topicId])

  const saveProgress = useCallback(async (pct: number) => {
    setSaving(true)
    try {
      await secondaryApi.updateLessonProgress(params.topicId, pct)
      setProgress(pct)
    } catch {}
    setSaving(false)
  }, [params.topicId])

  const currentIdx = allLessons.findIndex(l => l.id === params.topicId)
  const prevLesson = currentIdx > 0 ? allLessons[currentIdx - 1] : null
  const nextLesson = currentIdx < allLessons.length - 1 ? allLessons[currentIdx + 1] : null

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-5xl p-4 pb-24" role="main">
        <div className="rounded-2xl border border-red-100 bg-red-50 p-8 text-center">
          <p className="text-sm text-red-600">{error}</p>
          <button
            onClick={() => { setError(null); setLoading(true); }}
            className="mt-3 rounded-xl bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
            aria-label={tc("retry")}
          >
            {tc("retry")}
          </button>
        </div>
      </div>
    )
  }

  if (!lesson) {
    return (
      <div className="mx-auto max-w-5xl p-4" role="main">
        <p className="text-gray-500">{t("topicNotFound")}</p>
        <Link href={`/dashboard/secondary/subjects/${params.id}`} className="mt-2 text-indigo-600 underline" aria-label={t("backToSubject")}>
          {t("backToSubject")}
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href={`/dashboard/secondary/subjects/${params.id}`} className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div className="flex-1 min-w-0">
          <h1 className="text-lg font-bold text-gray-900 truncate">{lesson.title}</h1>
          <p className="text-sm text-gray-500">{t("topic")} {currentIdx + 1} {t("of")} {allLessons.length}</p>
        </div>
        {progress >= 100 ? (
          <span className="flex items-center gap-1 rounded-full bg-green-100 px-3 py-1 text-xs font-bold text-green-700">
            <CheckCircle className="size-3.5" /> {t("completed")}
          </span>
        ) : progress > 0 ? (
          <span className="flex items-center gap-1 rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700">
            <Clock className="size-3.5" /> {t("inProgress")}
          </span>
        ) : null}
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-4">
        <div className="flex items-center justify-between text-sm">
          <span className="font-medium text-gray-700">{t("yourProgress")}</span>
          <span className="text-gray-400">{Math.round(progress)}%</span>
        </div>
        <div
          className="mt-2 h-2 overflow-hidden rounded-full bg-gray-100"
          role="progressbar"
          aria-valuenow={progress}
          aria-valuemin={0}
          aria-valuemax={100}
        >
          <div
            className={`h-full rounded-full transition-all ${progress >= 100 ? "bg-green-500" : "bg-indigo-500"}`}
            style={{ width: `${progress}%` }}
          />
        </div>
        <div className="mt-3 flex gap-2">
          {[0, 25, 50, 75, 100].map(p => (
            <button
              key={p}
              onClick={() => saveProgress(p)}
              disabled={saving}
              className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                progress === p
                  ? "bg-indigo-100 text-indigo-700"
                  : "bg-gray-50 text-gray-500 hover:bg-gray-100"
              }`}
              aria-label={`${t("setProgress")} ${p === 0 ? t("notStarted") : p === 100 ? t("done") : `${p}%`}`}
            >
              {p === 0 ? t("notStarted") : p === 100 ? t("done") : `${p}%`}
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-6">
        {lesson.description && (
          <p className="mb-4 text-sm text-gray-500 italic">{lesson.description}</p>
        )}

        {lesson.contentText ? (
          <div className="prose prose-sm max-w-none text-gray-700">
            {lesson.contentText.split("\n").map((para, i) => (
              <p key={i} className="mb-3 leading-relaxed">{para}</p>
            ))}
          </div>
        ) : (
          <div className="py-8 text-center">
            <BookOpen className="mx-auto size-12 text-gray-200" />
            <p className="mt-3 text-sm text-gray-400">{t("contentWillBeAdded")}</p>
          </div>
        )}

        {lesson.videoUrl && (
          <div className="mt-6">
            <p className="mb-2 text-sm font-semibold text-gray-700">{t("video")}</p>
            <a
              href={lesson.videoUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-xl bg-indigo-50 px-4 py-2 text-sm font-medium text-indigo-600 hover:bg-indigo-100"
              aria-label={t("watchVideo")}
            >
              {t("watchVideo")} <ChevronRight className="size-4" />
            </a>
          </div>
        )}

        {lesson.fileAttachments && (
          <div className="mt-6">
            <p className="mb-2 text-sm font-semibold text-gray-700">{t("attachments")}</p>
            <a
              href={lesson.fileAttachments}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-xl bg-gray-50 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
              aria-label={t("viewFile")}
            >
              {t("viewFile")} <ChevronRight className="size-4" />
            </a>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between">
        {prevLesson ? (
          <Link
            href={`/dashboard/secondary/subjects/${params.id}/topics/${prevLesson.id}`}
            className="flex items-center gap-2 rounded-xl bg-gray-100 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-200"
            aria-label={`${t("previousTopic")} - ${prevLesson.title}`}
          >
            <ChevronLeft className="size-4" /> {prevLesson.title}
          </Link>
        ) : <div />}
        {nextLesson ? (
          <Link
            href={`/dashboard/secondary/subjects/${params.id}/topics/${nextLesson.id}`}
            className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-indigo-700"
            aria-label={`${t("nextTopic")} - ${nextLesson.title}`}
          >
            {nextLesson.title} <ChevronRight className="size-4" />
          </Link>
        ) : (
          <Link
            href={`/dashboard/secondary/subjects/${params.id}`}
            className="flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-green-700"
            aria-label={t("backToSubject")}
          >
            <CheckCircle className="size-4" /> {t("backToSubject")}
          </Link>
        )}
      </div>
    </div>
  )
}
