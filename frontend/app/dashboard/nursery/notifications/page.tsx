"use client"

import { useEffect, useState, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryNotification } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Bell, BookOpen, Award, Calendar, CheckCircle } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

const TYPE_CONFIG: Record<string, { icon: typeof Bell; color: string }> = {
  INFO: { icon: Bell, color: "bg-blue-100 text-blue-600" },
  REMINDER: { icon: Calendar, color: "bg-orange-100 text-orange-600" },
  ACHIEVEMENT: { icon: Award, color: "bg-green-100 text-green-600" },
  CLASS: { icon: Calendar, color: "bg-red-100 text-red-600" },
  ASSIGNMENT: { icon: BookOpen, color: "bg-purple-100 text-purple-600" },
  default: { icon: Bell, color: "bg-gray-100 text-gray-600" },
}

export default function NurseryNotificationsPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [notifications, setNotifications] = useState<NurseryNotification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadNotifications = useCallback(() => {
    if (!user) return
    setLoading(true)
    setError(null)
    nurseryApi.getNotifications(user.id)
      .then(setNotifications)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => { loadNotifications() }, [loadNotifications])

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-800">{t("notifications")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadNotifications} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const unread = notifications.filter(n => !n.isRead)
  const read = notifications.filter(n => n.isRead)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("notifications")}</h1>
          <p className="text-sm text-gray-500">
            {unread.length > 0 ? `${unread.length} new` : tc("completed")}
          </p>
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Bell className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noNotifications")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.teacherWillAdd")}</p>
        </div>
      ) : (
        <>
          {unread.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-bold text-gray-500">{t("filters.new")}</h2>
              <div className="space-y-2">
                {unread.map(n => {
                  const config = TYPE_CONFIG[n.notificationType] || TYPE_CONFIG.default
                  const Icon = config.icon
                  return (
                    <div key={n.id} className="nursery-card flex items-start gap-3 rounded-2xl border-l-4 border-primary bg-white p-4">
                      <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${config.color}`}>
                        <Icon className="size-5" />
                      </div>
                      <div className="flex-1">
                        <p className="font-bold text-gray-800">{n.title}</p>
                        <p className="mt-0.5 text-xs text-gray-500">{n.message}</p>
                        <p className="mt-1 text-[10px] text-gray-400">
                          {new Date(n.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                  )
                })}
              </div>
            </section>
          )}
          {read.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-bold text-gray-500">{t("filters.earlier")}</h2>
              <div className="space-y-2">
                {read.map(n => {
                  const config = TYPE_CONFIG[n.notificationType] || TYPE_CONFIG.default
                  const Icon = config.icon
                  return (
                    <div key={n.id} className="nursery-card flex items-start gap-3 rounded-2xl bg-white p-4 opacity-70">
                      <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${config.color}`}>
                        <Icon className="size-5" />
                      </div>
                      <div className="flex-1">
                        <p className="font-bold text-gray-600">{n.title}</p>
                        <p className="mt-0.5 text-xs text-gray-400">{n.message}</p>
                      </div>
                    </div>
                  )
                })}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
