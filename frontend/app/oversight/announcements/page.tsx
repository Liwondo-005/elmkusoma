"use client"

import { useTranslations } from "next-intl";

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useRequireAuth } from "@/lib/auth"
import {
  Megaphone,
  Send,
  Loader2,
  AlertCircle,
  Globe2,
  MapPin,
  Building2,
  CalendarClock,
} from "lucide-react"
import {
  oversightApi,
  type OversightAnnouncement,
  type OversightDistrict,
  type OversightRegion,
} from "@/lib/api"
import { cn } from "@/lib/utils"

export default function OversightAnnouncementsPage() {
  const t = useTranslations("oversight")
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const isNational = user?.role === "National Admin"
  // Regional Admins publish here too — always scoped to their own region
  // (the backend resolves/pins the scope; NATIONWIDE stays national-only).
  const isRegional = user?.role === "Regional Admin"
  const canCompose = isNational || isRegional

  const [items, setItems] = useState<OversightAnnouncement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)

  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [priority, setPriority] = useState("NORMAL")
  const [audienceType, setAudienceType] = useState("NATIONWIDE")
  const [regionId, setRegionId] = useState("")
  const [districtId, setDistrictId] = useState("")
  const [scheduledAt, setScheduledAt] = useState("")
  const [regions, setRegions] = useState<OversightRegion[]>([])
  const [districts, setDistricts] = useState<OversightDistrict[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(async () => {
    setError(null)
    try {
      setItems(await oversightApi.announcements())
    } catch {
      setError(t("announcements.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    if (authLoading || !user) return
    load()
    if (isNational) {
      oversightApi.regions().then(setRegions).catch(() => setRegions([]))
    }
    if (canCompose) {
      try {
        if (new URLSearchParams(window.location.search).get("action") === "create") {
          setShowForm(true)
        }
      } catch {}
    }
  }, [authLoading, user, isNational, canCompose, load])

  useEffect(() => {
    if (!regionId) {
      setDistricts([])
      return
    }
    let cancelled = false
    oversightApi
      .districts(regionId)
      .then((list) => {
        if (!cancelled) setDistricts(list)
      })
      .catch(() => {
        if (!cancelled) setDistricts([])
      })
    return () => {
      cancelled = true
    }
  }, [regionId])

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setSuccess(null)
    try {
      const created = await oversightApi.createAnnouncement({
        title,
        content,
        priority,
        audienceType: isRegional ? "REGION" : audienceType,
        audienceRegionId: isRegional ? null : audienceType === "REGION" ? regionId : null,
        audienceDistrictId: isRegional ? null : audienceType === "DISTRICT" ? districtId : null,
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : null,
      })
      setItems((prev) => [created, ...prev])
      setSuccess(t("announcements.created"))
      setTitle("")
      setContent("")
      setPriority("NORMAL")
      setAudienceType("NATIONWIDE")
      setRegionId("")
      setDistrictId("")
      setScheduledAt("")
      setShowForm(false)
      if (window.location.search.includes("action=create")) router.replace("/oversight/announcements")
    } catch (err) {
      setError(err instanceof Error ? err.message : t("announcements.createFailed"))
    } finally {
      setSubmitting(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("announcements.loading")}</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Megaphone className="size-5 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{t("announcements.title")}</h1>
              <p className="text-sm text-muted-foreground">{t("announcements.subtitle")}</p>
            </div>
          </div>
          {canCompose && (
            <button
              type="button"
              onClick={() => setShowForm((v) => !v)}
              className="flex items-center gap-1.5 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              <Send className="size-4" />
              {showForm ? t("announcements.cancel") : t("announcements.create")}
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}
      {success && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          {success}
        </div>
      )}

      {showForm && canCompose && (
        <form onSubmit={submit} className="space-y-4 rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-foreground">{t("announcements.createTitle")}</h2>

          <div>
            <label htmlFor="ann-title" className="mb-1 block text-sm font-medium text-foreground">
              {t("announcements.titleLabel")}
            </label>
            <input
              id="ann-title"
              required
              maxLength={300}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="ann-content" className="mb-1 block text-sm font-medium text-foreground">
              {t("announcements.contentLabel")}
            </label>
            <textarea
              id="ann-content"
              required
              rows={5}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring"
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label htmlFor="ann-priority" className="mb-1 block text-sm font-medium text-foreground">
                {t("announcements.priorityLabel")}
              </label>
              <select
                id="ann-priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="LOW">LOW</option>
                <option value="NORMAL">NORMAL</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>

            <div>
              <label htmlFor="ann-audience" className="mb-1 block text-sm font-medium text-foreground">
                {t("announcements.audienceLabel")}
              </label>
              <select
                id="ann-audience"
                value={isRegional ? "REGION" : audienceType}
                onChange={(e) => {
                  setAudienceType(e.target.value)
                  setDistrictId("")
                }}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                {isRegional ? (
                  // A Regional Admin's audience is always their own region —
                  // the server pins the scope regardless of what is sent.
                  <option value="REGION">{t("announcements.audienceRegion")}</option>
                ) : (
                  <>
                    <option value="NATIONWIDE">{t("announcements.audienceNationwide")}</option>
                    <option value="REGION">{t("announcements.audienceRegion")}</option>
                    <option value="DISTRICT">{t("announcements.audienceDistrict")}</option>
                  </>
                )}
              </select>
            </div>

            <div>
              <label htmlFor="ann-schedule" className="mb-1 block text-sm font-medium text-foreground">
                {t("announcements.scheduleLabel")}
              </label>
              <input
                id="ann-schedule"
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
          </div>

          {isNational && (audienceType === "REGION" || audienceType === "DISTRICT") && (
            <div>
              <label htmlFor="ann-region" className="mb-1 block text-sm font-medium text-foreground">
                {t("announcements.regionLabel")}
              </label>
              <select
                id="ann-region"
                required
                value={regionId}
                onChange={(e) => {
                  setRegionId(e.target.value)
                  setDistrictId("")
                }}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">{t("announcements.selectRegion")}</option>
                {regions.map((region) => (
                  <option key={region.id} value={region.id}>
                    {region.name} ({region.code})
                  </option>
                ))}
              </select>
            </div>
          )}

          {isNational && audienceType === "DISTRICT" && (
            <div>
              <label htmlFor="ann-district" className="mb-1 block text-sm font-medium text-foreground">
                {t("announcements.districtLabel")}
              </label>
              <select
                id="ann-district"
                required
                value={districtId}
                onChange={(e) => setDistrictId(e.target.value)}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">{t("announcements.selectDistrict")}</option>
                {districts.map((district) => (
                  <option key={district.id} value={district.id}>
                    {district.name} ({district.code})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted"
            >
              {t("announcements.cancel")}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
            >
              {submitting ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
              {t("announcements.publish")}
            </button>
          </div>
        </form>
      )}

      {items.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border p-10 text-center text-muted-foreground">
          {t("announcements.empty")}
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((announcement) => (
            <div key={announcement.id} className="rounded-2xl border border-border bg-card p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-semibold text-foreground">{announcement.title}</h2>
                    <AudienceBadge announcement={announcement} />
                    {announcement.status === "SCHEDULED" && (
                      <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                        <CalendarClock className="size-3" />
                        {t("announcements.statusScheduled")}
                      </span>
                    )}
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                      {announcement.priority}
                    </span>
                  </div>
                  <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">{announcement.content}</p>
                  <p className="mt-2 text-xs text-muted-foreground">
                    {announcement.authorName ? `${announcement.authorName} • ` : ""}
                    {announcement.publishedAt
                      ? new Date(announcement.publishedAt).toLocaleString()
                      : announcement.scheduledAt
                        ? `${t("announcements.statusScheduled")} ${new Date(announcement.scheduledAt).toLocaleString()}`
                        : announcement.createdAt
                          ? new Date(announcement.createdAt).toLocaleString()
                          : ""}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function AudienceBadge({ announcement }: { announcement: OversightAnnouncement }) {
  const t = useTranslations("oversight")
  const tone = cn("inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium")
  if (announcement.audienceType === "NATIONWIDE") {
    return (
      <span className={cn(tone, "bg-teal-100 text-teal-700")}>
        <Globe2 className="size-3" />
        {t("announcements.audienceNationwide")}
      </span>
    )
  }
  if (announcement.audienceType === "REGION") {
    return (
      <span className={cn(tone, "bg-blue-100 text-blue-700")}>
        <MapPin className="size-3" />
        {announcement.audienceRegionName || t("announcements.audienceRegion")}
      </span>
    )
  }
  if (announcement.audienceType === "DISTRICT") {
    return (
      <span className={cn(tone, "bg-purple-100 text-purple-700")}>
        <MapPin className="size-3" />
        {announcement.audienceDistrictName || t("announcements.audienceDistrict")}
      </span>
    )
  }
  return (
    <span className={cn(tone, "bg-gray-100 text-gray-700")}>
      <Building2 className="size-3" />
      {announcement.institutionName || t("announcements.audienceInstitution")}
    </span>
  )
}
