"use client"

import { useState } from "react"
import { BookOpen } from "lucide-react"
import { primaryClasses } from "@/lib/data"
import { cn } from "@/lib/utils"

export function PrimaryClasses() {
  const [activeId, setActiveId] = useState(primaryClasses[0].id)
  const activeClass = primaryClasses.find((c) => c.id === activeId) || primaryClasses[0]

  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Classes & Subjects
          </h2>
          <p className="mt-2 text-muted-foreground">
            Each class follows the Tanzanian primary curriculum. Select a class to view its subjects.
          </p>
        </div>

        <div className="mt-8 grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* Class tabs */}
          <div className="flex flex-col gap-2 lg:sticky lg:top-24 lg:self-start">
            {primaryClasses.map((cls) => (
              <button
                key={cls.id}
                type="button"
                onClick={() => setActiveId(cls.id)}
                className={cn(
                  "flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-medium transition-all",
                  activeId === cls.id
                    ? "border-primary bg-accent text-primary shadow-xs"
                    : "border-border bg-card text-foreground hover:border-primary/40 hover:shadow-xs",
                )}
              >
                <div
                  className={cn(
                    "flex size-9 shrink-0 items-center justify-center rounded-lg",
                    activeId === cls.id ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground",
                  )}
                >
                  <BookOpen className="size-4" />
                </div>
                <div>
                  <p className="font-semibold">{cls.name}</p>
                  <p className="text-xs text-muted-foreground">{cls.label} · {cls.ages}</p>
                </div>
              </button>
            ))}
          </div>

          {/* Subjects panel */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
                <BookOpen className="size-5" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-foreground">{activeClass.name}</h3>
                <p className="text-sm text-muted-foreground">{activeClass.label} · {activeClass.ages}</p>
              </div>
            </div>

            <div className="mt-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Masomo / Subjects ({activeClass.subjects.length})
              </p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {activeClass.subjects.map((subject, i) => (
                  <div
                    key={subject}
                    className="flex items-center gap-2 rounded-lg border border-border px-3 py-2.5"
                  >
                    <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-accent text-xs font-bold text-primary">
                      {i + 1}
                    </span>
                    <span className="text-sm font-medium text-foreground">{subject}</span>
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
