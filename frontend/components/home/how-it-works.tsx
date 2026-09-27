"use client"

import { useTranslations } from "next-intl"

export function HowItWorks() {
  const t = useTranslations("home")

  const steps = [
    { title: t("how.step1Title"), desc: t("how.step1Desc") },
    { title: t("how.step2Title"), desc: t("how.step2Desc") },
    { title: t("how.step3Title"), desc: t("how.step3Desc") },
    { title: t("how.step4Title"), desc: t("how.step4Desc") },
  ]

  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance text-3xl font-bold tracking-tight text-foreground">{t("how.title")}</h2>
          <p className="mt-3 text-pretty text-muted-foreground">
            {t("how.subtitle")}
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((step, i) => (
            <div key={step.title} className="relative rounded-2xl border border-border bg-card p-6">
              <span className="absolute right-5 top-5 flex size-9 items-center justify-center rounded-full bg-primary/10 text-lg font-extrabold text-primary">{i + 1}</span>
              <h3 className="text-base font-semibold text-foreground">{step.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
