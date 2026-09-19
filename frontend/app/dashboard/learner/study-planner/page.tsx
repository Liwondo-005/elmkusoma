"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { StudyTask } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { ClipboardList, CheckCircle2, Clock, Calendar, BookOpen, Pencil, Beaker, GraduationCap, Layers, Repeat, AlertCircle } from "lucide-react"

function PriorityBadge({ priority }: { priority: string }) {
  const colors: Record<string, string> = {
    LOW: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
    MEDIUM: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    HIGH: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    URGENT: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${colors[priority] || "bg-muted"}`}>
      {priority}
    </span>
  )
}

function TaskTypeIcon({ type }: { type: string }) {
  const icons: Record<string, typeof BookOpen> = {
    STUDY: BookOpen,
    REVISION: Repeat,
    ASSIGNMENT: Pencil,
    PROJECT: Layers,
    RESEARCH: Beaker,
    EXAM_PREP: GraduationCap,
    REPLAY: Clock,
    OTHER: AlertCircle,
  }
  const Icon = icons[type] || AlertCircle
  return <Icon className="size-4 text-muted-foreground" />
}

export default function StudyPlannerPage() {
  const { user, loading: authLoading } = useAuth()
  const [todayTasks, setTodayTasks] = useState<StudyTask[]>([])
  const [weekTasks, setWeekTasks] = useState<StudyTask[]>([])
  const [allTasks, setAllTasks] = useState<StudyTask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadTasks()
  }, [user])

  async function loadTasks() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const [todayRes, weekRes, allRes] = await Promise.allSettled([
        collegeApi.getTodayTasks(studentId),
        collegeApi.getWeekTasks(studentId),
        collegeApi.getStudentTasks(studentId),
      ])
      if (todayRes.status === "fulfilled") setTodayTasks(todayRes.value.data || [])
      if (weekRes.status === "fulfilled") setWeekTasks(weekRes.value.data || [])
      if (allRes.status === "fulfilled") setAllTasks(allRes.value.data || [])
    } catch {
      setError("Failed to load study tasks")
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) return <LoadingState />

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const completedCount = allTasks.filter(t => t.isCompleted).length
  const todayIncomplete = todayTasks.filter(t => !t.isCompleted).length
  const weekIncomplete = weekTasks.filter(t => !t.isCompleted).length

  const groupedByDate = allTasks
    .filter(t => t.scheduledDate)
    .reduce<Record<string, StudyTask[]>>((acc, task) => {
      const date = task.scheduledDate!
      if (!acc[date]) acc[date] = []
      acc[date].push(task)
      return acc
    }, {})

  const sortedDates = Object.keys(groupedByDate).sort()

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadTasks() }} className="ml-auto text-xs underline">Retry</button>
        </div>
      )}

      <LearnerHeader firstName={firstName} subtitle="Plan your study sessions, revision, and academic tasks." />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
              <ClipboardList className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Today's Tasks</p>
              <p className="text-2xl font-extrabold text-foreground">{todayIncomplete}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <Calendar className="size-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">This Week</p>
              <p className="text-2xl font-extrabold text-foreground">{weekIncomplete}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
              <CheckCircle2 className="size-5 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Completed</p>
              <p className="text-2xl font-extrabold text-foreground">{completedCount}</p>
            </div>
          </div>
        </div>
      </div>

      {allTasks.length === 0 ? (
        <EmptyState
          icon={<ClipboardList className="size-8" />}
          title="No study tasks yet"
          description="Create your first study task to start planning your academic sessions."
        />
      ) : (
        <div className="space-y-6">
          {sortedDates.map((date) => {
            const tasks = groupedByDate[date]
            return (
              <div key={date} className="space-y-3">
                <div className="flex items-center gap-2">
                  <Calendar className="size-4 text-muted-foreground" />
                  <h3 className="text-sm font-semibold text-foreground">
                    {new Date(date).toLocaleDateString("en-US", { weekday: "long", year: "numeric", month: "long", day: "numeric" })}
                  </h3>
                  <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                    {tasks.length} tasks
                  </span>
                </div>
                <div className="space-y-2">
                  {tasks.map((task) => (
                    <div
                      key={task.id}
                      className={`flex items-center gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs transition hover:shadow-md ${task.isCompleted ? "opacity-60" : ""}`}
                    >
                      <TaskTypeIcon type={task.taskType} />
                      <div className="flex-1 min-w-0">
                        <p className={`font-medium text-foreground ${task.isCompleted ? "line-through" : ""}`}>
                          {task.title}
                        </p>
                        {task.description && (
                          <p className="text-xs text-muted-foreground line-clamp-1">{task.description}</p>
                        )}
                      </div>
                      <PriorityBadge priority={task.priority} />
                      {task.scheduledTime && (
                        <span className="text-xs text-muted-foreground">{task.scheduledTime}</span>
                      )}
                      {task.durationMinutes && (
                        <span className="flex items-center gap-1 text-xs text-muted-foreground">
                          <Clock className="size-3" />
                          {task.durationMinutes}m
                        </span>
                      )}
                      {task.isCompleted && (
                        <CheckCircle2 className="size-4 text-emerald-500" />
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
