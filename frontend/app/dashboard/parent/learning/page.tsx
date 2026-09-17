"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { BookOpen, Loader2, ChevronRight, Video, Clock } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type ChildOverview, type ParentIntelligence } from "@/lib/parent-api"

export default function ParentLearningPage() {
  const { user } = useAuth()
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [intelligence, setIntelligence] = useState<ParentIntelligence | null>(null)
  const [liveClasses, setLiveClasses] = useState<Array<{ id: string; title: string; status: string; scheduledAt: string }>>([])
  const [library, setLibrary] = useState<{ categories: Array<{ name: string; description: string; items: Array<{ id: string; title: string; description: string; resourceType: string; fileUrl: string }> }> } | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (kids.length > 0) {
        const primary = kids.find((c) => c.isPrimary) || kids[0]
        setSelectedChildId(primary.studentId)
      }
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!selectedChildId) return
    Promise.all([
      parentApi.getChildIntelligence(selectedChildId).catch(() => null),
      parentApi.getChildLiveClasses(selectedChildId).catch(() => []),
      parentApi.getLibrary().catch(() => null),
    ]).then(([intel, lc, lib]) => {
      setIntelligence(intel)
      setLiveClasses(lc)
      setLibrary(lib)
    })
  }, [selectedChildId])

  if (loading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Learning</h1>
        <p className="mt-1 text-sm text-muted-foreground">Courses, lessons, and learning resources</p>
      </div>

      {children.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {children.map((child) => (
            <button key={child.studentId} onClick={() => setSelectedChildId(child.studentId)}
              className={`shrink-0 rounded-xl border px-4 py-2 text-sm font-medium transition-colors ${selectedChildId === child.studentId ? "border-primary bg-primary text-primary-foreground" : "border-border bg-card text-foreground hover:bg-muted"}`}>
              {child.studentName}
            </button>
          ))}
        </div>
      )}

      {/* Recommendations */}
      {intelligence && intelligence.recommendations.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">How You Can Help</h2>
          <div className="mt-3 space-y-3">
            {intelligence.recommendations.map((rec) => (
              <div key={rec.id} className="rounded-xl border border-border bg-muted/30 p-4">
                <p className="text-sm font-medium text-foreground">{rec.title}</p>
                <p className="mt-1 text-xs text-muted-foreground">{rec.description}</p>
                {rec.actionLabel && (
                  <Link href={rec.actionUrl || "#"} className="mt-2 inline-block text-xs font-medium text-primary hover:underline">
                    {rec.actionLabel} →
                  </Link>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Live Classes */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">Live Classes</h2>
          <Link href="/live-classes" className="text-xs font-medium text-primary hover:underline">View All</Link>
        </div>
        {liveClasses.length === 0 ? (
          <div className="mt-6 py-8 text-center">
            <Video className="mx-auto mb-3 size-8 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">No live classes scheduled</p>
          </div>
        ) : (
          <div className="mt-3 space-y-2">
            {liveClasses.slice(0, 5).map((lc) => (
              <div key={lc.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                <Video className="size-4 shrink-0 text-blue-500" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{lc.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {lc.scheduledAt ? new Date(lc.scheduledAt).toLocaleString("en-GB", { weekday: "short", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : ""}
                  </p>
                </div>
                <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${lc.status === "IN_PROGRESS" || lc.status === "LIVE" ? "bg-green-100 text-green-700" : "bg-muted text-muted-foreground"}`}>
                  {lc.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Learning Library */}
      {library && library.categories.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Family Learning Library</h2>
          <div className="mt-3 space-y-4">
            {library.categories.map((cat) => (
              <div key={cat.name}>
                <h3 className="text-sm font-medium text-foreground">{cat.name}</h3>
                <p className="text-xs text-muted-foreground">{cat.description}</p>
                <div className="mt-2 space-y-1">
                  {cat.items.slice(0, 5).map((item) => (
                    <div key={item.id} className="flex items-center gap-3 rounded-lg border border-border p-2.5">
                      <BookOpen className="size-4 shrink-0 text-muted-foreground" />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-foreground">{item.title}</p>
                        <p className="text-xs text-muted-foreground">{item.resourceType}</p>
                      </div>
                      {item.fileUrl && (
                        <a href={item.fileUrl} target="_blank" rel="noopener noreferrer" className="shrink-0 text-xs font-medium text-primary hover:underline">
                          Open
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Goals */}
      {selectedChildId && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-foreground">Learning Goals</h2>
            <Link href="/dashboard/parent/goals" className="text-xs font-medium text-primary hover:underline">View All</Link>
          </div>
          <GoalsPreview childId={selectedChildId} />
        </section>
      )}
    </div>
  )
}

function GoalsPreview({ childId }: { childId: string }) {
  const [goals, setGoals] = useState<{ activeCount: number; completedCount: number; goals: Array<{ id: string; title: string; status: string; progressPercentage: number }> } | null>(null)

  useEffect(() => {
    parentApi.getChildGoals(childId).then(setGoals).catch(() => null)
  }, [childId])

  if (!goals) return <div className="mt-4 py-4 text-center"><Loader2 className="size-4 animate-spin text-muted-foreground" /></div>
  if (goals.goals.length === 0) return <p className="mt-4 text-sm text-muted-foreground">No learning goals set yet.</p>

  return (
    <div className="mt-3 space-y-2">
      {goals.goals.slice(0, 3).map((goal) => (
        <div key={goal.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
          <div className={`size-2 shrink-0 rounded-full ${goal.status === "COMPLETED" ? "bg-green-500" : "bg-blue-500"}`} />
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium text-foreground">{goal.title}</p>
            <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-muted">
              <div className="h-full rounded-full bg-primary" style={{ width: `${goal.progressPercentage || 0}%` }} />
            </div>
          </div>
          <span className="text-xs text-muted-foreground">{Math.round(goal.progressPercentage || 0)}%</span>
        </div>
      ))}
    </div>
  )
}
