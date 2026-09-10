"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Users, ChevronRight, Loader2 } from "lucide-react"
import { parentApi, type ChildOverview } from "@/lib/parent-api"

export default function ParentChildrenPage() {
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getChildren().then(setChildren).finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-xl font-bold text-foreground">My Children</h1>
        <p className="mt-1 text-sm text-muted-foreground">All children linked to your account.</p>
      </div>

      {children.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <Users className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">No children linked yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Contact your school administrator to link your children.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {children.map((child) => (
            <Link
              key={child.studentId}
              href={`/dashboard/parent/child/${child.studentId}`}
              className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-xs transition-all hover:-translate-y-0.5 hover:shadow-md"
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">
                {child.studentName.charAt(0)}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="truncate text-sm font-semibold text-foreground">{child.studentName}</p>
                  {child.isPrimary && (
                    <span className="shrink-0 rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-semibold text-primary">PRIMARY</span>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  {child.admissionNumber} &middot; {child.className} &middot; {child.relationshipType}
                </p>
                <div className="mt-2 flex gap-4">
                  <span className="text-xs text-muted-foreground">
                    Attendance: <span className="font-semibold text-foreground">
                      {child.attendancePercentage != null ? `${Math.round(child.attendancePercentage)}%` : "—"}
                    </span>
                  </span>
                  <span className="text-xs text-muted-foreground">
                    Grade: <span className="font-semibold text-foreground">{child.latestGrade || "—"}</span>
                  </span>
                </div>
              </div>
              <ChevronRight className="size-5 shrink-0 text-muted-foreground" />
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
