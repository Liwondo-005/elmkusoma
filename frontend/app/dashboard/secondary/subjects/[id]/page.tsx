"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, ChevronRight, PenTool, Award, Calendar, Video, FileText, MessageSquare } from "lucide-react"
import Link from "next/link"

interface Lesson {
  id: string
  title: string
  description?: string
  sortOrder: number
  isPublished: boolean
}

interface Assignment {
  id: string
  title: string
  subjectName?: string
  dueDate?: string
  status: string
  totalMarks: number
}

interface Assessment {
  id: string
  title: string
  subjectName?: string
  scheduledAt?: string
  totalMarks: number
  status: string
}

export default function SubjectWorkspacePage({ params }: { params: { id: string } }) {
  const { user } = useRequireAuth()
  const [subject, setSubject] = useState<SubjectSummary | null>(null)
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [activeTab, setActiveTab] = useState<"topics" | "practice" | "assessments" | "feedback">("topics")
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getSubject(params.id),
      secondaryApi.getLessonsBySubject(params.id, user.classGroupId).catch(() => []),
      secondaryApi.getAssignments(user.classGroupId).catch(() => []),
      secondaryApi.getAssessments(user.classGroupId).catch(() => []),
    ])
      .then(([s, l, a, as]) => {
        setSubject(s)
        setLessons(l || [])
        setAssignments((a || []).filter(x => x.subjectName === s?.name))
        setAssessments((as || []).filter(x => x.subjectName === s?.name))
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user, params.id])

  if (loading) return <LoadingState />

  const tabs = [
    { id: "topics" as const, label: "Topics", icon: BookOpen },
    { id: "practice" as const, label: "Practice", icon: PenTool },
    { id: "assessments" as const, label: "Assessments", icon: Award },
    { id: "feedback" as const, label: "Feedback", icon: MessageSquare },
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{subject?.name || "Subject"}</h1>
          <p className="text-sm text-gray-500">
            {lessons.length} topic{lessons.length !== 1 ? "s" : ""}
            {subject?.averageScore !== undefined && ` · Average ${subject.averageScore}%`}
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 rounded-xl bg-gray-100 p-1">
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-sm font-medium transition-colors ${
              activeTab === tab.id ? "bg-white text-indigo-600 shadow-sm" : "text-gray-500 hover:text-gray-700"
            }`}
          >
            <tab.icon className="size-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === "topics" && (
        <div className="space-y-2">
          {lessons.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <BookOpen className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No topics yet</h3>
              <p className="mt-1 text-sm text-gray-500">Your teacher will add topics soon.</p>
            </div>
          ) : (
            lessons.map((lesson, idx) => (
              <Link
                key={lesson.id}
                href={`/dashboard/secondary/subjects/${params.id}/topics/${lesson.id}`}
                className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-gray-100 text-sm font-bold text-gray-500">
                  {idx + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lesson.title}</p>
                  {lesson.description && (
                    <p className="mt-0.5 text-xs text-gray-400 line-clamp-1">{lesson.description}</p>
                  )}
                </div>
                <ChevronRight className="size-5 shrink-0 text-gray-300" />
              </Link>
            ))
          )}
        </div>
      )}

      {activeTab === "practice" && (
        <div className="space-y-2">
          {assignments.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <PenTool className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No assignments</h3>
              <p className="mt-1 text-sm text-gray-500">Your teacher will assign practice work soon.</p>
            </div>
          ) : (
            assignments.map(a => (
              <div key={a.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-50">
                  <FileText className="size-5 text-amber-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{a.title}</p>
                  <p className="text-xs text-gray-400">
                    {a.dueDate && `Due ${new Date(a.dueDate).toLocaleDateString()}`}
                    {` · ${a.totalMarks} marks`}
                  </p>
                </div>
                <span className="rounded-full bg-amber-100 px-2.5 py-1 text-[10px] font-bold text-amber-700">{a.status}</span>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "assessments" && (
        <div className="space-y-2">
          {assessments.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <Award className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No assessments</h3>
              <p className="mt-1 text-sm text-gray-500">Your teacher will schedule assessments soon.</p>
            </div>
          ) : (
            assessments.map(a => (
              <Link
                key={a.id}
                href={`/dashboard/assessments/${a.id}`}
                className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                  <Award className="size-5 text-indigo-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{a.title}</p>
                  <p className="text-xs text-gray-400">{a.totalMarks} marks</p>
                </div>
                {a.scheduledAt && (
                  <p className="text-xs text-gray-400">{new Date(a.scheduledAt).toLocaleDateString()}</p>
                )}
              </Link>
            ))
          )}
        </div>
      )}

      {activeTab === "feedback" && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <MessageSquare className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">Teacher Feedback</h3>
          <p className="mt-1 text-sm text-gray-500">Feedback from your teacher on this subject will appear here.</p>
        </div>
      )}
    </div>
  )
}
