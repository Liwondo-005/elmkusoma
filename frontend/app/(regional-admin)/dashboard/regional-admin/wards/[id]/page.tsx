"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { Building2, MapPinned } from "lucide-react"
import { useTranslations } from "next-intl"
import { regionalAdminApi, type WardDetail } from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, StatCard,
} from "@/components/dashboard/regional-admin/ui"

function errMsg(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback
}

function isAccessError(e: unknown): boolean {
  return e instanceof Error && /API (403|404)/.test(e.message)
}

export default function RegionalWardDetailPage() {
  const t = useTranslations("regionalAdmin")
  const ts = useTranslations("status")
  const params = useParams() as { id: string }
  const id = params?.id ?? ""

  const [ward, setWard] = useState<WardDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    if (!id) {
      setWard(null)
      setError(t("wardDetail.notFoundError"))
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const data = await regionalAdminApi.getWard(id)
      setWard(data)
    } catch (e) {
      setWard(null)
      if (isAccessError(e)) {
        setError(t("wardDetail.accessError"))
      } else {
        setError(errMsg(e, t("wardDetail.loadError")))
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
        <PageHeader title={t("wardDetail.loadingTitle")} description={t("wardDetail.loadingDescription")} />
        <LoadingState label={t("wardDetail.loadingLabel")} />
      </div>
    )
  }

  if (error || !ward) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title={t("wardDetail.errorTitle")} description={t("wardDetail.errorDescription")} />
        <ErrorState message={error ?? errMsg(new Error("Ward not found"), t("wardDetail.loadError"))} onRetry={load} />
      </div>
    )
  }

  return (
    <div className="space-y-6 pb-8" data-testid="ward-detail-page">
      <PageHeader
        title={ward.name}
        description={`${ward.code} · ${ward.districtName} · ${ward.regionName ?? t("wardDetail.noRegion")}`}
        actions={
          <div className="flex items-center gap-2">
            <Chip tone={ward.isActive ? "success" : "danger"}>
              {ward.isActive ? ts("active") : ts("inactive")}
            </Chip>
            <Link
              href="/dashboard/regional-admin/wards"
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
            >
              {t("wardDetail.backLink")}
            </Link>
          </div>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label={t("wardDetail.statInstitutions")} value={ward.institutionCount} hint={ward.districtName} />
        <StatCard label={t("wardDetail.statDistrict")} value={ward.districtName} hint={ward.districtCode} />
        <StatCard label={t("wardDetail.statRegion")} value={ward.regionName ?? "—"} hint={ward.regionCode ?? undefined} />
        <StatCard label={t("wardDetail.statWardCode")} value={ward.code} />
      </div>

      <div>
        <h2 className="mb-3 text-sm font-semibold text-foreground">{t("wardDetail.institutionsHeading")}</h2>
        {ward.institutions.length === 0 ? (
          <EmptyState
            icon={<Building2 className="size-10" />}
            title={t("wardDetail.emptyTitle")}
            hint={t("wardDetail.emptyHint")}
          />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {ward.institutions.map((inst) => (
              <div
                key={inst.id}
                className="rounded-2xl border border-border bg-card p-4 shadow-sm transition-shadow hover:shadow-md"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-foreground">{inst.name}</p>
                    <p className="truncate text-xs text-muted-foreground">{inst.code}</p>
                  </div>
                  <Chip tone={inst.isActive ? "success" : "danger"}>
                    {inst.isActive ? ts("active") : ts("inactive")}
                  </Chip>
                </div>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                  <Chip tone="info">{inst.type}</Chip>
                  <Link
                    href={`/dashboard/regional-admin/institutions/${inst.id}`}
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

      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <MapPinned className="size-4" />
        {t("wardDetail.scopeFootnote")}
      </div>
    </div>
  )
}
