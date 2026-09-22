"use client"

import { ArrowLeft, Search, BookOpen, FileText } from "lucide-react"
import Link from "next/link"
import { useTranslations } from "next-intl"

export default function ResearchPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("secondary.backToSecondary")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("secondary.research")}</h1>
          <p className="text-sm text-gray-500">{t("secondary.investigateAndDiscover")}</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 p-5 text-white">
        <div className="flex items-center gap-3">
          <Search className="size-8" />
          <div>
            <h2 className="text-lg font-bold">{t("secondary.researchWorld")}</h2>
            <p className="text-sm text-white/70">{t("secondary.developYourResearchSkills")}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {[
          { icon: Search, title: t("secondary.question"), desc: t("secondary.defineWhatYouWantToFindOut"), color: "bg-blue-50 text-blue-600" },
          { icon: BookOpen, title: t("secondary.research"), desc: t("secondary.gatherInformationFromSources"), color: "bg-green-50 text-green-600" },
          { icon: FileText, title: t("secondary.organize"), desc: t("secondary.structureYourFindings"), color: "bg-amber-50 text-amber-600" },
          { icon: FileText, title: t("secondary.present"), desc: t("secondary.shareYourResearch"), color: "bg-purple-50 text-purple-600" },
        ].map(item => (
          <div key={item.title} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5">
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${item.color}`}>
              <item.icon className="size-6" />
            </div>
            <div>
              <p className="font-semibold text-gray-900">{item.title}</p>
              <p className="text-sm text-gray-400">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <Search className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">{t("secondary.researchProjectsComingSoon")}</h3>
        <p className="mt-1 text-sm text-gray-500">{t("secondary.teacherWillAssignResearchProjects")}</p>
      </div>
    </div>
  )
}
