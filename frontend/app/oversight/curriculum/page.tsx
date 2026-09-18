"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, BookOpen, Target, Clock, AlertTriangle, CheckCircle } from "lucide-react"
import Link from "next/link"

interface CurriculumMetrics {
  overallProgress: number
  totalLessons: number
  completedLessons: number
  schoolsOnTrack: number
  schoolsBehind: number
  subjectProgress: Array<{
    subjectName: string
    subjectCode: string
    totalLessons: number
    completedLessons: number
    progressRate: number
  }>
  schoolProgress: Array<{
    institutionId: string
    institutionName: string
    institutionCode: string
    totalLessons: number
    completedLessons: number
    progressRate: number
    status: "ON_TRACK" | "BEHIND" | "AT_RISK"
  }>
}

export default function OversightCurriculumPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [metrics, setMetrics] = useState<CurriculumMetrics | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchCurriculum()
    }
  }, [user, authLoading])

  async function fetchCurriculum() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/curriculum`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        setMetrics(await res.json())
      }
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading curriculum analytics...</div>
      </div>
    )
  }

  if (!metrics) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">Curriculum Monitoring</h1>
          <p className="mt-2 text-muted-foreground">Curriculum data unavailable.</p>
        </div>
      </div>
    )
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case "ON_TRACK": return "bg-green-100 text-green-700"
      case "BEHIND": return "bg-orange-100 text-orange-700"
      case "AT_RISK": return "bg-red-100 text-red-700"
      default: return "bg-gray-100 text-gray-700"
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <BookOpen className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Curriculum Monitoring</h1>
            <p className="text-sm text-muted-foreground">
              Curriculum progress tracking across your jurisdiction
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Target className="size-5" />} label="Overall Progress" value={`${metrics.overallProgress}%`} color="bg-teal-500/10 text-teal-600" />
        <StatCard icon={<BookOpen className="size-5" />} label="Total Lessons" value={metrics.totalLessons} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<CheckCircle className="size-5" />} label="Completed" value={metrics.completedLessons} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<AlertTriangle className="size-5" />} label="Schools Behind" value={metrics.schoolsBehind} color="bg-orange-500/10 text-orange-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <BookOpen className="size-5 text-muted-foreground" />
          Subject Progress Overview
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Subject</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Code</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Total Lessons</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Completed</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Progress</th>
              </tr>
            </thead>
            <tbody>
              {metrics.subjectProgress.map((subject) => (
                <tr key={subject.subjectCode} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">{subject.subjectName}</td>
                  <td className="py-3 px-4 text-muted-foreground">{subject.subjectCode}</td>
                  <td className="py-3 px-4 text-right text-foreground">{subject.totalLessons}</td>
                  <td className="py-3 px-4 text-right text-foreground">{subject.completedLessons}</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{subject.progressRate}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <Building2 className="size-5 text-muted-foreground" />
          School Curriculum Progress
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Code</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Total Lessons</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Completed</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Progress</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground">Status</th>
              </tr>
            </thead>
            <tbody>
              {metrics.schoolProgress.map((school) => (
                <tr key={school.institutionId} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">
                    <Link href={`/oversight/schools/${school.institutionId}`} className="hover:underline">
                      {school.institutionName}
                    </Link>
                  </td>
                  <td className="py-3 px-4 text-muted-foreground">{school.institutionCode}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.totalLessons}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.completedLessons}</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{school.progressRate}%</td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${getStatusColor(school.status)}`}>
                      {school.status.replace("_", " ")}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: string | number; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          {icon}
        </div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}