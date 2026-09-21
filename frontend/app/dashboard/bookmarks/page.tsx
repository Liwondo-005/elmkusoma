"use client"

import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { learnerApi, type Bookmark } from "@/lib/learner-api"
import { Bookmark as BookmarkIcon, Loader2, AlertCircle } from "lucide-react"
import Link from "next/link"

export default function DashboardBookmarksPage() {
  const tc = useTranslations("common")
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getBookmarks()
      setBookmarks(data)
    } catch {
      setError(tc("error"))
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Bookmarks</h1>
        </div>
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-sm text-muted-foreground">{tc("loading")}</span>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Bookmarks</h1>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <AlertCircle className="size-10 text-destructive/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{tc("error")}</p>
          <button
            onClick={loadData}
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
          >
            {tc("retry")}
          </button>
        </div>
      </div>
    )
  }

  if (bookmarks.length === 0) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Bookmarks</h1>
          <p className="mt-1 text-sm text-muted-foreground">Access your saved courses and materials.</p>
        </div>

        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <BookmarkIcon className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">{tc("noResults")}</p>
          <p className="mt-1 max-w-sm text-sm text-muted-foreground">
            Save courses, lessons, and materials for quick access. Bookmarks will appear here.
          </p>
          <Link
            href="/dashboard/courses"
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
          >
            Browse Courses
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Bookmarks</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {bookmarks.length} saved item{bookmarks.length !== 1 ? "s" : ""}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {bookmarks.map((bookmark) => (
          <div
            key={bookmark.id}
            className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md"
          >
            <div className="flex items-start gap-3">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <BookmarkIcon className="size-5 text-primary" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-sm font-semibold text-foreground truncate">{bookmark.targetTitle}</h3>
                <p className="mt-0.5 text-[10px] text-muted-foreground">{bookmark.targetType}</p>
              </div>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-[10px] text-muted-foreground">
                Saved {new Date(bookmark.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
              </span>
              <button
                onClick={() => {
                  learnerApi.removeBookmark(bookmark.id).catch(() => {})
                  setBookmarks((prev) => prev.filter((b) => b.id !== bookmark.id))
                }}
                className="text-[10px] font-medium text-destructive hover:underline"
              >
                Remove
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
