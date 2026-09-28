"use client"

import { useEffect, useState } from "react"
import { Loader2, X } from "lucide-react"
import { useTranslations } from "next-intl"
import { platformAdminApi, INSTITUTION_TYPES, type EventFormPayload } from "@/lib/platform-admin-api"

interface EventFormModalProps {
  open: boolean
  event?: { id: string; title: string; eventType?: string; description?: string; category?: string; location?: string; meetingUrl?: string; startsAt?: string; endsAt?: string; durationMinutes?: number; maxParticipants?: number; status?: string; thumbnailUrl?: string; tags?: string; isFree?: boolean; requiresApproval?: boolean; timezone?: string; accessLevel?: string; presenterName?: string; eventFormat?: string; difficulty?: string; targetAudience?: string; prerequisites?: string; learningOutcomes?: string; agenda?: string; relatedCourseId?: string; relatedModuleId?: string; relatedLessonId?: string; providerId?: string } | null
  onClose: () => void
  onSaved: (message: string) => void
}

const EMPTY_FORM: EventFormPayload = {
  title: "",
  eventType: "WEBINAR",
  startsAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
  description: "",
  category: "",
  location: "",
  meetingUrl: "",
  durationMinutes: 60,
  maxParticipants: undefined,
  status: "DRAFT",
  thumbnailUrl: "",
  tags: "",
  isFree: true,
  requiresApproval: false,
  timezone: "Africa/Dar_es_Salaam",
  accessLevel: "",
  presenterName: "",
  eventFormat: "",
  difficulty: "",
  targetAudience: "",
  prerequisites: "",
  learningOutcomes: "",
  agenda: "",
  relatedCourseId: undefined,
  relatedModuleId: undefined,
  relatedLessonId: undefined,
  providerId: undefined,
}

const EVENT_TYPES = [
  "WEBINAR",
  "WORKSHOP",
  "SEMINAR",
  "CONFERENCE",
  "MEETUP",
  "COURSE",
  "ORIENTATION",
  "CEREMONY",
  "EXAM",
  "HACKATHON",
  "COMPETITION",
  "NETWORKING",
  "TRAINING",
  "ORIENTATION",
  "GRADUATION",
  "OTHER",
] as const

const EVENT_CATEGORIES = [
  "EDUCATION",
  "TECHNOLOGY",
  "BUSINESS",
  "SCIENCE",
  "ARTS",
  "HEALTH",
  "SPORTS",
  "COMMUNITY",
  "CULTURE",
  "ENVIRONMENT",
  "GOVERNMENT",
  "NON_PROFIT",
  "OTHER",
] as const

const STATUSES = ["DRAFT", "SCHEDULED", "PUBLISHED", "LIVE", "COMPLETED", "CANCELLED", "ARCHIVED"] as const

