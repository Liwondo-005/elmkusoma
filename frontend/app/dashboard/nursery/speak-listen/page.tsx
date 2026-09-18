"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryActivity } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Headphones, Mic, Volume2 } from "lucide-react"
import Link from "next/link"

export default function SpeakListenPage() {
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    nurseryApi.getActivities(user.classGroupId)
      .then(data => setActivities((data || []).filter(a => a.type === "STORY" || a.type === "EDUCATIONAL")))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const WORD_GAMES = [
    { word: "Apple", hint: "A red fruit", emoji: "🍎" },
    { word: "Elephant", hint: "A big animal with a trunk", emoji: "🐘" },
    { word: "Sunshine", hint: "It shines in the sky", emoji: "☀️" },
    { word: "Butterfly", hint: "It has colorful wings", emoji: "🦋" },
    { word: "Banana", hint: "A yellow fruit monkeys love", emoji: "🍌" },
    { word: "Fish", hint: "It swims in water", emoji: "🐟" },
  ]

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Speak & Listen</h1>
          <p className="text-sm text-gray-500">Practice speaking and listening</p>
        </div>
      </div>

      {/* Listening Activities */}
      {activities.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">Listen & Learn</h2>
          <div className="space-y-2">
            {activities.map(a => (
              <div key={a.id} className="nursery-card flex items-center gap-4 rounded-2xl bg-white p-4">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-100">
                  <Headphones className="size-6 text-blue-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{a.title}</p>
                  {a.description && <p className="text-xs text-gray-500">{a.description}</p>}
                </div>
                <Volume2 className="size-5 text-gray-400" />
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Word Games */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">Guess the Word</h2>
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

      {/* Speaking Prompts */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">Say It Out Loud</h2>
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
