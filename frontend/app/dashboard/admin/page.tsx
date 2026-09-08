"use client"

import { useEffect, useState } from "react"
import { Users, GraduationCap, UserCheck, Award, Clock, Loader2 } from "lucide-react"
import { adminApi, getInstitutionId, type DashboardResponse } from "@/lib/api"

const statCards = [
  { key: "totalStudents" as const, label: "Total Students", icon: GraduationCap, color: "text-blue-600", bg: "bg-blue-500/10" },
  { key: "totalTeachers" as const, label: "Total Teachers", icon: Users, color: "text-teal", bg: "bg-teal/10" },
  { key: "totalParents" as const, label: "Total Parents", icon: UserCheck, color: "text-purple-600", bg: "bg-purple-500/10" },
  { key: "activeStudents" as const, label: "Active Students", icon: Users, color: "text-orange", bg: "bg-orange/10" },
  { key: "certificatesIssued" as const, label: "Certificates Issued", icon: Award, color: "text-amber-600", bg: "bg-amber-500/10" },
  { key: "pendingImportJobs" as const, label: "Pending Imports", icon: Clock, color: "text-muted-foreground", bg: "bg-muted" },
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
        <p className="mt-1 text-sm text-muted-foreground">Overview of your institution.</p>
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
