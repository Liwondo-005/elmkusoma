"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { PenTool, Loader2, CheckCircle, AlertTriangle, Clock, TrendingUp, TrendingDown, Minus } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type ChildOverview, type ParentAssessments, type AssessmentItem } from "@/lib/parent-api"

export default function ParentAssessmentsPage() {
  const { user } = useAuth()
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [assessments, setAssessments] = useState<ParentAssessments | null>(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<"completed" | "upcoming">("completed")

  useEffect(() => {
    parentApi.getChildren().then((kids) => {
      setChildren(kids)
      if (kids.length > 0) {
        const primary = kids.find((c) => c.isPrimary) || kids[0]
        setSelectedChildId(primary.studentId)
      }
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!selectedChildId) return
    setAssessments(null)
    parentApi.getChildAssessments(selectedChildId).then(setAssessments).catch(() => setAssessments(null))
  }, [selectedChildId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  const completed = assessments?.completed || []
  const upcoming = assessments?.upcoming || []
  const currentList = tab === "completed" ? completed : upcoming

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{tn("assessments")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {assessments ? `${assessments.studentName} · ${assessments.className}` : t("assessments.defaultSubtitle")}
        </p>
      </div>

      {children.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {children.map((child) => (
            <button key={child.studentId} onClick={() => setSelectedChildId(child.studentId)}
              className={`shrink-0 rounded-xl border px-4 py-2 text-sm font-medium transition-colors ${selectedChildId === child.studentId ? "border-primary bg-primary text-primary-foreground" : "border-border bg-card text-foreground hover:bg-muted"}`}>
              {child.studentName}
            </button>
          ))}
        </div>
      )}

      <div className="flex gap-1 rounded-lg border border-border bg-muted p-1">
        <button onClick={() => setTab("completed")}
          className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors ${tab === "completed" ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"}`}>
          {ts("completed")} ({completed.length})
        </button>
        <button onClick={() => setTab("upcoming")}
          className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors ${tab === "upcoming" ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"}`}>
          {t("assessments.upcoming")} ({upcoming.length})
        </button>
      </div>

      {currentList.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <PenTool className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{tab === "completed" ? t("assessments.emptyCompletedTitle") : t("assessments.emptyUpcomingTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {tab === "completed" ? t("assessments.emptyCompletedDesc") : t("assessments.emptyUpcomingDesc")}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {currentList.map((a) => (
            <AssessmentCard key={a.id} item={a} showScore={tab === "completed"} />
          ))}
        </div>
      )}
    </div>
  )
}

function AssessmentCard({ item, showScore }: { item: AssessmentItem; showScore: boolean }) {
  const t = useTranslations("parent")
  const scoreColor = item.isPassed === true ? "text-green-600" : item.isPassed === false ? "text-red-500" : "text-muted-foreground"

  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-foreground">{item.title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{item.subject}</p>
        </div>
        <div className="flex items-center gap-2">
          {item.status && (
            <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${
              item.status === "GRADED" ? "bg-green-100 text-green-700" :
              item.status === "PENDING" ? "bg-yellow-100 text-yellow-700" :
              item.status === "SCHEDULED" ? "bg-blue-100 text-blue-700" :
              "bg-muted text-muted-foreground"
            }`}>
              {item.status}
            </span>
          )}
          {showScore && item.isPassed !== null && (
            <span className="shrink-0">
              {item.isPassed ? <CheckCircle className="size-5 text-green-600" /> : <AlertTriangle className="size-5 text-red-500" />}
            </span>
          )}
        </div>
      </div>

      <div className="mt-3 grid grid-cols-3 gap-3">
        {item.totalMarks != null && (
          <div className="rounded-lg bg-muted/50 p-2.5 text-center">
            <p className="text-xs text-muted-foreground">{t("assessments.totalLabel")}</p>
            <p className="text-sm font-bold text-foreground">{item.totalMarks}</p>
          </div>
        )}
        {item.score != null && (
          <div className="rounded-lg bg-muted/50 p-2.5 text-center">
            <p className="text-xs text-muted-foreground">{t("assessments.scoreLabel")}</p>
            <p className={`text-sm font-bold ${scoreColor}`}>{item.score}</p>
          </div>
        )}
        {item.percentage != null && (
          <div className="rounded-lg bg-muted/50 p-2.5 text-center">
            <p className="text-xs text-muted-foreground">%</p>
            <p className={`text-sm font-bold ${scoreColor}`}>{Math.round(item.percentage)}%</p>
          </div>
        )}
      </div>

      {item.remarks && <p className="mt-3 text-xs text-muted-foreground italic">&ldquo;{item.remarks}&rdquo;</p>}

      {item.createdAt && (
        <p className="mt-2 text-[10px] text-muted-foreground">
          {new Date(item.createdAt).toLocaleDateString("en-GB", { month: "short", day: "numeric", year: "numeric" })}
        </p>
      )}
    </div>
  )
}
