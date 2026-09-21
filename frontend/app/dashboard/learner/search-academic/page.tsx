"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { collegeApi } from "@/lib/college-api"
import { learnerApi } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Search,
  BookOpen,
  FileText,
  Video,
  Users,
  Target,
  Award,
  Calendar,
  Clock,
  ChevronRight,
  Filter,
  X,
  GraduationCap,
  Briefcase,
  Brain,
} from "lucide-react"

type FilterType = "all" | "courses" | "research" | "projects" | "resources" | "live-sessions"

interface SearchResultItem {
  id: string
  type: "course" | "research" | "project" | "resource" | "live-session"
  title: string
  description: string
  status?: string
  date?: string
  metadata?: string
  tags?: string[]
}

const RECENT_SEARCHES_KEY = "elmkusoma_academic_search_recent"
const MAX_RECENT = 5

const POPULAR_SEARCHES = [
  "Research Methodology",
  "Thesis Writing",
  "Data Analysis",
  "Project Management",
  "Machine Learning",
  "Academic Writing",
  "Literature Review",
  "Statistics",
]

const QUICK_LINKS = [
  { label: "My Courses", icon: BookOpen, href: "/dashboard/learner/courses" },
  { label: "Research", icon: Brain, href: "/dashboard/learner/research" },
  { label: "Projects", icon: Briefcase, href: "/dashboard/learner/projects" },
  { label: "Calendar", icon: Calendar, href: "/dashboard/learner/calendar" },
  { label: "Live Sessions", icon: Video, href: "/dashboard/learner/live-classes" },
]

const FILTER_OPTIONS: { key: FilterType; label: string }[] = [
  { key: "all", label: "All" },
  { key: "courses", label: "Courses" },
  { key: "research", label: "Research" },
  { key: "projects", label: "Projects" },
  { key: "resources", label: "Resources" },
  { key: "live-sessions", label: "Live Sessions" },
]

const TYPE_ICONS: Record<string, typeof BookOpen> = {
  course: BookOpen,
  research: Brain,
  project: Briefcase,
  resource: FileText,
  "live-session": Video,
}

const TYPE_NAV: Record<string, string> = {
  course: "/dashboard/learner/course-workspace",
  research: "/dashboard/learner/research",
  project: "/dashboard/learner/projects",
  "live-session": "/dashboard/learner/live-classes",
  resource: "",
}

