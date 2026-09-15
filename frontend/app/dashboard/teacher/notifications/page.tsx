"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import {
  Bell,
  CheckCircle,
  BookOpen,
  Video,
  GraduationCap,
  FileText,
  Clock,
  CheckCheck,
  Loader2,
  AlertCircle,
  Megaphone,
  Award,
} from "lucide-react"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function teacherFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const institutionId =
    typeof window !== "undefined"
      ? localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001"
      : "00000000-0000-0000-0000-000000000001"
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

interface Notification {
  id: string
  title: string
  message: string
  notificationType: string
  targetType: string | null
  targetId: string | null
  isRead: boolean
  createdAt: string
}

function getNotificationIcon(type: string) {
  switch (type?.toUpperCase()) {
    case "ENROLLMENT":
      return <BookOpen className="size-4 text-blue-500" />
    case "COURSE_COMPLETION":
      return <GraduationCap className="size-4 text-green-500" />
    case "COURSE_UPDATE":
      return <FileText className="size-4 text-orange-500" />
    case "COURSE_ANNOUNCEMENT":
      return <Megaphone className="size-4 text-purple-500" />
    case "ASSIGNMENT_GRADED":
      return <Award className="size-4 text-yellow-500" />
    case "CERTIFICATE":
      return <GraduationCap className="size-4 text-amber-500" />
    case "VIDEO":
      return <Video className="size-4 text-red-500" />
    default:
      return <Bell className="size-4 text-primary" />
  }
}

function getNotificationTypeBadge(type: string) {
  const map: Record<string, { label: string; className: string }> = {
    ENROLLMENT: {
      label: "Enrollment",
      className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    },
    COURSE_COMPLETION: {
      label: "Completion",
      className: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
    },
    COURSE_UPDATE: {
      label: "Update",
      className: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
    },
    COURSE_ANNOUNCEMENT: {
      label: "Announcement",
      className: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    },
    ASSIGNMENT_GRADED: {
      label: "Graded",
      className: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
    },
    CERTIFICATE: {
      label: "Certificate",
      className: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    },
  }
  return map[type?.toUpperCase()] || {
    label: type?.replace(/_/g, " ") || "Notification",
    className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400",
  }
}

function relativeTime(iso: string) {
  if (!iso) return ""
  const now = Date.now()
  const then = new Date(iso).getTime()
  const diffSec = Math.floor((now - then) / 1000)
  if (diffSec < 60) return "Just now"
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin} minute${diffMin > 1 ? "s" : ""} ago`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr} hour${diffHr > 1 ? "s" : ""} ago`
  const diffDay = Math.floor(diffHr / 24)
  if (diffDay === 1) return "Yesterday"
  if (diffDay < 7) return `${diffDay} days ago`
  return new Date(iso).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })
}

export default function TeacherNotificationsPage() {
  const { user } = useAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [marking, setMarking] = useState<string | null>(null)
  const [filter, setFilter] = useState<"all" | "unread">("all")
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    if (!user) return
    loadNotifications()
  }, [user])

  async function loadNotifications() {
    try {
      setLoading(true)
      setError(null)
      const [data, unread] = await Promise.all([
        teacherFetch<Notification[]>("/v1/learner/me/notifications"),
        teacherFetch<{ count: number }>("/v1/learner/me/notifications/unread-count").catch(() => ({ count: 0 })),
      ])
      setNotifications(data)
      setUnreadCount(unread.count)
    } catch {
      setError("Failed to load notifications")
    } finally {
      setLoading(false)
    }
  }

  async function markRead(id: string) {
    const notif = notifications.find((n) => n.id === id)
    if (!notif || notif.isRead) return
    try {
      setMarking(id)
      await teacherFetch(`/v1/learner/me/notifications/${id}/read`, { method: "PUT" })
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)))
      setUnreadCount((prev) => Math.max(0, prev - 1))
    } catch {
      // silent fail
    } finally {
      setMarking(null)
    }
  }

  async function markAllRead() {
    try {
      await teacherFetch("/v1/learner/me/notifications/read-all", { method: "PUT" })
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
      setUnreadCount(0)
    } catch {
      // silent fail
    }
  }

  const filtered = filter === "unread" ? notifications.filter((n) => !n.isRead) : notifications

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            Notifications
            {unreadCount > 0 && (
              <span className="ml-2 inline-flex size-6 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
                {unreadCount > 99 ? "99+" : unreadCount}
              </span>
            )}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {unreadCount > 0
              ? `You have ${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}`
              : "All caught up!"}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button variant="outline" className="gap-2" onClick={markAllRead}>
            <CheckCheck className="size-4" />
            Mark All Read
          </Button>
        )}
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      )}

      <div className="flex gap-2 border-b border-border">
        <button
          onClick={() => setFilter("all")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            filter === "all" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          All ({notifications.length})
        </button>
        <button
          onClick={() => setFilter("unread")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            filter === "unread" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          Unread ({unreadCount})
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
          <Bell className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            {filter === "unread" ? "No unread notifications" : "No notifications"}
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {filter === "unread"
              ? "All your notifications have been read."
              : "You're all caught up! Check back later for updates."}
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((notification) => {
            const badge = getNotificationTypeBadge(notification.notificationType)
            return (
              <div
                key={notification.id}
                onClick={() => markRead(notification.id)}
                className={`flex items-start gap-4 rounded-2xl border bg-card p-4 shadow-xs transition-all cursor-pointer hover:shadow-md ${
                  notification.isRead
                    ? "border-border"
                    : "border-primary/20 bg-primary/5"
                } ${marking === notification.id ? "opacity-60" : ""}`}
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-muted">
                  {getNotificationIcon(notification.notificationType)}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <h3
                      className={`text-sm ${
                        notification.isRead ? "font-medium text-foreground" : "font-semibold text-foreground"
                      }`}
                    >
                      {notification.title}
                    </h3>
                    {!notification.isRead && (
                      <span className="size-2 shrink-0 mt-1 rounded-full bg-primary" />
                    )}
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{notification.message}</p>
                  <div className="mt-2 flex items-center gap-3 flex-wrap">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${badge.className}`}>
                      {badge.label}
                    </span>
                    <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                      <Clock className="size-3" />
                      {relativeTime(notification.createdAt)}
                    </span>
                    {notification.isRead && (
                      <span className="flex items-center gap-1 text-[10px] text-green-600">
                        <CheckCircle className="size-3" />
                        Read
                      </span>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
