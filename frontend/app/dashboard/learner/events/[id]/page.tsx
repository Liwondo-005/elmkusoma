"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { learnerApi, type EventItem, type EventMaterial } from "@/lib/learner-api"
import { VideoPlayer } from "@/components/events/video-player"
import { CalendarDays, Clock, MapPin, Users, ArrowLeft, CheckCircle, XCircle, Loader2, Download, Play, FileText, Presentation, File, Tag } from "lucide-react"

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric", year: "numeric" })
}

function formatTime(d: string) {
  return new Date(d).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
}

function getTypeColor(type: string) {
  const m: Record<string, string> = {
    SEMINAR: "bg-blue-100 text-blue-800", WORKSHOP: "bg-green-100 text-green-800",
    WEBINAR: "bg-purple-100 text-purple-800", TRAINING: "bg-orange-100 text-orange-800",
    CONFERENCE: "bg-red-100 text-red-800", LECTURE: "bg-teal-100 text-teal-800",
  }
  return m[type] || "bg-gray-100 text-gray-800"
}

function getMaterialIcon(type: string) {
  switch (type) {
    case "RECORDING": case "VIDEO": return <Play className="size-4" />
    case "DOCUMENT": case "PDF": return <FileText className="size-4" />
    case "PRESENTATION": return <Presentation className="size-4" />
    default: return <File className="size-4" />
  }
}

