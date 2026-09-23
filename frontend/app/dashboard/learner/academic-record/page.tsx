"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { AcademicRecord } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { ScrollText, Award, TrendingUp, Calendar, AlertCircle } from "lucide-react"

export default function AcademicRecordPage() {
  const t = useTranslations("highered")
  const tc = useTranslations("common")
  const { user, loading: authLoading } = useAuth()
  const [record, setRecord] = useState<AcademicRecord | null>(null)
  const [history, setHistory] = useState<AcademicRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const [recordRes, historyRes] = await Promise.allSettled([
        collegeApi.getAcademicRecord(studentId),
        collegeApi.getAcademicRecordHistory(studentId),
      ])
      if (recordRes.status === "fulfilled") setRecord((recordRes.value.data as AcademicRecord) || null)
      if (historyRes.status === "fulfilled") setHistory((historyRes.value.data as AcademicRecord[]) || [])
    } catch {
      setError(tc("error.generic"))
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>

  if (error) {
    return (
      <div role="main" className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("subtitle.academicRecord")} />
        <div className="rounded-2xl border border-border bg-card p-4 text-sm text-red-600 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadData() }} aria-label={tc("retry")} className="ml-auto text-xs underline">{tc("retry")}</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"

  return (
    <div role="main" className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("subtitle.academicRecord")} />

      {record ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
                  <TrendingUp className="size-5 text-primary" />
                </div>
                <div>
                  <p className="text-xs font-medium text-muted-foreground">{t("gpa")}</p>
                  <p className="text-2xl font-extrabold text-foreground">{record.cumulativeGpa?.toFixed(2) ?? "N/A"}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
                  <Award className="size-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="text-xs font-medium text-muted-foreground">{t("academicStanding")}</p>
                  <p className="text-lg font-extrabold text-foreground">{record.academicStanding || "N/A"}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
                  <ScrollText className="size-5 text-emerald-600 dark:text-emerald-400" />
                </div>
                <div>
                  <p className="text-xs font-medium text-muted-foreground">{t("creditHours")}</p>
                  <p className="text-2xl font-extrabold text-foreground">{record.earnedCreditHours ?? 0} / {record.totalCreditHours ?? 0}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
                  <Calendar className="size-5 text-amber-600 dark:text-amber-400" />
                </div>
                <div>
                  <p className="text-xs font-medium text-muted-foreground">{t("courses")}</p>
                  <p className="text-2xl font-extrabold text-foreground">{record.completedCourses ?? 0} / {record.totalCourses ?? 0}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h3 className="text-sm font-semibold text-foreground">{t("academicRecord")}</h3>
              <div className="mt-3 space-y-2.5 text-sm">
                <div className="flex justify-between"><span className="text-muted-foreground">{t("academicYear")}</span><span className="font-medium text-foreground">{record.academicYear || "—"}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("semester")}</span><span className="font-medium text-foreground">{record.semester || "—"}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("gpa")}</span><span className="font-medium text-foreground">{record.semesterGpa?.toFixed(2) ?? "—"}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("gpa")}</span><span className="font-medium text-foreground">{record.cumulativeGpa?.toFixed(2) ?? "—"}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("rank")}</span><span className="font-medium text-foreground">{record.classRank ? `${record.classRank} / ${record.totalStudentsInClass}` : "—"}</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("filters.pending")}</span><span className="font-medium text-foreground">{record.failedCourses ?? 0}</span></div>
              </div>
            </div>

            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h3 className="text-sm font-semibold text-foreground">{t("creditHours")}</h3>
              <div className="mt-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">{t("stats.completed")} / {t("stats.total")}</span>
                  <span className="font-semibold text-foreground">{record.earnedCreditHours ?? 0} / {record.totalCreditHours ?? 0}</span>
                </div>
                <div className="mt-2 h-3 w-full overflow-hidden rounded-full bg-muted" role="progressbar" aria-valuenow={record.totalCreditHours ? Math.round(((record.earnedCreditHours ?? 0) / record.totalCreditHours) * 100) : 0} aria-valuemin={0} aria-valuemax={100}>
                  <div
                    className="h-full rounded-full bg-primary transition-all"
                    style={{ width: `${record.totalCreditHours ? ((record.earnedCreditHours ?? 0) / record.totalCreditHours) * 100 : 0}%` }}
                  />
                </div>
                <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                  <div className="flex justify-between"><span className="text-muted-foreground">{t("stats.completed")}</span><span className="font-medium">{record.completedCourses ?? 0}</span></div>
                  <div className="flex justify-between"><span className="text-muted-foreground">{t("filters.pending")}</span><span className="font-medium">{record.failedCourses ?? 0}</span></div>
                </div>
              </div>
            </div>
          </div>
        </>
      ) : (
        <EmptyState
          icon={<ScrollText className="size-8" />}
          title={t("empty.noModules")}
          description={t("empty.noModules")}
        />
      )}

      {history.length > 0 && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h3 className="text-sm font-semibold text-foreground mb-3">{t("gpa")}</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border">
                <tr>
                  <th className="pb-2 font-medium text-muted-foreground">{t("academicYear")}</th>
                  <th className="pb-2 font-medium text-muted-foreground">{t("semester")}</th>
                  <th className="pb-2 font-medium text-muted-foreground">{t("semester")}</th>
                  <th className="pb-2 font-medium text-muted-foreground">{t("gpa")}</th>
                  <th className="pb-2 font-medium text-muted-foreground">{t("academicStanding")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {history.map((h) => (
                  <tr key={h.id}>
                    <td className="py-2 text-foreground">{h.academicYear || "—"}</td>
                    <td className="py-2 text-muted-foreground">{h.semester || "—"}</td>
                    <td className="py-2 font-medium text-foreground">{h.semesterGpa?.toFixed(2) ?? "—"}</td>
                    <td className="py-2 font-medium text-foreground">{h.cumulativeGpa?.toFixed(2) ?? "—"}</td>
                    <td className="py-2 text-muted-foreground">{h.academicStanding || "—"}</td>
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
