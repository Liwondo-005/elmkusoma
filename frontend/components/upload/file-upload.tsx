"use client"

import { useCallback, useEffect, useRef, useState, type DragEvent } from "react"
import { useTranslations } from "next-intl"
import { AlertCircle, FileText, Loader2, RotateCcw, Upload, X } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Progress } from "@/components/ui/progress"
import { isAbortError, type UploadOptions } from "@/lib/upload"

export type FileUploadStatus = "idle" | "selected" | "uploading" | "success" | "error"

export interface FileUploadProps {
  /** Existing endpoint call — receives REAL progress + abort signal (see lib/upload.ts). */
  upload: (file: File, options: UploadOptions) => Promise<unknown>
  /** Called after the backend confirms the upload. */
  onUploaded?: (result: unknown, file: File) => void
  onError?: (message: string) => void
  onStatusChange?: (status: FileUploadStatus) => void
  accept?: string
  /** Client-side UX limit (server remains authoritative). */
  maxSizeMB?: number
  /** Trigger label (idle state). */
  label?: string
  hint?: string
  variant?: "button" | "dropzone"
  /** Upload as soon as a file passes validation (default) — or wait for an explicit click. */
  autoUpload?: boolean
  /** After success, return to idle this many ms later (profile-style contexts). */
  resetSuccessMs?: number
  disabled?: boolean
  className?: string
}

