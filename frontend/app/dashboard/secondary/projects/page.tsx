"use client"

import { useTranslations } from "next-intl"
import { ArrowLeft, FolderOpen, Globe } from "lucide-react"
import Link from "next/link"

export default function SecondaryProjectsPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")

  const STEPS = [
    { step: "1", title: t("question"), desc: t("defineWhatYouInvestigate") },
    { step: "2", title: t("research"), desc: t("gatherInfoAndSources") },
    { step: "3", title: t("plan"), desc: t("organizeYourApproach") },
    { step: "4", title: t("create"), desc: t("buildYourProject") },
    { step: "5", title: t("present"), desc: t("shareYourFindings") },
    { step: "6", title: t("improve"), desc: t("incorporateFeedback") },
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("projects")}</h1>
          <p className="text-sm text-gray-500">{t("projectsDesc")}</p>
        </div>
      </div>

      <Link
        href="/dashboard/secondary/projects/passport"
        className="flex items-center gap-4 rounded-2xl border-2 border-indigo-200 bg-indigo-50 p-5 transition-all hover:border-indigo-300 hover:shadow-sm"
        aria-label={t("learningPassport")}
      >
        <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-indigo-100">
          <Globe className="size-6 text-indigo-600" />
        </div>
        <div>
          <p className="font-semibold text-indigo-900">{t("learningPassport")}</p>
          <p className="text-sm text-indigo-600">{t("completeAcademicRecord")}</p>
        </div>
      </Link>

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <FolderOpen className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">{t("projectsResearch")}</h3>
        <p className="mt-1 text-sm text-gray-500">
          {t("projectsResearchDesc")}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {STEPS.map(item => (
          <div key={item.step} className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4">
            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-sm font-bold text-indigo-600">
              {item.step}
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900">{item.title}</p>
              <p className="text-xs text-gray-400">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
