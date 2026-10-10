"use client"

import { useEffect, useState } from "react"
import Image from "next/image"
import Link from "next/link"
import { ArrowRight, Newspaper, Star } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import { formatNewsDate, newsApi, type NewsArticleView } from "@/lib/news-api"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/**
 * Real published news on the landing page.
 *
 * <p>Ordering is the backend's: featured first, then importance, then recency. This component does
 * not re-sort, because a second sort here would quietly disagree with /news the moment the two
 * drifted apart.</p>
 *
 * <p>Hides itself entirely when there is no news or the backend is unreachable, matching
 * CoursesSection. An empty marketing section reads as a broken page; an absent one does not.</p>
 */
export function NewsSection() {
  const t = useTranslations("news")
  const tn = useTranslations("nav")

  const [articles, setArticles] = useState<NewsArticleView[] | null>(null)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    let active = true
    newsApi.latest(3).then((result) => {
      if (!active) return
      setArticles(result)
      setLoaded(true)
    })
    return () => {
      active = false
    }
  }, [])

  // Still loading, nothing published, or the request failed: render nothing.
  if (!loaded || !articles || articles.length === 0) return null

  const [lead, ...rest] = articles

  return (
    <section className="py-16 lg:py-20">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl font-bold tracking-tight text-foreground">{t("sectionTitle")}</h2>
            <p className="mt-2 max-w-xl text-muted-foreground">{t("sectionSubtitle")}</p>
          </div>
          <Link href="/news" className={cn(buttonVariants({ variant: "outline" }), "h-10 gap-2 px-4")}>
            {tn("viewAllNews")}
            <ArrowRight className="size-4" aria-hidden />
          </Link>
        </div>

        <div className="mt-10 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {lead && <NewsCard article={lead} lead />}
          {rest.map((article) => (
            <NewsCard key={article.id} article={article} />
          ))}
        </div>
      </div>
    </section>
  )
}

function NewsCard({ article, lead = false }: { article: NewsArticleView; lead?: boolean }) {
  const t = useTranslations("news")
  const locale = useLocale()
  const date = formatNewsDate(article.publishedAt, locale)

  return (
    <article className="group overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg">
      <Link href={`/news/${article.slug}`} className="block h-full">
        <div className="relative aspect-[16/10] overflow-hidden bg-muted">
          {article.coverImageUrl ? (
            <Image
              src={article.coverImageUrl}
              alt=""
              fill
              className="object-cover transition-transform duration-500 group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, 33vw"
            />
          ) : (
            // No cover image is a normal state, not an error, so it gets a designed placeholder
            // rather than a broken-image icon.
            <div className="flex size-full items-center justify-center text-muted-foreground">
              <Newspaper className="size-8" aria-hidden />
            </div>
          )}
        </div>

        <div className="p-5">
          <div className="flex flex-wrap items-center gap-2">
            {article.new && <NewBadge />}
            {article.featured && <FeaturedBadge />}
            {article.category && (
              <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                {article.category}
              </span>
            )}
          </div>

          <h3 className={cn("mt-3 font-semibold leading-snug text-foreground", lead ? "text-lg" : "text-base")}>
            {article.title}
          </h3>
          <p className={cn("mt-2 text-muted-foreground", lead ? "line-clamp-3 text-base" : "line-clamp-2 text-sm")}>
            {article.summary}
          </p>

          {date && (
            <p className="mt-3 text-xs text-muted-foreground">
              <time dateTime={article.publishedAt}>{date}</time>
            </p>
          )}

          <span className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-primary">
            {t("readMore")}
            <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden />
          </span>
        </div>
      </Link>
    </article>
  )
}

/**
 * NEW is a time window, not a quality signal, so it reads as a quiet accent. Exported because the
 * archive and article pages need the same mark.
 */
export function NewBadge() {
  const t = useTranslations("news")
  return (
    <span className="inline-flex items-center gap-1 rounded-md bg-teal/10 px-2 py-0.5 text-xs font-semibold text-teal">
      <span className="size-1.5 rounded-full bg-teal" aria-hidden />
      {t("newBadge")}
    </span>
  )
}

/**
 * Featured is a deliberate editorial mark, so it carries a star and real visible text rather than
 * an icon alone - a colour and a glyph are not enough to convey meaning to every user.
 */
export function FeaturedBadge() {
  const t = useTranslations("news")
  return (
    <span className="inline-flex items-center gap-1 rounded-md bg-orange/10 px-2 py-0.5 text-xs font-semibold text-orange">
      <Star className="size-3 fill-current" aria-hidden />
      {t("featuredBadge")}
    </span>
  )
}