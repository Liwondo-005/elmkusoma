"use client"

import { useCallback, useEffect, useState } from "react"
import { Briefcase } from "lucide-react"
import { useTranslations } from "next-intl"
import { regionalAdminApi, type DistrictInfo, type PersonSummary } from "@/lib/regional-admin-api"
import { Chip, ErrorState, PageHeader, PagedList, formatDate } from "@/components/dashboard/regional-admin/ui"

function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return "?"
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

function renderItem(
  item: PersonSummary,
  t: ReturnType<typeof useTranslations>,
  ts: ReturnType<typeof useTranslations>,
) {
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
              {item.isActive ? ts("active") : ts("inactive")}
            </Chip>
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            {item.institutionName ? (
              <Chip>{item.institutionName}</Chip>
            ) : (
              <span className="text-xs text-muted-foreground">{t("staff.noInstitution")}</span>
            )}
            <Chip tone="info">{item.role}</Chip>
            <span className="text-xs text-muted-foreground">{t("staff.joinedLabel")} {formatDate(item.createdAt)}</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function RegionalEducationStaffPage() {
  const t = useTranslations("regionalAdmin")
  const ts = useTranslations("status")
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
      setDistrictsError(e instanceof Error ? e.message : t("staff.loadDistrictsError"))
    } finally {
      setDistrictsLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadDistricts()
  }, [loadDistricts])

  const fetcher = useCallback(
    (params: { page: number; size: number; search?: string }) => {
      if (districtId) return regionalAdminApi.listEducationStaff({ ...params, districtId })
      return regionalAdminApi.listEducationStaff(params)
    },
    [districtId],
  )

  const showDistrictFilter = districts.length > 0

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={t("staff.title")}
        description={t("staff.description")}
      />

      {districtsError && <ErrorState message={districtsError} onRetry={loadDistricts} />}

      <PagedList
        key={districtId || "all"}
        fetcher={fetcher}
        renderItem={(item) => renderItem(item, t, ts)}
        searchPlaceholder={t("staff.searchPlaceholder")}
        emptyTitle={t("staff.emptyTitle")}
        emptyHint={t("staff.emptyHint")}
        emptyIcon={<Briefcase className="size-10" />}
        toolbar={
          showDistrictFilter ? (
            <select
              value={districtId}
              onChange={(e) => setDistrictId(e.target.value)}
              aria-label={t("staff.filterAriaLabel")}
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              <option value="">{t("staff.allDistricts")}</option>
              {districts.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          ) : districtsLoading ? (
            <select
              disabled
              aria-label={t("staff.loadingDistrictsAriaLabel")}
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm text-muted-foreground opacity-60 outline-none"
            >
              <option>{t("staff.loadingDistricts")}</option>
            </select>
          ) : null
        }
      />
    </div>
  )
}
