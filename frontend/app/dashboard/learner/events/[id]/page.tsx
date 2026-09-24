"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, isAlmostFull, type EventItem, type EventMaterial } from "@/lib/learner-api"
import { VideoPlayer } from "@/components/events/video-player"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { useLowBandwidth } from "@/components/primary/low-bandwidth-provider"
import {
  CalendarDays,
  Clock,
  MapPin,
  Users,
  ArrowLeft,
  CheckCircle,
  XCircle,
  Loader2,
  Download,
  Play,
  FileText,
  Presentation,
  File,
  AlertCircle,
  RefreshCw,
  Tag,
  BookOpen,
  Layers,
  FileText as LessonIcon,
  ExternalLink,
  Globe,
  WifiOff,
} from "lucide-react"

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", {
    weekday: "long",
    month: "long",
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

function getMaterialIcon(type: string) {
  switch (type) {
    case "RECORDING":
    case "VIDEO":
      return <Play className="size-4" />
    case "DOCUMENT":
    case "PDF":
      return <FileText className="size-4" />
    case "PRESENTATION":
      return <Presentation className="size-4" />
    default:
      return <File className="size-4" />
  }
}

function getLiveStatus(startsAt: string, endsAt: string | null, durationMinutes: number | null, eventStatus: string) {
  const backend = (eventStatus || "").toUpperCase()
  if (backend === "LIVE" || backend === "STARTING") return "live"
  if (
    backend === "ENDED" ||
    backend === "RECORDING" ||
    backend === "PROCESSING" ||
    backend === "REPLAY_AVAILABLE" ||
    backend === "CANCELLED" ||
    backend === "FAILED" ||
    backend === "COMPLETED"
  ) {
    return "ended"
  }
  const now = new Date()
  const start = new Date(startsAt)
  const end = endsAt
    ? new Date(endsAt)
    : new Date(start.getTime() + (durationMinutes || 60) * 60000)
  if (now > end) return "ended"
  if (now >= start && now <= end) return "live"
  return "scheduled"
}

