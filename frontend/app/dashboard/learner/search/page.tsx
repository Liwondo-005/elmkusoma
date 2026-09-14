"use client"

import { useEffect, useState } from "react"
import { useSearchParams, useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learnerApi, type SearchResult, type CourseSummary, type Resource, type LiveClass, type SearchFilters } from "@/lib/learner-api"
import { EmptyState, LoadingState } from "@/components/learner/shared"
import { Search, BookOpen, FileText, Video, AlertCircle, SlidersHorizontal, ChevronDown, Megaphone } from "lucide-react"

export default function LearnerSearchPage() {
  const { user, loading: authLoading } = useAuth()
  const searchParams = useSearchParams()
  const router = useRouter()
  const [query, setQuery] = useState(searchParams.get("q") || "")
  const [activeTab, setActiveTab] = useState<"all" | "courses" | "resources" | "live-classes" | "announcements">(
    (searchParams.get("type") as any) || "all"
  )
  const [results, setResults] = useState<SearchResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState<SearchFilters>({
    level: searchParams.get("level") || undefined,
    category: searchParams.get("category") || undefined,
    provider: searchParams.get("provider") || undefined,
    dateFrom: searchParams.get("dateFrom") || undefined,
    dateTo: searchParams.get("dateTo") || undefined,
    sort: searchParams.get("sort") || "newest",
  })

  const levels = ["NURSERY", "PRIMARY", "SECONDARY", "COLLEGE", "VETA", "UNIVERSITY"]
  const categories = ["Mathematics", "Science", "English", "Kiswahili", "History", "Geography", "Computer Science", "Business", "Vocational"]
  const sortOptions = [
    { value: "newest", label: "Newest First" },
    { value: "oldest", label: "Oldest First" },
    { value: "az", label: "A - Z" },
  ]

  useEffect(() => {
    if (!user || user.role !== "Other Learner") return
    const q = searchParams.get("q")
    const t = searchParams.get("type")
    const lvl = searchParams.get("level")
    const cat = searchParams.get("category")
    const prv = searchParams.get("provider")
    const df = searchParams.get("dateFrom")
    const dt = searchParams.get("dateTo")
    const srt = searchParams.get("sort")
    if (q) {
      setQuery(q)
      if (t) setActiveTab(t as any)
      setFilters({
        level: lvl || undefined,
        category: cat || undefined,
        provider: prv || undefined,
        dateFrom: df || undefined,
        dateTo: dt || undefined,
        sort: srt || "newest",
      })
      performSearch(q, t || "all", { level: lvl || undefined, category: cat || undefined, provider: prv || undefined, dateFrom: df || undefined, dateTo: dt || undefined, sort: srt || "newest" })
    }
  }, [user, searchParams])

  async function performSearch(q: string, type: string, searchFilters?: SearchFilters) {
    if (!q.trim()) return
    try {
      setLoading(true)
      setError(null)
      const typeMap: Record<string, string> = { courses: "COURSE", resources: "RESOURCE", "live-classes": "LIVE_CLASS", announcements: "ANNOUNCEMENT" }
      const searchType = type === "all" ? undefined : (typeMap[type] || type)
      const data = await learnerApi.search(q, searchType, searchFilters || filters)
      setResults(data)
    } catch {
      setError("Search failed. Please try again.")
    } finally {
      setLoading(false)
    }
  }

  function buildParams(extraType?: string) {
    const params = new URLSearchParams()
    params.set("q", query)
    params.set("type", extraType || activeTab)
    if (filters.level) params.set("level", filters.level)
    if (filters.category) params.set("category", filters.category)
    if (filters.provider) params.set("provider", filters.provider)
    if (filters.dateFrom) params.set("dateFrom", filters.dateFrom)
    if (filters.dateTo) params.set("dateTo", filters.dateTo)
    if (filters.sort && filters.sort !== "newest") params.set("sort", filters.sort)
    return params.toString()
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!query.trim()) return
    router.push(`/dashboard/learner/search?${buildParams()}`)
    performSearch(query, activeTab)
  }

  function handleTabChange(tab: typeof activeTab) {
    setActiveTab(tab)
    if (query.trim()) {
      router.push(`/dashboard/learner/search?${buildParams(tab)}`)
      performSearch(query, tab)
    }
  }

  function handleFilterChange(key: keyof SearchFilters, value: string) {
    const newFilters = { ...filters, [key]: value || undefined }
    setFilters(newFilters)
    if (query.trim()) {
      const params = new URLSearchParams()
      params.set("q", query)
      params.set("type", activeTab)
      if (newFilters.level) params.set("level", newFilters.level)
      if (newFilters.category) params.set("category", newFilters.category)
      if (newFilters.provider) params.set("provider", newFilters.provider)
      if (newFilters.dateFrom) params.set("dateFrom", newFilters.dateFrom)
      if (newFilters.dateTo) params.set("dateTo", newFilters.dateTo)
      if (newFilters.sort && newFilters.sort !== "newest") params.set("sort", newFilters.sort)
      router.push(`/dashboard/learner/search?${params.toString()}`)
      performSearch(query, activeTab, newFilters)
    }
  }

  function clearFilters() {
    setFilters({ sort: "newest" })
    if (query.trim()) {
      router.push(`/dashboard/learner/search?q=${encodeURIComponent(query)}&type=${activeTab}`)
      performSearch(query, activeTab, { sort: "newest" })
    }
  }

  const hasActiveFilters = filters.level || filters.category || filters.provider || filters.dateFrom || filters.dateTo || (filters.sort && filters.sort !== "newest")

  const tabs = [
    { key: "all" as const, label: "All", count: results ? (results.courses?.length || 0) + (results.resources?.length || 0) + (results.liveClasses?.length || 0) + (results.announcements?.length || 0) : 0 },
    { key: "courses" as const, label: "Courses", count: results?.courses?.length || 0 },
    { key: "resources" as const, label: "Resources", count: results?.resources?.length || 0 },
    { key: "live-classes" as const, label: "Live Classes", count: results?.liveClasses?.length || 0 },
    { key: "announcements" as const, label: "Announcements", count: results?.announcements?.length || 0 },
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

      <div className="flex items-center justify-between">
        <div className="flex gap-2 border-b border-border flex-1">
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
        <button
          type="button"
          onClick={() => setShowFilters(!showFilters)}
          className={`ml-4 inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
            showFilters || hasActiveFilters
              ? "border-primary bg-primary/5 text-primary"
              : "border-border text-muted-foreground hover:text-foreground"
          }`}
        >
          <SlidersHorizontal className="size-3" />
          Filters
          {hasActiveFilters && (
            <span className="rounded-full bg-primary px-1.5 py-0.5 text-[9px] text-primary-foreground">
              {[filters.level, filters.category, filters.provider, filters.dateFrom, filters.dateTo, filters.sort !== "newest" ? filters.sort : null].filter(Boolean).length}
            </span>
          )}
        </button>
      </div>

      {showFilters && (
        <div className="rounded-xl border border-border bg-card p-4 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-foreground">Advanced Filters</h3>
            {hasActiveFilters && (
              <button onClick={clearFilters} className="text-xs text-primary hover:underline">
                Clear all
              </button>
            )}
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Level</label>
              <select
                value={filters.level || ""}
                onChange={(e) => handleFilterChange("level", e.target.value)}
                className="h-9 w-full rounded-lg border border-border bg-background px-3 text-xs outline-none focus:border-ring"
              >
                <option value="">All Levels</option>
                {levels.map((lvl) => (
                  <option key={lvl} value={lvl}>{lvl}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Category</label>
              <select
                value={filters.category || ""}
                onChange={(e) => handleFilterChange("category", e.target.value)}
                className="h-9 w-full rounded-lg border border-border bg-background px-3 text-xs outline-none focus:border-ring"
              >
                <option value="">All Categories</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Sort By</label>
              <select
                value={filters.sort || "newest"}
                onChange={(e) => handleFilterChange("sort", e.target.value)}
                className="h-9 w-full rounded-lg border border-border bg-background px-3 text-xs outline-none focus:border-ring"
              >
                {sortOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Provider ID</label>
              <input
                type="text"
                value={filters.provider || ""}
                onChange={(e) => handleFilterChange("provider", e.target.value)}
                placeholder="Institution UUID"
                className="h-9 w-full rounded-lg border border-border bg-background px-3 text-xs outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Date From</label>
              <input
                type="date"
                value={filters.dateFrom || ""}
                onChange={(e) => handleFilterChange("dateFrom", e.target.value)}
                className="h-9 w-full rounded-lg border border-border bg-background px-3 text-xs outline-none focus:border-ring"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Date To</label>
              <input
                type="date"
                value={filters.dateTo || ""}
                onChange={(e) => handleFilterChange("dateTo", e.target.value)}
                className="h-9 w-full rounded-lg border border-border bg-background px-3 text-xs outline-none focus:border-ring"
              />
            </div>
          </div>
        </div>
      )}

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
                        <div className="mt-2 flex items-center gap-2">
                          <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                            {resource.resourceType}
                          </span>
                          {resource.fileUrl && (
                            <a
                              href={resource.fileUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              onClick={(e) => e.stopPropagation()}
                              className="text-[10px] font-medium text-primary hover:underline"
                            >
                              Download
                            </a>
                          )}
                        </div>
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

          {(activeTab === "all" || activeTab === "announcements") && results.announcements && results.announcements.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold text-foreground">Announcements</h2>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {results.announcements.map((ann) => (
                  <div key={ann.id} className="rounded-xl border border-border p-4">
                    <div className="flex items-start gap-3">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-yellow-500/10">
                        <Megaphone className="size-5 text-yellow-600" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <h3 className="text-sm font-semibold text-foreground truncate">{ann.title}</h3>
                        {ann.content && (
                          <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{ann.content}</p>
                        )}
                        <div className="mt-2 flex items-center gap-2">
                          <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                            ann.priority === "URGENT" ? "bg-red-500/10 text-red-500" :
                            ann.priority === "HIGH" ? "bg-orange/10 text-orange" :
                            "bg-muted text-muted-foreground"
                          }`}>
                            {ann.priority}
                          </span>
                          <span className="text-[10px] text-muted-foreground">
                            {new Date(ann.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {results.courses?.length === 0 && results.resources?.length === 0 && results.liveClasses?.length === 0 && results.announcements?.length === 0 && (
            <EmptyState
              icon={<Search className="size-8" />}
              title="No results found"
              description={`No results for "${query}". Try different keywords or adjust your filters.`}
            />
          )}
        </div>
      )}
    </div>
  )
}
