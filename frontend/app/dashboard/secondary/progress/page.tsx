"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type AttendanceSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { BarChart3, ArrowLeft, BookOpen, Award, Clock, CheckCircle } from "lucide-react"
import Link from "next/link"

export default function SecondaryProgressPage() {
  const { user } = useRequireAuth()
  const [attendance, setAttendance] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    secondaryApi.getAttendance()
      .then(setAttendance)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">My Progress</h1>
          <p className="text-sm text-gray-500">Track your academic progress</p>
        </div>
      </div>

      {/* Attendance Overview */}
      {attendance && (
        <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Attendance</h2>
          <div className="mt-3 grid grid-cols-4 gap-3">
            <div className="rounded-xl bg-green-50 p-3 text-center">
              <p className="text-xl font-bold text-green-600">{attendance.present}</p>
              <p className="text-[10px] font-medium text-green-700">Present</p>
            </div>
            <div className="rounded-xl bg-red-50 p-3 text-center">
              <p className="text-xl font-bold text-red-600">{attendance.absent}</p>
              <p className="text-[10px] font-medium text-red-700">Absent</p>
            </div>
            <div className="rounded-xl bg-amber-50 p-3 text-center">
              <p className="text-xl font-bold text-amber-600">{attendance.late}</p>
              <p className="text-[10px] font-medium text-amber-700">Late</p>
            </div>
            <div className="rounded-xl bg-blue-50 p-3 text-center">
              <p className="text-xl font-bold text-blue-600">{attendance.attendanceRate > 0 ? `${Math.round(attendance.attendanceRate)}%` : "—"}</p>
              <p className="text-[10px] font-medium text-blue-700">Rate</p>
            </div>
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <BarChart3 className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">Detailed Progress</h3>
        <p className="mt-1 text-sm text-gray-500">
          Subject-by-subject progress, assessment results, and improvement tracking will appear here as you complete learning activities.
        </p>
      </div>
    </div>
  )
}