export function EventFormModal({ open, event, onClose, onSaved }: EventFormModalProps) {
  const t = useTranslations("platformAdmin")
  const tc = useTranslations("common")
  const isEdit = Boolean(event)
  const [form, setForm] = useState<EventFormPayload>(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setError(null)
    if (event) {
      setForm({
        title: event.title ?? "",
        eventType: event.eventType ?? "WEBINAR",
        description: event.description ?? "",
        category: event.category ?? "",
        location: event.location ?? "",
        meetingUrl: event.meetingUrl ?? "",
        startsAt: event.startsAt ? event.startsAt.slice(0, 16) : new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
        endsAt: event.endsAt ? event.endsAt.slice(0, 16) : undefined,
        durationMinutes: event.durationMinutes ?? 60,
        maxParticipants: event.maxParticipants ?? undefined,
        status: event.status ?? "DRAFT",
        thumbnailUrl: event.thumbnailUrl ?? "",
        tags: event.tags ?? "",
        isFree: event.isFree ?? true,
        requiresApproval: event.requiresApproval ?? false,
        timezone: event.timezone ?? "Africa/Dar_es_Salaam",
        accessLevel: event.accessLevel ?? "",
        presenterName: event.presenterName ?? "",
        eventFormat: event.eventFormat ?? "",
        difficulty: event.difficulty ?? "",
        targetAudience: event.targetAudience ?? "",
        prerequisites: event.prerequisites ?? "",
        learningOutcomes: event.learningOutcomes ?? "",
        agenda: event.agenda ?? "",
        relatedCourseId: event.relatedCourseId ?? undefined,
        relatedModuleId: event.relatedModuleId ?? undefined,
        relatedLessonId: event.relatedLessonId ?? undefined,
        providerId: event.providerId ?? undefined,
      })
    } else {
      setForm(EMPTY_FORM)
    }
  }, [open, event])

  if (!open) return null

  const set = (key: keyof EventFormPayload) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const title = form.title.trim()
    if (!title) {
      setError(t("eventForm.errTitleRequired"))
      return
    }
    if (!form.startsAt) {
      setError(t("eventForm.errStartRequired"))
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload: EventFormPayload = {
        ...form,
        title,
        description: form.description?.trim() || undefined,
        category: form.category?.trim() || undefined,
        location: form.location?.trim() || undefined,
        meetingUrl: form.meetingUrl?.trim() || undefined,
        durationMinutes: form.durationMinutes || undefined,
        maxParticipants: form.maxParticipants || undefined,
        thumbnailUrl: form.thumbnailUrl?.trim() || undefined,
        tags: form.tags?.trim() || undefined,
        timezone: form.timezone?.trim() || "Africa/Dar_es_Salaam",
        accessLevel: form.accessLevel?.trim() || undefined,
        presenterName: form.presenterName?.trim() || undefined,
        eventFormat: form.eventFormat?.trim() || undefined,
        difficulty: form.difficulty?.trim() || undefined,
        targetAudience: form.targetAudience?.trim() || undefined,
        prerequisites: form.prerequisites?.trim() || undefined,
        learningOutcomes: form.learningOutcomes?.trim() || undefined,
        agenda: form.agenda?.trim() || undefined,
        relatedCourseId: form.relatedCourseId?.trim() || undefined,
        relatedModuleId: form.relatedModuleId?.trim() || undefined,
        relatedLessonId: form.relatedLessonId?.trim() || undefined,
        providerId: form.providerId?.trim() || undefined,
      }
      if (isEdit && event) {
        await platformAdminApi.updateEvent(event.id, payload)
        onSaved(t("eventForm.updatedOk"))
      } else {
        await platformAdminApi.createEvent(payload)
        onSaved(t("eventForm.createdOk"))
      }
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("eventForm.saveFailed"))
    } finally {
      setSaving(false)
    }
  }

  const inputClass =
    "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
  const labelClass = "block text-xs font-medium text-muted-foreground mb-1.5"

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={saving ? undefined : onClose} />
      <div className="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-foreground">{isEdit ? t("eventForm.titleEdit") : t("eventForm.titleCreate")}</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {isEdit ? t("eventForm.subEdit") : t("eventForm.subCreate")}
            </p>
          </div>
          <button type="button" onClick={onClose} disabled={saving} className="rounded-lg p-1 text-muted-foreground hover:text-foreground disabled:opacity-50" aria-label={tc("close")}>
            <X className="size-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className={labelClass}>{t("eventForm.labelTitle")}</label>
            <input type="text" value={form.title} onChange={set("title")} required minLength={2} maxLength={300} placeholder={t("eventForm.phTitle")} className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>{t("eventForm.labelDescription")}</label>
            <textarea value={form.description} onChange={set("description")} rows={3} maxLength={2000} placeholder={t("eventForm.phDescription")} className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelEventType")}</label>
              <select value={form.eventType} onChange={set("eventType")} className={inputClass}>
                {EVENT_TYPES.map((et) => (
                  <option key={et} value={et}>{et}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelCategory")}</label>
              <select value={form.category} onChange={set("category")} className={inputClass}>
                <option value="">{t("eventForm.selectCategory")}</option>
                {EVENT_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelStatus")}</label>
              <select value={form.status} onChange={set("status")} className={inputClass}>
                {STATUSES.map((st) => (
                  <option key={st} value={st}>{st}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelTimezone")}</label>
              <input type="text" value={form.timezone} onChange={set("timezone")} maxLength={50} className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelStartsAt")}</label>
              <input type="datetime-local" value={form.startsAt} onChange={set("startsAt")} required className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelEndsAt")}</label>
              <input type="datetime-local" value={form.endsAt ?? ""} onChange={set("endsAt")} className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelDuration")}</label>
              <input type="number" value={form.durationMinutes ?? 60} onChange={set("durationMinutes")} min={1} max={10080} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelMaxParticipants")}</label>
              <input type="number" value={form.maxParticipants ?? ""} onChange={set("maxParticipants")} min={1} max={100000} className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelLocation")}</label>
              <input type="text" value={form.location} onChange={set("location")} maxLength={255} placeholder={t("eventForm.phLocation")} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelMeetingUrl")}</label>
              <input type="url" value={form.meetingUrl} onChange={set("meetingUrl")} maxLength={255} placeholder="https://meet.example.com/..." className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>{t("eventForm.labelThumbnail")}</label>
            <input type="url" value={form.thumbnailUrl} onChange={set("thumbnailUrl")} maxLength={500} placeholder="https://..." className={inputClass} />
          </div>

          <div>
            <label className={labelClass}>{t("eventForm.labelTags")}</label>
            <input type="text" value={form.tags} onChange={set("tags")} maxLength={500} placeholder={t("eventForm.phTags")} className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelAccessLevel")}</label>
              <input type="text" value={form.accessLevel} onChange={set("accessLevel")} maxLength={50} placeholder={t("eventForm.phAccessLevel")} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelPresenter")}</label>
              <input type="text" value={form.presenterName} onChange={set("presenterName")} maxLength={100} placeholder={t("eventForm.phPresenter")} className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelFormat")}</label>
              <input type="text" value={form.eventFormat} onChange={set("eventFormat")} maxLength={50} placeholder={t("eventForm.phFormat")} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelDifficulty")}</label>
              <input type="text" value={form.difficulty} onChange={set("difficulty")} maxLength={50} placeholder={t("eventForm.phDifficulty")} className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelAudience")}</label>
              <input type="text" value={form.targetAudience} onChange={set("targetAudience")} maxLength={255} placeholder={t("eventForm.phAudience")} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelPresenter")}</label>
              <input type="text" value={form.presenterName} onChange={set("presenterName")} maxLength={100} placeholder={t("eventForm.phPresenter")} className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelPrereq")}</label>
              <input type="text" value={form.prerequisites} onChange={set("prerequisites")} maxLength={500} placeholder={t("eventForm.phPrereq")} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelOutcomes")}</label>
              <input type="text" value={form.learningOutcomes} onChange={set("learningOutcomes")} maxLength={500} placeholder={t("eventForm.phOutcomes")} className={inputClass} />
            </div>
          </div>

          <div>
            <label className={labelClass}>{t("eventForm.labelAgenda")}</label>
            <textarea value={form.agenda} onChange={set("agenda")} rows={4} maxLength={2000} placeholder={t("eventForm.phAgenda")} className={inputClass} />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label className={labelClass}>{t("eventForm.labelCourseId")}</label>
              <input type="text" value={form.relatedCourseId ?? ""} onChange={set("relatedCourseId")} placeholder="UUID" className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelModuleId")}</label>
              <input type="text" value={form.relatedModuleId ?? ""} onChange={set("relatedModuleId")} placeholder="UUID" className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>{t("eventForm.labelLessonId")}</label>
              <input type="text" value={form.relatedLessonId ?? ""} onChange={set("relatedLessonId")} placeholder="UUID" className={inputClass} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className={labelClass}>{t("eventForm.labelProviderId")}</label>
              <input type="text" value={form.providerId ?? ""} onChange={set("providerId")} placeholder="UUID (optional)" className={inputClass} />
            </div>
          </div>

          <div className="flex items-center gap-4">
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={form.isFree} onChange={(e) => setForm((p) => ({ ...p, isFree: e.target.checked }))} className="rounded border-border" />
              <span className="text-sm">{t("eventForm.freeEvent")}</span>
            </label>
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={form.requiresApproval} onChange={(e) => setForm((p) => ({ ...p, requiresApproval: e.target.checked }))} className="rounded border-border" />
              <span className="text-sm">{t("eventForm.requiresApproval")}</span>
            </label>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} disabled={saving} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50">
              {tc("cancel")}
            </button>
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {saving && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? t("eventForm.btnSave") : t("eventForm.btnCreate")}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
