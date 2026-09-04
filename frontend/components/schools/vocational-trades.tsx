"use client"

import { useState } from "react"
import { vetaCategories } from "@/lib/data"
import { cn } from "@/lib/utils"

export function VocationalTrades() {
  const [activeId, setActiveId] = useState(vetaCategories[0].id)
  const activeCat = vetaCategories.find((c) => c.id === activeId) || vetaCategories[0]

  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Trade Categories
          </h2>
          <p className="mt-2 text-muted-foreground">
            Browse VETA trades by category. Select a category to view available programs and durations.
          </p>
        </div>

        <div className="mt-8 grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* Category tabs */}
          <div className="flex flex-col gap-2 lg:sticky lg:top-24 lg:self-start">
            {vetaCategories.map((cat) => {
              return (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => setActiveId(cat.id)}
                  className={cn(
                    "flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-medium transition-all",
                    activeId === cat.id
                      ? "border-primary bg-accent text-primary shadow-xs"
                      : "border-border bg-card text-foreground hover:border-primary/40 hover:shadow-xs",
                  )}
                >
                  <div className="flex-1">
                    <p className="font-semibold">{cat.name}</p>
                    <p className="text-xs text-muted-foreground">{cat.trades.length} trades</p>
                  </div>
                </button>
              )
            })}
          </div>

          {/* Trades panel */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex-1">
                <h3 className="text-lg font-bold text-foreground">{activeCat.name}</h3>
                <p className="text-sm text-muted-foreground">{activeCat.trades.length} trade programs</p>
              </div>
            </div>

            <div className="mt-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Trades / Fundi Skills
              </p>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                {activeCat.trades.map((trade, i) => (
                  <div
                    key={trade.id}
                    className="flex items-start gap-3 rounded-lg border border-border p-3"
                  >
                    <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-accent text-xs font-bold text-primary">
                      {i + 1}
                    </span>
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-foreground">{trade.name}</p>
                      <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                        <span className="rounded bg-muted px-1.5 py-0.5">{trade.duration}</span>
                        <span className="rounded bg-teal/10 px-1.5 py-0.5 text-teal font-medium">{trade.level}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
