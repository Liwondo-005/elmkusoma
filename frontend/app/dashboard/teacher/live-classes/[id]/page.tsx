"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { appFetch } from "@/lib/fetch"
import { Button } from "@/components/ui/button"
import {
  ArrowLeft, Video, Calendar, Clock, Users, BookOpen, GraduationCap,
  Loader2, Play, Square, Edit, Trash2, ExternalLink, CheckCircle2,
  AlertCircle, Settings, User
} from "lucide-react"

interface LiveClass {
  id: string
  title: string
  description: string
  scheduledAt: string
  durationMinutes: number
  status: string
  maxParticipants: number
  subjectName: string | null
  teacherName: string | null
  classGroupId: string | null
  subjectId: string | null
  recordingUrl: string | null
  currentParticipants: number | null
  canJoin: boolean | null
  createdAt: string
  recordingEnabled: boolean | null
}

interface Participant {
  id: string
  userId: string
  userName: string
  role: string
  joinedAt: string | null
  leftAt: string | null
  durationSeconds: number | null
  online: boolean
}

const statusConfig: Record<string, { label: string; className: string; description: string }> = {
  SCHEDULED: { label: "Scheduled", className: "bg-blue-100 text-blue-700", description: "This class is scheduled and waiting to start." },
  STARTING: { label: "Starting", className: "bg-amber-100 text-amber-700", description: "The class is about to begin." },
  IN_PROGRESS: { label: "Live", className: "bg-green-100 text-green-700", description: "This class is currently live." },
  LIVE: { label: "Live", className: "bg-green-100 text-green-700", description: "This class is currently live." },
  ENDING: { label: "Ending", className: "bg-amber-100 text-amber-700", description: "The class is ending." },
  COMPLETED: { label: "Completed", className: "bg-gray-100 text-gray-700", description: "This class has ended." },
  ENDED: { label: "Ended", className: "bg-gray-100 text-gray-700", description: "This class has ended." },
  CANCELLED: { label: "Cancelled", className: "bg-red-100 text-red-700", description: "This class has been cancelled." },
}

