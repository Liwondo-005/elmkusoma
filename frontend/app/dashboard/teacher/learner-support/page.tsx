"use client"

import { useEffect, useState, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import {
  Users,
  BookOpen,
  ClipboardCheck,
  BarChart3,
  TrendingUp,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock,
  Search,
  Loader2,
  AlertCircle,
  GraduationCap,
  FileText,
} from "lucide-react"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function teacherFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const institutionId = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001" : "00000000-0000-0000-0000-000000000001"
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

interface ClassOption {
  classGroupId: string
  className: string
  subjectName: string
}

interface StudentListItem {
  studentId: string
  fullName: string
  email: string
  admissionNumber: string
  className: string
  subjectName: string
  gender: string
  status: string
  classGroupId: string
}

interface AttendanceSummary {
  studentId: string
  studentName: string
  presentDays: number
  absentDays: number
  lateDays: number
  excusedDays: number
  attendancePercentage: number
}

interface LearningProgress {
  id: string
  lessonId: string
  studentId: string
  completionPercentage: number
  startedAt?: string
  completedAt?: string
  createdAt: string
}

interface Assignment {
  id: string
  title: string
  totalMarks: number
  classGroupId: string
  dueDate?: string
  createdAt: string
}

interface AssignmentSubmission {
  id: string
  assignmentId: string
  studentId: string
  submittedAt: string
  grade?: number
  feedback?: string
}

interface Assessment {
  id: string
  title: string
  totalMarks: number
  passMarks: number
  classGroupId: string
  createdAt: string
}

interface AssessmentResult {
  id: string
  assessmentId: string
  studentId: string
  totalScore: number
  isPassed: boolean
  gradedAt?: string
}

function formatDate(iso: string) {
  if (!iso) return "—"
  return new Date(iso).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  })
}

