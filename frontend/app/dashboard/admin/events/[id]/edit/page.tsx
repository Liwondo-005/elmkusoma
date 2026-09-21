"use client"

import { useEffect, useState } from "react"
import { useRouter, useParams } from "next/navigation"
import { useTranslations } from "next-intl"
import { ArrowLeft, Loader2, Play, Square, Video, Calendar } from "lucide-react"
import Link from "next/link"
import { adminApi } from "@/lib/api"
import { cn } from "@/lib/utils"

const EVENT_TYPES = [
  "LECTURE", "SEMINAR", "WEBINAR", "WORKSHOP", "TUTORIAL",
  "PRACTICAL_DEMONSTRATION", "GUEST_SESSION", "ACADEMIC_TALK",
  "PROFESSIONAL_TRAINING", "EDUCATIONAL_BROADCAST", "CONFERENCE_SESSION", "OTHER"
]

const CATEGORIES = ["ACADEMIC", "PROFESSIONAL", "COMMUNITY", "OTHER"]

const ACCESS_LEVELS = [
  "PUBLIC", "AUTHENTICATED", "REGISTERED", "ENROLLED",
  "INSTITUTION", "INVITED", "RESTRICTED"
]

const TIMEZONES = [
  "Africa/Nairobi", "Africa/Dar_es_Salaam", "Africa/Kampala",
  "Africa/Kigali", "Africa/Maputo", "Africa/Johannesburg",
  "UTC", "Europe/London", "America/New_York", "America/Chicago",
  "America/Los_Angeles", "Asia/Dubai", "Asia/Kolkata", "Asia/Shanghai",
  "Asia/Tokyo", "Australia/Sydney"
]

interface EventFormData {
  title: string
  description: string
  eventType: string
  category: string
  startDate: string
  durationMinutes: string
  timezone: string
  maxCapacity: string
  accessLevel: string
  presenterName: string
  relatedCourseId: string
  relatedModuleId: string
  relatedLessonId: string
  recordingEnabled: boolean
}

interface EventDetail {
  id: string
  title: string
  description?: string
  eventType: string
  category?: string
  status: string
  startDate: string
  durationMinutes?: number
  timezone?: string
  maxCapacity?: number
  currentRegistrations?: number
  accessLevel?: string
  presenterName?: string
  recordingEnabled?: boolean
  recordingUrl?: string
  meetingUrl?: string
  institutionId?: string
  relatedCourseId?: string
  relatedModuleId?: string
  relatedLessonId?: string
  createdAt: string
  statusHistory?: Array<{ status: string; timestamp: string; changedBy?: string }>
}

const statusColors: Record<string, string> = {
  DRAFT: "bg-gray-100 text-gray-700 border border-gray-200",
  PUBLISHED: "bg-blue-100 text-blue-700 border border-blue-200",
  LIVE: "bg-green-100 text-green-700 border border-green-200 animate-pulse",
  ENDED: "bg-gray-100 text-gray-500 border border-gray-200",
  CANCELLED: "bg-red-100 text-red-700 border border-red-200",
}

