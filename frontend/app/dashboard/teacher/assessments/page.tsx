"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { assessmentApi, type Assessment, type AssessmentResult } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { PenTool, Plus, Eye, BarChart3 } from "lucide-react"

export default function TeacherAssessmentsPage() {
  const { user } = useAuth()
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedAssessment, setSelectedAssessment] = useState<string | null>(null)
  const [results, setResults] = useState<AssessmentResult[]>([])
  const [loadingResults, setLoadingResults] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await assessmentApi.getByClass(user?.classGroupId || "")
      setAssessments(data)
    } catch {
      setAssessments([])
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

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Assessments</h1>
          <p className="mt-1 text-sm text-muted-foreground">Create and manage quizzes and assessments.</p>
        </div>
        <Button className="gap-2">
          <Plus className="size-4" />
          Create Assessment
        </Button>
      </div>

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
