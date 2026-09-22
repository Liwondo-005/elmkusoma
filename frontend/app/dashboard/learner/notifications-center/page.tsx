"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { collegeApi } from "@/lib/college-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Bell,
  BookOpen,
  Video,
  FileText,
  Target,
  Clock,
  Award,
  Users,
  AlertCircle,
  CheckCircle,
  Calendar,
  GraduationCap,
  Briefcase,
  Filter,
  Trash2,
  Eye,
  EyeOff,
} from "lucide-react"

type Notification = {
  id: string
  title: string
  message: string
  type: string
  read: boolean
  createdAt: string
  link?: string
}

const NOTIFICATION_CONFIG: Record<
  string,
  { icon: typeof Bell; color: string; bg: string; label: string; emoji: string }
> = {
  LIVE: { icon: Bell, color: "text-red-500", bg: "bg-red-500/10", label: "Live", emoji: "🔴" },
  COURSE: { icon: BookOpen, color: "text-blue-500", bg: "bg-blue-500/10", label: "Course", emoji: "📚" },
  ASSESSMENT: { icon: FileText, color: "text-amber-500", bg: "bg-amber-500/10", label: "Assessment", emoji: "📝" },
  RESEARCH: { icon: Award, color: "text-purple-500", bg: "bg-purple-500/10", label: "Research", emoji: "🔬" },
  PROJECT: { icon: Target, color: "text-cyan-500", bg: "bg-cyan-500/10", label: "Project", emoji: "📁" },
  ACADEMIC: { icon: GraduationCap, color: "text-indigo-500", bg: "bg-indigo-500/10", label: "Academic", emoji: "🎓" },
  REPLAY: { icon: Video, color: "text-emerald-500", bg: "bg-emerald-500/10", label: "Replay", emoji: "📹" },
  CAREER: { icon: Briefcase, color: "text-pink-500", bg: "bg-pink-500/10", label: "Career", emoji: "💼" },
}

const CATEGORIES = ["ALL", "LIVE", "COURSE", "ASSESSMENT", "RESEARCH", "PROJECT", "ACADEMIC", "CAREER"] as const
type CategoryFilter = (typeof CATEGORIES)[number]

function getTimeAgo(dateStr: string): string {
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diffMs = now - then
  const diffMin = Math.floor(diffMs / 60000)
  const diffHr = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) return "Just now"
  if (diffMin < 60) return `${diffMin} minute${diffMin > 1 ? "s" : ""} ago`
  if (diffHr < 24) return `${diffHr} hour${diffHr > 1 ? "s" : ""} ago`
  if (diffDay === 1) return "Yesterday"
  if (diffDay < 7) return `${diffDay} days ago`
  return new Date(dateStr).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
}

function getCategoryConfig(type: string) {
  return NOTIFICATION_CONFIG[type] || NOTIFICATION_CONFIG.COURSE
}

