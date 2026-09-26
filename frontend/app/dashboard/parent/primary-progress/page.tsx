"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import {
  BookOpen,
  Loader2,
  ChevronRight,
  BarChart3,
  ClipboardCheck,
  Clock,
  Trophy,
  Star,
  TrendingUp,
  TrendingDown,
  Award,
  Calendar,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Users,
} from "lucide-react"
import { useAuth } from "@/lib/auth"
import {
  parentApi,
  type ChildOverview,
  type SubjectPerformanceItem,
  type AttendanceDay,
  type ActivityItem,
  type TeacherItem,
  type AchievementItem,
} from "@/lib/parent-api"
import { TeacherInfoCard } from "@/components/primary/teacher-info-card"
import type { TeacherInfo } from "@/lib/api"

const STATUS_CONFIG: Record<string, { bg: string; text: string; label: string }> = {
  PRESENT: { bg: "bg-green-100", text: "text-green-700", label: "P" },
  ABSENT: { bg: "bg-red-100", text: "text-red-700", label: "A" },
  LATE: { bg: "bg-yellow-100", text: "text-yellow-700", label: "L" },
  EXCUSED: { bg: "bg-blue-100", text: "text-blue-700", label: "E" },
}

const TREND_ICONS: Record<string, typeof TrendingUp> = {
  UP: TrendingUp,
  DOWN: TrendingDown,
  STABLE: BarChart3,
}

const ACTIVITY_ICONS: Record<string, typeof CheckCircle2> = {
  LESSON_COMPLETED: BookOpen,
  ASSIGNMENT_SUBMITTED: ClipboardCheck,
  ASSESSMENT_COMPLETED: Award,
  BADGE_EARNED: Trophy,
  LIVE_CLASS_ATTENDED: Users,
  ATTENDANCE: Calendar,
}

function AttendanceRing({ percentage }: { percentage: number | null }) {
  if (percentage === null) {
    return (
      <div className="relative flex size-20 items-center justify-center">
        <svg className="size-20 -rotate-90" viewBox="0 0 36 36">
          <circle cx="18" cy="18" r="15.5" fill="none" stroke="currentColor" strokeWidth="3" className="text-muted/30" />
        </svg>
        <span className="absolute text-lg font-bold text-muted-foreground">--</span>
      </div>
    )
  }
  const rounded = Math.round(percentage)
  const color = rounded >= 80 ? "text-green-500" : rounded >= 60 ? "text-yellow-500" : "text-red-500"
  const dashOffset = 15.5 * 2 * Math.PI * (1 - rounded / 100)
  return (
    <div className="relative flex size-20 items-center justify-center">
      <svg className="size-20 -rotate-90" viewBox="0 0 36 36">
        <circle cx="18" cy="18" r="15.5" fill="none" stroke="currentColor" strokeWidth="3" className="text-muted/30" />
        <circle
          cx="18"
          cy="18"
          r="15.5"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeDasharray={`${2 * Math.PI * 15.5}`}
          strokeDashoffset={dashOffset}
          strokeLinecap="round"
          className={color}
        />
      </svg>
      <span className={`absolute text-lg font-bold ${color}`}>{rounded}%</span>
    </div>
  )
}

function SubjectBar({ item }: { item: SubjectPerformanceItem }) {
  const t = useTranslations("parent")
  const trend = item.trend ? TREND_ICONS[item.trend] : null
  const barWidth = item.averageMark !== null ? Math.min(item.averageMark, 100) : 0
  const barColor =
    barWidth >= 75 ? "bg-green-500" : barWidth >= 50 ? "bg-yellow-500" : "bg-red-500"

  return (
    <div className="rounded-xl border border-border bg-muted/30 p-4">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-foreground">{item.subjectName}</p>
        <div className="flex items-center gap-2">
          {item.grade && (
            <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">{item.grade}</span>
          )}
          {trend && (
            (() => { const TrendIcon = trend; return <TrendIcon className={`size-4 ${item.trend === "DOWN" ? "text-red-500" : item.trend === "UP" ? "text-green-500" : "text-muted-foreground"}`} /> })()
          )}
        </div>
      </div>
      <div className="mt-2 h-2.5 overflow-hidden rounded-full bg-muted">
        <div className={`h-full rounded-full transition-all duration-500 ${barColor}`} style={{ width: `${barWidth}%` }} />
      </div>
      <div className="mt-1.5 flex items-center justify-between text-xs text-muted-foreground">
        <span>{item.averageMark !== null ? `${Math.round(item.averageMark)}%` : t("primaryProgress.noData")}</span>
        <span>{t("primaryProgress.assessmentsCount", { completed: item.completedAssessments, total: item.totalAssessments })}</span>
      </div>
    </div>
  )
}

