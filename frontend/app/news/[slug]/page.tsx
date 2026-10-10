"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { ArrowLeft, Newspaper } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import { formatNewsDate, newsApi, type NewsArticleView } from "@/lib/news-api"
import { isApiError } from "@/lib/fetch"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { FeaturedBadge, NewBadge } from "@/components/home/news-section"
import { buttonVariants } from "@/components/ui/button"

/**
 * A single public news article at /news/{slug}.
 *
 * <p>Client component with useParams(), which is how every other data-backed public detail page in
 * this app works (see /live-classes/[id] and /certificates/verify/[code]). The slug is shareable:
 * it is the whole URL, so a refresh re-runs the same fetch.</p>
 *
 * <p>The body is rendered as React text nodes, split on blank lines. There is no
 * dangerouslySetInnerHTML anywhere in this path, and the server refuses markup in the first place -
 * two independent barriers, either of which alone would make stored XSS impossible.</p>
 */
export default function NewsArticlePage() {
  const t = useTranslations("news")
  const tn = useTranslations("nav")
  const locale = useLocale()
  const params = useParams()
  const slug = typeof params?.slug === "string" ? decodeURIComponent(params.slug) : ""

  const [article, setArticle] = useState<NewsArticleView | null>(null)
  const [state, setState] = useState<"loading" | "ready" | "missing" | "error">("loading")

  useEffect(() => {
    if (!slug) return
    let active = true
    newsApi
      .bySlug(slug)
      .then((result) => {
        if (!active) return
        setArticle(result)
        setState("ready")
      })
      .catch((error: unknown) => {
        if (!active) return
        // A 404 means no such published article - a real state with its own page, not a failure
        // worth shouting about. Distinguished by HTTP status rather than by matching the message,
        // because the backend's 404 body reads "NewsArticle not found with slug: ..." and any
        // other failure could carry a similar sentence.
        setState(isApiError(error, 404) ? "missing" : "error")
      })
    return () => {
      active = false
    }
  }, [slug])

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <article className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:py-20">
          <Link href="/news" className={buttonVariants({ variant: "ghost" })}>
            <ArrowLeft className="size-4" aria-hidden />
            {tn("backToNews")}
          </Link>

          {state === "loading" && (
            <p className="mt-8 text-sm text-muted-foreground" role="status">
              {t("loading")}
            </p>
          )}

          {state === "missing" && (
            <div className="mt-10 flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border py-16 text-center">
              <Newspaper className="size-8 text-muted-foreground" aria-hidden />
              <h1 className="text-lg font-semibold text-foreground">{t("notFoundTitle")}</h1>
              <p className="text-sm text-muted-foreground">{t("notFoundBody")}</p>
              <Link href="/news" className={buttonVariants({ variant: "outline" })}>
                {tn("viewAllNews")}
              </Link>
            </div>
          )}

          {state === "error" && (
            <div
              role="alert"
              className="mt-10 rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
            >
              {t("loadError")}
            </div>
          )}

          {state === "ready" && article && (
            <div className="mt-8">
              <div className="flex flex-wrap items-center gap-2">
                {article.new && <NewBadge />}
                {article.featured && <FeaturedBadge />}
                {article.category && (
                  <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                    {article.category}
                  </span>
                )}
              </div>

              <h1 className="mt-4 text-balance text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
                {article.title}
              </h1>
              <p className="mt-3 text-lg text-muted-foreground">{article.summary}</p>

              <div className="mt-5 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
                {article.publishedAt && (
                  <time dateTime={article.publishedAt}>
                    {formatNewsDate(article.publishedAt, locale)}
                  </time>
                )}
                {article.updatedAt && (
                  <span>{t("lastUpdated", { date: formatNewsDate(article.updatedAt, locale) })}</span>
                )}
                {article.authorName && <span>{article.authorName}</span>}
              </div>

              {/* Plain text only. See the class comment for why there is no HTML rendering here. */}
              <div className="mt-8 space-y-4">
                {(article.body ?? "")
                  .split(/\n\s*\n/)
                  .map((block) => block.trim())
                  .filter((block) => block.length > 0)
                  .map((paragraph, index) => (
                    <p key={index} className="leading-relaxed text-foreground">
                      {paragraph}
                    </p>
                  ))}
              </div>
            </div>
          )}
        </article>
      </main>
      <SiteFooter />
    </div>
  )
}