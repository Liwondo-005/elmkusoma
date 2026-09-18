"use client"

import { ArrowLeft, Heart } from "lucide-react"
import Link from "next/link"
import { useState } from "react"

const FEELINGS = [
  { emoji: "😊", label: "Happy", color: "bg-yellow-100 border-yellow-300" },
  { emoji: "😌", label: "Calm", color: "bg-blue-100 border-blue-300" },
  { emoji: "🤩", label: "Excited", color: "bg-orange-100 border-orange-300" },
  { emoji: "😴", label: "Tired", color: "bg-purple-100 border-purple-300" },
  { emoji: "😢", label: "Sad", color: "bg-gray-100 border-gray-300" },
  { emoji: "😤", label: "Frustrated", color: "bg-red-100 border-red-300" },
  { emoji: "🥰", label: "Loved", color: "bg-pink-100 border-pink-300" },
  { emoji: "🤔", label: "Confused", color: "bg-indigo-100 border-indigo-300" },
]

export default function FeelingsCheckinPage() {
  const [selected, setSelected] = useState<string | null>(null)
  const [checked, setChecked] = useState(false)

  function handleCheckin() {
    if (selected) setChecked(true)
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">How Are You Feeling?</h1>
          <p className="text-sm text-gray-500">Tap how you feel right now</p>
        </div>
      </div>

      {!checked ? (
        <>
          <div className="grid grid-cols-4 gap-3">
            {FEELINGS.map(f => (
              <button
                key={f.label}
                onClick={() => setSelected(f.label)}
                className={`nursery-card flex flex-col items-center gap-2 rounded-2xl border-2 p-4 transition-all ${
                  selected === f.label ? `${f.color} border-current scale-105` : "border-transparent bg-white"
                }`}
              >
                <span className="text-3xl">{f.emoji}</span>
                <span className="text-xs font-medium text-gray-700">{f.label}</span>
              </button>
            ))}
          </div>

          {selected && (
            <button
              onClick={handleCheckin}
              className="nursery-card w-full rounded-2xl bg-indigo-500 py-4 text-lg font-bold text-white hover:bg-indigo-600"
            >
              <Heart className="mr-2 inline size-5" /> I Feel {selected}
            </button>
          )}
        </>
      ) : (
        <div className="nursery-card rounded-2xl bg-gradient-to-r from-pink-400 to-purple-400 p-8 text-center text-white">
          <p className="text-5xl">{FEELINGS.find(f => f.label === selected)?.emoji}</p>
          <p className="mt-4 text-xl font-bold">You feel {selected?.toLowerCase()}</p>
          <p className="mt-2 text-sm text-white/70">
            Thank you for sharing! All feelings are okay. Your teacher cares about how you feel.
          </p>
          <button
            onClick={() => { setChecked(false); setSelected(null) }}
            className="mt-6 rounded-xl bg-white/20 px-6 py-2 text-sm font-medium text-white hover:bg-white/30"
          >
            Check In Again
          </button>
        </div>
      )}
    </div>
  )
}
