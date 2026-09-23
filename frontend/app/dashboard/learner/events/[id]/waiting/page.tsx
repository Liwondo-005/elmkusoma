"use client"

import { useEffect, useState, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, type EventItem } from "@/lib/learner-api"
import { ArrowLeft, Clock, CalendarDays, Users, RefreshCw, Loader2, Play } from "lucide-react"

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric", year: "numeric" })
}

function formatTime(d: string) {
  return new Date(d).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
}

function formatCountdown(diff: number) {
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)
  return { hours, minutes, seconds }
}

function getTypeColor(type: string) {
  const m: Record<string, string> = {
    SEMINAR: "bg-blue-100 text-blue-800", WORKSHOP: "bg-green-100 text-green-800",
    WEBINAR: "bg-purple-100 text-purple-800", TRAINING: "bg-orange-100 text-orange-800",
    CONFERENCE: "bg-red-100 text-red-800", LECTURE: "bg-teal-100 text-teal-800",
  }
  return m[type] || "bg-gray-100 text-gray-800"
}

export default function WaitingPage() {
  const params = useParams()
  const router = useRouter()
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const eventId = params.id as string

  const [event, setEvent] = useState<EventItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [refreshing, setRefreshing] = useState(false)
  const [countdown, setCountdown] = useState({ hours: 0, minutes: 0, seconds: 0 })
  const [isLive, setIsLive] = useState(false)
  const [now, setNow] = useState(Date.now())

  const loadEvent = useCallback(async () => {
    try {
      const ev = await learnerApi.getEvent(eventId)
      setEvent(ev)
      setError("")
    } catch (e: any) {
      setError(e.message || "Failed to load event")
    } finally {
      setLoading(false)
    }
  }, [eventId])

  useEffect(() => {
    loadEvent()
  }, [loadEvent])

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    if (!event) return
    const start = new Date(event.startsAt).getTime()
    const end = event.endsAt ? new Date(event.endsAt).getTime() : start + (event.durationMinutes || 60) * 60000
    const current = now

    if (current >= start && current <= end) {
      setIsLive(true)
      setCountdown({ hours: 0, minutes: 0, seconds: 0 })
    } else if (current < start) {
      setIsLive(false)
      setCountdown(formatCountdown(start - current))
    } else {
      setIsLive(false)
      setCountdown({ hours: 0, minutes: 0, seconds: 0 })
    }
  }, [event, now])

  useEffect(() => {
    if (isLive) return
    const interval = setInterval(async () => {
      try {
        const ev = await learnerApi.getEvent(eventId)
        setEvent(ev)
        const start = new Date(ev.startsAt).getTime()
        if (Date.now() >= start) setIsLive(true)
      } catch {}
    }, 30000)
    return () => clearInterval(interval)
  }, [eventId, isLive])

  const handleRefresh = async () => {
    setRefreshing(true)
    await loadEvent()
    setRefreshing(false)
  }

  if (loading) {
    return (
      <main role="main" aria-label={t("waiting.title")} className="flex items-center justify-center py-16">
        <Loader2 className="size-8 animate-spin text-primary" />
      </main>
    )
  }

  if (error || !event) {
    return (
      <main role="main" aria-label={t("waiting.title")} className="space-y-4">
        <Link href={`/dashboard/learner/events/${eventId}`} className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> {tc("back")}
        </Link>
        <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center">
          <p className="text-sm text-red-700">{error || tc("error.generic")}</p>
        </div>
      </main>
    )
  }

  return (
    <main role="main" aria-label={t("waiting.title")} className="space-y-6">
      <Link href={`/dashboard/learner/events/${eventId}`} className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> {t("waiting.returnToEvent")}
      </Link>

      <div className="rounded-xl border border-border bg-card p-6 text-center">
        <div className="mb-4 flex items-center justify-center gap-2">
          <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${getTypeColor(event.eventType)}`}>
            {event.eventType}
          </span>
          {event.category && (
            <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-xs font-medium">{event.category}</span>
          )}
        </div>

        <h1 className="text-2xl font-bold">{event.title}</h1>

        {!isLive ? (
          <>
            <p className="mt-3 text-muted-foreground">{t("waiting.sessionNotStarted")}</p>
            <div className="mt-4 inline-flex items-center gap-2 rounded-lg bg-muted/50 px-6 py-3">
              <Clock className="size-5 text-primary" />
              <span className="text-lg font-semibold tabular-nums">
                {t("waiting.startsIn")} {countdown.hours}{t("waiting.hours")} {countdown.minutes}{t("waiting.minutes")} {countdown.seconds}{t("waiting.seconds")}
              </span>
            </div>
          </>
        ) : (
          <div className="mt-4">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-red-100 px-4 py-2 text-sm font-medium text-red-700 animate-pulse">
              ● Live Now
            </span>
          </div>
        )}

        <div className="mt-6 grid gap-4 md:grid-cols-3 text-left">
          <div className="rounded-lg border border-border p-4">
            <p className="text-xs font-medium text-muted-foreground">{t("waiting.presenter")}</p>
            <p className="mt-1 text-sm font-medium">{event.organizerName || "—"}</p>
          </div>
          <div className="rounded-lg border border-border p-4">
            <p className="text-xs font-medium text-muted-foreground">{t("waiting.type")}</p>
            <p className="mt-1 text-sm font-medium">{event.eventType}</p>
          </div>
          <div className="rounded-lg border border-border p-4">
            <p className="text-xs font-medium text-muted-foreground">{t("waiting.duration")}</p>
            <p className="mt-1 text-sm font-medium">{event.durationMinutes || 60} {t("waiting.minutesUnit")}</p>
          </div>
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-2 text-left">
          <div className="flex items-start gap-3 rounded-lg border border-border p-4">
            <CalendarDays className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
            <div>
              <p className="text-xs font-medium text-muted-foreground">{t("waiting.scheduledStart")}</p>
              <p className="text-sm font-medium">{formatDate(event.startsAt)}</p>
              <p className="text-xs text-muted-foreground">{formatTime(event.startsAt)}</p>
            </div>
          </div>
          {event.maxParticipants && (
            <div className="flex items-start gap-3 rounded-lg border border-border p-4">
              <Users className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              <div>
                <p className="text-xs font-medium text-muted-foreground">Registered</p>
                <p className="text-sm font-medium">{event.registeredCount} / {event.maxParticipants}</p>
              </div>
            </div>
          )}
        </div>

        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex h-10 items-center justify-center gap-2 rounded-lg border border-border px-4 text-sm font-medium hover:bg-muted disabled:opacity-50"
            aria-label={t("waiting.refreshStatus")}
          >
            <RefreshCw className={`size-4 ${refreshing ? "animate-spin" : ""}`} />
            {t("waiting.refreshStatus")}
          </button>

          {isLive && event.meetingUrl && (
            <a
              href={event.meetingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              aria-label={t("waiting.joinNow")}
            >
              <Play className="size-4" />
              {t("waiting.joinNow")}
            </a>
          )}
        </div>
      </div>
    </main>
  )
}
