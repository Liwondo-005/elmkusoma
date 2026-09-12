"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import {
  FileText,
  Clock,
  CheckCircle,
  Plus,
  Eye,
  X,
  AlertCircle,
  Loader2,
  ChevronDown,
  Calendar,
  Users,
  Paperclip,
  Pencil,
  Trash2,
} from "lucide-react"

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

interface Assignment {
  id: string
  title: string
  description?: string
  instructions?: string
  assignmentType?: string
  dueDate?: string
  totalMarks: number
  status?: string
  attachments?: string
  classGroupId: string
  subjectId: string
  className?: string
  subjectName?: string
  submissionCount?: number
  totalStudents?: number
  createdAt: string
}

interface AssignmentSubmission {
  id: string
  assignmentId: string
  studentId: string
  studentName?: string
  fileUrl?: string
  submittedAt: string
  grade?: number
  obtainedMarks?: number
  feedback?: string
  gradedAt?: string
  createdAt: string
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectId: string
  subjectName: string
}

const assignmentTypes = [
  { value: "ESSAY", label: "Essay" },
  { value: "MULTIPLE_CHOICE", label: "Multiple Choice" },
  { value: "FILL_IN_THE_BLANK", label: "Fill in the Blank" },
  { value: "MATCHING", label: "Matching" },
  { value: "FILE_UPLOAD", label: "File Upload" },
]

const statusOptions = [
  { value: "", label: "All" },
  { value: "DRAFT", label: "Draft" },
  { value: "PUBLISHED", label: "Published" },
]

