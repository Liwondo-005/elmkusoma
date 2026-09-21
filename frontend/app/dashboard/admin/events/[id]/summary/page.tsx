"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import { useTranslations } from "next-intl"
import {
  ArrowLeft, Loader2, Calendar, Users, Clock, Download,
  Video, FileText, BarChart3, CheckCircle, XCircle, AlertCircle, Play
} from "lucide-react"
import Link from "next/link"
import { adminApi } from "@/lib/api"
import { cn } from "@/lib/utils"

interface EventSummary {
  id: string
  title: string
  eventType: string
  status: string
  startDate: string
  durationMinutes?: number
  timezone?: string
  presenterName?: string
  recordingEnabled?: boolean
  recordingUrl?: string
  maxCapacity?: number
  currentRegistrations?: number
  joinedCount?: number
  attendanceRate?: number
  participation?: number
  materials?: Array<{ id: string; name: string; url: string; type: string }>
  attendance?: Array<{
    userId: string
    userName: string
    email: string
    joinTime: string
    leaveTime?: string
    durationMinutes?: number
  }>
  analytics?: Record<string, unknown>
}

interface SummaryStats {
  registered: number
  joined: number
  attendanceRate: number
  participation: number
}

export default function EventSummaryPage() {
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const params = useParams()
  const eventId = params.id as string

  const [event, setEvent] = useState<EventSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadSummary()
  }, [eventId])

  async function loadSummary() {
    try {
      setLoading(true)
      const data = await adminApi.getEventSummary(eventId)
      setEvent(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load event summary")
    } finally {
      setLoading(false)
    }
  }

  const stats: SummaryStats = {
    registered: event?.currentRegistrations ?? 0,
    joined: event?.joinedCount ?? 0,
    attendanceRate: event?.attendanceRate ?? 0,
    participation: event?.participation ?? 0,
  }

  const statCards = [
    { key: "registered", label: t("admin.summary.statsGrid.registered"), value: stats.registered, icon: Users, color: "text-blue-600", bg: "bg-blue-500/10" },
    { key: "joined", label: t("admin.summary.statsGrid.joined"), value: stats.joined, icon: CheckCircle, color: "text-emerald-600", bg: "bg-emerald-500/10" },
    { key: "attendanceRate", label: t("admin.summary.statsGrid.attendanceRate"), value: `${stats.attendanceRate}%`, icon: BarChart3, color: "text-purple-600", bg: "bg-purple-500/10" },
    { key: "participation", label: t("admin.summary.statsGrid.participation"), value: `${stats.participation}%`, icon: Clock, color: "text-orange-600", bg: "bg-orange-500/10" },
  ]

  function handleExportAttendance() {
    if (!event?.attendance) return
    const headers = ["Name", "Email", "Join Time", "Leave Time", "Duration (min)"]
    const rows = event.attendance.map((a) => [
      a.userName, a.email,
      new Date(a.joinTime).toLocaleString(),
      a.leaveTime ? new Date(a.leaveTime).toLocaleString() : "",
      a.durationMinutes?.toString() || "",
    ])
    const csv = [headers, ...rows].map((r) => r.join(",")).join("\n")
    const blob = new Blob([csv], { type: "text/csv" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `attendance-${event.title.replace(/\s+/g, "-")}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error || !event) {
    return (
      <div className="mx-auto max-w-5xl space-y-6" role="main" aria-label={t("admin.summary.title")}>
        <Link href="/dashboard/admin/events" className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" />
          {t("admin.summary.backToEvents")}
        </Link>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 flex items-center gap-3">
          <AlertCircle className="size-5 text-destructive shrink-0" />
          <p className="text-sm font-medium text-destructive flex-1">{error || "Event not found"}</p>
          <button onClick={loadSummary} className="text-sm font-medium text-destructive hover:underline" aria-label={tc("retry")}>
            {tc("retry")}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6" role="main" aria-label={t("admin.summary.title")}>
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link
          href="/dashboard/admin/events"
          className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-foreground"
          aria-label={t("admin.summary.backToEvents")}
        >
          <ArrowLeft className="size-4" />
          {t("admin.summary.backToEvents")}
        </Link>
      </div>

      {/* Session Info */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-bold text-foreground">{event.title}</h2>
        <div className="mt-2 flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <Calendar className="size-4" />
            {new Date(event.startDate).toLocaleString()}
          </span>
          {event.durationMinutes && (
            <span className="inline-flex items-center gap-1.5">
              <Clock className="size-4" />
              {event.durationMinutes} min
            </span>
          )}
          {event.presenterName && (
            <span className="inline-flex items-center gap-1.5">
              <Users className="size-4" />
              {event.presenterName}
            </span>
          )}
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((card) => (
          <div key={card.key} className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="flex items-center gap-4">
              <div className={cn("flex size-12 items-center justify-center rounded-xl", card.bg)}>
                <card.icon className={cn("size-6", card.color)} />
              </div>
              <div>
                <p className="text-2xl font-bold text-foreground">{card.value}</p>
                <p className="text-xs text-muted-foreground">{card.label}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Recording & Replay */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
            <Video className="size-4 text-muted-foreground" />
            {t("admin.summary.recordingStatus")}
          </h3>
          {event.recordingUrl ? (
            <div className="space-y-3">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 px-3 py-1 text-xs font-medium">
                <CheckCircle className="size-3" />
                {t("admin.summary.recordingAvailable")}
              </span>
              <a
                href={event.recordingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90"
                aria-label={t("admin.actions.viewRecording")}
              >
                <Video className="size-4" />
                {t("admin.actions.viewRecording")}
              </a>
            </div>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-gray-50 text-gray-500 border border-gray-200 px-3 py-1 text-xs font-medium">
              <XCircle className="size-3" />
              {t("admin.summary.recordingNotAvailable")}
            </span>
          )}
        </div>
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
            <Play className="size-4 text-muted-foreground" />
            {t("admin.summary.replayAvailability")}
          </h3>
          {event.recordingUrl ? (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 px-3 py-1 text-xs font-medium">
              <CheckCircle className="size-3" />
              {t("admin.summary.replayAvailable")}
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-gray-50 text-gray-500 border border-gray-200 px-3 py-1 text-xs font-medium">
              <XCircle className="size-3" />
              {t("admin.summary.replayNotAvailable")}
            </span>
          )}
        </div>
      </div>

      {/* Materials */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h3 className="text-sm font-semibold text-foreground mb-4 flex items-center gap-2">
          <FileText className="size-4 text-muted-foreground" />
          {t("admin.summary.materialsUploaded")}
        </h3>
        {event.materials && event.materials.length > 0 ? (
          <div className="space-y-2">
            {event.materials.map((mat) => (
              <a
                key={mat.id}
                href={mat.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 rounded-lg border border-border px-4 py-3 hover:bg-muted/50 transition-colors"
              >
                <FileText className="size-4 text-muted-foreground shrink-0" />
                <span className="text-sm font-medium text-foreground">{mat.name}</span>
                <span className="ml-auto text-xs text-muted-foreground">{mat.type}</span>
              </a>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{t("admin.summary.noMaterials")}</p>
        )}
      </div>

      {/* Attendance Breakdown */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <Users className="size-4 text-muted-foreground" />
            {t("admin.summary.attendanceBreakdown")}
          </h3>
          {event.attendance && event.attendance.length > 0 && (
            <button
              onClick={handleExportAttendance}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
              aria-label={t("admin.summary.exportAttendance")}
            >
              <Download className="size-3.5" />
              {t("admin.summary.exportAttendance")}
            </button>
          )}
        </div>
        {event.attendance && event.attendance.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-4 py-2 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.summary.attendeeName")}</th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.summary.attendeeEmail")}</th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.summary.joinTime")}</th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.summary.leaveTime")}</th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-muted-foreground uppercase">{t("admin.summary.duration")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {event.attendance.map((a) => (
                  <tr key={a.userId} className="hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-2.5 text-sm font-medium text-foreground">{a.userName}</td>
                    <td className="px-4 py-2.5 text-sm text-muted-foreground">{a.email}</td>
                    <td className="px-4 py-2.5 text-sm text-muted-foreground">{new Date(a.joinTime).toLocaleString()}</td>
                    <td className="px-4 py-2.5 text-sm text-muted-foreground">{a.leaveTime ? new Date(a.leaveTime).toLocaleString() : "—"}</td>
                    <td className="px-4 py-2.5 text-sm text-muted-foreground">{a.durationMinutes ? `${a.durationMinutes} min` : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">No attendance data available</p>
        )}
      </div>

      {/* Analytics */}
      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h3 className="text-sm font-semibold text-foreground mb-4 flex items-center gap-2">
          <BarChart3 className="size-4 text-muted-foreground" />
          {t("admin.summary.analytics")}
        </h3>
        {event.analytics && Object.keys(event.analytics).length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Object.entries(event.analytics).map(([key, value]) => (
              <div key={key} className="rounded-xl border border-border p-4">
                <p className="text-xs text-muted-foreground uppercase">{key.replace(/([A-Z])/g, " $1").trim()}</p>
                <p className="mt-1 text-lg font-bold text-foreground">{String(value)}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{t("admin.summary.noAnalytics")}</p>
        )}
      </div>
    </div>
  )
}
