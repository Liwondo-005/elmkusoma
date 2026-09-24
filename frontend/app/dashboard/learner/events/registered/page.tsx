"use client"

import { useEffect, useState, useCallback } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, type EventItem } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import {
  CalendarDays,
  Clock,
  MapPin,
  ArrowLeft,
  CheckCircle,
  CalendarOff,
  ExternalLink,
  AlertCircle,
  RefreshCw,
  Play,
  Video,
  CalendarPlus,
} from "lucide-react"

type Tab = "upcoming" | "past" | "replays"

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

function getEventStatus(event: EventItem): "upcoming" | "past" {
  const backend = (event.eventStatus || event.status || "").toUpperCase()
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
  if (backend === "LIVE" || backend === "STARTING") return "upcoming"
  const now = new Date()
  const end = event.endsAt
    ? new Date(event.endsAt)
    : new Date(new Date(event.startsAt).getTime() + (event.durationMinutes || 60) * 60000)
  if (now > end) return "past"
  return "upcoming"
}

export default function RegisteredEventsPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")

  const [activeTab, setActiveTab] = useState<Tab>("upcoming")
  const [upcoming, setUpcoming] = useState<EventItem[]>([])
  const [past, setPast] = useState<EventItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [up, pa] = await Promise.all([
        learnerApi.getRegisteredEvents().catch(() => []),
        learnerApi.getRegisteredPastEvents().catch(() => []),
      ])
      setUpcoming(up)
      setPast(pa)
    } catch {
      setError(t("error.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadData()
  }, [loadData, retryKey])

  const replayEvents = past.filter((e) => e.hasRecording || e.eventStatus === "REPLAY_AVAILABLE")

  function addToCalendar(event: EventItem, e: React.MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    const start = new Date(event.startsAt)
    const end = event.endsAt
      ? new Date(event.endsAt)
      : new Date(start.getTime() + (event.durationMinutes || 60) * 60000)
    const tz = event.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone
    const pad = (n: number) => String(n).padStart(2, "0")
    const local = (d: Date) =>
      `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
    const ics = [
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "PRODID:-//ELMKUSOMA//Events//EN",
      "BEGIN:VTIMEZONE",
      `TZID:${tz}`,
      "BEGIN:STANDARD",
      "DTSTART:19700101T000000",
      "TZOFFSETFROM:+0000",
      "TZOFFSETTO:+0000",
      "TZNAME:UTC",
      "END:STANDARD",
      "END:VTIMEZONE",
      "BEGIN:VEVENT",
      `UID:${event.id}@elmkusoma`,
      `DTSTART;TZID=${tz}:${local(start)}`,
      `DTEND;TZID=${tz}:${local(end)}`,
      `SUMMARY:${event.title}`,
      event.location ? `LOCATION:${event.location}` : "",
      "END:VEVENT",
      "END:VCALENDAR",
    ].join("\r\n")
    const blob = new Blob([ics], { type: "text/calendar;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `${event.title.replace(/\s+/g, "_")}.ics`
    a.click()
    URL.revokeObjectURL(url)
  }

  const tabCounts: Record<Tab, number> = {
    upcoming: upcoming.length,
    past: past.length,
    replays: replayEvents.length,
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: "upcoming", label: t("tabs.upcoming") },
    { key: "past", label: t("tabs.past") },
    { key: "replays", label: t("tabs.replays") },
  ]

  const filteredByTab =
    activeTab === "upcoming"
      ? upcoming
      : activeTab === "past"
        ? past
        : replayEvents

  if (loading) {
    return (
      <div role="main" aria-label={t("myRegistrations")}>
        <LoadingState />
      </div>
    )
  }

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6" aria-label={t("myRegistrations")}>
      <div>
        <Link
          href="/dashboard/learner/events"
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-2"
          aria-label={t("detail.backToEvents")}
        >
          <ArrowLeft className="size-4" /> {t("detail.backToEvents")}
        </Link>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("myRegistrations")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
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

      <div className="flex items-center gap-2 overflow-x-auto pb-1" role="tablist" aria-label={t("myRegistrations")}>
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

      {filteredByTab.length === 0 ? (
        <EmptyState
          icon={<CalendarOff className="size-8" />}
          title={
            activeTab === "upcoming"
              ? t("empty.registered")
              : activeTab === "past"
                ? t("empty.past")
                : t("empty.replays")
          }
          description={
            activeTab === "upcoming"
              ? t("empty.registeredDesc")
              : activeTab === "past"
                ? t("empty.pastDesc")
                : t("empty.replaysDesc")
          }
          action={
            activeTab === "upcoming" ? (
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
        <div className="space-y-3">
          {filteredByTab.map((event) => {
            const status = getEventStatus(event)
            const isPast = status === "past"
            const backend = (event.eventStatus || event.status || "").toUpperCase()
            const isLive = backend === "LIVE" || backend === "STARTING"
            const attended = Boolean((event as any).attended)

            return (
              <Link
                key={event.id}
                href={`/dashboard/learner/events/${event.id}`}
                aria-label={`${event.title} - ${tabs.find((tb) => tb.key === activeTab)?.label || event.title}`}
                className={`flex items-center justify-between rounded-2xl border bg-card p-4 shadow-xs transition-all hover:shadow-md ${
                  isLive ? "border-green-500/30" : "border-border hover:border-primary/30"
                } ${isPast && activeTab !== "replays" ? "opacity-75 hover:opacity-100" : ""}`}
              >
                <div className="flex items-center gap-4">
                  <div
                    className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${
                      isLive ? "bg-green-500/10 text-green-600" : isPast ? "bg-muted text-muted-foreground" : "bg-primary/10 text-primary"
                    }`}
                  >
                    <CalendarDays className="size-5" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="font-semibold text-foreground truncate">{event.title}</h3>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${getTypeColor(event.eventType)}`}>
                        {event.eventType}
                      </span>
                      {isLive && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                          <span className="size-1.5 rounded-full bg-green-500 animate-pulse" />
                          {t("live.badge")}
                        </span>
                      )}
                    </div>
                    <div className="mt-1 flex items-center gap-4 text-xs text-muted-foreground flex-wrap">
                      <span className="flex items-center gap-1">
                        <CalendarDays className="size-3" /> {formatDate(event.startsAt)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock className="size-3" /> {formatTime(event.startsAt)}
                      </span>
                      {event.location && (
                        <span className="flex items-center gap-1">
                          <MapPin className="size-3" /> {event.location}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <button
                    type="button"
                    onClick={(e) => addToCalendar(event, e)}
                    className="flex size-8 items-center justify-center rounded-lg border border-border text-muted-foreground hover:bg-muted"
                    aria-label={`${t("registered.addToCalendar")} — ${event.title}`}
                    title={t("registered.addToCalendar")}
                  >
                    <CalendarPlus className="size-4" />
                  </button>
                  {isLive && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white">
                      <Video className="size-3" />
                      {t("live.joinNow")}
                    </span>
                  )}
                  {!isLive && !isPast && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-green-500/10 px-3 py-1.5 text-xs font-medium text-green-600">
                      <CheckCircle className="size-3" />
                      {t("registered.viewEvent")}
                    </span>
                  )}
                  {isPast && activeTab === "replays" && event.hasRecording && (
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary">
                      <Play className="size-3" />
                      {t("replays.watchNow")}
                    </span>
                  )}
                      {isPast && activeTab === "past" && event.hasRecording && (
                        <span className="inline-flex items-center gap-1 text-xs text-green-600 font-medium">
                          {t("past.recordingAvailable")}
                        </span>
                      )}
                      {isPast && attended && (
                        <span className="inline-flex items-center gap-1 text-xs text-teal font-medium">
                          <CheckCircle className="size-3" aria-hidden="true" /> {t("attendanceRecorded")}
                        </span>
                      )}
                      {isPast && attended && activeTab === "past" && (
                        <span className="inline-flex items-center gap-1 rounded-lg border border-teal-500/30 bg-teal-500/10 px-2.5 py-1 text-xs font-semibold text-teal-700" role="status">
                          <CheckCircle className="size-3" aria-hidden="true" />
                          {t("past.viewAttendance")}
                        </span>
                      )}
                      {isPast && event.eventStatus === "PROCESSING" && (
                        <span className="inline-flex items-center gap-1 text-xs text-amber-600 font-medium" role="status">
                          {t("status.processing")}
                        </span>
                      )}
                      {(event.recordingStatus || "").toUpperCase() === "FAILED" && (
                        <span className="inline-flex items-center gap-1 text-xs text-destructive font-medium" role="alert">
                          {t("status.recordingFailed")}
                        </span>
                      )}
                      {isPast && !isLive && !event.hasRecording && event.eventStatus !== "PROCESSING" && (
                        <ExternalLink className="size-4 text-muted-foreground" />
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
