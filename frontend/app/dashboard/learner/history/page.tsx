"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type Enrollment, type Bookmark } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { History, BookOpen, Bookmark as BookmarkIcon, Clock, ArrowRight, AlertCircle, Search } from "lucide-react"

interface HistoryItem {
  id: string
  type: "enrollment" | "bookmark"
  title: string
  subtitle: string
  date: string
  link: string
  status?: string
  progress?: number
}

export default function LearnerHistoryPage() {
  const { user, loading: authLoading } = useAuth()
  const [items, setItems] = useState<HistoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [filter, setFilter] = useState<"all" | "courses" | "saved">("all")

  useEffect(() => {
    if (!user || (user.role !== "Other Learner" && user.role !== "Student")) return
    loadHistory()
  }, [user])

  async function loadHistory() {
    try {
      setLoading(true)
      setError(null)
      const [enrollments, bookmarks] = await Promise.all([
        learnerApi.getEnrollments().catch(() => []),
        learnerApi.getBookmarks().catch(() => []),
      ])

      const enrollmentItems: HistoryItem[] = enrollments.map((e: Enrollment) => ({
        id: e.id,
        type: "enrollment" as const,
        title: e.courseTitle || "Untitled Course",
        subtitle: e.completedAt ? "Completed" : e.progressPercentage > 0 ? "In progress" : "Enrolled",
        date: e.enrolledAt,
        link: `/dashboard/learner/courses/${e.courseId}`,
        status: e.completedAt ? "completed" : "in-progress",
        progress: e.progressPercentage,
      }))

      const bookmarkItems: HistoryItem[] = bookmarks.map((b: Bookmark) => ({
        id: b.id,
        type: "bookmark" as const,
        title: b.targetTitle || "Saved Item",
        subtitle: b.targetType || "Item",
        date: b.createdAt,
        link: b.targetType?.toLowerCase() === "course"
          ? `/dashboard/learner/courses/${b.targetId}`
          : "/dashboard/learner/bookmarks",
        status: "saved",
      }))

      const allItems = [...enrollmentItems, ...bookmarkItems].sort(
        (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
      )
      setItems(allItems)
    } catch {
      setError("Failed to load history")
    } finally {
      setLoading(false)
    }
  }

  const filtered = items.filter((item) => {
    const matchesSearch = search === "" || item.title.toLowerCase().includes(search.toLowerCase())
    const matchesFilter = filter === "all" ||
      (filter === "courses" && item.type === "enrollment") ||
      (filter === "saved" && item.type === "bookmark")
    return matchesSearch && matchesFilter
  })

  function getItemIcon(item: HistoryItem) {
    if (item.type === "enrollment") {
      return item.status === "completed"
        ? <div className="flex size-9 items-center justify-center rounded-xl bg-green-500/10"><BookOpen className="size-4 text-green-600" /></div>
        : <div className="flex size-9 items-center justify-center rounded-xl bg-blue-500/10"><BookOpen className="size-4 text-blue-500" /></div>
    }
    return <div className="flex size-9 items-center justify-center rounded-xl bg-orange/10"><BookmarkIcon className="size-4 text-orange" /></div>
  }

  function getStatusBadge(item: HistoryItem) {
    if (item.status === "completed") {
      return <span className="rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold text-green-600">Completed</span>
    }
    if (item.status === "in-progress") {
      return <span className="rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-500">In Progress</span>
    }
    if (item.status === "saved") {
      return <span className="rounded-full bg-orange/10 px-2 py-0.5 text-[10px] font-semibold text-orange">Saved</span>
    }
    return <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">Enrolled</span>
  }

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Learning History</h1>
        <p className="mt-1 text-sm text-muted-foreground">Your learning activity and saved items.</p>
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
            placeholder="Search history..."
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
            onClick={() => setFilter("courses")}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "courses" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            Courses
          </button>
          <button
            onClick={() => setFilter("saved")}
            className={`h-10 rounded-lg px-4 text-sm font-medium transition-colors ${
              filter === "saved" ? "bg-primary text-primary-foreground" : "border border-border bg-background text-muted-foreground hover:bg-muted"
            }`}
          >
            Saved
          </button>
        </div>
      </div>

      {loading ? (
        <LoadingState />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<History className="size-8" />}
          title={search || filter !== "all" ? "No matching history" : "No history yet"}
          description={search || filter !== "all" ? "Try adjusting your search or filters." : "Your learning activity will appear here."}
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
          {filtered.map((item) => (
            <Link
              key={item.id}
              href={item.link}
              className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:shadow-md hover:border-primary/30"
            >
              {getItemIcon(item)}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-foreground truncate">{item.title}</p>
                  {getStatusBadge(item)}
                </div>
                <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                  <span>{item.subtitle}</span>
                  <span>&middot;</span>
                  <span className="flex items-center gap-1">
                    <Clock className="size-3" />
                    {new Date(item.date).toLocaleDateString()}
                  </span>
                </div>
                {item.progress !== undefined && item.progress > 0 && (
                  <div className="mt-2">
                    <div className="h-1.5 w-full max-w-[200px] overflow-hidden rounded-full bg-muted">
                      <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${item.progress}%` }} />
                    </div>
                  </div>
                )}
              </div>
              <ArrowRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