const statusConfig: Record<string, { label: string; className: string }> = {
  DRAFT: { label: "Draft", className: "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400" },
  PUBLISHED: { label: "Published", className: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400" },
  ARCHIVED: { label: "Archived", className: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400" },
}

const initialForm = {
  title: "",
  description: "",
  instructions: "",
  assignmentType: "ESSAY",
  classGroupId: "",
  subjectId: "",
  dueDate: "",
  totalMarks: 100,
  status: "DRAFT",
  attachments: "",
}

export default function TeacherAssignmentsPage() {
  const { user } = useAuth()
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)

  const [selectedClassId, setSelectedClassId] = useState("")
  const [statusFilter, setStatusFilter] = useState("")
  const [selectedAssignment, setSelectedAssignment] = useState<string | null>(null)
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loadingSubmissions, setLoadingSubmissions] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [classesData, assignmentsData] = await Promise.allSettled([
        teacherFetch<ClassOption[]>("/v1/teachers/me/classes"),
        loadAllAssignments(),
      ])
      if (classesData.status === "fulfilled") {
        setClasses(classesData.value)
      }
      if (assignmentsData.status === "fulfilled") {
        setAssignments(assignmentsData.value)
      }
    } catch {
      setError("Failed to load data")
    } finally {
      setLoading(false)
    }
  }

  async function loadAllAssignments(): Promise<Assignment[]> {
    const classesRes = await teacherFetch<ClassOption[]>("/v1/teachers/me/classes")
    const all: Assignment[] = []
    for (const cls of classesRes) {
      try {
        const data = await teacherFetch<Assignment[]>(`/v1/learning/assignments/class/${cls.classGroupId}`)
        const enriched = data.map((a) => ({
          ...a,
          className: cls.className,
          subjectName: cls.subjectName,
        }))
        all.push(...enriched)
      } catch {
        // skip
      }
    }
    return all
  }

  function resetForm() {
    setForm(initialForm)
    setEditingId(null)
    setShowForm(false)
  }

  function startEdit(a: Assignment) {
    setForm({
      title: a.title,
      description: a.description || "",
      instructions: a.instructions || "",
      assignmentType: a.assignmentType || "ESSAY",
      classGroupId: a.classGroupId,
      subjectId: a.subjectId,
      dueDate: a.dueDate ? new Date(a.dueDate).toISOString().slice(0, 16) : "",
      totalMarks: a.totalMarks,
      status: a.status || "DRAFT",
      attachments: a.attachments || "",
    })
    setEditingId(a.id)
    setShowForm(true)
  }

  async function handleSubmit() {
    if (!form.title.trim() || !form.classGroupId || !form.subjectId) {
      setError("Title, class, and subject are required")
      return
    }
    try {
      setSubmitting(true)
      setError(null)
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        instructions: form.instructions.trim(),
        assignmentType: form.assignmentType,
        classGroupId: form.classGroupId,
        subjectId: form.subjectId,
        dueDate: form.dueDate ? new Date(form.dueDate).toISOString() : undefined,
        totalMarks: Number(form.totalMarks) || 100,
        status: form.status,
        attachments: form.attachments.trim(),
      }

      if (editingId) {
        await teacherFetch(`/v1/learning/assignments/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
        setSuccess("Assignment updated successfully")
      } else {
        await teacherFetch("/v1/learning/assignments", {
          method: "POST",
          body: JSON.stringify(payload),
        })
        setSuccess("Assignment created successfully")
      }
      resetForm()
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save assignment")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("Are you sure you want to delete this assignment?")) return
    try {
      setError(null)
      await teacherFetch(`/v1/learning/assignments/${id}`, { method: "DELETE" })
      setSuccess("Assignment deleted")
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete assignment")
    }
  }

  async function viewSubmissions(assignmentId: string) {
    try {
      setLoadingSubmissions(true)
      setSelectedAssignment(assignmentId)
      const data = await teacherFetch<AssignmentSubmission[]>(`/v1/learning/assignments/${assignmentId}/submissions`)
      setSubmissions(data)
    } catch {
      setSubmissions([])
    } finally {
      setLoadingSubmissions(false)
    }
  }

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  const filtered = assignments.filter((a) => {
    if (selectedClassId && a.classGroupId !== selectedClassId) return false
    if (statusFilter && a.status !== statusFilter) return false
    return true
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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Assignments</h1>
          <p className="mt-1 text-sm text-muted-foreground">Create and manage assignments for your students.</p>
        </div>
        <Button
          className="gap-2"
          onClick={() => {
            resetForm()
            setShowForm(!showForm)
          }}
        >
          {showForm ? <X className="size-4" /> : <Plus className="size-4" />}
          {showForm ? "Cancel" : "Create Assignment"}
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
            <CheckCircle className="size-4 shrink-0" />
            {success}
          </div>
        </div>
      )}

      {showForm && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">
            {editingId ? "Edit Assignment" : "New Assignment"}
          </h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Title *</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Assignment title"
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Brief description..."
                rows={2}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Instructions</label>
              <textarea
                value={form.instructions}
                onChange={(e) => setForm({ ...form, instructions: e.target.value })}
                placeholder="Detailed instructions for students..."
                rows={4}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Type</label>
              <div className="relative">
                <select
                  value={form.assignmentType}
                  onChange={(e) => setForm({ ...form, assignmentType: e.target.value })}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  {assignmentTypes.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Class *</label>
              <div className="relative">
                <select
                  value={form.classGroupId}
                  onChange={(e) => {
                    const cls = classes.find((c) => c.classGroupId === e.target.value)
                    setForm({
                      ...form,
                      classGroupId: e.target.value,
                      subjectId: cls?.subjectId || "",
                    })
                  }}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  <option value="">Select class</option>
                  {classes.map((c) => (
                    <option key={c.classGroupId} value={c.classGroupId}>
                      {c.className} - {c.subjectName}
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Due Date</label>
              <input
                type="datetime-local"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Total Marks</label>
              <input
                type="number"
                value={form.totalMarks}
                onChange={(e) => setForm({ ...form, totalMarks: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Status</label>
              <div className="relative">
                <select
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                  className="h-10 w-full appearance-none rounded-lg border border-border bg-background px-3 pr-10 text-sm outline-none focus:border-ring"
                >
                  <option value="DRAFT">Draft</option>
                  <option value="PUBLISHED">Published</option>
                </select>
                <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              </div>
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Attachments (filename)</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={form.attachments}
                  onChange={(e) => setForm({ ...form, attachments: e.target.value })}
                  placeholder="e.g. assignment-file.pdf"
                  className="h-10 flex-1 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
                />
                <Button type="button" variant="outline" size="icon" className="shrink-0" title="Attach file">
                  <Paperclip className="size-4" />
                </Button>
              </div>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetForm}>Cancel</Button>
            <Button
              onClick={handleSubmit}
              disabled={submitting || !form.title.trim() || !form.classGroupId || !form.subjectId}
            >
              {submitting ? "Saving..." : editingId ? "Update" : "Create"}
            </Button>
          </div>
        </div>
      )}

      {selectedAssignment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Submissions</h2>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setSelectedAssignment(null)
                setSubmissions([])
              }}
            >
              Close
            </Button>
          </div>
          {loadingSubmissions ? (
            <div className="flex justify-center py-8">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : submissions.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No submissions yet</p>
          ) : (
            <div className="mt-4 overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    <th className="px-3 py-2">Student</th>
                    <th className="px-3 py-2">Submitted</th>
                    <th className="px-3 py-2">Marks</th>
                    <th className="px-3 py-2">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((s) => (
                    <tr key={s.id} className="border-b border-border last:border-0">
                      <td className="px-3 py-2 font-medium text-foreground">
                        {s.studentName || s.studentId.slice(0, 8)}...
                      </td>
                      <td className="px-3 py-2 text-muted-foreground">
                        {new Date(s.submittedAt).toLocaleDateString()}
                      </td>
                      <td className="px-3 py-2 text-muted-foreground">
                        {s.obtainedMarks ?? s.grade ?? "—"}
                      </td>
                      <td className="px-3 py-2">
                        <span
                          className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                            s.grade !== null && s.grade !== undefined
                              ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                              : "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400"
                          }`}
                        >
                          {s.grade !== null && s.grade !== undefined ? "GRADED" : "PENDING"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        {classes.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
            <label className="text-sm font-medium text-foreground">Filter by Class</label>
            <div className="relative mt-1">
              <select
                value={selectedClassId}
                onChange={(e) => {
                  setSelectedClassId(e.target.value)
                  setSelectedAssignment(null)
                  setSubmissions([])
                }}
                className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="">All classes</option>
                {classes.map((c) => (
                  <option key={c.classGroupId} value={c.classGroupId}>
                    {c.className} - {c.subjectName}
                  </option>
                ))}
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            </div>
          </div>
        )}
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <label className="text-sm font-medium text-foreground">Status</label>
          <div className="relative mt-1">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setSelectedAssignment(null)
                setSubmissions([])
              }}
              className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              {statusOptions.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          </div>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <FileText className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Assignments</h3>
          <p className="mt-2 text-sm text-muted-foreground">Create your first assignment to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((a) => {
            const overdue = isOverdue(a.dueDate)
            const status = a.status ? statusConfig[a.status] : null
            return (
              <div key={a.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                    <FileText className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                      {status && (
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      )}
                      {a.assignmentType && (
                        <span className="inline-flex rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                          {assignmentTypes.find((t) => t.value === a.assignmentType)?.label || a.assignmentType}
                        </span>
                      )}
                    </div>
                    {a.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                    )}
                    {a.instructions && (
                      <p className="mt-0.5 text-xs text-muted-foreground/70 line-clamp-1 italic">
                        Instructions: {a.instructions}
                      </p>
                    )}
                    <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                      {a.className && a.subjectName && (
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {a.className} · {a.subjectName}
                        </span>
                      )}
                      <span className="flex items-center gap-1">
                        <Calendar className="size-3" />
                        Total: {a.totalMarks} marks
                      </span>
                      {a.dueDate && (
                        <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                          <Clock className="size-3" />
                          Due: {new Date(a.dueDate).toLocaleDateString()}
                        </span>
                      )}
                      {(a.submissionCount !== undefined || a.totalStudents !== undefined) && (
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {a.submissionCount ?? 0}/{a.totalStudents ?? 0} submitted
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => viewSubmissions(a.id)}
                      className="gap-1"
                    >
                      <Eye className="size-3" /> Submissions
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => startEdit(a)}
                      title="Edit"
                    >
                      <Pencil className="size-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => handleDelete(a.id)}
                      title="Delete"
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
