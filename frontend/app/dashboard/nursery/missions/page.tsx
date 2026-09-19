"use client"

import { ArrowLeft, Map, CheckCircle, Trophy } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMission } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"

const BUILTIN_MISSIONS = [
  { id: "builtin-1", title: "Color Hunt", desc: "Find 5 things around you that are red", emoji: "🔴", area: "Home", type: "HOME" as const },
  { id: "builtin-2", title: "Nature Walk", desc: "Find a leaf, a stone, and a flower outside", emoji: "🌿", area: "Outside", type: "NATURE" as const },
  { id: "builtin-3", title: "Kitchen Helper", desc: "Help a grown-up wash vegetables", emoji: "🥕", area: "Kitchen", type: "HOME" as const },
  { id: "builtin-4", title: "Animal Watch", desc: "Watch a bird or insect for 2 minutes", emoji: "🐦", area: "Garden", type: "NATURE" as const },
  { id: "builtin-5", title: "Shape Finder", desc: "Find 3 different shapes around your house", emoji: "⬛", area: "Home", type: "COMMUNITY" as const },
  { id: "builtin-6", title: "Sound Safari", desc: "Close your eyes and name 3 sounds you hear", emoji: "👂", area: "Anywhere", type: "CREATIVITY" as const },
]

export default function MissionsPage() {
  const { user } = useRequireAuth()
  const [apiMissions, setApiMissions] = useState<NurseryMission[]>([])
  const [completed, setCompleted] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const classId = user?.classGroupId
    if (!classId) { setLoading(false); return }
    nurseryApi.getMissions(classId)
      .then(setApiMissions)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const allMissions = [
    ...BUILTIN_MISSIONS,
    ...apiMissions.map(m => ({ id: m.id, title: m.missionTitle, desc: m.missionDescription || "", emoji: "🎯", area: m.missionType, type: m.missionType })),
  ]

  function toggleMission(id: string) {
    setCompleted(prev => { const next = new Set(prev); if (next.has(id)) next.delete(id); else next.add(id); return next })
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100"><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Real-World Missions</h1>
          <p className="text-sm text-gray-500">Complete adventures in the real world</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-green-400 to-emerald-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Trophy className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Mission Progress</h2>
            <p className="text-sm text-white/70">{completed.size} of {allMissions.length} missions done</p>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        {allMissions.map(mission => (
          <button key={mission.id} onClick={() => toggleMission(mission.id)} className={`nursery-card flex w-full items-center gap-4 rounded-2xl bg-white p-4 text-left transition-all ${completed.has(mission.id) ? "ring-2 ring-green-400" : ""}`}>
            <div className="text-3xl">{mission.emoji}</div>
            <div className="flex-1">
              <p className={`font-bold ${completed.has(mission.id) ? "text-green-600 line-through" : "text-gray-800"}`}>{mission.title}</p>
              <p className="text-xs text-gray-500">{mission.desc}</p>
            </div>
            {completed.has(mission.id) && <CheckCircle className="size-5 shrink-0 text-green-500" />}
          </button>
        ))}
      </div>
    </div>
  )
}
