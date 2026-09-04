import { notFound } from "next/navigation"
import Link from "next/link"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  vetaTrades,
  getVetaTradeBySlug,
  getVetaLevelBySlug,
  getVetaModuleBySlug,
  getVetaLessonBySlug,
  vetaLessonIndex,
} from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  const params: { trade: string; level: string; module: string; lesson: string }[] = []
  for (const trade of vetaTrades) {
    for (const level of trade.levels) {
      for (const mod of level.modules) {
        for (const lesson of mod.lessons) {
          params.push({ trade: trade.id, level: level.id, module: mod.id, lesson: lesson.id })
        }
      }
    }
  }
  return params
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ trade: string; level: string; module: string; lesson: string }>
}): Promise<Metadata> {
  const { trade, level, module, lesson } = await params
  const t = getVetaTradeBySlug(trade)
  if (!t) return { title: "Not Found — ELMKUSOMA" }
  const l = getVetaLevelBySlug(t, level)
  if (!l) return { title: "Not Found — ELMKUSOMA" }
  const m = getVetaModuleBySlug(l, module)
  if (!m) return { title: "Not Found — ELMKUSOMA" }
  const les = getVetaLessonBySlug(m, lesson)
  if (!les) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${les.title} — ${m.name} — VETA — ELMKUSOMA`,
    description: les.description,
  }
}

export default async function LessonPage({
  params,
}: {
  params: Promise<{ trade: string; level: string; module: string; lesson: string }>
}) {
  const { trade, level, module, lesson } = await params
  const tradeData = getVetaTradeBySlug(trade)
  if (!tradeData) notFound()

  const levelData = getVetaLevelBySlug(tradeData, level)
  if (!levelData) notFound()

  const moduleData = getVetaModuleBySlug(levelData, module)
  if (!moduleData) notFound()

  const lessonData = getVetaLessonBySlug(moduleData, lesson)
  if (!lessonData) notFound()

  const currentIndex = vetaLessonIndex(moduleData, lesson)
  const prevLesson = currentIndex > 0 ? moduleData.lessons[currentIndex - 1] : null
  const nextLesson =
    currentIndex < moduleData.lessons.length - 1 ? moduleData.lessons[currentIndex + 1] : null

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1 bg-muted/40">
        <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Link href="/courses" className="transition-colors hover:text-foreground">Courses</Link>
            <span>/</span>
            <Link href="/courses/veta" className="transition-colors hover:text-foreground">VETA</Link>
            <span>/</span>
            <Link href={`/courses/veta/${trade}`} className="transition-colors hover:text-foreground">
              {tradeData.name}
            </Link>
            <span>/</span>
            <Link
              href={`/courses/veta/${trade}/${level}`}
              className="transition-colors hover:text-foreground"
            >
              {levelData.name}
            </Link>
            <span>/</span>
            <Link
              href={`/courses/veta/${trade}/${level}/${module}`}
              className="transition-colors hover:text-foreground"
            >
              {moduleData.name}
            </Link>
            <span>/</span>
            <span className="text-foreground">{lessonData.title}</span>
          </div>

          <div className="mt-6 rounded-2xl border border-border bg-card p-6 shadow-xs sm:p-8">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center rounded-md bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
                Lesson {currentIndex + 1} of {moduleData.lessons.length}
              </span>
              <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                {lessonData.duration}
              </span>
            </div>

            <h1 className="mt-4 text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              {lessonData.title}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {moduleData.name} &middot; {levelData.name} &middot; {tradeData.name}
            </p>

            <div className="mt-6">
              <h2 className="text-sm font-semibold text-foreground">Lesson Overview</h2>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                {lessonData.description}
              </p>
            </div>

            <div className="mt-8 rounded-xl border border-border bg-muted/50 p-6">
              <div className="flex items-center gap-3">
                <span className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                </span>
                <div>
                  <p className="text-sm font-semibold text-foreground">Learning Material</p>
                  <p className="text-xs text-muted-foreground">
                    Complete this lesson to track your progress.
                  </p>
                </div>
              </div>
              <div className="mt-4 rounded-lg border border-dashed border-border bg-background p-8 text-center">
                <p className="text-sm text-muted-foreground">
                  Lesson content will be available here — video, notes, and interactive exercises.
                </p>
              </div>
            </div>
          </div>

          <div className="mt-6 flex items-center justify-between">
            {prevLesson ? (
              <Link
                href={`/courses/veta/${trade}/${level}/${module}/${prevLesson.id}`}
                className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium text-foreground shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="text-left">
                  <span className="block text-[10px] uppercase tracking-wide text-muted-foreground">Previous</span>
                  <span className="block text-xs">{prevLesson.title}</span>
                </div>
              </Link>
            ) : (
              <div />
            )}
            {nextLesson ? (
              <Link
                href={`/courses/veta/${trade}/${level}/${module}/${nextLesson.id}`}
                className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium text-foreground shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="text-right">
                  <span className="block text-[10px] uppercase tracking-wide text-muted-foreground">Next</span>
                  <span className="block text-xs">{nextLesson.title}</span>
                </div>
              </Link>
            ) : (
              <Link
                href={`/courses/veta/${trade}/${level}/${module}`}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-medium text-primary-foreground shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md hover:bg-primary/90"
              >
                Back to Module
              </Link>
            )}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
