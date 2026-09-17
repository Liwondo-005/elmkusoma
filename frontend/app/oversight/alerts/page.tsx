"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, AlertTriangle, Clock, Target, BookOpen, ClipboardList, Video, FileText, Award, Bell, Shield, CheckCircle, X, MoreHorizontal } from "lucide-react"

interface Alert {
  id: string
  type: string
  title: string
  message: string
  severity: "HIGH" | "MEDIUM" | "LOW"
  institutionName: string
  institutionId: string
  indicator: string
  value: string
  threshold: string
  timestamp: string
  status: "NEW" | "ACKNOWLEDGED" | "RESOLVED"
  acknowledgedBy?: string
  acknowledgedAt?: string
  resolvedBy?: string
  resolvedAt?: string
}

interface AlertsResponse {
  alerts: Alert[]
  summary: {
    total: number
    high: number
    medium: number
    low: number
    new: number
    acknowledged: number
    resolved: number
  }
}

export default function OversightAlertsPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [data, setData] = useState<AlertsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<"all" | "HIGH" | "MEDIUM" | "LOW" | "NEW" | "ACKNOWLEDGED" | "RESOLVED">("all")

  useEffect(() => {
    if (!authLoading && user) {
      fetchAlerts()
    }
  }, [user, authLoading])

  async function fetchAlerts() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/alerts`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        setData(await res.json())
      }
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  const filteredAlerts = data?.alerts.filter(alert => {
    if (filter === "all") return true
    return alert.severity === filter || alert.status === filter
  }) || []

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading alerts...</div>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">Alerts & Attention</h1>
          <p className="mt-2 text-muted-foreground">Alert data unavailable.</p>
        </div>
      </div>
    )
  }

  const getSeverityConfig = (severity: string) => {
    switch (severity) {
      case "HIGH": return { color: "bg-red-100 text-red-700", icon: <AlertTriangle className="size-3" /> }
      case "MEDIUM": return { color: "bg-orange-100 text-orange-700", icon: <AlertTriangle className="size-3" /> }
      case "LOW": return { color: "bg-yellow-100 text-yellow-700", icon: <AlertTriangle className="size-3" /> }
      default: return { color: "bg-gray-100 text-gray-700", icon: <AlertTriangle className="size-3" /> }
    }
  }

  const getStatusConfig = (status: string) => {
    switch (status) {
      case "NEW": return { color: "bg-blue-100 text-blue-700" }
      case "ACKNOWLEDGED": return { color: "bg-orange-100 text-orange-700" }
      case "RESOLVED": return { color: "bg-green-100 text-green-700" }
      default: return { color: "bg-gray-100 text-gray-700" }
    }
  }

  const typeIcons: Record<string, React.ReactNode> = {
    LOW_ATTENDANCE: <GraduationCap className="size-4" />,
    LOW_PERFORMANCE: <Award className="size-4" />,
    LOW_CURRICULUM_PROGRESS: <BookOpen className="size-4" />,
    MISSED_ASSESSMENTS: <FileText className="size-4" />,
    LOW_LIVE_CLASS_ACTIVITY: <Video className="size-4" />,
    TEACHER_SHORTAGE: <Users className="size-4" />,
    HIGH_DROPOUT_RISK: <AlertTriangle className="size-4" />,
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Alerts & Attention</h1>
          <p className="text-sm text-muted-foreground">Monitor issues requiring review across your jurisdiction</p>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as typeof filter)}
            className="rounded-lg border border-border px-3 py-1.5 text-sm bg-background"
          >
            <option value="all">All Alerts</option>
            <option value="HIGH">High Severity</option>
            <option value="MEDIUM">Medium Severity</option>
            <option value="LOW">Low Severity</option>
            <option value="NEW">New</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="RESOLVED">Resolved</option>
          </select>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<AlertTriangle className="size-5" />} label="Total Alerts" value={data.summary.total} color="bg-gray-500/10 text-gray-600" />
        <StatCard icon={<AlertTriangle className="size-5" />} label="High Severity" value={data.summary.high} color="bg-red-500/10 text-red-600" />
        <StatCard icon={<AlertTriangle className="size-5" />} label="New" value={data.summary.new} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<CheckCircle className="size-5" />} label="Resolved" value={data.summary.resolved} color="bg-green-500/10 text-green-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground w-10"></th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Type</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Message</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Indicator</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Value / Threshold</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground w-24">Severity</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground w-24">Status</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Time</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground w-10">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredAlerts.map((alert) => {
                const severityConfig = getSeverityConfig(alert.severity)
                const statusConfig = getStatusConfig(alert.status)
                const typeIcon = typeIcons[alert.type] || <AlertTriangle className="size-4" />

                return (
                  <tr key={alert.id} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4">
                      <span className="flex size-8 items-center justify-center rounded-lg bg-gray-100">
                        {typeIcon}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <p className="font-medium text-foreground">{alert.title}</p>
                      <p className="text-xs text-muted-foreground">{alert.message}</p>
                    </td>
                    <td className="py-3 px-4 text-muted-foreground">{alert.institutionName}</td>
                    <td className="py-3 px-4 text-muted-foreground">{alert.indicator}</td>
                    <td className="py-3 px-4 text-muted-foreground">
                      <span className="font-mono">{alert.value} / {alert.threshold}</span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${severityConfig.color}`}>
                        {severityConfig.icon}
                        {alert.severity}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${statusConfig.color}`}>
                        {alert.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-muted-foreground">
                      {new Date(alert.timestamp).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {alert.status === "NEW" && (
                        <button className="text-primary hover:underline text-xs">
                          Acknowledge
                        </button>
                      )}
                      {alert.status === "ACKNOWLEDGED" && (
                        <button className="text-green-600 hover:underline text-xs">
                          Resolve
                        </button>
                      )}
                    </td>
                  </tr>
                )
              }
            )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: string | number; color: string }) {
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

function getSeverityConfig(severity: string) {
  switch (severity) {
    case "HIGH": return { color: "bg-red-100 text-red-700", icon: <AlertTriangle className="size-3" /> }
    case "MEDIUM": return { color: "bg-orange-100 text-orange-700", icon: <AlertTriangle className="size-3" /> }
    case "LOW": return { color: "bg-yellow-100 text-yellow-700", icon: <AlertTriangle className="size-3" /> }
    default: return { color: "bg-gray-100 text-gray-700", icon: <AlertTriangle className="size-3" /> }
  }
}

function getStatusConfig(status: string) {
  switch (status) {
    case "NEW": return { color: "bg-blue-100 text-blue-700" }
    case "ACKNOWLEDGED": return { color: "bg-orange-100 text-orange-700" }
    case "RESOLVED": return { color: "bg-green-100 text-green-700" }
    default: return { color: "bg-gray-100 text-gray-700" }
  }
}