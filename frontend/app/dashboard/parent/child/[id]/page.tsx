"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { ArrowLeft, Clock, FileText, BarChart3, BookOpen, ChevronRight, Loader2 } from "lucide-react"
import { parentApi, type ChildOverview } from "@/lib/parent-api"

export default function ChildDetailPage() {
  const params = useParams()
  const studentId = params.id as string
  const [child, setChild] = useState<ChildOverview | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getChild(studentId).then(setChild).finally(() => setLoading(false))
  }, [studentId])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!child) {
    return (
      <div className="py-16 text-center">
        <p className="text-sm text-muted-foreground">Child not found or not linked to your account.</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/parent" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> Back to Dashboard
      </Link>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-xl font-bold text-foreground">{child.studentName}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {child.admissionNumber} &middot; {child.relationshipType}
            </p>
          </div>
          {child.isPrimary && (
            <span className="rounded bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">PRIMARY</span>
          )}
        </div>

        <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {child.attendancePercentage !== null ? `${Math.round(child.attendancePercentage)}%` : "—"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Attendance</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">{child.latestGrade || "—"}</p>
            <p className="mt-1 text-xs text-muted-foreground">Latest Grade</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">{child.pendingAssignments}</p>
            <p className="mt-1 text-xs text-muted-foreground">Pending Tasks</p>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {child.learningProgress !== null ? `${Math.round(child.learningProgress)}%` : "—"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Learning Progress</p>
          </div>
        </div>

        {child.classRank && child.totalStudentsInClass && (
          <div className="mt-4 rounded-xl border border-border bg-muted/30 p-4 text-center">
            <p className="text-sm text-muted-foreground">
              Class Rank: <span className="font-bold text-foreground">{child.classRank}</span> of {child.totalStudentsInClass}
            </p>
          </div>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Link
          href={`/dashboard/parent/attendance?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <Clock className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">Attendance</p>
            <p className="text-xs text-muted-foreground">{child.daysPresent} present, {child.daysAbsent} absent</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/assignments?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <FileText className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">Assignments</p>
            <p className="text-xs text-muted-foreground">{child.pendingAssignments} pending, {child.overdueAssignments} overdue</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href={`/dashboard/parent/results?child=${studentId}`}
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <BarChart3 className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">Results</p>
            <p className="text-xs text-muted-foreground">View grades and report cards</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
        <Link
          href="/live-classes"
          className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <BookOpen className="size-8 shrink-0 text-primary" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-foreground">Live Classes</p>
            <p className="text-xs text-muted-foreground">Available learning sessions</p>
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
      </div>
    </div>
  )
}
