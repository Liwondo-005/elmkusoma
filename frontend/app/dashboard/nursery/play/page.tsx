"use client"

import { useEffect, useState, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryActivity } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { Puzzle, Music, Heart, Star, ArrowLeft, Sparkles, Gamepad2 } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

const GAME_CATEGORIES = [
  { labelKey: "allGames" as const, icon: Gamepad2, type: null, color: "bg-gradient-to-br from-purple-400 to-pink-500" },
  { labelKey: "funGames" as const, icon: Puzzle, type: "GAME", color: "bg-gradient-to-br from-pink-400 to-red-500" },
  { labelKey: "singAlong" as const, icon: Music, type: "SONG", color: "bg-gradient-to-br from-blue-400 to-cyan-500" },
  { labelKey: "moveAndDance" as const, icon: Heart, type: "PHYSICAL", color: "bg-gradient-to-br from-green-400 to-emerald-500" },
  { labelKey: "learnAndPlay" as const, icon: Sparkles, type: "EDUCATIONAL", color: "bg-gradient-to-br from-amber-400 to-orange-500" },
]

const CATEGORY_LABELS: Record<string, string> = {
  allGames: "All Games",
  funGames: "Fun Games",
  singAlong: "Sing Along",
  moveAndDance: "Move & Dance",
  learnAndPlay: "Learn & Play",
}

const ACTIVITY_ICONS: Record<string, typeof Puzzle> = {
  GAME: Puzzle,
  SONG: Music,
  STORY: Star,
  CRAFT: Heart,
  PHYSICAL: Heart,
  EDUCATIONAL: Sparkles,
}

export default function PlayAndLearnPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeType, setActiveType] = useState<string | null>(null)
  const [selectedActivity, setSelectedActivity] = useState<NurseryActivity | null>(null)

  const loadActivities = useCallback(() => {
    if (!user?.classGroupId) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    nurseryApi.getActivities(user.classGroupId)
      .then(setActivities)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => { loadActivities() }, [loadActivities])

  useEffect(() => {
    if (!selectedActivity) return
    function handleKey(e: KeyboardEvent) { if (e.key === "Escape") { setSelectedActivity(null) } }
    document.addEventListener("keydown", handleKey)
    return () => document.removeEventListener("keydown", handleKey)
  }, [selectedActivity])

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-800">{t("playAndLearn")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadActivities} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const filtered = activeType
    ? activities.filter(a => a.activityType === activeType)
    : activities

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("playAndLearn")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.play")}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {GAME_CATEGORIES.map((cat) => {
          const Icon = cat.icon
          return (
            <button
              key={cat.labelKey}
              onClick={() => setActiveType(activeType === cat.type ? null : cat.type)}
              aria-label={CATEGORY_LABELS[cat.labelKey]}
              aria-pressed={activeType === cat.type}
              className={`nursery-card flex flex-col items-center gap-2 rounded-2xl p-4 text-center text-white transition-all hover:scale-105 ${
                cat.color
              } ${activeType === cat.type ? "ring-4 ring-white shadow-lg" : ""}`}
            >
              <Icon className="size-8" />
              <span className="text-sm font-bold">{CATEGORY_LABELS[cat.labelKey]}</span>
            </button>
          )
        })}
      </div>

      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="nursery-card rounded-2xl bg-white p-8 text-center">
            <Gamepad2 className="mx-auto size-12 text-purple-400" />
            <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noGames")}</h3>
            <p className="mt-1 text-sm text-gray-500">{t("empty.teacherWillAdd")}</p>
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {filtered.map((activity) => {
              const Icon = ACTIVITY_ICONS[activity.activityType] || Puzzle
              const isCompleted = activity.status === "COMPLETED"
              return (
                <button
                  key={activity.id}
                  onClick={() => setSelectedActivity(activity)}
                  aria-label={`${activity.activityName} - ${activity.activityType}`}
                  className={`nursery-card flex flex-col items-center gap-3 rounded-2xl border-2 p-5 text-center transition-all hover:shadow-lg ${
                    isCompleted
                      ? "border-green-200 bg-green-50"
                      : "border-gray-100 bg-white hover:border-primary"
                  }`}
                >
                  <div className="flex size-14 items-center justify-center rounded-2xl bg-gradient-to-br from-purple-100 to-pink-100">
                    <Icon className="size-7 text-purple-500" />
                  </div>
                  <div>
                    <p className="font-bold text-gray-800">{activity.activityName}</p>
                    <p className="mt-1 text-xs text-gray-500">
                      {activity.durationMinutes || 15} min &bull; {activity.activityType}
                    </p>
                  </div>
                  {isCompleted && (
                    <span className="rounded-full bg-green-100 px-3 py-1 text-[10px] font-bold text-green-700">
                      {tc("completed")}
                    </span>
                  )}
                </button>
              )
            })}
          </div>
        )}
      </div>

      {selectedActivity && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 sm:items-center" role="dialog" aria-modal="true" aria-label={selectedActivity.activityName}>
          <div className="nursery-card w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-bold text-gray-800">{selectedActivity.activityName}</h2>
              <button
                onClick={() => setSelectedActivity(null)}
                className="flex size-8 items-center justify-center rounded-full bg-gray-100 text-gray-500"
                aria-label={tc("close")}
              >
                &#x2715;
              </button>
            </div>
            <div className="space-y-3">
              <div className="rounded-xl bg-purple-50 p-3">
                <p className="text-xs font-bold text-purple-700">Type</p>
                <p className="text-sm text-purple-600">{selectedActivity.activityType}</p>
              </div>
              {selectedActivity.description && (
                <div className="rounded-xl bg-blue-50 p-3">
                  <p className="text-xs font-bold text-blue-700">About this activity</p>
                  <p className="text-sm text-blue-600">{selectedActivity.description}</p>
                </div>
              )}
              {selectedActivity.instructions && (
                <div className="rounded-xl bg-green-50 p-3">
                  <p className="text-xs font-bold text-green-700">What to do</p>
                  <p className="text-sm text-green-600">{selectedActivity.instructions}</p>
                </div>
              )}
              {selectedActivity.durationMinutes && (
                <div className="rounded-xl bg-amber-50 p-3">
                  <p className="text-xs font-bold text-amber-700">Time</p>
                  <p className="text-sm text-amber-600">{selectedActivity.durationMinutes} minutes</p>
                </div>
              )}
              {selectedActivity.materialsNeeded && (
                <div className="rounded-xl bg-pink-50 p-3">
                  <p className="text-xs font-bold text-pink-700">Materials</p>
                  <p className="text-sm text-pink-600">{selectedActivity.materialsNeeded}</p>
                </div>
              )}
            </div>
            <button
              onClick={() => setSelectedActivity(null)}
              className="mt-4 w-full rounded-xl bg-primary py-3 text-sm font-bold text-white hover:bg-primary/90"
              aria-label={tc("startLearning")}
            >
              {tc("startLearning")}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
