"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Users, AlertTriangle, CheckCircle, Clock, FileText, BarChart3, ChevronRight, Loader2, Star, TrendingUp, Calendar, BookOpen, Video, Trophy, Target, AlertCircle, Info, PenTool, Activity } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type FamilyOverview, type ChildOverview, type ParentIntelligence, type AttentionItem, type PositiveSignal, type UpcomingItem } from "@/lib/parent-api"

const PRIORITY_COLORS: Record<string, string> = {
  CRITICAL: "bg-red-500",
  IMPORTANT: "bg-orange",
  NORMAL: "bg-blue-500",
  INFORMATIONAL: "bg-muted-foreground",
}

const TYPE_ICONS: Record<string, typeof AlertTriangle> = {
  ASSIGNMENT_OVERDUE: AlertTriangle,
  ASSIGNMENT_DUE_SOON: Clock,
  ATTENDANCE_PATTERN: AlertCircle,
  ASSESSMENT_DECLINE: TrendingUp,
  ASSIGNMENTS_SUBMITTED: CheckCircle,
  LESSONS_COMPLETED: BookOpen,
  LIVE_CLASSES_ATTENDED: Video,
  ACHIEVEMENT: Trophy,
  MILESTONE: Star,
  PERFORMANCE_IMPROVED: TrendingUp,
  GOALS_COMPLETED: Target,
}

