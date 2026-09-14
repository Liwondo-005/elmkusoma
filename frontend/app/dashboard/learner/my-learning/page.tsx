"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { BookOpen, ArrowRight, Loader2, AlertCircle, CheckCircle, Clock, Search } from "lucide-react"

export default function MyLearningPage() {
  const { user, loading: authLoading } = useAuth()
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<"all" | "in-progress" | "completed">("all")
  const [search, setSearch] = useState("")

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    loadEnrollments()
  }, [user])

  async function loadEnrollments() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getEnrollments()
      setEnrollments(data)
    } catch {
      setError("Failed to load your enrollments")
    } finally {
      setLoading(false)
    }
  }

  const filteredEnrollments = enrollments.filter((e) => {
    const matchesSearch = search === "" || e.courseName.toLowerCase().includes(search.toLowerCase())
    const matchesFilter = filter === "all" || 
      (filter === "in-progress" && !e.completedAt) ||
      (filter === "completed" && e.completedAt)
    return matchesSearch && matchesFilter
  })

  const inProgress = enrollments.filter((e) => !e.completedAt)
  const completed = enrollments.filter((e) => e.completedAt)

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Learning</h1>
        <p className="mt-1 text-sm text-muted-foreground">Track your enrolled courses and progress.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Total Enrolled</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{enrollments.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">In Progress</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{inProgress.length}</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Completed</p>
          <p className="mt-1 text-2xl font-extrabold text-foreground">{completed.length}</p>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search enrolled courses..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-3 text-sm outline-none focus:border-ring"
          />
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setFilter("all")}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "all" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            All
          </button>
          <button
            onClick={() => setFilter("in-progress")}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "in-progress" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            In Progress
          </button>
          <button
            onClick={() => setFilter("completed")}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "completed" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            Completed
          </button>
        </div>
      </div>

      {loading ? (
        <LoadingState />
      ) : filteredEnrollments.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title={search || filter !== "all" ? "No matching enrollments" : "No enrolled courses yet"}
          description={search || filter !== "all" ? "Try adjusting your search or filters." : "Start exploring and enroll in courses."}
          action={
            !search && filter === "all" ? (
              <Link
                href="/dashboard/learner/courses"
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Explore Courses <ArrowRight className="size-4" />
              </Link>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3">
          {filteredEnrollments.map((enrollment) => (
            <div key={enrollment.id} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs">
              {enrollment.thumbnailUrl ? (
                <div className="h-16 w-16 shrink-0 overflow-hidden rounded-xl bg-muted">
                  <img src={enrollment.thumbnailUrl} alt={enrollment.courseName} className="h-full w-full object-cover" />
                </div>
              ) : (
                <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                  <BookOpen className="size-6 text-primary/40" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-foreground truncate">{enrollment.courseName}</p>
                <div className="mt-1 flex items-center gap-2">
                  {enrollment.completedAt ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">
                      <CheckCircle className="size-3" /> Completed
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-500">
                      <Clock className="size-3" /> In Progress
                    </span>
                  )}
                  <span className="text-[10px] text-muted-foreground">
                    Enrolled {new Date(enrollment.enrolledAt).toLocaleDateString()}
                  </span>
                </div>
                <div className="mt-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Progress</span>
                    <span className="font-semibold text-teal">{enrollment.progressPercentage}%</span>
                  </div>
                  <div className="mt-1 h-1.5 w-full max-w-xs overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${enrollment.progressPercentage}%` }} />
                  </div>
                </div>
              </div>
              <Link
                href={`/dashboard/learner/courses/${enrollment.courseId}`}
                className="shrink-0 rounded-lg bg-primary px-4 py-2 text-xs font-medium text-primary-foreground hover:bg-primary/90"
              >
                {enrollment.completedAt ? "Review" : "Continue"}
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
