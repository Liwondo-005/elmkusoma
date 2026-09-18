"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Video, Radio, Clock, Calendar } from "lucide-react"
import Link from "next/link"

interface LiveClass {
  id: string
  title: string
  subjectName?: string
  scheduledAt: string
  durationMinutes: number
  status: string
  meetingUrl?: string
}

export default function NurseryLivePage() {
  const { user } = useRequireAuth()
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    fetch(`${process.env.NEXT_PUBLIC_API_URL || ""}/v1/student/dashboard/live-classes`, {
      headers: {
        "Authorization": `Bearer ${localStorage.getItem("elmkusoma_access_token")}`,
        "X-Institution-Id": localStorage.getItem("elmkusoma_institution_id") || "",
      },
    })
      .then(r => r.json())
      .then(d => setLiveClasses(d.data || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const liveNow = liveClasses.filter(c => c.status === "LIVE" || c.status === "IN_PROGRESS")
  const upcoming = liveClasses.filter(c => c.status === "SCHEDULED" || c.status === "PENDING")
  const past = liveClasses.filter(c => c.status === "COMPLETED" || c.status === "ENDED")

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Live Class</h1>
          <p className="text-sm text-gray-500">Join your class with your teacher</p>
        </div>
      </div>

      {/* Live Now */}
      {liveNow.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
            <Radio className="size-5 text-red-500" /> Live Now
          </h2>
          {liveNow.map(c => (
            <div key={c.id} className="nursery-card mb-3 rounded-2xl border-2 border-red-200 bg-red-50 p-4">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-red-100">
                  <Video className="size-6 text-red-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{c.title}</p>
                  {c.subjectName && <p className="text-xs text-gray-500">{c.subjectName}</p>}
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
                  Join Class
                </a>
              )}
            </div>
          ))}
        </section>
      )}

      {/* Upcoming */}
      {upcoming.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
            <Clock className="size-5 text-blue-500" /> Coming Soon
          </h2>
          {upcoming.map(c => (
            <div key={c.id} className="nursery-card mb-3 rounded-2xl bg-white p-4">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-50">
                  <Calendar className="size-6 text-blue-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{c.title}</p>
                  {c.subjectName && <p className="text-xs text-gray-500">{c.subjectName}</p>}
                  <p className="mt-1 text-xs text-gray-400">
                    {new Date(c.scheduledAt).toLocaleString()} • {c.durationMinutes} min
                  </p>
                </div>
              </div>
            </div>
          ))}
        </section>
      )}

      {/* Past */}
      {past.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">Past Classes</h2>
          {past.map(c => (
            <div key={c.id} className="nursery-card mb-3 rounded-2xl bg-gray-50 p-4 opacity-70">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-gray-100">
                  <Video className="size-6 text-gray-400" />
                </div>
                <div>
                  <p className="font-bold text-gray-600">{c.title}</p>
                  <p className="text-xs text-gray-400">Class ended</p>
                </div>
              </div>
            </div>
          ))}
        </section>
      )}

      {liveClasses.length === 0 && (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Video className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">No live classes yet!</h3>
          <p className="mt-1 text-sm text-gray-500">Your teacher will schedule live classes soon.</p>
        </div>
      )}
    </div>
  )
}
