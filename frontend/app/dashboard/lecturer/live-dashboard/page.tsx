"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Radio,
  Plus,
  Clock,
  Calendar,
  Video,
  Users,
  Play,
  Square,
  Settings,
  BarChart3,
  AlertCircle,
  ChevronRight,
  Filter,
  Trash2,
  Edit,
  Eye,
} from "lucide-react"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("elmkusoma_access_token")
}

type LiveClassItem = {
  id: string
  title: string
  description?: string
  scheduledAt: string
  durationMinutes: number
  status: string
  maxParticipants?: number
  currentParticipants?: number
  subjectName?: string
  teacherName: string
  teacherId: string
  subjectId?: string
  classGroupId?: string
  recordingUrl?: string
  canJoin?: boolean
  recordingEnabled?: boolean
  sessionType: string
  timezone?: string
}

type ScheduleForm = {
  title: string
  description: string
  sessionType: string
  scheduledAt: string
  durationMinutes: number
  maxParticipants: number
  recordingEnabled: boolean
  subjectId: string
  classGroupId: string
  timezone: string
}

const initialForm: ScheduleForm = {
  title: "",
  description: "",
  sessionType: "LECTURE",
  scheduledAt: "",
  durationMinutes: 60,
  maxParticipants: 50,
  recordingEnabled: false,
  subjectId: "",
  classGroupId: "",
  timezone: "Africa/Dar_es_Salaam",
}

const statusStyles: Record<string, { bg: string; text: string; border: string; dot: string }> = {
  IN_PROGRESS: {
    bg: "bg-red-50 dark:bg-red-950/20",
    text: "text-red-700 dark:text-red-400",
    border: "border-red-200 dark:border-red-800/40",
    dot: "bg-red-500",
  },
  SCHEDULED: {
    bg: "bg-blue-50 dark:bg-blue-950/20",
    text: "text-blue-700 dark:text-blue-400",
    border: "border-blue-200 dark:border-blue-800/40",
    dot: "bg-blue-500",
  },
  COMPLETED: {
    bg: "bg-green-50 dark:bg-green-950/20",
    text: "text-green-700 dark:text-green-400",
    border: "border-green-200 dark:border-green-800/40",
    dot: "bg-green-500",
  },
  CANCELLED: {
    bg: "bg-gray-50 dark:bg-gray-800/20",
    text: "text-gray-600 dark:text-gray-400",
    border: "border-gray-200 dark:border-gray-700/40",
    dot: "bg-gray-400",
  },
}

async function fetchTeacherLiveClasses(): Promise<LiveClassItem[]> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/live-classes`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error("Failed to fetch live classes")
  return res.json()
}

async function createLiveClass(data: ScheduleForm): Promise<LiveClassItem> {
  const token = getToken()
  const payload: Record<string, unknown> = {
    title: data.title.trim(),
    description: data.description.trim(),
    sessionType: data.sessionType,
    scheduledAt: new Date(data.scheduledAt).toISOString(),
    durationMinutes: data.durationMinutes,
    maxParticipants: data.maxParticipants,
    recordingEnabled: data.recordingEnabled,
    timezone: data.timezone,
  }
  if (data.subjectId) payload.subjectId = data.subjectId
  if (data.classGroupId) payload.classGroupId = data.classGroupId
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/live-classes`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  })
  if (!res.ok) throw new Error("Failed to create live class")
  return res.json()
}

async function deleteLiveClass(id: string): Promise<void> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/live-classes/${id}`, {
    method: "DELETE",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error("Failed to delete live class")
}

async function startLiveClass(id: string): Promise<void> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/live-classes/${id}/start`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error("Failed to start live class")
}

