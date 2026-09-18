"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, academicApi, type StudentResult } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { Award, ChevronDown, ChevronUp, Star, TrendingUp, BookOpen } from "lucide-react"

interface ResolvedNames {
  termNames: Record<string, string>
  yearNames: Record<string, string>
  subjectNames: Record<string, string>
}

function getGradeColor(grade: string) {
  const g = grade?.toUpperCase()?.trim()
  if (g === "A" || g === "A+" || g === "A-" || g === "B" || g === "B+" || g === "B-") {
    return "bg-green-100 text-green-700 border-green-200"
  }
  if (g === "C" || g === "C+" || g === "C-") {
    return "bg-amber-100 text-amber-700 border-amber-200"
  }
  return "bg-red-100 text-red-700 border-red-200"
}

function getGradeRingColor(grade: string) {
  const g = grade?.toUpperCase()?.trim()
  if (g === "A" || g === "A+" || g === "A-" || g === "B" || g === "B+" || g === "B-") {
    return "text-green-600"
  }
  if (g === "C" || g === "C+" || g === "C-") {
    return "text-amber-600"
  }
  return "text-red-600"
}

function getGradeBg(grade: string) {
  const g = grade?.toUpperCase()?.trim()
  if (g === "A" || g === "A+" || g === "A-" || g === "B" || g === "B+" || g === "B-") {
    return "bg-green-500"
  }
  if (g === "C" || g === "C+" || g === "C-") {
    return "bg-amber-500"
  }
  return "bg-red-500"
}

