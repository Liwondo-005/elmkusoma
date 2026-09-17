"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi } from "@/lib/api"
import { Video, Calendar, Clock, Users, ExternalLink } from "lucide-react"

interface LiveClass {
  id?: string
  title?: string
  scheduledAt?: string
  status?: string
  subjectName?: string
  teacherName?: string
  maxParticipants?: number
}

export default function LiveClassesPage() {
  const { user } = useRequireAuth()
  const [classes, setClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await dashboardApi.getLiveClasses().catch(() => [])
      setClasses(data as LiveClass[])
    } catch {
      setClasses([])
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const liveClasses = classes.filter((c) => c.status === "IN_PROGRESS" || c.status === "LIVE")
  const scheduledClasses = classes.filter((c) => c.status === "SCHEDULED")
  const otherClasses = classes.filter((c) => !["IN_PROGRESS", "LIVE", "SCHEDULED"].includes(c.status || ""))

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Live Classes</h1>
        <p className="mt-1 text-sm text-muted-foreground">Join scheduled live sessions with your teachers.</p>
      </div>

      {classes.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Live Classes</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            No upcoming live classes scheduled. Check back later.
          </p>
        </div>
      ) : (
        <div className="space-y-8">
          {liveClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                <span className="size-2 rounded-full bg-green-500 animate-pulse" />
                Live Now
              </h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {liveClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-green-500/30 bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      <span className="rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">LIVE</span>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span>• {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <div className="flex items-center gap-2">
                          <Calendar className="size-3" />
                          <span>{new Date(cls.scheduledAt).toLocaleString()}</span>
                        </div>
                      )}
                    </div>
                    <a
                      href={`/live-classes/${cls.id}`}
                      className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
                    >
                      <Video className="size-4" />
                      Join Now
                    </a>
                  </div>
                ))}
              </div>
            </section>
          )}

          {scheduledClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Upcoming Classes</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {scheduledClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700">Scheduled</span>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span>• {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <div className="flex items-center gap-2">
                          <Calendar className="size-3" />
                          <span>{new Date(cls.scheduledAt).toLocaleString()}</span>
                        </div>
                      )}
                      {cls.maxParticipants && (
                        <div className="flex items-center gap-2">
                          <Users className="size-3" />
                          <span>{cls.maxParticipants} max</span>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {otherClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground text-muted-foreground">Past Classes</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {otherClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 opacity-70">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-semibold text-gray-600">{cls.status}</span>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span>• {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <div className="flex items-center gap-2">
                          <Calendar className="size-3" />
                          <span>{new Date(cls.scheduledAt).toLocaleString()}</span>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  )
}
