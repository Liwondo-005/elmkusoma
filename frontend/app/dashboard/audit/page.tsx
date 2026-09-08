"use client"

import { useEffect, useState } from "react"
import { Shield, Activity, AlertTriangle, Loader2, CheckCircle } from "lucide-react"
import {
  auditApi,
  getInstitutionId,
  type AuditLogResponse,
  type ActivityFeedResponse,
  type SecurityEventResponse,
} from "@/lib/api"

type Tab = "logs" | "activity" | "security"

const tabs: { key: Tab; label: string; icon: typeof Shield }[] = [
  { key: "logs", label: "Audit Logs", icon: Shield },
  { key: "activity", label: "Activity Feed", icon: Activity },
  { key: "security", label: "Security Events", icon: AlertTriangle },
]

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

export default function AuditPage() {
  const [activeTab, setActiveTab] = useState<Tab>("logs")
  const institutionId = getInstitutionId()

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Audit</h1>
        <p className="mt-1 text-sm text-muted-foreground">System audit logs, activity feed, and security events.</p>
      </div>

      <div className="flex gap-1 rounded-xl border border-border bg-muted p-1">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
              activeTab === tab.key
                ? "bg-card text-foreground shadow-xs"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <tab.icon className="size-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {!institutionId ? (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">No institution context found. Please log in again.</p>
        </div>
      ) : activeTab === "logs" ? (
        <AuditLogs institutionId={institutionId} />
      ) : activeTab === "activity" ? (
        <ActivityFeed institutionId={institutionId} />
      ) : (
        <SecurityEvents institutionId={institutionId} />
      )}
    </div>
  )
}

function AuditLogs({ institutionId }: { institutionId: string }) {
  const [logs, setLogs] = useState<AuditLogResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    auditApi
      .listLogs(institutionId)
      .then(setLogs)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load audit logs"))
      .finally(() => setLoading(false))
  }, [institutionId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  if (error) return <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center"><p className="text-sm font-medium text-destructive">{error}</p></div>
  if (logs.length === 0) return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
      <Shield className="size-10 text-muted-foreground/50" />
      <p className="mt-4 text-sm font-medium text-foreground">No audit logs</p>
      <p className="mt-1 text-sm text-muted-foreground">Activity will appear here as it occurs.</p>
    </div>
  )

  return (
    <div className="space-y-3">
      {logs.map((log) => (
        <div key={log.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="rounded bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">{log.action}</span>
                <span className="text-xs font-medium text-foreground">{log.entityType}</span>
                {log.entityName && <span className="text-xs text-muted-foreground">— {log.entityName}</span>}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                {log.userEmail || log.userId} {log.userRole && `(${log.userRole})`}
              </p>
              {log.requestMethod && log.requestUrl && (
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {log.requestMethod} {log.requestUrl}
                  {log.responseStatus && ` → ${log.responseStatus}`}
                  {log.durationMs && ` (${log.durationMs}ms)`}
                </p>
              )}
            </div>
            <span className="shrink-0 text-xs text-muted-foreground">{formatDate(log.createdAt)}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

function ActivityFeed({ institutionId }: { institutionId: string }) {
  const [activities, setActivities] = useState<ActivityFeedResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    auditApi
      .listActivity(institutionId)
      .then(setActivities)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load activity"))
      .finally(() => setLoading(false))
  }, [institutionId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  if (error) return <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center"><p className="text-sm font-medium text-destructive">{error}</p></div>
  if (activities.length === 0) return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
      <Activity className="size-10 text-muted-foreground/50" />
      <p className="mt-4 text-sm font-medium text-foreground">No activity yet</p>
      <p className="mt-1 text-sm text-muted-foreground">Recent activity will appear here.</p>
    </div>
  )

  return (
    <div className="space-y-3">
      {activities.map((a) => (
        <div key={a.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-foreground">{a.actorName}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">{a.description}</p>
              <div className="mt-1 flex items-center gap-2">
                <span className="rounded bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">{a.action}</span>
                <span className="text-[10px] text-muted-foreground">{a.entityType}</span>
                {a.entityName && <span className="text-[10px] text-muted-foreground">— {a.entityName}</span>}
              </div>
            </div>
            <span className="shrink-0 text-xs text-muted-foreground">{formatDate(a.createdAt)}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

function SecurityEvents({ institutionId }: { institutionId: string }) {
  const [events, setEvents] = useState<SecurityEventResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [resolving, setResolving] = useState<string | null>(null)

  useEffect(() => {
    auditApi
      .listSecurityEvents(institutionId)
      .then(setEvents)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load security events"))
      .finally(() => setLoading(false))
  }, [institutionId])

  async function handleResolve(eventId: string) {
    setResolving(eventId)
    try {
      const updated = await auditApi.resolveSecurityEvent(eventId)
      setEvents((prev) => prev.map((e) => (e.id === eventId ? updated : e)))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to resolve event")
    } finally {
      setResolving(null)
    }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  if (error) return <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center"><p className="text-sm font-medium text-destructive">{error}</p></div>
  if (events.length === 0) return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
      <Shield className="size-10 text-muted-foreground/50" />
      <p className="mt-4 text-sm font-medium text-foreground">No security events</p>
      <p className="mt-1 text-sm text-muted-foreground">Security events will appear here.</p>
    </div>
  )

  return (
    <div className="space-y-3">
      {events.map((e) => (
        <div key={e.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span
                  className={`rounded px-2 py-0.5 text-[10px] font-semibold ${
                    e.severity === "CRITICAL"
                      ? "bg-destructive/10 text-destructive"
                      : e.severity === "HIGH"
                        ? "bg-orange/10 text-orange"
                        : e.severity === "MEDIUM"
                          ? "bg-amber-500/10 text-amber-600"
                          : "bg-muted text-muted-foreground"
                  }`}
                >
                  {e.severity}
                </span>
                <span className="text-xs font-medium text-foreground">{e.eventType}</span>
                {e.resolved && (
                  <span className="flex items-center gap-1 rounded bg-teal/10 px-2 py-0.5 text-[10px] font-medium text-teal">
                    <CheckCircle className="size-3" /> Resolved
                  </span>
                )}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{e.description}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {e.userEmail || e.userId}
                {e.ipAddress && ` from ${e.ipAddress}`}
              </p>
            </div>
            <div className="flex flex-col items-end gap-2">
              <span className="text-xs text-muted-foreground">{formatDate(e.createdAt)}</span>
              {!e.resolved && (
                <button
                  onClick={() => handleResolve(e.id)}
                  disabled={resolving === e.id}
                  className="flex h-7 items-center gap-1.5 rounded-lg border border-border px-2.5 text-[10px] font-medium text-foreground hover:bg-muted disabled:opacity-50"
                >
                  <CheckCircle className="size-3" /> {resolving === e.id ? "Resolving..." : "Resolve"}
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
