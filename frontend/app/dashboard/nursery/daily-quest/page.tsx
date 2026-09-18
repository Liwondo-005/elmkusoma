"use client"

import { ArrowLeft, Star, CheckCircle } from "lucide-react"
import Link from "next/link"
import { useState } from "react"

const QUESTS = [
  { id: "1", title: "Read a Story", desc: "Read or listen to one story today", points: 10, done: false },
  { id: "2", title: "Count to 10", desc: "Count from 1 to 10 out loud", points: 5, done: false },
  { id: "3", title: "Draw a Picture", desc: "Draw something you like", points: 10, done: false },
  { id: "4", title: "Sing a Song", desc: "Sing your favorite song", points: 5, done: false },
  { id: "5", title: "Help Someone", desc: "Do something kind for a friend", points: 10, done: false },
]

export default function DailyQuestPage() {
  const [quests, setQuests] = useState(QUESTS)
  const totalPoints = quests.filter(q => q.done).reduce((sum, q) => sum + q.points, 0)
  const maxPoints = quests.reduce((sum, q) => sum + q.points, 0)

  function toggleQuest(id: string) {
    setQuests(prev => prev.map(q => q.id === id ? { ...q, done: !q.done } : q))
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Daily Quest</h1>
          <p className="text-sm text-gray-500">Complete today&apos;s missions</p>
        </div>
      </div>

      {/* Points Banner */}
      <div className="nursery-card rounded-2xl bg-gradient-to-r from-amber-400 to-orange-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Star className="size-8" />
          <div>
            <p className="text-2xl font-bold">{totalPoints} / {maxPoints} Points</p>
            <p className="text-sm text-white/70">{quests.filter(q => q.done).length} of {quests.length} quests done</p>
          </div>
        </div>
        <div className="mt-3 h-3 overflow-hidden rounded-full bg-white/20">
          <div className="h-full rounded-full bg-white transition-all" style={{ width: `${(totalPoints / maxPoints) * 100}%` }} />
        </div>
      </div>

      {/* Quests */}
      <div className="space-y-2">
        {quests.map(q => (
          <button
            key={q.id}
            onClick={() => toggleQuest(q.id)}
            className={`nursery-card flex w-full items-center gap-4 rounded-2xl p-4 text-left transition-all ${
              q.done ? "bg-green-50" : "bg-white"
            }`}
          >
            <div className={`flex size-10 items-center justify-center rounded-full ${
              q.done ? "bg-green-200" : "bg-amber-100"
            }`}>
              {q.done ? <CheckCircle className="size-5 text-green-600" /> : <Star className="size-5 text-amber-500" />}
            </div>
            <div className="flex-1">
              <p className={`font-bold ${q.done ? "text-green-700 line-through" : "text-gray-800"}`}>{q.title}</p>
              <p className="text-xs text-gray-500">{q.desc}</p>
            </div>
            <span className="text-xs font-bold text-amber-500">+{q.points}</span>
          </button>
        ))}
      </div>

      {totalPoints === maxPoints && (
        <div className="nursery-card rounded-2xl bg-gradient-to-r from-green-400 to-emerald-400 p-5 text-center text-white">
          <p className="text-2xl">🎉</p>
          <p className="mt-2 text-lg font-bold">All Quests Complete!</p>
          <p className="text-sm text-white/70">You earned {totalPoints} points today!</p>
        </div>
      )}
    </div>
  )
}
