"use client"

import { useEffect, useState } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type SearchResult, type CourseSummary, type Resource, type LiveClass } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Search, BookOpen, FileText, Video, AlertCircle } from "lucide-react"

export default function LearnerSearchPage() {
  const { user, loading: authLoading } = useAuth()
  const searchParams = useSearchParams()
  const router = useRouter()
  const [query, setQuery] = useState(searchParams.get("q") || "")
  const [activeTab, setActiveTab] = useState<"all" | "courses" | "resources" | "live-classes">(
    (searchParams.get("type") as any) || "all"
  )
  const [results, setResults] = useState<SearchResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    const q = searchParams.get("q")
    const t = searchParams.get("type")
    if (q) {
      setQuery(q)
      if (t) setActiveTab(t as any)
      performSearch(q, t || "all")
    }
  }, [user, searchParams])

  async function performSearch(q: string, type: string) {
    if (!q.trim()) return
    try {
      setLoading(true)
      setError(null)
      const searchType = type === "all" ? undefined : type
      const data = await learnerApi.search(q, searchType)
      setResults(data)
    } catch {
      setError("Search failed. Please try again.")
    } finally {
      setLoading(false)
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!query.trim()) return
    router.push(`/dashboard/learner/search?q=${encodeURIComponent(query)}&type=${activeTab}`)
    performSearch(query, activeTab)
  }

  function handleTabChange(tab: typeof activeTab) {
    setActiveTab(tab)
    if (query.trim()) {
      router.push(`/dashboard/learner/search?q=${encodeURIComponent(query)}&type=${tab}`)
      performSearch(query, tab)
    }
  }

  const tabs = [
    { key: "all" as const, label: "All", count: results ? (results.courses?.length || 0) + (results.resources?.length || 0) + (results.liveClasses?.length || 0) : 0 },
    { key: "courses" as const, label: "Courses", count: results?.courses?.length || 0 },
    { key: "resources" as const, label: "Resources", count: results?.resources?.length || 0 },
    { key: "live-classes" as const, label: "Live Classes", count: results?.liveClasses?.length || 0 },
  ]

  if (authLoading || user?.role !== "Other Learner") {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Search</h1>
        <p className="mt-1 text-sm text-muted-foreground">Find courses, resources, and live classes.</p>
      </div>

      <form onSubmit={handleSearch} className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search for anything..."
          className="h-10 w-full rounded-lg border border-border bg-background pl-10 pr-20 text-sm outline-none focus:border-ring"
        />
        <button
          type="submit"
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
        >
          Search
        </button>
      </form>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="flex gap-2 border-b border-border">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => handleTabChange(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors border-b-2 ${
              activeTab === tab.key
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            {tab.label}
            {tab.count > 0 && (
              <span className="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-semibold">
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingState />
      ) : !results ? (
        <EmptyState
          icon={<Search className="size-8" />}
          title="Start searching"
          description="Enter a query to find courses, resources, and live classes."
        />
      ) : (
        <div className="space-y-6">
          {(activeTab === "all" || activeTab === "courses") && results.courses && results.courses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Courses</h2>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {results.courses.map((course) => (
                  <Link
                    key={course.id}
                    href={`/dashboard/learner/courses/${course.id}`}
                    className="rounded-xl border border-border p-4 transition-all hover:shadow-md hover:border-primary/30"
                  >
                    {course.thumbnailUrl ? (
                      <div className="mb-3 h-32 overflow-hidden rounded-lg bg-muted">
                        <img src={course.thumbnailUrl} alt={course.title} className="h-full w-full object-cover" />
                      </div>
                    ) : (
                      <div className="mb-3 flex h-32 items-center justify-center rounded-lg bg-primary/10">
                        <BookOpen className="size-8 text-primary/40" />
                      </div>
                    )}
                    <h3 className="text-sm font-semibold text-foreground truncate">{course.title}</h3>
                    {course.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{course.description}</p>
                    )}
                    <div className="mt-2 flex items-center gap-2">
                      {course.level && (
                        <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
                          {course.level}
                        </span>
                      )}
                      {course.category && (
                        <span className="text-[10px] text-muted-foreground">{course.category}</span>
                      )}
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          )}

          {(activeTab === "all" || activeTab === "resources") && results.resources && results.resources.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Resources</h2>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {results.resources.map((resource) => (
                  <div key={resource.id} className="rounded-xl border border-border p-4">
                    <div className="flex items-start gap-3">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted">
                        <FileText className="size-5 text-teal" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <h3 className="text-sm font-semibold text-foreground truncate">{resource.title}</h3>
                        {resource.description && (
                          <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{resource.description}</p>
                        )}
                        <span className="mt-2 inline-block rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                          {resource.resourceType}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {(activeTab === "all" || activeTab === "live-classes") && results.liveClasses && results.liveClasses.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Live Classes</h2>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {results.liveClasses.map((cls) => (
                  <div key={cls.id} className="rounded-xl border border-border p-4">
                    <div className="flex items-start justify-between">
                      <h3 className="text-sm font-semibold text-foreground">{cls.title}</h3>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                        cls.status === "SCHEDULED" ? "bg-blue-500/10 text-blue-500" :
                        cls.status === "IN_PROGRESS" ? "bg-green-500/10 text-green-600" : "bg-muted text-muted-foreground"
                      }`}>
                        {cls.status}
                      </span>
                    </div>
                    {cls.description && (
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{cls.description}</p>
                    )}
                    <div className="mt-2 text-xs text-muted-foreground">
                      {new Date(cls.scheduledAt).toLocaleString()} · {cls.durationMinutes} min
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {results.courses?.length === 0 && results.resources?.length === 0 && results.liveClasses?.length === 0 && (
            <EmptyState
              icon={<Search className="size-8" />}
              title="No results found"
              description={`No results for "${query}". Try different keywords.`}
            />
          )}
        </div>
      )}
    </div>
  )
}
