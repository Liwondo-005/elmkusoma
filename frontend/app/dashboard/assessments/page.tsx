"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { assessmentApi, type Assessment } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Compass, Clock, CheckCircle, Play, Star, Globe, Leaf, Calculator, ClipboardList, ArrowRight } from "lucide-react"

function isAvailable(a: Assessment) {
  if (!a.startsAt) return a.isPublished
  const now = new Date()
  const start = new Date(a.startsAt)
  const end = a.endsAt ? new Date(a.endsAt) : null
  return now >= start && (!end || now <= end)
}


function PrimaryDiscoverView({ assessments }: { assessments: Assessment[] }) {
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const discoveryAreas = [
  {
    title: t("assessments.areaTanzania"),
    icon: Leaf,
    color: "text-emerald-600 dark:text-emerald-400",
    bg: "bg-emerald-50 dark:bg-emerald-950/30",
    topics: [t("assessments.topicWildlife"), t("assessments.topicGeography"), t("assessments.topicPeople"), t("assessments.topicEnvironment")],
  },
  {
    title: t("assessments.areaWorld"),
    icon: Globe,
    color: "text-blue-600 dark:text-blue-400",
    bg: "bg-blue-50 dark:bg-blue-950/30",
    topics: [t("assessments.topicAfrica"), t("assessments.topicOceans"), t("assessments.topicSpace"), t("assessments.topicWorldCultures")],
  },
  {
    title: t("assessments.areaProblem"),
    icon: Calculator,
    color: "text-amber-600 dark:text-amber-400",
    bg: "bg-amber-50 dark:bg-amber-950/30",
    topics: [t("assessments.topicMathPuzzles"), t("assessments.topicLogic"), t("assessments.topicPatterns"), t("assessments.topicRealMath")],
  },
]
  return (
    <div className="space-y-8">
      <div className="rounded-2xl bg-gradient-to-br from-primary/10 via-primary/5 to-transparent p-6 border border-primary/10">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/15">
            <Compass className="size-6 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("assessments.discoverTitle")}</h1>
            <p className="text-sm text-muted-foreground">{t("assessments.discoverSubtitle")}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {discoveryAreas.map((area) => {
          const Icon = area.icon
          return (
            <div key={area.title} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md">
              <div className="flex items-center gap-3">
                <div className={`flex size-10 items-center justify-center rounded-xl ${area.bg}`}>
                  <Icon className={`size-5 ${area.color}`} />
                </div>
                <h3 className="text-sm font-semibold text-foreground">{area.title}</h3>
              </div>
              <ul className="mt-3 space-y-2">
                {area.topics.map((topic) => (
                  <li key={topic} className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Star className="size-3 shrink-0 text-primary/50" />
                    {topic}
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </div>

      <div>
        <h2 className="text-lg font-semibold text-foreground">{t("assessments.showWhatYouKnow")}</h2>
        <p className="text-sm text-muted-foreground">{t("assessments.pickQuiz")}</p>
      </div>

      {assessments.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <ClipboardList className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("assessments.emptyQuizTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("assessments.emptyQuizDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {assessments.map((a) => {
            const available = isAvailable(a)
            return (
              <div
                key={a.id}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <Compass className="size-5 text-primary" />
                  </div>
                  {available ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400">
                      <Play className="size-3" /> {t("assessments.available")}
                    </span>
                  ) : a.isPublished ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                      {t("assessments.upcoming")}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
                      <CheckCircle className="size-3" /> {ts("completed")}
                    </span>
                  )}
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground">{a.title}</h3>
                {a.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{a.description}</p>
                )}
                <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                  {a.timeLimitMinutes && (
                    <span className="flex items-center gap-1">
                      <Clock className="size-3" /> {t("assessments.minsCount", { count: a.timeLimitMinutes })}
                    </span>
                  )}
                  <span>{t("assignments.marksCount", { count: a.totalMarks })}</span>
                </div>
                {available && (
                  <Link
                    href={`/dashboard/assessments/${a.id}`}
                    className="mt-4 flex items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
                  >
                    {t("assessments.startQuiz")} <ArrowRight className="size-3.5" />
                  </Link>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default function AssessmentsPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [loading, setLoading] = useState(true)

  const isPrimary = user?.learningLevel?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await assessmentApi.getByClass(user!.classGroupId || "")
      setAssessments(data)
    } catch {
      setAssessments([])
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (isPrimary) {
    return <PrimaryDiscoverView assessments={assessments} />
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("assessments.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("assessments.subtitle")}</p>
      </div>

      {assessments.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <ClipboardList className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("assessments.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("assessments.emptyDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {assessments.map((a) => {
            const available = isAvailable(a)
            return (
              <div
                key={a.id}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <ClipboardList className="size-5 text-primary" />
                  </div>
                  {available ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                      <Play className="size-3" /> {t("assessments.available")}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
                      {t("assessments.upcoming")}
                    </span>
                  )}
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground">{a.title}</h3>
                {a.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{a.description}</p>
                )}
                <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                  <div className="flex items-center justify-between">
                    <span>{t("assessments.totalMarks")}</span>
                    <span className="font-medium text-foreground">{a.totalMarks}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>{t("assessments.passMarks")}</span>
                    <span className="font-medium text-foreground">{a.passMarks}</span>
                  </div>
                  {a.timeLimitMinutes && (
                    <div className="flex items-center justify-between">
                      <span>{t("assessments.timeLimit")}</span>
                      <span className="font-medium text-foreground">{t("assessments.minsCount", { count: a.timeLimitMinutes })}</span>
                    </div>
                  )}
                </div>
                {available && (
                  <Link href={`/dashboard/assessments/${a.id}`} className="mt-4 block">
                    <div className="flex w-full items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90">
                      {t("assessments.startQuiz")} <ArrowRight className="size-3.5" />
                    </div>
                  </Link>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
