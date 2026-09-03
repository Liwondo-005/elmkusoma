import { notFound } from "next/navigation"
import Link from "next/link"
import { Clock, ChevronRight, CheckCircle } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import {
  vetaTrades,
  getVetaTradeBySlug,
  getVetaLevelBySlug,
  getVetaModuleBySlug,
} from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  const params: { trade: string; level: string; module: string }[] = []
  for (const trade of vetaTrades) {
    for (const level of trade.levels) {
      for (const mod of level.modules) {
        params.push({ trade: trade.id, level: level.id, module: mod.id })
      }
    }
  }
  return params
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ trade: string; level: string; module: string }>
}): Promise<Metadata> {
  const { trade, level, module } = await params
  const t = getVetaTradeBySlug(trade)
  if (!t) return { title: "Not Found — ELMKUSOMA" }
  const l = getVetaLevelBySlug(t, level)
  if (!l) return { title: "Not Found — ELMKUSOMA" }
  const m = getVetaModuleBySlug(l, module)
  if (!m) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${m.name} — ${l.name} — ${t.name} — VETA — ELMKUSOMA`,
    description: m.description,
  }
}

export default async function ModulePage({
  params,
}: {
  params: Promise<{ trade: string; level: string; module: string }>
}) {
  const { trade, level, module } = await params
  const tradeData = getVetaTradeBySlug(trade)
  if (!tradeData) notFound()

  const levelData = getVetaLevelBySlug(tradeData, level)
  if (!levelData) notFound()

  const moduleData = getVetaModuleBySlug(levelData, module)
  if (!moduleData) notFound()

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
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
              <span className="text-foreground">{moduleData.name}</span>
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {moduleData.name}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {tradeData.name} &middot; {levelData.name}
            </p>
            <p className="mt-2 max-w-2xl text-muted-foreground">{moduleData.description}</p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">Lessons</h2>
          <div className="mt-5 space-y-3">
            {moduleData.lessons.map((lesson, index) => (
              <Link
                key={lesson.id}
                href={`/courses/veta/${trade}/${level}/${module}/${lesson.id}`}
                className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg sm:p-5"
              >
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-sm font-bold text-primary">
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-semibold text-foreground group-hover:text-primary">
                    {lesson.title}
                  </h3>
                  <p className="mt-0.5 text-xs text-muted-foreground">{lesson.description}</p>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span className="hidden items-center gap-1 text-xs text-muted-foreground sm:inline-flex">
                    <Clock className="size-3.5" />
                    {lesson.duration}
                  </span>
                  <ChevronRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:text-primary" />
                </div>
              </Link>
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
