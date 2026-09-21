"use client"
import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Loader2 } from "lucide-react"

interface ProviderStats {
  totalPrograms: number
  activePrograms: number
  totalLearners: number
  activeLearners: number
  totalSessions: number
  completedSessions: number
  totalCertificates: number
  totalProviders: number
  activeProviders: number
}

export default function ProviderDashboardPage() {
  const [stats, setStats] = useState<ProviderStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadStats = async () => {
      try {
        const token = localStorage.getItem("elmkusoma_access_token")
        if (!token) { window.location.href = "/login"; return }

        const res = await fetch("/v1/nfe/providers/stats", {
          headers: {
            "Authorization": `Bearer ${token}`,
            "X-Institution-Id": localStorage.getItem("elmkusoma_institution_id") || ""
          }
        })
        if (res.ok) {
          const data = await res.json()
          setStats(data.data || data)
        } else {
          setError("Failed to load stats")
        }
      } catch {
        setError("Network error")
      } finally {
        setLoading(false)
      }
    }
    loadStats()
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="size-8 animate-spin text-muted-foreground" />
    </div>
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Provider Dashboard</h1>
        <p className="text-muted-foreground">Manage your non-formal education programs</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Providers</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalProviders ?? 0}</div>
            <p className="text-xs text-muted-foreground">{stats?.activeProviders ?? 0} active</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Programs</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalPrograms ?? 0}</div>
            <p className="text-xs text-muted-foreground">{stats?.activePrograms ?? 0} active</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Learners</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalLearners ?? 0}</div>
            <p className="text-xs text-muted-foreground">{stats?.activeLearners ?? 0} active</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Sessions</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalSessions ?? 0}</div>
            <p className="text-xs text-muted-foreground">{stats?.completedSessions ?? 0} completed</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Certificates Issued</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">{stats?.totalCertificates ?? 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <a href="/dashboard/provider/programs" className="block rounded-xl border border-border px-4 py-3 text-sm font-medium hover:bg-muted transition-colors">Manage Programs</a>
            <a href="/dashboard/provider/sessions" className="block rounded-xl border border-border px-4 py-3 text-sm font-medium hover:bg-muted transition-colors">View Sessions</a>
            <a href="/dashboard/provider/learners" className="block rounded-xl border border-border px-4 py-3 text-sm font-medium hover:bg-muted transition-colors">Manage Learners</a>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
