"use client"

import { useEffect, useState } from "react"
import { learnerApi, type Resource } from "@/lib/learner-api"
import { VideoPlayer } from "@/components/events/video-player"
import { Search, Play, Loader2, VideoOff, Clock, Filter } from "lucide-react"

export default function VideoLibraryPage() {
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
      setError(e.message || "Failed to load videos")
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
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Video Library</h1>
        <p className="text-muted-foreground">Browse educational videos, tutorials, and recordings</p>
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
          placeholder="Search videos..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-10 w-full rounded-lg border border-border bg-card pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
        />
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-8 animate-spin text-primary" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <VideoOff className="size-12 text-muted-foreground/40" />
          <p className="mt-4 text-lg font-medium">No videos found</p>
          <p className="text-sm text-muted-foreground">
            {search ? "Try a different search term" : "Videos will appear here once uploaded"}
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
                  Video
                </div>
              </div>
              <div className="p-4">
                <h3 className="font-semibold group-hover:text-primary transition-colors line-clamp-2">{video.title}</h3>
                {video.description && <p className="mt-1.5 text-sm text-muted-foreground line-clamp-2">{video.description}</p>}
                <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><Clock className="size-3" /> {new Date(video.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
