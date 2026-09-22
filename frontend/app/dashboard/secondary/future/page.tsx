"use client"

import { useTranslations } from "next-intl"
import { ArrowLeft, Globe, GraduationCap, Wrench, Briefcase, Lightbulb } from "lucide-react"
import Link from "next/link"

export default function SecondaryFuturePage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")

  const CAREER_PATHS = [
    { icon: GraduationCap, title: t("university"), desc: t("bachelorDegreePrograms"), color: "bg-blue-50 text-blue-600" },
    { icon: Wrench, title: t("tvetCollege"), desc: t("technicalVocationalTraining"), color: "bg-green-50 text-green-600" },
    { icon: Briefcase, title: t("careerPaths"), desc: t("exploreProfessionalOpportunities"), color: "bg-purple-50 text-purple-600" },
    { icon: Lightbulb, title: t("skillsIndustries"), desc: t("discoverInDemandSkills"), color: "bg-amber-50 text-amber-600" },
  ]

  const FIELDS = [
    t("medicineHealthSciences"),
    t("engineeringTechnology"),
    t("computerScienceIT"),
    t("businessEconomics"),
    t("educationTeaching"),
    t("agricultureEnvironmentalScience"),
    t("lawSocialSciences"),
    t("artsHumanities"),
    t("naturalSciences"),
    t("hospitalityTourism"),
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("futureWorld")}</h1>
          <p className="text-sm text-gray-500">{t("exploreAfterSecondary")}</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-blue-500 p-5 text-white">
        <h2 className="text-lg font-bold">{t("explorePossibilities")}</h2>
        <p className="mt-1 text-sm text-white/70">
          {t("explorePossibilitiesDesc")}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {CAREER_PATHS.map(path => (
          <div key={path.title} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5">
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${path.color}`}>
              <path.icon className="size-6" />
            </div>
            <div>
              <p className="font-semibold text-gray-900">{path.title}</p>
              <p className="text-sm text-gray-400">{path.desc}</p>
            </div>
          </div>
        ))}
      </div>

      <section className="rounded-2xl border border-gray-100 bg-white p-5">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">{t("universityFields")}</h2>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          {FIELDS.map(field => (
            <div key={field} className="flex items-center gap-2 rounded-xl bg-gray-50 px-4 py-2.5">
              <Globe className="size-3.5 text-gray-400" />
              <span className="text-sm text-gray-700">{field}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
