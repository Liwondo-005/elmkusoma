"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
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

export default function LiveClassDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tc = useTranslations("common")
  const ts = useTranslations("status")
  const id = params.id as string
  const [liveClass, setLiveClass] = useState<LiveClass | null>(null)
  const [participants, setParticipants] = useState<Participant[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState(false)

  const statusConfig: Record<string, { label: string; className: string; description: string }> = {
    SCHEDULED: { label: ts("scheduled"), className: "bg-blue-100 text-blue-700", description: t("liveClassDetail.descScheduled") },
    STARTING: { label: t("liveClassDetail.statusStarting"), className: "bg-amber-100 text-amber-700", description: t("liveClassDetail.descStarting") },
    IN_PROGRESS: { label: t("liveClassDetail.statusLive"), className: "bg-green-100 text-green-700", description: t("liveClassDetail.descLive") },
    LIVE: { label: t("liveClassDetail.statusLive"), className: "bg-green-100 text-green-700", description: t("liveClassDetail.descLive") },
    ENDING: { label: t("liveClassDetail.statusEnding"), className: "bg-amber-100 text-amber-700", description: t("liveClassDetail.descEnding") },
    COMPLETED: { label: ts("completed"), className: "bg-gray-100 text-gray-700", description: t("liveClassDetail.descEnded") },
    ENDED: { label: t("liveClassDetail.statusEnded"), className: "bg-gray-100 text-gray-700", description: t("liveClassDetail.descEnded") },
    CANCELLED: { label: ts("cancelled"), className: "bg-red-100 text-red-700", description: t("liveClassDetail.descCancelled") },
  }

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
      setError(err instanceof Error ? err.message : t("liveClassDetail.loadError"))
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
      setSuccess(t("liveClassDetail.startedSuccess"))
      loadClass()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("liveClassDetail.startError"))
    } finally {
      setActionLoading(false)
    }
  }

  async function handleEndLive() {
    if (!confirm(t("liveClassDetail.endConfirm"))) return
    try {
      setActionLoading(true)
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}/end`, { method: "POST" })
      setSuccess(t("liveClassDetail.endedSuccess"))
      loadClass()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("liveClassDetail.endError"))
    } finally {
      setActionLoading(false)
    }
  }

  async function handleCancel() {
    if (!confirm(t("liveClassDetail.cancelConfirm"))) return
    try {
      setActionLoading(true)
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}`, { method: "DELETE" })
      setSuccess(t("liveClassDetail.cancelledSuccess"))
      loadClass()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("liveClassDetail.cancelError"))
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
          {t("liveClasses.backToList")}
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
        {t("liveClasses.backToList")}
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
          <h2 className="text-sm font-semibold text-foreground">{t("liveClassDetail.scheduleTitle")}</h2>
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
              <span className="text-muted-foreground">{t("liveClassDetail.minutesCount", { count: liveClass.durationMinutes })}</span>
            </div>
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <span>Timezone: Africa/Dar_es_Salaam (UTC+03:00)</span>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
          <h2 className="text-sm font-semibold text-foreground">{t("liveClassDetail.detailsTitle")}</h2>
          <div className="space-y-2">
            {liveClass.subjectName && (
              <div className="flex items-center gap-2 text-sm">
                <BookOpen className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">{liveClass.subjectName}</span>
              </div>
            )}
            <div className="flex items-center gap-2 text-sm">
              <Users className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-foreground">{t("liveClassDetail.maxParticipants", { count: liveClass.maxParticipants })}</span>
            </div>
            {liveClass.currentParticipants != null && (
              <div className="flex items-center gap-2 text-sm">
                <User className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">{t("liveClassDetail.joinedCount", { count: liveClass.currentParticipants })}</span>
              </div>
            )}
            <div className="flex items-center gap-2 text-sm">
              <Settings className="size-4 shrink-0 text-muted-foreground" />
              <span className="text-muted-foreground">
                {t("liveClassDetail.recordingLabel", { value: liveClass.recordingEnabled ? (liveClass.recordingUrl ? t("liveClassDetail.recAvailable") : t("liveClassDetail.recEnabled")) : t("liveClassDetail.recDisabled") })}
              </span>
            </div>
          </div>
        </div>
      </div>

      {liveClass.description && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-sm font-semibold text-foreground mb-2">{t("liveClassDetail.descTitle")}</h2>
          <p className="text-sm text-muted-foreground whitespace-pre-line">{liveClass.description}</p>
        </div>
      )}

      {/* Participants */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-foreground">{t("liveClassDetail.participantsTitle")}</h2>
          <Button variant="outline" size="sm" onClick={loadParticipants}>
            {t("liveClassDetail.loadParticipants")}
          </Button>
        </div>
        {participants.length === 0 ? (
          <p className="text-xs text-muted-foreground">{t("liveClassDetail.noParticipants", { action: t("liveClassDetail.loadParticipants") })}</p>
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
                  {p.joinedAt && <p>{t("liveClassDetail.joinedLabel", { date: formatShortDate(p.joinedAt) })}</p>}
                  {p.durationSeconds && <p>{t("classDetail.minutesCount", { count: Math.round(p.durationSeconds / 60) })}</p>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-sm font-semibold text-foreground mb-4">{t("liveClassDetail.actionsTitle")}</h2>
        <div className="flex flex-wrap gap-2">
          {isScheduled && (
            <>
              <Button
                className="gap-1 bg-green-600 hover:bg-green-700 text-white"
                onClick={handleStartLive}
                disabled={actionLoading}
              >
                {actionLoading ? <Loader2 className="size-4 animate-spin" /> : <Play className="size-4" />}
                {t("liveClassDetail.startLive")}
              </Button>
              <Button
                variant="outline"
                className="gap-1"
                onClick={() => window.open(`/dashboard/teacher/live-classes/${id}/prepare`, "_blank")}
              >
                <Settings className="size-4" />
                {t("liveClassDetail.prepare")}
              </Button>
              <Button
                variant="outline"
                className="gap-1"
                onClick={() => router.push(`/dashboard/teacher/live-classes?edit=${id}`)}
              >
                <Edit className="size-4" />
                {tc("edit")}
              </Button>
              <Button
                variant="destructive"
                className="gap-1"
                onClick={handleCancel}
                disabled={actionLoading}
              >
                <Trash2 className="size-4" />
                {tc("cancel")}
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
                {t("liveClassDetail.openClassroom")}
              </Button>
              <Button
                variant="destructive"
                className="gap-1"
                onClick={handleEndLive}
                disabled={actionLoading}
              >
                {actionLoading ? <Loader2 className="size-4 animate-spin" /> : <Square className="size-4" />}
                {t("liveClassDetail.endClass")}
              </Button>
            </>
          )}
          {isEnded && (
            <p className="text-sm text-muted-foreground">{liveClass.status === "CANCELLED" ? t("liveClassDetail.endedCancelled") : t("liveClassDetail.endedEnded")}</p>
          )}
        </div>
      </div>

      {/* Timestamps */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-sm font-semibold text-foreground mb-2">{t("liveClassDetail.metadataTitle")}</h2>
        <div className="space-y-1 text-xs text-muted-foreground">
          <p>{t("liveClassDetail.sessionId", { id: liveClass.id })}</p>
          <p>{t("liveClassDetail.createdLabel", { date: formatDate(liveClass.createdAt) })}</p>
          {liveClass.teacherName && <p>{t("liveClassDetail.teacherLabel", { name: liveClass.teacherName })}</p>}
        </div>
      </div>
    </div>
  )
}
