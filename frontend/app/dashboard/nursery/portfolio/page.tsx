"use client"

import { useEffect, useState, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMilestone } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Briefcase, Award, Image, Star } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

export default function MyPortfolioPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadMilestones = useCallback(() => {
    if (!user?.id) { setLoading(false); return }
    setLoading(true)
    setError(null)
    nurseryApi.getMilestones(user.id)
      .then(setMilestones)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => { loadMilestones() }, [loadMilestones])

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-800">{t("myPortfolio")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadMilestones} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const byCategory = milestones.reduce((acc, m) => {
    acc[m.category] = (acc[m.category] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("myPortfolio")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.portfolio")}</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-purple-500 to-pink-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Briefcase className="size-8" />
          <div>
            <h2 className="text-lg font-bold">{user?.name || t("myPortfolio")}</h2>
            <p className="text-sm text-white/70">{milestones.length} {t("stats.milestones")}</p>
          </div>
        </div>
      </div>

      {Object.keys(byCategory).length > 0 && (
        <div className="grid grid-cols-2 gap-3">
          {Object.entries(byCategory).map(([cat, count]) => (
            <div key={cat} className="nursery-card rounded-2xl bg-white p-4">
              <div className="flex items-center gap-2">
                <Star className="size-4 text-amber-400" />
                <p className="text-sm font-bold text-gray-700">{cat}</p>
              </div>
              <p className="mt-1 text-2xl font-bold text-gray-800">{count}</p>
            </div>
          ))}
        </div>
      )}

      {milestones.length > 0 ? (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">{t("myAchievements")}</h2>
          <div className="space-y-2">
            {milestones.map(m => (
              <div key={m.id} className="nursery-card flex items-center gap-3 rounded-2xl bg-white p-4">
                <div className="flex size-10 items-center justify-center rounded-full bg-amber-100">
                  <Award className="size-5 text-amber-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{m.description || m.category}</p>
                  <p className="text-xs text-gray-500">{m.category}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      ) : (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Briefcase className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noPortfolio")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.keepLearning")}</p>
        </div>
      )}
    </div>
  )
}
