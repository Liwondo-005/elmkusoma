"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { MapPinned } from "lucide-react"
import {
  regionalAdminApi,
  type DistrictInfo,
  type WardSummary,
} from "@/lib/regional-admin-api"
import { Chip, ErrorState, PageHeader, PagedList } from "@/components/dashboard/regional-admin/ui"

function renderItem(item: WardSummary) {
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
              {item.isActive ? "Active" : "Inactive"}
            </Chip>
          </div>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Chip tone="info">{item.districtName ?? "No district"}</Chip>
            <span className="truncate text-xs text-muted-foreground">
              {[item.regionName].filter(Boolean).join(" · ") || "No region"}
            </span>
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <span className="text-xs text-muted-foreground">
              Institutions{" "}
              <span className="font-semibold tabular-nums text-foreground">{item.institutionCount}</span>
            </span>
            <Link
              href={`/dashboard/regional-admin/wards/${item.id}`}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
            >
              Open →
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function RegionalWardsPage() {
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
      setFiltersError(e instanceof Error ? e.message : "Unable to load districts.")
    } finally {
      setFiltersLoading(false)
    }
  }, [])

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
      aria-label="Filter wards by district"
      className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
    >
      <option value="">All districts</option>
      {districts.map((d) => (
        <option key={d.id} value={d.id}>{d.name}</option>
      ))}
    </select>
  )

  return (
    <div className="space-y-6 pb-8" data-testid="wards-page">
      <PageHeader
        title="Wards"
        description="Ward-level geography inside your jurisdiction — the layer between district and institution."
      />

      {filtersError && <ErrorState message={filtersError} onRetry={loadFilters} />}

      <PagedList<WardSummary>
        fetcher={fetcher}
        renderItem={renderItem}
        searchPlaceholder="Search wards by name or code…"
        emptyTitle="No wards found"
        emptyHint="Wards for your jurisdiction have not been added yet, or none match your search."
        emptyIcon={<MapPinned className="size-10" />}
        toolbar={filtersLoading ? null : districts.length > 0 ? toolbar : null}
      />
    </div>
  )
}
