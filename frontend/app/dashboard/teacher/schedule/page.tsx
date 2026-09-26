"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherClassGroup } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Calendar, Clock, BookOpen, Users, AlertCircle, ChevronLeft, ChevronRight, Info } from "lucide-react"

const COLORS = [
  "bg-blue-500/10 text-blue-600",
  "bg-emerald-500/10 text-emerald-600",
  "bg-violet-500/10 text-violet-600",
  "bg-amber-500/10 text-amber-600",
  "bg-rose-500/10 text-rose-600",
  "bg-cyan-500/10 text-cyan-600",
  "bg-pink-500/10 text-pink-600",
]

function getWeekDates(offset: number): Date[] {
  const now = new Date()
  const dayOfWeek = now.getDay()
  const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  const monday = new Date(now)
  monday.setDate(now.getDate() + mondayOffset + offset * 7)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    return d
  })
}

function formatDate(d: Date): string {
  return d.toLocaleDateString("en-US", { month: "short", day: "numeric" })
}

function isToday(d: Date): boolean {
  const now = new Date()
  return d.getDate() === now.getDate() && d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear()
}

function isWeekday(dayIndex: number): boolean {
  return dayIndex >= 0 && dayIndex <= 4
}

export default function TeacherSchedulePage() {
  const { user } = useAuth()
  const t = useTranslations("teacher")
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [weekOffset, setWeekOffset] = useState(0)

  const days = [
    t("schedule.dayMonday"),
    t("schedule.dayTuesday"),
    t("schedule.dayWednesday"),
    t("schedule.dayThursday"),
    t("schedule.dayFriday"),
    t("schedule.daySaturday"),
    t("schedule.daySunday"),
  ]

  const weekDates = getWeekDates(weekOffset)

  useEffect(() => {
    if (!user) return
    loadClasses()
  }, [user])

  async function loadClasses() {
    try {
      setLoading(true)
      setError(null)
      const data = await teacherApi.getClasses()
      setClasses(data)
    } catch {
      setError(t("schedule.loadError"))
    } finally {
      setLoading(false)
    }
  }

  const totalStudents = classes.reduce((sum, c) => sum + c.enrolledStudents, 0)
  const totalLessons = classes.reduce((sum, c) => sum + c.totalLessons, 0)
  const totalAssignments = classes.reduce((sum, c) => sum + c.totalAssignments, 0)

  const todayDayIndex = new Date().getDay() === 0 ? 6 : new Date().getDay() - 1
  const todayClasses = isWeekday(todayDayIndex) ? classes : []

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("schedule.title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("schedule.subtitle")}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset((p) => p - 1)}>
            <ChevronLeft className="size-4" />
          </Button>
          <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset(0)}>
            {t("schedule.today")}
          </Button>
          <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset((p) => p + 1)}>
            <ChevronRight className="size-4" />
          </Button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4">
          <div className="flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="size-4" />
            {error}
          </div>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
              <BookOpen className="size-4 text-primary" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{loading ? "..." : classes.length}</p>
              <p className="text-xs text-muted-foreground">{t("analytics.classes")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg bg-emerald-500/10">
              <Users className="size-4 text-emerald-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{loading ? "..." : totalStudents}</p>
              <p className="text-xs text-muted-foreground">{t("analytics.totalStudents")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg bg-amber-500/10">
              <Clock className="size-4 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{loading ? "..." : `${totalLessons} / ${totalAssignments}`}</p>
              <p className="text-xs text-muted-foreground">{t("schedule.lessonsAssignments")}</p>
            </div>
          </div>
        </div>
      </div>

      {todayClasses.length > 0 && weekOffset === 0 && (
        <section className="rounded-2xl border border-primary/20 bg-primary/5 p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("schedule.todayClasses")}</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {todayClasses.map((cls, i) => (
              <div key={cls.classGroupId} className={`rounded-xl border border-border p-4 ${COLORS[i % COLORS.length]}`}>
                <p className="text-sm font-semibold truncate">{cls.className}</p>
                <p className="mt-1 text-xs opacity-70 truncate">{cls.subjectName}</p>
                <div className="mt-2 flex items-center gap-2 text-[10px] opacity-70">
                  <span className="flex items-center gap-0.5"><Users className="size-2.5" /> {cls.enrolledStudents}</span>
                  <span className="flex items-center gap-0.5"><BookOpen className="size-2.5" /> {t("schedule.lessonsCount", { count: cls.totalLessons })}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : classes.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Calendar className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("schedule.emptyTitle")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{t("schedule.emptyDesc")}</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-2 mb-4">
            <Info className="size-4 text-muted-foreground" />
            <p className="text-xs text-muted-foreground">{t("schedule.weekdayNote")}</p>
          </div>
          <div className="grid gap-4 lg:grid-cols-7">
            {days.map((day, dayIndex) => {
              const date = weekDates[dayIndex]
              const dayClasses = isWeekday(dayIndex) ? classes : []
              const today = isToday(date)

              return (
                <div key={day} className="space-y-2">
                  <div className={`rounded-xl p-3 text-center ${today ? "bg-primary text-primary-foreground" : "bg-muted/50"}`}>
                    <p className="text-xs font-medium uppercase tracking-wide">{day.slice(0, 3)}</p>
                    <p className="mt-0.5 text-lg font-bold">{date.getDate()}</p>
                  </div>
                  <div className="space-y-2 min-h-[120px]">
                    {dayClasses.length === 0 ? (
                      <div className="rounded-xl border border-dashed border-border p-3 text-center">
                        <p className="text-xs text-muted-foreground">{isWeekday(dayIndex) ? t("schedule.noClasses") : t("schedule.weekend")}</p>
                      </div>
                    ) : (
                      dayClasses.map((cls, i) => (
                        <div
                          key={cls.classGroupId}
                          className={`rounded-xl border border-border p-2 transition-all hover:-translate-y-0.5 hover:shadow-sm ${COLORS[i % COLORS.length]}`}
                        >
                          <p className="text-[10px] font-semibold truncate">{cls.className}</p>
                          <p className="mt-0.5 text-[9px] opacity-70 truncate">{cls.subjectName}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {classes.length > 0 && (
        <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground">{t("schedule.allClasses")}</h2>
          <div className="mt-4 space-y-3">
            {classes.map((cls, i) => (
              <div key={cls.classGroupId} className="flex items-center gap-4 rounded-xl border border-border p-4 hover:bg-muted/30 transition-colors">
                <div className={`flex size-10 items-center justify-center rounded-xl ${COLORS[i % COLORS.length]}`}>
                  <BookOpen className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground">{cls.className}</p>
                  <p className="text-xs text-muted-foreground">{cls.subjectName} &middot; {cls.classSection}</p>
                </div>
                <div className="flex items-center gap-6 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <Users className="size-3" /> {t("courses.students", { count: cls.enrolledStudents })}
                  </span>
                  <span className="flex items-center gap-1">
                    <BookOpen className="size-3" /> {t("schedule.lessonsCount", { count: cls.totalLessons })}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="size-3" /> {t("courses.assignments", { count: cls.totalAssignments })}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
