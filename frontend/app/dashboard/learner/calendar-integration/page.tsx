"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { useRouter } from "next/navigation"
import { collegeApi } from "@/lib/college-api"
import { learnerApi } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Clock,
  BookOpen,
  Video,
  FileText,
  Target,
  Plus,
  Filter,
  List,
  Grid3X3,
  CalendarDays,
  AlertCircle,
} from "lucide-react"

type ViewMode = "month" | "week"
type FilterType = "ALL" | "LIVE_SESSION" | "DEADLINE" | "PROJECT" | "RESEARCH"

interface CalendarEvent {
  id: string
  title: string
  date: string
  time?: string
  endTime?: string
  type: "LIVE_SESSION" | "DEADLINE" | "PROJECT" | "RESEARCH" | "STUDY_TASK" | "CAREER"
  location?: string
  link?: string
  duration?: number
  description?: string
}

const EVENT_COLORS: Record<string, { bg: string; text: string; dot: string }> = {
  LIVE_SESSION: { bg: "bg-red-100", text: "text-red-700", dot: "bg-red-500" },
  DEADLINE: { bg: "bg-amber-100", text: "text-amber-700", dot: "bg-amber-500" },
  PROJECT: { bg: "bg-cyan-100", text: "text-cyan-700", dot: "bg-cyan-500" },
  RESEARCH: { bg: "bg-purple-100", text: "text-purple-700", dot: "bg-purple-500" },
  STUDY_TASK: { bg: "bg-blue-100", text: "text-blue-700", dot: "bg-blue-500" },
  CAREER: { bg: "bg-emerald-100", text: "text-emerald-700", dot: "bg-emerald-500" },
}



const WEEKDAYS_FALLBACK = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

function getFirstDayOfMonth(year: number, month: number) {
  const day = new Date(year, month, 1).getDay()
  return day === 0 ? 6 : day - 1
}

function formatDateKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

function EventIcon({ type }: { type: string }) {
  const iconMap: Record<string, typeof Calendar> = {
    LIVE_SESSION: Video,
    DEADLINE: FileText,
    PROJECT: Target,
    RESEARCH: BookOpen,
    STUDY_TASK: BookOpen,
    CAREER: Calendar,
  }
  const Icon = iconMap[type] || Calendar
  return <Icon className="size-3.5" />
}

