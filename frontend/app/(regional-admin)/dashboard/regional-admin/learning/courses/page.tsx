"use client"

import { BookOpen } from "lucide-react"
import { regionalAdminApi, type CourseSummary } from "@/lib/regional-admin-api"
import { Chip, PageHeader, PagedList, formatDate } from "@/components/dashboard/regional-admin/ui"

const fetchCourses = (params: { page: number; size: number; search?: string }) =>
  regionalAdminApi.listCourses(params)

function renderItem(item: CourseSummary) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-foreground">{item.title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{item.institutionName ?? "No institution"}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Chip tone={item.isPublished ? "success" : "muted"}>
            {item.isPublished ? "Published" : "Draft"}
          </Chip>
          <span className="text-xs text-muted-foreground">{formatDate(item.createdAt)}</span>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {item.level && <Chip tone="info">{item.level}</Chip>}
        {item.category && <Chip tone="warning">{item.category}</Chip>}
        {item.subject && <Chip tone="default">{item.subject}</Chip>}
      </div>
    </div>
  )
}

export default function RegionalCoursesPage() {
  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Courses"
        description="Courses published by institutions inside your jurisdiction — every course is scoped to your region or district."
      />

      <PagedList
        fetcher={fetchCourses}
        renderItem={renderItem}
        searchPlaceholder="Search courses by title…"
        emptyTitle="No courses found in your jurisdiction"
        emptyHint="Try a different search term to find courses available in your region or district."
        emptyIcon={<BookOpen className="size-10" />}
      />
    </div>
  )
}
