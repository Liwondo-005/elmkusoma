"use client"

import { useTranslations } from "next-intl"
import { ArrowLeft, TrendingUp, BookOpen, FileText, AlertCircle, Calendar, Lightbulb, Brain, RefreshCw, Clock, FlaskConical, Search } from "lucide-react"
import Link from "next/link"

export default function SecondaryRevisionPage() {
  const t = useTranslations("secondary")
  const tc = useTranslations("common")

  const QUICK_ACTIONS = [
    { icon: FileText, title: t("upcomingAssessments"), desc: t("reviewWhatIsComing"), color: "bg-amber-50 text-amber-600", href: "/dashboard/secondary/assess" },
    { icon: BookOpen, title: t("recentlyStudied"), desc: t("revisitRecentTopics"), color: "bg-blue-50 text-blue-600", href: "/dashboard/secondary/learn" },
    { icon: TrendingUp, title: t("practiceSets"), desc: t("strengthenWeakAreas"), color: "bg-green-50 text-green-600", href: "/dashboard/secondary/practice" },
    { icon: AlertCircle, title: t("examPrep"), desc: t("prepareForUpcomingExams"), color: "bg-orange-50 text-orange-600", href: "/dashboard/secondary/revision/exam-prep" },
    { icon: Calendar, title: t("studyPlanner"), desc: t("planYourStudySessions"), color: "bg-indigo-50 text-indigo-600", href: "/dashboard/secondary/revision/study-planner" },
    { icon: Lightbulb, title: t("conceptExplorer"), desc: t("breakDownComplexIdeas"), color: "bg-yellow-50 text-yellow-600", href: "/dashboard/secondary/learn/concept-explorer" },
    { icon: Brain, title: t("problemSolving"), desc: t("practiceSolvingProblems"), color: "bg-purple-50 text-purple-600", href: "/dashboard/secondary/learn/problem-solving" },
    { icon: RefreshCw, title: t("errorAnalysis"), desc: t("learnFromMistakes"), color: "bg-red-50 text-red-600", href: "/dashboard/secondary/learn/error-analysis" },
    { icon: Clock, title: t("examMode"), desc: t("timedPracticeExam"), color: "bg-rose-50 text-rose-600", href: "/dashboard/secondary/learn/exam-mode" },
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={tc("goBack")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("revisionCenter")}</h1>
          <p className="text-sm text-gray-500">{t("revisionCenterDesc")}</p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {QUICK_ACTIONS.map(item => (
          <Link
            key={item.title}
            href={item.href}
            className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 transition-all hover:border-indigo-200 hover:shadow-sm"
            aria-label={item.title}
          >
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${item.color}`}>
              <item.icon className="size-6" />
            </div>
            <div>
              <p className="font-semibold text-gray-900">{item.title}</p>
              <p className="text-sm text-gray-400">{item.desc}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
