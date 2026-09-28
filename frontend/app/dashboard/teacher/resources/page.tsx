"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import {
  AlertCircle,
  ChevronDown,
  FileText,
  Link2,
  Loader2,
  Music,
  Image as ImageIcon,
  Pencil,
  Plus,
  Search,
  Trash2,
  Video,
  X,
} from "lucide-react"
import { teacherFetch } from "@/lib/teacher-api"

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
  sortOrder?: number | null
  isDownloadable?: boolean | null
  isPreviewable?: boolean | null
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
  storageUrl: "",
  externalUrl: "",
  mimeType: "",
  fileSize: "",
  lessonId: "",
  sortOrder: 0,
  isDownloadable: true,
  isPreviewable: false,
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

  useEffect(() => {
    if (!user) return
    loadResources(true)
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
  }

  function openCreate() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(true)
  }

  function openEdit(resource: ResourceItem) {
    setForm({
      title: resource.title || "",
      description: resource.description || "",
      resourceType: resource.resourceType || "DOCUMENT",
      visibility: resource.visibility || "DRAFT",
      storageUrl: resource.storageUrl || "",
      externalUrl: resource.externalUrl || "",
      mimeType: resource.mimeType || "",
      fileSize: resource.fileSize != null ? String(resource.fileSize) : "",
      lessonId: resource.lessonId || "",
      sortOrder: resource.sortOrder || 0,
      isDownloadable: resource.isDownloadable !== false,
      isPreviewable: resource.isPreviewable === true,
    })
    setEditingId(resource.id)
    setShowForm(true)
  }

  async function handleSave() {
    if (!form.title.trim()) return
    const lessonId = form.lessonId.trim()
    if (lessonId && !UUID_RE.test(lessonId)) {
      setError(t("resources.invalidLessonId"))
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        resourceType: form.resourceType === "LINK" ? "EXTERNAL_LINK" : form.resourceType,
        visibility: form.visibility,
        storageUrl: form.storageUrl.trim() || undefined,
        externalUrl: form.externalUrl.trim() || undefined,
        mimeType: form.mimeType.trim() || undefined,
        fileSize: form.fileSize.trim() ? Number(form.fileSize) || undefined : undefined,
        lessonId: lessonId || undefined,
        sortOrder: Number(form.sortOrder) || 0,
        isDownloadable: form.isDownloadable,
        isPreviewable: form.isPreviewable,
      }
      if (editingId) {
        await teacherFetch(`/v1/resources/${editingId}`, { method: "PUT", body: JSON.stringify(payload) })
      } else {
        await teacherFetch("/v1/resources", { method: "POST", body: JSON.stringify(payload) })
      }
      resetForm()
      await loadResources()
    } catch (err) {
      setError(err instanceof Error && err.message ? err.message : t("resources.saveError"))
    } finally {
      setSaving(false)
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

  function typeLabel(resourceType: string) {
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

  function visibilityLabel(visibility: string) {
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
                {typeLabel(type)}
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
                  onChange={(e) => setForm({ ...form, resourceType: e.target.value })}
                  className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 pr-10 text-sm text-foreground focus:border-primary focus:outline-none"
                >
                  {RESOURCE_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {typeLabel(type)}
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
                      {visibilityLabel(visibility)}
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.storageUrlLabel")}</label>
              <input
                value={form.storageUrl}
                onChange={(e) => setForm({ ...form, storageUrl: e.target.value })}
                placeholder="https://..."
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.externalUrlLabel")}</label>
              <input
                value={form.externalUrl}
                onChange={(e) => setForm({ ...form, externalUrl: e.target.value })}
                placeholder="https://..."
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.mimeTypeLabel")}</label>
              <input
                value={form.mimeType}
                onChange={(e) => setForm({ ...form, mimeType: e.target.value })}
                placeholder="application/pdf"
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">{t("resources.fileSizeLabel")}</label>
              <input
                type="number"
                value={form.fileSize}
                onChange={(e) => setForm({ ...form, fileSize: e.target.value })}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </div>
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
              <input
                type="number"
                value={form.sortOrder}
                onChange={(e) => setForm({ ...form, sortOrder: parseInt(e.target.value) || 0 })}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
              />
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
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>
              {tc("cancel")}
            </Button>
            <Button onClick={handleSave} disabled={saving || !form.title.trim()}>
              {saving ? <Loader2 className="size-4 animate-spin" /> : null}
              {editingId ? tc("save") : tc("create")}
            </Button>
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
        <div className="space-y-3">
          {filtered.map((resource) => (
            <div key={resource.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="flex min-w-0 items-start gap-3">
                  <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <TypeIcon type={resource.resourceType} />
                  </div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className="truncate text-sm font-semibold text-foreground">{resource.title}</h4>
                      <span
                        className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${typeClass[(resource.resourceType || "").toUpperCase()] || "bg-muted text-muted-foreground"}`}
                      >
                        {typeLabel(resource.resourceType)}
                      </span>
                      <span
                        className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${visibilityClass[(resource.visibility || "").toUpperCase()] || "bg-gray-100 text-gray-600"}`}
                      >
                        {visibilityLabel(resource.visibility)}
                      </span>
                    </div>
                    {resource.description && (
                      <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{resource.description}</p>
                    )}
                    <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                      <span>
                        {t("resources.colUpdated")}: {formatDate(resource.updatedAt || resource.createdAt)}
                      </span>
                      {resource.mimeType && <span>{resource.mimeType}</span>}
                      {resource.externalUrl && <span>{resource.externalUrl}</span>}
                    </div>
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <button
                    onClick={() => openEdit(resource)}
                    className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                    title={tc("edit")}
                    aria-label={tc("edit")}
                  >
                    <Pencil className="size-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(resource.id)}
                    disabled={deleting === resource.id}
                    className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                    title={tc("delete")}
                    aria-label={tc("delete")}
                  >
                    {deleting === resource.id ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
