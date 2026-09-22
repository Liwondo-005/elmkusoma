"use client"

import { useEffect, useState, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryActivity } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Headphones, Mic, Volume2 } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

export default function SpeakListenPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadActivities = useCallback(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    setLoading(true)
    setError(null)
    nurseryApi.getActivities(user.classGroupId)
      .then(data => setActivities((data || []).filter(a => a.activityType === "STORY" || a.activityType === "EDUCATIONAL")))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => { loadActivities() }, [loadActivities])

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-800">{t("speakAndListen")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadActivities} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const WORD_GAMES = [
    { word: "Apple", hint: "A red fruit", emoji: "\u{1F34E}" },
    { word: "Elephant", hint: "A big animal with a trunk", emoji: "\u{1F418}" },
    { word: "Sunshine", hint: "It shines in the sky", emoji: "\u2600\uFE0F" },
    { word: "Butterfly", hint: "It has colorful wings", emoji: "\u{1F98B}" },
    { word: "Banana", hint: "A yellow fruit monkeys love", emoji: "\u{1F34C}" },
    { word: "Fish", hint: "It swims in water", emoji: "\u{1F41F}" },
  ]

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("speakAndListen")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.speak")}</p>
        </div>
      </div>

      {activities.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">{t("listenAndLearn")}</h2>
          <div className="space-y-2">
            {activities.map(a => (
              <div key={a.id} className="nursery-card flex items-center gap-4 rounded-2xl bg-white p-4">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-100">
                  <Headphones className="size-6 text-blue-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{a.activityName}</p>
                  {a.description && <p className="text-xs text-gray-500">{a.description}</p>}
                </div>
                <Volume2 className="size-5 text-gray-400" />
              </div>
            ))}
          </div>
        </section>
      )}

      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">{t("guessWord")}</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {WORD_GAMES.map(w => (
            <div key={w.word} className="nursery-card rounded-2xl bg-white p-4 text-center">
              <p className="text-3xl">{w.emoji}</p>
              <p className="mt-2 text-sm text-gray-500">{w.hint}</p>
              <p className="mt-1 font-bold text-gray-800">{w.word}</p>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">{t("sayItLoud")}</h2>
        <div className="space-y-2">
          {[
            "Tell your teacher about your favorite animal",
            "Describe what you had for breakfast today",
            "Name three things that are blue",
            "Tell a story about a little bird",
          ].map((prompt, i) => (
            <div key={i} className="nursery-card flex items-center gap-3 rounded-2xl bg-white p-4">
              <div className="flex size-10 items-center justify-center rounded-full bg-green-100">
                <Mic className="size-5 text-green-500" />
              </div>
              <p className="text-sm font-medium text-gray-700">{prompt}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
