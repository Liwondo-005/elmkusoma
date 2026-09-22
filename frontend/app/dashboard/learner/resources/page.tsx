"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Resource } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { FileText, Video, Music, Image, Download, ExternalLink, Search, Filter, AlertCircle, Bookmark, BookmarkCheck, ArrowRight } from "lucide-react"
import Link from "next/link"

export default function LearnerResourcesPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [resources, setResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState<string>("all")
  const [bookmarkedIds, setBookmarkedIds] = useState<Set<string>>(new Set())
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadResources()
  }, [user, page])

  async function loadResources() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getResources({ page, size: 20 })
      setResources(data)
      if (data.length < 20 && page > 1) {
        setTotalPages(page)
      } else if (data.length === 20) {
        setTotalPages(page + 1)
      } else {
        setTotalPages(page)
      }

      try {
        const bookmarks = await learnerApi.getBookmarks()
        const resourceBookmarks = new Set(
          bookmarks.filter((b) => b.targetType === "resource").map((b) => b.targetId)
        )
        setBookmarkedIds(resourceBookmarks)
      } catch {
        // Ignore bookmark check failure
      }
    } catch {
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }

  async function toggleBookmark(resourceId: string) {
    try {
      if (bookmarkedIds.has(resourceId)) {
        const bookmarks = await learnerApi.getBookmarks()
        const existing = bookmarks.find((b) => b.targetType === "resource" && b.targetId === resourceId)
        if (existing) {
          await learnerApi.removeBookmark(existing.id)
        }
        setBookmarkedIds((prev) => {
          const next = new Set(prev)
          next.delete(resourceId)
          return next
        })
      } else {
        await learnerApi.addBookmark("resource", resourceId)
        setBookmarkedIds((prev) => new Set(prev).add(resourceId))
      }
    } catch {
      // Silent fail for bookmark toggle
    }
  }

  function getResourceIcon(type: string) {
    switch (type?.toUpperCase()) {
      case "VIDEO": return <Video className="size-5 text-red-500" />
      case "AUDIO": return <Music className="size-5 text-purple-500" />
      case "IMAGE": return <Image className="size-5 text-blue-500" />
      default: return <FileText className="size-5 text-teal" />
    }
  }

  function getResourceTypeBadge(type: string) {
    const colors: Record<string, string> = {
      DOCUMENT: "bg-blue-500/10 text-blue-500",
      VIDEO: "bg-red-500/10 text-red-500",
      AUDIO: "bg-purple-500/10 text-purple-500",
      IMAGE: "bg-orange/10 text-orange",
    }
    return colors[type?.toUpperCase()] || "bg-muted text-muted-foreground"
  }

  const types = [...new Set(resources.map((r) => r.resourceType).filter(Boolean))]

  const filteredResources = resources.filter((resource) => {
    const matchesSearch = search === "" ||
      resource.title.toLowerCase().includes(search.toLowerCase()) ||
      (resource.description?.toLowerCase().includes(search.toLowerCase()) ?? false)
    const matchesType = typeFilter === "all" || resource.resourceType === typeFilter
    return matchesSearch && matchesType
  })

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>
  }

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("resources.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("resources.subtitle")}</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
            <button onClick={() => { setError(null); loadResources() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("resources.searchPlaceholder")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label={t("resources.searchPlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          aria-label={t("resources.filterType")}
          className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
        >
          <option value="all">{tc("allTypes")}</option>
          {types.map((type) => (
            <option key={type} value={type}>{type}</option>
          ))}
        </select>
      </div>

      <div aria-live="polite" aria-busy={loading}>
      {loading ? (
        <div aria-busy="true"><LoadingState /></div>
      ) : filteredResources.length === 0 ? (
        <div role="status">
        <EmptyState
          icon={<FileText className="size-8" />}
          title={t("resources.noResources")}
          description={search ? t("resources.noResourcesSearch") : t("resources.noResourcesAvailable")}
        />
        </div>
      ) : (
        <>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredResources.map((resource) => (
            <Link
              key={resource.id}
              href={`/dashboard/learner/resources/${resource.id}`}
              aria-label={`${resource.title} - ${tc("viewDetails")}`}
              className="rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30 block"
            >
              <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-muted">
                  {getResourceIcon(resource.resourceType)}
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-semibold text-foreground truncate">{resource.title}</h3>
                  {resource.description && (
                    <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{resource.description}</p>
                  )}
                </div>
              </div>
              <div className="mt-3 flex items-center justify-between">
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getResourceTypeBadge(resource.resourceType)}`}>
                  {resource.resourceType}
                </span>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={(e) => { e.preventDefault(); toggleBookmark(resource.id) }}
                    aria-label={bookmarkedIds.has(resource.id) ? tc("removeBookmark") : tc("bookmark")}
                    className="inline-flex items-center justify-center rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  >
                    {bookmarkedIds.has(resource.id) ? (
                      <BookmarkCheck className="size-4 text-primary" />
                    ) : (
                      <Bookmark className="size-4" />
                    )}
                  </button>
                  <a
                    href={resource.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`${tc("download")} ${resource.title}`}
                    onClick={(e) => e.stopPropagation()}
                    className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    <Download className="size-3" />
                    {tc("download")}
                  </a>
                </div>
              </div>
              <p className="mt-2 text-[10px] text-muted-foreground">
                Added {new Date(resource.createdAt).toLocaleDateString()}
              </p>
            </Link>
          ))}
        </div>
        <div className="flex items-center justify-between pt-4">
          <span className="text-xs text-muted-foreground">Page {page} of {totalPages}</span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page <= 1}
              aria-label="Previous page"
              className="rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages}
              aria-label="Next page"
              className="rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
        </>
      )}
      </div>

      <div className="rounded-xl border border-border bg-card p-6 text-center">
        <p className="text-sm text-muted-foreground">Can't find what you need?</p>
        <Link href="/dashboard/learner/search" className="mt-2 inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline">
          Try Search <ArrowRight className="size-3" />
        </Link>
      </div>
    </div>
  )
}
