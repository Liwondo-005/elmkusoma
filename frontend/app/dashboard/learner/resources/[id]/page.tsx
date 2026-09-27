"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { learnerApi, type Resource, type ResourceAnnotation } from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import { FileText, Video, Music, Image, Download, ArrowLeft, AlertCircle, Bookmark, BookmarkCheck, MessageSquare, Trash2, Send } from "lucide-react"

export default function ResourceDetailPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const params = useParams()
  const resourceId = params.id as string

  const [resource, setResource] = useState<Resource | null>(null)
  const [relatedResources, setRelatedResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isBookmarked, setIsBookmarked] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)
  const [annotations, setAnnotations] = useState<ResourceAnnotation[]>([])
  const [annotationText, setAnnotationText] = useState("")
  const [annotationPrivate, setAnnotationPrivate] = useState(true)
  const [annotationSubmitting, setAnnotationSubmitting] = useState(false)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadResource()
    loadAnnotations()
  }, [user, resourceId])

  async function loadResource() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getResource(resourceId)
      setResource(data)

      learnerApi.getRelatedResources(resourceId).then(setRelatedResources).catch(() => {})
      learnerApi.checkBookmark("resource", resourceId).then((isBookmarked) => setIsBookmarked(isBookmarked)).catch(() => {})
    } catch {
      setError(t("res.loadError"))
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

  function loadAnnotations() {
    learnerApi.getResourceAnnotations(resourceId).then(setAnnotations).catch(() => setAnnotations([]))
  }

  async function submitAnnotation() {
    if (!annotationText.trim()) return
    try {
      setAnnotationSubmitting(true)
      await learnerApi.createResourceAnnotation(resourceId, {
        content: annotationText.trim(),
        isPrivate: annotationPrivate,
      })
      setAnnotationText("")
      loadAnnotations()
    } catch {
      // Silent fail
    } finally {
      setAnnotationSubmitting(false)
    }
  }

  async function deleteAnnotation(annotationId: string) {
    try {
      await learnerApi.deleteResourceAnnotation(resourceId, annotationId)
      setAnnotations((prev) => prev.filter((a) => a.id !== annotationId))
    } catch {
      // Silent fail
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
    return <div aria-busy="true"><LoadingState /></div>
  }

  if (!resource) {
    return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {t("res.notFound")}
          </div>
        </div>
        <Link href="/dashboard/learner/resources" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
          <ArrowLeft className="size-4" /> {t("res.backLink")}
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <Link href="/dashboard/learner/resources" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> {t("res.backLink")}
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
                aria-label={isBookmarked ? t("res.removeBm") : t("res.addBm")}
                className="shrink-0 rounded-lg border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
                title={isBookmarked ? t("res.removeBm") : t("res.bmShort")}
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
                {t("res.addedOn", { date: new Date(resource.createdAt).toLocaleDateString() })}
              </span>
            </div>
          </div>
        </div>
        <div className="mt-6 flex gap-3">
          <a
            href={resource.storageUrl || resource.fileUrl || resource.thumbnailUrl || "#"}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={t("res.downloadLabel", { title: resource.title })}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Download className="size-4" />
            {t("res.download")}
          </a>
          <button
            onClick={toggleBookmark}
            disabled={bookmarkLoading}
            aria-label={isBookmarked ? t("res.removeBm") : t("res.saveBm")}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-5 py-2.5 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50"
          >
            {isBookmarked ? <BookmarkCheck className="size-4 text-primary" /> : <Bookmark className="size-4" />}
            {isBookmarked ? t("res.bookmarked") : t("res.bmShort")}
          </button>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="flex items-center gap-2 text-lg font-semibold text-foreground">
          <MessageSquare className="size-5 text-primary" />
          Annotations
          <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
            {annotations.length}
          </span>
        </h2>

        <div className="mt-4 space-y-3">
          <textarea
            value={annotationText}
            onChange={(e) => setAnnotationText(e.target.value)}
            placeholder="Add your note about this resource..."
            rows={3}
            aria-label="Annotation content"
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          />
          <div className="flex items-center justify-between gap-3">
            <label className="flex items-center gap-2 text-sm text-muted-foreground">
              <input
                type="checkbox"
                checked={annotationPrivate}
                onChange={(e) => setAnnotationPrivate(e.target.checked)}
                className="size-4 rounded border-border"
              />
              Private note
            </label>
            <button
              onClick={submitAnnotation}
              disabled={annotationSubmitting || !annotationText.trim()}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              <Send className="size-4" />
              {annotationSubmitting ? "Saving..." : "Add annotation"}
            </button>
          </div>
        </div>

        {annotations.length === 0 ? (
          <p className="mt-4 text-sm text-muted-foreground">No annotations yet. Be the first to add a note.</p>
        ) : (
          <ul className="mt-5 space-y-3">
            {annotations.map((a) => (
              <li key={a.id} className="rounded-xl border border-border bg-background p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-foreground">{a.studentName}</span>
                      {a.isPrivate && (
                        <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                          Private
                        </span>
                      )}
                      <span className="text-xs text-muted-foreground">
                        {new Date(a.createdAt).toLocaleString()}
                      </span>
                    </div>
                    <p className="mt-1 whitespace-pre-wrap text-sm text-foreground">{a.content}</p>
                  </div>
                  <button
                    onClick={() => deleteAnnotation(a.id)}
                    aria-label="Delete annotation"
                    className="shrink-0 rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {relatedResources.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground">{t("res.related")}</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {relatedResources.slice(0, 3).map((rr) => (
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
