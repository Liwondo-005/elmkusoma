"use client"

import { useEffect, useState, useRef, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { appFetch } from "@/lib/fetch"
import { useMediaDevices, describeMediaError, type MediaErrorInfo } from "@/hooks/use-media-devices"
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
  Smartphone,
  Usb,
  Aperture,
  MonitorPlay,
  Cpu,
  Clapperboard,
  Radio,
  Copy,
  Check,
  MessageSquare,
  HelpCircle,
  BarChart3,
  ClipboardList,
  Hand,
  MonitorUp,
  Circle,
  AlertTriangle,
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
  recordingEnabled?: boolean
  broadcastSource?: string
}

type CheckKey = "camera" | "microphone" | "speaker" | "network" | "service"

interface CheckResult {
  key: CheckKey
  label: string
  status: "pending" | "checking" | "ready" | "failed"
  detail?: string
}

type BroadcastSource = "BROWSER" | "MOBILE" | "USB_CAMERA" | "PROFESSIONAL_CAMERA" | "OBS" | "ENCODER" | "STUDIO" | "OTHER"

const SOURCE_OPTIONS: Array<{ value: BroadcastSource; icon: typeof Video }> = [
  { value: "BROWSER", icon: Video },
  { value: "MOBILE", icon: Smartphone },
  { value: "USB_CAMERA", icon: Usb },
  { value: "PROFESSIONAL_CAMERA", icon: Aperture },
  { value: "OBS", icon: MonitorPlay },
  { value: "ENCODER", icon: Cpu },
  { value: "STUDIO", icon: Clapperboard },
  { value: "OTHER", icon: Radio },
]

const INGEST_SOURCES: BroadcastSource[] = ["OBS", "ENCODER", "STUDIO"]

interface IngressInfo {
  ingressId: string | null
  inputUrl: string | null
  streamKey: string | null
  status: string | null
  inputType?: string | null
}

interface IngressState {
  configured: boolean
  ingresses: IngressInfo[]
  message?: string
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

  // --- Source selection (persisted on the existing live class) ---
  const [broadcastSource, setBroadcastSource] = useState<BroadcastSource>("BROWSER")
  const [sourceBusy, setSourceBusy] = useState(false)
  const [sourceError, setSourceError] = useState("")
  const [sourceSaved, setSourceSaved] = useState(false)

  // --- Real device enumeration ---
  const {
    cameras,
    microphones,
    hasLabels,
    permission,
    error: deviceEnumError,
    supported: devicesSupported,
    requestPermission,
    getCameraStream,
    getMicrophoneStream,
  } = useMediaDevices()
  const [selectedCameraId, setSelectedCameraId] = useState("")
  const [selectedMicId, setSelectedMicId] = useState("")
  const [cameraError, setCameraError] = useState<MediaErrorInfo | null>(null)
  const [micError, setMicError] = useState<MediaErrorInfo | null>(null)

  // --- External ingest (OBS / encoder / studio) ---
  const [ingress, setIngress] = useState<IngressState | null>(null)
  const [ingressLoading, setIngressLoading] = useState(false)
  const [ingressBusy, setIngressBusy] = useState(false)
  const [ingressError, setIngressError] = useState("")
  const [ingestProtocol, setIngestProtocol] = useState<"WHIP" | "RTMP" | "SRT">("WHIP")
  const [copiedField, setCopiedField] = useState("")

  // --- Phone source link ---
  const [joinUrl, setJoinUrl] = useState("")

  // --- GO LIVE ---
  const [goLiveBusy, setGoLiveBusy] = useState(false)
  const [goLiveError, setGoLiveError] = useState("")

  const isIngestSource = INGEST_SOURCES.includes(broadcastSource)
  const canEditSource = liveClass?.status === "SCHEDULED"

