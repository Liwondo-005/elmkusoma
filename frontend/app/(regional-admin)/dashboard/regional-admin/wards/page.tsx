"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { MapPinned } from "lucide-react"
import { useTranslations } from "next-intl"
import {
  regionalAdminApi,
  type DistrictInfo,
  type WardSummary,
} from "@/lib/regional-admin-api"
import { Chip, ErrorState, PageHeader, PagedList } from "@/components/dashboard/regional-admin/ui"

function renderItem(
  item: WardSummary,
  t: ReturnType<typeof useTranslations>,
  ts: ReturnType<typeof useTranslations>,
) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm transition-shadow hover:shadow-md">
      <div className="flex items-start gap-4">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
          <MapPinned className="size-5 text-primary" />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-foreground">{item.name}</p>
              <p className="truncate text-xs text-muted-foreground">{item.code}</p>
            </div>
            <Chip tone={item.isActive ? "success" : "danger"}>
              {item.isActive ? ts("active") : ts("inactive")}
            </Chip>
          </div>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Chip tone="info">{item.districtName ?? t("wards.noDistrict")}</Chip>
            <span className="truncate text-xs text-muted-foreground">
              {[item.regionName].filter(Boolean).join(" · ") || t("wards.noRegion")}
            </span>
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <span className="text-xs text-muted-foreground">
              {t("wards.institutionsLabel")}{" "}
              <span className="font-semibold tabular-nums text-foreground">{item.institutionCount}</span>
            </span>
            <Link
              href={`/dashboard/regional-admin/wards/${item.id}`}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
            >
              {t("shared.open")} →
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function RegionalWardsPage() {
  const t = useTranslations("regionalAdmin")
  const ts = useTranslations("status")
  const [districts, setDistricts] = useState<DistrictInfo[]>([])
  const [filtersLoading, setFiltersLoading] = useState(true)
  const [filtersError, setFiltersError] = useState<string | null>(null)
  const [districtId, setDistrictId] = useState("")

  const loadFilters = useCallback(async () => {
    setFiltersLoading(true)
    setFiltersError(null)
    try {
      const regions = await regionalAdminApi.getRegions()
      if (regions.length === 0) {
        setDistricts([])
        return
      }
      const results = await Promise.allSettled(
        regions.map((r) => regionalAdminApi.getDistricts(r.id)),
      )
      const merged: DistrictInfo[] = []
      let firstError: unknown = null
      let loaded = 0
      for (const res of results) {
        if (res.status === "fulfilled") {
          loaded += 1
          merged.push(...res.value)
        } else if (firstError === null) {
          firstError = res.reason
        }
      }
      if (loaded === 0 && firstError !== null) throw firstError
      const seen = new Set<string>()
      const unique = merged
        .filter((d) => {
          if (seen.has(d.id)) return false
          seen.add(d.id)
          return true
        })
        .sort((a, b) => a.name.localeCompare(b.name))
      setDistricts(unique)
    } catch (e) {
      setDistricts([])
      setFiltersError(e instanceof Error ? e.message : t("wards.loadDistrictsError"))
    } finally {
      setFiltersLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadFilters()
  }, [loadFilters])

  const fetcher = useCallback(
    (params: { page: number; size: number; search?: string }) => {
      return regionalAdminApi.listWards({
        ...params,
        districtId: districtId || undefined,
      })
    },
    [districtId],
  )

  const toolbar = (
    <select
      value={districtId}
      onChange={(e) => setDistrictId(e.target.value)}
      aria-label={t("wards.filterAriaLabel")}
      className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
    >
      <option value="">{t("wards.allDistricts")}</option>
      {districts.map((d) => (
        <option key={d.id} value={d.id}>{d.name}</option>
      ))}
    </select>
  )

  return (
    <div className="space-y-6 pb-8" data-testid="wards-page">
      <PageHeader
        title={t("wards.title")}
        description={t("wards.description")}
      />

      {filtersError && <ErrorState message={filtersError} onRetry={loadFilters} />}

      <PagedList<WardSummary>
        fetcher={fetcher}
        renderItem={(item) => renderItem(item, t, ts)}
        searchPlaceholder={t("wards.searchPlaceholder")}
        emptyTitle={t("wards.emptyTitle")}
        emptyHint={t("wards.emptyHint")}
        emptyIcon={<MapPinned className="size-10" />}
        toolbar={filtersLoading ? null : districts.length > 0 ? toolbar : null}
      />
    </div>
  )
}
