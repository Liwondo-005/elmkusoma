"use client"

import { useCallback, useState } from "react"
import { Bell, CheckCheck } from "lucide-react"
import { useTranslations } from "next-intl"
import {
  regionalAdminApi,
  type NotificationSummary,
} from "@/lib/regional-admin-api"
import {
  Chip,
  PageHeader,
  PagedList,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

function NotificationRow({
  item, onMarked,
}: {
  item: NotificationSummary
  onMarked: () => void
}) {
  const t = useTranslations("regionalAdmin")
  const tn = useTranslations("notifications")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function markRead() {
    setBusy(true)
    setError(null)
    try {
      await regionalAdminApi.markNotificationRead(item.id)
      onMarked()
    } catch (e) {
      setError(e instanceof Error ? e.message : t("notificationsList.markReadError"))
    } finally {
      setBusy(false)
    }
  }

  return (
    <article className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex gap-3">
        <span
          aria-hidden="true"
          className={`mt-2 size-2.5 shrink-0 rounded-full ${
            item.isRead ? "bg-muted" : "bg-blue-500"
          }`}
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-sm font-bold text-foreground">{item.title}</h3>
            <Chip tone="info">{item.notificationType}</Chip>
            {!item.isRead && <Chip tone="default">{t("notificationsList.unreadBadge")}</Chip>}
          </div>
          <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{item.message}</p>
          <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
            <span>{formatDateTime(item.createdAt)}</span>
            <span>{item.targetType}</span>
            <button
              type="button"
              onClick={markRead}
              disabled={item.isRead || busy}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-3 py-1 text-xs font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
            >
              <CheckCheck className="size-3.5" />
              {busy ? t("notificationsList.saving") : tn("markAsRead")}
            </button>
          </div>
          {error && <p className="mt-2 text-xs text-destructive">{error}</p>}
        </div>
      </div>
    </article>
  )
}

export default function NotificationsPage() {
  const t = useTranslations("regionalAdmin")
  const [reloadKey, setReloadKey] = useState(0)

  // reloadKey is part of the fetcher identity so the list refetches after a read.
  const fetcher = useCallback(
    (p: { page: number; size: number; search?: string }) =>
      regionalAdminApi.listNotifications(p),
    [reloadKey],
  )

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={t("notificationsList.title")}
        description={t("notificationsList.description")}
      />

      <PagedList<NotificationSummary>
        fetcher={fetcher}
        searchPlaceholder={t("notificationsList.searchPlaceholder")}
        emptyTitle={t("notificationsList.emptyTitle")}
        emptyHint={t("notificationsList.emptyHint")}
        emptyIcon={<Bell className="size-10" />}
        renderItem={(item) => (
          <NotificationRow item={item} onMarked={() => setReloadKey((k) => k + 1)} />
        )}
      />
    </div>
  )
}
