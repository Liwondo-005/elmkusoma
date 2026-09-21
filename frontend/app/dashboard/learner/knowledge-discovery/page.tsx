"use client"

import { useState, useEffect, useCallback } from "react"
import { useAuth } from "@/lib/auth"
import { useRouter } from "next/navigation"
import { collegeApi } from "@/lib/college-api"
import { learnerApi } from "@/lib/learner-api"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import {
  Brain,
  BookOpen,
  Video,
  FileText,
  Target,
  ArrowRight,
  Lightbulb,
  Search,
  Layers,
  Link2,
  Zap,
  ChevronRight,
} from "lucide-react"

interface EnrolledCourse {
  id: string
  courseId: string
  courseTitle: string
  courseDescription: string | null
  courseLevel: string | null
  courseCategory: string | null
  enrolledAt: string
  completedAt: string | null
  progressPercentage: number
}

type DiscoveryArea = "course" | "topic" | "research" | "project"

const DISCOVERY_AREAS: { key: DiscoveryArea; label: string; icon: typeof Brain; description: string }[] = [
  { key: "course", label: "By Course", icon: BookOpen, description: "Group content by enrolled course" },
  { key: "topic", label: "By Topic", icon: Layers, description: "Cross-course topic connections" },
  { key: "research", label: "By Research", icon: Search, description: "Research-to-course connections" },
  { key: "project", label: "By Project", icon: Target, description: "Project-to-learning connections" },
]

const CONNECTION_STEPS = [
  { label: "Current Topic", icon: Brain, color: "bg-primary/10 text-primary" },
  { label: "Related Lecture", icon: BookOpen, color: "bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400" },
  { label: "Resource", icon: FileText, color: "bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400" },
  { label: "Live Replay", icon: Video, color: "bg-purple-100 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400" },
  { label: "Practice", icon: Zap, color: "bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400" },
  { label: "Assessment", icon: Target, color: "bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400" },
]

interface LearningPathItem {
  id: string
  title: string
  type: "course" | "resource" | "live_class" | "project" | "research" | "competency"
  icon: typeof Brain
  subtitle: string
  link: string
  connectedTo: string
}

