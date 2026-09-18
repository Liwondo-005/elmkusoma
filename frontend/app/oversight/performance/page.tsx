"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, Award, FileText, ClipboardList, BookOpen } from "lucide-react"
import Link from "next/link"

interface PerformanceMetrics {
  overallAverage: number
  passRate: number
  totalAssessments: number
  totalReportCards: number
  schoolPerformance: Array<{
    institutionId: string
    institutionName: string
    institutionCode: string
    averageScore: number
    passRate: number
    studentCount: number
    assessmentCount: number
  }>
  subjectPerformance: Array<{
    subjectName: string
    subjectCode: string
    averageScore: number
    passRate: number
    assessmentCount: number
  }>
}

export default function OversightPerformancePage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [metrics, setMetrics] = useState<PerformanceMetrics | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchPerformance()
    }
  }, [user, authLoading])

  async function fetchPerformance() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/performance`, {
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
        <div className="text-muted-foreground">Loading performance analytics...</div>
      </div>
    )
  }

  if (!metrics) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">Performance Analytics</h1>
          <p className="mt-2 text-muted-foreground">Performance data unavailable.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <TrendingUp className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Performance Analytics</h1>
            <p className="text-sm text-muted-foreground">
              Academic performance overview across your jurisdiction
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<BarChart3 className="size-5" />} label="Overall Average" value={`${metrics.overallAverage}%`} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<Award className="size-5" />} label="Pass Rate" value={`${metrics.passRate}%`} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<FileText className="size-5" />} label="Assessments" value={metrics.totalAssessments} color="bg-purple-500/10 text-purple-600" />
        <StatCard icon={<ClipboardList className="size-5" />} label="Report Cards" value={metrics.totalReportCards} color="bg-orange-500/10 text-orange-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <Building2 className="size-5 text-muted-foreground" />
          School Performance Comparison
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Code</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Students</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Assessments</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Avg Score</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Pass Rate</th>
              </tr>
            </thead>
            <tbody>
              {metrics.schoolPerformance.map((school) => (
                <tr key={school.institutionId} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">
                    <Link href={`/oversight/schools/${school.institutionId}`} className="hover:underline">
                      {school.institutionName}
                    </Link>
                  </td>
                  <td className="py-3 px-4 text-muted-foreground">{school.institutionCode}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.studentCount}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.assessmentCount}</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{school.averageScore}%</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{school.passRate}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <BookOpen className="size-5 text-muted-foreground" />
          Subject Performance
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Subject</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Code</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Assessments</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Avg Score</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Pass Rate</th>
              </tr>
            </thead>
            <tbody>
              {metrics.subjectPerformance.map((subject) => (
                <tr key={subject.subjectCode} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">{subject.subjectName}</td>
                  <td className="py-3 px-4 text-muted-foreground">{subject.subjectCode}</td>
                  <td className="py-3 px-4 text-right text-foreground">{subject.assessmentCount}</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{subject.averageScore}%</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{subject.passRate}%</td>
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