"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import { Progress } from "@/components/ui/progress"
import {
  Video,
  Film,
  Plus,
  X,
  Loader2,
  AlertCircle,
  Clock,
  Link2,
  VideoOff,
  Upload,
  CheckCircle,
  FileVideo,
  XCircle,
} from "lucide-react"
import {
  teacherFetch,
  getPresignedUploadUrl,
  uploadFileToPresignedUrl,
  createVideoTutorial,
  type PresignedUploadResponse,
} from "@/lib/teacher-api"

interface VideoTutorialItem {
  id: string
  title: string
  description: string | null
  recordingUrl: string | null
  status: string
  visibility: string
  durationSeconds: number | null
  createdAt: string
}

interface MediaAssetItem {
  id: string
  title: string
  mediaType: string
  fileUrl: string | null
  durationSeconds: number | null
}

const VIDEO_MEDIA_TYPES = ["VIDEO", "RECORDING"]

const ALLOWED_VIDEO_TYPES = ["video/mp4", "video/webm", "video/quicktime", "video/x-matroska"]
const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024 // 2GB

type UploadStatus = "pending" | "uploading" | "processing" | "ready" | "failed"

interface UploadState {
  file: File | null
  status: UploadStatus
  progress: number
  error: string | null
  videoTutorialId: string | null
  uploadUrl: string | null
}

