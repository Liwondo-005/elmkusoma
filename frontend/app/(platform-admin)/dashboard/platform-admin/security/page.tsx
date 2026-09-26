"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { ShieldAlert, Loader2, CheckCircle2, Globe, Clock } from "lucide-react"
import { platformAdminApi, type SecurityEventItem } from "@/lib/platform-admin-api"

export default function SecurityCenterPage() {
  const t = useTranslations("platformAdmin");
  const [events, setEvents] = useState<SecurityEventItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [resolving, setResolving] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await platformAdminApi.getSecurityEvents()
      setEvents(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("security.failedToLoadSecurity"))
    } finally {
      setLoading(false)
    }
  }

  const handleResolve = async (eventId: string) => {
    try {
      setResolving(eventId)
      await platformAdminApi.resolveSecurityEvent(eventId)
      setEvents((prev) => prev.filter((e) => e.id !== eventId))
    } catch (err) {
      setError(err instanceof Error ? err.message : t("security.failedToResolveEvent"))
    } finally {
      setResolving(null)
    }
  }

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case "HIGH":
        return <span className="inline-block rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-600">HIGH</span>
      case "MEDIUM":
        return <span className="inline-block rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600">MEDIUM</span>
      default:
        return <span className="inline-block rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-medium text-blue-600">INFO</span>
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleString("en-GB", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })

  const unresolved = events.filter((e) => !e.resolved)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("security.securityCenter")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("security.monitorAndResolveSecurity")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : unresolved.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <CheckCircle2 className="size-10 text-green-500/60" />
          <p className="mt-4 text-sm font-medium text-foreground">{t("security.noUnresolvedSecurityEvents")}</p>
          <p className="mt-1 text-sm text-muted-foreground">{t("security.allSecurityEventsHave")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {unresolved.map((event) => (
            <div
              key={event.id}
              className="rounded-2xl border border-border bg-card p-5 shadow-xs"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    {getSeverityBadge(event.severity)}
                    <span className="text-sm font-semibold text-foreground">{event.eventType}</span>
                  </div>
                  <p className="mt-1.5 text-sm text-muted-foreground">{event.description}</p>
                  <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1">
                      <Globe className="size-3" />{event.ipAddress}
                    </span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="size-3" />{formatDate(event.createdAt)}
                    </span>
                  </div>
                </div>
                <button
                  onClick={() => handleResolve(event.id)}
                  disabled={resolving === event.id}
                  className="shrink-0 inline-flex items-center gap-1.5 rounded-lg border border-green-200 bg-green-50 px-3 py-1.5 text-xs font-medium text-green-700 hover:bg-green-100 disabled:opacity-50 dark:border-green-800 dark:bg-green-900/20 dark:text-green-400"
                >
                  {resolving === event.id ? (
                    <Loader2 className="size-3.5 animate-spin" />
                  ) : (
                    <CheckCircle2 className="size-3.5" />
                  )}
                  {t("security.resolve")}</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
