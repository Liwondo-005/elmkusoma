"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { useTranslations } from "next-intl"
import { collegeApi } from "@/lib/college-api"
import { learnerApi } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  User,
  GraduationCap,
  BookOpen,
  Award,
  FileText,
  Download,
  Calendar,
  Clock,
  TrendingUp,
  ChevronRight,
  AlertCircle,
  CheckCircle,
  Star,
  BarChart3,
  Briefcase,
} from "lucide-react"

interface SemesterGrades {
  semester: string
  academicYear: string
  courses: {
    courseId: string
    courseCode: string
    courseTitle: string
    grade: string
    gradePoints: number
    creditHours: number
    status: string
  }[]
  semesterGpa: number
  creditsEarned: number
}

interface StudentInfo {
  fullName: string
  studentId: string
  email: string
  programmeName: string
  departmentName: string
  enrollmentStatus: string
  yearOfStudy: number
  semester: string
  enrollmentDate: string
  academicAdvisor: string
}

export default function StudentPortalPage() {
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()
  const t = useTranslations("learner")
  const tc = useTranslations("common")

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [studentInfo, setStudentInfo] = useState<StudentInfo>({
    fullName: "",
    studentId: "",
    email: "",
    programmeName: "",
    departmentName: "",
    enrollmentStatus: "ACTIVE",
    yearOfStudy: 1,
    semester: "1",
    enrollmentDate: "",
    academicAdvisor: "",
  })

  const [enrollments, setEnrollments] = useState<any[]>([])
  const [academicRecord, setAcademicRecord] = useState<any>(null)
  const [academicRecords, setAcademicRecords] = useState<any[]>([])
  const [competencies, setCompetencies] = useState<any[]>([])
  const [research, setResearch] = useState<any[]>([])
  const [projects, setProjects] = useState<any[]>([])
  const [certificates, setCertificates] = useState<any[]>([])
  const [semesterGrades, setSemesterGrades] = useState<SemesterGrades[]>([])
  const [cumulativeGpa, setCumulativeGpa] = useState(0)
  const [totalCreditsEarned, setTotalCreditsEarned] = useState(0)
  const [totalCreditsRequired, setTotalCreditsRequired] = useState(120)

  const fetchData = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      setError(null)

      const studentId = user.id

      const [enrollmentsRes, recordRes, recordHistoryRes, competenciesRes, researchRes, projectsRes] = await Promise.allSettled([
        collegeApi.getStudentEnrollments(studentId),
        collegeApi.getAcademicRecord(studentId),
        collegeApi.getAcademicRecordHistory(studentId),
        collegeApi.getStudentCompetencies(studentId),
        collegeApi.getStudentResearch(studentId),
        collegeApi.getStudentProjects(studentId),
      ])

      const enrollmentsData = enrollmentsRes.status === "fulfilled" ? (enrollmentsRes.value as any) : []
      const recordData = recordRes.status === "fulfilled" ? (recordRes.value as any) : null
      const recordHistoryData = recordHistoryRes.status === "fulfilled" ? (recordHistoryRes.value as any) : []
      const competenciesData = competenciesRes.status === "fulfilled" ? (competenciesRes.value as any) : []
      const researchData = researchRes.status === "fulfilled" ? (researchRes.value as any) : []
      const projectsData = projectsRes.status === "fulfilled" ? (projectsRes.value as any) : []

      const enrollmentsList = Array.isArray(enrollmentsData) ? enrollmentsData : enrollmentsData?.data || []
      const recordHistoryList = Array.isArray(recordHistoryData) ? recordHistoryData : recordHistoryData?.data || []
      const competenciesList = Array.isArray(competenciesData) ? competenciesData : competenciesData?.data || []
      const researchList = Array.isArray(researchData) ? researchData : researchData?.data || []
      const projectsList = Array.isArray(projectsData) ? projectsData : projectsData?.data || []

      setEnrollments(enrollmentsList)
      setAcademicRecord(recordData?.data || recordData)
      setAcademicRecords(recordHistoryList)
      setCompetencies(competenciesList)
      setResearch(researchList)
      setProjects(projectsList)

      setStudentInfo({
        fullName: user.name || `${user.firstName || ""} ${user.lastName || ""}`.trim() || t("studentPortal.studentFallback"),
        studentId: user.id,
        email: user.email || "",
        programmeName: recordData?.data?.programmeId || user.learningLevel || "N/A",
        departmentName: t("studentPortal.defaultDepartment"),
        enrollmentStatus: "ACTIVE",
        yearOfStudy: recordData?.data?.academicYear ? parseInt(recordData.data.academicYear) || 1 : 1,
        semester: recordData?.data?.semester || "1",
        enrollmentDate: enrollmentsList[0]?.enrolledDate || "",
        academicAdvisor: t("studentPortal.defaultAdvisor"),
      })

      const semesterMap = new Map<string, SemesterGrades>()
      let totalGpaPoints = 0
      let totalCreditHours = 0
      let earnedCredits = 0

      for (const enrollment of enrollmentsList) {
        const semKey = `${enrollment.academicYear || "N/A"}-${enrollment.semester || "1"}`
        if (!semesterMap.has(semKey)) {
          semesterMap.set(semKey, {
            semester: enrollment.semester || "1",
            academicYear: enrollment.academicYear || "N/A",
            courses: [],
            semesterGpa: 0,
            creditsEarned: 0,
          })
        }
        const sem = semesterMap.get(semKey)!
        const courseCode = enrollment.courseId?.slice(0, 8)?.toUpperCase() || "GEN-001"
        const grade = enrollment.grade || "N/A"
        const gradePoints = enrollment.gradePoints || 0
        const creditHours = enrollment.creditHours || 3

        sem.courses.push({
          courseId: enrollment.courseId,
          courseCode,
          courseTitle: t("studentPortal.courseTitleFallback", { code: courseCode }),
          grade,
          gradePoints,
          creditHours,
          status: enrollment.status || "ENROLLED",
        })

        if (grade !== "N/A" && grade !== "IN_PROGRESS") {
          totalGpaPoints += gradePoints * creditHours
          totalCreditHours += creditHours
          earnedCredits += creditHours
          sem.creditsEarned += creditHours
        }
      }

      for (const [, sem] of semesterMap) {
        if (sem.creditsEarned > 0) {
          const semGpaPoints = sem.courses.reduce(
            (acc, c) => acc + (c.gradePoints || 0) * c.creditHours,
            0
          )
          sem.semesterGpa = sem.creditsEarned > 0
            ? Math.round((semGpaPoints / sem.creditsEarned) * 100) / 100
            : 0
        }
      }

      const sortedSemesters = Array.from(semesterMap.values()).sort((a, b) => {
        if (a.academicYear !== b.academicYear) return b.academicYear.localeCompare(a.academicYear)
        return b.semester.localeCompare(a.semester)
      })

      setSemesterGrades(sortedSemesters)
      setCumulativeGpa(totalCreditHours > 0 ? Math.round((totalGpaPoints / totalCreditHours) * 100) / 100 : 0)
      setTotalCreditsEarned(earnedCredits)
    } catch (err: any) {
      setError(err.message || t("studentPortal.loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [user, t])

  useEffect(() => {
    if (!user) return
    fetchData()
  }, [user, fetchData])

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2)
  }

  const getGpaColor = (gpa: number) => {
    if (gpa >= 3.5) return "text-emerald-600"
    if (gpa >= 3.0) return "text-blue-600"
    if (gpa >= 2.0) return "text-amber-600"
    return "text-red-600"
  }

  const getGpaBg = (gpa: number) => {
    if (gpa >= 3.5) return "bg-emerald-50 border-emerald-200"
    if (gpa >= 3.0) return "bg-blue-50 border-blue-200"
    if (gpa >= 2.0) return "bg-amber-50 border-amber-200"
    return "bg-red-50 border-red-200"
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ACTIVE":
      case "ENROLLED":
        return "bg-emerald-100 text-emerald-700"
      case "ON_LEAVE":
        return "bg-amber-100 text-amber-700"
      case "GRADUATED":
        return "bg-blue-100 text-blue-700"
      case "COMPLETED":
        return "bg-emerald-100 text-emerald-700"
      case "PASSED":
        return "bg-emerald-100 text-emerald-700"
      case "FAILED":
        return "bg-red-100 text-red-700"
      case "IN_PROGRESS":
        return "bg-blue-100 text-blue-700"
      default:
        return "bg-gray-100 text-gray-700"
    }
  }

  const getGradeColor = (grade: string) => {
    if (["A", "A+", "A-", "B+", "B", "B-"].includes(grade)) return "text-emerald-600"
    if (["C+", "C", "C-"].includes(grade)) return "text-blue-600"
    if (["D+", "D"].includes(grade)) return "text-amber-600"
    return "text-red-600"
  }

  const getStanding = (gpa: number) => {
    if (gpa >= 3.5) return { label: t("studentPortal.standingDeansList"), color: "text-emerald-600 bg-emerald-50" }
    if (gpa >= 3.0) return { label: t("studentPortal.standingGood"), color: "text-blue-600 bg-blue-50" }
    if (gpa >= 2.0) return { label: t("studentPortal.standingSatisfactory"), color: "text-amber-600 bg-amber-50" }
    return { label: t("studentPortal.standingProbation"), color: "text-red-600 bg-red-50" }
  }

  const progressPercentage = totalCreditsRequired > 0
    ? Math.min(Math.round((totalCreditsEarned / totalCreditsRequired) * 100), 100)
    : 0

  const completedSemesters = semesterGrades.filter((s) =>
    s.courses.some((c) => c.status === "COMPLETED" || c.status === "PASSED")
  )

  const currentEnrollments = enrollments.filter(
    (e) => e.status === "ENROLLED" || e.status === "IN_PROGRESS"
  )

  const competentCount = competencies.filter(
    (c) => c.status === "COMPETENT" || c.status === "COMPLETED"
  ).length

  const standing = getStanding(cumulativeGpa)

  const quickActions = [
    { label: t("studentPortal.actionCalendar"), icon: Calendar, href: "/dashboard/learner/calendar-integration" },
    { label: t("studentPortal.actionResearch"), icon: BookOpen, href: "/dashboard/learner/research" },
    { label: t("studentPortal.actionProjects"), icon: Briefcase, href: "/dashboard/learner/projects" },
    { label: t("studentPortal.actionCareer"), icon: GraduationCap, href: "/dashboard/learner/career" },
  ]

  if (authLoading || loading) return <LoadingState />

  if (!user) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <EmptyState
          icon={<User className="size-6" />}
          title={t("studentPortal.loginTitle")}
          description={t("studentPortal.loginDescription")}
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader
        firstName={studentInfo.fullName.split(" ")[0] || t("studentPortal.studentFallback")}
        subtitle={t("studentPortal.headerSubtitle")}
      />

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {/* Student Profile Card */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex flex-col gap-6 sm:flex-row">
          <div className="flex shrink-0 items-start gap-4">
            <div className="flex size-16 items-center justify-center rounded-full bg-primary/10 text-lg font-bold text-primary">
              {getInitials(studentInfo.fullName)}
            </div>
            <div className="sm:hidden">
              <h2 className="text-lg font-bold text-foreground">{studentInfo.fullName}</h2>
              <p className="text-xs text-muted-foreground">{t("studentPortal.idLabel", { id: studentInfo.studentId.slice(0, 8).toUpperCase() })}</p>
            </div>
          </div>
          <div className="flex-1 space-y-3">
            <div className="hidden sm:block">
              <h2 className="text-lg font-bold text-foreground">{studentInfo.fullName}</h2>
              <p className="text-xs text-muted-foreground">{t("studentPortal.idLabel", { id: studentInfo.studentId.slice(0, 8).toUpperCase() })}</p>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldEmail")}</p>
                <p className="font-medium text-foreground">{studentInfo.email || "N/A"}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldProgramme")}</p>
                <p className="font-medium text-foreground">{studentInfo.programmeName}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldDepartment")}</p>
                <p className="font-medium text-foreground">{studentInfo.departmentName}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldStatus")}</p>
                <span className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(studentInfo.enrollmentStatus)}`}>
                  {studentInfo.enrollmentStatus}
                </span>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldYearSemester")}</p>
                <p className="font-medium text-foreground">{t("studentPortal.yearSem", { year: studentInfo.yearOfStudy, semester: studentInfo.semester })}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldEnrollmentDate")}</p>
                <p className="font-medium text-foreground">
                  {studentInfo.enrollmentDate
                    ? new Date(studentInfo.enrollmentDate).toLocaleDateString()
                    : "N/A"}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldAdvisor")}</p>
                <p className="font-medium text-foreground">{studentInfo.academicAdvisor}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("studentPortal.fieldStanding")}</p>
                <span className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold ${standing.color}`}>
                  {standing.label}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Academic Progress Dashboard */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
          <BarChart3 className="size-4 text-primary" />
          {t("studentPortal.progressTitle")}
        </h3>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <div className={`rounded-xl border p-4 ${getGpaBg(cumulativeGpa)}`}>
            <p className="text-xs font-medium text-muted-foreground">{t("studentPortal.overallGpa")}</p>
            <p className={`mt-1 text-3xl font-extrabold ${getGpaColor(cumulativeGpa)}`}>
              {cumulativeGpa.toFixed(2)}
            </p>
            <p className="mt-1 text-[10px] text-muted-foreground">{t("studentPortal.outOfFour")}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4">
            <p className="text-xs font-medium text-muted-foreground">{t("studentPortal.creditsCompleted")}</p>
            <p className="mt-1 text-3xl font-extrabold text-foreground">{totalCreditsEarned}</p>
            <p className="mt-1 text-[10px] text-muted-foreground">{t("studentPortal.ofRequired", { total: totalCreditsRequired })}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4">
            <p className="text-xs font-medium text-muted-foreground">{t("studentPortal.currentCourses")}</p>
            <p className="mt-1 text-3xl font-extrabold text-foreground">{currentEnrollments.length}</p>
            <p className="mt-1 text-[10px] text-muted-foreground">{t("studentPortal.activeEnrollments")}</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4">
            <p className="text-xs font-medium text-muted-foreground">{t("studentPortal.completedSemesters")}</p>
            <p className="mt-1 text-3xl font-extrabold text-foreground">{completedSemesters.length}</p>
            <p className="mt-1 text-[10px] text-muted-foreground">{t("studentPortal.semestersFinished")}</p>
          </div>
        </div>

        <div className="mt-4">
          <div className="flex items-center justify-between text-xs">
            <span className="text-muted-foreground">{t("studentPortal.degreeProgress")}</span>
            <span className="font-semibold text-foreground">{progressPercentage}%</span>
          </div>
          <div className="mt-1.5 h-3 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-primary transition-all"
              style={{ width: `${progressPercentage}%` }}
            />
          </div>
          <div className="mt-1 flex justify-between text-[10px] text-muted-foreground">
            <span>{t("studentPortal.creditsEarned", { count: totalCreditsEarned })}</span>
            <span>{t("studentPortal.creditsRemaining", { count: totalCreditsRequired - totalCreditsEarned })}</span>
          </div>
        </div>

        {completedSemesters.length > 0 && (
          <div className="mt-4">
            <p className="mb-2 text-xs font-medium text-muted-foreground">{t("studentPortal.completedSemesters")}</p>
            <div className="flex flex-wrap gap-2">
              {completedSemesters.map((sem, idx) => (
                <span
                  key={idx}
                  className="rounded-full border border-border bg-muted/50 px-3 py-1 text-xs font-medium text-foreground"
                >
                  {t("studentPortal.semChip", { year: sem.academicYear, semester: sem.semester, gpa: sem.semesterGpa.toFixed(2) })}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Grade Summary */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
          <Award className="size-4 text-primary" />
          {t("studentPortal.gradeSummary")}
        </h3>
        {semesterGrades.length === 0 ? (
          <EmptyState
            icon={<BookOpen className="size-5" />}
            title={t("studentPortal.noGrades")}
            description={t("studentPortal.noGradesHint")}
          />
        ) : (
          <div className="space-y-4">
            {semesterGrades.map((sem, semIdx) => (
              <div key={semIdx} className="rounded-xl border border-border bg-muted/20 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <h4 className="text-sm font-semibold text-foreground">
                    {t("studentPortal.semTitleYear", { year: sem.academicYear, semester: sem.semester })}
                  </h4>
                  <div className="flex items-center gap-3 text-xs">
                    <span className="text-muted-foreground">
                      {t("studentPortal.gpaLabel")}: <span className={`font-bold ${getGpaColor(sem.semesterGpa)}`}>{sem.semesterGpa.toFixed(2)}</span>
                    </span>
                    <span className="text-muted-foreground">
                      {t("studentPortal.creditsLabel")}: <span className="font-bold text-foreground">{sem.creditsEarned}</span>
                    </span>
                  </div>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="border-b border-border text-left text-muted-foreground">
                        <th className="pb-2 pr-4 font-medium">{t("studentPortal.colCourse")}</th>
                        <th className="pb-2 pr-4 font-medium">{t("studentPortal.colGrade")}</th>
                        <th className="pb-2 pr-4 font-medium">{t("studentPortal.colPoints")}</th>
                        <th className="pb-2 pr-4 font-medium">{t("studentPortal.colCredits")}</th>
                        <th className="pb-2 font-medium">{t("studentPortal.colStatus")}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sem.courses.map((course, cIdx) => (
                        <tr key={cIdx} className="border-b border-border/50 last:border-0">
                          <td className="py-2 pr-4">
                            <span className="font-medium text-foreground">{course.courseCode}</span>
                            <span className="ml-2 text-muted-foreground">{course.courseTitle}</span>
                          </td>
                          <td className={`py-2 pr-4 font-semibold ${getGradeColor(course.grade)}`}>
                            {course.grade}
                          </td>
                          <td className="py-2 pr-4 text-foreground">{course.gradePoints.toFixed(1)}</td>
                          <td className="py-2 pr-4 text-foreground">{course.creditHours}</td>
                          <td className="py-2">
                            <span className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(course.status)}`}>
                              {course.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}

            <div className="rounded-xl border border-border bg-muted/30 p-4">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold text-foreground">{t("studentPortal.cumulativeSummary")}</span>
                <div className="flex items-center gap-4 text-xs">
                  <span className="text-muted-foreground">
                    {t("studentPortal.gpaLabel")}: <span className={`font-bold ${getGpaColor(cumulativeGpa)}`}>{cumulativeGpa.toFixed(2)}</span>
                  </span>
                  <span className="text-muted-foreground">
                    {t("studentPortal.totalCreditsLabel")}: <span className="font-bold text-foreground">{totalCreditsEarned}</span>
                  </span>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${standing.color}`}>
                    {standing.label}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Academic Transcript */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs" id="transcript">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="flex items-center gap-2 text-base font-semibold text-foreground">
            <FileText className="size-4 text-primary" />
            {t("studentPortal.transcriptTitle")}
          </h3>
          <button
            onClick={() => window.print()}
            className="flex items-center gap-1.5 rounded-lg border border-border bg-muted/50 px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
          >
            <Download className="size-3.5" />
            {tc("download")}
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-border text-left text-muted-foreground">
                <th className="pb-2 pr-4 font-medium">{t("studentPortal.colCourse")}</th>
                <th className="pb-2 pr-4 font-medium">{t("studentPortal.colCredits")}</th>
                <th className="pb-2 pr-4 font-medium">{t("studentPortal.colGrade")}</th>
                <th className="pb-2 pr-4 font-medium">{t("studentPortal.colPoints")}</th>
                <th className="pb-2 font-medium">{t("studentPortal.colSemester")}</th>
              </tr>
            </thead>
            <tbody>
              {semesterGrades.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-muted-foreground">
                    {t("studentPortal.noTranscriptData")}
                  </td>
                </tr>
              ) : (
                semesterGrades.flatMap((sem) =>
                  sem.courses.map((course, cIdx) => (
                    <tr key={`${sem.academicYear}-${sem.semester}-${cIdx}`} className="border-b border-border/50 last:border-0">
                      <td className="py-2 pr-4">
                        <span className="font-medium text-foreground">{course.courseCode}</span>
                        <span className="ml-2 text-muted-foreground">{course.courseTitle}</span>
                      </td>
                      <td className="py-2 pr-4 text-foreground">{course.creditHours}</td>
                      <td className={`py-2 pr-4 font-semibold ${getGradeColor(course.grade)}`}>
                        {course.grade}
                      </td>
                      <td className="py-2 pr-4 text-foreground">{course.gradePoints.toFixed(1)}</td>
                      <td className="py-2 text-muted-foreground">
                        {t("studentPortal.semTitleShort", { year: sem.academicYear, semester: sem.semester })}
                      </td>
                    </tr>
                  ))
                )
              )}
            </tbody>
            {semesterGrades.length > 0 && (
              <tfoot>
                <tr className="border-t-2 border-border">
                  <td className="pt-3 pr-4 font-bold text-foreground">{t("studentPortal.cumulativeGpa")}</td>
                  <td className="pt-3 pr-4 font-bold text-foreground">{totalCreditsEarned}</td>
                  <td className={`pt-3 pr-4 font-bold ${getGpaColor(cumulativeGpa)}`}>{cumulativeGpa.toFixed(2)}</td>
                  <td className="pt-3 pr-4 font-bold text-foreground">—</td>
                  <td className="pt-3 text-muted-foreground">{t("studentPortal.allSemesters")}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </div>

      {/* Current Enrollments */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
          <BookOpen className="size-4 text-primary" />
          {t("studentPortal.currentEnrollments")}
        </h3>
        {currentEnrollments.length === 0 ? (
          <EmptyState
            icon={<BookOpen className="size-5" />}
            title={t("studentPortal.noActiveEnrollments")}
            description={t("studentPortal.noActiveEnrollmentsHint")}
          />
        ) : (
          <div className="space-y-3">
            {currentEnrollments.map((enrollment, idx) => (
              <div
                key={enrollment.id || idx}
                className="flex items-center gap-4 rounded-xl border border-border p-4 transition-colors hover:bg-muted/30"
              >
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <BookOpen className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">
                    {enrollment.courseId?.slice(0, 8)?.toUpperCase() || t("studentPortal.courseFallback")}
                  </p>
                  <div className="mt-0.5 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                    {enrollment.instructorId && (
                      <span className="flex items-center gap-1">
                        <User className="size-3" />
                        {t("studentPortal.instructorLabel")}
                      </span>
                    )}
                    {enrollment.creditHours && (
                      <span className="flex items-center gap-1">
                        <Clock className="size-3" />
                        {t("studentPortal.creditsValue", { count: enrollment.creditHours })}
                      </span>
                    )}
                    {enrollment.semester && (
                      <span>{t("studentPortal.semShort", { semester: enrollment.semester })}</span>
                    )}
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  {enrollment.grade && (
                    <span className={`text-sm font-bold ${getGradeColor(enrollment.grade)}`}>
                      {enrollment.grade}
                    </span>
                  )}
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${getStatusBadge(enrollment.status)}`}>
                    {enrollment.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Achievements & Awards */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
          <Star className="size-4 text-primary" />
          {t("studentPortal.achievementsTitle")}
        </h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {/* Dean's List */}
          <div className="rounded-xl border border-border bg-muted/20 p-4">
            <div className="mb-2 flex items-center gap-2">
              <Award className="size-4 text-amber-500" />
              <span className="text-sm font-semibold text-foreground">{t("studentPortal.standingDeansList")}</span>
            </div>
            {cumulativeGpa >= 3.5 ? (
              <p className="text-xs text-muted-foreground">
                {t("studentPortal.deansListQualified", { gpa: cumulativeGpa.toFixed(2) })}
              </p>
            ) : (
              <p className="text-xs text-muted-foreground">
                {t("studentPortal.deansListHint")}
              </p>
            )}
          </div>

          {/* Certifications */}
          <div className="rounded-xl border border-border bg-muted/20 p-4">
            <div className="mb-2 flex items-center gap-2">
              <CheckCircle className="size-4 text-emerald-500" />
              <span className="text-sm font-semibold text-foreground">{t("studentPortal.certifications")}</span>
            </div>
            {certificates.length > 0 ? (
              <ul className="space-y-1">
                {certificates.slice(0, 3).map((cert, idx) => (
                  <li key={idx} className="text-xs text-muted-foreground">
                    {cert.title || cert.name || t("studentPortal.certificateFallback")}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-muted-foreground">
                {t("studentPortal.noCertificationsHint")}
              </p>
            )}
          </div>

          {/* Competency Badges */}
          <div className="rounded-xl border border-border bg-muted/20 p-4">
            <div className="mb-2 flex items-center gap-2">
              <TrendingUp className="size-4 text-blue-500" />
              <span className="text-sm font-semibold text-foreground">{t("studentPortal.competencyBadges")}</span>
            </div>
            {competencies.length > 0 ? (
              <div>
                <p className="text-xs text-muted-foreground">
                  {t("studentPortal.competenciesAchieved", { achieved: competentCount, total: competencies.length })}
                </p>
                <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-blue-500 transition-all"
                    style={{
                      width: `${competencies.length > 0 ? Math.round((competentCount / competencies.length) * 100) : 0}%`,
                    }}
                  />
                </div>
              </div>
            ) : (
              <p className="text-xs text-muted-foreground">
                {t("studentPortal.noCompetenciesHint")}
              </p>
            )}
          </div>

          {/* Research Contributions */}
          <div className="rounded-xl border border-border bg-muted/20 p-4">
            <div className="mb-2 flex items-center gap-2">
              <BookOpen className="size-4 text-purple-500" />
              <span className="text-sm font-semibold text-foreground">{t("studentPortal.researchContributions")}</span>
            </div>
            {research.length > 0 ? (
              <ul className="space-y-1">
                {research.slice(0, 3).map((r, idx) => (
                  <li key={idx} className="text-xs text-muted-foreground">
                    {r.title} — <span className={`font-medium ${getStatusBadge(r.status).split(" ").pop()}`}>{r.status}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-muted-foreground">
                {t("studentPortal.noResearchHint")}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
          <TrendingUp className="size-4 text-primary" />
          {t("studentPortal.quickActions")}
        </h3>
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          {quickActions.map((action) => (
            <button
              key={action.href}
              onClick={() => router.push(action.href)}
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-left transition-colors hover:bg-muted/50"
            >
              <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <action.icon className="size-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-foreground">{action.label}</p>
              </div>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
