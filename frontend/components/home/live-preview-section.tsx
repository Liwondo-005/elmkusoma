"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { dashboardApi, type LiveClass as ApiLiveClass } from "@/lib/api"
import { LiveClassCard } from "@/components/live-class-card"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { LiveClass } from "@/lib/data"

function mapApiToCard(
  lc: ApiLiveClass,
  sx: { recorded: string; liveNow: string; ended: string; intermediate: string },
): LiveClass {
  const now = new Date()
  const scheduled = new Date(lc.scheduledAt)
  const end = new Date(scheduled.getTime() + lc.durationMinutes * 60000)
  const isPast = end < now
  const isLive = scheduled <= now && end >= now

  let status: LiveClass["status"]
  let badge: string
  if (isPast) { status = "past"; badge = sx.recorded }
  else if (isLive) { status = "live"; badge = sx.liveNow }
  else { status = "scheduled"; badge = scheduled.toLocaleDateString() }

  return {
    id: lc.id,
    title: lc.title,
    subtitle: lc.description?.slice(0, 40) || "",
    instructor: "",
    image: "/images/class-default.png",
    status,
    badge,
    time: isPast ? sx.ended : scheduled.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    level: "Intermediate",
  }
}

export function LivePreviewSection() {
  const t = useTranslations("home")
  const tn = useTranslations("nav")
  const [classes, setClasses] = useState<LiveClass[]>([])

  useEffect(() => {
    const sx = {
      recorded: t("live.statusRecorded"),
      liveNow: t("live.statusLiveNow"),
      ended: t("live.statusEnded"),
      intermediate: t("live.levelIntermediate"),
    }
    dashboardApi.getLiveClasses()
      .then((data) => setClasses(((data as ApiLiveClass[]) || []).slice(0, 4).map((lc) => mapApiToCard(lc, sx))))
      .catch(() => setClasses([]))
  }, [t])

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
