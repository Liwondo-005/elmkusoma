"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryMilestone } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, Award, Image, FileText, Video } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

export default function EvidenceOfLearningPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("nursery")
  const tc = useTranslations("common")
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.id) { setLoading(false); return }
    nurseryApi.getMilestones(user.id)
      .then(setMilestones)
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
        <Link href="/dashboard/nursery" aria-label={tc("back")} className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("evidenceOfLearning")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.evidence")}</p>
        </div>
      </div>

      {milestones.length === 0 ? (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Award className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noEvidence")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.keepLearning")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {milestones.map(m => (
            <div key={m.id} className="nursery-card rounded-2xl bg-white p-5">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-green-100">
                  <Award className="size-5 text-green-500" />
                </div>
                <div>
                  <p className="font-bold text-gray-800">{m.description || m.category}</p>
                  <p className="text-xs text-gray-500">{m.category} · {m.achievedDate ? new Date(m.achievedDate).toLocaleDateString() : "Pending"}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Evidence Types */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">{t("typesOfEvidence")}</h2>
        <div className="grid grid-cols-3 gap-3">
          {[
            { icon: Image, label: t("photos"), color: "bg-pink-100 text-pink-500" },
            { icon: FileText, label: t("drawings"), color: "bg-blue-100 text-blue-500" },
            { icon: Video, label: t("videos"), color: "bg-purple-100 text-purple-500" },
          ].map(e => (
            <div key={e.label} className="nursery-card rounded-2xl bg-white p-4 text-center">
              <div className={`mx-auto flex size-12 items-center justify-center rounded-2xl ${e.color}`}>
                <e.icon className="size-6" />
              </div>
              <p className="mt-2 text-sm font-bold text-gray-700">{e.label}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}