function matchesAccept(file: File, accept?: string): boolean {
  if (!accept?.trim()) return true
  const tokens = accept.split(",").map((s) => s.trim().toLowerCase()).filter(Boolean)
  const name = file.name.toLowerCase()
  const type = (file.type || "").toLowerCase()
  return tokens.some((token) =>
    token.endsWith("/*") ? type.startsWith(token.slice(0, -1)) : token.startsWith(".") ? name.endsWith(token) : token === type,
  )
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function isImage(file: File): boolean {
  return file.type.startsWith("image/")
}

export function FileUpload({
  upload,
  onUploaded,
  onError,
  onStatusChange,
  accept,
  maxSizeMB,
  label,
  hint,
  variant = "button",
  autoUpload = true,
  resetSuccessMs,
  disabled = false,
  className,
}: FileUploadProps) {
  const t = useTranslations("ui.mediaUpload")
  const [status, setStatus] = useState<FileUploadStatus>("idle")
  const [file, setFile] = useState<File | null>(null)
  const [progress, setProgress] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)
  const [dragActive, setDragActive] = useState(false)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)

  const inputRef = useRef<HTMLInputElement>(null)
  const abortRef = useRef<AbortController | null>(null)
  const fileRef = useRef<File | null>(null)
  const resetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const changeStatus = useCallback(
    (next: FileUploadStatus) => {
      setStatus(next)
      onStatusChange?.(next)
    },
    [onStatusChange],
  )

  const clearResetTimer = () => {
    if (resetTimerRef.current) {
      clearTimeout(resetTimerRef.current)
      resetTimerRef.current = null
    }
  }

  useEffect(() => {
    fileRef.current = file
    if (file && isImage(file)) {
      const url = URL.createObjectURL(file)
      setPreviewUrl(url)
      return () => URL.revokeObjectURL(url)
    }
    setPreviewUrl(null)
  }, [file])

  useEffect(
    () => () => {
      clearResetTimer()
      abortRef.current?.abort()
    },
    [],
  )

  const startUpload = useCallback(
    async (f: File) => {
      clearResetTimer()
      const controller = new AbortController()
      abortRef.current = controller
      setError(null)
      setDetailError(null)
      setProgress(null)
      changeStatus("uploading")
      try {
        const result = await upload(f, {
          onProgress: (pct) => setProgress(Math.min(pct, 99)),
          signal: controller.signal,
        })
        setProgress(100)
        changeStatus("success")
        onUploaded?.(result, f)
        if (resetSuccessMs && resetSuccessMs > 0) {
          resetTimerRef.current = setTimeout(() => {
            setFile(null)
            setProgress(null)
            changeStatus("idle")
          }, resetSuccessMs)
        }
      } catch (err) {
        if (isAbortError(err)) {
          setProgress(null)
          changeStatus("selected")
          return
        }
        const message = err instanceof Error && err.message ? err.message : t("uploadFailed")
        setError(t("uploadFailed"))
        setDetailError(message)
        changeStatus("error")
        onError?.(message)
      } finally {
        abortRef.current = null
      }
    },
    [upload, onUploaded, onError, resetSuccessMs, t, changeStatus],
  )

  const acceptFile = useCallback(
    (f: File) => {
      clearResetTimer()
      if (!matchesAccept(f, accept)) {
        setFile(null)
        setError(t("invalidType"))
        setDetailError(null)
        changeStatus("error")
        onError?.(t("invalidType"))
        return
      }
      if (maxSizeMB && f.size > maxSizeMB * 1024 * 1024) {
        setFile(null)
        setError(t("fileTooLarge", { max: maxSizeMB }))
        setDetailError(null)
        changeStatus("error")
        onError?.(t("fileTooLarge", { max: maxSizeMB }))
        return
      }
      setError(null)
      setDetailError(null)
      setFile(f)
      if (autoUpload) {
        void startUpload(f)
      } else {
        setProgress(null)
        changeStatus("selected")
      }
    },
    [accept, maxSizeMB, autoUpload, startUpload, t, onError, changeStatus],
  )

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0]
    e.target.value = ""
    if (f) acceptFile(f)
  }

  const handleDrop = (e: DragEvent) => {
    e.preventDefault()
    setDragActive(false)
    if (disabled || status === "uploading") return
    const f = e.dataTransfer.files?.[0]
    if (f) acceptFile(f)
  }

  const pick = () => {
    if (disabled || status === "uploading") return
    inputRef.current?.click()
  }

  const remove = () => {
    clearResetTimer()
    abortRef.current?.abort()
    setFile(null)
    setProgress(null)
    setError(null)
    setDetailError(null)
    changeStatus("idle")
  }

  const cancelUpload = () => {
    abortRef.current?.abort()
  }

  const busy = status === "uploading"
  const fileMeta = file ? `${file.name} · ${formatSize(file.size)}` : ""

  const statusAnnouncement =
    status === "uploading"
      ? progress !== null
        ? `${t("uploading")} ${progress}%`
        : t("uploading")
      : status === "success"
        ? t("uploaded")
        : status === "error"
          ? error || t("uploadFailed")
          : ""

  const checkIcon = (
    <svg
      viewBox="0 0 52 52"
      className={cn("size-9 shrink-0 text-emerald-600", status === "success" && "upload-success-icon")}
      aria-hidden="true"
    >
      <circle cx="26" cy="26" r="24" fill="none" stroke="currentColor" strokeWidth="3" className="upload-check-circle" />
      <path
        d="M15 27 l8 8 l15 -16"
        fill="none"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="upload-check-mark"
      />
    </svg>
  )

  const preview = file && isImage(file) && previewUrl ? (
    <img src={previewUrl} alt="" className="size-10 shrink-0 rounded-lg border border-border object-cover" />
  ) : file ? (
    <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary" aria-hidden="true">
      <FileText className="size-5" />
    </span>
  ) : null

  const truncateName = (extra: string = "") =>
    file ? (
      <span className={cn("min-w-0 flex-1 truncate text-sm font-medium text-foreground", extra)} title={file.name}>
        {file.name}
      </span>
    ) : null

  /* ─────────── state cards ─────────── */

  const idleButton = (
    <Button
      type="button"
      onClick={pick}
      disabled={disabled}
      aria-label={label || t("button")}
      className={cn(
        "h-10 w-full rounded-xl px-4 text-sm font-semibold shadow-sm shadow-primary/20 transition-all",
        "hover:-translate-y-0.5 hover:shadow-md hover:shadow-primary/30 active:translate-y-0 active:scale-[0.98]",
        "motion-reduce:transform-none motion-reduce:transition-none",
      )}
    >
      <Upload className="size-4" />
      {label || t("button")}
    </Button>
  )

  const idleDropzone = (
    <div
      onDragOver={(e) => {
        e.preventDefault()
        if (!disabled && !busy) setDragActive(true)
      }}
      onDragLeave={() => setDragActive(false)}
      onDrop={handleDrop}
      onClick={pick}
      className={cn(
        "flex cursor-pointer flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-border bg-muted/30 px-6 py-10 text-center transition-all",
        "hover:border-primary/40 hover:bg-primary/5 motion-reduce:transition-none",
        dragActive && "border-primary/70 bg-primary/10 scale-[1.01] motion-reduce:scale-none",
        disabled && "pointer-events-none opacity-60",
      )}
    >
      <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10">
        <Upload className="size-7 text-primary" />
      </div>
      <div>
        <p className="text-base font-medium text-foreground">{hint || t("dropTitle")}</p>
        <p className="mt-1 text-sm text-muted-foreground">{t("dropHint")}</p>
      </div>
      <Button
        type="button"
        tabIndex={-1}
        disabled={disabled}
        onClick={(e) => {
          e.stopPropagation()
          pick()
        }}
        className={cn(
          "pointer-events-auto h-10 rounded-xl px-5 text-sm font-semibold shadow-sm shadow-primary/20 transition-all",
          "hover:-translate-y-0.5 hover:shadow-md active:scale-[0.98] motion-reduce:transform-none",
        )}
      >
        <Upload className="size-4" />
        {label || t("button")}
      </Button>
      <input ref={inputRef} type="file" accept={accept} className="hidden" onChange={handleInputChange} disabled={disabled || busy} />
    </div>
  )

  const stateCard =
    status === "selected" ? (
      <div className="flex w-full items-center gap-3 rounded-xl border border-border bg-card px-3 py-2.5 shadow-sm animate-in fade-in slide-in-from-bottom-1 motion-reduce:animate-none">
        {preview}
        {truncateName("text-xs text-muted-foreground md:text-sm")}
        <span className="shrink-0 text-[11px] tabular-nums text-muted-foreground">{file ? formatSize(file.size) : ""}</span>
        <Button type="button" size="sm" className="h-8 shrink-0 rounded-lg" onClick={() => file && void startUpload(file)}>
          <Upload className="size-3.5" />
          {t("button")}
        </Button>
        <button
          type="button"
          onClick={remove}
          aria-label={t("remove")}
          className="shrink-0 rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50 motion-reduce:transition-none"
        >
          <X className="size-4" />
        </button>
      </div>
    ) : status === "uploading" ? (
      <div className="w-full rounded-xl border border-primary/30 bg-primary/5 px-3 py-2.5 shadow-sm animate-in fade-in slide-in-from-bottom-1 motion-reduce:animate-none">
        <div className="flex items-center gap-3">
          <Loader2 className="size-5 shrink-0 animate-spin text-primary motion-reduce:animate-none" aria-hidden="true" />
          {truncateName()}
          <span className="shrink-0 text-xs font-medium tabular-nums text-primary">
            {t("uploading")}
            {progress !== null ? ` ${progress}%` : ""}
          </span>
          <button
            type="button"
            onClick={cancelUpload}
            className="shrink-0 rounded-md px-1.5 py-0.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50 motion-reduce:transition-none"
          >
            {t("cancel")}
          </button>
        </div>
        {progress !== null ? (
          <Progress
            className="mt-2 h-1.5"
            value={progress}
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={progress}
            aria-label={t("progressLabel")}
          />
        ) : (
          <div
            className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-secondary"
            role="progressbar"
            aria-label={t("progressLabel")}
          >
            <div className="upload-indeterminate h-full w-1/3 rounded-full bg-primary" />
          </div>
        )}
      </div>
    ) : status === "success" ? (
      <div className="flex w-full items-center gap-3 rounded-xl border border-emerald-600/30 bg-emerald-50 px-3 py-2.5 shadow-sm dark:bg-emerald-950/40 animate-in fade-in slide-in-from-bottom-1 motion-reduce:animate-none">
        {checkIcon}
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-emerald-700 dark:text-emerald-400">{t("uploaded")}</p>
          {file && <p className="truncate text-xs text-muted-foreground" title={file.name}>{fileMeta}</p>}
        </div>
        <button
          type="button"
          onClick={remove}
          aria-label={t("remove")}
          className="shrink-0 rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50 motion-reduce:transition-none"
        >
          <X className="size-4" />
        </button>
      </div>
    ) : (
      /* error */
      <div className="w-full rounded-xl border border-destructive/30 bg-destructive/5 px-3 py-2.5 shadow-sm animate-in fade-in slide-in-from-bottom-1 motion-reduce:animate-none">
        <div className="flex items-center gap-3">
          <AlertCircle className="size-5 shrink-0 text-destructive" aria-hidden="true" />
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-destructive">{error || t("uploadFailed")}</p>
            {detailError && <p className="truncate text-xs text-muted-foreground" title={detailError}>{detailError}</p>}
            {file && <p className="truncate text-xs text-muted-foreground" title={file.name}>{fileMeta}</p>}
          </div>
          {file && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="h-8 shrink-0 rounded-lg"
              onClick={() => void startUpload(file)}
            >
              <RotateCcw className="size-3.5" />
              {t("retry")}
            </Button>
          )}
          <button
            type="button"
            onClick={remove}
            aria-label={t("remove")}
            className="shrink-0 rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50 motion-reduce:transition-none"
          >
            <X className="size-4" />
          </button>
        </div>
      </div>
    )

  const showCard = status !== "idle"
  const showDropzoneIdle = variant === "dropzone" && status === "idle"
  const showButtonIdle = variant === "button" && status === "idle"

  return (
    <div className={cn("w-full min-w-0", className)}>
      {/* shared hidden input (dropzone idle renders its own for click-area reasons) */}
      {variant === "button" && (
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          className="hidden"
          onChange={handleInputChange}
          disabled={disabled || busy}
        />
      )}
      {variant === "dropzone" && status !== "idle" && (
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          className="hidden"
          onChange={handleInputChange}
          disabled={disabled || busy}
        />
      )}

      {/* drag surface for button variant / non-idle dropzone */}
      {variant === "button" && showCard && (
        <div
          onDragOver={(e) => {
            e.preventDefault()
            if (!disabled && !busy) setDragActive(true)
          }}
          onDragLeave={() => setDragActive(false)}
          onDrop={handleDrop}
          className={cn(
            dragActive && "rounded-xl outline-2 outline-dashed outline-primary/60",
          )}
        >
          {stateCard}
        </div>
      )}

      {showButtonIdle && idleButton}
      {showDropzoneIdle && idleDropzone}
      {variant === "dropzone" && showCard && stateCard}

      {/* screen-reader status announcements */}
      <span aria-live="polite" aria-atomic="true" className="sr-only">
        {statusAnnouncement}
      </span>
    </div>
  )
}
