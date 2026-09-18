"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary, type AttendanceSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Briefcase, Award, BookOpen, TrendingUp, FileText } from "lucide-react"
import Link from "next/link"

export default function SecondaryPortfolioPage() {
  const { user } = useRequireAuth()
  const [subjects, setSubjects] = useState<SubjectSummary[]>([])
  const [attendance, setAttendance] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getSubjects(user.classGroupId).catch(() => []),
      secondaryApi.getAttendance().catch(() => null),
    ])
      .then(([s, a]) => { setSubjects(s); setAttendance(a) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">My Portfolio</h1>
          <p className="text-sm text-gray-500">Your academic work and achievements</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-indigo-700 to-purple-500 p-6 text-white">
        <div className="flex items-center gap-3">
          <Briefcase className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Academic Portfolio</h2>
            <p className="text-sm text-white/70">A record of your work across all subjects</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {[
          { icon: BookOpen, label: "Subjects", value: subjects.length, color: "bg-blue-50 text-blue-600" },
          { icon: TrendingUp, label: "Avg Score", value: subjects.length > 0 ? `${Math.round(subjects.reduce((s, x) => s + (x.averageScore || 0), 0) / subjects.length)}%` : "—", color: "bg-green-50 text-green-600" },
          { icon: Award, label: "Attendance", value: attendance ? `${Math.round(attendance.attendanceRate)}%` : "—", color: "bg-amber-50 text-amber-600" },
          { icon: FileText, label: "Projects", value: 0, color: "bg-purple-50 text-purple-600" },
        ].map(item => (
          <div key={item.label} className={`flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5`}>
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${item.color}`}>
              <item.icon className="size-6" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900">{item.value}</p>
              <p className="text-sm text-gray-400">{item.label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <Briefcase className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">Your portfolio grows with you</h3>
        <p className="mt-1 text-sm text-gray-500">
          As you complete assignments, projects, and assessments, your portfolio will showcase your academic journey.
        </p>
      </div>
    </div>
  )
}
