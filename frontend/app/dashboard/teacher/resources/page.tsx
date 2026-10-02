"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import {
  AlertCircle,
  Check,
  ChevronDown,
  FileText,
  Link2,
  Loader2,
  Music,
  Image as ImageIcon,
  Pencil,
  Plus,
  Search,
  Share2,
  Trash2,
  Upload,
  Video,
  X,
  GripVertical,
} from "lucide-react"
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from "@dnd-kit/core"
import {
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
  useSortable,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { teacherFetch, teacherApi, saveResourceWithFile, type TeacherAssignment } from "@/lib/teacher-api"

interface ResourceItem {
  id: string
  title: string
  description?: string | null
  resourceType: string
  visibility: string
  mimeType?: string | null
  fileSize?: number | null
  storageUrl?: string | null
  externalUrl?: string | null
  lessonId?: string | null
  teacherAssignmentId?: string | null
  sortOrder?: number | null
  isDownloadable?: boolean | null
  isPreviewable?: boolean | null
  processingStatus?: string | null
  processingError?: string | null
  pageCount?: number | null
  width?: number | null
  height?: number | null
  durationSeconds?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

const RESOURCE_TYPES = ["DOCUMENT", "VIDEO", "IMAGE", "AUDIO", "LINK", "EXTERNAL_LINK"]
const VISIBILITIES = ["PUBLIC", "INSTITUTION", "CLASS_ONLY", "COURSE_ONLY", "SCHOOL", "PRIVATE", "DRAFT"]
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const initialForm = {
  title: "",
  description: "",
  resourceType: "DOCUMENT",
  visibility: "DRAFT",
  externalUrl: "",
  lessonId: "",
  teacherAssignmentId: "",
  isDownloadable: true,
  isPreviewable: false,
}

/** File pickers restricted to what each type actually accepts. */
const FILE_ACCEPT: Record<string, string> = {
  VIDEO: "video/*",
  IMAGE: "image/*",
  AUDIO: "audio/*",
  DOCUMENT: "application/pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.odt",
}

function isLinkType(resourceType: string) {
  return resourceType === "LINK" || resourceType === "EXTERNAL_LINK"
}

function isValidHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return url.protocol === "http:" || url.protocol === "https:"
  } catch {
    return false
  }
}

const visibilityClass: Record<string, string> = {
  PUBLIC: "bg-green-100 text-green-700",
  INSTITUTION: "bg-blue-100 text-blue-700",
  CLASS_ONLY: "bg-cyan-100 text-cyan-700",
  COURSE_ONLY: "bg-indigo-100 text-indigo-700",
  SCHOOL: "bg-purple-100 text-purple-700",
  PRIVATE: "bg-orange-100 text-orange-700",
  DRAFT: "bg-gray-100 text-gray-600",
}

const typeClass: Record<string, string> = {
  DOCUMENT: "bg-blue-500/10 text-blue-600",
  VIDEO: "bg-red-500/10 text-red-600",
  AUDIO: "bg-purple-500/10 text-purple-600",
  IMAGE: "bg-orange-500/10 text-orange-600",
  LINK: "bg-teal-500/10 text-teal-600",
  EXTERNAL_LINK: "bg-teal-500/10 text-teal-600",
}

function normalizeList<T>(raw: unknown): T[] {
  if (Array.isArray(raw)) return raw as T[]
  if (raw && typeof raw === "object") {
    const obj = raw as Record<string, unknown>
    if (Array.isArray(obj.content)) return obj.content as T[]
    if (Array.isArray(obj.data)) return obj.data as T[]
    const nested = obj.data
    if (Array.isArray(nested)) return nested as T[]
    if (nested && typeof nested === "object") {
      const inner = nested as Record<string, unknown>
      if (Array.isArray(inner.content)) return inner.content as T[]
      if (Array.isArray(inner.data)) return inner.data as T[]
      if (Array.isArray(inner.records)) return inner.records as T[]
      if (Array.isArray(inner.items)) return inner.items as T[]
    }
  }
  return []
}