function getRecentSearches(): string[] {
  if (typeof window === "undefined") return []
  try {
    const raw = localStorage.getItem(RECENT_SEARCHES_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveRecentSearch(query: string) {
  if (typeof window === "undefined" || !query.trim()) return
  const recent = getRecentSearches().filter((r) => r.toLowerCase() !== query.toLowerCase())
  recent.unshift(query.trim())
  localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(recent.slice(0, MAX_RECENT)))
}

function matchesQuery(text: string | undefined | null, q: string): boolean {
  if (!text || !q) return false
  return text.toLowerCase().includes(q.toLowerCase())
}

export default function AcademicSearchPage() {
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()

  const [query, setQuery] = useState("")
  const [activeFilter, setActiveFilter] = useState<FilterType>("all")
  const [loading, setLoading] = useState(false)
  const [results, setResults] = useState<SearchResultItem[]>([])
  const [hasSearched, setHasSearched] = useState(false)
  const [recentSearches, setRecentSearches] = useState<string[]>([])
  const [allData, setAllData] = useState<{
    courses: any[]
    research: any[]
    projects: any[]
    resources: any[]
    liveSessions: any[]
  }>({ courses: [], research: [], projects: [], resources: [], liveSessions: [] })

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  useEffect(() => {
    setRecentSearches(getRecentSearches())
  }, [])

  const loadData = useCallback(async () => {
    if (!user) return
    try {
      const [coursesRes, researchRes, projectsRes, resourcesRes, liveSessionsRes] = await Promise.allSettled([
        learnerApi.getCourses(),
        collegeApi.getStudentResearch(user.id),
        collegeApi.getStudentProjects(user.id),
        learnerApi.getResources(),
        learnerApi.getLiveClasses(),
      ])
      const normalize = (r: PromiseSettledResult<any>): any[] => {
        if (r.status !== "fulfilled") return []
        const v = r.value
        return Array.isArray(v) ? v : v?.data || []
      }
      setAllData({
        courses: normalize(coursesRes),
        research: normalize(researchRes),
        projects: normalize(projectsRes),
        resources: normalize(resourcesRes),
        liveSessions: normalize(liveSessionsRes),
      })
    } catch {
      // silently fail — we'll show empty results
    }
  }, [user])

  useEffect(() => {
    if (user) loadData()
  }, [user, loadData])

  const performSearch = useCallback(
    (q: string) => {
      if (!q.trim()) {
        setResults([])
        setHasSearched(false)
        return
      }

      setLoading(true)
      const lower = q.toLowerCase()
      const items: SearchResultItem[] = []

      allData.courses.forEach((c: any) => {
        if (
          matchesQuery(c.title, q) ||
          matchesQuery(c.description, q) ||
          matchesQuery(c.category, q) ||
          matchesQuery(c.subject, q)
        ) {
          items.push({
            id: c.id,
            type: "course",
            title: c.title,
            description: c.description || "",
            status: c.isPublished ? "Published" : "Draft",
            date: c.createdAt,
            metadata: c.level ? `${c.level}${c.category ? " · " + c.category : ""}` : c.category || "",
          })
        }
      })

      allData.research.forEach((r: any) => {
        if (
          matchesQuery(r.title, q) ||
          matchesQuery(r.researchQuestion, q) ||
          matchesQuery(r.abstractText, q) ||
          matchesQuery(r.keywords, q) ||
          matchesQuery(r.objectives, q) ||
          matchesQuery(r.methodology, q)
        ) {
          items.push({
            id: r.id,
            type: "research",
            title: r.title,
            description: r.abstractText || r.researchQuestion || r.objectives || "",
            status: r.status?.replace(/_/g, " "),
            date: r.createdAt,
            metadata: r.keywords || "",
            tags: r.keywords ? r.keywords.split(",").map((k: string) => k.trim()) : [],
          })
        }
      })

      allData.projects.forEach((p: any) => {
        if (
          matchesQuery(p.title, q) ||
          matchesQuery(p.description, q) ||
          matchesQuery(p.objective, q)
        ) {
          items.push({
            id: p.id,
            type: "project",
            title: p.title,
            description: p.description || p.objective || "",
            status: p.status?.replace(/_/g, " "),
            date: p.createdAt,
            metadata: p.dueDate ? `Due: ${new Date(p.dueDate).toLocaleDateString()}` : "",
          })
        }
      })

      allData.resources.forEach((r: any) => {
        if (
          matchesQuery(r.title, q) ||
          matchesQuery(r.description, q) ||
          matchesQuery(r.resourceType, q)
        ) {
          items.push({
            id: r.id,
            type: "resource",
            title: r.title,
            description: r.description || "",
            status: r.resourceType,
            date: r.createdAt,
            metadata: r.fileUrl ? "Has attachment" : "",
          })
        }
      })

      allData.liveSessions.forEach((ls: any) => {
        if (
          matchesQuery(ls.title, q) ||
          matchesQuery(ls.description, q) ||
          matchesQuery(ls.teacherName, q) ||
          matchesQuery(ls.subjectName, q)
        ) {
          items.push({
            id: ls.id,
            type: "live-session",
            title: ls.title,
            description: ls.description || "",
            status: ls.status?.replace(/_/g, " "),
            date: ls.scheduledAt,
            metadata: ls.teacherName
              ? `${ls.teacherName}${ls.subjectName ? " · " + ls.subjectName : ""}`
              : ls.subjectName || "",
          })
        }
      })

      setResults(items)
      setHasSearched(true)
      setLoading(false)
      saveRecentSearch(q)
      setRecentSearches(getRecentSearches())
    },
    [allData]
  )

  useEffect(() => {
    if (!query.trim()) {
      setResults([])
      setHasSearched(false)
      return
    }
    const timer = setTimeout(() => performSearch(query), 300)
    return () => clearTimeout(timer)
  }, [query, performSearch])

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter") {
      performSearch(query)
    }
  }

  function handleClear() {
    setQuery("")
    setResults([])
    setHasSearched(false)
  }

  function handleRecentClick(q: string) {
    setQuery(q)
  }

  const filteredResults =
    activeFilter === "all"
      ? results
      : results.filter((r) => {
          if (activeFilter === "courses") return r.type === "course"
          if (activeFilter === "research") return r.type === "research"
          if (activeFilter === "projects") return r.type === "project"
          if (activeFilter === "resources") return r.type === "resource"
          if (activeFilter === "live-sessions") return r.type === "live-session"
          return true
        })

  const grouped = filteredResults.reduce<Record<string, SearchResultItem[]>>((acc, item) => {
    if (!acc[item.type]) acc[item.type] = []
    acc[item.type].push(item)
    return acc
  }, {})

  const groupLabels: Record<string, string> = {
    course: "Courses",
    research: "Research",
    project: "Projects",
    resource: "Resources",
    "live-session": "Live Sessions",
  }

  function navigateResult(item: SearchResultItem) {
    const base = TYPE_NAV[item.type]
    if (item.type === "resource") {
      return
    }
    if (base) {
      router.push(`${base}`)
    }
  }

  if (authLoading) return <LoadingState />

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Search across courses, resources, live sessions, projects, and more" />

      <div className="relative">
        <Search className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Search courses, research, projects, sessions..."
          autoFocus
          className="h-12 w-full rounded-2xl border border-border bg-card pl-12 pr-12 text-sm shadow-xs outline-none placeholder:text-muted-foreground focus:border-ring"
        />
        {query && (
          <button
            onClick={handleClear}
            className="absolute right-4 top-1/2 -translate-y-1/2 rounded-full p-1 text-muted-foreground hover:bg-muted"
            aria-label="Clear search"
          >
            <X className="size-4" />
          </button>
        )}
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
        {FILTER_OPTIONS.map((f) => (
          <button
            key={f.key}
            onClick={() => setActiveFilter(f.key)}
            className={`shrink-0 rounded-full border px-4 py-1.5 text-xs font-medium transition-colors ${
              activeFilter === f.key
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-card text-muted-foreground hover:bg-muted"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {!hasSearched && !loading && (
        <div className="space-y-8">
          {recentSearches.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold text-foreground">Recent Searches</h2>
              <div className="flex flex-wrap gap-2">
                {recentSearches.map((r, i) => (
                  <button
                    key={i}
                    onClick={() => handleRecentClick(r)}
                    className="inline-flex items-center gap-1.5 rounded-full border border-border bg-card px-3 py-1.5 text-xs text-muted-foreground hover:bg-muted"
                  >
                    <Clock className="size-3" />
                    {r}
                  </button>
                ))}
              </div>
            </section>
          )}

          <section>
            <h2 className="mb-3 text-sm font-semibold text-foreground">Popular Searches</h2>
            <div className="flex flex-wrap gap-2">
              {POPULAR_SEARCHES.map((term) => (
                <button
                  key={term}
                  onClick={() => setQuery(term)}
                  className="rounded-full border border-border bg-card px-3 py-1.5 text-xs text-muted-foreground hover:bg-muted"
                >
                  {term}
                </button>
              ))}
            </div>
          </section>

          <section>
            <h2 className="mb-3 text-sm font-semibold text-foreground">Quick Links</h2>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {QUICK_LINKS.map((link) => {
                const Icon = link.icon
                return (
                  <button
                    key={link.label}
                    onClick={() => router.push(link.href)}
                    className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs text-left transition-colors hover:bg-muted/50"
                  >
                    <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                      <Icon className="size-5 text-primary" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold text-foreground">{link.label}</p>
                    </div>
                    <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
                  </button>
                )
              })}
            </div>
          </section>
        </div>
      )}

      {loading && <LoadingState />}

      {hasSearched && !loading && filteredResults.length === 0 && (
        <EmptyState
          icon={<Search className="size-8" />}
          title="No results found"
          description={`No results for "${query}". Try different keywords or adjust your filter.`}
        />
      )}

      {hasSearched && !loading && filteredResults.length > 0 && (
        <div className="space-y-6">
          {Object.entries(grouped).map(([type, items]) => {
            const Icon = TYPE_ICONS[type] || FileText
            return (
              <section key={type}>
                <div className="mb-3 flex items-center gap-2">
                  <Icon className="size-4 text-muted-foreground" />
                  <h2 className="text-sm font-semibold text-foreground">{groupLabels[type] || type}</h2>
                  <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
                    {items.length}
                  </span>
                </div>
                <div className="space-y-2">
                  {items.map((item) => {
                    const ItemIcon = TYPE_ICONS[item.type] || FileText
                    return (
                      <button
                        key={item.id}
                        onClick={() => navigateResult(item)}
                        className="flex w-full items-center gap-3 rounded-2xl border border-border bg-card p-5 shadow-xs text-left transition-colors hover:bg-muted/50"
                      >
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                          <ItemIcon className="size-5 text-primary" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-bold text-foreground truncate">{item.title}</p>
                          {item.description && (
                            <p className="mt-0.5 text-xs text-muted-foreground line-clamp-2">{item.description}</p>
                          )}
                          <div className="mt-2 flex flex-wrap items-center gap-2">
                            {item.status && (
                              <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
                                {item.status}
                              </span>
                            )}
                            {item.metadata && (
                              <span className="text-[10px] text-muted-foreground">{item.metadata}</span>
                            )}
                            {item.date && (
                              <span className="text-[10px] text-muted-foreground">
                                {new Date(item.date).toLocaleDateString()}
                              </span>
                            )}
                          </div>
                          {item.tags && item.tags.length > 0 && (
                            <div className="mt-1.5 flex flex-wrap gap-1">
                              {item.tags.map((tag, ti) => (
                                <span key={ti} className="rounded bg-primary/5 px-1.5 py-0.5 text-[10px] text-primary">
                                  {tag}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                        <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
                      </button>
                    )
                  })}
                </div>
              </section>
            )
          })}
        </div>
      )}
    </div>
  )
}
