"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment, type CourseSummary } from "@/lib/learner-api"
import { LoadingState, EmptyState } from "@/components/learner/shared"
import { Map, BookOpen, ArrowRight, AlertCircle, CheckCircle, Clock, Compass } from "lucide-react"

interface LearningPath {
  id: string
  title: string
  description: string
  courses: Array<{
    courseId: string
    courseTitle: string
    level: string
    status: "completed" | "in-progress" | "not-started"
    progress: number
  }>
  overallProgress: number
}

function deriveLearningPaths(enrollments: Enrollment[], allCourses: CourseSummary[]): LearningPath[] {
  const paths: LearningPath[] = []
  const enrolledCourseIds = new Set(enrollments.map((e) => e.courseId))

  const levelGroups: Record<string, CourseSummary[]> = {}
  for (const course of allCourses) {
    const level = course.level || "General"
    if (!levelGroups[level]) levelGroups[level] = []
    levelGroups[level].push(course)
  }

  for (const [level, courses] of Object.entries(levelGroups)) {
    const relevantCourses = courses.slice(0, 6)
    if (relevantCourses.length < 2) continue

    const pathCourses = relevantCourses.map((course) => {
      const enrollment = enrollments.find((e) => e.courseId === course.id)
      return {
        courseId: course.id,
        courseTitle: course.title,
        level: course.level,
        status: enrollment
          ? enrollment.completedAt
            ? ("completed" as const)
            : ("in-progress" as const)
          : ("not-started" as const),
        progress: enrollment?.progressPercentage || 0,
      }
    })

    const completedCount = pathCourses.filter((c) => c.status === "completed").length
    const inProgressCount = pathCourses.filter((c) => c.status === "in-progress").length
    const overallProgress = Math.round(
      pathCourses.reduce((sum, c) => sum + c.progress, 0) / pathCourses.length
    )

    paths.push({
      id: `level-${level}`,
      title: `${level} Learning Path`,
      description: `A structured progression through ${level.toLowerCase()} level courses.`,
      courses: pathCourses,
      overallProgress,
    })
  }

  if (enrollments.length >= 3) {
    const activeCourses = enrollments
      .filter((e) => !e.completedAt)
      .sort((a, b) => b.progressPercentage - a.progressPercentage)
      .slice(0, 5)

    if (activeCourses.length >= 2) {
      paths.unshift({
        id: "active-focus",
        title: "Active Learning Focus",
        description: "Your currently active courses prioritized by progress.",
        courses: activeCourses.map((e) => ({
          courseId: e.courseId,
          courseTitle: e.courseTitle,
          level: e.courseLevel || "General",
          status: "in-progress" as const,
          progress: e.progressPercentage,
        })),
        overallProgress: Math.round(
          activeCourses.reduce((sum, e) => sum + e.progressPercentage, 0) / activeCourses.length
        ),
      })
    }
  }

  return paths
}

export default function LearningPathsPage() {
  const { user, loading: authLoading } = useAuth()
  const [paths, setPaths] = useState<LearningPath[]>([])
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [enrollmentsData, coursesData] = await Promise.all([
        learnerApi.getEnrollments().catch(() => []),
        learnerApi.getCourses().catch(() => []),
      ])
      setEnrollments(enrollmentsData)
      setPaths(deriveLearningPaths(enrollmentsData, coursesData))
    } catch {
      setError("Failed to load learning paths")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Learning Paths</h1>
        <p className="mt-1 text-sm text-muted-foreground">Structured progressions through your learning journey.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      {loading ? (
        <LoadingState />
      ) : paths.length === 0 ? (
        <EmptyState
          icon={<Map className="size-8" />}
          title="No learning paths yet"
          description="Enroll in multiple courses to create structured learning paths."
          action={
            <Link
              href="/dashboard/learner/courses"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Explore Courses <ArrowRight className="size-4" />
            </Link>
          }
        />
      ) : (
        <div className="space-y-6">
          {paths.map((path) => (
            <div key={path.id} className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                    <Compass className="size-5 text-primary" />
                  </div>
                  <div>
                    <h2 className="text-base font-semibold text-foreground">{path.title}</h2>
                    <p className="text-xs text-muted-foreground">{path.description}</p>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-lg font-extrabold text-teal">{path.overallProgress}%</p>
                  <p className="text-[10px] text-muted-foreground">overall</p>
                </div>
              </div>

              <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-teal transition-all"
                  style={{ width: `${path.overallProgress}%` }}
                />
              </div>

              <div className="mt-4 space-y-2">
                {path.courses.map((course, idx) => (
                  <Link
                    key={course.courseId}
                    href={`/dashboard/learner/courses/${course.courseId}`}
                    className="flex items-center gap-3 rounded-xl border border-border px-4 py-3 transition-all hover:bg-muted/30 hover:border-primary/20"
                  >
                    <div className={`flex size-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                      course.status === "completed"
                        ? "bg-green-500/10 text-green-600"
                        : course.status === "in-progress"
                          ? "bg-primary/10 text-primary"
                          : "bg-muted text-muted-foreground"
                    }`}>
                      {course.status === "completed" ? (
                        <CheckCircle className="size-3.5" />
                      ) : (
                        idx + 1
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">{course.courseTitle}</p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[10px] text-muted-foreground">{course.level}</span>
                        {course.status === "in-progress" && (
                          <span className="text-[10px] text-primary">{course.progress}%</span>
                        )}
                      </div>
                    </div>
                    {course.status === "in-progress" && (
                      <div className="w-16 shrink-0">
                        <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full rounded-full bg-primary"
                            style={{ width: `${course.progress}%` }}
                          />
                        </div>
                      </div>
                    )}
                    {course.status === "completed" && (
                      <span className="shrink-0 rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                        Done
                      </span>
                    )}
                    {course.status === "not-started" && (
                      <span className="shrink-0 text-[10px] text-muted-foreground">Not started</span>
                    )}
                  </Link>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
