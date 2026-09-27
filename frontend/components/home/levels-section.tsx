"use client"

import Link from "next/link"
import { ArrowRight } from "lucide-react"
import { useTranslations } from "next-intl"
import { levels } from "@/lib/data"

const levelKeys = ["nursery", "primary", "secondary", "college", "vocational", "university"] as const

export function LevelsSection() {
  const t = useTranslations("home")

  return (
    <section className="bg-muted/50 py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">
            {t("levels.title")}
          </h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            {t("levels.subtitle")}
          </p>
        </div>

        <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {levels.map((level, i) => {
            const key = levelKeys[i] ?? levelKeys[0]
            return (
              <Link
                key={level.name}
                href={level.href}
                className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-all hover:border-primary/40 hover:shadow-md"
              >
                <div className="min-w-0 flex-1">
                  <h3 className="text-base font-semibold text-foreground">{t(`levels.items.${key}.name`)}</h3>
                  <p className="truncate text-sm text-muted-foreground">{t(`levels.items.${key}.desc`)}</p>
                </div>
                <ArrowRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-primary" />
              </Link>
            )
          })}
        </div>
      </div>
    </section>
  )
}
