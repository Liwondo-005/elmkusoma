"use client"

import { Bell, AlertTriangle, CheckCircle, Clock, Info, Mail } from "lucide-react"
import { useState } from "react"

const mockNotifications = [
  { id: "1", type: "ABSENCE", title: "Absent Today", detail: "Amina Juma was marked absent for Mathematics", childName: "Amina Juma", time: "Today, 8:30 AM", read: false, priority: "HIGH" },
  { id: "2", type: "ASSIGNMENT", title: "Assignment Overdue", detail: "Science project due yesterday - not submitted", childName: "Amina Juma", time: "Yesterday", read: false, priority: "HIGH" },
  { id: "3", type: "GRADE", title: "New Report Card", detail: "Term 1 report card published", childName: "Juma Juma", time: "2 days ago", read: true, priority: "NORMAL" },
  { id: "4", type: "EVENT", title: "Parent-Teacher Meeting", detail: "Scheduled for Friday at 3:00 PM", childName: "All children", time: "3 days ago", read: true, priority: "NORMAL" },
  { id: "5", type: "LIVE_CLASS", title: "Live Class Reminder", detail: "Physics class starts in 30 minutes", childName: "Juma Juma", time: "4 days ago", read: true, priority: "NORMAL" },
  { id: "6", type: "PAYMENT", title: "Fee Reminder", detail: "Term 2 fees due in 5 days", childName: "All children", time: "5 days ago", read: true, priority: "NORMAL" },
]

const typeIcons: Record<string, typeof Bell> = {
  ABSENCE: AlertTriangle,
  ASSIGNMENT: AlertTriangle,
  GRADE: CheckCircle,
  EVENT: Clock,
  LIVE_CLASS: Info,
  PAYMENT: Mail,
}

const typeColors: Record<string, string> = {
  ABSENCE: "text-orange",
  ASSIGNMENT: "text-red-500",
  GRADE: "text-teal",
  EVENT: "text-primary",
  LIVE_CLASS: "text-primary",
  PAYMENT: "text-orange",
}

export default function ParentNotificationsPage() {
  const [notifications, setNotifications] = useState(mockNotifications)
  const unreadCount = notifications.filter((n) => !n.read).length

  function markAllRead() {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  }

  function markRead(id: string) {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)))
  }

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
          <button
            onClick={markAllRead}
            className="rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/20"
          >
            Mark all read
          </button>
        )}
      </div>

      <div className="space-y-2">
        {notifications.map((n) => {
          const Icon = typeIcons[n.type] || Bell
          const color = typeColors[n.type] || "text-muted-foreground"
          return (
            <button
              key={n.id}
              onClick={() => markRead(n.id)}
              className={`w-full rounded-2xl border border-border p-4 text-left shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md ${
                !n.read ? "bg-primary/5" : "bg-card"
              }`}
            >
              <div className="flex items-start gap-3">
                <div className={`mt-0.5 shrink-0 ${color}`}>
                  <Icon className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">{n.title}</p>
                    {!n.read && <span className="size-2 shrink-0 rounded-full bg-primary" />}
                  </div>
                  <p className="text-xs text-muted-foreground">{n.detail}</p>
                  <div className="mt-1 flex items-center gap-2">
                    <span className="text-[10px] text-muted-foreground">{n.childName}</span>
                    <span className="text-[10px] text-muted-foreground">&middot;</span>
                    <span className="text-[10px] text-muted-foreground">{n.time}</span>
                  </div>
                </div>
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}