export default function EventDetailPage() {
  const params = useParams()
  const router = useRouter()
  const eventId = params.id as string
  const [event, setEvent] = useState<EventItem | null>(null)
  const [materials, setMaterials] = useState<EventMaterial[]>([])
  const [loading, setLoading] = useState(true)
  const [registering, setRegistering] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [playingVideo, setPlayingVideo] = useState<string | null>(null)

  useEffect(() => {
    loadEvent()
  }, [eventId])

  async function loadEvent() {
    setLoading(true)
    try {
      const [ev, mats] = await Promise.all([
        learnerApi.getEvent(eventId),
        learnerApi.getEventMaterials(eventId),
      ])
      setEvent(ev)
      setMaterials(mats)
    } catch (e: any) {
      setError(e.message || "Failed to load event")
    } finally {
      setLoading(false)
    }
  }

  async function handleRegister() {
    setRegistering(true)
    setError("")
    try {
      await learnerApi.registerForEvent(eventId)
      setSuccess("Registration successful!")
      await loadEvent()
    } catch (e: any) {
      setError(e.message || "Registration failed")
    } finally {
      setRegistering(false)
    }
  }

  async function handleCancel() {
    if (!window.confirm("Are you sure you want to cancel your registration for this event?")) return
    setCancelling(true)
    setError("")
    try {
      await learnerApi.cancelEventRegistration(eventId)
      setSuccess("Registration cancelled")
      await loadEvent()
    } catch (e: any) {
      setError(e.message || "Cancellation failed")
    } finally {
      setCancelling(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  if (error && !event) {
    return (
      <div className="space-y-4">
        <Link href="/dashboard/learner/events" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Back to Events
        </Link>
        <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      </div>
    )
  }

  if (!event) return null

  const now = new Date()
  const start = new Date(event.startsAt)
  const end = event.endsAt ? new Date(event.endsAt) : new Date(start.getTime() + (event.durationMinutes || 60) * 60000)
  const isPast = now > end
  const isLive = now >= start && now <= end
  const hasCapacity = event.maxParticipants ? event.registeredCount < event.maxParticipants : true

  const recordings = materials.filter((m) => m.materialType === "RECORDING" || m.materialType === "VIDEO")
  const documents = materials.filter((m) => ["DOCUMENT", "PDF", "PRESENTATION", "IMAGE", "AUDIO", "LINK", "OTHER"].includes(m.materialType))

  return (
    <div className="space-y-6">
      <Link href="/dashboard/learner/events" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Back to Events
      </Link>

      {playingVideo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-4xl">
            <VideoPlayer url={playingVideo} onClose={() => setPlayingVideo(null)} />
          </div>
        </div>
      )}

      {success && (
        <div className="flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">
          <CheckCircle className="size-4 shrink-0" /> {success}
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <XCircle className="size-4 shrink-0" /> {error}
        </div>
      )}

      <div className="rounded-xl border border-border bg-card p-6">
        <div className="flex flex-wrap items-start gap-3">
          <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${getTypeColor(event.eventType)}`}>
            {event.eventType}
          </span>
          {event.category && <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-xs font-medium">{event.category}</span>}
          {isLive && <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-3 py-1 text-xs font-medium text-red-700 animate-pulse">● Live Now</span>}
          {isPast && <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600">Past Event</span>}
          {event.isRegistered && <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-700"><CheckCircle className="size-3" /> Registered</span>}
        </div>

        <h1 className="mt-4 text-2xl font-bold">{event.title}</h1>
        {event.organizerName && <p className="mt-1 text-sm text-muted-foreground">Organized by {event.organizerName}</p>}

        <div className="mt-6 grid gap-6 md:grid-cols-2">
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <CalendarDays className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              <div>
                <p className="font-medium">{formatDate(event.startsAt)}</p>
                <p className="text-sm text-muted-foreground">
                  {formatTime(event.startsAt)} {event.endsAt ? `- ${formatTime(event.endsAt)}` : ""}
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Clock className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              <p>{event.durationMinutes || 60} minutes</p>
            </div>
            {event.location && (
              <div className="flex items-start gap-3">
                <MapPin className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <p>{event.location}</p>
              </div>
            )}
            {event.maxParticipants && (
              <div className="flex items-start gap-3">
                <Users className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <p>{event.registeredCount} / {event.maxParticipants} registered</p>
              </div>
            )}
          </div>

          <div className="space-y-4">
            {!isPast && !isLive && event.status === "PUBLISHED" && (
              <div>
                {event.isRegistered ? (
                  <div className="space-y-2">
                    <div className="rounded-lg bg-green-50 p-3 text-sm text-green-700">
                      <CheckCircle className="mb-1 size-4" /> You are registered for this event
                    </div>
                    {event.meetingUrl && (
                      <a href={event.meetingUrl} target="_blank" rel="noopener noreferrer"
                        className="flex h-10 items-center justify-center rounded-lg bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90">
                        Join Meeting
                      </a>
                    )}
                    <button onClick={handleCancel} disabled={cancelling}
                      className="flex h-10 w-full items-center justify-center rounded-lg border border-border text-sm font-medium hover:bg-muted disabled:opacity-50">
                      {cancelling ? <Loader2 className="size-4 animate-spin" /> : "Cancel Registration"}
                    </button>
                  </div>
                ) : (
                  <button onClick={handleRegister} disabled={registering || !hasCapacity}
                    className="flex h-12 w-full items-center justify-center rounded-lg bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
                    {registering ? <Loader2 className="size-4 animate-spin" /> :
                     !hasCapacity ? "Event Full" :
                     "Register for Event"}
                  </button>
                )}
              </div>
            )}

            {isLive && event.meetingUrl && (
              <a href={event.meetingUrl} target="_blank" rel="noopener noreferrer"
                className="flex h-12 items-center justify-center rounded-lg bg-red-600 text-sm font-medium text-white hover:bg-red-700">
                Join Live Session
              </a>
            )}

            <div className="rounded-lg bg-muted/50 p-4 text-sm">
              <p className="font-medium">{event.isFree ? "Free Event" : "Paid Event"}</p>
              {event.requiresApproval && <p className="mt-1 text-muted-foreground">Requires approval to attend</p>}
            </div>
          </div>
        </div>

        {event.description && (
          <div className="mt-6 border-t border-border pt-6">
            <h2 className="mb-3 text-lg font-semibold">About this event</h2>
            <p className="whitespace-pre-line text-sm text-muted-foreground">{event.description}</p>
          </div>
        )}

        {event.tags && (
          <div className="mt-4 flex flex-wrap gap-2">
            {event.tags.split(",").map((tag, i) => (
              <span key={i} className="inline-flex items-center gap-1 rounded-full bg-muted px-3 py-1 text-xs">
                <Tag className="size-3" /> {tag.trim()}
              </span>
            ))}
          </div>
        )}
      </div>

      {isPast && recordings.length === 0 && documents.length === 0 && (
        <div className="rounded-xl border border-border bg-card p-6">
          <h2 className="mb-2 text-lg font-semibold">Event Materials</h2>
          <p className="text-sm text-muted-foreground">No materials or recordings available for this event yet.</p>
        </div>
      )}

      {(recordings.length > 0 || documents.length > 0) && (
        <div className="rounded-xl border border-border bg-card p-6">
          <h2 className="mb-4 text-lg font-semibold">Event Materials</h2>

          {recordings.length > 0 && (
            <div className="mb-6">
              <h3 className="mb-3 text-sm font-medium text-muted-foreground">Recordings &amp; Videos</h3>
              <div className="space-y-2">
                {recordings.map((mat) => (
                  <div key={mat.id} className="flex items-center justify-between rounded-lg border border-border p-3 hover:bg-muted/50">
                    <div className="flex items-center gap-3">
                      <div className="flex size-9 items-center justify-center rounded-lg bg-purple-100 text-purple-700">
                        <Play className="size-4" />
                      </div>
                      <div>
                        <p className="text-sm font-medium">{mat.title}</p>
                        {mat.durationMinutes && <p className="text-xs text-muted-foreground">{mat.durationMinutes} min</p>}
                      </div>
                    </div>
                    <button onClick={() => setPlayingVideo(mat.fileUrl)}
                      className="flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20">
                      <Play className="size-3" /> Watch
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {documents.length > 0 && (
            <div>
              <h3 className="mb-3 text-sm font-medium text-muted-foreground">Documents &amp; Materials</h3>
              <div className="space-y-2">
                {documents.map((mat) => (
                  <a key={mat.id} href={mat.fileUrl} target="_blank" rel="noopener noreferrer"
                    className="flex items-center justify-between rounded-lg border border-border p-3 hover:bg-muted/50">
                    <div className="flex items-center gap-3">
                      <div className="flex size-9 items-center justify-center rounded-lg bg-blue-100 text-blue-700">
                        {getMaterialIcon(mat.materialType)}
                      </div>
                      <div>
                        <p className="text-sm font-medium">{mat.title}</p>
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
    </div>
  )
}
