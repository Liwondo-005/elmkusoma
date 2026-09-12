"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type CourseSummary, type Enrollment } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { BookOpen, Search, ArrowRight, Loader2, AlertCircle, Filter } from "lucide-react"

export default function LearnerCoursesPage() {
  const { user, loading: authLoading } = useAuth()
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [levelFilter, setLevelFilter] = useState<string>("all")
  const [categoryFilter, setCategoryFilter] = useState<string>("all")

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      setError(null)
      const [coursesData, enrollmentsData] = await Promise.all([
        learnerApi.getCourses().catch(() => []),
        learnerApi.getEnrollments().catch(() => []),
      ])
      setCourses(coursesData)
      setEnrollments(enrollmentsData)
    } catch {
      setError("Failed to load courses")
    } finally {
      setLoading(false)
    }
  }

  function isEnrolled(courseId: string) {
    return enrollments.some((e) => e.courseId === courseId)
  }

  function getEnrollment(courseId: string) {
    return enrollments.find((e) => e.courseId === courseId)
  }

  const levels = [...new Set(courses.map((c) => c.level).filter(Boolean))]
  const categories = [...new Set(courses.map((c) => c.category).filter(Boolean))]

  const filteredCourses = courses.filter((course) => {
    const matchesSearch = search === "" || 
      course.title.toLowerCase().includes(search.toLowerCase()) ||
      (course.description?.toLowerCase().includes(search.toLowerCase()) ?? false)
    const matchesLevel = levelFilter === "all" || course.level === levelFilter
    const matchesCategory = categoryFilter === "all" || course.category === categoryFilter
    return matchesSearch && matchesLevel && matchesCategory
  })

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Explore Courses</h1>
        <p className="mt-1 text-sm text-muted-foreground">Discover courses to enhance your learning.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search courses..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <div className="flex gap-2">
          <select
            value={levelFilter}
            onChange={(e) => setLevelFilter(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            <option value="all">All Levels</option>
            {levels.map((level) => (
              <option key={level} value={level}>{level}</option>
            ))}
          </select>
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
          >
            <option value="all">All Categories</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <LoadingState />
      ) : filteredCourses.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title="No courses found"
          description={search ? "Try adjusting your search or filters." : "No courses are available yet."}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredCourses.map((course) => {
            const enrolled = isEnrolled(course.id)
            const enrollment = getEnrollment(course.id)
            return (
              <Link
                key={course.id}
                href={`/dashboard/learner/courses/${course.id}`}
                className="group rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
              >
                {course.thumbnailUrl ? (
                  <div className="mb-3 h-40 overflow-hidden rounded-xl bg-muted">
                    <img src={course.thumbnailUrl} alt={course.title} className="h-full w-full object-cover" />
                  </div>
                ) : (
                  <div className="mb-3 flex h-40 items-center justify-center rounded-xl bg-primary/10">
                    <BookOpen className="size-10 text-primary/40" />
                  </div>
                )}
                <h3 className="text-sm font-semibold text-foreground truncate group-hover:text-primary">{course.title}</h3>
                {course.description && (
                  <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{course.description}</p>
                )}
                <div className="mt-3 flex items-center gap-2 flex-wrap">
                  {course.level && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                      {course.level}
                    </span>
                  )}
                  {course.category && (
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                      {course.category}
                    </span>
                  )}
                </div>
                {enrolled && enrollment && (
                  <div className="mt-3">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-muted-foreground">Progress</span>
                      <span className="font-semibold text-teal">{enrollment.progressPercentage}%</span>
                    </div>
                    <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${enrollment.progressPercentage}%` }} />
                    </div>
                  </div>
                )}
                <div className="mt-3">
                  {enrolled ? (
                    <span className="inline-flex items-center gap-1 rounded-lg bg-teal/10 px-3 py-1.5 text-xs font-medium text-teal">
                      Enrolled
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground group-hover:bg-primary/90">
                      View Course <ArrowRight className="size-3" />
                    </span>
                  )}
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
