"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Bell, BookOpen, Award, Calendar, CheckCircle } from "lucide-react"
import Link from "next/link"

interface Notification {
  id: string
  title: string
  message: string
  type: string
  isRead: boolean
  createdAt: string
}

const TYPE_CONFIG: Record<string, { icon: typeof Bell; color: string }> = {
  course: { icon: BookOpen, color: "bg-blue-100 text-blue-600" },
  assignment: { icon: BookOpen, color: "bg-orange-100 text-orange-600" },
  live_class: { icon: Calendar, color: "bg-red-100 text-red-600" },
  certificate: { icon: Award, color: "bg-green-100 text-green-600" },
  milestone: { icon: CheckCircle, color: "bg-purple-100 text-purple-600" },
  default: { icon: Bell, color: "bg-gray-100 text-gray-600" },
}

export default function NurseryNotificationsPage() {
  const { user } = useRequireAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/v1/notifications`, {
      headers: {
        "Authorization": `Bearer ${localStorage.getItem("elmkusoma_access_token")}`,
        "X-Institution-Id": localStorage.getItem("elmkusoma_institution_id") || "",
      },
    })
      .then(r => r.json())
      .then(d => setNotifications(d.data || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const unread = notifications.filter(n => !n.isRead)
  const read = notifications.filter(n => n.isRead)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Notifications</h1>
          <p className="text-sm text-gray-500">
            {unread.length > 0 ? `You have ${unread.length} new messages` : "All caught up!"}
          </p>
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Bell className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No notifications yet!</h3>
          <p className="mt-1 text-sm text-gray-500">Messages from your teacher will appear here.</p>
        </div>
      ) : (
        <>
          {unread.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-bold text-gray-500">NEW</h2>
              <div className="space-y-2">
                {unread.map(n => {
                  const config = TYPE_CONFIG[n.type] || TYPE_CONFIG.default
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
              <h2 className="mb-3 text-sm font-bold text-gray-500">EARLIER</h2>
              <div className="space-y-2">
                {read.map(n => {
                  const config = TYPE_CONFIG[n.type] || TYPE_CONFIG.default
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
