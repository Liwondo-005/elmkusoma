"use client"

import Link from "next/link"
import { useTranslations } from "next-intl"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function CollegeCta() {
  const t = useTranslations("schools")
  const tc = useTranslations("common")
  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="flex flex-col justify-between overflow-hidden rounded-2xl bg-primary p-8 text-primary-foreground">
            <div>
              <h2 className="text-2xl font-bold">{t("collegeCta.exploreTitle")}</h2>
              <p className="mt-3 text-primary-foreground/80">
                {t("collegeCta.exploreDescription")}
              </p>
            </div>
            <Link
              href="/schools/colleges/dit"
              className={cn(
                buttonVariants(),
                "mt-8 h-11 w-full gap-2 bg-background text-foreground hover:bg-background/90 sm:w-auto sm:self-start",
              )}
            >
              {tc("startLearning")}
            </Link>
          </div>

          <div className="flex flex-col justify-between overflow-hidden rounded-2xl border border-border bg-card p-8">
            <div>
              <h2 className="text-2xl font-bold text-foreground">{t("collegeCta.applyTitle")}</h2>
              <p className="mt-3 text-muted-foreground">
                {t("collegeCta.applyDescription")}
              </p>
            </div>
            <Link
              href="/register"
              className={cn(
                buttonVariants({ variant: "outline" }),
                "mt-8 h-11 w-full gap-2 border-primary/30 text-primary hover:bg-accent sm:w-auto sm:self-start",
              )}
            >
              {t("collegeCta.applyNow")}
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}
