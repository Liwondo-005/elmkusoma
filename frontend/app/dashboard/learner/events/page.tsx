"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, isAlmostFull, type EventItem } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { useLowBandwidth } from "@/components/primary/low-bandwidth-provider"
import {
  CalendarDays,
  Clock,
  MapPin,
  Users,
  Search,
  Loader2,
  CalendarOff,
  Play,
  Eye,
  AlertCircle,
  Video,
  RefreshCw,
} from "lucide-react"

type Tab = "all" | "related" | "live" | "upcoming" | "registered" | "past" | "replays"

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
    year: "numeric",
  })
}

function formatTime(d: string) {
  return new Date(d).toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
  })
}

function getTypeColor(type: string) {
  const m: Record<string, string> = {
    SEMINAR: "bg-blue-500/10 text-blue-600",
    WORKSHOP: "bg-green-500/10 text-green-600",
    WEBINAR: "bg-purple-500/10 text-purple-600",
    TRAINING: "bg-orange-500/10 text-orange-600",
    CONFERENCE: "bg-red-500/10 text-red-600",
    LECTURE: "bg-teal-500/10 text-teal-600",
  }
  return m[type] || "bg-muted text-muted-foreground"
}

function getCountdown(startsAt: string): string {
  const now = new Date()
  const start = new Date(startsAt)
  const diff = start.getTime() - now.getTime()
  if (diff <= 0) return ""
  const days = Math.floor(diff / 86400000)
  const hours = Math.floor((diff % 86400000) / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  if (days > 0) return `${days}d ${hours}h`
  if (hours > 0) return `${hours}h ${minutes}m`
  return `${minutes}m`
}

function getEventStatus(event: EventItem): "live" | "upcoming" | "past" {
  const backend = (event.eventStatus || event.status || "").toUpperCase()
  if (backend === "LIVE" || backend === "STARTING" || backend === "ENDING") return "live"
  if (
    backend === "ENDED" ||
    backend === "RECORDING" ||
    backend === "PROCESSING" ||
    backend === "REPLAY_AVAILABLE" ||
    backend === "CANCELLED" ||
    backend === "FAILED" ||
    backend === "COMPLETED"
  ) {
    return "past"
  }
  if (backend === "PUBLISHED" || backend === "REGISTRATION_OPEN" || backend === "REGISTRATION_CLOSED" || backend === "PREPARING" || backend === "FULL") {
    return "upcoming"
  }
  const now = new Date()
  const start = new Date(event.startsAt)
  const end = event.endsAt
    ? new Date(event.endsAt)
    : new Date(start.getTime() + (event.durationMinutes || 60) * 60000)
  if (now >= start && now <= end) return "live"
  if (now > end) return "past"
  return "upcoming"
}

export default function EventsPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const { reducedAnimations } = useLowBandwidth()

  const [activeTab, setActiveTab] = useState<Tab>("upcoming")
  const [events, setEvents] = useState<EventItem[]>([])
  const [registeredEvents, setRegisteredEvents] = useState<EventItem[]>([])
  const [enrolledCourseIds, setEnrolledCourseIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [retryKey, setRetryKey] = useState(0)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [allEvents, registered, enrollments] = await Promise.all([
        learnerApi.getEvents().catch(() => []),
        learnerApi.getRegisteredEvents().catch(() => []),
        learnerApi.getEnrollments().catch(() => []),
      ])
      setEvents(allEvents)
      setRegisteredEvents(registered)
      setEnrolledCourseIds(new Set(enrollments.map((e) => e.courseId)))
    } catch {
      setError(t("error.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadData()
  }, [loadData, retryKey])

  const liveEvents = events.filter((e) => getEventStatus(e) === "live")
  const upcomingEvents = events.filter((e) => getEventStatus(e) === "upcoming")
  const pastEvents = events.filter((e) => getEventStatus(e) === "past")
  const replayEvents = events.filter((e) => e.hasRecording || e.eventStatus === "REPLAY_AVAILABLE")
  const relatedEvents = useMemo(
    () =>
      events.filter(
        (e) =>
          (e.relatedCourseId && enrolledCourseIds.has(e.relatedCourseId)) ||
          (e.relatedCourseTitle && enrolledCourseIds.size > 0),
      ),
    [events, enrolledCourseIds],
  )
  const allEvents = events

  const filteredByTab = (() => {
    const list =
      activeTab === "registered"
        ? registeredEvents
        : activeTab === "live"
          ? liveEvents
          : activeTab === "upcoming"
            ? upcomingEvents
            : activeTab === "past"
              ? pastEvents
              : activeTab === "all"
                ? allEvents
                : activeTab === "related"
                  ? relatedEvents
                  : replayEvents
    if (!search) return list
    const q = search.toLowerCase()
    return list.filter(
      (e) =>
        e.title.toLowerCase().includes(q) ||
        (e.description?.toLowerCase().includes(q) ?? false) ||
        e.eventType.toLowerCase().includes(q)
    )
  })()

  const tabCounts: Record<Tab, number> = {
    all: allEvents.length,
    related: relatedEvents.length,
    live: liveEvents.length,
    upcoming: upcomingEvents.length,
    registered: registeredEvents.length,
    past: pastEvents.length,
    replays: replayEvents.length,
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: "all", label: t("tabs.allEvents") },
    { key: "related", label: t("tabs.related") },
    { key: "live", label: t("tabs.liveNow") },
    { key: "upcoming", label: t("tabs.upcoming") },
    { key: "registered", label: t("tabs.registered") },
    { key: "past", label: t("tabs.past") },
    { key: "replays", label: t("tabs.replays") },
  ]

  if (loading) {
    return (
      <div role="main" aria-label={t("title")}>
        <LoadingState />
      </div>
    )
  }

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6" aria-label={t("title")}>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
        </div>
        <Link
          href="/dashboard/learner/events/registered"
          aria-label={t("myRegistrations")}
          className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
        >
          {t("myRegistrations")}
        </Link>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            <span>{error}</span>
            <button
              onClick={() => setRetryKey((k) => k + 1)}
              aria-label={tc("retry")}
              className="ml-auto inline-flex items-center gap-1 text-xs font-medium underline hover:no-underline"
            >
              <RefreshCw className="size-3" />
              {tc("retry")}
            </button>
          </div>
        </div>
      )}

      <div className="flex items-center gap-2 overflow-x-auto pb-1" role="tablist" aria-label={t("title")}>
        {tabs.map((tab) => (
          <button
            key={tab.key}
            role="tab"
            aria-selected={activeTab === tab.key}
            aria-label={`${tab.label} (${tabCounts[tab.key]})`}
            onClick={() => setActiveTab(tab.key)}
            className={`shrink-0 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === tab.key
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            }`}
          >
            {tab.label}
            {tabCounts[tab.key] > 0 && (
              <span className="ml-1.5 inline-flex size-5 items-center justify-center rounded-full bg-primary-foreground/20 text-[10px]">
                {tabCounts[tab.key]}
              </span>
            )}
          </button>
        ))}
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder={t("searchPlaceholder")}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label={t("searchPlaceholder")}
          className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
        />
      </div>

      {filteredByTab.length === 0 ? (
        <EmptyState
          icon={<CalendarOff className="size-8" />}
          title={
            activeTab === "live"
              ? t("empty.liveNow")
              : activeTab === "upcoming"
                ? t("empty.upcoming")
                : activeTab === "registered"
                  ? t("empty.registered")
                  : activeTab === "past"
                    ? t("empty.past")
                    : activeTab === "all"
                      ? t("empty.noEvents")
                      : activeTab === "related"
                        ? t("empty.related")
                        : t("empty.replays")
          }
          description={
            activeTab === "live"
              ? t("empty.liveNowDesc")
              : activeTab === "upcoming"
                ? t("empty.upcomingDesc")
                : activeTab === "registered"
                  ? t("empty.registeredDesc")
                  : activeTab === "past"
                    ? t("empty.pastDesc")
                    : activeTab === "all"
                      ? t("empty.noEventsDesc")
                      : activeTab === "related"
                        ? t("empty.relatedDesc")
                        : t("empty.replaysDesc")
          }
          action={
            activeTab !== "registered" ? (
              <Link
                href="/dashboard/learner/events"
                aria-label={t("empty.browseEvents")}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                {t("empty.browseEvents")}
              </Link>
            ) : undefined
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredByTab.map((event) => {
            const status = getEventStatus(event)
            const isLive = status === "live"
            const isPast = status === "past"
            const countdown = !isPast && !isLive ? getCountdown(event.startsAt) : ""
            const cancelReason = event.eventStatus === "CANCELLED" ? event.cancellationReason : null

            return (
              <Link
                key={event.id}
                href={`/dashboard/learner/events/${event.id}`}
                aria-label={`${event.title} - ${tabs.find((tb) => tb.key === activeTab)?.label || event.title}`}
                className={`group rounded-2xl border bg-card p-5 shadow-xs transition-all hover:shadow-md ${
                  isLive
                    ? "border-green-500/30"
                    : cancelReason
                      ? "border-destructive/30"
                      : "border-border hover:border-primary/30"
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-semibold ${getTypeColor(event.eventType)}`}
                  >
                    {event.eventType}
                  </span>
                  {isLive && (
                    <span className={`inline-flex items-center gap-1 rounded-full bg-green-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-green-600${reducedAnimations ? "" : " "}`}>
                      {!reducedAnimations && <span className="size-1.5 rounded-full bg-green-500 animate-pulse" aria-hidden="true" />}
                      {t("live.badge")}
                      <span className="sr-only">{t("live.badge")}</span>
                    </span>
                  )}
                  {isPast && (
                    <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {t("past.badge")}
                    </span>
                  )}
                  {event.eventStatus === "CANCELLED" && (
                    <span className="inline-flex items-center rounded-full bg-destructive/10 px-2.5 py-0.5 text-[10px] font-semibold text-destructive">
                      {t("status.cancelled")}
                    </span>
                  )}
                  {isAlmostFull(event) && !isPast && !isLive && event.eventStatus !== "CANCELLED" && (
                    <span
                      className="inline-flex items-center rounded-full bg-amber-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-amber-600"
                      role="status"
                      title={t("status.almostFull")}
                    >
                      {t("status.almostFull")}
                    </span>
                  )}
                  {event.isRegistered && !isLive && !isPast && event.eventStatus !== "CANCELLED" && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-green-600">
                      {t("registered.badge")}
                    </span>
                  )}
                </div>
                <span className="sr-only" role="status">
                  {event.eventStatus || event.status}
                </span>

                <h3 className="mt-3 text-sm font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2">
                  {event.title}
                </h3>

                {(event.presenterName || event.organizerName) && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("card.presenter", { name: event.presenterName || event.organizerName || "" })}
                  </p>
                )}

                {cancelReason && (
                  <p className="mt-1 text-xs text-destructive">
                    {t("status.cancelled")}{cancelReason ? `: ${cancelReason}` : ""}
                  </p>
                )}

                <div className="mt-3 space-y-1.5 text-xs text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <CalendarDays className="size-3 shrink-0" />
                    <span>{formatDate(event.startsAt)}</span>
                    <span className="text-muted-foreground/60">at {formatTime(event.startsAt)}</span>
                    {event.timezone && <span className="text-[10px]">{event.timezone}</span>}
                  </div>
                  <div className="flex items-center gap-2">
                    <Clock className="size-3 shrink-0" />
                    <span>{t("card.minutes", { count: event.durationMinutes || 60 })}</span>
                  </div>
                  {event.maxParticipants && (
                    <div className="flex items-center gap-2">
                      <Users className="size-3 shrink-0" />
                      <span>{t("card.capacity", { registered: event.registeredCount, max: event.maxParticipants })}</span>
                    </div>
                  )}
                </div>

                <div className="mt-4 flex items-center gap-2">
                  {(isLive || (event.isRegistered && event.eventStatus === "LIVE")) && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white">
                      <Video className="size-3" />
                      {t("live.joinNow")}
                    </span>
                  )}
                  {!isLive && !isPast && !event.isRegistered && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground group-hover:bg-primary/90">
                      {t("upcoming.register")}
                    </span>
                  )}
                  {!isLive && !isPast && event.isRegistered && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-green-500/10 px-3 py-1.5 text-xs font-medium text-green-600">
                      {t("registered.viewEvent")}
                    </span>
                  )}
                  {isPast && event.hasRecording && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary">
                      <Play className="size-3" />
                      {t("past.watchReplay")}
                    </span>
                  )}
                  {activeTab === "replays" && event.hasRecording && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary">
                      <Eye className="size-3" />
                      {t("replays.watchNow")}
                    </span>
                  )}
                  {countdown && (
                    <span className="ml-auto text-[10px] font-medium text-muted-foreground">
                      {t("upcoming.countdown", { time: countdown })}
                    </span>
                  )}
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
