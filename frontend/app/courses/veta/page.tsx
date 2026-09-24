"use client"

import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { VetaTradeCard } from "@/components/courses/veta-trade-card"
import { vetaTrades } from "@/lib/data"
import Link from "next/link"


export default function VetaPage() {
  const t = useTranslations("public")
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <Link
              href="/courses"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              {t("coursesHome.allLevels")}
            </Link>
            <span className="mt-4 inline-flex items-center gap-1.5 rounded-md bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-700">
              {t("veta.badge")}
            </span>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              {t("veta.title")}
            </h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">
              {t("veta.subtitle")}
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {vetaTrades.map((trade) => (
              <VetaTradeCard key={trade.id} trade={trade} />
            ))}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
