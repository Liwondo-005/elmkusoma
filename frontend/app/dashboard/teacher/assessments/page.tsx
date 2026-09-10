"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { assessmentApi, assessmentCreateApi, teacherApi, type Assessment, type AssessmentResult, type TeacherClassGroup, type CreateAssessmentRequest } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { PenTool, Plus, Eye, BarChart3, X, AlertCircle, CheckCircle } from "lucide-react"

export default function TeacherAssessmentsPage() {
  const { user } = useAuth()
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedAssessment, setSelectedAssessment] = useState<string | null>(null)
  const [results, setResults] = useState<AssessmentResult[]>([])
  const [loadingResults, setLoadingResults] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [form, setForm] = useState<CreateAssessmentRequest>({
    subjectId: "",
    classGroupId: "",
    title: "",
    description: "",
    totalMarks: 100,
    passMarks: 50,
    timeLimitMinutes: 60,
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

      const allAssessments: Assessment[] = []
      for (const cls of classesData) {
        try {
          const data = await assessmentApi.getByClass(cls.classGroupId)
          allAssessments.push(...data)
        } catch {
          // skip
        }
      }
      setAssessments(allAssessments)
    } catch {
      setError("Failed to load assessments")
    } finally {
      setLoading(false)
    }
  }

  async function viewResults(assessmentId: string) {
    try {
      setLoadingResults(true)
      setSelectedAssessment(assessmentId)
      const data = await assessmentApi.getResults(assessmentId)
      setResults(data)
    } catch {
      setResults([])
    } finally {
      setLoadingResults(false)
    }
  }

  async function handleCreate() {
    if (!form.title || !form.classGroupId || !form.subjectId) return
    try {
      setCreating(true)
      setError(null)
      await assessmentCreateApi.create(form)
      setSuccess("Assessment created successfully")
      setShowCreate(false)
      setForm({ subjectId: "", classGroupId: "", title: "", description: "", totalMarks: 100, passMarks: 50, timeLimitMinutes: 60 })
      loadData()
      setTimeout(() => setSuccess(null), 3000)
    } catch {
      setError("Failed to create assessment")
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Assessments</h1>
          <p className="mt-1 text-sm text-muted-foreground">Create and manage quizzes and assessments.</p>
        </div>
        <Button className="gap-2" onClick={() => setShowCreate(!showCreate)}>
          {showCreate ? <X className="size-4" /> : <Plus className="size-4" />}
          {showCreate ? "Cancel" : "Create Assessment"}
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
          <h2 className="text-base font-semibold text-foreground">New Assessment</h2>
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
                placeholder="Assessment title"
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Assessment instructions..."
                rows={3}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring resize-none"
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
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Pass Marks</label>
              <input
                type="number"
                value={form.passMarks}
                onChange={(e) => setForm({ ...form, passMarks: Number(e.target.value) })}
                min={0}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Time Limit (minutes)</label>
              <input
                type="number"
                value={form.timeLimitMinutes}
                onChange={(e) => setForm({ ...form, timeLimitMinutes: Number(e.target.value) })}
                min={1}
                className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={creating || !form.title || !form.classGroupId}>
              {creating ? "Creating..." : "Create Assessment"}
            </Button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : assessments.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <PenTool className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Assessments</h3>
          <p className="mt-2 text-sm text-muted-foreground">Create your first assessment to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {assessments.map((a) => (
            <div key={a.id} className="rounded-2xl border border-border bg-card p-4 transition-all hover:shadow-sm">
              <div className="flex items-center gap-4">
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 shrink-0">
                  <PenTool className="size-5 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                  {a.description && (
                    <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                  )}
                  <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                    <span>{a.totalMarks} marks</span>
                    <span>Pass: {a.passMarks} marks</span>
                    {a.timeLimitMinutes && <span>{a.timeLimitMinutes} min</span>}
                  </div>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => viewResults(a.id)}
                  className="gap-1 shrink-0"
                >
                  <BarChart3 className="size-3" /> Results
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedAssessment && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Assessment Results</h2>
            <Button variant="ghost" size="sm" onClick={() => { setSelectedAssessment(null); setResults([]) }}>
              Close
            </Button>
          </div>
          {loadingResults ? (
            <div className="flex justify-center py-8">
              <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : results.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No results yet</p>
          ) : (
            <div className="mt-4 overflow-hidden rounded-xl border border-border">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-border bg-muted/50">
                  <tr>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Student</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Score</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Status</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Graded</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {results.map((r) => (
                    <tr key={r.id}>
                      <td className="px-4 py-3 font-medium text-foreground">{r.studentId.slice(0, 8)}...</td>
                      <td className="px-4 py-3 text-foreground">{r.totalScore}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${r.isPassed ? "bg-teal/10 text-teal" : "bg-destructive/10 text-destructive"}`}>
                          {r.isPassed ? "Passed" : "Failed"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {r.gradedAt ? new Date(r.gradedAt).toLocaleDateString() : "Pending"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
