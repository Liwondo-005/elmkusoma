"use client"

import { ArrowLeft, Star, CheckCircle, Trophy } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryDailyQuest } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"

const BUILTIN_QUESTS = [
  { id: "builtin-1", title: "Read a Story", desc: "Read or listen to one story today", points: 10, type: "READING" as const },
  { id: "builtin-2", title: "Count to 10", desc: "Count from 1 to 10 out loud", points: 5, type: "MATH" as const },
  { id: "builtin-3", title: "Draw a Picture", desc: "Draw something you like", points: 10, type: "ART" as const },
  { id: "builtin-4", title: "Sing a Song", desc: "Sing your favorite song", points: 5, type: "LANGUAGE" as const },
  { id: "builtin-5", title: "Help Someone", desc: "Do something kind for a friend", points: 10, type: "PHYSICAL" as const },
]

export default function DailyQuestPage() {
  const { user } = useRequireAuth()
  const [apiQuests, setApiQuests] = useState<NurseryDailyQuest[]>([])
  const [completed, setCompleted] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const classId = user?.classGroupId
    if (!classId) { setLoading(false); return }
    nurseryApi.getDailyQuests(classId)
      .then(setApiQuests)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const allQuests = [
    ...BUILTIN_QUESTS,
    ...apiQuests.map(q => ({ id: q.id, title: q.questTitle, desc: q.questDescription || "", points: q.rewardPoints, type: q.questType })),
  ]

  function toggleQuest(id: string) {
    setCompleted(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const totalPoints = allQuests.filter(q => completed.has(q.id)).reduce((sum, q) => sum + q.points, 0)
  const maxPoints = allQuests.reduce((sum, q) => sum + q.points, 0)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100"><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Daily Quest</h1>
          <p className="text-sm text-gray-500">Complete fun challenges today</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-amber-400 to-orange-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Trophy className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Today&apos;s Points</h2>
            <p className="text-sm text-white/70">{totalPoints} / {maxPoints} points earned</p>
          </div>
        </div>
        <div className="mt-3 h-3 overflow-hidden rounded-full bg-white/30">
          <div className="h-full rounded-full bg-white transition-all" style={{ width: `${maxPoints > 0 ? (totalPoints / maxPoints) * 100 : 0}%` }} />
        </div>
      </div>

      <div className="space-y-3">
        {allQuests.map(quest => (
          <button key={quest.id} onClick={() => toggleQuest(quest.id)} className={`nursery-card flex w-full items-center gap-4 rounded-2xl bg-white p-4 text-left transition-all ${completed.has(quest.id) ? "ring-2 ring-green-400" : ""}`}>
            <div className={`flex size-12 items-center justify-center rounded-2xl ${completed.has(quest.id) ? "bg-green-100" : "bg-amber-100"}`}>
              {completed.has(quest.id) ? <CheckCircle className="size-6 text-green-500" /> : <Star className="size-6 text-amber-500" />}
            </div>
            <div className="flex-1">
              <p className={`font-bold ${completed.has(quest.id) ? "text-green-600 line-through" : "text-gray-800"}`}>{quest.title}</p>
              <p className="text-xs text-gray-500">{quest.desc}</p>
            </div>
            <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${completed.has(quest.id) ? "bg-green-100 text-green-600" : "bg-amber-100 text-amber-600"}`}>+{quest.points}</span>
          </button>
        ))}
      </div>
    </div>
  )
}
