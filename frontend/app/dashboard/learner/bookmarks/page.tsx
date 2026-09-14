"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Bookmark } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Bookmark as BookmarkIcon, Trash2, ExternalLink, AlertCircle, BookOpen, Video, FileText } from "lucide-react"

export default function LearnerBookmarksPage() {
  const { user, loading: authLoading } = useAuth()
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [removing, setRemoving] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadBookmarks()
  }, [user])

  async function loadBookmarks() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getBookmarks()
      setBookmarks(data)
    } catch {
      setError("Failed to load bookmarks")
    } finally {
      setLoading(false)
    }
  }

  async function removeBookmark(id: string) {
    try {
      setRemoving(id)
      await learnerApi.removeBookmark(id)
      setBookmarks((prev) => prev.filter((b) => b.id !== id))
    } catch {
      setError("Failed to remove bookmark")
    } finally {
      setRemoving(null)
    }
  }

  function getTypeIcon(type: string) {
    switch (type?.toLowerCase()) {
      case "course": return <BookOpen className="size-4 text-blue-500" />
      case "liveclass": case "live_class": return <Video className="size-4 text-red-500" />
      case "resource": return <FileText className="size-4 text-teal" />
      default: return <BookmarkIcon className="size-4 text-orange" />
    }
  }

  function getTypeBadge(type: string) {
    const colors: Record<string, string> = {
      course: "bg-blue-500/10 text-blue-500",
      liveclass: "bg-red-500/10 text-red-500",
      live_class: "bg-red-500/10 text-red-500",
      resource: "bg-teal/10 text-teal",
    }
    return colors[type?.toLowerCase()] || "bg-muted text-muted-foreground"
  }

  function getLink(bookmark: Bookmark) {
    switch (bookmark.targetType?.toLowerCase()) {
      case "course": return `/dashboard/learner/courses/${bookmark.targetId}`
      case "liveclass": case "live_class": return `/dashboard/learner/live-classes`
      case "resource": return `/dashboard/learner/resources`
      default: return "#"
    }
  }

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Bookmarks</h1>
        <p className="mt-1 text-sm text-muted-foreground">Your saved items for quick access.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {loading ? (
        <LoadingState />
      ) : bookmarks.length === 0 ? (
        <EmptyState
          icon={<BookmarkIcon className="size-8" />}
          title="No bookmarks yet"
          description="Save courses, resources, and classes for quick access."
        />
      ) : (
        <div className="space-y-3">
          {bookmarks.map((bookmark) => (
            <div key={bookmark.id} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-muted">
                {getTypeIcon(bookmark.targetType)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <Link href={getLink(bookmark)} className="text-sm font-medium text-foreground hover:text-primary truncate">
                    {bookmark.title}
                  </Link>
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${getTypeBadge(bookmark.targetType)}`}>
                    {bookmark.targetType}
                  </span>
                </div>
                {bookmark.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-1">{bookmark.description}</p>
                )}
                <p className="mt-1 text-[10px] text-muted-foreground">
                  Saved {new Date(bookmark.createdAt).toLocaleDateString()}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Link
                  href={getLink(bookmark)}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted"
                >
                  <ExternalLink className="size-3" />
                  View
                </Link>
                <button
                  onClick={() => removeBookmark(bookmark.id)}
                  disabled={removing === bookmark.id}
                  className="inline-flex items-center gap-1 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 disabled:opacity-50"
                >
                  <Trash2 className="size-3" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