export default function NotificationsCenterPage() {
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()

  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeFilter, setActiveFilter] = useState<CategoryFilter>("ALL")
  const [markingId, setMarkingId] = useState<string | null>(null)
  const [markingAll, setMarkingAll] = useState(false)

  const fetchNotifications = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      setError(null)
      const [data, unread] = await Promise.all([
        collegeApi.getNotifications(user.id),
        collegeApi.getUnreadCount(user.id),
      ])
      setNotifications(Array.isArray(data) ? data : (data as any)?.data || [])
      setUnreadCount(typeof unread === "number" ? unread : (unread as any)?.data?.count || 0)
    } catch (err: any) {
      setError(err?.message || "Failed to load notifications")
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  const markAsRead = useCallback(
    async (id: string) => {
      try {
        setMarkingId(id)
        await collegeApi.markNotificationRead(id)
        setNotifications((prev) =>
          prev.map((n) => (n.id === id ? { ...n, read: true } : n))
        )
        setUnreadCount((prev) => Math.max(0, prev - 1))
      } catch {
        // silently fail — notification stays unread
      } finally {
        setMarkingId(null)
      }
    },
    []
  )

  const markAllAsRead = useCallback(async () => {
    if (!user || markingAll) return
    try {
      setMarkingAll(true)
      await collegeApi.markAllNotificationsRead(user.id)
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
      setUnreadCount(0)
    } catch {
      // silently fail
    } finally {
      setMarkingAll(false)
    }
  }, [user, markingAll])

  if (authLoading || !user || (user.role !== "Other Learner" && user.role !== "Student")) {
    return <LoadingState />
  }

  const firstName = user.firstName || user.name?.split(" ")[0] || "Student"

  const filtered =
    activeFilter === "ALL"
      ? notifications
      : notifications.filter((n) => n.type === activeFilter)

  return (
    <div className="mx-auto max-w-4xl space-y-6 pb-12">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <LearnerHeader firstName={firstName} subtitle="Stay updated with your academic notifications" />
          {unreadCount > 0 && (
            <span className="inline-flex items-center justify-center rounded-full bg-primary px-2.5 py-0.5 text-xs font-semibold text-primary-foreground">
              {unreadCount}
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllAsRead}
            disabled={markingAll}
            className="inline-flex items-center gap-2 self-start rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted disabled:opacity-50"
          >
            <CheckCircle className="size-4" />
            {markingAll ? "Marking..." : "Mark All Read"}
          </button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Filter className="size-4 text-muted-foreground" />
        {CATEGORIES.map((cat) => {
          const isActive = activeFilter === cat
          const config = cat !== "ALL" ? NOTIFICATION_CONFIG[cat] : null
          const count =
            cat === "ALL"
              ? notifications.length
              : notifications.filter((n) => n.type === cat).length
          return (
            <button
              key={cat}
              onClick={() => setActiveFilter(cat)}
              className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-all ${
                isActive
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "border border-border bg-background text-muted-foreground hover:bg-muted/50 hover:text-foreground"
              }`}
            >
              {config && <span>{config.emoji}</span>}
              {cat === "ALL" ? "All" : config?.label || cat}
              <span
                className={`ml-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                  isActive ? "bg-primary-foreground/20 text-primary-foreground" : "bg-muted text-muted-foreground"
                }`}
              >
                {count}
              </span>
            </button>
          )
        })}
      </div>

      {unreadCount > 0 && activeFilter === "ALL" && (
        <div className="flex items-center gap-2 rounded-xl bg-primary/5 px-4 py-2.5 text-sm text-primary">
          <Bell className="size-4" />
          <span>
            You have <strong>{unreadCount}</strong> unread notification{unreadCount > 1 ? "s" : ""}
          </span>
        </div>
      )}

      {loading ? (
        <LoadingState />
      ) : error ? (
        <div className="space-y-4">
          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 text-center">
            <AlertCircle className="mx-auto mb-3 size-8 text-destructive" />
            <p className="text-sm font-medium text-destructive">{error}</p>
            <button
              onClick={fetchNotifications}
              className="mt-3 inline-flex items-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              Retry
            </button>
          </div>
        </div>
      ) : filtered.length === 0 ? (
        <div className="space-y-4">
          <EmptyState
            icon={<Bell className="size-8" />}
            title={
              activeFilter === "ALL"
                ? "No notifications yet"
                : `No ${NOTIFICATION_CONFIG[activeFilter]?.label || activeFilter.toLowerCase()} notifications`
            }
            description={
              activeFilter === "ALL"
                ? "You're all caught up! Academic notifications will appear here."
                : `There are no notifications in the ${NOTIFICATION_CONFIG[activeFilter]?.label || activeFilter.toLowerCase()} category right now.`
            }
          />
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((notification) => {
            const config = getCategoryConfig(notification.type)
            const IconComponent = config.icon
            return (
              <div
                key={notification.id}
                className={`group rounded-2xl border p-5 shadow-xs transition-all duration-200 ${
                  notification.read
                    ? "border-border bg-card"
                    : "border-primary/20 bg-primary/5"
                }`}
              >
                <div className="flex items-start gap-4">
                  <div
                    className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${config.bg}`}
                  >
                    <IconComponent className={`size-5 ${config.color}`} />
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <h3
                            className={`text-sm leading-snug ${
                              notification.read
                                ? "font-medium text-foreground"
                                : "font-semibold text-foreground"
                            }`}
                          >
                            {notification.title}
                          </h3>
                          {!notification.read && (
                            <span className="size-2 shrink-0 rounded-full bg-blue-500" />
                          )}
                        </div>
                        <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground line-clamp-2">
                          {notification.message}
                        </p>
                      </div>
                    </div>

                    <div className="mt-3 flex flex-wrap items-center gap-3">
                      <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                        <Clock className="size-3" />
                        {getTimeAgo(notification.createdAt)}
                      </span>

                      <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-medium ${config.bg} ${config.color}`}>
                        {config.emoji} {config.label}
                      </span>

                      {notification.link && (
                        <button
                          onClick={() => router.push(notification.link!)}
                          className="inline-flex items-center gap-1 text-[10px] font-medium text-primary hover:underline"
                        >
                          View Details
                          <span className="text-xs">→</span>
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="flex shrink-0 items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                    {!notification.read && (
                      <button
                        onClick={() => markAsRead(notification.id)}
                        disabled={markingId === notification.id}
                        title="Mark as read"
                        className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
                      >
                        {markingId === notification.id ? (
                          <span className="size-4 animate-spin rounded-full border-2 border-muted-foreground border-t-transparent" />
                        ) : (
                          <Eye className="size-4" />
                        )}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="text-center text-xs text-muted-foreground">
          Showing {filtered.length} notification{filtered.length !== 1 ? "s" : ""}
          {activeFilter !== "ALL" && (
            <span>
              {" "}
              in{" "}
              <span className="font-medium text-foreground">
                {NOTIFICATION_CONFIG[activeFilter]?.label || activeFilter}
              </span>
            </span>
          )}
        </div>
      )}
    </div>
  )
}
