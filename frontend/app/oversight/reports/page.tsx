"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, TrendingUp, BarChart3, FileBarChart, FileText, Download, Calendar, ClipboardList, Award, BookOpen, Video, AlertTriangle } from "lucide-react"

interface Report {
  id: string
  title: string
  description: string
  type: string
  icon: string
  color: string
  available: boolean
}

const reportTypes: Report[] = [
  {
    id: "school-performance",
    title: "School Performance Report",
    description: "Comprehensive academic performance analysis across all schools in jurisdiction",
    type: "Academic",
    icon: "Award",
    color: "bg-purple-500/10 text-purple-600",
    available: true,
  },
  {
    id: "attendance-report",
    title: "Attendance Report",
    description: "Detailed attendance analytics with trends, patterns, and at-risk identification",
    type: "Attendance",
    icon: "ClipboardList",
    color: "bg-teal-500/10 text-teal-600",
    available: true,
  },
  {
    id: "teacher-activity",
    title: "Teacher Activity Report",
    description: "Teacher workload, class assignments, live class sessions, and engagement metrics",
    type: "HR",
    icon: "Users",
    color: "bg-blue-500/10 text-blue-600",
    available: true,
  },
  {
    id: "student-statistics",
    title: "Student Statistics Report",
    description: "Enrollment trends, demographics, progression, and outcome analytics",
    type: "Demographics",
    icon: "GraduationCap",
    color: "bg-green-500/10 text-green-600",
    available: true,
  },
  {
    id: "assessment-report",
    title: "Assessment Report",
    description: "Assessment activity, completion rates, score distributions, and grading analytics",
    type: "Assessment",
    icon: "FileText",
    color: "bg-indigo-500/10 text-indigo-600",
    available: true,
  },
  {
    id: "curriculum-progress",
    title: "Curriculum Progress Report",
    description: "Lesson completion rates, topic coverage, and curriculum pacing analysis",
    type: "Curriculum",
    icon: "BookOpen",
    color: "bg-pink-500/10 text-pink-600",
    available: true,
  },
  {
    id: "live-class-activity",
    title: "Live Class Activity Report",
    description: "Live class schedules, participation rates, teacher performance, and session analytics",
    type: "Live Classes",
    icon: "Video",
    color: "bg-red-500/10 text-red-600",
    available: true,
  },
  {
    id: "school-comparison",
    title: "School Comparison Report",
    description: "Side-by-side school comparison across all key metrics and indicators",
    type: "Comparison",
    icon: "BarChart3",
    color: "bg-orange-500/10 text-orange-600",
    available: true,
  },
]

export default function OversightReportsPage() {
  const { user, loading: authLoading } = useRequireAuth()

  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading reports...</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <FileBarChart className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Reports Center</h1>
            <p className="text-sm text-muted-foreground">
              Generate and download jurisdiction-scoped reports
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {reportTypes.map((report) => (
          <ReportCard key={report.id} report={report} />
        ))}
      </div>
    </div>
  )
}

function ReportCard({ report }: { report: Report }) {
  const icons: Record<string, React.ReactNode> = {
    Award: <Award className="size-5" />,
    ClipboardList: <ClipboardList className="size-5" />,
    Users: <Users className="size-5" />,
    GraduationCap: <GraduationCap className="size-5" />,
    FileText: <FileText className="size-5" />,
    BookOpen: <BookOpen className="size-5" />,
    Video: <Video className="size-5" />,
    BarChart3: <BarChart3 className="size-5" />,
  }

  const Icon = icons[report.icon] || <FileBarChart className="size-5" />

  return (
    <div className="rounded-2xl border border-border bg-card p-5 hover:bg-muted/50 transition-colors cursor-pointer">
      <div className="flex items-start gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${report.color}`}>
          {Icon}
        </div>
        <div className="flex-1">
          <h3 className="font-medium text-foreground">{report.title}</h3>
          <p className="text-sm text-muted-foreground mt-1">{report.description}</p>
          <div className="flex items-center gap-2 mt-3">
            <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-700">
              {report.type}
            </span>
            {report.available && (
              <button className="text-primary hover:underline text-sm flex items-center gap-1">
                <Download className="size-3" />
                Generate
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

