"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type PortfolioItem } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Mic, BookOpen, Music, Palette, PenTool, ArrowRight, Folder } from "lucide-react"

const activities = [
  { title: "Record a Story", description: "Tell your own story", icon: Mic, color: "bg-red-50 text-red-600", border: "border-red-200", type: "STORY" },
  { title: "Practice Reading", description: "Read aloud and record", icon: BookOpen, color: "bg-blue-50 text-blue-600", border: "border-blue-200", type: "VOICE_RECORDING" },
  { title: "Sing a Song", description: "Music makes learning fun", icon: Music, color: "bg-purple-50 text-purple-600", border: "border-purple-200", type: "VOICE_RECORDING" },
  { title: "Create Art", description: "Draw or paint something", icon: Palette, color: "bg-pink-50 text-pink-600", border: "border-pink-200", type: "DRAWING" },
  { title: "Write a Poem", description: "Express yourself in words", icon: PenTool, color: "bg-amber-50 text-amber-600", border: "border-amber-200", type: "ESSAY" },
]

export default function SpeakCreatePage() {
  const { user } = useRequireAuth()
  const [portfolio, setPortfolio] = useState<PortfolioItem[]>([])
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await primaryApi.getPortfolio().catch(() => [])
      setPortfolio(data)
    } catch {
      setPortfolio([])
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-red-500/10">
            <Mic className="size-5 text-red-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Speak & Create</h1>
            <p className="text-sm text-muted-foreground">Express yourself through voice and art</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Mic className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Speak & Create is for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to start creating.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-red-500/5 via-card to-pink-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-red-500/10">
            <Mic className="size-6 text-red-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Speak & Create</h1>
            <p className="text-sm text-muted-foreground">Express yourself through voice and art</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {activities.map((activity) => {
          const Icon = activity.icon
          return (
            <Link
              key={activity.title}
              href={`/dashboard/portfolio?type=${activity.type}`}
              className={`rounded-2xl border ${activity.border} ${activity.color} p-6 shadow-xs transition-all hover:shadow-md`}
            >
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-white/50">
                  <Icon className="size-6" />
                </div>
                <div>
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
          <Folder className="size-5 text-primary" />
          <h2 className="text-lg font-semibold text-foreground">My Creations</h2>
        </div>
        {portfolio.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/20 py-12 text-center">
            <Palette className="size-10 text-muted-foreground/30" />
            <h3 className="mt-3 text-sm font-semibold text-foreground">What would you like to create today?</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Choose an activity above to start creating!
            </p>
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {portfolio.slice(0, 6).map((item) => (
              <div key={item.id} className="rounded-xl border border-border p-4 transition-all hover:shadow-sm">
                <h4 className="text-sm font-bold text-foreground line-clamp-1">{item.title}</h4>
                {item.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{item.description}</p>
                )}
                <div className="mt-2 flex items-center gap-2">
                  <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                    {item.portfolioType.replace("_", " ")}
                  </span>
                  {item.subjectName && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                      {item.subjectName}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