export default function KnowledgeDiscoveryPage() {
  const { user, loading: authLoading } = useAuth()
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [enrollments, setEnrollments] = useState<EnrolledCourse[]>([])
  const [research, setResearch] = useState<any[]>([])
  const [projects, setProjects] = useState<any[]>([])
  const [competencies, setCompetencies] = useState<any[]>([])
  const [liveClasses, setLiveClasses] = useState<any[]>([])
  const [resources, setResources] = useState<any[]>([])

  const [activeArea, setActiveArea] = useState<DiscoveryArea>("course")

  const loadData = useCallback(async () => {
    if (!user) return
    try {
      setLoading(true)
      setError(null)
      const studentId = user.id
      const [enrollRes, researchRes, projectsRes, compRes, liveRes, resRes] = await Promise.all([
        collegeApi.getStudentEnrollments(studentId).catch(() => ({ data: [] })),
        collegeApi.getStudentResearch(studentId).catch(() => ({ data: [] })),
        collegeApi.getStudentProjects(studentId).catch(() => ({ data: [] })),
        collegeApi.getStudentCompetencies(studentId).catch(() => ({ data: [] })),
        learnerApi.getLiveClasses().catch(() => []),
        learnerApi.getResources().catch(() => []),
      ])
      setEnrollments(enrollRes.data as any || [])
      setResearch(researchRes.data || [])
      setProjects(projectsRes.data || [])
      setCompetencies(compRes.data || [])
      setLiveClasses(Array.isArray(liveRes) ? liveRes : [])
      setResources(Array.isArray(resRes) ? resRes : [])
    } catch {
      setError("Failed to load discovery data")
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (authLoading || loading) return <LoadingState />

  if (error && enrollments.length === 0) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader
          firstName={user?.firstName || "Learner"}
          subtitle="Connected learning — explore how topics connect across courses, resources, and activities"
        />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <span>{error}</span>
          <button onClick={loadData} className="ml-auto text-xs underline">Retry</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const studentId = user?.id || ""

  const getCourseConnections = (courseId: string) => {
    const courseResearch = research.filter((r: any) => r.subjectId === courseId)
    const courseProjects = projects.filter((p: any) => p.subjectId === courseId)
    const courseLive = liveClasses.filter((l: any) => l.subjectId === courseId)
    const courseCompetencies = competencies.filter((c: any) => {
      const record = c
      return record.competencyType || record.status
    })
    return { courseResearch, courseProjects, courseLive, courseCompetencies }
  }

  const buildLearningPath = (): LearningPathItem[] => {
    const items: LearningPathItem[] = []

    enrollments.slice(0, 3).forEach((en) => {
      items.push({
        id: en.id,
        title: en.courseTitle,
        type: "course",
        icon: BookOpen,
        subtitle: `${en.progressPercentage}% complete`,
        link: `/dashboard/learner/courses/${en.courseId}`,
        connectedTo: "Continue exploring this course",
      })
    })

    resources.slice(0, 2).forEach((r: any) => {
      items.push({
        id: r.id,
        title: r.title,
        type: "resource",
        icon: FileText,
        subtitle: r.resourceType || "Resource",
        link: `/dashboard/learner/resources`,
        connectedTo: "Connected to your courses",
      })
    })

    liveClasses.filter((l: any) => l.recordingUrl).slice(0, 2).forEach((l: any) => {
      items.push({
        id: l.id,
        title: l.title,
        type: "live_class",
        icon: Video,
        subtitle: l.subjectName || "Live session",
        link: `/dashboard/learner/live-classes`,
        connectedTo: "Watch replay to reinforce learning",
      })
    })

    return items
  }

  const learningPath = buildLearningPath()

  const connectedCompetencies = competencies.slice(0, 6).map((c: any) => ({
    name: c.competencyName || c.name || "Competency",
    status: c.status || "NOT_STARTED",
    type: c.competencyType || c.type || "SKILL",
  }))

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader
        firstName={firstName}
        subtitle="Connected learning — explore how topics connect across courses, resources, and activities"
      />

      {/* Discovery Area Tabs */}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {DISCOVERY_AREAS.map((area) => {
          const Icon = area.icon
          const isActive = activeArea === area.key
          return (
            <button
              key={area.key}
              onClick={() => setActiveArea(area.key)}
              className={`rounded-2xl border p-4 text-left transition ${
                isActive
                  ? "border-primary bg-primary/5 shadow-xs ring-1 ring-primary/20"
                  : "border-border bg-card hover:shadow-xs"
              }`}
            >
              <div className="flex items-center gap-3">
                <div className={`flex size-10 items-center justify-center rounded-full ${isActive ? "bg-primary/10" : "bg-muted"}`}>
                  <Icon className={`size-5 ${isActive ? "text-primary" : "text-muted-foreground"}`} />
                </div>
                <div>
                  <p className={`text-sm font-semibold ${isActive ? "text-foreground" : "text-muted-foreground"}`}>
                    {area.label}
                  </p>
                  <p className="text-xs text-muted-foreground">{area.description}</p>
                </div>
              </div>
            </button>
          )
        })}
      </div>

      {/* Connection Visualization */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Link2 className="size-4 text-primary" />
          <h2 className="text-sm font-semibold text-foreground">Learning Connection Path</h2>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {CONNECTION_STEPS.map((step, idx) => {
            const Icon = step.icon
            return (
              <div key={step.label} className="flex items-center gap-2">
                <div className={`flex items-center gap-2 rounded-xl border border-border px-3 py-2 ${step.color}`}>
                  <Icon className="size-4" />
                  <span className="text-xs font-medium whitespace-nowrap">{step.label}</span>
                </div>
                {idx < CONNECTION_STEPS.length - 1 && (
                  <ChevronRight className="size-4 text-muted-foreground shrink-0" />
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Course Connections (default view) */}
      {activeArea === "course" && (
        <div className="space-y-4">
          <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <BookOpen className="size-4 text-primary" />
            Course Connections
          </h2>
          {enrollments.length === 0 ? (
            <EmptyState
              icon={<BookOpen className="size-8" />}
              title="No enrolled courses"
              description="Enroll in courses to discover connections between topics, resources, and activities."
            />
          ) : (
            <div className="space-y-4">
              {enrollments.map((en) => {
                const conn = getCourseConnections(en.courseId)
                return (
                  <div key={en.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold text-foreground">{en.courseTitle}</h3>
                        {en.courseDescription && (
                          <p className="mt-1 text-xs text-muted-foreground line-clamp-1">{en.courseDescription}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-1.5">
                        <div className="h-2 w-2 rounded-full bg-emerald-500" />
                        <span className="text-xs font-medium text-muted-foreground">{en.progressPercentage}%</span>
                      </div>
                    </div>

                    <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                      {/* Connected Resources */}
                      <div className="rounded-xl border border-border p-3">
                        <div className="flex items-center gap-2 mb-2">
                          <FileText className="size-3.5 text-amber-500" />
                          <span className="text-xs font-medium text-muted-foreground">Resources</span>
                        </div>
                        {conn.courseResearch.length > 0 ? (
                          <div className="space-y-1">
                            {conn.courseResearch.slice(0, 3).map((r: any) => (
                              <p key={r.id} className="text-xs text-foreground truncate">{r.title}</p>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-muted-foreground italic">No linked resources</p>
                        )}
                      </div>

                      {/* Connected Projects */}
                      <div className="rounded-xl border border-border p-3">
                        <div className="flex items-center gap-2 mb-2">
                          <Target className="size-3.5 text-primary" />
                          <span className="text-xs font-medium text-muted-foreground">Projects</span>
                        </div>
                        {conn.courseProjects.length > 0 ? (
                          <div className="space-y-1">
                            {conn.courseProjects.slice(0, 3).map((p: any) => (
                              <p key={p.id} className="text-xs text-foreground truncate">{p.title}</p>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-muted-foreground italic">No linked projects</p>
                        )}
                      </div>

                      {/* Connected Live Sessions */}
                      <div className="rounded-xl border border-border p-3">
                        <div className="flex items-center gap-2 mb-2">
                          <Video className="size-3.5 text-purple-500" />
                          <span className="text-xs font-medium text-muted-foreground">Live Sessions</span>
                        </div>
                        {conn.courseLive.length > 0 ? (
                          <div className="space-y-1">
                            {conn.courseLive.slice(0, 3).map((l: any) => (
                              <p key={l.id} className="text-xs text-foreground truncate">{l.title}</p>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-muted-foreground italic">No linked sessions</p>
                        )}
                      </div>

                      {/* Connected Competencies */}
                      <div className="rounded-xl border border-border p-3">
                        <div className="flex items-center gap-2 mb-2">
                          <Zap className="size-3.5 text-emerald-500" />
                          <span className="text-xs font-medium text-muted-foreground">Competencies</span>
                        </div>
                        {conn.courseCompetencies.length > 0 ? (
                          <div className="space-y-1">
                            {conn.courseCompetencies.slice(0, 3).map((c: any, i: number) => (
                              <p key={i} className="text-xs text-foreground truncate">{c.competencyName || c.name || "Competency"}</p>
                            ))}
                          </div>
                        ) : (
                          <p className="text-xs text-muted-foreground italic">No linked competencies</p>
                        )}
                      </div>
                    </div>

                    <button
                      onClick={() => router.push(`/dashboard/learner/courses/${en.courseId}`)}
                      className="mt-4 flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
                    >
                      Open course <ArrowRight className="size-3" />
                    </button>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* Topic View */}
      {activeArea === "topic" && (
        <div className="space-y-4">
          <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <Layers className="size-4 text-primary" />
            Cross-Course Topic Connections
          </h2>
          {competencies.length === 0 ? (
            <EmptyState
              icon={<Layers className="size-8" />}
              title="No topics yet"
              description="Topics will appear as you progress through courses and develop competencies."
            />
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {competencies.slice(0, 9).map((c: any, idx: number) => {
                const statusColors: Record<string, string> = {
                  NOT_STARTED: "bg-muted text-muted-foreground",
                  LEARNING: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
                  PRACTICING: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
                  ASSESSED: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
                  COMPETENT: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
                  NEEDS_PRACTICE: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
                  COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
                }
                const status = c.status || "NOT_STARTED"
                return (
                  <div key={idx} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <div className="flex size-8 items-center justify-center rounded-full bg-primary/10">
                          <Lightbulb className="size-4 text-primary" />
                        </div>
                        <div>
                          <p className="text-sm font-medium text-foreground">{c.competencyName || c.name || "Topic"}</p>
                          <p className="text-xs text-muted-foreground">{c.competencyType || c.type || "Knowledge"}</p>
                        </div>
                      </div>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-medium ${statusColors[status] || "bg-muted text-muted-foreground"}`}>
                        {status.replace(/_/g, " ")}
                      </span>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* Research View */}
      {activeArea === "research" && (
        <div className="space-y-4">
          <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <Search className="size-4 text-primary" />
            Research-to-Course Connections
          </h2>
          {research.length === 0 ? (
            <EmptyState
              icon={<Search className="size-8" />}
              title="No research projects yet"
              description="Research projects will connect to your courses as you advance."
            />
          ) : (
            <div className="space-y-3">
              {research.map((r: any) => {
                const relatedEnrollment = enrollments.find((e) => e.courseId === r.subjectId)
                return (
                  <div key={r.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <div className="flex items-start gap-3">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
                        <Search className="size-5 text-amber-600 dark:text-amber-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-semibold text-foreground">{r.title}</h3>
                        {r.researchQuestion && (
                          <p className="mt-1 text-xs text-muted-foreground italic line-clamp-2">{r.researchQuestion}</p>
                        )}
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          {r.status && (
                            <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                              {r.status.replace(/_/g, " ")}
                            </span>
                          )}
                          {relatedEnrollment && (
                            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                              <Link2 className="size-2.5" />
                              {relatedEnrollment.courseTitle}
                            </span>
                          )}
                          {r.keywords && (
                            <div className="flex flex-wrap gap-1">
                              {r.keywords.split(",").slice(0, 3).map((kw: string, i: number) => (
                                <span key={i} className="inline-block rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                                  {kw.trim()}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* Project View */}
      {activeArea === "project" && (
        <div className="space-y-4">
          <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <Target className="size-4 text-primary" />
            Project-to-Learning Connections
          </h2>
          {projects.length === 0 ? (
            <EmptyState
              icon={<Target className="size-8" />}
              title="No projects yet"
              description="Projects will appear here as your instructors assign them."
            />
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              {projects.map((p: any) => {
                const relatedEnrollment = enrollments.find((e) => e.courseId === p.subjectId)
                const relatedResearch = research.filter((r: any) => r.subjectId === p.subjectId)
                return (
                  <div key={p.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                    <div className="flex items-start gap-3">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
                        <Target className="size-5 text-emerald-600 dark:text-emerald-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-semibold text-foreground">{p.title}</h3>
                        {p.description && (
                          <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{p.description}</p>
                        )}
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                            {(p.status || "IDEATION").replace(/_/g, " ")}
                          </span>
                          {relatedEnrollment && (
                            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                              <Link2 className="size-2.5" />
                              {relatedEnrollment.courseTitle}
                            </span>
                          )}
                          {relatedResearch.length > 0 && (
                            <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                              <Search className="size-2.5" />
                              {relatedResearch.length} linked research
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* Recent Learning Path */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Zap className="size-4 text-primary" />
          <h2 className="text-sm font-semibold text-foreground">Recent Learning Path</h2>
        </div>
        {learningPath.length === 0 ? (
          <p className="text-xs text-muted-foreground text-center py-4">Start learning to see your path here.</p>
        ) : (
          <div className="space-y-2">
            {learningPath.map((item) => {
              const Icon = item.icon
              return (
                <div
                  key={item.id}
                  className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50 cursor-pointer"
                  onClick={() => router.push(item.link)}
                >
                  <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Icon className="size-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                    <p className="truncate text-xs text-muted-foreground">{item.connectedTo}</p>
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-primary font-medium shrink-0">
                    Continue <ArrowRight className="size-3" />
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Related Research */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Lightbulb className="size-4 text-amber-500" />
          <h2 className="text-sm font-semibold text-foreground">Related Research</h2>
        </div>
        {research.length === 0 ? (
          <p className="text-xs text-muted-foreground text-center py-4">No research projects connected to your courses yet.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {research.slice(0, 4).map((r: any) => {
              const relatedEnrollment = enrollments.find((e) => e.courseId === r.subjectId)
              return (
                <div key={r.id} className="flex items-start gap-3 rounded-xl border border-border p-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
                    <Search className="size-4 text-amber-600 dark:text-amber-400" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-foreground line-clamp-1">{r.title}</p>
                    {relatedEnrollment && (
                      <p className="mt-0.5 text-xs text-muted-foreground">{relatedEnrollment.courseTitle}</p>
                    )}
                    {r.keywords && (
                      <div className="mt-1 flex flex-wrap gap-1">
                        {r.keywords.split(",").slice(0, 2).map((kw: string, i: number) => (
                          <span key={i} className="inline-block rounded-full bg-muted px-1.5 py-0.5 text-[9px] text-muted-foreground">
                            {kw.trim()}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Skills Map */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-2 mb-4">
          <Zap className="size-4 text-emerald-500" />
          <h2 className="text-sm font-semibold text-foreground">Skills Map</h2>
        </div>
        {connectedCompetencies.length === 0 ? (
          <p className="text-xs text-muted-foreground text-center py-4">No competencies mapped yet.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {connectedCompetencies.map((comp, idx) => {
              const statusColors: Record<string, string> = {
                NOT_STARTED: "bg-muted text-muted-foreground",
                LEARNING: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
                PRACTICING: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
                ASSESSED: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
                COMPETENT: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
                NEEDS_PRACTICE: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
                COMPLETED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
              }
              return (
                <div
                  key={idx}
                  className="flex items-center gap-2 rounded-full border border-border px-3 py-1.5"
                >
                  <div className="flex size-6 items-center justify-center rounded-full bg-primary/10">
                    <Zap className="size-3 text-primary" />
                  </div>
                  <span className="text-xs font-medium text-foreground">{comp.name}</span>
                  <span className={`inline-flex items-center rounded-full px-1.5 py-0.5 text-[9px] font-medium ${statusColors[comp.status] || "bg-muted text-muted-foreground"}`}>
                    {comp.status.replace(/_/g, " ")}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
