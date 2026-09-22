"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type AttendanceSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { BarChart3, ArrowLeft, BookOpen, Award, Clock, CheckCircle } from "lucide-react"
import Link from "next/link"

export default function SecondaryProgressPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [attendance, setAttendance] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    secondaryApi.getAttendance()
      .then(setAttendance)
      .catch(() => setError(t("errorLoading")))
      .finally(() => setLoading(false))
  }, [])

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
          <h1 className="text-xl font-bold text-gray-900">{t("myProgress")}</h1>
          <p className="text-sm text-gray-500">{t("trackAcademicProgress")}</p>
        </div>
      </div>

      {attendance && (
        <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">{t("attendance")}</h2>
          <div className="mt-3 grid grid-cols-4 gap-3">
            <div className="rounded-xl bg-green-50 p-3 text-center">
              <p className="text-xl font-bold text-green-600">{attendance.present}</p>
              <p className="text-[10px] font-medium text-green-700">{t("present")}</p>
            </div>
            <div className="rounded-xl bg-red-50 p-3 text-center">
              <p className="text-xl font-bold text-red-600">{attendance.absent}</p>
              <p className="text-[10px] font-medium text-red-700">{t("absent")}</p>
            </div>
            <div className="rounded-xl bg-amber-50 p-3 text-center">
              <p className="text-xl font-bold text-amber-600">{attendance.late}</p>
              <p className="text-[10px] font-medium text-amber-700">{t("late")}</p>
            </div>
            <div className="rounded-xl bg-blue-50 p-3 text-center">
              <p className="text-xl font-bold text-blue-600">{attendance.attendanceRate > 0 ? `${Math.round(attendance.attendanceRate)}%` : "—"}</p>
              <p className="text-[10px] font-medium text-blue-700">{t("rate")}</p>
            </div>
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <BarChart3 className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">{t("detailedProgress")}</h3>
        <p className="mt-1 text-sm text-gray-500">
          {t("detailedProgressDesc")}
        </p>
      </div>
    </div>
  )
}
