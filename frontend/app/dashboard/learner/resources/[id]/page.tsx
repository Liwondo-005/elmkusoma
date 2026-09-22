"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Resource } from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import { FileText, Video, Music, Image, Download, ArrowLeft, AlertCircle, Bookmark, BookmarkCheck } from "lucide-react"

export default function ResourceDetailPage() {
  const { user, loading: authLoading } = useAuth()
  const params = useParams()
  const resourceId = params.id as string

  const [resource, setResource] = useState<Resource | null>(null)
  const [relatedResources, setRelatedResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isBookmarked, setIsBookmarked] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadResource()
  }, [user, resourceId])

  async function loadResource() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getResource(resourceId)
      setResource(data)

      learnerApi.getRelatedResources(resourceId).then(setRelatedResources).catch(() => {})
      learnerApi.checkBookmark("resource", resourceId).then((res) => setIsBookmarked(res.bookmarked)).catch(() => {})
    } catch {
      setError("Failed to load resource")
    } finally {
      setLoading(false)
    }
  }

  async function toggleBookmark() {
    try {
      setBookmarkLoading(true)
      if (isBookmarked) {
        const bookmarks = await learnerApi.getBookmarks()
        const existing = bookmarks.find((b) => b.targetType === "resource" && b.targetId === resourceId)
        if (existing) await learnerApi.removeBookmark(existing.id)
        setIsBookmarked(false)
      } else {
        await learnerApi.addBookmark("resource", resourceId)
        setIsBookmarked(true)
      }
    } catch {
      // Silent fail
    } finally {
      setBookmarkLoading(false)
    }
  }

  function getResourceIcon(type: string) {
    switch (type?.toUpperCase()) {
      case "VIDEO": return <Video className="size-8 text-red-500" />
      case "AUDIO": return <Music className="size-8 text-purple-500" />
      case "IMAGE": return <Image className="size-8 text-blue-500" />
      default: return <FileText className="size-8 text-teal" />
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

  if (authLoading || loading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  if (!resource) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            Resource not found
          </div>
        </div>
        <Link href="/dashboard/learner/resources" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
          <ArrowLeft className="size-4" /> Back to resources
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <Link href="/dashboard/learner/resources" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to resources
      </Link>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start gap-4">
          <div className="flex size-16 shrink-0 items-center justify-center rounded-xl bg-muted">
            {getResourceIcon(resource.resourceType)}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-4">
              <h1 className="text-xl font-bold text-foreground">{resource.title}</h1>
              <button
                onClick={toggleBookmark}
                disabled={bookmarkLoading}
                className="shrink-0 rounded-lg border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
                title={isBookmarked ? "Remove bookmark" : "Bookmark"}
              >
                {isBookmarked ? <BookmarkCheck className="size-5 text-primary" /> : <Bookmark className="size-5" />}
              </button>
            </div>
            {resource.description && (
              <p className="mt-2 text-sm text-muted-foreground">{resource.description}</p>
            )}
            <div className="mt-3 flex items-center gap-3">
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${getResourceTypeBadge(resource.resourceType)}`}>
                {resource.resourceType}
              </span>
              <span className="text-xs text-muted-foreground">
                Added {new Date(resource.createdAt).toLocaleDateString()}
              </span>
            </div>
          </div>
        </div>
        <div className="mt-6 flex gap-3">
          <a
            href={resource.fileUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Download className="size-4" />
            Download Resource
          </a>
          <button
            onClick={toggleBookmark}
            disabled={bookmarkLoading}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-5 py-2.5 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
          >
            {isBookmarked ? <BookmarkCheck className="size-4 text-primary" /> : <Bookmark className="size-4" />}
            {isBookmarked ? "Bookmarked" : "Bookmark"}
          </button>
        </div>
      </div>

      {relatedResources.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground">Related Resources</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {relatedResources.map((rr) => (
              <Link
                key={rr.id}
                href={`/dashboard/learner/resources/${rr.id}`}
                className="rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
              >
                <div className="flex items-start gap-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted">
                    {rr.resourceType === "VIDEO" ? <Video className="size-4 text-red-500" /> :
                     rr.resourceType === "AUDIO" ? <Music className="size-4 text-purple-500" /> :
                     rr.resourceType === "IMAGE" ? <Image className="size-4 text-blue-500" /> :
                     <FileText className="size-4 text-teal" />}
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="text-sm font-semibold text-foreground truncate">{rr.title}</h3>
                    <span className="mt-1 inline-block rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {rr.resourceType}
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
