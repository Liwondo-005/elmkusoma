"use client"

import { useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import Link from "next/link"
import { ArrowLeft, Loader2, Award } from "lucide-react"
import { parentApi, type ResultData, type ChildOverview } from "@/lib/parent-api"

export default function ParentResultsPage() {
  const searchParams = useSearchParams()
  const childId = searchParams.get("child")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedId, setSelectedId] = useState(childId || "")
  const [results, setResults] = useState<ResultData | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (!selectedId && kids.length > 0) setSelectedId(kids[0].studentId)
    })
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setLoading(true)
    parentApi.getChildResults(selectedId).then(setResults).finally(() => setLoading(false))
  }, [selectedId])

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/parent" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to Dashboard
      </Link>
      <h1 className="text-xl font-bold text-foreground">Results & Report Cards</h1>

      {children.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {children.map((c) => (
            <button
              key={c.studentId}
              onClick={() => setSelectedId(c.studentId)}
              className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                selectedId === c.studentId ? "bg-primary text-primary-foreground" : "border border-border bg-card text-foreground hover:bg-muted"
              }`}
            >
              {c.studentName}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : results && results.reportCards.length > 0 ? (
        <div className="space-y-4">
          {results.reportCards.map((rc) => (
            <div key={rc.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-semibold text-foreground">
                    {rc.term} &middot; {rc.academicYear}
                  </p>
                  <p className="text-xs text-muted-foreground">Status: {rc.status}</p>
                </div>
                {rc.overallGrade && (
                  <div className="rounded-xl bg-primary/10 px-3 py-2 text-center">
                    <p className="text-lg font-bold text-primary">{rc.overallGrade}</p>
                    <p className="text-[10px] text-muted-foreground">Grade</p>
                  </div>
                )}
              </div>

              <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {rc.averageMark != null && (
                  <div className="rounded-lg bg-muted/50 p-3 text-center">
                    <p className="text-lg font-bold text-foreground">{rc.averageMark.toFixed(1)}</p>
                    <p className="text-[10px] text-muted-foreground">Average</p>
                  </div>
                )}
                {rc.gpa != null && (
                  <div className="rounded-lg bg-muted/50 p-3 text-center">
                    <p className="text-lg font-bold text-foreground">{rc.gpa.toFixed(2)}</p>
                    <p className="text-[10px] text-muted-foreground">GPA</p>
                  </div>
                )}
                {rc.classRank != null && rc.totalStudentsInClass != null && (
                  <div className="rounded-lg bg-muted/50 p-3 text-center">
                    <p className="text-lg font-bold text-foreground">
                      {rc.classRank}/{rc.totalStudentsInClass}
                    </p>
                    <p className="text-[10px] text-muted-foreground">Class Rank</p>
                  </div>
                )}
                {rc.publishedAt && (
                  <div className="rounded-lg bg-muted/50 p-3 text-center">
                    <p className="text-sm font-bold text-foreground">
                      {new Date(rc.publishedAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                    </p>
                    <p className="text-[10px] text-muted-foreground">Published</p>
                  </div>
                )}
              </div>

              {rc.remarks && (
                <p className="mt-3 rounded-lg bg-muted/30 p-3 text-xs text-muted-foreground">
                  <span className="font-semibold text-foreground">Remarks: </span>{rc.remarks}
                </p>
              )}
            </div>
          ))}
        </div>
      ) : results && results.reportCards.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <Award className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No report cards available yet.</p>
          <p className="mt-1 text-xs text-muted-foreground">Results will appear here once published by the school.</p>
        </div>
      ) : (
        <p className="py-12 text-center text-sm text-muted-foreground">Select a child to view results.</p>
      )}
    </div>
  )
}
