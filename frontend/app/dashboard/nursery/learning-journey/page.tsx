"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMilestone } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { Star, CheckCircle, Clock, Trophy, ArrowLeft } from "lucide-react"
import Link from "next/link"

const CATEGORIES = [
  { key: "PHYSICAL", label: "Physical", icon: "🏃", color: "bg-red-50 border-red-200 text-red-700" },
  { key: "COGNITIVE", label: "Thinking", icon: "🧠", color: "bg-blue-50 border-blue-200 text-blue-700" },
  { key: "SOCIAL", label: "Friends", icon: "🤝", color: "bg-green-50 border-green-200 text-green-700" },
  { key: "EMOTIONAL", label: "Feelings", icon: "❤️", color: "bg-pink-50 border-pink-200 text-pink-700" },
  { key: "LANGUAGE", label: "Words", icon: "💬", color: "bg-purple-50 border-purple-200 text-purple-700" },
  { key: "MOTOR", label: "Hands", icon: "✋", color: "bg-amber-50 border-amber-200 text-amber-700" },
]

export default function LearningJourneyPage() {
  const { user } = useRequireAuth()
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)
  const [activeFilter, setActiveFilter] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.id) return
    nurseryApi.getMilestones(user.id)
      .then(setMilestones)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const filtered = activeFilter
    ? milestones.filter(m => m.category === activeFilter)
    : milestones

  const achieved = filtered.filter(m => m.status === "ACHIEVED").length
  const total = filtered.length
  const progress = total > 0 ? Math.round((achieved / total) * 100) : 0

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">My Learning Journey</h1>
          <p className="text-sm text-gray-500">Track how you grow and learn</p>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="nursery-card rounded-2xl bg-gradient-to-r from-purple-500 to-pink-500 p-5 text-white">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-white/80">Overall Progress</p>
            <p className="text-3xl font-bold">{progress}%</p>
          </div>
          <Trophy className="size-10 text-white/80" />
        </div>
        <div className="mt-3 h-3 overflow-hidden rounded-full bg-white/20">
          <div className="h-full rounded-full bg-white transition-all" style={{ width: `${progress}%` }} />
        </div>
        <p className="mt-2 text-xs text-white/70">{achieved} of {total} milestones achieved</p>
      </div>

      {/* Category Filters */}
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => setActiveFilter(null)}
          className={`rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
            !activeFilter ? "bg-primary text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"
          }`}
        >
          All
        </button>
        {CATEGORIES.map(cat => (
          <button
            key={cat.key}
            onClick={() => setActiveFilter(activeFilter === cat.key ? null : cat.key)}
            className={`rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
              activeFilter === cat.key ? "bg-primary text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            {cat.icon} {cat.label}
          </button>
        ))}
      </div>

      {/* Milestones List */}
      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="nursery-card rounded-2xl bg-white p-8 text-center">
            <Star className="mx-auto size-12 text-yellow-400" />
            <h3 className="mt-3 text-lg font-bold text-gray-800">No milestones yet!</h3>
            <p className="mt-1 text-sm text-gray-500">Your teacher will add milestones as you learn and grow.</p>
          </div>
        ) : (
          filtered.map((milestone) => {
            const cat = CATEGORIES.find(c => c.key === milestone.category)
            const isAchieved = milestone.status === "ACHIEVED"
            return (
              <div key={milestone.id} className={`nursery-card flex items-center gap-4 rounded-2xl border-2 p-4 ${
                isAchieved ? "border-green-200 bg-green-50" : "border-gray-100 bg-white"
              }`}>
                <span className="text-3xl">{cat?.icon || "⭐"}</span>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{milestone.milestoneName}</p>
                  {milestone.description && (
                    <p className="mt-0.5 text-xs text-gray-500">{milestone.description}</p>
                  )}
                  {milestone.expectedAgeMonths && (
                    <p className="mt-1 text-[10px] text-gray-400">
                      Expected at {milestone.expectedAgeMonths} months
                    </p>
                  )}
                </div>
                <div className="flex flex-col items-center gap-1">
                  {isAchieved ? (
                    <>
                      <CheckCircle className="size-6 text-green-500" />
                      <span className="text-[10px] font-bold text-green-600">Achieved!</span>
                    </>
                  ) : milestone.status === "IN_PROGRESS" ? (
                    <>
                      <Clock className="size-6 text-yellow-500" />
                      <span className="text-[10px] font-bold text-yellow-600">Working on it</span>
                    </>
                  ) : (
                    <>
                      <Star className="size-6 text-gray-300" />
                      <span className="text-[10px] font-bold text-gray-400">Upcoming</span>
                    </>
                  )}
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
