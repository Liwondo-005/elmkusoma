"use client"

import { useTranslations } from "next-intl";
import { useEffect, useState } from "react"
import Link from "next/link"
import {
  Users, School, Video, Award, Shield, Activity, ArrowRight, Loader2, RefreshCw,
  AlertTriangle, Package, Bell, Globe, Radio, CreditCard, GraduationCap,
  FileCheck, Eye, Settings, Search, UserCog, Building2, Zap, TrendingUp,
  Clock, CheckCircle2, XCircle, AlertCircle, ShieldAlert, Crown
} from "lucide-react"
import { platformAdminApi, type PlatformDashboard, type AttentionItem, type ActivityFeed, type PlatformHealth, type EnhancedDashboard } from "@/lib/platform-admin-api"

// ── Skeleton ──
function SkeletonCard() {
  return <div className="rounded-2xl border border-border bg-card p-5 animate-pulse"><div className="h-10 w-10 rounded-xl bg-muted" /><div className="mt-4 h-7 w-20 rounded bg-muted" /><div className="mt-2 h-3 w-24 rounded bg-muted" /></div>
}
function SkeletonList() {
  return <div className="space-y-2 animate-pulse"><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /><div className="h-16 rounded-xl bg-muted" /></div>
}

// ── Primary KPI ──
function PrimaryKpi({ icon: Icon, label, value, sub, href, accent }: { icon: any; label: string; value: number; sub: string; href: string; accent: string }) {
  return (
    <Link href={href} className="group relative overflow-hidden rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md hover:border-primary/20 hover:-translate-y-0.5">
      <div className="absolute -right-6 -top-6 size-20 rounded-full opacity-[0.04] group-hover:opacity-[0.07] transition-opacity" style={{ background: accent }} />
      <div className="flex items-start justify-between">
        <div className="flex size-10 items-center justify-center rounded-xl border" style={{ background: `${accent}14`, borderColor: `${accent}22`, color: accent }}><Icon className="size-5" /></div>
        <span className="flex size-7 items-center justify-center rounded-full bg-muted text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity"><ArrowRight className="size-3.5" /></span>
      </div>
      <div className="mt-4">
        <p className="text-[11px] font-bold tracking-widest uppercase text-muted-foreground">{label}</p>
        <p className="mt-1 text-2xl font-bold tracking-tight text-foreground tabular-nums">{value.toLocaleString()}</p>
        <p className="mt-1 text-xs text-muted-foreground">{sub}</p>
      </div>
    </Link>
  )
}

// ── Operational ──
function OperationalCard({ icon: Icon, label, value, href, color, sub }: { icon: any; label: string; value: number; href: string; color: string; sub: string }) {
  return (
    <Link href={href} className={`group flex items-center gap-4 rounded-2xl border p-4 shadow-sm transition-all hover:shadow-md hover:-translate-y-0.5 ${color}`}>
      <div className="flex size-11 items-center justify-center rounded-xl bg-white/80 shadow-sm"><Icon className="size-5" /></div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-bold tracking-widest uppercase opacity-60">{label}</p>
        <p className="text-xl font-bold tabular-nums">{value.toLocaleString()}</p>
        <p className="text-xs opacity-70">{sub}</p>
      </div>
      <ArrowRight className="size-4 opacity-40 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
    </Link>
  )
}

