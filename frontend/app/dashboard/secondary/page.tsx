"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import {
  secondaryApi,
  type ContinueLearningItem,
  type UpcomingAssessment,
  type AssignmentSummary,
  type LiveClassSummary,
  type SubjectSummary,
  type NextStep,
  type TodayItem,
  type FocusItem,
  type AcademicPulse,
  type AttendanceSummary,
} from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import {
  BookOpen, Clock, ArrowRight, Calendar, FileText, Award,
  Video, TrendingUp, Target, CheckCircle, AlertCircle, Sun,
  Moon, CloudSun, ChevronRight, Zap, Brain, Star, Users
} from "lucide-react"

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return { text: "Good Morning", icon: Sun }
  if (h < 17) return { text: "Good Afternoon", icon: CloudSun }
  return { text: "Good Evening", icon: Moon }
}

function getStageLabel(stage?: string | null, form?: string | null) {
  if (!stage) return ""
  const stageName = stage === "O_LEVEL" ? "O-Level" : stage === "A_LEVEL" ? "A-Level" : stage
  const formNum = form ? form.replace("FORM_", "Form ") : ""
  return formNum ? `${formNum} · ${stageName}` : stageName
}

export default function SecondaryHomePage() {
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const [continueItems, setContinueItems] = useState<ContinueLearningItem[]>([])
  const [assessments, setAssessments] = useState<UpcomingAssessment[]>([])
  const [assignments, setAssignments] = useState<AssignmentSummary[]>([])
  const [liveClasses, setLiveClasses] = useState<LiveClassSummary[]>([])
  const [subjects, setSubjects] = useState<SubjectSummary[]>([])
  const [attendance, setAttendance] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const firstName = user?.name?.split(" ")[0] || "Student"
  const greeting = getGreeting()
  const GreetingIcon = greeting.icon
  const stageLabel = getStageLabel(user?.secondaryStage, user?.form)

  useEffect(() => {
    if (user?.role === "Parent") router.replace("/dashboard/parent")
    else if (user?.role === "Teacher" || user?.role === "Instructor") router.replace("/dashboard/teacher")
  }, [user, router])

  useEffect(() => {
    if (!user) return
    async function loadData() {
      try {
        const classGroupId = user?.classGroupId || ""
        const [summary, continueData, attendData, liveData] = await Promise.all([
          secondaryApi.getDashboardSummary().catch(() => null),
          secondaryApi.getContinueLearning().catch(() => []),
          secondaryApi.getAttendance().catch(() => null),
          secondaryApi.getLiveClasses().catch(() => []),
        ])
        setContinueItems(continueData)
        setAttendance(attendData)
        setLiveClasses(liveData)

        if (classGroupId) {
          const [subjectData, assignmentData, assessmentData] = await Promise.all([
            secondaryApi.getSubjects(classGroupId).catch(() => []),
            secondaryApi.getAssignments(classGroupId).catch(() => []),
            secondaryApi.getAssessments(classGroupId).catch(() => []),
          ])
          setSubjects(subjectData)
          setAssignments(assignmentData)
          setAssessments(assessmentData)
        }
      } catch {}
      setLoading(false)
    }
    loadData()
  }, [user])

  if (authLoading || loading) return <LoadingState />

  const todayItems: TodayItem[] = [
    ...assessments
      .filter(a => a.scheduledAt || a.startsAt)
      .slice(0, 2)
      .map(a => ({
        id: a.id,
        time: a.scheduledAt ? new Date(a.scheduledAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : undefined,
        title: a.title,
        subjectName: a.subjectName,
        type: "assessment" as const,
        status: a.status,
        href: `/dashboard/assessments/${a.id}`,
      })),
    ...liveClasses
      .filter(l => l.status === "SCHEDULED" || l.status === "IN_PROGRESS")
      .slice(0, 2)
      .map(l => ({
        id: l.id,
        time: new Date(l.scheduledAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        title: l.title,
        subjectName: l.subjectName,
        type: "live_class" as const,
        status: l.status,
        href: `/dashboard/live-classes`,
      })),
  ].sort((a, b) => (a.time || "99:99").localeCompare(b.time || "99:99"))

  const nextStep: NextStep = continueItems.length > 0
    ? {
        type: "lesson",
        title: `Continue ${continueItems[0].subjectName || "Learning"}`,
        subtitle: continueItems[0].title || "Resume where you left off",
        subjectName: continueItems[0].subjectName,
        actionLabel: "Continue Learning",
        actionHref: `/dashboard/lessons/${continueItems[0].lessonId}`,
      }
    : assessments.length > 0
      ? {
          type: "assessment",
          title: `Prepare for ${assessments[0].subjectName || "Assessment"}`,
          subtitle: assessments[0].title,
          subjectName: assessments[0].subjectName,
          actionLabel: "Start Revision",
          actionHref: `/dashboard/assessments/${assessments[0].id}`,
        }
      : {
          type: "none",
          title: "Your learning space is ready",
          subtitle: "Choose a subject, explore a challenge, or continue your learning journey.",
          actionLabel: "Browse Subjects",
          actionHref: "/dashboard/courses",
        }

  const focusItems: FocusItem[] = subjects
    .filter(s => s.topicStatus === "NEEDS_PRACTICE" || s.averageScore !== undefined)
    .slice(0, 3)
    .map(s => ({
      subjectName: s.name,
      topicName: s.description,
      reason: s.averageScore !== undefined && s.averageScore < 60
        ? "Practice recommended"
        : "Review recommended",
      type: s.averageScore !== undefined && s.averageScore < 60 ? "practice" : "review",
      subjectId: s.id,
    }))

  const pulse: AcademicPulse = {
    topicsCompleted: subjects.reduce((sum, s) => sum + (s.completedLessons || 0), 0),
    assessmentsUpcoming: assessments.length,
    practiceUnfinished: assignments.filter(a => a.status === "PENDING" || a.status === "ACTIVE").length,
    feedbackAvailable: subjects.filter(s => s.hasTeacherFeedback).length,
    lessonsCompleted: continueItems.filter(i => i.completionPercentage === 100).length,
    attendanceRate: attendance?.attendanceRate || 0,
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-4 pb-24">
      {/* Hero */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-indigo-600 via-blue-600 to-blue-500 p-6 text-white shadow-lg">
        <div className="absolute -right-12 -top-12 size-40 rounded-full bg-white/5" />
        <div className="absolute -bottom-8 -left-8 size-32 rounded-full bg-white/5" />
        <div className="relative flex items-center gap-4">
          <div className="flex size-12 items-center justify-center rounded-xl bg-white/15">
            <GreetingIcon className="size-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold">{greeting.text}, {firstName}</h1>
            <p className="mt-0.5 text-sm text-white/70">
              {stageLabel ? `${stageLabel} · Ready to continue your learning?` : "Ready to continue your learning?"}
            </p>
          </div>
        </div>
        <Link
          href={nextStep.actionHref}
          className="relative mt-4 inline-flex items-center gap-2 rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-indigo-600 shadow-sm transition-colors hover:bg-white/90"
        >
          {nextStep.actionLabel} <ArrowRight className="size-4" />
        </Link>
      </div>

      {/* Your Next Step */}
      <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
        <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-gray-500">
          <Zap className="size-3.5" /> Your Next Step
        </div>
        <div className="mt-3 flex items-start gap-4">
          <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${
            nextStep.type === "lesson" ? "bg-blue-50 text-blue-600" :
            nextStep.type === "assessment" ? "bg-amber-50 text-amber-600" :
            "bg-gray-50 text-gray-400"
          }`}>
            {nextStep.type === "lesson" ? <BookOpen className="size-5" /> :
             nextStep.type === "assessment" ? <Target className="size-5" /> :
             <Star className="size-5" />}
          </div>
          <div className="flex-1">
            <p className="font-semibold text-gray-900">{nextStep.title}</p>
            <p className="mt-0.5 text-sm text-gray-500">{nextStep.subtitle}</p>
          </div>
          <Link
            href={nextStep.actionHref}
            className="shrink-0 rounded-lg bg-indigo-50 px-4 py-2 text-sm font-semibold text-indigo-600 transition-colors hover:bg-indigo-100"
          >
            {nextStep.actionLabel}
          </Link>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {/* Today */}
          <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Today</h2>
            {todayItems.length === 0 ? (
              <p className="mt-3 text-sm text-gray-400">Your day is clear. Choose something to learn or explore.</p>
            ) : (
              <div className="mt-3 space-y-2">
                {todayItems.map(item => (
                  <Link
                    key={item.id}
                    href={item.href}
                    className="flex items-center gap-3 rounded-xl p-3 transition-colors hover:bg-gray-50"
                  >
                    <span className="w-14 text-xs font-medium text-gray-400">{item.time || "—"}</span>
                    <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${
                      item.type === "assessment" ? "bg-amber-50 text-amber-600" :
                      item.type === "live_class" ? "bg-red-50 text-red-600" :
                      item.type === "lesson" ? "bg-blue-50 text-blue-600" :
                      "bg-gray-50 text-gray-600"
                    }`}>
                      {item.type === "assessment" ? <FileText className="size-4" /> :
                       item.type === "live_class" ? <Video className="size-4" /> :
                       <BookOpen className="size-4" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="truncate text-sm font-medium text-gray-900">{item.title}</p>
                      {item.subjectName && <p className="text-xs text-gray-400">{item.subjectName}</p>}
                    </div>
                    <ChevronRight className="size-4 shrink-0 text-gray-300" />
                  </Link>
                ))}
              </div>
            )}
          </section>

          {/* Continue Learning */}
          <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Continue Where You Left Off</h2>
            {continueItems.length === 0 ? (
              <p className="mt-3 text-sm text-gray-400">No recent learning activity. Start a lesson to begin tracking progress.</p>
            ) : (
              <div className="mt-3 space-y-3">
                {continueItems.slice(0, 3).map(item => (
                  <Link
                    key={item.lessonId}
                    href={`/dashboard/lessons/${item.lessonId}`}
                    className="flex items-center gap-4 rounded-xl border border-gray-100 p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
                  >
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                      <BookOpen className="size-5 text-indigo-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="truncate text-sm font-semibold text-gray-900">{item.title || "Lesson"}</p>
                      {item.subjectName && <p className="text-xs text-gray-400">{item.subjectName}</p>}
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-bold text-indigo-600">{Math.round(item.completionPercentage)}%</p>
                      <div className="mt-1 h-1.5 w-16 overflow-hidden rounded-full bg-gray-100">
                        <div className="h-full rounded-full bg-indigo-500" style={{ width: `${item.completionPercentage}%` }} />
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>

        {/* Right column */}
        <div className="space-y-6">
          {/* Academic Pulse */}
          <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Academic Pulse</h2>
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div className="rounded-xl bg-blue-50 p-3 text-center">
                <p className="text-xl font-bold text-blue-600">{pulse.topicsCompleted}</p>
                <p className="text-[10px] font-medium text-blue-700">Lessons Done</p>
              </div>
              <div className="rounded-xl bg-amber-50 p-3 text-center">
                <p className="text-xl font-bold text-amber-600">{pulse.assessmentsUpcoming}</p>
                <p className="text-[10px] font-medium text-amber-700">Assessments</p>
              </div>
              <div className="rounded-xl bg-orange-50 p-3 text-center">
                <p className="text-xl font-bold text-orange-600">{pulse.practiceUnfinished}</p>
                <p className="text-[10px] font-medium text-orange-700">To Complete</p>
              </div>
              <div className="rounded-xl bg-green-50 p-3 text-center">
                <p className="text-xl font-bold text-green-600">{pulse.attendanceRate > 0 ? `${Math.round(pulse.attendanceRate)}%` : "—"}</p>
                <p className="text-[10px] font-medium text-green-700">Attendance</p>
              </div>
            </div>
          </section>

          {/* Your Focus */}
          {focusItems.length > 0 && (
            <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Your Focus</h2>
              <div className="mt-3 space-y-2">
                {focusItems.map((item, i) => (
                  <div key={i} className="flex items-center gap-3 rounded-xl bg-gray-50 p-3">
                    <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${
                      item.type === "practice" ? "bg-orange-100 text-orange-600" :
                      "bg-blue-100 text-blue-600"
                    }`}>
                      {item.type === "practice" ? <AlertCircle className="size-4" /> : <Brain className="size-4" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">{item.subjectName}</p>
                      <p className="text-xs text-gray-400">{item.reason}</p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Quick Actions */}
          <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Quick Learn</h2>
            <div className="mt-3 grid grid-cols-2 gap-2">
              {[
                { label: "Learn", href: "/dashboard/courses", color: "bg-blue-50 text-blue-600" },
                { label: "Practice", href: "/dashboard/assessments", color: "bg-green-50 text-green-600" },
                { label: "Revise", href: "/dashboard/progress", color: "bg-purple-50 text-purple-600" },
                { label: "Live", href: "/dashboard/live-classes", color: "bg-red-50 text-red-600" },
              ].map(action => (
                <Link
                  key={action.label}
                  href={action.href}
                  className={`flex items-center justify-center gap-2 rounded-xl py-3 text-sm font-semibold transition-all hover:shadow-sm ${action.color}`}
                >
                  {action.label}
                </Link>
              ))}
            </div>
          </section>
        </div>
      </div>

      {/* My Subjects */}
      {subjects.length > 0 && (
        <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">My Subjects</h2>
            <Link href="/dashboard/courses" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700">
              View All
            </Link>
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {subjects.slice(0, 6).map(subject => (
              <Link
                key={subject.id}
                href={`/dashboard/courses/${subject.id}`}
                className="flex items-center gap-3 rounded-xl border border-gray-100 p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                  <BookOpen className="size-5 text-indigo-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="truncate text-sm font-semibold text-gray-900">{subject.name}</p>
                  <p className="text-xs text-gray-400">
                    {subject.totalLessons ? `${subject.totalLessons} topics` : ""}
                    {subject.upcomingAssessments ? ` · ${subject.upcomingAssessments} assessment${subject.upcomingAssessments > 1 ? "s" : ""}` : ""}
                  </p>
                </div>
                {subject.hasTeacherFeedback && (
                  <span className="shrink-0 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-bold text-green-700">
                    Feedback
                  </span>
                )}
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Live Learning */}
      {liveClasses.length > 0 && (
        <section className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">Live Learning</h2>
            <Link href="/dashboard/live-classes" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700">
              View All
            </Link>
          </div>
          <div className="mt-3 space-y-2">
            {liveClasses.slice(0, 3).map(lc => (
              <div key={lc.id} className="flex items-center gap-3 rounded-xl p-3 transition-colors hover:bg-gray-50">
                <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${
                  lc.status === "LIVE" || lc.status === "IN_PROGRESS" ? "bg-red-100 text-red-600" :
                  "bg-blue-50 text-blue-600"
                }`}>
                  <Video className="size-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="truncate text-sm font-medium text-gray-900">{lc.title}</p>
                  <p className="text-xs text-gray-400">
                    {new Date(lc.scheduledAt).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                    {lc.subjectName ? ` · ${lc.subjectName}` : ""}
                  </p>
                </div>
                {(lc.status === "LIVE" || lc.status === "IN_PROGRESS") && (
                  <span className="flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-1 text-[10px] font-bold text-red-600">
                    <span className="size-1.5 animate-pulse rounded-full bg-red-500" /> LIVE
                  </span>
                )}
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
