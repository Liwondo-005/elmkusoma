"use client"

import { useEffect, useState } from "react"
import { BarChart3, Loader2, ChevronDown } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { teacherFetch, type ClassGroupInfo, type GradingScale, type ReportCard } from "@/lib/teacher-api"

export default function TeacherGradingPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<ClassGroupInfo[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [scales, setScales] = useState<GradingScale[]>([])
  const [reportCards, setReportCards] = useState<ReportCard[]>([])
  const [loading, setLoading] = useState(true)
  const [reportLoading, setReportLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<"scales" | "reports">("scales")

  useEffect(() => {
    async function load() {
      try {
        const profileRes = await teacherFetch<{ content: { id: string; email: string }[] }>("/v1/teachers?page=0&size=50")
        const teacher = profileRes.content?.find((t) => t.email === user?.email)
        if (teacher) {
          const assigns = await teacherFetch<{ classGroupId: string }[]>(`/v1/teachers/${teacher.id}/assignments`).catch(() => [])
          const assignedClassIds = [...new Set(assigns.map((a) => a.classGroupId))]
          const allClasses = await teacherFetch<ClassGroupInfo[]>("/v1/academic/class-groups").catch(() => [])
          const filtered = allClasses.filter((c) => assignedClassIds.includes(c.id))
          setClasses(filtered.length > 0 ? filtered : allClasses.slice(0, 10))
        }
        const gradingScales = await teacherFetch<GradingScale[]>("/v1/grading/scales").catch(() => [])
        setScales(gradingScales)
      } catch { /* empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

  useEffect(() => {
    if (activeTab !== "reports") return
    setReportCards([])
    if (!selectedClassId) return

    let cancelled = false
    async function loadReportCards() {
      setReportLoading(true)
      try {
        const students = await teacherFetch<{ id: string }[]>(`/v1/students?classId=${selectedClassId}`).catch(() => [])
        const allCards: ReportCard[] = []
        for (const s of students.slice(0, 20)) {
          const cards = await teacherFetch<ReportCard[]>(`/v1/grading/report-cards/student/${s.id}`).catch(() => [])
          allCards.push(...cards)
        }
        if (!cancelled) setReportCards(allCards)
      } catch { if (!cancelled) setReportCards([]) }
      finally { if (!cancelled) setReportLoading(false) }
    }
    loadReportCards()
    return () => { cancelled = true }
  }, [activeTab, selectedClassId])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Grading</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage grading scales and view report cards.</p>
      </div>

      <div className="flex gap-2 border-b border-border">
        <button
          onClick={() => setActiveTab("scales")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "scales" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          Grading Scales ({scales.length})
        </button>
        <button
          onClick={() => setActiveTab("reports")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "reports" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          Report Cards
        </button>
      </div>

      {activeTab === "scales" && (
        <div className="space-y-4">
          {scales.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <BarChart3 className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No grading scales configured</p>
              <p className="mt-1 text-xs text-muted-foreground">Contact your admin to set up grading scales.</p>
            </div>
          ) : (
            scales.map((scale) => (
              <div key={scale.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-semibold text-foreground">{scale.name}</p>
                    {scale.description && <p className="mt-0.5 text-xs text-muted-foreground">{scale.description}</p>}
                    <p className="mt-1 text-xs text-muted-foreground">Range: {scale.minMark} - {scale.maxMark}</p>
                  </div>
                </div>
                {scale.gradeBoundaries.length > 0 && (
                  <div className="mt-4 overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                          <th className="px-3 py-2">Grade</th>
                          <th className="px-3 py-2">Min</th>
                          <th className="px-3 py-2">Max</th>
                          <th className="px-3 py-2">GPA</th>
                          <th className="px-3 py-2">Remarks</th>
                        </tr>
                      </thead>
                      <tbody>
                        {scale.gradeBoundaries.map((gb) => (
                          <tr key={gb.id} className="border-b border-border last:border-0">
                            <td className="px-3 py-2 font-semibold text-foreground">{gb.grade}</td>
                            <td className="px-3 py-2 text-muted-foreground">{gb.minMark}</td>
                            <td className="px-3 py-2 text-muted-foreground">{gb.maxMark}</td>
                            <td className="px-3 py-2 text-muted-foreground">{gb.gpaPoints ?? "—"}</td>
                            <td className="px-3 py-2 text-muted-foreground">{gb.remarks || "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === "reports" && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
            <label className="text-sm font-medium text-foreground">Select Class</label>
            <div className="relative mt-1">
              <select
                value={selectedClassId}
                onChange={(e) => setSelectedClassId(e.target.value)}
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

          {reportLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : reportCards.length > 0 ? (
            <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    <th className="px-4 py-3">Student</th>
                    <th className="px-4 py-3">Term</th>
                    <th className="px-4 py-3">Average</th>
                    <th className="px-4 py-3">Grade</th>
                    <th className="px-4 py-3">Rank</th>
                    <th className="px-4 py-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {reportCards.map((rc) => (
                    <tr key={rc.id} className="border-b border-border last:border-0">
                      <td className="px-4 py-3 font-medium text-foreground">{rc.studentName}</td>
                      <td className="px-4 py-3 text-muted-foreground">{rc.term || "—"}</td>
                      <td className="px-4 py-3 text-muted-foreground">{rc.averageMark != null ? `${rc.averageMark}%` : "—"}</td>
                      <td className="px-4 py-3 text-muted-foreground">{rc.overallGrade || "—"}</td>
                      <td className="px-4 py-3 text-muted-foreground">{rc.classRank != null ? `#${rc.classRank}` : "—"}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                          rc.status === "PUBLISHED" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"
                        }`}>{rc.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : selectedClassId ? (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <BarChart3 className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No report cards found</p>
              <p className="mt-1 text-xs text-muted-foreground">Report cards for this class have not been generated yet.</p>
            </div>
          ) : (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <BarChart3 className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">Select a class to view student report cards.</p>
              <p className="mt-1 text-xs text-muted-foreground">Choose a class above to see report cards.</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
