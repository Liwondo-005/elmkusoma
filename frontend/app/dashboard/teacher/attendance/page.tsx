"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import {
  teacherApi,
  attendanceApi,
  type TeacherClassGroup,
  type TeacherStudent,
} from "@/lib/api"
import { Button } from "@/components/ui/button"
import {
  ClipboardCheck,
  Calendar,
  Users,
  Check,
  X,
  Clock,
  AlertCircle,
} from "lucide-react"

type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "EXCUSED" | null

const statusConfig: Record<
  string,
  { label: string; icon: typeof Check; className: string; activeClassName: string }
> = {
  PRESENT: {
    label: "Present",
    icon: Check,
    className: "border-border bg-background text-muted-foreground hover:bg-muted",
    activeClassName: "border-teal/40 bg-teal/10 text-teal",
  },
  ABSENT: {
    label: "Absent",
    icon: X,
    className: "border-border bg-background text-muted-foreground hover:bg-muted",
    activeClassName: "border-destructive/40 bg-destructive/10 text-destructive",
  },
  LATE: {
    label: "Late",
    icon: Clock,
    className: "border-border bg-background text-muted-foreground hover:bg-muted",
    activeClassName: "border-amber-500/40 bg-amber-500/10 text-amber-600",
  },
  EXCUSED: {
    label: "Excused",
    icon: AlertCircle,
    className: "border-border bg-background text-muted-foreground hover:bg-muted",
    activeClassName: "border-primary/40 bg-primary/10 text-primary",
  },
}

export default function TeacherAttendancePage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [students, setStudents] = useState<TeacherStudent[]>([])
  const [attendance, setAttendance] = useState<
    Record<string, AttendanceStatus>
  >({})
  const [selectedClassId, setSelectedClassId] = useState("")
  const [selectedDate, setSelectedDate] = useState(() => {
    const d = new Date()
    return d.toISOString().split("T")[0]
  })
  const [loadingClasses, setLoadingClasses] = useState(true)
  const [loadingStudents, setLoadingStudents] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    if (!user) return
    loadClasses()
  }, [user])

  useEffect(() => {
    if (selectedClassId && selectedDate) {
      loadStudents()
    }
  }, [selectedClassId, selectedDate])

  async function loadClasses() {
    try {
      setLoadingClasses(true)
      setError(null)
      const data = await teacherApi.getClasses()
      setClasses(data)
    } catch {
      setError("Failed to load classes")
      setClasses([])
    } finally {
      setLoadingClasses(false)
    }
  }

  async function loadStudents() {
    try {
      setLoadingStudents(true)
      setError(null)
      setSuccess(false)
      const allStudents = await teacherApi.getStudents()
      const filtered = allStudents.filter(
        (s) => s.classGroupId === selectedClassId
      )
      setStudents(filtered)
      const initial: Record<string, AttendanceStatus> = {}
      filtered.forEach((s) => {
        initial[s.studentId] = null
      })
      setAttendance(initial)
    } catch {
      setError("Failed to load students")
      setStudents([])
    } finally {
      setLoadingStudents(false)
    }
  }

  function setStatus(studentId: string, status: AttendanceStatus) {
    setAttendance((prev) => ({ ...prev, [studentId]: status }))
  }

  function setAllStatuses(status: AttendanceStatus) {
    const next: Record<string, AttendanceStatus> = {}
    students.forEach((s) => {
      next[s.studentId] = status
    })
    setAttendance(next)
  }

  async function submitAttendance() {
    const records = Object.entries(attendance)
      .filter(([, status]) => status !== null)
      .map(([studentId, status]) => ({
        studentId,
        status: status!,
      }))

    if (records.length === 0) {
      setError("Mark at least one student before submitting")
      return
    }

    try {
      setSubmitting(true)
      setError(null)
      await attendanceApi.bulkMark({
        classGroupId: selectedClassId,
        attendanceDate: selectedDate,
        records,
      })
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch {
      setError("Failed to submit attendance")
    } finally {
      setSubmitting(false)
    }
  }

  const markedCount = Object.values(attendance).filter(
    (s) => s !== null
  ).length
  const totalCount = students.length

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Attendance
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Mark student attendance for your classes.
        </p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-teal/20 bg-teal/5 p-4">
          <div className="flex items-center gap-2 text-sm text-teal">
            <Check className="size-4" />
            Attendance submitted successfully
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative">
          <Users className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <select
            value={selectedClassId}
            onChange={(e) => {
              setSelectedClassId(e.target.value)
              setStudents([])
              setAttendance({})
              setSuccess(false)
            }}
            disabled={loadingClasses}
            className="h-10 rounded-lg border border-border bg-background pl-9 pr-8 text-sm outline-none focus:border-ring disabled:opacity-50"
          >
            <option value="">Select a class</option>
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>
                {c.className} — {c.subjectName}
              </option>
            ))}
          </select>
        </div>

        <div className="relative">
          <Calendar className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background pl-9 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>

        {students.length > 0 && (
          <div className="flex gap-2 sm:ml-auto">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setAllStatuses("PRESENT")}
            >
              <Check className="size-3.5" />
              Mark All Present
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setAllStatuses("ABSENT")}
            >
              <X className="size-3.5" />
              Mark All Absent
            </Button>
          </div>
        )}
      </div>

      {loadingClasses ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : classes.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <ClipboardCheck className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            No Classes Found
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            You have no assigned classes to take attendance for.
          </p>
        </div>
      ) : !selectedClassId ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <ClipboardCheck className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            Select a Class
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Choose a class and date to begin marking attendance.
          </p>
        </div>
      ) : loadingStudents ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : students.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">
            No Students
          </h3>
          <p className="mt-2 text-sm text-muted-foreground">
            No students are enrolled in this class.
          </p>
        </div>
      ) : (
        <>
          <div className="flex items-center justify-between rounded-2xl border border-border bg-card px-4 py-3">
            <span className="text-sm text-muted-foreground">
              {markedCount} of {totalCount} students marked
            </span>
            <Button
              onClick={submitAttendance}
              disabled={submitting || markedCount === 0}
              size="sm"
            >
              {submitting ? (
                <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <ClipboardCheck className="size-3.5" />
              )}
              {submitting ? "Submitting..." : "Submit Attendance"}
            </Button>
          </div>

          <div className="overflow-hidden rounded-2xl border border-border bg-card">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">
                    Student
                  </th>
                  <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">
                    Admission #
                  </th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-right">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {students.map((student) => (
                  <tr key={student.studentId} className="hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <p className="font-medium text-foreground">
                        {student.fullName}
                      </p>
                      {student.gender && (
                        <p className="text-xs text-muted-foreground">
                          {student.gender}
                        </p>
                      )}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground font-mono text-xs hidden sm:table-cell">
                      {student.admissionNumber}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1.5">
                        {(
                          Object.entries(statusConfig) as [
                            string,
                            (typeof statusConfig)[string]
                          ][]
                        ).map(([key, config]) => {
                          const Icon = config.icon
                          const isActive = attendance[student.studentId] === key
                          return (
                            <button
                              key={key}
                              onClick={() =>
                                setStatus(
                                  student.studentId,
                                  isActive ? null : key as AttendanceStatus
                                )
                              }
                              title={config.label}
                              className={`inline-flex items-center gap-1 rounded-lg border px-2 py-1 text-xs font-medium transition-colors ${isActive ? config.activeClassName : config.className}`}
                            >
                              <Icon className="size-3" />
                              <span className="hidden sm:inline">
                                {config.label}
                              </span>
                            </button>
                          )
                        })}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}
