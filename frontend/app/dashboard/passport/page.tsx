"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type StudentBadge, type PortfolioItem } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Map, Globe, Plane, Rocket, Flag, Star, Trophy, Compass } from "lucide-react"

const stages = [
  { name: "Tanzania", flag: "TZ", icon: Flag, color: "text-green-600", bgColor: "bg-green-50", requiredStamps: 0 },
  { name: "East Africa", flag: "EA", icon: Globe, color: "text-blue-600", bgColor: "bg-blue-50", requiredStamps: 3 },
  { name: "Africa", flag: "AF", icon: Globe, color: "text-purple-600", bgColor: "bg-purple-50", requiredStamps: 8 },
  { name: "World Explorer", flag: "WE", icon: Plane, color: "text-amber-600", bgColor: "bg-amber-50", requiredStamps: 15 },
  { name: "Space Explorer", flag: "SE", icon: Rocket, color: "text-red-600", bgColor: "bg-red-50", requiredStamps: 25 },
]

const milestoneStamps = [
  { id: "first-lesson", name: "First Lesson", description: "Complete your first lesson", threshold: 1 },
  { id: "5-lessons", name: "5 Lessons Done", description: "Complete 5 lessons", threshold: 5 },
  { id: "10-lessons", name: "Knowledge Seeker", description: "Complete 10 lessons", threshold: 10 },
  { id: "25-lessons", name: "Star Learner", description: "Complete 25 lessons", threshold: 25 },
  { id: "50-lessons", name: "Master Scholar", description: "Complete 50 lessons", threshold: 50 },
  { id: "first-badge", name: "Badge Collector", description: "Earn your first badge", threshold: 1 },
  { id: "5-badges", name: "Badge Master", description: "Earn 5 badges", threshold: 5 },
  { id: "first-project", name: "Creator", description: "Submit your first project", threshold: 1 },
  { id: "3-projects", name: "Builder", description: "Submit 3 projects", threshold: 3 },
  { id: "perfect-score", name: "Perfect Score", description: "Get 100% on any lesson", threshold: 1 },
  { id: "week-streak", name: "Week Warrior", description: "Maintain a 7-day streak", threshold: 7 },
  { id: "month-streak", name: "Monthly Champion", description: "Maintain a 30-day streak", threshold: 30 },
  { id: "all-subjects", name: "All-Rounder", description: "Try all 8 subjects", threshold: 8 },
  { id: "100-questions", name: "Quiz Master", description: "Answer 100 questions", threshold: 100 },
  { id: "first-essay", name: "Young Writer", description: "Write your first essay", threshold: 1 },
  { id: "3-essays", name: "Storyteller", description: "Write 3 essays", threshold: 3 },
  { id: "10-photos", name: "Photographer", description: "Upload 10 photos", threshold: 10 },
  { id: "help-friend", name: "Helper", description: "Help a classmate", threshold: 1 },
  { id: "attended-live", name: "Live Learner", description: "Attend a live class", threshold: 1 },
  { id: "reading-1hr", name: "Bookworm", description: "Read for 1 hour total", threshold: 60 },
  { id: "math-wizard", name: "Math Wizard", description: "Complete 10 math lessons", threshold: 10 },
  { id: "science-explorer", name: "Science Explorer", description: "Complete 10 science lessons", threshold: 10 },
  { id: "english-pro", name: "English Pro", description: "Complete 10 English lessons", threshold: 10 },
  { id: "kiswahili-master", name: "Kiswahili Master", description: "Complete 10 Kiswahili lessons", threshold: 10 },
  { id: "attendance-star", name: "Attendance Star", description: "Attend 30 days in a row", threshold: 30 },
]

