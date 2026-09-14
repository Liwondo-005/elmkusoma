"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learnerApi, type LiveClass } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Video, Calendar, Clock, Users, ExternalLink, Search, AlertCircle } from "lucide-react"

export default function LearnerLiveClassesPage() {
  const { user, loading: authLoading } = useAuth()
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadLiveClasses()
  }, [user])

  async function loadLiveClasses() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getLiveClasses()
      setLiveClasses(data)
    } catch {
      setError("Failed to load live classes")
    } finally {
      setLoading(false)
    }
  }

  function getStatusBadge(status: string) {
    const styles: Record<string, string> = {
      SCHEDULED: "bg-blue-500/10 text-blue-500",
      IN_PROGRESS: "bg-green-500/10 text-green-600",
      COMPLETED: "bg-muted text-muted-foreground",
      CANCELLED: "bg-red-500/10 text-red-500",
    }
    return styles[status] || "bg-muted text-muted-foreground"
  }

  function getStatusLabel(status: string) {
    const labels: Record<string, string> = {
      SCHEDULED: "Upcoming",
      IN_PROGRESS: "Live Now",
      COMPLETED: "Completed",
      CANCELLED: "Cancelled",
    }
    return labels[status] || status
  }

  const filteredClasses = liveClasses.filter((cls) => {
    return search === "" || cls.title.toLowerCase().includes(search.toLowerCase())
  })

  const upcomingClasses = filteredClasses.filter((cls) => cls.status === "SCHEDULED")
  const liveNowClasses = filteredClasses.filter((cls) => cls.status === "IN_PROGRESS")
  const pastClasses = filteredClasses.filter((cls) => cls.status === "COMPLETED" || cls.status === "CANCELLED")

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Live Classes</h1>
        <p className="mt-1 text-sm text-muted-foreground">Join live sessions with teachers.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search live classes..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
        />
      </div>

      {loading ? (
        <LoadingState />
      ) : filteredClasses.length === 0 ? (
        <EmptyState
          icon={<Video className="size-8" />}
          title="No live classes found"
          description={search ? "Try adjusting your search." : "No live classes scheduled yet."}
        />
      ) : (
        <div className="space-y-8">
          {liveNowClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                <span className="size-2 rounded-full bg-green-500 animate-pulse" />
                Live Now
              </h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {liveNowClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-green-500/30 bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                      <span className="rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                        LIVE
                      </span>
                    </div>
                    {cls.description && (
                      <p className="mt-2 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                    )}
                    <div className="mt-3 space-y-1">
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Calendar className="size-3" />
                        {new Date(cls.scheduledAt).toLocaleDateString()}
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="size-3" />
                        {cls.durationMinutes} minutes
                      </div>
                    </div>
                    {cls.meetingUrl && (
                      <a
                        href={cls.meetingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
                      >
                        <ExternalLink className="size-4" />
                        Join Now
                      </a>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {upcomingClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Upcoming Classes</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {upcomingClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(cls.status)}`}>
                        {getStatusLabel(cls.status)}
                      </span>
                    </div>
                    {cls.description && (
                      <p className="mt-2 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                    )}
                    <div className="mt-3 space-y-1">
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Calendar className="size-3" />
                        {new Date(cls.scheduledAt).toLocaleString()}
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="size-3" />
                        {cls.durationMinutes} minutes
                      </div>
                      {cls.maxParticipants && (
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <Users className="size-3" />
                          Max {cls.maxParticipants} participants
                        </div>
                      )}
                    </div>
                    {cls.meetingUrl && (
                      <a
                        href={cls.meetingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
                      >
                        <ExternalLink className="size-4" />
                        Join Link
                      </a>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {pastClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground text-muted-foreground">Past Classes</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {pastClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs opacity-60">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(cls.status)}`}>
                        {getStatusLabel(cls.status)}
                      </span>
                    </div>
                    <div className="mt-3 space-y-1">
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Calendar className="size-3" />
                        {new Date(cls.scheduledAt).toLocaleDateString()}
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="size-3" />
                        {cls.durationMinutes} minutes
                      </div>
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
