"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { AcademicRecord, StudentCourseEnrollment, CompetencySummary } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { TrendingUp, GraduationCap, Target, BookOpen, AlertCircle } from "lucide-react"

export default function AcademicProgressPage() {
  const { user, loading: authLoading } = useAuth()
  const [record, setRecord] = useState<AcademicRecord | null>(null)
  const [enrollments, setEnrollments] = useState<StudentCourseEnrollment[]>([])
  const [competencySummary, setCompetencySummary] = useState<CompetencySummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const [recordRes, enrollRes, compRes] = await Promise.allSettled([
        collegeApi.getAcademicRecord(studentId),
        collegeApi.getStudentEnrollments(studentId),
        collegeApi.getCompetencySummary(studentId),
      ])
      if (recordRes.status === "fulfilled") setRecord((recordRes.value.data as AcademicRecord) || null)
      if (enrollRes.status === "fulfilled") setEnrollments((enrollRes.value.data as StudentCourseEnrollment[]) || [])
      if (compRes.status === "fulfilled") setCompetencySummary((compRes.value.data as CompetencySummary) || null)
    } catch {
      setError("Failed to load academic progress")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle="Track your GPA, course completion, and competency progress." />
        <div className="rounded-2xl border border-border bg-card p-4 text-sm text-red-600 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadData() }} className="ml-auto text-xs underline">Retry</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const completedCourses = enrollments.filter((e) => e.status === "COMPLETED").length
  const activeCourses = enrollments.filter((e) => e.status === "ENROLLED" || e.status === "IN_PROGRESS").length
  const compCompleted = competencySummary?.competent ?? 0
  const compTotal = competencySummary?.total ?? 0

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Track your GPA, course completion, and competency progress." />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <TrendingUp className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Cumulative GPA</p>
              <p className="text-2xl font-extrabold text-foreground">{record?.cumulativeGpa?.toFixed(2) ?? "N/A"}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <GraduationCap className="size-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Courses Completed</p>
              <p className="text-2xl font-extrabold text-foreground">{completedCourses} / {enrollments.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <Target className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Competencies</p>
              <p className="text-2xl font-extrabold text-foreground">{compCompleted} / {compTotal}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
              <BookOpen className="size-5 text-amber-600 dark:text-amber-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Credit Hours</p>
              <p className="text-2xl font-extrabold text-foreground">{record?.earnedCreditHours ?? 0} / {record?.totalCreditHours ?? 0}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground">Academic Standing</h3>
          <p className="mt-2 text-2xl font-extrabold text-foreground">{record?.academicStanding || "Not Available"}</p>
          {record?.classRank && record?.totalStudentsInClass && (
            <p className="mt-1 text-xs text-muted-foreground">
              Ranked {record.classRank} of {record.totalStudentsInClass} students
            </p>
          )}
          <div className="mt-3 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted-foreground">Semester GPA</span>
              <span className="font-semibold text-foreground">{record?.semesterGpa?.toFixed(2) ?? "N/A"}</span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted-foreground">Completed Courses</span>
              <span className="font-semibold text-foreground">{record?.completedCourses ?? 0}</span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted-foreground">Failed Courses</span>
              <span className="font-semibold text-foreground">{record?.failedCourses ?? 0}</span>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground">Competency Progress</h3>
          {compTotal > 0 ? (
            <div className="mt-3">
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">Progress</span>
                <span className="font-semibold text-foreground">{Math.round((compCompleted / compTotal) * 100)}%</span>
              </div>
              <div className="mt-2 h-3 w-full overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full bg-emerald-500 transition-all" style={{ width: `${(compCompleted / compTotal) * 100}%` }} />
              </div>
              <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                <div className="flex items-center justify-between"><span className="text-muted-foreground">Learning</span><span className="font-medium">{competencySummary?.learning ?? 0}</span></div>
                <div className="flex items-center justify-between"><span className="text-muted-foreground">Practicing</span><span className="font-medium">{competencySummary?.practicing ?? 0}</span></div>
                <div className="flex items-center justify-between"><span className="text-muted-foreground">Assessed</span><span className="font-medium">{competencySummary?.assessed ?? 0}</span></div>
                <div className="flex items-center justify-between"><span className="text-muted-foreground">Needs Practice</span><span className="font-medium">{competencySummary?.needsPractice ?? 0}</span></div>
              </div>
            </div>
          ) : (
            <p className="mt-3 text-sm text-muted-foreground">No competency data available yet.</p>
          )}
        </div>
      </div>

      {enrollments.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground mb-3">Enrollment Status</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border">
                <tr>
                  <th className="pb-2 font-medium text-muted-foreground">Course</th>
                  <th className="pb-2 font-medium text-muted-foreground">Semester</th>
                  <th className="pb-2 font-medium text-muted-foreground">Status</th>
                  <th className="pb-2 font-medium text-muted-foreground">Grade</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {enrollments.map((e) => (
                  <tr key={e.id}>
                    <td className="py-2 text-foreground">{e.courseId}</td>
                    <td className="py-2 text-muted-foreground">{e.semester || "—"}</td>
                    <td className="py-2">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-medium ${
                        e.status === "COMPLETED" ? "bg-emerald-100 text-emerald-700" :
                        e.status === "ENROLLED" ? "bg-blue-100 text-blue-700" :
                        "bg-amber-100 text-amber-700"
                      }`}>{e.status}</span>
                    </td>
                    <td className="py-2 font-medium text-foreground">{e.grade || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!record && enrollments.length === 0 && !competencySummary && (
        <EmptyState
          icon={<TrendingUp className="size-8" />}
          title="No academic data yet"
          description="Your academic record will appear here once courses and assessments are recorded."
        />
      )}
    </div>
  )
}