function AttendanceCalendar({ days }: { days: AttendanceDay[] }) {
  const last7 = days.slice(-21)
  return (
    <div className="grid grid-cols-7 gap-1.5">
      {last7.map((day, i) => {
        const cfg = STATUS_CONFIG[day.status] || { bg: "bg-muted", text: "text-muted-foreground", label: "?" }
        const date = new Date(day.date)
        const dayNum = date.getDate()
        return (
          <div
            key={i}
            className={`flex flex-col items-center rounded-lg p-1.5 ${cfg.bg}`}
            title={`${day.date}: ${day.status}`}
          >
            <span className={`text-[10px] font-bold ${cfg.text}`}>{dayNum}</span>
            <span className={`text-[8px] font-semibold ${cfg.text}`}>{cfg.label}</span>
          </div>
        )
      })}
    </div>
  )
}

function BadgeItem({ badge }: { badge: AchievementItem }) {
  const colorMap: Record<string, string> = {
    GOLD: "bg-yellow-100 border-yellow-300",
    SILVER: "bg-gray-100 border-gray-300",
    BRONZE: "bg-orange-100 border-orange-300",
    MILESTONE: "bg-blue-100 border-blue-300",
  }
  const colorClass = badge.color ? colorMap[badge.color.toUpperCase()] || "bg-primary/10 border-primary/30" : "bg-primary/10 border-primary/30"

  return (
    <div className={`rounded-xl border p-3 ${colorClass}`}>
      <div className="flex items-center gap-2">
        <Trophy className="size-5 shrink-0 text-yellow-600" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-foreground">{badge.title}</p>
          <p className="truncate text-xs text-muted-foreground">{badge.description}</p>
        </div>
      </div>
    </div>
  )
}

