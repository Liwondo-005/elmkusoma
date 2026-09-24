"use client"

import { useEffect, useState, useRef } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { appFetch } from "@/lib/fetch"
import {
  ArrowLeft,
  Camera,
  CameraOff,
  Mic,
  MicOff,
  Wifi,
  WifiOff,
  CheckCircle2,
  XCircle,
  Loader2,
  Video,
  Volume2,
} from "lucide-react"

interface LiveClass {
  id: string
  title: string
  description: string
  scheduledAt: string
  durationMinutes: number
  status: string
  subjectName: string | null
  classGroupId: string | null
}

type CheckKey = "camera" | "microphone" | "speaker" | "network" | "service"

interface CheckResult {
  key: CheckKey
  label: string
  status: "pending" | "checking" | "ready" | "failed"
  detail?: string
}

export default function PrepareLiveClassPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const id = params.id as string
  const [liveClass, setLiveClass] = useState<LiveClass | null>(null)
  const [loading, setLoading] = useState(true)
  const [checks, setChecks] = useState<CheckResult[]>([
    { key: "camera", label: t("livePrepare.checkCamera"), status: "pending" },
    { key: "microphone", label: t("livePrepare.checkMicrophone"), status: "pending" },
    { key: "speaker", label: t("livePrepare.checkSpeaker"), status: "pending" },
    { key: "network", label: t("livePrepare.checkNetwork"), status: "pending" },
    { key: "service", label: t("livePrepare.checkService"), status: "pending" },
  ])
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null)
  const [micEnabled, setMicEnabled] = useState(false)
  const [cameraEnabled, setCameraEnabled] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    if (!id) return
    appFetch<LiveClass>(`/v1/teachers/me/live-classes/${id}`)
      .then(setLiveClass)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  async function runChecks() {
    const results: CheckResult[] = []

    // Camera check
    results.push({ key: "camera", label: t("livePrepare.checkCamera"), status: "checking" })
    setChecks([...results, ...checks.slice(1)])
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true })
      setCameraStream(stream)
      setCameraEnabled(true)
      if (videoRef.current) videoRef.current.srcObject = stream
      results[results.length - 1] = { key: "camera", label: t("livePrepare.checkCamera"), status: "ready", detail: t("livePrepare.detailConnected") }
    } catch {
      results[results.length - 1] = { key: "camera", label: t("livePrepare.checkCamera"), status: "failed", detail: t("livePrepare.detailNotAvailable") }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Mic check
    results.push({ key: "microphone", label: t("livePrepare.checkMicrophone"), status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      stream.getTracks().forEach((t) => t.stop())
      setMicEnabled(true)
      results[results.length - 1] = { key: "microphone", label: t("livePrepare.checkMicrophone"), status: "ready", detail: t("livePrepare.detailConnected") }
    } catch {
      results[results.length - 1] = { key: "microphone", label: t("livePrepare.checkMicrophone"), status: "failed", detail: t("livePrepare.detailNotAvailable") }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Speaker check (assume available if audio context works)
    results.push({ key: "speaker", label: t("livePrepare.checkSpeaker"), status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const ctx = new AudioContext()
      await ctx.resume()
      ctx.close()
      results[results.length - 1] = { key: "speaker", label: t("livePrepare.checkSpeaker"), status: "ready", detail: t("livePrepare.detailAvailable") }
    } catch {
      results[results.length - 1] = { key: "speaker", label: t("livePrepare.checkSpeaker"), status: "failed", detail: t("livePrepare.detailNotAvailable") }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Network check
    results.push({ key: "network", label: t("livePrepare.checkNetwork"), status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    const start = Date.now()
    try {
      await fetch("/api/health", { method: "HEAD" }).catch(() => fetch("/"))
      const latency = Date.now() - start
      results[results.length - 1] = {
        key: "network",
        label: t("livePrepare.checkNetwork"),
        status: "ready",
        detail: latency < 500 ? t("livePrepare.netGood", { latency }) : t("livePrepare.netSlow", { latency }),
      }
    } catch {
      results[results.length - 1] = { key: "network", label: t("livePrepare.checkNetwork"), status: "failed", detail: t("livePrepare.detailNoConnection") }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Live service check
    results.push({ key: "service", label: t("livePrepare.checkService"), status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const res = await appFetch<{ livekit: string }>("/v1/live-session/health")
      const status = (res as unknown as { details?: { livekit?: string } })?.details?.livekit || "configured"
      results[results.length - 1] = {
        key: "service",
        label: t("livePrepare.checkService"),
        status: status === "not configured" ? "failed" : "ready",
        detail: status === "not configured" ? t("livePrepare.detailChatOnly") : t("livePrepare.detailAvailable"),
      }
    } catch {
      results[results.length - 1] = { key: "service", label: t("livePrepare.checkService"), status: "failed", detail: t("livePrepare.detailUnavailable") }
    }
    setChecks(results)
  }

  function stopCamera() {
    if (cameraStream) {
      cameraStream.getTracks().forEach((t) => t.stop())
      setCameraStream(null)
      setCameraEnabled(false)
    }
  }

  useEffect(() => {
    return () => {
      if (cameraStream) cameraStream.getTracks().forEach((t) => t.stop())
    }
  }, [cameraStream])

  const allReady = checks.length > 0 && checks.every((c) => c.status === "ready" || c.status === "failed")
  const hasFailed = checks.some((c) => c.status === "failed")

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-6">
      <Link href="/dashboard/teacher/live-classes" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" />
        {t("liveClasses.backToList")}
      </Link>

      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("livePrepare.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {t("livePrepare.subtitle")}
        </p>
      </div>

      {liveClass && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Video className="size-5 text-primary" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-foreground">{liveClass.title}</h2>
              <p className="text-xs text-muted-foreground">
                {liveClass.scheduledAt ? new Date(liveClass.scheduledAt).toLocaleString() : "—"}
                {liveClass.subjectName && ` • ${liveClass.subjectName}`}
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
        <h2 className="text-sm font-semibold text-foreground">{t("livePrepare.deviceChecks")}</h2>

        {checks.map((check, i) => (
          <div key={i} className="flex items-center justify-between rounded-lg border border-border bg-background px-4 py-3">
            <div className="flex items-center gap-3">
              {check.key === "camera" && (check.status === "ready" ? <Camera className="size-4 text-green-600" /> : <CameraOff className="size-4 text-muted-foreground" />)}
              {check.key === "microphone" && (check.status === "ready" ? <Mic className="size-4 text-green-600" /> : <MicOff className="size-4 text-muted-foreground" />)}
              {check.key === "speaker" && <Volume2 className="size-4 text-muted-foreground" />}
              {check.key === "network" && (check.status === "ready" ? <Wifi className="size-4 text-green-600" /> : <WifiOff className="size-4 text-muted-foreground" />)}
              {check.key === "service" && (check.status === "ready" ? <CheckCircle2 className="size-4 text-green-600" /> : <XCircle className="size-4 text-muted-foreground" />)}
              <div>
                <p className="text-sm font-medium text-foreground">{check.label}</p>
                {check.detail && <p className="text-xs text-muted-foreground">{check.detail}</p>}
              </div>
            </div>
            <div>
              {check.status === "pending" && <span className="text-xs text-muted-foreground">{t("livePrepare.notChecked")}</span>}
              {check.status === "checking" && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
              {check.status === "ready" && <CheckCircle2 className="size-4 text-green-600" />}
              {check.status === "failed" && <XCircle className="size-4 text-red-500" />}
            </div>
          </div>
        ))}

        {allReady && !hasFailed && (
          <div className="rounded-lg bg-green-500/10 p-3 text-sm text-green-700 dark:text-green-400">
            {t("livePrepare.allPassed")}
          </div>
        )}
        {hasFailed && (
          <div className="rounded-lg bg-amber-500/10 p-3 text-sm text-amber-700 dark:text-amber-400">
            {t("livePrepare.someFailed")}
          </div>
        )}
      </div>

      {cameraEnabled && (
        <div className="rounded-2xl border border-border bg-card overflow-hidden">
          <video ref={videoRef} autoPlay playsInline muted className="w-full aspect-video object-cover bg-black" />
        </div>
      )}

      <div className="flex items-center justify-between">
        <Button variant="outline" onClick={runChecks} disabled={checks.some((c) => c.status === "checking")}>
          {checks.some((c) => c.status === "checking") ? t("livePrepare.checking") : t("livePrepare.runChecks")}
        </Button>
        <div className="flex gap-2">
          {cameraStream && (
            <Button variant="outline" onClick={stopCamera}>
              {t("livePrepare.stopCamera")}
            </Button>
          )}
          <Button
            className="gap-1 bg-green-600 hover:bg-green-700 text-white"
            onClick={() => {
              stopCamera()
              window.open(`/live-classes/${id}`, "_blank")
            }}
          >
            <Video className="size-4" />
            {t("livePrepare.startLive")}
          </Button>
        </div>
      </div>
    </div>
  )
}
