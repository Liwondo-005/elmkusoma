"use client"

import { ArrowLeft, AlertTriangle, CheckCircle, RefreshCw, AlertCircle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SecondaryError } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"

const BUILTIN_ERRORS = [
  { id: "builtin-1", errorTitle: "Distributive Property Mistake", errorDescription: "Forgetting to distribute to all terms", incorrectExample: "3(x + 2) = 3x + 2", correctExample: "3(x + 2) = 3x + 6", explanation: "Always distribute to ALL terms inside parentheses: 3·x + 3·2 = 3x + 6", category: "Algebra", frequency: "COMMON" as const },
  { id: "builtin-2", errorTitle: "Confusing Mass and Weight", errorDescription: "Using mass and weight interchangeably", incorrectExample: "My weight is 70 kg", correctExample: "My mass is 70 kg; my weight is about 686 N", explanation: "Mass is measured in kg (amount of matter). Weight is a force measured in Newtons (W = mg).", category: "Physics", frequency: "COMMON" as const },
  { id: "builtin-3", errorTitle: "Subject-Verb Agreement", errorDescription: "Using wrong verb form with collective nouns", incorrectExample: "The team are playing well", correctExample: "The team is playing well", explanation: "Collective nouns (team, family, group) typically take singular verbs in American English.", category: "English", frequency: "OCCASIONAL" as const },
]

export default function ErrorAnalysisPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [apiErrors, setApiErrors] = useState<SecondaryError[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (user?.classGroupId) {
      secondaryApi.getErrorsByClass(user.classGroupId)
        .then(setApiErrors)
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
          <h3 className="mt-3 text-lg font-bold text-red-800">{tc("error.generic")}</h3>
          <p className="mt-1 text-sm text-red-600">{error}</p>
        </div>
      </div>
    )
  }

  const allErrors = [
    ...BUILTIN_ERRORS,
    ...apiErrors.map(e => ({ id: e.id, errorTitle: e.errorTitle, errorDescription: e.errorDescription, incorrectExample: e.incorrectExample || "", correctExample: e.correctExample || "", explanation: e.explanation, category: e.category || "", frequency: e.frequency })),
  ]

  const active = allErrors.find(e => e.id === selected)

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary/learn" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToLearn")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("secondary.errorAnalysis")}</h1>
          <p className="text-sm text-gray-500">{t("secondary.learnFromCommonMistakes")}</p>
        </div>
      </div>

      {selected && active ? (
        <div className="space-y-4">
          <button onClick={() => setSelected(null)} className="text-sm font-semibold text-indigo-600 hover:text-indigo-700" aria-label={t("secondary.backToErrors")}>{t("secondary.backToErrors")}</button>
          <div className="rounded-2xl border border-gray-100 bg-white p-6">
            <div className="flex items-center gap-2"><AlertTriangle className="size-5 text-amber-600" /><span className="text-xs font-semibold uppercase tracking-wider text-amber-600">{active.frequency}</span></div>
            <h2 className="mt-2 text-lg font-bold text-gray-900">{active.errorTitle}</h2>
            <p className="mt-1 text-sm text-gray-600">{active.errorDescription}</p>
            {active.incorrectExample && (
              <div className="mt-4 rounded-xl bg-red-50 p-4"><p className="text-sm font-medium text-red-800">{t("secondary.commonMistake")}</p><p className="mt-1 text-sm text-red-700 font-mono">{active.incorrectExample}</p></div>
            )}
            {active.correctExample && (
              <div className="mt-3 rounded-xl bg-green-50 p-4"><p className="text-sm font-medium text-green-800">{t("secondary.correctApproach")}</p><p className="mt-1 text-sm text-green-700 font-mono">{active.correctExample}</p></div>
            )}
            <div className="mt-3 rounded-xl bg-blue-50 p-4"><p className="text-sm font-medium text-blue-800">{t("secondary.why")}</p><p className="mt-1 text-sm text-blue-700">{active.explanation}</p></div>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {allErrors.map(err => (
            <button key={err.id} onClick={() => setSelected(err.id)} className="flex w-full items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4 text-left transition-all hover:border-indigo-200 hover:shadow-sm" aria-label={`${err.errorTitle} - ${err.category} ${err.frequency}`}>
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-amber-50"><AlertTriangle className="size-6 text-amber-600" /></div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{err.errorTitle}</p>
                <p className="text-xs text-gray-400">{err.category} · {err.frequency}</p>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
