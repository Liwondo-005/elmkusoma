"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Bell, BookOpen, Award, Calendar } from "lucide-react"
import Link from "next/link"

interface Notification {
  id: string
  title: string
  message: string
  type: string
  isRead: boolean
  createdAt: string
}

export default function SecondaryNotificationsPage() {
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
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Notifications</h1>
          <p className="text-sm text-gray-500">
            {unread.length > 0 ? `${unread.length} new notifications` : "All caught up"}
          </p>
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Bell className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No notifications</h3>
          <p className="mt-1 text-sm text-gray-500">Updates from your teachers will appear here.</p>
        </div>
      ) : (
        <>
          {unread.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">New</h2>
              <div className="space-y-2">
                {unread.map(n => (
                  <div key={n.id} className="flex items-start gap-3 rounded-2xl border-l-4 border-indigo-500 bg-white p-4">
                    <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-indigo-50">
                      <Bell className="size-4 text-indigo-500" />
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-semibold text-gray-900">{n.title}</p>
                      <p className="mt-0.5 text-xs text-gray-500">{n.message}</p>
                      <p className="mt-1 text-[10px] text-gray-400">{new Date(n.createdAt).toLocaleString()}</p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
          {read.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Earlier</h2>
              <div className="space-y-2">
                {read.map(n => (
                  <div key={n.id} className="flex items-start gap-3 rounded-2xl bg-white p-4 opacity-60">
                    <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-gray-100">
                      <Bell className="size-4 text-gray-400" />
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-600">{n.title}</p>
                      <p className="mt-0.5 text-xs text-gray-400">{n.message}</p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
