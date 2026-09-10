"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi } from "@/lib/api"
import { Video, Calendar, Clock } from "lucide-react"

interface LiveClass {
  id?: string
  title?: string
  scheduledAt?: string
  status?: string
  meetingUrl?: string
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
            No upcoming live classes scheduled. Check back later or ask your teacher to schedule one.
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {classes.map((cls, i) => (
            <div key={cls.id || i} className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md">
              <div className="flex items-start justify-between">
                <div className="flex size-10 items-center justify-center rounded-xl bg-red-500/10">
                  <Video className="size-5 text-red-500" />
                </div>
                {cls.status && (
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                    cls.status === "LIVE" ? "bg-red-100 text-red-700" :
                    cls.status === "SCHEDULED" ? "bg-blue-100 text-blue-700" :
                    "bg-gray-100 text-gray-700"
                  }`}>
                    {cls.status}
                  </span>
                )}
              </div>
              <h3 className="mt-3 text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
              <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                {cls.scheduledAt && (
                  <div className="flex items-center gap-2">
                    <Calendar className="size-3" />
                    <span>{new Date(cls.scheduledAt).toLocaleDateString()}</span>
                  </div>
                )}
                {cls.scheduledAt && (
                  <div className="flex items-center gap-2">
                    <Clock className="size-3" />
                    <span>{new Date(cls.scheduledAt).toLocaleTimeString()}</span>
                  </div>
                )}
              </div>
              {cls.meetingUrl && (
                <a
                  href={cls.meetingUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-4 block rounded-xl bg-red-500 px-4 py-2.5 text-center text-sm font-semibold text-white hover:bg-red-600 transition-colors"
                >
                  Join Class
                </a>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
