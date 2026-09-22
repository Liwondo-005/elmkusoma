"use client"

import { ArrowLeft, Trophy, Star, CheckCircle, Clock, Target } from "lucide-react"
import Link from "next/link"
import { useState, useEffect, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMilestone } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { useTranslations } from "next-intl"

const CATEGORY_INFO: Record<string, { emoji: string; label: string; color: string }> = {
  PHYSICAL: { emoji: "\u{1F3C3}", label: "Physical", color: "bg-green-100 text-green-600" },
  COGNITIVE: { emoji: "\u{1F9E0}", label: "Cognitive", color: "bg-purple-100 text-purple-600" },
  SOCIAL: { emoji: "\u{1F46B}", label: "Social", color: "bg-blue-100 text-blue-600" },
  EMOTIONAL: { emoji: "\u2764\uFE0F", label: "Emotional", color: "bg-pink-100 text-pink-600" },
  LANGUAGE: { emoji: "\u{1F4AC}", label: "Language", color: "bg-amber-100 text-amber-600" },
  MOTOR: { emoji: "\u270B", label: "Motor", color: "bg-emerald-100 text-emerald-600" },
}

const STATUS_INFO: Record<string, { icon: typeof CheckCircle; color: string; label: string }> = {
  ACHIEVED: { icon: CheckCircle, color: "text-green-500", label: "Achieved" },
  IN_PROGRESS: { icon: Clock, color: "text-amber-500", label: "In Progress" },
  PENDING: { icon: Target, color: "text-gray-400", label: "Pending" },
  NOT_OBSERVED: { icon: Target, color: "text-gray-300", label: "Not Observed" },
}

export default function MilestonesPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<string | null>(null)

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
            <h1 className="text-xl font-bold text-gray-800">{t("milestones")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadMilestones} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const filtered = filter ? milestones.filter(m => m.category === filter) : milestones
  const achieved = milestones.filter(m => m.status === "ACHIEVED").length
  const inProgress = milestones.filter(m => m.status === "IN_PROGRESS").length
  const total = milestones.length

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("milestones")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.milestones")}</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-amber-400 to-orange-400 p-5 text-white">
        <div className="flex items-center gap-3"><Trophy className="size-8" /><div><h2 className="text-lg font-bold">{t("myAchievements")}</h2><p className="text-sm text-white/70">{t("progress.milestonesAchieved", { n: achieved, m: total })}</p></div></div>
        <div className="mt-3 grid grid-cols-3 gap-2">
          <div className="rounded-xl bg-white/10 p-2 text-center"><p className="text-lg font-bold">{achieved}</p><p className="text-[10px] text-white/70">{t("stats.achieved")}</p></div>
          <div className="rounded-xl bg-white/10 p-2 text-center"><p className="text-lg font-bold">{inProgress}</p><p className="text-[10px] text-white/70">{tc("inProgress")}</p></div>
          <div className="rounded-xl bg-white/10 p-2 text-center"><p className="text-lg font-bold">{total - achieved - inProgress}</p><p className="text-[10px] text-white/70">{t("filters.upcoming")}</p></div>
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1">
        <button onClick={() => setFilter(null)} aria-label={tc("all")} aria-pressed={filter === null} className={`shrink-0 rounded-full px-3 py-1.5 text-xs font-semibold ${!filter ? "bg-indigo-600 text-white" : "bg-gray-100 text-gray-600"}`}>{tc("all")}</button>
        {Object.entries(CATEGORY_INFO).map(([key, info]) => (
          <button key={key} onClick={() => setFilter(filter === key ? null : key)} aria-label={info.label} aria-pressed={filter === key} className={`shrink-0 rounded-full px-3 py-1.5 text-xs font-semibold ${filter === key ? "bg-indigo-600 text-white" : "bg-gray-100 text-gray-600"}`}>{info.emoji} {info.label}</button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Trophy className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noMilestones")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.teacherWillAdd")}</p>
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map(m => {
            const cat = CATEGORY_INFO[m.category] || { emoji: "\u2B50", label: m.category, color: "bg-gray-100 text-gray-600" }
            const status = STATUS_INFO[m.status] || STATUS_INFO.PENDING
            const StatusIcon = status.icon
            return (
              <div key={m.id} className={`nursery-card rounded-2xl bg-white p-4 ${m.status === "ACHIEVED" ? "ring-2 ring-green-300" : ""}`}>
                <div className="flex items-center gap-3">
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${cat.color}`}>{cat.emoji}</div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-800">{m.milestoneName}</p>
                    <p className="text-xs text-gray-500">{cat.label} {m.description ? `\u00B7 ${m.description}` : ""}</p>
                  </div>
                  <StatusIcon className={`size-5 shrink-0 ${status.color}`} />
                </div>
                {m.achievedDate && (
                  <p className="mt-2 text-[10px] text-gray-400">{new Date(m.achievedDate).toLocaleDateString()}</p>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
