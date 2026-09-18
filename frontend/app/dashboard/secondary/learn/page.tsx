"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { BookOpen, ArrowLeft, ChevronRight, FileText, Video, Award, TrendingUp } from "lucide-react"
import Link from "next/link"

export default function SecondaryLearnPage() {
  const { user } = useRequireAuth()
  const [subjects, setSubjects] = useState<SubjectSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    secondaryApi.getSubjects(user.classGroupId)
      .then(setSubjects)
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
          <h1 className="text-xl font-bold text-gray-900">Learn</h1>
          <p className="text-sm text-gray-500">Explore your subjects and topics</p>
        </div>
      </div>

      {subjects.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <BookOpen className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No subjects available</h3>
          <p className="mt-1 text-sm text-gray-500">Your subjects will appear here once your teacher sets them up.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {subjects.map(subject => (
            <Link
              key={subject.id}
              href={`/dashboard/courses/${subject.id}`}
              className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 transition-all hover:border-indigo-200 hover:shadow-sm"
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                <BookOpen className="size-6 text-indigo-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{subject.name}</p>
                <div className="mt-1 flex flex-wrap gap-2 text-xs text-gray-400">
                  {subject.totalLessons && <span>{subject.totalLessons} topics</span>}
                  {subject.completedLessons !== undefined && <span>· {subject.completedLessons} done</span>}
                  {subject.upcomingAssessments !== undefined && subject.upcomingAssessments > 0 && (
                    <span className="text-amber-600">· {subject.upcomingAssessments} assessment{subject.upcomingAssessments > 1 ? "s" : ""}</span>
                  )}
                </div>
              </div>
              <ChevronRight className="size-5 shrink-0 text-gray-300" />
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
