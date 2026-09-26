"use client"

import { notFound, useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { getVetaTradeBySlug } from "@/lib/data"

export default function TradePage() {
  const t = useTranslations("public")
  const params = useParams()
  const trade = params.trade as string
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
              <Link href="/courses" className="transition-colors hover:text-foreground">{t("coursesHome.title")}</Link>
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
                {t("vetaTrade.levelsCount", { count: tradeData.levels.length })}
              </span>
              <span className="inline-flex items-center gap-1.5">
                {t("vetaTrade.modulesCount", { count: totalModules })}
              </span>
              <span className="inline-flex items-center gap-1.5">
                {t("vetaTrade.lessonsCount", { count: totalLessons })}
              </span>
            </div>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">{t("vetaTrade.levelsTitle")}</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {tradeData.levels.map((level) => (
              <Link
                key={level.id}
                href={`/courses/veta/${trade}/${level.id}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="flex items-start justify-between">
                  <span className="text-xs text-muted-foreground">
                    {t("vetaTrade.modulesCount", { count: level.modules.length })}
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
