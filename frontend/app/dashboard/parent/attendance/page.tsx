"use client"

import { useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import Link from "next/link"
import { ArrowLeft, Loader2, CheckCircle, XCircle, Clock as ClockIcon } from "lucide-react"
import { parentApi, type AttendanceData, type ChildOverview } from "@/lib/parent-api"

export default function ParentAttendancePage() {
  const searchParams = useSearchParams()
  const childId = searchParams.get("child")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedId, setSelectedId] = useState(childId || "")
  const [attendance, setAttendance] = useState<AttendanceData | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (!selectedId && kids.length > 0) {
        setSelectedId(kids[0].studentId)
      }
    })
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setLoading(true)
    parentApi.getChildAttendance(selectedId).then(setAttendance).finally(() => setLoading(false))
  }, [selectedId])

  function statusIcon(status: string) {
    switch (status) {
      case "PRESENT": return <CheckCircle className="size-4 text-teal" />
      case "ABSENT": return <XCircle className="size-4 text-red-500" />
      case "LATE": return <ClockIcon className="size-4 text-orange" />
      default: return <CheckCircle className="size-4 text-muted-foreground" />
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/parent" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to Dashboard
      </Link>
      <h1 className="text-xl font-bold text-foreground">Attendance</h1>

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
      ) : attendance ? (
        <>
          <div className="grid gap-4 sm:grid-cols-5">
            <div className="rounded-xl border border-border bg-card p-4 text-center">
              <p className="text-2xl font-bold text-foreground">{attendance.totalDays}</p>
              <p className="text-xs text-muted-foreground">Total Days</p>
            </div>
            <div className="rounded-xl border border-border bg-card p-4 text-center">
              <p className="text-2xl font-bold text-teal">{attendance.presentDays}</p>
              <p className="text-xs text-muted-foreground">Present</p>
            </div>
            <div className="rounded-xl border border-border bg-card p-4 text-center">
              <p className="text-2xl font-bold text-red-500">{attendance.absentDays}</p>
              <p className="text-xs text-muted-foreground">Absent</p>
            </div>
            <div className="rounded-xl border border-border bg-card p-4 text-center">
              <p className="text-2xl font-bold text-orange">{attendance.lateDays}</p>
              <p className="text-xs text-muted-foreground">Late</p>
            </div>
            <div className="rounded-xl border border-border bg-card p-4 text-center">
              <p className="text-2xl font-bold text-foreground">
                {attendance.attendancePercentage != null ? `${Math.round(attendance.attendancePercentage)}%` : "—"}
              </p>
              <p className="text-xs text-muted-foreground">Rate</p>
            </div>
          </div>

          <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h2 className="text-base font-semibold text-foreground">Recent Attendance</h2>
            <div className="mt-4 space-y-2">
              {attendance.recentDays.length === 0 ? (
                <p className="py-4 text-center text-sm text-muted-foreground">No attendance records yet.</p>
              ) : (
                attendance.recentDays.map((day, i) => (
                  <div key={`${day.date}-${i}`} className="flex items-center gap-3 rounded-lg border border-border p-3">
                    {statusIcon(day.status)}
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">
                        {new Date(day.date).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })}
                      </p>
                      {day.remarks && <p className="text-xs text-muted-foreground">{day.remarks}</p>}
                    </div>
                    <span className={`text-xs font-semibold ${
                      day.status === "PRESENT" ? "text-teal" : day.status === "ABSENT" ? "text-red-500" : "text-orange"
                    }`}>
                      {day.status}
                    </span>
                  </div>
                ))
              )}
            </div>
          </section>
        </>
      ) : (
        <p className="py-12 text-center text-sm text-muted-foreground">Select a child to view attendance.</p>
      )}
    </div>
  )
}
