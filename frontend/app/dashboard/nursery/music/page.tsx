"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryActivity } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Music, Play, Pause } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

export default function MusicMovementPage() {
  const t = useTranslations("nursery")
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [loading, setLoading] = useState(true)
  const [playing, setPlaying] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    nurseryApi.getActivities(user.classGroupId)
      .then(data => setActivities((data || []).filter(a => a.activityType === "SONG")))
      .catch(() => setError("Failed to load songs"))
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  const MOVEMENTS = [
    { name: "Head, Shoulders, Knees & Toes", desc: "Touch each body part as you sing", emoji: "💃" },
    { name: "The Wheels on the Bus", desc: "Act out the bus movements", emoji: "🚌" },
    { name: "If You're Happy and You Know It", desc: "Clap your hands and stomp your feet", emoji: "😊" },
    { name: "Twinkle Twinkle Little Star", desc: "Wave your fingers like stars", emoji: "⭐" },
    { name: "The Hokey Pokey", desc: "Put your arms and legs in and out", emoji: "💃" },
    { name: "Row Row Row Your Boat", desc: "Rock side to side like you are in a boat", emoji: "🚣" },
  ]

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label="Back to nursery dashboard">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("musicAndMovement")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.music")}</p>
        </div>
      </div>

      {/* Songs from backend */}
      {activities.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">{t("songs")}</h2>
          <div className="space-y-2">
            {activities.map(a => (
              <div key={a.id} className="nursery-card flex items-center gap-4 rounded-2xl bg-white p-4">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-pink-100">
                  <Music className="size-6 text-pink-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{a.activityName}</p>
                  {a.description && <p className="text-xs text-gray-500">{a.description}</p>}
                </div>
                <button
                  onClick={() => setPlaying(playing === a.id ? null : a.id)}
                  aria-label={playing === a.id ? `Pause ${a.activityName}` : `Play ${a.activityName}`}
                  className="nursery-card flex size-10 items-center justify-center rounded-full bg-indigo-100"
                >
                  {playing === a.id ? <Pause className="size-5 text-indigo-600" /> : <Play className="size-5 text-indigo-600" />}
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Movement Activities */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">{t("movementActivities")}</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {MOVEMENTS.map(m => (
            <div key={m.name} className="nursery-card rounded-2xl bg-white p-4">
              <p className="text-2xl">{m.emoji}</p>
              <p className="mt-2 font-bold text-gray-800">{m.name}</p>
              <p className="text-xs text-gray-500">{m.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {activities.length === 0 && (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Music className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noSongs")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.teacherWillAdd")}</p>
        </div>
      )}
    </div>
  )
}