export default function EditEventPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const router = useRouter()
  const params = useParams()
  const eventId = params.id as string

  const [event, setEvent] = useState<EventDetail | null>(null)
  const [form, setForm] = useState<EventFormData>({
    title: "", description: "", eventType: "", category: "",
    startDate: "", durationMinutes: "60", timezone: "Africa/Nairobi",
    maxCapacity: "", accessLevel: "PUBLIC", presenterName: "",
    relatedCourseId: "", relatedModuleId: "", relatedLessonId: "",
    recordingEnabled: false,
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  useEffect(() => {
    loadEvent()
  }, [eventId])

  async function loadEvent() {
    try {
      setLoading(true)
      const data = await adminApi.getEvent(eventId)
      setEvent(data)
      setForm({
        title: data.title || "",
        description: data.description || "",
        eventType: data.eventType || "",
        category: data.category || "",
        startDate: data.startDate ? new Date(data.startDate).toISOString().slice(0, 16) : "",
        durationMinutes: data.durationMinutes?.toString() || "60",
        timezone: data.timezone || "Africa/Nairobi",
        maxCapacity: data.maxCapacity?.toString() || "",
        accessLevel: data.accessLevel || "PUBLIC",
        presenterName: data.presenterName || "",
        relatedCourseId: data.relatedCourseId || "",
        relatedModuleId: data.relatedModuleId || "",
        relatedLessonId: data.relatedLessonId || "",
        recordingEnabled: data.recordingEnabled || false,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load event")
    } finally {
      setLoading(false)
    }
  }

  function updateField<K extends keyof EventFormData>(key: K, value: EventFormData[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
    if (errors[key]) setErrors((prev) => { const n = { ...prev }; delete n[key]; return n })
  }

  function validate(): boolean {
    const e: Record<string, string> = {}
    if (!form.title.trim()) e.title = t("admin.form.required")
    if (!form.startDate) e.startDate = t("admin.form.required")
    if (!form.eventType) e.eventType = t("admin.form.required")
    setErrors(e)
    return Object.keys(e).length === 0
  }

  async function handleSave() {
    if (!validate()) return
    setSaving(true)
    setError(null)
    try {
      await adminApi.updateEvent(eventId, {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        eventType: form.eventType,
        category: form.category || undefined,
        startDate: form.startDate,
        durationMinutes: form.durationMinutes ? parseInt(form.durationMinutes) : undefined,
        timezone: form.timezone,
        maxCapacity: form.maxCapacity ? parseInt(form.maxCapacity) : undefined,
        accessLevel: form.accessLevel,
        presenterName: form.presenterName.trim() || undefined,
        relatedCourseId: form.relatedCourseId || undefined,
        relatedModuleId: form.relatedModuleId || undefined,
        relatedLessonId: form.relatedLessonId || undefined,
        recordingEnabled: form.recordingEnabled,
      })
      router.push("/dashboard/admin/events")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update event")
    } finally {
      setSaving(false)
    }
  }

  async function handleAction(action: string) {
    setActionLoading(action)
    try {
      switch (action) {
        case "startLive": await adminApi.startLiveEvent(eventId); break
        case "endLive": await adminApi.endLiveEvent(eventId); break
        case "cancel":
          if (!window.confirm(t("confirm.cancelMessage"))) return
          await adminApi.cancelEvent(eventId); break
      }
      loadEvent()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Action failed")
    } finally {
      setActionLoading(null)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error && !event) {
    return (
      <div className="mx-auto max-w-3xl space-y-6" role="main" aria-label={t("admin.form.editTitle")}>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
        <Link href="/dashboard/admin/events" className="text-sm font-medium text-primary hover:underline">{tc("back")}</Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6" role="main" aria-label={t("admin.form.editTitle")}>
      <div className="flex items-center gap-4">
        <Link
          href="/dashboard/admin/events"
          className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground"
          aria-label={tc("back")}
        >
          <ArrowLeft className="size-4" />
          {tc("back")}
        </Link>
        <div className="flex-1">
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("admin.form.editTitle")}</h1>
        </div>
        {/* Status badge */}
        {event && (
          <span className={cn("inline-flex items-center rounded-full px-3 py-1 text-xs font-medium", statusColors[event.status] || "")}>
            {event.status === "LIVE" && <span className="mr-1 size-1.5 rounded-full bg-green-600 animate-pulse" />}
            {t(`admin.status.${event.status}` as any)}
          </span>
        )}
      </div>

      {/* Live Session Actions */}
      {event && (event.status === "PUBLISHED" || event.status === "LIVE") && (
        <div className="flex gap-3">
          {event.status === "PUBLISHED" && (
            <button
              onClick={() => handleAction("startLive")}
              disabled={!!actionLoading}
              className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
              aria-label={t("admin.actions.startLive")}
            >
              {actionLoading === "startLive" ? <Loader2 className="size-4 animate-spin" /> : <Play className="size-4" />}
              {t("admin.actions.startLive")}
            </button>
          )}
          {event.status === "LIVE" && (
            <button
              onClick={() => handleAction("endLive")}
              disabled={!!actionLoading}
              className="inline-flex items-center gap-2 rounded-xl bg-orange-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50"
              aria-label={t("admin.actions.endLive")}
            >
              {actionLoading === "endLive" ? <Loader2 className="size-4 animate-spin" /> : <Square className="size-4" />}
              {t("admin.actions.endLive")}
            </button>
          )}
          {event.recordingUrl && (
            <a
              href={event.recordingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
              aria-label={t("admin.actions.viewRecording")}
            >
              <Video className="size-4" />
              {t("admin.actions.viewRecording")}
            </a>
          )}
          {event.status !== "ENDED" && event.status !== "CANCELLED" && (
            <button
              onClick={() => handleAction("cancel")}
              disabled={!!actionLoading}
              className="inline-flex items-center gap-2 rounded-xl border border-red-200 px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
              aria-label={t("admin.actions.cancel")}
            >
              {t("admin.actions.cancel")}
            </button>
          )}
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-6">
        {/* Title */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1">
            {t("admin.form.titleLabel")} <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={form.title}
            onChange={(e) => updateField("title", e.target.value)}
            placeholder={t("admin.form.titlePlaceholder")}
            className={cn("w-full rounded-xl border bg-card px-4 py-2.5 text-sm outline-none focus:ring-1 focus:ring-primary", errors.title ? "border-red-500" : "border-border focus:border-primary")}
            aria-label={t("admin.form.titleLabel")}
            aria-invalid={!!errors.title}
          />
          {errors.title && <p className="mt-1 text-xs text-red-500">{errors.title}</p>}
        </div>

        {/* Description */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.descriptionLabel")}</label>
          <textarea
            value={form.description}
            onChange={(e) => updateField("description", e.target.value)}
            placeholder={t("admin.form.descriptionPlaceholder")}
            rows={4}
            className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary resize-none"
            aria-label={t("admin.form.descriptionLabel")}
          />
        </div>

        {/* Type and Category */}
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              {t("admin.form.typeLabel")} <span className="text-red-500">*</span>
            </label>
            <select
              value={form.eventType}
              onChange={(e) => updateField("eventType", e.target.value)}
              className={cn("w-full rounded-xl border bg-card px-4 py-2.5 text-sm outline-none focus:ring-1 focus:ring-primary", errors.eventType ? "border-red-500" : "border-border focus:border-primary")}
              aria-label={t("admin.form.typeLabel")}
              aria-invalid={!!errors.eventType}
            >
              <option value="">{t("admin.form.typePlaceholder")}</option>
              {EVENT_TYPES.map((type) => (
                <option key={type} value={type}>{t(`admin.types.${type}` as any)}</option>
              ))}
            </select>
            {errors.eventType && <p className="mt-1 text-xs text-red-500">{errors.eventType}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.categoryLabel")}</label>
            <select
              value={form.category}
              onChange={(e) => updateField("category", e.target.value)}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.categoryLabel")}
            >
              <option value="">{t("admin.form.categoryPlaceholder")}</option>
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{t(`admin.categories.${cat}` as any)}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Date/Time & Duration */}
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              {t("admin.form.dateLabel")} <span className="text-red-500">*</span>
            </label>
            <input
              type="datetime-local"
              value={form.startDate}
              onChange={(e) => updateField("startDate", e.target.value)}
              className={cn("w-full rounded-xl border bg-card px-4 py-2.5 text-sm outline-none focus:ring-1 focus:ring-primary", errors.startDate ? "border-red-500" : "border-border focus:border-primary")}
              aria-label={t("admin.form.dateLabel")}
              aria-invalid={!!errors.startDate}
            />
            {errors.startDate && <p className="mt-1 text-xs text-red-500">{errors.startDate}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.durationLabel")}</label>
            <input
              type="number"
              min="1"
              value={form.durationMinutes}
              onChange={(e) => updateField("durationMinutes", e.target.value)}
              placeholder={t("admin.form.durationPlaceholder")}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.durationLabel")}
            />
          </div>
        </div>

        {/* Timezone & Capacity */}
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.timezoneLabel")}</label>
            <select
              value={form.timezone}
              onChange={(e) => updateField("timezone", e.target.value)}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.timezoneLabel")}
            >
              {TIMEZONES.map((tz) => (
                <option key={tz} value={tz}>{tz}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.capacityLabel")}</label>
            <input
              type="number"
              min="1"
              value={form.maxCapacity}
              onChange={(e) => updateField("maxCapacity", e.target.value)}
              placeholder={t("admin.form.capacityPlaceholder")}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.capacityLabel")}
            />
          </div>
        </div>

        {/* Access Level & Presenter */}
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.accessLevelLabel")}</label>
            <select
              value={form.accessLevel}
              onChange={(e) => updateField("accessLevel", e.target.value)}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.accessLevelLabel")}
            >
              {ACCESS_LEVELS.map((level) => (
                <option key={level} value={level}>{t(`admin.accessLevels.${level}` as any)}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.presenterLabel")}</label>
            <input
              type="text"
              value={form.presenterName}
              onChange={(e) => updateField("presenterName", e.target.value)}
              placeholder={t("admin.form.presenterPlaceholder")}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.presenterLabel")}
            />
          </div>
        </div>

        {/* Related Course/Module/Lesson */}
        <div className="grid gap-4 sm:grid-cols-3">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.courseLabel")}</label>
            <input
              type="text"
              value={form.relatedCourseId}
              onChange={(e) => updateField("relatedCourseId", e.target.value)}
              placeholder={t("admin.form.coursePlaceholder")}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.courseLabel")}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.moduleLabel")}</label>
            <input
              type="text"
              value={form.relatedModuleId}
              onChange={(e) => updateField("relatedModuleId", e.target.value)}
              placeholder={t("admin.form.modulePlaceholder")}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.moduleLabel")}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">{t("admin.form.lessonLabel")}</label>
            <input
              type="text"
              value={form.relatedLessonId}
              onChange={(e) => updateField("relatedLessonId", e.target.value)}
              placeholder={t("admin.form.lessonPlaceholder")}
              className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              aria-label={t("admin.form.lessonLabel")}
            />
          </div>
        </div>

        {/* Recording Toggle */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => updateField("recordingEnabled", !form.recordingEnabled)}
            className={cn(
              "relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors",
              form.recordingEnabled ? "bg-primary" : "bg-muted"
            )}
            role="switch"
            aria-checked={form.recordingEnabled}
            aria-label={t("admin.form.recordingLabel")}
          >
            <span className={cn("pointer-events-none inline-block size-5 rounded-full bg-white shadow-xs transition-transform", form.recordingEnabled ? "translate-x-5" : "translate-x-0")} />
          </button>
          <label className="text-sm font-medium text-foreground">{t("admin.form.recordingLabel")}</label>
        </div>

        {/* Save */}
        <div className="flex gap-3 border-t border-border pt-6">
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50"
            aria-label={tc("save")}
          >
            {saving && <Loader2 className="size-4 animate-spin" />}
            {tc("save")}
          </button>
        </div>
      </div>

      {/* Status History */}
      {event?.statusHistory && event.statusHistory.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground mb-4 flex items-center gap-2">
            <Calendar className="size-4 text-muted-foreground" />
            Status History
          </h3>
          <div className="space-y-3">
            {event.statusHistory.map((entry, i) => (
              <div key={i} className="flex items-center gap-3">
                <span className={cn("inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium", statusColors[entry.status] || "bg-muted text-muted-foreground")}>
                  {entry.status}
                </span>
                <span className="text-sm text-muted-foreground">
                  {new Date(entry.timestamp).toLocaleString()}
                </span>
                {entry.changedBy && (
                  <span className="text-xs text-muted-foreground">by {entry.changedBy}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
