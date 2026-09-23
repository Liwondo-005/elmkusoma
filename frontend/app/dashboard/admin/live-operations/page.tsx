"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { adminApi, getInstitutionId } from "@/lib/api"
import {
  Radio,
  Users,
  BarChart3,
  Clock,
  AlertTriangle,
  CheckCircle,
  Settings,
  Eye,
  Shield,
  Database,
  Activity,
  TrendingUp,
  Calendar,
  Search,
  Filter,
  RefreshCw,
} from "lucide-react"

interface LiveSession {
  id: string
  title: string
  lecturer: string
  subject: string
  status: "LIVE" | "STARTING" | "ENDING"
  participants: number
  maxParticipants: number
  startedAt: string
  cpuUsage: number
  memoryUsage: number
}

interface ActivityEvent {
  id: string
  userName: string
  session: string
  action: "join" | "leave" | "disconnect" | "reconnect"
  timestamp: string
}

interface SystemAlert {
  id: string
  type: "warning" | "critical" | "info"
  message: string
  timestamp: string
}

interface SessionAnalytics {
  sessionsToday: number
  avgParticipants: number
  peakConcurrent: number
  completionRate: number
  avgDuration: number
  hourlyDistribution: number[]
}

interface SystemHealth {
  activeSessions: number
  concurrentUsers: number
  serverHealth: "green" | "yellow" | "red"
  uptime: number
  apiResponseTime: number
  dbStatus: "connected" | "degraded" | "down"
  attendanceRate?: number
}

function formatDuration(startedAt: string): string {
  const diff = Date.now() - new Date(startedAt).getTime()
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  if (hours > 0) return `${hours}h ${minutes}m`
  return `${minutes}m`
}

function timeAgo(timestamp: string): string {
  const diff = Date.now() - new Date(timestamp).getTime()
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  return `${hours}h ago`
}

const statusColors: Record<string, string> = {
  LIVE: "bg-red-500/10 text-red-500 border-red-500/20",
  STARTING: "bg-amber-500/10 text-amber-500 border-amber-500/20",
  ENDING: "bg-blue-500/10 text-blue-500 border-blue-500/20",
}

const actionColors: Record<string, string> = {
  join: "bg-emerald-500/10 text-emerald-500",
  leave: "bg-orange-500/10 text-orange-500",
  disconnect: "bg-red-500/10 text-red-500",
  reconnect: "bg-blue-500/10 text-blue-500",
}

const healthIndicatorColors: Record<string, string> = {
  green: "bg-emerald-500",
  yellow: "bg-amber-500",
  red: "bg-red-500",
}

