"use client"

import { useEffect, useState, useCallback } from "react"
import { learnerApi, type Resource, type VideoTutorial } from "@/lib/learner-api"
import { VideoPlayer } from "@/components/events/video-player"
import { useTranslations } from "next-intl"
import { Search, Play, Loader2, VideoOff, Clock, Download, ArrowRight, CheckCircle2 } from "lucide-react"
import Link from "next/link"

interface VideoItem {
  id: string
  title: string
  description: string | null
  url: string
  createdAt: string
  durationSeconds: number | null
  source: "resource" | "tutorial"
  completionPercentage?: number
}

export default function VideoLibraryPage() {
  const t = useTranslations("learner")
  const [videos, setVideos] = useState<VideoItem[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [playing, setPlaying] = useState<VideoItem | null>(null)
  const [error, setError] = useState("")

  useEffect(() => {
    loadVideos()
  }, [])

  async function loadVideos() {
    setLoading(true)
    setError("")
    try {
      const [resources, tutorials, progressList] = await Promise.all([
        learnerApi.getVideoLibrary().catch(() => [] as Resource[]),
        learnerApi.getVideoTutorials().catch(() => [] as VideoTutorial[]),
        learnerApi.getVideoTutorialProgressList().catch(() => [] as never[]),
      ])

      const progressById = new Map(
        (progressList as { videoTutorialId: string; completionPercentage: number }[]).map((p) => [
          p.videoTutorialId,
          p.completionPercentage,
        ])
      )

      const resourceItems: VideoItem[] = resources
        .filter((r) => r.resourceType === "VIDEO")
        .map((r) => ({
          id: r.id,
          title: r.title,
          description: r.description,
          url: r.storageUrl || r.fileUrl || r.thumbnailUrl || "",
          createdAt: r.createdAt,
          durationSeconds: r.durationSeconds ?? null,
          source: "resource" as const,
        }))

      const tutorialItems: VideoItem[] = tutorials.map((t) => ({
        id: t.id,
        title: t.title,
        description: t.description,
        url: t.recordingUrl || "",
        createdAt: t.createdAt,
        durationSeconds: t.durationSeconds ?? null,
        source: "tutorial" as const,
        completionPercentage: progressById.get(t.id),
      }))

      setVideos([...tutorialItems, ...resourceItems])
    } catch (e: any) {
      setError(e.message || t("vids.loadError"))
    } finally {
      setLoading(false)
    }
  }

  const handleProgress = useCallback(
    (positionSeconds: number, durationSeconds: number, completed: boolean) => {
      if (!playing || playing.source !== "tutorial") return
      learnerApi
        .updateVideoProgress(playing.id, {
          positionSeconds,
          durationSeconds: durationSeconds > 0 ? durationSeconds : undefined,
          completed,
        })
        .catch(() => {})
    },
    [playing]
  )

  const filtered = search
    ? videos.filter(
        (v) =>
          v.title.toLowerCase().includes(search.toLowerCase()) ||
          (v.description && v.description.toLowerCase().includes(search.toLowerCase()))
      )
    : videos

  const formatDuration = (s: number | null) => {
    if (!s || s <= 0) return null
    const m = Math.floor(s / 60)
    const sec = Math.floor(s % 60)
    return `${m}:${sec.toString().padStart(2, "0")}`
  }

  return (
    <div role="main" className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t("vids.title")}</h1>
        <p className="text-muted-foreground">{t("vids.subtitle")}</p>
      </div>

      {playing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-4xl">
            <VideoPlayer
              url={playing.url}
              title={playing.title}
              onClose={() => {
                setPlaying(null)
                loadVideos()
              }}
              onProgress={handleProgress}
            />
          </div>
        </div>
      )}

      <div className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder={t("vids.searchPlaceholder")}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label={t("vids.searchPlaceholder")}
          className="h-10 w-full rounded-lg border border-border bg-card pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
        />
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

      <div aria-live="polite" aria-busy={loading}>
      {loading ? (
        <div aria-busy="true" className="flex items-center justify-center py-16">
          <Loader2 className="size-8 animate-spin text-primary" />
        </div>
      ) : filtered.length === 0 ? (
        <div role="status" className="flex flex-col items-center justify-center py-16 text-center">
          <VideoOff className="size-12 text-muted-foreground/40" />
          <p className="mt-4 text-lg font-medium">{t("vids.emptyTitle")}</p>
          <p className="text-sm text-muted-foreground">
            {search ? t("vids.emptySearch") : t("vids.emptyDefault")}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((video) => (
            <div key={`${video.source}-${video.id}`}
              className="group rounded-xl border border-border bg-card overflow-hidden transition-all hover:border-primary/30 hover:shadow-md cursor-pointer"
              onClick={() => { if (video.url) setPlaying(video) }}>
              <div className="relative aspect-video bg-muted flex items-center justify-center">
                <div className="flex size-14 items-center justify-center rounded-full bg-primary/20 text-primary transition-transform group-hover:scale-110">
                  <Play className="size-6" />
                </div>
                <div className="absolute bottom-2 right-2 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                  {video.source === "tutorial" ? (video.url ? "Tutorial" : "Processing") : "Video"}
                </div>
                {video.completionPercentage != null && video.completionPercentage > 0 && (
                  <div className="absolute top-2 right-2 flex items-center gap-1 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                    <CheckCircle2 className="size-3" />
                    {Math.round(video.completionPercentage)}%
                  </div>
                )}
              </div>
              <div className="p-4">
                <h3 className="font-semibold group-hover:text-primary transition-colors line-clamp-2">{video.title}</h3>
                {video.description && <p className="mt-1.5 text-sm text-muted-foreground line-clamp-2">{video.description}</p>}
                <div className="mt-3 flex items-center justify-between">
                  <span className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1"><Clock className="size-3" /> {new Date(video.createdAt).toLocaleDateString()}</span>
                    {formatDuration(video.durationSeconds) && <span>{formatDuration(video.durationSeconds)}</span>}
                  </span>
                  {video.url && (
                    <a
                      href={video.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      aria-label={t("vids.downloadLabel", { title: video.title })}
                      className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                    >
                      <Download className="size-3" />
                      {t("vids.download")}
                    </a>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      </div>

      <div className="rounded-xl border border-border bg-card p-6 text-center">
        <p className="text-sm text-muted-foreground">{t("vids.moreHint")}</p>
        <Link href="/dashboard/learner/resources" className="mt-2 inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline">
          {t("vids.browseAll")} <ArrowRight className="size-3" />
        </Link>
      </div>
    </div>
  )
}
