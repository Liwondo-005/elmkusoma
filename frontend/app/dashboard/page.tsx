"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, primaryApi, type DashboardSummary, type ContinueLearningItem, type RecentActivity } from "@/lib/api"
import { BookOpen, FileText, Loader2, PenTool, BarChart3, ArrowRight, Award, Users, TrendingUp, Clock, CheckCircle, Compass, Play, Star, Calendar, Video, ChevronRight, Sparkles, Lightbulb, Target, Map, GraduationCap } from "lucide-react"
import { getDashboardConfig, getLevelLabel, type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { LearnerHeader, ContinueLearningCard, LearningItemCard, AssignmentCard, ProgressCard, EmptyState, LoadingState } from "@/components/learner/shared"
import { GamificationBar } from "@/components/primary/gamification-bar"

interface DashboardData {
  totalLessons: number
  pendingAssignments: number
  assessments: number
  certificates: number
  recentLessons: { title: string; subject: string; progress: number; id?: string }[]
  pendingWork: { title: string; subject?: string; dueDate?: string; status: string; id?: string }[]
  recentActivity: { label: string; detail: string; time: string }[]
  liveClasses: { title: string; subject?: string; scheduledAt?: string; status: string; id?: string }[]
  subjects: { name: string; lessonCount: number; progress: number }[]
}

export default function DashboardPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [continueItems, setContinueItems] = useState<ContinueLearningItem[]>([])
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [loading, setLoading] = useState(true)
  const firstName = user?.name?.split(" ")[0] || "Student"
  const level = user?.learningLevel as LearningLevel | null
  const config = getDashboardConfig(level)
  const isPrimary = level?.toUpperCase() === "PRIMARY"
  const [data, setData] = useState<DashboardData>({
    totalLessons: 0,
    pendingAssignments: 0,
    assessments: 0,
    certificates: 0,
    recentLessons: [],
    pendingWork: [],
    recentActivity: [],
    liveClasses: [],
    subjects: [],
  })
  const [gamification, setGamification] = useState({ streak: 0, points: 0, badgeCount: 0, loading: true })

  useEffect(() => {
    if (!authLoading && user?.role === "Parent") {
      router.replace("/dashboard/parent")
    } else if (!authLoading && (user?.role === "Teacher" || user?.role === "Instructor")) {
      router.replace("/dashboard/teacher")
    } else if (!authLoading && (user?.role === "Admin" || user?.role === "Institution Admin")) {
      router.replace("/dashboard/admin")
    } else if (!authLoading && user?.role === "Other Learner") {
      router.replace("/dashboard/learner")
    } else if (!authLoading && user?.role === "National Admin") {
      router.replace("/dashboard/national")
    } else if (!authLoading && user?.role === "Regional Admin") {
      router.replace("/dashboard/regional")
    } else if (!authLoading && user?.role === "District Admin") {
      router.replace("/dashboard/district")
    } else if (!authLoading && user?.role === "Student" && (user?.learningLevel || "").toUpperCase() === "NURSERY") {
      router.replace("/dashboard/nursery")
    } else if (!authLoading && user?.role === "Student" && (user?.learningLevel || "").toUpperCase() === "SECONDARY") {
      router.replace("/dashboard/secondary")
    } else if (!authLoading && user?.role === "Student" && ((user?.learningLevel || "").toUpperCase() === "COLLEGE" || (user?.learningLevel || "").toUpperCase() === "UNIVERSITY" || (user?.learningLevel || "").toUpperCase() === "VETA")) {
      router.replace("/dashboard/learner")
    }
  }, [user, authLoading, router])

  useEffect(() => {
    if (!user || user.role === "Parent" || user.role === "Teacher" || user.role === "Admin" || user.role === "Institution Admin") return
    loadDashboard()
  }, [user])

  async function loadDashboard() {
    try {
      setLoading(true)
      const [summaryData, continueData, activityData] = await Promise.all([
        dashboardApi.getSummary().catch(() => null),
        dashboardApi.getContinueLearning().catch(() => []),
        dashboardApi.getRecentActivity().catch(() => []),
      ])
      setSummary(summaryData)
      setContinueItems(continueData)
      setActivities(activityData)

      try {
        const { studentApi, learningApi, assessmentApi, certificateApi, academicApi } = await import("@/lib/api")
        const institutionId = localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001"

        const students = await studentApi.getStudents(institutionId).catch(() => [])
        const student = students.find((s: any) => s.userId === user?.id || s.email === user?.email)

        if ((student as any)?.classGroupId) {
          try {
            const classGroup = await academicApi.getClassGroup((student as any).classGroupId)
            if (classGroup?.gradeId) {
              const grade = await academicApi.getGrade(classGroup.gradeId)
              if (grade?.educationLevel) {
                localStorage.setItem("elmkusoma_education_level", grade.educationLevel)
              }
            }
          } catch { /* education level stays default */ }
        }

        const classId = user?.classGroupId || (student as any)?.classGroupId || ""

        const [assignments, assessments, certificates, lessons, liveClasses] = await Promise.all([
          classId ? learningApi.getAssignments(classId).catch(() => []) : Promise.resolve([]),
          classId ? assessmentApi.getByClass(classId).catch(() => []) : Promise.resolve([]),
          certificateApi.list().catch(() => []),
          classId ? learningApi.getLessonsByClass(classId).catch(() => []) : Promise.resolve([]),
          dashboardApi.getLiveClasses().catch(() => []),
        ])

        const lessonList = (Array.isArray(lessons) ? lessons : []).map((l: any) => ({
          title: l.title || "Untitled Lesson",
          subject: l.subjectName || "",
          progress: l.completionPercentage || 0,
          id: l.id,
        }))

        const assignmentList = (Array.isArray(assignments) ? assignments : []).slice(0, 5).map((a: any) => ({
          title: a.title || "Assignment",
          subject: a.subjectName || "",
          dueDate: a.dueDate,
          status: a.status || "PENDING",
          id: a.id,
        }))

        const liveClassList = (Array.isArray(liveClasses) ? liveClasses : []).slice(0, 3).map((lc: any) => ({
          title: lc.title || "Live Class",
          subject: lc.subjectName || "",
          scheduledAt: lc.scheduledAt,
          status: lc.status || "SCHEDULED",
          id: lc.id,
        }))

        setData({
          totalLessons: lessonList.length,
          pendingAssignments: assignmentList.filter((a) => a.status === "PENDING" || a.status === "ACTIVE").length,
          assessments: Array.isArray(assessments) ? assessments.length : 0,
          certificates: Array.isArray(certificates) ? certificates.length : 0,
          recentLessons: lessonList.slice(0, 6),
          pendingWork: assignmentList,
          recentActivity: lessonList.slice(0, 3).map((l) => ({
            label: "Lesson available",
            detail: l.title,
            time: "",
          })),
          liveClasses: liveClassList,
          subjects: [],
        })
      } catch { /* loads with zero data */ }

      // Fetch gamification data for primary students
      try {
        const [badges, streak] = await Promise.all([
          primaryApi.getBadges().catch(() => []),
          primaryApi.getStreak().catch(() => ({ currentStreak: 0, longestStreak: 0, totalPoints: 0 })),
        ])
        setGamification({
          streak: streak.currentStreak || 0,
          points: streak.totalPoints || 0,
          badgeCount: Array.isArray(badges) ? badges.length : 0,
          loading: false,
        })
      } catch {
        setGamification((prev) => ({ ...prev, loading: false }))
      }
    } catch {
      // Dashboard data unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading || user?.role === "Parent" || user?.role === "Teacher" || user?.role === "Admin" || user?.role === "Institution Admin" || user?.role === "National Admin" || user?.role === "Regional Admin" || user?.role === "District Admin") {
    return <LoadingState />
  }

  const levelLabel = getLevelLabel(level)

  if (isPrimary) {
    return <PrimaryDashboard firstName={firstName} config={config} summary={summary} continueItems={continueItems} activities={activities} data={data} gamification={gamification} />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} level={levelLabel} subtitle={config.subtitle} />

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <ProgressCard label="Enrolled Courses" value={summary?.activeEnrollments ?? 0} />
        <ProgressCard label="Lessons Completed" value={summary?.completedLessons ?? 0} />
        <ProgressCard label="Attendance Rate" value={summary?.monthAttendanceRate ?? 0} />
        <ProgressCard label="Overall Average" value={summary?.overallAverage ?? 0} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Continue Learning */}
        <div className="lg:col-span-2">
          {continueItems.length > 0 ? (
            <div className="space-y-3">
              {continueItems.map((item) => (
                <Link
                  key={item.lessonId}
                  href={`/dashboard/lessons/${item.lessonId}`}
                  className="flex items-center gap-4 rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
                >
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <Clock className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">Lesson {item.lessonId.slice(0, 8)}</p>
                    <p className="text-xs text-muted-foreground">
                      {item.startedAt ? new Date(item.startedAt).toLocaleDateString() : "Recently started"}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-primary">{Math.round(item.completionPercentage)}%</p>
                    <div className="mt-1 h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-primary" style={{ width: `${item.completionPercentage}%` }} />
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : data.recentLessons.length > 0 ? (
            <ContinueLearningCard
              title={data.recentLessons[0].title}
              subject={data.recentLessons[0].subject}
              progress={data.recentLessons[0].progress}
              onResume={() => data.recentLessons[0].id && router.push("/dashboard/lessons")}
            />
          ) : (
            <ContinueLearningCard title={undefined} onResume={() => router.push("/dashboard/lessons")} />
          )}
        </div>

        {/* Quick Links */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Quick Links</h2>
          <div className="mt-4 space-y-2">
            {config.navItems.slice(1, 5).map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
              >
                <item.icon className="size-4 shrink-0 text-muted-foreground" />
                <span className="flex-1">{item.label}</span>
                <ArrowRight className="size-3 shrink-0 text-muted-foreground" />
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* Today's Learning */}
      {data.recentLessons.length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-foreground">Today&apos;s Learning</h2>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {data.recentLessons.map((lesson, i) => (
              <LearningItemCard
                key={i}
                title={lesson.title}
                subject={lesson.subject}
                icon={<BookOpen className="size-4" />}
              />
            ))}
          </div>
        </section>
      )}

      {/* Recent Activity */}
      {activities.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Recent Activity</h2>
          <div className="space-y-3">
            {activities.map((activity, i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className={`flex size-8 items-center justify-center rounded-lg ${
                  activity.type === "lesson_completed" ? "bg-green-500/10" : "bg-blue-500/10"
                }`}>
                  {activity.type === "lesson_completed" ? (
                    <CheckCircle className="size-4 text-green-600" />
                  ) : (
                    <Clock className="size-4 text-blue-600" />
                  )}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-foreground">
                    {activity.type === "lesson_completed" ? "Completed a lesson" : "Attendance recorded"}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {activity.completedAt
                      ? new Date(activity.completedAt).toLocaleDateString()
                      : activity.date
                        ? new Date(activity.date).toLocaleDateString()
                        : ""}
                  </p>
                </div>
                {activity.status && (
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                    activity.status === "PRESENT" ? "bg-green-100 text-green-700" :
                    activity.status === "ABSENT" ? "bg-red-100 text-red-700" :
                    "bg-yellow-100 text-yellow-700"
                  }`}>
                    {activity.status}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty state if no data */}
      {data.totalLessons === 0 && continueItems.length === 0 && activities.length === 0 && (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title={config.emptyStateTitle}
          description={config.emptyStateDescription}
          action={
            <Link href="/dashboard/lessons" className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              Browse Lessons <ArrowRight className="size-4" />
            </Link>
          }
        />
      )}
    </div>
  )
}

function PrimaryDashboard({
  firstName,
  config,
  summary,
  continueItems,
  activities,
  data,
  gamification,
}: {
  firstName: string
  config: ReturnType<typeof getDashboardConfig>
  summary: DashboardSummary | null
  continueItems: ContinueLearningItem[]
  activities: RecentActivity[]
  data: DashboardData
  gamification: { streak: number; points: number; badgeCount: number; loading: boolean }
}) {
  const router = useRouter()
  const hour = new Date().getHours()
  const greetingTime = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening"
  const [teacherInfo, setTeacherInfo] = useState<{ count: number; firstName: string } | null>(null)

  useEffect(() => {
    primaryApi.getTeachers().then((teachers) => {
      if (teachers && teachers.length > 0) {
        setTeacherInfo({ count: teachers.length, firstName: teachers[0].firstName })
      }
    }).catch(() => {})
  }, [])

  return (
    <div className="mx-auto max-w-6xl space-y-6" role="main" aria-label="My Learning World">
      {/* Gamification Bar */}
      <GamificationBar streak={gamification.streak} points={gamification.points} badgeCount={gamification.badgeCount} loading={gamification.loading} />

      {/* Teacher Info */}
      {teacherInfo && (
        <Link
          href="/dashboard/my-teacher"
          className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
            <GraduationCap className="size-5 text-blue-600" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium text-foreground">Your Teacher: {teacherInfo.firstName}</p>
            <p className="text-xs text-muted-foreground">{teacherInfo.count} {teacherInfo.count === 1 ? "teacher" : "teachers"} assigned</p>
          </div>
          <ChevronRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-1" />
        </Link>
      )}

      {/* Primary Welcome */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-teal/5 p-6 shadow-xs">
        <div className="flex items-start gap-4">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
            <Sparkles className="size-6 text-primary" />
          </div>
          <div className="flex-1">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              {greetingTime}, {firstName}!
            </h1>
            <p className="mt-1 text-muted-foreground">
              {config.subtitle}
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
                <Star className="size-3" /> Keep learning every day!
              </span>
              {summary && summary.activeEnrollments > 0 && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-teal/10 px-3 py-1 text-xs font-semibold text-teal">
                  <BookOpen className="size-3" /> {summary.activeEnrollments} {summary.activeEnrollments === 1 ? "subject" : "subjects"} enrolled
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Continue Learning - Hero Section */}
      {continueItems.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-foreground">Continue Learning</h2>
            <Link href="/dashboard/lessons" className="text-sm font-medium text-primary hover:underline flex items-center gap-1">
              View all <ChevronRight className="size-3" />
            </Link>
          </div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {continueItems.slice(0, 3).map((item) => (
              <Link
                key={item.lessonId}
                href={`/dashboard/lessons/${item.lessonId}`}
                className="group flex flex-col rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-center gap-3">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <Play className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">Lesson {item.lessonId.slice(0, 8)}</p>
                    <p className="text-xs text-muted-foreground">
                      {item.startedAt ? new Date(item.startedAt).toLocaleDateString() : "In progress"}
                    </p>
                  </div>
                </div>
                <div className="mt-3">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Progress</span>
                    <span className="font-semibold text-primary">{Math.round(item.completionPercentage)}%</span>
                  </div>
                  <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-primary to-teal transition-all"
                      style={{ width: `${item.completionPercentage}%` }}
                    />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* My Subjects */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-foreground">My Subjects</h2>
          <Link href="/dashboard/lessons" className="text-sm font-medium text-primary hover:underline flex items-center gap-1">
            View all <ChevronRight className="size-3" />
          </Link>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {primarySubjects.map((subject) => {
            const Icon = subject.icon
            return (
              <Link
                key={subject.name}
                href="/dashboard/lessons"
                className="group flex flex-col items-center gap-3 rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className={`flex size-12 items-center justify-center rounded-2xl ${subject.bgColor}`}>
                  <Icon className={`size-6 ${subject.color}`} />
                </div>
                <div className="text-center">
                  <p className="text-sm font-semibold text-foreground">{subject.name}</p>
                  <p className="text-xs text-muted-foreground mt-0.5">{subject.description}</p>
                </div>
              </Link>
            )
          })}
        </div>
      </div>

      {/* Two Column: Upcoming + Live */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Upcoming Work */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Upcoming</h2>
          {data.pendingWork.length > 0 ? (
            <div className="space-y-3">
              {data.pendingWork.slice(0, 4).map((work, i) => (
                <Link
                  key={i}
                  href={work.id ? `/dashboard/assignments` : "#"}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted"
                >
                  <div className="flex size-8 items-center justify-center rounded-lg bg-amber-50">
                    <FileText className="size-4 text-amber-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{work.title}</p>
                    <p className="text-xs text-muted-foreground">{work.subject || "Assignment"}</p>
                  </div>
                  {work.dueDate && (
                    <span className="text-xs text-muted-foreground">
                      {new Date(work.dueDate).toLocaleDateString()}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 text-center">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-muted">
                <Calendar className="size-6 text-muted-foreground" />
              </div>
              <p className="mt-3 text-sm font-medium text-foreground">All caught up!</p>
              <p className="text-xs text-muted-foreground">No upcoming assignments right now.</p>
            </div>
          )}
        </div>

        {/* Live Learning */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-foreground">Live Learning</h2>
            <Link href="/dashboard/live-classes" className="text-sm font-medium text-primary hover:underline flex items-center gap-1">
              View all <ChevronRight className="size-3" />
            </Link>
          </div>
          {data.liveClasses.length > 0 ? (
            <div className="space-y-3">
              {data.liveClasses.map((lc, i) => (
                <Link
                  key={i}
                  href={lc.id ? `/dashboard/live-classes` : "#"}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted"
                >
                  <div className={`flex size-8 items-center justify-center rounded-lg ${
                    lc.status === "LIVE" || lc.status === "IN_PROGRESS"
                      ? "bg-red-50 animate-pulse"
                      : "bg-blue-50"
                  }`}>
                    <Video className={`size-4 ${
                      lc.status === "LIVE" || lc.status === "IN_PROGRESS"
                        ? "text-red-600"
                        : "text-blue-600"
                    }`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{lc.title}</p>
                    <p className="text-xs text-muted-foreground">{lc.subject || "Live Class"}</p>
                  </div>
                  {lc.scheduledAt && (
                    <span className="text-xs text-muted-foreground">
                      {new Date(lc.scheduledAt).toLocaleDateString()}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 text-center">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-muted">
                <Video className="size-6 text-muted-foreground" />
              </div>
              <p className="mt-3 text-sm font-medium text-foreground">No live classes scheduled</p>
              <p className="text-xs text-muted-foreground">Your teacher will schedule live sessions soon.</p>
            </div>
          )}
        </div>
      </div>

      {/* Learning Progress */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <ProgressCard label="Subjects Enrolled" value={summary?.activeEnrollments ?? 0} />
        <ProgressCard label="Lessons Done" value={summary?.completedLessons ?? 0} />
        <ProgressCard label="Attendance" value={summary?.monthAttendanceRate ?? 0} />
        <ProgressCard label="Average Score" value={summary?.overallAverage ?? 0} />
      </div>

      {/* Learning Passport */}
      <Link
        href="/dashboard/passport"
        className="group flex items-center gap-4 rounded-2xl border-2 border-amber-200 bg-gradient-to-br from-amber-50 via-white to-orange-50 p-5 shadow-xs transition-all hover:shadow-md hover:border-amber-300"
      >
        <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100">
          <Map className="size-6 text-amber-600" />
        </div>
        <div className="flex-1">
          <h3 className="text-base font-semibold text-foreground">Learning Passport</h3>
          <p className="text-xs text-muted-foreground">Your learning adventure around the world!</p>
        </div>
        <div className="text-right">
          <p className="text-sm font-bold text-amber-600">Tanzania</p>
          <p className="text-[10px] text-muted-foreground">Current Stage</p>
        </div>
        <ChevronRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-1" />
      </Link>

      {/* Smart Revision - Based on real data */}
      {data.pendingWork.length > 0 && (
        <div className="rounded-2xl border border-border bg-gradient-to-br from-amber-50/50 to-card p-6 shadow-xs">
          <div className="flex items-center gap-2 mb-4">
            <Lightbulb className="size-5 text-amber-600" />
            <h2 className="text-lg font-semibold text-foreground">Suggested for You</h2>
          </div>
          <p className="text-xs text-muted-foreground mb-3">
            Based on your learning, here is what might help you next.
          </p>
          <div className="space-y-2">
            {data.pendingWork.slice(0, 2).map((work, i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl bg-amber-50/80 border border-amber-200/50 p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-amber-100">
                  <Target className="size-4 text-amber-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground">{work.title}</p>
                  <p className="text-xs text-muted-foreground">Practice this to improve your skills</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Teacher Feedback / Recent Activity */}
      {activities.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Recent Activity</h2>
          <div className="space-y-3">
            {activities.map((activity, i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className={`flex size-8 items-center justify-center rounded-lg ${
                  activity.type === "lesson_completed" ? "bg-green-500/10" : "bg-blue-500/10"
                }`}>
                  {activity.type === "lesson_completed" ? (
                    <CheckCircle className="size-4 text-green-600" />
                  ) : (
                    <Clock className="size-4 text-blue-600" />
                  )}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-foreground">
                    {activity.type === "lesson_completed" ? "Completed a lesson" : "Attendance recorded"}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {activity.completedAt
                      ? new Date(activity.completedAt).toLocaleDateString()
                      : activity.date
                        ? new Date(activity.date).toLocaleDateString()
                        : ""}
                  </p>
                </div>
                {activity.status && (
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                    activity.status === "PRESENT" ? "bg-green-100 text-green-700" :
                    activity.status === "ABSENT" ? "bg-red-100 text-red-700" :
                    "bg-yellow-100 text-yellow-700"
                  }`}>
                    {activity.status}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {data.totalLessons === 0 && continueItems.length === 0 && activities.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Sparkles className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{config.emptyStateTitle}</h3>
          <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">{config.emptyStateDescription}</p>
          <Link
            href="/dashboard/lessons"
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Start Learning <ArrowRight className="size-4" />
          </Link>
        </div>
      )}
    </div>
  )
}
