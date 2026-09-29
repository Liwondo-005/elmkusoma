"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { Video, CalendarClock, Radio, PlayCircle, Info } from "lucide-react"
import type { LiveLessonSummary } from "@/lib/learner-api"

function formatLiveDate(iso: string | null) {
  if (!iso) return ""
  return new Date(String(iso).replace(" ", "T")).toLocaleString(undefined, {
    weekday: "short",
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

/**
 * Lesson ↔ Live Class: shows the authoritative live session linked to a lesson
 * (state-aware: scheduled / LIVE NOW + join / ended + recording). Renders nothing
 * when the lesson has no linked live class. Reuses the existing live navigation
 * targets — /live-classes/{id} (room) and /dashboard/learner/live-classes/{id}.
 */
export function LessonLiveCard({ liveClass }: { liveClass: LiveLessonSummary | null | undefined }) {
  const t = useTranslations("learner")
  if (!liveClass) return null

  const isLive =
    liveClass.status === "IN_PROGRESS" || liveClass.status === "LIVE" || liveClass.status === "STARTING"
  const isScheduled = liveClass.status === "SCHEDULED"
  const isEnded = liveClass.status === "COMPLETED" || liveClass.status === "ENDED"
  const isCancelled = liveClass.status === "CANCELLED"

  return (
    <div className="mb-4 rounded-2xl border border-border bg-card p-4 shadow-xs">
      <div className="flex flex-wrap items-center gap-2">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-teal-500/10">
          <Video className="size-4 text-teal-600" />
        </span>
        <div className="min-w-0">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
            {t("lessonLive.sectionTitle")}
          </p>
          <p className="truncate text-sm font-medium text-foreground">{liveClass.title}</p>
        </div>
        <span className="ml-auto inline-flex shrink-0 items-center gap-1.5 rounded-full px-2 py-0.5 text-[10px] font-bold">
          {isLive && (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-red-100 px-2 py-0.5 text-[10px] font-bold text-red-700">
              <span className="size-1.5 animate-pulse rounded-full bg-red-600" />
              {t("lessonLive.liveNow")}
            </span>
          )}
          {isScheduled && (
            <span className="rounded-full bg-blue-100 px-2 py-0.5 text-blue-700">{t("lessonLive.scheduled")}</span>
          )}
          {isEnded && (
            <span className="rounded-full bg-gray-100 px-2 py-0.5 text-gray-700">{t("lessonLive.ended")}</span>
          )}
          {isCancelled && (
            <span className="rounded-full bg-red-100 px-2 py-0.5 text-red-700">{t("lessonLive.cancelled")}</span>
          )}
        </span>
      </div>

      {liveClass.scheduledAt && (
        <p className="mt-2 flex items-center gap-1 text-xs text-muted-foreground">
          <CalendarClock className="size-3.5 shrink-0" />
          {formatLiveDate(liveClass.scheduledAt)} · {liveClass.durationMinutes ?? 60} min
        </p>
      )}

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {isLive && (
          <Link
            href={`/live-classes/${liveClass.id}`}
            className="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-green-700"
          >
            <Radio className="size-3.5" />
            {t("lessonLive.join")}
          </Link>
        )}
        {isEnded && liveClass.recordingUrl && (
          <a
            href={liveClass.recordingUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground hover:bg-primary/90"
          >
            <PlayCircle className="size-3.5" />
            {t("lessonLive.watchRecording")}
          </a>
        )}
        {isEnded && !liveClass.recordingUrl && (
          <p className="text-xs text-muted-foreground">{t("lessonLive.noRecording")}</p>
        )}
        {(isScheduled || isEnded) && (
          <Link
            href={`/dashboard/learner/live-classes/${liveClass.id}`}
            className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
          >
            <Info className="size-3.5" />
            {t("lessonLive.viewDetails")}
          </Link>
        )}
        {isCancelled && <p className="text-xs text-muted-foreground">{t("lessonLive.cancelledNote")}</p>}
      </div>
    </div>
  )
}
