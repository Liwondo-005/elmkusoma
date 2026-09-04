"use client"

import { useState } from "react"
import { degreeLevels } from "@/lib/data"
import { cn } from "@/lib/utils"

const levelColors: Record<string, string> = {
  diploma: "bg-orange/10 text-orange",
  bachelors: "bg-primary/10 text-primary",
  masters: "bg-teal/10 text-teal",
  phd: "bg-purple-500/10 text-purple-600",
}

export function UniversityLevels() {
  const [activeId, setActiveId] = useState(degreeLevels[0].id)
  const activeLevel = degreeLevels.find((l) => l.id === activeId) || degreeLevels[0]

  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Degree Levels
          </h2>
          <p className="mt-2 text-muted-foreground">
            Explore programs by qualification level — from diplomas to doctorates.
          </p>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {degreeLevels.map((level) => {
            const color = levelColors[level.id] || "bg-muted text-muted-foreground"
            return (
              <button
                key={level.id}
                type="button"
                onClick={() => setActiveId(level.id)}
                className={cn(
                  "group flex flex-col items-start rounded-2xl border p-5 text-left transition-all",
                  activeId === level.id
                    ? "border-primary bg-accent shadow-xs"
                    : "border-border bg-card hover:border-primary/40 hover:shadow-xs",
                )}
              >
                <h3 className="mt-3 text-base font-bold text-foreground">{level.name}</h3>
                <p className="mt-0.5 text-xs text-muted-foreground">{level.duration}</p>
                <p className="mt-2 text-sm text-muted-foreground">{level.description}</p>
              </button>
            )
          })}
        </div>

        <div className="mt-8 rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div>
            <h3 className="text-lg font-bold text-foreground">{activeLevel.name}</h3>
            <p className="text-sm text-muted-foreground">{activeLevel.duration} · {activeLevel.description}</p>
          </div>

          <div className="mt-6">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Available at these universities
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {[
                { name: "UDSM", has: activeLevel.id === "diploma" || activeLevel.id === "bachelors" || activeLevel.id === "masters" || activeLevel.id === "phd" },
                { name: "UDOM", has: activeLevel.id === "bachelors" || activeLevel.id === "masters" },
                { name: "MUHAS", has: activeLevel.id === "bachelors" || activeLevel.id === "masters" || activeLevel.id === "phd" },
                { name: "ARU", has: activeLevel.id === "diploma" || activeLevel.id === "bachelors" || activeLevel.id === "masters" },
                { name: "NM-AIST", has: activeLevel.id === "masters" || activeLevel.id === "phd" },
              ]
                .filter((u) => u.has)
                .map((u) => (
                  <span
                    key={u.name}
                    className="rounded-lg border border-border bg-muted/50 px-3 py-1.5 text-sm font-medium text-foreground"
                  >
                    {u.name}
                  </span>
                ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
