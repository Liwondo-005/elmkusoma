"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, Activity, TrendingUp } from "lucide-react"

interface DashboardStats {
  totalInstitutions: number
  totalTeachers: number
  totalStudents: number
  totalUsers: number
  totalRegions: number
  totalDistricts: number
  topRegions: Array<{
    regionName: string
    regionCode: string
    institutionCount: number
    teacherCount: number
    studentCount: number
  }>
}

export default function NationalDashboardPage() {
  const t = useTranslations("oversight");
  const { user, loading: authLoading } = useRequireAuth()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchDashboard()
    }
  }, [user, authLoading])

  async function fetchDashboard() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/dashboard`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        setStats(await res.json())
      }
    } catch {
      // Dashboard unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("national.loadingNationalDashboard")}</div>
      </div>
    )
  }

  if (!stats) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">{t("national.nationalEducationOversight")}</h1>
          <p className="mt-2 text-muted-foreground">{t("national.welcomeDashboardDataUnavailable", { p0: user?.name })}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <Activity className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{t("national.nationalEducationOversight2")}</h1>
            <p className="text-sm text-muted-foreground">{t("national.welcome", { p0: user?.name })}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Building2 className="size-5" />} label="Institutions" value={stats.totalInstitutions} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<Users className="size-5" />} label="Teachers" value={stats.totalTeachers} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<GraduationCap className="size-5" />} label="Students" value={stats.totalStudents} color="bg-purple-500/10 text-purple-600" />
        <StatCard icon={<MapPin className="size-5" />} label="Regions" value={stats.totalRegions} color="bg-orange-500/10 text-orange-600" />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard icon={<MapPin className="size-5" />} label="Total Districts" value={stats.totalDistricts} color="bg-teal-500/10 text-teal-600" />
        <StatCard icon={<Users className="size-5" />} label="Total Users" value={stats.totalUsers} color="bg-indigo-500/10 text-indigo-600" />
      </div>

      {stats.topRegions.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
            <TrendingUp className="size-5 text-muted-foreground" />
            {t("national.topRegionsByInstitution")}</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("national.region")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("national.code")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("national.institutions")}</th>
                </tr>
              </thead>
              <tbody>
                {stats.topRegions.map((region) => (
                  <tr key={region.regionCode} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4 font-medium text-foreground">{region.regionName}</td>
                    <td className="py-3 px-4 text-muted-foreground">{region.regionCode}</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{region.institutionCount}</td>
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

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          {icon}
        </div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value.toLocaleString()}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}
