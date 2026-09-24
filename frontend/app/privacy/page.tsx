"use client"

import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"

export default function PrivacyPage() {
  const t = useTranslations("public")
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">{t("privacy.title")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">{t("privacy.updated")}</p>

          <div className="mt-10 space-y-8 text-sm leading-relaxed text-muted-foreground">
            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s1t")}</h2>
              <p className="mt-3">{t("privacy.s1b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s2t")}</h2>
              <p className="mt-3">{t("privacy.s2b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s3t")}</h2>
              <p className="mt-3">{t("privacy.s3b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s4t")}</h2>
              <p className="mt-3">{t("privacy.s4b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s5t")}</h2>
              <p className="mt-3">{t("privacy.s5b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s6t")}</h2>
              <p className="mt-3">{t("privacy.s6b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s7t")}</h2>
              <p className="mt-3">{t("privacy.s7b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s8t")}</h2>
              <p className="mt-3">{t("privacy.s8b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("privacy.s9t")}</h2>
              <p className="mt-3">
                {t("privacy.s9pre")}{" "}
                <a href="mailto:info@elmkusoma.co.tz" className="text-primary hover:underline">
                  info@elmkusoma.co.tz
                </a>{" "}
                {t("privacy.s9post")}
              </p>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}
