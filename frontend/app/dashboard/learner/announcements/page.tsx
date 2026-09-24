"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type Announcement } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Megaphone, AlertCircle, ArrowUpCircle, ArrowRightCircle, MinusCircle } from "lucide-react"

export default function AnnouncementsPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadAnnouncements()
  }, [user])

  async function loadAnnouncements() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getAnnouncements()
      setAnnouncements(data)
    } catch {
      setError(t("announce.loadError"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  function getPriorityIcon(priority: string) {
    switch (priority?.toUpperCase()) {
      case "URGENT": return <ArrowUpCircle className="size-4 text-red-500" />
      case "HIGH": return <ArrowUpCircle className="size-4 text-orange" />
      case "LOW": return <ArrowRightCircle className="size-4 text-blue-500" />
      default: return <MinusCircle className="size-4 text-muted-foreground" />
    }
  }

  function getPriorityBadge(priority: string) {
    const styles: Record<string, string> = {
      URGENT: "bg-red-500/10 text-red-500",
      HIGH: "bg-orange/10 text-orange",
      NORMAL: "bg-muted text-muted-foreground",
      LOW: "bg-blue-500/10 text-blue-500",
    }
    return (
      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${styles[priority] || styles.NORMAL}`}>
        {priority}
      </span>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("announce.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("announce.subtitle")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {announcements.length > 0 ? (
        <div className="space-y-3">
          {announcements.map((ann) => (
            <div key={ann.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start gap-3">
                <div className="mt-0.5">
                  <Megaphone className="size-5 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-semibold text-foreground">{ann.title}</h3>
                    {getPriorityIcon(ann.priority)}
                    {getPriorityBadge(ann.priority)}
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground leading-relaxed">{ann.content}</p>
                  <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                    {ann.authorName && <span>{t("announce.byAuthor", { name: ann.authorName })}</span>}
                    <span>{new Date(ann.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={<Megaphone className="size-8" />}
          title={t("announce.emptyTitle")}
          description={t("announce.emptyDesc")}
        />
      )}
    </div>
  )
}
