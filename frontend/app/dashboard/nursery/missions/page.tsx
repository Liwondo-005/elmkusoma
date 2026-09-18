"use client"

import { ArrowLeft, Map, CheckCircle } from "lucide-react"
import Link from "next/link"
import { useState } from "react"

const MISSIONS = [
  { id: "1", title: "Color Hunt", desc: "Find 5 things around you that are red", emoji: "🔴", area: "Home" },
  { id: "2", title: "Nature Walk", desc: "Find a leaf, a stone, and a flower outside", emoji: "🌿", area: "Outside" },
  { id: "3", title: "Kitchen Helper", desc: "Help a grown-up wash vegetables", emoji: "🥕", area: "Kitchen" },
  { id: "4", title: "Animal Watch", desc: "Watch a bird or insect for 2 minutes", emoji: "🐦", area: "Garden" },
  { id: "5", title: "Shape Finder", desc: "Find 3 different shapes around your house", emoji: "⬛", area: "Home" },
  { id: "6", title: "Sound Safari", desc: "Close your eyes and name 3 sounds you hear", emoji: "👂", area: "Anywhere" },
]

export default function RealWorldMissionsPage() {
  const [completed, setCompleted] = useState<string[]>([])

  function toggleMission(id: string) {
    setCompleted(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Real-World Missions</h1>
          <p className="text-sm text-gray-500">Explore the world around you</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Map className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Mission Status</h2>
            <p className="text-sm text-white/70">{completed.length} of {MISSIONS.length} missions done</p>
          </div>
        </div>
        <div className="mt-3 h-3 overflow-hidden rounded-full bg-white/20">
          <div className="h-full rounded-full bg-white transition-all" style={{ width: `${(completed.length / MISSIONS.length) * 100}%` }} />
        </div>
      </div>

      <div className="space-y-3">
        {MISSIONS.map(m => (
          <button
            key={m.id}
            onClick={() => toggleMission(m.id)}
            className={`nursery-card flex w-full items-center gap-4 rounded-2xl p-4 text-left transition-all ${
              completed.includes(m.id) ? "bg-green-50" : "bg-white"
            }`}
          >
            <span className="text-3xl">{m.emoji}</span>
            <div className="flex-1">
              <p className={`font-bold ${completed.includes(m.id) ? "text-green-700" : "text-gray-800"}`}>{m.title}</p>
              <p className="text-xs text-gray-500">{m.desc}</p>
              <p className="mt-1 text-[10px] font-medium text-gray-400">📍 {m.area}</p>
            </div>
            {completed.includes(m.id) && <CheckCircle className="size-6 text-green-500" />}
          </button>
        ))}
      </div>

      {completed.length === MISSIONS.length && (
        <div className="nursery-card rounded-2xl bg-gradient-to-r from-yellow-400 to-orange-400 p-5 text-center text-white">
          <p className="text-2xl">🏆</p>
          <p className="mt-2 text-lg font-bold">All Missions Complete!</p>
          <p className="text-sm text-white/70">You are a real-world explorer!</p>
        </div>
      )}
    </div>
  )
}
