"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learnerApi, type LearnerNotification } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { MessageSquare, Send, Bell, CheckCircle2, AlertCircle, Clock } from "lucide-react"

export default function DashboardMessagesPage() {
  const { user, loading: authLoading } = useAuth()
  const [messages, setMessages] = useState<LearnerNotification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadMessages()
  }, [user])

  async function loadMessages() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getNotifications()
      setMessages(data.filter((n) => n.notificationType === "MESSAGE" || n.notificationType === "ANNOUNCEMENT"))
    } catch {
      setError("Failed to load messages")
    } finally {
      setLoading(false)
    }
  }

  async function markAsRead(id: string) {
    try {
      await learnerApi.markNotificationRead(id)
      setMessages((prev) =>
        prev.map((m) => (m.id === id ? { ...m, isRead: true } : m))
      )
    } catch {
      // silent fail
    }
  }

  if (authLoading || loading) return <LoadingState />

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const unreadCount = messages.filter((m) => !m.isRead).length

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Messages</h1>
          <p className="mt-1 text-sm text-muted-foreground">Communicate with instructors and support.</p>
        </div>
        {unreadCount > 0 && (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
            <Bell className="size-3" />
            {unreadCount} unread
          </span>
        )}
      </div>

      {error && (
        <div className="rounded-2xl border border-border bg-card p-4 text-sm text-red-600 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadMessages() }} className="ml-auto text-xs underline">Retry</button>
        </div>
      )}

      {messages.length === 0 ? (
        <EmptyState
          icon={<MessageSquare className="size-8" />}
          title="No messages yet"
          description="Messages from teachers, instructors, and support will appear here."
        />
      ) : (
        <div className="space-y-3">
          {messages.map((msg) => (
            <div
              key={msg.id}
              onClick={() => !msg.isRead && markAsRead(msg.id)}
              className={`rounded-2xl border bg-card p-5 shadow-xs transition hover:shadow-md cursor-pointer ${
                msg.isRead ? "border-border" : "border-primary/30 bg-primary/5"
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className={`text-sm font-semibold ${msg.isRead ? "text-muted-foreground" : "text-foreground"}`}>
                      {msg.title}
                    </h3>
                    {!msg.isRead && (
                      <span className="size-2 shrink-0 rounded-full bg-primary" />
                    )}
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{msg.message}</p>
                  <div className="mt-2 flex items-center gap-3 text-[10px] text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Clock className="size-3" />
                      {msg.createdAt ? new Date(msg.createdAt).toLocaleDateString() : "—"}
                    </span>
                    {msg.notificationType && (
                      <span className="rounded-full bg-muted px-2 py-0.5 font-medium">
                        {msg.notificationType}
                      </span>
                    )}
                  </div>
                </div>
                {msg.isRead && (
                  <CheckCircle2 className="size-4 shrink-0 text-emerald-500" />
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
