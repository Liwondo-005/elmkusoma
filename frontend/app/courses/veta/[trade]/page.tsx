import { notFound } from "next/navigation"
import Link from "next/link"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { vetaTrades, getVetaTradeBySlug } from "@/lib/data"
import type { Metadata } from "next"

export function generateStaticParams() {
  return vetaTrades.map((t) => ({ trade: t.id }))
}

export async function generateMetadata({ params }: { params: Promise<{ trade: string }> }): Promise<Metadata> {
  const { trade } = await params
  const t = getVetaTradeBySlug(trade)
  if (!t) return { title: "Trade Not Found — ELMKUSOMA" }
  return {
    title: `${t.name} — VETA — ELMKUSOMA`,
    description: t.description,
  }
}

export default async function TradePage({ params }: { params: Promise<{ trade: string }> }) {
  const { trade } = await params
  const tradeData = getVetaTradeBySlug(trade)
  if (!tradeData) notFound()

  const totalModules = tradeData.levels.reduce((a, l) => a + l.modules.length, 0)
  const totalLessons = tradeData.levels.reduce(
    (a, l) => a + l.modules.reduce((b, m) => b + m.lessons.length, 0),
    0
  )

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
              <span className="text-foreground">{tradeData.name}</span>
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {tradeData.name}
            </h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">{tradeData.description}</p>
            <div className="mt-3 flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-1.5">
                {tradeData.levels.length} levels
              </span>
              <span className="inline-flex items-center gap-1.5">
                {totalModules} modules
              </span>
              <span className="inline-flex items-center gap-1.5">
                {totalLessons} lessons
              </span>
            </div>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">Training Levels</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {tradeData.levels.map((level) => (
              <Link
                key={level.id}
                href={`/courses/veta/${trade}/${level.id}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="flex items-start justify-between">
                  <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {level.modules.length} {level.modules.length === 1 ? "module" : "modules"}
                  </span>
                </div>
                <h3 className="mt-3 text-base font-semibold text-foreground group-hover:text-primary">
                  {level.name}
                </h3>
                <p className="mt-1 flex-1 text-sm text-muted-foreground">{level.description}</p>
              </Link>
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
