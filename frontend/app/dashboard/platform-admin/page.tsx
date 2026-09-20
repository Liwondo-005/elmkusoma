"use client"

import { useEffect, useState } from "react"
import { Users, School, Video, Award, Shield, Activity, ArrowRight, Loader2, RefreshCw, AlertTriangle, Package, Bell } from "lucide-react"
import Link from "next/link"
import { platformAdminApi, type PlatformDashboard, type AttentionItem, type ActivityFeed, type PlatformHealth, type EnhancedDashboard } from "@/lib/platform-admin-api"

function StatCard({ icon: Icon, label, value, href, color }: { icon: any; label: string; value: number; href: string; color: string }) {
  return (
    <Link href={href} className="group relative overflow-hidden rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md hover:border-primary/30">
      <div className="flex items-start justify-between">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}><Icon className="size-5" /></div>
        <ArrowRight className="size-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
      </div>
      <div className="mt-4">
        <p className="text-2xl font-bold tracking-tight text-foreground">{value.toLocaleString()}</p>
        <p className="mt-1 text-sm text-muted-foreground">{label}</p>
      </div>
    </Link>
  )
}

function AttentionCard({ item }: { item: AttentionItem }) {
  const colors: Record<string, string> = { HIGH: "bg-red-50 border-red-200 text-red-700", MEDIUM: "bg-amber-50 border-amber-200 text-amber-700", INFO: "bg-blue-50 border-blue-200 text-blue-700" }
  return (
    <Link href={item.actionUrl} className={`flex items-start gap-3 rounded-xl border p-4 transition-all hover:shadow-sm ${colors[item.severity] || colors.INFO}`}>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold">{item.title}</p>
        <p className="mt-0.5 text-xs opacity-80">{item.description}</p>
      </div>
      <ArrowRight className="mt-1 size-3.5 shrink-0 opacity-60" />
    </Link>
  )
}

function ActivityRow({ activity }: { activity: ActivityFeed }) {
  const colors: Record<string, string> = { CREATE: "text-green-600", UPDATE: "text-blue-600", DELETE: "text-red-600", LOGIN: "text-purple-600" }
  return (
    <div className="flex items-center gap-3 rounded-lg border border-border px-4 py-3 hover:bg-muted/50">
      <div className={`flex size-8 items-center justify-center rounded-lg bg-muted text-xs font-bold ${colors[activity.action] || "text-muted-foreground"}`}>{activity.action?.charAt(0) || "?"}</div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground truncate">{activity.description}</p>
        <p className="text-xs text-muted-foreground">{activity.entityType}</p>
      </div>
    </div>
  )
}

function HealthBar({ label, status }: { label: string; status: string }) {
  const c: Record<string, string> = { Operational: "bg-green-500", Degraded: "bg-amber-500", Failing: "bg-red-500" }
  return (
    <div className="flex items-center justify-between rounded-lg border border-border px-4 py-2.5">
      <div className="flex items-center gap-2.5"><div className={`size-2 rounded-full ${c[status] || "bg-gray-400"}`} /><span className="text-sm font-medium text-foreground">{label}</span></div>
      <span className="text-xs text-muted-foreground">{status}</span>
    </div>
  )
}

