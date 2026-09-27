"use client"

import { useTranslations } from "next-intl"

export function VocationalHero() {
  const t = useTranslations("schools")
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-accent via-background to-accent/50">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="max-w-3xl">
          <span className="inline-flex items-center gap-1.5 rounded-md bg-teal/10 px-2.5 py-1 text-xs font-semibold text-teal">
            {t("vocationalHero.badge")}
          </span>
          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            {t("vocationalHero.title")}
          </h1>
          <p className="mt-4 max-w-xl text-lg text-muted-foreground">
            {t("vocationalHero.description")}
          </p>

          <div className="mt-8 flex flex-wrap gap-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">{t("vocationalHero.stat1Value")}</span>
              <span>{t("vocationalHero.stat1Label")}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">{t("vocationalHero.stat2Value")}</span>
              <span>{t("vocationalHero.stat2Label")}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">{t("vocationalHero.stat3Value")}</span>
              <span>{t("vocationalHero.stat3Label")}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
