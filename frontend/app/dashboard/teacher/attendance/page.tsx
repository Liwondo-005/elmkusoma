"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import {
  teacherApi,
  attendanceApi,
  type TeacherClassGroup,
  type TeacherStudent,
} from "@/lib/api"
import { appFetch } from "@/lib/fetch"
import { Button } from "@/components/ui/button"
import {
  ClipboardCheck,
  ClipboardList,
  Calendar,
  Users,
  Check,
  X,
  Clock,
  AlertCircle,
  ChevronDown,
  History,
} from "lucide-react"

type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "EXCUSED" | null

interface AttendanceRecord {
  id: string
  studentId: string
  studentName?: string
  attendanceDate: string
  status: string
  remarks?: string
}

interface AttendanceSummary {
  studentId: string
  studentName?: string
  totalDays: number
  presentDays: number
  absentDays: number
  lateDays: number
  excusedDays: number
  attendanceRate: number
}

interface MarkTabProps {
  classes: TeacherClassGroup[]
  students: TeacherStudent[]
  attendance: Record<string, AttendanceStatus>
  selectedClassId: string
  selectedDate: string
  loadingClasses: boolean
  loadingStudents: boolean
  submitting: boolean
  success: boolean
  markedCount: number
  totalCount: number
  setSelectedClassId: (id: string) => void
  setSelectedDate: (date: string) => void
  setStatus: (studentId: string, status: AttendanceStatus) => void
  setAllStatuses: (status: AttendanceStatus) => void
  submitAttendance: () => void
}

