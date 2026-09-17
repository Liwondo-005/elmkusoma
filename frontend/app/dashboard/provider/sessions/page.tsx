"use client"
import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { Loader2, Plus, Search } from "lucide-react"

export default function SessionsPage() {
  const [sessions, setSessions] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")

  useEffect(() => {
    loadSessions()
  }, [])

  async function loadSessions() {
    try {
      setLoading(true)
      const data = await nfeApi.listSessions()
      setSessions(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  const filtered = sessions.filter((s) =>
    (s.title || s.name || "").toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Sessions</h1>
          <p className="text-muted-foreground">Manage training sessions</p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> Create Session
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder="Search sessions..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
          ) : filtered.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No sessions found.</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((session) => (
                <div key={session.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{session.title || session.name}</p>
                    <p className="text-xs text-muted-foreground">{session.date || session.scheduledAt || "No date set"}</p>
                  </div>
                  <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">{session.status || "Scheduled"}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
