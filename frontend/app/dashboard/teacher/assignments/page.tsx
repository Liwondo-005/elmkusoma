"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learningApi, teacherApi, assignmentCreateApi, type Assignment, type AssignmentSubmission, type TeacherClassGroup, type CreateAssignmentRequest } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { FileText, Clock, CheckCircle, Plus, Eye, X, AlertCircle, Loader2, ChevronDown, Calendar, Users } from "lucide-react"

export default function TeacherAssignmentsPage() {
  const { user } = useAuth()
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedAssignment, setSelectedAssignment] = useState<string | null>(null)
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loadingSubmissions, setLoadingSubmissions] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [selectedClassId, setSelectedClassId] = useState<string>("")

  const [form, setForm] = useState<CreateAssignmentRequest>({
    subjectId: "",
    classGroupId: "",
    title: "",
    description: "",
    dueDate: "",
    totalMarks: 100,
  })

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const classesData = await teacherApi.getClasses()
      setClasses(classesData)

      const allAssignments: Assignment[] = []
      for (const cls of classesData) {
        try {
          const data = await learningApi.getAssignments(cls.classGroupId)
          allAssignments.push(...data)
        } catch {
          // skip
        }
      }
      setAssignments(allAssignments)
    } catch {
      setError("Failed to load assignments")
    } finally {
      setLoading(false)
    }
  }

  async function viewSubmissions(assignmentId: string) {
    try {
      setLoadingSubmissions(true)
      setSelectedAssignment(assignmentId)
      const data = await learningApi.getSubmissions(assignmentId)
      setSubmissions(data)
    } catch {
      setSubmissions([])
    } finally {
      setLoadingSubmissions(false)
    }
  }

  async function handleCreate() {
    if (!form.title || !form.classGroupId || !form.subjectId) return
    try {
      setCreating(true)
      setError(null)
      await assignmentCreateApi.create({
        ...form,
        dueDate: form.dueDate ? new Date(form.dueDate).toISOString() : undefined,
      })
      setSuccess("Assignment created successfully")
      setShowCreate(false)
      setForm({ subjectId: "", classGroupId: "", title: "", description: "", dueDate: "", totalMarks: 100 })
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch {
      setError("Failed to create assignment")
    } finally {
      setCreating(false)
    }
  }

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  const filteredAssignments = selectedClassId
    ? assignments.filter((a) => a.classGroupId === selectedClassId)
    : assignments

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
        <Button className="gap-2" onClick={() => setShowCreate(!showCreate)}>
          {showCreate ? <X className="size-4" /> : <Plus className="size-4" />}
          {showCreate ? "Cancel" : "Create Assignment"}
        </Button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />{error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-teal/20 bg-teal/5 p-4">
          <div className="flex items-center gap-2 text-sm text-teal">
            <CheckCircle className="size-4" />{success}
          </div>
        </div>
      )}

      {classes.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <label className="text-sm font-medium text-foreground">Filter by Class</label>
          <div className="relative mt-1">
            <select
              value={selectedClassId}
              onChange={(e) => { setSelectedClassId(e.target.value); setSelectedAssignment(null); setSubmissions([]) }}
              className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="">All classes</option>
              {classes.map((c) => (
                <option key={c.classGroupId} value={c.classGroupId}>{c.className} - {c.subjectName}</option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          </div>
        </div>
      )}

      {showCreate && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">New Assignment</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Class *</label>
              <select
                value={form.classGroupId}
                onChange={(e) => {
                  const cls = classes.find(c => c.classGroupId === e.target.value)
                  setForm({ ...form, classGroupId: e.target.value, subjectId: cls?.subjectId || "" })
                }}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              >
                <option value="">Select class</option>
                {classes.map(c => (
                  <option key={c.classGroupId} value={c.classGroupId}>{c.className} - {c.subjectName}</option>
                ))}
              </select>
            </div>
            <div>
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
                placeholder="Assignment instructions..."
                rows={3}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
              />
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
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={creating || !form.title || !form.classGroupId}>
              {creating ? "Creating..." : "Create Assignment"}
            </Button>
          </div>
        </div>
      )}

      {selectedAssignment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Submissions</h2>
            <Button variant="ghost" size="sm" onClick={() => { setSelectedAssignment(null); setSubmissions([]) }}>
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
                      <td className="px-3 py-2 font-medium text-foreground">{(s as any).studentName || s.studentId.slice(0, 8)}...</td>
                      <td className="px-3 py-2 text-muted-foreground">{new Date(s.submittedAt).toLocaleDateString()}</td>
                      <td className="px-3 py-2 text-muted-foreground">{(s as any).obtainedMarks ?? s.grade ?? "—"}</td>
                      <td className="px-3 py-2">
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          s.grade !== null && s.grade !== undefined ? "bg-teal/10 text-teal" : "bg-orange/10 text-orange"
                        }`}>{s.grade !== null && s.grade !== undefined ? "GRADED" : "PENDING"}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {filteredAssignments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <FileText className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Assignments</h3>
          <p className="mt-2 text-sm text-muted-foreground">Create your first assignment to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredAssignments.map((a) => {
            const overdue = isOverdue(a.dueDate)
            return (
              <div key={a.id} className="rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm">
                <div className="flex items-center gap-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                    <FileText className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                    {a.description && (
                      <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                    )}
                    <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="inline-flex items-center gap-1"><Calendar className="size-3" />Total: {a.totalMarks} marks</span>
                      {a.dueDate && (
                        <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                          <Clock className="size-3" />
                          Due: {new Date(a.dueDate).toLocaleDateString()}
                        </span>
                      )}
                      <span className="inline-flex items-center gap-1"><Users className="size-3" />{a.submissionCount ?? 0}/{a.totalStudents ?? 0} submitted</span>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => viewSubmissions(a.id)}
                    className="gap-1 shrink-0"
                  >
                    <Eye className="size-3" /> Submissions
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
