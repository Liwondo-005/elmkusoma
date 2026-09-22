"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary, type AttendanceSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, Award, TrendingUp, FileText, Calendar, CheckCircle } from "lucide-react"
import Link from "next/link"

export default function LearningPassportPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [subjects, setSubjects] = useState<SubjectSummary[]>([])
  const [attendance, setAttendance] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getSubjects(user.classGroupId).catch(() => []),
      secondaryApi.getAttendance().catch(() => null),
    ])
      .then(([s, a]) => { setSubjects(s); setAttendance(a) })
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

  const totalTopics = subjects.reduce((sum, s) => sum + (s.totalLessons || 0), 0)
  const completedTopics = subjects.reduce((sum, s) => sum + (s.completedLessons || 0), 0)

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/projects" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("learningPassport")}</h1>
          <p className="text-sm text-gray-500">{t("completeLearningRecord")}</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-indigo-700 to-indigo-500 p-6 text-white">
        <h2 className="text-lg font-bold">{t("academicPassport")}</h2>
        <p className="mt-1 text-sm text-white/70">
          {t("academicPassportDesc")}
        </p>
      </div>

      <div className="space-y-4">
        {[
          {
            icon: BookOpen,
            title: t("subjectsStudied"),
            count: subjects.length,
            desc: t("allSubjectsEnrolled"),
            color: "bg-blue-50 text-blue-600",
          },
          {
            icon: TrendingUp,
            title: t("topicsCompleted"),
            count: completedTopics,
            desc: `${totalTopics} ${t("totalTopics")} · ${totalTopics > 0 ? Math.round((completedTopics / totalTopics) * 100) : 0}% ${t("progress")}`,
            color: "bg-green-50 text-green-600",
          },
          {
            icon: Award,
            title: t("attendanceRate"),
            count: attendance ? `${Math.round(attendance.attendanceRate)}%` : "—",
            desc: attendance ? `${attendance.present} ${t("daysPresent")} ${attendance.totalDays}` : t("noAttendanceData"),
            color: "bg-amber-50 text-amber-600",
          },
        ].map(section => (
          <div key={section.title} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5">
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${section.color}`}>
              <section.icon className="size-6" />
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <p className="font-semibold text-gray-900">{section.title}</p>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-500">
                  {section.count}
                </span>
              </div>
              <p className="text-sm text-gray-400">{section.desc}</p>
            </div>
          </div>
        ))}
      </div>

      {subjects.length === 0 && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Calendar className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("passportBuildsOverTime")}</h3>
          <p className="mt-1 text-sm text-gray-500">
            {t("passportFillAchievements")}
          </p>
        </div>
      )}
    </div>
  )
}
