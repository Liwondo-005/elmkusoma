"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment, type CourseSummary, type LiveClass, type EventItem, type Announcement, type Bookmark as BookmarkType } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Sparkles, Clock, Play, Video, BookOpen, BarChart3, FolderOpen,
  Award, CalendarDays, ChevronRight, CheckCircle2, AlertCircle,
  TrendingUp, Library, Bookmark as BookmarkIcon, Film, Search, Bell, ExternalLink, FlaskConical,
  Trophy, Rss
} from "lucide-react"

export default function LearnerDashboardPage() {
  const { user, loading: authLoading } = useAuth()
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [events, setEvents] = useState<EventItem[]>([])
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [bookmarks, setBookmarks] = useState<BookmarkType[]>([])
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
      const [enrollRes, coursesRes, liveRes, eventsRes, announceRes, bookmarkRes] = await Promise.all([
        learnerApi.getEnrollments().catch(() => []),
        learnerApi.getCourses().catch(() => []),
        learnerApi.getLiveClasses().catch(() => []),
        learnerApi.getUpcomingEvents().catch(() => []),
        learnerApi.getAnnouncements().catch(() => []),
        learnerApi.getBookmarks().catch(() => []),
      ])
      setEnrollments(enrollRes)
      setCourses(coursesRes)
      setLiveClasses(liveRes)
      setEvents(eventsRes)
      setAnnouncements(announceRes)
      setBookmarks(bookmarkRes)
    } catch {
      setError("Failed to load dashboard data")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />
  if (!user) return <LoadingState />

  const firstName = user.firstName || user.name?.split(" ")[0] || "Learner"
  const inProgress = enrollments.filter((e) => !e.completedAt)
  const completed = enrollments.filter((e) => e.completedAt)
  const liveNow = liveClasses.filter((c) => c.status === "IN_PROGRESS" || c.status === "LIVE")
  const upcomingLive = liveClasses.filter((c) => c.status === "SCHEDULED")
  const continueLearning = enrollments.filter((e) => e.progressPercentage > 0 && e.progressPercentage < 100).slice(0, 3)
  const recommended = courses.filter((c) => !enrollments.some((e) => e.courseId === c.id)).slice(0, 4)

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={loadDashboard} className="ml-auto text-xs underline">Retry</button>
        </div>
      )}

      {/* Welcome / Learning Context */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-primary/10 p-6 shadow-xs">
        <LearnerHeader firstName={firstName} subtitle="My Learning World" />
        <div className="mt-3 flex flex-wrap gap-2">
          {inProgress.length > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2.5 py-1 text-xs font-medium text-blue-600">
              <BookOpen className="size-3" /> {inProgress.length} in progress
            </span>
          )}
          {liveNow.length > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full bg-red-500/10 px-2.5 py-1 text-xs font-medium text-red-600">
              <span className="size-1.5 rounded-full bg-red-500 animate-pulse" /> {liveNow.length} live now
            </span>
          )}
          {completed.length > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-600">
              <CheckCircle2 className="size-3" /> {completed.length} completed
            </span>
          )}
        </div>
      </div>

      {/* Signature tagline */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-teal/5 p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-lg font-bold tracking-tight text-foreground">Learn. Practice. Build. Prove.</p>
            <p className="text-xs text-muted-foreground mt-0.5">Your journey from knowledge to professional capability.</p>
          </div>
          <Link href="/dashboard/learner/modules" className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors">
            Continue Learning <ChevronRight className="size-3.5" />
          </Link>
        </div>
      </div>

      {/* What's Next + Continue Learning */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* What's Next */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Sparkles className="size-4 text-primary" />
            <p className="text-xs font-medium text-muted-foreground">What&apos;s Next?</p>
          </div>
          {continueLearning.length > 0 ? (
            <div>
              <p className="font-semibold text-foreground line-clamp-1">{continueLearning[0].courseTitle}</p>
              {continueLearning[0].courseDescription && (
                <p className="text-xs text-muted-foreground line-clamp-1">{continueLearning[0].courseDescription}</p>
              )}
              <div className="mt-2">
                <div className="h-1.5 rounded-full bg-muted overflow-hidden">
                  <div className="h-full rounded-full bg-primary" style={{ width: `${continueLearning[0].progressPercentage}%` }} />
                </div>
                <p className="mt-1 text-[10px] text-muted-foreground">{continueLearning[0].progressPercentage}% complete</p>
              </div>
              <Link href={`/dashboard/learner/courses/${continueLearning[0].courseId}`} className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                Resume <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : liveNow.length > 0 ? (
            <div>
              <p className="font-semibold text-foreground line-clamp-1">{liveNow[0].title}</p>
              <p className="text-xs text-red-600 font-medium">Live now</p>
              <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                Join <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : upcomingLive.length > 0 ? (
            <div>
              <p className="font-semibold text-foreground line-clamp-1">{upcomingLive[0].title}</p>
              <p className="text-xs text-muted-foreground">
                {new Date(upcomingLive[0].scheduledAt).toLocaleDateString("en-US", { weekday: "short", hour: "2-digit", minute: "2-digit" })}
              </p>
              <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                View <ChevronRight className="size-3" />
              </Link>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">You&apos;re all caught up!</p>
          )}
        </div>

        {/* Enrolled Courses Summary */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <FolderOpen className="size-4 text-emerald-600" />
            <p className="text-xs font-medium text-muted-foreground">My Enrolled Courses</p>
          </div>
          <p className="text-2xl font-extrabold text-foreground">{enrollments.length}</p>
          <p className="text-xs text-muted-foreground">{inProgress.length} in progress, {completed.length} completed</p>
          {enrollments.length > 0 && (
            <Link href="/dashboard/learner/my-learning" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ChevronRight className="size-3" />
            </Link>
          )}
        </div>

        {/* Live Learning */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Video className="size-4 text-red-600" />
            <p className="text-xs font-medium text-muted-foreground">Live Learning</p>
          </div>
          {liveNow.length > 0 ? (
            <div>
              <p className="text-2xl font-extrabold text-red-600">{liveNow.length}</p>
              <p className="text-xs text-muted-foreground">sessions live now</p>
              <div className="mt-2 space-y-1">
                {liveNow.slice(0, 2).map((s) => (
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
          ) : upcomingLive.length > 0 ? (
            <div>
              <p className="text-2xl font-extrabold text-foreground">{upcomingLive.length}</p>
              <p className="text-xs text-muted-foreground">upcoming sessions</p>
              <div className="mt-2 space-y-1">
                {upcomingLive.slice(0, 2).map((s) => (
                  <div key={s.id} className="flex items-center gap-1.5 text-xs">
                    <span className="size-1.5 rounded-full bg-muted-foreground/40 shrink-0" />
                    <span className="line-clamp-1 text-foreground">{s.title}</span>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                View All <ChevronRight className="size-3" />
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
      </div>

      {/* Continue Learning Cards */}
      {continueLearning.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Play className="size-4 text-emerald-600" />
              <h3 className="font-semibold text-foreground">Continue Learning</h3>
            </div>
            <Link href="/dashboard/learner/my-learning" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              View All <ChevronRight className="size-3" />
            </Link>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {continueLearning.map((enrollment) => (
              <Link
                key={enrollment.id}
                href={`/dashboard/learner/courses/${enrollment.courseId}`}
                className="rounded-xl border border-border bg-muted/30 p-4 transition-colors hover:bg-muted/50"
              >
                <p className="font-medium text-foreground text-sm line-clamp-1">{enrollment.courseTitle}</p>
                {enrollment.courseDescription && (
                  <p className="text-xs text-muted-foreground line-clamp-1 mt-0.5">{enrollment.courseDescription}</p>
                )}
                <div className="mt-3">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Progress</span>
                    <span className="font-semibold text-primary">{enrollment.progressPercentage}%</span>
                  </div>
                  <div className="mt-1.5 h-1.5 rounded-full bg-muted overflow-hidden">
                    <div className="h-full rounded-full bg-primary" style={{ width: `${enrollment.progressPercentage}%` }} />
                  </div>
                </div>
                <p className="mt-2 text-xs font-medium text-primary hover:underline">Resume</p>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Practical Learning */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <FlaskConical className="size-4 text-teal-600" />
            <h3 className="font-semibold text-foreground">Practical Learning</h3>
          </div>
          <Link href="/dashboard/learner/workshops" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
            Open Practical World <ChevronRight className="size-3" />
          </Link>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-xl border border-border bg-muted/30 p-3">
            <p className="text-xs text-muted-foreground">Enrolled Courses</p>
            <p className="text-lg font-bold text-foreground">{enrollments.length}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-3">
            <p className="text-xs text-muted-foreground">In Progress</p>
            <p className="text-lg font-bold text-foreground">{inProgress.length}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-3">
            <p className="text-xs text-muted-foreground">Live Sessions</p>
            <p className="text-lg font-bold text-foreground">{liveNow.length}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-3">
            <p className="text-xs text-muted-foreground">Completed</p>
            <p className="text-lg font-bold text-foreground">{completed.length}</p>
          </div>
        </div>
      </div>

      {/* My Courses + Browse Courses */}
      <div className="grid gap-4 sm:grid-cols-2">
        {/* Enrolled Courses List */}
        {enrollments.length > 0 && (
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
            <div className="space-y-2">
              {enrollments.slice(0, 5).map((e) => (
                <Link
                  key={e.id}
                  href={`/dashboard/learner/courses/${e.courseId}`}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <BookOpen className="size-5 text-primary/40" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{e.courseTitle}</p>
                    <div className="mt-1 h-1 w-full overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-primary" style={{ width: `${e.progressPercentage}%` }} />
                    </div>
                  </div>
                  <span className="shrink-0 text-xs text-muted-foreground">{e.progressPercentage}%</span>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Discover Courses */}
        {recommended.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <TrendingUp className="size-4 text-amber-600" />
                <h3 className="font-semibold text-foreground">Discover Courses</h3>
              </div>
              <Link href="/dashboard/learner/courses" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
                View All <ChevronRight className="size-3" />
              </Link>
            </div>
            <div className="space-y-2">
              {recommended.slice(0, 5).map((c) => (
                <Link
                  key={c.id}
                  href={`/dashboard/learner/courses/${c.id}`}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
                    <BookOpen className="size-5 text-amber-500/40" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{c.title}</p>
                    <p className="truncate text-xs text-muted-foreground">{c.level}{c.category ? ` · ${c.category}` : ""}</p>
                  </div>
                  <ExternalLink className="size-3.5 shrink-0 text-muted-foreground" />
                </Link>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Events + Announcements */}
      <div className="grid gap-4 sm:grid-cols-2">
        {/* Upcoming Events */}
        {events.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <CalendarDays className="size-4 text-blue-600" />
                <h3 className="font-semibold text-foreground">Upcoming Events</h3>
              </div>
              <Link href="/dashboard/learner/events" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
                View All <ChevronRight className="size-3" />
              </Link>
            </div>
            <div className="space-y-2">
              {events.slice(0, 4).map((ev) => (
                <Link
                  key={ev.id}
                  href={`/dashboard/learner/events/${ev.id}`}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10">
                    <CalendarDays className="size-5 text-blue-500/40" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{ev.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(ev.startsAt).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })}
                      {ev.location ? ` · ${ev.location}` : ""}
                    </p>
                  </div>
                  {ev.isRegistered && (
                    <span className="shrink-0 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-600">Registered</span>
                  )}
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Announcements */}
        {announcements.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-3">
              <Bell className="size-4 text-violet-600" />
              <h3 className="font-semibold text-foreground">Announcements</h3>
            </div>
            <div className="space-y-2">
              {announcements.slice(0, 4).map((a) => (
                <div key={a.id} className="rounded-xl border border-border p-3">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium text-foreground line-clamp-1">{a.title}</p>
                    {a.priority === "HIGH" && (
                      <span className="shrink-0 rounded-full bg-red-500/10 px-1.5 py-0.5 text-[10px] font-semibold text-red-600">High</span>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground line-clamp-1 mt-0.5">{a.content}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">
                    {new Date(a.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                    {a.authorName ? ` · ${a.authorName}` : ""}
                  </p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Bottom Row: Bookmarks + Progress + Certificates */}
      <div className="grid gap-4 sm:grid-cols-3">
        {/* Bookmarks */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <BookmarkIcon className="size-4 text-rose-600" />
              <p className="text-xs font-medium text-muted-foreground">Saved</p>
            </div>
            {bookmarks.length > 0 && (
              <Link href="/dashboard/learner/bookmarks" className="text-xs font-medium text-primary hover:underline">View All</Link>
            )}
          </div>
          <p className="text-2xl font-extrabold text-foreground">{bookmarks.length}</p>
          <p className="text-xs text-muted-foreground">{bookmarks.length === 1 ? "item saved" : "items saved"}</p>
        </div>

        {/* Progress */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <BarChart3 className="size-4 text-teal-600" />
            <p className="text-xs font-medium text-muted-foreground">Progress</p>
          </div>
          <div className="space-y-1.5">
            <p className="text-sm">
              <span className="text-muted-foreground">Enrolled: </span>
              <span className="font-bold text-foreground">{enrollments.length}</span>
            </p>
            <p className="text-sm">
              <span className="text-muted-foreground">Completed: </span>
              <span className="font-bold text-foreground">{completed.length}</span>
            </p>
            {enrollments.length > 0 && (
              <p className="text-sm">
                <span className="text-muted-foreground">Avg: </span>
                <span className="font-bold text-foreground">
                  {Math.round(enrollments.reduce((sum, e) => sum + e.progressPercentage, 0) / enrollments.length)}%
                </span>
              </p>
            )}
          </div>
        </div>

        {/* Certificates */}
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Award className="size-4 text-amber-600" />
            <p className="text-xs font-medium text-muted-foreground">Certificates</p>
          </div>
          <p className="text-2xl font-extrabold text-foreground">{completed.length}</p>
          <p className="text-xs text-muted-foreground">{completed.length === 1 ? "earned" : "earned to date"}</p>
          {completed.length > 0 && (
            <Link href="/dashboard/learner/certificates" className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View <ChevronRight className="size-3" />
            </Link>
          )}
        </div>
      </div>

      {/* Achievements (completed courses as real achievements) */}
      {completed.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Trophy className="size-4 text-amber-500" />
              <h3 className="font-semibold text-foreground">Achievements</h3>
            </div>
            <Link href="/dashboard/learner/certificates" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              View All <ChevronRight className="size-3" />
            </Link>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {completed.slice(0, 4).map((e) => (
              <div key={e.id} className="flex items-center gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-amber-500/10">
                  <Award className="size-5 text-amber-500" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{e.courseTitle}</p>
                  <p className="text-[10px] text-muted-foreground">
                    Completed {new Date(e.completedAt!).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recently Accessed (from enrollments with recent activity) */}
      {inProgress.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Clock className="size-4 text-blue-600" />
              <h3 className="font-semibold text-foreground">Recently Accessed</h3>
            </div>
            <Link href="/dashboard/learner/history" className="text-xs font-medium text-primary hover:underline flex items-center gap-1">
              View History <ChevronRight className="size-3" />
            </Link>
          </div>
          <div className="space-y-2">
            {inProgress.slice(0, 4).map((e) => (
              <Link
                key={e.id}
                href={`/dashboard/learner/courses/${e.courseId}`}
                className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10">
                  <BookOpen className="size-5 text-blue-500/40" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{e.courseTitle}</p>
                  <p className="text-xs text-muted-foreground">Last accessed recently</p>
                </div>
                <div className="shrink-0 text-right">
                  <p className="text-xs font-semibold text-primary">{e.progressPercentage}%</p>
                  <div className="mt-1 h-1 w-16 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-primary" style={{ width: `${e.progressPercentage}%` }} />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Learning Feed (unified feed of announcements + events + new courses) */}
      {(announcements.length > 0 || events.length > 0 || recommended.length > 0) && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-3">
            <Rss className="size-4 text-indigo-600" />
            <h3 className="font-semibold text-foreground">Learning Feed</h3>
          </div>
          <div className="space-y-2">
            {announcements.slice(0, 2).map((a) => (
              <div key={`ann-${a.id}`} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-violet-500/10">
                  <Bell className="size-4 text-violet-500" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground line-clamp-1">{a.title}</p>
                  <p className="text-xs text-muted-foreground line-clamp-1">{a.content}</p>
                </div>
                <span className="shrink-0 text-[10px] text-muted-foreground">
                  {new Date(a.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                </span>
              </div>
            ))}
            {events.slice(0, 2).map((ev) => (
              <Link
                key={`evt-${ev.id}`}
                href={`/dashboard/learner/events/${ev.id}`}
                className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-500/10">
                  <CalendarDays className="size-4 text-blue-500" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground line-clamp-1">{ev.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {new Date(ev.startsAt).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })}
                    {ev.location ? ` · ${ev.location}` : ""}
                  </p>
                </div>
                {ev.isRegistered && (
                  <span className="shrink-0 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-600">Registered</span>
                )}
              </Link>
            ))}
            {recommended.slice(0, 2).map((c) => (
              <Link
                key={`crs-${c.id}`}
                href={`/dashboard/learner/courses/${c.id}`}
                className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50"
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10">
                  <BookOpen className="size-4 text-emerald-500" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground line-clamp-1">{c.title}</p>
                  <p className="text-xs text-muted-foreground">{c.level}{c.category ? ` · ${c.category}` : ""}</p>
                </div>
                <ExternalLink className="size-3.5 shrink-0 text-muted-foreground" />
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Empty State for New Learners */}
      {enrollments.length === 0 && courses.length === 0 && (
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