export default function PlatformAdminDashboard() {
  const [dash, setDash] = useState<EnhancedDashboard | null>(null)
  const [attention, setAttention] = useState<AttentionItem[]>([])
  const [activity, setActivity] = useState<ActivityFeed[]>([])
  const [health, setHealth] = useState<PlatformHealth | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    try {
      const [d, a, act, h] = await Promise.allSettled([
        platformAdminApi.getEnhancedDashboard(), platformAdminApi.getAttention(),
        platformAdminApi.getActivity(0, 10), platformAdminApi.getHealth(),
      ])
      if (d.status === "fulfilled") setDash(d.value)
      if (a.status === "fulfilled") setAttention(a.value)
      if (act.status === "fulfilled") setActivity(act.value)
      if (h.status === "fulfilled") setHealth(h.value)
    } catch {} finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <div className="text-center"><Loader2 className="mx-auto size-8 animate-spin text-primary" /><p className="mt-3 text-sm text-muted-foreground">Loading platform data...</p></div>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Platform Command Center</h1>
          <p className="mt-1 text-sm text-muted-foreground">ELMKUSOMA Operations &amp; Governance Center</p>
        </div>
        <button onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2 text-sm font-medium text-foreground hover:bg-muted transition-colors">
          <RefreshCw className="size-4" /> Refresh
        </button>
      </div>

      {dash && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard icon={Users} label="Total Users" value={dash.totalUsers} href="/dashboard/platform-admin/users" color="bg-blue-100 text-blue-700" />
          <StatCard icon={School} label="Institutions" value={dash.totalInstitutions} href="/dashboard/platform-admin/institutions" color="bg-emerald-100 text-emerald-700" />
          <StatCard icon={Video} label="Live Classes" value={dash.totalLiveClasses} href="/dashboard/platform-admin/live-classes" color="bg-purple-100 text-purple-700" />
          <StatCard icon={Award} label="Certificates" value={dash.totalCertificates} href="/dashboard/platform-admin/certificates" color="bg-amber-100 text-amber-700" />
        </div>
      )}

      {dash && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard icon={Users} label="Students" value={dash.totalStudents} href="/dashboard/platform-admin/users?role=STUDENT" color="bg-cyan-100 text-cyan-700" />
          <StatCard icon={Users} label="Teachers" value={dash.totalTeachers} href="/dashboard/platform-admin/users?role=TEACHER" color="bg-indigo-100 text-indigo-700" />
          <StatCard icon={Users} label="Parents" value={dash.totalParents} href="/dashboard/platform-admin/users?role=PARENT" color="bg-pink-100 text-pink-700" />
          <StatCard icon={Video} label="Active Live" value={dash.activeLiveClasses} href="/dashboard/platform-admin/live-classes?status=LIVE" color="bg-red-100 text-red-700" />
        </div>
      )}

      {dash && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard icon={AlertTriangle} label="Open Incidents" value={dash.openIncidents || 0} href="/dashboard/platform-admin/incidents" color="bg-red-100 text-red-700" />
          <StatCard icon={Package} label="Active Services" value={dash.activeServices || 0} href="/dashboard/platform-admin/services" color="bg-teal-100 text-teal-700" />
          <StatCard icon={Bell} label="Notifications" value={dash.totalNotifications || 0} href="/dashboard/platform-admin/communications" color="bg-violet-100 text-violet-700" />
          <StatCard icon={Shield} label="Pending Verifications" value={dash.pendingVerifications || 0} href="/dashboard/platform-admin/verifications" color="bg-amber-100 text-amber-700" />
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold text-foreground">Attention Required</h2>
              {attention.length > 0 && <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-bold text-red-700">{attention.length}</span>}
            </div>
            {attention.length === 0 ? (
              <div className="rounded-xl border border-dashed border-border p-8 text-center">
                <Shield className="mx-auto size-8 text-green-500" />
                <p className="mt-3 text-sm font-medium text-foreground">All Clear</p>
                <p className="mt-1 text-xs text-muted-foreground">No items requiring attention</p>
              </div>
            ) : (
              <div className="space-y-2">{attention.map((item, i) => <AttentionCard key={i} item={item} />)}</div>
            )}
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h2 className="text-base font-semibold text-foreground mb-4">Recent Activity</h2>
            {activity.length === 0 ? (
              <div className="rounded-xl border border-dashed border-border p-8 text-center">
                <Activity className="mx-auto size-8 text-muted-foreground" />
                <p className="mt-3 text-sm text-muted-foreground">No recent activity</p>
              </div>
            ) : (
              <div className="space-y-2">{activity.map((a) => <ActivityRow key={a.id} activity={a} />)}</div>
            )}
          </div>
        </div>

        <div className="space-y-4">
          {health && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground mb-4">Platform Health</h2>
              <div className="space-y-2">
                <HealthBar label="Database" status={health.databaseStatus} />
                <HealthBar label="API" status={health.apiStatus} />
              </div>
              <div className="mt-4 rounded-xl bg-muted/50 p-3">
                <div className="grid grid-cols-2 gap-2 text-center">
                  <div><p className="text-lg font-bold text-foreground">{health.activeUsers.toLocaleString()}</p><p className="text-xs text-muted-foreground">Active Users</p></div>
                  <div><p className="text-lg font-bold text-foreground">{health.activeInstitutions.toLocaleString()}</p><p className="text-xs text-muted-foreground">Active Institutions</p></div>
                </div>
              </div>
            </div>
          )}

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h2 className="text-base font-semibold text-foreground mb-4">Quick Actions</h2>
            <div className="space-y-2">
              {[
                { label: "Manage Users", href: "/dashboard/platform-admin/users", icon: Users },
                { label: "Review Institutions", href: "/dashboard/platform-admin/institutions", icon: School },
                { label: "Service Catalogue", href: "/dashboard/platform-admin/services", icon: Package },
                { label: "Pending Verifications", href: "/dashboard/platform-admin/verifications", icon: Shield },
                { label: "Security Center", href: "/dashboard/platform-admin/security", icon: Shield },
                { label: "Audit Logs", href: "/dashboard/platform-admin/audit", icon: Activity },
              ].map((action) => (
                <Link key={action.href} href={action.href} className="flex items-center gap-3 rounded-xl border border-border px-4 py-3 text-sm font-medium text-foreground hover:bg-muted transition-colors">
                  <action.icon className="size-4 text-muted-foreground" />{action.label}
                  <ArrowRight className="ml-auto size-3.5 text-muted-foreground" />
                </Link>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
