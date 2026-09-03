import { notFound } from "next/navigation"
import Link from "next/link"
import { BookOpen, FileText } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { vetaTrades, getVetaTradeBySlug, getVetaLevelBySlug } from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  const params: { trade: string; level: string }[] = []
  for (const trade of vetaTrades) {
    for (const level of trade.levels) {
      params.push({ trade: trade.id, level: level.id })
    }
  }
  return params
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ trade: string; level: string }>
}): Promise<Metadata> {
  const { trade, level } = await params
  const t = getVetaTradeBySlug(trade)
  if (!t) return { title: "Not Found — ELMKUSOMA" }
  const l = getVetaLevelBySlug(t, level)
  if (!l) return { title: "Not Found — ELMKUSOMA" }
  return {
    title: `${l.name} — ${t.name} — VETA — ELMKUSOMA`,
    description: l.description,
  }
}

export default async function LevelPage({
  params,
}: {
  params: Promise<{ trade: string; level: string }>
}) {
  const { trade, level } = await params
  const tradeData = getVetaTradeBySlug(trade)
  if (!tradeData) notFound()

  const levelData = getVetaLevelBySlug(tradeData, level)
  if (!levelData) notFound()

  const totalLessons = levelData.modules.reduce((a, m) => a + m.lessons.length, 0)

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
              <span className="text-foreground">{levelData.name}</span>
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {levelData.name}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">{tradeData.name}</p>
            <p className="mt-2 max-w-2xl text-muted-foreground">{levelData.description}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {levelData.modules.length} {levelData.modules.length === 1 ? "module" : "modules"} &middot;{" "}
              {totalLessons} {totalLessons === 1 ? "lesson" : "lessons"}
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">Modules</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {levelData.modules.map((mod) => (
              <Link
                key={mod.id}
                href={`/courses/veta/${trade}/${level}/${mod.id}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="flex items-start justify-between">
                  <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <BookOpen className="size-4" />
                  </span>
                  <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                    <FileText className="size-3" />
                    {mod.lessons.length} {mod.lessons.length === 1 ? "lesson" : "lessons"}
                  </span>
                </div>
                <h3 className="mt-3 text-base font-semibold text-foreground group-hover:text-primary">
                  {mod.name}
                </h3>
                <p className="mt-1 flex-1 text-sm text-muted-foreground">{mod.description}</p>
              </Link>
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
