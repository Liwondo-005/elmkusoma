"use client"

import { ArrowLeft, FlaskConical, Play, BookOpen } from "lucide-react"
import Link from "next/link"

const EXPERIMENTS = [
  { id: "1", title: "Volcano Eruption", desc: "Baking soda + vinegar = eruption", subject: "Science", steps: ["Add baking soda to a container", "Add a few drops of food coloring", "Pour in vinegar", "Watch the eruption!"], materials: ["Baking soda", "Vinegar", "Food coloring", "Container"] },
  { id: "2", title: "Growing Crystals", desc: "Make crystals from salt or sugar", subject: "Science", steps: ["Dissolve salt in hot water", "Hang a string in the water", "Wait 2-3 days", "Observe crystals forming"], materials: ["Salt or sugar", "Hot water", "String", "Glass jar"] },
]

export default function ScienceLabPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Science Lab</h1>
          <p className="text-sm text-gray-500">Hands-on experiments and discoveries</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-green-600 to-emerald-500 p-5 text-white">
        <div className="flex items-center gap-3">
          <FlaskConical className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Lab Experiments</h2>
            <p className="text-sm text-white/70">Learn by doing real experiments</p>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        {EXPERIMENTS.map(exp => (
          <div key={exp.id} className="rounded-2xl border border-gray-100 bg-white p-5">
            <div className="flex items-center gap-3">
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-green-50">
                <FlaskConical className="size-6 text-green-600" />
              </div>
              <div>
                <p className="font-semibold text-gray-900">{exp.title}</p>
                <p className="text-xs text-gray-400">{exp.desc}</p>
              </div>
            </div>

            <div className="mt-4">
              <p className="text-xs font-semibold text-gray-500">MATERIALS</p>
              <div className="mt-1 flex flex-wrap gap-1">
                {exp.materials.map(m => (
                  <span key={m} className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-medium text-gray-600">{m}</span>
                ))}
              </div>
            </div>

            <div className="mt-3">
              <p className="text-xs font-semibold text-gray-500">STEPS</p>
              <div className="mt-1 space-y-1">
                {exp.steps.map((step, i) => (
                  <div key={i} className="flex items-center gap-2 text-sm text-gray-600">
                    <span className="size-5 shrink-0 rounded-full bg-green-100 text-center text-[10px] font-bold leading-5 text-green-600">{i + 1}</span>
                    {step}
                  </div>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
