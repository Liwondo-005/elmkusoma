"use client"

import { useEffect, useState } from "react"
import { ArrowLeft, User, MessageSquare, BookOpen, Star } from "lucide-react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryTeacherInfo } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { useTranslations } from "next-intl"

export default function MyTeacherPage() {
  const t = useTranslations("nursery")
  const { user } = useRequireAuth()
  const [teachers, setTeachers] = useState<NurseryTeacherInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState("")

  useEffect(() => {
    if (!user?.id) { setLoading(false); return }
    nurseryApi.getMyTeachers(user.id)
      .then(setTeachers)
      .catch(() => setError("Failed to load teacher information"))
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  const primaryTeacher = teachers.length > 0 ? teachers[0] : null

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label="Back to nursery dashboard">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("myTeacher")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.teacher")}</p>
        </div>
      </div>

      {primaryTeacher ? (
        <div className="nursery-card rounded-2xl bg-white p-6 text-center">
          <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-indigo-100">
            <User className="size-10 text-indigo-500" />
          </div>
          <h2 className="mt-4 text-lg font-bold text-gray-800">
            {primaryTeacher.firstName} {primaryTeacher.lastName}
          </h2>
          <p className="mt-1 text-sm text-indigo-600 font-medium">{primaryTeacher.subject || primaryTeacher.className}</p>
          <div className="mt-4 grid grid-cols-2 gap-3">
            <div className="rounded-xl bg-green-50 p-3">
              <p className="text-lg font-bold text-green-600">{t("classes")}</p>
              <p className="text-xs text-gray-500">{t("classesWithTeacher")}</p>
            </div>
            <div className="rounded-xl bg-blue-50 p-3">
              <p className="text-lg font-bold text-blue-600">{t("activities")}</p>
              <p className="text-xs text-gray-500">{t("createdForYou")}</p>
            </div>
          </div>
        </div>
      ) : (
        <div className="nursery-card rounded-2xl bg-white p-6 text-center">
          <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-indigo-100">
            <User className="size-10 text-indigo-500" />
          </div>
          <h2 className="mt-4 text-lg font-bold text-gray-800">{t("myTeacher")}</h2>
          <p className="mt-1 text-sm text-gray-500">
            {t("yourTeacherGuide")}
          </p>
        </div>
      )}

      {teachers.length > 1 && (
        <div className="nursery-card rounded-2xl bg-white p-5">
          <h3 className="font-bold text-gray-800 mb-3">{t("allMyTeachers")}</h3>
          <div className="space-y-2">
            {teachers.map(teacher => (
              <div key={teacher.id} className="flex items-center gap-3 rounded-xl bg-gray-50 p-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-indigo-100">
                  <User className="size-5 text-indigo-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">{teacher.firstName} {teacher.lastName}</p>
                  <p className="text-xs text-gray-500">{teacher.subject || teacher.className}</p>
                </div>
                <Star className="size-4 text-amber-400" />
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="nursery-card rounded-2xl bg-white p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-full bg-pink-100">
            <MessageSquare className="size-5 text-pink-500" />
          </div>
          <div>
            <p className="font-bold text-gray-800">{t("sendMessage")}</p>
            <p className="text-xs text-gray-500">{t("subtitle.teacher")}</p>
          </div>
        </div>
        <textarea
          className="mt-3 w-full rounded-xl border border-gray-200 p-3 text-sm"
          rows={3}
          placeholder={t("messagePlaceholder")}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          aria-label={t("messagePlaceholder")}
        />
        <button className="mt-2 w-full rounded-xl bg-indigo-600 py-2 text-sm font-medium text-white" aria-label={t("sendMessage")}>
          {t("sendMessage")}
        </button>
      </div>
    </div>
  )
}
