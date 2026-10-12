import { cookies } from "next/headers"
import { getTranslations } from "next-intl/server"
import Image from "next/image"
import { AboutHighlights } from "@/components/home/platform-stats"

export async function AboutHero() {
  const cookieStore = await cookies()
  const cookieLocale = cookieStore.get("NEXT_LOCALE")?.value
  const t = await getTranslations({ locale: cookieLocale === "sw" ? "sw" : "en", namespace: "about" })

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
              {t("hero.titlePrefix")}
              <br className="hidden sm:block" />
              {t("hero.titleSecond")} <span className="text-teal">{t("hero.titleAccent")}</span>
            </h1>
            <p className="mt-3 text-sm font-medium text-muted-foreground">
              {t("hero.foundedBy")} <span className="font-semibold text-foreground">Eng. Arthur Assenga</span>
            </p>
            <p className="mt-4 max-w-lg text-pretty text-base leading-relaxed text-muted-foreground">
              {t("hero.description")}
            </p>

            <AboutHighlights />
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
          </div>
        </div>
      </div>
    </section>
  )
}
