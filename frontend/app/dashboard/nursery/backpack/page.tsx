"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMilestone } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Backpack, Download, Star, Award, Heart, CheckCircle } from "lucide-react"
import Link from "next/link"

const CATEGORY_CONFIG: Record<string, { label: string; emoji: string; color: string }> = {
  PHYSICAL: { label: "Physical", emoji: "🏃", color: "bg-red-50 text-red-600" },
  COGNITIVE: { label: "Thinking", emoji: "🧠", color: "bg-blue-50 text-blue-600" },
  SOCIAL: { label: "Friends", emoji: "🤝", color: "bg-green-50 text-green-600" },
  EMOTIONAL: { label: "Feelings", emoji: "❤️", color: "bg-pink-50 text-pink-600" },
  LANGUAGE: { label: "Words", emoji: "💬", color: "bg-purple-50 text-purple-600" },
  MOTOR: { label: "Hands", emoji: "✋", color: "bg-amber-50 text-amber-600" },
}

export default function BackpackPage() {
  const { user } = useRequireAuth()
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)
  const [activeCategory, setActiveCategory] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.id) return
    nurseryApi.getMilestones(user.id)
      .then(setMilestones)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const achieved = milestones.filter(m => m.status === "ACHIEVED")
  const filtered = activeCategory
    ? achieved.filter(m => m.category === activeCategory)
    : achieved

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">My Backpack</h1>
          <p className="text-sm text-gray-500">All the amazing things you have achieved</p>
        </div>
      </div>

      {/* Achievement Summary */}
      <div className="nursery-card rounded-2xl bg-gradient-to-r from-amber-400 to-orange-500 p-5 text-white">
        <div className="flex items-center gap-4">
          <div className="flex size-14 items-center justify-center rounded-2xl bg-white/20">
            <Award className="size-7" />
          </div>
          <div>
            <p className="text-sm text-white/80">Total Achievements</p>
            <p className="text-3xl font-bold">{achieved.length}</p>
          </div>
        </div>
      </div>

      {/* Category Filters */}
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => setActiveCategory(null)}
          className={`rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
            !activeCategory ? "bg-primary text-white" : "bg-gray-100 text-gray-600"
          }`}
        >
          All ({achieved.length})
        </button>
        {Object.entries(CATEGORY_CONFIG).map(([key, config]) => {
          const count = achieved.filter(m => m.category === key).length
          if (count === 0) return null
          return (
            <button
              key={key}
              onClick={() => setActiveCategory(activeCategory === key ? null : key)}
              className={`rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
                activeCategory === key ? "bg-primary text-white" : "bg-gray-100 text-gray-600"
              }`}
            >
              {config.emoji} {config.label} ({count})
            </button>
          )
        })}
      </div>

      {/* Achievements Grid */}
      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="nursery-card rounded-2xl bg-white p-8 text-center">
            <Backpack className="mx-auto size-12 text-amber-400" />
            <h3 className="mt-3 text-lg font-bold text-gray-800">Your backpack is empty!</h3>
            <p className="mt-1 text-sm text-gray-500">Keep learning and your achievements will appear here.</p>
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {filtered.map((milestone) => {
              const cat = CATEGORY_CONFIG[milestone.category]
              return (
                <div key={milestone.id} className="nursery-card flex items-center gap-3 rounded-2xl bg-white p-4">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-50 text-2xl">
                    {cat?.emoji || "⭐"}
                  </div>
                  <div className="flex-1">
                    <p className="font-bold text-gray-800">{milestone.milestoneName}</p>
                    <p className="text-[10px] text-gray-500">{cat?.label}</p>
                    {milestone.achievedDate && (
                      <p className="mt-0.5 text-[10px] text-gray-400">
                        {new Date(milestone.achievedDate).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                  <CheckCircle className="size-5 text-green-500" />
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
