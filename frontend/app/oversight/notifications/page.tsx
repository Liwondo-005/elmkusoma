"use client"

import { useTranslations } from "next-intl"

import { useCallback, useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import {
  Bell,
  CheckCheck,
  Loader2,
  AlertCircle,
  Megaphone,
  BookOpen,
  Video,
  GraduationCap,
  FileText,
  Award,
} from "lucide-react"
import { notificationsApi, type NotificationItem } from "@/lib/api"
import { cn } from "@/lib/utils"

function notificationIcon(type: string) {
  switch ((type || "").toUpperCase()) {
    case "ANNOUNCEMENT":
    case "COURSE_ANNOUNCEMENT":
      return <Megaphone className="size-4 text-purple-500" />
    case "ENROLLMENT":
      return <BookOpen className="size-4 text-blue-500" />
    case "COURSE_COMPLETION":
    case "CERTIFICATE":
      return <GraduationCap className="size-4 text-green-500" />
    case "VIDEO":
      return <Video className="size-4 text-red-500" />
    case "ASSIGNMENT_GRADED":
      return <Award className="size-4 text-yellow-500" />
    default:
      return <Bell className="size-4 text-primary" />
  }
}

export default function OversightNotificationsPage() {
  const t = useTranslations("oversight")
  const { user, loading: authLoading } = useRequireAuth()
  const [items, setItems] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<"all" | "unread">("all")
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setError(null)
    try {
      setItems(await notificationsApi.list())
    } catch {
      setError(t("notifications.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    if (authLoading || !user) return
    load()
  }, [authLoading, user, load])

  async function markRead(id: string) {
    setBusy(true)
    try {
      await notificationsApi.markRead(id)
      setItems((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)))
    } catch {
      setError(t("notifications.loadFailed"))
    } finally {
      setBusy(false)
    }
  }

  async function markAllRead() {
    setBusy(true)
    try {
      await notificationsApi.markAllRead()
      setItems((prev) => prev.map((n) => ({ ...n, isRead: true })))
    } catch {
      setError(t("notifications.loadFailed"))
    } finally {
      setBusy(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("notifications.loading")}</div>
      </div>
    )
  }

  const unread = items.filter((n) => !n.isRead)
  const visible = filter === "unread" ? unread : items

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Bell className="size-5 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{t("notifications.title")}</h1>
              <p className="text-sm text-muted-foreground">
                {t("notifications.subtitle")} — {unread.length} {t("notifications.unread")}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex rounded-lg border border-border p-0.5">
              <button
                type="button"
                onClick={() => setFilter("all")}
                className={cn(
                  "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                  filter === "all" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {t("notifications.all")}
              </button>
              <button
                type="button"
                onClick={() => setFilter("unread")}
                className={cn(
                  "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                  filter === "unread" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {t("notifications.unread")}
              </button>
            </div>
            <button
              type="button"
              onClick={markAllRead}
              disabled={busy || unread.length === 0}
              className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
            >
              {busy ? <Loader2 className="size-4 animate-spin" /> : <CheckCheck className="size-4" />}
              {t("notifications.markAllRead")}
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      {visible.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border p-10 text-center text-muted-foreground">
          {filter === "unread" ? t("notifications.emptyUnread") : t("notifications.empty")}
        </div>
      ) : (
        <div className="space-y-3">
          {visible.map((notification) => (
            <div
              key={notification.id}
              className={cn(
                "flex items-start gap-3 rounded-xl border border-border p-4 transition-colors",
                !notification.isRead && "border-l-4 border-l-primary bg-primary/5",
              )}
            >
              <div className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted">
                {notificationIcon(notification.notificationType)}
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium text-foreground">{notification.title}</p>
                <p className="mt-0.5 break-words text-sm text-muted-foreground">{notification.message}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ""}
                </p>
              </div>
              {!notification.isRead && (
                <button
                  type="button"
                  onClick={() => markRead(notification.id)}
                  disabled={busy}
                  className="shrink-0 rounded-lg border border-border px-2.5 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
                >
                  {t("notifications.markRead")}
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
