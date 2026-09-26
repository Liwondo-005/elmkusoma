"use client"

import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"

export default function TermsPage() {
  const t = useTranslations("public")
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">{t("terms.title")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">{t("terms.updated")}</p>

          <div className="mt-10 space-y-8 text-sm leading-relaxed text-muted-foreground">
            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s1t")}</h2>
              <p className="mt-3">{t("terms.s1b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s2t")}</h2>
              <p className="mt-3">{t("terms.s2b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s3t")}</h2>
              <p className="mt-3">{t("terms.s3b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s4t")}</h2>
              <p className="mt-3">{t("terms.s4b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s5t")}</h2>
              <p className="mt-3">{t("terms.s5b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s6t")}</h2>
              <p className="mt-3">{t("terms.s6b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s7t")}</h2>
              <p className="mt-3">{t("terms.s7b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s8t")}</h2>
              <p className="mt-3">{t("terms.s8b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s9t")}</h2>
              <p className="mt-3">{t("terms.s9b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s10t")}</h2>
              <p className="mt-3">{t("terms.s10b")}</p>
            </div>

            <div>
              <h2 className="text-base font-semibold text-foreground">{t("terms.s11t")}</h2>
              <p className="mt-3">
                {t("terms.s11pre")}{" "}
                <a href="mailto:info@elmkusoma.co.tz" className="text-primary hover:underline">
                  info@elmkusoma.co.tz
                </a>{" "}
                {t("terms.s11post")}
              </p>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}
