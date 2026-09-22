"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  AlertCircle,
  BookOpen,
  Calendar,
  Users,
  TrendingUp,
  Award,
  CheckCircle,
  Clock,
} from "lucide-react"

interface CourseWorkspaceItem {
  id: string
  courseTitle: string
  courseCode: string
  status: string
  progressPercent: number
  creditHours: number
  semester: string
  academicYear: string
  lessonsCompleted: number
  totalLessons: number
  assignmentsCompleted: number
  totalAssignments: number
  assessmentsCompleted: number
  totalAssessments: number
  currentGrade: string
  gradePoints?: number
}

export default function CourseWorkspacePage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user } = useAuth()
  const [courses, setCourses] = useState<CourseWorkspaceItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      setError(null)
      const res = await collegeApi.getStudentEnrollments(user.id)
      const data = (res as any)?.data ?? res
      const enrollments = Array.isArray(data) ? data : Array.isArray(data?.data) ? data.data : []
      const mapped: CourseWorkspaceItem[] = enrollments.map((e: any) => ({
        id: e.id,
        courseTitle: e.courseTitle ?? e.courseName ?? e.title ?? tc("untitledCourse"),
        courseCode: e.courseCode ?? e.code ?? "",
        status: e.status ?? "ENROLLED",
        progressPercent: e.progressPercent ?? e.progressPercentage ?? 0,
        creditHours: e.creditHours ?? e.creditHours ?? 0,
        semester: e.semester ?? "",
        academicYear: e.academicYear ?? "",
        lessonsCompleted: e.lessonsCompleted ?? 0,
        totalLessons: e.totalLessons ?? 0,
        assignmentsCompleted: e.assignmentsCompleted ?? 0,
        totalAssignments: e.totalAssignments ?? 0,
        assessmentsCompleted: e.assessmentsCompleted ?? 0,
        totalAssessments: e.totalAssessments ?? 0,
        currentGrade: e.currentGrade ?? e.grade ?? "",
        gradePoints: e.gradePoints,
      }))
      setCourses(mapped)
    } catch (err) {
      console.error("Failed to load course workspace:", err)
      setError(tc("error.load"))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    loadData()
  }, [loadData])

  function toggleExpand(id: string) {
    setExpandedId((prev) => (prev === id ? null : id))
  }

  function getStatusColor(status: string) {
    switch (status) {
      case "COMPLETED":
        return "bg-teal/10 text-teal"
      case "IN_PROGRESS":
      case "ENROLLED":
        return "bg-blue-500/10 text-blue-500"
      case "DROPPED":
      case "WITHDRAWN":
        return "bg-orange/10 text-orange"
      case "FAILED":
        return "bg-red-500/10 text-red-500"
      default:
        return "bg-muted text-muted-foreground"
    }
  }

  function getStatusLabel(status: string) {
    return status.replace(/_/g, " ")
  }

  const activeCourses = courses.filter((c) => c.status !== "COMPLETED")
  const completedCourses = courses.filter((c) => c.status === "COMPLETED")

  const totalCourses = courses.length
  const inProgressCount = activeCourses.length
  const completedCount = completedCourses.length
  const gpaCourses = courses.filter((c) => c.gradePoints != null && c.gradePoints > 0)
  const averageGpa =
    gpaCourses.length > 0
      ? (gpaCourses.reduce((sum, c) => sum + (c.gradePoints ?? 0), 0) / gpaCourses.length).toFixed(2)
      : "--"

  if (loading) return <div role="main"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader
        firstName={user?.firstName ?? "Student"}
        subtitle={t("courseWorkspace.subtitle")}
      />

      {error && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-red-500/10">
              <AlertCircle className="size-5 text-red-500" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-foreground">{t("courseWorkspace.errorTitle")}</p>
              <p className="text-xs text-muted-foreground">{error}</p>
            </div>
            <button
              onClick={loadData}
              aria-label={tc("retry")}
              className="shrink-0 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              {tc("retry")}
            </button>
          </div>
        </div>
      )}

      {courses.length === 0 && !error ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title={t("courseWorkspace.noCourses")}
          description={t("courseWorkspace.noCoursesDesc")}
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                  <BookOpen className="size-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{totalCourses}</p>
                  <p className="text-xs text-muted-foreground">{t("courseWorkspace.totalCourses")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-blue-500/10">
                  <Clock className="size-5 text-blue-500" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{inProgressCount}</p>
                  <p className="text-xs text-muted-foreground">{t("courseWorkspace.inProgress")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-teal/10">
                  <CheckCircle className="size-5 text-teal" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{completedCount}</p>
                  <p className="text-xs text-muted-foreground">{t("courseWorkspace.completed")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-xl bg-orange/10">
                  <Award className="size-5 text-orange" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{averageGpa}</p>
                  <p className="text-xs text-muted-foreground">{t("courseWorkspace.avgGpa")}</p>
                </div>
              </div>
            </div>
          </div>

          {activeCourses.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-4">
                <TrendingUp className="size-4 text-primary" />
                <h3 className="text-sm font-semibold text-foreground">{t("courseWorkspace.activeCourses")}</h3>
              </div>
              <div className="space-y-3">
                {activeCourses.map((course) => (
                  <div key={course.id} className="rounded-xl border border-border bg-muted/50 overflow-hidden">
                    <button
                      onClick={() => toggleExpand(course.id)}
                      aria-label={`${course.courseTitle} - ${expandedId === course.id ? tc("collapse") : tc("expand")}`}
                      className="w-full p-4 text-left hover:bg-muted/30 transition-colors"
                    >
                      <div className="flex items-start justify-between">
                        <div className="min-w-0 flex-1">
                          <h4 className="text-sm font-medium text-foreground truncate">{course.courseTitle}</h4>
                          <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                            {course.courseCode && <span className="font-mono">{course.courseCode}</span>}
                            <span>{course.creditHours} credits</span>
                            {course.semester && (
                              <span className="flex items-center gap-1">
                                <Calendar className="size-3" />
                                {course.semester} {course.academicYear}
                              </span>
                            )}
                          </div>
                        </div>
                        <span
                          className={`shrink-0 text-xs px-2 py-1 rounded-full font-medium ${getStatusColor(
                            course.status
                          )}`}
                        >
                          {getStatusLabel(course.status)}
                        </span>
                      </div>
                      <div className="mt-3">
                        <div className="flex items-center justify-between text-xs">
                          <span className="text-muted-foreground">{tc("progress")}</span>
                          <span className="font-semibold text-primary">{course.progressPercent}%</span>
                        </div>
                        <div
                          role="progressbar"
                          aria-valuenow={course.progressPercent}
                          aria-valuemin={0}
                          aria-valuemax={100}
                          className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted"
                        >
                          <div
                            className="h-full rounded-full bg-primary transition-all"
                            style={{ width: `${course.progressPercent}%` }}
                          />
                        </div>
                      </div>
                    </button>

                    {expandedId === course.id && (
                      <div className="border-t border-border bg-card/50 px-4 py-4">
                        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                          <div className="rounded-xl border border-border bg-background p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                              <BookOpen className="size-3.5 text-primary" />
                              <span className="text-xs font-medium text-muted-foreground">{t("courseWorkspace.lessons")}</span>
                            </div>
                            <p className="text-lg font-bold text-foreground">
                              {course.lessonsCompleted}
                              <span className="text-xs font-normal text-muted-foreground">
                                /{course.totalLessons || 0}
                              </span>
                            </p>
                            <div
                              role="progressbar"
                              aria-valuenow={course.totalLessons > 0 ? Math.round((course.lessonsCompleted / course.totalLessons) * 100) : 0}
                              aria-valuemin={0}
                              aria-valuemax={100}
                              className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-muted"
                            >
                              <div
                                className="h-full rounded-full bg-primary transition-all"
                                style={{
                                  width: `${
                                    course.totalLessons > 0
                                      ? Math.round((course.lessonsCompleted / course.totalLessons) * 100)
                                      : 0
                                  }%`,
                                }}
                              />
                            </div>
                          </div>
                          <div className="rounded-xl border border-border bg-background p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                              <Users className="size-3.5 text-blue-500" />
                              <span className="text-xs font-medium text-muted-foreground">{t("courseWorkspace.assignments")}</span>
                            </div>
                            <p className="text-lg font-bold text-foreground">
                              {course.assignmentsCompleted}
                              <span className="text-xs font-normal text-muted-foreground">
                                /{course.totalAssignments || 0}
                              </span>
                            </p>
                            <div
                              role="progressbar"
                              aria-valuenow={course.totalAssignments > 0 ? Math.round((course.assignmentsCompleted / course.totalAssignments) * 100) : 0}
                              aria-valuemin={0}
                              aria-valuemax={100}
                              className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-muted"
                            >
                              <div
                                className="h-full rounded-full bg-blue-500 transition-all"
                                style={{
                                  width: `${
                                    course.totalAssignments > 0
                                      ? Math.round(
                                          (course.assignmentsCompleted / course.totalAssignments) * 100
                                        )
                                      : 0
                                  }%`,
                                }}
                              />
                            </div>
                          </div>
                          <div className="rounded-xl border border-border bg-background p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                              <Award className="size-3.5 text-orange" />
                              <span className="text-xs font-medium text-muted-foreground">{t("courseWorkspace.assessments")}</span>
                            </div>
                            <p className="text-lg font-bold text-foreground">
                              {course.assessmentsCompleted}
                              <span className="text-xs font-normal text-muted-foreground">
                                /{course.totalAssessments || 0}
                              </span>
                            </p>
                            <div
                              role="progressbar"
                              aria-valuenow={course.totalAssessments > 0 ? Math.round((course.assessmentsCompleted / course.totalAssessments) * 100) : 0}
                              aria-valuemin={0}
                              aria-valuemax={100}
                              className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-muted"
                            >
                              <div
                                className="h-full rounded-full bg-orange transition-all"
                                style={{
                                  width: `${
                                    course.totalAssessments > 0
                                      ? Math.round(
                                          (course.assessmentsCompleted / course.totalAssessments) * 100
                                        )
                                      : 0
                                  }%`,
                                }}
                              />
                            </div>
                          </div>
                          <div className="rounded-xl border border-border bg-background p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                              <CheckCircle className="size-3.5 text-teal" />
                              <span className="text-xs font-medium text-muted-foreground">{t("courseWorkspace.currentGrade")}</span>
                            </div>
                            <p className="text-lg font-bold text-foreground">
                              {course.currentGrade || "--"}
                            </p>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {completedCourses.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-4">
                <CheckCircle className="size-4 text-teal" />
                <h3 className="text-sm font-semibold text-foreground">{t("courseWorkspace.completedCourses")}</h3>
              </div>
              <div className="space-y-3">
                {completedCourses.map((course) => (
                  <div
                    key={course.id}
                    className="flex items-center justify-between rounded-xl border border-border bg-muted/50 p-4"
                  >
                    <div className="min-w-0 flex-1">
                      <h4 className="text-sm font-medium text-foreground truncate">{course.courseTitle}</h4>
                      <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                        {course.courseCode && <span className="font-mono">{course.courseCode}</span>}
                        <span>{course.creditHours} credits</span>
                        {course.semester && (
                          <span>
                            {course.semester} {course.academicYear}
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="shrink-0 text-right ml-4">
                      {course.currentGrade ? (
                        <span className="text-sm font-bold text-teal">{course.currentGrade}</span>
                      ) : (
                        <span className="text-xs text-muted-foreground">{t("courseWorkspace.noGrade")}</span>
                      )}
                      <div className="mt-0.5 h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                        <div className="h-full rounded-full bg-teal" style={{ width: "100%" }} />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
