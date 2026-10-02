"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { Building2 } from "lucide-react"
import { useTranslations } from "next-intl"
import { regionalAdminApi, type DistrictDetail } from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, Percent, StatCard,
} from "@/components/dashboard/regional-admin/ui"

function errMsg(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback
}

function isAccessError(e: unknown): boolean {
  return e instanceof Error && /API (403|404)/.test(e.message)
}

function pct(value: number | null): string {
  return value === null || value === undefined ? "—" : `${value.toFixed(1)}%`
}

export default function RegionalDistrictDetailPage() {
  const t = useTranslations("regionalAdmin")
  const ts = useTranslations("status")
  const params = useParams() as { id: string }
  const id = params?.id ?? ""

  const [district, setDistrict] = useState<DistrictDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    if (!id) {
      setDistrict(null)
      setError(t("districtDetail.notFoundError"))
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const data = await regionalAdminApi.getDistrict(id)
      setDistrict(data)
    } catch (e) {
      setDistrict(null)
      if (isAccessError(e)) {
        setError(t("districtDetail.accessError"))
      } else {
        setError(errMsg(e, t("districtDetail.loadError")))
      }
    } finally {
      setLoading(false)
    }
  }, [id, t])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title={t("districtDetail.loadingTitle")} description={t("districtDetail.loadingDescription")} />
        <LoadingState label={t("districtDetail.loadingLabel")} />
      </div>
    )
  }

  if (error || !district) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title={t("districtDetail.errorTitle")} description={t("districtDetail.errorDescription")} />
        <ErrorState
          message={error ?? t("districtDetail.loadError")}
          onRetry={load}
        />
        <div className="flex justify-start">
          <Link
            href="/dashboard/regional-admin/districts"
            className="text-sm font-medium text-primary hover:underline"
          >
            {t("districtDetail.backLink")}
          </Link>
        </div>
      </div>
    )
  }

  const institutions = district.institutions ?? []

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={district.name}
        description={`${district.code} · ${district.regionName}`}
        actions={
          <Link
            href="/dashboard/regional-admin/districts"
            className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-muted"
          >
            {t("districtDetail.backLink")}
          </Link>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <Chip tone={district.isActive ? "success" : "danger"}>
          {district.isActive ? ts("active") : ts("inactive")}
        </Chip>
        <Chip tone="info">{district.regionName}</Chip>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
        <StatCard label={t("districtDetail.statInstitutions")} value={district.institutionCount} />
        <StatCard label={t("districtDetail.statSchools")} value={district.schoolCount} />
        <StatCard label={t("districtDetail.statTeachers")} value={district.teacherCount} />
        <StatCard label={t("districtDetail.statLearners")} value={district.learnerCount} />
        <StatCard label={t("districtDetail.statAttendance")} value={pct(district.attendanceRate)} />
        <StatCard label={t("districtDetail.statPerformance")} value={pct(district.averagePerformance)} />
        <StatCard label={t("districtDetail.statCurriculum")} value={pct(district.curriculumProgress)} />
      </div>

      <section className="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
        <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
          <h2 className="text-sm font-bold text-foreground">{t("districtDetail.institutionsHeading", { name: district.name })}</h2>
          <span className="text-xs text-muted-foreground">{t("districtDetail.totalLabel", { count: institutions.length.toLocaleString() })}</span>
        </div>

        {institutions.length === 0 ? (
          <div className="p-5">
            <EmptyState
              icon={<Building2 className="size-10" />}
              title={t("districtDetail.emptyTitle")}
              hint={t("districtDetail.emptyHint")}
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left">
                  <th className="px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colName")}</th>
                  <th className="px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colCode")}</th>
                  <th className="px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colType")}</th>
                  <th className="px-5 py-3 text-right text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colTeachers")}</th>
                  <th className="px-5 py-3 text-right text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colLearners")}</th>
                  <th className="px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colStatus")}</th>
                  <th className="px-5 py-3 text-right text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("districtDetail.colActions")}</th>
                </tr>
              </thead>
              <tbody>
                {institutions.map((inst) => (
                  <tr key={inst.id} className="border-b border-border last:border-0 hover:bg-muted/40">
                    <td className="max-w-64 px-5 py-3">
                      <p className="truncate font-medium text-foreground">{inst.name}</p>
                      <p className="truncate text-xs text-muted-foreground">{inst.districtName}</p>
                    </td>
                    <td className="px-5 py-3 text-muted-foreground">{inst.code}</td>
                    <td className="px-5 py-3"><Chip tone="info">{inst.type}</Chip></td>
                    <td className="px-5 py-3 text-right tabular-nums text-foreground">{inst.teacherCount ?? "—"}</td>
                    <td className="px-5 py-3 text-right tabular-nums text-foreground">{inst.studentCount ?? "—"}</td>
                    <td className="px-5 py-3">
                      <Chip tone={inst.isActive ? "success" : "danger"}>
                        {inst.isActive ? ts("active") : ts("inactive")}
                      </Chip>
                    </td>
                    <td className="px-5 py-3 text-right">
                      <Link
                        href={`/dashboard/regional-admin/institutions/${inst.id}`}
                        className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
                      >
                        {t("districtDetail.governanceLink")} →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
