"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type LiveClass } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Video, Calendar, Clock, Users, ExternalLink, Search, AlertCircle, Bookmark, BookmarkCheck, MessageSquare, Play } from "lucide-react"

export default function LearnerLiveClassesPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [bookmarkedIds, setBookmarkedIds] = useState<Set<string>>(new Set())

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) {
      setLoading(false)
      return
    }
    loadLiveClasses()
  }, [user])

  async function loadLiveClasses() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getLiveClasses()
      setLiveClasses(data)

      try {
        const bookmarks = await learnerApi.getBookmarks()
        const lcBookmarks = new Set(
          bookmarks.filter((b) => b.targetType === "liveclass" || b.targetType === "live_class").map((b) => b.targetId)
        )
        setBookmarkedIds(lcBookmarks)
      } catch {
        // Ignore bookmark check failure
      }
    } catch {
      setError(t("lclasses.loadError"))
    } finally {
      setLoading(false)
    }
  }

  async function toggleBookmark(liveClassId: string) {
    try {
      if (bookmarkedIds.has(liveClassId)) {
        const bookmarks = await learnerApi.getBookmarks()
        const existing = bookmarks.find(
          (b) => (b.targetType === "liveclass" || b.targetType === "live_class") && b.targetId === liveClassId
        )
        if (existing) {
          await learnerApi.removeBookmark(existing.id)
        }
        setBookmarkedIds((prev) => {
          const next = new Set(prev)
          next.delete(liveClassId)
          return next
        })
      } else {
        await learnerApi.addBookmark("liveclass", liveClassId)
        setBookmarkedIds((prev) => new Set(prev).add(liveClassId))
      }
    } catch {
      // Silent fail
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
      SCHEDULED: t("lclasses.upcomingBadge"),
      IN_PROGRESS: t("lclasses.liveBadge"),
      COMPLETED: t("lclasses.doneBadge"),
      CANCELLED: t("lclasses.cancelledBadge"),
    }
    return labels[status] || status
  }

  const filteredClasses = liveClasses.filter((cls) => {
    return search === "" || cls.title.toLowerCase().includes(search.toLowerCase())
  })

  const upcomingClasses = filteredClasses.filter((cls) => cls.status === "SCHEDULED")
  const liveNowClasses = filteredClasses.filter((cls) => cls.status === "IN_PROGRESS" || cls.status === "LIVE")
  const pastClasses = filteredClasses.filter((cls) => cls.status === "COMPLETED" || cls.status === "CANCELLED")

  if (authLoading) {
    return <LoadingState />
  }

  if (!user || (user.role !== "Other Learner" && user.role !== "Student")) {
    return (
      <EmptyState
        icon={<AlertCircle className="size-8" />}
        title={t("lclasses.gateTitle")}
        description={t("lclasses.gateDesc")}
      />
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("lclasses.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("lclasses.subtitle")}</p>
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
          placeholder={t("lclasses.searchPlaceholder")}
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
          title={t("lclasses.emptyTitle")}
          description={search ? t("lclasses.emptySearch") : t("lclasses.emptyDefault")}
        />
      ) : (
        <div className="space-y-8">
          {liveNowClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                <span className="size-2 rounded-full bg-green-500 animate-pulse" />
                {t("lclasses.liveNow")}
              </h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {liveNowClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-green-500/30 bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                        <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground flex-wrap">
                          {cls.subjectName && <span>{cls.subjectName}</span>}
                          {cls.teacherName && <span>• {cls.teacherName}</span>}
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <span className="rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                          {t("lclasses.liveBadgeShort")}
                        </span>
                        <button
                          onClick={() => toggleBookmark(cls.id)}
                          className="rounded-lg p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                          title={bookmarkedIds.has(cls.id) ? t("lclasses.removeBm") : t("lclasses.addBm")}
                        >
                          {bookmarkedIds.has(cls.id) ? (
                            <BookmarkCheck className="size-4 text-primary" />
                          ) : (
                            <Bookmark className="size-4" />
                          )}
                        </button>
                      </div>
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
                        {t("lclasses.minutesCount", { count: cls.durationMinutes })}
                      </div>
                    </div>
                    {(cls.status === "IN_PROGRESS" || cls.status === "LIVE") && (
                      <a
                        href={`/live-classes/${cls.id}`}
                        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
                      >
                        <Video className="size-4" />
                        {t("lclasses.joinLive")}
                      </a>
                    )}
                    <a
                      href={`/live-classes/${cls.id}`}
                      className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
                    >
                      <MessageSquare className="size-4" />
                      {t("lclasses.joinChat")}
                    </a>
                  </div>
                ))}
              </div>
            </section>
          )}

          {upcomingClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">{t("lclasses.upcomingTitle")}</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {upcomingClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                        <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground flex-wrap">
                          {cls.subjectName && <span>{cls.subjectName}</span>}
                          {cls.teacherName && <span>• {cls.teacherName}</span>}
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(cls.status)}`}>
                          {getStatusLabel(cls.status)}
                        </span>
                        <button
                          onClick={() => toggleBookmark(cls.id)}
                          className="rounded-lg p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                          title={bookmarkedIds.has(cls.id) ? t("lclasses.removeBm") : t("lclasses.addBm")}
                        >
                          {bookmarkedIds.has(cls.id) ? (
                            <BookmarkCheck className="size-4 text-primary" />
                          ) : (
                            <Bookmark className="size-4" />
                          )}
                        </button>
                      </div>
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
                        {t("lclasses.minutesCount", { count: cls.durationMinutes })}
                      </div>
                      {cls.maxParticipants && (
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <Users className="size-3" />
                          {t("lclasses.maxLine", { count: cls.maxParticipants })}
                        </div>
                      )}
                    </div>
                    <a
                      href={`/dashboard/learner/live-classes/${cls.id}`}
                      className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
                    >
                      <MessageSquare className="size-4" />
                      {t("lclasses.viewClassroom")}
                    </a>
                  </div>
                ))}
              </div>
            </section>
          )}

          {pastClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground text-muted-foreground">{t("lclasses.pastTitle")}</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {pastClasses.map((cls) => (
                  <div key={cls.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs opacity-80">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                      <div className="flex items-center gap-1.5">
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(cls.status)}`}>
                          {getStatusLabel(cls.status)}
                        </span>
                        <button
                          onClick={() => toggleBookmark(cls.id)}
                          className="rounded-lg p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                          title={bookmarkedIds.has(cls.id) ? t("lclasses.removeBm") : t("lclasses.addBm")}
                        >
                          {bookmarkedIds.has(cls.id) ? (
                            <BookmarkCheck className="size-4 text-primary" />
                          ) : (
                            <Bookmark className="size-4" />
                          )}
                        </button>
                      </div>
                    </div>
                    <div className="mt-3 space-y-1">
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Calendar className="size-3" />
                        {new Date(cls.scheduledAt).toLocaleDateString()}
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="size-3" />
                        {t("lclasses.minutesCount", { count: cls.durationMinutes })}
                      </div>
                    </div>
                    <div className="mt-3 flex gap-2">
                      {cls.recordingUrl ? (
                        <a
                          href={cls.recordingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                        >
                          <Play className="size-3" />
                          {t("lclasses.watchRecording")}
                        </a>
                      ) : (
                        <a
                          href={`/live-classes/${cls.id}`}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                        >
                          <MessageSquare className="size-3" />
                          {t("lclasses.viewDetails")}
                        </a>
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
