import { appFetch } from "@/lib/fetch"

/**
 * Public news and announcements.
 *
 * Contract: GET /v1/public/news, /v1/public/news/latest, /v1/public/news/{slug}
 *
 * The backend decides what is public. There is no status or date filter on these calls because
 * there is nothing a client could usefully narrow - a draft is invisible to the query itself, and
 * `isNew` arrives already computed so the badge cannot drift from the configured window.
 */

export interface NewsArticleView {
  id: string
  slug: string
  title: string
  summary: string
  /** Only present on the single-article endpoint; list responses omit it. */
  body?: string
  category?: string
  coverImageUrl?: string
  authorName?: string
  publishedAt?: string
  /** Null unless the article has genuinely been edited since publication. */
  updatedAt?: string
  featured: boolean
  priority: "NORMAL" | "IMPORTANT" | "URGENT"
  /** Server-computed against the configured NEW window. */
  new: boolean
}

export interface NewsPage {
  content: NewsArticleView[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export const NEWS_PRIORITIES = ["NORMAL", "IMPORTANT", "URGENT"] as const

export const newsApi = {
  /** Published news, paginated. Throws so the archive page can show an honest error state. */
  list: (page = 0, size = 12): Promise<NewsPage> =>
    appFetch<NewsPage>(`/v1/public/news?page=${page}&size=${size}`),

  /**
   * Most recent news for the landing page.
   *
   * Resolves to `null` on failure rather than throwing, so a backend outage removes the section
   * instead of showing a visitor an error box on the marketing page. An empty array is a
   * different thing and is returned as one.
   */
  latest: async (limit = 3): Promise<NewsArticleView[] | null> => {
    try {
      const page = await appFetch<NewsArticleView[]>(`/v1/public/news/latest?limit=${limit}`)
      return Array.isArray(page) ? page : null
    } catch {
      return null
    }
  },

  /** A single published article. Throws on 404, which the caller renders as "not found". */
  bySlug: (slug: string): Promise<NewsArticleView> =>
    appFetch<NewsArticleView>(`/v1/public/news/${encodeURIComponent(slug)}`),
}

/** Formats an ISO timestamp for display, falling back to the raw value if unparseable. */
export function formatNewsDate(iso: string | undefined, locale: string): string {
  if (!iso) return ""
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ""
  return new Intl.DateTimeFormat(locale === "sw" ? "sw-TZ" : "en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(date)
}