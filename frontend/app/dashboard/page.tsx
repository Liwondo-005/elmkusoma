"use client"

import Link from "next/link"
import Image from "next/image"
import { BookOpen, Video, Clock, Award, CheckCircle, ArrowRight } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

const statCards = [
  { label: "Enrolled Courses", value: "6", note: "Active", icon: BookOpen },
  { label: "Live Classes", value: "4", note: "This Week", icon: Video },
  { label: "Hours Learned", value: "24h 30m", note: "This Month", icon: Clock },
  { label: "Certificates", value: "2", note: "Earned", icon: Award },
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

const activity = [
  { label: "Completed lesson", detail: "CSS Flexbox & Grid", time: "2h ago" },
  { label: "Watched live class", detail: "Data Science Q&A", time: "Yesterday" },
  { label: "Submitted assignment", detail: "Data Analysis Project", time: "2 days ago" },
  { label: "Earned certificate", detail: "HTML & CSS Basics", time: "3 days ago" },
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
            <s.icon className="mb-2 size-5 text-muted-foreground" />
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
            <Link href="/dashboard/courses" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
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
            <Link href="/live-classes" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
              View All <ArrowRight className="size-3" />
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

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Recent activity */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Recent Activity</h2>
          <div className="mt-4 space-y-4">
            {activity.map((a) => (
              <div key={a.detail} className="flex items-center gap-3">
                <div className="min-w-0 flex-1">
                  <p className="text-xs text-muted-foreground">{a.label}</p>
                  <p className="truncate text-sm font-medium text-foreground">{a.detail}</p>
                </div>
                <span className="shrink-0 text-xs text-muted-foreground">{a.time}</span>
              </div>
            ))}
          </div>
        </section>

        {/* CTA */}
        <section className="flex flex-col justify-between overflow-hidden rounded-2xl bg-primary p-6 text-primary-foreground">
          <div>
            <h2 className="text-lg font-bold">Reach your goals faster</h2>
            <p className="mt-2 text-sm text-primary-foreground/80">
              Unlock premium live classes, recordings and the full digital library.
            </p>
          </div>
          <Link
            href="/dashboard/subscription"
            className={cn(
              buttonVariants(),
              "mt-6 h-11 w-full gap-2 bg-background text-foreground hover:bg-background/90",
            )}
          >
            Upgrade Plan
          </Link>
        </section>
      </div>
    </div>
  )
}
