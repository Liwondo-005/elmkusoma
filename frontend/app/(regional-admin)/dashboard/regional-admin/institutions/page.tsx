"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { Building2, School } from "lucide-react"
import {
  regionalAdminApi,
  type DistrictInfo,
  type RegionalInstitution,
} from "@/lib/regional-admin-api"
import { Chip, ErrorState, PageHeader, PagedList } from "@/components/dashboard/regional-admin/ui"

const TYPES = ["SCHOOL", "COLLEGE", "UNIVERSITY", "TVET", "OTHER"]

function renderItem(item: RegionalInstitution) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm transition-shadow hover:shadow-md">
      <div className="flex items-start gap-4">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
          <Building2 className="size-5 text-primary" />
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
            <Chip tone="info">{item.type}</Chip>
            <span className="truncate text-xs text-muted-foreground">
              {[item.districtName, item.regionName].filter(Boolean).join(" · ") || "No location"}
            </span>
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
              <span>
                Teachers <span className="font-semibold tabular-nums text-foreground">{item.teacherCount ?? "—"}</span>
              </span>
              <span>
                Learners <span className="font-semibold tabular-nums text-foreground">{item.studentCount ?? "—"}</span>
              </span>
            </div>
            <Link
              href={`/dashboard/regional-admin/institutions/${item.id}`}
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

export default function RegionalInstitutionsPage() {
  const [districts, setDistricts] = useState<DistrictInfo[]>([])
  const [filtersLoading, setFiltersLoading] = useState(true)
  const [filtersError, setFiltersError] = useState<string | null>(null)
  const [districtId, setDistrictId] = useState("")
  const [type, setType] = useState("")

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
      return regionalAdminApi.listInstitutions({
        ...params,
        districtId: districtId || undefined,
        type: type || undefined,
      })
    },
    [districtId, type],
  )

  const showDistrictFilter = districts.length > 0

  const toolbar = (
    <>
      {showDistrictFilter ? (
        <select
          value={districtId}
          onChange={(e) => setDistrictId(e.target.value)}
          aria-label="Filter institutions by district"
          className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">All districts</option>
          {districts.map((d) => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>
      ) : filtersLoading ? (
        <select
          disabled
          aria-label="Loading districts"
          className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm text-muted-foreground opacity-60 outline-none"
        >
          <option>Loading districts…</option>
        </select>
      ) : null}

      <select
        value={type}
        onChange={(e) => setType(e.target.value)}
        aria-label="Filter institutions by type"
        className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
      >
        <option value="">All types</option>
        {TYPES.map((t) => (
          <option key={t} value={t}>{t}</option>
        ))}
      </select>
    </>
  )

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Schools & Institutions"
        description="Every school, college, university and TVET institution inside your jurisdiction — filter by district or type, then open one to review its governance."
      />

      {filtersError && <ErrorState message={filtersError} onRetry={loadFilters} />}

      <PagedList<RegionalInstitution>
        key={`${districtId}|${type}`}
        fetcher={fetcher}
        renderItem={renderItem}
        searchPlaceholder="Search institutions by name or code…"
        emptyTitle="No institutions found in your jurisdiction"
        emptyHint="Try a different search term, or clear the district and type filters to see every institution you can access."
        emptyIcon={<School className="size-10" />}
        toolbar={toolbar}
      />
    </div>
  )
}
