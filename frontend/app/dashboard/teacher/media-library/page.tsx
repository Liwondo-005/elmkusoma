"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"

import { Video, Search, Film, FileText, Music, Image, Loader2, AlertCircle, Play, Trash2, Clock, HardDrive, Eye, EyeOff, Tag, Calendar } from "lucide-react"
import { appFetch } from "@/lib/fetch"

interface MediaAsset {
  id: string
  title: string
  description: string | null
  mediaType: string
  fileUrl: string | null
  thumbnailUrl: string | null
  durationSeconds: number | null
  fileSizeBytes: number | null
  mimeType: string | null
  status: string
  visibility: string
  sourceType: string | null
  sourceId: string | null
  teacherId: string | null
  courseId: string | null
  subjectId: string | null
  tags: string | null
  createdAt: string
}

function formatDuration(seconds: number | null): string {
  if (!seconds) return "--:--"
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
  return `${m}:${String(s).padStart(2, "0")}`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  })
}

export default function TeacherMediaLibraryPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const [media, setMedia] = useState<MediaAsset[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [typeFilter, setTypeFilter] = useState<string>("")
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const mediaTypeConfig: Record<string, { icon: typeof Video; label: string; color: string }> = {
    VIDEO: { icon: Video, label: t("mediaLibrary.typeVideo"), color: "text-red-500" },
    DOCUMENT: { icon: FileText, label: t("mediaLibrary.typeDocument"), color: "text-blue-500" },
    AUDIO: { icon: Music, label: t("mediaLibrary.typeAudio"), color: "text-purple-500" },
    IMAGE: { icon: Image, label: t("mediaLibrary.typeImage"), color: "text-green-500" },
    RECORDING: { icon: Film, label: t("mediaLibrary.typeRecording"), color: "text-orange-500" },
  }

  function formatFileSize(bytes: number | null): string {
    if (!bytes) return t("mediaLibrary.unknownSize")
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
  }

  const fetchMedia = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await appFetch<MediaAsset[]>("/api/v1/media/my")
      setMedia(data || [])
    } catch (err: any) {
      setError(err.message || t("mediaLibrary.loadError"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMedia()
  }, [])

  const handleDelete = async (id: string) => {
    if (!confirm(t("mediaLibrary.deleteConfirm"))) return
    try {
      setDeletingId(id)
      await appFetch(`/api/v1/media/${id}`, { method: "DELETE" })
      setMedia((prev) => prev.filter((m) => m.id !== id))
    } catch (err: any) {
      alert(err.message || t("mediaLibrary.deleteError"))
    } finally {
      setDeletingId(null)
    }
  }

  const filtered = media.filter((item) => {
    const matchesSearch =
      !searchQuery ||
      item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.tags?.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesType = !typeFilter || item.mediaType === typeFilter
    return matchesSearch && matchesType
  })

  const totalSize = media.reduce((sum, m) => sum + (m.fileSizeBytes || 0), 0)
  const recordingCount = media.filter((m) => m.mediaType === "RECORDING").length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{tn("mediaLibrary")}</h1>
          <p className="text-muted-foreground mt-1">
            {t("mediaLibrary.subtitle")}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="rounded-lg border p-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Video className="h-4 w-4" />
            {t("mediaLibrary.totalAssets")}
          </div>
          <p className="text-2xl font-bold mt-1">{media.length}</p>
        </div>
        <div className="rounded-lg border p-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Film className="h-4 w-4" />
            {t("mediaLibrary.recordings")}
          </div>
          <p className="text-2xl font-bold mt-1">{recordingCount}</p>
        </div>
        <div className="rounded-lg border p-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <HardDrive className="h-4 w-4" />
            {t("mediaLibrary.totalSize")}
          </div>
          <p className="text-2xl font-bold mt-1">{formatFileSize(totalSize)}</p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("mediaLibrary.searchPlaceholder")}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring pl-9"
          />
        </div>
        <div className="flex gap-2">
          {["", "VIDEO", "DOCUMENT", "AUDIO", "IMAGE", "RECORDING"].map((type) => (
            <Button
              key={type}
              variant={typeFilter === type ? "default" : "outline"}
              size="sm"
              onClick={() => setTypeFilter(type)}
            >
              {type || t("mediaLibrary.allFilter")}
            </Button>
          ))}
        </div>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-muted-foreground">{t("mediaLibrary.loadingMedia")}</span>
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-destructive/10 text-destructive">
          <AlertCircle className="h-5 w-5" />
          {error}
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <div className="text-center py-12">
          <Video className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
          <h3 className="text-lg font-medium">{t("mediaLibrary.emptyTitle")}</h3>
          <p className="text-muted-foreground mt-1">
            {searchQuery ? t("mediaLibrary.emptySearch") : t("mediaLibrary.emptyDesc")}
          </p>
        </div>
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((item) => {
            const typeInfo = mediaTypeConfig[item.mediaType] || mediaTypeConfig.VIDEO
            const Icon = typeInfo.icon
            return (
              <div
                key={item.id}
                className="rounded-lg border p-4 hover:shadow-md transition-shadow"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <Icon className={`h-5 w-5 ${typeInfo.color}`} />
                    <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-muted">
                      {typeInfo.label}
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    {item.visibility === "PRIVATE" ? (
                      <EyeOff className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Eye className="h-4 w-4 text-muted-foreground" />
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-7 w-7 p-0 text-destructive hover:text-destructive"
                      onClick={() => handleDelete(item.id)}
                      disabled={deletingId === item.id}
                    >
                      {deletingId === item.id ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <Trash2 className="h-3.5 w-3.5" />
                      )}
                    </Button>
                  </div>
                </div>

                <h3 className="font-medium mt-2 line-clamp-1">{item.title}</h3>
                {item.description && (
                  <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
                    {item.description}
                  </p>
                )}

                <div className="flex flex-wrap gap-2 mt-3 text-xs text-muted-foreground">
                  {item.durationSeconds != null && (
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {formatDuration(item.durationSeconds)}
                    </span>
                  )}
                  {item.fileSizeBytes != null && (
                    <span className="flex items-center gap-1">
                      <HardDrive className="h-3 w-3" />
                      {formatFileSize(item.fileSizeBytes)}
                    </span>
                  )}
                  <span className="flex items-center gap-1">
                    <Calendar className="h-3 w-3" />
                    {formatDate(item.createdAt)}
                  </span>
                </div>

                {item.tags && (
                  <div className="flex items-center gap-1 mt-2">
                    <Tag className="h-3 w-3 text-muted-foreground" />
                    <span className="text-xs text-muted-foreground line-clamp-1">{item.tags}</span>
                  </div>
                )}

                {item.fileUrl && (
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full mt-3"
                    onClick={() => window.open(item.fileUrl!, "_blank")}
                  >
                    <Play className="h-4 w-4 mr-1" />
                    {t("mediaLibrary.open")}
                  </Button>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
