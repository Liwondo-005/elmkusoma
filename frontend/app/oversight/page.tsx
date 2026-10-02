"use client"

import { useTranslations } from "next-intl"

import { useCallback, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import {
  Building2,
  Users,
  GraduationCap,
  MapPin,
  Activity,
  TrendingUp,
  AlertTriangle,
  Target,
  Video,
  BookOpen,
  FileBarChart,
  Layers,
  BookMarked,
  ChevronRight,
} from "lucide-react"
import {
  oversightApi,
  type OversightAttention,
  type OversightDashboard,
  type OversightDistrict,
  type OversightRegion,
  type OversightScope,
} from "@/lib/api"

export default function OversightOverviewPage() {
  const t = useTranslations("oversight")
  const { user, loading: authLoading } = useRequireAuth()

  const [regions, setRegions] = useState<OversightRegion[]>([])
  const [districts, setDistricts] = useState<OversightDistrict[]>([])
  const [selectedRegionId, setSelectedRegionId] = useState<string>("")
  const [selectedDistrictId, setSelectedDistrictId] = useState<string>("")
  const [stats, setStats] = useState<OversightDashboard | null>(null)
  const [attention, setAttention] = useState<OversightAttention | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const isAuthority =
    user?.role === "National Admin" ||
    user?.role === "Regional Admin" ||
    user?.role === "District Admin"

  const scope: OversightScope = useMemo(
    () => ({ regionId: selectedRegionId || null, districtId: selectedDistrictId || null }),
    [selectedRegionId, selectedDistrictId],
  )

  const loadScopeData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [dashboard, attentionData] = await Promise.all([
        oversightApi.dashboard(scope),
        oversightApi.attention(scope),
      ])
      setStats(dashboard)
      setAttention(attentionData)
    } catch {
      setError(t("overview.errorLoading"))
    } finally {
      setLoading(false)
    }
  }, [scope, t])

  // Initial data: regions list (for the scope selector) + own scope.
  useEffect(() => {
    if (authLoading || !user || !isAuthority) return
    oversightApi
      .regions()
      .then(setRegions)
      .catch(() => setRegions([]))
  }, [authLoading, user, isAuthority])

  // Regional admins receive exactly one region from the API (their own) —
  // pre-select it so the district drill-down is reachable.
  useEffect(() => {
    if (user?.role === "Regional Admin" && !selectedRegionId && regions.length === 1) {
      setSelectedRegionId(regions[0].id)
    }
  }, [user?.role, regions, selectedRegionId])

  useEffect(() => {
    if (authLoading || !user || !isAuthority) return
    loadScopeData()
  }, [authLoading, user, isAuthority, loadScopeData])

  // Districts follow the selected region (national scope selector §8).
  useEffect(() => {
    if (!selectedRegionId) {
      setDistricts([])
      return
    }
    let cancelled = false
    oversightApi
      .districts(selectedRegionId)
      .then((list) => {
        if (!cancelled) setDistricts(list)
      })
      .catch(() => {
        if (!cancelled) setDistricts([])
      })
    return () => {
      cancelled = true
    }
  }, [selectedRegionId])

  if (authLoading || (loading && !stats)) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("overview.loadingOversightDashboard")}</div>
      </div>
    )
  }

  if (!stats) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">{t("overview.educationOversight")}</h1>
          <p className="mt-2 text-muted-foreground">
            {error ?? t("overview.welcomeDashboardDataUnavailable", { p0: user?.name ?? "" })}
          </p>
        </div>
      </div>
    )
  }

  const jurisdictionLabel =
    stats.jurisdictionSummary?.type === "national"
      ? t("overview.national")
      : stats.jurisdictionSummary?.type === "region"
        ? t("overview.regional")
        : t("overview.district")

  const isNational = user?.role === "National Admin"
  const isRegional = user?.role === "Regional Admin"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <Activity className="size-5 text-primary" />
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="text-2xl font-bold text-foreground">
              {t("overview.educationOversightCommandCenter")}
            </h1>
            <p className="text-sm text-muted-foreground">
              {jurisdictionLabel} {t("overview.oversight")}
              {stats.jurisdictionSummary?.name || t("overview.overviewFallback")} — {user?.name}
            </p>
          </div>
        </div>

        {/* Scope selector (§8) — narrows within the caller's authority; the
            backend re-validates every scope on each request. */}
        {(isNational || isRegional) && (
          <div className="mt-4 flex flex-wrap items-end gap-3 border-t border-border pt-4">
            <div>
              <label htmlFor="scope-region" className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("overview.region")}
              </label>
              <select
                id="scope-region"
                value={selectedRegionId}
                onChange={(e) => {
                  setSelectedRegionId(e.target.value)
                  setSelectedDistrictId("")
                }}
                disabled={!isNational}
                className="h-10 w-56 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring disabled:opacity-60"
              >
                <option value="">{t("overview.allRegions")}</option>
                {regions.map((region) => (
                  <option key={region.id} value={region.id}>
                    {region.name} ({region.code})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="scope-district" className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("overview.district")}
              </label>
              <select
                id="scope-district"
                value={selectedDistrictId}
                onChange={(e) => setSelectedDistrictId(e.target.value)}
                className="h-10 w-56 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">{t("overview.allDistricts")}</option>
                {districts.map((district) => (
                  <option key={district.id} value={district.id}>
                    {district.name} ({district.code})
                  </option>
                ))}
              </select>
            </div>
            {(selectedRegionId || selectedDistrictId) && (
              <button
                type="button"
                onClick={() => {
                  setSelectedRegionId("")
                  setSelectedDistrictId("")
                }}
                className="h-10 rounded-lg border border-border px-3 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              >
                {t("overview.resetScope")}
              </button>
            )}
            <span className="ml-auto rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              {loading ? t("overview.loadingOversightDashboard") : t("overview.scopeApplied")}
            </span>
          </div>
        )}
      </div>

      {/* KPI cards (§11) — regions, districts, institutions, teachers,
          learners, courses, subjects plus the operational pulse. */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<MapPin className="size-5" />} label={t("overview.regions")} value={stats.totalRegions} color="bg-cyan-500/10 text-cyan-600" />
        <StatCard icon={<MapPin className="size-5" />} label={t("overview.districts")} value={stats.totalDistricts} color="bg-lime-500/10 text-lime-600" />
        <StatCard icon={<Building2 className="size-5" />} label={t("overview.institutions")} value={stats.totalInstitutions} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<Users className="size-5" />} label={t("overview.teachers")} value={stats.totalTeachers} color="bg-green-500/10 text-green-600" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<GraduationCap className="size-5" />} label={t("overview.learners")} value={stats.totalStudents} color="bg-purple-500/10 text-purple-600" />
        <StatCard icon={<Layers className="size-5" />} label={t("overview.courses")} value={stats.totalCourses ?? 0} color="bg-indigo-500/10 text-indigo-600" />
        <StatCard icon={<BookMarked className="size-5" />} label={t("overview.subjects")} value={stats.totalSubjects ?? 0} color="bg-pink-500/10 text-pink-600" />
        <StatCard icon={<Users className="size-5" />} label={t("overview.classes")} value={stats.totalClasses} color="bg-orange-500/10 text-orange-600" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Video className="size-5" />} label={t("overview.liveClasses")} value={stats.activeLiveClasses} color="bg-red-500/10 text-red-600" />
        <StatCard icon={<Target className="size-5" />} label={t("overview.attendance")} value={`${stats.attendanceRate}%`} color="bg-teal-500/10 text-teal-600" />
        <StatCard icon={<TrendingUp className="size-5" />} label={t("overview.performance")} value={`${stats.averagePerformance}%`} color="bg-indigo-500/10 text-indigo-600" />
        <StatCard icon={<BookOpen className="size-5" />} label={t("overview.curriculum")} value={`${stats.curriculumProgress}%`} color="bg-pink-500/10 text-pink-600" />
      </div>

      {/* Learning pulse (§11) — real computed rates, honest empty states. */}
      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-foreground">
          <Activity className="size-5 text-muted-foreground" />
          {t("overview.learningPulse")}
        </h2>
        <div className="space-y-4">
          <PulseBar label={t("overview.attendance")} value={stats.attendanceRate} color="bg-teal-500" />
          <PulseBar label={t("overview.performance")} value={stats.averagePerformance} color="bg-indigo-500" />
          <PulseBar label={t("overview.curriculum")} value={stats.curriculumProgress} color="bg-pink-500" />
        </div>
      </div>

      {/* Attention centre (§12) — operational + governance items from one API. */}
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="flex items-center gap-2 text-lg font-semibold text-foreground">
            <AlertTriangle className="size-5 text-orange-500" />
            {t("overview.attentionCenter")}
          </h2>
          <Link
            href="/oversight/alerts"
            className="flex items-center gap-1 text-sm font-medium text-primary hover:underline"
          >
            {t("overview.viewAllAlerts")}
            <ChevronRight className="size-4" />
          </Link>
        </div>

        {attention ? (
          <>
            <div className="mb-4 flex flex-wrap gap-2">
              <Chip label={t("overview.total")} value={attention.summary.total} tone="bg-muted text-foreground" />
              <Chip label="HIGH" value={attention.summary.high} tone="bg-red-100 text-red-700" />
              <Chip label="MEDIUM" value={attention.summary.medium} tone="bg-orange-100 text-orange-700" />
              <Chip label={t("overview.verifications")} value={attention.summary.verification} tone="bg-blue-100 text-blue-700" />
              <Chip label={t("overview.dataQuality")} value={attention.summary.dataQuality} tone="bg-purple-100 text-purple-700" />
            </div>
            {attention.items.length === 0 ? (
              <p className="rounded-xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
                {t("overview.noAttentionItems")}
              </p>
            ) : (
              <div className="space-y-3">
                {attention.items.slice(0, 5).map((item) => (
                  <div
                    key={`${item.category}-${item.id}`}
                    className="flex items-center justify-between rounded-xl border border-border p-4 transition-colors hover:bg-muted/50"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <div
                        className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${
                          item.severity === "HIGH"
                            ? "bg-red-100"
                            : item.severity === "MEDIUM"
                              ? "bg-orange-100"
                              : "bg-yellow-100"
                        }`}
                      >
                        <AlertTriangle
                          className={`size-4 ${
                            item.severity === "HIGH"
                              ? "text-red-600"
                              : item.severity === "MEDIUM"
                                ? "text-orange-600"
                                : "text-yellow-600"
                          }`}
                        />
                      </div>
                      <div className="min-w-0">
                        <p className="truncate font-medium text-foreground">{item.title}</p>
                        <p className="truncate text-sm text-muted-foreground">
                          {item.category}
                          {item.jurisdiction ? ` • ${item.jurisdiction}` : ""}
                          {item.timestamp ? ` • ${new Date(item.timestamp).toLocaleString()}` : ""}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`ml-3 inline-flex shrink-0 items-center rounded-full px-2 py-1 text-xs font-medium ${
                        item.severity === "HIGH"
                          ? "bg-red-100 text-red-700"
                          : item.severity === "MEDIUM"
                            ? "bg-orange-100 text-orange-700"
                            : "bg-yellow-100 text-yellow-700"
                      }`}
                    >
                      {item.severity}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </>
        ) : (
          <p className="text-sm text-muted-foreground">{t("overview.errorLoading")}</p>
        )}
      </div>

      {stats.topRegions.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6">
          <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-foreground">
            <TrendingUp className="size-5 text-muted-foreground" />
            {t("overview.topRegionsByInstitution")}
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("overview.region")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("overview.code")}</th>
                  <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t("overview.institutions")}</th>
                  <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t("overview.teachers")}</th>
                  <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t("overview.students")}</th>
                  <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t("overview.attendance")}</th>
                  <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t("overview.performance")}</th>
                </tr>
              </thead>
              <tbody>
                {stats.topRegions.map((region) => (
                  <tr key={region.regionCode} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="px-4 py-3 font-medium text-foreground">{region.regionName}</td>
                    <td className="px-4 py-3 text-muted-foreground">{region.regionCode}</td>
                    <td className="px-4 py-3 text-right font-medium text-foreground">{region.institutionCount}</td>
                    <td className="px-4 py-3 text-right font-medium text-foreground">{region.teacherCount}</td>
                    <td className="px-4 py-3 text-right font-medium text-foreground">{region.studentCount}</td>
                    <td className="px-4 py-3 text-right font-medium text-foreground">{region.attendanceRate}%</td>
                    <td className="px-4 py-3 text-right font-medium text-foreground">{region.averagePerformance}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({
  icon,
  label,
  value,
  color,
}: {
  icon: React.ReactNode
  label: string
  value: number | string
  color: string
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>{icon}</div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}

function PulseBar({ label, value, color }: { label: string; value: number; color: string }) {
  const safeValue = Math.max(0, Math.min(100, value || 0))
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-sm">
        <span className="font-medium text-foreground">{label}</span>
        <span className="text-muted-foreground">{safeValue}%</span>
      </div>
      <div className="h-2.5 overflow-hidden rounded-full bg-muted">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${safeValue}%` }} />
      </div>
    </div>
  )
}

function Chip({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium ${tone}`}>
      {label}: {value}
    </span>
  )
}