export default function PassportPage() {
  const { user } = useRequireAuth()
  const [badges, setBadges] = useState<StudentBadge[]>([])
  const [portfolioItems, setPortfolioItems] = useState<PortfolioItem[]>([])
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [badgeData, portfolioData] = await Promise.all([
        primaryApi.getBadges().catch(() => []),
        primaryApi.getPortfolio().catch(() => []),
      ])
      setBadges(badgeData)
      setPortfolioItems(portfolioData)
    } catch {
      setBadges([])
      setPortfolioItems([])
    } finally {
      setLoading(false)
    }
  }

  const totalStamps = badges.length + portfolioItems.length

  function getEarnedCount(): number {
    let count = 0
    if (totalStamps >= 1) count++
    if (totalStamps >= 5) count++
    if (totalStamps >= 10) count++
    if (totalStamps >= 25) count++
    if (totalStamps >= 50) count++
    if (badges.length >= 1) count++
    if (badges.length >= 5) count++
    if (portfolioItems.filter((p) => p.portfolioType === "PROJECT").length >= 1) count++
    if (portfolioItems.filter((p) => p.portfolioType === "PROJECT").length >= 3) count++
    if (totalStamps >= 7) count++
    if (totalStamps >= 30) count++
    if (badges.length >= 3) count++
    if (portfolioItems.length >= 10) count++
    if (portfolioItems.filter((p) => p.portfolioType === "ESSAY").length >= 1) count++
    if (portfolioItems.filter((p) => p.portfolioType === "ESSAY").length >= 3) count++
    if (portfolioItems.filter((p) => p.portfolioType === "PHOTO").length >= 10) count++
    if (totalStamps >= 8) count++
    if (badges.length >= 2) count++
    if (totalStamps >= 15) count++
    if (portfolioItems.length >= 5) count++
    if (totalStamps >= 20) count++
    if (totalStamps >= 40) count++
    return Math.min(count, milestoneStamps.length)
  }

  const earnedStampCount = getEarnedCount()

  const currentStageIdx = stages.findIndex((stage, i) => {
    const next = stages[i + 1]
    if (!next) return true
    return earnedStampCount < next.requiredStamps
  })
  const activeStage = stages[Math.max(0, currentStageIdx)]
  const nextStage = stages[currentStageIdx + 1]
  const progressToNext = nextStage
    ? Math.min(100, ((earnedStampCount - activeStage.requiredStamps) / (nextStage.requiredStamps - activeStage.requiredStamps)) * 100)
    : 100

  function isStampEarned(idx: number): boolean {
    return idx < earnedStampCount
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
          <Map className="size-6 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Learning Passport</h1>
          <p className="text-sm text-muted-foreground">Your learning adventure around the world!</p>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-3xl border-2 border-amber-200 bg-gradient-to-br from-amber-50 via-white to-orange-50 shadow-lg">
        <div className="absolute top-0 right-0 size-32 rounded-bl-full bg-amber-100/50" />
        <div className="absolute bottom-0 left-0 size-24 rounded-tr-full bg-amber-100/30" />
        <div className="relative p-8">
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-widest text-amber-700">
            <Map className="size-4" />
            Official Learning Passport
          </div>
          <h2 className="mt-3 text-3xl font-bold text-foreground">
            {user?.name || "Student"}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Level: {level || "Primary"} Learner
          </p>
          <div className="mt-4 flex items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-muted-foreground">Current Stage:</span>
              <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${activeStage.bgColor} ${activeStage.color}`}>
                <activeStage.icon className="size-3" />
                {activeStage.name}
              </span>
            </div>
          </div>
          <div className="mt-4 flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100">
              <Star className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-lg font-bold text-foreground">{earnedStampCount}</p>
              <p className="text-xs text-muted-foreground">Stamps Earned</p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">Your Journey</h2>
        <div className="relative">
          <div className="absolute left-5 top-0 bottom-0 w-0.5 bg-muted" />
          <div className="space-y-4">
            {stages.map((stage) => {
              const isActive = stage.name === activeStage.name
              const isCompleted = earnedStampCount >= stage.requiredStamps && !isActive
              const isLocked = !isCompleted && !isActive
              return (
                <div key={stage.name} className="relative flex items-center gap-4">
                  <div className={`relative z-10 flex size-10 items-center justify-center rounded-full border-2 ${
                    isCompleted
                      ? "border-green-500 bg-green-500 text-white"
                      : isActive
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-muted text-muted-foreground"
                  }`}>
                    {isCompleted ? (
                      <Star className="size-4 fill-current" />
                    ) : (
                      <stage.icon className="size-4" />
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className={`text-sm font-semibold ${isLocked ? "text-muted-foreground" : "text-foreground"}`}>
                        {stage.name}
                      </span>
                      {isCompleted && (
                        <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-700">
                          Visited
                        </span>
                      )}
                      {isActive && (
                        <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                          Current
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {stage.requiredStamps === 0 ? "Starting point" : `${stage.requiredStamps} stamps required`}
                    </p>
                    {isActive && nextStage && (
                      <div className="mt-2">
                        <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-primary to-teal transition-all"
                            style={{ width: `${progressToNext}%` }}
                          />
                        </div>
                        <p className="mt-1 text-[10px] text-muted-foreground">
                          {Math.round(progressToNext)}% to {nextStage.name}
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-foreground">Stamp Collection</h2>
          <span className="text-sm text-muted-foreground">
            {milestoneStamps.filter((_, i) => isStampEarned(i)).length} / {milestoneStamps.length} collected
          </span>
        </div>
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6">
          {milestoneStamps.map((stamp, idx) => {
            const earned = isStampEarned(idx)
            return (
              <div
                key={stamp.id}
                className={`group relative flex flex-col items-center rounded-2xl border-2 p-3 transition-all ${
                  earned
                    ? "border-primary bg-primary/5 shadow-sm hover:shadow-md"
                    : "border-dashed border-border bg-muted/30 opacity-60"
                }`}
              >
                <div className={`flex size-14 items-center justify-center rounded-full border-2 ${
                  earned
                    ? "border-primary bg-primary/10"
                    : "border-border bg-muted"
                }`}>
                  <span className={`text-lg font-bold ${earned ? "text-primary" : "text-muted-foreground"}`}>
                    {stamp.name.charAt(0)}
                  </span>
                </div>
                <span className={`mt-2 text-[10px] font-semibold text-center leading-tight ${
                  earned ? "text-foreground" : "text-muted-foreground"
                }`}>
                  {stamp.name}
                </span>
                {earned && (
                  <div className="absolute -top-1 -right-1 flex size-5 items-center justify-center rounded-full bg-green-500">
                    <Star className="size-3 text-white fill-white" />
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <Star className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{earnedStampCount}</p>
              <p className="text-xs text-muted-foreground">Total Stamps</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className={`flex size-10 items-center justify-center rounded-xl ${activeStage.bgColor}`}>
              <activeStage.icon className={`size-5 ${activeStage.color}`} />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{activeStage.name}</p>
              <p className="text-xs text-muted-foreground">Current Stage</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Trophy className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{badges.length}</p>
              <p className="text-xs text-muted-foreground">Badges Earned</p>
            </div>
          </div>
        </div>
      </div>

      {earnedStampCount === 0 && badges.length === 0 && (
        <div className="flex flex-col items-center rounded-2xl border border-dashed border-border py-16 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Map className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">Start your learning journey!</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Complete lessons, earn badges, and collect stamps to fill your passport. Your adventure begins now!
          </p>
        </div>
      )}
    </div>
  )
}