async function endLiveClass(id: string): Promise<void> {
  const token = getToken()
  const res = await fetch(`${API_BASE}/api/v1/teachers/me/live-classes/${id}/end`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error("Failed to end live class")
}

function formatDateTime(iso: string): string {
  if (!iso) return "—"
  return new Date(iso).toLocaleString(undefined, {
    weekday: "short",
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function getElapsedMinutes(scheduledAt: string): number {
  const diff = Date.now() - new Date(scheduledAt).getTime()
  return Math.max(0, Math.floor(diff / 60000))
}

function canStartSession(scheduledAt: string): boolean {
  const scheduled = new Date(scheduledAt).getTime()
  const now = Date.now()
  const diffMs = scheduled - now
  return diffMs <= 15 * 60 * 1000 && diffMs > -60 * 60 * 1000
}

function isToday(iso: string): boolean {
  const d = new Date(iso)
  const now = new Date()
  return (
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  )
}

function getStatusLabel(status: string, labels: Record<string, string>): string {
  return labels[status] || status
}

function SessionCard({
  session,
  onStart,
  onEnd,
  onDelete,
  onSelect,
}: {
  session: LiveClassItem
  onStart: (id: string) => void
  onEnd: (id: string) => void
  onDelete: (id: string) => void
  onSelect: (session: LiveClassItem) => void
}) {
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const style = statusStyles[session.status] || statusStyles.SCHEDULED
  const elapsed = session.status === "IN_PROGRESS" ? getElapsedMinutes(session.scheduledAt) : 0
  const statusLabels: Record<string, string> = {
    IN_PROGRESS: t("lecturerLive.statusLive"),
    SCHEDULED: t("lecturerLive.statusUpcoming"),
    COMPLETED: t("lecturerLive.statusCompleted"),
    CANCELLED: t("lecturerLive.statusCancelled"),
    ENDED: t("lecturerLive.statusCompleted"),
  }
  const sessionTypeLabels: Record<string, string> = {
    LECTURE: t("lecturerLive.sessionLecture"),
    TUTORIAL: t("lecturerLive.sessionTutorial"),
    WORKSHOP: t("lecturerLive.sessionWorkshop"),
    SEMINAR: t("lecturerLive.sessionSeminar"),
    LAB_DEMO: t("lecturerLive.sessionLabDemo"),
    WEBINAR: t("lecturerLive.sessionWebinar"),
    GUEST_SPEAKER: t("lecturerLive.sessionGuestSpeaker"),
    RESEARCH_PRESENTATION: t("lecturerLive.sessionResearchPresentation"),
    PROJECT_DEFENSE: t("lecturerLive.sessionProjectDefense"),
    CONFERENCE: t("lecturerLive.sessionConference"),
  }

  return (
    <div
      className={`rounded-2xl border bg-card p-5 shadow-xs transition-all hover:shadow-sm cursor-pointer ${style.border}`}
      onClick={() => onSelect(session)}
    >
      <div className="flex items-start gap-4">
        <div
          className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${style.bg}`}
        >
          {session.status === "IN_PROGRESS" ? (
            <Radio className={`size-5 ${style.text} animate-pulse`} />
          ) : session.status === "COMPLETED" ? (
            <Video className={`size-5 ${style.text}`} />
          ) : (
            <Calendar className={`size-5 ${style.text}`} />
          )}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="text-sm font-semibold text-foreground truncate">{session.title}</h3>
            <span
              className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${style.bg} ${style.text}`}
            >
              <span className={`size-1.5 rounded-full ${style.dot}`} />
              {getStatusLabel(session.status, statusLabels)}
            </span>
            {session.sessionType && session.sessionType !== "LECTURE" && (
              <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                {sessionTypeLabels[session.sessionType] ?? session.sessionType.replace(/_/g, " ")}
              </span>
            )}
          </div>

          {session.subjectName && (
            <p className="mt-1 text-xs text-muted-foreground">{session.subjectName}</p>
          )}

          {session.description && (
            <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{session.description}</p>
          )}

          <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
            <span className="flex items-center gap-1">
              <Calendar className="size-3" />
              {formatDateTime(session.scheduledAt)}
            </span>
            <span className="flex items-center gap-1">
              <Clock className="size-3" />
              {t("lecturerLive.durationMin", { count: session.durationMinutes })}
            </span>
            <span className="flex items-center gap-1">
              <Users className="size-3" />
              {session.currentParticipants ?? 0}/{session.maxParticipants ?? "∞"}
            </span>
            {elapsed > 0 && (
              <span className="flex items-center gap-1 font-medium text-red-600 dark:text-red-400">
                <Radio className="size-3" />
                {t("lecturerLive.elapsedMin", { count: elapsed })}
              </span>
            )}
          </div>
        </div>

        <div
          className="flex shrink-0 items-center gap-1"
          onClick={(e) => e.stopPropagation()}
        >
          {session.status === "SCHEDULED" && canStartSession(session.scheduledAt) && (
            <button
              onClick={() => onStart(session.id)}
              className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 transition-colors"
            >
              <Play className="size-3" />
              {t("lecturerLive.start")}
            </button>
          )}
          {session.status === "IN_PROGRESS" && (
            <>
              <button
                onClick={() => window.open(`/live-classes/${session.id}`, "_blank")}
                className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
              >
                <Eye className="size-3" />
                {tc("view")}
              </button>
              <button
                onClick={() => onEnd(session.id)}
                className="inline-flex items-center gap-1 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 transition-colors"
              >
                <Square className="size-3" />
                {t("lecturerLive.end")}
              </button>
            </>
          )}
          {session.status === "COMPLETED" && session.recordingUrl && (
            <button
              onClick={() => window.open(session.recordingUrl, "_blank")}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
            >
              <Video className="size-3" />
              {t("lecturerLive.recording")}
            </button>
          )}
          {(session.status === "SCHEDULED" || session.status === "COMPLETED") && (
            <button
              onClick={() => onDelete(session.id)}
              className="inline-flex items-center justify-center size-7 rounded-lg text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
              title={tc("delete")}
            >
              <Trash2 className="size-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

function SessionDetailModal({
  session,
  onClose,
  onStart,
  onEnd,
  onDelete,
}: {
  session: LiveClassItem
  onClose: () => void
  onStart: (id: string) => void
  onEnd: (id: string) => void
  onDelete: (id: string) => void
}) {
  const t = useTranslations("learner")
  const style = statusStyles[session.status] || statusStyles.SCHEDULED
  const elapsed =
    session.status === "IN_PROGRESS" ? getElapsedMinutes(session.scheduledAt) : 0
  const statusLabels: Record<string, string> = {
    IN_PROGRESS: t("lecturerLive.statusLive"),
    SCHEDULED: t("lecturerLive.statusUpcoming"),
    COMPLETED: t("lecturerLive.statusCompleted"),
    CANCELLED: t("lecturerLive.statusCancelled"),
    ENDED: t("lecturerLive.statusCompleted"),
  }
  const sessionTypeLabels: Record<string, string> = {
    LECTURE: t("lecturerLive.sessionLecture"),
    TUTORIAL: t("lecturerLive.sessionTutorial"),
    WORKSHOP: t("lecturerLive.sessionWorkshop"),
    SEMINAR: t("lecturerLive.sessionSeminar"),
    LAB_DEMO: t("lecturerLive.sessionLabDemo"),
    WEBINAR: t("lecturerLive.sessionWebinar"),
    GUEST_SPEAKER: t("lecturerLive.sessionGuestSpeaker"),
    RESEARCH_PRESENTATION: t("lecturerLive.sessionResearchPresentation"),
    PROJECT_DEFENSE: t("lecturerLive.sessionProjectDefense"),
    CONFERENCE: t("lecturerLive.sessionConference"),
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative z-10 w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-xl max-h-[85vh] overflow-y-auto">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-bold text-foreground">{session.title}</h2>
            <span
              className={`mt-1 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${style.bg} ${style.text}`}
            >
              <span className={`size-1.5 rounded-full ${style.dot}`} />
              {getStatusLabel(session.status, statusLabels)}
            </span>
          </div>
          <button
            onClick={onClose}
            className="flex size-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted transition-colors"
          >
            ✕
          </button>
        </div>

        <div className="mt-5 space-y-4">
          <div className="grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldSessionType")}</span>
              <p className="mt-0.5 text-foreground">
                {session.sessionType ? (sessionTypeLabels[session.sessionType] ?? session.sessionType.replace(/_/g, " ")) : t("lecturerLive.sessionLecture")}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldSubject")}</span>
              <p className="mt-0.5 text-foreground">{session.subjectName || "—"}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldScheduled")}</span>
              <p className="mt-0.5 text-foreground">{formatDateTime(session.scheduledAt)}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldDuration")}</span>
              <p className="mt-0.5 text-foreground">{t("lecturerLive.durationMinutes", { count: session.durationMinutes })}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldParticipants")}</span>
              <p className="mt-0.5 text-foreground">
                {session.currentParticipants ?? 0} / {session.maxParticipants ?? "∞"}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldRecording")}</span>
              <p className="mt-0.5 text-foreground">
                {session.recordingEnabled ? t("lecturerLive.enabled") : t("lecturerLive.disabled")}
              </p>
            </div>
            {session.timezone && (
              <div>
                <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldTimezone")}</span>
                <p className="mt-0.5 text-foreground">{session.timezone}</p>
              </div>
            )}
            {elapsed > 0 && (
              <div>
                <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldElapsed")}</span>
                <p className="mt-0.5 font-semibold text-red-600 dark:text-red-400">
                  {t("lecturerLive.durationMinutes", { count: elapsed })}
                </p>
              </div>
            )}
          </div>

          {session.description && (
            <div>
              <span className="text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldDescription")}</span>
              <p className="mt-1 text-sm text-foreground whitespace-pre-line">
                {session.description}
              </p>
            </div>
          )}

          {session.status === "COMPLETED" && (
            <div className="rounded-xl border border-green-200 bg-green-50 p-4 dark:border-green-800/40 dark:bg-green-950/20">
              <h4 className="text-sm font-semibold text-green-700 dark:text-green-400">
                {t("lecturerLive.analyticsSummary")}
              </h4>
              <div className="mt-2 grid grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-muted-foreground">{t("lecturerLive.totalParticipants")}</span>
                  <p className="font-semibold text-foreground">
                    {session.currentParticipants ?? 0}
                  </p>
                </div>
                <div>
                  <span className="text-muted-foreground">{t("lecturerLive.fieldDuration")}</span>
                  <p className="font-semibold text-foreground">{t("lecturerLive.durationMin", { count: session.durationMinutes })}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">{t("lecturerLive.completion")}</span>
                  <p className="font-semibold text-foreground">100%</p>
                </div>
                <div>
                  <span className="text-muted-foreground">{t("lecturerLive.fieldRecording")}</span>
                  <p className="font-semibold text-foreground">
                    {session.recordingUrl ? t("lecturerLive.available") : "N/A"}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="mt-6 flex flex-wrap items-center gap-2">
          {session.status === "SCHEDULED" && canStartSession(session.scheduledAt) && (
            <button
              onClick={() => {
                onStart(session.id)
                onClose()
              }}
              className="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 transition-colors"
            >
              <Play className="size-4" />
              {t("lecturerLive.startSession")}
            </button>
          )}
          {session.status === "IN_PROGRESS" && (
            <>
              <button
                onClick={() => window.open(`/live-classes/${session.id}`, "_blank")}
                className="inline-flex items-center gap-1.5 rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
              >
                <Eye className="size-4" />
                {t("lecturerLive.viewClassroom")}
              </button>
              <button
                onClick={() => {
                  onEnd(session.id)
                  onClose()
                }}
                className="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition-colors"
              >
                <Square className="size-4" />
                {t("lecturerLive.endSession")}
              </button>
            </>
          )}
          {session.status === "COMPLETED" && session.recordingUrl && (
            <button
              onClick={() => window.open(session.recordingUrl, "_blank")}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
            >
              <Video className="size-4" />
              {t("lecturerLive.viewRecording")}
            </button>
          )}
          {session.status === "COMPLETED" && (
            <button
              onClick={() => {
                onClose()
              }}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
            >
              <BarChart3 className="size-4" />
              {t("lecturerLive.viewAnalytics")}
            </button>
          )}
          {(session.status === "SCHEDULED" || session.status === "COMPLETED") && (
            <button
              onClick={() => {
                onDelete(session.id)
                onClose()
              }}
              className="inline-flex items-center gap-1.5 rounded-lg border border-destructive/30 px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 transition-colors ml-auto"
            >
              <Trash2 className="size-4" />
              {t("lecturerLive.deleteSession")}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

function ScheduleSessionForm({
  form,
  setForm,
  onSubmit,
  onCancel,
  submitting,
}: {
  form: ScheduleForm
  setForm: (f: ScheduleForm) => void
  onSubmit: () => void
  onCancel: () => void
  submitting: boolean
}) {
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const sessionTypes = [
    { value: "LECTURE", label: t("lecturerLive.sessionLecture") },
    { value: "TUTORIAL", label: t("lecturerLive.sessionTutorial") },
    { value: "WORKSHOP", label: t("lecturerLive.sessionWorkshop") },
    { value: "SEMINAR", label: t("lecturerLive.sessionSeminar") },
    { value: "LAB_DEMO", label: t("lecturerLive.sessionLabDemo") },
    { value: "WEBINAR", label: t("lecturerLive.sessionWebinar") },
    { value: "GUEST_SPEAKER", label: t("lecturerLive.sessionGuestSpeaker") },
    { value: "RESEARCH_PRESENTATION", label: t("lecturerLive.sessionResearchPresentation") },
    { value: "PROJECT_DEFENSE", label: t("lecturerLive.sessionProjectDefense") },
    { value: "CONFERENCE", label: t("lecturerLive.sessionConference") },
  ]
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-base font-semibold text-foreground">{t("lecturerLive.scheduleFormTitle")}</h2>
        <button
          onClick={onCancel}
          className="flex size-7 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted transition-colors"
        >
          ✕
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldTitle")}</label>
          <input
            type="text"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            placeholder={t("lecturerLive.titlePlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          />
        </div>

        <div className="sm:col-span-2">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldDescription")}</label>
          <textarea
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder={t("lecturerLive.descriptionPlaceholder")}
            rows={3}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldSessionType")}</label>
          <select
            value={form.sessionType}
            onChange={(e) => setForm({ ...form, sessionType: e.target.value })}
            className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            {sessionTypes.map((st) => (
              <option key={st.value} value={st.value}>
                {st.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldDateTime")}</label>
          <input
            type="datetime-local"
            value={form.scheduledAt}
            onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
            className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldDurationMinutes")}</label>
          <input
            type="number"
            value={form.durationMinutes}
            onChange={(e) => setForm({ ...form, durationMinutes: Number(e.target.value) })}
            min={1}
            max={480}
            className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldMaxParticipants")}</label>
          <input
            type="number"
            value={form.maxParticipants}
            onChange={(e) => setForm({ ...form, maxParticipants: Number(e.target.value) })}
            min={1}
            className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("lecturerLive.fieldTimezone")}</label>
          <select
            value={form.timezone}
            onChange={(e) => setForm({ ...form, timezone: e.target.value })}
            className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            <option value="Africa/Dar_es_Salaam">Africa/Dar es Salaam (UTC+03:00)</option>
            <option value="Africa/Nairobi">Africa/Nairobi (UTC+03:00)</option>
            <option value="Africa/Kampala">Africa/Kampala (UTC+03:00)</option>
            <option value="Africa/Lagos">Africa/Lagos (UTC+01:00)</option>
            <option value="Europe/London">Europe/London (UTC+00:00)</option>
            <option value="America/New_York">America/New York (UTC-05:00)</option>
          </select>
        </div>

        <div className="flex items-center gap-4 rounded-lg border border-border bg-background p-3">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={form.recordingEnabled}
              onChange={(e) => setForm({ ...form, recordingEnabled: e.target.checked })}
              className="size-4 rounded border-border"
            />
            <span className="text-sm text-foreground">{t("lecturerLive.enableRecording")}</span>
          </label>
          <span className="text-xs text-muted-foreground">{t("lecturerLive.recordingHint")}</span>
        </div>
      </div>

      <div className="flex justify-end gap-2">
        <button
          onClick={onCancel}
          className="inline-flex items-center rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors"
        >
          {tc("cancel")}
        </button>
        <button
          onClick={onSubmit}
          disabled={submitting || !form.title.trim() || !form.scheduledAt}
          className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
        >
          {submitting ? t("lecturerLive.scheduling") : t("lecturerLive.scheduleFormTitle")}
        </button>
      </div>
    </div>
  )
}

export default function LecturerLiveDashboardPage() {
  const { user } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [sessions, setSessions] = useState<LiveClassItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<ScheduleForm>(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [selectedSession, setSelectedSession] = useState<LiveClassItem | null>(null)
  const [filterStatus, setFilterStatus] = useState<string>("ALL")
  const [refreshTick, setRefreshTick] = useState(0)

  const loadSessions = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await fetchTeacherLiveClasses()
      setSessions(data)
    } catch {
      setError(t("lecturerLive.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (user) loadSessions()
  }, [user, loadSessions, refreshTick])

  useEffect(() => {
    const interval = setInterval(() => {
      setRefreshTick((t) => t + 1)
    }, 30000)
    return () => clearInterval(interval)
  }, [])

  const handleCreateSession = async () => {
    if (!form.title.trim() || !form.scheduledAt) return
    try {
      setSubmitting(true)
      setError(null)
      await createLiveClass(form)
      setSuccess(t("lecturerLive.scheduledSuccess"))
      setForm(initialForm)
      setShowForm(false)
      loadSessions()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lecturerLive.scheduleFailed"))
    } finally {
      setSubmitting(false)
    }
  }

  const handleStartSession = async (id: string) => {
    try {
      setError(null)
      await startLiveClass(id)
      setSuccess(t("lecturerLive.startedSuccess"))
      loadSessions()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lecturerLive.startFailed"))
    }
  }

  const handleEndSession = async (id: string) => {
    if (!confirm(t("lecturerLive.confirmEnd"))) return
    try {
      setError(null)
      await endLiveClass(id)
      setSuccess(t("lecturerLive.endedSuccess"))
      loadSessions()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lecturerLive.endFailed"))
    }
  }

  const handleDeleteSession = async (id: string) => {
    if (!confirm(t("lecturerLive.confirmDelete"))) return
    try {
      setError(null)
      await deleteLiveClass(id)
      setSuccess(t("lecturerLive.deletedSuccess"))
      loadSessions()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("lecturerLive.deleteFailed"))
    }
  }

  const liveNow = sessions.filter((s) => s.status === "IN_PROGRESS")
  const upcoming = sessions.filter((s) => s.status === "SCHEDULED")
  const completed = sessions.filter(
    (s) => (s.status === "COMPLETED" || s.status === "ENDED") && isToday(s.scheduledAt)
  )
  const allCompleted = sessions.filter((s) => s.status === "COMPLETED" || s.status === "ENDED")

  const filteredSessions = sessions.filter((s) => {
    if (filterStatus === "ALL") return true
    if (filterStatus === "LIVE") return s.status === "IN_PROGRESS"
    if (filterStatus === "UPCOMING") return s.status === "SCHEDULED"
    if (filterStatus === "COMPLETED") return s.status === "COMPLETED" || s.status === "ENDED"
    return true
  })

  const summaryCards = [
    {
      label: t("lecturerLive.summaryTotal"),
      value: sessions.length,
      icon: Calendar,
      color: "text-blue-600 dark:text-blue-400",
      bg: "bg-blue-50 dark:bg-blue-950/20",
    },
    {
      label: t("lecturerLive.summaryUpcoming"),
      value: upcoming.length,
      icon: Clock,
      color: "text-blue-600 dark:text-blue-400",
      bg: "bg-blue-50 dark:bg-blue-950/20",
    },
    {
      label: t("lecturerLive.summaryLiveNow"),
      value: liveNow.length,
      icon: Radio,
      color: "text-red-600 dark:text-red-400",
      bg: "bg-red-50 dark:bg-red-950/20",
    },
    {
      label: t("lecturerLive.summaryCompleted"),
      value: allCompleted.length,
      icon: Video,
      color: "text-green-600 dark:text-green-400",
      bg: "bg-green-50 dark:bg-green-950/20",
    },
  ]

  if (loading) return <LoadingState />

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader
        firstName={user?.firstName || user?.name || t("lecturerCourses.lecturerFallback")}
        subtitle={t("lecturerLive.headerSubtitle")}
      />

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
            <Video className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {summaryCards.map((card) => (
          <div
            key={card.label}
            className="rounded-2xl border border-border bg-card p-5 shadow-xs"
          >
            <div className="flex items-center gap-3">
              <div className={`flex size-9 items-center justify-center rounded-xl ${card.bg}`}>
                <card.icon className={`size-5 ${card.color}`} />
              </div>
              <div>
                <p className="text-2xl font-extrabold text-foreground">{card.value}</p>
                <p className="text-xs font-medium text-muted-foreground">{card.label}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Filter className="size-4 text-muted-foreground" />
          <div className="flex items-center gap-1">
            {[
              { key: "ALL", label: t("lecturerLive.filterAll") },
              { key: "LIVE", label: t("lecturerLive.filterLive") },
              { key: "UPCOMING", label: t("lecturerLive.filterUpcoming") },
              { key: "COMPLETED", label: t("lecturerLive.filterCompleted") },
            ].map((f) => (
              <button
                key={f.key}
                onClick={() => setFilterStatus(f.key)}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                  filterStatus === f.key
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted"
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        <button
          onClick={() => {
            setForm(initialForm)
            setShowForm(!showForm)
          }}
          className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          {showForm ? (
            <>
              <Settings className="size-4" />
              {tc("cancel")}
            </>
          ) : (
            <>
              <Plus className="size-4" />
              {t("lecturerLive.scheduleFormTitle")}
            </>
          )}
        </button>
      </div>

      {showForm && (
        <ScheduleSessionForm
          form={form}
          setForm={setForm}
          onSubmit={handleCreateSession}
          onCancel={() => {
            setForm(initialForm)
            setShowForm(false)
          }}
          submitting={submitting}
        />
      )}

      {liveNow.length > 0 && filterStatus === "ALL" && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
            <Radio className="size-4 text-red-500 animate-pulse" />
            {t("lecturerLive.sectionLiveNow")}
          </h2>
          <div className="space-y-3">
            {liveNow.map((session) => (
              <div key={session.id} className="ring-2 ring-red-200 dark:ring-red-800/40 rounded-2xl">
                <SessionCard
                  session={session}
                  onStart={handleStartSession}
                  onEnd={handleEndSession}
                  onDelete={handleDeleteSession}
                  onSelect={setSelectedSession}
                />
              </div>
            ))}
          </div>
        </section>
      )}

      {upcoming.length > 0 && filterStatus === "ALL" && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
            <Clock className="size-4 text-blue-500" />
            {t("lecturerLive.sectionUpcoming")}
          </h2>
          <div className="space-y-3">
            {upcoming
              .sort(
                (a, b) =>
                  new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime()
              )
              .map((session) => (
                <SessionCard
                  key={session.id}
                  session={session}
                  onStart={handleStartSession}
                  onEnd={handleEndSession}
                  onDelete={handleDeleteSession}
                  onSelect={setSelectedSession}
                />
              ))}
          </div>
        </section>
      )}

      {completed.length > 0 && filterStatus === "ALL" && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
            <Video className="size-4 text-green-500" />
            {t("lecturerLive.sectionCompletedToday")}
          </h2>
          <div className="space-y-3">
            {completed.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                onStart={handleStartSession}
                onEnd={handleEndSession}
                onDelete={handleDeleteSession}
                onSelect={setSelectedSession}
              />
            ))}
          </div>
        </section>
      )}

      {filterStatus !== "ALL" && filteredSessions.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
            <Filter className="size-4 text-muted-foreground" />
            {filterStatus === "LIVE"
              ? t("lecturerLive.sectionLive")
              : filterStatus === "UPCOMING"
                ? t("lecturerLive.sectionUpcoming")
                : t("lecturerLive.sectionCompleted")}
          </h2>
          <div className="space-y-3">
            {filteredSessions.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                onStart={handleStartSession}
                onEnd={handleEndSession}
                onDelete={handleDeleteSession}
                onSelect={setSelectedSession}
              />
            ))}
          </div>
        </section>
      )}

      {sessions.length === 0 && (
        <EmptyState
          icon={<Radio className="size-10" />}
          title={t("lecturerLive.emptyTitle")}
          description={t("lecturerLive.emptyDescription")}
          action={
            <button
              onClick={() => setShowForm(true)}
              className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              <Plus className="size-4" />
              {t("lecturerLive.emptyAction")}
            </button>
          }
        />
      )}

      {selectedSession && (
        <SessionDetailModal
          session={selectedSession}
          onClose={() => setSelectedSession(null)}
          onStart={handleStartSession}
          onEnd={handleEndSession}
          onDelete={handleDeleteSession}
        />
      )}
    </div>
  )
}
