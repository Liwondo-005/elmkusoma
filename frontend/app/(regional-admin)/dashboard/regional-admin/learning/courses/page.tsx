"use client"

import { BookOpen } from "lucide-react"
import { useTranslations } from "next-intl"
import { regionalAdminApi, type CourseSummary } from "@/lib/regional-admin-api"
import { Chip, PageHeader, PagedList, formatDate } from "@/components/dashboard/regional-admin/ui"

const fetchCourses = (params: { page: number; size: number; search?: string }) =>
  regionalAdminApi.listCourses(params)

function renderItem(
  item: CourseSummary,
  t: ReturnType<typeof useTranslations>,
) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-foreground">{item.title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{item.institutionName ?? t("courses.noInstitution")}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Chip tone={item.isPublished ? "success" : "muted"}>
            {item.isPublished ? t("courses.published") : t("courses.draft")}
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
  const t = useTranslations("regionalAdmin")
  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={t("courses.title")}
        description={t("courses.description")}
      />

      <PagedList
        fetcher={fetchCourses}
        renderItem={(item) => renderItem(item, t)}
        searchPlaceholder={t("courses.searchPlaceholder")}
        emptyTitle={t("courses.emptyTitle")}
        emptyHint={t("courses.emptyHint")}
        emptyIcon={<BookOpen className="size-10" />}
      />
    </div>
  )
}
