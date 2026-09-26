"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type StudentNotification } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Bell, BellOff, CheckCircle, AlertCircle, Info, Clock, Loader2 } from "lucide-react"

export default function NotificationsPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [notifications, setNotifications] = useState<StudentNotification[]>([])
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null

  useEffect(() => {
    if (!user) return
    loadNotifications()
  }, [user])

  async function loadNotifications() {
    try {
      setLoading(true)
      const data = await primaryApi.getNotifications()
      setNotifications(Array.isArray(data) ? data : [])
    } catch {
      setNotifications([])
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
          <Bell className="size-6 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("notifs.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("notifs.subtitle")}</p>
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border border-dashed border-border py-16 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-muted/50">
            <BellOff className="size-8 text-muted-foreground/50" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("notifs.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("notifs.emptyDesc")}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((notification) => (
            <div
              key={notification.id}
              className="rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md"
            >
              <div className="flex items-start gap-3">
                <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${
                  notification.notificationType === "ASSIGNMENT" ? "bg-blue-50" :
                  notification.notificationType === "LIVE_CLASS" ? "bg-green-50" :
                  notification.notificationType === "BADGE" ? "bg-purple-50" :
                  "bg-muted"
                }`}>
                  {notification.notificationType === "ASSIGNMENT" ? <CheckCircle className="size-5 text-blue-600" /> :
                   notification.notificationType === "LIVE_CLASS" ? <Clock className="size-5 text-green-600" /> :
                   notification.notificationType === "BADGE" ? <CheckCircle className="size-5 text-purple-600" /> :
                   <Info className="size-5 text-muted-foreground" />}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground">{notification.title}</p>
                  {notification.message && (
                    <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{notification.message}</p>
                  )}
                  <p className="mt-1 text-xs text-muted-foreground">
                    {new Date(notification.createdAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
