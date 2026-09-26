"use client"

import { useEffect, useState } from "react"
import { learnerApi, type Resource } from "@/lib/learner-api"
import { VideoPlayer } from "@/components/events/video-player"
import { useTranslations } from "next-intl"
import { Search, Play, Loader2, VideoOff, Clock, Filter, Download, ArrowRight } from "lucide-react"
import Link from "next/link"

export default function VideoLibraryPage() {
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const [videos, setVideos] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [playingUrl, setPlayingUrl] = useState<string | null>(null)
  const [playingTitle, setPlayingTitle] = useState("")
  const [error, setError] = useState("")

  useEffect(() => {
    loadVideos()
  }, [])

  async function loadVideos() {
    setLoading(true)
    setError("")
    try {
      const all = await learnerApi.getVideoLibrary()
      setVideos(all.filter((r) => r.resourceType === "VIDEO"))
    } catch (e: any) {
      setError(e.message || t("vids.loadError"))
    } finally {
      setLoading(false)
    }
  }

  const filtered = search
    ? videos.filter(
        (v) =>
          v.title.toLowerCase().includes(search.toLowerCase()) ||
          (v.description && v.description.toLowerCase().includes(search.toLowerCase()))
      )
    : videos

  return (
    <div role="main" className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t("vids.title")}</h1>
        <p className="text-muted-foreground">{t("vids.subtitle")}</p>
      </div>

      {playingUrl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-4xl">
            <VideoPlayer url={playingUrl} title={playingTitle} onClose={() => { setPlayingUrl(null); setPlayingTitle("") }} />
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
            <div key={video.id}
              className="group rounded-xl border border-border bg-card overflow-hidden transition-all hover:border-primary/30 hover:shadow-md cursor-pointer"
              onClick={() => { setPlayingUrl(video.fileUrl); setPlayingTitle(video.title) }}>
              <div className="relative aspect-video bg-muted flex items-center justify-center">
                <div className="flex size-14 items-center justify-center rounded-full bg-primary/20 text-primary transition-transform group-hover:scale-110">
                  <Play className="size-6" />
                </div>
                <div className="absolute bottom-2 right-2 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                  {t("vids.badge")}
                </div>
              </div>
              <div className="p-4">
                <h3 className="font-semibold group-hover:text-primary transition-colors line-clamp-2">{video.title}</h3>
                {video.description && <p className="mt-1.5 text-sm text-muted-foreground line-clamp-2">{video.description}</p>}
                <div className="mt-3 flex items-center justify-between">
                  <span className="flex items-center gap-1 text-xs text-muted-foreground"><Clock className="size-3" /> {new Date(video.createdAt).toLocaleDateString()}</span>
                  <a
                    href={video.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                    aria-label={t("vids.downloadLabel", { title: video.title })}
                    className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    <Download className="size-3" />
                    {t("vids.download")}
                  </a>
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
