"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { courseApi, learningApi, type Course, type LessonProgress } from "@/lib/api"
import { BookOpen, ArrowRight } from "lucide-react"

export default function DashboardCoursesPage() {
  const { user } = useRequireAuth()
  const [courses, setCourses] = useState<Course[]>([])
  const [progress, setProgress] = useState<LessonProgress[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const [coursesData, progressData] = await Promise.all([
        courseApi.listCourses(user?.institutionId || "").catch(() => [] as Course[]),
        learningApi.getStudentProgress(user!.id).catch(() => []),
      ])
      setCourses(Array.isArray(coursesData) ? coursesData : [])
      setProgress(progressData)
    } catch {
      setCourses([])
    } finally {
      setLoading(false)
    }
  }

  function getCourseProgress(courseId: string): number {
    const courseLessons = progress.filter((p) => p.lessonId?.startsWith(courseId.slice(0, 8)))
    if (courseLessons.length === 0) return 0
    const avg = courseLessons.reduce((sum, p) => sum + (p.completionPercentage || 0), 0) / courseLessons.length
    return Math.round(avg)
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Courses</h1>
        <p className="mt-1 text-sm text-muted-foreground">Continue learning from where you left off.</p>
      </div>

      {courses.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Courses Yet</h3>
          <p className="mt-2 text-sm text-muted-foreground">You haven&apos;t been enrolled in any courses yet.</p>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => {
            const pct = getCourseProgress(course.id)
            return (
              <Link
                key={course.id}
                href={`/dashboard/lessons?courseId=${course.id}`}
                className="group overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="p-5">
                  <p className="text-xs font-medium text-primary">{course.level || "General"}</p>
                  <h3 className="mt-1 text-base font-semibold text-foreground">{course.title}</h3>
                  {course.description && (
                    <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{course.description}</p>
                  )}
                  <div className="mt-3 flex items-center gap-3">
                    <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-teal" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="text-xs font-semibold text-teal">{pct}%</span>
                  </div>
                  <div className="mt-3 flex items-center gap-1 text-xs text-primary font-medium">
                    Continue <ArrowRight className="size-3" />
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
