"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import {
  Users, GraduationCap, UserCheck, Award, Clock, Loader2, BookOpen, FileText,
  Video, BookMarked, AlertTriangle, TrendingUp, Eye, School, Settings, Shield,
  Send, BarChart3, Activity, Bell
} from "lucide-react"
import Link from "next/link"
import { adminApi, getInstitutionId, type EnhancedDashboardResponse } from "@/lib/api"







export default function AdminDashboardPage() {
  const t = useTranslations("admin");
  const statCards = [
    { key: "totalStudents" as const, label: t("overview.totalStudents"), icon: GraduationCap, color: "text-blue-600", bg: "bg-blue-500/10", href: "/dashboard/admin/people" },
    { key: "totalTeachers" as const, label: t("overview.totalTeachers"), icon: Users, color: "text-teal-600", bg: "bg-teal-500/10", href: "/dashboard/admin/people" },
    { key: "totalParents" as const, label: t("overview.totalParents"), icon: UserCheck, color: "text-purple-600", bg: "bg-purple-500/10", href: "/dashboard/admin/people" },
    { key: "activeStudents" as const, label: t("overview.activeStudents"), icon: Users, color: "text-orange-600", bg: "bg-orange-500/10", href: "/dashboard/admin/people" },
    { key: "certificatesIssued" as const, label: t("overview.certificates"), icon: Award, color: "text-amber-600", bg: "bg-amber-500/10", href: "/dashboard/admin" },
    { key: "pendingImportJobs" as const, label: t("overview.pendingImports"), icon: Clock, color: "text-muted-foreground", bg: "bg-muted", href: "/dashboard/admin/import" },
  ]
  const courseCards = [
    { key: "totalCourses" as const, label: t("overview.totalCourses"), icon: BookOpen, color: "text-indigo-600", bg: "bg-indigo-500/10" },
    { key: "publishedCourses" as const, label: t("overview.published"), icon: BookMarked, color: "text-emerald-600", bg: "bg-emerald-500/10" },
    { key: "draftCourses" as const, label: t("overview.drafts"), icon: FileText, color: "text-yellow-600", bg: "bg-yellow-500/10" },
    { key: "totalModules" as const, label: t("overview.modules"), icon: BookOpen, color: "text-cyan-600", bg: "bg-cyan-500/10" },
    { key: "totalLessons" as const, label: t("overview.lessons"), icon: FileText, color: "text-pink-600", bg: "bg-pink-500/10" },
    { key: "liveClassesScheduled" as const, label: t("overview.liveClasses"), icon: Video, color: "text-red-600", bg: "bg-red-500/10" },
  ]
  const quickActions = [
    { label: t("overview.people"), href: "/dashboard/admin/people", icon: Users, color: "text-blue-600 bg-blue-500/10" },
    { label: t("overview.roles"), href: "/dashboard/admin/roles", icon: Shield, color: "text-purple-600 bg-purple-500/10" },
    { label: t("overview.profile"), href: "/dashboard/admin/profile", icon: School, color: "text-teal-600 bg-teal-500/10" },
    { label: t("overview.import"), href: "/dashboard/admin/import", icon: Send, color: "text-orange-600 bg-orange-500/10" },
    { label: t("overview.settings"), href: "/dashboard/admin/settings", icon: Settings, color: "text-gray-600 bg-gray-500/10" },
    { label: t("overview.audit"), href: "/dashboard/admin/audit", icon: Eye, color: "text-red-600 bg-red-500/10" },
  ]

  const [data, setData] = useState<EnhancedDashboardResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = () => {
    const institutionId = getInstitutionId()
    if (!institutionId) {
      setError(t("overview.noInstitutionContextFound"))
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    adminApi
      .getEnhancedDashboard(institutionId)
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : t("overview.failedToLoadDashboard")))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("overview.administration")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {data?.institutionName ? `${data.institutionName} — ` : ""}
            {t("overview.overviewAndManagementFor")}</p>
        </div>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
          <button
            onClick={load}
            className="mt-4 inline-flex h-9 items-center gap-2 rounded-lg border border-destructive/30 px-4 text-sm font-medium text-destructive hover:bg-destructive/10"
          >
            Retry
          </button>
        </div>
      )}

      {data && (
        <>
          {/* Attention Items */}
          {data.attentionItems && data.attentionItems.length > 0 && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-950/30">
              <div className="mb-3 flex items-center gap-2">
                <AlertTriangle className="size-4 text-amber-600" />
                <h3 className="text-sm font-semibold text-amber-800 dark:text-amber-200">{t("overview.needsAttention")}</h3>
              </div>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {data.attentionItems.map((item) => (
                  <Link
                    key={item.type}
                    href={item.actionUrl}
                    className="flex items-center gap-3 rounded-lg border border-amber-200 bg-white p-3 transition-colors hover:bg-amber-50 dark:border-amber-800 dark:bg-card dark:hover:bg-amber-950/30"
                  >
                    <span className="flex size-8 items-center justify-center rounded-full bg-amber-100 text-xs font-bold text-amber-700 dark:bg-amber-900 dark:text-amber-300">
                      {item.count}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-foreground truncate">{item.title}</p>
                      <p className="text-xs text-muted-foreground truncate">{item.description}</p>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Quick Actions */}
          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">{t("overview.quickActions")}</h2>
            <div className="grid grid-cols-3 gap-3 sm:grid-cols-6">
              {quickActions.map((action) => (
                <Link
                  key={action.href}
                  href={action.href}
                  className="flex flex-col items-center gap-2 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
                >
                  <div className={`flex size-10 items-center justify-center rounded-xl ${action.color}`}>
                    <action.icon className="size-5" />
                  </div>
                  <span className="text-xs font-medium text-foreground">{action.label}</span>
                </Link>
              ))}
            </div>
          </div>

          {/* Institution Overview Stats */}
          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">{t("overview.institutionOverview")}</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {statCards.map((card) => (
                <Link
                  key={card.key}
                  href={card.href}
                  className="rounded-2xl border border-border bg-card p-6 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
                >
                  <div className="flex items-center gap-4">
                    <div className={`flex size-12 items-center justify-center rounded-xl ${card.bg}`}>
                      <card.icon className={`size-6 ${card.color}`} />
                    </div>
                    <div>
                      <p className="text-2xl font-bold text-foreground">{data[card.key] ?? 0}</p>
                      <p className="text-xs text-muted-foreground">{card.label}</p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>

          {/* Course Management */}
          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">{t("overview.courseManagement")}</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {courseCards.map((card) => (
                <div key={card.key} className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                  <div className="flex items-center gap-4">
                    <div className={`flex size-12 items-center justify-center rounded-xl ${card.bg}`}>
                      <card.icon className={`size-6 ${card.color}`} />
                    </div>
                    <div>
                      <p className="text-2xl font-bold text-foreground">{data[card.key] ?? 0}</p>
                      <p className="text-xs text-muted-foreground">{card.label}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Recent Activity */}
          {data.recentActivity && data.recentActivity.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <div className="mb-4 flex items-center gap-2">
                <Activity className="size-4 text-muted-foreground" />
                <h2 className="text-sm font-semibold text-foreground">{t("overview.recentActivity")}</h2>
              </div>
              <div className="space-y-3">
                {data.recentActivity.map((activity, i) => (
                  <div key={i} className="flex items-start gap-3 rounded-lg border border-border px-4 py-3">
                    <div className="mt-0.5 size-2 shrink-0 rounded-full bg-primary" />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-foreground">{activity.title}</p>
                      {activity.description && (
                        <p className="text-xs text-muted-foreground truncate">{activity.description}</p>
                      )}
                    </div>
                    <span className="shrink-0 text-xs text-muted-foreground">
                      {new Date(activity.timestamp).toLocaleDateString()}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Enabled Services */}
          {data.enabledServices && data.enabledServices.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <div className="mb-4 flex items-center gap-2">
                <Settings className="size-4 text-muted-foreground" />
                <h2 className="text-sm font-semibold text-foreground">{t("overview.enabledServices")}</h2>
              </div>
              <div className="flex flex-wrap gap-2">
                {data.enabledServices.map((service) => (
                  <span key={service} className="inline-flex items-center rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                    {service}
                  </span>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