export default function PrimaryProgressPage() {
  const { user } = useAuth()
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const firstName = user?.name?.split(" ")[0] || t("primaryProgress.parentFallback")
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null)
  const [subjectPerformance, setSubjectPerformance] = useState<SubjectPerformanceItem[]>([])
  const [attendance, setAttendance] = useState<AttendanceDay[]>([])
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [teachers, setTeachers] = useState<TeacherItem[]>([])
  const [achievements, setAchievements] = useState<AchievementItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        const kids = await parentApi.getChildren()
        const primaryKids = kids.filter((c) => c.isPrimary)
        setChildren(primaryKids.length > 0 ? primaryKids : kids)
        if (primaryKids.length > 0) {
          setSelectedChildId(primaryKids[0].studentId)
        } else if (kids.length > 0) {
          setSelectedChildId(kids[0].studentId)
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : t("primaryProgress.loadError")
        setError(msg)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  useEffect(() => {
    if (!selectedChildId) return
    Promise.all([
      parentApi.getChildSubjectPerformance(selectedChildId).catch(() => ({ subjects: [] })),
      parentApi.getChildActivity(selectedChildId).catch(() => ({ activities: [] })),
      parentApi.getChildTeachers(selectedChildId).catch(() => ({ teachers: [] })),
      parentApi.getChildAchievements(selectedChildId).catch(() => ({ achievements: [] })),
      parentApi.getChildAttendance(selectedChildId).catch(() => ({ recentDays: [] })),
    ]).then(([sp, act, tch, ach, att]) => {
      setSubjectPerformance(sp.subjects || [])
      setActivities(act.activities || [])
      setTeachers(tch.teachers || [])
      setAchievements(ach.achievements || [])
      setAttendance(att.recentDays || [])
    })
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
          {t("primaryProgress.errorDesc")}
        </p>
      </div>
    )
  }

  const selectedChild = children.find((c) => c.studentId === selectedChildId)

  const mapTeacherToInfo = (t: TeacherItem): TeacherInfo => ({
    id: t.id,
    firstName: t.fullName.split(" ")[0] || t.fullName,
    lastName: t.fullName.split(" ").slice(1).join(" ") || "",
    email: t.email,
    subjectName: t.subject,
    specialization: t.specialization,
  })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {t("primaryProgress.title")}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {t("primaryProgress.subtitle", { name: firstName })}
        </p>
      </div>

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

      {selectedChild && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <p className="text-xs font-medium text-muted-foreground">{tn("attendance")}</p>
              <div className="mt-2 flex items-center gap-3">
                <AttendanceRing percentage={selectedChild.attendancePercentage} />
                <div>
                  <p className="text-sm text-muted-foreground">{selectedChild.className}</p>
                  <p className="text-xs text-muted-foreground">
                    {t("primaryProgress.daysPresent", { present: selectedChild.daysPresent, total: selectedChild.totalDays })}
                  </p>
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <p className="text-xs font-medium text-muted-foreground">{t("primaryProgress.subjectsEnrolled")}</p>
              <p className="mt-1 text-2xl font-extrabold text-foreground">{subjectPerformance.length}</p>
              <p className="text-xs text-muted-foreground">{t("reports.activeSubjects")}</p>
            </div>

            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <p className="text-xs font-medium text-muted-foreground">{t("primaryProgress.lessonsCompleted")}</p>
              <p className="mt-1 text-2xl font-extrabold text-foreground">{selectedChild.completedAssignments}</p>
              <p className="text-xs text-muted-foreground">{t("primaryProgress.ofTotal", { total: selectedChild.totalAssignments })}</p>
            </div>

            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <p className="text-xs font-medium text-muted-foreground">{t("primaryProgress.avgScore")}</p>
              <p className="mt-1 text-2xl font-extrabold text-foreground">
                {selectedChild.latestAverage !== null ? `${Math.round(selectedChild.latestAverage)}%` : "--"}
              </p>
              <p className="text-xs text-muted-foreground">
                {selectedChild.latestGrade ? t("primaryProgress.gradeValue", { grade: selectedChild.latestGrade }) : t("primaryProgress.noResults")}
              </p>
            </div>
          </div>

          {teachers.length > 0 && (
            <section>
              <h2 className="mb-3 text-base font-semibold text-foreground">{tn("teachers")}</h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {teachers.map((t) => (
                  <TeacherInfoCard key={t.id} teacher={mapTeacherToInfo(t)} />
                ))}
              </div>
            </section>
          )}

          {subjectPerformance.length > 0 && (
            <section>
              <h2 className="mb-3 text-base font-semibold text-foreground">{t("reports.subjectPerfTitle")}</h2>
              <div className="grid gap-3 sm:grid-cols-2">
                {subjectPerformance.map((sp) => (
                  <SubjectBar key={sp.subjectId} item={sp} />
                ))}
              </div>
            </section>
          )}

          <div className="grid gap-6 lg:grid-cols-2">
            {activities.length > 0 && (
              <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                <h2 className="text-base font-semibold text-foreground">{t("primaryProgress.recentActivity")}</h2>
                <div className="mt-3 space-y-2">
                  {activities.slice(0, 8).map((act) => {
                    const Icon = ACTIVITY_ICONS[act.type] || CheckCircle2
                    return (
                      <div key={act.id} className="flex items-center gap-3 rounded-xl bg-muted/30 p-3">
                        <Icon className="size-4 shrink-0 text-muted-foreground" />
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-foreground">{act.title}</p>
                          <p className="text-xs text-muted-foreground">
                            {act.timestamp ? new Date(act.timestamp).toLocaleDateString("en-GB", { weekday: "short", month: "short", day: "numeric" }) : ""}
                          </p>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </section>
            )}

            {attendance.length > 0 && (
              <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
                <h2 className="text-base font-semibold text-foreground">{t("primaryProgress.attendanceCalendar")}</h2>
                <div className="mt-3 flex items-center gap-3 text-[10px] text-muted-foreground">
                  <span className="flex items-center gap-1"><span className="size-2 rounded-sm bg-green-100" /> {ts("present")}</span>
                  <span className="flex items-center gap-1"><span className="size-2 rounded-sm bg-red-100" /> {ts("absent")}</span>
                  <span className="flex items-center gap-1"><span className="size-2 rounded-sm bg-yellow-100" /> {ts("late")}</span>
                </div>
                <div className="mt-3">
                  <AttendanceCalendar days={attendance} />
                </div>
              </section>
            )}
          </div>

          {achievements.length > 0 && (
            <section>
              <h2 className="mb-3 text-base font-semibold text-foreground">{t("primaryProgress.badgesTitle")}</h2>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {achievements.map((ach) => (
                  <BadgeItem key={ach.id} badge={ach} />
                ))}
              </div>
            </section>
          )}

          <div className="flex flex-wrap gap-3">
            <Link
              href="/dashboard/parent"
              className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <ChevronRight className="size-4 rotate-180" />
              {t("assignments.backToDashboard")}
            </Link>
          </div>
        </>
      )}
    </div>
  )
}
