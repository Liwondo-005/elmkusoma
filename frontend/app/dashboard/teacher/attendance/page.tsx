"use client"

import { useEffect, useState } from "react"
import { ClipboardList, Loader2, CheckCircle, XCircle, Clock, Save, ChevronDown } from "lucide-react"
import { useAuth } from "@/lib/auth"
import type { ClassGroupInfo, StudentInClass, AttendanceRecord } from "@/lib/teacher-api"

export default function TeacherAttendancePage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<ClassGroupInfo[]>([])
  const [selectedClassId, setSelectedClassId] = useState<string>("")
  const [students, setStudents] = useState<StudentInClass[]>([])
  const [attendance, setAttendance] = useState<Record<string, string>>({})
  const [remarks, setRemarks] = useState<Record<string, string>>({})
  const [todayRecords, setTodayRecords] = useState<AttendanceRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  const today = new Date().toISOString().split("T")[0]

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
      } catch { /* empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [user?.email])

  useEffect(() => {
    if (!selectedClassId) return
    async function loadClass() {
      try {
        const { teacherApi } = await import("@/lib/teacher-api")
        const [studs, records] = await Promise.all([
          teacherApi.getStudentsByClass(selectedClassId),
          teacherApi.getAttendanceByClass(selectedClassId, today).catch(() => []),
        ])
        setStudents(studs)
        setTodayRecords(records)
        const existing: Record<string, string> = {}
        records.forEach((r) => { existing[r.studentId] = r.status })
        setAttendance(existing)
      } catch { /* empty */ }
    }
    loadClass()
  }, [selectedClassId, today])

  async function handleSave() {
    if (!selectedClassId) return
    setSaving(true)
    try {
      const { teacherApi } = await import("@/lib/teacher-api")
      const records = students.map((s) => ({
        studentId: s.id,
        status: attendance[s.id] || "ABSENT",
        remarks: remarks[s.id] || undefined,
      }))
      await teacherApi.bulkMarkAttendance({
        classGroupId: selectedClassId,
        date: today,
        records,
      })
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch { /* empty */ }
    finally { setSaving(false) }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Mark Attendance</h1>
          <p className="mt-1 text-sm text-muted-foreground">Record daily attendance for your classes.</p>
        </div>
        {selectedClassId && students.length > 0 && (
          <button
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs transition-colors hover:bg-primary/90 disabled:opacity-50"
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
            {saved ? "Saved!" : "Save Attendance"}
          </button>
        )}
      </div>

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

      {selectedClassId && students.length > 0 && (
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          <div className="border-b border-border px-4 py-3">
            <p className="text-sm font-medium text-foreground">
              {students.length} students &middot; {today}
            </p>
          </div>
          <div className="divide-y divide-border">
            {students.map((student) => (
              <div key={student.id} className="flex items-center gap-4 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{student.firstName} {student.lastName}</p>
                  <p className="text-xs text-muted-foreground">{student.admissionNumber}</p>
                </div>
                <div className="flex gap-1.5">
                  {["PRESENT", "ABSENT", "LATE", "EXCUSED"].map((status) => (
                    <button
                      key={status}
                      onClick={() => setAttendance((prev) => ({ ...prev, [student.id]: status }))}
                      className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                        attendance[student.id] === status
                          ? status === "PRESENT" ? "bg-teal text-white"
                            : status === "ABSENT" ? "bg-red-500 text-white"
                            : status === "LATE" ? "bg-orange text-white"
                            : "bg-blue-500 text-white"
                          : "border border-border text-muted-foreground hover:bg-muted"
                      }`}
                    >
                      {status === "PRESENT" && <CheckCircle className="mr-1 inline size-3" />}
                      {status === "ABSENT" && <XCircle className="mr-1 inline size-3" />}
                      {status === "LATE" && <Clock className="mr-1 inline size-3" />}
                      {status.charAt(0) + status.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {selectedClassId && students.length === 0 && !loading && (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <ClipboardList className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No students in this class</p>
        </div>
      )}
    </div>
  )
}
