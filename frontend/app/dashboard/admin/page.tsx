"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import {
  Users, GraduationCap, UserCheck, Award, Clock, Loader2, BookOpen, FileText,
  Video, BookMarked, AlertTriangle, Eye, Settings, Shield,
  Activity, RefreshCw, ClipboardList, BookPlus, CalendarPlus, Zap,
  ChevronRight, CheckCircle2, UserPlus, ClipboardCheck, CreditCard, Wrench
} from "lucide-react"
import Link from "next/link"
import { adminApi, getInstitutionId, type EnhancedDashboardResponse } from "@/lib/api"
import { cn } from "@/lib/utils"

const QUICK_ACTION_ICONS: Record<string, typeof Zap> = {
  "book-plus": BookPlus,
  "user-plus": UserPlus,
  "video-plus": Video,
  "calendar-plus": CalendarPlus,
  "file-text": FileText,
  shield: Shield,
  eye: Eye,
  settings: Wrench,
  award: Award,
  upload: FileText,
  "clipboard-check": ClipboardCheck,
  "credit-card-check": CreditCard,
}

function healthStatusStyle(status: string) {
  switch (status) {
    case "HEALTHY":
      return "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
    case "DEGRADED":
      return "bg-amber-500/10 text-amber-600 dark:text-amber-400"
    case "DOWN":
    case "CRITICAL":
      return "bg-destructive/10 text-destructive"
    default:
      return "bg-muted text-muted-foreground"
  }
}

function healthStatusLabelKey(status: string): string {
  switch (status) {
    case "HEALTHY":
      return "statusHealthy"
    case "DEGRADED":
      return "statusDegraded"
    case "DOWN":
      return "statusDown"
    case "CRITICAL":
      return "statusCritical"
    default:
      return "statusUnknown"
  }
}







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

  // §018 — actions come from the server's permission/scope evaluation; any
  // action the API marks unavailable is hidden rather than rendered broken.
  const quickActions = (data?.quickActions ?? []).filter((action) => action.available)
  const healthSummary = data?.healthSummary ?? null
  const workQueue = data?.workQueueSummary ?? null
  const queueItems = workQueue?.items ?? []
  const queueTotals =
    (workQueue?.pendingApprovals ?? 0) +
    (workQueue?.pendingReviews ?? 0) +
    (workQueue?.pendingVerifications ?? 0)

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

          {/* Quick Actions — permission/scope driven by the server */}
          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">{t("overview.quickActions")}</h2>
            {quickActions.length > 0 ? (
              <div className="grid grid-cols-3 gap-3 sm:grid-cols-6">
                {quickActions.map((action) => {
                  const Icon = QUICK_ACTION_ICONS[action.icon] ?? Zap
                  return (
                    <Link
                      key={action.id}
                      href={action.actionUrl}
                      title={action.requiredPermission}
                      className="flex flex-col items-center gap-2 rounded-2xl border border-border bg-card p-4 text-center shadow-xs transition-all hover:shadow-md hover:border-primary/30"
                    >
                      <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                        <Icon className="size-5" />
                      </div>
                      <span className="text-xs font-medium text-foreground">{action.label}</span>
                    </Link>
                  )
                })}
              </div>
            ) : (
              <div className="rounded-2xl border border-border bg-card px-6 py-8 text-center">
                <p className="text-sm text-muted-foreground">{t("overview.noQuickActions")}</p>
              </div>
            )}
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

          {/* §014 Organization Health — real indicators only */}
          <div>
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                {t("overview.organizationHealth")}
              </h2>
              {healthSummary && (
                <span
                  className={cn(
                    "inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide",
                    healthStatusStyle(healthSummary.overallStatus),
                  )}
                >
                  {t(`overview.${healthStatusLabelKey(healthSummary.overallStatus)}`)}
                </span>
              )}
            </div>

            {healthSummary && healthSummary.metrics && healthSummary.metrics.length > 0 ? (
              <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                <div className="mb-4 flex items-center gap-2 text-xs text-muted-foreground">
                  <Activity className="size-3.5" />
                  <span>
                    {healthSummary.lastChecked
                      ? `${t("overview.lastChecked")}: ${new Date(healthSummary.lastChecked).toLocaleString()}`
                      : t("overview.lastCheckedUnavailable")}
                  </span>
                </div>
                <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {healthSummary.metrics.map((metric, i) => (
                    <li key={`${metric.name}-${i}`} className="rounded-xl border border-border px-4 py-3">
                      <div className="flex items-center justify-between gap-2">
                        <p className="truncate text-sm font-medium text-foreground">{metric.name}</p>
                        <span
                          className={cn(
                            "shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                            healthStatusStyle(metric.status),
                          )}
                        >
                          {t(`overview.${healthStatusLabelKey(metric.status)}`)}
                        </span>
                      </div>
                      <p className="mt-1 break-words text-xs text-muted-foreground">{metric.value}</p>
                      {metric.threshold && (
                        <p className="mt-0.5 text-[11px] text-muted-foreground">
                          {t("overview.expectedState")}: {metric.threshold}
                        </p>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            ) : (
              <div className="rounded-2xl border border-border bg-card px-6 py-10 text-center">
                <p className="text-sm text-muted-foreground">{t("overview.noHealthData")}</p>
                <button
                  type="button"
                  onClick={load}
                  className="mt-4 inline-flex h-8 items-center gap-2 rounded-lg border border-border px-3 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
                >
                  <RefreshCw className="size-3.5" />
                  {t("overview.refresh")}
                </button>
              </div>
            )}
          </div>

          {/* §017 Work Queue — institution-scoped counts from the API */}
          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">
              {t("overview.workQueue")}
            </h2>

            {queueItems.length > 0 ? (
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {queueItems.map((item) => (
                  <Link
                    key={item.type}
                    href={item.actionUrl}
                    className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
                  >
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
                      <ClipboardList className="size-5" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-foreground">{item.label}</p>
                      <p className="text-xs text-muted-foreground">
                        {t("overview.queueCount", { count: item.count })}
                      </p>
                    </div>
                    <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
                  </Link>
                ))}
              </div>
            ) : queueTotals > 0 && workQueue ? (
              <div className="grid gap-3 sm:grid-cols-3">
                {[
                  { label: t("overview.pendingApprovals"), value: workQueue.pendingApprovals },
                  { label: t("overview.pendingReviews"), value: workQueue.pendingReviews },
                  { label: t("overview.pendingVerifications"), value: workQueue.pendingVerifications },
                ].map((entry) => (
                  <div key={entry.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <p className="text-2xl font-bold text-foreground">{entry.value}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{entry.label}</p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="rounded-2xl border border-border bg-card px-6 py-10 text-center">
                <span className="mx-auto flex size-11 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="size-5" />
                </span>
                <p className="mt-3 text-sm font-medium text-foreground">{t("overview.noWorkQueue")}</p>
                <p className="mt-1 text-xs text-muted-foreground">{t("overview.noWorkQueueHint")}</p>
              </div>
            )}
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
