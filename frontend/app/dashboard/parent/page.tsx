"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Users, AlertTriangle, CheckCircle, Clock, FileText, BarChart3, ChevronRight, Loader2 } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type FamilyOverview, type ChildOverview } from "@/lib/parent-api"

export default function ParentDashboardPage() {
  const { user } = useAuth()
  const firstName = user?.name?.split(" ")[0] || "Parent"
  const [overview, setOverview] = useState<FamilyOverview | null>(null)
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        const [ov, kids] = await Promise.all([
          parentApi.getOverview(),
          parentApi.getChildren(),
        ])
        setOverview(ov)
        setChildren(kids)
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "Failed to load dashboard"
        setError(msg)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl py-16 text-center">
        <p className="text-sm text-muted-foreground">{error}</p>
        <p className="mt-2 text-xs text-muted-foreground">
          Make sure your parent profile is set up and you have children linked to your account.
        </p>
      </div>
    )
  }

  const highPriorityActions = overview?.todayActions.filter((a) => a.priority === "HIGH") || []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Welcome back, {firstName}!
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here is what is happening with your children today.
        </p>
      </div>

      {/* Family Summary Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <Users className="mb-2 size-5 text-muted-foreground" />
          <p className="text-2xl font-extrabold text-foreground">{overview?.totalChildren || 0}</p>
          <p className="text-sm font-medium text-foreground">Children</p>
          <p className="text-xs text-muted-foreground">Linked to your account</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <AlertTriangle className="mb-2 size-5 text-orange" />
          <p className="text-2xl font-extrabold text-foreground">{highPriorityActions.length}</p>
          <p className="text-sm font-medium text-foreground">Needs Attention</p>
          <p className="text-xs text-muted-foreground">Items requiring action</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <FileText className="mb-2 size-5 text-muted-foreground" />
          <p className="text-2xl font-extrabold text-foreground">
            {children.reduce((sum, c) => sum + (c.pendingAssignments || 0), 0)}
          </p>
          <p className="text-sm font-medium text-foreground">Pending Tasks</p>
          <p className="text-xs text-muted-foreground">Assignments across all children</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <BarChart3 className="mb-2 size-5 text-muted-foreground" />
          <p className="text-2xl font-extrabold text-foreground">
            {children.filter((c) => c.attendancePercentage !== null && c.attendancePercentage >= 75).length}
          </p>
          <p className="text-sm font-medium text-foreground">On Track</p>
          <p className="text-xs text-muted-foreground">Children with 75%+ attendance</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Action Center */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Today&apos;s Action Center</h2>
          {highPriorityActions.length === 0 ? (
            <div className="mt-6 flex flex-col items-center py-8 text-center">
              <CheckCircle className="mb-3 size-8 text-teal" />
              <p className="text-sm font-medium text-foreground">All caught up!</p>
              <p className="mt-1 text-xs text-muted-foreground">No urgent items for your children today.</p>
            </div>
          ) : (
            <div className="mt-4 space-y-3">
              {highPriorityActions.slice(0, 5).map((action, i) => (
                <div
                  key={`${action.type}-${action.childName}-${i}`}
                  className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3"
                >
                  <div className={`size-2 shrink-0 rounded-full ${action.type.includes("ATTENDANCE") ? "bg-orange" : "bg-red-500"}`} />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground">{action.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {action.childName} &middot; {action.detail}
                    </p>
                  </div>
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {action.date ? new Date(action.date).toLocaleDateString() : ""}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Quick Actions */}
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
          <div className="mt-4 space-y-2">
            <Link
              href="/dashboard/parent/attendance"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <Clock className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">View Attendance</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/parent/assignments"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <FileText className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">View Assignments</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
            <Link
              href="/dashboard/parent/results"
              className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <BarChart3 className="size-4 shrink-0 text-muted-foreground" />
              <span className="flex-1">View Results</span>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
            </Link>
          </div>
        </section>
      </div>

      {/* Children Cards */}
      <section>
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground">My Children</h2>
        </div>
        {children.length === 0 ? (
          <div className="mt-4 rounded-2xl border border-dashed border-border py-12 text-center">
            <Users className="mx-auto mb-3 size-8 text-muted-foreground" />
            <p className="text-sm font-medium text-foreground">No children linked yet</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Contact your school administrator to link your children.
            </p>
          </div>
        ) : (
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {children.map((child) => (
              <Link
                key={child.studentId}
                href={`/dashboard/parent/child/${child.studentId}`}
                className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm font-semibold text-foreground">{child.studentName}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      {child.admissionNumber} &middot; {child.relationshipType}
                    </p>
                  </div>
                  {child.isPrimary && (
                    <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-semibold text-primary">
                      PRIMARY
                    </span>
                  )}
                </div>

                <div className="mt-4 grid grid-cols-3 gap-3">
                  <div>
                    <p className="text-lg font-bold text-foreground">
                      {child.attendancePercentage !== null ? `${Math.round(child.attendancePercentage)}%` : "—"}
                    </p>
                    <p className="text-[10px] text-muted-foreground">Attendance</p>
                  </div>
                  <div>
                    <p className="text-lg font-bold text-foreground">{child.latestGrade || "—"}</p>
                    <p className="text-[10px] text-muted-foreground">Latest Grade</p>
                  </div>
                  <div>
                    <p className="text-lg font-bold text-foreground">
                      {child.overdueAssignments > 0 ? (
                        <span className="text-red-500">{child.overdueAssignments}</span>
                      ) : (
                        "0"
                      )}
                    </p>
                    <p className="text-[10px] text-muted-foreground">Overdue</p>
                  </div>
                </div>

                <div className="mt-4 flex items-center gap-1 text-xs font-medium text-primary">
                  View Details <ChevronRight className="size-3" />
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
