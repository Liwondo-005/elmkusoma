"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { dashboardApi, type AttendanceSummary } from "@/lib/api"
import { BarChart3, CheckCircle, XCircle, Clock, AlertTriangle } from "lucide-react"

export default function AttendancePage() {
  const { user } = useRequireAuth()
  const [summary, setSummary] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await dashboardApi.getAttendance().catch(() => null)
      setSummary(data)
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const stats = [
    { label: "Present", value: summary?.present ?? 0, icon: CheckCircle, color: "bg-green-500/10 text-green-600" },
    { label: "Absent", value: summary?.absent ?? 0, icon: XCircle, color: "bg-red-500/10 text-red-600" },
    { label: "Late", value: summary?.late ?? 0, icon: Clock, color: "bg-yellow-500/10 text-yellow-600" },
    { label: "Excused", value: summary?.excused ?? 0, icon: AlertTriangle, color: "bg-blue-500/10 text-blue-600" },
  ]

  function getStatusStyle(status: string) {
    switch (status) {
      case "PRESENT": return "bg-green-100 text-green-700"
      case "ABSENT": return "bg-red-100 text-red-700"
      case "LATE": return "bg-yellow-100 text-yellow-700"
      case "EXCUSED": return "bg-blue-100 text-blue-700"
      default: return "bg-gray-100 text-gray-700"
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Attendance</h1>
        <p className="mt-1 text-sm text-muted-foreground">Track your attendance for this month.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs sm:col-span-2 lg:col-span-1">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-teal/10">
              <BarChart3 className="size-5 text-teal" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{summary?.attendanceRate ?? 0}%</p>
              <p className="text-xs text-muted-foreground">Attendance Rate</p>
            </div>
          </div>
        </div>
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className={`flex size-8 items-center justify-center rounded-lg ${s.color}`}>
              <s.icon className="size-4" />
            </div>
            <p className="mt-2 text-xl font-bold text-foreground">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">Attendance Records</h2>
        {!summary?.records || summary.records.length === 0 ? (
          <div className="py-12 text-center">
            <BarChart3 className="mx-auto size-12 text-muted-foreground/50" />
            <h3 className="mt-4 text-lg font-semibold text-foreground">No Records</h3>
            <p className="mt-2 text-sm text-muted-foreground">No attendance records found for this month.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">Date</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">Check In</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">Check Out</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">Remarks</th>
                </tr>
              </thead>
              <tbody>
                {summary.records.map((record) => (
                  <tr key={record.id} className="border-b border-border last:border-0">
                    <td className="px-4 py-3 font-medium text-foreground">
                      {record.date ? new Date(record.date).toLocaleDateString("en-US", {
                        weekday: "short",
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      }) : "—"}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${getStatusStyle(record.status)}`}>
                        {record.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{record.checkInTime || "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground">{record.checkOutTime || "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground">{record.remarks || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
