"use client"

import { useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { ArrowLeft, Loader2, AlertTriangle, CheckCircle, Clock } from "lucide-react"
import { parentApi, type AssignmentData, type ChildOverview } from "@/lib/parent-api"

export default function ParentAssignmentsPage() {
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const searchParams = useSearchParams()
  const childId = searchParams.get("child")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedId, setSelectedId] = useState(childId || "")
  const [assignments, setAssignments] = useState<AssignmentData | null>(null)
  const [loading, setLoading] = useState(false)
  const [tab, setTab] = useState<"pending" | "overdue" | "completed">("pending")

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (!selectedId && kids.length > 0) setSelectedId(kids[0].studentId)
    })
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setLoading(true)
    parentApi.getChildAssignments(selectedId).then(setAssignments).finally(() => setLoading(false))
  }, [selectedId])

  const items = tab === "pending" ? assignments?.pending : tab === "overdue" ? assignments?.overdue : assignments?.completed

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link href="/dashboard/parent" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
        <ArrowLeft className="size-4" /> {t("assignments.backToDashboard")}
      </Link>
      <h1 className="text-xl font-bold text-foreground">{tn("assignments")}</h1>

      {children.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {children.map((c) => (
            <button
              key={c.studentId}
              onClick={() => setSelectedId(c.studentId)}
              className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                selectedId === c.studentId ? "bg-primary text-primary-foreground" : "border border-border bg-card text-foreground hover:bg-muted"
              }`}
            >
              {c.studentName}
            </button>
          ))}
        </div>
      )}

      {assignments && (
        <div className="flex gap-1 rounded-lg border border-border bg-muted p-1">
          {(["pending", "overdue", "completed"] as const).map((tabId) => (
            <button
              key={tabId}
              onClick={() => setTab(tabId)}
              className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                tab === tabId ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {ts(tabId)} ({(assignments[tabId] || []).length})
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : items && items.length > 0 ? (
        <div className="space-y-3">
          {items.map((item) => (
            <div key={item.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-semibold text-foreground">{item.title}</p>
                  <p className="text-xs text-muted-foreground">{item.subject}</p>
                </div>
                {item.status === "OVERDUE" ? (
                  <span className="inline-flex items-center gap-1 rounded bg-red-500/10 px-2 py-0.5 text-[10px] font-semibold text-red-500">
                    <AlertTriangle className="size-3" /> {ts("overdue")}
                  </span>
                ) : item.status === "COMPLETED" ? (
                  <span className="inline-flex items-center gap-1 rounded bg-teal/10 px-2 py-0.5 text-[10px] font-semibold text-teal">
                    <CheckCircle className="size-3" /> {ts("completed")}
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 rounded bg-orange/10 px-2 py-0.5 text-[10px] font-semibold text-orange">
                    <Clock className="size-3" /> {ts("pending")}
                  </span>
                )}
              </div>
              {item.dueDate && (
                <p className="mt-2 text-xs text-muted-foreground">
                  {t("assignments.dueLabel", { date: new Date(item.dueDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" }) })}
                </p>
              )}
              {item.obtainedMarks != null && (
                <p className="mt-1 text-xs font-medium text-foreground">
                  {t("assignments.scoreLabel", { obtained: item.obtainedMarks, total: item.totalMarks })}
                </p>
              )}
            </div>
          ))}
        </div>
      ) : items && items.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <CheckCircle className="mx-auto mb-3 size-8 text-teal" />
          <p className="text-sm font-medium text-foreground">
            {tab === "pending" ? t("assignments.emptyPending") : tab === "overdue" ? t("assignments.emptyOverdue") : t("assignments.emptyCompleted")}
          </p>
        </div>
      ) : (
        <p className="py-12 text-center text-sm text-muted-foreground">{t("assignments.selectChild")}</p>
      )}
    </div>
  )
}
