"use client"

import { useEffect, useState } from "react"
import { FileText, Loader2, Plus, ChevronDown, Calendar, Users } from "lucide-react"
import { useAuth } from "@/lib/auth"
import type { AssignmentInfo, ClassGroupInfo, AssignmentSubmission } from "@/lib/teacher-api"

export default function TeacherAssignmentsPage() {
  const { user } = useAuth()
  const [assignments, setAssignments] = useState<AssignmentInfo[]>([])
  const [classes, setClasses] = useState<ClassGroupInfo[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [selectedAssignment, setSelectedAssignment] = useState<string | null>(null)
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loadingSubs, setLoadingSubs] = useState(false)

  const [newTitle, setNewTitle] = useState("")
  const [newDesc, setNewDesc] = useState("")
  const [newDueDate, setNewDueDate] = useState("")
  const [newTotalMarks, setNewTotalMarks] = useState("100")
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    async function load() {
      try {
        const { teacherApi } = await import("@/lib/teacher-api")
        const profileRes = await teacherApi.listTeachers(0, 50)
        const teacher = profileRes.content?.find((t) => t.email === user?.email)
        if (teacher) {
          const assigns = await teacherApi.getAssignments(teacher.id).catch(() => [])
          const assignedClassIds = [...new Set(assigns.map((a) => a.classGroupId))]
          const allClasses = await teacherApi.getClassGroups().catch(() => [])
          const filtered = allClasses.filter((c) => assignedClassIds.includes(c.id))
          setClasses(filtered.length > 0 ? filtered : allClasses.slice(0, 10))
        }
      } catch { /* empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

  useEffect(() => {
    if (!selectedClassId) { setAssignments([]); return }
    async function loadAssignments() {
      try {
        const { teacherApi } = await import("@/lib/teacher-api")
        const assigns = await teacherApi.getAssignmentsByClass(selectedClassId)
        setAssignments(assigns)
      } catch { setAssignments([]) }
    }
    loadAssignments()
  }, [selectedClassId])

  async function handleCreate() {
    if (!newTitle.trim() || !selectedClassId) return
    setCreating(true)
    try {
      const { teacherApi } = await import("@/lib/teacher-api")
      await teacherApi.createAssignment({
        title: newTitle.trim(),
        description: newDesc.trim() || undefined,
        classGroupId: selectedClassId,
        dueDate: newDueDate || undefined,
        totalMarks: parseInt(newTotalMarks) || 100,
      })
      setShowCreate(false)
      setNewTitle(""); setNewDesc(""); setNewDueDate(""); setNewTotalMarks("100")
      const assigns = await teacherApi.getAssignmentsByClass(selectedClassId)
      setAssignments(assigns)
    } catch { /* empty */ }
    finally { setCreating(false) }
  }

  async function handleViewSubmissions(assignmentId: string) {
    setSelectedAssignment(assignmentId)
    setLoadingSubs(true)
    try {
      const { teacherApi } = await import("@/lib/teacher-api")
      const subs = await teacherApi.getSubmissions(assignmentId)
      setSubmissions(subs)
    } catch { setSubmissions([]) }
    finally { setLoadingSubs(false) }
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
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Assignments</h1>
          <p className="mt-1 text-sm text-muted-foreground">Create and manage assignments for your classes.</p>
        </div>
        {selectedClassId && (
          <button
            onClick={() => setShowCreate(!showCreate)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs transition-colors hover:bg-primary/90"
          >
            <Plus className="size-4" />
            New Assignment
          </button>
        )}
      </div>

      <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
        <label className="text-sm font-medium text-foreground">Select Class</label>
        <div className="relative mt-1">
          <select
            value={selectedClassId}
            onChange={(e) => { setSelectedClassId(e.target.value); setSelectedAssignment(null); setSubmissions([]) }}
            className="w-full appearance-none rounded-lg border border-border bg-background px-3 py-2.5 pr-10 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="">Choose a class...</option>
            {classes.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {showCreate && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <h3 className="text-sm font-semibold text-foreground">Create New Assignment</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">Title</label>
              <input value={newTitle} onChange={(e) => setNewTitle(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="Assignment title" />
            </div>
            <div className="sm:col-span-2">
              <label className="text-xs font-medium text-muted-foreground">Description</label>
              <textarea value={newDesc} onChange={(e) => setNewDesc(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" rows={3} placeholder="Optional description" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Due Date</label>
              <input type="date" value={newDueDate} onChange={(e) => setNewDueDate(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Total Marks</label>
              <input type="number" value={newTotalMarks} onChange={(e) => setNewTotalMarks(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
            </div>
          </div>
          <div className="flex gap-2">
            <button onClick={handleCreate} disabled={creating || !newTitle.trim()} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50">
              {creating ? <Loader2 className="size-4 animate-spin" /> : <FileText className="size-4" />}
              Create
            </button>
            <button onClick={() => setShowCreate(false)} className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-muted-foreground hover:bg-muted">Cancel</button>
          </div>
        </div>
      )}

      {selectedAssignment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">Submissions</h3>
            <button onClick={() => { setSelectedAssignment(null); setSubmissions([]) }} className="text-xs text-muted-foreground hover:text-foreground">Close</button>
          </div>
          {loadingSubs ? (
            <div className="flex justify-center py-8"><Loader2 className="size-5 animate-spin text-muted-foreground" /></div>
          ) : submissions.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No submissions yet</p>
          ) : (
            <div className="mt-3 overflow-x-auto">
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
                      <td className="px-3 py-2 font-medium text-foreground">{s.studentName}</td>
                      <td className="px-3 py-2 text-muted-foreground">{s.submittedAt ? new Date(s.submittedAt).toLocaleDateString() : "—"}</td>
                      <td className="px-3 py-2 text-muted-foreground">{s.obtainedMarks ?? "—"}</td>
                      <td className="px-3 py-2">
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          s.status === "GRADED" ? "bg-teal/10 text-teal" : "bg-orange/10 text-orange"
                        }`}>{s.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {selectedClassId && assignments.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <FileText className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No assignments yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Create your first assignment.</p>
        </div>
      )}

      {assignments.length > 0 && (
        <div className="space-y-3">
          {assignments.map((a) => (
            <div key={a.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-semibold text-foreground">{a.title}</p>
                  {a.description && <p className="mt-1 text-xs text-muted-foreground">{a.description}</p>}
                  <div className="mt-2 flex flex-wrap gap-3 text-xs text-muted-foreground">
                    {a.subjectName && <span className="inline-flex items-center gap-1"><FileText className="size-3" />{a.subjectName}</span>}
                    <span className="inline-flex items-center gap-1"><Calendar className="size-3" />Due: {a.dueDate ? new Date(a.dueDate).toLocaleDateString() : "No due date"}</span>
                    <span className="inline-flex items-center gap-1"><Users className="size-3" />{a.submissionCount}/{a.totalStudents} submitted</span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                    a.status === "ACTIVE" ? "bg-teal/10 text-teal" : "bg-muted text-muted-foreground"
                  }`}>{a.status}</span>
                  <button onClick={() => handleViewSubmissions(a.id)} className="text-xs font-medium text-primary hover:underline">View Submissions</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
