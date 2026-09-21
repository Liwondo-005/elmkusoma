"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary, type LiveClassSummary, type TeacherFeedback } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, BookOpen, ChevronRight, PenTool, Award, Calendar, Video, FileText, MessageSquare, Library, BarChart3, Clock, CheckCircle, Play, ClipboardList } from "lucide-react"
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
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [subject, setSubject] = useState<SubjectSummary | null>(null)
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [liveClasses, setLiveClasses] = useState<LiveClassSummary[]>([])
  const [feedback, setFeedback] = useState<TeacherFeedback[]>([])
  const [lessonProgress, setLessonProgress] = useState<Record<string, number>>({})
  const [activeTab, setActiveTab] = useState<"overview" | "topics" | "lessons" | "practice" | "assignments" | "assessments" | "resources" | "live" | "replays" | "progress" | "feedback">("overview")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    Promise.all([
      secondaryApi.getSubject(params.id),
      secondaryApi.getLessonsBySubject(params.id, user.classGroupId).catch(() => []),
      secondaryApi.getAssignments(user.classGroupId).catch(() => []),
      secondaryApi.getAssessments(user.classGroupId).catch(() => []),
      secondaryApi.getLiveClasses().catch(() => []),
      secondaryApi.getTeacherFeedback(params.id).catch(() => []),
      secondaryApi.getLessonProgress(user.id).catch(() => []),
    ])
      .then(([s, l, a, as, lc, fb, lp]) => {
        setSubject(s)
        setLessons(l || [])
        setAssignments((a || []).filter(x => x.subjectName === s?.name))
        setAssessments((as || []).filter(x => x.subjectName === s?.name))
        setLiveClasses((lc || []).filter(x => x.subjectName === s?.name))
        setFeedback(fb || [])
        const progressMap: Record<string, number> = {}
        ;(lp || []).forEach((p: any) => {
          if (p.lessonId) progressMap[p.lessonId] = p.completionPercentage || 0
        })
        setLessonProgress(progressMap)
      })
      .catch(() => setError(t("errorLoading")))
      .finally(() => setLoading(false))
  }, [user, params.id])

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

  const tabs = [
    { id: "overview" as const, label: t("overview"), icon: BookOpen },
    { id: "topics" as const, label: t("topics"), icon: BookOpen },
    { id: "lessons" as const, label: t("lessons"), icon: FileText },
    { id: "practice" as const, label: t("practice"), icon: PenTool },
    { id: "assignments" as const, label: t("assignments"), icon: ClipboardList },
    { id: "assessments" as const, label: t("assessments"), icon: Award },
    { id: "resources" as const, label: t("resources"), icon: Library },
    { id: "live" as const, label: t("live"), icon: Video },
    { id: "replays" as const, label: t("replays"), icon: Play },
    { id: "progress" as const, label: t("progress"), icon: BarChart3 },
    { id: "feedback" as const, label: t("feedback"), icon: MessageSquare },
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{subject?.name || t("subject")}</h1>
          <p className="text-sm text-gray-500">
            {lessons.length} {t("topicCount", { count: lessons.length })}
            {subject?.averageScore !== undefined && ` · ${t("average")} ${subject.averageScore}%`}
          </p>
        </div>
      </div>

      <div className="flex gap-1 rounded-xl bg-gray-100 p-1" role="tablist" aria-label={t("subjectTabs")}>
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-sm font-medium transition-colors ${
              activeTab === tab.id ? "bg-white text-indigo-600 shadow-sm" : "text-gray-500 hover:text-gray-700"
            }`}
            role="tab"
            aria-selected={activeTab === tab.id}
            aria-label={tab.label}
          >
            <tab.icon className="size-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "topics" && (
        <div className="space-y-2">
          {lessons.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <BookOpen className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noTopicsYet")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("teacherWillAddTopics")}</p>
            </div>
          ) : (
            lessons.map((lesson, idx) => (
              <Link
                key={lesson.id}
                href={`/dashboard/secondary/subjects/${params.id}/topics/${lesson.id}`}
                className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
                aria-label={`${lesson.title} - ${t("topic")}`}
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

      {activeTab === "lessons" && (
        <div className="space-y-2">
          {lessons.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <FileText className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noLessonsYet")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("teacherWillAddLessons")}</p>
            </div>
          ) : (
            lessons.map((lesson, idx) => (
              <Link
                key={lesson.id}
                href={`/dashboard/secondary/subjects/${params.id}/topics/${lesson.id}`}
                className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
                aria-label={`${lesson.title} - ${t("lesson")}`}
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-sm font-bold text-blue-600">
                  {idx + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lesson.title}</p>
                  {lesson.description && (
                    <p className="mt-0.5 text-xs text-gray-400 line-clamp-1">{lesson.description}</p>
                  )}
                </div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${lesson.isPublished ? "bg-green-100 text-green-600" : "bg-gray-100 text-gray-500"}`}>
                  {lesson.isPublished ? t("published") : t("draft")}
                </span>
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
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noAssignments")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("teacherWillAssignPractice")}</p>
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
                    {a.dueDate && `${t("due")} ${new Date(a.dueDate).toLocaleDateString()}`}
                    {` · ${a.totalMarks} ${t("marks")}`}
                  </p>
                </div>
                <span className="rounded-full bg-amber-100 px-2.5 py-1 text-[10px] font-bold text-amber-700">{a.status}</span>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "assignments" && (
        <div className="space-y-2">
          {assignments.length === 0 ? (
            <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
              <ClipboardList className="mx-auto size-12 text-gray-300" />
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noAssignments")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("teacherWillAssignWork")}</p>
            </div>
          ) : (
            assignments.map(a => (
              <div key={a.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-orange-50">
                  <ClipboardList className="size-5 text-orange-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{a.title}</p>
                  <p className="text-xs text-gray-400">
                    {a.dueDate && `${t("due")} ${new Date(a.dueDate).toLocaleDateString()}`}
                    {` · ${a.totalMarks} ${t("marks")}`}
                  </p>
                </div>
                <span className="rounded-full bg-orange-100 px-2.5 py-1 text-[10px] font-bold text-orange-700">{a.status}</span>
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
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noAssessments")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("teacherWillScheduleAssessments")}</p>
            </div>
          ) : (
            assessments.map(a => (
              <Link
                key={a.id}
                href={`/dashboard/assessments/${a.id}`}
                className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200"
                aria-label={`${a.title} - ${t("assessment")}`}
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                  <Award className="size-5 text-indigo-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{a.title}</p>
                  <p className="text-xs text-gray-400">{a.totalMarks} {t("marks")}</p>
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
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noFeedbackYet")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("teacherWillProvideFeedback")}</p>
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
            <p className="mt-1 text-sm text-gray-500">{subject?.description || t("noDescriptionAvailable")}</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-indigo-600">{lessons.length}</p>
              <p className="text-xs text-gray-500">{t("topics")}</p>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-green-600">{assignments.length}</p>
              <p className="text-xs text-gray-500">{t("assignments")}</p>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-amber-600">{assessments.length}</p>
              <p className="text-xs text-gray-500">{t("assessments")}</p>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-white p-4 text-center">
              <p className="text-2xl font-bold text-red-600">{liveClasses.length}</p>
              <p className="text-xs text-gray-500">{t("liveClasses")}</p>
            </div>
          </div>
          {subject?.averageScore !== undefined && (
            <div className="rounded-2xl border border-gray-100 bg-white p-5">
              <h3 className="font-semibold text-gray-900">{t("yourPerformance")}</h3>
              <div className="mt-3 flex items-center gap-4">
                <div className="text-3xl font-bold text-indigo-600">{subject.averageScore}%</div>
                <div className="flex-1">
                  <div
                    className="h-3 w-full overflow-hidden rounded-full bg-gray-100"
                    role="progressbar"
                    aria-valuenow={subject.averageScore}
                    aria-valuemin={0}
                    aria-valuemax={100}
                  >
                    <div className="h-full rounded-full bg-indigo-500" style={{ width: `${subject.averageScore}%` }} />
                  </div>
                  <p className="mt-1 text-xs text-gray-500">{t("averageScoreAcrossAssessments")}</p>
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
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noResourcesYet")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("studyMaterialsWillAppear")}</p>
            </div>
          ) : (
            lessons.map(lesson => (
              <div key={lesson.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50"><FileText className="size-5 text-blue-600" /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lesson.title}</p>
                  <p className="text-xs text-gray-400">{lesson.description || t("studyMaterial")}</p>
                </div>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-500">{t("topic")}</span>
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
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noLiveClassesScheduled")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("liveClassesWillAppear")}</p>
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
              <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noReplaysAvailable")}</h3>
              <p className="mt-1 text-sm text-gray-500">{t("recordingsWillAppear")}</p>
            </div>
          ) : (
            liveClasses.filter(lc => lc.status === "COMPLETED" || lc.status === "ENDED").map(lc => (
              <div key={lc.id} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-purple-50"><Play className="size-5 text-purple-600" /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900">{lc.title}</p>
                  <p className="text-xs text-gray-400">{new Date(lc.scheduledAt).toLocaleDateString()}</p>
                </div>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-500">{t("replay")}</span>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "progress" && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-gray-100 bg-white p-5">
            <h3 className="font-semibold text-gray-900">{t("topicProgress")}</h3>
            <div className="mt-3 space-y-3">
              {lessons.length === 0 ? (
                <p className="text-sm text-gray-500">{t("noTopicsYet")}</p>
              ) : (
                lessons.map((lesson, idx) => {
                  const pct = lessonProgress[lesson.id] || 0
                  const isComplete = pct >= 100
                  return (
                    <div key={lesson.id} className="flex items-center gap-3">
                      <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-gray-100 text-xs font-bold text-gray-500">
                        {idx + 1}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900">{lesson.title}</p>
                        {pct > 0 && (
                          <div className="mt-1 flex items-center gap-2">
                            <div
                              className="h-1.5 flex-1 overflow-hidden rounded-full bg-gray-100"
                              role="progressbar"
                              aria-valuenow={pct}
                              aria-valuemin={0}
                              aria-valuemax={100}
                            >
                              <div className={`h-full rounded-full ${isComplete ? "bg-green-500" : "bg-indigo-500"}`} style={{ width: `${Math.min(pct, 100)}%` }} />
                            </div>
                            <span className="text-[10px] text-gray-400">{Math.round(pct)}%</span>
                          </div>
                        )}
                      </div>
                      <CheckCircle className={`size-4 ${isComplete ? "text-green-500" : pct > 0 ? "text-indigo-400" : "text-gray-300"}`} />
                    </div>
                  )
                })
              )}
            </div>
          </div>
          <div className="rounded-2xl border border-gray-100 bg-white p-5">
            <h3 className="font-semibold text-gray-900">{t("assessmentScores")}</h3>
            <div className="mt-3">
              {assessments.length === 0 ? (
                <p className="text-sm text-gray-500">{t("noAssessmentScoresYet")}</p>
              ) : (
                <div className="space-y-2">
                  {assessments.map(a => (
                    <div key={a.id} className="flex items-center justify-between rounded-xl bg-gray-50 p-3">
                      <span className="text-sm font-medium text-gray-900">{a.title}</span>
                      <span className="text-sm font-bold text-indigo-600">{a.totalMarks} {t("marks")}</span>
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
