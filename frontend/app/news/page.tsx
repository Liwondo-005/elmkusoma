"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { ArrowLeft, Newspaper } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import { formatNewsDate, newsApi, type NewsArticleView } from "@/lib/news-api"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { FeaturedBadge, NewBadge } from "@/components/home/news-section"
import { buttonVariants } from "@/components/ui/button"

/**
 * The full news archive.
 *
 * <p>Plain client-side pagination over the backend's paged endpoint, matching how the rest of the
 * public pages consume data. The three states - loading, empty, error - are all rendered
 * explicitly rather than collapsed into a blank page.</p>
 */
export default function NewsArchivePage() {
  const t = useTranslations("news")
  const tn = useTranslations("nav")
  const locale = useLocale()

  const [articles, setArticles] = useState<NewsArticleView[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  const load = useCallback(async (requested: number) => {
    setLoading(true)
    setFailed(false)
    try {
      const result = await newsApi.list(requested, 12)
      setArticles(result.content ?? [])
      setTotalPages(result.totalPages ?? 0)
      setTotal(result.totalElements ?? 0)
    } catch {
      setFailed(true)
      setArticles([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load(0)
  }, [load])

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="py-16 lg:py-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <Link
              href="/"
              className={buttonVariants({ variant: "ghost" })}
              aria-label={tn("backToHome")}
            >
              <ArrowLeft className="size-4" aria-hidden />
              {tn("backToHome")}
            </Link>

            <div className="mt-6 max-w-2xl">
              <h1 className="text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
                {t("archiveTitle")}
              </h1>
              <p className="mt-3 text-pretty text-muted-foreground">{t("archiveSubtitle")}</p>
            </div>

            {loading && (
              <p className="mt-10 text-sm text-muted-foreground" role="status">
                {t("loading")}
              </p>
            )}

            {!loading && failed && (
              <div
                role="alert"
                className="mt-10 rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
              >
                {t("loadError")}
              </div>
            )}

            {!loading && !failed && articles.length === 0 && (
              <div className="mt-10 flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border py-16 text-center">
                <Newspaper className="size-8 text-muted-foreground" aria-hidden />
                <p className="text-sm text-muted-foreground">{t("emptyState")}</p>
              </div>
            )}

            {!loading && !failed && articles.length > 0 && (
              <>
                <p className="mt-8 text-sm text-muted-foreground">{t("resultCount", { count: total })}</p>
                <ul className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                  {articles.map((article) => (
                    <li key={article.id}>
                      <ArchiveCard article={article} locale={locale} />
                    </li>
                  ))}
                </ul>
              </>
            )}

            {totalPages > 1 && (
              <nav className="mt-10 flex items-center justify-center gap-3" aria-label={t("paginationLabel")}>
                <button
                  type="button"
                  className={buttonVariants({ variant: "outline" })}
                  disabled={page === 0 || loading}
                  onClick={() => {
                    const next = page - 1
                    setPage(next)
                    load(next)
                  }}
                >
                  {t("previous")}
                </button>
                <span className="text-sm text-muted-foreground">
                  {t("pageOf", { page: page + 1, total: totalPages })}
                </span>
                <button
                  type="button"
                  className={buttonVariants({ variant: "outline" })}
                  disabled={page >= totalPages - 1 || loading}
                  onClick={() => {
                    const next = page + 1
                    setPage(next)
                    load(next)
                  }}
                >
                  {t("next")}
                </button>
              </nav>
            )}
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}

function ArchiveCard({ article, locale }: { article: NewsArticleView; locale: string }) {
  const t = useTranslations("news")
  const date = formatNewsDate(article.publishedAt, locale)

  return (
    <article className="group h-full overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-lg">
      <Link href={`/news/${article.slug}`} className="block h-full">
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
          <h2 className="mt-3 text-base font-semibold leading-snug text-foreground">{article.title}</h2>
          <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">{article.summary}</p>
          {date && (
            <p className="mt-3 text-xs text-muted-foreground">
              <time dateTime={article.publishedAt}>{date}</time>
            </p>
          )}
        </div>
      </Link>
    </article>
  )
}