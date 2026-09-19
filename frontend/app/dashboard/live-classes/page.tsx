"use client"

import { useEffect, useState, useMemo } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Video, Calendar, Clock, Users, Play, CheckCircle, ArrowRight, User } from "lucide-react"

interface LiveClass {
  id?: string
  title?: string
  description?: string
  scheduledAt?: string
  status?: string
  subjectName?: string
  teacherName?: string
  maxParticipants?: number
  durationMinutes?: number
  recordingUrl?: string
}

function isToday(dateStr?: string): boolean {
  if (!dateStr) return false
  const d = new Date(dateStr)
  const now = new Date()
  return (
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  )
}

function wasRecentlyCompleted(dateStr?: string): boolean {
  if (!dateStr) return false
  const d = new Date(dateStr)
  const now = new Date()
  const diffHours = (now.getTime() - d.getTime()) / (1000 * 60 * 60)
  return diffHours >= 0 && diffHours <= 48
}

export default function LiveClassesPage() {
  const { user } = useRequireAuth()
  const [classes, setClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

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
  const completedClasses = classes.filter((c) => c.status === "COMPLETED" || c.status === "ENDED")
  const todayLive = useMemo(
    () => classes.filter((c) => (c.status === "IN_PROGRESS" || c.status === "LIVE" || c.status === "SCHEDULED") && isToday(c.scheduledAt)),
    [classes]
  )
  const recentlyCompleted = useMemo(
    () => completedClasses.filter((c) => wasRecentlyCompleted(c.scheduledAt)),
    [completedClasses]
  )
  const replays = completedClasses.filter((c) => c.recordingUrl && !wasRecentlyCompleted(c.scheduledAt))

  if (isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-gradient-to-br from-red-50 via-card to-orange-50 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-red-500/10">
              <Video className="size-6 text-red-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">Live Learning</h1>
              <p className="text-sm text-muted-foreground">Join your teacher for live lessons and learning activities.</p>
            </div>
          </div>
        </div>

        {classes.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border py-12 text-center">
            <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-muted">
              <Video className="size-8 text-muted-foreground" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-foreground">No live classes yet</h3>
            <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
              Your teacher will schedule live sessions soon. Check back later!
            </p>
          </div>
        ) : (
          <div className="space-y-8">
            {liveClasses.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                  <span className="size-2.5 rounded-full bg-red-500 animate-pulse" />
                  Live Now
                </h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {liveClasses.map((cls) => (
                    <div key={cls.id} className="rounded-2xl border-2 border-red-500/30 bg-gradient-to-br from-red-50 to-card p-5 shadow-md">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-red-500 px-2.5 py-1 text-xs font-bold text-white">
                          <span className="size-1.5 rounded-full bg-white animate-pulse" />
                          LIVE
                        </span>
                        {cls.durationMinutes && (
                          <span className="text-xs text-muted-foreground flex items-center gap-1">
                            <Clock className="size-3" /> {cls.durationMinutes} min
                          </span>
                        )}
                      </div>
                      <h3 className="mt-3 text-base font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      {cls.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                      )}
                      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                        {cls.subjectName && <span className="font-medium text-primary">{cls.subjectName}</span>}
                        {cls.teacherName && (
                          <span className="flex items-center gap-1">
                            <User className="size-3" /> {cls.teacherName}
                          </span>
                        )}
                      </div>
                      <p className="mt-2 text-xs text-green-600 font-medium flex items-center gap-1">
                        <CheckCircle className="size-3" /> You are eligible to join
                      </p>
                      <Link
                        href={`/live-classes/${cls.id}`}
                        className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-red-700 transition-colors"
                      >
                        <Video className="size-4" />
                        Join Now!
                      </Link>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {todayLive.length > 0 && todayLive.length !== liveClasses.length && (
              <section>
                <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                  <Calendar className="size-5 text-primary" />
                  Today&apos;s Live
                </h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {todayLive.filter((c) => c.status === "SCHEDULED").map((cls) => (
                    <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-700">
                          <Calendar className="size-3" /> Today
                        </span>
                        {cls.durationMinutes && (
                          <span className="text-xs text-muted-foreground flex items-center gap-1">
                            <Clock className="size-3" /> {cls.durationMinutes} min
                          </span>
                        )}
                      </div>
                      <h3 className="mt-3 text-base font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      {cls.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                      )}
                      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                        {cls.subjectName && <span className="font-medium text-primary">{cls.subjectName}</span>}
                        {cls.teacherName && (
                          <span className="flex items-center gap-1">
                            <User className="size-3" /> {cls.teacherName}
                          </span>
                        )}
                      </div>
                      {cls.scheduledAt && (
                        <p className="mt-2 text-xs text-muted-foreground flex items-center gap-1.5">
                          <Clock className="size-3" />
                          {new Date(cls.scheduledAt).toLocaleTimeString("en-GB", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            )}

            {scheduledClasses.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-foreground">Coming Up</h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {scheduledClasses.map((cls) => (
                    <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-700">
                          <Calendar className="size-3" /> Upcoming
                        </span>
                        {cls.durationMinutes && (
                          <span className="text-xs text-muted-foreground flex items-center gap-1">
                            <Clock className="size-3" /> {cls.durationMinutes} min
                          </span>
                        )}
                      </div>
                      <h3 className="mt-3 text-base font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      {cls.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                      )}
                      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                        {cls.subjectName && <span className="font-medium text-primary">{cls.subjectName}</span>}
                        {cls.teacherName && (
                          <span className="flex items-center gap-1">
                            <User className="size-3" /> {cls.teacherName}
                          </span>
                        )}
                      </div>
                      {cls.scheduledAt && (
                        <p className="mt-2 text-xs text-muted-foreground flex items-center gap-1.5">
                          <Clock className="size-3" />
                          {new Date(cls.scheduledAt).toLocaleDateString("en-GB", {
                            weekday: "short",
                            month: "short",
                            day: "numeric",
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            )}

            {recentlyCompleted.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-foreground">Recently Completed</h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {recentlyCompleted.map((cls) => (
                    <div key={cls.id} className="rounded-2xl border border-border bg-card p-5">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-700">
                          <CheckCircle className="size-3" /> Just Ended
                        </span>
                      </div>
                      <h3 className="mt-3 text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      {cls.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                      )}
                      <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                        {cls.subjectName && <span>{cls.subjectName}</span>}
                        {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                      </div>
                      {cls.recordingUrl && (
                        <a
                          href={cls.recordingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="mt-3 inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
                        >
                          <Play className="size-3" /> Watch Replay
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            )}

            {replays.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-foreground">Replays</h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {replays.map((cls) => (
                    <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 opacity-80">
                      <div className="flex items-start justify-between">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-green-100 px-2.5 py-1 text-xs font-semibold text-green-700">
                          <CheckCircle className="size-3" /> Completed
                        </span>
                      </div>
                      <h3 className="mt-3 text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      <div className="mt-1 text-xs text-muted-foreground">
                        {cls.subjectName && <span>{cls.subjectName}</span>}
                        {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                      </div>
                      {cls.recordingUrl && (
                        <a
                          href={cls.recordingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="mt-3 inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
                        >
                          <Play className="size-3" /> Watch Replay
                        </a>
                      )}
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
                    {cls.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                    )}
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <span className="flex items-center gap-1">
                          <Calendar className="size-3" />
                          {new Date(cls.scheduledAt).toLocaleString()}
                        </span>
                      )}
                      {cls.durationMinutes && (
                        <span className="flex items-center gap-1">
                          <Clock className="size-3" /> {cls.durationMinutes} min
                        </span>
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

          {todayLive.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                <Calendar className="size-5 text-primary" />
                Today&apos;s Live
              </h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {todayLive.filter((c) => c.status === "SCHEDULED").map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700">Today</span>
                    </div>
                    {cls.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                    )}
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <span className="flex items-center gap-1">
                          <Clock className="size-3" />
                          {new Date(cls.scheduledAt).toLocaleTimeString("en-GB", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                      )}
                      {cls.durationMinutes && (
                        <span className="flex items-center gap-1">
                          <Clock className="size-3" /> {cls.durationMinutes} min
                        </span>
                      )}
                    </div>
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
                    {cls.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                    )}
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <div className="flex items-center gap-2">
                          <Calendar className="size-3" />
                          <span>{new Date(cls.scheduledAt).toLocaleString()}</span>
                        </div>
                      )}
                      {cls.durationMinutes && (
                        <div className="flex items-center gap-2">
                          <Clock className="size-3" />
                          <span>{cls.durationMinutes} minutes</span>
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

          {recentlyCompleted.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Recently Completed</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {recentlyCompleted.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title || "Live Session"}</h3>
                      <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-700">Just Ended</span>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                      {cls.subjectName && <span>{cls.subjectName}</span>}
                      {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                    </div>
                    {cls.recordingUrl && (
                      <a
                        href={cls.recordingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="mt-3 inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
                      >
                        <Play className="size-3" /> Watch Replay
                      </a>
                    )}
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
                      {cls.teacherName && <span className="flex items-center gap-1"><User className="size-3" /> {cls.teacherName}</span>}
                    </div>
                    <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                      {cls.scheduledAt && (
                        <div className="flex items-center gap-2">
                          <Calendar className="size-3" />
                          <span>{new Date(cls.scheduledAt).toLocaleString()}</span>
                        </div>
                      )}
                      {cls.durationMinutes && (
                        <div className="flex items-center gap-2">
                          <Clock className="size-3" />
                          <span>{cls.durationMinutes} min</span>
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