  useEffect(() => {
    if (!id) return
    appFetch<LiveClass>(`/v1/teachers/me/live-classes/${id}`)
      .then((lc) => {
        setLiveClass(lc)
        setBroadcastSource((lc.broadcastSource as BroadcastSource) || "BROWSER")
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (typeof window !== "undefined") {
      setJoinUrl(`${window.location.origin}/live-classes/${id}`)
    }
  }, [id])

  // ---- Ingest endpoints (only for OBS/encoder/studio sources) ----
  const loadIngress = useCallback(async () => {
    setIngressLoading(true)
    setIngressError("")
    try {
      const data = await appFetch<IngressState>(`/v1/live-session/classes/${id}/ingress`)
      setIngress(data)
    } catch (err) {
      setIngress(null)
      setIngressError(err instanceof Error ? err.message : t("livePrepare.ingestLoadFailed"))
    } finally {
      setIngressLoading(false)
    }
  }, [id, t])

  useEffect(() => {
    if (isIngestSource) loadIngress()
  }, [isIngestSource, loadIngress])

  async function saveSource(source: BroadcastSource) {
    if (!liveClass || sourceBusy || !canEditSource) return
    setSourceBusy(true)
    setSourceError("")
    setSourceSaved(false)
    try {
      const updated = await appFetch<LiveClass>(`/v1/teachers/me/live-classes/${id}`, {
        method: "PUT",
        body: JSON.stringify({
          title: liveClass.title,
          scheduledAt: liveClass.scheduledAt,
          durationMinutes: liveClass.durationMinutes,
          broadcastSource: source,
        }),
      })
      setLiveClass(updated)
      setBroadcastSource((updated.broadcastSource as BroadcastSource) || source)
      setSourceSaved(true)
      setTimeout(() => setSourceSaved(false), 2500)
    } catch (err) {
      setSourceError(err instanceof Error ? err.message : t("livePrepare.sourceSaveFailed"))
    } finally {
      setSourceBusy(false)
    }
  }

  async function createIngest() {
    setIngressBusy(true)
    setIngressError("")
    try {
      await appFetch(`/v1/live-session/classes/${id}/ingress`, {
        method: "POST",
        body: JSON.stringify({ protocol: ingestProtocol }),
      })
      await loadIngress()
    } catch (err) {
      setIngressError(err instanceof Error ? err.message : t("livePrepare.ingestCreateFailed"))
    } finally {
      setIngressBusy(false)
    }
  }

  async function removeIngest() {
    setIngressBusy(true)
    setIngressError("")
    try {
      await appFetch(`/v1/live-session/classes/${id}/ingress`, { method: "DELETE" })
      await loadIngress()
    } catch (err) {
      setIngressError(err instanceof Error ? err.message : t("livePrepare.ingestDeleteFailed"))
    } finally {
      setIngressBusy(false)
    }
  }

  function copyField(field: string, value: string) {
    navigator.clipboard
      ?.writeText(value)
      .then(() => {
        setCopiedField(field)
        setTimeout(() => setCopiedField(""), 2000)
      })
      .catch(() => {})
  }

  // ---- Preview / device checks ----
  const startPreview = useCallback(async () => {
    try {
      const stream = await getCameraStream(selectedCameraId || undefined)
      setCameraStream((prev) => {
        prev?.getTracks().forEach((tr) => tr.stop())
        return stream
      })
      setCameraEnabled(true)
      setCameraError(null)
      if (videoRef.current) videoRef.current.srcObject = stream
    } catch (err) {
      setCameraEnabled(false)
      setCameraError(describeMediaError(err, "camera"))
    }
  }, [getCameraStream, selectedCameraId])

  // Restart the preview when the teacher picks another camera (device switching).
  useEffect(() => {
    if (cameraEnabled) startPreview()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCameraId])

  async function runChecks() {
    const results: CheckResult[] = []

    // Camera check — uses the selected camera when one is chosen.
    results.push({ key: "camera", label: t("livePrepare.checkCamera"), status: "checking" })
    setChecks([...results, ...checks.slice(1)])
    try {
      const stream = await getCameraStream(selectedCameraId || undefined)
      setCameraStream((prev) => {
        prev?.getTracks().forEach((tr) => tr.stop())
        return stream
      })
      setCameraEnabled(true)
      setCameraError(null)
      if (videoRef.current) videoRef.current.srcObject = stream
      results[results.length - 1] = { key: "camera", label: t("livePrepare.checkCamera"), status: "ready", detail: t("livePrepare.detailConnected") }
    } catch (err) {
      setCameraError(describeMediaError(err, "camera"))
      results[results.length - 1] = { key: "camera", label: t("livePrepare.checkCamera"), status: "failed", detail: t("livePrepare.detailNotAvailable") }
    }
    setChecks([...results, ...checks.slice(results.length)])

    // Mic check — uses the selected microphone when one is chosen.
    results.push({ key: "microphone", label: t("livePrepare.checkMicrophone"), status: "checking" })
    setChecks([...results, ...checks.slice(results.length)])
    try {
      const stream = await getMicrophoneStream(selectedMicId || undefined)
      stream.getTracks().forEach((tr) => tr.stop())
      setMicEnabled(true)
      setMicError(null)
      results[results.length - 1] = { key: "microphone", label: t("livePrepare.checkMicrophone"), status: "ready", detail: t("livePrepare.detailConnected") }
    } catch (err) {
      setMicError(describeMediaError(err, "microphone"))
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
      cameraStream.getTracks().forEach((tr) => tr.stop())
      setCameraStream(null)
      setCameraEnabled(false)
    }
  }

  useEffect(() => {
    return () => {
      if (cameraStream) cameraStream.getTracks().forEach((tr) => tr.stop())
    }
  }, [cameraStream])

  // ---- GO LIVE: starts the session through the existing lifecycle, then opens the classroom ----
  async function goLive() {
    if (!liveClass || goLiveBusy) return
    setGoLiveBusy(true)
    setGoLiveError("")
    try {
      if (liveClass.status === "SCHEDULED") {
        const started = await appFetch<LiveClass>(`/v1/teachers/me/live-classes/${id}/start`, { method: "POST" })
        setLiveClass({ ...liveClass, status: started.status || "IN_PROGRESS" })
      }
      stopCamera()
      router.push(`/live-classes/${id}`)
    } catch (err) {
      setGoLiveError(err instanceof Error ? err.message : t("livePrepare.goLiveFailed"))
    } finally {
      setGoLiveBusy(false)
    }
  }

  const allReady = checks.length > 0 && checks.every((c) => c.status === "ready" || c.status === "failed")
  const hasFailed = checks.some((c) => c.status === "failed")

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  const sourceLocked = !canEditSource

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <Link href="/dashboard/teacher/live-classes" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" />
        {t("liveClasses.backToList")}
      </Link>

      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("livePrepare.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("livePrepare.subtitle")}</p>
      </div>

      {liveClass && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
              <Video className="size-5 text-primary" />
            </div>
            <div className="min-w-0">
              <h2 className="truncate text-sm font-semibold text-foreground">{liveClass.title}</h2>
              <p className="text-xs text-muted-foreground">
                {liveClass.scheduledAt ? new Date(liveClass.scheduledAt).toLocaleString() : "—"}
                {liveClass.subjectName && ` • ${liveClass.subjectName}`}
                {` • ${liveClass.status}`}
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        {/* ---------------- SOURCE ---------------- */}
        <div className="space-y-4 rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div>
            <h2 className="text-sm font-semibold text-foreground">{t("livePrepare.sourceSection")}</h2>
            <p className="mt-1 text-xs text-muted-foreground">{t("livePrepare.sourceHint")}</p>
          </div>

          <div className="grid grid-cols-2 gap-2">
            {SOURCE_OPTIONS.map(({ value, icon: Icon }) => {
              const active = broadcastSource === value
              return (
                <button
                  key={value}
                  type="button"
                  disabled={sourceLocked || sourceBusy}
                  onClick={() => saveSource(value)}
                  className={`flex items-center gap-2 rounded-lg border px-3 py-2.5 text-left text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
                    active
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border bg-background text-foreground hover:border-primary/40"
                  }`}
                  title={t(`livePrepare.source.${value}`)}
                >
                  <Icon className={active ? "size-4 text-primary" : "size-4 text-muted-foreground"} />
                  <span className="truncate text-xs font-medium">{t(`livePrepare.source.${value}`)}</span>
                </button>
              )
            })}
          </div>

          {sourceBusy && (
            <p className="flex items-center gap-2 text-xs text-muted-foreground">
              <Loader2 className="size-3 animate-spin" /> {t("livePrepare.sourceSaving")}
            </p>
          )}
          {sourceSaved && !sourceBusy && (
            <p className="flex items-center gap-2 text-xs text-green-700 dark:text-green-400">
              <Check className="size-3" /> {t("livePrepare.sourceSaved")}
            </p>
          )}
          {sourceError && (
            <p className="flex items-center gap-2 text-xs text-red-600">
              <XCircle className="size-3" /> {sourceError}
            </p>
          )}
          {sourceLocked && (
            <p className="rounded-lg bg-muted p-2.5 text-xs text-muted-foreground">{t("livePrepare.sourceLocked")}</p>
          )}

          {/* Phone source: real guidance — the classroom runs in the phone's browser */}
          {broadcastSource === "MOBILE" && (
            <div className="rounded-lg border border-teal-200 bg-teal-50 p-3 dark:border-teal-900 dark:bg-teal-950/30">
              <p className="flex items-center gap-2 text-xs font-semibold text-teal-800 dark:text-teal-300">
                <Smartphone className="size-4" /> {t("livePrepare.mobileTitle")}
              </p>
              <p className="mt-1.5 text-xs text-teal-800/80 dark:text-teal-300/80">{t("livePrepare.mobileHint")}</p>
              <div className="mt-2 flex items-center gap-2">
                <code className="min-w-0 flex-1 truncate rounded bg-background px-2 py-1.5 text-[11px] text-muted-foreground">{joinUrl}</code>
                <button
                  type="button"
                  onClick={() => copyField("link", joinUrl)}
                  className="inline-flex shrink-0 items-center gap-1 rounded-md border border-border bg-background px-2 py-1.5 text-[11px] font-medium text-foreground hover:bg-accent"
                >
                  {copiedField === "link" ? <Check className="size-3" /> : <Copy className="size-3" />}
                  {copiedField === "link" ? t("livePrepare.copied") : t("livePrepare.copyLink")}
                </button>
              </div>
            </div>
          )}

          {/* Professional camera: honest path — capture card shows up as a camera, or push via OBS/encoder */}
          {broadcastSource === "PROFESSIONAL_CAMERA" && (
            <div className="rounded-lg border border-border bg-background p-3 text-xs text-muted-foreground">
              {t("livePrepare.proCameraHint")}
            </div>
          )}

          {/* OBS / Encoder / Studio: real LiveKit ingress endpoints */}
          {isIngestSource && (
            <div className="space-y-3 rounded-lg border border-border bg-background p-3">
              <p className="flex items-center gap-2 text-xs font-semibold text-foreground">
                <MonitorPlay className="size-4 text-primary" /> {t("livePrepare.ingestTitle")}
              </p>

              {ingressLoading && (
                <p className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Loader2 className="size-3 animate-spin" /> {t("livePrepare.ingestLoading")}
                </p>
              )}

              {ingress && !ingress.configured && (
                <div className="flex items-start gap-2 rounded-md bg-amber-500/10 p-2.5 text-xs text-amber-700 dark:text-amber-400">
                  <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
                  <span>{ingress.message || t("livePrepare.ingestNotConfigured")}</span>
                </div>
              )}

              {ingress?.configured && (
                <>
                  <p className="text-xs text-muted-foreground">{t("livePrepare.ingestConfigured")}</p>

                  {ingress.ingresses.length === 0 && (
                    <div className="flex flex-wrap items-center gap-2">
                      <select
                        value={ingestProtocol}
                        onChange={(e) => setIngestProtocol(e.target.value as "WHIP" | "RTMP" | "SRT")}
                        className="rounded-md border border-border bg-background px-2 py-1.5 text-xs text-foreground"
                      >
                        <option value="WHIP">{t("livePrepare.protocolWhip")}</option>
                        <option value="RTMP">{t("livePrepare.protocolRtmp")}</option>
                        <option value="SRT">{t("livePrepare.protocolSrt")}</option>
                      </select>
                      <Button size="sm" className="h-7 text-xs" onClick={createIngest} disabled={ingressBusy}>
                        {ingressBusy ? <Loader2 className="size-3 animate-spin" /> : null}
                        {t("livePrepare.createIngest")}
                      </Button>
                    </div>
                  )}

                  {ingress.ingresses.map((ing, i) => (
                    <div key={ing.ingressId || i} className="space-y-1.5 rounded-md border border-border bg-card p-2.5">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-[11px] font-medium text-muted-foreground">
                          {ing.inputType || ingestProtocol} • {ing.status || "—"}
                        </span>
                      </div>
                      {ing.inputUrl && (
                        <div className="flex items-center gap-1.5">
                          <span className="w-16 shrink-0 text-[11px] text-muted-foreground">{t("livePrepare.ingestUrl")}</span>
                          <code className="min-w-0 flex-1 truncate text-[11px] text-foreground">{ing.inputUrl}</code>
                          <button
                            type="button"
                            onClick={() => copyField(`url-${i}`, ing.inputUrl || "")}
                            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-accent"
                            title={t("livePrepare.copyLink")}
                          >
                            {copiedField === `url-${i}` ? <Check className="size-3" /> : <Copy className="size-3" />}
                          </button>
                        </div>
                      )}
                      {ing.streamKey && (
                        <div className="flex items-center gap-1.5">
                          <span className="w-16 shrink-0 text-[11px] text-muted-foreground">{t("livePrepare.streamKey")}</span>
                          <code className="min-w-0 flex-1 select-none truncate text-[11px] text-foreground">{ing.streamKey}</code>
                          <button
                            type="button"
                            onClick={() => copyField(`key-${i}`, ing.streamKey || "")}
                            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-accent"
                            title={t("livePrepare.copyLink")}
                          >
                            {copiedField === `key-${i}` ? <Check className="size-3" /> : <Copy className="size-3" />}
                          </button>
                        </div>
                      )}
                    </div>
                  ))}

                  {ingress.ingresses.length > 0 && (
                    <Button size="sm" variant="outline" className="h-7 text-xs" onClick={removeIngest} disabled={ingressBusy}>
                      {ingressBusy ? <Loader2 className="size-3 animate-spin" /> : null}
                      {t("livePrepare.deleteIngest")}
                    </Button>
                  )}
                </>
              )}

              {ingressError && (
                <p className="flex items-center gap-2 text-xs text-red-600">
                  <XCircle className="size-3" /> {ingressError}
                </p>
              )}
            </div>
          )}
        </div>

        {/* ---------------- DEVICES ---------------- */}
        <div className="space-y-4 rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div>
            <h2 className="text-sm font-semibold text-foreground">{t("livePrepare.devicesSection")}</h2>
            <p className="mt-1 text-xs text-muted-foreground">{t("livePrepare.devicesHint")}</p>
          </div>

          {!devicesSupported && (
            <div className="flex items-start gap-2 rounded-md bg-amber-500/10 p-2.5 text-xs text-amber-700 dark:text-amber-400">
              <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
              <span>{deviceEnumError?.message || t("livePrepare.devicesUnsupported")}</span>
            </div>
          )}

          {devicesSupported && !hasLabels && (
            <div className="space-y-2">
              <Button size="sm" variant="outline" className="h-8 text-xs" onClick={requestPermission}>
                <Camera className="size-3.5" />
                {t("livePrepare.allowDevices")}
              </Button>
              <p className="text-[11px] text-muted-foreground">{t("livePrepare.deviceNamesHint")}</p>
            </div>
          )}

          <div className="space-y-2">
            <label className="flex items-center gap-2 text-xs font-medium text-foreground">
              <Camera className="size-4 text-muted-foreground" />
              {t("livePrepare.selectCamera")}
            </label>
            <select
              value={selectedCameraId}
              onChange={(e) => setSelectedCameraId(e.target.value)}
              disabled={cameras.length === 0}
              className="w-full rounded-md border border-border bg-background px-2.5 py-2 text-sm text-foreground disabled:opacity-60"
            >
              <option value="">{t("livePrepare.defaultDevice")}</option>
              {cameras.map((c) => (
                <option key={c.deviceId} value={c.deviceId}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label className="flex items-center gap-2 text-xs font-medium text-foreground">
              <Mic className="size-4 text-muted-foreground" />
              {t("livePrepare.selectMicrophone")}
            </label>
            <select
              value={selectedMicId}
              onChange={(e) => setSelectedMicId(e.target.value)}
              disabled={microphones.length === 0}
              className="w-full rounded-md border border-border bg-background px-2.5 py-2 text-sm text-foreground disabled:opacity-60"
            >
              <option value="">{t("livePrepare.defaultDevice")}</option>
              {microphones.map((m) => (
                <option key={m.deviceId} value={m.deviceId}>
                  {m.label}
                </option>
              ))}
            </select>
          </div>

          {cameraError && (
            <p className="flex items-start gap-2 text-xs text-red-600">
              <XCircle className="mt-0.5 size-3.5 shrink-0" /> {cameraError.message}
            </p>
          )}
          {micError && (
            <p className="flex items-start gap-2 text-xs text-red-600">
              <XCircle className="mt-0.5 size-3.5 shrink-0" /> {micError.message}
            </p>
          )}
          {permission === "denied" && (
            <p className="rounded-md bg-amber-500/10 p-2.5 text-xs text-amber-700 dark:text-amber-400">{t("livePrepare.permissionDenied")}</p>
          )}

          <div className="flex items-center gap-2">
            <Button size="sm" variant="outline" className="h-8 text-xs" onClick={startPreview} disabled={cameraEnabled}>
              {t("livePrepare.startCamera")}
            </Button>
            {cameraStream && (
              <Button size="sm" variant="outline" className="h-8 text-xs" onClick={stopCamera}>
                {t("livePrepare.stopCamera")}
              </Button>
            )}
          </div>

          {cameraEnabled && (
            <div className="overflow-hidden rounded-xl border border-border">
              <video ref={videoRef} autoPlay playsInline muted className="aspect-video w-full bg-black object-cover" />
            </div>
          )}
        </div>
      </div>

      {/* ---------------- DEVICE STATUS ---------------- */}
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

      {/* ---------------- LEARNING TOOLS ---------------- */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-3">
        <div>
          <h2 className="text-sm font-semibold text-foreground">{t("livePrepare.toolsSection")}</h2>
          <p className="mt-1 text-xs text-muted-foreground">{t("livePrepare.toolsHint")}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {[
            { icon: MessageSquare, label: t("livePrepare.toolChat") },
            { icon: HelpCircle, label: t("livePrepare.toolQa") },
            { icon: BarChart3, label: t("livePrepare.toolPoll") },
            { icon: ClipboardList, label: t("livePrepare.toolQuiz") },
            { icon: Hand, label: t("livePrepare.toolHand") },
            { icon: MonitorUp, label: t("livePrepare.toolScreen") },
          ].map(({ icon: Icon, label }) => (
            <span key={label} className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background px-3 py-1.5 text-xs text-foreground">
              <Icon className="size-3.5 text-primary" /> {label}
            </span>
          ))}
          <span
            className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs ${
              liveClass?.recordingEnabled
                ? "border-red-200 bg-red-50 text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400"
                : "border-border bg-background text-muted-foreground"
            }`}
          >
            <Circle className="size-3.5" /> {liveClass?.recordingEnabled ? t("livePrepare.toolRecording") : t("livePrepare.toolRecordingOff")}
          </span>
        </div>
      </div>

      {/* ---------------- GO LIVE ---------------- */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        {goLiveError && (
          <p className="mb-3 flex items-center gap-2 text-sm text-red-600">
            <XCircle className="size-4 shrink-0" /> {goLiveError}
          </p>
        )}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="text-xs text-muted-foreground">
            {liveClass?.status === "SCHEDULED"
              ? t("livePrepare.goLiveScheduledHint")
              : liveClass?.status === "IN_PROGRESS" || liveClass?.status === "LIVE"
                ? t("livePrepare.goLiveRunningHint")
                : t("livePrepare.goLiveEndedHint")}
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={runChecks} disabled={checks.some((c) => c.status === "checking")}>
              {checks.some((c) => c.status === "checking") ? t("livePrepare.checking") : t("livePrepare.runChecks")}
            </Button>
            <Button
              className="gap-1 bg-green-600 text-white hover:bg-green-700"
              onClick={goLive}
              disabled={
                goLiveBusy ||
                !liveClass ||
                (liveClass.status !== "SCHEDULED" && liveClass.status !== "IN_PROGRESS" && liveClass.status !== "LIVE")
              }
            >
              {goLiveBusy ? <Loader2 className="size-4 animate-spin" /> : <Video className="size-4" />}
              {goLiveBusy
                ? t("livePrepare.goLiveStarting")
                : liveClass?.status === "IN_PROGRESS" || liveClass?.status === "LIVE"
                  ? t("livePrepare.goLiveEnter")
                  : t("livePrepare.goLive")}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
