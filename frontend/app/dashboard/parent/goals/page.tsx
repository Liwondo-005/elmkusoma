"use client"

import { useEffect, useState } from "react"
import { Target, Loader2 } from "lucide-react"
import { parentApi, type ChildOverview, type GoalItem } from "@/lib/parent-api"

export default function ParentGoalsPage() {
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [goals, setGoals] = useState<GoalItem[]>([])
  const [activeCount, setActiveCount] = useState(0)
  const [completedCount, setCompletedCount] = useState(0)
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
    parentApi.getChildGoals(selectedChildId).then((data) => {
      setGoals(data.goals || [])
      setActiveCount(data.activeCount || 0)
      setCompletedCount(data.completedCount || 0)
    }).catch(() => { setGoals([]); setActiveCount(0); setCompletedCount(0) })
  }, [selectedChildId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  const activeGoals = goals.filter((g) => g.status === "ACTIVE")
  const completedGoals = goals.filter((g) => g.status === "COMPLETED")

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Learning Goals</h1>
        <p className="mt-1 text-sm text-muted-foreground">{activeCount} active &middot; {completedCount} completed</p>
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

      {goals.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <Target className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No learning goals set yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Goals will appear here once set by teachers or the system.</p>
        </div>
      ) : (
        <>
          {activeGoals.length > 0 && (
            <section>
              <h2 className="text-base font-semibold text-foreground mb-3">Active Goals</h2>
              <div className="space-y-3">
                {activeGoals.map((goal) => (
                  <GoalCard key={goal.id} goal={goal} />
                ))}
              </div>
            </section>
          )}

          {completedGoals.length > 0 && (
            <section>
              <h2 className="text-base font-semibold text-foreground mb-3">Completed</h2>
              <div className="space-y-3">
                {completedGoals.map((goal) => (
                  <GoalCard key={goal.id} goal={goal} />
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}

function GoalCard({ goal }: { goal: GoalItem }) {
  const progress = goal.progressPercentage || 0
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-start gap-3">
        <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${goal.status === "COMPLETED" ? "bg-green-100" : "bg-primary/10"}`}>
          <Target className={`size-4 ${goal.status === "COMPLETED" ? "text-green-600" : "text-primary"}`} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-foreground">{goal.title}</p>
          {goal.description && <p className="mt-1 text-xs text-muted-foreground">{goal.description}</p>}
          <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-muted">
            <div className={`h-full rounded-full transition-all ${goal.status === "COMPLETED" ? "bg-green-500" : "bg-primary"}`} style={{ width: `${progress}%` }} />
          </div>
          <div className="mt-2 flex items-center justify-between text-xs text-muted-foreground">
            <span>{Math.round(progress)}% complete</span>
            {goal.targetDate && <span>Target: {new Date(goal.targetDate).toLocaleDateString("en-GB", { month: "short", day: "numeric" })}</span>}
          </div>
        </div>
      </div>
    </div>
  )
}
