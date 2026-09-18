"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type UpcomingAssessment } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { Award, ArrowLeft, Clock, FileText, AlertCircle } from "lucide-react"
import Link from "next/link"

export default function SecondaryAssessPage() {
  const { user } = useRequireAuth()
  const [assessments, setAssessments] = useState<UpcomingAssessment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    secondaryApi.getAssessments(user.classGroupId)
      .then(setAssessments)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const upcoming = assessments.filter(a => a.status === "SCHEDULED" || a.status === "PUBLISHED")
  const past = assessments.filter(a => a.status === "COMPLETED" || a.status === "ENDED")

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Assess</h1>
          <p className="text-sm text-gray-500">Tests, quizzes, and examinations</p>
        </div>
      </div>

      {assessments.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Award className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No assessments yet</h3>
          <p className="mt-1 text-sm text-gray-500">Your teacher will schedule assessments soon.</p>
        </div>
      ) : (
        <>
          {upcoming.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Upcoming</h2>
              <div className="space-y-2">
                {upcoming.map(a => (
                  <Link
                    key={a.id}
                    href={`/dashboard/assessments/${a.id}`}
                    className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
                  >
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-50">
                      <FileText className="size-5 text-amber-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-900">{a.title}</p>
                      <p className="text-xs text-gray-400">
                        {a.subjectName && `${a.subjectName} · `}
                        {a.totalMarks} marks
                        {a.timeLimitMinutes && ` · ${a.timeLimitMinutes} min`}
                      </p>
                    </div>
                    {a.scheduledAt && (
                      <div className="text-right">
                        <p className="text-xs font-medium text-gray-500">
                          {new Date(a.scheduledAt).toLocaleDateString()}
                        </p>
                        <p className="text-[10px] text-gray-400">
                          {new Date(a.scheduledAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                        </p>
                      </div>
                    )}
                  </Link>
                ))}
              </div>
            </section>
          )}
          {past.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Completed</h2>
              <div className="space-y-2">
                {past.map(a => (
                  <div key={a.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 opacity-70">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-green-50">
                      <Award className="size-5 text-green-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-700">{a.title}</p>
                      <p className="text-xs text-gray-400">{a.subjectName} · {a.totalMarks} marks</p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
