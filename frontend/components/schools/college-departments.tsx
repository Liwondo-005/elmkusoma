"use client"

import { useState } from "react"
import { Wrench, Briefcase, Monitor, Heart, GraduationCap, Leaf } from "lucide-react"
import { collegeDepartments } from "@/lib/data"
import { cn } from "@/lib/utils"

const iconMap: Record<string, typeof Wrench> = {
  Wrench,
  Briefcase,
  Monitor,
  Heart,
  GraduationCap,
  Leaf,
}

export function CollegeDepartments() {
  const [activeId, setActiveId] = useState(collegeDepartments[0].id)
  const activeDept = collegeDepartments.find((d) => d.id === activeId) || collegeDepartments[0]

  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Departments & Programs
          </h2>
          <p className="mt-2 text-muted-foreground">
            Browse departments and the programs they offer. Select a department to view available courses.
          </p>
        </div>

        <div className="mt-8 grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* Department tabs */}
          <div className="flex flex-col gap-2 lg:sticky lg:top-24 lg:self-start">
            {collegeDepartments.map((dept) => {
              const Icon = iconMap[dept.icon] || Wrench
              return (
                <button
                  key={dept.id}
                  type="button"
                  onClick={() => setActiveId(dept.id)}
                  className={cn(
                    "flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-medium transition-all",
                    activeId === dept.id
                      ? "border-primary bg-accent text-primary shadow-xs"
                      : "border-border bg-card text-foreground hover:border-primary/40 hover:shadow-xs",
                  )}
                >
                  <div
                    className={cn(
                      "flex size-9 shrink-0 items-center justify-center rounded-lg",
                      activeId === dept.id ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground",
                    )}
                  >
                    <Icon className="size-4" />
                  </div>
                  <div>
                    <p className="font-semibold">{dept.name}</p>
                    <p className="text-xs text-muted-foreground">{dept.programs.length} programs</p>
                  </div>
                </button>
              )
            })}
          </div>

          {/* Programs panel */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="flex items-center gap-3">
              {(() => {
                const Icon = iconMap[activeDept.icon] || Wrench
                return (
                  <div className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
                    <Icon className="size-5" />
                  </div>
                )
              })()}
              <div>
                <h3 className="text-lg font-bold text-foreground">{activeDept.name}</h3>
                <p className="text-sm text-muted-foreground">{activeDept.programs.length} programs available</p>
              </div>
            </div>

            <div className="mt-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Programs / Masomo
              </p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {activeDept.programs.map((program, i) => (
                  <div
                    key={program}
                    className="flex items-center gap-2 rounded-lg border border-border px-3 py-2.5"
                  >
                    <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-accent text-xs font-bold text-primary">
                      {i + 1}
                    </span>
                    <span className="text-sm font-medium text-foreground">{program}</span>
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
