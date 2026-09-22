"use client"

import { ArrowLeft, FlaskConical, AlertCircle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SecondaryProblem } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"

const BUILTIN_EXPERIMENTS = [
  { id: "builtin-1", problemTitle: "Volcano Eruption", problemDescription: "Baking soda + vinegar = eruption", problemType: "ESSAY" as const, solution: "The chemical reaction between baking soda (sodium bicarbonate) and vinegar (acetic acid) produces carbon dioxide gas, creating the eruption effect.", options: null, correctAnswer: "", difficultyLevel: "BASIC" as const, category: "Practical Science" },
  { id: "builtin-2", problemTitle: "Growing Crystals", problemDescription: "Make crystals from salt or sugar", problemType: "ESSAY" as const, solution: "Dissolve salt in hot water to create a supersaturated solution. As the water cools, the dissolved salt crystallizes on the string.", options: null, correctAnswer: "", difficultyLevel: "BASIC" as const, category: "Practical Science" },
  { id: "builtin-3", problemTitle: "Density Tower", problemDescription: "Layer different liquids to see density differences", problemType: "ESSAY" as const, solution: "Different liquids have different densities. Honey is densest (bottom), then corn syrup, dish soap, water, and oil (top). Objects float at their density level.", options: null, correctAnswer: "", difficultyLevel: "INTERMEDIATE" as const, category: "Practical Science" },
]

export default function ScienceLabPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [apiExperiments, setApiExperiments] = useState<SecondaryProblem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (user?.classGroupId) {
      secondaryApi.getProblemsByClass(user.classGroupId)
        .then(data => setApiExperiments(data.filter(p => p.problemType === "LONG_ANSWER" || p.problemType === "SHORT_ANSWER")))
        .catch(() => setError(t("loadError")))
        .finally(() => setLoading(false))
    } else { setLoading(false) }
  }, [user, t])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertCircle className="mx-auto size-12 text-red-400" />
          <h3 className="mt-3 text-lg font-bold text-red-800">{tc("error.generic")}</h3>
          <p className="mt-1 text-sm text-red-600">{error}</p>
        </div>
      </div>
    )
  }

  const allExperiments = [
    ...BUILTIN_EXPERIMENTS,
    ...apiExperiments.map(p => ({ id: p.id, problemTitle: p.problemTitle, problemDescription: p.problemDescription, problemType: p.problemType as any, solution: p.solution || "", options: p.options, correctAnswer: p.correctAnswer, difficultyLevel: p.difficultyLevel as any, category: "" })),
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToSecondary")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div><h1 className="text-xl font-bold text-gray-900">{t("secondary.scienceLab")}</h1><p className="text-sm text-gray-500">{t("secondary.handsOnExperimentsAndDiscoveries")}</p></div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-green-600 to-emerald-500 p-5 text-white">
        <div className="flex items-center gap-3"><FlaskConical className="size-8" /><div><h2 className="text-lg font-bold">{t("secondary.labExperiments")}</h2><p className="text-sm text-white/70">{t("secondary.learnByDoingRealExperiments")}</p></div></div>
      </div>

      <div className="space-y-4">
        {allExperiments.map(exp => (
          <div key={exp.id} className="rounded-2xl border border-gray-100 bg-white p-5">
            <div className="flex items-center gap-3">
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-green-50"><FlaskConical className="size-6 text-green-600" /></div>
              <div><p className="font-semibold text-gray-900">{exp.problemTitle}</p><p className="text-xs text-gray-400">{exp.problemDescription}</p></div>
            </div>
            {exp.solution && (
              <div className="mt-4 rounded-xl bg-green-50 p-4"><p className="text-sm font-medium text-green-800">{t("secondary.howItWorks")}</p><p className="mt-1 text-sm text-green-700">{exp.solution}</p></div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
