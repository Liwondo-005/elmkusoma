"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { Building2, MapPinned } from "lucide-react"
import { regionalAdminApi, type WardDetail } from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, StatCard,
} from "@/components/dashboard/regional-admin/ui"

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : "Unable to load this ward."
}

function isAccessError(e: unknown): boolean {
  return e instanceof Error && /API (403|404)/.test(e.message)
}

export default function RegionalWardDetailPage() {
  const params = useParams() as { id: string }
  const id = params?.id ?? ""

  const [ward, setWard] = useState<WardDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    if (!id) {
      setWard(null)
      setError("This ward could not be found.")
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
        setError(
          "This ward is outside your jurisdiction or does not exist. Contact your administrator if you believe this is a mistake.",
        )
      } else {
        setError(errMsg(e))
      }
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title="Ward" description="Loading ward…" />
        <LoadingState label="Loading ward…" />
      </div>
    )
  }

  if (error || !ward) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title="Ward" description="Ward detail" />
        <ErrorState message={error ?? errMsg(new Error("Ward not found"))} onRetry={load} />
      </div>
    )
  }

  return (
    <div className="space-y-6 pb-8" data-testid="ward-detail-page">
      <PageHeader
        title={ward.name}
        description={`${ward.code} · ${ward.districtName} · ${ward.regionName ?? "No region"}`}
        actions={
          <div className="flex items-center gap-2">
            <Chip tone={ward.isActive ? "success" : "danger"}>
              {ward.isActive ? "Active" : "Inactive"}
            </Chip>
            <Link
              href="/dashboard/regional-admin/wards"
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
            >
              ← All wards
            </Link>
          </div>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Institutions" value={ward.institutionCount} hint={ward.districtName} />
        <StatCard label="District" value={ward.districtName} hint={ward.districtCode} />
        <StatCard label="Region" value={ward.regionName ?? "—"} hint={ward.regionCode ?? undefined} />
        <StatCard label="Ward code" value={ward.code} />
      </div>

      <div>
        <h2 className="mb-3 text-sm font-semibold text-foreground">Institutions in this ward</h2>
        {ward.institutions.length === 0 ? (
          <EmptyState
            icon={<Building2 className="size-10" />}
            title="No institutions linked to this ward yet"
            hint="Institutions are linked to wards once ward-level addresses are collected. Until then they appear on the district page only."
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
                    {inst.isActive ? "Active" : "Inactive"}
                  </Chip>
                </div>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                  <Chip tone="info">{inst.type}</Chip>
                  <Link
                    href={`/dashboard/regional-admin/institutions/${inst.id}`}
                    className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-foreground hover:bg-muted"
                  >
                    Open →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <MapPinned className="size-4" />
        Ward geography is scoped to your jurisdiction — institutions and counts shown here are
        filtered server-side.
      </div>
    </div>
  )
}
