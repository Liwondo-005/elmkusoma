"use client"

import { Library } from "lucide-react"
import { regionalAdminApi, type ResourceSummary } from "@/lib/regional-admin-api"
import { Chip, PageHeader, PagedList, formatDate } from "@/components/dashboard/regional-admin/ui"

const fetchResources = (params: { page: number; size: number; search?: string }) =>
  regionalAdminApi.listResources(params)

function formatFileSize(bytes: number | null): string {
  if (bytes === null || bytes === undefined || Number.isNaN(bytes) || bytes < 0) return "—"
  if (bytes < 1024) return `${bytes} B`
  const units = ["KB", "MB", "GB", "TB"]
  let value = bytes
  let i = -1
  do {
    value /= 1024
    i += 1
  } while (value >= 1024 && i < units.length - 1)
  return `${value.toFixed(1)} ${units[i]}`
}

function renderItem(item: ResourceSummary) {
  const kind = [item.type, item.mimeType].filter(Boolean).join(" · ")
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-foreground">{item.title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{item.institutionName ?? "No institution"}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <span className="text-xs tabular-nums text-muted-foreground">{formatFileSize(item.fileSize)}</span>
          <span className="text-xs text-muted-foreground">{formatDate(item.createdAt)}</span>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        {kind && <Chip tone="info">{kind}</Chip>}
        {item.visibility && <Chip tone="default">{item.visibility}</Chip>}
      </div>
    </div>
  )
}

export default function RegionalResourcesPage() {
  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Learning Resources"
        description="Learning resources uploaded by institutions inside your jurisdiction — every file is scoped to your region or district."
      />

      <PagedList
        fetcher={fetchResources}
        renderItem={renderItem}
        searchPlaceholder="Search resources by title…"
        emptyTitle="No learning resources found in your jurisdiction"
        emptyHint="Try a different search term to find resources available in your region or district."
        emptyIcon={<Library className="size-10" />}
      />
    </div>
  )
}
