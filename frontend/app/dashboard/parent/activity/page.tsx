"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Activity, Loader2, BookOpen, Video, FileText, CheckCircle, Award, Clock } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { parentApi, type ChildOverview, type ParentActivity, type ActivityItem } from "@/lib/parent-api"

const typeIcons: Record<string, typeof Activity> = {
  LESSON_VIEWED: BookOpen,
  LESSON_COMPLETED: CheckCircle,
  ASSIGNMENT_SUBMITTED: FileText,
  LIVE_CLASS_JOINED: Video,
  ASSESSMENT_COMPLETED: Award,
  GOAL_COMPLETED: Award,
  RESOURCE_ACCESSED: BookOpen,
}

const typeColors: Record<string, string> = {
  LESSON_VIEWED: "bg-blue-100 text-blue-700",
  LESSON_COMPLETED: "bg-green-100 text-green-700",
  ASSIGNMENT_SUBMITTED: "bg-orange-100 text-orange-700",
  LIVE_CLASS_JOINED: "bg-purple-100 text-purple-700",
  ASSESSMENT_COMPLETED: "bg-teal-100 text-teal-700",
  GOAL_COMPLETED: "bg-yellow-100 text-yellow-700",
  RESOURCE_ACCESSED: "bg-indigo-100 text-indigo-700",
}

export default function ParentActivityPage() {
  const { user } = useAuth()
  const t = useTranslations("parent")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [activity, setActivity] = useState<ParentActivity | null>(null)
  const [loading, setLoading] = useState(true)

  function timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime()
    const mins = Math.floor(diff / 60000)
    if (mins < 1) return t("activity.timeJustNow")
    if (mins < 60) return t("activity.timeMinutesAgo", { count: mins })
    const hrs = Math.floor(mins / 60)
    if (hrs < 24) return t("activity.timeHoursAgo", { count: hrs })
    const days = Math.floor(hrs / 24)
    if (days === 1) return t("activity.timeYesterday")
    if (days < 7) return t("activity.timeDaysAgo", { count: days })
    return new Date(dateStr).toLocaleDateString("en-GB", { month: "short", day: "numeric" })
  }

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
    setActivity(null)
    parentApi.getChildActivity(selectedChildId).then(setActivity).catch(() => setActivity(null))
  }, [selectedChildId])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  const activities = activity?.activities || []

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("activity.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("activity.subtitle")}</p>
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

      {activities.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <Activity className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("activity.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("activity.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-2">
          {activities.map((item) => {
            const Icon = typeIcons[item.type] || Activity
            const color = typeColors[item.type] || "bg-muted text-muted-foreground"
            return (
              <div key={item.id} className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs">
                <div className={`flex size-9 shrink-0 items-center justify-center rounded-xl ${color}`}>
                  <Icon className="size-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{item.title}</p>
                  {item.description && <p className="text-xs text-muted-foreground">{item.description}</p>}
                  <div className="mt-1 flex items-center gap-2 text-[10px] text-muted-foreground">
                    <span>{item.type?.replace(/_/g, " ")}</span>
                    {item.status && (
                      <>
                        <span>&middot;</span>
                        <span>{item.status}</span>
                      </>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
                  <Clock className="size-3" />
                  {item.timestamp ? timeAgo(item.timestamp) : ""}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
