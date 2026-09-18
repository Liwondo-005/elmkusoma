"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type AssignmentSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { PenTool, ArrowLeft, FileText, Clock, CheckCircle } from "lucide-react"
import Link from "next/link"

export default function SecondaryPracticePage() {
  const { user } = useRequireAuth()
  const [assignments, setAssignments] = useState<AssignmentSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    secondaryApi.getAssignments(user.classGroupId)
      .then(setAssignments)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const pending = assignments.filter(a => a.status === "PENDING" || a.status === "ACTIVE")
  const submitted = assignments.filter(a => a.status === "SUBMITTED" || a.status === "GRADED")

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Practice</h1>
          <p className="text-sm text-gray-500">Complete assignments and practice exercises</p>
        </div>
      </div>

      {assignments.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <PenTool className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No practice work yet</h3>
          <p className="mt-1 text-sm text-gray-500">Your teacher will assign practice work soon.</p>
        </div>
      ) : (
        <>
          {pending.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">To Do</h2>
              <div className="space-y-2">
                {pending.map(a => (
                  <div key={a.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-50">
                      <FileText className="size-5 text-amber-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-900">{a.title}</p>
                      <p className="text-xs text-gray-400">
                        {a.subjectName && `${a.subjectName} · `}
                        {a.dueDate && `Due ${new Date(a.dueDate).toLocaleDateString()}`}
                      </p>
                    </div>
                    <span className="rounded-full bg-amber-100 px-2.5 py-1 text-[10px] font-bold text-amber-700">
                      {a.status}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          )}
          {submitted.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Completed</h2>
              <div className="space-y-2">
                {submitted.map(a => (
                  <div key={a.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 opacity-70">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-green-50">
                      <CheckCircle className="size-5 text-green-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-700">{a.title}</p>
                      <p className="text-xs text-gray-400">
                        {a.subjectName && `${a.subjectName} · `}
                        {a.grade !== undefined ? `Grade: ${a.grade}/${a.totalMarks}` : a.status}
                      </p>
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
