"use client"

import { ArrowLeft, Globe, MapPin } from "lucide-react"
import Link from "next/link"
import { useState, useEffect, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryTanzaniaDiscovery } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { useTranslations } from "next-intl"

const BUILTIN_DISCOVERIES = [
  { id: "builtin-1", name: "Mount Kilimanjaro", desc: "The tallest mountain in Africa, in Tanzania", emoji: "\u{1F3D4}\uFE0F", facts: ["5,895 meters tall", "Has three volcanic cones", "Snow caps near the equator"] },
  { id: "builtin-2", name: "Serengeti", desc: "Famous for great wildebeest migration", emoji: "\u{1F981}", facts: ["Home to the Big Five", "Over 1.5 million wildebeest", "UNESCO World Heritage Site"] },
  { id: "builtin-3", name: "Zanzibar", desc: "Beautiful island with white sand beaches", emoji: "\u{1F3D6}\uFE0F", facts: ["Known as the Spice Island", "Famous for cloves and spices", "Historic Stone Town"] },
  { id: "builtin-4", name: "Ngorongoro Crater", desc: "World's largest inactive volcanic crater", emoji: "\u{1F30B}", facts: ["250 km\u00B2 wide", "Home to 30,000 animals", "One of the Seven Natural Wonders of Africa"] },
  { id: "builtin-5", name: "Lake Victoria", desc: "Largest lake in Africa", emoji: "\u{1F30A}", facts: ["Shared by Tanzania, Kenya, Uganda", "Source of the Nile River", "Home to many fish species"] },
  { id: "builtin-6", name: "Baobab Trees", desc: "Ancient trees found across Tanzania", emoji: "\u{1F333}", facts: ["Can live over 1,000 years", "Store water in their trunks", "Called the Tree of Life"] },
]

export default function TanzaniaDiscoveryPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [apiTopics, setApiTopics] = useState<NurseryTanzaniaDiscovery[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadTopics = useCallback(() => {
    const classId = user?.classGroupId
    if (!classId) { setLoading(false); return }
    setLoading(true)
    setError(null)
    nurseryApi.getTanzaniaTopics(classId)
      .then(setApiTopics)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => { loadTopics() }, [loadTopics])

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-800">{t("tanzaniaDiscovery")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadTopics} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const allDiscoveries = [
    ...BUILTIN_DISCOVERIES,
    ...apiTopics.map(topic => ({ id: topic.id, name: topic.topicTitle, desc: topic.topicDescription || "", emoji: "\u{1F1F9}\u{1F1FF}", facts: topic.funFacts ? topic.funFacts.split("\n") : [] })),
  ]

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("tanzaniaDiscovery")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.tanzania")}</p>
        </div>
      </div>

      {selected ? (
        <div className="space-y-4">
          <button onClick={() => setSelected(null)} className="text-sm font-semibold text-indigo-600 hover:text-indigo-700" aria-label={t("backToDiscoveries")}>{t("backToDiscoveries")}</button>
          {(() => {
            const item = allDiscoveries.find(d => d.id === selected)
            if (!item) return null
            return (
              <div className="nursery-card rounded-2xl bg-white p-6 text-center">
                <div className="text-6xl">{item.emoji}</div>
                <h2 className="mt-4 text-xl font-bold text-gray-800">{item.name}</h2>
                <p className="mt-2 text-sm text-gray-600">{item.desc}</p>
                <div className="mt-4 space-y-2">
                  {item.facts.map((fact, i) => (
                    <div key={i} className="rounded-xl bg-indigo-50 p-3 text-left">
                      <p className="text-sm text-indigo-800">{fact}</p>
                    </div>
                  ))}
                </div>
              </div>
            )
          })()}
        </div>
      ) : (
        <div className="space-y-3">
          {allDiscoveries.map(item => (
            <button key={item.id} onClick={() => setSelected(item.id)} aria-label={item.name} className="nursery-card flex w-full items-center gap-4 rounded-2xl bg-white p-4 text-left transition-all hover:shadow-lg">
              <div className="text-4xl">{item.emoji}</div>
              <div className="flex-1">
                <p className="font-bold text-gray-800">{item.name}</p>
                <p className="text-xs text-gray-500">{item.desc}</p>
              </div>
              <MapPin className="size-4 shrink-0 text-gray-400" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
