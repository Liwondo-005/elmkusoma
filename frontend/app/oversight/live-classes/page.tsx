"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, Video, Play, Clock, AlertTriangle, Eye, Monitor, X, CheckCircle2, HelpCircle } from "lucide-react"
import Link from "next/link"

interface LiveClassMetrics {
  liveNow: number
  scheduledToday: number
  completedToday: number
  totalThisWeek: number
  liveClasses: Array<{
    id: string
    title: string
    institutionName: string
    institutionId: string
    subjectName: string
    teacherName: string
    scheduledAt: string
    durationMinutes: number
    status: "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
    participantCount: number
    maxParticipants: number
  }>
}

export default function OversightLiveClassesPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [metrics, setMetrics] = useState<LiveClassMetrics | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchLiveClasses()
    }
  }, [user, authLoading])

  async function fetchLiveClasses() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/live-classes`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        setMetrics(await res.json())
      }
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading live classes...</div>
      </div>
    )
  }

  if (!metrics) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">Live Class Monitoring</h1>
          <p className="mt-2 text-muted-foreground">Live class data unavailable.</p>
        </div>
      </div>
    )
  }

  const getStatusConfig = (status: string) => {
    switch (status) {
      case "IN_PROGRESS": return { color: "bg-red-100 text-red-700", icon: <Play className="size-3" />, label: "LIVE NOW" }
      case "SCHEDULED": return { color: "bg-blue-100 text-blue-700", icon: <Clock className="size-3" />, label: "SCHEDULED" }
      case "COMPLETED": return { color: "bg-green-100 text-green-700", icon: <CheckCircle2 className="size-3" />, label: "COMPLETED" }
      case "CANCELLED": return { color: "bg-gray-100 text-gray-700", icon: <X className="size-3" />, label: "CANCELLED" }
      default: return { color: "bg-gray-100 text-gray-700", icon: <HelpCircle className="size-3" />, label: status }
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Video className="size-5 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">Live Class Monitoring</h1>
              <p className="text-sm text-muted-foreground">Monitor live classes across your jurisdiction</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="w-3 h-3 rounded-full bg-red-500 animate-pulse" />
              <span>Live Now: {metrics.liveNow}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Video className="size-4" />
              <span>This Week: {metrics.totalThisWeek}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Video className="size-5" />} label="Live Now" value={metrics.liveNow} color="bg-red-500/10 text-red-600" />
        <StatCard icon={<Clock className="size-5" />} label="Scheduled Today" value={metrics.scheduledToday} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<CheckCircle2 className="size-5" />} label="Completed Today" value={metrics.completedToday} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<Video className="size-5" />} label="This Week" value={metrics.totalThisWeek} color="bg-purple-500/10 text-purple-600" />
      </div>

      {metrics.liveClasses.filter(c => c.status === "IN_PROGRESS").length > 0 && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6">
          <h2 className="text-lg font-semibold text-red-700 mb-4 flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-red-500 animate-pulse" />
            LIVE NOW
          </h2>
          <div className="space-y-3">
            {metrics.liveClasses.filter(c => c.status === "IN_PROGRESS").map((cls) => (
              <LiveClassCard key={cls.id} class={cls} isLive={true} />
            ))}
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <Video className="size-5 text-muted-foreground" />
          All Live Classes
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Class</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Subject</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Teacher</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Scheduled</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground">Status</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Participants</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground">Actions</th>
              </tr>
            </thead>
            <tbody>
              {metrics.liveClasses.map((cls) => {
                const config = getStatusConfig(cls.status)
                return (
                  <tr key={cls.id} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4 font-medium text-foreground">{cls.title}</td>
                    <td className="py-3 px-4 text-muted-foreground">
                      <Link href={`/oversight/schools/${cls.institutionId}`} className="hover:underline">
                        {cls.institutionName}
                      </Link>
                    </td>
                    <td className="py-3 px-4 text-muted-foreground">{cls.subjectName}</td>
                    <td className="py-3 px-4 text-muted-foreground">{cls.teacherName}</td>
                    <td className="py-3 px-4 text-muted-foreground">{new Date(cls.scheduledAt).toLocaleString()}</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${config.color}`}>
                        {config.icon}
                        {config.label}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right text-foreground">{cls.participantCount} / {cls.maxParticipants}</td>
                    <td className="py-3 px-4 text-center">
                      {cls.status === "IN_PROGRESS" && (
                        <button
                          className="text-primary hover:underline text-sm flex items-center justify-center gap-1 mx-auto"
                          onClick={() => window.open(`/live-classes/${cls.id}`, "_blank")}
                        >
                          <Eye className="size-3" />
                          Observe
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function LiveClassCard({ class: cls, isLive }: { class: any; isLive: boolean }) {
  const config = getStatusConfig(cls.status)
  return (
    <div className="flex items-center justify-between rounded-xl border border-border p-4 hover:bg-muted/50 transition-colors">
      <div className="flex items-center gap-4">
        <div className="flex size-12 items-center justify-center rounded-lg bg-red-100">
          <Video className="size-6 text-red-600" />
        </div>
        <div>
          <p className="font-medium text-foreground">{cls.title}</p>
          <p className="text-sm text-muted-foreground">{cls.institutionName} • {cls.subjectName} • Teacher: {cls.teacherName}</p>
          <p className="text-xs text-muted-foreground">{cls.participantCount} / {cls.maxParticipants} participants • {cls.durationMinutes} min</p>
        </div>
      </div>
      <div className="flex items-center gap-3">
        <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${config.color}`}>
          {config.icon}
          {config.label}
        </span>
        {isLive && (
          <button
            className="text-primary hover:underline text-sm flex items-center gap-1"
            onClick={() => window.open(`/live-classes/${cls.id}`, "_blank")}
          >
            <Eye className="size-3" />
            Observe
          </button>
        )}
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

function getStatusConfig(status: string) {
  switch (status) {
    case "IN_PROGRESS": return { color: "bg-red-100 text-red-700", icon: <Play className="size-3" />, label: "LIVE NOW" }
    case "SCHEDULED": return { color: "bg-blue-100 text-blue-700", icon: <Clock className="size-3" />, label: "SCHEDULED" }
    case "COMPLETED": return { color: "bg-green-100 text-green-700", icon: <CheckCircle2 className="size-3" />, label: "COMPLETED" }
    case "CANCELLED": return { color: "bg-gray-100 text-gray-700", icon: <X className="size-3" />, label: "CANCELLED" }
    default: return { color: "bg-gray-100 text-gray-700", icon: <HelpCircle className="size-3" />, label: status }
  }
}

