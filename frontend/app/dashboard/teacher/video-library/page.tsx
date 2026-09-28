"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import {
  Video,
  Film,
  Plus,
  X,
  Loader2,
  AlertCircle,
  Clock,
  Link2,
  VideoOff,
} from "lucide-react"
import { teacherFetch } from "@/lib/teacher-api"

interface VideoTutorialItem {
  id: string
  title: string
  description: string | null
  recordingUrl: string | null
  status: string
  visibility: string
  durationSeconds: number | null
  createdAt: string
}

interface MediaAssetItem {
  id: string
  title: string
  mediaType: string
  fileUrl: string | null
  durationSeconds: number | null
}

const VIDEO_MEDIA_TYPES = ["VIDEO", "RECORDING"]

function formatDuration(seconds: number | null): string | null {
  if (!seconds || seconds <= 0) return null
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, "0")}`
}

export default function TeacherVideoLibraryPage() {
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")

  const [videos, setVideos] = useState<VideoTutorialItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [showAttach, setShowAttach] = useState(false)
  const [media, setMedia] = useState<MediaAssetItem[]>([])
  const [mediaLoading, setMediaLoading] = useState(false)
  const [mediaError, setMediaError] = useState<string | null>(null)
  const [attachingId, setAttachingId] = useState<string | null>(null)
  const [attachError, setAttachError] = useState<string | null>(null)

  async function loadVideos() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherFetch<VideoTutorialItem[]>("/v1/video-tutorials")
      setVideos(data || [])
    } catch (err: any) {
      setError(err.message || t("videoLibrary.loadError"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadVideos()
  }, [])

  async function openAttachDialog() {
    setShowAttach(true)
    setAttachError(null)
    setMediaError(null)
    if (media.length > 0) return
    try {
      setMediaLoading(true)
      const data = await teacherFetch<MediaAssetItem[]>("/v1/media")
      setMedia((data || []).filter((m) => VIDEO_MEDIA_TYPES.includes(m.mediaType)))
    } catch (err: any) {
      setMediaError(err.message || t("videoLibrary.mediaLoadError"))
    } finally {
      setMediaLoading(false)
    }
  }

  async function handleAttach(assetId: string) {
    try {
      setAttachingId(assetId)
      setAttachError(null)
      await teacherFetch("/v1/video-tutorials/attach-recording", {
        method: "POST",
        body: JSON.stringify({ mediaAssetId: assetId }),
      })
      setShowAttach(false)
      await loadVideos()
    } catch (err: any) {
      setAttachError(err.message || t("videoLibrary.attachError"))
    } finally {
      setAttachingId(null)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("videoLibrary")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("videoLibrary.subtitle")}</p>
        </div>
        <Button onClick={openAttachDialog} className="gap-2">
          <Plus className="size-4" /> {t("videoLibrary.attachRecording")}
        </Button>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      {showAttach && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="dialog" aria-modal="true" aria-label={t("videoLibrary.attachDialogTitle")}>
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-5 shadow-lg">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-sm font-semibold text-foreground">{t("videoLibrary.attachDialogTitle")}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{t("videoLibrary.attachDialogDesc")}</p>
              </div>
              <button
                onClick={() => setShowAttach(false)}
                className="text-muted-foreground hover:text-foreground"
                aria-label={tc("close")}
              >
                <X className="size-4" />
              </button>
            </div>

            {attachError && (
              <div className="mt-3 flex items-center gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                <AlertCircle className="size-3.5 shrink-0" />
                {attachError}
              </div>
            )}

            <div className="mt-4 max-h-80 space-y-2 overflow-y-auto">
              {mediaLoading && (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-5 animate-spin text-muted-foreground" />
                </div>
              )}

              {!mediaLoading && mediaError && (
                <div className="flex items-center gap-2 rounded-lg bg-destructive/5 px-3 py-2 text-xs text-destructive">
                  <AlertCircle className="size-3.5 shrink-0" />
                  {mediaError}
                </div>
              )}

              {!mediaLoading && !mediaError && media.length === 0 && (
                <div className="rounded-lg border border-dashed border-border py-8 text-center">
                  <VideoOff className="mx-auto size-6 text-muted-foreground" />
                  <p className="mt-2 text-xs font-medium text-foreground">{t("videoLibrary.noRecordingsTitle")}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{t("videoLibrary.noRecordingsDesc")}</p>
                </div>
              )}

              {!mediaLoading && !mediaError &&
                media.map((asset) => (
                  <div
                    key={asset.id}
                    className="flex items-center justify-between gap-3 rounded-lg border border-border px-3 py-2.5"
                  >
                    <div className="flex min-w-0 items-center gap-2">
                      <Film className="size-4 shrink-0 text-orange-500" />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-foreground">{asset.title}</p>
                        <p className="text-[11px] text-muted-foreground">{asset.mediaType}</p>
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleAttach(asset.id)}
                      disabled={attachingId === asset.id}
                      className="shrink-0 gap-1.5"
                    >
                      {attachingId === asset.id ? (
                        <Loader2 className="size-3.5 animate-spin" />
                      ) : (
                        <Link2 className="size-3.5" />
                      )}
                      {t("videoLibrary.attach")}
                    </Button>
                  </div>
                ))}
            </div>

            <div className="mt-4 flex justify-end">
              <Button variant="outline" size="sm" onClick={() => setShowAttach(false)}>
                {tc("cancel")}
              </Button>
            </div>
          </div>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && !error && videos.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <Video className="mx-auto mb-3 size-8 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">{t("videoLibrary.emptyTitle")}</h3>
          <p className="mt-1 text-xs text-muted-foreground">{t("videoLibrary.emptyDesc")}</p>
          <Button onClick={openAttachDialog} className="mt-4 gap-2" size="sm">
            <Plus className="size-3" /> {t("videoLibrary.attachRecording")}
          </Button>
        </div>
      )}

      {!loading && videos.length > 0 && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {videos.map((video) => {
            const duration = formatDuration(video.durationSeconds)
            return (
              <div key={video.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-2">
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                      <Video className="size-4 text-primary" />
                    </div>
                    <div className="min-w-0">
                      <h4 className="truncate text-sm font-semibold text-foreground">{video.title}</h4>
                      <span className="text-[11px] text-muted-foreground">{video.status} · {video.visibility}</span>
                    </div>
                  </div>
                </div>
                {video.description && (
                  <p className="mt-2 line-clamp-2 text-xs text-muted-foreground">{video.description}</p>
                )}
                <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                  {duration && (
                    <span className="flex items-center gap-1">
                      <Clock className="size-3" /> {duration}
                    </span>
                  )}
                  <span className="flex items-center gap-1">
                    <Link2 className="size-3" />
                    {video.recordingUrl ? t("videoLibrary.linked") : t("videoLibrary.notLinked")}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
