"use client"

import { Bell, AlertTriangle, CheckCircle, Clock, Info, Mail, Loader2 } from "lucide-react"
import { useEffect, useState } from "react"
import { parentApi, type ParentNotificationItem } from "@/lib/parent-api"

const typeIcons: Record<string, typeof Bell> = {
  ABSENCE: AlertTriangle,
  ASSIGNMENT: AlertTriangle,
  GRADE: CheckCircle,
  EVENT: Clock,
  LIVE_CLASS: Info,
  PAYMENT: Mail,
  ATTENDANCE: AlertTriangle,
  ASSESSMENT: CheckCircle,
}

const typeColors: Record<string, string> = {
  ABSENCE: "text-orange",
  ASSIGNMENT: "text-red-500",
  GRADE: "text-teal",
  EVENT: "text-primary",
  LIVE_CLASS: "text-primary",
  PAYMENT: "text-orange",
  ATTENDANCE: "text-orange",
  ASSESSMENT: "text-purple-600",
}

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return "Just now"
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days === 1) return "Yesterday"
  if (days < 7) return `${days} days ago`
  return new Date(dateStr).toLocaleDateString("en-GB", { month: "short", day: "numeric" })
}

export default function ParentNotificationsPage() {
  const [notifications, setNotifications] = useState<ParentNotificationItem[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getNotifications(0, 50).then((data) => {
      setNotifications(data.notifications || [])
      setUnreadCount(data.unreadCount || 0)
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [])

  async function markRead(id: string) {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)))
    setUnreadCount((c) => Math.max(0, c - 1))
    parentApi.markNotificationRead(id).catch(() => {})
  }

  async function markAllRead() {
    const unread = notifications.filter((n) => !n.isRead)
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
    setUnreadCount(0)
    for (const n of unread) {
      parentApi.markNotificationRead(n.id).catch(() => {})
    }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">Notifications</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}` : "All caught up"}
          </p>
        </div>
        {unreadCount > 0 && (
          <button onClick={markAllRead} className="rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20">
            Mark all read
          </button>
        )}
      </div>

      <div className="space-y-2">
        {notifications.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border py-16 text-center">
            <Bell className="mx-auto mb-3 size-10 text-muted-foreground" />
            <p className="text-sm font-medium text-foreground">No notifications</p>
            <p className="mt-1 text-xs text-muted-foreground">You&apos;re all caught up.</p>
          </div>
        ) : (
          notifications.map((n) => {
            const Icon = typeIcons[n.category] || Bell
            const color = typeColors[n.category] || "text-muted-foreground"
            return (
              <button key={n.id} onClick={() => markRead(n.id)}
                className={`w-full rounded-2xl border border-border p-4 text-left shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md ${
                  !n.isRead ? "bg-primary/5" : "bg-card"
                }`}>
                <div className="flex items-start gap-3">
                  <div className={`mt-0.5 shrink-0 ${color}`}>
                    <Icon className="size-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-semibold text-foreground">{n.title}</p>
                      {!n.isRead && <span className="size-2 shrink-0 rounded-full bg-primary" />}
                    </div>
                    <p className="text-xs text-muted-foreground">{n.message}</p>
                    <div className="mt-1 flex items-center gap-2">
                      {n.category && <span className="text-[10px] text-muted-foreground">{n.category}</span>}
                      <span className="text-[10px] text-muted-foreground">&middot;</span>
                      <span className="text-[10px] text-muted-foreground">{timeAgo(n.createdAt)}</span>
                    </div>
                  </div>
                </div>
              </button>
            )
          })
        )}
      </div>
    </div>
  )
}
