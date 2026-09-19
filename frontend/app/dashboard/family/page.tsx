"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type LearningCollaboration } from "@/lib/api"
import { Home, Brain, BookOpen, FlaskConical, Palette, Music, Share2 } from "lucide-react"

export default function FamilyPage() {
  const { user } = useRequireAuth()
  const [activities, setActivities] = useState<LearningCollaboration[]>([])
  const [loading, setLoading] = useState(true)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await primaryApi.getCollaborations().catch(() => [])
      setActivities(data.filter(c => c.collaborationType === "FAMILY"))
    } catch {} finally {
      setLoading(false)
    }
  }

  function handleShare() {
    navigator.clipboard?.writeText(window.location.origin + "/dashboard/lessons").then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }).catch(() => {})
  }

  const familyActivities = [
    { title: "Family Quiz Night", description: "Test your knowledge together", icon: Brain, color: "text-amber-500", bg: "bg-amber-500/10" },
    { title: "Read Together", description: "Share stories with family", icon: BookOpen, color: "text-green-500", bg: "bg-green-500/10" },
    { title: "Science at Home", description: "Try experiments at home", icon: FlaskConical, color: "text-blue-500", bg: "bg-blue-500/10" },
    { title: "Art & Craft", description: "Create together", icon: Palette, color: "text-pink-500", bg: "bg-pink-500/10" },
    { title: "Music Time", description: "Sing and play together", icon: Music, color: "text-purple-500", bg: "bg-purple-500/10" },
  ]

  if (loading) {
    return <div className="flex min-h-[50vh] items-center justify-center"><div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-pink-50/50 via-card to-amber-50/50 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-pink-500/10"><Home className="size-6 text-pink-600" /></div>
          <div className="flex-1">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Family Learning</h1>
            <p className="text-sm text-muted-foreground">Learn together with your family!</p>
          </div>
          <button onClick={handleShare} className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted">
            <Share2 className="size-3.5" /> {copied ? "Copied!" : "Share with Family"}
          </button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {familyActivities.map(a => (
          <div key={a.title} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:shadow-md hover:border-primary/30 cursor-pointer">
            <div className={`flex size-10 items-center justify-center rounded-xl ${a.bg}`}><a.icon className={`size-5 ${a.color}`} /></div>
            <h3 className="mt-3 text-sm font-semibold text-foreground">{a.title}</h3>
            <p className="mt-1 text-xs text-muted-foreground">{a.description}</p>
          </div>
        ))}
      </div>

      {activities.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">Recent Family Activities</h2>
          <div className="space-y-3">
            {activities.map(a => (
              <div key={a.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <div className="flex size-8 items-center justify-center rounded-lg bg-pink-500/10"><Home className="size-4 text-pink-500" /></div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground">{a.activity}</p>
                  <p className="text-xs text-muted-foreground">with {a.partnerName}</p>
                </div>
                {a.isCompleted && <span className="text-xs font-medium text-green-600 bg-green-100 px-2 py-0.5 rounded-full">Done</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