// ── Attention ──
function AttentionCard({ item }: { item: AttentionItem }) {
  const map: Record<string, { bg: string; border: string; icon: any; iconBg: string }> = {
    HIGH: { bg: "bg-red-50", border: "border-red-200", icon: ShieldAlert, iconBg: "bg-red-500" },
    MEDIUM: { bg: "bg-amber-50", border: "border-amber-200", icon: AlertTriangle, iconBg: "bg-amber-500" },
    INFO: { bg: "bg-blue-50", border: "border-blue-200", icon: Bell, iconBg: "bg-blue-500" },
  }
  const c = map[item.severity] || map.INFO
  const Ico = c.icon
  return (
    <Link href={item.actionUrl} className={`group flex gap-3 rounded-xl border p-4 transition-all hover:shadow-sm ${c.bg} ${c.border}`}>
      <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${c.iconBg} text-white mt-0.5`}><Ico className="size-4" /></div>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2"><span className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-widest uppercase text-white ${c.iconBg}`}>{item.severity}</span><span className="text-xs text-muted-foreground">{item.category}</span></div>
        <p className="mt-1 text-sm font-semibold text-foreground">{item.title}</p>
        <p className="mt-0.5 text-xs leading-relaxed text-muted-foreground line-clamp-2">{item.description}</p>
      </div>
      <ArrowRight className="mt-2 size-4 shrink-0 text-muted-foreground opacity-40 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
    </Link>
  )
}

