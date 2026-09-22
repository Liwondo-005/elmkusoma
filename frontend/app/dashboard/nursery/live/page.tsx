"use client"

import { useEffect, useState, useCallback } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryLiveClass } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Video, Radio, Clock, Calendar } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

export default function NurseryLivePage() {
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const { user } = useRequireAuth()
  const [liveClasses, setLiveClasses] = useState<NurseryLiveClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadLiveClasses = useCallback(() => {
    if (!user) return
    setLoading(true)
    setError(null)
    nurseryApi.getLiveClasses(user.classGroupId || "")
      .then(setLiveClasses)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => { loadLiveClasses() }, [loadLiveClasses])

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="flex items-center gap-3">
          <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
            <ArrowLeft className="size-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-800">{t("liveClass")}</h1>
          </div>
        </div>
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <button onClick={loadLiveClasses} className="mt-4 rounded-xl bg-primary px-6 py-2 text-sm font-bold text-white hover:bg-primary/90" aria-label={tc("retry")}>{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const liveNow = liveClasses.filter(c => c.status === "LIVE")
  const upcoming = liveClasses.filter(c => c.status === "SCHEDULED")
  const past = liveClasses.filter(c => c.status === "COMPLETED" || c.status === "CANCELLED")

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToList")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("liveClass")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.live")}</p>
        </div>
      </div>

      {liveNow.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
            <Radio className="size-5 text-red-500" /> {t("liveNow")}
          </h2>
          {liveNow.map(c => (
            <div key={c.id} className="nursery-card mb-3 rounded-2xl border-2 border-red-200 bg-red-50 p-4">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-red-100">
                  <Video className="size-6 text-red-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{c.title}</p>
                  {c.description && <p className="text-xs text-gray-500">{c.description}</p>}
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
                  aria-label={`${t("joinClass")} - ${c.title}`}
                >
                  {t("joinClass")}
                </a>
              )}
            </div>
          ))}
        </section>
      )}

      {upcoming.length > 0 && (
        <section>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
            <Clock className="size-5 text-blue-500" /> {t("comingSoon")}
          </h2>
          {upcoming.map(c => (
            <div key={c.id} className="nursery-card mb-3 rounded-2xl bg-white p-4">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-50">
                  <Calendar className="size-6 text-blue-500" />
                </div>
                <div className="flex-1">
                  <p className="font-bold text-gray-800">{c.title}</p>
                  {c.description && <p className="text-xs text-gray-500">{c.description}</p>}
                  <p className="mt-1 text-xs text-gray-400">
                    {new Date(c.scheduledAt).toLocaleString()} &bull; {c.durationMinutes} min
                  </p>
                </div>
              </div>
            </div>
          ))}
        </section>
      )}

      {past.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">{t("pastClasses")}</h2>
          {past.map(c => (
            <div key={c.id} className="nursery-card mb-3 rounded-2xl bg-gray-50 p-4 opacity-70">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-gray-100">
                  <Video className="size-6 text-gray-400" />
                </div>
                <div>
                  <p className="font-bold text-gray-600">{c.title}</p>
                  <p className="text-xs text-gray-400">{t("pastClasses")}</p>
                </div>
              </div>
            </div>
          ))}
        </section>
      )}

      {liveClasses.length === 0 && (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Video className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.default")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.teacherWillAdd")}</p>
        </div>
      )}
    </div>
  )
}
