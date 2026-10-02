"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { MapPin } from "lucide-react"
import { useTranslations } from "next-intl"
import {
  regionalAdminApi,
  type DistrictInfo,
  type DistrictSummary,
  type RegionDetail,
  type RegionInfo,
} from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, Percent, StatCard,
} from "@/components/dashboard/regional-admin/ui"

interface DistrictRow {
  id: string
  name: string
  code: string
  isActive: boolean
  institutionCount: number | null
  schoolCount: number | null
  teacherCount: number | null
  learnerCount: number | null
  attendanceRate: number | null
  averagePerformance: number | null
}

function errMsg(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback
}

function pct(value: number | null): string {
  return value === null || value === undefined ? "—" : `${value.toFixed(1)}%`
}

function buildRows(districts: DistrictInfo[], detail: RegionDetail | null): DistrictRow[] {
  return districts.map((d) => {
    const stats: DistrictSummary | undefined = detail?.districts.find((x) => x.id === d.id)
    return {
      id: d.id,
      name: d.name,
      code: d.code,
      isActive: d.isActive,
      institutionCount: stats?.institutionCount ?? d.institutionCount,
      schoolCount: stats?.schoolCount ?? null,
      teacherCount: stats?.teacherCount ?? d.teacherCount,
      learnerCount: stats?.learnerCount ?? d.studentCount,
      attendanceRate: stats?.attendanceRate ?? null,
      averagePerformance: stats?.averagePerformance ?? null,
    }
  })
}

