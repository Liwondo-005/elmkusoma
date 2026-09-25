"use client"

import { useEffect, useRef } from "react"
import type { TrackPublication } from "livekit-client"
import { Video, Clock, WifiOff, AlertCircle, PlayCircle } from "lucide-react"

export type LivePlayerState =
  | "connecting"
  | "live"
  | "waiting"
  | "reconnecting"
  | "ended"
  | "error"
  | "scheduled"

interface LiveVideoPlayerProps {
  state: LivePlayerState
  remoteVideoTrack: TrackPublication | null
  remoteAudioTrack: TrackPublication | null
  isTeacher?: boolean
  localStream?: MediaStream | null
  screenStream?: MediaStream | null
  cameraEnabled?: boolean
  recordingUrl?: string | null
  scheduledLabel?: string
  onRetry?: () => void
  children?: React.ReactNode
  pipStream?: MediaStream | null
}

function attachMedia(
  el: HTMLVideoElement | HTMLAudioElement | null,
  pub: TrackPublication | null,
  kind: "video" | "audio",
) {
  if (!el) return
  const track = kind === "video" ? pub?.videoTrack : pub?.audioTrack
  const mst = track?.mediaStreamTrack
  if (!mst) {
    if (el.srcObject) el.srcObject = null
    return
  }
  const current = el.srcObject as MediaStream | null
  if (current && current.getTracks()[0] === mst) return
  const stream = new MediaStream([mst])
  el.srcObject = stream
  const p = el.play?.()
  if (p && typeof p.catch === "function") {
    p.catch(() => {
      el.muted = true
      el.play?.().catch(() => {})
    })
  }
}

export function LiveVideoPlayer({
  state,
  remoteVideoTrack,
  remoteAudioTrack,
  isTeacher = false,
  localStream = null,
  screenStream = null,
  cameraEnabled = false,
  recordingUrl = null,
  scheduledLabel,
  onRetry,
  children,
  pipStream = null,
}: LiveVideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const audioRef = useRef<HTMLAudioElement>(null)
  const screenRef = useRef<HTMLVideoElement>(null)
  const localRef = useRef<HTMLVideoElement>(null)
  const pipRef = useRef<HTMLVideoElement>(null)

  const hasRemoteVideo = state === "live" && !!remoteVideoTrack?.videoTrack

  useEffect(() => {
    attachMedia(videoRef.current, hasRemoteVideo ? remoteVideoTrack : null, "video")
  }, [remoteVideoTrack, hasRemoteVideo])

  useEffect(() => {
    attachMedia(audioRef.current, remoteAudioTrack, "audio")
  }, [remoteAudioTrack])

  useEffect(() => {
    const el = screenRef.current
    if (!el) return
    if (screenStream) {
      el.srcObject = screenStream
      el.play?.().catch(() => {})
    } else if (el.srcObject) {
      el.srcObject = null
    }
  }, [screenStream])

  useEffect(() => {
    const el = localRef.current
    if (!el) return
    if (localStream && cameraEnabled) {
      el.srcObject = localStream
      el.play?.().catch(() => {})
    } else if (el.srcObject) {
      el.srcObject = null
    }
  }, [localStream, cameraEnabled])

  useEffect(() => {
    const el = pipRef.current
    if (!el) return
    if (pipStream) {
      el.srcObject = pipStream
      el.play?.().catch(() => {})
    } else if (el.srcObject) {
      el.srcObject = null
    }
  }, [pipStream])

  const showLiveBadge = state === "live" || state === "waiting"

  const showTeacherLocal =
    isTeacher &&
    (state === "live" || state === "waiting")

  return (
    <div
      className="relative w-full overflow-hidden rounded-xl border border-border bg-slate-900 shadow-sm"
      style={{ aspectRatio: "16 / 9" }}
      data-live-player-state={state}
    >
      {state === "connecting" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-white">
          <span className="size-8 animate-spin rounded-full border-2 border-white/30 border-t-white" />
          <p className="text-sm opacity-80">Connecting to live session...</p>
        </div>
      )}

      {state === "reconnecting" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-white">
          <WifiOff className="size-8 animate-pulse opacity-60" />
          <p className="text-sm opacity-80">Reconnecting to live session...</p>
        </div>
      )}

      {state === "error" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-center text-white">
          <AlertCircle className="size-8 opacity-60" />
          <p className="text-sm opacity-80">Unable to connect to the live session</p>
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="mt-1 rounded-lg bg-white/20 px-4 py-1.5 text-xs font-medium text-white hover:bg-white/30 transition-colors"
            >
              Retry
            </button>
          )}
        </div>
      )}

      {state === "ended" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 p-6 text-center text-white">
          <p className="text-sm font-medium opacity-90">Live session ended</p>
          {recordingUrl ? (
            recordingUrl.startsWith("http") ? (
              <a
                href={recordingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-3 inline-flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 text-sm text-white hover:bg-white/30 transition-colors"
              >
                <PlayCircle className="size-4" /> Watch Recording
              </a>
            ) : (
              <p className="mt-2 text-xs opacity-50">Recording is being processed</p>
            )
          ) : (
            <p className="mt-1 text-xs opacity-50">No recording available</p>
          )}
        </div>
      )}

      {state === "scheduled" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-white">
          <Clock className="size-10 opacity-40" />
          <p className="text-xs opacity-60">{scheduledLabel || "Session has not started yet"}</p>
        </div>
      )}

      {state === "waiting" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-center text-white">
          <Video className="size-10 mb-1 opacity-40" />
          <p className="text-xs opacity-60">Waiting for the teacher&apos;s video...</p>
        </div>
      )}

      {hasRemoteVideo && (
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-contain"
          aria-label="Live video"
        />
      )}

      {remoteAudioTrack?.audioTrack && (state === "live" || state === "waiting") && (
        <audio ref={audioRef} autoPlay playsInline />
      )}

      {showTeacherLocal && screenStream && !hasRemoteVideo && (
        <video
          ref={screenRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-contain"
        />
      )}

      {showTeacherLocal && localStream && cameraEnabled && !screenStream && !hasRemoteVideo && (
        <video
          ref={localRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-cover"
        />
      )}

      {showTeacherLocal && !screenStream && !(localStream && cameraEnabled) && !hasRemoteVideo && (
        <div className="absolute inset-0 flex items-center justify-center text-center text-white">
          <div>
            <Video className="mx-auto mb-2 size-10 opacity-40" />
            <p className="text-xs opacity-60">Camera off</p>
          </div>
        </div>
      )}

      {showLiveBadge && (
        <div className="absolute left-3 top-3 z-10 inline-flex items-center gap-1.5 rounded-md bg-red-600/90 px-2 py-1 text-[11px] font-bold tracking-wide text-white shadow">
          <span className="size-1.5 animate-pulse rounded-full bg-white" />
          LIVE
        </div>
      )}

      {state === "waiting" && showLiveBadge && (
        <div className="absolute inset-x-0 top-14 z-10 text-center">
          <span className="rounded bg-black/60 px-2 py-1 text-[11px] text-white/90">
            Waiting for the teacher&apos;s video...
          </span>
        </div>
      )}

      {pipStream && (
        <div className="absolute bottom-16 right-2 z-10 w-40 overflow-hidden rounded-lg border-2 border-white/20" style={{ aspectRatio: "16 / 9" }}>
          <video
            ref={pipRef}
            autoPlay
            playsInline
            muted
            className="h-full w-full object-cover"
          />
        </div>
      )}

      {children && <>{children}</>}
    </div>
  )
}
