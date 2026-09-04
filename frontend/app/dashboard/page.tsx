"use client"

import Link from "next/link"
import Image from "next/image"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

const statCards = [
  { label: "Enrolled Courses", value: "6", note: "Active" },
  { label: "Live Classes", value: "4", note: "This Week" },
  { label: "Hours Learned", value: "24h 30m", note: "This Month" },
  { label: "Certificates", value: "2", note: "Earned" },
]

const continueLearning = [
  { title: "Web Development Bootcamp", image: "/images/class-webdev.png", progress: 75 },
  { title: "Data Science with Python", image: "/images/class-datascience.png", progress: 40 },
  { title: "Digital Marketing Strategy", image: "/images/class-marketing.png", progress: 20 },
]

const upcoming = [
  { title: "Advanced Mathematics", sub: "Calculus II", teacher: "Dr. John Mwangi", live: true, meta: "128 watching" },
  { title: "Physics Mechanics", sub: "Problem Solving", teacher: "Prof. A. Hamdan", live: false, meta: "Tomorrow, 10:00 AM" },
  { title: "Digital Marketing", sub: "Strategy", teacher: "Sarah K.", live: false, meta: "Fri, 12:00 PM" },
]

export default function DashboardPage() {
  const { user } = useAuth()
  const firstName = user?.name?.split(" ")[0] || "Student"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Welcome */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Welcome back, {firstName}!</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Keep up the great work and continue your learning journey.
        </p>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <p className="text-2xl font-extrabold text-foreground">{s.value}</p>
            <p className="text-sm font-medium text-foreground">{s.label}</p>
            <p className="text-xs text-muted-foreground">{s.note}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Continue learning */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Continue Learning</h2>
            <Link href="/courses" className="text-xs font-medium text-primary hover:underline">
              Browse Courses
            </Link>
          </div>
          <div className="mt-4 space-y-4">
            {continueLearning.map((c) => (
              <div key={c.title} className="flex items-center gap-4">
                <div className="relative size-14 shrink-0 overflow-hidden rounded-xl">
                  <Image src={c.image || "/placeholder.svg"} alt={c.title} fill className="object-cover" sizes="56px" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-3">
                    <p className="truncate text-sm font-medium text-foreground">{c.title}</p>
                    <span className="text-xs font-semibold text-teal">{c.progress}%</span>
                  </div>
                  <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-teal" style={{ width: `${c.progress}%` }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Upcoming live */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Upcoming Live</h2>
            <Link href="/live-classes" className="text-xs font-medium text-primary hover:underline">
              View All
            </Link>
          </div>
          <div className="mt-4 space-y-4">
            {upcoming.map((u) => (
              <div key={u.title} className="flex items-start gap-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">
                    {u.title} <span className="text-muted-foreground">{u.sub}</span>
                  </p>
                  <p className="truncate text-xs text-muted-foreground">{u.teacher}</p>
                  <div className="mt-1 flex items-center gap-2">
                    {u.live ? (
                      <span className="inline-flex items-center gap-1 rounded bg-teal/10 px-1.5 py-0.5 text-[10px] font-semibold text-teal">
                        <span className="size-1.5 animate-pulse rounded-full bg-teal" /> LIVE NOW
                      </span>
                    ) : (
                      <span className="text-[11px] text-muted-foreground">{u.meta}</span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* Quick actions */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
        <div className="mt-4 flex flex-wrap gap-3">
          <Link href="/courses" className="rounded-lg border border-border bg-muted/50 px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-muted">
            Browse Courses
          </Link>
          <Link href="/live-classes" className="rounded-lg border border-border bg-muted/50 px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-muted">
            Live Classes
          </Link>
          <Link href="/notes-library" className="rounded-lg border border-border bg-muted/50 px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-muted">
            Notes & Materials
          </Link>
          <Link href="/certificates/verify" className="rounded-lg border border-border bg-muted/50 px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-muted">
            Verify Certificate
          </Link>
        </div>
      </section>
    </div>
  )
}
