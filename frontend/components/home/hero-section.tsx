"use client"

import Link from "next/link"
import Image from "next/image"
import { useTranslations } from "next-intl"
import { buttonVariants } from "@/components/ui/button"
import { HeroLearnerBadge } from "@/components/home/platform-stats"
import { cn } from "@/lib/utils"

export function HeroSection() {
  const t = useTranslations("home")
  const tc = useTranslations("common")

  const highlights = [
    { title: t("hero.highlightLiveTitle"), desc: t("hero.highlightLiveDesc") },
    { title: t("hero.highlightAnywhereTitle"), desc: t("hero.highlightAnywhereDesc") },
    { title: t("hero.highlightTrustedTitle"), desc: t("hero.highlightTrustedDesc") },
  ]

  return (
    <section className="relative overflow-hidden">
      <div className="mx-auto max-w-7xl px-4 pb-12 pt-12 sm:px-6 lg:px-8 lg:pb-20 lg:pt-16">
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              <span className="size-1.5 rounded-full bg-teal" />
              {t("hero.badge")}
            </span>
            <h1 className="mt-5 text-balance text-4xl font-extrabold leading-[1.05] tracking-tight text-foreground sm:text-5xl lg:text-6xl">
              {t("hero.titleMain")} <br className="hidden sm:block" />
              {t("hero.titlePrefix")} <span className="text-teal">{t("hero.titleAccent")}</span>
            </h1>
            <p className="mt-5 max-w-md text-pretty text-base leading-relaxed text-muted-foreground">
              {t("hero.subtitle")}
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              <Link href="/register" className={cn(buttonVariants(), "h-12 px-6 text-base")}>
                {tc("startLearning")}
              </Link>
              <Link
                href="/schools/nursery"
                className={cn(buttonVariants({ variant: "outline" }), "h-12 gap-2 px-6 text-base")}
              >
                {t("hero.browseSchools")}
              </Link>
            </div>

            <dl className="mt-10 grid max-w-lg grid-cols-1 gap-6 sm:grid-cols-3">
              {highlights.map((h) => (
                <div key={h.title}>
                  <dt className="mt-1 text-sm font-semibold text-foreground">{h.title}</dt>
                  <dd className="mt-1 text-xs leading-relaxed text-muted-foreground">{h.desc}</dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="relative">
            <div className="relative overflow-hidden rounded-3xl border border-border bg-muted shadow-xl">
              <Image
                src="/images/hero-student.png"
                alt={t("hero.imageAlt")}
                width={720}
                height={620}
                priority
                className="h-full w-full object-cover"
              />
            </div>

            <HeroLearnerBadge />
          </div>
        </div>
      </div>
    </section>
  )
}
