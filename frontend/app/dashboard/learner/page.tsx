"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import { learnerApi, type CourseSummary as LearnerCourseSummary, type Bookmark as BookmarkType, getLastAccessedLesson } from "@/lib/learner-api"
import type {
  HigherEducationDashboard,
  CourseSummary,
  StudyTask,
  ResearchProject,
  LiveSessionSummary,
  TodayItem,
  DayItem,
} from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Sparkles, Clock, Play, Video, BookOpen, BarChart3, FolderOpen,
  Award, CalendarDays, ChevronRight, CheckCircle2, AlertCircle,
  TrendingUp, Target, FileText, Briefcase, GraduationCap, Lightbulb,
  FlaskConical, Layers, Search, Bell, ExternalLink, MapPin,
  Trophy, Rss, BookMarked, PenTool, Star, Bookmark as BookmarkIcon, Library, Film
} from "lucide-react"

export default function LearnerDashboardPage() {
  const { user, loading: authLoading } = useAuth()
  const isHE = user?.learningLevel === "UNIVERSITY" || user?.learningLevel === "COLLEGE"
  const [dashboard, setDashboard] = useState<HigherEducationDashboard | null>(null)
  const [generalCourses, setGeneralCourses] = useState<LearnerCourseSummary[]>([])
  const [bookmarks, setBookmarks] = useState<BookmarkType[]>([])
  const [continueState, setContinueState] = useState<ReturnType<typeof getLastAccessedLesson>>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadDashboard()
  }, [user])

  async function loadDashboard() {
    if (!user) return
    try {
      setLoading(true)
      setError(null)
      if (isHE) {
        const res = await collegeApi.getHEDashboard(user.id, user.learningLevel || "UNIVERSITY")
        setDashboard(res.data ?? null)
      } else {
        const [coursesRes, bookmarkRes] = await Promise.allSettled([
          learnerApi.getCourses(),
          learnerApi.getBookmarks(),
        ])
        if (coursesRes.status === "fulfilled") setGeneralCourses(coursesRes.value)
        if (bookmarkRes.status === "fulfilled") setBookmarks(bookmarkRes.value)
        setContinueState(getLastAccessedLesson())
      }
    } catch {
      setError("Failed to load dashboard data")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />
  if (!user) return <LoadingState />
  if (isHE && !dashboard) return <LoadingState />

  const firstName = user.firstName || user.name?.split(" ")[0] || "Learner"
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const dash = dashboard!

  if (!isHE) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        {error && (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
            <button onClick={loadDashboard} className="ml-auto text-xs underline">Retry</button>
          </div>
        )}
        <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-primary/10 p-6 shadow-xs">
          <LearnerHeader firstName={firstName} subtitle="My Learning World" />
        </div>
        {continueState && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-2">
              <Play className="size-4 text-emerald-600" />
              <p className="text-xs font-medium text-muted-foreground">Continue Learning</p>
            </div>
            <p className="font-semibold text-foreground text-sm line-clamp-1">{continueState.lessonTitle}</p>
            <p className="text-xs text-muted-foreground line-clamp-1 mt-0.5">{continueState.courseTitle}</p>
            <Link href={`/dashboard/learner/courses/${continueState.courseId}/lessons/${continueState.lessonId}`} className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Resume <ChevronRight className="size-3" />
            </Link>
          </div>
        )}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <BookOpen className="size-4 text-primary" />
              <h3 className="font-semibold text-foreground">My Courses</h3>
            </div>
            <Link href="/dashboard/learner/courses" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              Browse More <ChevronRight className="size-3" />
            </Link>
          </div>
          {generalCourses.length > 0 ? (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {generalCourses.slice(0, 6).map((course) => (
                <Link key={course.id} href={`/dashboard/learner/courses/${course.id}`} className="rounded-xl border border-border bg-muted/30 p-4 transition-colors hover:bg-muted/50">
                  <p className="font-medium text-foreground text-sm line-clamp-1">{course.title}</p>
                  <p className="text-xs text-muted-foreground line-clamp-1 mt-1">{course.description || "No description"}</p>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState icon={<BookOpen className="size-6 text-muted-foreground" />} title="No courses yet" description="Enroll in courses to start your learning journey." action={<Link href="/dashboard/learner/courses" className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">Explore Courses</Link>} />
          )}
        </div>
        {bookmarks.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <BookmarkIcon className="size-4 text-rose-600" />
                <h3 className="font-semibold text-foreground">Saved Items</h3>
              </div>
              <Link href="/dashboard/learner/bookmarks" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
                View All <ChevronRight className="size-3" />
              </Link>
            </div>
            <div className="space-y-2">
              {bookmarks.slice(0, 5).map((b) => (
                <Link key={b.id} href={b.targetType === "COURSE" ? `/dashboard/learner/courses/${b.targetId}` : `/dashboard/learner/resources/${b.targetId}`} className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{b.targetTitle}</p>
                    <p className="text-xs text-muted-foreground">{b.targetType}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        )}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Link href="/dashboard/learner/resources" className="flex items-center gap-3 rounded-xl border border-border bg-card p-4 shadow-xs hover:bg-muted/50">
            <Library className="size-5 text-primary" />
            <span className="text-sm font-medium">Resources</span>
          </Link>
          <Link href="/dashboard/learner/search" className="flex items-center gap-3 rounded-xl border border-border bg-card p-4 shadow-xs hover:bg-muted/50">
            <Search className="size-5 text-amber-600" />
            <span className="text-sm font-medium">Search</span>
          </Link>
          <Link href="/dashboard/learner/certificates" className="flex items-center gap-3 rounded-xl border border-border bg-card p-4 shadow-xs hover:bg-muted/50">
            <Award className="size-5 text-emerald-600" />
            <span className="text-sm font-medium">Certificates</span>
          </Link>
          <Link href="/dashboard/learner/replays" className="flex items-center gap-3 rounded-xl border border-border bg-card p-4 shadow-xs hover:bg-muted/50">
            <Film className="size-5 text-red-600" />
            <span className="text-sm font-medium">Replays</span>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={loadDashboard} className="ml-auto text-xs underline">Retry</button>
        </div>
      )}

      {/* Welcome / Academic Context */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-primary/10 p-6 shadow-xs">
        <LearnerHeader
          firstName={firstName}
          subtitle={dash.academicContext || "My Learning World"}
        />
        <div className="mt-3 flex flex-wrap gap-2">
          {dash.programmeName && (
            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
              <GraduationCap className="size-3" /> {dash.programmeName}
            </span>
          )}
          {dash.departmentName && (
            <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2.5 py-1 text-xs font-medium text-blue-600">
              <BookOpen className="size-3" /> {dash.departmentName}
            </span>
          )}
          {dash.semester && (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-600">
              <CalendarDays className="size-3" /> {dash.semester}
            </span>
          )}
          {dash.academicYear && (
            <span className="inline-flex items-center gap-1 rounded-full bg-violet-500/10 px-2.5 py-1 text-xs font-medium text-violet-600">
              <Target className="size-3" /> {dash.academicYear}
            </span>
          )}
        </div>
      </div>

      {/* What's Next */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-teal/5 p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="size-4 text-primary" />
            <h3 className="text-sm font-semibold text-foreground">What&apos;s Next?</h3>
          </div>
          {dash.whatsNext?.url && (
            <Link href={dash.whatsNext.url} className="inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline">
              Open <ChevronRight className="size-3.5" />
            </Link>
          )}
        </div>
        {dash.whatsNext ? (
          <div className="mt-2">
            <p className="font-semibold text-foreground line-clamp-1">{dash.whatsNext.title}</p>
            <p className="text-xs text-muted-foreground line-clamp-2 mt-0.5">{dash.whatsNext.description}</p>
            <div className="mt-2 flex items-center gap-2">
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                {dash.whatsNext.type}
              </span>
              {dash.whatsNext.deadline && (
                <span className="text-[10px] text-muted-foreground">
                  Due {new Date(dash.whatsNext.deadline).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                </span>
              )}
            </div>
            {!dash.whatsNext.url && (
              <p className="mt-2 text-xs text-muted-foreground italic">No action required right now.</p>
            )}
          </div>
        ) : (
          <p className="mt-2 text-sm text-muted-foreground">You&apos;re all caught up!</p>
        )}
      </div>

      {/* Today's Tasks */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Clock className="size-4 text-amber-600" />
            <h3 className="text-sm font-semibold text-foreground">Today&apos;s Tasks</h3>
          </div>
          {dash.today && dash.today.totalTasks > 0 && (
            <span className="text-xs text-muted-foreground">
              {dash.today.completedTasks}/{dash.today.totalTasks} done
            </span>
          )}
        </div>
        {dash.today && dash.today.items.length > 0 ? (
          <div className="space-y-2">
            {dash.today.items.slice(0, 5).map((item, idx) => (
              <TodayTaskRow key={idx} item={item} />
            ))}
          </div>
        ) : (
          <div className="py-4 text-center">
            <p className="text-sm text-muted-foreground">No tasks for today</p>
            <Link href="/dashboard/learner/courses" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Browse Courses <ChevronRight className="size-3" />
            </Link>
          </div>
        )}
        {dash.today && dash.today.pendingTasks > 0 && (
          <div className="mt-3 flex items-center gap-2">
            <div className="h-1.5 flex-1 rounded-full bg-muted overflow-hidden">
              <div
                className="h-full rounded-full bg-amber-500 transition-all"
                style={{ width: `${(dash.today.completedTasks / dash.today.totalTasks) * 100}%` }}
              />
            </div>
            <span className="text-[10px] text-muted-foreground">{dash.today.pendingTasks} pending</span>
          </div>
        )}
      </div>

      {/* Continue Learning + Live Campus */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* Continue Learning */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Play className="size-4 text-emerald-600" />
            <p className="text-xs font-medium text-muted-foreground">Continue Learning</p>
          </div>
          {dash.continueLearning && dash.continueLearning.lastCourse ? (
            <div>
              <p className="font-semibold text-foreground text-sm line-clamp-1">{dash.continueLearning.lastCourse}</p>
              {dash.continueLearning.lastModule && (
                <p className="text-xs text-muted-foreground line-clamp-1 mt-0.5">{dash.continueLearning.lastModule}</p>
              )}
              <div className="mt-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Progress</span>
                  <span className="font-semibold text-primary">{dash.continueLearning.progressPercent}%</span>
                </div>
                <div className="mt-1.5 h-1.5 rounded-full bg-muted overflow-hidden">
                  <div className="h-full rounded-full bg-primary" style={{ width: `${dash.continueLearning.progressPercent}%` }} />
                </div>
              </div>
              <Link
                href={dash.continueLearning.courseId ? `/dashboard/learner/courses/${dash.continueLearning.courseId}` : "/dashboard/learner/courses"}
                className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
              >
                Resume <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : (
            <div>
              <p className="text-sm text-muted-foreground">No course in progress</p>
              <Link href="/dashboard/learner/courses" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                Start Learning <ChevronRight className="size-3" />
              </Link>
            </div>
          )}
        </div>

        {/* Live Campus */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Video className="size-4 text-red-600" />
            <p className="text-xs font-medium text-muted-foreground">Live Campus</p>
          </div>
          {dash.liveCampus && dash.liveCampus.liveNow > 0 ? (
            <div>
              <p className="text-2xl font-extrabold text-red-600">{dash.liveCampus.liveNow}</p>
              <p className="text-xs text-muted-foreground">sessions live now</p>
              <div className="mt-2 space-y-1">
                {dash.liveCampus.sessions.filter(s => s.status === "LIVE").slice(0, 2).map((s) => (
                  <div key={s.id} className="flex items-center gap-1.5 text-xs">
                    <span className="size-1.5 rounded-full bg-red-500 animate-pulse shrink-0" />
                    <span className="line-clamp-1 text-foreground">{s.title}</span>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                Join <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : dash.liveCampus && dash.liveCampus.upcomingToday > 0 ? (
            <div>
              <p className="text-2xl font-extrabold text-foreground">{dash.liveCampus.upcomingToday}</p>
              <p className="text-xs text-muted-foreground">upcoming today</p>
              <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                View Schedule <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : (
            <div>
              <p className="text-2xl font-extrabold text-foreground">0</p>
              <p className="text-xs text-muted-foreground">no live sessions right now</p>
              <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                Browse Classes <ChevronRight className="size-3" />
              </Link>
            </div>
          )}
        </div>

        {/* Academic Load */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <BarChart3 className="size-4 text-teal-600" />
            <p className="text-xs font-medium text-muted-foreground">Academic Load</p>
          </div>
          {dash.academicLoad ? (
            <div className="space-y-1.5">
              {dash.academicLoad.cumulativeGpa != null && (
                <p className="text-sm">
                  <span className="text-muted-foreground">Cumulative GPA: </span>
                  <span className="font-bold text-foreground">{dash.academicLoad.cumulativeGpa.toFixed(2)}</span>
                </p>
              )}
              {dash.academicLoad.currentSemesterGpa != null && (
                <p className="text-sm">
                  <span className="text-muted-foreground">Semester GPA: </span>
                  <span className="font-bold text-foreground">{dash.academicLoad.currentSemesterGpa.toFixed(2)}</span>
                </p>
              )}
              <p className="text-sm">
                <span className="text-muted-foreground">Enrolled: </span>
                <span className="font-bold text-foreground">{dash.academicLoad.enrolledCourses}</span>
              </p>
              <p className="text-sm">
                <span className="text-muted-foreground">Credits: </span>
                <span className="font-bold text-foreground">{dash.academicLoad.completedCreditHours}/{dash.academicLoad.totalCreditHours}</span>
              </p>
              <Link href="/dashboard/learner/academic" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                View Record <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : (
            <div>
              <p className="text-sm text-muted-foreground">No academic data yet</p>
              <Link href="/dashboard/learner/courses" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                Enroll in Courses <ChevronRight className="size-3" />
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* My Courses */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <BookOpen className="size-4 text-primary" />
            <h3 className="font-semibold text-foreground">My Courses</h3>
          </div>
          <Link href="/dashboard/learner/courses" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
            Browse More <ChevronRight className="size-3" />
          </Link>
        </div>
        {dash.myCourses.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {dash.myCourses.slice(0, 6).map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        ) : (
          <EmptyState
            icon={<BookOpen className="size-6 text-muted-foreground" />}
            title="No courses yet"
            description="Enroll in courses to start your learning journey."
            action={
              <Link href="/dashboard/learner/courses" className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                Explore Courses <ChevronRight className="size-3.5" />
              </Link>
            }
          />
        )}
      </div>

      {/* Research + Projects */}
      <div className="grid gap-4 sm:grid-cols-2">
        {/* Research */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <FlaskConical className="size-4 text-violet-600" />
              <h3 className="font-semibold text-foreground">Research</h3>
            </div>
            <Link href="/dashboard/learner/research" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              View All <ChevronRight className="size-3" />
            </Link>
          </div>
          {dash.research.length > 0 ? (
            <div className="space-y-2">
              {dash.research.slice(0, 4).map((r) => (
                <Link
                  key={r.id}
                  href={`/dashboard/learner/research/${r.id}`}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-violet-500/10">
                    <FlaskConical className="size-5 text-violet-500/40" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{r.title}</p>
                    {r.researchQuestion && (
                      <p className="truncate text-xs text-muted-foreground">{r.researchQuestion}</p>
                    )}
                  </div>
                  {r.status && (
                    <span className="shrink-0 rounded-full bg-violet-500/10 px-2 py-0.5 text-[10px] font-semibold text-violet-600">{r.status}</span>
                  )}
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={<FlaskConical className="size-5 text-muted-foreground" />}
              title="No research projects"
              description="Start a research project to explore your academic interests."
              action={
                <Link href="/dashboard/learner/research" className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                  Start Research <ChevronRight className="size-3.5" />
                </Link>
              }
            />
          )}
        </div>

        {/* Projects */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Layers className="size-4 text-amber-600" />
              <h3 className="font-semibold text-foreground">Projects</h3>
            </div>
            <Link href="/dashboard/learner/projects" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              View All <ChevronRight className="size-3" />
            </Link>
          </div>
          {dash.projects.length > 0 ? (
            <div className="space-y-2">
              {dash.projects.slice(0, 4).map((p) => (
                <Link
                  key={p.id}
                  href={`/dashboard/learner/projects/${p.id}`}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
                    <Layers className="size-5 text-amber-500/40" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{p.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {p.completedModules}/{p.totalModules} modules
                    </p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-xs font-semibold text-primary">{p.progressPercent}%</p>
                    <div className="mt-1 h-1 w-16 overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-primary" style={{ width: `${p.progressPercent}%` }} />
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={<Layers className="size-5 text-muted-foreground" />}
              title="No projects yet"
              description="Work on projects to build practical skills."
              action={
                <Link href="/dashboard/learner/projects" className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                  Browse Projects <ChevronRight className="size-3.5" />
                </Link>
              }
            />
          )}
        </div>
      </div>

      {/* My Progress */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <TrendingUp className="size-4 text-teal-600" />
            <h3 className="font-semibold text-foreground">My Progress</h3>
          </div>
          <Link href="/dashboard/learner/progress" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
            View Details <ChevronRight className="size-3" />
          </Link>
        </div>
        {dash.myProgress ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {dash.myProgress.cumulativeGpa != null && (
              <div className="rounded-xl border border-border bg-muted/30 p-3">
                <p className="text-xs text-muted-foreground">Cumulative GPA</p>
                <p className="text-lg font-bold text-foreground">{dash.myProgress.cumulativeGpa.toFixed(2)}</p>
              </div>
            )}
            {dash.myProgress.semesterGpa != null && (
              <div className="rounded-xl border border-border bg-muted/30 p-3">
                <p className="text-xs text-muted-foreground">Semester GPA</p>
                <p className="text-lg font-bold text-foreground">{dash.myProgress.semesterGpa.toFixed(2)}</p>
              </div>
            )}
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Competencies</p>
              <p className="text-lg font-bold text-foreground">{dash.myProgress.competenciesCompleted}/{dash.myProgress.competenciesTotal}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Projects</p>
              <p className="text-lg font-bold text-foreground">{dash.myProgress.projectsCompleted}/{dash.myProgress.projectsTotal}</p>
            </div>
            {dash.myProgress.academicStanding && (
              <div className="sm:col-span-2 lg:col-span-4 rounded-xl border border-border bg-muted/30 p-3 flex items-center gap-2">
                <Award className="size-4 text-amber-500" />
                <p className="text-sm font-medium text-foreground">{dash.myProgress.academicStanding}</p>
              </div>
            )}
          </div>
        ) : (
          <div className="py-4 text-center">
            <p className="text-sm text-muted-foreground">No progress data yet</p>
            <Link href="/dashboard/learner/courses" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Start Learning <ChevronRight className="size-3" />
            </Link>
          </div>
        )}
      </div>

      {/* My Evidence */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <FileText className="size-4 text-rose-600" />
            <h3 className="font-semibold text-foreground">My Evidence</h3>
          </div>
          <Link href="/dashboard/learner/portfolio" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
            View Portfolio <ChevronRight className="size-3" />
          </Link>
        </div>
        {dash.myEvidence ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Portfolio Items</p>
              <p className="text-lg font-bold text-foreground">{dash.myEvidence.portfolioItems}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Demonstrations</p>
              <p className="text-lg font-bold text-foreground">{dash.myEvidence.demonstrations}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Project Submissions</p>
              <p className="text-lg font-bold text-foreground">{dash.myEvidence.projectSubmissions}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Logbook Entries</p>
              <p className="text-lg font-bold text-foreground">{dash.myEvidence.logbookEntries}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Competencies Recorded</p>
              <p className="text-lg font-bold text-foreground">{dash.myEvidence.competenciesRecorded}</p>
            </div>
          </div>
        ) : (
          <div className="py-4 text-center">
            <p className="text-sm text-muted-foreground">No evidence recorded yet</p>
            <Link href="/dashboard/learner/portfolio" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              Start Building Portfolio <ChevronRight className="size-3" />
            </Link>
          </div>
        )}
      </div>

      {/* Career World */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Briefcase className="size-4 text-blue-600" />
            <h3 className="font-semibold text-foreground">Career World</h3>
          </div>
          <Link href="/dashboard/learner/career" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
            {dash.careerWorld?.hasProfile ? "View Profile" : "Create Profile"} <ChevronRight className="size-3" />
          </Link>
        </div>
        {dash.careerWorld && dash.careerWorld.hasProfile ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {dash.careerWorld.careerObjective && (
              <div className="rounded-xl border border-border bg-muted/30 p-3 sm:col-span-2 lg:col-span-4">
                <p className="text-xs text-muted-foreground">Career Objective</p>
                <p className="text-sm font-medium text-foreground line-clamp-2">{dash.careerWorld.careerObjective}</p>
              </div>
            )}
            {dash.careerWorld.targetIndustry && (
              <div className="rounded-xl border border-border bg-muted/30 p-3">
                <p className="text-xs text-muted-foreground">Target Industry</p>
                <p className="text-sm font-bold text-foreground">{dash.careerWorld.targetIndustry}</p>
              </div>
            )}
            {dash.careerWorld.targetRole && (
              <div className="rounded-xl border border-border bg-muted/30 p-3">
                <p className="text-xs text-muted-foreground">Target Role</p>
                <p className="text-sm font-bold text-foreground">{dash.careerWorld.targetRole}</p>
              </div>
            )}
            <div className="rounded-xl border border-border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">Skills</p>
              <p className="text-sm font-bold text-foreground">{dash.careerWorld.skillsCount}</p>
            </div>
          </div>
        ) : (
          <div className="py-4 text-center">
            <p className="text-sm text-muted-foreground">Set up your career profile to get personalized recommendations</p>
            <Link href="/dashboard/learner/career" className="mt-3 inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              Create Profile <ChevronRight className="size-3.5" />
            </Link>
          </div>
        )}
      </div>

      {/* Study Planner */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Lightbulb className="size-4 text-amber-500" />
            <h3 className="font-semibold text-foreground">Study Planner</h3>
          </div>
          <Link href="/dashboard/learner/study-planner" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
            Manage Tasks <ChevronRight className="size-3" />
          </Link>
        </div>
        {dash.studyPlannerTasks.length > 0 ? (
          <div className="space-y-2">
            {dash.studyPlannerTasks.slice(0, 5).map((task) => (
              <StudyTaskRow key={task.id} task={task} />
            ))}
          </div>
        ) : (
          <EmptyState
            icon={<Lightbulb className="size-5 text-muted-foreground" />}
            title="No study tasks"
            description="Create study tasks to plan your learning sessions."
            action={
              <Link href="/dashboard/learner/study-planner" className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                Create Task <ChevronRight className="size-3.5" />
              </Link>
            }
          />
        )}
      </div>

      {/* Day/Week View */}
      {dash.dayWeekView && (dash.dayWeekView.todayItems.length > 0 || dash.dayWeekView.weekItems.length > 0) && (
        <div className="grid gap-4 sm:grid-cols-2">
          {/* Today */}
          {dash.dayWeekView.todayItems.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <CalendarDays className="size-4 text-primary" />
                <h3 className="font-semibold text-foreground">Today</h3>
              </div>
              <div className="space-y-2">
                {dash.dayWeekView.todayItems.slice(0, 5).map((item, idx) => (
                  <DayItemRow key={idx} item={item} />
                ))}
              </div>
            </div>
          )}
          {/* This Week */}
          {dash.dayWeekView.weekItems.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <CalendarDays className="size-4 text-blue-600" />
                <h3 className="font-semibold text-foreground">This Week</h3>
              </div>
              <div className="space-y-2">
                {dash.dayWeekView.weekItems.slice(0, 5).map((item, idx) => (
                  <DayItemRow key={idx} item={item} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Empty State for New Learners */}
      {dash.myCourses.length === 0 && dash.research.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-primary/10">
            <BookOpen className="size-8 text-primary/40" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">Welcome to your learning world</h3>
          <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
            Start exploring courses, enroll in what interests you, and begin your learning journey.
          </p>
          <Link
            href="/dashboard/learner/courses"
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            Explore Courses <ChevronRight className="size-4" />
          </Link>
        </div>
      )}
    </div>
  )
}

function CourseCard({ course }: { course: CourseSummary }) {
  return (
    <Link
      href={`/dashboard/learner/courses/${course.id}`}
      className="rounded-xl border border-border bg-muted/30 p-4 transition-colors hover:bg-muted/50"
    >
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <p className="font-medium text-foreground text-sm line-clamp-1">{course.title}</p>
          <p className="text-xs text-muted-foreground mt-0.5">{course.completedModules}/{course.totalModules} modules</p>
        </div>
        <span className="shrink-0 text-xs font-semibold text-primary">{course.progressPercent}%</span>
      </div>
      <div className="mt-3">
        <div className="h-1.5 rounded-full bg-muted overflow-hidden">
          <div className="h-full rounded-full bg-primary" style={{ width: `${course.progressPercent}%` }} />
        </div>
      </div>
    </Link>
  )
}

function TodayTaskRow({ item }: { item: TodayItem }) {
  const statusIcon = item.status === "COMPLETED" ? (
    <CheckCircle2 className="size-4 text-emerald-500" />
  ) : (
    <div className="size-4 rounded-full border-2 border-muted-foreground/30" />
  )
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border p-3">
      {statusIcon}
      <div className="min-w-0 flex-1">
        <p className={`text-sm font-medium line-clamp-1 ${item.status === "COMPLETED" ? "text-muted-foreground line-through" : "text-foreground"}`}>{item.title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-1.5 py-0.5 text-[10px] font-medium text-primary">{item.type}</span>
          {item.time && <span className="text-[10px] text-muted-foreground">{item.time}</span>}
        </div>
      </div>
    </div>
  )
}

function StudyTaskRow({ task }: { task: StudyTask }) {
  const priorityColors: Record<string, string> = {
    LOW: "bg-muted text-muted-foreground",
    MEDIUM: "bg-blue-500/10 text-blue-600",
    HIGH: "bg-amber-500/10 text-amber-600",
    URGENT: "bg-red-500/10 text-red-600",
  }
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border p-3">
      <div className={`size-2 rounded-full shrink-0 ${task.isCompleted ? "bg-emerald-500" : task.priority === "URGENT" ? "bg-red-500 animate-pulse" : task.priority === "HIGH" ? "bg-amber-500" : "bg-muted-foreground/40"}`} />
      <div className="min-w-0 flex-1">
        <p className={`text-sm font-medium line-clamp-1 ${task.isCompleted ? "text-muted-foreground line-through" : "text-foreground"}`}>{task.title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="inline-flex items-center rounded-full bg-primary/10 px-1.5 py-0.5 text-[10px] font-medium text-primary">{task.taskType}</span>
          <span className={`inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-medium ${priorityColors[task.priority] || "bg-muted text-muted-foreground"}`}>{task.priority}</span>
          {task.scheduledDate && (
            <span className="text-[10px] text-muted-foreground">
              {new Date(task.scheduledDate).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}

function DayItemRow({ item }: { item: DayItem }) {
  const statusColor = item.status === "COMPLETED"
    ? "bg-emerald-500/10 text-emerald-600"
    : item.status === "IN_PROGRESS"
      ? "bg-blue-500/10 text-blue-600"
      : "bg-muted text-muted-foreground"
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border p-3">
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground line-clamp-1">{item.title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="inline-flex items-center rounded-full bg-primary/10 px-1.5 py-0.5 text-[10px] font-medium text-primary">{item.type}</span>
          {item.time && <span className="text-[10px] text-muted-foreground">{item.time}</span>}
          {item.date && <span className="text-[10px] text-muted-foreground">{item.date}</span>}
        </div>
      </div>
      <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusColor}`}>{item.status}</span>
    </div>
  )
}
