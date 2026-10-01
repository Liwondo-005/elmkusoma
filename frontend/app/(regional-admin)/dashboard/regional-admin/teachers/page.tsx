"use client"

import { useCallback, useEffect, useState } from "react"
import { Users } from "lucide-react"
import { regionalAdminApi, type DistrictInfo, type PersonSummary } from "@/lib/regional-admin-api"
import { Chip, ErrorState, PageHeader, PagedList, formatDate } from "@/components/dashboard/regional-admin/ui"

function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return "?"
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

function renderItem(item: PersonSummary) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex items-start gap-4">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">
          {initials(item.fullName)}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-foreground">{item.fullName}</p>
              <p className="truncate text-xs text-muted-foreground">{item.email}</p>
            </div>
            <Chip tone={item.isActive ? "success" : "danger"}>
              {item.isActive ? "Active" : "Inactive"}
            </Chip>
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            {item.institutionName ? (
              <Chip>{item.institutionName}</Chip>
            ) : (
              <span className="text-xs text-muted-foreground">No institution</span>
            )}
            <Chip tone="info">{item.role}</Chip>
            <span className="text-xs text-muted-foreground">Joined {formatDate(item.createdAt)}</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function RegionalTeachersPage() {
  const [districts, setDistricts] = useState<DistrictInfo[]>([])
  const [districtsLoading, setDistrictsLoading] = useState(true)
  const [districtsError, setDistrictsError] = useState<string | null>(null)
  const [districtId, setDistrictId] = useState("")

  const loadDistricts = useCallback(async () => {
    setDistrictsLoading(true)
    setDistrictsError(null)
    try {
      const regions = await regionalAdminApi.getRegions()
      if (regions.length === 0) {
        setDistricts([])
        return
      }
      setDistricts(await regionalAdminApi.getDistricts(regions[0].id))
    } catch (e) {
      setDistricts([])
      setDistrictsError(e instanceof Error ? e.message : "Unable to load districts.")
    } finally {
      setDistrictsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDistricts()
  }, [loadDistricts])

  const fetcher = useCallback(
    (params: { page: number; size: number; search?: string }) => {
      if (districtId) return regionalAdminApi.listTeachers({ ...params, districtId })
      return regionalAdminApi.listTeachers(params)
    },
    [districtId],
  )

  const showDistrictFilter = districts.length > 0

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Teachers"
        description="Teachers working at institutions inside your jurisdiction — every record is scoped to your region or district."
      />

      {districtsError && <ErrorState message={districtsError} onRetry={loadDistricts} />}

      <PagedList
        key={districtId || "all"}
        fetcher={fetcher}
        renderItem={renderItem}
        searchPlaceholder="Search teachers by name or email…"
        emptyTitle="No teachers found in your jurisdiction"
        emptyHint="Try a different search term, or clear the district filter to see every teacher you can access."
        emptyIcon={<Users className="size-10" />}
        toolbar={
          showDistrictFilter ? (
            <select
              value={districtId}
              onChange={(e) => setDistrictId(e.target.value)}
              aria-label="Filter teachers by district"
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              <option value="">All districts</option>
              {districts.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          ) : districtsLoading ? (
            <select
              disabled
              aria-label="Loading districts"
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm text-muted-foreground opacity-60 outline-none"
            >
              <option>Loading districts…</option>
            </select>
          ) : null
        }
      />
    </div>
  )
}