export default function RegionalDistrictsPage() {
  const t = useTranslations("regionalAdmin")
  const ts = useTranslations("status")
  const [regions, setRegions] = useState<RegionInfo[] | null>(null)
  const [regionsError, setRegionsError] = useState<string | null>(null)
  const [regionId, setRegionId] = useState("")

  const [districts, setDistricts] = useState<DistrictInfo[] | null>(null)
  const [districtsError, setDistrictsError] = useState<string | null>(null)
  const [districtsLoading, setDistrictsLoading] = useState(false)

  const [detail, setDetail] = useState<RegionDetail | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [refresh, setRefresh] = useState(0)

  const loadRegions = useCallback(async () => {
    setRegionsError(null)
    setRegions(null)
    try {
      const list = await regionalAdminApi.getRegions()
      setRegions(list)
      setRegionId((prev) => (prev && list.some((r) => r.id === prev) ? prev : list[0]?.id ?? ""))
    } catch (e) {
      setRegionsError(e instanceof Error ? e.message : t("districts.loadRegionsError"))
    }
  }, [t])

  useEffect(() => {
    loadRegions()
  }, [loadRegions])

  useEffect(() => {
    if (!regionId) {
      setDistricts(null)
      setDetail(null)
      setDistrictsLoading(false)
      setDetailLoading(false)
      return
    }
    let active = true
    setDistricts(null)
    setDistrictsError(null)
    setDistrictsLoading(true)
    setDetail(null)
    setDetailError(null)
    setDetailLoading(true)
    Promise.allSettled([
      regionalAdminApi.getDistricts(regionId),
      regionalAdminApi.getRegionDetail(regionId),
    ]).then(([districtsRes, detailRes]) => {
      if (!active) return
      if (districtsRes.status === "fulfilled") setDistricts(districtsRes.value)
      else setDistrictsError(errMsg(districtsRes.reason, t("districts.loadDistrictsError")))
      if (detailRes.status === "fulfilled") setDetail(detailRes.value)
      else setDetailError(errMsg(detailRes.reason, t("districts.loadDistrictsError")))
      setDistrictsLoading(false)
      setDetailLoading(false)
    })
    return () => {
      active = false
    }
  }, [regionId, refresh, t])

  const retry = () => setRefresh((k) => k + 1)

  if (regionsError) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader
          title={t("districts.title")}
          description={t("districts.description")}
        />
        <ErrorState message={regionsError} onRetry={loadRegions} />
      </div>
    )
  }

  if (regions === null) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader
          title={t("districts.title")}
          description={t("districts.description")}
        />
        <LoadingState label={t("districts.loadingRegions")} />
      </div>
    )
  }

  if (regions.length === 0) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader
          title={t("districts.title")}
          description={t("districts.description")}
        />
        <EmptyState
          icon={<MapPin className="size-10" />}
          title={t("districts.noRegionAccessTitle")}
          hint={t("districts.noRegionAccessHint")}
        />
      </div>
    )
  }

  const rows = districts ? buildRows(districts, detail) : []
  const showRegionSwitcher = regions.length > 1

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={t("districts.title")}
        description={t("districts.description")}
        actions={
          showRegionSwitcher ? (
            <label className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="text-[10px] font-bold uppercase tracking-widest">{t("districts.regionLabel")}</span>
              <select
                value={regionId}
                onChange={(e) => setRegionId(e.target.value)}
                aria-label={t("districts.switchRegionAriaLabel")}
                className="rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
              >
                {regions.map((r) => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
            </label>
          ) : null
        }
      />

      {detailError && <ErrorState message={detailError} onRetry={retry} />}

      {detailLoading ? (
        <div className="h-24 animate-pulse rounded-2xl border border-border bg-card" />
      ) : detail ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <StatCard label={t("districts.statDistricts")} value={detail.districtCount} hint={`${detail.name} · ${detail.code}`} />
          <StatCard label={t("districts.statInstitutions")} value={detail.institutionCount} />
          <StatCard label={t("districts.statTeachers")} value={detail.teacherCount} />
          <StatCard label={t("districts.statLearners")} value={detail.learnerCount} />
          <StatCard label={t("districts.statAttendance")} value={pct(detail.attendanceRate)} />
          <StatCard label={t("districts.statPerformance")} value={pct(detail.averagePerformance)} />
        </div>
      ) : null}

      {districtsLoading ? (
        <LoadingState label={t("districts.loadingDistricts")} />
      ) : districtsError ? (
        <ErrorState message={districtsError} onRetry={retry} />
      ) : rows.length === 0 ? (
        <EmptyState
          title={t("districts.emptyTitle")}
          hint={t("districts.emptyHint")}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {rows.map((d) => (
            <div
              key={d.id}
              className="rounded-2xl border border-border bg-card p-5 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <h3 className="truncate font-semibold text-foreground">{d.name}</h3>
                  <p className="text-xs text-muted-foreground">{d.code}</p>
                </div>
                <Chip tone={d.isActive ? "success" : "danger"}>
                  {d.isActive ? ts("active") : ts("inactive")}
                </Chip>
              </div>

              <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-3">
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districts.statInstitutions")}</dt>
                  <dd className="mt-0.5 text-sm font-semibold tabular-nums text-foreground">{d.institutionCount ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districts.statSchools")}</dt>
                  <dd className="mt-0.5 text-sm font-semibold tabular-nums text-foreground">{d.schoolCount ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districts.statTeachers")}</dt>
                  <dd className="mt-0.5 text-sm font-semibold tabular-nums text-foreground">{d.teacherCount ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districts.statLearners")}</dt>
                  <dd className="mt-0.5 text-sm font-semibold tabular-nums text-foreground">{d.learnerCount ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districts.statAttendance")}</dt>
                  <dd className="mt-0.5 text-sm font-semibold text-foreground"><Percent value={d.attendanceRate} /></dd>
                </div>
                <div>
                  <dt className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districts.statPerformance")}</dt>
                  <dd className="mt-0.5 text-sm font-semibold text-foreground"><Percent value={d.averagePerformance} /></dd>
                </div>
              </dl>

              <div className="mt-4 flex justify-end">
                <Link
                  href={`/dashboard/regional-admin/districts/${d.id}`}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
                >
                  {t("shared.open")} →
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