export default function EventDetailPage() {
  const params = useParams()
  const eventId = params.id as string
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const { isLowBandwidth } = useLowBandwidth()

  const [event, setEvent] = useState<EventItem | null>(null)
  const [materials, setMaterials] = useState<EventMaterial[]>([])
  const [loading, setLoading] = useState(true)
  const [registering, setRegistering] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [playingVideo, setPlayingVideo] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    loadEvent()
  }, [eventId, retryKey])

  async function loadEvent() {
    setLoading(true)
    setError(null)
    try {
      const [ev, mats] = await Promise.all([
        learnerApi.getEvent(eventId),
        learnerApi.getEventMaterials(eventId).catch(() => []),
      ])
      setEvent(ev)
      setMaterials(mats)
    } catch {
      setError(tc("error.generic"))
    } finally {
      setLoading(false)
    }
  }

  async function handleRegister() {
    setRegistering(true)
    setError(null)
    try {
      await learnerApi.registerForEvent(eventId)
      setSuccess(t("detail.calendarAdded"))
      await loadEvent()
    } catch {
      setError(t("error.registerFailed"))
    } finally {
      setRegistering(false)
    }
  }

  async function handleCancel() {
    if (!window.confirm(tc("confirm"))) return
    setCancelling(true)
    setError(null)
    try {
      await learnerApi.cancelEventRegistration(eventId)
      setSuccess(t("detail.calendarAdded"))
      await loadEvent()
    } catch {
      setError(t("error.cancelFailed"))
    } finally {
      setCancelling(false)
    }
  }

  function handleAddToCalendar() {
    if (!event) return
    const start = new Date(event.startsAt)
    const end = event.endsAt
      ? new Date(event.endsAt)
      : new Date(start.getTime() + (event.durationMinutes || 60) * 60000)
    const tz = event.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone
    const fmtIcs = (d: Date) => d.toISOString().replace(/[-:]/g, "").split(".")[0] + "Z"
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
      `DTSTAMP:${fmtIcs(new Date())}`,
      `DTSTART;TZID=${tz}:${fmtIcsLocal(start)}`,
      `DTEND;TZID=${tz}:${fmtIcsLocal(end)}`,
      `SUMMARY:${event.title}`,
      `DESCRIPTION:${event.description || ""}`,
      event.location ? `LOCATION:${event.location}` : "",
      event.meetingUrl ? `URL:${event.meetingUrl}` : "",
      "END:VEVENT",
      "END:VCALENDAR",
    ]
      .filter(Boolean)
      .join("\r\n")
    const blob = new Blob([ics], { type: "text/calendar;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `${event.title.replace(/\s+/g, "_")}.ics`
    a.click()
    URL.revokeObjectURL(url)
  }

  function fmtIcsLocal(d: Date) {
    const pad = (n: number) => String(n).padStart(2, "0")
    return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
  }

  if (loading) {
    return (
      <div role="main" aria-label={tc("loading")}>
        <LoadingState />
      </div>
    )
  }

  if (error && !event) {
    return (
      <div role="main" className="mx-auto max-w-4xl space-y-4" aria-label={tc("error.generic")}>
        <Link
          href="/dashboard/learner/events"
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
          aria-label={t("detail.backToEvents")}
        >
          <ArrowLeft className="size-4" /> {t("detail.backToEvents")}
        </Link>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 text-center">
          <AlertCircle className="mx-auto mb-2 size-8 text-destructive/60" />
          <p className="text-sm text-destructive">{error}</p>
          <button
            onClick={() => setRetryKey((k) => k + 1)}
            aria-label={tc("retry")}
            className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-destructive underline hover:no-underline"
          >
            <RefreshCw className="size-3" />
            {tc("retry")}
          </button>
        </div>
      </div>
    )
  }

  if (!event) return null

  const liveStatus = getLiveStatus(event.startsAt, event.endsAt, event.durationMinutes, event.eventStatus || event.status)
  const isPast = liveStatus === "ended"
  const isLive = liveStatus === "live"
  const isCancelled = (event.eventStatus || event.status) === "CANCELLED"
  const isProcessing = event.recordingStatus === "PROCESSING" || event.eventStatus === "PROCESSING"
  const isRecordingFailed = (event.recordingStatus || "").toUpperCase() === "FAILED"
  const almostFull = isAlmostFull(event)
  const attended = Boolean(event.attended)
  const hostName = event.providerId || event.organizerName || event.presenterName || ""
  const hasCapacity = event.maxParticipants ? event.registeredCount < event.maxParticipants : true
  const canJoinViaMeeting = isLive && event.meetingUrl
  const preflightHref = `/dashboard/learner/events/${eventId}/preflight`
  const waitingHref = `/dashboard/learner/events/${eventId}/waiting`
  const statusLabel = isCancelled
    ? t("status.cancelled")
    : isLive
      ? t("detail.live")
      : isPast
        ? t("detail.pastEvent")
        : t("detail.scheduled")

  const relatedLessonHref =
    event.relatedLessonId && event.relatedCourseId
      ? `/dashboard/learner/courses/${event.relatedCourseId}/lessons/${event.relatedLessonId}`
      : event.relatedCourseId
        ? `/dashboard/learner/courses/${event.relatedCourseId}`
        : null

  const recordings = materials.filter((m) => m.materialType === "RECORDING" || m.materialType === "VIDEO")
  const documents = materials.filter((m) =>
    ["DOCUMENT", "PDF", "PRESENTATION", "IMAGE", "AUDIO", "LINK", "OTHER"].includes(m.materialType)
  )

  return (
    <div role="main" className="mx-auto max-w-4xl space-y-6" aria-label={event.title}>
      <Link
        href="/dashboard/learner/events"
        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
        aria-label={t("detail.backToEvents")}
      >
        <ArrowLeft className="size-4" /> {t("detail.backToEvents")}
      </Link>

      {playingVideo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-4xl">
            <VideoPlayer url={playingVideo} onClose={() => setPlayingVideo(null)} />
          </div>
        </div>
      )}

      {success && (
        <div className="flex items-center gap-2 rounded-2xl border border-green-500/20 bg-green-500/5 p-4 text-sm text-green-600">
          <CheckCircle className="size-4 shrink-0" /> {success}
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
          <XCircle className="size-4 shrink-0" /> {error}
        </div>
      )}

      {isLowBandwidth && (
        <div role="status" className="flex items-center gap-2 rounded-2xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-700">
          <WifiOff className="size-3.5 shrink-0" aria-hidden="true" />
          {t("lowBandwidth.notice")}
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex flex-wrap items-start gap-2">
          <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${getTypeColor(event.eventType)}`}>
            {event.eventType}
          </span>
          {event.category && (
            <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              {event.category}
            </span>
          )}
          {event.accessLevel && (
            <span
              className="inline-flex items-center gap-1 rounded-full bg-indigo-500/10 px-3 py-1 text-xs font-semibold text-indigo-600"
              title={t(`admin.accessLevels.${event.accessLevel}` as any)}
            >
              <Globe className="size-3" aria-hidden="true" />
              {t(`admin.accessLevels.${event.accessLevel}` as any)}
            </span>
          )}
          {almostFull && !isPast && !isCancelled && (
            <span
              className="inline-flex items-center rounded-full bg-amber-500/10 px-3 py-1 text-xs font-semibold text-amber-600"
              role="status"
              title={t("status.almostFull")}
            >
              {t("status.almostFull")}
            </span>
          )}
          {isCancelled && (
            <span className="inline-flex items-center rounded-full bg-destructive/10 px-3 py-1 text-xs font-semibold text-destructive">
              {t("status.cancelled")}
            </span>
          )}
          {isLive && !isCancelled && (
            <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-3 py-1 text-xs font-semibold text-green-600 animate-pulse">
              <span className="size-1.5 rounded-full bg-green-500" aria-hidden="true" />
              {t("detail.live")}
              <span className="sr-only">{t("detail.live")}</span>
            </span>
          )}
          {isPast && !isCancelled && (
            <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              {t("detail.pastEvent")}
            </span>
          )}
          {!isLive && !isPast && !isCancelled && (
            <span className="inline-flex items-center rounded-full bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-600">
              {t("detail.scheduled")}
            </span>
          )}
          <span className="sr-only" role="status">{statusLabel}</span>
          {event.isRegistered && (
            <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-3 py-1 text-xs font-semibold text-green-600">
              <CheckCircle className="size-3" aria-hidden="true" /> {t("registered.badge")}
            </span>
          )}
          {event.eventStatus === "RESCHEDULED" && event.rescheduledFrom && (
            <span className="inline-flex items-center rounded-full bg-amber-500/10 px-3 py-1 text-xs font-semibold text-amber-600">
              {t("status.rescheduled")}
            </span>
          )}
        </div>

        <h1 className="mt-4 text-2xl font-bold text-foreground">{event.title}</h1>
        {(hostName) && (
          <p className="mt-1 text-sm text-muted-foreground">
            {t("detail.organizer", { name: event.presenterName || event.organizerName || hostName })}
          </p>
        )}
        {event.providerId && (
          <p className="mt-0.5 text-xs text-muted-foreground">
            {t("detail.provider")}: <span className="font-medium text-foreground/80">{event.providerId}</span>
          </p>
        )}
        {event.cancellationReason && isCancelled && (
          <p className="mt-1 text-sm text-destructive">{event.cancellationReason}</p>
        )}
        {isProcessing && (
          <p className="mt-1 text-sm text-amber-600" role="status">{t("status.processing")}</p>
        )}
        {isRecordingFailed && (
          <div className="mt-2 flex items-start gap-2 rounded-xl border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive" role="alert">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <div>
              <p className="font-medium">{t("status.recordingFailed")}</p>
              <p className="mt-0.5 text-xs opacity-90">{t("status.recordingFailedDesc")}</p>
            </div>
          </div>
        )}

        <div className="mt-6 grid gap-6 md:grid-cols-2">
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <CalendarDays className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              <div>
                <p className="font-medium text-foreground">{formatDate(event.startsAt)}</p>
                <p className="text-sm text-muted-foreground">
                  {formatTime(event.startsAt)}
                  {event.endsAt ? ` - ${formatTime(event.endsAt)}` : ""}
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Clock className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              <p className="text-foreground">{event.durationMinutes || 60} min</p>
            </div>
            {event.location && (
              <div className="flex items-start gap-3">
                <MapPin className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <p className="text-foreground">{event.location}</p>
              </div>
            )}
            {event.maxParticipants && (
              <div className="flex items-start gap-3">
                <Users className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <p className="text-foreground">
                  {event.registeredCount} / {event.maxParticipants} {t("detail.capacity").toLowerCase()}
                </p>
              </div>
            )}
            {event.timezone && (
              <div className="flex items-start gap-3">
                <Clock className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("detail.timezone")}</p>
                  <p className="text-foreground">{event.timezone}</p>
                </div>
              </div>
            )}
            {(event.presenterName || event.organizerName) && (
              <div className="flex items-start gap-3">
                <Users className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("detail.presenter")}</p>
                  <p className="text-foreground">{event.presenterName || event.organizerName}</p>
                </div>
              </div>
            )}
            {event.relatedCourseId && (
              <div className="flex items-start gap-3">
                <BookOpen className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("detail.relatedCourse")}</p>
                  <Link
                    href={`/dashboard/learner/courses/${event.relatedCourseId}`}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {event.relatedCourseTitle || t("detail.course")}
                  </Link>
                  {event.relatedLessonId && (
                    <Link
                      href={`/dashboard/learner/courses/${event.relatedCourseId}/lessons/${event.relatedLessonId}`}
                      className="mt-0.5 block text-sm font-medium text-primary hover:underline"
                      aria-label={t("detail.lesson")}
                    >
                      {t("detail.lesson")}: {event.relatedLessonId}
                    </Link>
                  )}
                </div>
              </div>
            )}
            {!event.relatedCourseId && event.relatedLessonId && relatedLessonHref && (
              <div className="flex items-start gap-3">
                <BookOpen className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("detail.lesson")}</p>
                  <Link href={relatedLessonHref} className="text-sm font-medium text-primary hover:underline">
                    {t("detail.lesson")}
                  </Link>
                </div>
              </div>
            )}
          </div>

          <div className="space-y-3">
            {isCancelled && (
              <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
                {t("status.cancelled")}{event.cancellationReason ? `: ${event.cancellationReason}` : ""}
              </div>
            )}

            {isLive && !isCancelled && (
              <Link
                href={preflightHref}
                aria-label={t("preflight.joinLive")}
                className="flex h-12 items-center justify-center gap-2 rounded-xl bg-green-600 text-sm font-medium text-white hover:bg-green-700"
              >
                {t("preflight.joinLive")}
              </Link>
            )}

            {canJoinViaMeeting && (
              <a
                href={event.meetingUrl!}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${t("detail.joinLiveSession")} — ${t("join.external")}`}
                className="flex h-10 items-center justify-center gap-2 rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted"
              >
                {t("detail.joinLiveSession")}
                <ExternalLink className="size-3.5" aria-hidden="true" />
                <span className="sr-only">{t("join.external")}</span>
              </a>
            )}

            {!isPast && !isLive && !isCancelled && (
              <>
                {event.isRegistered ? (
                  <div className="space-y-2">
                    <div className="rounded-xl bg-green-500/5 border border-green-500/20 p-3 text-sm text-green-600">
                      <CheckCircle className="mb-1 size-4 inline" aria-hidden="true" /> {t("registered.youAreRegistered")}
                    </div>
                    <Link
                      href={waitingHref}
                      className="flex h-10 items-center justify-center rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted"
                    >
                      {t("waitingRoom")}
                    </Link>
                    <div className="flex gap-2">
                      <button
                        onClick={handleAddToCalendar}
                        aria-label={t("registered.addToCalendar")}
                        className="flex h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted"
                      >
                        <CalendarDays className="size-4" aria-hidden="true" />
                        {t("registered.addToCalendar")}
                      </button>
                      <button
                        onClick={handleCancel}
                        disabled={cancelling}
                        aria-label={t("registered.cancelRegistration")}
                        className="flex h-10 flex-1 items-center justify-center rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
                      >
                        {cancelling ? <Loader2 className="size-4 animate-spin" aria-hidden="true" /> : t("registered.cancelRegistration")}
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    onClick={handleRegister}
                    disabled={registering || !hasCapacity}
                    aria-label={!hasCapacity ? t("upcoming.eventFull") : almostFull ? t("status.almostFull") : t("upcoming.register")}
                    className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                  >
                    {registering ? (
                      <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                    ) : !hasCapacity ? (
                      t("upcoming.eventFull")
                    ) : (
                      t("upcoming.register")
                    )}
                  </button>
                )}
              </>
            )}

            {isPast && attended && (
              <Link
                href="/dashboard/learner/events/registered"
                aria-label={t("past.viewAttendance")}
                className="flex h-11 items-center justify-center gap-2 rounded-xl border border-teal-500/30 bg-teal-500/10 text-sm font-medium text-teal-700 hover:bg-teal-500/20"
              >
                <CheckCircle className="size-4" aria-hidden="true" />
                {t("past.viewAttendance")}
              </Link>
            )}
            {isPast && !attended && event.isRegistered && (
              <Link
                href="/dashboard/learner/events/registered"
                aria-label={t("past.registeredEvents")}
                className="flex h-10 items-center justify-center rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted"
              >
                {t("past.registeredEvents")}
              </Link>
            )}

            <div className="rounded-xl bg-muted/50 p-4 text-sm">
              <p className="font-medium text-foreground">{event.isFree ? t("detail.freeEvent") : t("detail.paidEvent")}</p>
              {event.requiresApproval && (
                <p className="mt-1 text-muted-foreground">{t("upcoming.requiresApproval")}</p>
              )}
            </div>
          </div>
        </div>

        {event.description && (
          <div className="mt-6 border-t border-border pt-6">
            <h2 className="mb-3 text-lg font-semibold text-foreground">{t("detail.aboutEvent")}</h2>
            <p className="whitespace-pre-line text-sm text-muted-foreground">{event.description}</p>
          </div>
        )}

        {event.tags && (
          <div className="mt-4 flex flex-wrap gap-2">
            {event.tags.split(",").map((tag, i) => (
              <span key={i} className="inline-flex items-center gap-1 rounded-full bg-muted px-3 py-1 text-xs text-muted-foreground">
                <Tag className="size-3" /> {tag.trim()}
              </span>
            ))}
          </div>
        )}
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("detail.beforeEvent")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {event.isRegistered ? t("registered.youAreRegistered") : t("upcoming.register")}
            {" · "}
            <button type="button" onClick={handleAddToCalendar} className="text-primary hover:underline">
              {t("registered.addToCalendar")}
            </button>
          </p>
          <Link href={preflightHref} className="mt-2 inline-block text-xs font-medium text-primary hover:underline">
            {t("preflight.title")}
          </Link>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("detail.duringEvent")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {isLive ? t("detail.joinLiveSession") : t("waiting.sessionNotStarted")}
          </p>
          <ul className="mt-2 space-y-1.5 text-xs text-muted-foreground">
            <li>{t("during.connection")}</li>
            <li>{t("during.attendance")}</li>
            <li>{t("during.questions")}</li>
            <li>{t("during.reconnect")}</li>
          </ul>
          {isLive && (
            <Link href={preflightHref} className="mt-2 inline-block text-xs font-medium text-primary hover:underline" aria-label={t("preflight.joinLive")}>
              {t("preflight.joinLive")}
            </Link>
          )}
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("detail.afterEvent")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {isRecordingFailed
              ? t("status.recordingFailed")
              : isProcessing
                ? t("status.processing")
                : event.hasRecording || event.eventStatus === "REPLAY_AVAILABLE"
                  ? t("past.watchReplay")
                  : t("past.noReplay")}
          </p>
          {(event.hasRecording || event.eventStatus === "REPLAY_AVAILABLE") && !isRecordingFailed && (
            <Link href="/dashboard/learner/replays" className="mt-2 inline-block text-xs font-medium text-primary hover:underline" aria-label={t("replays.title")}>
              {t("replays.title")}
            </Link>
          )}
          {attended && (
            <Link href="/dashboard/learner/events/registered" className="mt-1 block text-xs font-medium text-teal-700 hover:underline" aria-label={t("past.viewAttendance")}>
              {t("past.viewAttendance")}
            </Link>
          )}
        </div>
      </div>

      {(recordings.length > 0 || documents.length > 0) && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="mb-4 text-lg font-semibold text-foreground">{t("detail.materials")}</h2>

          {recordings.length > 0 && (
            <div className="mb-6">
              <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {t("detail.recordingsAndVideos")}
              </h3>
              <div className="space-y-2">
                {recordings.map((mat) => (
                  <div
                    key={mat.id}
                    className="flex items-center justify-between rounded-xl border border-border p-3 hover:bg-muted/50"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex size-9 items-center justify-center rounded-lg bg-purple-500/10 text-purple-600">
                        <Play className="size-4" />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">{mat.title}</p>
                        {mat.durationMinutes && (
                          <p className="text-xs text-muted-foreground">{mat.durationMinutes} min</p>
                        )}
                      </div>
                    </div>
                    <button
                      onClick={() => setPlayingVideo(mat.fileUrl)}
                      aria-label={`${tc("view")} - ${mat.title}`}
                      className="flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20"
                    >
                      <Play className="size-3" /> {tc("view")}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {documents.length > 0 && (
            <div>
              <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {t("detail.documentsAndMaterials")}
              </h3>
              <div className="space-y-2">
                {documents.map((mat) => (
                  <a
                    key={mat.id}
                    href={mat.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`${tc("view")} - ${mat.title}`}
                    className="flex items-center justify-between rounded-xl border border-border p-3 hover:bg-muted/50"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex size-9 items-center justify-center rounded-lg bg-blue-500/10 text-blue-600">
                        {getMaterialIcon(mat.materialType)}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">{mat.title}</p>
                        <p className="text-xs text-muted-foreground">{mat.materialType}</p>
                      </div>
                    </div>
                    <Download className="size-4 text-muted-foreground" />
                  </a>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {isPast && recordings.length === 0 && documents.length === 0 && !isRecordingFailed && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="mb-2 text-lg font-semibold text-foreground">{t("detail.materials")}</h2>
          <EmptyState
            icon={<FileText className="size-8" />}
            title={t("detail.noMaterials")}
          />
        </div>
      )}
    </div>
  )
}
