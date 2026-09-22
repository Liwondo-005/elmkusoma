"use client"

import { useEffect, useState, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { learnerApi } from "@/lib/learner-api"
import { ArrowLeft, Mic, MicOff, Camera, CameraOff, Volume2, VolumeX, Wifi, WifiOff, Loader2, CheckCircle, XCircle, RefreshCw } from "lucide-react"

type CheckStatus = "checking" | "ready" | "failed"

interface DeviceCheck {
  status: CheckStatus
  label: string
  icon: React.ReactNode
  failIcon: React.ReactNode
}

export default function PreflightPage() {
  const params = useParams()
  const router = useRouter()
  const t = useTranslations("events")
  const tc = useTranslations("common")
  const eventId = params.id as string

  const [micStatus, setMicStatus] = useState<CheckStatus>("checking")
  const [cameraStatus, setCameraStatus] = useState<CheckStatus>("checking")
  const [speakerStatus, setSpeakerStatus] = useState<CheckStatus>("checking")
  const [networkStatus, setNetworkStatus] = useState<CheckStatus>("checking")
  const [connectionStatus, setConnectionStatus] = useState<"connected" | "disconnected">("disconnected")

  const allReady = micStatus === "ready" && cameraStatus === "ready" && speakerStatus === "ready" && networkStatus === "ready"
  const anyFailed = micStatus === "failed" || cameraStatus === "failed" || speakerStatus === "failed" || networkStatus === "failed"
  const anyChecking = micStatus === "checking" || cameraStatus === "checking" || speakerStatus === "checking" || networkStatus === "checking"

  const checkMicrophone = useCallback(async () => {
    setMicStatus("checking")
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      stream.getTracks().forEach((t) => t.stop())
      setMicStatus("ready")
    } catch {
      setMicStatus("failed")
    }
  }, [])

  const checkCamera = useCallback(async () => {
    setCameraStatus("checking")
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true })
      stream.getTracks().forEach((t) => t.stop())
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

  const handleRetry = (check: "mic" | "camera" | "speaker" | "network") => {
    switch (check) {
      case "mic": checkMicrophone(); break
      case "camera": checkCamera(); break
      case "speaker": checkSpeaker(); break
      case "network": checkNetwork(); break
    }
  }

  const handleJoin = () => {
    router.push(`/dashboard/learner/events/${eventId}`)
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
            <div className={`size-2 rounded-full ${connectionStatus === "connected" ? "bg-green-500" : "bg-red-500"}`} />
            <span className="text-sm font-medium">{t("preflight.connectionStatus")}</span>
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

        <div className="mt-6">
          <button
            onClick={handleJoin}
            disabled={!allReady || anyChecking}
            className="flex h-12 w-full items-center justify-center rounded-lg bg-primary text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            aria-label={t("preflight.joinLive")}
          >
            {anyChecking ? <Loader2 className="size-4 animate-spin mr-2" /> : null}
            {t("preflight.joinLive")}
          </button>
        </div>
      </div>
    </main>
  )
}
