"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { Video, Plus, Clock, Users, ExternalLink, Pencil, XCircle, Loader2, AlertCircle, Calendar, Edit, Trash2 } from "lucide-react"

async function teacherFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const res = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

interface LiveClass {
  id: string
  title: string
  description: string
  scheduledAt: string
  durationMinutes: number
  meetingUrl: string
  maxParticipants: number
  status: string
  createdAt: string
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectName: string
}

const statusConfig: Record<string, { label: string; className: string }> = {
  SCHEDULED: { label: "Scheduled", className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400" },
  IN_PROGRESS: { label: "In Progress", className: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400" },
  COMPLETED: { label: "Completed", className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400" },
  CANCELLED: { label: "Cancelled", className: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" },
}

const initialForm = {
  title: "",
  description: "",
  scheduledAt: "",
  durationMinutes: 60,
  meetingUrl: "",
  maxParticipants: 50,
}

export default function TeacherLiveClassesPage() {
  const { user } = useAuth()
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [liveClassesData, classesData] = await Promise.allSettled([
        teacherFetch<LiveClass[]>("/v1/teachers/me/live-classes"),
        teacherFetch<ClassOption[]>("/v1/teachers/me/classes"),
      ])
      if (liveClassesData.status === "fulfilled") {
        setLiveClasses(liveClassesData.value)
      } else {
        setError("Failed to load live classes")
      }
      if (classesData.status === "fulfilled") {
        setClasses(classesData.value)
      }
    } catch {
      setError("Failed to load data")
    } finally {
      setLoading(false)
    }
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
  }

  function startEdit(lc: LiveClass) {
    setForm({
      title: lc.title,
      description: lc.description,
      scheduledAt: lc.scheduledAt ? new Date(lc.scheduledAt).toISOString().slice(0, 16) : "",
      durationMinutes: lc.durationMinutes,
      meetingUrl: lc.meetingUrl,
      maxParticipants: lc.maxParticipants,
    })
    setEditingId(lc.id)
    setShowForm(true)
  }

  async function handleSubmit() {
    if (!form.title.trim() || !form.scheduledAt) {
      setError("Title and scheduled date/time are required")
      return
    }
    try {
      setSubmitting(true)
      setError(null)
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        durationMinutes: Number(form.durationMinutes) || 60,
        meetingUrl: form.meetingUrl.trim(),
        maxParticipants: Number(form.maxParticipants) || 50,
      }

      if (editingId) {
        await teacherFetch(`/v1/teachers/me/live-classes/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
        setSuccess("Live class updated successfully")
      } else {
        await teacherFetch("/v1/teachers/me/live-classes", {
          method: "POST",
          body: JSON.stringify(payload),
        })
        setSuccess("Live class scheduled successfully")
      }
      resetForm()
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save live class")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCancel(id: string) {
    if (!confirm("Are you sure you want to cancel this live class?")) return
    try {
      setError(null)
      await teacherFetch(`/v1/teachers/me/live-classes/${id}`, { method: "DELETE" })
      setSuccess("Live class cancelled")
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel live class")
    }
  }

  function formatDate(iso: string) {
    if (!iso) return "—"
    return new Date(iso).toLocaleString(undefined, {
      weekday: "short",
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  const sortedClasses = [...liveClasses].sort((a, b) => {
    const dateA = new Date(a.scheduledAt).getTime()
    const dateB = new Date(b.scheduledAt).getTime()
    return dateB - dateA
  })

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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Live Classes</h1>
          <p className="mt-1 text-sm text-muted-foreground">Schedule and manage your live classes.</p>
        </div>
        <Button
          className="gap-2"
          onClick={() => {
            resetForm()
            setShowForm(!showForm)
          }}
        >
          {showForm ? <XCircle className="size-4" /> : <Plus className="size-4" />}
          {showForm ? "Cancel" : "Schedule Class"}
        </Button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-4">
          <div className="flex items-center gap-2 text-sm text-green-600">
            <Video className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">
            {editingId ? "Edit Live Class" : "Schedule New Live Class"}
          </h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Title *</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Live class title"
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Brief description of the class..."
                rows={3}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Date & Time *</label>
              <input
                type="datetime-local"
                value={form.scheduledAt}
                onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Duration (minutes)</label>
              <input
                type="number"
                value={form.durationMinutes}
                onChange={(e) => setForm({ ...form, durationMinutes: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Meeting URL</label>
              <input
                type="url"
                value={form.meetingUrl}
                onChange={(e) => setForm({ ...form, meetingUrl: e.target.value })}
                placeholder="https://meet.google.com/..."
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Max Participants</label>
              <input
                type="number"
                value={form.maxParticipants}
                onChange={(e) => setForm({ ...form, maxParticipants: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>Cancel</Button>
            <Button onClick={handleSubmit} disabled={submitting || !form.title.trim() || !form.scheduledAt}>
              {submitting ? "Saving..." : editingId ? "Update Class" : "Schedule Class"}
            </Button>
          </div>
        </div>
      )}

      {sortedClasses.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Live Classes</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Schedule your first live class to get started.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {sortedClasses.map((lc) => {
            const status = statusConfig[lc.status] || statusConfig.SCHEDULED
            return (
              <div key={lc.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                    <Video className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-sm font-semibold text-foreground">{lc.title}</h3>
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${status.className}`}>
                        {status.label}
                      </span>
                    </div>
                    {lc.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{lc.description}</p>
                    )}
                    <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground flex-wrap">
                      <span className="flex items-center gap-1">
                        <Calendar className="size-3" />
                        {formatDate(lc.scheduledAt)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock className="size-3" />
                        {lc.durationMinutes} min
                      </span>
                      <span className="flex items-center gap-1">
                        <Users className="size-3" />
                        {lc.maxParticipants} max
                      </span>
                      {lc.meetingUrl && (
                        <a
                          href={lc.meetingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center gap-1 text-primary hover:underline"
                        >
                          <ExternalLink className="size-3" />
                          Join
                        </a>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => startEdit(lc)}
                      title="Edit"
                    >
                      <Edit className="size-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => handleCancel(lc.id)}
                      title="Cancel"
                      className="text-destructive hover:text-destructive"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
