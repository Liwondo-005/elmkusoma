"use client"

import { useState, useRef } from "react"
import { useTranslations } from "next-intl"
import { Upload, Loader2, FileText, Image, Video } from "lucide-react"

interface MediaUploadProps {
  onUploadComplete?: (media: any) => void
  institutionId?: string
}

export function MediaUpload({ onUploadComplete, institutionId }: MediaUploadProps) {
  const t = useTranslations("ui")
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dragActive, setDragActive] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  async function handleFile(file: File) {
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const instId = institutionId || localStorage.getItem("elmkusoma_institution_id") || "a0000000-0000-0000-0000-000000000001"

      const formData = new FormData()
      formData.append("file", file)

      const res = await fetch("/api/v1/media/upload", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "X-Institution-Id": instId,
        },
        body: formData,
      })

      if (!res.ok) throw new Error(t("mediaUpload.uploadFailed"))
      const data = await res.json()
      onUploadComplete?.(data.data)
    } catch (err) {
      setError(err instanceof Error ? err.message : t("mediaUpload.uploadFailed"))
    } finally {
      setUploading(false)
    }
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragActive(false)
    if (e.dataTransfer.files?.[0]) handleFile(e.dataTransfer.files[0])
  }

  function getFileIcon(type: string) {
    if (type.startsWith("video/")) return <Video className="size-8 text-blue-500" />
    if (type.startsWith("image/")) return <Image className="size-8 text-green-500" />
    return <FileText className="size-8 text-orange-500" />
  }

  return (
    <div className="space-y-3">
      <div
        onDragOver={(e) => { e.preventDefault(); setDragActive(true) }}
        onDragLeave={() => setDragActive(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-8 cursor-pointer transition-colors ${
          dragActive ? "border-primary bg-primary/5" : "border-border hover:border-primary/50"
        }`}
      >
        {uploading ? (
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        ) : (
          <>
            <Upload className="size-8 text-muted-foreground" />
            <p className="mt-2 text-sm font-medium text-foreground">{t("mediaUpload.dropTitle")}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t("mediaUpload.dropHint")}</p>
          </>
        )}
      </div>
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        accept="*/*"
        onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])}
      />
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  )
}
