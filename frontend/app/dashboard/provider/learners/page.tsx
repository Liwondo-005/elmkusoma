"use client"
import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { Loader2, Plus, Search } from "lucide-react"

export default function LearnersPage() {
  const [learners, setLearners] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")

  useEffect(() => {
    loadLearners()
  }, [])

  async function loadLearners() {
    try {
      setLoading(true)
      const data = await nfeApi.listLearners()
      setLearners(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  const filtered = learners.filter((l) =>
    (l.name || l.fullName || "").toLowerCase().includes(search.toLowerCase()) ||
    (l.email || "").toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Learners</h1>
          <p className="text-muted-foreground">Manage enrolled NFE learners</p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> Add Learner
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder="Search learners..."
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
            <p className="py-4 text-center text-sm text-muted-foreground">No learners found.</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((learner) => (
                <div key={learner.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{learner.name || learner.fullName}</p>
                    <p className="text-xs text-muted-foreground">{learner.email || learner.phone || "No contact info"}</p>
                  </div>
                  <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">{learner.status || "Active"}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
