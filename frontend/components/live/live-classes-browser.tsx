"use client"

import { useState, useEffect } from "react"
import { dashboardApi, type LiveClass as ApiLiveClass } from "@/lib/api"
import { LiveClassCard } from "@/components/live-class-card"
import { cn } from "@/lib/utils"
import type { LiveClass } from "@/lib/data"

const tabs = [
  { id: "upcoming", label: "Upcoming Classes", count: 0 },
  { id: "live", label: "Live Now", count: 0 },
  { id: "past", label: "Past Classes", count: 0 },
] as const

type TabId = (typeof tabs)[number]["id"]

function mapApiToCard(lc: ApiLiveClass): LiveClass {
  const now = new Date()
  const scheduled = new Date(lc.scheduledAt)
  const end = new Date(scheduled.getTime() + lc.durationMinutes * 60000)
  const isPast = end < now
  const isLive = scheduled <= now && end >= now
  const diffMs = scheduled.getTime() - now.getTime()
  const diffHours = Math.round(diffMs / (1000 * 60 * 60))

  let status: LiveClass["status"]
  let badge: string
  if (isPast) { status = "past"; badge = "RECORDED" }
  else if (isLive) { status = "live"; badge = "LIVE NOW" }
  else if (diffHours <= 24) { status = "soon"; badge = "IN 1 HOUR" }
  else { status = "scheduled"; badge = scheduled.toLocaleDateString() }

  return {
    id: lc.id,
    title: lc.title,
    subtitle: lc.description?.slice(0, 40) || "",
    instructor: "",
    image: "/images/class-default.png",
    status,
    badge,
    time: isPast ? "Ended" : scheduled.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    level: "Intermediate",
  }
}

export function LiveClassesBrowser() {
  const [active, setActive] = useState<TabId>("upcoming")
  const [classes, setClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    dashboardApi.getLiveClasses()
      .then((data) => setClasses(((data as ApiLiveClass[]) || []).map(mapApiToCard)))
      .catch(() => setClasses([]))
      .finally(() => setLoading(false))
  }, [])

  const filtered = classes.filter((c) => {
    if (active === "live") return c.status === "live"
    if (active === "past") return c.status === "past"
    return c.status !== "live" && c.status !== "past"
  })

  const liveCount = classes.filter((c) => c.status === "live").length
  const upcomingCount = classes.filter((c) => c.status !== "live" && c.status !== "past").length
  const pastCount = classes.filter((c) => c.status === "past").length

  return (
    <section className="py-10">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-1 border-b border-border">
          {tabs.map((tab) => {
            const count = tab.id === "live" ? liveCount : tab.id === "past" ? pastCount : upcomingCount
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActive(tab.id)}
                className={cn(
                  "relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors",
                  active === tab.id ? "text-primary" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {tab.label}
                {count > 0 && (
                  <span className="inline-flex size-5 items-center justify-center rounded-full bg-orange text-[10px] font-bold text-orange-foreground">
                    {count}
                  </span>
                )}
                {active === tab.id && (
                  <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-primary" />
                )}
              </button>
            )
          })}
        </div>

        {loading ? (
          <div className="mt-8 flex justify-center py-20">
            <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : filtered.length > 0 ? (
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((item) => (
              <LiveClassCard key={item.id} item={item} />
            ))}
          </div>
        ) : (
          <div className="mt-8 flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/40 py-20 text-center">
            <p className="text-sm font-medium text-foreground">
              {active === "past" ? "No past classes yet" : "No upcoming classes"}
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              {active === "past"
                ? "Recorded sessions will appear here once live classes have ended."
                : "Check back later for scheduled live classes."}
            </p>
          </div>
        )}
      </div>
    </section>
  )
}
