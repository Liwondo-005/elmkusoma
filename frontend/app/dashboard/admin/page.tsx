"use client"

import { useEffect, useState } from "react"
import { Users, GraduationCap, UserCheck, Award, Clock, Loader2, BookOpen, FileText, Video, BookMarked } from "lucide-react"
import { adminApi, getInstitutionId, type DashboardResponse } from "@/lib/api"

const statCards = [
  { key: "totalStudents" as const, label: "Total Students", icon: GraduationCap, color: "text-blue-600", bg: "bg-blue-500/10" },
  { key: "totalTeachers" as const, label: "Total Teachers", icon: Users, color: "text-teal-600", bg: "bg-teal-500/10" },
  { key: "totalParents" as const, label: "Total Parents", icon: UserCheck, color: "text-purple-600", bg: "bg-purple-500/10" },
  { key: "activeStudents" as const, label: "Active Students", icon: Users, color: "text-orange-600", bg: "bg-orange-500/10" },
  { key: "certificatesIssued" as const, label: "Certificates Issued", icon: Award, color: "text-amber-600", bg: "bg-amber-500/10" },
  { key: "pendingImportJobs" as const, label: "Pending Imports", icon: Clock, color: "text-muted-foreground", bg: "bg-muted" },
]

const courseCards = [
  { key: "totalCourses" as const, label: "Total Courses", icon: BookOpen, color: "text-indigo-600", bg: "bg-indigo-500/10" },
  { key: "publishedCourses" as const, label: "Published", icon: BookMarked, color: "text-emerald-600", bg: "bg-emerald-500/10" },
  { key: "draftCourses" as const, label: "Drafts", icon: FileText, color: "text-yellow-600", bg: "bg-yellow-500/10" },
  { key: "totalModules" as const, label: "Total Modules", icon: BookOpen, color: "text-cyan-600", bg: "bg-cyan-500/10" },
  { key: "totalLessons" as const, label: "Total Lessons", icon: FileText, color: "text-pink-600", bg: "bg-pink-500/10" },
  { key: "liveClassesScheduled" as const, label: "Live Classes", icon: Video, color: "text-red-600", bg: "bg-red-500/10" },
]

export default function AdminDashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const institutionId = getInstitutionId()
    if (!institutionId) {
      setError("No institution context found. Please log in again.")
      setLoading(false)
      return
    }
    adminApi
      .getDashboard(institutionId)
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load dashboard"))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Administration</h1>
        <p className="mt-1 text-sm text-muted-foreground">Overview of your institution and all courses.</p>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      {data && (
        <>
          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">Institution Overview</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {statCards.map((card) => (
                <div key={card.key} className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                  <div className="flex items-center gap-4">
                    <div className={`flex size-12 items-center justify-center rounded-xl ${card.bg}`}>
                      <card.icon className={`size-6 ${card.color}`} />
                    </div>
                    <div>
                      <p className="text-2xl font-bold text-foreground">{data[card.key] ?? 0}</p>
                      <p className="text-xs text-muted-foreground">{card.label}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wider">Course Management</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {courseCards.map((card) => (
                <div key={card.key} className="rounded-2xl border border-border bg-card p-6 shadow-xs">
                  <div className="flex items-center gap-4">
                    <div className={`flex size-12 items-center justify-center rounded-xl ${card.bg}`}>
                      <card.icon className={`size-6 ${card.color}`} />
                    </div>
                    <div>
                      <p className="text-2xl font-bold text-foreground">{data[card.key] ?? 0}</p>
                      <p className="text-xs text-muted-foreground">{card.label}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}

      {data && data.additionalStats && Object.keys(data.additionalStats).length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="mb-4 text-sm font-semibold text-foreground">Additional Statistics</h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {Object.entries(data.additionalStats).map(([key, value]) => (
              <div key={key} className="flex items-center justify-between rounded-lg border border-border px-4 py-3">
                <span className="text-xs text-muted-foreground">{key.replace(/([A-Z])/g, " $1").trim()}</span>
                <span className="text-sm font-semibold text-foreground">{String(value)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