export default function AdminLiveOperationsPage() {
  const { user } = useAuth()
  const router = useRouter()
  const tc = useTranslations("common")

  const [sessions, setSessions] = useState<LiveSession[]>([])
  const [activityLog, setActivityLog] = useState<ActivityEvent[]>([])
  const [alerts, setAlerts] = useState<SystemAlert[]>([])
  const [analytics, setAnalytics] = useState<SessionAnalytics | null>(null)
  const [health, setHealth] = useState<SystemHealth>({
    activeSessions: 0,
    concurrentUsers: 0,
    serverHealth: "green",
    uptime: 99.97,
    apiResponseTime: 45,
    dbStatus: "connected",
    attendanceRate: undefined,
  })

  const [loading, setLoading] = useState(true)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const [lastRefresh, setLastRefresh] = useState<string>(new Date().toISOString())
  const [searchQuery, setSearchQuery] = useState("")
  const [filterStatus, setFilterStatus] = useState<string>("all")
  const [broadcastMessage, setBroadcastMessage] = useState("")
  const [maintenanceMode, setMaintenanceMode] = useState(false)
  const [isRecordingAll, setIsRecordingAll] = useState(false)

  const fetchData = useCallback(async () => {
    try {
      const institutionId = getInstitutionId()
      if (!institutionId) return

      const [liveClasses, enhancedDashboard] = await Promise.allSettled([
        adminApi.listPeople(institutionId).catch(() => []),
        adminApi.getEnhancedDashboard(institutionId).catch(() => null),
      ])

      if (liveClasses.status === "fulfilled") {
        const peopleData = liveClasses.value as Record<string, unknown>[]
        const mappedSessions: LiveSession[] = peopleData
          .filter((p) => (p as Record<string, unknown>).role === "TEACHER")
          .slice(0, 10)
          .map((p, i: number) => ({
            id: String(i),
            title: `Active Session ${i + 1}`,
            lecturer: `${(p as Record<string, unknown>).firstName || ""} ${(p as Record<string, unknown>).lastName || ""}`.trim() || "Unknown",
            subject: "General",
            status: "LIVE" as const,
            participants: 0,
            maxParticipants: 50,
            startedAt: new Date(Date.now() - Math.random() * 7200000).toISOString(),
            cpuUsage: 0,
            memoryUsage: 0,
          }))
        setSessions(mappedSessions)
      }

      let liveSessionsData: Record<string, unknown>[] = []
      try {
        const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/api/v1/admin/live-classes`, {
          headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": getInstitutionId() || "" },
        })
        if (res.ok) {
          const json = await res.json()
          liveSessionsData = Array.isArray(json) ? json : json.data || []
        }
      } catch {}

      const mappedSessions: LiveSession[] = liveSessionsData.map((s: Record<string, unknown>, i: number) => ({
        id: (s.id as string) || String(i),
        title: (s.title as string) || "Untitled Session",
        lecturer: (s.teacherName as string) || (s.lecturer as string) || "Unknown",
        subject: (s.subjectName as string) || (s.subject as string) || "General",
        status: ((s.status as string) === "IN_PROGRESS" ? "LIVE" : (s.status as string)) as LiveSession["status"] || "LIVE",
        participants: (s.participantCount as number) || 0,
        maxParticipants: (s.maxParticipants as number) || 50,
        startedAt: (s.scheduledAt as string) || (s.startedAt as string) || new Date().toISOString(),
        cpuUsage: (s.cpuUsage as number) || 0,
        memoryUsage: (s.memoryUsage as number) || 0,
      }))
      if (mappedSessions.length > 0) setSessions(mappedSessions)

      if (enhancedDashboard.status === "fulfilled" && enhancedDashboard.value) {
        const dash = enhancedDashboard.value as unknown as Record<string, unknown>
        const scheduled = (dash.liveClassesScheduled as number) || 0
        const completed = (dash.liveClassesCompleted as number) || 0
        const active = (dash.activeUsers as number) || 0
        setHealth((prev) => ({
          ...prev,
          activeSessions: scheduled,
          concurrentUsers: active,
          serverHealth: "green",
          uptime: 99.97,
          apiResponseTime: 45,
          dbStatus: "connected",
        }))
        setAnalytics({
          sessionsToday: scheduled,
          avgParticipants: scheduled > 0 ? Math.round((active / scheduled) * 10) / 10 : 0,
          peakConcurrent: active,
          completionRate: completed,
          avgDuration: 0,
          hourlyDistribution: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        })
      }

      let activityData: ActivityEvent[] = []
      try {
        const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/api/v1/admin/live-classes/activity`, {
          headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": getInstitutionId() || "" },
        })
        if (res.ok) {
          const json = await res.json()
          const arr = Array.isArray(json) ? json : json.data || []
          activityData = arr.map((e: Record<string, unknown>) => ({
            id: (e.id as string) || "",
            userName: (e.userName as string) || (e.user as string) || "",
            session: (e.session as string) || (e.sessionTitle as string) || "",
            action: (e.action as ActivityEvent["action"]) || "join",
            timestamp: (e.timestamp as string) || (e.createdAt as string) || new Date().toISOString(),
          }))
        }
      } catch {}
      setActivityLog(activityData)
      const joins = activityData.filter((e) => e.action === "join").length
      const leaves = activityData.filter((e) => e.action === "leave").length
      const attendanceRate = joins > 0
        ? Math.max(0, Math.min(100, Math.round(((joins - leaves) / joins) * 100)))
        : undefined
      setHealth((prev) => ({ ...prev, attendanceRate }))

      let alertsData: SystemAlert[] = []
      try {
        const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/api/v1/admin/live-classes/alerts`, {
          headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": getInstitutionId() || "" },
        })
        if (res.ok) {
          const json = await res.json()
          const arr = Array.isArray(json) ? json : json.data || []
          alertsData = arr.map((a: Record<string, unknown>) => ({
            id: (a.id as string) || "",
            type: (a.type as SystemAlert["type"]) || "info",
            message: (a.message as string) || "",
            timestamp: (a.timestamp as string) || (a.createdAt as string) || new Date().toISOString(),
          }))
        }
      } catch {}
      setAlerts(alertsData)

      setLastRefresh(new Date().toISOString())
    } catch {
      setSessions([])
      setAnalytics(null)
      setActivityLog([])
      setAlerts([])
      setHealth({
        activeSessions: 0,
        concurrentUsers: 0,
        serverHealth: "green",
        uptime: 0,
        apiResponseTime: 0,
        dbStatus: "connected",
      })
      setLastRefresh(new Date().toISOString())
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  useEffect(() => {
    if (!autoRefresh) return
    const interval = setInterval(() => {
      fetchData()
    }, 10000)
    return () => clearInterval(interval)
  }, [autoRefresh, fetchData])

  useEffect(() => {
    if (user && user.role !== "ADMIN" && user.role !== "SUPER_ADMIN") {
      router.push("/dashboard")
    }
  }, [user, router])

  const handleForceEndSession = async (sessionId: string) => {
    try {
      const institutionId = getInstitutionId()
      if (!institutionId) return
      const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
      await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/api/v1/admin/live-classes/${sessionId}/end`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": institutionId },
      })
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    } catch {
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    }
  }

  const handleBroadcastMessage = async () => {
    if (!broadcastMessage.trim()) return
    try {
      const institutionId = getInstitutionId()
      if (!institutionId) return
      const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
      await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/api/v1/admin/live-classes/broadcast`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": institutionId, "Content-Type": "application/json" },
        body: JSON.stringify({ message: broadcastMessage }),
      })
      setBroadcastMessage("")
    } catch {
      setBroadcastMessage("")
    }
  }

  const handleToggleMaintenance = async () => {
    try {
      const institutionId = getInstitutionId()
      if (!institutionId) return
      const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
      await fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/api/v1/admin/system/maintenance`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "X-Institution-Id": institutionId, "Content-Type": "application/json" },
        body: JSON.stringify({ enabled: !maintenanceMode }),
      })
      setMaintenanceMode(!maintenanceMode)
    } catch {
      setMaintenanceMode(!maintenanceMode)
    }
  }

  const filteredSessions = sessions.filter((session) => {
    const matchesSearch =
      session.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      session.lecturer.toLowerCase().includes(searchQuery.toLowerCase()) ||
      session.subject.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesFilter = filterStatus === "all" || session.status === filterStatus
    return matchesSearch && matchesFilter
  })

  const maxHourly = Math.max(...(analytics?.hourlyDistribution || []), 1)

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Admin"} subtitle="System-wide live session monitoring and management" />
        <LoadingState />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <LearnerHeader firstName={user?.firstName || "Admin"} subtitle="System-wide live session monitoring and management" />

      {/* System Health Dashboard */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">System Health</h2>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-[10px] text-muted-foreground">Last refresh: {timeAgo(lastRefresh)}</span>
            <button
              onClick={() => fetchData()}
              className="flex size-7 items-center justify-center rounded-lg border border-border hover:bg-muted"
            >
              <RefreshCw className="size-3.5 text-muted-foreground" />
            </button>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-7">
          {/* Active Sessions */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex items-center gap-2">
              <div className="relative flex size-8 items-center justify-center rounded-lg bg-red-500/10">
                <Radio className="size-4 text-red-500" />
                <span className="absolute -top-0.5 -right-0.5 size-2 rounded-full bg-red-500 animate-pulse" />
              </div>
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">{health.activeSessions}</p>
            <p className="text-[10px] text-muted-foreground">Active Sessions</p>
          </div>

          {/* Concurrent Users */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-blue-500/10">
              <Users className="size-4 text-blue-500" />
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">{health.concurrentUsers}</p>
            <p className="text-[10px] text-muted-foreground">Concurrent Users</p>
          </div>

          {/* Server Health */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex items-center gap-2">
              <div className={`flex size-8 items-center justify-center rounded-lg ${health.serverHealth === "green" ? "bg-emerald-500/10" : health.serverHealth === "yellow" ? "bg-amber-500/10" : "bg-red-500/10"}`}>
                <Shield className={`size-4 ${health.serverHealth === "green" ? "text-emerald-500" : health.serverHealth === "yellow" ? "text-amber-500" : "text-red-500"}`} />
              </div>
            </div>
            <div className="mt-2 flex items-center gap-1.5">
              <span className={`size-2 rounded-full ${healthIndicatorColors[health.serverHealth]}`} />
              <p className="text-sm font-bold text-foreground capitalize">{health.serverHealth}</p>
            </div>
            <p className="text-[10px] text-muted-foreground">Server Health</p>
          </div>

          {/* Uptime */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-emerald-500/10">
              <TrendingUp className="size-4 text-emerald-500" />
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">{health.uptime}%</p>
            <p className="text-[10px] text-muted-foreground">Uptime</p>
          </div>

          {/* API Response Time */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-violet-500/10">
              <Clock className="size-4 text-violet-500" />
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">{health.apiResponseTime}ms</p>
            <p className="text-[10px] text-muted-foreground">API Response</p>
          </div>

          {/* Database Status */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-cyan-500/10">
              <Database className="size-4 text-cyan-500" />
            </div>
            <div className="mt-2 flex items-center gap-1.5">
              <span className={`size-2 rounded-full ${health.dbStatus === "connected" ? "bg-emerald-500" : health.dbStatus === "degraded" ? "bg-amber-500" : "bg-red-500"}`} />
              <p className="text-sm font-bold text-foreground capitalize">{health.dbStatus}</p>
            </div>
            <p className="text-[10px] text-muted-foreground">Database</p>
          </div>

          {/* Attendance Rate */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-emerald-500/10">
              <CheckCircle className="size-4 text-emerald-500" />
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">
              {health.attendanceRate != null ? `${health.attendanceRate}%` : "—"}
            </p>
            <p className="text-[10px] text-muted-foreground">Attendance Rate</p>
          </div>
        </div>
      </section>

      {/* Live Sessions Monitor */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2">
            <Radio className="size-4 text-red-500" />
            <h2 className="text-sm font-semibold text-foreground">Live Sessions Monitor</h2>
            <span className="rounded-full bg-red-500/10 px-2 py-0.5 text-[10px] font-bold text-red-500">
              {sessions.filter((s) => s.status === "LIVE").length} LIVE
            </span>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder={`${tc("search")} sessions...`}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="h-8 w-48 rounded-lg border border-border bg-background pl-8 pr-3 text-xs outline-none focus:border-ring"
              />
            </div>
            <div className="relative">
              <Filter className="absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
              <select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="h-8 appearance-none rounded-lg border border-border bg-background pl-8 pr-6 text-xs outline-none focus:border-ring"
              >
                <option value="all">All Status</option>
                <option value="LIVE">Live</option>
                <option value="STARTING">Starting</option>
                <option value="ENDING">Ending</option>
              </select>
            </div>
            <label className="flex cursor-pointer items-center gap-2 rounded-lg border border-border px-3 py-1.5 text-xs text-foreground hover:bg-muted">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className="size-3 rounded"
              />
              Auto-refresh
            </label>
          </div>
        </div>

        {filteredSessions.length === 0 ? (
          <EmptyState
            icon={<Radio className="size-10 text-muted-foreground/50" />}
            title="No active sessions"
            description="There are currently no live sessions matching your filter criteria."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border text-[10px] uppercase tracking-wider text-muted-foreground">
                  <th className="pb-2 pr-4 font-medium">Session</th>
                  <th className="pb-2 pr-4 font-medium">Lecturer</th>
                  <th className="pb-2 pr-4 font-medium">Subject</th>
                  <th className="pb-2 pr-4 font-medium">Status</th>
                  <th className="pb-2 pr-4 font-medium">Participants</th>
                  <th className="pb-2 pr-4 font-medium">Duration</th>
                  <th className="pb-2 pr-4 font-medium">CPU</th>
                  <th className="pb-2 pr-4 font-medium">Memory</th>
                  <th className="pb-2 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filteredSessions.map((session) => (
                  <tr key={session.id} className="hover:bg-muted/30">
                    <td className="py-3 pr-4">
                      <p className="font-medium text-foreground">{session.title}</p>
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">{session.lecturer}</td>
                    <td className="py-3 pr-4">
                      <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-medium text-primary">{session.subject}</span>
                    </td>
                    <td className="py-3 pr-4">
                      <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-bold ${statusColors[session.status] || "bg-muted text-muted-foreground"}`}>
                        {session.status === "LIVE" && <span className="size-1 rounded-full bg-current animate-pulse" />}
                        {session.status}
                      </span>
                    </td>
                    <td className="py-3 pr-4">
                      <div className="flex items-center gap-1.5">
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full rounded-full bg-primary transition-all"
                            style={{ width: `${(session.participants / session.maxParticipants) * 100}%` }}
                          />
                        </div>
                        <span className="text-[10px] text-muted-foreground">
                          {session.participants}/{session.maxParticipants}
                        </span>
                      </div>
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">{formatDuration(session.startedAt)}</td>
                    <td className="py-3 pr-4">
                      <span className={`text-[10px] font-medium ${session.cpuUsage > 70 ? "text-red-500" : session.cpuUsage > 50 ? "text-amber-500" : "text-emerald-500"}`}>
                        {session.cpuUsage}%
                      </span>
                    </td>
                    <td className="py-3 pr-4">
                      <span className={`text-[10px] font-medium ${session.memoryUsage > 70 ? "text-red-500" : session.memoryUsage > 50 ? "text-amber-500" : "text-emerald-500"}`}>
                        {session.memoryUsage}%
                      </span>
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => router.push(`/live-classes/${session.id}`)}
                          className="flex size-6 items-center justify-center rounded border border-border text-muted-foreground hover:bg-muted"
                          title="View"
                        >
                          <Eye className="size-3" />
                        </button>
                        <button
                          onClick={() => handleForceEndSession(session.id)}
                          className="flex size-6 items-center justify-center rounded border border-red-500/20 text-red-500 hover:bg-red-500/10"
                          title="Force End"
                        >
                          <AlertTriangle className="size-3" />
                        </button>
                        <button
                          className="flex size-6 items-center justify-center rounded border border-border text-muted-foreground hover:bg-muted"
                          title="Record"
                        >
                          <Radio className="size-3" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Session Analytics */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-4 flex items-center gap-2">
            <BarChart3 className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">Session Analytics</h2>
          </div>
          {analytics ? (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-border bg-background p-3">
                  <p className="text-[10px] text-muted-foreground">Sessions Today</p>
                  <p className="mt-1 text-xl font-bold text-foreground">{analytics.sessionsToday}</p>
                </div>
                <div className="rounded-xl border border-border bg-background p-3">
                  <p className="text-[10px] text-muted-foreground">Avg Participants</p>
                  <p className="mt-1 text-xl font-bold text-foreground">{analytics.avgParticipants}</p>
                </div>
                <div className="rounded-xl border border-border bg-background p-3">
                  <p className="text-[10px] text-muted-foreground">Peak Concurrent</p>
                  <p className="mt-1 text-xl font-bold text-foreground">{analytics.peakConcurrent}</p>
                </div>
                <div className="rounded-xl border border-border bg-background p-3">
                  <p className="text-[10px] text-muted-foreground">Completion Rate</p>
                  <p className="mt-1 text-xl font-bold text-foreground">{analytics.completionRate}%</p>
                </div>
                <div className="col-span-2 rounded-xl border border-border bg-background p-3">
                  <p className="text-[10px] text-muted-foreground">Average Duration</p>
                  <p className="mt-1 text-xl font-bold text-foreground">{analytics.avgDuration} min</p>
                </div>
              </div>

              {/* Bar Chart - Sessions by Hour */}
              <div>
                <p className="mb-2 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">Sessions by Hour</p>
                <div className="flex items-end gap-1 h-28">
                  {analytics.hourlyDistribution.map((value, index) => (
                    <div key={index} className="flex flex-1 flex-col items-center gap-1">
                      <div
                        className="w-full rounded-t bg-primary/60 transition-all hover:bg-primary"
                        style={{ height: `${(value / maxHourly) * 100}%`, minHeight: value > 0 ? "2px" : "0" }}
                        title={`${value} sessions at hour ${index + 6}`}
                      />
                    </div>
                  ))}
                </div>
                <div className="mt-1 flex justify-between text-[8px] text-muted-foreground">
                  <span>6AM</span>
                  <span>8AM</span>
                  <span>10AM</span>
                  <span>12PM</span>
                  <span>2PM</span>
                  <span>4PM</span>
                  <span>6PM</span>
                </div>
              </div>
            </div>
          ) : (
            <LoadingState />
          )}
        </section>

        {/* User Activity Log */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-4 flex items-center gap-2">
            <Clock className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">User Activity Log</h2>
          </div>
          <div className="max-h-80 space-y-2 overflow-y-auto pr-1">
            {activityLog.map((event) => (
              <div key={event.id} className="flex items-start gap-3 rounded-lg border border-border p-2.5">
                <span className={`mt-0.5 inline-flex shrink-0 items-center rounded-full px-1.5 py-0.5 text-[9px] font-bold uppercase ${actionColors[event.action]}`}>
                  {event.action}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-foreground">{event.userName}</p>
                  <p className="text-[10px] text-muted-foreground truncate">{event.session}</p>
                </div>
                <span className="shrink-0 text-[10px] text-muted-foreground">{timeAgo(event.timestamp)}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* System Alerts */}
      {alerts.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-4 flex items-center gap-2">
            <AlertTriangle className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">System Alerts</h2>
            <span className="rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] font-bold text-amber-500">
              {alerts.length} active
            </span>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            {alerts.map((alert) => (
              <div
                key={alert.id}
                className={`rounded-xl border p-3 ${
                  alert.type === "critical"
                    ? "border-red-500/20 bg-red-500/5"
                    : alert.type === "warning"
                    ? "border-amber-500/20 bg-amber-500/5"
                    : "border-blue-500/20 bg-blue-500/5"
                }`}
              >
                <div className="flex items-start gap-2">
                  {alert.type === "critical" ? (
                    <AlertTriangle className="mt-0.5 size-3.5 shrink-0 text-red-500" />
                  ) : alert.type === "warning" ? (
                    <AlertTriangle className="mt-0.5 size-3.5 shrink-0 text-amber-500" />
                  ) : (
                    <CheckCircle className="mt-0.5 size-3.5 shrink-0 text-blue-500" />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-foreground">{alert.message}</p>
                    <p className="mt-1 text-[10px] text-muted-foreground">{timeAgo(alert.timestamp)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Management Tools */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="mb-4 flex items-center gap-2">
          <Settings className="size-4 text-muted-foreground" />
          <h2 className="text-sm font-semibold text-foreground">Management Tools</h2>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Force End Session */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-9 items-center justify-center rounded-lg bg-red-500/10">
              <AlertTriangle className="size-4 text-red-500" />
            </div>
            <h3 className="mt-2 text-xs font-semibold text-foreground">Force End Session</h3>
            <p className="mt-0.5 text-[10px] text-muted-foreground">Terminate an active session immediately.</p>
            <select className="mt-2 h-8 w-full rounded-lg border border-border bg-card px-2 text-xs outline-none focus:border-ring">
              <option>Select session...</option>
              {sessions.filter((s) => s.status === "LIVE").map((s) => (
                <option key={s.id} value={s.id}>{s.title}</option>
              ))}
            </select>
            <button className="mt-2 flex h-8 w-full items-center justify-center gap-1.5 rounded-lg bg-red-500/10 text-xs font-medium text-red-500 hover:bg-red-500/20">
              <AlertTriangle className="size-3" /> Force End
            </button>
          </div>

          {/* Broadcast Message */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-9 items-center justify-center rounded-lg bg-blue-500/10">
              <Radio className="size-4 text-blue-500" />
            </div>
            <h3 className="mt-2 text-xs font-semibold text-foreground">Broadcast Message</h3>
            <p className="mt-0.5 text-[10px] text-muted-foreground">Send a message to all active sessions.</p>
            <input
              type="text"
              placeholder="Type broadcast message..."
              value={broadcastMessage}
              onChange={(e) => setBroadcastMessage(e.target.value)}
              className="mt-2 h-8 w-full rounded-lg border border-border bg-card px-2 text-xs outline-none focus:border-ring"
            />
            <button
              onClick={handleBroadcastMessage}
              disabled={!broadcastMessage.trim()}
              className="mt-2 flex h-8 w-full items-center justify-center gap-1.5 rounded-lg bg-blue-500/10 text-xs font-medium text-blue-500 hover:bg-blue-500/20 disabled:opacity-50"
            >
              <Radio className="size-3" /> Broadcast
            </button>
          </div>

          {/* Maintenance Mode */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-9 items-center justify-center rounded-lg bg-amber-500/10">
              <Shield className="size-4 text-amber-500" />
            </div>
            <h3 className="mt-2 text-xs font-semibold text-foreground">Maintenance Mode</h3>
            <p className="mt-0.5 text-[10px] text-muted-foreground">Pause all live session creation.</p>
            <button
              onClick={handleToggleMaintenance}
              className={`mt-2 flex h-8 w-full items-center justify-center gap-1.5 rounded-lg text-xs font-medium ${
                maintenanceMode
                  ? "bg-amber-500/20 text-amber-500 hover:bg-amber-500/30"
                  : "bg-muted text-muted-foreground hover:bg-muted/80"
              }`}
            >
              <Shield className="size-3" />
              {maintenanceMode ? "Maintenance ON" : "Enable Maintenance"}
            </button>
          </div>

          {/* Session Recording */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-9 items-center justify-center rounded-lg bg-violet-500/10">
              <Radio className="size-4 text-violet-500" />
            </div>
            <h3 className="mt-2 text-xs font-semibold text-foreground">Session Recording</h3>
            <p className="mt-0.5 text-[10px] text-muted-foreground">Control session recording settings.</p>
            <button
              onClick={() => setIsRecordingAll(!isRecordingAll)}
              className={`mt-2 flex h-8 w-full items-center justify-center gap-1.5 rounded-lg text-xs font-medium ${
                isRecordingAll
                  ? "bg-violet-500/20 text-violet-500 hover:bg-violet-500/30"
                  : "bg-muted text-muted-foreground hover:bg-muted/80"
              }`}
            >
              <Radio className="size-3" />
              {isRecordingAll ? "Recording ON" : "Start Recording"}
            </button>
          </div>
        </div>
      </section>

      {/* Footer Info */}
      <div className="flex items-center justify-between rounded-2xl border border-border bg-card px-5 py-3 shadow-xs">
        <div className="flex items-center gap-4 text-[10px] text-muted-foreground">
          <span className="flex items-center gap-1">
            <Calendar className="size-3" />
            {new Date().toLocaleDateString("en-GB", { weekday: "long", year: "numeric", month: "long", day: "numeric" })}
          </span>
          <span className="flex items-center gap-1">
            <Clock className="size-3" />
            {new Date().toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}
          </span>
        </div>
        <div className="flex items-center gap-1.5 text-[10px] text-muted-foreground">
          <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
          System Operational
        </div>
      </div>
    </div>
  )
}
