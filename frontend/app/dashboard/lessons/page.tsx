"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learningApi, type Lesson, type LessonProgress } from "@/lib/api"
import { getDashboardConfig, type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { BookOpen, Clock, CheckCircle, Play, ArrowRight, Search, Filter } from "lucide-react"

export default function LessonsPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [progress, setProgress] = useState<LessonProgress[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [lessonsData, progressData] = await Promise.all([
        learningApi.getLessonsByClass(user!.classGroupId || ""),
        learningApi.getStudentProgress(user!.id).catch(() => []),
      ])
      setLessons(lessonsData)
      setProgress(progressData)
    } catch {
      setLessons([])
    } finally {
      setLoading(false)
    }
  }

  function getLessonProgress(lessonId: string): number {
    const p = progress.find((pr) => pr.lessonId === lessonId)
    return p?.completionPercentage || 0
  }

  const filteredLessons = lessons.filter((lesson) => {
    const matchesSubject = !selectedSubject || lesson.subjectName === selectedSubject
    const matchesSearch = !searchQuery || lesson.title.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesSubject && matchesSearch
  })

  const subjectGroups = isPrimary
    ? primarySubjects.map((subject) => ({
        ...subject,
        lessons: filteredLessons.filter((l) => l.subjectName === subject.name),
      })).filter((group) => group.lessons.length > 0 || !selectedSubject)
    : []

  if (isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("lessons.learnTitle")}</h1>
          <p className="text-sm text-muted-foreground">{t("lessons.learnSubtitle")}</p>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("lessons.searchPlaceholder")}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>

        {/* Subject Filter Pills */}
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setSelectedSubject(null)}
            className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
              !selectedSubject
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            {t("lessons.allSubjects")}
          </button>
          {primarySubjects.map((subject) => (
            <button
              key={subject.name}
              onClick={() => setSelectedSubject(selectedSubject === subject.name ? null : subject.name)}
              className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
                selectedSubject === subject.name
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-muted-foreground hover:bg-muted/80"
              }`}
            >
              {subject.name}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : filteredLessons.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border py-12 text-center">
            <div className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary/10">
              <BookOpen className="size-8 text-primary" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-foreground">{t("lessons.emptyTitle")}</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {selectedSubject
                ? t("lessons.emptySubject", { subject: selectedSubject })
                : t("lessons.emptyDefault")}
            </p>
          </div>
        ) : selectedSubject ? (
          /* Subject-Filtered View: Show lessons in a grid */
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filteredLessons.map((lesson) => {
              const pct = getLessonProgress(lesson.id)
              return (
                <Link
                  key={lesson.id}
                  href={`/dashboard/lessons/${lesson.id}`}
                  className="group rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md hover:border-primary/30"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                      {pct >= 100 ? (
                        <CheckCircle className="size-5 text-green-600" />
                      ) : pct > 0 ? (
                        <Clock className="size-5 text-amber-600" />
                      ) : (
                        <Play className="size-5 text-primary" />
                      )}
                    </div>
                    <ArrowRight className="size-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                  </div>
                  <h3 className="mt-3 text-sm font-semibold text-foreground line-clamp-1">{lesson.title}</h3>
                  {lesson.description && (
                    <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{lesson.description}</p>
                  )}
                  <div className="mt-3">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>{pct >= 100 ? ts("completed") : pct > 0 ? ts("inProgress") : ts("notStarted")}</span>
                      <span>{Math.round(pct)}%</span>
                    </div>
                    <div className="mt-1.5 h-1.5 w-full rounded-full bg-muted overflow-hidden">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-primary to-teal transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        ) : (
          /* Subject Card View: Show grouped subjects */
          <div className="space-y-6">
            {subjectGroups.map((group) => {
              const Icon = group.icon
              const completedCount = group.lessons.filter((l) => getLessonProgress(l.id) >= 100).length
              const totalLessons = group.lessons.length
              const avgProgress = totalLessons > 0
                ? group.lessons.reduce((sum, l) => sum + getLessonProgress(l.id), 0) / totalLessons
                : 0

              return (
                <div key={group.name} className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                  <div className="flex items-center gap-4">
                    <div className={`flex size-12 items-center justify-center rounded-2xl ${group.bgColor}`}>
                      <Icon className={`size-6 ${group.color}`} />
                    </div>
                    <div className="flex-1">
                      <h2 className="text-lg font-semibold text-foreground">{group.name}</h2>
                      <p className="text-xs text-muted-foreground">
                        {t("lessons.groupSummary", { done: completedCount, total: totalLessons })}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-bold text-primary">{Math.round(avgProgress)}%</p>
                      <div className="mt-1 h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-primary to-teal"
                          style={{ width: `${avgProgress}%` }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Lessons in this subject */}
                  <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {group.lessons.slice(0, 6).map((lesson) => {
                      const pct = getLessonProgress(lesson.id)
                      return (
                        <Link
                          key={lesson.id}
                          href={`/dashboard/lessons/${lesson.id}`}
                          className="flex items-center gap-3 rounded-xl border border-border p-3 transition-all hover:bg-muted/50"
                        >
                          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                            {pct >= 100 ? (
                              <CheckCircle className="size-4 text-green-600" />
                            ) : pct > 0 ? (
                              <Clock className="size-4 text-amber-600" />
                            ) : (
                              <Play className="size-4 text-primary" />
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-foreground truncate">{lesson.title}</p>
                            <p className="text-xs text-muted-foreground">
                              {pct >= 100 ? t("lessons.done") : pct > 0 ? `${Math.round(pct)}%` : t("lessons.start")}
                            </p>
                          </div>
                        </Link>
                      )
                    })}
                  </div>

                  {group.lessons.length > 6 && (
                    <button
                      onClick={() => setSelectedSubject(group.name)}
                      className="mt-3 text-sm font-medium text-primary hover:underline"
                    >
                      {t("lessons.viewAll", { count: group.lessons.length })}
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    )
  }

  /* Non-Primary: Original lessons page */
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("lessons.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("lessons.subtitle")}</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : lessons.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("lessons.emptyTitleGeneral")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("lessons.emptyGeneralDesc")}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {lessons.map((lesson) => {
            const pct = getLessonProgress(lesson.id)
            return (
              <Link
                key={lesson.id}
                href={`/dashboard/lessons/${lesson.id}`}
                className="group rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start justify-between">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    {pct >= 100 ? (
                      <CheckCircle className="size-5 text-primary" />
                    ) : pct > 0 ? (
                      <Clock className="size-5 text-primary" />
                    ) : (
                      <Play className="size-5 text-primary" />
                    )}
                  </div>
                  <ArrowRight className="size-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground line-clamp-1">{lesson.title}</h3>
                {lesson.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{lesson.description}</p>
                )}
                <div className="mt-3">
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span>{pct >= 100 ? ts("completed") : pct > 0 ? ts("inProgress") : ts("notStarted")}</span>
                    <span>{Math.round(pct)}%</span>
                  </div>
                  <div className="mt-1.5 h-1.5 w-full rounded-full bg-muted overflow-hidden">
                    <div
                      className="h-full rounded-full bg-primary transition-all"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
