"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary, type AttendanceSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, Award, TrendingUp, FileText, Calendar, CheckCircle } from "lucide-react"
import Link from "next/link"

export default function LearningPassportPage() {
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

  const totalTopics = subjects.reduce((sum, s) => sum + (s.totalLessons || 0), 0)
  const completedTopics = subjects.reduce((sum, s) => sum + (s.completedLessons || 0), 0)

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/projects" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Learning Passport</h1>
          <p className="text-sm text-gray-500">Your complete learning record</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-indigo-700 to-indigo-500 p-6 text-white">
        <h2 className="text-lg font-bold">Academic Passport</h2>
        <p className="mt-1 text-sm text-white/70">
          A record of everything you have accomplished during your secondary education.
        </p>
      </div>

      <div className="space-y-4">
        {[
          {
            icon: BookOpen,
            title: "Subjects Studied",
            count: subjects.length,
            desc: "All subjects you are enrolled in",
            color: "bg-blue-50 text-blue-600",
          },
          {
            icon: TrendingUp,
            title: "Topics Completed",
            count: completedTopics,
            desc: `${totalTopics} total topics · ${totalTopics > 0 ? Math.round((completedTopics / totalTopics) * 100) : 0}% progress`,
            color: "bg-green-50 text-green-600",
          },
          {
            icon: Award,
            title: "Attendance Rate",
            count: attendance ? `${Math.round(attendance.attendanceRate)}%` : "—",
            desc: attendance ? `${attendance.present} days present out of ${attendance.totalDays}` : "No attendance data yet",
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
          <h3 className="mt-3 text-lg font-bold text-gray-800">Your passport builds over time</h3>
          <p className="mt-1 text-sm text-gray-500">
            As you complete subjects, assessments, and projects, your Learning Passport will fill with your achievements.
          </p>
        </div>
      )}
    </div>
  )
}
