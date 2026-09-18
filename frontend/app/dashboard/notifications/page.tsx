"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, primaryApi, type RecentActivity, type StudentNotification } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Bell, CheckCircle, Clock, AlertCircle, Info, BookOpen, Award, Users, X } from "lucide-react"

const notificationTypeConfig: Record<string, { icon: typeof Bell; color: string; bgColor: string }> = {
  LESSON_COMPLETED: { icon: CheckCircle, color: "text-green-600", bgColor: "bg-green-50" },
  ASSIGNMENT_DUE: { icon: Clock, color: "text-amber-600", bgColor: "bg-amber-50" },
  BADGE_AWARDED: { icon: Award, color: "text-purple-600", bgColor: "bg-purple-50" },
  TEACHER_MESSAGE: { icon: Users, color: "text-blue-600", bgColor: "bg-blue-50" },
  ATTENDANCE: { icon: BookOpen, color: "text-teal-600", bgColor: "bg-teal-50" },
  GENERAL: { icon: Info, color: "text-muted-foreground", bgColor: "bg-muted" },
}

export default function NotificationsPage() {
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [primaryNotifications, setPrimaryNotifications] = useState<StudentNotification[]>([])
  const [loading, setLoading] = useState(true)
  const [dismissedIds, setDismissedIds] = useState<Set<string>>(new Set())
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      if (isPrimary) {
        const notifData = await primaryApi.getNotifications().catch(() => [])
        setPrimaryNotifications(notifData)
      } else {
        const data = await dashboardApi.getRecentActivity().catch(() => [])
        setActivities(data)
      }
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  function dismissNotification(id: string) {
    setDismissedIds((prev) => new Set(prev).add(id))
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (isPrimary) {
    const visibleNotifications = primaryNotifications.filter((n) => !dismissedIds.has(n.id))

    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/10">
              <Bell className="size-5 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">Notifications</h1>
              <p className="text-sm text-muted-foreground">Stay updated on your learning.</p>
            </div>
          </div>
        </div>

        {visibleNotifications.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
            <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
              <Bell className="size-8 text-primary" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-foreground">No new notifications</h3>
            <p className="mt-2 max-w-sm text-sm text-muted-foreground">
              You are all caught up. Notifications about lessons, badges, and messages will appear here.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {visibleNotifications.map((notif) => {
              const config = notificationTypeConfig[notif.notificationType] || notificationTypeConfig.GENERAL
              const Icon = config.icon
              return (
                <div
                  key={notif.id}
                  className={`relative flex items-start gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all ${
                    !notif.isRead ? "border-l-4 border-l-primary" : ""
                  }`}
                >
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${config.bgColor}`}>
                    <Icon className={`size-5 ${config.color}`} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-foreground">{notif.title}</p>
                    <p className="mt-1 text-sm text-muted-foreground">{notif.message}</p>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {new Date(notif.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => dismissNotification(notif.id)}
                    className="shrink-0 rounded-lg p-1 text-muted-foreground hover:bg-muted"
                  >
                    <X className="size-4" />
                  </button>
                </div>
              )
            })}
          </div>
        )}
      </div>
    )
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