function TypeIcon({ type }: { type?: string }) {
  switch ((type || "").toUpperCase()) {
    case "VIDEO":
      return <Video className="size-4 text-red-500" />
    case "AUDIO":
      return <Music className="size-4 text-purple-500" />
    case "IMAGE":
      return <ImageIcon className="size-4 text-orange-500" />
    case "LINK":
    case "EXTERNAL_LINK":
      return <Link2 className="size-4 text-teal-600" />
    default:
      return <FileText className="size-4 text-blue-600" />
  }
}

function formatDate(iso?: string | null) {
  if (!iso) return "—"
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return "—"
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

interface SortableResourceRowProps {
  resource: ResourceItem
  index: number
  onEdit: () => void
  onDelete: () => void
  onShare: () => void
  copied: boolean
  deleting: boolean
  /** Resolved teaching assignment this resource targets, if any (V117). */
  assignmentTarget?: TeacherAssignment | null
  t: ReturnType<typeof useTranslations>
  tc: ReturnType<typeof useTranslations>
}

function SortableResourceRow({ resource, index, onEdit, onDelete, onShare, copied, deleting, assignmentTarget, t, tc }: SortableResourceRowProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: resource.id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="rounded-2xl border border-border bg-card p-4 shadow-xs"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <button
            {...attributes}
            {...listeners}
            className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-muted-foreground hover:text-foreground cursor-grab active:cursor-grabbing"
            aria-label={t("resources.dragHandle")}
            title={t("resources.dragHandle")}
          >
            <GripVertical className="size-5" />
          </button>
          <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
            <TypeIcon type={resource.resourceType} />
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="truncate text-sm font-semibold text-foreground">{resource.title}</h4>
              <span
                className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${typeClass[(resource.resourceType || "").toUpperCase()] || "bg-muted text-muted-foreground"}`}
              >
                {typeLabel(resource.resourceType, t)}
              </span>
              <span
                className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${visibilityClass[(resource.visibility || "").toUpperCase()] || "bg-gray-100 text-gray-600"}`}
              >
                {visibilityLabel(resource.visibility, t)}
              </span>
              {resource.processingStatus && resource.processingStatus !== "READY" && (
                <span
                  className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${
                    resource.processingStatus === "FAILED"
                      ? "bg-red-100 text-red-700"
                      : "bg-amber-100 text-amber-700"
                  }`}
                >
                  {resource.processingStatus === "FAILED"
                    ? t("resources.statusFailed")
                    : t("resources.statusProcessing")}
                </span>
              )}
            </div>
            {resource.description && (
              <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{resource.description}</p>
            )}
            <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
              <span>
                {t("resources.colUpdated")}: {formatDate(resource.updatedAt || resource.createdAt)}
              </span>
              {assignmentTarget && (
                <span>
                  {assignmentTarget.classGroupName || assignmentTarget.classGroupId} ·{" "}
                  {assignmentTarget.subjectName || assignmentTarget.subjectId}
                  {assignmentTarget.status === "ENDED"
                    ? ` (${t("resources.assignmentEnded")})`
                    : ""}
                </span>
              )}
              {resource.mimeType && <span>{resource.mimeType}</span>}
              {resource.pageCount != null && (
                <span>
                  {resource.pageCount}{" "}
                  {resource.pageCount === 1 ? t("resources.page") : t("resources.pages")}
                </span>
              )}
              {resource.durationSeconds != null && (
                <span>{t("resources.duration", { seconds: resource.durationSeconds })}</span>
              )}
              {resource.width != null && resource.height != null && (
                <span>
                  {resource.width}×{resource.height}
                </span>
              )}
              {resource.externalUrl && <span className="truncate">{resource.externalUrl}</span>}
            </div>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button
            onClick={onShare}
            className={`rounded-lg p-1.5 hover:bg-primary/10 ${copied ? "text-teal-600" : "text-muted-foreground hover:text-primary"}`}
            title={copied ? t("resources.linkCopied") : t("resources.shareLink")}
            aria-label={copied ? t("resources.linkCopied") : t("resources.shareLink")}
          >
            {copied ? <Check className="size-4" /> : <Share2 className="size-4" />}
          </button>
          <button
            onClick={onEdit}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
            title={tc("edit")}
            aria-label={tc("edit")}
          >
            <Pencil className="size-4" />
          </button>
          <button
            onClick={onDelete}
            disabled={deleting}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
            title={tc("delete")}
            aria-label={tc("delete")}
          >
            {deleting ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
          </button>
        </div>
      </div>
    </div>
  )
}

function typeLabel(resourceType: string, t: ReturnType<typeof useTranslations>) {
  const map: Record<string, string> = {
    DOCUMENT: "resources.typeDocument",
    VIDEO: "resources.typeVideo",
    IMAGE: "resources.typeImage",
    AUDIO: "resources.typeAudio",
    LINK: "resources.typeLink",
    EXTERNAL_LINK: "resources.typeExternalLink",
  }
  const key = map[(resourceType || "").toUpperCase()]
  return key ? t(key) : resourceType
}

function visibilityLabel(visibility: string, t: ReturnType<typeof useTranslations>) {
  const map: Record<string, string> = {
    PUBLIC: "visibility.public",
    INSTITUTION: "visibility.institution",
    CLASS_ONLY: "visibility.classOnly",
    COURSE_ONLY: "visibility.courseOnly",
    SCHOOL: "visibility.school",
    PRIVATE: "visibility.private",
    DRAFT: "visibility.draft",
  }
  const key = map[(visibility || "").toUpperCase()]
  return key ? t(key) : visibility
}

export default function TeacherResourcesPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")

  const [resources, setResources] = useState<ResourceItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState("")
  const [reordering, setReordering] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [uploadProgress, setUploadProgress] = useState<number | null>(null)
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [editingHasFile, setEditingHasFile] = useState(false)
  const [assignments, setAssignments] = useState<TeacherAssignment[]>([])

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )

  useEffect(() => {
    if (!user) return
    loadResources(true)
  }, [user])

  // Teacher's teaching assignments — the authoritative targets for class-only
  // resources. Resolved server-side via /me/profile → /{id}/assignments
  // (self-ownership enforced by the backend).
  useEffect(() => {
    if (!user) return
    let cancelled = false
    ;(async () => {
      try {
        const profile = await teacherApi.getMeProfile()
        if (cancelled) return
        const list = await teacherApi.getAssignments(profile.id)
        if (!cancelled) setAssignments(Array.isArray(list) ? list : [])
      } catch {
        if (!cancelled) setAssignments([])
      }
    })()
    return () => {
      cancelled = true
    }
  }, [user])

  async function loadResources(initial = false) {
    if (initial) {
      setLoading(true)
      setError(null)
    }
    try {
      const raw = await teacherFetch<unknown>("/v1/resources")
      setResources(normalizeList<ResourceItem>(raw))
    } catch {
      setResources([])
      setError(t("resources.loadError"))
    } finally {
      if (initial) setLoading(false)
    }
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
    setFile(null)
    setUploadProgress(null)
    setEditingHasFile(false)
  }

  function openCreate() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(true)
    setFile(null)
    setEditingHasFile(false)
  }

  function openEdit(resource: ResourceItem) {
    setForm({
      title: resource.title || "",
      description: resource.description || "",
      resourceType: resource.resourceType || "DOCUMENT",
      visibility: resource.visibility || "DRAFT",
      externalUrl: resource.externalUrl || "",
      lessonId: resource.lessonId || "",
      teacherAssignmentId: resource.teacherAssignmentId || "",
      isDownloadable: resource.isDownloadable !== false,
      isPreviewable: resource.isPreviewable === true,
    })
    setEditingId(resource.id)
    setEditingHasFile(!!resource.storageUrl)
    setFile(null)
    setShowForm(true)
  }

  /**
   * Type switch cleans up state that no longer applies: crossing between a
   * file-based type and a link type drops the other side's value so nothing
   * stale can be submitted.
   */
  function handleTypeChange(nextType: string) {
    const willBeLink = isLinkType(nextType)
    setForm((prev) => ({
      ...prev,
      resourceType: nextType,
      externalUrl: willBeLink ? prev.externalUrl : "",
    }))
    setFile(null)
  }

  async function handleShare(resource: ResourceItem) {
    // Share/Copy Link always points at the protected learner route — never at
    // a raw storage URL.
    const url = `${window.location.origin}/dashboard/learner/resources/${resource.id}`
    try {
      await navigator.clipboard.writeText(url)
      setCopiedId(resource.id)
      setTimeout(() => setCopiedId((prev) => (prev === resource.id ? null : prev)), 2000)
    } catch {
      setError(t("resources.shareCopyError"))
    }
  }

  async function handleSave() {
    if (!form.title.trim()) return
    const lessonId = form.lessonId.trim()
    if (lessonId && !UUID_RE.test(lessonId)) {
      setError(t("resources.invalidLessonId"))
      return
    }

    const linkType = isLinkType(form.resourceType)
    const externalUrl = form.externalUrl.trim()

    // A new class-only resource needs an authoritative target: one of the
    // teacher's ACTIVE assignments or a lesson link. (Edits are validated
    // server-side, including legacy class-only rows without targets.)
    const teacherAssignmentId = form.teacherAssignmentId.trim()
    if (!editingId && form.visibility === "CLASS_ONLY" && !lessonId && !teacherAssignmentId) {
      setError(t("resources.assignmentRequired"))
      return
    }

    if (linkType && !isValidHttpUrl(externalUrl)) {
      setError(t("resources.externalUrlInvalid"))
      return
    }
    if (!linkType && !file && (!editingId || !editingHasFile)) {
      setError(t("resources.fileRequired"))
      return
    }

    setSaving(true)
    setError(null)
    setUploadProgress(null)
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        resourceType: form.resourceType,
        visibility: form.visibility,
        lessonId: lessonId || undefined,
        // Only class-only resources carry a teaching-assignment target; the
        // server re-validates ownership, institution and ACTIVE status.
        teacherAssignmentId:
          form.visibility === "CLASS_ONLY" && teacherAssignmentId ? teacherAssignmentId : undefined,
        externalUrl: linkType ? externalUrl : undefined,
        isDownloadable: form.isDownloadable,
        isPreviewable: form.isPreviewable,
      }

      let result: ResourceItem | null = null
      if (file) {
        // Multipart: the server reads the real bytes to extract metadata and
        // stores them through the media service.
        result = await saveResourceWithFile(payload, file, {
          resourceId: editingId ?? undefined,
          onProgress: (p) => setUploadProgress(p),
        })
      } else if (editingId) {
        result = await teacherFetch<ResourceItem>(`/v1/resources/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
      } else {
        result = await teacherFetch<ResourceItem>("/v1/resources", {
          method: "POST",
          body: JSON.stringify(payload),
        })
      }

      if (result?.processingStatus === "FAILED") {
        // The row exists but storage/extraction failed — surface the real
        // error instead of pretending success.
        setError(result.processingError || t("resources.processingFailed"))
      } else {
        resetForm()
      }
      await loadResources()
    } catch (err) {
      setError(err instanceof Error && err.message ? err.message : t("resources.saveError"))
    } finally {
      setSaving(false)
      setUploadProgress(null)
      setFile(null)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t("resources.deleteConfirm"))) return
    setDeleting(id)
    setError(null)
    try {
      await teacherFetch(`/v1/resources/${id}`, { method: "DELETE" })
      setResources((prev) => prev.filter((r) => r.id !== id))
    } catch (err) {
      setError(err instanceof Error && err.message ? err.message : t("resources.deleteError"))
    } finally {
      setDeleting(null)
    }
  }

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event

    if (!over || active.id === over.id) return

    const oldIndex = resources.findIndex((r) => r.id === active.id)
    const newIndex = resources.findIndex((r) => r.id === over.id)

    const newResources = Array.from(resources)
    const [removed] = newResources.splice(oldIndex, 1)
    newResources.splice(newIndex, 0, removed)

    const reorderItems = newResources.map((resource, index) => ({
      id: resource.id,
      sortOrder: index,
    }))

    setResources(newResources)
    setReordering(true)

    try {
      await teacherApi.reorderResources(reorderItems)
    } catch (err) {
      setError(err instanceof Error && err.message ? err.message : t("resources.reorderError"))
      await loadResources()
    } finally {
      setReordering(false)
    }
  }

  const filtered = resources
    .filter((resource) => {
      const query = search.trim().toLowerCase()
      const matchesSearch =
        !query ||
        resource.title.toLowerCase().includes(query) ||
        (resource.description || "").toLowerCase().includes(query)
      const matchesType = !typeFilter || resource.resourceType === typeFilter
      return matchesSearch && matchesType
    })
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("teacherResources")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("resources.subtitle")}</p>
        </div>
        <Button onClick={showForm ? resetForm : openCreate} className="gap-2">
          {showForm ? <X className="size-4" /> : <Plus className="size-4" />}
          {showForm ? tc("cancel") : t("resources.newResource")}
        </Button>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
          <button onClick={() => setError(null)} className="ml-auto" aria-label={tc("close")}>
            <X className="size-4" />
          </button>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t("resources.searchPlaceholder")}
            aria-label={t("resources.searchPlaceholder")}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm text-foreground outline-none focus:border-ring"
          />
        </div>
        <div className="relative">
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            aria-label={t("resources.filterType")}
            className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm text-foreground outline-none focus:border-ring sm:w-52"
          >
            <option value="">{tc("allTypes")}</option>
            {RESOURCE_TYPES.map((type) => (
              <option key={type} value={type}>
                {typeLabel(type, t)}
              </option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">
              {editingId ? t("resources.editTitle") : t("resources.createTitle")}
            </h3>
            <button onClick={resetForm} className="text-muted-foreground hover:text-foreground" aria-label={tc("close")}>
              <X className="size-4" />
            </button>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">{t("resources.titleLabel")}</label>
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t("resources.titlePlaceholder")}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">{t("resources.descLabel")}</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={2}
                placeholder={t("resources.descPlaceholder")}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.typeLabel")}</label>
              <div className="relative mt-1">
                <select
                  value={form.resourceType}
                  onChange={(e) => handleTypeChange(e.target.value)}
                  className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 pr-10 text-sm text-foreground focus:border-primary focus:outline-none"
                >
                  {RESOURCE_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {typeLabel(type, t)}
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("visibility.label")}</label>
              <div className="relative mt-1">
                <select
                  value={form.visibility}
                  onChange={(e) => setForm({ ...form, visibility: e.target.value })}
                  className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 pr-10 text-sm text-foreground focus:border-primary focus:outline-none"
                >
                  {VISIBILITIES.map((visibility) => (
                    <option key={visibility} value={visibility}>
                      {visibilityLabel(visibility, t)}
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            {form.visibility === "CLASS_ONLY" && (
              <div className="sm:col-span-2">
                <label className="text-xs font-medium text-muted-foreground">
                  {t("resources.assignmentLabel")}
                  {!form.lessonId.trim() ? " *" : ""}
                </label>
                <div className="relative mt-1">
                  <select
                    value={form.teacherAssignmentId}
                    onChange={(e) => setForm({ ...form, teacherAssignmentId: e.target.value })}
                    className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 pr-10 text-sm text-foreground focus:border-primary focus:outline-none"
                  >
                    <option value="">{t("resources.assignmentPick")}</option>
                    {assignments.map((assignment) => {
                      const ended = assignment.status === "ENDED"
                      const label = `${assignment.classGroupName || assignment.classGroupId} · ${
                        assignment.subjectName || assignment.subjectId
                      }${ended ? ` (${t("resources.assignmentEnded")})` : ""}`
                      return (
                        <option
                          key={assignment.id}
                          value={assignment.id}
                          // Ended assignments are history: selectable only when
                          // they are the resource's existing target (no-op edit).
                          disabled={ended && assignment.id !== form.teacherAssignmentId}
                        >
                          {label}
                        </option>
                      )
                    })}
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {assignments.length === 0
                    ? t("resources.assignmentEmpty")
                    : t("resources.assignmentHint")}
                </p>
              </div>
            )}
            {isLinkType(form.resourceType) ? (
              <div className="sm:col-span-2">
                <label className="text-xs font-medium text-muted-foreground">
                  {t("resources.externalUrlLabel")} *
                </label>
                <input
                  value={form.externalUrl}
                  onChange={(e) => setForm({ ...form, externalUrl: e.target.value })}
                  type="url"
                  inputMode="url"
                  placeholder="https://..."
                  className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                />
                <p className="mt-1 text-xs text-muted-foreground">{t("resources.linkTypeHint")}</p>
              </div>
            ) : (
              <div className="sm:col-span-2">
                <label className="text-xs font-medium text-muted-foreground">
                  {editingId && editingHasFile ? t("resources.replaceFileLabel") : t("resources.fileLabel")} *
                </label>
                <input
                  type="file"
                  accept={FILE_ACCEPT[form.resourceType]}
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                  className="mt-1 block w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground file:mr-3 file:rounded-md file:border-0 file:bg-primary/10 file:px-3 file:py-1 file:text-xs file:font-medium file:text-primary focus:border-primary focus:outline-none"
                />
                <p className="mt-1 text-xs text-muted-foreground">{t("resources.fileHint")}</p>
                {file && (
                  <p className="mt-1 text-xs text-foreground">
                    {file.name} · {(file.size / (1024 * 1024)).toFixed(2)} MB
                  </p>
                )}
                {!file && editingHasFile && (
                  <p className="mt-1 text-xs text-muted-foreground">{t("resources.keepExistingFile")}</p>
                )}
              </div>
            )}
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.lessonIdLabel")}</label>
              <input
                value={form.lessonId}
                onChange={(e) => setForm({ ...form, lessonId: e.target.value })}
                placeholder="00000000-0000-0000-0000-000000000000"
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.sortOrderLabel")}</label>
              <p className="mt-2 text-xs text-muted-foreground">{t("resources.sortOrderAuto")}</p>
            </div>
            <div className="flex flex-wrap items-center gap-x-6 gap-y-2 sm:col-span-2">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={form.isDownloadable}
                  onChange={(e) => setForm({ ...form, isDownloadable: e.target.checked })}
                  className="size-4 rounded border-border"
                />
                <span className="text-sm text-foreground">{t("resources.downloadableLabel")}</span>
              </label>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={form.isPreviewable}
                  onChange={(e) => setForm({ ...form, isPreviewable: e.target.checked })}
                  className="size-4 rounded border-border"
                />
                <span className="text-sm text-foreground">{t("resources.previewableLabel")}</span>
              </label>
            </div>
          </div>
          <div className="space-y-3">
            {uploadProgress != null && (
              <div aria-live="polite">
                <div className="flex justify-between text-xs text-muted-foreground">
                  <span>{t("resources.uploading")}</span>
                  <span>{uploadProgress}%</span>
                </div>
                <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-primary transition-all duration-200"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
              </div>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={resetForm} disabled={saving}>
                {tc("cancel")}
              </Button>
              <Button
                onClick={handleSave}
                disabled={
                  saving ||
                  !form.title.trim() ||
                  (!isLinkType(form.resourceType) && !file && (!editingId || !editingHasFile))
                }
              >
                {saving ? <Loader2 className="size-4 animate-spin" /> : <Upload className="size-4" />}
                {saving && uploadProgress != null
                  ? `${t("resources.uploading")} ${uploadProgress}%`
                  : editingId
                    ? tc("save")
                    : tc("create")}
              </Button>
            </div>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <FileText className="mx-auto mb-3 size-8 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">{t("resources.emptyTitle")}</h3>
          <p className="mt-1 text-xs text-muted-foreground">{t("resources.emptyDesc")}</p>
          <Button onClick={openCreate} className="mt-4 gap-2" size="sm">
            <Plus className="size-3" /> {t("resources.newResource")}
          </Button>
        </div>
      ) : (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext items={filtered.map((r) => r.id)} strategy={verticalListSortingStrategy}>
            <div className="space-y-3" role="list" aria-label={t("resources.listLabel")}>
              {filtered.map((resource, index) => (
                <SortableResourceRow
                  key={resource.id}
                  resource={resource}
                  index={index}
                  onEdit={() => openEdit(resource)}
                  onDelete={() => handleDelete(resource.id)}
                  onShare={() => handleShare(resource)}
                  copied={copiedId === resource.id}
                  deleting={deleting === resource.id}
                  assignmentTarget={
                    resource.teacherAssignmentId
                      ? (assignments.find((a) => a.id === resource.teacherAssignmentId) ?? null)
                      : null
                  }
                  t={t}
                  tc={tc}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}
      {reordering && (
        <div className="fixed bottom-4 right-4 rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground shadow-lg animate-in fade-in slide-in-from-bottom-2">
          {t("resources.reorderSaving")}
        </div>
      )}
    </div>
  )
}