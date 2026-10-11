"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { dashboardApi, type LiveClass as ApiLiveClass } from "@/lib/api"
import { LiveClassCard } from "@/components/live-class-card"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { LiveClass } from "@/lib/data"
import { useAuth } from "@/lib/auth"

function mapApiToCard(
  lc: ApiLiveClass & { teacherName?: string | null; subjectName?: string | null; hasRecording?: boolean | null },
  sx: { recorded: string; liveNow: string; ended: string; intermediate: string },
): LiveClass {
  // Status comes from the API. It was previously recomputed in the browser from
  // scheduledAt+duration, which mislabelled CANCELLED and in-progress sessions.
  const status = (lc.status ?? "").toUpperCase()
  const scheduled = lc.scheduledAt ? new Date(lc.scheduledAt) : null
  const isLive = ["IN_PROGRESS", "LIVE", "STARTING", "ENDING"].includes(status)
  const isPast = ["COMPLETED", "ENDED", "CANCELLED", "SERVICE_UNAVAILABLE"].includes(status)

  let bucket: LiveClass["status"]
  let badge: string
  if (isLive) { bucket = "live"; badge = sx.liveNow }
  else if (isPast) { bucket = "past"; badge = status === "CANCELLED" ? "CANCELLED" : sx.recorded }
  else { bucket = "scheduled"; badge = scheduled ? scheduled.toLocaleDateString() : "" }

  return {
    id: lc.id,
    title: lc.title,
    subtitle: lc.subjectName ?? "",
    instructor: lc.teacherName ?? "",
    image: "/images/class-default.png",
    status: bucket,
    badge,
    time: isPast ? sx.ended : scheduled ? scheduled.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "",
    level: "Intermediate",
    hasRecording: lc.hasRecording === true,
    canJoin: isLive,
  }
}

export function LivePreviewSection() {
  const t = useTranslations("home")
  const tn = useTranslations("nav")
  const [classes, setClasses] = useState<LiveClass[]>([])
  const { token, loading: authLoading } = useAuth()

  useEffect(() => {
    if (authLoading || !token) return
    const sx = {
      recorded: t("live.statusRecorded"),
      liveNow: t("live.statusLiveNow"),
      ended: t("live.statusEnded"),
      intermediate: t("live.levelIntermediate"),
    }
    // Same real endpoint as the /live-classes discovery page: it returns running,
    // upcoming and recently finished sessions.
    dashboardApi.getStudentLiveClasses()
      .then((data) => setClasses(((data as (ApiLiveClass & { teacherName?: string | null; hasRecording?: boolean | null })[]) || []).slice(0, 4).map((lc) => mapApiToCard(lc, sx))))
      .catch(() => setClasses([]))
  }, [t, token, authLoading])

  if (classes.length === 0) return null

  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-md bg-teal/10 px-2.5 py-1 text-xs font-semibold text-teal">
                <span className="size-1.5 animate-pulse rounded-full bg-teal" />
                {t("live.badge")}
              </span>
              <span className="text-sm text-muted-foreground">{t("live.tagline")}</span>
            </div>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-foreground">{tn("liveClasses")}</h2>
            <p className="mt-2 max-w-xl text-muted-foreground">
              {t("live.subtitle")}
            </p>
          </div>
          <Link
            href="/live-classes"
            className={cn(buttonVariants({ variant: "outline" }), "h-10 gap-2 px-4")}
          >
            {t("live.viewAll")}
          </Link>
        </div>

        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {classes.map((item) => (
            <LiveClassCard key={item.id} item={item} />
          ))}
        </div>
      </div>
    </section>
  )
}
