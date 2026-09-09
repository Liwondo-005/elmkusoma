"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { learningApi, type Assignment, type AssignmentSubmission } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { FileText, Clock, CheckCircle, Plus, Eye } from "lucide-react"

export default function TeacherAssignmentsPage() {
  const { user } = useAuth()
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [loading, setLoading] = useState(true)
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
      const data = await learningApi.getAssignments(user?.classGroupId || "")
      setAssignments(data)
    } catch {
      setAssignments([])
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

  async function handleGrade(submissionId: string, grade: number) {
    try {
      await learningApi.getSubmissions(selectedAssignment || "")
      loadData()
    } catch {
      // ignore
    }
  }

  function isOverdue(dueDate?: string) {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Assignments</h1>
          <p className="mt-1 text-sm text-muted-foreground">Create and manage assignments for your students.</p>
        </div>
        <Button className="gap-2">
          <Plus className="size-4" />
          Create Assignment
        </Button>
      </div>

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
            <h2 className="text-base font-semibold text-foreground">Submissions</h2>
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
            <div className="mt-4 space-y-2">
              {submissions.map((s) => (
                <div key={s.id} className="flex items-center justify-between rounded-lg border border-border p-3">
                  <div>
                    <p className="text-sm font-medium text-foreground">Student: {s.studentId.slice(0, 8)}...</p>
                    <p className="text-xs text-muted-foreground">Submitted: {new Date(s.submittedAt).toLocaleDateString()}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {s.grade !== null && s.grade !== undefined ? (
                      <span className="text-sm font-semibold text-primary">Grade: {s.grade}</span>
                    ) : (
                      <span className="text-xs text-muted-foreground">Pending</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
