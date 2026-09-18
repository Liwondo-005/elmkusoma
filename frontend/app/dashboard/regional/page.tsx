"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, ChevronRight } from "lucide-react"

interface District {
  id: string
  name: string
  code: string
  regionName: string
  institutionCount: number
  teacherCount: number
  studentCount: number
  isActive: boolean
}

interface RegionStats {
  totalInstitutions: number
  totalTeachers: number
  totalStudents: number
}

export default function RegionalDashboardPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [districts, setDistricts] = useState<District[]>([])
  const [stats, setStats] = useState<RegionStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchDashboard()
    }
  }, [user, authLoading])

  async function fetchDashboard() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const regionId = user?.regionId || ""

      const [dashRes, distRes] = await Promise.all([
        fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/dashboard${regionId ? `?regionId=${regionId}` : ""}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        regionId
          ? fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/regions/${regionId}/districts`, {
              headers: { Authorization: `Bearer ${token}` },
            })
          : null,
      ])

      if (dashRes.ok) {
        const data = await dashRes.json()
        setStats({
          totalInstitutions: data.totalInstitutions,
          totalTeachers: data.totalTeachers,
          totalStudents: data.totalStudents,
        })
      }

      if (distRes?.ok) {
        setDistricts(await distRes.json())
      }
    } catch {
      // Dashboard unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading regional dashboard...</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-green-500/10">
            <MapPin className="size-5 text-green-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Regional Education Oversight</h1>
            <p className="text-sm text-muted-foreground">Welcome, {user?.name}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard icon={<Building2 className="size-5" />} label="Institutions" value={stats?.totalInstitutions ?? 0} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<Users className="size-5" />} label="Teachers" value={stats?.totalTeachers ?? 0} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<GraduationCap className="size-5" />} label="Students" value={stats?.totalStudents ?? 0} color="bg-purple-500/10 text-purple-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4">Districts in Your Region</h2>
        {districts.length === 0 ? (
          <p className="text-muted-foreground">No districts found.</p>
        ) : (
          <div className="space-y-3">
            {districts.map((district) => (
              <div
                key={district.id}
                className="flex items-center justify-between rounded-xl border border-border p-4 hover:bg-muted/50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="flex size-8 items-center justify-center rounded-lg bg-teal-500/10">
                    <MapPin className="size-4 text-teal-600" />
                  </div>
                  <div>
                    <p className="font-medium text-foreground">{district.name}</p>
                    <p className="text-sm text-muted-foreground">{district.code} — {district.institutionCount} institutions</p>
                  </div>
                </div>
                <ChevronRight className="size-4 text-muted-foreground" />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          {icon}
        </div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value.toLocaleString()}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}
