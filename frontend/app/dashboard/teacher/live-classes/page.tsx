"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import { Video, Plus, Clock, Users, XCircle, Loader2, AlertCircle, Calendar, Edit, Trash2, Play, Square, ExternalLink, BookOpen, GraduationCap, CheckCircle2, Circle } from "lucide-react"
import { appFetch } from "@/lib/fetch"

interface LiveClass {
  id: string
  title: string
  description: string
  scheduledAt: string
  durationMinutes: number
  maxParticipants: number
  status: string
  subjectName: string | null
  teacherName: string | null
  classGroupId: string | null
  subjectId: string | null
  createdAt: string
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectName: string
}

interface SubjectOption {
  id: string
  name: string
  code: string
}

const statusConfig: Record<string, { label: string; className: string }> = {
  SCHEDULED: { label: "Scheduled", className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400" },
  STARTING: { label: "Starting", className: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400" },
  IN_PROGRESS: { label: "Live", className: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400" },
  LIVE: { label: "Live", className: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400" },
  ENDING: { label: "Ending", className: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400" },
  COMPLETED: { label: "Completed", className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400" },
  ENDED: { label: "Ended", className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400" },
  CANCELLED: { label: "Cancelled", className: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" },
  SERVICE_DEGRADED: { label: "Degraded", className: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400" },
  SERVICE_UNAVAILABLE: { label: "Unavailable", className: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" },
  RECOVERING: { label: "Recovering", className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400" },
}

const initialForm = {
  title: "",
  description: "",
  scheduledAt: "",
  durationMinutes: 60,
  maxParticipants: 50,
  classGroupId: "",
  subjectId: "",
  enableRecording: false,
}

export default function TeacherLiveClassesPage() {
  const { user } = useAuth()
  const [liveClasses, setLiveClasses] = useState<LiveClass[]>([])
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [subjects, setSubjects] = useState<SubjectOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [reviewMode, setReviewMode] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [liveClassesData, classesData] = await Promise.allSettled([
        appFetch<LiveClass[]>("/v1/teachers/me/live-classes"),
        appFetch<ClassOption[]>("/v1/teachers/me/classes"),
      ])
      if (liveClassesData.status === "fulfilled") {
        setLiveClasses(liveClassesData.value)
      } else {
        setError("Failed to load live classes")
      }
      if (classesData.status === "fulfilled") {
        setClasses(classesData.value)
        const uniqueSubjects = new Map<string, SubjectOption>()
        classesData.value.forEach((c) => {
          if (c.subjectName && c.classGroupId) {
            uniqueSubjects.set(c.subjectName, {
              id: c.classGroupId,
              name: c.subjectName,
              code: c.subjectName,
            })
          }
        })
        setSubjects(Array.from(uniqueSubjects.values()))
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
    setReviewMode(false)
  }

  function startEdit(lc: LiveClass) {
    setForm({
      title: lc.title,
      description: lc.description || "",
      scheduledAt: lc.scheduledAt ? new Date(lc.scheduledAt).toISOString().slice(0, 16) : "",
      durationMinutes: lc.durationMinutes,
      maxParticipants: lc.maxParticipants || 50,
      classGroupId: lc.classGroupId || "",
      subjectId: lc.subjectId || "",
    })
    setEditingId(lc.id)
    setShowForm(true)
  }

  function handleProceedToReview() {
    if (!form.title.trim() || !form.scheduledAt) {
      setError("Title and scheduled date/time are required")
      return
    }
    const scheduledDate = new Date(form.scheduledAt)
    if (scheduledDate < new Date()) {
      setError("Scheduled time must be in the future")
      return
    }
    setError(null)
    setReviewMode(true)
  }

  async function handleSubmit() {
    try {
      setSubmitting(true)
      setError(null)
      const scheduledDate = new Date(form.scheduledAt)
      const payload: Record<string, unknown> = {
        title: form.title.trim(),
        description: form.description.trim(),
        scheduledAt: scheduledDate.toISOString(),
        durationMinutes: Number(form.durationMinutes) || 60,
        maxParticipants: Number(form.maxParticipants) || 50,
      }
      if (form.classGroupId) payload.classGroupId = form.classGroupId
      if (form.subjectId) payload.subjectId = form.subjectId

      if (editingId) {
        await appFetch(`/v1/teachers/me/live-classes/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
        setSuccess("Live class updated successfully")
      } else {
        await appFetch("/v1/teachers/me/live-classes", {
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
      await appFetch(`/v1/teachers/me/live-classes/${id}`, { method: "DELETE" })
      setSuccess("Live class cancelled")
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel live class")
    }
  }

  async function handleStartLive(id: string) {
    try {
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}/start`, { method: "POST" })
      setSuccess("Live class started!")
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start live class")
    }
  }

  async function handleEndLive(id: string) {
    if (!confirm("End this live class? Students will no longer be able to join.")) return
    try {
      setError(null)
      await appFetch(`/v1/teachers/me/live-classes/${id}/end`, { method: "POST" })
      setSuccess("Live class ended")
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to end live class")
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

  function getClassName(classGroupId: string | null) {
    if (!classGroupId) return null
    const found = classes.find((c) => c.classGroupId === classGroupId)
    return found?.className || null
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
                placeholder="e.g. Algebra — Introduction to Linear Equations"
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                <GraduationCap className="mr-1 inline size-3" />
                Class / Group
              </label>
              <select
                value={form.classGroupId}
                onChange={(e) => setForm({ ...form, classGroupId: e.target.value })}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">Select class (optional)</option>
                {classes.map((c) => (
                  <option key={c.classGroupId} value={c.classGroupId}>
                    {c.className} {c.subjectName ? `— ${c.subjectName}` : ""}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                <BookOpen className="mr-1 inline size-3" />
                Subject
              </label>
              <select
                value={form.subjectId}
                onChange={(e) => setForm({ ...form, subjectId: e.target.value })}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">Select subject (optional)</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="What will be covered in this session..."
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
                max={480}
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

          <div className="flex items-center gap-4 rounded-lg border border-border bg-background p-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={form.enableRecording}
                onChange={(e) => setForm({ ...form, enableRecording: e.target.checked })}
                className="size-4 rounded border-border"
              />
              <span className="text-sm text-foreground">Enable recording</span>
            </label>
            <span className="text-xs text-muted-foreground">Record this session for replay</span>
          </div>

          <div className="rounded-lg bg-muted/50 px-3 py-1.5 text-xs text-muted-foreground">
            Timezone: Africa/Dar_es_Salaam (UTC+03:00)
          </div>

          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>Cancel</Button>
            {editingId ? (
              <Button onClick={handleSubmit} disabled={submitting || !form.title.trim() || !form.scheduledAt}>
                {submitting ? "Saving..." : "Update Class"}
              </Button>
            ) : (
              <Button onClick={handleProceedToReview} disabled={!form.title.trim() || !form.scheduledAt}>
                Review & Schedule
              </Button>
            )}
          </div>
        </div>
      )}

      {reviewMode && !editingId && (
        <div className="rounded-2xl border border-primary/30 bg-card p-5 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="size-5 text-primary" />
            <h2 className="text-base font-semibold text-foreground">Review Live Class</h2>
          </div>
          <div className="grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <span className="text-xs font-medium text-muted-foreground">Title</span>
              <p className="text-foreground">{form.title}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">Date & Time</span>
              <p className="text-foreground">{form.scheduledAt ? new Date(form.scheduledAt).toLocaleString() : "—"}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">Duration</span>
              <p className="text-foreground">{form.durationMinutes} minutes</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">Max Participants</span>
              <p className="text-foreground">{form.maxParticipants}</p>
            </div>
            {form.classGroupId && (
              <div>
                <span className="text-xs font-medium text-muted-foreground">Class</span>
                <p className="text-foreground">{getClassName(form.classGroupId) || form.classGroupId}</p>
              </div>
            )}
            {form.subjectId && (
              <div>
                <span className="text-xs font-medium text-muted-foreground">Subject</span>
                <p className="text-foreground">{subjects.find((s) => s.id === form.subjectId)?.name || form.subjectId}</p>
              </div>
            )}
            <div>
              <span className="text-xs font-medium text-muted-foreground">Recording</span>
              <p className="text-foreground">{form.enableRecording ? "Enabled" : "Disabled"}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">Timezone</span>
              <p className="text-foreground">Africa/Dar_es_Salaam (UTC+03:00)</p>
            </div>
          </div>
          {form.description && (
            <div>
              <span className="text-xs font-medium text-muted-foreground">Description</span>
              <p className="mt-1 text-sm text-foreground whitespace-pre-line">{form.description}</p>
            </div>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setReviewMode(false)}>Back to Edit</Button>
            <Button onClick={handleSubmit} disabled={submitting} className="gap-1 bg-green-600 hover:bg-green-700 text-white">
              {submitting ? "Scheduling..." : "Confirm & Schedule"}
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
            const className = getClassName(lc.classGroupId)
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
                    <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                      {lc.subjectName && (
                        <span className="flex items-center gap-1">
                          <BookOpen className="size-3" />
                          {lc.subjectName}
                        </span>
                      )}
                      {className && (
                        <span className="flex items-center gap-1">
                          <GraduationCap className="size-3" />
                          {className}
                        </span>
                      )}
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
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {lc.status === "SCHEDULED" && (
                      <>
                        <Button
                          size="sm"
                          variant="outline"
                          className="gap-1"
                          onClick={() => window.open(`/dashboard/teacher/live-classes/${lc.id}/prepare`, "_blank")}
                        >
                          Prepare
                        </Button>
                        <Button
                          size="sm"
                          className="gap-1 bg-green-600 hover:bg-green-700 text-white"
                          onClick={() => handleStartLive(lc.id)}
                        >
                          <Play className="size-3" />
                          Start Live
                        </Button>
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
                      </>
                    )}
                    {(lc.status === "IN_PROGRESS" || lc.status === "LIVE") && (
                      <>
                        <Button
                          size="sm"
                          className="gap-1"
                          onClick={() => window.open(`/live-classes/${lc.id}`, "_blank")}
                        >
                          <ExternalLink className="size-3" />
                          Open Classroom
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          className="gap-1"
                          onClick={() => handleEndLive(lc.id)}
                        >
                          <Square className="size-3" />
                          End
                        </Button>
                      </>
                    )}
                    {(lc.status === "COMPLETED" || lc.status === "ENDED" || lc.status === "CANCELLED") && (
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => handleCancel(lc.id)}
                        title="Delete"
                        className="text-destructive hover:text-destructive"
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    )}
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
