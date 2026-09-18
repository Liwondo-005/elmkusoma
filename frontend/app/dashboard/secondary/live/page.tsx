"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type LiveClassSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { Video, ArrowLeft, Radio, Clock, Calendar } from "lucide-react"
import Link from "next/link"

export default function SecondaryLivePage() {
  const { user } = useRequireAuth()
  const [liveClasses, setLiveClasses] = useState<LiveClassSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    secondaryApi.getLiveClasses()
      .then(setLiveClasses)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  const liveNow = liveClasses.filter(c => c.status === "LIVE" || c.status === "IN_PROGRESS")
  const upcoming = liveClasses.filter(c => c.status === "SCHEDULED" || c.status === "PENDING")
  const past = liveClasses.filter(c => c.status === "COMPLETED" || c.status === "ENDED")

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Live Learning</h1>
          <p className="text-sm text-gray-500">Join live sessions with your teachers</p>
        </div>
      </div>

      {liveNow.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-gray-500">
            <Radio className="size-3.5 text-red-500" /> Live Now
          </h2>
          {liveNow.map(c => (
            <div key={c.id} className="mb-3 rounded-2xl border-2 border-red-200 bg-red-50 p-5">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-xl bg-red-100">
                  <Video className="size-6 text-red-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-900">{c.title}</p>
                  {c.subjectName && <p className="text-sm text-gray-500">{c.subjectName}</p>}
                </div>
                <span className="flex items-center gap-1 rounded-full bg-red-100 px-3 py-1 text-xs font-bold text-red-600">
                  <span className="size-2 animate-pulse rounded-full bg-red-500" /> LIVE
                </span>
              </div>
              {c.meetingUrl && (
                <a
                  href={c.meetingUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-3 block w-full rounded-xl bg-red-500 py-3 text-center text-sm font-bold text-white hover:bg-red-600"
                >
                  Join Session
                </a>
              )}
            </div>
          ))}
        </section>
      )}

      {upcoming.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-gray-500">
            <Clock className="size-3.5 text-blue-500" /> Upcoming
          </h2>
          {upcoming.map(c => (
            <div key={c.id} className="mb-3 rounded-2xl border border-gray-100 bg-white p-4">
              <div className="flex items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50">
                  <Calendar className="size-5 text-blue-500" />
                </div>
                <div className="flex-1">
                  <p className="font-semibold text-gray-900">{c.title}</p>
                  {c.subjectName && <p className="text-xs text-gray-400">{c.subjectName}</p>}
                  <p className="mt-0.5 text-xs text-gray-400">
                    {new Date(c.scheduledAt).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                    {' · '}{c.durationMinutes} min
                  </p>
                </div>
              </div>
            </div>
          ))}
        </section>
      )}

      {past.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Past Sessions</h2>
          {past.map(c => (
            <div key={c.id} className="mb-3 rounded-2xl border border-gray-100 bg-white p-4 opacity-60">
              <div className="flex items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-gray-100">
                  <Video className="size-5 text-gray-400" />
                </div>
                <div>
                  <p className="font-semibold text-gray-600">{c.title}</p>
                  <p className="text-xs text-gray-400">Session ended</p>
                </div>
              </div>
            </div>
          ))}
        </section>
      )}

      {liveClasses.length === 0 && (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <Video className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No live sessions</h3>
          <p className="mt-1 text-sm text-gray-500">Your teacher will schedule live sessions soon.</p>
        </div>
      )}
    </div>
  )
}
