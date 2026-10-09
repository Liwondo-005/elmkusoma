"use client"

import { useEffect, useRef } from "react"
import { useTranslations } from "next-intl"
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
  /** Teacher screen-share publication. Tracked apart from the camera so a screen share
   *  can take the main stage instead of competing with the camera for one slot. */
  remoteScreenTrack?: TrackPublication | null
  isTeacher?: boolean
  localStream?: MediaStream | null
  screenStream?: MediaStream | null
  cameraEnabled?: boolean
  recordingUrl?: string | null
  scheduledLabel?: string
  onRetry?: () => void
  children?: React.ReactNode
  pipStream?: MediaStream | null
  // Authoritative live-session identity (driven by the classroom's polled
  // LiveClass status, never by media-connection state): shows the ELMKUSOMA
  // broadcast bug + elapsed duration only while the session is really LIVE.
  sessionLive?: boolean
  // Elapsed live duration formatted HH:MM:SS from the authoritative start
  // timestamp. null/undefined hides the timer (scheduled/ended/replay).
  liveElapsed?: string | null
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
  remoteScreenTrack = null,
  isTeacher = false,
  localStream = null,
  screenStream = null,
  cameraEnabled = false,
  recordingUrl = null,
  scheduledLabel,
  onRetry,
  children,
  pipStream = null,
  sessionLive = false,
  liveElapsed = null,
}: LiveVideoPlayerProps) {
  const t = useTranslations("live")
  const tc = useTranslations("common")
  const ts = useTranslations("status")
  const te = useTranslations("events")
  const videoRef = useRef<HTMLVideoElement>(null)
  const audioRef = useRef<HTMLAudioElement>(null)
  const screenRef = useRef<HTMLVideoElement>(null)
  const localScreenRef = useRef<HTMLVideoElement>(null)
  const localRef = useRef<HTMLVideoElement>(null)
  const pipRef = useRef<HTMLVideoElement>(null)

  const hasRemoteVideo = state === "live" && !!remoteVideoTrack?.videoTrack
  const hasRemoteScreen = state === "live" && !!remoteScreenTrack?.videoTrack
  // A screen share owns the stage; the camera drops to picture-in-picture while it is up.
  const screenOnStage = hasRemoteScreen

  useEffect(() => {
    attachMedia(videoRef.current, hasRemoteVideo ? remoteVideoTrack : null, "video")
  }, [remoteVideoTrack, hasRemoteVideo])

  useEffect(() => {
    attachMedia(screenRef.current, hasRemoteScreen ? remoteScreenTrack : null, "video")
  }, [remoteScreenTrack, hasRemoteScreen])

  useEffect(() => {
    attachMedia(audioRef.current, remoteAudioTrack, "audio")
  }, [remoteAudioTrack])

  useEffect(() => {
    const el = localScreenRef.current
    if (!el) return
    // The host must always see their own screen share, even when a learner camera is
    // subscribed (previously the local screen preview was hidden by any remote video).
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
          <p className="text-sm opacity-80">{t("playerConnecting")}</p>
        </div>
      )}

      {state === "reconnecting" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-white">
          <WifiOff className="size-8 animate-pulse opacity-60" />
          <p className="text-sm opacity-80">{t("playerReconnecting")}</p>
        </div>
      )}

      {state === "error" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-center text-white">
          <AlertCircle className="size-8 opacity-60" />
          <p className="text-sm opacity-80">{t("playerConnectError")}</p>
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="mt-1 rounded-lg bg-white/20 px-4 py-1.5 text-xs font-medium text-white hover:bg-white/30 transition-colors"
            >
              {tc("retry")}
            </button>
          )}
        </div>
      )}

      {state === "ended" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 p-6 text-center text-white">
          <p className="text-sm font-medium opacity-90">{t("playerEnded")}</p>
          {recordingUrl ? (
            recordingUrl.startsWith("http") ? (
              <a
                href={recordingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-3 inline-flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 text-sm text-white hover:bg-white/30 transition-colors"
              >
                <PlayCircle className="size-4" /> {t("playerWatchRecording")}
              </a>
            ) : (
              <p className="mt-2 text-xs opacity-50">{te("status.processing")}</p>
            )
          ) : (
            <p className="mt-1 text-xs opacity-50">{te("past.noReplay")}</p>
          )}
        </div>
      )}

      {state === "scheduled" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-white">
          <Clock className="size-10 opacity-40" />
          <p className="text-xs opacity-60">{scheduledLabel || t("playerSessionNotStarted")}</p>
        </div>
      )}

      {state === "waiting" && !showTeacherLocal && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-center text-white">
          <Video className="size-10 mb-1 opacity-40" />
          <p className="text-xs opacity-60">{t("playerWaitingTeacher")}</p>
        </div>
      )}

      {screenOnStage && (
        <video
          ref={screenRef}
          autoPlay
          playsInline
          className="absolute inset-0 h-full w-full object-contain"
          aria-label={t("playerLiveScreenAria")}
        />
      )}

      {/* Camera: full stage normally, picture-in-picture while a screen share is on stage. */}
      {hasRemoteVideo && !screenOnStage && (
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-contain"
          aria-label={t("playerLiveVideoAria")}
        />
      )}
      {hasRemoteVideo && screenOnStage && (
        <div className="absolute bottom-20 right-2 z-10 w-32 overflow-hidden rounded-lg border border-white/20 shadow-lg sm:w-44">
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="h-full w-full object-cover"
            aria-label={t("playerLiveVideoAria")}
          />
        </div>
      )}

      {remoteAudioTrack?.audioTrack && (state === "live" || state === "waiting") && (
        <audio ref={audioRef} autoPlay playsInline />
      )}

      {showTeacherLocal && screenStream && !screenOnStage && (
        <video
          ref={localScreenRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-contain"
          aria-label={t("playerYourScreenAria")}
        />
      )}

      {showTeacherLocal && localStream && cameraEnabled && !screenStream && !screenOnStage && !hasRemoteVideo && (
        <video
          ref={localRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-cover"
        />
      )}

      {showTeacherLocal && !screenStream && !(localStream && cameraEnabled) && !screenOnStage && !hasRemoteVideo && (
        <div className="absolute inset-0 flex items-center justify-center text-center text-white">
          <div>
            <Video className="mx-auto mb-2 size-10 opacity-40" />
            <p className="text-xs opacity-60">
              {state === "waiting"
                ? t("playerStartScreenHint")
                : t("playerCameraOff")}
            </p>
          </div>
        </div>
      )}

      {sessionLive && (
        <div
          className="absolute left-2 top-2 z-10 inline-flex items-center gap-1.5 rounded-md bg-black/55 px-2 py-1 text-[10px] font-bold tracking-wide text-white shadow-sm sm:left-3 sm:top-3 sm:text-[11px]"
          data-live-identity
        >
          <span className="font-extrabold tracking-[0.12em]">ELMKUSOMA</span>
          <span className="inline-flex items-center gap-1 border-l border-white/30 pl-1.5">
            <span className="size-1.5 animate-pulse rounded-full bg-red-500 motion-reduce:animate-none" aria-hidden="true" />
            <span>LIVE</span>
          </span>
        </div>
      )}

      {sessionLive && liveElapsed != null && (
        <div
          className="absolute right-2 top-2 z-10 rounded-md bg-black/55 px-2 py-1 font-mono text-[10px] font-semibold tabular-nums text-white shadow-sm sm:right-3 sm:top-3 sm:text-[11px]"
          data-live-elapsed
          aria-label="Live session duration"
        >
          {liveElapsed}
        </div>
      )}

      {state === "waiting" && showLiveBadge && !isTeacher && (
        <div className="absolute inset-x-0 top-14 z-10 text-center">
          <span className="rounded bg-black/60 px-2 py-1 text-[11px] text-white/90">
            {t("playerWaitingTeacher")}
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
