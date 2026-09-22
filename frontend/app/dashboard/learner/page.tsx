"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type {
  HigherEducationDashboard,
  CourseSummary,
  LiveSessionSummary,
  StudyTask,
  ResearchProject,
  WhatsNext,
  TodayItem,
  DayItem,
} from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Sparkles, Clock, Play, Video, BookOpen, BarChart3, FolderOpen,
  Award, CalendarDays, ChevronRight, CheckCircle2, AlertCircle,
  TrendingUp, Library, FlaskConical, Trophy, Rss,
  Briefcase, GraduationCap, Target, FileText, ClipboardList,
  PenTool, Search, MapPin, Users, Star, Lightbulb,
  ListChecks, CalendarClock, BookMarked, Scale,
} from "lucide-react"

export default function LearnerDashboardPage() {
  const { user, loading: authLoading } = useAuth()
  const [dashboard, setDashboard] = useState<HigherEducationDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadDashboard()
  }, [user])

  async function loadDashboard() {
    try {
      setLoading(true)
      setError(null)
      const res = await collegeApi.getHEDashboard(
        user!.id,
        user!.learningLevel || "UNIVERSITY"
      )
      setDashboard(res.data ?? null)
    } catch {
      setError("Failed to load dashboard data")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />
  if (!user) return <LoadingState />

  const firstName = user.firstName || user.name?.split(" ")[0] || "Learner"

  if (error) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={loadDashboard} className="ml-auto text-xs underline">
            Retry
          </button>
        </div>
      </div>
    )
  }

  if (!dashboard) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <LearnerHeader firstName={firstName} subtitle="Higher Education Dashboard" />
        <EmptyState
          icon={<GraduationCap className="size-6" />}
          title="No dashboard data available"
          description="Your academic dashboard is being prepared. Please check back soon."
        />
      </div>
    )
  }

  const {
    academicContext,
    programmeName,
    departmentName,
    academicYear,
    semester,
    whatsNext,
    today,
    continueLearning,
    liveCampus,
    myCourses,
    academicLoad,
    projects,
    research,
    myProgress,
    myEvidence,
    careerWorld,
    dayWeekView,
    studyPlannerTasks,
  } = dashboard

  const todayItems = today?.items ?? []
  const pendingTasks = today?.pendingTasks ?? 0
  const completedTasks = today?.completedTasks ?? 0
  const totalTasks = today?.totalTasks ?? 0
  const upcomingSessions = liveCampus?.sessions?.filter(
    (s) => s.status === "SCHEDULED" || s.status === "UPCOMING"
  ) ?? []
  const liveNowSessions = liveCampus?.sessions?.filter(
    (s) => s.status === "IN_PROGRESS" || s.status === "LIVE"
  ) ?? []
  const plannerTasks = studyPlannerTasks ?? []
  const upcomingPlanner = plannerTasks
    .filter((t) => !t.isCompleted)
    .sort((a, b) => {
      if (!a.scheduledDate) return 1
      if (!b.scheduledDate) return -1
      return new Date(a.scheduledDate).getTime() - new Date(b.scheduledDate).getTime()
    })
  const researchItems = research ?? []
  const projectItems = projects ?? []
  const dayItems = dayWeekView?.todayItems ?? []
  const weekItems = dayWeekView?.weekItems ?? []

  function priorityColor(p: string) {
    switch (p) {
      case "URGENT": return "text-red-600 bg-red-500/10"
      case "HIGH": return "text-orange-600 bg-orange-500/10"
      case "MEDIUM": return "text-blue-600 bg-blue-500/10"
      default: return "text-muted-foreground bg-muted"
    }
  }

  function statusColor(s: string) {
    switch (s) {
      case "COMPLETED": return "text-emerald-600 bg-emerald-500/10"
      case "IN_PROGRESS": return "text-blue-600 bg-blue-500/10"
      case "REVIEW": case "UNDER_REVIEW": return "text-amber-600 bg-amber-500/10"
      case "IDEATION": case "PLANNING": return "text-violet-600 bg-violet-500/10"
      default: return "text-muted-foreground bg-muted"
    }
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* ─── Welcome Header with Academic Context ─── */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-primary/10 p-6 shadow-xs">
        <LearnerHeader firstName={firstName} subtitle={academicContext || "Higher Education Dashboard"} />
        <div className="mt-3 flex flex-wrap gap-2">
          {programmeName && (
            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
              <GraduationCap className="size-3" /> {programmeName}
            </span>
          )}
          {departmentName && (
            <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2.5 py-1 text-xs font-medium text-blue-600">
              <Users className="size-3" /> {departmentName}
            </span>
          )}
          {semester && (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-600">
              <CalendarDays className="size-3" /> {semester}
            </span>
          )}
          {academicYear && (
            <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-1 text-xs font-medium text-amber-600">
              <Clock className="size-3" /> {academicYear}
            </span>
          )}
          {academicLoad?.cumulativeGpa != null && (
            <span className="inline-flex items-center gap-1 rounded-full bg-violet-500/10 px-2.5 py-1 text-xs font-medium text-violet-600">
              <BarChart3 className="size-3" /> GPA: {academicLoad.cumulativeGpa.toFixed(2)}
            </span>
          )}
        </div>
      </div>

      {/* ─── What's Next ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Sparkles className="size-4 text-primary" />
            <h3 className="font-semibold text-foreground">What&apos;s Next</h3>
          </div>
        </div>
        {whatsNext ? (
          <div className="flex items-start gap-4">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
              <Lightbulb className="size-5 text-primary" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="font-semibold text-foreground">{whatsNext.title}</p>
              <p className="text-sm text-muted-foreground line-clamp-2">{whatsNext.description}</p>
              <div className="mt-2 flex items-center gap-3">
                {whatsNext.type && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                    {whatsNext.type}
                  </span>
                )}
                {whatsNext.deadline && (
                  <span className="text-xs text-muted-foreground">
                    Due {new Date(whatsNext.deadline).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                  </span>
                )}
                {whatsNext.url && (
                  <Link href={whatsNext.url} className="text-xs font-medium text-primary hover:underline inline-flex items-center gap-1">
                    Open <ChevronRight className="size-3" />
                  </Link>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <CheckCircle2 className="size-5 text-emerald-500" />
            <p className="text-sm">You&apos;re all caught up! Nothing pending right now.</p>
          </div>
        )}
      </div>

      {/* ─── Today's Tasks ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <ClipboardList className="size-4 text-blue-600" />
            <h3 className="font-semibold text-foreground">Today</h3>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="text-emerald-600 font-medium">{completedTasks} done</span>
            <span>/</span>
            <span>{totalTasks} total</span>
          </div>
        </div>
        {todayItems.length > 0 ? (
          <div className="space-y-2">
            {todayItems.map((item: TodayItem, idx: number) => (
              <div
                key={idx}
                className="flex items-center gap-3 rounded-xl border border-border p-3"
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-500/10">
                  {item.status === "COMPLETED" ? (
                    <CheckCircle2 className="size-4 text-emerald-500" />
                  ) : (
                    <Clock className="size-4 text-blue-500" />
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <p className={`text-sm font-medium ${item.status === "COMPLETED" ? "text-muted-foreground line-through" : "text-foreground"}`}>
                    {item.title}
                  </p>
                  <div className="flex items-center gap-2 mt-0.5">
                    {item.type && (
                      <span className="text-[10px] text-muted-foreground">{item.type}</span>
                    )}
                    {item.time && (
                      <span className="text-[10px] text-muted-foreground">{item.time}</span>
                    )}
                  </div>
                </div>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${item.status === "COMPLETED" ? "text-emerald-600 bg-emerald-500/10" : "text-blue-600 bg-blue-500/10"}`}>
                  {item.status}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">No tasks scheduled for today.</p>
        )}
      </div>

      {/* ─── Continue Learning ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Play className="size-4 text-emerald-600" />
            <h3 className="font-semibold text-foreground">Continue Learning</h3>
          </div>
          {continueLearning?.courseId && (
            <Link
              href={`/dashboard/learner/courses/${continueLearning.courseId}`}
              className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
            >
              Resume <ChevronRight className="size-3" />
            </Link>
          )}
        </div>
        {continueLearning?.lastCourse ? (
          <div className="flex items-start gap-4">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10">
              <BookOpen className="size-5 text-emerald-500" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="font-semibold text-foreground">{continueLearning.lastCourse}</p>
              {continueLearning.lastModule && (
                <p className="text-xs text-muted-foreground">{continueLearning.lastModule}</p>
              )}
              <div className="mt-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Progress</span>
                  <span className="font-semibold text-emerald-600">{continueLearning.progressPercent}%</span>
                </div>
                <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-emerald-500 transition-all"
                    style={{ width: `${continueLearning.progressPercent}%` }}
                  />
                </div>
              </div>
              {continueLearning.courseId && (
                <Link
                  href={`/dashboard/learner/courses/${continueLearning.courseId}`}
                  className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                >
                  Resume Learning <ChevronRight className="size-3" />
                </Link>
              )}
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <BookOpen className="size-5" />
            <div>
              <p className="text-sm font-medium">Start your learning journey</p>
              <p className="text-xs">Enroll in a course to begin tracking your progress.</p>
            </div>
            <Link
              href="/dashboard/learner/courses"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Browse Courses
            </Link>
          </div>
        )}
      </div>

      {/* ─── Live Campus ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Video className="size-4 text-red-600" />
            <h3 className="font-semibold text-foreground">Live Campus</h3>
          </div>
          <Link
            href="/dashboard/learner/live-classes"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            View All <ChevronRight className="size-3" />
          </Link>
        </div>
        {liveNowSessions.length > 0 ? (
          <div>
            <div className="flex items-center gap-2 mb-3">
              <span className="size-2 rounded-full bg-red-500 animate-pulse" />
              <span className="text-sm font-medium text-red-600">{liveNowSessions.length} live now</span>
            </div>
            <div className="space-y-2">
              {liveNowSessions.slice(0, 3).map((session: LiveSessionSummary) => (
                <div key={session.id} className="flex items-center gap-3 rounded-xl border border-red-500/20 bg-red-500/5 p-3">
                  <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-red-500/10">
                    <span className="size-2 rounded-full bg-red-500 animate-pulse" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground line-clamp-1">{session.title}</p>
                    <p className="text-[10px] text-muted-foreground">{session.sessionType}</p>
                  </div>
                  <Link
                    href="/dashboard/learner/live-classes"
                    className="shrink-0 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700"
                  >
                    Join
                  </Link>
                </div>
              ))}
            </div>
          </div>
        ) : upcomingSessions.length > 0 ? (
          <div>
            <p className="text-sm font-medium text-muted-foreground mb-2">{upcomingSessions.length} upcoming today</p>
            <div className="space-y-2">
              {upcomingSessions.slice(0, 3).map((session: LiveSessionSummary) => (
                <div key={session.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted">
                    <CalendarClock className="size-4 text-muted-foreground" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground line-clamp-1">{session.title}</p>
                    <p className="text-[10px] text-muted-foreground">
                      {new Date(session.startTime).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })}
                      {" · "}{session.sessionType}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <Video className="size-5" />
            <p className="text-sm">No live sessions right now. Check back during class hours.</p>
          </div>
        )}
      </div>

      {/* ─── My Courses ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <BookOpen className="size-4 text-primary" />
            <h3 className="font-semibold text-foreground">My Courses</h3>
          </div>
          <Link
            href="/dashboard/learner/courses"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            Browse More <ChevronRight className="size-3" />
          </Link>
        </div>
        {myCourses.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {myCourses.map((course: CourseSummary) => (
              <Link
                key={course.id}
                href={`/dashboard/learner/courses/${course.id}`}
                className="rounded-xl border border-border p-4 transition-colors hover:bg-muted/50"
              >
                <div className="flex items-center gap-3 mb-3">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <BookOpen className="size-5 text-primary/40" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{course.title}</p>
                    <p className="text-[10px] text-muted-foreground">
                      {course.completedModules}/{course.totalModules} modules
                    </p>
                  </div>
                </div>
                <div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Progress</span>
                    <span className="font-semibold text-primary">{course.progressPercent}%</span>
                  </div>
                  <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary"
                      style={{ width: `${course.progressPercent}%` }}
                    />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <FolderOpen className="size-5" />
            <div>
              <p className="text-sm font-medium">No courses enrolled yet</p>
              <p className="text-xs">Browse our course catalog to get started.</p>
            </div>
            <Link
              href="/dashboard/learner/courses"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Explore
            </Link>
          </div>
        )}
      </div>

      {/* ─── Academic Load ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Scale className="size-4 text-violet-600" />
            <h3 className="font-semibold text-foreground">Academic Load</h3>
          </div>
          <Link
            href="/dashboard/learner/academic-records"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            View Records <ChevronRight className="size-3" />
          </Link>
        </div>
        {academicLoad ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <p className="text-xs text-muted-foreground">Cumulative GPA</p>
              <p className="text-2xl font-extrabold text-foreground mt-1">
                {academicLoad.cumulativeGpa != null ? academicLoad.cumulativeGpa.toFixed(2) : "N/A"}
              </p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <p className="text-xs text-muted-foreground">Semester GPA</p>
              <p className="text-2xl font-extrabold text-foreground mt-1">
                {academicLoad.currentSemesterGpa != null ? academicLoad.currentSemesterGpa.toFixed(2) : "N/A"}
              </p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <p className="text-xs text-muted-foreground">Credit Hours</p>
              <p className="text-2xl font-extrabold text-foreground mt-1">
                {academicLoad.completedCreditHours}/{academicLoad.totalCreditHours}
              </p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <p className="text-xs text-muted-foreground">Enrolled Courses</p>
              <p className="text-2xl font-extrabold text-foreground mt-1">
                {academicLoad.currentSemesterCourses}
              </p>
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">No academic records available yet.</p>
        )}
      </div>

      {/* ─── Research ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <FlaskConical className="size-4 text-teal-600" />
            <h3 className="font-semibold text-foreground">Research</h3>
          </div>
          <Link
            href="/dashboard/learner/research"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            View All <ChevronRight className="size-3" />
          </Link>
        </div>
        {researchItems.length > 0 ? (
          <div className="space-y-2">
            {researchItems.slice(0, 4).map((item: ResearchProject) => (
              <Link
                key={item.id}
                href={`/dashboard/learner/research/${item.id}`}
                className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-teal-500/10">
                  <FlaskConical className="size-5 text-teal-500/40" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                  <p className="text-[10px] text-muted-foreground line-clamp-1">
                    {item.researchQuestion || "No research question set"}
                  </p>
                </div>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusColor(item.status)}`}>
                  {item.status.replace(/_/g, " ")}
                </span>
              </Link>
            ))}
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <FlaskConical className="size-5" />
            <div>
              <p className="text-sm font-medium">No research projects yet</p>
              <p className="text-xs">Start a research project to track your academic inquiry.</p>
            </div>
            <Link
              href="/dashboard/learner/research"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Start Research
            </Link>
          </div>
        )}
      </div>

      {/* ─── Projects ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <FolderOpen className="size-4 text-amber-600" />
            <h3 className="font-semibold text-foreground">Projects</h3>
          </div>
          <Link
            href="/dashboard/learner/projects"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            View All <ChevronRight className="size-3" />
          </Link>
        </div>
        {projectItems.length > 0 ? (
          <div className="space-y-2">
            {projectItems.slice(0, 4).map((item: CourseSummary) => (
              <Link
                key={item.id}
                href={`/dashboard/learner/projects/${item.id}`}
                className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
                  <FolderOpen className="size-5 text-amber-500/40" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                  <p className="text-[10px] text-muted-foreground">
                    {item.completedModules}/{item.totalModules} milestones
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-xs font-semibold text-amber-600">{item.progressPercent}%</p>
                  <div className="mt-1 h-1 w-16 overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-amber-500"
                      style={{ width: `${item.progressPercent}%` }}
                    />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <FolderOpen className="size-5" />
            <div>
              <p className="text-sm font-medium">No projects yet</p>
              <p className="text-xs">Create a project to showcase your practical work.</p>
            </div>
            <Link
              href="/dashboard/learner/projects"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Create Project
            </Link>
          </div>
        )}
      </div>

      {/* ─── My Progress ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <BarChart3 className="size-4 text-teal-600" />
            <h3 className="font-semibold text-foreground">My Progress</h3>
          </div>
        </div>
        {myProgress ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <div className="flex items-center gap-2 mb-1">
                <BarChart3 className="size-3.5 text-teal-600" />
                <p className="text-xs text-muted-foreground">GPA</p>
              </div>
              <p className="text-xl font-extrabold text-foreground">
                {myProgress.cumulativeGpa != null ? myProgress.cumulativeGpa.toFixed(2) : "N/A"}
              </p>
              {myProgress.semesterGpa != null && (
                <p className="text-[10px] text-muted-foreground mt-0.5">Semester: {myProgress.semesterGpa.toFixed(2)}</p>
              )}
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <div className="flex items-center gap-2 mb-1">
                <Target className="size-3.5 text-blue-600" />
                <p className="text-xs text-muted-foreground">Competencies</p>
              </div>
              <p className="text-xl font-extrabold text-foreground">
                {myProgress.competenciesCompleted}/{myProgress.competenciesTotal}
              </p>
              {myProgress.competenciesTotal > 0 && (
                <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-blue-500"
                    style={{ width: `${Math.round((myProgress.competenciesCompleted / myProgress.competenciesTotal) * 100)}%` }}
                  />
                </div>
              )}
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <div className="flex items-center gap-2 mb-1">
                <FolderOpen className="size-3.5 text-amber-600" />
                <p className="text-xs text-muted-foreground">Projects</p>
              </div>
              <p className="text-xl font-extrabold text-foreground">
                {myProgress.projectsCompleted}/{myProgress.projectsTotal}
              </p>
              {myProgress.projectsTotal > 0 && (
                <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-amber-500"
                    style={{ width: `${Math.round((myProgress.projectsCompleted / myProgress.projectsTotal) * 100)}%` }}
                  />
                </div>
              )}
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <div className="flex items-center gap-2 mb-1">
                <Award className="size-3.5 text-violet-600" />
                <p className="text-xs text-muted-foreground">Courses</p>
              </div>
              <p className="text-xl font-extrabold text-foreground">
                {myProgress.completedCourses}/{myProgress.totalCourses}
              </p>
              {myProgress.academicStanding && (
                <p className="text-[10px] text-muted-foreground mt-0.5">{myProgress.academicStanding}</p>
              )}
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">Progress tracking will appear once you begin coursework.</p>
        )}
      </div>

      {/* ─── My Evidence ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <FileText className="size-4 text-rose-600" />
            <h3 className="font-semibold text-foreground">My Evidence</h3>
          </div>
          <Link
            href="/dashboard/learner/portfolio"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            View Portfolio <ChevronRight className="size-3" />
          </Link>
        </div>
        {myEvidence ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <div className="rounded-xl border border-border bg-muted/30 p-3 text-center">
              <p className="text-2xl font-extrabold text-foreground">{myEvidence.portfolioItems}</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Portfolio Items</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3 text-center">
              <p className="text-2xl font-extrabold text-foreground">{myEvidence.demonstrations}</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Demonstrations</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3 text-center">
              <p className="text-2xl font-extrabold text-foreground">{myEvidence.projectSubmissions}</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Submissions</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3 text-center">
              <p className="text-2xl font-extrabold text-foreground">{myEvidence.logbookEntries}</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Logbook Entries</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3 text-center">
              <p className="text-2xl font-extrabold text-foreground">{myEvidence.competenciesRecorded}</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Competencies</p>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <FileText className="size-5" />
            <div>
              <p className="text-sm font-medium">No evidence collected yet</p>
              <p className="text-xs">Your portfolio, demonstrations, and submissions will appear here.</p>
            </div>
            <Link
              href="/dashboard/learner/portfolio"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Open Portfolio
            </Link>
          </div>
        )}
      </div>

      {/* ─── Career World ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Briefcase className="size-4 text-indigo-600" />
            <h3 className="font-semibold text-foreground">Career World</h3>
          </div>
          <Link
            href="/dashboard/learner/career-profile"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            {careerWorld?.hasProfile ? "Edit Profile" : "Create Profile"} <ChevronRight className="size-3" />
          </Link>
        </div>
        {careerWorld?.hasProfile ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {careerWorld.careerObjective && (
              <div className="rounded-xl border border-border bg-muted/30 p-4 sm:col-span-2">
                <p className="text-xs text-muted-foreground mb-1">Career Objective</p>
                <p className="text-sm font-medium text-foreground line-clamp-2">{careerWorld.careerObjective}</p>
              </div>
            )}
            {careerWorld.targetIndustry && (
              <div className="rounded-xl border border-border bg-muted/30 p-4">
                <p className="text-xs text-muted-foreground mb-1">Target Industry</p>
                <p className="text-sm font-medium text-foreground">{careerWorld.targetIndustry}</p>
              </div>
            )}
            {careerWorld.targetRole && (
              <div className="rounded-xl border border-border bg-muted/30 p-4">
                <p className="text-xs text-muted-foreground mb-1">Target Role</p>
                <p className="text-sm font-medium text-foreground">{careerWorld.targetRole}</p>
              </div>
            )}
            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <p className="text-xs text-muted-foreground mb-1">Skills Listed</p>
              <p className="text-xl font-extrabold text-foreground">{careerWorld.skillsCount}</p>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <Briefcase className="size-5" />
            <div>
              <p className="text-sm font-medium">Build your career profile</p>
              <p className="text-xs">Set your career goals, list your skills, and plan your professional path.</p>
            </div>
            <Link
              href="/dashboard/learner/career-profile"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Get Started
            </Link>
          </div>
        )}
      </div>

      {/* ─── Day & Week View ─── */}
      <div className="grid gap-4 sm:grid-cols-2">
        {/* Today */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-3">
            <CalendarDays className="size-4 text-blue-600" />
            <h3 className="font-semibold text-foreground">Today</h3>
          </div>
          {dayItems.length > 0 ? (
            <div className="space-y-2">
              {dayItems.slice(0, 5).map((item: DayItem, idx: number) => (
                <div key={idx} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground line-clamp-1">{item.title}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      {item.time && <span className="text-[10px] text-muted-foreground">{item.time}</span>}
                      {item.type && <span className="text-[10px] text-muted-foreground">{item.type}</span>}
                    </div>
                  </div>
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusColor(item.status)}`}>
                    {item.status}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No items scheduled for today.</p>
          )}
        </div>

        {/* This Week */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-3">
            <CalendarClock className="size-4 text-violet-600" />
            <h3 className="font-semibold text-foreground">This Week</h3>
          </div>
          {weekItems.length > 0 ? (
            <div className="space-y-2">
              {weekItems.slice(0, 5).map((item: DayItem, idx: number) => (
                <div key={idx} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground line-clamp-1">{item.title}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      {item.date && <span className="text-[10px] text-muted-foreground">{item.date}</span>}
                      {item.time && <span className="text-[10px] text-muted-foreground">{item.time}</span>}
                      {item.type && <span className="text-[10px] text-muted-foreground">{item.type}</span>}
                    </div>
                  </div>
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusColor(item.status)}`}>
                    {item.status}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No items scheduled for this week.</p>
          )}
        </div>
      </div>

      {/* ─── Study Planner ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <ListChecks className="size-4 text-emerald-600" />
            <h3 className="font-semibold text-foreground">Study Planner</h3>
          </div>
          <Link
            href="/dashboard/learner/study-planner"
            className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
          >
            Open Planner <ChevronRight className="size-3" />
          </Link>
        </div>
        {upcomingPlanner.length > 0 ? (
          <div className="space-y-2">
            {upcomingPlanner.slice(0, 5).map((task: StudyTask) => (
              <div key={task.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10">
                  {task.taskType === "EXAM_PREP" ? (
                    <GraduationCap className="size-4 text-emerald-500" />
                  ) : task.taskType === "ASSIGNMENT" ? (
                    <PenTool className="size-4 text-emerald-500" />
                  ) : task.taskType === "RESEARCH" ? (
                    <FlaskConical className="size-4 text-emerald-500" />
                  ) : task.taskType === "PROJECT" ? (
                    <FolderOpen className="size-4 text-emerald-500" />
                  ) : (
                    <BookMarked className="size-4 text-emerald-500" />
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground line-clamp-1">{task.title}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    {task.scheduledDate && (
                      <span className="text-[10px] text-muted-foreground">
                        {new Date(task.scheduledDate).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })}
                      </span>
                    )}
                    {task.scheduledTime && (
                      <span className="text-[10px] text-muted-foreground">{task.scheduledTime}</span>
                    )}
                    {task.durationMinutes && (
                      <span className="text-[10px] text-muted-foreground">{task.durationMinutes}min</span>
                    )}
                  </div>
                </div>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${priorityColor(task.priority)}`}>
                  {task.priority}
                </span>
              </div>
            ))}
          </div>
        ) : plannerTasks.length > 0 ? (
          <div className="flex items-center gap-3 text-muted-foreground">
            <CheckCircle2 className="size-5 text-emerald-500" />
            <p className="text-sm">All study tasks completed! Great work.</p>
          </div>
        ) : (
          <div className="flex items-center gap-3 text-muted-foreground">
            <ListChecks className="size-5" />
            <div>
              <p className="text-sm font-medium">No study tasks yet</p>
              <p className="text-xs">Create tasks to plan your study schedule.</p>
            </div>
            <Link
              href="/dashboard/learner/study-planner"
              className="ml-auto shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              Plan Studies
            </Link>
          </div>
        )}
      </div>

      {/* ─── Quick Links ─── */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-3">
          <Rss className="size-4 text-indigo-600" />
          <h3 className="font-semibold text-foreground">Quick Links</h3>
        </div>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <Link
            href="/dashboard/learner/courses"
            className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
          >
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
              <BookOpen className="size-4 text-primary" />
            </div>
            <span className="text-sm font-medium text-foreground">Course Catalog</span>
          </Link>
          <Link
            href="/dashboard/learner/live-classes"
            className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
          >
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-red-500/10">
              <Video className="size-4 text-red-500" />
            </div>
            <span className="text-sm font-medium text-foreground">Live Classes</span>
          </Link>
          <Link
            href="/dashboard/learner/certificates"
            className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
          >
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
              <Award className="size-4 text-amber-500" />
            </div>
            <span className="text-sm font-medium text-foreground">Certificates</span>
          </Link>
          <Link
            href="/dashboard/learner/portfolio"
            className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
          >
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-rose-500/10">
              <FileText className="size-4 text-rose-500" />
            </div>
            <span className="text-sm font-medium text-foreground">Portfolio</span>
          </Link>
        </div>
      </div>
    </div>
  )
}
