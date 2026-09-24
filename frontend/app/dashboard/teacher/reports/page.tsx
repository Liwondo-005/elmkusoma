"use client"

import { useEffect, useState, useMemo } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { Button } from "@/components/ui/button"
import {
  BarChart3,
  Users,
  FileText,
  ClipboardCheck,
  PenTool,
  Loader2,
  AlertCircle,
  ChevronDown,
  Download,
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  BookOpen,
} from "lucide-react"
import { teacherFetch } from "@/lib/teacher-api"

interface ClassOption {
  classGroupId: string
  className: string
  subjectName: string
}

interface TeacherStudent {
  studentId: string
  fullName: string
  admissionNumber?: string
  gender?: string
  email?: string
  classGroupId: string
  className?: string
}

interface AnalyticsData {
  totalStudents: number
  totalAssignments: number
  totalLessons: number
  avgAttendancePercentage: number
  pendingGrading: number
  classesCount: number
  upcomingDeadlines: Array<{
    id: string
    title: string
    dueDate: string
    className?: string
  }>
  recentSubmissions: Array<{
    id: string
    studentName: string
    assignmentTitle: string
    submittedAt: string
    obtainedMarks?: number
  }>
}

interface AttendanceSummary {
  studentId: string
  studentName?: string
  className?: string
  totalDays: number
  presentDays: number
  absentDays: number
  lateDays: number
  excusedDays: number
  attendanceRate: number
}

interface Assignment {
  id: string
  title: string
  description?: string
  assignmentType?: string
  dueDate?: string
  totalMarks: number
  status?: string
  classGroupId: string
  className?: string
  subjectName?: string
  submissionCount?: number
  totalStudents?: number
}

interface Assessment {
  id: string
  title: string
  description?: string
  totalMarks: number
  passMarks: number
  timeLimitMinutes?: number
  classGroupId: string
  className?: string
  subjectName?: string
  attemptCount?: number
  totalStudents?: number
}

interface AssessmentResult {
  id: string
  studentId: string
  studentName?: string
  totalScore: number
  isPassed: boolean
  gradedAt?: string
}

type ReportTab = "overview" | "students" | "attendance" | "assignments" | "assessments"

