"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import { Users, BookOpen, Loader2, CheckCircle, XCircle, Clock } from "lucide-react"
import type { ClassGroupInfo, StudentInClass, AttendanceSummary } from "@/lib/teacher-api"

export default function TeacherClassDetailPage() {
  const params = useParams()
  const classId = params.id as string
  const [cls, setCls] = useState<ClassGroupInfo | null>(null)
  const [students, setStudents] = useState<StudentInClass[]>([])
  const [summaries, setSummaries] = useState<AttendanceSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"students" | "attendance">("students")

  useEffect(() => {
    async function load() {
      try {
        const { teacherApi } = await import("@/lib/teacher-api")
        const [allClasses, studs, attSummaries] = await Promise.all([
          teacherApi.getClassGroups().catch(() => []),
          teacherApi.getStudentsByClass(classId).catch(() => []),
          teacherApi.getAttendanceSummary(classId).catch(() => []),
        ])
        setCls(allClasses.find((c) => c.id === classId) || null)
        setStudents(studs)
        setSummaries(attSummaries)
      } catch { /* empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [classId])

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
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {cls?.name || "Class Details"}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {cls?.gradeName} &middot; {students.length} students
        </p>
      </div>

      <div className="flex gap-2 border-b border-border">
        <button
          onClick={() => setActiveTab("students")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "students" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          <Users className="mr-1.5 inline size-4" />
          Students ({students.length})
        </button>
        <button
          onClick={() => setActiveTab("attendance")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "attendance" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          <CheckCircle className="mr-1.5 inline size-4" />
          Attendance Summary
        </button>
      </div>

      {activeTab === "students" && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {students.length === 0 ? (
            <div className="py-12 text-center">
              <Users className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No students in this class</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    <th className="px-4 py-3">Name</th>
                    <th className="px-4 py-3">Admission No.</th>
                    <th className="px-4 py-3">Email</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((s) => (
                    <tr key={s.id} className="border-b border-border last:border-0">
                      <td className="px-4 py-3 font-medium text-foreground">{s.firstName} {s.lastName}</td>
                      <td className="px-4 py-3 text-muted-foreground">{s.admissionNumber}</td>
                      <td className="px-4 py-3 text-muted-foreground">{s.email || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === "attendance" && (
        <div className="space-y-4">
          {summaries.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <Clock className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No attendance data yet</p>
            </div>
          ) : (
            <div className="rounded-2xl border border-border bg-card shadow-xs overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    <th className="px-4 py-3">Student</th>
                    <th className="px-4 py-3">Present</th>
                    <th className="px-4 py-3">Absent</th>
                    <th className="px-4 py-3">Late</th>
                    <th className="px-4 py-3">Rate</th>
                  </tr>
                </thead>
                <tbody>
                  {summaries.map((s) => (
                    <tr key={s.studentId} className="border-b border-border last:border-0">
                      <td className="px-4 py-3 font-medium text-foreground">{s.studentName}</td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 text-teal">
                          <CheckCircle className="size-3.5" /> {s.presentDays}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 text-red-500">
                          <XCircle className="size-3.5" /> {s.absentDays}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 text-orange">
                          <Clock className="size-3.5" /> {s.lateDays}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-medium text-foreground">{Math.round(s.attendancePercentage)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
