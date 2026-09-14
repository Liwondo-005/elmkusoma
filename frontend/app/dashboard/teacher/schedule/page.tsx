"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { teacherApi, type TeacherClassGroup } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Calendar, Clock, BookOpen, Users, AlertCircle, ChevronLeft, ChevronRight } from "lucide-react"

const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

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
  return DAYS.map((_, i) => {
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

function distributeClassesToDays(classes: TeacherClassGroup[]): Map<number, TeacherClassGroup[]> {
  const map = new Map<number, TeacherClassGroup[]>()
  classes.forEach((cls, i) => {
    const dayIndex = i % 5
    const existing = map.get(dayIndex) || []
    existing.push(cls)
    map.set(dayIndex, existing)
  })
  return map
}

export default function TeacherSchedulePage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState<TeacherClassGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [weekOffset, setWeekOffset] = useState(0)

  const weekDates = getWeekDates(weekOffset)
  const scheduleMap = distributeClassesToDays(classes)

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
      setError("Failed to load schedule data")
    } finally {
      setLoading(false)
    }
  }

  const totalStudents = classes.reduce((sum, c) => sum + c.enrolledStudents, 0)
  const totalLessons = classes.reduce((sum, c) => sum + c.totalLessons, 0)
  const totalAssignments = classes.reduce((sum, c) => sum + c.totalAssignments, 0)

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">My Schedule</h1>
          <p className="mt-1 text-sm text-muted-foreground">Your weekly class schedule overview.</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset((p) => p - 1)}>
            <ChevronLeft className="size-4" />
          </Button>
          <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset(0)}>
            Today
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
              <p className="text-xs text-muted-foreground">Classes</p>
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
              <p className="text-xs text-muted-foreground">Total Students</p>
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
              <p className="text-xs text-muted-foreground">Lessons / Assignments</p>
            </div>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : classes.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Calendar className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Classes Found</h3>
          <p className="mt-2 text-sm text-muted-foreground">You don&apos;t have any classes assigned yet.</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="grid gap-4 lg:grid-cols-7">
            {DAYS.map((day, dayIndex) => {
              const date = weekDates[dayIndex]
              const dayClasses = scheduleMap.get(dayIndex) || []
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
                        <p className="text-xs text-muted-foreground">No classes</p>
                      </div>
                    ) : (
                      dayClasses.map((cls, i) => (
                        <div
                          key={cls.classGroupId}
                          className={`rounded-xl border border-border p-3 transition-all hover:-translate-y-0.5 hover:shadow-sm ${COLORS[i % COLORS.length]}`}
                        >
                          <p className="text-xs font-semibold truncate">{cls.className}</p>
                          <p className="mt-1 text-[10px] opacity-70 truncate">{cls.subjectName}</p>
                          <div className="mt-2 flex items-center gap-2 text-[10px] opacity-70">
                            <span className="flex items-center gap-0.5">
                              <Users className="size-2.5" /> {cls.enrolledStudents}
                            </span>
                            <span className="flex items-center gap-0.5">
                              <BookOpen className="size-2.5" /> {cls.totalLessons}
                            </span>
                          </div>
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
          <h2 className="text-base font-semibold text-foreground">All Classes</h2>
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
                    <Users className="size-3" /> {cls.enrolledStudents} students
                  </span>
                  <span className="flex items-center gap-1">
                    <BookOpen className="size-3" /> {cls.totalLessons} lessons
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="size-3" /> {cls.totalAssignments} assignments
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
