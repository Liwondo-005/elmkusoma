"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type StudentBadge, type PortfolioItem, type LearningPassport } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Map, Globe, Plane, Rocket, Flag, Star, Trophy, Compass } from "lucide-react"

const stages = [
  { name: "Tanzania", flag: "TZ", icon: Flag, color: "text-green-600", bgColor: "bg-green-50", requiredStamps: 0 },
  { name: "East Africa", flag: "EA", icon: Globe, color: "text-blue-600", bgColor: "bg-blue-50", requiredStamps: 3 },
  { name: "Africa", flag: "AF", icon: Globe, color: "text-purple-600", bgColor: "bg-purple-50", requiredStamps: 8 },
  { name: "World Explorer", flag: "WE", icon: Plane, color: "text-amber-600", bgColor: "bg-amber-50", requiredStamps: 15 },
  { name: "Space Explorer", flag: "SE", icon: Rocket, color: "text-red-600", bgColor: "bg-red-50", requiredStamps: 25 },
]


export default function PassportPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const milestoneStamps = [
  { id: "first-lesson", name: t("passport.ms0n"), description: t("passport.ms0d"), threshold: 1 },
  { id: "5-lessons", name: t("passport.ms1n"), description: t("passport.ms1d"), threshold: 5 },
  { id: "10-lessons", name: t("passport.ms2n"), description: t("passport.ms2d"), threshold: 10 },
  { id: "25-lessons", name: t("passport.ms3n"), description: t("passport.ms3d"), threshold: 25 },
  { id: "50-lessons", name: t("passport.ms4n"), description: t("passport.ms4d"), threshold: 50 },
  { id: "first-badge", name: t("passport.ms5n"), description: t("passport.ms5d"), threshold: 1 },
  { id: "5-badges", name: t("passport.ms6n"), description: t("passport.ms6d"), threshold: 5 },
  { id: "first-project", name: t("passport.ms7n"), description: t("passport.ms7d"), threshold: 1 },
  { id: "3-projects", name: t("passport.ms8n"), description: t("passport.ms8d"), threshold: 3 },
  { id: "perfect-score", name: t("passport.ms9n"), description: t("passport.ms9d"), threshold: 1 },
  { id: "week-streak", name: t("passport.ms10n"), description: t("passport.ms10d"), threshold: 7 },
  { id: "month-streak", name: t("passport.ms11n"), description: t("passport.ms11d"), threshold: 30 },
  { id: "all-subjects", name: t("passport.ms12n"), description: t("passport.ms12d"), threshold: 8 },
  { id: "100-questions", name: t("passport.ms13n"), description: t("passport.ms13d"), threshold: 100 },
  { id: "first-essay", name: t("passport.ms14n"), description: t("passport.ms14d"), threshold: 1 },
  { id: "3-essays", name: t("passport.ms15n"), description: t("passport.ms15d"), threshold: 3 },
  { id: "10-photos", name: t("passport.ms16n"), description: t("passport.ms16d"), threshold: 10 },
  { id: "help-friend", name: t("passport.ms17n"), description: t("passport.ms17d"), threshold: 1 },
  { id: "attended-live", name: t("passport.ms18n"), description: t("passport.ms18d"), threshold: 1 },
  { id: "reading-1hr", name: t("passport.ms19n"), description: t("passport.ms19d"), threshold: 60 },
  { id: "math-wizard", name: t("passport.ms20n"), description: t("passport.ms20d"), threshold: 10 },
  { id: "science-explorer", name: t("passport.ms21n"), description: t("passport.ms21d"), threshold: 10 },
  { id: "english-pro", name: t("passport.ms22n"), description: t("passport.ms22d"), threshold: 10 },
  { id: "kiswahili-master", name: t("passport.ms23n"), description: t("passport.ms23d"), threshold: 10 },
  { id: "attendance-star", name: t("passport.ms24n"), description: t("passport.ms24d"), threshold: 30 },
]
  const ts = useTranslations("status")
  const [badges, setBadges] = useState<StudentBadge[]>([])
  const [portfolioItems, setPortfolioItems] = useState<PortfolioItem[]>([])
  const [passport, setPassport] = useState<LearningPassport | null>(null)
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [badgeData, portfolioData, passportData] = await Promise.all([
        primaryApi.getBadges().catch(() => []),
        primaryApi.getPortfolio().catch(() => []),
        primaryApi.getLearningPassport().catch(() => null),
      ])
      setBadges(badgeData)
      setPortfolioItems(portfolioData)
      setPassport(passportData)
    } catch {
      setBadges([])
      setPortfolioItems([])
      setPassport(null)
    } finally {
      setLoading(false)
    }
  }

  const totalStamps = passport?.stampsEarned ?? badges.length + portfolioItems.length

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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("passport.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("passport.subtitle")}</p>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-3xl border-2 border-amber-200 bg-gradient-to-br from-amber-50 via-white to-orange-50 shadow-lg">
        <div className="absolute top-0 right-0 size-32 rounded-bl-full bg-amber-100/50" />
        <div className="absolute bottom-0 left-0 size-24 rounded-tr-full bg-amber-100/30" />
        <div className="relative p-8">
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-widest text-amber-700">
            <Map className="size-4" />
            {t("passport.official")}
          </div>
          <h2 className="mt-3 text-3xl font-bold text-foreground">
            {user?.name || t("passport.studentFallback")}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {t("passport.levelLine", { level: level || t("passport.primaryFallback") })}
          </p>
          <div className="mt-4 flex items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-muted-foreground">{t("passport.currentStage")}</span>
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
              <p className="text-xs text-muted-foreground">{t("journey.stampsEarned")}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("passport.journeyTitle")}</h2>
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
                          {t("passport.visited")}
                        </span>
                      )}
                      {isActive && (
                        <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                          {t("passport.current")}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {stage.requiredStamps === 0 ? t("passport.startingPoint") : t("passport.stampsRequired", { count: stage.requiredStamps })}
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
                          {t("passport.progressTo", { pct: Math.round(progressToNext), name: nextStage.name })}
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
          <h2 className="text-lg font-semibold text-foreground">{t("passport.stampsTitle")}</h2>
          <span className="text-sm text-muted-foreground">
            {t("passport.collected", { done: milestoneStamps.filter((_, i) => isStampEarned(i)).length, total: milestoneStamps.length })}
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
              <p className="text-xs text-muted-foreground">{t("passport.totalStamps")}</p>
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
              <p className="text-xs text-muted-foreground">{t("passport.current")} Stage</p>
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
              <p className="text-xs text-muted-foreground">{t("passport.badgesEarned")}</p>
            </div>
          </div>
        </div>
      </div>

      {earnedStampCount === 0 && badges.length === 0 && (
        <div className="flex flex-col items-center rounded-2xl border border-dashed border-border py-16 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Map className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("passport.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("passport.emptyDesc")}
          </p>
        </div>
      )}
    </div>
  )
}
