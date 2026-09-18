"use client"

import { useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import Link from "next/link"
import { ArrowLeft, Loader2, Video, Clock, User, CheckCircle, AlertTriangle } from "lucide-react"
import { parentApi, type LiveClassData, type ChildOverview } from "@/lib/parent-api"

const statusConfig: Record<string, { label: string; color: string }> = {
  SCHEDULED: { label: "Scheduled", color: "bg-blue-500/10 text-blue-500" },
  IN_PROGRESS: { label: "Live Now", color: "bg-red-500/10 text-red-500" },
  COMPLETED: { label: "Completed", color: "bg-teal/10 text-teal" },
  CANCELLED: { label: "Cancelled", color: "bg-muted text-muted-foreground" },
}

export default function ParentLiveClassesPage() {
  const searchParams = useSearchParams()
  const childId = searchParams.get("child")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedId, setSelectedId] = useState(childId || "")
  const [liveClasses, setLiveClasses] = useState<LiveClassData | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (!selectedId && kids.length > 0) setSelectedId(kids[0].studentId)
    })
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setLoading(true)
    parentApi.getChildLiveClasses(selectedId).then(setLiveClasses).finally(() => setLoading(false))
  }, [selectedId])

  const now = new Date()
  const upcoming = liveClasses?.liveClasses.filter((lc) => lc.status === "SCHEDULED" && new Date(lc.scheduledAt) > now) || []
  const liveNow = liveClasses?.liveClasses.filter((lc) => lc.status === "IN_PROGRESS") || []
  const past = liveClasses?.liveClasses.filter((lc) => lc.status === "COMPLETED" || lc.status === "CANCELLED" || (lc.status === "SCHEDULED" && new Date(lc.scheduledAt) <= now)) || []

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/parent" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to Dashboard
      </Link>
      <h1 className="text-xl font-bold text-foreground">Live Classes</h1>

      {children.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {children.map((c) => (
            <button
              key={c.studentId}
              onClick={() => setSelectedId(c.studentId)}
              className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                selectedId === c.studentId ? "bg-primary text-primary-foreground" : "border border-border bg-card text-foreground hover:bg-muted"
              }`}
            >
              {c.studentName}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : liveClasses && liveClasses.liveClasses.length > 0 ? (
        <div className="space-y-6">
          {liveNow.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold text-foreground">Happening Now</h2>
              <div className="space-y-3">
                {liveNow.map((lc) => {
                  const st = statusConfig[lc.status] || statusConfig.SCHEDULED
                  return (
                    <div key={lc.id} className="rounded-2xl border border-red-500/20 bg-red-500/5 p-4 shadow-xs">
                      <div className="flex items-start justify-between">
                        <div className="flex items-start gap-3">
                          <div className="mt-0.5 size-2 shrink-0 animate-pulse rounded-full bg-red-500" />
                          <div>
                            <p className="text-sm font-semibold text-foreground">{lc.title}</p>
                            <p className="text-xs text-muted-foreground">{lc.teacherName}</p>
                          </div>
                        </div>
                        <span className={`shrink-0 rounded px-2 py-0.5 text-[10px] font-semibold ${st.color}`}>
                          {st.label}
                        </span>
                      </div>
                      {lc.description && (
                        <p className="mt-2 text-xs text-muted-foreground">{lc.description}</p>
                      )}
                      <div className="mt-3 flex gap-4 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1"><Clock className="size-3" /> {lc.durationMinutes} min</span>
                        <span className="flex items-center gap-1"><User className="size-3" /> {lc.teacherName}</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            </section>
          )}

          {upcoming.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold text-foreground">Upcoming</h2>
              <div className="space-y-3">
                {upcoming.map((lc) => {
                  const st = statusConfig[lc.status] || statusConfig.SCHEDULED
                  const scheduledDate = new Date(lc.scheduledAt)
                  const diffMs = scheduledDate.getTime() - now.getTime()
                  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
                  const diffDays = Math.floor(diffHours / 24)
                  let timeLabel = ""
                  if (diffDays > 0) timeLabel = `in ${diffDays} day${diffDays > 1 ? "s" : ""}`
                  else if (diffHours > 0) timeLabel = `in ${diffHours} hour${diffHours > 1 ? "s" : ""}`
                  else timeLabel = "starting soon"

                  return (
                    <div key={lc.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="text-sm font-semibold text-foreground">{lc.title}</p>
                          <p className="text-xs text-muted-foreground">{lc.teacherName}</p>
                        </div>
                        <span className={`shrink-0 rounded px-2 py-0.5 text-[10px] font-semibold ${st.color}`}>
                          {st.label}
                        </span>
                      </div>
                      {lc.description && (
                        <p className="mt-2 text-xs text-muted-foreground">{lc.description}</p>
                      )}
                      <div className="mt-3 flex gap-4 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Clock className="size-3" />
                          {scheduledDate.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                        </span>
                        <span className="flex items-center gap-1"><Clock className="size-3" /> {lc.durationMinutes} min</span>
                      </div>
                      <p className="mt-2 text-xs font-medium text-primary">{timeLabel}</p>
                    </div>
                  )
                })}
              </div>
            </section>
          )}

          {past.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold text-foreground">Past Classes</h2>
              <div className="space-y-3">
                {past.map((lc) => {
                  const st = statusConfig[lc.status] || statusConfig.COMPLETED
                  return (
                    <div key={lc.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs opacity-70">
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="text-sm font-semibold text-foreground">{lc.title}</p>
                          <p className="text-xs text-muted-foreground">{lc.teacherName}</p>
                        </div>
                        <span className={`shrink-0 rounded px-2 py-0.5 text-[10px] font-semibold ${st.color}`}>
                          {st.label}
                        </span>
                      </div>
                      <div className="mt-3 flex gap-4 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Clock className="size-3" />
                          {new Date(lc.scheduledAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>
            </section>
          )}
        </div>
      ) : liveClasses && liveClasses.liveClasses.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <Video className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No live classes available for this child.</p>
          <p className="mt-1 text-xs text-muted-foreground">Live classes will appear here when scheduled by the school.</p>
        </div>
      ) : (
        <p className="py-12 text-center text-sm text-muted-foreground">Select a child to view live classes.</p>
      )}
    </div>
  )
}