function formatDateTime(iso: string) {
  if (!iso) return "—"
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function getGradeColor(score: number, total: number) {
  const pct = total > 0 ? (score / total) * 100 : 0
  if (pct >= 80) return "text-teal bg-teal/10"
  if (pct >= 60) return "text-primary bg-primary/10"
  if (pct >= 40) return "text-amber-600 bg-amber-500/10"
  return "text-destructive bg-destructive/10"
}

function getAttendanceBarColor(pct: number) {
  if (pct >= 80) return "bg-teal"
  if (pct >= 60) return "bg-primary"
  if (pct >= 40) return "bg-amber-500"
  return "bg-destructive"
}

export default function TeacherLearnerSupportPage() {
  const { user } = useAuth()

  const [classes, setClasses] = useState<ClassOption[]>([])
  const [allStudents, setAllStudents] = useState<StudentListItem[]>([])
  const [selectedStudentId, setSelectedStudentId] = useState<string>("")
  const [search, setSearch] = useState("")

  const [loadingClasses, setLoadingClasses] = useState(true)
  const [loadingStudents, setLoadingStudents] = useState(true)
  const [loadingProfile, setLoadingProfile] = useState(false)

  const [attendance, setAttendance] = useState<AttendanceSummary | null>(null)
  const [loadingAttendance, setLoadingAttendance] = useState(false)

  const [progress, setProgress] = useState<LearningProgress[]>([])
  const [loadingProgress, setLoadingProgress] = useState(false)

  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loadingAssignments, setLoadingAssignments] = useState(false)

  const [assessments, setAssessments] = useState<Assessment[]>([])
  const [assessmentResults, setAssessmentResults] = useState<AssessmentResult[]>([])
  const [loadingAssessments, setLoadingAssessments] = useState(false)

  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadInitialData()
  }, [user])

  async function loadInitialData() {
    try {
      setLoadingClasses(true)
      setLoadingStudents(true)
      setError(null)
      const [classesData, studentsData] = await Promise.all([
        teacherFetch<ClassOption[]>("/v1/teachers/me/classes"),
        teacherFetch<StudentListItem[]>("/v1/teachers/me/students"),
      ])
      setClasses(classesData)
      setAllStudents(studentsData)
    } catch {
      setError("Failed to load data")
      setClasses([])
      setAllStudents([])
    } finally {
      setLoadingClasses(false)
      setLoadingStudents(false)
    }
  }

  const loadStudentData = useCallback(async (studentId: string) => {
    if (!studentId) return
    try {
      setLoadingProfile(true)
      setLoadingAttendance(true)
      setLoadingProgress(true)
      setLoadingAssignments(true)
      setLoadingAssessments(true)
      setError(null)
      setAttendance(null)
      setProgress([])
      setAssignments([])
      setSubmissions([])
      setAssessments([])
      setAssessmentResults([])

      const student = allStudents.find((s) => s.studentId === studentId)
      if (!student) return

      const classGroupId = student.classGroupId

      const [attendanceData, progressData, assignmentsData, assessmentsData] = await Promise.allSettled([
        teacherFetch<AttendanceSummary>(`/v1/attendance/summary/student/${studentId}`),
        teacherFetch<LearningProgress[]>(`/v1/learning/progress/student/${studentId}`),
        teacherFetch<Assignment[]>(`/v1/learning/assignments/class/${classGroupId}`),
        teacherFetch<Assessment[]>(`/v1/assessments/class/${classGroupId}`),
      ])

      if (attendanceData.status === "fulfilled") {
        setAttendance(attendanceData.value)
      }
      setLoadingAttendance(false)

      if (progressData.status === "fulfilled") {
        setProgress(progressData.value)
      }
      setLoadingProgress(false)

      const loadedAssignments = assignmentsData.status === "fulfilled" ? assignmentsData.value : []
      setAssignments(loadedAssignments)
      setLoadingAssignments(false)

      const loadedAssessments = assessmentsData.status === "fulfilled" ? assessmentsData.value : []
      setAssessments(loadedAssessments)
      setLoadingAssessments(false)

      const subResults: AssignmentSubmission[] = []
      for (const assignment of loadedAssignments) {
        try {
          const subs = await teacherFetch<AssignmentSubmission[]>(`/v1/learning/assignments/${assignment.id}/submissions`)
          const studentSubs = subs.filter((s) => s.studentId === studentId)
          subResults.push(...studentSubs)
        } catch {
          // skip
        }
      }
      setSubmissions(subResults)

      const resResults: AssessmentResult[] = []
      for (const assessment of loadedAssessments) {
        try {
          const results = await teacherFetch<AssessmentResult[]>(`/v1/assessments/${assessment.id}/results`)
          const studentResults = results.filter((r) => r.studentId === studentId)
          resResults.push(...studentResults)
        } catch {
          // skip
        }
      }
      setAssessmentResults(resResults)
    } catch {
      setError("Failed to load student data")
    } finally {
      setLoadingProfile(false)
    }
  }, [allStudents])

  useEffect(() => {
    if (selectedStudentId) {
      loadStudentData(selectedStudentId)
    }
  }, [selectedStudentId, loadStudentData])

  const selectedStudent = allStudents.find((s) => s.studentId === selectedStudentId)

  const filteredStudents = allStudents.filter((s) =>
    s.fullName.toLowerCase().includes(search.toLowerCase()) ||
    s.email.toLowerCase().includes(search.toLowerCase()) ||
    s.admissionNumber.toLowerCase().includes(search.toLowerCase())
  )

  const classMap = new Map(classes.map((c) => [c.classGroupId, c]))

  const studentAssignments = assignments.map((a) => {
    const sub = submissions.find((s) => s.assignmentId === a.id)
    return { ...a, submission: sub }
  })

  const studentAssessments = assessments.map((a) => {
    const result = assessmentResults.find((r) => r.assessmentId === a.id)
    return { ...a, result }
  })

  const gradedAssignments = studentAssignments.filter((a) => a.submission?.grade !== undefined && a.submission?.grade !== null)
  const gradedAssessments = studentAssessments.filter((a) => a.result !== undefined)

  const avgAssignmentGrade = gradedAssignments.length > 0
    ? gradedAssignments.reduce((sum, a) => {
        const pct = (a.submission!.grade! / a.totalMarks) * 100
        return sum + pct
      }, 0) / gradedAssignments.length
    : null

  const avgAssessmentGrade = gradedAssessments.length > 0
    ? gradedAssessments.reduce((sum, a) => {
        const pct = (a.result!.totalScore / a.totalMarks) * 100
        return sum + pct
      }, 0) / gradedAssessments.length
    : null

  const overallAvg = avgAssignmentGrade !== null && avgAssessmentGrade !== null
    ? (avgAssignmentGrade + avgAssessmentGrade) / 2
    : avgAssignmentGrade ?? avgAssessmentGrade

  const recentSubmissions = [...submissions]
    .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())
    .slice(0, 5)

  const recentAssessments = [...assessmentResults]
    .sort((a, b) => new Date(b.gradedAt || 0).getTime() - new Date(a.gradedAt || 0).getTime())
    .slice(0, 5)

  const weakAreas: string[] = []
  if (attendance && attendance.attendancePercentage < 75) {
    weakAreas.push(`Attendance is low at ${attendance.attendancePercentage.toFixed(0)}%`)
  }
  if (attendance && attendance.absentDays > attendance.presentDays) {
    weakAreas.push(`More absences (${attendance.absentDays}) than present days (${attendance.presentDays})`)
  }
  if (overallAvg !== null && overallAvg < 50) {
    weakAreas.push(`Overall average is ${overallAvg.toFixed(0)}% — below passing`)
  }
  gradedAssignments.forEach((a) => {
    const pct = (a.submission!.grade! / a.totalMarks) * 100
    if (pct < 40) {
      weakAreas.push(`Low assignment grade on "${a.title}" (${a.submission!.grade}/${a.totalMarks})`)
    }
  })
  gradedAssessments.forEach((a) => {
    if (!a.result!.isPassed) {
      weakAreas.push(`Failed assessment "${a.title}" (${a.result!.totalScore}/${a.totalMarks})`)
    }
  })

  const isAnyLoading = loadingAttendance || loadingProgress || loadingAssignments || loadingAssessments

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Learner Support</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          View detailed information about individual students in your classes.
        </p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search students by name, email, or admission #..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <div className="relative">
          <Users className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <select
            value={selectedStudentId}
            onChange={(e) => setSelectedStudentId(e.target.value)}
            disabled={loadingStudents}
            className="h-10 rounded-lg border border-border bg-background pl-9 pr-8 text-sm outline-none focus:border-ring disabled:opacity-50"
          >
            <option value="">Select a student</option>
            {(search ? filteredStudents : allStudents).map((s) => (
              <option key={s.studentId} value={s.studentId}>
                {s.fullName} — {s.className}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loadingClasses || loadingStudents ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : allStudents.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Users className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Students Found</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            No students are enrolled in your assigned classes yet.
          </p>
        </div>
      ) : !selectedStudentId ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <GraduationCap className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Select a Student</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Choose a student from the dropdown above to view their detailed profile and performance.
          </p>
        </div>
      ) : !selectedStudent ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <AlertCircle className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">Student Not Found</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            The selected student could not be found.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Profile Card */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            {loadingProfile ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="size-5 animate-spin text-muted-foreground" />
              </div>
            ) : (
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                  <Users className="size-7" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-3">
                    <h2 className="text-xl font-bold text-foreground">{selectedStudent.fullName}</h2>
                    <span className={`inline-flex w-fit items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      selectedStudent.status === "ACTIVE" ? "bg-teal/10 text-teal" : "bg-muted text-muted-foreground"
                    }`}>
                      {selectedStudent.status}
                    </span>
                  </div>
                  <div className="mt-2 grid grid-cols-1 gap-x-6 gap-y-1.5 text-sm sm:grid-cols-2 lg:grid-cols-3">
                    <div className="flex items-center gap-2">
                      <span className="text-muted-foreground">Email:</span>
                      <span className="font-medium text-foreground">{selectedStudent.email}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-muted-foreground">Admission #:</span>
                      <span className="font-mono text-xs font-medium text-foreground">{selectedStudent.admissionNumber}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-muted-foreground">Class:</span>
                      <span className="font-medium text-foreground">{selectedStudent.className}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-muted-foreground">Subject:</span>
                      <span className="font-medium text-foreground">{selectedStudent.subjectName}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-muted-foreground">Gender:</span>
                      <span className="font-medium text-foreground">{selectedStudent.gender || "—"}</span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            {/* Attendance Overview */}
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-foreground">Attendance Overview</h2>
                <ClipboardCheck className="size-4 text-muted-foreground" />
              </div>
              {loadingAttendance ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-5 animate-spin text-muted-foreground" />
                </div>
              ) : !attendance ? (
                <div className="py-6 text-center text-sm text-muted-foreground">No attendance data available</div>
              ) : (
                <div className="mt-4 space-y-4">
                  <div className="flex items-end gap-3">
                    <span className="text-3xl font-extrabold text-foreground">
                      {attendance.attendancePercentage.toFixed(0)}%
                    </span>
                    <span className="mb-1 text-sm text-muted-foreground">attendance rate</span>
                  </div>
                  <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${getAttendanceBarColor(attendance.attendancePercentage)}`}
                      style={{ width: `${Math.min(attendance.attendancePercentage, 100)}%` }}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <div className="rounded-xl border border-border p-3 text-center">
                      <CheckCircle className="mx-auto mb-1 size-4 text-teal" />
                      <p className="text-lg font-bold text-foreground">{attendance.presentDays}</p>
                      <p className="text-xs text-muted-foreground">Present</p>
                    </div>
                    <div className="rounded-xl border border-border p-3 text-center">
                      <XCircle className="mx-auto mb-1 size-4 text-destructive" />
                      <p className="text-lg font-bold text-foreground">{attendance.absentDays}</p>
                      <p className="text-xs text-muted-foreground">Absent</p>
                    </div>
                    <div className="rounded-xl border border-border p-3 text-center">
                      <Clock className="mx-auto mb-1 size-4 text-amber-500" />
                      <p className="text-lg font-bold text-foreground">{attendance.lateDays}</p>
                      <p className="text-xs text-muted-foreground">Late</p>
                    </div>
                    <div className="rounded-xl border border-border p-3 text-center">
                      <AlertCircle className="mx-auto mb-1 size-4 text-primary" />
                      <p className="text-lg font-bold text-foreground">{attendance.excusedDays}</p>
                      <p className="text-xs text-muted-foreground">Excused</p>
                    </div>
                  </div>
                </div>
              )}
            </section>

            {/* Academic Performance */}
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-foreground">Academic Performance</h2>
                <BarChart3 className="size-4 text-muted-foreground" />
              </div>
              {loadingAssignments && loadingAssessments ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-5 animate-spin text-muted-foreground" />
                </div>
              ) : (
                <div className="mt-4 space-y-4">
                  <div className="grid grid-cols-3 gap-3">
                    <div className="rounded-xl border border-border p-3 text-center">
                      <p className="text-lg font-bold text-foreground">
                        {overallAvg !== null ? `${overallAvg.toFixed(0)}%` : "—"}
                      </p>
                      <p className="text-xs text-muted-foreground">Overall Average</p>
                    </div>
                    <div className="rounded-xl border border-border p-3 text-center">
                      <p className="text-lg font-bold text-foreground">
                        {avgAssignmentGrade !== null ? `${avgAssignmentGrade.toFixed(0)}%` : "—"}
                      </p>
                      <p className="text-xs text-muted-foreground">Assignment Avg</p>
                    </div>
                    <div className="rounded-xl border border-border p-3 text-center">
                      <p className="text-lg font-bold text-foreground">
                        {avgAssessmentGrade !== null ? `${avgAssessmentGrade.toFixed(0)}%` : "—"}
                      </p>
                      <p className="text-xs text-muted-foreground">Assessment Avg</p>
                    </div>
                  </div>

                  {gradedAssignments.length === 0 && gradedAssessments.length === 0 ? (
                    <p className="py-4 text-center text-sm text-muted-foreground">No graded work yet</p>
                  ) : (
                    <div className="space-y-2">
                      {studentAssignments.filter((a) => a.submission).slice(0, 4).map((a) => (
                        <div key={a.id} className="flex items-center justify-between rounded-xl border border-border p-2.5">
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-medium text-foreground">{a.title}</p>
                            <p className="text-xs text-muted-foreground">Assignment</p>
                          </div>
                          <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${getGradeColor(a.submission!.grade!, a.totalMarks)}`}>
                            {a.submission!.grade}/{a.totalMarks}
                          </span>
                        </div>
                      ))}
                      {studentAssessments.filter((a) => a.result).slice(0, 4).map((a) => (
                        <div key={a.id} className="flex items-center justify-between rounded-xl border border-border p-2.5">
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-medium text-foreground">{a.title}</p>
                            <p className="text-xs text-muted-foreground">Assessment</p>
                          </div>
                          <div className="flex items-center gap-2">
                            {a.result!.isPassed ? (
                              <CheckCircle className="size-3.5 text-teal" />
                            ) : (
                              <XCircle className="size-3.5 text-destructive" />
                            )}
                            <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${getGradeColor(a.result!.totalScore, a.totalMarks)}`}>
                              {a.result!.totalScore}/{a.totalMarks}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </section>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            {/* Recent Activity */}
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-foreground">Recent Activity</h2>
                <TrendingUp className="size-4 text-muted-foreground" />
              </div>
              <div className="mt-4 space-y-3">
                {loadingAssignments && loadingAssessments ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="size-5 animate-spin text-muted-foreground" />
                  </div>
                ) : recentSubmissions.length === 0 && recentAssessments.length === 0 ? (
                  <p className="py-4 text-center text-sm text-muted-foreground">No recent activity</p>
                ) : (
                  <>
                    {recentSubmissions.map((sub) => (
                      <div key={sub.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                        <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                          <FileText className="size-4 text-primary" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-foreground">
                            {assignments.find((a) => a.id === sub.assignmentId)?.title || "Assignment"}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            Submitted {formatDateTime(sub.submittedAt)}
                          </p>
                        </div>
                        {sub.grade !== undefined && sub.grade !== null && (
                          <span className="shrink-0 rounded-full bg-teal/10 px-2 py-0.5 text-[10px] font-semibold text-teal">
                            {sub.grade}
                          </span>
                        )}
                      </div>
                    ))}
                    {recentAssessments.map((res) => (
                      <div key={res.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                        <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
                          <ClipboardCheck className="size-4 text-amber-600" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-foreground">
                            {assessments.find((a) => a.id === res.assessmentId)?.title || "Assessment"}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {res.gradedAt ? `Graded ${formatDateTime(res.gradedAt)}` : "Pending"}
                          </p>
                        </div>
                        {res.isPassed ? (
                          <CheckCircle className="size-4 shrink-0 text-teal" />
                        ) : (
                          <XCircle className="size-4 shrink-0 text-destructive" />
                        )}
                        <span className="shrink-0 text-xs font-semibold text-foreground">
                          {res.totalScore}
                        </span>
                      </div>
                    ))}
                  </>
                )}
              </div>
            </section>

            {/* Learning Progress */}
            <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-foreground">Learning Progress</h2>
                <BookOpen className="size-4 text-muted-foreground" />
              </div>
              <div className="mt-4 space-y-3">
                {loadingProgress ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="size-5 animate-spin text-muted-foreground" />
                  </div>
                ) : progress.length === 0 ? (
                  <p className="py-4 text-center text-sm text-muted-foreground">No learning progress recorded</p>
                ) : (
                  <>
                    {(() => {
                      const totalLessons = progress.length
                      const completed = progress.filter((p) => p.completionPercentage >= 100).length
                      const inProgress = progress.filter((p) => p.completionPercentage > 0 && p.completionPercentage < 100).length
                      const notStarted = totalLessons - completed - inProgress
                      return (
                        <div className="grid grid-cols-3 gap-3">
                          <div className="rounded-xl border border-border p-3 text-center">
                            <p className="text-lg font-bold text-teal">{completed}</p>
                            <p className="text-xs text-muted-foreground">Completed</p>
                          </div>
                          <div className="rounded-xl border border-border p-3 text-center">
                            <p className="text-lg font-bold text-primary">{inProgress}</p>
                            <p className="text-xs text-muted-foreground">In Progress</p>
                          </div>
                          <div className="rounded-xl border border-border p-3 text-center">
                            <p className="text-lg font-bold text-muted-foreground">{notStarted}</p>
                            <p className="text-xs text-muted-foreground">Not Started</p>
                          </div>
                        </div>
                      )
                    })()}
                    {progress.slice(0, 5).map((p) => (
                      <div key={p.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                        <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${
                          p.completionPercentage >= 100 ? "bg-teal/10" : p.completionPercentage > 0 ? "bg-primary/10" : "bg-muted"
                        }`}>
                          {p.completionPercentage >= 100 ? (
                            <CheckCircle className="size-4 text-teal" />
                          ) : (
                            <BookOpen className="size-4 text-muted-foreground" />
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-foreground">Lesson Progress</p>
                          <p className="text-xs text-muted-foreground">
                            {p.completionPercentage}% complete
                            {p.completedAt && ` · Completed ${formatDate(p.completedAt)}`}
                          </p>
                        </div>
                        <div className="h-2 w-16 shrink-0 overflow-hidden rounded-full bg-muted">
                          <div
                            className={`h-full rounded-full ${p.completionPercentage >= 100 ? "bg-teal" : "bg-primary"}`}
                            style={{ width: `${p.completionPercentage}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </>
                )}
              </div>
            </section>
          </div>

          {/* Weak Areas */}
          <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-foreground">Areas Needing Attention</h2>
              <AlertTriangle className="size-4 text-muted-foreground" />
            </div>
            <div className="mt-4">
              {isAnyLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-5 animate-spin text-muted-foreground" />
                </div>
              ) : weakAreas.length === 0 ? (
                <div className="flex items-center gap-3 rounded-xl border border-teal/20 bg-teal/5 p-4">
                  <CheckCircle className="size-5 shrink-0 text-teal" />
                  <p className="text-sm text-foreground">
                    {selectedStudent?.fullName} is performing well across all areas. No immediate concerns.
                  </p>
                </div>
              ) : (
                <div className="space-y-2">
                  {weakAreas.map((area, i) => (
                    <div key={i} className="flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3">
                      <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" />
                      <p className="text-sm text-foreground">{area}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
