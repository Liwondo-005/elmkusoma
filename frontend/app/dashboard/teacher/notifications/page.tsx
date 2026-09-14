"use client"

import { Bell, Construction } from "lucide-react"

export default function TeacherNotificationsPage() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Notifications</h1>
        <p className="mt-1 text-sm text-muted-foreground">Stay updated on your teaching activities.</p>
      </div>

      <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
        <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-muted">
          <Construction className="size-7 text-muted-foreground" />
        </div>
        <h3 className="mt-4 text-lg font-semibold text-foreground">Notifications Coming Soon</h3>
        <p className="mt-2 max-w-md mx-auto text-sm text-muted-foreground">
          The notification system is under development. You will receive alerts for attendance reminders, assignment deadlines, new messages, and grade publications.
        </p>
        <div className="mt-6 rounded-xl border border-border bg-muted/40 p-4 max-w-sm mx-auto">
          <p className="text-xs text-muted-foreground">
            <strong>Planned notifications:</strong> Attendance alerts, assignment deadlines, message alerts, grade publications, and schedule changes.
          </p>
        </div>
      </div>
    </div>
  )
}
