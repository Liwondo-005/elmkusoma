"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, AlertTriangle, ClipboardList, Calendar, Target } from "lucide-react"
import Link from "next/link"

interface AttendanceMetrics {
  overallRate: number
  totalStudents: number
  schoolsAtRisk: number
  dailyTrend: Array<{ date: string; present: number; absent: number; late: number; excused: number }>
  schoolAttendance: Array<{
    institutionId: string
    institutionName: string
    institutionCode: string
    studentCount: number
    attendanceRate: number
    absentCount: number
    lateCount: number
  }>
  lowAttendanceStudents: Array<{
    studentId: string
    studentName: string
    institutionName: string
    attendanceRate: number
    daysAbsent: number
  }>
}

export default function OversightAttendancePage() {
  const { user, loading: authLoading } = useRequireAuth()
  const [metrics, setMetrics] = useState<AttendanceMetrics | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchAttendance()
    }
  }, [user, authLoading])

  async function fetchAttendance() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/attendance`, {
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
        <div className="text-muted-foreground">Loading attendance analytics...</div>
      </div>
    )
  }

  if (!metrics) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">Attendance Analytics</h1>
          <p className="mt-2 text-muted-foreground">Attendance data unavailable.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <ClipboardList className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Attendance Analytics</h1>
            <p className="text-sm text-muted-foreground">
              Attendance monitoring across your jurisdiction
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Target className="size-5" />} label="Overall Rate" value={`${metrics.overallRate}%`} color="bg-teal-500/10 text-teal-600" />
        <StatCard icon={<GraduationCap className="size-5" />} label="Total Students" value={metrics.totalStudents} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<AlertTriangle className="size-5" />} label="Schools at Risk" value={metrics.schoolsAtRisk} color="bg-orange-500/10 text-orange-600" />
        <StatCard icon={<Building2 className="size-5" />} label="Low Attendance" value={metrics.lowAttendanceStudents.length} color="bg-red-500/10 text-red-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
          <Building2 className="size-5 text-muted-foreground" />
          School Attendance Overview
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                <th className="text-left py-3 px-4 font-medium text-muted-foreground">Code</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Students</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Attendance</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Absent</th>
                <th className="text-right py-3 px-4 font-medium text-muted-foreground">Late</th>
              </tr>
            </thead>
            <tbody>
              {metrics.schoolAttendance.map((school) => (
                <tr key={school.institutionId} className="border-b border-border last:border-0 hover:bg-muted/50">
                  <td className="py-3 px-4 font-medium text-foreground">
                    <Link href={`/oversight/schools/${school.institutionId}`} className="hover:underline">
                      {school.institutionName}
                    </Link>
                  </td>
                  <td className="py-3 px-4 text-muted-foreground">{school.institutionCode}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.studentCount}</td>
                  <td className="py-3 px-4 text-right font-medium text-foreground">{school.attendanceRate}%</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.absentCount}</td>
                  <td className="py-3 px-4 text-right text-foreground">{school.lateCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {metrics.lowAttendanceStudents.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
            <AlertTriangle className="size-5 text-orange-500" />
            Students Requiring Attention (Attendance < 75%)
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">Student</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">School</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">Attendance</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">Days Absent</th>
                </tr>
              </thead>
              <tbody>
                {metrics.lowAttendanceStudents.slice(0, 20).map((student) => (
                  <tr key={student.studentId} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4 font-medium text-foreground">{student.studentName}</td>
                    <td className="py-3 px-4 text-muted-foreground">{student.institutionName}</td>
                    <td className="py-3 px-4 text-right font-medium text-red-600">{student.attendanceRate}%</td>
                    <td className="py-3 px-4 text-right text-foreground">{student.daysAbsent}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
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