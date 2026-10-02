"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Building2, GraduationCap, MapPin, Search, User, Users, Briefcase, BookOpen, ShieldCheck } from "lucide-react"
import { useTranslations } from "next-intl"
import { regionalAdminApi, type SearchResult } from "@/lib/regional-admin-api"
import { Chip, EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/dashboard/regional-admin/ui"

const TYPE_ICONS: Record<string, typeof MapPin> = {
  DISTRICT: MapPin,
  INSTITUTION: Building2,
  LEARNER: GraduationCap,
  TEACHER: Users,
  EDUCATION_STAFF: Briefcase,
  COURSE: BookOpen,
}

function ResultRow({ item }: { item: SearchResult }) {
  const Icon = TYPE_ICONS[item.type] ?? (item.type.includes("STUDENT") || item.type.includes("LEARNER") ? GraduationCap : item.type === "TEACHER" || item.type === "INSTRUCTOR" ? Users : item.type === "INSTITUTION_ADMIN" ? Briefcase : item.type.startsWith("REGIONAL") || item.type.startsWith("DISTRICT") ? ShieldCheck : User)
  return (
    <Link
      href={item.route}
      className="flex items-start gap-3 rounded-2xl border border-border bg-card p-4 shadow-sm transition-colors hover:bg-accent"
    >
      <span className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
        <Icon className="size-4" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-bold text-foreground">{item.title}</span>
          <Chip tone="info">{item.type}</Chip>
        </span>
        {item.subtitle ? (
          <span className="mt-0.5 block truncate text-xs text-muted-foreground">{item.subtitle}</span>
        ) : null}
      </span>
    </Link>
  )
}

export default function RegionalSearchPage() {
  const t = useTranslations("regionalAdmin")
  const params = useSearchParams()
  const router = useRouter()
  const [input, setInput] = useState(params.get("q") ?? "")
  const [query, setQuery] = useState(params.get("q") ?? "")
  const [results, setResults] = useState<SearchResult[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searched, setSearched] = useState(false)

  const load = useCallback(async (q: string) => {
    const term = q.trim()
    if (term.length < 2) {
      setResults([])
      setSearched(false)
      setError(null)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await regionalAdminApi.search(term, 30)
      setResults(res)
      setSearched(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : t("searchPage.unavailableError"))
      setResults([])
      setSearched(true)
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    const handle = window.setTimeout(() => {
      if (input.trim() !== query.trim()) {
        setQuery(input.trim())
        router.replace(input.trim() ? `/dashboard/regional-admin/search?q=${encodeURIComponent(input.trim())}` : "/dashboard/regional-admin/search")
      }
    }, 350)
    return () => window.clearTimeout(handle)
  }, [input, query, router])

  useEffect(() => {
    load(query)
  }, [load, query])

  return (
    <div className="space-y-4">
      <PageHeader
        title={t("searchPage.title")}
        description={t("searchPage.description")}
      />

      <form
        role="search"
        onSubmit={(e) => {
          e.preventDefault()
          setQuery(input.trim())
          router.replace(input.trim() ? `/dashboard/regional-admin/search?q=${encodeURIComponent(input.trim())}` : "/dashboard/regional-admin/search")
        }}
        className="relative"
      >
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="search"
          autoFocus
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={t("searchPage.inputPlaceholder")}
          aria-label={t("searchPage.inputAriaLabel")}
          className="w-full rounded-xl border border-border bg-background py-3 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-ring"
        />
      </form>

      {error && <ErrorState message={error} onRetry={() => load(query)} />}

      {loading ? (
        <LoadingState label={t("searchPage.searchingLabel")} />
      ) : !searched ? (
        <EmptyState
          title={t("searchPage.startTitle")}
          hint={t("searchPage.startHint")}
          icon={<Search className="size-10" />}
        />
      ) : results.length === 0 ? (
        <EmptyState
          title={t("searchPage.noMatchesTitle")}
          hint={t("searchPage.noMatchesHint")}
          icon={<Search className="size-10" />}
        />
      ) : (
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            {t("searchPage.resultsCount", { count: results.length })}
          </p>
          {results.map((item) => (
            <ResultRow key={`${item.type}-${item.id}`} item={item} />
          ))}
        </div>
      )}
    </div>
  )
}
