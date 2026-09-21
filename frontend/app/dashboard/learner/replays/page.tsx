"use client"

import { useEffect, useState, useCallback } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, type ReplayItem } from "@/lib/learner-api"
import { Search, Filter, Play, Clock, Eye, CalendarDays, Loader2, XCircle } from "lucide-react"

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
      setError(e.message || tc("error"))
    } finally {
      setLoading(false)
    }
  }, [search, typeFilter, dateFrom, dateTo, tc])

  useEffect(() => {
    loadReplays()
  }, [loadReplays])

  const continueWatching = replays.filter((r) => r.positionSeconds > 0 && !r.completed)
  const otherReplays = replays.filter((r) => r.positionSeconds === 0 || r.completed)

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
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-8 animate-spin text-primary" />
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <XCircle className="size-4 shrink-0" /> {error}
        </div>
      )}

      {!loading && !error && replays.length === 0 && (
        <div className="rounded-xl border border-border bg-card p-12 text-center">
          <Play className="mx-auto size-12 text-muted-foreground/50" />
          <h2 className="mt-4 text-lg font-semibold">{t("replays.noReplays")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("replays.noReplaysDescription")}</p>
        </div>
      )}

      {!loading && !error && continueWatching.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-semibold">{t("replays.continueWatching")}</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {continueWatching.map((replay) => (
              <Link
                key={replay.id}
                href={`/dashboard/learner/replays/${replay.id}`}
                className="group rounded-xl border border-border bg-card p-4 transition-shadow hover:shadow-md"
              >
                <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
                  {replay.thumbnailUrl ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <Play className="size-10 text-muted-foreground/50" />
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
                <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><CalendarDays className="size-3" /> {formatDate(replay.recordedAt)}</span>
                  <span className="flex items-center gap-1"><Eye className="size-3" /> {replay.viewCount} {t("replays.viewCount")}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {!loading && !error && otherReplays.length > 0 && (
        <section>
          {continueWatching.length > 0 && <h2 className="mb-3 text-lg font-semibold">All Replays</h2>}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {otherReplays.map((replay) => (
              <Link
                key={replay.id}
                href={`/dashboard/learner/replays/${replay.id}`}
                className="group rounded-xl border border-border bg-card p-4 transition-shadow hover:shadow-md"
              >
                <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
                  {replay.thumbnailUrl ? (
                    <img src={replay.thumbnailUrl} alt={replay.title} className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <Play className="size-10 text-muted-foreground/50" />
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
                    href={`/dashboard/learner/courses/${replay.relatedCourseId}`}
                    className="block rounded-lg border border-border p-3 text-sm font-medium hover:bg-muted/50"
                  >
                    {t("replays.relatedLesson")}: {replay.relatedLessonTitle}
                  </Link>
                )}
              </div>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}
