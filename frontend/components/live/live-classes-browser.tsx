"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { AlertTriangle, RefreshCw } from "lucide-react"
import { dashboardApi } from "@/lib/api"
import { LiveClassCard } from "@/components/live-class-card"
import { useAuth } from "@/lib/auth"
import { cn } from "@/lib/utils"
import type { LiveClass, LiveStatus } from "@/lib/data"

const tabs = [
  { id: "live", labelKey: "liveBrowser.liveNow" },
  { id: "upcoming", labelKey: "liveBrowser.upcoming" },
  { id: "past", labelKey: "liveBrowser.past" },
] as const

type TabId = (typeof tabs)[number]["id"]

/** Shape returned by GET /v1/student/live-classes (includes running + recent past). */
interface DiscoveryLiveClass {
  id: string
  title: string
  description?: string | null
  status?: string | null
  scheduledAt?: string | null
  durationMinutes?: number | null
  sessionType?: string | null
  subjectName?: string | null
  teacherName?: string | null
  hasRecording?: boolean | null
}

/** Backend session status -> discovery bucket. */
const LIVE_STATUSES = new Set(["IN_PROGRESS", "LIVE", "STARTING", "ENDING"])
const ENDED_STATUSES = new Set(["COMPLETED", "ENDED", "CANCELLED", "SERVICE_UNAVAILABLE"])

export function mapToDiscoveryCard(
  lc: DiscoveryLiveClass,
  strings: {
    badgeLive: string
    badgeEnded: string
    badgeCancelled: string
    badgeStarting: string
    endedLabel: string
    timeFormat: { hour: "2-digit"; minute: "2-digit" }
    dateFormat: Intl.DateTimeFormatOptions
  },
): LiveClass {
  const status = (lc.status ?? "").toUpperCase()
  const scheduled = lc.scheduledAt ? new Date(lc.scheduledAt) : null
  const isLive = LIVE_STATUSES.has(status)
  const isEnded = ENDED_STATUSES.has(status)

  let bucket: LiveStatus = "scheduled"
  let badge = ""
  if (isLive) {
    bucket = "live"
    badge = status === "STARTING" ? strings.badgeStarting : strings.badgeLive
  } else if (isEnded) {
    bucket = "past"
    badge = status === "CANCELLED" ? strings.badgeCancelled : strings.badgeEnded
  } else if (scheduled) {
    const hours = Math.round((scheduled.getTime() - Date.now()) / 3_600_000)
    bucket = hours <= 24 ? "soon" : "scheduled"
    badge = scheduled.toLocaleDateString(undefined, strings.dateFormat)
  }

  const time = isEnded
    ? strings.endedLabel
    : scheduled
      ? scheduled.toLocaleTimeString(undefined, strings.timeFormat)
      : ""

  return {
    id: lc.id,
    title: lc.title,
    subtitle: lc.subjectName ?? "",
    // Real API value. Previously hardcoded to "" on every card.
    instructor: lc.teacherName ?? "",
    image: "/images/class-default.png",
    status: bucket,
    badge,
    time,
    level: "Intermediate",
    hasRecording: lc.hasRecording === true,
    canJoin: isLive,
  }
}