export default function ResultsPage() {
  const { user } = useRequireAuth()
  const [results, setResults] = useState<StudentResult[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [names, setNames] = useState<ResolvedNames>({ termNames: {}, yearNames: {}, subjectNames: {} })

  const isPrimary = user?.learningLevel?.toUpperCase() === "PRIMARY"

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

  if (isPrimary) {
    return <PrimaryResultsView results={results} names={names} resolveTerm={resolveTerm} resolveYear={resolveYear} resolveSubject={resolveSubject} expandedId={expandedId} setExpandedId={setExpandedId} />
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

function PrimaryResultsView({
  results,
  names,
  resolveTerm,
  resolveYear,
  resolveSubject,
  expandedId,
  setExpandedId,
}: {
  results: StudentResult[]
  names: ResolvedNames
  resolveTerm: (id: string) => string
  resolveYear: (id: string) => string
  resolveSubject: (id: string) => string
  expandedId: string | null
  setExpandedId: (id: string | null) => void
}) {
  const totalSubjects = new Set(
    results.flatMap((r) => (r.subjectGrades || []).map((sg) => sg.subjectId))
  ).size

  const avgScore =
    results.length > 0
      ? results.reduce((sum, r) => sum + (r.averageMark ?? 0), 0) / results.length
      : 0

  const bestSubject = (() => {
    const subjectAvgs: Record<string, { name: string; avg: number }> = {}
    for (const r of results) {
      for (const sg of r.subjectGrades || []) {
        const name = names.subjectNames[sg.subjectId] || sg.subjectId?.slice(0, 8) || "—"
        if (!subjectAvgs[sg.subjectId]) {
          subjectAvgs[sg.subjectId] = { name, avg: 0, count: 0 } as any
        }
        ;(subjectAvgs[sg.subjectId] as any).avg += sg.marksObtained
        ;(subjectAvgs[sg.subjectId] as any).count += 1
      }
    }
    let best: { name: string; avg: number } | null = null
    for (const entry of Object.values(subjectAvgs) as any[]) {
      const a = entry.avg / entry.count
      if (!best || a > best.avg) best = { name: entry.name, avg: a }
    }
    return best
  })()

  const termsCompleted = results.length

  if (results.length === 0) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-amber-500/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
              <Award className="size-6 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">My Results</h1>
              <p className="text-sm text-muted-foreground">See how well you did!</p>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-12 text-center shadow-xs">
          <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-primary/10">
            <Award className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Results Yet!</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Keep learning and your results will appear here soon. You are doing great!
          </p>
          <div className="mt-4 flex items-center justify-center gap-1 text-amber-500">
            <Star className="size-4 fill-current" />
            <Star className="size-4 fill-current" />
            <Star className="size-4 fill-current" />
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Gradient Header */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/5 via-card to-amber-500/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
            <Award className="size-6 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">My Results</h1>
            <p className="text-sm text-muted-foreground">See how well you did!</p>
          </div>
        </div>
      </div>

      {/* Stats Row */}
      <div className="grid gap-4 grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
              <BookOpen className="size-5 text-blue-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{totalSubjects}</p>
              <p className="text-xs text-muted-foreground">Total Subjects</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <TrendingUp className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{avgScore.toFixed(1)}</p>
              <p className="text-xs text-muted-foreground">Average Score</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Star className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-lg font-extrabold text-foreground truncate">{bestSubject?.name || "—"}</p>
              <p className="text-xs text-muted-foreground">Best Subject</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-purple-50">
              <Award className="size-5 text-purple-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{termsCompleted}</p>
              <p className="text-xs text-muted-foreground">Terms Completed</p>
            </div>
          </div>
        </div>
      </div>

      {/* Report Cards */}
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
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10">
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
                    <p className="text-2xl font-extrabold text-foreground">
                      {result.averageMark?.toFixed(1) ?? "—"}
                    </p>
                    <p className="text-xs text-muted-foreground">Average</p>
                  </div>
                  {result.overallGrade && (
                    <div className="text-right">
                      <span className={`inline-block rounded-lg px-3 py-1 text-sm font-bold border ${getGradeColor(result.overallGrade)}`}>
                        {result.overallGrade}
                      </span>
                      <p className="text-xs text-muted-foreground mt-0.5">Grade</p>
                    </div>
                  )}
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
                    <div className="space-y-3">
                      <h4 className="text-sm font-semibold text-foreground">Subject Grades</h4>
                      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                        {result.subjectGrades.map((sg, i) => (
                          <div
                            key={i}
                            className="rounded-xl border border-border bg-muted/30 p-4 space-y-2"
                          >
                            <div className="flex items-center justify-between">
                              <p className="text-sm font-semibold text-foreground truncate">
                                {resolveSubject(sg.subjectId)}
                              </p>
                              <span className={`flex size-7 items-center justify-center rounded-lg text-xs font-bold border ${getGradeColor(sg.grade)}`}>
                                {sg.grade}
                              </span>
                            </div>
                            <div className="flex items-baseline gap-2">
                              <span className="text-2xl font-extrabold text-foreground">
                                {sg.marksObtained}
                              </span>
                              <span className="text-xs text-muted-foreground">marks</span>
                            </div>
                            <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all ${getGradeBg(sg.grade)}`}
                                style={{ width: `${Math.min(100, Math.max(0, sg.marksObtained))}%` }}
                              />
                            </div>
                            {sg.teacherRemarks && (
                              <p className="text-xs text-muted-foreground italic leading-snug">
                                &ldquo;{sg.teacherRemarks}&rdquo;
                              </p>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground">No subject grades available for this term.</p>
                  )}

                  {result.overallGrade && (
                    <div className="mt-4 flex items-center gap-2 rounded-xl bg-muted/50 p-3">
                      <span className={`size-3 rounded-full ${getGradeBg(result.overallGrade)}`} />
                      <span className="text-sm font-semibold text-foreground">
                        Overall Grade: {result.overallGrade}
                      </span>
                      {result.classRank && (
                        <span className="text-sm text-muted-foreground ml-auto">
                          Class Rank: #{result.classRank}
                        </span>
                      )}
                    </div>
                  )}

                  {result.remarks && (
                    <div className="mt-3 rounded-xl bg-primary/5 border border-primary/10 p-3">
                      <p className="text-xs font-medium text-muted-foreground mb-1">Teacher Remarks</p>
                      <p className="text-sm text-foreground">{result.remarks}</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
