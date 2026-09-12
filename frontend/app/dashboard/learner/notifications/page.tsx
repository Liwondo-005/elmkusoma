"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth"
import { learnerApi, type LearnerNotification } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Bell, CheckCheck, AlertCircle, Clock, FileText, BookOpen, Video, Award } from "lucide-react"

export default function LearnerNotificationsPage() {
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()
  const [notifications, setNotifications] = useState<LearnerNotification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [marking, setMarking] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadNotifications()
  }, [user])

  async function loadNotifications() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getNotifications()
      setNotifications(data)
    } catch {
      setError("Failed to load notifications")
    } finally {
      setLoading(false)
    }
  }

  async function markRead(id: string) {
    try {
      setMarking(id)
      await learnerApi.markNotificationRead(id)
      setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, isRead: true } : n))
    } catch {
      // silent fail
    } finally {
      setMarking(null)
    }
  }

  async function markAllRead() {
    try {
      await learnerApi.markAllRead()
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
    } catch {
      // silent fail
    }
  }

  function getNotificationIcon(type: string) {
    switch (type?.toLowerCase()) {
      case "course": case "enrollment": return <BookOpen className="size-4 text-blue-500" />
      case "assignment": return <FileText className="size-4 text-orange" />
      case "liveclass": case "live_class": return <Video className="size-4 text-red-500" />
      case "certificate": return <Award className="size-4 text-yellow-500" />
      default: return <Bell className="size-4 text-primary" />
    }
  }

  function getTargetLink(notification: LearnerNotification) {
    if (!notification.targetType || !notification.targetId) return null
    switch (notification.targetType.toLowerCase()) {
      case "course": return `/dashboard/learner/courses/${notification.targetId}`
      case "liveclass": case "live_class": return `/dashboard/learner/live-classes`
      case "resource": return `/dashboard/learner/resources`
      case "certificate": return `/dashboard/learner/certificates`
      default: return null
    }
  }

  const unreadCount = notifications.filter((n) => !n.isRead).length

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Notifications</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {unreadCount > 0 ? `You have ${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}` : "All caught up!"}
          </p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllRead}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
          >
            <CheckCheck className="size-4" />
            Mark All Read
          </button>
        )}
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {loading ? (
        <LoadingState />
      ) : notifications.length === 0 ? (
        <EmptyState
          icon={<Bell className="size-8" />}
          title="No notifications"
          description="You're all caught up! Check back later for updates."
        />
      ) : (
        <div className="space-y-2">
          {notifications.map((notification) => {
            const targetLink = getTargetLink(notification)
            return (
              <div
                key={notification.id}
                className={`flex items-start gap-4 rounded-2xl border bg-card p-4 shadow-xs transition-colors ${
                  notification.isRead ? "border-border" : "border-primary/20 bg-primary/5"
                }`}
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-muted">
                  {getNotificationIcon(notification.notificationType)}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className={`text-sm font-medium ${notification.isRead ? "text-foreground" : "text-foreground"}`}>
                      {notification.title}
                    </h3>
                    {!notification.isRead && (
                      <span className="size-2 shrink-0 rounded-full bg-primary" />
                    )}
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{notification.message}</p>
                  <div className="mt-2 flex items-center gap-3">
                    <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                      <Clock className="size-3" />
                      {new Date(notification.createdAt).toLocaleString()}
                    </span>
                    {targetLink && (
                      <button
                        onClick={() => router.push(targetLink)}
                        className="text-[10px] font-medium text-primary hover:underline"
                      >
                        View Details
                      </button>
                    )}
                    {!notification.isRead && (
                      <button
                        onClick={() => markRead(notification.id)}
                        disabled={marking === notification.id}
                        className="text-[10px] font-medium text-muted-foreground hover:text-foreground disabled:opacity-50"
                      >
                        {marking === notification.id ? "Marking..." : "Mark as read"}
                      </button>
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
