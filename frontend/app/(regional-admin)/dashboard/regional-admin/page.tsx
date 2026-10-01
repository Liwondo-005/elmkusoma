"use client"

import { useEffect, useState, useCallback } from "react"
import Link from "next/link"
import {
  MapPin, School, Users, GraduationCap, BookOpen, Video, ClipboardCheck,
  ShieldCheck, Activity, RefreshCw, Loader2, AlertTriangle, Bell, Zap,
  ArrowRight, CheckCircle2, XCircle, FileSearch, BarChart3, Database,
} from "lucide-react"
import {
  regionalAdminApi,
  type RegionalDashboard,
  type AttentionItem,
  type RegionalPulse,
} from "@/lib/regional-admin-api"

function Skeleton() {
  return <div className="h-24 animate-pulse rounded-2xl border border-border bg-card" />
}

function Kpi({
  icon: Icon, label, value, sub, href, accent,
}: {
  icon: typeof MapPin; label: string; value: string | number; sub?: string; href: string; accent: string
}) {
  return (
    <Link
      href={href}
      className="group relative overflow-hidden rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/20 hover:shadow-md"
    >
      <div
        className="absolute -right-6 -top-6 size-20 rounded-full opacity-[0.05] transition-opacity group-hover:opacity-10"
        style={{ background: accent }}
      />
      <div className="flex items-start justify-between">
        <div
          className="flex size-10 items-center justify-center rounded-xl border"
          style={{ background: `${accent}14`, borderColor: `${accent}22`, color: accent }}
        >
          <Icon className="size-5" />
        </div>
        <ArrowRight className="size-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
      </div>
      <div className="mt-4">
        <p className="text-[11px] font-bold uppercase tracking-widest text-muted-foreground">{label}</p>
        <p className="mt-1 text-2xl font-bold tabular-nums tracking-tight text-foreground">
          {typeof value === "number" ? value.toLocaleString() : value}
        </p>
        {sub && <p className="mt-1 text-xs text-muted-foreground">{sub}</p>}
      </div>
    </Link>
  )
}

const SEVERITY_STYLES: Record<string, { card: string; chip: string }> = {
  HIGH: { card: "border-red-200 bg-red-50", chip: "bg-red-500" },
  CRITICAL: { card: "border-red-200 bg-red-50", chip: "bg-red-500" },
  MEDIUM: { card: "border-amber-200 bg-amber-50", chip: "bg-amber-500" },
  LOW: { card: "border-blue-200 bg-blue-50", chip: "bg-blue-500" },
  INFO: { card: "border-blue-200 bg-blue-50", chip: "bg-blue-500" },
}