export default function ParentDashboardPage() {
  const { user } = useAuth()
  const firstName = user?.name?.split(" ")[0] || "Parent"
  const [overview, setOverview] = useState<FamilyOverview | null>(null)
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [intelligence, setIntelligence] = useState<ParentIntelligence | null>(null)
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
        if (kids.length > 0) {
          const primary = kids.find((c) => c.isPrimary) || kids[0]
          setSelectedChildId(primary.studentId)
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "Failed to load dashboard"
        setError(msg)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  useEffect(() => {
    if (!selectedChildId) return
    parentApi.getChildIntelligence(selectedChildId)
      .then(setIntelligence)
      .catch(() => setIntelligence(null))
  }, [selectedChildId])

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

  const selectedChild = children.find((c) => c.studentId === selectedChildId)
  const brief = intelligence?.weeklyBrief

  const hasPrimaryChild = children.some((c) => c.isPrimary)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Good {getGreeting()}, {firstName}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here&apos;s how your children are doing today.
        </p>
      </div>

      {hasPrimaryChild && (
        <Link
          href="/dashboard/parent/primary-progress"
          className="flex items-center gap-4 rounded-2xl border-2 border-primary/30 bg-primary/5 p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:border-primary/50 hover:shadow-md"
        >
          <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <BarChart3 className="size-6" />
          </div>
          <div className="flex-1">
            <p className="text-base font-bold text-foreground">Primary School View</p>
            <p className="text-sm text-muted-foreground">
              See your child&apos;s progress with a kid-friendly dashboard, subject performance, and teacher info.
            </p>
          </div>
          <ChevronRight className="size-5 shrink-0 text-primary" />
        </Link>
      )}

      {/* Child Selector */}
      {children.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {children.map((child) => (
            <button
              key={child.studentId}
              onClick={() => setSelectedChildId(child.studentId)}
              className={`shrink-0 rounded-xl border px-4 py-2.5 text-sm font-medium transition-colors ${
                selectedChildId === child.studentId
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-foreground hover:bg-muted"
              }`}
            >
              {child.studentName}
              <span className="ml-2 text-xs opacity-70">{child.className}</span>
            </button>
          ))}
        </div>
      )}

      {/* Child Overview Cards */}
      {selectedChild && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <p className="text-xs font-medium text-muted-foreground">Attendance</p>
            <p className="mt-1 text-2xl font-extrabold text-foreground">
              {selectedChild.attendancePercentage !== null ? `${Math.round(selectedChild.attendancePercentage)}%` : "—"}
            </p>
            <p className="text-xs text-muted-foreground">{selectedChild.className}</p>
          </div>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <p className="text-xs font-medium text-muted-foreground">Learning Progress</p>
            <p className="mt-1 text-2xl font-extrabold text-foreground">
              {selectedChild.learningProgress !== null ? `${Math.round(selectedChild.learningProgress)}%` : "—"}
            </p>
            <p className="text-xs text-muted-foreground">Overall completion</p>
          </div>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <p className="text-xs font-medium text-muted-foreground">Pending Tasks</p>
            <p className="mt-1 text-2xl font-extrabold text-foreground">{selectedChild.pendingAssignments}</p>
            <p className="text-xs text-muted-foreground">Assignments to complete</p>
          </div>
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <p className="text-xs font-medium text-muted-foreground">Latest Grade</p>
            <p className="mt-1 text-2xl font-extrabold text-foreground">{selectedChild.latestGrade || "—"}</p>
            <p className="text-xs text-muted-foreground">{selectedChild.latestAverage != null ? `Avg: ${Math.round(selectedChild.latestAverage)}%` : "No data yet"}</p>
          </div>
        </div>
      )}

      {/* Needs Your Attention */}
      {intelligence && intelligence.needsAttention.length > 0 && (
        <section className="rounded-2xl border border-red-200 bg-red-50 p-5 shadow-xs dark:border-red-900 dark:bg-red-950/30">
          <h2 className="text-base font-semibold text-foreground">Needs Your Attention</h2>
          <div className="mt-3 space-y-2">
            {intelligence.needsAttention.slice(0, 5).map((item) => (
              <AttentionCard key={item.id} item={item} />
            ))}
          </div>
        </section>
      )}

      {/* Doing Well */}
      {intelligence && intelligence.doingWell.length > 0 && (
        <section className="rounded-2xl border border-green-200 bg-green-50 p-5 shadow-xs dark:border-green-900 dark:bg-green-950/30">
          <h2 className="text-base font-semibold text-foreground">Doing Well</h2>
          <div className="mt-3 space-y-2">
            {intelligence.doingWell.slice(0, 5).map((item) => (
              <PositiveCard key={item.id} item={item} />
            ))}
          </div>
        </section>
      )}

      {/* Weekly Brief */}
      {brief && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">This Week</h2>
          <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <StatCard label="Lessons Completed" value={brief.lessonsCompleted} icon={BookOpen} />
            <StatCard label="Assignments Done" value={brief.assignmentsCompleted} icon={FileText} />
            <StatCard label="Live Classes" value={brief.liveClassesAttended} icon={Video} />
            <StatCard label="Assessments" value={brief.assessmentsCompleted} icon={BarChart3} />
          </div>
          {brief.highlights.length > 0 && (
            <div className="mt-4 space-y-1">
              {brief.highlights.map((h, i) => (
                <div key={i} className="flex items-center gap-2 text-sm text-foreground">
                  <CheckCircle className="size-4 shrink-0 text-green-600" />
                  {h}
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* Upcoming */}
      {intelligence && intelligence.upcoming.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">Upcoming</h2>
          <div className="mt-3 space-y-2">
            {intelligence.upcoming.slice(0, 6).map((item) => (
              <UpcomingCard key={item.id} item={item} />
            ))}
          </div>
        </section>
      )}

      {/* Quick Actions */}
      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs lg:col-span-2">
          <h2 className="text-base font-semibold text-foreground">Quick Actions</h2>
          <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
            <QuickAction href="/dashboard/parent/learning" icon={BookOpen} label="Learning" />
            <QuickAction href="/dashboard/parent/reports" icon={BarChart3} label="Progress" />
            <QuickAction href="/dashboard/parent/assessments" icon={PenTool} label="Assessments" />
            <QuickAction href="/dashboard/parent/activity" icon={Activity} label="Activity" />
            <QuickAction href="/dashboard/parent/teachers" icon={Users} label="Teachers" />
            <QuickAction href="/dashboard/parent/calendar" icon={Calendar} label="Calendar" />
          </div>
        </section>

        {/* Recommendations */}
        {intelligence && intelligence.recommendations.length > 0 && (
          <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <h2 className="text-base font-semibold text-foreground">How You Can Help</h2>
            <div className="mt-3 space-y-3">
              {intelligence.recommendations.slice(0, 3).map((rec) => (
                <div key={rec.id} className="rounded-xl border border-border bg-muted/30 p-3">
                  <p className="text-sm font-medium text-foreground">{rec.title}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{rec.description}</p>
                </div>
              ))}
            </div>
          </section>
        )}
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
                      {child.className} &middot; {child.relationshipType}
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
                    <p className="text-[10px] text-muted-foreground">Grade</p>
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

function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 12) return "morning"
  if (hour < 17) return "afternoon"
  return "evening"
}

function StatCard({ label, value, icon: Icon }: { label: string; value: number; icon: typeof BookOpen }) {
  return (
    <div className="rounded-xl border border-border bg-muted/30 p-3">
      <Icon className="size-4 text-muted-foreground" />
      <p className="mt-1 text-xl font-bold text-foreground">{value}</p>
      <p className="text-xs text-muted-foreground">{label}</p>
    </div>
  )
}

function QuickAction({ href, icon: Icon, label }: { href: string; icon: typeof BookOpen; label: string }) {
  return (
    <Link
      href={href}
      className="flex items-center gap-3 rounded-xl border border-border p-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
    >
      <Icon className="size-4 shrink-0 text-muted-foreground" />
      <span className="flex-1">{label}</span>
      <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
    </Link>
  )
}

function AttentionCard({ item }: { item: AttentionItem }) {
  const Icon = TYPE_ICONS[item.type] || AlertTriangle
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-card p-3">
      <div className={`size-2 shrink-0 rounded-full ${PRIORITY_COLORS[item.priority] || "bg-muted-foreground"}`} />
      <Icon className="size-4 shrink-0 text-muted-foreground" />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{item.title}</p>
        <p className="text-xs text-muted-foreground">{item.description}</p>
      </div>
      {item.actionLabel && (
        <Link href={item.actionUrl || "#"} className="shrink-0 text-xs font-medium text-primary hover:underline">
          {item.actionLabel}
        </Link>
      )}
    </div>
  )
}

function PositiveCard({ item }: { item: PositiveSignal }) {
  const Icon = TYPE_ICONS[item.type] || CheckCircle
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-card p-3">
      <CheckCircle className="size-4 shrink-0 text-green-600" />
      <Icon className="size-4 shrink-0 text-muted-foreground" />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{item.title}</p>
        {item.description && <p className="text-xs text-muted-foreground">{item.description}</p>}
      </div>
    </div>
  )
}

function UpcomingCard({ item }: { item: UpcomingItem }) {
  const typeColors: Record<string, string> = {
    LIVE_CLASS: "bg-blue-100 text-blue-700",
    ASSIGNMENT: "bg-orange-100 text-orange-700",
    ASSESSMENT: "bg-purple-100 text-purple-700",
    SCHOOL_EVENT: "bg-teal-100 text-teal-700",
  }
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
      <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${typeColors[item.type] || "bg-muted text-muted-foreground"}`}>
        {item.type.replace("_", " ")}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{item.title}</p>
        <p className="text-xs text-muted-foreground">
          {item.timestamp ? new Date(item.timestamp).toLocaleDateString("en-GB", { weekday: "short", month: "short", day: "numeric" }) : ""}
        </p>
      </div>
    </div>
  )
}
