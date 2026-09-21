"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { learnerApi } from "@/lib/learner-api"
import type { EventItem } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Calendar, Clock, MapPin, Users, Filter, AlertCircle } from "lucide-react"

type ViewMode = "today" | "week" | "month"

function EventTypeBadge({ type }: { type: string }) {
  const colors: Record<string, string> = {
    WORKSHOP: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    SEMINAR: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    LECTURE: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
    CONFERENCE: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    EXAM: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    DEADLINE: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
    SOCIAL: "bg-pink-100 text-pink-700 dark:bg-pink-900/30 dark:text-pink-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[type] || "bg-muted text-muted-foreground"}`}>
      {type}
    </span>
  )
}

export default function CalendarPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [events, setEvents] = useState<EventItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>("today")
  const [filterType, setFilterType] = useState<string>("ALL")

  useEffect(() => {
    if (!user) return
    loadEvents()
  }, [user])

  async function loadEvents() {
    try {
      setLoading(true)
      const res = await learnerApi.getUpcomingEvents()
      setEvents(Array.isArray(res) ? res : [])
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  const now = new Date()
  const todayStr = now.toISOString().split("T")[0]
  const weekEnd = new Date(now)
  weekEnd.setDate(weekEnd.getDate() + 7)
  const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0)

  const filtered = events.filter((e) => {
    if (filterType !== "ALL" && e.eventType !== filterType) return false
    return true
  })

  const todayEvents = filtered.filter((e) => {
    const eventDate = new Date(e.startsAt)
    return eventDate.toISOString().split("T")[0] === todayStr
  })

  const weekEvents = filtered.filter((e) => {
    const eventDate = new Date(e.startsAt)
    return eventDate >= now && eventDate <= weekEnd
  })

  const monthEvents = filtered.filter((e) => {
    const eventDate = new Date(e.startsAt)
    return eventDate >= now && eventDate <= monthEnd
  })

  const displayed = viewMode === "today" ? todayEvents : viewMode === "week" ? weekEvents : monthEvents

  const eventTypes = [...new Set(events.map(e => e.eventType).filter(Boolean))]

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadEvents() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      )}

      <LearnerHeader firstName={firstName} subtitle={t("calendar.subtitle")} />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-1 rounded-xl border border-border bg-card p-1 shadow-xs">
          {(["today", "week", "month"] as ViewMode[]).map((mode) => (
            <button
              key={mode}
              onClick={() => setViewMode(mode)}
              aria-label={mode === "today" ? t("calendar.today") : mode === "week" ? t("calendar.thisWeek") : t("calendar.thisMonth")}
              className={`rounded-lg px-4 py-2 text-xs font-medium transition ${
                viewMode === mode
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-muted"
              }`}
            >
              {mode === "today" ? t("calendar.today") : mode === "week" ? t("calendar.thisWeek") : t("calendar.thisMonth")}
            </button>
          ))}
        </div>

        <div className="relative">
          <Filter className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            aria-label={t("calendar.filterEvents")}
            className="appearance-none rounded-xl border border-border bg-card py-2.5 pl-10 pr-8 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option value="ALL">{t("calendar.allEvents")}</option>
            {eventTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </div>
      </div>

      {displayed.length === 0 ? (
        <EmptyState
          icon={<Calendar className="size-8" />}
          title={t("calendar.noEvents")}
          description={
            viewMode === "today"
              ? t("calendar.noEventsToday")
              : viewMode === "week"
                ? t("calendar.noEventsWeek")
                : t("calendar.noEventsMonth")
          }
        />
      ) : (
        <div className="space-y-3">
          {displayed.map((event) => (
            <div key={event.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold text-foreground">{event.title}</h3>
                    {event.eventType && <EventTypeBadge type={event.eventType} />}
                  </div>
                  {event.description && (
                    <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{event.description}</p>
                  )}
                  <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                    {event.startsAt && (
                      <span className="flex items-center gap-1.5">
                        <Clock className="size-3.5" />
                        {new Date(event.startsAt).toLocaleString("en-US", {
                          weekday: "short",
                          month: "short",
                          day: "numeric",
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </span>
                    )}
                    {event.location && (
                      <span className="flex items-center gap-1.5">
                        <MapPin className="size-3.5" />
                        {event.location}
                      </span>
                    )}
                    {event.maxParticipants && (
                      <span className="flex items-center gap-1.5">
                        <Users className="size-3.5" />
                        {event.maxParticipants} {tc("seats")}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
