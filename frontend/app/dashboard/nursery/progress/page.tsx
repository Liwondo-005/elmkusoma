"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMilestone, type NurseryActivity } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Trophy, Star, TrendingUp, Calendar, CheckCircle } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

const CATEGORY_CONFIG: Record<string, { label: string; emoji: string; color: string }> = {
  PHYSICAL: { label: "Physical", emoji: "🏃", color: "bg-red-100 text-red-600" },
  COGNITIVE: { label: "Thinking", emoji: "🧠", color: "bg-blue-100 text-blue-600" },
  SOCIAL: { label: "Friends", emoji: "🤝", color: "bg-green-100 text-green-600" },
  EMOTIONAL: { label: "Feelings", emoji: "❤️", color: "bg-pink-100 text-pink-600" },
  LANGUAGE: { label: "Words", emoji: "💬", color: "bg-purple-100 text-purple-600" },
  MOTOR: { label: "Hands", emoji: "✋", color: "bg-amber-100 text-amber-600" },
}

export default function ProgressPage() {
  const t = useTranslations("nursery")
  const { user } = useRequireAuth()
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    Promise.all([
      user.id ? nurseryApi.getMilestones(user.id).catch(() => []) : Promise.resolve([]),
      user.classGroupId ? nurseryApi.getActivities(user.classGroupId).catch(() => []) : Promise.resolve([]),
    ]).then(([m, a]) => {
      setMilestones(m)
      setActivities(a)
    }).catch(() => setError("Failed to load progress data"))
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  const achieved = milestones.filter(m => m.status === "ACHIEVED").length
  const inProgress = milestones.filter(m => m.status === "IN_PROGRESS").length
  const completedActivities = activities.filter(a => a.status === "COMPLETED").length
  const totalActivities = activities.length

  const categoryStats = Object.entries(CATEGORY_CONFIG).map(([key, config]) => {
    const catMilestones = milestones.filter(m => m.category === key)
    const catAchieved = catMilestones.filter(m => m.status === "ACHIEVED").length
    return {
      ...config,
      key,
      total: catMilestones.length,
      achieved: catAchieved,
      percent: catMilestones.length > 0 ? Math.round((catAchieved / catMilestones.length) * 100) : 0,
    }
  })

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label="Back to nursery dashboard">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("progress.overall")}</h1>
          <p className="text-sm text-gray-500">{t("growing")}</p>
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="nursery-card rounded-2xl bg-yellow-50 p-4 text-center">
          <Trophy className="mx-auto size-8 text-yellow-500" />
          <div className="mt-2 text-2xl font-bold text-yellow-600">{achieved}</div>
          <div className="text-[10px] font-bold text-yellow-700">{t("progress.milestonesAchieved")}</div>
        </div>
        <div className="nursery-card rounded-2xl bg-blue-50 p-4 text-center">
          <Star className="mx-auto size-8 text-blue-500" />
          <div className="mt-2 text-2xl font-bold text-blue-600">{inProgress}</div>
          <div className="text-[10px] font-bold text-blue-700">{t("progress.missionsDone")}</div>
        </div>
        <div className="nursery-card rounded-2xl bg-green-50 p-4 text-center">
          <CheckCircle className="mx-auto size-8 text-green-500" />
          <div className="mt-2 text-2xl font-bold text-green-600">{completedActivities}</div>
          <div className="text-[10px] font-bold text-green-700">{t("activities")}</div>
        </div>
        <div className="nursery-card rounded-2xl bg-purple-50 p-4 text-center">
          <Calendar className="mx-auto size-8 text-purple-500" />
          <div className="mt-2 text-2xl font-bold text-purple-600">{totalActivities}</div>
          <div className="text-[10px] font-bold text-purple-700">{t("stats.total")}</div>
        </div>
      </div>

      {/* Category Progress */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">{t("growing")}</h2>
        <div className="space-y-3">
          {categoryStats.map((cat) => (
            <div key={cat.key} className="nursery-card rounded-2xl bg-white p-4">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{cat.emoji}</span>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <p className="font-bold text-gray-800">{cat.label}</p>
                    <span className="text-xs font-bold text-gray-500">{cat.achieved}/{cat.total}</span>
                  </div>
                  <div className="mt-2 h-3 overflow-hidden rounded-full bg-gray-100" role="progressbar" aria-valuenow={cat.percent} aria-valuemin={0} aria-valuemax={100} aria-label={`${cat.label} progress: ${cat.percent}%`}>
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-primary to-purple-500 transition-all"
                      style={{ width: `${cat.percent}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Recent Milestones */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">{t("recentMilestones")}</h2>
        {milestones.length === 0 ? (
          <div className="nursery-card rounded-2xl bg-white p-8 text-center">
            <Star className="mx-auto size-12 text-yellow-400" />
            <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noMilestones")}</h3>
            <p className="mt-1 text-sm text-gray-500">{t("empty.teacherWillAdd")}</p>
          </div>
        ) : (
          <div className="space-y-2">
            {milestones.slice(0, 10).map((m) => {
              const cat = CATEGORY_CONFIG[m.category]
              return (
                <div key={m.id} className="nursery-card flex items-center gap-3 rounded-2xl bg-white p-3">
                  <span className="text-xl">{cat?.emoji || "⭐"}</span>
                  <div className="flex-1">
                    <p className="text-sm font-bold text-gray-800">{m.milestoneName}</p>
                    <p className="text-[10px] text-gray-500">{cat?.label}</p>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                    m.status === "ACHIEVED" ? "bg-green-100 text-green-700" :
                    m.status === "IN_PROGRESS" ? "bg-yellow-100 text-yellow-700" :
                    "bg-gray-100 text-gray-500"
                  }`}>
                    {m.status === "ACHIEVED" ? t("stats.completed") : m.status}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
