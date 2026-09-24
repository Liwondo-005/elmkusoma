"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, Activity, TrendingUp, AlertTriangle, Clock, Target, Video, BookOpen, FileBarChart } from "lucide-react"

interface OversightDashboardStats {
  totalInstitutions: number
  totalTeachers: number
  totalStudents: number
  totalUsers: number
  totalRegions: number
  totalDistricts: number
  activeLiveClasses: number
  totalLessons: number
  totalClasses: number
  attendanceRate: number
  averagePerformance: number
  curriculumProgress: number
  alertsCount: number
  topRegions: Array<{
    regionName: string
    regionCode: string
    institutionCount: number
    teacherCount: number
    studentCount: number
    attendanceRate: number
    averagePerformance: number
  }>
  recentAlerts: Array<{
    id: string
    type: string
    message: string
    severity: string
    institutionName: string
    timestamp: string
  }>
  jurisdictionSummary: {
    type: "national" | "region" | "district"
    name: string
    code: string
  }
}

export default function OversightOverviewPage() {
  const t = useTranslations("oversight");
  const { user, loading: authLoading } = useRequireAuth()
  const [stats, setStats] = useState<OversightDashboardStats | null>(null)
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
        <div className="text-muted-foreground">{t("overview.loadingOversightDashboard")}</div>
      </div>
    )
  }

  if (!stats) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">{t("overview.educationOversight")}</h1>
          <p className="mt-2 text-muted-foreground">{t("overview.welcomeDashboardDataUnavailable", { p0: user?.name })}</p>
        </div>
      </div>
    )
  }

  const jurisdictionLabel = stats.jurisdictionSummary?.type === "national" ? t("overview.national") :
    stats.jurisdictionSummary?.type === "region" ? t("overview.regional") : t("overview.district")

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <Activity className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{t("overview.educationOversightCommandCenter")}</h1>
            <p className="text-sm text-muted-foreground">
              {jurisdictionLabel} {t("overview.oversight")}{stats.jurisdictionSummary?.name || t("overview.overviewFallback")} {t("overview.welcome")}{user?.name}
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Building2 className="size-5" />} label="Institutions" value={stats.totalInstitutions} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<Users className="size-5" />} label="Teachers" value={stats.totalTeachers} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<GraduationCap className="size-5" />} label="Students" value={stats.totalStudents} color="bg-purple-500/10 text-purple-600" />
        <StatCard icon={<Users className="size-5" />} label="Classes" value={stats.totalClasses} color="bg-orange-500/10 text-orange-600" />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Video className="size-5" />} label="Live Classes" value={stats.activeLiveClasses} color="bg-red-500/10 text-red-600" />
        <StatCard icon={<Target className="size-5" />} label="Attendance Rate" value={`${stats.attendanceRate}%`} color="bg-teal-500/10 text-teal-600" />
        <StatCard icon={<TrendingUp className="size-5" />} label="Avg Performance" value={`${stats.averagePerformance}%`} color="bg-indigo-500/10 text-indigo-600" />
        <StatCard icon={<BookOpen className="size-5" />} label="Curriculum Progress" value={`${stats.curriculumProgress}%`} color="bg-pink-500/10 text-pink-600" />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<AlertTriangle className="size-5" />} label="Active Alerts" value={stats.alertsCount} color="bg-orange-500/10 text-orange-600" />
        <StatCard icon={<FileBarChart className="size-5" />} label="Total Lessons" value={stats.totalLessons} color="bg-gray-500/10 text-gray-600" />
        <StatCard icon={<MapPin className="size-5" />} label="Regions" value={stats.totalRegions} color="bg-cyan-500/10 text-cyan-600" />
        <StatCard icon={<MapPin className="size-5" />} label="Districts" value={stats.totalDistricts} color="bg-lime-500/10 text-lime-600" />
      </div>

      {stats.topRegions.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
            <TrendingUp className="size-5 text-muted-foreground" />
            {t("overview.topRegionsByInstitution")}</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("overview.region")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("overview.code")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("overview.institutions")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("overview.teachers")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("overview.students")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("overview.attendance")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("overview.performance")}</th>
                </tr>
              </thead>
              <tbody>
                {stats.topRegions.map((region) => (
                  <tr key={region.regionCode} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4 font-medium text-foreground">{region.regionName}</td>
                    <td className="py-3 px-4 text-muted-foreground">{region.regionCode}</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{region.institutionCount}</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{region.teacherCount}</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{region.studentCount}</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{region.attendanceRate}%</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{region.averagePerformance}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {stats.recentAlerts.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
            <AlertTriangle className="size-5 text-orange-500" />
            {t("overview.recentAlertsRequiringAttention")}</h2>
          <div className="space-y-3">
            {stats.recentAlerts.slice(0, 5).map((alert) => (
              <div
                key={alert.id}
                className="flex items-center justify-between rounded-xl border border-border p-4 hover:bg-muted/50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className={`flex size-8 items-center justify-center rounded-lg ${
                    alert.severity === "HIGH" ? "bg-red-100" :
                    alert.severity === "MEDIUM" ? "bg-orange-100" : "bg-yellow-100"
                  }`}>
                    <AlertTriangle className={`size-4 ${alert.severity === "HIGH" ? "text-red-600" : alert.severity === "MEDIUM" ? "text-orange-600" : "text-yellow-600"}`} />
                  </div>
                  <div>
                    <p className="font-medium text-foreground">{alert.message}</p>
                    <p className="text-sm text-muted-foreground">{alert.institutionName} • {new Date(alert.timestamp).toLocaleString()}</p>
                  </div>
                </div>
                <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                  alert.severity === "HIGH" ? "bg-red-100 text-red-700" :
                  alert.severity === "MEDIUM" ? "bg-orange-100 text-orange-700" : "bg-yellow-100 text-yellow-700"
                }`}>
                  {alert.severity}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number | string; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          {icon}
        </div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}