"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type Bookmark, type Resource } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Bookmark as BookmarkIcon, FileText, Video, BookOpen, Search, AlertCircle } from "lucide-react"

type Tab = "bookmarks" | "resources"

export default function MyLearningKitPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [tab, setTab] = useState<Tab>("bookmarks")
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([])
  const [resources, setResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [bookmarksRes, resourcesRes] = await Promise.all([
        learnerApi.getBookmarks().catch(() => []),
        learnerApi.getResources().catch(() => []),
      ])
      setBookmarks(bookmarksRes)
      setResources(resourcesRes)
    } catch {
      setError(t("kit.loadError"))
    } finally {
      setLoading(false)
    }
  }

  function getResourceIcon(type: string) {
    switch (type?.toLowerCase()) {
      case "video": return <Video className="size-4 text-red-500" />
      case "document": return <FileText className="size-4 text-blue-500" />
      default: return <BookOpen className="size-4 text-teal" />
    }
  }

  function getBookmarkLink(b: Bookmark) {
    switch (b.targetType?.toLowerCase()) {
      case "course": return `/dashboard/learner/courses/${b.targetId}`
      case "resource": return `/dashboard/learner/resources`
      default: return "#"
    }
  }

  if (authLoading || loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || t("kit.learnerFallback")} subtitle={t("kit.subtitle")} />
        <div className="rounded-2xl border border-border bg-card p-4 text-sm text-red-600 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadData() }} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || t("kit.learnerFallback")
  const filteredBookmarks = bookmarks.filter((b) => b.targetTitle?.toLowerCase().includes(search.toLowerCase()))
  const filteredResources = resources.filter((r) => r.title?.toLowerCase().includes(search.toLowerCase()))

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("kit.subtitle")} />

      <div className="flex items-center gap-4 border-b border-border">
        {(["bookmarks", "resources"] as Tab[]).map((tabId) => (
          <button
            key={tabId}
            onClick={() => setTab(tabId)}
            className={`border-b-2 pb-3 text-sm font-medium transition ${
              tab === tabId ? "border-primary text-foreground" : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            {tab === "bookmarks" ? t("kit.tabBookmarks") : t("kit.tabResources")}
          </button>
        ))}
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder={t("kit.searchPlaceholder", { tab })}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
        />
      </div>

      {tab === "bookmarks" && (
        filteredBookmarks.length === 0 ? (
          <EmptyState
            icon={<BookmarkIcon className="size-8" />}
            title={t("marks.emptyTitle")}
            description={t("kit.marksDesc")}
          />
        ) : (
          <div className="space-y-3">
            {filteredBookmarks.map((b) => (
              <Link key={b.id} href={getBookmarkLink(b)} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs transition hover:shadow-md">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-muted">
                  {getResourceIcon(b.targetType)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="truncate text-sm font-medium text-foreground">{b.targetTitle || t("marks.untitled")}</p>
                  <p className="mt-0.5 text-[10px] text-muted-foreground">
                    {t("marks.savedOn", { date: new Date(b.createdAt).toLocaleDateString() })}
                  </p>
                </div>
                <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                  {b.targetType}
                </span>
              </Link>
            ))}
          </div>
        )
      )}

      {tab === "resources" && (
        filteredResources.length === 0 ? (
          <EmptyState
            icon={<FileText className="size-8" />}
            title={t("kit.noResources")}
            description={t("kit.noResourcesDesc")}
          />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filteredResources.map((r) => (
              <a
                key={r.id}
                href={r.fileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  {getResourceIcon(r.resourceType)}
                  <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                    {r.resourceType}
                  </span>
                </div>
                <h3 className="mt-3 font-semibold text-foreground line-clamp-1">{r.title}</h3>
                {r.description && <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{r.description}</p>}
                <p className="mt-2 text-[10px] text-muted-foreground">{new Date(r.createdAt).toLocaleDateString()}</p>
              </a>
            ))}
          </div>
        )
      )}
    </div>
  )
}
