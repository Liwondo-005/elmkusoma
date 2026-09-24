"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useRequireAuth } from "@/lib/auth"
import { secondaryApi, type SubjectSummary } from "@/lib/secondary-api"
import { LoadingState } from "@/components/learner/shared"
import { BookOpen, ArrowLeft, ChevronRight, FileText, Video, Award, TrendingUp, FlaskConical, Brain, MessageSquare, Wrench, Sparkles, AlertCircle } from "lucide-react"
import Link from "next/link"

export default function SecondaryLearnPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("secondary")
  const tc = useTranslations("common")
  const [subjects, setSubjects] = useState<SubjectSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.classGroupId) { setLoading(false); return }
    secondaryApi.getSubjects(user.classGroupId)
      .then(setSubjects)
      .catch(() => setError(t("loadError")))
      .finally(() => setLoading(false))
  }, [user, t])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertCircle className="mx-auto size-12 text-red-400" />
          <h3 className="mt-3 text-lg font-bold text-red-800">{tc("error.generic")}</h3>
          <p className="mt-1 text-sm text-red-600">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label={t("backToSecondary")}>
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{t("learn")}</h1>
          <p className="text-sm text-gray-500">{t("exploreSubjectsAndTopics")}</p>
        </div>
      </div>

      {subjects.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
          <BookOpen className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("noSubjectsAvailable")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("subjectsWillAppearOnceSetup")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {subjects.map(subject => (
            <Link
              key={subject.id}
              href={`/dashboard/secondary/subjects/${subject.id}`}
              className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 transition-all hover:border-indigo-200 hover:shadow-sm"
              aria-label={subject.name}
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50">
                <BookOpen className="size-6 text-indigo-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-gray-900">{subject.name}</p>
                <div className="mt-1 flex flex-wrap gap-2 text-xs text-gray-400">
                  {subject.totalLessons && <span>{subject.totalLessons} {t("topics")}</span>}
                  {subject.completedLessons !== undefined && <span>· {subject.completedLessons} {t("done")}</span>}
                  {subject.upcomingAssessments !== undefined && subject.upcomingAssessments > 0 && (
                    <span className="text-amber-600">· {subject.upcomingAssessments} {t("assessment")}{subject.upcomingAssessments > 1 ? "s" : ""}</span>
                  )}
                </div>
              </div>
              <ChevronRight className="size-5 shrink-0 text-gray-300" />
            </Link>
          ))}
        </div>
      )}

      {/* Learning Skills */}
      <div className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">{t("learningSkills")}</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          {[
            { label: t("conceptExplorer"), desc: t("deepDiveIntoKeyConcepts"), href: "/dashboard/secondary/learn/concept-explorer", icon: Brain, color: "bg-purple-50 text-purple-600" },
            { label: t("problemSolving"), desc: t("practiceSolvingProblemsStepByStep"), href: "/dashboard/secondary/learn/problem-solving", icon: Wrench, color: "bg-blue-50 text-blue-600" },
            { label: t("criticalThinking"), desc: t("analyzeAndEvaluateArguments"), href: "/dashboard/secondary/learn/critical-thinking", icon: Brain, color: "bg-indigo-50 text-indigo-600" },
            { label: t("communication"), desc: t("writingSpeakingAndDebate"), href: "/dashboard/secondary/learn/communication", icon: MessageSquare, color: "bg-green-50 text-green-600" },
            { label: t("practicalLearning"), desc: t("handsOnExperimentsAndLabWork"), href: "/dashboard/secondary/learn/practical-learning", icon: FlaskConical, color: "bg-amber-50 text-amber-600" },
            { label: t("examMode"), desc: t("timedPracticeUnderExamConditions"), href: "/dashboard/secondary/learn/exam-mode", icon: Award, color: "bg-red-50 text-red-600" },
            { label: t("controlledAI"), desc: t("aiGuidedLearningWithGuardrails"), href: "/dashboard/secondary/learn/controlled-ai", icon: Sparkles, color: "bg-violet-50 text-violet-600" },
          ].map(skill => (
            <Link
              key={skill.label}
              href={skill.href}
              className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4 transition-all hover:border-indigo-200 hover:shadow-sm"
              aria-label={skill.label}
            >
              <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${skill.color}`}>
                <skill.icon className="size-5" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-gray-900">{skill.label}</p>
                <p className="text-xs text-gray-400">{skill.desc}</p>
              </div>
              <ChevronRight className="size-4 shrink-0 text-gray-300" />
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
