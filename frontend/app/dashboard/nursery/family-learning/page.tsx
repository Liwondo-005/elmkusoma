"use client"

import { ArrowLeft, Users, BookOpen, Star, Home, Leaf, Palette, Music, CheckCircle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryParentLearning } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"

const SUGGESTED_ACTIVITIES = [
  { icon: BookOpen, title: "Story Time with Family", desc: "Read a story with your parent or guardian", color: "bg-blue-100 text-blue-500", type: "READING" as const, tip: "Take turns reading pages. Ask questions about the story." },
  { icon: Star, title: "Count Together", desc: "Count objects around the house together", color: "bg-amber-100 text-amber-500", type: "COUNTING" as const, tip: "Count toys, fruits, or steps. Practice counting to 20." },
  { icon: Users, title: "Cook Together", desc: "Help prepare a simple meal with family", color: "bg-green-100 text-green-500", type: "COOKING" as const, tip: "Measure ingredients, wash vegetables, mix things together." },
  { icon: Leaf, title: "Garden Together", desc: "Plant a seed and watch it grow", color: "bg-emerald-100 text-emerald-500", type: "GARDENING" as const, tip: "Water the plant daily and draw how it grows each week." },
  { icon: Palette, title: "Art at Home", desc: "Draw or paint together with family", color: "bg-pink-100 text-pink-500", type: "CRAFTING" as const, tip: "Draw your family, your house, or your favourite animal." },
  { icon: Music, title: "Sing Songs", desc: "Sing nursery rhymes and songs together", color: "bg-purple-100 text-purple-500", type: "SINGING" as const, tip: "Clap along to the rhythm and make up actions." },
]

export default function ParentLearningPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const [apiActivities, setApiActivities] = useState<NurseryParentLearning[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.id) { setLoading(false); return }
    nurseryApi.getParentLearningByStudent(user.id)
      .then(setApiActivities)
      .catch(() => setError(tc("error")))
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <main role="main" className="mx-auto flex min-h-[50vh] max-w-4xl flex-col items-center justify-center p-4 text-center">
        <h2 className="text-lg font-bold text-gray-800">{tc("error")}</h2>
        <p className="mt-1 text-sm text-gray-500">{t("empty.default")}</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          aria-label={tc("retry")}
          className="mt-4 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white hover:bg-primary/90"
        >
          {tc("retry")}
        </button>
      </main>
    )
  }

  return (
    <main role="main" className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" aria-label={tc("back")} className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100"><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("familyLearning")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.family")}</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-pink-400 to-rose-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Home className="size-8" />
          <div>
            <h2 className="text-lg font-bold">{t("learnAtHome")}</h2>
            <p className="text-sm text-white/70">{t("homeActivities")}</p>
          </div>
        </div>
      </div>

      {apiActivities.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">{t("yourActivities")}</h2>
          {apiActivities.map(act => (
            <div key={act.id} className={`nursery-card rounded-2xl bg-white p-4 ${act.completionStatus === "COMPLETED" ? "ring-2 ring-green-400" : ""}`}>
              <div className="flex items-center gap-3">
                <div className={`flex size-10 items-center justify-center rounded-xl ${act.completionStatus === "COMPLETED" ? "bg-green-100" : "bg-indigo-100"}`}>
                  {act.completionStatus === "COMPLETED" ? <CheckCircle className="size-5 text-green-500" /> : <BookOpen className="size-5 text-indigo-500" />}
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{act.activityTitle}</p>
                  <p className="text-xs text-gray-500">{act.activityType} {act.parentName ? `• with ${act.parentName}` : ""}</p>
                </div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${act.completionStatus === "COMPLETED" ? "bg-green-100 text-green-600" : "bg-amber-100 text-amber-600"}`}>{act.completionStatus}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">{t("suggestedActivities")}</h2>
        {SUGGESTED_ACTIVITIES.map(item => (
          <div key={item.title} className="nursery-card rounded-2xl bg-white p-4">
            <div className="flex items-center gap-4">
              <div className={`flex size-12 items-center justify-center rounded-2xl ${item.color}`}><item.icon className="size-6" /></div>
              <div className="flex-1">
                <p className="font-bold text-gray-800">{item.title}</p>
                <p className="text-xs text-gray-500">{item.desc}</p>
              </div>
            </div>
            <div className="mt-3 rounded-xl bg-gray-50 px-3 py-2"><p className="text-xs text-gray-600">Tip: {item.tip}</p></div>
          </div>
        ))}
      </div>
    </main>
  )
}
