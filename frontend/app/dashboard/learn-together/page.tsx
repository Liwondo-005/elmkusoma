"use client"

import { useRequireAuth } from "@/lib/auth"
import { type LearningLevel } from "@/lib/learner-config"
import { Users, BookOpen, Swords, Share2, UserPlus, ArrowRight } from "lucide-react"
import Link from "next/link"

const activities = [
  { title: "Study Groups", description: "Create or join a study group with classmates", icon: Users, color: "bg-blue-50 text-blue-600", border: "border-blue-200", href: "/dashboard/lessons" },
  { title: "Partner Reading", description: "Read together with a friend", icon: BookOpen, color: "bg-green-50 text-green-600", border: "border-green-200", href: "/dashboard/reading" },
  { title: "Team Challenges", description: "Solve problems as a team", icon: Swords, color: "bg-purple-50 text-purple-600", border: "border-purple-200", href: "/dashboard/quests" },
  { title: "Share My Work", description: "Show your creations to friends", icon: Share2, color: "bg-pink-50 text-pink-600", border: "border-pink-200", href: "/dashboard/portfolio" },
]

export default function LearnTogetherPage() {
  const { user } = useRequireAuth()
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-blue-500/10">
            <Users className="size-5 text-blue-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Learn Together</h1>
            <p className="text-sm text-muted-foreground">Learning is more fun with friends!</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Users className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Learn Together is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to collaborate with friends.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-blue-500/5 via-card to-green-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-500/10">
            <Users className="size-6 text-blue-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Learn Together</h1>
            <p className="text-sm text-muted-foreground">Learning is more fun with friends!</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
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
                <ArrowRight className="size-4 opacity-60" />
              </div>
            </Link>
          )
        })}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
          <UserPlus className="size-5 text-primary" />
          My Collaborations
        </h2>
        <div className="mt-4 flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/20 py-12 text-center">
          <Users className="size-10 text-muted-foreground/30" />
          <h3 className="mt-3 text-sm font-semibold text-foreground">Invite a friend to learn together!</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Learning with friends makes everything more fun. Choose an activity above to get started.
          </p>
        </div>
      </div>
    </div>
  )
}
