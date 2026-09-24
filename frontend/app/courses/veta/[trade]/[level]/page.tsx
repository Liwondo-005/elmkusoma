"use client"

import { notFound, useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"

import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { getVetaTradeBySlug, getVetaLevelBySlug } from "@/lib/data"

export default function VetaLevelPage() {
  const t = useTranslations("public")
  const params = useParams()
  const trade = params.trade as string
  const level = params.level as string
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
              <Link href="/courses" className="transition-colors hover:text-foreground">{t("coursesHome.title")}</Link>
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
              {t("vetaLevel.summary", { modules: levelData.modules.length, lessons: totalLessons })}
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <h2 className="text-lg font-semibold text-foreground">{t("vetaLevel.modulesTitle")}</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {levelData.modules.map((mod) => (
              <Link
                key={mod.id}
                href={`/courses/veta/${trade}/${level}/${mod.id}`}
                className="group flex flex-col rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="flex items-start justify-between">
                  <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                    {t("vetaLevel.lessonsCount", { count: mod.lessons.length })}
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
