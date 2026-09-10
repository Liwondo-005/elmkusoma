"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learningApi, teacherApi, assignmentCreateApi, type Assignment, type AssignmentSubmission, type TeacherClassGroup, type CreateAssignmentRequest } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { FileText, Clock, CheckCircle, Plus, Eye, X, AlertCircle, Send } from "lucide-react"

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
  const [gradingId, setGradingId] = useState<string | null>(null)
  const [gradeForm, setGradeForm] = useState<Record<string, { grade: string; feedback: string }>>({})

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
      const formState: Record<string, { grade: string; feedback: string }> = {}
      data.forEach((s) => {
        formState[s.id] = {
          grade: s.grade !== null && s.grade !== undefined ? String(s.grade) : "",
          feedback: s.feedback || "",
        }
      })
      setGradeForm(formState)
    } catch {
      setSubmissions([])
    } finally {
      setLoadingSubmissions(false)
    }
  }

  async function handleGrade(submissionId: string) {
    const gf = gradeForm[submissionId]
    if (!gf || !gf.grade) return
    try {
      setGradingId(submissionId)
      setError(null)
      await learningApi.gradeSubmission(submissionId, Number(gf.grade), gf.feedback)
      setSuccess("Submission graded")
      if (selectedAssignment) await viewSubmissions(selectedAssignment)
      setTimeout(() => setSuccess(null), 3000)
    } catch {
      setError("Failed to grade submission")
    } finally {
      setGradingId(null)
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

  const selectedAssignmentData = assignments.find((a) => a.id === selectedAssignment)
  const gradedCount = submissions.filter((s) => s.grade !== null && s.grade !== undefined).length

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

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : assignments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <FileText className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Assignments</h3>
          <p className="mt-2 text-sm text-muted-foreground">Create your first assignment to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {assignments.map((a) => {
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
                      <span>Total: {a.totalMarks} marks</span>
                      {a.dueDate && (
                        <span className={`flex items-center gap-1 ${overdue ? "text-destructive" : ""}`}>
                          <Clock className="size-3" />
                          Due: {new Date(a.dueDate).toLocaleDateString()}
                        </span>
                      )}
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

      {selectedAssignment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-semibold text-foreground">
                Submissions {selectedAssignmentData ? `- ${selectedAssignmentData.title}` : ""}
              </h2>
              <p className="text-xs text-muted-foreground">
                {gradedCount}/{submissions.length} graded
              </p>
            </div>
            <Button variant="ghost" size="sm" onClick={() => { setSelectedAssignment(null); setSubmissions([]) }}>
              Close
            </Button>
          </div>
          {loadingSubmissions ? (
            <div className="flex justify-center py-8">
              <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : submissions.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No submissions yet</p>
          ) : (
            <div className="mt-4 space-y-3">
              {submissions.map((s) => {
                const gf = gradeForm[s.id] || { grade: "", feedback: "" }
                const isGraded = s.grade !== null && s.grade !== undefined
                const isSaving = gradingId === s.id
                return (
                  <div key={s.id} className="rounded-xl border border-border p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-foreground">Student: {s.studentId.slice(0, 8)}...</p>
                        <p className="text-xs text-muted-foreground">
                          Submitted: {new Date(s.submittedAt).toLocaleString()}
                        </p>
                      </div>
                      {isGraded && (
                        <span className="rounded-full bg-teal/10 px-2.5 py-0.5 text-xs font-medium text-teal">
                          Graded: {s.grade}
                        </span>
                      )}
                    </div>
                    {s.content && (
                      <div className="rounded-lg bg-muted/50 p-3">
                        <p className="text-xs font-medium text-muted-foreground mb-1">Submission:</p>
                        <p className="text-sm text-foreground">{s.content}</p>
                      </div>
                    )}
                    <div className="grid gap-3 sm:grid-cols-[120px_1fr_auto]">
                      <div>
                        <label className="mb-1 block text-xs font-medium text-muted-foreground">Grade</label>
                        <input
                          type="number"
                          value={gf.grade}
                          onChange={(e) => setGradeForm({ ...gradeForm, [s.id]: { ...gf, grade: e.target.value } })}
                          placeholder="0"
                          min={0}
                          className="h-9 w-full rounded-lg border border-border bg-background px-2 text-sm outline-none focus:border-ring"
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-medium text-muted-foreground">Feedback</label>
                        <input
                          type="text"
                          value={gf.feedback}
                          onChange={(e) => setGradeForm({ ...gradeForm, [s.id]: { ...gf, feedback: e.target.value } })}
                          placeholder="Optional feedback..."
                          className="h-9 w-full rounded-lg border border-border bg-background px-2 text-sm outline-none focus:border-ring"
                        />
                      </div>
                      <div className="flex items-end">
                        <Button
                          size="sm"
                          onClick={() => handleGrade(s.id)}
                          disabled={isSaving || !gf.grade}
                          className="gap-1"
                        >
                          {isSaving ? (
                            <div className="size-3 animate-spin rounded-full border-2 border-current border-t-transparent" />
                          ) : (
                            <Send className="size-3" />
                          )}
                          {isGraded ? "Update" : "Grade"}
                        </Button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
