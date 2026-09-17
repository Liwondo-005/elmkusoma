"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, FileText, Award, PenTool, ClipboardList } from "lucide-react"
import Link from "next/link"

interface AssessmentMetrics {
  totalAssessments: number
  completedAssessments: number
  averageScore: number
  passRate: number
  pendingGrading: number
  schoolAssessments: Array<{
    institutionId: string
    institutionName: string
    institutionCode: string
    assessmentCount: number
    completedCount: number
    averageScore: number
    passRate: number
  }>
  recentAssessments: Array<{
    id: string
    title: string
    institutionName: string
    subjectName: string
    scheduledDate: string
    status: string
    participantCount: number
  }>
}

export default function OversightAssessmentsPage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [metrics, setMetrics] = useState<AssessmentMetrics | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchAssessments()
    }
  }, [user, authLoading])

  async function fetchAssessments() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/assessments`, {
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
        <div className="text-muted-foreground">Loading assessment analytics...</div>
      </div>
    )
  }

  if (!metrics) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">Assessment Analytics</h1>
          <p className="mt-2 text-muted-foreground">Assessment data unavailable.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <FileText className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Assessment Analytics</h1>
            <p className="text-sm text-muted-foreground">
              Assessment activity and performance across your jurisdiction
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <StatCard icon={<FileText className="size-5" />} label="Total" value={metrics.totalAssessments} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<CheckCircle className="size-5" />} label="Completed" value={metrics.completedAssessments} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<Award className="size-5" />} label="Avg Score" value={`${metrics.averageScore}%`} color="bg-purple-500/10 text-purple-600" />
        <StatCard icon={<Award className="size-5" />} label="Pass Rate" value={`${metrics.passRate}%`} color="bg-indigo-500/10 text-indigo-600" />
        <StatCard icon={<PenTool className="size-5" />} label="Pending Grading" value={metrics.pendingGrading} color="bg-orange-500/10 text-orange-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <Building2 className="size-5 text-muted-foreground" />
          School Assessment Activity
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Code</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Assessments</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Completed</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Avg Score</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Pass Rate</th>
              </tr>
            </thead>
            <tbody>
              {metrics.schoolAssessments.map((school) => (
                <tr key={school.institutionId} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">
                    <Link href={`/oversight/schools/${school.institutionId}`} className="hover:underline">
                      {school.institutionName}
                    </Link>
                  </td>
                  <td className="py-3 px-4 text-muted-foreground">{school.institutionCode}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.assessmentCount}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.completedCount}</td>
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
          <ClipboardList className="size-5 text-muted-foreground" />
          Recent Assessments
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Title</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Subject</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Date</th>
                <th className="text-center py-3 px-4 font-medium text-muted-foreground">Status</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Participants</th>
              </tr>
            </thead>
            <tbody>
              {metrics.recentAssessments.map((assessment) => (
                <tr key={assessment.id} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">{assessment.title}</td>
                  <td className="py-3 px-4 text-muted-foreground">{assessment.institutionName}</td>
                  <td className="py-3 px-4 text-muted-foreground">{assessment.subjectName}</td>
                  <td className="py-3 px-4 text-muted-foreground">{new Date(assessment.scheduledDate).toLocaleDateString()}</td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                      assessment.status === "COMPLETED" ? "bg-green-100 text-green-700" :
                      assessment.status === "IN_PROGRESS" ? "bg-blue-100 text-blue-700" :
                      "bg-gray-100 text-gray-700"
                    }`}>
                      {assessment.status}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right text-foreground">{assessment.participantCount}</td>
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

import { CheckCircle } from "lucide-react"