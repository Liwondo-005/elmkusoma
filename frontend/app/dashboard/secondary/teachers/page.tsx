"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Users, MessageSquare, Star } from "lucide-react"
import Link from "next/link"

interface Teacher {
  id: string
  name: string
  subject?: string
}

interface Feedback {
  id: string
  teacherName: string
  subjectName?: string
  message: string
  createdAt: string
}

export default function SecondaryTeachersPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [feedback, setFeedback] = useState<Feedback[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    const token = localStorage.getItem("elmkusoma_access_token")
    const instId = localStorage.getItem("elmkusoma_institution_id") || ""
    const headers = { "Authorization": `Bearer ${token}`, "X-Institution-Id": instId }

    Promise.all([
      fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/v1/academic/class-groups/${user.classGroupId}`, { headers })
        .then(r => r.json()).then(d => d.data?.teachers || []).catch(() => []),
      fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/v1/student/dashboard/feedback`, { headers })
        .then(r => r.json()).then(d => d.data || []).catch(() => []),
    ])
      .then(([t, f]) => { setTeachers(t); setFeedback(f) })
      .catch(() => setError(t("errorLoading")))
      .finally(() => setLoading(false))
  }, [user])

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

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("myTeachers")}</h1>
          <p className="text-sm text-gray-500">{t("subjectTeachersAndFeedback")}</p>
        </div>
      </div>

      {teachers.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">{t("yourTeachers")}</h2>
          <div className="space-y-2">
            {teachers.map(teacher => (
              <div key={teacher.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                  <Users className="size-6 text-indigo-600" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900">{teacher.name}</p>
                  {teacher.subject && <p className="text-xs text-gray-400">{teacher.subject}</p>}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {feedback.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">{t("recentFeedback")}</h2>
          <div className="space-y-3">
            {feedback.map(f => (
              <div key={f.id} className="rounded-2xl border border-gray-100 bg-white p-5">
                <div className="flex items-center gap-2">
                  <MessageSquare className="size-4 text-indigo-500" />
                  <p className="text-sm font-semibold text-gray-900">{f.teacherName}</p>
                  {f.subjectName && <span className="text-xs text-gray-400">· {f.subjectName}</span>}
                </div>
                <p className="mt-2 text-sm text-gray-600">{f.message}</p>
                <p className="mt-2 text-[10px] text-gray-400">{new Date(f.createdAt).toLocaleDateString()}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      {teachers.length === 0 && feedback.length === 0 && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Users className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("yourTeachers")}</h3>
          <p className="mt-1 text-sm text-gray-500">
            {t("teachersInfoWillAppear")}
          </p>
        </div>
      )}
    </div>
  )
}
