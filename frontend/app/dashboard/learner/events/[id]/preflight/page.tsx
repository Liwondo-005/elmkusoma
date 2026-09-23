"use client"

import { useEffect, useState, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi, type EventItem } from "@/lib/learner-api"
import { announce } from "@/lib/announce"
import { useLowBandwidth } from "@/components/primary/low-bandwidth-provider"
import { ArrowLeft, Mic, MicOff, Camera, CameraOff, Volume2, VolumeX, Wifi, WifiOff, Loader2, CheckCircle, XCircle, RefreshCw, ExternalLink } from "lucide-react"

type CheckStatus = "checking" | "ready" | "failed"

export default function PreflightPage() {
  const params = useParams()
  const router = useRouter()
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const eventId = params.id as string
  const { isLowBandwidth } = useLowBandwidth()

  const [micStatus, setMicStatus] = useState<CheckStatus>("checking")
  const [cameraStatus, setCameraStatus] = useState<CheckStatus>("checking")
  const [speakerStatus, setSpeakerStatus] = useState<CheckStatus>("checking")
  const [networkStatus, setNetworkStatus] = useState<CheckStatus>("checking")
  const [connectionStatus, setConnectionStatus] = useState<"connected" | "disconnected">("disconnected")
  const [event, setEvent] = useState<EventItem | null>(null)
  const [eventError, setEventError] = useState("")
  const [joining, setJoining] = useState(false)

  const allReady = micStatus === "ready" && cameraStatus === "ready" && speakerStatus === "ready" && networkStatus === "ready"
  const anyChecking = micStatus === "checking" || cameraStatus === "checking" || speakerStatus === "checking" || networkStatus === "checking"
  const eventStatus = (event?.eventStatus || event?.status || "").toUpperCase()
  const isLive = eventStatus === "LIVE" || eventStatus === "STARTING"
  const isEnded = ["ENDED", "RECORDING", "PROCESSING", "REPLAY_AVAILABLE", "CANCELLED", "FAILED", "COMPLETED"].includes(eventStatus)
  const canOpenMeeting = Boolean(isLive && event?.meetingUrl)

  const checkMicrophone = useCallback(async () => {
    setMicStatus("checking")
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      stream.getTracks().forEach((tr) => tr.stop())
      setMicStatus("ready")
    } catch {
      setMicStatus("failed")
    }
  }, [])

  const checkCamera = useCallback(async () => {
    setCameraStatus("checking")
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true })
      stream.getTracks().forEach((tr) => tr.stop())
      setCameraStatus("ready")
    } catch {
      setCameraStatus("failed")
    }
  }, [])

  const checkSpeaker = useCallback(async () => {
    setSpeakerStatus("checking")
    try {
      const ctx = new AudioContext()
      if (ctx.state === "suspended") await ctx.resume()
      const oscillator = ctx.createOscillator()
      const gain = ctx.createGain()
      gain.gain.value = 0
      oscillator.connect(gain)
      gain.connect(ctx.destination)
      oscillator.start()
      oscillator.stop(ctx.currentTime + 0.1)
      await new Promise((r) => setTimeout(r, 200))
      await ctx.close()
      setSpeakerStatus("ready")
    } catch {
      setSpeakerStatus("failed")
    }
  }, [])

  const checkNetwork = useCallback(async () => {
    setNetworkStatus("checking")
    try {
      await learnerApi.getLiveSessionHealth()
      setNetworkStatus("ready")
      setConnectionStatus("connected")
    } catch {
      setNetworkStatus("failed")
      setConnectionStatus("disconnected")
    }
  }, [])

  useEffect(() => {
    checkMicrophone()
    checkCamera()
    checkSpeaker()
    checkNetwork()
  }, [checkMicrophone, checkCamera, checkSpeaker, checkNetwork])

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const ev = await learnerApi.getEvent(eventId)
        if (!cancelled) setEvent(ev)
      } catch (e: any) {
        if (!cancelled) setEventError(e?.message || tc("error.generic"))
      }
    }
    load()
    const pollMs = isLowBandwidth ? 45000 : 15000
    const id = setInterval(load, pollMs)
    return () => {
      cancelled = true
      clearInterval(id)
    }
  }, [eventId, tc, isLowBandwidth])

  useEffect(() => {
    if (isLive) announce(t("detail.live"))
  }, [isLive, t])

  const handleRetry = (check: "mic" | "camera" | "speaker" | "network") => {
    switch (check) {
      case "mic": checkMicrophone(); break
      case "camera": checkCamera(); break
      case "speaker": checkSpeaker(); break
      case "network": checkNetwork(); break
    }
  }

  const handleJoin = async () => {
    if (isEnded) {
      router.push(`/dashboard/learner/events/${eventId}`)
      return
    }
    if (isLive) {
      setJoining(true)
      try {
        const join = await learnerApi.joinEvent(eventId)
        announce(t("detail.joinLiveSession"))
        // In-app event room is not available yet — open the external meeting URL
        // only AFTER the backend join has recorded intent/token (§16/§73/§104).
        const fallbackUrl = join?.meetingUrl || event?.meetingUrl
        if (fallbackUrl) {
          window.open(fallbackUrl, "_blank", "noopener,noreferrer")
        }
      } catch (e: any) {
        // Last-resort external fallback when join fails but a meeting URL exists
        if (event?.meetingUrl) {
          window.open(event.meetingUrl, "_blank", "noopener,noreferrer")
          announce(t("join.external"))
        } else {
          announce(e?.message || tc("error.generic"))
          setEventError(e?.message || tc("error.generic"))
        }
      } finally {
        setJoining(false)
      }
      return
    }
    router.push(`/dashboard/learner/events/${eventId}/waiting`)
  }

  const checks: { key: "mic" | "camera" | "speaker" | "network"; status: CheckStatus; label: string; readyIcon: React.ReactNode; failIcon: React.ReactNode }[] = [
    { key: "mic", status: micStatus, label: t("preflight.microphone"), readyIcon: <Mic className="size-5" />, failIcon: <MicOff className="size-5" /> },
    { key: "camera", status: cameraStatus, label: t("preflight.camera"), readyIcon: <Camera className="size-5" />, failIcon: <CameraOff className="size-5" /> },
    { key: "speaker", status: speakerStatus, label: t("preflight.speaker"), readyIcon: <Volume2 className="size-5" />, failIcon: <VolumeX className="size-5" /> },
    { key: "network", status: networkStatus, label: t("preflight.network"), readyIcon: <Wifi className="size-5" />, failIcon: <WifiOff className="size-5" /> },
  ]

  return (
    <main role="main" aria-label={t("preflight.title")} className="space-y-6">
      <Link
        href={`/dashboard/learner/events/${eventId}`}
        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
        aria-label={tc("back")}
      >
        <ArrowLeft className="size-4" /> {tc("back")}
      </Link>

      <div className="rounded-xl border border-border bg-card p-6">
        <h1 className="text-2xl font-bold">{t("preflight.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("preflight.subtitle")}</p>
        {isLowBandwidth && (
          <p className="mt-2 flex items-center gap-1.5 rounded-lg border border-amber-500/20 bg-amber-500/5 px-3 py-2 text-xs text-amber-700" role="status">
            <WifiOff className="size-3.5 shrink-0" aria-hidden="true" />
            {t("lowBandwidth.notice")}
          </p>
        )}
        <p className="mt-2 text-xs text-muted-foreground" role="status" aria-live="polite">
          {eventStatus ? `${t("detail.eventDetails")}: ${eventStatus}` : t("checkingAccess")}
        </p>
        {eventError && (
          <p className="mt-2 text-xs text-destructive" role="alert">{eventError}</p>
        )}
        {isEnded && (
          <p className="mt-2 text-xs text-muted-foreground">{t("sessionEnded")}</p>
        )}
        {!isLive && !isEnded && event && (
          <p className="mt-2 text-xs text-muted-foreground">{t("waiting.sessionNotStarted")}</p>
        )}

        <div className="mt-6 space-y-4">
          {checks.map((check) => (
            <div key={check.key} className="flex items-center justify-between rounded-lg border border-border p-4">
              <div className="flex items-center gap-3">
                <div className={`flex size-10 items-center justify-center rounded-lg ${
                  check.status === "ready" ? "bg-green-100 text-green-700" :
                  check.status === "failed" ? "bg-red-100 text-red-700" :
                  "bg-muted text-muted-foreground"
                }`}>
                  {check.status === "checking" ? (
                    <Loader2 className="size-5 animate-spin" />
                  ) : check.status === "ready" ? (
                    check.readyIcon
                  ) : (
                    check.failIcon
                  )}
                </div>
                <div>
                  <p className="font-medium">{check.label}</p>
                  <p className="text-xs text-muted-foreground">
                    {check.status === "checking" && t("preflight.checking")}
                    {check.status === "ready" && (
                      <span className="text-green-600 flex items-center gap-1">
                        <CheckCircle className="size-3" /> {t("preflight.ready")}
                      </span>
                    )}
                    {check.status === "failed" && (
                      <span className="text-red-600 flex items-center gap-1">
                        <XCircle className="size-3" /> {t("preflight.failed")}
                      </span>
                    )}
                  </p>
                </div>
              </div>
              {check.status === "failed" && (
                <button
                  onClick={() => handleRetry(check.key)}
                  className="flex items-center gap-1.5 rounded-lg bg-muted px-3 py-1.5 text-xs font-medium hover:bg-muted/80"
                  aria-label={`${t("preflight.retry")} ${check.label}`}
                >
                  <RefreshCw className="size-3" /> {t("preflight.retry")}
                </button>
              )}
            </div>
          ))}
        </div>

        <div className="mt-6 flex items-center justify-between rounded-lg bg-muted/50 p-4">
          <div className="flex items-center gap-2">
            <div
              className={`size-2 rounded-full ${connectionStatus === "connected" ? "bg-green-500" : "bg-red-500"}`}
              aria-hidden="true"
              title={connectionStatus === "connected" ? t("preflight.connected") : t("preflight.disconnected")}
            />
            <span className="text-sm font-medium">{t("preflight.connectionStatus")}</span>
            <span className="sr-only" role="status">
              {connectionStatus === "connected" ? t("preflight.connected") : t("preflight.disconnected")}
            </span>
          </div>
          <span className="text-sm text-muted-foreground">
            {connectionStatus === "connected" ? t("preflight.connected") : t("preflight.disconnected")}
          </span>
        </div>

        {allReady && (
          <div className="mt-4 rounded-lg border border-green-200 bg-green-50 p-4 text-center">
            <p className="text-sm font-medium text-green-700">{t("preflight.allReady")}</p>
          </div>
        )}

        <div className="mt-6 space-y-2">
          <button
            onClick={handleJoin}
            disabled={!allReady || anyChecking || joining}
            className="flex h-12 w-full items-center justify-center rounded-lg bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            aria-label={t("preflight.joinLive")}
          >
            {anyChecking || joining ? <Loader2 className="size-4 animate-spin mr-2" /> : null}
            {isLive ? t("preflight.joinLive") : isEnded ? tc("back") : t("waitingRoom")}
          </button>
          {canOpenMeeting && (
            <a
              href={event!.meetingUrl!}
              target="_blank"
              rel="noopener noreferrer"
              className="flex h-10 w-full items-center justify-center gap-2 rounded-lg border border-border text-sm font-medium text-foreground hover:bg-muted"
              aria-label={`${t("detail.joinLiveSession")} — ${t("join.external")}`}
            >
              {t("detail.joinLiveSession")}
              <ExternalLink className="size-3.5" aria-hidden="true" />
              <span className="sr-only">{t("join.external")}</span>
            </a>
          )}
        </div>
      </div>
    </main>
  )
}