function MarkAttendanceTab({
  classes, students, attendance, selectedClassId, selectedDate,
  loadingClasses, loadingStudents, submitting, success,
  markedCount, totalCount, setSelectedClassId, setSelectedDate,
  setStatus, setAllStatuses, submitAttendance,
}: MarkTabProps) {
  const t = useTranslations("teacher")
  const ts = useTranslations("status")

  const statusConfig: Record<
    string,
    { label: string; icon: typeof Check; className: string; activeClassName: string }
  > = {
    PRESENT: {
      label: ts("present"),
      icon: Check,
      className: "border-border bg-background text-muted-foreground hover:bg-muted",
      activeClassName: "border-teal/40 bg-teal/10 text-teal",
    },
    ABSENT: {
      label: ts("absent"),
      icon: X,
      className: "border-border bg-background text-muted-foreground hover:bg-muted",
      activeClassName: "border-destructive/40 bg-destructive/10 text-destructive",
    },
    LATE: {
      label: ts("late"),
      icon: Clock,
      className: "border-border bg-background text-muted-foreground hover:bg-muted",
      activeClassName: "border-amber-500/40 bg-amber-500/10 text-amber-600",
    },
    EXCUSED: {
      label: ts("excused"),
      icon: AlertCircle,
      className: "border-border bg-background text-muted-foreground hover:bg-muted",
      activeClassName: "border-primary/40 bg-primary/10 text-primary",
    },
  }

  return (
    <>
      {success && (
        <div className="rounded-2xl border border-teal/20 bg-teal/5 p-4">
          <div className="flex items-center gap-2 text-sm text-teal">
            <Check className="size-4" />
            {t("attendance.submitSuccess")}
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
            }}
            disabled={loadingClasses}
            className="h-10 rounded-lg border border-border bg-background pl-9 pr-8 text-sm outline-none focus:border-ring disabled:opacity-50"
          >
            <option value="">{t("attendance.selectClassOption")}</option>
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
            <Button variant="outline" size="sm" onClick={() => setAllStatuses("PRESENT")}>
              <Check className="size-3.5" /> {t("attendance.markAllPresent")}
            </Button>
            <Button variant="outline" size="sm" onClick={() => setAllStatuses("ABSENT")}>
              <X className="size-3.5" /> {t("attendance.markAllAbsent")}
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
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("schedule.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("attendance.noClassesDesc")}</p>
        </div>
      ) : !selectedClassId ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <ClipboardCheck className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("gradebook.selectClass")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("attendance.selectClassDesc")}</p>
        </div>
      ) : loadingStudents ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : students.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("gradebook.noStudents")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("gradebook.noStudentsDesc")}</p>
        </div>
      ) : (
        <>
          <div className="flex items-center justify-between rounded-2xl border border-border bg-card px-4 py-3">
            <span className="text-sm text-muted-foreground">{t("attendance.markedCount", { marked: markedCount, total: totalCount })}</span>
            <Button onClick={submitAttendance} disabled={submitting || markedCount === 0} size="sm">
              {submitting ? (
                <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              ) : (
                <ClipboardCheck className="size-3.5" />
              )}
              {submitting ? t("attendance.submitting") : t("attendance.submitAttendance")}
            </Button>
          </div>

          <div className="overflow-hidden rounded-2xl border border-border bg-card">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("gradebook.colStudent")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">{t("students.colAdmission")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-right">{t("students.colStatus")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {students.map((student) => (
                  <tr key={student.studentId} className="hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <p className="font-medium text-foreground">{student.fullName}</p>
                      {student.gender && <p className="text-xs text-muted-foreground">{student.gender}</p>}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground font-mono text-xs hidden sm:table-cell">{student.admissionNumber}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1.5">
                        {(Object.entries(statusConfig) as [string, (typeof statusConfig)[string]][]).map(([key, config]) => {
                          const Icon = config.icon
                          const isActive = attendance[student.studentId] === key
                          return (
                            <button
                              key={key}
                              onClick={() => setStatus(student.studentId, isActive ? null : key as AttendanceStatus)}
                              title={config.label}
                              className={`inline-flex items-center gap-1 rounded-lg border px-2 py-1 text-xs font-medium transition-colors ${isActive ? config.activeClassName : config.className}`}
                            >
                              <Icon className="size-3" />
                              <span className="hidden sm:inline">{config.label}</span>
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
    </>
  )
}

interface HistoryTabProps {
  classes: TeacherClassGroup[]
  historyClassId: string
  historyStartDate: string
  historyEndDate: string
  historyRecords: AttendanceRecord[]
  historySummary: AttendanceSummary[]
  loadingHistory: boolean
  setHistoryClassId: (id: string) => void
  setHistoryStartDate: (date: string) => void
  setHistoryEndDate: (date: string) => void
  loadHistory: () => void
}

function HistoryTab({
  classes, historyClassId, historyStartDate, historyEndDate,
  historyRecords, historySummary, loadingHistory,
  setHistoryClassId, setHistoryStartDate, setHistoryEndDate, loadHistory,
}: HistoryTabProps) {
  const t = useTranslations("teacher")
  const tc = useTranslations("common")
  const ts = useTranslations("status")

  function historyStatusLabel(status: string) {
    switch (status) {
      case "PRESENT": return ts("present")
      case "ABSENT": return ts("absent")
      case "LATE": return ts("late")
      case "EXCUSED": return ts("excused")
      default: return status
    }
  }

  return (
    <>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="flex items-center gap-2">
          <Calendar className="size-4 text-muted-foreground" />
          <label className="text-sm font-medium">{t("attendance.classLabel")}</label>
          <select
            value={historyClassId}
            onChange={(e) => setHistoryClassId(e.target.value)}
            className="rounded-lg border border-border bg-background px-3 py-1.5 text-sm"
          >
            <option value="">{t("lessons.selectClassOption")}</option>
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>{c.className}</option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium">{t("attendance.fromLabel")}</label>
          <input type="date" value={historyStartDate} onChange={(e) => setHistoryStartDate(e.target.value)} className="rounded-lg border border-border bg-background px-3 py-1.5 text-sm" />
          <label className="text-sm font-medium">{t("attendance.toLabel")}</label>
          <input type="date" value={historyEndDate} onChange={(e) => setHistoryEndDate(e.target.value)} className="rounded-lg border border-border bg-background px-3 py-1.5 text-sm" />
        </div>
        <Button onClick={loadHistory} disabled={!historyClassId || loadingHistory} size="sm">
          {loadingHistory ? (
            <div className="size-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
          ) : (
            <History className="size-3.5" />
          )}
          {loadingHistory ? tc("loading") : t("attendance.viewHistory")}
        </Button>
      </div>

      {historySummary.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-4">
          <h3 className="text-sm font-semibold text-foreground mb-3">{t("attendance.historySummaryTitle")}</h3>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
            <div className="text-center">
              <p className="text-2xl font-bold text-foreground">{historySummary.reduce((s, r) => s + r.presentDays, 0)}</p>
              <p className="text-xs text-muted-foreground">{ts("present")}</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-destructive">{historySummary.reduce((s, r) => s + r.absentDays, 0)}</p>
              <p className="text-xs text-muted-foreground">{ts("absent")}</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-yellow-600">{historySummary.reduce((s, r) => s + r.lateDays, 0)}</p>
              <p className="text-xs text-muted-foreground">{ts("late")}</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-muted-foreground">{historySummary.reduce((s, r) => s + r.excusedDays, 0)}</p>
              <p className="text-xs text-muted-foreground">{ts("excused")}</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-teal">
                {historySummary.length > 0 ? Math.round(historySummary.reduce((s, r) => s + r.attendanceRate, 0) / historySummary.length) : 0}%
              </p>
              <p className="text-xs text-muted-foreground">{t("attendance.avgRate")}</p>
            </div>
          </div>
        </div>
      )}

      {historyRecords.length > 0 && (
        <div className="rounded-2xl border border-border bg-card overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 font-medium text-muted-foreground text-left">{t("gradebook.colStudent")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground text-left hidden sm:table-cell">{t("students.colAdmission")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground text-left">{t("attendance.colDate")}</th>
                <th className="px-4 py-3 font-medium text-muted-foreground text-left">{t("students.colStatus")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {historyRecords.map((record) => (
                <tr key={record.id} className="hover:bg-muted/30">
                  <td className="px-4 py-3 font-medium text-foreground">{record.studentName || record.studentId}</td>
                  <td className="px-4 py-3 text-muted-foreground font-mono text-xs hidden sm:table-cell">{record.studentId.slice(0, 8)}</td>
                  <td className="px-4 py-3 text-muted-foreground">{record.attendanceDate}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${
                      record.status === "PRESENT" ? "bg-teal/10 text-teal" :
                      record.status === "ABSENT" ? "bg-destructive/10 text-destructive" :
                      record.status === "LATE" ? "bg-yellow-500/10 text-yellow-600" :
                      "bg-muted text-muted-foreground"
                    }`}>
                      {historyStatusLabel(record.status)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {historyClassId && !loadingHistory && historyRecords.length === 0 && (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <ClipboardList className="mx-auto size-8 text-muted-foreground/40" />
          <p className="mt-2 text-sm text-muted-foreground">{t("attendance.noHistoryRecords")}</p>
        </div>
      )}

      {!historyClassId && (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Calendar className="mx-auto size-8 text-muted-foreground/40" />
          <p className="mt-2 text-sm text-muted-foreground">{t("attendance.selectClassHistory")}</p>
        </div>
      )}
    </>
  )
}

export default function TeacherAttendancePage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
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

  const [activeTab, setActiveTab] = useState<"mark" | "history">("mark")
  const [historyStartDate, setHistoryStartDate] = useState(() => {
    const d = new Date()
    d.setDate(d.getDate() - 30)
    return d.toISOString().split("T")[0]
  })
  const [historyEndDate, setHistoryEndDate] = useState(() => new Date().toISOString().split("T")[0])
  const [historyClassId, setHistoryClassId] = useState("")
  const [historyRecords, setHistoryRecords] = useState<AttendanceRecord[]>([])
  const [historySummary, setHistorySummary] = useState<AttendanceSummary[]>([])
  const [loadingHistory, setLoadingHistory] = useState(false)

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
      setError(t("classes.loadError"))
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
      setError(t("students.loadError"))
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
      setError(t("attendance.markRequired"))
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
      setError(t("attendance.submitError"))
    } finally {
      setSubmitting(false)
    }
  }

  const markedCount = Object.values(attendance).filter(
    (s) => s !== null
  ).length
  const totalCount = students.length

  async function loadHistory() {
    if (!historyClassId) return
    try {
      setLoadingHistory(true)
      setError(null)
      const records = await appFetch<AttendanceRecord[]>(
        `/v1/attendance?classId=${historyClassId}&date=${historyEndDate}`
      )
      setHistoryRecords(records)
      const summaryMap = new Map<string, { studentId: string; studentName: string; totalDays: number; presentDays: number; absentDays: number; lateDays: number; excusedDays: number; attendanceRate: number }>()
      for (const r of records) {
        const existing = summaryMap.get(r.studentId)
        if (existing) {
          existing.totalDays++
          if (r.status === "PRESENT") existing.presentDays++
          else if (r.status === "ABSENT") existing.absentDays++
          else if (r.status === "LATE") existing.lateDays++
          else if (r.status === "EXCUSED") existing.excusedDays++
          existing.attendanceRate = existing.totalDays > 0 ? (existing.presentDays / existing.totalDays) * 100 : 0
        } else {
          summaryMap.set(r.studentId, {
            studentId: r.studentId,
            studentName: r.studentName || r.studentId,
            totalDays: 1,
            presentDays: r.status === "PRESENT" ? 1 : 0,
            absentDays: r.status === "ABSENT" ? 1 : 0,
            lateDays: r.status === "LATE" ? 1 : 0,
            excusedDays: r.status === "EXCUSED" ? 1 : 0,
            attendanceRate: r.status === "PRESENT" ? 100 : 0,
          })
        }
      }
      setHistorySummary(Array.from(summaryMap.values()))
    } catch {
      setHistoryRecords([])
      setHistorySummary([])
    } finally {
      setLoadingHistory(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            {tn("attendance")}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {t("attendance.subtitle")}
          </p>
        </div>
        {activeTab === "mark" && selectedClassId && students.length > 0 && (
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
            {submitting ? t("attendance.submitting") : t("attendance.submitAttendance")}
          </Button>
        )}
      </div>

      <div className="flex gap-1 rounded-xl border border-border bg-muted/50 p-1">
        <button
          onClick={() => setActiveTab("mark")}
          className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
            activeTab === "mark" ? "bg-background text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          <ClipboardCheck className="size-4" /> {t("attendance.markTab")}
        </button>
        <button
          onClick={() => setActiveTab("history")}
          className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
            activeTab === "history" ? "bg-background text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          <History className="size-4" /> {tn("history")}
        </button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {activeTab === "mark" && (
        <MarkAttendanceTab
          classes={classes}
          students={students}
          attendance={attendance}
          selectedClassId={selectedClassId}
          selectedDate={selectedDate}
          loadingClasses={loadingClasses}
          loadingStudents={loadingStudents}
          submitting={submitting}
          success={success}
          markedCount={markedCount}
          totalCount={totalCount}
          setSelectedClassId={setSelectedClassId}
          setSelectedDate={setSelectedDate}
          setStatus={setStatus}
          setAllStatuses={setAllStatuses}
          submitAttendance={submitAttendance}
        />
      )}

      {activeTab === "history" && (
        <HistoryTab
          classes={classes}
          historyClassId={historyClassId}
          historyStartDate={historyStartDate}
          historyEndDate={historyEndDate}
          historyRecords={historyRecords}
          historySummary={historySummary}
          loadingHistory={loadingHistory}
          setHistoryClassId={setHistoryClassId}
          setHistoryStartDate={setHistoryStartDate}
          setHistoryEndDate={setHistoryEndDate}
          loadHistory={loadHistory}
        />
      )}
    </div>
  )
}
