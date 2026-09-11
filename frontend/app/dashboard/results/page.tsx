"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, academicApi, type StudentResult } from "@/lib/api"
import { Award, ChevronDown, ChevronUp } from "lucide-react"

interface ResolvedNames {
  termNames: Record<string, string>
  yearNames: Record<string, string>
  subjectNames: Record<string, string>
}

export default function ResultsPage() {
  const { user } = useRequireAuth()
  const [results, setResults] = useState<StudentResult[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [names, setNames] = useState<ResolvedNames>({ termNames: {}, yearNames: {}, subjectNames: {} })

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await dashboardApi.getResults().catch(() => [])
      setResults(data)

      const termNames: Record<string, string> = {}
      const yearNames: Record<string, string> = {}
      const subjectNames: Record<string, string> = {}

      const yearIds = [...new Set(data.map((r) => r.academicYearId).filter(Boolean))]
      await Promise.all(yearIds.map(async (id) => {
        try {
          const year = await academicApi.getAcademicYear(id)
          if (year) yearNames[id] = year.yearLabel
          const terms = await academicApi.getTerms(id)
          for (const t of terms) termNames[t.id] = t.name
        } catch {}
      }))

      const subjectIds = [...new Set(data.flatMap((r) => (r.subjectGrades || []).map((sg) => sg.subjectId).filter(Boolean)))]
      await Promise.all(subjectIds.map(async (id) => {
        try {
          const subject = await academicApi.getSubject(id)
          if (subject) subjectNames[id] = subject.name
        } catch {}
      }))

      setNames({ termNames, yearNames, subjectNames })
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  function resolveTerm(id: string) { return names.termNames[id] || `Term ${id?.slice(0, 8) || "N/A"}` }
  function resolveYear(id: string) { return names.yearNames[id] || `Year ${id?.slice(0, 8) || "N/A"}` }
  function resolveSubject(id: string) { return names.subjectNames[id] || id?.slice(0, 8) || "—" }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Results</h1>
        <p className="mt-1 text-sm text-muted-foreground">View your report cards and subject grades.</p>
      </div>

      {results.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Award className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Results Yet</h3>
          <p className="mt-2 text-sm text-muted-foreground">Your results will appear here once published by your teacher.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {results.map((result) => {
            const isExpanded = expandedId === result.id
            return (
              <div key={result.id} className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
                <button
                  type="button"
                  onClick={() => setExpandedId(isExpanded ? null : result.id)}
                  className="flex w-full items-center justify-between p-5 text-left hover:bg-muted/50 transition-colors"
                >
                  <div className="flex items-center gap-4">
                    <div className="flex size-12 items-center justify-center rounded-xl bg-primary/10">
                      <Award className="size-6 text-primary" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">
                        {resolveTerm(result.termId)}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {resolveYear(result.academicYearId)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <p className="text-2xl font-extrabold text-foreground">{result.averageMark?.toFixed(1) ?? "—"}</p>
                      <p className="text-xs text-muted-foreground">Average</p>
                    </div>
                    {result.classRank && (
                      <div className="text-right">
                        <p className="text-lg font-bold text-primary">#{result.classRank}</p>
                        <p className="text-xs text-muted-foreground">Rank</p>
                      </div>
                    )}
                    {isExpanded ? <ChevronUp className="size-5 text-muted-foreground" /> : <ChevronDown className="size-5 text-muted-foreground" />}
                  </div>
                </button>

                {isExpanded && (
                  <div className="border-t border-border p-5">
                    {result.subjectGrades && result.subjectGrades.length > 0 ? (
                      <div className="space-y-2">
                        <h4 className="text-sm font-semibold text-foreground mb-3">Subject Grades</h4>
                    <div className="rounded-xl border border-border overflow-hidden">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="bg-muted/50">
                            <th className="px-4 py-2.5 text-left font-medium text-muted-foreground">Subject</th>
                            <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">Marks</th>
                            <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">Grade</th>
                            <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">Points</th>
                            <th className="px-4 py-2.5 text-left font-medium text-muted-foreground">Remarks</th>
                          </tr>
                        </thead>
                        <tbody>
                          {result.subjectGrades.map((sg, i) => (
                            <tr key={i} className="border-t border-border">
                              <td className="px-4 py-2.5 font-medium text-foreground">{resolveSubject(sg.subjectId)}</td>
                              <td className="px-4 py-2.5 text-right text-foreground">{sg.marksObtained}</td>
                              <td className="px-4 py-2.5 text-right font-semibold text-foreground">{sg.grade}</td>
                              <td className="px-4 py-2.5 text-right text-foreground">{sg.gradePoints}</td>
                              <td className="px-4 py-2.5 text-muted-foreground text-xs">{sg.teacherRemarks || "—"}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                      </div>
                    ) : (
                      <p className="text-sm text-muted-foreground">No subject grades available for this term.</p>
                    )}

                    {result.remarks && (
                      <div className="mt-4 space-y-2">
                        <div className="rounded-xl bg-muted/50 p-3">
                          <p className="text-xs font-medium text-muted-foreground">Remarks</p>
                          <p className="mt-1 text-sm text-foreground">{result.remarks}</p>
                        </div>
                      </div>
                    )}
                    {result.overallGrade && (
                      <div className="mt-2">
                        <span className="text-sm font-semibold text-primary">Overall Grade: {result.overallGrade}</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
