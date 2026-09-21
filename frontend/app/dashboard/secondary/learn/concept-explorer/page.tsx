"use client"

import { ArrowLeft, Lightbulb, Search, AlertCircle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SecondaryConcept } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"

const BUILTIN_CONCEPTS = [
  { id: "builtin-1", conceptName: "Quadratic Equations", category: "Mathematics", conceptDescription: "Understanding x² + bx + c = 0. A quadratic equation is a polynomial equation of degree 2.", examples: "x² + 5x + 6 = 0, x² - 4 = 0", difficultyLevel: "INTERMEDIATE" as const },
  { id: "builtin-2", conceptName: "Photosynthesis", category: "Biology", conceptDescription: "How plants make food from sunlight. Plants use chlorophyll to capture light energy.", examples: "6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂", difficultyLevel: "BASIC" as const },
  { id: "builtin-3", conceptName: "Newton's First Law", category: "Physics", conceptDescription: "An object stays at rest or in motion unless acted upon by a net external force.", examples: "A book on a table stays still; a rolling ball continues until friction stops it.", difficultyLevel: "BASIC" as const },
]

export default function ConceptExplorerPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [apiConcepts, setApiConcepts] = useState<SecondaryConcept[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (user?.classGroupId) {
      secondaryApi.getConceptsByClass(user.classGroupId)
        .then(setApiConcepts)
        .catch(() => setError(t("loadError")))
        .finally(() => setLoading(false))
    } else { setLoading(false) }
  }, [user, t])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertCircle className="mx-auto size-12 text-red-400" />
          <h3 className="mt-3 text-lg font-bold text-red-800">{tc("common.error")}</h3>
          <p className="mt-1 text-sm text-red-600">{error}</p>
        </div>
      </div>
    )
  }

  const allConcepts = [
    ...BUILTIN_CONCEPTS,
    ...apiConcepts.map(c => ({ id: c.id, conceptName: c.conceptName, category: c.category || "", conceptDescription: c.conceptDescription, examples: c.examples || "", difficultyLevel: c.difficultyLevel })),
  ]

  const active = allConcepts.find(c => c.id === selected)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToLearn")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("secondary.conceptExplorer")}</h1>
          <p className="text-sm text-gray-500">{t("secondary.deepDiveIntoKeyConcepts")}</p>
        </div>
      </div>

      {selected && active ? (
        <div className="space-y-4">
          <button onClick={() => setSelected(null)} className="text-sm font-semibold text-indigo-600 hover:text-indigo-700" aria-label={t("secondary.backToConcepts")}>{t("secondary.backToConcepts")}</button>
          <div className="rounded-2xl border border-gray-100 bg-white p-6">
            <div className="flex items-center gap-2"><Lightbulb className="size-5 text-indigo-600" /><span className="text-xs font-semibold uppercase tracking-wider text-indigo-600">{active.category}</span></div>
            <h2 className="mt-2 text-lg font-bold text-gray-900">{active.conceptName}</h2>
            <p className="mt-1 text-sm text-gray-600">{active.conceptDescription}</p>
            {active.examples && (
              <div className="mt-4 rounded-xl bg-indigo-50 p-4"><p className="text-sm font-medium text-indigo-800">{t("secondary.examples")}</p><p className="mt-1 text-sm text-indigo-700">{active.examples}</p></div>
            )}
            <div className="mt-3"><span className="rounded-full bg-gray-100 px-2.5 py-1 text-[10px] font-bold text-gray-600">{active.difficultyLevel}</span></div>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {allConcepts.map(concept => (
            <button key={concept.id} onClick={() => setSelected(concept.id)} className="flex w-full items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 text-left transition-all hover:border-indigo-200 hover:shadow-sm" aria-label={`${concept.conceptName} - ${concept.category} ${concept.difficultyLevel}`}>
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-purple-50"><Lightbulb className="size-6 text-purple-600" /></div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{concept.conceptName}</p>
                <p className="text-xs text-gray-400">{concept.category} · {concept.difficultyLevel}</p>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