export default function RegionalAdminOverview() {
  const [dashboard, setDashboard] = useState<RegionalDashboard | null>(null)
  const [attention, setAttention] = useState<AttentionItem[]>([])
  const [pulse, setPulse] = useState<RegionalPulse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const [d, a, p] = await Promise.allSettled([
      regionalAdminApi.getDashboard(),
      regionalAdminApi.getAttention(),
      regionalAdminApi.getPulse(),
    ])
    if (d.status === "fulfilled") setDashboard(d.value)
    else setError("Unable to load the regional command center.")
    if (a.status === "fulfilled") setAttention(a.value)
    if (p.status === "fulfilled") setPulse(p.value)
    setLoading(false)
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const jurisdiction = dashboard?.jurisdictionSummary
  const pct = (v: number | null | undefined) => (v === null || v === undefined ? "—" : `${v.toFixed(1)}%`)

  return (
    <div className="space-y-6 pb-8">
      {/* Hero */}
      <div className="relative overflow-hidden rounded-2xl border border-border bg-gradient-to-br from-primary via-primary to-primary/80 p-6 text-primary-foreground shadow-sm">
        <div className="absolute -right-12 -top-12 size-40 rounded-full bg-white/10" />
        <div className="absolute -bottom-10 -left-10 size-32 rounded-full bg-white/10" />
        <div className="relative">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-white/70">
            <MapPin className="size-3.5" />
            {jurisdiction ? `${jurisdiction.name} · ${jurisdiction.code}` : "Resolving jurisdiction…"}
          </div>
          <h1 className="mt-1 text-2xl font-bold tracking-tight md:text-3xl">
            Regional Education Command Center
          </h1>
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-white/80">
            Real institutions, people and learning activity inside your authorized jurisdiction — every figure is
            scoped server-side to your region or district.
          </p>
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={load}
              className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary shadow-sm transition-colors hover:bg-white/90"
            >
              <RefreshCw className="size-4" /> Refresh
            </button>
            <Link
              href="/dashboard/regional-admin/governance/verification"
              className="inline-flex items-center gap-2 rounded-xl bg-white/15 px-4 py-2 text-sm font-semibold text-white backdrop-blur transition-colors hover:bg-white/20"
            >
              <ShieldCheck className="size-4" />
              Verification queue{dashboard ? ` (${dashboard.pendingVerifications})` : ""}
            </Link>
            {dashboard && (
              <span className="text-xs text-white/75">
                Last updated: {new Date(dashboard.lastUpdated).toLocaleString()}
              </span>
            )}
          </div>
        </div>
      </div>

      {error && (
        <div className="flex items-center justify-between rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          <span className="flex items-center gap-2">
            <XCircle className="size-4" /> {error}
          </span>
          <button
            type="button"
            onClick={load}
            className="rounded-lg border border-red-200 bg-white px-3 py-1 text-xs font-semibold"
          >
            Retry
          </button>
        </div>
      )}

      {/* KPIs */}
      <section aria-label="Jurisdiction indicators">
        <h2 className="mb-3 text-xs font-bold uppercase tracking-widest text-muted-foreground">
          Jurisdiction overview
        </h2>
        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <Skeleton key={i} />
            ))}
          </div>
        ) : dashboard ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Kpi icon={MapPin} label="Districts" value={dashboard.totalDistricts} href="/dashboard/regional-admin/districts" accent="#2563eb" />
            <Kpi icon={School} label="Schools & Institutions" value={dashboard.totalInstitutions} sub={`${dashboard.totalSchools} schools`} href="/dashboard/regional-admin/institutions" accent="#059669" />
            <Kpi icon={GraduationCap} label="Teachers" value={dashboard.totalTeachers} sub="Institutions in scope" href="/dashboard/regional-admin/teachers" accent="#7c3aed" />
            <Kpi icon={Users} label="Learners" value={dashboard.totalLearners} href="/dashboard/regional-admin/learners" accent="#d97706" />
            <Kpi icon={BookOpen} label="Courses" value={dashboard.totalCourses} sub={`${dashboard.totalLessons} lessons`} href="/dashboard/regional-admin/learning/courses" accent="#0891b2" />
            <Kpi icon={Video} label="Live Classes" value={dashboard.activeLiveClasses} sub={`${dashboard.totalClasses} total sessions`} href="/oversight/live-classes" accent="#db2777" />
            <Kpi icon={ClipboardCheck} label="Attendance" value={pct(dashboard.attendanceRate)} href="/oversight/attendance" accent="#16a34a" />
            <Kpi icon={ShieldCheck} label="Pending Verification" value={dashboard.pendingVerifications} sub={`${dashboard.dataQualityIssues} data quality findings`} href="/dashboard/regional-admin/governance/verification" accent="#dc2626" />
          </div>
        ) : (
          <div className="rounded-xl border border-dashed p-8 text-center text-sm text-muted-foreground">
            No data available in your jurisdiction.
          </div>
        )}
      </section>

      {/* Attention */}
      <section
        aria-label="Regional attention required"
        className="rounded-2xl border border-amber-200 bg-gradient-to-br from-amber-50/50 to-white p-5 shadow-sm"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-base font-bold text-foreground">
            <span className="flex size-8 items-center justify-center rounded-lg bg-amber-500 text-white">
              <AlertTriangle className="size-4" />
            </span>
            Regional Attention Required
          </h2>
          <span className="rounded-full bg-red-500 px-2.5 py-1 text-xs font-bold text-white">
            {attention.length}
          </span>
        </div>
        {loading ? (
          <div className="space-y-2 animate-pulse">
            <div className="h-16 rounded-xl bg-muted" />
            <div className="h-16 rounded-xl bg-muted" />
          </div>
        ) : attention.length === 0 ? (
          <div className="rounded-xl border border-dashed border-emerald-200 bg-emerald-50/50 p-8 text-center">
            <CheckCircle2 className="mx-auto size-10 text-emerald-500" />
            <p className="mt-3 text-sm font-semibold text-foreground">All clear in your jurisdiction</p>
            <p className="mt-1 text-xs text-muted-foreground">
              No items currently require regional action.
            </p>
          </div>
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {attention.map((item, i) => {
              const style = SEVERITY_STYLES[item.severity] || SEVERITY_STYLES.INFO
              return (
                <Link
                  key={`${item.title}-${i}`}
                  href={item.actionUrl}
                  className={`group flex gap-3 rounded-xl border p-4 transition-all hover:shadow-sm ${style.card}`}
                >
                  <div className={`mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg text-white ${style.chip}`}>
                    <AlertTriangle className="size-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest text-white ${style.chip}`}>
                        {item.severity}
                      </span>
                      <span className="text-xs text-muted-foreground">{item.category}</span>
                    </div>
                    <p className="mt-1 text-sm font-semibold text-foreground">{item.title}</p>
                    <p className="mt-0.5 line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                      {item.description}
                    </p>
                  </div>
                  <ArrowRight className="mt-2 size-4 shrink-0 text-muted-foreground opacity-40 transition-all group-hover:translate-x-0.5 group-hover:opacity-100" />
                </Link>
              )
            })}
          </div>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Pulse */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-sm lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground">
              <Activity className="size-4 text-primary" /> Regional Education Pulse
            </h2>
            <Link href="/dashboard/regional-admin/pulse" className="text-xs font-semibold text-primary hover:underline">
              Open pulse →
            </Link>
          </div>
          {loading ? (
            <div className="space-y-2 animate-pulse">
              <div className="h-16 rounded-xl bg-muted" />
              <div className="h-16 rounded-xl bg-muted" />
            </div>
          ) : pulse ? (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {[
                { label: "Live now", value: pulse.liveNow, icon: Video, color: "text-pink-600" },
                { label: "Scheduled today", value: pulse.scheduledToday, icon: ClipboardCheck, color: "text-blue-600" },
                { label: "Completed today", value: pulse.completedToday, icon: CheckCircle2, color: "text-emerald-600" },
                { label: "Lessons published", value: pulse.publishedLessons, icon: BookOpen, color: "text-violet-600" },
                { label: "Alerts", value: pulse.alertsCount, icon: Bell, color: "text-amber-600" },
                { label: "Pending verifications", value: pulse.pendingVerifications, icon: ShieldCheck, color: "text-red-600" },
              ].map((m) => (
                <div key={m.label} className="rounded-xl border border-border bg-muted/20 p-4">
                  <div className="flex items-center gap-2">
                    <m.icon className={`size-4 ${m.color}`} />
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{m.label}</p>
                  </div>
                  <p className="mt-2 text-xl font-bold tabular-nums text-foreground">{m.value.toLocaleString()}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">Pulse data unavailable.</p>
          )}
          {dashboard && (
            <p className="mt-4 text-xs text-muted-foreground">
              Attendance {pct(dashboard.attendanceRate)} · Average performance {pct(dashboard.averagePerformance)} ·
              Curriculum progress {pct(dashboard.curriculumProgress)}
            </p>
          )}
        </section>

        {/* Quick actions */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <h2 className="mb-3 flex items-center gap-2 text-sm font-bold text-foreground">
            <Zap className="size-4 text-emerald-600" /> Quick Actions
          </h2>
          <div className="grid grid-cols-2 gap-2">
            {(dashboard?.quickActions ?? []).map((action) => (
              <Link
                key={action.id}
                href={action.route}
                title={action.description}
                className="flex flex-col items-start gap-1.5 rounded-xl border border-border bg-muted/20 p-3 text-xs font-semibold transition-colors hover:border-primary/20 hover:bg-muted"
              >
                <span className="text-primary">
                  {action.icon === "SCHOOL" ? <School className="size-5" />
                    : action.icon === "USERS" ? <Users className="size-5" />
                    : action.icon === "MAP" ? <MapPin className="size-5" />
                    : action.icon === "SHIELD" ? <ShieldCheck className="size-5" />
                    : action.icon === "DATABASE" ? <Database className="size-5" />
                    : action.icon === "AUDIT" ? <FileSearch className="size-5" />
                    : action.icon === "REPORT" ? <BarChart3 className="size-5" />
                    : <Zap className="size-5" />}
                </span>
                <span className="leading-tight">{action.label}</span>
              </Link>
            ))}
            {!loading && (dashboard?.quickActions?.length ?? 0) === 0 && (
              <p className="col-span-2 text-xs text-muted-foreground">No quick actions available.</p>
            )}
          </div>
        </section>
      </div>

      {/* Top districts drill-down */}
      {dashboard && dashboard.topDistricts.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground">
              <MapPin className="size-4 text-primary" /> Districts in your jurisdiction
            </h2>
            <Link href="/dashboard/regional-admin/districts" className="text-xs font-semibold text-primary hover:underline">
              View all →
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border text-[11px] uppercase tracking-widest text-muted-foreground">
                  <th className="px-3 py-2 font-bold">District</th>
                  <th className="px-3 py-2 font-bold">Institutions</th>
                  <th className="px-3 py-2 font-bold">Teachers</th>
                  <th className="px-3 py-2 font-bold">Learners</th>
                  <th className="px-3 py-2 font-bold">Attendance</th>
                  <th className="px-3 py-2 font-bold">Performance</th>
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody>
                {dashboard.topDistricts.map((d) => (
                  <tr key={d.id} className="border-b border-border/60 transition-colors hover:bg-muted/40">
                    <td className="px-3 py-2.5">
                      <p className="font-medium text-foreground">{d.name}</p>
                      <p className="text-xs text-muted-foreground">{d.code}</p>
                    </td>
                    <td className="px-3 py-2.5 tabular-nums">{d.institutionCount}</td>
                    <td className="px-3 py-2.5 tabular-nums">{d.teacherCount}</td>
                    <td className="px-3 py-2.5 tabular-nums">{d.learnerCount}</td>
                    <td className="px-3 py-2.5 tabular-nums">{pct(d.attendanceRate)}</td>
                    <td className="px-3 py-2.5 tabular-nums">{pct(d.averagePerformance)}</td>
                    <td className="px-3 py-2.5 text-right">
                      <Link href={`/dashboard/regional-admin/districts/${d.id}`} className="text-xs font-semibold text-primary hover:underline">
                        Open →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {loading && !dashboard && <Loader2 className="mx-auto size-6 animate-spin text-muted-foreground" />}
    </div>
  )
}
