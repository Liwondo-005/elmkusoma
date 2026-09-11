"use client"

import { useEffect, useState } from "react"
import { BarChart3, Loader2, ChevronDown, Save } from "lucide-react"
import { useAuth } from "@/lib/auth"
import type { ClassGroupInfo, GradingScale, ReportCard, AssignmentSubmission } from "@/lib/teacher-api"

export default function TeacherGradingPage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<ClassGroupInfo[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [scales, setScales] = useState<GradingScale[]>([])
  const [reportCards, setReportCards] = useState<ReportCard[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"scales" | "reports">("scales")

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
        const gradingScales = await teacherApi.getGradingScales().catch(() => [])
        setScales(gradingScales)
      } catch { /* empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

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
          <div className="rounded-2xl border border-dashed border-border py-12 text-center">
            <BarChart3 className="mx-auto mb-3 size-8 text-muted-foreground" />
            <p className="text-sm font-medium text-foreground">Report cards will appear here</p>
            <p className="mt-1 text-xs text-muted-foreground">Select a class to view student report cards.</p>
          </div>
        </div>
      )}
    </div>
  )
}
