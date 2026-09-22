"use client"

import { ArrowLeft, Footprints, Timer } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"

const BREAKS = [
  { name: "Jumping Jacks", duration: 30, emoji: " jumping" },
  { name: "Run in Place", duration: 20, emoji: "🏃" },
  { name: "Stretch Up High", duration: 15, emoji: "🙆" },
  { name: "Toe Touches", duration: 20, emoji: "🧎" },
  { name: "Dance Party", duration: 30, emoji: "💃" },
  { name: "Hop on One Foot", duration: 15, emoji: "🦩" },
]

export default function MovementBreaksPage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const [active, setActive] = useState<number | null>(null)
  const [timer, setTimer] = useState(0)

  useEffect(() => {
    if (active === null || timer <= 0) return
    const interval = setInterval(() => setTimer(t => t - 1), 1000)
    return () => clearInterval(interval)
  }, [active, timer])

  useEffect(() => {
    if (timer === 0 && active !== null) {
      setTimeout(() => setActive(null), 1000)
    }
  }, [timer, active])

  function startBreak(index: number) {
    setActive(index)
    setTimer(BREAKS[index].duration)
  }

  return (
    <main role="main" className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" aria-label={tc("back")} className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("movementBreaks")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.movement")}</p>
        </div>
      </div>

      {active !== null && (
        <div className="nursery-card rounded-2xl bg-gradient-to-r from-green-400 to-emerald-400 p-8 text-center text-white">
          <p className="text-5xl">{BREAKS[active].emoji}</p>
          <p className="mt-4 text-2xl font-bold">{BREAKS[active].name}</p>
          <p className="mt-2 text-5xl font-bold">{timer}s</p>
          <div
            role="progressbar"
            aria-valuenow={timer}
            aria-valuemax={BREAKS[active].duration}
            aria-valuemin={0}
            aria-label={`${BREAKS[active].name} ${timer} ${t("seconds")}`}
            className="mt-4 h-3 overflow-hidden rounded-full bg-white/20"
          >
            <div
              className="h-full rounded-full bg-white transition-all"
              style={{ width: `${(timer / BREAKS[active].duration) * 100}%` }}
            />
          </div>
          <button
            onClick={() => { setActive(null); setTimer(0) }}
            aria-label={t("stop")}
            className="mt-4 rounded-xl bg-white/20 px-6 py-2 text-sm font-medium text-white hover:bg-white/30"
          >
            {t("stop")}
          </button>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        {BREAKS.map((b, i) => (
          <button
            key={b.name}
            onClick={() => startBreak(i)}
            disabled={active !== null}
            aria-label={`${b.name} - ${b.duration} ${t("seconds")}`}
            className={`nursery-card flex items-center gap-4 rounded-2xl bg-white p-4 text-left transition-all ${
              active !== null ? "opacity-50" : "hover:scale-105"
            }`}
          >
            <span className="text-3xl">{b.emoji}</span>
            <div className="flex-1">
              <p className="font-bold text-gray-800">{b.name}</p>
              <div className="flex items-center gap-1 text-xs text-gray-500">
                <Timer className="size-3" /> {b.duration} {t("seconds")}
              </div>
            </div>
          </button>
        ))}
      </div>
    </main>
  )
}
