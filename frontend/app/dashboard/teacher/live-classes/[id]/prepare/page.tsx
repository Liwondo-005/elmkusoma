"use client"

import { useEffect, useState, useRef } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
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

interface CheckResult {
  label: string
  status: "pending" | "checking" | "ready" | "failed"
  detail?: string
}

export default function PrepareLiveClassPage() {
  const params = useParams()
  const router = useRouter()
  const { user } = useAuth()
  const id = params.id as string
  const [liveClass, setLiveClass] = useState<LiveClass | null>(null)
  const [loading, setLoading] = useState(true)
  const [checks, setChecks] = useState<CheckResult[]>([
    { label: "Camera", status: "pending" },
    { label: "Microphone", status: "pending" },
    { label: "Speaker", status: "pending" },
    { label: "Network", status: "pending" },
    { label: "Live service", status: "pending" },
  ])
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null)
  const [micEnabled, setMicEnabled] = useState(false)
  const [cameraEnabled, setCameraEnabled] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    if (!id) return
    appFetch<LiveClass>(`/v1/learner/live-classes/${id}`)
      .then(setLiveClass)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  async function runChecks() {
    const results: CheckResult[] = []

    // Camera check
    results.push({ label: "Camera", status: "checking" })
    setChecks([...results, ...checks.slice(1)])
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true })
      setCameraStream(stream)
      setCameraEnabled(true)
      if (videoRef.current) videoRef.current.srcObject = stream
      results[results.length - 1] = { label: "Camera", status: "ready", detail: "Connected" }
    } catch {
      results[results.length - 1] = { label: "Camera", status: "failed", detail: "Not available" }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Mic check
    results.push({ label: "Microphone", status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      stream.getTracks().forEach((t) => t.stop())
      setMicEnabled(true)
      results[results.length - 1] = { label: "Microphone", status: "ready", detail: "Connected" }
    } catch {
      results[results.length - 1] = { label: "Microphone", status: "failed", detail: "Not available" }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Speaker check (assume available if audio context works)
    results.push({ label: "Speaker", status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const ctx = new AudioContext()
      await ctx.resume()
      ctx.close()
      results[results.length - 1] = { label: "Speaker", status: "ready", detail: "Available" }
    } catch {
      results[results.length - 1] = { label: "Speaker", status: "failed", detail: "Not available" }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Network check
    results.push({ label: "Network", status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    const start = Date.now()
    try {
      await fetch("/api/health", { method: "HEAD" }).catch(() => fetch("/"))
      const latency = Date.now() - start
      results[results.length - 1] = {
        label: "Network",
        status: "ready",
        detail: latency < 500 ? `Good (${latency}ms)` : `Slow (${latency}ms)`,
      }
    } catch {
      results[results.length - 1] = { label: "Network", status: "failed", detail: "No connection" }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Live service check
    results.push({ label: "Live service", status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const res = await appFetch<{ livekit: string }>("/v1/live-session/health")
      const status = (res as unknown as { details?: { livekit?: string } })?.details?.livekit || "configured"
      results[results.length - 1] = {
        label: "Live service",
        status: status === "not configured" ? "failed" : "ready",
        detail: status === "not configured" ? "Chat-only mode" : "Available",
      }
    } catch {
      results[results.length - 1] = { label: "Live service", status: "failed", detail: "Unavailable" }
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
        Back to Live Classes
      </Link>

      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Prepare Live Class</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Test your devices before going live. This will not start the session.
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
        <h2 className="text-sm font-semibold text-foreground">Device Checks</h2>

        {checks.map((check, i) => (
          <div key={i} className="flex items-center justify-between rounded-lg border border-border bg-background px-4 py-3">
            <div className="flex items-center gap-3">
              {check.label === "Camera" && (check.status === "ready" ? <Camera className="size-4 text-green-600" /> : <CameraOff className="size-4 text-muted-foreground" />)}
              {check.label === "Microphone" && (check.status === "ready" ? <Mic className="size-4 text-green-600" /> : <MicOff className="size-4 text-muted-foreground" />)}
              {check.label === "Speaker" && <Volume2 className="size-4 text-muted-foreground" />}
              {check.label === "Network" && (check.status === "ready" ? <Wifi className="size-4 text-green-600" /> : <WifiOff className="size-4 text-muted-foreground" />)}
              {check.label === "Live service" && (check.status === "ready" ? <CheckCircle2 className="size-4 text-green-600" /> : <XCircle className="size-4 text-muted-foreground" />)}
              <div>
                <p className="text-sm font-medium text-foreground">{check.label}</p>
                {check.detail && <p className="text-xs text-muted-foreground">{check.detail}</p>}
              </div>
            </div>
            <div>
              {check.status === "pending" && <span className="text-xs text-muted-foreground">Not checked</span>}
              {check.status === "checking" && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
              {check.status === "ready" && <CheckCircle2 className="size-4 text-green-600" />}
              {check.status === "failed" && <XCircle className="size-4 text-red-500" />}
            </div>
          </div>
        ))}

        {allReady && !hasFailed && (
          <div className="rounded-lg bg-green-500/10 p-3 text-sm text-green-700 dark:text-green-400">
            All checks passed. You are ready to go live.
          </div>
        )}
        {hasFailed && (
          <div className="rounded-lg bg-amber-500/10 p-3 text-sm text-amber-700 dark:text-amber-400">
            Some checks failed. You can still proceed in chat-only mode, but video/audio will not be available.
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
          {checks.some((c) => c.status === "checking") ? "Checking..." : "Run Checks"}
        </Button>
        <div className="flex gap-2">
          {cameraStream && (
            <Button variant="outline" onClick={stopCamera}>
              Stop Camera
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
            Start Live Class
          </Button>
        </div>
      </div>
    </div>
  )
}