export function LiveClassesBrowser() {
  const t = useTranslations("ui")
  const tp = useTranslations("public")
  const [active, setActive] = useState<TabId>("live")
  const [classes, setClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const { user, token, loading: authLoading } = useAuth()

  const strings = useMemo(
    () => ({
      badgeLive: t("liveBrowser.badgeLiveNow"),
      badgeEnded: t("liveBrowser.badgeRecorded"),
      badgeCancelled: t("liveBrowser.badgeCancelled"),
      badgeStarting: t("liveBrowser.badgeStarting"),
      endedLabel: t("liveBrowser.endedLabel"),
      timeFormat: { hour: "2-digit", minute: "2-digit" } as const,
      dateFormat: { month: "short", day: "numeric" } as const,
    }),
    [t],
  )

  const load = useCallback(async () => {
    if (!token) {
      setLoading(false)
      setError(null)
      return
    }
    setLoading(true)
    setError(null)
    try {
    // Map: server-derived (status, hasRecording). Scheduled classes use the server clock via
    // the API status, never a client-side countdown.
    // Find the live-now endpoint that INCLUDES running + past: /v1/student/dashboard/live-classes
    // queries "now -> +7d" and can never return a running or ended class.
      const data = (await dashboardApi.getStudentLiveClasses()) as DiscoveryLiveClass[]
      setClasses((data || []).map((lc) => mapToDiscoveryCard(lc, strings)))
    } catch (err) {
      setClasses([])
      setError(err instanceof Error ? err.message : t("liveBrowser.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [token, strings, t])

  useEffect(() => {
    if (authLoading) return
    void load()
  }, [authLoading, load])

  const counts = useMemo(
    () => ({
      live: classes.filter((c) => c.status === "live").length,
      past: classes.filter((c) => c.status === "past").length,
      upcoming: classes.filter((c) => c.status !== "live" && c.status !== "past").length,
    }),
    [classes],
  )

  const filtered = useMemo(() => {
    if (active === "live") return classes.filter((c) => c.status === "live")
    if (active === "past") return classes.filter((c) => c.status === "past")
    return classes.filter((c) => c.status !== "live" && c.status !== "past")
  }, [classes, active])

  // Live classes are institution-scoped, never public: an anonymous visitor is told to sign
  // in instead of being shown an empty list that looks like "nothing is scheduled".
  if (!authLoading && !user) {
    return (
      <section className="py-10">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-border bg-muted/40 px-6 py-16 text-center">
            <p className="text-sm font-medium text-foreground">{tp("liveClassesIndex.badge")}</p>
            <p className="max-w-md text-sm text-muted-foreground">{t("liveBrowser.signInRequired")}</p>
            <Link
              href="/login?redirect=/live-classes"
              className="mt-2 inline-flex h-10 items-center rounded-lg bg-primary px-5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              {t("liveBrowser.signInAction")}
            </Link>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="py-10">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-1 border-b border-border" role="tablist" aria-label={tp("liveClassesIndex.badge")}>
          {tabs.map((tab) => {
            const count = counts[tab.id]
            const selected = active === tab.id
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                id={`live-discovery-tab-${tab.id}`}
                aria-selected={selected}
                aria-controls={`live-discovery-panel-${tab.id}`}
                tabIndex={selected ? 0 : -1}
                onClick={() => setActive(tab.id)}
                onKeyDown={e => {
                  const order = tabs.map((x) => x.id)
                  const idx = order.indexOf(tab.id)
                  if (e.key === "ArrowRight" || e.key === "ArrowLeft") {
                    e.preventDefault()
                    const next = order[(idx + (e.key === "ArrowRight" ? 1 : order.length - 1)) % order.length]
                    setActive(next)
                    document.getElementById(`live-discovery-tab-${next}`)?.focus()
                  }
                }}
                className={cn(
                  "relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors",
                  selected ? "text-primary" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {t(tab.labelKey)}
                {count > 0 && (
                  <span className="inline-flex size-5 items-center justify-center rounded-full bg-orange text-[10px] font-bold text-orange-foreground">
                    {count}
                  </span>
                )}
                {selected && (
                  <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-primary" />
                )}
              </button>
            )
          })}
        </div>

        {loading ? (
          <div className="mt-8 flex justify-center py-20" role="status" aria-live="polite">
            <span className="sr-only">{t("liveBrowser.loading")}</span>
            <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent motion-reduce:animate-none" />
          </div>
        ) : error ? (
          <div role="alert" className="mt-8 flex flex-col items-center justify-center gap-3 rounded-2xl border border-red-200 bg-red-50 px-6 py-16 text-center">
            <AlertTriangle className="size-6 text-red-600" aria-hidden />
            <p className="text-sm font-medium text-red-800">{t("liveBrowser.loadFailed")}</p>
            <p className="max-w-md text-xs text-red-700">{error}</p>
            <button
              type="button"
              onClick={() => void load()}
              className="mt-1 inline-flex h-9 items-center gap-2 rounded-lg border border-red-300 bg-white px-4 text-sm font-medium text-red-700 transition-colors hover:bg-red-100"
            >
              <RefreshCw className="size-4" aria-hidden />
              {t("liveBrowser.retry")}
            </button>
          </div>
        ) : filtered.length > 0 ? (
          <div
            id={`live-discovery-panel-${active}`}
            role="tabpanel"
            aria-labelledby={`live-discovery-tab-${active}`}
            className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
          >
            {filtered.map((item) => (
              <LiveClassCard key={item.id} item={item} />
            ))}
          </div>
        ) : (
          <div
            id={`live-discovery-panel-${active}`}
            role="tabpanel"
            aria-labelledby={`live-discovery-tab-${active}`}
            className="mt-8 flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/40 px-6 py-20 text-center"
          >
            <p className="text-sm font-medium text-foreground">
              {active === "past" ? t("liveBrowser.emptyPastTitle") : t("liveBrowser.emptyUpcomingTitle")}
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              {active === "past" ? t("liveBrowser.emptyPastDesc") : t("liveBrowser.emptyUpcomingDesc")}
            </p>
          </div>
        )}
      </div>
    </section>
  )
}
