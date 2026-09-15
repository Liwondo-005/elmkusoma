"use client"

import { useEffect, useState } from "react"
import { BarChart3, Loader2, ChevronDown, AlertCircle, Users, Award } from "lucide-react"
import { useAuth } from "@/lib/auth"
import type { ClassGroupInfo, GradingScale } from "@/lib/teacher-api"

interface ReportCardEntry {
  id: string
  studentId: string
  studentName: string
  className: string
  term: string
  academicYear: string
  overallGrade: string | null
  averageMark: number | null
  classRank: number | null
  totalStudentsInClass: number | null
  remarks: string | null
  status: string
  publishedAt: string | null
}

export default function TeacherGradingPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<ClassGroupInfo[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [scales, setScales] = useState<GradingScale[]>([])
  const [reportCards, setReportCards] = useState<ReportCardEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingReports, setLoadingReports] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<"scales" | "reports">("scales")

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
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
        const gradingScales = await teacherApi.getGradingScales().catch(() => [])
        setScales(gradingScales)
      } catch { /* empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

  useEffect(() => {
    if (!selectedClassId || activeTab !== "reports") return
    async function loadReports() {
      try {
        setLoadingReports(true)
        setError(null)
        const { teacherApi } = await import("@/lib/teacher-api")
        const students = await teacherApi.getStudentsByClass(selectedClassId).catch(() => [])
        if (students.length === 0) {
          setReportCards([])
          return
        }
        const allCards: ReportCardEntry[] = []
        await Promise.all(
          students.map(async (s) => {
            try {
              const cards = await teacherApi.getReportCardsByStudent(s.id)
              allCards.push(...cards.map((c) => ({
                ...c,
                studentName: `${s.firstName} ${s.lastName}`,
              })))
            } catch { /* skip */ }
          })
        )
        setReportCards(allCards.sort((a, b) => (b.averageMark ?? 0) - (a.averageMark ?? 0)))
      } catch {
        setError("Failed to load report cards")
        setReportCards([])
      } finally {
        setLoadingReports(false)
      }
    }
    loadReports()
  }, [selectedClassId, activeTab])

  const selectedClass = classes.find((c) => c.id === selectedClassId)

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

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

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
                  <option key={c.id} value={c.id}>{c.name} ({c.studentCount} students)</option>
                ))}
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            </div>
          </div>

          {loadingReports ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
              <span className="ml-2 text-sm text-muted-foreground">Loading report cards...</span>
            </div>
          ) : !selectedClassId ? (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <BarChart3 className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">Select a class to view student report cards.</p>
              <p className="mt-1 text-xs text-muted-foreground">Choose a class above to see report cards.</p>
            </div>
          ) : reportCards.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <Award className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No report cards found</p>
              <p className="mt-1 text-xs text-muted-foreground">No report cards have been generated for {selectedClass?.name} yet.</p>
            </div>
          ) : (
            <>
              <div className="grid gap-4 sm:grid-cols-3">
                <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                  <p className="text-xs font-medium text-muted-foreground">Students</p>
                  <p className="mt-1 text-2xl font-extrabold text-foreground">{reportCards.length}</p>
                </div>
                <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                  <p className="text-xs font-medium text-muted-foreground">Published</p>
                  <p className="mt-1 text-2xl font-extrabold text-foreground">{reportCards.filter((r) => r.status === "PUBLISHED").length}</p>
                </div>
                <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                  <p className="text-xs font-medium text-muted-foreground">Average Mark</p>
                  <p className="mt-1 text-2xl font-extrabold text-foreground">
                    {reportCards.length > 0 ? Math.round(reportCards.reduce((sum, r) => sum + (r.averageMark ?? 0), 0) / reportCards.length) : 0}%
                  </p>
                </div>
              </div>

              <div className="rounded-2xl border border-border bg-card shadow-xs">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        <th className="px-4 py-3">Student</th>
                        <th className="px-4 py-3">Average</th>
                        <th className="px-4 py-3">Grade</th>
                        <th className="px-4 py-3">Rank</th>
                        <th className="px-4 py-3">Status</th>
                        <th className="px-4 py-3">Remarks</th>
                      </tr>
                    </thead>
                    <tbody>
                      {reportCards.map((card) => (
                        <tr key={card.id} className="border-b border-border last:border-0">
                          <td className="px-4 py-3 font-medium text-foreground">{card.studentName}</td>
                          <td className="px-4 py-3">
                            <span className={`font-semibold ${card.averageMark != null && card.averageMark >= 70 ? "text-green-600" : card.averageMark != null && card.averageMark >= 50 ? "text-foreground" : "text-red-600"}`}>
                              {card.averageMark != null ? `${card.averageMark}%` : "—"}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                              {card.overallGrade || "—"}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-muted-foreground">
                            {card.classRank != null ? `${card.classRank}/${card.totalStudentsInClass || "?"}` : "—"}
                          </td>
                          <td className="px-4 py-3">
                            <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                              card.status === "PUBLISHED" ? "bg-green-100 text-green-700" :
                              card.status === "DRAFT" ? "bg-gray-100 text-gray-700" :
                              "bg-orange-100 text-orange-700"
                            }`}>
                              {card.status}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-muted-foreground truncate max-w-[200px]">{card.remarks || "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  )
}