function EventTypeBadge({ type }: { type: string }) {
  const t = useTranslations("learner")
  const colors = EVENT_COLORS[type] || { bg: "bg-muted", text: "text-muted-foreground" }
  const labels: Record<string, string> = { LIVE_SESSION: t("cal.typeLive"), DEADLINE: t("cal.typeDeadline"), PROJECT: t("cal.typeProject"), RESEARCH: t("cal.typeResearch"), STUDY_TASK: t("cal.typeStudy"), CAREER: t("cal.typeCareer") }
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${colors.bg} ${colors.text}`}>
      <EventIcon type={type} />
      {labels[type] || type}
    </span>
  )
}

export default function CalendarIntegrationPage() {
  const t = useTranslations("learner")
  const FILTER_OPTIONS: { key: FilterType; label: string }[] = [
  { key: "ALL", label: t("cal.filterAll") },
  { key: "LIVE_SESSION", label: t("cal.filterLive") },
  { key: "DEADLINE", label: t("cal.filterDeadlines") },
  { key: "PROJECT", label: t("cal.filterProjects") },
  { key: "RESEARCH", label: t("cal.filterResearch") },
]
  const EVENT_LABELS: Record<string, string> = {
  LIVE_SESSION: t("cal.typeLive"),
  DEADLINE: t("cal.typeDeadline"),
  PROJECT: t("cal.typeProject"),
  RESEARCH: t("cal.typeResearch"),
  STUDY_TASK: t("cal.typeStudy"),
  CAREER: t("cal.typeCareer"),
}
  const WEEKDAYS = [t("cal.wdMon"), t("cal.wdTue"), t("cal.wdWed"), t("cal.wdThu"), t("cal.wdFri"), t("cal.wdSat"), t("cal.wdSun")]
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()

  const [events, setEvents] = useState<CalendarEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>("month")
  const [filterType, setFilterType] = useState<FilterType>("ALL")
  const [currentDate, setCurrentDate] = useState(new Date())
  const [selectedDate, setSelectedDate] = useState<Date | null>(null)

  const loadEvents = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      setError(null)

      const [liveClassesRes, projectsRes, researchRes] = await Promise.allSettled([
        learnerApi.getLiveClasses(),
        collegeApi.getStudentProjects(user.id),
        collegeApi.getStudentResearch(user.id),
      ])

      const allEvents: CalendarEvent[] = []

      if (liveClassesRes.status === "fulfilled") {
        const classes = Array.isArray(liveClassesRes.value) ? liveClassesRes.value : []
        for (const cls of classes) {
          if (cls.scheduledAt) {
            const d = new Date(cls.scheduledAt)
            allEvents.push({
              id: `live-${cls.id}`,
              title: cls.title,
              date: formatDateKey(d),
              time: d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
              type: "LIVE_SESSION",
              location: cls.subjectName || undefined,
              link: `/dashboard/learner/live-classes/${cls.id}`,
              duration: cls.durationMinutes || undefined,
              description: cls.description || undefined,
            })
          }
        }
      }

      if (projectsRes.status === "fulfilled") {
        const projects = Array.isArray(projectsRes.value) ? projectsRes.value : []
        for (const project of projects) {
          if (project.dueDate) {
            const d = new Date(project.dueDate)
            allEvents.push({
              id: `project-${project.id}`,
              title: t("cal.dueTitle", { title: project.title }),
              date: formatDateKey(d),
              time: "23:59",
              type: "PROJECT",
              description: project.description || undefined,
            })
          }
          if (project.startDate) {
            const d = new Date(project.startDate)
            allEvents.push({
              id: `project-start-${project.id}`,
              title: t("cal.startTitle", { title: project.title }),
              date: formatDateKey(d),
              time: "09:00",
              type: "PROJECT",
              description: project.description || undefined,
            })
          }
        }
      }

      if (researchRes.status === "fulfilled") {
        const research = Array.isArray(researchRes.value) ? researchRes.value : []
        for (const res of research) {
          if (res.dueDate) {
            const d = new Date(res.dueDate)
            allEvents.push({
              id: `research-${res.id}`,
              title: res.title,
              date: formatDateKey(d),
              time: "14:00",
              type: "RESEARCH",
              description: res.researchQuestion || res.description || undefined,
            })
          }
          if (res.startDate) {
            const d = new Date(res.startDate)
            allEvents.push({
              id: `research-start-${res.id}`,
              title: t("cal.kickoffTitle", { title: res.title }),
              date: formatDateKey(d),
              time: "10:00",
              type: "RESEARCH",
              description: res.researchQuestion || undefined,
            })
          }
        }
      }

      setEvents(allEvents)
    } catch {
      setError(t("cal.loadError"))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (!user) return
    loadEvents()
  }, [user, loadEvents])

  const firstName = user?.firstName || user?.name?.split(" ")[0] || t("cal.learnerFallback")

  const today = new Date()
  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()
  const daysInMonth = getDaysInMonth(year, month)
  const firstDay = getFirstDayOfMonth(year, month)

  const monthName = currentDate.toLocaleString("en-US", { month: "long", year: "numeric" })

  const filteredEvents = events.filter((e) => {
    if (filterType !== "ALL" && e.type !== filterType) return false
    return true
  })

  function getEventsForDate(dateKey: string): CalendarEvent[] {
    return filteredEvents.filter((e) => e.date === dateKey)
  }

  function navigateMonth(direction: number) {
    const next = new Date(currentDate)
    next.setMonth(next.getMonth() + direction)
    setCurrentDate(next)
  }

  function goToToday() {
    setCurrentDate(new Date())
    setSelectedDate(new Date())
  }

  function selectDate(date: Date) {
    setSelectedDate(date)
  }

  const calendarDays: { date: Date; dayNum: number; isCurrentMonth: boolean }[] = []
  const prevMonth = new Date(year, month, 0)
  const prevMonthDays = prevMonth.getDate()

  for (let i = 0; i < firstDay; i++) {
    const d = new Date(year, month - 1, prevMonthDays - firstDay + i + 1)
    calendarDays.push({ date: d, dayNum: d.getDate(), isCurrentMonth: false })
  }

  for (let i = 1; i <= daysInMonth; i++) {
    const d = new Date(year, month, i)
    calendarDays.push({ date: d, dayNum: i, isCurrentMonth: true })
  }

  const remainingCells = 42 - calendarDays.length
  for (let i = 1; i <= remainingCells; i++) {
    const d = new Date(year, month + 1, i)
    calendarDays.push({ date: d, dayNum: i, isCurrentMonth: false })
  }

  const upcomingEvents = filteredEvents
    .filter((e) => {
      const eventDate = new Date(e.date + (e.time ? `T${e.time}` : ""))
      const now = new Date()
      return eventDate >= now
    })
    .sort((a, b) => {
      const da = new Date(a.date + (a.time ? `T${a.time}` : ""))
      const db = new Date(b.date + (b.time ? `T${b.time}` : ""))
      return da.getTime() - db.getTime()
    })
    .slice(0, 7)

  const selectedDateEvents = selectedDate ? getEventsForDate(formatDateKey(selectedDate)) : []

  const weekStart = new Date(currentDate)
  const dayOfWeek = weekStart.getDay()
  weekStart.setDate(weekStart.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1))

  const weekDays: Date[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(weekStart)
    d.setDate(weekStart.getDate() + i)
    weekDays.push(d)
  }

  const timeSlots: string[] = []
  for (let h = 8; h <= 20; h++) {
    timeSlots.push(`${h.toString().padStart(2, "0")}:00`)
  }

  if (authLoading) return <LoadingState />

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("cal.subtitle")} />

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
            <button onClick={() => { setError(null); loadEvents() }} className="ml-auto text-xs underline">
              {tc("retry")}
            </button>
          </div>
        </div>
      )}

      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:gap-6">
        <div className="flex-1 space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-2">
                <div className="flex gap-1 rounded-xl border border-border bg-background p-1">
                  <button
                    onClick={() => setViewMode("month")}
                    className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                      viewMode === "month" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"
                    }`}
                  >
                    <Grid3X3 className="size-3.5" />
                    {t("cal.viewMonth")}
                  </button>
                  <button
                    onClick={() => setViewMode("week")}
                    className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                      viewMode === "week" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"
                    }`}
                  >
                    <List className="size-3.5" />
                    {t("cal.viewWeek")}
                  </button>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => navigateMonth(-1)}
                  className="rounded-lg border border-border p-1.5 text-muted-foreground transition hover:bg-muted"
                >
                  <ChevronLeft className="size-4" />
                </button>
                <h2 className="min-w-[160px] text-center text-sm font-semibold text-foreground">{monthName}</h2>
                <button
                  onClick={() => navigateMonth(1)}
                  className="rounded-lg border border-border p-1.5 text-muted-foreground transition hover:bg-muted"
                >
                  <ChevronRight className="size-4" />
                </button>
                <button
                  onClick={goToToday}
                  className="ml-1 flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition hover:bg-muted"
                >
                  <CalendarDays className="size-3.5" />
                  {t("cal.todayBtn")}
                </button>
              </div>
            </div>

            <div className="mt-3 flex flex-wrap gap-1.5">
              {FILTER_OPTIONS.map((opt) => (
                <button
                  key={opt.key}
                  onClick={() => setFilterType(opt.key)}
                  className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                    filterType === opt.key
                      ? "bg-primary text-primary-foreground"
                      : "border border-border bg-background text-muted-foreground hover:bg-muted"
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {loading ? (
            <LoadingState />
          ) : viewMode === "month" ? (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="grid grid-cols-7 gap-px">
                {WEEKDAYS.map((day) => (
                  <div key={day} className="py-2 text-center text-xs font-semibold text-muted-foreground">
                    {day}
                  </div>
                ))}
              </div>

              <div className="grid grid-cols-7 gap-px">
                {calendarDays.map((cell, idx) => {
                  const dateKey = formatDateKey(cell.date)
                  const dayEvents = getEventsForDate(dateKey)
                  const isToday = isSameDay(cell.date, today)
                  const isSelected = selectedDate && isSameDay(cell.date, selectedDate)
                  const hasEvents = dayEvents.length > 0

                  return (
                    <button
                      key={idx}
                      onClick={() => selectDate(cell.date)}
                      className={`relative flex min-h-[80px] flex-col rounded-lg p-1.5 text-left transition hover:bg-muted/50 ${
                        !cell.isCurrentMonth ? "opacity-30" : ""
                      } ${isToday ? "ring-2 ring-primary" : ""} ${isSelected && !isToday ? "ring-1 ring-border" : ""} ${
                        hasEvents && cell.isCurrentMonth ? "bg-primary/5" : ""
                      }`}
                    >
                      <span
                        className={`text-xs font-medium ${
                          is{t("cal.todayBtn")}
                            ? "flex size-6 items-center justify-center rounded-full bg-primary text-primary-foreground"
                            : "text-foreground"
                        }`}
                      >
                        {cell.dayNum}
                      </span>
                      <div className="mt-1 flex flex-1 flex-col gap-0.5 overflow-hidden">
                        {dayEvents.slice(0, 3).map((evt) => {
                          const colors = EVENT_COLORS[evt.type] || { dot: "bg-muted-foreground" }
                          return (
                            <div key={evt.id} className="flex items-center gap-1">
                              <span className={`size-1.5 shrink-0 rounded-full ${colors.dot}`} />
                              <span className="truncate text-[10px] text-foreground">{evt.title}</span>
                            </div>
                          )
                        })}
                        {dayEvents.length > 3 && (
                          <span className="text-[10px] font-medium text-muted-foreground">{t("cal.moreCount", { count: dayEvents.length - 3 })}</span>
                        )}
                      </div>
                    </button>
                  )
                })}
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="grid grid-cols-8 gap-px">
                <div />
                {weekDays.map((d, i) => {
                  const isToday = isSameDay(d, today)
                  return (
                    <div key={i} className="py-2 text-center">
                      <span className="text-xs text-muted-foreground">
                        {d.toLocaleDateString("en-US", { weekday: "short" })}
                      </span>
                      <div
                        className={`mt-1 mx-auto flex size-7 items-center justify-center rounded-full text-xs font-semibold ${
                          isToday ? "bg-primary text-primary-foreground" : "text-foreground"
                        }`}
                      >
                        {d.getDate()}
                      </div>
                    </div>
                  )
                })}
              </div>

              <div className="grid grid-cols-8 gap-px border-t border-border">
                {timeSlots.map((slot) => (
                  <div key={slot} className="contents">
                    <div className="flex items-start justify-end pr-2 pt-1">
                      <span className="text-[10px] text-muted-foreground">{slot}</span>
                    </div>
                    {weekDays.map((d, di) => {
                      const dateKey = formatDateKey(d)
                      const hour = parseInt(slot.split(":")[0])
                      const dayEvents = getEventsForDate(dateKey).filter((e) => {
                        if (!e.time) return hour === 9
                        const eHour = parseInt(e.time.split(":")[0])
                        return eHour === hour
                      })

                      return (
                        <div key={di} className="min-h-[48px] border-l border-border p-0.5">
                          {dayEvents.map((evt) => {
                            const colors = EVENT_COLORS[evt.type] || { bg: "bg-muted", text: "text-muted-foreground" }
                            return (
                              <div
                                key={evt.id}
                                onClick={() => selectDate(d)}
                                className={`mb-0.5 cursor-pointer rounded px-1 py-0.5 text-[10px] font-medium ${colors.bg} ${colors.text} transition hover:opacity-80`}
                                title={evt.title}
                              >
                                {evt.title.length > 12 ? evt.title.slice(0, 12) + "…" : evt.title}
                              </div>
                            )
                          })}
                        </div>
                      )
                    })}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="w-full space-y-4 lg:w-80">
          {selectedDate && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-semibold text-foreground">
                    {selectedDate.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric", year: "numeric" })}
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    {t("cal.eventsCount", { count: selectedDateEvents.length })}
                  </p>
                </div>
                <button
                  onClick={() => setSelectedDate(null)}
                  className="rounded-lg p-1 text-muted-foreground transition hover:bg-muted"
                >
                  <span className="sr-only">{t("cal.closeLabel")}</span>
                  <AlertCircle className="size-4" />
                </button>
              </div>

              <div className="mt-4 space-y-3">
                {selectedDateEvents.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-border py-6 text-center">
                    <Calendar className="mx-auto size-6 text-muted-foreground" />
                    <p className="mt-2 text-xs text-muted-foreground">{t("cal.emptyDay")}</p>
                  </div>
                ) : (
                  selectedDateEvents.map((evt) => (
                    <div key={evt.id} className="rounded-xl border border-border p-3 transition hover:bg-muted/50">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-foreground">{evt.title}</p>
                          <div className="mt-1 flex flex-wrap items-center gap-2">
                            {evt.time && (
                              <span className="flex items-center gap-1 text-xs text-muted-foreground">
                                <Clock className="size-3" />
                                {evt.time}
                              </span>
                            )}
                            {evt.duration && (
                              <span className="text-xs text-muted-foreground">{t("cal.minsShort", { count: evt.duration })}</span>
                            )}
                          </div>
                        </div>
                        <EventTypeBadge type={evt.type} />
                      </div>
                      {evt.location && (
                        <p className="mt-1.5 text-xs text-muted-foreground">{evt.location}</p>
                      )}
                      {evt.link && (
                        <a
                          href={evt.link}
                          className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                        >
                          {t("cal.viewDetails")}
                        </a>
                      )}
                    </div>
                  ))
                )}
              </div>

              <button className="mt-4 flex w-full items-center justify-center gap-1.5 rounded-lg border border-border bg-background px-3 py-2 text-xs font-medium text-foreground transition hover:bg-muted">
                <Plus className="size-3.5" />
                {t("cal.addEvent")}
              </button>
            </div>
          )}

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-2">
              <Filter className="size-4 text-muted-foreground" />
              <h3 className="text-sm font-semibold text-foreground">{t("cal.legend")}
            </div>
            <div className="mt-3 space-y-2">
              {Object.entries(EVENT_COLORS).map(([type, colors]) => (
                <div key={type} className="flex items-center gap-2">
                  <span className={`size-2.5 rounded-full ${colors.dot}`} />
                  <span className="text-xs text-foreground">{labels[type] || type}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h3 className="text-sm font-semibold text-foreground">{t("cal.upcoming")}
            <p className="text-xs text-muted-foreground">{t("cal.next7")}
            <div className="mt-3 space-y-2">
              {upcomingEvents.length === 0 ? (
                <p className="py-4 text-center text-xs text-muted-foreground">{t("cal.emptyUpcoming")}</p>
              ) : (
                upcomingEvents.map((evt) => {
                  const colors = EVENT_COLORS[evt.type] || { bg: "bg-muted", text: "text-muted-foreground", dot: "bg-muted-foreground" }
                  const eventDate = new Date(evt.date)
                  const dayLabel = isSameDay(eventDate, today)
                    ? t("cal.todayLabel")
                    : isSameDay(eventDate, new Date(today.getTime() + 86400000))
                      ? t("cal.tomorrowLabel")
                      : eventDate.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })

                  return (
                    <div key={evt.id} className="rounded-xl border border-border p-3 transition hover:bg-muted/50">
                      <div className="flex items-start gap-2">
                        <span className={`mt-0.5 size-2 shrink-0 rounded-full ${colors.dot}`} />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-xs font-medium text-foreground">{evt.title}</p>
                          <div className="mt-0.5 flex items-center gap-1.5 text-[10px] text-muted-foreground">
                            <span>{dayLabel}</span>
                            {evt.time && (
                              <>
                                <span>·</span>
                                <span>{evt.time}</span>
                              </>
                            )}
                          </div>
                        </div>
                        <EventTypeBadge type={evt.type} />
                      </div>
                      {evt.link && (
                        <a
                          href={evt.link}
                          className="mt-1.5 inline-flex items-center gap-1 text-[10px] font-medium text-primary hover:underline"
                        >
                          {t("cal.joinView")}
                        </a>
                      )}
                    </div>
                  )
                })
              )}
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h3 className="text-sm font-semibold text-foreground">{t("cal.stats")}
            <div className="mt-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">{t("cal.totalEvents")}</span>
                <span className="text-xs font-semibold text-foreground">{events.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">{t("cal.liveSessions")}</span>
                <span className="text-xs font-semibold text-red-600">
                  {events.filter((e) => e.type === "LIVE_SESSION").length}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">{t("cal.deadlines")}
                <span className="text-xs font-semibold text-amber-600">
                  {events.filter((e) => e.type === "DEADLINE").length}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">{t("cal.projectsLabel")}</span>
                <span className="text-xs font-semibold text-cyan-600">
                  {events.filter((e) => e.type === "PROJECT").length}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">{t("cal.researchLabel")}</span>
                <span className="text-xs font-semibold text-purple-600">
                  {events.filter((e) => e.type === "RESEARCH").length}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
