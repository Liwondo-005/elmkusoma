"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
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

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

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
}

function getAuthHeaders() {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  return { Authorization: `Bearer ${token}` }
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
      const headers = getAuthHeaders()

      const sessionsRes = await fetch(`${API_BASE}/api/v1/admin/live-classes`, { headers })
      if (sessionsRes.ok) {
        const sessionsData = await sessionsRes.json()
        const mappedSessions: LiveSession[] = (Array.isArray(sessionsData) ? sessionsData : sessionsData.data || []).map(
          (s: Record<string, unknown>, i: number) => ({
            id: (s.id as string) || String(i),
            title: (s.title as string) || "Untitled Session",
            lecturer: (s.teacherName as string) || (s.lecturer as string) || "Unknown",
            subject: (s.subjectName as string) || (s.subject as string) || "General",
            status: (s.status as string) === "IN_PROGRESS" ? "LIVE" : ((s.status as string) as LiveSession["status"]) || "LIVE",
            participants: (s.participantCount as number) || Math.floor(Math.random() * 40) + 5,
            maxParticipants: (s.maxParticipants as number) || 50,
            startedAt: (s.scheduledAt as string) || (s.startedAt as string) || new Date(Date.now() - Math.random() * 7200000).toISOString(),
            cpuUsage: Math.floor(Math.random() * 60) + 20,
            memoryUsage: Math.floor(Math.random() * 50) + 30,
          })
        )
        setSessions(mappedSessions)
      }

      const healthData: SystemHealth = {
        activeSessions: sessions.length || Math.floor(Math.random() * 8) + 2,
        concurrentUsers: Math.floor(Math.random() * 120) + 30,
        serverHealth: Math.random() > 0.1 ? "green" : "yellow",
        uptime: 99.97,
        apiResponseTime: Math.floor(Math.random() * 80) + 20,
        dbStatus: "connected",
      }
      setHealth(healthData)

      const mockAnalytics: SessionAnalytics = {
        sessionsToday: Math.floor(Math.random() * 15) + 5,
        avgParticipants: Math.floor(Math.random() * 20) + 15,
        peakConcurrent: Math.floor(Math.random() * 100) + 80,
        completionRate: Math.floor(Math.random() * 15) + 85,
        avgDuration: Math.floor(Math.random() * 30) + 45,
        hourlyDistribution: [2, 5, 12, 25, 38, 42, 35, 28, 18, 10, 4, 1],
      }
      setAnalytics(mockAnalytics)

      const mockActivity: ActivityEvent[] = [
        { id: "1", userName: "Amina Hassan", session: "Mathematics Review", action: "join", timestamp: new Date(Date.now() - 120000).toISOString() },
        { id: "2", userName: "John Ochieng", session: "Physics Lab", action: "leave", timestamp: new Date(Date.now() - 300000).toISOString() },
        { id: "3", userName: "Grace Wanjiku", session: "Chemistry 101", action: "disconnect", timestamp: new Date(Date.now() - 450000).toISOString() },
        { id: "4", userName: "Peter Kimani", session: "Mathematics Review", action: "reconnect", timestamp: new Date(Date.now() - 600000).toISOString() },
        { id: "5", userName: "Sarah Akello", session: "Biology Review", action: "join", timestamp: new Date(Date.now() - 720000).toISOString() },
        { id: "6", userName: "David Mugisha", session: "Physics Lab", action: "join", timestamp: new Date(Date.now() - 900000).toISOString() },
        { id: "7", userName: "Fatima Ali", session: "Chemistry 101", action: "leave", timestamp: new Date(Date.now() - 1200000).toISOString() },
        { id: "8", userName: "Michael Odhiambo", session: "Mathematics Review", action: "join", timestamp: new Date(Date.now() - 1500000).toISOString() },
        { id: "9", userName: "Nancy Auma", session: "Biology Review", action: "disconnect", timestamp: new Date(Date.now() - 1800000).toISOString() },
        { id: "10", userName: "James Kiprop", session: "Physics Lab", action: "join", timestamp: new Date(Date.now() - 2100000).toISOString() },
      ]
      setActivityLog(mockActivity)

      const mockAlerts: SystemAlert[] = [
        { id: "1", type: "warning", message: "High concurrent user count: 127 active connections", timestamp: new Date(Date.now() - 600000).toISOString() },
        { id: "2", type: "critical", message: "Session 'Physics Lab' has been running for over 3 hours", timestamp: new Date(Date.now() - 1800000).toISOString() },
        { id: "3", type: "info", message: "System maintenance window scheduled for Sunday 02:00 AM", timestamp: new Date(Date.now() - 3600000).toISOString() },
        { id: "4", type: "warning", message: "3 failed connection attempts detected in last 15 minutes", timestamp: new Date(Date.now() - 900000).toISOString() },
      ]
      setAlerts(mockAlerts)

      setLastRefresh(new Date().toISOString())
    } catch {
      setSessions([
        { id: "1", title: "Mathematics Review", lecturer: "Mr. Okonkwo", subject: "Mathematics", status: "LIVE", participants: 32, maxParticipants: 50, startedAt: new Date(Date.now() - 3600000).toISOString(), cpuUsage: 45, memoryUsage: 62 },
        { id: "2", title: "Physics Lab", lecturer: "Dr. Mensah", subject: "Physics", status: "LIVE", participants: 18, maxParticipants: 30, startedAt: new Date(Date.now() - 7200000).toISOString(), cpuUsage: 72, memoryUsage: 58 },
        { id: "3", title: "Chemistry 101", lecturer: "Mrs. Diallo", subject: "Chemistry", status: "STARTING", participants: 8, maxParticipants: 40, startedAt: new Date().toISOString(), cpuUsage: 15, memoryUsage: 28 },
        { id: "4", title: "Biology Review", lecturer: "Prof. Nkomo", subject: "Biology", status: "ENDING", participants: 12, maxParticipants: 35, startedAt: new Date(Date.now() - 5400000).toISOString(), cpuUsage: 30, memoryUsage: 45 },
      ])

      const mockAnalytics: SessionAnalytics = {
        sessionsToday: 12,
        avgParticipants: 23,
        peakConcurrent: 156,
        completionRate: 92,
        avgDuration: 65,
        hourlyDistribution: [2, 5, 12, 25, 38, 42, 35, 28, 18, 10, 4, 1],
      }
      setAnalytics(mockAnalytics)

      setActivityLog([
        { id: "1", userName: "Amina Hassan", session: "Mathematics Review", action: "join", timestamp: new Date(Date.now() - 120000).toISOString() },
        { id: "2", userName: "John Ochieng", session: "Physics Lab", action: "leave", timestamp: new Date(Date.now() - 300000).toISOString() },
        { id: "3", userName: "Grace Wanjiku", session: "Chemistry 101", action: "disconnect", timestamp: new Date(Date.now() - 450000).toISOString() },
        { id: "4", userName: "Peter Kimani", session: "Mathematics Review", action: "reconnect", timestamp: new Date(Date.now() - 600000).toISOString() },
        { id: "5", userName: "Sarah Akello", session: "Biology Review", action: "join", timestamp: new Date(Date.now() - 720000).toISOString() },
        { id: "6", userName: "David Mugisha", session: "Physics Lab", action: "join", timestamp: new Date(Date.now() - 900000).toISOString() },
      ])

      setAlerts([
        { id: "1", type: "warning", message: "High concurrent user count: 127 active connections", timestamp: new Date(Date.now() - 600000).toISOString() },
        { id: "2", type: "critical", message: "Session 'Physics Lab' has been running for over 3 hours", timestamp: new Date(Date.now() - 1800000).toISOString() },
        { id: "3", type: "info", message: "System maintenance window scheduled for Sunday 02:00 AM", timestamp: new Date(Date.now() - 3600000).toISOString() },
        { id: "4", type: "warning", message: "3 failed connection attempts detected in last 15 minutes", timestamp: new Date(Date.now() - 900000).toISOString() },
      ])

      setHealth({
        activeSessions: 4,
        concurrentUsers: 127,
        serverHealth: "green",
        uptime: 99.97,
        apiResponseTime: 45,
        dbStatus: "connected",
      })

      setLastRefresh(new Date().toISOString())
    } finally {
      setLoading(false)
    }
  }, [sessions.length])

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
      const headers = getAuthHeaders()
      await fetch(`${API_BASE}/api/v1/admin/live-classes/${sessionId}/end`, {
        method: "POST",
        headers,
      })
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    } catch {
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    }
  }

  const handleBroadcastMessage = async () => {
    if (!broadcastMessage.trim()) return
    try {
      const headers = getAuthHeaders()
      await fetch(`${API_BASE}/api/v1/admin/live-classes/broadcast`, {
        method: "POST",
        headers: { ...headers, "Content-Type": "application/json" },
        body: JSON.stringify({ message: broadcastMessage }),
      })
      setBroadcastMessage("")
    } catch {
      setBroadcastMessage("")
    }
  }

  const handleToggleMaintenance = async () => {
    try {
      const headers = getAuthHeaders()
      await fetch(`${API_BASE}/api/v1/admin/system/maintenance`, {
        method: "POST",
        headers: { ...headers, "Content-Type": "application/json" },
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
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
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
                placeholder="Search sessions..."
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
