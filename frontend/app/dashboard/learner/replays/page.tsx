"use client"

import { useEffect, useState, useCallback } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, isReplayFailed, type ReplayItem, LearnerApiError } from "@/lib/learner-api"
import { useLowBandwidth } from "@/components/primary/low-bandwidth-provider"
import { Search, Play, CalendarDays, Loader2, XCircle, ArrowRight, Eye, RefreshCw, AlertCircle, Clock } from "lucide-react"

function formatDuration(seconds: number) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}h ${m}m`
  if (m > 0) return `${m}m ${s}s`
  return `${s}s`
}

function formatPosition(seconds: number) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`
  return `${m}:${s.toString().padStart(2, "0")}`
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
}

const EVENT_TYPES = ["SEMINAR", "WORKSHOP", "WEBINAR", "TRAINING", "CONFERENCE", "LECTURE"]

export default function ReplaysPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const { lazyLoadImages } = useLowBandwidth()

  const [replays, setReplays] = useState<ReplayItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState("")
  const [dateFrom, setDateFrom] = useState("")
  const [dateTo, setDateTo] = useState("")

  const loadReplays = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const data = await learnerApi.getReplays({
        search: search || undefined,
        eventType: typeFilter || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      })
      setReplays(data)
    } catch (e: any) {
      if (e instanceof LearnerApiError) {
        setError(`${e.message}${e.status ? ` (${e.status})` : ""}`)
      } else {
        setError(e.message || tc("error.load"))
      }
    } finally {
      setLoading(false)
    }
  }, [search, typeFilter, dateFrom, dateTo, tc])

  useEffect(() => {
    loadReplays()
  }, [loadReplays])

  const continueWatching = replays.filter((r) => r.positionSeconds > 0 && !r.completed && !isReplayFailed(r))
  const failedReplays = replays.filter((r) => isReplayFailed(r))
  const processingReplays = replays.filter((r) => (r.status || r.recordingStatus || "").toUpperCase() === "PROCESSING")
  const newRecordings = [...replays]
    .filter((r) => (r.positionSeconds === 0 || r.completed) && !isReplayFailed(r))
    .sort((a, b) => (b.recordedAt || "").localeCompare(a.recordedAt || ""))
    .slice(0, 6)
  const otherReplays = replays.filter((r) => (r.positionSeconds === 0 || r.completed) && !isReplayFailed(r))

  return (
    <main role="main" aria-label={t("replays.title")} className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t("replays.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("replays.subtitle")}</p>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t("replays.searchPlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            aria-label={t("replays.searchPlaceholder")}
          />
        </div>
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="h-10 rounded-lg border border-border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
          aria-label={t("replays.filterByType")}
        >
          <option value="">{t("replays.allTypes")}</option>
          {EVENT_TYPES.map((type) => (
            <option key={type} value={type}>{type}</option>
          ))}
        </select>
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => setDateFrom(e.target.value)}
          className="h-10 rounded-lg border border-border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
          aria-label={t("replays.filterByDate")}
        />
        <input
          type="date"
          value={dateTo}
          onChange={(e) => setDateTo(e.target.value)}
          className="h-10 rounded-lg border border-border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
          aria-label={t("replays.filterByDate")}
        />
      </div>

      {loading && (
        <div aria-busy="true" className="flex items-center justify-center py-16">
          <Loader2 className="size-8 animate-spin text-primary" />
        </div>
      )}

      {error && (
        <div role="alert" className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <XCircle className="size-4 shrink-0" />
          <span className="flex-1">{error}</span>
          <button
            type="button"
            onClick={loadReplays}
            className="inline-flex items-center gap-1 rounded-md border border-red-200 px-2 py-1 text-xs font-medium hover:bg-red-100"
            aria-label={tc("retry")}
          >
            <RefreshCw className="size-3" /> {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && replays.length === 0 && (
        <div role="status" className="rounded-xl border border-border bg-card p-12 text-center">
          <Play className="mx-auto size-12 text-muted-foreground/50" aria-hidden="true" />
          <h2 className="mt-4 text-lg font-semibold">{t("replays.noReplays")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("replays.noReplaysDescription")}</p>
          <p className="mt-2 text-xs text-muted-foreground">{t("replays.processingNote")}</p>
        </div>
      )}

      {!loading && !error && failedReplays.length > 0 && (
        <section aria-label={t("status.recordingFailed")}>
          <div className="mb-3 flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive" role="alert">
            <AlertCircle className="size-4 shrink-0" aria-hidden="true" />
            <span>{t("status.recordingFailed")}: {failedReplays.length}</span>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {failedReplays.map((replay) => (
              <div key={`failed-${replay.id}`} className="rounded-xl border border-destructive/20 bg-card p-4">
                <div className="flex items-center gap-2 text-destructive">
                  <AlertCircle className="size-4" aria-hidden="true" />
                  <span className="text-xs font-semibold uppercase tracking-wide" role="status">{t("status.recordingFailed")}</span>
                </div>
                <h3 className="mt-2 line-clamp-2 text-sm font-semibold">{replay.title}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{replay.eventTitle}</p>
                <p className="mt-2 text-xs text-muted-foreground">{t("status.recordingFailedDesc")}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      {!loading && !error && processingReplays.length > 0 && (
        <div role="status" className="flex items-center gap-2 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-sm text-amber-700">
          <Clock className="size-4 shrink-0" aria-hidden="true" />
          {t("status.processing")} — {t("replays.processingNote")}
        </div>
      )}

      {!loading && !error && continueWatching.length > 0 && (
        <section aria-live="polite">
          <h2 className="mb-3 text-lg font-semibold">{t("replays.continueWatching")}</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {continueWatching.map((replay) => (
              <Link
                key={replay.id}
                href={`/dashboard/learner/replays/${replay.id}`}
                className="group rounded-xl border border-border bg-card p-4 transition-shadow hover:shadow-md"
              >
                <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
                  {replay.thumbnailUrl && !lazyLoadImages ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} className="h-full w-full object-cover" />
                  ) : replay.thumbnailUrl && lazyLoadImages ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} loading="lazy" className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <Play className="size-10 text-muted-foreground/50" aria-hidden="true" />
                    </div>
                  )}
                  <div className="absolute bottom-2 right-2 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                    {formatDuration(replay.durationSeconds)}
                  </div>
                  <div className="absolute bottom-2 left-2 rounded bg-primary/90 px-2 py-0.5 text-xs font-medium text-white">
                    {t("replays.continueFrom")} {formatPosition(replay.positionSeconds)}
                  </div>
                </div>
                <h3 className="mt-3 line-clamp-2 text-sm font-semibold group-hover:text-primary">{replay.title}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{replay.eventTitle}</p>
                <div className="mt-2">
                  <div className="flex items-center justify-between text-[10px] text-muted-foreground mb-1">
                    <span>{formatPosition(replay.positionSeconds)}</span>
                    <span>{formatDuration(replay.durationSeconds)}</span>
                  </div>
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary transition-all"
                      style={{ width: `${replay.durationSeconds > 0 ? Math.min((replay.positionSeconds / replay.durationSeconds) * 100, 100) : 0}%` }}
                    />
                  </div>
                </div>
                <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><CalendarDays className="size-3" /> {formatDate(replay.recordedAt)}</span>
                  <span className="flex items-center gap-1"><Eye className="size-3" /> {replay.viewCount} {t("replays.viewCount")}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {!loading && !error && newRecordings.length > 0 && (
        <section aria-label={t("replays.newRecordings")}>
          <h2 className="mb-3 text-lg font-semibold">{t("replays.newRecordings")}</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {newRecordings.map((replay) => (
              <Link
                key={`new-${replay.id}`}
                href={`/dashboard/learner/replays/${replay.id}`}
                className="group rounded-xl border border-border bg-card p-4 transition-shadow hover:shadow-md"
              >
                <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
                  {replay.thumbnailUrl && !lazyLoadImages ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} className="h-full w-full object-cover" />
                  ) : replay.thumbnailUrl && lazyLoadImages ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} loading="lazy" className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <Play className="size-10 text-muted-foreground/50" aria-hidden="true" />
                    </div>
                  )}
                  <div className="absolute bottom-2 right-2 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                    {formatDuration(replay.durationSeconds)}
                  </div>
                </div>
                <h3 className="mt-3 line-clamp-2 text-sm font-semibold group-hover:text-primary">{replay.title}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{replay.eventTitle}</p>
                <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><CalendarDays className="size-3" aria-hidden="true" /> {formatDate(replay.recordedAt)}</span>
                  <span className="flex items-center gap-1"><Eye className="size-3" aria-hidden="true" /> {replay.viewCount} {t("replays.viewCount")}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {!loading && !error && otherReplays.length > 0 && (
        <section>
          {continueWatching.length > 0 && <h2 className="mb-3 text-lg font-semibold">{t("allReplays")}</h2>}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {otherReplays.map((replay) => (
              <Link
                key={replay.id}
                href={`/dashboard/learner/replays/${replay.id}`}
                className="group rounded-xl border border-border bg-card p-4 transition-shadow hover:shadow-md"
              >
                <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
                  {replay.thumbnailUrl && !lazyLoadImages ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} className="h-full w-full object-cover" />
                  ) : replay.thumbnailUrl && lazyLoadImages ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} loading="lazy" className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <Play className="size-10 text-muted-foreground/50" aria-hidden="true" />
                    </div>
                  )}
                  <div className="absolute bottom-2 right-2 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                    {formatDuration(replay.durationSeconds)}
                  </div>
                </div>
                <h3 className="mt-3 line-clamp-2 text-sm font-semibold group-hover:text-primary">{replay.title}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{replay.eventTitle}</p>
                {replay.presenterName && (
                  <p className="mt-0.5 text-xs text-muted-foreground">{replay.presenterName}</p>
                )}
                {(replay.status || replay.recordingStatus || "").toUpperCase() === "PROCESSING" && (
                  <span className="mt-1 inline-flex items-center gap-1 rounded bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold text-amber-700" role="status">
                    <Clock className="size-3" aria-hidden="true" /> {t("status.processing")}
                  </span>
                )}
                {replay.durationSeconds > 0 && (
                  <div className="mt-2">
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                      <div
                        className="h-full rounded-full bg-primary transition-all"
                        style={{ width: `${replay.durationSeconds > 0 ? Math.min((replay.positionSeconds / replay.durationSeconds) * 100, 100) : 0}%` }}
                      />
                    </div>
                  </div>
                )}
                <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><CalendarDays className="size-3" /> {formatDate(replay.recordedAt)}</span>
                  <span className="flex items-center gap-1"><Eye className="size-3" /> {replay.viewCount} {t("replays.viewCount")}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {!loading && !error && replays.length > 0 && (
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="mb-4 text-lg font-semibold">{t("replays.relatedLearning")}</h2>
          <div className="grid gap-4 sm:grid-cols-3">
            {replays.slice(0, 3).map((replay) => (
              <div key={replay.id} className="space-y-2">
                {replay.relatedCourseId && (
                  <Link
                    href={`/dashboard/learner/courses/${replay.relatedCourseId}`}
                    className="block rounded-lg border border-border p-3 text-sm font-medium hover:bg-muted/50"
                  >
                    {t("replays.relatedCourse")}: {replay.relatedCourseTitle}
                  </Link>
                )}
                {replay.relatedLessonId && (
                  <Link
                    href={
                      replay.relatedLessonId && replay.relatedCourseId
                        ? `/dashboard/learner/courses/${replay.relatedCourseId}/lessons/${replay.relatedLessonId}`
                        : replay.relatedCourseId
                          ? `/dashboard/learner/courses/${replay.relatedCourseId}`
                          : "#"
                    }
                    className="block rounded-lg border border-border p-3 text-sm font-medium hover:bg-muted/50"
                    aria-label={`${t("replays.relatedLesson")}: ${replay.relatedLessonTitle || replay.relatedLessonId}`}
                  >
                    {t("replays.relatedLesson")}: {replay.relatedLessonTitle}
                  </Link>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      <div className="rounded-xl border border-border bg-card p-6 text-center">
        <p className="text-sm text-muted-foreground">{t("browseUpcomingLive")}</p>
        <Link href="/dashboard/learner/live-classes" className="mt-2 inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline">
          {t("browseLiveClasses")} <ArrowRight className="size-3" />
        </Link>
      </div>
    </main>
  )
}
