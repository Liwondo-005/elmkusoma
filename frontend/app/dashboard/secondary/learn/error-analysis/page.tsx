"use client"

import { ArrowLeft, AlertTriangle, CheckCircle, RefreshCw } from "lucide-react"
import Link from "next/link"
import { useState } from "react"

const ERRORS = [
  {
    id: "1", subject: "Mathematics",
    wrong: "Solve: 3(x + 2) = 3x + 2",
    correct: "Solve: 3(x + 2) = 3x + 6",
    mistake: "Forgot to distribute 3 to both terms inside parentheses",
    lesson: "Always distribute to ALL terms: 3(x + 2) = 3·x + 3·2 = 3x + 6",
  },
  {
    id: "2", subject: "Science",
    wrong: "Plants get food from soil through roots",
    correct: "Plants make their own food through photosynthesis using sunlight, water, and CO₂",
    mistake: "Confused water/nutrient absorption with food production",
    lesson: "Plants absorb water and minerals through roots, but make food (glucose) through photosynthesis in leaves",
  },
  {
    id: "3", subject: "English",
    wrong: "He goed to school yesterday",
    correct: "He went to school yesterday",
    mistake: "Used incorrect past tense of 'go'",
    lesson: "Go → went (irregular verb). Not all verbs add -ed for past tense.",
  },
]

export default function ErrorAnalysisPage() {
  const [current, setCurrent] = useState(0)
  const [showLesson, setShowLesson] = useState(false)
  const error = ERRORS[current]

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Error Analysis</h1>
          <p className="text-sm text-gray-500">Learn from common mistakes</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-red-500 to-orange-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <AlertTriangle className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Mistakes Help You Learn</h2>
            <p className="text-sm text-white/70">Understanding errors makes you stronger</p>
          </div>
        </div>
      </div>

      {/* Error Card */}
      <div className="space-y-4">
        <div className="rounded-2xl border-2 border-red-200 bg-red-50 p-5">
          <p className="text-xs font-semibold text-red-600">Common Mistake</p>
          <p className="mt-2 font-medium text-gray-800">{error.wrong}</p>
          <p className="mt-2 text-sm text-red-600">Error: {error.mistake}</p>
        </div>

        <div className="rounded-2xl border-2 border-green-200 bg-green-50 p-5">
          <p className="text-xs font-semibold text-green-600">Correct Version</p>
          <p className="mt-2 font-medium text-gray-800">{error.correct}</p>
        </div>

        {!showLesson ? (
          <button onClick={() => setShowLesson(true)} className="flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 py-3 text-sm font-bold text-white hover:bg-indigo-700">
            <RefreshCw className="size-4" /> Show Me Why
          </button>
        ) : (
          <div className="rounded-2xl border border-indigo-200 bg-indigo-50 p-5">
            <p className="text-xs font-semibold text-indigo-600">Key Lesson</p>
            <p className="mt-2 text-sm text-indigo-800">{error.lesson}</p>
          </div>
        )}
      </div>

      {/* Navigation */}
      <div className="flex gap-3">
        <button
          onClick={() => { setCurrent(c => Math.max(0, c - 1)); setShowLesson(false) }}
          disabled={current === 0}
          className="flex-1 rounded-xl bg-gray-100 py-3 text-sm font-medium text-gray-600 hover:bg-gray-200 disabled:opacity-50"
        >
          Previous
        </button>
        <button
          onClick={() => { setCurrent(c => Math.min(ERRORS.length - 1, c + 1)); setShowLesson(false) }}
          disabled={current === ERRORS.length - 1}
          className="flex-1 rounded-xl bg-indigo-600 py-3 text-sm font-bold text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Next Error
        </button>
      </div>

      <p className="text-center text-sm text-gray-400">{current + 1} of {ERRORS.length}</p>
    </div>
  )
}
