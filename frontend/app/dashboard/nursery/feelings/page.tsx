"use client"

import { ArrowLeft, Heart } from "lucide-react"
import Link from "next/link"
import { useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi } from "@/lib/nursery-api"
import { useTranslations } from "next-intl"

const FEELINGS = [
  { emoji: "\u{1F60A}", label: "Happy", feeling: "HAPPY", color: "bg-yellow-100 border-yellow-300" },
  { emoji: "\u{1F60C}", label: "Calm", feeling: "CALM", color: "bg-blue-100 border-blue-300" },
  { emoji: "\u{1F929}", label: "Excited", feeling: "EXCITED", color: "bg-orange-100 border-orange-300" },
  { emoji: "\u{1F634}", label: "Tired", feeling: "TIRED", color: "bg-purple-100 border-purple-300" },
  { emoji: "\u{1F622}", label: "Sad", feeling: "SAD", color: "bg-gray-100 border-gray-300" },
  { emoji: "\u{1F624}", label: "Frustrated", feeling: "ANGRY", color: "bg-red-100 border-red-300" },
  { emoji: "\u{1F970}", label: "Loved", feeling: "PROUD", color: "bg-pink-100 border-pink-300" },
  { emoji: "\u{1F914}", label: "Worried", feeling: "WORRIED", color: "bg-indigo-100 border-indigo-300" },
]

export default function FeelingsCheckinPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [selected, setSelected] = useState<typeof FEELINGS[0] | null>(null)
  const [checked, setChecked] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleCheckin() {
    if (!selected || !user) return
    setSubmitting(true)
    setError(null)
    try {
      await nurseryApi.createFeelingsCheckin({
        studentId: user.id,
        classGroupId: user.classGroupId || "",
        feeling: selected.feeling as any,
        emoji: selected.emoji,
        note: null,
        checkinDate: new Date().toISOString().split("T")[0],
      })
      setChecked(true)
    } catch (err: any) {
      setError(err.message || tc("error"))
    }
    setSubmitting(false)
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}><ArrowLeft className="size-5 text-gray-600" /></Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("howFeeling")}</h1>
          <p className="text-sm text-gray-500">{t("tapFeeling")}</p>
        </div>
      </div>

      {!checked ? (
        <>
          {error && (
            <div className="nursery-card rounded-2xl bg-red-50 p-4 text-center">
              <p className="text-sm text-red-500">{error}</p>
              <button onClick={handleCheckin} className="mt-2 text-sm font-bold text-red-600 underline" aria-label={tc("retry")}>{tc("retry")}</button>
            </div>
          )}
          <div className="grid grid-cols-4 gap-3">
            {FEELINGS.map(f => (
              <button
                key={f.label}
                onClick={() => setSelected(f)}
                aria-label={f.label}
                aria-pressed={selected?.label === f.label}
                className={`nursery-card flex flex-col items-center gap-2 rounded-2xl border-2 p-4 transition-all ${selected?.label === f.label ? `${f.color} border-current scale-105` : "border-transparent bg-white"}`}
              >
                <span className="text-3xl">{f.emoji}</span>
                <span className="text-xs font-medium text-gray-700">{f.label}</span>
              </button>
            ))}
          </div>
          {selected && (
            <button onClick={handleCheckin} disabled={submitting} className="nursery-card w-full rounded-2xl bg-indigo-500 py-4 text-lg font-bold text-white hover:bg-indigo-600 disabled:opacity-50" aria-label={`${t("howFeeling")} - ${selected.label}`}>
              <Heart className="mr-2 inline size-5" /> {submitting ? tc("loading") : t("youFeel", { feeling: selected.label.toLowerCase() })}
            </button>
          )}
        </>
      ) : (
        <div className="nursery-card rounded-2xl bg-gradient-to-r from-pink-400 to-purple-400 p-8 text-center text-white">
          <p className="text-5xl">{selected?.emoji}</p>
          <p className="mt-4 text-xl font-bold">{t("youFeel", { feeling: selected?.label.toLowerCase() || "" })}</p>
          <p className="mt-2 text-sm text-white/70">{t("thankYou")}</p>
          <button onClick={() => { setChecked(false); setSelected(null) }} className="mt-6 rounded-xl bg-white/20 px-6 py-2 text-sm font-medium text-white hover:bg-white/30" aria-label={t("checkInAgain")}>{t("checkInAgain")}</button>
        </div>
      )}
    </div>
  )
}
