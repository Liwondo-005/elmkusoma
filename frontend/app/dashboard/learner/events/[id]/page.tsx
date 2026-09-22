"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, type EventItem, type EventMaterial } from "@/lib/learner-api"
import { VideoPlayer } from "@/components/events/video-player"
import { EmptyState, LoadingState } from "@/components/learner/shared"
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
  const now = new Date()
  const start = new Date(startsAt)
  const end = endsAt
    ? new Date(endsAt)
    : new Date(start.getTime() + (durationMinutes || 60) * 60000)
  if (eventStatus === "COMPLETED" || now > end) return "ended"
  if (now >= start && now <= end) return "live"
  return "scheduled"
}

export default function EventDetailPage() {
  const params = useParams()
  const eventId = params.id as string
  const t = useTranslations("events")
  const tc = useTranslations("common")

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
      setError(tc("error"))
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
    const ics = [
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "BEGIN:VEVENT",
      `DTSTART:${start.toISOString().replace(/[-:]/g, "").split(".")[0]}Z`,
      `DTEND:${end.toISOString().replace(/[-:]/g, "").split(".")[0]}Z`,
      `SUMMARY:${event.title}`,
      `DESCRIPTION:${event.description || ""}`,
      event.location ? `LOCATION:${event.location}` : "",
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

  if (loading) {
    return (
      <div role="main" aria-label={tc("loading")}>
        <LoadingState />
      </div>
    )
  }

  if (error && !event) {
    return (
      <div role="main" className="mx-auto max-w-4xl space-y-4" aria-label={tc("error")}>
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

  const liveStatus = getLiveStatus(event.startsAt, event.endsAt, event.durationMinutes, event.status)
  const isPast = liveStatus === "ended"
  const isLive = liveStatus === "live"
  const hasCapacity = event.maxParticipants ? event.registeredCount < event.maxParticipants : true

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
          {isLive && (
            <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-3 py-1 text-xs font-semibold text-green-600 animate-pulse">
              <span className="size-1.5 rounded-full bg-green-500" />
              {t("detail.live")}
            </span>
          )}
          {isPast && (
            <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              {t("detail.pastEvent")}
            </span>
          )}
          {!isLive && !isPast && (
            <span className="inline-flex items-center rounded-full bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-600">
              {t("detail.scheduled")}
            </span>
          )}
          {event.isRegistered && (
            <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-3 py-1 text-xs font-semibold text-green-600">
              <CheckCircle className="size-3" /> {t("registered.badge")}
            </span>
          )}
        </div>

        <h1 className="mt-4 text-2xl font-bold text-foreground">{event.title}</h1>
        {event.organizerName && (
          <p className="mt-1 text-sm text-muted-foreground">{t("detail.organizer", { name: event.organizerName })}</p>
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
          </div>

          <div className="space-y-3">
            {isLive && event.meetingUrl && (
              <a
                href={event.meetingUrl}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={t("detail.joinLiveSession")}
                className="flex h-12 items-center justify-center gap-2 rounded-xl bg-green-600 text-sm font-medium text-white hover:bg-green-700"
              >
                {t("detail.joinLiveSession")}
              </a>
            )}

            {!isPast && !isLive && event.status === "PUBLISHED" && (
              <>
                {event.isRegistered ? (
                  <div className="space-y-2">
                    <div className="rounded-xl bg-green-500/5 border border-green-500/20 p-3 text-sm text-green-600">
                      <CheckCircle className="mb-1 size-4 inline" /> {t("registered.youAreRegistered")}
                    </div>
                    {event.meetingUrl && (
                      <a
                        href={event.meetingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={t("registered.meetingUrl")}
                        className="flex h-10 items-center justify-center rounded-xl bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90"
                      >
                        {t("registered.meetingUrl")}
                      </a>
                    )}
                    <div className="flex gap-2">
                      <button
                        onClick={handleAddToCalendar}
                        aria-label={t("registered.addToCalendar")}
                        className="flex h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted"
                      >
                        <CalendarDays className="size-4" />
                        {t("registered.addToCalendar")}
                      </button>
                      <button
                        onClick={handleCancel}
                        disabled={cancelling}
                        aria-label={t("registered.cancelRegistration")}
                        className="flex h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-border bg-background text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
                      >
                        {cancelling ? <Loader2 className="size-4 animate-spin" /> : t("registered.cancelRegistration")}
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    onClick={handleRegister}
                    disabled={registering || !hasCapacity}
                    aria-label={!hasCapacity ? t("upcoming.eventFull") : t("upcoming.register")}
                    className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                  >
                    {registering ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : !hasCapacity ? (
                      t("upcoming.eventFull")
                    ) : (
                      t("upcoming.register")
                    )}
                  </button>
                )}
              </>
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
            {t("registered.youAreRegistered")} {t("registered.addToCalendar")}
          </p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("detail.duringEvent")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {isLive ? t("detail.joinLiveSession") : t("detail.scheduled")}
          </p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{t("detail.afterEvent")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {event.hasRecording ? t("past.watchReplay") : t("past.noReplay")}
          </p>
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

      {isPast && recordings.length === 0 && documents.length === 0 && (
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
