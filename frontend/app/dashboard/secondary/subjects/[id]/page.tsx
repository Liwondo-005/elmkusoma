"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary, type LiveClassSummary, type TeacherFeedback } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, ChevronRight, PenTool, Award, Calendar, Video, FileText, MessageSquare, Library, BarChart3, Clock, CheckCircle, Play } from "lucide-react"
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
  const [liveClasses, setLiveClasses] = useState<LiveClassSummary[]>([])
  const [feedback, setFeedback] = useState<TeacherFeedback[]>([])
  const [activeTab, setActiveTab] = useState<"overview" | "topics" | "practice" | "assessments" | "resources" | "live" | "replays" | "progress" | "feedback">("overview")
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getSubject(params.id),
      secondaryApi.getLessonsBySubject(params.id, user.classGroupId).catch(() => []),
      secondaryApi.getAssignments(user.classGroupId).catch(() => []),
      secondaryApi.getAssessments(user.classGroupId).catch(() => []),
      secondaryApi.getLiveClasses().catch(() => []),
      secondaryApi.getTeacherFeedback(params.id).catch(() => []),
    ])
      .then(([s, l, a, as, lc, fb]) => {
        setSubject(s)
        setLessons(l || [])
        setAssignments((a || []).filter(x => x.subjectName === s?.name))
        setAssessments((as || []).filter(x => x.subjectName === s?.name))
        setLiveClasses((lc || []).filter(x => x.subjectName === s?.name))
        setFeedback(fb || [])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user, params.id])

  if (loading) return <LoadingState />

  const tabs = [
    { id: "overview" as const, label: "Overview", icon: BookOpen },
    { id: "topics" as const, label: "Topics", icon: BookOpen },
    { id: "practice" as const, label: "Practice", icon: PenTool },
    { id: "assessments" as const, label: "Assessments", icon: Award },
    { id: "resources" as const, label: "Resources", icon: Library },
    { id: "live" as const, label: "Live", icon: Video },
    { id: "replays" as const, label: "Replays", icon: Play },
    { id: "progress" as const, label: "Progress", icon: BarChart3 },
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
        <div className="space-y-3">
          {feedback.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <MessageSquare className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No feedback yet</h3>
              <p className="mt-1 text-sm text-gray-500">Your teacher will provide feedback on your work here.</p>
            </div>
          ) : (
            feedback.map(f => (
              <div key={f.id} className="rounded-2xl border border-gray-100 bg-white p-5">
                <div className="flex items-center gap-2">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-600">{f.teacherName?.charAt(0) || "T"}</div>
                  <div>
                    <p className="text-sm font-semibold text-gray-900">{f.teacherName}</p>
                    <p className="text-[10px] text-gray-400">{new Date(f.createdAt).toLocaleDateString()}</p>
                  </div>
                </div>
                <p className="mt-3 text-sm text-gray-600">{f.message}</p>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "overview" && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-gray-100 bg-white p-5">
            <h3 className="font-semibold text-gray-900">{subject?.name}</h3>
            <p className="mt-1 text-sm text-gray-500">{subject?.description || "No description available."}</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-indigo-600">{lessons.length}</p>
              <p className="text-xs text-gray-500">Topics</p>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-green-600">{assignments.length}</p>
              <p className="text-xs text-gray-500">Assignments</p>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-amber-600">{assessments.length}</p>
              <p className="text-xs text-gray-500">Assessments</p>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-red-600">{liveClasses.length}</p>
              <p className="text-xs text-gray-500">Live Classes</p>
            </div>
          </div>
          {subject?.averageScore !== undefined && (
            <div className="rounded-2xl border border-gray-100 bg-white p-5">
              <h3 className="font-semibold text-gray-900">Your Performance</h3>
              <div className="mt-3 flex items-center gap-4">
                <div className="text-3xl font-bold text-indigo-600">{subject.averageScore}%</div>
                <div className="flex-1">
                  <div className="h-3 w-full overflow-hidden rounded-full bg-gray-100">
                    <div className="h-full rounded-full bg-indigo-500" style={{ width: `${subject.averageScore}%` }} />
                  </div>
                  <p className="mt-1 text-xs text-gray-500">Average score across all assessments</p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {activeTab === "resources" && (
        <div className="space-y-3">
          {lessons.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <Library className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No resources yet</h3>
              <p className="mt-1 text-sm text-gray-500">Study materials will appear here once your teacher adds them.</p>
            </div>
          ) : (
            lessons.map(lesson => (
              <div key={lesson.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50"><FileText className="size-5 text-blue-600" /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lesson.title}</p>
                  <p className="text-xs text-gray-400">{lesson.description || "Study material"}</p>
                </div>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-500">Topic</span>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "live" && (
        <div className="space-y-2">
          {liveClasses.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <Video className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No live classes scheduled</h3>
              <p className="mt-1 text-sm text-gray-500">Live classes for this subject will appear here.</p>
            </div>
          ) : (
            liveClasses.map(lc => (
              <div key={lc.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${
                  lc.status === "LIVE" || lc.status === "IN_PROGRESS" ? "bg-red-100" : "bg-blue-50"
                }`}>
                  <Video className={`size-5 ${lc.status === "LIVE" || lc.status === "IN_PROGRESS" ? "text-red-600" : "text-blue-600"}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lc.title}</p>
                  <p className="text-xs text-gray-400">
                    {new Date(lc.scheduledAt).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                  </p>
                </div>
                {(lc.status === "LIVE" || lc.status === "IN_PROGRESS") && (
                  <span className="flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-1 text-[10px] font-bold text-red-600">
                    <span className="size-1.5 animate-pulse rounded-full bg-red-500" /> LIVE
                  </span>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "replays" && (
        <div className="space-y-3">
          {liveClasses.filter(lc => lc.status === "COMPLETED" || lc.status === "ENDED").length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <Play className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">No replays available</h3>
              <p className="mt-1 text-sm text-gray-500">Recordings of past live classes will appear here.</p>
            </div>
          ) : (
            liveClasses.filter(lc => lc.status === "COMPLETED" || lc.status === "ENDED").map(lc => (
              <div key={lc.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-purple-50"><Play className="size-5 text-purple-600" /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lc.title}</p>
                  <p className="text-xs text-gray-400">{new Date(lc.scheduledAt).toLocaleDateString()}</p>
                </div>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-500">Replay</span>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "progress" && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-gray-100 bg-white p-5">
            <h3 className="font-semibold text-gray-900">Topic Progress</h3>
            <div className="mt-3 space-y-3">
              {lessons.length === 0 ? (
                <p className="text-sm text-gray-500">No topics yet.</p>
              ) : (
                lessons.map((lesson, idx) => (
                  <div key={lesson.id} className="flex items-center gap-3">
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-gray-100 text-xs font-bold text-gray-500">
                      {idx + 1}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">{lesson.title}</p>
                    </div>
                    <CheckCircle className="size-4 text-gray-300" />
                  </div>
                ))
              )}
            </div>
          </div>
          <div className="rounded-2xl border border-gray-100 bg-white p-5">
            <h3 className="font-semibold text-gray-900">Assessment Scores</h3>
            <div className="mt-3">
              {assessments.length === 0 ? (
                <p className="text-sm text-gray-500">No assessment scores yet.</p>
              ) : (
                <div className="space-y-2">
                  {assessments.map(a => (
                    <div key={a.id} className="flex items-center justify-between rounded-xl bg-gray-50 p-3">
                      <span className="text-sm font-medium text-gray-900">{a.title}</span>
                      <span className="text-sm font-bold text-indigo-600">{a.totalMarks} marks</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
