"use client"

import { useEffect, useState, useRef, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, type ReplayDetail } from "@/lib/learner-api"
import {
  ArrowLeft, Play, Pause, Volume2, VolumeX, Maximize,
  Minimize, Loader2, CalendarDays, Clock, User, Eye,
  ChevronRight, CheckCircle
} from "lucide-react"

function formatDuration(seconds: number) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}h ${m}m ${s}s`
  if (m > 0) return `${m}m ${s}s`
  return `${s}s`
}

function formatTime(seconds: number) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`
  return `${m}:${s.toString().padStart(2, "0")}`
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric", year: "numeric" })
}

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2]

export default function ReplayViewerPage() {
  const params = useParams()
  const router = useRouter()
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const replayId = params.id as string

  const videoRef = useRef<HTMLVideoElement>(null)
  const progressRef = useRef<HTMLDivElement>(null)
  const saveTimerRef = useRef<NodeJS.Timeout | null>(null)

  const [data, setData] = useState<ReplayDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  const [playing, setPlaying] = useState(false)
  const [muted, setMuted] = useState(false)
  const [volume, setVolume] = useState(1)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [speed, setSpeed] = useState(1)
  const [fullscreen, setFullscreen] = useState(false)
  const [showSpeedMenu, setShowSpeedMenu] = useState(false)
  const [connectionStatus, setConnectionStatus] = useState<"connected" | "disconnected">("connected")

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0

  const loadReplay = useCallback(async () => {
    try {
      const detail = await learnerApi.getReplay(replayId)
      setData(detail)
      setError("")
    } catch (e: any) {
      setError(e.message || tc("error"))
    } finally {
      setLoading(false)
    }
  }, [replayId, tc])

  useEffect(() => {
    loadReplay()
  }, [loadReplay])

  useEffect(() => {
    if (!data) return
    const video = videoRef.current
    if (!video) return

    const savedPos = data.replay.positionSeconds
    if (savedPos > 0 && savedPos < video.duration - 5) {
      video.currentTime = savedPos
    }

    const onTimeUpdate = () => setCurrentTime(video.currentTime)
    const onLoadedMetadata = () => setDuration(video.duration)
    const onEnded = () => setPlaying(false)
    const onPlay = () => setPlaying(true)
    const onPause = () => setPlaying(false)

    video.addEventListener("timeupdate", onTimeUpdate)
    video.addEventListener("loadedmetadata", onLoadedMetadata)
    video.addEventListener("ended", onEnded)
    video.addEventListener("play", onPlay)
    video.addEventListener("pause", onPause)

    return () => {
      video.removeEventListener("timeupdate", onTimeUpdate)
      video.removeEventListener("loadedmetadata", onLoadedMetadata)
      video.removeEventListener("ended", onEnded)
      video.removeEventListener("play", onPlay)
      video.removeEventListener("pause", onPause)
    }
  }, [data])

  useEffect(() => {
    if (!data) return
    saveTimerRef.current = setInterval(async () => {
      const video = videoRef.current
      if (!video || video.paused) return
      try {
        await learnerApi.updateReplayProgress(replayId, video.currentTime)
      } catch {}
    }, 10000)
    return () => {
      if (saveTimerRef.current) clearInterval(saveTimerRef.current)
    }
  }, [data, replayId])

  const togglePlay = () => {
    const video = videoRef.current
    if (!video) return
    if (playing) { video.pause() } else { video.play() }
  }

  const toggleMute = () => {
    const video = videoRef.current
    if (!video) return
    video.muted = !muted
    setMuted(!muted)
  }

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const video = videoRef.current
    if (!video) return
    const v = parseFloat(e.target.value)
    video.volume = v
    setVolume(v)
    if (v === 0) { video.muted = true; setMuted(true) }
    else if (muted) { video.muted = false; setMuted(false) }
  }

  const handleSeek = (e: React.MouseEvent<HTMLDivElement>) => {
    const video = videoRef.current
    if (!video || !duration) return
    const rect = e.currentTarget.getBoundingClientRect()
    const pct = (e.clientX - rect.left) / rect.width
    video.currentTime = pct * duration
  }

  const handleSpeedChange = (s: number) => {
    const video = videoRef.current
    if (!video) return
    video.playbackRate = s
    setSpeed(s)
    setShowSpeedMenu(false)
  }

  const toggleFullscreen = () => {
    const el = videoRef.current?.parentElement
    if (!el) return
    if (document.fullscreenElement) {
      document.exitFullscreen()
      setFullscreen(false)
    } else {
      el.requestFullscreen()
      setFullscreen(true)
    }
  }

  useEffect(() => {
    const handler = () => setFullscreen(!!document.fullscreenElement)
    document.addEventListener("fullscreenchange", handler)
    return () => document.removeEventListener("fullscreenchange", handler)
  }, [])

  if (loading) {
    return (
      <main role="main" aria-label={t("viewer.eventContext")} className="flex items-center justify-center py-16">
        <Loader2 className="size-8 animate-spin text-primary" />
      </main>
    )
  }

  if (error || !data) {
    return (
      <main role="main" aria-label={t("viewer.eventContext")} className="space-y-4">
        <Link href="/dashboard/learner/replays" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> {tc("back")}
        </Link>
        <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center">
          <p className="text-sm text-red-700">{error || tc("error")}</p>
        </div>
      </main>
    )
  }

  const { replay, relatedResources, upcomingEvents } = data

  return (
    <main role="main" aria-label={t("viewer.eventContext")} className="space-y-6">
      <Link href="/dashboard/learner/replays" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Back to Replays
      </Link>

      <div className="rounded-xl border border-border bg-card overflow-hidden">
        <div className="relative bg-black">
          <video
            ref={videoRef}
            src={replay.videoUrl}
            className="w-full"
            preload="metadata"
            onClick={togglePlay}
          />

          <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent px-4 pb-3 pt-10">
            <div
              ref={progressRef}
              onClick={handleSeek}
              className="mb-2 h-1.5 w-full cursor-pointer rounded-full bg-white/30"
              role="slider"
              aria-label="Seek"
              aria-valuemin={0}
              aria-valuemax={duration}
              aria-valuenow={currentTime}
            >
              <div
                className="h-1.5 rounded-full bg-primary transition-all"
                style={{ width: `${progress}%` }}
              />
            </div>

            <div className="flex items-center gap-3">
              <button onClick={togglePlay} className="text-white hover:text-primary" aria-label={playing ? "Pause" : "Play"}>
                {playing ? <Pause className="size-5" /> : <Play className="size-5" />}
              </button>

              <span className="text-xs text-white/70 tabular-nums">
                {formatTime(currentTime)} / {formatTime(duration)}
              </span>

              <div className="relative ml-auto flex items-center gap-2">
                <div className="relative">
                  <button
                    onClick={() => setShowSpeedMenu(!showSpeedMenu)}
                    className="rounded px-1.5 py-0.5 text-xs text-white/70 hover:text-white"
                    aria-label={t("viewer.playbackSpeed")}
                  >
                    {speed}x
                  </button>
                  {showSpeedMenu && (
                    <div className="absolute bottom-full mb-2 right-0 rounded-lg border border-border bg-card p-1 shadow-lg">
                      {SPEEDS.map((s) => (
                        <button
                          key={s}
                          onClick={() => handleSpeedChange(s)}
                          className={`block w-full rounded px-3 py-1 text-left text-xs hover:bg-muted ${speed === s ? "font-medium text-primary" : ""}`}
                        >
                          {s === 1 ? t("viewer.normal") : `${s}x`}
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-1">
                  <button onClick={toggleMute} className="text-white/70 hover:text-white" aria-label={muted ? "Unmute" : "Mute"}>
                    {muted || volume === 0 ? <VolumeX className="size-4" /> : <Volume2 className="size-4" />}
                  </button>
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.05"
                    value={muted ? 0 : volume}
                    onChange={handleVolumeChange}
                    className="w-16 accent-primary"
                    aria-label="Volume"
                  />
                </div>

                <button onClick={toggleFullscreen} className="text-white/70 hover:text-white" aria-label={fullscreen ? "Exit fullscreen" : "Fullscreen"}>
                  {fullscreen ? <Minimize className="size-4" /> : <Maximize className="size-4" />}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="p-4">
          <div className="flex items-center gap-2">
            <div className={`size-2 rounded-full ${connectionStatus === "connected" ? "bg-green-500" : "bg-red-500"}`} />
            <span className="text-xs text-muted-foreground">{t("viewer.connectionStatus")}</span>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-border bg-card p-6">
            <h2 className="text-xl font-bold">{replay.title}</h2>
            <p className="mt-1 text-sm text-muted-foreground">{replay.eventTitle}</p>

            <div className="mt-4 grid gap-4 sm:grid-cols-3">
              <div className="flex items-start gap-2">
                <User className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("viewer.presenter")}</p>
                  <p className="text-sm font-medium">{replay.presenterName || "—"}</p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <CalendarDays className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("viewer.recordedOn")}</p>
                  <p className="text-sm font-medium">{formatDate(replay.recordedAt)}</p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Clock className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">{t("viewer.totalDuration")}</p>
                  <p className="text-sm font-medium">{formatDuration(replay.durationSeconds)}</p>
                </div>
              </div>
            </div>

            <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
              <Eye className="size-4" />
              <span>{replay.viewCount} views</span>
            </div>
          </div>

          {relatedResources.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="mb-4 text-lg font-semibold">{t("viewer.relatedLearning")}</h3>
              <div className="space-y-2">
                {relatedResources.map((res) => (
                  <a
                    key={res.id}
                    href={res.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center justify-between rounded-lg border border-border p-3 hover:bg-muted/50"
                  >
                    <div>
                      <p className="text-sm font-medium">{res.title}</p>
                      <p className="text-xs text-muted-foreground">{res.resourceType}</p>
                    </div>
                    <ChevronRight className="size-4 text-muted-foreground" />
                  </a>
                ))}
              </div>
            </div>
          )}

          {upcomingEvents.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="mb-4 text-lg font-semibold">{t("viewer.upcomingEvents")}</h3>
              <div className="space-y-2">
                {upcomingEvents.map((ev) => (
                  <Link
                    key={ev.id}
                    href={`/dashboard/learner/events/${ev.id}`}
                    className="flex items-center justify-between rounded-lg border border-border p-3 hover:bg-muted/50"
                  >
                    <div>
                      <p className="text-sm font-medium">{ev.title}</p>
                      <p className="text-xs text-muted-foreground">{formatDate(ev.startsAt)}</p>
                    </div>
                    <ChevronRight className="size-4 text-muted-foreground" />
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="space-y-6">
          {(replay.relatedCourseId || replay.relatedLessonId) && (
            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="mb-3 text-lg font-semibold">{t("viewer.relatedLearning")}</h3>
              {replay.relatedCourseId && (
                <Link
                  href={`/dashboard/learner/courses/${replay.relatedCourseId}`}
                  className="mb-2 block rounded-lg border border-border p-3 text-sm font-medium hover:bg-muted/50"
                >
                  {t("viewer.relatedCourse")}: {replay.relatedCourseTitle}
                </Link>
              )}
              {replay.relatedLessonId && (
                <Link
                  href={`/dashboard/learner/courses/${replay.relatedCourseId}`}
                  className="block rounded-lg border border-border p-3 text-sm font-medium hover:bg-muted/50"
                >
                  {t("viewer.relatedLesson")}: {replay.relatedLessonTitle}
                </Link>
              )}
            </div>
          )}

          <div className="rounded-xl border border-border bg-card p-6">
            <h3 className="mb-3 text-lg font-semibold">{t("viewer.continueLearning")}</h3>
            <p className="text-sm text-muted-foreground">{t("viewer.continueDescription")}</p>
            {replay.relatedCourseId && (
              <Link
                href={`/dashboard/learner/courses/${replay.relatedCourseId}`}
                className="mt-3 flex h-10 items-center justify-center rounded-lg bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                {tc("continue")}
              </Link>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
