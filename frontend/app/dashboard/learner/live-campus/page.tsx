"use client"

import { useState, useEffect, useCallback } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { learnerApi } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Radio,
  Clock,
  Calendar,
  Video,
  Users,
  Play,
  ExternalLink,
  AlertCircle,
  ChevronRight,
  Filter,
  Search,
  BookOpen,
  Mic,
  Monitor,
  Award,
} from "lucide-react"

type LiveClassItem = {
  id: string
  title: string
  description?: string
  scheduledAt: string
  durationMinutes: number
  status: string
  maxParticipants?: number
  currentParticipants?: number
  subjectName?: string
  teacherName: string
  teacherId: string
  subjectId?: string
  classGroupId?: string
  recordingUrl?: string
  canJoin?: boolean
  recordingEnabled?: boolean
  sessionType: string
  timezone?: string
}

type SessionFilter = "all" | "live" | "upcoming" | "recordings"

const SESSION_TYPE_LABELS: Record<string, string> = {
  LECTURE: "Lecture",
  TUTORIAL: "Tutorial",
  WORKSHOP: "Workshop",
  SEMINAR: "Seminar",
  LAB_DEMO: "Lab Demo",
  WEBINAR: "Webinar",
  GUEST_SPEAKER: "Guest Speaker",
  RESEARCH_PRESENTATION: "Research Presentation",
  PROJECT_DEFENSE: "Project Defense",
  CONFERENCE: "Conference",
  PROFESSIONAL_TRAINING: "Professional Training",
  CAREER_EVENT: "Career Event",
}

const SESSION_TYPE_COLORS: Record<string, string> = {
  LECTURE: "bg-blue-500/10 text-blue-600",
  TUTORIAL: "bg-teal-500/10 text-teal-600",
  WORKSHOP: "bg-orange-500/10 text-orange-600",
  SEMINAR: "bg-purple-500/10 text-purple-600",
  LAB_DEMO: "bg-pink-500/10 text-pink-600",
  WEBINAR: "bg-cyan-500/10 text-cyan-600",
  GUEST_SPEAKER: "bg-emerald-500/10 text-emerald-600",
  RESEARCH_PRESENTATION: "bg-indigo-500/10 text-indigo-600",
  PROJECT_DEFENSE: "bg-amber-500/10 text-amber-600",
  CONFERENCE: "bg-rose-500/10 text-rose-600",
  PROFESSIONAL_TRAINING: "bg-violet-500/10 text-violet-600",
  CAREER_EVENT: "bg-lime-500/10 text-lime-600",
}

const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: "bg-blue-500/10 text-blue-600",
  STARTING: "bg-amber-500/10 text-amber-600",
  IN_PROGRESS: "bg-red-500/10 text-red-600",
  COMPLETED: "bg-green-500/10 text-green-600",
  ENDED: "bg-muted text-muted-foreground",
  CANCELLED: "bg-gray-500/10 text-gray-500",
}

function isToday(dateStr: string): boolean {
  const d = new Date(dateStr)
  const now = new Date()
  return (
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  )
}

function isThisWeek(dateStr: string): boolean {
  const d = new Date(dateStr)
  const now = new Date()
  const startOfWeek = new Date(now)
  startOfWeek.setDate(now.getDate() - now.getDay())
  startOfWeek.setHours(0, 0, 0, 0)
  const endOfWeek = new Date(startOfWeek)
  endOfWeek.setDate(startOfWeek.getDate() + 7)
  return d >= startOfWeek && d < endOfWeek
}

function isFuture(dateStr: string): boolean {
  return new Date(dateStr) > new Date()
}

function getDayLabel(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const tomorrow = new Date(now)
  tomorrow.setDate(now.getDate() + 1)

  if (
    d.getFullYear() === tomorrow.getFullYear() &&
    d.getMonth() === tomorrow.getMonth() &&
    d.getDate() === tomorrow.getDate()
  ) {
    return "Tomorrow"
  }

  return d.toLocaleDateString("en-US", { weekday: "long", month: "short", day: "numeric" })
}

