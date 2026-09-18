"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { HigherEducationDashboard } from "@/lib/types/college"
import { LearnerHeader, LoadingState } from "@/components/learner/shared"
import {
  Sparkles, Clock, Play, Video, BookOpen, BarChart3, FolderOpen,
  FlaskConical, Award, Briefcase, CalendarDays, ClipboardList,
  ChevronRight, CheckCircle2, AlertCircle, TrendingUp, Target
} from "lucide-react"

export default function HigherEducationDashboardPage() {
  const { user, loading: authLoading } = useAuth()
  const [dashboard, setDashboard] = useState<HigherEducationDashboard | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadDashboard()
  }, [user])

  async function loadDashboard() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const level = (user?.learningLevel || "COLLEGE").toUpperCase()
      const res = await collegeApi.getHEDashboard(studentId, level)
      setDashboard(res.data || null)
    } catch {
      // silent
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />
  if (!dashboard) return <LoadingState />

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Student"
  const ctx = dashboard.academicContext || "COLLEGE"

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={`My Academic & Professional World — ${ctx}`} />

      {/* Row 1: Academic Context + What's Next + Today */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* Academic Context */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Academic Context</p>
          <p className="mt-1 text-lg font-bold text-foreground">{ctx}</p>
          {dashboard.academicYear && <p className="text-xs text-muted-foreground">{dashboard.academicYear} — {dashboard.semester}</p>}
        </div>

        {/* What's Next */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Sparkles className="size-4 text-primary" />
            <p className="text-xs font-medium text-muted-foreground">What's Next?</p>
          </div>
          {dashboard.whatsNext && dashboard.whatsNext.type !== "NONE" ? (
            <div>
              <p className="font-semibold text-foreground line-clamp-1">{dashboard.whatsNext.title}</p>
              <p className="text-xs text-muted-foreground line-clamp-1">{dashboard.whatsNext.description}</p>
              {dashboard.whatsNext.deadline && (
                <p className="mt-1 text-xs text-amber-600">Due {new Date(dashboard.whatsNext.deadline).toLocaleDateString()}</p>
              )}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">You&apos;re all caught up!</p>
          )}
        </div>

        {/* Today */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <CalendarDays className="size-4 text-blue-600" />
            <p className="text-xs font-medium text-muted-foreground">Today</p>
          </div>
          {dashboard.today && dashboard.today.totalTasks > 0 ? (
            <div>
              <p className="text-2xl font-extrabold text-foreground">{dashboard.today.pendingTasks}</p>
              <p className="text-xs text-muted-foreground">tasks pending, {dashboard.today.completedTasks} done</p>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No tasks for today</p>
          )}
        </div>
      </div>

      {/* Row 2: Continue Learning + Live Campus + Academic Load */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* Continue Learning */}
        {dashboard.continueLearning && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-2">
              <Play className="size-4 text-emerald-600" />
              <p className="text-xs font-medium text-muted-foreground">Continue Learning</p>
            </div>
            <p className="font-semibold text-foreground line-clamp-1">{dashboard.continueLearning.lastCourse}</p>
            {dashboard.continueLearning.lastModule && (
              <p className="text-xs text-muted-foreground line-clamp-1">{dashboard.continueLearning.lastModule}</p>
            )}
            <Link href="/dashboard/learner/study-planner" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Resume <ChevronRight className="size-3" />
            </Link>
          </div>
        )}

        {/* Live Campus */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Video className="size-4 text-red-600" />
            <p className="text-xs font-medium text-muted-foreground">Live Campus</p>
          </div>
          <p className="text-2xl font-extrabold text-foreground">{dashboard.liveCampus?.liveNow || 0}</p>
          <p className="text-xs text-muted-foreground">live sessions now</p>
          <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
            View All <ChevronRight className="size-3" />
          </Link>
        </div>

        {/* Academic Load */}
        {dashboard.academicLoad && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-2">
              <BookOpen className="size-4 text-violet-600" />
              <p className="text-xs font-medium text-muted-foreground">Academic Load</p>
            </div>
            <p className="text-2xl font-extrabold text-foreground">{dashboard.academicLoad.enrolledCourses}</p>
            <p className="text-xs text-muted-foreground">active courses, {dashboard.academicLoad.totalCreditHours} credits</p>
            {dashboard.academicLoad.cumulativeGpa != null && (
              <p className="mt-1 text-xs font-medium text-foreground">GPA: {dashboard.academicLoad.cumulativeGpa.toFixed(2)}</p>
            )}
          </div>
        )}
      </div>

      {/* Row 3: My Courses / Modules */}
      {dashboard.myCourses.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <FolderOpen className="size-4 text-primary" />
              <h3 className="font-semibold text-foreground">My Courses</h3>
            </div>
            <Link href="/dashboard/learner/my-learning" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              View All <ChevronRight className="size-3" />
            </Link>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {dashboard.myCourses.slice(0, 4).map((c) => (
              <div key={c.id} className="rounded-xl border border-border bg-muted/30 p-3">
                <p className="font-medium text-foreground text-sm line-clamp-1">{c.title}</p>
                <div className="mt-2 h-1.5 rounded-full bg-muted overflow-hidden">
                  <div className="h-full rounded-full bg-primary" style={{ width: `${c.progressPercent}%` }} />
                </div>
                <p className="mt-1 text-[10px] text-muted-foreground">{c.progressPercent}% complete</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Row 4: Projects + Research + My Progress */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* Projects */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Target className="size-4 text-amber-600" />
            <p className="text-xs font-medium text-muted-foreground">Projects</p>
          </div>
          <p className="text-2xl font-extrabold text-foreground">{dashboard.projects.length}</p>
          <p className="text-xs text-muted-foreground">total projects</p>
          <Link href="/dashboard/learner/projects" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
            View All <ChevronRight className="size-3" />
          </Link>
        </div>

        {/* Research */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <FlaskConical className="size-4 text-indigo-600" />
            <p className="text-xs font-medium text-muted-foreground">Research</p>
          </div>
          <p className="text-2xl font-extrabold text-foreground">{dashboard.research.length}</p>
          <p className="text-xs text-muted-foreground">research projects</p>
          <Link href="/dashboard/learner/research" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
            View All <ChevronRight className="size-3" />
          </Link>
        </div>

        {/* My Progress */}
        {dashboard.myProgress && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-2">
              <BarChart3 className="size-4 text-emerald-600" />
              <p className="text-xs font-medium text-muted-foreground">My Progress</p>
            </div>
            <div className="space-y-1.5">
              {dashboard.myProgress.cumulativeGpa != null && (
                <p className="text-sm"><span className="text-muted-foreground">GPA:</span> <span className="font-bold text-foreground">{dashboard.myProgress.cumulativeGpa.toFixed(2)}</span></p>
              )}
              <p className="text-sm"><span className="text-muted-foreground">Competencies:</span> <span className="font-bold text-foreground">{dashboard.myProgress.competenciesCompleted}/{dashboard.myProgress.competenciesTotal}</span></p>
              <p className="text-sm"><span className="text-muted-foreground">Projects:</span> <span className="font-bold text-foreground">{dashboard.myProgress.projectsCompleted}/{dashboard.myProgress.projectsTotal}</span></p>
              {dashboard.myProgress.academicStanding && (
                <p className="text-sm"><span className="text-muted-foreground">Standing:</span> <span className="font-bold text-foreground">{dashboard.myProgress.academicStanding}</span></p>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Row 5: My Evidence + Career World + Study Planner */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* My Evidence */}
        {dashboard.myEvidence && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-2">
              <Award className="size-4 text-rose-600" />
              <p className="text-xs font-medium text-muted-foreground">My Evidence</p>
            </div>
            <div className="space-y-1.5 text-sm">
              <p><span className="text-muted-foreground">Portfolio items:</span> <span className="font-bold text-foreground">{dashboard.myEvidence.portfolioItems}</span></p>
              <p><span className="text-muted-foreground">Demonstrations:</span> <span className="font-bold text-foreground">{dashboard.myEvidence.demonstrations}</span></p>
              <p><span className="text-muted-foreground">Project submissions:</span> <span className="font-bold text-foreground">{dashboard.myEvidence.projectSubmissions}</span></p>
              <p><span className="text-muted-foreground">Logbook entries:</span> <span className="font-bold text-foreground">{dashboard.myEvidence.logbookEntries}</span></p>
              <p><span className="text-muted-foreground">Competencies recorded:</span> <span className="font-bold text-foreground">{dashboard.myEvidence.competenciesRecorded}</span></p>
            </div>
            <Link href="/dashboard/learner/portfolio" className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View Portfolio <ChevronRight className="size-3" />
            </Link>
          </div>
        )}

        {/* Career World */}
        {dashboard.careerWorld && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-2">
              <Briefcase className="size-4 text-teal-600" />
              <p className="text-xs font-medium text-muted-foreground">Career & Professional World</p>
            </div>
            {dashboard.careerWorld.hasProfile ? (
              <div className="space-y-1.5 text-sm">
                {dashboard.careerWorld.targetRole && <p><span className="text-muted-foreground">Target:</span> <span className="font-bold text-foreground">{dashboard.careerWorld.targetRole}</span></p>}
                {dashboard.careerWorld.targetIndustry && <p><span className="text-muted-foreground">Industry:</span> <span className="font-bold text-foreground">{dashboard.careerWorld.targetIndustry}</span></p>}
                <p><span className="text-muted-foreground">Skills:</span> <span className="font-bold text-foreground">{dashboard.careerWorld.skillsCount}</span></p>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">Set up your career profile to get started.</p>
            )}
            <Link href="/dashboard/learner/career" className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              {dashboard.careerWorld.hasProfile ? "View Profile" : "Create Profile"} <ChevronRight className="size-3" />
            </Link>
          </div>
        )}

        {/* Study Planner */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <ClipboardList className="size-4 text-orange-600" />
            <p className="text-xs font-medium text-muted-foreground">Study Planner</p>
          </div>
          {dashboard.studyPlannerTasks.length > 0 ? (
            <div className="space-y-1.5">
              {dashboard.studyPlannerTasks.slice(0, 3).map((t) => (
                <div key={t.id} className="flex items-center gap-2 text-sm">
                  {t.isCompleted ? (
                    <CheckCircle2 className="size-3.5 text-emerald-500 shrink-0" />
                  ) : (
                    <AlertCircle className="size-3.5 text-amber-500 shrink-0" />
                  )}
                  <span className={`line-clamp-1 ${t.isCompleted ? "text-muted-foreground line-through" : "text-foreground"}`}>{t.title}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No tasks scheduled</p>
          )}
          <Link href="/dashboard/learner/study-planner" className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
            Open Planner <ChevronRight className="size-3" />
          </Link>
        </div>
      </div>

      {/* Row 6: My Day / My Week */}
      {dashboard.dayWeekView && (
        <div className="grid gap-4 sm:grid-cols-2">
          {/* Today */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-3">
              <Clock className="size-4 text-primary" />
              <h3 className="font-semibold text-foreground">My Day</h3>
            </div>
            {dashboard.dayWeekView.todayItems.length > 0 ? (
              <div className="space-y-2">
                {dashboard.dayWeekView.todayItems.map((item, i) => (
                  <div key={i} className="flex items-center gap-2 text-sm">
                    <span className="text-xs text-muted-foreground w-12 shrink-0">{item.time || "--:--"}</span>
                    <span className={`flex-1 line-clamp-1 ${item.status === "DONE" ? "text-muted-foreground line-through" : "text-foreground"}`}>{item.title}</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-muted text-muted-foreground">{item.type}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">Nothing scheduled for today</p>
            )}
          </div>

          {/* Week */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-3">
              <CalendarDays className="size-4 text-blue-600" />
              <h3 className="font-semibold text-foreground">My Week</h3>
            </div>
            {dashboard.dayWeekView.weekItems.length > 0 ? (
              <div className="space-y-2">
                {dashboard.dayWeekView.weekItems.slice(0, 5).map((item, i) => (
                  <div key={i} className="flex items-center gap-2 text-sm">
                    <span className="text-xs text-muted-foreground w-16 shrink-0">{item.date ? new Date(item.date).toLocaleDateString("en-US", { weekday: "short" }) : ""}</span>
                    <span className={`flex-1 line-clamp-1 ${item.status === "DONE" ? "text-muted-foreground line-through" : "text-foreground"}`}>{item.title}</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-muted text-muted-foreground">{item.type}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">Nothing scheduled this week</p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
