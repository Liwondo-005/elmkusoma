"use client"
import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export default function ProviderDashboardPage() {
  const [stats, setStats] = useState({ programs: 0, learners: 0, sessions: 0, certificates: 0 })

  useEffect(() => {
    const token = localStorage.getItem("elmkusoma_access_token")
    if (!token) { window.location.href = "/login"; return }
    setStats({ programs: 12, learners: 156, sessions: 8, certificates: 42 })
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Provider Dashboard</h1>
        <p className="text-muted-foreground">Manage your non-formal education programs</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Programs</CardTitle>
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.programs}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Learners</CardTitle>
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.learners}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Sessions</CardTitle>
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.sessions}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Certificates</CardTitle>
          </CardHeader>
          <CardContent><div className="text-2xl font-bold">{stats.certificates}</div></CardContent>
        </Card>
      </div>
      <Card>
        <CardHeader><CardTitle>Recent Programs</CardTitle></CardHeader>
        <CardContent>
          <p className="text-muted-foreground">No programs yet. Create your first program to get started.</p>
        </CardContent>
      </Card>
    </div>
  )
}
