"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type UpcomingAssessment, type SubjectSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Clock, FileText, AlertCircle, CheckCircle, Target } from "lucide-react"
import Link from "next/link"

export default function ExamPrepPage() {
  const { user } = useRequireAuth()
  const [assessments, setAssessments] = useState<UpcomingAssessment[]>([])
  const [subjects, setSubjects] = useState<SubjectSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getAssessments(user.classGroupId),
      secondaryApi.getSubjects(user.classGroupId).catch(() => []),
    ])
      .then(([a, s]) => { setAssessments(a || []); setSubjects(s || []) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const upcoming = assessments.filter(a => a.status === "SCHEDULED" || a.status === "PUBLISHED")
    .sort((a, b) => (a.scheduledAt || "").localeCompare(b.scheduledAt || ""))

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/revision" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Exam Prep</h1>
          <p className="text-sm text-gray-500">Prepare for upcoming assessments</p>
        </div>
      </div>

      {/* Readiness Overview */}
      <div className="rounded-2xl bg-gradient-to-r from-amber-500 to-orange-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Target className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Assessment Readiness</h2>
            <p className="text-sm text-white/70">
              {upcoming.length === 0 ? "No upcoming assessments" :
               `${upcoming.length} assessment${upcoming.length > 1 ? "s" : ""} to prepare for`}
            </p>
          </div>
        </div>
      </div>

      {/* Upcoming Assessments */}
      {upcoming.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Upcoming Assessments</h2>
          <div className="space-y-3">
            {upcoming.map(a => {
              const daysUntil = a.scheduledAt
                ? Math.ceil((new Date(a.scheduledAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24))
                : null

              return (
                <div key={a.id} className="rounded-2xl border border-gray-100 bg-white p-5">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="font-semibold text-gray-900">{a.title}</p>
                      <p className="mt-1 text-sm text-gray-500">{a.subjectName || "General"}</p>
                      <div className="mt-2 flex flex-wrap gap-2 text-xs text-gray-400">
                        <span>{a.totalMarks} marks</span>
                        {a.timeLimitMinutes && <span>· {a.timeLimitMinutes} min</span>}
                      </div>
                    </div>
                    {daysUntil !== null && (
                      <div className={`rounded-xl px-3 py-2 text-center ${
                        daysUntil <= 3 ? "bg-red-50" : daysUntil <= 7 ? "bg-amber-50" : "bg-green-50"
                      }`}>
                        <p className={`text-lg font-bold ${
                          daysUntil <= 3 ? "text-red-600" : daysUntil <= 7 ? "text-amber-600" : "text-green-600"
                        }`}>
                          {daysUntil === 0 ? "Today" : daysUntil === 1 ? "Tomorrow" : `${daysUntil}d`}
                        </p>
                        <p className="text-[10px] text-gray-400">
                          {new Date(a.scheduledAt!).toLocaleDateString([], { month: "short", day: "numeric" })}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Preparation Checklist */}
                  <div className="mt-4 space-y-2">
                    {[
                      { label: "Review topic notes", done: false },
                      { label: "Complete practice exercises", done: false },
                      { label: "Revise key formulas/concepts", done: false },
                      { label: "Attempt past questions", done: false },
                    ].map(item => (
                      <div key={item.label} className="flex items-center gap-2 text-sm text-gray-600">
                        <div className="size-4 rounded border border-gray-300" />
                        <span>{item.label}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        </section>
      )}

      {/* Subject-wise Prep */}
      {subjects.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Subject Focus</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {subjects.map(s => (
              <Link
                key={s.id}
                href={`/dashboard/secondary/subjects/${s.id}`}
                className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                  <FileText className="size-5 text-indigo-600" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900">{s.name}</p>
                  {s.upcomingAssessments !== undefined && s.upcomingAssessments > 0 && (
                    <p className="text-xs text-amber-600">{s.upcomingAssessments} assessment{s.upcomingAssessments > 1 ? "s" : ""}</p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {upcoming.length === 0 && subjects.length === 0 && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <CheckCircle className="mx-auto size-12 text-green-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">All clear!</h3>
          <p className="mt-1 text-sm text-gray-500">No upcoming assessments. Keep up the good work.</p>
        </div>
      )}
    </div>
  )
}
