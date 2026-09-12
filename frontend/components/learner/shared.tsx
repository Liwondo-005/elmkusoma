"use client"

import { type ReactNode } from "react"
import { Loader2 } from "lucide-react"

export function LearnerHeader({ firstName, level, subtitle }: { firstName: string; level?: string; subtitle?: string }) {
  const hour = new Date().getHours()
  const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening"

  return (
    <div>
      <h1 className="text-2xl font-bold tracking-tight text-foreground">
        {greeting}, {firstName}!
      </h1>
      <p className="mt-1 text-sm text-muted-foreground">
        {subtitle || "Continue your learning journey."}
      </p>
    </div>
  )
}

export function ContinueLearningCard({
  title,
  subject,
  progress,
  onResume,
}: {
  title?: string
  subject?: string
  progress?: number
  onResume?: () => void
}) {
  if (!title) {
    return (
      <div className="rounded-2xl border border-dashed border-border bg-card p-6 text-center">
        <div className="mx-auto mb-3 flex size-12 items-center justify-center rounded-full bg-primary/10">
          <svg className="size-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <h3 className="text-sm font-semibold text-foreground">Your learning journey starts here</h3>
        <p className="mt-1 text-xs text-muted-foreground">Begin a lesson to see your progress here.</p>
        {onResume && (
          <button onClick={onResume} className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            Start Learning
          </button>
        )}
      </div>
    )
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">Continue Learning</p>
      <h3 className="mt-2 text-base font-semibold text-foreground">{title}</h3>
      {subject && <p className="mt-0.5 text-xs text-muted-foreground">{subject}</p>}
      {progress !== undefined && (
        <div className="mt-3">
          <div className="flex items-center justify-between text-xs">
            <span className="text-muted-foreground">Progress</span>
            <span className="font-semibold text-teal">{progress}%</span>
          </div>
          <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
            <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${progress}%` }} />
          </div>
        </div>
      )}
      {onResume && (
        <button onClick={onResume} className="mt-4 w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          Resume
        </button>
      )}
    </div>
  )
}

export function LearningItemCard({ title, subject, icon }: { title: string; subject?: string; icon?: ReactNode }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border p-3 transition-colors hover:bg-muted/50">
      {icon && <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">{icon}</div>}
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-foreground">{title}</p>
        {subject && <p className="truncate text-xs text-muted-foreground">{subject}</p>}
      </div>
    </div>
  )
}

export function AssignmentCard({ title, subject, dueDate, status }: { title: string; subject?: string; dueDate?: string; status?: string }) {
  const statusColors: Record<string, string> = {
    PENDING: "bg-orange/10 text-orange",
    SUBMITTED: "bg-blue-500/10 text-blue-500",
    GRADED: "bg-teal/10 text-teal",
    OVERDUE: "bg-red-500/10 text-red-500",
  }
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border p-3">
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-foreground">{title}</p>
        <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
          {subject && <span>{subject}</span>}
          {dueDate && <span>Due: {new Date(dueDate).toLocaleDateString()}</span>}
        </div>
      </div>
      {status && (
        <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusColors[status] || "bg-muted text-muted-foreground"}`}>
          {status}
        </span>
      )}
    </div>
  )
}

export function ProgressCard({ label, value, total }: { label: string; value: number; total?: number }) {
  const pct = total ? Math.round((value / total) * 100) : value
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-extrabold text-foreground">{total ? `${value}/${total}` : `${pct}%`}</p>
      {total !== undefined && (
        <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-muted">
          <div className="h-full rounded-full bg-teal" style={{ width: `${pct}%` }} />
        </div>
      )}
    </div>
  )
}

export function EmptyState({ icon, title, description, action }: { icon?: ReactNode; title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-border py-12 text-center">
      {icon && <div className="mx-auto mb-3 flex size-10 items-center justify-center text-muted-foreground">{icon}</div>}
      <p className="text-sm font-medium text-foreground">{title}</p>
      {description && <p className="mt-1 text-xs text-muted-foreground">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function LoadingState() {
  return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="size-6 animate-spin text-muted-foreground" />
    </div>
  )
}