export default function TeacherReportsPage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const [classes, setClasses] = useState<ClassOption[]>([])
  const [allStudents, setAllStudents] = useState<TeacherStudent[]>([])
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null)
  const [selectedClassId, setSelectedClassId] = useState("")
  const [activeTab, setActiveTab] = useState<ReportTab>("overview")

  const [attendanceSummaries, setAttendanceSummaries] = useState<Record<string, AttendanceSummary[]>>({})
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [assessmentResults, setAssessmentResults] = useState<Record<string, AssessmentResult[]>>({})

  const [loading, setLoading] = useState(true)
  const [loadingAttendance, setLoadingAttendance] = useState(false)
  const [loadingAssignments, setLoadingAssignments] = useState(false)
  const [loadingAssessments, setLoadingAssessments] = useState(false)
  const [loadingResults, setLoadingResults] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const tabs: { id: ReportTab; label: string; icon: typeof BarChart3 }[] = [
    { id: "overview", label: t("reports.tabOverview"), icon: BarChart3 },
    { id: "students", label: t("reports.tabStudents"), icon: Users },
    { id: "attendance", label: tn("attendance"), icon: ClipboardCheck },
    { id: "assignments", label: tn("assignments"), icon: FileText },
    { id: "assessments", label: tn("assessments"), icon: PenTool },
  ]

  useEffect(() => {
    if (!user) return
    loadInitialData()
  }, [user])

  useEffect(() => {
    if (selectedClassId) {
      loadClassSpecificData(selectedClassId)
    }
  }, [selectedClassId])

  async function loadInitialData() {
    try {
      setLoading(true)
      setError(null)
      const [classesData, studentsData, analyticsData] = await Promise.allSettled([
        teacherFetch<ClassOption[]>("/v1/teachers/me/classes"),
        teacherFetch<TeacherStudent[]>("/v1/teachers/me/students"),
        teacherFetch<AnalyticsData>("/v1/teachers/me/analytics"),
      ])
      if (classesData.status === "fulfilled") setClasses(classesData.value)
      if (studentsData.status === "fulfilled") setAllStudents(studentsData.value)
      if (analyticsData.status === "fulfilled") setAnalytics(analyticsData.value)
    } catch {
      setError(t("reports.loadError"))
    } finally {
      setLoading(false)
    }
  }

  async function loadClassSpecificData(classId: string) {
    try {
      setLoadingAttendance(true)
      setLoadingAssignments(true)
      setLoadingAssessments(true)

      const [attendanceData, assignmentsData, assessmentsData] = await Promise.allSettled([
        teacherFetch<AttendanceSummary[]>(`/v1/attendance/summary/class/${classId}`).catch(() => []),
        teacherFetch<Assignment[]>(`/v1/learning/assignments/class/${classId}`).catch(() => []),
        teacherFetch<Assessment[]>(`/v1/assessments/class/${classId}`).catch(() => []),
      ])

      if (attendanceData.status === "fulfilled") {
        setAttendanceSummaries((prev) => ({ ...prev, [classId]: attendanceData.value }))
      }
      if (assignmentsData.status === "fulfilled") {
        const enriched = assignmentsData.value.map((a) => {
          const cls = classes.find((c) => c.classGroupId === classId)
          return { ...a, className: cls?.className, subjectName: cls?.subjectName }
        })
        setAssignments((prev) => {
          const withoutClass = prev.filter((a) => a.classGroupId !== classId)
          return [...withoutClass, ...enriched]
        })
      }
      if (assessmentsData.status === "fulfilled") {
        const enriched = assessmentsData.value.map((a) => {
          const cls = classes.find((c) => c.classGroupId === classId)
          return { ...a, className: cls?.className, subjectName: cls?.subjectName }
        })
        setAssessments((prev) => {
          const withoutClass = prev.filter((a) => a.classGroupId !== classId)
          return [...withoutClass, ...enriched]
        })
      }
    } catch {
      // partial failures handled by individual loaders
    } finally {
      setLoadingAttendance(false)
      setLoadingAssignments(false)
      setLoadingAssessments(false)
    }
  }

  async function loadAssessmentResults(assessmentId: string) {
    try {
      setLoadingResults(true)
      const data = await teacherFetch<AssessmentResult[]>(`/v1/assessments/${assessmentId}/results`)
      setAssessmentResults((prev) => ({ ...prev, [assessmentId]: data }))
    } catch {
      setAssessmentResults((prev) => ({ ...prev, [assessmentId]: [] }))
    } finally {
      setLoadingResults(false)
    }
  }

  const filteredStudents = useMemo(() => {
    if (!selectedClassId) return allStudents
    return allStudents.filter((s) => s.classGroupId === selectedClassId)
  }, [allStudents, selectedClassId])

  const filteredAssignments = useMemo(() => {
    if (!selectedClassId) return assignments
    return assignments.filter((a) => a.classGroupId === selectedClassId)
  }, [assignments, selectedClassId])

  const filteredAssessments = useMemo(() => {
    if (!selectedClassId) return assessments
    return assessments.filter((a) => a.classGroupId === selectedClassId)
  }, [assessments, selectedClassId])

  const filteredAttendance = useMemo(() => {
    if (!selectedClassId) {
      return Object.values(attendanceSummaries).flat()
    }
    return attendanceSummaries[selectedClassId] || []
  }, [attendanceSummaries, selectedClassId])

  const classAttendanceStats = useMemo(() => {
    const data = filteredAttendance
    if (data.length === 0) return null
    const totalDays = data.reduce((sum, s) => sum + s.totalDays, 0)
    const present = data.reduce((sum, s) => sum + s.presentDays, 0)
    const absent = data.reduce((sum, s) => sum + s.absentDays, 0)
    const late = data.reduce((sum, s) => sum + s.lateDays, 0)
    const excused = data.reduce((sum, s) => sum + s.excusedDays, 0)
    const rate = totalDays > 0 ? Math.round((present / totalDays) * 100) : 0
    return { totalStudents: data.length, totalDays, present, absent, late, excused, rate }
  }, [filteredAttendance])

  function handleExport() {
    window.print()
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("reports")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("reports.subtitle")}</p>
        </div>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("reports")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("reports.subtitle")}</p>
        </div>
        <Button variant="outline" size="sm" onClick={handleExport} className="gap-1.5">
          <Download className="size-3.5" />
          {t("grading.exportCsv")}
        </Button>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative">
          <Users className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <select
            value={selectedClassId}
            onChange={(e) => setSelectedClassId(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background pl-9 pr-8 text-sm outline-none focus:border-ring"
          >
            <option value="">{t("students.allClasses")}</option>
            {classes.map((c) => (
              <option key={c.classGroupId} value={c.classGroupId}>
                {c.className} — {c.subjectName}
              </option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      <div className="flex gap-1 overflow-x-auto rounded-xl border border-border bg-muted/50 p-1">
        {tabs.map((tab) => {
          const Icon = tab.icon
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? "bg-card text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Icon className="size-4" />
              <span className="hidden sm:inline">{tab.label}</span>
            </button>
          )
        })}
      </div>

      {activeTab === "overview" && (
        <ClassOverviewTab
          analytics={analytics}
          students={filteredStudents}
          classes={classes}
          selectedClassId={selectedClassId}
          assignments={filteredAssignments}
          assessments={filteredAssessments}
          attendanceStats={classAttendanceStats}
        />
      )}

      {activeTab === "students" && (
        <StudentPerformanceTab
          students={filteredStudents}
          attendance={filteredAttendance}
          assignments={filteredAssignments}
          selectedClassId={selectedClassId}
        />
      )}

      {activeTab === "attendance" && (
        <AttendanceTab
          summaries={filteredAttendance}
          stats={classAttendanceStats}
          loading={loadingAttendance}
          selectedClassId={selectedClassId}
        />
      )}

      {activeTab === "assignments" && (
        <AssignmentsTab
          assignments={filteredAssignments}
          loading={loadingAssignments}
          selectedClassId={selectedClassId}
        />
      )}

      {activeTab === "assessments" && (
        <AssessmentsTab
          assessments={filteredAssessments}
          results={assessmentResults}
          loading={loadingAssessments}
          loadingResults={loadingResults}
          selectedClassId={selectedClassId}
          onLoadResults={loadAssessmentResults}
        />
      )}
    </div>
  )
}

function getPerformanceColor(rate: number): string {
  if (rate >= 80) return "text-teal"
  if (rate >= 60) return "text-amber-600"
  return "text-destructive"
}

function getPerformanceBg(rate: number): string {
  if (rate >= 80) return "bg-teal/10"
  if (rate >= 60) return "bg-amber-500/10"
  return "bg-destructive/10"
}

function StatCard({ label, value, icon: Icon, color, bg }: {
  label: string
  value: string | number
  icon: typeof Users
  color: string
  bg: string
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className={`mb-3 inline-flex size-9 items-center justify-center rounded-xl ${bg}`}>
        <Icon className={`size-5 ${color}`} />
      </div>
      <p className="text-2xl font-extrabold text-foreground">{value}</p>
      <p className="text-sm font-medium text-foreground">{label}</p>
    </div>
  )
}

function ClassOverviewTab({ analytics, students, classes, selectedClassId, assignments, assessments, attendanceStats }: {
  analytics: AnalyticsData | null
  students: TeacherStudent[]
  classes: ClassOption[]
  selectedClassId: string
  assignments: Assignment[]
  assessments: Assessment[]
  attendanceStats: { rate: number } | null
}) {
  const t = useTranslations("teacher")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const cls = classes.find((c) => c.classGroupId === selectedClassId)

  const stats = [
    { key: "students", label: t("grading.studentsLabel"), value: students.length, icon: Users, color: "text-blue-500", bg: "bg-blue-500/10" },
    { key: "assignments", label: tn("assignments"), value: assignments.length, icon: FileText, color: "text-purple-500", bg: "bg-purple-500/10" },
    { key: "assessments", label: tn("assessments"), value: assessments.length, icon: PenTool, color: "text-teal-500", bg: "bg-teal-500/10" },
    {
      key: "attendance",
      label: t("reports.attendanceRate"),
      value: attendanceStats ? `${attendanceStats.rate}%` : `${analytics?.avgAttendancePercentage ?? 0}%`,
      icon: BarChart3,
      color: "text-green-500",
      bg: "bg-green-500/10",
    },
  ]

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map(({ key, ...rest }) => (
          <StatCard key={key} {...rest} />
        ))}
      </div>

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">
            {cls ? t("reports.studentListTitle", { name: `${cls.className} — ${cls.subjectName}` }) : t("reports.studentListTitle", { name: t("students.allClasses") })}
          </h2>
          <span className="text-sm text-muted-foreground">{t("courses.students", { count: students.length })}</span>
        </div>
        {students.length === 0 ? (
          <div className="py-8 text-center">
            <Users className="mx-auto size-10 text-muted-foreground/50" />
            <p className="mt-2 text-sm text-muted-foreground">{t("reports.noStudents")}</p>
          </div>
        ) : (
          <div className="mt-4 overflow-hidden rounded-xl border border-border">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("gradebook.colStudent")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">{t("students.colAdmission")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground hidden md:table-cell">{t("reports.colGender")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-right">{t("students.colStatus")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {students.map((student) => (
                  <tr key={student.studentId} className="hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <p className="font-medium text-foreground">{student.fullName}</p>
                      {student.email && (
                        <p className="text-xs text-muted-foreground">{student.email}</p>
                      )}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-muted-foreground hidden sm:table-cell">
                      {student.admissionNumber || "—"}
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground hidden md:table-cell">
                      {student.gender || "—"}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className="inline-flex items-center gap-1 rounded-full bg-teal/10 px-2 py-0.5 text-[10px] font-semibold text-teal">
                        <CheckCircle className="size-3" />
                        {ts("active")}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">{t("analytics.upcomingDeadlines")}</h2>
            <Clock className="size-4 text-muted-foreground" />
          </div>
          <div className="mt-4 space-y-3">
            {!analytics?.upcomingDeadlines?.length ? (
              <p className="py-4 text-center text-sm text-muted-foreground">{t("analytics.noDeadlines")}</p>
            ) : (
              analytics.upcomingDeadlines.slice(0, 5).map((item) => (
                <div key={item.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-orange/10">
                    <Clock className="size-4 text-orange" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>{new Date(item.dueDate).toLocaleDateString()}</span>
                      {item.className && (
                        <>
                          <span className="text-border">·</span>
                          <span>{item.className}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">{t("analytics.recentSubmissions")}</h2>
            <TrendingUp className="size-4 text-muted-foreground" />
          </div>
          <div className="mt-4 space-y-3">
            {!analytics?.recentSubmissions?.length ? (
              <p className="py-4 text-center text-sm text-muted-foreground">{t("analytics.noSubmissions")}</p>
            ) : (
              analytics.recentSubmissions.slice(0, 5).map((sub) => (
                <div key={sub.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <Users className="size-4 text-primary" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{sub.studentName}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span className="truncate">{sub.assignmentTitle}</span>
                      <span className="text-border">·</span>
                      <span>{new Date(sub.submittedAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                  {sub.obtainedMarks !== undefined && sub.obtainedMarks !== null && (
                    <span className="shrink-0 rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-400">
                      {sub.obtainedMarks}
                    </span>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

function StudentPerformanceTab({ students, attendance, assignments, selectedClassId }: {
  students: TeacherStudent[]
  attendance: AttendanceSummary[]
  assignments: Assignment[]
  selectedClassId: string
}) {
  const t = useTranslations("teacher")

  function getPerformanceLabel(rate: number): string {
    if (rate >= 80) return t("reports.perfExcellent")
    if (rate >= 60) return t("reports.perfGood")
    return t("reports.perfNeeds")
  }

  const studentPerformance = useMemo(() => {
    return students.map((student) => {
      const att = attendance.find((a) => a.studentId === student.studentId)
      const attendanceRate = att ? att.attendanceRate : 0

      const completionRate = assignments.length > 0
        ? Math.round((assignments.filter((a) => a.submissionCount !== undefined && a.submissionCount > 0).length / assignments.length) * 100)
        : 0

      const avgGrade = att && att.totalDays > 0
        ? Math.round(att.attendanceRate)
        : 0

      return {
        ...student,
        attendanceRate,
        completionRate,
        avgGrade,
      }
    })
  }, [students, attendance, assignments])

  return (
    <div className="space-y-6">
      {students.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("gradebook.noStudents")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("reports.noStudentsClass")}</p>
        </div>
      ) : (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("reports.perfTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("reports.perfDesc")}</p>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("gradebook.colStudent")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">{t("students.colAdmission")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{t("reports.colAttendance")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{t("reports.colCompletion")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-right">{t("reports.colPerformance")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {studentPerformance.map((student) => (
                  <tr key={student.studentId} className="hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <p className="font-medium text-foreground">{student.fullName}</p>
                      {student.gender && (
                        <p className="text-xs text-muted-foreground">{student.gender}</p>
                      )}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-muted-foreground hidden sm:table-cell">
                      {student.admissionNumber || "—"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col items-center gap-1">
                        <div className="flex items-center gap-2">
                          <div className="h-2 w-24 overflow-hidden rounded-full bg-muted">
                            <div
                              className={`h-full rounded-full transition-all ${
                                student.attendanceRate >= 80 ? "bg-teal" : student.attendanceRate >= 60 ? "bg-amber-500" : "bg-destructive"
                              }`}
                              style={{ width: `${student.attendanceRate}%` }}
                            />
                          </div>
                          <span className={`text-xs font-medium ${getPerformanceColor(student.attendanceRate)}`}>
                            {student.attendanceRate}%
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col items-center gap-1">
                        <div className="flex items-center gap-2">
                          <div className="h-2 w-24 overflow-hidden rounded-full bg-muted">
                            <div
                              className="h-full rounded-full bg-primary transition-all"
                              style={{ width: `${student.completionRate}%` }}
                            />
                          </div>
                          <span className="text-xs font-medium text-foreground">{student.completionRate}%</span>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${getPerformanceBg(student.attendanceRate)} ${getPerformanceColor(student.attendanceRate)}`}>
                        {student.attendanceRate >= 80 ? (
                          <TrendingUp className="size-3" />
                        ) : student.attendanceRate >= 60 ? (
                          <BookOpen className="size-3" />
                        ) : (
                          <TrendingDown className="size-3" />
                        )}
                        {getPerformanceLabel(student.attendanceRate)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}

function AttendanceTab({ summaries, stats, loading, selectedClassId }: {
  summaries: AttendanceSummary[]
  stats: { totalStudents: number; totalDays: number; present: number; absent: number; late: number; excused: number; rate: number } | null
  loading: boolean
  selectedClassId: string
}) {
  const t = useTranslations("teacher")
  const ts = useTranslations("status")

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {stats && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <StatCard label={t("analytics.totalStudents")} value={stats.totalStudents} icon={Users} color="text-blue-500" bg="bg-blue-500/10" />
          <StatCard label={ts("present")} value={stats.present} icon={CheckCircle} color="text-teal" bg="bg-teal/10" />
          <StatCard label={ts("absent")} value={stats.absent} icon={XCircle} color="text-destructive" bg="bg-destructive/10" />
          <StatCard label={ts("late")} value={stats.late} icon={Clock} color="text-amber-600" bg="bg-amber-500/10" />
          <StatCard label={ts("excused")} value={stats.excused} icon={AlertTriangle} color="text-primary" bg="bg-primary/10" />
        </div>
      )}

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">{t("reports.attendanceByStudent")}</h2>
          {stats && (
            <span className="text-sm text-muted-foreground">
              {t("reports.overallRate")} <span className={`font-semibold ${getPerformanceColor(stats.rate)}`}>{stats.rate}%</span>
            </span>
          )}
        </div>

        {summaries.length === 0 ? (
          <div className="py-8 text-center">
            <ClipboardCheck className="mx-auto size-10 text-muted-foreground/50" />
            <p className="mt-2 text-sm text-muted-foreground">{t("support.noAttendance")}</p>
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("gradebook.colStudent")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{ts("present")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{ts("absent")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{ts("late")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{ts("excused")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-right">{t("classDetail.colRate")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {summaries.map((summary) => (
                  <tr key={summary.studentId} className="hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <p className="font-medium text-foreground">{summary.studentName || summary.studentId.slice(0, 8) + "..."}</p>
                      {summary.className && (
                        <p className="text-xs text-muted-foreground">{summary.className}</p>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="inline-flex items-center gap-1 text-sm text-teal">
                        <CheckCircle className="size-3" />
                        {summary.presentDays}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="inline-flex items-center gap-1 text-sm text-destructive">
                        <XCircle className="size-3" />
                        {summary.absentDays}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="inline-flex items-center gap-1 text-sm text-amber-600">
                        <Clock className="size-3" />
                        {summary.lateDays}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="inline-flex items-center gap-1 text-sm text-primary">
                        <AlertTriangle className="size-3" />
                        {summary.excusedDays}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <div className="h-2 w-20 overflow-hidden rounded-full bg-muted">
                          <div
                            className={`h-full rounded-full transition-all ${
                              summary.attendanceRate >= 80 ? "bg-teal" : summary.attendanceRate >= 60 ? "bg-amber-500" : "bg-destructive"
                            }`}
                            style={{ width: `${summary.attendanceRate}%` }}
                          />
                        </div>
                        <span className={`text-xs font-semibold ${getPerformanceColor(summary.attendanceRate)}`}>
                          {summary.attendanceRate}%
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function AssignmentsTab({ assignments, loading, selectedClassId }: {
  assignments: Assignment[]
  loading: boolean
  selectedClassId: string
}) {
  const t = useTranslations("teacher")

  const stats = useMemo(() => {
    if (assignments.length === 0) return null
    const totalSubmissions = assignments.reduce((sum, a) => sum + (a.submissionCount || 0), 0)
    const totalStudents = assignments.reduce((sum, a) => sum + (a.totalStudents || 0), 0)
    const avgCompletion = totalStudents > 0 ? Math.round((totalSubmissions / totalStudents) * 100) : 0
    const published = assignments.filter((a) => a.status === "PUBLISHED").length
    return { total: assignments.length, avgCompletion, published, totalSubmissions }
  }, [assignments])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {stats && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label={t("analytics.totalAssignments")} value={stats.total} icon={FileText} color="text-purple-500" bg="bg-purple-500/10" />
          <StatCard label={t("grading.publishedLabel")} value={stats.published} icon={CheckCircle} color="text-teal" bg="bg-teal/10" />
          <StatCard label={t("reports.totalSubmissions")} value={stats.totalSubmissions} icon={Users} color="text-blue-500" bg="bg-blue-500/10" />
          <StatCard label={t("reports.avgCompletion")} value={`${stats.avgCompletion}%`} icon={BarChart3} color="text-green-500" bg="bg-green-500/10" />
        </div>
      )}

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">{t("reports.assignmentList")}</h2>

        {assignments.length === 0 ? (
          <div className="py-8 text-center">
            <FileText className="mx-auto size-10 text-muted-foreground/50" />
            <p className="mt-2 text-sm text-muted-foreground">{t("reports.noAssignments")}</p>
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 font-medium text-muted-foreground">{t("reports.colAssignment")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">{t("reports.colClass")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{t("grading.colStatus")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{t("reports.colSubmissions")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-center">{t("reports.colCompletion")}</th>
                  <th className="px-4 py-3 font-medium text-muted-foreground text-right">{t("reports.colTotalMarks")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {assignments.map((a) => {
                  const completion = (a.totalStudents && a.totalStudents > 0)
                    ? Math.round(((a.submissionCount || 0) / a.totalStudents) * 100)
                    : 0
                  return (
                    <tr key={a.id} className="hover:bg-muted/30">
                      <td className="px-4 py-3">
                        <p className="font-medium text-foreground">{a.title}</p>
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          {a.assignmentType && <span>{a.assignmentType}</span>}
                          {a.dueDate && (
                            <>
                              <span className="text-border">·</span>
                              <span>{t("classDetail.dueLabel", { date: new Date(a.dueDate).toLocaleDateString() })}</span>
                            </>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground hidden sm:table-cell">
                        {a.className || "—"}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          a.status === "PUBLISHED"
                            ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                            : "bg-gray-100 text-gray-700 dark:bg-gray-800/30 dark:text-gray-400"
                        }`}>
                          {a.status === "PUBLISHED" ? t("lessons.published") : a.status === "DRAFT" ? t("lessons.draft") : a.status || t("mediaLibrary.unknownSize")}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center text-sm text-foreground">
                        {a.submissionCount ?? 0}/{a.totalStudents ?? "—"}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <div className="flex items-center justify-center gap-2">
                          <div className="h-2 w-16 overflow-hidden rounded-full bg-muted">
                            <div
                              className={`h-full rounded-full transition-all ${
                                completion >= 80 ? "bg-teal" : completion >= 50 ? "bg-amber-500" : "bg-destructive"
                              }`}
                              style={{ width: `${completion}%` }}
                            />
                          </div>
                          <span className="text-xs font-medium text-foreground">{completion}%</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right text-sm text-muted-foreground">
                        {a.totalMarks}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function AssessmentsTab({ assessments, results, loading, loadingResults, selectedClassId, onLoadResults }: {
  assessments: Assessment[]
  results: Record<string, AssessmentResult[]>
  loading: boolean
  loadingResults: boolean
  selectedClassId: string
  onLoadResults: (id: string) => void
}) {
  const t = useTranslations("teacher")
  const ts = useTranslations("status")

  const stats = useMemo(() => {
    if (assessments.length === 0) return null
    const totalAttempts = assessments.reduce((sum, a) => sum + (a.attemptCount || 0), 0)
    const totalPossible = assessments.reduce((sum, a) => sum + (a.totalStudents || 0), 0)
    const avgAttempts = totalPossible > 0 ? Math.round((totalAttempts / totalPossible) * 100) : 0
    const totalPassed = Object.values(results).flat().filter((r) => r.isPassed).length
    const totalGraded = Object.values(results).flat().length
    const passRate = totalGraded > 0 ? Math.round((totalPassed / totalGraded) * 100) : 0
    return { total: assessments.length, totalAttempts, avgAttempts, passRate, totalPassed, totalGraded }
  }, [assessments, results])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {stats && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label={t("reports.totalAssessments")} value={stats.total} icon={PenTool} color="text-teal" bg="bg-teal/10" />
          <StatCard label={t("reports.totalAttempts")} value={stats.totalAttempts} icon={Users} color="text-blue-500" bg="bg-blue-500/10" />
          <StatCard label={t("reports.gradedResults")} value={stats.totalGraded} icon={CheckCircle} color="text-purple-500" bg="bg-purple-500/10" />
          <StatCard label={t("reports.passRate")} value={`${stats.passRate}%`} icon={BarChart3} color="text-green-500" bg="bg-green-500/10" />
        </div>
      )}

      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">{t("reports.assessmentList")}</h2>

        {assessments.length === 0 ? (
          <div className="py-8 text-center">
            <PenTool className="mx-auto size-10 text-muted-foreground/50" />
            <p className="mt-2 text-sm text-muted-foreground">{t("reports.noAssessmentsFound")}</p>
          </div>
        ) : (
          <div className="mt-4 space-y-3">
            {assessments.map((a) => {
              const assessmentResults = results[a.id]
              const gradedCount = assessmentResults?.length || 0
              const passedCount = assessmentResults?.filter((r) => r.isPassed).length || 0
              const passRate = gradedCount > 0 ? Math.round((passedCount / gradedCount) * 100) : 0

              return (
                <div key={a.id} className="rounded-xl border border-border p-4 transition-all hover:shadow-sm">
                  <div className="flex items-start gap-4">
                    <div className="flex size-10 items-center justify-center rounded-xl bg-teal/10 shrink-0">
                      <PenTool className="size-5 text-teal" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="text-sm font-semibold text-foreground">{a.title}</h3>
                        {a.className && (
                          <span className="inline-flex rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                            {a.className}
                          </span>
                        )}
                      </div>
                      {a.description && (
                        <p className="mt-1 text-xs text-muted-foreground line-clamp-1">{a.description}</p>
                      )}
                      <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                        <span>{t("reports.passMarks", { pass: a.passMarks, total: a.totalMarks })}</span>
                        {a.timeLimitMinutes && <span>{t("classDetail.minutesCount", { count: a.timeLimitMinutes })}</span>}
                        {a.attemptCount !== undefined && (
                          <span>{t("reports.attemptsLabel", { done: a.attemptCount, total: a.totalStudents || "—" })}</span>
                        )}
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => onLoadResults(a.id)}
                      className="gap-1 shrink-0"
                    >
                      <BarChart3 className="size-3" />
                      {t("assessments.resultsBtn")}
                    </Button>
                  </div>

                  {assessmentResults && assessmentResults.length > 0 && (
                    <div className="mt-4 overflow-hidden rounded-xl border border-border">
                      <table className="w-full text-left text-sm">
                        <thead className="border-b border-border bg-muted/50">
                          <tr>
                            <th className="px-4 py-2 font-medium text-muted-foreground">{t("gradebook.colStudent")}</th>
                            <th className="px-4 py-2 font-medium text-muted-foreground text-center">{t("assessments.colScore")}</th>
                            <th className="px-4 py-2 font-medium text-muted-foreground text-center">{t("grading.colStatus")}</th>
                            <th className="px-4 py-2 font-medium text-muted-foreground text-right">{t("assessments.colGraded")}</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-border">
                          {assessmentResults.map((r) => (
                            <tr key={r.id} className="hover:bg-muted/30">
                              <td className="px-4 py-2 font-medium text-foreground">
                                {r.studentName || r.studentId.slice(0, 8) + "..."}
                              </td>
                              <td className="px-4 py-2 text-center text-foreground">{r.totalScore}</td>
                              <td className="px-4 py-2 text-center">
                                <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                  r.isPassed
                                    ? "bg-teal/10 text-teal"
                                    : "bg-destructive/10 text-destructive"
                                }`}>
                                  {r.isPassed ? (
                                    <><CheckCircle className="size-3" /> {t("assessments.passedStatus")}</>
                                  ) : (
                                    <><XCircle className="size-3" /> {t("assessments.failedStatus")}</>
                                  )}
                                </span>
                              </td>
                              <td className="px-4 py-2 text-right text-muted-foreground">
                                {r.gradedAt ? new Date(r.gradedAt).toLocaleDateString() : ts("pending")}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  {assessmentResults && assessmentResults.length > 0 && (
                    <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                      <span>{t("reports.gradedCount", { count: gradedCount })}</span>
                      <span>{t("reports.passedCount", { count: passedCount })}</span>
                      <span className={`font-semibold ${getPerformanceColor(passRate)}`}>{t("reports.passRateLabel", { rate: passRate })}</span>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
