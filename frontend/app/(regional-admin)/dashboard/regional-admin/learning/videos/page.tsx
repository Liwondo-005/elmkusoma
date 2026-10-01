"use client"

import { Film } from "lucide-react"
import { regionalAdminApi, type VideoTutorialSummary } from "@/lib/regional-admin-api"
import { Chip, PageHeader, PagedList, formatDate } from "@/components/dashboard/regional-admin/ui"

const fetchVideos = (params: { page: number; size: number; search?: string }) =>
  regionalAdminApi.listVideoTutorials(params)

function formatDuration(seconds: number | null): string {
  if (seconds === null || seconds === undefined || Number.isNaN(seconds) || seconds <= 0) return "—"
  const total = Math.floor(seconds)
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const mm = h > 0 ? String(m).padStart(2, "0") : String(m)
  const ss = String(s).padStart(2, "0")
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`
}

function statusTone(status: string): "success" | "warning" | "danger" | "muted" {
  const s = status.toUpperCase()
  if (s === "COMPLETED" || s === "READY") return "success"
  if (s === "PROCESSING") return "warning"
  if (s === "FAILED") return "danger"
  return "muted"
}

function renderItem(item: VideoTutorialSummary) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-foreground">{item.title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{item.institutionName ?? "No institution"}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Chip tone={statusTone(item.status)}>{item.status}</Chip>
          <span className="text-xs tabular-nums text-muted-foreground">{formatDate(item.createdAt)}</span>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <span className="text-xs text-muted-foreground">Duration {formatDuration(item.durationSeconds)}</span>
      </div>
    </div>
  )
}

export default function RegionalVideosPage() {
  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Video Tutorials"
        description="Video tutorials created by institutions inside your jurisdiction — every video is scoped to your region or district."
      />

      <PagedList
        fetcher={fetchVideos}
        renderItem={renderItem}
        searchPlaceholder="Search video tutorials by title…"
        emptyTitle="No video tutorials found in your jurisdiction"
        emptyHint="Try a different search term to find video tutorials available in your region or district."
        emptyIcon={<Film className="size-10" />}
      />
    </div>
  )
}
