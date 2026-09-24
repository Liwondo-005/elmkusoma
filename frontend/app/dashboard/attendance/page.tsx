"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { dashboardApi, type AttendanceSummary } from "@/lib/api"
import { type LearningLevel } from "@/lib/learner-config"
import { BarChart3, CheckCircle, XCircle, Clock, AlertTriangle, Calendar, Star, TrendingUp } from "lucide-react"

export default function AttendancePage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [summary, setSummary] = useState<AttendanceSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await dashboardApi.getAttendance().catch(() => null)
      setSummary(data)
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  const stats = [
    { label: ts("present"), value: summary?.present ?? 0, icon: CheckCircle, color: "bg-green-500/10 text-green-600", ringColor: "text-green-500" },
    { label: ts("absent"), value: summary?.absent ?? 0, icon: XCircle, color: "bg-red-500/10 text-red-600", ringColor: "text-red-500" },
    { label: ts("late"), value: summary?.late ?? 0, icon: Clock, color: "bg-yellow-500/10 text-yellow-600", ringColor: "text-yellow-500" },
    { label: ts("excused"), value: summary?.excused ?? 0, icon: AlertTriangle, color: "bg-blue-500/10 text-blue-600", ringColor: "text-blue-500" },
  ]

  function getStatusStyle(status: string) {
    switch (status) {
      case "PRESENT": return "bg-green-100 text-green-700"
      case "ABSENT": return "bg-red-100 text-red-700"
      case "LATE": return "bg-yellow-100 text-yellow-700"
      case "EXCUSED": return "bg-blue-100 text-blue-700"
      default: return "bg-gray-100 text-gray-700"
    }
  }

  function getStatusEmoji(status: string) {
    switch (status) {
      case "PRESENT": return "✓"
      case "ABSENT": return "✗"
      case "LATE": return "⏰"
      case "EXCUSED": return "📋"
      return "?"
    }
  }

  const attendanceRate = summary?.attendanceRate ?? 0

  if (isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        {/* Header */}
        <div className="rounded-2xl border border-border bg-gradient-to-br from-green-50 via-card to-teal/5 p-6 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-green-500/10">
              <Calendar className="size-6 text-green-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("attend.title")}</h1>
              <p className="text-sm text-muted-foreground">{t("attend.subtitle")}</p>
            </div>
          </div>
        </div>

        {/* Attendance Rate Visual */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="flex items-center gap-6">
            <div className="relative size-24">
              <svg className="size-24 -rotate-90" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="none" stroke="currentColor" strokeWidth="8" className="text-muted" />
                <circle
                  cx="50" cy="50" r="40" fill="none" strokeWidth="8"
                  className={attendanceRate >= 90 ? "text-green-500" : attendanceRate >= 75 ? "text-amber-500" : "text-red-500"}
                  strokeDasharray={`${2 * Math.PI * 40}`}
                  strokeDashoffset={`${2 * Math.PI * 40 * (1 - attendanceRate / 100)}`}
                  strokeLinecap="round"
                />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-xl font-bold text-foreground">{attendanceRate}%</span>
              </div>
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-foreground">
                {attendanceRate >= 90 ? t("attend.excellent") : attendanceRate >= 75 ? t("attend.good") : t("attend.improve")}
              </h3>
              <p className="text-sm text-muted-foreground mt-1">
                {attendanceRate >= 90
                  ? t("attend.excellentDesc")
                  : attendanceRate >= 75
                    ? t("attend.goodDesc")
                    : t("attend.improveDesc")}
              </p>
              <div className="mt-2 flex items-center gap-1.5">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Star
                    key={i}
                    className={`size-5 ${
                      i < Math.floor(attendanceRate / 20)
                        ? "fill-amber-400 text-amber-400"
                        : "text-muted"
                    }`}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          {stats.map((s) => (
            <div key={s.label} className="rounded-2xl border border-border bg-card p-4 shadow-xs text-center">
              <div className={`mx-auto flex size-10 items-center justify-center rounded-xl ${s.color}`}>
                <s.icon className="size-5" />
              </div>
              <p className="mt-2 text-2xl font-extrabold text-foreground">{s.value}</p>
              <p className="text-xs text-muted-foreground">{s.label}</p>
            </div>
          ))}
        </div>

        {/* Attendance Records */}
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="text-lg font-semibold text-foreground mb-4">{t("attend.schoolDays")}</h2>
          {!summary?.records || summary.records.length === 0 ? (
            <div className="flex flex-col items-center py-8 text-center">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-muted">
                <Calendar className="size-6 text-muted-foreground" />
              </div>
              <p className="mt-3 text-sm font-medium text-foreground">{t("attend.emptyTitle")}</p>
              <p className="text-xs text-muted-foreground">{t("attend.emptyDesc")}</p>
            </div>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {summary.records.map((record) => (
                <div
                  key={record.id}
                  className={`flex items-center gap-3 rounded-xl border p-3 ${
                    record.status === "PRESENT"
                      ? "border-green-200 bg-green-50/50"
                      : record.status === "ABSENT"
                        ? "border-red-200 bg-red-50/50"
                        : record.status === "LATE"
                          ? "border-yellow-200 bg-yellow-50/50"
                          : "border-border bg-muted/50"
                  }`}
                >
                  <div className={`flex size-8 items-center justify-center rounded-full ${
                    record.status === "PRESENT" ? "bg-green-100" :
                    record.status === "ABSENT" ? "bg-red-100" :
                    record.status === "LATE" ? "bg-yellow-100" : "bg-gray-100"
                  }`}>
                    <span className="text-sm">
                      {record.status === "PRESENT" ? "✓" : record.status === "ABSENT" ? "✗" : record.status === "LATE" ? "⏰" : "📋"}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground">
                      {record.date ? new Date(record.date).toLocaleDateString("en-US", {
                        weekday: "short",
                        month: "short",
                        day: "numeric",
                      }) : "—"}
                    </p>
                    <p className="text-xs text-muted-foreground">{record.status}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    )
  }

  /* Non-Primary: Original attendance page */
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("attend.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("attend.monthDesc")}</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs sm:col-span-2 lg:col-span-1">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-teal/10">
              <BarChart3 className="size-5 text-teal" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{attendanceRate}%</p>
              <p className="text-xs text-muted-foreground">{t("attend.rateLabel")}</p>
            </div>
          </div>
        </div>
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className={`flex size-8 items-center justify-center rounded-lg ${s.color}`}>
              <s.icon className="size-4" />
            </div>
            <p className="mt-2 text-xl font-bold text-foreground">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("attend.recordsTitle")}</h2>
        {!summary?.records || summary.records.length === 0 ? (
          <div className="py-12 text-center">
            <BarChart3 className="mx-auto size-12 text-muted-foreground/50" />
            <h3 className="mt-4 text-lg font-semibold text-foreground">{t("attend.noRecords")}</h3>
            <p className="mt-2 text-sm text-muted-foreground">{t("attend.noRecordsDesc")}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("attend.colDate")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("attend.colStatus")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("attend.colIn")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("attend.colOut")}</th>
                  <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t("attend.colRemarks")}</th>
                </tr>
              </thead>
              <tbody>
                {summary.records.map((record) => (
                  <tr key={record.id} className="border-b border-border last:border-0">
                    <td className="px-4 py-3 font-medium text-foreground">
                      {record.date ? new Date(record.date).toLocaleDateString("en-US", {
                        weekday: "short",
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      }) : "—"}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${getStatusStyle(record.status)}`}>
                        {record.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{record.checkInTime || "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground">{record.checkOutTime || "—"}</td>
                    <td className="px-4 py-3 text-muted-foreground">{record.remarks || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
