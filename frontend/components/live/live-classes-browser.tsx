"use client"

import { useState } from "react"
import { liveClasses } from "@/lib/data"
import { LiveClassCard } from "@/components/live-class-card"
import { cn } from "@/lib/utils"

const tabs = [
  { id: "upcoming", label: "Upcoming Classes" },
  { id: "live", label: "Live Now", count: 1 },
  { id: "past", label: "Past Classes" },
] as const

type TabId = (typeof tabs)[number]["id"]

export function LiveClassesBrowser() {
  const [active, setActive] = useState<TabId>("upcoming")

  const filtered = liveClasses.filter((c) => {
    if (active === "live") return c.status === "live"
    if (active === "past") return false
    return c.status !== "live"
  })

  return (
    <section className="py-10">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-1 border-b border-border">
          {tabs.map((tab) => (
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
              {tab.count ? (
                <span className="inline-flex size-5 items-center justify-center rounded-full bg-orange text-[10px] font-bold text-orange-foreground">
                  {tab.count}
                </span>
              ) : null}
              {active === tab.id && (
                <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-primary" />
              )}
            </button>
          ))}
        </div>

        {filtered.length > 0 ? (
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((item) => (
              <LiveClassCard key={item.id} item={item} />
            ))}
          </div>
        ) : (
          <div className="mt-8 flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/40 py-20 text-center">
            <p className="text-sm font-medium text-foreground">No past classes yet</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Recorded sessions will appear here once live classes have ended.
            </p>
          </div>
        )}
      </div>
    </section>
  )
}
