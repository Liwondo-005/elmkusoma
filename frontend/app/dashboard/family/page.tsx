"use client"

import { useRequireAuth } from "@/lib/auth"
import { type LearningLevel } from "@/lib/learner-config"
import { Home, HelpCircle, BookOpen, FlaskConical, Palette, Music, ArrowRight, Share2 } from "lucide-react"
import Link from "next/link"

const activities = [
  { title: "Family Quiz Night", description: "Fun quiz questions for the whole family", icon: HelpCircle, color: "bg-amber-50 text-amber-600", border: "border-amber-200", href: "/dashboard/assessments" },
  { title: "Read Together", description: "Share stories with your family", icon: BookOpen, color: "bg-green-50 text-green-600", border: "border-green-200", href: "/dashboard/reading" },
  { title: "Science at Home", description: "Fun experiments to do together", icon: FlaskConical, color: "bg-blue-50 text-blue-600", border: "border-blue-200", href: "/dashboard/labs" },
  { title: "Art & Craft", description: "Create something beautiful together", icon: Palette, color: "bg-pink-50 text-pink-600", border: "border-pink-200", href: "/dashboard/portfolio" },
  { title: "Music Time", description: "Sing and make music together", icon: Music, color: "bg-purple-50 text-purple-600", border: "border-purple-200", href: "/dashboard/speak-create" },
]

export default function FamilyPage() {
  const { user } = useRequireAuth()
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-pink-500/10">
            <Home className="size-5 text-pink-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Family Learning</h1>
            <p className="text-sm text-muted-foreground">Learn together with your family!</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Home className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Family Learning is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to start family activities.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-pink-500/5 via-card to-amber-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-pink-500/10">
            <Home className="size-6 text-pink-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Family Learning</h1>
            <p className="text-sm text-muted-foreground">Learn together with your family!</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {activities.map((activity) => {
          const Icon = activity.icon
          return (
            <Link
              key={activity.title}
              href={activity.href}
              className={`rounded-2xl border ${activity.border} ${activity.color} p-6 shadow-xs transition-all hover:shadow-md`}
            >
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-white/50">
                  <Icon className="size-6" />
                </div>
                <div className="flex-1">
                  <h3 className="text-base font-bold">{activity.title}</h3>
                  <p className="text-xs opacity-80">{activity.description}</p>
                </div>
              </div>
              <div className="mt-4 inline-flex items-center gap-1 text-xs font-semibold">
                Start <ArrowRight className="size-3" />
              </div>
            </Link>
          )
        })}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Share2 className="size-5 text-primary" />
          <h2 className="text-lg font-semibold text-foreground">Share with Family</h2>
        </div>
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/20 py-12 text-center">
          <Home className="size-10 text-muted-foreground/30" />
          <h3 className="mt-3 text-sm font-semibold text-foreground">Invite your family to learn together!</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Choose an activity above to start a family learning session.
          </p>
        </div>
      </div>
    </div>
  )
}