function formatDuration(seconds: number | null): string | null {
  if (!seconds || seconds <= 0) return null
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, "0")}`
}

function formatFileSize(bytes: number): string {
  if (bytes >= 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
  }
  return `${(bytes / 1024).toFixed(2)} KB`
}

export default function TeacherVideoLibraryPage() {
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const tu = useTranslations("teacher.videoLibrary.upload")

  const [videos, setVideos] = useState<VideoTutorialItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [showAttach, setShowAttach] = useState(false)
  const [media, setMedia] = useState<MediaAssetItem[]>([])
  const [mediaLoading, setMediaLoading] = useState(false)
  const [mediaError, setMediaError] = useState<string | null>(null)
  const [attachingId, setAttachingId] = useState<string | null>(null)
  const [attachError, setAttachError] = useState<string | null>(null)

  const [showUpload, setShowUpload] = useState(false)
  const [upload, setUpload] = useState<UploadState>({
    file: null,
    status: "pending",
    progress: 0,
    error: null,
    videoTutorialId: null,
    uploadUrl: null,
  })
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [visibility, setVisibility] = useState("draft")

  async function loadVideos() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherFetch<VideoTutorialItem[]>("/v1/video-tutorials")
      setVideos(data || [])
    } catch (err: any) {
      setError(err.message || t("videoLibrary.loadError"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadVideos()
  }, [])

  async function openAttachDialog() {
    setShowAttach(true)
    setAttachError(null)
    setMediaError(null)
    if (media.length > 0) return
    try {
      setMediaLoading(true)
      const data = await teacherFetch<MediaAssetItem[]>("/v1/media")
      setMedia((data || []).filter((m) => VIDEO_MEDIA_TYPES.includes(m.mediaType)))
    } catch (err: any) {
      setMediaError(err.message || t("videoLibrary.mediaLoadError"))
    } finally {
      setMediaLoading(false)
    }
  }

  async function handleAttach(assetId: string) {
    try {
      setAttachingId(assetId)
      setAttachError(null)
      await teacherFetch("/v1/video-tutorials/attach-recording", {
        method: "POST",
        body: JSON.stringify({ mediaAssetId: assetId }),
      })
      setShowAttach(false)
      await loadVideos()
    } catch (err: any) {
      setAttachError(err.message || t("videoLibrary.attachError"))
    } finally {
      setAttachingId(null)
    }
  }

  function openUploadDialog() {
    setShowUpload(true)
    resetUploadState()
  }

  function resetUploadState() {
    setUpload({
      file: null,
      status: "pending",
      progress: 0,
      error: null,
      videoTutorialId: null,
      uploadUrl: null,
    })
    setTitle("")
    setDescription("")
    setVisibility("draft")
  }

  function handleFileSelect(file: File) {
    if (!ALLOWED_VIDEO_TYPES.includes(file.type)) {
      setUpload((prev) => ({
        ...prev,
        error: tu("invalidType"),
        status: "failed",
      }))
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      setUpload((prev) => ({
        ...prev,
        error: tu("fileTooLarge"),
        status: "failed",
      }))
      return
    }
    setUpload((prev) => ({
      ...prev,
      file,
      status: "pending",
      progress: 0,
      error: null,
    }))
  }

  function handleDragOver(e: React.DragEvent) {
    e.preventDefault()
    e.stopPropagation()
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    e.stopPropagation()
    const file = e.dataTransfer.files[0]
    if (file) {
      handleFileSelect(file)
    }
  }

  function handleFileInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) {
      handleFileSelect(file)
    }
  }

  async function handleUpload() {
    const file = upload.file
    if (!file) return

    try {
      // Step 1: Get presigned URL
      setUpload((prev) => ({ ...prev, status: "uploading", progress: 0, error: null }))
      const presigned = await getPresignedUploadUrl(file.name, file.type)
      setUpload((prev) => ({ ...prev, videoTutorialId: presigned.videoTutorialId, uploadUrl: presigned.uploadUrl }))

      // Step 2: Upload file to presigned URL
      await uploadFileToPresignedUrl(presigned.uploadUrl, file, (progress) => {
        setUpload((prev) => ({ ...prev, progress }))
      })

      // Step 3: Create video tutorial
      setUpload((prev) => ({ ...prev, status: "processing", progress: 100 }))
      await createVideoTutorial({
        title: title || file.name,
        description: description || undefined,
        visibility,
      })

      setUpload((prev) => ({ ...prev, status: "ready", progress: 100 }))
      
      // Refresh video list
      await loadVideos()
      
      // Close dialog after a short delay to show success
      setTimeout(() => {
        setShowUpload(false)
        resetUploadState()
      }, 1500)
    } catch (err: any) {
      setUpload((prev) => ({
        ...prev,
        status: "failed",
        error: err.message || tu("error"),
      }))
    }
  }

  function handleRetry() {
    resetUploadState()
  }

  function handleCancel() {
    setShowUpload(false)
    resetUploadState()
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("videoLibrary")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("videoLibrary.subtitle")}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button onClick={openUploadDialog} className="gap-2">
            <Upload className="size-4" /> {tu("title")}
          </Button>
          <Button onClick={openAttachDialog} className="gap-2">
            <Plus className="size-4" /> {t("videoLibrary.attachRecording")}
          </Button>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      {showAttach && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="dialog" aria-modal="true" aria-label={t("videoLibrary.attachDialogTitle")}>
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-5 shadow-lg">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-sm font-semibold text-foreground">{t("videoLibrary.attachDialogTitle")}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{t("videoLibrary.attachDialogDesc")}</p>
              </div>
              <button
                onClick={() => setShowAttach(false)}
                className="text-muted-foreground hover:text-foreground"
                aria-label={tc("close")}
              >
                <X className="size-4" />
              </button>
            </div>

            {attachError && (
              <div className="mt-3 flex items-center gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                <AlertCircle className="size-3.5 shrink-0" />
                {attachError}
              </div>
            )}

            <div className="mt-4 max-h-80 space-y-2 overflow-y-auto">
              {mediaLoading && (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-5 animate-spin text-muted-foreground" />
                </div>
              )}

              {!mediaLoading && mediaError && (
                <div className="flex items-center gap-2 rounded-lg bg-destructive/5 px-3 py-2 text-xs text-destructive">
                  <AlertCircle className="size-3.5 shrink-0" />
                  {mediaError}
                </div>
              )}

              {!mediaLoading && !mediaError && media.length === 0 && (
                <div className="rounded-lg border border-dashed border-border py-8 text-center">
                  <VideoOff className="mx-auto size-6 text-muted-foreground" />
                  <p className="mt-2 text-xs font-medium text-foreground">{t("videoLibrary.noRecordingsTitle")}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{t("videoLibrary.noRecordingsDesc")}</p>
                </div>
              )}

              {!mediaLoading && !mediaError &&
                media.map((asset) => (
                  <div
                    key={asset.id}
                    className="flex items-center justify-between gap-3 rounded-lg border border-border px-3 py-2.5"
                  >
                    <div className="flex min-w-0 items-center gap-2">
                      <Film className="size-4 shrink-0 text-orange-500" />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-foreground">{asset.title}</p>
                        <p className="text-[11px] text-muted-foreground">{asset.mediaType}</p>
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleAttach(asset.id)}
                      disabled={attachingId === asset.id}
                      className="shrink-0 gap-1.5"
                    >
                      {attachingId === asset.id ? (
                        <Loader2 className="size-3.5 animate-spin" />
                      ) : (
                        <Link2 className="size-3.5" />
                      )}
                      {t("videoLibrary.attach")}
                    </Button>
                  </div>
                ))}
            </div>

            <div className="mt-4 flex justify-end">
              <Button variant="outline" size="sm" onClick={() => setShowAttach(false)}>
                {tc("cancel")}
              </Button>
            </div>
          </div>
        </div>
      )}

      {showUpload && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="dialog" aria-modal="true" aria-label={tu("title")}>
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-5 shadow-lg">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-sm font-semibold text-foreground">{tu("title")}</h3>
                <p className="mt-1 text-xs text-muted-foreground">{tu("description")}</p>
              </div>
              <button
                onClick={handleCancel}
                className="text-muted-foreground hover:text-foreground"
                aria-label={tc("close")}
                disabled={upload.status === "uploading" || upload.status === "processing"}
              >
                <X className="size-4" />
              </button>
            </div>

            {upload.error && (
              <div className="mt-3 flex items-center gap-2 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                <AlertCircle className="size-3.5 shrink-0" />
                {upload.error}
              </div>
            )}

            <div className="mt-4 space-y-4">
              {/* File Dropzone / Selected File */}
              {upload.file ? (
                <div className="rounded-lg border border-green-500/30 bg-green-500/5 p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <FileVideo className="size-6 text-green-500 shrink-0" />
                      <div>
                        <p className="text-sm font-medium text-foreground">{upload.file.name}</p>
                        <p className="text-xs text-muted-foreground">{formatFileSize(upload.file.size)}</p>
                      </div>
                    </div>
                    <button
                      onClick={resetUploadState}
                      className="text-xs text-muted-foreground hover:text-foreground"
                    >
                      {tu("cancel")}
                    </button>
                  </div>
                </div>
              ) : (
                <div
                  className="rounded-lg border-2 border-dashed border-border hover:border-primary/50 transition-colors p-8 text-center"
                  onDragOver={handleDragOver}
                  onDrop={handleDrop}
                >
                  <input
                    type="file"
                    id="video-upload"
                    accept="video/mp4,video/webm,video/quicktime,video/x-matroska"
                    onChange={handleFileInputChange}
                    className="hidden"
                    disabled={upload.status === "uploading" || upload.status === "processing"}
                  />
                  <label htmlFor="video-upload" className="cursor-pointer">
                    <Upload className="mx-auto size-10 text-muted-foreground" />
                    <p className="mt-3 text-sm font-medium text-foreground">{tu("dropzoneText")}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{tu("dropzoneHint")}</p>
                    <Button type="button" variant="outline" className="mt-4" onClick={(e) => e.stopPropagation()}>
                      {tu("selectFile")}
                    </Button>
                  </label>
                </div>
              )}

              {/* Upload Progress */}
              {upload.status === "uploading" && upload.file && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">{tu("progress")}</span>
                    <span className="font-medium">{upload.progress}%</span>
                  </div>
                  <Progress value={upload.progress} max={100} className="h-2" />
                  <p className="text-xs text-muted-foreground">{tu("uploading")}</p>
                </div>
              )}

              {upload.status === "processing" && (
                <div className="space-y-2">
                  <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
                    <Loader2 className="size-4 animate-spin" />
                    <span>{tu("processing")}</span>
                  </div>
                  <Progress value={100} max={100} className="h-2" />
                </div>
              )}

              {upload.status === "ready" && (
                <div className="flex items-center justify-center gap-2 text-green-600">
                  <CheckCircle className="size-5" />
                  <span className="text-sm font-medium">{tu("success")}</span>
                </div>
              )}

              {upload.status === "failed" && upload.error && (
                <div className="flex items-center justify-center gap-2 text-destructive">
                  <XCircle className="size-5" />
                  <span className="text-sm font-medium">{upload.error}</span>
                </div>
              )}

              {/* Metadata Form - shown when file is selected and not uploading/processing/ready */}
              {upload.file && upload.status === "pending" && (
                <div className="space-y-4 border-t border-border pt-4">
                  <div>
                    <label className="block text-xs font-medium text-foreground mb-1">{tu("titleLabel")}</label>
                    <input
                      type="text"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder={tu("titlePlaceholder")}
                      className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-foreground mb-1">{tu("descriptionLabel")}</label>
                    <textarea
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder={tu("descriptionPlaceholder")}
                      rows={3}
                      className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-foreground mb-1">{tu("visibilityLabel")}</label>
                    <select
                      value={visibility}
                      onChange={(e) => setVisibility(e.target.value)}
                      className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    >
                      <option value="draft">{tu("visibilityDraft")}</option>
                      <option value="private">{tu("visibilityPrivate")}</option>
                      <option value="classOnly">{tu("visibilityClassOnly")}</option>
                      <option value="courseOnly">{tu("visibilityCourseOnly")}</option>
                      <option value="institution">{tu("visibilityInstitution")}</option>
                      <option value="school">{tu("visibilitySchool")}</option>
                      <option value="public">{tu("visibilityPublic")}</option>
                    </select>
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex justify-end gap-2 border-t border-border pt-4">
                {upload.status === "pending" && upload.file && (
                  <>
                    <Button variant="outline" onClick={handleCancel}>
                      {tc("cancel")}
                    </Button>
                    <Button onClick={handleUpload} disabled={!title.trim()}>
                      {tu("upload")}
                    </Button>
                  </>
                )}
                {upload.status === "failed" && (
                  <>
                    <Button variant="outline" onClick={handleRetry}>
                      {tu("retry")}
                    </Button>
                    <Button variant="outline" onClick={handleCancel}>
                      {tu("close")}
                    </Button>
                  </>
                )}
                {upload.status === "ready" && (
                  <Button onClick={handleCancel}>
                    {tu("close")}
                  </Button>
                )}
                {(upload.status === "uploading" || upload.status === "processing") && (
                  <Button variant="outline" disabled>
                    {upload.status === "uploading" ? tu("uploading") : tu("processing")}
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && !error && videos.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <Video className="mx-auto mb-3 size-8 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">{t("videoLibrary.emptyTitle")}</h3>
          <p className="mt-1 text-xs text-muted-foreground">{t("videoLibrary.emptyDesc")}</p>
          <Button onClick={openAttachDialog} className="mt-4 gap-2" size="sm">
            <Plus className="size-3" /> {t("videoLibrary.attachRecording")}
          </Button>
        </div>
      )}

      {!loading && videos.length > 0 && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {videos.map((video) => {
            const duration = formatDuration(video.durationSeconds)
            return (
              <div key={video.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-2">
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                      <Video className="size-4 text-primary" />
                    </div>
                    <div className="min-w-0">
                      <h4 className="truncate text-sm font-semibold text-foreground">{video.title}</h4>
                      <span className="text-[11px] text-muted-foreground">{video.status} · {video.visibility}</span>
                    </div>
                  </div>
                </div>
                {video.description && (
                  <p className="mt-2 line-clamp-2 text-xs text-muted-foreground">{video.description}</p>
                )}
                <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                  {duration && (
                    <span className="flex items-center gap-1">
                      <Clock className="size-3" /> {duration}
                    </span>
                  )}
                  <span className="flex items-center gap-1">
                    <Link2 className="size-3" />
                    {video.recordingUrl ? t("videoLibrary.linked") : t("videoLibrary.notLinked")}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
