"use client"

import { Bell, CheckCircle, AlertTriangle, Calendar, FileText, MessageSquare } from "lucide-react"

const mockNotifications = [
  { id: "1", type: "ATTENDANCE", title: "Attendance reminder", detail: "Mark attendance for Form 2A", time: "8:00 AM", read: false },
  { id: "2", type: "ASSIGNMENT", title: "Assignment deadline today", detail: "Math homework due for Form 3B", time: "Yesterday", read: false },
  { id: "3", type: "MEETING", title: "Staff meeting", detail: "Department meeting at 3pm", time: "Yesterday", read: true },
  { id: "4", type: "GRADE", title: "Results published", detail: "Term 1 results for Form 2A are ready", time: "2 days ago", read: true },
  { id: "5", type: "MESSAGE", title: "New message from Admin", detail: "Updated timetable for next week", time: "3 days ago", read: true },
]

const typeIcons: Record<string, typeof Bell> = {
  ATTENDANCE: AlertTriangle,
  ASSIGNMENT: FileText,
  MEETING: Calendar,
  GRADE: CheckCircle,
  MESSAGE: MessageSquare,
}

export default function TeacherNotificationsPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Notifications</h1>
        <p className="mt-1 text-sm text-muted-foreground">Stay updated on your teaching activities.</p>
      </div>

      <div className="space-y-3">
        {mockNotifications.map((n) => {
          const Icon = typeIcons[n.type] || Bell
          return (
            <div
              key={n.id}
              className={`flex items-start gap-3 rounded-2xl border border-border p-4 shadow-xs transition-colors hover:bg-muted/50 ${
                !n.read ? "bg-primary/5" : ""
              }`}
            >
              <div className={`mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full ${
                !n.read ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
              }`}>
                <Icon className="size-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-foreground">{n.title}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">{n.detail}</p>
                <p className="mt-1 text-[10px] text-muted-foreground">{n.time}</p>
              </div>
              {!n.read && (
                <div className="mt-1 size-2 shrink-0 rounded-full bg-primary" />
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
