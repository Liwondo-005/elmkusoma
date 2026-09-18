"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { learningApi, type Lesson, type LessonProgress, primaryApi, type LearningPassport } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { Map, CheckCircle, Clock, ArrowRight, Compass } from "lucide-react"

export default function JourneyPage() {
  const { user } = useRequireAuth()
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [progress, setProgress] = useState<LessonProgress[]>([])
  const [passport, setPassport] = useState<LearningPassport | null>(null)
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [lessonsData, progressData, passportData] = await Promise.all([
        learningApi.getLessonsByClass(user!.classGroupId || ""),
        learningApi.getStudentProgress(user!.id).catch(() => []),
        primaryApi.getLearningPassport().catch(() => null),
      ])
      setLessons(lessonsData)
      setProgress(progressData)
      setPassport(passportData)
    } catch {
      setLessons([])
    } finally {
      setLoading(false)
    }
  }

  function getSubjectProgress(subjectName: string) {
    const subjectLessons = lessons.filter((l) => l.subjectName === subjectName)
    const subjectProgress = progress.filter((p) =>
      subjectLessons.some((l) => l.id === p.lessonId)
    )
    const completed = subjectProgress.filter((p) => p.completionPercentage >= 100).length
    const inProgress = subjectProgress.filter(
      (p) => p.completionPercentage > 0 && p.completionPercentage < 100
    ).length
    const total = subjectLessons.length
    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0
    return { completed, inProgress, total, percentage }
  }

  const subjectJourney = primarySubjects.map((subject) => {
    const prog = getSubjectProgress(subject.name)
    let status: "not_started" | "in_progress" | "completed" = "not_started"
    if (prog.percentage >= 100) status = "completed"
    else if (prog.percentage > 0 || prog.inProgress > 0) status = "in_progress"
    return { ...subject, ...prog, status }
  })

  const completedCount = subjectJourney.filter((s) => s.status === "completed").length
  const totalCount = subjectJourney.length

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/10">
            <Map className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">My Learning Journey</h1>
            <p className="text-sm text-muted-foreground">Safari yangu ya Masomo</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Compass className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">Journey view available for Primary learners</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Switch to a primary learner account to see your learning journey map.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-teal/5 via-card to-primary/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-teal/10">
            <Map className="size-6 text-teal" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">My Learning Journey</h1>
            <p className="text-sm text-muted-foreground">Safari yangu ya Masomo</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <CheckCircle className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{completedCount}</p>
              <p className="text-xs text-muted-foreground">Subjects Completed</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Clock className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{totalCount - completedCount}</p>
              <p className="text-xs text-muted-foreground">Still Exploring</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Compass className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{passport?.stampsEarned ?? 0}</p>
              <p className="text-xs text-muted-foreground">Stamps Earned</p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-foreground">Journey Progress</h2>
            <p className="text-sm text-muted-foreground">{completedCount} of {totalCount} subjects completed</p>
          </div>
          <span className="text-2xl font-bold text-primary">{totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0}%</span>
        </div>
        <div className="mt-3 h-3 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-gradient-to-r from-primary to-teal transition-all"
            style={{ width: `${totalCount > 0 ? (completedCount / totalCount) * 100 : 0}%` }}
          />
        </div>
      </div>

      <div className="relative">
        <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-border md:left-8" />

        <div className="space-y-6">
          {subjectJourney.map((subject, index) => {
            const Icon = subject.icon
            const statusColor =
              subject.status === "completed"
                ? "bg-green-500"
                : subject.status === "in_progress"
                ? "bg-primary"
                : "bg-muted-foreground/30"
            const statusBg =
              subject.status === "completed"
                ? "bg-green-50 border-green-200"
                : subject.status === "in_progress"
                ? "bg-primary/5 border-primary/20"
                : "bg-muted/30 border-border"

            return (
              <div key={subject.name} className="relative flex gap-4 md:gap-6">
                <div className="relative z-10 flex flex-col items-center">
                  <div className={`flex size-12 items-center justify-center rounded-full ${statusColor} text-white shadow-md md:size-14`}>
                    {subject.status === "completed" ? (
                      <CheckCircle className="size-6" />
                    ) : (
                      <Icon className="size-6" />
                    )}
                  </div>
                </div>

                <Link
                  href={`/dashboard/lessons?subject=${encodeURIComponent(subject.name)}`}
                  className={`flex-1 rounded-2xl border p-5 shadow-xs transition-all hover:shadow-md ${statusBg}`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-base font-bold text-foreground">{subject.name}</h3>
                      <p className="mt-0.5 text-xs text-muted-foreground">{subject.description}</p>
                    </div>
                    <ArrowRight className="size-4 text-muted-foreground" />
                  </div>

                  <div className="mt-4 flex items-center gap-4 text-xs">
                    <span className="text-muted-foreground">
                      {subject.completed} / {subject.total} lessons
                    </span>
                    {subject.inProgress > 0 && (
                      <span className="text-primary font-medium">{subject.inProgress} in progress</span>
                    )}
                  </div>

                  <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className={`h-full rounded-full transition-all ${
                        subject.status === "completed"
                          ? "bg-green-500"
                          : subject.status === "in_progress"
                          ? "bg-primary"
                          : "bg-muted-foreground/20"
                      }`}
                      style={{ width: `${subject.percentage}%` }}
                    />
                  </div>

                  <div className="mt-2 flex items-center gap-2">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                        subject.status === "completed"
                          ? "bg-green-100 text-green-700"
                          : subject.status === "in_progress"
                          ? "bg-primary/10 text-primary"
                          : "bg-muted text-muted-foreground"
                      }`}
                    >
                      {subject.status === "completed"
                        ? "Completed"
                        : subject.status === "in_progress"
                        ? "In Progress"
                        : "Not Started"}
                    </span>
                    <span className="text-[10px] text-muted-foreground">{subject.percentage}%</span>
                  </div>
                </Link>
              </div>
            )
          })}
        </div>
      </div>

      {subjectJourney.every((s) => s.status === "not_started" && s.total === 0) && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <Compass className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">Your journey awaits!</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            Your teacher will set up your learning path. Once lessons are assigned, your journey will appear here.
          </p>
          <Link
            href="/dashboard/lessons"
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Browse Lessons <ArrowRight className="size-4" />
          </Link>
        </div>
      )}
    </div>
  )
}