function getTimeUntil(dateStr: string): string {
  const now = new Date()
  const target = new Date(dateStr)
  const diffMs = target.getTime() - now.getTime()

  if (diffMs <= 0) return "Starting now"

  const diffMin = Math.floor(diffMs / 60000)
  const hours = Math.floor(diffMin / 60)
  const mins = diffMin % 60

  if (hours > 24) {
    const days = Math.floor(hours / 24)
    return `Starts in ${days}d ${hours % 24}h`
  }
  if (hours > 0) return `Starts in ${hours}h ${mins}m`
  return `Starts in ${mins} minute${mins !== 1 ? "s" : ""}`
}

function getElapsedMinutes(dateStr: string): number {
  const now = new Date()
  const start = new Date(dateStr)
  return Math.max(0, Math.floor((now.getTime() - start.getTime()) / 60000))
}

function groupByDay(sessions: LiveClassItem[]): Record<string, LiveClassItem[]> {
  const grouped: Record<string, LiveClassItem[]> = {}
  for (const s of sessions) {
    const key = getDayLabel(s.scheduledAt)
    if (!grouped[key]) grouped[key] = []
    grouped[key].push(s)
  }
  return grouped
}

export default function LiveCampusPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()
  const [sessions, setSessions] = useState<LiveClassItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<SessionFilter>("all")
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState<string>("ALL")

  const loadSessions = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getLiveClasses()
      setSessions(data as unknown as LiveClassItem[])
    } catch {
      setError(tc("error"))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadSessions()
  }, [user, loadSessions])

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>
  }

  const liveNow = sessions.filter((s) => s.status === "IN_PROGRESS" || s.status === "STARTING")
  const scheduledToday = sessions.filter((s) => s.status === "SCHEDULED" && isToday(s.scheduledAt))
  const scheduledThisWeek = sessions.filter(
    (s) => s.status === "SCHEDULED" && isThisWeek(s.scheduledAt) && !isToday(s.scheduledAt) && isFuture(s.scheduledAt)
  )
  const recordings = sessions.filter((s) => s.status === "COMPLETED" && s.recordingUrl)
  const upcomingAll = sessions.filter(
    (s) => (s.status === "SCHEDULED" || s.status === "STARTING") && isFuture(s.scheduledAt)
  )

  const filteredSessions = sessions.filter((s) => {
    const matchesSearch =
      search === "" ||
      s.title.toLowerCase().includes(search.toLowerCase()) ||
      (s.subjectName || "").toLowerCase().includes(search.toLowerCase()) ||
      s.teacherName.toLowerCase().includes(search.toLowerCase())

    const matchesType = typeFilter === "ALL" || s.sessionType === typeFilter

    let matchesFilter = true
    if (filter === "live") matchesFilter = s.status === "IN_PROGRESS" || s.status === "STARTING"
    else if (filter === "upcoming") matchesFilter = s.status === "SCHEDULED"
    else if (filter === "recordings") matchesFilter = s.status === "COMPLETED" && !!s.recordingUrl

    return matchesSearch && matchesType && matchesFilter
  })

  function navigateToClass(id: string) {
    router.push(`/dashboard/learner/live-classes/${id}`)
  }

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6 pb-12">
      <LearnerHeader
        firstName={user?.firstName || "Student"}
        subtitle={t("subtitle.liveCampus")}
      />

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm text-destructive">
              <AlertCircle className="size-4 shrink-0" />
              {error}
            </div>
            <button
              onClick={loadSessions}
              aria-label={tc("retry")}
              className="text-xs font-medium text-destructive underline underline-offset-2 hover:no-underline"
            >
              {tc("retry")}
            </button>
          </div>
        </div>
      )}

      {loading ? (
        <div aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-red-200 bg-red-50/50 p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-red-500/10">
                  <Radio className="size-5 text-red-500" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{liveNow.length}</p>
                  <p className="text-xs text-muted-foreground">{t("live")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-blue-500/10">
                  <Clock className="size-5 text-blue-500" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{scheduledToday.length}</p>
                  <p className="text-xs text-muted-foreground">{t("filters.upcoming")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-indigo-500/10">
                  <Calendar className="size-5 text-indigo-500" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{scheduledThisWeek.length}</p>
                  <p className="text-xs text-muted-foreground">{t("filters.upcoming")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-green-500/10">
                  <Play className="size-5 text-green-500" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{recordings.length}</p>
                  <p className="text-xs text-muted-foreground">{t("filters.completed")}</p>
                </div>
              </div>
            </div>
          </div>

          {liveNow.length > 0 && (
            <section>
              <div className="mb-4 flex items-center gap-2">
                <span className="relative flex size-3">
                  <span className="absolute inline-flex size-full animate-ping rounded-full bg-red-400 opacity-75" />
                  <span className="relative inline-flex size-3 rounded-full bg-red-500" />
                </span>
                <h2 className="text-lg font-semibold text-foreground">{t("live")}</h2>
                <span className="rounded-full bg-red-500/10 px-2 py-0.5 text-xs font-semibold text-red-600">
                  {liveNow.length}
                </span>
              </div>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {liveNow.map((s) => (
                  <div
                    key={s.id}
                    className="rounded-2xl border border-red-200 bg-red-50/50 p-5 shadow-xs transition-shadow hover:shadow-md"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <h3 className="text-sm font-semibold text-foreground line-clamp-1">{s.title}</h3>
                        {s.subjectName && (
                          <p className="mt-0.5 text-xs text-muted-foreground">{s.subjectName}</p>
                        )}
                        <p className="mt-0.5 text-xs text-muted-foreground">{s.teacherName}</p>
                      </div>
                      <span className="shrink-0 rounded-full bg-red-500 px-2 py-0.5 text-[10px] font-bold text-white uppercase">
                        {t("live")}
                      </span>
                    </div>
                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      {s.sessionType && (
                        <span
                          className={`text-xs px-2 py-1 rounded-full ${SESSION_TYPE_COLORS[s.sessionType] || "bg-muted text-muted-foreground"}`}
                        >
                          {SESSION_TYPE_LABELS[s.sessionType] || s.sessionType}
                        </span>
                      )}
                      {s.currentParticipants !== undefined && (
                        <span className="flex items-center gap-1 text-xs text-muted-foreground">
                          <Users className="size-3" />
                          {s.currentParticipants}
                        </span>
                      )}
                      <span className="flex items-center gap-1 text-xs text-muted-foreground">
                        <Clock className="size-3" />
                        {getElapsedMinutes(s.scheduledAt)}m elapsed
                      </span>
                    </div>
                    <div className="mt-4 flex gap-2">
                      {s.canJoin && (
                        <button
                          onClick={() => navigateToClass(s.id)}
                          aria-label={t("joinSession")}
                          className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition-colors"
                        >
                          <Video className="size-4" />
                          {t("joinSession")}
                        </button>
                      )}
                      <button
                        onClick={() => navigateToClass(s.id)}
                        aria-label={t("viewDetails")}
                        className="flex items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
                      >
                        <ExternalLink className="size-4" />
                        {t("viewDetails")}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {scheduledToday.length > 0 && (
            <section>
              <div className="mb-4 flex items-center gap-2">
                <Clock className="size-5 text-blue-500" />
                <h2 className="text-lg font-semibold text-foreground">{t("filters.upcoming")}</h2>
                <span className="rounded-full bg-blue-500/10 px-2 py-0.5 text-xs font-semibold text-blue-600">
                  {scheduledToday.length}
                </span>
              </div>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {scheduledToday
                  .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime())
                  .map((s) => (
                    <div
                      key={s.id}
                      className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-shadow hover:shadow-md"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0 flex-1">
                          <h3 className="text-sm font-semibold text-foreground line-clamp-1">{s.title}</h3>
                          {s.subjectName && (
                            <p className="mt-0.5 text-xs text-muted-foreground">{s.subjectName}</p>
                          )}
                          <p className="mt-0.5 text-xs text-muted-foreground">{s.teacherName}</p>
                        </div>
                        <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${STATUS_COLORS[s.status] || "bg-muted text-muted-foreground"}`}>
                          {t("filters.upcoming")}
                        </span>
                      </div>
                      <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Calendar className="size-3" />
                          {new Date(s.scheduledAt).toLocaleTimeString("en-US", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                        <span className="flex items-center gap-1">
                          <Clock className="size-3" />
                          {s.durationMinutes}m
                        </span>
                      </div>
                      <div className="mt-2">
                        <span className="text-xs font-medium text-blue-600">{getTimeUntil(s.scheduledAt)}</span>
                      </div>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {s.sessionType && (
                          <span
                            className={`text-xs px-2 py-1 rounded-full ${SESSION_TYPE_COLORS[s.sessionType] || "bg-muted text-muted-foreground"}`}
                          >
                            {SESSION_TYPE_LABELS[s.sessionType] || s.sessionType}
                          </span>
                        )}
                      </div>
                      <div className="mt-4 flex gap-2">
                        <button
                          onClick={() => navigateToClass(s.id)}
                          aria-label={t("workshop")}
                          className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
                        >
                          <BookOpen className="size-4" />
                          {t("workshop")}
                        </button>
                        <button
                          onClick={() => navigateToClass(s.id)}
                          aria-label={t("viewDetails")}
                          className="flex items-center justify-center rounded-lg p-2 text-muted-foreground hover:bg-muted transition-colors"
                        >
                          <ChevronRight className="size-4" />
                        </button>
                      </div>
                    </div>
                  ))}
              </div>
            </section>
          )}

          {scheduledThisWeek.length > 0 && (
            <section>
              <div className="mb-4 flex items-center gap-2">
                <Calendar className="size-5 text-indigo-500" />
                <h2 className="text-lg font-semibold text-foreground">{t("filters.upcoming")}</h2>
                <span className="rounded-full bg-indigo-500/10 px-2 py-0.5 text-xs font-semibold text-indigo-600">
                  {scheduledThisWeek.length}
                </span>
              </div>
              {Object.entries(groupByDay(scheduledThisWeek)).map(([day, daySessions]) => (
                <div key={day} className="mb-4">
                  <h3 className="mb-3 text-sm font-semibold text-muted-foreground">{day}</h3>
                  <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {daySessions
                      .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime())
                      .map((s) => (
                        <div
                          key={s.id}
                          className="rounded-2xl border border-border bg-card p-4 shadow-xs transition-shadow hover:shadow-md"
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="min-w-0 flex-1">
                              <h4 className="text-sm font-semibold text-foreground line-clamp-1">{s.title}</h4>
                              {s.subjectName && (
                                <p className="mt-0.5 text-xs text-muted-foreground">{s.subjectName}</p>
                              )}
                            </div>
                          </div>
                          <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                            <span className="flex items-center gap-1">
                              <Calendar className="size-3" />
                              {new Date(s.scheduledAt).toLocaleTimeString("en-US", {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </span>
                            <span>{s.teacherName}</span>
                          </div>
                          <div className="mt-2">
                            {s.sessionType && (
                              <span
                                className={`text-xs px-2 py-1 rounded-full ${SESSION_TYPE_COLORS[s.sessionType] || "bg-muted text-muted-foreground"}`}
                              >
                                {SESSION_TYPE_LABELS[s.sessionType] || s.sessionType}
                              </span>
                            )}
                          </div>
                          <button
                            onClick={() => navigateToClass(s.id)}
                            aria-label={t("viewDetails")}
                            className="mt-3 w-full flex items-center justify-center gap-2 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
                          >
                            {t("viewDetails")}
                            <ChevronRight className="size-3" />
                          </button>
                        </div>
                      ))}
                  </div>
                </div>
              ))}
            </section>
          )}

          {recordings.length > 0 && (
            <section>
              <div className="mb-4 flex items-center gap-2">
                <Play className="size-5 text-green-500" />
                <h2 className="text-lg font-semibold text-foreground">{t("filters.completed")}</h2>
                <span className="rounded-full bg-green-500/10 px-2 py-0.5 text-xs font-semibold text-green-600">
                  {recordings.length}
                </span>
              </div>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {recordings.map((s) => (
                  <div
                    key={s.id}
                    className="group rounded-2xl border border-border bg-card p-5 shadow-xs transition-shadow hover:shadow-md"
                  >
                    <div className="relative mb-3 flex size-12 items-center justify-center rounded-xl bg-muted">
                      <Play className="size-5 text-muted-foreground group-hover:text-green-500 transition-colors" />
                    </div>
                    <div>
                      <h3 className="text-sm font-semibold text-foreground line-clamp-1">{s.title}</h3>
                      {s.subjectName && (
                        <p className="mt-0.5 text-xs text-muted-foreground">{s.subjectName}</p>
                      )}
                      <p className="mt-0.5 text-xs text-muted-foreground">{s.teacherName}</p>
                    </div>
                    <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Calendar className="size-3" />
                        {new Date(s.scheduledAt).toLocaleDateString()}
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock className="size-3" />
                        {s.durationMinutes}m
                      </span>
                    </div>
                    <button
                      onClick={() => navigateToClass(s.id)}
                      aria-label={t("viewDetails")}
                      className="mt-4 flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 transition-colors"
                    >
                      <Play className="size-4" />
                      {t("viewDetails")}
                    </button>
                  </div>
                ))}
              </div>
            </section>
          )}

          <section>
            <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-2">
                <Monitor className="size-5 text-foreground" />
                <h2 className="text-lg font-semibold text-foreground">{t("live")}</h2>
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
                  {filteredSessions.length}
                </span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                {(["all", "live", "upcoming", "recordings"] as SessionFilter[]).map((f) => (
                  <button
                    key={f}
                    onClick={() => setFilter(f)}
                    aria-pressed={filter === f}
                    aria-label={f === "all" ? tc("filter") : f}
                    className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                      filter === f
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground hover:bg-muted/80"
                    }`}
                  >
                    {f === "all" ? tc("filter") : f === "live" ? t("live") : f === "upcoming" ? t("filters.upcoming") : t("filters.completed")}
                  </button>
                ))}
              </div>
            </div>

            <div className="mb-4 flex flex-col gap-3 sm:flex-row">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="text"
                  placeholder={tc("search") + "..."}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  aria-label={tc("search")}
                  className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
                />
              </div>
              <div className="relative">
                <Filter className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <select
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value)}
                  aria-label={tc("filter")}
                  className="h-10 rounded-lg border border-border bg-background pl-10 pr-8 text-sm outline-none focus:border-ring appearance-none cursor-pointer"
                >
                  <option value="ALL">{tc("filter")}</option>
                  {Object.entries(SESSION_TYPE_LABELS).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {filteredSessions.length === 0 ? (
              <EmptyState
                icon={<Video className="size-8" />}
                title={t("empty.noLiveSessions")}
                description={
                  search || typeFilter !== "ALL" || filter !== "all"
                    ? tc("noResults")
                    : t("empty.noLiveSessions")
                }
              />
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {filteredSessions.map((s) => {
                  const isLive = s.status === "IN_PROGRESS" || s.status === "STARTING"
                  const isRecording = s.status === "COMPLETED" && s.recordingUrl
                  const isScheduled = s.status === "SCHEDULED"

                  return (
                    <div
                      key={s.id}
                      className={`rounded-2xl border bg-card p-5 shadow-xs transition-shadow hover:shadow-md ${
                        isLive ? "border-red-200 bg-red-50/30" : "border-border"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0 flex-1">
                          <h3 className="text-sm font-semibold text-foreground line-clamp-1">{s.title}</h3>
                          {s.subjectName && (
                            <p className="mt-0.5 text-xs text-muted-foreground">{s.subjectName}</p>
                          )}
                          <p className="mt-0.5 text-xs text-muted-foreground">{s.teacherName}</p>
                        </div>
                        <div className="flex shrink-0 items-center gap-1.5">
                          {isLive && (
                            <span className="relative flex size-2">
                              <span className="absolute inline-flex size-full animate-ping rounded-full bg-red-400 opacity-75" />
                              <span className="relative inline-flex size-2 rounded-full bg-red-500" />
                            </span>
                          )}
                          <span
                            className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${STATUS_COLORS[s.status] || "bg-muted text-muted-foreground"}`}
                          >
                            {s.status === "IN_PROGRESS" || s.status === "STARTING"
                              ? t("live")
                              : s.status === "SCHEDULED"
                                ? t("filters.upcoming")
                                : s.status === "COMPLETED"
                                  ? t("stats.completed")
                                  : s.status}
                          </span>
                        </div>
                      </div>

                      <div className="mt-3 space-y-1.5">
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <Calendar className="size-3 shrink-0" />
                          {new Date(s.scheduledAt).toLocaleDateString("en-US", {
                            weekday: "short",
                            month: "short",
                            day: "numeric",
                          })}
                          {" at "}
                          {new Date(s.scheduledAt).toLocaleTimeString("en-US", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </div>
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <Clock className="size-3 shrink-0" />
                          {s.durationMinutes} minutes
                        </div>
                        {s.currentParticipants !== undefined && (
                          <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            <Users className="size-3 shrink-0" />
                            {s.currentParticipants} participant{s.currentParticipants !== 1 ? "s" : ""}
                            {s.maxParticipants ? ` / ${s.maxParticipants}` : ""}
                          </div>
                        )}
                      </div>

                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {s.sessionType && (
                          <span
                            className={`text-xs px-2 py-1 rounded-full ${SESSION_TYPE_COLORS[s.sessionType] || "bg-muted text-muted-foreground"}`}
                          >
                            {SESSION_TYPE_LABELS[s.sessionType] || s.sessionType}
                          </span>
                        )}
                        {s.timezone && (
                          <span className="flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground">
                            {s.timezone}
                          </span>
                        )}
                      </div>

                      {s.description && (
                        <p className="mt-3 text-xs text-muted-foreground line-clamp-2">{s.description}</p>
                      )}

                      <div className="mt-4 flex gap-2">
                        {isLive && s.canJoin && (
                          <button
                            onClick={() => navigateToClass(s.id)}
                            aria-label={t("joinSession")}
                            className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition-colors"
                          >
                            <Video className="size-4" />
                            {t("joinSession")}
                          </button>
                        )}
                        {isRecording && (
                          <button
                            onClick={() => navigateToClass(s.id)}
                            aria-label={t("viewDetails")}
                            className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 transition-colors"
                          >
                            <Play className="size-4" />
                            {t("viewDetails")}
                          </button>
                        )}
                        {isScheduled && (
                          <button
                            onClick={() => navigateToClass(s.id)}
                            aria-label={t("workshop")}
                            className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
                          >
                            <BookOpen className="size-4" />
                            {t("workshop")}
                          </button>
                        )}
                        {!isLive && !isRecording && !isScheduled && (
                          <button
                            onClick={() => navigateToClass(s.id)}
                            aria-label={t("viewDetails")}
                            className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
                          >
                            <ExternalLink className="size-4" />
                            {t("viewDetails")}
                          </button>
                        )}
                        <button
                          onClick={() => navigateToClass(s.id)}
                          aria-label={t("viewDetails")}
                          className="flex items-center justify-center rounded-lg p-2 text-muted-foreground hover:bg-muted transition-colors"
                        >
                          <ChevronRight className="size-4" />
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}
