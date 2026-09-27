"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  adminApi,
  auditApi,
  getInstitutionId,
  type ActiveLiveSession,
  type LiveSessionStats,
  type LiveSessionHealth,
  type SessionParticipant,
  type ActivityFeedResponse,
} from "@/lib/api"
import {
  Radio,
  Users,
  Clock,
  Activity,
  BarChart3,
  Search,
  RefreshCw,
  Calendar,
  CheckCircle,
  AlertTriangle,
  XCircle,
} from "lucide-react"

function timeAgo(timestamp: string): string {
  const diff = Date.now() - new Date(timestamp).getTime()
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

export default function AdminLiveOperationsPage() {
  const { user } = useAuth()
  const router = useRouter()
  const tc = useTranslations("common")

  const [sessions, setSessions] = useState<ActiveLiveSession[]>([])
  const [teacherNames, setTeacherNames] = useState<Record<string, string>>({})
  const [stats, setStats] = useState<LiveSessionStats | null>(null)
  const [health, setHealth] = useState<LiveSessionHealth | null>(null)
  const [activity, setActivity] = useState<ActivityFeedResponse[]>([])
  const [activityError, setActivityError] = useState<string | null>(null)

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const [lastRefresh, setLastRefresh] = useState<string>(new Date().toISOString())
  const [searchQuery, setSearchQuery] = useState("")

  // Participants drill-down (real endpoint: /v1/admin/live-sessions/participants/{id})
  const [participantsFor, setParticipantsFor] = useState<ActiveLiveSession | null>(null)
  const [participants, setParticipants] = useState<SessionParticipant[] | null>(null)
  const [participantsLoading, setParticipantsLoading] = useState(false)
  const [participantsError, setParticipantsError] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    const institutionId = getInstitutionId()
    if (!institutionId) {
      setLoadError("No institution context found. Please log in again.")
      setLoading(false)
      return
    }

    // All four calls are real backend endpoints; Promise.allSettled keeps the
    // page usable when one source fails (e.g. audit activity for an account
    // without an institution membership).
    const [activeRes, statsRes, healthRes, activityRes, peopleRes] = await Promise.allSettled([
      adminApi.getActiveLiveSessions(),
      adminApi.getLiveSessionStats(),
      adminApi.getLiveSessionHealth(),
      auditApi.listActivity(institutionId, 0, 10),
      adminApi.listPeople(institutionId, 0, 200),
    ])

    if (activeRes.status === "fulfilled") {
      setSessions(activeRes.value)
      setLoadError(null)
    } else {
      setLoadError("Could not load live sessions. Retrying may help — check your connection.")
    }

    if (statsRes.status === "fulfilled") setStats(statsRes.value)
    if (healthRes.status === "fulfilled") setHealth(healthRes.value)

    if (activityRes.status === "fulfilled") {
      setActivity(activityRes.value)
      setActivityError(null)
    } else {
      // Backend resolves institution from active memberships; accounts without
      // one get rejected. Report honestly instead of showing fabricated events.
      setActivity([])
      setActivityError("Activity feed is unavailable for this account (no institution membership).")
    }

    if (peopleRes.status === "fulfilled") {
      const names: Record<string, string> = {}
      for (const p of peopleRes.value) {
        if (p.userId) names[p.userId] = p.fullName || `${p.firstName} ${p.lastName}`.trim()
      }
      setTeacherNames(names)
    }

    setLastRefresh(new Date().toISOString())
    setLoading(false)
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
    // Frontend roles are mapped (backend ADMIN → "Admin"); mirrors proxy.ts
    // isAdminRoute and backend hasAnyRole('INSTITUTION_ADMIN','ADMIN').
    if (user && user.role !== "Admin" && user.role !== "Institution Admin") {
      router.push("/dashboard")
    }
  }, [user, router])

  const openParticipants = async (session: ActiveLiveSession) => {
    setParticipantsFor(session)
    setParticipants(null)
    setParticipantsError(null)
    setParticipantsLoading(true)
    try {
      const list = await adminApi.getSessionParticipants(session.id)
      setParticipants(list)
    } catch {
      setParticipantsError("Could not load participants for this session.")
    } finally {
      setParticipantsLoading(false)
    }
  }

  const filteredSessions = sessions.filter((session) => {
    const q = searchQuery.toLowerCase()
    if (!q) return true
    const lecturer = teacherNames[session.teacherId ?? ""] || ""
    return session.title.toLowerCase().includes(q) || lecturer.toLowerCase().includes(q)
  })

  const participantsOnline = sessions.reduce((sum, s) => sum + (s.currentParticipants || 0), 0)
  const completionRate =
    stats && stats.totalSessions > 0
      ? Math.round((stats.completed / stats.totalSessions) * 100)
      : null

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Admin"} subtitle="Live session monitoring and management" />
        <LoadingState />
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Admin"} subtitle="Live session monitoring and management" />
        <section className="rounded-2xl border border-border bg-card p-8 text-center shadow-xs">
          <AlertTriangle className="mx-auto size-8 text-destructive" />
          <p className="mt-3 text-sm font-medium text-destructive">{loadError}</p>
          <button
            onClick={() => {
              setLoading(true)
              fetchData()
            }}
            className="mt-4 inline-flex h-9 items-center gap-2 rounded-lg border border-border px-4 text-sm text-foreground hover:bg-muted"
          >
            <RefreshCw className="size-4" /> Retry
          </button>
        </section>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <LearnerHeader firstName={user?.firstName || "Admin"} subtitle="Live session monitoring and management" />

      {/* System Health — real values from /v1/live-session/health */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">Live Service Health</h2>
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
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          {/* Service status */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-emerald-500/10">
              <CheckCircle className="size-4 text-emerald-500" />
            </div>
            <div className="mt-2 flex items-center gap-1.5">
              <span
                className={`size-2 rounded-full ${
                  health?.status === "OPERATIONAL" ? "bg-emerald-500" : "bg-amber-500"
                }`}
              />
              <p className="text-sm font-bold text-foreground">{health?.status ?? "—"}</p>
            </div>
            <p className="text-[10px] text-muted-foreground">{health?.service ?? "Live service"}</p>
          </div>

          {/* Mode */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-blue-500/10">
              <Radio className="size-4 text-blue-500" />
            </div>
            <p className="mt-2 text-sm font-bold text-foreground capitalize">{health?.mode ?? "—"}</p>
            <p className="text-[10px] text-muted-foreground">
              Mode{health ? (health.liveKitConfigured ? " (LiveKit configured)" : " (video not configured)") : ""}
            </p>
          </div>

          {/* In-progress sessions */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-red-500/10">
              <Radio className="size-4 text-red-500" />
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">{stats?.inProgress ?? sessions.length}</p>
            <p className="text-[10px] text-muted-foreground">Sessions In Progress</p>
          </div>

          {/* Participants currently connected */}
          <div className="rounded-xl border border-border bg-background p-4">
            <div className="flex size-8 items-center justify-center rounded-lg bg-violet-500/10">
              <Users className="size-4 text-violet-500" />
            </div>
            <p className="mt-2 text-2xl font-bold text-foreground">{participantsOnline}</p>
            <p className="text-[10px] text-muted-foreground">Participants Connected</p>
          </div>

          {/* Service message */}
          <div className="col-span-2 rounded-xl border border-border bg-background p-4 sm:col-span-1">
            <div className="flex size-8 items-center justify-center rounded-lg bg-amber-500/10">
              <AlertTriangle className="size-4 text-amber-500" />
            </div>
            <p className="mt-2 text-xs font-medium text-foreground">{health?.message ?? "Health check unavailable"}</p>
            <p className="mt-1 text-[10px] text-muted-foreground">Latest status message</p>
          </div>
        </div>
      </section>

      {/* Live Sessions Monitor — real /v1/admin/live-sessions/active data */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2">
            <Radio className="size-4 text-red-500" />
            <h2 className="text-sm font-semibold text-foreground">Live Sessions Monitor</h2>
            <span className="rounded-full bg-red-500/10 px-2 py-0.5 text-[10px] font-bold text-red-500">
              {sessions.length} LIVE
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
            description="There are currently no live sessions in progress for this institution."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border text-[10px] uppercase tracking-wider text-muted-foreground">
                  <th className="pb-2 pr-4 font-medium">Session</th>
                  <th className="pb-2 pr-4 font-medium">Lecturer</th>
                  <th className="pb-2 pr-4 font-medium">Status</th>
                  <th className="pb-2 pr-4 font-medium">Participants</th>
                  <th className="pb-2 pr-4 font-medium">Scheduled</th>
                  <th className="pb-2 pr-4 font-medium">Duration</th>
                  <th className="pb-2 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filteredSessions.map((session) => (
                  <tr key={session.id} className="hover:bg-muted/30">
                    <td className="py-3 pr-4">
                      <p className="font-medium text-foreground">{session.title}</p>
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {session.teacherId
                        ? teacherNames[session.teacherId] || `${session.teacherId.slice(0, 8)}…`
                        : "—"}
                    </td>
                    <td className="py-3 pr-4">
                      <span className="inline-flex items-center gap-1 rounded-full border border-red-500/20 bg-red-500/10 px-2 py-0.5 text-[10px] font-bold text-red-500">
                        <span className="size-1 rounded-full bg-current animate-pulse" />
                        LIVE
                      </span>
                    </td>
                    <td className="py-3 pr-4">
                      <div className="flex items-center gap-1.5">
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full rounded-full bg-primary transition-all"
                            style={{
                              width: `${
                                session.maxParticipants
                                  ? Math.min(100, (session.currentParticipants / session.maxParticipants) * 100)
                                  : 0
                              }%`,
                            }}
                          />
                        </div>
                        <span className="text-[10px] text-muted-foreground">
                          {session.currentParticipants}
                          {session.maxParticipants ? `/${session.maxParticipants}` : ""}
                        </span>
                      </div>
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {session.scheduledAt
                        ? new Date(session.scheduledAt).toLocaleString("en-GB", {
                            day: "2-digit",
                            month: "short",
                            hour: "2-digit",
                            minute: "2-digit",
                          })
                        : "—"}
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {session.durationMinutes ? `${session.durationMinutes} min` : "—"}
                    </td>
                    <td className="py-3">
                      <button
                        onClick={() => openParticipants(session)}
                        className="flex size-6 items-center justify-center rounded border border-border text-muted-foreground hover:bg-muted"
                        title="View participants"
                      >
                        <Users className="size-3" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Session Statistics — real /v1/admin/live-sessions/stats */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-4 flex items-center gap-2">
            <BarChart3 className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">Session Statistics</h2>
          </div>
          {stats ? (
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-xl border border-border bg-background p-3">
                <p className="text-[10px] text-muted-foreground">Total Sessions</p>
                <p className="mt-1 text-xl font-bold text-foreground">{stats.totalSessions}</p>
              </div>
              <div className="rounded-xl border border-border bg-background p-3">
                <p className="text-[10px] text-muted-foreground">Scheduled</p>
                <p className="mt-1 text-xl font-bold text-foreground">{stats.scheduled}</p>
              </div>
              <div className="rounded-xl border border-border bg-background p-3">
                <p className="text-[10px] text-muted-foreground">In Progress</p>
                <p className="mt-1 text-xl font-bold text-foreground">{stats.inProgress}</p>
              </div>
              <div className="rounded-xl border border-border bg-background p-3">
                <p className="text-[10px] text-muted-foreground">Completed</p>
                <p className="mt-1 text-xl font-bold text-foreground">{stats.completed}</p>
              </div>
              <div className="col-span-2 rounded-xl border border-border bg-background p-3">
                <p className="text-[10px] text-muted-foreground">Completion Rate</p>
                <p className="mt-1 text-xl font-bold text-foreground">
                  {completionRate != null ? `${completionRate}%` : "—"}
                </p>
                <p className="mt-1 text-[9px] text-muted-foreground">
                  {stats.totalSessions > 0
                    ? `${stats.completed} of ${stats.totalSessions} sessions completed`
                    : "No sessions recorded yet"}
                </p>
              </div>
            </div>
          ) : (
            <p className="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground">
              Statistics unavailable.
            </p>
          )}
        </section>

        {/* Recent Activity — real /v1/audit/activity audit feed */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-4 flex items-center gap-2">
            <Clock className="size-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold text-foreground">Recent Activity</h2>
          </div>
          {activityError ? (
            <p className="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground">
              {activityError}
            </p>
          ) : activity.length === 0 ? (
            <p className="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground">
              No activity recorded yet.
            </p>
          ) : (
            <div className="max-h-80 space-y-2 overflow-y-auto pr-1">
              {activity.map((event) => (
                <div key={event.id} className="flex items-start gap-3 rounded-lg border border-border p-2.5">
                  <span className="mt-0.5 inline-flex shrink-0 items-center rounded-full bg-primary/10 px-1.5 py-0.5 text-[9px] font-bold uppercase text-primary">
                    {event.action.split("_")[0]}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-foreground">{event.actorName || "System"}</p>
                    <p className="truncate text-[10px] text-muted-foreground">
                      {event.description || event.entityName || event.entityType}
                    </p>
                  </div>
                  <span className="shrink-0 text-[10px] text-muted-foreground">{timeAgo(event.createdAt)}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      {/* Participants drill-down */}
      {participantsFor && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users className="size-4 text-muted-foreground" />
              <h2 className="text-sm font-semibold text-foreground">
                Participants — {participantsFor.title}
              </h2>
            </div>
            <button
              onClick={() => setParticipantsFor(null)}
              className="flex size-7 items-center justify-center rounded-lg border border-border hover:bg-muted"
              title="Close"
            >
              <XCircle className="size-3.5 text-muted-foreground" />
            </button>
          </div>
          {participantsLoading ? (
            <LoadingState />
          ) : participantsError ? (
            <p className="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground">
              {participantsError}
            </p>
          ) : !participants || participants.length === 0 ? (
            <p className="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground">
              No participants have joined this session yet.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-border text-[10px] uppercase tracking-wider text-muted-foreground">
                    <th className="pb-2 pr-4 font-medium">Name</th>
                    <th className="pb-2 pr-4 font-medium">Role</th>
                    <th className="pb-2 pr-4 font-medium">Status</th>
                    <th className="pb-2 pr-4 font-medium">Joined</th>
                    <th className="pb-2 font-medium">Duration</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {participants.map((p) => (
                    <tr key={p.userId} className="hover:bg-muted/30">
                      <td className="py-2.5 pr-4 font-medium text-foreground">{p.userName}</td>
                      <td className="py-2.5 pr-4 text-muted-foreground">{p.role}</td>
                      <td className="py-2.5 pr-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold ${
                            p.online ? "bg-emerald-500/10 text-emerald-600" : "bg-muted text-muted-foreground"
                          }`}
                        >
                          {p.online ? "Online" : "Left"}
                        </span>
                      </td>
                      <td className="py-2.5 pr-4 text-muted-foreground">
                        {p.joinedAt ? new Date(p.joinedAt).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" }) : "—"}
                      </td>
                      <td className="py-2.5 text-muted-foreground">
                        {p.durationSeconds != null ? `${Math.floor(p.durationSeconds / 60)}m` : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {/* Footer Info — real service status, not a hardcoded claim */}
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
          <span
            className={`size-1.5 rounded-full animate-pulse ${
              health?.status === "OPERATIONAL" ? "bg-emerald-500" : "bg-amber-500"
            }`}
          />
          {health ? `Live service: ${health.status}` : "Live service status unknown"}
        </div>
      </div>
    </div>
  )
}
