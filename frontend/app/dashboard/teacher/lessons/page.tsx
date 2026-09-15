"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { BookOpen, Plus, Pencil, Trash2, Loader2, AlertCircle, ChevronDown, Eye, EyeOff, GripVertical, X } from "lucide-react"
import { teacherFetch, type ClassGroupInfo } from "@/lib/teacher-api"

interface Lesson {
  id: string
  subjectId: string
  classGroupId: string
  title: string
  description: string | null
  contentText: string | null
  videoUrl: string | null
  fileAttachments: string | null
  sortOrder: number
  isPublished: boolean
  createdAt: string
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectName: string
  subjectId: string
}

const initialForm = {
  title: "",
  description: "",
  contentText: "",
  videoUrl: "",
  subjectId: "",
  classGroupId: "",
  sortOrder: 0,
  isPublished: false,
}

export default function TeacherLessonsPage() {
  const { user } = useAuth()
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingLesson, setEditingLesson] = useState<Lesson | null>(null)
  const [form, setForm] = useState(initialForm)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const profileRes = await teacherFetch<{ content: { id: string; email: string }[] }>("/v1/teachers?page=0&size=50")
      const teacher = profileRes.content?.find((t) => t.email === user?.email)
      if (teacher) {
        const assigns = await teacherFetch<{ classGroupId: string; subjectId: string }[]>(`/v1/teachers/${teacher.id}/assignments`).catch(() => [])
        const allClasses = await teacherFetch<ClassGroupInfo[]>("/v1/academic/class-groups").catch(() => [])
        const classOptions: ClassOption[] = assigns.map((a) => {
          const cg = allClasses.find((c) => c.id === a.classGroupId)
          return {
            classGroupId: a.classGroupId,
            className: cg?.name || "Unknown Class",
            subjectName: cg?.gradeName || "General",
            subjectId: a.subjectId,
          }
        })
        const unique = classOptions.filter((c, i, arr) => arr.findIndex((x) => x.classGroupId === c.classGroupId) === i)
        setClasses(unique.length > 0 ? unique : allClasses.slice(0, 5).map((c) => ({ classGroupId: c.id, className: c.name, subjectName: c.gradeName || "", subjectId: "" })))
      }
      await loadLessons()
    } catch {
      setError("Failed to load data")
    } finally {
      setLoading(false)
    }
  }

  async function loadLessons() {
    try {
      if (selectedClassId) {
        const data = await teacherFetch<Lesson[]>(`/v1/learning/lessons/class/${selectedClassId}`)
        setLessons(data)
      }
    } catch {
      setLessons([])
    }
  }

  useEffect(() => {
    if (selectedClassId) loadLessons()
  }, [selectedClassId])

  async function handleSave() {
    if (!form.title || !form.classGroupId) return
    setSaving(true)
    try {
      const body = {
        title: form.title,
        description: form.description || undefined,
        contentText: form.contentText || undefined,
        videoUrl: form.videoUrl || undefined,
        subjectId: form.subjectId || classes.find((c) => c.classGroupId === form.classGroupId)?.subjectId || "",
        classGroupId: form.classGroupId,
        sortOrder: form.sortOrder,
        isPublished: form.isPublished,
      }
      if (editingLesson) {
        await teacherFetch(`/v1/learning/lessons/${editingLesson.id}`, { method: "PUT", body: JSON.stringify(body) })
      } else {
        await teacherFetch("/v1/learning/lessons", { method: "POST", body: JSON.stringify(body) })
      }
      setShowForm(false)
      setEditingLesson(null)
      setForm(initialForm)
      await loadLessons()
    } catch {
      setError("Failed to save lesson")
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("Delete this lesson?")) return
    setDeleting(id)
    try {
      await teacherFetch(`/v1/learning/lessons/${id}`, { method: "DELETE" })
      await loadLessons()
    } catch {
      setError("Failed to delete lesson")
    } finally {
      setDeleting(null)
    }
  }

  async function togglePublish(lesson: Lesson) {
    try {
      await teacherFetch(`/v1/learning/lessons/${lesson.id}`, {
        method: "PUT",
        body: JSON.stringify({ isPublished: !lesson.isPublished }),
      })
      await loadLessons()
    } catch {
      setError("Failed to update lesson")
    }
  }

  function openEdit(lesson: Lesson) {
    setEditingLesson(lesson)
    setForm({
      title: lesson.title,
      description: lesson.description || "",
      contentText: lesson.contentText || "",
      videoUrl: lesson.videoUrl || "",
      subjectId: lesson.subjectId,
      classGroupId: lesson.classGroupId,
      sortOrder: lesson.sortOrder,
      isPublished: lesson.isPublished,
    })
    setShowForm(true)
  }

  function openCreate() {
    setEditingLesson(null)
    setForm({ ...initialForm, classGroupId: selectedClassId, subjectId: classes.find((c) => c.classGroupId === selectedClassId)?.subjectId || "" })
    setShowForm(true)
  }

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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Lessons</h1>
          <p className="mt-1 text-sm text-muted-foreground">Create and manage lesson content for your classes.</p>
        </div>
        {selectedClassId && (
          <Button onClick={openCreate} className="gap-2">
            <Plus className="size-4" /> Create Lesson
          </Button>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />{error}
          <button onClick={() => setError(null)} className="ml-auto"><X className="size-4" /></button>
        </div>
      )}

      <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
        <label className="text-sm font-medium text-foreground">Select Class</label>
        <div className="relative mt-1">
          <select
            value={selectedClassId}
            onChange={(e) => { setSelectedClassId(e.target.value); setLessons([]) }}
            className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="">Choose a class...</option>
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>{c.className}</option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">{editingLesson ? "Edit Lesson" : "Create Lesson"}</h3>
            <button onClick={() => { setShowForm(false); setEditingLesson(null) }} className="text-muted-foreground hover:text-foreground"><X className="size-4" /></button>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">Title *</label>
              <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="Lesson title" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Class *</label>
              <select value={form.classGroupId} onChange={(e) => setForm({ ...form, classGroupId: e.target.value })} className="mt-1 w-full appearance-none rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none">
                <option value="">Select class</option>
                {classes.map((c) => <option key={c.classGroupId} value={c.classGroupId}>{c.className}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Sort Order</label>
              <input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: parseInt(e.target.value) || 0 })} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
            </div>
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">Description</label>
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="Brief description" />
            </div>
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">Content</label>
              <textarea value={form.contentText} onChange={(e) => setForm({ ...form, contentText: e.target.value })} rows={6} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="Lesson content (supports plain text)" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Video URL</label>
              <input value={form.videoUrl} onChange={(e) => setForm({ ...form, videoUrl: e.target.value })} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="https://..." />
            </div>
            <div className="flex items-end">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.isPublished} onChange={(e) => setForm({ ...form, isPublished: e.target.checked })} className="size-4 rounded border-border" />
                <span className="text-sm text-foreground">Published</span>
              </label>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => { setShowForm(false); setEditingLesson(null) }}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving || !form.title || !form.classGroupId}>
              {saving ? <Loader2 className="size-4 animate-spin" /> : null}
              {editingLesson ? "Update" : "Create"}
            </Button>
          </div>
        </div>
      )}

      {!selectedClassId ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">Select a class to manage lessons</p>
          <p className="mt-1 text-xs text-muted-foreground">Choose a class above to see and create lessons.</p>
        </div>
      ) : lessons.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <BookOpen className="mx-auto mb-3 size-8 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">No Lessons Yet</h3>
          <p className="mt-1 text-xs text-muted-foreground">Create your first lesson for this class.</p>
          <Button onClick={openCreate} className="mt-4 gap-2" size="sm"><Plus className="size-3" /> Create Lesson</Button>
        </div>
      ) : (
        <div className="space-y-3">
          {lessons.sort((a, b) => a.sortOrder - b.sortOrder).map((lesson) => (
            <div key={lesson.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3 min-w-0">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-xs font-bold text-primary">{lesson.sortOrder}</div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-semibold text-foreground truncate">{lesson.title}</h4>
                      <span className={`inline-flex shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${
                        lesson.isPublished ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"
                      }`}>{lesson.isPublished ? "Published" : "Draft"}</span>
                    </div>
                    {lesson.description && <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{lesson.description}</p>}
                    <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                      {lesson.contentText && <span className="flex items-center gap-1"><BookOpen className="size-3" /> Has content</span>}
                      {lesson.videoUrl && <span className="flex items-center gap-1">🎥 Video</span>}
                    </div>
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <button onClick={() => togglePublish(lesson)} className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground" title={lesson.isPublished ? "Unpublish" : "Publish"}>
                    {lesson.isPublished ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </button>
                  <button onClick={() => openEdit(lesson)} className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground" title="Edit"><Pencil className="size-4" /></button>
                  <button onClick={() => handleDelete(lesson.id)} disabled={deleting === lesson.id} className="rounded-lg p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive" title="Delete">
                    {deleting === lesson.id ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
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
