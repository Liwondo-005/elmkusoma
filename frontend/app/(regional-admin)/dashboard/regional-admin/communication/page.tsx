"use client"

import { Suspense, useCallback, useEffect, useRef, useState, type FormEvent } from "react"
import { useSearchParams } from "next/navigation"
import { CheckCircle2, Plus, Send, X, XCircle } from "lucide-react"
import {
  regionalAdminApi,
  type AnnouncementSummary,
  type DistrictInfo,
  type RegionalInstitution,
} from "@/lib/regional-admin-api"
import {
  Chip,
  ErrorState,
  LoadingState,
  PageHeader,
  PagedList,
  formatDate,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

const PRIORITIES = ["NORMAL", "HIGH", "URGENT"]
const AUDIENCES = ["ALL", "DISTRICTS", "INSTITUTIONS"]

function priorityTone(priority: string): "danger" | "warning" | "default" {
  if (priority === "URGENT") return "danger"
  if (priority === "HIGH") return "warning"
  return "default"
}

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback
}

function AnnouncementsContent() {
  const searchParams = useSearchParams()

  const [composeOpen, setComposeOpen] = useState(searchParams.get("compose") === "1")
  const [reloadKey, setReloadKey] = useState(0)
  const [success, setSuccess] = useState<string | null>(null)

  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [priority, setPriority] = useState("NORMAL")
  const [audienceType, setAudienceType] = useState("ALL")
  const [districtIds, setDistrictIds] = useState<string[]>([])
  const [institutionIds, setInstitutionIds] = useState<string[]>([])

  const [districts, setDistricts] = useState<DistrictInfo[]>([])
  const [institutions, setInstitutions] = useState<RegionalInstitution[]>([])
  const [optionsLoading, setOptionsLoading] = useState(false)
  const [optionsError, setOptionsError] = useState<string | null>(null)

  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const districtsLoaded = useRef(false)
  const institutionsLoaded = useRef(false)

  const loadDistricts = useCallback(async () => {
    setOptionsLoading(true)
    setOptionsError(null)
    try {
      const regions = await regionalAdminApi.getRegions()
      const lists = await Promise.all(
        regions.map((region) => regionalAdminApi.getDistricts(region.id)),
      )
      setDistricts(lists.flat())
    } catch (e) {
      setOptionsError(errorMessage(e, "Unable to load districts."))
    } finally {
      setOptionsLoading(false)
    }
  }, [])

  const loadInstitutions = useCallback(async () => {
    setOptionsLoading(true)
    setOptionsError(null)
    try {
      const page = await regionalAdminApi.listInstitutions({ size: 100 })
      setInstitutions(page.content)
    } catch (e) {
      setOptionsError(errorMessage(e, "Unable to load institutions."))
    } finally {
      setOptionsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (searchParams.get("compose") === "1") setComposeOpen(true)
  }, [searchParams])

  useEffect(() => {
    if (audienceType !== "DISTRICTS" || districtsLoaded.current) return
    districtsLoaded.current = true
    loadDistricts()
  }, [audienceType, loadDistricts])

  useEffect(() => {
    if (audienceType !== "INSTITUTIONS" || institutionsLoaded.current) return
    institutionsLoaded.current = true
    loadInstitutions()
  }, [audienceType, loadInstitutions])

  // reloadKey is part of the fetcher identity so PagedList refetches after a send.
  const fetcher = useCallback(
    (p: { page: number; size: number; search?: string }) =>
      regionalAdminApi.listAnnouncements(p),
    [reloadKey],
  )

  function toggle(list: string[], id: string): string[] {
    return list.includes(id) ? list.filter((x) => x !== id) : [...list, id]
  }

  function validate(): string | null {
    if (!title.trim()) return "Title is required."
    if (!content.trim()) return "Message content is required."
    if (audienceType === "DISTRICTS" && districtIds.length === 0) {
      return "Select at least one district."
    }
    if (audienceType === "INSTITUTIONS" && institutionIds.length === 0) {
      return "Select at least one institution."
    }
    return null
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    const validation = validate()
    if (validation) {
      setFormError(validation)
      return
    }
    setSubmitting(true)
    setFormError(null)
    setSuccess(null)
    try {
      const created = await regionalAdminApi.createAnnouncement({
        title: title.trim(),
        content: content.trim(),
        priority,
        audienceType,
        ...(audienceType === "DISTRICTS" ? { targetDistrictIds: districtIds } : {}),
        ...(audienceType === "INSTITUTIONS" ? { targetInstitutionIds: institutionIds } : {}),
      })
      setSuccess(`Sent to ${created.recipientCount} recipients.`)
      setTitle("")
      setContent("")
      setPriority("NORMAL")
      setAudienceType("ALL")
      setDistrictIds([])
      setInstitutionIds([])
      setComposeOpen(false)
      setReloadKey((k) => k + 1)
    } catch (e) {
      setFormError(errorMessage(e, "Unable to send the announcement."))
    } finally {
      setSubmitting(false)
    }
  }

  const fieldClass =
    "w-full rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
  const labelClass = "block text-[10px] font-bold uppercase tracking-widest text-muted-foreground"

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Regional Announcements"
        description="Send announcements to validated audiences inside your jurisdiction — all recipients are resolved server-side from your districts and institutions."
        actions={
          <button
            type="button"
            onClick={() => {
              setComposeOpen((open) => !open)
              setFormError(null)
            }}
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
          >
            {composeOpen ? <X className="size-4" /> : <Plus className="size-4" />}
            {composeOpen ? "Close" : "New announcement"}
          </button>
        }
      />

      {success && (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          <span className="flex items-center gap-2">
            <CheckCircle2 className="size-4 shrink-0" /> {success}
          </span>
          <button
            type="button"
            onClick={() => setSuccess(null)}
            className="text-xs font-semibold underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {composeOpen && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="text-sm font-bold text-foreground">New announcement</h2>

          <form onSubmit={submit} className="mt-4 space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <label htmlFor="announcement-title" className={labelClass}>
                  Title
                </label>
                <input
                  id="announcement-title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Term opening and examination timetable"
                  className={`mt-1.5 ${fieldClass}`}
                />
              </div>

              <div className="sm:col-span-2">
                <label htmlFor="announcement-content" className={labelClass}>
                  Content
                </label>
                <textarea
                  id="announcement-content"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  rows={5}
                  placeholder="Full message shown to every recipient"
                  className={`mt-1.5 ${fieldClass}`}
                />
              </div>

              <div>
                <label htmlFor="announcement-priority" className={labelClass}>
                  Priority
                </label>
                <select
                  id="announcement-priority"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  className={`mt-1.5 ${fieldClass}`}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="announcement-audience" className={labelClass}>
                  Audience
                </label>
                <select
                  id="announcement-audience"
                  value={audienceType}
                  onChange={(e) => {
                    setAudienceType(e.target.value)
                    setFormError(null)
                    setOptionsError(null)
                  }}
                  className={`mt-1.5 ${fieldClass}`}
                >
                  {AUDIENCES.map((a) => (
                    <option key={a} value={a}>
                      {a}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {audienceType === "DISTRICTS" && (
              <div className="rounded-xl border border-border bg-muted/20 p-4">
                <p className={labelClass}>Districts ({districtIds.length} selected)</p>
                {optionsLoading && <LoadingState label="Loading districts…" />}
                {optionsError && (
                  <div className="mt-2">
                    <ErrorState message={optionsError} onRetry={loadDistricts} />
                  </div>
                )}
                {!optionsLoading && !optionsError && districts.length === 0 && (
                  <p className="mt-2 text-sm text-muted-foreground">
                    No districts are available inside your jurisdiction.
                  </p>
                )}
                {!optionsLoading && districts.length > 0 && (
                  <div className="mt-2 max-h-64 space-y-1.5 overflow-y-auto pr-1">
                    {districts.map((d) => (
                      <label
                        key={d.id}
                        className="flex cursor-pointer items-center gap-2 rounded-lg border border-border bg-background px-3 py-2 text-sm"
                      >
                        <input
                          type="checkbox"
                          checked={districtIds.includes(d.id)}
                          onChange={() => setDistrictIds((prev) => toggle(prev, d.id))}
                          className="size-4 accent-primary"
                        />
                        <span className="min-w-0 flex-1 truncate">{d.name}</span>
                        <span className="text-xs text-muted-foreground">{d.code}</span>
                      </label>
                    ))}
                  </div>
                )}
              </div>
            )}

            {audienceType === "INSTITUTIONS" && (
              <div className="rounded-xl border border-border bg-muted/20 p-4">
                <p className={labelClass}>Institutions ({institutionIds.length} selected)</p>
                {optionsLoading && <LoadingState label="Loading institutions…" />}
                {optionsError && (
                  <div className="mt-2">
                    <ErrorState message={optionsError} onRetry={loadInstitutions} />
                  </div>
                )}
                {!optionsLoading && !optionsError && institutions.length === 0 && (
                  <p className="mt-2 text-sm text-muted-foreground">
                    No institutions were returned for your jurisdiction.
                  </p>
                )}
                {!optionsLoading && institutions.length > 0 && (
                  <div className="mt-2 max-h-64 space-y-1.5 overflow-y-auto pr-1">
                    {institutions.map((inst) => (
                      <label
                        key={inst.id}
                        className="flex cursor-pointer items-center gap-2 rounded-lg border border-border bg-background px-3 py-2 text-sm"
                      >
                        <input
                          type="checkbox"
                          checked={institutionIds.includes(inst.id)}
                          onChange={() => setInstitutionIds((prev) => toggle(prev, inst.id))}
                          className="size-4 accent-primary"
                        />
                        <span className="min-w-0 flex-1 truncate">{inst.name}</span>
                        <span className="text-xs text-muted-foreground">{inst.districtName}</span>
                      </label>
                    ))}
                  </div>
                )}
              </div>
            )}

            {formError && (
              <p className="flex items-center gap-2 rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
                <XCircle className="size-4 shrink-0" /> {formError}
              </p>
            )}

            <div className="flex items-center gap-3">
              <button
                type="submit"
                disabled={submitting}
                className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Send className="size-4" />
                {submitting ? "Sending…" : "Send announcement"}
              </button>
              <p className="text-xs text-muted-foreground">
                Recipients are counted by the server. Large audiences are rejected with an explicit
                limit error.
              </p>
            </div>
          </form>
        </section>
      )}

      <section className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h2 className="text-sm font-bold text-foreground">Sent announcements</h2>
        <div className="mt-4">
          <PagedList<AnnouncementSummary>
            fetcher={fetcher}
            searchPlaceholder="Search announcements…"
            emptyTitle="No announcements have been sent yet."
            emptyHint="Announcements you send to your jurisdiction will be listed here."
            renderItem={(item) => (
              <article className="rounded-2xl border border-border bg-muted/20 p-4">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-sm font-bold text-foreground">{item.title}</h3>
                  <Chip tone={priorityTone(item.priority)}>{item.priority}</Chip>
                  <Chip tone="info">{item.audienceType}</Chip>
                </div>
                <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">
                  {item.summary}
                </p>
                <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                  <span className="font-semibold text-foreground">
                    {item.recipientCount.toLocaleString()} recipients
                  </span>
                  <span>Sent by {item.sentBy}</span>
                  <span>{formatDateTime(item.sentAt)}</span>
                  {item.expiresAt && <span>Expires {formatDate(item.expiresAt)}</span>}
                </div>
              </article>
            )}
          />
        </div>
      </section>
    </div>
  )
}

export default function RegionalAnnouncementsPage() {
  return (
    <Suspense fallback={<LoadingState label="Loading announcements…" />}>
      <AnnouncementsContent />
    </Suspense>
  )
}
