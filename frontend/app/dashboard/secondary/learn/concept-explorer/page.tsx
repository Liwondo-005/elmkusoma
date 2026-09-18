"use client"

import { ArrowLeft, Lightbulb, BookOpen, Search } from "lucide-react"
import Link from "next/link"
import { useState } from "react"

const CONCEPTS = [
  { id: "1", title: "Quadratic Equations", subject: "Mathematics", description: "Understanding x² + bx + c = 0", steps: ["Identify the equation type", "Find factors or use formula", "Solve for x", "Verify your answer"] },
  { id: "2", title: "Photosynthesis", subject: "Biology", description: "How plants make food from sunlight", steps: ["Light hits chlorophyll", "Water splits into H and O", "CO₂ is captured", "Glucose is produced"] },
  { id: "3", title: "Newton's First Law", subject: "Physics", description: "An object stays at rest or in motion unless acted upon", steps: ["Observe the object", "Identify forces", "Apply the law", "Predict the outcome"] },
]

export default function ConceptExplorerPage() {
  const [selected, setSelected] = useState<string | null>(null)
  const active = CONCEPTS.find(c => c.id === selected)

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Concept Explorer</h1>
          <p className="text-sm text-gray-500">Break down complex ideas step by step</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-amber-500 to-orange-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Lightbulb className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Understand. Think. Apply.</h2>
            <p className="text-sm text-white/70">Explore concepts by breaking them into steps</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {CONCEPTS.map(c => (
          <button
            key={c.id}
            onClick={() => setSelected(c.id)}
            className={`rounded-2xl border p-5 text-left transition-all ${
              selected === c.id ? "border-amber-300 bg-amber-50 shadow-md" : "border-gray-100 bg-white hover:border-amber-200"
            }`}
          >
            <span className="text-xs font-semibold text-amber-600">{c.subject}</span>
            <h3 className="mt-1 font-bold text-gray-900">{c.title}</h3>
            <p className="mt-1 text-sm text-gray-500">{c.description}</p>
          </button>
        ))}
      </div>

      {active && (
        <div className="rounded-2xl border border-amber-200 bg-white p-6">
          <h2 className="text-lg font-bold text-gray-900">{active.title}</h2>
          <p className="text-sm text-gray-500">{active.description}</p>
          <div className="mt-4 space-y-3">
            {active.steps.map((step, i) => (
              <div key={i} className="flex items-center gap-3">
                <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-amber-100 text-sm font-bold text-amber-600">
                  {i + 1}
                </div>
                <p className="text-sm text-gray-700">{step}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
