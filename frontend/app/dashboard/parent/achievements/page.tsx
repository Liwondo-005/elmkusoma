"use client"

import { useEffect, useState } from "react"
import { Trophy, Loader2, Star } from "lucide-react"
import { parentApi, type ChildOverview, type AchievementItem } from "@/lib/parent-api"

export default function ParentAchievementsPage() {
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [achievements, setAchievements] = useState<AchievementItem[]>([])
  const [total, setTotal] = useState(0)
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
    parentApi.getChildAchievements(selectedChildId).then((data) => {
      setAchievements(data.achievements || [])
      setTotal(data.totalAchievements || 0)
    }).catch(() => { setAchievements([]); setTotal(0) })
  }, [selectedChildId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Achievements</h1>
        <p className="mt-1 text-sm text-muted-foreground">{total} achievements earned</p>
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

      {achievements.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <Trophy className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No achievements yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Achievements will appear here as your child progresses.</p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {achievements.map((ach) => (
            <div key={ach.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                  <Trophy className="size-5 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{ach.title}</p>
                  {ach.description && <p className="mt-1 text-xs text-muted-foreground">{ach.description}</p>}
                  <p className="mt-2 text-xs text-muted-foreground">
                    {new Date(ach.achievedAt).toLocaleDateString("en-GB", { month: "short", day: "numeric", year: "numeric" })}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
