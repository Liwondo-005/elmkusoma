"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, type RecentActivity } from "@/lib/api"
import { Bell, CheckCircle, Clock, AlertCircle } from "lucide-react"

export default function NotificationsPage() {
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await dashboardApi.getRecentActivity().catch(() => [])
      setActivities(data)
    } catch {
      setActivities([])
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

  function getIcon(type: string) {
    switch (type) {
      case "lesson_completed": return <CheckCircle className="size-4 text-green-600" />
      case "attendance": return <Clock className="size-4 text-blue-600" />
      default: return <AlertCircle className="size-4 text-muted-foreground" />
    }
  }

  function getLabel(type: string) {
    switch (type) {
      case "lesson_completed": return "Lesson Completed"
      case "attendance": return "Attendance Recorded"
      default: return "Activity"
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Notifications</h1>
        <p className="mt-1 text-sm text-muted-foreground">Stay updated on your learning activity.</p>
      </div>

      {activities.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Bell className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No notifications yet</p>
          <p className="mt-1 max-w-sm text-sm text-muted-foreground">
            You&apos;ll see updates about lessons, attendance, and assignments here.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {activities.map((activity, i) => (
            <div key={i} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-muted">
                {getIcon(activity.type)}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-foreground">{getLabel(activity.type)}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {activity.completedAt
                    ? new Date(activity.completedAt).toLocaleDateString()
                    : activity.date
                      ? new Date(activity.date).toLocaleDateString()
                      : ""}
                </p>
              </div>
              {activity.status && (
                <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                  activity.status === "PRESENT" ? "bg-green-100 text-green-700" :
                  activity.status === "ABSENT" ? "bg-red-100 text-red-700" :
                  "bg-yellow-100 text-yellow-700"
                }`}>
                  {activity.status}
                </span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