export default function LiveClassDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useAuth()
  const id = params.id as string
  const [liveClass, setLiveClass] = useState<LiveClass | null>(null)
  const [participants, setParticipants] = useState<Participant[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState(false)

  useEffect(() => {
    if (!id) return
    loadClass()
  }, [id])

  async function loadClass() {
    try {
      setLoading(true)
      setError(null)
      const data = await appFetch<LiveClass>(`/v1/teachers/me/live-classes/${id}`)
      setLiveClass(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load live class")
    } finally {
      setLoading(false)
    }
  }

  async function loadParticipants() {
    try {
      const data = await appFetch<Participant[]>(`/v1/teachers/me/live-classes/${id}/participants`)
      setParticipants(data)
    } catch {
    }
  }

  async function handleStartLive() {
    try {
      setActionLoading(true)
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}/start`, { method: "POST" })
      setSuccess("Live class started!")
      loadClass()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start live class")
    } finally {
      setActionLoading(false)
    }
  }

  async function handleEndLive() {
    if (!confirm("End this live class? Students will no longer be able to join.")) return
    try {
      setActionLoading(true)
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}/end`, { method: "POST" })
      setSuccess("Live class ended")
      loadClass()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to end live class")
    } finally {
      setActionLoading(false)
    }
  }

  async function handleCancel() {
    if (!confirm("Are you sure you want to cancel this live class? This cannot be undone.")) return
    try {
      setActionLoading(true)
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}`, { method: "DELETE" })
      setSuccess("Live class cancelled")
      loadClass()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel live class")
    } finally {
      setActionLoading(false)
    }
  }

  function formatDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  function formatShortDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
    })
  }

  function formatTime(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleTimeString(undefined, {
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  function getEndTime(scheduledAt: string, durationMinutes: number) {
    const start = new Date(scheduledAt)
    start.setMinutes(start.getMinutes() + durationMinutes)
    return start.toISOString()
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error && !liveClass) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-6">
        <Link href="/dashboard/teacher/live-classes" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" />
          Back to Live Classes
        </Link>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center">
          <AlertCircle className="mx-auto mb-3 size-10 text-destructive" />
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      </div>
    )
  }

  if (!liveClass) return null

  const status = statusConfig[liveClass.status] || statusConfig.SCHEDULED
  const isActive = liveClass.status === "IN_PROGRESS" || liveClass.status === "LIVE"
  const isScheduled = liveClass.status === "SCHEDULED"
  const isEnded = liveClass.status === "COMPLETED" || liveClass.status === "ENDED" || liveClass.status === "CANCELLED"

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <Link href="/dashboard/teacher/live-classes" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" />
        Back to Live Classes
      </Link>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-4">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <CheckCircle2 className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {/* Header */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-4">
            <div className="flex size-12 items-center justify-center rounded-xl bg-primary/10 shrink-0">
              <Video className="size-6 text-primary" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-foreground">{liveClass.title}</h1>
              <div className="mt-1 flex items-center gap-2">
                <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${status.className}`}>
                  {status.label}
                </span>
                <span className="text-xs text-muted-foreground">{status.description}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Details Grid */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
          <h2 className="text-sm font-semibold text-foreground">Schedule</h2>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm">
              <Calendar className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-foreground">{formatShortDate(liveClass.scheduledAt)}</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Clock className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-foreground">
                {formatTime(liveClass.scheduledAt)} – {formatTime(getEndTime(liveClass.scheduledAt, liveClass.durationMinutes))}
              </span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Clock className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-muted-foreground">{liveClass.durationMinutes} minutes</span>
            </div>
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <span>Timezone: Africa/Dar_es_Salaam (UTC+03:00)</span>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
          <h2 className="text-sm font-semibold text-foreground">Class Details</h2>
          <div className="space-y-2">
            {liveClass.subjectName && (
              <div className="flex items-center gap-2 text-sm">
                <BookOpen className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">{liveClass.subjectName}</span>
              </div>
            )}
            <div className="flex items-center gap-2 text-sm">
              <Users className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-foreground">{liveClass.maxParticipants} max participants</span>
            </div>
            {liveClass.currentParticipants != null && (
              <div className="flex items-center gap-2 text-sm">
                <User className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">{liveClass.currentParticipants} joined</span>
              </div>
            )}
            <div className="flex items-center gap-2 text-sm">
              <Settings className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-muted-foreground">
                Recording: {liveClass.recordingEnabled ? (liveClass.recordingUrl ? "Available" : "Enabled") : "Not enabled"}
              </span>
            </div>
          </div>
        </div>
      </div>

      {liveClass.description && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-sm font-semibold text-foreground mb-2">Description</h2>
          <p className="text-sm text-muted-foreground whitespace-pre-line">{liveClass.description}</p>
        </div>
      )}

      {/* Participants */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-foreground">Participants</h2>
          <Button variant="outline" size="sm" onClick={loadParticipants}>
            Load Participants
          </Button>
        </div>
        {participants.length === 0 ? (
          <p className="text-xs text-muted-foreground">No participants yet. Click &quot;Load Participants&quot; to view.</p>
        ) : (
          <div className="space-y-2">
            {participants.map((p) => (
              <div key={p.id} className="flex items-center justify-between rounded-lg border border-border bg-background px-4 py-2.5">
                <div className="flex items-center gap-3">
                  <div className={`size-2 rounded-full ${p.online ? "bg-green-500" : "bg-gray-300"}`} />
                  <div>
                    <p className="text-sm font-medium text-foreground">{p.userName}</p>
                    <p className="text-[10px] text-muted-foreground">{p.role}</p>
                  </div>
                </div>
                <div className="text-right text-xs text-muted-foreground">
                  {p.joinedAt && <p>Joined {formatShortDate(p.joinedAt)}</p>}
                  {p.durationSeconds && <p>{Math.round(p.durationSeconds / 60)} min</p>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-sm font-semibold text-foreground mb-4">Actions</h2>
        <div className="flex flex-wrap gap-2">
          {isScheduled && (
            <>
              <Button
                className="gap-1 bg-green-600 hover:bg-green-700 text-white"
                onClick={handleStartLive}
                disabled={actionLoading}
              >
                {actionLoading ? <Loader2 className="size-4 animate-spin" /> : <Play className="size-4" />}
                Start Live
              </Button>
              <Button
                variant="outline"
                className="gap-1"
                onClick={() => window.open(`/dashboard/teacher/live-classes/${id}/prepare`, "_blank")}
              >
                <Settings className="size-4" />
                Prepare
              </Button>
              <Button
                variant="outline"
                className="gap-1"
                onClick={() => router.push(`/dashboard/teacher/live-classes?edit=${id}`)}
              >
                <Edit className="size-4" />
                Edit
              </Button>
              <Button
                variant="destructive"
                className="gap-1"
                onClick={handleCancel}
                disabled={actionLoading}
              >
                <Trash2 className="size-4" />
                Cancel
              </Button>
            </>
          )}
          {isActive && (
            <>
              <Button
                className="gap-1"
                onClick={() => window.open(`/live-classes/${id}`, "_blank")}
              >
                <ExternalLink className="size-4" />
                Open Classroom
              </Button>
              <Button
                variant="destructive"
                className="gap-1"
                onClick={handleEndLive}
                disabled={actionLoading}
              >
                {actionLoading ? <Loader2 className="size-4 animate-spin" /> : <Square className="size-4" />}
                End Class
              </Button>
            </>
          )}
          {isEnded && (
            <p className="text-sm text-muted-foreground">This class has {liveClass.status === "CANCELLED" ? "been cancelled" : "ended"}.</p>
          )}
        </div>
      </div>

      {/* Timestamps */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-sm font-semibold text-foreground mb-2">Metadata</h2>
        <div className="space-y-1 text-xs text-muted-foreground">
          <p>Session ID: {liveClass.id}</p>
          <p>Created: {formatDate(liveClass.createdAt)}</p>
          {liveClass.teacherName && <p>Teacher: {liveClass.teacherName}</p>}
        </div>
      </div>
    </div>
  )
}
