"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { ArrowLeft, Loader2, Calendar } from "lucide-react"
import Link from "next/link"
import { adminApi, getInstitutionId } from "@/lib/api"
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

const initialForm: EventFormData = {
  title: "",
  description: "",
  eventType: "",
  category: "",
  startDate: "",
  durationMinutes: "60",
  timezone: "Africa/Nairobi",
  maxCapacity: "",
  accessLevel: "PUBLIC",
  presenterName: "",
  relatedCourseId: "",
  relatedModuleId: "",
  relatedLessonId: "",
  recordingEnabled: false,
}

export default function NewEventPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const router = useRouter()

  const [form, setForm] = useState<EventFormData>(initialForm)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})

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

  async function handleSave(status: "DRAFT" | "PUBLISHED") {
    if (!validate()) return
    const institutionId = getInstitutionId()
    if (!institutionId) { setError("No institution context found"); return }

    setSaving(true)
    setError(null)
    try {
      await adminApi.createEvent({
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
        status,
        institutionId,
      })
      router.push("/dashboard/admin/events")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create event")
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6" role="main" aria-label={t("admin.form.createTitle")}>
      <div className="flex items-center gap-4">
        <Link
          href="/dashboard/admin/events"
          className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground"
          aria-label={tc("back")}
        >
          <ArrowLeft className="size-4" />
          {tc("back")}
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("admin.form.createTitle")}</h1>
        </div>
      </div>

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

        {/* Actions */}
        <div className="flex gap-3 border-t border-border pt-6">
          <button
            onClick={() => handleSave("DRAFT")}
            disabled={saving}
            className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl border border-border px-4 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted disabled:opacity-50"
            aria-label={t("admin.form.saveDraft")}
          >
            {saving && <Loader2 className="size-4 animate-spin" />}
            {t("admin.form.saveDraft")}
          </button>
          <button
            onClick={() => handleSave("PUBLISHED")}
            disabled={saving}
            className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50"
            aria-label={t("admin.form.publish")}
          >
            {saving && <Loader2 className="size-4 animate-spin" />}
            {t("admin.form.publish")}
          </button>
        </div>
      </div>
    </div>
  )
}
