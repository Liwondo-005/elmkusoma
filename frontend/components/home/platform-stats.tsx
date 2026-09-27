"use client"

import { usePublicStats } from "@/lib/public-stats"

function fmt(n: number): string {
  return n.toLocaleString("en-US")
}

/** Stats `dl` inside the homepage CTA banner (replaces mock `stats` from lib/data). */
export function CtaStats() {
  const stats = usePublicStats()
  if (!stats) return null

  const items = [
    { value: fmt(stats.learners), label: "Learners" },
    { value: fmt(stats.instructors), label: "Instructors" },
    { value: fmt(stats.courses + stats.liveClasses), label: "Courses & Classes" },
    { value: fmt(stats.institutions), label: "Institutions" },
  ]

  return (
    <dl className="mt-10 grid grid-cols-2 gap-6 sm:grid-cols-4">
      {items.map((s) => (
        <div key={s.label}>
          <dt className="text-2xl font-extrabold">{s.value}</dt>
          <dd className="mt-1 text-xs text-primary-foreground/70">{s.label}</dd>
        </div>
      ))}
    </dl>
  )
}

/** Card grid on the About page (replaces mock `stats` from lib/data). */
export function AboutStatsGrid() {
  const stats = usePublicStats()
  if (!stats) return null

  const items = [
    { value: fmt(stats.learners), label: "Learners" },
    { value: fmt(stats.instructors), label: "Instructors" },
    { value: fmt(stats.courses + stats.liveClasses), label: "Courses & Classes" },
    { value: fmt(stats.institutions), label: "Institutions" },
  ]

  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      {items.map((s) => (
        <div key={s.label} className="rounded-2xl border border-border bg-card p-6 text-center shadow-xs">
          <p className="text-3xl font-extrabold text-primary">{s.value}</p>
          <p className="mt-2 text-sm font-medium text-foreground">{s.label}</p>
        </div>
      ))}
    </div>
  )
}

/** Floating learner-count badge over the hero image (replaces hardcoded "8,500+"). */
export function HeroLearnerBadge() {
  const stats = usePublicStats()
  if (!stats) return null

  return (
    <div className="absolute bottom-5 right-5 flex items-center gap-3 rounded-2xl border border-border bg-background/95 p-3 shadow-lg backdrop-blur">
      <div className="flex -space-x-2">
        {["A", "M", "J"].map((initial) => (
          <span
            key={initial}
            className="flex size-7 items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-bold text-primary-foreground"
          >
            {initial}
          </span>
        ))}
      </div>
      <div>
        <p className="text-sm font-bold text-foreground">{fmt(stats.learners)}</p>
        <p className="text-xs text-muted-foreground">Learners on ELMKUSOMA</p>
      </div>
    </div>
  )
}

/** Learner/instructor chips on the About hero (replaces hardcoded "8,500+ Learners"). */
export function AboutHighlights() {
  const stats = usePublicStats()
  if (!stats) return null

  const highlights = [`${fmt(stats.learners)} Learners`, `${fmt(stats.instructors)} Instructors`]

  return (
    <div className="mt-7 flex flex-wrap gap-3">
      {highlights.map((label) => (
        <span
          key={label}
          className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5 text-sm font-medium text-foreground shadow-xs"
        >
          {label}
        </span>
      ))}
    </div>
  )
}
