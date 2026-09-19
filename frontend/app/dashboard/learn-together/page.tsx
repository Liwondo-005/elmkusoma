"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type LearningCollaboration } from "@/lib/api"
import { Users, BookOpen, Trophy, Share2, MessageSquare } from "lucide-react"

export default function LearnTogetherPage() {
  const { user } = useRequireAuth()
  const [collaborations, setCollaborations] = useState<LearningCollaboration[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await primaryApi.getCollaborations().catch(() => [])
      setCollaborations(data.filter(c => c.collaborationType === "LEARN_TOGETHER"))
    } catch {} finally {
      setLoading(false)
    }
  }

  const activities = [
    { title: "Study Groups", description: "Find study partners", icon: Users, color: "text-blue-500", bg: "bg-blue-500/10" },
    { title: "Partner Reading", description: "Read together", icon: BookOpen, color: "text-green-500", bg: "bg-green-500/10" },
    { title: "Team Challenges", description: "Compete as a team", icon: Trophy, color: "text-amber-500", bg: "bg-amber-500/10" },
    { title: "Share My Work", description: "Show your creations", icon: Share2, color: "text-purple-500", bg: "bg-purple-500/10" },
  ]

  if (loading) {
    return <div className="flex min-h-[50vh] items-center justify-center"><div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-blue-50/50 via-card to-purple-50/50 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-500/10"><Users className="size-6 text-blue-600" /></div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Learn Together</h1>
            <p className="text-sm text-muted-foreground">Learning is more fun with friends!</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {activities.map(a => (
          <div key={a.title} className="rounded-2xl border border-border bg-card p-5 shadow-xs text-center transition-all hover:shadow-md hover:border-primary/30 cursor-pointer">
            <div className={`mx-auto flex size-12 items-center justify-center rounded-2xl ${a.bg}`}><a.icon className={`size-6 ${a.color}`} /></div>
            <h3 className="mt-3 text-sm font-semibold text-foreground">{a.title}</h3>
            <p className="mt-1 text-xs text-muted-foreground">{a.description}</p>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">My Collaborations</h2>
        {collaborations.length === 0 ? (
          <div className="py-8 text-center">
            <Users className="mx-auto size-12 text-muted-foreground/50" />
            <p className="mt-3 text-sm font-medium text-foreground">No collaborations yet</p>
            <p className="text-xs text-muted-foreground">Invite a friend to learn together!</p>
          </div>
        ) : (
          <div className="space-y-3">
            {collaborations.map(c => (
              <div key={c.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-blue-500/10"><Users className="size-4 text-blue-500" /></div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground">{c.activity}</p>
                  <p className="text-xs text-muted-foreground">with {c.partnerName}</p>
                </div>
                {c.isCompleted && <span className="text-xs font-medium text-green-600 bg-green-100 px-2 py-0.5 rounded-full">Done</span>}
                {!c.isCompleted && <MessageSquare className="size-4 text-muted-foreground" />}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
