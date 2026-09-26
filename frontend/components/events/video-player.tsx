"use client"

import { useState, useRef, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Play, Pause, Volume2, VolumeX, Maximize, X } from "lucide-react"

interface VideoPlayerProps {
  url: string
  title?: string
  onClose?: () => void
}

export function VideoPlayer({ url, title, onClose }: VideoPlayerProps) {
  const t = useTranslations("ui")
  const videoRef = useRef<HTMLVideoElement>(null)
  const [playing, setPlaying] = useState(false)
  const [muted, setMuted] = useState(false)
  const [progress, setProgress] = useState(0)
  const [duration, setDuration] = useState(0)
  const [currentTime, setCurrentTime] = useState(0)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return
    const onTimeUpdate = () => {
      setCurrentTime(video.currentTime)
      setProgress((video.currentTime / video.duration) * 100 || 0)
    }
    const onLoadedMetadata = () => setDuration(video.duration)
    const onEnded = () => setPlaying(false)
    video.addEventListener("timeupdate", onTimeUpdate)
    video.addEventListener("loadedmetadata", onLoadedMetadata)
    video.addEventListener("ended", onEnded)
    return () => {
      video.removeEventListener("timeupdate", onTimeUpdate)
      video.removeEventListener("loadedmetadata", onLoadedMetadata)
      video.removeEventListener("ended", onEnded)
    }
  }, [])

  const togglePlay = () => {
    const video = videoRef.current
    if (!video) return
    if (playing) { video.pause() } else { video.play() }
    setPlaying(!playing)
  }

  const toggleMute = () => {
    const video = videoRef.current
    if (!video) return
    video.muted = !muted
    setMuted(!muted)
  }

  const seek = (e: React.MouseEvent<HTMLDivElement>) => {
    const video = videoRef.current
    if (!video) return
    const rect = e.currentTarget.getBoundingClientRect()
    const pct = (e.clientX - rect.left) / rect.width
    video.currentTime = pct * video.duration
  }

  const toggleFullscreen = () => {
    const video = videoRef.current
    if (!video) return
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      video.requestFullscreen()
    }
  }

  const fmt = (s: number) => {
    const m = Math.floor(s / 60)
    const sec = Math.floor(s % 60)
    return `${m}:${sec.toString().padStart(2, "0")}`
  }

  const isYouTube = url.includes("youtube.com") || url.includes("youtu.be")
  const isVimeo = url.includes("vimeo.com")
  const isVideoFile = url.endsWith(".mp4") || url.endsWith(".webm") || url.endsWith(".ogg")

  if (isYouTube) {
    const videoId = url.includes("youtu.be")
      ? url.split("youtu.be/")[1]?.split("?")[0]
      : url.split("v=")[1]?.split("&")[0]
    return (
      <div className="relative w-full overflow-hidden rounded-xl bg-black">
        {onClose && (
          <button onClick={onClose} className="absolute right-3 top-3 z-10 rounded-full bg-black/60 p-1.5 text-white hover:bg-black/80">
            <X className="size-4" />
          </button>
        )}
        <div className="relative w-full" style={{ paddingBottom: "56.25%" }}>
          <iframe
            src={`https://www.youtube.com/embed/${videoId}?rel=0`}
            title={title || t("videoPlayer.defaultTitle")}
            className="absolute inset-0 h-full w-full"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        </div>
        {title && <p className="mt-2 text-sm font-medium text-foreground">{title}</p>}
      </div>
    )
  }

  if (isVimeo) {
    const vimeoId = url.split("vimeo.com/")?.[1]?.split("?")?.[0]
    return (
      <div className="relative w-full overflow-hidden rounded-xl bg-black">
        {onClose && (
          <button onClick={onClose} className="absolute right-3 top-3 z-10 rounded-full bg-black/60 p-1.5 text-white hover:bg-black/80">
            <X className="size-4" />
          </button>
        )}
        <div className="relative w-full" style={{ paddingBottom: "56.25%" }}>
          <iframe
            src={`https://player.vimeo.com/video/${vimeoId}?byline=0&portrait=0`}
            title={title || t("videoPlayer.defaultTitle")}
            className="absolute inset-0 h-full w-full"
            allow="autoplay; fullscreen; picture-in-picture"
            allowFullScreen
          />
        </div>
        {title && <p className="mt-2 text-sm font-medium text-foreground">{title}</p>}
      </div>
    )
  }

  return (
    <div className="relative w-full overflow-hidden rounded-xl bg-black">
      {onClose && (
        <button onClick={onClose} className="absolute right-3 top-3 z-10 rounded-full bg-black/60 p-1.5 text-white hover:bg-black/80">
          <X className="size-4" />
        </button>
      )}
      <video ref={videoRef} src={url} className="w-full" onClick={togglePlay} preload="metadata" />
      <div className="flex items-center gap-3 bg-black/80 px-4 py-2">
        <button onClick={togglePlay} className="text-white hover:text-primary">
          {playing ? <Pause className="size-5" /> : <Play className="size-5" />}
        </button>
        <div className="flex-1 cursor-pointer" onClick={seek}>
          <div className="h-1 w-full rounded-full bg-white/30">
            <div className="h-1 rounded-full bg-primary transition-all" style={{ width: `${progress}%` }} />
          </div>
        </div>
        <span className="min-w-[80px] text-center text-xs text-white/70">
          {fmt(currentTime)} / {fmt(duration)}
        </span>
        <button onClick={toggleMute} className="text-white hover:text-primary">
          {muted ? <VolumeX className="size-4" /> : <Volume2 className="size-4" />}
        </button>
        <button onClick={toggleFullscreen} className="text-white hover:text-primary">
          <Maximize className="size-4" />
        </button>
      </div>
      {title && <p className="mt-2 text-sm font-medium text-foreground">{title}</p>}
    </div>
  )
}