// ── Activity ──
function ActivityRow({ a }: { a: ActivityFeed }) {
  const col: Record<string, string> = { CREATE: "bg-emerald-500", UPDATE: "bg-blue-500", DELETE: "bg-red-500", LOGIN: "bg-violet-500" }
  return (
    <div className="flex gap-3 rounded-xl border border-border bg-card px-4 py-3 hover:bg-muted/40 transition-colors">
      <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg text-white text-xs font-bold ${col[a.action] || "bg-slate-400"}`}>{a.action?.charAt(0) || "?"}</div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground truncate">{a.description}</p>
        <p className="text-xs text-muted-foreground flex items-center gap-1.5"><span className="rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-bold uppercase">{a.entityType}</span><span className="truncate">{a.actorName}</span><span>·</span><span>{new Date(a.createdAt).toLocaleDateString()}</span></p>
      </div>
    </div>
  )
}

// ── Health ──
function HealthDot({ status }: { status: string }) {
  const m: Record<string, string> = { Operational: "bg-emerald-500 shadow-emerald-500/30", Degraded: "bg-amber-500 shadow-amber-500/30", Failing: "bg-red-500 shadow-red-500/30" }
  return <span className={`size-2 rounded-full shadow ${m[status] || "bg-slate-300"}`} />
}

export default function PlatformAdminDashboard() {
  const t = useTranslations("platformAdmin");
  const tc = useTranslations("common");
  const [dash, setDash] = useState<EnhancedDashboard | null>(null)
  const [attention, setAttention] = useState<AttentionItem[]>([])
  const [activity, setActivity] = useState<ActivityFeed[]>([])
  const [health, setHealth] = useState<PlatformHealth | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  async function load() {
    setLoading(true); setErr(null)
    try {
      const [d, a, act, h] = await Promise.allSettled([
        platformAdminApi.getEnhancedDashboard(), platformAdminApi.getAttention(),
        platformAdminApi.getActivity(0, 10), platformAdminApi.getHealth(),
      ])
      if (d.status === "fulfilled") setDash(d.value); else setErr(t("dashboard.unableToLoadDashboard"))
      if (a.status === "fulfilled") setAttention(a.value)
      if (act.status === "fulfilled") setActivity(act.value)
      if (h.status === "fulfilled") setHealth(h.value)
    } catch (e: any) { setErr(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  return (
    <div className="space-y-6 pb-8">
      {/* Hero */}
      <div className="relative overflow-hidden rounded-2xl border border-border bg-gradient-to-br from-primary via-primary to-primary/80 p-6 text-primary-foreground shadow-sm">
        <div className="absolute -right-12 -top-12 size-40 rounded-full bg-white/10" />
        <div className="absolute -bottom-10 -left-10 size-32 rounded-full bg-white/10" />
        <div className="absolute right-6 top-6 hidden lg:flex items-center gap-2 rounded-full bg-white/15 px-3 py-1.5 backdrop-blur">
          <span className="size-2 animate-pulse rounded-full bg-emerald-300" /><span className="text-xs font-semibold">{t("dashboard.operationsGovernance")}</span>
        </div>
        <div className="relative">
          <div className="flex items-center gap-2 text-xs font-bold tracking-widest uppercase text-white/70"><Crown className="size-3.5" /> {t("dashboard.platformAdmin")}</div>
          <h1 className="mt-1 text-2xl font-bold tracking-tight md:text-3xl">{t("dashboard.platformCommandCenter")}</h1>
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-white/80">{t("dashboard.realPlatformStateActionable")}</p>
          <div className="mt-4 flex flex-wrap gap-2">
            <button onClick={load} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary shadow-sm hover:bg-white/90 transition-colors"><RefreshCw className="size-4" /> {t("dashboard.refresh")}</button>
            <Link href="/dashboard/platform-admin/attention" className="inline-flex items-center gap-2 rounded-xl bg-white/15 px-4 py-2 text-sm font-semibold text-white backdrop-blur hover:bg-white/20 transition-colors"><AlertTriangle className="size-4" /> {t("dashboard.attention")}{attention.length})</Link>
          </div>
        </div>
      </div>

      {err && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between"><span className="flex items-center gap-2"><XCircle className="size-4" />{err}</span><button onClick={load} className="rounded-lg bg-white px-3 py-1 text-xs font-semibold border">{t("dashboard.retry")}</button></div>}

      {/* Primary KPIs */}
      <div>
        <h2 className="text-xs font-bold tracking-widest uppercase text-muted-foreground mb-3">{t("dashboard.platformOverview")}</h2>
        {loading ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[1, 2, 3, 4].map(i => <SkeletonCard key={i} />)}</div> : dash ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <PrimaryKpi icon={Users} label="Total Users" value={dash.totalUsers} sub={`${dash.totalStudents} students · ${dash.totalTeachers} educators`} href="/dashboard/platform-admin/users" accent="#2563eb" />
            <PrimaryKpi icon={Building2} label="Institutions" value={dash.totalInstitutions} sub="Schools · Colleges · TVET · Universities" href="/dashboard/platform-admin/institutions" accent="#059669" />
            <PrimaryKpi icon={Radio} label="Live Classes" value={dash.totalLiveClasses} sub={`${dash.activeLiveClasses} active now`} href="/dashboard/platform-admin/live-classes" accent="#7c3aed" />
            <PrimaryKpi icon={FileCheck} label="Certificates" value={dash.totalCertificates} sub="Issued & verified" href="/dashboard/platform-admin/certificates" accent="#d97706" />
          </div>
        ) : <div className="rounded-xl border border-dashed p-8 text-center text-sm text-muted-foreground">{t("dashboard.dataUnavailable")}</div>}
      </div>

      {/* Operational */}
      {loading ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[1, 2, 3, 4].map(i => <SkeletonCard key={i} />)}</div> : dash ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <OperationalCard icon={AlertTriangle} label="Open Incidents" value={dash.openIncidents || 0} sub="Requires resolution" href="/dashboard/platform-admin/incidents" color="bg-red-50 border-red-200 text-red-700" />
          <OperationalCard icon={ShieldAlert} label="Pending Verification" value={dash.pendingVerifications || 0} sub="Awaiting review" href="/dashboard/platform-admin/verifications" color="bg-amber-50 border-amber-200 text-amber-700" />
          <OperationalCard icon={Package} label="Active Services" value={dash.activeServices || 0} sub="Enabled catalogue" href="/dashboard/platform-admin/services" color="bg-teal-50 border-teal-200 text-teal-700" />
          <OperationalCard icon={Bell} label="Notifications" value={dash.totalNotifications || 0} sub="Platform broadcasts" href="/dashboard/platform-admin/communications" color="bg-violet-50 border-violet-200 text-violet-700" />
        </div>
      ) : null}

      {/* Attention — most prominent */}
      <div className="rounded-2xl border border-amber-200 bg-gradient-to-br from-amber-50/50 to-white p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="flex items-center gap-2 text-base font-bold text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-amber-500 text-white"><AlertTriangle className="size-4" /></span> {t("dashboard.attentionRequired")}</h2>
          <div className="flex items-center gap-2">
            {attention.length > 0 && <span className="rounded-full bg-red-500 px-2.5 py-1 text-xs font-bold text-white">{attention.length}</span>}
            <Link href="/dashboard/platform-admin/attention" className="text-xs font-semibold text-primary hover:underline">{t("dashboard.viewAll")} →</Link>
          </div>
        </div>
        {loading ? <SkeletonList /> : attention.length === 0 ? (
          <div className="rounded-xl border border-dashed border-emerald-200 bg-emerald-50/50 p-8 text-center">
            <CheckCircle2 className="mx-auto size-10 text-emerald-500" />
            <p className="mt-3 text-sm font-semibold text-foreground">{t("dashboard.allClear")}</p>
            <p className="mt-1 text-xs text-muted-foreground max-w-md mx-auto">{t("dashboard.noItemsRequiringAttention")}</p>
          </div>
        ) : (
          <div className="grid gap-3 md:grid-cols-2">{attention.slice(0, 6).map((item, i) => <AttentionCard key={i} item={item} />)}</div>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Activity */}
        <div className="lg:col-span-2 rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="flex items-center gap-2 text-sm font-bold tracking-tight text-foreground"><Activity className="size-4 text-primary" /> {t("dashboard.platformActivity")}</h2>
            <Link href="/dashboard/platform-admin/audit" className="text-xs font-semibold text-primary hover:underline">{t("dashboard.auditLogs")} →</Link>
          </div>
          {loading ? <SkeletonList /> : activity.length === 0 ? (
            <div className="rounded-xl border border-dashed p-8 text-center"><Activity className="mx-auto size-8 text-muted-foreground" /><p className="mt-3 text-sm text-muted-foreground">{t("dashboard.noRecentActivity")}</p><p className="mt-1 text-xs text-muted-foreground">{t("dashboard.verifiedActionsWillAppear")}</p></div>
          ) : (
            <div className="space-y-2">{activity.slice(0, 8).map(a => <ActivityRow key={a.id} a={a} />)}</div>
          )}
        </div>

        {/* Health + Ecosystem + Quick Actions */}
        <div className="space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground mb-4"><Zap className="size-4 text-emerald-600" /> {t("dashboard.platformHealth")}</h2>
            {health ? (
              <>
                <div className="space-y-2">
                  <div className="flex items-center justify-between rounded-xl border px-4 py-3 bg-muted/20"><span className="flex items-center gap-2 text-sm font-medium"><HealthDot status={health.databaseStatus} /> {t("dashboard.database")}</span><span className="text-xs font-semibold">{health.databaseStatus}</span></div>
                  <div className="flex items-center justify-between rounded-xl border px-4 py-3 bg-muted/20"><span className="flex items-center gap-2 text-sm font-medium"><HealthDot status={health.apiStatus} /> API</span><span className="text-xs font-semibold">{health.apiStatus}</span></div>
                </div>
                <div className="mt-3 grid grid-cols-2 gap-2">
                  <div className="rounded-xl bg-primary/5 border border-primary/10 p-3 text-center"><p className="text-lg font-bold text-primary tabular-nums">{health.activeUsers.toLocaleString()}</p><p className="text-xs text-muted-foreground">{t("dashboard.activeUsers")}</p></div>
                  <div className="rounded-xl bg-emerald-50 border border-emerald-100 p-3 text-center"><p className="text-lg font-bold text-emerald-700 tabular-nums">{health.activeInstitutions.toLocaleString()}</p><p className="text-xs text-muted-foreground">{t("dashboard.activeInstitutions")}</p></div>
                </div>
                <p className="mt-3 text-xs leading-relaxed text-muted-foreground">{t("dashboard.statusesAreDerivedFrom")}</p>
              </>
            ) : loading ? <SkeletonList /> : <p className="text-sm text-muted-foreground">{t("dashboard.healthStatusUnavailable")}</p>}
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="text-sm font-bold text-foreground mb-3">{t("dashboard.ecosystemOverview")}</h2>
            {dash ? (
              <div className="space-y-3 text-sm">
                <div className="flex justify-between"><span className="text-muted-foreground">{t("dashboard.institutions")}</span><span className="font-semibold">{dash.totalInstitutions}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("dashboard.providersPending")}</span><span className="font-semibold">{dash.pendingVerifications}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("dashboard.liveCertificates")}</span><span className="font-semibold">{dash.totalLiveClasses} · {dash.totalCertificates}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("dashboard.commerce")}</span><span className="font-semibold">{t("dashboard.payments", { p0: dash.totalPayments })}</span></div>
                <div className="pt-3 border-t flex flex-wrap gap-1.5 text-xs"><span className="rounded-full bg-muted px-2 py-1">{t("dashboard.institutions2")}</span><span className="rounded-full bg-muted px-2 py-1">{t("dashboard.providers")}</span><span className="rounded-full bg-muted px-2 py-1">{t("dashboard.learning")}</span><span className="rounded-full bg-muted px-2 py-1">{t("dashboard.live")}</span><span className="rounded-full bg-muted px-2 py-1">{t("dashboard.commerce2")}</span></div>
              </div>
            ) : <p className="text-xs text-muted-foreground">{t("dashboard.dataUnavailable2")}</p>}
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="flex items-center gap-2 text-sm font-bold text-foreground mb-3"><Crown className="size-4 text-primary" /> {t("dashboard.quickActions")}</h2>
            <div className="grid grid-cols-2 gap-2">
              {[
                { label: t("dashboard.users"), href: "/dashboard/platform-admin/users", icon: Users },
                { label: t("dashboard.institutions3"), href: "/dashboard/platform-admin/institutions", icon: School },
                { label: t("dashboard.providers2"), href: "/dashboard/platform-admin/providers", icon: Globe },
                { label: t("dashboard.services"), href: "/dashboard/platform-admin/services", icon: Package },
                { label: t("dashboard.verifications"), href: "/dashboard/platform-admin/verifications", icon: Shield },
                { label: t("dashboard.live2"), href: "/dashboard/platform-admin/live-classes", icon: Video },
                { label: t("dashboard.incidents"), href: "/dashboard/platform-admin/incidents", icon: AlertTriangle },
                { label: tc("search"), href: "/dashboard/platform-admin/search", icon: Search },
              ].map(a => (
                <Link key={a.href} href={a.href} className="flex flex-col items-center gap-1.5 rounded-xl border border-border bg-muted/20 p-3 text-xs font-semibold hover:bg-muted hover:border-primary/20 transition-colors">
                  <a.icon className="size-5 text-primary" />{a.label}
                </Link>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Bottom nav cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Link href="/dashboard/platform-admin/security" className="group rounded-2xl border border-border bg-card p-5 hover:shadow-md transition-all"><div className="flex size-10 items-center justify-center rounded-xl bg-red-50 text-red-600"><Shield className="size-5" /></div><p className="mt-3 text-sm font-bold">{t("dashboard.securityCenter")}</p><p className="text-xs text-muted-foreground">{t("dashboard.eventsResolutionsAudit")}</p></Link>
        <Link href="/dashboard/platform-admin/config" className="group rounded-2xl border border-border bg-card p-5 hover:shadow-md transition-all"><div className="flex size-10 items-center justify-center rounded-xl bg-slate-100 text-slate-700"><Settings className="size-5" /></div><p className="mt-3 text-sm font-bold">{t("dashboard.platformConfiguration")}</p><p className="text-xs text-muted-foreground">{t("dashboard.flagsPoliciesMaintenance")}</p></Link>
        <Link href="/dashboard/platform-admin/audit" className="group rounded-2xl border border-border bg-card p-5 hover:shadow-md transition-all"><div className="flex size-10 items-center justify-center rounded-xl bg-violet-50 text-violet-600"><Eye className="size-5" /></div><p className="mt-3 text-sm font-bold">{t("dashboard.auditCompliance")}</p><p className="text-xs text-muted-foreground">{t("dashboard.actorActionResourceResult")}</p></Link>
      </div>
    </div>
  )
}
